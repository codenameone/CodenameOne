/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bazaar.web;

import com.codename1.backend.Crypto;
import com.codename1.backend.Http1Date;
import com.codename1.backend.HttpServer;
import com.codenameone.examples.bazaar.Ids;

import java.io.IOException;

/// HTTP validators for the public pages: an `ETag` from a digest of the body,
/// so an unchanged page costs a browser or a CDN a 304 and no bytes, and
/// `Last-Modified` where the data says when it last changed.
final class Conditional {
    private Conditional() {
    }

    /// A weak entity tag for `body`: equal bodies, equal tags.
    static String tag(byte[] body) throws IOException {
        return "W/\"" + Ids.hex(Crypto.sha256(body)).substring(0, 32) + "\"";
    }

    /// Whether the request's `If-None-Match` names `tag` (or `*`).
    static boolean matches(HttpServer.Request request, String tag) {
        String wanted = request.getHeader("If-None-Match");
        if (wanted == null) {
            return false;
        }
        String bare = tag.startsWith("W/") ? tag.substring(2) : tag;
        // indexOf, not split: the device class library has no regex.
        int start = 0;
        while (start <= wanted.length()) {
            int comma = wanted.indexOf(',', start);
            int end = comma < 0 ? wanted.length() : comma;
            String p = wanted.substring(start, end).trim();
            start = end + 1;
            if (p.equals("*") || p.equals(tag) || p.equals(bare)
                    || p.startsWith("W/") && p.substring(2).equals(bare)) {
                return true;
            }
        }
        return false;
    }

    /// `body` with validators and `cacheControl`, or a 304 when the client
    /// already has it. `lastModified` is epoch milliseconds, or 0 for unknown.
    static HttpServer.Response answer(HttpServer.Request request, String type, byte[] body,
            String cacheControl, long lastModified) throws IOException {
        String tag = tag(body);
        boolean fresh = matches(request, tag);
        if (!fresh && lastModified > 0 && request.getHeader("If-None-Match") == null) {
            String since = request.getHeader("If-Modified-Since");
            long at = since == null ? -1 : Http1Date.parse(since);
            fresh = at >= 0 && lastModified / 1000 <= at / 1000;
        }
        HttpServer.Response out = fresh ? new HttpServer.Response(304, type, new byte[0])
                : new HttpServer.Response(200, type, body);
        out.header("ETag", tag).header("Cache-Control", cacheControl);
        if (lastModified > 0) {
            out.header("Last-Modified", Http1Date.format(lastModified));
        }
        return out;
    }
}
