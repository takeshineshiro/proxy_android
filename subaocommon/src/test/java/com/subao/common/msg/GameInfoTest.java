package com.subao.common.msg;

import com.subao.common.data.AppType;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * GameInfoTest
 * <p>Created by YinHaiBo on 2016/12/20.</p>
 */
public class GameInfoTest {

    @Test
    public void gameInfo() {
        String appLabel = "label";
        String pkgName = "pkgName";
        AppType appType = AppType.ANDROID_SDK;
        MessageTools.GameInfo g1 = new MessageTools.GameInfo(appLabel, pkgName, appType);
        MessageTools.GameInfo g2 = new MessageTools.GameInfo(
            new Message_App(appLabel, pkgName), appType);
        assertEquals(g1, g2);
        assertTrue(g1.equals(g1));
        assertFalse(g1.equals(null));
        assertFalse(g1.equals(this));
        assertEquals(appLabel, g1.messageApp.appLabel);
        assertEquals(pkgName, g1.messageApp.pkgName);
        assertEquals(appType, g1.appType);
    }
}