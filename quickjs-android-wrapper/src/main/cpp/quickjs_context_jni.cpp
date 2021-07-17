#include <jni.h>
#include <string>
#include <android/log.h>

extern "C"
JNIEXPORT jobject

JNICALL
Java_com_whl_quickjs_wrapper_QuickJSContext_evaluate(JNIEnv *env, jobject thiz, jstring script,
                                                     jstring file_name, jint eval_flag) {
    __android_log_print(ANDROID_LOG_DEBUG, "quickjs-android-wrapper", "hello=%s", "123");
    return NULL;
}