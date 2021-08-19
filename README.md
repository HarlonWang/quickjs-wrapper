# quickjs-android-wrapper
coming soon

# Other
## 一些说明：
1. getProperty 多次获取同一个 JSObject，需要每次 free 下，保持计数平衡
2. 作为参数的 JSObject 不可以 FreeValue 释放，会报错，猜测是参数的 JSObject 会自动释放，不需要额外 FreeValue

## 常见错误：
1. fault addr 0x18 in tid 22363

一般是 JSValue 已经被 FreeValue，再次调用就会报这个错误

2. list_empty(&rt->gc_obj_list)" failed

一般是使用的 JSValue 没有被 FreeValue 导致

3. TypeError: not a function" failed

一般是该 function 对象被释放后，再次调用的时候就会报这个错

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