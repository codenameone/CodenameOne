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

import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkEvent;
import com.codename1.io.NetworkManager;
import com.codename1.io.rest.Response;
import com.codename1.io.rest.Rest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.Display;
import com.codename1.ui.DisplayTest;
import com.codename1.ui.events.ActionListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.codename1.io.oidc.OidcTestSupport.await;
import static com.codename1.io.oidc.OidcTestSupport.text;
import static com.codename1.io.oidc.OidcTestSupport.tokenJson;
import static com.codename1.io.oidc.OidcTestSupport.tokens;
import static com.codename1.io.oidc.OidcTestSupport.utf8;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link OidcRequestAuthorizer} against a service that accepts one access token and a token
 * endpoint that exchanges one refresh token: the renewal, its sharing between requests refused
 * together, and the end of a session.
 */
public class OidcRequestAuthorizerTest extends UITestBase {

    private static final String ISSUER = "https://login.example.com";
    private static final String TOKEN_EP = ISSUER + "/oauth2/token";
    private static final String API = "https://api.example.com";

    private final List<String> seenByApi = Collections.synchronizedList(new ArrayList<String>());
    private final List<String> tokenRequests = Collections.synchronizedList(new ArrayList<String>());
    private final List<String> tokenRequestHeaders = Collections.synchronizedList(new ArrayList<String>());
    private volatile String accepted;
    private volatile int tokenStatus;
    private volatile String tokenBody;
    private OidcTestSupport.MemoryStore store;
    private OidcClient client;
    private OidcRequestAuthorizer authorizer;

    @BeforeEach
    void serve() {
        seenByApi.clear();
        tokenRequests.clear();
        tokenRequestHeaders.clear();
        accepted = "Bearer AT-2";
        tokenStatus = 200;
        tokenBody = tokenJson("AT-2", "RT-2");
        store = new OidcTestSupport.MemoryStore();
        client = OidcClient.create(OidcConfiguration.newBuilder()
                        .issuer(ISSUER)
                        .authorizationEndpoint(ISSUER + "/oauth2/authorize")
                        .tokenEndpoint(TOKEN_EP)
                        .build())
                .setClientId("app")
                .setTokenStore(store);
        authorizer = new OidcRequestAuthorizer(client).install(API);
        TestCodenameOneImplementation.getInstance().setNetworkMockHandler(
                new TestCodenameOneImplementation.NetworkMockHandler() {
                    public void handle(TestCodenameOneImplementation.TestConnection c) {
                        String sent = c.getHeaders().get("Authorization");
                        String body = text(c.getOutputData());
                        c.clearRequest();
                        if (c.getUrl().startsWith(TOKEN_EP)) {
                            tokenRequests.add(body);
                            tokenRequestHeaders.add(String.valueOf(sent));
                            if (tokenStatus < 0) {
                                throw new IllegalStateException("offline");
                            }
                            c.respond(tokenStatus, tokenStatus == 200 ? "OK" : "Bad Request",
                                    utf8(tokenBody));
                        } else {
                            seenByApi.add(String.valueOf(sent));
                            if (accepted.equals(sent)) {
                                c.respond(200, "OK", utf8("welcome"));
                            } else {
                                c.respond(401, "Unauthorized", utf8("denied"));
                            }
                        }
                    }
                });
    }

    @AfterEach
    void forget() {
        NetworkManager.getInstance().setAuthorizer(API, null);
        NetworkManager.getInstance().setAuthorizer(ISSUER, null);
        TestCodenameOneImplementation.getInstance().clearNetworkMocks();
    }

    private static final class Probe extends ConnectionRequest {
        final List<Integer> delivered = Collections.synchronizedList(new ArrayList<Integer>());

        Probe(String url) {
            setUrl(url);
            setPost(false);
            setReadResponseForErrors(true);
            setDuplicateSupported(true);
            setFailSilently(true);
            addResponseListener(new ActionListener<NetworkEvent>() {
                public void actionPerformed(NetworkEvent evt) {
                    delivered.add(Integer.valueOf(getResponseCode()));
                }
            });
        }
    }

    private void settle(Probe... probes) {
        long deadline = System.currentTimeMillis() + 20000;
        boolean done = false;
        while (!done && System.currentTimeMillis() < deadline) {
            DisplayTest.flushEdt();
            done = true;
            for (Probe p : probes) {
                done &= !p.delivered.isEmpty();
            }
        }
        DisplayTest.flushEdt();
        assertTrue(done, "a request was never delivered");
    }

    @Test
    void nothingIsSentBeforeAnyoneSignedIn() {
        assertFalse(authorizer.isSignedIn());

        Probe p = new Probe(API + "/pets");
        NetworkManager.getInstance().addToQueue(p);
        settle(p);

        assertEquals(Collections.singletonList("null"), seenByApi);
        assertEquals(Collections.singletonList(Integer.valueOf(401)), p.delivered);
        assertTrue(tokenRequests.isEmpty(), "there is nothing to renew");
    }

    @Test
    void loadBringsBackASavedSession() {
        store.saved = tokens("AT-2", "RT-1");

        assertEquals("AT-2", await(authorizer.load()).value.getAccessToken());

        assertTrue(authorizer.isSignedIn());
        Response<String> r = Rest.get(API + "/pets").getAsString();
        assertEquals(200, r.getResponseCode());
        assertEquals(Collections.singletonList("Bearer AT-2"), seenByApi);
    }

    @Test
    void aLoadCannotUndoSignOutOrReplaceANewerSignIn() {
        for (boolean signInAgain : new boolean[] {false, true}) {
            final com.codename1.util.AsyncResource<OidcTokens> pending =
                    new com.codename1.util.AsyncResource<OidcTokens>();
            client.setTokenStore(new TokenStore() {
                public com.codename1.util.AsyncResource<OidcTokens> load(String key) {
                    return pending;
                }
                public com.codename1.util.AsyncResource<Boolean> save(String key, OidcTokens value) {
                    return store.save(key, value);
                }
                public com.codename1.util.AsyncResource<Boolean> clear(String key) {
                    return store.clear(key);
                }
            });
            com.codename1.util.AsyncResource<OidcTokens> loading = authorizer.load();
            await(authorizer.signOut());
            if (signInAgain) {
                authorizer.setTokens(tokens("NEW", "NEW-RT"));
            }
            pending.complete(tokens("STALE", "OLD-RT"));
            assertNull(await(loading).value, "an invalidated load must not return stale tokens");
            if (signInAgain) {
                assertEquals("NEW", authorizer.getTokens().getAccessToken());
            } else {
                assertFalse(authorizer.isSignedIn());
                Probe request = new Probe(API + "/pets");
                NetworkManager.getInstance().addToQueue(request);
                settle(request);
                assertEquals(Collections.singletonList("null"), seenByApi);
            }
        }
    }

    @Test
    void anExpiredAccessTokenIsRefreshedAndTheRequestSentAgain() {
        authorizer.setTokens(tokens("AT-1", "RT-1"));

        Probe p = new Probe(API + "/pets");
        NetworkManager.getInstance().addToQueue(p);
        settle(p);

        assertEquals(Arrays.asList("Bearer AT-1", "Bearer AT-2"), seenByApi);
        assertEquals(Collections.singletonList(Integer.valueOf(200)), p.delivered);
        assertEquals(1, tokenRequests.size());
        assertTrue(tokenRequests.get(0).contains("grant_type=refresh_token"), tokenRequests.get(0));
        assertTrue(tokenRequests.get(0).contains("refresh_token=RT-1"), tokenRequests.get(0));
        assertTrue(tokenRequests.get(0).contains("client_id=app"), tokenRequests.get(0));
        assertEquals("AT-2", authorizer.getTokens().getAccessToken());
        assertEquals("RT-2", authorizer.getTokens().getRefreshToken(), "the rotated one");
        assertEquals("RT-2", store.saved.getRefreshToken(), "and it is what was saved");
    }

    // ---- renewing ahead of the expiry -----------------------------------

    /** A token set that expires {@code seconds} from now. */
    private static OidcTokens expiring(String access, String refresh, int seconds) {
        java.util.Map<String, Object> json = new java.util.HashMap<String, Object>();
        json.put("access_token", access);
        json.put("token_type", "Bearer");
        json.put("expires_in", Integer.valueOf(seconds));
        if (refresh != null) {
            json.put("refresh_token", refresh);
        }
        return OidcTokens.fromTokenResponse(json, null);
    }

    @Test
    void aTokenAboutToExpireIsRenewedBeforeTheRequestIsSent() {
        // Still accepted by the service, as a token thirty seconds from its end is: the
        // request must go out with the new one all the same, and never be refused.
        authorizer.setTokens(expiring("AT-1", "RT-1", 30));

        Probe p = new Probe(API + "/pets");
        NetworkManager.getInstance().addToQueue(p);
        settle(p);

        assertEquals(Collections.singletonList("Bearer AT-2"), seenByApi,
                "sent once, with the new token, and no 401 round trip");
        assertEquals(1, tokenRequests.size());
        assertTrue(tokenRequests.get(0).contains("refresh_token=RT-1"), tokenRequests.get(0));
        assertEquals(Collections.singletonList(Integer.valueOf(200)), p.delivered);
        assertEquals("RT-2", authorizer.getTokens().getRefreshToken());

        // The new token has an hour: the next request is sent at once.
        Probe next = new Probe(API + "/owners");
        NetworkManager.getInstance().addToQueue(next);
        settle(next);
        assertEquals(1, tokenRequests.size());
        assertEquals(Arrays.asList("Bearer AT-2", "Bearer AT-2"), seenByApi);
    }

    @Test
    void anExpiredTokenIsRenewedAheadToo() {
        authorizer.setTokens(expiring("AT-1", "RT-1", -120));

        Probe p = new Probe(API + "/pets");
        NetworkManager.getInstance().addToQueue(p);
        settle(p);

        assertEquals(Collections.singletonList("Bearer AT-2"), seenByApi);
        assertEquals(1, tokenRequests.size());
    }

    @Test
    void requestsQueuedDuringTheRenewalWaitForTheSameOne() throws Exception {
        authorizer.setTokens(expiring("AT-1", "RT-1", 30));
        final java.util.concurrent.CountDownLatch atTokenEndpoint =
                new java.util.concurrent.CountDownLatch(1);
        final java.util.concurrent.CountDownLatch release =
                new java.util.concurrent.CountDownLatch(1);
        TestCodenameOneImplementation.getInstance().setNetworkMockHandler(
                new TestCodenameOneImplementation.NetworkMockHandler() {
                    public void handle(TestCodenameOneImplementation.TestConnection c) {
                        String sent = c.getHeaders().get("Authorization");
                        String body = text(c.getOutputData());
                        c.clearRequest();
                        if (c.getUrl().startsWith(TOKEN_EP)) {
                            tokenRequests.add(body);
                            atTokenEndpoint.countDown();
                            try {
                                // The network thread -- the only one -- is busy with the
                                // exchange while the others are queued.
                                release.await(15, java.util.concurrent.TimeUnit.SECONDS);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            c.respond(200, "OK", utf8(tokenJson("AT-2", "RT-2")));
                        } else {
                            seenByApi.add(c.getUrl().substring(API.length()) + " " + sent);
                            c.respond("Bearer AT-2".equals(sent) ? 200 : 401, "x", utf8("x"));
                        }
                    }
                });
        Probe a = new Probe(API + "/pets");
        Probe b = new Probe(API + "/owners");
        Probe c = new Probe(API + "/visits");

        NetworkManager.getInstance().addToQueue(a);
        long deadline = System.currentTimeMillis() + 15000;
        while (atTokenEndpoint.getCount() > 0 && System.currentTimeMillis() < deadline) {
            DisplayTest.flushEdt();
        }
        assertEquals(0, atTokenEndpoint.getCount(), "the exchange never started");
        NetworkManager.getInstance().addToQueue(b);
        NetworkManager.getInstance().addToQueue(c);
        // The same request queued again while it waits is not sent twice.
        NetworkManager.getInstance().addToQueue(a);
        DisplayTest.flushEdt();
        assertTrue(seenByApi.isEmpty(), "sent before the token was renewed: " + seenByApi);
        release.countDown();
        settle(a, b, c);

        assertEquals(1, tokenRequests.size(), "one exchange for all three: " + tokenRequests);
        List<String> sorted = new ArrayList<String>(seenByApi);
        Collections.sort(sorted);
        assertEquals(Arrays.asList("/owners Bearer AT-2", "/pets Bearer AT-2",
                "/visits Bearer AT-2"), sorted, "each sent once, with the new token");
        assertEquals(Collections.singletonList(Integer.valueOf(200)), a.delivered);
        assertEquals(Collections.singletonList(Integer.valueOf(200)), b.delivered);
        assertEquals(Collections.singletonList(Integer.valueOf(200)), c.delivered);
    }

    @Test
    void aRenewalAheadThatIsRefusedEndsTheSessionAndTheRequestIsUnauthorized() {
        authorizer.setTokens(expiring("AT-1", "RT-1", 30));
        store.saved = authorizer.getTokens();
        tokenStatus = 400;
        tokenBody = "{\"error\":\"invalid_grant\",\"error_description\":\"revoked\"}";
        final List<Throwable> told = new ArrayList<Throwable>();
        authorizer.addSignInRequiredListener(new OidcRequestAuthorizer.SignInRequiredListener() {
            public void signInRequired(OidcRequestAuthorizer source, Throwable reason) {
                told.add(reason);
            }
        });

        Probe p = new Probe(API + "/pets");
        NetworkManager.getInstance().addToQueue(p);
        settle(p);

        assertEquals(1, told.size(), "the application is told to sign in, once");
        assertEquals("invalid_grant", ((OidcException) told.get(0)).getError());
        assertEquals(Collections.singletonList(Integer.valueOf(401)), p.delivered,
                "and the request is delivered as unauthorized");
        assertEquals(Collections.singletonList("null"), seenByApi,
                "sent once, with no token: the one it had was given up");
        assertEquals(1, tokenRequests.size(), "the spent refresh token is not tried again");
        assertNull(authorizer.getTokens());
        assertNull(store.saved);
    }

    @Test
    void aRenewalAheadThatCannotReachTheServerSendsTheTokenItHas() {
        authorizer.setTokens(expiring("AT-1", "RT-1", 30));
        accepted = "Bearer AT-1";
        tokenStatus = -1;
        final List<Throwable> told = new ArrayList<Throwable>();
        authorizer.addSignInRequiredListener(new OidcRequestAuthorizer.SignInRequiredListener() {
            public void signInRequired(OidcRequestAuthorizer source, Throwable reason) {
                told.add(reason);
            }
        });

        Probe p = new Probe(API + "/pets");
        NetworkManager.getInstance().addToQueue(p);
        settle(p);
        Probe next = new Probe(API + "/owners");
        NetworkManager.getInstance().addToQueue(next);
        settle(next);

        assertTrue(told.isEmpty(), "nothing said the refresh token is bad");
        assertEquals(Arrays.asList("Bearer AT-1", "Bearer AT-1"), seenByApi);
        assertEquals(Collections.singletonList(Integer.valueOf(200)), p.delivered);
        assertEquals(Collections.singletonList(Integer.valueOf(200)), next.delivered);
        assertEquals(1, tokenRequests.size(),
                "the request right after a failure does not try ahead again");
        assertEquals("RT-1", authorizer.getTokens().getRefreshToken());
    }

    @Test
    void blockingCallersGetTheFinalAnswerOnTheEdtAndOffIt() {
        authorizer.setTokens(expiring("AT-1", "RT-1", 30));
        final Response[] out = new Response[1];
        Display.getInstance().callSeriallyAndWait(new Runnable() {
            public void run() {
                out[0] = Rest.get(API + "/pets").getAsString();
            }
        }, 20000);
        assertNotNull(out[0], "the blocking call on the EDT never returned");
        assertEquals(200, out[0].getResponseCode());
        assertEquals("welcome", out[0].getResponseData());
        assertEquals(Collections.singletonList("Bearer AT-2"), seenByApi);
        assertEquals(1, tokenRequests.size());

        // Off the EDT, from this thread: the same, for the next token.
        seenByApi.clear();
        tokenRequests.clear();
        authorizer.setTokens(expiring("AT-1", "RT-1", 30));
        Probe p = new Probe(API + "/owners");
        NetworkManager.getInstance().addToQueueAndWait(p);
        assertEquals(200, p.getResponseCode());
        assertEquals(Collections.singletonList("Bearer AT-2"), seenByApi);
        assertEquals(1, tokenRequests.size());
    }

    @Test
    void theLeewayDecidesWhatAboutToExpireMeans() {
        // Five minutes left and a leeway of one: sent as it is.
        authorizer.setTokens(expiring("AT-2", "RT-1", 300));
        Probe p = new Probe(API + "/pets");
        NetworkManager.getInstance().addToQueue(p);
        settle(p);
        assertTrue(tokenRequests.isEmpty());
        assertEquals(60, authorizer.getRefreshLeeway());

        // A leeway of ten minutes: renewed first.
        accepted = "Bearer AT-3";
        tokenBody = tokenJson("AT-3", "RT-3");
        authorizer.setRefreshLeeway(600);
        Probe q = new Probe(API + "/pets");
        NetworkManager.getInstance().addToQueue(q);
        settle(q);
        assertEquals(1, tokenRequests.size());
        assertEquals(Collections.singletonList(Integer.valueOf(200)), q.delivered);

        // Turned off: an expired token is sent, refused, and renewed the old way.
        seenByApi.clear();
        tokenRequests.clear();
        accepted = "Bearer AT-4";
        tokenBody = tokenJson("AT-4", "RT-4");
        authorizer.setRefreshLeeway(-1);
        authorizer.setTokens(expiring("AT-old", "RT-3", -60));
        Probe r = new Probe(API + "/pets");
        NetworkManager.getInstance().addToQueue(r);
        settle(r);
        assertEquals(Arrays.asList("Bearer AT-old", "Bearer AT-4"), seenByApi);
        assertEquals(Collections.singletonList(Integer.valueOf(200)), r.delivered);
    }

    @Test
    void aRequestKilledWhileItWaitsIsNeverSentAndReleasesWhoeverWaits() throws Exception {
        authorizer.setTokens(expiring("AT-1", "RT-1", 30));
        final java.util.concurrent.CountDownLatch atTokenEndpoint =
                new java.util.concurrent.CountDownLatch(1);
        final java.util.concurrent.CountDownLatch release =
                new java.util.concurrent.CountDownLatch(1);
        TestCodenameOneImplementation.getInstance().setNetworkMockHandler(
                new TestCodenameOneImplementation.NetworkMockHandler() {
                    public void handle(TestCodenameOneImplementation.TestConnection c) {
                        String sent = c.getHeaders().get("Authorization");
                        c.clearRequest();
                        if (c.getUrl().startsWith(TOKEN_EP)) {
                            atTokenEndpoint.countDown();
                            try {
                                release.await(15, java.util.concurrent.TimeUnit.SECONDS);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            c.respond(200, "OK", utf8(tokenJson("AT-2", "RT-2")));
                        } else {
                            seenByApi.add(String.valueOf(sent));
                            c.respond(200, "OK", utf8("welcome"));
                        }
                    }
                });
        final Probe p = new Probe(API + "/pets");
        final boolean[] returned = new boolean[1];
        Thread waiter = new Thread(new Runnable() {
            public void run() {
                NetworkManager.getInstance().addToQueueAndWait(p);
                returned[0] = true;
            }
        });
        waiter.start();
        long deadline = System.currentTimeMillis() + 15000;
        while (atTokenEndpoint.getCount() > 0 && System.currentTimeMillis() < deadline) {
            DisplayTest.flushEdt();
        }
        assertEquals(0, atTokenEndpoint.getCount(), "the exchange never started");
        p.kill();
        release.countDown();
        deadline = System.currentTimeMillis() + 15000;
        while (!returned[0] && System.currentTimeMillis() < deadline) {
            DisplayTest.flushEdt();
            Thread.sleep(10);
        }
        waiter.join(2000);
        assertTrue(returned[0], "whoever waited for the killed request is still waiting");
        assertTrue(seenByApi.isEmpty(), "a killed request was sent: " + seenByApi);
    }

    @Test
    void theClientsOwnRequestsNeverCarryTheApplicationsToken() {
        // The issuer is under an authorizer's base URL too, as it is when one server is both.
        NetworkManager.getInstance().setAuthorizer(ISSUER, authorizer);
        authorizer.setTokens(tokens("AT-1", "RT-1"));

        Probe p = new Probe(API + "/pets");
        NetworkManager.getInstance().addToQueue(p);
        settle(p);

        assertEquals(Collections.singletonList("null"), tokenRequestHeaders);
        assertEquals(Collections.singletonList(Integer.valueOf(200)), p.delivered);
    }

    @Test
    void requestsRefusedTogetherShareOneRefresh() {
        authorizer.setTokens(tokens("AT-1", "RT-1"));
        Probe a = new Probe(API + "/pets");
        Probe b = new Probe(API + "/owners");
        Probe c = new Probe(API + "/visits");

        NetworkManager.getInstance().addToQueue(a);
        NetworkManager.getInstance().addToQueue(b);
        NetworkManager.getInstance().addToQueue(c);
        settle(a, b, c);

        assertEquals(1, tokenRequests.size(),
                "a refresh token is good for one exchange: " + tokenRequests);
        assertEquals(Collections.singletonList(Integer.valueOf(200)), a.delivered);
        assertEquals(Collections.singletonList(Integer.valueOf(200)), b.delivered);
        assertEquals(Collections.singletonList(Integer.valueOf(200)), c.delivered);
    }

    @Test
    void aRefusedRefreshTokenEndsTheSession() {
        authorizer.setTokens(tokens("AT-1", "RT-1"));
        store.saved = authorizer.getTokens();
        tokenStatus = 400;
        tokenBody = "{\"error\":\"invalid_grant\",\"error_description\":\"revoked\"}";
        final List<Throwable> told = new ArrayList<Throwable>();
        final boolean[] signedInWhenTold = new boolean[1];
        authorizer.addSignInRequiredListener(new OidcRequestAuthorizer.SignInRequiredListener() {
            public void signInRequired(OidcRequestAuthorizer source, Throwable reason) {
                told.add(reason);
                signedInWhenTold[0] = source.isSignedIn();
            }
        });

        Probe p = new Probe(API + "/pets");
        NetworkManager.getInstance().addToQueue(p);
        settle(p);

        assertEquals(Collections.singletonList(Integer.valueOf(401)), p.delivered,
                "the caller gets the service's 401");
        assertEquals(Arrays.asList("Bearer AT-1", "Bearer AT-1"), seenByApi);
        assertEquals(1, told.size());
        assertInstanceOf(OidcException.class, told.get(0));
        assertEquals("invalid_grant", ((OidcException) told.get(0)).getError());
        assertFalse(signedInWhenTold[0], "the tokens are gone before the listener runs");
        assertNull(authorizer.getTokens());
        assertNull(store.saved, "and gone from the store");
        assertTrue(store.clears > 0);
    }

    @Test
    void aSessionWithoutARefreshTokenEndsAtTheFirst401() {
        authorizer.setTokens(tokens("AT-1", null));
        final List<Throwable> told = new ArrayList<Throwable>();
        authorizer.addSignInRequiredListener(new OidcRequestAuthorizer.SignInRequiredListener() {
            public void signInRequired(OidcRequestAuthorizer source, Throwable reason) {
                told.add(reason);
            }
        });

        Probe p = new Probe(API + "/pets");
        NetworkManager.getInstance().addToQueue(p);
        settle(p);

        assertEquals(Collections.singletonList(Integer.valueOf(401)), p.delivered);
        assertTrue(tokenRequests.isEmpty());
        assertEquals(1, told.size());
        assertNull(told.get(0));
        assertFalse(authorizer.isSignedIn());
    }

    @Test
    void aRefreshThatCannotReachTheServerKeepsTheSession() {
        authorizer.setTokens(tokens("AT-1", "RT-1"));
        // The token endpoint cannot be reached at all: the request fails before any answer.
        final List<Throwable> told = new ArrayList<Throwable>();
        authorizer.addSignInRequiredListener(new OidcRequestAuthorizer.SignInRequiredListener() {
            public void signInRequired(OidcRequestAuthorizer source, Throwable reason) {
                told.add(reason);
            }
        });
        tokenStatus = -1;

        Probe p = new Probe(API + "/pets");
        NetworkManager.getInstance().addToQueue(p);
        settle(p);

        assertEquals(Collections.singletonList(Integer.valueOf(401)), p.delivered);
        assertTrue(told.isEmpty(), "nothing said the refresh token is bad");
        assertEquals("RT-1", authorizer.getTokens().getRefreshToken());
    }

    @Test
    void aTokenEndpointThatFailsWithoutAnOAuthErrorKeepsTheSession() {
        authorizer.setTokens(tokens("AT-1", "RT-1"));
        store.saved = authorizer.getTokens();
        final List<Throwable> told = new ArrayList<Throwable>();
        authorizer.addSignInRequiredListener(new OidcRequestAuthorizer.SignInRequiredListener() {
            public void signInRequired(OidcRequestAuthorizer source, Throwable reason) {
                told.add(reason);
            }
        });
        // The server is failing, in words of its own. It has not said the token is bad.
        tokenStatus = 500;
        tokenBody = "{\"message\":\"failure\"}";

        Probe p = new Probe(API + "/pets");
        NetworkManager.getInstance().addToQueue(p);
        settle(p);

        assertEquals(Collections.singletonList(Integer.valueOf(401)), p.delivered);
        assertEquals(1, tokenRequests.size());
        assertTrue(told.isEmpty(), "nothing said the refresh token is bad");
        assertEquals("AT-1", authorizer.getTokens().getAccessToken(),
                "an answer with no tokens in it replaced the ones that were there");
        assertEquals("RT-1", authorizer.getTokens().getRefreshToken());
        assertEquals("AT-1", store.saved.getAccessToken());
    }

    @Test
    void signingOutDuringARenewalIsNotUndoneWhenTheRenewalSucceeds() throws Exception {
        authorizer.setTokens(expiring("AT-1", "RT-1", 30));
        store.saved = authorizer.getTokens();
        final List<Throwable> told = new ArrayList<Throwable>();
        authorizer.addSignInRequiredListener(new OidcRequestAuthorizer.SignInRequiredListener() {
            public void signInRequired(OidcRequestAuthorizer source, Throwable reason) {
                told.add(reason);
            }
        });
        final java.util.concurrent.CountDownLatch atTokenEndpoint =
                new java.util.concurrent.CountDownLatch(1);
        final java.util.concurrent.CountDownLatch release =
                new java.util.concurrent.CountDownLatch(1);
        TestCodenameOneImplementation.getInstance().setNetworkMockHandler(
                new TestCodenameOneImplementation.NetworkMockHandler() {
                    public void handle(TestCodenameOneImplementation.TestConnection c) {
                        String sent = c.getHeaders().get("Authorization");
                        String body = text(c.getOutputData());
                        c.clearRequest();
                        if (c.getUrl().startsWith(TOKEN_EP)) {
                            tokenRequests.add(body);
                            atTokenEndpoint.countDown();
                            try {
                                release.await(15, java.util.concurrent.TimeUnit.SECONDS);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            c.respond(200, "OK", utf8(tokenJson("AT-2", "RT-2")));
                        } else {
                            seenByApi.add(String.valueOf(sent));
                            c.respond("Bearer AT-2".equals(sent) ? 200 : 401, "x", utf8("x"));
                        }
                    }
                });
        Probe held = new Probe(API + "/pets");
        NetworkManager.getInstance().addToQueue(held);
        long deadline = System.currentTimeMillis() + 15000;
        while (atTokenEndpoint.getCount() > 0 && System.currentTimeMillis() < deadline) {
            DisplayTest.flushEdt();
        }
        assertEquals(0, atTokenEndpoint.getCount(), "the exchange never started");

        // The user signs out while the exchange is at the server, which then answers
        // with a perfectly good new set.
        await(authorizer.signOut());
        assertNull(store.saved);
        release.countDown();
        settle(held);
        // Long enough for the answer to have been handed over, had it been.
        for (int i = 0; i < 20; i++) {
            DisplayTest.flushEdt();
            Thread.sleep(10);
        }

        assertFalse(authorizer.isSignedIn(), "the renewal signed the user back in");
        assertNull(authorizer.getTokens());
        assertNull(store.saved, "the renewal put tokens back in the store");
        assertEquals(Collections.singletonList("null"), seenByApi,
                "the request that waited goes out with no token");
        assertEquals(Collections.singletonList(Integer.valueOf(401)), held.delivered);
        assertTrue(told.isEmpty(), "signing out is not a session the server ended");

        // And the authorizer is not left waiting on the exchange it gave up on.
        authorizer.setTokens(expiring("AT-1", "RT-1", 30));
        Probe next = new Probe(API + "/owners");
        NetworkManager.getInstance().addToQueue(next);
        settle(next);
        assertEquals(2, tokenRequests.size());
        assertEquals(Collections.singletonList(Integer.valueOf(200)), next.delivered);
        assertEquals("AT-2", authorizer.getTokens().getAccessToken());
    }

    @Test
    void no401LoopWhenTheServiceRefusesTheNewTokenToo() {
        authorizer.setTokens(tokens("AT-1", "RT-1"));
        accepted = "Bearer something-else";

        Probe p = new Probe(API + "/pets");
        NetworkManager.getInstance().addToQueue(p);
        settle(p);

        assertEquals(Arrays.asList("Bearer AT-1", "Bearer AT-2"), seenByApi);
        assertEquals(1, tokenRequests.size());
        assertEquals(Collections.singletonList(Integer.valueOf(401)), p.delivered);
    }

    @Test
    void aBlockingFetchOnTheEdtComesBackRefreshed() {
        authorizer.setTokens(tokens("AT-1", "RT-1"));
        final Response[] out = new Response[1];

        Display.getInstance().callSeriallyAndWait(new Runnable() {
            public void run() {
                out[0] = Rest.get(API + "/pets").getAsString();
            }
        }, 20000);

        assertNotNull(out[0], "the blocking call never returned");
        assertEquals(200, out[0].getResponseCode());
        assertEquals("welcome", out[0].getResponseData());
        assertEquals(1, tokenRequests.size());
    }

    @Test
    void signOutWaitsForPendingPersistenceAndThenClearsIt() {
        for (boolean failSave : new boolean[] {false, true}) {
            final com.codename1.util.AsyncResource<Boolean> pending =
                    new com.codename1.util.AsyncResource<Boolean>();
            final OidcTokens[] disk = new OidcTokens[1];
            final OidcTokens[] queued = new OidcTokens[1];
            final int[] clears = new int[1];
            client.setTokenStore(new TokenStore() {
                public com.codename1.util.AsyncResource<OidcTokens> load(String key) {
                    com.codename1.util.AsyncResource<OidcTokens> out =
                            new com.codename1.util.AsyncResource<OidcTokens>();
                    out.complete(disk[0]);
                    return out;
                }
                public com.codename1.util.AsyncResource<Boolean> save(String key, OidcTokens value) {
                    queued[0] = value;
                    return pending;
                }
                public com.codename1.util.AsyncResource<Boolean> clear(String key) {
                    clears[0]++;
                    disk[0] = null;
                    com.codename1.util.AsyncResource<Boolean> out =
                            new com.codename1.util.AsyncResource<Boolean>();
                    out.complete(Boolean.TRUE);
                    return out;
                }
            });
            assertEquals("AT-2", await(client.refresh("RT-1")).value.getAccessToken());
            assertNotNull(queued[0]);
            com.codename1.util.AsyncResource<Boolean> signedOut = authorizer.signOut();
            assertFalse(authorizer.isSignedIn());
            assertFalse(signedOut.isDone(), "sign-out completed ahead of the pending write");
            assertEquals(0, clears[0]);
            disk[0] = queued[0];
            if (failSave) {
                pending.error(new RuntimeException("save failed after writing"));
            } else {
                pending.complete(Boolean.TRUE);
            }
            assertNull(await(signedOut).error);
            assertEquals(1, clears[0]);
            assertNull(await(client.loadStoredTokens()).value);
            assertFalse(authorizer.isSignedIn());
        }
    }

    @Test
    void signOutDropsTheTokensEverywhere() {
        authorizer.setTokens(tokens("AT-2", "RT-1"));
        store.saved = authorizer.getTokens();

        await(authorizer.signOut());

        assertFalse(authorizer.isSignedIn());
        assertNull(store.saved);
        Probe p = new Probe(API + "/pets");
        NetworkManager.getInstance().addToQueue(p);
        settle(p);
        assertEquals(Collections.singletonList("null"), seenByApi);
    }

    @Test
    void aClientWithoutARedirectUriCanStillRefresh() {
        // The device grant has none, and refresh() used to insist on one.
        assertEquals("AT-2", await(client.refresh("RT-1")).value.getAccessToken());
        assertEquals("AT-2", authorizer.getTokens().getAccessToken(),
                "tokens the client obtains reach its authorizer by themselves");
    }
}
