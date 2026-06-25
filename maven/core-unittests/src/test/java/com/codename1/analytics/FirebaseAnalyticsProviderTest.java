package com.codename1.analytics;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import static org.junit.jupiter.api.Assertions.*;

/// In the simulator there is no native Firebase peer, so the provider must
/// degrade to a no-op: it accepts every call without error and queues no
/// network requests.
class FirebaseAnalyticsProviderTest extends UITestBase {

    @FormTest
    void degradesToNoOpWithoutNativePeer() {
        implementation.clearQueuedRequests();
        Analytics.clearProviders();
        Analytics.setConsentMode(ConsentMode.OPT_OUT);
        Analytics.setConsent(null);
        FirebaseAnalyticsProvider provider = new FirebaseAnalyticsProvider();
        Analytics.addProvider(provider);

        Analytics.screen("Home", null);
        Analytics.event(AnalyticsEvent.create("purchase").param("value", 1).build());
        Analytics.setUserId("u1");
        Analytics.setUserProperty("plan", "pro");
        Analytics.crash(new RuntimeException("boom"), "boom", true);
        Analytics.flush();

        assertEquals(0, implementation.getQueuedRequests().size(),
                "firebase provider must not queue HTTP requests");
        assertTrue(provider.supports(AnalyticsCapability.CRASH_REPORTING));

        Analytics.clearProviders();
        Analytics.setConsent(null);
    }
}
