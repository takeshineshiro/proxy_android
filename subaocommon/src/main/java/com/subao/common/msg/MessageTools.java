package com.subao.common.msg;

import android.content.Context;

import com.subao.common.Misc;
import com.subao.common.data.AppType;
import com.subao.common.net.NetTypeDetector;

/**
 * 消息上报模块所需的一个Tools，提供所需的一切功能
 */
public interface MessageTools {

    /**
     * 取Context
     */
    Context getContext();

    /**
     * 取NetTypeDetector
     */
    NetTypeDetector getNetTypeDetector();

    /**
     * 取{@link AppType}
     */
    AppType getAppType();

    /**
     * 在主线程里执行一个Runnable
     */
    void runInMainThread(Runnable r);

    /**
     * 当Start消息上报成功时被调用
     */
    void onMessageStartSent();

    String getIMSI();

    /**
     * 取消息体构建器对象
     *
     * @return {@link MessageBuilder}
     */
    MessageBuilder getMessageBuilder();

    /**
     * 取当前的加速模式。
     *
     * @return {@link com.subao.common.msg.Message_Gaming.AccelMode}
     */
    Message_Gaming.AccelMode getCurrentAccelMode();

    /**
     * 返回一个{@link MessagePersistent}对象
     *
     * @return {@link MessagePersistent}
     */
    MessagePersistent getMessagePersistent();

    class GameInfo {
        public final Message_App messageApp;
        public final AppType appType;

        public GameInfo(Message_App messageApp, AppType appType) {
            this.messageApp = messageApp;
            this.appType = appType;
        }

        public GameInfo(String appLabel, String packageName, AppType appType) {
            this(new Message_App(appLabel, packageName), appType);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (o == null) {
                return false;
            }
            if (!(o instanceof GameInfo)) {
                return false;
            }
            GameInfo other = (GameInfo) o;
            return this.appType == other.appType
                && Misc.isEquals(this.messageApp, other.messageApp);
        }
    }

}
