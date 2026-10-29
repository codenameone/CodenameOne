/*
Copyright (c) 2007, Sun Microsystems, Inc.
 
All rights reserved.
 
Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
 
 * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in
      the documentation and/or other materials provided with the
      distribution.
 * Neither the name of Sun Microsystems, Inc. nor the names of its
      contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.
 
THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

Derivative Revision History:

2012-03 - Pulled this code from Sun's JavaME JSON impl, JSONObject
    and moved to separate utility class.
2026-10 - Moved into com.codename1.io beside JSONParser, which writes
    through it, so the backend shares one JSON writer with the client.
 */
package com.codename1.io;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/// The indented writer behind [JSONParser#mapToJson(Map)] and
/// [JSONParser#listToJson(List)], and so behind `com.codename1.processing.Result`.
/// It lives here rather than in `processing` so the server can share it: that
/// package drags the XML DOM into every server binary.
@com.codename1.impl.SharedWithBackend
final class PrettyPrinter {
    private PrettyPrinter() { // NOPMD UnnecessaryConstructor - a utility class, as UseUtilityClass asks
    }

    static String print(Map<?, ?> map) {
        if (map == null) {
            throw new IllegalArgumentException("content cannot be null");
        }
        if (map.containsKey("ROOT")) {
            Object root = map.get("ROOT");
            if (!(root instanceof Map)) {
                throw new ClassCastException("ROOT is not a map");
            }
            return printMap((Map<?, ?>) root, 0);
        }
        return printMap(map, 0);
    }

    private static void indent(StringBuilder sb, int n) {
        for (int i = 0; i < n; i++) {
            sb.append(' ');
        }
    }

    private static String printMap(Map<?, ?> map, int indent) {
        int n = map.size();
        if (n == 0) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder("{");
        int newindent = indent + 2;
        if (n == 1) {
            Map.Entry<?, ?> e = map.entrySet().iterator().next();
            sb.append(quote(e.getKey().toString()));
            sb.append(": ");
            sb.append(valueToString(e.getValue(), indent));
        } else {
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (sb.length() > 1) {
                    sb.append(",\n");
                } else {
                    sb.append('\n');
                }
                indent(sb, newindent);
                sb.append(quote(e.getKey().toString()));
                sb.append(": ");
                sb.append(valueToString(e.getValue(), newindent));
            }
            sb.append('\n');
            indent(sb, indent);
        }
        sb.append('}');
        return sb.toString();
    }

    static String printList(List<?> v, int indent) {
        int len = v.size();
        if (len == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        if (len == 1) {
            sb.append(valueToString(v.get(0), indent));
        } else {
            int newindent = indent + 2;
            sb.append('\n');
            for (int i = 0; i < len; i++) {
                if (i > 0) {
                    sb.append(",\n");
                }
                indent(sb, newindent);
                sb.append(valueToString(v.get(i), newindent));
            }
            sb.append('\n');
            indent(sb, indent);
        }
        sb.append(']');
        return sb.toString();
    }

    private static String valueToString(Object value, int indent) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            return quote((String) value);
        }
        if (value instanceof Float || value instanceof Double
                || value instanceof Byte || value instanceof Short
                || value instanceof Integer || value instanceof Long) {
            return trimNumber(value.toString());
        }
        if (value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Map) {
            return printMap((Map<?, ?>) value, indent);
        }
        if (value instanceof List) {
            return printList((List<?>) value, indent);
        }
        if (value instanceof Date) {
            return quote(formatDate((Date) value));
        }
        return quote(value.toString());
    }

    /// `yyyy-MM-dd'T'HH:mm:ss.SSS` in the default zone, as the
    /// `SimpleDateFormat` the old writer used formats it.
    private static String formatDate(Date d) {
        Calendar c = Calendar.getInstance(TimeZone.getDefault());
        c.setTime(d);
        StringBuilder sb = new StringBuilder();
        pad(sb, c.get(Calendar.YEAR), 4);
        sb.append('-');
        pad(sb, c.get(Calendar.MONTH) - Calendar.JANUARY + 1, 2);
        sb.append('-');
        pad(sb, c.get(Calendar.DAY_OF_MONTH), 2);
        sb.append('T');
        pad(sb, c.get(Calendar.HOUR_OF_DAY), 2);
        sb.append(':');
        pad(sb, c.get(Calendar.MINUTE), 2);
        sb.append(':');
        pad(sb, c.get(Calendar.SECOND), 2);
        sb.append('.');
        pad(sb, c.get(Calendar.MILLISECOND), 3);
        return sb.toString();
    }

    private static void pad(StringBuilder sb, int v, int size) {
        String s = String.valueOf(v);
        for (int i = s.length(); i < size; i++) {
            sb.append('0');
        }
        sb.append(s);
    }

    private static String trimNumber(String s) {
        if (s.indexOf('.') > 0 && s.indexOf('e') < 0 && s.indexOf('E') < 0) {
            while (s.endsWith("0")) {
                s = s.substring(0, s.length() - 1);
            }
            if (s.endsWith(".")) {
                s = s.substring(0, s.length() - 1);
            }
        }
        return s;
    }

    private static String quote(String string) {
        if (string == null || string.length() == 0) {
            return "\"\"";
        }
        char c = 0;
        int len = string.length();
        StringBuilder sb = new StringBuilder(len + 4);
        sb.append('"');
        for (int i = 0; i < len; i++) {
            char b = c;
            c = string.charAt(i);
            switch (c) {
                case '\\':
                case '"':
                    sb.append('\\');
                    sb.append(c);
                    break;
                case '/':
                    if (b == '<') {
                        sb.append('\\');
                    }
                    sb.append(c);
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                default:
                    if (c < ' ') {
                        String t = "000" + Integer.toHexString(c);
                        sb.append("\\u").append(t.substring(t.length() - 4));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
