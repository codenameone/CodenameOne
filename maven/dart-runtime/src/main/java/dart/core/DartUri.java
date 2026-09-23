/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package dart.core;

/**
 * A minimal {@code dart:core} {@code Uri}. The new_gallery app mainly uses
 * {@code Uri.parse(String)} to hand a URL to {@code url_launcher} and reads it
 * back via {@code toString()}, but Dart code also inspects {@code scheme},
 * {@code host}, {@code pathSegments} and {@code queryParameters}, so this holds
 * the original text and parses those components on demand. The component
 * accessors follow Dart semantics: {@code scheme} is lower-cased and empty when
 * absent; {@code queryParameters} preserves insertion order and URL-decodes.
 */
public final class DartUri {

    private final String text;
    private String scheme = "";
    private String host = "";
    private long port = 0;
    /// Whether the authority named a port. Without one, port() answers the
    /// scheme's default, as Dart's Uri.port does.
    private boolean explicitPort;
    private String path = "";
    private String query = "";
    private String fragment = "";

    private DartUri(String text) {
        this.text = text;
        parse();
    }

    /** Dart's {@code Uri.parse}. */
    public static DartUri parse(String uri) {
        return new DartUri(uri == null ? "" : uri);
    }

    /** Dart's {@code Uri.tryParse} — never throws (this parser is total). */
    public static DartUri tryParse(String uri) {
        return uri == null ? null : new DartUri(uri);
    }

    private void parse() {
        String s = text;
        int hash = s.indexOf('#');
        if (hash >= 0) {
            fragment = s.substring(hash + 1);
            s = s.substring(0, hash);
        }
        int q = s.indexOf('?');
        if (q >= 0) {
            query = s.substring(q + 1);
            s = s.substring(0, q);
        }
        int colon = s.indexOf(':');
        if (colon > 0 && isScheme(s.substring(0, colon))) {
            scheme = asciiLower(s.substring(0, colon));
            s = s.substring(colon + 1);
        }
        if (s.startsWith("//")) {
            s = s.substring(2);
            int slash = s.indexOf('/');
            String authority = slash >= 0 ? s.substring(0, slash) : s;
            s = slash >= 0 ? s.substring(slash) : "";
            int at = authority.indexOf('@');
            if (at >= 0) {
                authority = authority.substring(at + 1);
            }
            // An IPv6 literal is bracketed, and its colons are not the port
            // separator: http://[::1]:8080 took the FIRST colon as one, leaving the
            // host "[" and a port that failed to parse. Dart's host is the address
            // without its brackets, and the port follows the closing bracket.
            int close = authority.startsWith("[") ? authority.indexOf(']') : -1;
            int pc = close >= 0 ? authority.indexOf(':', close) : authority.indexOf(':');
            if (close >= 0) {
                host = authority.substring(1, close);
                if (pc >= 0) {
                    explicitPort = true;
                    try {
                        port = Long.parseLong(authority.substring(pc + 1));
                    } catch (NumberFormatException ignored) {
                        port = 0;
                    }
                }
            } else if (pc >= 0) {
                explicitPort = true;
                host = authority.substring(0, pc);
                try {
                    port = Long.parseLong(authority.substring(pc + 1));
                } catch (NumberFormatException ignored) {
                    port = 0;
                }
            } else {
                host = authority;
            }
            // Dart canonicalises a registered name to lower case, so
            // https://EXAMPLE.COM/ and https://example.com/ have the same host --
            // host allowlists, route matches and host-keyed caches depend on it.
            host = asciiLower(host);
        }
        path = s;
    }

    /**
     * ASCII-only lower case. Schemes and host names are ASCII by definition, and
     * String.toLowerCase is locale sensitive: on a Turkish device "HTTP" folds
     * its I to a dotless i and no longer equals "http".
     */
    private static String asciiLower(String s) {
        if (s == null) {
            return null;
        }
        char[] out = null;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                if (out == null) {
                    out = s.toCharArray();
                }
                out[i] = (char) (c + ('a' - 'A'));
            }
        }
        return out == null ? s : new String(out);
    }

    private static boolean isScheme(String s) {
        if (s.isEmpty() || !Character.isLetter(s.charAt(0))) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '+' && c != '-' && c != '.') {
                return false;
            }
        }
        return true;
    }

    /** Dart's {@code Uri.scheme} — lower-cased, empty when absent. */
    public String scheme() {
        return scheme;
    }

    /** Dart's {@code Uri.host}. */
    public String host() {
        return host;
    }

    /** Dart's {@code Uri.port}. */
    /// Dart's Uri.port: the explicit port, or the scheme's default when the URI
    /// names none -- 80 for http, 443 for https, 0 otherwise. Returning 0 for
    /// https://example.com/path sent code that splits a URL into host and port
    /// to port 0 unless every URL spelled out :443.
    public long port() {
        if (explicitPort) {
            return port;
        }
        if ("http".equals(scheme)) {
            return 80;
        }
        if ("https".equals(scheme)) {
            return 443;
        }
        return 0;
    }

    /** Dart's {@code Uri.path}. */
    public String path() {
        return path;
    }

    /** Dart's {@code Uri.query}. */
    public String query() {
        return query;
    }

    /** Dart's {@code Uri.fragment}. */
    public String fragment() {
        return fragment;
    }

    /** Dart's {@code Uri.pathSegments} — the non-empty, decoded path segments. */
    public DartList<String> pathSegments() {
        DartList<String> out = new DartList<>();
        String p = path;
        if (p.startsWith("/")) {
            p = p.substring(1);
        }
        if (!p.isEmpty()) {
            for (String seg : p.split("/", -1)) {
                out.add(decodeComponent(seg));
            }
        }
        return out;
    }

    /** Dart's {@code Uri.queryParameters} — insertion-ordered, decoded. */
    public DartMap<String, String> queryParameters() {
        DartMap<String, String> out = new DartMap<>();
        if (!query.isEmpty()) {
            for (String pair : query.split("&", -1)) {
                if (pair.isEmpty()) {
                    continue;
                }
                int eq = pair.indexOf('=');
                if (eq >= 0) {
                    out.put(decodeQueryComponent(pair.substring(0, eq)),
                            decodeQueryComponent(pair.substring(eq + 1)));
                } else {
                    out.put(decodeQueryComponent(pair), "");
                }
            }
        }
        return out;
    }

    /// Dart's {@code Uri.decodeComponent}, for path segments: a {@code +} is a
    /// literal plus. URLDecoder is a form decoder and would turn it into a space,
    /// so it is escaped first.
    private static String decodeComponent(String s) {
        try {
            return java.net.URLDecoder.decode(s.replace("+", "%2B"), "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }

    /// Dart's {@code Uri.decodeQueryComponent}, for query keys and values, where
    /// a {@code +} IS a space -- {@code ?q=hello+world} reads back as "hello world".
    /// This is exactly URLDecoder's form decoding. The two used to share the
    /// path rule, which kept every plus in a query literal.
    private static String decodeQueryComponent(String s) {
        try {
            return java.net.URLDecoder.decode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }

    /**
     * The text with its scheme and host folded to lower case, as Dart's Uri prints
     * them: Uri.parse('HTTPS://EXAMPLE.COM/P').toString() is https://example.com/P.
     * The path, query and fragment are case sensitive and stay as written.
     */
    @Override
    public String toString() {
        if (canonical == null) {
            canonical = canonicalText(text);
        }
        return canonical;
    }

    private String canonical;

    private static String canonicalText(String t) {
        int end = t.length();
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c == '?' || c == '#') {
                end = i;
                break;
            }
        }
        char[] out = null;
        int rest = 0;
        int colon = t.indexOf(':');
        if (colon > 0 && colon < end && isScheme(t.substring(0, colon))) {
            out = foldRange(t, out, 0, colon);
            rest = colon + 1;
        }
        if (t.startsWith("//", rest)) {
            int authStart = rest + 2;
            int authEnd = end;
            for (int i = authStart; i < end; i++) {
                if (t.charAt(i) == '/') {
                    authEnd = i;
                    break;
                }
            }
            int at = t.indexOf('@', authStart);
            int hostStart = at >= 0 && at < authEnd ? at + 1 : authStart;
            int hostEnd = authEnd;
            if (hostStart < authEnd && t.charAt(hostStart) == '[') {
                int close = t.indexOf(']', hostStart);
                hostEnd = close >= 0 && close < authEnd ? close : authEnd;
            } else {
                int pc = t.indexOf(':', hostStart);
                if (pc >= 0 && pc < authEnd) {
                    hostEnd = pc;
                }
            }
            out = foldRange(t, out, hostStart, hostEnd);
        }
        return out == null ? t : new String(out);
    }

    private static char[] foldRange(String t, char[] out, int from, int to) {
        for (int i = from; i < to; i++) {
            char c = t.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                if (out == null) {
                    out = t.toCharArray();
                }
                out[i] = (char) (c + ('a' - 'A'));
            }
        }
        return out;
    }
}
