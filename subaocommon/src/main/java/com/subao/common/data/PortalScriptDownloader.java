package com.subao.common.data;

import android.util.Log;

import com.subao.common.LogTag;
import com.subao.common.Logger;
import com.subao.common.net.Http;
import com.subao.common.thread.ThreadPool;
import com.subao.common.utils.StringUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executor;

/**
 * 负责从Portal进行脚本下载
 * <p>Created by YinHaiBo on 2017/1/18.</p>
 */

public class PortalScriptDownloader extends PortalDataDownloader {

    private static final String TAG = LogTag.DATA;

    protected PortalScriptDownloader(Arguments arguments) {
        super(arguments);
    }

    /**
     * 开始工作。Portal脚本下载的流程比较特殊，详述如下：
     * <ol>
     * <li>所有数据都需要用MD5校验，MD5值=ETag值（注意ETag里的MD5是用双引号括起来的）</li>
     * <li>先从本地缓存文件加载数据（上一次下载到的）</li>
     * <li>判断这个缓存文件的Version，如果与当前客户端Version不相等，则不能使用</li>
     * <li>启动线程开始下载，同时在{@link #executeOnExecutor(Executor, Object[])}的时候，传递额外参数：</li>
     * <ul>
     * <li>如果Version一致，则传递这个缓存文件内容</li>
     * <li>如果Version不一致，则传递null</li>
     * </ul>
     * <li>从HTTP下载如果取得Response Code，则：</li>
     * <ul>
     *     <li>200：成功获得，存储在本地</li>
     *     <li>304：内容未发生变化，不修改本地存储</li>
     *     <li>404：本版本已没有Portal数据，需要删除本地文件（如果有的话）</li>
     *     <li>其它：无动作</li>
     * </ul>
     * </ol>
     *
     * @return 本地缓存的“上一次下载的”且“版本号与当前客户端版本号相等”的数据，或null
     */
    public static PortalDataEx start(Arguments arguments) {
        PortalScriptDownloader portalScriptDownloader = new PortalScriptDownloader(arguments);
        PortalDataEx localData = portalScriptDownloader.loadFromPersistent();
        if (localData != null) {
            if (!portalScriptDownloader.isScriptValid(localData)) {
                portalScriptDownloader.deletePersistent();  // 本地文件版本不符，已经不能用了，删掉
                localData = null;
            }
        }
        portalScriptDownloader.executeOnExecutor(ThreadPool.getExecutor(), localData);
        return localData;
    }

    @Override
    protected PortalDataEx doInBackground(PortalDataEx... params) {
        return super.doInBackground(params);
    }

    @Override
    protected boolean checkDownloadData(PortalDataEx data) {
        return super.checkDownloadData(data) && isScriptValid(data);
    }

    @Override
    protected String getId() {
        return "scripts";
    }

    @Override
    protected String getUrlPart() {
        return "scripts/" + getArguments().version;
    }

    @Override
    protected String getHttpAcceptType() {
        return Http.ContentType.ANY.str;
    }

    /**
     * 判断给定的数据是否有效？
     * <p><b>仅当版本号相符、且MD5检验成功才算有效</b></p>
     *
     * @param portalData 脚本数据
     * @return true表示有效
     */
    final boolean isScriptValid(PortalDataEx portalData) {
        boolean allowLog = Logger.isLoggableDebug(TAG);
        if (portalData == null) {
            return false;
        }
        if (!isVersionValid(portalData)) {
            if (allowLog) {
                Log.d(TAG, "Invalid script version");
            }
            return false;
        }
        byte[] scripts = portalData.getData();
        if (scripts == null) {
            if (allowLog) {
                Log.d(TAG, "Script is null");
            }
            return false;
        }
        String md5Expected = portalData.getCacheTag();
        if (md5Expected == null || md5Expected.length() != 32 + 2) { // 用双引号括起来的
            if (allowLog) {
                Log.d(TAG, "Invalid script digest");
            }
            return false;
        }
        byte[] digest;
        try {
            digest = MessageDigest.getInstance("MD5").digest(scripts);
        } catch (NoSuchAlgorithmException e) {
            if (allowLog) {
                Log.d(TAG, "Digest calc failed");
            }
            return false;
        }
        String digestString = StringUtils.toHexString(digest, false);
        md5Expected = md5Expected.substring(1, md5Expected.length() - 1);
        boolean result = md5Expected.equalsIgnoreCase(digestString);
        if (allowLog) {
            if (result) {
                Log.d(TAG, "Script check ok");
            } else {
                Log.d(TAG, "Script digest is not expected");
            }
        }
        return result;
    }
}
