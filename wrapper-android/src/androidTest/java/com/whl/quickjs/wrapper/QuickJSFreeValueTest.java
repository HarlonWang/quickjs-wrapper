package com.whl.quickjs.wrapper;

import android.util.Log;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import com.whl.quickjs.android.QuickJSLoader;

public class QuickJSFreeValueTest {

    private QuickJSContext context;

    @Before
    public void setup() {
        QuickJSLoader.init();
        QuickJSLoader.startRedirectingStdoutStderr("quickjs");
        context = QuickJSContext.create();
    }

    @After
    public void teardown() {
        context.destroy();
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

        String result = evaluate.stringify();

        assertEquals("{\"name\":\"hello\",\"age\":12,\"sex\":\"男\"}", result);

        evaluate.release();

        context.evaluate("var user = {};");

        JSObject user = (JSObject) context.getGlobalObject().getProperty("user");
        user.setProperty("name", "Jack");
        user.release();

        JSFunction function = (JSFunction) context.getGlobalObject().getProperty("test");
        JSObject result1 = (JSObject) function.call();
        JSObject result2 = (JSObject) function.call();

        String name = (String) result1.getProperty("name");
        assertEquals("hello", name);

        String name1 = (String) result1.getProperty("name");
        assertEquals("hello", name1);

        result2.release();
        result1.release();
        function.release();
    }

    @Test
    public void testState() {
        context.getGlobalObject().setProperty("setState", args -> {
            JSObject ret = (JSObject) args[0];
            Log.d("test", ret.toString());
            ret.release();
            return "test";
        });

        context.evaluate("setState({age: 12});");
    }

    @Test
    public void testGetProperty() {
        context.evaluate("var obj1 = {age: {age_a: 12}};");

        JSObject obj1 = (JSObject) context.getGlobalObject().getProperty("obj1");

        JSObject age = (JSObject) obj1.getProperty("age");
        JSObject age1 = (JSObject) obj1.getProperty("age");
        JSObject age2 = (JSObject) obj1.getProperty("age");
        JSObject age3 = (JSObject) obj1.getProperty("age");
//
        age.release();
        age1.release();
        age2.release();
        age3.release();

        obj1.release();
    }

    @Test
    public void funcArgsFreeTest() {
        // set console.log
        context.evaluate("var console = {};");
        JSObject console = (JSObject) context.getGlobalObject().getProperty("console");
        console.setProperty("log", new JSCallFunction() {
            @Override
            public Object call(Object... args) {
                StringBuilder b = new StringBuilder();
                for (Object o: args) {
                    b.append(o == null ? "null" : o.toString());
                }

                Log.d("tiny-console", b.toString());
                return null;
            }
        });

        context.evaluate("var state = {};\n" +
                "\n" +
                "\n" +
                "function setState(data) {\n" +
                "\tstate = data;\n" +
                "}\n" +
                "\n" +
                "function getState() {\n" +
                "\treturn state;\n" +
                "}");


        context.evaluate("\n" +
                "function stateTest() {\n" +
                "\tsetState({count: 0});\n" +
                "\n" +
                "\tconsole.log(getState().count);\n" +
                "}\n");

        context.evaluate("stateTest();");
        context.evaluate("setState({count: 1});");
        context.evaluate("console.log(getState().count);");
        console.release();
    }

}
