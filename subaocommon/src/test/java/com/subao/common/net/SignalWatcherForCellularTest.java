package com.subao.common.net;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;

import com.subao.common.RoboBase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * SignalWatcherForCellularTest
 * <p>Created by YinHaiBo on 2017/3/13.</p>
 */
public class SignalWatcherForCellularTest extends RoboBase {

    private SignalWatcherForCellular signalWatcher;
    private Listener listener;

    @Before
    public void setUp() {
        listener = new Listener();
        signalWatcher = new SignalWatcherForCellular(listener);
        signalWatcher.start(getContext());
    }

    @After
    public void tearDown() {
        signalWatcher.shutdown();
    }

    @Test(expected = RuntimeException.class)
    public void testConstructorNullArgument() {
        new SignalWatcherForCellular(null);
    }

    @Test
    public void testNotifyListener() {
        signalWatcher.notifyListener(55);
        assertEquals(55, listener.strengthPercent);
        signalWatcher.notifyListener(-1);
        assertEquals(0, listener.strengthPercent);
        signalWatcher.notifyListener(101);
        assertEquals(100, listener.strengthPercent);
    }

    @Test
    public void testUtils() {
        assertEquals(0, SignalWatcherForCellular.MyPhoneStateListener.MIN_SIGNAL_LEVEL);
        assertEquals(4, SignalWatcherForCellular.MyPhoneStateListener.MAX_SIGNAL_LEVEL);
        int[] values = new int[]{
            -69, 4,
            -70, 4,
            -84, 3,
            -85, 3,
            -94, 2,
            -95, 2,
            -99, 1,
            -100, 1,
            -101, 0,
        };
        for (int i = 0; i < values.length; i += 2) {
            int dbm = values[i];
            int expected = values[i + 1];
            assertEquals(expected, SignalWatcherForCellular.MyPhoneStateListener.calcLevelFromDBM(dbm));
        }
        //
        int[] values2 = new int[]{
            -89, 4,
            -90, 4,
            -109, 3,
            -110, 3,
            -129, 2,
            -130, 2,
            -131, 1,
            -150, 1,
            -151, 0,
        };
        for (int i = 0; i < values2.length - 1; i += 2) {
            int ecio = values2[i];
            int expected = values[i + 1];
            assertEquals(expected, SignalWatcherForCellular.MyPhoneStateListener.calcLevelFromECIO(ecio));
        }
        //
        int[] values3 = new int[]{
            8, 4,
            7, 4,
            6, 3,
            5, 3,
            4, 2,
            3, 2,
            2, 1,
            1, 1,
            0, 0,
        };
        for (int i = 0; i < values3.length - 1; i += 2) {
            int snr = values3[i];
            int expected = values[i + 1];
            assertEquals(expected, SignalWatcherForCellular.MyPhoneStateListener.calcLevelFromSNR(snr));
        }
        //
        assertEquals(0, SignalWatcherForCellular.MyPhoneStateListener.signalLevelToPercent(-1));
        assertEquals(0, SignalWatcherForCellular.MyPhoneStateListener.signalLevelToPercent(0));
        assertEquals(25, SignalWatcherForCellular.MyPhoneStateListener.signalLevelToPercent(1));
        assertEquals(50, SignalWatcherForCellular.MyPhoneStateListener.signalLevelToPercent(2));
        assertEquals(75, SignalWatcherForCellular.MyPhoneStateListener.signalLevelToPercent(3));
        assertEquals(100, SignalWatcherForCellular.MyPhoneStateListener.signalLevelToPercent(4));
        assertEquals(100, SignalWatcherForCellular.MyPhoneStateListener.signalLevelToPercent(5));
    }

    @Test
    public void testSignalStrength() {
        testSignalStrength(true, true, 0);
        testSignalStrength(false, true, 0);
        testSignalStrength(false, false, -1);
        testSignalStrength(false, false, 7);
    }

    private void testSignalStrength(final boolean isGSM, final boolean isLte, final int snr) {
        signalWatcher.shutdown();
        final TelephonyManager telephonyManager = mock(TelephonyManager.class);
        doAnswer(new Answer() {
            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                PhoneStateListener listener = invocation.getArgument(0);
                SignalStrength ss = mock(SignalStrength.class);
                listener.onSignalStrengthsChanged(ss);
                //
                ss = mock(SignalStrength.class);
                doReturn(-1).when(ss).getLevel();
                if (isGSM) {
                    doReturn(true).when(ss).isGsm();
                }
                if (isLte) {
                    doReturn(TelephonyManager.NETWORK_TYPE_LTE).when(telephonyManager).getNetworkType();
                }
                doReturn(snr).when(ss).getEvdoSnr();
                listener.onSignalStrengthsChanged(ss);
                //

                return null;
            }
        }).when(telephonyManager).listen(any(PhoneStateListener.class), anyInt());
        Context context = mock(Context.class);
        doReturn(telephonyManager).when(context).getSystemService(Context.TELEPHONY_SERVICE);
        doReturn(context).when(context).getApplicationContext();
        signalWatcher.start(context);
    }

    @Test
    public void testInvokeMethod() {
        SignalStrength ss = mock(SignalStrength.class);
        assertTrue(SignalWatcherForCellular.MyPhoneStateListener.invokeMethod(ss, "not_exists") < 0);
    }

    private SignalStrength createSignalStrength(
        int gsmSignalStrength, int gsmBitErrorRate,
        int cdmaDbm, int cdmaEcio,
        int evdoDbm, int evdoEcio, int evdoSnr,
        int lteSignalStrength, int lteRsrp, int lteRsrq, int lteRssnr, int lteCqi,
        boolean gsm
    ) {
        try {
            Constructor<SignalStrength> constructor = SignalStrength.class.getConstructor(
                int.class, int.class,
                int.class, int.class,
                int.class, int.class, int.class,
                int.class, int.class, int.class, int.class, int.class,
                boolean.class);
            constructor.setAccessible(true);
            return constructor.newInstance(
                gsmSignalStrength, gsmBitErrorRate,
                cdmaDbm, cdmaEcio,
                evdoDbm, evdoEcio, evdoSnr,
                lteSignalStrength, lteRsrp, lteRsrq, lteRssnr, lteCqi,
                gsm);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static class Listener implements SignalWatcher.Listener {

        int strengthPercent;

        @Override
        public void onSignalChange(int strengthPercent) {
            this.strengthPercent = strengthPercent;
        }
    }
}