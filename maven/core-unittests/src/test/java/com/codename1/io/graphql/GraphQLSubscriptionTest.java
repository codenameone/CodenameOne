/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package com.codename1.io.graphql;

import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers the reachable, transport-independent surface of
 * {@link GraphQLSubscription}.
 *
 * <p>The live message protocol (connection_init / connection_ack /
 * subscribe / next / complete / error dispatch, {@code cancel()}, and the
 * private envelope builders) runs over a {@link com.codename1.io.WebSocket},
 * which the unit-test platform does not support ({@code build()} throws
 * "WebSocket not supported"). Those paths cannot be exercised here without
 * a mock socket transport (out of scope: no Mockito / reflection), so this
 * suite verifies the input guards that run before any socket is created
 * plus the request-body construction shared with the {@code subscribe}
 * message, and documents the WebSocket dependency for the rest.
 */
class GraphQLSubscriptionTest extends UITestBase {

    private static final String HTTP_ENDPOINT = "https://api.example.com/graphql";
    private static final String DOCUMENT = "subscription OnTick { tick { value } }";

    private static GraphQLSubscription.Handler<Object> noopHandler() {
        return new GraphQLSubscription.Handler<Object>() {
            public void onNext(GraphQLResponse<Object> response) {
            }

            public void onError(GraphQLResponse<Object> response) {
            }

            public void onComplete() {
            }
        };
    }

    // ---- guards that run before any socket is opened -----------------

    @Test
    void startRejectsNullHandler() {
        // The null-handler guard fires before WebSocket.build(), so this
        // is reachable on a platform without WebSocket support.
        assertThrows(IllegalArgumentException.class, () ->
                GraphQLSubscription.start("wss://api.example.com/graphql", null,
                        "OnTick", DOCUMENT, null, Object.class, null));
    }

    @Test
    void subscribeRejectsNullHandlerThroughDelegation() {
        // GraphQL.subscribe delegates to GraphQLSubscription.start; the
        // same guard must surface through the public entry point.
        assertThrows(IllegalArgumentException.class, () ->
                GraphQL.subscribe(HTTP_ENDPOINT, null, "OnTick", DOCUMENT, null,
                        Object.class, null));
    }

    @Test
    void subprotocolIsGraphqlTransportWs() {
        // The graphql-ws spec mandates this exact subprotocol token in the
        // Sec-WebSocket-Protocol handshake header.
        assertEquals("graphql-transport-ws", GraphQLSubscription.SUBPROTOCOL);
    }

    // ---- the subscribe payload the connection sends after the ack ----
    //
    // subscribeMessage() (private) wraps GraphQL.buildRequestBody(...);
    // verifying that builder confirms the exact `payload` a live
    // subscription would transmit on its `subscribe` frame.

    @Test
    void subscribeRequestBodyCarriesQueryAndOperationName() {
        String body = GraphQL.buildRequestBody("OnTick", DOCUMENT, null);
        assertTrue(body.contains("\"query\""), "must carry the document under 'query'");
        assertTrue(body.contains("OnTick"), "must carry the operation name");
        assertTrue(body.contains("tick"), "must embed the subscription document");
        // No variables supplied -> no variables key.
        assertFalse(body.contains("\"variables\""));
    }

    @Test
    void subscribeRequestBodyEmbedsVariables() {
        Map<String, Object> vars = new LinkedHashMap<String, Object>();
        vars.put("room", "general");
        vars.put("limit", 10);
        String body = GraphQL.buildRequestBody("OnMessages", DOCUMENT, vars);
        assertTrue(body.contains("\"variables\""), "variables must be spliced in");
        assertTrue(body.contains("room"));
        assertTrue(body.contains("general"));
        assertTrue(body.contains("10"));
    }

    @Test
    void subscribeRewritesHttpsEndpointToSecureWebSocketScheme() {
        // GraphQL.subscribe rewrites the HTTP(S) endpoint to ws(s):// before
        // handing it to GraphQLSubscription.start. Verifying the rewrite
        // confirms a subscription opened against an https endpoint targets
        // wss, independent of socket availability.
        assertEquals("wss://api.example.com/graphql",
                GraphQL.toWebSocketUrl(HTTP_ENDPOINT));
        assertEquals("ws://api.example.com/graphql",
                GraphQL.toWebSocketUrl("http://api.example.com/graphql"));
        // Already-WebSocket URLs pass through unchanged.
        assertEquals("wss://x/graphql", GraphQL.toWebSocketUrl("wss://x/graphql"));
    }

    @Test
    void startWithoutWebSocketSupportSurfacesUnsupportedTransport() {
        // With a valid handler the guard passes and WebSocket.build() is
        // reached; the unit-test platform has no WebSocket transport, so
        // the attempt fails fast rather than silently no-op'ing.
        assertThrows(RuntimeException.class, () ->
                GraphQLSubscription.start("wss://api.example.com/graphql", "token",
                        "OnTick", DOCUMENT, null, Object.class, noopHandler()));
    }
}
