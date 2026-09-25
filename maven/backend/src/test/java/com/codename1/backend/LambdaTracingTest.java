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
package com.codename1.backend;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Lambda loop's span, and the span a failing tracer leaves behind. */
class LambdaTracingTest {

    @AfterEach
    void uninstall() {
        Tracing.install(null);
    }

    @Test
    @DisplayName("an invocation whose result the runtime API refuses ends as a failure")
    void refusedDeliveryIsRecorded() throws Exception {
        HttpServer api = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        api.createContext("/2018-06-01/runtime/invocation/next", (HttpExchange ex) -> {
            byte[] body = "{}".getBytes("UTF-8");
            ex.getResponseHeaders().add("Lambda-Runtime-Aws-Request-Id", "req-1");
            ex.sendResponseHeaders(200, body.length);
            ex.getResponseBody().write(body);
            ex.close();
        });
        api.createContext("/2018-06-01/runtime/invocation/req-1/response", (HttpExchange ex) -> {
            drain(ex);
            ex.sendResponseHeaders(413, -1);
            ex.close();
        });
        api.createContext("/2018-06-01/runtime/invocation/req-1/error", (HttpExchange ex) -> {
            drain(ex);
            ex.sendResponseHeaders(202, -1);
            ex.close();
        });
        api.start();
        Recorder recorder = new Recorder();
        Tracing.install(recorder);
        try {
            LambdaRuntime.pumpOnce(new Handler() {
                public String handle(String event, String requestId) {
                    return "{\"ok\":true}";
                }
            }, "127.0.0.1", api.getAddress().getPort());
        } finally {
            api.stop(0);
        }
        assertEquals(1, recorder.spans.size());
        RecordedSpan span = (RecordedSpan)recorder.spans.get(0);
        assertTrue(span.ended, "the invocation span was never ended");
        assertTrue(span.error != null && span.error.indexOf("413") >= 0,
                "the lost result must be on the span, not the handler's success: " + span.error);
    }

    @Test
    @DisplayName("a failed invocation's span stays open through the error report, and says it was lost")
    void anUndeliveredErrorReportIsRecorded() throws Exception {
        final Recorder recorder = new Recorder();
        final boolean[] endedBeforeReport = new boolean[1];
        HttpServer api = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        api.createContext("/2018-06-01/runtime/invocation/next", (HttpExchange ex) -> {
            byte[] body = "{}".getBytes("UTF-8");
            ex.getResponseHeaders().add("Lambda-Runtime-Aws-Request-Id", "req-2");
            ex.sendResponseHeaders(200, body.length);
            ex.getResponseBody().write(body);
            ex.close();
        });
        api.createContext("/2018-06-01/runtime/invocation/req-2/error", (HttpExchange ex) -> {
            drain(ex);
            endedBeforeReport[0] = ((RecordedSpan)recorder.spans.get(0)).ended;
            ex.sendResponseHeaders(500, -1);
            ex.close();
        });
        api.start();
        Tracing.install(recorder);
        boolean keepPolling;
        try {
            keepPolling = LambdaRuntime.pumpOnce(new Handler() {
                public String handle(String event, String requestId) {
                    throw new IllegalStateException("handler failed");
                }
            }, "127.0.0.1", api.getAddress().getPort());
        } finally {
            api.stop(0);
        }
        assertFalse(keepPolling, "an unreported invocation must stop the loop");
        RecordedSpan span = (RecordedSpan)recorder.spans.get(0);
        assertFalse(endedBeforeReport[0], "the span ended before the error report was sent");
        assertTrue(span.ended);
        assertEquals(2, span.errors.size(), String.valueOf(span.errors));
        assertEquals("handler failed", span.errors.get(0));
        assertTrue(String.valueOf(span.errors.get(1)).contains("did not accept the error report"),
                String.valueOf(span.errors));
    }

    @Test
    @DisplayName("a failed start-up whose tracer was replaced meanwhile retires the one it displaced")
    void aRollBackAfterAnotherInstallRetiresWhatItDisplaced() {
        Recorder x = new Recorder();
        Recorder failing = new Recorder();
        Recorder other = new Recorder();
        Tracing.install(x);
        // Start-up A swaps X out, start-up B swaps A out and completes, then A fails.
        Tracing.Swap a = Tracing.swap(failing);
        Tracing.commit(Tracing.swap(other));
        Tracing.rollBack(a);
        assertTrue(Tracing.getTracer() == other, "the later install was undone");
        assertEquals(1, x.shutdowns, "the tracer the failed start-up displaced leaked");
        assertEquals(1, failing.shutdowns, "retired once, by the start-up that replaced it");
    }

    @Test
    @DisplayName("overlapping failed start-ups restore the original tracer and stop both of theirs")
    void nestedRollBacksRestoreTheOriginal() {
        Recorder x = new Recorder();
        Recorder f = new Recorder();
        Recorder g = new Recorder();
        Tracing.install(x);
        // A swaps X for F, B swaps F for G; A fails first, then B.
        Tracing.Swap a = Tracing.swap(f);
        Tracing.Swap b = Tracing.swap(g);
        Tracing.rollBack(a);
        assertTrue(Tracing.getTracer() == g, "B is still starting; its tracer stays");
        assertEquals(0, x.shutdowns, "A retired the original while B could still restore it");
        Tracing.rollBack(b);
        assertTrue(Tracing.getTracer() == x, "the original was not restored");
        assertEquals(0, x.shutdowns);
        assertEquals(1, f.shutdowns, "A's failed tracer");
        assertEquals(1, g.shutdowns, "B's failed tracer");
        // A failed start-up's spans are the ones that explain the failure: its
        // tracer gets a window to export them, not a zero timeout.
        assertTrue(g.lastShutdownMillis > 0, "a rolled-back tracer was stopped with no flush window");
        assertTrue(f.lastShutdownMillis > 0);

        // And the other order: B fails first, then A.
        Tracing.Swap a2 = Tracing.swap(f = new Recorder());
        Tracing.Swap b2 = Tracing.swap(g = new Recorder());
        Tracing.rollBack(b2);
        assertTrue(Tracing.getTracer() == f);
        Tracing.rollBack(a2);
        assertTrue(Tracing.getTracer() == x);
        assertEquals(0, x.shutdowns);
        assertEquals(1, f.shutdowns);
        assertEquals(1, g.shutdowns);
    }

    @Test
    @DisplayName("a server that stops while another is starting cannot have its tracer restored")
    void aStoppedServersTracerIsNotRestoredByARollBack() {
        Recorder a = new Recorder();
        Recorder b = new Recorder();
        Tracing.install(a);
        // B starts over A's tracer; A stops; then B fails.
        Tracing.Swap claim = Tracing.swap(b);
        Tracing.shutdown(a, 0);
        assertEquals(1, a.shutdowns, "the stopped server's tracer kept running");
        Tracing.rollBack(claim);
        assertTrue(Tracing.getTracer() == null,
                "a failed start-up put back the tracer of a server that had stopped");
        assertEquals(1, a.shutdowns);
        assertEquals(1, b.shutdowns);
    }

    @Test
    @DisplayName("a caller's tracestate alone is its own trace context")
    void aStandaloneTracestateIsTheCallers() {
        List lines = new ArrayList();
        lines.add("Accept: application/json");
        assertFalse(Tracing.callerTraceparent(lines));
        lines.add("tracestate: vendor=opaque");
        assertTrue(Tracing.callerTraceparent(lines),
                "a traceparent of ours would have been paired with the caller's state");
        List other = new ArrayList();
        other.add("tracestatement: not-a-trace-header");
        assertFalse(Tracing.callerTraceparent(other));
        other.add("Traceparent: 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");
        assertTrue(Tracing.callerTraceparent(other));
    }

    @Test
    @DisplayName("a span whose decoration throws is still ended")
    void abandonedSpansAreEnded() throws Exception {
        Recorder recorder = new Recorder();
        recorder.throwOnAttributes = true;
        Tracing.install(recorder);
        assertEquals(null, Tracing.startLambda(null, "req"));
        assertEquals(1, recorder.spans.size());
        RecordedSpan span = (RecordedSpan)recorder.spans.get(0);
        assertTrue(span.ended, "a span dropped after a decoration failure was never ended");
        assertTrue(span.discarded, "an incomplete span must not be exported");
    }

    private static void drain(HttpExchange ex) throws IOException {
        byte[] chunk = new byte[4096];
        while(ex.getRequestBody().read(chunk) > 0) {
            // Reading the request fully before answering.
        }
    }

    private static final class Recorder implements Tracer {
        final List spans = new ArrayList();
        boolean throwOnAttributes;

        public boolean open(Config config) {
            return true;
        }

        public Span startSpan(String name, int kind, Span parent, String traceparent,
                              String tracestate) {
            RecordedSpan span = new RecordedSpan(throwOnAttributes);
            spans.add(span);
            return span;
        }

        public void flush(int timeoutMillis) {
        }

        int shutdowns;
        int lastShutdownMillis = -1;

        public void shutdown(int timeoutMillis) {
            shutdowns++;
            lastShutdownMillis = timeoutMillis;
        }

        public com.codename1.backend.HttpServer.Handler relay() {
            return null;
        }

        public void metrics(Map out) {
        }
    }

    private static final class RecordedSpan extends Span {
        private final boolean throwOnAttributes;
        boolean ended;
        boolean discarded;
        String error;
        final List errors = new ArrayList();

        RecordedSpan(boolean throwOnAttributes) {
            this.throwOnAttributes = throwOnAttributes;
        }

        private Span attr() {
            if(throwOnAttributes) {
                throw new IllegalStateException("tracer bug");
            }
            return this;
        }

        public Span setAttribute(String key, String value) {
            return attr();
        }

        public Span setAttribute(String key, long value) {
            return attr();
        }

        public Span setAttribute(String key, double value) {
            return attr();
        }

        public Span setAttribute(String key, boolean value) {
            return attr();
        }

        public Span recordException(Throwable err) {
            error = String.valueOf(err.getMessage());
            errors.add(error);
            return this;
        }

        public Span setError(String description) {
            error = description;
            return this;
        }

        public Span updateName(String name) {
            return this;
        }

        public String getName() {
            return "invoke";
        }

        public int getKind() {
            return KIND_SERVER;
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
            discarded = true;
        }

        public void end() {
            ended = true;
        }
    }
}
