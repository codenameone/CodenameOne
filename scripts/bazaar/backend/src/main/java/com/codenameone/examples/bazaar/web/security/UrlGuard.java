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

import java.net.InetAddress;
import java.net.UnknownHostException;

/// Refuses a URL the server should never call on a merchant's behalf: one
/// that names this machine, the private network or a cloud metadata address
/// (server-side request forgery). Webhook URLs are checked when saved, by
/// name and literal address, and again before each delivery, by every
/// address the name resolves to -- a name that resolved to a public address
/// at save time can point inward later.
public final class UrlGuard {
    /// Resolves a host name; tests replace it so they need no DNS.
    public interface Resolver {
        byte[][] resolve(String host) throws UnknownHostException;
    }

    public static final Resolver DNS = new Resolver() {
        @Override
        public byte[][] resolve(String host) throws UnknownHostException {
            InetAddress[] all = InetAddress.getAllByName(host);
            byte[][] out = new byte[all.length][];
            for (int iter = 0; iter < all.length; iter++) {
                out[iter] = all[iter].getAddress();
            }
            return out;
        }
    };

    private UrlGuard() {
    }

    /// The host of an http(s) URL, lower case, without brackets or port; null
    /// when the URL is not one.
    public static String host(String url) {
        if (url == null) {
            return null;
        }
        int start;
        if (url.regionMatches(true, 0, "https://", 0, 8)) {
            start = 8;
        } else if (url.regionMatches(true, 0, "http://", 0, 7)) {
            start = 7;
        } else {
            return null;
        }
        int end = start;
        while (end < url.length() && "/?#".indexOf(url.charAt(end)) < 0) {
            end++;
        }
        String authority = url.substring(start, end);
        if (authority.indexOf('@') >= 0 || authority.indexOf('\\') >= 0) {
            return null;
        }
        String host;
        if (authority.startsWith("[")) {
            int close = authority.indexOf(']');
            if (close < 0) {
                return null;
            }
            host = authority.substring(1, close);
        } else {
            int colon = authority.indexOf(':');
            host = colon < 0 ? authority : authority.substring(0, colon);
        }
        while (host.endsWith(".")) {
            host = host.substring(0, host.length() - 1);
        }
        return host.length() == 0 ? null : Ascii.lower(host);
    }

    /// The save-time check: why the URL is refused, or null when it may be kept.
    public static String refuseByName(String url) {
        String host = host(url);
        if (host == null) {
            return "The URL is an http or https address";
        }
        if (host.equals("localhost") || host.endsWith(".localhost") || host.endsWith(".local")
                || host.endsWith(".internal") || host.endsWith(".lan") || host.indexOf('.') < 0
                    && host.indexOf(':') < 0) {
            return "The URL must name a public host";
        }
        byte[] literal = literal(host);
        if (literal != null && !isPublic(literal)) {
            return "The URL must name a public address";
        }
        if (literal == null && looksNumeric(host)) {
            // 0x7f.1, 2130706433 and other spellings an HTTP client may accept.
            return "The URL must name a public address";
        }
        return null;
    }

    /// The delivery-time check: every address the host resolves to is public.
    public static String refuse(String url, Resolver resolver) {
        String byName = refuseByName(url);
        if (byName != null) {
            return byName;
        }
        String host = host(url);
        if (literal(host) != null) {
            return null;
        }
        byte[][] addresses;
        try {
            addresses = resolver.resolve(host);
        } catch (UnknownHostException none) {
            return "The host does not resolve";
        }
        if (addresses == null || addresses.length == 0) {
            return "The host does not resolve";
        }
        for (int iter = 0; iter < addresses.length; iter++) {
            if (!isPublic(addresses[iter])) {
                return "The host resolves to a private address";
            }
        }
        return null;
    }

    private static boolean looksNumeric(String host) {
        for (int iter = 0; iter < host.length(); iter++) {
            char ch = host.charAt(iter);
            if (!(ch >= '0' && ch <= '9' || ch == '.' || ch == 'x' || ch >= 'a' && ch <= 'f')) {
                return false;
            }
        }
        // Every label hexadecimal or decimal: a name like "cafe.bad" stays a name
        // only when some label is not a number.
        String[] labels = split(host, '.');
        for (int iter = 0; iter < labels.length; iter++) {
            String l = labels[iter];
            boolean num = l.length() > 0;
            boolean hex = l.startsWith("0x");
            for (int c = hex ? 2 : 0; c < l.length(); c++) {
                char ch = l.charAt(c);
                if (!(ch >= '0' && ch <= '9' || hex && ch >= 'a' && ch <= 'f')) {
                    num = false;
                }
            }
            if (!num) {
                return false;
            }
        }
        return true;
    }

    /// A dotted IPv4 or an IPv6 literal as bytes, or null for a name.
    static byte[] literal(String host) {
        if (host.indexOf(':') >= 0) {
            return ipv6(host);
        }
        String[] parts = split(host, '.');
        if (parts.length != 4) {
            return null;
        }
        byte[] out = new byte[4];
        for (int iter = 0; iter < 4; iter++) {
            String p = parts[iter];
            if (p.length() == 0 || p.length() > 3) {
                return null;
            }
            int v = 0;
            for (int c = 0; c < p.length(); c++) {
                char ch = p.charAt(c);
                if (ch < '0' || ch > '9') {
                    return null;
                }
                v = v * 10 + (ch - '0');
            }
            if (v > 255) {
                return null;
            }
            out[iter] = (byte) v;
        }
        return out;
    }

    private static byte[] ipv6(String host) {
        int zone = host.indexOf('%');
        String h = zone < 0 ? host : host.substring(0, zone);
        if (h.indexOf('.') >= 0) {
            // ::ffff:a.b.c.d -- judge the IPv4 part.
            byte[] v4 = literal(h.substring(h.lastIndexOf(':') + 1));
            if (v4 == null) {
                return new byte[16];
            }
            byte[] out = new byte[16];
            out[10] = (byte) 0xff;
            out[11] = (byte) 0xff;
            System.arraycopy(v4, 0, out, 12, 4);
            return out;
        }
        int gap = h.indexOf("::");
        String[] head = gap < 0 ? split(h, ':') : split(h.substring(0, gap), ':');
        String[] tail = gap < 0 ? new String[0] : split(h.substring(gap + 2), ':');
        int[] words = new int[8];
        int n = 0;
        for (int iter = 0; iter < head.length && n < 8; iter++) {
            if (head[iter].length() > 0) {
                words[n++] = hex(head[iter]);
            }
        }
        int t = 0;
        for (int iter = 0; iter < tail.length; iter++) {
            if (tail[iter].length() > 0) {
                t++;
            }
        }
        int at = 8 - t;
        for (int iter = 0; iter < tail.length && at < 8; iter++) {
            if (tail[iter].length() > 0) {
                words[at++] = hex(tail[iter]);
            }
        }
        byte[] out = new byte[16];
        for (int iter = 0; iter < 8; iter++) {
            if (words[iter] < 0) {
                return new byte[16];
            }
            out[iter * 2] = (byte) (words[iter] >> 8);
            out[iter * 2 + 1] = (byte) words[iter];
        }
        return out;
    }

    private static int hex(String s) {
        if (s.length() > 4) {
            return -1;
        }
        int v = 0;
        for (int iter = 0; iter < s.length(); iter++) {
            int d = Character.digit(s.charAt(iter), 16);
            if (d < 0) {
                return -1;
            }
            v = v * 16 + d;
        }
        return v;
    }

    /// Whether an address is routable on the public internet.
    public static boolean isPublic(byte[] a) {
        if (a.length == 16) {
            boolean mapped = true;
            for (int iter = 0; iter < 10; iter++) {
                mapped &= a[iter] == 0;
            }
            if (mapped && (a[10] & 0xff) == 0xff && (a[11] & 0xff) == 0xff) {
                return isPublic(new byte[] {a[12], a[13], a[14], a[15]});
            }
            int first = a[0] & 0xff;
            boolean zero = true;
            for (int iter = 0; iter < 15; iter++) {
                zero &= a[iter] == 0;
            }
            if (zero) {
                return false; // :: and ::1
            }
            if ((first & 0xfe) == 0xfc) {
                return false; // unique local fc00::/7
            }
            if (first == 0xfe && (a[1] & 0xc0) == 0x80) {
                return false; // link local fe80::/10
            }
            return first != 0xff; // multicast
        }
        if (a.length != 4) {
            return false;
        }
        int b0 = a[0] & 0xff;
        int b1 = a[1] & 0xff;
        return !(b0 == 0 || b0 == 10 || b0 == 127 || b0 >= 224
                || b0 == 169 && b1 == 254
                || b0 == 172 && b1 >= 16 && b1 <= 31
                || b0 == 192 && b1 == 168
                || b0 == 100 && b1 >= 64 && b1 <= 127
                || b0 == 192 && b1 == 0 && (a[2] & 0xff) == 0
                || b0 == 198 && (b1 == 18 || b1 == 19));
    }

    /// String.split is a regex and not in the device's class library.
    private static String[] split(String s, char sep) {
        java.util.List<String> out = new java.util.ArrayList<String>();
        int from = 0;
        for (int iter = 0; iter <= s.length(); iter++) {
            if (iter == s.length() || s.charAt(iter) == sep) {
                out.add(s.substring(from, iter));
                from = iter + 1;
            }
        }
        return out.toArray(new String[out.size()]);
    }
}
