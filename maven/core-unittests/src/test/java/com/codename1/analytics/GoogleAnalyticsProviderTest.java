package com.codename1.analytics;

import com.codename1.io.ConnectionRequest;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GoogleAnalyticsProviderTest extends UITestBase {

    @FormTest
    void screenViewPostsGa4Payload() {
        implementation.clearQueuedRequests();
        Analytics.clearProviders();
        Analytics.setConsentMode(ConsentMode.OPT_OUT);
        Analytics.setConsent(null);
        Analytics.addProvider(new GoogleAnalyticsProvider("G-TEST", "secret"));

        Analytics.screen("Home", "Splash");

        List<ConnectionRequest> requests = implementation.getQueuedRequests();
        assertEquals(1, requests.size());
        ConnectionRequest request = requests.get(0);
        assertTrue(request.getUrl().contains("google-analytics"));
        assertTrue(request.getUrl().contains("measurement_id=G-TEST"));
        assertTrue(request.isPost());
        String body = request.getRequestBody();
        assertNotNull(body);
        assertTrue(body.contains("\"client_id\""));
        assertTrue(body.contains("screen_view"));
        assertTrue(body.contains("Home"));

        Analytics.clearProviders();
        Analytics.setConsent(null);
    }

    @FormTest
    void eventIncludesUserPropertiesAndSanitizedName() {
        implementation.clearQueuedRequests();
        Analytics.clearProviders();
        Analytics.setConsentMode(ConsentMode.OPT_OUT);
        Analytics.setConsent(null);
        GoogleAnalyticsProvider provider = new GoogleAnalyticsProvider("G-TEST", "secret");
        Analytics.addProvider(provider);

        Analytics.setUserProperty("plan", "pro");
        Analytics.event(AnalyticsEvent.create("add to cart").param("value", 5).build());

        List<ConnectionRequest> requests = implementation.getQueuedRequests();
        assertEquals(1, requests.size());
        String body = requests.get(0).getRequestBody();
        assertTrue(body.contains("user_properties"));
        assertTrue(body.contains("\"plan\""));
        // "add to cart" must be sanitised to a GA4-legal event name.
        assertTrue(body.contains("add_to_cart"));

        Analytics.clearProviders();
        Analytics.setConsent(null);
    }

    @FormTest
    void crashPostsAppException() {
        implementation.clearQueuedRequests();
        Analytics.clearProviders();
        Analytics.setConsentMode(ConsentMode.OPT_OUT);
        Analytics.setConsent(null);
        Analytics.addProvider(new GoogleAnalyticsProvider("G-TEST", "secret"));

        Analytics.crash(new RuntimeException("boom"), "boom", true);

        List<ConnectionRequest> requests = implementation.getQueuedRequests();
        assertEquals(1, requests.size());
        String body = requests.get(0).getRequestBody();
        assertTrue(body.contains("app_exception"));
        assertTrue(body.contains("\"fatal\":true"));

        Analytics.clearProviders();
        Analytics.setConsent(null);
    }
}
