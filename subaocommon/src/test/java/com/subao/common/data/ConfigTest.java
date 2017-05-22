package com.subao.common.data;

import com.subao.common.Logger;
import com.subao.common.RoboBase;
import com.subao.common.io.Persistent;
import com.subao.common.io.PersistentFactory;
import com.subao.common.mock.MockPersistent;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.io.File;

import static org.junit.Assert.assertEquals;

/**
 * ConfigTest
 * <p>Created by YinHaiBo on 2016/12/22.</p>
 */
@org.robolectric.annotation.Config(shadows = {ConfigTest.ShadowPersistentFactory.class})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ConfigTest extends RoboBase {

    @Before
    public void setUp() {
        Logger.setLoggableChecker(new Logger.LoggableChecker() {
            @Override
            public boolean isLoggable(String tag, int level) {
                return true;
            }
        });
    }

    @After
    public void tearDown() {
        Logger.setLoggableChecker(null);
    }

    private static void sleep() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test()
    public void test1_load() throws Exception {
        Config config = Config.getInstance();
        //
        assertEquals(0, config.getDayReportStartMessage());
        int dayReportStartMessage = 1234;
        config.setDayReportStartMessage(dayReportStartMessage);
        assertEquals(dayReportStartMessage, config.getDayReportStartMessage());
        //
        sleep();
        MockPersistent mockPersistent = (MockPersistent) config.getPersistent();
        byte[] savedContent = mockPersistent.getData();
        mockPersistent.delete();
        config.setDayReportStartMessage(3);
        //
        sleep();
        String json = new String(savedContent);
        json = "{\"test\":null," + json.substring(1);
        mockPersistent.setData(json.getBytes());
        config.load();
        assertEquals(dayReportStartMessage, config.getDayReportStartMessage());
        //
        // test save exception
        //
        mockPersistent.ioExceptionWhenOpenOutput = true;
        config.setDayReportStartMessage(1);
        sleep();
        mockPersistent.ioExceptionWhenOpenOutput = false;
        mockPersistent.runtimeExceptionWhenOpenOutput = true;
        config.setDayReportStartMessage(1);
        sleep();
    }

    @Test
    public void test2_loadException() {
        Config config = Config.getInstance();
        MockPersistent mockPersistent = (MockPersistent) config.getPersistent();
        mockPersistent.ioExceptionWhenOpenInput = true;
        config.load();
        mockPersistent.ioExceptionWhenOpenInput = false;
        mockPersistent.runtimeExceptionWhenOpenInput = true;
        mockPersistent.setData("{}".getBytes());
        config.load();
    }

    @Implements(value = PersistentFactory.class, isInAndroidSdk = false)
    public static class ShadowPersistentFactory {

        @Implementation
        public static Persistent createByFile(File file) {
            return new MockPersistent();
        }
    }
}