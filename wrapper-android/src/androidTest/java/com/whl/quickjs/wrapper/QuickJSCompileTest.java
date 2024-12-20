package com.whl.quickjs.wrapper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.*;

import com.whl.quickjs.android.QuickJSLoader;

public class QuickJSCompileTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setup() {
        QuickJSLoader.init();
    }

    @Test
    public void helloWorld() {
        try (QuickJSContext context = QuickJSContext.create()){
            byte[] code = context.compile("'hello, world!'.toUpperCase();");
            Object hello = context.execute(code);
            assertEquals(hello, "HELLO, WORLD!");
        }
    }

    @Test
    public void testDifferentContexts() {
        byte[] code;
        try (QuickJSContext context = QuickJSContext.create()){
            code = context.compile("'hello, world!'.toUpperCase();");
        }

        try (QuickJSContext context = QuickJSContext.create()){
            Object hello = context.execute(code);
            assertEquals(hello, "HELLO, WORLD!");
        }
    }

    @Test
    public void testPromise() {
        try (QuickJSContext context = QuickJSContext.create()){
            byte[] bytes = context.compile("var ret; new Promise((resolve, reject) => { ret = 'resolved'; }); ret;");
            Object ret = context.execute(bytes);
            assertEquals(ret, "resolved");
        }
    }

    @Test(expected = QuickJSException.class)
    public void testThrowErrorWithFileName() {
        try (QuickJSContext context = QuickJSContext.create()){
            byte[] bytes = context.compile("test;", "test.js");
            context.execute(bytes);
        }
    }

    @Test
    public void testFreeValueReturnedOfExecute() {
        QuickJSLoader.startRedirectingStdoutStderr("quickjs_android");
        QuickJSContext context = QuickJSContext.create();
        QuickJSLoader.initConsoleLog(context);

        byte[] bytes = context.compile("test = () => { console.log('test'); }");
        JSObject ret = (JSObject) context.execute(bytes);
        ret.release();
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

    @Test
    public void testCompileModuleWithoutModuleLoader() {
        thrown.expect(QuickJSException.class);
        thrown.expectMessage("Failed to load module, the ModuleLoader can not be null!");

        QuickJSContext context = QuickJSContext.create();
        context.compileModule("import { a } from 'a.js';");
        context.destroy();
    }

    @Test
    public void testCompileModuleWithMockModuleLoader() {
        thrown.expect(QuickJSException.class);
        thrown.expectMessage("Could not find export 'a' in module 'a.js'");

        QuickJSContext context = QuickJSContext.create();
        context.setModuleLoader(new QuickJSContext.DefaultModuleLoader() {
            @Override
            public String getModuleStringCode(String moduleName) {
                return "";
            }
        });
        // 在 ModuleLoader 中返回空字符串，可以实现仅编译当前模块字节码，而不用编译它所依赖的模块
        byte[] bytes = context.compileModule("import { a } from 'a.js';");
        context.execute(bytes);
        context.destroy();
    }

    @Test
    public void testStringCodeModuleLoaderReturnNull() {
        thrown.expect(QuickJSException.class);
        thrown.expectMessage("Failed to load module, cause string code was null!");

        QuickJSContext context = QuickJSContext.create();
        context.setModuleLoader(new QuickJSContext.DefaultModuleLoader() {
            @Override
            public String getModuleStringCode(String moduleName) {
                return null;
            }
        });
        context.compileModule("import { a } from 'a.js';");
        context.destroy();
    }

    @Test
    public void testBytecodeModuleLoaderReturnNull() {
        thrown.expect(QuickJSException.class);
        thrown.expectMessage("Failed to load module, cause bytecode was null!");

        QuickJSContext context = QuickJSContext.create();
        context.setModuleLoader(new QuickJSContext.BytecodeModuleLoader() {
            @Override
            public byte[] getModuleBytecode(String moduleName) {
                return null;
            }
        });
        context.compileModule("import { a } from 'a.js';");
        context.destroy();
    }

}
