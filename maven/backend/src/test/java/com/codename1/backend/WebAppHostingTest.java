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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The server hosting the browser build of its own application.
 *
 * <p>Real servers on real ports again: what is being promised is what a browser
 * receives -- which file, under which headers, in which encoding -- and none of
 * that exists short of a response on a socket.
 */
class WebAppHostingTest {

    /** The port of the server the running test started. */
    private int port;

    @Test
    @DisplayName("the app is served at the root, index first, revalidated every time")
    void servesTheAppAtTheRoot(@TempDir File dir) throws Exception {
        write(new File(dir, "index.html"), "<html>the app</html>");
        write(new File(dir, "translated_app.js"), "run();");
        new File(dir, "js").mkdir();
        write(new File(dir, "js/fontmetrics.js"), "metrics();");
        Backend backend = start(dir, null, null);
        try {
            Reply index = get(port, "/", null, null);
            assertEquals(200, index.status);
            assertEquals("<html>the app</html>", index.text());
            assertTrue(index.type.startsWith("text/html"), index.type);
            // The files of a build keep their names, so a browser has to ask.
            assertEquals("no-cache", index.cacheControl);
            assertEquals("run();", get(port, "/translated_app.js", null, null).text());
            assertEquals("metrics();", get(port, "/js/fontmetrics.js", null, null).text());
            assertEquals(404, get(port, "/nothing-here.js", null, null).status);
        } finally {
            backend.stop();
        }
    }

    @Test
    @DisplayName("an unchanged file answers 304 to the validator it was served with")
    void revalidatesWithAnEntityTag(@TempDir File dir) throws Exception {
        write(new File(dir, "index.html"), "<html>the app</html>");
        Backend backend = start(dir, null, null);
        try {
            Reply first = get(port, "/index.html", null, null);
            assertTrue(first.etag != null && first.etag.length() > 0, "no ETag on a revalidated file");
            Reply again = get(port, "/index.html", null, first.etag);
            assertEquals(304, again.status);
        } finally {
            backend.stop();
        }
    }

    @Test
    @DisplayName("a route answers before the app does, so the API keeps its paths")
    void routesWinOverTheApp(@TempDir File dir) throws Exception {
        write(new File(dir, "index.html"), "<html>the app</html>");
        new File(dir, "api").mkdir();
        write(new File(dir, "api/rides"), "from the file");
        Properties settings = settings(dir, null, null);
        Backend backend = Backend.builder(Config.of(settings, "test"))
                .quiet()
                .handler(new HttpServer.Handler() {
                    public HttpServer.Response handle(HttpServer.Request request) throws Exception {
                        if ("/api/rides".equals(request.getTarget())) {
                            return new HttpServer.Response(200, "text/plain",
                                    "from the handler".getBytes("UTF-8"));
                        }
                        return null;
                    }
                })
                .start();
        try {
            assertEquals("from the handler", get(port, "/api/rides", null, null).text());
            assertEquals("<html>the app</html>", get(port, "/", null, null).text());
        } finally {
            backend.stop();
        }
    }

    @Test
    @DisplayName("a compressed copy beside a file is what a browser that accepts gzip gets")
    void servesThePrecompressedCopy(@TempDir File dir) throws Exception {
        StringBuilder script = new StringBuilder();
        for (int i = 0; i < 2000; i++) {
            script.append("function f").append(i).append("(){return ").append(i).append(";}\n");
        }
        write(new File(dir, "index.html"), "<html>the app</html>");
        write(new File(dir, "translated_app.js"), script.toString());
        writeGzip(new File(dir, "translated_app.js.gz"), script.toString());
        long packed = new File(dir, "translated_app.js.gz").length();
        Backend backend = start(dir, null, null);
        try {
            Reply gzip = get(port, "/translated_app.js", "gzip, deflate, br", null);
            assertEquals(200, gzip.status);
            assertEquals("gzip", gzip.encoding);
            assertEquals(packed, gzip.body.length);
            assertEquals(script.toString(), gunzip(gzip.body));
            // Named for what it IS, not for the bytes on the wire.
            assertTrue(gzip.type.indexOf("javascript") >= 0, gzip.type);
            assertTrue(gzip.vary != null && gzip.vary.indexOf("Accept-Encoding") >= 0, "Vary: " + gzip.vary);

            Reply plain = get(port, "/translated_app.js", "identity", null);
            assertNull(plain.encoding);
            assertEquals(script.toString(), plain.text());
            assertTrue(plain.vary != null && plain.vary.indexOf("Accept-Encoding") >= 0, "Vary: " + plain.vary);
            // Two representations of one URL: a validator for one must not
            // answer 304 for the other.
            assertNotEquals(gzip.etag, plain.etag);

            // No copy beside it: the file itself, whatever was accepted.
            Reply index = get(port, "/", "gzip", null);
            assertNull(index.encoding);
            assertEquals("<html>the app</html>", index.text());

            // The copy is not a page of its own under a guessed type.
            Reply refused = get(port, "/translated_app.js", "gzip;q=0", null);
            assertNull(refused.encoding);
            assertEquals(script.toString(), refused.text());
        } finally {
            backend.stop();
        }
    }

    @Test
    @DisplayName("a byte range is of the file, never of its compressed copy")
    void aRangeIsOfTheFileItself(@TempDir File dir) throws Exception {
        write(new File(dir, "index.html"), "<html>the app</html>");
        write(new File(dir, "data.json"), "0123456789");
        writeGzip(new File(dir, "data.json.gz"), "0123456789");
        Backend backend = start(dir, null, null);
        try {
            HttpURLConnection connection = open(port, "/data.json");
            connection.setRequestProperty("Accept-Encoding", "gzip");
            connection.setRequestProperty("Range", "bytes=2-5");
            try {
                assertEquals(206, connection.getResponseCode());
                assertNull(connection.getHeaderField("Content-Encoding"));
                assertEquals("2345", new String(readAll(connection.getInputStream()), "UTF-8"));
            } finally {
                connection.disconnect();
            }
        } finally {
            backend.stop();
        }
    }

    @Test
    @DisplayName("a path cannot climb out of the app's directory")
    void cannotEscapeTheDirectory(@TempDir File dir) throws Exception {
        File root = new File(dir, "webapp");
        root.mkdir();
        write(new File(root, "index.html"), "<html>the app</html>");
        write(new File(dir, "secret.txt"), "not for the browser");
        writeGzip(new File(dir, "secret.txt.gz"), "not for the browser");
        Backend backend = start(root, null, null);
        try {
            String[] attempts = {
                "/../secret.txt", "/%2e%2e/secret.txt", "/..%2fsecret.txt",
                "/js/../../secret.txt", "/%2e%2e%2fsecret.txt"
            };
            for (String attempt : attempts) {
                Reply reply = raw(port, attempt, "gzip");
                assertTrue(reply.status == 400 || reply.status == 403 || reply.status == 404,
                        attempt + " answered " + reply.status);
                assertTrue(reply.text().indexOf("not for the browser") < 0, attempt);
            }
        } finally {
            backend.stop();
        }
    }

    @Test
    @DisplayName("it can be mounted beneath a path and given a cache policy")
    void mountsBeneathAPath(@TempDir File dir) throws Exception {
        write(new File(dir, "index.html"), "<html>the app</html>");
        write(new File(dir, "port.js"), "port();");
        Backend backend = start(dir, "/app", "public, max-age=60");
        try {
            Reply script = get(port, "/app/port.js", null, null);
            assertEquals("port();", script.text());
            assertEquals("public, max-age=60", script.cacheControl);
            assertEquals("<html>the app</html>", get(port, "/app/", null, null).text());
            assertEquals(404, get(port, "/port.js", null, null).status);
        } finally {
            backend.stop();
        }
    }

    @Test
    @DisplayName("a server with no web build serves none, and says nothing")
    void absentByDefault() throws Exception {
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(freePort()));
        assertNull(Backend.webApp(Config.of(settings, "test")));
    }

    @Test
    @DisplayName("naming a directory that holds no app stops the server from starting")
    void aNamedDirectoryMustHoldTheApp(@TempDir File dir) throws Exception {
        final Properties settings = settings(dir, null, null);
        IOException refused = assertThrows(IOException.class, () ->
                Backend.builder(Config.of(settings, "test")).quiet().handler(nothing()).start());
        assertTrue(refused.getMessage().indexOf(Config.WEBAPP_ROOT) >= 0, refused.getMessage());
        assertTrue(refused.getMessage().indexOf("index.html") >= 0, refused.getMessage());
    }

    @Test
    @DisplayName("it can be switched off without moving the directory")
    void canBeSwitchedOff(@TempDir File dir) throws Exception {
        write(new File(dir, "index.html"), "<html>the app</html>");
        Properties settings = settings(dir, null, null);
        settings.setProperty(Config.WEBAPP_ENABLED, "false");
        assertNull(Backend.webApp(Config.of(settings, "test")));
    }

    private Backend start(File root, String path, String cacheControl) throws Exception {
        return Backend.builder(Config.of(settings(root, path, cacheControl), "test"))
                .quiet().handler(nothing()).start();
    }

    private Properties settings(File root, String path, String cacheControl) throws IOException {
        Properties settings = new Properties();
        port = freePort();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        settings.setProperty(Config.WEBAPP_ROOT, root.getAbsolutePath());
        if (path != null) {
            settings.setProperty(Config.WEBAPP_PATH, path);
        }
        if (cacheControl != null) {
            settings.setProperty(Config.WEBAPP_CACHE_CONTROL, cacheControl);
        }
        return settings;
    }

    private static HttpServer.Handler nothing() {
        return new HttpServer.Handler() {
            public HttpServer.Response handle(HttpServer.Request request) throws Exception {
                return null;
            }
        };
    }

    /** What came back, with the headers these tests are about. */
    private static final class Reply {
        int status;
        String type;
        String encoding;
        String cacheControl;
        String etag;
        String vary;
        byte[] body = new byte[0];

        String text() throws IOException {
            return new String(body, "UTF-8");
        }
    }

    private static HttpURLConnection open(int port, String path) throws IOException {
        HttpURLConnection connection = (HttpURLConnection)
                new URL("http://127.0.0.1:" + port + path).openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        return connection;
    }

    private static Reply get(int port, String path, String acceptEncoding, String ifNoneMatch)
            throws IOException {
        HttpURLConnection connection = open(port, path);
        if (acceptEncoding != null) {
            connection.setRequestProperty("Accept-Encoding", acceptEncoding);
        }
        if (ifNoneMatch != null) {
            connection.setRequestProperty("If-None-Match", ifNoneMatch);
        }
        try {
            Reply reply = new Reply();
            reply.status = connection.getResponseCode();
            reply.type = connection.getHeaderField("Content-Type");
            reply.encoding = connection.getHeaderField("Content-Encoding");
            reply.cacheControl = connection.getHeaderField("Cache-Control");
            reply.etag = connection.getHeaderField("ETag");
            reply.vary = connection.getHeaderField("Vary");
            InputStream in = reply.status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            if (in != null) {
                reply.body = readAll(in);
            }
            return reply;
        } finally {
            connection.disconnect();
        }
    }

    /**
     * A request whose target reaches the server exactly as written: a URL class
     * would tidy the dot segments away before the server ever saw them.
     */
    private static Reply raw(int port, String target, String acceptEncoding) throws IOException {
        java.net.Socket socket = new java.net.Socket("127.0.0.1", port);
        try {
            socket.setSoTimeout(5000);
            OutputStream out = socket.getOutputStream();
            out.write(("GET " + target + " HTTP/1.1\r\nHost: 127.0.0.1\r\nAccept-Encoding: "
                    + acceptEncoding + "\r\nConnection: close\r\n\r\n").getBytes("ISO-8859-1"));
            out.flush();
            byte[] all = readAll(socket.getInputStream());
            String head = new String(all, "ISO-8859-1");
            Reply reply = new Reply();
            reply.status = Integer.parseInt(head.substring(9, 12));
            reply.body = all;
            return reply;
        } finally {
            socket.close();
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read = in.read(chunk);
            while (read >= 0) {
                out.write(chunk, 0, read);
                read = in.read(chunk);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }

    private static String gunzip(byte[] packed) throws IOException {
        return new String(readAll(new GZIPInputStream(new ByteArrayInputStream(packed))), "UTF-8");
    }

    private static int freePort() throws IOException {
        java.net.ServerSocket probe = new java.net.ServerSocket(0);
        try {
            return probe.getLocalPort();
        } finally {
            probe.close();
        }
    }

    private static void write(File file, String content) throws IOException {
        OutputStream out = new FileOutputStream(file);
        try {
            out.write(content.getBytes("UTF-8"));
        } finally {
            out.close();
        }
    }

    private static void writeGzip(File file, String content) throws IOException {
        OutputStream out = new GZIPOutputStream(new FileOutputStream(file));
        try {
            out.write(content.getBytes("UTF-8"));
        } finally {
            out.close();
        }
    }
}
