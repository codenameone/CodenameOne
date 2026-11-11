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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Server-Sent Events: the framing, and real sockets showing each event reaches
/// the client before the producer has finished.
class SseTest {
    private Backend backend;

    @AfterEach
    void stop() {
        if (backend != null) {
            backend.stop();
        }
    }

    private static String frame(Sse.Producer producer) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        producer.produce(new Sse.Events(out, null));
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    @Test
    void framesFieldsAndMultiLineData() throws IOException {
        assertEquals("data: hi\n\n", frame(e -> e.data("hi")));
        assertEquals("event: tick\nid: 7\nretry: 1500\ndata: a\ndata: b\ndata: c\ndata:\ndata: d\n\n",
                frame(e -> e.event("tick").id("7").retry(1500).data("a\nb\r\nc\r\rd")));
        assertEquals("data:\n\n", frame(e -> e.data("")));
        // Fields belong to one event only.
        assertEquals("event: x\ndata: 1\n\ndata: 2\n\n", frame(e -> {
            e.event("x").data("1");
            e.data("2");
        }));
        assertEquals(": note\n\n:\n\n", frame(e -> {
            e.comment("note");
            e.ping();
        }));
        assertEquals("id: 9\n\n", frame(e -> e.id("9").send()));
        assertEquals("data: caf\u00e9 \u2603\n\n", frame(e -> e.data("caf\u00e9 \u2603")));
        Map value = new HashMap();
        value.put("text", "line\nbreak");
        assertEquals("data: {\"text\":\"line\\nbreak\"}\n\n", frame(e -> e.json(value)));
    }

    @Test
    void refusesFieldsThatWouldForgeAnother() {
        Sse.Events events = new Sse.Events(new ByteArrayOutputStream(), null);
        assertThrows(IllegalArgumentException.class, () -> events.event("a\ndata: forged"));
        assertThrows(IllegalArgumentException.class, () -> events.id("1\r2"));
        assertThrows(IllegalArgumentException.class, () -> events.id("1\u00002"));
        assertThrows(IllegalArgumentException.class, () -> events.retry(-1));
    }

    /// Fresh per test, so a rerun still proves the first event arrived early.
    private final CountDownLatch firstRead = new CountDownLatch(1);
    private volatile HttpProxy proxy; //NOPMD AvoidUsingVolatile - written before the server starts, read by its worker

    private int start(boolean compression) throws Exception {
        ServerSocket s = new ServerSocket(0);
        int port = s.getLocalPort();
        s.close();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        if (compression) {
            settings.setProperty(Compression.ENABLED, "true");
            settings.setProperty(Compression.MIN_SIZE, "0");
            settings.setProperty(Compression.MIME_TYPES, "text/event-stream");
        }
        proxy = new HttpProxy("http://127.0.0.1:" + port);
        backend = Backend.builder(Config.of(settings, "test")).quiet().handler(request ->
                request.getTarget().startsWith("/proxy")
                ? proxy.forward(request, "/events")
                : Sse.stream(request, events -> {
                    events.id("1").data("first " + events.lastEventId());
                    // Held until the client has read the first event: a server that
                    // buffered to the end would deadlock here and the test time out.
                    try {
                        if (!firstRead.await(20, TimeUnit.SECONDS)) {
                            throw new IOException("first event never reached the client");
                        }
                    } catch (InterruptedException err) {
                        throw new IOException(err.toString());
                    }
                    events.event("done").data("second");
                })).start();
        return port;
    }

    private String exchange(int port, String request, boolean signal) throws IOException {
        try (Socket socket = new Socket("127.0.0.1", port)) {
            socket.setSoTimeout(30000);
            OutputStream out = socket.getOutputStream();
            out.write(request.getBytes(StandardCharsets.US_ASCII));
            out.flush();
            InputStream in = socket.getInputStream();
            StringBuilder seen = new StringBuilder();
            byte[] buffer = new byte[4096];
            int n;
            while ((n = in.read(buffer)) > 0) {
                seen.append(new String(buffer, 0, n, StandardCharsets.UTF_8));
                if (signal && seen.indexOf("first ") >= 0 && seen.indexOf("\n\n", seen.indexOf("first ")) >= 0) {
                    firstRead.countDown();
                }
            }
            return seen.toString();
        }
    }

    @Test
    void eachEventIsFlushedAsWritten() throws Exception {
        int port = start(true);
        String reply = exchange(port, "GET /events HTTP/1.1\r\nHost: x\r\nAccept-Encoding: gzip\r\n"
                + "Last-Event-ID: 41\r\nConnection: close\r\n\r\n", true);
        assertTrue(reply.startsWith("HTTP/1.1 200"), reply);
        assertTrue(reply.contains("Content-Type: text/event-stream; charset=utf-8"), reply);
        assertTrue(reply.contains("Cache-Control: no-cache"), reply);
        assertTrue(reply.contains("Transfer-Encoding: chunked"), reply);
        // Compression is on for this type with no minimum, and still not applied.
        assertFalse(reply.toLowerCase().contains("content-encoding"), reply);
        assertTrue(reply.contains("id: 1\ndata: first 41\n\n"), reply);
        assertTrue(reply.contains("event: done\ndata: second\n\n"), reply);
    }

    @Test
    void theProxyRelaysEachEventAsItArrives() throws Exception {
        int port = start(false);
        String reply = exchange(port, "GET /proxy HTTP/1.1\r\nHost: x\r\n"
                + "Last-Event-ID: 5\r\nConnection: close\r\n\r\n", true);
        assertTrue(reply.startsWith("HTTP/1.1 200"), reply);
        assertTrue(reply.contains("id: 1\ndata: first 5\n\n"), reply);
        assertTrue(reply.contains("event: done\ndata: second\n\n"), reply);
    }
}
