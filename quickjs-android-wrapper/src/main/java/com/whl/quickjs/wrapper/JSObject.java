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

    public String getStringProperty(String name) {
        Object value = getProperty(name);
        return value instanceof String ? (String) value : null;
    }

    public Integer getIntProperty(String name) {
        Object value = getProperty(name);
        return value instanceof Integer ? (Integer) value : null;
    }

    public Boolean getBooleanProperty(String name) {
        Object value = getProperty(name);
        return value instanceof Boolean ? (Boolean) value : null;
    }

    public JSObject getJSObjectProperty(String name) {
        Object value = getProperty(name);
        return value instanceof JSObject ? (JSObject) value : null;
    }

    public JSFunction getJSFunctionProperty(String name) {
        Object value = getProperty(name);
        return value instanceof JSFunction ? (JSFunction) value : null;
    }

    public JSArray getJSArrayProperty(String name) {
        Object value = getProperty(name);
        return value instanceof JSArray ? (JSArray) value : null;
    }

    public JSArray getOwnPropertyNames() {
        JSFunction getOwnPropertyNames = (JSFunction) context.evaluate("Object.getOwnPropertyNames");
        return (JSArray) getOwnPropertyNames.call(this);
    }

    public void release() {
        checkReleased();

        context.freeValue(this);
        isReleased = true;
    }

    /**
     * Release with @freeDupValue
     */
    public void dupValue() {
        checkReleased();
        context.dupValue(this);
    }

    public void freeDupValue() {
        checkReleased();
        context.freeDupValue(this);
    }

    public void hold() {
        context.hold(this);
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
