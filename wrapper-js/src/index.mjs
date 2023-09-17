import {existDir, loadFile, removeFile, writeToFile} from "./utils/max-node-fs.mjs";

const LINE = "\n"
const TAB = "    "

const DIR_EXTEND_LIBRARIES = "./wrapper-js/extend/libraries/"
const DIR_CPP = "./native/cpp/"
const FILE_CPP = DIR_CPP + "quickjs_extend_libraries.cpp"

if (existDir(FILE_CPP)) {
    removeFile(FILE_CPP)
}

(function init() {
    let code = ""

    // 以下开始写入扩展库代码

    // include 引用文件
    code += `#include <string>` + LINE
    code += `#include "../quickjs/quickjs.h"` + LINE
    code += LINE

    // 写入 date-polyfill.js
    const datePVarName = "DATE_POLYFILL"
    const datePCode = loadFile(DIR_EXTEND_LIBRARIES + "date-polyfill.js")
    code += `const char *${datePVarName} = R\"lit(`
    code += datePCode
    code += ")lit\";"
    code += LINE

    code += LINE.repeat(2)

    // 写入 console.js
    const consoleVarName = "CONSOLE"
    const consoleCode = loadFile(DIR_EXTEND_LIBRARIES + "console.js")
    code += `const char *${consoleVarName} = R\"lit(`
    code += consoleCode
    code += ")lit\";"
    code += LINE

    code += LINE.repeat(2)

    // 定义入口方法
    code += "void loadExtendLibraries(JSContext *ctx) {"
    code += LINE

    code += TAB
    code += `JS_Eval(ctx, ${datePVarName}, strlen(${datePVarName}), "date-polyfill.js", JS_EVAL_TYPE_GLOBAL);`
    code += LINE
    
    code += TAB
    code += `JS_FreeValue(ctx, JS_Eval(ctx, ${consoleVarName}, strlen(${consoleVarName}), "console.js", JS_EVAL_TYPE_GLOBAL));`
    code += LINE

    // 入口方法结尾
    code += "}"

    writeToFile(FILE_CPP, code)
})()
