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
package com.codename1.compat.jdk;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/// `java.util.PropertyResourceBundle` for the Codename One runtime: a
/// resource bundle read from a `.properties` file.
///
/// The parser implements the whole `java.util.Properties` text format, since
/// the device library has no `Properties` class to delegate to: `#` and `!`
/// comment lines, `=`, `:` or whitespace between key and value, a logical line
/// continued by a trailing backslash, and the escapes -- tab, newline, form
/// feed, carriage return, backslash-u followed by four hex digits, and a
/// backslash before any other character standing for that character.
///
/// A stream is decoded as ISO-8859-1, one byte to one character, which is
/// what the JDK did for as long as `.properties` files have carried
/// backslash-u escapes for everything else. Pass a `Reader` to choose another
/// encoding.
public class PropertyResourceBundle extends ResourceBundle {

    private final Map<String, Object> lookup = new HashMap<String, Object>();

    public PropertyResourceBundle(InputStream stream) throws IOException {
        StringBuilder text = new StringBuilder();
        byte[] buffer = new byte[4096];
        int n = stream.read(buffer);
        while (n >= 0) {
            for (int i = 0; i < n; i++) {
                text.append((char) (buffer[i] & 0xff));
            }
            n = stream.read(buffer);
        }
        parse(text.toString(), lookup);
    }

    public PropertyResourceBundle(Reader reader) throws IOException {
        StringBuilder text = new StringBuilder();
        char[] buffer = new char[4096];
        int n = reader.read(buffer, 0, buffer.length);
        while (n >= 0) {
            text.append(buffer, 0, n);
            n = reader.read(buffer, 0, buffer.length);
        }
        parse(text.toString(), lookup);
    }

    @Override
    public Object handleGetObject(String key) {
        if (key == null) {
            throw new NullPointerException();
        }
        return lookup.get(key);
    }

    @Override
    public Enumeration<String> getKeys() {
        return new ResourceBundle.KeyEnumeration(keySet().iterator());
    }

    @Override
    protected Set<String> handleKeySet() {
        return lookup.keySet();
    }

    private static boolean isBlank(char c) {
        return c == ' ' || c == '\t' || c == '\f';
    }

    private static boolean isLineEnd(char c) {
        return c == '\n' || c == '\r';
    }

    /// Reads `.properties` text into `out`, a later definition of a key
    /// replacing an earlier one.
    static void parse(String text, Map<String, ? super String> out) {
        int n = text.length();
        int i = 0;
        StringBuilder line = new StringBuilder();
        while (i < n) {
            // Blank lines and the indentation of a logical line carry nothing.
            while (i < n && (isBlank(text.charAt(i)) || isLineEnd(text.charAt(i)))) {
                i++;
            }
            if (i >= n) {
                break;
            }
            char c = text.charAt(i);
            if (c == '#' || c == '!') {
                // A comment runs to the end of its natural line and cannot be
                // continued, whatever it ends with.
                while (i < n && !isLineEnd(text.charAt(i))) {
                    i++;
                }
                continue;
            }
            line.setLength(0);
            boolean escaped = false;
            // A line that is nothing but a continuation is no entry, unless
            // the input ends in the middle of it: then the JDK defines the
            // empty key, and so does this.
            boolean cutShort = false;
            while (i < n) {
                c = text.charAt(i);
                if (isLineEnd(c)) {
                    if (!escaped) {
                        break;
                    }
                    // An odd run of backslashes before the terminator joins
                    // the next natural line, minus its indentation.
                    line.setLength(line.length() - 1);
                    i++;
                    cutShort = i >= n;
                    if (c == '\r' && i < n && text.charAt(i) == '\n') {
                        i++;
                    }
                    while (i < n && isBlank(text.charAt(i))) {
                        i++;
                    }
                    escaped = false;
                    continue;
                }
                line.append(c);
                escaped = c == '\\' && !escaped;
                i++;
            }
            if (escaped) {
                // A lone backslash at the end of the input escapes nothing.
                line.setLength(line.length() - 1);
                cutShort = true;
            }
            if (line.length() > 0 || cutShort) {
                store(line, out);
            }
        }
    }

    private static void store(StringBuilder line, Map<String, ? super String> out) {
        int len = line.length();
        int keyLen = 0;
        int valueStart = len;
        boolean hasSeparator = false;
        boolean escaped = false;
        while (keyLen < len) {
            char c = line.charAt(keyLen);
            if ((c == '=' || c == ':') && !escaped) {
                valueStart = keyLen + 1;
                hasSeparator = true;
                break;
            }
            if (isBlank(c) && !escaped) {
                valueStart = keyLen + 1;
                break;
            }
            escaped = c == '\\' && !escaped;
            keyLen++;
        }
        // Whitespace may surround the one separator character.
        while (valueStart < len) {
            char c = line.charAt(valueStart);
            if (!isBlank(c)) {
                if (!hasSeparator && (c == '=' || c == ':')) {
                    hasSeparator = true;
                } else {
                    break;
                }
            }
            valueStart++;
        }
        out.put(unescape(line, 0, keyLen), unescape(line, valueStart, len));
    }

    private static int hexDigit(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        throw new IllegalArgumentException("Malformed unicode escape in properties text");
    }

    private static String unescape(StringBuilder in, int from, int to) {
        StringBuilder sb = new StringBuilder(to - from);
        int i = from;
        while (i < to) {
            char c = in.charAt(i++);
            if (c != '\\' || i >= to) {
                if (c != '\\') {
                    sb.append(c);
                }
                continue;
            }
            c = in.charAt(i++);
            if (c == 'u') {
                if (i + 4 > to) {
                    throw new IllegalArgumentException("Malformed unicode escape in properties text");
                }
                int value = 0;
                for (int k = 0; k < 4; k++) {
                    value = (value << 4) | hexDigit(in.charAt(i++));
                }
                sb.append((char) value);
            } else if (c == 't') {
                sb.append('\t');
            } else if (c == 'r') {
                sb.append('\r');
            } else if (c == 'n') {
                sb.append('\n');
            } else if (c == 'f') {
                sb.append('\f');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
