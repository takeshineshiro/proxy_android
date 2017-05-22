package com.subao.common.data;

import android.text.TextUtils;

import com.subao.common.net.Http;

import java.net.URLEncoder;

/**
 * 从HR下载指定用户的动态脚本
 * <p>Created by YinHaiBo on 2017/2/8.</p>
 *
 * @see PortalScriptDownloader
 */
public class CustomerScriptDownloader extends HRDataTrans {

    protected final UserInfoEx userInfoEx;

    protected CustomerScriptDownloader(Arguments arguments, UserInfo userInfo, UserInfoEx userInfoEx) {
        super(arguments, userInfo, Http.Method.GET, null);
        this.userInfoEx = userInfoEx;
    }

    public static DownloadData parseDownloadDataFromResult(Result result) {
        String md5 = parseMD5DigestStringFromResponse(result);
        return new DownloadData(md5, result == null ? null : result.response);
    }

    private static String parseMD5DigestStringFromResponse(Result result) {
        if (result == null || result.connection == null) {
            return null;
        }
        String value = result.connection.getHeaderField("ETag");
        if (value == null || value.length() != 32 + 2) {
            return null;
        }
        return value.substring(1, 33);
    }

    @SuppressWarnings("deprecation")
    private static String encodeParam(String value) {
        if (TextUtils.isEmpty(value)) {
            return "";
        }
        return URLEncoder.encode(value);
    }

    @Override
    protected String getUrlPart() {
        return String.format("/api/v2/%s/scripts?serviceId=%s&userId=%s&subaoId=%s&clientVersion=%s",
            arguments.clientType,
            encodeParam(userInfoEx.serviceId),
            encodeParam(userInfo.userId),
            encodeParam(userInfoEx.subaoId),
            encodeParam(arguments.version));
    }

    @Override
    protected String getUrlProtocol() {
        return "https";
    }

    public static class UserInfoEx {
        public final String serviceId;
        public final String subaoId;

        public UserInfoEx(String serviceId, String subaoId) {
            this.serviceId = serviceId;
            this.subaoId = subaoId;
        }

    }

    public static class DownloadData {
        public final String md5;
        public final Http.Response response;

        public DownloadData(String md5, Http.Response response) {
            this.md5 = md5;
            this.response = response;
        }
    }
}
