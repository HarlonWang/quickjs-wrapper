package com.whl.quickjs.wrapper;

public class QuickJSContext {

    private static final String UNKNOWN_FILE = "unknown.js";

    public static QuickJSContext create(long runtime) {
        return new QuickJSContext(runtime);
    }

    public static QuickJSContext create() {
        return new QuickJSContext(createRuntime());
    }

    public static void destroyRuntime(QuickJSContext context) {
        destroyRuntime(context.getRuntime());
    }

    public static boolean isLiveObject(long runtime, JSObject jsObj) {
        return isLiveObject(runtime, jsObj.getPointer());
    }

    public static boolean isLiveObject(QuickJSContext context, JSObject jsObj) {
        return isLiveObject(context.getRuntime(), jsObj.getPointer());
    }

    public static void setMaxStackSize(QuickJSContext context, int maxStackSize) {
        setMaxStackSize(context.getRuntime(), maxStackSize);
    }

    public static void setMemoryLimit(QuickJSContext context, int memoryLimitSize) {
        setMemoryLimit(context.getRuntime(), memoryLimitSize);
    }

    public static void runGC(QuickJSContext context) {
        runGC(context.getRuntime());
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

    private QuickJSContext(long runtime) {
        this.runtime = runtime;
        try {
            context = createContext(runtime);
        } catch (UnsatisfiedLinkError e) {
            throw new QuickJSException("The so library must be initialized before createContext! QuickJSLoader.init should be called on the Android platform. In the JVM, you need to manually call System.loadLibrary");
        }
        currentThreadId = Thread.currentThread().getId();
    }

    public long getRuntime() {
        return runtime;
    }

    private void checkSameThread() {
        boolean isSameThread = currentThreadId == Thread.currentThread().getId();
        if (!isSameThread) {
            throw new QuickJSException("Must be call same thread in QuickJSContext.create!");
        }
    }

    public Object evaluate(String script) {
        return evaluate(script, UNKNOWN_FILE);
    }

    public Object evaluate(String script, String fileName) {
        checkSameThread();
        return evaluate(context, script, fileName);
    }

    public JSObject getGlobalObject() {
        checkSameThread();
        return getGlobalObject(context);
    }

    public void destroy() {
        checkSameThread();

        nativeCleaner.forceClean();
        destroyContext(context);
    }

    public String stringify(JSObject jsObj) {
        checkSameThread();
        return stringify(context, jsObj.getPointer());
    }

    public Object getProperty(JSObject jsObj, String name) {
        checkSameThread();
        return getProperty(context, jsObj.getPointer(), name);
    }

    public void setProperty(JSObject jsObj, String name, Object value) {
        checkSameThread();

        setProperty(context, jsObj.getPointer(), name, value);
    }

    public void freeValue(JSObject jsObj) {
        checkSameThread();
        freeValue(context, jsObj.getPointer());
    }

    /**
     * Native 层注册的 JS 方法里的对象需要在其他地方使用，
     * 调用该方法进行计数加一增加引用，不然 JS 方法执行完会被回收掉。
     * 注意：不再使用的时候，调用对应的 {@link #freeDupValue(JSObject)} 方法进行计数减一。
     */
    private void dupValue(JSObject jsObj) {
        checkSameThread();
        dupValue(context, jsObj.getPointer());
    }

    /**
     * 引用计数减一，对应 {@link #dupValue(JSObject)}
     */
    private void freeDupValue(JSObject jsObj) {
        checkSameThread();
        freeDupValue(context, jsObj.getPointer());
    }

    public int length(JSArray jsArray) {
        checkSameThread();
        return length(context, jsArray.getPointer());
    }

    public Object get(JSArray jsArray, int index) {
        checkSameThread();
        return get(context, jsArray.getPointer(), index);
    }

    public void set(JSArray jsArray, Object value, int index) {
        checkSameThread();
        set(context, jsArray.getPointer(), value, index);
    }

    Object call(JSObject func, long objPointer, Object... args) {
        checkSameThread();

        return call(context, func.getPointer(), objPointer, args);
    }

    /**
     * Automatically manage the release of objects，
     * the hold method is equivalent to call the
     * dupValue and freeDupValue methods with NativeCleaner.
     */
    public void hold(JSObject jsObj) {
        checkSameThread();

        dupValue(jsObj);
        nativeCleaner.register(jsObj, jsObj.getPointer());
    }

    public JSObject createNewJSObject() {
        return parseJSON("{}");
    }

    public JSArray createNewJSArray() {
        return (JSArray) parseJSON("[]");
    }

    public JSObject parseJSON(String json) {
        checkSameThread();
        return parseJSON(context, json);
    }

    public byte[] compile(String sourceCode) {
        checkSameThread();
        return compile(context, sourceCode, UNKNOWN_FILE);
    }

    public byte[] compile(String sourceCode, String fileName) {
        checkSameThread();
        return compile(context, sourceCode, fileName);
    }

    public Object execute(byte[] code) {
        checkSameThread();
        return execute(context, code);
    }

    public Object evaluateModule(String script, String moduleName) {
        return evaluateModule(context, script, moduleName);
    }
    
    public Object evaluateModule(String script) {
        checkSameThread();
        return evaluateModule(script, UNKNOWN_FILE);
    }

    public void throwJSException(String error) {
        checkSameThread();

        // throw $error;
        String errorScript = "throw " + "\"" + error + "\"" + ";";
        evaluate(errorScript);
    }

    // runtime
    public static native long createRuntime();
    public static native void destroyRuntime(long runtime);
    private static native void setMaxStackSize(long runtime, int size); // The default is 1024 * 256, and 0 means unlimited.
    private static native boolean isLiveObject(long runtime, long objValue);
    private static native void runGC(long runtime);
    private static native void setMemoryLimit(long runtime, int size);

    // context
    private native long createContext(long runtime);
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
    private native void set(long context, long objValue, Object value, int index);
    private native void freeValue(long context, long objValue);
    private native void dupValue(long context, long objValue);
    private native void freeDupValue(long context, long objValue);
    private native JSObject parseJSON(long context, String json);
    private native byte[] compile(long context, String sourceCode, String fileName); // Bytecode compile
    private native Object execute(long context, byte[] bytecode); // Bytecode execute
}
