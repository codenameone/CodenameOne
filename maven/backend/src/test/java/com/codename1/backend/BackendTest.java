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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;

import com.codename1.backend.sql.Dialect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The entry point every server now shares.
 *
 * <p>These start real servers on real ports and talk HTTP to them, because the
 * builder's job is an ORDER of operations -- resolve the configuration, open the
 * database, build the handlers that need it, bind, drain -- and each of those
 * steps only means something in terms of the next one.
 */
class BackendTest {

    @Test
    @DisplayName("a server comes up on the configured port and answers")
    void servesOnTheConfiguredPort() throws Exception {
        int port = freePort();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        Backend backend = Backend.builder(Config.of(settings, "test"))
                .quiet()
                .handler(new HttpServer.Handler() {
                    public HttpServer.Response handle(HttpServer.Request request) throws Exception {
                        return new HttpServer.Response(200, "text/plain",
                                "ok".getBytes("UTF-8"));
                    }
                })
                .start();
        try {
            assertEquals("ok", get(port, "/anything"));
        } finally {
            backend.stop();
        }
    }

    @Test
    @DisplayName("handlers are tried in order and static files come last")
    void staticFilesCannotShadowARoute(@TempDir File dir) throws Exception {
        // A file named like a route would otherwise decide which of the two
        // answers by what happens to be in a directory.
        write(new File(dir, "hello.txt"), "from the file");
        int port = freePort();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        settings.setProperty(Config.STATIC_ROOT, dir.getAbsolutePath());
        settings.setProperty(Config.STATIC_PREFIX, "/files");
        Backend backend = Backend.builder(Config.of(settings, "test"))
                .quiet()
                .handler(new HttpServer.Handler() {
                    public HttpServer.Response handle(HttpServer.Request request) throws Exception {
                        if("/files/hello.txt".equals(request.getTarget())) {
                            return new HttpServer.Response(200, "text/plain",
                                    "from the handler".getBytes("UTF-8"));
                        }
                        return null;
                    }
                })
                .start();
        try {
            assertEquals("from the handler", get(port, "/files/hello.txt"));
        } finally {
            backend.stop();
        }
    }

    @Test
    @DisplayName("a server with no database opens none")
    void opensNoDatabaseWhenThereIsNothingToOpen() throws Exception {
        int port = freePort();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        Backend backend = Backend.builder(Config.of(settings, "test"))
                .quiet()
                .handler(ok())
                .start();
        try {
            assertNull(backend.getDataSource(), "nothing asked for a database");
        } finally {
            backend.stop();
        }
    }

    @Test
    @DisplayName("the configured database is opened and handed to the handlers")
    void opensTheConfiguredDatabase() throws Exception {
        int port = freePort();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        settings.setProperty(Config.DATASOURCE_URL, ":memory:");
        final DataSource[] seen = new DataSource[1];
        Backend backend = Backend.builder(Config.of(settings, "test"))
                .quiet()
                .handlers(new Backend.Handlers() {
                    public HttpServer.Handler[] create(DataSource dataSource,
                            com.codename1.backend.orm.EntityManager entities) {
                        // The hook exists because a handler that talks to a
                        // database cannot be constructed before the pool is open.
                        seen[0] = dataSource;
                        return new HttpServer.Handler[] {ok()};
                    }
                })
                .start();
        try {
            assertNotNull(backend.getDataSource());
            assertEquals(Dialect.SQLITE, backend.getDataSource().dialect());
            assertEquals(backend.getDataSource(), seen[0]);
            assertEquals("ok", get(port, "/"));
        } finally {
            backend.stop();
        }
        // Stopping the server closes the pool it opened.
        assertThrows(IOException.class, () -> seen[0].borrow());
    }

    @Test
    @DisplayName("code wins over configuration, so a port written here is the port")
    void explicitSettingsWin() throws Exception {
        int configured = freePort();
        int explicit = freePort();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(configured));
        Backend backend = Backend.builder(Config.of(settings, "test"))
                .quiet()
                .port(explicit)
                .handler(ok())
                .start();
        try {
            assertEquals("ok", get(explicit, "/"));
        } finally {
            backend.stop();
        }
    }

    @Test
    @DisplayName("a server with no handlers is refused rather than started")
    void refusesAServerThatWouldAnswerNothing() {
        // Every request would be a 404, which is not a server anybody meant to
        // deploy.
        IOException err = assertThrows(IOException.class,
                () -> Backend.builder(Config.of(new Properties(), "test")).quiet().start());
        assertTrue(err.getMessage().contains("no handlers"), err.getMessage());
    }

    @Test
    @DisplayName("TLS configured by halves is refused")
    void refusesHalfConfiguredTls() {
        // A deployment that believes it is serving TLS and is serving plaintext
        // on the port a browser reaches over https.
        Properties settings = new Properties();
        settings.setProperty(Config.TLS_CERTIFICATE, "/nonexistent.pem");
        IOException err = assertThrows(IOException.class,
                () -> Backend.builder(Config.of(settings, "test")).quiet().handler(ok()).start());
        assertTrue(err.getMessage().contains(Config.TLS_KEY), err.getMessage());
    }

    @Test
    @DisplayName("a server that cannot wait for a stop signal refuses to start")
    void refusesWhenTheShutdownHandlerCannotBeInstalled() throws Exception {
        // installShutdownHandler answers false when the self-pipe or the
        // sigaction cannot be set up -- under descriptor exhaustion, say. Ignored,
        // the shutdown watcher reads its signal immediately, stops the server it
        // just started and exits 0: a container that never served a request,
        // reporting success.
        int port = freePort();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        Backend.Builder builder = Backend.builder(Config.of(settings, "test"))
                .quiet()
                .handler(ok());
        assertThrows(IOException.class, () -> builder.run(false));
        // And the refusal has not bound the port: a server that is already
        // listening when it refuses is the failure this replaces.
        java.net.ServerSocket probe = new java.net.ServerSocket(port);
        probe.close();
    }

    @Test
    @DisplayName("a bind that fails does not leak the connections it opened")
    void closesThePoolWhenTheBindFails() throws Exception {
        // The port is taken, which is the ordinary case when a previous run has
        // not exited. A supervisor restarting every second used to run the
        // database out of connections before the server ever served a request.
        java.net.ServerSocket taken = new java.net.ServerSocket(0);
        try {
            Properties settings = new Properties();
            settings.setProperty(Config.SERVER_PORT, String.valueOf(taken.getLocalPort()));
            settings.setProperty(Config.DATASOURCE_URL, ":memory:");
            final DataSource[] opened = new DataSource[1];
            assertThrows(IOException.class, () -> Backend.builder(Config.of(settings, "test"))
                    .quiet()
                    .handlers(new Backend.Handlers() {
                        public HttpServer.Handler[] create(DataSource dataSource,
                                com.codename1.backend.orm.EntityManager entities) {
                            opened[0] = dataSource;
                            return new HttpServer.Handler[] {ok()};
                        }
                    })
                    .start());
            assertNotNull(opened[0], "the pool was opened before the bind");
            assertThrows(IOException.class, () -> opened[0].borrow());
        } finally {
            taken.close();
        }
    }

    private static HttpServer.Handler ok() {
        return new HttpServer.Handler() {
            public HttpServer.Response handle(HttpServer.Request request) throws Exception {
                return new HttpServer.Response(200, "text/plain", "ok".getBytes("UTF-8"));
            }
        };
    }

    private static String get(int port, String path) throws Exception {
        HttpURLConnection connection = (HttpURLConnection)
                new URL("http://127.0.0.1:" + port + path).openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        try {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "UTF-8"));
            try {
                StringBuilder out = new StringBuilder();
                String line = in.readLine();
                while(line != null) {
                    out.append(line);
                    line = in.readLine();
                }
                return out.toString();
            } finally {
                in.close();
            }
        } finally {
            connection.disconnect();
        }
    }

    /** A port nothing is listening on, the way the backend's own tests find one. */
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
}
