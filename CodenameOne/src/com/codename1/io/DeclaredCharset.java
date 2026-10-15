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
    private static final String CHARSET_PARAMETER = "charset=";

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
        int start = indexOfIgnoreCase(declared, CHARSET_PARAMETER);
        if (start >= 0) {
            start += CHARSET_PARAMETER.length();
        } else if (declared.indexOf('/') >= 0) {
            // A media type without a charset parameter: it names no charset.
            return null;
        } else {
            start = 0;
        }
        int end = declared.length();
        for (int i = start; i < end; i++) {
            char c = declared.charAt(i);
            if (c == ';' || c == ',') {
                end = i;
                break;
            }
        }
        String name = declared.substring(start, end).trim();
        if (name.length() > 0 && (name.charAt(0) == '"' || name.charAt(0) == '\'')) {
            char quote = name.charAt(0);
            name = name.substring(1);
            int close = name.indexOf(quote);
            if (close >= 0) {
                name = name.substring(0, close);
            }
            name = name.trim();
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

    // regionMatches compares without case and without the device's locale,
    // which toLowerCase would bring in.
    private static int indexOfIgnoreCase(String s, String part) {
        int last = s.length() - part.length();
        for (int i = 0; i <= last; i++) {
            if (s.regionMatches(true, i, part, 0, part.length())) {
                return i;
            }
        }
        return -1;
    }
}
