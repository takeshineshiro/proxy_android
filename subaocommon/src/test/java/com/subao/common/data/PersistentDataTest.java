package com.subao.common.data;

import com.subao.common.RoboBase;
import com.subao.common.io.Persistent;
import com.subao.common.mock.MockPersistent;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;

/**
 * PersistentDataTest
 * <p>Created by YinHaiBo on 2017/2/27.</p>
 */
public class PersistentDataTest extends RoboBase {

    private static final String NAME = "Hello";
    private static final byte[] VALUE = "World".getBytes();

    @Test
    public void save() throws IOException {
        Persistent dir = new MockPersistent();
        PersistentData persistentData = new PersistentData(dir);
        persistentData.save(NAME, VALUE);
        //
        byte[] data = persistentData.load(NAME);
        assertArrayEquals(data, VALUE);
        try {
            persistentData.load(NAME + "_");
            fail();
        } catch (IOException e) {

        }
        //
        persistentData.save(NAME, null);
        try {
            persistentData.load(NAME);
            fail();
        } catch (IOException e) {

        }
    }

}