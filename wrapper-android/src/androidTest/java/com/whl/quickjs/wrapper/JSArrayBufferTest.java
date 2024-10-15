package com.whl.quickjs.wrapper;

import com.whl.quickjs.android.QuickJSLoader;
import org.junit.Test;

import static org.junit.Assert.*;

public class JSArrayBufferTest {
    @Test
    public void toByteArray() {
        QuickJSLoader.init();
        QuickJSContext context = QuickJSContext.create();
        byte[] bytes = "test测试".getBytes();
        JSObject buffer = context.newArrayBuffer(bytes);
        assertEquals(bytes, context.toByteArray(buffer));
        buffer.release();
        context.destroy();
    }

    @Test
    public void jsArrayBuffer() {
        QuickJSLoader.init();
        QuickJSContext context = QuickJSContext.create();
        byte[] bytes = "test测试".getBytes();
        //new TextEncoder().encode("test测试").buffer
        JSObject buffer = (JSObject) context.evaluate("new Uint8Array([116, 101, 115, 116, 230, 181, 139, 232, 175, 149]).buffer");
        assertEquals(bytes, context.toByteArray(buffer));
        buffer.release();
        context.destroy();
    }
}
