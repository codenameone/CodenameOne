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

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.codename1.backend.orm.EntityDefinition;
import com.codename1.backend.orm.EntityManager;

/**
 * A configured, running server: the twenty lines every main used to open with,
 * written once.
 *
 * <pre>
 *   Backend.builder()
 *          .handler(new ApiRouter(new Api()))
 *          .run();
 * </pre>
 *
 * <p>That reads the configuration, opens the database the deployment named,
 * registers the generated daos, binds the port, installs a shutdown handler that
 * drains what is in flight, and waits. Each of those was previously the
 * developer's to write and to get wrong -- a main that returns ends the process
 * without a word, because the host threads are detached, and a server with no
 * signal handler loses every connection it was serving when the orchestrator
 * stops it.
 *
 * <h2>What comes from where</h2>
 *
 * <p>Anything the builder is TOLD is used as given. Anything it is not told, it
 * reads from {@link Config}: the port, the worker count, TLS, static files and
 * the database, in that layered order of system property, environment variable,
 * profile file, base file. The rule is worth stating once because the opposite
 * rule is also defensible: a value in the source wins over a value in the
 * environment, so a port written here is the port, and a port that should follow
 * the deployment is one nobody writes here.
 *
 * <p>The database is opened when there is one to open: a URL is configured, a
 * pool was handed in, or the build generated at least one entity and the ORM
 * therefore needs one. A server with no database opens none, which is what makes
 * this the same entry point for both kinds.
 *
 * <pre>
 *   # a laptop
 *   CN1_PROFILE=dev ./server
 *   # production
 *   DATABASE_URL=postgres://app:secret@db.internal/app PORT=8080 ./server
 * </pre>
 */
public final class Backend {
    private final HttpServer server;
    private final DataSource dataSource;
    private final EntityManager entities;
    private final Config config;
    private final int shutdownMillis;
    /**
     * The tracer this server installed, or null. The INSTANCE, not a flag: the
     * global slot may hold another tracer by the time this server stops -- a
     * second server in the same process, or one the application installed -- and
     * stopping this one must not shut that down.
     */
    private final Tracer ownTracer;

    private Backend(HttpServer server, DataSource dataSource, EntityManager entities,
                    Config config, int shutdownMillis, Tracer ownTracer) {
        this.server = server;
        this.dataSource = dataSource;
        this.entities = entities;
        this.config = config;
        this.shutdownMillis = shutdownMillis;
        this.ownTracer = ownTracer;
    }

    /** A builder whose defaults come from the configuration this process sees. */
    public static Builder builder() {
        return new Builder(null);
    }

    /** A builder over a configuration the caller already loaded or built. */
    public static Builder builder(Config config) {
        return new Builder(config);
    }

    /** The running server, for its metrics or to stop it. */
    public HttpServer getServer() {
        return server;
    }

    /** The connection pool, or null when this server has no database. */
    public DataSource getDataSource() {
        return dataSource;
    }

    /** The entity manager, or null when this build generated no entities. */
    public EntityManager getEntityManager() {
        return entities;
    }

    /** The configuration this server resolved its settings from. */
    public Config getConfig() {
        return config;
    }

    /** Blocks until the server stops. */
    public void awaitTermination() {
        server.awaitTermination();
    }

    /**
     * Stops accepting, lets what is in flight finish, and closes the database.
     *
     * <p>The order matters and is the reason this exists rather than two calls:
     * closing the pool first would fail the requests that were still being
     * served with it.
     */
    public void stop() {
        server.stop(shutdownMillis);
        if(dataSource != null) {
            dataSource.close();
        }
        // LAST, so the spans of the requests the drain let finish are exported
        // rather than lost with the process.
        if(ownTracer != null) {
            Tracing.shutdown(ownTracer, shutdownMillis);
        }
    }

    /**
     * The pool, or a refusal naming the handler that asked for one.
     *
     * <p>Called from generated wiring: a controller declaring a
     * {@link DataSource} constructor is declaring a DEPENDENCY, and handing it
     * null because nothing configured a database turns that into a server that
     * starts, reports healthy and fails on the first request that touches the
     * database. The deployment is missing a setting, and start-up is where that
     * is cheap to see.
     */
    public static DataSource requireDataSource(DataSource dataSource, String handler)
            throws IOException {
        if(dataSource == null) {
            throw new IOException(handler + " takes a DataSource, so it needs a database, and "
                    + "none is configured. Set " + Config.DATASOURCE_URL + " (or DATABASE_URL), "
                    + "or run on a development profile, which substitutes an in-memory one.");
        }
        return dataSource;
    }

    /** The entity manager, or a refusal naming the handler that asked for one. */
    public static EntityManager requireEntities(EntityManager entities, String handler)
            throws IOException {
        if(entities == null) {
            throw new IOException(handler + " takes an EntityManager, so it needs the generated "
                    + "daos and a database to reach them through. Either no class in this build "
                    + "carries @Entity, or no database is configured: set "
                    + Config.DATASOURCE_URL + " (or DATABASE_URL), or run on a development "
                    + "profile.");
        }
        return entities;
    }

    /**
     * Where the handlers are built, once the things they need exist.
     *
     * <p>A handler that talks to a database cannot be constructed before the pool
     * is open, and the pool is opened from configuration this builder resolves --
     * so a builder that took ready-made handlers could not inject anything into
     * them. This is the hook the generated entry point uses to construct a
     * controller with the dao it asked for.
     */
    public interface Handlers {
        HttpServer.Handler[] create(DataSource dataSource, EntityManager entities)
                throws Exception;
    }

    /**
     * Where a server's websocket endpoints come from.
     *
     * Deliberately the same shape as {@link Handlers}: the server calls this once
     * while it is starting, with whatever it opened, and the callback registers
     * what it wants. A controller is found by the build, a Handler is called, a
     * DataSource is handed over -- nothing in this runtime is configured by an
     * application reaching into a started server, and websockets are not the
     * exception.
     */
    public interface WebSocketEndpoints {
        void register(HttpServer.WebSocketRegistry registry, DataSource dataSource,
                      EntityManager entities) throws Exception;
    }

    /** Collects what a server needs and starts one. */
    public static final class Builder {
        private Config config;
        private final List handlers = new ArrayList();
        private Handlers factory;
        private DataSource dataSource;
        private String dataSourceUrl;
        private boolean dataSourceGiven;
        private EntityManager entities;
        private int port = -1;
        private int backlog = -1;
        private int workers = -1;
        private int shutdownMillis = -1;
        private String host;
        private Tls tls;
        private String tlsCertificate;
        private String tlsKey;
        private StaticFiles staticFiles;
        private WebSocketEndpoints webSocketEndpoints;
        private boolean createTables;
        private boolean createTablesGiven;
        private boolean handlersNeedADatabase;
        private boolean quiet;
        private Tracer tracer;

        Builder(Config config) {
            this.config = config;
        }

        /**
         * Adds a handler. They are tried in the order they were added and the
         * first that answers wins, so the generated routers go in before any
         * catch-all.
         */
        public Builder handler(HttpServer.Handler handler) {
            if(handler != null) {
                handlers.add(handler);
            }
            return this;
        }

        /** Adds handlers built once the database exists. See {@link Handlers}. */
        public Builder handlers(Handlers factory) {
            this.factory = factory;
            return this;
        }

        /**
         * Registers this server's websocket endpoints when it starts.
         *
         * Called once, before the listener accepts anything, so there is no window
         * in which a route exists in the application's mind and not in the
         * server's -- which is what a `websocket(path, endpoint)` setter on a
         * started server left open.
         */
        public Builder webSockets(WebSocketEndpoints endpoints) {
            this.webSocketEndpoints = endpoints;
            return this;
        }

        /** The port. Otherwise cn1.server.port, PORT, or 8080. */
        public Builder port(int port) {
            this.port = port;
            return this;
        }

        /** The address to bind, or null for every interface. */
        public Builder host(String host) {
            this.host = host;
            return this;
        }

        /** The listen backlog. Otherwise cn1.server.backlog, or 512. */
        public Builder backlog(int backlog) {
            this.backlog = backlog;
            return this;
        }

        /** The size of the request thread pool. Otherwise cn1.server.workers, or 16. */
        public Builder workers(int workers) {
            this.workers = workers;
            return this;
        }

        /** How long a stop waits for requests in flight. */
        public Builder shutdownTimeoutMillis(int millis) {
            this.shutdownMillis = millis;
            return this;
        }

        /**
         * Terminates TLS with this certificate and key.
         *
         * <p>Drops a context given to the other overload earlier, because
         * resolveTls answers from the context FIRST: without this, a builder
         * configured conditionally kept serving the old certificate and this call
         * did nothing at all -- silently, which is the part that matters, since
         * an obsolete certificate looks like a working server until it expires.
         * The last TLS choice wins, as it does for every other setter here.
         *
         * <p>Only when something is actually supplied. tls(null, null) is not a
         * way to turn a context off; it would make an argument nobody meant as a
         * choice erase one that was.
         */
        public Builder tls(String certificatePath, String keyPath) {
            this.tlsCertificate = certificatePath;
            this.tlsKey = keyPath;
            if(certificatePath != null || keyPath != null) {
                this.tls = null;
            }
            return this;
        }

        /**
         * Terminates TLS with a context the caller built.
         *
         * <p>Drops paths given to the other overload earlier, for the reason
         * stated there, and on the same terms: a null context is not a choice and
         * erases nothing.
         */
        public Builder tls(Tls tls) {
            this.tls = tls;
            if(tls != null) {
                this.tlsCertificate = null;
                this.tlsKey = null;
            }
            return this;
        }

        /** Serves a directory, after every handler, so a file cannot shadow a route. */
        public Builder staticFiles(String root, String prefix, String indexFile,
                                   String cacheControl) throws IOException {
            this.staticFiles = new StaticFiles(root, prefix, indexFile, cacheControl);
            return this;
        }

        /**
         * The database, as a SQLite path or a postgres:// or mysql:// URL.
         *
         * <p>Drops a pool given to the other overload earlier: openDataSource
         * answers from the pool FIRST, so without this a builder configured
         * conditionally kept the earlier pool and this call was ignored -- which
         * is a server reading and WRITING to the wrong database while its
         * configuration says otherwise. The last choice wins.
         *
         * <p>Ownership follows the same rule and stays correct either way: the
         * builder opens this URL itself and therefore closes it, while a pool
         * handed in belongs to the caller.
         */
        public Builder dataSource(String url) {
            this.dataSourceUrl = url;
            this.dataSourceGiven = true;
            if(url != null) {
                this.dataSource = null;
            }
            return this;
        }

        /**
         * A pool the caller opened. It is closed when this server stops.
         *
         * <p>Drops a URL given to the other overload earlier, for the reason
         * stated there. A null pool is not a choice and erases nothing.
         */
        public Builder dataSource(DataSource dataSource) {
            this.dataSource = dataSource;
            this.dataSourceGiven = true;
            if(dataSource != null) {
                this.dataSourceUrl = null;
            }
            return this;
        }

        /**
         * Whether to create the table of every generated entity at start-up.
         * Otherwise cn1.orm.createTables, which defaults to true on a development
         * profile and false everywhere else.
         */
        public Builder createTables(boolean create) {
            this.createTables = create;
            this.createTablesGiven = true;
            return this;
        }

        /**
         * Says that the handlers this server builds need a database, so one is
         * opened even when nothing else asks for it.
         *
         * <p>The generated entry point calls this when any controller declares a
         * constructor taking a {@link DataSource} or an
         * {@link EntityManager}. Without it, a controller that declares a
         * database dependency, has no entities behind it, and runs on a
         * development profile with no URL configured was refused at start-up by
         * {@link #requireDataSource} -- whose message suggests running on a
         * development profile, which is what was already happening.
         *
         * <p>The alternative was to open the development default whenever the
         * profile allows it. That is the wrong fix: it would give a database to
         * every server that has no use for one, which contradicts "a server with
         * no database opens none" and costs a file handle to prove it. What was
         * actually missing is that a DECLARED dependency did not drive the
         * decision, and the build knows exactly which controllers declare one.
         */
        public Builder requiresDataSource() {
            this.handlersNeedADatabase = true;
            return this;
        }

        /**
         * Traces every request with this tracer, once {@link Tracer#open} has read
         * the configuration and agreed to. The build calls this from the entry
         * point it generates for a project that enables OpenTelemetry, which is
         * why nothing else refers to a tracer implementation.
         */
        public Builder tracing(Tracer tracer) {
            this.tracer = tracer;
            return this;
        }

        /** Suppresses the line this prints when the server comes up. */
        public Builder quiet() {
            this.quiet = true;
            return this;
        }

        /**
         * Starts the server and returns, without installing a signal handler or
         * waiting. Tests want this; a process wants {@link #run}.
         */
        public Backend start() throws Exception {
            if(config == null) {
                config = Config.load();
            }
            // BEFORE the database, so the statements start-up runs -- the ORM's
            // CREATE TABLE -- are traced like any other, and before anything that
            // could fail, so a refused configuration is refused up front.
            boolean tracing = tracer != null && tracer.open(config);
            // Installed without stopping whatever tracer was there before -- another
            // server's, or one the program installed -- which is retired only once
            // this start-up commits, and put back if it does not.
            Tracing.Swap claim = tracing ? Tracing.swap(tracer) : null;
            Backend started;
            try {
                started = startTraced(tracing);
            } catch (Exception err) {
                if(tracing) {
                    Tracing.rollBack(claim);
                }
                throw err;
            }
            if(tracing) {
                Tracing.commit(claim);
            }
            return started;
        }

        private Backend startTraced(boolean tracing) throws Exception {
            DataSource pool = openDataSource();
            try {
                return startWith(pool, tracing);
            } catch (Exception err) {
                // EVERY failure after the pool is open, not just the bind. A
                // controller constructor that rejects its configuration, a
                // static root that is not a directory, a CREATE TABLE the
                // server refuses: each of those used to leave the connections
                // open, and a supervisor that retries turns that into a pool of
                // dead sessions the database still counts.
                //
                // Only a pool this builder OPENED. One handed in belongs to the
                // caller and is theirs to close.
                // Ownership is whether THIS BUILDER opened it, which is not the
                // same as whether anything was configured: .dataSource(url)
                // makes the builder open one, and reading dataSourceGiven here
                // left exactly that case leaking on a failed start.
                if(pool != null && dataSource == null) {
                    pool.close();
                }
                throw err;
            }
        }

        /** {@link #start} once the database, if any, is open. */
        private Backend startWith(DataSource pool, boolean tracing) throws Exception {
            EntityManager manager = openEntityManager(pool);
            List routers = new ArrayList();
            HttpServer.Handler relay = tracing ? tracer.relay() : null;
            if(relay != null) {
                // FIRST: it answers one exact path, and a catch-all handler
                // added before it would otherwise take the app's exports.
                routers.add(relay);
            }
            routers.addAll(handlers);
            if(factory != null) {
                HttpServer.Handler[] built = factory.create(pool, manager);
                if(built != null) {
                    for(int iter = 0 ; iter < built.length ; iter++) {
                        if(built[iter] != null) {
                            routers.add(built[iter]);
                        }
                    }
                }
            }
            if(staticFiles == null) {
                String root = config.get(Config.STATIC_ROOT);
                if(root != null && root.length() > 0) {
                    staticFiles = new StaticFiles(root,
                            config.get(Config.STATIC_PREFIX, "/static"),
                            config.get(Config.STATIC_INDEX, "index.html"),
                            config.get(Config.STATIC_CACHE_CONTROL, "public, max-age=3600"));
                }
            }
            if(staticFiles != null) {
                // LAST, always. A file that answered before the routers could
                // shadow a route by being named like one, and which of them won
                // would depend on what happened to be in a directory.
                routers.add(staticFiles);
            }
            boolean servesWebSockets = webSocketEndpoints != null;
            if(routers.isEmpty() && !servesWebSockets) {
                throw new IOException("This server has no handlers, so every request would "
                        + "be a 404. Add one with handler(), webSockets(), or a "
                        + "@RestController class for the build to generate one from.");
            }
            if(routers.isEmpty()) {
                // A WEBSOCKET-ONLY SERVER IS A REAL SERVER, and it is what the
                // build generates for a module whose only endpoints are
                // @WebSocketMapping. Without this that generated application
                // compiled, started, and threw before it ever bound a port.
                // Ordinary HTTP requests get the 404 they would have got anyway;
                // the upgrade path is consulted before this chain runs.
                routers.add(new HttpServer.Handler() {
                    public HttpServer.Response handle(HttpServer.Request request) {
                        return null;          // null is a 404 from the chain below
                    }
                });
            }
            final HttpServer.Handler[] chain =
                    (HttpServer.Handler[])routers.toArray(new HttpServer.Handler[routers.size()]);
            int listenPort = port >= 0 ? port : config.getInt(Config.SERVER_PORT, 8080);
            int listenBacklog = backlog >= 0 ? backlog : config.getInt(Config.SERVER_BACKLOG, 512);
            int workerCount = workers >= 0 ? workers : config.getInt(Config.SERVER_WORKERS, 16);
            int drain = shutdownMillis >= 0 ? shutdownMillis
                    : config.getInt(Config.SERVER_SHUTDOWN_MILLIS, 10000);
            if(drain < 0) {
                // HttpServer.stop takes this as a duration and computes its
                // deadline as now + drain, so a negative one is a deadline
                // already past: the drain is skipped and every request in flight
                // has its socket closed mid-response. A typo in a properties
                // file would turn graceful shutdown into truncated answers and
                // say nothing. Zero is kept as the documented "do not wait",
                // like the pool's timeouts.
                //
                // Only the CONFIGURED value can be negative here: a negative
                // passed to the builder means "not set" to the ternary above,
                // the sentinel port, backlog and workers all use.
                throw new IllegalStateException(Config.SERVER_SHUTDOWN_MILLIS + " is "
                        + drain + ", and a shutdown timeout cannot be negative -- it would "
                        + "close the sockets of every request in flight instead of waiting "
                        + "for them. Use 0 to stop immediately on purpose.");
            }
            if(listenBacklog <= 0) {
                // THE TWO RUNTIMES DO NOT AGREE ABOUT A NON-POSITIVE ONE, and
                // both start anyway. Java SE hands it to ServerSocketChannel.bind,
                // where anything not positive selects an implementation default;
                // the packaged runtime hands it straight to listen(), whose
                // behaviour for 0 or less is platform-defined and which reports
                // success either way. So a mistyped properties file gives a
                // development server and a production server materially different
                // accept queues and says nothing -- which is the failure a backlog
                // exists to make visible, arrived at by configuration.
                //
                // Only the CONFIGURED value can land here: a negative passed to
                // the builder means "not set" to the ternary above, which is the
                // sentinel port, backlog and workers all use.
                throw new IllegalStateException(Config.SERVER_BACKLOG + " is "
                        + listenBacklog + ", and an accept queue cannot be empty or "
                        + "negative. It is the number of connections the kernel holds "
                        + "while the server is busy; the two runtimes read a "
                        + "non-positive one differently and both start regardless.");
            }
            Tls context = resolveTls();
            // OURS ONLY WHEN WE MADE IT, the same rule the pool follows above: a
            // context handed to .tls(Tls) belongs to the caller. On the packaged
            // runtime Tls.create allocates a native SSL_CTX, and every path out
            // of HttpServer.start below can fail -- an occupied port, a refused
            // worker count, a reactor that will not set up -- so a supervisor
            // that catches the error and retries leaked one context per attempt.
            // Ownership passes to the server once start returns; until then it
            // is this method's to release.
            //
            // Not covered by a JVM test, and cannot be: the Java SE arm's
            // Tls.create always throws and its constructor is private, so no
            // context exists to leak there. This is a packaged-runtime path.
            boolean ownsContext = context != null && tls == null;
            HttpServer server;
            try {
                server = HttpServer.start(host, listenPort, listenBacklog, workerCount,
                        new HttpServer.Handler() {
                            public HttpServer.Response handle(HttpServer.Request request)
                                    throws Exception {
                                for(int iter = 0 ; iter < chain.length ; iter++) {
                                    HttpServer.Response response = chain[iter].handle(request);
                                    if(response != null) {
                                        return response;
                                    }
                                }
                                // Null is a 404 from here, which is what a router
                                // answers for a path it does not route.
                                return null;
                            }
                        }, context, webSocketEndpoints == null ? null
                                : new HttpServer.WebSocketRoutes() {
                            public void register(HttpServer.WebSocketRegistry registry)
                                    throws Exception {
                                // The same two arguments a Handlers factory gets,
                                // and for the same reason: an endpoint that needs
                                // the database declares it rather than reaching
                                // for a static.
                                webSocketEndpoints.register(registry, pool, manager);
                            }
                        });
            } catch (Exception err) {
                if(ownsContext) {
                    context.close();
                }
                throw err;
            }
            // The websocket routes went in through start() above, before the
            // listener began accepting -- registering them here instead left a
            // window in which a valid upgrade was answered as ordinary HTTP.
            Backend backend = new Backend(server, pool, manager, config, drain,
                    tracing ? tracer : null);
            if(!quiet) {
                announce(backend, listenPort, context != null);
            }
            return backend;
        }

        /**
         * Starts the server, drains it on SIGTERM or SIGINT, and blocks until it
         * stops. This is what a main does.
         */
        public void run() throws Exception {
            run(Signals.installShutdownHandler());
        }

        /**
         * {@link #run} with the answer the signal install gave.
         *
         * <p>Package visible so a test can pass false. The Java SE arm's
         * installShutdownHandler cannot fail -- the JVM registers the hook -- so
         * the refusal below is otherwise reachable only on a translated binary
         * under descriptor exhaustion, which is to say never, in a test.
         */
        void run(boolean shutdownHandlerInstalled) throws Exception {
            // CHECKED, because the failure is otherwise a server that exits
            // saying it succeeded: installShutdownHandler answers false when the
            // self-pipe or the sigaction cannot be set up, and the watcher below
            // then reads its shutdown immediately, stops the server it just
            // started and exits 0. A container that never served a request,
            // reporting success.
            //
            // And BEFORE start(), so a refusal has not already bound the port.
            if(!shutdownHandlerInstalled) {
                throw new IOException("Could not install the shutdown handler, so a stop "
                        + "signal could not be waited for; refusing to start rather than "
                        + "exiting silently once it is registered");
            }
            final Backend backend = start();
            Signals.onShutdown(new Runnable() {
                public void run() {
                    // Stop accepting and let what is in flight finish. Signals
                    // ends the process; exiting from here would deadlock the
                    // shutdown hook this runs from.
                    backend.stop();
                }
            });
            // Required: the host threads are detached, so a main that returned
            // would end the process without a word.
            backend.awaitTermination();
        }

        private Tls resolveTls() throws IOException {
            if(tls != null) {
                return tls;
            }
            String certificate = tlsCertificate != null ? tlsCertificate
                    : config.get(Config.TLS_CERTIFICATE);
            String key = tlsKey != null ? tlsKey : config.get(Config.TLS_KEY);
            if(certificate == null || key == null) {
                if(certificate != null || key != null) {
                    // One without the other is a deployment that believes it is
                    // serving TLS. Plaintext on the port a browser will reach
                    // over https is a failure to connect at best.
                    throw new IOException("TLS needs both a certificate and a key; "
                            + (certificate == null ? Config.TLS_CERTIFICATE : Config.TLS_KEY)
                            + " is not set");
                }
                return null;
            }
            return Tls.create(certificate, key, config.getBoolean(Config.TLS_HTTP2, true));
        }

        /**
         * The pool, or null when this server has no database.
         *
         * <p>One is opened when the deployment named a database, when the caller
         * handed one in, when a handler said it needs one (see
         * {@link #requiresDataSource}), or when the build generated an entity --
         * because an entity with nowhere to live is a server that would fail on
         * its first query instead of at start-up.
         */
        private DataSource openDataSource() throws IOException {
            if(dataSource != null) {
                return dataSource;
            }
            if(dataSourceUrl != null) {
                // Empty is an explicit choice too: let DataSource reject it
                // instead of silently opening a different configured database.
                // The URL is the builder's, the POOL SETTINGS are still the
                // deployment's. cn1.datasource.pool.size, the busy timeout and
                // the borrow timeout have no builder methods, so opening with
                // the defaults here left an operator no way to tune a pool whose
                // URL the code happens to name -- against this builder's own
                // rule that what it is not told comes from the configuration.
                return DataSource.open(dataSourceUrl,
                        config.getInt(Config.DATASOURCE_POOL_SIZE, 0),
                        config.getInt(Config.DATASOURCE_BUSY_MILLIS, 5000),
                        config.getInt(Config.DATASOURCE_BORROW_MILLIS, 10000));
            }
            boolean configured = config.get(Config.DATASOURCE_URL) != null;
            if(!configured && !handlersNeedADatabase && EntityManager.registered().length == 0) {
                return null;
            }
            return DataSource.fromConfig(config);
        }

        private EntityManager openEntityManager(DataSource pool) throws IOException {
            if(entities != null) {
                return entities;
            }
            EntityDefinition[] known = EntityManager.registered();
            if(known.length == 0 || pool == null) {
                return null;
            }
            EntityManager manager = EntityManager.open(pool);
            boolean create = createTablesGiven ? createTables
                    : config.getBoolean(Config.ORM_CREATE_TABLES, config.isDevelopmentProfile());
            if(create) {
                manager.createTables();
            }
            return manager;
        }

        /**
         * One line saying what came up and where its data is.
         *
         * <p>Through the pool's own description rather than the URL, which holds
         * the password. A start-up line is the easiest way for a credential to
         * reach a log aggregator.
         */
        private void announce(Backend backend, int listenPort, boolean secure) {
            StringBuilder line = new StringBuilder("listening on ");
            line.append(secure ? "https" : "http").append(" port ").append(listenPort);
            line.append(", ").append(config.describe());
            if(backend.getDataSource() != null) {
                line.append(", database ").append(backend.getDataSource());
            }
            if(backend.getEntityManager() != null) {
                line.append(", ").append(EntityManager.registered().length).append(" entities");
            }
            System.out.println(line.toString());
        }
    }
}
