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
package com.codename1.svg.transcoder.parser;

/** Shared scanner state for SVG numeric lists. Trims units like px, pt, %. */
public final class NumberParser {

    private final String s;
    private int pos;

    public NumberParser(String s) {
        this.s = s == null ? "" : s;
    }

    public boolean hasMore() {
        skipWsAndCommas();
        return pos < s.length();
    }

    public float nextFloat() {
        skipWsAndCommas();
        int start = pos;
        int len = s.length();
        if (pos < len && (s.charAt(pos) == '+' || s.charAt(pos) == '-')) pos++;
        boolean sawDigit = false;
        while (pos < len && Character.isDigit(s.charAt(pos))) { pos++; sawDigit = true; }
        if (pos < len && s.charAt(pos) == '.') {
            pos++;
            while (pos < len && Character.isDigit(s.charAt(pos))) { pos++; sawDigit = true; }
        }
        if (pos < len && (s.charAt(pos) == 'e' || s.charAt(pos) == 'E')) {
            pos++;
            if (pos < len && (s.charAt(pos) == '+' || s.charAt(pos) == '-')) pos++;
            while (pos < len && Character.isDigit(s.charAt(pos))) pos++;
        }
        if (!sawDigit) {
            throw new IllegalArgumentException("Expected number at " + start + " in '" + s + "'");
        }
        String tok = s.substring(start, pos);
        // tolerate trailing unit
        while (pos < len) {
            char c = s.charAt(pos);
            if (Character.isLetter(c) || c == '%') pos++;
            else break;
        }
        return Float.parseFloat(tok);
    }

    /** Read a binary flag (0 or 1) -- used by SVG arc commands. */
    public int nextFlag() {
        skipWsAndCommas();
        if (pos >= s.length()) {
            throw new IllegalArgumentException("Expected flag in '" + s + "'");
        }
        char c = s.charAt(pos++);
        if (c != '0' && c != '1') {
            throw new IllegalArgumentException("Expected flag (0 or 1) at " + (pos - 1) + " in '" + s + "'");
        }
        return c - '0';
    }

    private void skipWsAndCommas() {
        int len = s.length();
        while (pos < len) {
            char c = s.charAt(pos);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == ',') pos++;
            else break;
        }
    }

    /**
     * How many CSS pixels one of {@code unit} is. A length in an absolute
     * unit is that many pixels, at 96 to the inch; any other suffix, the
     * pixel among them, leaves the number as it is written.
     */
    private static float pixelsPer(String unit) {
        if ("in".equals(unit)) return 96f;
        if ("cm".equals(unit)) return 96f / 2.54f;
        if ("mm".equals(unit)) return 96f / 25.4f;
        if ("Q".equals(unit)) return 96f / 101.6f;
        if ("pt".equals(unit)) return 96f / 72f;
        if ("pc".equals(unit)) return 16f;
        return 1f;
    }

    /** Parse a single float possibly suffixed with a unit; an absolute unit is converted to pixels. */
    public static float parseFloat(String value) {
        if (value == null) return 0f;
        String v = value.trim();
        if (v.isEmpty()) return 0f;
        int end = v.length();
        while (end > 0) {
            char c = v.charAt(end - 1);
            if (Character.isLetter(c) || c == '%') end--;
            else break;
        }
        return Float.parseFloat(v.substring(0, end)) * pixelsPer(v.substring(end));
    }
}
