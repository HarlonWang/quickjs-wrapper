//
// Created by yonglan.whl on 2021/7/14.
//
#include "quickjs_wrapper.h"

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

static const char* js_std_dump_error(JSContext *ctx) {
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


// js function callback
static JSClassID js_func_callback_class_id;

typedef struct {
    jobject value;
    jobject thiz;
} JSFuncCallback;

static void js_func_callback_finalizer(JSRuntime *rt, JSValue val) {
    auto wrapper = reinterpret_cast<const QuickJSWrapper*>(JS_GetRuntimeOpaque(rt));
    if (wrapper) {
        auto *jsFc = reinterpret_cast<JSFuncCallback *>(JS_GetOpaque2(wrapper->context, val, js_func_callback_class_id));
        if (jsFc) {
            wrapper->jniEnv->DeleteGlobalRef(jsFc->thiz);
            wrapper->jniEnv->DeleteGlobalRef(jsFc->value);
            delete jsFc;
        }
    }
}

static JSClassDef js_func_callback_class = {
        "JSFuncCallback",
        .finalizer = js_func_callback_finalizer,
};

static JSValue jsFnCallback(JSContext *ctx,
                            JSValueConst this_obj,
                            int argc, JSValueConst *argv,
                            int magic, JSValue *func_data) {

    auto *jsFc = reinterpret_cast<JSFuncCallback *>(JS_GetOpaque2(ctx, func_data[0], js_func_callback_class_id));
    auto wrapper = reinterpret_cast<QuickJSWrapper*>(JS_GetRuntimeOpaque(JS_GetRuntime(ctx)));
    return wrapper->jsFuncCall(jsFc->value, jsFc->thiz, this_obj, argc, argv);
}

static void js_func_callback_init(JSContext *ctx) {
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

// js print
static JSValue js_c_func_print(JSContext *ctx, JSValueConst this_val,
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
    __android_log_print(ANDROID_LOG_DEBUG, "qjs-console", "%s", str.c_str());
    return JS_UNDEFINED;
}

static void js_print_init(JSContext *ctx) {
    JSValue global_obj, console;

    /* XXX: should these global definitions be enumerable? */
    global_obj = JS_GetGlobalObject(ctx);

    console = JS_NewObject(ctx);
    JS_SetPropertyStr(ctx, console, "log",
                      JS_NewCFunction(ctx, js_c_func_print, "log", 1));
    JS_SetPropertyStr(ctx, global_obj, "console", console);

    JS_FreeValue(ctx, global_obj);
}

// js format string
static void js_format_string_init(JSContext *ctx) {
    const char* format_string_script = R"lit(function __format_string(a) {
    var stack = [];
    var string = '';

    function format_rec(a) {
        var n, i, keys, key, type;

        type = typeof(a);
        if (type === "object") {
            if (a === null) {
                string += a;
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

static void js_std_add_helpers(JSContext *ctx)
{
    js_print_init(ctx);
    js_format_string_init(ctx);
}

QuickJSWrapper::QuickJSWrapper(JNIEnv *env) {
    jniEnv = env;
    runtime = JS_NewRuntime();

    // init ES6Module
    JS_SetModuleLoaderFunc(runtime, jsModuleNormalizeFunc, jsModuleLoaderFunc, nullptr);

    context = JS_NewContext(runtime);

    JS_SetRuntimeOpaque(runtime, this);
    js_func_callback_init(context);

    js_std_add_helpers(context);

    objectClass = (jclass)(jniEnv->NewGlobalRef(jniEnv->FindClass("java/lang/Object")));
    booleanClass = (jclass)(jniEnv->NewGlobalRef(jniEnv->FindClass("java/lang/Boolean")));
    integerClass = (jclass)(jniEnv->NewGlobalRef(jniEnv->FindClass("java/lang/Integer")));
    doubleClass = (jclass)(jniEnv->NewGlobalRef(jniEnv->FindClass("java/lang/Double")));
    stringClass = (jclass)(jniEnv->NewGlobalRef(jniEnv->FindClass("java/lang/String")));
    jsObjectClass = (jclass)(jniEnv->NewGlobalRef(jniEnv->FindClass("com/whl/quickjs/wrapper/JSObject")));
    jsArrayClass = (jclass)(jniEnv->NewGlobalRef(jniEnv->FindClass("com/whl/quickjs/wrapper/JSArray")));
    jsFunctionClass = (jclass)(jniEnv->NewGlobalRef(jniEnv->FindClass("com/whl/quickjs/wrapper/JSFunction")));
    jsCallFunctionClass = (jclass)(jniEnv->NewGlobalRef(jniEnv->FindClass("com/whl/quickjs/wrapper/JSCallFunction")));
    jsModuleClass = (jclass)(jniEnv->NewGlobalRef(jniEnv->FindClass("com/whl/quickjs/wrapper/JSModule")));

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
    return checkJSException(val);
}

JSValue QuickJSWrapper::getGlobalObject() const {
    JSValue val = JS_GetGlobalObject(context);
    return checkJSException(val);
}

JSValue QuickJSWrapper::getProperty(JSValue &this_obj, const char *propName) const {
    JSValue val = JS_GetPropertyStr(context, this_obj, propName);
    return checkJSException(val);
}

int QuickJSWrapper::setProperty(JSValue &this_obj, const char *propName, JSValue &val) const {
    return JS_SetPropertyStr(context, this_obj, propName, val);
}

JSValue QuickJSWrapper::call(JSValue &func_obj, JSValue &this_obj, int argc, JSValue *argv) const {
    JSValue val = JS_Call(context, func_obj, this_obj, argc, argv);
    return checkJSException(val);
}

const char * QuickJSWrapper::stringify(JSValue &value) const {
    JSValue obj = JS_JSONStringify(context, value, JS_UNDEFINED, JS_UNDEFINED);
    auto result = JS_ToCString(context, checkJSException(obj));
    JS_FreeValue(context, obj);
    return result;
}

JSValue QuickJSWrapper::checkJSException(JSValue &value) const {
    if (JS_IsException(value)) {
        throwJSException(value);
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

void QuickJSWrapper::set(JNIEnv *env, jobject thiz, jlong this_obj, jobject value, jint index) {
    JSValue jsObj = JS_MKPTR(JS_TAG_OBJECT, reinterpret_cast<void *>(this_obj));
    JSValue child = toJSValue(env, value);
    JS_SetPropertyUint32(context, jsObj, index, child);
}

void
QuickJSWrapper::setProperty(JNIEnv *env, jobject thiz, jlong this_obj, jstring name, jobject value) {
    JSValue propValue;
    if (value == nullptr) {
        propValue = JS_UNDEFINED;
    } else {
        auto classType = env->GetObjectClass(value);
        if (env->IsAssignableFrom(classType, stringClass)) {
            const auto s = env->GetStringUTFChars((jstring)(value), JNI_FALSE);
            propValue = JS_NewString(context, s);
            env->ReleaseStringUTFChars((jstring)(value), s);
        } else if (env->IsAssignableFrom(classType, doubleClass)) {
            propValue = JS_NewFloat64(context, env->CallDoubleMethod(value, doubleGetValue));
        } else if (env->IsAssignableFrom(classType, integerClass)) {
            propValue = JS_NewInt32(context, env->CallIntMethod(value, integerGetValue));
        } else if (env->IsAssignableFrom(classType, booleanClass)) {
            propValue = JS_NewBool(context, env->CallBooleanMethod(value, booleanGetValue));
        } else {
            if (env->IsInstanceOf(value, jsCallFunctionClass)) {
                // 这里的 obj 是用来获取 JSFuncCallback 对象的
                JSValue obj = JS_NewObjectClass(context, js_func_callback_class_id);
                propValue = JS_NewCFunctionData(context, jsFnCallback, 1, 0, 1, &obj);
                // 因为 JS_NewCFunctionData 有 dupValue obj，这里需要对 obj 计数减一，保持计数平衡
                JS_FreeValue(context, obj);

                auto *jsFc = new JSFuncCallback;
                jsFc->value = env->NewGlobalRef(value);
                jsFc->thiz = env->NewGlobalRef(thiz);

                JS_SetOpaque(obj, jsFc);

            } else if(env->IsInstanceOf(value, jsObjectClass)) {
                propValue = JS_MKPTR(JS_TAG_OBJECT, reinterpret_cast<void *>(env->CallLongMethod(value, jsObjectGetValue)));
                // 这里需要手动增加引用计数，不然 QuickJS 垃圾回收会报 assertion "p->ref_count > 0" 的错误。
                JS_DupValue(context, propValue);
            } else {
                const auto typeName = getJavaName(env, classType);
                // Throw an exception for unsupported argument type.
                throwJavaException(env, "java/lang/IllegalArgumentException", "Unsupported Java type %s",
                                   typeName.c_str());
                return;
            }
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

    jniEnv->DeleteLocalRef(funcClass);
    jniEnv->DeleteLocalRef(javaArgs);

    JSValue jsValue = toJSValue(jniEnv, result);

    if (JS_IsObject(jsValue)) {
        // JS 对象作为方法返回值，需要引用计数加1，不然会被释放掉
        JS_DupValue(context, jsValue);
    }

    return jsValue;
}

JSValue QuickJSWrapper::toJSValue(JNIEnv *env, jobject value) const {
    if (!value) {
        return JS_UNDEFINED;
    }

    auto classType = env->GetObjectClass(value);

    JSValue result;
    if (env->IsAssignableFrom(classType, stringClass)) {
        const auto s = env->GetStringUTFChars((jstring)(value), JNI_FALSE);
        result = JS_NewString(context, s);
        env->ReleaseStringUTFChars((jstring)(value), s);
    } else if (env->IsAssignableFrom(classType, doubleClass)) {
        result = JS_NewFloat64(context, env->CallDoubleMethod(value, doubleGetValue));
    } else if (env->IsAssignableFrom(classType, integerClass)) {
        result = JS_NewInt32(context, env->CallIntMethod(value, integerGetValue));
    } else if (env->IsAssignableFrom(classType, booleanClass)) {
        result = JS_NewBool(context, env->CallBooleanMethod(value, booleanGetValue));
    } else if (env->IsInstanceOf(value, jsObjectClass)) {
        result = JS_MKPTR(JS_TAG_OBJECT, reinterpret_cast<void *>(env->CallLongMethod(value, jsObjectGetValue)));
    } else {
        const auto typeName = getJavaName(env, classType);
        // Throw an exception for unsupported argument type.
        throwJavaException(env, "java/lang/IllegalArgumentException", "Unsupported Java type %s",
                           typeName.c_str());
        result = JS_EXCEPTION;
    }

    env->DeleteLocalRef(classType);
    env->DeleteLocalRef(value);

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
        throwJSException(jsonObj);
        return nullptr;
    }

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
        throwJSException(compiled);
        return nullptr;
    }

    size_t bufferLength = 0;
    auto buffer = JS_WriteObject(context, &bufferLength, compiled, JS_WRITE_OBJ_BYTECODE | JS_WRITE_OBJ_REFERENCE);

    auto result = buffer && bufferLength > 0 ? env->NewByteArray(bufferLength) : nullptr;
    if (result) {
        env->SetByteArrayRegion(result, 0, bufferLength, reinterpret_cast<const jbyte*>(buffer));
    } else {
        throwJSException(compiled);
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
        throwJSException(obj);
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
        throwJSException(val);
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

void QuickJSWrapper::throwJSException(const JSValue &value) const {
    const char* error = js_std_dump_error(context);
    throwJavaException(jniEnv, "com/whl/quickjs/wrapper/QuickJSException",
                       error);
}

void QuickJSWrapper::setMaxStackSize(jint stack_size) const {
    JS_SetMaxStackSize(runtime, stack_size);
}
