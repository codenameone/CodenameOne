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

        String other = raw("POST /text HTTP/1.1\r\nHost: x\r\nOrigin: http://evil.example\r\n"
                + "Content-Type: text/plain\r\nContent-Length: 2\r\nConnection: close\r\n\r\nhi");
        assertTrue(other.startsWith("HTTP/1.1 200"), other);
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
    @DisplayName("with any origin allowed and no credentials, the answer does not vary by Origin")
    void anyOriginDoesNotVary() throws Exception {
        Properties p = new Properties();
        p.setProperty("cn1.cors.allowedOrigins", "*");
        start(p);
        String answer = raw("POST /text HTTP/1.1\r\nHost: x\r\nOrigin: http://app.example\r\n"
                + "Content-Type: text/plain\r\nContent-Length: 2\r\nConnection: close\r\n\r\nhi");
        assertTrue(answer.contains("Access-Control-Allow-Origin: *"), answer);
        assertTrue(!answer.contains("Vary: Origin"), answer);
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

        c = (HttpURLConnection) new URL("http://127.0.0.1:" + port + "/text").openConnection();
        c.setRequestProperty("Accept-Encoding", "gzip");
        assertNull(c.getHeaderField("Content-Encoding"), "a small body was compressed");

        // An explicit gzip entry decides, wherever it sits; * counts only otherwise.
        assertEquals(null, encodingFor("*;q=1, gzip;q=0"), "gzip was refused explicitly");
        assertEquals("gzip", encodingFor("*;q=0, gzip"), "gzip was accepted explicitly");
        assertEquals("gzip", encodingFor("br, *"), "* accepts gzip when it is not named");
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
