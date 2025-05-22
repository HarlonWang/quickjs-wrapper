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
import androidx.test.platform.app.InstrumentationRegistry;

import com.whl.quickjs.android.QuickJSLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    public static String readFile(String fileName) {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // 打开 assets 目录下的文件
        InputStream inputStream;
        try {
            inputStream = context.getAssets().open(fileName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String line;

        while (true) {
            try {
                if ((line = reader.readLine()) == null) break;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            stringBuilder.append(line).append("\n");
        }

        return stringBuilder.toString();
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void initSo() {
        QuickJSLoader.init();
        QuickJSLoader.startRedirectingStdoutStderr("QuickJSTest");
    }

    public static QuickJSContext createContext() throws QuickJSException {
        QuickJSContext context = QuickJSContext.create();
        context.setConsole(new LogcatConsole("console-test"));
        context.setModuleLoader(new QuickJSContext.DefaultModuleLoader() {
            @Override
            public String getModuleStringCode(String moduleName) {
                return readFile(moduleName.replace("./", ""));
            }
        });
        return context;
    }

    @Test
    public void setPropertyTest() {
        try (QuickJSContext context = createContext()) {
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
        }
    }

    @Test
    public void getPropertyTest() {
        try (QuickJSContext context = createContext()) {
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
        }
    }

    @Test
    public void getJSArrayTest() {
        try (QuickJSContext context = createContext()) {
            JSArray ret = (JSArray) context.evaluate("function test(value) {\n" +
                    "\treturn [1, 2, value];\n" +
                    "}\n" +
                    "\n" +
                    "test(3);");
            assertEquals(3, ret.get(2));
            ret.release();
        }
    }

    @Test
    public void JSFunctionArgsTest() {
        try (QuickJSContext context = createContext()) {
            context.evaluate("function test(intValue, stringValue, doubleValue, booleanValue) {\n" +
                    "\treturn \"hello, \" + intValue + stringValue + doubleValue + booleanValue;\n" +
                    "}");
            JSObject globalObject = context.getGlobalObject();
            JSFunction func = (JSFunction) globalObject.getProperty("test");
            assertEquals("hello, 1string123.11true", func.call(1, "string", 123.11, true));
            func.release();
        }
    }

    @Test
    public void JSFunctionNullArgsTest() {
        try (QuickJSContext context = createContext()) {
            context.evaluate("function test(arg1, arg2, arg3) {\n" +
                    "\treturn \"hello, \" + arg1 + arg2 + arg3;\n" +
                    "}");
            JSObject globalObject = context.getGlobalObject();
            JSFunction func = (JSFunction) globalObject.getProperty("test");
            assertEquals("hello, null-13", func.call(null, -1, 3));
            func.release();
        }
    }

    @Test
    public void JSFunctionArgsTestWithUnSupportType() {
        try (QuickJSContext context = createContext()) {
            context.evaluate("function test(name) {\n" +
                    "\treturn \"hello, \" + name;\n" +
                    "}");
            JSObject globalObject = context.getGlobalObject();
            JSFunction func = (JSFunction) globalObject.getProperty("test");
            try {
                func.call((Object) new int[]{1, 2});
                fail();
            } catch (Exception e) {
                func.release();
                assertTrue(e.toString().contains("Unsupported Java type"));
            }
        }
    }

    @Test
    public void arrowFuncTest() {
        try (QuickJSContext context = createContext()) {
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
        }
    }

    @Test
    public void setPropertyWithJSObjectTest() {
        try (QuickJSContext context = createContext()) {
            context.evaluate("var test = {count: 0};");
            JSObject test = context.getGlobalObject().getJSObject("test");
            context.getGlobalObject().setProperty("test1", test);

            JSObject test1 = context.getGlobalObject().getJSObject("test1");

            assertEquals("{\"count\":0}", test1.stringify());

            test.release();
            test1.release();
        }
    }

    @Test
    public void jsonParseTest() {
        String text = "{\"phoneNumber\":\"呼叫 18505815627\",\"leadsId\":\"270\",\"leadsBizId\":\"xxx\",\"options\":[{\"type\":\"aliyun\",\"avatarUrl\":\"https://gw.alicdn.com/tfs/TB1BYz0vpYqK1RjSZLeXXbXppXa-187-187.png\",\"personName\":\"老板\",\"storeName\":\"小店名称\",\"title\":\"智能办公电话\",\"content\":\"免费拨打\"},{\"type\":\"direct\",\"title\":\"普通电话\",\"content\":\"运营商拨打\"}]}";

        try (QuickJSContext context = createContext()) {
            JSObject result = (JSObject) context.parse(text);
            assertEquals("270", result.getProperty("leadsId"));

            context.getGlobalObject().setProperty("test", result);

            result.release();

            JSObject test = context.getGlobalObject().getJSObject("test");
            assertEquals(text, test.stringify());
            test.release();
        }
    }

    @Test
    public void jsonParseTest3() {
        String text = "{\"phoneNumber\":\"呼叫 18505815627\",\"leadsId\":\"270\",\"leadsBizId\":\"xxx\",\"options\":[{\"type\":\"aliyun\",\"avatarUrl\":\"https://gw.alicdn.com/tfs/TB1BYz0vpYqK1RjSZLeXXbXppXa-187-187.png\",\"personName\":\"老板\",\"storeName\":\"小店名称\",\"title\":\"智能办公电话\",\"content\":\"免费拨打\"},{\"type\":\"direct\",\"title\":\"普通电话\",\"content\":\"运营商拨打\"}]}\n";
        try (QuickJSContext context = createContext()) {
            JSObject a = (JSObject) context.evaluate("var a = {}; a;");
            JSObject textObj = (JSObject) context.parse(text);
            a.setProperty("test", textObj);

            a.release();
            textObj.release();

            Object ret = context.evaluate("a.test.leadsId;");
            assertEquals("270", ret);
        }
    }

    @Test
    public void jsonParseTest4() {
        String text = "{\"phoneNumber\":\"呼叫 18505815627\",\"leadsId\":\"270\",\"leadsBizId\":\"xxx\",\"options\":[{\"type\":\"aliyun\",\"avatarUrl\":\"https://gw.alicdn.com/tfs/TB1BYz0vpYqK1RjSZLeXXbXppXa-187-187.png\",\"personName\":\"老板\",\"storeName\":\"小店名称\",\"title\":\"智能办公电话\",\"content\":\"免费拨打\"},{\"type\":\"direct\",\"title\":\"普通电话\",\"content\":\"运营商拨打\"}]}\n";
        try (QuickJSContext context = createContext()) {
            JSObject a = (JSObject) context.evaluate("var a = {b: {}}; a;");
            JSObject b = a.getJSObject("b");
            JSObject c = (JSObject) context.parse(text);
            b.setProperty("test", c);

            c.release();
            b.release();
            a.release();

            Object ret = context.evaluate("a.b.test.leadsId;");
            assertEquals("270", ret);
        }
    }

    @Test
    public void jsonParseTest5() {
        String text = "{\"phoneNumber\":\"呼叫 18505815627\",\"leadsId\":\"270\",\"leadsBizId\":\"xxx\",\"options\":[{\"type\":\"aliyun\",\"avatarUrl\":\"https://gw.alicdn.com/tfs/TB1BYz0vpYqK1RjSZLeXXbXppXa-187-187.png\",\"personName\":\"老板\",\"storeName\":\"小店名称\",\"title\":\"智能办公电话\",\"content\":\"免费拨打\"},{\"type\":\"direct\",\"title\":\"普通电话\",\"content\":\"运营商拨打\"}]}";
        try (QuickJSContext context = createContext()) {
            context.getGlobalObject().setProperty("test", args -> context.parse(text));

            JSObject ret = (JSObject) context.evaluate("var a = test(); a;");
            assertEquals(text, ret.stringify());
            ret.release();
        }
    }

    @Test
    public void testFlat() {
        try (QuickJSContext context = createContext()) {
            JSArray ret = (JSArray) context.evaluate("let a = [1,[2,3]];  \n" +
                    "a = a.flat();\n" +
                    "a;");

            assertEquals(1, ret.get(0));
            assertEquals(2, ret.get(1));
            assertEquals(3, ret.get(2));

            ret.release();

        }
    }

    @Test
    public void testClass() {
        try (QuickJSContext context = createContext()) {
            Object ret = context.evaluate("class User {\n" +
                    "\tconstructor() {\n" +
                    "\t\tthis.name = \"HarlonWang\";\n" +
                    "\t}\n" +
                    "}\n" +
                    "\n" +
                    "var user = new User();\n" +
                    "user.name;");
            assertEquals("HarlonWang", ret);
        }
    }

    @Test
    public void testGetOwnPropertyNames() {
        try (QuickJSContext context = createContext()) {
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

        }
    }

    @Test
    public void testDumpStackError() {
        try (QuickJSContext context = createContext()) {
            try {
                context.evaluate("var a = 1; a();");
            } catch (Exception e) {
                assertTrue(Objects.requireNonNull(e.getMessage()).contains("not a function"));
            }
        }
    }

    @Test
    public void testPromise2() {
        try (QuickJSContext context = createContext()) {
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
        }
    }

    @Test
    public void testProxy() {
        try (QuickJSContext context = createContext()) {
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

        }
    }

    @Test(expected = QuickJSException.class)
    public void testQuickJSException() {
        try (QuickJSContext context = createContext()) {
            context.evaluate("a;");
        }
    }

    @Test
    public void testReturnParseJSON() {
        try (QuickJSContext context = createContext()) {
            context.getGlobalObject().setProperty("test", args -> context.parse("{}"));
            JSObject ret = (JSObject) context.evaluate("test();test();test();");
            ret.release();
        }
    }

    @Test
    public void testCreateNewJSArray() {
        try (QuickJSContext context = createContext()) {
            JSArray jsArray = context.createNewJSArray();
            jsArray.set(11, 0);
            jsArray.set("222", 1);
            JSFunction function = (JSFunction) context.evaluate("var test = (arg) => { return arg; };test;");
            JSArray result = (JSArray) function.call(jsArray);
            assertEquals("11,222", result.toString());

            result.release();
            function.release();
            jsArray.release();

        }
    }

    @Test
    public void testMissingFormalParameter() {
        try (QuickJSContext context = createContext()) {
            try {
                context.evaluate("function y(1){}");
            } catch (QuickJSException e) {
                assertTrue(e.toString().contains("missing formal parameter"));
            }
        }
    }

    @Test
    public void testStackSizeWithLimited() {
        try (QuickJSContext context = createContext()) {
            context.setMaxStackSize(1024 * 512);
            try {
                context.evaluate("function y(){y();} y();");
            } catch (QuickJSException e) {
                assertTrue(e.toString().contains("stack overflow"));
            }
        }
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
        try (QuickJSContext context = createContext()) {

            context.getGlobalObject().setProperty("test1", TestJava.class);
            assertTrue((boolean) context.evaluate("test1.test1('arg1');"));
            assertTrue((boolean) context.evaluate("test1.test2('arg1', 18);"));
            assertTrue((boolean) context.evaluate("test1.testArray(['arg1', 18]);"));
            assertTrue((boolean) context.evaluate("test1.testObject({'arg1':'a', 'arg2':18});"));
            assertTrue((boolean) context.evaluate("test1.testAll({'arg1':'a', 'arg2':18}, [1,2,3],18, 'name');"));

        }
    }

    @Test(expected = QuickJSException.class)
    public void testOnError() {
        try (QuickJSContext context = createContext()) {
            context.getGlobalObject().setProperty("assertTrue", args -> {
                assertTrue((Boolean) args[0]);
                assertEquals("'a' is not defined", args[1]);
                return null;
            });
            context.evaluate("onError = (e) => { assertTrue(e instanceof Error, e.message); }; a();");
        }
    }

    @Test
    public void testPromiseUnhandledRejections() {
        thrown.expect(QuickJSException.class);
        thrown.expectMessage("'aaa' is not defined");

        try (QuickJSContext context = createContext()) {
            context.evaluate("new Promise(() => { aaa; });");
        }
    }

    @Test
    public void testPromiseUnhandledRejections2() {
        try (QuickJSContext context = createContext()) {
            context.getGlobalObject().setProperty("assert", args -> {
                String error = (String) args[0];
                assertEquals("'aaa' is not defined", error);
                return null;
            });
            JSObject ret = (JSObject) context.evaluate("new Promise(() => { aaa; }).catch((res) => { assert(res.message); });");
            ret.release();
        }
    }

    @Test
    public void testPromiseUnhandledRejections3() {
        thrown.expect(QuickJSException.class);
        thrown.expectMessage("1");

        try (QuickJSContext context = createContext()) {
            context.evaluate("new Promise((resolve, reject) => { reject(1); });");
        }
    }

    @Test
    public void testPromiseUnhandledRejections4() {
        thrown.expect(QuickJSException.class);
        thrown.expectMessage("1");

        try (QuickJSContext context = createContext()) {
            context.evaluate("new Promise((resolve, reject) => { reject(1); }); new Promise(() => { aaa; });");
        }
    }

    @Test
    public void testPromiseUnhandledRejections5() {
        try (QuickJSContext context = createContext()) {
            context.getGlobalObject().setProperty("assert", args -> {
                assertEquals(1, args[0]);
                return null;
            });
            JSObject ret = (JSObject) context.evaluate("new Promise((resolve, reject) => { resolve(1); }).then((res) => { assert(res); });");
            ret.release();
        }
    }

    @Test
    public void testPromiseUnhandledRejections6() {
        thrown.expect(QuickJSException.class);
        thrown.expectMessage("1");

        try (QuickJSContext context = createContext()) {
            context.evaluate("new Promise((resolve, reject) => { reject(1); }).then((res) => { res; });");
        }
    }

    @Test
    public void testPromiseUnhandledRejections7() {
        thrown.expect(QuickJSException.class);
        thrown.expectMessage("'a' is not defined");

        try (QuickJSContext context = createContext()) {
            context.evaluate("new Promise((resolve, reject) => { reject(1); }).catch((res) => { a; });");
        }
    }

    @Test
    public void testPromiseUnhandledRejections8() {
        thrown.expect(QuickJSException.class);
        thrown.expectMessage("'t2' is not defined");


        try (QuickJSContext context = createContext()) {
            context.evaluate("(function(){\n" +
                    "    return new Promise((resolve, reject) => {\n" +
                    "        reject(1);\n" +
                    "    });\n" +
                    "})().then((res) => {t0(res);}).catch((res) => {\n" +
                    "    t1(a);\n" +
                    "}).catch((res) => {\n" +
                    "    t2(res);\n" +
                    "});\n");
        }
    }

    @Test
    public void testPromiseUnhandledRejections9() {
        try (QuickJSContext context = createContext()) {
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
        }
    }

    @Test
    public void testPromiseUnhandledRejections10() {
        thrown.expect(QuickJSException.class);
        thrown.expectMessage("'t4' is not defined");

        try (QuickJSContext context = createContext()) {
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
        }
    }

    @Test
    public void testPromiseUnhandledRejections11() {
        thrown.expect(QuickJSException.class);
        thrown.expectMessage("bad");

        try (QuickJSContext context = createContext()) {
            context.evaluate("new Promise((resolve, reject) => {\n" +
                    "  resolve();\n" +
                    "}).then(() => {\n" +
                    "  throw new Error('bad');\n" +
                    "});");
        }
    }

    @Test
    public void testQuickJSExceptionWithJSError() {
        try (QuickJSContext context = createContext()) {
            try {
                context.evaluate("a;");
            } catch (QuickJSException e) {
                assertTrue(e.isJSError());
            }
        }
    }

    @Test
    public void testQuickJSExceptionWithJavaError() {
        try (QuickJSContext context = createContext()) {
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

        try (QuickJSContext context = createContext()) {
            JSObject object = context.createNewJSObject();
            object.getJSObject(null);
        }
    }

    @Test
    public void testNullExceptionWithSetProperty() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("Property Name cannot be null");

        try (QuickJSContext context = createContext()) {
            JSObject object = context.createNewJSObject();
            object.setProperty(null, context.getGlobalObject());
        }
    }

    @Test
    public void testNullExceptionWithEval() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("Script cannot be null");

        try (QuickJSContext context = createContext()) {
            context.evaluate(null);
        }
    }

    @Test
    public void testNullExceptionWithCompile() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("Script cannot be null with unknown.js");

        try (QuickJSContext context = createContext()) {
            context.compile(null);
        }
    }

    @Test
    public void testNullExceptionWithEvalModule() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("Script cannot be null");

        try (QuickJSContext context = createContext()) {
            context.evaluateModule(null);
        }
    }

    @Test
    public void testNullExceptionWithParseJSON() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("JSON cannot be null");

        try (QuickJSContext context = createContext()) {
            context.parse(null);
        }
    }

    @Test
    public void testLongValue() {
        try (QuickJSContext context = createContext()) {

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
            assertEquals(1686026400093L, (long) context.getGlobalObject().getLong("a"));

        }
    }

    @Test
    public void dumpMemoryUsageTest() {
        try (QuickJSContext context = createContext()) {
            context.evaluate("var a = 100000; var b = 2000000; var c= a + b;");
            Context androidContext = ApplicationProvider.getApplicationContext();
            File file = new File(androidContext.getCacheDir(), "dump_memory.txt");
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            context.dumpMemoryUsage(file);
        }
    }

    @Test
    public void dumpObjectsTest() {
        try (QuickJSContext context = createContext()) {
            context.evaluate("var testDumpObject = {data: 'I am test data'};");
            Context androidContext = ApplicationProvider.getApplicationContext();
            File file = new File(androidContext.getCacheDir(), "dump_objects.txt");
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            context.dumpObjects(file);
        }
    }

    @Test
    public void testGetNativeFuncName() {
        try (QuickJSContext context = createContext()) {

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
        }
    }

    @Test
    public void testArrayAtSupport() {
        try (QuickJSContext context = createContext()) {
            context.evaluate("if([1, 2].at(0) !== 1) { throw 'not equal'; }");
        }
    }

    @Test
    public void testPolyfillDate() {
        try (QuickJSContext context = createContext()) {
            context.evaluate(readFile("test_polyfill_date.js"));
        }
    }

    @Test
    public void testNativeCallWithAsyncFunc() {
        try (QuickJSContext context = createContext()) {
            context.getGlobalObject().setProperty("nativeCall", args -> {
                JSFunction function = (JSFunction) args[0];
                JSObject ret = (JSObject) function.call();
                ret.release();
                function.release();
                return null;
            });
            context.evaluate("nativeCall(async () => { console.log(123); });");
        }
    }

    @Test
    public void testNativeCallWithAsyncFuncResult() {
        try (QuickJSContext context = createContext()) {
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

        }
    }

    @Test
    public void testJSArraySetParseJSON() {
        try (QuickJSContext context = createContext()) {
            context.getGlobalObject().setProperty("getData", args -> {
                JSArray jsArray = context.createNewJSArray();
                JSObject jsObject = (JSObject) context.parse("{\"name\": \"Jack\", \"age\": 33}");
                jsArray.set(jsObject, 0);

                JSObject jsObject1 = (JSObject) context.parse("{\"name\": \"Jack\", \"age\": 33}");
                jsArray.set(jsObject1, 1);

                jsObject.release();
                jsObject1.release();
                return jsArray;
            });
            context.evaluate("var array = getData();console.log(JSON.stringify(array));console.log(array[0]);");
        }
    }

    @Test
    public void testJSONParse() {
        try (QuickJSContext context = createContext()) {
            String ret = (String) context.parse("\"test\"");
            assertEquals("test", ret);
        }
    }

    @Test
    public void testExceptionWhenParse() {
        thrown.expect(QuickJSException.class);
        thrown.expectMessage("unexpected token: 'test'");

        try (QuickJSContext context = createContext()) {
            context.parse("test");
        }
    }

    @Test
    public void testExceptionWhenParseJSON() {
        thrown.expect(QuickJSException.class);
        thrown.expectMessage("Only parse json with valid format, must be start with '{', if it contains other case, use parse(String) replace.");

        try (QuickJSContext context = createContext()) {
            context.parseJSON("\"test\"");
        }
    }

    @Test
    public void testReturnJSCallback() {
        try (QuickJSContext context = createContext()) {
            context.getGlobalObject().setProperty("test", args -> (JSCallFunction) args1 -> "123");
            context.evaluate("console.log(test()());");
        }
    }

    @Test
    public void testPromiseCrash() {
        thrown.expect(QuickJSException.class);
        thrown.expectMessage("我来自Exception的值");
        try (QuickJSContext context = createContext()) {
            JSObject pofeng = context.createNewJSObject();
            JSObject gol = context.getGlobalObject();
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
            context.evaluate(js);
        }
    }

    @Test
    public void testJSCallFunctionReleased() {
        QuickJSContext jsContext = createContext();
        for (int i = 0; i < 100; i++) {
            JSObject jsObject = jsContext.createNewJSObject();
            JSCallFunction function = args -> null;
            jsObject.setProperty("test", function);
            jsObject.release();
        }

        // QuickJSLoader.initConsoleLog 方法里有调用过一次 setProperty，总数还剩1个
        assertEquals(1, jsContext.getCallFunctionMapSize());
        jsContext.releaseObjectRecords();
        jsContext.releaseObjectRecords();
        jsContext.destroy();
        assertEquals(0, jsContext.getCallFunctionMapSize());
    }

    @Test
    public void testLongMaxValue() {
        try (QuickJSContext context = createContext()) {
            context.getGlobalObject().setProperty("assert", args -> {
                assertEquals(Long.MAX_VALUE, args[0]);
                return null;
            });
            context.getGlobalObject().setProperty("longMaxValue", args -> Long.MAX_VALUE);
            context.evaluate("assert(longMaxValue());");
        }
    }

    @Test
    public void testMaxSafeInteger() {
        try (QuickJSContext context = createContext()) {
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

        }
    }

    @Test
    public void testNumberWithLongType() {
        try (QuickJSContext context = createContext()) {
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

        }
    }

    @Test
    public void testParseObjectReleased() {
        int maxSize = 1000000;

        try (QuickJSContext context = createContext()) {
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
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    @Test
    public void testArrayAt() {
        try (QuickJSContext context = createContext()) {
            int ret0 = (int) context.evaluate("[5, 12, 8, 130, 44].at(2);");
            // Expected output: "Using an index of 2 the item returned is 8"
            assertEquals(8, ret0);

            int ret1 = (int) context.evaluate("[5, 12, 8, 130, 44].at(-2);");
            // Expected output: "Using an index of -2 item returned is 130"
            assertEquals(130, ret1);

            // index < -array.length 或 index >= array.length，则总是返回 undefined
            assertTrue((Boolean) context.evaluate("[5, 12, 8, 130, 44].at(5) === undefined"));
            assertTrue((Boolean) context.evaluate("[5, 12, 8, 130, 44].at(-6) === undefined"));

        }
    }

    @Test
    public void testDumpObjects() {
        try (QuickJSContext context = createContext()) {
            context.dumpObjects();
            context.dumpMemoryUsage();
        }
    }

    @Test
    public void testAsyncErrorInByteCode() {
        thrown.expect(QuickJSException.class);
        thrown.expectMessage("'aa' is not defined");

        try (QuickJSContext context = createContext()) {
            byte[] bytes = context.compile("async function test() { aa; } test();");
            context.execute(bytes);
        }
    }

    @Test
    public void testObjectToMap() {
        try (QuickJSContext context = createContext()) {
            JSObject ret = (JSObject) context.evaluate("var a = {'a': '123', 'b': [1, 2]};a.c = a;a;");
            assertEquals("{a=123, b=[1, 2], c=(this Map)}", ret.toMap().toString());
            ret.release();

            JSArray array = (JSArray) context.evaluate("var b = [{a: { c : 'xxx'}}, 2, 'qqq', 1.22]; b.push(b); b;");
            assertEquals("[{a={c=xxx}}, 2, qqq, 1.22, (this Collection)]", array.toArray().toString());
            array.release();

            JSObject emptyObj = (JSObject) context.evaluate("var a = { emptyArray: [] };a;");
            assertEquals("{emptyArray=[]}", emptyObj.toMap().toString());
            emptyObj.release();

        }
    }

    @Test
    public void testObjectToMapFilter() {
        try (QuickJSContext context = createContext()) {
            HashMap<String, Object> map = context.getGlobalObject().toMap((key, pointer, extra) -> key.equals("Math") || key.equals("Infinity"));
            assertEquals(7, map.size());
            assertEquals(map, map.get("globalThis"));
            assertEquals(new HashMap<>(), map.get("console"));
            assertEquals(new HashMap<>(), map.get("Reflect"));
            assertTrue(((Double) Objects.requireNonNull(map.get("NaN"))).isNaN());
            assertEquals(new HashMap<>(), map.get("JSON"));
            assertEquals(new HashMap<>(), map.get("Atomics"));
            assertNull(map.get("undefined"));
        }
    }

    @Test
    public void testObjectToCustomMap() {
        try (QuickJSContext context = createContext()) {
            StringBuilder expected = new StringBuilder("{");
            JSObject object = context.createNewJSObject();
            for (int i = 0; i < 100; i++) {
                object.setProperty(Integer.toString(i), i);
                expected.append(i).append("=").append(i);
                if (i != 99) {
                    expected.append(", ");
                }
            }
            expected.append("}");
            Map<String, Object> linkedMap = object.toMap(null, null, LinkedHashMap::new);
            HashMap<String, Object> hashMap = object.toMap();
            assertEquals(expected.toString(), linkedMap.toString());
            assertNotEquals(expected.toString(), hashMap.toString());
            object.release();
        }
    }

    @Test
    public void testObjectLeakDetection() {
        try (QuickJSContext context = createContext()) {
            context.setLeakDetectionListener((leak, stringValue) -> {
                assertEquals("{ name: 'leak1' }", stringValue);
            });

            // 泄漏场景
            JSObject o = context.createNewJSObject();
            o.setProperty("name", "leak1");
            // o.release();

        }
    }

    @Test
    public void testArrayBytes() {
        try (QuickJSContext context = createContext()) {
            byte[] bytes = "test测试".getBytes();
            byte[] buffer = (byte[]) context.evaluate("new Int8Array([116, 101, 115, 116, -26, -75, -117, -24, -81, -107]).buffer");
            assertArrayEquals(bytes, buffer);

            context.getGlobalObject().setProperty("testBuffer", bytes);
            byte[] testBuffers = context.getGlobalObject().getBytes("testBuffer");
            assertArrayEquals(testBuffers, buffer);

        }
    }

    @Test
    public void testArrayBytes1() {
        try (QuickJSContext context = createContext()) {
            JSFunction bufferTest = (JSFunction) context.evaluate("const bufferTest = (buffer) => { if(new Int8Array(buffer)[0] !== 116) { throw Error('failed, not equal'); }; }; bufferTest;");
            bufferTest.callVoid((Object) "test测试".getBytes());
        }
    }

    @Test
    public void testAsyncSourceFunc() {
        try (QuickJSContext context = createContext()) {
            byte[] compile = context.compile("async function testUpdate() {\n" +
                    "\tconsole.log(123);\n" +
                    "}\n" +
                    "testUpdate;");
            Object evaluate = context.execute(compile);
            if (evaluate instanceof JSFunction) {
                System.out.println("string: " + evaluate);
                ((JSFunction) evaluate).callVoid();
            }
        }
    }

    @Test
    public void testBaseModule() {
        try (QuickJSContext context = createContext()) {
            context.evaluateModule(readFile("test_base_module1.mjs"));
        }
    }

    @Test
    public void testEvalModuleReturn() {
        try (QuickJSContext context = createContext()) {
            assertEquals("[object Promise]", context.evaluateModule("1;").toString());
        }
    }

    @Test
    public void testDynamicImport() {
        try (QuickJSContext context = createContext()) {
            context.evaluateModule(readFile("test_module_import_dynamic.js"));
        }
    }

    @Test
    public void testArraySameRefToMap() {
        try (QuickJSContext context = createContext()){
            JSObject ret = (JSObject) context.evaluate("const a = [7]\n" +
                    "\n" +
                    "const b = {\n" +
                    "\tc: {\n" +
                    "\t\td: a\n" +
                    "\t},\n" +
                    "\te: a,\n" +
                    "\tg: null,\n" +
                    "\tk: undefined\n" +
                    "}\n" +
                    "\n" +
                    "b.f = b\n" +
                    "\n" +
                    "console.log(b)\n" +
                    "b;");
            HashMap<String, Object> map = ret.toMap();
            assertEquals(5, map.size());

            assertEquals(7, ((ArrayList<?>) Objects.requireNonNull(((HashMap<?, ?>) Objects.requireNonNull(map.get("c"))).get("d"))).get(0));
            assertEquals(7, ((ArrayList<?>) Objects.requireNonNull(map.get("e"))).get(0));
            assertNull(map.get("g"));
            assertNull(map.get("k"));
            assertEquals(map, map.get("f"));
        }
    }

    @Test
    public void testCallThrowError() {
        QuickJSContext context = createContext();
        JSFunction function = null;
        try {
            function = (JSFunction) context.evaluate("function testThrowError() {\n" +
                    "\tthrow new Error(\"test\");\n" +
                    "}\n" +
                    "\n" +
                    "testThrowError;");
            function.call();
        } catch (Exception e) {
            if (function != null) {
                // 测试
                function.release();
            }
            int defaultLength = 1;
            assertEquals(defaultLength, context.getObjectRecords().size());
            context.destroy();
        }
    }

}
