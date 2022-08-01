package com.whl.quickjs.wrapper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class QuickJSCompileTest {

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
    public void helloWorld() {
        byte[] code = context.compile("'hello, world!'.toUpperCase();");

        context.destroyContext();
        context = QuickJSContext.create();

        Object hello = context.execute(code);
        assertEquals(hello, "HELLO, WORLD!");
    }

    @Test
    public void testPromise() {
        byte[] bytes = context.compile("var ret; new Promise((resolve, reject) => { ret = 'resolved'; }); ret;");
        context.destroyContext();
        context = QuickJSContext.create();
        Object ret = context.execute(bytes);
        assertEquals(ret, "resolved");
    }

}
