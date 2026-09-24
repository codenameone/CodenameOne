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
package com.codename1.backend.otel;

import com.codename1.backend.Config;
import com.codename1.backend.Crypto;
import com.codename1.backend.HttpServer;
import com.codename1.backend.Span;
import com.codename1.backend.Tracer;
import com.codename1.backend.Tracing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * OpenTelemetry tracing over OTLP/HTTP, with no OpenTelemetry library behind it.
 *
 * <p>The OpenTelemetry Java SDK cannot run here: the server is translated to C
 * against a Java subset with no reflection and no service loading, and a binary
 * that linked the SDK would be carrying it whether or not anyone traced. This is
 * the part of it a server actually needs -- W3C trace context, the standard
 * samplers, a bounded batch exporter, and the OTLP wire format in both its
 * encodings -- and it is only in the binary when the project enables it.
 *
 * <p>Configured the way every OpenTelemetry SDK is, so operations tooling works
 * unchanged. Each setting has a {@code cn1.otel.*} key, readable from
 * application.properties like any other, and the standard {@code OTEL_*}
 * environment variable is honoured for it too:
 *
 * <table>
 *   <tr><th>key</th><th>variable</th><th>default</th></tr>
 *   <tr><td>cn1.otel.endpoint</td><td>OTEL_EXPORTER_OTLP_ENDPOINT</td>
 *       <td>http://localhost:4318; /v1/traces is appended</td></tr>
 *   <tr><td>cn1.otel.traces.endpoint</td><td>OTEL_EXPORTER_OTLP_TRACES_ENDPOINT</td>
 *       <td>used as it is, and wins over the one above</td></tr>
 *   <tr><td>cn1.otel.headers</td><td>OTEL_EXPORTER_OTLP_HEADERS</td>
 *       <td>none; {@code name=value,name=value}</td></tr>
 *   <tr><td>cn1.otel.protocol</td><td>OTEL_EXPORTER_OTLP_PROTOCOL</td>
 *       <td>http/protobuf, or http/json</td></tr>
 *   <tr><td>cn1.otel.service.name</td><td>OTEL_SERVICE_NAME</td>
 *       <td>what the build named it, else unknown_service</td></tr>
 *   <tr><td>cn1.otel.resource.attributes</td><td>OTEL_RESOURCE_ATTRIBUTES</td><td>none</td></tr>
 *   <tr><td>cn1.otel.sampler</td><td>OTEL_TRACES_SAMPLER</td><td>parentbased_always_on</td></tr>
 *   <tr><td>cn1.otel.sampler.arg</td><td>OTEL_TRACES_SAMPLER_ARG</td><td>the ratio, 1</td></tr>
 *   <tr><td>cn1.otel.disabled</td><td>OTEL_SDK_DISABLED</td><td>false</td></tr>
 * </table>
 *
 * <p>{@code cn1.otel.attributes.exclude} names attributes never to record, as a
 * comma separated list ({@code db.query.text,user_agent.original}), and
 * {@code cn1.otel.relay=true} opens the endpoint the app's own spans are relayed
 * through; see {@link OtlpRelay}.
 */
public final class OtlpTracer implements Tracer {
    public static final String DISABLED = "cn1.otel.disabled";
    public static final String ENDPOINT = "cn1.otel.endpoint";
    public static final String TRACES_ENDPOINT = "cn1.otel.traces.endpoint";
    public static final String HEADERS = "cn1.otel.headers";
    public static final String TRACES_HEADERS = "cn1.otel.traces.headers";
    public static final String PROTOCOL = "cn1.otel.protocol";
    public static final String TRACES_PROTOCOL = "cn1.otel.traces.protocol";
    public static final String SERVICE_NAME = "cn1.otel.service.name";
    public static final String RESOURCE_ATTRIBUTES = "cn1.otel.resource.attributes";
    public static final String SAMPLER = "cn1.otel.sampler";
    public static final String SAMPLER_ARG = "cn1.otel.sampler.arg";
    public static final String ATTRIBUTES_EXCLUDE = "cn1.otel.attributes.exclude";
    public static final String QUEUE_SIZE = "cn1.otel.queue.size";
    public static final String BATCH_SIZE = "cn1.otel.batch.size";
    public static final String EXPORT_DELAY = "cn1.otel.export.delayMillis";
    public static final String RELAY = "cn1.otel.relay";
    public static final String RELAY_PATH = "cn1.otel.relay.path";
    public static final String RELAY_TOKEN = "cn1.otel.relay.token";
    public static final String RELAY_MAX_BYTES = "cn1.otel.relay.maxBytes";
    public static final String RELAY_MAX_SPANS = "cn1.otel.relay.maxSpans";
    public static final String RELAY_CORS_ORIGIN = "cn1.otel.relay.corsOrigin";

    /** What the instrumentation scope is called in every export. */
    static final String SCOPE_NAME = "com.codename1.backend";

    private final String defaultServiceName;
    private Sampler sampler;
    private Set excluded = new HashSet();
    private BatchExporter exporter;
    private OtlpRelay relay;
    private long idState;

    /** A tracer whose service name comes from configuration alone. */
    public OtlpTracer() {
        this(null);
    }

    /**
     * @param defaultServiceName used when neither {@code cn1.otel.service.name}
     *        nor {@code OTEL_SERVICE_NAME} is set; the build passes the name
     *        {@code @OpenTelemetry} gave
     */
    public OtlpTracer(String defaultServiceName) {
        this.defaultServiceName = defaultServiceName;
    }

    /**
     * A tracer opened against {@code config}, or null when the configuration
     * turns tracing off. For a program that starts {@link HttpServer} or the
     * Lambda loop itself:
     *
     * <pre>
     *   Tracing.install(OtlpTracer.open(Config.load(), "orders"));
     * </pre>
     */
    public static OtlpTracer open(Config config, String defaultServiceName) throws IOException {
        OtlpTracer tracer = new OtlpTracer(defaultServiceName);
        return tracer.open(config) ? tracer : null;
    }

    public boolean open(Config config) throws IOException {
        if(config.getBoolean(DISABLED, false)) {
            return false;
        }
        sampler = Sampler.parse(config.get(SAMPLER), config.get(SAMPLER_ARG));
        excluded = splitSet(config.get(ATTRIBUTES_EXCLUDE));

        String protocol = config.get(TRACES_PROTOCOL, config.get(PROTOCOL, "http/protobuf")).trim();
        boolean protobuf;
        if("http/protobuf".equals(protocol)) {
            protobuf = true;
        } else if("http/json".equals(protocol)) {
            protobuf = false;
        } else {
            // grpc is the one other value the specification names, and the one a
            // copied collector config most often carries. Refused by name: the
            // server would otherwise start and send nothing anyone receives.
            throw new IOException(PROTOCOL + " is '" + protocol + "'; this server exports "
                    + "OTLP over HTTP, so use http/protobuf or http/json, and point the "
                    + "endpoint at the collector's HTTP port (4318 by default, not 4317)");
        }

        String endpoint = config.get(TRACES_ENDPOINT);
        if(endpoint == null || endpoint.trim().length() == 0) {
            String base = config.get(ENDPOINT, "http://localhost:4318").trim();
            endpoint = appendTracesPath(base);
        }
        endpoint = endpoint.trim();
        if(!hasHttpAuthority(endpoint)) {
            throw new IOException("The trace endpoint must be an http or https URL naming "
                    + "a host and is '" + BatchExporter.redact(endpoint) + "'");
        }

        // The signal-specific setting REPLACES the generic one, as the endpoint and
        // protocol settings do and as the specification says: it is not a list to
        // append to. Appending sent both, and Web sends every line, so a deployment
        // that set a traces-only token next to a generic one exported two
        // Authorization headers and was refused by collectors that reject that.
        List headers = new ArrayList();
        String tracesHeaders = config.get(TRACES_HEADERS);
        parseHeaders(tracesHeaders != null ? tracesHeaders : config.get(HEADERS), headers);

        Map resourceAttributes = new LinkedHashMap();
        parsePairs(config.get(RESOURCE_ATTRIBUTES), resourceAttributes, RESOURCE_ATTRIBUTES);
        // Each source in turn, a blank one counting as absent: tested raw, a name
        // of " " was chosen and then trimmed to an empty service.name, where the
        // specification wants unknown_service.
        Object fromResource = resourceAttributes.get("service.name");
        String service = nonBlank(config.get(SERVICE_NAME));
        if(service == null) {
            service = nonBlank(fromResource == null ? null : String.valueOf(fromResource));
        }
        if(service == null) {
            service = nonBlank(defaultServiceName);
        }
        resourceAttributes.put("service.name", service == null ? "unknown_service" : service);
        resourceAttributes.put("telemetry.sdk.name", "codenameone");
        resourceAttributes.put("telemetry.sdk.language", "java");
        Map resource = new LinkedHashMap();
        resource.put("attributes", keyValues(resourceAttributes));

        int queue = positive(config, QUEUE_SIZE, 2048);
        int batch = Math.min(queue, positive(config, BATCH_SIZE, 512));
        int delay = positive(config, EXPORT_DELAY, 5000);
        int relayBytes = positive(config, RELAY_MAX_BYTES, 1024 * 1024);

        seedIds();
        exporter = new BatchExporter(endpoint, headers, protobuf, resource, queue, batch,
                delay, relayBytes * 4L);
        if(config.getBoolean(RELAY, false)) {
            String path = canonicalPath(config.get(RELAY_PATH, "/otel/v1/traces").trim());
            if(path == null) {
                throw new IOException(RELAY_PATH + " must be a path such as /otel/v1/traces: "
                        + "it starts with /, has no query or fragment, and uses only the "
                        + "ASCII characters a URL path allows (percent-encode anything "
                        + "else); it is '" + path + "'");
            }
            relay = new OtlpRelay(path, config.get(RELAY_TOKEN), relayBytes,
                    positive(config, RELAY_MAX_SPANS, 1000), corsOrigin(config),
                    exporter);
        }
        exporter.start();
        return true;
    }

    public Span startSpan(String name, int kind, Span parent, String traceparent,
                          String tracestate) {
        long hi;
        long lo;
        long parentId = 0;
        boolean hasParent = false;
        boolean parentSampled = false;
        boolean remote = false;
        String state = null;
        // Only a parent THIS tracer made. After Tracing.install() replaces the
        // tracer, a request still in flight holds the old one's span as current,
        // and adopting it filed the new tracer's children under the old trace, its
        // sampling decision and its clock -- exported, possibly, to another
        // collector than their parent's. Such a child starts a trace of its own.
        OtelSpan local = parent instanceof OtelSpan && ((OtelSpan)parent).isFrom(this)
                ? (OtelSpan)parent : null;
        if(local != null) {
            hi = local.traceHi;
            lo = local.traceLo;
            parentId = local.spanId;
            parentSampled = local.sampled;
            state = local.tracestate;
            hasParent = true;
        } else {
            TraceContext context = TraceContext.parse(traceparent);
            if(context != null) {
                hi = context.traceHi;
                lo = context.traceLo;
                parentId = context.spanId;
                parentSampled = context.sampled();
                state = TraceContext.vetTracestate(tracestate);
                hasParent = true;
                remote = true;
            } else {
                hi = nextId();
                lo = nextId();
            }
        }
        boolean sampled = sampler.sample(hasParent, parentSampled, lo);
        return new OtelSpan(this, name, kind, hi, lo, nextId(), parentId, remote, sampled, state,
                local);
    }

    public void flush(int timeoutMillis) {
        if(exporter != null) {
            exporter.flush(timeoutMillis);
        }
    }

    public void shutdown(int timeoutMillis) {
        if(exporter != null) {
            exporter.shutdown(timeoutMillis);
        }
    }

    public HttpServer.Handler relay() {
        return relay;
    }

    public void metrics(Map out) {
        if(exporter != null) {
            exporter.metrics(out);
        }
    }

    void ended(OtelSpan span) {
        exporter.add(span);
    }

    /**
     * {@code path} as the server will compare it, or null when it is not an
     * origin-form path the relay could ever match.
     *
     * <p>Only RFC 3986 pchar and "/": a non-ASCII character used to be folded to
     * '?', so the relay listened somewhere nobody configured, and a query or
     * fragment could never match, since only the path is compared.
     *
     * <p>Then normalized as the server normalizes every request path before
     * {@code Request.pathIs} compares it (RFC 3986 6.2.2): an escaped unreserved
     * character is decoded and a kept escape gets upper-case hex digits. Compared
     * as configured, {@code /otel/%74races} or {@code /otel/%2f} could never
     * match any request. A '%' not followed by two hex digits is refused.
     */
    static String canonicalPath(String path) {
        if(path.length() == 0 || path.charAt(0) != '/') {
            return null;
        }
        StringBuilder out = new StringBuilder(path.length());
        for(int iter = 0 ; iter < path.length() ; iter++) {
            char c = path.charAt(iter);
            if(c == '%') {
                int hi = iter + 2 < path.length() ? hexValue(path.charAt(iter + 1)) : -1;
                int lo = hi < 0 ? -1 : hexValue(path.charAt(iter + 2));
                if(lo < 0) {
                    return null;
                }
                char decoded = (char)((hi << 4) | lo);
                if(isUnreserved(decoded)) {
                    out.append(decoded);
                } else {
                    out.append('%').append(Character.toUpperCase(path.charAt(iter + 1)))
                            .append(Character.toUpperCase(path.charAt(iter + 2)));
                }
                iter += 2;
                continue;
            }
            if(!isUnreserved(c) && "!$&'()*+,;=:@/".indexOf(c) < 0) {
                return null;
            }
            out.append(c);
        }
        return out.toString();
    }

    private static boolean isUnreserved(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                || c == '-' || c == '.' || c == '_' || c == '~';
    }

    private static int hexValue(char c) {
        if(c >= '0' && c <= '9') {
            return c - '0';
        }
        if(c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        if(c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        return -1;
    }

    /**
     * {@code cn1.otel.relay.corsOrigin}, checked: {@code *}, or ONE serialized
     * origin -- scheme, host and port, nothing after. The value goes verbatim into
     * Access-Control-Allow-Origin, and a browser matches it character for
     * character against the page's origin, so a path, a trailing slash or a
     * comma-separated list matches nothing: every preflight failed while the
     * backend and the app's fail-silent exporter both looked configured.
     */
    static String corsOrigin(Config config) throws IOException {
        String value = config.get(RELAY_CORS_ORIGIN);
        if(value == null || value.trim().length() == 0) {
            return null;
        }
        value = value.trim();
        if("*".equals(value)) {
            return value;
        }
        int start = value.regionMatches(true, 0, "https://", 0, 8) ? 8
                : value.regionMatches(true, 0, "http://", 0, 7) ? 7 : -1;
        boolean valid = start > 0 && hasHttpAuthority(value)
                && value.indexOf('/', start) < 0 && value.indexOf('?') < 0
                && value.indexOf('#') < 0 && value.indexOf('@') < 0
                && value.indexOf(',') < 0;
        if(!valid) {
            throw new IOException(RELAY_CORS_ORIGIN + " must be * or one origin such as "
                    + "https://app.example.com (scheme, host and port, with no path or "
                    + "trailing slash); it is '" + value + "'");
        }
        return serializedOrigin(value, start);
    }

    /**
     * An origin as a browser serializes it -- lower-case scheme and host, no
     * default port -- since that is the string Access-Control-Allow-Origin is
     * compared against, character for character. HTTPS://APP.EXAMPLE.COM:443 was
     * accepted as written and matched no page. Called on a value already checked
     * to be scheme://host[:port].
     */
    private static String serializedOrigin(String value, int start) {
        String scheme = start == 8 ? "https" : "http";
        String hostPort = value.substring(start);
        int close = hostPort.lastIndexOf(']');
        int colon = hostPort.lastIndexOf(':');
        String host = hostPort;
        String port = null;
        if(colon > close) {
            host = hostPort.substring(0, colon);
            port = hostPort.substring(colon + 1);
        }
        StringBuilder out = new StringBuilder(value.length());
        out.append(scheme).append("://");
        for(int iter = 0 ; iter < host.length() ; iter++) {
            char c = host.charAt(iter);
            out.append(c >= 'A' && c <= 'Z' ? (char)(c + 32) : c);
        }
        if(port != null && port.length() > 0) {
            int number = Integer.parseInt(port);
            if(number != (start == 8 ? 443 : 80)) {
                out.append(':').append(number);
            }
        }
        return out.toString();
    }

    /**
     * Whether {@code url} is http or https with a host, and a valid port if it
     * names one. A bare {@code https://} passed a scheme check and started an
     * exporter whose every POST then failed: the server ran, validated, and
     * produced no traces.
     */
    static boolean hasHttpAuthority(String url) {
        // The WHOLE URL first, by the rule Web applies when it sends: no space,
        // control or DEL anywhere. Only the authority was checked, so a space in
        // the path passed and every export then failed at transport, silently.
        for(int iter = 0 ; iter < url.length() ; iter++) {
            char c = url.charAt(iter);
            if(c <= 0x20 || c == 0x7f) {
                return false;
            }
        }
        int start;
        if(url.regionMatches(true, 0, "http://", 0, 7)) {
            start = 7;
        } else if(url.regionMatches(true, 0, "https://", 0, 8)) {
            start = 8;
        } else {
            return false;
        }
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
        if(at >= 0 && !validUserinfo(authority.substring(0, at))) {
            return false;
        }
        String hostPort = at < 0 ? authority : authority.substring(at + 1);
        String host;
        String port = null;
        if(hostPort.startsWith("[")) {
            int close = hostPort.indexOf(']');
            if(close < 0) {
                return false;
            }
            host = hostPort.substring(1, close);
            // An IPv6 literal: hex digits and colons, with dots for an embedded
            // IPv4 tail. Anything else is not an address any stack will parse.
            if(host.indexOf(':') < 0 || !onlyChars(host, "0123456789abcdefABCDEF:.")) {
                return false;
            }
            String rest = hostPort.substring(close + 1);
            if(rest.length() > 0) {
                if(rest.charAt(0) != ':') {
                    return false;
                }
                port = rest.substring(1);
            }
        } else {
            int colon = hostPort.lastIndexOf(':');
            host = colon < 0 ? hostPort : hostPort.substring(0, colon);
            port = colon < 0 ? null : hostPort.substring(colon + 1);
            // A DNS name or an IPv4 address: letters, digits, '-', '.', and the
            // other unreserved characters. A space, a control, a backslash or a
            // stray bracket passed the emptiness check and started an exporter no
            // resolver or libcurl could connect with, so every span was lost.
            if(!onlyChars(host, "-._~")) {
                return false;
            }
        }
        if(host.length() == 0) {
            return false;
        }
        // An empty port ("host:") is the scheme's default, as RFC 3986 allows.
        if(port != null && port.length() > 0) {
            if(port.length() > 5) {
                return false;
            }
            int value = 0;
            for(int iter = 0 ; iter < port.length() ; iter++) {
                char c = port.charAt(iter);
                if(c < '0' || c > '9') {
                    return false;
                }
                value = value * 10 + (c - '0');
            }
            return value > 0 && value <= 65535;
        }
        return true;
    }

    /** {@code value} trimmed, or null when that leaves nothing. */
    private static String nonBlank(String value) {
        if(value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() == 0 ? null : trimmed;
    }

    /**
     * RFC 3986 userinfo: unreserved characters, sub-delims, ':' and complete
     * percent escapes. Skipped over, a space, a control or a stray '%' in it
     * passed validation and failed only at transport, where exports fail silently.
     */
    static boolean validUserinfo(String userinfo) {
        for(int iter = 0 ; iter < userinfo.length() ; iter++) {
            char c = userinfo.charAt(iter);
            if(c == '%') {
                if(iter + 2 >= userinfo.length() || OtlpSchema.hexDigit(userinfo.charAt(iter + 1)) < 0
                        || OtlpSchema.hexDigit(userinfo.charAt(iter + 2)) < 0) {
                    return false;
                }
                iter += 2;
                continue;
            }
            if(!onlyChars(String.valueOf(c), "-._~!$&'()*+,;=:")) {
                return false;
            }
        }
        return true;
    }

    /** Whether every character of {@code value} is an ASCII letter, a digit, or one of {@code extra}. */
    private static boolean onlyChars(String value, String extra) {
        for(int iter = 0 ; iter < value.length() ; iter++) {
            char c = value.charAt(iter);
            if(!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || extra.indexOf(c) >= 0)) {
                return false;
            }
        }
        return true;
    }

    boolean excluded(String key) {
        return !excluded.isEmpty() && excluded.contains(key);
    }

    // ------------------------------------------------------------------
    // Ids
    // ------------------------------------------------------------------

    /**
     * Seeded once from the platform's secure generator and advanced with
     * SplitMix64, which is a bijection over its counter: ids never repeat inside a
     * process, and processes seeded independently collide only by chance. Trace
     * ids need to be unique and unpredictable enough not to be guessed into
     * someone else's trace, not secret, so a CSPRNG call per span -- a native call
     * into OpenSSL on the packaged runtime -- would buy nothing.
     */
    private void seedIds() {
        long seed;
        try {
            byte[] random = Crypto.randomBytes(8);
            seed = 0;
            for(int iter = 0 ; iter < 8 ; iter++) {
                seed = (seed << 8) | (random[iter] & 0xff);
            }
        } catch (IOException err) {
            seed = System.currentTimeMillis() ^ (System.nanoTime() << 21)
                    ^ System.identityHashCode(this);
        }
        idState = seed;
    }

    private synchronized long nextId() {
        long z;
        do {
            idState += 0x9E3779B97F4A7C15L;
            z = idState;
            z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
            z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
            z = z ^ (z >>> 31);
        } while(z == 0);
        return z;
    }

    // ------------------------------------------------------------------
    // The export tree
    // ------------------------------------------------------------------

    /** An ExportTraceServiceRequest for these spans, as OTLP/JSON's tree. */
    static Map exportRequest(Map resource, List spans) {
        List encoded = new ArrayList(spans.size());
        for(int iter = 0 ; iter < spans.size() ; iter++) {
            Object span = spans.get(iter);
            if(span instanceof OtelSpan) {
                encoded.add(spanTree((OtelSpan)span));
            }
        }
        Map scope = new LinkedHashMap();
        scope.put("name", SCOPE_NAME);
        Map scopeSpans = new LinkedHashMap();
        scopeSpans.put("scope", scope);
        scopeSpans.put("spans", encoded);
        List scopes = new ArrayList(1);
        scopes.add(scopeSpans);
        Map resourceSpans = new LinkedHashMap();
        resourceSpans.put("resource", resource);
        resourceSpans.put("scopeSpans", scopes);
        List all = new ArrayList(1);
        all.add(resourceSpans);
        Map request = new LinkedHashMap();
        request.put("resourceSpans", all);
        return request;
    }

    private static Map spanTree(OtelSpan span) {
        Map out = new LinkedHashMap();
        out.put("traceId", TraceContext.hex(span.traceHi) + TraceContext.hex(span.traceLo));
        out.put("spanId", TraceContext.hex(span.spanId));
        if(span.tracestate != null) {
            out.put("traceState", span.tracestate);
        }
        if(span.parentId != 0) {
            out.put("parentSpanId", TraceContext.hex(span.parentId));
        }
        // Trace flags in the low byte, then HAS_IS_REMOTE and IS_REMOTE: whether
        // the parent was in another process, which a backend uses to draw the
        // service boundary. Only when there IS a parent: the bits describe the
        // parent's context (trace.proto: "unknown, is not remote, is remote"), and
        // setting HAS_IS_REMOTE on a root claimed a local parent it does not have.
        long flags = span.sampled ? 1 : 0;
        if(span.parentId != 0) {
            flags |= 0x100 | (span.parentRemote ? 0x200 : 0);
        }
        out.put("flags", Long.valueOf(flags));
        out.put("name", span.name);
        out.put("kind", Integer.valueOf(span.kind));
        // Strings, as proto3's JSON mapping writes 64-bit integers.
        out.put("startTimeUnixNano", String.valueOf(span.startEpochNanos));
        out.put("endTimeUnixNano", String.valueOf(span.endEpochNanos));
        out.put("attributes", keyValues(span.attributes));
        if(span.droppedAttributes > 0) {
            out.put("droppedAttributesCount", Integer.valueOf(span.droppedAttributes));
        }
        if(span.events != null && !span.events.isEmpty()) {
            List events = new ArrayList(span.events.size());
            for(int iter = 0 ; iter < span.events.size() ; iter++) {
                Object[] event = (Object[])span.events.get(iter);
                Map e = new LinkedHashMap();
                e.put("timeUnixNano", String.valueOf(event[0]));
                e.put("name", event[1]);
                e.put("attributes", keyValues((Map)event[2]));
                events.add(e);
            }
            out.put("events", events);
        }
        if(span.droppedEvents > 0) {
            out.put("droppedEventsCount", Integer.valueOf(span.droppedEvents));
        }
        if(span.statusCode != 0) {
            Map status = new LinkedHashMap();
            if(span.statusMessage != null) {
                status.put("message", span.statusMessage);
            }
            status.put("code", Integer.valueOf(span.statusCode));
            out.put("status", status);
        }
        return out;
    }

    static List keyValues(Map attributes) {
        List out = new ArrayList(attributes == null ? 0 : attributes.size());
        if(attributes == null) {
            return out;
        }
        Iterator it = attributes.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry entry = (Map.Entry)it.next();
            Object v = entry.getValue();
            Map value = new LinkedHashMap();
            if(v instanceof Boolean) {
                value.put("boolValue", v);
            } else if(v instanceof Long || v instanceof Integer) {
                value.put("intValue", String.valueOf(v));
            } else if(v instanceof Double) {
                value.put("doubleValue", v);
            } else {
                value.put("stringValue", String.valueOf(v));
            }
            Map kv = new LinkedHashMap();
            kv.put("key", String.valueOf(entry.getKey()));
            kv.put("value", value);
            out.add(kv);
        }
        return out;
    }

    // ------------------------------------------------------------------
    // Configuration parsing
    // ------------------------------------------------------------------

    /**
     * The generic endpoint plus /v1/traces, appended to the PATH. An endpoint can
     * carry its credential in the query ({@code https://c.example/otlp?api-key=...});
     * appending to the whole string put the path inside the key's value and sent
     * the export to the base path with a corrupted credential.
     */
    static String appendTracesPath(String base) {
        int cut = base.length();
        int query = base.indexOf('?');
        int fragment = base.indexOf('#');
        if(query >= 0) {
            cut = query;
        }
        if(fragment >= 0 && fragment < cut) {
            cut = fragment;
        }
        String path = base.substring(0, cut);
        while(path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path + "/v1/traces" + base.substring(cut);
    }

    private static int positive(Config config, String key, int fallback) throws IOException {
        int value = config.getInt(key, fallback);
        if(value <= 0) {
            throw new IOException(key + " must be a positive number and is " + value);
        }
        return value;
    }

    private static Set splitSet(String list) {
        Set out = new HashSet();
        if(list == null) {
            return out;
        }
        int at = 0;
        while(at <= list.length()) {
            int comma = list.indexOf(',', at);
            if(comma < 0) {
                comma = list.length();
            }
            String item = list.substring(at, comma).trim();
            if(item.length() > 0) {
                out.add(item);
            }
            at = comma + 1;
        }
        return out;
    }

    /** OTEL_EXPORTER_OTLP_HEADERS: {@code name=value,...}, values percent-encoded. */
    private static void parseHeaders(String text, List out) throws IOException {
        Map pairs = new LinkedHashMap();
        parsePairs(text, pairs, HEADERS);
        Iterator it = pairs.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry entry = (Map.Entry)it.next();
            String name = String.valueOf(entry.getKey());
            if(name.length() == 0) {
                throw new IOException(HEADERS + " has an entry with no header name");
            }
            if(name.equalsIgnoreCase("content-type")) {
                // The exporter sets it from the protocol, and Web sends every line,
                // so a configured one went out as a SECOND Content-Type -- one of
                // which contradicts the body.
                throw new IOException(HEADERS + " sets Content-Type, which the exporter "
                        + "sets from " + PROTOCOL);
            }
            for(int iter = 0 ; iter < name.length() ; iter++) {
                char c = name.charAt(iter);
                boolean token = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                        || (c >= '0' && c <= '9') || "!#$%&'*+-.^_`|~".indexOf(c) >= 0;
                if(!token) {
                    throw new IOException(HEADERS + " names a header '" + name
                            + "' that is not a valid header name");
                }
            }
            out.add(name + ": " + entry.getValue());
        }
        // A value is percent-decoded, so %0A becomes a real newline here. Checked
        // now, by the rules Web applies when it sends: accepting it started a
        // server that then failed every export on that check and dropped every
        // span. The message names the header, never the value -- it is usually a
        // credential.
        try {
            Tracing.checkHeaderLines(out);
        } catch (IOException err) {
            IOException refused = new IOException(HEADERS + ": " + err.getMessage());
            refused.initCause(err);
            throw refused;
        }
    }

    /**
     * The W3C Baggage-style list both OTEL_EXPORTER_OTLP_HEADERS and
     * OTEL_RESOURCE_ATTRIBUTES use. A malformed entry is an error, not skipped:
     * the usual one is an Authorization header whose value was pasted with a
     * space where the specification wants %20, and silently dropping it is a
     * collector that answers 401 to every export.
     */
    static void parsePairs(String text, Map out, String key) throws IOException {
        if(text == null) {
            return;
        }
        int at = 0;
        while(at < text.length()) {
            int comma = text.indexOf(',', at);
            if(comma < 0) {
                comma = text.length();
            }
            String item = text.substring(at, comma).trim();
            at = comma + 1;
            if(item.length() == 0) {
                continue;
            }
            int eq = item.indexOf('=');
            if(eq <= 0) {
                throw new IOException(key + " must be name=value pairs separated by commas");
            }
            out.put(item.substring(0, eq).trim(), percentDecode(item.substring(eq + 1).trim(), key));
        }
    }

    private static String percentDecode(String value, String key) throws IOException {
        if(value.indexOf('%') < 0) {
            return value;
        }
        com.codename1.backend.ByteSink bytes = new com.codename1.backend.ByteSink(value.length());
        for(int iter = 0 ; iter < value.length() ; iter++) {
            char c = value.charAt(iter);
            if(c != '%') {
                // By CODE POINT: a supplementary character is two chars here, and
                // encoding each half on its own wrote invalid UTF-8 that the String
                // constructor below replaced -- corrupting a resource value, or a
                // credential so that every export was refused.
                if(Character.isHighSurrogate(c) && iter + 1 < value.length()
                        && Character.isLowSurrogate(value.charAt(iter + 1))) {
                    bytes.putCodePoint(Character.toCodePoint(c, value.charAt(iter + 1)));
                    iter++;
                    continue;
                }
                bytes.putCodePoint(c);
                continue;
            }
            if(iter + 2 >= value.length()) {
                throw new IOException(key + " has a malformed percent escape");
            }
            int hi = OtlpSchema.hexDigit(value.charAt(iter + 1));
            int lo = OtlpSchema.hexDigit(value.charAt(iter + 2));
            if(hi < 0 || lo < 0) {
                throw new IOException(key + " has a malformed percent escape");
            }
            bytes.put((hi << 4) | lo);
            iter += 2;
        }
        // Well-formed UTF-8, or refused. Decoding replaced a bad sequence
        // ("orders%C3%28") with U+FFFD, and resource attributes meet no later check,
        // so the tracer started and exported a mangled service.name. A decode that
        // re-encodes to anything but the same bytes was not well formed: a bad
        // sequence comes back as the replacement character, an overlong one as
        // its shorter spelling.
        byte[] raw = new byte[bytes.length()];
        System.arraycopy(bytes.bytes(), 0, raw, 0, raw.length);
        String decoded = new String(raw, "UTF-8");
        if(!java.util.Arrays.equals(raw, decoded.getBytes("UTF-8"))) {
            throw new IOException(key + " has a percent escape that is not well-formed UTF-8");
        }
        return decoded;
    }
}
