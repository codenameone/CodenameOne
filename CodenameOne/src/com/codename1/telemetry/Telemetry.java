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

import com.codename1.analytics.Analytics;
import com.codename1.analytics.AnalyticsConsent;
import com.codename1.analytics.ConsentMode;
import com.codename1.io.ConnectionRequest;
import com.codename1.io.Data;
import com.codename1.io.Log;
import com.codename1.io.NetworkManager;
import com.codename1.io.NetworkTracer;
import com.codename1.security.Hash;
import com.codename1.security.SecureRandom;
import com.codename1.ui.CN;
import com.codename1.ui.Display;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

/// OpenTelemetry tracing for the app: every network request is a span, and carries
/// W3C trace context to the server it reaches, so the app's request and the
/// backend's handling of it are one trace.
///
/// Usually installed by the build rather than by hand -- put
/// `@OpenTelemetry` on the main class and the generated bootstrap calls
/// [#install(TelemetryConfig)] before the app starts:
///
/// ```java
/// @OpenTelemetry(relay = "https://api.example.com", serviceName = "shop-app")
/// public class ShopApp extends Lifecycle { ... }
/// ```
///
/// Every [ConnectionRequest] is covered, which is every REST, gRPC-Web and GraphQL
/// client the build generates. To group the requests a user action causes, time
/// the action:
///
/// ```java
/// Telemetry.run("checkout", () -> cart.submit());
/// ```
///
/// Spans are batched and exported with OTLP/HTTP -- through the app's own backend
/// by default, which keeps the collector's credentials out of the app (see
/// [TelemetryConfig]).
public final class Telemetry {
    private static final String TRACEPARENT = "traceparent";
    private static State state;
    /// The span [#run(String, Runnable)] has made current, per thread. Mostly the
    /// EDT's, since that is where a user action runs and where requests are queued,
    /// but `run` is public and a background task may time its own work: one static
    /// slot let two overlapping tasks adopt and restore each other's spans.
    private static final ThreadLocal<TelemetrySpan> CURRENT = new ThreadLocal<TelemetrySpan>();

    private Telemetry() {
    }

    /// Installs telemetry, replacing any earlier installation.
    ///
    /// Safe to call before `Display.init`, which is where the generated bootstrap
    /// calls it: nothing that needs the platform -- not even the secure random
    /// source the ids come from -- is touched until the first span.
    ///
    /// #### Parameters
    ///
    /// - `config`: where and how to export; a configuration with no endpoint
    ///   installs nothing
    public static void install(TelemetryConfig config) {
        uninstall();
        if (config == null || config.exportUrl() == null) {
            Log.p("Telemetry: no endpoint configured, so no spans are recorded");
            return;
        }
        State installed = new State(config);
        state = installed;
        NetworkManager.setNetworkTracer(installed);
    }

    /// Stops recording, exports what is buffered, and removes the network hook.
    public static void uninstall() {
        State old = state;
        state = null;
        CURRENT.remove();
        if (old != null) {
            NetworkManager.setNetworkTracer(null);
            old.stop();
        }
    }

    /// Whether telemetry is installed.
    public static boolean isInstalled() {
        return state != null;
    }

    /// A new span, a child of the current one. It is not made current; the caller
    /// must end it. Never null -- with telemetry off it records nothing.
    ///
    /// #### Parameters
    ///
    /// - `name`: what the span times
    public static TelemetrySpan startSpan(String name) {
        State s = state;
        TelemetrySpan span = s == null || !s.permitted() ? null
                : s.start(name, TelemetrySpan.KIND_INTERNAL, CURRENT.get());
        return span != null ? span : new TelemetrySpan(null, name, TelemetrySpan.KIND_INTERNAL,
                "00000000000000000000000000000000", "0000000000000000", null, false);
    }

    /// Runs `work` inside a new span, which is current while it runs: requests
    /// queued inside it become its children. A RuntimeException is recorded on
    /// the span and rethrown.
    ///
    /// #### Parameters
    ///
    /// - `name`: what the span times
    ///
    /// - `work`: the work
    public static void run(String name, Runnable work) {
        TelemetrySpan span = startSpan(name);
        TelemetrySpan previous = CURRENT.get();
        CURRENT.set(span);
        try {
            work.run();
        } catch (RuntimeException err) {
            span.recordException(err);
            throw err;
        } finally {
            CURRENT.set(previous);
            span.end();
        }
    }

    /// The span [#run(String, Runnable)] made current on this thread, or null.
    public static TelemetrySpan getCurrentSpan() {
        return CURRENT.get();
    }

    /// Exports what is buffered now, rather than at the next interval. Call it
    /// when the app is paused: a span still in memory when the process is
    /// reclaimed is lost.
    public static void flush() {
        State s = state;
        if (s != null) {
            s.flush();
        }
    }

    /// Everything one installation owns. It is the [NetworkTracer] too, so the
    /// network thread reaches the same configuration the app installed.
    static final class State implements NetworkTracer {
        private final TelemetryConfig config;
        private final String exportUrl;
        private final String backendHost;
        /// Touched on the EDT only: spans that end elsewhere are marshalled there.
        private final List<TelemetrySpan> buffer = new ArrayList<TelemetrySpan>();
        private Timer timer;
        private Map<String, Object> resource;
        private boolean stopped;
        /// Set once, the first time the platform refuses secure random bytes.
        private boolean idsUnavailable;

        State(TelemetryConfig config) {
            this.config = config;
            this.exportUrl = config.exportUrl();
            this.backendHost = config.mode == TelemetryConfig.Mode.RELAY ? host(exportUrl) : null;
        }

        /// A new span, or null when ids cannot be made on this platform.
        TelemetrySpan start(String name, int kind, TelemetrySpan parent) {
            String traceId;
            String parentId = null;
            boolean sampled;
            byte[] spanBytes = random(8);
            if (spanBytes == null) {
                return null;
            }
            if (parent != null && parent.traceId.length() == 32 && !isZero(parent.spanId)) {
                traceId = parent.traceId;
                parentId = parent.spanId;
                // Follow the parent's decision, so a trace is whole or absent.
                sampled = parent.sampled;
            } else {
                byte[] traceBytes = random(16);
                if (traceBytes == null) {
                    return null;
                }
                traceId = Hash.toHex(traceBytes);
                sampled = sample(traceBytes);
            }
            return new TelemetrySpan(this, name, kind, traceId, Hash.toHex(spanBytes), parentId,
                    sampled, parentId == null ? null : parent);
        }

        /// Secure random bytes, or null when the platform has none. Asked here,
        /// at the first span, rather than at install: install runs before
        /// Display.init, where no platform exists yet to answer, and treating that
        /// as "no random source" switched telemetry off on every device.
        private byte[] random(int length) {
            if (idsUnavailable) {
                return null;
            }
            try {
                return SecureRandom.bytes(length);
            } catch (RuntimeException err) {
                idsUnavailable = true;
                Log.p("Telemetry: this platform has no secure random source, so it cannot make "
                        + "trace ids; no spans are recorded");
                return null;
            }
        }

        /// Whether tracing may run now: always, unless the configuration asked for
        /// analytics consent, in which case the recorded choice -- or, before the
        /// user has made one, the consent mode -- decides. Read each time, so a
        /// choice the user changes takes effect on the next request.
        boolean permitted() {
            if (!config.requireAnalyticsConsent) {
                return true;
            }
            AnalyticsConsent consent = Analytics.getConsent();
            if (consent != null) {
                return consent.isAnalytics();
            }
            return Analytics.getConsentMode() == ConsentMode.OPT_OUT;
        }

        /// The ratio decision, from the id's low bytes as the other OpenTelemetry
        /// SDKs take it, so a backend sampling at the same ratio agrees without
        /// being told.
        private boolean sample(byte[] traceId) {
            if (config.sampleRatio >= 1) {
                return true;
            }
            if (config.sampleRatio <= 0) {
                return false;
            }
            long low = 0;
            for (int i = 8; i < 16; i++) {
                low = (low << 8) | (traceId[i] & 0xff);
            }
            long bound = (long) (config.sampleRatio * (double) Long.MAX_VALUE);
            return (low & Long.MAX_VALUE) < bound;
        }

        /// From any thread, as a span ends. The buffer is the EDT's, so the span
        /// is handed over there rather than guarded where it is.
        void ended(final TelemetrySpan span) {
            if (stopped) {
                return;
            }
            if (!Display.isInitialized()) {
                // Nowhere to deliver it yet; a span before the app exists is
                // start-up noise, not what anyone is tracing.
                return;
            }
            if (CN.isEdt()) {
                record(span);
                return;
            }
            CN.callSerially(new Runnable() {
                @Override
                public void run() {
                    record(span);
                }
            });
        }

        private void record(TelemetrySpan span) {
            if (stopped || !permitted()) {
                return;
            }
            buffer.add(span);
            if (timer == null) {
                // Started on the first span, on the EDT, because CN.setInterval
                // needs the display and install() may run before there is one.
                timer = CN.setInterval(config.flushIntervalMillis, new Runnable() {
                    @Override
                    public void run() {
                        flush();
                    }
                });
            }
            if (buffer.size() >= config.batchSize) {
                flush();
            }
        }

        void flush() {
            if (!CN.isEdt()) {
                if (Display.isInitialized()) {
                    CN.callSerially(new Runnable() {
                        @Override
                        public void run() {
                            flush();
                        }
                    });
                }
                return;
            }
            if (buffer.isEmpty()) {
                return;
            }
            if (!permitted()) {
                // Consent was withdrawn after these were recorded: what the user
                // refused is not sent, whenever it was collected.
                buffer.clear();
                return;
            }
            List<TelemetrySpan> batch = new ArrayList<TelemetrySpan>(buffer);
            buffer.clear();
            boolean json = config.mode == TelemetryConfig.Mode.RELAY || !config.protobuf;
            byte[] body = json ? OtlpEncoding.json(resource(), batch)
                    : OtlpEncoding.protobuf(resource(), batch);
            ExportRequest request = new ExportRequest(body);
            request.setUrl(exportUrl);
            request.setPost(true);
            request.setHttpMethod("POST");
            request.setContentType(json ? "application/json" : "application/x-protobuf");
            if (config.mode == TelemetryConfig.Mode.DIRECT) {
                for (String[] header : config.headers) {
                    request.addRequestHeader(header[0], header[1]);
                }
            } else if (config.relayToken != null && config.relayToken.length() > 0) {
                request.addRequestHeader("X-CN1-Telemetry-Token", config.relayToken);
            }
            // A failed export is dropped, never retried into the queue: telemetry
            // must not compete with the app's own requests for the network.
            request.setFailSilently(true);
            request.setReadResponseForErrors(false);
            NetworkManager.getInstance().addToQueue(request);
        }

        void stop() {
            flush();
            stopped = true;
            if (timer != null) {
                timer.cancel();
                timer = null;
            }
        }

        /// The app, as the collector should see it. Built at the first export,
        /// when the display that knows these things exists.
        private Map<String, Object> resource() {
            if (resource == null) {
                Map<String, Object> out = new LinkedHashMap<String, Object>();
                String appName = Display.getInstance().getProperty("AppName", null);
                String service = config.serviceName != null && config.serviceName.length() > 0
                        ? config.serviceName : appName;
                out.put("service.name", service == null || service.length() == 0
                        ? "unknown_service" : service);
                String version = Display.getInstance().getProperty("AppVersion", null);
                if (version != null && version.length() > 0) {
                    out.put("service.version", version);
                }
                String platform = Display.getInstance().getPlatformName();
                if (platform != null) {
                    out.put("os.name", platform);
                }
                String osVersion = Display.getInstance().getProperty("OSVer", null);
                if (osVersion != null && osVersion.length() > 0) {
                    out.put("os.version", osVersion);
                }
                out.put("telemetry.sdk.name", "codenameone");
                out.put("telemetry.sdk.language", "java");
                resource = out;
            }
            return resource;
        }

        // --------------------------------------------------------------
        // NetworkTracer
        // --------------------------------------------------------------

        @Override
        public Object requestQueued(ConnectionRequest request) {
            return CURRENT.get();
        }

        @Override
        public Object beforeRequest(ConnectionRequest request, Object parent) {
            if (stopped || request instanceof ExportRequest || !permitted()) {
                // The export's own request is not traced: each would be a span,
                // and exporting that one another.
                return null;
            }
            String url = request.getUrl();
            String method = request.getHttpMethod();
            if (method == null || method.length() == 0) {
                method = request.isPost() ? "POST" : "GET";
            }
            TelemetrySpan span = start(method, TelemetrySpan.KIND_CLIENT,
                    parent instanceof TelemetrySpan ? (TelemetrySpan) parent : null);
            if (span == null) {
                return null;
            }
            String host = host(url);
            if (span.isRecording()) {
                span.setAttribute("http.request.method", method);
                if (url != null) {
                    span.setAttribute("url.full", redact(url));
                }
                if (host != null) {
                    span.setAttribute("server.address", host);
                }
            }
            if (shouldPropagate(host)) {
                // Never over a traceparent the APP set: that is a deliberate choice
                // of which trace the request belongs to. Ours is removed again when
                // the attempt ends (afterRequest), so a retry or a redirect starts
                // clean -- the request object is reused, and a header left from an
                // allowed host would otherwise follow a redirect to one that is not.
                request.addRequestHeaderIfAbsent(TRACEPARENT, span.getTraceparent());
            }
            return span;
        }

        @Override
        public void afterRequest(ConnectionRequest request, Object attempt, int responseCode,
                                 Throwable error) {
            if (!(attempt instanceof TelemetrySpan)) {
                return;
            }
            TelemetrySpan span = (TelemetrySpan) attempt;
            // Only if it is still the value this attempt set; an app's own
            // traceparent, or one it replaced ours with, is left alone.
            request.removeRequestHeaderIfUnchanged(TRACEPARENT, span.getTraceparent());
            if (responseCode >= 100) {
                span.setAttribute("http.response.status_code", responseCode);
            }
            if (error != null) {
                span.recordException(error);
            } else if (responseCode >= 400) {
                span.setError(String.valueOf(responseCode));
            }
            span.end();
        }

        /// Where the trace context may go. See [TelemetryConfig#propagateTo(String)]
        /// for why the web is different.
        private boolean shouldPropagate(String host) {
            if (config.propagateToAll) {
                return true;
            }
            if (host != null) {
                if (host.equalsIgnoreCase(backendHost)) {
                    return true;
                }
                for (String allowed : config.propagateTo) {
                    if (host.equalsIgnoreCase(allowed)) {
                        return true;
                    }
                }
            }
            return !"HTML5".equals(Display.getInstance().getPlatformName());
        }
    }

    /// The export's own request, a type of its own so the tracer can recognise it,
    /// and a binary body without an intermediate String.
    static final class ExportRequest extends ConnectionRequest {
        ExportRequest(byte[] body) {
            setRequestBody(new ByteBody(body));
        }
    }

    /// A request body that is already bytes.
    private static final class ByteBody implements Data {
        private final byte[] body;

        ByteBody(byte[] body) {
            this.body = body;
        }

        @Override
        public void appendTo(OutputStream output) throws IOException {
            output.write(body);
        }

        @Override
        public long getSize() {
            return body.length;
        }
    }

    /// The host of an absolute URL, without port or userinfo; null when there is
    /// none.
    static String host(String url) {
        if (url == null) {
            return null;
        }
        int scheme = url.indexOf("://");
        if (scheme < 0) {
            return null;
        }
        int start = scheme + 3;
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
        if (at >= 0) {
            authority = authority.substring(at + 1);
        }
        if (authority.startsWith("[")) {
            int close = authority.indexOf(']');
            return close > 0 ? authority.substring(1, close) : authority;
        }
        int colon = authority.indexOf(':');
        return colon >= 0 ? authority.substring(0, colon) : authority;
    }

    /// The URL as it may be recorded: no query, no fragment, no userinfo. A query
    /// is where tokens and personal data travel, and a trace backend is not where
    /// either belongs.
    static String redact(String url) {
        int cut = url.length();
        int query = url.indexOf('?');
        int fragment = url.indexOf('#');
        if (query >= 0) {
            cut = query;
        }
        if (fragment >= 0 && fragment < cut) {
            cut = fragment;
        }
        String out = url.substring(0, cut);
        int scheme = out.indexOf("://");
        if (scheme >= 0) {
            int at = out.indexOf('@', scheme + 3);
            int slash = out.indexOf('/', scheme + 3);
            if (at >= 0 && (slash < 0 || at < slash)) {
                out = out.substring(0, scheme + 3) + out.substring(at + 1);
            }
        }
        return out;
    }

    private static boolean isZero(String hex) {
        for (int i = 0; i < hex.length(); i++) {
            if (hex.charAt(i) != '0') {
                return false;
            }
        }
        return true;
    }
}
