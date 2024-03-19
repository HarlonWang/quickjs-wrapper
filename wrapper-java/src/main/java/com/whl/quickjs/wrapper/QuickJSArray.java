package com.whl.quickjs.wrapper;

/**
 * Created by Harlon Wang on 2024/2/13.
 */
public class QuickJSArray extends QuickJSObject implements JSArray {

    public QuickJSArray(QuickJSContext context, long pointer) {
        super(context, pointer);
    }

    @Override
    public int length() {
        checkRefCountIsZero();
        return getContext().length(this);
    }

    @Override
    public Object get(int index) {
        checkRefCountIsZero();
        return getContext().get(this, index);
    }

    @Override
    public void set(Object value, int index) {
        checkRefCountIsZero();
        getContext().set(this, value, index);
    }
}
