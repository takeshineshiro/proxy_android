package com.subao.common.data;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * PortalGeneralConfigDownloader_MiscConfigTest
 * <p>Created by YinHaiBo on 2017/2/19.</p>
 */
public class PortalGeneralConfigDownloader_MiscConfigTest {

    @Test
    public void testDefaultFieldValud() {
        assertEquals(10000, PortalMiscConfigDownloader.MiscConfig.MAX_EVENT_RATIO);
        assertEquals("er_tg", PortalMiscConfigDownloader.MiscConfig.KEY_EVENT_RATIO_TG);
        assertEquals("er_auth", PortalMiscConfigDownloader.MiscConfig.KEY_EVENT_RATIO_AUTH);
        assertEquals("auth_http", PortalMiscConfigDownloader.MiscConfig.KEY_AUTH_HTTP);
        //
        PortalMiscConfigDownloader.MiscConfig miscConfig = new PortalMiscConfigDownloader.MiscConfig();
        assertEquals(100, miscConfig.getEventRateTencent());
        assertEquals(1000, miscConfig.getEventRateAuth());
        assertFalse(miscConfig.doesAuthUseHttp());
    }

    @Test
    public void calcSpecEventReportAllow() {
        int[] rate = { -1, 0, 10000, 10001 };
        for (int r : rate) {
            boolean allowed = PortalMiscConfigDownloader.MiscConfig.calcSpecEventReportAllow(r);
            boolean expected = (r >= 10000);
            assertEquals(expected, allowed);
        }
        PortalMiscConfigDownloader.MiscConfig.calcSpecEventReportAllow(5000);
    }

}