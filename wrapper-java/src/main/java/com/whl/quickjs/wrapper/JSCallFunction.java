package com.whl.quickjs.wrapper;

public abstract class JSCallFunction {

    private int callbackId;

    public JSCallFunction(QuickJSContext context) {
        this.callbackId = context.nextCallbackId();
    }

    public void setCallbackId(int callbackId) {
        this.callbackId = callbackId;
    }

    public int getCallbackId() {
        return callbackId;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof JSCallFunction)) return false;
        JSCallFunction other = (JSCallFunction) obj;
        return this.callbackId == other.callbackId;
    }

    @Override
    public int hashCode() {
        return callbackId;
    }

    public abstract Object call(Object... args);
}
