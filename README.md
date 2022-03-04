# QuickJS For Android
QuickJS wrapper for Android.

## Download

```Groovy
repositories {
  mavenCentral()
}
        
dependencies {
  implementation 'wang.harlon.quickjs:wrapper:0.0.18'
}
```

## Usage

### Create QuickJSContext

```Java
QuickJSContext context = QuickJSContext.create();
```

### Evaluating JavaScript

```Java
QuickJSContext context = QuickJSContext.create();
context.evaluate("var a = 1 + 2;");
```

### Supported Java Types
Currently, the following Java types are supported with JavaScript:
- `boolean`
- `int` when calling JavaScript from Java.
- `double`
- `String`
- `void` - as a return value.
- `JSObject` represents a JavaScript object
- `JSFunction` represents a JavaScript function
- `JSArray` represents a JavaScript Array
                
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

## Concurrency
JavaScript runtimes are single threaded. All execution in the JavaScript runtime is gauranteed thread safe, by way of Java synchronization.

## Reference

- [quickjs-java](https://github.com/cashapp/quickjs-java)
- [quack](https://github.com/koush/quack)
- [quickjs-android](https://github.com/taoweiji/quickjs-android)                