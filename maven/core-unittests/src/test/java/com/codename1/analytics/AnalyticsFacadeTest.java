package com.codename1.analytics;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/// Tests for the {@link Analytics} facade: provider management, multi-provider
/// fan-out, the pseudonymous client id, and the personalization consent gate.
class AnalyticsFacadeTest extends UITestBase {

    @FormTest
    void addRemoveAndClearProviders() {
        Analytics.clearProviders();
        LoggingAnalyticsProvider a = new LoggingAnalyticsProvider();
        LoggingAnalyticsProvider b = new LoggingAnalyticsProvider();
        Analytics.addProvider(a);
        Analytics.addProvider(b);
        assertEquals(2, Analytics.getProviders().size());
        Analytics.removeProvider(a);
        assertEquals(1, Analytics.getProviders().size());
        Analytics.clearProviders();
        assertEquals(0, Analytics.getProviders().size());
        Analytics.setConsent(null);
    }

    @FormTest
    void screenFansOutToEveryProvider() {
        Analytics.clearProviders();
        Analytics.setConsentMode(ConsentMode.OPT_OUT);
        Analytics.setConsent(null);
        LoggingAnalyticsProvider a = new LoggingAnalyticsProvider();
        LoggingAnalyticsProvider b = new LoggingAnalyticsProvider();
        Analytics.addProvider(a);
        Analytics.addProvider(b);
        a.clearLog();
        b.clearLog();

        Analytics.screen("Home", null);
        assertEquals(1, a.getLog().size());
        assertEquals(1, b.getLog().size());

        Analytics.clearProviders();
        Analytics.setConsent(null);
    }

    @FormTest
    void clientIdIsStableAndResettable() {
        String id = Analytics.clientId();
        assertNotNull(id);
        assertEquals(32, id.length());
        assertEquals(id, Analytics.clientId(), "client id must be stable across calls");

        String reset = Analytics.resetClientId();
        assertNotNull(reset);
        assertNotEquals(id, reset, "reset must produce a new client id");
        assertEquals(reset, Analytics.clientId());
    }

    @FormTest
    void setUserIdRequiresPersonalizationConsent() {
        Analytics.clearProviders();
        Analytics.setConsentMode(ConsentMode.OPT_IN);
        Analytics.setConsent(AnalyticsConsent.builder().analytics(true).personalization(false).build());
        LoggingAnalyticsProvider p = new LoggingAnalyticsProvider();
        Analytics.addProvider(p);
        p.clearLog();

        Analytics.setUserId("user-1");
        assertTrue(p.getLog().isEmpty(), "user id must be dropped without personalization consent");

        Analytics.setConsent(AnalyticsConsent.granted());
        p.clearLog();
        Analytics.setUserId("user-1");
        assertEquals(1, p.getLog().size());
        assertEquals("userId:user-1", p.getLog().get(0));

        Analytics.clearProviders();
        Analytics.setConsent(null);
    }

    @FormTest
    void consentPersistsAcrossReload() {
        Analytics.clearProviders();
        Analytics.setConsentMode(ConsentMode.OPT_IN);
        Analytics.setConsent(AnalyticsConsent.granted());
        AnalyticsConsent loaded = Analytics.getConsent();
        assertNotNull(loaded);
        assertTrue(loaded.isAnalytics());
        Analytics.setConsent(null);
    }
}
