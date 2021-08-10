//
// Created by yonglan.whl on 2021/7/14.
//
#include "quickjs_wrapper.h"

static JSValue jsCall(JSContext *ctx, JSValueConst func_obj, JSValueConst this_val, int argc, JSValueConst *argv, int flags) {
    auto wrapper = reinterpret_cast<const QuickJSWrapper*>(JS_GetRuntimeOpaque(JS_GetRuntime(ctx)));
    auto funcCall = static_cast<QuickJSFunctionProxy *>(JS_GetOpaque(func_obj, wrapper->jsClassId));
    return funcCall->wrapper->jsFuncCall(funcCall->value, funcCall->thiz, this_val, argc, argv);
}

static void jsFinalizer(JSRuntime *rt, JSValue val) {
    auto wrapper = reinterpret_cast<const QuickJSWrapper*>(JS_GetRuntimeOpaque(rt));
    if (wrapper) {
        auto funcCall = static_cast<QuickJSFunctionProxy *>(JS_GetOpaque(val, wrapper->jsClassId));
        wrapper->jniEnv->DeleteGlobalRef(funcCall->value);
        wrapper->jniEnv->DeleteGlobalRef(funcCall->thiz);
        delete funcCall;
    }
}

QuickJSWrapper::QuickJSWrapper(JNIEnv *env) {
    jniEnv = env;
    runtime = JS_NewRuntime();
    context = JS_NewContext(runtime);
    JS_SetRuntimeOpaque(runtime, this);
    jsClassId = 0;

    objectClass = static_cast<jclass>(jniEnv->NewGlobalRef(jniEnv->FindClass("java/lang/Object")));
    booleanClass = static_cast<jclass>(jniEnv->NewGlobalRef(jniEnv->FindClass("java/lang/Boolean")));
    integerClass = static_cast<jclass>(jniEnv->NewGlobalRef(jniEnv->FindClass("java/lang/Integer")));
    doubleClass = static_cast<jclass>(jniEnv->NewGlobalRef(jniEnv->FindClass("java/lang/Double")));
    jsObjectClass = static_cast<jclass>(jniEnv->NewGlobalRef(jniEnv->FindClass("com/whl/quickjs/wrapper/JSObject")));
    jsArrayClass = static_cast<jclass>(jniEnv->NewGlobalRef(jniEnv->FindClass("com/whl/quickjs/wrapper/JSArray")));
    jsFunctionClass = static_cast<jclass>(jniEnv->NewGlobalRef(jniEnv->FindClass("com/whl/quickjs/wrapper/JSFunction")));
    jsCallFunctionClass = static_cast<jclass>(jniEnv->NewGlobalRef(jniEnv->FindClass("com/whl/quickjs/wrapper/JSCallFunction")));

    booleanValueOf = jniEnv->GetStaticMethodID(booleanClass, "valueOf", "(Z)Ljava/lang/Boolean;");
    integerValueOf = jniEnv->GetStaticMethodID(integerClass, "valueOf", "(I)Ljava/lang/Integer;");
    doubleValueOf = jniEnv->GetStaticMethodID(doubleClass, "valueOf", "(D)Ljava/lang/Double;");

    booleanGetValue = jniEnv->GetMethodID(booleanClass, "booleanValue", "()Z");
    integerGetValue = jniEnv->GetMethodID(integerClass, "intValue", "()I");
    doubleGetValue = jniEnv->GetMethodID(doubleClass, "doubleValue", "()D");

    jsObjectInit = jniEnv->GetMethodID(jsObjectClass, "<init>", "(Lcom/whl/quickjs/wrapper/QuickJSContext;J)V");
    jsArrayInit = jniEnv->GetMethodID(jsArrayClass, "<init>", "(Lcom/whl/quickjs/wrapper/QuickJSContext;J)V");
    jsFunctionInit = jniEnv->GetMethodID(jsFunctionClass, "<init>","(Lcom/whl/quickjs/wrapper/QuickJSContext;J)V");
}

QuickJSWrapper::~QuickJSWrapper() {
    jniEnv->DeleteGlobalRef(objectClass);
    jniEnv->DeleteGlobalRef(doubleClass);
    jniEnv->DeleteGlobalRef(integerClass);
    jniEnv->DeleteGlobalRef(booleanClass);
    jniEnv->DeleteGlobalRef(jsObjectClass);
    jniEnv->DeleteGlobalRef(jsArrayClass);
    jniEnv->DeleteGlobalRef(jsFunctionClass);
    jniEnv->DeleteGlobalRef(jsCallFunctionClass);

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

jobject QuickJSWrapper::toJavaObject(JNIEnv *env, jobject thiz, const JSValueConst& value){
    jobject result;
    switch (JS_VALUE_GET_NORM_TAG(value)) {
        case JS_TAG_EXCEPTION: {
            result = nullptr;
            break;
        }

        case JS_TAG_STRING: {
            const char* string = JS_ToCString(context, value);
            result = env->NewStringUTF(string);
            JS_FreeCString(context, string);
            break;
        }

        case JS_TAG_BOOL: {
            jvalue v;
            v.z = static_cast<jboolean>(JS_VALUE_GET_BOOL(value));
            result = env->CallStaticObjectMethodA(booleanClass, booleanValueOf, &v);
            break;
        }

        case JS_TAG_INT: {
            jvalue v;
            v.j = static_cast<jint>(JS_VALUE_GET_INT(value));
            result = env->CallStaticObjectMethodA(integerClass, integerValueOf, &v);
            break;
        }

        case JS_TAG_FLOAT64: {
            jvalue v;
            v.d = static_cast<jdouble>(JS_VALUE_GET_FLOAT64(value));
            result = env->CallStaticObjectMethodA(doubleClass, doubleValueOf, &v);
            break;
        }

        case JS_TAG_OBJECT: {
            auto value_ptr = reinterpret_cast<jlong>(JS_VALUE_GET_PTR(value));
            if (JS_IsFunction(context, value)) {
                result = env->NewObject(jsFunctionClass, jsFunctionInit, thiz, value_ptr);
            } else if (JS_IsArray(context, value)) {
                result = env->NewObject(jsArrayClass, jsArrayInit, thiz, value_ptr);
            } else {
                result = env->NewObject(jsObjectClass, jsObjectInit, thiz, value_ptr);
            }

            // todo refactor
            values.insert(value_ptr);
            break;
        }

        default:
            result = nullptr;
            break;
    }

    return result;
}

jobject QuickJSWrapper::evaluate(JNIEnv *env, jobject thiz, jstring script, jstring file_name) {
    const char *c_script = env->GetStringUTFChars(script, JNI_FALSE);
    const char *c_file_name = env->GetStringUTFChars(file_name, JNI_FALSE);

    JSValue result = evaluate(c_script, c_file_name);

    env->ReleaseStringUTFChars(script, c_script);
    env->ReleaseStringUTFChars(file_name, c_file_name);

    return toJavaObject(env, thiz, result);
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

jobject QuickJSWrapper::getGlobalObject(JNIEnv *env, jobject thiz) {
    JSValue value = getGlobalObject();
    auto result = reinterpret_cast<jlong>(JS_VALUE_GET_PTR(value));

    // todo refactor
    values.insert(result);

    return toJavaObject(env, thiz, value);
}

jobject QuickJSWrapper::getProperty(JNIEnv *env, jobject thiz, jlong value, jstring name) {
    const char *propsName = env->GetStringUTFChars(name, JNI_FALSE);
    JSValue jsObject = JS_MKPTR(JS_TAG_OBJECT, reinterpret_cast<void *>(value));
    JSValue propsValue = getProperty(jsObject, propsName);

    env->ReleaseStringUTFChars(name, propsName);

    return toJavaObject(env, thiz, propsValue);
}

jobject QuickJSWrapper::call(JNIEnv *env, jobject thiz, jlong func, jlong this_obj,
                             jobjectArray args) {
    int argc = env->GetArrayLength(args);
    vector<JSValue> arguments;
    for (int numArgs = 0; numArgs < argc && !env->ExceptionCheck(); numArgs++) {
        jobject arg = env->GetObjectArrayElement(args, numArgs);
        if (!arg) {
            __android_log_print(ANDROID_LOG_DEBUG, "quickjs-android-wrapper", "call Java type with null");
            break;
        }

        auto classType = env->GetObjectClass(arg);
        const auto typeName = getName(env, classType);
        __android_log_print(ANDROID_LOG_DEBUG, "quickjs-android-wrapper", "call args type=%s", typeName.c_str());

        if (!typeName.empty() && typeName[0] == '[') {
            throwJavaException(env, "java/lang/RuntimeException",
                               "Unsupported Java type with Array!");
            return nullptr;
        }

        arguments.push_back(toJSValue(env, arg));

        env->DeleteLocalRef(arg);
    }

    JSValue jsObj = JS_MKPTR(JS_TAG_OBJECT, reinterpret_cast<void *>(this_obj));
    JSValue jsFunc = JS_MKPTR(JS_TAG_OBJECT, reinterpret_cast<void *>(func));

    JSValue funcRet = call(jsFunc, jsObj, arguments.size(), arguments.data());

    JS_FreeValue(context, jsObj);
    JS_FreeValue(context, jsFunc);
    for (JSValue argument : arguments) {
        JS_FreeValue(context, argument);
    }

    const char *r_result = stringify(funcRet);

    __android_log_print(ANDROID_LOG_DEBUG, "quickjs-android-wrapper", "get props func_value=%s", r_result);

    return toJavaObject(env, thiz, funcRet);
}

jstring QuickJSWrapper::stringify(JNIEnv *env, jlong value) const {
    JSValue jsObj = JS_MKPTR(JS_TAG_OBJECT, reinterpret_cast<void *>(value));
    const char *result = stringify(jsObj);
    return env->NewStringUTF(result);
}

jint QuickJSWrapper::length(JNIEnv *env, jlong value) {
    JSValue jsObj = JS_MKPTR(JS_TAG_OBJECT, reinterpret_cast<void *>(value));
    JSValue length = getProperty(jsObj, "length");
    return JS_VALUE_GET_INT(length);
}

jobject QuickJSWrapper::get(JNIEnv *env, jobject thiz, jlong value, jint index) {
    JSValue jsObj = JS_MKPTR(JS_TAG_OBJECT, reinterpret_cast<void *>(value));
    JSValue child = JS_GetPropertyUint32(context, jsObj, index);
    const char *childStr = stringify(child);
    __android_log_print(ANDROID_LOG_DEBUG, "quickjs-android-wrapper", "get index=%s", childStr);

    return toJavaObject(env, thiz, child);
}

void
QuickJSWrapper::setProperty(JNIEnv *env, jobject thiz, jlong this_obj, jstring name, jobject value) {
    auto classType = env->GetObjectClass(value);
    const auto typeName = getName(env, classType);
    __android_log_print(ANDROID_LOG_DEBUG, "quickjs-native-wrapper", "call args type=%s", typeName.c_str());

    if (!typeName.empty() && typeName[0] == '[') {
        throwJavaException(env, "java/lang/RuntimeException",
                           "Unsupported Java type with Array!");
        return;
    }

    JSValue propValue;
    if (typeName == "java.lang.String") {
        const auto s = env->GetStringUTFChars(static_cast<jstring>(value), JNI_FALSE);
        propValue = JS_NewString(context, s);
        env->ReleaseStringUTFChars(static_cast<jstring>(value), s);
    } else if (typeName == "java.lang.Double" || typeName == "double") {
        propValue = JS_NewFloat64(context, env->CallDoubleMethod(value, doubleGetValue));
    } else if (typeName == "java.lang.Integer" || typeName == "int") {
        propValue = JS_NewInt32(context, env->CallIntMethod(value, integerGetValue));
    } else if (typeName == "java.lang.Boolean" || typeName == "boolean") {
        propValue = JS_NewBool(context, env->CallBooleanMethod(value, booleanGetValue));
    } else {
        if (env->IsInstanceOf(value, jsCallFunctionClass)) {
            if (jsClassId == 0) {
                JS_NewClassID(&jsClassId);
                JSClassDef classDef;
                memset(&classDef, 0, sizeof(JSClassDef));
                classDef.class_name = "WrapperJSCallProxy";
                classDef.finalizer = jsFinalizer;
                classDef.call = jsCall;
                if (JS_NewClass(runtime, jsClassId, &classDef)) {
                    jsClassId = 0;
                    throwJavaException(env, "java/lang/NullPointerException",
                                       "Failed to allocate JavaScript proxy class");
                }
            }

            if (jsClassId != 0) {
                propValue = JS_NewObjectClass(context, jsClassId);
                auto *funcProxy = new QuickJSFunctionProxy;
                funcProxy->wrapper = this;
                funcProxy->value = env->NewGlobalRef(value);
                funcProxy->thiz = env->NewGlobalRef(thiz);
                JS_SetOpaque(propValue, (void *) funcProxy);
            }
        } else {
            // Throw an exception for unsupported argument type.
            throwJavaException(env, "java/lang/IllegalArgumentException", "Unsupported Java type %s",
                               typeName.c_str());
        }
    }

    JSValue jsObj = JS_MKPTR(JS_TAG_OBJECT, reinterpret_cast<void *>(this_obj));
    const char* propName = env->GetStringUTFChars(name, JNI_FALSE);
    setProperty(jsObj, propName, propValue);

    env->ReleaseStringUTFChars(name, propName);
}

JSValue QuickJSWrapper::jsFuncCall(jobject func_value, jobject thiz, JSValueConst this_val, int argc, JSValueConst *argv){
    jobjectArray javaArgs = jniEnv->NewObjectArray((jsize)argc, objectClass, nullptr);

    for (int i = 0; i < argc; i++) {
        jniEnv->SetObjectArrayElement(javaArgs, (jsize)i, toJavaObject(jniEnv, thiz, argv[i]));
    }

    auto funcClass = jniEnv->GetObjectClass(func_value);
    auto funcMethodId = jniEnv->GetMethodID(funcClass, "call", "([Ljava/lang/Object;)Ljava/lang/Object;");
    auto result = jniEnv->CallObjectMethod(func_value, funcMethodId, javaArgs);

    jniEnv->DeleteLocalRef(javaArgs);

    return toJSValue(jniEnv, result);
}

JSValue QuickJSWrapper::toJSValue(JNIEnv *env, jobject value) {
    if (!value) {
        return JS_UNDEFINED;
    }

    auto classType = env->GetObjectClass(value);
    const auto typeName = getName(env, classType);

    if (!typeName.empty() && typeName[0] == '[') {
        throwJavaException(env, "java/lang/RuntimeException",
                           "Unsupported Java type with Array!");
        return JS_UNDEFINED;
    }

    JSValue result;
    if (typeName == "java.lang.String") {
        const auto s = env->GetStringUTFChars(static_cast<jstring>(value), JNI_FALSE);
        result = JS_NewString(context, s);
        env->ReleaseStringUTFChars(static_cast<jstring>(value), s);
    } else if (typeName == "java.lang.Double" || typeName == "double") {
        result = JS_NewFloat64(context, env->CallDoubleMethod(value, doubleGetValue));
    } else if (typeName == "java.lang.Integer" || typeName == "int") {
        result = JS_NewInt32(context, env->CallIntMethod(value, integerGetValue));
    } else if (typeName == "java.lang.Boolean" || typeName == "boolean") {
        result = JS_NewBool(context, env->CallBooleanMethod(value, booleanGetValue));
    } else {
        // Throw an exception for unsupported argument type.
        throwJavaException(env, "java/lang/IllegalArgumentException", "Unsupported Java type %s",
                           typeName.c_str());
        result = JS_UNDEFINED;
    }

    return result;
}

void QuickJSWrapper::freeValue(jlong value) {
    JSValue jsObj = JS_MKPTR(JS_TAG_OBJECT, reinterpret_cast<void *>(value));
    JS_FreeValue(context, jsObj);
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