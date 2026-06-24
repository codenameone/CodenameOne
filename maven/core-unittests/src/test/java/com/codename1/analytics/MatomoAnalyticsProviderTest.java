package com.codename1.analytics;

import com.codename1.io.ConnectionRequest;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MatomoAnalyticsProviderTest extends UITestBase {

    @FormTest
    void screenViewQueuesMatomoTrackerRequest() {
        implementation.clearQueuedRequests();
        Analytics.clearProviders();
        Analytics.setConsentMode(ConsentMode.OPT_OUT);
        Analytics.setConsent(null);
        Analytics.addProvider(new MatomoAnalyticsProvider("https://matomo.example.com", 7));

        Analytics.screen("Home", null);

        List<ConnectionRequest> requests = implementation.getQueuedRequests();
        assertEquals(1, requests.size());
        ConnectionRequest request = requests.get(0);
        assertTrue(request.getUrl().contains("matomo.php"));
        assertFalse(request.isPost());

        Analytics.clearProviders();
        Analytics.setConsent(null);
    }

    @FormTest
    void eventQueuesMatomoTrackerRequest() {
        implementation.clearQueuedRequests();
        Analytics.clearProviders();
        Analytics.setConsentMode(ConsentMode.OPT_OUT);
        Analytics.setConsent(null);
        Analytics.addProvider(new MatomoAnalyticsProvider("https://matomo.example.com/matomo.php", 7));

        Analytics.event(AnalyticsEvent.create("purchase").category("commerce").param("value", 9.99).build());

        List<ConnectionRequest> requests = implementation.getQueuedRequests();
        assertEquals(1, requests.size());
        assertTrue(requests.get(0).getUrl().contains("matomo.php"));

        Analytics.clearProviders();
        Analytics.setConsent(null);
    }
}
