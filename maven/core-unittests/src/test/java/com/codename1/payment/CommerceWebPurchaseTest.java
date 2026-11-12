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
import com.codename1.testing.TestCodenameOneImplementation;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/// The parts of the commerce client that leave the platform store: the Mac
/// store name, web checkout, the billing portal and signed promotional offers.
class CommerceWebPurchaseTest extends CommerceTestBase {
    /// One offering whose package has a web price and no store product.
    private static final String WEB_OFFERING =
            "{\"currentOfferingId\":\"main\",\"offerings\":[{\"id\":\"main\",\"packages\":["
                    + "{\"id\":\"annual\",\"type\":\"ANNUAL\",\"period\":\"P1Y\"},"
                    + "{\"id\":\"monthly\",\"type\":\"MONTHLY\",\"productId\":\"pro_monthly\",\"period\":\"P1M\"}]}]}";

    private final List<String> bodies = new ArrayList<String>();
    private final List<String> offerCalls = new ArrayList<String>();

    @Override
    protected Purchase createStore() {
        return new Purchase() {
            @Override
            public void subscribe(String sku) {
                storeCalls.add("subscribe:" + sku);
            }

            @Override
            public void purchase(String sku) {
                storeCalls.add("purchase:" + sku);
            }

            @Override
            public void purchase(String sku, PromotionalOffer offer) {
                if (offer instanceof ApplePromotionalOffer) {
                    ApplePromotionalOffer a = (ApplePromotionalOffer) offer;
                    offerCalls.add(sku + "|" + a.getOfferIdentifier() + "|" + a.getKeyIdentifier() + "|"
                            + a.getNonce() + "|" + a.getSignature() + "|" + a.getTimestamp());
                } else {
                    offerCalls.add(sku + "|" + offer);
                }
            }
        };
    }

    /// Keeps the body of every request, beside the canned answers.
    private void recordBodies() {
        bodies.clear();
        implementation.setNetworkMockHandler(new TestCodenameOneImplementation.NetworkMockHandler() {
            @Override
            public void handle(TestCodenameOneImplementation.TestConnection connection) {
                byte[] out = connection.getOutputData();
                if (out != null) {
                    bodies.add(new String(out, StandardCharsets.UTF_8));
                }
                signConnection(connection);
            }
        });
    }

    private boolean anyBodyHas(String text) {
        for (String b : bodies) {
            if (b.contains(text)) {
                return true;
            }
        }
        return false;
    }

    private void customerOn(String store, String managementUrl) {
        mock("/customer", "{\"accepted\":true,\"appUserId\":\"u1\","
                + "\"managementUrl\":\"" + managementUrl + "\","
                + "\"entitlements\":{"
                + "\"old\":{\"entitlementId\":\"old\",\"active\":true,\"expiresDate\":10,\"store\":\"apple\"},"
                + "\"pro\":{\"entitlementId\":\"pro\",\"active\":true,\"expiresDate\":5000,\"store\":\"" + store + "\"},"
                + "\"gone\":{\"entitlementId\":\"gone\",\"active\":false,\"expiresDate\":9000,\"store\":\"paddle\"}}}");
        CommerceManager cm = CommerceManager.getInstance();
        cm.setAppUserId("u1");
        cm.refresh();
    }

    @FormTest
    void aMacReceiptIsSentUnderTheMacStore() {
        Receipt r = new Receipt();
        r.setStoreCode(Receipt.STORE_CODE_ITUNES);
        Receipt play = new Receipt();
        play.setStoreCode(Receipt.STORE_CODE_PLAY);
        try {
            implementation.setPlatformName("mac");
            assertEquals("mac", CommerceManager.storeOf(r));
            assertEquals("google", CommerceManager.storeOf(play));
            implementation.setPlatformName("ios");
            assertEquals("apple", CommerceManager.storeOf(r));
        } finally {
            implementation.setPlatformName(null);
        }
    }

    @FormTest
    void aPackageKnowsItsOfferingAndTheCheckoutIsAskedForByBothIds() throws Exception {
        CommercePackage annual = offerings(WEB_OFFERING).getCurrent().getPackage("annual");
        assertEquals("main", annual.getOfferingId());
        mock("/web-purchase", "{\"accepted\":true,\"url\":\"https://pay.test/c/1\"}");
        recordBodies();
        CommerceManager cm = CommerceManager.getInstance();
        assertEquals("https://pay.test/c/1", cm.getWebPurchaseUrl(annual, "app://done", "app://back"));
        assertTrue(anyBodyHas("\"offeringId\":\"main\""), bodies.toString());
        assertTrue(anyBodyHas("\"packageId\":\"annual\""), bodies.toString());
        assertTrue(anyBodyHas("\"successUrl\":\"app://done\""), bodies.toString());
        assertTrue(anyBodyHas("\"cancelUrl\":\"app://back\""), bodies.toString());
    }

    @FormTest
    void aRefusedOrUnreachableCheckoutIsNull() throws Exception {
        CommercePackage annual = offerings(WEB_OFFERING).getCurrent().getPackage("annual");
        CommerceManager cm = CommerceManager.getInstance();
        mock("/web-purchase", "{\"accepted\":false}");
        assertNull(cm.getWebPurchaseUrl(annual, null, null));
        assertFalse(cm.purchaseOnWeb(annual));
        mock("/web-purchase", "{\"accepted\":true,\"url\":\"\"}");
        assertNull(cm.getWebPurchaseUrl(annual, null, null));
        mockFailure("/web-purchase");
        assertNull(cm.getWebPurchaseUrl(annual, null, null));
        assertNull(cm.getWebPurchaseUrl(null, null, null));
    }

    @FormTest
    void withNoNativeStoreAPackageWithNoProductIsSoldOnTheWeb() throws Exception {
        Offering main = offerings(WEB_OFFERING).getCurrent();
        CommercePackage annual = main.getPackage("annual");
        CommercePackage monthly = main.getPackage("monthly");
        mock("/web-purchase", "{\"accepted\":true,\"url\":\"https://pay.test/c/2\"}");
        CommerceManager cm = CommerceManager.getInstance();
        assertTrue(cm.isWebPurchase(annual));
        assertFalse(cm.isWebPurchase(monthly));
        cm.purchase(annual);
        assertEquals("https://pay.test/c/2", implementation.getExecuteURL());
        assertTrue(storeCalls.isEmpty());
        cm.purchase(monthly);
        assertEquals("[subscribe:pro_monthly]", storeCalls.toString());
    }

    @FormTest
    void aNativeStoreNeverFallsBackToTheWeb() throws Exception {
        implementation.setInAppPurchase(new Purchase() {
            @Override
            public boolean isSubscriptionSupported() {
                return true;
            }
        });
        CommercePackage annual = offerings(WEB_OFFERING).getCurrent().getPackage("annual");
        assertFalse(CommerceManager.getInstance().isWebPurchase(annual));
    }

    @FormTest
    void aWebSubscriptionIsManagedInThePortal() {
        customerOn("stripe", "https://store.test/manage");
        CommerceManager cm = CommerceManager.getInstance();
        // nothing was asked yet, and asking is never implicit
        assertEquals("https://store.test/manage", cm.getManagementUrl());
        mock("/web-portal", "{\"accepted\":true,\"url\":\"https://billing.test/p/1\"}");
        assertEquals("https://billing.test/p/1", cm.fetchManagementUrl());
        assertEquals("https://billing.test/p/1", cm.getManagementUrl());
        mock("/web-portal", "{\"accepted\":false}");
        assertEquals("https://billing.test/p/1", cm.fetchManagementUrl());
    }

    @FormTest
    void aStoreSubscriptionIsManagedInTheStore() {
        customerOn("apple", "https://store.test/manage");
        recordBodies();
        CommerceManager cm = CommerceManager.getInstance();
        mock("/web-portal", "{\"accepted\":true,\"url\":\"https://billing.test/p/1\"}");
        assertEquals("https://store.test/manage", cm.fetchManagementUrl());
        assertEquals("https://store.test/manage", cm.getManagementUrl());
        assertTrue(bodies.isEmpty(), bodies.toString());
    }

    @FormTest
    void aSignedPromotionalOfferReachesTheStore() {
        mock("/promo-offer-signature", "{\"accepted\":true,\"keyId\":\"KEY1\",\"nonce\":\"n-1\","
                + "\"timestamp\":1700000000123,\"signature\":\"c2ln\"}");
        recordBodies();
        CommerceManager cm = CommerceManager.getInstance();
        try {
            // no promotional offers on this store: nothing is asked or bought
            assertFalse(cm.purchaseWithPromotionalOffer("pro_half", "half"));
            assertTrue(bodies.isEmpty(), bodies.toString());
            implementation.setPlatformName("ios");
            assertTrue(cm.purchaseWithPromotionalOffer("pro_half", "half"));
            assertEquals("[pro_half|half|KEY1|n-1|c2ln|1700000000123]", offerCalls.toString());
            assertTrue(anyBodyHas("\"productId\":\"pro_half\""), bodies.toString());
            assertTrue(anyBodyHas("\"offerId\":\"half\""), bodies.toString());
            offerCalls.clear();
            mock("/promo-offer-signature", "{\"accepted\":false}");
            assertFalse(cm.purchaseWithPromotionalOffer("pro_half", "half"));
            mock("/promo-offer-signature", "{\"accepted\":true,\"keyId\":\"KEY1\"}");
            assertFalse(cm.purchaseWithPromotionalOffer("pro_half", "half"));
            assertFalse(cm.purchaseWithPromotionalOffer("pro_half", null));
            assertTrue(offerCalls.isEmpty());
            assertTrue(storeCalls.isEmpty());
        } finally {
            implementation.setPlatformName(null);
        }
    }
}
