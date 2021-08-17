package com.whl.quickjs.wrapper.sample;

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

        evaluate.free();
    }

}
