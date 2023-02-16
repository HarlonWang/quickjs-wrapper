package com.whl.quickjs.wrapper.sample;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import com.whl.quickjs.android.QuickJSLoader;
import com.whl.quickjs.wrapper.QuickJSContext;

public class MainActivity extends AppCompatActivity {

    QuickJSContext jsContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        QuickJSLoader.init();

        jsContext = QuickJSContext.create();
        jsContext.evaluate("var text = 'Hello QuickJS';");
        String text = jsContext.getGlobalObject().getString("text");
        TextView textView = findViewById(R.id.text);
        textView.setText(text);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        jsContext.destroy();
    }
}