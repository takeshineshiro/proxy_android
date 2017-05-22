package com.subao.common.data;

import com.subao.common.Logger;
import com.subao.common.RoboBase;
import com.subao.common.io.Persistent;
import com.subao.common.mock.MockNetTypeDetector;
import com.subao.common.mock.MockPersistent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * PortalMiscConfigDownloaderTest
 * <p>Created by YinHaiBo on 2017/3/1.</p>
 */
public class PortalMiscConfigDownloaderTest extends RoboBase {

    private PortalDataDownloader.Arguments arguments;

    @Before
    public void setUp() {
        Logger.setLoggableChecker(new Logger.LoggableChecker() {
            @Override
            public boolean isLoggable(String tag, int level) {
                return true;
            }
        });
        arguments = new PortalDataDownloader.Arguments("android", "2.0.0", null, new MockNetTypeDetector()) {
            @Override
            public Persistent createPersistent(String filename) {
                return new MockPersistent();
            }
        };
    }

    @After
    public void tearDown() {
        Logger.setLoggableChecker(null);
    }

    @Test
    public void genaral() {
        PortalMiscConfigDownloader downloader = new PortalMiscConfigDownloader(arguments);
        assertEquals("configs/misc", downloader.getUrlPart());
        assertNotNull(downloader.getId());
        //
        assertEquals("er_tg", PortalMiscConfigDownloader.MiscConfig.KEY_EVENT_RATIO_TG);
        assertEquals("er_auth", PortalMiscConfigDownloader.MiscConfig.KEY_EVENT_RATIO_AUTH);
        assertEquals("auth_http", PortalMiscConfigDownloader.MiscConfig.KEY_AUTH_HTTP);
        assertEquals("acc_info_up_proto", PortalMiscConfigDownloader.MiscConfig.KEY_ACCEL_INFO_UP_PROTOCOL);
        assertEquals(10000, PortalMiscConfigDownloader.MiscConfig.MAX_EVENT_RATIO);
        //
        PortalMiscConfigDownloader.MiscConfig miscConfig = new PortalMiscConfigDownloader.MiscConfig();
        assertEquals(100, miscConfig.getEventRateTencent());
        assertEquals(1000, miscConfig.getEventRateAuth());
        assertFalse(miscConfig.doesAuthUseHttp());
        assertNull(miscConfig.getProtocolAccelInfoUpload());
        //
        miscConfig.parse("acc_info_up_proto", "https");
        assertEquals("https", miscConfig.getProtocolAccelInfoUpload());
        miscConfig.parse("auth_http", "1");
        assertTrue(miscConfig.doesAuthUseHttp());
        miscConfig.parse("er_auth", "123");
        assertEquals(123, miscConfig.getEventRateAuth());
        miscConfig.parse("er_tg", "456");
        assertEquals(456, miscConfig.getEventRateTencent());
        miscConfig.parse("er_tg", "non-number");
        assertEquals(456, miscConfig.getEventRateTencent());
        miscConfig.parse("hello", "world");
        //
        assertFalse(PortalMiscConfigDownloader.MiscConfig.calcSpecEventReportAllow(-1));
        assertFalse(PortalMiscConfigDownloader.MiscConfig.calcSpecEventReportAllow(0));
        assertTrue(PortalMiscConfigDownloader.MiscConfig.calcSpecEventReportAllow(10000));
        assertTrue(PortalMiscConfigDownloader.MiscConfig.calcSpecEventReportAllow(10001));
        //
        assertFalse(PortalMiscConfigDownloader.MiscConfig.calcSpecEventReportAllow(500, 501));
        assertTrue(PortalMiscConfigDownloader.MiscConfig.calcSpecEventReportAllow(500, 499));
        //

    }



}