package com.subao.common.utils;

import com.subao.common.Misc;
import com.subao.common.collection.Buffer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

public class FileUtils {

    public static byte[] read(File file, int validFileSize) throws IOException {
        if (!file.exists() || !file.isFile()) {
            throw new FileNotFoundException(file.getAbsolutePath());
        }
        int fileLength = (int) file.length();
        if (fileLength > validFileSize) {
            throw new IOException("File is too large.");
        }
        FileInputStream input = null;
        try {
            input = new FileInputStream(file);
            Buffer buffer = new Buffer(fileLength);
            while (buffer.readFromInputStream(input, fileLength) > 0) ;
            return buffer.cloneArray();
        } finally {
            Misc.close(input);
        }
    }

    public static void write(File file, byte[] data) throws IOException {
        write(file, data, 0, data.length);
    }

    public static void write(File file, byte[] data, int offset, int size) throws IOException {
        File dir = file.getParentFile();
        if (dir != null && (!dir.exists() || !dir.isDirectory())) {
            dir.mkdirs();
        }
        //
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(file);
            output.write(data, offset, size);
        } finally {
            Misc.close(output);
        }
    }

    public interface FileOperator {

        FileOperator DEFAULT = new FileOperator() {

            @Override
            public File createFile(String pathname) {
                return new File(pathname);
            }

            @Override
            public Reader openReader(String filename) throws IOException {
                return new FileReader(filename);
            }

            @Override
            public InputStream openInput(String filename) throws IOException {
                return new FileInputStream(filename);
            }
        };

        /**
         * 根据指定的文件名创建一个{@link File}对象
         *
         * @param pathname 路径名
         * @return {@link File}
         */
        File createFile(String pathname);

        Reader openReader(String filename) throws IOException;

        InputStream openInput(String filename) throws IOException;


    }
}
