package com.whl.quickjs.wrapper.sample;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

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

        findViewById(R.id.text).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "当时发生的", Toast.LENGTH_SHORT).show();
            }
        });

        Log.d("quickjs-android-wrapper", "Main value = " + result.toString());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        context.destroyContext();
    }
}