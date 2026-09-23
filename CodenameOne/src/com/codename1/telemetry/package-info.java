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
/// OpenTelemetry tracing for the app, exported over OTLP/HTTP.
///
/// Installed by the build from `@OpenTelemetry` on the main class, or by hand with
/// {@link com.codename1.telemetry.Telemetry#install(com.codename1.telemetry.TelemetryConfig)}.
/// Every {@link com.codename1.io.ConnectionRequest} then becomes a span and sends the
/// W3C `traceparent` header, so a Codename One backend -- or any service that speaks
/// W3C Trace Context -- continues the app's trace instead of starting its own.
///
/// Spans leave the app either through the app's own backend, which relays them to
/// the collector with credentials the app never holds, or directly to a collector;
/// see {@link com.codename1.telemetry.TelemetryConfig}.
package com.codename1.telemetry;
