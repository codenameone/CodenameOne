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
package com.codename1.telemetry;

import java.util.ArrayList;
import java.util.List;

/// Where [Telemetry] sends spans, and how.
///
/// Two ways to reach a collector:
///
/// - **Relay** (the default): the app posts OTLP/JSON to its own backend -- a
///   Codename One backend with `cn1.otel.relay=true` -- which adds the collector's
///   credentials and forwards the spans. Nothing secret ships in the app, and a
///   browser never has to reach a third-party collector, so the JavaScript port
///   needs no CORS setup on it.
/// - **Direct**: the app posts to an OTLP/HTTP collector itself. Simpler to stand
///   up, but whatever header authenticates the export is inside the app package,
///   and anything inside an app package is public. Use an ingest token scoped to
///   writing traces and nothing else.
///
/// ```java
/// Telemetry.install(new TelemetryConfig()
///         .relay("https://api.example.com")
///         .serviceName("shop-app"));
/// ```
public final class TelemetryConfig {
    /// How spans leave the app.
    public enum Mode {
        /// Through the app's own backend, which forwards to the collector.
        RELAY,
        /// Straight to an OTLP/HTTP collector.
        DIRECT
    }

    Mode mode = Mode.RELAY;
    String endpoint;
    String serviceName;
    String relayToken;
    boolean protobuf = true;
    double sampleRatio = 1;
    int batchSize = 32;
    int flushIntervalMillis = 10000;
    final List<String[]> headers = new ArrayList<String[]>();
    final List<String> propagateTo = new ArrayList<String>();
    boolean propagateToAll;
    boolean requireAnalyticsConsent;

    /// Sends spans through a Codename One backend's relay.
    ///
    /// #### Parameters
    ///
    /// - `backendUrl`: the backend's base URL, such as `https://api.example.com`;
    ///   `/otel/v1/traces` is appended. A URL that already names a path ending in
    ///   `/v1/traces` is used as it is, for a relay mounted elsewhere.
    ///
    /// #### Returns
    ///
    /// this configuration
    public TelemetryConfig relay(String backendUrl) {
        this.mode = Mode.RELAY;
        this.endpoint = backendUrl;
        return this;
    }

    /// Sends spans straight to an OTLP/HTTP collector.
    ///
    /// #### Parameters
    ///
    /// - `collectorUrl`: the collector's base URL, `/v1/traces` appended unless
    ///   it is already there -- the same rule `OTEL_EXPORTER_OTLP_ENDPOINT`
    ///   follows everywhere
    ///
    /// #### Returns
    ///
    /// this configuration
    public TelemetryConfig direct(String collectorUrl) {
        this.mode = Mode.DIRECT;
        this.endpoint = collectorUrl;
        return this;
    }

    /// The `service.name` the app's spans are reported under. Defaults to the
    /// app's name.
    ///
    /// #### Returns
    ///
    /// this configuration
    public TelemetryConfig serviceName(String name) {
        this.serviceName = name;
        return this;
    }

    /// A header sent with every direct export -- the collector's credential.
    /// Ignored in relay mode, where the backend holds the credential.
    ///
    /// #### Returns
    ///
    /// this configuration
    public TelemetryConfig header(String name, String value) {
        if (name != null && value != null) {
            headers.add(new String[] {name, value});
        }
        return this;
    }

    /// The shared secret a backend's relay may require
    /// (`cn1.otel.relay.token`). It keeps casual traffic off the relay; it is not
    /// a credential, since it ships in the app.
    ///
    /// #### Returns
    ///
    /// this configuration
    public TelemetryConfig relayToken(String token) {
        this.relayToken = token;
        return this;
    }

    /// Whether direct exports use binary protobuf (the default) or JSON. Some
    /// collectors accept only protobuf. The relay always receives JSON, and
    /// re-encodes it for the collector as the backend is configured to.
    ///
    /// #### Returns
    ///
    /// this configuration
    public TelemetryConfig protobuf(boolean protobuf) {
        this.protobuf = protobuf;
        return this;
    }

    /// The share of NEW traces recorded, from 0 to 1. A request made inside a
    /// span follows that span's decision, and the backend follows the app's.
    ///
    /// #### Returns
    ///
    /// this configuration
    public TelemetryConfig sampleRatio(double ratio) {
        this.sampleRatio = ratio < 0 ? 0 : ratio > 1 ? 1 : ratio;
        return this;
    }

    /// How many ended spans are buffered before an export. Defaults to 32.
    ///
    /// #### Returns
    ///
    /// this configuration
    public TelemetryConfig batchSize(int size) {
        this.batchSize = size < 1 ? 1 : size;
        return this;
    }

    /// How often buffered spans are exported even when the batch is not full.
    /// Defaults to ten seconds.
    ///
    /// #### Returns
    ///
    /// this configuration
    public TelemetryConfig flushIntervalMillis(int millis) {
        this.flushIntervalMillis = millis < 1000 ? 1000 : millis;
        return this;
    }

    /// Sends the W3C trace context to requests for this host as well.
    ///
    /// By default the context goes to the relay's host -- the app's own backend --
    /// and, on every platform except the web, to every host. The web is the
    /// exception because `traceparent` is not a CORS-safelisted header: sending it
    /// to a server that does not allow it turns a working cross-origin request
    /// into a failed preflight. Name the hosts that do allow it here.
    ///
    /// #### Returns
    ///
    /// this configuration
    public TelemetryConfig propagateTo(String host) {
        if (host != null && host.length() > 0) {
            propagateTo.add(host);
        }
        return this;
    }

    /// Sends the trace context to every host, on every platform, the web
    /// included. For an app whose every request goes to servers that allow the
    /// header.
    ///
    /// #### Returns
    ///
    /// this configuration
    public TelemetryConfig propagateToAllHosts() {
        this.propagateToAll = true;
        return this;
    }

    /// Records and propagates traces only while the user has granted ANALYTICS
    /// consent through `com.codename1.analytics.Analytics`, the same consent the
    /// analytics providers honour. Off by default: whether trace data needs consent
    /// is the app's decision, and depends on what it records and where it ships.
    ///
    /// With it on and no consent, requests are sent exactly as they would be
    /// without telemetry -- no span, and no `traceparent` header -- and spans
    /// already buffered are dropped rather than exported. Before the user answers,
    /// the analytics `ConsentMode` decides: `OPT_IN` (the default) means no.
    ///
    /// #### Returns
    ///
    /// this configuration
    public TelemetryConfig requireAnalyticsConsent(boolean require) {
        this.requireAnalyticsConsent = require;
        return this;
    }

    /// The URL spans are posted to, or null when no endpoint was given.
    String exportUrl() {
        if (endpoint == null || endpoint.length() == 0) {
            return null;
        }
        String base = endpoint.trim();
        // Trailing slashes first: ".../v1/traces/" is the full URL too, and testing
        // before stripping appended the path a second time -- a route no collector
        // serves, and the export fails silently by design.
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.endsWith("/v1/traces")) {
            return base;
        }
        return base + (mode == Mode.RELAY ? "/otel/v1/traces" : "/v1/traces");
    }
}
