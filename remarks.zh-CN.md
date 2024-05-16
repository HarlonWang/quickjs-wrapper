# Other
## 一些说明：
- getProperty 多次获取同一个 JSObject，需要每次 free 下，保持计数平衡
- 作为参数的 JSObject 不可以 FreeValue 释放，会报错，猜测是参数的 JSObject 会自动释放，不需要额外 FreeValue

## 日志调试
在 `quickjs.c` 文件加入以下代码，可以在 `Android` 层打印 `logcat` 日志，方便排查泄漏问题

        // 注意需要放在 #include <stdio.h> 后面，不然会编译报错
        #include <android/log.h>
        #define printf(...) __android_log_print(ANDROID_LOG_DEBUG, "__quickjs__", __VA_ARGS__);
        
        
        #define DUMP_LEAKS  1

## 常见错误：
- fault addr 0x18 in tid 22363：一般是 JSValue 已经被 FreeValue，再次调用就会报这个错误

- list_empty(&rt->gc_obj_list)" failed：一般是使用的 JSValue 没有被 FreeValue 导致，可以打开 `quickjs.c` 里的 `DUMP_LEAKS` 开关查看没有释放的泄漏对象

- TypeError: not a function" failed：一般是该 function 对象被释放后，再次调用的时候就会报这个错

                
                context.setProperty(context.getGlobalObject(), "setTimeout", new JSCallFunction() {
                    @Override
                    public Object call(Object... args) {
                        JSFunction argFunc = (JSFunction) args[0];
                        int delay = (int) args[1];
        
                        // 1. 这里调用没问题，因为还没有执行到 return
                        // context.call(argFunc, context.getGlobalObject());
        
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                // 3. 这里调用有问题，因为是延迟执行，argFunc 实际已经被回收了
                                // 再调用就会报 TypeError: not a function" failed
                                context.call(argFunc, context.getGlobalObject());
                            }
                        }, delay);
                        
                        // 2. 这里 return 执行完就会对 argFunc 回收处理.
                        return null;
                    }
                });
                
- globalObject 的引用计数默认是 2，暂时未弄清楚具体逻辑，目前的释放逻辑是每次 getGlobalObject 后都会 Free 下。待后续优化
                
- String/Atom 内存泄漏问题：虽然调用了 JSCString 的释放方法，但是因为其所在的对象是 GlobalObject，无法被释放，导致一直会被持有，这里需要进一步优化.

- `void gc_decref_child(JSRuntime *, JSGCObjectHeader *): assertion "p->ref_count > 0" failed`
    - 原因：某个对象引用计数为未加一，导致减一的时候校验失败
    - 排查方式：先打印出 `gc_decref_child` 里 `p->ref_count <= 0` 的对象信息：
        ```c
            static void gc_decref_child(JSRuntime *rt, JSGCObjectHeader *p)
            {
                // 打印异常对象信息
                if (p->ref_count <= 0) {
                    JS_DumpGCObject(rt, p)
                }
                assert(p->ref_count > 0);
                ...
            }
        ```
        因为执行到方法时，对象一般已经释放，会显示为 `null`，但是可以看到指针信息，可以选择以下任一方式定位到具体对象信息：
        - 方式1：打开 `quickjs.c` 里的 DUMP_FREE 开关，打印出所有的 `free` 对象，然后指针去匹配查找到释放前的对象信息
        - 方式2：找到 `__JS_FreeValueRT` 方法，并注释以下代码，不释放对象，这样在 `gc_decref_child` 里就可以看到对象的具体信息。              
    

                        case JS_TAG_OBJECT:
                        case JS_TAG_FUNCTION_BYTECODE:
                            {
                    //            JSGCObjectHeader *p = JS_VALUE_GET_PTR(v);
                    //            if (rt->gc_phase != JS_GC_PHASE_REMOVE_CYCLES) {
                    //                list_del(&p->link);
                    //                list_add(&p->link, &rt->gc_zero_ref_count_list);
                    //                if (rt->gc_phase == JS_GC_PHASE_NONE) {
                    //                    free_zero_refcount(rt);
                    //                }
                    //            }