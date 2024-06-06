package com.whl.quickjs.wrapper;

/**
 * Created by Harlon Wang on 2024/2/12.
 */
public class QuickJSFunction extends QuickJSObject implements JSFunction {

    private final long thisPointer;

    public QuickJSFunction(QuickJSContext context, long pointer, long thisPointer) {
        super(context, pointer);
        this.thisPointer = thisPointer;
    }

    @Override
    public Object call(Object... args) {
        checkRefCountIsZero();
        return getContext().call(this, thisPointer, args);
    }

    @Override
    public void callVoid(Object... args) {
        checkRefCountIsZero();
        Object ret = getContext().call(this, thisPointer, args);
        if (ret instanceof JSObject) {
            ((JSObject) ret).release();
        }
    }

}
