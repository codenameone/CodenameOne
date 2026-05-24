/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.router;

import com.codename1.io.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Normalized representation of a deep-link URL.
///
/// Parses an arbitrary URL such as `myapp://users/42?tab=posts#bio` (custom schemes,
/// universal links, app links, in-app `Router.push` strings) into addressable parts:
/// scheme, host, path, decoded path segments, query parameters, and fragment.
///
/// Instances are immutable and safe to pass between threads. Parsing is intentionally
/// permissive: a malformed input never throws; missing parts come back as empty strings
/// or empty collections so handlers can branch with simple null-free checks.
///
/// #### Example
///
/// ```java
/// Display.getInstance().setDeepLinkHandler(new LinkHandler() {
///     public boolean handle(DeepLink link) {
///         if ("/users".equals(link.getPath()) || link.getPath().startsWith("/users/")) {
///             Router.getInstance().push(link.getPath());
///             return true;
///         }
///         return false;
///     }
/// });
/// ```
///
/// #### Since 8.0
public final class DeepLink {
    private final String raw;
    private final String scheme;
    private final String host;
    private final String path;
    private final String fragment;
    private final List<String> segments;
    private final Map<String, String> query;

    private DeepLink(String raw, String scheme, String host, String path, String fragment,
                     List<String> segments, Map<String, String> query) {
        this.raw = raw;
        this.scheme = scheme;
        this.host = host;
        this.path = path;
        this.fragment = fragment;
        this.segments = Collections.unmodifiableList(segments);
        this.query = Collections.unmodifiableMap(query);
    }

    /// The raw input URL exactly as it was received from the platform. Never null;
    /// returns an empty string when constructed from a null input.
    public String getRaw() {
        return raw;
    }

    /// Lower-cased URL scheme such as `https`, `myapp`. Empty when the input was a
    /// bare path (e.g. an internal `Router.push("/profile/42")`).
    public String getScheme() {
        return scheme;
    }

    /// Lower-cased URL host such as `example.com`. Empty for custom-scheme links
    /// that don't include a host (e.g. `myapp:profile/42`).
    public String getHost() {
        return host;
    }

    /// URL path starting with `/`. Always non-null; the root is `/`. Trailing slashes
    /// are preserved.
    public String getPath() {
        return path;
    }

    /// URL fragment without the leading `#`. Empty when no fragment was present.
    public String getFragment() {
        return fragment;
    }

    /// Decoded non-empty path segments. For `/users/42` this returns `["users", "42"]`.
    /// Unmodifiable.
    public List<String> getSegments() {
        return segments;
    }

    /// Decoded query parameters. Repeated keys keep only the last value. Unmodifiable.
    public Map<String, String> getQueryParameters() {
        return query;
    }

    /// Returns the decoded value of a single query parameter, or null if absent.
    public String getQueryParameter(String name) {
        return query.get(name);
    }

    /// Returns true when the link is fully empty (no scheme, host, or non-root path).
    /// Useful for `getAppArg` cold-launches where the value may be blank.
    public boolean isEmpty() {
        return scheme.length() == 0 && host.length() == 0
                && (path.length() == 0 || "/".equals(path))
                && fragment.length() == 0 && query.isEmpty();
    }

    /// Returns a new DeepLink with the given path, preserving the rest of the URL.
    /// Useful when a guard rewrites a request before passing it down the chain.
    public DeepLink withPath(String newPath) {
        String p = (newPath == null || newPath.length() == 0) ? "/"
                : (newPath.charAt(0) == '/' ? newPath : "/" + newPath);
        return new DeepLink(raw, scheme, host, p, fragment, splitSegments(p), query);
    }

    @Override
    public String toString() {
        return raw;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DeepLink)) {
            return false;
        }
        return raw.equals(((DeepLink) o).raw);
    }

    @Override
    public int hashCode() {
        return raw.hashCode();
    }

    /// Parses any URL-like string into a DeepLink. Tolerant of custom schemes,
    /// missing hosts, percent-encoded segments, and bare paths. Never throws.
    /// A null input becomes an empty DeepLink whose #isEmpty returns true.
    public static DeepLink parse(String url) {
        if (url == null || url.length() == 0) {
            return new DeepLink("", "", "", "/", "",
                    new ArrayList<String>(), new LinkedHashMap<String, String>());
        }
        String raw = url;
        String rest = url;
        String fragment = "";
        int hash = rest.indexOf('#');
        if (hash >= 0) {
            fragment = decode(rest.substring(hash + 1));
            rest = rest.substring(0, hash);
        }
        String queryStr = "";
        int qix = rest.indexOf('?');
        if (qix >= 0) {
            queryStr = rest.substring(qix + 1);
            rest = rest.substring(0, qix);
        }
        Map<String, String> query = parseQuery(queryStr);

        String scheme = "";
        String host = "";
        String path;

        int schemeIx = rest.indexOf(':');
        // Detect scheme: must be alpha[alnum+.-]* followed by ':'. Avoids parsing
        // a path-only "/foo:bar" as a scheme.
        if (schemeIx > 0 && isValidSchemePrefix(rest, schemeIx)) {
            scheme = rest.substring(0, schemeIx).toLowerCase();
            String afterScheme = rest.substring(schemeIx + 1);
            if (afterScheme.startsWith("//")) {
                String hostAndPath = afterScheme.substring(2);
                int slash = hostAndPath.indexOf('/');
                if (slash < 0) {
                    host = stripUserAndPort(hostAndPath);
                    path = "/";
                } else {
                    host = stripUserAndPort(hostAndPath.substring(0, slash));
                    path = hostAndPath.substring(slash);
                }
            } else {
                // Custom scheme without `//` -- treat the remainder as the path.
                path = afterScheme.length() == 0 || afterScheme.charAt(0) == '/'
                        ? (afterScheme.length() == 0 ? "/" : afterScheme)
                        : "/" + afterScheme;
            }
        } else {
            // Bare path -- internal Router.push("/x") and similar.
            path = (rest.length() == 0 || rest.charAt(0) == '/') ? rest : "/" + rest;
            if (path.length() == 0) {
                path = "/";
            }
        }

        return new DeepLink(raw, scheme, host.toLowerCase(), path, fragment,
                splitSegments(path), query);
    }

    private static boolean isValidSchemePrefix(String s, int colon) {
        if (colon <= 0) {
            return false;
        }
        char c0 = s.charAt(0);
        if (!isAlpha(c0)) {
            return false;
        }
        for (int i = 1; i < colon; i++) {
            char c = s.charAt(i);
            if (!(isAlpha(c) || isDigit(c) || c == '+' || c == '-' || c == '.')) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static String stripUserAndPort(String hostPart) {
        // Strip user-info `user:pass@`.
        int at = hostPart.lastIndexOf('@');
        if (at >= 0) {
            hostPart = hostPart.substring(at + 1);
        }
        // Strip port.
        int colon = hostPart.indexOf(':');
        if (colon >= 0) {
            hostPart = hostPart.substring(0, colon);
        }
        return hostPart;
    }

    private static List<String> splitSegments(String path) {
        ArrayList<String> out = new ArrayList<String>();
        if (path == null || path.length() == 0 || "/".equals(path)) {
            return out;
        }
        String p = path.charAt(0) == '/' ? path.substring(1) : path;
        int start = 0;
        for (int i = 0; i < p.length(); i++) {
            if (p.charAt(i) == '/') {
                if (i > start) {
                    out.add(decode(p.substring(start, i)));
                }
                start = i + 1;
            }
        }
        if (start < p.length()) {
            out.add(decode(p.substring(start)));
        }
        return out;
    }

    private static Map<String, String> parseQuery(String q) {
        LinkedHashMap<String, String> out = new LinkedHashMap<String, String>();
        if (q == null || q.length() == 0) {
            return out;
        }
        int start = 0;
        for (int i = 0; i <= q.length(); i++) {
            if (i == q.length() || q.charAt(i) == '&') {
                if (i > start) {
                    String pair = q.substring(start, i);
                    int eq = pair.indexOf('=');
                    if (eq < 0) {
                        out.put(decode(pair), "");
                    } else {
                        out.put(decode(pair.substring(0, eq)), decode(pair.substring(eq + 1)));
                    }
                }
                start = i + 1;
            }
        }
        return out;
    }

    private static String decode(String s) {
        // Util.decode handles `+` as space which is correct for query strings but
        // wrong for path segments. We accept that tradeoff: paths in deep links
        // shouldn't contain literal `+` in practice, and Util is platform-portable.
        try {
            return Util.decode(s, "UTF-8", false);
        } catch (Throwable t) {
            return s;
        }
    }
}
