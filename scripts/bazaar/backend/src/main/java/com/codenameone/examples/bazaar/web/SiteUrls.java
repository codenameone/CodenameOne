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

import com.codename1.appkit.server.security.AccountSecurity;
import com.codename1.backend.HttpServer;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Value;

/// The absolute addresses the storefront hands out: canonical links, the
/// sitemap, link previews and the checkout hand-off to the web app.
///
/// `bazaar.public.url` is the address this server is reached at, and the
/// authorization server's issuer when it is unset, as for the payment return
/// page. Without either -- a development run -- it is the request's own
/// `Host`, which is right on a laptop and never trusted in production,
/// where a canonical link must not be whatever a client typed.
///
/// `bazaar.app.url` is where the web build of the app is served. A cart page
/// sends the shopper there to check out, with `?store=<handle>&cart=<token>`;
/// unset it is `<public url>/app/`.
@Component
public class SiteUrls {
    private final String publicUrl;
    private final String appUrl;

    public SiteUrls(@Value("${bazaar.public.url:}") String publicUrl,
            @Value("${" + AccountSecurity.ISSUER_KEY + ":}") String issuer,
            @Value("${bazaar.app.url:}") String appUrl) {
        this.publicUrl = trim(publicUrl.trim().length() > 0 ? publicUrl.trim() : issuer.trim());
        this.appUrl = appUrl.trim();
    }

    private static String trim(String url) {
        String out = url;
        while (out.endsWith("/")) {
            out = out.substring(0, out.length() - 1);
        }
        return out;
    }

    /// The scheme, host and port pages are served from, without a slash.
    public String base(HttpServer.Request request) {
        if (publicUrl.length() > 0) {
            return publicUrl;
        }
        String host = request == null ? null : request.getHeader("Host");
        if (host == null || host.length() == 0 || !plainHost(host)) {
            host = "localhost";
        }
        return (request != null && request.isSecure() ? "https://" : "http://") + host;
    }

    /// Letters, digits, dots, dashes, a colon and brackets: a host and port,
    /// never anything that could break out of an attribute or a header.
    private static boolean plainHost(String host) {
        if (host.length() > 255) {
            return false;
        }
        for (int iter = 0; iter < host.length(); iter++) {
            char c = host.charAt(iter);
            boolean ok = c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9'
                    || c == '.' || c == '-' || c == ':' || c == '[' || c == ']';
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    /// `path` (absolute, starting with a slash) made absolute.
    public String absolute(HttpServer.Request request, String path) {
        return base(request) + path;
    }

    /// The web app opened on a store's cart, where checkout happens.
    public String checkout(HttpServer.Request request, String handle, String token) {
        String app = appUrl.length() > 0 ? appUrl : base(request) + "/app/";
        return app + (app.indexOf('?') >= 0 ? "&" : "?") + "store=" + encode(handle)
                + (token == null || token.length() == 0 ? "" : "&cart=" + encode(token));
    }

    /// Percent-encodes everything but unreserved ASCII.
    static String encode(String value) {
        StringBuilder out = new StringBuilder();
        byte[] bytes;
        try {
            bytes = value.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException impossible) {
            return "";
        }
        for (int iter = 0; iter < bytes.length; iter++) {
            int b = bytes[iter] & 0xff;
            boolean plain = b >= 'a' && b <= 'z' || b >= 'A' && b <= 'Z' || b >= '0' && b <= '9'
                    || b == '-' || b == '_' || b == '.' || b == '~';
            if (plain) {
                out.append((char) b);
            } else {
                out.append('%').append("0123456789ABCDEF".charAt(b >> 4))
                        .append("0123456789ABCDEF".charAt(b & 0xf));
            }
        }
        return out.toString();
    }
}
