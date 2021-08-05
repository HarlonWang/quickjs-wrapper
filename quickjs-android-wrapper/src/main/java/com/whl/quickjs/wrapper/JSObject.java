package com.whl.quickjs.wrapper;

public class JSObject {

    private final QuickJSContext context;
    private final long pointer;

    public JSObject(QuickJSContext context, long pointer) {
        this.context = context;
        this.pointer = pointer;
    }

    public long getPointer() {
        return pointer;
    }

    public QuickJSContext getContext() {
        return context;
    }

    public Object getProperty(String name) {
        return context.getProperty(this, name);
    }

    @Override
    public String toString() {
        return context.stringify(this);
    }
}
