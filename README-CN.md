# QuickJS For Android
Android 下的 QuickJS Binding 库

## 特性
- 支持 Java 和 JavaScript 类型互转
- 支持 Promise 异步执行
- 支持字节码编译和执行
- 支持模块加载执行
- 内置 `console.log` 实现
- 统一的 JavaScript 异常处理

## 依赖

```Groovy
repositories {
  mavenCentral()
}
        
dependencies {
  implementation 'wang.harlon.quickjs:wrapper:0.0.18'
}
```

## 使用

### Create QuickJSContext

```Java
QuickJSContext context = QuickJSContext.create();
```

### Evaluating JavaScript

```Java
QuickJSContext context = QuickJSContext.create();
context.evaluate("var a = 1 + 2;");
```

### 支持的 Java 类型
以下的 Java 类型可以直接转换到 JavaScript 中使用:
- `boolean`
- `int` 
- `double`
- `String`
- `null` 
- `JSObject` 代表一个 JavaScript 对象
- `JSFunction` 代表一个 JavaScript 方法
- `JSArray` 代表一个 JavaScript 数组
                
### 属性设置
Java

```java
JSObject globalObj = context.getGlobalObject();
JSObject obj1 = context.createNewJSObject();
obj1.setProperty("stringProperty", "hello");
obj1.setProperty("intProperty", 1);
obj1.setProperty("doubleProperty", 0.1);
obj1.setProperty("booleanProperty", true);
obj1.setProperty("functionProperty", (JSCallFunction) args -> {
    return args[0] + "Wang";
});
globalObj.setProperty("obj1", obj1);
```

JavaScript

```javascript
obj1.stringProperty; // hello string
obj1.intProperty; // 1
obj1.doubleProperty; // 0.1
obj1.booleanProperty; // true
obj1.functionProperty('Harlon'); // HarlonWang
```                

### 属性获取
JavaScript

```JavaScript
var obj1 = {
	stringProperty: 'hello string',
	intProperety: 1,
	doubleProperty: 0.1,
	booleanProperty: true,
	functionProperty: (name) => { return name + 'Wang'; }
}
```
Java

```Java
JSObject globalObject = context.getGlobalObject();
JSObject obj1 = globalObject.getJSObjectProperty("obj1");
obj1.getProperty("stringProperty"); // hello
obj1.getProperty("intProperty"); // 1
obj1.getProperty("doubleProperty"); // 0.1
obj1.getProperty("booleanProperty"); // true
obj1.getJSFunctionProperty("functionProperty").call("Harlon"); // HarlonWang
```

### 编译和执行字节码

```Java
byte[] code = context.compile("'hello, world!'.toUpperCase();");
context.execute(code);
```

### ESModule 模块加载和执行
Java
```Java
JSModule.setModuleLoader(new JSModule.Loader() {
    @Override
    public String getModuleScript(String moduleName) {
        return "export var name = 'Hello world';\n" +
                "export var age = 18;";
    }
});
```
JavaScript
```JavaScript
import {name, age} from './a.js';

console.log('name：' + name); // Jack
console.log('age：' + age); // 18
```

## 同步
所有的 JavaScript 代码执行都是单线程，基于创建时的线程决定，不支持多线程切换。

## 参考

- [quickjs-java](https://github.com/cashapp/quickjs-java)
- [quack](https://github.com/koush/quack)
- [quickjs-android](https://github.com/taoweiji/quickjs-android)                