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
import com.codename1.backend.security.apikey.ApiKey;
import com.codename1.backend.security.apikey.ApiKeyAuthenticationToken;

/// Keeps an API key to the key API. A key signs a request in with no role,
/// so the staff and admin routes refuse it already; this refuses it on the
/// rest of `/api` too -- the account, cards, carts -- which only ask that
/// somebody be signed in, and would otherwise act for an account named
/// `apikey:...`. Runs right after the filter that accepts keys, and holds an
/// app's token to its call allowance too.
///
/// It also picks the API version a key API request is served at: the one
/// its [#VERSION_HEADER] names, or [#LATEST] without one. A version this
/// server does not know is 400, never a guess; every answer under the key
/// API carries the header with the version served.
public final class KeyApiOnly implements SecurityFilter {
    /// What every key API path starts with.
    public static final String PATH = "/api/v1/";
    /// The request header naming an API version, echoed on the answer.
    public static final String VERSION_HEADER = "X-Bazaar-Api-Version";
    /// The version served when a request names none.
    public static final String LATEST = "2026-10";
    /// Every version served; all answer alike today.
    static final String[] VERSIONS = {"2026-07", LATEST};

    private final AppCallLimits limits;

    public KeyApiOnly() {
        this(null);
    }

    /// With `limits`, an app installation's token is also held to its call
    /// allowance ([AppCallLimits]), and every answer to it says where it stands.
    public KeyApiOnly(AppCallLimits limits) {
        this.limits = limits;
    }

    /// The version `asked` names, or null for one there is not.
    public static String version(String asked) {
        if (asked == null || asked.length() == 0) {
            return LATEST;
        }
        for (int iter = 0; iter < VERSIONS.length; iter++) {
            if (VERSIONS[iter].equals(asked.trim())) {
                return VERSIONS[iter];
            }
        }
        return null;
    }

    @Override
    public HttpServer.Response doFilter(HttpServer.Request request, FilterChain chain)
            throws Exception {
        String target = request.getTarget();
        if (target == null || !target.startsWith(PATH)) {
            return keysOnly(request, chain);
        }
        String served = version(request.getHeader(VERSION_HEADER));
        if (served == null) {
            return HttpServer.Response.json(400, "{\"error\":\"Unknown API version; the versions are "
                    + VERSIONS[0] + " and " + LATEST + "\"}");
        }
        HttpServer.Response answer;
        try {
            answer = keysOnly(request, chain);
        } catch (com.codename1.backend.ResponseStatusException refused) {
            // Answered as the server answers it, with the version on it too.
            answer = HttpServer.Response.text(refused.getStatus(), refused.getReason());
        }
        return answer == null ? null : answer.header(VERSION_HEADER, served);
    }

    private HttpServer.Response keysOnly(HttpServer.Request request, FilterChain chain)
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
        if (limits != null && a instanceof ApiKeyAuthenticationToken) {
            ApiKey key = ((ApiKeyAuthenticationToken) a).getApiKey();
            if (key.getOwner().startsWith(StoreApiKeys.APP_OWNER_PREFIX)) {
                int level = limits.take(key.getId());
                String header = (level < 0 ? limits.capacity() : level) + "/" + limits.capacity();
                if (level < 0) {
                    return HttpServer.Response.json(429,
                            "{\"error\":\"The app's call limit is used up; retry later\"}")
                            .header("Retry-After", String.valueOf(-level))
                            .header(AppCallLimits.HEADER, header);
                }
                HttpServer.Response answer = chain.doFilter(request);
                return answer == null ? null : answer.header(AppCallLimits.HEADER, header);
            }
        }
        return chain.doFilter(request);
    }
}
