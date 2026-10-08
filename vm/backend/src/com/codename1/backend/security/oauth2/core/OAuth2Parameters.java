/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.backend.security.oauth2.core;

import com.codename1.backend.Base64Url;
import com.codename1.backend.Crypto;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// The small pieces of text an OAuth2 exchange is made of: a form, a query, a
/// list of scopes, a random value, a PKCE challenge. Written here once because
/// both sides of the exchange -- the client that signs in elsewhere and the
/// server that issues tokens -- read and write the same ones.
public final class OAuth2Parameters {
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private OAuth2Parameters() {
    }

    /// `value` as `application/x-www-form-urlencoded` writes it: every byte of
    /// its UTF-8 that is not a letter, a digit or one of `-._~` as `%XX`.
    public static String encode(String value) {
        byte[] bytes = utf8(value);
        StringBuilder sb = new StringBuilder(bytes.length + 16);
        for (byte one : bytes) {
            int b = one & 0xff;
            if ((b >= 'a' && b <= 'z') || (b >= 'A' && b <= 'Z') || (b >= '0' && b <= '9')
                    || b == '-' || b == '.' || b == '_' || b == '~') {
                sb.append((char) b);
            } else {
                sb.append('%').append(HEX[b >> 4]).append(HEX[b & 15]);
            }
        }
        return sb.toString();
    }

    /// The reverse of [#encode], with `+` read as a space; null when `value` is
    /// not a well-formed encoding.
    public static String decode(String value) {
        byte[] out = new byte[value.length()];
        int size = 0;
        for (int iter = 0 ; iter < value.length() ; iter++) {
            char c = value.charAt(iter);
            if (c == '+') {
                out[size++] = ' ';
            } else if (c == '%') {
                if (iter + 2 >= value.length()) {
                    return null;
                }
                int high = hex(value.charAt(iter + 1));
                int low = hex(value.charAt(iter + 2));
                if (high < 0 || low < 0) {
                    return null;
                }
                out[size++] = (byte) ((high << 4) | low);
                iter += 2;
            } else if (c > 0x7f) {
                return null;
            } else {
                out[size++] = (byte) c;
            }
        }
        try {
            return new String(out, 0, size, "UTF-8");
        } catch (java.io.UnsupportedEncodingException err) {
            throw new IllegalStateException("UTF-8 is required", err);
        }
    }

    private static int hex(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        return -1;
    }

    /// The fields of a form or a query, in order. A field that appears twice
    /// keeps its first value; one that does not decode is left out.
    public static Map<String, String> parse(String form) {
        Map<String, String> out = new LinkedHashMap<String, String>();
        if (form == null) {
            return out;
        }
        int start = 0;
        while (start <= form.length()) {
            int amp = form.indexOf('&', start);
            int end = amp < 0 ? form.length() : amp;
            if (end > start) {
                int eq = form.indexOf('=', start);
                String name = decode(form.substring(start, eq < 0 || eq > end ? end : eq));
                String value = eq < 0 || eq > end ? "" : decode(form.substring(eq + 1, end));
                if (name != null && value != null && !out.containsKey(name)) {
                    out.put(name, value);
                }
            }
            if (amp < 0) {
                break;
            }
            start = amp + 1;
        }
        return out;
    }

    /// Every value the field `name` has in a form or a query, in order: for a
    /// parameter that may be sent more than once, as `resource` may. A value
    /// that does not decode is left out.
    public static List<String> values(String form, String name) {
        List<String> out = new ArrayList<String>();
        if (form == null || name == null) {
            return out;
        }
        int start = 0;
        while (start <= form.length()) {
            int amp = form.indexOf('&', start);
            int end = amp < 0 ? form.length() : amp;
            if (end > start) {
                int eq = form.indexOf('=', start);
                String field = decode(form.substring(start, eq < 0 || eq > end ? end : eq));
                if (name.equals(field)) {
                    String value = eq < 0 || eq > end ? "" : decode(form.substring(eq + 1, end));
                    if (value != null) {
                        out.add(value);
                    }
                }
            }
            if (amp < 0) {
                break;
            }
            start = amp + 1;
        }
        return out;
    }

    /// `fields` as a form body or a query, without a leading `?`. A null value
    /// is left out.
    public static String format(Map<String, ?> fields) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, ?> field : fields.entrySet()) {
            if (field.getValue() == null) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(encode(field.getKey())).append('=')
                    .append(encode(String.valueOf(field.getValue())));
        }
        return sb.toString();
    }

    /// `uri` with `fields` added to its query.
    public static String append(String uri, Map<String, ?> fields) {
        String query = format(fields);
        if (query.length() == 0) {
            return uri;
        }
        int fragment = uri.indexOf('#');
        String head = fragment < 0 ? uri : uri.substring(0, fragment);
        String tail = fragment < 0 ? "" : uri.substring(fragment);
        return head + (head.indexOf('?') < 0 ? "?" : head.endsWith("?") || head.endsWith("&")
                ? "" : "&") + query + tail;
    }

    /// The scopes of a space-separated list, in order, each once.
    public static Set<String> scopes(String list) {
        Set<String> out = new LinkedHashSet<String>();
        if (list == null) {
            return out;
        }
        int start = 0;
        while (start < list.length()) {
            int space = start;
            while (space < list.length() && !isSeparator(list.charAt(space))) {
                space++;
            }
            if (space > start) {
                out.add(list.substring(start, space));
            }
            start = space + 1;
        }
        return out;
    }

    private static boolean isSeparator(char c) {
        return c == ' ' || c == ',' || c == '\t';
    }

    /// `scopes` as one space-separated value.
    public static String scopes(Collection<String> scopes) {
        StringBuilder sb = new StringBuilder();
        for (String scope : scopes) {
            sb.append(sb.length() == 0 ? "" : " ").append(scope);
        }
        return sb.toString();
    }

    /// A value nobody can guess: `bytes` bytes from the system's generator,
    /// written in the URL-safe alphabet. 32 bytes is 256 bits.
    public static String random(int bytes) {
        try {
            return Base64Url.encode(Crypto.randomBytes(bytes));
        } catch (IOException err) {
            throw new IllegalStateException("The system has no source of random bytes: "
                    + err.getMessage(), err);
        }
    }

    /// The SHA-256 of `value` in the URL-safe alphabet: the S256 challenge of a
    /// PKCE verifier, and what a secret token is stored and looked up by.
    public static String sha256(String value) {
        return Base64Url.encode(Crypto.sha256(utf8(value)));
    }

    /// Whether two values are equal, in time that does not depend on where
    /// they differ.
    public static boolean equalsConstantTime(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return Crypto.equalsConstantTime(utf8(a), utf8(b));
    }

    /// The bytes of `value` in UTF-8.
    public static byte[] utf8(String value) {
        try {
            return value.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException err) {
            throw new IllegalStateException("UTF-8 is required", err);
        }
    }

    /// The text of `bytes`, read as UTF-8.
    public static String string(byte[] bytes) {
        try {
            return new String(bytes, "UTF-8");
        } catch (java.io.UnsupportedEncodingException err) {
            throw new IllegalStateException("UTF-8 is required", err);
        }
    }
}
