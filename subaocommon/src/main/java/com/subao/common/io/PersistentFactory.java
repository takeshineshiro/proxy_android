package com.subao.common.io;

import com.subao.common.Misc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

public class PersistentFactory {


    public static Persistent createByFile(File file) {
        return new PersistentFileImpl(file);
    }

    /**
     * 基于{@link File}的{@link Persistent}
     */
    private static class PersistentFileImpl implements Persistent {

        private final File file;

        public PersistentFileImpl(File file) {
            if (file == null) {
                throw new NullPointerException("File is null");
            }
            this.file = file;
        }

        @Override
        public boolean exists() {
            return file.exists() && file.isFile();
        }

        @Override
        public InputStream openInput() throws IOException {
            return new FileInputStream(this.file);
        }

        @Override
        public OutputStream openOutput() throws IOException {
            return new FileOutputStream(this.file);
        }

        @Override
        public boolean delete() {
            return file.delete();
        }

        @Override
        public Persistent createChild(String name) {
            if (!file.exists() || !file.isDirectory()) {
                file.mkdirs();
            }
            return new PersistentFileImpl(new File(file, name));
        }

        @Override
        public byte[] read() throws IOException {
            byte[] result;
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
            try {
                result = new byte[(int)randomAccessFile.length()];
                randomAccessFile.read(result);
            } finally {
                Misc.close(randomAccessFile);
            }
            return result;
        }
    }

}
