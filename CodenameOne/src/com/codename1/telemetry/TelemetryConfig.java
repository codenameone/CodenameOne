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

    /// A copy the caller cannot reach, taken when telemetry is installed. The
    /// installation read the caller's object live, while caching what it derives
    /// from it (the export URL, the relay's origin) -- so reusing the config after
    /// install, say `direct(...)` on one installed as a relay, sent direct-mode
    /// protobuf to the old relay, which dropped every batch. EVERY field goes
    /// here; a new one that is not copied is read live again.
    TelemetryConfig copy() {
        TelemetryConfig out = new TelemetryConfig();
        out.mode = mode;
        out.endpoint = endpoint;
        out.serviceName = serviceName;
        out.relayToken = relayToken;
        out.protobuf = protobuf;
        out.sampleRatio = sampleRatio;
        out.batchSize = batchSize;
        out.flushIntervalMillis = flushIntervalMillis;
        for (String[] header : headers) {
            out.headers.add(new String[] {header[0], header[1]});
        }
        out.propagateTo.addAll(propagateTo);
        out.propagateToAll = propagateToAll;
        out.requireAnalyticsConsent = requireAnalyticsConsent;
        return out;
    }

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
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when the URL is not http or https with a
    ///   host and a valid port
    public TelemetryConfig relay(String backendUrl) {
        this.mode = Mode.RELAY;
        this.endpoint = checkedEndpoint(backendUrl);
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
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when the URL is not http or https with a
    ///   host and a valid port
    public TelemetryConfig direct(String collectorUrl) {
        this.mode = Mode.DIRECT;
        this.endpoint = checkedEndpoint(collectorUrl);
        return this;
    }

    /// Refuses an endpoint no export could reach, when it is given. The build
    /// checks an annotation's URL, but a configuration written in code never meets
    /// that check: `direct("https://")` became `https:/v1/traces`, telemetry
    /// reported itself installed, and every export -- which fails silently by
    /// design -- was lost. Null or empty still means "no endpoint".
    private static String checkedEndpoint(String url) {
        if (url == null || url.trim().length() == 0) {
            return url;
        }
        if (!isHttpUrl(url.trim())) {
            throw new IllegalArgumentException("A telemetry endpoint must be an http or https "
                    + "URL with a host, such as https://collector.example:4318; it is '"
                    + Telemetry.redact(url.trim()) + "'");
        }
        return url;
    }

    /// Whether `url` is http or https with a host (a DNS name, an IPv4 address or
    /// a bracketed IPv6 literal) and, if it names one, a port from 1 to 65535.
    static boolean isHttpUrl(String url) {
        int start;
        if (url.regionMatches(true, 0, "http://", 0, 7)) {
            start = 7;
        } else if (url.regionMatches(true, 0, "https://", 0, 8)) {
            start = 8;
        } else {
            return false;
        }
        int end = url.length();
        for (int i = start; i < url.length(); i++) {
            char c = url.charAt(i);
            if (c == '/' || c == '?' || c == '#') {
                end = i;
                break;
            }
        }
        String authority = url.substring(start, end);
        int at = authority.lastIndexOf('@');
        if (at >= 0 && !validUserinfo(authority.substring(0, at))) {
            return false;
        }
        String hostPort = authority.substring(at + 1);
        String host;
        String port = null;
        if (hostPort.startsWith("[")) {
            int close = hostPort.indexOf(']');
            if (close < 0) {
                return false;
            }
            host = hostPort.substring(1, close);
            if (host.indexOf(':') < 0 || !onlyChars(host, "0123456789abcdefABCDEF:.")) {
                return false;
            }
            String rest = hostPort.substring(close + 1);
            if (rest.length() > 0) {
                if (rest.charAt(0) != ':') {
                    return false;
                }
                port = rest.substring(1);
            }
        } else {
            int colon = hostPort.lastIndexOf(':');
            host = colon < 0 ? hostPort : hostPort.substring(0, colon);
            port = colon < 0 ? null : hostPort.substring(colon + 1);
            if (!onlyChars(host, "-._~")) {
                return false;
            }
        }
        if (host.length() == 0) {
            return false;
        }
        if (port == null || port.length() == 0) {
            return true;
        }
        if (port.length() > 5) {
            return false;
        }
        int value = 0;
        for (int i = 0; i < port.length(); i++) {
            char c = port.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
            value = value * 10 + (c - '0');
        }
        return value >= 1 && value <= 65535;
    }

    /// RFC 3986 userinfo: unreserved characters, sub-delims, ':' and complete
    /// percent escapes. Skipped over, a space, a control or a stray '%' in it passed
    /// validation and failed only at transport, where the export fails silently.
    static boolean validUserinfo(String userinfo) {
        for (int i = 0; i < userinfo.length(); i++) {
            char c = userinfo.charAt(i);
            if (c == '%') {
                if (i + 2 >= userinfo.length() || !isHex(userinfo.charAt(i + 1))
                        || !isHex(userinfo.charAt(i + 2))) {
                    return false;
                }
                i += 2;
                continue;
            }
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || "-._~!$&'()*+,;=:".indexOf(c) >= 0)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isHex(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private static boolean onlyChars(String value, String extra) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || extra.indexOf(c) >= 0)) {
                return false;
            }
        }
        return true;
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
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when the name is not an HTTP token or the
    ///   value holds a control character
    public TelemetryConfig header(String name, String value) {
        if (name != null && value != null) {
            checkHeader(name, value);
            headers.add(new String[] {name, value});
        }
        return this;
    }

    /// The rules the annotation processor applies to a header, for one written in
    /// code, which never meets it. A bad header is refused only when an export
    /// runs -- after its batch has left the buffer, and silently, by design -- so
    /// accepting it here lost every batch. The message names the header, never
    /// the value, which is usually a credential.
    private static void checkHeader(String name, String value) {
        if (name.length() == 0) {
            throw new IllegalArgumentException("A telemetry header needs a name");
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || "!#$%&'*+-.^_`|~".indexOf(c) >= 0)) {
                throw new IllegalArgumentException("'" + name + "' is not a valid HTTP header name");
            }
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if ((c < 0x20 && c != '\t') || c == 0x7f) {
                throw new IllegalArgumentException("The value of telemetry header " + name
                        + " holds a control character, which no HTTP header may carry");
            }
        }
    }

    /// The shared secret a backend's relay may require
    /// (`cn1.otel.relay.token`). It keeps casual traffic off the relay; it is not
    /// a credential, since it ships in the app.
    ///
    /// #### Returns
    ///
    /// this configuration
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when the token holds a control character
    public TelemetryConfig relayToken(String token) {
        if (token != null) {
            // Sent as a header, so held to the same rules.
            checkHeader("X-CN1-Telemetry-Token", token);
        }
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
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: for NaN
    public TelemetryConfig sampleRatio(double ratio) {
        // NaN fails both comparisons below, so it was stored as it was, and the
        // sampler then declined every trace while telemetry reported itself on.
        // A range can be clamped into; NaN has no nearest value, so it is refused
        // -- as the annotation processor refuses it.
        if (Double.isNaN(ratio)) {
            throw new IllegalArgumentException("The telemetry sample ratio is NaN; "
                    + "give a number from 0 to 1");
        }
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
        String url = endpoint.trim();
        // The path is what gets the suffix; a query or fragment -- where a
        // collector's api-key often travels -- is set aside and put back after it.
        // Appending to the whole string put the path inside the credential and
        // sent the export to the base path.
        int cut = url.length();
        int query = url.indexOf('?');
        int fragment = url.indexOf('#');
        if (query >= 0) {
            cut = query;
        }
        if (fragment >= 0 && fragment < cut) {
            cut = fragment;
        }
        String base = url.substring(0, cut);
        String suffix = url.substring(cut);
        // Trailing slashes first: ".../v1/traces/" is the full URL too, and testing
        // before stripping appended the path a second time -- a route no collector
        // serves, and the export fails silently by design.
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.endsWith("/v1/traces")) {
            return base + suffix;
        }
        return base + (mode == Mode.RELAY ? "/otel/v1/traces" : "/v1/traces") + suffix;
    }
}
