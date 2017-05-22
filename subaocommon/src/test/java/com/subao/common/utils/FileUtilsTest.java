package com.subao.common.utils;

import com.subao.common.Misc;
import com.subao.common.MockFile;

import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class FileUtilsTest {

    private static File createTempFile(File dir, int bytes) throws IOException {
        File file = File.createTempFile("xunyou", "subao", dir);
        try {
            if (bytes > 0) {
                OutputStream output = new FileOutputStream(file);
                try {
                    byte[] data = buildData(bytes);
                    output.write(data);
                } finally {
                    Misc.close(output);
                }
            }
        } catch (IOException e) {
            file.delete();
            return null;
        }
        return file;
    }

    private static byte[] buildData(int bytes) {
        byte[] data = new byte[bytes];
        for (int i = 0; i < data.length; ++i) {
            data[i] = (byte) (i & 0xff);
        }
        return data;
    }

    @Test
    public void testConstructor() {
        new FileUtils();
    }

    @Test(expected = IOException.class)
    public void readFileNotExists() throws IOException {
        MockFile file = new MockFile();
        file.valueIsFile = true;
        FileUtils.read(file, 100);
    }

    @Test(expected = IOException.class)
    public void readNotFile() throws IOException {
        MockFile file = new MockFile();
        file.valueExists = true;
        FileUtils.read(file, 100);
    }

    @Test(expected = IOException.class)
    public void readTooLargeFile() throws IOException {
        MockFile file = new MockFile();
        file.valueExists = true;
        file.valueIsFile = true;
        file.valueLength = 100;
        FileUtils.read(file, 99);
    }

    @Test
    public void read() throws IOException {
        int size = 64;
        File file = createTempFile(null, size);
        try {
            byte[] data = FileUtils.read(file, size);
            byte[] expected = buildData(size);
            assertEquals(size, data.length);
            assertEquals(size, expected.length);
            for (int i = 0; i < size; ++i) {
                if (data[i] != expected[i]) {
                    fail();
                }
            }
        } finally {
            file.delete();
        }
    }

    @Test
    public void testWrite() throws IOException {
        File file = createTempFile(null, 0);
        try {
            byte[] data = buildData(300);
            FileUtils.write(file, data);
            byte[] read = FileUtils.read(file, 300);
            assertEquals(data.length, read.length);
            for (int i = 0; i < read.length; ++i) {
                assertEquals(data[i], read[i]);
            }
        } finally {
            file.delete();
        }
    }

}
