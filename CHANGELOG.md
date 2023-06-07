# Change Log

## 0.19.3 *(2023-06-07)*

- 修复：JNI 层 hashCode 无法获取问题

## 0.19.2 *(2023-06-06)*

- 特性：支持 `Long` 类型数据在 JavaScript 中的传递

## 0.19.1 *(2023-06-05)*

- 修复 `Attempt to remove non-JNI local reference` 的错误警告
- 修复 `QuickJSWrapper` 析构函数执行时可能会出现的 `use deleted global reference` 异常
- 重构 `JSCallFunction` 的绑定逻辑，避免使用 NewGlobalRef 带来的 global reference table overflow (max=51200) 异常
- 集成 GitHub Action 提升版本发布效率