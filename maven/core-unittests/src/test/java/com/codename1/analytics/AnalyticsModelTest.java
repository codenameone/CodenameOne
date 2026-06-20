package com.codename1.analytics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Pure-model tests for the analytics value objects and provider capability
/// reporting. These need no simulator, so they extend nothing.
class AnalyticsModelTest {

    @Test
    void eventBuilderCarriesNameCategoryAndParams() {
        AnalyticsEvent e = AnalyticsEvent.create("purchase")
                .category("commerce")
                .param("sku", "abc-123")
                .param("value", 9.99)
                .timestamp(42L)
                .build();
        assertEquals("purchase", e.getName());
        assertEquals("commerce", e.getCategory());
        assertEquals("abc-123", e.getParameters().get("sku"));
        assertEquals(9.99, ((Number) e.getParameters().get("value")).doubleValue(), 0.0001);
        assertEquals(42L, e.getTimestamp());
    }

    @Test
    void eventParametersAreUnmodifiable() {
        AnalyticsEvent e = AnalyticsEvent.create("x").param("a", "b").build();
        try {
            e.getParameters().put("c", "d");
            fail("parameters must be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
    }

    @Test
    void consentGrantedAndDenied() {
        AnalyticsConsent g = AnalyticsConsent.granted();
        assertTrue(g.isAnalytics());
        assertTrue(g.isCrashReporting());
        assertTrue(g.isPersonalization());
        assertTrue(g.isAdStorage());

        AnalyticsConsent d = AnalyticsConsent.denied();
        assertFalse(d.isAnalytics());
        assertFalse(d.isCrashReporting());
    }

    @Test
    void consentBuilderAndAsBuilderToggleOneCategory() {
        AnalyticsConsent c = AnalyticsConsent.builder()
                .analytics(true).crashReporting(false).build();
        assertTrue(c.isAnalytics());
        assertFalse(c.isCrashReporting());

        AnalyticsConsent c2 = c.asBuilder().crashReporting(true).build();
        assertTrue(c2.isAnalytics());
        assertTrue(c2.isCrashReporting());
    }

    @Test
    void crashReportCarriesThrowableAndCustomKeys() {
        RuntimeException ex = new RuntimeException("boom");
        AnalyticsCrashReport r = AnalyticsCrashReport.create(ex, "boom", true)
                .asBuilder().customKey("screen", "Home").build();
        assertSame(ex, r.getThrowable());
        assertEquals("boom", r.getMessage());
        assertTrue(r.isFatal());
        assertEquals("Home", r.getCustomKeys().get("screen"));
    }

    @Test
    void providerCapabilitiesAreReportedCorrectly() {
        assertTrue(new LoggingAnalyticsProvider().supports(AnalyticsCapability.RAW_EXPORT));

        GoogleAnalyticsProvider ga = new GoogleAnalyticsProvider("G-X", "s");
        assertTrue(ga.supports(AnalyticsCapability.SCREEN_VIEWS));
        assertTrue(ga.supports(AnalyticsCapability.USER_PROPERTIES));
        assertFalse(ga.supports(AnalyticsCapability.RAW_EXPORT));

        MatomoAnalyticsProvider matomo = new MatomoAnalyticsProvider("https://m.example.com", 1);
        assertTrue(matomo.supports(AnalyticsCapability.SCREEN_VIEWS));
        assertFalse(matomo.supports(AnalyticsCapability.USER_PROPERTIES));

        CodenameOneAnalyticsProvider cn1 = new CodenameOneAnalyticsProvider();
        assertTrue(cn1.supports(AnalyticsCapability.RAW_EXPORT));
    }

    @Test
    void abstractProviderDefaultsToNoCapabilities() {
        AnalyticsProvider p = new AbstractAnalyticsProvider() {
            @Override
            public String getName() {
                return "test";
            }
        };
        assertFalse(p.supports(AnalyticsCapability.SCREEN_VIEWS));
        assertEquals("test", p.getName());
    }
}
