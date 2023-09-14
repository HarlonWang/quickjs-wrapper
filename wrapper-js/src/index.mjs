import {existDir, loadFile, removeFile, writeToFile} from "./utils/max-node-fs.mjs";

const LINE = "\n"
const TAB = "    "

const DIR_EXTEND_LIBRARIES = "./wrapper-js/src/extend-libraries/"
const DIR_CPP = "./native/cpp/"
const FILE_CPP = DIR_CPP + "quickjs_extend_libraries.cpp"

if (existDir(FILE_CPP)) {
    removeFile(FILE_CPP)
}

(function init() {
    let code = ""

    // 以下开始写入扩展库代码

    // include 引用文件
    code += "#include \"../quickjs/quickjs.h\""
    code += LINE.repeat(2)

    // 写入 date-polyfill.js
    const datePVarName = "DATE_POLYFILL"
    const datePCode = loadFile(DIR_EXTEND_LIBRARIES + "date-polyfill.js")
    code += `const char* ${datePVarName} = R\"lit(`
    code += datePCode
    code += ")lit\";"
    code += LINE

    code += LINE.repeat(2)

    // 定义入口方法
    code += "void loadExtendLibraries(JSContext *ctx) {"
    code += LINE

    code += TAB
    code += `JS_Eval(ctx, ${datePVarName}, strlen(${datePVarName}), "date-polyfill.js", JS_EVAL_TYPE_GLOBAL);`

    // 入口方法结尾
    code += LINE
    code += "}"

    writeToFile(FILE_CPP, code)
})()
