package com.whl.quickjs.wrapper;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import com.whl.quickjs.android.QuickJSLoader;

public class QuickJSCompileTest {

    private QuickJSContext context;

    @Before
    public void setup() {
        QuickJSLoader.init();
    }

    @Test
    public void helloWorld() {
        context = QuickJSContext.create();
        byte[] code = context.compile("'hello, world!'.toUpperCase();");
        QuickJSContext.destroy(context);
        QuickJSContext.destroyRuntime(context);

        context = QuickJSContext.create();
        Object hello = context.execute(code);
        assertEquals(hello, "HELLO, WORLD!");
        QuickJSContext.destroy(context);
        QuickJSContext.destroyRuntime(context);
    }

    @Test
    public void testPromise() {
        context = QuickJSContext.create();
        byte[] bytes = context.compile("var ret; new Promise((resolve, reject) => { ret = 'resolved'; }); ret;");
        QuickJSContext.destroy(context);
        QuickJSContext.destroyRuntime(context);

        context = QuickJSContext.create();
        Object ret = context.execute(bytes);
        assertEquals(ret, "resolved");
        QuickJSContext.destroy(context);
        QuickJSContext.destroyRuntime(context);
    }

    @Test(expected = QuickJSException.class)
    public void testThrowErrorWithFileName() {
        context = QuickJSContext.create();
        byte[] bytes = context.compile("test;", "test.js");
        QuickJSContext.destroy(context);
        QuickJSContext.destroyRuntime(context);
        context = QuickJSContext.create();
        context.execute(bytes);
        QuickJSContext.destroy(context);
        QuickJSContext.destroyRuntime(context);
    }

}
