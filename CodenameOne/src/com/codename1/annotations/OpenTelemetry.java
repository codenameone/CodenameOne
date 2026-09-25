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
package com.codename1.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Traces the app with OpenTelemetry: every network request becomes a span, and
/// carries the W3C trace context to the server it reaches, so a tap in the app and
/// the backend work it caused are one trace.
///
/// Put it on the main class. The build generates a bootstrap that installs
/// `com.codename1.telemetry.Telemetry` before the app starts, and nothing else in
/// the app changes.
///
/// ```java
/// // Through the app's own Codename One backend (cn1.otel.relay=true there),
/// // which holds the collector's credentials:
/// @OpenTelemetry(relay = "https://api.example.com", serviceName = "shop-app")
/// public class ShopApp extends Lifecycle { ... }
///
/// // Or straight to a collector. The header ships inside the app, so use a token
/// // that can write traces and nothing else:
/// @OpenTelemetry(endpoint = "https://collector.example.com:4318",
///         headers = "Authorization: Api-Token dt0c01.ingest-only")
/// public class ShopApp extends Lifecycle { ... }
/// ```
///
/// Exactly one of `relay` and `endpoint` is set. Without the annotation the
/// telemetry classes are never referenced, so the app does not carry them.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface OpenTelemetry {
    /// The base URL of a Codename One backend that relays spans to the collector.
    String relay() default "";

    /// The base URL of an OTLP/HTTP collector to export to directly;
    /// `/v1/traces` is appended.
    String endpoint() default "";

    /// The `service.name` the app reports as. Defaults to the app's name.
    String serviceName() default "";

    /// Headers for a direct export, each `"Name: value"`.
    String[] headers() default {};

    /// The token a relay configured with `cn1.otel.relay.token` expects.
    String relayToken() default "";

    /// The share of new traces recorded, from 0 to 1.
    double sampleRatio() default 1.0;

    /// Whether a direct export is binary protobuf (the default) or JSON.
    boolean protobuf() default true;

    /// Further hosts to send the trace context to. See
    /// `TelemetryConfig.propagateTo`: on the web only the relay's host and these
    /// receive it, because the header needs CORS permission.
    String[] propagateTo() default {};

    /// Traces only while the user has granted analytics consent through
    /// `com.codename1.analytics.Analytics`. See
    /// `TelemetryConfig.requireAnalyticsConsent`.
    boolean requireAnalyticsConsent() default false;
}
