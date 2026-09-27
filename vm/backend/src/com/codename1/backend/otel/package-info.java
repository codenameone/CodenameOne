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
/// OpenTelemetry tracing and metrics for a backend, exported over OTLP/HTTP
/// without an OpenTelemetry library.
///
/// Most servers never name these classes: the `OpenTelemetry` annotation in
/// `com.codename1.backend.annotations` has the build wire an [OtlpTracer] into the
/// generated entry point. Code that assembles its own server installs one with
/// `com.codename1.backend.Tracing.install`, and every request, outbound call and
/// database statement is then exported as a span to the collector the deployment
/// names. [OtlpMetricExporter] sends the process's metrics to the same collector
/// on an interval.
///
/// Server code: this package is not available in the app.
package com.codename1.backend.otel;
