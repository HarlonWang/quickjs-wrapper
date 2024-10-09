package com.whl.quickjs.wrapper;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.*;
import androidx.test.core.app.ApplicationProvider;
import com.whl.quickjs.android.QuickJSLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuickJSTest {

    static final class LogcatConsole implements QuickJSContext.Console {

        private final String tag;

        public LogcatConsole(String tag) {
            this.tag = tag;
        }

        @Override
        public void log(String info) {
            Log.d(tag, info);
        }

        @Override
        public void info(String info) {
            Log.i(tag, info);
        }

        @Override
        public void warn(String info) {
            Log.w(tag, info);
        }

        @Override
        public void error(String info) {
            Log.e(tag, info);
        }
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void initSo() {
        QuickJSLoader.init();
        QuickJSLoader.startRedirectingStdoutStderr("QuickJSTest");
    }

    public static QuickJSContext createContext() {
        QuickJSContext context = QuickJSContext.create();
        context.setConsole(new LogcatConsole("console-test"));
        return context;
    }

    @Test
    public void createQuickJSContextTest() {
        QuickJSContext context = createContext();
        context.destroy();
    }

    @Test
    public void destroyQuickJSContextTest() {
        QuickJSContext context = createContext();
        context.evaluate("var a = 123;");

        JSObject globalObject = context.getGlobalObject();
        assertEquals(123, globalObject.getProperty("a"));
        context.destroy();
    }

    @Test
    public void setPropertyTest() {
        QuickJSContext context = createContext();
        JSObject globalObj = context.getGlobalObject();
        JSObject obj1 = context.createNewJSObject();
        obj1.setProperty("stringProperty", "hello");
        obj1.setProperty("intProperty", 1);
        obj1.setProperty("doubleProperty", 0.1);
        obj1.setProperty("longProperty", 1686026400093L);
        obj1.setProperty("booleanProperty", true);
        obj1.setProperty("functionProperty", (JSCallFunction) args -> args[0] + "Wang");
        obj1.setProperty("nullProperty", (String) null);
        globalObj.setProperty("obj1", obj1);
        obj1.release();

        assertEquals("hello", context.evaluate("obj1.stringProperty;"));
        assertEquals(1, context.evaluate("obj1.intProperty;"));
        assertEquals(0.1, context.evaluate("obj1.doubleProperty;"));
        assertEquals(1686026400093L, context.evaluate("obj1.longProperty;"));
        assertEquals(true, context.evaluate("obj1.booleanProperty;"));
        assertEquals("HarlonWang", context.evaluate("obj1.functionProperty(\"Harlon\");"));
        assertNull(context.evaluate("obj1.nullProperty;"));

        context.destroy();
    }

    @Test
    public void getPropertyTest() {
        QuickJSContext context = createContext();
        context.evaluate("var obj1 = {\n" +
                "\tstringProperty: 'hello',\n" +
                "\tintProperty: 1,\n" +
                "\tdoubleProperty: 0.1,\n" +
                "\tlongProperty: 1686026400093n,\n" +
                "\tbooleanProperty: true,\n" +
                "\tnullProperty: null,\n" +
                "\tfunctionProperty: (name) => { return name + 'Wang'; }\n" +
                "}");
        JSObject globalObject = context.getGlobalObject();
        JSObject obj1 = globalObject.getJSObject("obj1");
        assertEquals("hello", obj1.getString("stringProperty"));
        assertEquals(1, obj1.getProperty("intProperty"));
        assertEquals(0.1, obj1.getProperty("doubleProperty"));
        assertEquals(1686026400093L, obj1.getProperty("longProperty"));
        assertEquals(true, obj1.getProperty("booleanProperty"));
        assertNull(obj1.getProperty("nullProperty"));
        JSFunction fn = obj1.getJSFunction("functionProperty");
        assertEquals("HarlonWang", fn.call("Harlon"));

        fn.release();
        obj1.release();

        context.destroy();
    }

    @Test
    public void getJSArrayTest() {
        QuickJSContext context = createContext();
        JSArray ret = (JSArray) context.evaluate("function test(value) {\n" +
                "\treturn [1, 2, value];\n" +
                "}\n" +
                "\n" +
                "test(3);");
        assertEquals(3, ret.get(2));
        ret.release();

        context.destroy();
    }

    @Test
    public void JSFunctionArgsTest() {
        QuickJSContext context = createContext();
        context.evaluate("function test(intValue, stringValue, doubleValue, booleanValue) {\n" +
                "\treturn \"hello, \" + intValue + stringValue + doubleValue + booleanValue;\n" +
                "}");
        JSObject globalObject = context.getGlobalObject();
        JSFunction func = (JSFunction) globalObject.getProperty("test");
        assertEquals("hello, 1string123.11true", func.call(1, "string", 123.11, true));
        func.release();

        context.destroy();
    }

    @Test
    public void JSFunctionNullArgsTest() {
        QuickJSContext context = createContext();
        context.evaluate("function test(arg1, arg2, arg3) {\n" +
                "\treturn \"hello, \" + arg1 + arg2 + arg3;\n" +
                "}");
        JSObject globalObject = context.getGlobalObject();
        JSFunction func = (JSFunction) globalObject.getProperty("test");
        assertEquals("hello, undefined-13", func.call(null, -1, 3));
        func.release();

        context.destroy();
    }

    @Test
    public void JSFunctionArgsTestWithUnSupportType() {
        QuickJSContext context = createContext();
        context.evaluate("function test(name) {\n" +
                "\treturn \"hello, \" + name;\n" +
                "}");
        JSObject globalObject = context.getGlobalObject();
        JSFunction func = (JSFunction) globalObject.getProperty("test");
        try {
            func.call(new int[]{1, 2});
            fail();
        } catch (Exception e) {
            func.release();
            assertTrue(e.toString().contains("Unsupported Java type"));
        }

        context.destroy();
    }

    @Test
    public void arrowFuncTest() {
        QuickJSContext context = createContext();
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

        jsFunction.release();
        jsObj.release();

        context.destroy();
    }

    @Test
    public void setPropertyWithJSObjectTest() {
        QuickJSContext context = createContext();
        context.evaluate("var test = {count: 0};");
        JSObject test = context.getGlobalObject().getJSObject("test");
        context.getGlobalObject().setProperty("test1", test);

        JSObject test1 = context.getGlobalObject().getJSObject("test1");

        assertEquals("{\"count\":0}", test1.stringify());

        test.release();
        test1.release();

        context.destroy();
    }

    @Test
    public void jsonParseTest() {
        String text = "{\"phoneNumber\":\"呼叫 18505815627\",\"leadsId\":\"270\",\"leadsBizId\":\"xxx\",\"options\":[{\"type\":\"aliyun\",\"avatarUrl\":\"https://gw.alicdn.com/tfs/TB1BYz0vpYqK1RjSZLeXXbXppXa-187-187.png\",\"personName\":\"老板\",\"storeName\":\"小店名称\",\"title\":\"智能办公电话\",\"content\":\"免费拨打\"},{\"type\":\"direct\",\"title\":\"普通电话\",\"content\":\"运营商拨打\"}]}";

        QuickJSContext context = createContext();
        JSObject result = context.parseJSON(text);
        assertEquals("270", result.getProperty("leadsId"));

        context.getGlobalObject().setProperty("test", result);

        result.release();

        JSObject test = context.getGlobalObject().getJSObject("test");
        assertEquals(text, test.stringify());
        test.release();

        context.destroy();
    }

    @Test
    public void jsonParseTest3() {
        String text = "{\"phoneNumber\":\"呼叫 18505815627\",\"leadsId\":\"270\",\"leadsBizId\":\"xxx\",\"options\":[{\"type\":\"aliyun\",\"avatarUrl\":\"https://gw.alicdn.com/tfs/TB1BYz0vpYqK1RjSZLeXXbXppXa-187-187.png\",\"personName\":\"老板\",\"storeName\":\"小店名称\",\"title\":\"智能办公电话\",\"content\":\"免费拨打\"},{\"type\":\"direct\",\"title\":\"普通电话\",\"content\":\"运营商拨打\"}]}\n";
        QuickJSContext context = createContext();
        JSObject a = (JSObject) context.evaluate("var a = {}; a;");
        JSObject textObj = (JSObject) context.parse(text);
        a.setProperty("test", textObj);

        a.release();
        textObj.release();

        Object ret = context.evaluate("a.test.leadsId;");
        assertEquals("270", ret);
        context.destroy();
    }

    @Test
    public void jsonParseTest4() {
        String text = "{\"phoneNumber\":\"呼叫 18505815627\",\"leadsId\":\"270\",\"leadsBizId\":\"xxx\",\"options\":[{\"type\":\"aliyun\",\"avatarUrl\":\"https://gw.alicdn.com/tfs/TB1BYz0vpYqK1RjSZLeXXbXppXa-187-187.png\",\"personName\":\"老板\",\"storeName\":\"小店名称\",\"title\":\"智能办公电话\",\"content\":\"免费拨打\"},{\"type\":\"direct\",\"title\":\"普通电话\",\"content\":\"运营商拨打\"}]}\n";
        QuickJSContext context = createContext();
        JSObject a = (JSObject) context.evaluate("var a = {b: {}}; a;");
        JSObject b = a.getJSObject("b");
        JSObject c = (JSObject) context.parse(text);
        b.setProperty("test", c);

        c.release();
        b.release();
        a.release();

        Object ret = context.evaluate("a.b.test.leadsId;");
        assertEquals("270", ret);
        context.destroy();
    }

    @Test
    public void jsonParseTest5() {
        String text = "{\"phoneNumber\":\"呼叫 18505815627\",\"leadsId\":\"270\",\"leadsBizId\":\"xxx\",\"options\":[{\"type\":\"aliyun\",\"avatarUrl\":\"https://gw.alicdn.com/tfs/TB1BYz0vpYqK1RjSZLeXXbXppXa-187-187.png\",\"personName\":\"老板\",\"storeName\":\"小店名称\",\"title\":\"智能办公电话\",\"content\":\"免费拨打\"},{\"type\":\"direct\",\"title\":\"普通电话\",\"content\":\"运营商拨打\"}]}";
        QuickJSContext context = createContext();
        context.getGlobalObject().setProperty("test", args -> context.parse(text));

        JSObject ret = (JSObject) context.evaluate("var a = test(); a;");
        assertEquals(text, ret.stringify());
        ret.release();
        context.destroy();
    }

    @Test
    public void testFlat() {
        QuickJSContext context = createContext();
        JSArray ret = (JSArray) context.evaluate("let a = [1,[2,3]];  \n" +
                "a = a.flat();\n" +
                "a;");

        assertEquals(1, ret.get(0));
        assertEquals(2, ret.get(1));
        assertEquals(3, ret.get(2));

        ret.release();

        context.destroy();
    }

    @Test
    public void testClass() {
        QuickJSContext context = createContext();
        Object ret = context.evaluate("class User {\n" +
                "\tconstructor() {\n" +
                "\t\tthis.name = \"HarlonWang\";\n" +
                "\t}\n" +
                "}\n" +
                "\n" +
                "var user = new User();\n" +
                "user.name;");
        assertEquals("HarlonWang", ret);
        context.destroy();
    }

    @Test
    public void testGetOwnPropertyNames() {
        QuickJSContext context = createContext();
        context.evaluate("var a = {age: 1, ff: () => {}};");
        JSObject a = context.getGlobalObject().getJSObject("a");
        JSArray array = a.getNames();
        for (int i = 0; i < array.length(); i++) {
            String item = (String) array.get(i);
            if (i == 0) {
                assertEquals("age", item);
            } else {
                assertEquals("ff", item);
            }
        }

        array.release();
        a.release();

        context.destroy();
    }

    @Test
    public void testDumpStackError() {
        QuickJSContext context = createContext();
        try {
            context.evaluate("var a = 1; a();");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("not a function"));
        }
        context.destroy();
    }

    @Test
    public void testPromise2() {
        QuickJSContext context = createContext();
        context.getGlobalObject().setProperty("assert", args -> {
            assertEquals("哈哈", args[0]);
            return null;
        });
        JSObject ret = (JSObject) context.evaluate("    var defer =\n" +
                "        'function' == typeof Promise\n" +
                "            ? Promise.resolve().then.bind(Promise.resolve())\n" +
                "            : setTimeout;\n" +
                "    defer(() => { assert('哈哈'); });");
        ret.release();
        context.destroy();
    }

    @Test
    public void testProxy() {
        QuickJSContext context = createContext();
        context.getGlobalObject().setProperty("assert0", args -> {
            assertEquals(1, args[0]);
            assertNull(args[1]);
            return null;
        });
        context.getGlobalObject().setProperty("assert1", args -> {
            assertEquals(false, args[0]);
            assertEquals(37, args[1]);
            return null;
        });
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
                "assert0(p.a, p.b);      // 1, undefined\n" +
                "assert1('c' in p, p.c); // false, 37");

        context.destroy();
    }

    @Test(expected = QuickJSException.class)
    public void testQuickJSException() {
        QuickJSContext context = createContext();
        context.evaluate("a;");
        context.destroy();
    }

    @Test
    public void testReturnParseJSON() {
        QuickJSContext context = createContext();
        context.getGlobalObject().setProperty("test", args -> context.parse("{}"));
        JSObject ret = (JSObject) context.evaluate("test();test();test();");
        ret.release();
        context.destroy();
    }

    @Test
    public void testCreateNewJSArray() {
        QuickJSContext context = createContext();
        JSArray jsArray = context.createNewJSArray();
        jsArray.set(11, 0);
        jsArray.set("222", 1);
        JSFunction function = (JSFunction) context.evaluate("var test = (arg) => { return arg; };test;");
        JSArray result = (JSArray) function.call(jsArray);
        assertEquals("11,222", result.toString());

        result.release();
        function.release();
        jsArray.release();

        context.destroy();
    }

    @Test
    public void testMissingFormalParameter() {
        QuickJSContext context = createContext();
        try {
            context.evaluate("function y(1){}");
        } catch (QuickJSException e) {
            assertTrue(e.toString().contains("missing formal parameter"));
        }
        context.destroy();
    }

    @Test
    public void testStackSizeWithLimited() {
        QuickJSContext context = createContext();
        context.setMaxStackSize(1024 * 512);
        try {
            context.evaluate("function y(){y();} y();");
        } catch (QuickJSException e) {
            assertTrue(e.toString().contains("stack overflow"));
        }
        context.destroy();
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
            array.release();
            return TextUtils.equals("arg1", arg1) && 18 == age;
        }

        @JSMethod
        public boolean testObject(JSObject jsObj) {
            String str = jsObj.stringify();
            jsObj.release();
            return TextUtils.equals("{\"arg1\":\"a\",\"arg2\":18}", str);
        }

        @JSMethod
        public boolean testAll(JSObject object, JSArray message, int age, String name) {
            String oStr = object.stringify();
            String mStr = message.stringify();
            object.release();
            message.release();
            return TextUtils.equals("{\"arg1\":\"a\",\"arg2\":18},[1,2,3],18,name", oStr + "," + mStr + "," + age + "," + name);
        }
    }

    @Test
    public void testAnnotationMethod() {
        QuickJSContext context = createContext();

        context.getGlobalObject().setProperty("test1", TestJava.class);
        assertTrue((boolean) context.evaluate("test1.test1('arg1');"));
        assertTrue((boolean) context.evaluate("test1.test2('arg1', 18);"));
        assertTrue((boolean) context.evaluate("test1.testArray(['arg1', 18]);"));
        assertTrue((boolean) context.evaluate("test1.testObject({'arg1':'a', 'arg2':18});"));
        assertTrue((boolean) context.evaluate("test1.testAll({'arg1':'a', 'arg2':18}, [1,2,3],18, 'name');"));

        context.destroy();
    }

    @Test(expected = QuickJSException.class)
    public void testOnError() {
        QuickJSContext context = createContext();
        context.getGlobalObject().setProperty("assertTrue", args -> {
            assertTrue((Boolean) args[0]);
            assertEquals("'a' is not defined", args[1]);
            return null;
        });
        context.evaluate("onError = (e) => { assertTrue(e instanceof Error, e.message); }; a();");
        context.destroy();
    }

    @Test
    public void testPromiseUnhandledRejections() {
        thrown.expect(QuickJSException.class);
        thrown.expectMessage("'aaa' is not defined");

        QuickJSContext context = createContext();
        context.evaluate("new Promise(() => { aaa; });");
        context.destroy();
    }

    @Test
    public void testPromiseUnhandledRejections2() {
        QuickJSContext context = createContext();
        context.getGlobalObject().setProperty("assert", args -> {
            String error = (String) args[0];
            assertEquals(error, "'aaa' is not defined");
            return null;
        });
        JSObject ret = (JSObject) context.evaluate("new Promise(() => { aaa; }).catch((res) => { assert(res.message); });");
        ret.release();
        context.destroy();
    }

    @Test
    public void testPromiseUnhandledRejections3() {
        thrown.expect(QuickJSException.class);
        thrown.expectMessage("1");

        QuickJSContext context = createContext();
        context.evaluate("new Promise((resolve, reject) => { reject(1); });");
        context.destroy();
    }

    @Test
    public void testPromiseUnhandledRejections4() {
        thrown.expect(QuickJSException.class);
        thrown.expectMessage("1");

        QuickJSContext context = createContext();
        context.evaluate("new Promise((resolve, reject) => { reject(1); }); new Promise(() => { aaa; });");
        context.destroy();
    }

    @Test
    public void testPromiseUnhandledRejections5() {
        QuickJSContext context = createContext();
        context.getGlobalObject().setProperty("assert", args -> {
            assertEquals(1, args[0]);
            return null;
        });
        JSObject ret = (JSObject) context.evaluate("new Promise((resolve, reject) => { resolve(1); }).then((res) => { assert(res); });");
        ret.release();
        context.destroy();
    }

    @Test
    public void testPromiseUnhandledRejections6() {
        thrown.expect(QuickJSException.class);
        thrown.expectMessage("1");

        QuickJSContext context = createContext();
        context.evaluate("new Promise((resolve, reject) => { reject(1); }).then((res) => { res; });");
        context.destroy();
    }

    @Test
    public void testPromiseUnhandledRejections7() {
        thrown.expect(QuickJSException.class);
        thrown.expectMessage("'a' is not defined");

        QuickJSContext context = createContext();
        context.evaluate("new Promise((resolve, reject) => { reject(1); }).catch((res) => { a; });");
        context.destroy();
    }

    @Test
    public void testPromiseUnhandledRejections8() {
        thrown.expect(QuickJSException.class);
        thrown.expectMessage("'t2' is not defined");


        QuickJSContext context = createContext();
        context.evaluate("(function(){\n" +
                "    return new Promise((resolve, reject) => {\n" +
                "        reject(1);\n" +
                "    });\n" +
                "})().then((res) => {t0(res);}).catch((res) => {\n" +
                "    t1(a);\n" +
                "}).catch((res) => {\n" +
                "    t2(res);\n" +
                "});\n");
        context.destroy();
    }

    @Test
    public void testPromiseUnhandledRejections9() {
        QuickJSContext context = createContext();
        context.getGlobalObject().setProperty("assert", args -> {
            assertEquals(1, args[0]);
            return null;
        });
        JSObject ret = (JSObject) context.evaluate("(function(){\n" +
                "    return new Promise((resolve, reject) => {\n" +
                "        reject(1);\n" +
                "    });\n" +
                "})().then((res) => {\n" +
                "    t0.log(res);\n" +
                "}, (res) => {\n" +
                "    assert(res);\n" +
                "}).catch((res) => {\n" +
                "    t1(a);\n" +
                "}).catch((res) => {\n" +
                "    t2(res);\n" +
                "});");
        ret.release();
        context.destroy();
    }

    @Test
    public void testPromiseUnhandledRejections10() {
        thrown.expect(QuickJSException.class);
        thrown.expectMessage("'t4' is not defined");

        QuickJSContext context = createContext();
        context.evaluate("(function(){\n" +
                "    return new Promise((resolve, reject) => {\n" +
                "        reject(1);\n" +
                "    });\n" +
                "})().then((res) => {\n" +
                "    t1.log(res);\n" +
                "}, (res) => {\n" +
                "    t2.log(aa);\n" +
                "}).catch((res) => {\n" +
                "    t3.log(a);\n" +
                "}).catch((res) => {\n" +
                "    t4.log(res);\n" +
                "});\n");
        context.destroy();
    }

    @Test
    public void testPromiseUnhandledRejections11() {
        thrown.expect(QuickJSException.class);
        thrown.expectMessage("bad");

        QuickJSContext context = createContext();
        context.evaluate("new Promise((resolve, reject) => {\n" +
                "  resolve();\n" +
                "}).then(() => {\n" +
                "  throw new Error('bad');\n" +
                "});");
        context.destroy();
    }

    @Test
    public void testQuickJSExceptionWithJSError() {
        QuickJSContext context = createContext();
        try {
            context.evaluate("a;");
        } catch (QuickJSException e) {
            assertTrue(e.isJSError());
        }
        context.destroy();
    }

    @Test
    public void testQuickJSExceptionWithJavaError() {
        QuickJSContext context = createContext();
        Thread t1 = new Thread(() -> {
            try {
                context.evaluate("var a = 1;");
            } catch (QuickJSException e) {
                assertFalse(e.isJSError());
                assertEquals("Must be call same thread in QuickJSContext.create!", e.getMessage());
            }
        });
        t1.start();
        try {
            t1.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testIsAliveObject() {
        QuickJSContext context = createContext();
        JSObject object = context.createNewJSObject();
        context.getGlobalObject().setProperty("a", object);
        assertTrue(object.isAlive());
        object.release();
        context.destroy();
        assertFalse(object.isAlive());
    }

    @Test
    public void testNullExceptionWithGetProperty() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("Property Name cannot be null");

        QuickJSContext context = createContext();
        JSObject object = context.createNewJSObject();
        object.getJSObject(null);
        context.destroy();
    }

    @Test
    public void testNullExceptionWithSetProperty() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("Property Name cannot be null");

        QuickJSContext context = createContext();
        JSObject object = context.createNewJSObject();
        object.setProperty(null, context.getGlobalObject());
        context.destroy();
    }

    @Test
    public void testNullExceptionWithEval() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("Script cannot be null");

        QuickJSContext context = createContext();
        context.evaluate(null);
        context.destroy();
    }

    @Test
    public void testNullExceptionWithCompile() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("Script cannot be null with unknown.js");

        QuickJSContext context = createContext();
        context.compile(null);
        context.destroy();
    }

    @Test
    public void testNullExceptionWithEvalModule() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("Script cannot be null");

        QuickJSContext context = createContext();
        context.evaluateModule(null);
        context.destroy();
    }

    @Test
    public void testNullExceptionWithParseJSON() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("JSON cannot be null");

        QuickJSContext context = createContext();
        context.parse(null);
        context.destroy();
    }

    @Test
    public void testLongValue() {
        QuickJSContext context = createContext();

        // Java -> JavaScript
        long currentTime = System.currentTimeMillis();
        context.getGlobalObject().setProperty("longValue", args -> currentTime);
        context.getGlobalObject().setProperty("assert", args -> {
            long actual = (long) args[0];
            assertEquals(currentTime, actual);
            return null;
        });
        context.evaluate("assert(longValue());");

        // JavaScript -> Java
        context.evaluate("var a = 1686026400093n;");
        assertEquals(1686026400093L, (long)context.getGlobalObject().getLong("a"));

        context.destroy();
    }

    @Test
    public void dumpMemoryUsageTest() {
        QuickJSContext context = createContext();
        context.evaluate("var a = 100000; var b = 2000000; var c= a + b;");
        Context androidContext = ApplicationProvider.getApplicationContext();
        File file = new File(androidContext.getCacheDir(), "dump_memory.txt");
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        context.dumpMemoryUsage(file);
        context.destroy();
    }

    @Test
    public void dumpObjectsTest() {
        QuickJSContext context = createContext();
        context.evaluate("var testDumpObject = {data: 'I am test data'};");
        Context androidContext = ApplicationProvider.getApplicationContext();
        File file = new File(androidContext.getCacheDir(), "dump_objects.txt");
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        context.dumpObjects(file);
        context.destroy();
    }

    @Test
    public void testGetNativeFuncName() {
        QuickJSContext context = createContext();

        JSObject console1 = context.createNewJSObject();
        context.getGlobalObject().setProperty("console", console1);
        console1.release();

        JSObject console2 = context.getGlobalObject().getJSObject("console");
        console2.setProperty("log", args -> {
            assertEquals("nativeCall", args[0].toString());
            return null;
        });
        console2.release();

        context.evaluate("var nativeObj = {};");

        JSObject tinyDOM = context.getGlobalObject().getJSObject("nativeObj");
        tinyDOM.setProperty("nativeCall", args -> null);
        tinyDOM.release();
        context.evaluate("console.log(nativeObj.nativeCall.name);");
        context.destroy();
    }

    @Test
    public void testArrayAtSupport() {
        QuickJSContext context = createContext();
        context.evaluate("if([1, 2].at(0) !== 1) { throw 'not equal'; }");
        context.destroy();
    }

    @Test
    public void testPolyfillDate() {
        QuickJSContext context = createContext();
        context.evaluate("function assert(expected, actual) {\n" +
                "    if ((Date.parse(expected) === new Date(expected).getTime()) && (Date.parse(expected) === actual)) {\n" +
                "        console.log('✅assert passed with ' + expected, 'Date.parse = ' + Date.parse(expected), 'Date.construct = ' + new Date(expected).getTime(), 'actual = ' + actual);\n" +
                "    } else {\n" +
                "        console.log('❌assert failed with ' + expected, 'Date.parse = ' + Date.parse(expected), 'Date.construct = ' + new Date(expected).getTime(), 'actual = ' + actual);\n" +
                "        throw Error('parse failed.');\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "assert('20130108', 1357603200000);\n" +
                "assert('2018-04-24', 1524528000000);\n" +
                "assert('2018-04-24 11:12', 1524539520000);\n" +
                "assert('2018-05-02 11:12:13', 1525230733000);\n" +
                "assert('2018-05-02 11:12:13.998', 1525230733998);\n" +
                "assert('2018-4-1', 1522540800000);\n" +
                "assert('2018-4-1 11:12', 1522552320000);\n" +
                "assert('2018-4-1 1:1:1:223', 1522515661223);\n" +
                "assert('2018-01', 1514764800000);\n" +
                "assert('2018', 1514764800000);\n" +
                "assert('2018-05-02T11:12:13Z', 1525259533000);");
        context.destroy();
    }

    @Test
    public void testNativeCallWithAsyncFunc() {
        QuickJSContext context = createContext();
        context.getGlobalObject().setProperty("nativeCall", args -> {
            JSFunction function = (JSFunction) args[0];
            JSObject ret = (JSObject) function.call();
            ret.release();
            function.release();
            return null;
        });
        context.evaluate("nativeCall(async () => { console.log(123); });");
        context.destroy();
    }

    @Test
    public void testNativeCallWithAsyncFuncResult() {
        QuickJSContext context = createContext();
        context.evaluate("async function test() {return \"123\";}");
        JSFunction test = context.getGlobalObject().getJSFunction("test");
        JSObject promise = (JSObject) test.call();
        JSFunction then = promise.getJSFunction("then");
        JSObject ret = (JSObject) then.call((JSCallFunction) args -> {
            System.out.println(args[0]);
            return null;
        });

        ret.release();
        then.release();
        promise.release();
        test.release();

        context.destroy();
    }

    @Test
    public void testJSArraySetParseJSON() {
        QuickJSContext context = createContext();
        context.getGlobalObject().setProperty("getData", args -> {
            JSArray jsArray = context.createNewJSArray();
            JSObject jsObject = context.parseJSON("{\"name\": \"Jack\", \"age\": 33}");
            jsArray.set(jsObject, 0);

            JSObject jsObject1 = context.parseJSON("{\"name\": \"Jack\", \"age\": 33}");
            jsArray.set(jsObject1, 1);

            jsObject.release();
            jsObject1.release();
            return jsArray;
        });
        context.evaluate("var array = getData();console.log(JSON.stringify(array));console.log(array[0]);");
        context.destroy();
    }

    @Test
    public void testJSONParse() {
        QuickJSContext context = createContext();
        String ret = (String) context.parse("\"test\"");
        assertEquals(ret, "test");
        context.destroy();
    }

    @Test
    public void testExceptionWhenParse() {
        thrown.expect(QuickJSException.class);
        thrown.expectMessage("unexpected token: 'test'");

        QuickJSContext context = createContext();
        context.parse("test");
        context.destroy();
    }

    @Test
    public void testExceptionWhenParseJSON() {
        thrown.expect(QuickJSException.class);
        thrown.expectMessage("Only parse json with valid format, must be start with '{', if it contains other case, use parse(String) replace.");

        QuickJSContext context = createContext();
        context.parseJSON("\"test\"");
        context.destroy();
    }

    @Test
    public void testReturnJSCallback() {
        QuickJSContext context = createContext();
        context.getGlobalObject().setProperty("test", args -> (JSCallFunction) args1 -> "123");
        context.evaluate("console.log(test()());");
        context.destroy();
    }

    @Test
    public void testPromiseCrash() {
        thrown.expect(QuickJSException.class);
        thrown.expectMessage("我来自Exception的值");
        QuickJSContext jsContext = createContext();
        JSObject pofeng = jsContext.createNewJSObject();
        JSObject gol = jsContext.getGlobalObject();
        gol.setProperty("pofeng", pofeng);
        pofeng.setProperty("getSystemInfo", args -> {
            ((JSFunction) ((JSObject) args[0]).getJSObject("success")).call("我来自Exception的值");
            return "我来自Java的值";
        });

        String js = "new Promise((resolve, reject) => {\n" +
                "            pofeng.getSystemInfo({\n" +
                "                success: res => {\n" +
                "                    reject(res);\n" +
                "                }\n" +
                "            })\n" +
                "        })";
        jsContext.evaluate(js);
        jsContext.destroy();
    }

    @Test
    public void testJSCallFunctionReleased() {
        QuickJSContext jsContext = createContext();
        for (int i = 0; i < 100; i++) {
            JSObject jsObject = jsContext.createNewJSObject();
            JSCallFunction function = args -> null;
            jsObject.setProperty("test", function);
            // jsObject.release();
        }

        // QuickJSLoader.initConsoleLog 方法里有调用过一次 setProperty，总数还剩1个
        // assertEquals(1, jsContext.getCallFunctionMapSize());
        jsContext.releaseObjectRecords();
        jsContext.releaseObjectRecords();
        jsContext.destroy();
        assertEquals(0, jsContext.getCallFunctionMapSize());
    }

    @Test
    public void testLongMaxValue() {
        QuickJSContext context = createContext();
        context.getGlobalObject().setProperty("assert", args -> {
            assertEquals(Long.MAX_VALUE, args[0]);
            return null;
        });
        context.getGlobalObject().setProperty("longMaxValue", args -> Long.MAX_VALUE);
        context.evaluate("assert(longMaxValue());");
        context.destroy();
    }

    @Test
    public void testMaxSafeInteger() {
        QuickJSContext context = createContext();
        context.getGlobalObject().setProperty("assertEquals", args -> {
            assertEquals(args[0], args[1]);
            return null;
        });

        context.getGlobalObject().setProperty("minThanMSF", args -> 9007199254740990L);
        context.getGlobalObject().setProperty("equalThanMSF", args -> 9007199254740991L);
        context.getGlobalObject().setProperty("maxThanMSF", args -> 9007199254740993L);

        context.evaluate("assertEquals(typeof minThanMSF(), 'number'); assertEquals(minThanMSF(), 9007199254740990);");
        context.evaluate("assertEquals(typeof equalThanMSF(), 'number'); assertEquals(equalThanMSF(), 9007199254740991);");
        context.evaluate("assertEquals(typeof maxThanMSF(), 'bigint'); assertEquals(maxThanMSF(), 9007199254740993n);");

        context.getGlobalObject().setProperty("minThanMinSF", args -> -9007199254740993L);
        context.getGlobalObject().setProperty("equalThanMinSF", args -> -9007199254740991L);
        context.getGlobalObject().setProperty("maxThanMinSF", args -> -9007199254740990L);

        context.evaluate("assertEquals(typeof minThanMinSF(), 'bigint'); assertEquals(minThanMinSF(), -9007199254740993n);");
        context.evaluate("assertEquals(typeof equalThanMinSF(), 'number'); assertEquals(equalThanMinSF(), -9007199254740991);");
        context.evaluate("assertEquals(typeof maxThanMinSF(), 'number'); assertEquals(maxThanMinSF(), -9007199254740990);");

        context.destroy();
    }

    @Test
    public void testNumberWithLongType() {
        QuickJSContext context = createContext();
        Object d = context.evaluate("3.214;");
        assertEquals("Double", d.getClass().getSimpleName());
        assertEquals(3.214, d);

        Object i = context.evaluate("123;");
        assertEquals("Integer", i.getClass().getSimpleName());
        assertEquals(123, i);

        Object l = context.evaluate("Number.MAX_SAFE_INTEGER;");
        assertEquals("Long", l.getClass().getSimpleName());
        assertEquals(9007199254740991L, l);

        Object l1 = context.evaluate("BigInt(Number.MAX_SAFE_INTEGER) + 2n;");
        assertEquals("Long", l1.getClass().getSimpleName());
        assertEquals(9007199254740993L, l1);

        context.destroy();
    }

    @Test
    public void testParseObjectReleased() {
        int maxSize = 1000000;

        QuickJSContext context = createContext();
        StringBuilder builder = new StringBuilder();
        builder.append("\"");
        for (int i = 0; i < maxSize; i++) {
            builder.append("s");
        }
        builder.append("\"");
        context.parse(builder.toString());

        Context androidContext = ApplicationProvider.getApplicationContext();
        File file = new File(androidContext.getCacheDir(), "dump_memory.txt");
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        context.dumpMemoryUsage(file);

        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder stringBuilder = new StringBuilder();

            char[] buffer = new char[10];
            while (reader.read(buffer) != -1) {
                stringBuilder.append(new String(buffer));
                buffer = new char[10];
            }
            reader.close();
            String content = stringBuilder.toString();
            // Log.d("ParseReleased", content);

            // allocated size should be < max size, if released.
            Matcher matcher = Pattern.compile("memory.allocated.*block").matcher(content);
            if (matcher.find()) {
                String group = matcher.group();
                String[] split = group.split(" ");
                List<String> split2 = new ArrayList<>();
                for (String s : split) {
                    String item = s.trim();
                    if (!TextUtils.isEmpty(item)) {
                        split2.add(item);
                    }
                }

                String allocatedSize = split2.get(3);
                assertTrue(Integer.parseInt(allocatedSize) < maxSize);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        context.destroy();
    }

    @Test
    public void testArrayAt() {
        QuickJSContext context = createContext();
        int ret0 = (int) context.evaluate("[5, 12, 8, 130, 44].at(2);");
        // Expected output: "Using an index of 2 the item returned is 8"
        assertEquals(8, ret0);

        int ret1 = (int) context.evaluate("[5, 12, 8, 130, 44].at(-2);");
        // Expected output: "Using an index of -2 item returned is 130"
        assertEquals(130, ret1);

        // index < -array.length 或 index >= array.length，则总是返回 undefined
        assertTrue((Boolean) context.evaluate("[5, 12, 8, 130, 44].at(5) === undefined"));
        assertTrue((Boolean) context.evaluate("[5, 12, 8, 130, 44].at(-6) === undefined"));

        context.destroy();
    }

    @Test
    public void testDumpObjects() {
        QuickJSContext context = createContext();
        context.dumpObjects();
        context.dumpMemoryUsage();
        context.destroy();
    }

    @Test
    public void testAsyncErrorInByteCode() {
        thrown.expect(QuickJSException.class);
        thrown.expectMessage("'aa' is not defined");

        QuickJSContext context = createContext();
        byte[] bytes = context.compile("async function test() { aa; } test();");
        context.execute(bytes);
        context.destroy();
    }

    @Test
    public void testObjectToMap() {
        QuickJSContext context = createContext();
        JSObject ret = (JSObject) context.evaluate("var a = {'a': '123', 'b': [1, 2]};a.c = a;;a;");
        assertEquals("{a=123, b=[1, 2]}", ret.toMap().toString());
        ret.release();

        JSArray array = (JSArray) context.evaluate("var b = [{a: { c : 'xxx'}}, 2, 'qqq', 1.22]; b.push(b); b;");
        assertEquals("[{a={c=xxx}}, 2, qqq, 1.22]", array.toArray().toString());
        array.release();

        assertEquals("{a={a=123, b=[1, 2]}, b=[{a={c=xxx}}, 2, qqq, 1.22], Infinity=-9223372036854775808, NaN=NaN, Math={LN2=0.6931471805599453, LN10=2.302585092994046, LOG2E=1.4426950408889634, E=2.718281828459045, SQRT2=1.4142135623730951, LOG10E=0.4342944819032518, PI=3.141592653589793, SQRT1_2=0.7071067811865476}, undefined=null}", context.getGlobalObject().toMap().toString());
        context.destroy();
    }

    @Test
    public void testObjectToMapFilter() {
        QuickJSContext context = createContext();
        HashMap<String, Object> map = context.getGlobalObject().toMap((key, pointer, extra) -> {
            assertEquals("test", extra.toString());
            return key.equals("Infinity");
        }, "test");
        System.out.println(map.toString());
        assertEquals("{NaN=NaN, Math={LN2=0.6931471805599453, LN10=2.302585092994046, LOG2E=1.4426950408889634, E=2.718281828459045, SQRT2=1.4142135623730951, LOG10E=0.4342944819032518, PI=3.141592653589793, SQRT1_2=0.7071067811865476}, undefined=null}", map.toString());
        context.destroy();
    }

    @Test
    public void testObjectLeakDetection() {
        QuickJSContext context = createContext();
        context.setLeakDetectionListener((leak, stringValue) -> {
            assertEquals(stringValue, "{ name: 'leak1' }");
        });

        // 泄漏场景
        JSObject o = context.createNewJSObject();
        o.setProperty("name", "leak1");
        // o.release();

        context.destroy();
    }

}
