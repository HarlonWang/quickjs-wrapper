package com.whl.quickjs.wrapper.sample;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.whl.quickjs.wrapper.QuickJSContext;

public class MainActivity extends AppCompatActivity {

    QuickJSContext context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = QuickJSContext.create();
        context.evaluate("var a = 1;");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        context.destroyContext();
    }
}