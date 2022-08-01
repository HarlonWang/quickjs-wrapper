package com.whl.quickjs.wrapper;

import org.junit.Test;

/**
 * Created by Harlon Wang on 2021/10/12.
 */
public class QuickJSModuleTest {

    @Test
    public void testModule() {
        JSModule.setModuleLoader(moduleName -> {
            if (moduleName.equals("a.js")) {
                return "export var name = 'Jack';\n" +
                        "export var age = 18;";
            }

            return null;
        });
        QuickJSContext context = QuickJSContext.create();
        context.evaluateModule("import {name, age} from './a.js';\n" +
                "\n" +
                "console.log('name：' + name);\n" +
                "console.log('age：' + age);\n" +
                "new Promise((resolve, reject) => { name = 'Updated'; }).catch((res) => { console.log(res); });\n" +
                "console.log(name);");
    }


}
