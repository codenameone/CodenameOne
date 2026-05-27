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

    /** Parse a single float possibly suffixed with a unit. */
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
        return Float.parseFloat(v.substring(0, end));
    }
}
