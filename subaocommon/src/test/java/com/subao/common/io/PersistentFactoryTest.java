package com.subao.common.io;

import com.subao.common.Misc;
import com.subao.common.RoboBase;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PersistentFactoryTest extends RoboBase {

    @Test(expected = NullPointerException.class)
    public void testFileNull() {
        new PersistentFactory();
        PersistentFactory.createByFile(null);
    }

    @Test
    public void exists() throws IOException {
        MockFile file = new MockFile();
        Persistent p = PersistentFactory.createByFile(file);
        //
        assertFalse(p.exists());
        file.isExists = true;
        assertTrue(p.exists());
        file.isFile = false;
        assertFalse(p.exists());
    }

    @Test
    public void io() throws IOException {
        byte[] data = "hello".getBytes();
        File file = File.createTempFile("Subao", "tmp");
        try {
            Persistent p = PersistentFactory.createByFile(file);
            OutputStream output = p.openOutput();
            output.write(data);
            Misc.close(output);
            //
            InputStream input = p.openInput();
            byte[] buffer = new byte[128];
            assertEquals(5, input.read(buffer));
            assertEquals(new String(data), new String(buffer, 0, 5));
            Misc.close(input);
            //
            byte[] read = p.read();
            assertEquals(5, read.length);
            assertArrayEquals(data, read);

        } finally {
            file.delete();
        }
    }

    @Test
    public void createChild() throws IOException {
        File tmpDir = new File(System.getProperty("java.io.tmpdir", "."));
        tmpDir = new File(tmpDir, UUID.randomUUID().toString());
        try {
            Persistent p = PersistentFactory.createByFile(tmpDir);
            p.createChild("child");
            assertTrue(tmpDir.isDirectory());
            assertTrue(tmpDir.exists());
        } finally {
            tmpDir.delete();
        }
    }

    @Test
    public void delete() {
        MockFile file = new MockFile();
        Persistent p = PersistentFactory.createByFile(file);
        file.isExists = true;
        assertTrue(p.delete());
        assertFalse(p.delete());
    }

    private static class MockFile extends File {

        public boolean isExists, isFile = true;

        public MockFile() {
            super("The Mock File");
        }

        @Override
        public boolean exists() {
            return isExists;
        }

        @Override
        public boolean delete() {
            if (isExists) {
                isExists = false;
                return true;
            } else {
                return false;
            }
        }

        @Override
        public boolean isFile() {
            return this.isFile;
        }
    }
}
