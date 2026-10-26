/*
 * Copyright (c) 2008-2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.crypto.encoding;

import com.codename1.crypto.UInt256;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// A small JSON reader and writer shared by the device and the server, for the JSON-RPC
/// framing and EIP-712 documents the crypto packages exchange.
///
/// Objects read as `LinkedHashMap<String, Object>` in document order, arrays as
/// `ArrayList<Object>`, strings as `String`, `true`/`false` as `Boolean`, `null` as `null`,
/// and numbers as `Long` when integral and in range, [UInt256] when integral, positive and
/// beyond a `long`, and `Double` otherwise -- a `double` would silently round a chain
/// quantity.
@com.codename1.impl.SharedWithBackend
public final class Json {
    private final String text;
    private int pos;

    private Json(String text) {
        this.text = text;
    }

    /// Parses a JSON document.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: for malformed input, naming the offset
    public static Object parse(String json) {
        if (json == null) {
            throw new IllegalArgumentException("null JSON");
        }
        Json p = new Json(json);
        Object out = p.value(0);
        p.skip();
        if (p.pos != json.length()) {
            throw p.error("trailing characters");
        }
        return out;
    }

    /// Writes a value as compact JSON. Accepts what [#parse] produces plus any `Number`,
    /// `byte[]` (written as a `0x` hex string), `Object[]` and any `Map`/`List`.
    public static String stringify(Object value) {
        StringBuilder out = new StringBuilder();
        write(out, value);
        return out.toString();
    }

    private static void write(StringBuilder out, Object value) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof String) {
            quote(out, (String) value);
        } else if (value instanceof Boolean || value instanceof UInt256 || value instanceof Long
                || value instanceof Integer || value instanceof Short || value instanceof Byte) {
            out.append(value.toString());
        } else if (value instanceof Number) {
            double d = ((Number) value).doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                throw new IllegalArgumentException("JSON has no NaN or infinity");
            }
            if (d == Math.floor(d) && Math.abs(d) < 1e15) {
                out.append((long) d);
            } else {
                out.append(d);
            }
        } else if (value instanceof byte[]) {
            quote(out, "0x" + Hex.encode((byte[]) value));
        } else if (value instanceof Map) {
            out.append('{');
            boolean first = true;
            Iterator it = ((Map) value).entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry e = (Map.Entry) it.next();
                if (!first) {
                    out.append(',');
                }
                first = false;
                quote(out, String.valueOf(e.getKey()));
                out.append(':');
                write(out, e.getValue());
            }
            out.append('}');
        } else if (value instanceof List) {
            List l = (List) value;
            out.append('[');
            for (int iter = 0; iter < l.size(); iter++) {
                if (iter > 0) {
                    out.append(',');
                }
                write(out, l.get(iter));
            }
            out.append(']');
        } else if (value instanceof Object[]) {
            Object[] a = (Object[]) value;
            out.append('[');
            for (int iter = 0; iter < a.length; iter++) {
                if (iter > 0) {
                    out.append(',');
                }
                write(out, a[iter]);
            }
            out.append(']');
        } else {
            throw new IllegalArgumentException("cannot write " + value.getClass().getName() + " as JSON");
        }
    }

    private static final String HEX = "0123456789abcdef";

    private static void quote(StringBuilder out, String s) {
        out.append('"');
        for (int iter = 0; iter < s.length(); iter++) {
            char c = s.charAt(iter);
            switch (c) {
                case '"':
                    out.append("\\\"");
                    break;
                case '\\':
                    out.append("\\\\");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        out.append("\\u00").append(HEX.charAt(c >> 4)).append(HEX.charAt(c & 15));
                    } else {
                        out.append(c);
                    }
            }
        }
        out.append('"');
    }

    private IllegalArgumentException error(String what) {
        return new IllegalArgumentException("JSON " + what + " at offset " + pos);
    }

    private void skip() {
        while (pos < text.length()) {
            char c = text.charAt(pos);
            if (c != ' ' && c != '\n' && c != '\r' && c != '\t') {
                return;
            }
            pos++;
        }
    }

    private Object value(int depth) {
        if (depth > 512) {
            throw error("nesting too deep");
        }
        skip();
        if (pos >= text.length()) {
            throw error("unexpected end");
        }
        char c = text.charAt(pos);
        switch (c) {
            case '{':
                return object(depth);
            case '[':
                return array(depth);
            case '"':
                return string();
            case 't':
                literal("true");
                return Boolean.TRUE;
            case 'f':
                literal("false");
                return Boolean.FALSE;
            case 'n':
                literal("null");
                return null;
            default:
                return number();
        }
    }

    private void literal(String word) {
        if (!text.startsWith(word, pos)) {
            throw error("unexpected token");
        }
        pos += word.length();
    }

    private Map<String, Object> object(int depth) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        pos++;
        skip();
        if (pos < text.length() && text.charAt(pos) == '}') {
            pos++;
            return out;
        }
        while (true) {
            skip();
            if (pos >= text.length() || text.charAt(pos) != '"') {
                throw error("expected a key");
            }
            String key = string();
            skip();
            expect(':');
            out.put(key, value(depth + 1));
            skip();
            if (pos < text.length() && text.charAt(pos) == ',') {
                pos++;
                continue;
            }
            expect('}');
            return out;
        }
    }

    private List<Object> array(int depth) {
        List<Object> out = new ArrayList<Object>();
        pos++;
        skip();
        if (pos < text.length() && text.charAt(pos) == ']') {
            pos++;
            return out;
        }
        while (true) {
            out.add(value(depth + 1));
            skip();
            if (pos < text.length() && text.charAt(pos) == ',') {
                pos++;
                continue;
            }
            expect(']');
            return out;
        }
    }

    private void expect(char c) {
        if (pos >= text.length() || text.charAt(pos) != c) {
            throw error("expected '" + c + "'");
        }
        pos++;
    }

    private String string() {
        pos++;
        StringBuilder out = new StringBuilder();
        while (true) {
            if (pos >= text.length()) {
                throw error("unterminated string");
            }
            char c = text.charAt(pos++);
            if (c == '"') {
                return out.toString();
            }
            if (c < 0x20) {
                throw error("control character in string");
            }
            if (c != '\\') {
                out.append(c);
                continue;
            }
            if (pos >= text.length()) {
                throw error("unterminated escape");
            }
            char e = text.charAt(pos++);
            switch (e) {
                case '"':
                case '\\':
                case '/':
                    out.append(e);
                    break;
                case 'b':
                    out.append('\b');
                    break;
                case 'f':
                    out.append('\f');
                    break;
                case 'n':
                    out.append('\n');
                    break;
                case 'r':
                    out.append('\r');
                    break;
                case 't':
                    out.append('\t');
                    break;
                case 'u':
                    if (pos + 4 > text.length()) {
                        throw error("short unicode escape");
                    }
                    int code = 0;
                    for (int iter = 0; iter < 4; iter++) {
                        int d = Character.digit(text.charAt(pos++), 16);
                        if (d < 0) {
                            throw error("bad unicode escape");
                        }
                        code = code * 16 + d;
                    }
                    out.append((char) code);
                    break;
                default:
                    throw error("bad escape");
            }
        }
    }

    private Object number() {
        int start = pos;
        boolean integral = true;
        if (pos < text.length() && text.charAt(pos) == '-') {
            pos++;
        }
        int digits = pos;
        while (pos < text.length()) {
            char c = text.charAt(pos);
            if (c >= '0' && c <= '9') {
                pos++;
            } else if (c == '.' || c == 'e' || c == 'E' || c == '+' || (c == '-' && pos > digits)) {
                integral = false;
                pos++;
            } else {
                break;
            }
        }
        if (pos == digits) {
            throw error("unexpected token");
        }
        String n = text.substring(start, pos);
        try {
            if (integral) {
                int len = n.length();
                if (len < 19 || (len == 19 && n.charAt(0) != '-' && n.compareTo("9223372036854775807") <= 0)
                        || (len == 20 && n.charAt(0) == '-' && n.compareTo("-9223372036854775808") <= 0)) {
                    return Long.valueOf(Long.parseLong(n));
                }
                if (n.charAt(0) != '-') {
                    return UInt256.parse(n, 10);
                }
            }
            return Double.valueOf(n);
        } catch (NumberFormatException bad) {
            throw error("bad number");
        }
    }
}
