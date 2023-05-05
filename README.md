# QuickJS For Android/JVM
QuickJS wrapper for Android/JVM.

## Feature
- Java types are supported with JavaScript
- Support promise execute
- JavaScript exception handler
- Optimize not a function with its name when type error
- Compile bytecode

Experimental Features Stability not guaranteed.
- ESModule (import, export)

## Download

[![Maven Central](https://img.shields.io/maven-central/v/wang.harlon.quickjs/wrapper-android.svg?label=Maven%20Central&color=blue)](https://search.maven.org/search?q=g:%22wang.harlon.quickjs%22%20AND%20a:%22wrapper-android%22)

```Groovy
repositories {
  mavenCentral()
}

dependencies {
  // Pick one:

  // 1. Android - Use wrapper in your public API:
  api 'wang.harlon.quickjs:wrapper-android:latest.version'

  // 2. JVM - Use wrapper in your implementation only:
  implementation 'wang.harlon.quickjs:wrapper-java:latest.version'
}
```

### SNAPSHOT 
[![Wrapper](https://img.shields.io/static/v1?label=snapshot&message=wrapper&logo=apache%20maven&color=yellowgreen)](https://s01.oss.sonatype.org/content/repositories/snapshots/wang/harlon/quickjs/wrapper-android/) <br>

<details>
 <summary>See how to import the snapshot</summary>

#### Including the SNAPSHOT
Snapshots of the current development version of Wrapper are available, which track [the latest versions](https://s01.oss.sonatype.org/content/repositories/snapshots/wang/harlon/quickjs/wrapper-android/).

To import snapshot versions on your project, add the code snippet below on your gradle file:
```Gradle
repositories {
   maven { url 'https://s01.oss.sonatype.org/content/repositories/snapshots/' }
}
```

Next, add the dependency below to your **module**'s `build.gradle` file:
```gradle
dependencies {
    // For Android
    implementation "wang.harlon.quickjs:wrapper-android:latest-SNAPSHOT"
    // For JVM
    implementation "wang.harlon.quickjs:wrapper-java:latest-SNAPSHOT"
}
```

</details>

## Usage

### Initialization
In Android Platforms:
```Java
// It is usually init in the application.
QuickJSLoader.init();
```

[Refer to here for other platforms.](./wrapper-java/README.md)

### Create QuickJSContext

```Java
QuickJSContext context = QuickJSContext.create();
```

### Destroy QuickJSContext

```Java
QuickJSContext context = QuickJSContext.create();
context.destroy();
```

### Evaluating JavaScript

```Java
QuickJSContext context = QuickJSContext.create();
context.evaluate("var a = 1 + 2;");
```

### Console Support
```Java
QuickJSContext context = QuickJSContext.create();
QuickJSLoader.initConsoleLog(context);
// or custom console.
// QuickJSLoader.initConsoleLog(context, your console implementation.);
```

### Supported Types

#### Java and JavaScript can directly convert to each other for the following basic types
- `boolean`
- `int`
- `double`
- `String`
- `null`

#### Mutual conversion of JS object types
- `JSObject` represents a JavaScript object
- `JSFunction` represents a JavaScript function
- `JSArray` represents a JavaScript Array

#### About Long Type
Because there is no Long type corresponding to Java in JavaScript, the conversion of Long type is special.
- Long value can be directly transferred from Java to JavaScript, which will be converted to Int64 bit。
- To convert a value from JavaScript to Java into a Long type, you need to use the Double type, as shown in the following example:
    ```Java
    ((Double)target).longValue());
    ```

### Set Property
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

### Get Property
JavaScript

```JavaScript
var obj1 = {
	stringProperty: 'hello string',
	intProperty: 1,
	doubleProperty: 0.1,
	booleanProperty: true,
	functionProperty: (name) => { return name + 'Wang'; }
}
```
Java

```Java
JSObject globalObject = context.getGlobalObject();
JSObject obj1 = globalObject.getJSObjectProperty("obj1");
obj1.getString("stringProperty"); // hello
obj1.getInteger("intProperty"); // 1
obj1.getDouble("doubleProperty"); // 0.1
obj1.getBoolean("booleanProperty"); // true
obj1.getJSFunction("functionProperty").call("Harlon"); // HarlonWang
```

### Compile ByteCode

```Java
byte[] code = context.compile("'hello, world!'.toUpperCase();");
context.execute(code);
```

### ESModule
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

## R8 / ProGuard
If you are using R8 the shrinking and obfuscation rules are included automatically.

ProGuard users must manually add the options from [consumer-rules.pro](/wrapper-android/consumer-rules.pro).

## Concurrency
JavaScript runtimes are single threaded. All execution in the JavaScript runtime is guaranteed thread safe, by way of Java synchronization.

## Who's using quickjs-wrapper?
If your product uses quickjs-wrapper, welcome to contribute by creating a issue or let me know through other contacts! 

## Find this repository useful?
Support it by joining __[stargazers](https://github.com/HarlonWang/quickjs-wrapper/stargazers)__ for this repository. <br>
Also, __[follow me](https://github.com/HarlonWang)__ on GitHub for my next creations! 

## Stargazers over time

[![Stargazers over time](https://starchart.cc/HarlonWang/quickjs-wrapper.svg)](https://starchart.cc/HarlonWang/quickjs-wrapper)

## Reference

- [quickjs-java](https://github.com/cashapp/quickjs-java)
- [quack](https://github.com/koush/quack)
- [quickjs-android](https://github.com/taoweiji/quickjs-android)                
