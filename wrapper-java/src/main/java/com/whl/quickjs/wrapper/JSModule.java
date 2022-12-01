package com.whl.quickjs.wrapper;

/**
 * Created by Harlon Wang on 2021/10/12.
 */
public final class JSModule {

    private static JSModuleLoader sModuleLoader;

    public static void setModuleLoader(JSModuleLoader sModuleLoader) {
        JSModule.sModuleLoader = sModuleLoader;
    }

    static String getModuleScript(String moduleName) {
        return sModuleLoader.getModuleScript(moduleName);
    }

    static String convertModuleName(String moduleBaseName, String moduleName) {
        return sModuleLoader.convertModuleName(moduleBaseName, moduleName);
    }

}
