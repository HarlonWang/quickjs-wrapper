package com.whl.quickjs.wrapper;

/**
 * Created by Harlon Wang on 2024/2/12.
 */
public class QuickJSFunction extends QuickJSObject implements JSFunction {

    private final long thisPointer;

    public QuickJSFunction(QuickJSContext context, long pointer, long thisPointer, boolean needToRelease) {
        super(context, pointer, needToRelease);
        this.thisPointer = thisPointer;
    }

    @Override
    public Object call(Object... args) {
        return getContext().call(this, thisPointer, args);
    }

}
