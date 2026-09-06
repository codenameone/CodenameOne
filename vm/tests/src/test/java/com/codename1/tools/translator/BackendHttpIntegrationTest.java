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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Drives the translated server-side binary over a real socket.
 *
 * The functional half could be written against curl; the protocol half cannot.
 * Pipelining, a request that carries both Content-Length and Transfer-Encoding,
 * an obsolete folded header -- no ordinary client will send any of those, and they
 * are exactly the inputs a server has to get right. So every assertion here goes
 * through a raw socket and reads the bytes back.
 *
 * The suite builds vm/backend once and runs one server for the class. It SKIPS
 * rather than fails when the toolchain is missing (no JDK 8, no clang, Windows),
 * because a machine that cannot build the binary has nothing to say about whether
 * the binary is correct.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BackendHttpIntegrationTest {

    private static Process server;
    private static int port;
    private static Path work;
    private static String skipReason;

    @BeforeAll
    void startServer() throws Exception {
        if (CompilerHelper.isWindows()) {
            skipReason = "the server-side backend is POSIX-only for now";
            BackendTestSupport.skipOrFail(skipReason);
        }
        Path backend = Paths.get("..", "backend").normalize().toAbsolutePath();
        BackendTestSupport.require(Files.isDirectory(backend), "vm/backend is not present");

        Path jdk8 = findJdk8();
        BackendTestSupport.require(jdk8 != null, "no JDK 8 available to compile the backend");

        work = Files.createTempDirectory("backend-http-test");
        Path binary = work.resolve("petserver");
        Path staticRoot = Files.createDirectories(work.resolve("www"));
        Files.write(staticRoot.resolve("index.html"),
                "<!doctype html><h1>index</h1>".getBytes(StandardCharsets.UTF_8));
        byte[] blob = new byte[256 * 1024];
        for (int i = 0; i < blob.length; i++) {
            blob[i] = (byte) (i & 0xff);
        }
        Files.write(staticRoot.resolve("big.bin"), blob);

        // The real build script, not a reimplementation of it: a test that builds
        // differently from the product is testing something else.
        ProcessBuilder build = new ProcessBuilder("./build.sh", "PetServer", "com.demo",
                binary.toString());
        build.directory(backend.toFile());
        build.environment().put("JDK_8_HOME", jdk8.toString());
        build.environment().put("JAVA_HOME", jdk8.toString());
        build.environment().put("CN1_BACKEND_DEMO", "demo/petserver");
        build.redirectErrorStream(true);
        Process p = build.start();
        String buildLog = readFully(p.getInputStream());
        boolean built = p.waitFor(20, TimeUnit.MINUTES) && p.exitValue() == 0
                && Files.isExecutable(binary);
        if (!built) {
            String tail = buildLog.length() > 3000
                    ? buildLog.substring(buildLog.length() - 3000) : buildLog;
            BackendTestSupport.skipOrFail("could not build the backend binary:\n" + tail);
        }

        port = freePort();
        ProcessBuilder run = new ProcessBuilder(binary.toString());
        run.environment().put("CN1_PORT", String.valueOf(port));
        run.environment().put("CN1_DB_PATH", work.resolve("test.db").toString());
        run.environment().put("CN1_STATIC_ROOT", staticRoot.toString());
        run.environment().put("CN1_HTTP_TIMEOUT_MS", "4000");
        run.redirectErrorStream(true);
        run.redirectOutput(work.resolve("server.log").toFile());
        server = run.start();
        assertTrue(waitForPort(port, 30000), "the server never accepted a connection");
    }

    @AfterAll
    void stopServer() {
        if (server != null) {
            server.destroy();
            try {
                if (!server.waitFor(10, TimeUnit.SECONDS)) {
                    server.destroyForcibly();
                }
            } catch (InterruptedException err) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // ------------------------------------------------------------------
    // Functional
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a DTO round-trips through the generated dispatcher and SQLite")
    void dtoRoundTrip() throws Exception {
        String created = body(request("POST", "/pet",
                "{\"name\":\"Fido\",\"species\":\"dog\",\"weight\":12.5,\"good\":true}", null));
        assertTrue(created.contains("\"name\":\"Fido\""), created);
        assertTrue(created.contains("\"weight\":12.5"), "a double must survive the round trip: " + created);
        assertTrue(created.contains("\"good\":true"), "a boolean must survive the round trip: " + created);

        String listed = body(request("GET", "/pets", null, null));
        assertTrue(listed.startsWith("["), listed);
        assertTrue(listed.contains("Fido"), listed);
    }

    @Test
    @DisplayName("headers and cookies bind from the request")
    void headersAndCookiesBind() throws Exception {
        String response = body(request("GET", "/whoami", null,
                new String[]{"X-User: shai", "Cookie: theme=dark; session=abc123"}));
        assertTrue(response.contains("user=shai"), response);
        assertTrue(response.contains("session=abc123"), response);
    }

    @Test
    @DisplayName("a protected route needs a valid bearer token")
    void authGuardsMutatingRoutes() throws Exception {
        assertEquals(401, status(request("DELETE", "/pet/9999", null, null)));

        String token = body(request("POST", "/login",
                "{\"username\":\"shai\",\"password\":\"hunter2\"}", null)).replace("\"", "");
        assertTrue(token.split("\\.").length == 3, "expected a three-part JWT, got: " + token);

        assertEquals(401, status(request("POST", "/login",
                "{\"username\":\"shai\",\"password\":\"wrong\"}", null)));
        // An unknown user and a wrong password must be indistinguishable, or the
        // login endpoint becomes a list of valid usernames.
        assertEquals(401, status(request("POST", "/login",
                "{\"username\":\"nobody\",\"password\":\"hunter2\"}", null)));

        String created = body(request("POST", "/pet", "{\"name\":\"Doomed\"}", null));
        // Asserted rather than substring'd straight away: indexOf returning -1 here
        // throws StringIndexOutOfBounds and takes the response with it, which is
        // how an intermittent malformed body was reported for a while as nothing
        // more than "String index out of range: -1".
        //
        // KNOWN OPEN DEFECT, and this assertion is what finally named it: the
        // server occasionally answers a request with NOTHING. The body captured
        // here came back empty, and transactionRollsBack fails the same way from
        // the other side -- status -1 after exactly 15.05s, which is this class's
        // own setSoTimeout(15000) expiring with no reply. Measured on the
        // dispatching path at 2 failures in 4 full-suite runs (about 100 requests
        // each), and it predates the virtual-thread work: a build with none of it
        // fails identically. It does NOT reproduce in isolation -- 400 plain
        // POSTs and 120 replays of this test's exact request sequence were both
        // clean -- so the trigger is interaction with the other tests against the
        // shared server, not this request. Not a flake to be re-run: a request
        // that goes unanswered is a server bug and this is where it surfaced.
        assertTrue(created.indexOf(":") >= 0 && created.indexOf(",") >= 0,
                "POST /pet returned a body that is not the expected JSON: [" + created + "]");
        String id = created.substring(created.indexOf(":") + 1, created.indexOf(","));
        assertEquals(200, status(request("DELETE", "/pet/" + id, null,
                new String[]{"Authorization: Bearer " + token})));

        String tampered = token.substring(0, token.length() - 1) + "X";
        assertEquals(401, status(request("DELETE", "/pet/1", null,
                new String[]{"Authorization: Bearer " + tampered})));
    }

    @Test
    @DisplayName("a failed batch rolls back every row it had already written")
    void transactionRollsBack() throws Exception {
        String token = body(request("POST", "/login",
                "{\"username\":\"shai\",\"password\":\"hunter2\"}", null)).replace("\"", "");
        int before = countPets();
        assertEquals(400, status(request("POST", "/pets/bulk",
                "[{\"name\":\"Rollback1\"},{\"species\":\"nameless\"}]",
                new String[]{"Authorization: Bearer " + token})));
        assertEquals(before, countPets(),
                "the row written before the failure must not survive");
    }

    @Test
    @DisplayName("a chunked body is decoded")
    void chunkedUpload() throws Exception {
        String payload = "{\"name\":\"Chunky\",\"species\":\"cat\"}";
        StringBuilder chunks = new StringBuilder();
        for (int i = 0; i < payload.length(); i += 7) {
            String part = payload.substring(i, Math.min(i + 7, payload.length()));
            chunks.append(Integer.toHexString(part.length())).append("\r\n").append(part).append("\r\n");
        }
        chunks.append("0\r\n\r\n");
        byte[] response = raw("POST /pet HTTP/1.1\r\nHost: x\r\nTransfer-Encoding: chunked\r\n"
                + "Connection: close\r\n\r\n" + chunks);
        assertEquals(200, statusOf(response), new String(response, StandardCharsets.UTF_8));
        assertTrue(new String(response, StandardCharsets.UTF_8).contains("Chunky"));
    }

    @Test
    @DisplayName("metrics report what the server is doing")
    void metricsEndpoint() throws Exception {
        String health = body(request("GET", "/healthz", null, null));
        assertTrue(health.contains("\"status\":\"ok\""), health);
        assertTrue(health.contains("\"requestsServed\""), health);
        assertTrue(health.contains("\"activeRequests\""), health);
    }

    // ------------------------------------------------------------------
    // Static files
    // ------------------------------------------------------------------

    @Test
    @DisplayName("static files serve, 404, and refuse to leave the document root")
    void staticFileBasics() throws Exception {
        assertTrue(body(request("GET", "/static/index.html", null, null)).contains("<h1>index</h1>"));
        assertEquals(404, status(request("GET", "/static/missing.html", null, null)));
        // Percent-encoded traversal: a check on the raw request string misses this.
        int traversal = status(request("GET", "/static/..%2f..%2fetc%2fpasswd", null, null));
        assertTrue(traversal == 403 || traversal == 404,
                "a traversal must not be served, got " + traversal);
    }

    @Test
    @DisplayName("conditional requests answer 304")
    void conditionalGet() throws Exception {
        byte[] first = request("GET", "/static/index.html", null, null);
        String etag = header(first, "ETag");
        assertNotNull(etag, "a static response must carry an ETag");
        assertEquals(304, status(request("GET", "/static/index.html", null,
                new String[]{"If-None-Match: " + etag})));

        String lastModified = header(first, "Last-Modified");
        assertNotNull(lastModified, "a static response must carry Last-Modified");
        assertEquals(304, status(request("GET", "/static/index.html", null,
                new String[]{"If-Modified-Since: " + lastModified})));
    }

    @Test
    @DisplayName("ranges are honoured and an impossible one is refused")
    void rangeRequests() throws Exception {
        byte[] partial = request("GET", "/static/big.bin", null,
                new String[]{"Range: bytes=0-99"});
        assertEquals(206, statusOf(partial));
        assertEquals("bytes 0-99/262144", header(partial, "Content-Range"));
        assertEquals("100", header(partial, "Content-Length"));

        assertEquals(416, status(request("GET", "/static/big.bin", null,
                new String[]{"Range: bytes=999999999-"})));
    }

    @Test
    @DisplayName("HEAD reports the length a GET would send, not zero")
    void headReportsRealLength() throws Exception {
        byte[] head = request("HEAD", "/static/big.bin", null, null);
        assertEquals(200, statusOf(head));
        assertEquals("262144", header(head, "Content-Length"),
                "a HEAD that reports 0 tells the client the resource is empty");
        assertEquals(0, bodyBytes(head).length, "a HEAD response must carry no body");
    }

    // ------------------------------------------------------------------
    // Protocol correctness. No ordinary client sends any of this, which is
    // exactly why a server has to get it right.
    // ------------------------------------------------------------------

    @Test
    @DisplayName("two pipelined requests both get answered")
    void pipelinedRequestsAreNotLost() throws Exception {
        byte[] response = raw("GET /healthz HTTP/1.1\r\nHost: x\r\n\r\n"
                + "GET /healthz HTTP/1.1\r\nHost: x\r\nConnection: close\r\n\r\n");
        int responses = countOccurrences(new String(response, StandardCharsets.UTF_8), "HTTP/1.1 ");
        assertEquals(2, responses,
                "a client may send a second request before reading the first reply");
    }

    @Test
    @DisplayName("Content-Length together with Transfer-Encoding is refused")
    void refusesConflictingFraming() throws Exception {
        // Two framings in one request is how a request is smuggled past a proxy
        // that believes one of them and a server that believes the other.
        byte[] response = raw("POST /pet HTTP/1.1\r\nHost: x\r\nContent-Length: 6\r\n"
                + "Transfer-Encoding: chunked\r\nConnection: close\r\n\r\n0\r\n\r\n");
        assertEquals(400, statusOf(response));
    }

    @Test
    @DisplayName("two different Content-Length values are refused")
    void refusesDuplicateContentLength() throws Exception {
        byte[] response = raw("POST /pet HTTP/1.1\r\nHost: x\r\nContent-Length: 5\r\n"
                + "Content-Length: 6\r\nConnection: close\r\n\r\nhello");
        assertEquals(400, statusOf(response),
                "disagreeing lengths must be refused, not guessed at");
    }

    @Test
    @DisplayName("an HTTP/1.1 request without Host is refused")
    void requiresHostHeader() throws Exception {
        byte[] response = raw("GET /healthz HTTP/1.1\r\nConnection: close\r\n\r\n");
        assertEquals(400, statusOf(response));
    }

    @Test
    @DisplayName("an obsolete folded header is refused")
    void refusesObsoleteLineFolding() throws Exception {
        // Folding is how two parsers are made to disagree about where a header
        // ends; RFC 9112 says a server must reject it.
        byte[] response = raw("GET /healthz HTTP/1.1\r\nHost: x\r\nX-Fold: a\r\n  b\r\n"
                + "Connection: close\r\n\r\n");
        assertEquals(400, statusOf(response));
    }

    @Test
    @DisplayName("HTTP/1.0 closes unless the client asks to keep alive")
    void httpTenClosesByDefault() throws Exception {
        byte[] response = raw("GET /healthz HTTP/1.0\r\n\r\n");
        assertEquals(200, statusOf(response));
        String connection = header(response, "Connection");
        assertTrue(connection == null || "close".equalsIgnoreCase(connection),
                "HTTP/1.0 defaults to close, got Connection: " + connection);
    }

    @Test
    @DisplayName("Expect: 100-continue gets an interim response")
    void honoursExpectContinue() throws Exception {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress("127.0.0.1", port), 5000);
        socket.setSoTimeout(3000);
        try {
            OutputStream out = socket.getOutputStream();
            String payload = "{\"name\":\"Expectant\"}";
            out.write(("POST /pet HTTP/1.1\r\nHost: x\r\nContent-Length: " + payload.length()
                    + "\r\nExpect: 100-continue\r\nConnection: close\r\n\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            out.flush();
            // The client is entitled to wait here. A server that never answers
            // makes every such client pay its whole timeout before sending.
            byte[] interim = new byte[64];
            int n = socket.getInputStream().read(interim);
            String head = new String(interim, 0, Math.max(n, 0), StandardCharsets.UTF_8);
            assertTrue(head.startsWith("HTTP/1.1 100"),
                    "expected an interim 100 Continue, got: " + head.trim());
        } finally {
            socket.close();
        }
    }

    @Test
    @DisplayName("an unknown method is refused rather than routed")
    void refusesUnknownMethod() throws Exception {
        // Methods are case-sensitive, so "get" is not GET.
        byte[] response = raw("get /healthz HTTP/1.1\r\nHost: x\r\nConnection: close\r\n\r\n");
        int code = statusOf(response);
        assertTrue(code == 400 || code == 501,
                "a method that is not a known verb must be refused, got " + code);
    }

    @Test
    @DisplayName("a silent client is shed and does not hold a worker")
    void shedsIdleConnections() throws Exception {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress("127.0.0.1", port), 5000);
        socket.setSoTimeout(15000);
        try {
            // Enough to be handed to a worker, never enough to be a request.
            socket.getOutputStream().write("GET /healthz HTTP/1.1\r\nHost: x\r\n"
                    .getBytes(StandardCharsets.UTF_8));
            socket.getOutputStream().flush();
            long started = System.currentTimeMillis();
            int first = socket.getInputStream().read();
            long elapsed = System.currentTimeMillis() - started;
            assertEquals(-1, first, "the connection should be closed, not answered");
            assertTrue(elapsed < 12000, "the deadline should have shed it, took " + elapsed + "ms");
        } finally {
            socket.close();
        }
        // And the server is still healthy afterwards.
        assertEquals(200, status(request("GET", "/healthz", null, null)));
    }

    // ------------------------------------------------------------------
    // HTTP/2
    // ------------------------------------------------------------------

    @Test
    @DisplayName("cleartext HTTP/2 by prior knowledge serves a request")
    void http2CleartextRequest() throws Exception {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress("127.0.0.1", port), 5000);
        socket.setSoTimeout(10000);
        try {
            OutputStream out = socket.getOutputStream();
            out.write("PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            out.write(frame(4, 0, 0, new byte[0]));          // empty SETTINGS
            ByteArrayOutputStream block = new ByteArrayOutputStream();
            hpackLiteral(block, ":method", "GET");
            hpackLiteral(block, ":path", "/healthz");
            hpackLiteral(block, ":scheme", "http");
            hpackLiteral(block, ":authority", "127.0.0.1");
            // END_STREAM | END_HEADERS: a GET with no body is complete at once.
            out.write(frame(1, 0x05, 1, block.toByteArray()));
            out.flush();

            boolean sawHeaders = false;
            boolean sawData = false;
            String data = "";
            long deadline = System.currentTimeMillis() + 8000;
            InputStream in = socket.getInputStream();
            while (System.currentTimeMillis() < deadline && !(sawHeaders && sawData)) {
                byte[] header = readExactly(in, 9);
                if (header == null) {
                    break;
                }
                int length = ((header[0] & 0xff) << 16) | ((header[1] & 0xff) << 8) | (header[2] & 0xff);
                int type = header[3] & 0xff;
                byte[] payload = length == 0 ? new byte[0] : readExactly(in, length);
                if (payload == null) {
                    break;
                }
                if (type == 1) {
                    sawHeaders = true;
                    // The status is HPACK-encoded; 200 is static-table index 8,
                    // which nghttp2 emits as the single byte 0x88.
                    assertTrue(payload.length > 0, "an empty HEADERS payload is not a response");
                    assertEquals((byte) 0x88, payload[0],
                            "expected an indexed :status 200 as the first header");
                } else if (type == 0) {
                    sawData = true;
                    data = new String(payload, StandardCharsets.UTF_8);
                } else if (type == 7) {
                    fail("the server sent GOAWAY: " + new String(payload, StandardCharsets.UTF_8));
                }
            }
            assertTrue(sawHeaders, "no HEADERS frame came back");
            assertTrue(sawData, "no DATA frame came back");
            assertTrue(data.contains("\"status\":\"ok\""), data);
        } finally {
            socket.close();
        }
    }

    @Test
    @DisplayName("an HTTP/1.1 request still works on the same port as h2c")
    void httpOneStillWorksAlongsideHttp2() throws Exception {
        // The preface detector must not swallow ordinary requests: "GET" diverges
        // from "PRI" at the second byte and has to fall straight through.
        assertEquals(200, status(request("GET", "/healthz", null, null)));
    }

    /** An HTTP/2 frame: 3-byte length, type, flags, 4-byte stream id, payload. */
    private static byte[] frame(int type, int flags, int streamId, byte[] payload) {
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

    /**
     * One HPACK "literal header field without indexing, new name", uncompressed.
     * Writing a full HPACK encoder into a test would be testing the test; this is
     * the one form every decoder must accept.
     */
    private static void hpackLiteral(ByteArrayOutputStream out, String name, String value) {
        byte[] n = name.getBytes(StandardCharsets.UTF_8);
        byte[] v = value.getBytes(StandardCharsets.UTF_8);
        out.write(0x00);
        out.write(n.length);   // H=0, length < 127 for every name used here
        out.write(n, 0, n.length);
        out.write(v.length);
        out.write(v, 0, v.length);
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

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private int countPets() throws Exception {
        String listed = body(request("GET", "/pets", null, null));
        return countOccurrences(listed, "\"id\":");
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        int at = 0;
        while ((at = haystack.indexOf(needle, at)) >= 0) {
            count++;
            at += needle.length();
        }
        return count;
    }

    /** One request on its own connection, returning the whole raw response. */
    private byte[] request(String method, String target, String body, String[] extraHeaders)
            throws IOException {
        StringBuilder head = new StringBuilder();
        head.append(method).append(' ').append(target).append(" HTTP/1.1\r\n");
        head.append("Host: 127.0.0.1\r\n");
        if (extraHeaders != null) {
            for (String h : extraHeaders) {
                head.append(h).append("\r\n");
            }
        }
        byte[] payload = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
        head.append("Content-Length: ").append(payload.length).append("\r\n");
        head.append("Connection: close\r\n\r\n");
        return raw(head.toString(), payload);
    }

    private byte[] raw(String head) throws IOException {
        return raw(head, new byte[0]);
    }

    private byte[] raw(String head, byte[] body) throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress("127.0.0.1", port), 5000);
        socket.setSoTimeout(15000);
        try {
            OutputStream out = socket.getOutputStream();
            out.write(head.getBytes(StandardCharsets.UTF_8));
            if (body.length > 0) {
                out.write(body);
            }
            out.flush();
            return readFullyBytes(socket.getInputStream());
        } finally {
            socket.close();
        }
    }

    private static int status(byte[] response) {
        return statusOf(response);
    }

    private static int statusOf(byte[] response) {
        String text = new String(response, StandardCharsets.UTF_8);
        int firstSpace = text.indexOf(' ');
        if (firstSpace < 0) {
            return -1;
        }
        int secondSpace = text.indexOf(' ', firstSpace + 1);
        try {
            return Integer.parseInt(text.substring(firstSpace + 1,
                    secondSpace < 0 ? text.length() : secondSpace).trim());
        } catch (NumberFormatException err) {
            return -1;
        }
    }

    private static String header(byte[] response, String name) {
        String text = new String(response, StandardCharsets.UTF_8);
        int end = text.indexOf("\r\n\r\n");
        String head = end < 0 ? text : text.substring(0, end);
        for (String line : head.split("\r\n")) {
            int colon = line.indexOf(':');
            if (colon > 0 && line.substring(0, colon).trim().equalsIgnoreCase(name)) {
                return line.substring(colon + 1).trim();
            }
        }
        return null;
    }

    private static String body(byte[] response) {
        return new String(bodyBytes(response), StandardCharsets.UTF_8).trim();
    }

    private static byte[] bodyBytes(byte[] response) {
        String text = new String(response, StandardCharsets.UTF_8);
        int end = text.indexOf("\r\n\r\n");
        if (end < 0) {
            return new byte[0];
        }
        int start = end + 4;
        byte[] out = new byte[response.length - start];
        System.arraycopy(response, start, out, 0, out.length);
        return out;
    }

    private static byte[] readFullyBytes(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        try {
            while ((n = in.read(buffer)) > 0) {
                out.write(buffer, 0, n);
            }
        } catch (IOException err) {
            // A read timeout means the peer said nothing more; whatever arrived is
            // the response.
        }
        return out.toByteArray();
    }

    private static String readFully(InputStream in) throws IOException {
        return new String(readFullyBytes(in), StandardCharsets.UTF_8);
    }

    private static int freePort() throws IOException {
        ServerSocket probe = new ServerSocket(0);
        try {
            return probe.getLocalPort();
        } finally {
            probe.close();
        }
    }

    private static boolean waitForPort(int port, long timeoutMillis) {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            Socket socket = new Socket();
            try {
                socket.connect(new InetSocketAddress("127.0.0.1", port), 500);
                return true;
            } catch (IOException err) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            } finally {
                try {
                    socket.close();
                } catch (IOException ignored) {
                    // closing a probe socket that never connected
                }
            }
        }
        return false;
    }

    private static Path findJdk8() {
        String env = System.getenv("JDK_8_HOME");
        if (env != null && Files.isExecutable(Paths.get(env, "bin", "javac"))) {
            return Paths.get(env);
        }
        List<Path> candidates = new ArrayList<Path>();
        String home = System.getProperty("user.home");
        candidates.add(Paths.get("/Library/Java/JavaVirtualMachines"));
        candidates.add(Paths.get(home, "Library", "Java", "JavaVirtualMachines"));
        candidates.add(Paths.get("/usr/lib/jvm"));
        for (Path root : candidates) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            try {
                java.util.Iterator<Path> it = Files.list(root).iterator();
                while (it.hasNext()) {
                    Path entry = it.next();
                    String name = entry.getFileName().toString().toLowerCase();
                    if (name.indexOf("1.8") < 0 && name.indexOf("-8") < 0 && name.indexOf("jdk8") < 0) {
                        continue;
                    }
                    Path javac = entry.resolve("Contents/Home/bin/javac");
                    if (Files.isExecutable(javac)) {
                        return entry.resolve("Contents/Home");
                    }
                    javac = entry.resolve("bin/javac");
                    if (Files.isExecutable(javac)) {
                        return entry;
                    }
                }
            } catch (IOException err) {
                // unreadable directory; try the next candidate
            }
        }
        return null;
    }
}
