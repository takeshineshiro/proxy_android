package com.subao.common.data;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import com.subao.common.Misc;

import java.util.ArrayList;
import java.util.List;

/**
 * 本机已安装应用列表
 * <p>Created by YinHaiBo on 2017/3/28.</p>
 */
public class InstalledApp {

    private static final String PERMISSION_KEYWORD = "com.subao.permission.USE_SDK";
    private static final int PERMISSION_KEYWORD_LENGTH = PERMISSION_KEYWORD.length();

    private InstalledApp() {
    }

    /**
     * 返回除系统应用和本程序自身以外的，已安装的应用列表
     *
     * @return 成功返回一个{@link Info}的列表，失败返回null
     */
    public static List<Info> getInstalledAppList(Context context) {
        String myPackageName = context.getPackageName();
        PackageManager packageManager = context.getPackageManager();
        if (packageManager != null) {
            List<ApplicationInfo> listApplications = packageManager.getInstalledApplications(0); // FIXME: MATCH_UNINSTALLED ??
            if (listApplications != null && !listApplications.isEmpty()) {
                List<Info> result = new ArrayList<Info>(listApplications.size());
                for (ApplicationInfo ai : listApplications) {
                    if (Misc.isApplicationsUID(ai.uid)
                        && (ai.flags & ApplicationInfo.FLAG_SYSTEM) == 0
                        && !Misc.isEquals(myPackageName, ai.packageName)
                        ) {
                        String label = ai.loadLabel(packageManager).toString();
                        Info info = new Info(ai, label, isSDKEmbed(packageManager, ai.packageName));
                        result.add(info);
                    }
                }
                return result;
            }
        }
        return null;
    }

    /**
     * 给定应用包名，判断该应用是否内嵌了迅游手游加速SDK
     */
    public static boolean isSDKEmbed(PackageManager pm, String packageName) {
        PackageInfo pi;
        try {
            pi = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        if (pi == null) {
            return false;
        }
        String[] permissions = pi.requestedPermissions;
        if (permissions == null) {
            return false;
        }
        for (int i = 0, len = permissions.length; i < len; ++i) {
            String s = permissions[i];
            if (s != null && s.length() >= PERMISSION_KEYWORD_LENGTH && s.startsWith(PERMISSION_KEYWORD)) {
                return true;
            }
        }
        return false;
    }

    public static class Info {
        private final ApplicationInfo app;
        private final String label;
        private final boolean isSDKEmbed;

        public Info(ApplicationInfo app, String label, boolean isSDKEmbed) {
            this.app = app;
            this.label = label;
            this.isSDKEmbed = isSDKEmbed;
        }

        public String getPackageName() {
            return this.app.packageName;
        }

        public int getUid() {
            return this.app.uid;
        }

        public String getAppLabel() {
            return this.label;
        }

        public Drawable loadIcon(Context context) {
            PackageManager pm = context.getPackageManager();
            if (pm != null) {
                try {
                    return this.app.loadIcon(pm);
                } catch (RuntimeException e) {
                }
            }
            return null;
        }

        public boolean isSDKEmbed() {
            return this.isSDKEmbed;
        }
    }

}
