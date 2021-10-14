package com.whl.quickjs.wrapper;

import android.util.Log;

/**
 * Created by Harlon Wang on 2021/10/12.
 */
public class ConsoleLogHelper {

    public static void initConsole(QuickJSContext context) {
        context.evaluate("var console = {};");
        JSObject consoleObj = (JSObject) context.getGlobalObject().getProperty("console");
        consoleObj.setProperty("log", (JSCallFunction) args -> {
            Log.d("tiny-console", getInfo(args));
            return null;
        });
    }

    private static String getInfo(Object... objects) {
        StringBuilder b = new StringBuilder();
        for (Object o : objects) {
            b.append(o == null ? "null" : o.toString());
        }

        return b.toString();
    }

}
