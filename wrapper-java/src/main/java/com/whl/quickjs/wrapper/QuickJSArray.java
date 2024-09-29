package com.whl.quickjs.wrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

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
        return toMap(null);
    }

    @Override
    public HashMap<String, Object> toMap(MapFilter filter) {
        return toMap(filter, null);
    }

    @Override
    public ArrayList<Object> toArray() {
        return toArray(null);
    }

    @Override
    public ArrayList<Object> toArray(MapFilter filter) {
        return toArray(filter, null);
    }

    @Override
    public ArrayList<Object> toArray(MapFilter filter, Object extra) {
        ArrayList<Object> arrayList = new ArrayList<>(length());
        HashSet<Long> circulars = new HashSet<>();
        convertToMap(this, arrayList, circulars, filter, extra);
        circulars.clear();
        return arrayList;
    }

    @Override
    public HashMap<String, Object> toMap(MapFilter filter, Object extra) {
        throw new UnsupportedOperationException("Array types are not yet supported for conversion to map. You should use toArray.");
    }
}
