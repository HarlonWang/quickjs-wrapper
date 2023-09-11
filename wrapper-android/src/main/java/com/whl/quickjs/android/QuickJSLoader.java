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
                        console.log(info);
                        break;
                }
            }

            return null;
        });
        // todo 临时使用，后续改为模块导入方式
        context.evaluate("const LINE = \"\\n\"\n" +
                "const TAB = \"  \"\n" +
                "const SPACE = \" \"\n" +
                "\n" +
                "function format(value, opt) {\n" +
                "    const defaultOpt = {\n" +
                "        maxStringLength: 10000,\n" +
                "        depth: 2,\n" +
                "        maxArrayLength: 100,\n" +
                "        seen: [],\n" +
                "        reduceStringLength: 100\n" +
                "    }\n" +
                "    if (!opt) {\n" +
                "        opt = defaultOpt\n" +
                "    } else {\n" +
                "        opt = Object.assign(defaultOpt, opt)\n" +
                "    }\n" +
                "\n" +
                "    return formatValue(value, opt, 0)\n" +
                "}\n" +
                "\n" +
                "function formatValue(value, opt, recurseTimes) {\n" +
                "    if (typeof value !== 'object' && typeof value !== 'function') {\n" +
                "        return formatPrimitive(value, opt)\n" +
                "    }\n" +
                "\n" +
                "    if (value === null) {\n" +
                "        return 'null'\n" +
                "    }\n" +
                "\n" +
                "    if (typeof value === 'function') {\n" +
                "        return formatFunction(value)\n" +
                "    }\n" +
                "\n" +
                "    if (typeof value === 'object') {\n" +
                "        if (opt.seen.includes(value)) {\n" +
                "            let index = 1\n" +
                "            if (opt.circular === undefined) {\n" +
                "                opt.circular = new Map()\n" +
                "                opt.circular.set(value, index)\n" +
                "            } else {\n" +
                "                index = opt.circular.get(value)\n" +
                "                if (index === undefined) {\n" +
                "                    index = opt.circular.size + 1\n" +
                "                    opt.circular.set(value, index)\n" +
                "                }\n" +
                "            }\n" +
                "\n" +
                "            return `[Circular *${index}]`\n" +
                "        }\n" +
                "\n" +
                "        if (opt.depth !== null && ((recurseTimes - 1) === opt.depth)) {\n" +
                "            if (value instanceof Array) {\n" +
                "                return '[Array]'\n" +
                "            }\n" +
                "            return '[Object]'\n" +
                "        }\n" +
                "\n" +
                "        recurseTimes++\n" +
                "        opt.seen.push(value)\n" +
                "        const string = formatObject(value, opt, recurseTimes)\n" +
                "        opt.seen.pop()\n" +
                "        return string\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "function formatObject(value, opt, recurseTimes) {\n" +
                "    if (value instanceof RegExp) {\n" +
                "        return `${value.toString()}`\n" +
                "    }\n" +
                "\n" +
                "    if (value instanceof Promise) {\n" +
                "        // quickjs 环境下通过 native 提供的方式获取 Promise 状态\n" +
                "        if (typeof getPromiseState !== \"undefined\") {\n" +
                "            const { result, state} = getPromiseState(value)\n" +
                "            if (state === 'fulfilled') {\n" +
                "                return `Promise { ${formatValue(result, opt, recurseTimes)} }`\n" +
                "            } else if (state === 'rejected'){\n" +
                "                return `Promise { <rejected> ${formatValue(result, opt, recurseTimes)} }`\n" +
                "            } else if (state === 'pending'){\n" +
                "                return `Promise { <pending> }`\n" +
                "            }\n" +
                "        } else {\n" +
                "            return `Promise {${formatValue(value, opt, recurseTimes)}}`\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    if (value instanceof Array) {\n" +
                "        return formatArray(value, opt, recurseTimes)\n" +
                "    }\n" +
                "\n" +
                "    if (value instanceof Float64Array) {\n" +
                "        return `Float64Array(1) [ ${value} ]`\n" +
                "    }\n" +
                "\n" +
                "    if (value instanceof BigInt64Array) {\n" +
                "        return `BigInt64Array(1) [ ${value}n ]`\n" +
                "    }\n" +
                "\n" +
                "    if (value instanceof Map) {\n" +
                "        return formatMap(value, opt, recurseTimes)\n" +
                "    }\n" +
                "\n" +
                "    return formatProperty(value, opt, recurseTimes)\n" +
                "}\n" +
                "\n" +
                "function formatProperty(value, opt, recurseTimes) {\n" +
                "    let string = ''\n" +
                "    string += '{'\n" +
                "    const keys = Object.keys(value)\n" +
                "    const length = keys.length\n" +
                "    for (let i = 0; i < length; i++) {\n" +
                "        if (i === 0) {\n" +
                "            string += SPACE\n" +
                "        }\n" +
                "        string += LINE\n" +
                "        string += TAB.repeat(recurseTimes)\n" +
                "\n" +
                "        const key = keys[i]\n" +
                "        string += `${key}: `\n" +
                "        string += formatValue(value[key], opt, recurseTimes)\n" +
                "        if (i < length -1) {\n" +
                "            string += ','\n" +
                "        }\n" +
                "        string += SPACE\n" +
                "    }\n" +
                "\n" +
                "    string += LINE\n" +
                "    string += TAB.repeat(recurseTimes - 1)\n" +
                "    string += '}'\n" +
                "\n" +
                "    if (string.length < opt.reduceStringLength) {\n" +
                "        string = string.replaceAll(LINE, \"\").replaceAll(TAB, \"\")\n" +
                "    }\n" +
                "\n" +
                "    return string\n" +
                "}\n" +
                "\n" +
                "function formatMap(value, opt, recurseTimes) {\n" +
                "    let string = `Map(${value.size}) `\n" +
                "    string += '{'\n" +
                "    let isEmpty = true\n" +
                "    value.forEach((v, k, map) => {\n" +
                "        isEmpty = false\n" +
                "        string += ` ${format(k, opt, recurseTimes)} => ${format(v, opt, recurseTimes)}`\n" +
                "        string += ','\n" +
                "    })\n" +
                "\n" +
                "    if (!isEmpty) {\n" +
                "        // 删除最后多余的逗号\n" +
                "        string = string.substr(0, string.length -1) + ' '\n" +
                "    }\n" +
                "\n" +
                "    string += '}'\n" +
                "    return string\n" +
                "}\n" +
                "\n" +
                "function formatArray(value, opt, recurseTimes) {\n" +
                "    let string = '['\n" +
                "    value.forEach((item, index, array) => {\n" +
                "        if (index === 0) {\n" +
                "            string += ' '\n" +
                "        }\n" +
                "        string += formatValue(item, opt, recurseTimes)\n" +
                "        if (index === opt.maxArrayLength - 1) {\n" +
                "            string += `... ${array.length - opt.maxArrayLength} more item${array.length - opt.maxArrayLength > 1 ? 's' : ''}`\n" +
                "        } else if (index !== array.length - 1) {\n" +
                "            string += ','\n" +
                "        }\n" +
                "        string += ' '\n" +
                "    })\n" +
                "    string += ']'\n" +
                "    return string\n" +
                "}\n" +
                "\n" +
                "function formatFunction(value) {\n" +
                "    let type = 'Function'\n" +
                "\n" +
                "    if (value.constructor.name === 'AsyncFunction') {\n" +
                "        type = 'AsyncFunction'\n" +
                "    }\n" +
                "\n" +
                "    if (value.constructor.name === 'GeneratorFunction') {\n" +
                "        type = 'GeneratorFunction'\n" +
                "    }\n" +
                "\n" +
                "    if (value.constructor.name === 'AsyncGeneratorFunction') {\n" +
                "        type = 'AsyncGeneratorFunction'\n" +
                "    }\n" +
                "\n" +
                "    let fn = `${value.name ? `: ${value.name}` : ' (anonymous)'}`\n" +
                "    return `[${type + fn}]`\n" +
                "}\n" +
                "\n" +
                "function formatPrimitive(value, opt) {\n" +
                "    const type = typeof value\n" +
                "    switch (type) {\n" +
                "        case \"string\":\n" +
                "            return formatString(value, opt)\n" +
                "        case \"number\":\n" +
                "            return Object.is(value, -0) ? '-0' : `${value}`\n" +
                "        case \"bigint\":\n" +
                "            return `${String(value)}n`\n" +
                "        case \"boolean\":\n" +
                "            return `${value}`\n" +
                "        case \"undefined\":\n" +
                "            return \"undefined\"\n" +
                "        case \"symbol\":\n" +
                "            return `${value.toString()}`\n" +
                "        default:\n" +
                "            return value.toString\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "function formatString(value, opt) {\n" +
                "    let trailer = ''\n" +
                "    if (opt.maxStringLength && value.length > opt.maxStringLength) {\n" +
                "        const remaining = value.length - opt.maxStringLength\n" +
                "        value = value.slice(0, opt.maxStringLength)\n" +
                "        trailer = `... ${remaining} more character${remaining > 1 ? 's' : ''}`\n" +
                "    }\n" +
                "\n" +
                "    return `'${value}'${trailer}`\n" +
                "}", "format.js");
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
                "        arg = format(m);\n" +
                "    } else if (args.length > 1) {\n" +
                "        for (let i = 0; i < args.length; i++) {\n" +
                "            if (i > 0) {\n" +
                "                arg = arg.concat(', ');\n" +
                "            }\n" +
                "            let m = args[i];\n" +
                "            arg = arg.concat(format(m));\n" +
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
