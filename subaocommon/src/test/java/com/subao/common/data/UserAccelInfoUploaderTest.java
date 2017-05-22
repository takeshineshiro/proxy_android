package com.subao.common.data;

import com.subao.common.RoboBase;
import com.subao.common.mock.MockNetTypeDetector;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * UserAccelInfoUploaderTest
 * <p>Created by YinHaiBo on 2017/2/25.</p>
 */
public class UserAccelInfoUploaderTest extends RoboBase {

    @Test
    public void getUrlPart() throws Exception {
        UserAccelInfoUploader uploader = new UserAccelInfoUploader(
            new HRDataTrans.Arguments("android", "2.0.0",
                new ServiceLocation(null, "127.0.0.1", 1024),
                new MockNetTypeDetector()),
            new HRDataTrans.UserInfo("userId", "jwt"),
            "Hello".getBytes());
        assertEquals(
            "/api/v1/android/users/userId/gameAccel",
            uploader.getUrlPart());
    }

}