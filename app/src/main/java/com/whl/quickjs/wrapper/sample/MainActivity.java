package com.whl.quickjs.wrapper.sample;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import com.whl.quickjs.wrapper.JSValue;
import com.whl.quickjs.wrapper.QuickJSContext;

public class MainActivity extends AppCompatActivity {

    private static final String TEST_SCRIPT = "var a = 1;\n" +
            "\n" +
            "function test(name) {\n" +
            "\treturn \"圣诞节快乐都解放了\" + name;\n" +
            "}";

    QuickJSContext context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = QuickJSContext.create();
        JSValue value = context.evaluate(TEST_SCRIPT);
        JSValue testFunc = context.getGlobalObject().getProperty("test");
        JSValue result = context.call(testFunc, context.getGlobalObject(), 1, null);

        Log.d("quickjs-android-wrapper", "Main value = " + result.toString());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        context.destroyContext();
    }
}