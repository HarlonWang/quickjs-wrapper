package com.whl.quickjs.android;

/**
 * Created by Harlon Wang on 2022/8/12.
 */
public final class QuickJSLoader {

    public static void init() {
        System.loadLibrary("quickjs-android-wrapper");
    }

}
