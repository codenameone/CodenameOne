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
import com.codenameone.examples.bazaar.web.security.PageEtags;

/// Caching and canonical headers for the storefront's compiled pages.
///
/// A view's response is built by generated code, so the headers a page needs
/// are added here, after the controller ran:
///
/// - `Link: <...>; rel="canonical"`. A compiled view only takes static link
///   element addresses, and search engines read the header as they read the
///   element.
/// - Public pages -- the same bytes for everyone, since none of them reads the
///   session -- are marked so, and [PageEtags] gives them `Cache-Control:
///   public` for a minute (a cache may serve a stale copy for ten more while it
///   refetches) and an `ETag` it answers with 304. They carry no CSP nonce: the
///   shop chain allows their inline style by hash.
/// - The cart, the checkout and an order's pages are one visitor's:
///   `private, no-store`, whatever the method -- a checkout page drawn
///   again for a post holds the address typed into it.
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
        if (response == null || !storefront(request.getTarget())) {
            return;
        }
        if (isPrivate(request.getTarget()) && !isCart(request.getTarget())) {
            noStore(response);
            return;
        }
        if (response.getStatus() != 200 || !"GET".equals(request.getMethod())) {
            return;
        }
        String type = response.getContentType();
        if (type == null || !type.startsWith("text/html")) {
            return;
        }
        response.header("Link", "<" + urls.absolute(request, canonicalPath(request.getTarget()))
                + ">; rel=\"canonical\"");
        if (isCart(request.getTarget())) {
            noStore(response);
            return;
        }
        // The page filter (web.security.PageEtags) writes the header and an
        // ETag; this marks the page as one a shared cache may keep. It carries
        // no nonce (the shop chain allows its inline style by hash) and reads
        // no session, so every visitor gets the same bytes.
        com.codename1.backend.security.SecurityExchange current =
                com.codename1.backend.security.SecurityExchange.current();
        if (current != null && current.getAttribute(
                com.codenameone.examples.bazaar.web.slots.Slots.PERSONAL) != null) {
            // The visitor's consent put snippets on this copy that the shared
            // one leaves out: theirs alone (PageEtags keeps it private).
            response.header("Cache-Control", "private, no-cache");
            return;
        }
        response.header("Cache-Control", PageEtags.PUBLIC_POLICY);
        if (current != null) {
            current.setAttribute(PageEtags.PUBLIC_CACHE, "1");
        }
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

    private static void noStore(HttpServer.Response response) {
        response.header("Cache-Control", "private, no-store");
        com.codename1.backend.security.SecurityExchange exchange =
                com.codename1.backend.security.SecurityExchange.current();
        if (exchange != null) {
            exchange.setAttribute(
                    com.codenameone.examples.bazaar.web.security.PageEtags.NO_STORE, "1");
        }
    }

    /// The checkout (`/s/<handle>/checkout...`) and an order's pages
    /// (`/s/<handle>/orders/...`), besides the cart.
    static boolean isPrivate(String target) {
        int q = target.indexOf('?');
        String path = q < 0 ? target : target.substring(0, q);
        if (!path.startsWith("/s/")) {
            return false;
        }
        int slash = path.indexOf('/', 3);
        String rest = slash < 0 ? "" : path.substring(slash);
        return rest.equals("/cart") || rest.equals("/checkout") || rest.startsWith("/checkout/")
                || rest.startsWith("/orders/");
    }

    private static boolean isCart(String target) {
        int q = target.indexOf('?');
        String path = q < 0 ? target : target.substring(0, q);
        return path.startsWith("/s/") && path.endsWith("/cart");
    }
}
