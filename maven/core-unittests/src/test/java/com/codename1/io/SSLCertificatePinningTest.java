package com.codename1.io;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.testing.TestCodenameOneImplementation.TestConnection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SSLCertificatePinningTest extends UITestBase {
    private static final String TEST_URL = "https://weblite.ca/tmp/postecho.php";

    @BeforeEach
    void setUpNetwork() {
        implementation.clearConnections();
        implementation.clearQueuedRequests();
        implementation.setConnectionResponseProvider(null);
        implementation.clearSslCertificates();
        implementation.setSslCertificatesSupported(false);
    }

    @AfterEach
    void tearDownNetwork() {
        implementation.clearConnections();
        implementation.clearQueuedRequests();
        implementation.clearSslCertificates();
        implementation.setSslCertificatesSupported(false);
    }

    @FormTest
    void certificatePinningStopsUntrustedRequestBeforeBody() {
        implementation.setSslCertificatesSupported(true);
        implementation.setSslCertificates(TEST_URL, new String[]{
                "SHA-256:trusted-cert",
                "SHA-1:untrusted"
        });

        final boolean[] checkCalled = new boolean[1];
        final boolean[] bodyCalled = new boolean[1];
        final List<String> observedCertificates = new ArrayList<String>();

        ConnectionRequest request = new ConnectionRequest() {
            @Override
            protected void buildRequestBody(OutputStream os) {
                bodyCalled[0] = true;
                try {
                    os.write("Key1=Val1".getBytes("UTF-8"));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            protected void checkSSLCertificates(ConnectionRequest.SSLCertificate[] certificates) {
                checkCalled[0] = true;
                for (SSLCertificate certificate : certificates) {
                    if (certificate == null) {
                        continue;
                    }
                    observedCertificates.add(certificate.getCertificteAlgorithm() + ":" + certificate.getCertificteUniqueKey());
                    if ("untrusted".equals(certificate.getCertificteUniqueKey())) {
                        kill();
                    }
                }
            }
        };

        request.setCheckSSLCertificates(true);
        request.setUrl(TEST_URL);
        request.setPost(true);
        request.setHttpMethod("POST");

        NetworkManager.getInstance().addToQueueAndWait(request);

        assertTrue(checkCalled[0]);
        assertFalse(bodyCalled[0]);
        assertTrue(observedCertificates.contains("SHA-1:untrusted"));

        TestConnection connection = implementation.getConnection(TEST_URL);
        assertNotNull(connection);
        assertEquals(0, connection.getOutputData().length);
    }

    @FormTest
    void certificatePinningAllowsTrustedRequestAndCapturesResponse() {
        implementation.setSslCertificatesSupported(true);
        implementation.setSslCertificates(TEST_URL, new String[]{"RSA:abcdef"});
        implementation.setConnectionResponseProvider(url -> "Post received".getBytes(StandardCharsets.UTF_8));

        final boolean[] checkCalled = new boolean[1];
        final boolean[] bodyCalled = new boolean[1];
        final List<String> observedCertificates = new ArrayList<String>();

        ConnectionRequest request = new ConnectionRequest() {
            @Override
            protected void buildRequestBody(OutputStream os) {
                bodyCalled[0] = true;
                try {
                    os.write("hello=world".getBytes("UTF-8"));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            protected void checkSSLCertificates(ConnectionRequest.SSLCertificate[] certificates) {
                checkCalled[0] = true;
                for (SSLCertificate certificate : certificates) {
                    if (certificate != null) {
                        observedCertificates.add(certificate.getCertificteAlgorithm() + ":" + certificate.getCertificteUniqueKey());
                    }
                }
            }
        };

        request.setCheckSSLCertificates(true);
        request.setUrl(TEST_URL);
        request.setPost(true);
        request.setHttpMethod("POST");

        NetworkManager.getInstance().addToQueueAndWait(request);

        assertTrue(checkCalled[0]);
        assertTrue(bodyCalled[0]);
        assertEquals(200, request.getResponseCode());
        assertEquals("RSA:abcdef", observedCertificates.get(0));

        TestConnection connection = implementation.getConnection(TEST_URL);
        assertNotNull(connection);
        assertEquals("hello=world", new String(connection.getOutputData(), StandardCharsets.UTF_8));
        assertEquals("Post received", new String(request.getResponseData(), StandardCharsets.UTF_8));
    }
}
