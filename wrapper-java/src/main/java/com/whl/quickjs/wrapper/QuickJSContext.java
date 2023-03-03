package com.whl.quickjs.wrapper;

import java.io.File;

public class QuickJSContext {

    private static final String UNKNOWN_FILE = "unknown.js";

    public static QuickJSContext create() {
        return new QuickJSContext();
    }

    public boolean isLiveObject(JSObject jsObj) {
        return isLiveObject(runtime, jsObj.getPointer());
    }

    public void setMaxStackSize(int maxStackSize) {
        setMaxStackSize(runtime, maxStackSize);
    }

    public void runGC() {
        runGC(runtime);
    }

    public void setMemoryLimit(int memoryLimitSize) {
        setMemoryLimit(runtime, memoryLimitSize);
    }

    public void dumpMemoryUsage(File target) {
        if (target == null || !target.exists()) {
            return;
        }

        dumpMemoryUsage(runtime, target.getAbsolutePath());
    }

    private final long runtime;
    private final long context;
    private final NativeCleaner<JSObject> nativeCleaner = new NativeCleaner<JSObject>() {
        @Override
        public void onRemove(long pointer) {
            freeDupValue(context, pointer);
        }
    };
    private final long currentThreadId;
    private boolean destroyed = false;

    private QuickJSContext() {
        try {
            runtime = createRuntime();
            context = createContext(runtime);
        } catch (UnsatisfiedLinkError e) {
            throw new QuickJSException("The so library must be initialized before createContext! QuickJSLoader.init should be called on the Android platform. In the JVM, you need to manually call System.loadLibrary");
        }
        currentThreadId = Thread.currentThread().getId();
    }

    private void checkSameThread() {
        boolean isSameThread = currentThreadId == Thread.currentThread().getId();
        if (!isSameThread) {
            throw new QuickJSException("Must be call same thread in QuickJSContext.create!");
        }
    }

    private void checkDestroyed() {
        if (destroyed) {
            throw new QuickJSException("Can not called this after QuickJSContext was destroyed!");
        }
    }

    public Object evaluate(String script) {
        return evaluate(script, UNKNOWN_FILE);
    }

    public Object evaluate(String script, String fileName) {
        checkSameThread();
        checkDestroyed();
        return evaluate(context, script, fileName);
    }

    public JSObject getGlobalObject() {
        checkSameThread();
        checkDestroyed();
        return getGlobalObject(context);
    }

    public void destroy() {
        checkSameThread();
        checkDestroyed();

        nativeCleaner.forceClean();
        destroyContext(context);
        destroyed = true;
    }

    public String stringify(JSObject jsObj) {
        checkSameThread();
        checkDestroyed();
        return stringify(context, jsObj.getPointer());
    }

    public Object getProperty(JSObject jsObj, String name) {
        checkSameThread();
        checkDestroyed();
        return getProperty(context, jsObj.getPointer(), name);
    }

    public void setProperty(JSObject jsObj, String name, Object value) {
        checkSameThread();
        checkDestroyed();

        setProperty(context, jsObj.getPointer(), name, value);
    }

    public void freeValue(JSObject jsObj) {
        checkSameThread();
        checkDestroyed();

        freeValue(context, jsObj.getPointer());
    }

    /**
     * Native 层注册的 JS 方法里的对象需要在其他地方使用，
     * 调用该方法进行计数加一增加引用，不然 JS 方法执行完会被回收掉。
     * 注意：不再使用的时候，调用对应的 {@link #freeDupValue(JSObject)} 方法进行计数减一。
     */
    private void dupValue(JSObject jsObj) {
        checkSameThread();
        checkDestroyed();

        dupValue(context, jsObj.getPointer());
    }

    /**
     * 引用计数减一，对应 {@link #dupValue(JSObject)}
     */
    private void freeDupValue(JSObject jsObj) {
        checkSameThread();
        checkDestroyed();

        freeDupValue(context, jsObj.getPointer());
    }

    public int length(JSArray jsArray) {
        checkSameThread();
        checkDestroyed();

        return length(context, jsArray.getPointer());
    }

    public Object get(JSArray jsArray, int index) {
        checkSameThread();
        checkDestroyed();

        return get(context, jsArray.getPointer(), index);
    }

    public void set(JSArray jsArray, Object value, int index) {
        checkSameThread();
        checkDestroyed();

        set(context, jsArray.getPointer(), value, index);
    }

    Object call(JSObject func, long objPointer, Object... args) {
        checkSameThread();
        checkDestroyed();

        return call(context, func.getPointer(), objPointer, args);
    }

    /**
     * Automatically manage the release of objects，
     * the hold method is equivalent to call the
     * dupValue and freeDupValue methods with NativeCleaner.
     */
    public void hold(JSObject jsObj) {
        checkSameThread();
        checkDestroyed();

        dupValue(jsObj);
        nativeCleaner.register(jsObj, jsObj.getPointer());
    }

    public JSObject createNewJSObject() {
        return (JSObject) parseJSON("{}");
    }

    public JSArray createNewJSArray() {
        return (JSArray) parseJSON("[]");
    }

    /**
     * Use {@link #parse(String)} replace.
     */
    @Deprecated
    public JSObject parseJSON(String json) {
        checkSameThread();
        checkDestroyed();

        Object obj = parseJSON(context, json);
        if (!(obj instanceof JSObject)) {
            throw new QuickJSException("Only parse json with valid format, must be start with '{', if it contains other case, use parse(String) replace.");
        }

        return (JSObject) obj;
    }

    public Object parse(String json) {
        return parseJSON(context, json);
    }

    public byte[] compile(String sourceCode) {
        return compile(sourceCode, UNKNOWN_FILE);
    }

    public byte[] compile(String sourceCode, String fileName) {
        checkSameThread();
        checkDestroyed();

        return compile(context, sourceCode, fileName);
    }

    public Object execute(byte[] code) {
        checkSameThread();
        checkDestroyed();

        return execute(context, code);
    }

    public Object evaluateModule(String script, String moduleName) {
        checkSameThread();
        checkDestroyed();

        return evaluateModule(context, script, moduleName);
    }
    
    public Object evaluateModule(String script) {
        return evaluateModule(script, UNKNOWN_FILE);
    }

    public void throwJSException(String error) {
        // throw $error;
        String errorScript = "throw " + "\"" + error + "\"" + ";";
        evaluate(errorScript);
    }

    // runtime
    private native long createRuntime();
    private native void setMaxStackSize(long runtime, int size); // The default is 1024 * 256, and 0 means unlimited.
    private native boolean isLiveObject(long runtime, long objValue);
    private native void runGC(long runtime);
    private native void setMemoryLimit(long runtime, int size);
    private native void dumpMemoryUsage(long runtime, String fileName);

    // context
    private native long createContext(long runtime);
    private native Object evaluate(long context, String script, String fileName);
    private native Object evaluateModule(long context, String script, String fileName);
    private native JSObject getGlobalObject(long context);
    private native Object call(long context, long func, long thisObj, Object[] args);
    private native Object getProperty(long context, long objValue, String name);
    private native void setProperty(long context, long objValue, String name, Object value);
    private native String stringify(long context, long objValue);
    private native int length(long context, long objValue);
    private native Object get(long context, long objValue, int index);
    private native void set(long context, long objValue, Object value, int index);
    private native void freeValue(long context, long objValue);
    private native void dupValue(long context, long objValue);
    private native void freeDupValue(long context, long objValue);
    private native Object parseJSON(long context, String json);
    private native byte[] compile(long context, String sourceCode, String fileName); // Bytecode compile
    private native Object execute(long context, byte[] bytecode); // Bytecode execute

    // destroy context and runtime
    private native void destroyContext(long context);
}
