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

    public interface Console {
        void log(String info);
        void debug(String info);
        void info(String info);
        void warn(String info);
        void error(String info);
    }

    static final class AndroidConsole implements Console {

        private final String tag;

        public AndroidConsole(String tag) {
            this.tag = tag;
        }

        @Override
        public void log(String info) {
            Log.d(tag, info);
        }

        @Override
        public void debug(String info) {
            Log.d(tag, info);
        }

        @Override
        public void info(String info) {
            Log.i(tag, info);
        }

        @Override
        public void warn(String info) {
            Log.w(tag, info);
        }

        @Override
        public void error(String info) {
            Log.e(tag, info);
        }
    }

    public static void initConsoleLog(QuickJSContext context) {
        initConsoleLog(context, new AndroidConsole("quickjs"));
    }

    public static void initConsoleLog(QuickJSContext context, String tag) {
        initConsoleLog(context, new AndroidConsole(tag));
    }

    public static void initConsoleLog(QuickJSContext context, Console console) {
        context.getGlobalObject().setProperty("nativeLog", args -> {
            if (args.length == 2) {
                String level = (String) args[0];
                String info = (String) args[1];
                switch (level) {
                    case "info":
                        console.info(info);
                        break;
                    case "warn":
                        console.warn(info);
                        break;
                    case "error":
                        console.error(info);
                        break;
                    case "log":
                    case "debug":
                    default:
                        console.debug(info);
                        break;
                }
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
