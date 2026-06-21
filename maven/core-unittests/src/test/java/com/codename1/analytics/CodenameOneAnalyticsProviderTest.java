package com.codename1.analytics;

import com.codename1.io.ConnectionRequest;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CodenameOneAnalyticsProviderTest extends UITestBase {

    @FormTest
    void flushPostsBatchToCloudEndpoint() {
        implementation.clearQueuedRequests();
        Analytics.clearProviders();
        Analytics.setConsentMode(ConsentMode.OPT_OUT);
        Analytics.setConsent(null);
        CodenameOneAnalyticsProvider provider = new CodenameOneAnalyticsProvider();
        Analytics.addProvider(provider);

        Analytics.screen("Home", "Splash");
        Analytics.event(AnalyticsEvent.create("purchase").param("value", 9.99).build());
        // Nothing is sent until the batch flushes.
        assertEquals(0, implementation.getQueuedRequests().size());

        Analytics.flush();

        List<ConnectionRequest> requests = implementation.getQueuedRequests();
        assertEquals(1, requests.size());
        ConnectionRequest request = requests.get(0);
        assertTrue(request.getUrl().contains("/api/v2/analytics/events"));
        assertTrue(request.isPost());
        String body = request.getRequestBody();
        assertTrue(body.contains("\"consentAnalytics\":true"));
        assertTrue(body.contains("screen_view"));
        assertTrue(body.contains("purchase"));

        Analytics.clearProviders();
        Analytics.setConsent(null);
    }

    @FormTest
    void batchCarriesDeviceSegmentationMetadata() {
        implementation.clearQueuedRequests();
        Analytics.clearProviders();
        Analytics.setConsentMode(ConsentMode.OPT_OUT);
        Analytics.setConsent(null);
        CodenameOneAnalyticsProvider provider = new CodenameOneAnalyticsProvider();
        Analytics.addProvider(provider);

        Analytics.screen("Home", null);
        Analytics.flush();

        String body = implementation.getQueuedRequests().get(0).getRequestBody();
        // The first-party batch must carry the segmentation metadata the
        // console needs: a session id plus the device dimensions. Values are
        // environment-specific, so assert the keys are present and well-formed.
        assertTrue(body.contains("\"sessionId\":\""), "sessionId missing: " + body);
        assertTrue(body.contains("\"deviceModel\":"), "deviceModel missing");
        assertTrue(body.contains("\"deviceManufacturer\":"), "deviceManufacturer missing");
        assertTrue(body.contains("\"formFactor\":\""), "formFactor missing");
        assertTrue(body.contains("\"density\":"), "density missing");
        assertTrue(body.contains("\"screenWidth\":"), "screenWidth missing");
        assertTrue(body.contains("\"screenHeight\":"), "screenHeight missing");
        assertTrue(body.contains("\"network\":"), "network missing");
        assertTrue(body.contains("\"language\":"), "language missing");

        Analytics.clearProviders();
        Analytics.setConsent(null);
    }

    @FormTest
    void sessionIdStableWithinProviderRun() {
        implementation.clearQueuedRequests();
        Analytics.clearProviders();
        Analytics.setConsentMode(ConsentMode.OPT_OUT);
        Analytics.setConsent(null);
        CodenameOneAnalyticsProvider provider = new CodenameOneAnalyticsProvider();
        Analytics.addProvider(provider);

        Analytics.screen("A", null);
        Analytics.flush();
        Analytics.screen("B", null);
        Analytics.flush();

        List<ConnectionRequest> reqs = implementation.getQueuedRequests();
        assertEquals(2, reqs.size());
        String s1 = extractSession(reqs.get(0).getRequestBody());
        String s2 = extractSession(reqs.get(1).getRequestBody());
        assertNotNull(s1);
        assertEquals(s1, s2, "session id must be stable across flushes in one run");

        Analytics.clearProviders();
        Analytics.setConsent(null);
    }

    private static String extractSession(String body) {
        String key = "\"sessionId\":\"";
        int i = body.indexOf(key);
        if (i < 0) {
            return null;
        }
        int start = i + key.length();
        int end = body.indexOf('"', start);
        return end < 0 ? null : body.substring(start, end);
    }

    @FormTest
    void autoFlushesWhenBatchFills() {
        implementation.clearQueuedRequests();
        Analytics.clearProviders();
        Analytics.setConsentMode(ConsentMode.OPT_OUT);
        Analytics.setConsent(null);
        CodenameOneAnalyticsProvider provider = new CodenameOneAnalyticsProvider();
        provider.setBatchSize(2);
        Analytics.addProvider(provider);

        Analytics.screen("A", null);
        assertEquals(0, implementation.getQueuedRequests().size());
        Analytics.screen("B", null);
        assertEquals(1, implementation.getQueuedRequests().size(), "filling the batch must auto-flush");

        Analytics.clearProviders();
        Analytics.setConsent(null);
    }
}
