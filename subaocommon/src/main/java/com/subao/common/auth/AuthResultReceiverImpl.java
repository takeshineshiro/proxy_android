package com.subao.common.auth;

import android.text.TextUtils;
import android.util.Log;

import com.subao.common.LogTag;
import com.subao.common.Logger;
import com.subao.common.data.CustomerScriptDownloader;
import com.subao.common.data.HRDataTrans;
import com.subao.common.data.ServiceLocation;
import com.subao.common.data.SubaoIdManager;
import com.subao.common.jni.JniWrapper;
import com.subao.common.msg.MessageUserId;
import com.subao.common.net.IPInfoQuery;
import com.subao.common.thread.ThreadPool;
import com.subao.common.utils.StringUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * {@link AuthResultReceiver} 的默认实现
 * <p>Created by YinHaiBo on 2017/1/6.</p>
 */

public class AuthResultReceiverImpl implements AuthResultReceiver {

    private static final String TAG = LogTag.DATA;

    private final JniWrapper jniWrapper;
    private final HRDataTrans.Arguments arguments;
    private final ServiceLocation serviceLocationOfIPInfoQuery;

    public AuthResultReceiverImpl(JniWrapper jniWrapper, HRDataTrans.Arguments arguments, ServiceLocation serviceLocationOfIPInfoQuery) {
        this.jniWrapper = jniWrapper;
        this.arguments = arguments;
        this.serviceLocationOfIPInfoQuery = serviceLocationOfIPInfoQuery;
    }

    private static boolean isUserStatusVIP(int userStatus) {
        return userStatus == 2 || userStatus == 4 || userStatus == 6;
    }

    private void notifyIPInfoQueryWhenUserStatusChanged(int status) {
        IPInfoQuery.onUserAuthComplete(isUserStatusVIP(status), serviceLocationOfIPInfoQuery);
    }

    @Override
    public void onGetJWTTokenResult(int cid, String jwtToken, long expires, String shortId, int userStatus, String expiredTime, boolean result, int code) {
        notifyIPInfoQueryWhenUserStatusChanged(userStatus);
        jniWrapper.userAuthResult(cid, result, code, jwtToken, (int) expires, shortId, userStatus, expiredTime);
    }

    @Override
    public void onGetUserAccelStatusResult(int cid, String shortId, int status, String expiredTime, boolean result, int code) {
        notifyIPInfoQueryWhenUserStatusChanged(status);
        jniWrapper.userStateResult(cid, result, code, status, shortId, expiredTime);
    }

    @Override
    public void onGetTokenResult(int cid, String ip, byte[] token, int length, int expires, boolean result, int code) {
        jniWrapper.linkAuthResult(cid, result, code, ip, token, expires);
    }

    @Override
    public void onGetUserConfigResult(int cid, String jwtToken, String userId, AuthExecutor.Configs configs, int code, boolean result) {
        jniWrapper.userConfigResult(cid, result, code, configs == null ? null : configs.userConfig);
        if (result && configs != null && !TextUtils.isEmpty(configs.scriptId)) {
            // 有动态脚本可下载
            if (Logger.isLoggableDebug(TAG)) {
                Log.d(TAG, "Has customer script need download, script-id: " + configs.scriptId);
            }
            ScriptDownloader.start(this.arguments, jniWrapper, jwtToken, userId);
        }
    }

    static class ScriptDownloader extends CustomerScriptDownloader {


        /**
         * 一个静态的标志，当HR下载的脚本已经注入过的时候为true，防止重复注入
         */
        private static boolean scriptInjected;

        private final JniWrapper jniWrapper;

        private ScriptDownloader(HRDataTrans.Arguments arguments, JniWrapper jniWrapper, String jwtToken, String userId) {
            super(arguments,
                new UserInfo(userId, jwtToken),
                new UserInfoEx(MessageUserId.getCurrentServiceId(), SubaoIdManager.getInstance().getSubaoId())
            );
            this.jniWrapper = jniWrapper;
        }

        public static boolean start(HRDataTrans.Arguments arguments, JniWrapper jniWrapper, String jwtToken, String userId) {
            if (scriptInjected) {
                outputLogWhenPreviousScriptAlreadyInjected();
                return false;
            } else {
                ScriptDownloader downloader = new ScriptDownloader(arguments, jniWrapper, jwtToken, userId);
                downloader.executeOnExecutor(ThreadPool.getExecutor());
                return true;
            }
        }

        private static void outputLogWhenPreviousScriptAlreadyInjected() {
            Logger.d(TAG, "Previous customer script already injected, do not download again.");
        }

        @Override
        protected Result doInBackground(Void... params) {
            Result result = super.doInBackground(params);
            processDownloadData(parseDownloadDataFromResult(result));
            return result;
        }

        private void processDownloadData(DownloadData downloadData) {
            boolean allowLog = Logger.isLoggableDebug(TAG);
            if (downloadData == null) {
                if (allowLog) {
                    Log.d(TAG, "Download customer script failed, IO or runtime exception");
                }
                return;
            }
            if (downloadData.md5 == null || downloadData.md5.length() != 32) {
                if (allowLog) {
                    Log.d(TAG, "Invalid digest in download customer script");
                }
                return;
            }
            if (downloadData.response == null) {
                if (allowLog) {
                    Log.d(TAG, "Invalid response in download customer script");
                }
                return;
            }
            if (allowLog) {
                Log.d(TAG, "Download customer script, response code: " + downloadData.response.code);
            }
            if (downloadData.response.code != 200) {
                return;
            }
            byte[] pcode = downloadData.response.data;
            if (pcode == null || pcode.length == 0) {
                Log.w(TAG, "Customer script downloaded, but pcode is null !!!");
                return;
            }
            String md5;
            try {
                md5 = StringUtils.toHexString(MessageDigest.getInstance("MD5").digest(pcode), false);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                if (allowLog) {
                    Log.w(TAG, "Download customer script, calc digest failed");
                }
                return;
            }
            if (!downloadData.md5.equalsIgnoreCase(md5)) {
                if (allowLog) {
                    Log.w(TAG, "Download customer script, digest verify failed");
                }
                return;
            }
            //
            boolean prevScriptInjected;
            synchronized (ScriptDownloader.class) {
                prevScriptInjected = scriptInjected;
                scriptInjected = true;
            }
            if (prevScriptInjected) {
                outputLogWhenPreviousScriptAlreadyInjected();
            } else {
                if (allowLog) {
                    Log.d(TAG, "Inject customer scripts ...");
                }
                jniWrapper.injectPCode(pcode);
            }
        }

    }
}
