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
package com.codenameone.examples.bazaar.web.security;

import com.codename1.backend.HttpServer;
import com.codename1.backend.security.FilterChain;
import com.codename1.backend.security.SecurityExchange;
import com.codename1.backend.security.SecurityFilter;

/// A year of immutable caching for a static file whose name carries its
/// content hash -- `app.1a2b3c4d.css`, eight hex digits before the extension.
/// The bytes behind such a name never change (a test holds every name to its
/// bytes, `WebAssetsTest`), so a browser need never ask again. Any other
/// static file keeps the server's `no-cache`.
final class ImmutableAssets implements SecurityFilter {
    @Override
    public HttpServer.Response doFilter(HttpServer.Request request, FilterChain chain)
            throws Exception {
        HttpServer.Response response = chain.doFilter(request);
        if (response != null && response.getStatus() == 200
                && fingerprinted(request.pathFrom(0))) {
            SecurityExchange.current().setResponseHeader("Cache-Control",
                    "public, max-age=31536000, immutable");
        }
        return response;
    }

    /// Whether `path` ends in `.<8 hex>.<ext>`.
    static boolean fingerprinted(String path) {
        if (path == null) {
            return false;
        }
        int ext = path.lastIndexOf('.');
        int dot = ext < 0 ? -1 : path.lastIndexOf('.', ext - 1);
        if (dot < 0 || ext - dot != 9) {
            return false;
        }
        for (int iter = dot + 1; iter < ext; iter++) {
            char c = path.charAt(iter);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))) {
                return false;
            }
        }
        return true;
    }
}
