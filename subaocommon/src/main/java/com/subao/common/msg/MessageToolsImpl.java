package com.subao.common.msg;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.subao.common.data.AppType;
import com.subao.common.data.Config;
import com.subao.common.net.NetTypeDetector;
import com.subao.common.thread.MainHandler;
import com.subao.common.utils.CalendarUtils;
import com.subao.common.utils.ThreadUtils;

public class MessageToolsImpl implements MessageTools {

    private final Context context;
    private final String imsi;

    private final NetTypeDetector netTypeDetector;

    private final MessageBuilder messageBuilder;
    private final Message_App messageApp;

    private final MessagePersistent messagePersistent;

    public MessageToolsImpl(
        Context context,
        String versionName, String channel,
        String imsi,
        NetTypeDetector netTypeDetector,
        MessagePersistent messagePersistent
    ) {
        this.context = context.getApplicationContext();
        this.imsi = imsi;
        this.netTypeDetector = netTypeDetector;
        this.messageBuilder = new MessageBuilder(
            context,
            AppType.ANDROID_SDK,
            Message_VersionInfo.create(versionName, channel));
        this.messageApp = createMessageApp(this.context);
        this.messagePersistent = messagePersistent;
    }

    private static Message_App createMessageApp(Context context) {
        String appLabel = "";
        String packageName = context.getPackageName();
        PackageManager pm = context.getPackageManager();
        if (pm != null) {
            ApplicationInfo appInfo = context.getApplicationInfo();
            if (appInfo != null) {
                CharSequence label = appInfo.loadLabel(pm);
                if (label != null) {
                    appLabel = label.toString();
                }
            }
        }
        return new Message_App(appLabel, packageName);
    }

    @Override
    public void runInMainThread(Runnable r) {
        if (ThreadUtils.isInAndroidUIThread()) {
            r.run();
        } else {
            MainHandler.getInstance().post(r);
        }
    }

    @Override
    public void onMessageStartSent() {
        runInMainThread(new MessageStartAfterSent());
    }

    @Override
    public Context getContext() {
        return this.context;
    }

    @Override
    public AppType getAppType() {
        return AppType.ANDROID_SDK;
    }

    @Override
    public String getIMSI() {
        return this.imsi;
    }

    @Override
    public NetTypeDetector getNetTypeDetector() {
        return this.netTypeDetector;
    }

    @Override
    public Message_Gaming.AccelMode getCurrentAccelMode() {
        // SDK总是返回这个值
        return Message_Gaming.AccelMode.UNKNOWN_ACCEL_MODE;
    }

    @Override
    public MessageBuilder getMessageBuilder() {
        return messageBuilder;
    }

    @Override
    public MessagePersistent getMessagePersistent() {
        return this.messagePersistent;
    }

    private static class MessageStartAfterSent implements Runnable {

        @Override
        public void run() {
            Config.getInstance().setDayReportStartMessage(CalendarUtils.todayCST());
        }

    }

}
