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
        assertEquals(0x101, get.getFlags(), "sampled, with a parent known to be local");
        assertEquals(1, checkout.getFlags(), "a root claims nothing about a parent it lacks");
        assertEquals(API, attribute(get.getAttributesList(), "url.full"),
                "the query string is never recorded");
        // One clock per trace: the request sits inside the action that caused it.
        assertTrue(get.getStartTimeUnixNano() >= checkout.getStartTimeUnixNano());
        assertTrue(get.getEndTimeUnixNano() <= checkout.getEndTimeUnixNano());
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
        assertNull(span.getTraceparent(),
                "a span with no trace must not hand out an all-zero traceparent");
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
        TelemetrySpan unsampled = Telemetry.startSpan("unsampled");
        assertNotNull(unsampled.getTraceparent(), "an unsampled trace still propagates");
        unsampled.end();
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
    void aQueuedExportStopsWhenConsentIsWithdrawn() throws Exception {
        AnalyticsConsent before = Analytics.getConsent();
        try {
            Analytics.setConsent(AnalyticsConsent.granted());
            Telemetry.State gated = new Telemetry.State(new TelemetryConfig()
                    .direct("http://collector.test").requireAnalyticsConsent(true));
            Telemetry.ExportRequest export = new Telemetry.ExportRequest(gated, new byte[] {1});
            assertFalse(export.shouldStop(), "with consent the export goes");
            Analytics.setConsent(AnalyticsConsent.denied());
            assertTrue(export.shouldStop(),
                    "an export queued before consent was withdrawn must not be sent");

            // The export answers to the installation that recorded it, not to
            // whatever is installed when it runs: neither nothing, nor a
            // replacement that does not ask for consent.
            Telemetry.uninstall();
            assertTrue(export.shouldStop(), "uninstalling released a consent-gated export");
            Telemetry.install(new TelemetryConfig().direct("http://collector.test"));
            assertTrue(export.shouldStop(),
                    "an ungated reinstall released a consent-gated export");
        } finally {
            Analytics.setConsent(before);
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
        ConnectionRequest own = request(API + "/own");
        own.addRequestHeader("Traceparent", mine);
        NetworkManager.getInstance().addToQueueAndWait(own);
        assertEquals(mine, connection(API + "/own").getHeaders().get("Traceparent"));
        assertNull(connection(API + "/own").getHeaders().get("traceparent"),
                "a second spelling of the header was added beside the app's");

        ConnectionRequest ours = request(API + "/ours");
        NetworkManager.getInstance().addToQueueAndWait(ours);
        assertNotNull(connection(API + "/ours").getHeaders().get("traceparent"));
        assertTrue(ours.addRequestHeaderIfAbsent("traceparent", "x"),
                "the tracer's header must be taken off the request when the attempt ends");

        // The app's request joins the app's trace downstream, so no span of ours
        // may describe it in another one; ours is recorded as usual.
        Telemetry.flush();
        List<Span> spans = exported(1);
        boolean sawOurs = false;
        for (Span span : spans) {
            String url = attribute(span.getAttributesList(), "url.full");
            assertFalse((API + "/own").equals(url),
                    "a span was recorded for a request that carries the app's own trace");
            sawOurs |= (API + "/ours").equals(url);
        }
        assertTrue(sawOurs, "the ordinary request's span is missing: " + spans);
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

    @Test
    void aQueuedParentGoesOnlyToTheTracerThatCapturedIt() throws Exception {
        // Swapped between queueing and running: the new tracer must not be handed
        // the old one's opaque context.
        final Object[] handed = new Object[] {"unset"};
        final com.codename1.io.NetworkTracer second = new com.codename1.io.NetworkTracer() {
            @Override
            public Object requestQueued(ConnectionRequest request) {
                return null;
            }

            @Override
            public Object beforeRequest(ConnectionRequest request, Object parent) {
                handed[0] = parent;
                return null;
            }

            @Override
            public void afterRequest(ConnectionRequest request, Object attempt, int status,
                                     Throwable error) {
            }
        };
        NetworkManager.setNetworkTracer(new com.codename1.io.NetworkTracer() {
            @Override
            public Object requestQueued(ConnectionRequest request) {
                NetworkManager.setNetworkTracer(second);
                return "the first tracer's context";
            }

            @Override
            public Object beforeRequest(ConnectionRequest request, Object parent) {
                return null;
            }

            @Override
            public void afterRequest(ConnectionRequest request, Object attempt, int status,
                                     Throwable error) {
            }
        });
        try {
            NetworkManager.getInstance().addToQueueAndWait(request(API + "?handover"));
        } finally {
            NetworkManager.setNetworkTracer(null);
        }
        assertNull(handed[0], "another tracer's context was passed on");
    }

    @Test
    void aTrailingSlashDoesNotDuplicateTheTracesPath() {
        assertEquals("https://c.test/v1/traces",
                new TelemetryConfig().direct("https://c.test/v1/traces/").exportUrl());
        assertEquals("https://c.test/v1/traces",
                new TelemetryConfig().direct("https://c.test//").exportUrl());
        assertEquals("https://api.test/otel/v1/traces",
                new TelemetryConfig().relay("https://api.test/otel/v1/traces/").exportUrl());
        assertEquals("https://api.test/otel/v1/traces",
                new TelemetryConfig().relay("https://api.test/").exportUrl());
        // A query carries the collector's key: the path goes BEFORE it.
        assertEquals("https://c.test/otlp/v1/traces?api-key=s3cret",
                new TelemetryConfig().direct("https://c.test/otlp?api-key=s3cret").exportUrl());
        assertEquals("https://c.test/v1/traces?api-key=s3cret",
                new TelemetryConfig().direct("https://c.test/v1/traces/?api-key=s3cret").exportUrl());
    }

    @Test
    void whileExportsAreBackedUpNoMoreAreQueuedAndTheBufferIsBounded() throws Exception {
        // A queue that already holds the maximum of this installation's exports.
        final Telemetry.State backedUp = new Telemetry.State(
                new TelemetryConfig().direct("http://collector.test")) {
            @Override
            int pendingExports() {
                return Telemetry.MAX_PENDING_EXPORTS;
            }
        };
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        impl.clearQueuedRequests();
        com.codename1.ui.CN.callSeriallyAndWait(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 1000; i++) {
                    backedUp.record(backedUp.start("s" + i, TelemetrySpan.KIND_INTERNAL, null));
                }
            }
        });
        try {
            for (ConnectionRequest queued : impl.getQueuedRequests()) {
                assertFalse(queued instanceof Telemetry.ExportRequest,
                        "an export was queued behind the ones already waiting");
            }
            // 32 per batch, so the bound is the 128 floor; the newest spans are kept.
            assertEquals(128, backedUp.buffer.size(), "the buffer grew past its bound");
            assertEquals("s999", backedUp.buffer.get(backedUp.buffer.size() - 1).getName());
        } finally {
            // Its flush timer started with the first span; it is never installed,
            // so nothing else would stop it.
            com.codename1.ui.CN.callSeriallyAndWait(new Runnable() {
                @Override
                public void run() {
                    backedUp.stop();
                }
            });
        }
    }

    @Test
    void uninstallLeavesAReplacementTracerInPlace() {
        Telemetry.install(new TelemetryConfig().direct("http://collector.test"));
        com.codename1.io.NetworkTracer mine = new com.codename1.io.NetworkTracer() {
            @Override
            public Object requestQueued(ConnectionRequest request) {
                return null;
            }

            @Override
            public Object beforeRequest(ConnectionRequest request, Object parent) {
                return null;
            }

            @Override
            public void afterRequest(ConnectionRequest request, Object attempt, int status,
                                     Throwable error) {
            }
        };
        NetworkManager.setNetworkTracer(mine);
        try {
            Telemetry.uninstall();
            assertTrue(NetworkManager.getNetworkTracer() == mine,
                    "uninstalling telemetry switched off the app's own tracer");
        } finally {
            NetworkManager.setNetworkTracer(null);
        }
    }

    @Test
    void anExportIsShortAndBehindTheAppsOwnRequests() throws Exception {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        impl.clearQueuedRequests();
        Telemetry.install(new TelemetryConfig().direct("http://collector.test"));
        NetworkManager.getInstance().addToQueueAndWait(request(API + "/timed"));
        Telemetry.flush();
        awaitConnection(COLLECTOR);
        Telemetry.ExportRequest export = null;
        for (ConnectionRequest queued : impl.getQueuedRequests()) {
            if (queued instanceof Telemetry.ExportRequest) {
                export = (Telemetry.ExportRequest) queued;
            }
        }
        assertNotNull(export, "no export was queued");
        assertEquals(10000, export.getTimeout());
        assertEquals(10000, export.getReadTimeout());
        assertEquals(ConnectionRequest.PRIORITY_LOW, export.getPriority());
    }

    @Test
    void onTheWebARelativeUrlIsSameOriginAndCarriesTheContext() throws Exception {
        assertTrue(Telemetry.isSameOriginRelative("/api/orders"));
        assertTrue(Telemetry.isSameOriginRelative("orders?next=http://x.test/"));
        assertFalse(Telemetry.isSameOriginRelative("//other.test/api"));
        assertFalse(Telemetry.isSameOriginRelative("http://other.test/api"));
        assertFalse(Telemetry.isSameOriginRelative("data:text/plain,x"));
        assertFalse(Telemetry.isSameOriginRelative(""));

        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        impl.addNetworkMockResponse("/api/orders", 200, "OK", new byte[0]);
        impl.addNetworkMockResponse("http://other.test/api", 200, "OK", new byte[0]);
        impl.setPlatformName("HTML5");
        try {
            Telemetry.install(new TelemetryConfig().direct("http://collector.test"));
            // validate() refuses a relative URL; a request that sends one to its
            // own origin has to relax it.
            ConnectionRequest relative = new ConnectionRequest() {
                @Override
                protected void validate() {
                }

                @Override
                protected void readResponse(InputStream input) {
                }
            };
            relative.setUrl("/api/orders");
            relative.setPost(false);
            NetworkManager.getInstance().addToQueueAndWait(relative);
            assertNotNull(connection("/api/orders").getHeaders().get("traceparent"),
                    "a same-origin request lost its trace context on the web");
            NetworkManager.getInstance().addToQueueAndWait(request("http://other.test/api"));
            assertNull(connection("http://other.test/api").getHeaders().get("traceparent"),
                    "a cross-origin request outside the allowlist got the header");
        } finally {
            impl.setPlatformName(null);
        }
    }

    @Test
    void aFailedAttemptThatIsRetriedStillReportsItsFailure() throws Exception {
        // The retry re-queues the request from inside the exception handler; the
        // failed attempt must be ended with its exception before that happens.
        Telemetry.install(new TelemetryConfig().direct("http://collector.test"));
        final int[] reads = new int[1];
        ConnectionRequest flaky = new ConnectionRequest() {
            @Override
            protected void readResponse(InputStream input) throws java.io.IOException {
                if (reads[0]++ == 0) {
                    throw new java.io.IOException("connection reset");
                }
            }

            @Override
            protected void handleIOException(java.io.IOException err) {
                retry();
            }
        };
        flaky.setUrl(API + "?flaky");
        flaky.setPost(false);
        NetworkManager.getInstance().addToQueue(flaky);
        long deadline = System.currentTimeMillis() + 5000;
        while (reads[0] < 2 && System.currentTimeMillis() < deadline) {
            flushSerialCalls();
            Thread.sleep(20);
        }
        assertEquals(2, reads[0], "the request was not retried");

        List<Span> spans = exported(2);
        Span failed = null;
        for (Span span : spans) {
            if (span.getStatus().getCode()
                    == io.opentelemetry.proto.trace.v1.Status.StatusCode.STATUS_CODE_ERROR) {
                failed = span;
            }
        }
        assertNotNull(failed, "the failed attempt was exported as neither failed nor answered: "
                + spans);
        assertEquals("connection reset", failed.getStatus().getMessage());
        assertEquals("exception", failed.getEvents(0).getName());
    }

    @Test
    void aSpanFromAnotherInstallationIsNeverAParent() {
        Telemetry.State before = new Telemetry.State(
                new TelemetryConfig().direct("http://collector.test"));
        Telemetry.State after = new Telemetry.State(
                new TelemetryConfig().direct("http://collector.test"));
        TelemetrySpan old = before.start("old action", TelemetrySpan.KIND_INTERNAL, null);
        TelemetrySpan mine = after.start("request", TelemetrySpan.KIND_CLIENT, old);
        TelemetrySpan child = after.start("child", TelemetrySpan.KIND_CLIENT, mine);
        assertNotNull(old);
        assertNotNull(mine);
        assertFalse(old.getTraceId().equals(mine.getTraceId()),
                "a new installation joined the previous one's trace");
        assertNull(mine.parentSpanId);
        assertEquals(mine.getTraceId(), child.getTraceId(), "its own spans still nest");
        assertEquals(mine.getSpanId(), child.parentSpanId);
    }

    @Test
    void anEndpointNoExportCouldReachIsRefusedWhenGiven() {
        String[] bad = {"https://", "https:///v1/traces", "ftp://collector.test",
            "collector.test:4318", "https://collector example", "https://c.test:99999",
            "https://[nope]:4318"};
        for (String url : bad) {
            try {
                new TelemetryConfig().direct(url);
                throw new AssertionError("accepted " + url);
            } catch (IllegalArgumentException expected) {
                // Refused at the call, not lost at the first export.
            }
        }
        try {
            new TelemetryConfig().relay("https://user:s3cret@bad host/");
            throw new AssertionError("accepted a host with a space");
        } catch (IllegalArgumentException expected) {
            assertFalse(expected.getMessage().contains("s3cret"),
                    "the refusal quoted a credential: " + expected.getMessage());
        }
        assertNull(new TelemetryConfig().direct(null).exportUrl(), "no endpoint is still allowed");
        assertNull(new TelemetryConfig().relay("").exportUrl());
        assertEquals("https://[::1]:4318/v1/traces",
                new TelemetryConfig().direct("https://[::1]:4318").exportUrl());
    }

    @Test
    void aTruncatedValueNeverEndsInHalfACharacter() {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < TelemetrySpan.MAX_VALUE_LENGTH - 1; i++) {
            text.append('a');
        }
        text.append("\ud83d\ude00tail");
        String bounded = TelemetrySpan.bound(text.toString());
        assertEquals(TelemetrySpan.MAX_VALUE_LENGTH - 1, bounded.length(),
                "the pair straddling the limit must go whole");
        assertFalse(Character.isHighSurrogate(bounded.charAt(bounded.length() - 1)));
    }

    @Test
    void anExceptionStatusIsBounded() {
        Telemetry.install(new TelemetryConfig().direct("http://collector.test"));
        TelemetrySpan span = Telemetry.startSpan("big");
        StringBuilder huge = new StringBuilder();
        while (huge.length() < 20000) {
            huge.append("0123456789");
        }
        span.recordException(new RuntimeException(huge.toString()));
        assertEquals(TelemetrySpan.MAX_VALUE_LENGTH, span.statusMessage.length());
        span.end();
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
