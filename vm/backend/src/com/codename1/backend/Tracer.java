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
import java.util.Map;

/**
 * Where spans come from and where they go.
 *
 * <p>The server is instrumented against this interface and nothing else, so a
 * binary that installs no tracer carries no tracing implementation: the
 * translator keeps a class only when something reaches it, and the one
 * implementation, {@code com.codename1.backend.otel.OtlpTracer}, is reached only
 * from the entry point the build generates for a project that asks for it (see
 * {@code @OpenTelemetry}).
 */
public interface Tracer {
    /**
     * Reads the settings and starts whatever exports spans. Called once, by
     * {@link Backend.Builder#start} or by the application before
     * {@link Tracing#install}.
     *
     * @return false when the configuration turned tracing off, in which case the
     *         tracer is not installed and nothing was started
     */
    boolean open(Config config) throws IOException;

    /**
     * A new span. Never null: a span the sampler declines is a non-recording one
     * that still carries its trace context.
     *
     * @param parent      the local parent, or null
     * @param traceparent a remote parent's {@code traceparent} header, used when
     *                    {@code parent} is null; null or malformed starts a new trace
     * @param tracestate  the remote parent's {@code tracestate}, or null
     */
    Span startSpan(String name, int kind, Span parent, String traceparent, String tracestate);

    /**
     * Blocks until what has ended so far is exported, or the time runs out. A
     * Lambda host freezes the process between invocations, so a background
     * exporter there only runs when something waits for it.
     */
    void flush(int timeoutMillis);

    /** Flushes, then stops the exporter. */
    void shutdown(int timeoutMillis);

    /**
     * The handler that relays client spans to the collector, or null when this
     * deployment does not offer one. The builder puts it ahead of the routers.
     */
    HttpServer.Handler relay();

    /** Adds this tracer's counters to a metrics snapshot. */
    void metrics(Map out);
}
