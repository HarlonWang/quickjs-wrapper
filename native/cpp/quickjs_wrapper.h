//
// Created by yonglan.whl on 2021/7/14.
//

#ifndef QUICKJS_TEST_CONTEXT_WRAPPER_H
#define QUICKJS_TEST_CONTEXT_WRAPPER_H

#include <iostream>
#include <set>
#include <vector>
#include <queue>
using namespace std;

#include "../quickjs/quickjs.h"
#include <jni.h>
#include <map>

class QuickJSWrapper {
private:
    jobject toJavaObject(JNIEnv *env, jobject thiz, JSValueConst& this_obj, JSValueConst& value, bool non_js_callback = true);
    JSValue toJSValue(JNIEnv *env, jobject thiz, jobject value) const;

public:
    JNIEnv *jniEnv;
    jobject jniThiz;
    JSRuntime *runtime;
    JSContext *context;

    map<jlong, JSValue> values;
    queue<JSValueConst> unhandledRejections;

    jclass objectClass;
    jclass booleanClass;
    jclass integerClass;
    jclass longClass;
    jclass doubleClass;
    jclass stringClass;
    jclass jsObjectClass;
    jclass jsArrayClass;
    jclass jsFunctionClass;
    jclass jsCallFunctionClass;
    jclass quickjsContextClass;
    jclass moduleLoaderClass;

    jmethodID booleanValueOf;
    jmethodID integerValueOf;
    jmethodID longValueOf;
    jmethodID doubleValueOf;

    jmethodID booleanGetValue;
    jmethodID integerGetValue;
    jmethodID longGetValue;
    jmethodID doubleGetValue;
    jmethodID jsObjectGetValue;

    jmethodID jsObjectInit;
    jmethodID jsArrayInit;
    jmethodID jsFunctionInit;

    jmethodID callFunctionBackM;
    jmethodID removeCallFunctionM;
    jmethodID callFunctionHashCodeM;

    QuickJSWrapper(JNIEnv *env, jobject thiz, JSRuntime *rt);
    ~QuickJSWrapper();

    jobject evaluate(JNIEnv*, jobject thiz, jstring script, jstring file_name);
    jobject getGlobalObject(JNIEnv*, jobject thiz) const;
    jobject getProperty(JNIEnv*, jobject thiz, jlong value, jstring name);
    void setProperty(JNIEnv*, jobject thiz, jlong this_obj, jstring name, jobject value) const;
    jobject call(JNIEnv *env, jobject thiz, jlong func, jlong this_obj, jobjectArray args);
    jstring jsonStringify(JNIEnv *env, jlong value) const;
    jint length(JNIEnv *env, jlong value) const;
    jobject get(JNIEnv *env, jobject thiz, jlong value, jint index);
    void set(JNIEnv *env, jobject thiz, jlong this_obj, jobject value, jint index);
    JSValue jsFuncCall(int callback_id, JSValueConst this_val, int argc, JSValueConst *argv);
    void removeCallFunction(int callback_id) const;
    void freeValue(jlong);
    void dupValue(jlong) const;
    void freeDupValue(jlong) const;
    jobject parseJSON(JNIEnv*, jobject, jstring);

    // JS --> bytecode
    jbyteArray compile(JNIEnv*, jstring, jstring, jboolean) const;
    // bytecode --> result
    jobject execute(JNIEnv*, jobject, jbyteArray);

    jobject evaluateModule(JNIEnv *env, jobject thiz, jstring script, jstring file_name);
};

#endif //QUICKJS_TEST_CONTEXT_WRAPPER_H
