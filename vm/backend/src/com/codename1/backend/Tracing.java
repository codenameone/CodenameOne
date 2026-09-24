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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Distributed tracing for the server, and the hooks it is instrumented through.
 *
 * <p>Nothing here does anything until a {@link Tracer} is installed. The build
 * installs one when a project asks for it -- {@code @OpenTelemetry} on any class,
 * or {@code cn1.otel.enabled=true} in application.properties -- and from then on
 * every request, outbound {@link Web} call and {@link Database} statement is a
 * span, W3C trace context travels on every outbound request, and an incoming
 * {@code traceparent} makes the request part of the caller's trace. No code in
 * the application changes for any of that.
 *
 * <p>What an application may still want is here: {@link #current()} to decorate
 * the request's span, and {@link #inSpan} to time a block of its own work.
 *
 * <p>THE CURRENT SPAN IS PER THREAD, and that is per REQUEST here: a virtual
 * thread serves one request at a time and ThreadLocal is per virtual thread (see
 * the measurement recorded beside HttpServer.SERVING_FD). The server sets it
 * before the handler runs and clears it in the same finally that ends the
 * request, because the next request on a kept-alive connection runs on the same
 * thread and must not inherit it.
 *
 * <p>Every hook is guarded: a tracer that throws is reported once and the
 * request goes on untraced, because a monitoring fault must never become an
 * outage.
 */
public final class Tracing {
    /** The installed tracer, or null. Volatile: installed once, read by every host. */
    private static volatile Tracer tracer;
    private static final ThreadLocal CURRENT = new ThreadLocal();
    private static final ThreadLocal SUPPRESSED = new ThreadLocal();
    private static volatile boolean reportedFailure;
    private static final Span NOOP = new NoopSpan();

    /** The W3C header names, lower case as HTTP/2 requires. */
    static final String TRACEPARENT = "traceparent";
    static final String TRACESTATE = "tracestate";

    private Tracing() {
    }

    /** What {@link #inSpan} runs. */
    public interface Work {
        Object run(Span span) throws Exception;
    }

    /** How long a replaced tracer gets to export what it already holds. */
    static final int REPLACED_SHUTDOWN_MILLIS = 2000;

    /**
     * Installs the tracer every hook reports to, replacing any earlier one. Pass
     * null to turn tracing off. The builder does this itself; a program that
     * starts {@link HttpServer} or {@link LambdaRuntime} directly calls it after
     * {@link Tracer#open}.
     *
     * <p>A tracer this replaces is shut down: its spans so far are exported, for
     * up to {@link #REPLACED_SHUTDOWN_MILLIS}, and its exporter stops. Nothing
     * else holds it once it is out of this slot, so leaving it running leaked its
     * export thread and queues on every reconfiguration.
     */
    public static void install(Tracer installed) {
        retire(swap(installed), installed);
    }

    /**
     * Installs {@code installed} WITHOUT stopping the tracer it replaces, and
     * returns that one. For a start-up that can still fail: it traces its own
     * start-up with the new tracer, then {@link #retire}s the old one if it
     * committed or {@link #rollBack}s if it did not. Shutting the old one down up
     * front left a failed second server's process with no tracer at all, while
     * the first server kept running untraced.
     */
    static Tracer swap(Tracer installed) {
        Tracer previous = tracer;
        tracer = installed;
        return previous;
    }

    /** Stops {@code previous}, replaced by {@code installed}, exporting what it held. */
    static void retire(Tracer previous, Tracer installed) {
        if(previous != null && previous != installed) {
            try {
                previous.shutdown(REPLACED_SHUTDOWN_MILLIS);
            } catch (RuntimeException err) {
                failed(err);
            }
        }
    }

    /**
     * Undoes a {@link #swap}: {@code previous} is back in the slot, untouched, and
     * {@code failed} -- the tracer of a start-up that did not complete -- is
     * stopped. Only if the slot still holds {@code failed}; anything installed
     * since then is someone else's.
     */
    static void rollBack(Tracer failed, Tracer previous) {
        if(tracer == failed) {
            tracer = previous;
        }
        if(failed != null && failed != previous) {
            try {
                failed.shutdown(0);
            } catch (RuntimeException err) {
                failed(err);
            }
        }
    }

    /** The installed tracer, or null. */
    public static Tracer getTracer() {
        return tracer;
    }

    /** Whether a tracer is installed. */
    public static boolean isEnabled() {
        return tracer != null;
    }

    /**
     * The span of the work this thread is doing: the request's, inside a handler.
     * Never null -- with no tracer, or outside a request, it is a no-op span.
     */
    public static Span current() {
        Span span = currentOrNull();
        return span == null ? NOOP : span;
    }

    /**
     * The current trace as a W3C {@code traceparent} value, for a transport the
     * server does not instrument itself -- a message queue, a raw socket. Null
     * when there is no trace.
     */
    public static String currentTraceparent() {
        Span span = currentOrNull();
        return span == null ? null : span.traceparent();
    }

    /**
     * Runs {@code work} inside a new span that is a child of the current one, and
     * is current itself while it runs. An exception is recorded on the span and
     * rethrown.
     */
    public static Object inSpan(String name, Work work) throws Exception {
        Span span = begin(name, Span.KIND_INTERNAL, null, null);
        if(span == null) {
            return work.run(NOOP);
        }
        try {
            return work.run(span);
        } catch (Exception err) {
            guardedException(span, err);
            throw err;
        } finally {
            finish(span);
        }
    }

    /**
     * A new child of the current span that is NOT made current, for work whose
     * start and end are in different places. The caller must end it.
     */
    public static Span startSpan(String name) {
        Tracer t = tracer;
        if(t == null || isSuppressed()) {
            return NOOP;
        }
        try {
            Span span = t.startSpan(name, Span.KIND_INTERNAL, currentOrNull(), null, null);
            return span == null ? NOOP : span;
        } catch (RuntimeException err) {
            failed(err);
            return NOOP;
        }
    }

    /**
     * Turns span creation off, or back on, for the calling thread. The exporter
     * uses it so its own requests to the collector are not traced -- each export
     * would otherwise produce a span, and exporting that one another.
     */
    public static void setSuppressed(boolean suppressed) {
        SUPPRESSED.set(suppressed ? Boolean.TRUE : null);
    }

    /**
     * Applies the rules {@link Web} enforces on request header lines to lines an
     * exporter will send, so a tracer can refuse a bad configuration when it is
     * opened. Otherwise the server starts, and every export then fails on the
     * same check. One rule set, not a copy of it.
     *
     * @param lines {@code "Name: value"} strings
     * @throws java.io.IOException naming the first line that could not be sent;
     *         the message never quotes a value, since values carry credentials
     */
    public static void checkHeaderLines(List lines) throws java.io.IOException {
        HeaderLines.validate(lines);
    }

    /** Whether {@link #setSuppressed} is in force on this thread. */
    public static boolean isSuppressed() {
        return SUPPRESSED.get() != null;
    }

    /**
     * Names the current server span after the route that matched. Called by the
     * generated routers, which are the only code that knows the TEMPLATE -- the
     * path alone would make every pet id its own operation.
     */
    public static void route(String template) {
        // Every generated router calls this on every matched request, traced or
        // not; with no tracer that is this one read and nothing else.
        if(tracer == null) {
            return;
        }
        Span span = currentOrNull();
        if(span == null || template == null) {
            return;
        }
        try {
            // Inside the guard: the generated routers call this on every matched
            // request, so a tracer whose getKind throws would otherwise turn a
            // request that succeeded into a 500.
            if(span.getKind() != Span.KIND_SERVER) {
                return;
            }
            if(span.isRecording()) {
                span.setAttribute("http.route", template);
            }
            String name = span.getName();
            // The name is the method until a route is known. Only the first match
            // renames it, so two chained routers cannot append twice.
            if(name != null && name.indexOf(' ') < 0) {
                span.updateName(name + " " + template);
            }
        } catch (RuntimeException err) {
            failed(err);
        }
    }

    // ------------------------------------------------------------------
    // Hooks the runtime calls. Each one is a single null test when tracing is
    // off.
    // ------------------------------------------------------------------

    /**
     * The span for one request, made current. Everything is read from the
     * request NOW, because a Request is valid only while its handler runs.
     */
    static Span startServer(HttpServer.Request request, boolean secure) {
        Tracer t = tracer;
        if(t == null || request == null) {
            return null;
        }
        Span span = null;
        try {
            String method = request.getMethod();
            span = t.startSpan(method == null ? "HTTP" : method, Span.KIND_SERVER, null,
                    request.getHeader(TRACEPARENT), request.getHeader(TRACESTATE));
            if(span == null) {
                return null;
            }
            if(span.isRecording()) {
                span.setAttribute("http.request.method", method);
                String target = request.getTarget();
                if(target != null) {
                    // The PATH only. The query string is where tokens and
                    // personal data travel, and a trace backend is not where
                    // either belongs.
                    int query = target.indexOf('?');
                    span.setAttribute("url.path", query < 0 ? target : target.substring(0, query));
                }
                span.setAttribute("url.scheme", secure ? "https" : "http");
                String version = request.getVersion();
                if(version != null && version.startsWith("HTTP/")) {
                    span.setAttribute("network.protocol.version", version.substring(5));
                }
                String host = request.getHeader("host");
                if(host != null) {
                    span.setAttribute("server.address", hostOnly(host));
                }
                String agent = request.getHeader("user-agent");
                if(agent != null) {
                    span.setAttribute("user_agent.original", agent);
                }
            }
            enter(span);
            return span;
        } catch (RuntimeException err) {
            failed(err);
            abandon(span);
            return null;
        }
    }

    /**
     * Ends a request's span once the response is written -- or failed to be -- and
     * clears the current span for the next request on the thread.
     *
     * @param status the status sent, or -1 when the write failed
     */
    static void endServer(Span span, int status, Throwable error) {
        if(span == null) {
            return;
        }
        try {
            if(span.isRecording()) {
                if(status > 0) {
                    span.setAttribute("http.response.status_code", (long)status);
                }
                if(error != null) {
                    span.recordException(error);
                } else if(status >= 500) {
                    // Server spans fail on 5xx only. A 404 is this server answering
                    // correctly about something that is not there.
                    span.setError(String.valueOf(status));
                } else if(status < 0) {
                    span.setError("the response could not be written");
                }
            }
        } catch (RuntimeException err) {
            failed(err);
        }
        finish(span);
    }

    /**
     * An outbound HTTP span, made current while the call runs. Null when tracing
     * is off, suppressed on this thread, or already inside a client span -- one
     * outbound operation is one span, whatever it happens to be built from.
     */
    static Span startHttpClient(String method, String url, List callerHeaders) {
        Tracer t = tracer;
        if(t == null || isSuppressed()) {
            return null;
        }
        if(callerTraceparent(callerHeaders)) {
            // The caller chose which trace this request belongs to, and the service
            // it reaches joins that one. A span recorded here would sit in the
            // CURRENT trace and describe a request whose context it never sent.
            return null;
        }
        String verb = method == null ? "GET" : method;
        Span span = begin(verb, Span.KIND_CLIENT, null, null);
        if(span == null) {
            return null;
        }
        try {
            if(span.isRecording()) {
                span.setAttribute("http.request.method", verb);
                if(url != null) {
                    // Redacted the way every log line here redacts it: a presigned
                    // URL's query IS the credential, and userinfo is a password.
                    span.setAttribute("url.full", Urls.forMessage(url));
                    String host = authority(url);
                    if(host != null) {
                        span.setAttribute("server.address", hostOnly(host));
                    }
                }
            }
        } catch (RuntimeException err) {
            failed(err);
        }
        return span;
    }

    /**
     * The header lines that carry the trace to the service being called, or null
     * for none. None when the caller already set a traceparent of its own: that
     * is a deliberate choice about which trace the request belongs to.
     */
    static List propagationHeaders(Span span, List callerHeaders) {
        if(span == null) {
            return null;
        }
        if(callerTraceparent(callerHeaders)) {
            return null;
        }
        try {
            String parent = span.traceparent();
            if(parent == null) {
                return null;
            }
            List out = new ArrayList(2);
            out.add(TRACEPARENT + ": " + parent);
            String state = span.tracestate();
            if(state != null && state.length() > 0) {
                out.add(TRACESTATE + ": " + state);
            }
            return out;
        } catch (RuntimeException err) {
            failed(err);
            return null;
        }
    }

    /**
     * Ends an outbound span.
     *
     * @param status the response status, or -1 when there was none
     */
    static void endHttpClient(Span span, int status, Throwable error) {
        if(span == null) {
            return;
        }
        try {
            if(span.isRecording()) {
                if(status > 0) {
                    span.setAttribute("http.response.status_code", (long)status);
                }
                if(error != null) {
                    span.recordException(error);
                } else if(status >= 400) {
                    // A client span fails on 4xx as well: the call did not get
                    // what it asked for, whoever was wrong.
                    span.setError(String.valueOf(status));
                }
            }
        } catch (RuntimeException err) {
            failed(err);
        }
        finish(span);
    }

    /**
     * A span for one database statement, made current while it runs.
     *
     * <p>The statement is recorded as the application wrote it, placeholders and
     * all. The bound VALUES never are: they are the rows. A deployment that also
     * inlines literals into its SQL can drop the text with
     * {@code cn1.otel.attributes.exclude=db.query.text}.
     */
    static Span startDatabase(String system, String sql) {
        Tracer t = tracer;
        if(t == null || isSuppressed()) {
            return null;
        }
        String operation = firstKeyword(sql);
        Span span = begin(operation == null ? system : operation, Span.KIND_CLIENT, null, null);
        if(span == null) {
            return null;
        }
        try {
            if(span.isRecording()) {
                span.setAttribute("db.system", system);
                span.setAttribute("db.system.name", system);
                if(operation != null) {
                    span.setAttribute("db.operation.name", operation);
                }
                if(sql != null) {
                    span.setAttribute("db.query.text", sql);
                }
            }
        } catch (RuntimeException err) {
            failed(err);
        }
        return span;
    }

    /** An integer attribute on a span, guarded like every other hook. */
    static void setAttribute(Span span, String key, long value) {
        if(span == null) {
            return;
        }
        try {
            if(span.isRecording()) {
                span.setAttribute(key, value);
            }
        } catch (RuntimeException err) {
            failed(err);
        }
    }

    /** Whether the caller's own header lines already carry a traceparent. */
    static boolean callerTraceparent(List callerHeaders) {
        if(callerHeaders == null) {
            return false;
        }
        for(int iter = 0 ; iter < callerHeaders.size() ; iter++) {
            String line = String.valueOf(callerHeaders.get(iter)).trim();
            if(line.regionMatches(true, 0, TRACEPARENT, 0, TRACEPARENT.length())
                    && line.length() > TRACEPARENT.length()
                    && (line.charAt(TRACEPARENT.length()) == ':'
                        || line.charAt(TRACEPARENT.length()) == ' ')) {
                return true;
            }
        }
        return false;
    }

    /** Ends a statement's span. */
    static void endDatabase(Span span, Throwable error) {
        if(span == null) {
            return;
        }
        if(error != null) {
            guardedException(span, error);
        }
        finish(span);
    }

    /**
     * The span for one Lambda invocation, made current.
     *
     * <p>The host hands the invocation's trace over in X-Ray's own format rather
     * than W3C's; when that is all there is, it is translated so the invocation
     * still joins the caller's trace. The two describe the same 128-bit id: X-Ray
     * writes it as a version, 8 hex digits of time and 24 of randomness, and W3C
     * as the 32 of them run together.
     */
    static Span startLambda(String traceHeader, String requestId) {
        Tracer t = tracer;
        if(t == null) {
            return null;
        }
        Span span = null;
        try {
            String name = System.getenv("AWS_LAMBDA_FUNCTION_NAME");
            span = t.startSpan(name == null ? "invoke" : name, Span.KIND_SERVER, null,
                    fromXRay(traceHeader), null);
            if(span == null) {
                return null;
            }
            if(span.isRecording()) {
                span.setAttribute("cloud.provider", "aws");
                span.setAttribute("faas.trigger", "other");
                if(requestId != null) {
                    span.setAttribute("faas.invocation_id", requestId);
                }
            }
            enter(span);
            return span;
        } catch (RuntimeException err) {
            failed(err);
            abandon(span);
            return null;
        }
    }

    /**
     * Ends an invocation's span. The runtime loop then flushes before it polls
     * again, rather than here, so the result is posted without waiting on the
     * collector.
     */
    static void endLambda(Span span, Throwable error) {
        endLambda(span, error, null);
    }

    /**
     * Ends an invocation span with what went wrong, in order: the invocation's own
     * failure, then a failure to tell the host about it. Either may be null.
     */
    static void endLambda(Span span, Throwable error, Throwable reporting) {
        if(span == null) {
            return;
        }
        if(error != null) {
            guardedException(span, error);
        }
        if(reporting != null) {
            guardedException(span, reporting);
        }
        finish(span);
    }

    /** Flushes the installed tracer, if any. */
    static void flush(int timeoutMillis) {
        Tracer t = tracer;
        if(t == null) {
            return;
        }
        try {
            t.flush(timeoutMillis);
        } catch (RuntimeException err) {
            failed(err);
        }
    }

    /**
     * Flushes, stops and uninstalls {@code owned} -- if it is still the installed
     * tracer. One that has been replaced was already shut down by
     * {@link #install}, and whatever replaced it belongs to someone else.
     */
    static void shutdown(Tracer owned, int timeoutMillis) {
        Tracer t = tracer;
        if(t == null || t != owned) {
            return;
        }
        tracer = null;
        try {
            t.shutdown(timeoutMillis);
        } catch (RuntimeException err) {
            failed(err);
        }
    }

    /** The tracer's counters, when one is installed. */
    static void metrics(Map out) {
        Tracer t = tracer;
        if(t == null) {
            return;
        }
        try {
            t.metrics(out);
        } catch (RuntimeException err) {
            failed(err);
        }
    }

    // ------------------------------------------------------------------

    private static Span currentOrNull() {
        Object value = CURRENT.get();
        return value instanceof Span ? (Span)value : null;
    }

    /**
     * A child of the current span, made current. Null when tracing is off,
     * suppressed, or when a CLIENT span is already current: an insert that runs a
     * query of its own to learn its key is one statement to the caller, and an
     * outbound call cannot have another outbound call inside it.
     */
    private static Span begin(String name, int kind, String traceparent, String tracestate) {
        Tracer t = tracer;
        if(t == null || isSuppressed()) {
            return null;
        }
        Span parent = currentOrNull();
        try {
            // Inside the guard, like every other call into the tracer: this runs for
            // every outbound call and statement, and a span whose getKind throws must
            // not fail the operation it was only meant to observe.
            if(kind == Span.KIND_CLIENT && parent != null && parent.getKind() == Span.KIND_CLIENT) {
                return null;
            }
            Span span = t.startSpan(name, kind, parent, traceparent, tracestate);
            if(span == null) {
                return null;
            }
            enter(span);
            return span;
        } catch (RuntimeException err) {
            failed(err);
            return null;
        }
    }

    private static void enter(Span span) {
        span.previous = currentOrNull();
        span.entered = true;
        CURRENT.set(span);
    }

    /**
     * Makes {@code span} stop being this thread's current span WITHOUT ending it.
     * HTTP/2 serves several streams in one turn and writes their responses after
     * the last handler, so a span has to leave -- or the next stream's span would
     * start as its child -- before it can end.
     */
    static void leave(Span span) {
        if(span != null && span.entered) {
            span.entered = false;
            CURRENT.set(span.previous);
            span.previous = null;
        }
    }

    /** Ends a span and restores what was current before it. */
    private static void finish(Span span) {
        if(span.entered) {
            span.entered = false;
            CURRENT.set(span.previous);
            span.previous = null;
        }
        try {
            span.end();
        } catch (RuntimeException err) {
            failed(err);
        }
    }

    /**
     * Finishes a span the tracer handed out before a later call into it failed.
     * Dropping the reference instead would leave the tracer holding whatever state
     * it keeps for an open span -- once per request, for as long as the fault lasts.
     * Discarded, because what it recorded is incomplete; each step guarded, because
     * the tracer is already known to be failing.
     */
    private static void abandon(Span span) {
        if(span == null) {
            return;
        }
        try {
            span.discard();
        } catch (RuntimeException err) {
            // The tracer is already failing; ending the span below still matters.
            failed(err);
        }
        try {
            span.end();
        } catch (RuntimeException err) {
            failed(err);
        }
    }

    private static void guardedException(Span span, Throwable error) {
        try {
            span.recordException(error);
        } catch (RuntimeException err) {
            failed(err);
        }
    }

    /** Once per process: a broken tracer must not also flood the log. */
    private static void failed(RuntimeException err) {
        if(!reportedFailure) {
            reportedFailure = true;
            System.err.println("tracing failed and the operation continued untraced: " + err);
        }
    }

    /** "example.com:8080" to "example.com", "[::1]:80" to "::1". */
    static String hostOnly(String host) {
        String h = host.trim();
        if(h.startsWith("[")) {
            int close = h.indexOf(']');
            return close > 0 ? h.substring(1, close) : h;
        }
        int colon = h.indexOf(':');
        return colon >= 0 ? h.substring(0, colon) : h;
    }

    /** The host[:port] of an absolute URL, without userinfo; null when there is none. */
    static String authority(String url) {
        int scheme = url.indexOf("://");
        if(scheme < 0) {
            return null;
        }
        int start = scheme + 3;
        int end = url.length();
        for(int iter = start ; iter < url.length() ; iter++) {
            char c = url.charAt(iter);
            if(c == '/' || c == '?' || c == '#') {
                end = iter;
                break;
            }
        }
        String authority = url.substring(start, end);
        int at = authority.lastIndexOf('@');
        return at >= 0 ? authority.substring(at + 1) : authority;
    }

    /**
     * The SQL keyword a statement opens with, upper-cased, or null. Folded by
     * hand: String.toUpperCase is locale sensitive, and on a Turkish host
     * "insert" would come back with a dotted capital I.
     */
    static String firstKeyword(String sql) {
        if(sql == null) {
            return null;
        }
        int at = 0;
        int n = sql.length();
        while(at < n && (sql.charAt(at) <= ' ' || sql.charAt(at) == '(')) {
            at++;
        }
        StringBuilder out = new StringBuilder();
        while(at < n && out.length() < 16) {
            char c = sql.charAt(at);
            if(c >= 'a' && c <= 'z') {
                out.append((char)(c - 32));
            } else if(c >= 'A' && c <= 'Z') {
                out.append(c);
            } else {
                break;
            }
            at++;
        }
        return out.length() == 0 ? null : out.toString();
    }

    /**
     * X-Ray's {@code Root=1-5759e988-bd862e3fe1be46a994272793;Parent=53995c3f42cd8ad8;Sampled=1}
     * as a W3C traceparent, or null when it is absent or not in that shape.
     */
    static String fromXRay(String header) {
        if(header == null) {
            return null;
        }
        String root = null;
        String parent = null;
        String sampled = "0";
        int at = 0;
        while(at < header.length()) {
            int end = header.indexOf(';', at);
            if(end < 0) {
                end = header.length();
            }
            String part = header.substring(at, end).trim();
            at = end + 1;
            if(part.startsWith("Root=")) {
                root = part.substring(5);
            } else if(part.startsWith("Parent=")) {
                parent = part.substring(7);
            } else if(part.startsWith("Sampled=")) {
                sampled = part.substring(8);
            }
        }
        // 1-xxxxxxxx-xxxxxxxxxxxxxxxxxxxxxxxx: version, 8 hex, 24 hex.
        if(root == null || parent == null || root.length() != 35 || !root.startsWith("1-")
                || root.charAt(10) != '-' || parent.length() != 16) {
            return null;
        }
        String traceId = root.substring(2, 10) + root.substring(11);
        if(!isLowerHex(traceId) || !isLowerHex(parent)) {
            return null;
        }
        return "00-" + traceId + "-" + parent + "-" + ("1".equals(sampled) ? "01" : "00");
    }

    private static boolean isLowerHex(String value) {
        for(int iter = 0 ; iter < value.length() ; iter++) {
            char c = value.charAt(iter);
            if(!(c >= '0' && c <= '9') && !(c >= 'a' && c <= 'f')) {
                return false;
            }
        }
        return true;
    }

    /** What every hook answers with when there is no tracer. */
    private static final class NoopSpan extends Span {
        public Span setAttribute(String key, String value) {
            return this;
        }

        public Span setAttribute(String key, long value) {
            return this;
        }

        public Span setAttribute(String key, double value) {
            return this;
        }

        public Span setAttribute(String key, boolean value) {
            return this;
        }

        public Span recordException(Throwable error) {
            return this;
        }

        public Span setError(String description) {
            return this;
        }

        public Span updateName(String name) {
            return this;
        }

        public String getName() {
            return "";
        }

        public int getKind() {
            return KIND_INTERNAL;
        }

        public boolean isRecording() {
            return false;
        }

        public String traceparent() {
            return null;
        }

        public String tracestate() {
            return null;
        }

        public void discard() {
        }

        public void end() {
        }
    }
}
