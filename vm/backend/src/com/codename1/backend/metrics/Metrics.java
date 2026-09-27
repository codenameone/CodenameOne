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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// The server's metrics: every [Instrument] by name, and the ones the
/// server records about itself.
///
/// ```java
///   private static final Counter ORDERS = Metrics.counter("orders.placed",
///           "Orders accepted", "{order}");
///   ...
///   ORDERS.increment();
/// ```
///
/// Asking for a name twice answers the same instrument, so a static field
/// initialized in two classes, or a server restarted in one process, shares it.
/// Asking for an existing name as a different kind is refused.
///
/// What reads them: the OTLP exporter, when the build enables OpenTelemetry;
/// the management endpoint's `metrics` and `prometheus` views; and
/// the development MCP server. None of them is linked into a server whose build
/// does not ask for it, and an instrument nothing reads costs its own
/// recording and nothing more.
///
/// ## What the server records
///
/// Once [#enableServer] has run -- the generated entry point calls it when
/// metrics are on -- every request is measured in
/// `http.server.request.duration` (milliseconds, by route template, method
/// and status), and the server's counters, the database pool, the task executors
/// and the process's memory are published as gauges.
public final class Metrics {
    private static final Map INSTRUMENTS = new LinkedHashMap();
    private static final long STARTED = System.currentTimeMillis();

    /// Whether the server records its own requests. Plain: see [#enableServer].
    static boolean serverEnabled;
    private static Histogram requestDuration;
    private static Histogram jobDuration;
    private static final ThreadLocal ROUTE = new ThreadLocal();

    private Metrics() {
    }

    /// A counter; the same one for the same name.
    public static Counter counter(String name, String description, String unit) {
        return (Counter) register(name, Instrument.COUNTER, description, unit, null, null);
    }

    /// A counter that can go down too -- items in a queue, open sessions.
    public static Counter upDownCounter(String name, String description, String unit) {
        return (Counter) register(name, Instrument.UP_DOWN_COUNTER, description, unit, null,
                null);
    }

    /// A histogram with the default bucket boundaries, which suit milliseconds.
    public static Histogram histogram(String name, String description, String unit) {
        return histogram(name, description, unit, null, null);
    }

    /// A histogram with the given bucket boundaries, ascending, and label keys (up
    /// to three), or null for either default.
    public static Histogram histogram(String name, String description, String unit,
                                      double[] bounds, String[] labels) {
        if (labels != null && labels.length > 3) {
            throw new IllegalArgumentException("A histogram takes at most three labels");
        }
        return (Histogram) register(name, Instrument.HISTOGRAM, description, unit, bounds,
                labels);
    }

    /// A gauge read from `source` when metrics are collected. A gauge
    /// registered again under the same name REPLACES the old one, since its
    /// source is usually an object that has been replaced too -- a restarted
    /// server's pool.
    public static Gauge gauge(String name, String description, String unit,
                              Gauge.Source source) {
        Gauge g = new Gauge(name, description, unit, source);
        replaceGauge(g);
        return g;
    }

    /// A gauge with several labelled values. See [Gauge.MultiSource].
    public static Gauge gauge(String name, String description, String unit,
                              Gauge.MultiSource source) {
        Gauge g = new Gauge(name, description, unit, source);
        replaceGauge(g);
        return g;
    }

    private static synchronized void replaceGauge(Gauge g) {
        Instrument existing = (Instrument) INSTRUMENTS.get(g.getName());
        if (existing != null && existing.getKind() != Instrument.GAUGE) {
            throw new IllegalArgumentException("Metric " + g.getName()
                    + " already exists as another kind");
        }
        claimPrometheusNames(g.getName(), Instrument.GAUGE);
        INSTRUMENTS.put(g.getName(), g);
    }

    /// Prometheus series name -> the instrument name that renders it.
    private static final Map PROMETHEUS_NAMES = new HashMap();

    /// The Prometheus view folds every character a metric name cannot hold to
    /// `_` and adds suffixes, so distinct instruments -- orders.total and
    /// orders_total, or a counter orders beside a gauge orders_total -- would
    /// render as one series, which a scrape rejects or silently merges. Refused
    /// when the instrument is created instead.
    private static void claimPrometheusNames(String name, int kind) {
        String base = promName(name);
        String[] series = kind == Instrument.COUNTER ? new String[] {base + "_total"}
                : kind == Instrument.HISTOGRAM ? new String[] {base, base + "_bucket",
                        base + "_sum", base + "_count"}
                : new String[] {base};
        for (String element : series) {
            Object owner = PROMETHEUS_NAMES.get(element);
            if (owner != null && !owner.equals(name)) {
                throw new IllegalArgumentException("Metric " + name + " would be exported to "
                        + "Prometheus as " + element + ", which metric " + owner
                        + " already is; rename one");
            }
        }
        for (String element : series) {
            PROMETHEUS_NAMES.put(element, name);
        }
    }

    private static void releasePrometheusNames(String name) {
        Iterator it = PROMETHEUS_NAMES.values().iterator();
        while (it.hasNext()) {
            if (name.equals(it.next())) {
                it.remove();
            }
        }
    }

    private static synchronized Instrument register(String name, int kind, String description,
                                                    String unit, double[] bounds,
                                                    String[] labels) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("A metric needs a name");
        }
        Instrument existing = (Instrument) INSTRUMENTS.get(name);
        if (existing != null) {
            if (existing.getKind() != kind) {
                throw new IllegalArgumentException("Metric " + name
                        + " already exists as another kind");
            }
            return existing;
        }
        Instrument created;
        if (kind == Instrument.HISTOGRAM) {
            created = new Histogram(name, description, unit, bounds, labels);
        } else {
            created = new Counter(name, description, unit, kind == Instrument.UP_DOWN_COUNTER);
        }
        // After construction, so a histogram refusing its bounds claims nothing.
        claimPrometheusNames(name, kind);
        INSTRUMENTS.put(name, created);
        return created;
    }

    /// Every instrument, in registration order.
    public static synchronized List instruments() {
        return new ArrayList(INSTRUMENTS.values());
    }

    /// The instrument called `name`, or null.
    public static synchronized Instrument get(String name) {
        return (Instrument) INSTRUMENTS.get(name);
    }

    /// When this process's metrics started counting, for cumulative points.
    public static long startTimeMillis() {
        return STARTED;
    }

    // ------------------------------------------------------- server instrumentation

    /// Gauges several servers contribute to: name -> List of Gauge.Source.
    private static final Map SHARED = new HashMap();

    /// Adds one server's source to the gauge called `name`, which reports
    /// the sum of every source still registered. A server's managed-resource
    /// gauges go through here and are removed when it stops, so a stopped
    /// server's bean is never read again and a second live server adds to the
    /// gauge instead of silently replacing the first one's.
    public static synchronized void addSource(String name, String description, String unit,
                                              Gauge.Source source) {
        List sources = (List) SHARED.get(name);
        if (sources == null) {
            final List all = new ArrayList();
            sources = all;
            // Registered FIRST: a gauge refused here -- another kind under the
            // name, a Prometheus clash -- must leave no entry behind, or a retry
            // would find it and skip registering the gauge at all.
            replaceGauge(new Gauge(name, description, unit, new Gauge.Source() {
                @Override
                public double read() {
                    Object[] each;
                    synchronized (Metrics.class) {
                        each = all.toArray();
                    }
                    double sum = 0;
                    boolean any = false;
                    for (Object element : each) {
                        try {
                            double v = ((Gauge.Source) element).read();
                            if (!Double.isNaN(v)) {
                                sum += v;
                                any = true;
                            }
                        } catch (Throwable err) {
                            // One source failing leaves the others' values.
                        }
                    }
                    return any ? sum : Double.NaN;
                }
            }));
            SHARED.put(name, all);
        }
        sources.add(source);
    }

    /// Removes a source [#addSource] added; the gauge goes with its last one.
    public static synchronized void removeSource(String name, Gauge.Source source) {
        List sources = (List) SHARED.get(name);
        if (sources == null) {
            return;
        }
        sources.remove(source);
        if (sources.isEmpty()) {
            SHARED.remove(name);
            INSTRUMENTS.remove(name);
            releasePrometheusNames(name);
        }
    }

    /// The servers recording their own metrics, and their pools.
    private static final List LIVE_SERVERS = new ArrayList();
    private static final List LIVE_POOLS = new ArrayList();

    /// Starts recording a server's own metrics. Called once the server is
    /// listening; the pool may be null.
    ///
    /// The instruments are the process's, as an OpenTelemetry meter's are, so
    /// with two servers in one process each built-in gauge reports the SUM over
    /// the servers still running, and the request histogram counts both. Each
    /// server used to register its own source under the same name, so the last
    /// one started silently replaced the others' -- and a stopped server's stayed.
    public static void enableServer(final com.codename1.backend.HttpServer server,
                                    final com.codename1.backend.DataSource pool) {
        synchronized (Metrics.class) {
            if (!LIVE_SERVERS.contains(server)) {
                LIVE_SERVERS.add(server);
            }
            if (pool != null && !LIVE_POOLS.contains(pool)) {
                LIVE_POOLS.add(pool);
            }
        }
        requestDuration = histogram("http.server.request.duration",
                "Duration of HTTP server requests", "ms", null,
                new String[] {"http.route", "http.request.method", "http.response.status_code"});
        jobDuration = histogram("cn1.scheduler.run.duration",
                "Duration of scheduled job runs", "ms", null,
                new String[] {"cn1.job", "cn1.outcome", null});
        serverMetric("http.server.active_requests", "activeRequests",
                "Requests being served", "{request}");
        serverMetric("http.server.open_connections", "openConnections",
                "Open client connections", "{connection}");
        serverMetric("cn1.server.websocket_connections", "webSocketConnections",
                "Open websocket connections", "{connection}");
        serverMetric("cn1.server.requests_served", "requestsServed",
                "Requests answered since start", "{request}");
        serverMetric("cn1.server.connections_refused", "connectionsRefused",
                "Connections refused for being over the limit", "{connection}");
        gauge("db.client.connection.count", "Open database connections", "{connection}",
                new Gauge.Source() {
                    @Override
                    public double read() {
                        return poolSum(false);
                    }
                });
        gauge("db.client.connection.idle", "Idle database connections", "{connection}",
                new Gauge.Source() {
                    @Override
                    public double read() {
                        return poolSum(true);
                    }
                });
        gauge("cn1.task.queue_depth", "Tasks waiting for a thread, by executor", "{task}",
                new Gauge.MultiSource() {
                    @Override
                    public List read() {
                        // Summed by name: two servers in the process each have a
                        // "default" executor, and two points with one label set are one
                        // series twice -- duplicate samples a scrape rejects.
                        Map byName = new LinkedHashMap();
                        List all = com.codename1.backend.Tasks.executors();
                        for (Object element : all) {
                            com.codename1.backend.TaskExecutor e =
                                    (com.codename1.backend.TaskExecutor) element;
                            Long sum = (Long) byName.get(e.getName());
                            byName.put(e.getName(), Long.valueOf((sum == null ? 0 : sum.longValue())
                                    + e.getQueueDepth()));
                        }
                        List out = new ArrayList();
                        Iterator names = byName.entrySet().iterator();
                        while (names.hasNext()) {
                            Map.Entry entry = (Map.Entry) names.next();
                            out.add(Gauge.point("cn1.executor", (String) entry.getKey(),
                                    ((Long) entry.getValue()).longValue()));
                        }
                        return out;
                    }
                });
        gauge("process.runtime.memory.used", "Heap in use", "By", new Gauge.Source() {
            @Override
            public double read() {
                Runtime r = Runtime.getRuntime();
                return r.totalMemory() - r.freeMemory();
            }
        });
        gauge("process.uptime", "Seconds since the process started", "s", new Gauge.Source() {
            @Override
            public double read() {
                return (System.currentTimeMillis() - STARTED) / 1000.0;
            }
        });
        synchronized (Metrics.class) {
            serverEnabled = true;
        }
    }

    /// A server has stopped: its gauges stop counting it, and once none is left
    /// the server instruments stop recording.
    public static void disableServer(com.codename1.backend.HttpServer server,
                                     com.codename1.backend.DataSource pool) {
        synchronized (Metrics.class) {
            LIVE_SERVERS.remove(server);
            if (pool != null) {
                LIVE_POOLS.remove(pool);
            }
            if (LIVE_SERVERS.isEmpty()) {
                serverEnabled = false;
            }
        }
    }

    private static synchronized List liveServers() {
        return new ArrayList(LIVE_SERVERS);
    }

    private static double poolSum(boolean idle) {
        List pools;
        synchronized (Metrics.class) {
            pools = new ArrayList(LIVE_POOLS);
        }
        double sum = 0;
        for (Object element : pools) {
            com.codename1.backend.DataSource p = (com.codename1.backend.DataSource) element;
            sum += idle ? p.getIdleCount() : p.getOpenCount();
        }
        return sum;
    }

    private static void serverMetric(String name, final String key, String description,
                                     String unit) {
        gauge(name, description, unit, new Gauge.Source() {
            @Override
            public double read() {
                List servers = liveServers();
                double sum = 0;
                for (Object element : servers) {
                    Object v = ((com.codename1.backend.HttpServer) element)
                            .getMetrics().get(key);
                    if (v instanceof Number) {
                        sum += ((Number) v).doubleValue();
                    }
                }
                return sum;
            }
        });
    }

    /// Records which route template matched, for the request histogram.
    public static void route(String template) {
        if (serverEnabled) {
            ROUTE.set(template);
        }
    }

    /// A request is starting; answers the time to hand to [#requestEnded].
    public static long requestStarted() {
        return serverEnabled ? System.nanoTime() : 0L;
    }

    /// A request has been answered.
    public static void requestEnded(long started, String method, int status) {
        // The route is cleared on every path: a server that does not measure
        // still has its routers call route(), and a value left behind would be
        // recorded under the next request this thread serves.
        Object route = ROUTE.get();
        if (route != null) {
            ROUTE.set(null);
        }
        if (!serverEnabled || started == 0L) {
            return;
        }
        requestDuration.record((System.nanoTime() - started) / 1000000.0, route, method,
                Integer.valueOf(status));
    }

    /// A scheduled job has run.
    public static void jobRan(String job, long millis, boolean failed) {
        if (serverEnabled) {
            jobDuration.record(millis, job, failed ? "failure" : "success", null);
        }
    }

    // ------------------------------------------------------------------ views

    /// Every instrument and its points, for the management endpoint and MCP:
    /// `name -> {kind, description, unit, points`}.
    public static Map snapshot() {
        Map out = new LinkedHashMap();
        List all = instruments();
        for (Object element : all) {
            Instrument i = (Instrument) element;
            Map m = new LinkedHashMap();
            m.put("kind", kindName(i.getKind()));
            if (i.getDescription().length() > 0) {
                m.put("description", i.getDescription());
            }
            if (i.getUnit().length() > 0) {
                m.put("unit", i.getUnit());
            }
            m.put("points", i.points());
            out.put(i.getName(), m);
        }
        return out;
    }

    static String kindName(int kind) {
        switch (kind) {
            case Instrument.COUNTER: return "counter";
            case Instrument.UP_DOWN_COUNTER: return "upDownCounter";
            case Instrument.GAUGE: return "gauge";
            default: return "histogram";
        }
    }

    /// Every instrument in the Prometheus text exposition format.
    public static String prometheus() {
        StringBuilder sb = new StringBuilder();
        List all = instruments();
        for (Object entry : all) {
            Instrument i = (Instrument) entry;
            String name = promName(i.getName());
            String type;
            switch (i.getKind()) {
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
            if (i.getDescription().length() > 0) {
                sb.append("# HELP ").append(name).append(' ')
                        .append(escapeHelp(i.getDescription())).append('\n');
            }
            sb.append("# TYPE ").append(name).append(' ').append(type).append('\n');
            List points = i.points();
            for (Object element : points) {
                Map point = (Map) element;
                Map attributes = (Map) point.get("attributes");
                if (i.getKind() == Instrument.HISTOGRAM) {
                    List bounds = (List) point.get("bounds");
                    List buckets = (List) point.get("buckets");
                    long cumulative = 0;
                    for (int b = 0 ; b < buckets.size() ; b++) {
                        cumulative += ((Number) buckets.get(b)).longValue();
                        String le = b < bounds.size()
                                ? number(((Number) bounds.get(b)).doubleValue()) : "+Inf";
                        sb.append(name).append("_bucket");
                        labels(sb, attributes, le);
                        sb.append(' ').append(cumulative).append('\n');
                    }
                    sb.append(name).append("_sum");
                    labels(sb, attributes, null);
                    sb.append(' ').append(number(((Number) point.get("sum")).doubleValue()))
                            .append('\n');
                    sb.append(name).append("_count");
                    labels(sb, attributes, null);
                    sb.append(' ').append(point.get("count")).append('\n');
                } else {
                    sb.append(name);
                    labels(sb, attributes, null);
                    Object value = point.get("value");
                    // A counter's Long exactly, not through a double.
                    sb.append(' ').append(value instanceof Long ? String.valueOf(value)
                            : number(((Number) value).doubleValue())).append('\n');
                }
            }
        }
        return sb.toString();
    }

    private static void labels(StringBuilder sb, Map attributes, String le) {
        boolean any = (attributes != null && !attributes.isEmpty()) || le != null;
        if (!any) {
            return;
        }
        sb.append('{');
        boolean first = true;
        if (attributes != null) {
            Iterator it = attributes.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry e = (Map.Entry) it.next();
                if (!first) {
                    sb.append(',');
                }
                first = false;
                sb.append(promLabel(String.valueOf(e.getKey()))).append("=\"")
                        .append(escapeLabel(String.valueOf(e.getValue()))).append('"');
            }
        }
        if (le != null) {
            if (!first) {
                sb.append(',');
            }
            sb.append("le=\"").append(le).append('"');
        }
        sb.append('}');
    }

    static String promName(String name) {
        StringBuilder sb = new StringBuilder(name.length());
        for (int iter = 0 ; iter < name.length() ; iter++) {
            char c = name.charAt(iter);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_'
                    || c == ':' || (iter > 0 && c >= '0' && c <= '9');
            sb.append(ok ? c : '_');
        }
        return sb.toString();
    }

    /// A label name in Prometheus's alphabet, which is the metric-name alphabet
    /// WITHOUT the colon: `tenant:id` written as a label is a syntax error the
    /// scraper rejects the whole exposition for.
    static String promLabel(String name) {
        StringBuilder sb = new StringBuilder(name.length());
        for (int iter = 0 ; iter < name.length() ; iter++) {
            char c = name.charAt(iter);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_'
                    || (iter > 0 && c >= '0' && c <= '9');
            sb.append(ok ? c : '_');
        }
        return sb.toString();
    }

    private static String escapeLabel(String value) {
        StringBuilder sb = new StringBuilder(value.length());
        for (int iter = 0 ; iter < value.length() ; iter++) {
            char c = value.charAt(iter);
            if (c == '\\' || c == '"') {
                sb.append('\\').append(c);
            } else if (c == '\n') {
                sb.append("\\n");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String escapeHelp(String value) {
        StringBuilder sb = new StringBuilder(value.length());
        for (int iter = 0 ; iter < value.length() ; iter++) {
            char c = value.charAt(iter);
            if (c == '\\') {
                sb.append("\\\\");
            } else if (c == '\n') {
                sb.append("\\n");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String number(double d) {
        if (Double.isNaN(d)) {
            return "NaN";
        }
        if (Double.isInfinite(d)) {
            return d > 0 ? "+Inf" : "-Inf";
        }
        if (d == Math.floor(d) && Math.abs(d) < 1e15) {
            return Long.toString((long) d);
        }
        return Double.toString(d);
    }
}
