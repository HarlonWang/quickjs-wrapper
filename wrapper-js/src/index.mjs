import {existDir, loadFile, removeFile, writeToFile} from "./utils/max-node-fs.mjs";

const LINE = "\n"
const TAB = "    "

const DIR_EXTEND_LIBRARIES = "./wrapper-js/extend/libraries/"
const DIR_CPP = "./native/cpp/"
const FILE_H = DIR_CPP + "quickjs_extend_libraries.h"

if (existDir(FILE_H)) {
    removeFile(FILE_H)
}

(function init() {
    let code = ""

    // 以下开始写入扩展库代码

    code += "#ifndef QUICKJS_EXTEND_LIBRARIES" + LINE
    code += "#define QUICKJS_EXTEND_LIBRARIES" + LINE
    code += LINE

    // include 引用文件
    code += `#include <string>` + LINE
    code += `#include <cstring>` + LINE
    code += `#include "../quickjs/quickjs.h"` + LINE
    code += LINE

    // 写入 date-polyfill.js
    const datePVarName = "DATE_POLYFILL"
    const datePCode = loadFile(DIR_EXTEND_LIBRARIES + "date-polyfill.js")
    code += `const char *${datePVarName} = R\"lit(`
    code += datePCode
    code += ")lit\";"
    code += LINE

    code += LINE

    // 写入 console.js
    const consoleVarName = "CONSOLE"
    const consoleCode = loadFile(DIR_EXTEND_LIBRARIES + "console.js")
    code += `const char *${consoleVarName} = R\"lit(`
    code += consoleCode
    code += ")lit\";"
    code += LINE

    code += LINE.repeat(2)

    // 定义入口方法
    code += "static inline void loadExtendLibraries(JSContext *ctx) {"
    code += LINE

    code += TAB
    code += `JS_FreeValue(ctx, JS_Eval(ctx, ${datePVarName}, strlen(${datePVarName}), "date-polyfill.js", JS_EVAL_TYPE_GLOBAL));`
    code += LINE

    code += TAB
    code += `JS_FreeValue(ctx, JS_Eval(ctx, ${consoleVarName}, strlen(${consoleVarName}), "console.js", JS_EVAL_TYPE_GLOBAL));`
    code += LINE

    // 入口方法结尾
    code += "}"

    code += LINE.repeat(2)
    code += "#endif //QUICKJS_EXTEND_LIBRARIES"

    writeToFile(FILE_H, code)
})()
