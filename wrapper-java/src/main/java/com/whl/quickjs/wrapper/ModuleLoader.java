package com.whl.quickjs.wrapper;

/**
 * Created by Harlon Wang on 2023/8/26.
 */
public interface ModuleLoader {
    /**
     * 模块加载模式：
     * True 会调用 {@link #getModuleBytecode(String)}
     * False 会调用 {@link #getModuleStringCode(String)}
     * @return 是否字节码模式
     */
    boolean isBytecodeMode();

    /**
     * 获取字节码代码内容
     * @param moduleName 模块路径名，例如 "xxx.js"
     * @return 代码内容
     */
    byte[] getModuleBytecode(String moduleName);

    /**
     * 获取字符串代码内容
     * @param moduleName 模块路径名，例如 "xxx.js"
     * @return 代码内容
     */
    String getModuleStringCode(String moduleName);

}
