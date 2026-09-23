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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The websocket server, over a real socket.
 *
 * Everything here goes through {@link RawWebSocketClient} rather than a websocket
 * library, for the reason the HTTP integration tests give for the same choice: the
 * functional half could be written against any client, and the protocol half
 * cannot, because no conformant client will send the frames a server has to get
 * right.
 *
 * This runs on the Java SE arm, which is always POOL mode -- `VirtualThread.supported()`
 * is false on a JVM. The virtual-thread arm runs the same `src/` code over a
 * different `impl/`, and is covered by the translated tests in `vm/tests`.
 */
class WebSocketServerTest {

    private static HttpServer server;
    private static int port;
    private static final List EVENTS = Collections.synchronizedList(new ArrayList());

    @BeforeAll
    static void startServer() throws Exception {
        final WebSocket echo = new WebSocket() {
            public void onOpen(WebSocketSession session) {
                EVENTS.add("open path=" + session.getPath() + " query=" + session.getQuery()
                        + " subprotocol=" + session.getSubprotocol());
            }
            public void onText(WebSocketSession session, String message) throws IOException {
                session.sendText(message);
            }
            public void onBinary(WebSocketSession session, byte[] message, int offset, int length)
                    throws IOException {
                session.sendBinary(message, offset, length);
            }
            public void onClose(WebSocketSession session, int code, String reason) {
                EVENTS.add("close " + code);
            }
            public String[] getSubprotocols() {
                return new String[]{"v2.chat", "v1.chat"};
            }
        };
        server = HttpServer.start("127.0.0.1", 0, 64, 4, new HttpServer.Handler() {
            public HttpServer.Response handle(HttpServer.Request request) {
                return HttpServer.Response.text(200, "plain http");
            }
        }, null, new HttpServer.WebSocketRoutes() {
            public void register(HttpServer.WebSocketRegistry registry) {
                registry.route("/echo", echo);
            }
        });
        port = server.getPort();
    }

    @AfterAll
    static void stopServer() {
        if(server != null) {
            server.stop(2000);
        }
    }

    @Test
    @DisplayName("an ordinary HTTP request on the same server is untouched")
    void plainHttpStillWorks() throws Exception {
        Socket socket = new Socket("127.0.0.1", port);
        socket.setSoTimeout(5000);
        try {
            OutputStream out = socket.getOutputStream();
            out.write("GET /anything HTTP/1.1\r\nHost: x\r\n\r\n".getBytes("ISO-8859-1"));
            out.flush();
            byte[] buffer = new byte[256];
            int read = socket.getInputStream().read(buffer);
            assertTrue(new String(buffer, 0, read, "ISO-8859-1").startsWith("HTTP/1.1 200"));
        } finally {
            socket.close();
        }
    }

    @Test
    @DisplayName("handshake, echo, and the server's subprotocol preference")
    void handshakeAndEcho() throws Exception {
        RawWebSocketClient client = new RawWebSocketClient(port, "/echo?room=7",
                "Sec-WebSocket-Protocol: v1.chat, v2.chat\r\n");
        try {
            assertTrue(client.getStatusLine().startsWith("HTTP/1.1 101"), client.getStatusLine());
            assertEquals("websocket", client.getResponseHeader("upgrade"));
            assertEquals("Upgrade", client.getResponseHeader("connection"));
            // The client listed v1 first; the server's own order decides.
            assertEquals("v2.chat", client.getResponseHeader("sec-websocket-protocol"));
            client.sendText("hello");
            assertTrue(client.readFrame());
            assertEquals(WebSocketFrames.OP_TEXT, client.getLastOpcode());
            assertEquals("hello", client.getLastText());
            assertTrue(EVENTS.contains("open path=/echo query=room=7 subprotocol=v2.chat"),
                    "path and query are snapshotted at upgrade: " + EVENTS);
        } finally {
            client.close();
        }
    }

    @Test
    @DisplayName("a 300KB message fragmented at 64KB round-trips, which is the device shape")
    void largeFragmentedBinary() throws Exception {
        RawWebSocketClient client = new RawWebSocketClient(port);
        try {
            byte[] payload = new byte[300000];
            new Random(4L).nextBytes(payload);
            int chunk = 65536;                    // what the hand-rolled clients use
            for(int offset = 0 ; offset < payload.length ; offset += chunk) {
                int length = Math.min(chunk, payload.length - offset);
                byte[] piece = new byte[length];
                System.arraycopy(payload, offset, piece, 0, length);
                boolean first = offset == 0;
                boolean last = offset + length >= payload.length;
                client.send(last, 0,
                        first ? WebSocketFrames.OP_BINARY : WebSocketFrames.OP_CONTINUATION,
                        true, piece, 0);
            }
            ByteArrayOutputStream echoed = new ByteArrayOutputStream();
            while(echoed.size() < payload.length && client.readFrame()) {
                echoed.write(client.getLastPayload());
            }
            assertArrayEquals(payload, echoed.toByteArray());
        } finally {
            client.close();
        }
    }

    @Test
    @DisplayName("a frame delivered one byte per packet still decodes")
    void frameSplitAcrossEveryByte() throws Exception {
        RawWebSocketClient client = new RawWebSocketClient(port);
        try {
            // Every field spans a read boundary: the two header bytes, the mask
            // key, and the payload. This is the case a decoder that assumes a
            // whole frame per read gets wrong.
            client.dribble(client.buildFrame(true, 0, WebSocketFrames.OP_TEXT, true,
                    Utf8.encode("dribbled"), 0));
            assertTrue(client.readFrame());
            assertEquals("dribbled", client.getLastText());
        } finally {
            client.close();
        }
    }

    @Test
    @DisplayName("a frame pipelined into the same packet as the handshake is not lost")
    void framePipelinedWithHandshake() throws Exception {
        // This is what a browser does, and it is the case the borrowed-buffer
        // handoff exists for: those bytes are sitting in the connection's read
        // buffer when the upgrade happens, and a session that starts by reading
        // the socket would never see them.
        Socket socket = new Socket("127.0.0.1", port);
        socket.setSoTimeout(5000);
        try {
            byte[] nonce = new byte[16];
            new Random(8L).nextBytes(nonce);
            String key = Base64.encode(nonce);
            ByteArrayOutputStream packet = new ByteArrayOutputStream();
            packet.write(("GET /echo HTTP/1.1\r\nHost: x\r\nUpgrade: websocket\r\n"
                    + "Connection: Upgrade\r\nSec-WebSocket-Version: 13\r\n"
                    + "Sec-WebSocket-Key: " + key + "\r\n\r\n").getBytes("ISO-8859-1"));
            byte[] mask = {1, 2, 3, 4};
            byte[] body = Utf8.encode("early");
            packet.write(0x81);
            packet.write(0x80 | body.length);
            packet.write(mask, 0, 4);
            for(int iter = 0 ; iter < body.length ; iter++) {
                packet.write(body[iter] ^ mask[iter & 3]);
            }
            socket.getOutputStream().write(packet.toByteArray());   // ONE write
            socket.getOutputStream().flush();

            InputStream in = socket.getInputStream();
            StringBuilder head = new StringBuilder();
            while(!head.toString().endsWith("\r\n\r\n")) {
                int c = in.read();
                assertTrue(c >= 0, "connection closed during the handshake");
                head.append((char)c);
            }
            assertTrue(head.toString().startsWith("HTTP/1.1 101"), head.toString());
            in.read();                                   // opcode byte
            int length = in.read() & 0x7f;
            byte[] echoed = new byte[length];
            int got = 0;
            while(got < length) {
                int read = in.read(echoed, got, length - got);
                assertTrue(read >= 0, "connection closed before the echo");
                got += read;
            }
            assertEquals("early", Utf8.decode(echoed, 0, echoed.length));
        } finally {
            socket.close();
        }
    }

    @Test
    @DisplayName("a PING is answered with a PONG carrying the same payload")
    void pingIsAnswered() throws Exception {
        RawWebSocketClient client = new RawWebSocketClient(port);
        try {
            client.send(true, 0, WebSocketFrames.OP_PING, true, Utf8.encode("beat"), 0);
            assertTrue(client.readFrame());
            assertEquals(WebSocketFrames.OP_PONG, client.getLastOpcode());
            assertEquals("beat", client.getLastText());
        } finally {
            client.close();
        }
    }

    @Test
    @DisplayName("every protocol violation gets the close code the RFC names")
    void protocolViolations() throws Exception {
        assertCloses(1002, true, 4, WebSocketFrames.OP_TEXT, true, Utf8.encode("x"), 0,
                "a reserved bit with no extension negotiated");
        assertCloses(1002, true, 0, WebSocketFrames.OP_TEXT, false, Utf8.encode("x"), 0,
                "an unmasked client frame");
        assertCloses(1002, true, 0, 3, true, Utf8.encode("x"), 0,
                "a reserved data opcode");
        assertCloses(1002, true, 0, 0xb, true, Utf8.encode("x"), 0,
                "a reserved control opcode");
        assertCloses(1002, false, 0, WebSocketFrames.OP_PING, true, Utf8.encode("x"), 0,
                "a fragmented control frame");
        assertCloses(1002, true, 0, WebSocketFrames.OP_PING, true, new byte[126], 0,
                "a control frame over 125 bytes");
        assertCloses(1002, true, 0, WebSocketFrames.OP_CONTINUATION, true, Utf8.encode("x"), 0,
                "a continuation with no message open");
        assertCloses(1002, true, 0, WebSocketFrames.OP_TEXT, true, Utf8.encode("abc"), 2,
                "a length that could have been spelled shorter");
        assertCloses(1007, true, 0, WebSocketFrames.OP_TEXT, true,
                new byte[]{(byte)0xc0, (byte)0x80}, 0, "invalid UTF-8 in a text frame");
        assertCloses(1002, true, 0, WebSocketFrames.OP_CLOSE, true,
                new byte[]{0x03, (byte)0xEC}, 0, "close code 1004, which is reserved");
        assertCloses(1002, true, 0, WebSocketFrames.OP_CLOSE, true, new byte[]{0x01}, 0,
                "a one-byte close payload");
    }

    @Test
    @DisplayName("a text message whose UTF-8 breaks mid-fragment fails at that fragment")
    void invalidUtf8AcrossFragments() throws Exception {
        RawWebSocketClient client = new RawWebSocketClient(port);
        try {
            // A valid start, then a continuation that is not valid UTF-8. A server
            // that only validates at FIN would accept this fragment and wait for
            // more; Autobahn calls that NON-STRICT.
            client.send(false, 0, WebSocketFrames.OP_TEXT, true, Utf8.encode("ok"), 0);
            client.send(true, 0, WebSocketFrames.OP_CONTINUATION, true,
                    new byte[]{(byte)0xf5, (byte)0x80}, 0);
            assertEquals(1007, client.readUntilClose());
        } finally {
            client.close();
        }
    }

    @Test
    @DisplayName("a new data frame while a message is still open is refused")
    void interleavedDataFrames() throws Exception {
        RawWebSocketClient client = new RawWebSocketClient(port);
        try {
            client.send(false, 0, WebSocketFrames.OP_TEXT, true, Utf8.encode("one"), 0);
            client.send(true, 0, WebSocketFrames.OP_TEXT, true, Utf8.encode("two"), 0);
            assertEquals(1002, client.readUntilClose());
        } finally {
            client.close();
        }
    }

    @Test
    @DisplayName("a PING between two fragments is answered without breaking reassembly")
    void controlFrameBetweenFragments() throws Exception {
        RawWebSocketClient client = new RawWebSocketClient(port);
        try {
            client.send(false, 0, WebSocketFrames.OP_TEXT, true, Utf8.encode("frag"), 0);
            client.send(true, 0, WebSocketFrames.OP_PING, true, Utf8.encode("mid"), 0);
            assertTrue(client.readFrame());
            assertEquals(WebSocketFrames.OP_PONG, client.getLastOpcode());
            assertEquals("mid", client.getLastText());
            client.send(true, 0, WebSocketFrames.OP_CONTINUATION, true, Utf8.encode("ment"), 0);
            assertTrue(client.readFrame());
            assertEquals(WebSocketFrames.OP_TEXT, client.getLastOpcode());
            assertEquals("fragment", client.getLastText(),
                    "the interleaved control frame must not join the message");
        } finally {
            client.close();
        }
    }

    @Test
    @DisplayName("a close is echoed, which completes the handshake")
    void closeHandshake() throws Exception {
        RawWebSocketClient client = new RawWebSocketClient(port);
        try {
            client.send(true, 0, WebSocketFrames.OP_CLOSE, true, new byte[]{0x03, (byte)0xE8}, 0);
            assertEquals(1000, client.readUntilClose());
        } finally {
            client.close();
        }
    }

    @Test
    @DisplayName("the handshake is refused with the status that says why")
    void handshakeRefusals() throws Exception {
        assertRefused("Sec-WebSocket-Version: 8\r\n", "HTTP/1.1 426", "a version this server does not speak");
        assertRefused("Sec-WebSocket-Key: extra\r\n", "HTTP/1.1 400", "a second Sec-WebSocket-Key");
        // The body has to actually be sent. Declaring one and withholding it is
        // an incomplete request, and the server correctly waits for it -- which
        // is the slowloris deadline's job, not this check's. By the time the
        // upgrade check runs, readRequest has consumed the body, so the refusal
        // is about the request being wrong rather than about stray bytes.
        assertRefused("Content-Length: 3\r\n", "abc", "HTTP/1.1 400",
                "a handshake carrying a body");

        RawWebSocketClient unrouted = new RawWebSocketClient(port, "/no-such-route", null);
        try {
            assertTrue(unrouted.getStatusLine().startsWith("HTTP/1.1 404"), unrouted.getStatusLine());
        } finally {
            unrouted.close();
        }
    }

    @Test
    @DisplayName("a 426 names the version, so a client can retry rather than guess")
    void upgradeRequiredNamesTheVersion() throws Exception {
        String response = rawHandshake("Sec-WebSocket-Version: 8\r\n");
        assertTrue(response.startsWith("HTTP/1.1 426"), response);
        assertTrue(response.contains("Sec-WebSocket-Version: 13"), response);
    }

    @Test
    @DisplayName("a session idle past the HTTP timeout survives")
    void idleLongerThanTheRequestTimeout() throws Exception {
        // The HTTP read deadline is about a client that began a request and
        // stopped; a websocket parked between messages looks exactly like one.
        // On the virtual-thread arm this was fatal -- advance() re-arms
        // SOCKET_TIMEOUT_MILLIS on EVERY park, so it overwrote the websocket's
        // own allowance and closed any connection quiet for more than fifteen
        // seconds, with both peers still believing it was open. The browser
        // screenshot suite delivered 31 of 181 images before its socket went away
        // with nothing logged on either side.
        //
        // This arm is pool mode and does not go through advance(), so what it
        // pins is the other half of the same rule: the deadline armed at upgrade
        // outlives the request timeout, and a quiet session is still there
        // afterwards.
        RawWebSocketClient client = new RawWebSocketClient(port);
        try {
            client.sendText("before");
            assertTrue(client.readFrame());
            assertEquals("before", client.getLastText());

            Thread.sleep(IDLE_PAST_REQUEST_TIMEOUT_MILLIS);

            client.sendText("after");
            assertTrue(client.readFrame(),
                    "the session was closed while it was merely idle");
            assertEquals("after", client.getLastText());
        } finally {
            client.close();
        }
    }

    /**
     * Comfortably past the default request timeout and nowhere near the websocket
     * one, so the test proves the distinction rather than the wall clock.
     */
    private static final long IDLE_PAST_REQUEST_TIMEOUT_MILLIS = 3000;

    @Test
    @DisplayName("an open websocket is a connection, not an active request")
    void idleSessionIsNotSaturation() throws Exception {
        RawWebSocketClient client = new RawWebSocketClient(port);
        try {
            // Give the upgrade a moment to register before reading the gauges.
            for(int attempt = 0 ; attempt < 50 ; attempt++) {
                Object count = server.getMetrics().get("webSocketConnections");
                if(count != null && ((Integer)count).intValue() >= 1) {
                    break;
                }
                Thread.sleep(20);
            }
            assertTrue(((Integer)server.getMetrics().get("webSocketConnections")).intValue() >= 1,
                    "the session shows in the metrics");
            // If this ever fails, stop() waits out its whole drain window for every
            // idle websocket, every time.
            assertEquals(0, server.getActiveRequests(),
                    "an idle websocket must not read as an in-flight request");
        } finally {
            client.close();
        }
    }

    // ------------------------------------------------------------------ helpers

    private void assertCloses(int expectedCode, boolean fin, int rsv, int opcode, boolean masked,
                              byte[] payload, int lengthWidth, String what) throws Exception {
        RawWebSocketClient client = new RawWebSocketClient(port);
        try {
            client.send(fin, rsv, opcode, masked, payload, lengthWidth);
            assertEquals(expectedCode, client.readUntilClose(), what);
        } finally {
            client.close();
        }
    }

    private void assertRefused(String extraHeaders, String expectedStatus, String what)
            throws Exception {
        assertRefused(extraHeaders, null, expectedStatus, what);
    }

    private void assertRefused(String extraHeaders, String body, String expectedStatus,
                               String what) throws Exception {
        String response = rawHandshake(extraHeaders, body);
        assertTrue(response.startsWith(expectedStatus), what + ": " + response);
    }

    private String rawHandshake(String extraHeaders) throws Exception {
        return rawHandshake(extraHeaders, null);
    }

    private String rawHandshake(String extraHeaders, String body) throws Exception {
        Socket socket = new Socket("127.0.0.1", port);
        socket.setSoTimeout(5000);
        try {
            byte[] nonce = new byte[16];
            new Random(3L).nextBytes(nonce);
            String request = "GET /echo HTTP/1.1\r\nHost: x\r\nUpgrade: websocket\r\n"
                    + "Connection: Upgrade\r\nSec-WebSocket-Version: 13\r\n"
                    + "Sec-WebSocket-Key: " + Base64.encode(nonce) + "\r\n"
                    + extraHeaders + "\r\n" + (body == null ? "" : body);
            socket.getOutputStream().write(request.getBytes("ISO-8859-1"));
            socket.getOutputStream().flush();
            byte[] buffer = new byte[1024];
            int read = socket.getInputStream().read(buffer);
            assertTrue(read > 0, "the server answered nothing");
            return new String(buffer, 0, read, "ISO-8859-1");
        } finally {
            socket.close();
        }
    }
}
