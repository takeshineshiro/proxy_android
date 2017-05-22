package com.subao.common.data;

import com.subao.common.RoboBase;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * AccelGameMapTest
 * <p>Created by YinHaiBo on 2017/4/4.</p>
 */
public class AccelGameMapTest extends RoboBase {

    private final static String TEST_PACKAGE_NAME = "com.tencent.tmgp.sgame";
    private final static String TEST_APP_LABEL = "王者荣耀";

    @Test
    public void findGame() {
        List<AccelGame> accelGameList = new ArrayList<AccelGame>(5);
        accelGameList.add(AccelGame.create(TEST_APP_LABEL, 0, 0xffff, null, null, null, null));
        accelGameList.add(AccelGame.create("ABC", 0, 0xffff, null, null, null, null));
        accelGameList.add(AccelGame.create("he", 0, 0, null, null, null, null));
        accelGameList.add(AccelGame.create("Hello", 0, 0, null, null, null, null));
        accelGameList.add(AccelGame.create("12", 0, 0, null, null, null, null));
        //
        AccelGameMap map = new AccelGameMap(accelGameList);
        assertNull(map.findAccelGame(TEST_PACKAGE_NAME, null));
        assertNull(map.findAccelGame(TEST_PACKAGE_NAME, "掌上英雄联盟")); // Bad App Label
        assertNull(map.findAccelGame("com.kugou.android", TEST_APP_LABEL)); // Bad Package Name
        AccelGame found = map.findAccelGame(TEST_PACKAGE_NAME, TEST_APP_LABEL);
        assertEquals(TEST_APP_LABEL, found.appLabel);
        found = map.findAccelGame(null, "ABC");
        assertNotNull(found);
        assertNull(map.findAccelGame(null, "AB"));
        // 模糊匹配
        assertNotNull(map.findAccelGame(TEST_PACKAGE_NAME, "helloWorld"));
        // 模糊匹配，但包含Bad Word
        assertNull(map.findAccelGame(TEST_PACKAGE_NAME, "hello-攻略"));
        // 小于3个字符的，不进行模糊匹配
        assertNull(map.findAccelGame("Some time", "Some time"));
    }

    @Test
    public void emptyAccelGameList() {
        AccelGameMap map = new AccelGameMap(null);
        assertNull(map.findAccelGame(TEST_PACKAGE_NAME, TEST_APP_LABEL));
    }

}