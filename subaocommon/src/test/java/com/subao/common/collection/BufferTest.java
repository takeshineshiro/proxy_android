package com.subao.common.collection;

import org.junit.Test;

import java.io.ByteArrayInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * BufferTest
 * <p>Created by YinHaiBo on 2016/11/26.</p>
 */
public class BufferTest {

    @Test
    public void readFromInputStream() throws Exception {
        byte[] data = new byte[3731];
        for (int i = data.length - 1; i >= 0; --i) {
            data[i] = (byte) (Math.random() * 256);
        }
        Buffer buffer = new Buffer(7);
        ByteArrayInputStream input = new ByteArrayInputStream(data);

        int[] readList = new int[] { 1, 3, 3, 7, 70, 100, 77, 99, 1000, 1000 };
        int total = 0;
        int i = 0;
        while (true) {
            int readBytes = readList[i];
            if (i < readList.length - 1) {
                ++i;
            }
            int read = buffer.readFromInputStream(input, readBytes);
            if (read <= 0) {
                break;
            }
            total += read;
        }
        assertEquals(data.length, total);
        //
        byte[] clone = buffer.cloneArray();
        assertEquals(clone.length, data.length);
        for (int n = data.length - 1; n >= 0; --n) {
            if (data[n] != clone[n]) {
                fail();
            }
        }
    }

}