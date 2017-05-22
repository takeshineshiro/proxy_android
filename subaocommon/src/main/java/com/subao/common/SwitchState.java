package com.subao.common;

/**
 * 开关状态定义
 * <p>Created by YinHaiBo on 2017/3/7.</p>
 */
public enum SwitchState {

    /**
     * 状态未知
     */
    UNKNOWN(-1),

    /**
     * 关闭
     */
    OFF(0),

    /**
     * 开启
     */
    ON(1);


    private final int id;

    SwitchState(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }
}
