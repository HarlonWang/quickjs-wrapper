package com.whl.quickjs.wrapper;

import android.util.AndroidRuntimeException;
import android.util.Log;
import org.junit.Test;

import static org.junit.Assert.*;

public class QuickJSTest {

    @Test
    public void createQuickJSContextTest() {
        QuickJSContext context = QuickJSContext.create();
        context.destroyContext();
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

        context.destroyContext();
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
        assertEquals("hello, yonglan-whl", function.call("yonglan-whl"));

        context.destroyContext();
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

        context.destroyContext();
    }

    @Test
    public void JSFunctionArgsTest() {
        QuickJSContext context = QuickJSContext.create();
        context.evaluate("function test(intValue, stringValue, doubleValue, booleanValue) {\n" +
                "\treturn \"hello, \" + intValue + stringValue + doubleValue + booleanValue;\n" +
                "}");
        JSObject globalObject = context.getGlobalObject();
        JSFunction func = (JSFunction) globalObject.getProperty("test");
        assertEquals("hello, 1string123.11true", func.call(1, "string", 123.11, true));

        context.destroyContext();
    }

    @Test
    public void JSFunctionNullArgsTest() {
        QuickJSContext context = QuickJSContext.create();
        context.evaluate("function test(arg1, arg2, arg3) {\n" +
                "\treturn \"hello, \" + arg1 + arg2 + arg3;\n" +
                "}");
        JSObject globalObject = context.getGlobalObject();
        JSFunction func = (JSFunction) globalObject.getProperty("test");
        assertEquals("hello, undefined-13", func.call(null, -1, 3));

        context.destroyContext();
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
            func.call(new int[]{1, 2});
            fail();
        } catch (Exception e) {
            assertTrue(e.toString().contains("Unsupported Java type"));
        }

        context.destroyContext();
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

        context.destroyContext();
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
        jsFunction.call();

        context.destroyContext();
    }

    @Test
    public void setPropertyWithJSObjectTest() {
        QuickJSContext context = QuickJSContext.create();
        context.evaluate("var test = {count: 0};");
        context.getGlobalObject().setProperty("test1", context.getGlobalObject().getProperty("test"));

        assertEquals("{\"count\":0}", context.getGlobalObject().getJSObjectProperty("test1").stringify());
        context.destroyContext();
    }

    @Test
    public void jsonParseTest() {
        String text = "{\"phoneNumber\":\"呼叫 18505815627\",\"leadsId\":\"270\",\"leadsBizId\":\"xxx\",\"options\":[{\"type\":\"aliyun\",\"avatarUrl\":\"https://gw.alicdn.com/tfs/TB1BYz0vpYqK1RjSZLeXXbXppXa-187-187.png\",\"personName\":\"老板\",\"storeName\":\"小店名称\",\"title\":\"智能办公电话\",\"content\":\"免费拨打\"},{\"type\":\"direct\",\"title\":\"普通电话\",\"content\":\"运营商拨打\"}]}\n";

        QuickJSContext context = QuickJSContext.create();
        JSObject result = context.parseJSON(text);
        assertEquals("270", result.getProperty("leadsId"));

        context.getGlobalObject().setProperty("test", result);
        Log.d("__quickjs__", "123------------------" + context.getGlobalObject().getJSObjectProperty("test").stringify());


        context.destroyContext();
    }

    @Test
    public void jsonParseTest2() {
        String text = "{\"phoneNumber\":\"呼叫 18505815627\",\"leadsId\":\"270\",\"leadsBizId\":\"xxx\",\"options\":[{\"type\":\"aliyun\",\"avatarUrl\":\"https://gw.alicdn.com/tfs/TB1BYz0vpYqK1RjSZLeXXbXppXa-187-187.png\",\"personName\":\"老板\",\"storeName\":\"小店名称\",\"title\":\"智能办公电话\",\"content\":\"免费拨打\"},{\"type\":\"direct\",\"title\":\"普通电话\",\"content\":\"运营商拨打\"}]}\n";

        QuickJSContext context = QuickJSContext.create();
        JSFunction log = (JSFunction) context.evaluate("console.log");
        JSObject jsonObj = context.parseJSON(text);
        log.call(jsonObj);

        context.destroyContext();
    }

    @Test
    public void jsonParseTest3() {
        String text = "{\"phoneNumber\":\"呼叫 18505815627\",\"leadsId\":\"270\",\"leadsBizId\":\"xxx\",\"options\":[{\"type\":\"aliyun\",\"avatarUrl\":\"https://gw.alicdn.com/tfs/TB1BYz0vpYqK1RjSZLeXXbXppXa-187-187.png\",\"personName\":\"老板\",\"storeName\":\"小店名称\",\"title\":\"智能办公电话\",\"content\":\"免费拨打\"},{\"type\":\"direct\",\"title\":\"普通电话\",\"content\":\"运营商拨打\"}]}\n";
        QuickJSContext context = QuickJSContext.create();
        JSObject a = (JSObject) context.evaluate("var a = {}; a;");
        a.setProperty("test", context.parseJSON(text));
        context.evaluate("console.log(a.test.leadsId);");
        context.destroyContext();
    }

    @Test
    public void jsonParseTest4() {
        String text = "{\"phoneNumber\":\"呼叫 18505815627\",\"leadsId\":\"270\",\"leadsBizId\":\"xxx\",\"options\":[{\"type\":\"aliyun\",\"avatarUrl\":\"https://gw.alicdn.com/tfs/TB1BYz0vpYqK1RjSZLeXXbXppXa-187-187.png\",\"personName\":\"老板\",\"storeName\":\"小店名称\",\"title\":\"智能办公电话\",\"content\":\"免费拨打\"},{\"type\":\"direct\",\"title\":\"普通电话\",\"content\":\"运营商拨打\"}]}\n";
        QuickJSContext context = QuickJSContext.create();
        JSObject a = (JSObject) context.evaluate("var a = {b: {}}; a;");
        a.getJSObjectProperty("b").setProperty("test", context.parseJSON(text));
        context.evaluate("console.log(a.b.test.leadsId);");
        context.destroyContext();
    }

    @Test
    public void jsonParseTest5() {
        String text = "{\"phoneNumber\":\"呼叫 18505815627\",\"leadsId\":\"270\",\"leadsBizId\":\"xxx\",\"options\":[{\"type\":\"aliyun\",\"avatarUrl\":\"https://gw.alicdn.com/tfs/TB1BYz0vpYqK1RjSZLeXXbXppXa-187-187.png\",\"personName\":\"老板\",\"storeName\":\"小店名称\",\"title\":\"智能办公电话\",\"content\":\"免费拨打\"},{\"type\":\"direct\",\"title\":\"普通电话\",\"content\":\"运营商拨打\"}]}\n";
        QuickJSContext context = QuickJSContext.create();
        context.getGlobalObject().setProperty("test", new JSCallFunction() {
            @Override
            public Object call(Object... args) {
                JSObject obj = context.parseJSON(text);
                obj.dupValue();
                return obj;
            }
        });

        context.evaluate("var a = test(); console.log(a);");
        context.destroyContext();
    }

    @Test
    public void testFlat() {
        QuickJSContext context = QuickJSContext.create();
        context.evaluate("let a = [1,[2,3]];  \n" +
                "a = a.flat();\n" +
                "console.log(a);");

        context.destroyContext();
    }

    @Test
    public void testClass() {
        QuickJSContext context = QuickJSContext.create();
        context.evaluate("class User {\n" +
                "\tconstructor() {\n" +
                "\t\tthis.name = \"HarlonWang\";\n" +
                "\t}\n" +
                "}\n" +
                "\n" +
                "var user = new User();\n" +
                "console.log(user.name);");
        context.destroyContext();
    }

    @Test
    public void testGetOwnPropertyNames() {
        QuickJSContext context = QuickJSContext.create();
        context.evaluate("var a = {age: 1, ff: () => {}};");
        JSArray array = context.getGlobalObject().getJSObjectProperty("a").getOwnPropertyNames();
        for (int i = 0; i < ((JSArray) array).length(); i++) {
            String item = (String) ((JSArray) array).get(i);
            if (i == 0) {
                assertEquals("age", item);
            } else {
                assertEquals("ff", item);
            }
        }

        context.destroyContext();
    }

    @Test
    public void testDumpStackError() {
        QuickJSContext context = QuickJSContext.create();
        try {
            context.evaluate("var a = 1; a();");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("not a function"));
        }
        context.destroyContext();
    }

    @Test
    public void testPromise() {
        QuickJSContext context = QuickJSContext.create();
        context.evaluate("const promiseA = new Promise( (resolutionFunc,rejectionFunc) => {\n" +
                "    resolutionFunc(777);\n" +
                "});\n" +
                "// 这时，\"promiseA\" 已经被敲定了。\n" +
                "promiseA.then( (val) => console.log(\"asynchronous logging has val:\",val) );\n" +
                "console.log(\"immediate logging\");\n" +
                "\n" +
                "// produces output in this order:\n" +
                "// immediate logging\n" +
                "// asynchronous logging has val: 777\n" +
                "\n" +
                "\n" +
                "const promiseB = new Promise( (resolutionFunc,rejectionFunc) => {\n" +
                "    resolutionFunc(888);\n" +
                "});\n" +
                "\n" +
                "promiseB.then( (val) => console.log(\"asynchronous logging has val:\",val) );\n");

        int err;
        for(;;) {
            err = context.executePendingJob();
            if (err <= 0) {
                if (err < 0) {
                    throw new AndroidRuntimeException("Promise execute exception!");
                }
                break;
            }
        }

        context.destroyContext();
    }

    @Test
    public void testPromise2() {
        QuickJSContext context = QuickJSContext.create();
        context.evaluate("    var defer =\n" +
                "        'function' == typeof Promise\n" +
                "            ? Promise.resolve().then.bind(Promise.resolve())\n" +
                "            : setTimeout;\n" +
                "    defer(() => {console.log('哈哈');});");

        int err;
        for(;;) {
            err = context.executePendingJob();
            if (err <= 0) {
                if (err < 0) {
                    throw new AndroidRuntimeException("Promise execute exception!");
                }
                break;
            }
        }

        context.destroyContext();
    }

    @Test
    public void testProxy() {
        QuickJSContext context = QuickJSContext.create();

        context.evaluate("const handler = {\n" +
                "    get: function(obj, prop) {\n" +
                "        return prop in obj ? obj[prop] : 37;\n" +
                "    }\n" +
                "};\n" +
                "\n" +
                "const p = new Proxy({}, handler);\n" +
                "p.a = 1;\n" +
                "p.b = undefined;\n" +
                "\n" +
                "console.log(p.a, p.b);      // 1, undefined\n" +
                "console.log('c' in p, p.c); // false, 37");

        context.destroyContext();
    }

    @Test
    public void testQuickJSException() {
        QuickJSContext context = QuickJSContext.create();
        context.setExceptionHandler(error -> Log.d("QuickJSException", error));

        context.evaluate("console.log(a);");

        context.destroyContext();
    }

}
