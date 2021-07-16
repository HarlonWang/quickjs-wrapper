//
// Created by yonglan.whl on 2021/7/14.
//

#ifndef QUICKJS_TEST_CONTEXT_WRAPPER_H
#define QUICKJS_TEST_CONTEXT_WRAPPER_H

#include "quickjs/quickjs.h"


class ContextWrapper {
private:
    JSValue checkNotException(JSValue &value) const;
public:
    JSRuntime *runtime;
    JSContext *context;

    ContextWrapper();
    ~ContextWrapper();

    JSValue evaluate(const char *script, const char *file_name = "undefined.js", int eval_flag = JS_EVAL_TYPE_GLOBAL) const;
    JSValue getGlobalObject() const;
    int setProperty(JSValue &this_obj, const char *propName, JSValue &val) const;
    JSValue getProperty(JSValue &this_obj, const char *propName) const;
    JSValue call(JSValue &func_obj, JSValue &this_obj, int argc, JSValue *argv) const;
    const char* stringify(JSValue &value) const;

    // JS --> bytecode
    const char* compile(const char *script, int eval_flag = JS_EVAL_FLAG_COMPILE_ONLY | JS_EVAL_TYPE_GLOBAL) const;
    // bytecode --> result
    JSValue evaluate_binary(const char *script) const;
};

#endif //QUICKJS_TEST_CONTEXT_WRAPPER_H
