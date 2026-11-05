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
package com.codename1.python.transpiler.parser;

import com.codename1.python.transpiler.api.PythonSyntaxError;

import java.util.ArrayList;
import java.util.List;

/// Python's tokenizer: logical lines, INDENT/DEDENT, implicit joining inside
/// brackets, explicit `\` continuation, and every string literal form
/// including PEP 701 f-strings (which may nest quotes of their own kind).
///
/// An f-string is returned as one STRING token whose `value` is its raw body;
/// the parser splits the replacement fields and lexes each expression again.
public final class Lexer {
    private static final String[] OPS3 = {"**=", "//=", ">>=", "<<=", "...", "!="};
    private static final String[] OPS2 = {"**", "//", ">>", "<<", "<=", ">=", "==", "!=", "->", "+=", "-=", "*=",
        "/=", "%=", "&=", "|=", "^=", "@=", ":="};
    private static final String OPS1 = "+-*/%@&|^~<>()[]{},:.;=";

    private final String src;
    private final String file;
    private final List<Token> out = new ArrayList<Token>();
    private final List<Integer> indents = new ArrayList<Integer>();
    private int pos;
    private int line = 1;
    private int lineStart;
    private int depth;

    public Lexer(String src, String file) {
        this.src = src.replace("\r\n", "\n").replace('\r', '\n');
        this.file = file;
        indents.add(Integer.valueOf(0));
    }

    private PythonSyntaxError error(String msg) {
        return new PythonSyntaxError(file, line, pos - lineStart + 1, msg);
    }

    private void add(Token.Kind k, String text, int startCol) {
        out.add(new Token(k, text, line, startCol, null, null));
    }

    public List<Token> tokenize() {
        boolean atLineStart = true;
        int n = src.length();
        if (n > 0 && src.charAt(0) == '\uFEFF') {
            pos = 1;
        }
        while (pos < n) {
            if (atLineStart && depth == 0) {
                int col = 0;
                int p = pos;
                while (p < n && (src.charAt(p) == ' ' || src.charAt(p) == '\t' || src.charAt(p) == '\f')) {
                    col = src.charAt(p) == '\t' ? (col / 8 + 1) * 8 : src.charAt(p) == '\f' ? 0 : col + 1;
                    p++;
                }
                if (p >= n) {
                    pos = p;
                    break;
                }
                char c = src.charAt(p);
                if (c == '\n' || c == '#' || c == '\\' && p + 1 < n && src.charAt(p + 1) == '\n') {
                    // Blank or comment-only line: no indentation change.
                    pos = p;
                    if (c == '#') {
                        while (pos < n && src.charAt(pos) != '\n') {
                            pos++;
                        }
                    }
                    if (pos < n && src.charAt(pos) == '\\') {
                        pos++;
                    }
                    if (pos < n) {
                        pos++;
                        line++;
                        lineStart = pos;
                    }
                    continue;
                }
                pos = p;
                int top = indents.get(indents.size() - 1).intValue();
                if (col > top) {
                    indents.add(Integer.valueOf(col));
                    add(Token.Kind.INDENT, "", 1);
                } else {
                    while (col < indents.get(indents.size() - 1).intValue()) {
                        indents.remove(indents.size() - 1);
                        add(Token.Kind.DEDENT, "", 1);
                    }
                    if (col != indents.get(indents.size() - 1).intValue()) {
                        throw error("unindent does not match any outer indentation level");
                    }
                }
                atLineStart = false;
            }
            char c = src.charAt(pos);
            int startCol = pos - lineStart + 1;
            if (c == '\n') {
                if (depth == 0) {
                    add(Token.Kind.NEWLINE, "", startCol);
                    atLineStart = true;
                }
                pos++;
                line++;
                lineStart = pos;
                continue;
            }
            if (c == ' ' || c == '\t' || c == '\f') {
                pos++;
                continue;
            }
            if (c == '#') {
                while (pos < n && src.charAt(pos) != '\n') {
                    pos++;
                }
                continue;
            }
            if (c == '\\') {
                if (pos + 1 < n && src.charAt(pos + 1) == '\n') {
                    pos += 2;
                    line++;
                    lineStart = pos;
                    continue;
                }
                throw error("unexpected character after line continuation character");
            }
            if (isIdStart(src.codePointAt(pos))) {
                int s = pos;
                while (pos < n && isIdPart(src.codePointAt(pos))) {
                    pos += Character.charCount(src.codePointAt(pos));
                }
                String word = src.substring(s, pos);
                if (pos < n && (src.charAt(pos) == '"' || src.charAt(pos) == '\'') && isTemplatePrefix(word)) {
                    throw error("t-strings are not supported yet");
                }
                if (pos < n && (src.charAt(pos) == '"' || src.charAt(pos) == '\'') && isPrefix(word)) {
                    string(word, s, startCol);
                    continue;
                }
                add(Token.Kind.NAME, word, startCol);
                continue;
            }
            if (c == '"' || c == '\'') {
                string("", pos, startCol);
                continue;
            }
            if (Character.isDigit(c) || c == '.' && pos + 1 < n && Character.isDigit(src.charAt(pos + 1))) {
                number(startCol);
                continue;
            }
            String op = null;
            for (int i = 0; i < OPS3.length && op == null; i++) {
                if (src.startsWith(OPS3[i], pos)) {
                    op = OPS3[i];
                }
            }
            for (int i = 0; i < OPS2.length && op == null; i++) {
                if (src.startsWith(OPS2[i], pos)) {
                    op = OPS2[i];
                }
            }
            if (op == null && OPS1.indexOf(c) >= 0) {
                op = String.valueOf(c);
            }
            if (op == null) {
                throw error("invalid character '" + c + "' (U+" + Integer.toHexString(c) + ")");
            }
            if ("([{".indexOf(op.charAt(0)) >= 0 && op.length() == 1) {
                depth++;
            } else if (")]}".indexOf(op.charAt(0)) >= 0 && op.length() == 1) {
                depth = Math.max(0, depth - 1);
            }
            pos += op.length();
            add(Token.Kind.OP, op, startCol);
        }
        if (!out.isEmpty() && out.get(out.size() - 1).kind != Token.Kind.NEWLINE
                && out.get(out.size() - 1).kind != Token.Kind.DEDENT) {
            add(Token.Kind.NEWLINE, "", 1);
        }
        while (indents.size() > 1) {
            indents.remove(indents.size() - 1);
            add(Token.Kind.DEDENT, "", 1);
        }
        add(Token.Kind.EOF, "", 1);
        return out;
    }

    /// Python identifiers are XID_Start / XID_Continue; Java's Unicode
    /// identifier classes are the same sets apart from the ignorable format
    /// characters, which Python rejects. Code points, not chars, so a
    /// supplementary-plane letter or variation selector is one character.
    private static boolean isIdStart(int c) {
        return c == '_' || Character.isUnicodeIdentifierStart(c);
    }

    private static boolean isIdPart(int c) {
        return c == '_' || (Character.isUnicodeIdentifierPart(c) && !Character.isIdentifierIgnorable(c));
    }

    private static boolean isTemplatePrefix(String w) {
        String l = w.toLowerCase(java.util.Locale.ROOT);
        return "t".equals(l) || "rt".equals(l) || "tr".equals(l);
    }

    private static boolean isPrefix(String w) {
        String l = w.toLowerCase(java.util.Locale.ROOT);
        return "r".equals(l) || "u".equals(l) || "b".equals(l) || "f".equals(l) || "rb".equals(l) || "br".equals(l)
                || "fr".equals(l) || "rf".equals(l);
    }

    private void number(int startCol) {
        int s = pos;
        int n = src.length();
        if (src.charAt(pos) == '0' && pos + 1 < n && "xXoObB".indexOf(src.charAt(pos + 1)) >= 0) {
            pos += 2;
            while (pos < n && (Character.isLetterOrDigit(src.charAt(pos)) || src.charAt(pos) == '_')) {
                pos++;
            }
        } else {
            while (pos < n && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '_')) {
                pos++;
            }
            if (pos < n && src.charAt(pos) == '.') {
                pos++;
                while (pos < n && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '_')) {
                    pos++;
                }
            }
            if (pos < n && (src.charAt(pos) == 'e' || src.charAt(pos) == 'E')) {
                int save = pos;
                pos++;
                if (pos < n && (src.charAt(pos) == '+' || src.charAt(pos) == '-')) {
                    pos++;
                }
                if (pos < n && Character.isDigit(src.charAt(pos))) {
                    while (pos < n && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '_')) {
                        pos++;
                    }
                } else {
                    pos = save;
                }
            }
            if (pos < n && (src.charAt(pos) == 'j' || src.charAt(pos) == 'J')) {
                pos++;
            }
        }
        out.add(new Token(Token.Kind.NUMBER, src.substring(s, pos), line, startCol, null, null));
    }

    private void string(String prefix, int start, int startCol) {
        String p = prefix.toLowerCase(java.util.Locale.ROOT);
        boolean raw = p.indexOf('r') >= 0;
        boolean fstr = p.indexOf('f') >= 0;
        int startLine = line;
        char q = src.charAt(pos);
        boolean triple = src.startsWith("" + q + q + q, pos);
        pos += triple ? 3 : 1;
        int bodyStart = pos;
        int n = src.length();
        int braces = 0;
        int parens = 0;
        // Bit k: the replacement field at nesting level k is in its format spec.
        long specLevels = 0;
        while (true) {
            if (pos >= n) {
                throw new PythonSyntaxError(file, startLine, startCol, "unterminated string literal");
            }
            char c = src.charAt(pos);
            if (fstr && braces > 0) {
                if (c == '{') {
                    braces++;
                    pos++;
                    continue;
                }
                if (c == '}') {
                    if (braces < 63) {
                        specLevels &= ~(1L << braces);
                    }
                    braces--;
                    pos++;
                    continue;
                }
                if (c == '(' || c == '[') {
                    parens++;
                } else if ((c == ')' || c == ']') && parens > 0) {
                    parens--;
                } else if (c == ':' && parens == 0 && braces < 63 && (specLevels & (1L << braces)) == 0
                        && (pos + 1 >= n || src.charAt(pos + 1) != '=')) {
                    specLevels |= 1L << braces;
                }
                if (braces < 63 && (specLevels & (1L << braces)) != 0) {
                    if (c == '\n') {
                        line++;
                        lineStart = pos + 1;
                    }
                    pos++;
                    continue;
                }
                if (c == '\'' || c == '"') {
                    skipNestedString();
                    continue;
                }
                if (c == '#') {
                    // PEP 701: a comment inside a multi-line replacement field.
                    while (pos < n && src.charAt(pos) != '\n') {
                        pos++;
                    }
                    continue;
                }
                if (c == '\n') {
                    line++;
                    lineStart = pos + 1;
                }
                pos++;
                continue;
            }
            if (c == '\\') {
                if (pos + 1 < n && src.charAt(pos + 1) == '\n') {
                    line++;
                    lineStart = pos + 2;
                }
                pos += 2;
                continue;
            }
            if (fstr && c == '{') {
                if (pos + 1 < n && src.charAt(pos + 1) == '{') {
                    pos += 2;
                    continue;
                }
                braces = 1;
                parens = 0;
                specLevels = 0;
                pos++;
                continue;
            }
            if (c == '\n') {
                if (!triple) {
                    throw new PythonSyntaxError(file, startLine, startCol, "unterminated string literal");
                }
                line++;
                lineStart = pos + 1;
                pos++;
                continue;
            }
            if (c == q && (!triple || src.startsWith("" + q + q + q, pos))) {
                break;
            }
            pos++;
        }
        String body = src.substring(bodyStart, pos);
        pos += triple ? 3 : 1;
        String value = fstr ? body : (raw ? body : decode(body, p.indexOf('b') >= 0, startLine, startCol));
        out.add(new Token(Token.Kind.STRING, src.substring(start, pos), startLine, startCol, value, p));
    }

    /// Skips a string literal nested inside an f-string replacement field.
    private void skipNestedString() {
        int n = src.length();
        int s = pos;
        int ps = s;
        while (ps > 0 && Character.isLetter(src.charAt(ps - 1))) {
            ps--;
        }
        boolean nestedF = src.substring(ps, s).toLowerCase(java.util.Locale.ROOT).indexOf('f') >= 0;
        char q = src.charAt(pos);
        boolean triple = src.startsWith("" + q + q + q, pos);
        pos += triple ? 3 : 1;
        int braces = 0;
        while (pos < n) {
            char c = src.charAt(pos);
            if (nestedF && braces > 0) {
                if (c == '{') {
                    braces++;
                } else if (c == '}') {
                    braces--;
                } else if (c == '\'' || c == '"') {
                    skipNestedString();
                    continue;
                }
                pos++;
                continue;
            }
            if (c == '\\') {
                pos += 2;
                continue;
            }
            if (nestedF && c == '{') {
                if (pos + 1 < n && src.charAt(pos + 1) == '{') {
                    pos += 2;
                    continue;
                }
                braces = 1;
                pos++;
                continue;
            }
            if (c == q && (!triple || src.startsWith("" + q + q + q, pos))) {
                pos += triple ? 3 : 1;
                return;
            }
            if (c == '\n') {
                line++;
                lineStart = pos + 1;
            }
            pos++;
        }
        throw error("unterminated string literal");
    }

    /// Decodes backslash escapes. Bytes literals keep each byte as a char.
    public static String decode(String body, boolean bytes, int line, int col) {
        if (body.indexOf('\\') < 0) {
            return body;
        }
        StringBuilder sb = new StringBuilder(body.length());
        int n = body.length();
        for (int i = 0; i < n; i++) {
            char c = body.charAt(i);
            if (c != '\\' || i + 1 >= n) {
                sb.append(c);
                continue;
            }
            char e = body.charAt(++i);
            switch (e) {
                case '\n':
                    break;
                case '\\':
                    sb.append('\\');
                    break;
                case '\'':
                    sb.append('\'');
                    break;
                case '"':
                    sb.append('"');
                    break;
                case 'a':
                    sb.append((char) 7);
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
                case 'v':
                    sb.append((char) 11);
                    break;
                case 'x':
                    sb.append((char) Integer.parseInt(body.substring(i + 1, i + 3), 16));
                    i += 2;
                    break;
                case 'u':
                    if (bytes) {
                        sb.append('\\').append(e);
                        break;
                    }
                    sb.append((char) Integer.parseInt(body.substring(i + 1, i + 5), 16));
                    i += 4;
                    break;
                case 'U':
                    if (bytes) {
                        sb.append('\\').append(e);
                        break;
                    }
                    sb.appendCodePoint(Integer.parseInt(body.substring(i + 1, i + 9), 16));
                    i += 8;
                    break;
                case 'N': {
                    if (bytes) {
                        sb.append('\\').append(e);
                        break;
                    }
                    int close = i + 1 < n && body.charAt(i + 1) == '{' ? body.indexOf('}', i + 2) : -1;
                    if (close < 0) {
                        throw new PythonSyntaxError("", line, col, "malformed \\N character escape");
                    }
                    int cp = UnicodeNames.lookup(body.substring(i + 2, close));
                    if (cp < 0) {
                        throw new PythonSyntaxError("", line, col, "unknown Unicode character name");
                    }
                    sb.appendCodePoint(cp);
                    i = close;
                    break;
                }
                default:
                    if (e >= '0' && e <= '7') {
                        int v = e - '0';
                        int k = 1;
                        while (k < 3 && i + 1 < n && body.charAt(i + 1) >= '0' && body.charAt(i + 1) <= '7') {
                            v = v * 8 + (body.charAt(++i) - '0');
                            k++;
                        }
                        sb.append((char) v);
                    } else {
                        sb.append('\\').append(e);
                    }
                    break;
            }
        }
        return sb.toString();
    }
}
