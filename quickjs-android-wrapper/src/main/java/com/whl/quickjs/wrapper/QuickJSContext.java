package com.whl.quickjs.wrapper;

import android.util.AndroidRuntimeException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class QuickJSContext {

    static {
        System.loadLibrary("quickjs-android-wrapper");
    }

    private static final String UNDEFINED = "undefined.js";


    public static QuickJSContext create() {
        return new QuickJSContext();
    }

    public static QuickJSContext create(int maxStackSize) {
        QuickJSContext context = create();
        context.setMaxStackSize(maxStackSize);
        return context;
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

    public interface ExceptionHandler {
        void handle(String error);
    }

    private final long context;
    private final NativeCleaner<JSObject> nativeCleaner = new NativeCleaner<JSObject>() {
        @Override
        public void onRemove(long pointer) {
            freeDupValue(context, pointer);
        }
    };
    private ExceptionHandler exceptionHandler;
    private final long currentThreadId;
    private final List<JSCallFunction> jsCallFunctions = new ArrayList<>();

    private QuickJSContext() {
        context = createContext();
        currentThreadId = Thread.currentThread().getId();
    }

    private void checkSameThread() {
        boolean isSameThread = currentThreadId == Thread.currentThread().getId();
        if (!isSameThread) {
            throw new AndroidRuntimeException("Must be call same thread in QuickJSContext.create!");
        }
    }

    public Object evaluate(String script) {
        return evaluate(script, UNDEFINED);
    }

    public Object evaluate(String script, String fileName) {
        checkSameThread();

        Object obj = null;
        try {
            obj = evaluate(context, script, fileName);
        } catch (QuickJSException e) {
            if (exceptionHandler != null) {
                exceptionHandler.handle(writerToString(e));
            } else {
                e.printStackTrace();
            }
        }

        executePendingJobLoop(this);

        return obj;
    }

    public JSObject getGlobalObject() {
        checkSameThread();
        return getGlobalObject(context);
    }

    public void destroyContext() {
        checkSameThread();
        jsCallFunctions.clear();
        nativeCleaner.forceClean();
        destroyContext(context);
    }

    public String stringify(JSObject jsObj) {
        checkSameThread();

        try {
            return stringify(context, jsObj.getPointer());
        } catch (QuickJSException e) {
            if (exceptionHandler != null) {
                exceptionHandler.handle(writerToString(e));
            } else {
                e.printStackTrace();
            }
        }

        return null;
    }

    public Object getProperty(JSObject jsObj, String name) {
        checkSameThread();

        try {
            return getProperty(context, jsObj.getPointer(), name);
        } catch (QuickJSException e) {
            if (exceptionHandler != null) {
                exceptionHandler.handle(writerToString(e));
            } else {
                e.printStackTrace();
            }
        }

        return null;
    }

    public void setProperty(JSObject jsObj, String name, Object value) {
        checkSameThread();

        setProperty(context, jsObj.getPointer(), name, value);
    }

    public void setJavaObjectApi(JSObject rootObject, String name, Object javaObject) {
        checkSameThread();
        JSObject jsObject = createNewJSObject();
        setProperty(context, rootObject.getPointer(), name, jsObject);
        for (Method method : javaObject.getClass().getMethods()) {
            if (method.isAnnotationPresent(JSJavaApi.class)) {
                MyJSCallFunction function = new MyJSCallFunction(javaObject, method);
                jsCallFunctions.add(function);
                jsObject.setProperty(method.getName(), function);
            }
        }
        jsObject.release();
    }

    private static class MyJSCallFunction implements JSCallFunction {
        private final Method method;
        private final Object javaObject;

        public MyJSCallFunction(Object javaObject, Method method) {
            this.javaObject = javaObject;
            this.method = method;
        }

        @Override
        public Object call(Object... args) {
            try {
                return method.invoke(javaObject, args);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            return null;
        }
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

        Object obj = null;
        try {
            obj = call(context, func.getPointer(), objPointer, args);
        } catch (QuickJSException e) {
            if (exceptionHandler != null) {
                exceptionHandler.handle(writerToString(e));
            } else {
                e.printStackTrace();
            }
        }

        executePendingJobLoop(this);

        return obj;
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
        try {
            return parseJSON(context, json);
        } catch (QuickJSException e) {
            if (exceptionHandler != null) {
                exceptionHandler.handle(writerToString(e));
            } else {
                e.printStackTrace();
            }
        }

        return null;
    }

    public byte[] compile(String sourceCode) {
        checkSameThread();
        return compile(context, sourceCode);
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
        return evaluateModule(script, UNDEFINED);
    }

    public int executePendingJob() {
        checkSameThread();
        return executePendingJob(context);
    }

    public void setExceptionHandler(ExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    private String writerToString(QuickJSException e) {
        Writer writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    public void throwJSException(String error) {
        checkSameThread();

        // throw $error;
        String errorScript = "throw " + "\"" + error + "\"" + ";";
        evaluate(errorScript);
    }

    public void setMaxStackSize(int maxStackSize) {
        checkSameThread();
        setMaxStackSize(context, maxStackSize);
    }

    // context
    private native long createContext();
    private native void destroyContext(long context);

    private native Object evaluate(long context, String script, String fileName) throws QuickJSException;
    private native Object evaluateModule(long context, String script, String fileName);
    private native JSObject getGlobalObject(long context);
    private native Object call(long context, long func, long thisObj, Object[] args) throws QuickJSException;

    private native Object getProperty(long context, long objValue, String name) throws QuickJSException;
    private native void setProperty(long context, long objValue, String name, Object value);
    private native String stringify(long context, long objValue) throws QuickJSException;
    private native int length(long context, long objValue);
    private native Object get(long context, long objValue, int index);
    private native void set(long context, long objValue, Object value, int index);
    private native void freeValue(long context, long objValue);
    private native void dupValue(long context, long objValue);
    private native void freeDupValue(long context, long objValue);

    // JSON.parse
    private native JSObject parseJSON(long context, String json) throws QuickJSException;

    // bytecode
    private native byte[] compile(long context, String sourceCode);
    private native Object execute(long context, byte[] bytecode);

    // Promise
    private native int executePendingJob(long context);

    // The default is 1024 * 256, and 0 means unlimited.
    private native void setMaxStackSize(long context, int size);
}
