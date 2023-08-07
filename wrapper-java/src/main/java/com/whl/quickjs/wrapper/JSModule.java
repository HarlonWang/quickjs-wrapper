package com.whl.quickjs.wrapper;

/**
 * Created by Harlon Wang on 2021/10/12.
 */
public final class JSModule {

    private static ModuleLoader sModuleLoader;

    public static abstract class ModuleLoader {
        public abstract String getModuleScript(String moduleName);
    }

    public static void setModuleLoader(ModuleLoader moduleLoader) {
        sModuleLoader = moduleLoader;
    }

    static String getModuleScript(String moduleName) {
        return sModuleLoader.getModuleScript(moduleName);
    }

}
