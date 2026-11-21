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
import com.codename1.backend.annotations.Component;
import com.codename1.backend.web.HandlerInterceptor;
import com.codename1.backend.web.HandlerMethod;

/// Caching and canonical headers for the storefront's compiled pages.
///
/// A view's response is built by generated code, so the headers a page needs
/// are added here, after the controller ran:
///
/// - `Link: <...>; rel="canonical"`. A compiled view only takes static link
///   element addresses, and search engines read the header as they read the
///   element.
/// - Public pages -- the same bytes for everyone, since none of them reads the
///   session -- get `Cache-Control: public` for a minute, and a cache may serve
///   a stale copy for ten more while it refetches. They carry no `ETag`: an
///   interceptor cannot turn the view's 200 into a 304 (the response is
///   immutable here and `ResponseStatusException` takes error statuses only),
///   and a validator the server never honours would only make every
///   revalidation a full download that looks like a cache bug. The sitemap and
///   the photos, which are answered by code that holds the bytes, do answer
///   conditional requests (see [Conditional]).
/// - The cart is one visitor's: `private, no-store`.
///
/// Requests outside the storefront pass untouched.
@Component
public class StorefrontCaching implements HandlerInterceptor {
    private final SiteUrls urls;

    public StorefrontCaching(SiteUrls urls) {
        this.urls = urls;
    }

    @Override
    public void postHandle(HttpServer.Request request, HandlerMethod handler,
            HttpServer.Response response) throws Exception {
        if (response == null || response.getStatus() != 200
                || !"GET".equals(request.getMethod()) || !storefront(request.getTarget())) {
            return;
        }
        String type = response.getContentType();
        if (type == null || !type.startsWith("text/html")) {
            return;
        }
        response.header("Link", "<" + urls.absolute(request, canonicalPath(request.getTarget()))
                + ">; rel=\"canonical\"");
        if (isCart(request.getTarget())) {
            response.header("Cache-Control", "private, no-store");
            com.codename1.backend.security.SecurityExchange exchange =
                    com.codename1.backend.security.SecurityExchange.current();
            if (exchange != null) {
                exchange.setAttribute(
                        com.codenameone.examples.bazaar.web.security.PageEtags.NO_STORE, "1");
            }
            return;
        }
        // The page security filter (web.security.PageEtags) replaces this with
        // "private, no-cache" and an ETag: every page now carries a per-response
        // CSP nonce, which a shared cache must not hand to a second visitor.
        response.header("Cache-Control", "public, max-age=60, stale-while-revalidate=600");
    }

    /// The page's address without the query, except a (zero-based) page number past the
    /// first: sort orders and variant selections are the same document. The
    /// interceptor cannot read the controller's model (a view's model is not
    /// exposed after rendering), so this is derived from the path alone and
    /// kept equal to the `og:url` the templates print.
    static String canonicalPath(String target) {
        int q = target.indexOf('?');
        if (q < 0) {
            return target;
        }
        String path = target.substring(0, q);
        String query = target.substring(q + 1);
        int start = 0;
        while (start <= query.length()) {
            int amp = query.indexOf('&', start);
            int end = amp < 0 ? query.length() : amp;
            String part = query.substring(start, end);
            if (part.startsWith("page=") && !"page=0".equals(part) && part.length() > 5) {
                return path + "?" + part;
            }
            start = end + 1;
        }
        return path;
    }

    static boolean storefront(String target) {
        return target != null && (target.equals("/") || target.startsWith("/?")
                || target.startsWith("/s/") || target.startsWith("/track/"));
    }

    private static boolean isCart(String target) {
        int q = target.indexOf('?');
        String path = q < 0 ? target : target.substring(0, q);
        return path.startsWith("/s/") && path.endsWith("/cart");
    }
}
