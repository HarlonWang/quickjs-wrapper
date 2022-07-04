package com.whl.quickjs.wrapper;

import android.text.TextUtils;
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
    public void setPropertyTest() {
        QuickJSContext context = QuickJSContext.create();
        JSObject globalObj = context.getGlobalObject();
        JSObject obj1 = context.createNewJSObject();
        obj1.setProperty("stringProperty", "hello");
        obj1.setProperty("intProperty", 1);
        obj1.setProperty("doubleProperty", 0.1);
        obj1.setProperty("booleanProperty", true);
        obj1.setProperty("functionProperty", (JSCallFunction) args -> args[0] + "Wang");
        obj1.setProperty("nullProperty", (String) null);
        globalObj.setProperty("obj1", obj1);

        assertEquals("hello", context.evaluate("obj1.stringProperty;"));
        assertEquals(1, context.evaluate("obj1.intProperty;"));
        assertEquals(0.1, context.evaluate("obj1.doubleProperty;"));
        assertEquals(true, context.evaluate("obj1.booleanProperty;"));
        assertEquals("HarlonWang", context.evaluate("obj1.functionProperty(\"Harlon\");"));
        assertNull(context.evaluate("obj1.nullProperty;"));

        context.destroyContext();
    }

    @Test
    public void getPropertyTest() {
        QuickJSContext context = QuickJSContext.create();
        context.evaluate("var obj1 = {\n" +
                "\tstringProperty: 'hello',\n" +
                "\tintProperty: 1,\n" +
                "\tdoubleProperty: 0.1,\n" +
                "\tbooleanProperty: true,\n" +
                "\tnullProperty: null,\n" +
                "\tfunctionProperty: (name) => { return name + 'Wang'; }\n" +
                "}");
        JSObject globalObject = context.getGlobalObject();
        JSObject obj1 = globalObject.getJSObjectProperty("obj1");
        assertEquals("hello", obj1.getProperty("stringProperty"));
        assertEquals(1, obj1.getProperty("intProperty"));
        assertEquals(0.1, obj1.getProperty("doubleProperty"));
        assertEquals(true, obj1.getProperty("booleanProperty"));
        assertNull(obj1.getProperty("nullProperty"));
        assertEquals("HarlonWang", obj1.getJSFunctionProperty("functionProperty").call("Harlon"));

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
        context.getGlobalObject().setProperty("test1", (JSObject) context.getGlobalObject().getProperty("test"));

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
        context.getGlobalObject().setProperty("test", (JSCallFunction) args -> context.parseJSON(text));

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

    @Test
    public void testReturnParseJSON() {
        QuickJSContext context = QuickJSContext.create();
        context.getGlobalObject().setProperty("test", (JSCallFunction) args -> context.parseJSON("{}"));
        context.evaluate("test();test();test();");
        context.destroyContext();
    }

    @Test
    public void testCreateNewJSObject() {
        QuickJSContext context = QuickJSContext.create();
        JSObject jsObject = context.createNewJSObject();
        jsObject.setProperty("name", context.createNewJSObject());
        JSFunction function = (JSFunction) context.evaluate("var test = (arg) => { return arg; };test;");
        Object result = function.call(jsObject);
        assertEquals("{ name: {  } }", result.toString());
        context.destroyContext();
    }

    @Test
    public void testCreateNewJSArray() {
        QuickJSContext context = QuickJSContext.create();
        JSArray jsArray = context.createNewJSArray();
        jsArray.set(11, 0);
        jsArray.set("222", 1);
        JSFunction function = (JSFunction) context.evaluate("var test = (arg) => { return arg; };test;");
        Object result = function.call(jsArray);
        assertEquals("[ 11, 222 ]", result.toString());
        context.destroyContext();
    }

    @Test
    public void testFormatToString() {
        QuickJSContext context = QuickJSContext.create();
        String result = (String) context.evaluate("__format_string(this);");
        assertEquals(result, "{ console: { log: function log() }, __format_string: function __format_string() }");
        assertEquals(context.getGlobalObject().toString(), "{ console: { log: function log() }, __format_string: function __format_string() }");
        context.destroyContext();
    }

    @Test
    public void testNotAFunction() {
        QuickJSContext context = QuickJSContext.create();
        context.setExceptionHandler(error -> assertTrue(error.contains("'a' is not a function")));
        context.evaluate("var a = 1; a();");
        context.destroyContext();
    }

    @Test
    public void testNotAFunction2() {
        QuickJSContext context = QuickJSContext.create();

        QuickJSContext.ExceptionHandler loc0Assert = error -> assertTrue(error.contains("'b' is not a function"));
        context.setExceptionHandler(loc0Assert);
        context.evaluate("function a() {\n" +
                "\tvar b = {};\n" +
                "\tb();\n" +
                "}\n" +
                "a();");

        QuickJSContext.ExceptionHandler loc1Assert = error -> assertTrue(error.contains("'d' is not a function"));
        context.setExceptionHandler(loc1Assert);
        context.evaluate("function a() {\n" +
                "\tvar b = {}; var d = 1;\n" +
                "\td();\n" +
                "}\n" +
                "a();");

        QuickJSContext.ExceptionHandler loc2Assert = error -> assertTrue(error.contains("'c' is not a function"));
        context.setExceptionHandler(loc2Assert);
        context.evaluate("function a() {\n" +
                "\tvar b = {}; var d = 1; var c = 1;\n" +
                "\tc();\n" +
                "}\n" +
                "a();");

        QuickJSContext.ExceptionHandler loc3Assert = error -> assertTrue(error.contains("'d' is not a function"));
        context.setExceptionHandler(loc3Assert);
        context.evaluate("function a() {\n" +
                "\tvar b = {}; var d = 1; var c = 1;var d = [];\n" +
                "\td();\n" +
                "}\n" +
                "a();");

        QuickJSContext.ExceptionHandler loc8Assert = error -> assertTrue(error.contains("'e' is not a function"));
        context.setExceptionHandler(loc8Assert);
        context.evaluate("function a() {\n" +
                "\tvar b = {}; var d = 1; var c = 1;var d = []; var e = {};\n" +
                "\te();\n" +
                "}\n" +
                "a();");

        QuickJSContext.ExceptionHandler arg0Assert = error -> assertTrue(error.contains("'aa' is not a function"));
        context.setExceptionHandler(arg0Assert);
        context.evaluate("function a(aa) {\n" +
                "\taa();\n" +
                "}\n" +
                "a();");

        QuickJSContext.ExceptionHandler arg1Assert = error -> assertTrue(error.contains("'bb' is not a function"));
        context.setExceptionHandler(arg1Assert);
        context.evaluate("function a(aa, bb) {\n" +
                "\tbb();\n" +
                "}\n" +
                "a();");

        QuickJSContext.ExceptionHandler arg2Assert = error -> assertTrue(error.contains("'cc' is not a function"));
        context.setExceptionHandler(arg2Assert);
        context.evaluate("function a(aa, bb, cc) {\n" +
                "\tcc();\n" +
                "}\n" +
                "a();");

        QuickJSContext.ExceptionHandler arg3Assert = error -> assertTrue(error.contains("'dd' is not a function"));
        context.setExceptionHandler(arg3Assert);
        context.evaluate("function a(aa, bb, cc, dd) {\n" +
                "\tdd();\n" +
                "}\n" +
                "a();");

        QuickJSContext.ExceptionHandler argAssert = error -> assertTrue(error.contains("'ee' is not a function"));
        context.setExceptionHandler(argAssert);
        context.evaluate("function a(aa, bb, cc, dd, ee) {\n" +
                "\tee();\n" +
                "}\n" +
                "a();");

        QuickJSContext.ExceptionHandler ref0Assert = error -> assertTrue(error.contains("'a' is not a function"));
        context.setExceptionHandler(ref0Assert);
        context.evaluate("function test (){var a = {}; function test1 () {a(); } test1();} test();");

        QuickJSContext.ExceptionHandler refAssert = error -> assertTrue(error.contains("'e' is not a function"));
        context.setExceptionHandler(refAssert);
        context.evaluate("function test (){var a = {}; var b = 1;var c = 1; var d = 1; var e = 1; function test1 () {b = a; c = a; d = e; c = b;e(); } test1();} test();");
    }

    @Test
    public void testNotAFunctionInPromise() {
        QuickJSContext context = QuickJSContext.create();
        context.setExceptionHandler(error -> assertTrue(error.contains("'[object Object]' is not a function")));
        context.evaluate("new Promise({name: 'a'});");
        context.destroyContext();
    }

    @Test
    public void testStackOverflowWithStackSize() {
        QuickJSContext context = QuickJSContext.create(1024);
        context.setExceptionHandler(error -> assertTrue(error.contains("stack overflow")));
        context.evaluate("function y(){}");
        context.destroyContext();
    }

    @Test
    public void testMissingFormalParameter() {
        QuickJSContext context = QuickJSContext.create();
        context.setExceptionHandler(error -> assertTrue(error.contains("missing formal parameter")));
        context.evaluate("function y(1){}");
        context.destroyContext();
    }

    @Test
    public void testStackSizeWithLimited() {
        QuickJSContext context = QuickJSContext.create(1024 * 512);
        context.setExceptionHandler(error -> assertTrue(error.contains("stack overflow")));
        context.evaluate("function y(){y();} y();");
        context.destroyContext();
    }

    public static class TestJava {
        @JSMethod
        public boolean test1(String message) {
            return TextUtils.equals("arg1", message);
        }

        @JSMethod
        public boolean test2(String message, int age) {
            return TextUtils.equals("arg1", message) && 18 == age;
        }

        @JSMethod
        public boolean testArray(JSArray array) {
            String arg1 = (String) array.get(0);
            int age = (int) array.get(1);
            return TextUtils.equals("arg1", arg1) && 18 == age;
        }

        @JSMethod
        public boolean testObject(JSObject jsObj) {
            return TextUtils.equals("{\"arg1\":\"a\",\"arg2\":18}", jsObj.stringify());
        }

        @JSMethod
        public boolean testAll(JSObject object, JSArray message, int age, String name) {
            return TextUtils.equals("{\"arg1\":\"a\",\"arg2\":18},[1,2,3],18,name", object.stringify() + "," + message.stringify() + "," + age + "," + name);
        }
    }

    @Test
    public void testAnnotationMethod() {
        QuickJSContext context = QuickJSContext.create();

        context.getGlobalObject().setProperty("test1", TestJava.class);
        assertTrue((boolean) context.evaluate("test1.test1('arg1');"));
        assertTrue((boolean) context.evaluate("test1.test2('arg1', 18);"));
        assertTrue((boolean) context.evaluate("test1.testArray(['arg1', 18]);"));
        assertTrue((boolean) context.evaluate("test1.testObject({'arg1':'a', 'arg2':18});"));
        assertTrue((boolean) context.evaluate("test1.testAll({'arg1':'a', 'arg2':18}, [1,2,3],18, 'name');"));

        context.getGlobalObject().setProperty("test2", TestJava.class);
        assertTrue((boolean) context.evaluate("test2.testObject({'arg1':'a', 'arg2':18});"));

        context.destroyContext();
    }

    // todo fix
//    @Test
//    public void testJSArraySetParseJSON() {
//        QuickJSContext context = QuickJSContext.create();
//        context.getGlobalObject().setProperty("getData", args -> {
//            JSArray jsArray = context.createNewJSArray();
//            JSObject jsObject = context.parseJSON("{\"name\": \"Jack\", \"age\": 33}");
//            jsArray.set(jsObject, 0);
//            // jsArray.set(context.parseJSON("{\"name\": \"Jack\", \"age\": 33}"), 1);
//            return jsArray;
//        });
//        context.evaluate("var array = getData();console.log(JSON.stringify(array));");
//        context.destroyContext();
//    }

}
