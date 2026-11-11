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

import com.codename1.io.JSONParser;
import com.codename1.io.Preferences;
import com.codename1.io.Storage;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.security.Ed25519;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.Display;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.util.Base64;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CommerceManagerTest extends UITestBase {
    private static final String ENDPOINT = "http://commerce.test/api";

    /// A customer with one active entitlement that ended long ago by any
    /// device clock: only the server decides when it stops being active.
    private static final String PRO_CUSTOMER =
            "{\"accepted\":true,\"degraded\":false,\"serverTime\":1700000000000,"
                    + "\"appUserId\":\"USER\",\"originalAppUserId\":\"anon-1\","
                    + "\"managementUrl\":\"https://store.test/manage\","
                    + "\"activeSubscriptions\":[\"pro_monthly\"],"
                    + "\"entitlements\":{\"pro\":{\"entitlementId\":\"pro\",\"active\":true,"
                    + "\"expiresDate\":1000,\"productId\":\"pro_monthly\",\"store\":\"apple\","
                    + "\"periodType\":\"NORMAL\",\"ownershipType\":\"PURCHASED\","
                    + "\"billingStage\":\"ACTIVE\",\"willRenew\":true,\"provenance\":\"PURCHASE\"},"
                    + "\"old\":{\"entitlementId\":\"old\",\"active\":false,\"expiresDate\":5}},"
                    + "\"attributes\":{\"plan\":\"gold\"}}";

    @BeforeEach
    void setUpCommerce() throws Exception {
        Storage.setStorageInstance(null);
        Storage.getInstance().clearStorage();
        Storage.getInstance().clearCache();
        Preferences.clearAll();
        implementation.clearNetworkMocks();
        implementation.setInAppPurchase(new Purchase() {
        });
        Display d = Display.getInstance();
        d.setProperty("build_key", "test-build-key");
        d.setProperty("package_name", "com.example.app");
        d.setProperty("commerce.cloud.endpoint", ENDPOINT);
        d.setProperty("commerce.cloud.enabled", "true");
        d.setProperty("commerce.cloud.publicKey", "");
        CommerceManager.resetForTests();
    }

    @AfterEach
    void tearDownCommerce() {
        Display d = Display.getInstance();
        d.setProperty("build_key", "");
        d.setProperty("commerce.cloud.publicKey", "");
        implementation.clearNetworkMocks();
        implementation.setInAppPurchase(null);
        CommerceManager.resetForTests();
    }

    @FormTest
    void customerLookupWithNoReceiptsGrantsEntitlement() {
        mock("/customer", envelope(PRO_CUSTOMER.replace("USER", "u1"), null));
        CommerceManager cm = CommerceManager.getInstance();
        cm.setAppUserId("u1");
        assertFalse(cm.isEntitled("pro"));
        assertNotNull(cm.getCustomerInfo());
        assertNull(cm.getCustomerInfo().getAppUserId());

        cm.refresh();

        assertFalse(cm.isDegraded());
        assertTrue(cm.isEntitled("pro"));
        assertFalse(cm.isEntitled("old"));
        assertEquals(1, cm.getActiveEntitlements().size());
        assertEquals("pro", cm.getActiveEntitlements().get(0));
        CustomerInfo info = cm.getCustomerInfo();
        assertEquals("u1", info.getAppUserId());
        assertEquals("anon-1", info.getOriginalAppUserId());
        assertEquals("https://store.test/manage", info.getManagementUrl());
        assertEquals(1700000000000L, info.getServerTime());
        assertEquals("pro_monthly", info.getActiveSubscriptions().get(0));
        assertEquals("gold", info.getAttributes().get("plan"));
        assertEquals(1, info.getActiveEntitlements().size());
        EntitlementInfo pro = info.getEntitlement("pro");
        assertEquals("pro", pro.getId());
        assertEquals(1000L, pro.getExpiresDate());
        assertEquals("pro_monthly", pro.getProductId());
        assertEquals("apple", pro.getStore());
        assertEquals("NORMAL", pro.getPeriodType());
        assertEquals("PURCHASED", pro.getOwnershipType());
        assertEquals("ACTIVE", pro.getBillingStage());
        assertTrue(pro.isWillRenew());
        assertEquals("PURCHASE", pro.getProvenance());
        assertNull(info.getEntitlement("missing"));
    }

    @FormTest
    void coldStartAnswersFromTheStoredCustomerBeforeAnyNetwork() {
        mock("/customer", envelope(PRO_CUSTOMER.replace("USER", "u1"), null));
        CommerceManager.getInstance().setAppUserId("u1");
        CommerceManager.getInstance().refresh();

        CommerceManager.resetForTests();
        implementation.clearNetworkMocks();
        CommerceManager cold = CommerceManager.getInstance();

        assertEquals("u1", cold.getAppUserId());
        // expiresDate is 1000 ms after the epoch: long past by this clock,
        // and still reported as the server last said.
        assertTrue(cold.isEntitled("pro"));
        assertEquals(1000L, cold.getCustomerInfo().getEntitlement("pro").getExpiresDate());
        assertEquals("gold", cold.getCustomerInfo().getAttributes().get("plan"));
        assertEquals("pro", cold.getActiveEntitlements().get(0));
    }

    @FormTest
    void storedCustomerBelongsToItsUser() {
        mock("/customer", envelope(PRO_CUSTOMER.replace("USER", "u1"), null));
        CommerceManager cm = CommerceManager.getInstance();
        cm.setAppUserId("u1");
        cm.refresh();
        assertTrue(cm.isEntitled("pro"));

        cm.setAppUserId("someone/else");
        assertFalse(cm.isEntitled("pro"));
        assertNull(cm.getCustomerInfo().getAppUserId());

        cm.setAppUserId("u1");
        assertTrue(cm.isEntitled("pro"));
    }

    @FormTest
    void badOrMissingSignatureIsIgnoredWhenAKeyIsKnown() throws Exception {
        byte[] priv = privateKey();
        Display.getInstance().setProperty("commerce.cloud.publicKey",
                Base64.encodeNoNewline(Ed25519.publicKey(priv)));
        CommerceManager cm = CommerceManager.getInstance();
        cm.setAppUserId("u1");

        // Signed, but over a nonce this request never carried.
        String payload = PRO_CUSTOMER.replace("USER", "u1");
        byte[] sig = Ed25519.sign(priv, ("not-the-nonce\n" + payload).getBytes(StandardCharsets.UTF_8));
        mock("/customer", envelope(payload, Base64.encodeNoNewline(sig)));
        cm.refresh();
        assertTrue(cm.isDegraded());
        assertFalse(cm.isEntitled("pro"));
        assertNull(cm.getCustomerInfo().getAppUserId());

        // Not signed at all.
        mock("/customer", envelope(payload, null));
        cm.refresh();
        assertTrue(cm.isDegraded());
        assertFalse(cm.isEntitled("pro"));

        // Nothing was stored for a cold start either.
        CommerceManager.resetForTests();
        assertFalse(CommerceManager.getInstance().isEntitled("pro"));
    }

    @FormTest
    void goodSignatureOverTheRequestNonceIsAccepted() throws Exception {
        final byte[] priv = privateKey();
        Display.getInstance().setProperty("commerce.cloud.publicKey",
                Base64.encodeNoNewline(Ed25519.publicKey(priv)));
        final String payload = PRO_CUSTOMER.replace("USER", "u1");
        final List<String> nonces = new ArrayList<String>();
        implementation.setNetworkMockHandler(new TestCodenameOneImplementation.NetworkMockHandler() {
            @Override
            public void handle(TestCodenameOneImplementation.TestConnection connection) {
                try {
                    String body = new String(connection.getOutputData(), StandardCharsets.UTF_8);
                    Map<String, Object> request = JSONParser.parseJSON(body);
                    String nonce = String.valueOf(request.get("nonce"));
                    nonces.add(nonce);
                    assertEquals("test-build-key", request.get("buildKey"));
                    assertEquals("com.example.app", request.get("packageName"));
                    assertEquals("u1", request.get("appUserId"));
                    byte[] sig = Ed25519.sign(priv, (nonce + "\n" + payload).getBytes(StandardCharsets.UTF_8));
                    connection.setInputData(envelope(payload, Base64.encodeNoNewline(sig))
                            .getBytes(StandardCharsets.UTF_8));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        CommerceManager cm = CommerceManager.getInstance();
        cm.setAppUserId("u1");

        cm.refresh();
        assertFalse(cm.isDegraded());
        assertTrue(cm.isEntitled("pro"));

        cm.refresh();
        assertTrue(nonces.size() >= 2);
        assertEquals(32, nonces.get(0).length());
        assertNotEquals(nonces.get(0), nonces.get(nonces.size() - 1));
    }

    @FormTest
    void unsignedAnswerIsAcceptedWhenNoKeyIsKnown() {
        mock("/customer", envelope(PRO_CUSTOMER.replace("USER", "u1"), null));
        CommerceManager cm = CommerceManager.getInstance();
        cm.setAppUserId("u1");
        cm.refresh();
        assertFalse(cm.isDegraded());
        assertTrue(cm.isEntitled("pro"));
    }

    @FormTest
    void unreachableCloudIsDegradedAndKeepsTheLastAnswer() {
        mock("/customer", envelope(PRO_CUSTOMER.replace("USER", "u1"), null));
        CommerceManager cm = CommerceManager.getInstance();
        cm.setAppUserId("u1");
        cm.refresh();
        implementation.addNetworkMockResponse(ENDPOINT + "/customer", 500, "Error", new byte[0]);
        cm.refresh();
        assertTrue(cm.isDegraded());
        assertTrue(cm.isEntitled("pro"));
    }

    @FormTest
    void logInSwitchesTheIdAndAdoptsTheAnswer() {
        CommerceManager cm = CommerceManager.getInstance();
        String anonymous = cm.getAppUserId();
        assertTrue(anonymous.startsWith("anon-"));
        mock("/identify", envelope(PRO_CUSTOMER.replace("USER", "account-7"), null));

        assertTrue(cm.logIn("account-7"));

        assertEquals("account-7", cm.getAppUserId());
        assertEquals("account-7", cm.getCustomerInfo().getAppUserId());
        assertTrue(cm.isEntitled("pro"));
        assertFalse(cm.isDegraded());

        // The new id and its customer survive a restart.
        CommerceManager.resetForTests();
        implementation.clearNetworkMocks();
        assertEquals("account-7", CommerceManager.getInstance().getAppUserId());
        assertTrue(CommerceManager.getInstance().isEntitled("pro"));
    }

    @FormTest
    void failedLogInKeepsTheId() {
        CommerceManager cm = CommerceManager.getInstance();
        String anonymous = cm.getAppUserId();
        implementation.addNetworkMockResponse(ENDPOINT + "/identify", 500, "Error", new byte[0]);
        assertFalse(cm.logIn("account-7"));
        assertEquals(anonymous, cm.getAppUserId());
        assertTrue(cm.isDegraded());
    }

    @FormTest
    void logOutClearsTheCustomer() {
        mock("/identify", envelope(PRO_CUSTOMER.replace("USER", "account-7"), null));
        CommerceManager cm = CommerceManager.getInstance();
        String first = cm.getAppUserId();
        assertTrue(cm.logIn("account-7"));
        assertTrue(cm.isEntitled("pro"));

        cm.logOut();

        String anonymous = cm.getAppUserId();
        assertTrue(anonymous.startsWith("anon-"));
        assertNotEquals("account-7", anonymous);
        assertNotEquals(first, anonymous);
        assertFalse(cm.isEntitled("pro"));
        assertNull(cm.getCustomerInfo().getAppUserId());
        assertTrue(cm.getActiveEntitlements().isEmpty());

        // Logging back in to the same account on a cold start reads nothing
        // stale: the stored customer went with the log out.
        CommerceManager.resetForTests();
        CommerceManager.getInstance().setAppUserId("account-7");
        assertFalse(CommerceManager.getInstance().isEntitled("pro"));
    }

    @FormTest
    void listenerFiresWhenTheCustomerChanges() {
        mock("/customer", envelope(PRO_CUSTOMER.replace("USER", "u1"), null));
        CommerceManager cm = CommerceManager.getInstance();
        cm.setAppUserId("u1");
        final List<Object> sources = new ArrayList<Object>();
        final boolean[] onEdt = {true};
        ActionListener l = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                sources.add(evt.getSource());
                onEdt[0] &= Display.getInstance().isEdt();
            }
        };
        cm.addCustomerInfoListener(l);

        cm.refresh();
        com.codename1.ui.DisplayTest.flushEdt();
        assertEquals(1, sources.size());
        assertSame(cm.getCustomerInfo(), sources.get(0));
        assertTrue(onEdt[0]);

        // The same state with a later server clock is not a change.
        mock("/customer", envelope(PRO_CUSTOMER.replace("USER", "u1")
                .replace("1700000000000", "1700000009999"), null));
        cm.refresh();
        com.codename1.ui.DisplayTest.flushEdt();
        assertEquals(1, sources.size());
        assertEquals(1700000009999L, cm.getCustomerInfo().getServerTime());

        mock("/customer", envelope(PRO_CUSTOMER.replace("USER", "u1")
                .replace("\"active\":true", "\"active\":false"), null));
        cm.refresh();
        com.codename1.ui.DisplayTest.flushEdt();
        assertEquals(2, sources.size());
        assertFalse(cm.isEntitled("pro"));

        cm.removeCustomerInfoListener(l);
        cm.logOut();
        com.codename1.ui.DisplayTest.flushEdt();
        assertEquals(2, sources.size());
    }

    @FormTest
    void customerRoundTripsThroughItsMap() throws Exception {
        CustomerInfo a = new CustomerInfo(JSONParser.parseJSON(PRO_CUSTOMER));
        CustomerInfo b = new CustomerInfo(JSONParser.parseJSON(JSONParser.mapToJson(a.toMap())));
        assertTrue(a.sameState(b));
        assertEquals(a.getServerTime(), b.getServerTime());
        assertEquals(1000L, b.getEntitlement("pro").getExpiresDate());
        assertTrue(new CustomerInfo(null).getEntitlements().isEmpty());
        assertFalse(new CustomerInfo(null).isEntitled("pro"));
    }

    @FormTest
    void cloudDisabledIsANoOp() {
        Display.getInstance().setProperty("build_key", "");
        // Any request would fail the test by degrading the manager.
        implementation.addNetworkMockResponse(ENDPOINT + "/customer", 500, "Error", new byte[0]);
        CommerceManager cm = CommerceManager.getInstance();
        assertFalse(cm.isCloudEnabled());

        cm.refresh();
        CommerceManager.receiptsSynchronized();
        cm.restore();
        assertFalse(cm.setAttributes(new HashMap<String, Object>()));

        assertFalse(cm.isDegraded());
        assertFalse(cm.isEntitled("pro"));
        assertNotNull(cm.getCustomerInfo());
        assertTrue(cm.getActiveEntitlements().isEmpty());
        assertTrue(cm.logIn("account-7"));
        assertEquals("account-7", cm.getAppUserId());
    }

    @FormTest
    void attributesArePosted() {
        implementation.addNetworkMockResponse(ENDPOINT + "/attributes", 204, "No Content", new byte[0]);
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("plan", "gold");
        attrs.put("gone", null);
        assertTrue(CommerceManager.getInstance().setAttributes(attrs));
        implementation.addNetworkMockResponse(ENDPOINT + "/attributes", 403, "Forbidden", new byte[0]);
        assertFalse(CommerceManager.getInstance().setAttributes(attrs));
    }

    /// The service picks a verifier by store name; a receipt whose store it
    /// does not recognise is one it cannot validate.
    @FormTest
    void receiptsAreSentUnderTheStoreNameTheServiceRoutesOn() {
        Receipt amazon = new Receipt();
        amazon.setStoreCode(Receipt.STORE_CODE_AMAZON);
        assertEquals("amazon", CommerceManager.storeOf(amazon));

        Receipt play = new Receipt();
        play.setStoreCode(Receipt.STORE_CODE_PLAY);
        assertEquals("google", CommerceManager.storeOf(play));

        Receipt apple = new Receipt();
        apple.setStoreCode(Receipt.STORE_CODE_ITUNES);
        assertEquals("apple", CommerceManager.storeOf(apple));

        assertEquals("", CommerceManager.storeOf(new Receipt()));
    }

    private void mock(String path, String body) {
        implementation.addNetworkMockResponse(ENDPOINT + path, 200, "OK", body.getBytes(StandardCharsets.UTF_8));
    }

    /// The signed envelope of the contract: the payload travels as JSON text
    /// inside a string, since that text is what the signature covers.
    private static String envelope(String payload, String signature) {
        StringBuilder sb = new StringBuilder("{\"payload\":\"");
        for (int i = 0; i < payload.length(); i++) {
            char c = payload.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\');
            }
            sb.append(c);
        }
        sb.append("\",\"signature\":");
        if (signature == null) {
            sb.append("null");
        } else {
            sb.append('"').append(signature).append('"');
        }
        sb.append(",\"keyId\":null}");
        return sb.toString();
    }

    private static byte[] privateKey() {
        byte[] priv = new byte[Ed25519.PRIVATE_KEY_LENGTH];
        for (int i = 0; i < priv.length; i++) {
            priv[i] = (byte) (i * 7 + 3);
        }
        return priv;
    }
}
