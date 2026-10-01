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
/// Metrics for a backend: counters, gauges and histograms named after the
/// OpenTelemetry semantic conventions.
///
/// [Metrics] holds every instrument in the process. The server records its own
/// -- request durations, open connections, database pool use, scheduled runs --
/// and an application adds more through the `Timed`, `Counted` and
/// `ManagedResource` annotations, or by creating instruments directly. A
/// [MetricReader] takes them from there: the OTLP exporter in
/// `com.codename1.backend.otel`, or the management endpoints' JSON and
/// Prometheus text.
///
/// Server code: this package is not available in the app.
package com.codename1.backend.metrics;
