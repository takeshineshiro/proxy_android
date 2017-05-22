package com.subao.common.data;

import android.util.JsonWriter;

import com.subao.common.Misc;
import com.subao.common.RoboBase;
import com.subao.common.utils.JsonUtils;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * ServiceConfigTest
 * <p>Created by YinHaiBo on 2016/11/23.</p>
 */
public class ServiceConfigTest extends RoboBase {

    private static String serializeServiceConfig(ServiceConfig serviceConfig) throws IOException {
        StringWriter stringWriter = new StringWriter(2048);
        JsonWriter writer = new JsonWriter(stringWriter);
        writer.beginObject();
        writer.name("unknown_field").nullValue();
        if (serviceConfig.isInitAlwaysFail()) {
            writer.name("init").value("fail");
        }
        JsonUtils.writeString(writer, "url_h5", serviceConfig.getUrlH5());
        JsonUtils.writeObject(writer, "url_portal", serviceConfig.getPortalServiceLocation());
        JsonUtils.writeObject(writer, "url_auth", serviceConfig.getAuthServiceLocation());
        JsonUtils.writeObject(writer, "url_message", serviceConfig.getMessageServiceLocation());
        JsonUtils.writeObject(writer,"url_hr",serviceConfig.getHrServiceLocation()) ;
        if (serviceConfig.getAccelRecommendation() != null) {
            writer.name("accel_recommend").value(serviceConfig.getAccelRecommendation());
        }
        AccelNodesDownloader.NodesInfo nodesInfo = serviceConfig.getNodesInfo();
        if (nodesInfo != null) {
            JsonUtils.writeString(writer, "nodes_info", nodesInfo.dataForJNI);
        }
        if (serviceConfig.getLogLevel() != null) {
            writer.name("log_level").value(serviceConfig.getLogLevel());
        }
        writer.endObject();
        Misc.close(writer);
        return stringWriter.toString();
    }

    private static ServiceConfig createFromTemplate(ServiceConfig template) throws IOException {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.loadFromReader(new StringReader(serializeServiceConfig(template)));
        return serviceConfig;
    }

    @Test
    public void validLogLevel() {
        for (int i = -1; i <= 7; ++i) {
            assertEquals(Math.min(5, Math.max(1, i)), ServiceConfig.validLogLevel(i));
        }
    }

    @Test
    public void loadFromFile() throws IOException {
        ServiceConfig cfg = new ServiceConfig();
        cfg.loadFromFile(null, true);
        cfg.loadFromFile(null, false);
    }

    @Test
    public void initAlwaysFail() throws IOException {
        ServiceConfig template = new ServiceConfig();
        assertFalse(template.isInitAlwaysFail());
        template.initAlwaysFail = true;
        assertTrue(template.isInitAlwaysFail());
        ServiceConfig cfg = createFromTemplate(template);
        assertTrue(cfg.isInitAlwaysFail());
    }

    @Test
    public void urlPortal() throws IOException {
        ServiceConfig template = new ServiceConfig();
        assertNull(template.getPortalServiceLocation());
        template.portalServiceLocation = ServiceLocation.parse("http://www.google.com:2400");
        assertNotNull(template.getPortalServiceLocation());
        ServiceConfig cfg = createFromTemplate(template);
        assertEquals(template.getPortalServiceLocation(), cfg.getPortalServiceLocation());
    }

    @Test
    public void urlH5() throws IOException {
        ServiceConfig template = new ServiceConfig();
        assertNull(template.getUrlH5());
        template.urlH5 = "http://www.google.com:2400";
        assertNotNull(template.getUrlH5());
        ServiceConfig cfg = createFromTemplate(template);
        assertEquals(template.getUrlH5(), cfg.getUrlH5());
    }

    @Test
    public void urlAuth() throws IOException {
        ServiceConfig template = new ServiceConfig();
        assertNull(template.getAuthServiceLocation());
        template.authServiceLocation = ServiceLocation.parse("http://www.google.com:2400");
        assertNotNull(template.getAuthServiceLocation());
        ServiceConfig cfg = createFromTemplate(template);
        assertEquals(template.getAuthServiceLocation(), cfg.getAuthServiceLocation());
    }

    @Test
    public void messageServiceLocation() throws IOException {
        ServiceConfig template = new ServiceConfig();
        assertNull(template.getMessageServiceLocation());
        template.messageServiceLocation = ServiceLocation.parse("http://www.google.com:2400");
        assertNotNull(template.getMessageServiceLocation());
        ServiceConfig cfg = createFromTemplate(template);
        assertEquals(template.getMessageServiceLocation(), cfg.getMessageServiceLocation());
    }

    @Test
    public void hrServiceLocation() throws IOException {
        ServiceConfig template = new ServiceConfig();
        assertNull(template.getHrServiceLocation());
        template.hrServiceLocation = ServiceLocation.parse("http://pre-api.xunyou.mobi");
        assertNotNull(template.getHrServiceLocation());
        ServiceConfig cfg = createFromTemplate(template);
        assertEquals(template.getHrServiceLocation(), cfg.getHrServiceLocation());
    }





    @Test
    public void accelRecommend() throws IOException {
        ServiceConfig template = new ServiceConfig();
        assertNull(template.getAccelRecommendation());
        template.accelRecommendation = 1;
        assertEquals(1, (int) template.getAccelRecommendation());
        ServiceConfig cfg = createFromTemplate(template);
        assertEquals(template.getAccelRecommendation(), cfg.getAccelRecommendation());
    }

    @Test
    public void logLevel() throws IOException {
        ServiceConfig template = new ServiceConfig();
        assertNull(template.getLogLevel());
        for (int i = -1; i <= 7; ++i) {
            template.logLevel = 0;
            ServiceConfig cfg = createFromTemplate(template);
            assertEquals(ServiceConfig.validLogLevel(template.getLogLevel()), (int) cfg.getLogLevel());
        }
    }

    @Test
    public void nodeList() throws IOException {
        ServiceConfig template = new ServiceConfig();
        assertNull(template.getNodesInfo());
        template.nodesInfo = new AccelNodesDownloader.NodesInfo(2, "1:127.0.0.1:cnc,2:192.168.1.1:cmcc,");
        ServiceConfig cfg = createFromTemplate(template);
        assertEquals(cfg.getNodesInfo(), template.nodesInfo);
    }

    @Test
    public void createFile() {
        File dir = new MockFile("the_dir");
        File file = ServiceConfig.createFile(dir, false);
        assertEquals("the_dir", file.getParent());
    }

    public static class MockFile extends File {

        boolean methodCalled_mkdirs;

        public MockFile(String path) {
            super(path);
        }

        @Override
        public boolean mkdirs() {
            return methodCalled_mkdirs = true;
        }
    }


}