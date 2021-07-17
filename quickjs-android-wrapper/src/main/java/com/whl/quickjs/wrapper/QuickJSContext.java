package com.whl.quickjs.wrapper;

public class QuickJSContext {

    static {
        System.loadLibrary("quickjs-android-wrapper");
    }

    public native Object evaluate(String script, String fileName, int evalFlag);

}
