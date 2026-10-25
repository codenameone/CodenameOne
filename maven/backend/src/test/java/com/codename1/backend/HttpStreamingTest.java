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
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Streaming request bodies and multipart parts past the spool threshold, the
/// streamed outbound exchange, and the forwarding proxy -- real sockets, with
/// the default limits (1 MiB to disk, 8 MiB in all).
class HttpStreamingTest {
    private final List backends = new ArrayList();

    @AfterEach
    void stop() {
        for (Object each : backends) {
            ((Backend) each).stop();
        }
    }

    private static final int BIG = 3 * 1024 * 1024 + 17;
    private static final AtomicBoolean WRITER_FAILED = new AtomicBoolean();
    private static final CountDownLatch WRITER_DONE = new CountDownLatch(1);
    private static final AtomicReference SEEN_HEADERS = new AtomicReference();
    private static final java.util.concurrent.atomic.AtomicInteger UNRUN_RAN =
            new java.util.concurrent.atomic.AtomicInteger();
    private static final java.util.concurrent.atomic.AtomicInteger UNRUN_CLOSED =
            new java.util.concurrent.atomic.AtomicInteger();

    /// A writer holding a resource, for the responses the server never writes.
    private static final class Held implements HttpServer.BodyWriter, java.io.Closeable {
        public void writeTo(OutputStream output) throws IOException {
            UNRUN_RAN.incrementAndGet();
            output.write('x');
        }

        public void close() {
            UNRUN_CLOSED.incrementAndGet();
        }
    }

    /// The proxies forward to this same backend (one runs per process): a
    /// request under /proxy goes through `proxy`, everything else is the
    /// upstream's own route.
    private volatile HttpProxy proxy;
    private volatile HttpProxy following;
    private volatile HttpProxy crossing;
    private volatile int selfPort;

    private int upstream() throws Exception {
        selfPort = freePort();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(selfPort));
        backends.add(Backend.builder(Config.of(settings, "test")).quiet().handler(handler())
                .start());
        proxy = new HttpProxy("http://127.0.0.1:" + selfPort);
        following = new HttpProxy("http://127.0.0.1:" + selfPort).setFollowRedirects(3);
        crossing = new HttpProxy("http://127.0.0.1:" + selfPort).allowHost("localhost")
                .setFollowRedirects(3);
        return selfPort;
    }

    private HttpServer.Handler handler() {
        return new HttpServer.Handler() {
            public HttpServer.Response handle(HttpServer.Request request) throws Exception {
                String path = request.getTarget();
                if (path.startsWith("/proxy/elsewhere")) {
                    return proxy.forward(request, "http://example.com/");
                }
                if (path.startsWith("/proxy/cross/")) {
                    return crossing.forward(request, path.substring("/proxy/cross".length()));
                }
                if (path.startsWith("/toself")) {
                    return HttpServer.Response.empty(302, null, null).header("Location", "/echo");
                }
                if (path.startsWith("/tolocal")) {
                    return HttpServer.Response.empty(302, null, null)
                            .header("Location", "http://localhost:" + selfPort + "/echo");
                }
                if (path.startsWith("/proxy/follow")) {
                    return following.forward(request, "/redirect");
                }
                if (path.startsWith("/proxy/raw/")) {
                    return new HttpProxy("http://127.0.0.1:"
                            + path.substring("/proxy/raw/".length())).forward(request, "/x");
                }
                if (path.startsWith("/proxy/")) {
                    return proxy.forward(request, path.substring("/proxy".length()));
                }
                if (path.startsWith("/unrun")) {
                    int status = path.equals("/unrun/204") ? 204 : 200;
                    return HttpServer.Response.stream(status, "text/plain", new Held());
                }
                if (path.startsWith("/endless")) {
                    return HttpServer.Response.stream(200, "application/octet-stream",
                            new HttpServer.BodyWriter() {
                                public void writeTo(OutputStream output) throws IOException {
                                    byte[] block = pattern(64 * 1024);
                                    try {
                                        for (int iter = 0 ; iter < 16384 ; iter++) {
                                            output.write(block);
                                        }
                                    } catch (IOException err) {
                                        WRITER_FAILED.set(true);
                                        throw err;
                                    } finally {
                                        WRITER_DONE.countDown();
                                    }
                                }
                            });
                }
                if (path.startsWith("/sum")) {
                    // Read as a stream, never as an array: the spooled path.
                    CRC32 crc = new CRC32();
                    long n = 0;
                    InputStream in = request.getBodyStream();
                    byte[] buffer = new byte[8192];
                    int r;
                    while ((r = in.read(buffer)) > 0) {
                        crc.update(buffer, 0, r);
                        n += r;
                    }
                    return HttpServer.Response.text(200, n + ":" + crc.getValue() + ":"
                            + request.getBodyLength());
                }
                if (path.startsWith("/parts")) {
                    StringBuilder sb = new StringBuilder();
                    for (Object each : request.getParts()) {
                        HttpServer.Part p = (HttpServer.Part) each;
                        CRC32 crc = new CRC32();
                        InputStream in = p.getInputStream();
                        byte[] buffer = new byte[8192];
                        int r;
                        while ((r = in.read(buffer)) > 0) {
                            crc.update(buffer, 0, r);
                        }
                        sb.append(p.getName()).append('=').append(p.getLength()).append('/')
                                .append(crc.getValue());
                        if (p.getFilename() == null) {
                            sb.append('/').append(p.getText());
                        }
                        sb.append(';');
                    }
                    return HttpServer.Response.text(200, sb.toString());
                }
                if (path.startsWith("/download")) {
                    return HttpServer.Response.stream(200, "application/octet-stream",
                            new HttpServer.BodyWriter() {
                                public void writeTo(OutputStream output) throws IOException {
                                    output.write(pattern(BIG));
                                }
                            });
                }
                if (path.startsWith("/echo")) {
                    final InputStream in = request.getBodyStream();
                    SEEN_HEADERS.set(request.getHeaders());
                    return HttpServer.Response.stream(200, request.getHeader("Content-Type"),
                            new HttpServer.BodyWriter() {
                                public void writeTo(OutputStream output) throws IOException {
                                    byte[] buffer = new byte[8192];
                                    int r;
                                    while ((r = in.read(buffer)) > 0) {
                                        output.write(buffer, 0, r);
                                    }
                                }
                            }).header("Set-Cookie", "a=1");
                }
                if (path.startsWith("/redirect")) {
                    return HttpServer.Response.empty(302, null, null)
                            .header("Location", "/sum");
                }
                return HttpServer.Response.text(404, "no");
            }
        };
    }

    static byte[] pattern(int size) {
        byte[] out = new byte[size];
        for (int iter = 0 ; iter < size ; iter++) {
            out[iter] = (byte) (iter * 31 + (iter >> 9));
        }
        return out;
    }

    static long crc(byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(data, 0, data.length);
        return crc.getValue();
    }

    private static String post(int port, String path, String type, byte[] body, boolean chunked)
            throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL("http://127.0.0.1:" + port + path)
                .openConnection();
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", type);
        if (chunked) {
            c.setChunkedStreamingMode(64 * 1024);
        } else {
            c.setFixedLengthStreamingMode(body.length);
        }
        OutputStream out = c.getOutputStream();
        out.write(body);
        out.close();
        int status = c.getResponseCode();
        InputStream in = status >= 400 ? c.getErrorStream() : c.getInputStream();
        String text = in == null ? "" : new String(readAll(in), StandardCharsets.UTF_8);
        return status + " " + text;
    }

    static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) > 0) {
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
    }

    @Test
    void aLargeUploadIsSpooledAndStreamedToTheHandler() throws Exception {
        int port = upstream();
        byte[] body = pattern(BIG);
        String expected = "200 " + BIG + ":" + crc(body) + ":" + BIG;
        assertEquals(expected, post(port, "/sum", "application/octet-stream", body, false));
        assertEquals(expected, post(port, "/sum", "application/octet-stream", body, true));
    }

    @Test
    void aBinaryBodyPastTheLimitIs413OnBothFramings() throws Exception {
        int port = upstream();
        byte[] body = pattern(9 * 1024 * 1024);
        assertTrue(post(port, "/sum", "application/octet-stream", body, false).startsWith("413"));
        String chunked;
        try {
            chunked = post(port, "/sum", "application/octet-stream", body, true);
        } catch (IOException resetWhileSending) {
            // The server answers 413 and closes as soon as the limit is passed,
            // which a client still writing may see as a reset instead.
            chunked = "413";
        }
        assertTrue(chunked.startsWith("413"), chunked);
    }

    @Test
    void aLargeMultipartFileIsParsedInPlace() throws Exception {
        int port = upstream();
        byte[] file = pattern(BIG);
        ByteArrayOutputStream form = new ByteArrayOutputStream();
        String boundary = "XyZ123";
        form.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"title\"\r\n\r\n"
                + "hello\r\n--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; "
                + "filename=\"a.bin\"\r\nContent-Type: application/octet-stream\r\n\r\n")
                .getBytes(StandardCharsets.US_ASCII));
        form.write(file);
        form.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.US_ASCII));
        String answer = post(port, "/parts", "multipart/form-data; boundary=" + boundary,
                form.toByteArray(), false);
        assertEquals("200 title=5/" + crc("hello".getBytes(StandardCharsets.US_ASCII))
                + "/hello;file=" + BIG + "/" + crc(file) + ";", answer);
    }

    @Test
    void anExchangeStreamsBothWays() throws Exception {
        int port = upstream();
        Web.Exchange download = Web.open("GET", "http://127.0.0.1:" + port + "/download", null,
                Web.NO_BODY);
        try {
            assertEquals(200, download.getStatus());
            assertEquals(crc(pattern(BIG)), crc(readAll(download.getResponseBody())));
        } finally {
            download.close();
        }
        byte[] body = pattern(BIG);
        Web.Exchange upload = Web.open("POST", "http://127.0.0.1:" + port + "/sum",
                listOf("Content-Type: application/octet-stream"), Web.UNKNOWN_LENGTH);
        try {
            OutputStream out = upload.getRequestBody();
            for (int at = 0 ; at < body.length ; at += 100000) {
                out.write(body, at, Math.min(100000, body.length - at));
            }
            out.close();
            assertEquals(200, upload.getStatus());
            assertEquals(BIG + ":" + crc(body) + ":" + BIG,
                    new String(readAll(upload.getResponseBody()), StandardCharsets.UTF_8));
        } finally {
            upload.close();
        }
    }

    @Test
    void aRedirectToAnotherHostDropsTheClientsCredentials() throws Exception {
        int port = upstream();
        String[] hops = {"/toself", "/tolocal"};
        for (int iter = 0; iter < hops.length; iter++) {
            SEEN_HEADERS.set(null);
            HttpURLConnection c = (HttpURLConnection) new URL("http://127.0.0.1:" + port
                    + "/proxy/cross" + hops[iter]).openConnection();
            c.setRequestProperty("Authorization", "Bearer secret");
            c.setRequestProperty("Cookie", "session=1");
            assertEquals(200, c.getResponseCode());
            readAll(c.getInputStream());
            Map seen = (Map) SEEN_HEADERS.get();
            boolean sameHost = iter == 0;
            assertEquals(sameHost, seen.containsKey("authorization"), hops[iter]);
            assertEquals(sameHost, seen.containsKey("cookie"), hops[iter]);
        }
    }

    @Test
    void aGzipUploadIsForwardedDecoded() throws Exception {
        int port = upstream();
        java.io.ByteArrayOutputStream packed = new java.io.ByteArrayOutputStream();
        java.util.zip.GZIPOutputStream gz = new java.util.zip.GZIPOutputStream(packed);
        gz.write("{\"a\":1}".getBytes(StandardCharsets.UTF_8));
        gz.close();
        HttpURLConnection c = (HttpURLConnection) new URL("http://127.0.0.1:" + port
                + "/proxy/echo").openConnection();
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/json");
        c.setRequestProperty("Content-Encoding", "gzip");
        c.getOutputStream().write(packed.toByteArray());
        assertEquals(200, c.getResponseCode());
        assertEquals("{\"a\":1}", new String(readAll(c.getInputStream()), StandardCharsets.UTF_8));
        assertFalse(((Map) SEEN_HEADERS.get()).containsKey("content-encoding"));
    }

    @Test
    void aProxiedHeadKeepsTheUpstreamLength() throws Exception {
        int port = upstream();
        HttpURLConnection c = (HttpURLConnection) new URL("http://127.0.0.1:" + port
                + "/proxy/sum").openConnection();
        c.setRequestMethod("HEAD");
        assertEquals(200, c.getResponseCode());
        assertEquals("5", c.getHeaderField("Content-Length"));
        c.disconnect();
        c = (HttpURLConnection) new URL("http://127.0.0.1:" + port + "/proxy/download")
                .openConnection();
        c.setRequestMethod("HEAD");
        assertEquals(200, c.getResponseCode());
        assertNull(c.getHeaderField("Content-Length"), "a length nobody gave was invented");
    }

    @Test
    void aWriterTheServerNeverRunsIsClosed() throws Exception {
        int port = upstream();
        UNRUN_RAN.set(0);
        UNRUN_CLOSED.set(0);
        java.net.HttpURLConnection c = (java.net.HttpURLConnection)
                new java.net.URL("http://127.0.0.1:" + port + "/unrun/204").openConnection();
        assertEquals(204, c.getResponseCode());
        c.disconnect();
        c = (java.net.HttpURLConnection)
                new java.net.URL("http://127.0.0.1:" + port + "/unrun/head").openConnection();
        c.setRequestMethod("HEAD");
        assertEquals(200, c.getResponseCode());
        c.disconnect();
        assertEquals(0, UNRUN_RAN.get(), "a bodiless response ran its writer");
        assertEquals(2, UNRUN_CLOSED.get(), "a writer that never ran was not closed");
    }

    @Test
    void closingAnExchangeEarlyCancelsTheProducer() throws Exception {
        int port = upstream();
        Web.Exchange download = Web.open("GET", "http://127.0.0.1:" + port + "/endless", null,
                Web.NO_BODY);
        assertEquals(200, download.getStatus());
        byte[] some = new byte[1024];
        assertTrue(download.getResponseBody().read(some) > 0);
        download.close();
        assertTrue(WRITER_DONE.await(20, TimeUnit.SECONDS));
        assertTrue(WRITER_FAILED.get(), "the upstream writer never saw the cancellation");
    }

    @Test
    void theProxyForwardsBodiesAndEndToEndHeaders() throws Exception {
        int port = upstream();
        byte[] body = pattern(BIG);
        HttpURLConnection c = (HttpURLConnection) new URL("http://127.0.0.1:" + port + "/proxy/echo?x=1")
                .openConnection();
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/octet-stream");
        c.setRequestProperty("X-Keep", "kept");
        c.setFixedLengthStreamingMode(body.length);
        OutputStream out = c.getOutputStream();
        out.write(body);
        out.close();
        assertEquals(200, c.getResponseCode());
        assertEquals(crc(body), crc(readAll(c.getInputStream())));
        assertEquals("a=1", c.getHeaderField("Set-Cookie"));
        Map seen = (Map) SEEN_HEADERS.get();
        assertEquals("kept", seen.get("x-keep"));
        // HttpURLConnection replaces a Connection header with its own, so the
        // hop-by-hop half goes over a raw socket.
        Socket s = new Socket("127.0.0.1", port);
        try {
            s.setSoTimeout(20000);
            s.getOutputStream().write(("GET /proxy/echo HTTP/1.1\r\nHost: x\r\n"
                    + "Connection: close, X-Drop\r\nX-Drop: secret\r\nTE: trailers\r\n"
                    + "X-Keep: again\r\n\r\n").getBytes("US-ASCII"));
            String reply = new String(readAll(s.getInputStream()), "ISO-8859-1");
            assertTrue(reply.startsWith("HTTP/1.1 200"), reply);
        } finally {
            s.close();
        }
        seen = (Map) SEEN_HEADERS.get();
        assertEquals("again", seen.get("x-keep"));
        assertNull(seen.get("x-drop"), "a header Connection named was forwarded");
        assertNull(seen.get("te"), "a hop-by-hop header was forwarded");
        assertTrue(String.valueOf(seen.get("x-forwarded-for")).contains("127.0.0.1"));
        assertEquals("http", seen.get("x-forwarded-proto"));
    }

    @Test
    void theProxyFiltersUpstreamHopByHopAndKeepsRepeatedHeaders() throws Exception {
        final ServerSocket raw = new ServerSocket(0);
        Thread server = new Thread(new Runnable() {
            public void run() {
                try {
                    Socket s = raw.accept();
                    InputStream in = s.getInputStream();
                    int state = 0;
                    while (state < 4) {
                        int b = in.read();
                        if (b < 0) {
                            break;
                        }
                        state = (b == '\r' || b == '\n') ? state + 1 : 0;
                    }
                    byte[] payload = {0, 1, 2, (byte) 0xff, 13, 10};
                    OutputStream o = s.getOutputStream();
                    o.write(("HTTP/1.1 200 OK\r\nContent-Type: application/x-thing\r\n"
                            + "Set-Cookie: a=1\r\nSet-Cookie: b=2\r\nConnection: close, X-Hop\r\n"
                            + "X-Hop: gone\r\nKeep-Alive: timeout=5\r\nX-End: here\r\n"
                            + "Content-Length: " + payload.length + "\r\n\r\n")
                            .getBytes(StandardCharsets.US_ASCII));
                    o.write(payload);
                    o.flush();
                    s.close();
                } catch (IOException ignored) {
                    // the test fails on the client side
                }
            }
        });
        server.setDaemon(true);
        server.start();
        try {
            int port = upstream();
            HttpURLConnection c = (HttpURLConnection) new URL("http://127.0.0.1:" + port
                    + "/proxy/raw/" + raw.getLocalPort()).openConnection();
            assertEquals(200, c.getResponseCode());
            byte[] got = readAll(c.getInputStream());
            assertEquals(6, got.length);
            assertEquals((byte) 0xff, got[3]);
            assertEquals("application/x-thing", c.getContentType());
            List cookies = null;
            for (Map.Entry<String, List<String>> e : c.getHeaderFields().entrySet()) {
                if ("set-cookie".equalsIgnoreCase(e.getKey())) {
                    cookies = e.getValue();
                }
            }
            assertEquals(2, cookies.size(), String.valueOf(cookies));
            assertEquals("here", c.getHeaderField("X-End"));
            assertNull(c.getHeaderField("X-Hop"));
            assertNull(c.getHeaderField("Keep-Alive"));
        } finally {
            raw.close();
        }
    }

    @Test
    void theProxyRefusesOtherDestinationsAndPassesRedirectsThrough() throws Exception {
        int port = upstream();
        HttpURLConnection c = (HttpURLConnection) new URL("http://127.0.0.1:" + port
                + "/proxy/elsewhere").openConnection();
        assertEquals(502, c.getResponseCode());
        c = (HttpURLConnection) new URL("http://127.0.0.1:" + port + "/proxy/redirect")
                .openConnection();
        c.setInstanceFollowRedirects(false);
        assertEquals(302, c.getResponseCode());
        assertEquals("/sum", c.getHeaderField("Location"));
        c = (HttpURLConnection) new URL("http://127.0.0.1:" + port + "/proxy/follow")
                .openConnection();
        assertEquals(200, c.getResponseCode());
        assertEquals("0:0:0", new String(readAll(c.getInputStream()), StandardCharsets.UTF_8));
        assertFalse(HttpProxy.isHopByHop("x-end"));
        assertNull(HttpProxy.hostOf("http://user@evil/"));
    }

    private static List listOf(String line) {
        List out = new ArrayList();
        out.add(line);
        return out;
    }

    private static int freePort() throws IOException {
        ServerSocket s = new ServerSocket(0);
        try {
            return s.getLocalPort();
        } finally {
            s.close();
        }
    }
}
