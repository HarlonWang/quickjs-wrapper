# QuickJS For Android/JVM
Android/JVM 下的 QuickJS Binding 库

## 特性
- 支持 Java 和 JavaScript 类型互转
- 支持 Promise 异步执行
- 统一的 JavaScript 异常处理
- 优化 not a function 报错并显示名字
- 支持字节码编译和执行

实验特性（不保证稳定性）：
- 支持模块加载执行（import, export）

## 依赖
[![Maven Central](https://img.shields.io/maven-central/v/wang.harlon.quickjs/wrapper-android.svg?label=Maven%20Central&color=blue)](https://search.maven.org/search?q=g:%22wang.harlon.quickjs%22%20AND%20a:%22wrapper-android%22)

```Groovy
repositories {
  mavenCentral()
}
        
dependencies {
  // For Android
  implementation 'wang.harlon.quickjs:wrapper-android:latest.version'
  // For JVM
  implementation 'wang.harlon.quickjs:wrapper-java:latest.version'
}
```

### 快照 
[![Wrapper](https://img.shields.io/static/v1?label=snapshot&message=wrapper&logo=apache%20maven&color=yellowgreen)](https://s01.oss.sonatype.org/content/repositories/snapshots/wang/harlon/quickjs/wrapper-android/) <br>

<details>
 <summary>如何使用快照</summary>

#### 依赖快照
可以获得 Wrapper 当前开发版本的快照, 查看 [最新快照版本](https://s01.oss.sonatype.org/content/repositories/snapshots/wang/harlon/quickjs/wrapper-android/).

要在项目中导入快照版本，请在 gradle 文件中添加下面的代码片段:
```Gradle
repositories {
   maven { url 'https://s01.oss.sonatype.org/content/repositories/snapshots/' }
}
```
	
接下来，将下面的依赖项添加到你的 **module**'s `build.gradle`:
```gradle
dependencies {
    // For Android
    implementation "wang.harlon.quickjs:wrapper-android:latest-SNAPSHOT"
    // For JVM
    implementation "wang.harlon.quickjs:wrapper-java:latest-SNAPSHOT"
}
```

</details>

## 使用

### 初始化
在 Android 中:
```Java
// 建议放在 Application 中初始化
QuickJSLoader.init();
```

[其他平台请参考这里.](./wrapper-java/README.md)

### 创建 JSContext

```Java
QuickJSContext context = QuickJSContext.create();
```

### 销毁 JSContext

```Java
QuickJSContext context = QuickJSContext.create();
context.destroy();
QuickJSContext.destroyRuntime(context);
```

### 执行 JavaScript

```Java
QuickJSContext context = QuickJSContext.create();
context.evaluate("var a = 1 + 2;");
```

### 支持的类型

#### 以下基础类型，Java 和 JavaScript 可以直接互转使用
- `boolean`
- `int`
- `double`
- `String`
- `null`

#### JS 对象类型的互转
- `JSObject` 代表一个 JavaScript 对象
- `JSFunction` 代表一个 JavaScript 方法
- `JSArray` 代表一个 JavaScript 数组

#### 关于 Long 类型
因为 JavaScript 里没有对应 Java 的 Long 类型，所以，Long 类型的转换比较特殊。
- 从 Java 可以直接传 Long 值到 JavaScript，会转为 Int64 位。
- 从 JavaScript 传值到 Java 转为为 Long 类型，需要借助 Double 类型，示例如下：
    ```Java
    ((Double)target).longValue());
    ```

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
所有的 JavaScript 代码执行都是单线程，基于创建时的线程决定，不支持多线程执行。

## 参考

- [quickjs-java](https://github.com/cashapp/quickjs-java)
- [quack](https://github.com/koush/quack)
- [quickjs-android](https://github.com/taoweiji/quickjs-android)
