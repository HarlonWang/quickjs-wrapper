//
// Created by yonglan.whl on 2021/7/14.
//
#include "quickjs_wrapper.h"

QuickJSWrapper::QuickJSWrapper(JNIEnv *jniEnv) {
    env = jniEnv;
    runtime = JS_NewRuntime();
    context = JS_NewContext(runtime);

    booleanClass = static_cast<jclass>(env->NewGlobalRef(env->FindClass("java/lang/Boolean")));
    integerClass = static_cast<jclass>(env->NewGlobalRef(env->FindClass("java/lang/Integer")));
    doubleClass = static_cast<jclass>(env->NewGlobalRef(env->FindClass("java/lang/Double")));
    jsObjectClass = static_cast<jclass>(env->NewGlobalRef(env->FindClass("com/whl/quickjs/wrapper/JSObject")));
    jsArrayClass = static_cast<jclass>(env->NewGlobalRef(env->FindClass("com/whl/quickjs/wrapper/JSArray")));
    jsFunctionClass = static_cast<jclass>(env->NewGlobalRef(env->FindClass("com/whl/quickjs/wrapper/JSFunction")));

    booleanValueOf = env->GetStaticMethodID(booleanClass, "valueOf", "(Z)Ljava/lang/Boolean;");
    integerValueOf = env->GetStaticMethodID(integerClass, "valueOf", "(I)Ljava/lang/Integer;");
    doubleValueOf = env->GetStaticMethodID(doubleClass, "valueOf", "(D)Ljava/lang/Double;");

    booleanGetValue = env->GetMethodID(booleanClass, "booleanValue", "()Z");
    integerGetValue = env->GetMethodID(integerClass, "intValue", "()I");
    doubleGetValue = env->GetMethodID(doubleClass, "doubleValue", "()D");

    jsObjectInit = env->GetMethodID(jsObjectClass, "<init>", "(Lcom/whl/quickjs/wrapper/QuickJSContext;J)V");
    jsArrayInit = env->GetMethodID(jsArrayClass, "<init>", "(Lcom/whl/quickjs/wrapper/QuickJSContext;J)V");
    jsFunctionInit = env->GetMethodID(jsFunctionClass, "<init>","(Lcom/whl/quickjs/wrapper/QuickJSContext;J)V");
}

QuickJSWrapper::~QuickJSWrapper() {
    env->DeleteGlobalRef(doubleClass);
    env->DeleteGlobalRef(integerClass);
    env->DeleteGlobalRef(booleanClass);

    if (!values.empty()) {
        for(auto i = values.begin(); i != values.end(); i++) {
            JSValue v = JS_MKPTR(JS_TAG_OBJECT, reinterpret_cast<void *>(*i));
            JS_FreeValue(context, v);
        }

        values.clear();
    }

    JS_FreeContext(context);

    // todo try catch
    // void JS_FreeRuntime(JSRuntime *): assertion "list_empty(&rt->gc_obj_list)" failed
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

string getName(JNIEnv* env, jobject javaClass) {
    auto classType = env->GetObjectClass(javaClass);
    const jmethodID method = env->GetMethodID(classType, "getName", "()Ljava/lang/String;");
    auto javaString = static_cast<jstring>(env->CallObjectMethod(javaClass, method));
    const auto s = env->GetStringUTFChars(javaString, nullptr);

    std::string str(s);
    env->ReleaseStringUTFChars(javaString, s);
    env->DeleteLocalRef(javaString);
    env->DeleteLocalRef(classType);
    return str;
}

void throwJavaException(JNIEnv *env, const char *exceptionClass, const char *fmt, ...) {
    char msg[512];
    va_list args;
    va_start (args, fmt);
    vsnprintf(msg, sizeof(msg), fmt, args);
    va_end (args);
    env->ThrowNew(env->FindClass(exceptionClass), msg);
}