package com.whl.quickjs.wrapper;

import org.junit.Test;

/**
 * Created by Harlon Wang on 2021/10/12.
 */
public class QuickJSModuleTest {

    @Test
    public void testModule() {
        JSModule.setModuleLoader(new JSModule.Loader() {
            @Override
            public String getModuleScript(String moduleName) {
                return "export var name = 'Hello world';\n" +
                        "export var age = 18;";
            }
        });
        QuickJSContext context = QuickJSContext.create();

        ConsoleLogHelper.initConsole(context);

        context.evaluateModule("import {name, age} from './a.js';\n" +
                "\n" +
                "function onCreate() {\n" +
                "\tconsole.log('年龄：' + age);\n" +
                "}\n" +
                "\n" +
                "console.log('性别：' + name); onCreate();");
    }


}
