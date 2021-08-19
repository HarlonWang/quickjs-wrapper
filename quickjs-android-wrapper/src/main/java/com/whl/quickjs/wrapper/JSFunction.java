package com.whl.quickjs.wrapper;

public class JSFunction extends JSObject{

    public JSFunction(QuickJSContext context, long pointer) {
        super(context, pointer);
    }

    // todo getContext().call();
//    public Object call() {
//        return null;
//    }

}
