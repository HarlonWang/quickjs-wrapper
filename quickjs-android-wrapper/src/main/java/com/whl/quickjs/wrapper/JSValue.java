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

    public JSValue getProperty(String name) {
        return new JSValue(context, getProperty(context, value, name));
    }

    @Override
    public String toString() {
        return stringify(context, value);
    }

    private native long getProperty(long context, long value, String name);
    private native String stringify(long context, long value);

}
