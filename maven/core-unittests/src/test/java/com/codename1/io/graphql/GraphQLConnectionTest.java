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
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.util.OnComplete;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Coverage for the private {@code GraphQL.GraphQLConnection}, reached through
 * {@link GraphQL#execute}. Drives the success envelope, the {@code errors}
 * envelope, and an HTTP-error response (which the connection swallows from the
 * framework dialog and reports through the callback) over the mock network. A
 * few direct {@link GraphQL#decodeJson} / {@link GraphQL#toWebSocketUrl} cases
 * round out the side-effect-free helpers the connection relies on.
 */
class GraphQLConnectionTest extends UITestBase {

    private static final String ENDPOINT = "http://gql.test/graphql";

    /** Data type with no registered mapper -> mapData returns null. */
    private static final class NoMapperData {
    }

    @AfterEach
    void clearMocks() {
        TestCodenameOneImplementation.getInstance().clearNetworkMocks();
        TestCodenameOneImplementation.getInstance().clearConnections();
    }

    private static byte[] utf8(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private GraphQLResponse<NoMapperData> run(int code, String body) {
        TestCodenameOneImplementation.getInstance()
                .addNetworkMockResponse(ENDPOINT, code, code == 200 ? "OK" : "ERR", utf8(body));
        final AtomicReference<GraphQLResponse<NoMapperData>> holder =
                new AtomicReference<GraphQLResponse<NoMapperData>>();
        final CountDownLatch latch = new CountDownLatch(1);
        GraphQL.execute(ENDPOINT, "Bearer tok", "Op", "query { x }", null, NoMapperData.class,
                new OnComplete<GraphQLResponse<NoMapperData>>() {
                    public void completed(GraphQLResponse<NoMapperData> value) {
                        holder.set(value);
                        latch.countDown();
                    }
                });
        waitFor(latch, 5000);
        return holder.get();
    }

    @Test
    void successEnvelopeParsesWithNoErrors() {
        GraphQLResponse<NoMapperData> r = run(200, "{\"data\":{\"x\":1}}");
        assertNotNull(r);
        assertEquals(200, r.getResponseCode());
        assertTrue(r.getErrors().isEmpty());
        // No mapper registered for NoMapperData -> data maps to null.
        assertNull(r.getData());
    }

    @Test
    void errorsEnvelopeIsSurfacedThroughCallback() {
        GraphQLResponse<NoMapperData> r = run(200,
                "{\"data\":null,\"errors\":[{\"message\":\"boom\"}]}");
        assertNotNull(r);
        assertEquals(1, r.getErrors().size());
        assertEquals("boom", r.getErrors().get(0).getMessage());
        assertEquals("boom", r.getResponseErrorMessage());
    }

    @Test
    void httpErrorIsReportedThroughCallbackNotADialog() {
        GraphQLResponse<NoMapperData> r = run(500, "{}");
        assertNotNull(r);
        assertEquals(500, r.getResponseCode());
        assertNull(r.getData());
        assertTrue(r.getErrors().isEmpty());
    }

    @Test
    void executeRequiresACallback() {
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                GraphQL.execute(ENDPOINT, null, null, "query { x }", null, NoMapperData.class, null);
            }
        });
    }

    // ---- side-effect-free helpers the connection delegates to ----------

    @Test
    void decodeJsonEmptyBodyReportsEmptyResponse() {
        GraphQLResponse<NoMapperData> r = GraphQL.decodeJson(new byte[0], 200, NoMapperData.class);
        assertNull(r.getData());
        assertEquals("Empty response body", r.getResponseErrorMessage());
    }

    @Test
    void decodeJsonParsesErrorList() {
        GraphQLResponse<NoMapperData> r = GraphQL.decodeJson(
                utf8("{\"errors\":[{\"message\":\"bad\"}]}"), 200, NoMapperData.class);
        assertEquals(1, r.getErrors().size());
        assertEquals("bad", r.getErrors().get(0).getMessage());
    }

    @Test
    void toWebSocketUrlRewritesHttpSchemes() {
        assertEquals("wss://h/graphql", GraphQL.toWebSocketUrl("https://h/graphql"));
        assertEquals("ws://h/graphql", GraphQL.toWebSocketUrl("http://h/graphql"));
        // Already-ws URLs and null pass through unchanged.
        assertEquals("ws://h/graphql", GraphQL.toWebSocketUrl("ws://h/graphql"));
        assertNull(GraphQL.toWebSocketUrl(null));
    }
}
