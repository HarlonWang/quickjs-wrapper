//
// Created by yonglan.whl on 2021/7/14.
//

#ifndef QUICKJS_TEST_CONTEXT_WRAPPER_H
#define QUICKJS_TEST_CONTEXT_WRAPPER_H

#include <iostream>
#include <set>
using namespace std;

#include "quickjs/quickjs.h"
#include <jni.h>

class QuickJSWrapper {
    JSValue checkNotException(JSValue &value) const;
public:
    JNIEnv *env;
    JSRuntime *runtime;
    JSContext *context;

    set<jlong> values;

    jclass booleanClass;
    jclass integerClass;
    jclass doubleClass;
    jclass longClass;
    jclass jsObjectClass;
    jclass jsArrayClass;
    jmethodID booleanValueOf;
    jmethodID integerValueOf;
    jmethodID doubleValueOf;
    jmethodID longValueOf;
    jmethodID jsObjectInit;
    jmethodID jsArrayInit;

    QuickJSWrapper(JNIEnv *env);
    ~QuickJSWrapper();

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
