package com.whl.quickjs.wrapper;

import android.util.Log;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by Harlon Wang on 2022/5/9.
 */
public class QuickJSPerformanceTest {

    private QuickJSContext context;

    @Before
    public void setup() {
        context = QuickJSContext.create();
        context.getGlobalObject().setProperty("test", args -> null);
        context.getGlobalObject().setProperty("testString", new JSCallFunction() {
            @Override
            public Object call(Object... args) {
                return args[0];
            }
        });
        context.getGlobalObject().setProperty("testLargeObject", new JSCallFunction() {
            @Override
            public Object call(Object... args) {
                JSObject obj = (JSObject) args[0];
                Log.d("qjs-console", obj.getStringProperty("key-0"));
                return obj;
            }
        });
    }

    @Test
    public void testOnce() {
        context.evaluate("var start = new Date().getTime();\n" +
                "test();\n" +
                "var end = new Date().getTime();\n" +
                "console.log('cost is', `${end - start}ms`);");
    }

    @Test
    public void testTenThousandTimes() {
        context.evaluate("var start = new Date().getTime();\n" +
                "for (var i = 0; i < 10000; i++) {\n" +
                "\ttest();\n" +
                "}\n" +
                "var end = new Date().getTime();\n" +
                "console.log('cost is', `${end - start}ms`);");
    }

    @Test
    public void testMillionTimes() {
        context.evaluate("var start = new Date().getTime();\n" +
                "for (var i = 0; i < 1000000; i++) {\n" +
                "\ttest();\n" +
                "}\n" +
                "var end = new Date().getTime();\n" +
                "console.log('cost is', `${end - start}ms`);");
    }

    @Test
    public void testWithLargeStringArgs() {
        context.evaluate("var start = new Date().getTime();\n" +
                "\n" +
                "var result = '';\n" +
                "for (var i = 0; i < 100000; i++) {\n" +
                "\tresult = result + \", index: \" + i;\n" +
                "}\n" +
                "var ret = testString(result);\n" +
                "\n" +
                "var end = new Date().getTime();\n" +
                "\n" +
                "console.log('cost is', `${end - start}ms, length: ${ret.length}`);");
    }

    @Test
    public void testWithLargeJSObjectArgs() {
        context.evaluate("var start = new Date().getTime();\n" +
                "var obj = {};\n" +
                "for(var i = 0; i < 10000000; i++){\n" +
                "  obj[`key-${i}`] = `如何通过js创建一个很大内存的对象？`\n" +
                "}\n" +
                "\n" +
                "var ret = testLargeObject(obj);\n" +
                "\n" +
                "var end = new Date().getTime();\n" +
                "console.log('cost is', `${end - start}ms`);");
    }

    @After
    public void tearDown() {
        context.destroyContext();
    }

}
