package com.subao.common.thread;

import android.os.Handler;
import android.os.Looper;

/**
 * MainHandler
 * <p>Created by YinHaiBo on 2017/1/6.</p>
 */

public class MainHandler extends Handler {

    private final static Handler instance = new MainHandler();

    private MainHandler() {
        super(Looper.getMainLooper());
    }

    public final static Handler getInstance() {
        return instance;
    }
}
