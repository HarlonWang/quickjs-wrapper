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
        context.destroy();

        context = QuickJSContext.create();
        Object hello = context.execute(code);
        assertEquals(hello, "HELLO, WORLD!");
        context.destroy();
    }

    @Test
    public void testPromise() {
        context = QuickJSContext.create();
        byte[] bytes = context.compile("var ret; new Promise((resolve, reject) => { ret = 'resolved'; }); ret;");
        context.destroy();

        context = QuickJSContext.create();
        Object ret = context.execute(bytes);
        assertEquals(ret, "resolved");
        context.destroy();
    }

    @Test(expected = QuickJSException.class)
    public void testThrowErrorWithFileName() {
        context = QuickJSContext.create();
        byte[] bytes = context.compile("test;", "test.js");
        context.destroy();
        context = QuickJSContext.create();
        context.execute(bytes);
        context.destroy();
    }

    @Test
    public void testFreeValueReturnedOfExecute() {
        QuickJSLoader.startRedirectingStdoutStderr("quickjs_android");
        QuickJSContext context = QuickJSContext.create();
        QuickJSLoader.initConsoleLog(context);

        byte[] bytes = context.compile("test = () => { console.log('test'); }");
        context.execute(bytes);
        context.destroy();
    }

    @Test
    public void testCompileModule() {
        QuickJSContext context = QuickJSContext.create();
        QuickJSLoader.initConsoleLog(context);
        context.setModuleLoader(new QuickJSContext.BytecodeModuleLoader() {
            @Override
            public byte[] getModuleBytecode(String moduleName) {
                return context.compileModule("export const a = {name: 'test'};", moduleName);
            }
        });
        byte[] bytes = context.compileModule("import {a} from 'a.js'; if(a.name !== 'test') { throw new Error('failed') }", "aaa.js");
        context.execute(bytes);
        context.destroy();
    }

}
