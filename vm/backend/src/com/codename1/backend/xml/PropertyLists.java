/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.backend.xml;

import com.codename1.backend.Base64;
import com.codename1.impl.backend.CivilTime;
import com.codename1.xml.Element;
import com.codename1.xml.XMLParseException;
import com.codename1.xml.XMLParser;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Reads Apple XML property lists: provisioning profiles, entitlements,
/// `Info.plist` files.
///
/// ```java
/// Map<String, Object> profile = (Map<String, Object>) PropertyLists.parse(mobileprovisionBytes);
/// String team = (String) ((List<Object>) profile.get("TeamIdentifier")).get(0);
/// Date expires = (Date) profile.get("ExpirationDate");
/// ```
///
/// Values map to Java types as follows:
///
/// | plist | Java |
/// |---|---|
/// | `<dict>` | `Map<String, Object>`, in document order |
/// | `<array>` | `List<Object>` |
/// | `<string>` | `String` |
/// | `<integer>` | `Long` |
/// | `<real>` | `Double` |
/// | `<true/>` `<false/>` | `Boolean` |
/// | `<data>` | `byte[]`, base64 decoded |
/// | `<date>` | `java.util.Date` (UTC, as `2025-01-31T12:00:00Z` is written) |
///
/// Check a value's type with `instanceof` before casting it.
///
/// A provisioning profile (`.mobileprovision`) is not a plist itself but a
/// CMS/PKCS#7 signed envelope with the XML plist inside it. [#parse(byte[])]
/// accepts it as it is, by locating the plist with [#extractXml(byte[])]; the
/// signature is not verified. Binary property lists (`bplist00`) are not
/// supported and are reported as such.
///
/// Parsing goes through the shared [XMLParser] refusing malformed input, so a document
/// type declaration is never interpreted and nesting is bounded: a hostile
/// plist cannot fetch a URL, expand entities or exhaust the stack.
public final class PropertyLists {
    private PropertyLists() {
    }

    /// Parses a property list -- an XML plist, or any binary blob that wraps one,
    /// such as a signed provisioning profile -- and answers its top-level value,
    /// usually a `Map<String, Object>`.
    ///
    /// #### Throws
    ///
    /// - `XMLParseException`: when no XML plist is found, the data is a binary plist,
    ///   or the plist is malformed (an unknown element, a `<dict>` key with no
    ///   value, an integer that does not parse).
    public static Object parse(byte[] data) {
        if (data == null) {
            throw new XMLParseException("no data");
        }
        if (startsWith(data, 0, "bplist")) {
            throw new XMLParseException("binary property lists are not supported");
        }
        int first = 0;
        while (first < data.length && isSpace(data[first])) {
            first++;
        }
        boolean bom = startsWithBom(data, first);
        byte[] xml = data;
        if (!bom && (first >= data.length || data[first] != '<')) {
            xml = extractXml(data);
            if (xml == null) {
                throw new XMLParseException("no XML property list found");
            }
        }
        try {
            return fromDocument(parseXml(new InputStreamReader(new ByteArrayInputStream(xml), "UTF-8")));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 is not supported"); //NOPMD PreserveStackTrace - UTF-8 is always present; there is nothing to preserve
        }
    }

    /// Parses an XML property list held in a string.
    ///
    /// #### Throws
    ///
    /// - `XMLParseException`: when the plist is malformed.
    public static Object parse(String xml) {
        if (xml == null) {
            throw new XMLParseException("no data");
        }
        return fromDocument(parseXml(new StringReader(xml)));
    }

    /// Finds the XML property list embedded in binary data and answers its bytes,
    /// from `<?xml` (or `<plist` when there is no declaration) through the end of
    /// the first `</plist>` after it; null when there is none.
    ///
    /// This is how the plist is read out of a provisioning profile, which stores
    /// it unencrypted inside a signed CMS envelope. The bytes before and after the
    /// span are not examined.
    public static byte[] extractXml(byte[] data) {
        if (data == null) {
            return null;
        }
        int start = indexOf(data, "<?xml", 0);
        int plist = indexOf(data, "<plist", 0);
        if (start < 0 || (plist >= 0 && plist < start)) {
            start = plist;
        }
        if (start < 0) {
            return null;
        }
        int end = indexOf(data, "</plist>", start);
        if (end < 0) {
            return null;
        }
        end += "</plist>".length();
        byte[] out = new byte[end - start];
        System.arraycopy(data, start, out, 0, out.length);
        return out;
    }

    /// Parses strictly: well formed or an exception, no entity expansion, the
    /// default nesting cap.
    private static Element parseXml(Reader in) {
        XMLParser parser = new XMLParser();
        parser.setRejectMalformed(true);
        Element root = parser.parse(in);
        if (root == null) {
            throw new XMLParseException("no root element");
        }
        return root;
    }

    /// The element children of `e`, in order; the whitespace between them is
    /// not a value.
    private static List<Element> elements(Element e) {
        List<Element> out = new ArrayList<Element>();
        int count = e.getNumChildren();
        for (int i = 0; i < count; i++) {
            Element c = e.getChildAt(i);
            if (!c.isTextElement()) {
                out.add(c);
            }
        }
        return out;
    }

    /// The character data of `e`, its text and CDATA children joined as written.
    private static String text(Element e) {
        StringBuilder b = new StringBuilder();
        int count = e.getNumChildren();
        for (int i = 0; i < count; i++) {
            Element c = e.getChildAt(i);
            if (c.isTextElement()) {
                b.append(c.getText());
            }
        }
        return b.toString();
    }

    private static Object fromDocument(Element root) {
        if (!"plist".equals(root.getTagName())) {
            return value(root);
        }
        List<Element> kids = elements(root);
        if (kids.size() != 1) {
            throw new XMLParseException("<plist> must hold exactly one value, found " + kids.size());
        }
        return value(kids.get(0));
    }

    private static Object value(Element e) {
        String n = e.getTagName();
        if ("dict".equals(n)) {
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            List<Element> kids = elements(e);
            int count = kids.size();
            for (int i = 0; i < count; i += 2) {
                Element key = kids.get(i);
                if (!"key".equals(key.getTagName())) {
                    throw new XMLParseException("expected <key> in <dict>, found <" + key.getTagName() + ">");
                }
                if (i + 1 >= count) {
                    throw new XMLParseException("<key>" + text(key) + "</key> has no value");
                }
                map.put(text(key), value(kids.get(i + 1)));
            }
            return map;
        }
        if ("array".equals(n)) {
            List<Object> list = new ArrayList<Object>();
            for (Element c : elements(e)) {
                list.add(value(c));
            }
            return list;
        }
        if ("string".equals(n)) {
            return text(e);
        }
        if ("integer".equals(n)) {
            String t = text(e).trim();
            try {
                return Long.valueOf(Long.parseLong(t));
            } catch (NumberFormatException ex) {
                throw new XMLParseException("bad <integer> " + t); //NOPMD PreserveStackTrace - the shared exception has no cause constructor (CLDC); the message names the value
            }
        }
        if ("real".equals(n)) {
            String t = text(e).trim();
            try {
                return Double.valueOf(Double.parseDouble(t));
            } catch (NumberFormatException ex) {
                throw new XMLParseException("bad <real> " + t); //NOPMD PreserveStackTrace - the shared exception has no cause constructor (CLDC); the message names the value
            }
        }
        if ("true".equals(n)) {
            return Boolean.TRUE;
        }
        if ("false".equals(n)) {
            return Boolean.FALSE;
        }
        if ("data".equals(n)) {
            byte[] bytes = Base64.decode(stripWhitespace(text(e)));
            if (bytes == null) {
                throw new XMLParseException("bad base64 in <data>");
            }
            return bytes;
        }
        if ("date".equals(n)) {
            return parseDate(text(e).trim());
        }
        throw new XMLParseException("unknown property list element <" + n + ">");
    }

    /// Parses the one form plists write, `yyyy-MM-ddTHH:mm:ssZ`, as UTC.
    private static Date parseDate(String s) {
        if (s.length() != 20 || s.charAt(4) != '-' || s.charAt(7) != '-' || s.charAt(10) != 'T'
                || s.charAt(13) != ':' || s.charAt(16) != ':' || s.charAt(19) != 'Z') {
            throw new XMLParseException("bad <date> " + s);
        }
        int year = digits(s, 0, 4);
        int month = digits(s, 5, 2);
        int day = digits(s, 8, 2);
        int hour = digits(s, 11, 2);
        int minute = digits(s, 14, 2);
        int second = digits(s, 17, 2);
        if (year < 0 || month < 1 || month > 12 || day < 1 || day > 31
                || hour < 0 || hour > 23 || minute < 0 || minute > 59 || second < 0 || second > 60) {
            throw new XMLParseException("bad <date> " + s);
        }
        if (day > CivilTime.daysInMonth(year, month)) {
            throw new XMLParseException("bad <date> " + s);
        }
        long days = CivilTime.daysFromCivil(year, month, day);
        long seconds = days * 86400L + hour * 3600L + minute * 60L + second;
        return new Date(seconds * 1000L);
    }

    private static int digits(String s, int start, int count) {
        int v = 0;
        for (int i = start; i < start + count; i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return -1;
            }
            v = v * 10 + (c - '0');
        }
        return v;
    }

    private static String stripWhitespace(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != ' ' && c != '\t' && c != '\r' && c != '\n') {
                b.append(c);
            }
        }
        return b.toString();
    }

    private static boolean isSpace(byte b) {
        return b == ' ' || b == '\t' || b == '\r' || b == '\n';
    }

    private static boolean startsWithBom(byte[] data, int at) {
        return at + 2 < data.length && (data[at] & 0xff) == 0xef && (data[at + 1] & 0xff) == 0xbb
                && (data[at + 2] & 0xff) == 0xbf;
    }

    private static boolean startsWith(byte[] data, int at, String ascii) {
        if (at + ascii.length() > data.length) {
            return false;
        }
        for (int i = 0; i < ascii.length(); i++) {
            if (data[at + i] != ascii.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private static int indexOf(byte[] data, String ascii, int from) {
        int last = data.length - ascii.length();
        for (int i = from; i <= last; i++) {
            if (startsWith(data, i, ascii)) {
                return i;
            }
        }
        return -1;
    }
}
