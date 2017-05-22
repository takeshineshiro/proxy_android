package com.subao.common.accel;

import android.content.Context;
import android.os.Handler;

import com.subao.common.ErrorCode;
import com.subao.common.RoboBase;
import com.subao.common.collection.Ref;
import com.subao.common.jni.JniWrapper;
import com.subao.common.net.SignalWatcher;
import com.subao.common.net.SignalWatcherForCellular;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * SignalStrengthDetectorTest
 * <p>Created by YinHaiBo on 2017/3/17.</p>
 */
public class SignalStrengthDetectorTest extends RoboBase {

    static int signalStrenthPercentOfWatcher = -1;

    @Test
    public void constructor() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        testPrivateConstructor(EngineWrapper.SignalStrengthDetector.class);
    }

    @Test
    @Config(shadows = ShadowSignalWatcher.class)
    public void test() {
        doTest(getContext(), -1);
        doTest(getContext(), 25);
        doTest(getContext(), 0);
        doTest(getContext(), 100);
    }

    private static void doTest(Context context, int signalStrenthPercent) {
        signalStrenthPercentOfWatcher = signalStrenthPercent;
        int cid = 123;
        //
        final Ref<Integer> cidRef = new Ref<Integer>();
        final Ref<Integer> fdRef = new Ref<Integer>();
        final Ref<Integer> errorRef = new Ref<Integer>();
        final Ref<Boolean> canRetryRef = new Ref<Boolean>();
        //
        JniWrapper jniWrapper = mock(JniWrapper.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                cidRef.set((Integer) invocation.getArgument(0));
                fdRef.set((Integer) invocation.getArgument(1));
                errorRef.set((Integer) invocation.getArgument(2));
                canRetryRef.set((Boolean) invocation.getArgument(3));
                return null;
            }
        }).when(jniWrapper).requestMobileFDResult(anyInt(), anyInt(), anyInt(), anyBoolean());
        //
        Handler handler = mock(Handler.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Runnable r = invocation.getArgument(0);
                r.run();
                return null;
            }
        }).when(handler).postDelayed(any(Runnable.class), anyLong());
        int rawErrorCode = ErrorCode.WIFI_ACCEL_NO_AVAILABLE_CELLULAR_NETWORK_SWITCH_ON;
        EngineWrapper.SignalStrengthDetector.execute(context, handler, rawErrorCode, jniWrapper, cid);
        assertEquals(cid, (int)cidRef.get());
        assertEquals(-1, (int)fdRef.get());
        if (signalStrenthPercent < 0) {
            assertEquals(rawErrorCode, (int) errorRef.get());
        } else {
            assertEquals(ErrorCode.WIFI_ACCEL_NO_AVAILABLE_CELLULAR_SIGNAL_STRENGTH + signalStrenthPercent, (int) errorRef.get());
        }
        assertTrue(canRetryRef.get());
    }

    @Implements(value = SignalWatcherForCellular.class)
    public static class ShadowSignalWatcher {

        private SignalWatcher.Listener listener;

        @Implementation
        public void __constructor__(SignalWatcher.Listener listener) {
            this.listener = listener;
        }

        @Implementation
        public void start(Context context) {
            if (signalStrenthPercentOfWatcher >= 0) {
                this.listener.onSignalChange(signalStrenthPercentOfWatcher);
            }
        }
    }

}