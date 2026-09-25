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
import java.io.InputStream;
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
    private static final String TRACESTATE = "tracestate";
    /// How many export batches may wait in the network queue at once.
    static final int MAX_PENDING_EXPORTS = 2;
    /// The installation. A plain field, deliberately, like the tracer and guard
    /// slots in NetworkManager it fills: install and uninstall are lifecycle
    /// calls -- the generated bootstrap's, before Display.init, or the app's, on
    /// the EDT -- and Codename One core does not synchronize framework state
    /// (PMD's AvoidUsingVolatile gate says the same). A network thread or a task
    /// started after the install sees it through Thread.start's happens-before
    /// edge. Reinstalling telemetry while background tasks are mid-flight is not
    /// a supported pattern, and locking every span start to serve it would tax
    /// every request of every app that never does it.
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
            // Only if the slot still holds OURS: an app may have replaced the
            // generated tracer with its own, and uninstalling telemetry must not
            // silently switch that one off.
            if (NetworkManager.getNetworkTracer() == old) { //NOPMD CompareObjectsWithEquals -- identity: is the slot still THIS installation
                NetworkManager.setNetworkTracer(null);
            }
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
    /// Not final so a test can stand in for the network queue: whether exports are
    /// backed up is a property of the shared NetworkManager, which a test cannot hold
    /// still reliably.
    static class State implements NetworkTracer {
        private final TelemetryConfig config;
        private final String exportUrl;
        /// The relay's ORIGIN -- scheme, host and effective port -- which is what
        /// the browser keys CORS on. The host alone let a request to another port
        /// or scheme on the same host through, and a traceparent can turn a request
        /// the browser would have sent as-is into a preflight that fails.
        private final String backendOrigin;
        /// Touched on the EDT only: spans that end elsewhere are marshalled there.
        final List<TelemetrySpan> buffer = new ArrayList<TelemetrySpan>();
        private Timer timer;
        private Map<String, Object> resource;
        private boolean stopped;
        /// Set once, the first time the platform refuses secure random bytes.
        private boolean idsUnavailable;

        State(TelemetryConfig config) {
            // A snapshot: see TelemetryConfig.copy.
            this.config = config.copy();
            this.exportUrl = config.exportUrl();
            this.backendOrigin = config.mode == TelemetryConfig.Mode.RELAY ? origin(exportUrl) : null;
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
            // Only a parent THIS installation recorded. A thread still inside
            // run() across an uninstall and reinstall holds the old one's span, and
            // inheriting it would file the new installation's spans under the old
            // trace, with the old sampling decision, possibly at another collector.
            if (parent != null && parent.isOwnedBy(this) && parent.traceId.length() == 32
                    && !isZero(parent.spanId)) {
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
            if (!Display.isInitialized()) {
                // Too early to ask, not a platform without a source: before
                // Display.init there is no implementation behind SecureRandom. Not
                // cached, or a span a program started during start-up switched
                // telemetry off for good once the display did exist.
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
            if (!Display.isInitialized()) {
                // No storage yet, so the saved choice cannot be read -- and reading
                // it anyway made Analytics record "loaded, nothing saved" for the
                // rest of the run, ignoring a persisted grant, or in opt-out mode a
                // persisted denial. Nothing is traced until it can be asked.
                return false;
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
                    recordHandedOff(span);
                }
            });
        }

        /// A span that ended on another thread and reaches the EDT only now. It
        /// passed the stopped check where it ended, so it finished while this
        /// installation was running; if an uninstall or a reinstall ran on the EDT
        /// in between, it is exported on its own rather than discarded -- which
        /// lost the request spans that were completing just as telemetry was
        /// reconfigured.
        void recordHandedOff(TelemetrySpan span) {
            if (!stopped) {
                record(span);
                return;
            }
            if (!permitted()) {
                return;
            }
            buffer.add(span);
            if (buffer.size() > maxBuffered()) {
                buffer.remove(0);
            }
            // ONE final flush for the burst, not one per span: every other late
            // handoff already queued on the EDT runs before this, so they share a
            // single export. A flush per span put an uncapped export -- and its
            // encoded body -- in the network queue for each of them.
            if (!lateFlushScheduled) {
                lateFlushScheduled = true;
                CN.callSerially(new Runnable() {
                    @Override
                    public void run() {
                        lateFlushScheduled = false;
                        flush(true);
                    }
                });
            }
        }

        /// Whether a final flush for late handoffs is already queued on the EDT.
        private boolean lateFlushScheduled;

        void record(TelemetrySpan span) {
            if (stopped || !permitted()) {
                return;
            }
            buffer.add(span);
            if (buffer.size() > maxBuffered()) {
                // BOUNDED. While exports cannot keep up -- the collector is slow, or
                // the app's own traffic keeps outranking them -- the oldest spans
                // go, rather than the app's memory.
                buffer.remove(0);
            }
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
            flush(false);
        }

        /// `last` is the installation's final flush, from [#stop()]: it sends the
        /// buffer even when exports are already waiting. Otherwise an uninstall or a
        /// reinstall behind a full export queue left the buffer behind for good --
        /// the state stops, its timer is cancelled, and nothing would ever flush it
        /// again. One extra batch, and the buffer it comes from is bounded.
        void flush(final boolean last) {
            if (!CN.isEdt()) {
                if (Display.isInitialized()) {
                    CN.callSerially(new Runnable() {
                        @Override
                        public void run() {
                            flush(last);
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
            if (!last && pendingExports() >= MAX_PENDING_EXPORTS) {
                // Exports are already waiting in the network queue. Queuing another
                // batch would hold one more byte array per flush for as long as the
                // app's requests outrank them, with no limit; the spans stay in the
                // bounded buffer and go out once the queue drains.
                return;
            }
            List<TelemetrySpan> batch = new ArrayList<TelemetrySpan>(buffer);
            buffer.clear();
            boolean json = config.mode == TelemetryConfig.Mode.RELAY || !config.protobuf;
            byte[] body = json ? OtlpEncoding.json(resource(), batch)
                    : OtlpEncoding.protobuf(resource(), batch);
            ExportRequest request = new ExportRequest(this, body);
            request.setUrl(exportUrl);
            request.setPost(true);
            request.setHttpMethod("POST");
            if (config.mode == TelemetryConfig.Mode.DIRECT) {
                for (String[] header : config.headers) {
                    request.addRequestHeader(header[0], header[1]);
                }
            } else if (config.relayToken != null && config.relayToken.length() > 0) {
                request.addRequestHeader("X-CN1-Telemetry-Token", config.relayToken);
            }
            // AFTER the configured headers, so nothing among them can relabel the
            // body: the config refuses a Content-Type, and this holds regardless.
            request.setContentType(json ? "application/json" : "application/x-protobuf");
            // A failed export is dropped, never retried into the queue: telemetry
            // must not compete with the app's own requests for the network.
            request.setFailSilently(true);
            request.setReadResponseForErrors(false);
            // Never followed. A redirect re-queues this same request with its
            // headers intact, so a collector that redirected elsewhere -- another
            // origin included -- would be handed the Authorization or API-key
            // header meant for it, and 301/302/303 turn the POST into a bodiless
            // GET anyway. An export that is redirected fails, and is dropped like
            // any other failed export; point the endpoint at the real collector.
            request.setFollowRedirects(false);
            // SHORT, and behind the app's own requests. By default a request may
            // take five minutes and the manager has one network thread, so a
            // collector that accepts the connection and never answers would hold
            // every request the app makes behind an export nobody is waiting for.
            request.setTimeout(10000);
            request.setReadTimeout(10000);
            request.setPriority(ConnectionRequest.PRIORITY_LOW);
            NetworkManager.getInstance().addToQueue(request);
        }

        /// Telemetry exports still waiting to be sent. Read from the queue itself
        /// rather than counted, because a fail-silent export that fails reports
        /// nothing back to count with. Any installation's exports count: after a
        /// reinstall the previous one's are still competing for the same network.
        int pendingExports() {
            int count = 0;
            java.util.Enumeration queue = NetworkManager.getInstance().enumurateQueue();
            while (queue.hasMoreElements()) {
                if (queue.nextElement() instanceof ExportRequest) {
                    count++;
                }
            }
            return count;
        }

        private int maxBuffered() {
            return Math.max(config.batchSize * 4, 128);
        }

        void stop() {
            flush(true);
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
                // Trimmed, as the annotation's is: " " is no name, and exported
                // as one the app had no usable service identity.
                String configured = config.serviceName == null ? "" : config.serviceName.trim();
                String service = configured.length() > 0 ? configured : appName;
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
            if (request.getRequestHeader(TRACEPARENT) != null
                    || request.getRequestHeader(TRACESTATE) != null) {
                // The app chose which trace this request belongs to. The service it
                // reaches joins THAT trace, so a span recorded here in another one
                // would describe the same request twice, in two traces that never
                // meet. The app's own instrumentation owns this request. A
                // tracestate alone counts too: it is part of the app's context,
                // and a traceparent of ours beside it paired the app's vendor state
                // with an unrelated trace id.
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
            if (shouldPropagate(url, host)) {
                // The app's own traceparent was ruled out above. Ours is removed
                // again when the attempt ends (afterRequest), so a retry or a
                // redirect starts clean -- the request object is reused, and a
                // header left from an allowed host would otherwise follow a redirect
                // to one that is not.
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
            } else if (responseCode < 100) {
                // Neither a response nor an exception: the request was killed or
                // stopped after it had started. Ended without a status, it read as
                // a success in every trace backend.
                span.setError("cancelled before a response");
            }
            span.end();
        }

        /// Where the trace context may go. See [TelemetryConfig#propagateTo(String)]
        /// for why the web is different.
        private boolean shouldPropagate(String url, String host) {
            if (config.propagateToAll || isSameOriginRelative(url)) {
                return true;
            }
            if (backendOrigin != null && backendOrigin.equals(origin(url))) {
                return true;
            }
            if (host != null) {
                // Hosts the app named itself, as it named them.
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
        /// The installation that recorded these spans. Its consent policy, not
        /// whichever installation is current when the export finally runs, decides
        /// whether they may be sent: after an uninstall, or a reinstall without the
        /// consent flag, the static slot says nothing about THESE spans.
        private final State origin;

        ExportRequest(State origin, byte[] body) {
            this.origin = origin;
            setRequestBody(new ByteBody(body));
        }

        /// The acknowledgement is read and DROPPED, at most 64KB of it. An OTLP
        /// success body is empty or a few bytes; the inherited reader kept the whole
        /// stream in memory, so a misbehaving collector or proxy answering 200 with
        /// a large body could spend a phone's memory on every export.
        @Override
        protected void readResponse(InputStream input) throws IOException {
            byte[] discard = new byte[4096];
            int total = 0;
            int read;
            while (total < 65536 && (read = input.read(discard)) > 0) {
                total += read;
            }
        }

        /// None of the app's default headers. They are the app's credentials for
        /// its own services; copied onto an export they reached a third-party
        /// collector, and a default Content-Type relabelled the body. An export
        /// carries only what the telemetry configuration names.
        @Override
        protected boolean shouldApplyDefaultHeaders() {
            return false;
        }

        /// Identity. The inherited equality compares URL and arguments, so every
        /// export to one collector compared equal though each carries its own
        /// spans; that made no two exports distinguishable to the queue.
        @Override
        public boolean equals(Object o) {
            return o == this; //NOPMD CompareObjectsWithEquals
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }

        /// Stopped, and nothing sent, once consent is required and no longer
        /// given. An export waits in the queue behind the app's own requests, and
        /// a user who withdraws consent in that time has refused these spans too;
        /// performOperationComplete asks this before it connects.
        @Override
        protected boolean shouldStop() {
            if (super.shouldStop()) {
                return true;
            }
            return !origin.permitted();
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

    /// Whether `url` is relative to the page, `/api/orders` or `orders?id=1`, and
    /// so goes to the origin that served the app. The stock
    /// `ConnectionRequest.validate()` refuses such a URL, but a request that
    /// overrides it can send one, and in the browser it reaches the app's own
    /// backend -- the one destination that never needs a CORS allowance. Refusing
    /// it, which [#host(String)] returning null did, dropped the context from the
    /// call it matters most on. A scheme (`data:`, `mailto:`) or a
    /// protocol-relative `//host/...` names another origin and is not relative.
    private static boolean isSlash(char c) {
        return c == '/' || c == '\\';
    }

    static boolean isSameOriginRelative(String url) {
        if (url == null) {
            return false;
        }
        // Read as the browser reads it (the WHATWG URL parser): leading and
        // trailing C0 controls and spaces are stripped, and every tab and newline
        // removed, BEFORE anything is resolved -- so " //host/x" and "\t\\\\host/x"
        // are network paths to it, and were same-origin here.
        StringBuilder cleaned = new StringBuilder(url.length());
        for (int i = 0; i < url.length(); i++) {
            char c = url.charAt(i);
            if (c != '\t' && c != '\n' && c != '\r') {
                cleaned.append(c);
            }
        }
        int start = 0;
        int end = cleaned.length();
        while (start < end && cleaned.charAt(start) <= ' ') {
            start++;
        }
        while (end > start && cleaned.charAt(end - 1) <= ' ') {
            end--;
        }
        // Through toString(): CLDC11's StringBuilder has no substring.
        url = cleaned.toString().substring(start, end);
        if (url.length() == 0) {
            return false;
        }
        // A network-path reference names another origin, and a browser's URL
        // parser reads '\' as '/' for http(s): "\\host/x" and "/\\host" are
        // "//host/x" to it. Refused in any mix of the two, or the trace header
        // went cross-origin without a propagateTo allowance.
        if (url.length() >= 2 && isSlash(url.charAt(0)) && isSlash(url.charAt(1))) {
            return false;
        }
        for (int i = 0; i < url.length(); i++) {
            char c = url.charAt(i);
            if (c == ':') {
                return false;
            }
            if (c == '/' || c == '?' || c == '#') {
                return true;
            }
        }
        return true;
    }

    /// `scheme://host:port` of an http or https URL, lower case, with the
    /// scheme's default port filled in, so two spellings of one origin compare
    /// equal; null for anything else.
    static String origin(String url) {
        String host = host(url);
        if (host == null) {
            return null;
        }
        String scheme;
        int defaultPort;
        if (url.regionMatches(true, 0, "https://", 0, 8)) {
            scheme = "https";
            defaultPort = 443;
        } else if (url.regionMatches(true, 0, "http://", 0, 7)) {
            scheme = "http";
            defaultPort = 80;
        } else {
            return null;
        }
        int start = scheme.length() + 3;
        String authority = url.substring(start, authorityEnd(url, start));
        authority = authority.substring(authority.lastIndexOf('@') + 1);
        int close = authority.lastIndexOf(']');
        int colon = authority.lastIndexOf(':');
        int port = defaultPort;
        if (colon > close && colon + 1 < authority.length()) {
            try {
                port = Integer.parseInt(authority.substring(colon + 1));
            } catch (NumberFormatException err) {
                return null;
            }
        }
        // host() drops an IPv6 literal's brackets; an origin keeps them, or the
        // port could not be told from the address.
        String name = asciiLower(host);
        if (name.indexOf(':') >= 0) {
            name = "[" + name + "]";
        }
        return scheme + "://" + name + ":" + port;
    }

    private static String asciiLower(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            out.append(c >= 'A' && c <= 'Z' ? (char) (c + 32) : c);
        }
        return out.toString();
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
        String authority = url.substring(start, authorityEnd(url, start));
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
            // The LAST '@' inside the authority, as URL parsers split it: in
            // "https://alice:secret@tenant@host/x" the first '@' belongs to the
            // password, and cutting there exported "tenant@" as part of url.full.
            int start = scheme + 3;
            int at = out.substring(start, authorityEnd(out, start)).lastIndexOf('@');
            if (at >= 0) {
                out = out.substring(0, start) + out.substring(start + at + 1);
            }
        }
        return out;
    }

    /// Where the authority that starts at `start` ends: the first '/', '\\', '?' or
    /// '#'. The backslash counts because an http(s) URL parser -- the browser the
    /// JavaScript port runs in, among them -- reads it as a slash, so
    /// "https://evil.example\\@api.example/x" goes to evil.example. Reading the
    /// backslash as part of the authority instead named api.example as the host,
    /// and a trace context approved for that host went to the other one.
    static int authorityEnd(String url, int start) {
        for (int i = start; i < url.length(); i++) {
            char c = url.charAt(i);
            if (c == '/' || c == '\\' || c == '?' || c == '#') {
                return i;
            }
        }
        return url.length();
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
