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
/// sitemap, link previews and the checkout.
///
/// `bazaar.public.url` is the address this server is reached at, and the
/// authorization server's issuer when it is unset, as for the payment return
/// page. Without either -- a development run -- it is the request's own
/// `Host`, which is right on a laptop and never trusted in production,
/// where a canonical link must not be whatever a client typed.
///
/// `bazaar.app.url` is where the web build of the app is served, unset
/// `<public url>/app/`. The cart reminder mail opens the cart there
/// ([#appCart]): its token travels in the link, and the web storefront's cart
/// lives in a browser session the mail cannot reach.
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

    /// The app opened on a store's cart, `?store=<handle>&cart=<token>`.
    public String appCart(HttpServer.Request request, String handle, String token) {
        String app = appUrl.length() > 0 ? appUrl : base(request) + "/app/";
        return app + (app.indexOf('?') >= 0 ? "&" : "?") + "store=" + encode(handle)
                + (token == null || token.length() == 0 ? "" : "&cart=" + encode(token));
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

    /// Finds stores by their custom domain; null outside the container.
    @com.codename1.backend.annotations.Autowired
    private com.codenameone.examples.bazaar.repository.StoreRepository stores;

    /// The storefront paths a custom domain serves at its root
    /// ([Storefront]); every other store page keeps its `/s/{handle}` path,
    /// which answers on any host.
    private static final String[] ROOT_PATHS = {"/collections/", "/products/", "/search",
        "/cart"};

    /// `path` (absolute, starting with a slash) made absolute. A store page
    /// of a store with a custom domain is addressed on that domain, over
    /// HTTPS, so canonical links, the sitemap and structured data name it.
    public String absolute(HttpServer.Request request, String path) {
        String own = ownDomain(path);
        return own != null ? own : base(request) + path;
    }

    private String ownDomain(String path) {
        if (stores == null || path == null || !path.startsWith("/s/")) {
            return null;
        }
        int slash = path.indexOf('/', 3);
        int end = slash < 0 ? path.length() : slash;
        int q = path.indexOf('?', 3);
        if (q >= 0 && q < end) {
            end = q;
        }
        String handle = path.substring(3, end);
        if (handle.length() == 0) {
            return null;
        }
        com.codenameone.examples.bazaar.domain.Store store = stores.findByHandle(handle);
        String own = served(store);
        if (own == null) {
            return null;
        }
        String rest = path.substring(end);
        String site = "https://" + own;
        if (rest.length() == 0 || rest.charAt(0) == '?') {
            return site + "/" + rest;
        }
        for (int iter = 0; iter < ROOT_PATHS.length; iter++) {
            if (rest.startsWith(ROOT_PATHS[iter])) {
                return site + rest;
            }
        }
        return site + path;
    }

    /// The handle of the active store whose custom domain the request's
    /// `Host` names, or null for the platform's own host and any other.
    public String storeForHost(HttpServer.Request request) {
        String host = request == null ? null : request.getHeader("Host");
        if (stores == null || host == null) {
            return null;
        }
        int colon = host.lastIndexOf(':');
        if (colon > 0 && host.indexOf(']') < colon) {
            host = host.substring(0, colon);
        }
        String domain = domain(host);
        if (domain == null || domain.length() == 0) {
            return null;
        }
        com.codenameone.examples.bazaar.domain.Store store = stores.findByCustomDomain(domain);
        return served(store) == null
                || !com.codenameone.examples.bazaar.store.Stores.ACTIVE.equals(
                store.getStatus()) ? null : store.getHandle();
    }

    /// `store`'s custom domain once its ownership is proved
    /// (`web.DomainVerification`), else null. An unproved domain is neither
    /// served nor named in links, so claiming somebody else's domain does
    /// nothing.
    public static String served(com.codenameone.examples.bazaar.domain.Store store) {
        return store == null || store.getCustomDomain() == null
                || store.getDomainVerifiedAt() <= 0 ? null : store.getCustomDomain();
    }

    /// `raw` as a custom domain: trimmed, ASCII lower case, without a
    /// trailing dot; "" for empty, null when it is not a hostname (labels
    /// of letters, digits and inner dashes, at least two, 253 at most, and
    /// a top-level label that is not all digits, so no IP address).
    public static String domain(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        if (s.endsWith(".")) {
            s = s.substring(0, s.length() - 1);
        }
        if (s.length() == 0) {
            return "";
        }
        if (s.length() > 253) {
            return null;
        }
        StringBuilder b = new StringBuilder(s.length());
        int labelStart = 0;
        int labels = 0;
        boolean digitsOnly = true;
        for (int iter = 0; iter <= s.length(); iter++) {
            char c = iter == s.length() ? '.' : s.charAt(iter);
            if (c == '.') {
                int len = iter - labelStart;
                if (len == 0 || len > 63 || s.charAt(labelStart) == '-'
                        || s.charAt(iter - 1) == '-') {
                    return null;
                }
                labels++;
                if (iter < s.length()) {
                    digitsOnly = true;
                    labelStart = iter + 1;
                    b.append('.');
                }
                continue;
            }
            if (c >= 'A' && c <= 'Z') {
                c = (char) (c + ('a' - 'A'));
            }
            boolean digit = c >= '0' && c <= '9';
            if (!(c >= 'a' && c <= 'z' || digit || c == '-')) {
                return null;
            }
            digitsOnly &= digit;
            b.append(c);
        }
        return labels < 2 || digitsOnly ? null : b.toString();
    }

    /// A store's HTML checkout ([CheckoutController]), relative: the session
    /// cart it places is this server's, so it never leaves this origin.
    public static String checkout(String handle) {
        return "/s/" + encode(handle) + "/checkout";
    }

    /// A store's web cart with the recovery token that restores `token`'s
    /// cart into the session ([StorefrontPages#cart]), relative.
    public static String recoverCart(String handle, String token) {
        return "/s/" + encode(handle) + "/cart?r=" + encode(token);
    }

    /// The confirmation page of an order, with the link token that opens it
    /// to whoever placed it without an account.
    public static String order(String handle, int number, String token) {
        return "/s/" + encode(handle) + "/orders/" + number + "?t=" + encode(token);
    }

    /// Percent-encodes everything but unreserved ASCII.
    public static String encode(String value) {
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
