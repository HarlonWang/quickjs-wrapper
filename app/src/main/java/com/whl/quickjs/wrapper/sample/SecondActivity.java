package com.whl.quickjs.wrapper.sample;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.whl.quickjs.wrapper.JSArray;
import com.whl.quickjs.wrapper.JSObject;
import com.whl.quickjs.wrapper.QuickJSContext;

public class SecondActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        QuickJSContext context = QuickJSContext.create();
        context.evaluate(Script.ELEMENT);
        context.evaluate(Script.RENDER);

        JSObject globalObj = context.getGlobalObject();
        JSObject renderFunc = (JSObject) globalObj.getProperty("render");
        JSObject result = (JSObject) context.call(renderFunc, globalObj, 1, null);
        Log.d("quickjs-android-wrapper", "Second value = " + result.toString());

        JSArray children = (JSArray) result.getProperty("children");
//        if (children.isArray()) {
//            int length = children.getLength();
//            Log.d("quickjs-android-wrapper", "Second length = " + children.getLength());
//            for (int i = 0; i < length; i++) {
//                JSValue item = (JSValue) children.getByIndex(i);
//                Log.d("quickjs-android-wrapper", "item value = " + item.toString());
//            }
//        }

    }
}
