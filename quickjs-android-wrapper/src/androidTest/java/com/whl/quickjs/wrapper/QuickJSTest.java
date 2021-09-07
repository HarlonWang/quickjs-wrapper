package com.whl.quickjs.wrapper;

import android.util.Log;
import org.junit.Test;

import static org.junit.Assert.*;

public class QuickJSTest {

    @Test
    public void createQuickJSContextTest() {
        QuickJSContext.create();
    }

    @Test
    public void destroyQuickJSContextTest() {
        QuickJSContext context = QuickJSContext.create();
        context.evaluate("var a = 123;");

        JSObject gloObj = context.getGlobalObject();
        gloObj.release();

        JSObject globalObject = context.getGlobalObject();
        assertEquals(123, globalObject.getProperty("a"));
        context.destroyContext();
    }

    @Test
    public void evalReturnTypeTest() {
        QuickJSContext context = QuickJSContext.create();
        assertEquals(true, context.evaluate("true;"));
        assertEquals(false, context.evaluate("false;"));
        assertEquals(123, context.evaluate("123;"));
        assertEquals(1.23, context.evaluate("1.23;"));
        assertEquals("hello wrapper", context.evaluate("\"hello wrapper\";"));
    }

    @Test
    public void getPropertiesTest() {
        QuickJSContext context = QuickJSContext.create();
        context.evaluate("var intValue = 1;\n" +
                "var doubleValue = 1.23;\n" +
                "var stringValue = \"hi Jack\";\n" +
                "var booleanValue = true;\n" +
                "\n" +
                "function testFunc(name) {\n" +
                "\treturn \"hello, \" + name;\n" +
                "}");
        JSObject globalObject = context.getGlobalObject();
        assertEquals(1, globalObject.getProperty("intValue"));
        assertEquals(1.23, globalObject.getProperty("doubleValue"));
        assertEquals("hi Jack", globalObject.getProperty("stringValue"));
        assertEquals(true, globalObject.getProperty("booleanValue"));
        JSFunction function = (JSFunction) globalObject.getProperty("testFunc");
        assertEquals("hello, yonglan-whl", context.call(function, globalObject, "yonglan-whl"));
    }

    @Test
    public void getJSArrayTest() {
        QuickJSContext context = QuickJSContext.create();
        JSArray ret = (JSArray) context.evaluate("function test(value) {\n" +
                "\treturn [1, 2, value];\n" +
                "}\n" +
                "\n" +
                "test(3);");
        assertEquals(3, ret.get(2));
    }

    @Test
    public void JSFunctionArgsTest() {
        QuickJSContext context = QuickJSContext.create();
        context.evaluate("function test(intValue, stringValue, doubleValue, booleanValue) {\n" +
                "\treturn \"hello, \" + intValue + stringValue + doubleValue + booleanValue;\n" +
                "}");
        JSObject globalObject = context.getGlobalObject();
        JSFunction func = (JSFunction) globalObject.getProperty("test");
        assertEquals("hello, 1string123.11true", context.call(func, globalObject, 1, "string", 123.11, true));
    }

    @Test
    public void JSFunctionNullArgsTest() {
        QuickJSContext context = QuickJSContext.create();
        context.evaluate("function test(arg1, arg2, arg3) {\n" +
                "\treturn \"hello, \" + arg1 + arg2 + arg3;\n" +
                "}");
        JSObject globalObject = context.getGlobalObject();
        JSFunction func = (JSFunction) globalObject.getProperty("test");
        assertEquals("hello, undefined-13", context.call(func, globalObject, null, -1, 3));
    }

    @Test
    public void JSFunctionArgsTestWithUnSupportType() {
        QuickJSContext context = QuickJSContext.create();
        context.evaluate("function test(name) {\n" +
                "\treturn \"hello, \" + name;\n" +
                "}");
        JSObject globalObject = context.getGlobalObject();
        JSFunction func = (JSFunction) globalObject.getProperty("test");
        try {
            context.call(func, globalObject, new int[]{1, 2});
            fail();
        } catch (Exception e) {
            assertTrue(e.toString().contains("Unsupported Java type"));
        }

    }

    @Test
    public void setPropertiesTest() {
        QuickJSContext context = QuickJSContext.create();
        JSObject globalObj = context.getGlobalObject();
        globalObj.setProperty("stringValue", "hello test");
        globalObj.setProperty("intValue", 123);
        globalObj.setProperty("doubleValue", 123.11);
        globalObj.setProperty("booleanValue", true);
        globalObj.setProperty("functionValue", new JSCallFunction() {
            @Override
            public Object call(Object... args) {
                System.out.println("arg = " + args.length);
                return "call back";
            }
        });
        assertEquals("hello test", context.evaluate("stringValue;"));
        assertEquals(123, context.evaluate("intValue;"));
        assertEquals(123.11, context.evaluate("doubleValue;"));
        assertEquals(true, context.evaluate("booleanValue;"));
        assertEquals("call back", context.evaluate("functionValue();"));

        context.destroyContext();
    }

    @Test
    public void setConsoleLogTest() {
        QuickJSContext context = QuickJSContext.create();
        context.evaluate("var console = {};");
        JSObject console = (JSObject) context.getGlobalObject().getProperty("console");
        console.setProperty("log", new JSCallFunction() {
            @Override
            public Object call(Object... args) {
                StringBuilder b = new StringBuilder();
                for (Object o: args) {
                    b.append(o == null ? "null" : o.toString());
                }

                assertEquals("123", b.toString());
                return null;
            }
        });

        context.evaluate("console.log(123)");
    }

    @Test
    public void arrowFuncTest() {
        // set console.log
        QuickJSContext context = QuickJSContext.create();
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


        context.evaluate("function test(index) {\n" +
                "\tconsole.log(index);\n" +
                "}\n" +
                "\n" +
                "function render() {\n" +
                "\n" +
                "\tvar index = 123;\n" +
                "\tvar invokeTest = () => {\n" +
                "\t\ttest(index);\n" +
                "\t}\n" +
                "\n" +
                "\treturn {\n" +
                "\t\tfunc: invokeTest\n" +
                "\t};\n" +
                "}");

        JSObject jsObj = (JSObject) context.evaluate("render();");
        JSFunction jsFunction = (JSFunction) jsObj.getProperty("func");
        context.call(jsFunction, jsObj);
    }

    @Test
    public void setPropertyWithJSObjectTest() {
        QuickJSContext context = QuickJSContext.create();
        context.evaluate("var test = {count: 0};");
        context.getGlobalObject().setProperty("test1", context.getGlobalObject().getProperty("test"));

        assertEquals("{\"count\":0}", context.getGlobalObject().getProperty("test1").toString());
    }

    @Test
    public void jsonParseTest() {
        String text = "{\"phoneNumber\":\"呼叫 18505815627\",\"leadsId\":\"270\",\"leadsBizId\":\"xxx\",\"options\":[{\"type\":\"aliyun\",\"avatarUrl\":\"https://gw.alicdn.com/tfs/TB1BYz0vpYqK1RjSZLeXXbXppXa-187-187.png\",\"personName\":\"老板\",\"storeName\":\"小店名称\",\"title\":\"智能办公电话\",\"content\":\"免费拨打\"},{\"type\":\"direct\",\"title\":\"普通电话\",\"content\":\"运营商拨打\"}]}\n";

        QuickJSContext context = QuickJSContext.create();
        JSObject result = context.parseJSON(text);
        assertEquals("270", result.getProperty("leadsId"));

        context.destroyContext();
    }

}
