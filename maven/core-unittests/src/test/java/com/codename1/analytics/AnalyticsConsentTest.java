package com.codename1.analytics;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import static org.junit.jupiter.api.Assertions.*;

class AnalyticsConsentTest extends UITestBase {

    @FormTest
    void optInDropsEventsUntilConsentGranted() {
        Analytics.clearProviders();
        Analytics.setConsentMode(ConsentMode.OPT_IN);
        Analytics.setConsent(null);
        LoggingAnalyticsProvider provider = new LoggingAnalyticsProvider();
        Analytics.addProvider(provider);
        provider.clearLog();

        Analytics.screen("Home", null);
        Analytics.event(AnalyticsEvent.create("ping").build());
        assertTrue(provider.getLog().isEmpty(), "opt-in must drop events before consent");

        Analytics.setConsent(AnalyticsConsent.granted());
        provider.clearLog();
        Analytics.screen("Home", null);
        assertEquals(1, provider.getLog().size());
        assertEquals("screen:Home", provider.getLog().get(0));

        Analytics.clearProviders();
        Analytics.setConsent(null);
    }

    @FormTest
    void optOutSendsByDefault() {
        Analytics.clearProviders();
        Analytics.setConsentMode(ConsentMode.OPT_OUT);
        Analytics.setConsent(null);
        LoggingAnalyticsProvider provider = new LoggingAnalyticsProvider();
        Analytics.addProvider(provider);
        provider.clearLog();

        Analytics.event(AnalyticsEvent.create("ping").build());
        assertEquals(1, provider.getLog().size());

        Analytics.clearProviders();
        Analytics.setConsent(null);
    }

    @FormTest
    void allGrantsEveryCategoryAndAliasesMatch() {
        AnalyticsConsent all = AnalyticsConsent.all();
        assertTrue(all.isAnalytics());
        assertTrue(all.isCrashReporting());
        assertTrue(all.isPersonalization());
        assertTrue(all.isAdStorage());

        AnalyticsConsent none = AnalyticsConsent.none();
        assertFalse(none.isAnalytics());
        assertFalse(none.isCrashReporting());
        assertFalse(none.isPersonalization());
        assertFalse(none.isAdStorage());

        // granted()/denied() are aliases of all()/none()
        assertEquals(AnalyticsConsent.all().isAnalytics(), AnalyticsConsent.granted().isAnalytics());
        assertEquals(AnalyticsConsent.none().isAnalytics(), AnalyticsConsent.denied().isAnalytics());

        // builder().all() seeds every category so one can be selectively revoked
        AnalyticsConsent allButAds = AnalyticsConsent.builder().all().adStorage(false).build();
        assertTrue(allButAds.isAnalytics());
        assertTrue(allButAds.isCrashReporting());
        assertTrue(allButAds.isPersonalization());
        assertFalse(allButAds.isAdStorage());
    }

    @FormTest
    void crashRequiresCrashConsentCategory() {
        Analytics.clearProviders();
        Analytics.setConsentMode(ConsentMode.OPT_IN);
        Analytics.setConsent(AnalyticsConsent.builder().analytics(true).crashReporting(false).build());
        LoggingAnalyticsProvider provider = new LoggingAnalyticsProvider();
        Analytics.addProvider(provider);
        provider.clearLog();

        Analytics.crash(new RuntimeException("boom"), "boom", true);
        assertTrue(provider.getLog().isEmpty(), "crash must be dropped without crash consent");

        Analytics.screen("Home", null);
        assertEquals(1, provider.getLog().size(), "analytics consent still allows screen views");

        Analytics.clearProviders();
        Analytics.setConsent(null);
    }
}
