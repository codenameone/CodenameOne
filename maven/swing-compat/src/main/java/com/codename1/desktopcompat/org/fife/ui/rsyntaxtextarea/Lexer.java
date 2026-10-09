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
package com.codename1.desktopcompat.org.fife.ui.rsyntaxtextarea;

import java.util.HashSet;

/// The small tokenizer behind the colors of [RSyntaxTextArea].
///
/// It knows four shapes of language rather than each language: the C
/// family (line and block comments, quoted strings, numbers, keywords),
/// the languages whose comments start with a hash, SQL, and markup. That
/// is enough to tell comments, strings, numbers and keywords apart, which
/// is what reading code needs; it is not a parser, and a construct it
/// does not know is painted as plain text.
final class Lexer {

    static final int NONE = 0;
    static final int C_LIKE = 1;
    static final int HASH = 2;
    static final int SQL = 3;
    static final int MARKUP = 4;

    private static final HashSet<String> KEYWORDS = set(
            "abstract as assert async await break case catch class const continue debugger default defer"
            + " delete do else enum export extends extern final finally fn for foreach from func function"
            + " go goto if impl implements import in instanceof interface internal is let match mod native"
            + " namespace new object of operator override package private protected pub public return"
            + " sealed select static strictfp struct super switch synchronized this throw throws trait"
            + " transient try type typedef typeof use using val var virtual volatile when where while"
            + " with yield");
    private static final HashSet<String> TYPES = set(
            "boolean bool byte char double float int long short void string String Object var auto"
            + " unsigned signed size_t i8 i16 i32 i64 u8 u16 u32 u64 f32 f64 usize isize str number any");
    private static final HashSet<String> LITERALS = set("true false null nil None True False undefined");
    private static final HashSet<String> HASH_KEYWORDS = set(
            "and as assert async await begin break case class continue def del do done elif else elsif"
            + " end esac except export fi finally for from function global if import in is lambda local"
            + " module nonlocal not or pass raise require rescue return then try unless until while"
            + " with yield echo exit set unset source");
    private static final HashSet<String> SQL_KEYWORDS = set(
            "ADD ALL ALTER AND AS ASC BEGIN BETWEEN BY CASE COLUMN COMMIT CONSTRAINT CREATE DATABASE"
            + " DEFAULT DELETE DESC DISTINCT DROP ELSE END EXISTS FOREIGN FROM FULL GROUP HAVING IN INDEX"
            + " INNER INSERT INTO IS JOIN KEY LEFT LIKE LIMIT NOT NULL OFFSET ON OR ORDER OUTER PRIMARY"
            + " REFERENCES RIGHT ROLLBACK SELECT SET TABLE THEN UNION UNIQUE UPDATE VALUES VIEW WHEN"
            + " WHERE WITH");

    /// The runs of the line scanned last: the end of each and its type.
    int[] ends = new int[32];
    int[] types = new int[32];
    int count;

    private static HashSet<String> set(String words) {
        HashSet<String> out = new HashSet<String>();
        int start = 0;
        for (int i = 0; i <= words.length(); i++) {
            if (i == words.length() || words.charAt(i) == ' ') {
                if (i > start) {
                    out.add(words.substring(start, i));
                }
                start = i + 1;
            }
        }
        return out;
    }

    /// The shape of language a syntax style names.
    static int family(String style) {
        if (style == null || style.length() < 6 || SyntaxConstants.SYNTAX_STYLE_NONE.equals(style)) {
            return NONE;
        }
        String s = style.substring(5);
        if ("python".equals(s) || "unix".equals(s) || "yaml".equals(s) || "properties".equals(s)
                || "ruby".equals(s) || "perl".equals(s) || "dockerfile".equals(s) || "makefile".equals(s)
                || "ini".equals(s) || "hosts".equals(s) || "htaccess".equals(s) || "tcl".equals(s)) {
            return HASH;
        }
        if ("sql".equals(s)) {
            return SQL;
        }
        if ("xml".equals(s) || "html".equals(s) || "jsp".equals(s) || "mxml".equals(s) || "dtd".equals(s)
                || "handlebars".equals(s) || "php".equals(s)) {
            return MARKUP;
        }
        if ("markdown".equals(s) || "csv".equals(s) || "bbcode".equals(s) || "latex".equals(s)
                || "bat".equals(s) || "asm".equals(s) || "asm6502".equals(s) || "fortran".equals(s)
                || "lisp".equals(s) || "clojure".equals(s) || "vb".equals(s) || "vhdl".equals(s)
                || "sas".equals(s) || "nsis".equals(s) || "lua".equals(s) || "delphi".equals(s)) {
            return NONE;
        }
        return C_LIKE;
    }

    private void run(int end, int type) {
        if (count > 0 && types[count - 1] == type) {
            ends[count - 1] = end;
            return;
        }
        if (count == ends.length) {
            int[] e = new int[count * 2];
            int[] t = new int[count * 2];
            System.arraycopy(ends, 0, e, 0, count);
            System.arraycopy(types, 0, t, 0, count);
            ends = e;
            types = t;
        }
        ends[count] = end;
        types[count] = type;
        count++;
    }

    private static boolean wordStart(char c) {
        return c == '_' || c == '$' || c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c > 127;
    }

    private static boolean digit(char c) {
        return c >= '0' && c <= '9';
    }

    private static String upper(String s) {
        char[] out = new char[s.length()];
        for (int i = 0; i < out.length; i++) {
            char c = s.charAt(i);
            out[i] = c >= 'a' && c <= 'z' ? (char) (c - 32) : c;
        }
        return new String(out);
    }

    /// Scans one line into [#ends] and [#types].
    ///
    /// #### Parameters
    ///
    /// - `s`: the line, without its line end
    ///
    /// - `family`: the shape of the language
    ///
    /// - `state`: 1 when the line starts inside a block comment, else 0
    ///
    /// #### Returns
    ///
    /// 1 when the line ends inside a block comment, else 0
    int scan(String s, int family, int state) {
        count = 0;
        int n = s.length();
        if (family == NONE) {
            if (n > 0) {
                run(n, TokenTypes.IDENTIFIER);
            }
            return 0;
        }
        String open = family == MARKUP ? "<!--" : "/*";
        String close = family == MARKUP ? "-->" : "*/";
        int commentType = family == MARKUP ? TokenTypes.MARKUP_COMMENT : TokenTypes.COMMENT_MULTILINE;
        boolean blocks = family != HASH;
        boolean inTag = false;
        int i = 0;
        while (i < n) {
            if (state == 1) {
                int end = s.indexOf(close, i);
                if (end < 0) {
                    run(n, commentType);
                    return 1;
                }
                i = end + close.length();
                run(i, commentType);
                state = 0;
                continue;
            }
            char c = s.charAt(i);
            if (blocks && s.startsWith(open, i)) {
                state = 1;
                run(i + open.length(), commentType);
                i += open.length();
                continue;
            }
            if (family == MARKUP) {
                if (c == '<') {
                    int j = i + 1;
                    if (j < n && (s.charAt(j) == '/' || s.charAt(j) == '?' || s.charAt(j) == '!')) {
                        j++;
                    }
                    run(j, TokenTypes.MARKUP_TAG_DELIMITER);
                    int k = j;
                    while (k < n && (wordStart(s.charAt(k)) || digit(s.charAt(k)) || s.charAt(k) == ':'
                            || s.charAt(k) == '-' || s.charAt(k) == '.')) {
                        k++;
                    }
                    if (k > j) {
                        run(k, TokenTypes.MARKUP_TAG_NAME);
                    }
                    i = k;
                    inTag = true;
                    continue;
                }
                if (!inTag) {
                    int end = s.indexOf('<', i);
                    i = end < 0 ? n : end;
                    run(i, TokenTypes.IDENTIFIER);
                    continue;
                }
                if (c == '>' || (c == '/' || c == '?') && i + 1 < n && s.charAt(i + 1) == '>') {
                    i += c == '>' ? 1 : 2;
                    run(i, TokenTypes.MARKUP_TAG_DELIMITER);
                    inTag = false;
                    continue;
                }
                if (c == '"' || c == '\'') {
                    int end = s.indexOf(c, i + 1);
                    i = end < 0 ? n : end + 1;
                    run(i, TokenTypes.MARKUP_TAG_ATTRIBUTE_VALUE);
                    continue;
                }
                if (wordStart(c)) {
                    int k = i + 1;
                    while (k < n && (wordStart(s.charAt(k)) || digit(s.charAt(k)) || s.charAt(k) == ':'
                            || s.charAt(k) == '-')) {
                        k++;
                    }
                    i = k;
                    run(i, TokenTypes.MARKUP_TAG_ATTRIBUTE);
                    continue;
                }
                i++;
                run(i, c == ' ' ? TokenTypes.WHITESPACE : TokenTypes.OPERATOR);
                continue;
            }
            if (family == C_LIKE && c == '/' && i + 1 < n && s.charAt(i + 1) == '/'
                    || family == HASH && c == '#'
                    || family == SQL && c == '-' && i + 1 < n && s.charAt(i + 1) == '-') {
                run(n, TokenTypes.COMMENT_EOL);
                return 0;
            }
            if (c == '"' || c == '\'' || c == '`') {
                int k = i + 1;
                while (k < n && s.charAt(k) != c) {
                    k += s.charAt(k) == '\\' ? 2 : 1;
                }
                i = Math.min(n, k + 1);
                run(i, c == '"' ? TokenTypes.LITERAL_STRING_DOUBLE_QUOTE
                        : c == '`' ? TokenTypes.LITERAL_BACKQUOTE : TokenTypes.LITERAL_CHAR);
                continue;
            }
            if (digit(c)) {
                int k = i + 1;
                boolean hex = c == '0' && k < n && (s.charAt(k) == 'x' || s.charAt(k) == 'X');
                while (k < n && (digit(s.charAt(k)) || wordStart(s.charAt(k)) || s.charAt(k) == '.')) {
                    k++;
                }
                i = k;
                run(i, hex ? TokenTypes.LITERAL_NUMBER_HEXADECIMAL : TokenTypes.LITERAL_NUMBER_DECIMAL_INT);
                continue;
            }
            if (wordStart(c)) {
                int k = i + 1;
                while (k < n && (wordStart(s.charAt(k)) || digit(s.charAt(k)))) {
                    k++;
                }
                String word = s.substring(i, k);
                int type = TokenTypes.IDENTIFIER;
                if (family == SQL) {
                    if (SQL_KEYWORDS.contains(upper(word))) {
                        type = TokenTypes.RESERVED_WORD;
                    }
                } else if (LITERALS.contains(word)) {
                    type = TokenTypes.LITERAL_BOOLEAN;
                } else if (family == HASH ? HASH_KEYWORDS.contains(word) : KEYWORDS.contains(word)) {
                    type = TokenTypes.RESERVED_WORD;
                } else if (family == C_LIKE && TYPES.contains(word)) {
                    type = TokenTypes.DATA_TYPE;
                }
                i = k;
                run(i, type);
                continue;
            }
            if (c == '@' && family == C_LIKE) {
                int k = i + 1;
                while (k < n && (wordStart(s.charAt(k)) || digit(s.charAt(k)) || s.charAt(k) == '.')) {
                    k++;
                }
                i = k;
                run(i, TokenTypes.ANNOTATION);
                continue;
            }
            i++;
            run(i, c == ' ' ? TokenTypes.WHITESPACE
                    : c == '(' || c == ')' || c == '{' || c == '}' || c == '[' || c == ']' || c == ';'
                            || c == ',' ? TokenTypes.SEPARATOR : TokenTypes.OPERATOR);
        }
        return state;
    }
}
