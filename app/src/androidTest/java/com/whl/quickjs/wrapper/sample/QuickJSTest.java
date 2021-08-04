package com.whl.quickjs.wrapper.sample;

import android.util.Log;

import com.whl.quickjs.wrapper.QuickJSContext;

import org.junit.Test;

public class QuickJSTest {

    @Test
    public void createQuickJSContext() {
        QuickJSContext.create();
    }

    @Test
    public void evalReturnTypeTest() {
        QuickJSContext context = QuickJSContext.create();
        Log.d("QuickJSTest", context.evaluate("true;").toString());
        Log.d("QuickJSTest", context.evaluate("false;").toString());
        Log.d("QuickJSTest", context.evaluate("1123;").toString());
        Log.d("QuickJSTest", context.evaluate("1.1231;").toString());
        Log.d("QuickJSTest", context.evaluate("\"hello wrapper\";").toString());
    }

}
