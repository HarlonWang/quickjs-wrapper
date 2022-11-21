package com.whl.quickjs.wrapper;

import org.junit.Test;

import static org.junit.Assert.*;

import com.whl.quickjs.android.QuickJSLoader;

/**
 * Created by Harlon Wang on 2021/10/12.
 */
public class QuickJSModuleTest {

    @Test
    public void testModule() {
        QuickJSLoader.init();
        JSModule.setModuleLoader(moduleName -> {
            if (moduleName.equals("a.js")) {
                return "export var name = 'Jack';\n" +
                        "export var age = 18;";
            }

            return null;
        });
        QuickJSContext context = QuickJSContext.create();
        context.getGlobalObject().setProperty("assertName", args -> {
            assertEquals("Jack", args[0]);
            return null;
        });
        context.getGlobalObject().setProperty("assertAge", args -> {
            assertEquals(18, args[0]);
            return null;
        });
        context.getGlobalObject().setProperty("assertNameUpdated", args -> {
            assertEquals("TypeError: 'name' is read-only", args[0].toString());
            return null;
        });
        context.evaluateModule("import {name, age} from './a.js';\n" +
                "\n" +
                "assertName(name);\n" +
                "assertAge(age);\n" +
                "new Promise((resolve, reject) => { name = 'Updated'; }).catch((res) => { assertNameUpdated(res); });");

        context.destroy();
        QuickJSContext.destroyRuntime(context);
    }


}
