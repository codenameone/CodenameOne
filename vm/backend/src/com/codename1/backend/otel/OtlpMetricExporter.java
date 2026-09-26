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

/**
 * Exports {@link Metrics} over OTLP/HTTP, every
 * {@code cn1.otel.metrics.intervalMillis} (OTEL_METRIC_EXPORT_INTERVAL, one minute
 * by default), with cumulative temporality.
 *
 * <p>Configured like the tracer, with the metrics-specific settings taking
 * precedence the way the OpenTelemetry specification says:
 *
 * <table>
 *   <tr><th>key</th><th>environment</th><th>default</th></tr>
 *   <tr><td>cn1.otel.metrics.endpoint</td><td>OTEL_EXPORTER_OTLP_METRICS_ENDPOINT</td>
 *       <td>cn1.otel.endpoint + /v1/metrics</td></tr>
 *   <tr><td>cn1.otel.metrics.headers</td><td>OTEL_EXPORTER_OTLP_METRICS_HEADERS</td>
 *       <td>cn1.otel.headers</td></tr>
 *   <tr><td>cn1.otel.metrics.protocol</td><td>OTEL_EXPORTER_OTLP_METRICS_PROTOCOL</td>
 *       <td>cn1.otel.protocol</td></tr>
 *   <tr><td>cn1.otel.metrics.enabled</td><td></td><td>true</td></tr>
 * </table>
 *
 * <p>{@code OTEL_SDK_DISABLED=true} turns it off with the tracer.
 */
public final class OtlpMetricExporter implements MetricReader {
    public static final String ENABLED = "cn1.otel.metrics.enabled";
    public static final String ENDPOINT = "cn1.otel.metrics.endpoint";
    public static final String HEADERS = "cn1.otel.metrics.headers";
    public static final String PROTOCOL = "cn1.otel.metrics.protocol";
    public static final String INTERVAL = "cn1.otel.metrics.intervalMillis";

    private final String defaultServiceName;
    private String endpoint;
    private List headers;
    private boolean protobuf;
    private int intervalMillis;
    private Map resource;
    private Thread thread;
    private final Object lock = new Object();
    private boolean stopping;
    /** Whether the exporter thread sends one last export before it ends. */
    private boolean finalExport;
    private long exports;
    private long failures;
    private String lastError;

    public OtlpMetricExporter(String defaultServiceName) {
        this.defaultServiceName = defaultServiceName;
    }

    public boolean open(Config config) throws IOException {
        if(config.getBoolean(OtlpTracer.DISABLED, false) || !config.getBoolean(ENABLED, true)) {
            return false;
        }
        String protocol = config.get(PROTOCOL, config.get(OtlpTracer.PROTOCOL,
                "http/protobuf")).trim();
        if("http/protobuf".equals(protocol)) {
            protobuf = true;
        } else if("http/json".equals(protocol)) {
            protobuf = false;
        } else {
            throw new IOException(PROTOCOL + " is '" + protocol + "'; this server exports "
                    + "OTLP over HTTP, so use http/protobuf or http/json");
        }
        String target = config.get(ENDPOINT);
        if(target == null || target.trim().length() == 0) {
            target = OtlpTracer.appendSignalPath(
                    config.get(OtlpTracer.ENDPOINT, "http://localhost:4318").trim(),
                    "/v1/metrics");
        }
        endpoint = target.trim();
        if(!OtlpTracer.hasHttpAuthority(endpoint)) {
            throw new IOException("The metrics endpoint must be an http or https URL naming "
                    + "a host and is '" + BatchExporter.redact(endpoint) + "'");
        }
        headers = new ArrayList();
        String own = config.get(HEADERS);
        OtlpTracer.parseHeaders(own != null ? own : config.get(OtlpTracer.HEADERS), headers);
        intervalMillis = OtlpTracer.positive(config, INTERVAL, 60000);
        resource = OtlpTracer.resource(config, defaultServiceName);
        thread = new Thread(new Runnable() {
            public void run() {
                loop();
            }
        }, "cn1-otel-metrics");
        thread.setDaemon(true);
        thread.start();
        return true;
    }

    private void loop() {
        while(true) {
            synchronized(lock) {
                long deadline = System.currentTimeMillis() + intervalMillis;
                while(!stopping) {
                    long left = deadline - System.currentTimeMillis();
                    if(left <= 0) {
                        break;
                    }
                    try {
                        lock.wait(left);
                    } catch (InterruptedException err) {
                        return;
                    }
                }
                if(stopping) {
                    if(!finalExport) {
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

    /** Sends one export now. Answers whether the collector accepted it. */
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
            synchronized(lock) {
                exports++;
                if(status < 200 || status >= 300) {
                    failures++;
                    lastError = "the collector answered " + status;
                    return false;
                }
            }
            return true;
        } catch (Exception err) {
            synchronized(lock) {
                failures++;
                lastError = BatchExporter.bounded("could not export metrics: "
                        + err.getMessage());
                if(failures == 1 || failures % 100 == 0) {
                    System.err.println(lastError);
                }
            }
            return false;
        }
    }

    public void shutdown(int timeoutMillis) {
        synchronized(lock) {
            if(stopping) {
                return;
            }
            stopping = true;
            // One last export, so the counts of the final minute are not lost --
            // made by the exporter thread, which is bounded by the join below
            // rather than by the HTTP client's own connect and read timeouts.
            finalExport = timeoutMillis > 0;
            lock.notifyAll();
        }
        if(thread != null && timeoutMillis > 0) {
            try {
                thread.join(timeoutMillis);
            } catch (InterruptedException err) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /** Exports attempted, failures, and the last error, for the management view. */
    public Map status() {
        Map out = new LinkedHashMap();
        synchronized(lock) {
            out.put("endpoint", BatchExporter.redact(endpoint));
            out.put("exports", new Long(exports));
            out.put("failures", new Long(failures));
            if(lastError != null) {
                out.put("lastError", lastError);
            }
        }
        return out;
    }

    /** The ExportMetricsServiceRequest tree for these instruments, at {@code now}. */
    static Map request(Map resource, List instruments, long nowMillis) {
        String start = nanos(Metrics.startTimeMillis());
        String time = nanos(nowMillis);
        List metrics = new ArrayList(instruments.size());
        for(int iter = 0 ; iter < instruments.size() ; iter++) {
            Instrument instrument = (Instrument)instruments.get(iter);
            Map metric = new LinkedHashMap();
            metric.put("name", instrument.getName());
            if(instrument.getDescription().length() > 0) {
                metric.put("description", instrument.getDescription());
            }
            if(instrument.getUnit().length() > 0) {
                metric.put("unit", instrument.getUnit());
            }
            List points = instrument.points();
            List dataPoints = new ArrayList(points.size());
            for(int p = 0 ; p < points.size() ; p++) {
                Map point = (Map)points.get(p);
                Map dp = new LinkedHashMap();
                dp.put("attributes", OtlpTracer.keyValues((Map)point.get("attributes")));
                if(instrument.getKind() != Instrument.GAUGE) {
                    dp.put("startTimeUnixNano", start);
                }
                dp.put("timeUnixNano", time);
                if(instrument.getKind() == Instrument.HISTOGRAM) {
                    dp.put("count", String.valueOf(point.get("count")));
                    dp.put("sum", point.get("sum"));
                    List buckets = (List)point.get("buckets");
                    List counts = new ArrayList(buckets.size());
                    for(int b = 0 ; b < buckets.size() ; b++) {
                        counts.add(String.valueOf(buckets.get(b)));
                    }
                    dp.put("bucketCounts", counts);
                    dp.put("explicitBounds", point.get("bounds"));
                    if(point.get("min") != null) {
                        dp.put("min", point.get("min"));
                        dp.put("max", point.get("max"));
                    }
                } else {
                    Object value = point.get("value");
                    if(value instanceof Double && ((Double)value).isNaN()) {
                        continue;
                    }
                    dp.put("asDouble", value);
                }
                dataPoints.add(dp);
            }
            Map data = new LinkedHashMap();
            data.put("dataPoints", dataPoints);
            switch(instrument.getKind()) {
                case Instrument.GAUGE:
                    metric.put("gauge", data);
                    break;
                case Instrument.HISTOGRAM:
                    data.put("aggregationTemporality", new Integer(2));
                    metric.put("histogram", data);
                    break;
                default:
                    data.put("aggregationTemporality", new Integer(2));
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
}
