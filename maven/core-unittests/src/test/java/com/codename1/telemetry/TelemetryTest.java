/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.telemetry;

import com.codename1.analytics.Analytics;
import com.codename1.analytics.AnalyticsConsent;
import com.codename1.analytics.ConsentMode;
import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkManager;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;

import com.google.protobuf.ByteString;

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.Span;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The app's side of distributed tracing, through the real NetworkManager against
/// the mocked network: what a request carries to the server, and what reaches the
/// collector -- decoded with the specification's own generated classes.
class TelemetryTest extends UITestBase {
    private static final String API = "http://api.test/pets";
    private static final String COLLECTOR = "http://collector.test/v1/traces";
    private static final String RELAY = "http://backend.test/otel/v1/traces";

    @BeforeEach
    void mocks() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        impl.clearNetworkMocks();
        impl.clearConnections();
        impl.addNetworkMockResponse(API, 200, "OK", "[]".getBytes(StandardCharsets.UTF_8));
        impl.addNetworkMockResponse("http://api.test/missing", 404, "Not Found", new byte[0]);
        impl.addNetworkMockResponse(COLLECTOR, 200, "OK", new byte[0]);
        impl.addNetworkMockResponse(RELAY, 200, "OK", "{}".getBytes(StandardCharsets.UTF_8));
    }

    @AfterEach
    void uninstall() {
        Telemetry.uninstall();
        TestCodenameOneImplementation.getInstance().clearNetworkMocks();
    }

    @Test
    void aRequestCarriesTheTraceAndBecomesAChildSpan() throws Exception {
        Telemetry.install(new TelemetryConfig().direct("http://collector.test")
                .serviceName("shop-app").header("Authorization", "Api-Token t0k"));
        final TelemetrySpan[] action = new TelemetrySpan[1];
        Telemetry.run("checkout", new Runnable() {
            @Override
            public void run() {
                action[0] = Telemetry.getCurrentSpan();
                NetworkManager.getInstance().addToQueueAndWait(request(API + "?token=secret"));
            }
        });
        assertNull(Telemetry.getCurrentSpan(), "run() restores the previous span");

        TestCodenameOneImplementation.TestConnection api = connection(API + "?token=secret");
        String traceparent = api.getHeaders().get("traceparent");
        assertNotNull(traceparent, "the request did not carry the trace context");
        assertTrue(traceparent.startsWith("00-" + action[0].getTraceId() + "-"),
                "the request is part of the action's trace: " + traceparent);
        assertTrue(traceparent.endsWith("-01"));

        Telemetry.flush();
        List<Span> spans = exported(2);
        Span get = find(spans, "GET");
        Span checkout = find(spans, "checkout");
        assertEquals(action[0].getSpanId(), hex(get.getParentSpanId()));
        assertEquals(hex(get.getSpanId()), traceparent.substring(36, 52),
                "the header names the request's own span as the server's parent");
        assertEquals(Span.SpanKind.SPAN_KIND_CLIENT, get.getKind());
        assertEquals(Span.SpanKind.SPAN_KIND_INTERNAL, checkout.getKind());
        assertEquals(API, attribute(get.getAttributesList(), "url.full"),
                "the query string is never recorded");
        assertEquals("200", attribute(get.getAttributesList(), "http.response.status_code"));

        TestCodenameOneImplementation.TestConnection export = connection(COLLECTOR);
        assertEquals("Api-Token t0k", export.getHeaders().get("Authorization"));
        assertNull(export.getHeaders().get("traceparent"),
                "the export's own request must not be traced");
    }

    @Test
    void aFailedRequestIsAnErrorSpan() throws Exception {
        Telemetry.install(new TelemetryConfig().direct("http://collector.test"));
        ConnectionRequest missing = request("http://api.test/missing");
        missing.setFailSilently(true);
        NetworkManager.getInstance().addToQueueAndWait(missing);
        Telemetry.flush();
        Span span = find(exported(1), "GET");
        assertEquals("404", attribute(span.getAttributesList(), "http.response.status_code"));
        assertEquals(io.opentelemetry.proto.trace.v1.Status.StatusCode.STATUS_CODE_ERROR,
                span.getStatus().getCode());
        assertEquals(0, span.getParentSpanId().size(), "a request outside any action is a root");
    }

    @Test
    void theRelayGetsJsonAndTheTokenAndNeverTheCredential() throws Exception {
        Telemetry.install(new TelemetryConfig().relay("http://backend.test")
                .relayToken("r3lay").header("Authorization", "never-sent"));
        NetworkManager.getInstance().addToQueueAndWait(request(API));
        Telemetry.flush();
        TestCodenameOneImplementation.TestConnection relay = awaitConnection(RELAY);
        assertEquals("r3lay", relay.getHeaders().get("X-CN1-Telemetry-Token"));
        assertNull(relay.getHeaders().get("Authorization"),
                "a relay export must not carry a collector credential");
        String json = new String(relay.getOutputData(), StandardCharsets.UTF_8);
        assertTrue(json.startsWith("{\"resourceSpans\":[{\"resource\":{\"attributes\":"), json);
        assertTrue(json.contains("\"name\":\"GET\""), json);
        assertTrue(json.contains("\"kind\":3"), json);
    }

    @Test
    void nothingIsAddedWhenTelemetryIsOff() throws Exception {
        assertFalse(Telemetry.isInstalled());
        NetworkManager.getInstance().addToQueueAndWait(request(API));
        assertNull(connection(API).getHeaders().get("traceparent"));
        TelemetrySpan span = Telemetry.startSpan("noop");
        assertFalse(span.isRecording());
        span.end();
    }

    @Test
    void anUnsampledTraceStillPropagatesItsDecision() throws Exception {
        Telemetry.install(new TelemetryConfig().direct("http://collector.test").sampleRatio(0));
        NetworkManager.getInstance().addToQueueAndWait(request(API));
        String traceparent = connection(API).getHeaders().get("traceparent");
        assertNotNull(traceparent);
        assertTrue(traceparent.endsWith("-00"),
                "the backend must be told not to record either: " + traceparent);
    }

    @Test
    void consentWhenAskedForGatesTracingEntirely() throws Exception {
        AnalyticsConsent before = Analytics.getConsent();
        ConsentMode mode = Analytics.getConsentMode();
        try {
            Analytics.setConsentMode(ConsentMode.OPT_IN);
            Analytics.setConsent(null);
            Telemetry.install(new TelemetryConfig().direct("http://collector.test")
                    .requireAnalyticsConsent(true));
            NetworkManager.getInstance().addToQueueAndWait(request(API + "?before"));
            assertNull(connection(API + "?before").getHeaders().get("traceparent"),
                    "no consent yet under OPT_IN: the request must go out untouched");
            assertFalse(Telemetry.startSpan("x").isRecording());

            Analytics.setConsent(AnalyticsConsent.granted());
            NetworkManager.getInstance().addToQueueAndWait(request(API + "?after"));
            assertNotNull(connection(API + "?after").getHeaders().get("traceparent"),
                    "consent granted: the next request is traced");
        } finally {
            Analytics.setConsent(before);
            Analytics.setConsentMode(mode);
        }
    }

    @Test
    void withoutTheFlagConsentIsNotConsulted() throws Exception {
        AnalyticsConsent before = Analytics.getConsent();
        try {
            Analytics.setConsent(AnalyticsConsent.denied());
            Telemetry.install(new TelemetryConfig().direct("http://collector.test"));
            NetworkManager.getInstance().addToQueueAndWait(request(API + "?noflag"));
            assertNotNull(connection(API + "?noflag").getHeaders().get("traceparent"));
        } finally {
            Analytics.setConsent(before);
        }
    }

    @Test
    void aRedirectIsTwoAttemptsEachWithItsOwnStatusAndHeader() throws Exception {
        // The first attempt answers 302. It returns before the guard's capture
        // runs, and was reported as "no response"; and the request object is
        // reused for the second attempt, which must carry ITS span, not the
        // first attempt's header left behind.
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        TestCodenameOneImplementation.TestConnection hop = impl.createConnection("http://hop.test/a");
        hop.setResponseCode(302);
        hop.setHeader("location", API + "?hopped");
        Telemetry.install(new TelemetryConfig().direct("http://collector.test"));
        NetworkManager.getInstance().addToQueueAndWait(request("http://hop.test/a"));
        String second = connection(API + "?hopped").getHeaders().get("traceparent");
        assertNotNull(second, "the redirected attempt was not traced");

        Telemetry.flush();
        List<Span> spans = exported(2);
        Span redirect = null;
        Span landed = null;
        for (Span span : spans) {
            String status = attribute(span.getAttributesList(), "http.response.status_code");
            if ("302".equals(status)) {
                redirect = span;
            } else if ("200".equals(status)) {
                landed = span;
            }
        }
        assertNotNull(redirect, "the 302 attempt must report its status: " + spans);
        assertNotNull(landed, "the attempt the redirect reached: " + spans);
        assertEquals(hex(landed.getSpanId()), second.substring(36, 52),
                "the second attempt carried its own span, not the first's header");
        assertFalse(hex(redirect.getSpanId()).equals(hex(landed.getSpanId())));
    }

    @Test
    void anAppsOwnTraceparentIsNeverReplacedAndOursDoesNotOutliveTheAttempt() throws Exception {
        Telemetry.install(new TelemetryConfig().direct("http://collector.test"));
        String mine = "00-11111111111111111111111111111111-2222222222222222-01";
        ConnectionRequest own = request(API + "?own");
        own.addRequestHeader("Traceparent", mine);
        NetworkManager.getInstance().addToQueueAndWait(own);
        assertEquals(mine, connection(API + "?own").getHeaders().get("Traceparent"));
        assertNull(connection(API + "?own").getHeaders().get("traceparent"),
                "a second spelling of the header was added beside the app's");

        ConnectionRequest ours = request(API + "?ours");
        NetworkManager.getInstance().addToQueueAndWait(ours);
        assertNotNull(connection(API + "?ours").getHeaders().get("traceparent"));
        assertTrue(ours.addRequestHeaderIfAbsent("traceparent", "x"),
                "the tracer's header must be taken off the request when the attempt ends");
    }

    @Test
    void theCurrentSpanBelongsToTheThreadThatStartedIt() throws Exception {
        Telemetry.install(new TelemetryConfig().direct("http://collector.test"));
        final TelemetrySpan[] seenElsewhere = new TelemetrySpan[] {Telemetry.startSpan("sentinel")};
        Telemetry.run("action", new Runnable() {
            @Override
            public void run() {
                Thread other = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        seenElsewhere[0] = Telemetry.getCurrentSpan();
                    }
                });
                other.start();
                try {
                    other.join();
                } catch (InterruptedException err) {
                    Thread.currentThread().interrupt();
                }
                assertNotNull(Telemetry.getCurrentSpan());
            }
        });
        assertNull(seenElsewhere[0], "another thread saw this thread's action as its own");
    }

    @Test
    void anAttemptIsEndedByTheTracerThatStartedIt() throws Exception {
        // The slot can be emptied while an attempt is in flight; the attempt must
        // still be ended, and by its own tracer.
        final int[] ended = new int[1];
        NetworkManager.setNetworkTracer(new com.codename1.io.NetworkTracer() {
            @Override
            public Object requestQueued(ConnectionRequest request) {
                return null;
            }

            @Override
            public Object beforeRequest(ConnectionRequest request, Object parent) {
                NetworkManager.setNetworkTracer(null);
                return "attempt";
            }

            @Override
            public void afterRequest(ConnectionRequest request, Object attempt, int status,
                                     Throwable error) {
                if ("attempt".equals(attempt)) {
                    ended[0]++;
                }
            }
        });
        try {
            NetworkManager.getInstance().addToQueueAndWait(request(API + "?swap"));
        } finally {
            NetworkManager.setNetworkTracer(null);
        }
        assertEquals(1, ended[0]);
    }

    // ------------------------------------------------------------------

    private static ConnectionRequest request(String url) {
        ConnectionRequest request = new ConnectionRequest() {
            @Override
            protected void readResponse(InputStream input) {
                // The body is not what these tests are about.
            }
        };
        request.setUrl(url);
        request.setPost(false);
        return request;
    }

    private static TestCodenameOneImplementation.TestConnection connection(String url) {
        TestCodenameOneImplementation.TestConnection c =
                TestCodenameOneImplementation.getInstance().getConnection(url);
        assertNotNull(c, "no request was made to " + url);
        return c;
    }

    /// Waits for the export: the span reaches the buffer through the EDT, and the
    /// export itself is queued behind it.
    private TestCodenameOneImplementation.TestConnection awaitConnection(String url)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            flushSerialCalls();
            TestCodenameOneImplementation.TestConnection c =
                    TestCodenameOneImplementation.getInstance().getConnection(url);
            if (c != null && c.getOutputData().length > 0) {
                return c;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("nothing was exported to " + url);
    }

    /// Every span the collector has received, once there are at least `wanted`.
    private List<Span> exported(int wanted) throws Exception {
        long deadline = System.currentTimeMillis() + 5000;
        List<Span> spans = new ArrayList<Span>();
        while (System.currentTimeMillis() < deadline) {
            flushSerialCalls();
            Telemetry.flush();
            TestCodenameOneImplementation.TestConnection c =
                    TestCodenameOneImplementation.getInstance().getConnection(COLLECTOR);
            if (c != null && c.getOutputData().length > 0) {
                // Every export to one URL writes into the same mock connection,
                // so the bytes are several requests back to back. Protobuf
                // messages concatenate by merging their repeated fields, which
                // is exactly "every export's spans".
                spans = new ArrayList<Span>();
                ExportTraceServiceRequest merged = ExportTraceServiceRequest.parseFrom(
                        c.getOutputData());
                for (int r = 0; r < merged.getResourceSpansCount(); r++) {
                    for (int s = 0; s < merged.getResourceSpans(r).getScopeSpansCount(); s++) {
                        spans.addAll(merged.getResourceSpans(r).getScopeSpans(s).getSpansList());
                    }
                }
                assertEquals("com.codename1.telemetry",
                        merged.getResourceSpans(0).getScopeSpans(0).getScope().getName());
                if (spans.size() >= wanted) {
                    return spans;
                }
            }
            Thread.sleep(20);
        }
        throw new AssertionError("expected " + wanted + " spans, got " + spans);
    }

    private static Span find(List<Span> spans, String name) {
        for (Span span : spans) {
            if (name.equals(span.getName())) {
                return span;
            }
        }
        throw new AssertionError("no span named " + name + " in " + spans);
    }

    private static String attribute(List<KeyValue> attributes, String key) {
        for (KeyValue kv : attributes) {
            if (kv.getKey().equals(key)) {
                return kv.getValue().hasIntValue()
                        ? String.valueOf(kv.getValue().getIntValue())
                        : kv.getValue().getStringValue();
            }
        }
        return null;
    }

    private static String hex(ByteString bytes) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < bytes.size(); i++) {
            out.append(String.format("%02x", bytes.byteAt(i) & 0xff));
        }
        return out.toString();
    }
}
