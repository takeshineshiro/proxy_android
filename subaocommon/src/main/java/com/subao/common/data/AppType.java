package com.subao.common.data;

import com.subao.common.msg.MessageEnum;

/** APP类型 */
public enum AppType implements MessageEnum {
    UNKNOWN_APPTYPE(0),
    ANDROID_APP(1),
    ANDROID_SDK_EMBEDED(2),
    ANDROID_SDK(3),
    IOS_APP(4),
    IOS_SDK_EMBEDED(5),
    IOS_SDK(6),
    WIN_APP(7),
    WIN_SDK_EMBEDED(8),
    WIN_SDK(9),
    WEB_SDK(10);

    AppType(int id) {
        this.id = id;
    }

    private final int id;

    @Override
    public int getId() {
        return this.id;
    }
}
