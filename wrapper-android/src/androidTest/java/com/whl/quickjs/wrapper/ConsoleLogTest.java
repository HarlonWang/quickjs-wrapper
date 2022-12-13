package com.whl.quickjs.wrapper;

import static org.junit.Assert.assertEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.whl.quickjs.android.QuickJSLoader;;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by Harlon Wang on 2022/3/31.
 */
@RunWith(AndroidJUnit4.class)
public class ConsoleLogTest {

    private QuickJSContext context;

    @Before
    public void setUp() {
        QuickJSLoader.init();
        context = QuickJSContext.create();
    }

    @After
    public void tearDown() {
        QuickJSContext.destroy(context);
        QuickJSContext.destroyRuntime(context);
    }

    @Test
    public void testLogString() {
        QuickJSLoader.initConsoleLog(context, new QuickJSLoader.Console() {
            @Override
            public void log(String info) {
                assertEquals("hello log test", info);
            }

            @Override
            public void debug(String info) {

            }

            @Override
            public void info(String info) {

            }

            @Override
            public void warn(String info) {

            }

            @Override
            public void error(String info) {

            }
        });
        context.evaluate("console.log('hello log test');");
    }

    @Test
    public void testLogInteger() {
        QuickJSLoader.initConsoleLog(context, new QuickJSLoader.Console() {
            @Override
            public void log(String info) {
                assertEquals("123", info);
            }

            @Override
            public void debug(String info) {

            }

            @Override
            public void info(String info) {

            }

            @Override
            public void warn(String info) {

            }

            @Override
            public void error(String info) {

            }
        });
        context.evaluate("console.log(123);");
    }

    @Test
    public void testLogDouble() {
        QuickJSLoader.initConsoleLog(context, new QuickJSLoader.Console() {
            @Override
            public void log(String info) {
                assertEquals("1.23", info);
            }

            @Override
            public void debug(String info) {

            }

            @Override
            public void info(String info) {

            }

            @Override
            public void warn(String info) {

            }

            @Override
            public void error(String info) {

            }
        });
        context.evaluate("console.log(1.23);");
    }

    @Test
    public void testLogBoolean() {
        QuickJSLoader.initConsoleLog(context, new QuickJSLoader.Console() {
            @Override
            public void log(String info) {
                assertEquals("false", info);
            }

            @Override
            public void debug(String info) {

            }

            @Override
            public void info(String info) {

            }

            @Override
            public void warn(String info) {

            }

            @Override
            public void error(String info) {

            }
        });
        context.evaluate("console.log(false);");
    }

    @Test
    public void testLogArray() {
        QuickJSLoader.initConsoleLog(context, new QuickJSLoader.Console() {
            @Override
            public void log(String info) {
                assertEquals("[ 1, 2, 3 ]", info);
            }

            @Override
            public void debug(String info) {

            }

            @Override
            public void info(String info) {

            }

            @Override
            public void warn(String info) {

            }

            @Override
            public void error(String info) {

            }
        });
        context.evaluate("console.log([1, 2, 3]);");
    }

    @Test
    public void testLogArrowFunc() {
        QuickJSLoader.initConsoleLog(context, new QuickJSLoader.Console() {
            @Override
            public void log(String info) {
                assertEquals("function ()", info);
            }

            @Override
            public void debug(String info) {

            }

            @Override
            public void info(String info) {

            }

            @Override
            public void warn(String info) {

            }

            @Override
            public void error(String info) {

            }
        });
        context.evaluate("console.log(() => {});");
    }

    @Test
    public void testLogSimpleObj() {
        QuickJSLoader.initConsoleLog(context, new QuickJSLoader.Console() {
            @Override
            public void log(String info) {
                assertEquals("{ a: 1, b: 2 }", info);
            }

            @Override
            public void debug(String info) {

            }

            @Override
            public void info(String info) {

            }

            @Override
            public void warn(String info) {

            }

            @Override
            public void error(String info) {

            }
        });
        context.evaluate("console.log({ a: 1, b: 2 });");
    }

    @Test
    public void testLogNull() {
        QuickJSLoader.initConsoleLog(context, new QuickJSLoader.Console() {
            @Override
            public void log(String info) {
                assertEquals("null", info);
            }

            @Override
            public void debug(String info) {

            }

            @Override
            public void info(String info) {

            }

            @Override
            public void warn(String info) {

            }

            @Override
            public void error(String info) {

            }
        });
        context.evaluate("console.log(null);");
    }

    @Test
    public void testLogUndefined() {
        QuickJSLoader.initConsoleLog(context, new QuickJSLoader.Console() {
            @Override
            public void log(String info) {
                assertEquals("undefined", info);
            }

            @Override
            public void debug(String info) {

            }

            @Override
            public void info(String info) {

            }

            @Override
            public void warn(String info) {

            }

            @Override
            public void error(String info) {

            }
        });
        context.evaluate("console.log(undefined);");
    }

    @Test
    public void testLogArgs() {
        QuickJSLoader.initConsoleLog(context, new QuickJSLoader.Console() {
            @Override
            public void log(String info) {
                assertEquals("obj -> , { name: HarlonWang }", info);
            }

            @Override
            public void debug(String info) {

            }

            @Override
            public void info(String info) {

            }

            @Override
            public void warn(String info) {

            }

            @Override
            public void error(String info) {

            }
        });
        context.evaluate("console.log('obj -> ', { name: 'HarlonWang' });");
    }

    @Test
    public void testLogConcat() {
        QuickJSLoader.initConsoleLog(context, new QuickJSLoader.Console() {
            @Override
            public void log(String info) {
                assertEquals("obj -> [object Object]", info);
            }

            @Override
            public void debug(String info) {

            }

            @Override
            public void info(String info) {

            }

            @Override
            public void warn(String info) {

            }

            @Override
            public void error(String info) {

            }
        });
        context.evaluate("console.log('obj -> ' + { name: 'HarlonWang' });");
    }

    @Test
    public void testLogDefaultLevel() {
        QuickJSLoader.initConsoleLog(context, new QuickJSLoader.Console() {
            @Override
            public void log(String info) {
                assertEquals("log", info);
            }

            @Override
            public void debug(String info) {

            }

            @Override
            public void info(String info) {

            }

            @Override
            public void warn(String info) {

            }

            @Override
            public void error(String info) {

            }
        });
        context.evaluate("console.log('log');");
    }

    @Test
    public void testLogDebugLevel() {
        QuickJSLoader.initConsoleLog(context, new QuickJSLoader.Console() {
            @Override
            public void log(String info) {

            }

            @Override
            public void debug(String info) {
                assertEquals("debug", info);
            }

            @Override
            public void info(String info) {

            }

            @Override
            public void warn(String info) {

            }

            @Override
            public void error(String info) {

            }
        });
        context.evaluate("console.debug('debug');");
    }

    @Test
    public void testLogInfoLevel() {
        QuickJSLoader.initConsoleLog(context, new QuickJSLoader.Console() {
            @Override
            public void log(String info) {

            }

            @Override
            public void debug(String info) {

            }

            @Override
            public void info(String info) {
                assertEquals("info", info);
            }

            @Override
            public void warn(String info) {

            }

            @Override
            public void error(String info) {

            }
        });
        context.evaluate("console.info('info');");
    }

    @Test
    public void testLogWarnLevel() {
        QuickJSLoader.initConsoleLog(context, new QuickJSLoader.Console() {
            @Override
            public void log(String info) {

            }

            @Override
            public void debug(String info) {

            }

            @Override
            public void info(String info) {

            }

            @Override
            public void warn(String info) {
                assertEquals("warn", info);
            }

            @Override
            public void error(String info) {

            }
        });
        context.evaluate("console.warn('warn');");
    }

    @Test
    public void testLogErrorLevel() {
        QuickJSLoader.initConsoleLog(context, new QuickJSLoader.Console() {
            @Override
            public void log(String info) {

            }

            @Override
            public void debug(String info) {

            }

            @Override
            public void info(String info) {

            }

            @Override
            public void warn(String info) {

            }

            @Override
            public void error(String info) {
                assertEquals("error", info);
            }
        });
        context.evaluate("console.error('error');");
    }

    @Test
    public void testLogStringLengthNoLimited() {
        QuickJSLoader.initConsoleLog(context, new QuickJSLoader.Console() {
            @Override
            public void log(String info) {
                assertEquals(596, info.length());
            }

            @Override
            public void debug(String info) {

            }

            @Override
            public void info(String info) {

            }

            @Override
            public void warn(String info) {

            }

            @Override
            public void error(String info) {

            }
        });
        context.evaluate("var print_text = 'print_text';\n" +
                        "var print_texts = 'print_texts';\n" +
                        "\n" +
                        "var a = { command: print_texts, params: { type: 6, columns: [ { colsWidthArr: 1, command: print_text, align: 0, anticolor: 0, bold: 0, size: 38, text: '名称', textspace: 0, type: 1, underline: 0 }, { colsWidthArr: 1, command: print_text, align: 0, anticolor: 0, bold: 0, size: 24, text: '单价', textspace: 0, type: 1, underline: 0 }, { colsWidthArr: 1, command: print_text, align: 0, anticolor: 0, bold: 0, size: 24, text: '单位', textspace: 0, type: 1, underline: 0 }, { colsWidthArr: 1, command: print_text, align: 0, anticolor: 0, bold: 1, size: 12, text: '总价111', textspace: 1, type: 1, underline: 1 } ] } };");

        context.evaluate("var b = JSON.stringify(a); console.log(__format_string(b));");
    }

    @Test
    public void testLogErrorMessage() {
        QuickJSLoader.initConsoleLog(context, new QuickJSLoader.Console() {
            @Override
            public void log(String info) {
                assertEquals("----, TypeError: not a function", info);
            }

            @Override
            public void debug(String info) {

            }

            @Override
            public void info(String info) {

            }

            @Override
            public void warn(String info) {

            }

            @Override
            public void error(String info) {

            }
        });
        context.evaluate("try {\n" +
                "\tvar a = '123';\n" +
                "\ta.push(456);\n" +
                "} catch(err) {\n" +
                "\tconsole.log('----', err);\n" +
                "}");
    }

}
