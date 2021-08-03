#include <jni.h>
#include <string>
#include <android/log.h>
#include "quickjs_wrapper.h"

extern "C"
JNIEXPORT jlong JNICALL
Java_com_whl_quickjs_wrapper_QuickJSContext_evaluate(JNIEnv *env, jobject thiz, jlong context, jstring script,
                                                     jstring file_name) {
    auto wrapper = reinterpret_cast<QuickJSWrapper*>(context);
    const char *js = env->GetStringUTFChars(script, JNI_FALSE);

    // todo file name
    JSValue value = wrapper->evaluate(js);

    return reinterpret_cast<jlong>(JS_VALUE_GET_PTR(value));
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_whl_quickjs_wrapper_QuickJSContext_createContext(JNIEnv *env, jobject thiz) {
    return reinterpret_cast<jlong>(new QuickJSWrapper);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_whl_quickjs_wrapper_QuickJSContext_destroyContext(JNIEnv *env, jobject thiz,
                                                           jlong context) {
    // todo
    // delete reinterpret_cast<QuickJSWrapper*>(context);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_whl_quickjs_wrapper_QuickJSContext_getGlobalObject(JNIEnv *env, jobject thiz,
                                                            jlong context) {
    auto wrapper = reinterpret_cast<QuickJSWrapper*>(context);
    JSValue value = wrapper->getGlobalObject();
    return reinterpret_cast<jlong>(JS_VALUE_GET_PTR(value));
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_whl_quickjs_wrapper_JSValue_getProperty(JNIEnv *env, jobject thiz, jlong context, jlong value,
                                                 jstring name) {
    auto wrapper = reinterpret_cast<QuickJSWrapper*>(context);

    const char *propsName = env->GetStringUTFChars(name, JNI_FALSE);

    JSValue jsObject = JS_MKPTR(JS_TAG_OBJECT, reinterpret_cast<void *>(value));
    const char *result = wrapper->stringify(jsObject);
    __android_log_print(ANDROID_LOG_DEBUG, "quickjs-android-wrapper", "get props result=%s", result);

    JSValue propsValue = wrapper->getProperty(jsObject, propsName);
    const char *r_result = wrapper->stringify(propsValue);
    __android_log_print(ANDROID_LOG_DEBUG, "quickjs-android-wrapper", "get props r_value=%s", r_result);

    return reinterpret_cast<jlong>(JS_VALUE_GET_PTR(propsValue));
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_whl_quickjs_wrapper_QuickJSContext_call(JNIEnv *env, jobject thiz, jlong context,
                                                 jlong func, jlong this_obj, jint arg_count,
                                                 jlong arg_value) {
    auto wrapper = reinterpret_cast<QuickJSWrapper*>(context);

    // todo delete
    JSValue argv = JS_NewString(wrapper->context, "args");

    JSValue jsObj = JS_MKPTR(JS_TAG_OBJECT, reinterpret_cast<void *>(this_obj));
    JSValue jsFunc = JS_MKPTR(JS_TAG_OBJECT, reinterpret_cast<void *>(func));
    JSValue funcRet = wrapper->call(jsFunc, jsObj, 1, &argv);
    const char *r_result = wrapper->stringify(funcRet);
    __android_log_print(ANDROID_LOG_DEBUG, "quickjs-android-wrapper", "get props func_value=%s", r_result);

    return reinterpret_cast<jlong>(JS_VALUE_GET_PTR(funcRet));
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_whl_quickjs_wrapper_JSValue_stringify(JNIEnv *env, jobject thiz, jlong context,
                                               jlong value) {
    auto wrapper = reinterpret_cast<QuickJSWrapper*>(context);
    JSValue jsObj = JS_MKPTR(JS_TAG_STRING, reinterpret_cast<void *>(value));
    const char *stringify = wrapper->stringify(jsObj);
    return env->NewStringUTF(stringify);
}