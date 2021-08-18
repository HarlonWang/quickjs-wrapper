package com.whl.quickjs.wrapper.sample;

import android.util.Log;

import com.whl.quickjs.wrapper.JSCallFunction;
import com.whl.quickjs.wrapper.JSFunction;
import com.whl.quickjs.wrapper.JSObject;
import com.whl.quickjs.wrapper.QuickJSContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class JSFreeValueTest {

    private static final String TAG = "JSFreeValueTest";
    private QuickJSContext context;

    @Before
    public void setup() {
        context = QuickJSContext.create();
    }

    @After
    public void teardown() {
        context.destroyContext();
    }

    @Test
    public void globalFreeTest() {
        JSObject globalObj = context.getGlobalObject();
        JSObject globalObj1 = context.getGlobalObject();
        JSObject globalObj2 = context.getGlobalObject();
        JSObject globalObj3 = context.getGlobalObject();
        JSObject globalObj4 = context.getGlobalObject();


        globalObj.setProperty("name", "Jack");
        assertEquals("Jack", globalObj.getProperty("name"));



        globalObj1.setProperty("age", 12);

        assertEquals(12, globalObj4.getProperty("age"));
    }

    @Test
    public void evalTestFree() {
        JSObject evaluate = (JSObject) context.evaluate("function test() {\n" +
                "\treturn {name: \"hello\", age: 12, sex: \"男\"};\n" +
                "}\n" +
                "\n" +
                "test();");

        String result = evaluate.toString();

        assertEquals("{\"name\":\"hello\",\"age\":12,\"sex\":\"男\"}", result);

        context.evaluate("var user = {};");

        JSObject user = (JSObject) context.getGlobalObject().getProperty("user");
        user.setProperty("name", "Jack");

        JSFunction function = (JSFunction) context.getGlobalObject().getProperty("test");
        JSObject result1 = (JSObject) context.call(function, context.getGlobalObject());
        JSObject result2 = (JSObject) context.call(function, context.getGlobalObject());

        String name = (String) result1.getProperty("name");
        assertEquals("hello", name);

        String name1 = (String) result1.getProperty("name");
        assertEquals("hello", name1);

        // evaluate.free();
    }

    @Test
    public void testState() {
        context.getGlobalObject().setProperty("setState", new JSCallFunction() {
            @Override
            public Object call(Object... args) {
                Log.d("test", args[0].toString());
                return "test";
            }
        });

        context.evaluate("setState({age: 12});");
    }

    @Test
    public void testGetProperty() {
        context.evaluate("var obj1 = {age: {age_a: 12}};");

        JSObject obj1 = (JSObject) context.getGlobalObject().getProperty("obj1");

        JSObject age = (JSObject) obj1.getProperty("age");
        JSObject age1 = (JSObject) obj1.getProperty("age");
//
//        age1.free();
//        age2.free();
//        age3.free();
    }

}
