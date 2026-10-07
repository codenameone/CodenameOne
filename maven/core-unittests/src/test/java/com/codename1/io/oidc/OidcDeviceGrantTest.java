/*
 * Copyright (c) 2012-2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
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
package com.codename1.io.oidc;

import com.codename1.io.NetworkManager;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.DisplayTest;
import com.codename1.util.AsyncResource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.codename1.io.oidc.OidcTestSupport.await;
import static com.codename1.io.oidc.OidcTestSupport.text;
import static com.codename1.io.oidc.OidcTestSupport.tokenJson;
import static com.codename1.io.oidc.OidcTestSupport.utf8;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The device authorization grant on {@link OidcClient}, against a token endpoint that plays
 * a script of answers. A "second" of polling interval lasts 20 ms here.
 */
public class OidcDeviceGrantTest extends UITestBase {

    private static final String ISSUER = "https://login.example.com";
    private static final String DEVICE_EP = ISSUER + "/oauth2/device_authorization";
    private static final String TOKEN_EP = ISSUER + "/oauth2/token";
    private static final int UNIT = 20;

    private final List<String> deviceRequests = Collections.synchronizedList(new ArrayList<String>());
    private final List<String> polls = Collections.synchronizedList(new ArrayList<String>());
    private final List<Long> pollTimes = Collections.synchronizedList(new ArrayList<Long>());
    private final List<String> script = Collections.synchronizedList(new ArrayList<String>());
    private volatile String deviceAnswer;
    private volatile int deviceStatus;
    private OidcTestSupport.MemoryStore store;
    private OidcClient client;

    @BeforeEach
    void serve() {
        deviceRequests.clear();
        polls.clear();
        pollTimes.clear();
        script.clear();
        deviceStatus = 200;
        deviceAnswer = "{\"device_code\":\"DC\",\"user_code\":\"WDJB-MJHT\","
                + "\"verification_uri\":\"" + ISSUER + "/activate\","
                + "\"verification_uri_complete\":\"" + ISSUER + "/activate?user_code=WDJB-MJHT\","
                + "\"expires_in\":600,\"interval\":1}";
        store = new OidcTestSupport.MemoryStore();
        client = OidcClient.create(OidcConfiguration.newBuilder()
                        .issuer(ISSUER)
                        .authorizationEndpoint(ISSUER + "/oauth2/authorize")
                        .tokenEndpoint(TOKEN_EP)
                        .deviceAuthorizationEndpoint(DEVICE_EP)
                        .build())
                .setClientId("tv")
                .setScopes("pets.read")
                .setTokenStore(store);
        client.devicePollUnitMillis = UNIT;
        TestCodenameOneImplementation.getInstance().setNetworkMockHandler(
                new TestCodenameOneImplementation.NetworkMockHandler() {
                    public void handle(TestCodenameOneImplementation.TestConnection c) {
                        String body = text(c.getOutputData());
                        c.clearRequest();
                        if (c.getUrl().startsWith(DEVICE_EP)) {
                            deviceRequests.add(body);
                            c.respond(deviceStatus, "x", utf8(deviceAnswer));
                            return;
                        }
                        polls.add(body);
                        pollTimes.add(Long.valueOf(System.currentTimeMillis()));
                        String next = script.isEmpty() ? "authorization_pending" : script.remove(0);
                        if ("tokens".equals(next)) {
                            c.respond(200, "OK", utf8(tokenJson("AT", "RT")));
                        } else {
                            c.respond(400, "Bad Request", utf8("{\"error\":\"" + next + "\"}"));
                        }
                    }
                });
    }

    @AfterEach
    void forget() {
        NetworkManager.getInstance().setAuthorizer(ISSUER, null);
        TestCodenameOneImplementation.getInstance().clearNetworkMocks();
    }

    private OidcDeviceAuthorization start() {
        OidcTestSupport.Outcome<OidcDeviceAuthorization> r = await(client.requestDeviceAuthorization());
        assertNull(r.error);
        return r.value;
    }

    @Test
    void theAuthorizationRequestCarriesTheClientAndScopesAndIsParsed() {
        client.setScopes("openid", "pets.read");
        OidcDeviceAuthorization d = start();

        assertEquals(1, deviceRequests.size());
        assertTrue(deviceRequests.get(0).contains("client_id=tv"), deviceRequests.get(0));
        assertTrue(deviceRequests.get(0).contains("scope=openid"), deviceRequests.get(0));
        assertEquals("DC", d.getDeviceCode());
        assertEquals("WDJB-MJHT", d.getUserCode());
        assertEquals(ISSUER + "/activate", d.getVerificationUri());
        assertEquals(ISSUER + "/activate?user_code=WDJB-MJHT", d.getVerificationUriComplete());
        assertEquals(1, d.getInterval());
        assertFalse(d.isExpired());
        assertNotNull(d.getExpiresAt());
    }

    @Test
    void anOpenIdDeviceGrantCannotSignInWithoutAnIdToken() {
        client.setScopes("openid", "pets.read");
        script.add("tokens");
        OidcRequestAuthorizer authorizer = new OidcRequestAuthorizer(client);
        OidcTestSupport.Outcome<OidcTokens> result = await(client.pollDeviceToken(start()));
        assertNull(result.value);
        assertEquals(OidcException.INVALID_ID_TOKEN, ((OidcException) result.error).getError());
        assertNull(store.saved);
        assertFalse(authorizer.isSignedIn());
    }

    @Test
    void aRefusedAuthorizationRequestReportsTheServersError() {
        deviceStatus = 400;
        deviceAnswer = "{\"error\":\"invalid_client\",\"error_description\":\"unknown client\"}";

        OidcTestSupport.Outcome<OidcDeviceAuthorization> r = await(client.requestDeviceAuthorization());

        assertInstanceOf(OidcException.class, r.error);
        assertEquals("invalid_client", ((OidcException) r.error).getError());
        assertEquals("unknown client", ((OidcException) r.error).getErrorDescription());
    }

    @Test
    void theGrantNeedsAnEndpointAndAClientId() {
        OidcClient noEndpoint = OidcClient.create(OidcConfiguration.newBuilder()
                .authorizationEndpoint(ISSUER + "/a").tokenEndpoint(TOKEN_EP).build()).setClientId("tv");
        assertThrows(IllegalStateException.class, noEndpoint::requestDeviceAuthorization);
        assertThrows(IllegalArgumentException.class, () -> client.pollDeviceToken(null));
        assertThrows(IllegalStateException.class,
                () -> OidcClient.create(client.getConfiguration()).requestDeviceAuthorization());
    }

    @Test
    void pollingWaitsThroughPendingAndSlowDownUntilApproval() {
        OidcDeviceAuthorization d = start();
        script.add("authorization_pending");
        script.add("slow_down");
        script.add("tokens");
        long began = System.currentTimeMillis();

        OidcTestSupport.Outcome<OidcTokens> r = await(client.pollDeviceToken(d));

        assertNull(r.error);
        assertEquals("AT", r.value.getAccessToken());
        assertEquals(3, polls.size());
        assertTrue(polls.get(0).contains("device_code=DC"), polls.get(0));
        assertTrue(polls.get(0).contains("grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Adevice_code"),
                polls.get(0));
        assertTrue(polls.get(0).contains("client_id=tv"), polls.get(0));
        // Nothing is asked before the first interval has passed, and slow_down adds five
        // "seconds" to the wait that follows it.
        assertTrue(pollTimes.get(0).longValue() - began >= UNIT - 5,
                "first poll after " + (pollTimes.get(0).longValue() - began) + " ms");
        long afterSlowDown = pollTimes.get(2).longValue() - pollTimes.get(1).longValue();
        assertTrue(afterSlowDown >= 6 * UNIT - 5, "waited " + afterSlowDown + " ms after slow_down");
        assertEquals("RT", store.saved.getRefreshToken(), "the tokens are stored as authorize() stores them");
    }

    @Test
    void theTokensReachTheClientsAuthorizer() {
        OidcRequestAuthorizer authorizer = new OidcRequestAuthorizer(client);
        script.add("tokens");

        await(client.pollDeviceToken(start()));

        assertEquals("AT", authorizer.getTokens().getAccessToken());
    }

    @Test
    void aDenialStopsThePolling() {
        OidcDeviceAuthorization d = start();
        script.add("authorization_pending");
        script.add("access_denied");

        OidcTestSupport.Outcome<OidcTokens> r = await(client.pollDeviceToken(d));

        assertInstanceOf(OidcException.class, r.error);
        assertEquals(OidcException.ACCESS_DENIED, ((OidcException) r.error).getError());
        assertNoMorePolls(2);
        assertNull(store.saved);
    }

    @Test
    void anExpiredCodeReportedByTheServerStopsThePolling() {
        OidcDeviceAuthorization d = start();
        script.add("expired_token");

        OidcTestSupport.Outcome<OidcTokens> r = await(client.pollDeviceToken(d));

        assertEquals(OidcException.EXPIRED_TOKEN, ((OidcException) r.error).getError());
        assertNoMorePolls(1);
    }

    @Test
    void aCodePastItsExpiryIsNotAskedAbout() {
        Map<String, Object> json = new HashMap<String, Object>();
        json.put("device_code", "DC");
        json.put("user_code", "AAAA-BBBB");
        json.put("verification_uri", ISSUER + "/activate");
        json.put("expires_in", Integer.valueOf(0));
        json.put("interval", Integer.valueOf(1));
        OidcDeviceAuthorization expired = OidcDeviceAuthorization.fromJson(json);
        assertTrue(expired.isExpired());

        OidcTestSupport.Outcome<OidcTokens> r = await(client.pollDeviceToken(expired));

        assertEquals(OidcException.EXPIRED_TOKEN, ((OidcException) r.error).getError());
        assertTrue(polls.isEmpty());
    }

    @Test
    void cancellingStopsThePolling() throws Exception {
        OidcDeviceAuthorization d = start();
        AsyncResource<OidcTokens> waiting = client.pollDeviceToken(d);
        long deadline = System.currentTimeMillis() + 20000;
        while (polls.size() < 2 && System.currentTimeMillis() < deadline) {
            DisplayTest.flushEdt();
            Thread.sleep(5);
        }
        assertTrue(polls.size() >= 2, "polling never started");

        waiting.cancel(false);
        DisplayTest.flushEdt();
        Thread.sleep(2 * UNIT);
        DisplayTest.flushEdt();
        int seen = polls.size();
        assertNoMorePolls(seen);
        assertTrue(waiting.isCancelled());
    }

    @Test
    void anAnswerWithoutItsCodesIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> OidcDeviceAuthorization.fromJson(new HashMap<String, Object>()));
        assertThrows(IllegalArgumentException.class, () -> OidcDeviceAuthorization.fromJson(null));
        deviceAnswer = "{\"user_code\":\"AAAA-BBBB\"}";
        OidcTestSupport.Outcome<OidcDeviceAuthorization> r = await(client.requestDeviceAuthorization());
        assertInstanceOf(OidcException.class, r.error);
    }

    @Test
    void anAnswerWithNoPageToTypeTheCodeOnIsRefused() {
        Map<String, Object> json = new HashMap<String, Object>();
        json.put("device_code", "DC");
        json.put("user_code", "AAAA-BBBB");
        assertEquals("A device authorization response needs verification_uri",
                assertThrows(IllegalArgumentException.class,
                        () -> OidcDeviceAuthorization.fromJson(json)).getMessage());
        // The complete form alone is not it: it is for a QR code, and optional.
        json.put("verification_uri_complete", ISSUER + "/activate?user_code=AAAA-BBBB");
        assertThrows(IllegalArgumentException.class, () -> OidcDeviceAuthorization.fromJson(json));

        deviceAnswer = "{\"device_code\":\"DC\",\"user_code\":\"AAAA-BBBB\",\"expires_in\":600}";
        OidcTestSupport.Outcome<OidcDeviceAuthorization> r = await(client.requestDeviceAuthorization());
        assertNull(r.value);
        assertInstanceOf(OidcException.class, r.error);
        assertEquals("A device authorization response needs verification_uri", r.error.getMessage());
    }

    @Test
    void deviceAuthorizationRequiresABoundedNumericExpiry() {
        Map<String, Object> json = new HashMap<String, Object>();
        json.put("device_code", "DC");
        json.put("user_code", "AAAA-BBBB");
        json.put("verification_uri", ISSUER + "/activate");
        for (Object invalid : new Object[] {null, "later", Long.valueOf(-1),
                Long.valueOf(Long.MAX_VALUE), Double.valueOf(Double.NaN), Double.valueOf(1.5)}) {
            json.put("expires_in", invalid);
            assertThrows(IllegalArgumentException.class, () -> OidcDeviceAuthorization.fromJson(json),
                    String.valueOf(invalid));
        }
    }

    @Test
    void theIntervalDefaultsToFiveSecondsAndTheOldUriNameIsRead() {
        Map<String, Object> json = new HashMap<String, Object>();
        json.put("device_code", "DC");
        json.put("user_code", "AAAA-BBBB");
        json.put("verification_url", "https://example.com/device");
        json.put("expires_in", Integer.valueOf(600));
        OidcDeviceAuthorization d = OidcDeviceAuthorization.fromJson(json);

        assertEquals(5, d.getInterval());
        assertEquals("https://example.com/device", d.getVerificationUri());
        assertNotNull(d.getExpiresAt());
        assertFalse(d.isExpired());
    }

    @Test
    void discoveryReadsTheDeviceEndpoint() {
        Map<String, Object> doc = new HashMap<String, Object>();
        doc.put("authorization_endpoint", ISSUER + "/oauth2/authorize");
        doc.put("device_authorization_endpoint", DEVICE_EP);
        OidcConfiguration cfg = OidcConfiguration.fromDiscoveryJson(doc);

        assertEquals(DEVICE_EP, cfg.getDeviceAuthorizationEndpoint());
        assertEquals(DEVICE_EP, OidcConfiguration.newBuilder(cfg).build().getDeviceAuthorizationEndpoint());
    }

    private void assertNoMorePolls(int expected) throws AssertionError {
        try {
            Thread.sleep(8 * UNIT);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        DisplayTest.flushEdt();
        assertEquals(expected, polls.size(), "polling went on after it should have stopped");
    }
}
