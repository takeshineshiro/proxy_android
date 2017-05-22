package com.subao.common.data;

import android.content.pm.ApplicationInfo;

import com.subao.common.RoboBase;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * SupportGameListTest
 * <p>Created by YinHaiBo on 2017/4/4.</p>
 */
public class SupportGameListTest extends RoboBase {

    private static List<AccelGame> buildAccelGames() {
        int accelGameCount = 5;
        List<AccelGame> accelGameList = new ArrayList<AccelGame>(5);
        for (int i = 0; i < accelGameCount; ++i) {
            AccelGame accelGame = AccelGame.create(
                String.format("label_%d", i),
                i % 3,
                (int) Math.random(),
                null, null, null, null
            );
            accelGameList.add(accelGame);
        }
        return accelGameList;
    }

    public static SupportGameList buildNotEmptySupportGameList() {
        List<AccelGame> accelGameList = buildAccelGames();
        List<InstalledApp.Info> installedAppList = new ArrayList<InstalledApp.Info>(2);
        for (int i = 0; i < 2; ++i) {
            ApplicationInfo applicationInfo = new ApplicationInfo();
            applicationInfo.packageName = "pkg_" + i;
            installedAppList.add(new InstalledApp.Info(
                applicationInfo,
                "label_" + i, false
            ));
        }
        return SupportGameList.build(accelGameList, installedAppList);
    }

    @Test
    public void buildNull() {
        List<AccelGame> accelGameList = buildAccelGames();
        assertNull(SupportGameList.build(accelGameList, null));
        assertNull(null, InstalledApp.getInstalledAppList(getContext()));
    }

    @Test
    public void buildHasInstalledGame() {
        List<AccelGame> accelGameList = buildAccelGames();
        List<InstalledApp.Info> installedAppList = new ArrayList<InstalledApp.Info>(2);
        installedAppList.add(new InstalledApp.Info(
            new ApplicationInfo(),
            "label_1", false
        ));
        SupportGameList supportGameList = SupportGameList.build(accelGameList, installedAppList);
        assertEquals(1, supportGameList.getCount());
        SupportGame supportGame = supportGameList.iterator().next();
        assertEquals("label_1", supportGame.appLabel);
    }

    @Test
    public void emptySupportGameList() {
        SupportGameList supportGameList = new SupportGameList(null);
        assertEquals(0, supportGameList.getCount());
        Iterator<SupportGame> iterator = supportGameList.iterator();
        assertFalse(iterator.hasNext());
        try {
            iterator.remove();
            fail();
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            iterator.next();
        } catch (NoSuchElementException e) {
            // expected
        }
    }

    @Test
    public void buildNoInstalledGame() {
        List<AccelGame> accelGameList = buildAccelGames();
        List<InstalledApp.Info> installedAppList = new ArrayList<InstalledApp.Info>(2);
        installedAppList.add(new InstalledApp.Info(
            new ApplicationInfo(),
            "no match label", false
        ));
        SupportGameList supportGameList = SupportGameList.build(accelGameList, installedAppList);
        assertNull(supportGameList);
    }

    @Test
    public void getPackageNameList() {
        SupportGameList supportGameList = buildNotEmptySupportGameList();
        List<String> packageNameList = supportGameList.getPackageNameList();
        assertEquals(2, packageNameList.size());
        assertEquals("pkg_0", packageNameList.get(0));
        assertEquals("pkg_1", packageNameList.get(1));
        //
        assertNull(new SupportGameList(null).getPackageNameList());
    }
}