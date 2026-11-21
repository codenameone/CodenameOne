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

import java.security.MessageDigest;

/// Validators for the web app's HTML: an `ETag` over the page, and a 304 when
/// the browser already has it.
///
/// A page is per request in exactly one place -- the CSP nonce printed on its
/// script and style elements -- so the hash is taken with the nonce cut out.
/// Two renders of the same content therefore share a tag, and a 304 tells the
/// browser to reuse a copy that still carries its old nonce, which is the
/// nonce of the policy it was served with. The CSRF token is per session and
/// stays in the hash, so another visitor's copy never validates.
///
/// Signed-in pages are `private, no-cache`: a shared cache must not hand one
/// visitor's copy -- nonce, token, cart -- to another, and revalidating costs a
/// 304. Public storefront pages carry no nonce ([WebHeaders] in public mode)
/// and are `public, max-age=60, stale-while-revalidate=600`, with the same tag.
public final class PageEtags implements SecurityFilter {
    /// An exchange attribute: this page is one visitor's and is never stored.
    public static final String NO_STORE = "bazaar.noStore";
    /// An exchange attribute: this page is the same for every visitor and a
    /// shared cache may keep it (set by the storefront's caching interceptor).
    public static final String PUBLIC_CACHE = "bazaar.publicCache";
    /// What a public page is cached for: a minute fresh, ten more served stale
    /// while a cache refetches.
    /// An exchange attribute: text mixed into the page's tag besides its bytes
    /// -- the store's app slot version, which also changes the policy header,
    /// so a copy is never revalidated against a policy it was not served with.
    public static final String SALT = "bazaar.etagSalt";
    public static final String PUBLIC_POLICY = "public, max-age=60, stale-while-revalidate=600";

    @Override
    public HttpServer.Response doFilter(HttpServer.Request request, FilterChain chain)
            throws Exception {
        HttpServer.Response response = chain.doFilter(request);
        if (response == null || response.getStatus() != 200
                || !("GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod()))) {
            return response;
        }
        String type = response.getContentType();
        if (type == null || !type.startsWith("text/html")) {
            return response;
        }
        byte[] body;
        try {
            body = response.getBody();
        } catch (IllegalStateException streamed) {
            return response;
        }
        SecurityExchange exchange = SecurityExchange.current();
        if (exchange.getAttribute(NO_STORE) != null) {
            exchange.setResponseHeader("Cache-Control", "private, no-store");
            return response;
        }
        Object nonce = exchange.getAttribute(WebHeaders.NONCE);
        Object salt = exchange.getAttribute(SALT);
        String tag = "W/\"" + hash(body, nonce == null ? null : (String) nonce,
                salt == null ? "" : salt.toString()) + "\"";
        // A signed-in page carries a per-response CSP nonce and a session's
        // CSRF token, so no shared cache may keep one. A public page has
        // neither (its policy allows its inline style by hash), and only a
        // page the storefront marked as the same for everyone is public.
        boolean shared = exchange.getAttribute(WebHeaders.PUBLIC) != null
                && exchange.getAttribute(PUBLIC_CACHE) != null;
        exchange.setResponseHeader("Cache-Control", shared ? PUBLIC_POLICY : "private, no-cache");
        exchange.setResponseHeader("ETag", tag);
        String sent = request.getHeader("If-None-Match");
        if (sent != null && matches(sent, tag)) {
            return HttpServer.Response.empty(304, type, null);
        }
        return response;
    }

    static boolean matches(String header, String tag) {
        String bare = tag.substring(2);
        int start = 0;
        while (start < header.length()) {
            int comma = header.indexOf(',', start);
            String one = header.substring(start, comma < 0 ? header.length() : comma).trim();
            if (one.equals("*") || one.equals(tag) || one.equals(bare)) {
                return true;
            }
            if (comma < 0) {
                break;
            }
            start = comma + 1;
        }
        return false;
    }

    /// The tag of a page: its bytes with the per-response nonce and the
    /// session's CSRF token cut out, so a page that did not change keeps its
    /// tag across responses of one session.
    static String hash(byte[] body, String nonce) throws Exception {
        return hash(body, nonce, "");
    }

    /// [#hash(byte[], String)] with `salt` mixed in.
    static String hash(byte[] body, String nonce, String salt) throws Exception {
        String text = salt + new String(body, "ISO-8859-1");
        if (nonce != null && nonce.length() > 0) {
            text = text.replace(nonce, "");
        }
        String marker = "name=\"_csrf\" value=\"";
        int at = text.indexOf(marker);
        if (at >= 0) {
            int end = text.indexOf('"', at + marker.length());
            String token = text.substring(at + marker.length(), end < 0 ? text.length() : end);
            if (token.length() > 0) {
                text = text.replace(token, "");
            }
        }
        byte[] d = MessageDigest.getInstance("SHA-256").digest(text.getBytes("ISO-8859-1"));
        StringBuilder out = new StringBuilder();
        for (int iter = 0; iter < 12; iter++) {
            out.append(Character.forDigit((d[iter] >> 4) & 15, 16))
                    .append(Character.forDigit(d[iter] & 15, 16));
        }
        return out.toString();
    }
}
