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
package com.codename1.tools.javac;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Splits Java source into tokens. Comments and whitespace are dropped. */
final class Lexer {
    private static final Map<String, Token.Kind> KEYWORDS = new HashMap<String, Token.Kind>();
    private static final String[] OPERATORS = {
        ">>>=", "<<=", ">>=", ">>>", "...", "::", "->", "==", "<=", ">=", "!=", "&&", "||", "++", "--",
        "+=", "-=", "*=", "/=", "&=", "|=", "^=", "%=", "<<", ">>",
        "(", ")", "{", "}", "[", "]", ";", ",", ".", "@", "=", ">", "<", "!", "~", "?", ":",
        "+", "-", "*", "/", "&", "|", "^", "%"
    };
    private static final Token.Kind[] OPERATOR_KINDS = {
        Token.Kind.GTGTGTEQ, Token.Kind.LTLTEQ, Token.Kind.GTGTEQ, Token.Kind.GTGTGT, Token.Kind.ELLIPSIS,
        Token.Kind.COLONCOLON, Token.Kind.ARROW, Token.Kind.EQEQ, Token.Kind.LTEQ, Token.Kind.GTEQ, Token.Kind.BANGEQ,
        Token.Kind.AMPAMP, Token.Kind.BARBAR, Token.Kind.PLUSPLUS, Token.Kind.SUBSUB,
        Token.Kind.PLUSEQ, Token.Kind.SUBEQ, Token.Kind.STAREQ, Token.Kind.SLASHEQ, Token.Kind.AMPEQ, Token.Kind.BAREQ,
        Token.Kind.CARETEQ, Token.Kind.PERCENTEQ, Token.Kind.LTLT, Token.Kind.GTGT,
        Token.Kind.LPAREN, Token.Kind.RPAREN, Token.Kind.LBRACE, Token.Kind.RBRACE, Token.Kind.LBRACKET,
        Token.Kind.RBRACKET, Token.Kind.SEMI, Token.Kind.COMMA, Token.Kind.DOT, Token.Kind.AT, Token.Kind.EQ,
        Token.Kind.GT, Token.Kind.LT, Token.Kind.BANG, Token.Kind.TILDE, Token.Kind.QUES, Token.Kind.COLON,
        Token.Kind.PLUS, Token.Kind.SUB, Token.Kind.STAR, Token.Kind.SLASH, Token.Kind.AMP, Token.Kind.BAR,
        Token.Kind.CARET, Token.Kind.PERCENT
    };

    static {
        String[] words = {"abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float", "for", "goto",
            "if", "implements", "import", "instanceof", "int", "interface", "long", "native", "new", "package", "private",
            "protected", "public", "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
            "throw", "throws", "transient", "try", "void", "volatile", "while", "true", "false", "null"};
        Token.Kind[] kinds = {Token.Kind.ABSTRACT, Token.Kind.ASSERT, Token.Kind.BOOLEAN, Token.Kind.BREAK,
            Token.Kind.BYTE, Token.Kind.CASE, Token.Kind.CATCH, Token.Kind.CHAR, Token.Kind.CLASS, Token.Kind.CONST,
            Token.Kind.CONTINUE, Token.Kind.DEFAULT, Token.Kind.DO, Token.Kind.DOUBLE, Token.Kind.ELSE, Token.Kind.ENUM,
            Token.Kind.EXTENDS, Token.Kind.FINAL, Token.Kind.FINALLY, Token.Kind.FLOAT, Token.Kind.FOR, Token.Kind.GOTO,
            Token.Kind.IF, Token.Kind.IMPLEMENTS, Token.Kind.IMPORT, Token.Kind.INSTANCEOF, Token.Kind.INT,
            Token.Kind.INTERFACE, Token.Kind.LONG, Token.Kind.NATIVE, Token.Kind.NEW, Token.Kind.PACKAGE,
            Token.Kind.PRIVATE, Token.Kind.PROTECTED, Token.Kind.PUBLIC, Token.Kind.RETURN, Token.Kind.SHORT,
            Token.Kind.STATIC, Token.Kind.STRICTFP, Token.Kind.SUPER, Token.Kind.SWITCH, Token.Kind.SYNCHRONIZED,
            Token.Kind.THIS, Token.Kind.THROW, Token.Kind.THROWS, Token.Kind.TRANSIENT, Token.Kind.TRY, Token.Kind.VOID,
            Token.Kind.VOLATILE, Token.Kind.WHILE, Token.Kind.TRUE, Token.Kind.FALSE, Token.Kind.NULL};
        for (int i = 0; i < words.length; i++) {
            KEYWORDS.put(words[i], kinds[i]);
        }
    }

    private final Source source;
    private final String s;
    private final Log log;
    private int i;

    Lexer(Source source, Log log) {
        this.source = source;
        this.s = translateUnicodeEscapes(source.text);
        this.log = log;
    }

    /** Applies the \\uXXXX pass Java performs before lexing (an odd run of backslashes only). */
    static String translateUnicodeEscapes(String text) {
        if (text.indexOf("\\u") < 0) {
            return text;
        }
        StringBuilder b = new StringBuilder(text.length());
        int n = text.length();
        int i = 0;
        while (i < n) {
            char c = text.charAt(i);
            if (c == '\\') {
                int run = 0;
                while (i + run < n && text.charAt(i + run) == '\\') {
                    run++;
                }
                if ((run & 1) == 1 && i + run < n && text.charAt(i + run) == 'u') {
                    b.append(text, i, i + run - 1);
                    int j = i + run;
                    while (j < n && text.charAt(j) == 'u') {
                        j++;
                    }
                    if (j + 4 <= n) {
                        int value = 0;
                        boolean ok = true;
                        for (int k = 0; k < 4; k++) {
                            int d = Character.digit(text.charAt(j + k), 16);
                            if (d < 0) {
                                ok = false;
                                break;
                            }
                            value = value * 16 + d;
                        }
                        if (ok) {
                            b.append((char) value);
                            i = j + 4;
                            continue;
                        }
                    }
                    b.append(text, i + run - 1, j);
                    i = j;
                    continue;
                }
                b.append(text, i, i + run);
                i += run;
                continue;
            }
            b.append(c);
            i++;
        }
        return b.toString();
    }

    List<Token> tokenize() {
        List<Token> out = new ArrayList<Token>();
        while (true) {
            Token t = next();
            out.add(t);
            if (t.kind == Token.Kind.EOF) {
                return out;
            }
        }
    }

    private CompileError error(int pos, String message) {
        log.error(source, pos, message);
        return new CompileError(message);
    }

    private void skipTrivia() {
        int n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f') {
                i++;
            } else if (c == '/' && i + 1 < n && s.charAt(i + 1) == '/') {
                while (i < n && s.charAt(i) != '\n') {
                    i++;
                }
            } else if (c == '/' && i + 1 < n && s.charAt(i + 1) == '*') {
                int end = s.indexOf("*/", i + 2);
                if (end < 0) {
                    throw error(i, "unclosed comment");
                }
                i = end + 2;
            } else {
                return;
            }
        }
    }

    private Token next() {
        skipTrivia();
        int n = s.length();
        if (i >= n) {
            return new Token(Token.Kind.EOF, n, n, "<EOF>", null);
        }
        int start = i;
        char c = s.charAt(i);
        if (Character.isJavaIdentifierStart(c)) {
            i++;
            while (i < n && Character.isJavaIdentifierPart(s.charAt(i))) {
                i++;
            }
            String word = s.substring(start, i);
            Token.Kind kw = KEYWORDS.get(word);
            return new Token(kw == null ? Token.Kind.IDENT : kw, start, i, word, null);
        }
        if (c >= '0' && c <= '9' || c == '.' && i + 1 < n && s.charAt(i + 1) >= '0' && s.charAt(i + 1) <= '9') {
            return number(start);
        }
        if (c == '"') {
            if (s.startsWith("\"\"\"", i)) {
                return textBlock(start);
            }
            return string(start);
        }
        if (c == '\'') {
            i++;
            char value;
            if (i < n && s.charAt(i) == '\\') {
                value = escape();
            } else if (i < n && s.charAt(i) != '\'' && s.charAt(i) != '\n') {
                value = s.charAt(i++);
            } else {
                throw error(start, "empty character literal");
            }
            if (i >= n || s.charAt(i) != '\'') {
                throw error(start, "unclosed character literal");
            }
            i++;
            return new Token(Token.Kind.CHAR_LITERAL, start, i, s.substring(start, i), Character.valueOf(value));
        }
        for (int k = 0; k < OPERATORS.length; k++) {
            if (s.startsWith(OPERATORS[k], i)) {
                i += OPERATORS[k].length();
                return new Token(OPERATOR_KINDS[k], start, i, OPERATORS[k], null);
            }
        }
        i++;
        throw error(start, "illegal character '" + c + "'");
    }

    private char escape() {
        int[] cursor = {i};
        char c = escapeAt(s, cursor, i);
        i = cursor[0];
        return c;
    }

    private static String[] splitLines(String text) {
        List<String> lines = new ArrayList<String>();
        int from = 0;
        while (true) {
            int nl = text.indexOf('\n', from);
            if (nl < 0) {
                lines.add(text.substring(from));
                return lines.toArray(new String[lines.size()]);
            }
            lines.add(text.substring(from, nl));
            from = nl + 1;
        }
    }

    /** Decodes the escape at cursor[0] (a backslash) in {@code text}, advancing the cursor. */
    private char escapeAt(String text, int[] cursor, int errorPos) {
        int at = cursor[0] + 1;
        if (at >= text.length()) {
            throw error(errorPos, "illegal escape character");
        }
        char c = text.charAt(at++);
        cursor[0] = at;
        switch (c) {
            case 'b': return '\b';
            case 't': return '\t';
            case 'n': return '\n';
            case 'f': return '\f';
            case 'r': return '\r';
            case 's': return ' ';
            case '"': return '"';
            case '\'': return '\'';
            case '\\': return '\\';
            default:
                if (c >= '0' && c <= '7') {
                    int value = c - '0';
                    int max = c <= '3' ? 2 : 1;
                    int k = cursor[0];
                    for (int d = 0; d < max && k < text.length() && text.charAt(k) >= '0' && text.charAt(k) <= '7'; d++) {
                        value = value * 8 + (text.charAt(k++) - '0');
                    }
                    cursor[0] = k;
                    return (char) value;
                }
                throw error(errorPos, "illegal escape character");
        }
    }

    private Token string(int start) {
        i++;
        StringBuilder b = new StringBuilder();
        while (true) {
            if (i >= s.length() || s.charAt(i) == '\n') {
                throw error(start, "unclosed string literal");
            }
            char c = s.charAt(i);
            if (c == '"') {
                i++;
                break;
            }
            if (c == '\\') {
                b.append(escape());
            } else {
                b.append(c);
                i++;
            }
        }
        return new Token(Token.Kind.STRING_LITERAL, start, i, s.substring(start, i), b.toString());
    }

    /** A text block: incidental indentation stripped, trailing spaces removed, escapes applied last. */
    private Token textBlock(int start) {
        i += 3;
        while (i < s.length() && (s.charAt(i) == ' ' || s.charAt(i) == '\t' || s.charAt(i) == '\f')) {
            i++;
        }
        if (i < s.length() && s.charAt(i) == '\r') {
            i++;
        }
        if (i >= s.length() || s.charAt(i) != '\n') {
            throw error(start, "illegal text block open delimiter sequence, missing line terminator");
        }
        i++;
        int contentStart = i;
        int end = -1;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\\') {
                i += 2;
                continue;
            }
            if (s.startsWith("\"\"\"", i)) {
                end = i;
                i += 3;
                break;
            }
            i++;
        }
        if (end < 0) {
            throw error(start, "unclosed text block");
        }
        String raw = s.substring(contentStart, end).replace("\r\n", "\n");
        String[] lines = splitLines(raw);
        // The closing delimiter's line counts for indentation when it is blank.
        int indent = Integer.MAX_VALUE;
        for (int k = 0; k < lines.length; k++) {
            String line = lines[k];
            boolean last = k == lines.length - 1;
            if (!last && line.trim().isEmpty()) {
                continue;
            }
            int lead = 0;
            while (lead < line.length() && (line.charAt(lead) == ' ' || line.charAt(lead) == '\t')) {
                lead++;
            }
            if (last && lead == line.length()) {
                indent = Math.min(indent, lead);
            } else if (!last || lead < line.length()) {
                indent = Math.min(indent, lead);
            }
        }
        if (indent == Integer.MAX_VALUE) {
            indent = 0;
        }
        StringBuilder stripped = new StringBuilder();
        for (int k = 0; k < lines.length; k++) {
            String line = lines[k];
            boolean last = k == lines.length - 1;
            if (last && line.trim().isEmpty()) {
                break;
            }
            String body = line.length() >= indent ? line.substring(indent) : line.trim();
            int e = body.length();
            while (e > 0 && (body.charAt(e - 1) == ' ' || body.charAt(e - 1) == '\t')) {
                e--;
            }
            stripped.append(body, 0, e);
            if (!last) {
                stripped.append('\n');
            }
        }
        // Escapes are interpreted last, including \<newline> (joins lines) and \s.
        String text = stripped.toString();
        StringBuilder out = new StringBuilder(text.length());
        int k = 0;
        while (k < text.length()) {
            char c = text.charAt(k);
            if (c != '\\' || k + 1 >= text.length()) {
                out.append(c);
                k++;
                continue;
            }
            char d = text.charAt(k + 1);
            if (d == '\n') {
                k += 2;
                continue;
            }
            int[] cursor = {k};
            out.append(escapeAt(text, cursor, start));
            k = cursor[0];
        }
        return new Token(Token.Kind.STRING_LITERAL, start, i, s.substring(start, i), out.toString());
    }

    private Token number(int start) {
        int n = s.length();
        boolean hex = false;
        boolean bin = false;
        boolean floating = false;
        StringBuilder digits = new StringBuilder();
        if (s.charAt(i) == '0' && i + 1 < n && (s.charAt(i + 1) == 'x' || s.charAt(i + 1) == 'X')) {
            hex = true;
            i += 2;
        } else if (s.charAt(i) == '0' && i + 1 < n && (s.charAt(i + 1) == 'b' || s.charAt(i + 1) == 'B')) {
            bin = true;
            i += 2;
        }
        while (i < n) {
            char c = s.charAt(i);
            if (c == '_') {
                i++;
            } else if (Character.isDigit(c) || hex && (c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F')) {
                digits.append(c);
                i++;
            } else if (c == '.' && !bin && !(i + 1 < n && s.charAt(i + 1) == '.')
                    && (i + 1 >= n || !Character.isJavaIdentifierStart(s.charAt(i + 1)) || hex)) {
                floating = true;
                digits.append(c);
                i++;
            } else if (!hex && (c == 'e' || c == 'E') || hex && (c == 'p' || c == 'P')) {
                floating = true;
                digits.append(c);
                i++;
                if (i < n && (s.charAt(i) == '+' || s.charAt(i) == '-')) {
                    digits.append(s.charAt(i++));
                }
            } else {
                break;
            }
        }
        char suffix = i < n ? s.charAt(i) : 0;
        String text = digits.toString();
        try {
            if (suffix == 'f' || suffix == 'F') {
                i++;
                float v = hex ? (float) parseHexDouble(text) : Float.parseFloat(text);
                return new Token(Token.Kind.FLOAT_LITERAL, start, i, s.substring(start, i), Float.valueOf(v));
            }
            if (suffix == 'd' || suffix == 'D' || floating) {
                if (suffix == 'd' || suffix == 'D') {
                    i++;
                }
                double v = hex ? parseHexDouble(text) : Double.parseDouble(text);
                return new Token(Token.Kind.DOUBLE_LITERAL, start, i, s.substring(start, i), Double.valueOf(v));
            }
            boolean isLong = suffix == 'l' || suffix == 'L';
            if (isLong) {
                i++;
            }
            int radix = hex ? 16 : bin ? 2 : text.length() > 1 && text.charAt(0) == '0' ? 8 : 10;
            String body = radix == 8 ? text.substring(1) : text;
            if (body.isEmpty()) {
                body = "0";
            }
            long value = parseUnsigned(body, radix, isLong ? 64 : 32, start);
            if (isLong) {
                return new Token(Token.Kind.LONG_LITERAL, start, i, s.substring(start, i), Long.valueOf(value));
            }
            return new Token(Token.Kind.INT_LITERAL, start, i, s.substring(start, i), Integer.valueOf((int) value));
        } catch (NumberFormatException e) {
            throw error(start, "malformed number: " + s.substring(start, i));
        }
    }

    /**
     * Parses digits as an unsigned value of the given width. A decimal literal may
     * only reach 2^31 (int) or 2^63 (long) -- the magnitude of MIN_VALUE, legal only
     * under unary minus, which the parser checks; other radixes may fill the width.
     */
    private long parseUnsigned(String digits, int radix, int bits, int pos) {
        long value = 0;
        long limit = bits == 32 ? 0xFFFFFFFFL : -1L;
        for (int k = 0; k < digits.length(); k++) {
            int d = Character.digit(digits.charAt(k), radix);
            if (d < 0) {
                throw error(pos, "illegal digit in number");
            }
            if (bits == 64) {
                // value * radix + d must stay within 64 unsigned bits.
                if (unsignedCompare(value, unsignedMaxOver(radix)) > 0) {
                    throw error(pos, "integer number too large");
                }
                long next = value * radix + d;
                if (unsignedCompare(next, value * radix) < 0) {
                    throw error(pos, "integer number too large");
                }
                value = next;
            } else {
                value = value * radix + d;
                if (value > limit) {
                    throw error(pos, "integer number too large");
                }
            }
        }
        if (radix == 10) {
            if (bits == 32 && value > 0x80000000L || bits == 64 && unsignedCompare(value, 0x8000000000000000L) > 0) {
                throw error(pos, "integer number too large");
            }
        }
        return value;
    }

    private static int unsignedCompare(long a, long b) {
        long x = a + Long.MIN_VALUE;
        long y = b + Long.MIN_VALUE;
        return x < y ? -1 : x == y ? 0 : 1;
    }

    /** floor((2^64 - 1) / radix), computed without unsigned division. */
    private static long unsignedMaxOver(int radix) {
        long q = (Long.MAX_VALUE / radix) << 1;
        long r = -1L - q * radix;
        while (unsignedCompare(r, radix) >= 0) {
            q++;
            r -= radix;
        }
        return q;
    }

    private static double parseHexDouble(String text) {
        int p = text.indexOf('p');
        if (p < 0) {
            p = text.indexOf('P');
        }
        String mantissa = p < 0 ? text : text.substring(0, p);
        int exp = p < 0 ? 0 : Integer.parseInt(text.substring(p + 1).replace("+", ""));
        int dot = mantissa.indexOf('.');
        String whole = dot < 0 ? mantissa : mantissa.substring(0, dot);
        String frac = dot < 0 ? "" : mantissa.substring(dot + 1);
        double v = 0;
        for (int k = 0; k < whole.length(); k++) {
            v = v * 16 + Character.digit(whole.charAt(k), 16);
        }
        double scale = 1.0 / 16;
        for (int k = 0; k < frac.length(); k++) {
            v += Character.digit(frac.charAt(k), 16) * scale;
            scale /= 16;
        }
        return v * Math.pow(2, exp);
    }
}
