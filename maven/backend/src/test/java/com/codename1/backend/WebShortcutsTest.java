/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.backend.web.MappedInterceptor;
import com.codename1.backend.web.HandlerInterceptor;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.file.Files;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class WebShortcutsTest {
    @Test
    void interceptorPatternsMatchAsSpringsDo() {
        MappedInterceptor m = new MappedInterceptor(new HandlerInterceptor() { })
                .addPathPatterns("/api/**", "/files/*.txt")
                .excludePathPatterns("/api/health");
        assertTrue(m.matches("/api"));
        assertTrue(m.matches("/api/users/4"));
        assertFalse(m.matches("/apix"));
        assertFalse(m.matches("/api/health"));
        assertTrue(m.matches("/files/a.txt"));
        assertFalse(m.matches("/files/a/b.txt"));
        assertFalse(m.matches("/other"));
        assertTrue(new MappedInterceptor(new HandlerInterceptor() { }).matches("/anything"));
    }

    @Test
    void viewControllersAndSeveralStaticRootsAnswer() throws Exception {
        File first = Files.createTempDirectory("cn1-static-a").toFile();
        File second = Files.createTempDirectory("cn1-static-b").toFile();
        write(new File(first, "both.txt"), "first");
        write(new File(second, "both.txt"), "second");
        write(new File(second, "only.txt"), "only second");
        final HttpServer.Handler files = StaticFiles.firstOf(
                new StaticFiles(first.getPath(), "/s", null, null),
                new StaticFiles(second.getPath(), "/s", null, null));
        final ViewControllers shortcuts = new ViewControllers()
                .redirect("/docs", "/docs/index.html").status("/favicon.ico", 204);
        ServerSocket socket = new ServerSocket(0);
        int port = socket.getLocalPort();
        socket.close();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        Backend b = Backend.builder(Config.of(settings, "test")).quiet().handler(
                new HttpServer.Handler() {
                    public HttpServer.Response handle(HttpServer.Request request) throws Exception {
                        HttpServer.Response r = shortcuts.handle(request);
                        return r != null ? r : files.handle(request);
                    }
                }).start();
        try {
            assertEquals("first", get(port, "/s/both.txt"));
            assertEquals("only second", get(port, "/s/only.txt"));
            HttpURLConnection c = open(port, "/s/none.txt");
            assertEquals(404, c.getResponseCode());
            c = open(port, "/docs");
            c.setInstanceFollowRedirects(false);
            assertEquals(302, c.getResponseCode());
            assertEquals("/docs/index.html", c.getHeaderField("Location"));
            assertEquals(204, open(port, "/favicon.ico").getResponseCode());
        } finally {
            b.stop();
        }
    }

    private static void write(File f, String text) throws Exception {
        FileOutputStream out = new FileOutputStream(f);
        out.write(text.getBytes("UTF-8"));
        out.close();
    }

    private static HttpURLConnection open(int port, String path) throws Exception {
        return (HttpURLConnection) new URL("http://127.0.0.1:" + port + path).openConnection();
    }

    private static String get(int port, String path) throws Exception {
        HttpURLConnection c = open(port, path);
        assertEquals(200, c.getResponseCode());
        InputStream in = c.getInputStream();
        byte[] body = new byte[256];
        int n = in.read(body);
        in.close();
        return new String(body, 0, n, "UTF-8");
    }
}
