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
