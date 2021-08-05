package com.whl.quickjs.wrapper.sample;

import android.util.Log;

import com.whl.quickjs.wrapper.JSArray;
import com.whl.quickjs.wrapper.JSFunction;
import com.whl.quickjs.wrapper.JSObject;
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
        JSObject globalObject = context.getGlobalObject();
        int a = (int) globalObject.getProperty("a");
        Log.d(TAG, "a = " + a);
    }

    @Test
    public void getPropertyStringTest() {
        QuickJSContext context = QuickJSContext.create();
        context.evaluate("var a = \"string test\";");
        JSObject globalObject = context.getGlobalObject();
        String a = (String) globalObject.getProperty("a");
        Log.d(TAG, "a = " + a);
    }

    @Test
    public void getPropertyFunctionTest() {
        QuickJSContext context = QuickJSContext.create();
        context.evaluate("function test(name) {\n" +
                "\treturn \"hello, \" + name;\n" +
                "}");
        JSObject globalObject = context.getGlobalObject();
        JSObject func = (JSObject) globalObject.getProperty("test");
        Log.d(TAG, "func = " + func.getPointer());

        String returnRet = (String) context.call(func, globalObject, 1, null);
        Log.d(TAG, "returnRet = " + returnRet);

        context.destroyContext();
    }

    @Test
    public void getJSArrayTest() {
        QuickJSContext context = QuickJSContext.create();
        JSArray ret = (JSArray) context.evaluate("function test(name) {\n" +
                "\treturn [1, 2, name];\n" +
                "}\n" +
                "\n" +
                "test(3);");
        Log.d(TAG, "ret = " + ret.get(2));

        context.destroyContext();
    }

    @Test
    public void getJSFunctionTest() {
        QuickJSContext context = QuickJSContext.create();
        context.evaluate("function test(name) {\n" +
                "\treturn \"hello, \" + name;\n" +
                "}");
        JSObject globalObject = context.getGlobalObject();
        JSFunction func = (JSFunction) globalObject.getProperty("test");
        Log.d(TAG, "func: " + func.getPointer());
        Object result = context.call(func, globalObject, 1, null);
        Log.d(TAG, "result: " + result);
    }

}
