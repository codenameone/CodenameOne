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
/// Parsing goes through [XmlPullParser], so a document type declaration is never
/// interpreted and nesting is bounded: a hostile plist cannot fetch a URL,
/// expand entities or exhaust the stack.
public final class PropertyLists {
    private PropertyLists() {
    }

    /// Parses a property list -- an XML plist, or any binary blob that wraps one,
    /// such as a signed provisioning profile -- and answers its top-level value,
    /// usually a `Map<String, Object>`.
    ///
    /// #### Throws
    ///
    /// - `XmlException`: when no XML plist is found, the data is a binary plist,
    ///   or the plist is malformed (an unknown element, a `<dict>` key with no
    ///   value, an integer that does not parse).
    public static Object parse(byte[] data) throws XmlException {
        if (data == null) {
            throw new XmlException("no data");
        }
        if (startsWith(data, 0, "bplist")) {
            throw new XmlException("binary property lists are not supported");
        }
        int first = 0;
        while (first < data.length && isSpace(data[first])) {
            first++;
        }
        boolean bom = startsWithBom(data, first);
        XmlElement root;
        if (bom || (first < data.length && data[first] == '<')) {
            root = XmlElement.parse(data);
        } else {
            byte[] xml = extractXml(data);
            if (xml == null) {
                throw new XmlException("no XML property list found");
            }
            root = XmlElement.parse(xml);
        }
        return fromDocument(root);
    }

    /// Parses an XML property list held in a string.
    ///
    /// #### Throws
    ///
    /// - `XmlException`: when the plist is malformed.
    public static Object parse(String xml) throws XmlException {
        return fromDocument(XmlElement.parse(xml));
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

    private static Object fromDocument(XmlElement root) throws XmlException {
        if (!"plist".equals(root.getName())) {
            return value(root);
        }
        List<XmlElement> kids = root.getChildren();
        if (kids.size() != 1) {
            throw new XmlException("<plist> must hold exactly one value, found " + kids.size());
        }
        return value(kids.get(0));
    }

    private static Object value(XmlElement e) throws XmlException {
        String n = e.getName();
        if ("dict".equals(n)) {
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            List<XmlElement> kids = e.getChildren();
            int count = kids.size();
            for (int i = 0; i < count; i += 2) {
                XmlElement key = kids.get(i);
                if (!"key".equals(key.getName())) {
                    throw new XmlException("expected <key> in <dict>, found <" + key.getName() + ">");
                }
                if (i + 1 >= count) {
                    throw new XmlException("<key>" + key.getText() + "</key> has no value");
                }
                map.put(key.getText(), value(kids.get(i + 1)));
            }
            return map;
        }
        if ("array".equals(n)) {
            List<Object> list = new ArrayList<Object>();
            for (XmlElement c : e.getChildren()) {
                list.add(value(c));
            }
            return list;
        }
        if ("string".equals(n)) {
            return e.getText();
        }
        if ("integer".equals(n)) {
            String t = e.getText().trim();
            try {
                return Long.valueOf(Long.parseLong(t));
            } catch (NumberFormatException ex) {
                throw new XmlException("bad <integer> " + t, ex);
            }
        }
        if ("real".equals(n)) {
            String t = e.getText().trim();
            try {
                return Double.valueOf(Double.parseDouble(t));
            } catch (NumberFormatException ex) {
                throw new XmlException("bad <real> " + t, ex);
            }
        }
        if ("true".equals(n)) {
            return Boolean.TRUE;
        }
        if ("false".equals(n)) {
            return Boolean.FALSE;
        }
        if ("data".equals(n)) {
            byte[] bytes = Base64.decode(stripWhitespace(e.getText()));
            if (bytes == null) {
                throw new XmlException("bad base64 in <data>");
            }
            return bytes;
        }
        if ("date".equals(n)) {
            return parseDate(e.getText().trim());
        }
        throw new XmlException("unknown property list element <" + n + ">");
    }

    /// Parses the one form plists write, `yyyy-MM-ddTHH:mm:ssZ`, as UTC.
    private static Date parseDate(String s) throws XmlException {
        if (s.length() != 20 || s.charAt(4) != '-' || s.charAt(7) != '-' || s.charAt(10) != 'T'
                || s.charAt(13) != ':' || s.charAt(16) != ':' || s.charAt(19) != 'Z') {
            throw new XmlException("bad <date> " + s);
        }
        int year = digits(s, 0, 4);
        int month = digits(s, 5, 2);
        int day = digits(s, 8, 2);
        int hour = digits(s, 11, 2);
        int minute = digits(s, 14, 2);
        int second = digits(s, 17, 2);
        if (year < 0 || month < 1 || month > 12 || day < 1 || day > 31
                || hour < 0 || hour > 23 || minute < 0 || minute > 59 || second < 0 || second > 60) {
            throw new XmlException("bad <date> " + s);
        }
        boolean leap = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0);
        int length = month == 2 ? (leap ? 29 : 28)
                : month == 4 || month == 6 || month == 9 || month == 11 ? 30 : 31;
        if (day > length) {
            throw new XmlException("bad <date> " + s);
        }
        // Days from the civil calendar (H. Hinnant's algorithm), proleptic Gregorian.
        // y is -1 for January and February of year 0, so the era floors.
        int y = month <= 2 ? year - 1 : year;
        int era = (y >= 0 ? y : y - 399) / 400;
        int yoe = y - era * 400;
        int mp = (month + 9) % 12;
        int doy = (153 * mp + 2) / 5 + day - 1;
        int doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        long days = (long) era * 146097 + doe - 719468;
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
