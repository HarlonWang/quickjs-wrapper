package com.whl.quickjs.wrapper;

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

    public void setProperty(String name, String value) {
        context.setProperty(this, name, value);
    }

    public void setProperty(String name, int value) {
        context.setProperty(this, name, value);
    }

    public void setProperty(String name, JSObject value) {
        context.setProperty(this, name, value);
    }

    public void setProperty(String name, boolean value) {
        context.setProperty(this, name, value);
    }

    public void setProperty(String name, double value) {
        context.setProperty(this, name, value);
    }

    public void setProperty(String name, JSCallFunction value) {
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

    public Double getDoubleProperty(String name) {
        Object value = getProperty(name);
        return value instanceof Double ? (Double) value : null;
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

    /**
     * JSObject 确定不再使用后，调用该方法可主动释放对 JS 对象的引用。
     * 注意：该方法不能调用多次以及释放后不能再被使用对应的 JS 对象。
     */
    public void release() {
        checkReleased();

        context.freeValue(this);
        isReleased = true;
    }

    public void hold() {
        context.hold(this);
    }

    @Override
    public String toString() {
        checkReleased();

        Object formatString = context.evaluate("__format_string;");
        if (formatString instanceof JSFunction) {
            return (String) ((JSFunction) formatString).call(this);
        }

        return super.toString();
    }

    public String stringify() {
        return context.stringify(this);
    }

    final void checkReleased() {
        if (isReleased) {
            throw new NullPointerException("This JSObject was Released, Can not call this!");
        }
    }

}
