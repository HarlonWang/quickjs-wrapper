package com.whl.quickjs.wrapper;

public class QuickJSContext {

    static {
        System.loadLibrary("quickjs-android-wrapper");
    }

    private static final String UNDEFINED = "undefined.js";


    public static QuickJSContext create() {
        return new QuickJSContext();
    }

    private final long context;

    private QuickJSContext() {
        context = createContext();
    }

    public JSValue evaluate(String script) {
        return evaluate(script, UNDEFINED);
    }

    public JSValue evaluate(String script, String fileName) {
        return new JSValue(context, evaluate(context, script, fileName));
    }

    public JSValue getGlobalObject() {
        return new JSValue(context, getGlobalObject(context));
    }

    public JSValue call(JSValue func, JSValue thisObj, int argCount, JSValue argValue) {
        return new JSValue(context, call(context, func.getValue(), thisObj.getValue(), argCount, 123));
    }

    public void destroyContext() {
        destroyContext(context);
    }

    private native long createContext();
    private native void destroyContext(long context);
    private native long evaluate(long context, String script, String fileName);
    private native long getGlobalObject(long context);
    private native long call(long context, long func, long thisObj, int argCount, long argValue);

}
