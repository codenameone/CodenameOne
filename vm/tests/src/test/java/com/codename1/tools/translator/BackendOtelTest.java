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
package com.codename1.tools.translator;

import com.google.protobuf.ByteString;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.proto.trace.v1.Status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * OpenTelemetry tracing on the PACKAGED runtime.
 *
 * <p>The JVM arm has its own end-to-end test in the backend module; this one is
 * for what only a translated binary can get wrong. The encoder, the id
 * generator and the exporter thread all run as translated C here, outbound HTTP is
 * libcurl -- whose redirect rule had to learn that trace headers are not the
 * caller's -- and the claim that an untraced server carries no tracer is a claim
 * about what the translator leaves in the binary, which only a binary can answer.
 */
class BackendOtelTest {
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String CALLER_SPAN = "00f067aa0ba902b7";
    /** The trace the HTTP/2 request carries, so its span is told from the HTTP/1 ones. */
    private static final String H2_TRACE = "0af7651916cd43dd8448eb211c80319c";
    /** The traces the websocket handshakes carry: one accepted, one refused. */
    private static final String WS_TRACE = "5b8aa5a2d2c872e8321cf37308d69df2";
    private static final String WS_REFUSED_TRACE = "7d0a1e4bb7c9a2f35e61d8c04f2b9a13";

    @Test
    @DisplayName("a translated server exports one connected trace, and an untraced one carries no tracer")
    void tracesOnThePackagedRuntime() throws Exception {
        if (CompilerHelper.isWindows()) {
            BackendTestSupport.skipOrFail("the server-side backend is POSIX-only for now");
        }
        Path jdk8 = BackendTestSupport.findJdk8();
        BackendTestSupport.require(jdk8 != null, "no JDK 8 available to compile the backend");
        BackendTestSupport.require(BackendTestSupport.hasCommand("nm"),
                "nm is needed to read the binaries' symbols");
        Path work = Files.createTempDirectory("backend-otel");
        Path traced = work.resolve("otelserver");
        String failure = BackendTestSupport.build("OtelServer", "demo/oteltest", traced, jdk8);
        if (failure != null) {
            BackendTestSupport.skipOrFail(failure);
        }

        // THE ZERO-COST CLAIM, from the binaries. WebCheck is a program that never
        // mentions a tracer; it is compiled against the same runtime tree -- the
        // otel package included -- so the only thing keeping the tracer out of it
        // is the translator's reachability. The traced binary is the control: a
        // symbol spelling that matched nothing would pass the first assertion for
        // any build.
        Path untraced = work.resolve("webcheck");
        failure = BackendTestSupport.build("WebCheck", "demo/webcheck", untraced, jdk8);
        if (failure != null) {
            BackendTestSupport.skipOrFail(failure);
        }
        assertTrue(otelSymbols(traced) > 0,
                "the traced binary carries no tracer symbols; the check below would be vacuous");
        assertEquals(0, otelSymbols(untraced),
                "a server that never asked for tracing links the tracer anyway");

        final List exports = Collections.synchronizedList(new ArrayList());
        final List contentTypes = Collections.synchronizedList(new ArrayList());
        final List authorizations = Collections.synchronizedList(new ArrayList());
        final Map downstreamHeaders = Collections.synchronizedMap(new HashMap());
        HttpServer peer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        peer.createContext("/v1/traces", (HttpExchange exchange) -> {
            exports.add(readAll(exchange.getRequestBody()));
            contentTypes.add(exchange.getRequestHeaders().getFirst("Content-Type"));
            authorizations.add(String.valueOf(exchange.getRequestHeaders().getFirst("Authorization")));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        peer.createContext("/hop", (HttpExchange exchange) -> {
            exchange.getResponseHeaders().add("Location", "/downstream");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        peer.createContext("/downstream", (HttpExchange exchange) -> {
            downstreamHeaders.put("traceparent", exchange.getRequestHeaders().getFirst("traceparent"));
            downstreamHeaders.put("tracestate", exchange.getRequestHeaders().getFirst("tracestate"));
            byte[] body = "down".getBytes("UTF-8");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        peer.start();
        String peerBase = "http://127.0.0.1:" + peer.getAddress().getPort();

        int port = BackendTestSupport.freePort();
        Map<String, String> env = new HashMap<String, String>();
        env.put("PORT", String.valueOf(port));
        env.put("OTEL_EXPORTER_OTLP_ENDPOINT", peerBase);
        env.put("OTEL_EXPORTER_OTLP_HEADERS", "Authorization=Api-Token%20t0k");
        env.put("OTEL_BSP_SCHEDULE_DELAY", "200");
        env.put("CN1_OTEL_RELAY", "true");
        env.put("CN1_OTEL_DOWNSTREAM", peerBase);
        Path log = work.resolve("server.log");
        Process server = BackendTestSupport.start(traced, env, log);
        try {
            assertTrue(BackendTestSupport.waitForPort(port, 30000),
                    "the traced server never listened:\n" + read(log));
            HttpURLConnection request = (HttpURLConnection) new URL(
                    "http://127.0.0.1:" + port + "/work?token=secret").openConnection();
            request.setRequestProperty("traceparent", "00-" + TRACE + "-" + CALLER_SPAN + "-01");
            request.setRequestProperty("tracestate", "vendor=opaque");
            assertEquals(200, request.getResponseCode(), read(log));
            // "Rex" from the database, and the outbound call FOLLOWED the redirect
            // to reach "down": carrying trace headers did not make libcurl treat
            // the call as one with credentials to protect.
            assertEquals("Rex 200 down", new String(readAll(request.getInputStream()), "UTF-8"));

            HttpURLConnection boom = (HttpURLConnection) new URL(
                    "http://127.0.0.1:" + port + "/boom").openConnection();
            assertEquals(500, boom.getResponseCode());

            assertEquals(200, relay(port, "{\"resourceSpans\":[{\"resource\":{\"attributes\":"
                    + "[{\"key\":\"service.name\",\"value\":{\"stringValue\":\"app\"}}]},"
                    + "\"scopeSpans\":[{\"spans\":[{\"traceId\":\"" + TRACE + "\",\"spanId\":\""
                    + CALLER_SPAN + "\",\"name\":\"tap\",\"kind\":3,\"startTimeUnixNano\":"
                    + "\"1700000000000000000\",\"endTimeUnixNano\":\"1700000000100000000\"}]}]}]}"));

            // Over HTTP/2 as well: its span ends only after the response is
            // written, and :authority -- HTTP/2's Host -- has to reach
            // server.address just as Host does.
            assertTrue(http2Get(port, "/h2probe", "00-" + H2_TRACE + "-" + CALLER_SPAN + "-01"),
                    "no HTTP/2 response came back:\n" + read(log));

            // A websocket handshake is a request with a span of its own, and what
            // onOpen does is its child; a refused one is a span with the refusal.
            assertEquals("101", handshake(port, "/ws", WS_TRACE), read(log));
            assertEquals("404", handshake(port, "/nows", WS_REFUSED_TRACE), read(log));

            List spans = awaitSpans(exports, 9, 30000);
            assertEquals("application/x-protobuf", contentTypes.get(0));
            assertEquals("Api-Token t0k", authorizations.get(0));

            Span work0 = find(spans, "GET /work", Span.SpanKind.SPAN_KIND_SERVER);
            Span query = null;
            for (int iter = 0; iter < spans.size(); iter++) {
                Span s = (Span) spans.get(iter);
                if ("SELECT".equals(s.getName()) && hex(s.getParentSpanId()).equals(hex(work0.getSpanId()))) {
                    query = s;
                }
            }
            assertTrue(query != null, "no statement under GET /work in " + spans);
            Span outbound = find(spans, "GET", Span.SpanKind.SPAN_KIND_CLIENT);
            Span failed = null;
            Span overH2 = null;
            for (int iter = 0; iter < spans.size(); iter++) {
                Span s = (Span) spans.get(iter);
                if (s.getKind() != Span.SpanKind.SPAN_KIND_SERVER) {
                    continue;
                }
                if (H2_TRACE.equals(hex(s.getTraceId()))) {
                    overH2 = s;
                } else if (s.getStatus().getCode() == Status.StatusCode.STATUS_CODE_ERROR) {
                    failed = s;
                }
            }
            Span accepted = null;
            Span refused = null;
            Span onOpenQuery = null;
            for (int iter = 0; iter < spans.size(); iter++) {
                Span s = (Span) spans.get(iter);
                String trace = hex(s.getTraceId());
                if (WS_TRACE.equals(trace) && s.getKind() == Span.SpanKind.SPAN_KIND_SERVER) {
                    accepted = s;
                } else if (WS_TRACE.equals(trace)) {
                    onOpenQuery = s;
                } else if (WS_REFUSED_TRACE.equals(trace)) {
                    refused = s;
                }
            }
            assertTrue(accepted != null, "the accepted handshake has no span: " + spans);
            assertEquals("101", attribute(accepted.getAttributesList(), "http.response.status_code"));
            assertEquals("/ws", attribute(accepted.getAttributesList(), "url.path"));
            // Named after the endpoint it reached, as an HTTP route is: otherwise
            // every endpoint's handshake is one operation called "GET".
            assertEquals("GET /ws", accepted.getName());
            assertEquals("/ws", attribute(accepted.getAttributesList(), "http.route"));
            assertTrue(onOpenQuery != null, "onOpen's statement is not in the handshake's trace");
            assertEquals(hex(accepted.getSpanId()), hex(onOpenQuery.getParentSpanId()),
                    "onOpen's work is a child of the handshake");
            assertTrue(refused != null, "the refused handshake has no span: " + spans);
            assertEquals("404", attribute(refused.getAttributesList(), "http.response.status_code"));
            assertTrue(failed != null, "no failed server span in " + spans);
            assertTrue(overH2 != null, "no span for the HTTP/2 request in " + spans);
            assertEquals(CALLER_SPAN, hex(overH2.getParentSpanId()));
            assertEquals("2", attribute(overH2.getAttributesList(), "network.protocol.version"));
            assertEquals("127.0.0.1", attribute(overH2.getAttributesList(), "server.address"),
                    "HTTP/2's :authority did not reach server.address");
            assertEquals("404", attribute(overH2.getAttributesList(), "http.response.status_code"),
                    "the span of a written HTTP/2 response carries the status sent");
            Span relayed = find(spans, "tap", Span.SpanKind.SPAN_KIND_CLIENT);

            assertEquals(TRACE, hex(work0.getTraceId()));
            assertEquals(CALLER_SPAN, hex(work0.getParentSpanId()));
            assertEquals("vendor=opaque", work0.getTraceState());
            assertEquals("/work", attribute(work0.getAttributesList(), "url.path"));
            assertEquals("/work", attribute(work0.getAttributesList(), "http.route"));
            assertEquals(hex(work0.getSpanId()), hex(query.getParentSpanId()));
            assertEquals("sqlite", attribute(query.getAttributesList(), "db.system"));
            assertEquals(hex(work0.getSpanId()), hex(outbound.getParentSpanId()));
            assertTrue(work0.getEndTimeUnixNano() >= outbound.getEndTimeUnixNano());

            // What actually went over the wire to the service the call reached.
            assertEquals("00-" + TRACE + "-" + hex(outbound.getSpanId()) + "-01",
                    downstreamHeaders.get("traceparent"));
            assertEquals("vendor=opaque", downstreamHeaders.get("tracestate"));

            assertEquals(Status.StatusCode.STATUS_CODE_ERROR, failed.getStatus().getCode());
            assertEquals("java.lang.IllegalStateException",
                    attribute(failed.getEvents(0).getAttributesList(), "exception.type"));

            assertEquals(TRACE, hex(relayed.getTraceId()),
                    "the app's span, relayed through the translated server");
        } finally {
            BackendTestSupport.stop(server);
            peer.stop(0);
        }
    }

    /** How many translated symbols of the tracer package a binary holds. */
    private static int otelSymbols(Path binary) throws Exception {
        String symbols = BackendTestSupport.run(Arrays.asList("nm", binary.toString()), 120);
        int count = 0;
        int at = 0;
        while ((at = symbols.indexOf("com_codename1_backend_otel_", at)) >= 0) {
            count++;
            at++;
        }
        return count;
    }

    private static List awaitSpans(List exports, int wanted, long timeoutMillis) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        List spans = new ArrayList();
        while (System.currentTimeMillis() < deadline) {
            spans = new ArrayList();
            Object[] snapshot = exports.toArray();
            for (int iter = 0; iter < snapshot.length; iter++) {
                ExportTraceServiceRequest request =
                        ExportTraceServiceRequest.parseFrom((byte[]) snapshot[iter]);
                for (ResourceSpans rs : request.getResourceSpansList()) {
                    for (ScopeSpans ss : rs.getScopeSpansList()) {
                        spans.addAll(ss.getSpansList());
                    }
                }
            }
            if (spans.size() >= wanted) {
                return spans;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("expected " + wanted + " spans, the collector received "
                + spans.size() + ": " + spans);
    }

    /**
     * A websocket handshake carrying a traceparent; the status code the server
     * answered, as text. The connection is closed as soon as the status line is in.
     */
    private static String handshake(int port, String path, String trace) throws IOException {
        java.net.Socket socket = new java.net.Socket();
        socket.connect(new InetSocketAddress("127.0.0.1", port), 5000);
        socket.setSoTimeout(10000);
        try {
            OutputStream out = socket.getOutputStream();
            out.write(("GET " + path + " HTTP/1.1\r\n"
                    + "Host: 127.0.0.1\r\n"
                    + "Upgrade: websocket\r\n"
                    + "Connection: Upgrade\r\n"
                    + "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n"
                    + "Sec-WebSocket-Version: 13\r\n"
                    + "traceparent: 00-" + trace + "-" + CALLER_SPAN + "-01\r\n"
                    + "\r\n").getBytes("UTF-8"));
            out.flush();
            InputStream in = socket.getInputStream();
            StringBuilder line = new StringBuilder();
            int c;
            while ((c = in.read()) >= 0 && c != '\n') {
                line.append((char) c);
            }
            String status = line.toString().trim();
            int space = status.indexOf(' ');
            return space < 0 ? status : status.substring(space + 1, Math.min(status.length(), space + 4));
        } finally {
            socket.close();
        }
    }

    /**
     * One GET over cleartext HTTP/2 by prior knowledge, carrying a traceparent;
     * whether a response HEADERS frame came back. The frame layout is
     * BackendHttpIntegrationTest's.
     */
    private static boolean http2Get(int port, String path, String traceparent) throws IOException {
        java.net.Socket socket = new java.net.Socket();
        socket.connect(new InetSocketAddress("127.0.0.1", port), 5000);
        socket.setSoTimeout(10000);
        try {
            OutputStream out = socket.getOutputStream();
            out.write("PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n".getBytes("UTF-8"));
            out.write(h2Frame(4, 0, 0, new byte[0]));
            ByteArrayOutputStream block = new ByteArrayOutputStream();
            hpackLiteral(block, ":method", "GET");
            hpackLiteral(block, ":path", path);
            hpackLiteral(block, ":scheme", "http");
            hpackLiteral(block, ":authority", "127.0.0.1");
            hpackLiteral(block, "traceparent", traceparent);
            out.write(h2Frame(1, 0x05, 1, block.toByteArray()));
            out.flush();
            InputStream in = socket.getInputStream();
            long deadline = System.currentTimeMillis() + 8000;
            while (System.currentTimeMillis() < deadline) {
                byte[] header = readExactly(in, 9);
                if (header == null) {
                    return false;
                }
                int length = ((header[0] & 0xff) << 16) | ((header[1] & 0xff) << 8) | (header[2] & 0xff);
                if (length > 0 && readExactly(in, length) == null) {
                    return false;
                }
                if ((header[3] & 0xff) == 1) {
                    return true;
                }
            }
            return false;
        } finally {
            socket.close();
        }
    }

    private static byte[] h2Frame(int type, int flags, int streamId, byte[] payload) {
        byte[] out = new byte[9 + payload.length];
        out[0] = (byte) ((payload.length >>> 16) & 0xff);
        out[1] = (byte) ((payload.length >>> 8) & 0xff);
        out[2] = (byte) (payload.length & 0xff);
        out[3] = (byte) type;
        out[4] = (byte) flags;
        out[5] = (byte) ((streamId >>> 24) & 0x7f);
        out[6] = (byte) ((streamId >>> 16) & 0xff);
        out[7] = (byte) ((streamId >>> 8) & 0xff);
        out[8] = (byte) (streamId & 0xff);
        System.arraycopy(payload, 0, out, 9, payload.length);
        return out;
    }

    /** An uncompressed HPACK literal, the one form every decoder accepts. */
    private static void hpackLiteral(ByteArrayOutputStream out, String name, String value)
            throws IOException {
        byte[] n = name.getBytes("ISO-8859-1");
        byte[] v = value.getBytes("ISO-8859-1");
        out.write(0x00);
        out.write(n.length);
        out.write(n);
        out.write(v.length);
        out.write(v);
    }

    private static byte[] readExactly(InputStream in, int count) throws IOException {
        byte[] out = new byte[count];
        int filled = 0;
        while (filled < count) {
            int n = in.read(out, filled, count - filled);
            if (n < 0) {
                return null;
            }
            filled += n;
        }
        return out;
    }

    private static Span find(List spans, String name, Span.SpanKind kind) {
        for (int iter = 0; iter < spans.size(); iter++) {
            Span span = (Span) spans.get(iter);
            if (name.equals(span.getName()) && span.getKind() == kind) {
                return span;
            }
        }
        throw new AssertionError("no " + kind + " span named " + name + " in " + spans);
    }

    private static String attribute(List attributes, String key) {
        for (int iter = 0; iter < attributes.size(); iter++) {
            KeyValue kv = (KeyValue) attributes.get(iter);
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
        for (int iter = 0; iter < bytes.size(); iter++) {
            out.append(String.format("%02x", bytes.byteAt(iter) & 0xff));
        }
        return out.toString();
    }

    private static int relay(int port, String json) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(
                "http://127.0.0.1:" + port + "/otel/v1/traces").openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        OutputStream out = connection.getOutputStream();
        out.write(json.getBytes("UTF-8"));
        out.close();
        return connection.getResponseCode();
    }

    private static String read(Path log) {
        try {
            return new String(Files.readAllBytes(log), "UTF-8");
        } catch (IOException err) {
            return "(no log: " + err + ")";
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int n;
        while ((n = in.read(chunk)) > 0) {
            out.write(chunk, 0, n);
        }
        in.close();
        return out.toByteArray();
    }
}
