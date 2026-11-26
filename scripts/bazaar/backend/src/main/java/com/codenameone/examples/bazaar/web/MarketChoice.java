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

import com.codename1.backend.HttpServer;
import com.codename1.backend.annotations.Controller;
import com.codename1.backend.annotations.PostMapping;
import com.codename1.backend.annotations.Value;

/// The shopper's market picker and the country banner: a form posts `market`
/// (a code, or empty for the store's own prices) and `back`, the page to
/// return to. The choice is the `market` cookie, which prices every page the
/// shopper visits afterwards, the checkout included ([StorefrontMarkets]).
///
/// A plain form post, so it works without script, refused from another site
/// like the language picker ([Storefront#sameSite]). `?market=` addresses
/// stay what they were -- each market's shared, crawlable copy -- and never
/// set a cookie, so a cached page carries no `Set-Cookie`.
@Controller
public class MarketChoice {
    private final boolean secureCookies;

    public MarketChoice(@Value("${bazaar.web.hsts:true}") boolean secureCookies) {
        this.secureCookies = secureCookies;
    }

    @PostMapping("/market")
    public HttpServer.Response choose(HttpServer.Request request) {
        Storefront.sameSite(request);
        String code = StorefrontMarkets.code(param(request, StorefrontMarkets.COOKIE));
        String cookie = code != null
                ? StorefrontMarkets.COOKIE + "=" + code + "; Path=/; Max-Age=" + LanguageChoice.MAX_AGE
                : StorefrontMarkets.COOKIE + "=; Path=/; Max-Age=0";
        return HttpServer.Response.text(303, "")
                .header("Location", back(param(request, "back")))
                .header("Set-Cookie", cookie + "; SameSite=Lax; HttpOnly"
                        + (secureCookies ? "; Secure" : ""))
                .header("Cache-Control", "no-store");
    }

    /// `back` when it is a path on this site, without a `market=` (which would
    /// outrank the new cookie); `/` for anything else, never an open redirect.
    static String back(String back) {
        if (back == null || back.length() > 500) {
            return "/";
        }
        int q = back.indexOf('?');
        String rawPath = q < 0 ? back : back.substring(0, q);
        String path = StoreText.back(rawPath);
        if (q < 0 || !path.equals(rawPath)) {
            return path;
        }
        StringBuilder kept = new StringBuilder(path);
        String query = back.substring(q + 1);
        for (int iter = 0; iter < query.length(); iter++) {
            char c = query.charAt(iter);
            if (c <= ' ' || c >= 127 || c == '\\') {
                return path;
            }
        }
        char sep = '?';
        int start = 0;
        while (start <= query.length()) {
            int amp = query.indexOf('&', start);
            int end = amp < 0 ? query.length() : amp;
            String part = query.substring(start, end);
            if (part.length() > 0 && !part.startsWith("market=")) {
                kept.append(sep).append(part);
                sep = '&';
            }
            start = end + 1;
        }
        return kept.toString();
    }

    private static String param(HttpServer.Request request, String name) {
        try {
            return request.param(name);
        } catch (RuntimeException malformed) {
            return null;
        }
    }
}
