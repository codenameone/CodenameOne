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
package com.codename1.backend.metrics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The server's metrics: every {@link Instrument} by name, and the ones the
 * server records about itself.
 *
 * <pre>
 *   private static final Counter ORDERS = Metrics.counter("orders.placed",
 *           "Orders accepted", "{order}");
 *   ...
 *   ORDERS.increment();
 * </pre>
 *
 * <p>Asking for a name twice answers the same instrument, so a static field
 * initialized in two classes, or a server restarted in one process, shares it.
 * Asking for an existing name as a different kind is refused.
 *
 * <p>What reads them: the OTLP exporter, when the build enables OpenTelemetry;
 * the management endpoint's {@code metrics} and {@code prometheus} views; and
 * the development MCP server. None of them is linked into a server whose build
 * does not ask for it, and an instrument nothing reads costs its own
 * recording and nothing more.
 *
 * <h2>What the server records</h2>
 *
 * <p>Once {@link #enableServer} has run -- the generated entry point calls it when
 * metrics are on -- every request is measured in
 * {@code http.server.request.duration} (milliseconds, by route template, method
 * and status), and the server's counters, the database pool, the task executors
 * and the process's memory are published as gauges.
 */
public final class Metrics {
    private static final Map INSTRUMENTS = new LinkedHashMap();
    private static final long STARTED = System.currentTimeMillis();

    /** Whether the server records its own requests. Plain: see {@link #enableServer}. */
    static boolean serverEnabled;
    private static Histogram requestDuration;
    private static Histogram jobDuration;
    private static final ThreadLocal ROUTE = new ThreadLocal();

    private Metrics() {
    }

    /** A counter; the same one for the same name. */
    public static Counter counter(String name, String description, String unit) {
        return (Counter)register(name, Instrument.COUNTER, description, unit, null, null);
    }

    /** A counter that can go down too -- items in a queue, open sessions. */
    public static Counter upDownCounter(String name, String description, String unit) {
        return (Counter)register(name, Instrument.UP_DOWN_COUNTER, description, unit, null,
                null);
    }

    /** A histogram with the default bucket boundaries, which suit milliseconds. */
    public static Histogram histogram(String name, String description, String unit) {
        return histogram(name, description, unit, null, null);
    }

    /**
     * A histogram with the given bucket boundaries, ascending, and label keys (up
     * to three), or null for either default.
     */
    public static Histogram histogram(String name, String description, String unit,
                                      double[] bounds, String[] labels) {
        if(labels != null && labels.length > 3) {
            throw new IllegalArgumentException("A histogram takes at most three labels");
        }
        return (Histogram)register(name, Instrument.HISTOGRAM, description, unit, bounds,
                labels);
    }

    /**
     * A gauge read from {@code source} when metrics are collected. A gauge
     * registered again under the same name REPLACES the old one, since its
     * source is usually an object that has been replaced too -- a restarted
     * server's pool.
     */
    public static Gauge gauge(String name, String description, String unit,
                              Gauge.Source source) {
        Gauge g = new Gauge(name, description, unit, source);
        replaceGauge(g);
        return g;
    }

    /** A gauge with several labelled values. See {@link Gauge.MultiSource}. */
    public static Gauge gauge(String name, String description, String unit,
                              Gauge.MultiSource source) {
        Gauge g = new Gauge(name, description, unit, source);
        replaceGauge(g);
        return g;
    }

    private static synchronized void replaceGauge(Gauge g) {
        Instrument existing = (Instrument)INSTRUMENTS.get(g.getName());
        if(existing != null && existing.getKind() != Instrument.GAUGE) {
            throw new IllegalArgumentException("Metric " + g.getName()
                    + " already exists as another kind");
        }
        INSTRUMENTS.put(g.getName(), g);
    }

    private static synchronized Instrument register(String name, int kind, String description,
                                                    String unit, double[] bounds,
                                                    String[] labels) {
        if(name == null || name.length() == 0) {
            throw new IllegalArgumentException("A metric needs a name");
        }
        Instrument existing = (Instrument)INSTRUMENTS.get(name);
        if(existing != null) {
            if(existing.getKind() != kind) {
                throw new IllegalArgumentException("Metric " + name
                        + " already exists as another kind");
            }
            return existing;
        }
        Instrument created;
        if(kind == Instrument.HISTOGRAM) {
            created = new Histogram(name, description, unit, bounds, labels);
        } else {
            created = new Counter(name, description, unit, kind == Instrument.UP_DOWN_COUNTER);
        }
        INSTRUMENTS.put(name, created);
        return created;
    }

    /** Every instrument, in registration order. */
    public static synchronized List instruments() {
        return new ArrayList(INSTRUMENTS.values());
    }

    /** The instrument called {@code name}, or null. */
    public static synchronized Instrument get(String name) {
        return (Instrument)INSTRUMENTS.get(name);
    }

    /** When this process's metrics started counting, for cumulative points. */
    public static long startTimeMillis() {
        return STARTED;
    }

    // ------------------------------------------------------- server instrumentation

    /**
     * Starts recording the server's own metrics. Called once the server is
     * listening; the pool may be null.
     */
    public static void enableServer(final com.codename1.backend.HttpServer server,
                                    final com.codename1.backend.DataSource pool) {
        requestDuration = histogram("http.server.request.duration",
                "Duration of HTTP server requests", "ms", null,
                new String[] {"http.route", "http.request.method", "http.response.status_code"});
        jobDuration = histogram("cn1.scheduler.run.duration",
                "Duration of scheduled job runs", "ms", null,
                new String[] {"cn1.job", "cn1.outcome", null});
        serverMetric(server, "http.server.active_requests", "activeRequests",
                "Requests being served", "{request}");
        serverMetric(server, "http.server.open_connections", "openConnections",
                "Open client connections", "{connection}");
        serverMetric(server, "cn1.server.websocket_connections", "webSocketConnections",
                "Open websocket connections", "{connection}");
        serverMetric(server, "cn1.server.requests_served", "requestsServed",
                "Requests answered since start", "{request}");
        serverMetric(server, "cn1.server.connections_refused", "connectionsRefused",
                "Connections refused for being over the limit", "{connection}");
        if(pool != null) {
            gauge("db.client.connection.count", "Open database connections", "{connection}",
                    new Gauge.Source() {
                public double read() {
                    return pool.getOpenCount();
                }
            });
            gauge("db.client.connection.idle", "Idle database connections", "{connection}",
                    new Gauge.Source() {
                public double read() {
                    return pool.getIdleCount();
                }
            });
        }
        gauge("cn1.task.queue_depth", "Tasks waiting for a thread, by executor", "{task}",
                new Gauge.MultiSource() {
            public List read() {
                List out = new ArrayList();
                List all = com.codename1.backend.Tasks.executors();
                for(int iter = 0 ; iter < all.size() ; iter++) {
                    com.codename1.backend.TaskExecutor e =
                            (com.codename1.backend.TaskExecutor)all.get(iter);
                    out.add(Gauge.point("cn1.executor", e.getName(), e.getQueueDepth()));
                }
                return out;
            }
        });
        gauge("process.runtime.memory.used", "Heap in use", "By", new Gauge.Source() {
            public double read() {
                Runtime r = Runtime.getRuntime();
                return r.totalMemory() - r.freeMemory();
            }
        });
        gauge("process.uptime", "Seconds since the process started", "s", new Gauge.Source() {
            public double read() {
                return (System.currentTimeMillis() - STARTED) / 1000.0;
            }
        });
        serverEnabled = true;
    }

    private static void serverMetric(final com.codename1.backend.HttpServer server,
                                     String name, final String key, String description,
                                     String unit) {
        gauge(name, description, unit, new Gauge.Source() {
            public double read() {
                Object v = server.getMetrics().get(key);
                return v instanceof Number ? ((Number)v).doubleValue() : Double.NaN;
            }
        });
    }

    /** Records which route template matched, for the request histogram. */
    public static void route(String template) {
        if(serverEnabled) {
            ROUTE.set(template);
        }
    }

    /** A request is starting; answers the time to hand to {@link #requestEnded}. */
    public static long requestStarted() {
        return serverEnabled ? System.nanoTime() : 0L;
    }

    /** A request has been answered. */
    public static void requestEnded(long started, String method, int status) {
        if(!serverEnabled || started == 0L) {
            return;
        }
        Object route = ROUTE.get();
        ROUTE.set(null);
        requestDuration.record((System.nanoTime() - started) / 1000000.0, route, method,
                new Integer(status));
    }

    /** A scheduled job has run. */
    public static void jobRan(String job, long millis, boolean failed) {
        if(serverEnabled) {
            jobDuration.record(millis, job, failed ? "failure" : "success", null);
        }
    }

    // ------------------------------------------------------------------ views

    /**
     * Every instrument and its points, for the management endpoint and MCP:
     * {@code name -> {kind, description, unit, points}}.
     */
    public static Map snapshot() {
        Map out = new LinkedHashMap();
        List all = instruments();
        for(int iter = 0 ; iter < all.size() ; iter++) {
            Instrument i = (Instrument)all.get(iter);
            Map m = new LinkedHashMap();
            m.put("kind", kindName(i.getKind()));
            if(i.getDescription().length() > 0) {
                m.put("description", i.getDescription());
            }
            if(i.getUnit().length() > 0) {
                m.put("unit", i.getUnit());
            }
            m.put("points", i.points());
            out.put(i.getName(), m);
        }
        return out;
    }

    static String kindName(int kind) {
        switch(kind) {
            case Instrument.COUNTER: return "counter";
            case Instrument.UP_DOWN_COUNTER: return "upDownCounter";
            case Instrument.GAUGE: return "gauge";
            default: return "histogram";
        }
    }

    /** Every instrument in the Prometheus text exposition format. */
    public static String prometheus() {
        StringBuilder sb = new StringBuilder();
        List all = instruments();
        for(int iter = 0 ; iter < all.size() ; iter++) {
            Instrument i = (Instrument)all.get(iter);
            String name = promName(i.getName());
            String type;
            switch(i.getKind()) {
                case Instrument.COUNTER:
                    type = "counter";
                    name = name + "_total";
                    break;
                case Instrument.HISTOGRAM:
                    type = "histogram";
                    break;
                default:
                    type = "gauge";
            }
            if(i.getDescription().length() > 0) {
                sb.append("# HELP ").append(name).append(' ')
                        .append(escapeHelp(i.getDescription())).append('\n');
            }
            sb.append("# TYPE ").append(name).append(' ').append(type).append('\n');
            List points = i.points();
            for(int p = 0 ; p < points.size() ; p++) {
                Map point = (Map)points.get(p);
                Map attributes = (Map)point.get("attributes");
                if(i.getKind() == Instrument.HISTOGRAM) {
                    List bounds = (List)point.get("bounds");
                    List buckets = (List)point.get("buckets");
                    long cumulative = 0;
                    for(int b = 0 ; b < buckets.size() ; b++) {
                        cumulative += ((Number)buckets.get(b)).longValue();
                        String le = b < bounds.size()
                                ? number(((Number)bounds.get(b)).doubleValue()) : "+Inf";
                        sb.append(name).append("_bucket");
                        labels(sb, attributes, le);
                        sb.append(' ').append(cumulative).append('\n');
                    }
                    sb.append(name).append("_sum");
                    labels(sb, attributes, null);
                    sb.append(' ').append(number(((Number)point.get("sum")).doubleValue()))
                            .append('\n');
                    sb.append(name).append("_count");
                    labels(sb, attributes, null);
                    sb.append(' ').append(point.get("count")).append('\n');
                } else {
                    sb.append(name);
                    labels(sb, attributes, null);
                    sb.append(' ').append(number(((Number)point.get("value")).doubleValue()))
                            .append('\n');
                }
            }
        }
        return sb.toString();
    }

    private static void labels(StringBuilder sb, Map attributes, String le) {
        boolean any = (attributes != null && !attributes.isEmpty()) || le != null;
        if(!any) {
            return;
        }
        sb.append('{');
        boolean first = true;
        if(attributes != null) {
            Iterator it = attributes.entrySet().iterator();
            while(it.hasNext()) {
                Map.Entry e = (Map.Entry)it.next();
                if(!first) {
                    sb.append(',');
                }
                first = false;
                sb.append(promName(String.valueOf(e.getKey()))).append("=\"")
                        .append(escapeLabel(String.valueOf(e.getValue()))).append('"');
            }
        }
        if(le != null) {
            if(!first) {
                sb.append(',');
            }
            sb.append("le=\"").append(le).append('"');
        }
        sb.append('}');
    }

    static String promName(String name) {
        StringBuilder sb = new StringBuilder(name.length());
        for(int iter = 0 ; iter < name.length() ; iter++) {
            char c = name.charAt(iter);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_'
                    || c == ':' || (iter > 0 && c >= '0' && c <= '9');
            sb.append(ok ? c : '_');
        }
        return sb.toString();
    }

    private static String escapeLabel(String value) {
        StringBuilder sb = new StringBuilder(value.length());
        for(int iter = 0 ; iter < value.length() ; iter++) {
            char c = value.charAt(iter);
            if(c == '\\' || c == '"') {
                sb.append('\\').append(c);
            } else if(c == '\n') {
                sb.append("\\n");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String escapeHelp(String value) {
        StringBuilder sb = new StringBuilder(value.length());
        for(int iter = 0 ; iter < value.length() ; iter++) {
            char c = value.charAt(iter);
            if(c == '\\') {
                sb.append("\\\\");
            } else if(c == '\n') {
                sb.append("\\n");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String number(double d) {
        if(Double.isNaN(d)) {
            return "NaN";
        }
        if(Double.isInfinite(d)) {
            return d > 0 ? "+Inf" : "-Inf";
        }
        if(d == Math.floor(d) && Math.abs(d) < 1e15) {
            return Long.toString((long)d);
        }
        return Double.toString(d);
    }
}
