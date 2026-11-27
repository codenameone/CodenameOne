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
package com.codenameone.examples.bazaar.web.slots;

import com.codename1.backend.HttpServer;

/// The visitor's cookie-consent choice, kept in the `bz_consent` cookie.
///
/// The value is the accepted optional categories joined by dots
/// (`analytics.marketing`), or `none`. "Necessary" is never in it: it is not a
/// choice. No cookie at all means the visitor has not answered, which is the
/// only state the banner shows in.
public final class Consent {
    /// The cookie's name.
    public static final String COOKIE = "bz_consent";
    public static final String NECESSARY = "necessary";
    public static final String ANALYTICS = "analytics";
    public static final String MARKETING = "marketing";
    /// Half a year, as the banner says.
    static final int MAX_AGE = 15552000;

    /// Whether the visitor answered at all.
    public final boolean answered;
    public final boolean analytics;
    public final boolean marketing;

    public Consent(boolean answered, boolean analytics, boolean marketing) {
        this.answered = answered;
        this.analytics = analytics;
        this.marketing = marketing;
    }

    /// Whether a snippet of `category` may load for this visitor.
    public boolean allows(String category) {
        if (NECESSARY.equals(category)) {
            return true;
        }
        if (ANALYTICS.equals(category)) {
            return analytics;
        }
        return MARKETING.equals(category) && marketing;
    }

    /// Whether `category` is one of the three.
    public static boolean isCategory(String category) {
        return NECESSARY.equals(category) || ANALYTICS.equals(category)
                || MARKETING.equals(category);
    }

    /// The cookie value for this choice.
    public String value() {
        if (analytics && marketing) {
            return ANALYTICS + "." + MARKETING;
        }
        if (analytics) {
            return ANALYTICS;
        }
        return marketing ? MARKETING : "none";
    }

    /// The choice the request's cookie records.
    public static Consent of(HttpServer.Request request) {
        String raw = cookie(request.getHeader("Cookie"), COOKIE);
        if (raw == null) {
            return new Consent(false, false, false);
        }
        return parse(raw);
    }

    /// A cookie value read back; unknown tokens are ignored.
    static Consent parse(String raw) {
        boolean a = false;
        boolean m = false;
        int start = 0;
        while (start <= raw.length()) {
            int dot = raw.indexOf('.', start);
            int end = dot < 0 ? raw.length() : dot;
            String token = raw.substring(start, end);
            if (ANALYTICS.equals(token)) {
                a = true;
            } else if (MARKETING.equals(token)) {
                m = true;
            }
            start = end + 1;
        }
        return new Consent(true, a, m);
    }

    /// The value of cookie `name` in a `Cookie` header, or null.
    static String cookie(String header, String name) {
        if (header == null) {
            return null;
        }
        int start = 0;
        while (start < header.length()) {
            int semi = header.indexOf(';', start);
            int end = semi < 0 ? header.length() : semi;
            String pair = header.substring(start, end).trim();
            int eq = pair.indexOf('=');
            if (eq > 0 && pair.substring(0, eq).trim().equals(name)) {
                return pair.substring(eq + 1).trim();
            }
            start = end + 1;
        }
        return null;
    }

    /// The `Set-Cookie` value that records this choice. `HttpOnly`, because no
    /// script reads it: the server decides what a page loads.
    public String setCookie(boolean secure) {
        return COOKIE + "=" + value() + "; Path=/; Max-Age=" + MAX_AGE
                + "; SameSite=Lax; HttpOnly" + (secure ? "; Secure" : "");
    }
}
