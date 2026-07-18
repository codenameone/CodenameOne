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

package com.codename1.ui.editor;

import java.util.ArrayList;
import java.util.List;

/// A small, allocation light, stateful lexer for the pure code editor. It tokenizes a single line at a
/// time given the lexer state carried over from the previous line, and returns the state to carry into
/// the next line. This makes rehighlighting incremental: after an edit only the changed line and any
/// following lines whose entry state changed need to be retokenized.
///
/// Only the spans that carry color (keyword, string, comment, number) are emitted; the gaps between them
/// are drawn in the default text color.
public class Tokenizer implements SyntaxHighlighter {
    /// Token kind: a language keyword.
    public static final int KEYWORD = SyntaxToken.KEYWORD;
    /// Token kind: a string or character literal.
    public static final int STRING = SyntaxToken.STRING;
    /// Token kind: a comment.
    public static final int COMMENT = SyntaxToken.COMMENT;
    /// Token kind: a numeric literal.
    public static final int NUMBER = SyntaxToken.NUMBER;
    /// Token kind: an XML/HTML tag or a declared type.
    public static final int TYPE = SyntaxToken.TYPE;
    /// Token kind: an attribute name or object property.
    public static final int PROPERTY = SyntaxToken.PROPERTY;

    /// Lexer state: normal code.
    public static final int STATE_NORMAL = 0;
    /// Lexer state: inside a block comment that opened on a previous line.
    public static final int STATE_BLOCK_COMMENT = 1;
    /// Lexer state: inside a backtick template string that opened on a previous line.
    public static final int STATE_TEMPLATE = 2;
    /// Lexer state: inside an XML/HTML comment.
    public static final int STATE_XML_COMMENT = 3;
    /// Lexer state: inside a Python triple-single-quoted string.
    public static final int STATE_TRIPLE_SINGLE = 4;
    /// Lexer state: inside a Python triple-double-quoted string.
    public static final int STATE_TRIPLE_DOUBLE = 5;
    /// Lexer state: inside a CSS declaration block.
    public static final int STATE_CSS_DECLARATION = 6;
    /// Lexer state: inside a CSS comment opened from a declaration block.
    public static final int STATE_CSS_COMMENT_DECLARATION = 7;

    private final LanguageDef def;

    /// Creates a tokenizer for the given language.
    public Tokenizer(LanguageDef def) {
        this.def = def;
    }

    private static boolean isIdentStart(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_' || c == '$';
    }

    private static boolean isIdentPart(char c) {
        return isIdentStart(c) || (c >= '0' && c <= '9');
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    /// Tokenizes one line.
    ///
    /// #### Parameters
    ///
    /// - `line`: the line text (without its trailing newline)
    ///
    /// - `startState`: the lexer state carried from the previous line
    ///
    /// #### Returns
    ///
    /// the colored spans and exit state
    @Override
    public SyntaxHighlightResult tokenize(String line, int startState) {
        List<SyntaxToken> tokens = new ArrayList<SyntaxToken>();
        if (def.isPlain()) {
            return new SyntaxHighlightResult(tokens, STATE_NORMAL);
        }
        if (def.getGrammar() == LanguageDef.XML_GRAMMAR) {
            return tokenizeXml(line, startState);
        }
        if (def.getGrammar() == LanguageDef.CSS_GRAMMAR) {
            return tokenizeCss(line, startState);
        }
        if (def.getGrammar() == LanguageDef.JSON_GRAMMAR) {
            return tokenizeJson(line);
        }
        if (def.getGrammar() == LanguageDef.PYTHON_GRAMMAR) {
            return tokenizePython(line, startState);
        }
        int n = line.length();
        int i = 0;
        int state = startState;

        if (state == STATE_BLOCK_COMMENT) {
            int end = indexOfClose(line, 0, "*/");
            if (end < 0) {
                if (n > 0) {
                    tokens.add(new SyntaxToken(0, n, COMMENT));
                }
                return new SyntaxHighlightResult(tokens, STATE_BLOCK_COMMENT);
            }
            tokens.add(new SyntaxToken(0, end + 2, COMMENT));
            i = end + 2;
            state = STATE_NORMAL;
        } else if (state == STATE_TEMPLATE) {
            int end = scanTemplate(line, 0);
            if (end < 0) {
                if (n > 0) {
                    tokens.add(new SyntaxToken(0, n, STRING));
                }
                return new SyntaxHighlightResult(tokens, STATE_TEMPLATE);
            }
            tokens.add(new SyntaxToken(0, end + 1, STRING));
            i = end + 1;
            state = STATE_NORMAL;
        }

        while (i < n) {
            char c = line.charAt(i);
            // line comment
            if (def.hasLineCommentSlash() && c == '/' && i + 1 < n && line.charAt(i + 1) == '/') {
                tokens.add(new SyntaxToken(i, n - i, COMMENT));
                break;
            }
            if (def.hasLineCommentHash() && c == '#') {
                tokens.add(new SyntaxToken(i, n - i, COMMENT));
                break;
            }
            // block comment
            if (def.hasBlockComment() && c == '/' && i + 1 < n && line.charAt(i + 1) == '*') {
                int end = indexOfClose(line, i + 2, "*/");
                if (end < 0) {
                    tokens.add(new SyntaxToken(i, n - i, COMMENT));
                    return new SyntaxHighlightResult(tokens, STATE_BLOCK_COMMENT);
                }
                tokens.add(new SyntaxToken(i, end + 2 - i, COMMENT));
                i = end + 2;
                continue;
            }
            // template string
            if (def.hasTemplateString() && c == '`') {
                int end = scanTemplate(line, i + 1);
                if (end < 0) {
                    tokens.add(new SyntaxToken(i, n - i, STRING));
                    return new SyntaxHighlightResult(tokens, STATE_TEMPLATE);
                }
                tokens.add(new SyntaxToken(i, end + 1 - i, STRING));
                i = end + 1;
                continue;
            }
            // string / char literal
            if (c == '"' || c == '\'') {
                int end = scanString(line, i + 1, c);
                int stop = end < 0 ? n : end + 1;
                tokens.add(new SyntaxToken(i, stop - i, STRING));
                i = stop;
                continue;
            }
            // number
            if (isDigit(c) || (c == '.' && i + 1 < n && isDigit(line.charAt(i + 1)))) {
                int j = scanNumber(line, i);
                tokens.add(new SyntaxToken(i, j - i, NUMBER));
                i = j;
                continue;
            }
            // identifier / keyword
            if (isIdentStart(c)) {
                int j = i + 1;
                while (j < n && isIdentPart(line.charAt(j))) {
                    j++;
                }
                String word = line.substring(i, j);
                if (def.isKeyword(word)) {
                    tokens.add(new SyntaxToken(i, j - i, KEYWORD));
                }
                i = j;
                continue;
            }
            i++;
        }
        return new SyntaxHighlightResult(tokens, state);
    }

    private static int scanNumber(String line, int start) {
        int n = line.length();
        int i = start;
        if (line.charAt(i) == '.') {
            i++;
            while (i < n && (isDigit(line.charAt(i)) || line.charAt(i) == '_')) {
                i++;
            }
        } else if (i + 1 < n && line.charAt(i) == '0'
                && (line.charAt(i + 1) == 'x' || line.charAt(i + 1) == 'X'
                || line.charAt(i + 1) == 'b' || line.charAt(i + 1) == 'B')) {
            boolean binary = line.charAt(i + 1) == 'b' || line.charAt(i + 1) == 'B';
            i += 2;
            while (i < n && ((binary ? line.charAt(i) == '0' || line.charAt(i) == '1'
                    : Character.digit(line.charAt(i), 16) >= 0) || line.charAt(i) == '_')) {
                i++;
            }
        } else {
            while (i < n && (isDigit(line.charAt(i)) || line.charAt(i) == '_')) {
                i++;
            }
            if (i < n && line.charAt(i) == '.') {
                i++;
                while (i < n && (isDigit(line.charAt(i)) || line.charAt(i) == '_')) {
                    i++;
                }
            }
            if (i < n && (line.charAt(i) == 'e' || line.charAt(i) == 'E')) {
                int exponent = i++;
                if (i < n && (line.charAt(i) == '+' || line.charAt(i) == '-')) {
                    i++;
                }
                int digits = i;
                while (i < n && (isDigit(line.charAt(i)) || line.charAt(i) == '_')) {
                    i++;
                }
                if (digits == i) {
                    i = exponent;
                }
            }
        }
        while (i < n && "fFdDlLuU".indexOf(line.charAt(i)) >= 0) {
            i++;
        }
        return i;
    }

    private SyntaxHighlightResult tokenizeJson(String line) {
        List<SyntaxToken> tokens = new ArrayList<SyntaxToken>();
        int i = 0;
        while (i < line.length()) {
            char c = line.charAt(i);
            if (c == '"') {
                int end = scanString(line, i + 1, '"');
                int stop = end < 0 ? line.length() : end + 1;
                int j = stop;
                while (j < line.length() && Character.isWhitespace(line.charAt(j))) {
                    j++;
                }
                tokens.add(new SyntaxToken(i, stop - i, j < line.length() && line.charAt(j) == ':' ? PROPERTY : STRING));
                i = stop;
            } else if (c == '-' || isDigit(c)) {
                int start = i++;
                if (c == '-' && (i >= line.length() || !isDigit(line.charAt(i)))) {
                    continue;
                }
                i = scanJsonNumber(line, start);
                tokens.add(new SyntaxToken(start, i - start, NUMBER));
            } else if (isIdentStart(c)) {
                int start = i++;
                while (i < line.length() && isIdentPart(line.charAt(i))) {
                    i++;
                }
                if (def.isKeyword(line.substring(start, i))) {
                    tokens.add(new SyntaxToken(start, i - start, KEYWORD));
                }
            } else {
                i++;
            }
        }
        return new SyntaxHighlightResult(tokens, STATE_NORMAL);
    }

    private static int scanJsonNumber(String line, int start) {
        int i = start;
        if (i < line.length() && line.charAt(i) == '-') {
            i++;
        }
        while (i < line.length() && isDigit(line.charAt(i))) {
            i++;
        }
        if (i < line.length() && line.charAt(i) == '.') {
            i++;
            while (i < line.length() && isDigit(line.charAt(i))) {
                i++;
            }
        }
        if (i < line.length() && (line.charAt(i) == 'e' || line.charAt(i) == 'E')) {
            int exponent = i++;
            if (i < line.length() && (line.charAt(i) == '+' || line.charAt(i) == '-')) {
                i++;
            }
            int digits = i;
            while (i < line.length() && isDigit(line.charAt(i))) {
                i++;
            }
            if (digits == i) {
                i = exponent;
            }
        }
        return i;
    }

    private SyntaxHighlightResult tokenizeXml(String line, int startState) {
        List<SyntaxToken> tokens = new ArrayList<SyntaxToken>();
        int i = 0;
        if (startState == STATE_XML_COMMENT) {
            int end = line.indexOf("-->");
            if (end < 0) {
                if (line.length() > 0) {
                    tokens.add(new SyntaxToken(0, line.length(), COMMENT));
                }
                return new SyntaxHighlightResult(tokens, STATE_XML_COMMENT);
            }
            tokens.add(new SyntaxToken(0, end + 3, COMMENT));
            i = end + 3;
        }
        while (i < line.length()) {
            int open = line.indexOf('<', i);
            if (open < 0) {
                break;
            }
            if (line.startsWith("<!--", open)) {
                int end = line.indexOf("-->", open + 4);
                if (end < 0) {
                    tokens.add(new SyntaxToken(open, line.length() - open, COMMENT));
                    return new SyntaxHighlightResult(tokens, STATE_XML_COMMENT);
                }
                tokens.add(new SyntaxToken(open, end + 3 - open, COMMENT));
                i = end + 3;
                continue;
            }
            int p = open + 1;
            if (p < line.length() && (line.charAt(p) == '/' || line.charAt(p) == '?' || line.charAt(p) == '!')) {
                p++;
            }
            int name = p;
            while (p < line.length() && isXmlName(line.charAt(p))) {
                p++;
            }
            if (p > name) {
                tokens.add(new SyntaxToken(name, p - name, TYPE));
            }
            while (p < line.length() && line.charAt(p) != '>') {
                if (line.charAt(p) == '"' || line.charAt(p) == '\'') {
                    char quote = line.charAt(p);
                    int end = scanString(line, p + 1, quote);
                    int stop = end < 0 ? line.length() : end + 1;
                    tokens.add(new SyntaxToken(p, stop - p, STRING));
                    p = stop;
                } else if (isXmlName(line.charAt(p))) {
                    int attr = p++;
                    while (p < line.length() && isXmlName(line.charAt(p))) {
                        p++;
                    }
                    tokens.add(new SyntaxToken(attr, p - attr, PROPERTY));
                } else {
                    p++;
                }
            }
            i = p < line.length() ? p + 1 : p;
        }
        return new SyntaxHighlightResult(tokens, STATE_NORMAL);
    }

    private static boolean isXmlName(char c) {
        return isIdentPart(c) || c == '-' || c == ':' || c == '.';
    }

    private SyntaxHighlightResult tokenizeCss(String line, int startState) {
        List<SyntaxToken> tokens = new ArrayList<SyntaxToken>();
        int i = 0;
        if (startState == STATE_BLOCK_COMMENT || startState == STATE_CSS_COMMENT_DECLARATION) {
            int end = line.indexOf("*/");
            if (end < 0) {
                if (line.length() > 0) {
                    tokens.add(new SyntaxToken(0, line.length(), COMMENT));
                }
                return new SyntaxHighlightResult(tokens, startState);
            }
            tokens.add(new SyntaxToken(0, end + 2, COMMENT));
            i = end + 2;
        }
        boolean declaration = startState == STATE_CSS_DECLARATION
                || startState == STATE_CSS_COMMENT_DECLARATION;
        while (i < line.length()) {
            if (line.startsWith("/*", i)) {
                int end = line.indexOf("*/", i + 2);
                if (end < 0) {
                    tokens.add(new SyntaxToken(i, line.length() - i, COMMENT));
                    return new SyntaxHighlightResult(tokens, declaration ? STATE_CSS_COMMENT_DECLARATION : STATE_BLOCK_COMMENT);
                }
                tokens.add(new SyntaxToken(i, end + 2 - i, COMMENT));
                i = end + 2;
                continue;
            }
            char c = line.charAt(i);
            if (c == '"' || c == '\'') {
                int end = scanString(line, i + 1, c);
                int stop = end < 0 ? line.length() : end + 1;
                tokens.add(new SyntaxToken(i, stop - i, STRING));
                i = stop;
            } else if (c == '{') {
                declaration = true; i++;
            } else if (c == '}') {
                declaration = false; i++;
            } else if (isDigit(c) || (c == '.' && i + 1 < line.length() && isDigit(line.charAt(i + 1)))) {
                int end = scanNumber(line, i);
                tokens.add(new SyntaxToken(i, end - i, NUMBER));
                i = end;
            } else if (isIdentStart(c) || c == '-' || c == '@') {
                int start = i++;
                while (i < line.length() && (isIdentPart(line.charAt(i)) || line.charAt(i) == '-')) {
                    i++;
                }
                int p = i;
                while (p < line.length() && Character.isWhitespace(line.charAt(p))) {
                    p++;
                }
                if (c != '@' && p < line.length() && line.charAt(p) == ':'
                        && (declaration || line.indexOf('{', p) < 0)) {
                    tokens.add(new SyntaxToken(start, i - start, PROPERTY));
                } else if (!declaration || c == '@') {
                    tokens.add(new SyntaxToken(start, i - start, TYPE));
                }
            } else {
                i++;
            }
        }
        return new SyntaxHighlightResult(tokens, declaration ? STATE_CSS_DECLARATION : STATE_NORMAL);
    }

    private SyntaxHighlightResult tokenizePython(String line, int startState) {
        List<SyntaxToken> tokens = new ArrayList<SyntaxToken>();
        int i = 0;
        if (startState == STATE_TRIPLE_SINGLE || startState == STATE_TRIPLE_DOUBLE) {
            String quote = startState == STATE_TRIPLE_SINGLE ? "'''" : "\"\"\"";
            int end = line.indexOf(quote);
            if (end < 0) {
                if (line.length() > 0) {
                    tokens.add(new SyntaxToken(0, line.length(), STRING));
                }
                return new SyntaxHighlightResult(tokens, startState);
            }
            tokens.add(new SyntaxToken(0, end + 3, STRING));
            i = end + 3;
        }
        while (i < line.length()) {
            char c = line.charAt(i);
            if (c == '#') {
                tokens.add(new SyntaxToken(i, line.length() - i, COMMENT));
                break;
            }
            if (line.startsWith("'''", i) || line.startsWith("\"\"\"", i)) {
                String quote = line.substring(i, i + 3);
                int end = line.indexOf(quote, i + 3);
                if (end < 0) {
                    tokens.add(new SyntaxToken(i, line.length() - i, STRING));
                    return new SyntaxHighlightResult(tokens, quote.charAt(0) == '\'' ? STATE_TRIPLE_SINGLE : STATE_TRIPLE_DOUBLE);
                }
                tokens.add(new SyntaxToken(i, end + 3 - i, STRING));
                i = end + 3;
            } else if (c == '"' || c == '\'') {
                int end = scanString(line, i + 1, c);
                int stop = end < 0 ? line.length() : end + 1;
                tokens.add(new SyntaxToken(i, stop - i, STRING));
                i = stop;
            } else if (isDigit(c) || (c == '.' && i + 1 < line.length() && isDigit(line.charAt(i + 1)))) {
                int end = scanNumber(line, i);
                tokens.add(new SyntaxToken(i, end - i, NUMBER));
                i = end;
            } else if (isIdentStart(c)) {
                int start = i++;
                while (i < line.length() && isIdentPart(line.charAt(i))) {
                    i++;
                }
                if (def.isKeyword(line.substring(start, i))) {
                    tokens.add(new SyntaxToken(start, i - start, KEYWORD));
                }
            } else {
                i++;
            }
        }
        return new SyntaxHighlightResult(tokens, STATE_NORMAL);
    }

    private static int indexOfClose(String line, int from, String close) {
        return line.indexOf(close, from);
    }

    // scans a single or double quoted string starting after the opening quote; returns the index of the
    // closing quote, or -1 if the line ends first (single line strings do not span lines)
    private static int scanString(String line, int from, char quote) {
        int n = line.length();
        int i = from;
        while (i < n) {
            char c = line.charAt(i);
            if (c == '\\') {
                i += 2;
                continue;
            }
            if (c == quote) {
                return i;
            }
            i++;
        }
        return -1;
    }

    // scans a backtick template string; returns index of the closing backtick or -1 if it spans lines
    private static int scanTemplate(String line, int from) {
        int n = line.length();
        int i = from;
        while (i < n) {
            char c = line.charAt(i);
            if (c == '\\') {
                i += 2;
                continue;
            }
            if (c == '`') {
                return i;
            }
            i++;
        }
        return -1;
    }
}
