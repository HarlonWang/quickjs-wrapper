#include <jni.h>
#include <string>
#include <android/log.h>
#include "quickjs_wrapper.h"
#include <vector>

static jobject toJavaObject(JNIEnv *env, jobject &thiz, QuickJSWrapper *wrapper, const JSValueConst& value) {
    jobject result;
    switch (JS_VALUE_GET_NORM_TAG(value)) {
        case JS_TAG_EXCEPTION: {
            result = nullptr;
            break;
        }

        case JS_TAG_STRING: {
            const char* string = JS_ToCString(wrapper->context, value);
            result = env->NewStringUTF(string);
            JS_FreeCString(wrapper->context, string);
            break;
        }

        case JS_TAG_BOOL: {
            jvalue v;
            v.z = static_cast<jboolean>(JS_VALUE_GET_BOOL(value));
            result = env->CallStaticObjectMethodA(wrapper->booleanClass, wrapper->booleanValueOf, &v);
            break;
        }

        case JS_TAG_INT: {
            jvalue v;
            v.j = static_cast<jint>(JS_VALUE_GET_INT(value));
            result = env->CallStaticObjectMethodA(wrapper->integerClass, wrapper->integerValueOf, &v);
            break;
        }

        case JS_TAG_FLOAT64: {
            jvalue v;
            v.d = static_cast<jdouble>(JS_VALUE_GET_FLOAT64(value));
            result = env->CallStaticObjectMethodA(wrapper->doubleClass, wrapper->doubleValueOf, &v);
            break;
        }

        case JS_TAG_OBJECT: {
            auto value_ptr = reinterpret_cast<jlong>(JS_VALUE_GET_PTR(value));
            if (JS_IsFunction(wrapper->context, value)) {
                result = env->NewObject(wrapper->jsFunctionClass, wrapper->jsFunctionInit, thiz, value_ptr);
            } else if (JS_IsArray(wrapper->context, value)) {
                result = env->NewObject(wrapper->jsArrayClass, wrapper->jsArrayInit, thiz, value_ptr);
            } else {
                result = env->NewObject(wrapper->jsObjectClass, wrapper->jsObjectInit, thiz, value_ptr);
            }

            // todo refactor
            wrapper->values.insert(value_ptr);
            break;
        }

        default:
            result = nullptr;
            break;
    }

    return result;
}

static void j_println(QuickJSWrapper *wrapper, JSValue &value) {
    const char *result = wrapper->stringify(value);
    __android_log_print(ANDROID_LOG_DEBUG, "quickjs-android-native", "println=%s", result);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_whl_quickjs_wrapper_QuickJSContext_destroyContext(JNIEnv *env, jobject thiz,
                                                           jlong context) {
    delete reinterpret_cast<QuickJSWrapper*>(context);
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_whl_quickjs_wrapper_QuickJSContext_evaluate(JNIEnv *env, jobject thiz, jlong context, jstring script,
                                                     jstring file_name) {
    auto wrapper = reinterpret_cast<QuickJSWrapper*>(context);
    if (!wrapper) {
        throwJavaException(env, "java/lang/NullPointerException",
                           "Null QuickJS wrapper - did you destroy your QuickJS?");
        return nullptr;
    }

    const char *c_script = env->GetStringUTFChars(script, JNI_FALSE);
    const char *c_file_name = env->GetStringUTFChars(file_name, JNI_FALSE);

    JSValue result = wrapper->evaluate(c_script, c_file_name);

    j_println(wrapper, result);

    env->ReleaseStringUTFChars(script, c_script);
    env->ReleaseStringUTFChars(file_name, c_file_name);

    return toJavaObject(env, thiz, wrapper, result);
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_whl_quickjs_wrapper_QuickJSContext_getGlobalObject(JNIEnv *env, jobject thiz,
                                                            jlong context) {
    auto wrapper = reinterpret_cast<QuickJSWrapper*>(context);
    JSValue value = wrapper->getGlobalObject();
    auto result = reinterpret_cast<jlong>(JS_VALUE_GET_PTR(value));

    // todo refactor
    wrapper->values.insert(result);

    return toJavaObject(env, thiz, wrapper, value);
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_whl_quickjs_wrapper_QuickJSContext_getProperty(JNIEnv *env, jobject thiz, jlong context, jlong value,
                                                 jstring name) {
    auto wrapper = reinterpret_cast<QuickJSWrapper*>(context);

    const char *propsName = env->GetStringUTFChars(name, JNI_FALSE);

    JSValue jsObject = JS_MKPTR(JS_TAG_OBJECT, reinterpret_cast<void *>(value));
    const char *result = wrapper->stringify(jsObject);
    __android_log_print(ANDROID_LOG_DEBUG, "quickjs-android-wrapper", "get props result=%s", result);

    JSValue propsValue = wrapper->getProperty(jsObject, propsName);
    const char *r_result = wrapper->stringify(propsValue);
    __android_log_print(ANDROID_LOG_DEBUG, "quickjs-android-wrapper", "get props r_value=%s", r_result);

    return toJavaObject(env, thiz, wrapper, propsValue);
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_whl_quickjs_wrapper_QuickJSContext_call(JNIEnv *env, jobject thiz, jlong context,
                                                 jlong func, jlong this_obj, jobjectArray args) {
    auto wrapper = reinterpret_cast<QuickJSWrapper*>(context);

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

        if (typeName == "java.lang.String") {
            const auto s = env->GetStringUTFChars(static_cast<jstring>(arg), JNI_FALSE);
            auto jsString = JS_NewString(wrapper->context, s);
            env->ReleaseStringUTFChars(static_cast<jstring>(arg), s);
            arguments.push_back(jsString);
        } else if (typeName == "java.lang.Double" || typeName == "double") {
            arguments.push_back(JS_NewFloat64(wrapper->context, env->CallDoubleMethod(arg, wrapper->doubleGetValue)));
        } else if (typeName == "java.lang.Integer" || typeName == "int") {
            arguments.push_back(JS_NewInt32(wrapper->context, env->CallIntMethod(arg, wrapper->integerGetValue)));
        } else if (typeName == "java.lang.Boolean" || typeName == "boolean") {
            arguments.push_back(JS_NewBool(wrapper->context, env->CallBooleanMethod(arg, wrapper->booleanGetValue)));
        } else {
            // Throw an exception for unsupported argument type.
            throwJavaException(env, "java/lang/IllegalArgumentException", "Unsupported Java type %s",
                               typeName.c_str());
        }

        env->DeleteLocalRef(arg);
    }

    JSValue jsObj = JS_MKPTR(JS_TAG_OBJECT, reinterpret_cast<void *>(this_obj));
    JSValue jsFunc = JS_MKPTR(JS_TAG_OBJECT, reinterpret_cast<void *>(func));

    JSValue funcRet = wrapper->call(jsFunc, jsObj, arguments.size(), arguments.data());

    JS_FreeValue(wrapper->context, jsObj);
    JS_FreeValue(wrapper->context, jsFunc);
    for (JSValue argument : arguments) {
        JS_FreeValue(wrapper->context, argument);
    }

    const char *r_result = wrapper->stringify(funcRet);

    __android_log_print(ANDROID_LOG_DEBUG, "quickjs-android-wrapper", "get props func_value=%s", r_result);

    return toJavaObject(env, thiz, wrapper, funcRet);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_whl_quickjs_wrapper_QuickJSContext_stringify(JNIEnv *env, jobject thiz, jlong context,
                                               jlong value) {
    auto wrapper = reinterpret_cast<QuickJSWrapper*>(context);
    JSValue jsObj = JS_MKPTR(JS_TAG_OBJECT, reinterpret_cast<void *>(value));
    const char *stringify = wrapper->stringify(jsObj);
    return env->NewStringUTF(stringify);
}extern "C"
JNIEXPORT jint JNICALL
Java_com_whl_quickjs_wrapper_QuickJSContext_length(JNIEnv *env, jobject thiz, jlong context,
                                               jlong value) {
    auto wrapper = reinterpret_cast<QuickJSWrapper*>(context);
    JSValue jsObj = JS_MKPTR(JS_TAG_OBJECT, reinterpret_cast<void *>(value));
    JSValue length = wrapper->getProperty(jsObj, "length");
    return JS_VALUE_GET_INT(length);
}extern "C"
JNIEXPORT jobject JNICALL
Java_com_whl_quickjs_wrapper_QuickJSContext_get(JNIEnv *env, jobject thiz, jlong context, jlong value,
                                         jint index) {
    auto wrapper = reinterpret_cast<QuickJSWrapper*>(context);
    JSValue jsObj = JS_MKPTR(JS_TAG_OBJECT, reinterpret_cast<void *>(value));
    JSValue child = JS_GetPropertyUint32(wrapper->context, jsObj, index);
    const char *childStr = wrapper->stringify(child);
    __android_log_print(ANDROID_LOG_DEBUG, "quickjs-android-wrapper", "get index=%s", childStr);

    return toJavaObject(env, thiz, wrapper, child);
}extern "C"
JNIEXPORT jlong JNICALL
Java_com_whl_quickjs_wrapper_QuickJSContext_createContext(JNIEnv *env, jobject thiz) {
    auto *wrapper = new(std::nothrow) QuickJSWrapper(env);
    if (!wrapper || !wrapper->context || !wrapper->runtime) {
        delete wrapper;
        wrapper = nullptr;
    }

    return reinterpret_cast<jlong>(wrapper);
}