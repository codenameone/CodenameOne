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

/// Parses formula text into a tree and writes a tree back as text, in the dialect of Excel
/// files or of OpenDocument files. The two differ in how a reference is written
/// (`Sheet1!A1:B2` against `[$Sheet1.A1:.B2]`), in the separators of arguments and array
/// constants, and in the prefixes of newer function names; the trees are the same.
///
/// Operator precedence follows Excel: range, intersection, union, sign, percent, power,
/// multiplication, addition, concatenation, comparison. The sign binds tighter than the power,
/// so `-2^2` is 4, and the power associates to the left.
final class FormulaParser {
    private static final String[] ERRORS = {
        "#DIV/0!", "#N/A", "#NAME?", "#NULL!", "#NUM!", "#REF!", "#VALUE!"
    };
    private static final String[] PREFIXES = {
        "_XLFN._XLWS.", "_XLFN.", "_XLWS.", "COM.MICROSOFT.", "ORG.LIBREOFFICE.", "ORG.OPENOFFICE."
    };

    private final String s;
    private final int len;
    private final boolean ods;
    private final char argSeparator;
    private int p;

    private FormulaParser(String s, boolean ods) {
        this.s = s;
        this.len = s.length();
        this.ods = ods;
        this.argSeparator = ods ? ';' : ',';
    }

    /// Parses formula text that does not include the leading equals sign.
    static Node parse(String text, boolean ods) {
        FormulaParser parser = new FormulaParser(text, ods);
        Node n = parser.comparison();
        parser.ws();
        if (parser.p < parser.len) {
            throw parser.fail();
        }
        return n;
    }

    private FormulaSyntaxException fail() {
        return new FormulaSyntaxException("Cannot parse formula at " + p + ": " + s);
    }

    private void ws() {
        while (p < len) {
            char c = s.charAt(p);
            if (c != ' ' && c != '\n' && c != '\r' && c != '\t') {
                return;
            }
            p++;
        }
    }

    private boolean eat(char c) {
        if (p < len && s.charAt(p) == c) {
            p++;
            return true;
        }
        return false;
    }

    private void expect(char c) {
        ws();
        if (!eat(c)) {
            throw fail();
        }
    }

    private Node comparison() {
        Node left = concat();
        for (;;) {
            ws();
            String op;
            if (s.startsWith("<>", p)) {
                op = "<>";
            } else if (s.startsWith("<=", p)) {
                op = "<=";
            } else if (s.startsWith(">=", p)) {
                op = ">=";
            } else if (s.startsWith("<", p)) {
                op = "<";
            } else if (s.startsWith(">", p)) {
                op = ">";
            } else if (s.startsWith("=", p)) {
                op = "=";
            } else {
                return left;
            }
            p += op.length();
            left = new Node(Node.BINARY, op, left, concat());
        }
    }

    private Node concat() {
        Node left = additive();
        for (;;) {
            ws();
            if (!eat('&')) {
                return left;
            }
            left = new Node(Node.BINARY, "&", left, additive());
        }
    }

    private Node additive() {
        Node left = multiplicative();
        for (;;) {
            ws();
            if (eat('+')) {
                left = new Node(Node.BINARY, "+", left, multiplicative());
            } else if (eat('-')) {
                left = new Node(Node.BINARY, "-", left, multiplicative());
            } else {
                return left;
            }
        }
    }

    private Node multiplicative() {
        Node left = power();
        for (;;) {
            ws();
            if (eat('*')) {
                left = new Node(Node.BINARY, "*", left, power());
            } else if (eat('/')) {
                left = new Node(Node.BINARY, "/", left, power());
            } else {
                return left;
            }
        }
    }

    private Node power() {
        Node left = percent();
        for (;;) {
            ws();
            if (!eat('^')) {
                return left;
            }
            left = new Node(Node.BINARY, "^", left, percent());
        }
    }

    private Node percent() {
        Node n = unary();
        for (;;) {
            ws();
            if (!eat('%')) {
                return n;
            }
            n = new Node(Node.PERCENT, null, n);
        }
    }

    private Node unary() {
        ws();
        if (p < len) {
            char c = s.charAt(p);
            if (c == '-' || c == '+') {
                p++;
                return new Node(Node.UNARY, c == '-' ? "-" : "+", unary());
            }
            if (c == '@') {
                // implicit intersection: with one value per cell there is nothing to pick
                p++;
                return unary();
            }
        }
        return intersection();
    }

    private Node intersection() {
        Node left = range();
        for (;;) {
            int save = p;
            if (ods) {
                ws();
                if (eat('~')) {
                    left = new Node(Node.BINARY, ",", left, range());
                    continue;
                }
                if (p < len && s.charAt(p) == '!' && left.isReferenceLike()) {
                    p++;
                    left = new Node(Node.BINARY, " ", left, range());
                    continue;
                }
                p = save;
                return left;
            }
            // In Excel a space between two references is the intersection operator.
            if (p >= len || s.charAt(p) != ' ' || !left.isReferenceLike()) {
                return left;
            }
            ws();
            if (p >= len) {
                return left;
            }
            char c = s.charAt(p);
            if (!isLetter(c) && c != '$' && c != '\'' && c != '(') {
                p = save;
                return left;
            }
            Node right = range();
            if (!right.isReferenceLike()) {
                throw fail();
            }
            left = new Node(Node.BINARY, " ", left, right);
        }
    }

    private Node range() {
        Node left = primary();
        while (p < len && s.charAt(p) == ':') {
            p++;
            Node right = primary();
            if (left.kind == Node.REF && right.kind == Node.REF && right.sheet == null) {
                left = bound(left, right);
            } else {
                left = new Node(Node.BINARY, ":", left, right);
            }
        }
        return left;
    }

    /// The area that spans two references, which is what the colon means between them.
    private static Node bound(Node a, Node b) {
        Node n = a.copyRef();
        n.area = true;
        n.wholeCol = a.wholeCol && b.wholeCol;
        n.wholeRow = a.wholeRow && b.wholeRow;
        if (b.r1 < a.r1) {
            n.r1 = b.r1;
            n.absR1 = b.absR1;
        }
        if (b.c1 < a.c1) {
            n.c1 = b.c1;
            n.absC1 = b.absC1;
        }
        n.r2 = a.r2;
        n.absR2 = a.absR2;
        if (b.r2 >= a.r2) {
            n.r2 = b.r2;
            n.absR2 = b.absR2;
        }
        n.c2 = a.c2;
        n.absC2 = a.absC2;
        if (b.c2 >= a.c2) {
            n.c2 = b.c2;
            n.absC2 = b.absC2;
        }
        return n;
    }

    private Node primary() {
        ws();
        if (p >= len) {
            throw fail();
        }
        char c = s.charAt(p);
        if (c == '"') {
            return new Node(Node.STR, string());
        }
        if (c == '(') {
            p++;
            Node inner = comparison();
            ws();
            while (!ods && eat(',')) {
                inner = new Node(Node.BINARY, ",", inner, comparison());
                ws();
            }
            expect(')');
            return new Node(Node.PAREN, null, inner);
        }
        if (c == '{') {
            return arrayConstant();
        }
        if (c == '#') {
            return new Node(Node.ERR, errorLiteral());
        }
        if (ods && c == '[') {
            return odsReference();
        }
        if (isDigit(c) || c == '.' || c == '$' && p + 1 < len && isDigit(s.charAt(p + 1))) {
            Node rows = wholeRows();
            if (rows != null) {
                return rows;
            }
            if (c == '$') {
                throw fail();
            }
            return new Node(Node.NUM, number());
        }
        if (c == '\'' && !ods) {
            String sheet = quoted();
            String sheet2 = null;
            if (eat(':')) {
                sheet2 = p < len && s.charAt(p) == '\'' ? quoted() : plainName();
            }
            if (!eat('!')) {
                throw fail();
            }
            return sheetReference(sheet, sheet2);
        }
        if (isLetter(c) || c == '_' || c == '$' || c == '\\') {
            return identifier();
        }
        throw fail();
    }

    private String plainName() {
        int start = p;
        while (p < len && isIdentifierPart(s.charAt(p))) {
            p++;
        }
        if (p == start) {
            throw fail();
        }
        return s.substring(start, p);
    }

    private Node identifier() {
        int start = p;
        String id = plainName();
        if (!ods) {
            if (eat('!')) {
                return sheetReference(id, null);
            }
            if (p < len && s.charAt(p) == ':') {
                // Sheet1:Sheet3!A1, a reference through several sheets
                int q = p + 1;
                int from = q;
                while (q < len && isIdentifierPart(s.charAt(q))) {
                    q++;
                }
                if (q > from && q < len && s.charAt(q) == '!') {
                    String sheet2 = s.substring(from, q);
                    p = q + 1;
                    return sheetReference(id, sheet2);
                }
            }
        }
        int q = p;
        while (q < len && s.charAt(q) == ' ') {
            q++;
        }
        if (q < len && s.charAt(q) == '(' && id.indexOf('$') < 0) {
            p = q + 1;
            return function(id);
        }
        p = start;
        Node ref = cellOrColumns();
        if (ref != null) {
            return ref;
        }
        p = start + id.length();
        if ("TRUE".equalsIgnoreCase(id)) {
            return new Node(Node.BOOL, Boolean.TRUE);
        }
        if ("FALSE".equalsIgnoreCase(id)) {
            return new Node(Node.BOOL, Boolean.FALSE);
        }
        if (id.indexOf('$') >= 0) {
            throw fail();
        }
        return new Node(Node.NAME, id);
    }

    private Node sheetReference(String sheet, String sheet2) {
        Node n = cellOrColumns();
        if (n == null) {
            n = wholeRows();
        }
        if (n == null) {
            if (p < len && s.charAt(p) == '#') {
                return new Node(Node.ERR, errorLiteral());
            }
            // a name scoped to a sheet; names are looked up in the workbook either way
            return new Node(Node.NAME, plainName());
        }
        n.sheet = sheet;
        n.sheet2 = sheet2;
        return n;
    }

    /// Reads `A1`, `$A$1` or the whole columns `A:C` at the current position, or answers null
    /// and consumes nothing. `LOG10` is a cell in such a file, and a function only when a
    /// parenthesis follows, which the caller has already ruled out.
    private Node cellOrColumns() {
        int save = p;
        boolean absC = eat('$');
        int cs = p;
        while (p < len && isLetter(s.charAt(p)) && p - cs < 4) {
            p++;
        }
        int letters = p - cs;
        if (letters >= 1 && letters <= 3) {
            int col = column(s, cs, p);
            if (col <= Node.MAX_COL) {
                int afterCol = p;
                boolean absR = eat('$');
                int rs = p;
                while (p < len && isDigit(s.charAt(p)) && p - rs < 8) {
                    p++;
                }
                if (p > rs && p - rs <= 7 && !(p < len && isIdentifierPart(s.charAt(p)))) {
                    int row = Integer.parseInt(s.substring(rs, p)) - 1;
                    if (row >= 0 && row <= Node.MAX_ROW) {
                        Node n = new Node(Node.REF);
                        n.r1 = row;
                        n.r2 = row;
                        n.c1 = col;
                        n.c2 = col;
                        n.absR1 = absR;
                        n.absR2 = absR;
                        n.absC1 = absC;
                        n.absC2 = absC;
                        return n;
                    }
                }
                p = afterCol;
                if (p < len && s.charAt(p) == ':') {
                    int q = p + 1;
                    boolean abs2 = q < len && s.charAt(q) == '$';
                    if (abs2) {
                        q++;
                    }
                    int cs2 = q;
                    while (q < len && isLetter(s.charAt(q)) && q - cs2 < 4) {
                        q++;
                    }
                    if (q > cs2 && q - cs2 <= 3 && !(q < len && (isIdentifierPart(s.charAt(q)) || s.charAt(q) == '('
                            || s.charAt(q) == '!'))) {
                        int col2 = column(s, cs2, q);
                        if (col2 <= Node.MAX_COL) {
                            p = q;
                            Node n = new Node(Node.REF);
                            n.area = true;
                            n.wholeCol = true;
                            n.r1 = 0;
                            n.r2 = Node.MAX_ROW;
                            n.c1 = Math.min(col, col2);
                            n.c2 = Math.max(col, col2);
                            n.absC1 = absC;
                            n.absC2 = abs2;
                            return n;
                        }
                    }
                }
            }
        }
        p = save;
        return null;
    }

    /// Reads the whole rows `1:3` or `$1:$3`, or answers null and consumes nothing.
    private Node wholeRows() {
        int save = p;
        boolean abs1 = eat('$');
        int rs = p;
        while (p < len && isDigit(s.charAt(p))) {
            p++;
        }
        if (p > rs && p - rs <= 7 && p < len && s.charAt(p) == ':') {
            int first = Integer.parseInt(s.substring(rs, p)) - 1;
            p++;
            boolean abs2 = eat('$');
            int rs2 = p;
            while (p < len && isDigit(s.charAt(p))) {
                p++;
            }
            if (p > rs2 && p - rs2 <= 7 && !(p < len && (isIdentifierPart(s.charAt(p))))) {
                int second = Integer.parseInt(s.substring(rs2, p)) - 1;
                if (first >= 0 && second >= 0 && first <= Node.MAX_ROW && second <= Node.MAX_ROW) {
                    Node n = new Node(Node.REF);
                    n.area = true;
                    n.wholeRow = true;
                    n.c1 = 0;
                    n.c2 = Node.MAX_COL;
                    n.r1 = Math.min(first, second);
                    n.r2 = Math.max(first, second);
                    n.absR1 = abs1;
                    n.absR2 = abs2;
                    return n;
                }
            }
        }
        p = save;
        return null;
    }

    /// An OpenDocument reference: `[.A1]`, `[.A1:.B2]`, `[$Sheet1.A1]`, `['My Sheet'.A1:.B2]`,
    /// `[.A:.C]`.
    private Node odsReference() {
        p++;
        Node a = odsPart();
        if (a.kind != Node.REF) {
            expect(']');
            return a;
        }
        if (eat(':')) {
            Node b = odsPart();
            if (b.kind != Node.REF) {
                expect(']');
                return b;
            }
            String second = b.sheet;
            b.sheet = null;
            a = bound(a, b);
            if (second != null && !second.equals(a.sheet)) {
                a.sheet2 = second;
            }
        }
        expect(']');
        return a;
    }

    private Node odsPart() {
        if (p < len && s.charAt(p) == '#') {
            return new Node(Node.ERR, errorLiteral());
        }
        eat('$');
        String sheet = null;
        if (p < len && s.charAt(p) == '\'') {
            sheet = quoted();
        } else {
            int start = p;
            while (p < len && s.charAt(p) != '.' && s.charAt(p) != ']') {
                p++;
            }
            if (p > start) {
                sheet = s.substring(start, p);
            }
        }
        if (!eat('.')) {
            throw fail();
        }
        if (p < len && s.charAt(p) == '#') {
            return new Node(Node.ERR, errorLiteral());
        }
        Node n = cellOrColumns();
        if (n == null) {
            // half of a whole column or whole row reference: `.A` or `.1`
            boolean abs = eat('$');
            int start = p;
            n = new Node(Node.REF);
            if (p < len && isLetter(s.charAt(p))) {
                while (p < len && isLetter(s.charAt(p))) {
                    p++;
                }
                n.wholeCol = true;
                n.c1 = column(s, start, p);
                n.c2 = n.c1;
                n.r2 = Node.MAX_ROW;
                n.absC1 = abs;
                n.absC2 = abs;
            } else {
                while (p < len && isDigit(s.charAt(p))) {
                    p++;
                }
                if (p == start || p - start > 7) {
                    throw fail();
                }
                n.wholeRow = true;
                n.r1 = Integer.parseInt(s.substring(start, p)) - 1;
                n.r2 = n.r1;
                n.c2 = Node.MAX_COL;
                n.absR1 = abs;
                n.absR2 = abs;
            }
            if (n.c1 > Node.MAX_COL || n.r1 < 0 || n.r1 > Node.MAX_ROW) {
                throw fail();
            }
        }
        n.sheet = sheet;
        return n;
    }

    private Node function(String id) {
        String name = canonicalFunctionName(id);
        if (ods) {
            name = Functions.fromOds(name);
        }
        ArrayList<Node> args = new ArrayList<Node>();
        ws();
        if (eat(')')) {
            return new Node(Node.FUNC, name, new Node[0]);
        }
        for (;;) {
            ws();
            if (p < len && (s.charAt(p) == argSeparator || s.charAt(p) == ')')) {
                args.add(new Node(Node.MISSING));
            } else {
                args.add(comparison());
            }
            ws();
            if (!eat(argSeparator)) {
                expect(')');
                break;
            }
        }
        Node[] array = new Node[args.size()];
        args.toArray(array);
        return new Node(Node.FUNC, name, array);
    }

    /// Function names are upper case in the tree, without the prefixes the files put on names
    /// newer than the format (`_xlfn.XLOOKUP`, `COM.MICROSOFT.XLOOKUP`).
    static String canonicalFunctionName(String id) {
        String name = asciiUpper(id);
        for (String prefix : PREFIXES) {
            if (name.startsWith(prefix)) {
                return name.substring(prefix.length());
            }
        }
        return name;
    }

    private Node arrayConstant() {
        p++;
        char colSeparator = ods ? ';' : ',';
        char rowSeparator = ods ? '|' : ';';
        ArrayList<Object[]> rows = new ArrayList<Object[]>();
        ArrayList<Object> row = new ArrayList<Object>();
        for (;;) {
            ws();
            row.add(arrayElement());
            ws();
            if (eat(colSeparator)) {
                continue;
            }
            rows.add(row.toArray());
            row = new ArrayList<Object>();
            if (!eat(rowSeparator)) {
                expect('}');
                break;
            }
        }
        Node n = new Node(Node.ARRAY);
        n.array = new Object[rows.size()][];
        rows.toArray(n.array);
        int width = n.array[0].length;
        for (int i = 1; i < n.array.length; i++) {
            if (n.array[i].length != width) {
                throw fail();
            }
        }
        return n;
    }

    private Object arrayElement() {
        if (p >= len) {
            throw fail();
        }
        char c = s.charAt(p);
        if (c == '"') {
            return string();
        }
        if (c == '#') {
            return errorLiteral();
        }
        if (c == '-' || c == '+') {
            p++;
            double d = number().doubleValue();
            return Double.valueOf(c == '-' ? -d : d);
        }
        if (isDigit(c) || c == '.') {
            return number();
        }
        if (s.regionMatches(true, p, "TRUE", 0, 4)) {
            p += 4;
            return Boolean.TRUE;
        }
        if (s.regionMatches(true, p, "FALSE", 0, 5)) {
            p += 5;
            return Boolean.FALSE;
        }
        throw fail();
    }

    private CellError errorLiteral() {
        for (String e : ERRORS) {
            if (s.regionMatches(true, p, e, 0, e.length())) {
                p += e.length();
                return CellError.valueOf(e);
            }
        }
        throw fail();
    }

    private Double number() {
        int start = p;
        while (p < len && isDigit(s.charAt(p))) {
            p++;
        }
        if (p < len && s.charAt(p) == '.') {
            p++;
            while (p < len && isDigit(s.charAt(p))) {
                p++;
            }
        }
        if (p < len && (s.charAt(p) == 'E' || s.charAt(p) == 'e')) {
            int q = p + 1;
            if (q < len && (s.charAt(q) == '+' || s.charAt(q) == '-')) {
                q++;
            }
            if (q < len && isDigit(s.charAt(q))) {
                while (q < len && isDigit(s.charAt(q))) {
                    q++;
                }
                p = q;
            }
        }
        String text = s.substring(start, p);
        if (text.length() == 0 || ".".equals(text)) {
            throw fail();
        }
        try {
            return Double.valueOf(text);
        } catch (NumberFormatException err) {
            throw fail();
        }
    }

    private String string() {
        p++;
        StringBuilder sb = new StringBuilder();
        while (p < len) {
            char c = s.charAt(p++);
            if (c == '"') {
                if (p < len && s.charAt(p) == '"') {
                    sb.append('"');
                    p++;
                } else {
                    return sb.toString();
                }
            } else {
                sb.append(c);
            }
        }
        throw fail();
    }

    private String quoted() {
        p++;
        StringBuilder sb = new StringBuilder();
        while (p < len) {
            char c = s.charAt(p++);
            if (c == '\'') {
                if (p < len && s.charAt(p) == '\'') {
                    sb.append('\'');
                    p++;
                } else {
                    return sb.toString();
                }
            } else {
                sb.append(c);
            }
        }
        throw fail();
    }

    static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    static boolean isLetter(char c) {
        return c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z';
    }

    private static boolean isIdentifierPart(char c) {
        return isLetter(c) || isDigit(c) || c == '_' || c == '.' || c == '$' || c == '\\' || c == '?' || c > 127;
    }

    /// Function names and column letters are ASCII by specification; `toUpperCase()` would
    /// fold them by the device's locale.
    static String asciiUpper(String text) {
        int n = text.length();
        StringBuilder sb = null;
        for (int i = 0; i < n; i++) {
            char c = text.charAt(i);
            if (c >= 'a' && c <= 'z') {
                if (sb == null) {
                    sb = new StringBuilder(n);
                    sb.append(text.substring(0, i));
                }
                sb.append((char) (c - 32));
            } else if (sb != null) {
                sb.append(c);
            }
        }
        return sb == null ? text : sb.toString();
    }

    /// The zero based column of letters such as `A` or `xfd`.
    static int column(String text, int from, int to) {
        int col = 0;
        for (int i = from; i < to; i++) {
            char c = text.charAt(i);
            if (c >= 'a') {
                c -= 32;
            }
            col = col * 26 + (c - 'A' + 1);
        }
        return col - 1;
    }

    static void appendColumn(StringBuilder sb, int col) {
        int n = col + 1;
        int start = sb.length();
        while (n > 0) {
            int rem = (n - 1) % 26;
            sb.insert(start, (char) ('A' + rem));
            n = (n - 1) / 26;
        }
    }

    // ------------------------------------------------------------------ writing

    /// Writes a tree as formula text, without the leading equals sign.
    static String print(Node n, boolean ods) {
        StringBuilder sb = new StringBuilder();
        print(sb, n, ods, true);
        return sb.toString();
    }

    /// Writes a tree the way a formula is shown and typed: Excel's syntax, and function
    /// names without the prefixes files give to the newer ones.
    static String display(Node n) {
        StringBuilder sb = new StringBuilder();
        print(sb, n, false, false);
        return sb.toString();
    }

    private static void print(StringBuilder sb, Node n, boolean ods, boolean prefixes) {
        switch (n.kind) {
            case Node.NUM:
                sb.append(numberText(((Double) n.value).doubleValue()));
                break;
            case Node.STR:
                appendString(sb, n.name());
                break;
            case Node.BOOL:
                sb.append(((Boolean) n.value).booleanValue() ? "TRUE" : "FALSE");
                break;
            case Node.ERR:
                if (ods && n.value == CellError.REF) { //NOPMD CompareObjectsWithEquals - an error value is a constant
                    sb.append("[.#REF!]");
                } else {
                    sb.append(n.value.toString());
                }
                break;
            case Node.REF:
                printReference(sb, n, ods);
                break;
            case Node.NAME:
                sb.append(n.name());
                break;
            case Node.FUNC:
                sb.append(!prefixes ? n.name() : ods ? Functions.toOds(n.name())
                        : Functions.filePrefix(n.name(), false) + n.name()).append('(');
                for (int i = 0; i < n.args.length; i++) {
                    if (i > 0) {
                        sb.append(ods ? ';' : ',');
                    }
                    print(sb, n.args[i], ods, prefixes);
                }
                sb.append(')');
                break;
            case Node.BINARY:
                print(sb, n.args[0], ods, prefixes);
                String op = n.name();
                if (ods && ",".equals(op)) {
                    sb.append('~');
                } else if (ods && " ".equals(op)) {
                    sb.append('!');
                } else {
                    sb.append(op);
                }
                print(sb, n.args[1], ods, prefixes);
                break;
            case Node.UNARY:
                sb.append(n.name());
                print(sb, n.args[0], ods, prefixes);
                break;
            case Node.PERCENT:
                print(sb, n.args[0], ods, prefixes);
                sb.append('%');
                break;
            case Node.PAREN:
                sb.append('(');
                print(sb, n.args[0], ods, prefixes);
                sb.append(')');
                break;
            case Node.ARRAY:
                sb.append('{');
                for (int r = 0; r < n.array.length; r++) {
                    if (r > 0) {
                        sb.append(ods ? '|' : ';');
                    }
                    for (int c = 0; c < n.array[r].length; c++) {
                        if (c > 0) {
                            sb.append(ods ? ';' : ',');
                        }
                        Object v = n.array[r][c];
                        if (v instanceof String) {
                            appendString(sb, (String) v);
                        } else if (v instanceof Double) {
                            sb.append(numberText(((Double) v).doubleValue()));
                        } else if (v instanceof Boolean) {
                            sb.append(((Boolean) v).booleanValue() ? "TRUE" : "FALSE");
                        } else {
                            sb.append(v);
                        }
                    }
                }
                sb.append('}');
                break;
            default:
                break;
        }
    }

    private static void appendString(StringBuilder sb, String text) {
        sb.append('"');
        int n = text.length();
        for (int i = 0; i < n; i++) {
            char c = text.charAt(i);
            if (c == '"') {
                sb.append('"');
            }
            sb.append(c);
        }
        sb.append('"');
    }

    /// A number the way a formula spells it: no trailing `.0`, and never the grouping or the
    /// decimal comma of a locale.
    static String numberText(double d) {
        if (d == Math.floor(d) && Math.abs(d) < 1e15) {
            return Long.toString((long) d);
        }
        return Double.toString(d);
    }

    private static void printReference(StringBuilder sb, Node n, boolean ods) {
        if (ods) {
            sb.append('[');
            if (n.sheet != null) {
                sb.append('$');
                appendSheetName(sb, n.sheet);
            }
            sb.append('.');
            printCorner(sb, n, true);
            if (n.area) {
                sb.append(':');
                if (n.sheet2 != null) {
                    sb.append('$');
                    appendSheetName(sb, n.sheet2);
                }
                sb.append('.');
                printCorner(sb, n, false);
            }
            sb.append(']');
            return;
        }
        if (n.sheet != null) {
            if (n.sheet2 != null) {
                // a quoted span is quoted as a whole: 'First Sheet:Last Sheet'!A1
                if (needsQuotes(n.sheet) || needsQuotes(n.sheet2)) {
                    sb.append('\'').append(escapeQuote(n.sheet)).append(':').append(escapeQuote(n.sheet2)).append('\'');
                } else {
                    sb.append(n.sheet).append(':').append(n.sheet2);
                }
            } else {
                appendSheetName(sb, n.sheet);
            }
            sb.append('!');
        }
        printCorner(sb, n, true);
        if (n.area) {
            sb.append(':');
            printCorner(sb, n, false);
        }
    }

    private static void printCorner(StringBuilder sb, Node n, boolean first) {
        if (!n.wholeRow) {
            if (first ? n.absC1 : n.absC2) {
                sb.append('$');
            }
            appendColumn(sb, first ? n.c1 : n.c2);
        }
        if (!n.wholeCol) {
            if (first ? n.absR1 : n.absR2) {
                sb.append('$');
            }
            sb.append((first ? n.r1 : n.r2) + 1);
        }
    }

    static void appendSheetName(StringBuilder sb, String name) {
        if (needsQuotes(name)) {
            sb.append('\'').append(escapeQuote(name)).append('\'');
        } else {
            sb.append(name);
        }
    }

    private static String escapeQuote(String name) {
        if (name.indexOf('\'') < 0) {
            return name;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '\'') {
                sb.append('\'');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static boolean needsQuotes(String name) {
        int n = name.length();
        if (n == 0 || isDigit(name.charAt(0))) {
            return true;
        }
        for (int i = 0; i < n; i++) {
            char c = name.charAt(i);
            if (!isLetter(c) && !isDigit(c) && c != '_') {
                return true;
            }
        }
        // a sheet called A1 or TRUE would read as a cell or a literal
        return looksLikeCellOrLiteral(name);
    }

    private static boolean looksLikeCellOrLiteral(String name) {
        if ("TRUE".equalsIgnoreCase(name) || "FALSE".equalsIgnoreCase(name)) {
            return true;
        }
        int i = 0;
        int n = name.length();
        while (i < n && isLetter(name.charAt(i))) {
            i++;
        }
        if (i == 0 || i > 3 || i == n) {
            return false;
        }
        while (i < n && isDigit(name.charAt(i))) {
            i++;
        }
        return i == n;
    }

    // ------------------------------------------------------------------ moving

    /// The formula as it reads when its cell is moved by a number of rows and columns:
    /// relative references follow, absolute ones stay. This is how a shared formula and a
    /// filled-down formula are derived from the one that was written.
    static Node shift(Node n, int dr, int dc) {
        if (n.kind == Node.REF) {
            Node m = n.copyRef();
            if (!n.wholeCol) {
                if (!n.absR1) {
                    m.r1 += dr;
                }
                if (!n.absR2) {
                    m.r2 += dr;
                }
            }
            if (!n.wholeRow) {
                if (!n.absC1) {
                    m.c1 += dc;
                }
                if (!n.absC2) {
                    m.c2 += dc;
                }
            }
            if (m.r1 < 0 || m.c1 < 0 || m.r2 > Node.MAX_ROW || m.c2 > Node.MAX_COL || m.r2 < 0 || m.c2 < 0
                    || m.r1 > Node.MAX_ROW || m.c1 > Node.MAX_COL) {
                return new Node(Node.ERR, CellError.REF);
            }
            return m;
        }
        if (n.args == null || n.args.length == 0) {
            return n;
        }
        Node[] args = new Node[n.args.length];
        for (int i = 0; i < args.length; i++) {
            args[i] = shift(n.args[i], dr, dc);
        }
        Node m = new Node(n.kind, n.value, args);
        m.array = n.array;
        return m;
    }

    /// The formula as it reads after rows or columns were inserted into, or removed from, a
    /// sheet. References into that sheet move with their cells; a reference whose cells were
    /// all removed becomes `#REF!`.
    ///
    /// #### Parameters
    ///
    /// - `n`: the formula
    ///
    /// - `refersToEdited`: whether a reference with no sheet name points at the edited sheet
    ///
    /// - `editedSheet`: the name of the edited sheet
    ///
    /// - `rows`: true for rows, false for columns
    ///
    /// - `at`: the first inserted or removed index
    ///
    /// - `delta`: the number inserted, or minus the number removed
    static Node restructure(Node n, boolean refersToEdited, String editedSheet, boolean rows, int at, int delta) {
        if (n.kind == Node.REF) {
            boolean hit = n.sheet == null ? refersToEdited : n.sheet.equalsIgnoreCase(editedSheet) && n.sheet2 == null;
            if (!hit || rows && n.wholeCol || !rows && n.wholeRow) {
                return n;
            }
            int a = rows ? n.r1 : n.c1;
            int b = rows ? n.r2 : n.c2;
            int max = rows ? Node.MAX_ROW : Node.MAX_COL;
            if (delta > 0) {
                if (a >= at) {
                    a += delta;
                }
                if (b >= at) {
                    b += delta;
                }
            } else {
                int count = -delta;
                int end = at + count;
                if (a >= at && b < end) {
                    return new Node(Node.ERR, CellError.REF);
                }
                if (a >= end) {
                    a -= count;
                } else if (a >= at) {
                    a = at;
                }
                if (b >= end) {
                    b -= count;
                } else if (b >= at) {
                    b = at - 1;
                }
            }
            if (b > max || a > max) {
                return new Node(Node.ERR, CellError.REF);
            }
            Node m = n.copyRef();
            if (rows) {
                m.r1 = a;
                m.r2 = b;
            } else {
                m.c1 = a;
                m.c2 = b;
            }
            return m;
        }
        if (n.args == null || n.args.length == 0) {
            return n;
        }
        Node[] args = new Node[n.args.length];
        boolean changed = false;
        for (int i = 0; i < args.length; i++) {
            args[i] = restructure(n.args[i], refersToEdited, editedSheet, rows, at, delta);
            changed |= args[i] != n.args[i]; //NOPMD CompareObjectsWithEquals - the same object is meant, not an equal one
        }
        if (!changed) {
            return n;
        }
        Node m = new Node(n.kind, n.value, args);
        m.array = n.array;
        return m;
    }
}
