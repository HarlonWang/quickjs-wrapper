# quickjs-android-wrapper
coming soon

# Other
## 一些说明：
1. getProperty 多次获取同一个 JSObject，需要每次 free 下，保持计数平衡
2. 作为参数的 JSObject 不可以 FreeValue 释放，会报错，猜测是参数的 JSObject 会自动释放，不需要额外 FreeValue

## 常见错误：
1. fault addr 0x18 in tid 22363
一般是 JSValue 已经被 FreeValue，再次调用就会报这个错误

2. list_empty(&rt->gc_obj_list)" failed
一般是使用的 JSValue 没有被 FreeValue 导致