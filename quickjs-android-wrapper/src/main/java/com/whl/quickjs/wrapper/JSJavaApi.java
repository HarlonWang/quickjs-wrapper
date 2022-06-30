package com.whl.quickjs.wrapper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by baokang yu on 2022/06/29.
 * setJavaObjectApi 传入的javaObject的方法需要添加此注解，才会被注入到qjs中
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.METHOD})
public @interface JSJavaApi {
}