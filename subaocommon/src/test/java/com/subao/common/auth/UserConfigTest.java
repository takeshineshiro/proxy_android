package com.subao.common.auth;

import android.annotation.SuppressLint;

import com.subao.common.RoboBase;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by hujd on 16-7-19.
 */
public class UserConfigTest extends RoboBase {

    @Test
    public void create() throws IOException {
        UserConfig uc = UserConfig.create("112");
        assertEquals(uc.value, "112");
        assertTrue(uc.accel);
        assertTrue(uc.parallel);
        assertEquals(uc.accelMode, '2');
        //
        assertNull(UserConfig.create("11"));
        assertNull(UserConfig.create(""));
        assertNull(UserConfig.create(null));
    }

    @Test
    public void testConstruction() {
        for (int accel = 0; accel <= 1; ++accel) {
            for (int parallel = 0; parallel <= 1; ++parallel) {
                for (int mode = 0; mode <= 2; ++mode) {
                    @SuppressLint("DefaultLocale") String value = String.format("%d%d%d", accel, parallel, mode);
                    UserConfig uc = UserConfig.create(value);
                    assertEquals(value, uc.toString());
                    assertTrue(uc.accel || accel == 0);
                    assertTrue(uc.parallel || parallel == 0);
                    assertEquals((char)(mode + '0'), uc.accelMode);
                    //
                    UserConfig uc2 = new UserConfig(accel != 0, parallel != 0, (char) (mode + '0'));
                    assertEquals(uc, uc2);
                    assertEquals(uc.value, uc2.value);
                }
            }
        }
    }

    @Test
    public void testEquals() {
        UserConfig uc = UserConfig.create("123");
        assertFalse(uc.equals(null));
        assertTrue(uc.equals(uc));
        UserConfig uc2 = UserConfig.create("123");
        assertEquals(uc2, uc);
        assertFalse(uc.equals(new Object()));
        assertFalse(uc.equals(UserConfig.create("122")));
        assertFalse(uc.equals(UserConfig.create("133")));
        assertFalse(uc.equals(UserConfig.create("223")));
    }

    @Test
    public void isParallelSwitchOn() {
        assertFalse(UserConfig.isParallelSwitchOn(null));
        assertFalse(UserConfig.isParallelSwitchOn(""));
        assertFalse(UserConfig.isParallelSwitchOn("1"));
        assertFalse(UserConfig.isParallelSwitchOn("100"));
        assertTrue(UserConfig.isParallelSwitchOn("120"));
        assertTrue(UserConfig.isParallelSwitchOn("010"));
    }

}