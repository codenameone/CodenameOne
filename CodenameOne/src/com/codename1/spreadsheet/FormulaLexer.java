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
package com.codename1.spreadsheet;

import java.util.ArrayList;
import java.util.List;

/// Reads formula text the way the editor needs it while it is typed: half a formula is the
/// normal case, so nothing here throws. It splits the text into the pieces the editor colors,
/// finds the references the grid outlines, the function and argument the caret is in, and
/// the name being typed.
final class FormulaLexer {
    static final int OPERATOR = 1;
    static final int STRING = 2;
    static final int NUMBER = 3;
    static final int ERROR = 4;
    static final int BOOLEAN = 5;
    static final int FUNCTION = 6;
    static final int NAME = 7;
    static final int REFERENCE = 8;
    static final int OPEN = 9;
    static final int CLOSE = 10;
    static final int SEPARATOR = 11;
    static final int SPACE = 12;
    static final int OTHER = 13;

    private static final String[] ERRORS = {
        "#DIV/0!", "#N/A", "#NAME?", "#NULL!", "#NUM!", "#REF!", "#VALUE!", "#SPILL!", "#CALC!", "#GETTING_DATA"
    };

    /// A piece of formula text.
    static final class Token {
        final int start;
        final int end;
        final int kind;
        /// For a reference: the sheet it names, or null for the formula's own sheet.
        String sheet;
        int r1;
        int c1;
        int r2;
        int c2;
        /// For a reference: which color it is shown in, the same for the same reference.
        int color = -1;

        Token(int start, int end, int kind) {
            this.start = start;
            this.end = end;
            this.kind = kind;
        }
    }

    private FormulaLexer() {
    }

    // The colors references take, in the editor's text and as outlines in the grid, one after
    // another as a spreadsheet application gives them: on a light background, then a dark one.
    private static final int[] LIGHT = {0x1f5fbf, 0xc4302b, 0x7b3fc4, 0x13803d, 0xc25e00, 0x007f99, 0xb0279d, 0x5c6b10};
    private static final int[] DARK = {0x6ea8ff, 0xff7b72, 0xc39bff, 0x56d38a, 0xffa657, 0x4fd1e8, 0xf28ce0, 0xc8d65c};

    /// The color of the reference given color index `index`.
    static int referenceColor(int index, boolean dark) {
        int[] colors = dark ? DARK : LIGHT;
        return colors[(index % colors.length + colors.length) % colors.length];
    }

    /// The pieces of a formula. Text that does not start with `=` is a value, and has none.
    static List<Token> lex(String text) {
        List<Token> out = new ArrayList<Token>();
        if (text == null || text.length() == 0 || text.charAt(0) != '=') {
            return out;
        }
        out.add(new Token(0, 1, OPERATOR));
        int n = text.length();
        int i = 1;
        while (i < n) {
            char c = text.charAt(i);
            int start = i;
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                while (i < n && text.charAt(i) <= ' ') {
                    i++;
                }
                out.add(new Token(start, i, SPACE));
            } else if (c == '"') {
                i++;
                while (i < n) {
                    if (text.charAt(i) == '"') {
                        if (i + 1 < n && text.charAt(i + 1) == '"') {
                            i += 2;
                            continue;
                        }
                        i++;
                        break;
                    }
                    i++;
                }
                out.add(new Token(start, i, STRING));
            } else if (c == '#') {
                int end = start + 1;
                for (String e : ERRORS) {
                    if (text.regionMatches(true, start, e, 0, e.length())) {
                        end = start + e.length();
                        break;
                    }
                }
                i = end;
                out.add(new Token(start, i, end > start + 1 ? ERROR : OTHER));
            } else if (c == '\'') {
                // a quoted sheet name, then the reference after it
                i++;
                while (i < n) {
                    if (text.charAt(i) == '\'') {
                        if (i + 1 < n && text.charAt(i + 1) == '\'') {
                            i += 2;
                            continue;
                        }
                        i++;
                        break;
                    }
                    i++;
                }
                Token t = null;
                if (i < n && text.charAt(i) == '!') {
                    String sheet = text.substring(start + 1, i - 1).replace("''", "'");
                    t = reference(text, i + 1, start, sheet);
                }
                if (t == null) {
                    out.add(new Token(start, i, OTHER));
                } else {
                    out.add(t);
                    i = t.end;
                }
            } else if (FormulaParser.isDigit(c) || c == '.' && i + 1 < n && FormulaParser.isDigit(text.charAt(i + 1))) {
                Token t = reference(text, i, i, null);
                if (t != null) {
                    out.add(t);
                    i = t.end;
                } else {
                    i = number(text, i);
                    out.add(new Token(start, i, NUMBER));
                }
            } else if (isWord(c)) {
                while (i < n && isWord(text.charAt(i))) {
                    i++;
                }
                if (i < n && text.charAt(i) == '!') {
                    Token t = reference(text, i + 1, start, text.substring(start, i));
                    if (t != null) {
                        out.add(t);
                        i = t.end;
                        continue;
                    }
                }
                int after = i;
                while (after < n && text.charAt(after) == ' ') {
                    after++;
                }
                if (after < n && text.charAt(after) == '(') {
                    out.add(new Token(start, i, FUNCTION));
                    continue;
                }
                Token t = reference(text, start, start, null);
                if (t != null && t.end >= i) {
                    out.add(t);
                    i = t.end;
                    continue;
                }
                String word = text.substring(start, i);
                out.add(new Token(start, i, "TRUE".equalsIgnoreCase(word) || "FALSE".equalsIgnoreCase(word) ? BOOLEAN : NAME));
            } else if (c == '(' || c == '{') {
                i++;
                out.add(new Token(start, i, OPEN));
            } else if (c == ')' || c == '}') {
                i++;
                out.add(new Token(start, i, CLOSE));
            } else if (c == ',' || c == ';') {
                i++;
                out.add(new Token(start, i, SEPARATOR));
            } else if ("+-*/^&=<>%:@".indexOf(c) >= 0) {
                i++;
                if (i < n && (c == '<' && (text.charAt(i) == '=' || text.charAt(i) == '>') || c == '>' && text.charAt(i) == '=')) {
                    i++;
                }
                out.add(new Token(start, i, OPERATOR));
            } else {
                i++;
                out.add(new Token(start, i, OTHER));
            }
        }
        color(out);
        return out;
    }

    private static boolean isWord(char c) {
        return FormulaParser.isLetter(c) || FormulaParser.isDigit(c) || c == '_' || c == '.' || c == '$' || c == '\\'
                || c > 127;
    }

    private static int number(String text, int i) {
        int n = text.length();
        while (i < n && (FormulaParser.isDigit(text.charAt(i)) || text.charAt(i) == '.')) {
            i++;
        }
        if (i < n && (text.charAt(i) == 'e' || text.charAt(i) == 'E')) {
            int j = i + 1;
            if (j < n && (text.charAt(j) == '+' || text.charAt(j) == '-')) {
                j++;
            }
            if (j < n && FormulaParser.isDigit(text.charAt(j))) {
                i = j;
                while (i < n && FormulaParser.isDigit(text.charAt(i))) {
                    i++;
                }
            }
        }
        return i;
    }

    /// A reference at `at`: a cell, a range of cells, whole columns or whole rows. The token
    /// starts at `start`, before any sheet name. Null when the text there is none.
    private static Token reference(String text, int at, int start, String sheet) {
        int[] a = new int[3];
        int end = corner(text, at, a);
        if (end < 0) {
            return null;
        }
        int[] b = null;
        if (end < text.length() && text.charAt(end) == ':') {
            int[] other = new int[3];
            int end2 = corner(text, end + 1, other);
            // A1:B2, A:B and 1:2, never a cell with a column
            if (end2 > 0 && kindOf(a) == kindOf(other)) {
                b = other;
                end = end2;
            }
        }
        if (kindOf(a) != 0 && b == null) {
            // a lone column or row is a name or a number, not a reference
            return null;
        }
        if (end < text.length() && (isWord(text.charAt(end)) || text.charAt(end) == '(')) {
            return null;
        }
        Token t = new Token(start, end, REFERENCE);
        t.sheet = sheet;
        int[] last = b == null ? a : b;
        t.r1 = Math.min(a[0], last[0]);
        t.r2 = Math.max(a[0], last[0]);
        t.c1 = Math.min(a[1], last[1]);
        t.c2 = Math.max(a[1], last[1]);
        if (kindOf(a) == 1) {
            t.r1 = 0;
            t.r2 = Node.MAX_ROW;
        } else if (kindOf(a) == 2) {
            t.c1 = 0;
            t.c2 = Node.MAX_COL;
        }
        return t;
    }

    // 0 for a cell, 1 for a whole column, 2 for a whole row
    private static int kindOf(int[] corner) {
        return corner[2];
    }

    /// Reads `$A$1`, `$A` or `$1` into row, column and what it was; the end, or -1.
    private static int corner(String text, int at, int[] out) {
        int n = text.length();
        int i = at;
        if (i < n && text.charAt(i) == '$') {
            i++;
        }
        int letters = i;
        while (i < n && FormulaParser.isLetter(text.charAt(i)) && i - letters < 3) {
            i++;
        }
        int col = -1;
        if (i > letters) {
            col = FormulaParser.column(text, letters, i);
            if (col < 0 || col > Node.MAX_COL || i < n && FormulaParser.isLetter(text.charAt(i))) {
                return -1;
            }
            if (i < n && text.charAt(i) == '$') {
                i++;
            }
        }
        int digits = i;
        long row = 0;
        while (i < n && FormulaParser.isDigit(text.charAt(i)) && i - digits < 8) {
            row = row * 10 + (text.charAt(i) - '0');
            i++;
        }
        if (i < n && FormulaParser.isDigit(text.charAt(i))) {
            return -1;
        }
        if (col >= 0 && i > digits) {
            if (row < 1 || row > Node.MAX_ROW + 1) {
                return -1;
            }
            out[0] = (int) row - 1;
            out[1] = col;
            out[2] = 0;
            return i;
        }
        if (col >= 0) {
            out[0] = 0;
            out[1] = col;
            out[2] = 1;
            return i;
        }
        if (i > digits && row >= 1 && row <= Node.MAX_ROW + 1) {
            out[0] = (int) row - 1;
            out[1] = 0;
            out[2] = 2;
            return i;
        }
        return -1;
    }

    /// Gives each distinct reference its color, in the order they appear.
    private static void color(List<Token> tokens) {
        List<Token> seen = new ArrayList<Token>();
        for (Token t : tokens) {
            if (t.kind != REFERENCE) {
                continue;
            }
            for (Token s : seen) {
                if (s.r1 == t.r1 && s.c1 == t.c1 && s.r2 == t.r2 && s.c2 == t.c2
                        && (s.sheet == null ? t.sheet == null : s.sheet.equalsIgnoreCase(t.sheet))) {
                    t.color = s.color;
                    break;
                }
            }
            if (t.color < 0) {
                t.color = seen.size();
                seen.add(t);
            }
        }
    }

    /// The function the caret is in and which of its arguments, counted from 0: `{name, index}`
    /// as an `Object[]`, or null outside every function.
    static Object[] call(List<Token> tokens, String text, int caret) {
        List<Object[]> stack = new ArrayList<Object[]>();
        String pending = null;
        for (Token t : tokens) {
            if (t.start >= caret) {
                break;
            }
            if (t.kind == FUNCTION) {
                pending = FormulaParser.asciiUpper(text.substring(t.start, t.end));
                continue;
            }
            if (t.kind == SPACE) {
                continue;
            }
            if (t.kind == OPEN) {
                // an array constant's commas separate its values, not arguments
                stack.add(new Object[] {text.charAt(t.start) == '{' ? null : pending, new int[1]});
            } else if (t.kind == CLOSE) {
                if (!stack.isEmpty()) {
                    stack.remove(stack.size() - 1);
                }
            } else if (t.kind == SEPARATOR && !stack.isEmpty()) {
                ((int[]) stack.get(stack.size() - 1)[1])[0]++;
            }
            pending = null;
        }
        for (int i = stack.size() - 1; i >= 0; i--) {
            Object[] top = stack.get(i);
            // the innermost function: parentheses that only group, and the braces of an array
            // constant, are passed through to the call around them
            if (top[0] != null) {
                return new Object[] {top[0], Integer.valueOf(((int[]) top[1])[0])};
            }
        }
        return null;
    }

    /// Where a function or name being typed at the caret starts, or -1 when the caret is not
    /// at the end of one.
    static int wordAt(List<Token> tokens, int caret) {
        for (Token t : tokens) {
            if (t.end == caret && (t.kind == NAME || t.kind == FUNCTION || t.kind == BOOLEAN)) {
                return t.start;
            }
            if (t.end == caret && t.kind == REFERENCE && t.sheet == null) {
                // CO or LOG1 are cells too, until the name is typed out
                return t.start;
            }
        }
        return -1;
    }

    /// Whether a reference may be put at the caret by tapping a cell: after `=`, an operator,
    /// an opening parenthesis or a separator.
    static boolean takesReference(List<Token> tokens, int caret) {
        Token before = null;
        for (Token t : tokens) {
            if (t.end > caret) {
                break;
            }
            if (t.kind != SPACE) {
                before = t;
            }
        }
        if (before == null) {
            return false;
        }
        return before.kind == OPERATOR && before.end <= caret || before.kind == OPEN || before.kind == SEPARATOR;
    }

    /// The reference that ends at the caret, or null.
    static Token referenceEndingAt(List<Token> tokens, int caret) {
        for (Token t : tokens) {
            if (t.kind == REFERENCE && t.end == caret) {
                return t;
            }
        }
        return null;
    }
}
