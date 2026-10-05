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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Request bodies that are not UTF-8 text -- uploads, multipart forms, gzip --
/// and the two response policies a browser or a mobile client relies on: CORS
/// and compression. Real sockets, so the HTTP/1.1 parser is what decides.
class HttpBodiesTest {
    private Backend backend;
    private int port;

    @AfterEach
    void stop() {
        if (backend != null) {
            backend.stop();
        }
    }

    /// One Response for every request, as a handler with a constant answer returns.
    private static final HttpServer.Response SHARED;

    static {
        StringBuilder sb = new StringBuilder();
        for (int iter = 0 ; iter < 500 ; iter++) {
            sb.append("{\"n\":").append(iter).append("},");
        }
        SHARED = HttpServer.Response.json(200, "[" + sb + "{}]");
    }

    /// Echoes what the request arrived as: the content type, the body's length
    /// and form, its parts and parameters.
    private void start(Properties extra) throws Exception {
        port = freePort();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        if (extra != null) {
            settings.putAll(extra);
        }
        backend = Backend.builder(Config.of(settings, "test")).quiet()
                .handler(new HttpServer.Handler() {
                    public HttpServer.Response handle(HttpServer.Request request) throws Exception {
                        String path = request.getTarget();
                        if ("OPTIONS".equals(request.getMethod())) {
                            // Declined, as a generated router declines a method it
                            // has no route for, so the CORS policy answers it.
                            return null;
                        }
                        if (path.startsWith("/bytes")) {
                            byte[] body = request.getBodyBytes();
                            return new HttpServer.Response(200, "application/octet-stream",
                                    body == null ? new byte[0] : body);
                        }
                        if (path.startsWith("/text")) {
                            return HttpServer.Response.text(200, String.valueOf(request.getBody()));
                        }
                        if (path.startsWith("/parts")) {
                            StringBuilder sb = new StringBuilder();
                            List parts = request.getParts();
                            for (int iter = 0 ; iter < parts.size() ; iter++) {
                                HttpServer.Part p = (HttpServer.Part) parts.get(iter);
                                sb.append(p.getName()).append('|').append(p.getFilename())
                                        .append('|').append(p.getContentType()).append('|')
                                        .append(p.getSize()).append(';');
                            }
                            return HttpServer.Response.text(200, sb.toString());
                        }
                        if (path.startsWith("/param")) {
                            return HttpServer.Response.text(200, request.param("a") + ","
                                    + request.param("b"));
                        }
                        if (path.startsWith("/shared")) {
                            return SHARED;
                        }
                        if (path.startsWith("/boom")) {
                            throw new IllegalStateException("boom");
                        }
                        if (path.startsWith("/etag")) {
                            StringBuilder sb = new StringBuilder();
                            while (sb.length() < 5000) {
                                sb.append("{\"n\":1},");
                            }
                            return HttpServer.Response.json(200, "[" + sb + "{}]")
                                    .header("ETag", path.endsWith("weak") ? "W/\"v1\"" : "\"v1\"");
                        }
                        if (path.startsWith("/range")) {
                            // Bytes 0-4999 of a larger JSON document, as a range answer.
                            StringBuilder sb = new StringBuilder();
                            while (sb.length() < 5000) {
                                sb.append('x');
                            }
                            return HttpServer.Response.json(206, sb.toString())
                                    .header("Content-Range", "bytes 0-4999/20000");
                        }
                        if (path.startsWith("/big")) {
                            StringBuilder sb = new StringBuilder();
                            for (int iter = 0 ; iter < 500 ; iter++) {
                                sb.append("{\"n\":").append(iter).append("},");
                            }
                            return HttpServer.Response.json(200, "[" + sb + "{}]");
                        }
                        return null;
                    }
                })
                .start();
    }

    @Test
    @DisplayName("a binary upload arrives byte for byte; it used to be refused as bad UTF-8")
    void aBinaryBodyIsKeptAsBytes() throws Exception {
        start(null);
        byte[] png = new byte[256];
        for (int iter = 0 ; iter < png.length ; iter++) {
            png[iter] = (byte) iter;
        }
        HttpURLConnection c = post("/bytes", "image/png", png, null);
        assertEquals(200, c.getResponseCode());
        assertArrayEquals(png, read(c.getInputStream()));
    }

    @Test
    @DisplayName("a text body that is not UTF-8 is still a 400")
    void malformedTextIsStillRefused() throws Exception {
        start(null);
        HttpURLConnection c = post("/text", "text/plain", new byte[] {(byte) 0xC3, (byte) 0x28},
                null);
        assertEquals(400, c.getResponseCode());
    }

    @Test
    @DisplayName("a gzip request body is inflated before the handler sees it")
    void aGzipBodyIsDecoded() throws Exception {
        start(null);
        HttpURLConnection c = post("/text", "application/json", gzip("{\"hello\":\"world\"}"),
                "gzip");
        assertEquals(200, c.getResponseCode());
        assertEquals("{\"hello\":\"world\"}", new String(read(c.getInputStream()), "UTF-8"));

        c = post("/text", "application/json", "not gzip".getBytes("UTF-8"), "gzip");
        assertEquals(400, c.getResponseCode(), "bytes that are not gzip were accepted");
        c = post("/text", "application/json", "x".getBytes("UTF-8"), "br");
        assertEquals(415, c.getResponseCode(), "a coding the server cannot decode was accepted");
    }

    @Test
    @DisplayName("a multipart form splits into its fields and files")
    void multipartPartsAndFields() throws Exception {
        start(null);
        String boundary = "----cn1boundary";
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"a\"\r\n\r\n"
                + "first\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"x.bin\"\r\n"
                + "Content-Type: application/octet-stream\r\n\r\n").getBytes("UTF-8"));
        body.write(new byte[] {0, (byte) 0xFF, 13, 10, 45, 45});
        body.write(("\r\n--" + boundary + "--\r\n").getBytes("UTF-8"));
        String type = "multipart/form-data; boundary=" + boundary;

        HttpURLConnection c = post("/parts", type, body.toByteArray(), null);
        assertEquals(200, c.getResponseCode());
        assertEquals("a|null|null|5;file|x.bin|application/octet-stream|6;",
                new String(read(c.getInputStream()), "UTF-8"));

        // A text part is a @RequestParam too; a file part is not.
        c = post("/param", type, body.toByteArray(), null);
        assertEquals("first,null", new String(read(c.getInputStream()), "UTF-8"));
    }

    @Test
    @DisplayName("a urlencoded form is read by param(), after the query string")
    void formFieldsAreParameters() throws Exception {
        start(null);
        HttpURLConnection c = post("/param?b=fromQuery", "application/x-www-form-urlencoded",
                "a=one+two%21&b=fromForm".getBytes("UTF-8"), null);
        assertEquals("one two!,fromQuery", new String(read(c.getInputStream()), "UTF-8"));
    }

    @Test
    @DisplayName("CORS answers an allowed origin's preflight and tags its responses")
    void corsPreflightAndResponses() throws Exception {
        Properties p = new Properties();
        p.setProperty("cn1.cors.allowedOrigins", "http://app.example");
        p.setProperty("cn1.cors.exposedHeaders", "X-Total");
        start(p);
        // A raw socket: HttpURLConnection drops Origin and the
        // Access-Control-Request-* headers as restricted, so through it a
        // preflight never arrives as one.
        String preflight = raw("OPTIONS /text HTTP/1.1\r\nHost: x\r\nOrigin: http://app.example\r\n"
                + "Access-Control-Request-Method: POST\r\n"
                + "Access-Control-Request-Headers: content-type\r\nConnection: close\r\n\r\n");
        assertTrue(preflight.startsWith("HTTP/1.1 204"), preflight);
        assertTrue(preflight.contains("Access-Control-Allow-Origin: http://app.example"), preflight);
        assertTrue(preflight.contains("Access-Control-Allow-Headers: content-type"), preflight);

        String refused = raw("OPTIONS /text HTTP/1.1\r\nHost: x\r\nOrigin: http://evil.example\r\n"
                + "Access-Control-Request-Method: POST\r\nConnection: close\r\n\r\n");
        assertTrue(refused.startsWith("HTTP/1.1 403"), refused);

        String allowed = raw("POST /text HTTP/1.1\r\nHost: x\r\nOrigin: http://app.example\r\n"
                + "Content-Type: text/plain\r\nContent-Length: 2\r\nConnection: close\r\n\r\nhi");
        assertTrue(allowed.startsWith("HTTP/1.1 200"), allowed);
        assertTrue(allowed.contains("Access-Control-Allow-Origin: http://app.example"), allowed);
        assertTrue(allowed.contains("Access-Control-Expose-Headers: X-Total"), allowed);
        assertTrue(allowed.contains("Vary: Origin"), allowed);

        // Refused before any handler runs, as Spring's CorsProcessor refuses it: a
        // simple request needs no preflight, so the handler used to run anyway.
        String other = raw("POST /text HTTP/1.1\r\nHost: x\r\nOrigin: http://evil.example\r\n"
                + "Content-Type: text/plain\r\nContent-Length: 2\r\nConnection: close\r\n\r\nhi");
        assertTrue(other.startsWith("HTTP/1.1 403"), other);
        assertTrue(!other.contains("Access-Control-Allow-Origin"),
                "an origin the policy does not list was allowed to read: " + other);
        // The answer still depends on Origin, so a shared cache must key on it --
        // for a denied origin, for none at all, and for the preflights.
        assertTrue(other.contains("Vary: Origin"), "a denied origin's answer did not vary: " + other);
        String none = raw("POST /text HTTP/1.1\r\nHost: x\r\nContent-Type: text/plain\r\n"
                + "Content-Length: 2\r\nConnection: close\r\n\r\nhi");
        assertTrue(none.contains("Vary: Origin"), "an answer without Origin did not vary: " + none);
        assertTrue(preflight.contains("Vary: Origin"), preflight);
        assertTrue(refused.contains("Vary: Origin"), refused);
    }

    @Test
    @DisplayName("a simple cross-origin request with a method the policy omits is refused")
    void corsEnforcesAllowedMethodsOnSimpleRequests() throws Exception {
        Properties p = new Properties();
        p.setProperty("cn1.cors.allowedOrigins", "http://app.example");
        p.setProperty("cn1.cors.allowedMethods", "GET");
        start(p);
        String post = raw("POST /text HTTP/1.1\r\nHost: x\r\nOrigin: http://app.example\r\n"
                + "Content-Type: text/plain\r\nContent-Length: 2\r\nConnection: close\r\n\r\nhi");
        assertTrue(post.startsWith("HTTP/1.1 403"), "a POST passed a GET-only policy: " + post);
        String preflight = raw("OPTIONS /text HTTP/1.1\r\nHost: x\r\nOrigin: http://app.example\r\n"
                + "Access-Control-Request-Method: DELETE\r\nConnection: close\r\n\r\n");
        assertTrue(preflight.startsWith("HTTP/1.1 403"), preflight);
        String get = raw("GET /text HTTP/1.1\r\nHost: x\r\nOrigin: http://app.example\r\n"
                + "Connection: close\r\n\r\n");
        assertTrue(get.startsWith("HTTP/1.1 200"), get);
        // A same-origin request carries Origin too, and is not a CORS request.
        String same = raw("POST /text HTTP/1.1\r\nHost: 127.0.0.1:" + port + "\r\nOrigin: http://127.0.0.1:"
                + port + "\r\nContent-Type: text/plain\r\nContent-Length: 2\r\nConnection: close\r\n\r\nhi");
        assertTrue(same.startsWith("HTTP/1.1 200"), "a same-origin POST was refused: " + same);
        // The same authority under another scheme is another origin: this server
        // is plain HTTP, so an https page calling it is a CORS request.
        String otherScheme = raw("POST /text HTTP/1.1\r\nHost: 127.0.0.1:" + port + "\r\nOrigin: https://127.0.0.1:"
                + port + "\r\nContent-Type: text/plain\r\nContent-Length: 2\r\nConnection: close\r\n\r\nhi");
        assertTrue(otherScheme.startsWith("HTTP/1.1 403"), "another scheme passed as same-origin: " + otherScheme);
        // Behind a TLS terminating proxy that says so, the https page is the server itself.
        String proxied = raw("POST /text HTTP/1.1\r\nHost: 127.0.0.1:" + port + "\r\nX-Forwarded-Proto: https\r\n"
                + "Origin: https://127.0.0.1:" + port + "\r\nContent-Type: text/plain\r\nContent-Length: 2\r\n"
                + "Connection: close\r\n\r\nhi");
        assertTrue(proxied.startsWith("HTTP/1.1 200"), "a proxied same-origin POST was refused: " + proxied);
    }

    @Test
    @DisplayName("the server's own 404 and 500 carry the CORS headers")
    void fallbackAnswersAreDecorated() throws Exception {
        Properties p = new Properties();
        p.setProperty("cn1.cors.allowedOrigins", "http://app.example");
        start(p);
        String missing = raw("GET /nowhere HTTP/1.1\r\nHost: x\r\nOrigin: http://app.example\r\n"
                + "Connection: close\r\n\r\n");
        assertTrue(missing.startsWith("HTTP/1.1 404"), missing);
        assertTrue(missing.contains("Access-Control-Allow-Origin: http://app.example"), missing);
        String failed = raw("GET /boom HTTP/1.1\r\nHost: x\r\nOrigin: http://app.example\r\n"
                + "Connection: close\r\n\r\n");
        assertTrue(failed.startsWith("HTTP/1.1 500"), failed);
        assertTrue(failed.contains("Access-Control-Allow-Origin: http://app.example"), failed);
    }

    @Test
    @DisplayName("a credentialed preflight against a wildcard method list echoes the method")
    void aCredentialedPreflightEchoesTheMethod() throws Exception {
        Properties p = new Properties();
        p.setProperty("cn1.cors.allowedOrigins", "http://app.example");
        p.setProperty("cn1.cors.allowedMethods", "*");
        p.setProperty("cn1.cors.allowCredentials", "true");
        start(p);
        String preflight = raw("OPTIONS /text HTTP/1.1\r\nHost: x\r\nOrigin: http://app.example\r\n"
                + "Access-Control-Request-Method: PUT\r\nConnection: close\r\n\r\n");
        assertTrue(preflight.startsWith("HTTP/1.1 204"), preflight);
        assertTrue(preflight.contains("Access-Control-Allow-Methods: PUT"), preflight);
        // The echoed method makes the answer depend on that request header.
        assertTrue(preflight.matches("(?s).*Vary: [^\r]*Access-Control-Request-Method.*"), preflight);
    }

    @Test
    @DisplayName("with any origin allowed and no credentials, the answer does not vary by Origin")
    void anyOriginDoesNotVary() throws Exception {
        Properties p = new Properties();
        p.setProperty("cn1.cors.allowedOrigins", "*");
        start(p);
        String answer = raw("POST /text HTTP/1.1\r\nHost: x\r\nOrigin: http://app.example\r\n"
                + "Content-Type: text/plain\r\nContent-Length: 2\r\nConnection: close\r\n\r\nhi");
        assertTrue(answer.contains("Access-Control-Allow-Origin: *"), answer);
        assertTrue(!answer.contains("Vary: Origin"), answer);
        // Without Vary, a copy cached from a request with no Origin must be right
        // for a browser's too.
        String noOrigin = raw("POST /text HTTP/1.1\r\nHost: x\r\n"
                + "Content-Type: text/plain\r\nContent-Length: 2\r\nConnection: close\r\n\r\nhi");
        assertTrue(noOrigin.contains("Access-Control-Allow-Origin: *"), noOrigin);
    }

    @Test
    @DisplayName("a preflight refused for its method varies by the requested method")
    void aRefusedPreflightVariesByMethod() throws Exception {
        Properties p = new Properties();
        p.setProperty("cn1.cors.allowedOrigins", "http://app.example");
        p.setProperty("cn1.cors.allowedMethods", "GET,POST");
        start(p);
        String refused = raw("OPTIONS /text HTTP/1.1\r\nHost: x\r\nOrigin: http://app.example\r\n"
                + "Access-Control-Request-Method: DELETE\r\nConnection: close\r\n\r\n");
        assertTrue(refused.startsWith("HTTP/1.1 403"), refused);
        assertTrue(refused.matches("(?s).*Vary: [^\r]*Access-Control-Request-Method.*"), refused);
    }

    @Test
    @DisplayName("a multipart declaration with no boundary, or endless part headers, is refused")
    void malformedMultipartIsRefused() throws Exception {
        HttpServer.Request noBoundary = new HttpServer.Request("POST", "/x", "HTTP/1.1",
                java.util.Collections.singletonMap("content-type", "multipart/form-data"), null);
        noBoundary.setBody(null, "--x\r\n\r\nhi\r\n--x--".getBytes("UTF-8"));
        assertThrows(IllegalArgumentException.class, noBoundary::getParts,
                "a multipart body with no boundary read as no parts at all");
        // Short header lines, each under the limit, adding up past it.
        StringBuilder headers = new StringBuilder();
        while (headers.length() < 9000) {
            headers.append("X-A: b\r\n");
        }
        HttpServer.Request many = new HttpServer.Request("POST", "/x", "HTTP/1.1",
                java.util.Collections.singletonMap("content-type", "multipart/form-data; boundary=x"),
                null);
        many.setBody(null, ("--x\r\n" + headers + "\r\nhi\r\n--x--").getBytes("UTF-8"));
        assertThrows(IllegalArgumentException.class, many::getParts,
                "the part header block limit is per line, not for the block");
        HttpServer.Request empty = new HttpServer.Request("POST", "/x", "HTTP/1.1",
                java.util.Collections.singletonMap("content-type", "multipart/form-data; boundary=x"),
                null);
        assertThrows(IllegalArgumentException.class, empty::getParts,
                "a multipart body with no bytes, not even its closing delimiter, read as no parts");
        HttpServer.Request huge = new HttpServer.Request("POST", "/x", "HTTP/1.1",
                java.util.Collections.singletonMap("content-type", "text/plain"), null);
        byte[] tooLarge = new byte[8 * 1024 * 1024 + 1];
        IOException refused = assertThrows(IOException.class,
                () -> HttpServer.bodyContent(huge, tooLarge, 0, tooLarge.length),
                "a body past the cap was accepted outside the listener");
        assertEquals(413, HttpServer.refusalStatus(refused));
    }

    @Test
    @DisplayName("a part whose bytes contain the boundary followed by more is not cut there")
    void aBoundaryPrefixInsideAPartIsData() throws Exception {
        HttpServer.Request r = new HttpServer.Request("POST", "/x", "HTTP/1.1",
                java.util.Collections.singletonMap("content-type", "multipart/form-data; boundary=x"),
                null);
        r.setBody(null, ("--x\r\nContent-Disposition: form-data; name=\"f\"\r\n\r\n"
                + "before\r\n--xsuffix after\r\n--x--\r\n").getBytes("UTF-8"));
        java.util.List parts = r.getParts();
        assertEquals(1, parts.size());
        assertEquals("before\r\n--xsuffix after",
                new String(((HttpServer.Part) parts.get(0)).getBytes(), "UTF-8"));
    }

    @Test
    @DisplayName("an allowed origin with a path, query or trailing slash is refused at start")
    void anOriginWithAPathIsRefused() throws Exception {
        for (String bad : new String[] {"https://app.example/", "https://app.example/ui",
                "https://app.example?x=1", "https://app.example#top", "app.example", "https://"}) {
            Properties p = new Properties();
            p.setProperty("cn1.cors.allowedOrigins", bad);
            assertThrows(IOException.class, () -> Cors.fromConfig(Config.of(p, "test")),
                    "accepted " + bad + ", which no browser Origin can match");
        }
        Properties ok = new Properties();
        ok.setProperty("cn1.cors.allowedOrigins", "https://app.example, http://127.0.0.1:8080, *");
        assertTrue(Cors.fromConfig(Config.of(ok, "test")) != null);
    }

    @Test
    @DisplayName("an explicit host(null) binds every interface over a configured address")
    void anExplicitWildcardHostBeatsTheConfiguredAddress() throws Exception {
        // 203.0.113.1 is TEST-NET-3: no machine has it, so binding it fails. The
        // configured address is used only when the builder named no host.
        Properties p = new Properties();
        p.setProperty(Config.SERVER_PORT, String.valueOf(freePort()));
        p.setProperty(Config.SERVER_ADDRESS, "203.0.113.1");
        HttpServer.Handler none = new HttpServer.Handler() {
            public HttpServer.Response handle(HttpServer.Request request) {
                return null;
            }
        };
        assertThrows(Exception.class, () -> Backend.builder(Config.of(p, "test")).quiet().handler(none)
                .start().stop(), "the configured address was not used when no host was named");
        backend = Backend.builder(Config.of(p, "test")).quiet().handler(none).host(null).start();
    }

    @Test
    @DisplayName("without cn1.cors.allowedOrigins there is no CORS at all")
    void corsIsOffByDefault() throws Exception {
        start(null);
        String answer = raw("POST /text HTTP/1.1\r\nHost: x\r\nOrigin: http://app.example\r\n"
                + "Content-Type: text/plain\r\nContent-Length: 2\r\nConnection: close\r\n\r\nhi");
        assertTrue(answer.startsWith("HTTP/1.1 200"), answer);
        assertTrue(!answer.contains("Access-Control-Allow-Origin"), answer);
    }

    private String raw(String request) throws IOException {
        java.net.Socket socket = new java.net.Socket("127.0.0.1", port);
        try {
            socket.setSoTimeout(10000);
            socket.getOutputStream().write(request.getBytes("UTF-8"));
            socket.getOutputStream().flush();
            return new String(read(socket.getInputStream()), "UTF-8");
        } finally {
            socket.close();
        }
    }

    @Test
    @DisplayName("compression gzips a large JSON answer for a client that accepts it")
    void compressionWhenAccepted() throws Exception {
        Properties p = new Properties();
        p.setProperty("cn1.server.compression.enabled", "true");
        start(p);
        HttpURLConnection c = (HttpURLConnection) new URL("http://127.0.0.1:" + port + "/big")
                .openConnection();
        c.setRequestProperty("Accept-Encoding", "gzip");
        assertEquals(200, c.getResponseCode());
        assertEquals("gzip", c.getHeaderField("Content-Encoding"));
        assertTrue(c.getHeaderField("Vary").contains("Accept-Encoding"));
        String json = new String(read(new GZIPInputStream(c.getInputStream())), "UTF-8");
        assertTrue(json.startsWith("[{\"n\":0}"), json);

        c = (HttpURLConnection) new URL("http://127.0.0.1:" + port + "/big").openConnection();
        c.setRequestProperty("Accept-Encoding", "gzip;q=0, identity");
        assertNull(c.getHeaderField("Content-Encoding"), "q=0 refuses gzip");
        // The identity answer was chosen by Accept-Encoding too, so it varies by it.
        assertTrue(String.valueOf(c.getHeaderField("Vary")).contains("Accept-Encoding"),
                "an uncompressed answer a gzip client would get compressed must vary");

        c = (HttpURLConnection) new URL("http://127.0.0.1:" + port + "/text").openConnection();
        c.setRequestProperty("Accept-Encoding", "gzip");
        assertNull(c.getHeaderField("Content-Encoding"), "a small body was compressed");

        // An explicit gzip entry decides, wherever it sits; * counts only otherwise.
        assertEquals(null, encodingFor("*;q=1, gzip;q=0"), "gzip was refused explicitly");
        assertEquals("gzip", encodingFor("*;q=0, gzip"), "gzip was accepted explicitly");
        assertEquals("gzip", encodingFor("br, *"), "* accepts gzip when it is not named");
    }

    @Test
    @DisplayName("a close delimiter followed by anything but padding and CRLF is malformed")
    void aCloseDelimiterMustEnd() throws Exception {
        String part = "--x\r\nContent-Disposition: form-data; name=\"f\"\r\n\r\nv\r\n";
        assertThrows(IllegalArgumentException.class,
                () -> Multipart.parse((part + "--x--garbage").getBytes("UTF-8"), "x"));
        assertThrows(IllegalArgumentException.class,
                () -> Multipart.parse("--x--garbage".getBytes("UTF-8"), "x"));
        assertEquals(1, Multipart.parse((part + "--x--").getBytes("UTF-8"), "x").size());
        assertEquals(1, Multipart.parse((part + "--x-- \r\nan epilogue").getBytes("UTF-8"), "x")
                .size(), "padding, CRLF and an epilogue may follow");
    }

    @Test
    @DisplayName("an unterminated quoted boundary or field name is malformed")
    void anUnterminatedQuoteIsMalformed() throws Exception {
        assertNull(Multipart.boundary("multipart/form-data; boundary=\"b"), "boundary=\"b read as b");
        assertEquals("b", Multipart.boundary("multipart/form-data; boundary=\"b\""));
        assertThrows(IllegalArgumentException.class,
                () -> Multipart.parse("--x\r\nContent-Disposition: form-data; name=\"f\r\n\r\nv\r\n--x--".getBytes("UTF-8"),
                        "x"), "an unterminated field name was accepted");
    }

    @Test
    @DisplayName("a form part without a form-data disposition or a field name is malformed")
    void aFormPartMustNameItsField() throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> Multipart.parse("--x\r\nContent-Type: text/plain\r\n\r\nv\r\n--x--".getBytes("UTF-8"), "x"),
                "a part with no Content-Disposition was accepted");
        assertThrows(IllegalArgumentException.class,
                () -> Multipart.parse(("--x\r\nContent-Disposition: attachment; name=\"f\"\r\n\r\nv\r\n--x--")
                        .getBytes("UTF-8"), "x"), "an attachment disposition was accepted");
        assertThrows(IllegalArgumentException.class,
                () -> Multipart.parse("--x\r\nContent-Disposition: form-data\r\n\r\nv\r\n--x--".getBytes("UTF-8"),
                        "x"), "a part naming no field was accepted");
        assertEquals(1, Multipart.parse(("--x\r\nContent-Disposition: Form-Data; name=\"f\"\r\n\r\nv\r\n--x--")
                .getBytes("UTF-8"), "x").size(), "the disposition type is case-insensitive");
    }

    @Test
    @DisplayName("the opening delimiter must start a line, and preamble text is skipped")
    void theOpeningDelimiterStartsALine() throws Exception {
        String part = "Content-Disposition: form-data; name=\"f\"\r\n\r\nv\r\n--x--";
        // The only delimiter is mid-line: nothing opens the body.
        assertThrows(IllegalArgumentException.class,
                () -> Multipart.parse(("prefix--x\r\nContent-Disposition: form-data; name=\"f\"\r\n\r\nv")
                        .getBytes("UTF-8"), "x"),
                "a delimiter in the middle of a line was taken as the opening");
        // A preamble that mentions the boundary mid-line, before the real one.
        java.util.List parts = Multipart.parse(("a preamble naming --x inline\r\n--x\r\n" + part)
                .getBytes("UTF-8"), "x");
        assertEquals(1, parts.size());
    }

    @Test
    @DisplayName("a boundary search over a body of dashes is linear")
    void aDashBoundaryOverDashesIsLinear() throws Exception {
        StringBuilder boundary = new StringBuilder();
        while (boundary.length() < 69) {
            boundary.append('-');
        }
        boundary.append('z');
        byte[] body = new byte[8 * 1024 * 1024];
        java.util.Arrays.fill(body, (byte) '-');
        long started = System.nanoTime();
        assertThrows(IllegalArgumentException.class,
                () -> Multipart.parse(body, boundary.toString()));
        long ms = (System.nanoTime() - started) / 1000000L;
        assertTrue(ms < 3000, "searching 8 MB of dashes took " + ms + "ms");
    }

    @Test
    @DisplayName("a form value that is not UTF-8 once decoded is refused")
    void aFormValueMustDecodeToUtf8() {
        assertThrows(IllegalStateException.class, () -> Multipart.formValue("name=%FF", "name"));
        assertEquals("\u00e9", Multipart.formValue("name=%C3%A9", "name"));
    }

    @Test
    @DisplayName("a form value keeps a supplementary character beside + and %")
    void aFormValueKeepsSupplementaryCharacters() {
        String emoji = "\ud83d\ude00";
        assertEquals(emoji + " ok%", Multipart.formValue("v=" + emoji + "+ok%25", "v"));
    }

    @Test
    @DisplayName("a Vary the handler gave as a list is extended, not stringified")
    void aListValuedVaryIsJoined() {
        java.util.Map headers = new java.util.LinkedHashMap();
        headers.put("Vary", java.util.Arrays.asList("Origin", "Cookie"));
        HttpServer.Response r = HttpServer.Response.empty(200, "text/plain", headers);
        r.appendToken("Vary", "Accept-Encoding");
        assertEquals("Origin, Cookie, Accept-Encoding", r.headerValue("Vary"));
        r.appendToken("vary", "origin");
        assertEquals("Origin, Cookie, Accept-Encoding", r.headerValue("Vary"),
                "a token already listed was added again");
    }

    @Test
    @DisplayName("a gzip body of several members is decoded whole, and trailing junk refused")
    void everyGzipMemberIsDecoded() throws Exception {
        java.io.ByteArrayOutputStream both = new java.io.ByteArrayOutputStream();
        both.write(gzip("first ".getBytes("UTF-8")));
        both.write(gzip("second".getBytes("UTF-8")));
        byte[] data = both.toByteArray();
        assertEquals("first second", new String(HttpServer.gunzip(data, 0, data.length), "UTF-8"));
        java.io.ByteArrayOutputStream junk = new java.io.ByteArrayOutputStream();
        junk.write(gzip("first".getBytes("UTF-8")));
        junk.write("not gzip".getBytes("UTF-8"));
        byte[] bad = junk.toByteArray();
        assertThrows(IOException.class, () -> HttpServer.gunzip(bad, 0, bad.length),
                "bytes after the last member were ignored");
    }

    private static byte[] gzip(byte[] data) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        GZIPOutputStream gz = new GZIPOutputStream(out);
        gz.write(data);
        gz.close();
        return out.toByteArray();
    }

    @Test
    @DisplayName("a strong ETag keeps its response uncompressed; a weak one does not")
    void aStrongETagIsNotCompressed() throws Exception {
        Properties p = new Properties();
        p.setProperty("cn1.server.compression.enabled", "true");
        start(p);
        HttpURLConnection c = (HttpURLConnection) new URL("http://127.0.0.1:" + port + "/etag")
                .openConnection();
        c.setRequestProperty("Accept-Encoding", "gzip");
        assertNull(c.getHeaderField("Content-Encoding"), "a strong ETag was compressed");
        assertEquals("\"v1\"", c.getHeaderField("ETag"));
        read(c.getInputStream());
        c = (HttpURLConnection) new URL("http://127.0.0.1:" + port + "/etag-weak").openConnection();
        c.setRequestProperty("Accept-Encoding", "gzip");
        assertEquals("gzip", c.getHeaderField("Content-Encoding"));
        read(new GZIPInputStream(c.getInputStream()));
    }

    @Test
    @DisplayName("a range answer is not compressed, so its Content-Range stays true")
    void aRangeIsNotCompressed() throws Exception {
        Properties p = new Properties();
        p.setProperty("cn1.server.compression.enabled", "true");
        start(p);
        HttpURLConnection c = (HttpURLConnection) new URL("http://127.0.0.1:" + port + "/range")
                .openConnection();
        c.setRequestProperty("Accept-Encoding", "gzip");
        assertEquals(206, c.getResponseCode());
        assertNull(c.getHeaderField("Content-Encoding"), "a 206 was compressed");
        assertEquals("bytes 0-4999/20000", c.getHeaderField("Content-Range"));
        assertEquals(5000, read(c.getInputStream()).length);
    }

    @Test
    @DisplayName("a Response the handler shares between requests is not changed by one of them")
    void aSharedResponseIsCopiedBeforeThePolicies() throws Exception {
        Properties p = new Properties();
        p.setProperty("cn1.server.compression.enabled", "true");
        start(p);
        HttpURLConnection c = (HttpURLConnection) new URL("http://127.0.0.1:" + port + "/shared")
                .openConnection();
        c.setRequestProperty("Accept-Encoding", "gzip");
        assertEquals("gzip", c.getHeaderField("Content-Encoding"));
        read(new GZIPInputStream(c.getInputStream()));
        // The next client never offered gzip. Compression used to rewrite the
        // shared object, so it got gzip anyway.
        c = (HttpURLConnection) new URL("http://127.0.0.1:" + port + "/shared").openConnection();
        c.setRequestProperty("Accept-Encoding", "identity");
        assertNull(c.getHeaderField("Content-Encoding"), "the shared response stayed gzipped");
        String json = new String(read(c.getInputStream()), "UTF-8");
        assertTrue(json.startsWith("[{\"n\":0}"), json);
    }

    private String encodingFor(String acceptEncoding) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL("http://127.0.0.1:" + port + "/big")
                .openConnection();
        c.setRequestProperty("Accept-Encoding", acceptEncoding);
        String encoding = c.getHeaderField("Content-Encoding");
        read(c.getInputStream());
        return encoding;
    }

    private HttpURLConnection post(String path, String type, byte[] body, String encoding)
            throws IOException {
        return post(path, type, body, encoding, null);
    }

    private HttpURLConnection post(String path, String type, byte[] body, String encoding,
                                   String origin) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL("http://127.0.0.1:" + port + path)
                .openConnection();
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", type);
        if (encoding != null) {
            c.setRequestProperty("Content-Encoding", encoding);
        }
        if (origin != null) {
            c.setRequestProperty("Origin", origin);
        }
        OutputStream out = c.getOutputStream();
        out.write(body);
        out.close();
        return c;
    }

    private static byte[] gzip(String text) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gz = new GZIPOutputStream(out);
        gz.write(text.getBytes("UTF-8"));
        gz.close();
        return out.toByteArray();
    }

    private static byte[] read(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] b = new byte[4096];
        int n;
        while ((n = in.read(b)) > 0) {
            out.write(b, 0, n);
        }
        in.close();
        return out.toByteArray();
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
