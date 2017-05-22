package com.subao.common.data;

import com.subao.common.RoboBase;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * PortalDataExTest
 * <p>Created by YinHaiBo on 2017/2/16.</p>
 */
public class PortalDataExTest extends RoboBase {

    private static final String TEST_CACHE_TAG = "the cache tag";
    private static final long TEST_EXPIRE_TIME = 1234L;
    private static final byte[] TEST_DATA = new byte[]{1, 2, 3};
    private static final String TEST_VERSION = "the version";

    private static PortalDataEx deserializeFromByteArray(byte[] serialized) throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream(serialized);
        return PortalDataEx.deserialize(input);
    }

    public static byte[] serializeToByteArray(PortalDataEx portalData) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(1024);
        portalData.serialize(output);
        return output.toByteArray();
    }

    @Test
    public void constructor() {
        PortalDataEx data = new PortalDataEx(TEST_CACHE_TAG, TEST_EXPIRE_TIME, TEST_VERSION, TEST_DATA, true);
        assertEquals(TEST_CACHE_TAG, data.getCacheTag());
        assertEquals(TEST_VERSION, data.getVersion());
        assertArrayEquals(TEST_DATA, data.getData());
        assertTrue(data.isNewByDownload);
        assertNotNull(data.toString());
        assertEquals(data, data);
        assertEquals(data, new PortalDataEx(TEST_CACHE_TAG, TEST_EXPIRE_TIME, TEST_VERSION, TEST_DATA, true));
        assertNotEquals(data, null);
        assertNotEquals(data, this);
        assertNotEquals(data, new PortalDataEx(TEST_CACHE_TAG, TEST_EXPIRE_TIME, TEST_VERSION, TEST_DATA, false));
    }

    @Test
    public void test() throws Exception {
        String[] cacheTagList = new String[]{null, "", TEST_CACHE_TAG};
        String[] versionList = new String[]{null, "", TEST_VERSION};
        byte[][] dataList = new byte[][]{null, new byte[0], TEST_DATA};
        for (String cacheTag : cacheTagList) {
            for (long expireTime = 0; expireTime <= 1; ++expireTime) {
                for (String version : versionList) {
                    for (byte[] data : dataList) {
                        PortalDataEx p1 = new PortalDataEx(cacheTag, expireTime, version, data);
                        assertEquals(cacheTag, p1.getCacheTag());
                        assertEquals(version, p1.getVersion());
                        assertEquals(data == null ? 0 : data.length, p1.getDataSize());
                        assertEquals(data, p1.getData());
                        assertNotNull(p1.toString());
                        //
                        assertEquals(p1, p1);
                        assertFalse(p1.equals(null));
                        assertFalse(p1.equals(this));
                        assertEquals(p1, new PortalDataEx(cacheTag, expireTime, version, data));
                        //
                        byte[] serialized = serializeToByteArray(p1);
                        PortalDataEx p2 = deserializeFromByteArray(serialized);
                        assertEquals(p1, p2);
                    }
                }
            }
        }
    }

    @Test(expected = IOException.class)
    public void getNextBlockException() throws IOException {
        PortalDataEx p1 = new PortalDataEx(TEST_CACHE_TAG, TEST_EXPIRE_TIME, TEST_VERSION, TEST_DATA);
        byte[] serialized = serializeToByteArray(p1);
        serialized[4] = 0x7f;
        deserializeFromByteArray(serialized);
    }

    @Test(expected = EOFException.class)
    public void getNextLongException() throws IOException {
        PortalDataEx p1 = new PortalDataEx(TEST_CACHE_TAG, TEST_EXPIRE_TIME, TEST_VERSION, TEST_DATA);
        byte[] serialized = serializeToByteArray(p1);
        int size = 4 + 4 + TEST_CACHE_TAG.length() + 7;
        byte[] data = Arrays.copyOf(serialized, size);
        data[0] = (byte) (size >> 24);
        data[1] = (byte) (size >> 16);
        data[2] = (byte) (size >> 8);
        data[3] = (byte) (size);
        deserializeFromByteArray(data);
    }

    @Test(expected = IOException.class)
    public void totalSizeError_1() throws IOException {
        PortalDataEx p1 = new PortalDataEx(TEST_CACHE_TAG, TEST_EXPIRE_TIME, TEST_VERSION, TEST_DATA);
        byte[] serialized = serializeToByteArray(p1);
        serialized[0] = 0x7f;
        deserializeFromByteArray(serialized);
    }

    @Test(expected = IOException.class)
    public void totalSizeError_2() throws IOException {
        PortalDataEx p1 = new PortalDataEx(TEST_CACHE_TAG, TEST_EXPIRE_TIME, TEST_VERSION, TEST_DATA);
        byte[] serialized = serializeToByteArray(p1);
        serialized[3] = 15;
        deserializeFromByteArray(serialized);
    }

    @Test(expected = IOException.class)
    public void totalSizeError_3() throws IOException {
        byte[] serialized = new byte[3];
        deserializeFromByteArray(serialized);
    }

    @Test(expected = IOException.class)
    public void totalSizeError_4() throws IOException {
        PortalDataEx p1 = new PortalDataEx(TEST_CACHE_TAG, TEST_EXPIRE_TIME, TEST_VERSION, TEST_DATA);
        byte[] serialized = serializeToByteArray(p1);
        serialized[3] = (byte) (serialized[3] + 1);
        deserializeFromByteArray(serialized);
    }
}