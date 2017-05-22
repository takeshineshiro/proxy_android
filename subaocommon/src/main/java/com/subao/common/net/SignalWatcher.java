package com.subao.common.net;

import android.content.Context;

/**
 * 信号强度监测器
 * <p>Created by YinHaiBo on 2017/3/13.</p>
 */
public abstract class SignalWatcher {

    private final Listener listener;

    protected SignalWatcher(Listener listener) {
        if (listener == null) {
            throw new NullPointerException("Listener can not be null");
        }
        this.listener = listener;
    }

    protected final void notifyListener(int strengthPercent) {
        if (strengthPercent < 0) {
            strengthPercent = 0;
        } else if (strengthPercent > 100) {
            strengthPercent = 100;
        }
        this.listener.onSignalChange(strengthPercent);
    }

    public abstract void start(Context context);

    public abstract void shutdown();

    /**
     * 回调
     */
    public interface Listener {

        /**
         * 当信号强度发生改变的时候被调用
         *
         * @param strengthPercent 0 ~ 100 之间的表示信号强度的百分比，0为最终，100为最强
         */
        void onSignalChange(int strengthPercent);
    }


}
