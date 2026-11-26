/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bazaar.web.security;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Calls to URLs someone else chose: capped while read, timed as a whole,
/// never redirected, and refused for a private address. A one-connection
/// server on loopback plays the far end.
class OutboundHttpTest {
    /// Answers each connection with `head` and then `bodyChunks` copies of
    /// `chunk`, pausing `pauseMillis` between them; counts connections.
    private static final class Server implements Runnable {
        final ServerSocket socket;
        final String head;
        final byte[] chunk;
        final int bodyChunks;
        final long pauseMillis;
        final AtomicInteger connections = new AtomicInteger();

        Server(String head, byte[] chunk, int bodyChunks, long pauseMillis) throws IOException {
            this.socket = new ServerSocket(0, 5, InetAddress.getByName("127.0.0.1"));
            this.head = head;
            this.chunk = chunk;
            this.bodyChunks = bodyChunks;
            this.pauseMillis = pauseMillis;
            Thread t = new Thread(this, "outbound-http-test");
            t.setDaemon(true);
            t.start();
        }

        String url(String path) {
            return "http://127.0.0.1:" + socket.getLocalPort() + path;
        }

        @Override
        public void run() {
            while (!socket.isClosed()) {
                try (Socket s = socket.accept()) {
                    connections.incrementAndGet();
                    InputStream in = s.getInputStream();
                    // Read the request head.
                    int a = 0;
                    int b = 0;
                    int c = 0;
                    int d;
                    while ((d = in.read()) >= 0) {
                        if (a == '\r' && b == '\n' && c == '\r' && d == '\n') {
                            break;
                        }
                        a = b;
                        b = c;
                        c = d;
                    }
                    OutputStream out = s.getOutputStream();
                    out.write(head.getBytes("UTF-8"));
                    out.flush();
                    for (int iter = 0; iter < bodyChunks; iter++) {
                        if (pauseMillis > 0) {
                            Thread.sleep(pauseMillis);
                        }
                        out.write(chunk);
                        out.flush();
                    }
                } catch (IOException | InterruptedException closed) {
                    // The client went away, or the test is over.
                }
            }
        }

        void close() throws IOException {
            socket.close();
        }
    }

    @Test
    void anEndlessAnswerIsCutOffAtTheCapNotBuffered() throws Exception {
        // About 80 MB on offer, no length given; the cap is 64 KB.
        Server s = new Server("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n"
                + "Connection: close\r\n\r\n", new byte[8192], 10000, 0);
        try {
            IOException e = assertThrows(IOException.class, () -> OutboundHttp.sendUnguarded(
                    "GET", s.url("/big"), new ArrayList<String>(), null, 5000, 64 * 1024));
            assertTrue(e.getMessage().indexOf("longer than") >= 0, e.getMessage());
        } finally {
            s.close();
        }
    }

    @Test
    void anAnswerWithinTheCapArrivesWhole() throws Exception {
        Server s = new Server("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n"
                + "Content-Length: 5\r\nConnection: close\r\n\r\n", "hello".getBytes("UTF-8"),
                1, 0);
        try {
            OutboundHttp.Reply r = OutboundHttp.sendUnguarded("POST", s.url("/ok"),
                    new ArrayList<String>(), "{}".getBytes("UTF-8"), 5000, 64);
            assertEquals(200, r.status);
            assertEquals("hello", r.text());
        } finally {
            s.close();
        }
    }

    @Test
    void aRedirectIsAnsweredAsItIsAndNeverFollowed() throws Exception {
        Server inside = new Server("HTTP/1.1 200 OK\r\nContent-Length: 6\r\n"
                + "Connection: close\r\n\r\n", "secret".getBytes("UTF-8"), 1, 0);
        Server out = new Server("HTTP/1.1 302 Found\r\nLocation: " + inside.url("/meta")
                + "\r\nContent-Length: 0\r\nConnection: close\r\n\r\n", new byte[0], 0, 0);
        try {
            OutboundHttp.Reply r = OutboundHttp.sendUnguarded("GET", out.url("/x"), null, null,
                    5000, 1024);
            assertEquals(302, r.status);
            assertEquals(0, inside.connections.get(), "the redirect's target is never called");
        } finally {
            inside.close();
            out.close();
        }
    }

    @Test
    void aSlowDripPastTheBudgetFails() throws Exception {
        // A byte every 300 ms keeps every single read inside its timeout.
        Server s = new Server("HTTP/1.1 200 OK\r\nConnection: close\r\n\r\n", new byte[1], 100,
                300);
        try {
            long started = System.currentTimeMillis();
            assertThrows(IOException.class, () -> OutboundHttp.sendUnguarded("GET",
                    s.url("/slow"), null, null, 1000, 1 << 20));
            assertTrue(System.currentTimeMillis() - started < 5000);
        } finally {
            s.close();
        }
    }

    @Test
    void aPrivateAddressIsRefusedBeforeAnythingIsSent() throws Exception {
        Server s = new Server("HTTP/1.1 200 OK\r\nContent-Length: 0\r\n\r\n", new byte[0], 0, 0);
        try {
            assertThrows(IOException.class, () -> OutboundHttp.send("GET", s.url("/"), null,
                    null, 1000, 1024));
            assertEquals(0, s.connections.get());
        } finally {
            s.close();
        }
    }
}
