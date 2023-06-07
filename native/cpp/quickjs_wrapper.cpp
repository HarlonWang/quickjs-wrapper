//
// Created by yonglan.whl on 2021/7/14.
//
#include "quickjs_wrapper.h"
#include "../quickjs/cutils.h"
#include <cstring>

// util
static string getJavaName(JNIEnv* env, jobject javaClass) {
    auto classType = env->GetObjectClass(javaClass);
    const auto method = env->GetMethodID(classType, "getName", "()Ljava/lang/String;");
    auto javaString = (jstring)(env->CallObjectMethod(javaClass, method));
    const auto s = env->GetStringUTFChars(javaString, nullptr);

    std::string str(s);
    env->ReleaseStringUTFChars(javaString, s);
    env->DeleteLocalRef(javaString);
    env->DeleteLocalRef(classType);
    return str;
}

static void throwJavaException(JNIEnv *env, const char *exceptionClass, const char *fmt, ...) {
    char msg[512];
    va_list args;
    va_start (args, fmt);
    vsnprintf(msg, sizeof(msg), fmt, args);
    va_end (args);
    jclass e = env->FindClass(exceptionClass);
    env->ThrowNew(e, msg);
    env->DeleteLocalRef(e);
}

static const char* jsDumpObj(JSContext *ctx, JSValueConst val)
{
    const char *str;

    str = JS_ToCString(ctx, val);
    if (str) {
        return str;
    } else {
        return "[exception]";
    }
}

static void tryToTriggerOnError(JSContext *ctx, JSValueConst *error) {
    JSValue global = JS_GetGlobalObject(ctx);
    JSValue onerror = JS_GetPropertyStr(ctx, global, "onError");
    if (JS_IsNull(onerror)) {
        // may be lowercase
        onerror = JS_GetPropertyStr(ctx, global, "onerror");
    }

    if (JS_IsNull(onerror)) {
        // do nothing
        return;
    }

    JS_Call(ctx, onerror, global, 1, error);
    JS_FreeValue(ctx, onerror);
    JS_FreeValue(ctx, global);
}

static string getJSErrorStr(JSContext *ctx, JSValueConst error) {
    JSValue val;
    bool is_error;
    is_error = JS_IsError(ctx, error);
    string jsException;
    if (is_error) {
        tryToTriggerOnError(ctx, &error);

        JSValue message = JS_GetPropertyStr(ctx, error, "message");
        jsException = JS_ToCString(ctx, message);
        JS_FreeValue(ctx, message);

        val = JS_GetPropertyStr(ctx, error, "stack");
        if (!JS_IsUndefined(val)) {
            jsException += "\n";
            jsException += jsDumpObj(ctx, val);
        }
        JS_FreeValue(ctx, val);
    } else {
        jsException = jsDumpObj(ctx, error);
    }
    return jsException;
}

static string getJSErrorStr(JSContext *ctx) {
    JSValue error = JS_GetException(ctx);
    string error_str = getJSErrorStr(ctx, error);
    JS_FreeValue(ctx, error);
    return error_str;
}

// js function callback
static JSClassID js_func_callback_class_id;

static void jsFuncCallbackFinalizer(JSRuntime *rt, JSValue val) {
    auto wrapper = reinterpret_cast<const QuickJSWrapper*>(JS_GetRuntimeOpaque(rt));
    if (wrapper) {
        int *callbackId = (int *)(JS_GetOpaque2(wrapper->context, val, js_func_callback_class_id));
        wrapper->removeCallFunction(*callbackId);
        delete callbackId;
    }
}

static JSClassDef js_func_callback_class = {
        "JSFuncCallback",
        .finalizer = jsFuncCallbackFinalizer,
};

static JSValue jsFnCallback(JSContext *ctx,
                            JSValueConst this_obj,
                            int argc, JSValueConst *argv,
                            int magic, JSValue *func_data) {

    int callbackId = *((int *)JS_GetOpaque2(ctx, func_data[0], js_func_callback_class_id));
    auto wrapper = reinterpret_cast<QuickJSWrapper*>(JS_GetRuntimeOpaque(JS_GetRuntime(ctx)));
    JSValue value = wrapper->jsFuncCall(callbackId, this_obj, argc, argv);
    return value;
}

static void initJSFuncCallback(JSContext *ctx) {
    // JSFuncCallback class
    JS_NewClassID(&js_func_callback_class_id);
    JS_NewClass(JS_GetRuntime(ctx), js_func_callback_class_id, &js_func_callback_class);
}

// js module
static char *jsModuleNormalizeFunc(JSContext *ctx, const char *module_base_name,
                                            const char *module_name, void *opaque) {
    auto wrapper = reinterpret_cast<const QuickJSWrapper*>(JS_GetRuntimeOpaque(JS_GetRuntime(ctx)));
    auto env = wrapper->jniEnv;

    jobject result = env->CallStaticObjectMethod(wrapper->jsModuleClass, wrapper->jsConvertModuleName,
                                                    env->NewStringUTF(module_base_name),
                                                    env->NewStringUTF(module_name));
    if (result == nullptr) {
        return nullptr;
    }
    return (char *) env->GetStringUTFChars((jstring) result, nullptr);
}

static JSModuleDef *
jsModuleLoaderFunc(JSContext *ctx, const char *module_name, void *opaque) {
    auto wrapper = reinterpret_cast<const QuickJSWrapper*>(JS_GetRuntimeOpaque(JS_GetRuntime(ctx)));
    auto env = wrapper->jniEnv;
    auto arg = env->NewStringUTF(module_name);

    auto result = env->CallStaticObjectMethod(wrapper->jsModuleClass, wrapper->jsGetModuleScript, arg);
    const auto script = env->GetStringUTFChars((jstring)(result), JNI_FALSE);

    int scriptLen = env->GetStringUTFLength((jstring) result);

    if (script == nullptr) {
        return nullptr;
    }

    JSValue func_val = JS_Eval(ctx, script, scriptLen, module_name,
                               JS_EVAL_TYPE_MODULE | JS_EVAL_FLAG_COMPILE_ONLY);
    void *m = JS_VALUE_GET_PTR(func_val);
    JS_FreeValue(ctx, func_val);
    return (JSModuleDef *) m;
}

// Support Array.prototype.at with javascript, like: [1, 2].at(0);
static void initArrayPrototypeAt(JSContext *ctx) {
    const char* at = "Array.prototype.at === undefined && (Array.prototype.at = function(no) { return this[no >= 0 ? no : (this.length+no)] })";
    JSValue res = JS_Eval(ctx, at, strlen(at), "ArrayPrototypeAt.js", JS_EVAL_TYPE_GLOBAL);
    JS_FreeValue(ctx, res);
}

// It is usually show object on the console.
static void initFormatObject(JSContext *ctx) {
    const char* format_string_script = R"lit(function __format_string(a) {
    var stack = [];
    var string = '';

    function format_rec(a) {
        var n, i, keys, key, type;

        type = typeof(a);
        if (type === "object") {
            if (a === null) {
                string += a;
            } else if(a instanceof Error) {
                string += a.toString();
            } else if (stack.indexOf(a) >= 0) {
                string += "[circular]";
            } else {
                stack.push(a);
                if (Array.isArray(a)) {
                    n = a.length;
                    string += "[ ";
                    for(i = 0; i < n; i++) {
                        if (i !== 0)
                            string += ", ";
                        if (i in a) {
                            format_rec(a[i]);
                        } else {
                            string += "<empty>";
                        }
                        if (i > 20) {
                            string += "...";
                            break;
                        }
                    }
                    string += " ]";
                } else {
                    keys = Object.keys(a);
                    n = keys.length;
                    string += "{ ";
                    for(i = 0; i < n; i++) {
                        if (i !== 0)
                            string += ", ";
                        key = keys[i];
                        string = string + key + ": ";
                        format_rec(a[key]);
                    }
                    string += " }";
                }
                stack.pop(a);
            }
        } else if (type === "string") {
            string += a;
        } else if (type === "number") {
            string += a.toString();
        } else if (type === "symbol") {
            string += String(a);
        } else if (type === "function") {
            string = string + "function " + a.name + "()";
        } else {
            string += a;
        }
    }
    format_rec(a);

    return string;
})lit";
    JS_Eval(ctx, format_string_script, strlen(format_string_script), "__format_string.js", JS_EVAL_TYPE_GLOBAL);
}

static void initPolyfillDate(JSContext *ctx) {
    const char* polyfill_date = R"lit(
(() => {
    const _Date = Date;
    // use _Date avoid recursion in _parse.
    const _parse = (date) => {
        if (date === null) {
            // null is invalid
            return new _Date(NaN);
        }
        if (date === undefined) {
            // today
            return new _Date();
        }
        if (date instanceof Date) {
            return new _Date(date);
        }

        if (typeof date === 'string' && !/Z$/i.test(date)) {
            // YYYY-MM-DD HH:mm:ss.sssZ
            const d = date.match(/^(\d{4})[-/]?(\d{1,2})?[-/]?(\d{0,2})[Tt\s]*(\d{1,2})?:?(\d{1,2})?:?(\d{1,2})?[.:]?(\d+)?$/);
            if (d) {
                let YYYY = d[1];
                let MM = d[2] - 1 || 0;
                let DD = d[3] || 1;

                const HH = d[4] || 0;
                const mm = d[5] || 0;
                const ss = d[6] || 0;
                const sssZ = (d[7] || '0').substring(0, 3);

                // Consider that only date strings (such as "1970-01-01") will be processed as UTC instead of local time.
                let utc = (d[4] === undefined) && (d[5] === undefined) && (d[6] === undefined) && (d[7] === undefined);
                if (utc) {
                    return new Date(Date.UTC(YYYY, MM, DD, HH, mm, ss, sssZ));
                }
                return new Date(YYYY, MM, DD, HH, mm, ss, sssZ);
            }
        }

        // everything else
        return new _Date(date);
    };

    const handler = {
        construct: function (target, args) {
            if (args.length === 1 && typeof args[0] === 'string') {
                return _parse(args[0]);
            }

            return new target(...args);
        },
        get(target, prop) {
            if (typeof target[prop] === 'function' && target[prop].name === 'parse') {
                return new Proxy(target[prop], {
                    apply: (target, thisArg, argumentsList) => {
                        if (argumentsList.length === 1 && typeof argumentsList[0] === 'string') {
                            return _parse(argumentsList[0]).getTime();
                        }

                        return Reflect.apply(target, thisArg, argumentsList);
                    }
                });
            } else {
                return Reflect.get(target, prop);
            }
        }
    };

    Date = new Proxy(Date, handler);
})();
)lit";
    JS_Eval(ctx, polyfill_date, strlen(polyfill_date), "polyfill_date.js", JS_EVAL_TYPE_GLOBAL);
}

static void throwJSException(JNIEnv *env, const char* msg) {
    jclass e = env->FindClass("com/whl/quickjs/wrapper/QuickJSException");
    jmethodID init = env->GetMethodID(e, "<init>", "(Ljava/lang/String;Z)V");
    jstring ret = env->NewStringUTF(msg);
    auto t = (jthrowable)env->NewObject(e, init, ret, JNI_TRUE);
    env->Throw(t);
    env->DeleteLocalRef(e);
}

static void throwJSException(JNIEnv *env, JSContext *ctx) {
    string error = getJSErrorStr(ctx);
    throwJSException(env, error.c_str());
}

static bool throwIfUnhandledRejections(QuickJSWrapper *wrapper, JSContext *ctx) {
    string error;
    while (!wrapper->unhandledRejections.empty()) {
        JSValueConst reason = wrapper->unhandledRejections.front();
        error += getJSErrorStr(ctx, reason);
        error += "\n";
        JS_FreeValue(ctx, reason);
        wrapper->unhandledRejections.pop();
    }

    bool is_error = !error.empty();
    if (is_error) {
        error = "UnhandledPromiseRejectionException: " + error;
        throwJSException(wrapper->jniEnv, error.c_str());
    }
    return is_error;
}

static bool executePendingJobLoop(JNIEnv *env, JSRuntime *rt, JSContext *ctx) {
    if (env->ExceptionCheck()) {
        return false;
    }

    JSContext *ctx1;
    bool success = true;
    int err;
    /* execute the pending jobs */
    for(;;) {
        err = JS_ExecutePendingJob(rt, &ctx1);
        if (err <= 0) {
            if (err < 0) {
                success = false;
                string error = getJSErrorStr(ctx);
                throwJSException(env, error.c_str());
            }
            break;
        }
    }

    if (success && throwIfUnhandledRejections(reinterpret_cast<QuickJSWrapper *>(JS_GetRuntimeOpaque(rt)), ctx)) {
        success = false;
    }

    return success;
}

static void promiseRejectionTracker(JSContext *ctx, JSValueConst promise,
                                    JSValueConst reason, BOOL is_handled, void *opaque) {
    auto unhandledRejections = static_cast<queue<JSValue> *>(opaque);
    if (!is_handled) {
        unhandledRejections->push(JS_DupValue(ctx, reason));
    } else {
        JSValueConst rej = unhandledRejections->front();
        JS_FreeValue(ctx, rej);
        unhandledRejections->pop();
    }
}

QuickJSWrapper::QuickJSWrapper(JNIEnv *env, jobject thiz, JSRuntime *rt) {
    jniEnv = env;
    runtime = rt;
    jniThiz = jniEnv->NewGlobalRef(thiz);

    // init ES6Module
    JS_SetModuleLoaderFunc(runtime, jsModuleNormalizeFunc, jsModuleLoaderFunc, nullptr);

    JS_SetHostPromiseRejectionTracker(runtime, promiseRejectionTracker, &unhandledRejections);

    context = JS_NewContext(runtime);

    JS_SetRuntimeOpaque(runtime, this);
    initJSFuncCallback(context);
    initArrayPrototypeAt(context);
    initFormatObject(context);
    initPolyfillDate(context);

    objectClass = (jclass)(jniEnv->NewGlobalRef(jniEnv->FindClass("java/lang/Object")));
    booleanClass = (jclass)(jniEnv->NewGlobalRef(jniEnv->FindClass("java/lang/Boolean")));
    integerClass = (jclass)(jniEnv->NewGlobalRef(jniEnv->FindClass("java/lang/Integer")));
    longClass = (jclass)(jniEnv->NewGlobalRef(jniEnv->FindClass("java/lang/Long")));
    doubleClass = (jclass)(jniEnv->NewGlobalRef(jniEnv->FindClass("java/lang/Double")));
    stringClass = (jclass)(jniEnv->NewGlobalRef(jniEnv->FindClass("java/lang/String")));
    jsObjectClass = (jclass)(jniEnv->NewGlobalRef(jniEnv->FindClass("com/whl/quickjs/wrapper/JSObject")));
    jsArrayClass = (jclass)(jniEnv->NewGlobalRef(jniEnv->FindClass("com/whl/quickjs/wrapper/JSArray")));
    jsFunctionClass = (jclass)(jniEnv->NewGlobalRef(jniEnv->FindClass("com/whl/quickjs/wrapper/JSFunction")));
    jsCallFunctionClass = (jclass)(jniEnv->NewGlobalRef(jniEnv->FindClass("com/whl/quickjs/wrapper/JSCallFunction")));
    jsModuleClass = (jclass)(jniEnv->NewGlobalRef(jniEnv->FindClass("com/whl/quickjs/wrapper/JSModule")));
    quickjsContextClass = (jclass)(jniEnv->NewGlobalRef(jniEnv->FindClass("com/whl/quickjs/wrapper/QuickJSContext")));

    booleanValueOf = jniEnv->GetStaticMethodID(booleanClass, "valueOf", "(Z)Ljava/lang/Boolean;");
    integerValueOf = jniEnv->GetStaticMethodID(integerClass, "valueOf", "(I)Ljava/lang/Integer;");
    longValueOf = jniEnv->GetStaticMethodID(longClass, "valueOf", "(J)Ljava/lang/Long;");
    doubleValueOf = jniEnv->GetStaticMethodID(doubleClass, "valueOf", "(D)Ljava/lang/Double;");

    booleanGetValue = jniEnv->GetMethodID(booleanClass, "booleanValue", "()Z");
    integerGetValue = jniEnv->GetMethodID(integerClass, "intValue", "()I");
    longGetValue = jniEnv->GetMethodID(longClass, "longValue", "()J");
    doubleGetValue = jniEnv->GetMethodID(doubleClass, "doubleValue", "()D");
    jsObjectGetValue = jniEnv->GetMethodID(jsObjectClass, "getPointer", "()J");

    jsObjectInit = jniEnv->GetMethodID(jsObjectClass, "<init>", "(Lcom/whl/quickjs/wrapper/QuickJSContext;J)V");
    jsArrayInit = jniEnv->GetMethodID(jsArrayClass, "<init>", "(Lcom/whl/quickjs/wrapper/QuickJSContext;J)V");
    jsFunctionInit = jniEnv->GetMethodID(jsFunctionClass, "<init>","(Lcom/whl/quickjs/wrapper/QuickJSContext;JJ)V");
    jsConvertModuleName = jniEnv->GetStaticMethodID(jsModuleClass, "convertModuleName",
                                              "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
    jsGetModuleScript = jniEnv->GetStaticMethodID(jsModuleClass, "getModuleScript", "(Ljava/lang/String;)Ljava/lang/String;");

    callFunctionBackM = jniEnv->GetMethodID(quickjsContextClass, "callFunctionBack", "(I[Ljava/lang/Object;)Ljava/lang/Object;");
    removeCallFunctionM = jniEnv->GetMethodID(quickjsContextClass, "removeCallFunction", "(I)V");
    callFunctionHashCodeM = jniEnv->GetMethodID(objectClass, "hashCode", "()I");
}

QuickJSWrapper::~QuickJSWrapper() {
    map<jlong, JSValue>::iterator i;
    for (i = values.begin(); i != values.end(); ++i) {
        JSValue item = i->second;
        JS_FreeValue(context, item);
    }
    values.clear();

    JS_FreeContext(context);
    JS_FreeRuntime(runtime);

    jniEnv->DeleteGlobalRef(jniThiz);
    jniEnv->DeleteGlobalRef(objectClass);
    jniEnv->DeleteGlobalRef(doubleClass);
    jniEnv->DeleteGlobalRef(integerClass);
    jniEnv->DeleteGlobalRef(longClass);
    jniEnv->DeleteGlobalRef(booleanClass);
    jniEnv->DeleteGlobalRef(stringClass);
    jniEnv->DeleteGlobalRef(jsObjectClass);
    jniEnv->DeleteGlobalRef(jsArrayClass);
    jniEnv->DeleteGlobalRef(jsFunctionClass);
    jniEnv->DeleteGlobalRef(jsCallFunctionClass);
    jniEnv->DeleteGlobalRef(jsModuleClass);
    jniEnv->DeleteGlobalRef(quickjsContextClass);
}

jobject QuickJSWrapper::toJavaObject(JNIEnv *env, jobject thiz, JSValueConst& this_obj, JSValueConst& value, bool hold){
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

        case JS_TAG_BIG_INT: {
            int64_t e;
            JS_ToBigInt64(context, &e, value);
            jvalue v;
            v.j = e;
            result = env->CallStaticObjectMethodA(longClass, longValueOf, &v);
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
                auto obj_ptr = reinterpret_cast<jlong>(JS_VALUE_GET_PTR(this_obj));
                result = env->NewObject(jsFunctionClass, jsFunctionInit, thiz, obj_ptr, value_ptr);
            } else if (JS_IsArray(context, value)) {
                result = env->NewObject(jsArrayClass, jsArrayInit, thiz, value_ptr);
            } else {
                result = env->NewObject(jsObjectClass, jsObjectInit, thiz, value_ptr);
            }

            if (hold) {
                if (values.count(value_ptr) == 0) {
                    values.insert(pair<jlong, JSValue>(value_ptr, value));
                } else{
                    JS_FreeValue(context, value);
                }
            }

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

    JSValue result = JS_Eval(context, c_script, strlen(c_script), c_file_name, JS_EVAL_TYPE_GLOBAL);
    env->ReleaseStringUTFChars(script, c_script);
    env->ReleaseStringUTFChars(file_name, c_file_name);
    if (JS_IsException(result)) {
        throwJSException(env, context);
        return nullptr;
    }

    if (!executePendingJobLoop(env, runtime, context)) {
        JS_FreeValue(context, result);
        return nullptr;
    }

    JSValue global = JS_GetGlobalObject(context);
    jobject jsObj = toJavaObject(env, thiz, global, result);
    JS_FreeValue(context, global);

    return jsObj;
}

jobject QuickJSWrapper::getGlobalObject(JNIEnv *env, jobject thiz) const {
    JSValue value = JS_GetGlobalObject(context);

    auto value_ptr = reinterpret_cast<jlong>(JS_VALUE_GET_PTR(value));
    jobject result = env->NewObject(jsObjectClass, jsObjectInit, thiz, value_ptr);

    JS_FreeValue(context, value);
    return result;
}

jobject QuickJSWrapper::getProperty(JNIEnv *env, jobject thiz, jlong value, jstring name) {
    JSValue jsObject = JS_MKPTR(JS_TAG_OBJECT, reinterpret_cast<void *>(value));

    const char *propsName = env->GetStringUTFChars(name, JNI_FALSE);
    JSValue propsValue = JS_GetPropertyStr(context, jsObject, propsName);
    env->ReleaseStringUTFChars(name, propsName);
    if (JS_IsException(propsValue)) {
        throwJSException(env, context);
        return nullptr;
    }

    return toJavaObject(env, thiz, jsObject, propsValue);
}

jobject QuickJSWrapper::call(JNIEnv *env, jobject thiz, jlong func, jlong this_obj,
                             jobjectArray args) {
    int argc = env->GetArrayLength(args);
    vector<JSValue> arguments;
    vector<JSValue> freeArguments;
    for (int numArgs = 0; numArgs < argc && !env->ExceptionCheck(); numArgs++) {
        jobject arg = env->GetObjectArrayElement(args, numArgs);
        auto jsArg = toJSValue(env, thiz, arg);
        if (JS_IsException(jsArg)) {
            return nullptr;
        }

        // 基础类型(例如 string )和 Java callback 类型需要使用完 free.
        if (env->IsInstanceOf(arg, stringClass) || env->IsInstanceOf(arg, doubleClass) ||
            env->IsInstanceOf(arg, integerClass) || env->IsInstanceOf(arg, longClass) ||
            env->IsInstanceOf(arg, booleanClass) || env->IsInstanceOf(arg, jsCallFunctionClass)) {
            freeArguments.push_back(jsArg);
        }

        env->DeleteLocalRef(arg);

        arguments.push_back(jsArg);
    }

    JSValue jsObj;
    if (this_obj == 0) {
        jsObj = JS_UNDEFINED;
    } else {
        jsObj = JS_MKPTR(JS_TAG_OBJECT, reinterpret_cast<void *>(this_obj));
    }

    JSValue jsFunc = JS_MKPTR(JS_TAG_OBJECT, reinterpret_cast<void *>(func));

    JSValue ret = JS_Call(context, jsFunc, jsObj, arguments.size(), arguments.data());
    if (JS_IsException(ret)) {
        throwJSException(env, context);
        return nullptr;
    }

    for (JSValue argument : freeArguments) {
        JS_FreeValue(context, argument);
    }

    // release vector by swap.
    vector<JSValue>().swap(arguments);
    vector<JSValue>().swap(freeArguments);

    if (!executePendingJobLoop(env, runtime, context)) {
        JS_FreeValue(context, ret);
        return nullptr;
    }

    return toJavaObject(env, thiz, jsObj, ret);
}

jstring QuickJSWrapper::jsonStringify(JNIEnv *env, jlong value) const {
    JSValue obj = JS_JSONStringify(context, JS_MKPTR(JS_TAG_OBJECT, reinterpret_cast<void *>(value)), JS_UNDEFINED, JS_UNDEFINED);
    if (JS_IsException(obj)){
        throwJSException(env, context);
        return nullptr;
    }

    auto result = JS_ToCString(context, obj);
    JS_FreeValue(context, obj);
    jstring string =env->NewStringUTF(result);
    JS_FreeCString(context, result);
    return string;
}

jint QuickJSWrapper::length(JNIEnv *env, jlong value) const {
    JSValue jsObj = JS_MKPTR(JS_TAG_OBJECT, reinterpret_cast<void *>(value));

    JSValue length = JS_GetPropertyStr(context, jsObj, "length");
    if (JS_IsException(length)) {
        throwJSException(env, context);
        return -1;
    }

    JS_FreeValue(context, length);

    return JS_VALUE_GET_INT(length);
}

jobject QuickJSWrapper::get(JNIEnv *env, jobject thiz, jlong value, jint index) {
    JSValue jsObj = JS_MKPTR(JS_TAG_OBJECT, reinterpret_cast<void *>(value));
    JSValue child = JS_GetPropertyUint32(context, jsObj, index);

    return toJavaObject(env, thiz, jsObj, child);
}

void QuickJSWrapper::set(JNIEnv *env, jobject thiz, jlong this_obj, jobject value, jint index) {
    JSValue jsObj = JS_MKPTR(JS_TAG_OBJECT, reinterpret_cast<void *>(this_obj));
    JSValue child = toJSValue(env, thiz, value);
    JS_SetPropertyUint32(context, jsObj, index, JS_DupValue(context, child));
}

void
QuickJSWrapper::setProperty(JNIEnv *env, jobject thiz, jlong this_obj, jstring name, jobject value) const {
    const char* propName = env->GetStringUTFChars(name, JNI_FALSE);
    JSValue propValue = toJSValue(env, thiz, value);
    if(env->IsInstanceOf(value, jsObjectClass)) {
        // 这里需要手动增加引用计数，不然 QuickJS 垃圾回收会报 assertion "p->ref_count > 0" 的错误。
        JS_DupValue(context, propValue);
    } else if (env->IsInstanceOf(value, jsCallFunctionClass)) {
        // 通过 JS_NewCFunctionData 创建的 fn 对象的 name 属性值被定义为 Empty 了，
        // 这里需要额外定义下，不然 js 层拿到的 fn.name 的值为空.
        JSAtom name_atom = JS_NewAtom(context, propName);
        JSAtom name_atom_key = JS_NewAtom(context, "name");
        JS_DefinePropertyValue(context, propValue, name_atom_key,
                               JS_AtomToString(context, name_atom), JS_PROP_CONFIGURABLE);
        JS_FreeAtom(context, name_atom);
        JS_FreeAtom(context, name_atom_key);
    }

    JSValue jsObj = JS_MKPTR(JS_TAG_OBJECT, reinterpret_cast<void *>(this_obj));
    JS_SetPropertyStr(context, jsObj, propName, propValue);

    env->ReleaseStringUTFChars(name, propName);
}

JSValue QuickJSWrapper::jsFuncCall(int callback_id, JSValueConst this_val, int argc, JSValueConst *argv){
    jobjectArray javaArgs = jniEnv->NewObjectArray((jsize)argc, objectClass, nullptr);

    for (int i = 0; i < argc; i++) {
        auto java_arg = toJavaObject(jniEnv, jniThiz, this_val, argv[i], false);
        jniEnv->SetObjectArrayElement(javaArgs, (jsize)i, java_arg);
        jniEnv->DeleteLocalRef(java_arg);
    }

    auto result = jniEnv->CallObjectMethod(jniThiz, callFunctionBackM, callback_id, javaArgs);

    jniEnv->DeleteLocalRef(javaArgs);

    JSValue jsValue = toJSValue(jniEnv, jniThiz, result);

    // JS 对象作为方法返回值，需要引用计数加1，不然会被释放掉
    if (JS_IsObject(jsValue) && !jniEnv->IsInstanceOf(result, jsCallFunctionClass)) {
        JS_DupValue(context, jsValue);
    }

    jniEnv->DeleteLocalRef(result);
    return jsValue;
}

void QuickJSWrapper::removeCallFunction(int callback_id) const {
    jniEnv->CallVoidMethod(jniThiz, removeCallFunctionM, callback_id);
}

JSValue QuickJSWrapper::toJSValue(JNIEnv *env, jobject thiz, jobject value) const {
    if (value == nullptr) {
        return JS_UNDEFINED;
    }

    JSValue result;
    if (env->IsInstanceOf(value, stringClass)) {
        const auto s = env->GetStringUTFChars((jstring)(value), JNI_FALSE);
        result = JS_NewString(context, s);
        env->ReleaseStringUTFChars((jstring)(value), s);
    } else if (env->IsInstanceOf(value, doubleClass)) {
        result = JS_NewFloat64(context, env->CallDoubleMethod(value, doubleGetValue));
    } else if (env->IsInstanceOf(value, integerClass)) {
        result = JS_NewInt32(context, env->CallIntMethod(value, integerGetValue));
    } else if(env->IsInstanceOf(value, longClass)) {
        result = JS_NewBigInt64(context, env->CallLongMethod(value, longGetValue));
    } else if (env->IsInstanceOf(value, booleanClass)) {
        result = JS_NewBool(context, env->CallBooleanMethod(value, booleanGetValue));
    } else if (env->IsInstanceOf(value, jsObjectClass)) {
        result = JS_MKPTR(JS_TAG_OBJECT, reinterpret_cast<void *>(env->CallLongMethod(value, jsObjectGetValue)));
    } else if (env->IsInstanceOf(value, jsCallFunctionClass)) {
        // 这里的 obj 是用来获取 JSFuncCallback 对象的
        JSValue obj = JS_NewObjectClass(context, js_func_callback_class_id);
        result = JS_NewCFunctionData(context, jsFnCallback, 1, 0, 1, &obj);
        // JS_NewCFunctionData 有 dupValue obj，这里需要对 obj 计数减一，保持计数平衡
        JS_FreeValue(context, obj);

        int *callbackId = new int(jniEnv->CallIntMethod(value, callFunctionHashCodeM));
        JS_SetOpaque(obj, callbackId);
    } else {
        auto classType = env->GetObjectClass(value);
        const auto typeName = getJavaName(env, classType);
        env->DeleteLocalRef(classType);
        // Throw an exception for unsupported argument type.
        throwJavaException(env, "java/lang/IllegalArgumentException", "Unsupported Java type %s",
                           typeName.c_str());
        result = JS_EXCEPTION;
    }

    return result;
}

void QuickJSWrapper::freeValue(jlong value) {
    // current only free exist value in map.
    // todo refactor
    map<jlong, JSValue>::iterator exist;
    exist = values.find(value);
    if (exist != values.end()) {
        values.erase(exist);

        JSValue jsObj = JS_MKPTR(JS_TAG_OBJECT, reinterpret_cast<void *>(value));
        JS_FreeValue(context, jsObj);
    }
}

void QuickJSWrapper::dupValue(jlong value) const {
    JSValue jsObj = JS_MKPTR(JS_TAG_OBJECT, reinterpret_cast<void *>(value));
    JS_DupValue(context, jsObj);
}

void QuickJSWrapper::freeDupValue(jlong value) const {
    JSValue jsObj = JS_MKPTR(JS_TAG_OBJECT, reinterpret_cast<void *>(value));
    JS_FreeValue(context, jsObj);
}

jobject QuickJSWrapper::parseJSON(JNIEnv *env, jobject thiz, jstring json) {
    const char *c_json = env->GetStringUTFChars(json, JNI_FALSE);
    auto jsonObj = JS_ParseJSON(context, c_json, strlen(c_json), "parseJSON.js");
    if (JS_IsException(jsonObj)) {
        throwJSException(env, context);
        return nullptr;
    }

    JSValue jsObj = JS_UNDEFINED;
    jobject result = toJavaObject(env, thiz, jsObj, jsonObj);
    env->ReleaseStringUTFChars(json, c_json);
    return result;
}

jbyteArray QuickJSWrapper::compile(JNIEnv *env, jstring source, jstring file_name) const {
    const auto sourceCode = env->GetStringUTFChars(source, JNI_FALSE);
    const auto fileName = env->GetStringUTFChars(file_name, JNI_FALSE);
    auto compiled = JS_Eval(context, sourceCode, strlen(sourceCode), fileName, JS_EVAL_FLAG_COMPILE_ONLY);
    env->ReleaseStringUTFChars(source, sourceCode);
    env->ReleaseStringUTFChars(file_name, fileName);

    if (JS_IsException(compiled)) {
        throwJSException(env, context);
        return nullptr;
    }

    size_t bufferLength = 0;
    auto buffer = JS_WriteObject(context, &bufferLength, compiled, JS_WRITE_OBJ_BYTECODE | JS_WRITE_OBJ_REFERENCE);

    auto result = buffer && bufferLength > 0 ? env->NewByteArray(bufferLength) : nullptr;
    if (result) {
        env->SetByteArrayRegion(result, 0, bufferLength, reinterpret_cast<const jbyte*>(buffer));
    } else {
        throwJSException(env, context);
    }

    JS_FreeValue(context, compiled);
    js_free(context, buffer);

    return result;
}

jobject QuickJSWrapper::execute(JNIEnv *env, jobject thiz, jbyteArray byteCode) {
    const auto buffer = env->GetByteArrayElements(byteCode, nullptr);
    const auto bufferLength = env->GetArrayLength(byteCode);
    const auto flags = JS_READ_OBJ_BYTECODE | JS_READ_OBJ_REFERENCE;
    auto obj = JS_ReadObject(context, reinterpret_cast<const uint8_t*>(buffer), bufferLength, flags);
    env->ReleaseByteArrayElements(byteCode, buffer, JNI_ABORT);

    if (JS_IsException(obj)) {
        throwJSException(env, context);
        return nullptr;
    }

    if (JS_ResolveModule(context, obj)) {
        // TODO throwJsExceptionFmt(env, this, "Failed to resolve JS module");
        return nullptr;
    }

    auto val = JS_EvalFunction(context, obj);
    jobject result;
    if (!JS_IsException(val)) {
        result = toJavaObject(env, thiz, obj, val);
    } else {
        result = nullptr;
        throwJSException(env, context);
    }

    return result;
}

jobject
QuickJSWrapper::evaluateModule(JNIEnv *env, jobject thiz, jstring script, jstring file_name) {
    const char *c_script = env->GetStringUTFChars(script, JNI_FALSE);
    const char *c_file_name = env->GetStringUTFChars(file_name, JNI_FALSE);

    JSValue result = JS_Eval(context, c_script, strlen(c_script), c_file_name, JS_EVAL_TYPE_MODULE);
    env->ReleaseStringUTFChars(script, c_script);
    env->ReleaseStringUTFChars(file_name, c_file_name);
    if (JS_IsException(result)) {
        throwJSException(env, context);
        return nullptr;
    }

    if (!executePendingJobLoop(env, runtime, context)) {
        JS_FreeValue(context, result);
        return nullptr;
    }

    JSValue global = JS_GetGlobalObject(context);
    jobject jsObj = toJavaObject(env, thiz, global, result);
    JS_FreeValue(context, global);
    return jsObj;
}