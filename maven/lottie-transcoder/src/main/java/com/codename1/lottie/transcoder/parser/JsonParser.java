/*
 * Copyright (c) 2025, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 */
package com.codename1.lottie.transcoder.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal recursive-descent JSON parser. Returns nested {@link Map} /
 * {@link List} / {@link Double} / {@link String} / {@link Boolean} / null.
 * No external dependency -- keeps the transcoder consistent with the SVG
 * transcoder's "no Batik / no Jackson" stance.
 */
public final class JsonParser {

    private final String text;
    private int pos;

    private JsonParser(String text) {
        this.text = text;
        this.pos = 0;
    }

    public static Object parse(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        Reader r = new InputStreamReader(in, "UTF-8");
        char[] buf = new char[4096];
        int n;
        while ((n = r.read(buf)) > 0) {
            sb.append(buf, 0, n);
        }
        return parse(sb.toString());
    }

    public static Object parse(String text) {
        JsonParser p = new JsonParser(text);
        p.skipWhite();
        Object v = p.readValue();
        p.skipWhite();
        return v;
    }

    private Object readValue() {
        skipWhite();
        if (pos >= text.length()) {
            throw err("unexpected end of input");
        }
        char c = text.charAt(pos);
        if (c == '{') return readObject();
        if (c == '[') return readArray();
        if (c == '"') return readString();
        if (c == 't' || c == 'f') return readBool();
        if (c == 'n') return readNull();
        if (c == '-' || (c >= '0' && c <= '9')) return readNumber();
        throw err("unexpected character '" + c + "'");
    }

    private Map<String, Object> readObject() {
        expect('{');
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        skipWhite();
        if (peek() == '}') { pos++; return out; }
        while (true) {
            skipWhite();
            String key = readString();
            skipWhite();
            expect(':');
            Object v = readValue();
            out.put(key, v);
            skipWhite();
            char c = peek();
            if (c == ',') { pos++; continue; }
            if (c == '}') { pos++; return out; }
            throw err("expected ',' or '}'");
        }
    }

    private List<Object> readArray() {
        expect('[');
        List<Object> out = new ArrayList<Object>();
        skipWhite();
        if (peek() == ']') { pos++; return out; }
        while (true) {
            Object v = readValue();
            out.add(v);
            skipWhite();
            char c = peek();
            if (c == ',') { pos++; continue; }
            if (c == ']') { pos++; return out; }
            throw err("expected ',' or ']'");
        }
    }

    private String readString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (pos < text.length()) {
            char c = text.charAt(pos++);
            if (c == '"') return sb.toString();
            if (c == '\\') {
                if (pos >= text.length()) throw err("bad escape");
                char e = text.charAt(pos++);
                switch (e) {
                    case '"':  sb.append('"');  break;
                    case '\\': sb.append('\\'); break;
                    case '/':  sb.append('/');  break;
                    case 'b':  sb.append('\b'); break;
                    case 'f':  sb.append('\f'); break;
                    case 'n':  sb.append('\n'); break;
                    case 'r':  sb.append('\r'); break;
                    case 't':  sb.append('\t'); break;
                    case 'u':
                        if (pos + 4 > text.length()) throw err("bad \\u escape");
                        sb.append((char) Integer.parseInt(text.substring(pos, pos + 4), 16));
                        pos += 4;
                        break;
                    default: throw err("bad escape \\" + e);
                }
            } else {
                sb.append(c);
            }
        }
        throw err("unterminated string");
    }

    private Double readNumber() {
        int start = pos;
        if (peek() == '-') pos++;
        while (pos < text.length()) {
            char c = text.charAt(pos);
            if ((c >= '0' && c <= '9') || c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') {
                pos++;
            } else break;
        }
        return Double.valueOf(Double.parseDouble(text.substring(start, pos)));
    }

    private Boolean readBool() {
        if (text.startsWith("true", pos)) { pos += 4; return Boolean.TRUE; }
        if (text.startsWith("false", pos)) { pos += 5; return Boolean.FALSE; }
        throw err("expected boolean");
    }

    private Object readNull() {
        if (text.startsWith("null", pos)) { pos += 4; return null; }
        throw err("expected null");
    }

    private void skipWhite() {
        while (pos < text.length()) {
            char c = text.charAt(pos);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                pos++;
            } else break;
        }
    }

    private void expect(char c) {
        if (pos >= text.length() || text.charAt(pos) != c) {
            throw err("expected '" + c + "'");
        }
        pos++;
    }

    private char peek() {
        if (pos >= text.length()) throw err("unexpected end of input");
        return text.charAt(pos);
    }

    private RuntimeException err(String msg) {
        int line = 1;
        int col = 1;
        for (int i = 0; i < pos && i < text.length(); i++) {
            if (text.charAt(i) == '\n') { line++; col = 1; } else col++;
        }
        return new IllegalArgumentException(msg + " at line " + line + " col " + col);
    }

    /** Static helpers so callers do not have to cast on every property read. */
    public static Map<String, Object> asMap(Object o) {
        if (o == null) return null;
        return (Map<String, Object>) o;
    }

    public static List<Object> asList(Object o) {
        if (o == null) return null;
        return (List<Object>) o;
    }

    public static double asDouble(Object o, double dflt) {
        if (o instanceof Number) return ((Number) o).doubleValue();
        return dflt;
    }

    public static int asInt(Object o, int dflt) {
        if (o instanceof Number) return ((Number) o).intValue();
        return dflt;
    }

    public static String asString(Object o, String dflt) {
        if (o instanceof String) return (String) o;
        return dflt;
    }

    public static boolean asBoolean(Object o, boolean dflt) {
        if (o instanceof Boolean) return ((Boolean) o).booleanValue();
        if (o instanceof Number) return ((Number) o).intValue() != 0;
        return dflt;
    }
}
