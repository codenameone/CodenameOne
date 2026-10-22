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
package dart.convert;

import dart.core.DartList;
import dart.core.DartMap;
import dart.core.FormatException;
import dart.runtime.DartRuntime;
import dart.runtime.Funcs;

import java.util.Map;

/**
 * Dart's JSON codec ({@code dart:convert}): {@code jsonDecode} builds
 * {@link DartMap}/{@link DartList} trees with {@code Long} integers and {@code Double}
 * fractions, as Dart's decoder yields int and double; {@code jsonEncode} writes maps,
 * iterables, strings, numbers, booleans and null, and calls {@code toJson()} on anything
 * else (see {@link JsonEncodable}).
 */
public final class JsonCodec {

    private final Object reviver;
    private final Object toEncodable;

    public JsonCodec() {
        this(null, null);
    }

    public JsonCodec(Object reviver, Object toEncodable) {
        this.reviver = reviver;
        this.toEncodable = toEncodable;
    }

    public Object decode(String source) {
        return decode(source, reviver);
    }

    public Object decode(String source, Object reviver) {
        Object v = new Parser(source).parseTop();
        if (reviver != null) {
            v = revive(null, v, reviver);
        }
        return v;
    }

    public String encode(Object value) {
        return encode(value, toEncodable);
    }

    public String encode(Object value, Object toEncodable) {
        StringBuilder sb = new StringBuilder();
        write(sb, value, toEncodable, null, "", 0);
        return sb.toString();
    }

    public JsonEncoder encoder() {
        return new JsonEncoder(toEncodable);
    }

    public JsonDecoder decoder() {
        return new JsonDecoder(reviver);
    }

    @SuppressWarnings("unchecked")
    private static Object revive(Object key, Object value, Object reviver) {
        if (value instanceof DartMap) {
            DartMap<String, Object> m = (DartMap<String, Object>) value;
            for (Map.Entry<String, Object> e : m.entrySet()) {
                e.setValue(revive(e.getKey(), e.getValue(), reviver));
            }
        } else if (value instanceof DartList) {
            DartList<Object> l = (DartList<Object>) value;
            for (int i = 0; i < l.size(); i++) {
                l.set(i, revive(Long.valueOf(i), l.get(i), reviver));
            }
        }
        if (reviver instanceof Funcs.Func2) {
            return ((Funcs.Func2<Object, Object, Object>) reviver).call(key, value);
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    static void write(StringBuilder sb, Object v, Object toEncodable, String indent, String cur, int depth) {
        if (depth > 1000) {
            throw new JsonUnsupportedObjectError(v, "Cyclic or too deeply nested structure");
        }
        if (v == null) {
            sb.append("null");
        } else if (v instanceof String) {
            quote(sb, (String) v);
        } else if (v instanceof Boolean) {
            sb.append(((Boolean) v).booleanValue() ? "true" : "false");
        } else if (v instanceof Double || v instanceof Float) {
            double d = ((Number) v).doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                throw new JsonUnsupportedObjectError(v, "Unsupported number");
            }
            sb.append(DartRuntime.doubleStr(d));
        } else if (v instanceof Number) {
            sb.append(((Number) v).longValue());
        } else if (v instanceof Map) {
            Map<Object, Object> m = (Map<Object, Object>) v;
            if (m.isEmpty()) {
                sb.append("{}");
                return;
            }
            sb.append('{');
            String inner = indent == null ? null : cur + indent;
            boolean first = true;
            for (Map.Entry<Object, Object> e : m.entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                if (inner != null) {
                    sb.append('\n').append(inner);
                }
                if (!(e.getKey() instanceof String)) {
                    throw new JsonUnsupportedObjectError(v, "Map keys must be strings");
                }
                quote(sb, (String) e.getKey());
                sb.append(inner != null ? ": " : ":");
                write(sb, e.getValue(), toEncodable, indent, inner == null ? "" : inner, depth + 1);
            }
            if (inner != null) {
                sb.append('\n').append(cur);
            }
            sb.append('}');
        } else if (v instanceof Iterable) {
            String inner = indent == null ? null : cur + indent;
            sb.append('[');
            boolean first = true;
            for (Object o : (Iterable<Object>) v) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                if (inner != null) {
                    sb.append('\n').append(inner);
                }
                write(sb, o, toEncodable, indent, inner == null ? "" : inner, depth + 1);
            }
            if (inner != null && !first) {
                sb.append('\n').append(cur);
            }
            sb.append(']');
        } else {
            // Enums (Java enums here) go this way too: toEncodable or a toJson can
            // encode one, and only without either is it unsupported, as in Dart.
            Object converted;
            if (toEncodable instanceof Funcs.Func1) {
                converted = ((Funcs.Func1<Object, Object>) toEncodable).call(v);
            } else if (v instanceof JsonEncodable) {
                converted = ((JsonEncodable) v).toJson();
            } else {
                throw new JsonUnsupportedObjectError(v, "Converting object to an encodable object failed");
            }
            write(sb, converted, toEncodable, indent, cur, depth + 1);
        }
    }

    private static void quote(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                default:
                    if (c < 0x20) {
                        String hex = Integer.toHexString(c);
                        sb.append("\\u");
                        for (int k = hex.length(); k < 4; k++) {
                            sb.append('0');
                        }
                        sb.append(hex);
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }

    /** A strict RFC 8259 parser; a malformed document is a FormatException, as in Dart. */
    /// JSON numbers are ASCII digits only; Character.isDigit also took other scripts'
    /// decimal digits, which Long.parseLong then accepted, so "1" + ARABIC-INDIC TWO read as 12.
    static boolean isAsciiDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static final class Parser {
        private final String s;
        private int i;

        Parser(String s) {
            if (s == null) {
                throw new dart.core.TypeError("Null is not a subtype of String");
            }
            this.s = s;
        }

        Object parseTop() {
            skipWs();
            Object v = value();
            skipWs();
            if (i != s.length()) {
                fail("Unexpected character");
            }
            return v;
        }

        private void fail(String msg) {
            throw new FormatException(msg + " (at character " + (i + 1) + ")");
        }

        private void skipWs() {
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                    i++;
                } else {
                    break;
                }
            }
        }

        private Object value() {
            if (i >= s.length()) {
                fail("Unexpected end of input");
            }
            char c = s.charAt(i);
            switch (c) {
                case '{':
                    return object();
                case '[':
                    return array();
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
                    if (c == '-' || (c >= '0' && c <= '9')) {
                        return number();
                    }
                    fail("Unexpected character");
                    return null;
            }
        }

        private void literal(String word) {
            if (!s.startsWith(word, i)) {
                fail("Unexpected character");
            }
            i += word.length();
        }

        private DartMap<String, Object> object() {
            DartMap<String, Object> m = new DartMap<String, Object>();
            i++;
            skipWs();
            if (i < s.length() && s.charAt(i) == '}') {
                i++;
                return m;
            }
            while (true) {
                skipWs();
                if (i >= s.length() || s.charAt(i) != '"') {
                    fail("Expected a string key");
                }
                String k = string();
                skipWs();
                if (i >= s.length() || s.charAt(i) != ':') {
                    fail("Expected ':'");
                }
                i++;
                skipWs();
                m.put(k, value());
                skipWs();
                if (i >= s.length()) {
                    fail("Unexpected end of input");
                }
                char c = s.charAt(i++);
                if (c == '}') {
                    return m;
                }
                if (c != ',') {
                    i--;
                    fail("Expected ',' or '}'");
                }
            }
        }

        private DartList<Object> array() {
            DartList<Object> l = new DartList<Object>();
            i++;
            skipWs();
            if (i < s.length() && s.charAt(i) == ']') {
                i++;
                return l;
            }
            while (true) {
                skipWs();
                l.add(value());
                skipWs();
                if (i >= s.length()) {
                    fail("Unexpected end of input");
                }
                char c = s.charAt(i++);
                if (c == ']') {
                    return l;
                }
                if (c != ',') {
                    i--;
                    fail("Expected ',' or ']'");
                }
            }
        }

        private String string() {
            i++;
            StringBuilder sb = null;
            int start = i;
            while (true) {
                if (i >= s.length()) {
                    fail("Unterminated string");
                }
                char c = s.charAt(i);
                if (c == '"') {
                    String out = sb == null ? s.substring(start, i) : sb.append(s, start, i).toString();
                    i++;
                    return out;
                }
                if (c < 0x20) {
                    fail("Control character in string");
                }
                if (c == '\\') {
                    if (sb == null) {
                        sb = new StringBuilder();
                    }
                    sb.append(s, start, i);
                    i++;
                    if (i >= s.length()) {
                        fail("Unterminated string");
                    }
                    char e = s.charAt(i++);
                    switch (e) {
                        case '"':
                        case '\\':
                        case '/':
                            sb.append(e);
                            break;
                        case 'b':
                            sb.append('\b');
                            break;
                        case 'f':
                            sb.append('\f');
                            break;
                        case 'n':
                            sb.append('\n');
                            break;
                        case 'r':
                            sb.append('\r');
                            break;
                        case 't':
                            sb.append('\t');
                            break;
                        case 'u':
                            if (i + 4 > s.length()) {
                                fail("Invalid unicode escape");
                            }
                            int code = 0;
                            for (int k = 0; k < 4; k++) {
                                int d = Character.digit(s.charAt(i + k), 16);
                                if (d < 0) {
                                    fail("Invalid unicode escape");
                                }
                                code = code * 16 + d;
                            }
                            sb.append((char) code);
                            i += 4;
                            break;
                        default:
                            i--;
                            fail("Invalid escape");
                    }
                    start = i;
                } else {
                    i++;
                }
            }
        }

        private Object number() {
            int start = i;
            boolean fraction = false;
            if (s.charAt(i) == '-') {
                i++;
            }
            if (i >= s.length() || !isAsciiDigit(s.charAt(i))) {
                fail("Invalid number");
            }
            if (s.charAt(i) == '0') {
                i++;
            } else {
                while (i < s.length() && isAsciiDigit(s.charAt(i))) {
                    i++;
                }
            }
            if (i < s.length() && s.charAt(i) == '.') {
                fraction = true;
                i++;
                if (i >= s.length() || !isAsciiDigit(s.charAt(i))) {
                    fail("Invalid number");
                }
                while (i < s.length() && isAsciiDigit(s.charAt(i))) {
                    i++;
                }
            }
            if (i < s.length() && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) {
                fraction = true;
                i++;
                if (i < s.length() && (s.charAt(i) == '+' || s.charAt(i) == '-')) {
                    i++;
                }
                if (i >= s.length() || !isAsciiDigit(s.charAt(i))) {
                    fail("Invalid number");
                }
                while (i < s.length() && isAsciiDigit(s.charAt(i))) {
                    i++;
                }
            }
            String text = s.substring(start, i);
            if (!fraction) {
                // Dart's decoder yields an int when the literal fits, else a double.
                if (text.length() < 19) {
                    return Long.valueOf(Long.parseLong(text));
                }
                try {
                    return Long.valueOf(Long.parseLong(text));
                } catch (NumberFormatException overflow) {
                    return Double.valueOf(Double.parseDouble(text));
                }
            }
            return Double.valueOf(Double.parseDouble(text));
        }
    }
}
