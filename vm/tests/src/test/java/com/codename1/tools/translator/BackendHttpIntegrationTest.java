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

    /**
     * A third server, pinned to ONE virtual-thread host by CN1_WORKERS=1, so a
     * test can keep that host continuously busy. With several hosts the traffic
     * and the silent connection may land on different ones and the test would
     * prove nothing some of the time, which is worse than not having it.
     */
    private static Process busyServer;
    private static int busyPort;
    private static Process smallUploadServer;
    private static int smallUploadPort;
    private static Path smallUploadLog;

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
        // A NON-ASCII NAME, for the two spellings a client may ask for it by. The
        // name is built from a char rather than written as a literal so this source
        // stays ASCII.
        Files.write(staticRoot.resolve("caf" + ((char) 0xe9) + ".txt"),
                "accented".getBytes(StandardCharsets.UTF_8));

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
        startBusyServer(work, binary, staticRoot);
        startSmallUploadServer(work, binary, staticRoot);
    }

    /**
     * A copy with a SMALL in-flight upload budget, so the budget can be reached
     * with megabytes instead of the default sixty-four. A test that has to move
     * 64MB to reach a limit is a test nobody runs.
     */
    private static void startSmallUploadServer(Path work, Path binary, Path staticRoot)
            throws Exception {
        smallUploadPort = freePort();
        ProcessBuilder run = new ProcessBuilder(binary.toString());
        run.environment().put("CN1_PORT", String.valueOf(smallUploadPort));
        run.environment().put("CN1_DB_PATH", work.resolve("upload.db").toString());
        run.environment().put("CN1_STATIC_ROOT", staticRoot.toString());
        run.environment().put("CN1_HTTP_MAX_UPLOAD_MB", "16");
        // And a small HTTP/2 body ceiling, so that one can be reached with a
        // few megabytes as well.
        run.environment().put("CN1_HTTP_MAX_H2_BODY_MB", "4");
        // A DELIBERATELY invalid rate. Zero is a divisor in fillTo() and in
        // requireChunkedProgress(), so an unguarded server throws
        // ArithmeticException on the first body needing a second read and
        // drops the connection with no response at all. Set here rather than
        // on its own fixture so every upload test on this port carries the
        // proof, and named in a test below so it cannot be deleted as noise.
        run.environment().put("CN1_HTTP_MIN_BODY_RATE", "0");
        // Invalid for the same reason and refused the same way. A non-positive
        // socket timeout means "no deadline" to the Java SE arm and disables
        // SO_RCVTIMEO on the packaged one, so a connection that opens and says
        // nothing holds a worker forever -- and a NEGATIVE one made setsockopt
        // fail inside acceptAll before the descriptor was registered, where the
        // old error path could not close it. Carried on this fixture so every
        // test on this port runs against the fallback, and named below.
        run.environment().put("CN1_HTTP_TIMEOUT_MS", "-1");
        run.redirectErrorStream(true);
        smallUploadLog = work.resolve("upload-server.log");
        run.redirectOutput(smallUploadLog.toFile());
        smallUploadServer = run.start();
        if (!waitForPort(smallUploadPort, 30000)) {
            smallUploadServer.destroy();
            smallUploadServer = null;
            smallUploadPort = 0;
        }
    }

    /** The single-host server described on busyServer. */
    private static void startBusyServer(Path work, Path binary, Path staticRoot)
            throws Exception {
        busyPort = freePort();
        ProcessBuilder run = new ProcessBuilder(binary.toString());
        run.environment().put("CN1_PORT", String.valueOf(busyPort));
        run.environment().put("CN1_DB_PATH", work.resolve("busy.db").toString());
        run.environment().put("CN1_STATIC_ROOT", staticRoot.toString());
        run.environment().put("CN1_HTTP_TIMEOUT_MS", "2000");
        run.environment().put("CN1_WORKERS", "1");
        run.redirectErrorStream(true);
        run.redirectOutput(work.resolve("busy-server.log").toFile());
        busyServer = run.start();
        if (!waitForPort(busyPort, 30000)) {
            busyServer.destroy();
            busyServer = null;
            busyPort = 0;
        }
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
        if (smallUploadServer != null) {
            smallUploadServer.destroy();
            try {
                if (!smallUploadServer.waitFor(10, TimeUnit.SECONDS)) {
                    smallUploadServer.destroyForcibly();
                }
            } catch (InterruptedException err) {
                Thread.currentThread().interrupt();
            }
        }
        if (busyServer != null) {
            busyServer.destroy();
            try {
                if (!busyServer.waitFor(10, TimeUnit.SECONDS)) {
                    busyServer.destroyForcibly();
                }
            } catch (InterruptedException err) {
                Thread.currentThread().interrupt();
            }
        }
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
    @DisplayName("the mount prefix itself redirects to its directory form")
    void theMountPrefixRedirectsToItsDirectoryForm() throws Exception {
        // GET /static -- the mount named EXACTLY, with no trailing slash. Stripping
        // the prefix leaves an empty target, which became "/" and then "/index.html"
        // and resolved to a FILE, so the directory branch that exists to issue this
        // redirect never ran: the index came back 200 at /static, and a browser then
        // resolved "style.css" in it against / rather than /static/. Every relative
        // reference in an otherwise valid site pointed one level too high.
        byte[] bare = request("GET", "/static", null, null);
        assertEquals(301, statusOf(bare),
                "GET /static did not redirect:\n" + new String(bare, StandardCharsets.UTF_8));
        assertTrue(new String(bare, StandardCharsets.UTF_8).indexOf("Location: /static/") >= 0,
                "GET /static did not point at the directory form:\n"
                        + new String(bare, StandardCharsets.UTF_8));

        // The query was addressed to this resource, so it rides across -- the same
        // rule the directory redirect below the prefix already follows.
        byte[] withQuery = request("GET", "/static?v=2", null, null);
        assertEquals(301, statusOf(withQuery));
        assertTrue(new String(withQuery, StandardCharsets.UTF_8)
                        .indexOf("Location: /static/?v=2") >= 0,
                "the query was dropped from the redirect:\n"
                        + new String(withQuery, StandardCharsets.UTF_8));

        // And the directory form itself still serves the index, which is the
        // behaviour this must not have traded away.
        assertTrue(body(request("GET", "/static/", null, null)).contains("<h1>index</h1>"),
                "/static/ stopped serving the index");

        // A sibling that merely SHARES the prefix is still not ours. The boundary
        // check above the redirect is what keeps /static2 out, and inserting a new
        // early return is exactly the kind of edit that could have bypassed it.
        assertEquals(404, statusOf(request("GET", "/static2", null, null)),
                "/static2 was treated as the mount");
    }

    @Test
    @DisplayName("static files serve, 404, and refuse to leave the document root")
    void staticFileBasics() throws Exception {
        assertTrue(body(request("GET", "/static/index.html", null, null)).contains("<h1>index</h1>"));
        assertEquals(404, status(request("GET", "/static/missing.html", null, null)));
        // Percent-encoded traversal: a check on the raw request string misses this.
        int traversal = status(request("GET", "/static/..%2f..%2fetc%2fpasswd", null, null));
        assertTrue(traversal == 403 || traversal == 404,
                "a traversal must not be served, got " + traversal);

        // Valid hex that is NOT valid UTF-8. %C3%28 is a truncated two-byte
        // sequence, and new String(_, "UTF-8") answers U+FFFD instead of failing --
        // so this path resolved to whatever a name genuinely containing U+FFFD
        // resolves to, while a bad hex DIGIT was already refused. One file, two
        // spellings, and only one of them checked.
        assertEquals(400, status(request("GET", "/static/%C3%28.html", null, null)),
                "malformed UTF-8 in a static path must be refused");

        // A signed hex pair is not a hex pair: Integer.parseInt(_, 16) accepts
        // "+1", which spelled the byte 1 a third way.
        assertEquals(400, status(request("GET", "/static/%+1.html", null, null)),
                "a signed escape must be refused");

        // And a WELL-FORMED multi-byte name still resolves, which is the direction
        // this kind of guard breaks.
        assertEquals(404, status(request("GET", "/static/caf%C3%A9.html", null, null)),
                "a valid accented name must reach the lookup and 404 on its own merits, "
                        + "not be rejected as malformed");
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

        // If-None-Match is a LIST, so a tag among others matches...
        assertEquals(304, status(request("GET", "/static/index.html", null,
                new String[]{"If-None-Match: \"other\", " + etag + ", \"third\""})),
                "a matching tag anywhere in the list is a match");
        // ...and the weak form of the same tag names the same representation,
        // which is the comparison If-None-Match takes.
        assertEquals(304, status(request("GET", "/static/index.html", null,
                new String[]{"If-None-Match: W/" + etag})),
                "If-None-Match uses weak comparison");
        assertEquals(304, status(request("GET", "/static/index.html", null,
                new String[]{"If-None-Match: *"})), "* matches any representation");

        // A DIFFERENT tag that merely embeds this one's characters is not this
        // one. Review reported the old substring test as taking "prefix<tag>" for
        // <tag>; it did not, because the tag is compared WITH its quotes and the
        // opening quote is followed by 'p' there. Asserted anyway, both because
        // the claim deserves an answer that is checked rather than argued and
        // because the list reader replacing it has to agree.
        String inner = etag.substring(1, etag.length() - 1);
        assertEquals(200, status(request("GET", "/static/index.html", null,
                new String[]{"If-None-Match: \"prefix" + inner + "\""})),
                "a tag that embeds this one is a different tag");
        assertEquals(200, status(request("GET", "/static/index.html", null,
                new String[]{"If-None-Match: \"" + inner + "suffix\""})),
                "and so is one this tag is a prefix of");
        // The unquoted spelling is not a validator at all.
        assertEquals(200, status(request("GET", "/static/index.html", null,
                new String[]{"If-None-Match: " + inner})),
                "a bare, unquoted tag is malformed and must not answer 304");

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

        // A multi-range request is VALID and satisfiable; this server just does
        // not assemble multipart/byteranges. 416 asserts that none of what was
        // asked for exists, which is a different and untrue statement, so the
        // Range is ignored and the whole representation is sent instead.
        byte[] multi = request("GET", "/static/big.bin", null,
                new String[]{"Range: bytes=0-99,200-299"});
        assertEquals(200, statusOf(multi),
                "a satisfiable multi-range must not be refused as unsatisfiable");
        assertEquals("262144", header(multi, "Content-Length"));
        assertEquals(null, header(multi, "Content-Range"),
                "a 200 describes the whole representation, so it carries no Content-Range");

        // And a Range that cannot be parsed at all is ignored for the same reason.
        assertEquals(200, status(request("GET", "/static/big.bin", null,
                new String[]{"Range: bytes=abc"})));

        // A BOUND IS AN UNSIGNED RUN OF ASCII DIGITS. Long.parseLong accepts a
        // sign and the whole of Character.digit's repertoire, so these were read
        // as ordinary ranges and answered 206 -- a partial response to a field
        // that is not a byte-range-spec. Ignored rather than refused, so the
        // client still gets the whole representation.
        String[] notRanges = {
            "bytes=+0-1",        // a sign is not part of first-byte-pos
            "bytes=0-+1",        // nor of last-byte-pos
            "bytes=-+1",         // nor of a suffix length
            "bytes=\u0660-\u0661",   // Arabic-Indic digits are digits to parseLong
            "bytes= 0-1",        // no whitespace is allowed inside the spec
            "bytes=0 -1",
            "bytes=0- 1",
            "bytes=-",           // both bounds absent is not a range
        };
        for(int iter = 0 ; iter < notRanges.length ; iter++) {
            byte[] answer = request("GET", "/static/big.bin", null,
                    new String[]{"Range: " + notRanges[iter]});
            assertEquals(200, statusOf(answer),
                    "\"" + notRanges[iter] + "\" is not a byte-range-spec and must be "
                            + "ignored, not answered as a range");
            assertEquals(null, header(answer, "Content-Range"),
                    "an ignored Range must leave no Content-Range: " + notRanges[iter]);
        }

        // The real forms still work, which is what being strict here must not cost.
        assertEquals(206, status(request("GET", "/static/big.bin", null,
                new String[]{"Range: bytes=0-99"})));
        assertEquals(206, status(request("GET", "/static/big.bin", null,
                new String[]{"Range: bytes=100-"})));
        assertEquals(206, status(request("GET", "/static/big.bin", null,
                new String[]{"Range: bytes=-100"})));
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
    @DisplayName("a silent connection is shed even while its host stays busy")
    void deadlinesAreSweptOnABusyHost() throws Exception {
        // The sweep used to run only when a poll came back EMPTY, so a host that
        // always had an event never swept -- and a client can keep that true with
        // a trickle of traffic while its other connections sit silent, holding
        // them past any timeout until the process ceiling is reached.
        //
        // This server is pinned to one virtual-thread host (CN1_WORKERS=1), so
        // the traffic below and the silent connection are certainly on the same
        // one. With several hosts they might not be, and the test would pass by
        // luck rather than by the fix.
        Assumptions.assumeTrue(busyServer != null && busyPort != 0,
                "the single-host server is not running");
        Socket quiet = new Socket();
        quiet.connect(new InetSocketAddress("127.0.0.1", busyPort), 5000);
        quiet.setSoTimeout(12000);
        try {
            // Never speaks. Its deadline is the only thing that can close it.
            InputStream in = quiet.getInputStream();
            long deadline = System.currentTimeMillis() + 10000;
            boolean closed = false;
            while (System.currentTimeMillis() < deadline) {
                // Keep the host receiving events, so a poll never comes back empty.
                Socket chatter = new Socket();
                chatter.connect(new InetSocketAddress("127.0.0.1", busyPort), 5000);
                chatter.setSoTimeout(5000);
                try {
                    chatter.getOutputStream().write(("GET /healthz HTTP/1.1\r\nHost: x\r\n"
                            + "Connection: close\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                    chatter.getOutputStream().flush();
                    while (chatter.getInputStream().read() >= 0) {
                        // drain
                    }
                } finally {
                    chatter.close();
                }
                if (in.available() > 0 || quiet.isClosed()) {
                    closed = true;
                    break;
                }
                // A read with a short timeout tells us whether the peer hung up.
                quiet.setSoTimeout(200);
                try {
                    if (in.read() < 0) {
                        closed = true;
                        break;
                    }
                } catch (java.net.SocketTimeoutException stillOpen) {
                    // expected while the deadline has not yet passed
                }
            }
            assertTrue(closed, "a connection that never spoke must be shed by its "
                    + "deadline even while the host is busy");
        } finally {
            quiet.close();
        }
    }

    @Test
    @DisplayName("the packaged client talks to the packaged server")
    void theTranslatedClientDrivesTheServer() throws Exception {
        // ParparVM on BOTH ends. Every other test here drives the server from
        // JUnit, over a raw socket or an HttpURLConnection, so the packaged
        // OUTBOUND client -- Web, and the libcurl under it -- was never exercised
        // against a real server at all. Two things only this can show: that each
        // verb arrives as itself, PATCH included, which the Java SE arm cannot
        // send and therefore cannot test; and that the two halves agree when they
        // actually meet.
        Path work = Files.createTempDirectory("backend-webcheck");
        Path clientBinary = work.resolve("webcheck");
        Path jdk8 = BackendTestSupport.findJdk8();
        BackendTestSupport.require(jdk8 != null, "no JDK 8 available to build the client");
        String failure = BackendTestSupport.build("WebCheck", "demo/webcheck", clientBinary, jdk8);
        if (failure != null) {
            BackendTestSupport.skipOrFail(failure);
            return;
        }
        ProcessBuilder run = new ProcessBuilder(clientBinary.toString());
        run.environment().put("CN1_WEBCHECK_BASE", "http://127.0.0.1:" + port);
        run.redirectErrorStream(true);
        Process client = run.start();
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        InputStream clientOut = client.getInputStream();
        byte[] chunk = new byte[4096];
        int n;
        while ((n = clientOut.read(chunk)) > 0) {
            captured.write(chunk, 0, n);
        }
        String out = new String(captured.toByteArray(), StandardCharsets.UTF_8);
        int exit = client.waitFor();
        assertTrue(out.contains("WEBCHECK OK"),
                "the translated client reported failures against the server:\n" + out);
        assertEquals(0, exit, "the translated client exited nonzero:\n" + out);
    }

    @Test
    @DisplayName("a megabyte-scale upload is read whole and answered")
    void aLargeUploadIsReadWhole() throws Exception {
        // The whole upload path had no test with a body big enough to grow the
        // buffer more than once: the 8MB fixture is a static FILE, so it exercises
        // downloads. That left the doubling growth, the rate bound and the
        // in-flight budget all resting on small bodies. Two megabytes crosses the
        // starting chunk about seven times.
        StringBuilder json = new StringBuilder(2 * 1024 * 1024 + 16);
        json.append("[\"");
        for (int i = 0; i < 2 * 1024 * 1024; i++) {
            json.append('a');
        }
        json.append("\"]");
        byte[] body = json.toString().getBytes(StandardCharsets.UTF_8);
        byte[] response = raw("POST /api/notes HTTP/1.1\r\nHost: x\r\nContent-Type: "
                + "application/json\r\nContent-Length: " + body.length
                + "\r\nConnection: close\r\n\r\n", body);
        String text = new String(response, StandardCharsets.UTF_8);
        assertTrue(text.startsWith("HTTP/1.1 "),
                "a large upload must be answered, not dropped:\n"
                        + text.substring(0, Math.min(200, text.length())));
        assertEquals(-1, text.substring(0, Math.min(64, text.length())).indexOf(" 503"),
                "a legitimate upload must not hit the in-flight budget:\n"
                        + text.substring(0, Math.min(200, text.length())));
    }

    @Test
    @DisplayName("a chunk arriving after a pause is not mistaken for a hangup")
    void aChunkInASecondPacketIsNotAHangup() throws Exception {
        // Plaintext descriptors are NON-BLOCKING in virtual-thread mode, so a read
        // with nothing ready gets EAGAIN, and reporting that as end of stream drops
        // a request that was still arriving.
        //
        // It has to be CHUNKED to reach that read. A Content-Length body goes
        // through fillTo(), which uses the copying path and parks correctly -- a
        // first version of this test used one, passed with the fix reverted, and
        // proved nothing. readChunked calls fill(), which takes the zero-copy
        // branch once the buffered bytes run out, and that is the read in
        // question.
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress("127.0.0.1", port), 5000);
        socket.setSoTimeout(20000);
        try {
            OutputStream out = socket.getOutputStream();
            // Headers and the first chunk together, so the head is fully parsed and
            // the buffered bytes are consumed before the gap.
            out.write(("POST /echo HTTP/1.1\r\nHost: x\r\nContent-Type: application/json\r\n"
                    + "Transfer-Encoding: chunked\r\nConnection: close\r\n\r\n"
                    + "2\r\n[\"\r\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
            Thread.sleep(400);
            try {
                out.write("5\r\nsplit\r\n".getBytes(StandardCharsets.UTF_8));
                out.flush();
                Thread.sleep(400);
                out.write("2\r\n\"]\r\n0\r\n\r\n".getBytes(StandardCharsets.UTF_8));
                out.flush();
            } catch (IOException closedDuringTheGap) {
                // Said in words rather than as a raw socket error: when this
                // regresses, the server has hung up mid-upload and the next write
                // meets a closed socket. "Broken pipe" alone does not say that.
                fail("the server closed the connection while the body was still "
                        + "arriving, so a valid chunked upload was dropped: "
                        + closedDuringTheGap);
            }
            byte[] response = readFullyBytes(socket.getInputStream());
            assertEquals(200, status(response),
                    "the chunks arrived in separate packets and the request was dropped:\n"
                            + new String(response, StandardCharsets.UTF_8));
            String text = new String(response, StandardCharsets.UTF_8);
            assertTrue(text.indexOf("len=9") > 0,
                    "every chunk must reach the handler, got:\n" + text);
        } finally {
            socket.close();
        }
    }

    @Test
    @DisplayName("an invalid minimum rate falls back instead of dividing by zero")
    void aZeroMinimumBodyRateDoesNotKillTheConnection() throws Exception {
        // CN1_HTTP_MIN_BODY_RATE=0 reaches a division in both body readers. The
        // ArithmeticException that follows is caught as an ordinary read failure,
        // so the connection is dropped WITHOUT a response -- a setting that looks
        // like a tuning knob and silently makes every upload fail.
        //
        // This fixture server runs with that value on purpose, so the upload tests
        // above already depend on the fallback; this one says so out loud.
        Assumptions.assumeTrue(smallUploadPort > 0,
                "the small-budget server did not start");
        // The clamp SAYS SO on stderr, and the fixture's output is captured, so
        // this is what actually bites when the guard is removed: the two runtimes
        // disagree about what a zero divisor does -- Java SE throws
        // ArithmeticException and drops the connection, ParparVM answers 0 and
        // quietly loses the rate part of the deadline -- so behaviour alone cannot
        // catch it on both. The message can.
        String log = new String(java.nio.file.Files.readAllBytes(smallUploadLog),
                StandardCharsets.UTF_8);
        // JUnit 5 here: condition first, message second.
        assertTrue(log.indexOf("CN1_HTTP_MIN_BODY_RATE=0 is below the minimum") >= 0,
                "the server did not report refusing CN1_HTTP_MIN_BODY_RATE=0:\n" + log);
        // The same fixture carries a negative socket timeout. Unclamped it is
        // worse than a useless deadline: setsockopt fails in acceptAll BEFORE the
        // descriptor reaches liveConnections, and the old handler routed that to
        // drop(), which returns without closing a descriptor it does not own --
        // one leaked fd per connection until the process runs out. That the
        // server answered every other test on this port at all is the behavioural
        // half; the refusal message is the half that cannot be explained away.
        assertTrue(log.indexOf("CN1_HTTP_TIMEOUT_MS=-1 is below the minimum") >= 0,
                "the server did not report refusing CN1_HTTP_TIMEOUT_MS=-1:\n" + log);

        // And it still serves. The body has to arrive in a SECOND packet: sent in
        // one write it is already buffered when fillTo() looks, so the method
        // returns before it ever computes the allowance -- a first version of this
        // test did exactly that and passed with the guard removed.
        byte[] body = ("[\"" + repeat('a', 4096) + "\"]").getBytes(StandardCharsets.UTF_8);
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress("127.0.0.1", smallUploadPort), 5000);
        socket.setSoTimeout(20000);
        try {
            OutputStream out = socket.getOutputStream();
            out.write(("POST /echo HTTP/1.1\r\nHost: x\r\nContent-Type: application/json\r\n"
                    + "Content-Length: " + body.length + "\r\nConnection: close\r\n\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            out.flush();
            Thread.sleep(300);
            out.write(body);
            out.flush();
            byte[] response = readFullyBytes(socket.getInputStream());
            assertEquals(200, status(response),
                    "a body needing a second read must still be served:\n"
                            + new String(response, StandardCharsets.UTF_8));
        } finally {
            socket.close();
        }
    }

    private static String repeat(char c, int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    @Test
    @DisplayName("chunked uploads are charged against the process budget too")
    void chunkedUploadsAreChargedAndReleased() throws Exception {
        // The budget bounded the fixed-length reader and nothing else, so a
        // chunked body was capped per request at 8MB and by nothing at all across
        // requests: enough clients sending almost that much and pausing before the
        // terminating chunk retain gigabytes with CN1_HTTP_MAX_UPLOAD_MB looking
        // on.
        //
        // Sized so that a leak is what fails: nine 2MB bodies against the 16MB
        // ceiling charge 18MB cumulatively, while each one alone peaks at 2MB. If
        // the charge were never made the release could not leak either, so this
        // proves both halves are wired -- and it is sequential on purpose, because
        // detecting the leak needs accumulation, not concurrency.
        Assumptions.assumeTrue(smallUploadPort > 0,
                "the small-budget server did not start");
        for (int i = 0; i < 9; i++) {
            byte[] response = chunkedPost(smallUploadPort, 2 * 1024 * 1024);
            assertEquals(200, status(response),
                    "chunked upload " + i + " was refused, so an earlier one's "
                            + "reservation was never released:\n"
                            + new String(response, StandardCharsets.UTF_8));
        }
    }

    /**
     * Posts `size` bytes of JSON to /echo, chunk-encoded.
     *
     * Built whole and written in one go rather than streamed: writing it
     * incrementally raced the server's own answer, so a legitimate early response
     * arrived as a broken pipe on the next write and the status that explained it
     * was never read.
     */
    private byte[] chunkedPost(int onPort, int size) throws IOException {
        ByteArrayOutputStream framed = new ByteArrayOutputStream();
        framed.write("2\r\n[\"\r\n".getBytes(StandardCharsets.UTF_8));
        byte[] payload = new byte[256 * 1024];
        java.util.Arrays.fill(payload, (byte) 'a');
        int sent = 0;
        while (sent < size) {
            int n = Math.min(payload.length, size - sent);
            framed.write((Integer.toHexString(n) + "\r\n").getBytes(StandardCharsets.UTF_8));
            framed.write(payload, 0, n);
            framed.write("\r\n".getBytes(StandardCharsets.UTF_8));
            sent += n;
        }
        framed.write("2\r\n\"]\r\n".getBytes(StandardCharsets.UTF_8));
        framed.write("0\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        return rawOn(onPort, "POST /echo HTTP/1.1\r\nHost: x\r\n"
                + "Content-Type: application/json\r\n"
                + "Transfer-Encoding: chunked\r\nConnection: close\r\n\r\n",
                framed.toByteArray());
    }

    @Test
    @DisplayName("a body that is not UTF-8 is refused rather than repaired")
    void malformedUtf8BodiesAreRefused() throws Exception {
        // new String(bytes, "UTF-8") never fails: it substitutes U+FFFD, so the
        // handler ran on text the client never sent and anything that validated
        // the body validated the REPLACEMENT. 0x80 is a continuation byte with
        // nothing to continue, inside an otherwise perfectly good JSON string.
        byte[] body = new byte[] {
            '[', '"', 'a', (byte) 0x80, 'b', '"', ']',
        };
        byte[] response = raw("POST /echo HTTP/1.1\r\nHost: x\r\nContent-Type: "
                + "application/json\r\nContent-Length: " + body.length
                + "\r\nConnection: close\r\n\r\n", body);
        assertEquals(400, status(response),
                "a malformed sequence must be a 400, not a silent replacement:\n"
                        + new String(response, StandardCharsets.UTF_8));

        // Multi-byte UTF-8 that IS well formed still has to get through -- the
        // rule is about malformed bytes, not about non-ASCII.
        byte[] good = ("[\"caf\u00e9\"]").getBytes(StandardCharsets.UTF_8);
        byte[] ok = raw("POST /echo HTTP/1.1\r\nHost: x\r\nContent-Type: "
                + "application/json\r\nContent-Length: " + good.length
                + "\r\nConnection: close\r\n\r\n", good);
        assertEquals(200, status(ok), new String(ok, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("a chunk size is 1*HEXDIG and nothing else")
    void chunkSizesAreStrictHex() throws Exception {
        // Integer.parseInt(_, 16) accepted a SIGN and any Unicode digit, so "+1"
        // framed a one-byte chunk and "-0" framed the TERMINATING one -- while a
        // conforming proxy in front rejects both. A server that frames a message
        // differently from the intermediary ahead of it is the whole of request
        // smuggling, which is why this parser already refuses bare LF and
        // obsolete folding.
        //
        // U+0661 is ARABIC-INDIC DIGIT ONE. Character.digit answers 1 for it, so
        // parseInt did too; it is spelled as UTF-8 bytes here because a Java
        // source file in this tree must be ASCII.
        byte[] arabicOne = new byte[] { (byte) 0xD9, (byte) 0xA1 };
        String[] bad = {
            "+1",
            "-0",
            " 1",
            "1 ",
            "",
        };
        for (int i = 0; i < bad.length; i++) {
            byte[] response = rawBytes(chunkedWithSize(bad[i].getBytes(StandardCharsets.UTF_8)));
            assertEquals(400, status(response),
                    "chunk size \"" + bad[i] + "\" must be refused:\n"
                            + new String(response, StandardCharsets.UTF_8));
        }
        byte[] unicodeDigit = rawBytes(chunkedWithSize(arabicOne));
        assertEquals(400, status(unicodeDigit),
                "a non-ASCII digit is not a hex digit:\n"
                        + new String(unicodeDigit, StandardCharsets.UTF_8));

        // And an ordinary hex size still frames a body, in both cases.
        assertEquals(200, status(rawBytes(chunkedWithSize("5".getBytes(StandardCharsets.UTF_8)))),
                "a plain size must still work");
        assertEquals(200, status(rawBytes(chunkedWithSize("5;ext=1".getBytes(StandardCharsets.UTF_8)))),
                "a chunk extension is legal and must still work");
    }

    /** A one-chunk request whose size line is exactly these bytes. */
    private byte[] chunkedWithSize(byte[] size) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(("POST /echo HTTP/1.1\r\nHost: x\r\nContent-Type: application/json\r\n"
                + "Transfer-Encoding: chunked\r\nConnection: close\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8));
        out.write(size);
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
        out.write("[\"a\"]".getBytes(StandardCharsets.UTF_8));
        out.write("\r\n0\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }

    @Test
    @DisplayName("a chunked body that is not UTF-8 is refused too")
    void malformedUtf8ChunkedBodiesAreRefused() throws Exception {
        // The chunked path decodes separately, so it needs its own proof: fixing
        // one of two body readers is how the fixed-length path came to be bounded
        // while this one was not.
        String chunk = "5\r\n";
        byte[] head = ("POST /echo HTTP/1.1\r\nHost: x\r\nContent-Type: application/json\r\n"
                + "Transfer-Encoding: chunked\r\nConnection: close\r\n\r\n" + chunk)
                .getBytes(StandardCharsets.UTF_8);
        byte[] payload = new byte[] { '[', '"', (byte) 0xC3, '"', ']' };
        byte[] tail = "\r\n0\r\n\r\n".getBytes(StandardCharsets.UTF_8);
        byte[] all = new byte[head.length + payload.length + tail.length];
        System.arraycopy(head, 0, all, 0, head.length);
        System.arraycopy(payload, 0, all, head.length, payload.length);
        System.arraycopy(tail, 0, all, head.length + payload.length, tail.length);
        byte[] response = rawBytes(all);
        assertEquals(400, status(response),
                "a truncated multi-byte sequence must be a 400:\n"
                        + new String(response, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("concurrent uploads reserve and release their budget")
    void concurrentUploadsDoNotLeakTheirBudget() throws Exception {
        // The in-flight budget is what bounds concurrent uploads, so it is charged
        // BEFORE the memory is allocated -- a budget checked afterwards bounds
        // nothing, since every thread at a growth boundary takes its memory first
        // and learns it was over the limit second.
        //
        // The ORDER is not observable from out here. A leaked RESERVATION is, and
        // only if the numbers are chosen for it: against a 16MB budget, three
        // concurrent 2MB uploads peak at 6MB and pass, while three rounds of them
        // charge 18MB cumulatively and start answering 503 the moment the release
        // stops happening. A first version of this test ran four rounds of six
        // against the DEFAULT 64MB budget -- 48MB, which never reaches the limit,
        // so it passed with the release deleted and proved nothing.
        Assumptions.assumeTrue(smallUploadPort > 0,
                "the small-upload-budget server did not start");
        final int rounds = 3;
        final int concurrent = 3;
        StringBuilder json = new StringBuilder(2 * 1024 * 1024 + 16);
        json.append("[\"");
        for (int i = 0; i < 2 * 1024 * 1024; i++) {
            json.append('a');
        }
        json.append("\"]");
        final byte[] body = json.toString().getBytes(StandardCharsets.UTF_8);

        for (int round = 0; round < rounds; round++) {
            final String[] outcomes = new String[concurrent];
            Thread[] threads = new Thread[concurrent];
            for (int i = 0; i < concurrent; i++) {
                final int slot = i;
                threads[i] = new Thread(new Runnable() {
                    public void run() {
                        try {
                            byte[] response = rawOn(smallUploadPort,
                                    "POST /api/notes HTTP/1.1\r\nHost: x\r\n"
                                    + "Content-Type: application/json\r\nContent-Length: "
                                    + body.length + "\r\nConnection: close\r\n\r\n", body);
                            String text = new String(response, StandardCharsets.UTF_8);
                            outcomes[slot] = text.substring(0, Math.min(32, text.length()));
                        } catch (Exception err) {
                            outcomes[slot] = "threw: " + err;
                        }
                    }
                });
                threads[i].start();
            }
            for (int i = 0; i < concurrent; i++) {
                threads[i].join(120000);
            }
            for (int i = 0; i < concurrent; i++) {
                assertNotNull(outcomes[i], "upload " + i + " of round " + round
                        + " never answered");
                assertEquals(-1, outcomes[i].indexOf(" 503"),
                        "round " + round + " upload " + i + " hit the in-flight budget, so a "
                                + "reservation from an earlier round was never released: "
                                + outcomes[i]);
            }
        }
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
    @DisplayName("a bare LF inside a header value is refused, not carried")
    void headerValuesMayNotHideAnotherField() throws Exception {
        // This parser ends a field at CRLF, so a bare LF in a value is just a
        // byte to it -- while an intermediary that accepts bare LF as a
        // delimiter reads TWO fields here, the second a Content-Length, and
        // frames the body by it. One connection read two ways is how the next
        // request on it becomes whatever the attacker appended.
        byte[] smuggled = raw("GET /healthz HTTP/1.1\r\nHost: x\r\n"
                + "X-Thing: value\nContent-Length: 5\r\nConnection: close\r\n\r\n");
        String text = new String(smuggled, StandardCharsets.UTF_8);
        assertTrue(text.startsWith("HTTP/1.1 400"),
                "a header value carrying a bare LF must be refused:\n" + text);

        // A name that is not a token goes the same way.
        byte[] badName = raw("GET /healthz HTTP/1.1\r\nHost: x\r\n"
                + "X Thing: value\r\nConnection: close\r\n\r\n");
        assertTrue(new String(badName, StandardCharsets.UTF_8).startsWith("HTTP/1.1 400"),
                "a field name that is not a token must be refused");

        // And an ordinary request still works, including a tab inside a value,
        // which RFC 9110 allows and which a blanket control-character rule would
        // have broken.
        byte[] ok = raw("GET /healthz HTTP/1.1\r\nHost: x\r\nX-Thing: a\tb\r\n"
                + "Connection: close\r\n\r\n");
        assertTrue(new String(ok, StandardCharsets.UTF_8).startsWith("HTTP/1.1 200"),
                "a tab is legal inside a field value");
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
    @DisplayName("a reflected header value cannot smuggle a newline through a wide char")
    void aWideCharInAHeaderValueCannotSplitTheResponse() throws Exception {
        // ?v=%C4%8A decodes to U+010A. The old value rule compared CHARS against
        // '\r', '\n' and NUL, and U+010A is none of them -- but both writers
        // narrow with a plain (byte) cast, so it reached the wire as 0x0A. A
        // handler reflecting a parameter into a header, which is the exact shape
        // that rule exists to protect, could therefore be made to write a real
        // newline into the header block: response splitting.
        byte[] response = raw("GET /rawheader?v=%C4%8A HTTP/1.1\r\nHost: x\r\n"
                + "Connection: close\r\n\r\n");
        String text = new String(response, StandardCharsets.ISO_8859_1);
        int blank = text.indexOf("\r\n\r\n");
        assertTrue(blank > 0, "no header section:\n" + text);
        String head = text.substring(0, blank);
        // A BARE LF, wherever it landed -- one not preceded by CR. Every line in
        // the header block legitimately ends CRLF, so searching for '\n' alone
        // finds those and says nothing. An injected U+010A arrives as a lone 0x0A
        // after ": ", which is what this looks for. Asserting only on the absence
        // of "X-Reflected" would pass if the header were emitted with the newline
        // still inside it.
        for(int at = 0 ; at < head.length() ; at++) {
            if(head.charAt(at) == '\n') {
                assertTrue(at > 0 && head.charAt(at - 1) == '\r',
                        "a bare LF reached the header block at " + at + ":\n"
                                + head.replace('\r', '.').replace('\n', '!'));
            }
        }
        assertEquals(-1, head.indexOf("X-Reflected"),
                "the reflected value should have been dropped entirely:\n" + head);
        assertTrue(head.startsWith("HTTP/1.1 200"), "the reply should still be a 200:\n" + text);
        assertTrue(head.indexOf("X-Good: ok") >= 0,
                "the well formed headers beside it must still be sent:\n" + head);

        // A value that is merely NON-ASCII is still legal: obs-text is 0x80-0xFF,
        // so narrowing it is lossless and this must not have become stricter than
        // RFC 9110. %C3%A9 decodes to U+00E9, which fits in one byte.
        byte[] accented = raw("GET /rawheader?v=%C3%A9 HTTP/1.1\r\nHost: x\r\n"
                + "Connection: close\r\n\r\n");
        String accentedText = new String(accented, StandardCharsets.ISO_8859_1);
        assertTrue(accentedText.indexOf("X-Reflected: \u00e9") >= 0,
                "an obs-text value is legal and must still be sent:\n" + accentedText);
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
    @DisplayName("an h2 body over the ceiling is refused, and the ceiling is given back")
    void http2BodiesAreBoundedAndReleased() throws Exception {
        // The ceiling is reserved natively, in the same step as the allocation --
        // a limit tested in Java and enforced in C is two steps with a gap, and
        // two sessions being processed at once both read the total below the
        // ceiling and then both allocate.
        //
        // What a client can see is the two ends of that: a body over the ceiling
        // is refused rather than served, and the reservation comes back when the
        // body is done, so the NEXT request over the ceiling is refused for the
        // same reason rather than because the first one is still charged. Without
        // the release, request two would be refused at any size at all.
        Assumptions.assumeTrue(smallUploadPort > 0,
                "the small-ceiling server did not start");
        assertEquals(503, h2StatusFor(smallUploadPort, "/bulk?size=" + (6 * 1024 * 1024)),
                "a body over the ceiling must be refused");
        assertEquals(200, h2StatusFor(smallUploadPort, "/bulk?size=1024"),
                "a small body after it must still be served: the refusal must not "
                        + "have left its bytes charged");
        // THREE two-megabyte bodies against a four-megabyte ceiling. Each one is
        // under it, but their sum is not, so they only all succeed if each
        // reservation is released when its body finishes. A first version of this
        // test asked for one 1KB and one 2MB body -- never reaching the ceiling
        // cumulatively -- and so passed with every release deleted.
        for (int i = 0; i < 3; i++) {
            assertEquals(200, h2StatusFor(smallUploadPort, "/bulk?size=" + (2 * 1024 * 1024)),
                    "body " + i + " of three under the ceiling was refused, so an "
                            + "earlier one's reservation was never released");
        }
        assertEquals(503, h2StatusFor(smallUploadPort, "/bulk?size=" + (6 * 1024 * 1024)),
                "and the ceiling still applies afterwards");
    }

    @Test
    @DisplayName("valid hex that is not valid UTF-8 is refused in the target")
    void malformedUtf8InTheTargetIsRefused() throws Exception {
        // %C3%28 is well formed hex and a TRUNCATED two-byte sequence. new String(_,
        // "UTF-8") answers U+FFFD instead of failing, so the handler received
        // "\uFFFD(" and the request became indistinguishable from the legitimate
        // spelling of that value, %EF%BF%BD%28. A frontend that validates UTF-8
        // rejects one and passes the other; this backend answered both the same.
        assertEquals(400, statusOf(raw("GET /healthz?name=%C3%28 HTTP/1.1\r\nHost: x\r\n"
                + "Connection: close\r\n\r\n")),
                "malformed UTF-8 in a query value must be refused");
        assertEquals(400, statusOf(raw("GET /he%C3%28althz HTTP/1.1\r\nHost: x\r\n"
                + "Connection: close\r\n\r\n")),
                "and in the path, which decodes the same way");

        // WELL-FORMED multi-byte text still gets through, which is the direction a
        // guard like this breaks. %C3%A9 is e-acute.
        assertEquals(200, statusOf(raw("GET /healthz?name=%C3%A9 HTTP/1.1\r\nHost: x\r\n"
                + "Connection: close\r\n\r\n")),
                "a valid accented query value must still be served");
        // And the legitimate spelling of U+FFFD itself is text, not a forgery.
        assertEquals(200, statusOf(raw("GET /healthz?name=%EF%BF%BD%28 HTTP/1.1\r\nHost: x\r\n"
                + "Connection: close\r\n\r\n")),
                "the real encoding of U+FFFD is valid UTF-8");
        // A target with no escapes at all takes the fast path and must be unaffected.
        assertEquals(200, statusOf(raw("GET /healthz?name=plain HTTP/1.1\r\nHost: x\r\n"
                + "Connection: close\r\n\r\n")),
                "an unencoded target must be unaffected");
        // AND OVER h2, which is the half a parser-only fix leaves behind: the
        // handler cannot tell which protocol carried the request, so one server
        // answering 400 on HTTP/1 and 200 on h2 is the same divergence as the
        // method check above.
        assertEquals(400, h2StatusFor(port, "/healthz?name=%C3%28"),
                "malformed UTF-8 must be refused over h2 as well");
        assertEquals(200, h2StatusFor(port, "/healthz?name=%C3%A9"),
                "and a valid accented value must still be served over h2");

        // RAW BYTES, with no escape anywhere -- sent as BYTES, because
        // raw(String) encodes UTF-8 and would turn the very byte under test into
        // a well formed two-byte sequence. The fast path used to return early for
        // any target with no '%' in it, so a truncated sequence sent literally
        // reached the handler as U+FFFD and aliased the same legitimate spelling
        // the escaped form does.
        assertEquals(400, statusOf(rawBytes(targetBytes(
                new byte[]{(byte) 0xC3, (byte) '('}))),
                "raw malformed UTF-8 in the target must be refused");
        // Raw UTF-8 that is WELL formed is text and must still be served: C3 A9
        // is e-acute, the same two bytes %C3%A9 spells.
        assertEquals(200, statusOf(rawBytes(targetBytes(
                new byte[]{(byte) 0xC3, (byte) 0xA9}))),
                "a raw well-formed sequence must still be served");
        // And a raw control byte is not a request target at all.
        assertEquals(400, statusOf(rawBytes(targetBytes(new byte[]{1}))),
                "a raw control byte in the target must be refused");
        assertEquals(400, statusOf(rawBytes(targetBytes(new byte[]{(byte) 0x7f}))),
                "nor is DEL");

        // A malformed ESCAPE is deliberately NOT what this rejects: percentDecode
        // passes it through as literal bytes and browsers do send a bare '%'.
        assertEquals(200, statusOf(raw("GET /healthz?pct=100%25andmore HTTP/1.1\r\nHost: x\r\n"
                + "Connection: close\r\n\r\n")),
                "an encoded percent sign is ordinary text");
    }

    @Test
    @DisplayName("a Host that is not an authority is refused before the handler")
    void malformedHostAuthoritiesAreRefused() throws Exception {
        // Missing, duplicate and disagreeing Host headers were already 400. What
        // none of those looked at is whether the value is an authority at all, so
        // these reached the handler intact -- and an application that routes or
        // authorizes on getHeader("host") then acts on a value a conforming proxy
        // in front of it would have rejected. Proxy and origin disagreeing about
        // the authority is the Content-Length/Transfer-Encoding problem wearing a
        // different header.
        String[] bad = new String[] {
            "user@internal",        // userinfo: the finding, and the dangerous one
            "example.com:notaport", // a port that is not a number
            "example.com:0",        // and one that is not connectable
            "example.com:99999",    // past the port space
            "exam ple.com",         // a space inside the authority
            "[::1",                 // an unterminated IPv6 literal
            // The brackets used to admit any run of hex, colons and dots, which
            // is not what an IPv6 literal is.
            "[.]",                  // not an address by any reading
            "[1:]",                 // a trailing single colon
            "[:1]",                 // and a leading one
            "[::1::2]",             // two runs of "::" leave the length ambiguous
            "[12345::1]",           // five hex digits is not a group
            "[1:2:3:4:5:6:7]",      // seven groups without "::"
            "[1:2:3:4:5:6:7:8:9]",  // and nine with none
            "[]",                   // empty
            "[::ffff:999.1.1.1]",   // a dotted tail out of range
            "[::ffff:01.2.3.4]",    // and one with a padded octet
            "[v1.fe80--1]",         // IPvFuture, which this deliberately refuses
            ":8080",                // no host at all
            // A '%' opens a pct-encoded triplet or it is not a '%'. Accepting it
            // as an ordinary character let an authority through that a conforming
            // frontend rejects or normalises -- the same proxy-versus-origin
            // disagreement this check exists to close, one level down.
            "bad%zz.example",       // two characters, neither of them hex
            "bad%4.example",        // one hex digit is not two
            "bad%",                 // and a triplet that runs off the end
            "a,a",                  // what a repeated field combines to
            "example.com,example.com",
            "[fe80::1%]",           // the same rule inside the bracketed form
        };
        for(int iter = 0 ; iter < bad.length ; iter++) {
            byte[] response = raw("GET /healthz HTTP/1.1\r\nHost: " + bad[iter]
                    + "\r\nConnection: close\r\n\r\n");
            assertEquals(400, statusOf(response),
                    "Host: " + bad[iter] + " should be refused:\n"
                            + new String(response, StandardCharsets.UTF_8));
        }

        // The forms that ARE authorities still serve, which is the half that keeps
        // this from being a denial of service against ordinary clients.
        String[] good = new String[] {
            "x",
            "example.com",
            "example.com:8080",
            "127.0.0.1:" + port,
            "[::1]:8080",
            "[::]",                 // the any address
            "[::1]",                // loopback, unbracketed port
            "[2001:db8::1]:443",
            "[fe80:0:0:0:0:0:0:1]", // fully written out, eight groups
            "[::ffff:127.0.0.1]",   // an IPv4-mapped address
            "[1:2:3:4:5:6:7:8]",
            "xn--80ak6aa92e.com",   // punycode, which is how a client sends an IDN
            "example.com.",         // a fully qualified name keeps its root dot
            "ok%41.example",        // a COMPLETE triplet is legal and must pass
        };
        for(int iter = 0 ; iter < good.length ; iter++) {
            byte[] response = raw("GET /healthz HTTP/1.1\r\nHost: " + good[iter]
                    + "\r\nConnection: close\r\n\r\n");
            assertEquals(200, statusOf(response),
                    "Host: " + good[iter] + " is a legal authority and must be served:\n"
                            + new String(response, StandardCharsets.UTF_8));
        }
    }

    @Test
    @DisplayName("an h2 path is not run through the generated-literal decoder")
    void h2PathsKeepTheirLiteralBytes() throws Exception {
        // The h2 natives handed :path to newStringFromCString, which is the
        // GENERATED-LITERAL reader rather than a decoder: it expands "~~uXXXX"
        // into the code unit named. So the path chose its own characters after the
        // frontend had seen the originals -- a proxy authorizing one path while
        // this server dispatched another.
        //
        // "/~~u0068ealthz" separates the two readings by STATUS alone: ~~u0068 is
        // 'h', so expanded it becomes "/healthz" and answers 200, while read
        // literally it matches no route and is a 404. It keeps the leading slash
        // that h2 requires -- a path without one is refused by nghttp2 before this
        // server sees it, measured, so the shorter "~~u002fhealthz" spelling
        // cannot be used to show this.
        assertEquals(404, h2StatusFor(port, "/~~u0068ealthz", "GET", "127.0.0.1", null),
                "an escape sequence in :path must not be expanded into a route");
        // And the real path still routes, so the check is not passing because h2
        // stopped working.
        assertEquals(200, h2StatusFor(port, "/healthz", "GET", "127.0.0.1", null),
                "the real path must still route");

        // RAW UTF-8 IN THE QUERY, which must reach the handler as the same text
        // HTTP/1 delivers. The natives build :path one char per octet, so
        // Request.byteAt takes it apart to the bytes that arrived and percentDecode
        // reads them as UTF-8 -- decoding in the native instead made byteAt narrow
        // an already-decoded character and hand the handler U+FFFD.
        assertEquals(200, h2StatusFor(port, "/accent?caf\u00c3\u00a9=x", "GET",
                "127.0.0.1", null),
                "a raw multi-byte query name must not be mangled into a bad request");
        // Malformed raw bytes are still refused, over h2 as over HTTP/1.
        assertEquals(400, h2StatusFor(port, "/healthz?name=\u00c3(", "GET",
                "127.0.0.1", null),
                "raw malformed UTF-8 must be refused over h2 too");
    }

    @Test
    @DisplayName("h2 holds :authority to the same rules as Host")
    void h2AuthoritiesAreValidated() throws Exception {
        // :authority IS Host over h2 -- this server copies it into the
        // handler-visible "host" field. A handler reading getHeader("host") cannot
        // tell which protocol carried the request, so an authority refused on one
        // side and served on the other is one server giving two answers, and
        // host-based routing or authorization is what acts on the difference.
        //
        // WHAT IS ASSERTED HERE IS WHAT MEASUREMENT SHOWED, not the whole set of
        // malformed authorities. nghttp2 applies its own HTTP messaging validation
        // first and refuses some of them at the stream level, with a stream error
        // and no response at all -- measured: ":authority: user@internal" and one
        // containing a space both time out rather than answering, so they never
        // reach this server's code and cannot be asserted as a 400. The review
        // that asked for this named user@internal specifically; the cases below
        // are the ones that DO arrive, and they did reach the handler before.
        assertEquals(400, h2StatusFor(port, "/healthz", "GET", "example.com:notaport", null),
                "a port that is not a number must be refused");
        assertEquals(400, h2StatusFor(port, "/healthz", "GET", "bad%zz.example", null),
                "an incomplete percent triplet must be refused");

        // The legal forms still serve, which is the direction this breaks.
        assertEquals(200, h2StatusFor(port, "/healthz", "GET", "example.com", null));
        assertEquals(200, h2StatusFor(port, "/healthz", "GET", "example.com:8080", null));

        // RFC 9113 8.3.1: when both are sent they have to agree. The HTTP/1 parser
        // already refuses a target authority that disagrees with Host for the same
        // reason -- a request carrying two answers to "which host did you mean" has
        // no honest reading.
        assertEquals(400, h2StatusFor(port, "/healthz", "GET", "example.com", "other.example"),
                ":authority and a Host field that disagree must be refused");
        // A DUPLICATE Host over h2. Http2 combines repeated fields on "," as RFC
        // 9110 says a repeated field line means, so two "host: a" lines arrive as
        // the one value "a,a" and HttpServer sees a single map entry -- there is
        // no count left for it to reject. The comma is what gives it away, and
        // the HTTP/1 parser answers 400 for a duplicate Host, so this is the two
        // protocols disagreeing about one request again.
        assertEquals(400, h2StatusFor(port, "/healthz", "GET", "dup.example,dup.example", null),
                "a combined duplicate Host must be refused");
        assertEquals(200, h2StatusFor(port, "/healthz", "GET", "example.com", "example.com"),
                "and agreeing ones must be served");
    }

    @Test
    @DisplayName("h2 refuses an unsupported method the same way HTTP/1 does")
    void unsupportedMethodsAreRefusedOverHttp2() throws Exception {
        // The HTTP/1 request line answers 501 for anything outside KNOWN_METHODS,
        // and the h2 path built a Request from :method and dispatched it. So the
        // two protocols on ONE server disagreed about the same request: "BREW"
        // reached application code over h2 and never over HTTP/1, where a
        // generated router turns it into a 404 and a hand-written handler may
        // treat anything that is not a GET as a write.
        assertEquals(501, h2StatusFor(port, "/healthz", "BREW"),
                "an extension method should be refused before the handler");
        // Methods are case SENSITIVE, which is the half a folded comparison would
        // have missed.
        assertEquals(501, h2StatusFor(port, "/healthz", "get"),
                "a lowercase method is not GET");
        // And the supported ones still route, which is what this must not trade.
        assertEquals(200, h2StatusFor(port, "/healthz", "GET"),
                "GET must still be served over h2");

        // The same two over HTTP/1, so the agreement is asserted rather than
        // assumed -- the point of the fix is that the answers match.
        String brewed = new String(raw("BREW /healthz HTTP/1.1\r\nHost: x\r\n"
                + "Connection: close\r\n\r\n"), StandardCharsets.UTF_8);
        assertTrue(brewed.startsWith("HTTP/1.1 501"),
                "HTTP/1 should answer 501 for BREW:\n" + brewed);
    }

    /** The :status of one h2c GET, decoded from the HEADERS block. */
    private int h2StatusFor(int onPort, String path) throws Exception {
        return h2StatusFor(onPort, path, "GET");
    }

    private int h2StatusFor(int onPort, String path, String method) throws Exception {
        return h2StatusFor(onPort, path, method, "127.0.0.1", null);
    }

    /**
     * @param authority  the :authority pseudo-header
     * @param hostHeader an additional literal "host" field, or null to send none.
     *                   RFC 9113 8.3.1 requires the two to agree when both are sent.
     */
    private int h2StatusFor(int onPort, String path, String method, String authority,
                            String hostHeader) throws Exception {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress("127.0.0.1", onPort), 5000);
        socket.setSoTimeout(20000);
        try {
            OutputStream out = socket.getOutputStream();
            out.write("PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            out.write(frame(4, 0, 0, new byte[0]));
            byte[] windowUpdate = new byte[4];
            int increment = 8 * 1024 * 1024;
            windowUpdate[0] = (byte) ((increment >> 24) & 0x7f);
            windowUpdate[1] = (byte) ((increment >> 16) & 0xff);
            windowUpdate[2] = (byte) ((increment >> 8) & 0xff);
            windowUpdate[3] = (byte) (increment & 0xff);
            out.write(frame(8, 0, 0, windowUpdate));
            ByteArrayOutputStream block = new ByteArrayOutputStream();
            hpackLiteral(block, ":method", method);
            hpackLiteral(block, ":path", path);
            hpackLiteral(block, ":scheme", "http");
            hpackLiteral(block, ":authority", authority);
            if (hostHeader != null) {
                hpackLiteral(block, "host", hostHeader);
            }
            out.write(frame(1, 0x05, 1, block.toByteArray()));
            out.flush();
            out.write(frame(8, 0, 1, windowUpdate));
            out.flush();

            long deadline = System.currentTimeMillis() + 20000;
            InputStream in = socket.getInputStream();
            boolean done = false;
            int status = -1;
            while (System.currentTimeMillis() < deadline && !done) {
                byte[] header = readExactly(in, 9);
                if (header == null) {
                    break;
                }
                int length = ((header[0] & 0xff) << 16) | ((header[1] & 0xff) << 8)
                        | (header[2] & 0xff);
                int type = header[3] & 0xff;
                int flags = header[4] & 0xff;
                byte[] payload = length == 0 ? new byte[0] : readExactly(in, length);
                if (payload == null) {
                    break;
                }
                if (type == 1 && payload.length > 0) {
                    status = hpackStatus(payload);
                    done = (flags & 0x01) != 0;
                } else if (type == 0) {
                    done = (flags & 0x01) != 0;
                } else if (type == 7) {
                    fail("the server sent GOAWAY: " + new String(payload, StandardCharsets.UTF_8));
                }
            }
            return status;
        } finally {
            socket.close();
        }
    }

    @Test
    @DisplayName("a large h2 body survives the bounded output buffer")
    void http2DeliversABodyLargerThanTheOutputBuffer() throws Exception {
        // The serialisation buffer is capped, and the send callback answers
        // WOULDBLOCK once it is full so nghttp2 stops and keeps the rest. That
        // only works because drain() pumps again after emptying; a cap without
        // the re-pump would truncate every response bigger than the buffer, and
        // the small bodies every other test sends would never notice.
        int size = 3 * 1024 * 1024;
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress("127.0.0.1", port), 5000);
        socket.setSoTimeout(20000);
        try {
            OutputStream out = socket.getOutputStream();
            out.write("PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            out.write(frame(4, 0, 0, new byte[0]));
            // Raise the connection window so the whole body is writable at once:
            // that is the condition under which nghttp2 fills the buffer in a
            // single pump, which is exactly what the cap has to survive.
            byte[] windowUpdate = new byte[4];
            int increment = size + 65536;
            windowUpdate[0] = (byte) ((increment >> 24) & 0x7f);
            windowUpdate[1] = (byte) ((increment >> 16) & 0xff);
            windowUpdate[2] = (byte) ((increment >> 8) & 0xff);
            windowUpdate[3] = (byte) (increment & 0xff);
            out.write(frame(8, 0, 0, windowUpdate));
            ByteArrayOutputStream block = new ByteArrayOutputStream();
            hpackLiteral(block, ":method", "GET");
            hpackLiteral(block, ":path", "/bulk?size=" + size);
            hpackLiteral(block, ":scheme", "http");
            hpackLiteral(block, ":authority", "127.0.0.1");
            out.write(frame(1, 0x05, 1, block.toByteArray()));
            out.flush();
            out.write(frame(8, 0, 1, windowUpdate));   // and the stream window
            out.flush();

            ByteArrayOutputStream received = new ByteArrayOutputStream();
            boolean endStream = false;
            long deadline = System.currentTimeMillis() + 20000;
            InputStream in = socket.getInputStream();
            while (System.currentTimeMillis() < deadline && !endStream) {
                byte[] header = readExactly(in, 9);
                if (header == null) {
                    break;
                }
                int length = ((header[0] & 0xff) << 16) | ((header[1] & 0xff) << 8) | (header[2] & 0xff);
                int type = header[3] & 0xff;
                int flags = header[4] & 0xff;
                byte[] payload = length == 0 ? new byte[0] : readExactly(in, length);
                if (payload == null) {
                    break;
                }
                if (type == 0) {
                    received.write(payload);
                    endStream = (flags & 0x01) != 0;
                } else if (type == 7) {
                    fail("the server sent GOAWAY: " + new String(payload, StandardCharsets.UTF_8));
                }
            }
            assertTrue(endStream, "the stream never ended; got " + received.size() + " of " + size);
            assertEquals(size, received.size(), "the body was truncated");
            byte[] bytes = received.toByteArray();
            for (int iter = 0; iter < bytes.length; iter++) {
                if (bytes[iter] != (byte) ('a' + (iter % 26))) {
                    fail("byte " + iter + " is wrong: the frames were reassembled out of order");
                }
            }
        } finally {
            socket.close();
        }
    }

    @Test
    @DisplayName("an h2 HEAD of a static file closes its descriptor exactly once")
    void http2HeadOfAFileClosesItOnce() throws Exception {
        // A review reported this as a LEAK: the bodiless branch never closes the
        // descriptor, so repeated HEADs exhaust the process. It is not -- the
        // responseBodyFor() call below that branch closes it in a finally, which
        // is why it is called at all on a path that wants no body.
        //
        // Adding a close there anyway made it a DOUBLE close, and this is what
        // says so: the count runs NEGATIVE, one per request. That is worse than
        // the reported bug, because a descriptor number is reusable the moment
        // the first close returns and the second lands on whoever took it.
        //
        // Both directions are pinned here on purpose. Zero is the answer; a
        // positive number is the leak the review predicted and a negative one is
        // the "fix" for it.
        int before = openStaticFiles();
        for (int i = 0; i < 10; i++) {
            assertEquals(200, h2StatusFor(port, "/static/big.bin", "HEAD"),
                    "the HEAD itself must be answered");
        }
        assertEquals(before, openStaticFiles(),
                "ten HEADs must leave the descriptor count exactly where it was");
    }

    @Test
    void bothSpellingsOfANonAsciiPathFindTheSameFile() throws Exception {
        // A target arrives as bytes, and a client may spell a non-ASCII filename
        // percent-encoded or raw. Both are valid and both name the same file.
        //
        // The raw one used to 404. The decoder returned the target untouched when
        // it held no '%', so its two octets stayed two CHARACTERS, and the name
        // went to the open re-encoded as UTF-8 -- four bytes, matching nothing on
        // disk. The escaped spelling decoded to one character and found the file,
        // which is what made this a divergence between two spellings of one
        // request rather than a plain miss.
        byte[] escaped = rawGet("/static/caf%C3%A9.txt");
        assertEquals(200, statusOf(escaped),
                "the percent-encoded spelling must find the file");
        assertEquals("accented", body(escaped));

        ByteArrayOutputStream target = new ByteArrayOutputStream();
        target.write("/static/caf".getBytes(StandardCharsets.US_ASCII));
        target.write(0xc3);
        target.write(0xa9);
        target.write(".txt".getBytes(StandardCharsets.US_ASCII));
        byte[] rawSpelling = rawGet(new String(target.toByteArray(),
                StandardCharsets.ISO_8859_1));
        assertEquals(200, statusOf(rawSpelling),
                "and so must the raw one, which names the same file");
        assertEquals("accented", body(rawSpelling));
    }

    @Test
    void aMalformedPercentEscapeIsRefused() throws Exception {
        // RFC 3986 leaves one reading of '%': two hex digits follow it. The server
        // used to decline to DECODE a malformed escape and then copy it through as
        // literal bytes, which are valid UTF-8, so the target was accepted and
        // handed to a hand-written handler as itself -- while the generated routers
        // and StaticFiles refused the same target. One request, two answers,
        // depending on which kind of handler sat behind it.
        assertEquals(400, statusOf(rawGet("/healthz?name=%ZZ")),
                "a non-hex escape is not a target");
        assertEquals(400, statusOf(rawGet("/healthz?name=%2")),
                "and neither is a truncated one");
        assertEquals(400, statusOf(rawGet("/healthz%")),
                "nor a bare percent at the end");
        // The well-formed spelling of the same thing still works, so what was
        // refused is the malformation and not the escape.
        assertEquals(200, statusOf(rawGet("/healthz?name=%41")),
                "a well-formed escape is still served");
    }

    /**
     * A GET whose target is written as exactly these octets.
     *
     * <p>Not raw(String), which encodes with UTF-8 and so re-spells any non-ASCII
     * character as a well-formed sequence -- the opposite of a test that needs the
     * bytes it wrote. The target is carried here as ISO-8859-1 so each char is one
     * octet on the wire.
     */
    private byte[] rawGet(String target) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write("GET ".getBytes(StandardCharsets.US_ASCII));
        out.write(target.getBytes(StandardCharsets.ISO_8859_1));
        out.write(" HTTP/1.1\r\nHost: x\r\nConnection: close\r\n\r\n"
                .getBytes(StandardCharsets.US_ASCII));
        return rawBytes(out.toByteArray());
    }

    /** The server's own count of descriptors handed out and not yet closed. */
    private int openStaticFiles() throws Exception {
        String metrics = body(request("GET", "/healthz", null, null));
        int at = metrics.indexOf("\"openStaticFiles\"");
        assertTrue(at >= 0, "the server does not report openStaticFiles: " + metrics);
        int colon = metrics.indexOf(':', at);
        int end = colon + 1;
        // The MINUS matters. A first version scanned for digits only, so the -10
        // that a double close produces was read as 10 and reported as the leak
        // being looked for -- the measurement agreed with the hypothesis by
        // discarding the character that disproved it.
        while (end < metrics.length() && "-0123456789".indexOf(metrics.charAt(end)) < 0) {
            end++;
        }
        int start = end;
        if (end < metrics.length() && metrics.charAt(end) == '-') {
            end++;
        }
        while (end < metrics.length() && "0123456789".indexOf(metrics.charAt(end)) >= 0) {
            end++;
        }
        return Integer.parseInt(metrics.substring(start, end));
    }

    @Test
    @DisplayName("a HEAD over h2 reports the length a GET would send")
    void http2HeadReportsRealLength() throws Exception {
        // The HTTP/1 writer keeps the representation length for a HEAD, because
        // describing what is NOT being sent is the whole point of asking. The
        // HTTP/2 path did not, so one static file answered a size over one
        // protocol and nothing over the other, from the same handler.
        //
        // The presence of the header is what is asserted here: its value is
        // HPACK-encoded and may be Huffman-coded, and headReportsRealLength
        // already pins the exact number over HTTP/1. Static index 28 is
        // content-length (RFC 7541 Appendix A).
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress("127.0.0.1", port), 5000);
        socket.setSoTimeout(10000);
        try {
            OutputStream out = socket.getOutputStream();
            out.write("PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            out.write(frame(4, 0, 0, new byte[0]));
            ByteArrayOutputStream block = new ByteArrayOutputStream();
            hpackLiteral(block, ":method", "HEAD");
            hpackLiteral(block, ":path", "/static/big.bin");
            hpackLiteral(block, ":scheme", "http");
            hpackLiteral(block, ":authority", "127.0.0.1");
            out.write(frame(1, 0x05, 1, block.toByteArray()));
            out.flush();

            byte[] responseHeaders = null;
            long deadline = System.currentTimeMillis() + 8000;
            InputStream in = socket.getInputStream();
            while (System.currentTimeMillis() < deadline && responseHeaders == null) {
                byte[] header = readExactly(in, 9);
                if (header == null) {
                    break;
                }
                int length = ((header[0] & 0xff) << 16) | ((header[1] & 0xff) << 8)
                        | (header[2] & 0xff);
                int type = header[3] & 0xff;
                byte[] payload = length == 0 ? new byte[0] : readExactly(in, length);
                if (payload == null) {
                    break;
                }
                if (type == 1) {
                    responseHeaders = payload;
                }
            }
            assertNotNull(responseHeaders, "no HEADERS frame came back for the HEAD");
            assertTrue(hpackNameIndices(responseHeaders).contains(Integer.valueOf(28)),
                    "a HEAD over h2 must report the length it is not sending");
        } finally {
            socket.close();
        }
    }

    @Test
    @DisplayName("a HEAD over h2 reports the length of a DEFERRED json body")
    void http2HeadReportsDeferredJsonLength() throws Exception {
        // respondJson leaves the value unserialised so the HTTP/1 writer can render
        // it straight into the connection buffer, which means response.body is
        // EMPTY. The h2 HEAD calculation measured that array and answered
        // content-length: 0 for a representation that is not -- and the earlier h2
        // HEAD fix did not close it, because it only learned about file and eager
        // byte-array bodies. /deferred is the only route shaped this way.
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress("127.0.0.1", port), 5000);
        socket.setSoTimeout(10000);
        try {
            OutputStream out = socket.getOutputStream();
            out.write("PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            out.write(frame(4, 0, 0, new byte[0]));
            ByteArrayOutputStream block = new ByteArrayOutputStream();
            hpackLiteral(block, ":method", "HEAD");
            hpackLiteral(block, ":path", "/deferred");
            hpackLiteral(block, ":scheme", "http");
            hpackLiteral(block, ":authority", "127.0.0.1");
            out.write(frame(1, 0x05, 1, block.toByteArray()));
            out.flush();

            byte[] responseHeaders = null;
            long deadline = System.currentTimeMillis() + 8000;
            InputStream in = socket.getInputStream();
            while (System.currentTimeMillis() < deadline && responseHeaders == null) {
                byte[] header = readExactly(in, 9);
                if (header == null) {
                    break;
                }
                int length = ((header[0] & 0xff) << 16) | ((header[1] & 0xff) << 8)
                        | (header[2] & 0xff);
                int type = header[3] & 0xff;
                byte[] payload = length == 0 ? new byte[0] : readExactly(in, length);
                if (payload == null) {
                    break;
                }
                if (type == 1) {
                    responseHeaders = payload;
                }
            }
            assertNotNull(responseHeaders, "no HEADERS frame came back for the HEAD");
            long described = hpackNumericValue(responseHeaders, 28);
            assertTrue(described > 0,
                    "a HEAD of a deferred json body reported " + described
                            + " instead of the length a GET would send");
            // And it is the length a GET really sends, not merely nonzero.
            String json = body(request("GET", "/deferred", null, null));
            assertEquals(json.getBytes(StandardCharsets.UTF_8).length, described,
                    "the described length is not the one a GET returns");
        } finally {
            socket.close();
        }
    }

    @Test
    @DisplayName("a HEAD of a 204 over h2 carries no length either")
    void http2HeadOfABodilessStatusHasNoLength() throws Exception {
        // The HEAD rule and the STATUS rule meet here. A HEAD describes the
        // representation it is not sending, but a 204 has none to describe and
        // RFC 9110 6.4.1 forbids the field outright -- which the HTTP/1 writer
        // already honours. Adding it unconditionally on the h2 path made one
        // response valid over one protocol and invalid over the other, the exact
        // divergence the HEAD fix existed to remove.
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress("127.0.0.1", port), 5000);
        socket.setSoTimeout(10000);
        try {
            OutputStream out = socket.getOutputStream();
            out.write("PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            out.write(frame(4, 0, 0, new byte[0]));
            ByteArrayOutputStream block = new ByteArrayOutputStream();
            hpackLiteral(block, ":method", "HEAD");
            hpackLiteral(block, ":path", "/nocontent");
            hpackLiteral(block, ":scheme", "http");
            hpackLiteral(block, ":authority", "127.0.0.1");
            out.write(frame(1, 0x05, 1, block.toByteArray()));
            out.flush();

            byte[] responseHeaders = null;
            long deadline = System.currentTimeMillis() + 8000;
            InputStream in = socket.getInputStream();
            while (System.currentTimeMillis() < deadline && responseHeaders == null) {
                byte[] header = readExactly(in, 9);
                if (header == null) {
                    break;
                }
                int length = ((header[0] & 0xff) << 16) | ((header[1] & 0xff) << 8)
                        | (header[2] & 0xff);
                int type = header[3] & 0xff;
                byte[] payload = length == 0 ? new byte[0] : readExactly(in, length);
                if (payload == null) {
                    break;
                }
                if (type == 1) {
                    responseHeaders = payload;
                }
            }
            assertNotNull(responseHeaders, "no HEADERS frame came back for the HEAD");
            assertTrue(!hpackNameIndices(responseHeaders).contains(Integer.valueOf(28)),
                    "a 204 must not carry content-length, over either protocol");
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

    /**
     * The value of one static-index field, read as a number, or -1 if absent.
     *
     * Only content-length is asked for here and its value is always digits, so the
     * Huffman side needs the ten digit codes and nothing more (RFC 7541 Appendix B:
     * 0, 1 and 2 are five bits, 3 through 9 are six). nghttp2 picks Huffman only
     * when it is strictly shorter, which for digits starts at three of them -- so a
     * test that read the raw bytes alone would pass on short lengths and quietly
     * stop asserting on longer ones.
     */
    private static long hpackNumericValue(byte[] block, int nameIndex) {
        int at = 0;
        while (at < block.length) {
            int b = block[at] & 0xff;
            int prefixBits;
            boolean hasValue;
            if ((b & 0x80) != 0) {
                prefixBits = 7;
                hasValue = false;
            } else if ((b & 0xC0) == 0x40) {
                prefixBits = 6;
                hasValue = true;
            } else if ((b & 0xE0) == 0x20) {
                prefixBits = 5;
                hasValue = false;
            } else {
                prefixBits = 4;
                hasValue = true;
            }
            int[] cursor = { at };
            int index = hpackInteger(block, cursor, prefixBits);
            if (index < 0) {
                return -1;
            }
            at = cursor[0];
            if (index == 0) {
                at = hpackSkipString(block, at);
                if (at < 0) {
                    return -1;
                }
            }
            if (hasValue) {
                int valueAt = at;
                at = hpackSkipString(block, at);
                if (at < 0) {
                    return -1;
                }
                if (index == nameIndex) {
                    return hpackDigits(block, valueAt);
                }
            }
        }
        return -1;
    }

    /**
     * The :status of a HEADERS block, decoded rather than guessed.
     *
     * A first version read "first byte is 0x88, so 200, otherwise 503". That is
     * true only when nghttp2 happens to emit the indexed form first, and a 200
     * carrying content-length did not -- so a perfectly good response was
     * reported as the failure the test was looking for, which is the worst
     * direction for a guess to be wrong in.
     *
     * :status occupies static-table entries 8 through 14 (200, 204, 206, 304,
     * 400, 404, 500); anything else arrives as a literal against name index 8.
     */
    private static int hpackStatus(byte[] block) {
        int[] indexed = { 0, 0, 0, 0, 0, 0, 0, 0, 200, 204, 206, 304, 400, 404, 500 };
        int at = 0;
        while (at < block.length) {
            int b = block[at] & 0xff;
            int prefixBits;
            boolean hasValue;
            if ((b & 0x80) != 0) {
                prefixBits = 7;
                hasValue = false;
            } else if ((b & 0xC0) == 0x40) {
                prefixBits = 6;
                hasValue = true;
            } else if ((b & 0xE0) == 0x20) {
                prefixBits = 5;
                hasValue = false;
            } else {
                prefixBits = 4;
                hasValue = true;
            }
            int[] cursor = { at };
            int index = hpackInteger(block, cursor, prefixBits);
            if (index < 0) {
                return -1;
            }
            at = cursor[0];
            if (!hasValue && index >= 8 && index <= 14) {
                return indexed[index];
            }
            if (index == 0) {
                at = hpackSkipString(block, at);
                if (at < 0) {
                    return -1;
                }
            }
            if (hasValue) {
                int valueAt = at;
                at = hpackSkipString(block, at);
                if (at < 0) {
                    return -1;
                }
                if (index == 8) {
                    return (int) hpackDigits(block, valueAt);
                }
            }
        }
        return -1;
    }

    /** A length-prefixed string of digits, raw or Huffman, as a number. */
    private static long hpackDigits(byte[] block, int at) {
        boolean huffman = (block[at] & 0x80) != 0;
        int[] cursor = { at };
        int length = hpackInteger(block, cursor, 7);
        if (length < 0 || cursor[0] + length > block.length) {
            return -1;
        }
        StringBuilder text = new StringBuilder();
        if (!huffman) {
            for (int iter = 0; iter < length; iter++) {
                text.append((char) (block[cursor[0] + iter] & 0xff));
            }
        } else {
            int bits = length * 8;
            int position = 0;
            while (bits - position >= 5) {
                int five = hpackBits(block, cursor[0], position, 5);
                if (five <= 2) {                       // 00000, 00001, 00010
                    text.append((char) ('0' + five));
                    position += 5;
                    continue;
                }
                if (bits - position < 6) {
                    break;                             // what is left is padding
                }
                int six = hpackBits(block, cursor[0], position, 6);
                if (six < 0x19 || six > 0x1f) {        // 011001 .. 011111
                    return -1;                         // not a digit: give up loudly
                }
                text.append((char) ('3' + (six - 0x19)));
                position += 6;
            }
        }
        try {
            return Long.parseLong(text.toString());
        } catch (NumberFormatException notANumber) {
            return -1;
        }
    }

    /** `count` bits starting `position` bits into the bytes at `from`. */
    private static int hpackBits(byte[] block, int from, int position, int count) {
        int value = 0;
        for (int iter = 0; iter < count; iter++) {
            int bit = position + iter;
            int b = block[from + (bit >> 3)] & 0xff;
            value = (value << 1) | ((b >> (7 - (bit & 7))) & 1);
        }
        return value;
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
        // ISO-8859-1, so one char is one OCTET. An HPACK literal carries bytes,
        // not text, and encoding UTF-8 here meant a test could not express a raw
        // byte at all: "\u00c3" went out as the well formed pair C3 83 and the
        // server rightly answered 200, which read as the check failing. Every
        // other call site passes ASCII, where the two encodings agree.
        byte[] n = name.getBytes(StandardCharsets.ISO_8859_1);
        byte[] v = value.getBytes(StandardCharsets.ISO_8859_1);
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
        return rawOn(port, head, body);
    }

    /** Writes exactly these bytes, for a request whose body is not text. */
    /**
     * A complete GET whose query value is exactly {@code value}, byte for byte.
     *
     * raw(String) encodes with UTF-8, which re-encodes any non-ASCII char into a
     * WELL formed sequence -- the opposite of what a malformed-byte test needs.
     */
    private byte[] targetBytes(byte[] value) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write("GET /healthz?name=".getBytes(StandardCharsets.US_ASCII));
        out.write(value, 0, value.length);
        out.write((" HTTP/1.1\r\nHost: x\r\nConnection: close\r\n\r\n")
                .getBytes(StandardCharsets.US_ASCII));
        return out.toByteArray();
    }

    private byte[] rawBytes(byte[] all) throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress("127.0.0.1", port), 5000);
        socket.setSoTimeout(15000);
        try {
            socket.getOutputStream().write(all);
            socket.getOutputStream().flush();
            return readFullyBytes(socket.getInputStream());
        } finally {
            socket.close();
        }
    }

    private byte[] rawOn(int onPort, String head, byte[] body) throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress("127.0.0.1", onPort), 5000);
        socket.setSoTimeout(15000);
        try {
            OutputStream out = socket.getOutputStream();
            try {
                out.write(head.getBytes(StandardCharsets.UTF_8));
                if (body.length > 0) {
                    out.write(body);
                }
                out.flush();
            } catch (IOException earlyClose) {
                // A server is allowed to answer and close before the body finishes
                // arriving -- a 413 or a 503 is exactly that -- and then the rest
                // of the write meets a closed socket. Reading its answer here is
                // the difference between a test that reports "503" and one that
                // reports "Broken pipe" and hides the reason.
                byte[] answered = readFullyBytes(socket.getInputStream());
                if (answered.length > 0) {
                    return answered;
                }
                throw earlyClose;
            }
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
