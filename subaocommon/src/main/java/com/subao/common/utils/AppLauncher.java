package com.subao.common.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

/**
 * 负责启动指定APP的类
 */
public class AppLauncher {

    private AppLauncher() {

    }

    /**
     * 启动指定包名的APP
     *
     * @param context     {@link Context}
     * @param packageName 包名
     * @return true表示成功启动，false表示失败
     */
    public static boolean execute(Context context, String packageName) {
        return execute(context, packageName, null);
    }

    static boolean execute(Context context, String packageName, AMExecutor amExecutor) {
        if (packageName == null || packageName.length() == 0) {
            return false;
        }
        try {
            Intent intent = getLaunchIntend(context, packageName);
            if (intent == null) {
                return false;
            }
            if (amExecutor == null) {
                amExecutor = new AMExecutor();
            }
            if (amExecutor.execute(intent, null)) {
                return true;
            }
            context.startActivity(intent);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static Intent getLaunchIntend(Context context, String packageName) {
        PackageManager packageManager = context.getPackageManager();
        if (packageManager == null) {
            return null;
        }
        try {
            return packageManager.getLaunchIntentForPackage(packageName);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static String getComponentName(Intent intent) {
        if (intent == null) {
            return null;
        }
        ComponentName cn = intent.getComponent();
        if (cn != null) {
            return String.format("%s/%s", cn.getPackageName(), cn.getClassName());
        } else {
            return null;
        }
    }

    static class AMExecutor {

        public boolean execute(Intent intent, ProcessBuilderWrapper processBuilder) {
            String componentName = getComponentName(intent);
            if (componentName == null) {
                return false;
            }
            boolean result = false;
            if (processBuilder == null) {
                processBuilder = new ProcessBuilderWrapper();
            }
            processBuilder.command(new String[]{"am", "start", "--user", "0", componentName});
            try {
                processBuilder.redirectErrorStream(true);
                Process process = processBuilder.start();
                //
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.toLowerCase(Locale.US);
                    if (line.contains("starting: intent")) {
                        result = true;
                    } else if (line.contains("error") || line.contains("exception")) {
                        result = false;
                    }
                }
            } catch (IOException e) {
                return false;
            } catch (RuntimeException e) {
                return false;
            }
            return result;
        }
    }

    /**
     * {@link ProcessBuilder}无法被mock，所以设计一个包装类，以便测试
     */
    static class ProcessBuilderWrapper {

        private final ProcessBuilder processBuilder;

        ProcessBuilderWrapper() {
            this.processBuilder = new ProcessBuilder();
        }

        public Process start() throws IOException {
            return this.processBuilder.start();
        }

        public void redirectErrorStream(boolean redirectErrorStream) {
            this.processBuilder.redirectErrorStream(redirectErrorStream);
        }

        public void command(String[] commands) {
            this.processBuilder.command(commands);
        }
    }
}
