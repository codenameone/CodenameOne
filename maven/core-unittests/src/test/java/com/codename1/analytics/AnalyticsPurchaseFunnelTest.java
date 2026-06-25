package com.codename1.analytics;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.payment.Purchase;

import static org.junit.jupiter.api.Assertions.*;

/// Verifies the purchase-funnel auto-instrumentation: calling
/// {@link Purchase#purchase(String)} emits a {@code purchase_initiated} event
/// through the template method, regardless of the platform implementation.
class AnalyticsPurchaseFunnelTest extends UITestBase {

    @FormTest
    void purchaseInitiatedFiresThroughTemplateMethod() {
        Analytics.clearProviders();
        Analytics.setConsentMode(ConsentMode.OPT_OUT);
        Analytics.setConsent(null);
        LoggingAnalyticsProvider provider = new LoggingAnalyticsProvider();
        Analytics.addProvider(provider);
        provider.clearLog();

        final boolean[] reachedImpl = {false};
        Purchase p = new Purchase() {
            @Override
            protected void purchaseImpl(String sku) {
                reachedImpl[0] = true;
            }
        };
        p.purchase("pro-upgrade");

        assertTrue(reachedImpl[0], "purchase() must still reach the platform purchaseImpl");
        boolean fired = false;
        for (String e : provider.getLog()) {
            if (e.startsWith("event:purchase_initiated")) {
                fired = true;
                break;
            }
        }
        assertTrue(fired, "purchase() must emit purchase_initiated; log=" + provider.getLog());

        Analytics.clearProviders();
        Analytics.setConsent(null);
    }

    @FormTest
    void purchaseInitiatedDroppedWithoutConsent() {
        Analytics.clearProviders();
        Analytics.setConsentMode(ConsentMode.OPT_IN);
        Analytics.setConsent(null);
        LoggingAnalyticsProvider provider = new LoggingAnalyticsProvider();
        Analytics.addProvider(provider);
        provider.clearLog();

        Purchase p = new Purchase() {
            @Override
            protected void purchaseImpl(String sku) {
            }
        };
        p.purchase("pro-upgrade");

        assertTrue(provider.getLog().isEmpty(),
                "opt-in must drop purchase_initiated before consent; log=" + provider.getLog());

        Analytics.clearProviders();
        Analytics.setConsent(null);
    }
}
