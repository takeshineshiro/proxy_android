package com.subao.common.data;

import com.subao.common.Logger;
import com.subao.common.RoboBase;
import com.subao.common.io.Persistent;
import com.subao.common.mock.MockNetTypeDetector;
import com.subao.common.mock.MockPersistent;
import com.subao.common.utils.StringUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import okhttp3.mockwebserver.MockWebServer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * PortalScriptDownloaderTest
 * <p>Created by YinHaiBo on 2017/1/22.</p>
 */
public class PortalScriptDownloaderTest extends RoboBase {

    private static final String VERSION = "2.0";
    private static final long EXPIRE_TIME = 1234L;
    private static final String CLIENT_TYPE = "clientType";

    private MockPersistent mockPersistent;
    private MockNetTypeDetector mockNetTypeDetector;
    private MockWebServer mockWebServer;
    private PortalDataDownloader.Arguments arguments;

    private PortalScriptDownloader downloader;

    @Before
    public void setUp() throws IOException {
        Logger.setLoggableChecker(new Logger.LoggableChecker() {
            @Override
            public boolean isLoggable(String tag, int level) {
                return true;
            }
        });
        mockPersistent = new MockPersistent();
        mockNetTypeDetector = new MockNetTypeDetector();
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        arguments = new PortalDataDownloader.Arguments(CLIENT_TYPE, VERSION,
            new ServiceLocation("http", mockWebServer.getHostName(), mockWebServer.getPort()),
            mockNetTypeDetector
        ) {
            @Override
            public Persistent createPersistent(String filename) {
                return mockPersistent;
            }
        };
        downloader = new PortalScriptDownloader(arguments);
    }

    @After
    public void tearDown() throws IOException {
        downloader = null;
        mockWebServer.shutdown();
        mockWebServer = null;
        mockPersistent = null;
        mockNetTypeDetector = null;
        arguments = null;
        Logger.setLoggableChecker(null);
    }

    @Test
    public void misc() {
        assertEquals("scripts", downloader.getId());
        assertEquals("scripts/" + VERSION, downloader.getUrlPart());
        assertEquals("*", downloader.getHttpAcceptType());
    }

    @Test
    public void checkDownloadData() throws NoSuchAlgorithmException {
        byte[] script = "Hello, world".getBytes();
        String md5 = StringUtils.toHexString(MessageDigest.getInstance("MD5").digest(script), false);
        md5 = "\"" + md5 + "\"";
        assertFalse(downloader.checkDownloadData(null));
        assertFalse(downloader.isScriptValid(null));
        PortalDataEx portalData = new PortalDataEx(md5, EXPIRE_TIME, VERSION, null);
        assertFalse(downloader.checkDownloadData(portalData));
        //
        portalData = new PortalDataEx(md5, EXPIRE_TIME, VERSION, script);
        assertTrue(downloader.checkDownloadData(portalData));
        portalData = new PortalDataEx(md5, EXPIRE_TIME, VERSION + "_", script);
        assertFalse(downloader.checkDownloadData(portalData));
        portalData = new PortalDataEx(md5.substring(0, 30), EXPIRE_TIME, VERSION, script);
        assertFalse(downloader.checkDownloadData(portalData));
    }

//    @Test
//    @Config(shadows = {ShadowMessageDigest.class})
//    public void v() {
//        byte[] script = "Hello, world".getBytes();
//        MD5Digest digest = new MD5Digest();
//        digest.update(script, 0, script.length);
//        byte[] buf = new byte[digest.getDigestSize()];
//        digest.doFinal(buf, 0);
//        String md5 = StringUtils.toHexString(buf, false);
//        PortalDataEx portalData = new PortalDataEx(md5, VERSION, script);
//        assertFalse(downloader.checkDownloadData(portalData));
//    }
//
//    @Implements(value = MessageDigest.class)
//    public static class ShadowMessageDigest {
//        @Implementation
//        public static MessageDigest getInstance(String algorithm) throws NoSuchAlgorithmException {
//            throw new NoSuchAlgorithmException();
//        }
//    }

}