package com.whl.quickjs.wrapper.sample;

import android.util.Log;

import com.whl.quickjs.wrapper.JSValue;
import com.whl.quickjs.wrapper.QuickJSContext;

import org.junit.Test;

public class QuickJSTest {

    private static final String TAG = "QuickJSTest";

    @Test
    public void createQuickJSContext() {
        QuickJSContext.create();
    }

    @Test
    public void evalReturnTypeTest() {
        QuickJSContext context = QuickJSContext.create();
        Log.d(TAG, context.evaluate("true;").toString());
        Log.d(TAG, context.evaluate("false;").toString());
        Log.d(TAG, context.evaluate("1123;").toString());
        Log.d(TAG, context.evaluate("1.1231;").toString());
        Log.d(TAG, context.evaluate("\"hello wrapper\";").toString());
    }

    @Test
    public void getPropertyIntTest() {
        QuickJSContext context = QuickJSContext.create();
        context.evaluate("var a = 1;");
        JSValue globalObject = context.getGlobalObject();
        int a = (int) globalObject.getProperty("a");
        Log.d(TAG, "a = " + a);
    }

    @Test
    public void getPropertyStringTest() {
        QuickJSContext context = QuickJSContext.create();
        context.evaluate("var a = \"string test\";");
        JSValue globalObject = context.getGlobalObject();
        String a = (String) globalObject.getProperty("a");
        Log.d(TAG, "a = " + a);
    }

    @Test
    public void getPropertyFunctionTest() {
        QuickJSContext context = QuickJSContext.create();
        context.evaluate("function test(name) {\n" +
                "\treturn \"hello, \" + name;\n" +
                "}");
        JSValue globalObject = context.getGlobalObject();
        JSValue func = (JSValue) globalObject.getProperty("test");
        Log.d(TAG, "func = " + func.getValue());

        String returnRet = (String) context.call(func, globalObject, 1, null);
        Log.d(TAG, "returnRet = " + returnRet);
    }

}
