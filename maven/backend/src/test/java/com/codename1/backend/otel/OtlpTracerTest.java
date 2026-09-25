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
package com.codename1.backend.otel;

import com.codename1.backend.Backend;
import com.codename1.backend.Config;
import com.codename1.backend.Database;
import com.codename1.backend.HttpServer;
import com.codename1.backend.Tracing;
import com.codename1.backend.Web;

import com.google.protobuf.ByteString;
import com.sun.net.httpserver.HttpExchange;

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The tracer end to end, on the JVM arm: a real server, real outbound calls, a
 * real database, and a collector that decodes what arrives with the protobuf
 * classes opentelemetry-proto generates -- so the hand-written encoder is judged
 * by the schema itself rather than by a decoder written alongside it.
 */
class OtlpTracerTest {
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String CALLER_SPAN = "00f067aa0ba902b7";

    private com.sun.net.httpserver.HttpServer collector;
    private final List exports = Collections.synchronizedList(new ArrayList());
    private final List contentTypes = Collections.synchronizedList(new ArrayList());
    private final List authorizations = Collections.synchronizedList(new ArrayList());
    private final List authorizationCounts = Collections.synchronizedList(new ArrayList());
    /** When each POST reached the collector, in arrival order. */
    private final List postTimes = Collections.synchronizedList(new ArrayList());
    /** Statuses the collector answers with, in order; 200 once they run out. */
    private final java.util.concurrent.ConcurrentLinkedQueue answers =
            new java.util.concurrent.ConcurrentLinkedQueue();
    /** How long the collector takes to answer; a slow one keeps the queue from emptying. */
    private volatile int collectorDelayMillis;

    @BeforeEach
    void startCollector() throws IOException {
        collector = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        collector.createContext("/v1/traces", (HttpExchange exchange) -> {
            exports.add(readAll(exchange.getRequestBody()));
            if(collectorDelayMillis > 0) {
                try {
                    Thread.sleep(collectorDelayMillis);
                } catch (InterruptedException err) {
                    Thread.currentThread().interrupt();
                }
            }
            contentTypes.add(exchange.getRequestHeaders().getFirst("Content-Type"));
            authorizations.add(String.valueOf(exchange.getRequestHeaders().getFirst("Authorization")));
            List all = exchange.getRequestHeaders().get("Authorization");
            authorizationCounts.add(Integer.valueOf(all == null ? 0 : all.size()));
            postTimes.add(Long.valueOf(System.currentTimeMillis()));
            Object answer = answers.poll();
            exchange.sendResponseHeaders(answer == null ? 200 : ((Integer)answer).intValue(), -1);
            exchange.close();
        });
        collector.start();
    }

    @AfterEach
    void stopCollector() {
        Tracing.install(null);
        collector.stop(0);
    }

    private Properties settings(int port) {
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        settings.setProperty(OtlpTracer.ENDPOINT,
                "http://127.0.0.1:" + collector.getAddress().getPort());
        settings.setProperty(OtlpTracer.HEADERS, "Authorization=Api-Token%20abc123");
        settings.setProperty(OtlpTracer.EXPORT_DELAY, "60000");
        return settings;
    }

    @Test
    @DisplayName("a request, its outbound call, the call it reaches and a statement make one trace")
    void oneTraceAcrossEverything() throws Exception {
        final int port = freePort();
        final Database db = Database.open(":memory:");
        db.execute("CREATE TABLE pets (id INTEGER PRIMARY KEY, name TEXT)", null);
        Backend backend = Backend.builder(Config.of(settings(port), "test"))
                .quiet()
                .tracing(new OtlpTracer("pets"))
                .handler(new HttpServer.Handler() {
                    public HttpServer.Response handle(HttpServer.Request request) throws Exception {
                        if(request.pathIs(ascii("/downstream"))) {
                            return HttpServer.Response.text(200, "down");
                        }
                        if(!request.pathIs(ascii("/work"))) {
                            return null;
                        }
                        Tracing.route("/work");
                        Tracing.current().setAttribute("pets.checked", 2L);
                        // The caller's own context: no client span may describe it.
                        java.util.List own = new java.util.ArrayList();
                        own.add("traceparent: 00-11111111111111111111111111111111-2222222222222222-01");
                        Web.request("GET", "http://127.0.0.1:" + port + "/downstream", own, null);
                        db.query("SELECT name FROM pets WHERE id = ?", new Object[] {Long.valueOf(7)});
                        Web.Result down = Web.get("http://127.0.0.1:" + port + "/downstream");
                        return HttpServer.Response.text(200, "ok " + down.getBodyAsString());
                    }
                })
                .start();
        try {
            HttpURLConnection connection = (HttpURLConnection)new URL(
                    "http://127.0.0.1:" + port + "/work?secret=1").openConnection();
            connection.setRequestProperty("traceparent", "00-" + TRACE + "-" + CALLER_SPAN + "-01");
            connection.setRequestProperty("tracestate", "vendor=opaque");
            assertEquals(200, connection.getResponseCode());
            assertEquals("ok down", new String(readAll(connection.getInputStream()), "UTF-8"));
        } finally {
            backend.stop();
            db.close();
        }

        assertFalse(exports.isEmpty(), "stop() must flush the spans before returning");
        assertEquals("application/x-protobuf", contentTypes.get(0));
        assertEquals("Api-Token abc123", authorizations.get(0),
                "OTEL_EXPORTER_OTLP_HEADERS values are percent-decoded");

        List spans = new ArrayList();
        String service = null;
        for(int iter = 0 ; iter < exports.size() ; iter++) {
            ExportTraceServiceRequest request = ExportTraceServiceRequest.parseFrom(
                    (byte[])exports.get(iter));
            for(ResourceSpans rs : request.getResourceSpansList()) {
                service = attribute(rs.getResource().getAttributesList(), "service.name");
                for(ScopeSpans ss : rs.getScopeSpansList()) {
                    assertEquals("com.codename1.backend", ss.getScope().getName());
                    spans.addAll(ss.getSpansList());
                }
            }
        }
        assertEquals("pets", service);

        Span work = find(spans, "GET /work", Span.SpanKind.SPAN_KIND_SERVER);
        Span query = find(spans, "SELECT", Span.SpanKind.SPAN_KIND_CLIENT);
        // By KIND as well as name: the downstream request is "GET" too, since no
        // route names it, and it is a server span.
        Span outbound = find(spans, "GET", Span.SpanKind.SPAN_KIND_CLIENT);
        Span downstream = null;
        int clientGets = 0;
        for(Object o : spans) {
            Span s = (Span)o;
            if("GET".equals(s.getName()) && s.getKind() == Span.SpanKind.SPAN_KIND_CLIENT) {
                clientGets++;
            }
            if("GET".equals(s.getName()) && s.getKind() == Span.SpanKind.SPAN_KIND_SERVER
                    && TRACE.equals(hex(s.getTraceId()))) {
                downstream = s;
            }
        }
        assertEquals(1, clientGets,
                "a client span was recorded for the call that carried the caller's own traceparent");
        assertTrue(downstream != null, "the downstream request's span in this trace");

        // The server span continues the caller's trace, under the caller's span.
        assertEquals(TRACE, hex(work.getTraceId()));
        assertEquals(CALLER_SPAN, hex(work.getParentSpanId()));
        assertEquals(Span.SpanKind.SPAN_KIND_SERVER, work.getKind());
        assertEquals("vendor=opaque", work.getTraceState());
        assertEquals(0x301, work.getFlags(), "sampled, with a parent known to be remote");
        assertEquals("/work", attribute(work.getAttributesList(), "http.route"));
        assertEquals("/work", attribute(work.getAttributesList(), "url.path"),
                "the query string is never recorded");
        assertEquals("200", attribute(work.getAttributesList(), "http.response.status_code"));
        assertEquals("2", attribute(work.getAttributesList(), "pets.checked"));
        assertTrue(work.getEndTimeUnixNano() >= work.getStartTimeUnixNano());

        // The statement and the outbound call are its children.
        assertEquals(TRACE, hex(query.getTraceId()));
        assertEquals(hex(work.getSpanId()), hex(query.getParentSpanId()));
        assertEquals(Span.SpanKind.SPAN_KIND_CLIENT, query.getKind());
        assertEquals("sqlite", attribute(query.getAttributesList(), "db.system"));
        assertEquals("SELECT name FROM pets WHERE id = ?",
                attribute(query.getAttributesList(), "db.query.text"),
                "the statement is recorded, the bound value is not");
        assertEquals(hex(work.getSpanId()), hex(outbound.getParentSpanId()));
        assertEquals(Span.SpanKind.SPAN_KIND_CLIENT, outbound.getKind());
        // Children sit inside their parent on ONE timeline. Each span reading the
        // wall clock for itself put them up to a millisecond apart, and a child
        // was reported ending after the request that contains it.
        for(Span child : new Span[] {query, outbound}) {
            assertTrue(child.getStartTimeUnixNano() >= work.getStartTimeUnixNano(),
                    child.getName() + " starts before its parent");
            assertTrue(child.getEndTimeUnixNano() <= work.getEndTimeUnixNano(),
                    child.getName() + " ends after its parent");
        }

        // And the traceparent the outbound call carried made the request it
        // reached a child of THAT span -- propagation, observed from the far side.
        assertEquals(TRACE, hex(downstream.getTraceId()));
        assertEquals(hex(outbound.getSpanId()), hex(downstream.getParentSpanId()));
        assertEquals("vendor=opaque", downstream.getTraceState(), "tracestate travels too");
    }

    @Test
    @DisplayName("a failing handler is an error span with the exception recorded")
    void errorsAreRecorded() throws Exception {
        int port = freePort();
        Backend backend = Backend.builder(Config.of(settings(port), "test"))
                .quiet()
                .tracing(new OtlpTracer())
                .handler(new HttpServer.Handler() {
                    public HttpServer.Response handle(HttpServer.Request request) throws Exception {
                        StringBuilder huge = new StringBuilder("boom");
                        while(huge.length() < 20000) {
                            huge.append(" and more");
                        }
                        throw new IllegalStateException(huge.toString());
                    }
                })
                .start();
        try {
            HttpURLConnection connection = (HttpURLConnection)new URL(
                    "http://127.0.0.1:" + port + "/x").openConnection();
            assertEquals(500, connection.getResponseCode());
        } finally {
            backend.stop();
        }
        ExportTraceServiceRequest request = ExportTraceServiceRequest.parseFrom(
                (byte[])exports.get(0));
        Span span = request.getResourceSpans(0).getScopeSpans(0).getSpans(0);
        assertEquals("unknown_service",
                attribute(request.getResourceSpans(0).getResource().getAttributesList(),
                        "service.name"));
        assertEquals(io.opentelemetry.proto.trace.v1.Status.StatusCode.STATUS_CODE_ERROR,
                span.getStatus().getCode());
        assertEquals("exception", span.getEvents(0).getName());
        assertEquals(OtelSpan.MAX_VALUE_LENGTH, span.getStatus().getMessage().length(),
                "the status description is bounded like the event attribute");
        assertEquals("java.lang.IllegalStateException",
                attribute(span.getEvents(0).getAttributesList(), "exception.type"));
        assertEquals(0, span.getParentSpanId().size(), "a root span has no parent");
        assertEquals(1, span.getFlags(),
                "a root has no parent context, so its remoteness is not claimed either way");
        assertEquals(32, hex(span.getTraceId()).length());
    }

    @Test
    @DisplayName("an excluded exception attribute is recorded neither in the event nor the status")
    void excludedExceptionAttributes() throws Exception {
        int port = freePort();
        Properties settings = settings(port);
        settings.setProperty(OtlpTracer.ATTRIBUTES_EXCLUDE, "exception.message");
        Backend backend = Backend.builder(Config.of(settings, "test"))
                .quiet()
                .tracing(new OtlpTracer())
                .handler(new HttpServer.Handler() {
                    public HttpServer.Response handle(HttpServer.Request request) throws Exception {
                        throw new IllegalStateException("card 4111 declined");
                    }
                })
                .start();
        try {
            HttpURLConnection connection = (HttpURLConnection)new URL(
                    "http://127.0.0.1:" + port + "/x").openConnection();
            assertEquals(500, connection.getResponseCode());
        } finally {
            backend.stop();
        }
        Span span = ExportTraceServiceRequest.parseFrom((byte[])exports.get(0))
                .getResourceSpans(0).getScopeSpans(0).getSpans(0);
        assertEquals("exception", span.getEvents(0).getName());
        assertNull(attribute(span.getEvents(0).getAttributesList(), "exception.message"),
                "an excluded attribute was exported on the exception event");
        assertEquals("java.lang.IllegalStateException",
                attribute(span.getEvents(0).getAttributesList(), "exception.type"));
        assertEquals("java.lang.IllegalStateException", span.getStatus().getMessage(),
                "the status description carried the excluded message");
    }

    @Test
    @DisplayName("installing a tracer shuts down the one it replaces, and only that one")
    void replacingATracerShutsDownThePreviousOne() {
        final int[] shutdowns = new int[2];
        ThrowingTracer first = new ThrowingTracer() {
            public void shutdown(int timeoutMillis) {
                shutdowns[0]++;
            }
        };
        ThrowingTracer second = new ThrowingTracer() {
            public void shutdown(int timeoutMillis) {
                shutdowns[1]++;
            }
        };
        Tracing.install(first);
        Tracing.install(first);
        assertEquals(0, shutdowns[0], "re-installing the same tracer stopped it");
        Tracing.install(second);
        assertEquals(1, shutdowns[0], "the replaced tracer was left running");
        Tracing.install(null);
        assertEquals(1, shutdowns[1], "turning tracing off left the tracer running");
        assertEquals(1, shutdowns[0]);
    }

    @Test
    @DisplayName("flush returns once what was queued before it has gone, though the queue never empties")
    void flushWaitsOnlyForWhatItFound() throws Exception {
        collectorDelayMillis = 20;
        Properties settings = settings(freePort());
        settings.setProperty(OtlpTracer.BATCH_SIZE, "1");
        settings.setProperty(OtlpTracer.QUEUE_SIZE, "5");
        final OtlpTracer tracer = new OtlpTracer();
        assertTrue(tracer.open(Config.of(settings, "test")));
        final java.util.concurrent.atomic.AtomicBoolean stop =
                new java.util.concurrent.atomic.AtomicBoolean();
        Thread busy = new Thread(() -> {
            // Spans ending far faster than one export per 20ms: the queue is never
            // empty again for as long as this runs.
            while(!stop.get()) {
                tracer.startSpan("busy", com.codename1.backend.Span.KIND_INTERNAL,
                        null, null, null).end();
                Thread.yield();
            }
        });
        busy.start();
        try {
            Thread.sleep(100);
            long started = System.currentTimeMillis();
            tracer.flush(10000);
            long took = System.currentTimeMillis() - started;
            assertTrue(took < 5000, "flush waited " + took
                    + "ms for spans queued after it was called");
        } finally {
            stop.set(true);
            busy.join();
            tracer.shutdown(0);
        }
    }

    @Test
    @DisplayName("a header value that cannot be sent is refused when the tracer opens")
    void anUnsendableHeaderIsRefusedAtStartup() throws Exception {
        Properties settings = settings(freePort());
        settings.setProperty(OtlpTracer.HEADERS, "Authorization=token%0Aextra");
        IOException refused = assertThrows(IOException.class,
                () -> new OtlpTracer().open(Config.of(settings, "test")));
        assertTrue(refused.getMessage().contains("Authorization"), refused.getMessage());
        assertFalse(refused.getMessage().contains("token"),
                "the refusal quoted the credential: " + refused.getMessage());
        settings.setProperty(OtlpTracer.HEADERS, "Host=collector.example");
        assertThrows(IOException.class, () -> new OtlpTracer().open(Config.of(settings, "test")));
        settings.setProperty(OtlpTracer.HEADERS, "Content-Type=application/json");
        assertThrows(IOException.class, () -> new OtlpTracer().open(Config.of(settings, "test")),
                "a configured Content-Type went out as a second one");
    }

    @Test
    @DisplayName("the relay refuses a negative timestamp instead of forwarding it")
    void aNegativeTimestampIsRefused() throws Exception {
        java.util.Map request = (java.util.Map)com.codename1.backend.Json.parse(
                "{\"resourceSpans\":[{\"scopeSpans\":[{\"spans\":[{\"traceId\":\""
                + TRACE + "\",\"spanId\":\"" + CALLER_SPAN + "\",\"name\":\"x\","
                + "\"startTimeUnixNano\":\"-1\"}]}]}]}");
        IOException refused = assertThrows(IOException.class,
                () -> OtlpSchema.sanitize(request));
        assertTrue(refused.getMessage().contains("startTimeUnixNano"), refused.getMessage());
    }

    @Test
    @DisplayName("stopping a server leaves a tracer it did not install running")
    void stoppingAServerLeavesAnotherTracerAlone() throws Exception {
        int port = freePort();
        Backend backend = Backend.builder(Config.of(settings(port), "test"))
                .quiet()
                .tracing(new OtlpTracer())
                .handler(new HttpServer.Handler() {
                    public HttpServer.Response handle(HttpServer.Request request) {
                        return null;
                    }
                })
                .start();
        final int[] shutdowns = new int[1];
        ThrowingTracer replacement = new ThrowingTracer() {
            public void shutdown(int timeoutMillis) {
                shutdowns[0]++;
            }
        };
        try {
            Tracing.install(replacement);
        } finally {
            backend.stop();
        }
        assertSame(replacement, Tracing.getTracer(),
                "stopping the server uninstalled a tracer it never installed");
        assertEquals(0, shutdowns[0], "stopping the server shut down another tracer");
    }

    @Test
    @DisplayName("a span from a replaced tracer is never adopted as a parent")
    void aReplacedTracersSpanIsNotAParent() throws Exception {
        OtlpTracer before = new OtlpTracer();
        OtlpTracer after = new OtlpTracer();
        assertTrue(before.open(Config.of(settings(freePort()), "test")));
        assertTrue(after.open(Config.of(settings(freePort()), "test")));
        try {
            OtelSpan old = (OtelSpan)before.startSpan("request", com.codename1.backend.Span.KIND_SERVER,
                    null, null, null);
            OtelSpan child = (OtelSpan)after.startSpan("query", com.codename1.backend.Span.KIND_CLIENT,
                    old, null, null);
            assertFalse(old.traceHi == child.traceHi && old.traceLo == child.traceLo,
                    "the new tracer's span joined the replaced tracer's trace");
            assertEquals(0, child.parentId);
            OtelSpan grandchild = (OtelSpan)after.startSpan("row", com.codename1.backend.Span.KIND_INTERNAL,
                    child, null, null);
            assertEquals(child.spanId, grandchild.parentId, "its own spans still nest");
        } finally {
            before.shutdown(0);
            after.shutdown(0);
        }
    }

    @Test
    @DisplayName("a relay path is matched as the server normalizes request paths")
    void relayPathIsNormalized() throws Exception {
        int port = freePort();
        Properties settings = settings(port);
        settings.setProperty(OtlpTracer.RELAY, "true");
        settings.setProperty(OtlpTracer.RELAY_PATH, "/otel/%74races");
        Backend backend = Backend.builder(Config.of(settings, "test"))
                .quiet()
                .tracing(new OtlpTracer("pets"))
                .handler(new HttpServer.Handler() {
                    public HttpServer.Response handle(HttpServer.Request request) {
                        return HttpServer.Response.text(404, "not the relay");
                    }
                })
                .start();
        try {
            assertEquals(200, post(port, "/otel/traces", "{\"resourceSpans\":[]}", null),
                    "the relay configured as /otel/%74races never answered /otel/traces");
            assertEquals(200, post(port, "/otel/%74races", "{\"resourceSpans\":[]}", null),
                    "nor the spelling that was configured");
        } finally {
            backend.stop();
        }
    }

    @Test
    @DisplayName("a blank service name falls back to unknown_service")
    void aBlankServiceNameIsUnknownService() throws Exception {
        Properties settings = settings(freePort());
        settings.setProperty(OtlpTracer.SERVICE_NAME, "   ");
        OtlpTracer tracer = new OtlpTracer("  ");
        assertTrue(tracer.open(Config.of(settings, "test")));
        try {
            tracer.startSpan("x", com.codename1.backend.Span.KIND_INTERNAL, null, null, null).end();
            tracer.flush(5000);
        } finally {
            tracer.shutdown(0);
        }
        ExportTraceServiceRequest request = ExportTraceServiceRequest.parseFrom(
                (byte[])exports.get(0));
        assertEquals("unknown_service",
                attribute(request.getResourceSpans(0).getResource().getAttributesList(),
                        "service.name"));
    }

    @Test
    @DisplayName("a server that fails to start leaves the tracer it found installed and running")
    void aFailedStartKeepsTheInstalledTracer() throws Exception {
        final int[] shutdowns = new int[1];
        ThrowingTracer existing = new ThrowingTracer() {
            public void shutdown(int timeoutMillis) {
                shutdowns[0]++;
            }
        };
        Tracing.install(existing);
        // The port is taken, so this start-up fails after its tracer went in.
        java.net.ServerSocket blocker = new java.net.ServerSocket(0);
        try {
            int port = blocker.getLocalPort();
            assertThrows(Exception.class, () -> Backend.builder(Config.of(settings(port), "test"))
                    .quiet()
                    .tracing(new OtlpTracer())
                    .handler(new HttpServer.Handler() {
                        public HttpServer.Response handle(HttpServer.Request request) {
                            return null;
                        }
                    })
                    .start());
        } finally {
            blocker.close();
        }
        assertSame(existing, Tracing.getTracer(),
                "a failed start-up left the process with no tracer at all");
        assertEquals(0, shutdowns[0], "a failed start-up shut down a tracer that was working");
    }

    @Test
    @DisplayName("shutdown stops within its window instead of draining a slow collector's queue")
    void shutdownIsBoundedBySlowCollectors() throws Exception {
        collectorDelayMillis = 1500;
        Properties settings = settings(freePort());
        settings.setProperty(OtlpTracer.BATCH_SIZE, "1");
        OtlpTracer tracer = new OtlpTracer();
        assertTrue(tracer.open(Config.of(settings, "test")));
        for(int i = 0 ; i < 10 ; i++) {
            tracer.startSpan("s" + i, com.codename1.backend.Span.KIND_INTERNAL, null, null, null).end();
        }
        long started = System.currentTimeMillis();
        tracer.shutdown(200);
        assertTrue(System.currentTimeMillis() - started < 3000, "shutdown overran its window");
        // Draining would post all ten, one every 1.5s; bounded, the post already in
        // flight is the last.
        Thread.sleep(5000);
        assertTrue(exports.size() <= 2,
                "the stopped exporter kept posting to the collector: " + exports.size() + " posts");
    }

    @Test
    @DisplayName("a retryable failure is retried once, after a backoff, without sleeping")
    void aRetryableFailureIsRetriedOnceFromTheQueue() throws Exception {
        Properties settings = settings(freePort());
        settings.setProperty(OtlpTracer.EXPORT_DELAY, "200");
        answers.add(Integer.valueOf(503));
        OtlpTracer tracer = new OtlpTracer();
        assertTrue(tracer.open(Config.of(settings, "test")));
        try {
            tracer.startSpan("once", com.codename1.backend.Span.KIND_INTERNAL, null, null, null).end();
            tracer.flush(5000);
            java.util.Map metrics = new java.util.LinkedHashMap();
            tracer.metrics(metrics);
            assertEquals(2, exports.size(), "the 503 and the retry that succeeded");
            assertEquals(Long.valueOf(1), metrics.get("spansExported"));

            // Refused again on its retry: dropped, never tried a third time.
            answers.add(Integer.valueOf(503));
            answers.add(Integer.valueOf(503));
            tracer.startSpan("twice", com.codename1.backend.Span.KIND_INTERNAL, null, null, null).end();
            // flush returns once the span is exported OR dropped: here, dropped.
            tracer.flush(5000);
            metrics.clear();
            tracer.metrics(metrics);
            assertEquals(4, exports.size(), "a span was retried more than once");
            assertEquals(Long.valueOf(1), metrics.get("spansDropped"));
        } finally {
            tracer.shutdown(0);
        }
    }

    @Test
    @DisplayName("the backoff holds even after a batch that used its retry is dropped")
    void backoffHoldsAfterADroppedBatch() throws Exception {
        Properties settings = settings(freePort());
        settings.setProperty(OtlpTracer.EXPORT_DELAY, "500");
        answers.add(Integer.valueOf(429));
        answers.add(Integer.valueOf(429));
        OtlpTracer tracer = new OtlpTracer();
        assertTrue(tracer.open(Config.of(settings, "test")));
        try {
            tracer.startSpan("first", com.codename1.backend.Span.KIND_INTERNAL, null, null, null).end();
            tracer.flush(5000);   // posted, retried, dropped
            tracer.startSpan("second", com.codename1.backend.Span.KIND_INTERNAL, null, null, null).end();
            tracer.flush(5000);
            assertEquals(3, postTimes.size(), String.valueOf(postTimes));
            long afterDrop = ((Long)postTimes.get(2)).longValue() - ((Long)postTimes.get(1)).longValue();
            assertTrue(afterDrop >= 400, "the next batch went out " + afterDrop
                    + "ms after a 429, not after the backoff");
        } finally {
            tracer.shutdown(0);
        }
    }

    @Test
    @DisplayName("relay payloads that were never posted keep their own retry")
    void unsentRelayPayloadsKeepTheirRetry() throws Exception {
        // One round of two: A's first POST fails, and A and the unsent B go back.
        // B's own first POST then fails too, and must still get its retry.
        answers.add(Integer.valueOf(503));   // A
        answers.add(Integer.valueOf(200));   // A, retried
        answers.add(Integer.valueOf(503));   // B, first real attempt
        answers.add(Integer.valueOf(200));   // B, retried
        BatchExporter exporter = new BatchExporter(
                "http://127.0.0.1:" + collector.getAddress().getPort() + "/v1/traces",
                new ArrayList(), false, new java.util.LinkedHashMap(), 16, 4, 100, 1 << 20);
        assertTrue(exporter.addRelayed("{\"a\":1}".getBytes("UTF-8"), "application/json"));
        assertTrue(exporter.addRelayed("{\"b\":1}".getBytes("UTF-8"), "application/json"));
        exporter.start();
        try {
            exporter.flush(10000);
            java.util.Map metrics = new java.util.LinkedHashMap();
            exporter.metrics(metrics);
            assertEquals(Long.valueOf(2), metrics.get("clientExportsRelayed"),
                    "a payload that was never sent was dropped on its first failure");
            assertEquals(Long.valueOf(0), metrics.get("clientExportsDropped"));
        } finally {
            exporter.shutdown(0);
        }
    }

    @Test
    @DisplayName("a request that stops the server still exports its own span")
    void theStoppingRequestKeepsItsSpan() throws Exception {
        int port = freePort();
        final java.util.concurrent.atomic.AtomicReference server =
                new java.util.concurrent.atomic.AtomicReference();
        Backend backend = Backend.builder(Config.of(settings(port), "test"))
                .quiet()
                .tracing(new OtlpTracer())
                .handler(new HttpServer.Handler() {
                    public HttpServer.Response handle(HttpServer.Request request) {
                        if(request.getTarget().startsWith("/shutdown")) {
                            ((Backend)server.get()).stop();
                            return HttpServer.Response.text(200, "stopping");
                        }
                        return null;
                    }
                })
                .start();
        server.set(backend);
        HttpURLConnection connection = (HttpURLConnection)new URL(
                "http://127.0.0.1:" + port + "/shutdown").openConnection();
        assertEquals(200, connection.getResponseCode());
        long deadline = System.currentTimeMillis() + 10000;
        boolean found = false;
        while(!found && System.currentTimeMillis() < deadline) {
            for(int iter = 0 ; iter < exports.size() && !found ; iter++) {
                ExportTraceServiceRequest sent = ExportTraceServiceRequest.parseFrom((byte[])exports.get(iter));
                for(ResourceSpans rs : sent.getResourceSpansList()) {
                    for(io.opentelemetry.proto.trace.v1.ScopeSpans ss : rs.getScopeSpansList()) {
                        for(Span span : ss.getSpansList()) {
                            found |= "/shutdown".equals(attribute(span.getAttributesList(), "url.path"));
                        }
                    }
                }
            }
            if(!found) {
                Thread.sleep(50);
            }
        }
        assertTrue(found, "the request that stopped the server lost its span");
    }

    @Test
    @DisplayName("the relay refuses too many spans before validating them")
    void relaySpanCapComesFirst() throws Exception {
        int port = freePort();
        Properties settings = settings(port);
        settings.setProperty(OtlpTracer.RELAY, "true");
        settings.setProperty(OtlpTracer.RELAY_MAX_SPANS, "1");
        Backend backend = Backend.builder(Config.of(settings, "test"))
                .quiet()
                .tracing(new OtlpTracer("pets"))
                .handler(new HttpServer.Handler() {
                    public HttpServer.Response handle(HttpServer.Request request) {
                        return null;
                    }
                })
                .start();
        try {
            // Two spans, both with ids the sanitizer would refuse (400). Counting
            // first answers 413 without building the sanitized copy at all.
            String body = "{\"resourceSpans\":[{\"scopeSpans\":[{\"spans\":["
                    + "{\"traceId\":\"nothex\",\"spanId\":\"x\"},"
                    + "{\"traceId\":\"nothex\",\"spanId\":\"y\"}]}]}]}";
            assertEquals(413, post(port, "/otel/v1/traces", body, null));
        } finally {
            backend.stop();
        }
    }

    @Test
    @DisplayName("OTEL_SDK_DISABLED leaves the server untraced and the collector untouched")
    void disabledAtRunTime() throws Exception {
        int port = freePort();
        Properties settings = settings(port);
        settings.setProperty(OtlpTracer.DISABLED, "true");
        Backend backend = Backend.builder(Config.of(settings, "test"))
                .quiet()
                .tracing(new OtlpTracer("pets"))
                .handler(new HttpServer.Handler() {
                    public HttpServer.Response handle(HttpServer.Request request) throws Exception {
                        assertNull(Tracing.currentTraceparent());
                        return HttpServer.Response.text(200, "ok");
                    }
                })
                .start();
        try {
            assertFalse(Tracing.isEnabled());
            HttpURLConnection connection = (HttpURLConnection)new URL(
                    "http://127.0.0.1:" + port + "/x").openConnection();
            assertEquals(200, connection.getResponseCode());
        } finally {
            backend.stop();
        }
        assertTrue(exports.isEmpty());
    }

    @Test
    @DisplayName("the traces-specific headers replace the generic ones rather than adding to them")
    void signalHeadersReplaceGenericOnes() throws Exception {
        int port = freePort();
        Properties settings = settings(port);
        settings.setProperty(OtlpTracer.TRACES_HEADERS, "Authorization=Api-Token%20traces");
        Backend backend = Backend.builder(Config.of(settings, "test"))
                .quiet()
                .tracing(new OtlpTracer("pets"))
                .handler(new HttpServer.Handler() {
                    public HttpServer.Response handle(HttpServer.Request request) throws Exception {
                        return HttpServer.Response.text(200, "ok");
                    }
                })
                .start();
        try {
            HttpURLConnection connection = (HttpURLConnection)new URL(
                    "http://127.0.0.1:" + port + "/x").openConnection();
            assertEquals(200, connection.getResponseCode());
        } finally {
            backend.stop();
        }
        assertEquals("Api-Token traces", authorizations.get(0));
        assertEquals(Integer.valueOf(1), authorizationCounts.get(0),
                "two Authorization headers went to the collector");
    }

    @Test
    @DisplayName("a tracer that throws while decorating a query does not fail the query")
    void aBrokenTracerCannotFailAQuery() throws Exception {
        Database db = Database.open(":memory:");
        db.execute("CREATE TABLE t (v INTEGER)", null);
        db.execute("INSERT INTO t (v) VALUES (1)", null);
        Tracing.install(new ThrowingTracer());
        try {
            assertEquals(1, db.query("SELECT v FROM t", null).size(),
                    "the rows a query fetched must reach the caller whatever the tracer does");
            // And inside a span, where starting the statement's span first asks the
            // parent's kind -- which this tracer's spans throw from.
            Object nested = Tracing.inSpan("work", span -> db.query("SELECT v FROM t", null).size());
            assertEquals(Integer.valueOf(1), nested);
        } finally {
            Tracing.install(null);
            db.close();
        }
    }

    @Test
    @DisplayName("a non-ASCII relay token is not satisfied by its ASCII lookalike")
    void relayTokenComparesEveryCharacter() throws Exception {
        int port = freePort();
        Properties settings = settings(port);
        settings.setProperty(OtlpTracer.RELAY, "true");
        settings.setProperty(OtlpTracer.RELAY_TOKEN, "s\u00ebcret");
        Backend backend = Backend.builder(Config.of(settings, "test"))
                .quiet()
                .tracing(new OtlpTracer("pets"))
                .handler(new HttpServer.Handler() {
                    public HttpServer.Response handle(HttpServer.Request request) throws Exception {
                        return null;
                    }
                })
                .start();
        try {
            // The old comparison folded every non-ASCII character to '?'.
            assertEquals(401, post(port, "/otel/v1/traces", "{\"resourceSpans\":[]}", "s?cret"));
        } finally {
            backend.stop();
        }
    }

    @Test
    @DisplayName("a tracer that throws while a route is named does not fail the request")
    void aBrokenTracerCannotFailARoute() throws Exception {
        Tracing.install(new ThrowingTracer());
        try {
            Object result = Tracing.inSpan("work", span -> {
                // What every generated router calls on a matched request.
                Tracing.route("/pets/{id}");
                return "handled";
            });
            assertEquals("handled", result);
        } finally {
            Tracing.install(null);
        }
    }

    /** A tracer whose spans throw from every decoration. */
    private static class ThrowingTracer implements com.codename1.backend.Tracer {
        public boolean open(Config config) {
            return true;
        }

        public com.codename1.backend.Span startSpan(String name, int kind,
                com.codename1.backend.Span parent, String traceparent, String tracestate) {
            return new com.codename1.backend.Span() {
                public com.codename1.backend.Span setAttribute(String key, String value) {
                    throw new IllegalStateException("tracer bug");
                }

                public com.codename1.backend.Span setAttribute(String key, long value) {
                    throw new IllegalStateException("tracer bug");
                }

                public com.codename1.backend.Span setAttribute(String key, double value) {
                    throw new IllegalStateException("tracer bug");
                }

                public com.codename1.backend.Span setAttribute(String key, boolean value) {
                    throw new IllegalStateException("tracer bug");
                }

                public com.codename1.backend.Span recordException(Throwable error) {
                    throw new IllegalStateException("tracer bug");
                }

                public com.codename1.backend.Span setError(String description) {
                    throw new IllegalStateException("tracer bug");
                }

                public com.codename1.backend.Span updateName(String name) {
                    return this;
                }

                public String getName() {
                    return "x";
                }

                public int getKind() {
                    throw new IllegalStateException("tracer bug");
                }

                public boolean isRecording() {
                    return true;
                }

                public String traceparent() {
                    return null;
                }

                public String tracestate() {
                    return null;
                }

                public void discard() {
                }

                public void end() {
                }
            };
        }

        public void flush(int timeoutMillis) {
        }

        public void shutdown(int timeoutMillis) {
        }

        public HttpServer.Handler relay() {
            return null;
        }

        public void metrics(java.util.Map out) {
        }
    }

    @Test
    @DisplayName("spans a collector rejects in a partial success are not counted as exported")
    void partialSuccessIsCounted() throws Exception {
        collector.removeContext("/v1/traces");
        collector.createContext("/v1/traces", (HttpExchange exchange) -> {
            readAll(exchange.getRequestBody());
            byte[] answer = io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse
                    .newBuilder()
                    .setPartialSuccess(io.opentelemetry.proto.collector.trace.v1
                            .ExportTracePartialSuccess.newBuilder()
                            .setRejectedSpans(1).setErrorMessage("span too old").build())
                    .build().toByteArray();
            exchange.getResponseHeaders().add("Content-Type", "application/x-protobuf");
            exchange.sendResponseHeaders(200, answer.length);
            exchange.getResponseBody().write(answer);
            exchange.close();
        });
        int port = freePort();
        Backend backend = Backend.builder(Config.of(settings(port), "test"))
                .quiet()
                .tracing(new OtlpTracer("pets"))
                .handler(new HttpServer.Handler() {
                    public HttpServer.Response handle(HttpServer.Request request) throws Exception {
                        return HttpServer.Response.text(200, "ok");
                    }
                })
                .start();
        java.util.Map metrics;
        try {
            HttpURLConnection connection = (HttpURLConnection)new URL(
                    "http://127.0.0.1:" + port + "/x").openConnection();
            assertEquals(200, connection.getResponseCode());
            Tracing.getTracer().flush(5000);
            metrics = backend.getServer().getMetrics();
        } finally {
            backend.stop();
        }
        assertEquals(Long.valueOf(1), metrics.get("spansRejected"), String.valueOf(metrics));
        assertEquals(Long.valueOf(0), metrics.get("spansExported"), String.valueOf(metrics));
    }

    @Test
    @DisplayName("http/json exports the same tree as OTLP/JSON")
    void jsonProtocol() throws Exception {
        int port = freePort();
        Properties settings = settings(port);
        settings.setProperty(OtlpTracer.PROTOCOL, "http/json");
        Backend backend = Backend.builder(Config.of(settings, "test"))
                .quiet()
                .tracing(new OtlpTracer("pets"))
                .handler(new HttpServer.Handler() {
                    public HttpServer.Response handle(HttpServer.Request request) throws Exception {
                        return HttpServer.Response.text(200, "ok");
                    }
                })
                .start();
        try {
            HttpURLConnection connection = (HttpURLConnection)new URL(
                    "http://127.0.0.1:" + port + "/x").openConnection();
            assertEquals(200, connection.getResponseCode());
        } finally {
            backend.stop();
        }
        assertEquals("application/json", contentTypes.get(0));
        String json = new String((byte[])exports.get(0), "UTF-8");
        assertTrue(json.startsWith("{\"resourceSpans\":[{\"resource\":{\"attributes\":"), json);
        assertTrue(json.contains("\"kind\":2"), json);
        // And it is the SAME export: re-encoding the JSON through the schema gives
        // bytes the protobuf classes read back as the span that was sent.
        byte[] proto = OtlpSchema.protobuf(com.codename1.backend.Json.parseObject(json));
        Span span = ExportTraceServiceRequest.parseFrom(proto)
                .getResourceSpans(0).getScopeSpans(0).getSpans(0);
        assertEquals("GET", span.getName());
    }

    @Test
    @DisplayName("the relay re-encodes a client's JSON export and refuses what is not one")
    void relay() throws Exception {
        int port = freePort();
        Properties settings = settings(port);
        settings.setProperty(OtlpTracer.RELAY, "true");
        settings.setProperty(OtlpTracer.RELAY_TOKEN, "s3cret");
        Backend backend = Backend.builder(Config.of(settings, "test"))
                .quiet()
                .tracing(new OtlpTracer("pets"))
                .handler(new HttpServer.Handler() {
                    public HttpServer.Response handle(HttpServer.Request request) throws Exception {
                        return null;
                    }
                })
                .start();
        String client = "{\"resourceSpans\":[{\"resource\":{\"attributes\":[{\"key\":"
                + "\"service.name\",\"value\":{\"stringValue\":\"app\"}}]},\"scopeSpans\":[{"
                + "\"scope\":{\"name\":\"com.codename1.io\"},\"spans\":[{\"traceId\":\""
                + TRACE + "\",\"spanId\":\"" + CALLER_SPAN + "\",\"name\":\"GET /pets\","
                + "\"kind\":3,\"startTimeUnixNano\":\"1700000000000000000\","
                + "\"endTimeUnixNano\":\"1700000000500000000\",\"smuggled\":\"x\","
                + "\"attributes\":[{\"key\":\"http.response.status_code\",\"value\":"
                + "{\"intValue\":\"200\"}}]}]}]}]}";
        try {
            assertEquals(401, post(port, "/otel/v1/traces", client, null));
            assertEquals(401, post(port, "/otel/v1/traces", client, "s3cre?"),
                    "a wrong token is refused");
            assertEquals(415, postTyped(port, "/otel/v1/traces", client, "s3cret", "text/plain"));
            assertEquals(400, post(port, "/otel/v1/traces",
                    client.replace(TRACE, "nothex"), "s3cret"));
            assertEquals(400, post(port, "/otel/v1/traces",
                    client.replace(TRACE, "00000000000000000000000000000000"), "s3cret"),
                    "an all-zero trace id is refused before the relay answers 200");
            assertEquals(400, post(port, "/otel/v1/traces",
                    client.replace("\"traceId\":\"" + TRACE + "\",", ""), "s3cret"),
                    "a span without a trace id is refused");
            assertEquals(400, post(port, "/otel/v1/traces",
                    client.replace("{\"intValue\":\"200\"}",
                            "{\"intValue\":\"200\",\"stringValue\":\"x\"}"), "s3cret"),
                    "an AnyValue holding two alternatives of its oneof is refused");
            assertEquals(200, post(port, "/otel/v1/traces", client, "s3cret"));
        } finally {
            backend.stop();
        }
        // The relay's own request is not traced, so the one export is the client's.
        assertEquals(1, exports.size());
        ExportTraceServiceRequest request = ExportTraceServiceRequest.parseFrom(
                (byte[])exports.get(0));
        ResourceSpans rs = request.getResourceSpans(0);
        assertEquals("app", attribute(rs.getResource().getAttributesList(), "service.name"),
                "the client's resource is forwarded as the client's");
        Span span = rs.getScopeSpans(0).getSpans(0);
        assertEquals(TRACE, hex(span.getTraceId()));
        assertEquals("GET /pets", span.getName());
        assertEquals(1700000000500000000L, span.getEndTimeUnixNano());
        assertEquals("200", attribute(span.getAttributesList(), "http.response.status_code"));
        assertEquals("Api-Token abc123", authorizations.get(0),
                "the server adds the collector credential the app never had");
    }

    // ------------------------------------------------------------------

    private static Span find(List spans, String name, Span.SpanKind kind) {
        for(int iter = 0 ; iter < spans.size() ; iter++) {
            Span span = (Span)spans.get(iter);
            if(name.equals(span.getName()) && span.getKind() == kind) {
                return span;
            }
        }
        throw new AssertionError("no " + kind + " span named " + name + " in " + spans);
    }

    private static String attribute(List attributes, String key) {
        for(int iter = 0 ; iter < attributes.size() ; iter++) {
            KeyValue kv = (KeyValue)attributes.get(iter);
            if(kv.getKey().equals(key)) {
                if(kv.getValue().hasIntValue()) {
                    return String.valueOf(kv.getValue().getIntValue());
                }
                return kv.getValue().getStringValue();
            }
        }
        return null;
    }

    private static String hex(ByteString bytes) {
        StringBuilder out = new StringBuilder();
        for(int iter = 0 ; iter < bytes.size() ; iter++) {
            out.append(String.format("%02x", bytes.byteAt(iter) & 0xff));
        }
        return out.toString();
    }

    private static int post(int port, String path, String body, String token) throws IOException {
        return postTyped(port, path, body, token, "application/json");
    }

    private static int postTyped(int port, String path, String body, String token, String type)
            throws IOException {
        HttpURLConnection connection = (HttpURLConnection)new URL(
                "http://127.0.0.1:" + port + path).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", type);
        if(token != null) {
            connection.setRequestProperty("X-CN1-Telemetry-Token", token);
        }
        OutputStream out = connection.getOutputStream();
        out.write(body.getBytes("UTF-8"));
        out.close();
        return connection.getResponseCode();
    }

    private static byte[] ascii(String value) {
        try {
            return value.getBytes("US-ASCII");
        } catch (IOException err) {
            throw new IllegalStateException(err);
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int n;
        while((n = in.read(chunk)) > 0) {
            out.write(chunk, 0, n);
        }
        in.close();
        return out.toByteArray();
    }

    private static int freePort() throws IOException {
        java.net.ServerSocket probe = new java.net.ServerSocket(0);
        try {
            return probe.getLocalPort();
        } finally {
            probe.close();
        }
    }
}
