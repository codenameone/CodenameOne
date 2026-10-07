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
import java.util.Arrays;
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
    void authorizationCoverageUsesNormalizedPaths() {
        NetworkManager nm = NetworkManager.getInstance();
        String base = API + "/api";
        nm.setAuthorizer(base, authorizer);
        try {
            for (String path : new String[] {"/api/../public/upload", "/api/%2e%2e/public/upload",
                    "/api/.%2E/public/upload", "/api/%2E./public/upload", "/api/a/../../public",
                    "/api/%2e%2e%2fpublic", "/api/..\\public", "/api//../public",
                    "/api/%252e%252e/public"}) {
                assertNull(nm.getAuthorizer(API + path), path);
                assertFalse(NetworkManager.covers(base, API + path), path);
                seenByApi.clear();
                send(new Probe(API + path));
                assertEquals(Collections.singletonList("null"), seenByApi, path);
            }
            assertSame(authorizer, nm.getAuthorizer(API + "/api/a/../pets"));
            assertSame(authorizer, nm.getAuthorizer(API + "/public/../api/pets"));
            assertSame(authorizer, nm.getAuthorizer(API + "/api/./pets?next=/../public"));
        } finally {
            nm.setAuthorizer(base, null);
        }
    }

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
    void aPortLeftOutIsTheSchemesOwn() {
        NetworkManager nm = NetworkManager.getInstance();
        nm.setAuthorizer(API, authorizer);
        try {
            assertSame(authorizer, nm.getAuthorizer("https://api.example.com:443/pets"));
            assertSame(authorizer, nm.getAuthorizer("HTTPS://Api.Example.COM:0443/pets"));
            assertSame(authorizer, nm.getAuthorizer("https://api.example.com./pets"));
            assertSame(authorizer, nm.getAuthorizer("https://api.example.com:443"));
            assertSame(authorizer, nm.getAuthorizer("https://api.example.com?x=1"));
            // Another port, another scheme, and the other scheme's default port.
            assertNull(nm.getAuthorizer("https://api.example.com:8443/pets"));
            assertNull(nm.getAuthorizer("https://api.example.com:80/pets"));
            assertNull(nm.getAuthorizer("http://api.example.com/pets"));
            assertNull(nm.getAuthorizer("http://api.example.com:443/pets"));
            // What only looks like the host.
            assertNull(nm.getAuthorizer("https://api.example.com:443@evil.test/pets"));
            assertNull(nm.getAuthorizer("https://api.example.com.evil.test:443/pets"));
            assertNull(nm.getAuthorizer("https://evil.test/https://api.example.com/pets"));
            assertNull(nm.getAuthorizer("https://evil.test?https://api.example.com:443/"));

            // Registered with the port written, found without it -- and the
            // same registration, so naming it either way replaces or removes it.
            FakeAuthorizer other = new FakeAuthorizer();
            nm.setAuthorizer("https://API.example.com:443/", other);
            assertSame(other, nm.getAuthorizer(API + "/pets"));
            nm.setAuthorizer("https://api.example.com:443", null);
            assertNull(nm.getAuthorizer(API + "/pets"));

            nm.setAuthorizer("http://plain.example.com:80/api", authorizer);
            assertSame(authorizer, nm.getAuthorizer("http://plain.example.com/api/x"));
            assertSame(authorizer, nm.getAuthorizer("http://PLAIN.example.com:80/api"));
            assertNull(nm.getAuthorizer("https://plain.example.com/api/x"));
            assertNull(nm.getAuthorizer("http://plain.example.com:8080/api/x"));
        } finally {
            nm.setAuthorizer("http://plain.example.com/api", null);
        }
        assertNull(nm.getAuthorizer("http://plain.example.com/api/x"));
    }

    @Test
    void aPathPrefixEndsWhereASegmentEnds() {
        NetworkManager nm = NetworkManager.getInstance();
        nm.setAuthorizer(API + "/api", authorizer);
        try {
            assertSame(authorizer, nm.getAuthorizer(API + "/api"));
            assertSame(authorizer, nm.getAuthorizer(API + "/api/"));
            assertSame(authorizer, nm.getAuthorizer(API + "/api/pets/7"));
            assertSame(authorizer, nm.getAuthorizer(API + "/api?x=1"));
            assertSame(authorizer, nm.getAuthorizer(API + "/api#top"));
            assertSame(authorizer, nm.getAuthorizer("https://api.example.com:443/api/pets"));
            assertNull(nm.getAuthorizer(API + "/apiary"));
            assertNull(nm.getAuthorizer(API + "/api-v2/pets"));
            assertNull(nm.getAuthorizer(API + "/api.json"));
            assertNull(nm.getAuthorizer(API + "/API/pets"));
            assertNull(nm.getAuthorizer(API + "/x/api/pets"));
            assertNull(nm.getAuthorizer(API + "?/api/pets"));
            assertNull(nm.getAuthorizer(API));
        } finally {
            nm.setAuthorizer(API + "/api/", null);
        }
        assertNull(nm.getAuthorizer(API + "/api/pets"));
    }

    @Test
    void anOriginIsWrittenOneWay() {
        assertEquals("https://api.example.com:443", NetworkManager.originOf("HTTPS://API.Example.com/a/B"));
        assertEquals("https://api.example.com:443", NetworkManager.originOf("https://api.example.com:443"));
        assertEquals("http://h:80", NetworkManager.originOf("http://H?x"));
        assertEquals("wss://h:443", NetworkManager.originOf("WSS://h#f"));
        assertEquals("http://h:8080", NetworkManager.originOf("http://h:8080/"));
        assertEquals("https://[::1]:443", NetworkManager.originOf("https://[::1]/x"));
        assertEquals("https://[::1]:8443", NetworkManager.originOf("https://[::1]:8443/x"));
        assertEquals("custom://host:", NetworkManager.originOf("custom://Host/x"));
        assertEquals("https://user:pw@evil.test:443",
                NetworkManager.originOf("https://user:pw@Evil.test/x"));
        assertEquals("/relative", NetworkManager.originOf("/relative"));
        assertEquals("", NetworkManager.originOf(null));
        assertEquals("/a/B?c#d", NetworkManager.pathOf("https://h:1/a/B?c#d"));
        assertEquals("", NetworkManager.pathOf("https://h"));
    }

    @Test
    void aRedirectToAnotherOriginGoesWithoutTheToken() {
        accepted = "Bearer T1";
        final List<String> hops = Collections.synchronizedList(new ArrayList<String>());
        TestCodenameOneImplementation.getInstance().setNetworkMockHandler(
                new TestCodenameOneImplementation.NetworkMockHandler() {
                    public void handle(TestCodenameOneImplementation.TestConnection c) {
                        String sent = c.getHeaders().get("Authorization");
                        c.clearRequest();
                        hops.add(c.getUrl() + " " + sent);
                        if (c.getUrl().equals(API + "/away")) {
                            c.setHeader("location", ELSEWHERE + "/landing");
                            c.respond(302, "Found", utf8(""));
                        } else if (c.getUrl().equals(API + "/same")) {
                            // The same origin, spelled with its port.
                            c.setHeader("location", "https://API.example.com:443/pets");
                            c.respond(302, "Found", utf8(""));
                        } else if (c.getUrl().equals(API + "/downgrade")) {
                            c.setHeader("location", "http://api.example.com/pets");
                            c.respond(302, "Found", utf8(""));
                        } else {
                            c.respond(200, "OK", utf8("arrived"));
                        }
                    }
                });
        NetworkManager.getInstance().setAuthorizer(API, authorizer);

        Probe away = send(new Probe(API + "/away"));
        assertEquals(Arrays.asList(API + "/away Bearer T1", ELSEWHERE + "/landing null"), hops,
                "the token followed a redirect to another host");
        assertEquals(Collections.singletonList(Integer.valueOf(200)), away.delivered);

        hops.clear();
        send(new Probe(API + "/same"));
        assertEquals(Arrays.asList(API + "/same Bearer T1",
                "https://API.example.com:443/pets Bearer T1"), hops);

        hops.clear();
        send(new Probe(API + "/downgrade"));
        assertEquals(Arrays.asList(API + "/downgrade Bearer T1",
                "http://api.example.com/pets null"), hops, "the token left over plain http");

        // An authorizer set on the request itself is held to the first origin too.
        NetworkManager.getInstance().setAuthorizer(API, null);
        for (String path : new String[] {"/away", "/downgrade"}) {
            hops.clear();
            Probe own = new Probe(API + path);
            own.setAuthorizer(authorizer);
            send(own);
            assertEquals(2, hops.size(), hops.toString());
            assertEquals(API + path + " Bearer T1", hops.get(0));
            assertTrue(hops.get(1).endsWith(" null"), hops.get(1));
        }
        hops.clear();
        Probe own = new Probe(API + "/same");
        own.setAuthorizer(authorizer);
        send(own);
        assertEquals("https://API.example.com:443/pets Bearer T1", hops.get(1));
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
    @Test
    void cancellationCompletesAProactivelyHeldRequestWithoutWaitingForRenewal() throws Exception {
        NetworkManager.getInstance().start();
        final AsyncResource<Boolean> renewal = new AsyncResource<Boolean>();
        final java.util.concurrent.CountDownLatch held = new java.util.concurrent.CountDownLatch(2);
        RequestAuthorizer.Proactive pending = new RequestAuthorizer.Proactive() {
            public AsyncResource<Boolean> prepareAuthorization(ConnectionRequest request) {
                held.countDown();
                return renewal;
            }
            public String getAuthorization(ConnectionRequest request) { return "Bearer T2"; }
            public AsyncResource<Boolean> refreshAuthorization(ConnectionRequest request, String rejected) {
                return renewal;
            }
        };
        final Probe synchronous = new Probe(API + "/sync");
        synchronous.setAuthorizer(pending);
        Probe asynchronous = new Probe(API + "/async");
        asynchronous.setAuthorizer(pending);
        Thread waiter = new Thread(() -> NetworkManager.getInstance().addToQueueAndWait(synchronous));
        waiter.setDaemon(true);
        waiter.start();
        AsyncResource<ConnectionRequest> done = NetworkManager.getInstance().addToQueueAsync(asynchronous);
        try {
            assertTrue(held.await(10, java.util.concurrent.TimeUnit.SECONDS));
            DisplayTest.flushEdt();
            asynchronous.kill();
            synchronous.kill();
            DisplayTest.flushEdt();
            waiter.join(2000);
            assertFalse(waiter.isAlive(), "cancelled synchronous request is still waiting for renewal");
            assertTrue(done.isDone(), "cancelled async request is still waiting for renewal");
            assertFalse(renewal.isDone(), "cancelling a request must not cancel a shared renewal");
            assertTrue(seenByApi.isEmpty());
        } finally {
            renewal.complete(Boolean.TRUE);
            DisplayTest.flushEdt();
            waiter.join(2000);
        }
        assertTrue(seenByApi.isEmpty(), "late renewal must not send a cancelled request");
    }

    // ---- threads -------------------------------------------------------

    /** Renews ahead of time once, then on a 401, and notes the thread of every call it gets. */
    private static final class ThreadNotingAuthorizer implements RequestAuthorizer.Proactive {
        final List<String> offTheEdt = Collections.synchronizedList(new ArrayList<String>());
        final List<String> calls = Collections.synchronizedList(new ArrayList<String>());
        // Plain fields, as the contract allows: every call is on one thread.
        String token = "T0";
        boolean preparedOnce;

        private void note(String method) {
            calls.add(method);
            if (!Display.getInstance().isEdt()) {
                offTheEdt.add(method + " on " + Thread.currentThread().getName());
            }
        }

        public String getAuthorization(ConnectionRequest request) {
            note("getAuthorization");
            return "Bearer " + token;
        }

        public AsyncResource<Boolean> prepareAuthorization(ConnectionRequest request) {
            note("prepareAuthorization");
            if (preparedOnce) {
                return null;
            }
            preparedOnce = true;
            token = "T1";
            // Not done yet, so the request is really held and released later.
            final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    out.complete(Boolean.TRUE);
                }
            });
            return out;
        }

        public AsyncResource<Boolean> refreshAuthorization(ConnectionRequest request,
                String rejected) {
            note("refreshAuthorization");
            token = "T2";
            AsyncResource<Boolean> out = new AsyncResource<Boolean>();
            out.complete(Boolean.TRUE);
            return out;
        }
    }

    @Test
    void anAuthorizerIsNeverCalledOffTheEdt() throws Exception {
        assertFalse(Display.getInstance().isEdt(), "this test has to queue from another thread");
        final ThreadNotingAuthorizer noting = new ThreadNotingAuthorizer();
        NetworkManager.getInstance().setAuthorizer(API, noting);

        // Held before it is sent, refused with T1, renewed, and sent again with T2 -- every
        // step an authorizer takes part in, for a request queued from this thread.
        Probe first = send(new Probe(API + "/pets"));
        assertEquals(Collections.singletonList(Integer.valueOf(200)), first.delivered);
        assertEquals(Arrays.asList("Bearer T1", "Bearer T2"), seenByApi);

        // And from threads of their own, with one set on the request too.
        final Probe second = new Probe(API + "/owners");
        final Probe third = new Probe(ELSEWHERE + "/visits");
        third.setAuthorizer(noting);
        Thread[] queueing = new Thread[] {
            new Thread(new Runnable() {
                public void run() {
                    NetworkManager.getInstance().addToQueueAndWait(second);
                }
            }, "queues-second"),
            new Thread(new Runnable() {
                public void run() {
                    NetworkManager.getInstance().addToQueueAndWait(third);
                }
            }, "queues-third")
        };
        for (Thread t : queueing) {
            t.start();
        }
        for (Thread t : queueing) {
            long deadline = System.currentTimeMillis() + 20000;
            while (t.isAlive() && System.currentTimeMillis() < deadline) {
                DisplayTest.flushEdt();
                t.join(20);
            }
            assertFalse(t.isAlive(), t.getName() + " never returned");
        }
        DisplayTest.flushEdt();

        assertEquals(Collections.singletonList("Bearer T2"), seenElsewhere,
                "the header captured on the EDT is the one the network thread sent");
        assertTrue(noting.calls.contains("getAuthorization")
                && noting.calls.contains("prepareAuthorization")
                && noting.calls.contains("refreshAuthorization"),
                "every method must have been exercised: " + noting.calls);
        assertTrue(noting.calls.size() >= 6, "too few calls to mean anything: " + noting.calls);
        assertEquals(Collections.emptyList(), noting.offTheEdt,
                "an authorizer's state belongs to the EDT; it was called from another thread");
    }

    @Test
    void theRegistrationsAreReadAndChangedOnTheEdtWhoeverAsks() {
        assertFalse(Display.getInstance().isEdt());
        final List<String> offTheEdt = Collections.synchronizedList(new ArrayList<String>());
        RequestAuthorizer watching = new RequestAuthorizer() {
            public String getAuthorization(ConnectionRequest request) {
                if (!Display.getInstance().isEdt()) {
                    offTheEdt.add(Thread.currentThread().getName());
                }
                return "Bearer T2";
            }

            public AsyncResource<Boolean> refreshAuthorization(ConnectionRequest request,
                    String rejected) {
                return null;
            }
        };
        NetworkManager nm = NetworkManager.getInstance();

        // Registered and read back from this thread, at once: the call waits for the EDT.
        nm.setAuthorizer(API + "/v1", watching);
        assertSame(watching, nm.getAuthorizer(API + "/v1/pets"));
        send(new Probe(API + "/v1/pets"));
        nm.setAuthorizer(API + "/v1", null);
        assertNull(nm.getAuthorizer(API + "/v1/pets"));
        send(new Probe(API + "/v1/pets"));

        assertEquals(Arrays.asList("Bearer T2", "null"), seenByApi);
        assertEquals(Collections.emptyList(), offTheEdt);
    }
}
