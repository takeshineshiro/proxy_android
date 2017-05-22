package com.subao.common;

import java.io.File;

/**
 * MockFile
 * <p>Created by YinHaiBo on 2016/11/28.</p>
 */
public class MockFile extends File {

    public boolean valueIsFile;
    public boolean valueExists;
    public long valueLength;
    public boolean mkdirFail, mkdirsFail;

    public MockFile() {
        super("null");
    }

    public MockFile(String path) {
        super(path);
    }

    @Override
    public boolean delete() {
        if (exists()) {
            this.valueExists = false;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void deleteOnExit() {

    }

    @Override
    public boolean exists() {
        return valueExists;
    }

    @Override
    public boolean isFile() {
        return this.valueIsFile;
    }

    @Override
    public long length() {
        return this.valueLength;
    }

    @Override
    public boolean mkdir() {
        return !mkdirFail;
    }

    @Override
    public boolean mkdirs() {
        return !mkdirsFail;
    }
}
