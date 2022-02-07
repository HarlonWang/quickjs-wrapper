package com.whl.quickjs.wrapper;

import android.util.AndroidRuntimeException;

public class QuickJSContext {

    static {
        System.loadLibrary("quickjs-android-wrapper");
    }

    private static final String UNDEFINED = "undefined.js";


    public static QuickJSContext create() {
        return new QuickJSContext();
    }

    /**
     * 处理 Promise 等异步任务的消息循环队列
     */
    private static void executePendingJobLoop(QuickJSContext context) {
        int err;
        for(;;) {
            err = context.executePendingJob();
            if (err <= 0) {
                if (err < 0) {
                    throw new AndroidRuntimeException("Promise execute exception!");
                }
                break;
            }
        }
    }

    private final long context;
    private final NativeCleaner<JSObject> nativeCleaner = new NativeCleaner<JSObject>() {
        @Override
        public void onRemove(long pointer) {
            freeDupValue(context, pointer);
        }
    };

    private QuickJSContext() {
        context = createContext();
    }

    public Object evaluate(String script) {
        return evaluate(script, UNDEFINED);
    }

    public Object evaluate(String script, String fileName) {
        Object obj = evaluate(context, script, fileName);

        executePendingJobLoop(this);

        return obj;
    }

    public JSObject getGlobalObject() {
        return getGlobalObject(context);
    }

    public void destroyContext() {
        nativeCleaner.forceClean();
        destroyContext(context);
    }

    public String stringify(JSObject jsObj) {
        return stringify(context, jsObj.getPointer());
    }

    public Object getProperty(JSObject jsObj, String name) {
        return getProperty(context, jsObj.getPointer(), name);
    }

    public void setProperty(JSObject jsObj, String name, Object value) {
        setProperty(context, jsObj.getPointer(), name, value);
    }

    public void freeValue(JSObject jsObj) {
        freeValue(context, jsObj.getPointer());
    }

    public void dupValue(JSObject jsObj) {
        dupValue(context, jsObj.getPointer());
    }

    public void freeDupValue(JSObject jsObj) {
        freeDupValue(context, jsObj.getPointer());
    }

    public int length(JSArray jsArray) {
        return length(context, jsArray.getPointer());
    }

    public Object get(JSArray jsArray, int index) {
        return get(context, jsArray.getPointer(), index);
    }

    Object call(JSObject func, long objPointer, Object... args) {
        Object obj = call(context, func.getPointer(), objPointer, args);

        executePendingJobLoop(this);

        return obj;
    }

    /**
     * Automatically manage the release of objects，
     * the hold method is equivalent to call the
     * dupValue and freeDupValue methods with NativeCleaner.
     */
    public void hold(JSObject jsObj) {
        jsObj.dupValue();
        nativeCleaner.register(jsObj, jsObj.getPointer());
    }

    /**
     * 1. 返回结果作为参数被 JSFunction 调用，无需关注引用，会自动释放，示例代码如下：
     *    jsFunc.call(parseJSON(json));
     *
     * 2. 返回结果作为属性对象，需要引用计数加1，如果是在 setProperty() 方法里使用，已自动处理，无需关注，示例代码如下：
     *    JSObject obj = parseJSON(json);
     *    globalObj.setProperty(name, obj);
     *
     * 3. 返回结果作为一个JS层方法调用并返回时，需要手动引用计数加1，示例代码如下：
     *        globalObj.setProperty(name, new JSCallFunction() {
     *             public Object call(Object... args) {
     *                 JSObject obj = context.parseJSON(text);
     *                 obj.dupValue();
     *                 return obj;
     *             }
     *         });
     */
    public JSObject parseJSON(String json) {
        return parseJSON(context, json);
    }

    public byte[] compile(String sourceCode) {
        return compile(context, sourceCode);
    }

    public Object execute(byte[] code) {
        return execute(context, code);
    }

    public Object evaluateModule(String script, String moduleName) {
        return evaluateModule(context, script, moduleName);
    }
    
    public Object evaluateModule(String script) {
        return evaluateModule(script, UNDEFINED);
    }

    public int executePendingJob() {
        return executePendingJob(context);
    }

    // context
    private native long createContext();
    private native void destroyContext(long context);

    private native Object evaluate(long context, String script, String fileName);
    private native Object evaluateModule(long context, String script, String fileName);
    private native JSObject getGlobalObject(long context);
    private native Object call(long context, long func, long thisObj, Object[] args);

    private native Object getProperty(long context, long objValue, String name);
    private native void setProperty(long context, long objValue, String name, Object value);
    private native String stringify(long context, long objValue);
    private native int length(long context, long objValue);
    private native Object get(long context, long objValue, int index);
    private native void freeValue(long context, long objValue);
    private native void dupValue(long context, long objValue);
    private native void freeDupValue(long context, long objValue);

    // JSON.parse
    private native JSObject parseJSON(long context, String json);

    // bytecode
    private native byte[] compile(long context, String sourceCode);
    private native Object execute(long context, byte[] bytecode);

    // Promise
    private native int executePendingJob(long context);
}
