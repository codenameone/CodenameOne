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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// The published schema of the metrics the server records about itself: each
/// one's name, kind, unit, attributes and lifecycle, the runtimes that measure
/// it, and the Micrometer meter a Spring Boot dashboard reads for the same
/// figure. The names, units and attributes listed here are stable; a dashboard
/// or alert written against them keeps working across releases and across the
/// JVM and the packaged native server.
///
/// A figure a runtime cannot measure is never reported as zero there. Its
/// instrument is not registered, or -- for a pool gauge on a server with no
/// database -- reports no point, so it is absent from every view, and
/// [#describe] lists it as unavailable with the reason. The Micrometer meters
/// neither runtime has an equivalent for are listed by [#unmeasured].
///
/// `GET /manage/schema` serves [#describe] for the running server.
public final class MetricSchema {
    /// Measured by every runtime.
    public static final String ALWAYS = "always";
    /// Measured when the server has a database pool.
    public static final String DATABASE = "database";
    /// Measured on the JVM only; a packaged native server has no equivalent.
    public static final String JVM = "jvm";

    /// A value that only grows while the process runs, and starts again from
    /// zero when it restarts. Rate and increase functions apply.
    public static final String CUMULATIVE = "cumulative";
    /// A value read when the metrics are collected, which may rise or fall.
    public static final String INSTANT = "instant";

    /// One instrument of the schema.
    public static final class Entry {
        private final String name;
        private final int kind;
        private final String unit;
        private final String[] attributes;
        private final String temporality;
        private final String availability;
        private final String micrometer;
        private final String description;

        Entry(String name, int kind, String unit, String[] attributes, String temporality,
                String availability, String micrometer, String description) {
            this.name = name;
            this.kind = kind;
            this.unit = unit;
            this.attributes = attributes;
            this.temporality = temporality;
            this.availability = availability;
            this.micrometer = micrometer;
            this.description = description;
        }

        /// The instrument's name, as the OTLP exporter and the JSON view send it.
        public String getName() {
            return name;
        }

        /// One of the [Instrument] kinds.
        public int getKind() {
            return kind;
        }

        /// The UCUM unit, as OpenTelemetry writes it: `ms`, `s`, `By`, or a
        /// `{curly}` annotation for a count.
        public String getUnit() {
            return unit;
        }

        /// The attribute keys a point may carry, in order.
        public String[] getAttributes() {
            return attributes.clone();
        }

        /// [#CUMULATIVE] or [#INSTANT].
        public String getTemporality() {
            return temporality;
        }

        /// [#ALWAYS], [#DATABASE] or [#JVM].
        public String getAvailability() {
            return availability;
        }

        /// The Micrometer meter a Spring Boot application reports the same
        /// figure under, or null when it has none.
        public String getMicrometer() {
            return micrometer;
        }

        public String getDescription() {
            return description;
        }

        /// The name the Prometheus view exposes it under: dots become
        /// underscores, and a counter gains `_total`. A histogram's series add
        /// `_bucket`, `_sum` and `_count` to it.
        public String getPrometheusName() {
            String base = Metrics.promName(name);
            return kind == Instrument.COUNTER ? base + "_total" : base;
        }
    }

    private static final String[] NONE = new String[0];
    private static final List ENTRIES;
    private static final List UNMEASURED;

    static {
        List e = new ArrayList();
        e.add(new Entry("http.server.request.duration", Instrument.HISTOGRAM, "ms",
                new String[] {"http.route", "http.request.method", "http.response.status_code"},
                CUMULATIVE, ALWAYS, "http.server.requests",
                "Duration of HTTP server requests, by route template, method and status"));
        e.add(gauge("http.server.active_requests", "{request}", INSTANT, ALWAYS,
                "http.server.requests.active", "Requests being served"));
        e.add(gauge("http.server.open_connections", "{connection}", INSTANT, ALWAYS,
                "tomcat.connections.current", "Open client connections"));
        e.add(gauge("cn1.server.websocket_connections", "{connection}", INSTANT, ALWAYS,
                null, "Open websocket connections"));
        e.add(gauge("cn1.server.requests_served", "{request}", CUMULATIVE, ALWAYS, null,
                "Requests answered since start"));
        e.add(gauge("cn1.server.connections_refused", "{connection}", CUMULATIVE, ALWAYS,
                null, "Connections refused for being over the limit"));
        e.add(gauge("db.client.connection.count", "{connection}", INSTANT, DATABASE,
                "hikaricp.connections", "Open database connections, borrowed or idle"));
        e.add(gauge("db.client.connection.idle", "{connection}", INSTANT, DATABASE,
                "hikaricp.connections.idle", "Idle database connections"));
        e.add(gauge("db.client.connection.max", "{connection}", INSTANT, DATABASE,
                "hikaricp.connections.max", "Connections the pool may hold"));
        e.add(gauge("db.client.connection.pending_requests", "{request}", INSTANT, DATABASE,
                "hikaricp.connections.pending", "Borrowers waiting for a connection"));
        e.add(gauge("db.client.connection.timeouts", "{timeout}", CUMULATIVE, DATABASE,
                "hikaricp.connections.timeout",
                "Borrows that gave up waiting for a connection since start"));
        e.add(executor("cn1.task.queue_depth", "{task}", INSTANT, "executor.queued",
                "Tasks waiting for a thread"));
        e.add(executor("cn1.task.active", "{task}", INSTANT, "executor.active",
                "Tasks running"));
        e.add(executor("cn1.task.completed", "{task}", CUMULATIVE, "executor.completed",
                "Tasks finished since start"));
        e.add(executor("cn1.task.pool_size", "{thread}", INSTANT, "executor.pool.size",
                "Threads the executor runs tasks on"));
        e.add(new Entry("cn1.scheduler.run.duration", Instrument.HISTOGRAM, "ms",
                new String[] {"cn1.job", "cn1.outcome"}, CUMULATIVE, ALWAYS,
                "tasks.scheduled.execution",
                "Duration of scheduled job runs, by job and outcome (success or failure)"));
        e.add(gauge("process.runtime.memory.used", "By", INSTANT, ALWAYS, "jvm.memory.used",
                "Heap in use"));
        e.add(gauge("process.runtime.memory.committed", "By", INSTANT, ALWAYS,
                "jvm.memory.committed", "Heap the runtime holds from the system"));
        e.add(gauge("process.runtime.memory.limit", "By", INSTANT, JVM, "jvm.memory.max",
                "The most heap the process may use"));
        e.add(gauge("process.uptime", "s", INSTANT, ALWAYS, "process.uptime",
                "Seconds since the process started"));
        e.add(gauge("jvm.thread.count", "{thread}", INSTANT, JVM, "jvm.threads.live",
                "Live platform threads"));
        e.add(gauge("jvm.class.count", "{class}", INSTANT, JVM, "jvm.classes.loaded",
                "Classes currently loaded"));
        e.add(gauge("jvm.cpu.count", "{cpu}", INSTANT, JVM, "system.cpu.count",
                "Processors available to the runtime"));
        e.add(gauge("cn1.jvm.gc.collections", "{collection}", CUMULATIVE, JVM,
                "jvm.gc.pause", "Garbage collections since start (the pause timer's count)"));
        e.add(gauge("cn1.jvm.gc.time", "ms", CUMULATIVE, JVM, "jvm.gc.pause",
                "Time spent collecting since start (the pause timer's total)"));
        ENTRIES = Collections.unmodifiableList(e);

        List u = new ArrayList();
        u.add(unmeasured("jvm.gc.pause", "Not as a histogram of single pauses by collector and "
                + "cause; cn1.jvm.gc.collections and cn1.jvm.gc.time carry its count and total "
                + "on the JVM, and ParparVM's collector runs concurrently, without pauses to time."));
        u.add(unmeasured("jvm.gc.memory.allocated", "Neither runtime counts allocated bytes."));
        u.add(unmeasured("jvm.gc.memory.promoted", "Neither runtime has generations to promote "
                + "between that the server reads."));
        u.add(unmeasured("jvm.memory.used{area,id}", "Memory is reported for the heap as a whole, "
                + "not per memory pool."));
        u.add(unmeasured("jvm.buffer.memory.used", "Direct buffers are not tracked."));
        u.add(unmeasured("jvm.threads.states", "Thread states are not tracked; requests run on "
                + "virtual threads, which http.server.active_requests counts."));
        u.add(unmeasured("process.cpu.usage", "CPU time is not read on either runtime."));
        u.add(unmeasured("system.cpu.usage", "CPU time is not read on either runtime."));
        u.add(unmeasured("system.load.average.1m", "The system load is not read."));
        u.add(unmeasured("process.files.open", "Open descriptors are not counted; "
                + "http.server.open_connections counts the client sockets."));
        u.add(unmeasured("hikaricp.connections.usage", "Connection hold times are not timed."));
        u.add(unmeasured("hikaricp.connections.acquire", "Borrow waits are not timed; "
                + "db.client.connection.pending_requests counts the borrowers waiting."));
        u.add(unmeasured("logback.events", "Log events are not counted."));
        UNMEASURED = Collections.unmodifiableList(u);
    }

    private MetricSchema() {
    }

    private static Entry gauge(String name, String unit, String temporality,
            String availability, String micrometer, String description) {
        return new Entry(name, Instrument.GAUGE, unit, NONE, temporality, availability,
                micrometer, description);
    }

    private static Entry executor(String name, String unit, String temporality,
            String micrometer, String description) {
        return new Entry(name, Instrument.GAUGE, unit, new String[] {"cn1.executor"},
                temporality, ALWAYS, micrometer, description + ", by executor");
    }

    private static String[] unmeasured(String micrometer, String reason) {
        return new String[] {micrometer, reason};
    }

    /// Every instrument of the schema, in a fixed order.
    public static List entries() {
        return ENTRIES;
    }

    /// The schema's instrument of this name, or null.
    public static Entry get(String name) {
        for (Object element : ENTRIES) {
            Entry e = (Entry) element;
            if (e.name.equals(name)) {
                return e;
            }
        }
        return null;
    }

    /// The Micrometer meters a Spring Boot dashboard may read that no runtime
    /// here reports, each as `{meter, reason}`.
    public static List unmeasured() {
        return UNMEASURED;
    }

    /// The runtime this process is: `jvm` or `native`.
    public static String runtime() {
        return com.codename1.impl.backend.RuntimeGauges.runtime();
    }

    /// Whether this process reports the instrument now: it is registered and,
    /// for a gauge without attributes, has its point to report. A gauge with
    /// attributes has a point per value -- an executor, say -- and none while
    /// there is no such value yet, which is a measured "none". False for every
    /// instrument before a server has started.
    public static boolean isAvailable(Entry entry) {
        Instrument i = Metrics.get(entry.name);
        if (i == null) {
            return false;
        }
        return entry.kind != Instrument.GAUGE || entry.attributes.length > 0
                || !i.points().isEmpty();
    }

    /// The schema as the management endpoint serves it: the runtime, every
    /// instrument with whether this process reports it and, when it does not,
    /// why, and the Micrometer meters nothing here reports.
    public static Map describe() {
        Map out = new LinkedHashMap();
        out.put("runtime", runtime());
        List metrics = new ArrayList();
        for (Object element : ENTRIES) {
            Entry e = (Entry) element;
            Map m = new LinkedHashMap();
            m.put("name", e.name);
            m.put("prometheus", e.getPrometheusName());
            m.put("kind", Metrics.kindName(e.kind));
            m.put("unit", e.unit);
            List attributes = new ArrayList();
            for (String a : e.attributes) {
                attributes.add(a);
            }
            m.put("attributes", attributes);
            m.put("temporality", e.temporality);
            m.put("availability", e.availability);
            if (e.micrometer != null) {
                m.put("micrometer", e.micrometer);
            }
            m.put("description", e.description);
            boolean available = isAvailable(e);
            m.put("available", Boolean.valueOf(available));
            if (!available) {
                m.put("reason", reason(e));
            }
            metrics.add(m);
        }
        out.put("metrics", metrics);
        List unmeasured = new ArrayList();
        for (Object element : UNMEASURED) {
            String[] u = (String[]) element;
            Map m = new LinkedHashMap();
            m.put("micrometer", u[0]);
            m.put("reason", u[1]);
            unmeasured.add(m);
        }
        out.put("unmeasured", unmeasured);
        return out;
    }

    private static String reason(Entry e) {
        if (JVM.equals(e.availability) && !"jvm".equals(runtime())) {
            return "Measured on the JVM only; this runtime has no equivalent.";
        }
        if (DATABASE.equals(e.availability)) {
            return "This server has no database pool.";
        }
        if (Metrics.get(e.name) == null) {
            return "The server is not recording its own metrics.";
        }
        return "The runtime could not read it.";
    }
}
