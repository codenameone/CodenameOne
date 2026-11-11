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

import com.codename1.junit.FormTest;
import com.codename1.ui.Display;

import static org.junit.jupiter.api.Assertions.*;

class CommerceOfferingsTest extends CommerceTestBase {
    static final String OFFERINGS =
            "{\"accepted\":true,\"currentOfferingId\":\"main\","
                    + "\"experiment\":{\"id\":\"exp1\",\"variant\":\"B\"},"
                    + "\"offerings\":[{\"id\":\"main\",\"description\":\"Main offer\","
                    + "\"metadata\":{\"theme\":\"gold\",\"tags\":[\"a\",\"b\"]},"
                    + "\"paywall\":{\"version\":1,\"id\":\"pw1\",\"root\":{\"type\":\"stack\"}},"
                    + "\"packages\":["
                    + "{\"id\":\"monthly\",\"type\":\"MONTHLY\",\"productId\":\"pro_monthly\",\"period\":\"P1M\","
                    + "\"trialPeriod\":\"P7D\",\"introPrice\":\"0.99\",\"introPeriod\":\"P1M\",\"eligibleForIntro\":true},"
                    + "{\"id\":\"annual\",\"type\":\"annual\",\"productId\":\"pro_annual\",\"period\":\"P1Y\","
                    + "\"eligibleForIntro\":false},"
                    + "{\"id\":\"weekly\",\"type\":\"WEEKLY\",\"productId\":\"pro_weekly\",\"period\":\"P1W\"},"
                    + "{\"id\":\"forever\",\"type\":\"LIFETIME\",\"productId\":\"pro_forever\"},"
                    + "{\"id\":\"coins\",\"type\":\"CUSTOM\",\"productId\":\"coins_100\"},"
                    + "{\"id\":\"odd\",\"type\":\"CUSTOM\",\"productId\":\"pro_odd\",\"period\":\"P10D\"}]},"
                    + "{\"id\":\"sale\",\"description\":null,\"metadata\":{},\"paywall\":null,\"packages\":[]}]}";

    @FormTest
    void parsesOfferingsAndLooksPackagesUp() throws Exception {
        Offerings all = offerings(OFFERINGS);
        assertFalse(all.isEmpty());
        assertEquals(2, all.getAll().size());
        assertEquals("exp1", all.getExperimentId());
        assertEquals("B", all.getExperimentVariant());
        Offering main = all.getCurrent();
        assertSame(main, all.getOffering("main"));
        assertNull(all.getOffering("missing"));
        assertNull(all.getOffering(null));
        assertEquals("Main offer", main.getDescription());
        assertEquals("gold", main.getMetadata().get("theme"));
        assertEquals(6, main.getPackages().size());
        assertEquals("pw1", main.getPaywall().get("id"));
        // What a caller does to the answer does not reach the offering.
        main.getPaywall().clear();
        main.getPackages().clear();
        main.getMetadata().clear();
        assertEquals("pw1", main.getPaywall().get("id"));
        assertEquals(6, main.getPackages().size());
        assertEquals("gold", main.getMetadata().get("theme"));

        CommercePackage monthly = main.getMonthly();
        assertEquals("monthly", monthly.getId());
        assertEquals(CommercePackage.TYPE_MONTHLY, monthly.getType());
        assertEquals("pro_monthly", monthly.getProductId());
        assertEquals("P1M", monthly.getPeriod());
        assertEquals("P7D", monthly.getTrialPeriod());
        assertEquals("0.99", monthly.getIntroPrice());
        assertEquals("P1M", monthly.getIntroPeriod());
        assertTrue(monthly.isEligibleForIntro());
        assertNull(monthly.getProduct());
        assertTrue(monthly.isSubscription());

        // A type is a protocol token: it matches whatever its case.
        CommercePackage annual = main.getAnnual();
        assertEquals("annual", annual.getId());
        assertFalse(annual.isEligibleForIntro());
        assertNull(annual.getTrialPeriod());
        assertEquals("weekly", main.getWeekly().getId());
        assertEquals("forever", main.getLifetime().getId());
        assertFalse(main.getLifetime().isSubscription());
        assertFalse(main.getPackage("coins").isSubscription());
        assertTrue(main.getPackage("odd").isSubscription());
        assertNull(main.getPackage("missing"));

        Offering sale = all.getOffering("sale");
        assertNull(sale.getPaywall());
        assertNull(sale.getMonthly());
        assertTrue(sale.getPackages().isEmpty());

        Offerings empty = new Offerings(null);
        assertTrue(empty.isEmpty());
        assertNull(empty.getCurrent());
        assertNull(empty.getExperimentId());
    }

    @FormTest
    void fetchesOfferingsAndAttachesStoreProducts() {
        implementation.setInAppPurchase(new Purchase() {
            @Override
            public boolean isItemListingSupported() {
                return true;
            }

            @Override
            public Product[] getProducts(String[] skus) {
                Product p = new Product();
                p.setSku("pro_annual");
                p.setLocalizedPrice("$49.99");
                return new Product[] {p, null};
            }
        });
        mock("/offerings", OFFERINGS);
        Offerings all = CommerceManager.getInstance().getOfferings("onboarding");
        Offering main = all.getCurrent();
        assertNotNull(main);
        assertEquals("$49.99", main.getAnnual().getProduct().getLocalizedPrice());
        assertNull(main.getMonthly().getProduct());
    }

    @FormTest
    void answersFromTheStoredOfferingsWhenTheNetworkFails() {
        mock("/offerings", OFFERINGS);
        CommerceManager cm = CommerceManager.getInstance();
        cm.setAppUserId("u1");
        assertEquals("main", cm.getOfferings().getCurrent().getId());

        CommerceManager.resetForTests();
        implementation.clearNetworkMocks();
        mockFailure("/offerings");
        Offerings stored = CommerceManager.getInstance().getOfferings();
        Offering main = stored.getCurrent();
        assertNotNull(main);
        assertEquals("exp1", stored.getExperimentId());
        assertEquals(6, main.getPackages().size());
        assertTrue(main.getMonthly().isEligibleForIntro());
        assertEquals("P7D", main.getMonthly().getTrialPeriod());
        assertEquals("pw1", main.getPaywall().get("id"));

        // Another placement, and another customer, have no stored answer.
        assertTrue(CommerceManager.getInstance().getOfferings("settings").isEmpty());
    }

    @FormTest
    void aRefusedAnswerIsNotOfferings() {
        mock("/offerings", "{\"accepted\":false,\"currentOfferingId\":\"main\",\"offerings\":[{\"id\":\"main\"}]}");
        assertTrue(CommerceManager.getInstance().getOfferings().isEmpty());
    }

    @FormTest
    void purchasingAPackageSubscribesOrBuysOnce() throws Exception {
        Offering main = offerings(OFFERINGS).getCurrent();
        CommerceManager cm = CommerceManager.getInstance();
        cm.purchase(main.getMonthly());
        cm.purchase(main.getLifetime());
        cm.purchase(main.getPackage("coins"));
        cm.purchase(main.getPackage("odd"));
        cm.purchase((CommercePackage) null);
        assertEquals("[subscribe:pro_monthly, purchase:pro_forever, purchase:coins_100, subscribe:pro_odd]",
                storeCalls.toString());
    }

    @FormTest
    void managementUrlAndCurrenciesComeFromTheCustomer() {
        mock("/customer", "{\"accepted\":true,\"appUserId\":\"u1\",\"managementUrl\":\"https://store.test/m\","
                + "\"currencies\":{\"gems\":120,\"coins\":0}}");
        CommerceManager cm = CommerceManager.getInstance();
        cm.setAppUserId("u1");
        assertNull(cm.getManagementUrl());
        cm.refresh();
        assertEquals("https://store.test/m", cm.getManagementUrl());
        CustomerInfo info = cm.getCustomerInfo();
        assertEquals(120L, info.getCurrencyBalance("gems"));
        assertEquals(0L, info.getCurrencyBalance("missing"));
        assertEquals(0L, info.getCurrencyBalance(null));
        assertEquals(2, info.getCurrencies().size());
        // and they survive a cold start
        CommerceManager.resetForTests();
        assertEquals(120L, CommerceManager.getInstance().getCustomerInfo().getCurrencyBalance("gems"));
    }

    @FormTest
    void eventsAreQueuedAndSentTogether() {
        final CommerceManager cm = CommerceManager.getInstance();
        final String[] body = new String[1];
        Runnable r = new Runnable() {
            @Override
            public void run() {
                cm.logEvent("IMPRESSION", "main", "pw1", null);
                cm.logEvent("CANCEL_REASON", null, null, null, "too \"pricey\"");
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
        assertTrue(body[0].contains("\"events\":[{\"type\":\"IMPRESSION\",\"offeringId\":\"main\",\"paywallId\":\"pw1\",\"packageId\":null,\"time\":"));
        assertTrue(body[0].contains("\"reason\":\"too \\\"pricey\\\"\""));
        assertTrue(body[0].contains("\"buildKey\":\"test-build-key\""));
        assertNull(cm.drainEvents());

        // With the cloud off an event goes nowhere.
        Display.getInstance().setProperty("commerce.cloud.enabled", "false");
        cm.logEvent("CLOSE", null, null, null);
        assertNull(cm.drainEvents());
    }
}
