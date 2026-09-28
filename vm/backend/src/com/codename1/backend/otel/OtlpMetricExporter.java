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
package com.codename1.backend.otel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.codename1.backend.Config;
import com.codename1.backend.Web;
import com.codename1.backend.metrics.Instrument;
import com.codename1.backend.metrics.MetricReader;
import com.codename1.backend.metrics.Metrics;

/// Exports [Metrics] over OTLP/HTTP, every
/// `cn1.otel.metrics.intervalMillis` (OTEL_METRIC_EXPORT_INTERVAL, one minute
/// by default), with cumulative temporality.
///
/// Configured like the tracer, with the metrics-specific settings taking
/// precedence the way the OpenTelemetry specification says:
///
/// | Key | Environment | Default |
/// |---|---|---|
/// | `cn1.otel.metrics.endpoint` | `OTEL_EXPORTER_OTLP_METRICS_ENDPOINT` | `cn1.otel.endpoint` + `/v1/metrics` |
/// | `cn1.otel.metrics.headers` | `OTEL_EXPORTER_OTLP_METRICS_HEADERS` | `cn1.otel.headers` |
/// | `cn1.otel.metrics.protocol` | `OTEL_EXPORTER_OTLP_METRICS_PROTOCOL` | `cn1.otel.protocol` |
/// | `cn1.otel.metrics.enabled` | | `true` |
///
/// `OTEL_SDK_DISABLED=true` turns it off with the tracer.
public final class OtlpMetricExporter implements MetricReader {
    public static final String ENABLED = "cn1.otel.metrics.enabled";
    public static final String ENDPOINT = "cn1.otel.metrics.endpoint";
    public static final String HEADERS = "cn1.otel.metrics.headers";
    public static final String PROTOCOL = "cn1.otel.metrics.protocol";
    public static final String INTERVAL = "cn1.otel.metrics.intervalMillis";

    private final String defaultServiceName;
    private String endpoint;
    private List headers = new ArrayList();
    private boolean protobuf;
    private int intervalMillis;
    private Map resource;
    private Thread thread;
    private final Object lock = new Object();
    /// The lifecycle of one open(): a builder started again reuses this reader,
    /// and a fresh Run is what keeps the new thread from inheriting the stopped
    /// one's flags -- and an old thread still finishing its last export from
    /// being revived by the new ones.
    private Run run;
    /// Exporters open in this process; see open().
    private static final List OPEN = new ArrayList();
    private long exports;
    private long failures;
    private String lastError;

    public OtlpMetricExporter(String defaultServiceName) {
        this.defaultServiceName = defaultServiceName;
    }

    @Override
    public boolean open(Config config) throws IOException {
        if (config.getBoolean(OtlpTracer.DISABLED, false) || !config.getBoolean(ENABLED, true)) {
            return false;
        }
        String protocol = config.get(PROTOCOL, config.get(OtlpTracer.PROTOCOL,
                "http/protobuf")).trim();
        if ("http/protobuf".equals(protocol)) {
            protobuf = true;
        } else if ("http/json".equals(protocol)) {
            protobuf = false;
        } else {
            throw new IOException(PROTOCOL + " is '" + protocol + "'; this server exports "
                    + "OTLP over HTTP, so use http/protobuf or http/json");
        }
        String target = config.get(ENDPOINT);
        if (target == null || target.trim().length() == 0) {
            target = OtlpTracer.appendSignalPath(
                    config.get(OtlpTracer.ENDPOINT, "http://localhost:4318").trim(),
                    "/v1/metrics");
        }
        endpoint = target.trim();
        if (!OtlpTracer.hasHttpAuthority(endpoint)) {
            throw new IOException("The metrics endpoint must be an http or https URL naming "
                    + "a host and is '" + BatchExporter.redact(endpoint) + "'");
        }
        headers = new ArrayList();
        String own = config.get(HEADERS);
        OtlpTracer.parseHeaders(own != null ? own : config.get(OtlpTracer.HEADERS), headers);
        intervalMillis = OtlpTracer.positive(config, INTERVAL, 60000);
        resource = OtlpTracer.resource(config, defaultServiceName);
        synchronized (OPEN) {
            // The instruments are the process's -- every server's requests, jobs
            // and gauges in one set -- so two exporters with different resources
            // would each send the SAME numbers under their own service name, or
            // to their own collector, and both would be wrong. One process, one
            // metrics identity; the same one twice is fine.
            //
            // The same identity includes how it is sent: only one of them
            // exports, so a second server's API-key header -- the tenant on a
            // shared collector -- or its protocol would be silently ignored,
            // its numbers sent under the first one's credentials, and the
            // credentials would change again at a hand-over. Compared as sets:
            // the order the headers are listed in means nothing.
            for (Object element : OPEN) {
                OtlpMetricExporter other = (OtlpMetricExporter) element;
                if (other != this && (!other.resource.equals(resource) //NOPMD CompareObjectsWithEquals - the exporter itself, by identity
                        || !other.endpoint.equals(endpoint))) {
                    throw new IOException("Another server in this process already exports "
                            + "metrics as a different service or to a different collector. "
                            + "Metrics are per process, so they would be reported twice "
                            + "under two names; give both servers the same OpenTelemetry "
                            + "service and endpoint, or set " + ENABLED + "=false on one.");
                }
                if (other != this && (other.protobuf != protobuf //NOPMD CompareObjectsWithEquals - the exporter itself, by identity
                        || !sameHeaders(other.headers, headers))) {
                    // The values are never printed: they are usually credentials.
                    throw new IOException("Another server in this process already exports "
                            + "metrics to this collector with different " + HEADERS + " or "
                            + PROTOCOL + ". Metrics are per process and only one server "
                            + "sends them, so this one's settings would be ignored; give "
                            + "both servers the same headers and protocol, or set "
                            + ENABLED + "=false on one.");
                }
            }
            if (OPEN.contains(this)) {
                // A second thread would export the same streams beside the first,
                // and shutdown() tracks one run: the other would never stop.
                throw new IOException("This metrics exporter is already open; give each "
                        + "server its own, or open it once");
            }
            OPEN.add(this);
            if (OPEN.size() > 1) {
                // The same identity as the one already exporting: sending the
                // process's instruments again would give the collector every point
                // twice and run every gauge callback twice. This one waits, and
                // takes over if that one shuts down first.
                return true;
            }
        }
        startExporting();
        return true;
    }

    /// Whether two header lists name the same headers, in any order.
    private static boolean sameHeaders(List a, List b) {
        return new java.util.HashSet(a).equals(new java.util.HashSet(b));
    }

    /// Starts exporting for a leader that stopped -- but only while this one is
    /// still open and first in line, checked and started under the same lock
    /// its own shutdown() takes: a successor that shut down meanwhile would
    /// otherwise get a thread nothing ever stops, exporting after both servers
    /// are gone.
    void takeOver() {
        takeOver(null);
    }

    /// [#takeOver()], its thread first waiting for `predecessor` to exit.
    void takeOver(Thread predecessor) {
        synchronized (OPEN) {
            if (OPEN.isEmpty() || OPEN.get(0) != this) { //NOPMD CompareObjectsWithEquals - the exporter itself, by identity
                return;
            }
            synchronized (lock) {
                if (run != null && !run.stopping) {
                    return;
                }
            }
            startExporting(predecessor);
        }
    }

    /// Starts this exporter's thread: it is the one exporting the process's metrics.
    private void startExporting() {
        startExporting(null);
    }

    private void startExporting(final Thread predecessor) {
        final Run mine = new Run();
        Thread started = new Thread(new Runnable() {
            @Override
            public void run() {
                if (predecessor != null) {
                    try {
                        predecessor.join();
                    } catch (InterruptedException err) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                loop(mine);
            }
        }, "cn1-otel-metrics");
        started.setDaemon(true);
        synchronized (lock) {
            run = mine;
            thread = started;
        }
        started.start();
    }

    private void loop(Run mine) {
        while (true) {
            synchronized (lock) {
                long deadline = System.currentTimeMillis() + intervalMillis;
                while (!mine.stopping) {
                    long left = deadline - System.currentTimeMillis();
                    if (left <= 0) {
                        break;
                    }
                    try {
                        lock.wait(left);
                    } catch (InterruptedException err) {
                        return;
                    }
                }
                if (mine.stopping) {
                    if (!mine.finalExport) {
                        return;
                    }
                    break;
                }
            }
            export();
        }
        // On THIS thread, after any periodic export still in flight, so the two
        // never overlap -- and shutdown() only waits for it as long as it was
        // told to.
        export();
    }

    /// Sends one export now. Answers whether the collector accepted it.
    public boolean export() {
        try {
            Map request = request(resource, Metrics.instruments(), System.currentTimeMillis());
            byte[] body = protobuf ? OtlpSchema.metricsProtobuf(request)
                    : OtlpSchema.json(request);
            List lines = new ArrayList(headers.size() + 1);
            lines.add("Content-Type: " + (protobuf ? "application/x-protobuf"
                    : "application/json"));
            lines.addAll(headers);
            Web.Result result = Web.request("POST", endpoint, lines, body);
            int status = result.getStatus();
            synchronized (lock) {
                exports++;
                if (status < 200 || status >= 300) {
                    failures++;
                    lastError = "the collector answered " + status;
                    return false;
                }
            }
            return true;
        } catch (Throwable err) {
            // Throwable, not Exception: this runs on the exporter's only thread,
            // and an Error out of an instrument or the encoder must cost one export,
            // not every export after it.
            synchronized (lock) {
                failures++;
                lastError = BatchExporter.bounded("could not export metrics: "
                        + err);
                if (failures == 1 || failures % 100 == 0) {
                    System.err.println(lastError);
                }
            }
            return false;
        }
    }

    @Override
    public void shutdown(int timeoutMillis) {
        OtlpMetricExporter successor = null;
        synchronized (OPEN) {
            boolean leading = !OPEN.isEmpty() && OPEN.get(0) == this; //NOPMD CompareObjectsWithEquals - the exporter itself, by identity
            OPEN.remove(this);
            if (leading && !OPEN.isEmpty()) {
                successor = (OtlpMetricExporter) OPEN.get(0);
            }
        }
        Thread exporter = null;
        synchronized (lock) {
            if (run != null && !run.stopping) {
                run.stopping = true;
                // One last export, so the counts of the final minute are not lost
                // -- made by the exporter thread, which is bounded by the join
                // below rather than by the HTTP client's own timeouts.
                run.finalExport = timeoutMillis > 0;
                exporter = thread;
                lock.notifyAll();
            }
        }
        if (successor != null) {
            // The one that waited behind this, with the same identity, exports
            // from now on; the process's metrics are not left unreported while
            // another server is still running. It starts only once THIS thread has
            // exited -- its periodic or final export may still be in flight, and
            // the two at once would send the process's stream twice.
            successor.takeOver(exporter);
        }
        if (exporter != null && timeoutMillis > 0) {
            try {
                exporter.join(timeoutMillis);
            } catch (InterruptedException err) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /// Whether one open()'s thread should stop, and whether it exports once more first.
    private static final class Run {
        boolean stopping;
        boolean finalExport;
    }

    /// Exports attempted, failures, and the last error, for the management view.
    public Map status() {
        Map out = new LinkedHashMap();
        synchronized (lock) {
            out.put("endpoint", BatchExporter.redact(endpoint));
            out.put("exports", Long.valueOf(exports));
            out.put("failures", Long.valueOf(failures));
            if (lastError != null) {
                out.put("lastError", lastError);
            }
        }
        return out;
    }

    /// The ExportMetricsServiceRequest tree for these instruments, at `now`.
    static Map request(Map resource, List instruments, long nowMillis) {
        String start = nanos(Metrics.startTimeMillis());
        String time = nanos(nowMillis);
        List metrics = new ArrayList(instruments.size());
        for (Object item : instruments) {
            Instrument instrument = (Instrument) item;
            Map metric = new LinkedHashMap();
            metric.put("name", instrument.getName());
            if (instrument.getDescription().length() > 0) {
                metric.put("description", instrument.getDescription());
            }
            if (instrument.getUnit().length() > 0) {
                metric.put("unit", instrument.getUnit());
            }
            List points = instrument.points();
            List dataPoints = new ArrayList(points.size());
            for (Object entry : points) {
                Map point = (Map) entry;
                Map dp = new LinkedHashMap();
                dp.put("attributes", OtlpTracer.keyValues((Map) point.get("attributes")));
                if (instrument.getKind() != Instrument.GAUGE) {
                    dp.put("startTimeUnixNano", start);
                }
                dp.put("timeUnixNano", time);
                if (instrument.getKind() == Instrument.HISTOGRAM) {
                    dp.put("count", String.valueOf(point.get("count")));
                    dp.put("sum", jsonDouble(point.get("sum")));
                    List buckets = (List) point.get("buckets");
                    List counts = new ArrayList(buckets.size());
                    for (Object element : buckets) {
                        counts.add(String.valueOf(element));
                    }
                    dp.put("bucketCounts", counts);
                    dp.put("explicitBounds", point.get("bounds"));
                    if (point.get("min") != null) {
                        dp.put("min", jsonDouble(point.get("min")));
                        dp.put("max", jsonDouble(point.get("max")));
                    }
                } else {
                    Object value = point.get("value");
                    if (value instanceof Double && ((Double) value).isNaN()) {
                        continue;
                    }
                    if (value instanceof Long) {
                        // as_int, exactly: through asDouble a counter past 2^53
                        // would be rounded. A string, as OTLP JSON writes int64.
                        dp.put("asInt", String.valueOf(value));
                    } else {
                        dp.put("asDouble", jsonDouble(value));
                    }
                }
                dataPoints.add(dp);
            }
            Map data = new LinkedHashMap();
            data.put("dataPoints", dataPoints);
            switch (instrument.getKind()) {
                case Instrument.GAUGE:
                    metric.put("gauge", data);
                    break;
                case Instrument.HISTOGRAM:
                    data.put("aggregationTemporality", Integer.valueOf(2));
                    metric.put("histogram", data);
                    break;
                default:
                    data.put("aggregationTemporality", Integer.valueOf(2));
                    data.put("isMonotonic", Boolean.valueOf(
                            instrument.getKind() == Instrument.COUNTER));
                    metric.put("sum", data);
            }
            metrics.add(metric);
        }
        Map scope = new LinkedHashMap();
        scope.put("name", "com.codename1.backend");
        Map scopeMetrics = new LinkedHashMap();
        scopeMetrics.put("scope", scope);
        scopeMetrics.put("metrics", metrics);
        List scopes = new ArrayList(1);
        scopes.add(scopeMetrics);
        Map resourceMetrics = new LinkedHashMap();
        resourceMetrics.put("resource", resource);
        resourceMetrics.put("scopeMetrics", scopes);
        List all = new ArrayList(1);
        all.add(resourceMetrics);
        Map request = new LinkedHashMap();
        request.put("resourceMetrics", all);
        return request;
    }

    private static String nanos(long millis) {
        return String.valueOf(millis) + "000000";
    }

    /// A double as OTLP/JSON carries it. The protobuf JSON mapping writes the
    /// non-finite values as the strings "Infinity", "-Infinity" and "NaN"; the
    /// generic writer would put null there instead, which a collector reads as an
    /// unset value -- or rejects, with the whole export. An infinite gauge
    /// reading, or an infinite observation in a histogram's sum, min or max,
    /// is a real value to report. The same tree feeds the protobuf encoder, which
    /// parses a double field given as a string, so both encodings carry it.
    static Object jsonDouble(Object value) {
        if (value instanceof Double || value instanceof Float) {
            double d = ((Number) value).doubleValue();
            if (Double.isNaN(d)) {
                return "NaN";
            }
            if (Double.isInfinite(d)) {
                return d > 0 ? "Infinity" : "-Infinity";
            }
        }
        return value;
    }
}
