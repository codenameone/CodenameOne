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
package com.codename1.io;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Hashtable;

/// Reads and writes text in a charset that the DATA declared -- an HTTP
/// `Content-Type` header, an XML declaration, an HTML form -- rather than one
/// the application chose.
///
/// A name the data declares is not under the application's control, and the
/// platforms do not all support the same set: a feed labelled `Shift_JIS` or
/// `KOI8-R` is read by the simulator and by Android, and refused with an
/// `UnsupportedEncodingException` on a device whose class library does not
/// convert that charset. So a declared name that the platform cannot convert
/// falls back to UTF-8, the way a browser falls back for a label it does not
/// know, and is logged once. The text is still read, approximately where the
/// declaration was right, and exactly where it was wrong, which on the web it
/// often is.
///
/// A charset the application names itself should be passed to
/// `InputStreamReader`, `OutputStreamWriter` or `String` directly. They throw
/// for an unsupported name, as they do on the JDK, and that is the right
/// answer for a name that is a mistake in the application's own code.
///
/// A declaration is accepted as a bare name (`ISO-8859-1`), as a parameter
/// (`charset=utf-8`), or as a whole header value
/// (`text/xml; charset="UTF-8"; foo=bar`); quotes, surrounding white space
/// and other parameters are dropped. A media type with no `charset`
/// parameter declares nothing, and is read as UTF-8 without a log line.
public final class DeclaredCharset {
    private static final String CHARSET_PARAMETER = "charset";

    // The names already logged, so a feed polled every minute logs its
    // unsupported label once rather than once a minute. Bounded, because the
    // names come from the network.
    private static final int MAX_LOGGED = 64;
    private static final Hashtable<String, String> LOGGED = new Hashtable<String, String>();

    private DeclaredCharset() {
    }

    /// The charset name a declaration carries, or null when it carries none.
    ///
    /// #### Parameters
    ///
    /// - `declared`: a charset name, a `charset=` parameter or a whole
    ///   `Content-Type` value; null is accepted and carries no name
    ///
    /// #### Returns
    ///
    /// the name with quotes, white space and other parameters removed, or null
    public static String name(String declared) {
        if (declared == null) {
            return null;
        }
        // The declaration is parsed into its parameters rather than searched
        // for "charset=": a search also matches a parameter whose name merely
        // ends in charset (`x-charset=windows-1252`) and text inside another
        // parameter's quoted value (`foo="a;charset=b"`), and either one was
        // taken as the content charset.
        int length = declared.length();
        int first = endOfValue(declared, 0);
        String head = declared.substring(0, first).trim();
        int pos;
        String bare = null;
        if (head.indexOf('=') >= 0) {
            // It starts with a parameter (`charset=utf-8; foo`): parse it as one.
            pos = 0;
        } else {
            if (head.indexOf('/') < 0) {
                // A bare name (`ISO-8859-1`, `'koi8-r'`), unless a charset
                // parameter follows it.
                bare = head;
            }
            pos = first < length ? first + 1 : length;
        }
        while (pos < length) {
            int eq = pos;
            while (eq < length && declared.charAt(eq) != '=' && !isDelimiter(declared.charAt(eq))) {
                eq++;
            }
            if (eq >= length || declared.charAt(eq) != '=') {
                // A parameter without a value names nothing.
                pos = eq + 1;
                continue;
            }
            boolean charset = isCharsetName(declared, pos, eq);
            int valueStart = eq + 1;
            int valueEnd = endOfValue(declared, valueStart);
            if (charset) {
                return unquote(declared.substring(valueStart, valueEnd));
            }
            pos = valueEnd + 1;
        }
        return bare == null ? null : unquote(bare);
    }

    // The parameter name between from and to, with its surrounding white
    // space, is exactly "charset". regionMatches compares without case and
    // without the device's locale, which toLowerCase would bring in.
    private static boolean isCharsetName(String s, int from, int to) {
        while (from < to && s.charAt(from) <= ' ') {
            from++;
        }
        while (to > from && s.charAt(to - 1) <= ' ') {
            to--;
        }
        return to - from == CHARSET_PARAMETER.length()
                && s.regionMatches(true, from, CHARSET_PARAMETER, 0, CHARSET_PARAMETER.length());
    }

    private static boolean isDelimiter(char c) {
        return c == ';' || c == ',';
    }

    // Where the value starting at from ends: at the first delimiter that is
    // not inside a quoted string. A quote opens a string only where the value
    // begins, as in a header; an escaped character inside double quotes is
    // skipped, so `\"` does not close it. An unclosed string runs to the end.
    private static int endOfValue(String s, int from) {
        int length = s.length();
        int i = from;
        while (i < length && s.charAt(i) <= ' ') {
            i++;
        }
        if (i < length && (s.charAt(i) == '"' || s.charAt(i) == '\'')) {
            char quote = s.charAt(i);
            i++;
            while (i < length && s.charAt(i) != quote) {
                if (quote == '"' && s.charAt(i) == '\\') {
                    i++;
                }
                i++;
            }
            i++;
        }
        while (i < length && !isDelimiter(s.charAt(i))) {
            i++;
        }
        return i < length ? i : length;
    }

    // The value with its white space and surrounding quotes removed, and the
    // escapes of a double quoted string resolved; null when nothing is left.
    private static String unquote(String value) {
        String name = value.trim();
        if (name.length() > 0 && (name.charAt(0) == '"' || name.charAt(0) == '\'')) {
            char quote = name.charAt(0);
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < name.length(); i++) {
                char c = name.charAt(i);
                if (c == quote) {
                    break;
                }
                if (quote == '"' && c == '\\' && i + 1 < name.length()) {
                    c = name.charAt(++i);
                }
                sb.append(c);
            }
            name = sb.toString().trim();
        }
        return name.length() == 0 ? null : name;
    }

    /// A reader for `in` in the charset `declared` names, or in UTF-8 when it
    /// names none or one this platform cannot convert.
    ///
    /// #### Parameters
    ///
    /// - `in`: the stream to read
    ///
    /// - `declared`: the declaration, in any form `name(String)` accepts
    ///
    /// #### Returns
    ///
    /// a reader over `in`; it never throws for the charset
    public static InputStreamReader reader(InputStream in, String declared) {
        String name = name(declared);
        if (name != null) {
            try {
                return new InputStreamReader(in, name);
            } catch (UnsupportedEncodingException unsupported) {
                logUnsupported(name);
            } catch (IllegalArgumentException illegal) {
                // A name that is not even well formed, which some runtimes
                // report this way rather than as unsupported.
                logUnsupported(name);
            }
        }
        return Util.getReader(in);
    }

    /// A writer to `out` in the charset `declared` names, or in UTF-8 when it
    /// names none or one this platform cannot convert.
    ///
    /// #### Parameters
    ///
    /// - `out`: the stream to write
    ///
    /// - `declared`: the declaration, in any form `name(String)` accepts
    ///
    /// #### Returns
    ///
    /// a writer to `out`; it never throws for the charset
    public static OutputStreamWriter writer(OutputStream out, String declared) {
        String name = name(declared);
        if (name != null) {
            try {
                return new OutputStreamWriter(out, name);
            } catch (UnsupportedEncodingException unsupported) {
                logUnsupported(name);
            } catch (IllegalArgumentException illegal) {
                logUnsupported(name);
            }
        }
        return Util.getWriter(out);
    }

    private static void logUnsupported(String name) {
        if (LOGGED.containsKey(name) || LOGGED.size() >= MAX_LOGGED) {
            return;
        }
        LOGGED.put(name, name);
        Log.p("The data declares the charset \"" + name
                + "\", which this platform does not support; using UTF-8 instead", Log.WARNING);
    }
}
