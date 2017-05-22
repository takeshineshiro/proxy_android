package com.subao.common.data;

import android.os.Environment;

import com.subao.common.Misc;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * 本地测试人员编写的脚本
 * <p>Created by YinHaiBo on 2017/2/22.</p>
 */
public class LocalScripts {

    private static final String FILENAME_PREFIX = "com.subao.gamemaster.script.";

    public static byte[] load(boolean isSDK) throws IOException {
        File file = getFile(isSDK);
        if (file.isFile() && file.exists()) {
            byte[] buffer;
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
            try {
                long size = randomAccessFile.length();
                if (size > 1024 * 1024 * 2) {
                    throw new IOException("Script file too large");
                }
                buffer = new byte[(int) size];
                randomAccessFile.read(buffer);
            } finally {
                Misc.close(randomAccessFile);
            }
            return buffer;
        } else {
            return null;
        }
    }

    public static File getFile(boolean isSDK) {
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        return new File(dir, FILENAME_PREFIX + (isSDK ? "sdk" : "app"));
    }
}
