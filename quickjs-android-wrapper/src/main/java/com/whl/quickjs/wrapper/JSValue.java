package com.whl.quickjs.wrapper;

public class JSValue {
    private final long context;
    private final long value;

    public JSValue(long context, long value) {
        this.context = context;
        this.value = value;
    }

    @Override
    public String toString() {
        return "JSValue{" +
                "value=" + value +
                '}';
    }
}
