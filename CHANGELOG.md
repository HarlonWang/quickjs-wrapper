# Change Log

## 2.4.0 *(2024-11-14)*
- 新增方法: 获取使用内存的大小信息（getMemoryUsedSize）
- 支持 ArrayBuffer 转为 Byte 数组（深拷贝，对性能有一些影响）

## 2.2.1 *(2024-09-29)*
- JSObject 增加 toMap 方法，支持转 HashMap 类型

## 2.1.0 *(2024-09-20)*
- 升级 QuickJS 至 2024-0214 版本
- 优化不同平台的 string 转换

## 2.0.0 *(2024-08-01)*
> :warning: 请注意，这是一次比较大的改动，如果是从老版本升级到该版本，需要你自己验证并回归自己的场景！
- JSObject 相关类接口化改造，方便扩展
- 稳定性提升
  - 修复了一些引用计数带来的崩溃异常
  - 优化了函数执行状态判断，避免一些复杂场景下的时机问题
  - 去处部分主动释放引用的逻辑，交由使用方自行释放
  - 其他可能会影响稳定的逻辑优化
- 增加一些内存泄漏的排查能力
- 一些历史 bugfix

## 1.0.0 *(2023-09-28)*
- 修复：模块重复加载问题
- 特性：支持模块字节码编译
- 优化：console 日志模块
- 优化：异常检测逻辑

## 0.21.1 *(2023-08-01)*
- 修复：字节码执行中没有调用 `executePendingJobLoop`

## 0.21.0 *(2023-07-28)*
- 优化：DumpObjects 日志输出到指定文件中方便查看
- 优化：JSObject.toString 和 JavaScript 保持一致
- 文档：修证书写错误

## 0.20.2 *(2023-06-30)*
- 修复: cmake 构建提示 floor 方法的头文件缺失问题

## 0.20.1 *(2023-06-14)*
- 特性: 优化 Long 和 Number、 BigInt 类型的互转逻辑

## 0.20.0 *(2023-06-07)*
- 修复: JSString 对象没有释放引起的泄漏问题
- 优化: 移除 js 层对 Array.at 的支持，由 c 层实现

## 0.19.3 *(2023-06-07)*

- 修复：JNI 层 hashCode 无法获取问题

## 0.19.2 *(2023-06-06)*

- 特性：支持 `Long` 类型数据在 JavaScript 中的传递

## 0.19.1 *(2023-06-05)*

- 修复 `Attempt to remove non-JNI local reference` 的错误警告
- 修复 `QuickJSWrapper` 析构函数执行时可能会出现的 `use deleted global reference` 异常
- 重构 `JSCallFunction` 的绑定逻辑，避免使用 NewGlobalRef 带来的 global reference table overflow (max=51200) 异常
- 集成 GitHub Action 提升版本发布效率