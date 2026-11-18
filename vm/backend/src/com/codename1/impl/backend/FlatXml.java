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
package com.codename1.impl.backend;

import java.util.ArrayList;
import java.util.List;

/// Element text out of flat service XML, shared by the object storage clients.
/// Internal: not part of the backend's API.
public final class FlatXml {
    private FlatXml() {
    }

    /// The text of every `<name>` element, in order.
    ///
    /// A deliberate non-parser for the flat XML that S3 and Azure Blob Storage
    /// answer with: their list and error responses are flat, the element
    /// names wanted are known, and a real XML parser is a dependency this runtime
    /// does not have. It decodes the five predefined entities and numeric
    /// references, which is what those services escape in a key.
    public static List<String> elements(String xml, String name) {
        List<String[]> tagged = tagged(xml, name);
        List<String> out = new ArrayList<String>(tagged.size());
        for (String[] t : tagged) {
            out.add(t[1]);
        }
        return out;
    }

    /// Like [#elements], with each value paired with the attribute text of its
    /// opening tag: `{attributes, value}`, the attributes empty for a bare
    /// `<name>`. An element that carries attributes is matched too, which
    /// [#elements] relies on: Azure marks a blob name it had to percent-encode
    /// as `<Name Encoded="true">`, and a matcher for the literal `<Name>` would
    /// drop that key from a listing without a word.
    public static List<String[]> tagged(String xml, String name) {
        List<String[]> out = new ArrayList<String[]>();
        if (xml == null) {
            return out;
        }
        String open = "<" + name;
        String close = "</" + name + ">";
        int at = 0;
        while (true) {
            int start = xml.indexOf(open, at);
            if (start < 0) {
                return out;
            }
            int after = start + open.length();
            if (after >= xml.length()) {
                return out;
            }
            char c = xml.charAt(after);
            if (c != '>' && c != '/' && c != ' ' && c != '\t' && c != '\r' && c != '\n') {
                // A longer name sharing the prefix: <NameSpace> is not <Name>.
                at = after;
                continue;
            }
            int gt = xml.indexOf('>', after);
            if (gt < 0) {
                return out;
            }
            if (xml.charAt(gt - 1) == '/') {
                // <Name/> or <Name a="b"/>: an empty value.
                out.add(new String[] {xml.substring(after, gt - 1).trim(), ""});
                at = gt + 1;
                continue;
            }
            int end = xml.indexOf(close, gt + 1);
            if (end < 0) {
                return out;
            }
            out.add(new String[] {xml.substring(after, gt).trim(),
                    unescape(xml.substring(gt + 1, end))});
            at = end + close.length();
        }
    }

    /// A numeric XML entity's code point, or -1 for anything that is not one.
    ///
    /// Deliberately not Integer.parseInt: it accepts a leading sign and any Unicode
    /// digit Character.digit knows, so "&#x+41;" and "&#+65;" both decoded to "A" --
    /// two more spellings of a character, in a decoder whose output becomes object
    /// keys. com.codename1.backend.Hex applies the same rule to HTTP and URI input.
    /// That class is package-private to com.codename1.backend, so the rule is
    /// restated here rather than widening an internal utility to public.
    private static int entityCodePoint(String text, int from, int radix) {
        if (from >= text.length()) {
            return -1;
        }
        long value = 0;
        for (int iter = from ; iter < text.length() ; iter++) {
            char c = text.charAt(iter);
            int digit;
            if (c >= '0' && c <= '9') {
                digit = c - '0';
            } else if (radix == 16 && c >= 'a' && c <= 'f') {
                digit = c - 'a' + 10;
            } else if (radix == 16 && c >= 'A' && c <= 'F') {
                digit = c - 'A' + 10;
            } else {
                return -1;
            }
            value = value * radix + digit;
            if (value > 0x10FFFF) {
                return -1;
            }
        }
        return (int) value;
    }

    public static String unescape(String value) {
        if (value.indexOf('&') < 0) {
            return value;
        }
        StringBuilder out = new StringBuilder(value.length());
        int at = 0;
        while (at < value.length()) {
            char c = value.charAt(at);
            if (c != '&') {
                out.append(c);
                at++;
                continue;
            }
            int semi = value.indexOf(';', at);
            if (semi < 0) {
                out.append(c);
                at++;
                continue;
            }
            String entity = value.substring(at + 1, semi);
            if ("amp".equals(entity)) {
                out.append('&');
            } else if ("lt".equals(entity)) {
                out.append('<');
            } else if ("gt".equals(entity)) {
                out.append('>');
            } else if ("quot".equals(entity)) {
                out.append('"');
            } else if ("apos".equals(entity)) {
                out.append('\'');
            } else if (entity.length() > 1 && entity.charAt(0) == '#') {
                // appendCodePoint, not a cast. A cast to char keeps the low 16
                // bits, so the perfectly legal &#x1F600; became U+F600 -- a
                // private-use character -- and an object key or a continuation
                // token carrying an emoji came back corrupted. A truncated token
                // pages from the wrong place, which is a wrong ANSWER rather than
                // an error.
                //
                boolean hexEntity = entity.charAt(1) == 'x' || entity.charAt(1) == 'X';
                int code = hexEntity ? entityCodePoint(entity, 2, 16)
                                     : entityCodePoint(entity, 1, 10);
                if (code >= 0 && code <= 0x10FFFF) {
                    // The surrogate pair BY HAND. StringBuilder.appendCodePoint
                    // does not exist in vm/JavaAPI, so it compiles against the JDK
                    // for the Java SE arm and fails the translated build -- which
                    // is the rule that core code may only call what the VM
                    // actually defines.
                    if (code > 0xFFFF) {
                        int astral = code - 0x10000;
                        out.append((char) (0xD800 + (astral >> 10)));
                        out.append((char) (0xDC00 + (astral & 0x3FF)));
                    } else {
                        out.append((char) code);
                    }
                } else {
                    out.append('&').append(entity).append(';');
                }
            } else {
                out.append('&').append(entity).append(';');
            }
            at = semi + 1;
        }
        return out.toString();
    }
}
