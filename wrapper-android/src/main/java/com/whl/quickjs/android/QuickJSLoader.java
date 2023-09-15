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
        context.getGlobalObject().getJSObject("console").setProperty("stdout", args -> {
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
                        console.log(info);
                        break;
                }
            }

            return null;
        });
    }

    /**
     * Start threads to show stdout and stderr in logcat.
     * @param tag Android Tag
     */
    public native static void startRedirectingStdoutStderr(String tag);

}
