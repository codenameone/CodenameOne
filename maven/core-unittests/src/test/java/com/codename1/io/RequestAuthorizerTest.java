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
package com.codename1.io;

import com.codename1.io.rest.Response;
import com.codename1.io.rest.Rest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.Display;
import com.codename1.ui.DisplayTest;
import com.codename1.ui.events.ActionListener;
import com.codename1.util.AsyncResource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * What an authorizer does to a request, with a service that answers by the header it is sent:
 * 200 to the current token and 401 to anything else.
 */
public class RequestAuthorizerTest extends UITestBase {

    private static final String API = "https://api.example.com";
    private static final String ELSEWHERE = "https://other.example.org";

    /** Hands out one token and replaces it on request, counting how often it was asked. */
    private static class FakeAuthorizer implements RequestAuthorizer {
        String token = "T1";
        String next = "T2";
        boolean renew = true;
        final List<String> refused = Collections.synchronizedList(new ArrayList<String>());
        AsyncResource<Boolean> pending;

        public String getAuthorization(ConnectionRequest request) {
            return token == null ? null : "Bearer " + token;
        }

        public AsyncResource<Boolean> refreshAuthorization(ConnectionRequest request,
                String rejected) {
            refused.add(rejected);
            if (pending != null) {
                return pending;
            }
            AsyncResource<Boolean> out = new AsyncResource<Boolean>();
            if (renew) {
                token = next;
            }
            out.complete(Boolean.valueOf(renew));
            return out;
        }
    }

    private final List<String> seenByApi = Collections.synchronizedList(new ArrayList<String>());
    private final List<String> seenElsewhere = Collections.synchronizedList(new ArrayList<String>());
    private volatile String accepted = "Bearer T2";
    private FakeAuthorizer authorizer;

    @BeforeEach
    void serve() {
        seenByApi.clear();
        seenElsewhere.clear();
        accepted = "Bearer T2";
        authorizer = new FakeAuthorizer();
        TestCodenameOneImplementation.getInstance().setNetworkMockHandler(
                new TestCodenameOneImplementation.NetworkMockHandler() {
                    public void handle(TestCodenameOneImplementation.TestConnection c) {
                        String sent = c.getHeaders().get("Authorization");
                        if (sent == null) {
                            sent = c.getHeaders().get("authorization");
                        }
                        c.clearRequest();
                        if (c.getUrl().startsWith(API)) {
                            seenByApi.add(String.valueOf(sent));
                            if (accepted.equals(sent)) {
                                c.respond(200, "OK", utf8("welcome"));
                            } else {
                                c.respond(401, "Unauthorized", utf8("denied"));
                            }
                        } else {
                            seenElsewhere.add(String.valueOf(sent));
                            c.respond(200, "OK", utf8("public"));
                        }
                    }
                });
    }

    @AfterEach
    void forget() {
        NetworkManager.getInstance().setAuthorizer(API, null);
        NetworkManager.getInstance().setAuthorizer(API + "/v1", null);
        TestCodenameOneImplementation.getInstance().clearNetworkMocks();
    }

    private static byte[] utf8(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static String text(byte[] b) {
        try {
            return b == null ? null : new String(b, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /** A quiet GET whose deliveries are counted. */
    private static final class Probe extends ConnectionRequest {
        final List<Integer> delivered = Collections.synchronizedList(new ArrayList<Integer>());
        final List<Integer> errorCodes = Collections.synchronizedList(new ArrayList<Integer>());

        Probe(String url) {
            setUrl(url);
            setPost(false);
            setReadResponseForErrors(true);
            setDuplicateSupported(true);
            addResponseListener(new ActionListener<NetworkEvent>() {
                public void actionPerformed(NetworkEvent evt) {
                    delivered.add(Integer.valueOf(getResponseCode()));
                }
            });
        }

        protected void handleErrorResponseCode(int code, String message) {
            errorCodes.add(Integer.valueOf(code));
        }
    }

    private Probe send(Probe p) {
        NetworkManager.getInstance().addToQueueAndWait(p);
        DisplayTest.flushEdt();
        return p;
    }

    // ---- which requests get the header --------------------------------

    @Test
    void theDefaultAuthorizerCoversItsBaseUrlAndNothingElse() {
        accepted = "Bearer T1";
        NetworkManager.getInstance().setAuthorizer(API, authorizer);

        send(new Probe(API + "/pets"));
        send(new Probe(ELSEWHERE + "/pets"));

        assertEquals(Collections.singletonList("Bearer T1"), seenByApi);
        assertEquals(Collections.singletonList("null"), seenElsewhere,
                "the token must never leave for another host");
    }

    @Test
    void aBaseUrlIsMatchedBySegmentsNotByPrefix() {
        NetworkManager nm = NetworkManager.getInstance();
        nm.setAuthorizer(API + "/v1/", authorizer);

        assertSame(authorizer, nm.getAuthorizer(API + "/v1"));
        assertSame(authorizer, nm.getAuthorizer(API + "/v1/pets?x=1"));
        assertSame(authorizer, nm.getAuthorizer("HTTPS://API.EXAMPLE.COM/v1/pets"));
        assertNull(nm.getAuthorizer(API + "/v10/pets"));
        assertNull(nm.getAuthorizer(API + "/V1/pets"), "a path is case sensitive");
        assertNull(nm.getAuthorizer(API));
        assertNull(nm.getAuthorizer("https://api.example.com.evil.test/v1"));
        assertNull(nm.getAuthorizer("https://api.example.com@evil.test/v1"));
        assertNull(nm.getAuthorizer("http://api.example.com/v1"));
        assertNull(nm.getAuthorizer("https://api.example.com:8443/v1"));
        assertNull(nm.getAuthorizer(null));
    }

    @Test
    void theLongestBaseUrlWinsAndNullRemovesARegistration() {
        NetworkManager nm = NetworkManager.getInstance();
        FakeAuthorizer narrow = new FakeAuthorizer();
        nm.setAuthorizer(API, authorizer);
        nm.setAuthorizer(API + "/v1", narrow);

        assertSame(narrow, nm.getAuthorizer(API + "/v1/pets"));
        assertSame(authorizer, nm.getAuthorizer(API + "/v2/pets"));

        nm.setAuthorizer(API + "/v1", null);
        assertSame(authorizer, nm.getAuthorizer(API + "/v1/pets"));
        assertThrows(IllegalArgumentException.class, () -> nm.setAuthorizer("api.example.com", authorizer));
    }

    @Test
    void aHeaderTheCallerSetWinsAndIsNeverRenewed() {
        NetworkManager.getInstance().setAuthorizer(API, authorizer);
        Probe p = new Probe(API + "/pets");
        p.addRequestHeader("authorization", "Basic abc");

        send(p);

        assertEquals(Collections.singletonList("Basic abc"), seenByApi);
        assertTrue(authorizer.refused.isEmpty(), "not this authorizer's header to renew");
        assertEquals(Collections.singletonList(Integer.valueOf(401)), p.delivered);
        assertEquals(Collections.singletonList(Integer.valueOf(401)), p.errorCodes);
    }

    @Test
    void noneKeepsTheDefaultAwayFromOneRequest() {
        NetworkManager.getInstance().setAuthorizer(API, authorizer);
        Probe p = new Probe(API + "/pets");
        p.setAuthorizer(RequestAuthorizer.NONE);

        send(p);

        assertEquals(Collections.singletonList("null"), seenByApi);
        assertTrue(authorizer.refused.isEmpty());
    }

    @Test
    void anAuthorizerSetOnTheRequestNeedsNoRegistration() {
        accepted = "Bearer T1";
        Probe p = new Probe(API + "/pets");
        p.setAuthorizer(authorizer);
        assertSame(authorizer, p.getAuthorizer());

        send(p);

        assertEquals(Collections.singletonList("Bearer T1"), seenByApi);
        assertEquals(Collections.singletonList(Integer.valueOf(200)), p.delivered);
    }

    // ---- 401 ----------------------------------------------------------

    @Test
    void aRefusedTokenIsRenewedAndTheRequestDeliveredOnce() {
        NetworkManager.getInstance().setAuthorizer(API, authorizer);

        Probe p = send(new Probe(API + "/pets"));

        assertEquals(java.util.Arrays.asList("Bearer T1", "Bearer T2"), seenByApi);
        assertEquals(Collections.singletonList("Bearer T1"), authorizer.refused);
        assertEquals(Collections.singletonList(Integer.valueOf(200)), p.delivered,
                "the 401 that was held must not reach the listeners");
        assertTrue(p.errorCodes.isEmpty(), "nor the error handling: " + p.errorCodes);
        assertEquals("welcome", text(p.getResponseData()));
    }

    @Test
    void aSecond401IsDeliveredAndNotRenewedAgain() {
        accepted = "Bearer never";
        NetworkManager.getInstance().setAuthorizer(API, authorizer);

        Probe p = send(new Probe(API + "/pets"));

        assertEquals(java.util.Arrays.asList("Bearer T1", "Bearer T2"), seenByApi);
        assertEquals(1, authorizer.refused.size(), "one renewal per request");
        assertEquals(Collections.singletonList(Integer.valueOf(401)), p.delivered);
        assertEquals(Collections.singletonList(Integer.valueOf(401)), p.errorCodes);
        assertEquals("denied", text(p.getResponseData()));
    }

    @Test
    void whenRenewingFailsTheRefusalIsDeliveredAsTheServiceSentIt() {
        authorizer.renew = false;
        NetworkManager.getInstance().setAuthorizer(API, authorizer);

        Probe p = send(new Probe(API + "/pets"));

        // Sent again as it was refused, so the 401 goes through the ordinary handling.
        assertEquals(java.util.Arrays.asList("Bearer T1", "Bearer T1"), seenByApi);
        assertEquals(Collections.singletonList(Integer.valueOf(401)), p.delivered);
        assertEquals(Collections.singletonList(Integer.valueOf(401)), p.errorCodes);
    }

    @Test
    void theRefusalIsStillDeliveredWhenTheAuthorizerHasDroppedItsToken() {
        NetworkManager.getInstance().setAuthorizer(API, new FakeAuthorizer() {
            public AsyncResource<Boolean> refreshAuthorization(ConnectionRequest r, String rejected) {
                refused.add(rejected);
                token = null;
                return null;
            }
        });

        Probe p = send(new Probe(API + "/pets"));

        assertEquals(java.util.Arrays.asList("Bearer T1", "Bearer T1"), seenByApi);
        assertEquals(Collections.singletonList(Integer.valueOf(401)), p.delivered);
    }

    @Test
    void aRequestQueuedAgainGetsItsRenewalAgain() {
        NetworkManager.getInstance().setAuthorizer(API, authorizer);
        Probe p = send(new Probe(API + "/pets"));
        assertEquals(Collections.singletonList(Integer.valueOf(200)), p.delivered);

        // The service rotates what it accepts; the same request object is reused.
        accepted = "Bearer T3";
        authorizer.next = "T3";
        // From the EDT, where waiting is by completion event. Off it the wait reads a flag
        // the first run left set, and returns before the request has been sent again.
        final Probe again = p;
        Display.getInstance().callSeriallyAndWait(new Runnable() {
            public void run() {
                NetworkManager.getInstance().addToQueueAndWait(again);
            }
        }, 20000);
        DisplayTest.flushEdt();

        assertEquals(java.util.Arrays.asList("Bearer T1", "Bearer T2", "Bearer T2", "Bearer T3"),
                seenByApi);
        assertEquals(java.util.Arrays.asList(Integer.valueOf(200), Integer.valueOf(200)), p.delivered);
    }

    @Test
    void anAuthorizerThatThrowsLeavesTheRequestToTheService() {
        NetworkManager.getInstance().setAuthorizer(API, new RequestAuthorizer() {
            public String getAuthorization(ConnectionRequest request) {
                throw new IllegalStateException("broken");
            }

            public AsyncResource<Boolean> refreshAuthorization(ConnectionRequest r, String rejected) {
                throw new AssertionError("a request it added nothing to is not its to renew");
            }
        });

        Probe p = send(new Probe(API + "/pets"));

        assertEquals(Collections.singletonList("null"), seenByApi);
        assertEquals(Collections.singletonList(Integer.valueOf(401)), p.delivered);
    }

    // ---- callers that wait -------------------------------------------

    @Test
    void aBlockingFetchOffTheEdtReturnsTheRenewedAnswer() {
        NetworkManager.getInstance().setAuthorizer(API, authorizer);

        Response<String> r = Rest.get(API + "/pets").getAsString();

        assertEquals(200, r.getResponseCode());
        assertEquals("welcome", r.getResponseData());
        assertEquals(java.util.Arrays.asList("Bearer T1", "Bearer T2"), seenByApi);
    }

    @Test
    void aBlockingFetchOnTheEdtReturnsTheRenewedAnswer() {
        NetworkManager.getInstance().setAuthorizer(API, authorizer);
        final Response[] out = new Response[1];

        Display.getInstance().callSeriallyAndWait(new Runnable() {
            public void run() {
                out[0] = Rest.get(API + "/pets").getAsString();
            }
        }, 20000);

        assertNotNull(out[0], "the blocking call never returned");
        assertEquals(200, out[0].getResponseCode());
        assertEquals("welcome", out[0].getResponseData());
    }

    @Test
    void theBuilderTakesAnAuthorizerOfItsOwn() {
        Response<String> r = Rest.get(API + "/pets").authorizer(authorizer).getAsString();

        assertEquals(200, r.getResponseCode());
        assertEquals(java.util.Arrays.asList("Bearer T1", "Bearer T2"), seenByApi);
    }

    @Test
    void anAsyncFetchResolvesOnceWithTheFinalAnswer() {
        NetworkManager.getInstance().setAuthorizer(API, authorizer);
        final List<Integer> completions = Collections.synchronizedList(new ArrayList<Integer>());
        Probe p = new Probe(API + "/pets");

        AsyncResource<ConnectionRequest> done = NetworkManager.getInstance().addToQueueAsync(p);
        long deadline = System.currentTimeMillis() + 20000;
        while (!done.isDone() && System.currentTimeMillis() < deadline) {
            DisplayTest.flushEdt();
        }
        completions.add(Integer.valueOf(p.getResponseCode()));
        DisplayTest.flushEdt();

        assertTrue(done.isDone());
        assertEquals(Collections.singletonList(Integer.valueOf(200)), completions);
        assertEquals(Collections.singletonList(Integer.valueOf(200)), p.delivered);
    }

    @Test
    void aRequestKilledWhileItsTokenIsRenewedReleasesWhoeverWaits() throws Exception {
        authorizer.pending = new AsyncResource<Boolean>();
        NetworkManager.getInstance().setAuthorizer(API, authorizer);
        final Probe p = new Probe(API + "/pets");
        final boolean[] returned = new boolean[1];
        Thread waiter = new Thread(new Runnable() {
            public void run() {
                NetworkManager.getInstance().addToQueueAndWait(p);
                returned[0] = true;
            }
        });
        waiter.start();
        long deadline = System.currentTimeMillis() + 20000;
        while (authorizer.refused.isEmpty() && System.currentTimeMillis() < deadline) {
            DisplayTest.flushEdt();
        }
        assertEquals(1, authorizer.refused.size(), "the request never reached its renewal");

        p.kill();
        authorizer.pending.complete(Boolean.TRUE);
        DisplayTest.flushEdt();
        waiter.join(20000);

        assertTrue(returned[0], "the waiter is stuck on a request nobody will send again");
        assertEquals(Collections.singletonList("Bearer T1"), seenByApi, "a killed request is not sent");
        assertTrue(p.delivered.isEmpty());
    }
}
