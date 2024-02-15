package com.whl.quickjs.wrapper;

public interface JSObject {
    void setProperty(String name, String value);
    void setProperty(String name, int value);
    void setProperty(String name, long value);
    void setProperty(String name, JSObject value);
    void setProperty(String name, boolean value);
    void setProperty(String name, double value);
    void setProperty(String name, JSCallFunction value);
    void setProperty(String name, Class<?> clazz);
    long getPointer();
    QuickJSContext getContext();
    Object getProperty(String name);
    String getString(String name);
    Integer getInteger(String name);
    Boolean getBoolean(String name);
    Double getDouble(String name);
    Long getLong(String name);
    JSObject getJSObject(String name);
    JSFunction getJSFunction(String name);
    JSArray getJSArray(String name);
    JSArray getNames();
    String stringify();
    boolean isAlive();
    void release();
    void hold();
}
