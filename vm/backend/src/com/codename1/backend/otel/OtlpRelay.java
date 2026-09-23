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

import com.codename1.backend.Crypto;
import com.codename1.backend.HttpServer;
import com.codename1.backend.Json;
import com.codename1.backend.Tracing;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Accepts the app's spans and forwards them to the collector this server
 * exports to.
 *
 * <p>This is what lets a mobile app report spans without shipping the
 * collector's credentials in its binary -- anything inside an app package is
 * public -- and without the collector having to answer a browser's CORS
 * preflight. The app posts OTLP/JSON to its own backend, the way it already
 * talks to it, and the backend adds the ingest token on the way out.
 *
 * <p>JSON in, because the server hands a handler its request body as text and a
 * binary protobuf body would not survive that. What goes out is re-encoded, in
 * whichever protocol this server exports with, from a tree rebuilt against the
 * OTLP schema: a field the schema does not name is dropped, a malformed id is
 * refused, and only then does the payload join the export queue. The relay is a
 * public endpoint, so it treats what it receives as input, not as a message to
 * pass along.
 *
 * <p>It answers as soon as the payload is queued. Forwarding inside the request
 * would block the connection's host thread on the collector (see
 * {@link BatchExporter}), and the client does not need to wait for the collector
 * to hear about its own spans.
 */
final class OtlpRelay implements HttpServer.Handler {
    private final byte[] path;
    private final String pathText;
    private final String token;
    private final int maxBytes;
    private final int maxSpans;
    private final String corsOrigin;
    private final BatchExporter exporter;

    /** The header a client puts the relay token in. */
    static final String TOKEN_HEADER = "x-cn1-telemetry-token";

    OtlpRelay(String path, String token, int maxBytes, int maxSpans, String corsOrigin,
              BatchExporter exporter) {
        this.pathText = path;
        this.path = ascii(path);
        this.token = token == null || token.length() == 0 ? null : token;
        this.maxBytes = maxBytes;
        this.maxSpans = maxSpans;
        this.corsOrigin = corsOrigin == null || corsOrigin.length() == 0 ? null : corsOrigin;
        this.exporter = exporter;
    }

    public HttpServer.Response handle(HttpServer.Request request) throws Exception {
        if(!request.pathIs(path)) {
            return null;
        }
        // The relay's own request is not a trace anyone asked for: a client
        // exporting every few seconds would otherwise fill the backend's traces
        // with the exports themselves.
        Tracing.current().discard();
        String method = request.getMethod();
        if("OPTIONS".equals(method)) {
            return preflight();
        }
        if(!"POST".equals(method)) {
            return answer(405, "POST OTLP/JSON to " + pathText);
        }
        if(token != null) {
            String offered = request.getHeader(TOKEN_HEADER);
            // UTF-8 on both sides, not the ASCII folding used for fixed replies:
            // that mapped every non-ASCII character to '?', so distinct tokens
            // compared equal: "s?cret" opened a relay whose token had an accented e.
            if(offered == null || !Crypto.equalsConstantTime(utf8(offered), utf8(token))) {
                return answer(401, "missing or wrong " + TOKEN_HEADER);
            }
        }
        String type = request.getHeader("content-type");
        if(type == null || !type.regionMatches(true, 0, "application/json", 0, 16)) {
            return answer(415, "the relay accepts application/json");
        }
        String body = request.getBody();
        if(body == null || body.length() == 0) {
            return answer(400, "empty export");
        }
        // Characters, a lower bound on bytes: close enough for a ceiling whose
        // job is to keep a hostile body from being parsed at all.
        if(body.length() > maxBytes) {
            return answer(413, "export larger than " + maxBytes + " bytes");
        }
        byte[] encoded;
        try {
            Object parsed = Json.parse(body);
            if(!(parsed instanceof Map)) {
                return answer(400, "an export is a JSON object");
            }
            Map clean = OtlpSchema.sanitize((Map)parsed);
            int spans = OtlpSchema.countSpans(clean);
            if(spans == 0) {
                return ok();
            }
            if(spans > maxSpans) {
                return answer(413, "export holds more than " + maxSpans + " spans");
            }
            encoded = exporter.isProtobuf() ? OtlpSchema.protobuf(clean) : OtlpSchema.json(clean);
        } catch (Exception err) {
            return answer(400, "not an OTLP trace export: " + err.getMessage());
        }
        if(!exporter.addRelayed(encoded,
                exporter.isProtobuf() ? "application/x-protobuf" : "application/json")) {
            // OTLP/HTTP's own signal for "try again later", and what a client's
            // exporter already backs off on.
            return answer(503, "the relay queue is full");
        }
        return ok();
    }

    private HttpServer.Response preflight() {
        if(corsOrigin == null) {
            return answer(405, "cross-origin export is not enabled on this relay");
        }
        Map headers = cors();
        headers.put("Access-Control-Allow-Methods", "POST, OPTIONS");
        headers.put("Access-Control-Allow-Headers", "Content-Type, X-CN1-Telemetry-Token");
        headers.put("Access-Control-Max-Age", "86400");
        return HttpServer.Response.empty(204, "text/plain", headers);
    }

    /** An empty ExportTraceServiceResponse: success, nothing rejected. */
    private HttpServer.Response ok() {
        return new HttpServer.Response(200, "application/json", ascii("{}"), cors());
    }

    private HttpServer.Response answer(int status, String message) {
        return new HttpServer.Response(status, "text/plain; charset=utf-8", ascii(message), cors());
    }

    private Map cors() {
        Map headers = new LinkedHashMap();
        if(corsOrigin != null) {
            headers.put("Access-Control-Allow-Origin", corsOrigin);
            headers.put("Vary", "Origin");
        }
        return headers;
    }

    private static byte[] utf8(String value) throws java.io.IOException {
        return value.getBytes("UTF-8");
    }

    private static byte[] ascii(String value) {
        byte[] out = new byte[value.length()];
        for(int iter = 0 ; iter < out.length ; iter++) {
            char c = value.charAt(iter);
            out[iter] = (byte)(c < 0x80 ? c : '?');
        }
        return out;
    }
}
