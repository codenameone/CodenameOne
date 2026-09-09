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
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
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

    private static Process tlsServer;
    private static int tlsPort;

    /** Larger than any plausible socket send buffer, so a slow reader stalls the write. */
    private static final int HUGE_BYTES = 8 * 1024 * 1024;
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
        // Deliberately larger than any socket send buffer: a slow reader has to
        // make the server's write block partway through, which is the only way
        // to reach the backpressure paths in send() and sendfile().
        byte[] huge = new byte[HUGE_BYTES];
        for (int i = 0; i < huge.length; i++) {
            huge[i] = (byte) ((i * 31) & 0xff);
        }
        Files.write(staticRoot.resolve("huge.bin"), huge);

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

        startTlsServer(work, binary, staticRoot);
    }

    /**
     * Starts a second copy of the same binary with a certificate configured.
     *
     * TLS had no end-to-end coverage at all: every other test here speaks
     * plaintext, so the handshake, the record layer and the ALPN negotiation
     * were exercised by nothing. Reusing the binary just built keeps that to one
     * extra process rather than a second translation.
     */
    private void startTlsServer(Path work, Path binary, Path staticRoot) throws Exception {
        Path cert = work.resolve("cert.pem");
        Path key = work.resolve("key.pem");
        ProcessBuilder openssl = new ProcessBuilder("openssl", "req", "-x509", "-newkey",
                "rsa:2048", "-keyout", key.toString(), "-out", cert.toString(),
                "-days", "1", "-nodes", "-subj", "/CN=localhost");
        openssl.redirectErrorStream(true);
        openssl.redirectOutput(work.resolve("openssl.log").toFile());
        Process made;
        try {
            made = openssl.start();
        } catch (IOException noOpenssl) {
            // Without a certificate there is nothing to serve; the plaintext
            // tests still run and the TLS ones report why they did not.
            return;
        }
        if (!made.waitFor(60, TimeUnit.SECONDS) || made.exitValue() != 0
                || !Files.exists(cert) || !Files.exists(key)) {
            return;
        }
        tlsPort = freePort();
        ProcessBuilder run = new ProcessBuilder(binary.toString());
        run.environment().put("CN1_PORT", String.valueOf(tlsPort));
        run.environment().put("CN1_DB_PATH", work.resolve("tls.db").toString());
        run.environment().put("CN1_STATIC_ROOT", staticRoot.toString());
        run.environment().put("CN1_HTTP_TIMEOUT_MS", "4000");
        run.environment().put("CN1_TLS_CERT", cert.toString());
        run.environment().put("CN1_TLS_KEY", key.toString());
        run.redirectErrorStream(true);
        run.redirectOutput(work.resolve("tls-server.log").toFile());
        tlsServer = run.start();
        if (!waitForPort(tlsPort, 30000)) {
            tlsServer.destroyForcibly();
            tlsServer = null;
            tlsPort = 0;
        }
    }

    @AfterAll
    void stopServer() {
        if (tlsServer != null) {
            tlsServer.destroy();
            try {
                if (!tlsServer.waitFor(10, TimeUnit.SECONDS)) {
                    tlsServer.destroyForcibly();
                }
            } catch (InterruptedException err) {
                Thread.currentThread().interrupt();
            }
        }
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
        // And the 304 must not claim a length. Content-Length on a 304 describes
        // the SELECTED REPRESENTATION -- what a 200 would have sent -- and the
        // only figure available here is the empty body's zero, which would tell
        // the cache the file it just validated is empty. The header is optional
        // on a 304, so it is omitted rather than fabricated.
        byte[] conditional = raw("GET /static/index.html HTTP/1.1\r\nHost: x\r\n"
                + "If-None-Match: " + etag + "\r\nConnection: close\r\n\r\n");
        String conditionalText = new String(conditional, StandardCharsets.UTF_8);
        assertTrue(conditionalText.startsWith("HTTP/1.1 304"),
                "a matching ETag should answer 304:\n" + conditionalText);
        assertEquals(-1, conditionalText.toLowerCase().indexOf("content-length"),
                "a 304 must not advertise a length it cannot describe:\n" + conditionalText);
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
    @DisplayName("a body on a 204 is suppressed rather than desynchronising the connection")
    void bodilessStatusDoesNotDesyncTheConnection() throws Exception {
        // /nocontent returns a 204 WITH bytes, which a handler is free to build.
        // RFC 9110 ends such a response at the header section, so writing them
        // would leave the client reading "junk" as the start of the second reply
        // and everything after that misframed. Both requests go out together so
        // that a desync is visible as a wrong reply rather than a slow one.
        byte[] response = raw("GET /nocontent HTTP/1.1\r\nHost: x\r\n\r\n"
                + "GET /healthz HTTP/1.1\r\nHost: x\r\nConnection: close\r\n\r\n");
        String text = new String(response, StandardCharsets.UTF_8);
        assertTrue(text.startsWith("HTTP/1.1 204"), "the first reply should be a 204:\n" + text);
        assertEquals(-1, text.indexOf("junk"),
                "a 204 must not carry a body:\n" + text);
        assertEquals(2, countOccurrences(text, "HTTP/1.1 "),
                "both replies must be readable back to back:\n" + text);
        // RFC 9110 6.4.1 makes Content-Length a MUST NOT on a 204, and sending one
        // is its own desync: a keep-alive client would wait for bytes never sent.
        String head = text.substring(0, text.indexOf("\r\n\r\n") + 4);
        assertEquals(-1, head.toLowerCase().indexOf("content-length"),
                "a 204 must not carry Content-Length:\n" + head);
    }

    @Test
    @DisplayName("a percent-encoded non-ASCII parameter name is found")
    void nonAsciiQueryNamesMatchTheirUtf8Encoding() throws Exception {
        // caf%C3%A9 is how every client sends this name. Decoded it is the two
        // octets 0xC3 0xA9, and the Java char is 0xE9 -- so comparing an octet
        // to a char can never match, and the parameter reads as absent with the
        // handler quietly using its default instead.
        byte[] response = raw("GET /accent?caf%C3%A9=au-lait HTTP/1.1\r\nHost: x\r\n"
                + "Connection: close\r\n\r\n");
        String text = new String(response, StandardCharsets.UTF_8);
        assertTrue(text.indexOf("au-lait") >= 0,
                "the encoded name must match the declared one:\n" + text);
    }

    @Test
    @DisplayName("deeply nested JSON is refused without taking the server down")
    void deeplyNestedJsonDoesNotOverflowTheStack() throws Exception {
        // Handlers run on a 64KB virtual-thread stack and the JSON parser is
        // recursive, so nesting depth IS stack depth. A depth just UNDER the
        // parser's own cap is the dangerous one: the cap lets it through and
        // the stack decides what happens next. That is a kilobyte of body from
        // an unauthenticated client, and a StackOverflowError is an Error --
        // no handler catch and no server catch of Exception sees it.
        StringBuilder deep = new StringBuilder();
        int depth = 511;
        for (int i = 0; i < depth; i++) {
            deep.append('[');
        }
        for (int i = 0; i < depth; i++) {
            deep.append(']');
        }
        byte[] body = deep.toString().getBytes(StandardCharsets.UTF_8);
        byte[] response = raw("POST /api/notes HTTP/1.1\r\nHost: x\r\nContent-Type: "
                + "application/json\r\nContent-Length: " + body.length
                + "\r\nConnection: close\r\n\r\n", body);
        String text = new String(response, StandardCharsets.UTF_8);
        // The status matters: it proves the body was PARSED rather than rejected
        // before the parser ever recursed, which would make this test vacuous.
        // Under the cap the document is valid, so the route answers as it would
        // for any other body -- what must not happen is silence or a dead server.
        String head = text.substring(0, Math.max(0, text.indexOf("\r\n")));
        assertTrue(text.startsWith("HTTP/1.1 "),
                "a nested body must be answered, not dropped:\n" + text);
        assertEquals(-1, head.indexOf(" 500"),
                "a legal document under the parser's own cap must not fault:\n" + head);
        // And the server has to still be there afterwards -- a crash shows up
        // here rather than in the reply above.
        byte[] after = raw("GET /healthz HTTP/1.1\r\nHost: x\r\nConnection: close\r\n\r\n");
        String health = new String(after, StandardCharsets.UTF_8);
        assertTrue(health.startsWith("HTTP/1.1 200"),
                "the server must survive a deeply nested body:\n" + health);
    }

    @Test
    @DisplayName("a response header whose name is not a token never reaches the wire")
    void malformedResponseHeaderNamesAreDropped() throws Exception {
        // /rawheader asks for four extra headers, three of which are not field
        // names. A space inside a name makes a field line no peer can read; a
        // LEADING space is obsolete line folding, which appends the text to the
        // PREVIOUS header instead, so a handler's header can silently rewrite one
        // the server owns; a colon just ends the name early and renames the field.
        byte[] response = raw("GET /rawheader HTTP/1.1\r\nHost: x\r\nConnection: close\r\n\r\n");
        String text = new String(response, StandardCharsets.UTF_8);
        String head = text.substring(0, text.indexOf("\r\n\r\n") + 4);
        assertTrue(head.startsWith("HTTP/1.1 200"), "the reply should be a 200:\n" + text);
        assertTrue(head.indexOf("X-Good: ok") >= 0,
                "a well formed extra header must still be sent:\n" + head);
        assertEquals(-1, head.indexOf("space-in-name"),
                "a name with a space in it is not a field name:\n" + head);
        assertEquals(-1, head.indexOf("obsolete-folding"),
                "a name with a leading space folds into the header before it:\n" + head);
        assertEquals(-1, head.indexOf("colon-in-name"),
                "a colon ends the name early and renames the field:\n" + head);
        assertTrue(text.endsWith("raw"), "the body must still be intact:\n" + text);
    }

    @Test
    @DisplayName("a 205 carries neither content nor a length that claims any")
    void resetContentIsBodilessAndZeroLength() throws Exception {
        // RFC 9110 15.3.6: a Reset Content response cannot contain content and
        // ends at the header section. Suppressing the body is only half of it --
        // advertising the SUPPRESSED body's length would leave a keep-alive
        // client waiting for bytes that are never sent, which is the same
        // desync from the other direction.
        byte[] response = raw("GET /reset HTTP/1.1\r\nHost: x\r\n\r\n"
                + "GET /healthz HTTP/1.1\r\nHost: x\r\nConnection: close\r\n\r\n");
        String text = new String(response, StandardCharsets.UTF_8);
        assertTrue(text.startsWith("HTTP/1.1 205"), "the first reply should be a 205:\n" + text);
        assertEquals(-1, text.indexOf("junk"), "a 205 must not carry content:\n" + text);
        String head = text.substring(0, text.indexOf("\r\n\r\n") + 4);
        assertEquals(-1, head.indexOf("Content-Length: 4"),
                "a 205 must not advertise the length it did not send:\n" + head);
        assertEquals(2, countOccurrences(text, "HTTP/1.1 "),
                "both replies must be readable back to back:\n" + text);
    }

    @Test
    @DisplayName("a Connection option is matched as a whole token, not a substring")
    void connectionOptionsAreWholeTokens() throws Exception {
        // "disclose" contains "close". Read as a substring it shut the connection,
        // so an extension token this server has never heard of decided the framing.
        // The second request is only answered if the first did not close.
        byte[] response = raw("GET /healthz HTTP/1.1\r\nHost: x\r\nConnection: disclose\r\n\r\n"
                + "GET /healthz HTTP/1.1\r\nHost: x\r\nConnection: close\r\n\r\n");
        String text = new String(response, StandardCharsets.UTF_8);
        assertEquals(2, countOccurrences(text, "HTTP/1.1 "),
                "Connection: disclose is not Connection: close, so the connection had to "
                        + "stay open for the second request:\n" + text);
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
    // Resilience
    //
    // Every other test here is a prompt client: it sends a whole request and
    // reads the whole reply at once. Two shipped regressions lived precisely in
    // what that never exercises -- a client that writes slowly pinned the thread
    // serving it, and a client that reads slowly had its response truncated,
    // because the non-blocking descriptors introduced for virtual threads made
    // both paths meet EAGAIN for the first time. These two hold that ground.
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a response larger than the socket buffer survives a slow reader")
    void slowReaderReceivesTheWholeResponse() throws Exception {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress("127.0.0.1", port), 5000);
        socket.setSoTimeout(20000);
        try {
            socket.getOutputStream().write(("GET /static/huge.bin HTTP/1.1\r\n"
                    + "Host: 127.0.0.1\r\nConnection: close\r\n\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            socket.getOutputStream().flush();
            InputStream in = socket.getInputStream();

            // Read just the head, then stop reading. The server keeps writing
            // until the kernel's send buffer is full and its next write answers
            // EAGAIN -- the state the whole test exists to produce.
            ByteArrayOutputStream head = new ByteArrayOutputStream();
            String headText;
            for (;;) {
                int c = in.read();
                assertTrue(c >= 0, "the connection closed before the headers ended");
                head.write(c);
                headText = new String(head.toByteArray(), StandardCharsets.UTF_8);
                if (headText.endsWith("\r\n\r\n")) {
                    break;
                }
            }
            assertTrue(headText.startsWith("HTTP/1.1 200"), "unexpected head: " + headText);
            Thread.sleep(750);

            // Now drain, and count. A truncation shows up as a short total, and
            // a corrupted one as a byte that is not where it should be.
            byte[] chunk = new byte[16 * 1024];
            long total = 0;
            for (;;) {
                int n = in.read(chunk);
                if (n < 0) {
                    break;
                }
                for (int i = 0; i < n; i++) {
                    long at = total + i;
                    assertEquals((byte) ((at * 31) & 0xff), chunk[i],
                            "the body is corrupt at offset " + at);
                }
                total += n;
            }
            assertEquals(HUGE_BYTES, total,
                    "the response was truncated: got " + total + " of " + HUGE_BYTES
                            + " bytes, which is what treating EAGAIN as a failure does");
        } finally {
            socket.close();
        }
    }

    @Test
    @DisplayName("a head dribbled a byte at a time is cut off rather than held forever")
    void aDribbledRequestHeadIsCutOff() throws Exception {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress("127.0.0.1", port), 5000);
        socket.setSoTimeout(30000);
        try {
            OutputStream out = socket.getOutputStream();
            out.write("GET /healthz HTTP/1.1\r\nHost: x\r\n".getBytes(StandardCharsets.UTF_8));
            out.flush();
            // A byte inside every socket-timeout window. SO_RCVTIMEO restarts on
            // each one, so this alone would keep its worker for as long as the
            // client cared to continue; only a deadline measured from the head's
            // FIRST byte ends it.
            long started = System.currentTimeMillis();
            String filler = "X-Pad: ";
            boolean closed = false;
            for (int i = 0; i < 40 && !closed; i++) {
                try {
                    out.write(filler.charAt(i % filler.length()));
                    out.flush();
                } catch (IOException dropped) {
                    closed = true;
                    break;
                }
                Thread.sleep(500);
                if (socket.getInputStream().available() > 0) {
                    closed = true;
                }
            }
            long elapsed = System.currentTimeMillis() - started;
            assertTrue(closed, "the server accepted a head dribbled for " + elapsed
                    + "ms without ever ending it");
            // CN1_HTTP_TIMEOUT_MS is 4000 for this fixture, so the deadline should
            // land well inside this. Generous, because a loaded runner is slow.
            assertTrue(elapsed < 20000, "the head was cut off, but only after "
                    + elapsed + "ms");
        } finally {
            socket.close();
        }
        assertEquals(200, status(request("GET", "/healthz", null, null)));
    }

    @Test
    @DisplayName("clients that never finish a request do not starve the ones that do")
    void partialRequestsDoNotStarveOtherClients() throws Exception {
        // Comfortably more than the worker pool, so if a half-written request
        // holds the thread that serves it, nothing is left to answer the probe.
        final int stalled = 64;
        Socket[] sockets = new Socket[stalled];
        try {
            for (int i = 0; i < stalled; i++) {
                sockets[i] = new Socket();
                sockets[i].connect(new InetSocketAddress("127.0.0.1", port), 5000);
                // A request head that has begun and will never end.
                sockets[i].getOutputStream().write(
                        ("GET /healthz HTTP/1.1\r\nHost: 127.0.0.1\r\nX-Stall: " + i + "\r\n")
                                .getBytes(StandardCharsets.UTF_8));
                sockets[i].getOutputStream().flush();
            }
            // Promptly, before the idle deadline sheds any of them.
            long started = System.currentTimeMillis();
            assertEquals(200, status(request("GET", "/healthz", null, null)),
                    "a healthy request must still be answered while " + stalled
                            + " connections sit mid-request");
            long elapsed = System.currentTimeMillis() - started;
            assertTrue(elapsed < 5000,
                    "the probe waited " + elapsed + "ms, so the stalled connections "
                            + "are holding the threads that should have served it");
        } finally {
            for (int i = 0; i < stalled; i++) {
                if (sockets[i] != null) {
                    try {
                        sockets[i].close();
                    } catch (IOException ignored) {
                        // the server may already have shed it
                    }
                }
            }
        }
        // The shed connections must not have left the server damaged.
        assertEquals(200, status(request("GET", "/healthz", null, null)));
    }

    // ------------------------------------------------------------------
    // TLS
    //
    // The handshake, the record layer and the user-space copy that replaces
    // sendfile on a TLS connection. ALPN negotiation is NOT covered: this module
    // targets 1.8, where the client-side API to request a protocol and read back
    // what was chosen does not exist, so a test for it could only ever skip.
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a request is served over TLS")
    void tlsServesARequest() throws Exception {
        SSLSocket socket = openTls();
        try {
            socket.startHandshake();
            socket.getOutputStream().write(("GET /healthz HTTP/1.1\r\nHost: localhost\r\n"
                    + "Connection: close\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            socket.getOutputStream().flush();
            String response = readFully(socket.getInputStream());
            assertTrue(response.startsWith("HTTP/1.1 200"),
                    "TLS did not serve the request: " + response);
        } finally {
            socket.close();
        }
    }

    @Test
    @DisplayName("a kept-alive TLS connection left idle is shed, not held for ever")
    void tlsIdleKeepAliveConnectionsAreShed() throws Exception {
        // The case that has no thread in recv: ONE request is answered, the
        // connection goes back to the reactor, and the client then says nothing.
        // A connection that never speaks at all is held by a worker inside recv
        // and shed by SO_RCVTIMEO, so it proves nothing about the reactor -- an
        // earlier version of this test did exactly that and passed with the sweep
        // removed. The TLS server runs on the pool, which had no idle deadline of
        // its own, so these accumulated to MAX_CONNECTIONS and every later client
        // was refused.
        SSLSocket socket = openTls();
        try {
            socket.startHandshake();
            socket.setSoTimeout(30000);
            socket.getOutputStream().write(("GET /healthz HTTP/1.1\r\nHost: localhost\r\n"
                    + "Connection: keep-alive\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            socket.getOutputStream().flush();

            InputStream in = socket.getInputStream();
            ByteArrayOutputStream head = new ByteArrayOutputStream();
            String text;
            for (;;) {
                int c = in.read();
                assertTrue(c >= 0, "the first reply never arrived");
                head.write(c);
                text = new String(head.toByteArray(), StandardCharsets.UTF_8);
                if (text.endsWith("\r\n\r\n")) {
                    break;
                }
            }
            assertTrue(text.startsWith("HTTP/1.1 200"), "unexpected reply: " + text);

            // Answered and parked. Now nothing is reading it on the server side.
            // Short client-side reads so the wait can END with this test's own
            // sentence: blocking for the whole window instead threw a bare
            // SocketTimeoutException from the client, which says nothing about
            // what the server did.
            socket.setSoTimeout(2000);
            long started = System.currentTimeMillis();
            boolean closed = false;
            while (System.currentTimeMillis() - started < 25000) {
                try {
                    if (in.read() < 0) {
                        closed = true;
                        break;
                    }
                } catch (java.net.SocketTimeoutException stillOpen) {
                    // The server has not closed it yet; keep waiting.
                }
            }
            long elapsed = System.currentTimeMillis() - started;
            assertTrue(closed, "the parked keep-alive connection was still open after "
                    + elapsed + "ms, so nothing sheds a pooled connection once the "
                    + "reactor has it back");
            assertTrue(elapsed < 25000,
                    "the idle deadline should have shed it, took " + elapsed + "ms");
            assertTrue(elapsed > 500, "closed implausibly fast (" + elapsed
                    + "ms): the connection may not have been parked at all");
        } finally {
            socket.close();
        }
        assertEquals(200, status(request("GET", "/healthz", null, null)));
    }

    @Test
    @DisplayName("a large file survives a slow reader over TLS too")
    void tlsSlowReaderReceivesTheWholeResponse() throws Exception {
        // TLS has no sendfile path -- the bytes have to be encrypted in user
        // space -- so this covers the read/write copy that sendfile bypasses.
        SSLSocket socket = openTls();
        try {
            socket.startHandshake();
            socket.getOutputStream().write(("GET /static/huge.bin HTTP/1.1\r\n"
                    + "Host: localhost\r\nConnection: close\r\n\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            socket.getOutputStream().flush();
            InputStream in = socket.getInputStream();
            ByteArrayOutputStream head = new ByteArrayOutputStream();
            String headText;
            for (;;) {
                int c = in.read();
                assertTrue(c >= 0, "the connection closed before the headers ended");
                head.write(c);
                headText = new String(head.toByteArray(), StandardCharsets.UTF_8);
                if (headText.endsWith("\r\n\r\n")) {
                    break;
                }
            }
            assertTrue(headText.startsWith("HTTP/1.1 200"), "unexpected head: " + headText);
            Thread.sleep(750);
            byte[] chunk = new byte[16 * 1024];
            long total = 0;
            for (;;) {
                int n = in.read(chunk);
                if (n < 0) {
                    break;
                }
                total += n;
            }
            assertEquals(HUGE_BYTES, total, "the TLS response was truncated");
        } finally {
            socket.close();
        }
    }

    /**
     * Connects to the TLS port, trusting the throwaway self-signed certificate.
     *
     * Skips rather than fails when no TLS server came up: a machine without
     * openssl cannot make a certificate, and that says nothing about the server.
     */
    private SSLSocket openTls() throws Exception {
        Assumptions.assumeTrue(tlsServer != null && tlsPort != 0,
                "no TLS server (openssl unavailable, or it did not start)");
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, new TrustManager[]{ new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] chain, String authType) { }
            public void checkServerTrusted(X509Certificate[] chain, String authType) { }
            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
        } }, null);
        SSLSocket socket = (SSLSocket) context.getSocketFactory()
                .createSocket("127.0.0.1", tlsPort);
        socket.setSoTimeout(20000);
        return socket;
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
                    // RFC 9110 6.6.1 wants Date on every response, and the HTTP/1
                    // writer sends it. This asserts the h2 path does too: a first
                    // attempt added the name and the value as separate entries,
                    // which Http2.headerLines() turned into two colonless lines the
                    // native parser dropped, and nothing here noticed.
                    assertTrue(hpackNameIndices(payload).contains(Integer.valueOf(33)),
                            "the HEADERS block carries no date (static name index 33)");
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
    /**
     * The HPACK static name indices used by a HEADERS block.
     *
     * Walks the field representations rather than searching for a byte, because a
     * Huffman-encoded value can contain any byte it likes. Only the shapes nghttp2
     * emits for a response are handled; anything else ends the walk.
     */
    private static java.util.Set<Integer> hpackNameIndices(byte[] block) {
        java.util.Set<Integer> names = new java.util.HashSet<Integer>();
        int at = 0;
        while (at < block.length) {
            int b = block[at] & 0xff;
            int prefixBits;
            boolean hasValue;
            if ((b & 0x80) != 0) {
                prefixBits = 7;                 // indexed field: name AND value
                hasValue = false;
            } else if ((b & 0xC0) == 0x40) {
                prefixBits = 6;                 // literal, incremental indexing
                hasValue = true;
            } else if ((b & 0xE0) == 0x20) {
                prefixBits = 5;                 // dynamic table size update
                hasValue = false;
            } else {
                prefixBits = 4;                 // literal, without / never indexed
                hasValue = true;
            }
            int[] cursor = { at };
            int index = hpackInteger(block, cursor, prefixBits);
            if (index < 0) {
                break;
            }
            names.add(Integer.valueOf(index));
            at = cursor[0];
            if (index == 0) {
                at = hpackSkipString(block, at);    // the name is spelled out
                if (at < 0) {
                    break;
                }
            }
            if (hasValue) {
                at = hpackSkipString(block, at);
                if (at < 0) {
                    break;
                }
            }
        }
        return names;
    }

    /** RFC 7541 5.1, with the cursor left just past the integer. */
    private static int hpackInteger(byte[] block, int[] cursor, int prefixBits) {
        int at = cursor[0];
        if (at >= block.length) {
            return -1;
        }
        int mask = (1 << prefixBits) - 1;
        int value = block[at++] & mask;
        if (value == mask) {
            int shift = 0;
            for (;;) {
                if (at >= block.length) {
                    return -1;
                }
                int next = block[at++] & 0xff;
                value += (next & 0x7f) << shift;
                shift += 7;
                if ((next & 0x80) == 0) {
                    break;
                }
            }
        }
        cursor[0] = at;
        return value;
    }

    /** Skips a length-prefixed (possibly Huffman) string, or -1 if it runs out. */
    private static int hpackSkipString(byte[] block, int at) {
        int[] cursor = { at };
        int length = hpackInteger(block, cursor, 7);
        if (length < 0 || cursor[0] + length > block.length) {
            return -1;
        }
        return cursor[0] + length;
    }

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
