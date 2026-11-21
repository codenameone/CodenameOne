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

import java.security.SecureRandom;

/// The security headers of every page the web app serves, and the per-request
/// nonce its content security policy is built around.
///
/// A nonce is made before the request reaches the controller and kept in the
/// exchange, where the page model reads it ([com.codenameone.examples.bazaar.web.Shell]):
/// the compiled views print it on the one inline stylesheet and on the script
/// elements they carry. The policy is written after the controller ran, so
/// that what the page added -- the hosts an app slot may load from
/// ([CspSources]) -- is in it.
///
/// The policy is strict: no script without the nonce or from another host
/// nobody allowed, no `eval`, no plugins, no `<base>`, forms post only here,
/// and no other site may frame a page. The one route that is framed -- an app
/// slot's sandboxed widget -- writes its own policy, which is left alone.
public final class WebHeaders implements SecurityFilter {
    /// The exchange attribute the nonce is kept under.
    public static final String NONCE = "bazaar.cspNonce";
    /// The exchange attribute a page's extra sources are kept under.
    public static final String SOURCES = "bazaar.cspSources";
    /// Set by a response that wrote its own policy (the widget sandbox).
    public static final String OWN_POLICY = "X-Bazaar-Own-Policy";

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] B64 =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_".toCharArray();

    /// Set on the exchange of a public page: no nonce, the policy allows the
    /// public templates' inline elements by hash ([InlineHashes]).
    public static final String PUBLIC = "bazaar.cspPublic";

    private final boolean hsts;
    private final boolean publicPages;

    /// @param hsts whether to send Strict-Transport-Security
    public WebHeaders(boolean hsts) {
        this(hsts, false);
    }

    /// @param hsts whether to send Strict-Transport-Security
    /// @param publicPages whether the pages are public -- one copy for every
    ///        visitor, which a shared cache may keep -- and so carry no nonce
    public WebHeaders(boolean hsts, boolean publicPages) {
        this.hsts = hsts;
        this.publicPages = publicPages;
    }

    /// The nonce of the request being served, or a fresh one outside a chain
    /// (a page rendered by a test without the filter still gets a valid one).
    public static String currentNonce() {
        SecurityExchange exchange = SecurityExchange.current();
        if (exchange != null && exchange.getAttribute(PUBLIC) != null) {
            // A public page is cached and shared: a nonce in it would be one
            // visitor's, and the policy allows its inline style by hash.
            return "";
        }
        Object n = exchange == null ? null : exchange.getAttribute(NONCE);
        return n == null ? newNonce() : (String) n;
    }

    /// Adds `sources` to the policy of the request being served.
    public static void allow(CspSources sources) {
        SecurityExchange exchange = SecurityExchange.current();
        if (exchange == null || sources == null) {
            return;
        }
        CspSources had = (CspSources) exchange.getAttribute(SOURCES);
        if (had == null) {
            had = new CspSources();
            exchange.setAttribute(SOURCES, had);
        }
        had.addAll(sources);
    }

    @Override
    public HttpServer.Response doFilter(HttpServer.Request request, FilterChain chain)
            throws Exception {
        SecurityExchange exchange = SecurityExchange.current();
        String nonce = null;
        if (publicPages) {
            exchange.setAttribute(PUBLIC, "1");
        } else {
            nonce = newNonce();
            exchange.setAttribute(NONCE, nonce);
        }
        HttpServer.Response response = chain.doFilter(request);
        exchange.setResponseHeader("X-Content-Type-Options", "nosniff");
        exchange.setResponseHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        exchange.setResponseHeader("Permissions-Policy",
                "camera=(), microphone=(), geolocation=(), usb=(), payment=(self)");
        if (hsts) {
            exchange.setResponseHeader("Strict-Transport-Security",
                    "max-age=31536000; includeSubDomains");
        }
        if (exchange.getAttribute(OWN_POLICY) != null) {
            return response;
        }
        exchange.setResponseHeader("X-Frame-Options", "DENY");
        exchange.setResponseHeader("Cross-Origin-Opener-Policy", "same-origin");
        exchange.setResponseHeader("Content-Security-Policy",
                policy(nonce, (CspSources) exchange.getAttribute(SOURCES)));
        return response;
    }

    /// The policy for a page with `nonce` and the slot sources `extra` (may be
    /// null). A null `nonce` is a public page's policy: no inline script at
    /// all, and inline style only as the public templates have it
    /// ([InlineHashes]).
    public static String policy(String nonce, CspSources extra) {
        StringBuilder b = new StringBuilder();
        b.append("default-src 'self'; script-src 'self'");
        if (nonce != null) {
            b.append(" 'nonce-").append(nonce).append('\'');
        }
        CspSources.append(extra, b, CspSources.SCRIPT);
        b.append("; style-src 'self'");
        if (nonce != null) {
            b.append(" 'nonce-").append(nonce).append('\'');
        } else {
            for (int iter = 0; iter < InlineHashes.PUBLIC_STYLES.length; iter++) {
                b.append(' ').append(InlineHashes.PUBLIC_STYLES[iter]);
            }
        }
        b.append("; img-src 'self' data:");
        CspSources.append(extra, b, CspSources.IMG);
        b.append("; connect-src 'self'");
        CspSources.append(extra, b, CspSources.CONNECT);
        b.append("; frame-src 'self'");
        CspSources.append(extra, b, CspSources.FRAME);
        b.append("; object-src 'none'; base-uri 'none'; form-action 'self'; "
                + "frame-ancestors 'none'");
        return b.toString();
    }

    static String newNonce() {
        byte[] bytes = new byte[18];
        RANDOM.nextBytes(bytes);
        StringBuilder out = new StringBuilder(24);
        for (int iter = 0; iter < bytes.length; iter += 3) {
            int v = ((bytes[iter] & 0xff) << 16) | ((bytes[iter + 1] & 0xff) << 8)
                    | (bytes[iter + 2] & 0xff);
            out.append(B64[(v >> 18) & 63]).append(B64[(v >> 12) & 63])
                    .append(B64[(v >> 6) & 63]).append(B64[v & 63]);
        }
        return out.toString();
    }
}
