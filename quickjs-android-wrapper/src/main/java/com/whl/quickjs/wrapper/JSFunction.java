package com.whl.quickjs.wrapper;

public class JSFunction extends JSObject{

    public JSFunction(QuickJSContext context, long pointer) {
        super(context, pointer);
    }

    public Object call() {
        // todo getContext().call();
        return null;
    }

}
