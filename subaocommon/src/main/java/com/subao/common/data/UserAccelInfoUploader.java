package com.subao.common.data;

import com.subao.common.net.Http;
import com.subao.common.thread.ThreadPool;

/**
 * 用户加速信息的上传
 * <p>Created by YinHaiBo on 2017/2/22.</p>
 */
public class UserAccelInfoUploader extends HRDataTrans {

    public static final String DEFAULT_PROTOCOL = "http";
    private static String protocol = DEFAULT_PROTOCOL;

    protected UserAccelInfoUploader(Arguments arguments, UserInfo userInfo, byte[] postData) {
        super(arguments, userInfo, Http.Method.POST, postData);
    }

    public static void start(Arguments arguments, UserInfo userInfo, byte[] postData) {
        UserAccelInfoUploader userAccelInfoUploader = new UserAccelInfoUploader(
            arguments, userInfo, postData
        );
        userAccelInfoUploader.executeOnExecutor(ThreadPool.getExecutor());
    }

    public static void setProtocol(String value) {
        if ("https".equals(value)) {
            protocol = value;
        } else {
            protocol = DEFAULT_PROTOCOL;
        }
    }

    @Override
    protected String getUrlPart() {
        return "/api/v1/" + arguments.clientType + "/users/" + userInfo.userId + "/gameAccel";
    }

    @Override
    protected String getUrlProtocol() {
        return protocol;
    }
}
