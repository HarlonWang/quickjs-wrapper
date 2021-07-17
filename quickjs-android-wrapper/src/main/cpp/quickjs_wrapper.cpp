//
// Created by yonglan.whl on 2021/7/14.
//
#include <iostream>
using namespace std;

#include "quickjs_wrapper.h"

QuickJSWrapper::QuickJSWrapper() {
    runtime = JS_NewRuntime();
    context = JS_NewContext(runtime);
}

QuickJSWrapper::~QuickJSWrapper() {
    JS_FreeContext(context);
    JS_FreeRuntime(runtime);
}

JSValue QuickJSWrapper::evaluate(const char *script, const char *file_name, int eval_flag) const {
    JSValue val = JS_Eval(context, script, strlen(script), file_name, eval_flag);
    return checkNotException(val);
}

JSValue QuickJSWrapper::getGlobalObject() const {
    JSValue val = JS_GetGlobalObject(context);
    return checkNotException(val);
}

JSValue QuickJSWrapper::getProperty(JSValue &this_obj, const char *propName) const {
    JSValue val = JS_GetPropertyStr(context, this_obj, propName);
    return checkNotException(val);
}

int QuickJSWrapper::setProperty(JSValue &this_obj, const char *propName, JSValue &val) const {
    return JS_SetPropertyStr(context, this_obj, propName, val);
}

JSValue QuickJSWrapper::call(JSValue &func_obj, JSValue &this_obj, int argc, JSValue *argv) const {
    JSValue val = JS_Call(context, func_obj, this_obj, argc, argv);
    return checkNotException(val);
}

const char * QuickJSWrapper::stringify(JSValue &value) const {
    JSValue obj = JS_JSONStringify(context, value, JS_UNDEFINED, JS_UNDEFINED);
    return JS_ToCString(context, checkNotException(obj));
}

JSValue QuickJSWrapper::checkNotException(JSValue &value) const {
    if (JS_IsException(value)) {
        JSValue exception = JS_GetException(context);
        const char* excStr = JS_ToCString(context, exception);
        throw runtime_error(excStr);
    }

    return value;
}