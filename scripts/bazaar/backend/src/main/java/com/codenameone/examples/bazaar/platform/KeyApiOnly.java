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
package com.codenameone.examples.bazaar.platform;

import com.codename1.backend.HttpServer;
import com.codename1.backend.security.Authentication;
import com.codename1.backend.security.FilterChain;
import com.codename1.backend.security.SecurityContext;
import com.codename1.backend.security.SecurityContextHolder;
import com.codename1.backend.security.SecurityFilter;
import com.codename1.backend.security.apikey.ApiKeyAuthenticationToken;

/// Keeps an API key to the key API. A key signs a request in with no role,
/// so the staff and admin routes refuse it already; this refuses it on the
/// rest of `/api` too -- the account, cards, carts -- which only ask that
/// somebody be signed in, and would otherwise act for an account named
/// `apikey:...`. Runs right after the filter that accepts keys.
public final class KeyApiOnly implements SecurityFilter {
    /// What every key API path starts with.
    public static final String PATH = "/api/v1/";

    @Override
    public HttpServer.Response doFilter(HttpServer.Request request, FilterChain chain)
            throws Exception {
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication a = context == null ? null : context.getAuthentication();
        String target = request.getTarget();
        int query = target == null ? -1 : target.indexOf('?');
        String path = query < 0 ? target : target.substring(0, query);
        // A dot segment or an escape could name another route once decoded,
        // so a key's path must be plain as well as under the prefix.
        if (a instanceof ApiKeyAuthenticationToken && (path == null || !path.startsWith(PATH)
                || path.indexOf("..") >= 0 || path.indexOf('%') >= 0
                || path.indexOf('\\') >= 0)) {
            return HttpServer.Response.json(403,
                    "{\"error\":\"An API key reaches only the key API under /api/v1\"}");
        }
        return chain.doFilter(request);
    }
}
