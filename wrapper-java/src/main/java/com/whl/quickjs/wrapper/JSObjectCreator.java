package com.whl.quickjs.wrapper;

/**
 * Created by Harlon Wang on 2024/2/15.
 */
public interface JSObjectCreator {
    JSObject newObject(QuickJSContext context, long pointer, boolean needToRelease);
    JSArray newArray(QuickJSContext context, long pointer, boolean needToRelease);
    JSFunction newFunction(QuickJSContext context, long pointer, long thisPointer, boolean needToRelease);
}
