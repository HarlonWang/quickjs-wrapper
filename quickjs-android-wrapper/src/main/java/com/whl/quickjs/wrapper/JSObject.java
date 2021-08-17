package com.whl.quickjs.wrapper;

import android.util.AndroidRuntimeException;

public class JSObject {

    private final QuickJSContext context;
    private final long pointer;

    private boolean isReleased;

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
        checkReleased();
        return context.getProperty(this, name);
    }

    public void setProperty(String name, Object value) {
        context.setProperty(this, name, value);
    }

    public void free() {
        checkReleased();

        context.freeValue(this);
        isReleased = true;
    }

    @Override
    public String toString() {
        checkReleased();
        return context.stringify(this);
    }

    final void checkReleased() {
        if (isReleased) {
            throw new AndroidRuntimeException("This JSObject was Released, Can not call this!");
        }
    }

}
