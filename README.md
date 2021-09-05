# QuickJS Android Wrapper
Simple to use JavaScript for Android with QuickJS engine.

## Download

        repositories {
          mavenCentral()
        }
        
        dependencies {
          implementation 'wang.harlon.quickjs:wrapper:0.0.2'
        }

## Usage

### Create QuickJSContext

        QuickJSContext context = QuickJSContext.create();

### Evaluating JavaScript

        QuickJSContext context = QuickJSContext.create();
        context.evaluate("var a = 123;");

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
                
### Example

ReturnType

        assertEquals(true, context.evaluate("true;"));
        assertEquals(false, context.evaluate("false;"));
        assertEquals(123, context.evaluate("123;"));
        assertEquals(1.23, context.evaluate("1.23;"));
        assertEquals("hello wrapper", context.evaluate("\"hello wrapper\";"));
            
GetProperties

        context.evaluate("var intValue = 1;\n" +
                "var doubleValue = 1.23;\n" +
                "var stringValue = \"hi Jack\";\n" +
                "var booleanValue = true;\n" +
                "\n" +
                "function testFunc(name) {\n" +
                "\treturn \"hello, \" + name;\n" +
                "}");
        JSObject globalObject = context.getGlobalObject();
        assertEquals(1, globalObject.getProperty("intValue"));
        assertEquals(1.23, globalObject.getProperty("doubleValue"));
        assertEquals("hi Jack", globalObject.getProperty("stringValue"));
        assertEquals(true, globalObject.getProperty("booleanValue"));
        JSFunction function = (JSFunction) globalObject.getProperty("testFunc");
        assertEquals("hello, yonglan-whl", context.call(function, globalObject, "yonglan-whl"));
                

SetProperties

        globalObj.setProperty("stringValue", "hello test");
        globalObj.setProperty("intValue", 123);
        globalObj.setProperty("doubleValue", 123.11);
        globalObj.setProperty("booleanValue", true);
        globalObj.setProperty("functionValue", new JSCallFunction() {
            @Override
            public Object call(Object... args) {
                System.out.println("arg = " + args.length);
                return "call back";
            }
        });

JSArray

        JSArray ret = (JSArray) context.evaluate("function test(value) {\n" +
                "\treturn [1, 2, value];\n" +
                "}\n" +
                "\n" +
                "test(3);");
        assertEquals(3, ret.get(2));

JSFunction
        
        context.evaluate("function test(intValue, stringValue, doubleValue, booleanValue) {\n" +
                "\treturn \"hello, \" + intValue + stringValue + doubleValue + booleanValue;\n" +
                "}");
        JSObject globalObject = context.getGlobalObject();
        JSFunction func = (JSFunction) globalObject.getProperty("test");
        assertEquals("hello, 1string123.11true", context.call(func, globalObject, 1, "string", 123.11, true));

more usage case, can look [QuickJSTest.java](https://github.com/HarlonWang/quickjs-android-wrapper/blob/main/quickjs-android-wrapper/src/androidTest/java/com/whl/quickjs/wrapper/QuickJSTest.java)

## Thanks

- [quickjs-java](https://github.com/cashapp/quickjs-java)
- [quack](https://github.com/koush/quack)
                
   