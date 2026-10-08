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
package android.net;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// An immutable URI reference.
public final class Uri implements Comparable<Uri>, android.os.Parcelable {

    public static final Uri EMPTY = new Uri("");

    private final String string;

    private Uri(String s) {
        string = s;
    }

    public static Uri parse(String uriString) {
        return new Uri(uriString);
    }

    public static Uri fromParts(String scheme, String ssp, String fragment) {
        if (scheme == null || ssp == null) {
            throw new NullPointerException("scheme and scheme-specific part must not be null");
        }
        return new Uri(scheme + ":" + encode(ssp) + (fragment == null ? "" : "#" + encode(fragment)));
    }

    public static Uri withAppendedPath(Uri baseUri, String pathSegment) {
        return baseUri.buildUpon().appendEncodedPath(pathSegment).build();
    }

    public static Uri fromFile(Object file) {
        // A shim directory (`getFilesDir()`) is already a `file:` path;
        // prefixing another scheme gave `file://file:///...`.
        String s = file instanceof com.codename1.compat.jdk.File
                ? ((com.codename1.compat.jdk.File) file).storagePath() : String.valueOf(file);
        // The path is percent-encoded (everything but `/`) as Android's
        // fromFile does: a raw `#` or `?` in a file name started a fragment
        // or query, so `getPath()` named another file. The authority a
        // sandboxed port's storage path carries (`file://home/`) is kept.
        String prefix;
        String path;
        if (s.startsWith("file://")) {
            int slash = s.indexOf('/', 7);
            if (slash < 0) {
                slash = s.length();
            }
            prefix = s.substring(0, slash);
            path = s.substring(slash);
        } else if (s.startsWith("file:")) {
            prefix = "file:";
            path = s.substring(5);
        } else {
            prefix = "file://";
            path = s;
        }
        return new Uri(prefix + encode(path, "/"));
    }

    public String getScheme() {
        int colon = string.indexOf(':');
        int slash = string.indexOf('/');
        int q = string.indexOf('?');
        int h = string.indexOf('#');
        if (colon <= 0 || (slash >= 0 && slash < colon) || (q >= 0 && q < colon) || (h >= 0 && h < colon)) {
            return null;
        }
        return string.substring(0, colon);
    }

    public boolean isAbsolute() {
        return getScheme() != null;
    }

    public boolean isRelative() {
        return !isAbsolute();
    }

    public boolean isHierarchical() {
        String s = getScheme();
        return s == null || string.startsWith(s + ":/");
    }

    public boolean isOpaque() {
        return !isHierarchical();
    }

    private String afterScheme() {
        String s = getScheme();
        return s == null ? string : string.substring(s.length() + 1);
    }

    public String getSchemeSpecificPart() {
        return decode(getEncodedSchemeSpecificPart());
    }

    public String getEncodedSchemeSpecificPart() {
        String a = afterScheme();
        int h = a.indexOf('#');
        return h >= 0 ? a.substring(0, h) : a;
    }

    public String getEncodedAuthority() {
        String a = afterScheme();
        if (!a.startsWith("//")) {
            return null;
        }
        int end = a.length();
        for (int i = 2; i < a.length(); i++) {
            char c = a.charAt(i);
            if (c == '/' || c == '?' || c == '#') {
                end = i;
                break;
            }
        }
        return a.substring(2, end);
    }

    public String getAuthority() {
        return decode(getEncodedAuthority());
    }

    public String getUserInfo() {
        String a = getEncodedAuthority();
        if (a == null) {
            return null;
        }
        int at = a.lastIndexOf('@');
        return at < 0 ? null : decode(a.substring(0, at));
    }

    public String getHost() {
        String a = getEncodedAuthority();
        if (a == null) {
            return null;
        }
        int at = a.lastIndexOf('@');
        String hp = at >= 0 ? a.substring(at + 1) : a;
        if (hp.startsWith("[")) {
            int close = hp.indexOf(']');
            return close > 0 ? hp.substring(0, close + 1) : hp;
        }
        int colon = hp.lastIndexOf(':');
        return decode(colon >= 0 ? hp.substring(0, colon) : hp);
    }

    public int getPort() {
        String a = getEncodedAuthority();
        if (a == null) {
            return -1;
        }
        int at = a.lastIndexOf('@');
        String hp = at >= 0 ? a.substring(at + 1) : a;
        int colon = hp.lastIndexOf(':');
        if (colon < 0 || hp.indexOf(']') > colon) {
            return -1;
        }
        try {
            return Integer.parseInt(hp.substring(colon + 1));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public String getEncodedPath() {
        String a = afterScheme();
        int start = 0;
        if (a.startsWith("//")) {
            start = a.length();
            for (int i = 2; i < a.length(); i++) {
                char c = a.charAt(i);
                if (c == '/' || c == '?' || c == '#') {
                    start = i;
                    break;
                }
            }
        } else if (getScheme() != null && !a.startsWith("/")) {
            return null;
        }
        int end = a.length();
        for (int i = start; i < a.length(); i++) {
            char c = a.charAt(i);
            if (c == '?' || c == '#') {
                end = i;
                break;
            }
        }
        return a.substring(start, end);
    }

    public String getPath() {
        String p = getEncodedPath();
        return p == null ? null : decode(p);
    }

    public List<String> getPathSegments() {
        String p = getEncodedPath();
        if (p == null) {
            return Collections.emptyList();
        }
        ArrayList<String> out = new ArrayList<String>();
        int start = 0;
        for (int i = 0; i <= p.length(); i++) {
            if (i == p.length() || p.charAt(i) == '/') {
                if (i > start) {
                    out.add(decode(p.substring(start, i)));
                }
                start = i + 1;
            }
        }
        return out;
    }

    public String getLastPathSegment() {
        List<String> s = getPathSegments();
        return s.isEmpty() ? null : s.get(s.size() - 1);
    }

    /// The query, or null. An opaque URI has none: the `?` in
    /// `mailto:a@b.c?subject=x` is part of its scheme-specific part, as on
    /// Android. Neither does a `?` inside the fragment start one.
    public String getEncodedQuery() {
        if (!isHierarchical()) {
            return null;
        }
        int q = string.indexOf('?');
        int h = string.indexOf('#');
        if (q < 0 || (h >= 0 && h < q)) {
            return null;
        }
        return h >= 0 ? string.substring(q + 1, h) : string.substring(q + 1);
    }

    public String getQuery() {
        String q = getEncodedQuery();
        return q == null ? null : decode(q);
    }

    public String getEncodedFragment() {
        int h = string.indexOf('#');
        return h < 0 ? null : string.substring(h + 1);
    }

    public String getFragment() {
        String f = getEncodedFragment();
        return f == null ? null : decode(f);
    }

    public java.util.Set<String> getQueryParameterNames() {
        if (!isHierarchical()) {
            throw new UnsupportedOperationException("This isn't a hierarchical URI");
        }
        java.util.LinkedHashSet<String> names = new java.util.LinkedHashSet<String>();
        String q = getEncodedQuery();
        if (q == null) {
            return names;
        }
        for (String part : split(q, '&')) {
            int eq = part.indexOf('=');
            names.add(decode(eq >= 0 ? part.substring(0, eq) : part));
        }
        return names;
    }

    /// The first value of `key`, decoded. As on Android (which documents it),
    /// a literal `+` here decodes to a space; [#getQueryParameters(String)]
    /// keeps it, as Android's does. Encode a literal plus as `%2B`.
    public String getQueryParameter(String key) {
        List<String> v = queryParameters(key, true);
        return v.isEmpty() ? null : v.get(0);
    }

    public List<String> getQueryParameters(String key) {
        return queryParameters(key, false);
    }

    private List<String> queryParameters(String key, boolean plusIsSpace) {
        if (!isHierarchical()) {
            throw new UnsupportedOperationException("This isn't a hierarchical URI");
        }
        ArrayList<String> out = new ArrayList<String>();
        String q = getEncodedQuery();
        if (q == null) {
            return out;
        }
        for (String part : split(q, '&')) {
            int eq = part.indexOf('=');
            String k = decode(eq >= 0 ? part.substring(0, eq) : part);
            if (k.equals(key)) {
                if (eq < 0) {
                    out.add("");
                } else {
                    String raw = part.substring(eq + 1);
                    out.add(decode(plusIsSpace ? raw.replace('+', ' ') : raw));
                }
            }
        }
        return out;
    }

    public boolean getBooleanQueryParameter(String key, boolean defaultValue) {
        String v = getQueryParameter(key);
        if (v == null) {
            return defaultValue;
        }
        // Android folds the value to lower case first; equalsIgnoreCase does
        // that without the locale-sensitive toLowerCase().
        return !"false".equalsIgnoreCase(v) && !"0".equals(v);
    }

    public Builder buildUpon() {
        Builder b = new Builder();
        b.scheme = getScheme();
        b.authority = getEncodedAuthority();
        b.path = getEncodedPath();
        b.query = getEncodedQuery();
        b.fragment = getEncodedFragment();
        if (!isHierarchical()) {
            b.opaque = getEncodedSchemeSpecificPart();
        }
        return b;
    }

    /// This URI with its scheme folded to lower case (`HTTP://x` becomes
    /// `http://x`); the URI itself when the scheme is absent or already lower
    /// case. Nothing after the scheme changes.
    public Uri normalizeScheme() {
        String s = getScheme();
        if (s == null) {
            return this;
        }
        String lower = asciiLower(s);
        if (lower.equals(s)) {
            return this;
        }
        return new Uri(lower + string.substring(s.length()));
    }

    /// Lower-cases ASCII letters only. A scheme is ASCII by specification,
    /// and `toLowerCase()` would fold by the device locale (a Turkish
    /// device turns `I` into a dotless i).
    private static String asciiLower(String s) {
        char[] c = s.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (c[i] >= 'A' && c[i] <= 'Z') {
                c[i] = (char) (c[i] + ('a' - 'A'));
            }
        }
        return new String(c);
    }

    private static List<String> split(String s, char sep) {
        ArrayList<String> out = new ArrayList<String>();
        int start = 0;
        for (int i = 0; i <= s.length(); i++) {
            if (i == s.length() || s.charAt(i) == sep) {
                out.add(s.substring(start, i));
                start = i + 1;
            }
        }
        return out;
    }

    public static String encode(String s) {
        return encode(s, null);
    }

    /// Percent-encodes everything except unreserved characters and `allow`.
    public static String encode(String s, String allow) {
        if (s == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        byte[] bytes;
        try {
            bytes = s.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 is not supported");
        }
        for (byte b : bytes) {
            char c = (char) (b & 0xff);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '_' || c == '-' || c == '!' || c == '.' || c == '~' || c == '\'' || c == '(' || c == ')'
                    || c == '*' || (allow != null && allow.indexOf(c) >= 0);
            if (ok) {
                sb.append(c);
            } else {
                sb.append('%');
                String h = Integer.toHexString(b & 0xff);
                if (h.length() < 2) {
                    sb.append('0');
                }
                for (int i = 0; i < h.length(); i++) {
                    char hc = h.charAt(i);
                    sb.append(hc >= 'a' && hc <= 'f' ? (char) (hc - 32) : hc);
                }
            }
        }
        return sb.toString();
    }

    public static String decode(String s) {
        if (s == null) {
            return null;
        }
        if (s.indexOf('%') < 0) {
            return s;
        }
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '%' && i + 2 < s.length()) {
                int hi = Character.digit(s.charAt(i + 1), 16);
                int lo = Character.digit(s.charAt(i + 2), 16);
                if (hi >= 0 && lo >= 0) {
                    out.write(hi * 16 + lo);
                    i += 2;
                    continue;
                }
            }
            if (c < 0x80) {
                out.write(c);
            } else {
                // A supplementary character is encoded whole: its surrogates
                // encoded one at a time are not UTF-8 and decode as '?'.
                int end = i + 1;
                if (Character.isHighSurrogate(c) && end < s.length()
                        && Character.isLowSurrogate(s.charAt(end))) {
                    end++;
                }
                try {
                    byte[] b = s.substring(i, end).getBytes("UTF-8");
                    out.write(b, 0, b.length);
                } catch (java.io.UnsupportedEncodingException e) {
                    out.write('?');
                }
                i = end - 1;
            }
        }
        try {
            return new String(out.toByteArray(), "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return s;
        }
    }

    @Override
    public int compareTo(Uri other) {
        return string.compareTo(other.string);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Uri && ((Uri) o).string.equals(string);
    }

    @Override
    public int hashCode() {
        return string.hashCode();
    }

    @Override
    public String toString() {
        return string;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(android.os.Parcel dest, int flags) {
        dest.writeString(string);
    }

    /// Builds a URI piece by piece. As on Android, setting an authority, path
    /// or query makes the URI hierarchical and drops any opaque part, so
    /// `mailto:a@b` rebuilt with a path does not keep its stale opaque part.
    public static final class Builder {
        String scheme;
        String authority;
        String path;
        String query;
        String fragment;
        String opaque;

        public Builder scheme(String scheme) {
            this.scheme = scheme;
            return this;
        }

        public Builder authority(String authority) {
            this.opaque = null;
            this.authority = encode(authority, "@:[]");
            return this;
        }

        public Builder encodedAuthority(String authority) {
            this.opaque = null;
            this.authority = authority;
            return this;
        }

        public Builder path(String path) {
            this.opaque = null;
            this.path = encode(path, "/");
            return this;
        }

        public Builder encodedPath(String path) {
            this.opaque = null;
            this.path = path;
            return this;
        }

        public Builder appendPath(String segment) {
            return appendEncodedPath(encode(segment));
        }

        public Builder appendEncodedPath(String segment) {
            opaque = null;
            if (path == null || path.length() == 0) {
                path = segment.startsWith("/") ? segment : "/" + segment;
            } else {
                path = path.endsWith("/") ? path + (segment.startsWith("/") ? segment.substring(1) : segment)
                        : path + (segment.startsWith("/") ? segment : "/" + segment);
            }
            return this;
        }

        public Builder opaquePart(String opaque) {
            this.opaque = encode(opaque);
            return this;
        }

        public Builder query(String query) {
            this.opaque = null;
            this.query = encode(query);
            return this;
        }

        public Builder encodedQuery(String query) {
            this.opaque = null;
            this.query = query;
            return this;
        }

        public Builder appendQueryParameter(String key, String value) {
            opaque = null;
            String pair = encode(key) + "=" + encode(value);
            query = query == null || query.length() == 0 ? pair : query + "&" + pair;
            return this;
        }

        public Builder clearQuery() {
            opaque = null;
            query = null;
            return this;
        }

        public Builder fragment(String fragment) {
            this.fragment = encode(fragment);
            return this;
        }

        public Builder encodedFragment(String fragment) {
            this.fragment = fragment;
            return this;
        }

        public Uri build() {
            StringBuilder sb = new StringBuilder();
            if (scheme != null) {
                sb.append(scheme).append(':');
            }
            if (opaque != null) {
                sb.append(opaque);
            } else {
                if (authority != null) {
                    sb.append("//").append(authority);
                }
                if (path != null) {
                    if ((scheme != null || authority != null) && path.length() > 0 && !path.startsWith("/")) {
                        sb.append('/');
                    }
                    sb.append(path);
                }
                if (query != null) {
                    sb.append('?').append(query);
                }
            }
            if (fragment != null) {
                sb.append('#').append(fragment);
            }
            return new Uri(sb.toString());
        }

        @Override
        public String toString() {
            return build().toString();
        }
    }
}
