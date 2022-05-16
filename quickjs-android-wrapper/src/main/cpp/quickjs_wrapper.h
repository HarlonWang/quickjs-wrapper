//
// Created by yonglan.whl on 2021/7/14.
//

#ifndef QUICKJS_TEST_CONTEXT_WRAPPER_H
#define QUICKJS_TEST_CONTEXT_WRAPPER_H

#include <iostream>
#include <set>
#include <vector>
using namespace std;

#include "quickjs/quickjs.h"
#include <jni.h>
#include <android/log.h>
#include <map>

class QuickJSWrapper {
private:
    JSValue checkJSException(JSValue &value) const;
    JSValue evaluate(const char *script, const char *file_name = "undefined.js", int eval_flag = JS_EVAL_TYPE_GLOBAL) const;
    JSValue getGlobalObject() const;
    JSValue getProperty(JSValue &this_obj, const char *propName) const;
    int setProperty(JSValue &this_obj, const char *propName, JSValue &val) const;
    JSValue call(JSValue &func_obj, JSValue &this_obj, int argc, JSValue *argv) const;
    const char* stringify(JSValue &value) const;

    /**
     * 将 JSValue 转为 Java 类型
     * @param env
     * @param thiz
     * @param this_obj 所属的对象
     * @param value
     * @param hold
     * @return
     */
    jobject toJavaObject(JNIEnv *env, jobject thiz, JSValueConst& this_obj, JSValueConst& value, bool hold = true);

    /**
     * 将 Java 类型转为 JSValue
     * @param env
     * @param value
     * @return
     */
    JSValue toJSValue(JNIEnv *env, jobject value) const;

public:
    JNIEnv *jniEnv;
    JSRuntime *runtime;
    JSContext *context;

    map<jlong, JSValue> values;

    jclass objectClass;
    jclass booleanClass;
    jclass integerClass;
    jclass doubleClass;
    jclass stringClass;
    jclass jsObjectClass;
    jclass jsArrayClass;
    jclass jsFunctionClass;
    jclass jsCallFunctionClass;
    jclass jsModuleClass;

    jmethodID booleanValueOf;
    jmethodID integerValueOf;
    jmethodID doubleValueOf;

    jmethodID booleanGetValue;
    jmethodID integerGetValue;
    jmethodID doubleGetValue;
    jmethodID jsObjectGetValue;

    jmethodID jsObjectInit;
    jmethodID jsArrayInit;
    jmethodID jsFunctionInit;
    jmethodID jsGetModuleScript;
    jmethodID jsConvertModuleName;

    QuickJSWrapper(JNIEnv *env);
    ~QuickJSWrapper();

    jobject evaluate(JNIEnv*, jobject thiz, jstring script, jstring file_name);
    jobject getGlobalObject(JNIEnv*, jobject thiz);
    jobject getProperty(JNIEnv*, jobject thiz, jlong value, jstring name);
    void setProperty(JNIEnv*, jobject thiz, jlong this_obj, jstring name, jobject value);
    jobject call(JNIEnv *env, jobject thiz, jlong func, jlong this_obj, jobjectArray args);
    jstring stringify(JNIEnv *env, jlong value) const;
    jint length(JNIEnv *env, jlong value);
    jobject get(JNIEnv *env, jobject thiz, jlong value, jint index);
    void set(JNIEnv *env, jobject thiz, jlong this_obj, jobject value, jint index);
    JSValue jsFuncCall(jobject func_value, jobject thiz, JSValueConst this_val, int argc, JSValueConst *argv);
    void freeValue(jlong);
    void dupValue(jlong) const;
    void freeDupValue(jlong) const;
    jobject parseJSON(JNIEnv*, jobject, jstring);

    // JS --> bytecode
    jbyteArray compile(JNIEnv*, jstring) const;
    // bytecode --> result
    jobject execute(JNIEnv*, jobject, jbyteArray);

    jobject evaluateModule(JNIEnv *env, jobject thiz, jstring script, jstring file_name);

    jint executePendingJob() const;

    void throwJSException(const JSValue& value) const;
};

#endif //QUICKJS_TEST_CONTEXT_WRAPPER_H
