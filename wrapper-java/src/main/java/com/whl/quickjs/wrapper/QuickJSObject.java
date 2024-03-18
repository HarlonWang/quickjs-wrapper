package com.whl.quickjs.wrapper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Created by Harlon Wang on 2024/2/12.
 */
public class QuickJSObject implements JSObject {

    private final QuickJSContext context;
    private final long pointer;

    private boolean isReleased;

    public QuickJSObject(QuickJSContext context, long pointer) {
        this.context = context;
        this.pointer = pointer;
    }

    @Override
    public long getPointer() {
        return pointer;
    }

    @Override
    public QuickJSContext getContext() {
        return context;
    }

    @Override
    public Object getProperty(String name) {
        checkReleased();
        return context.getProperty(this, name);
    }

    @Override
    public String getStringProperty(String name) {
        return getString(name);
    }

    @Override
    public void setProperty(String name, String value) {
        context.setProperty(this, name, value);
    }

    @Override
    public void setProperty(String name, int value) {
        context.setProperty(this, name, value);
    }

    @Override
    public void setProperty(String name, long value) {
        context.setProperty(this, name, value);
    }

    @Override
    public void setProperty(String name, JSObject value) {
        context.setProperty(this, name, value);
    }

    @Override
    public void setProperty(String name, boolean value) {
        context.setProperty(this, name, value);
    }

    @Override
    public void setProperty(String name, double value) {
        context.setProperty(this, name, value);
    }

    @Override
    public void setProperty(String name, JSCallFunction value) {
        context.setProperty(this, name, value);
    }

    @Override
    public void setProperty(String name, Class<?> clazz) {
        Object javaObj = null;
        try {
            javaObj = clazz.newInstance();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }

        if (javaObj == null) {
            throw new NullPointerException("The JavaObj cannot be null. An error occurred in newInstance!");
        }

        JSObject jsObj = context.createNewJSObject();
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(JSMethod.class)) {
                Object finalJavaObj = javaObj;
                jsObj.setProperty(method.getName(), args -> {
                    try {
                        return method.invoke(finalJavaObj, args);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                    return null;
                });
            }
        }

        setProperty(name, jsObj);
    }

    @Override
    public String getString(String name) {
        Object value = getProperty(name);
        return value instanceof String ? (String) value : null;
    }

    @Override
    public Integer getIntProperty(String name) {
        return getInteger(name);
    }

    @Override
    public Integer getInteger(String name) {
        Object value = getProperty(name);
        return value instanceof Integer ? (Integer) value : null;
    }

    @Override
    public Boolean getBooleanProperty(String name) {
        return getBoolean(name);
    }

    @Override
    public Boolean getBoolean(String name) {
        Object value = getProperty(name);
        return value instanceof Boolean ? (Boolean) value : null;
    }

    @Override
    public Double getDoubleProperty(String name) {
        return getDouble(name);
    }

    @Override
    public Double getDouble(String name) {
        Object value = getProperty(name);
        return value instanceof Double ? (Double) value : null;
    }

    @Override
    public Long getLong(String name) {
        Object value = getProperty(name);
        return value instanceof Long ? (Long) value : null;
    }

    @Override
    public JSObject getJSObjectProperty(String name) {
        return getJSObject(name);
    }

    @Override
    public JSObject getJSObject(String name) {
        Object value = getProperty(name);
        return value instanceof JSObject ? (JSObject) value : null;
    }

    @Override
    public JSFunction getJSFunctionProperty(String name) {
        return getJSFunction(name);
    }

    @Override
    public JSFunction getJSFunction(String name) {
        Object value = getProperty(name);
        return value instanceof JSFunction ? (JSFunction) value : null;
    }

    @Override
    public JSArray getJSArrayProperty(String name) {
        return getJSArray(name);
    }

    @Override
    public JSArray getJSArray(String name) {
        Object value = getProperty(name);
        return value instanceof JSArray ? (JSArray) value : null;
    }

    @Override
    public JSArray getOwnPropertyNames() {
        return getNames();
    }

    @Override
    public JSArray getNames() {
        return (JSArray) context.getOwnPropertyNames(this);
    }

    @Override
    public void release() {
        checkReleased();

        context.freeValue(this);
        isReleased = true;
    }

    @Override
    public void hold() {
        context.hold(this);
    }

    @Override
    public String stringify() {
        return context.stringify(this);
    }

    @Override
    public boolean isAlive() {
        return context.isLiveObject(this);
    }

    final void checkReleased() {
        if (isReleased) {
            throw new NullPointerException("This JSObject was Released, Can not call this!");
        }
    }

    @Override
    public String toString() {
        checkReleased();

        JSFunction toString = getJSFunction("toString");
        return (String) toString.call();
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new long[]{pointer});
    }

}
