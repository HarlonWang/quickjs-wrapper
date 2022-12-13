package com.whl.quickjs.android;

import android.util.Log;

import com.whl.quickjs.wrapper.QuickJSContext;

/**
 * Created by Harlon Wang on 2022/8/12.
 */
public final class QuickJSLoader {

    public static void init() {
        System.loadLibrary("quickjs-android-wrapper");
    }

    public static void initConsoleLog(QuickJSContext context) {
        context.getGlobalObject().setProperty("nativeLog", args -> {
            String level = (String) args[0];
            String info = (String) args[1];
            String tag = "tiny-console";
            if (args.length > 2) {
                tag = (String) args[2];
            }

            switch (level) {
                case "info":
                    Log.i(tag, info);
                    break;
                case "warn":
                    Log.w(tag, info);
                    break;
                case "error":
                    Log.e(tag, info);
                    break;
                case "log":
                case "debug":
                default:
                    Log.d(tag, info);
                    break;
            }

            return null;
        });
        context.evaluate("const console = {\n" +
                "    log: (...args) => printLog(\"log\", ...args),\n" +
                "    debug: (...args) => printLog(\"debug\", ...args),\n" +
                "    info: (...args) => printLog(\"info\", ...args),\n" +
                "    warn: (...args) => printLog(\"warn\", ...args),\n" +
                "    error: (...args) => printLog(\"error\", ...args)\n" +
                "};\n" +
                "\n" +
                "const printLog = (level, ...args) => {\n" +
                "    let arg = '';\n" +
                "    if (args.length == 1) {\n" +
                "        let m = args[0];\n" +
                "        arg = __format_string(m);\n" +
                "    } else if (args.length > 1) {\n" +
                "        for (let i = 0; i < args.length; i++) {\n" +
                "            if (i > 0) {\n" +
                "                arg = arg.concat(', ');\n" +
                "            }\n" +
                "            let m = args[i];\n" +
                "            arg = arg.concat(__format_string(m));\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    nativeLog(level, arg);\n" +
                "};");
    }

    /**
     * Start threads to show stdout and stderr in logcat.
     * @param tag Android Tag
     */
    public native static void startRedirectingStdoutStderr(String tag);

}
