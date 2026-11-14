/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.luau;

import java.util.ArrayList;

/// Splits Luau source into tokens, including the Luau additions: compound
/// assignment, `//`, `->`, `::`, and backtick interpolated strings.
final class Lexer {
    static final int EOF = 0;
    static final int NAME = 1;
    static final int NUMBER = 2;
    static final int STRING = 3;
    /// A backtick string with no `{`.
    static final int ISIMPLE = 4;
    /// Text of a backtick string up to its first `{`.
    static final int IBEGIN = 5;
    /// Text between a `}` and the next `{`.
    static final int IMID = 6;
    /// Text from the last `}` to the closing backtick.
    static final int IEND = 7;
    static final int OP = 8;
    static final int KEYWORD = 9;

    private static final String[] KEYWORDS = {
        "and", "break", "do", "else", "elseif", "end", "false", "for", "function",
        "if", "in", "local", "nil", "not", "or", "repeat", "return", "then",
        "true", "until", "while"
    };

    private static final String[] OPS3 = {"...", "..=", "//="};
    private static final String[] OPS2 = {
        "==", "~=", "<=", ">=", "..", "::", "->", "+=", "-=", "*=", "/=", "%=", "^=", "//"
    };
    private static final String OPS1 = "+-*/%^#&|<>=(){}[];:,.?@~";

    static final class Token {
        final int type;
        final String text;
        final double num;
        final int line;

        Token(int type, String text, double num, int line) {
            this.type = type;
            this.text = text;
            this.num = num;
            this.line = line;
        }

        boolean is(String s) {
            return (type == OP || type == KEYWORD) && text.equals(s);
        }

        @Override
        public String toString() {
            switch (type) {
                case EOF:
                    return "<eof>";
                case STRING:
                    return "string";
                case NUMBER:
                    return "number";
                default:
                    return "'" + text + "'";
            }
        }
    }

    private final String src;
    private final String chunk;
    private int pos;
    private int line = 1;
    private final ArrayList<Integer> interpDepth = new ArrayList<Integer>();

    Lexer(String src, String chunk) {
        this.src = src;
        this.chunk = chunk;
        if (src.startsWith("#")) {
            while (pos < src.length() && src.charAt(pos) != '\n') {
                pos++;
            }
        }
    }

    ArrayList<Token> tokenize() {
        ArrayList<Token> out = new ArrayList<Token>();
        while (true) {
            Token t = next();
            out.add(t);
            if (t.type == EOF) {
                return out;
            }
        }
    }

    private LuaError err(String msg) {
        return new LuaError(chunk + ":" + line + ": " + msg);
    }

    private char peek(int off) {
        int p = pos + off;
        return p < src.length() ? src.charAt(p) : '\0';
    }

    private static boolean isNameStart(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == '_';
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private Token next() {
        skipSpace();
        if (pos >= src.length()) {
            return new Token(EOF, "<eof>", 0, line);
        }
        char c = src.charAt(pos);
        int startLine = line;
        if (isNameStart(c)) {
            int s = pos;
            while (pos < src.length() && (isNameStart(src.charAt(pos)) || isDigit(src.charAt(pos)))) {
                pos++;
            }
            String w = src.substring(s, pos);
            for (String k : KEYWORDS) {
                if (k.equals(w)) {
                    return new Token(KEYWORD, w, 0, startLine);
                }
            }
            return new Token(NAME, w, 0, startLine);
        }
        if (isDigit(c) || c == '.' && isDigit(peek(1))) {
            return number();
        }
        if (c == '"' || c == '\'') {
            pos++;
            return new Token(STRING, quoted(c), 0, startLine);
        }
        if (c == '[' && (peek(1) == '[' || peek(1) == '=')) {
            int level = longBracketLevel();
            if (level >= 0) {
                return new Token(STRING, longString(level), 0, startLine);
            }
        }
        if (c == '`') {
            pos++;
            return interp(true, startLine);
        }
        if (c == '}' && !interpDepth.isEmpty() && interpDepth.get(interpDepth.size() - 1).intValue() == 0) {
            pos++;
            interpDepth.remove(interpDepth.size() - 1);
            return interp(false, startLine);
        }
        for (String op : OPS3) {
            if (src.startsWith(op, pos)) {
                pos += 3;
                return new Token(OP, op, 0, startLine);
            }
        }
        for (String op : OPS2) {
            if (src.startsWith(op, pos)) {
                pos += 2;
                return new Token(OP, op, 0, startLine);
            }
        }
        if (OPS1.indexOf(c) >= 0) {
            pos++;
            if (!interpDepth.isEmpty()) {
                int top = interpDepth.size() - 1;
                if (c == '{') {
                    interpDepth.set(top, Integer.valueOf(interpDepth.get(top).intValue() + 1));
                } else if (c == '}') {
                    interpDepth.set(top, Integer.valueOf(interpDepth.get(top).intValue() - 1));
                }
            }
            return new Token(OP, String.valueOf(c), 0, startLine);
        }
        throw err("unexpected symbol '" + c + "'");
    }

    private void skipSpace() {
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (c == '\n') {
                line++;
                pos++;
            } else if (c == ' ' || c == '\t' || c == '\r' || c == '\f' || c == 11) {
                pos++;
            } else if (c == '-' && peek(1) == '-') {
                pos += 2;
                if (peek(0) == '[') {
                    int level = longBracketLevel();
                    if (level >= 0) {
                        longString(level);
                        continue;
                    }
                }
                while (pos < src.length() && src.charAt(pos) != '\n') {
                    pos++;
                }
            } else {
                return;
            }
        }
    }

    /// At a `[`, the level of a long bracket (`[==[` is 2), consuming it;
    /// -1 and nothing consumed when it is not one.
    private int longBracketLevel() {
        int p = pos + 1;
        int level = 0;
        while (p < src.length() && src.charAt(p) == '=') {
            level++;
            p++;
        }
        if (p < src.length() && src.charAt(p) == '[') {
            pos = p + 1;
            return level;
        }
        return -1;
    }

    private String longString(int level) {
        if (peek(0) == '\r') {
            pos++;
        }
        if (peek(0) == '\n') {
            pos++;
            line++;
        }
        StringBuilder close = new StringBuilder("]");
        for (int i = 0; i < level; i++) {
            close.append('=');
        }
        close.append(']');
        int end = src.indexOf(close.toString(), pos);
        if (end < 0) {
            throw err("unfinished long string");
        }
        String s = src.substring(pos, end);
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\n') {
                line++;
            }
        }
        pos = end + close.length();
        return s;
    }

    private Token number() {
        int startLine = line;
        int s = pos;
        if (peek(0) == '0' && (peek(1) == 'x' || peek(1) == 'X')) {
            pos += 2;
            long v = 0;
            boolean any = false;
            while (pos < src.length()) {
                char c = src.charAt(pos);
                int d = Character.digit(c, 16);
                if (c == '_') {
                    pos++;
                    continue;
                }
                if (d < 0) {
                    break;
                }
                v = v * 16 + d;
                any = true;
                pos++;
            }
            if (!any) {
                throw err("malformed number near '" + src.substring(s, pos) + "'");
            }
            return new Token(NUMBER, src.substring(s, pos), (double) v, startLine);
        }
        if (peek(0) == '0' && (peek(1) == 'b' || peek(1) == 'B')) {
            pos += 2;
            long v = 0;
            while (pos < src.length() && (peek(0) == '0' || peek(0) == '1' || peek(0) == '_')) {
                if (peek(0) != '_') {
                    v = v * 2 + (peek(0) - '0');
                }
                pos++;
            }
            return new Token(NUMBER, src.substring(s, pos), (double) v, startLine);
        }
        StringBuilder b = new StringBuilder();
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (isDigit(c) || c == '.') {
                b.append(c);
                pos++;
            } else if (c == '_') {
                pos++;
            } else if ((c == 'e' || c == 'E')) {
                b.append(c);
                pos++;
                if (peek(0) == '+' || peek(0) == '-') {
                    b.append(peek(0));
                    pos++;
                }
            } else {
                break;
            }
        }
        if (pos < src.length() && isNameStart(src.charAt(pos))) {
            throw err("malformed number near '" + src.substring(s, pos + 1) + "'");
        }
        double d;
        try {
            d = Double.parseDouble(b.toString());
        } catch (NumberFormatException e) {
            LuaError le = err("malformed number near '" + b + "'");
            le.initCause(e);
            throw le;
        }
        return new Token(NUMBER, b.toString(), d, startLine);
    }

    private String quoted(char q) {
        StringBuilder b = new StringBuilder();
        while (true) {
            if (pos >= src.length()) {
                throw err("unfinished string");
            }
            char c = src.charAt(pos++);
            if (c == q) {
                return b.toString();
            }
            if (c == '\n') {
                throw err("unfinished string");
            }
            if (c == '\\') {
                escape(b);
            } else {
                b.append(c);
            }
        }
    }

    private Token interp(boolean begin, int startLine) {
        StringBuilder b = new StringBuilder();
        while (true) {
            if (pos >= src.length()) {
                throw err("unfinished interpolated string");
            }
            char c = src.charAt(pos++);
            if (c == '`') {
                return new Token(begin ? ISIMPLE : IEND, b.toString(), 0, startLine);
            }
            if (c == '{') {
                if (peek(0) == '{') {
                    throw err("double braces are not permitted within interpolated strings");
                }
                interpDepth.add(Integer.valueOf(0));
                return new Token(begin ? IBEGIN : IMID, b.toString(), 0, startLine);
            }
            if (c == '\n') {
                throw err("unfinished interpolated string");
            }
            if (c == '\\') {
                escape(b);
            } else {
                b.append(c);
            }
        }
    }

    private void escape(StringBuilder b) {
        if (pos >= src.length()) {
            throw err("unfinished string");
        }
        char e = src.charAt(pos++);
        switch (e) {
            case 'n':
                b.append('\n');
                break;
            case 't':
                b.append('\t');
                break;
            case 'r':
                b.append('\r');
                break;
            case 'a':
                b.append((char) 7);
                break;
            case 'b':
                b.append('\b');
                break;
            case 'f':
                b.append('\f');
                break;
            case 'v':
                b.append((char) 11);
                break;
            case '\\':
            case '"':
            case '\'':
            case '`':
            case '{':
            case '}':
                b.append(e);
                break;
            case '\n':
                line++;
                b.append('\n');
                break;
            case 'x': {
                int v = 0;
                for (int i = 0; i < 2; i++) {
                    int d = Character.digit(peek(0), 16);
                    if (d < 0) {
                        throw err("invalid escape sequence");
                    }
                    v = v * 16 + d;
                    pos++;
                }
                b.append((char) v);
                break;
            }
            case 'z':
                while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
                    if (src.charAt(pos) == '\n') {
                        line++;
                    }
                    pos++;
                }
                break;
            case 'u': {
                if (peek(0) != '{') {
                    throw err("invalid escape sequence");
                }
                pos++;
                int v = 0;
                while (peek(0) != '}') {
                    int d = Character.digit(peek(0), 16);
                    if (d < 0) {
                        throw err("invalid escape sequence");
                    }
                    v = v * 16 + d;
                    pos++;
                }
                pos++;
                Utf8.encode(b, v);
                break;
            }
            default:
                if (isDigit(e)) {
                    int v = e - '0';
                    for (int i = 0; i < 2 && isDigit(peek(0)); i++) {
                        v = v * 10 + (src.charAt(pos++) - '0');
                    }
                    if (v > 255) {
                        throw err("invalid escape sequence");
                    }
                    b.append((char) v);
                } else {
                    throw err("invalid escape sequence '\\" + e + "'");
                }
        }
    }
}
