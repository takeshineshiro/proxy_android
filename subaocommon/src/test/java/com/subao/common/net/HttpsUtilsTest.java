package com.subao.common.net;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertNotNull;

public class HttpsUtilsTest {

    private static final String CER = "-----BEGIN CERTIFICATE-----\n" +
            "MIIC0DCCAbigAwIBAgIEV24u4DANBgkqhkiG9w0BAQUFADAqMRQwEgYDVQQLEwt3c2Rhc2hpLmNv\n" +
            "bTESMBAGA1UEAxMJbG9jYWxob3N0MB4XDTE2MDYyNTA3MTIzMloXDTE3MDYyNTA3MTIzMlowKjEU\n" +
            "MBIGA1UECxMLd3NkYXNoaS5jb20xEjAQBgNVBAMTCWxvY2FsaG9zdDCCASIwDQYJKoZIhvcNAQEB\n" +
            "BQADggEPADCCAQoCggEBANNRABiyRdPmL//0F++6/XsxCo6J90U4HFDJ8JFN+/my9O0gRVtKFtNj\n" +
            "GEfXPAvZsIyOwrG/3nwdfGZJAGDfekKchQxYIzhzF68jtYooVOFv3IghW57SHuhUl+ZSwO4bc+m3\n" +
            "TrGceqegEOMyZbZWOwCmlzNwfFa+m4gGAnVjPReThSvjUwstOut+Nx4PEjIqmGoA2CC8yysTkaTm\n" +
            "YDHZoVlXI88LNMFijWZWSJkWp88n4PqBQHE8iMJJhLoc969rBOhqOCRGMo1w/upWi26cvoqphoNx\n" +
            "Jc+lDSsY9t62wW2tBqwAg+Mc2SHrwmVRUNnvX7C/i/glhfklO9KgFYRd7MMCAwEAATANBgkqhkiG\n" +
            "9w0BAQUFAAOCAQEAk9StlQqPjcWhrveDtEkDAnd38UuUuYCM51coAHh/Y0/KK7o+8GSN7CcVcTal\n" +
            "RUIq4/MX65zK1cistiwnH2Uy8li5UCWeijdbD3Fo1K9vgrdB9fKGvCIYRDOpsbq5r4XE7cJF1ZO0\n" +
            "fqj7wWF7x/EdJc2Nz0oISTHSLrkqBWYhXWZBjmUjXKWs8uBEdq0xi8jFXPRAtn3tOOldzKKEf9Kx\n" +
            "sWazUO9rVk/wMJz+tDp7OmYQaKTw+6J+EGyCCknA5sgUMwVG/HVoJ8Nk6A3Vxky4+2kMtpewI/ht\n" +
            "8qGYtGEa/cdHppyfc5lW0sQ85e6Ixw0vrLiwXl0pEuzZkcHq0OGhlw==\n" +
            "-----END CERTIFICATE-----";
	@Test
	public void testCreateSSLParams() throws IOException {
		assertNotNull(HttpsUtils.createSSLParams(null, null, null));
        InputStream stream = new ByteArrayInputStream(CER.getBytes("UTF-8"));
        InputStream[] streams = new ByteArrayInputStream[1];
        streams[0] = stream;
        HttpsUtils.createSSLParams(streams, null, null);

        HttpsUtils.SSLParams sslParams = HttpsUtils.createSSLParams(null, stream, "123");
        assertNotNull(sslParams.socketFactory);
        assertNotNull(sslParams.trustManager);
	}

}
