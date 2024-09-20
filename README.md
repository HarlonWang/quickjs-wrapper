# QuickJS For Android/JVM
QuickJS wrapper for Android/JVM.

## Feature
- Java types are supported with JavaScript
- Support promise execute
- JavaScript exception handler
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

## Building the Project
This repository use git submodules and so when you are checking out the app, you'll need to ensure the submodules are initialized properly. You can use the `--recursive` flag when cloning the project to do this.
```git
git clone --recursive https://github.com/HarlonWang/quickjs-wrapper.git
```

Alternatively, if you already have the project checked out, you can initialize the submodules manually.
```git
git submodule update --init
```

## Usage

### Initialization
In Android Platforms:
```Java
// You usually need to initialize it before using it..
QuickJSLoader.init();
```

[Refer to here for other platforms.](./wrapper-java/README.md)

### Create QuickJSContext

```Java
QuickJSContext context = QuickJSContext.create();
```

### Destroy QuickJSContext

```Java
context.destroy();
```

### Evaluating JavaScript

```Java
context.evaluate("var a = 1 + 2;");
```

### Console Support
```Java
context.setConsole(your console implementation.);
```

### Supported Types

#### Java and JavaScript can directly convert to each other for the following basic types
- `boolean`
- `int`
- `long`
- `double`
- `String`
- `null`

#### Mutual conversion of JS object types
- `JSObject` represents a JavaScript object
- `JSFunction` represents a JavaScript function
- `JSArray` represents a JavaScript Array

#### About Long type
There is no Long type in JavaScript, the conversion of Long type is special.

- Java --> JavaScript
    - The Long value <= Number.MAX_SAFE_INTEGER, will be convert to Number type.
    - The Long value > Number.MAX_SAFE_INTEGER, will be convert to BigInt type.
    - Number.MIN_SAFE_INTEGER is the same to above.

- JavaScript --> Java
    - Number(Int64) or BigInt --> Long type

### Set Property
Java

```java
QuickJSContext context = QuickJSContext.create();
JSObject globalObj = context.getGlobalObject();
JSObject repository = context.createNewJSObject();
obj1.setProperty("name", "QuickJS Wrapper");
obj1.setProperty("created", 2022);
obj1.setProperty("version", 1.1);
obj1.setProperty("signing_enabled", true);
obj1.setProperty("getUrl", (JSCallFunction) args -> {
    return "https://github.com/HarlonWang/quickjs-wrapper";
});
globalObj.setProperty("repository", repository);
repository.release();
```

JavaScript

```javascript
repository.name; // QuickJS Wrapper
repository.created; // 2022
repository.version; // 1.1
repository.signing_enabled; // true
repository.getUrl(); // https://github.com/HarlonWang/quickjs-wrapper
```                

### Get Property
JavaScript

```JavaScript
var repository = {
	name: 'QuickJS Wrapper',
	created: 2022,
	version: 1.1,
	signing_enabled: true,
	getUrl: (name) => { return 'https://github.com/HarlonWang/quickjs-wrapper'; }
}
```
Java

```Java
QuickJSContext context = QuickJSContext.create();
JSObject globalObject = context.getGlobalObject();
JSObject repository = globalObject.getJSObject("repository");
repository.getString("name"); // QuickJS Wrapper
repository.getInteger("created"); // 2022
repository.getDouble("version"); // 1.1
repository.getBoolean("signing_enabled"); // true
JSFunction fn = repository.getJSFunction("getUrl");
String url = fn.call(); // https://github.com/HarlonWang/quickjs-wrapper
fn.release();
repository.release();
```

### Create JSObject in Java
```Java
QuickJSContext context = QuickJSContext.create();
JSObject obj = context.createNewJSObject();
// Need to release, otherwise it will cause a memory leak.
obj.release();
```

### Create JSArray in Java
```Java
QuickJSContext context = QuickJSContext.create();
JSArray array = context.createNewJSArray();
array.release();
```

### How to return Function to JavaScript in Java
```Java
QuickJSContext context = createContext();
context.getGlobalObject().setProperty("test", args -> (JSCallFunction) args1 -> "123");
context.evaluate("console.log(test()());");
```

Also, you can view it in `QuickJSTest.testReturnJSCallback` code


### Compile ByteCode

```Java
byte[] code = context.compile("'hello, world!'.toUpperCase();");
context.execute(code);
```

### ESModule
Java
```Java
// 1. string code mode
context.setModuleLoader(new QuickJSContext.DefaultModuleLoader() {
    @Override
    public String getModuleStringCode(String moduleName) {
       if (moduleName.equals("a.js")) {
           return "export var name = 'Jack';\n" +
                   "export var age = 18;";
       }
       return null;
    }
});

// 2. bytecode mode
context.setModuleLoader(new QuickJSContext.BytecodeModuleLoader() {
    @Override
    public byte[] getModuleBytecode(String moduleName) {
        return context.compileModule("export var name = 'Jack';export var age = 18;", moduleName);
    }
});
```
JavaScript
```JavaScript
import {name, age} from './a.js';

console.log('name：' + name); // Jack
console.log('age：' + age); // 18
```

### Object release
We typically recommend releasing reference relationships actively after using Java objects to avoid memory leaks. Additionally, the engine will release unreleased objects when destroy, but this timing may be a bit later.
```java
JSFunction func = xxx.getJSFunction("test");
func.call();
func.release();

JSObject obj = xxx.getJSObject("test");
int a = obj.getString("123");
obj.release();

// If the JSFunction does not need to handle the return value, it is recommended to call the following method.
jsFunction.callVoid(xxx);
```

It's important to note that if the result is being returned for use in JavaScript, there is no need to release it.
```java
context.getGlobalObject().setProperty("test", new JSCallFunction() {
  @Override
  public Object call(Object... args) {
    JSObject ret = context.createNewJSObject();
    // There is no need to call the release method here.
    // ret.release();
    return ret;
  }
});
```

## R8 / ProGuard
If you are using R8 the shrinking and obfuscation rules are included automatically.

ProGuard users must manually add the options from [consumer-rules.pro](/wrapper-android/consumer-rules.pro).

## Concurrency
JavaScript runtimes are single threaded. All execution in the JavaScript runtime is guaranteed thread safe, by way of Java synchronization.

## Find this repository useful?
Support it by joining __[stargazers](https://github.com/HarlonWang/quickjs-wrapper/stargazers)__ for this repository. <br>
Also, __[follow me](https://github.com/HarlonWang)__ on GitHub for my next creations! 

## Stargazers over time

[![Stargazers over time](https://starchart.cc/HarlonWang/quickjs-wrapper.svg)](https://starchart.cc/HarlonWang/quickjs-wrapper)

## Reference

- [quickjs-java](https://github.com/cashapp/quickjs-java)
- [quack](https://github.com/koush/quack)
- [quickjs-android](https://github.com/taoweiji/quickjs-android)
