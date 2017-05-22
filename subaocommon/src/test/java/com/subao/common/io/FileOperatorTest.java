package com.subao.common.io;

import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class FileOperatorTest {

    @Test
    public void init() {
        Context context = RuntimeEnvironment.application.getApplicationContext();
        FileOperator.init(context, true);
        FileOperator.init(context, false);
    }

    @Test
    public void getDataFile() throws Exception {
        assertNotNull(FileOperator.getDataFile("temp"));
    }

    @Test
    public void read() throws IOException {
        byte[] data = "Hello, world".getBytes();
        File file = File.createTempFile("subao_test", "tmp");
        try {
            assertTrue(FileOperator.write(file, data));
            byte[] read = FileOperator.read(file);
            assertArrayEquals(data, read);
        } finally {
            file.delete();
        }
    }

    @Test
    public void read2() throws IOException {
        File file = new File("A:/not_exist");
        assertNull(FileOperator.read(file));
    }

    @Test
    public void getDataDirectoryAbsolutePath() {
        assertNotNull(FileOperator.getDataDirectoryAbsolutePath());
    }

    @Test
    public void appendPathCharIfNeed() {
        assertNull(FileOperator.appendPathCharIfNeed(null));
        assertEquals("/", FileOperator.appendPathCharIfNeed(""));
        assertEquals("hello/", FileOperator.appendPathCharIfNeed("hello"));
        assertEquals("hello/", FileOperator.appendPathCharIfNeed("hello/"));
    }

}