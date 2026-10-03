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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// gzip for the application's responses, from `cn1.server.compression.*` -- the
/// same three settings Spring Boot's `server.compression` has, and like it off
/// unless enabled: compression trades CPU for bytes, and a server behind a proxy
/// that already compresses pays twice.
///
/// A response is compressed when the client accepts gzip, its type is one of the
/// configured MIME types, its body is at least the minimum size, it is not a
/// file streamed from disk (that path is zero-copy, and compressing it would
/// read the whole file into memory), and its handler set no Content-Encoding of
/// its own.
final class Compression {
    static final String ENABLED = "cn1.server.compression.enabled";
    static final String MIN_SIZE = "cn1.server.compression.minResponseSize";
    static final String MIME_TYPES = "cn1.server.compression.mimeTypes";

    private static final String DEFAULT_TYPES = "text/html, text/xml, text/plain, text/css, "
            + "text/javascript, application/javascript, application/json, application/xml";

    private final int minSize;
    private final List types;

    private Compression(int minSize, List types) {
        this.minSize = minSize;
        this.types = types;
    }

    /// The configured policy, or null when compression is off.
    static Compression fromConfig(Config config) throws IOException {
        if (!config.getBoolean(ENABLED, false)) {
            return null;
        }
        int min = config.getInt(MIN_SIZE, 2048);
        if (min < 0) {
            throw new IOException(MIN_SIZE + " is " + min + "; it can't be negative");
        }
        String listed = config.get(MIME_TYPES, DEFAULT_TYPES);
        List types = new ArrayList();
        int pos = 0;
        while (pos <= listed.length()) {
            int comma = listed.indexOf(',', pos);
            int end = comma < 0 ? listed.length() : comma;
            String type = listed.substring(pos, end).trim();
            if (type.length() > 0) {
                types.add(type);
            }
            if (comma < 0) {
                break;
            }
            pos = comma + 1;
        }
        return new Compression(min, types);
    }

    /// Compresses `response` in place when the rules above allow it.
    void apply(HttpServer.Request request, HttpServer.Response response) throws IOException {
        // A 206 (or anything carrying Content-Range) is a range of the identity
        // representation: compressing it would leave the offsets describing bytes
        // the client never receives, and a resumed download assembled corrupt.
        if (response == null || response.fileFd >= 0
                || response.status == 204 || response.status == 304 || response.status == 206
                || response.hasHeader("Content-Range")
                || response.hasHeader("Content-Encoding") || !compressible(response.contentType)) {
            return;
        }
        response.serializeDeferredJson();
        byte[] body = response.body;
        if (body == null || body.length < minSize) {
            return;
        }
        // Chosen by Accept-Encoding from here on, whichever way it goes: the
        // identity answer varies too, or a shared cache that stored it for a
        // client without gzip serves it to every client after.
        response.appendToken("Vary", "Accept-Encoding");
        if (!acceptsGzip(request)) {
            return;
        }
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(body.length / 3 + 64);
        com.codename1.io.gzip.GZIPOutputStream gzip =
                new com.codename1.io.gzip.GZIPOutputStream(out);
        gzip.write(body, 0, body.length);
        gzip.close();
        response.body = out.toByteArray();
        response.header("Content-Encoding", "gzip");
    }

    private boolean compressible(String contentType) {
        if (contentType == null) {
            return false;
        }
        int semi = contentType.indexOf(';');
        String type = (semi < 0 ? contentType : contentType.substring(0, semi)).trim();
        for (Object listed : types) {
            if (type.equalsIgnoreCase((String) listed)) {
                return true;
            }
        }
        return false;
    }

    /// Whether Accept-Encoding lists gzip with a quality above zero (RFC 9110
    /// 12.5.3). An explicit gzip entry decides, wherever it sits in the list; `*`
    /// counts only when gzip is not named. Returning at the first match let
    /// `*;q=1, gzip;q=0` -- a client refusing gzip outright -- be sent gzip, and
    /// `*;q=0, gzip` be refused it.
    static boolean acceptsGzip(HttpServer.Request request) {
        String accept = request.getHeader("Accept-Encoding");
        if (accept == null) {
            return false;
        }
        int explicit = 0;     // 0 unnamed, 1 accepted, -1 refused
        int wildcard = 0;
        int pos = 0;
        while (pos <= accept.length()) {
            int comma = accept.indexOf(',', pos);
            int end = comma < 0 ? accept.length() : comma;
            String item = accept.substring(pos, end).trim();
            int semi = item.indexOf(';');
            String coding = (semi < 0 ? item : item.substring(0, semi)).trim();
            boolean accepted = semi < 0 || !zeroQuality(item.substring(semi + 1));
            if ("gzip".equalsIgnoreCase(coding) || "x-gzip".equalsIgnoreCase(coding)) {
                // gzip and its x-gzip alias are one coding: either accepting it is enough.
                explicit = accepted || explicit == 1 ? 1 : -1;
            } else if ("*".equals(coding)) {
                wildcard = accepted ? 1 : -1;
            }
            if (comma < 0) {
                break;
            }
            pos = comma + 1;
        }
        return explicit != 0 ? explicit == 1 : wildcard == 1;
    }

    private static boolean zeroQuality(String params) {
        String q = Multipart.parameter(params, "q");
        if (q == null) {
            return false;
        }
        q = q.trim();
        for (int iter = 0 ; iter < q.length() ; iter++) {
            char c = q.charAt(iter);
            if (c != '0' && c != '.') {
                return false;
            }
        }
        return q.length() > 0;
    }
}
