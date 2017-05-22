package com.subao.common.auth;

import com.subao.common.RoboBase;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * UserConfigListTest
 * <p>Created by YinHaiBo on 2016/12/12.</p>
 */
public class UserConfigListTest extends RoboBase {
    @Test
    public void userConfigList() {
        UserConfigList target = new UserConfigList();
        UserConfig uc = UserConfig.create("110");
        target.put(null, uc);
        assertNull(target.get(null));
        target.put("", uc);
        assertNull(target.get(""));
        target.put("hello", uc);
        assertEquals("110", target.getConfigString("hello"));
        assertNull(target.getConfigString("world"));
        target.put("hello", null);
        assertNull(target.getConfigString("hello"));
    }
}