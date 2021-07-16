//
// Created by yonglan.whl on 2021/7/14.
//
#include <iostream>
using namespace std;

#include "context_wrapper.h"

ContextWrapper::ContextWrapper() {
    runtime = JS_NewRuntime();
    context = JS_NewContext(runtime);
}

ContextWrapper::~ContextWrapper() {
    JS_FreeContext(context);
    JS_FreeRuntime(runtime);
}

JSValue ContextWrapper::evaluate(const char *script, const char *file_name, int eval_flag) const {
    JSValue val = JS_Eval(context, script, strlen(script), file_name, eval_flag);
    return checkNotException(val);
}

JSValue ContextWrapper::getGlobalObject() const {
    JSValue val = JS_GetGlobalObject(context);
    return checkNotException(val);
}

JSValue ContextWrapper::getProperty(JSValue &this_obj, const char *propName) const {
    JSValue val = JS_GetPropertyStr(context, this_obj, propName);
    return checkNotException(val);
}

int ContextWrapper::setProperty(JSValue &this_obj, const char *propName, JSValue &val) const {
    return JS_SetPropertyStr(context, this_obj, propName, val);
}

JSValue ContextWrapper::call(JSValue &func_obj, JSValue &this_obj, int argc, JSValue *argv) const {
    JSValue val = JS_Call(context, func_obj, this_obj, argc, argv);
    return checkNotException(val);
}

const char * ContextWrapper::stringify(JSValue &value) const {
    JSValue obj = JS_JSONStringify(context, value, JS_UNDEFINED, JS_UNDEFINED);
    return JS_ToCString(context, checkNotException(obj));
}

JSValue ContextWrapper::checkNotException(JSValue &value) const {
    if (JS_IsException(value)) {
        JSValue exception = JS_GetException(context);
        const char* excStr = JS_ToCString(context, exception);
        throw runtime_error(excStr);
    }

    return value;
}