# Other
## Disclaimer: this file was translated using DeepL Translate
## Some notes:
- getProperty gets the same JSObject multiple times, need to free it every time to keep the count balanced.
- JSObject as a parameter can not be FreeValue released, it will report an error, guess the parameter JSObject will be automatically released, no need to extra FreeValue.

## Log debugging
Add the following code to the `quickjs.c` file to print a `logcat` log at the `Android` level for troubleshooting leaks.

        // Note that it needs to be placed after #include <stdio.h>, otherwise it will compile with errors.
        #include <android/log.h
        #define printf(...) __android_log_print(ANDROID_LOG_DEBUG, "__quickjs__", __VA_ARGS__);
        
        
        #define DUMP_LEAKS 1

## Common error:
- fault addr 0x18 in tid 22363: generally JSValue has been FreeValue, call again will report this error

- list_empty(&rt->gc_obj_list)" failed: Usually the JSValue used is not FreeValue, you can open the `DUMP_LEAKS` switch in `quickjs.c` to check the leaked objects that have not been freed.

- TypeError: not a function" failed: Usually when the function object is released and called again, this error will be reported.

                
                context.setProperty(context.getGlobalObject(), "setTimeout", new JSCallFunction() {
                    @Override
                    public Object call(Object... args) {
                        JSFunction argFunc = (JSFunction) args[0];
                        int delay = (int) args[1]; // 1.
        
                        // 1. The call here is fine, because it hasn't reached return yet.
                        // context.call(argFunc, context.getGlobalObject()); new Handler(Looper.getGlobalObject()); new Handler(Looper.
        
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            postDelayed(new Runnable() { @Override
                            public void run() {
                                // 3. There's a problem with this call, because it's delayed and argFunc is actually recycled.
                                // If you call it again, you get a TypeError: not a function" failed
                                context.call(argFunc, context.getGlobalObject()); }
                            }
                        context.call(argFunc, context.getGlobalObject()); }, delay); // 2.
                        
                        // 2. Here, argFunc is recycled after return is executed.
                        return null; }
                    }
                }); // 2.
                
- The reference count of globalObject is 2 by default, but we haven't figured out the logic yet. The current release logic is to free the globalObject after each getGlobalObject. To be optimized later.
                
- String/Atom memory leakage problem: Although JSCString's free method is called, it can't be freed because the object it is in is GlobalObject, so it will be held for a long time, which needs to be further optimized.

- `void gc_decref_child(JSRuntime *, JSGCObjectHeader *): assertion "p->ref_count > 0" failed`
    - Reason: The reference count of an object has not been increased by one, so the check fails when decreasing by one.
    - Troubleshooting: first print out the information of the object `p->ref_count <= 0` in `gc_decref_child`:
        `gc_decref_child` static void gc_decref_child
            static void gc_decref_child(JSRuntime *rt, JSGCObjectHeader *p)
            {
                // Print the exception object information
                if (p->ref_count <= 0) {
                    JS_DumpGCObject(rt, p)
                }
                assert(p->ref_count > 0); ...
                ...
            }
        ```
        Since the object is usually freed by the time the method is executed, it will appear as `null`, but you can see the pointer information, and you can locate the specific object information in one of the following ways:
        - Way 1: Turn on the DUMP_FREE switch in `quickjs.c`, print out all the `free` objects, and then match the pointers to find the information of the objects before they were freed.
        - Option 2: Find the `__JS_FreeValueRT` method and comment out the following code without freeing the object so that you can see the object's details in `gc_decref_child`.              
    

                        case JS_TAG_OBJECT.
                        case JS_TAG_FUNCTION_BYTECODE.
                            {
                    // JSGCObjectHeader *p = JS_VALUE_GET_PTR(v); }
                    // if (rt->gc_phase ! = JS_GC_PHASE_REMOVE_CYCLES) {
                    // list_del(&p->link); }
                    // list_add(&p->link, &rt->gc_zero_ref_count_list);
                    // if (rt->gc_phase == JS_GC_PHASE_NONE) {
                    // free_zero_refcount(rt); }
                    // }
                    // }