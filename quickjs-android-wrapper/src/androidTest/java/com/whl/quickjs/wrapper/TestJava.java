package com.whl.quickjs.wrapper;

public class TestJava {

    @JSJavaApi
    public int test1(String message) {
        System.out.println("==TestJava==test1 " + message);
        return 1;
    }

    @JSJavaApi
    public int test2(String message, int age) {
        System.out.println("==TestJava==test2 " + message + "---" + age);
        return 1;
    }

    @JSJavaApi
    public int testArray(JSArray message) {
        System.out.println("==TestJava==testArray " + message);
        return 1;
    }

    @JSJavaApi
    public int testObject(JSObject object) {
        System.out.println("==TestJava==testObject " + object);
        return 1;
    }

    @JSJavaApi
    public int testAll(JSObject object, JSArray message, int age, String name) {
        System.out.println("==TestJava==testAll " + object + "," + message + "," + age + "," + name);
        return 1;
    }

}
