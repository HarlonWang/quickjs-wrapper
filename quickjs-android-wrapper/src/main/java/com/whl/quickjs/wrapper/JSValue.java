package com.whl.quickjs.wrapper;

public class JSValue {
    private final long context;
    private final long value;

    public JSValue(long context, long value) {
        this.context = context;
        this.value = value;
    }

    public long getValue() {
        return value;
    }

    public long getContext() {
        return context;
    }

    public Object getProperty(String name) {
        Object result = getProperty(context, value, name);
        if (result instanceof Long) {
            return new JSValue(context, (Long) result);
        }

        return result;
    }

    public boolean isArray() {
        return isArray(context, value);
    }

    public int getLength() {
        return getLength(context, value);
    }

    public JSValue getByIndex(int index) {
        return new JSValue(context, get(context, value, index));
    }

    @Override
    public String toString() {
        return stringify(context, value);
    }

    private native Object getProperty(long context, long value, String name);
    private native String stringify(long context, long value);
    private native boolean isArray(long context, long value);
    private native int getLength(long context, long value);
    private native long get(long context, long value, int index);

}
