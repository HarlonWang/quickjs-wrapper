package com.whl.quickjs.wrapper.sample;

import com.whl.quickjs.wrapper.JSValue;
import com.whl.quickjs.wrapper.QuickJSContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class QuickJSFreeJSValueTest {

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
    public void freeJSValueTest() {
        context.evaluate("var a = 1;");
        JSValue globalObject = context.getGlobalObject();
    }

}
