package com.subao.common;

import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class WeakReferenceHandlerTest extends RoboBase {

    private static class Target extends WeakReferenceHandler<String> {

        public Message msg;

        public Target(String ref) {
            super(ref);
        }

        public Target(String ref, Looper looper) {
            super(ref, looper);
        }

        @Override
        protected void handleMessage(String ref, Message msg) {
            this.msg = new Message();
            this.msg.what = msg.what;
            this.msg.obj = msg.obj;
        }
    }

    @Test
    public void clearRef() {
        String s = "Hello";
        Target t = new Target(s);
        assertEquals(s, t.getRef());
        t.clearRef();
        assertNull(t.getRef());
    }

    @Test
    public void handleMessage() {
        String ref = "hello";
        Target t = new Target(ref);
        t.sendMessage(t.obtainMessage(1234, "world"));
        assertEquals(1234, t.msg.what);
        assertEquals("world", t.msg.obj);
    }

    @Test
    public void thread() {
        HandlerThread ht = new HandlerThread("test handler thread");
        ht.start();
        Target t = new Target("Hello", ht.getLooper());
        t.sendEmptyMessage(1);
        ht.quit();
    }


}