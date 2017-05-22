package com.subao.common;

/**
 * SetUpAndTearDown
 * <p>Created by YinHaiBo on 2016/12/1.</p>
 */

public class SetUpAndTearDown {
    public static void setUp() {
        Logger.setLoggableChecker(new Logger.LoggableChecker() {
            @Override
            public boolean isLoggable(String tag, int level) {
                return true;
            }
        });
    }

    public static void tearDown() {
        Logger.setLoggableChecker(null);
    }
}
