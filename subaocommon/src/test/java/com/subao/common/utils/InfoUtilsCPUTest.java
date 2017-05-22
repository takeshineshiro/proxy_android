package com.subao.common.utils;

import android.os.Build;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * InfoUtilsCPUTest
 * <p>Created by YinHaiBo on 2016/12/28.</p>
 */
public class InfoUtilsCPUTest {

    private MockFileOperator mockFileOperator;

    @Before
    public void setUp() throws Exception {
        this.mockFileOperator = new MockFileOperator();
        InfoUtils.setFileReaderCreator(mockFileOperator);
    }

    @After
    public void tearDown() throws Exception {
        InfoUtils.setFileReaderCreator(null);
        this.mockFileOperator = null;
    }

    @Test
    public void constructor() {
        new InfoUtils.CPU();
    }

    @Test
    public void getCpuName() throws Exception {
        assertEquals("/proc/cpuinfo", InfoUtils.CPU.FILENAME_CPU_INFO);
        String cpuName = "Qualcomm MSM8974PRO-AC";
        String fileContent = String.format(
            "Processor       : ARMv7 Processor rev 1 (v7l)\n" +
                "processor       : 0\n" +
                "BogoMIPS        : 38.40\n" +
                "\n" +
                "processor       : 1\n" +
                "BogoMIPS        : 38.40\n" +
                "\n" +
                "processor       : 2\n" +
                "BogoMIPS        : 38.40\n" +
                "\n" +
                "processor       : 3\n" +
                "BogoMIPS        : 38.40\n" +
                "\n" +
                "Features        : swp half thumb fastmult vfp edsp neon vfpv3 tls vfpv4 idiva idivt\n" +
                "CPU implementer : 0x51\n" +
                "CPU architecture: 7\n" +
                "CPU variant     : 0x2\n" +
                "CPU part        : 0x06f\n" +
                "CPU revision    : 1\n" +
                "\n" +
                "Hardware        : %s\n" +
                "Revision        : 0000\n" +
                "Serial          : 0000000000000000",
            cpuName
        );
        //
        assertNull(InfoUtils.CPU.getCpuName());
        mockFileOperator.ioException = true;
        assertNull(InfoUtils.CPU.getCpuName());
        mockFileOperator.ioException = false;
        //
        mockFileOperator.content = fileContent.getBytes();
        assertEquals(cpuName, InfoUtils.CPU.getCpuName());
        //
        mockFileOperator.content = "Hello\nWorld".getBytes();
        assertNull(InfoUtils.CPU.getCpuName());
    }
//
//    @Test
//    public void parseLongFromFile() throws IOException {
//        File file = File.createTempFile("test", "temp");
//        String filename = file.getAbsolutePath();
//        try {
//            FileWriter writer = new FileWriter(file);
//            writer.write("123456");
//            Misc.close(writer);
//            long value = InfoUtils.CPU.parseLongFromFile(filename);
//            assertEquals(123456L, value);
//        } finally {
//            file.delete();
//        }
//        //
//        assertEquals(-1L, InfoUtils.CPU.parseLongFromFile(filename));
//    }

    @Test
    public void getCores() throws IOException {
        assertEquals("/sys/devices/system/cpu/", InfoUtils.CPU.DIRECTORY_CPU);
        assertEquals(1, InfoUtils.CPU.getCores(Build.VERSION_CODES.GINGERBREAD_MR1));
        //
        mockFileOperator.file = new MockDir(InfoUtils.CPU.DIRECTORY_CPU);
        assertTrue(InfoUtils.CPU.getCores() >= 1);
        assertEquals(3, InfoUtils.CPU.getCores(Build.VERSION_CODES.GINGERBREAD_MR1 + 1));
        mockFileOperator.runtimeException = true;
        assertEquals(1, InfoUtils.CPU.getCores(Build.VERSION_CODES.GINGERBREAD_MR1 + 1));
    }

    @Test
    public void getCpuFileCount() throws Exception {

    }

    @Test
    public void getMaxFreqKHz() throws Exception {

    }

    private static class MockDir extends File {

        public final String path;

        private static final String[] names = new String[] {
            "cpu3", "cpu7", "cpu8", "cpuu", "cpua",
            "other", "hello",
        };

        MockDir(String path) {
            super(path);
            this.path = path;
        }

        @Override
        public File[] listFiles(FileFilter filter) {
            List<File> files = new ArrayList<File>(5);
            for (String name : names) {
                File file = new File(path, name);
                if (filter.accept(file)) {
                    files.add(file);
                }
            }
            File[] result = new File[files.size()];
            return files.toArray(result);
        }
    }

    private static class MockFileOperator implements FileUtils.FileOperator {

        boolean ioException;
        boolean runtimeException;

        byte[] content;

        File file;

        @Override
        public File createFile(String pathname) {
            if (runtimeException) {
                throw new RuntimeException();
            }
            return this.file;
        }

        @Override
        public Reader openReader(String filename) throws IOException {
            return new InputStreamReader(new ByteArrayInputStream(content));
        }

        @Override
        public InputStream openInput(String filename) throws IOException {
            if (ioException) {
                throw new IOException();
            }
            if (runtimeException) {
                throw new RuntimeException();
            }
            return new ByteArrayInputStream(content);
        }
    }

}