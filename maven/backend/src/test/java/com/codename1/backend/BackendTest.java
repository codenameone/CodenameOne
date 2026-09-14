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
        // SAID OUT LOUD, because the other reason to open a database is a
        // registered entity, and that registry is static and never cleared -- a
        // server with one is SUPPOSED to open a pool. If a test class that
        // registers one ever shares this JVM, the assertion below stops being
        // about what it says it is, so the precondition fails first and names
        // why. The module forks a JVM per test class to keep this true.
        assertEquals(0, com.codename1.backend.orm.EntityManager.registered().length,
                "another test class registered an entity into this JVM, so this server "
                        + "has a reason to open a database and the assertion below would "
                        + "be measuring that instead");
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

    @Test
    @DisplayName("a handler that asked for a database is refused one that is not there")
    void refusesAMissingDependency() {
        // The generated wiring passes a controller's declared dependency through
        // these. Handing null instead produces a server that starts, reports
        // healthy, and fails on the first request that touches the database --
        // when what is actually wrong is a missing deployment setting.
        IOException err = assertThrows(IOException.class,
                () -> Backend.requireDataSource(null, "com.example.Api"));
        assertTrue(err.getMessage().contains("com.example.Api"), err.getMessage());
        assertTrue(err.getMessage().contains(Config.DATASOURCE_URL), err.getMessage());
        IOException entities = assertThrows(IOException.class,
                () -> Backend.requireEntities(null, "com.example.Api"));
        assertTrue(entities.getMessage().contains("com.example.Api"), entities.getMessage());
        // And what is there is passed straight through.
        assertThrows(IOException.class, () -> Backend.requireDataSource(null, "x"));
    }

    @Test
    @DisplayName("a failure while building handlers closes the pool that was opened for them")
    void closesThePoolWhenHandlerConstructionFails() throws Exception {
        // The pool is open before the handlers exist, because the handlers are
        // what needs it. A controller constructor that rejects its configuration
        // used to leave those connections open, and a supervisor that retries
        // turns that into a database full of dead sessions.
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(freePort()));
        settings.setProperty(Config.DATASOURCE_URL, ":memory:");
        final DataSource[] opened = new DataSource[1];
        assertThrows(IllegalStateException.class, () -> Backend.builder(Config.of(settings, "test"))
                .quiet()
                .handlers(new Backend.Handlers() {
                    public HttpServer.Handler[] create(DataSource dataSource,
                            com.codename1.backend.orm.EntityManager entities) {
                        opened[0] = dataSource;
                        throw new IllegalStateException("this controller refuses to start");
                    }
                })
                .start());
        assertNotNull(opened[0], "the pool was opened before the handlers were built");
        assertThrows(IOException.class, () -> opened[0].borrow());
    }

    @Test
    @DisplayName("a pool the builder opened from a URL is closed when the start fails")
    void closesAUrlBuiltPoolOnFailure() throws Exception {
        // Ownership is whether the BUILDER opened it, not whether anything was
        // configured. .dataSource(url) makes the builder open one, and the first
        // version of this cleanup read a flag that both overloads set -- so
        // exactly this case went on leaking.
        final DataSource[] opened = new DataSource[1];
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(freePort()));
        assertThrows(IllegalStateException.class, () -> Backend.builder(Config.of(settings, "test"))
                .quiet()
                .dataSource(":memory:")
                .handlers(new Backend.Handlers() {
                    public HttpServer.Handler[] create(DataSource dataSource,
                            com.codename1.backend.orm.EntityManager entities) {
                        opened[0] = dataSource;
                        throw new IllegalStateException("this controller refuses to start");
                    }
                })
                .start());
        assertNotNull(opened[0]);
        assertThrows(IOException.class, () -> opened[0].borrow());
    }

    @Test
    @DisplayName("a pool the caller handed in survives a failed start")
    void leavesACallerOwnedPoolAlone() throws Exception {
        // The other side of the same rule: what the caller opened is the
        // caller's to close, and a builder that closed it would break the retry
        // it was handed for.
        DataSource mine = DataSource.open(":memory:");
        try {
            Properties settings = new Properties();
            settings.setProperty(Config.SERVER_PORT, String.valueOf(freePort()));
            assertThrows(IllegalStateException.class,
                    () -> Backend.builder(Config.of(settings, "test"))
                            .quiet()
                            .dataSource(mine)
                            .handlers(new Backend.Handlers() {
                                public HttpServer.Handler[] create(DataSource dataSource,
                                        com.codename1.backend.orm.EntityManager entities) {
                                    throw new IllegalStateException("refused");
                                }
                            })
                            .start());
            pooledStillWorks(mine);
        } finally {
            mine.close();
        }
    }

    private static void pooledStillWorks(DataSource pool) throws Exception {
        Database db = pool.borrow();
        try {
            assertTrue(db.isOpen());
        } finally {
            pool.release(db);
        }
    }

    @Test
    @DisplayName("a handler that declares it needs a database gets the development default")
    void opensTheDevelopmentDefaultForAHandlerThatNeedsOne() throws Exception {
        // A controller declaring a DataSource, with no entities behind it and no
        // URL configured, on a development profile. The pool used to be null
        // here and requireDataSource then advised running on a development
        // profile -- which is what was already happening. The build says the
        // handlers need one; the profile says where to get it.
        int port = freePort();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        final DataSource[] seen = new DataSource[1];
        Backend backend = Backend.builder(Config.of(settings, "dev"))
                .quiet()
                .requiresDataSource()
                .handlers(new Backend.Handlers() {
                    public HttpServer.Handler[] create(DataSource dataSource,
                            com.codename1.backend.orm.EntityManager entities) throws Exception {
                        seen[0] = Backend.requireDataSource(dataSource, "com.example.Api");
                        return new HttpServer.Handler[] {ok()};
                    }
                })
                .start();
        try {
            assertNotNull(seen[0]);
            assertEquals("ok", get(port, "/"));
        } finally {
            backend.stop();
        }
        // And a server that does NOT say it needs one still opens none: the
        // point is the declared dependency, not the profile.
        Properties quiet = new Properties();
        quiet.setProperty(Config.SERVER_PORT, String.valueOf(freePort()));
        Backend none = Backend.builder(Config.of(quiet, "dev")).quiet().handler(ok()).start();
        try {
            assertNull(none.getDataSource());
        } finally {
            none.stop();
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
