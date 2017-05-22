package com.subao.common.net;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;

import java.lang.reflect.Method;

/**
 * 监测蜂窝网络信号强度的{@link SignalWatcher}
 * <p>Created by YinHaiBo on 2017/3/13.</p>
 */

public class SignalWatcherForCellular extends SignalWatcher {

    private TelephonyManager telephonyManager;
    private PhoneStateListener phoneStateListener;

    public SignalWatcherForCellular(Listener listener) {
        super(listener);
    }

    @Override
    public void start(Context context) {
        synchronized (this) {
            if (this.telephonyManager == null) {
                this.telephonyManager = (TelephonyManager) context.getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
                if (this.telephonyManager != null) {
                    phoneStateListener = new MyPhoneStateListener(this, telephonyManager);
                    telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
                }
            }
        }
    }

    @Override
    public void shutdown() {
        synchronized (this) {
            if (telephonyManager != null) {
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
                phoneStateListener = null;
                telephonyManager = null;
            }
        }
    }

    static class MyPhoneStateListener extends PhoneStateListener {

        /**
         * 信号强度等级最小值
         */
        final static int MIN_SIGNAL_LEVEL = 0;
        /**
         * 信号强度等级最大值
         */
        final static int MAX_SIGNAL_LEVEL = 4;

        private final TelephonyManager telephonyManager;
        private final SignalWatcher watcher;

        MyPhoneStateListener(SignalWatcher watcher, TelephonyManager telephonyManager) {
            this.telephonyManager = telephonyManager;
            this.watcher = watcher;
        }

        static int calcLevelFromDBM(int dbm) {
            if (dbm >= -70) {
                return MAX_SIGNAL_LEVEL;
            } else if (dbm >= -85) {
                return 3;
            } else if (dbm >= -95) {
                return 2;
            } else if (dbm >= -100) {
                return 1;
            } else {
                return MIN_SIGNAL_LEVEL;
            }
        }

        static int calcLevelFromECIO(int ecio) {
            // Ec/Io are in dB*10
            if (ecio >= -90) {
                return MAX_SIGNAL_LEVEL;
            } else if (ecio >= -110) {
                return 3;
            } else if (ecio >= -130) {
                return 2;
            } else if (ecio >= -150) {
                return 1;
            } else {
                return MIN_SIGNAL_LEVEL;
            }
        }

        static int calcLevelFromSNR(int snr) {
            if (snr >= 7) {
                return MAX_SIGNAL_LEVEL;
            } else if (snr >= 5) {
                return 3;
            } else if (snr >= 3) {
                return 2;
            } else if (snr >= 1) {
                return 1;
            } else {
                return MIN_SIGNAL_LEVEL;
            }
        }

        static int invokeMethod(SignalStrength signalStrength, String name) {
            int result = -1;
            try {
                Method m = signalStrength.getClass().getMethod(name);
                if (m != null) {
                    Object obj = m.invoke(signalStrength);
                    if (obj != null && (obj instanceof Integer)) {
                        result = (Integer) obj;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        }

        static int getSignalStrengthLevel(
            TelephonyManager telephonyManager,
            SignalStrength signalStrength
        ) {
            int level = invokeMethod(signalStrength, "getLevel");
            if (level >= 0) {
                return Math.min(level, MAX_SIGNAL_LEVEL);
            }
            // GSM
            if (signalStrength.isGsm()) {
                int value = signalStrength.getGsmSignalStrength();
                if (value != 99) {
                    return calcLevelFromDBM(-113 + 2 * value);
                }
            }
            // 4G
            if (telephonyManager.getNetworkType() == TelephonyManager.NETWORK_TYPE_LTE) {
                int lteLevel = invokeMethod(signalStrength, "getLteLevel");
                if (lteLevel >= 0) {
                    return Math.min(lteLevel, MAX_SIGNAL_LEVEL);
                }
            }
            //
            int snr = signalStrength.getEvdoSnr();
            if (snr < 0) {
                int levelDbm = calcLevelFromDBM(signalStrength.getCdmaDbm());
                int levelEcio = calcLevelFromECIO(signalStrength.getCdmaEcio());
                return (levelDbm < levelEcio) ? levelDbm : levelEcio;
            }
            return calcLevelFromSNR(snr);
        }

        static int signalLevelToPercent(int level) {
            if (level <= 0) {
                return 0;
            } else if (level >= MAX_SIGNAL_LEVEL) {
                return 100;
            } else {
                return level * 100 / MAX_SIGNAL_LEVEL;
            }
        }

        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            super.onSignalStrengthsChanged(signalStrength);
            int level = getSignalStrengthLevel(telephonyManager, signalStrength);
            this.watcher.notifyListener(signalLevelToPercent(level));
        }

    }
}
