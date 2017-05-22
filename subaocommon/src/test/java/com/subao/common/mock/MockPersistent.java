package com.subao.common.mock;

import android.os.ConditionVariable;

import com.subao.common.Misc;
import com.subao.common.io.Persistent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Mock implement from {@link Persistent}
 */
public class MockPersistent implements Persistent {

    public boolean ioExceptionWhenOpenOutput;
    public boolean ioExceptionWhenOpenInput;

    public boolean runtimeExceptionWhenOpenOutput;
    public boolean runtimeExceptionWhenOpenInput;

    public ConditionVariable outputStreamClosed = new ConditionVariable();

    private final Map<String, Persistent> children = new HashMap<String, Persistent>();

    private byte[] data;

    @Override
    public boolean exists() {
        return this.data != null;
    }

    @Override
    public InputStream openInput() throws IOException {
        if (!exists()) {
            throw new FileNotFoundException();
        }
        if (ioExceptionWhenOpenInput) {
            throw new IOException("Mock");
        }
        if (runtimeExceptionWhenOpenInput) {
            throw new SecurityException("Test");
        }
        return new ByteArrayInputStream(this.data);
    }

    @Override
    public OutputStream openOutput() throws IOException {
        if (ioExceptionWhenOpenOutput) {
            throw new IOException();
        }
        if (runtimeExceptionWhenOpenOutput) {
            throw new SecurityException("test");
        }
        return new ByteArrayOutputStreamWrapper(4096);
    }

    @Override
    public boolean delete() {
        if (data != null) {
            data = null;
            return true;
        }
        return false;
    }

    @Override
    public Persistent createChild(String name) {
        Persistent child = children.get(name);
        if (child == null) {
            child = new MockPersistent();
            children.put(name, child);
        }
        return child;
    }

    @Override
    public byte[] read() throws IOException {
        InputStream inputStream = null;
        ByteArrayOutputStream outputStream = null;
        try {
            inputStream = this.openInput();
            outputStream = new ByteArrayOutputStream(1024);
            byte[] buffer = new byte[1024];
            while (true) {
                int size = inputStream.read(buffer);
                if (size <= 0) {
                    break;
                }
                outputStream.write(buffer, 0, size);
            }
        } finally {
            Misc.close(inputStream);
            Misc.close(outputStream);
        }
        return outputStream.toByteArray();
    }

    public byte[] getData() {
        return this.data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    private class ByteArrayOutputStreamWrapper extends ByteArrayOutputStream {

        ByteArrayOutputStreamWrapper(int capacity) {
            super(capacity);
        }

        @Override
        public void close() throws IOException {
            super.close();
            MockPersistent.this.data = this.toByteArray();
            MockPersistent.this.outputStreamClosed.open();
        }

    }
}
