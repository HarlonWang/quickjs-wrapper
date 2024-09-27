package com.whl.quickjs.wrapper;

import java.util.ArrayList;
import java.util.HashMap;

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

    @Override
    public HashMap<String, Object> toMap() {
        throw new UnsupportedOperationException("Array types are not yet supported for conversion to map. You should use toArray.");
    }

    @Override
    public ArrayList<Object> toArray() {
        ArrayList<Object> arrayList = new ArrayList<>(length());
        convertToMap(this, arrayList);
        circulars.clear();
        return arrayList;
    }
}
