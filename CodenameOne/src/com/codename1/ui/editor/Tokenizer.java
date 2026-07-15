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
    public static final int KEYWORD = 1;
    /// Token kind: a string or character literal.
    public static final int STRING = 2;
    /// Token kind: a comment.
    public static final int COMMENT = 3;
    /// Token kind: a numeric literal.
    public static final int NUMBER = 4;

    /// Lexer state: normal code.
    public static final int STATE_NORMAL = 0;
    /// Lexer state: inside a block comment that opened on a previous line.
    public static final int STATE_BLOCK_COMMENT = 1;
    /// Lexer state: inside a backtick template string that opened on a previous line.
    public static final int STATE_TEMPLATE = 2;

    private final LanguageDef def;

    /// Creates a tokenizer for the given language.
    public Tokenizer(LanguageDef def) {
        this.def = def;
    }

    /// A colored span within a line: `[start, start + length)` has the given kind.
    public static final class Token {
        /// The start column within the line.
        public final int start;
        /// The number of characters.
        public final int length;
        /// One of the token kind constants.
        public final int kind;

        public Token(int start, int length, int kind) {
            this.start = start;
            this.length = length;
            this.kind = kind;
        }
    }

    /// The result of tokenizing one line: the colored spans and the lexer state to carry to the next line.
    public static final class TokenLine {
        /// The colored spans, ordered by start.
        public final List<Token> tokens;
        /// The lexer state on leaving the line.
        public final int endState;

        public TokenLine(List<Token> tokens, int endState) {
            this.tokens = tokens;
            this.endState = endState;
        }
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
    public TokenLine tokenize(String line, int startState) {
        List<Token> tokens = new ArrayList<Token>();
        if (def.isPlain()) {
            return new TokenLine(tokens, STATE_NORMAL);
        }
        int n = line.length();
        int i = 0;
        int state = startState;

        if (state == STATE_BLOCK_COMMENT) {
            int end = indexOfClose(line, 0, "*/");
            if (end < 0) {
                if (n > 0) {
                    tokens.add(new Token(0, n, COMMENT));
                }
                return new TokenLine(tokens, STATE_BLOCK_COMMENT);
            }
            tokens.add(new Token(0, end + 2, COMMENT));
            i = end + 2;
            state = STATE_NORMAL;
        } else if (state == STATE_TEMPLATE) {
            int end = scanTemplate(line, 0);
            if (end < 0) {
                if (n > 0) {
                    tokens.add(new Token(0, n, STRING));
                }
                return new TokenLine(tokens, STATE_TEMPLATE);
            }
            tokens.add(new Token(0, end + 1, STRING));
            i = end + 1;
            state = STATE_NORMAL;
        }

        while (i < n) {
            char c = line.charAt(i);
            // line comment
            if (def.hasLineCommentSlash() && c == '/' && i + 1 < n && line.charAt(i + 1) == '/') {
                tokens.add(new Token(i, n - i, COMMENT));
                break;
            }
            if (def.hasLineCommentHash() && c == '#') {
                tokens.add(new Token(i, n - i, COMMENT));
                break;
            }
            // block comment
            if (def.hasBlockComment() && c == '/' && i + 1 < n && line.charAt(i + 1) == '*') {
                int end = indexOfClose(line, i + 2, "*/");
                if (end < 0) {
                    tokens.add(new Token(i, n - i, COMMENT));
                    return new TokenLine(tokens, STATE_BLOCK_COMMENT);
                }
                tokens.add(new Token(i, end + 2 - i, COMMENT));
                i = end + 2;
                continue;
            }
            // template string
            if (def.hasTemplateString() && c == '`') {
                int end = scanTemplate(line, i + 1);
                if (end < 0) {
                    tokens.add(new Token(i, n - i, STRING));
                    return new TokenLine(tokens, STATE_TEMPLATE);
                }
                tokens.add(new Token(i, end + 1 - i, STRING));
                i = end + 1;
                continue;
            }
            // string / char literal
            if (c == '"' || c == '\'') {
                int end = scanString(line, i + 1, c);
                int stop = end < 0 ? n : end + 1;
                tokens.add(new Token(i, stop - i, STRING));
                i = stop;
                continue;
            }
            // number
            if (isDigit(c)) {
                int j = scanNumber(line, i);
                tokens.add(new Token(i, j - i, NUMBER));
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
                    tokens.add(new Token(i, j - i, KEYWORD));
                }
                i = j;
                continue;
            }
            i++;
        }
        return new TokenLine(tokens, state);
    }

    private static int scanNumber(String line, int start) {
        int n = line.length();
        int i = start;
        if (i + 1 < n && line.charAt(i) == '0'
                && (line.charAt(i + 1) == 'x' || line.charAt(i + 1) == 'X'
                || line.charAt(i + 1) == 'b' || line.charAt(i + 1) == 'B')) {
            i += 2;
            while (i < n && (Character.digit(line.charAt(i), 16) >= 0 || line.charAt(i) == '_')) {
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
