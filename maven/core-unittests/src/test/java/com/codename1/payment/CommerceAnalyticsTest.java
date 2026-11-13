/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.payment;

import com.codename1.analytics.Analytics;
import com.codename1.analytics.AnalyticsConsent;
import com.codename1.analytics.AnalyticsEvent;
import com.codename1.analytics.invite.InviteAttribution;
import com.codename1.analytics.invite.InviteListener;
import com.codename1.analytics.invite.Invites;
import com.codename1.io.JSONParser;
import com.codename1.junit.FormTest;
import com.codename1.payment.paywall.PaywallView;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BoxLayout;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/// Commerce joined to analytics and invites: the client id in requests, the
/// events of a paywall, the context of a purchase, experiment exposure, and
/// the refresh an invite attribution causes.
class CommerceAnalyticsTest extends CommerceTestBase {
    private static final String OFFERINGS =
            "{\"accepted\":true,\"currentOfferingId\":\"main\","
                    + "\"experiment\":{\"id\":\"exp1\",\"variant\":\"B\"},"
                    + "\"offerings\":[{\"id\":\"main\",\"packages\":["
                    + "{\"id\":\"forever\",\"type\":\"LIFETIME\",\"productId\":\"pro_forever\"},"
                    + "{\"id\":\"annual\",\"type\":\"ANNUAL\",\"productId\":\"pro_annual\",\"period\":\"P1Y\"}]},"
                    + "{\"id\":\"other\",\"packages\":["
                    + "{\"id\":\"monthly\",\"type\":\"MONTHLY\",\"productId\":\"pro_monthly\",\"period\":\"P1M\"}]}]}";

    private static final String REWARDED_CUSTOMER =
            "{\"accepted\":true,\"degraded\":false,\"serverTime\":1700000000000,\"appUserId\":\"u1\","
                    + "\"activeSubscriptions\":[],\"entitlements\":{},"
                    + "\"rewards\":[{\"currencyCode\":\"coins\",\"amount\":50,\"expiresDate\":0,"
                    + "\"reason\":\"invite_purchase\"},"
                    + "{\"entitlementId\":\"pro\",\"expiresDate\":1900000000000,\"reason\":\"invited\"},"
                    + "\"not a reward\"]}";

    /// The body of an `/events` request made now.
    private String eventsBody() {
        final CommerceManager cm = CommerceManager.getInstance();
        final String[] body = new String[1];
        Runnable r = new Runnable() {
            @Override
            public void run() {
                cm.logEvent("IMPRESSION", "main", "pw1", null);
                body[0] = cm.drainEvents();
            }
        };
        if (Display.getInstance().isEdt()) {
            r.run();
        } else {
            Display.getInstance().callSerially(r);
            flushSerialCalls();
        }
        assertNotNull(body[0]);
        return body[0];
    }

    @FormTest
    void theClientIdTravelsWithRequestsAndStopsAfterOptOut() {
        // No provider: the application does not use analytics, and no id is
        // made for it.
        assertNull(CommerceManager.analyticsClientId());
        assertFalse(eventsBody().contains("analyticsClientId"));

        analyticsOn();
        String id = Analytics.clientId();
        assertEquals(id, CommerceManager.analyticsClientId());
        assertTrue(eventsBody().contains("\"analyticsClientId\":\"" + id + "\""));

        Analytics.setConsent(AnalyticsConsent.denied());
        assertNull(CommerceManager.analyticsClientId());
        assertFalse(eventsBody().contains("analyticsClientId"));

        Analytics.setConsent(null);
        assertFalse(eventsBody().contains("analyticsClientId"), "opt-in with no choice on record sends nothing");
    }

    @FormTest
    void aPaywallReportsItsEventsWithPlacementAndExperiment() {
        AnalyticsRecorder recorder = analyticsOn();
        CommerceManager cm = CommerceManager.getInstance();
        mock("/offerings", OFFERINGS);
        Offerings offerings = cm.getOfferings("onboarding");

        assertEquals(1, recorder.count("experiment_exposure"));
        assertEquals("commerce", recorder.last("experiment_exposure").getCategory());
        assertEquals("exp1", recorder.param("experiment_exposure", "experiment"));
        assertEquals("B", recorder.param("experiment_exposure", "variant"));
        mock("/offerings", OFFERINGS);
        cm.getOfferings("onboarding");
        assertEquals(1, recorder.count("experiment_exposure"), "an exposure is reported once");
        mock("/offerings", OFFERINGS.replace("\"variant\":\"B\"", "\"variant\":\"C\""));
        offerings = cm.getOfferings("onboarding");
        assertEquals(2, recorder.count("experiment_exposure"), "another variant is another exposure");
        assertEquals("C", recorder.param("experiment_exposure", "variant"));

        PaywallView view = new PaywallView(offerings.getCurrent());
        Form form = new Form("Paywall", BoxLayout.y());
        form.add(view);
        form.show();
        flushSerialCalls();
        assertEquals(1, recorder.count("paywall_view"));
        AnalyticsEvent shown = recorder.last("paywall_view");
        assertEquals("commerce", shown.getCategory());
        assertEquals("main", shown.getParameters().get("offering"));
        assertEquals("onboarding", shown.getParameters().get("placement"));
        assertEquals("exp1", shown.getParameters().get("experiment"));
        assertEquals("C", shown.getParameters().get("variant"));
        assertFalse(shown.getParameters().containsKey("package"));

        view.selectPackage("annual");
        assertEquals(1, recorder.count("paywall_package_selected"));
        assertEquals("annual", recorder.param("paywall_package_selected", "package"));
        assertEquals("pro_annual", recorder.param("paywall_package_selected", "product"));
        assertEquals("main", recorder.param("paywall_package_selected", "offering"));
        assertEquals("C", recorder.param("paywall_package_selected", "variant"));

        view.restore();
        assertEquals(1, recorder.count("paywall_restore"));
        assertEquals("main", recorder.param("paywall_restore", "offering"));

        view.close();
        assertEquals(1, recorder.count("paywall_close"));
        assertEquals("onboarding", recorder.param("paywall_close", "placement"));

        // The experiment chose the current offering and labels no other.
        PaywallView other = new PaywallView(offerings.getOffering("other"));
        other.selectPackage("monthly");
        assertEquals("other", recorder.param("paywall_package_selected", "offering"));
        assertNull(recorder.param("paywall_package_selected", "experiment"));
        assertEquals("onboarding", recorder.param("paywall_package_selected", "placement"));
    }

    @FormTest
    void withNoProviderNothingIsReportedAndNoExposureIsSpent() {
        CommerceManager cm = CommerceManager.getInstance();
        mock("/offerings", OFFERINGS);
        Offerings offerings = cm.getOfferings();
        new PaywallView(offerings.getCurrent()).selectPackage("annual");
        cm.analyticsEvent(null, null, null, null, null, null);

        // The exposure nobody could hear is reported once somebody can.
        AnalyticsRecorder recorder = analyticsOn();
        mock("/offerings", OFFERINGS);
        cm.getOfferings();
        assertEquals(1, recorder.count("experiment_exposure"));
    }

    @FormTest
    void thePurchaseEventsCarryThePackageAndTheContextIsUsedOnce() {
        implementation.setInAppPurchase(new Purchase() {
            @Override
            protected void purchaseImpl(String sku) {
                storeCalls.add("purchase:" + sku);
            }
        });
        AnalyticsRecorder recorder = analyticsOn();
        CommerceManager cm = CommerceManager.getInstance();
        mock("/offerings", OFFERINGS);
        CommercePackage forever = cm.getOfferings("settings").getCurrent().getPackage("forever");

        cm.purchase(forever);
        assertEquals(1, storeCalls.size());
        assertEquals(1, recorder.count("purchase_initiated"));
        assertEquals("pro_forever", recorder.param("purchase_initiated", "sku"));
        assertEquals("main", recorder.param("purchase_initiated", "offering"));
        assertEquals("forever", recorder.param("purchase_initiated", "package"));
        assertEquals("pro_forever", recorder.param("purchase_initiated", "product"));
        assertEquals("settings", recorder.param("purchase_initiated", "placement"));
        assertEquals("exp1", recorder.param("purchase_initiated", "experiment"));
        assertEquals("B", recorder.param("purchase_initiated", "variant"));

        // The receipt of another product leaves the context where it is.
        Purchase.postReceipt(Receipt.STORE_CODE_ITUNES, "pro_other", "t0", 0, "data");
        assertNull(recorder.param("purchase", "offering"));
        Purchase.postReceipt(Receipt.STORE_CODE_ITUNES, "pro_forever", "t1", 0, "data");
        assertEquals(2, recorder.count("purchase"));
        assertEquals("commerce", recorder.last("purchase").getCategory());
        assertEquals("main", recorder.param("purchase", "offering"));
        assertEquals("forever", recorder.param("purchase", "package"));
        assertEquals("B", recorder.param("purchase", "variant"));

        // Used up: a later receipt of the same product is not labelled.
        Purchase.postReceipt(Receipt.STORE_CODE_ITUNES, "pro_forever", "t2", 0, "data");
        assertEquals("pro_forever", recorder.param("purchase", "sku"));
        assertNull(recorder.param("purchase", "offering"));

        // And a purchase nobody completed lapses.
        cm.purchase(forever);
        CommerceManager.agePurchaseContextForTests(11L * 60L * 1000L);
        Purchase.postReceipt(Receipt.STORE_CODE_ITUNES, "pro_forever", "t3", 0, "data");
        assertNull(recorder.param("purchase", "offering"));
    }

    @FormTest
    void aWebPurchaseReportsItsOwnStart() {
        AnalyticsRecorder recorder = analyticsOn();
        CommerceManager cm = CommerceManager.getInstance();
        mock("/offerings", OFFERINGS);
        CommercePackage annual = cm.getOfferings().getCurrent().getPackage("annual");
        mock("/web-purchase", "{\"accepted\":false}");
        assertFalse(cm.purchaseOnWeb(annual));
        assertEquals(0, recorder.count("purchase_initiated"));
        mock("/web-purchase", "{\"accepted\":true,\"url\":\"https://pay.test/c/1\"}");
        assertTrue(cm.purchaseOnWeb(annual));
        assertEquals(1, recorder.count("purchase_initiated"));
        assertEquals("web", recorder.param("purchase_initiated", "store"));
        assertEquals("pro_annual", recorder.param("purchase_initiated", "sku"));
        assertEquals("annual", recorder.param("purchase_initiated", "package"));
        assertEquals("exp1", recorder.param("purchase_initiated", "experiment"));
    }

    @FormTest
    void anInviteAttributionRefreshesTheCustomerAndLeavesTheListenerAlone() throws Exception {
        InviteListener own = new InviteListener() {
            @Override
            public void inviteReceived(InviteAttribution attribution) {
                // the application's
            }

            @Override
            public void attributionUnavailable(String reason) {
                // the application's
            }
        };
        try {
            Invites.setInviteListener(own);
            CommerceManager cm = CommerceManager.getInstance();
            cm.setAppUserId("u1");
            // First use registers for the attribution; nothing is known yet.
            assertTrue(cm.getCustomerInfo().getReferralRewards().isEmpty());
            mock("/customer", REWARDED_CUSTOMER);

            Analytics.attributionAvailable(true);
            // The refresh runs on a background thread; the EDT keeps
            // dispatching while this waits for it.
            long end = System.currentTimeMillis() + 3000L;
            while (cm.getCustomerInfo().getReferralRewards().isEmpty() && System.currentTimeMillis() < end) {
                Display.getInstance().invokeAndBlock(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                });
            }
            assertEquals(2, cm.getCustomerInfo().getReferralRewards().size(),
                    "the reward the cloud granted arrives without the application asking");
            assertSame(own, Invites.getInviteListener());
        } finally {
            Invites.setInviteListener(null);
        }
    }

    @FormTest
    void referralRewardsAreParsedStoredAndOptional() throws Exception {
        CustomerInfo info = new CustomerInfo(JSONParser.parseJSON(REWARDED_CUSTOMER));
        List<ReferralReward> rewards = info.getReferralRewards();
        assertEquals(2, rewards.size());
        assertTrue(rewards.get(0).isCurrency());
        assertEquals("coins", rewards.get(0).getCurrencyCode());
        assertNull(rewards.get(0).getEntitlementId());
        assertEquals(50L, rewards.get(0).getAmount());
        assertEquals(0L, rewards.get(0).getExpiresDate());
        assertEquals("invite_purchase", rewards.get(0).getReason());
        assertFalse(rewards.get(1).isCurrency());
        assertEquals("pro", rewards.get(1).getEntitlementId());
        assertEquals(1900000000000L, rewards.get(1).getExpiresDate());
        assertNotNull(rewards.get(1).toString());

        // The list is the caller's, and the stored form gives it back.
        rewards.clear();
        assertEquals(2, info.getReferralRewards().size());
        CustomerInfo again = new CustomerInfo(info.toMap());
        assertEquals(2, again.getReferralRewards().size());
        assertTrue(info.sameState(again));

        assertTrue(new CustomerInfo(null).getReferralRewards().isEmpty());
        assertTrue(new CustomerInfo(JSONParser.parseJSON("{\"appUserId\":\"u1\",\"rewards\":\"none\"}"))
                .getReferralRewards().isEmpty());
        assertFalse(new CustomerInfo(null).toMap().containsKey("rewards"));
    }
}
