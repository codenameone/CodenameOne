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
            scheme = s.substring(0, colon).toLowerCase();
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
            int pc = authority.indexOf(':');
            if (pc >= 0) {
                host = authority.substring(0, pc);
                try {
                    port = Long.parseLong(authority.substring(pc + 1));
                } catch (NumberFormatException ignored) {
                    port = 0;
                }
            } else {
                host = authority;
            }
        }
        path = s;
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
    public long port() {
        return port;
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
                out.add(decode(seg));
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
                    out.put(decode(pair.substring(0, eq)), decode(pair.substring(eq + 1)));
                } else {
                    out.put(decode(pair), "");
                }
            }
        }
        return out;
    }

    private static String decode(String s) {
        try {
            return java.net.URLDecoder.decode(s.replace("+", "%2B"), "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }

    @Override
    public String toString() {
        return text;
    }
}
