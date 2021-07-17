#include <jni.h>
#include <string>
#include <android/log.h>
#include "quickjs_wrapper.h"

extern "C"
JNIEXPORT jobject

JNICALL
Java_com_whl_quickjs_wrapper_QuickJSContext_evaluate(JNIEnv *env, jobject thiz, jlong context, jstring script,
                                                     jstring file_name) {
    auto wrapper = reinterpret_cast<QuickJSWrapper*>(context);
    const char *js = env->GetStringUTFChars(script, JNI_FALSE);
    wrapper->evaluate(js);
    __android_log_print(ANDROID_LOG_DEBUG, "quickjs-android-wrapper", "hello=%s", "123");
    return NULL;
}extern "C"
JNIEXPORT jlong JNICALL
Java_com_whl_quickjs_wrapper_QuickJSContext_createContext(JNIEnv *env, jobject thiz) {
    return reinterpret_cast<jlong>(new QuickJSWrapper);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_whl_quickjs_wrapper_QuickJSContext_destroyContext(JNIEnv *env, jobject thiz,
                                                           jlong context) {
    delete reinterpret_cast<QuickJSWrapper*>(context);
}