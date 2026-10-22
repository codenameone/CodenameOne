/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.skirmish.rules.forge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// The JSON a content pack is written in, read and written without reflection so
/// the app, the server and the tests share it. It is deliberately small: objects
/// keep their key order, numbers are whole (a pack has no use for a fraction and
/// the rules have no floating point), and a document is refused past fixed limits
/// on size and nesting, because packs arrive from strangers.
public final class Json {
    public static final int MAX_CHARS = 512 * 1024;
    public static final int MAX_DEPTH = 32;

    private final String src;
    private final boolean lenient;
    private int at;
    private int depth;

    private Json(String src, boolean lenient) {
        this.src = src;
        this.lenient = lenient;
    }

    /// Parses a document into maps (`LinkedHashMap`), lists, strings, `Long`s,
    /// `Boolean`s and null.
    public static Object parse(String text) {
        return parse(text, false, MAX_CHARS);
    }

    /// Parses JSON written by other tools -- a glTF file's header -- where numbers
    /// may have fractions. Those come back as their text, unconverted: nothing here
    /// does arithmetic with them, and the rules must never see a float.
    public static Object parseLenient(String text, int maxChars) {
        return parse(text, true, maxChars);
    }

    private static Object parse(String text, boolean lenient, int maxChars) {
        if (text == null) {
            throw new IllegalArgumentException("empty document");
        }
        if (text.length() > maxChars) {
            throw new IllegalArgumentException("document too large");
        }
        Json p = new Json(text, lenient);
        p.ws();
        Object v = p.value();
        p.ws();
        if (p.at != text.length()) {
            throw p.error("trailing characters");
        }
        return v;
    }

    private IllegalArgumentException error(String what) {
        return new IllegalArgumentException(what + " at " + at);
    }

    private void ws() {
        while (at < src.length()) {
            char c = src.charAt(at);
            if (c != ' ' && c != '\n' && c != '\r' && c != '\t') {
                return;
            }
            at++;
        }
    }

    private char peek() {
        if (at >= src.length()) {
            throw error("unexpected end");
        }
        return src.charAt(at);
    }

    private Object value() {
        char c = peek();
        if (c == '{') {
            return object();
        }
        if (c == '[') {
            return array();
        }
        if (c == '"') {
            return string();
        }
        if (c == '-' || (c >= '0' && c <= '9')) {
            return number();
        }
        if (src.startsWith("true", at)) {
            at += 4;
            return Boolean.TRUE;
        }
        if (src.startsWith("false", at)) {
            at += 5;
            return Boolean.FALSE;
        }
        if (src.startsWith("null", at)) {
            at += 4;
            return null;
        }
        throw error("unexpected character");
    }

    private void enter() {
        if (++depth > MAX_DEPTH) {
            throw error("nested too deeply");
        }
    }

    private Map<String, Object> object() {
        enter();
        at++;
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        ws();
        if (peek() == '}') {
            at++;
            depth--;
            return out;
        }
        while (true) {
            ws();
            if (peek() != '"') {
                throw error("expected a key");
            }
            String key = string();
            ws();
            if (peek() != ':') {
                throw error("expected ':'");
            }
            at++;
            ws();
            if (out.containsKey(key)) {
                throw error("duplicate key " + key);
            }
            out.put(key, value());
            ws();
            char c = peek();
            at++;
            if (c == '}') {
                depth--;
                return out;
            }
            if (c != ',') {
                throw error("expected ',' or '}'");
            }
        }
    }

    private List<Object> array() {
        enter();
        at++;
        List<Object> out = new ArrayList<Object>();
        ws();
        if (peek() == ']') {
            at++;
            depth--;
            return out;
        }
        while (true) {
            ws();
            out.add(value());
            ws();
            char c = peek();
            at++;
            if (c == ']') {
                depth--;
                return out;
            }
            if (c != ',') {
                throw error("expected ',' or ']'");
            }
        }
    }

    private String string() {
        at++;
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = peek();
            at++;
            if (c == '"') {
                return sb.toString();
            }
            if (c < 0x20) {
                throw error("control character in string");
            }
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            char e = peek();
            at++;
            switch (e) {
                case '"': sb.append('"'); break;
                case '\\': sb.append('\\'); break;
                case '/': sb.append('/'); break;
                case 'n': sb.append('\n'); break;
                case 't': sb.append('\t'); break;
                case 'r': sb.append('\r'); break;
                case 'b': sb.append('\b'); break;
                case 'f': sb.append('\f'); break;
                case 'u':
                    if (at + 4 > src.length()) {
                        throw error("short escape");
                    }
                    sb.append((char) hex4(src.substring(at, at + 4)));
                    at += 4;
                    break;
                default:
                    throw error("bad escape");
            }
        }
    }

    private int hex4(String h) {
        int v = 0;
        for (int i = 0; i < 4; i++) {
            char c = h.charAt(i);
            int d;
            if (c >= '0' && c <= '9') {
                d = c - '0';
            } else if (c >= 'a' && c <= 'f') {
                d = c - 'a' + 10;
            } else if (c >= 'A' && c <= 'F') {
                d = c - 'A' + 10;
            } else {
                throw error("bad escape");
            }
            v = v * 16 + d;
        }
        return v;
    }

    private Object number() {
        int start = at;
        if (peek() == '-') {
            at++;
        }
        while (at < src.length() && src.charAt(at) >= '0' && src.charAt(at) <= '9') {
            at++;
        }
        if (at < src.length()) {
            char c = src.charAt(at);
            if (c == '.' || c == 'e' || c == 'E') {
                if (!lenient) {
                    throw error("only whole numbers are allowed");
                }
                while (at < src.length() && "0123456789.eE+-".indexOf(src.charAt(at)) >= 0) {
                    at++;
                }
                return src.substring(start, at);
            }
        }
        String digits = src.substring(start, at);
        if (digits.length() == 0 || "-".equals(digits) || digits.length() > 18) {
            throw error("bad number");
        }
        return Long.valueOf(Long.parseLong(digits));
    }

    // ------------------------------------------------------------------ writing

    /// Writes `v` compactly, keys in their map's order. The same pack always
    /// writes the same text, which is what its content hash is taken over.
    public static String write(Object v) {
        StringBuilder sb = new StringBuilder();
        write(sb, v);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static void write(StringBuilder sb, Object v) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof String) {
            quote(sb, (String) v);
        } else if (v instanceof Boolean) {
            sb.append(((Boolean) v).booleanValue() ? "true" : "false");
        } else if (v instanceof Long || v instanceof Integer) {
            sb.append(((Number) v).longValue());
        } else if (v instanceof Map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<String, Object> e : ((Map<String, Object>) v).entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                quote(sb, e.getKey());
                sb.append(':');
                write(sb, e.getValue());
            }
            sb.append('}');
        } else if (v instanceof List) {
            sb.append('[');
            List<Object> l = (List<Object>) v;
            for (int i = 0; i < l.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                write(sb, l.get(i));
            }
            sb.append(']');
        } else {
            throw new IllegalArgumentException("cannot write " + v.getClass().getName());
        }
    }

    private static void quote(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c < 0x20 || c > 0x7e) {
                // Everything outside printable ASCII is escaped, so the text --
                // and the hash taken over it -- is the same on every platform.
                String h = Integer.toHexString(c);
                sb.append("\\u");
                for (int k = h.length(); k < 4; k++) {
                    sb.append('0');
                }
                sb.append(h);
            } else {
                sb.append(c);
            }
        }
        sb.append('"');
    }
}
