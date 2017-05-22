package com.subao.common.io;

import android.content.Context;

import com.subao.common.Misc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * 文件操作工具包
 */
public class FileOperator {

    private static File dirData;

    public static synchronized void init(Context context, boolean isSDK) {
        if (dirData == null) {
            if (isSDK) {
                dirData = context.getDir("cn.wsds.sdk.game.data", Context.MODE_PRIVATE);
            } else {
                dirData = context.getFilesDir();
            }
        }
    }

    public static File getDataDirectory() {
        return dirData;
    }

    public static File getDataFile(String name) {
        return new File(dirData, name);
    }

    /**
     * 非追加的文件写入
     *
     * @param file
     * @param data
     * @param failIfFileAlreadyExists True表示如果文件已存在就不写
     */
    private static boolean write(File file, byte[] data, boolean failIfFileAlreadyExists) {
        try {
            // 判断目录是否存在，不存在则创建
            File parentFile = file.getParentFile();
            if (!parentFile.exists()) {
                if (!parentFile.mkdirs()) {
                    return false;
                }
            }
            if (file.exists()) {
                if (failIfFileAlreadyExists) {
                    return false;
                }
            } else {
                file.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(file, false);
            fos.write(data);
            fos.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    public static boolean write(File file, byte[] data) {
        return write(file, data, false);
    }

    /**
     * 读取文件字节流
     *
     * @param file
     * @return 成功时返回文件内容，失败时返回null
     */
    public static byte[] read(File file) {
        if (!file.isFile() || !file.exists()) {
            return null;
        }
        long size = file.length();
        if (size <= 0 || size > 100 * 1024 * 1024) {
            return null;
        }
        byte[] result = new byte[(int) size];
        try {
            InputStream input = new FileInputStream(file);
            try {
                input.read(result);
            } finally {
                Misc.close(input);
            }
        } catch (Exception ex) {
            return null;
        }
        return result;
    }

    public static String getDataDirectoryAbsolutePath() {
        return appendPathCharIfNeed(dirData.getAbsolutePath());
    }

    static String appendPathCharIfNeed(String directory) {
        if (directory == null) {
            return null;
        }
        if (directory.length() == 0) {
            return "/";
        }
        int idxOfEnd = directory.length() - 1;
        if (directory.charAt(idxOfEnd) != '/') {
            return directory + "/";
        } else {
            return directory;
        }
    }

}
