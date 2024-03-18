package com.whl.quickjs.wrapper;

/**
 * Created by Harlon Wang on 2024/2/13.
 */
public class QuickJSArray extends QuickJSObject implements JSArray {

    public QuickJSArray(QuickJSContext context, long pointer, boolean needToRelease) {
        super(context, pointer, needToRelease);
    }

    @Override
    public int length() {
        checkReleased();
        return getContext().length(this);
    }

    @Override
    public Object get(int index) {
        checkReleased();
        return getContext().get(this, index);
    }

    @Override
    public void set(Object value, int index) {
        checkReleased();
        getContext().set(this, value, index);
    }
}
