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
