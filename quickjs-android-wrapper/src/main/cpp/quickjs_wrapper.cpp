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
    const auto script = env->GetStringUTFChars(static_cast<jstring>(result), JNI_FALSE);

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

static JSValue js_print(JSContext *ctx, JSValueConst this_val,
                        int argc, JSValueConst *argv)
{
    int i;
    string str;
    size_t len;

    for(i = 0; i < argc; i++) {
        if (i != 0)
            str += ' ';
        const char *arg_str = JS_ToCStringLen(ctx, &len, argv[i]);
        if (!arg_str)
            return JS_EXCEPTION;
        str += arg_str;
        JS_FreeCString(ctx, arg_str);
    }
    __android_log_print(ANDROID_LOG_DEBUG, "tiny-console", "%s", str.c_str());
    return JS_UNDEFINED;
}

static void js_std_add_helpers(JSContext *ctx)
{
    JSValue global_obj, console;

    /* XXX: should these global definitions be enumerable? */
    global_obj = JS_GetGlobalObject(ctx);

    console = JS_NewObject(ctx);
    JS_SetPropertyStr(ctx, console, "log",
                      JS_NewCFunction(ctx, js_print, "log", 1));
    JS_SetPropertyStr(ctx, global_obj, "console", console);

    JS_FreeValue(ctx, global_obj);
}

QuickJSWrapper::QuickJSWrapper(JNIEnv *env) {
    jniEnv = env;
    runtime = JS_NewRuntime();

    // init ES6Module
    JS_SetModuleLoaderFunc(runtime, jsModuleNormalizeFunc, jsModuleLoaderFunc, nullptr);

    context = JS_NewContext(runtime);

    JS_SetRuntimeOpaque(runtime, this);

    js_std_add_helpers(context);

    jsClassId = 0;

    objectClass = static_cast<jclass>(jniEnv->NewGlobalRef(jniEnv->FindClass("java/lang/Object")));
    booleanClass = static_cast<jclass>(jniEnv->NewGlobalRef(jniEnv->FindClass("java/lang/Boolean")));
    integerClass = static_cast<jclass>(jniEnv->NewGlobalRef(jniEnv->FindClass("java/lang/Integer")));
    doubleClass = static_cast<jclass>(jniEnv->NewGlobalRef(jniEnv->FindClass("java/lang/Double")));
    stringClass = static_cast<jclass>(jniEnv->NewGlobalRef(jniEnv->FindClass("java/lang/String")));
    jsObjectClass = static_cast<jclass>(jniEnv->NewGlobalRef(jniEnv->FindClass("com/whl/quickjs/wrapper/JSObject")));
    jsArrayClass = static_cast<jclass>(jniEnv->NewGlobalRef(jniEnv->FindClass("com/whl/quickjs/wrapper/JSArray")));
    jsFunctionClass = static_cast<jclass>(jniEnv->NewGlobalRef(jniEnv->FindClass("com/whl/quickjs/wrapper/JSFunction")));
    jsCallFunctionClass = static_cast<jclass>(jniEnv->NewGlobalRef(jniEnv->FindClass("com/whl/quickjs/wrapper/JSCallFunction")));
    jsModuleClass = static_cast<jclass>(jniEnv->NewGlobalRef(jniEnv->FindClass("com/whl/quickjs/wrapper/JSModule")));

    booleanValueOf = jniEnv->GetStaticMethodID(booleanClass, "valueOf", "(Z)Ljava/lang/Boolean;");
    integerValueOf = jniEnv->GetStaticMethodID(integerClass, "valueOf", "(I)Ljava/lang/Integer;");
    doubleValueOf = jniEnv->GetStaticMethodID(doubleClass, "valueOf", "(D)Ljava/lang/Double;");

    booleanGetValue = jniEnv->GetMethodID(booleanClass, "booleanValue", "()Z");
    integerGetValue = jniEnv->GetMethodID(integerClass, "intValue", "()I");
    doubleGetValue = jniEnv->GetMethodID(doubleClass, "doubleValue", "()D");
    jsObjectGetValue = jniEnv->GetMethodID(jsObjectClass, "getPointer", "()J");

    jsObjectInit = jniEnv->GetMethodID(jsObjectClass, "<init>", "(Lcom/whl/quickjs/wrapper/QuickJSContext;J)V");
    jsArrayInit = jniEnv->GetMethodID(jsArrayClass, "<init>", "(Lcom/whl/quickjs/wrapper/QuickJSContext;J)V");
    jsFunctionInit = jniEnv->GetMethodID(jsFunctionClass, "<init>","(Lcom/whl/quickjs/wrapper/QuickJSContext;JJ)V");
    jsConvertModuleName = jniEnv->GetStaticMethodID(jsModuleClass, "convertModuleName",
                                              "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
    jsGetModuleScript = jniEnv->GetStaticMethodID(jsModuleClass, "getModuleScript", "(Ljava/lang/String;)Ljava/lang/String;");
}

QuickJSWrapper::~QuickJSWrapper() {
    jniEnv->DeleteGlobalRef(objectClass);
    jniEnv->DeleteGlobalRef(doubleClass);
    jniEnv->DeleteGlobalRef(integerClass);
    jniEnv->DeleteGlobalRef(booleanClass);
    jniEnv->DeleteGlobalRef(stringClass);
    jniEnv->DeleteGlobalRef(jsObjectClass);
    jniEnv->DeleteGlobalRef(jsArrayClass);
    jniEnv->DeleteGlobalRef(jsFunctionClass);
    jniEnv->DeleteGlobalRef(jsCallFunctionClass);
    jniEnv->DeleteGlobalRef(jsModuleClass);

    map<jlong, JSValue>::iterator i;
    for (i = values.begin(); i != values.end(); ++i) {
        JSValue item = i->second;

//        auto text = stringify(item);
//        __android_log_print(ANDROID_LOG_DEBUG, "quickjs-native-wrapper", "free value=%s", text);
//        JS_FreeCString(context, text);

        JS_FreeValue(context, item);
    }
    values.clear();

    JS_FreeContext(context);

    // todo try catch
    // void JS_FreeRuntime(JSRuntime *): assertion "list_empty(&rt->gc_obj_list)" failed
    JS_FreeRuntime(runtime);
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
//                    auto text = stringify(value);
//                    __android_log_print(ANDROID_LOG_DEBUG, "quickjs-native-wrapper", "insert value=%s", text);
//                    JS_FreeCString(context, text);

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

    JSValue result = evaluate(c_script, c_file_name);

    env->ReleaseStringUTFChars(script, c_script);
    env->ReleaseStringUTFChars(file_name, c_file_name);

    JSValue global = getGlobalObject();
    jobject jsObj = toJavaObject(env, thiz, global, result);
    JS_FreeValue(context, global);
    return jsObj;
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
    auto result = JS_ToCString(context, checkNotException(obj));
    JS_FreeValue(context, obj);
    return result;
}

JSValue QuickJSWrapper::checkNotException(JSValue &value) const {
    if (JS_IsException(value)) {
        const char* error = js_std_dump_error(context);
        throwJavaException(jniEnv, "android/util/AndroidRuntimeException",
                           error);
    }

    return value;
}

jobject QuickJSWrapper::getGlobalObject(JNIEnv *env, jobject thiz) {
    JSValue value = getGlobalObject();

    auto value_ptr = reinterpret_cast<jlong>(JS_VALUE_GET_PTR(value));
    jobject result = env->NewObject(jsObjectClass, jsObjectInit, thiz, value_ptr);

    JS_FreeValue(context, value);
    return result;
}

jobject QuickJSWrapper::getProperty(JNIEnv *env, jobject thiz, jlong value, jstring name) {
    JSValue jsObject = JS_MKPTR(JS_TAG_OBJECT, reinterpret_cast<void *>(value));

    const char *propsName = env->GetStringUTFChars(name, JNI_FALSE);
    JSValue propsValue = getProperty(jsObject, propsName);

    env->ReleaseStringUTFChars(name, propsName);

    return toJavaObject(env, thiz, jsObject, propsValue);
}

jobject QuickJSWrapper::call(JNIEnv *env, jobject thiz, jlong func, jlong this_obj,
                             jobjectArray args) {
    int argc = env->GetArrayLength(args);
    vector<JSValue> arguments;
    for (int numArgs = 0; numArgs < argc && !env->ExceptionCheck(); numArgs++) {
        jobject arg = env->GetObjectArrayElement(args, numArgs);
        auto jsArg = toJSValue(env, arg);
        if (JS_IsException(jsArg)) {
            return nullptr;
        }

        arguments.push_back(jsArg);
        env->DeleteLocalRef(arg);
    }

    JSValue jsObj = JS_MKPTR(JS_TAG_OBJECT, reinterpret_cast<void *>(this_obj));
    JSValue jsFunc = JS_MKPTR(JS_TAG_OBJECT, reinterpret_cast<void *>(func));

    JSValue funcRet = call(jsFunc, jsObj, arguments.size(), arguments.data());

    // todo refactor
    // JS_FreeValue(context, jsObj);
    // JS_FreeValue(context, jsFunc);

    for (JSValue argument : arguments) {
        if (!JS_IsObject(argument)) {
            JS_FreeValue(context, argument);
        }
    }

    return toJavaObject(env, thiz, jsObj, funcRet);
}

jstring QuickJSWrapper::stringify(JNIEnv *env, jlong value) const {
    JSValue jsObj = JS_MKPTR(JS_TAG_OBJECT, reinterpret_cast<void *>(value));
    const char *result = stringify(jsObj);
    jstring string =env->NewStringUTF(result);
    JS_FreeCString(context, result);
    return string;
}

jint QuickJSWrapper::length(JNIEnv *env, jlong value) {
    JSValue jsObj = JS_MKPTR(JS_TAG_OBJECT, reinterpret_cast<void *>(value));

    JSValue length = getProperty(jsObj, "length");
    JS_FreeValue(context, length);

    return JS_VALUE_GET_INT(length);
}

jobject QuickJSWrapper::get(JNIEnv *env, jobject thiz, jlong value, jint index) {
    JSValue jsObj = JS_MKPTR(JS_TAG_OBJECT, reinterpret_cast<void *>(value));
    JSValue child = JS_GetPropertyUint32(context, jsObj, index);

    return toJavaObject(env, thiz, jsObj, child);
}

void
QuickJSWrapper::setProperty(JNIEnv *env, jobject thiz, jlong this_obj, jstring name, jobject value) {
    auto classType = env->GetObjectClass(value);

    JSValue propValue;
    if (env->IsAssignableFrom(classType, stringClass)) {
        const auto s = env->GetStringUTFChars(static_cast<jstring>(value), JNI_FALSE);
        propValue = JS_NewString(context, s);
        env->ReleaseStringUTFChars(static_cast<jstring>(value), s);
    } else if (env->IsAssignableFrom(classType, doubleClass)) {
        propValue = JS_NewFloat64(context, env->CallDoubleMethod(value, doubleGetValue));
    } else if (env->IsAssignableFrom(classType, integerClass)) {
        propValue = JS_NewInt32(context, env->CallIntMethod(value, integerGetValue));
    } else if (env->IsAssignableFrom(classType, booleanClass)) {
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
                    return;
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
        } else if(env->IsInstanceOf(value, jsObjectClass)) {
            propValue = JS_MKPTR(JS_TAG_OBJECT, reinterpret_cast<void *>(env->CallLongMethod(value, jsObjectGetValue)));
        } else {
            const auto typeName = getName(env, classType);
            // Throw an exception for unsupported argument type.
            throwJavaException(env, "java/lang/IllegalArgumentException", "Unsupported Java type %s",
                               typeName.c_str());
            return;
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
        auto java_arg = toJavaObject(jniEnv, thiz, this_val, argv[i], false);
        jniEnv->SetObjectArrayElement(javaArgs, (jsize)i, java_arg);
        jniEnv->DeleteLocalRef(java_arg);
    }

    auto funcClass = jniEnv->GetObjectClass(func_value);
    auto funcMethodId = jniEnv->GetMethodID(funcClass, "call", "([Ljava/lang/Object;)Ljava/lang/Object;");
    auto result = jniEnv->CallObjectMethod(func_value, funcMethodId, javaArgs);

    jniEnv->DeleteLocalRef(javaArgs);

    return toJSValue(jniEnv, result);
}

JSValue QuickJSWrapper::toJSValue(JNIEnv *env, jobject value) const {
    if (!value) {
        return JS_UNDEFINED;
    }

    auto classType = env->GetObjectClass(value);

    JSValue result;
    if (env->IsAssignableFrom(classType, stringClass)) {
        const auto s = env->GetStringUTFChars(static_cast<jstring>(value), JNI_FALSE);
        result = JS_NewString(context, s);
        env->ReleaseStringUTFChars(static_cast<jstring>(value), s);
    } else if (env->IsAssignableFrom(classType, doubleClass)) {
        result = JS_NewFloat64(context, env->CallDoubleMethod(value, doubleGetValue));
    } else if (env->IsAssignableFrom(classType, integerClass)) {
        result = JS_NewInt32(context, env->CallIntMethod(value, integerGetValue));
    } else if (env->IsAssignableFrom(classType, booleanClass)) {
        result = JS_NewBool(context, env->CallBooleanMethod(value, booleanGetValue));
    } else if (env->IsInstanceOf(value, jsObjectClass)) {
        result = JS_MKPTR(JS_TAG_OBJECT, reinterpret_cast<void *>(env->CallLongMethod(value, jsObjectGetValue)));
    } else {
        const auto typeName = getName(env, classType);
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
    JSValue jsObj = JS_UNDEFINED;
    jobject result = toJavaObject(env, thiz, jsObj, jsonObj);
    env->ReleaseStringUTFChars(json, c_json);
    return result;
}

jbyteArray QuickJSWrapper::compile(JNIEnv *env, jstring source) const {
    const auto sourceCode = env->GetStringUTFChars(source, 0);
    auto compiled = JS_Eval(context, sourceCode, strlen(sourceCode), "compile.js", JS_EVAL_FLAG_COMPILE_ONLY);
    env->ReleaseStringUTFChars(source, sourceCode);

    if (JS_IsException(compiled)) {
        // TODO throwJsException(env, compiled);
        return nullptr;
    }

    size_t bufferLength = 0;
    auto buffer = JS_WriteObject(context, &bufferLength, compiled, JS_WRITE_OBJ_BYTECODE | JS_WRITE_OBJ_REFERENCE);

    auto result = buffer && bufferLength > 0 ? env->NewByteArray(bufferLength) : nullptr;
    if (result) {
        env->SetByteArrayRegion(result, 0, bufferLength, reinterpret_cast<const jbyte*>(buffer));
    } else {
        // TODO throwJsException(env, compiled);
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
        // TODO throwJsException(env, obj);
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
        // TODO throwJsException(env, val);
    }
    JS_FreeValue(context, val);

    return result;
}

jobject
QuickJSWrapper::evaluateModule(JNIEnv *env, jobject thiz, jstring script, jstring file_name) {
    const char *c_script = env->GetStringUTFChars(script, JNI_FALSE);
    const char *c_file_name = env->GetStringUTFChars(file_name, JNI_FALSE);

    JSValue result = evaluate(c_script, c_file_name, JS_EVAL_TYPE_MODULE);

    env->ReleaseStringUTFChars(script, c_script);
    env->ReleaseStringUTFChars(file_name, c_file_name);

    JSValue global = getGlobalObject();
    jobject jsObj = toJavaObject(env, thiz, global, result);
    JS_FreeValue(context, global);
    return jsObj;
}

jint QuickJSWrapper::executePendingJob() const {
    JSContext *jobCtx;
    return JS_ExecutePendingJob(runtime, &jobCtx);
}

string getName(JNIEnv* env, jobject javaClass) {
    auto classType = env->GetObjectClass(javaClass);
    const auto method = env->GetMethodID(classType, "getName", "()Ljava/lang/String;");
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

static const char* js_dump_obj(JSContext *ctx, JSValueConst val)
{
    const char *str;

    str = JS_ToCString(ctx, val);
    if (str) {
        return str;
    } else {
        return "[exception]";
    }
}

const char* js_std_dump_error(JSContext *ctx) {
    JSValue exception_val;

    exception_val = JS_GetException(ctx);

    JSValue val;
    bool is_error;
    is_error = JS_IsError(ctx, exception_val);
    string jsException = js_dump_obj(ctx, exception_val);
    if (is_error) {
        val = JS_GetPropertyStr(ctx, exception_val, "stack");
        if (!JS_IsUndefined(val)) {
            jsException += "\n";
            jsException += js_dump_obj(ctx, val);
        }
        JS_FreeValue(ctx, val);
    }

    JS_FreeValue(ctx, exception_val);
    const char* errorStr = jsException.c_str();
    return errorStr;
}