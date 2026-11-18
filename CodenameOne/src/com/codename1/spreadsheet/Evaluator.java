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

import com.codename1.util.MathUtil;
import java.util.ArrayList;

/// Evaluates formula trees.
///
/// A formula evaluates to a scalar (Double, String, Boolean, CellError, or null for an empty
/// cell), to a reference (`Ref`) or to an array (`Object[][]` of scalars). Operators and the
/// functions registered as scalar are applied element by element when an operand is an array
/// or a range, which is what makes `SUMPRODUCT((A1:A9="x")*B1:B9)` work.
///
/// A range used where one value is expected, outside the arguments of a function and outside
/// an array formula, stands for the cell of that range in the formula's own row or column.
/// That is the meaning such a formula has in a file.
final class Evaluator {
    /// An argument that was left out, as the second one of `IF(A1,,0)`.
    static final Object MISSING = new Object();

    final Workbook workbook;
    Sheet sheet;
    int row;
    int col;
    private boolean arrayFormula;
    private int argDepth;
    private int nameDepth;
    boolean skipSubtotals;
    private final ArrayList<String> letNames = new ArrayList<String>();
    private final ArrayList<Object> letValues = new ArrayList<Object>();

    Evaluator(Workbook workbook) {
        this.workbook = workbook;
    }

    /// Evaluates a formula as the content of a cell and answers the one scalar the cell shows.
    Object evaluate(Node formula, Sheet inSheet, int inRow, int inCol, boolean asArrayFormula) {
        Sheet savedSheet = sheet;
        int savedRow = row;
        int savedCol = col;
        boolean savedArray = arrayFormula;
        int savedDepth = argDepth;
        int savedLets = letNames.size();
        sheet = inSheet;
        row = inRow;
        col = inCol;
        arrayFormula = asArrayFormula;
        argDepth = 0;
        try {
            return result(eval(formula));
        } catch (EvalError e) {
            return e.error; //NOPMD UnnecessaryLocalBeforeReturn - the caught error carries the value
        } finally {
            sheet = savedSheet;
            row = savedRow;
            col = savedCol;
            arrayFormula = savedArray;
            argDepth = savedDepth;
            while (letNames.size() > savedLets) {
                letNames.remove(letNames.size() - 1);
                letValues.remove(letValues.size() - 1);
            }
        }
    }

    private Object result(Object v) {
        if (v instanceof Ref) {
            Ref r = (Ref) v;
            if (r.isSingleCell()) {
                v = value(r.sheet, r.r1, r.c1);
            } else if (!arrayFormula) {
                v = intersectImplicitly(r);
            } else {
                v = value(r.sheet, r.r1, r.c1);
            }
        } else if (v instanceof Object[][]) {
            Object[][] m = (Object[][]) v;
            v = m.length == 0 || m[0].length == 0 ? null : m[0][0];
        }
        if (v == null || v == MISSING) {
            return Double.valueOf(0);
        }
        if (v instanceof Double) {
            double d = ((Double) v).doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                return CellError.NUM;
            }
        }
        return v;
    }

    private Object intersectImplicitly(Ref r) {
        if (r.next == null && r.lastSheet == null) {
            if (r.c1 == r.c2 && row >= r.r1 && row <= r.r2) {
                return value(r.sheet, row, r.c1);
            }
            if (r.r1 == r.r2 && col >= r.c1 && col <= r.c2) {
                return value(r.sheet, r.r1, col);
            }
        }
        return CellError.VALUE;
    }

    /// The value of a cell, calculated first if an edit made it stale.
    Object value(Sheet s, int r, int c) {
        return workbook.valueOf(s.cell(r, c));
    }

    // ------------------------------------------------------------------ the tree

    Object eval(Node n) {
        switch (n.kind) {
            case Node.NUM:
            case Node.STR:
            case Node.BOOL:
            case Node.ERR:
                return n.value;
            case Node.MISSING:
                return MISSING;
            case Node.REF:
                return reference(n);
            case Node.NAME:
                return name(n.name());
            case Node.PAREN:
                return eval(n.args[0]);
            case Node.ARRAY:
                return n.array;
            case Node.UNARY:
                Object u = operand(eval(n.args[0]));
                return "-".equals(n.value) ? lift('n', u, null) : u;
            case Node.PERCENT:
                return lift('%', operand(eval(n.args[0])), null);
            case Node.BINARY:
                return binary(n);
            case Node.FUNC:
                return call(n);
            default:
                return CellError.VALUE;
        }
    }

    private Object reference(Node n) {
        Sheet s = sheet;
        Sheet last = null;
        if (n.sheet != null) {
            s = workbook.getSheet(n.sheet);
            if (s == null) {
                return CellError.REF;
            }
            if (n.sheet2 != null) {
                last = workbook.getSheet(n.sheet2);
                if (last == null) {
                    return CellError.REF;
                }
                if (workbook.indexOf(last) < workbook.indexOf(s)) {
                    Sheet swap = s;
                    s = last;
                    last = swap;
                }
            }
        }
        return new Ref(s, last, n.r1, n.c1, n.r2, n.c2);
    }

    private Object name(String name) {
        for (int i = letNames.size() - 1; i >= 0; i--) {
            if (letNames.get(i).equalsIgnoreCase(name)) {
                return letValues.get(i);
            }
        }
        Node definition = workbook.definedNameNode(name);
        if (definition == null || nameDepth > 32) {
            return CellError.NAME;
        }
        nameDepth++;
        try {
            return eval(definition);
        } finally {
            nameDepth--;
        }
    }

    private Object binary(Node n) {
        String op = n.name();
        char c = op.charAt(0);
        if (c == ':' || c == ' ' || c == ',') {
            Object a = eval(n.args[0]);
            Object b = eval(n.args[1]);
            if (a instanceof CellError) {
                return a;
            }
            if (b instanceof CellError) {
                return b;
            }
            if (!(a instanceof Ref) || !(b instanceof Ref)) {
                return CellError.VALUE;
            }
            Ref x = (Ref) a;
            Ref y = (Ref) b;
            if (c == ',') {
                Ref head = new Ref(x.sheet, x.lastSheet, x.r1, x.c1, x.r2, x.c2);
                Ref tail = head;
                for (Ref r = x.next; r != null; r = r.next) {
                    tail.next = new Ref(r.sheet, r.lastSheet, r.r1, r.c1, r.r2, r.c2);
                    tail = tail.next;
                }
                tail.next = y;
                return head;
            }
            if (x.sheet != y.sheet || x.next != null || y.next != null) { //NOPMD CompareObjectsWithEquals - a sheet is an identity
                return CellError.VALUE;
            }
            if (c == ':') {
                return new Ref(x.sheet, x.lastSheet, Math.min(x.r1, y.r1), Math.min(x.c1, y.c1), Math.max(x.r2, y.r2),
                        Math.max(x.c2, y.c2));
            }
            int r1 = Math.max(x.r1, y.r1);
            int c1 = Math.max(x.c1, y.c1);
            int r2 = Math.min(x.r2, y.r2);
            int c2 = Math.min(x.c2, y.c2);
            if (r1 > r2 || c1 > c2) {
                return CellError.NULL;
            }
            return new Ref(x.sheet, null, r1, c1, r2, c2);
        }
        char code = c;
        if (op.length() == 2) {
            // <> <= >=
            code = c == '<' ? (op.charAt(1) == '>' ? '!' : 'l') : 'g';
        }
        return lift(code, operand(eval(n.args[0])), operand(eval(n.args[1])));
    }

    /// Turns what an operand evaluated to into a scalar or an array.
    Object operand(Object v) {
        if (v instanceof Ref) {
            Ref r = (Ref) v;
            if (r.isSingleCell()) {
                return value(r.sheet, r.r1, r.c1);
            }
            if (argDepth == 0 && !arrayFormula) {
                return intersectImplicitly(r);
            }
            return matrix(r);
        }
        return v == MISSING ? null : v;
    }

    /// Applies an operator, element by element when an operand is an array. A one row or one
    /// column operand is repeated along the other dimension.
    private Object lift(char op, Object a, Object b) {
        boolean arrayA = a instanceof Object[][];
        boolean arrayB = b instanceof Object[][];
        if (!arrayA && !arrayB) {
            return apply(op, a, b);
        }
        Object[][] x = arrayA ? (Object[][]) a : null;
        Object[][] y = arrayB ? (Object[][]) b : null;
        int rows = Math.max(x == null ? 1 : x.length, y == null ? 1 : y.length);
        int cols = Math.max(x == null ? 1 : width(x), y == null ? 1 : width(y));
        Object[][] out = new Object[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                out[r][c] = apply(op, x == null ? a : element(x, r, c), y == null ? b : element(y, r, c));
            }
        }
        return out;
    }

    static int width(Object[][] m) {
        return m.length == 0 ? 0 : m[0].length;
    }

    static int width2(boolean[][] m) {
        return m.length == 0 ? 0 : m[0].length;
    }

    /// The values of a range that go with a mask of another range: the same shape, counted
    /// from the top left cell. A range of another shape is read as if it had the mask's.
    Object[][] shaped(Object arg, int rows, int cols) {
        if (arg instanceof Ref) {
            Ref r = (Ref) arg;
            Object[][] out = new Object[rows][cols];
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    out[i][j] = value(r.sheet, r.r1 + i, r.c1 + j);
                }
            }
            return out;
        }
        Object[][] m = matrix(arg);
        if (m.length != rows || width(m) != cols) {
            throw EvalError.VALUE;
        }
        return m;
    }

    /// The results XLOOKUP picks from: as many rows, or columns, as there are keys.
    Object[][] shaped2(Object arg, boolean down, int count) {
        if (arg instanceof CellError) {
            throw EvalError.of((CellError) arg);
        }
        if (arg instanceof Ref) {
            Ref r = (Ref) arg;
            return down ? shaped(r, count, Math.min(r.cols(), Math.max(1, r.clip().cols())))
                    : shaped(r, Math.min(r.rows(), Math.max(1, r.clip().rows())), count);
        }
        Object[][] m = matrix(arg);
        if ((down ? m.length : width(m)) != count) {
            throw EvalError.VALUE;
        }
        return m;
    }

    /// The element of an array at a position of a larger array it is combined with.
    static Object element(Object[][] m, int r, int c) {
        if (m.length == 0) {
            return CellError.NA;
        }
        if (m.length == 1) {
            r = 0;
        }
        if (m[0].length == 1) {
            c = 0;
        }
        if (r >= m.length || c >= m[r].length) {
            return CellError.NA;
        }
        return m[r][c];
    }

    private static Object apply(char op, Object a, Object b) {
        if (a instanceof CellError) {
            return a;
        }
        if (b instanceof CellError) {
            return b;
        }
        try {
            switch (op) {
                case '+':
                    return number(Values.num(a) + Values.num(b));
                case '-':
                    return number(Values.num(a) - Values.num(b));
                case '*':
                    return number(Values.num(a) * Values.num(b));
                case '/': {
                    double x = Values.num(a);
                    double y = Values.num(b);
                    if (y == 0) {
                        return CellError.DIV0;
                    }
                    return number(x / y);
                }
                case '^':
                    return number(power(Values.num(a), Values.num(b)));
                case 'n':
                    return number(-Values.num(a));
                case '%':
                    return number(Values.num(a) / 100);
                case '&':
                    return Values.text(a) + Values.text(b);
                case '=':
                    return Boolean.valueOf(Values.compare(a, b) == 0);
                case '!':
                    return Boolean.valueOf(Values.compare(a, b) != 0);
                case '<':
                    return Boolean.valueOf(Values.compare(a, b) < 0);
                case '>':
                    return Boolean.valueOf(Values.compare(a, b) > 0);
                case 'l':
                    return Boolean.valueOf(Values.compare(a, b) <= 0);
                case 'g':
                    return Boolean.valueOf(Values.compare(a, b) >= 0);
                default:
                    return CellError.VALUE;
            }
        } catch (EvalError e) {
            return e.error; //NOPMD UnnecessaryLocalBeforeReturn - the caught error carries the value
        }
    }

    static Object number(double d) {
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            return CellError.NUM;
        }
        return Double.valueOf(d);
    }

    /// A power. Whole exponents are multiplied out, so that 2^10 is exactly 1024 and 10^-2
    /// exactly what 1/100 is, whatever the platform's `pow` rounds to.
    static double power(double x, double y) {
        if (x == 0 && y == 0) {
            throw EvalError.NUM;
        }
        if (x == 0 && y < 0) {
            throw EvalError.DIV0;
        }
        if (y == Math.floor(y) && Math.abs(y) <= 1024) {
            int n = (int) Math.abs(y);
            double result = 1;
            double base = x;
            while (n > 0) {
                if ((n & 1) == 1) {
                    result *= base;
                }
                base *= base;
                n >>= 1;
            }
            return y < 0 ? 1 / result : result;
        }
        if (x < 0) {
            throw EvalError.NUM;
        }
        return MathUtil.pow(x, y);
    }

    // ------------------------------------------------------------------ functions

    private Object call(Node n) {
        String name = n.name();
        int info = Functions.info(name);
        if (info < 0) {
            return CellError.NAME;
        }
        Node[] a = n.args;
        argDepth++;
        try {
            if ((info & Functions.CATEGORY) == Functions.SPECIAL) {
                return special(name, a);
            }
            Object[] args = new Object[a.length];
            for (int i = 0; i < a.length; i++) {
                args[i] = eval(a[i]);
            }
            if ((info & Functions.SCALAR) != 0) {
                return liftCall(info, name, args);
            }
            return Functions.call(info, name, this, args);
        } catch (EvalError e) {
            return e.error; //NOPMD UnnecessaryLocalBeforeReturn - the caught error carries the value
        } finally {
            argDepth--;
        }
    }

    /// Calls a function of scalars, once per element when an argument is an array. Such a
    /// function never sees an error, an array or a reference: an error argument is the
    /// result.
    private Object liftCall(int info, String name, Object[] args) {
        int rows = 0;
        int cols = 0;
        for (int i = 0; i < args.length; i++) {
            Object v = args[i];
            if (v instanceof Ref) {
                Ref r = (Ref) v;
                v = r.isSingleCell() ? value(r.sheet, r.r1, r.c1) : (Object) matrix(r);
                args[i] = v;
            }
            if (v instanceof Object[][]) {
                Object[][] m = (Object[][]) v;
                rows = Math.max(rows, m.length);
                cols = Math.max(cols, width(m));
            }
        }
        if (rows == 0) {
            return callScalar(info, name, args);
        }
        Object[][] out = new Object[rows][cols];
        Object[] scalars = new Object[args.length];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                for (int i = 0; i < args.length; i++) {
                    Object v = args[i];
                    scalars[i] = v instanceof Object[][] ? element((Object[][]) v, r, c) : v;
                }
                out[r][c] = callScalar(info, name, scalars);
            }
        }
        return out;
    }

    private Object callScalar(int info, String name, Object[] args) {
        for (Object arg : args) {
            if (arg instanceof CellError) {
                return arg;
            }
        }
        try {
            Object v = Functions.call(info, name, this, args);
            if (v instanceof Double) {
                return number(((Double) v).doubleValue());
            }
            return v;
        } catch (EvalError e) {
            return e.error; //NOPMD UnnecessaryLocalBeforeReturn - the caught error carries the value
        }
    }

    /// The functions that decide which of their arguments to evaluate, or that need an
    /// argument unevaluated.
    private Object special(String name, Node[] a) {
        switch (Functions.index(SWITCH_1, name)) {
            case 0: // IF
                return ifFunction(a);
            case 1: // IFERROR
            case 2: { // IFNA
                if (a.length < 1) {
                    return CellError.VALUE;
                }
                Object v = operand(eval(a[0]));
                boolean onlyNa = "IFNA".equals(name);
                if (v instanceof Object[][]) {
                    Object[][] m = (Object[][]) v;
                    Object other = a.length > 1 ? operand(eval(a[1])) : null;
                    Object[][] out = new Object[m.length][width(m)];
                    for (int r = 0; r < m.length; r++) {
                        for (int c = 0; c < out[r].length; c++) {
                            Object e = m[r][c];
                            boolean replace = onlyNa ? e == CellError.NA : e instanceof CellError; //NOPMD CompareObjectsWithEquals - an error value is a constant
                            out[r][c] = !replace ? e : other instanceof Object[][] ? element((Object[][]) other, r, c)
                                    : other;
                        }
                    }
                    return out;
                }
                boolean replace = onlyNa ? v == CellError.NA : v instanceof CellError; //NOPMD CompareObjectsWithEquals - an error value is a constant
                if (!replace) {
                    return v;
                }
                return a.length > 1 ? blankToEmpty(eval(a[1])) : "";
            }
            case 3: // IFS
                for (int i = 0; i + 1 < a.length; i += 2) {
                    Object cond = scalar(eval(a[i]));
                    if (cond instanceof CellError) {
                        return cond;
                    }
                    if (Values.bool(cond)) {
                        return eval(a[i + 1]);
                    }
                }
                return CellError.NA;
            case 4: { // SWITCH
                if (a.length < 3) {
                    return CellError.VALUE;
                }
                Object subject = scalar(eval(a[0]));
                if (subject instanceof CellError) {
                    return subject;
                }
                int i = 1;
                for (; i + 1 < a.length; i += 2) {
                    Object candidate = scalar(eval(a[i]));
                    if (candidate instanceof CellError) {
                        return candidate;
                    }
                    if (Values.compare(subject, candidate) == 0) {
                        return eval(a[i + 1]);
                    }
                }
                return i < a.length ? eval(a[i]) : CellError.NA;
            }
            case 5: { // CHOOSE
                if (a.length < 2) {
                    return CellError.VALUE;
                }
                Object index = scalar(eval(a[0]));
                if (index instanceof CellError) {
                    return index;
                }
                int i = (int) Math.floor(Values.num(index));
                if (i < 1 || i >= a.length) {
                    return CellError.VALUE;
                }
                return eval(a[i]);
            }
            case 6: { // LET
                if (a.length < 3 || (a.length & 1) == 0) {
                    return CellError.VALUE;
                }
                int mark = letNames.size();
                try {
                    for (int i = 0; i + 1 < a.length; i += 2) {
                        if (a[i].kind != Node.NAME && a[i].kind != Node.REF) {
                            return CellError.VALUE;
                        }
                        Object v = eval(a[i + 1]);
                        letNames.add(a[i].kind == Node.NAME ? a[i].name() : FormulaParser.print(a[i], false));
                        letValues.add(v);
                    }
                    return eval(a[a.length - 1]);
                } finally {
                    while (letNames.size() > mark) {
                        letNames.remove(letNames.size() - 1);
                        letValues.remove(letValues.size() - 1);
                    }
                }
            }
            case 7: // ROW
            case 8: { // COLUMN
                boolean rows = "ROW".equals(name);
                if (a.length == 0 || a[0].kind == Node.MISSING) {
                    return Double.valueOf((rows ? row : col) + 1);
                }
                Object v = eval(a[0]);
                if (!(v instanceof Ref)) {
                    return v instanceof CellError ? v : CellError.VALUE;
                }
                Ref r = (Ref) v;
                int count = rows ? r.rows() : r.cols();
                int first = rows ? r.r1 : r.c1;
                if (count == 1) {
                    return Double.valueOf(first + 1);
                }
                Object[][] out = rows ? new Object[count][1] : new Object[1][count];
                for (int i = 0; i < count; i++) {
                    if (rows) {
                        out[i][0] = Double.valueOf(first + i + 1);
                    } else {
                        out[0][i] = Double.valueOf(first + i + 1);
                    }
                }
                return out;
            }
            case 9: // ISFORMULA
            case 10: { // FORMULATEXT
                if (a.length != 1) {
                    return CellError.VALUE;
                }
                Object v = eval(a[0]);
                if (!(v instanceof Ref)) {
                    return v instanceof CellError ? v : CellError.VALUE;
                }
                Ref r = (Ref) v;
                Cell cell = r.sheet.cell(r.r1, r.c1);
                boolean formula = cell != null && cell.isFormula();
                if ("ISFORMULA".equals(name)) {
                    return Boolean.valueOf(formula);
                }
                return formula ? "=" + cell.formulaText : (Object) CellError.NA;
            }
            default:
                return CellError.NAME;
        }
    }

    private Object ifFunction(Node[] a) {
        if (a.length < 2) {
            return CellError.VALUE;
        }
        Object cond = operand(eval(a[0]));
        if (cond instanceof Object[][]) {
            Object[][] m = (Object[][]) cond;
            Object yes = operand(eval(a[1]));
            Object no = a.length > 2 ? operand(eval(a[2])) : Boolean.FALSE;
            Object[][] out = new Object[m.length][width(m)];
            for (int r = 0; r < m.length; r++) {
                for (int c = 0; c < out[r].length; c++) {
                    Object e = m[r][c];
                    if (e instanceof CellError) {
                        out[r][c] = e;
                        continue;
                    }
                    boolean pick;
                    try {
                        pick = Values.bool(e);
                    } catch (EvalError err) {
                        out[r][c] = err.error;
                        continue;
                    }
                    Object chosen = pick ? yes : no;
                    out[r][c] = chosen instanceof Object[][] ? element((Object[][]) chosen, r, c) : chosen;
                }
            }
            return out;
        }
        if (cond instanceof CellError) {
            return cond;
        }
        if (Values.bool(cond)) {
            return blankToZero(eval(a[1]));
        }
        return a.length > 2 ? blankToZero(eval(a[2])) : Boolean.FALSE;
    }

    private static Object blankToZero(Object v) {
        return v == MISSING ? Double.valueOf(0) : v;
    }

    private static Object blankToEmpty(Object v) {
        return v == MISSING ? "" : v;
    }

    // ------------------------------------------------------------------ helpers for functions

    /// One value out of whatever an argument evaluated to: the cell of a single cell
    /// reference, the first element of an array or a range.
    Object scalar(Object v) {
        if (v instanceof Ref) {
            Ref r = (Ref) v;
            return value(r.sheet, r.r1, r.c1);
        }
        if (v instanceof Object[][]) {
            Object[][] m = (Object[][]) v;
            return m.length == 0 || m[0].length == 0 ? null : m[0][0];
        }
        return v == MISSING ? null : v;
    }

    /// The values of an argument as rows and columns. A range is cut down to the rows and
    /// columns that hold data first.
    Object[][] matrix(Object v) {
        if (v instanceof Object[][]) {
            return (Object[][]) v;
        }
        if (v instanceof Ref) {
            Ref r = ((Ref) v).clip();
            int rows = r.rows();
            int cols = r.cols();
            Object[][] out = new Object[rows][cols];
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    out[i][j] = value(r.sheet, r.r1 + i, r.c1 + j);
                }
            }
            return out;
        }
        return new Object[][] {{v == MISSING ? null : v}};
    }

    /// Gathers the numbers of an argument the way SUM reads it. Typed straight into the
    /// formula, a logical value counts as 1 or 0 and text has to be a number; inside a range
    /// or an array, text, logical values and empty cells are skipped. An error anywhere is
    /// the result.
    void numbers(Object arg, DoubleList out, boolean countTextAndLogicals) {
        if (arg instanceof Ref) {
            for (Ref r = (Ref) arg; r != null; r = r.next) {
                int first = workbook.indexOf(r.sheet);
                int last = r.lastSheet == null ? first : workbook.indexOf(r.lastSheet);
                for (int s = first; s <= last; s++) {
                    Sheet sh = workbook.getSheet(s);
                    int maxRow = Math.min(r.r2, sh.usedRows() - 1);
                    for (int i = r.r1; i <= maxRow; i++) {
                        Cell[] cells = sh.rowCells(i);
                        if (cells == null) {
                            continue;
                        }
                        int maxCol = Math.min(r.c2, cells.length - 1);
                        for (int j = r.c1; j <= maxCol; j++) {
                            if (cells[j] != null && !(skipSubtotals && isSubtotal(cells[j]))) {
                                addRangeValue(workbook.valueOf(cells[j]), out, countTextAndLogicals);
                            }
                        }
                    }
                }
            }
        } else if (arg instanceof Object[][]) {
            Object[][] m = (Object[][]) arg;
            for (Object[] line : m) {
                for (Object value : line) {
                    addRangeValue(value, out, countTextAndLogicals);
                }
            }
        } else if (arg != MISSING) {
            if (arg == null) {
                return;
            }
            out.add(Values.num(arg));
        }
    }

    /// SUBTOTAL leaves out the cells that are themselves subtotals, so that a grand total over
    /// a column of sections does not count every row twice.
    private static boolean isSubtotal(Cell cell) {
        Node f = cell.formula;
        while (f != null && f.kind == Node.PAREN) {
            f = f.args[0];
        }
        return f != null && f.kind == Node.FUNC && ("SUBTOTAL".equals(f.value) || "AGGREGATE".equals(f.value));
    }

    private static void addRangeValue(Object v, DoubleList out, boolean countTextAndLogicals) {
        if (v instanceof Double) {
            out.add(((Double) v).doubleValue());
        } else if (v instanceof CellError) {
            throw EvalError.of((CellError) v);
        } else if (countTextAndLogicals && v != null) {
            out.add(v instanceof Boolean && ((Boolean) v).booleanValue() ? 1 : 0);
        }
    }

    DoubleList numbers(Object[] args, int from) {
        DoubleList list = new DoubleList();
        for (int i = from; i < args.length; i++) {
            numbers(args[i], list, false);
        }
        return list;
    }

    /// The argument at an index, MISSING when the call has fewer.
    static Object arg(Object[] a, int i) {
        return i < a.length ? a[i] : MISSING;
    }

    static boolean has(Object[] a, int i) {
        return i < a.length && a[i] != MISSING;
    }

    /// A required numeric argument of a function of scalars.
    static double num(Object[] a, int i) {
        if (i >= a.length) {
            throw EvalError.VALUE;
        }
        return Values.num(a[i]);
    }

    static double num(Object[] a, int i, double fallback) {
        return has(a, i) ? Values.num(a[i]) : fallback;
    }

    /// A numeric argument cut to a whole number, towards zero, as functions that take a count
    /// or an index do.
    static int integer(Object[] a, int i) {
        double d = num(a, i);
        if (d > Integer.MAX_VALUE || d < Integer.MIN_VALUE) {
            throw EvalError.NUM;
        }
        return (int) d;
    }

    static int integer(Object[] a, int i, int fallback) {
        return has(a, i) ? integer(a, i) : fallback;
    }

    static String text(Object[] a, int i) {
        if (i >= a.length) {
            throw EvalError.VALUE;
        }
        return Values.text(a[i]);
    }

    static boolean bool(Object[] a, int i, boolean fallback) {
        return has(a, i) ? Values.bool(a[i]) : fallback;
    }

    /// A numeric argument of a function that takes ranges: the one value of the argument.
    double scalarNum(Object[] a, int i) {
        if (i >= a.length) {
            throw EvalError.VALUE;
        }
        return Values.num(scalar(a[i]));
    }

    double scalarNum(Object[] a, int i, double fallback) {
        return has(a, i) ? Values.num(scalar(a[i])) : fallback;
    }

    String scalarText(Object[] a, int i) {
        if (i >= a.length) {
            throw EvalError.VALUE;
        }
        return Values.text(scalar(a[i]));
    }

    private static final java.util.HashMap<String, Integer> SWITCH_1 = Functions.switchTable(
            "IF", "IFERROR", "IFNA", "IFS", "SWITCH", "CHOOSE", "LET", "ROW", "COLUMN", "ISFORMULA",
            "FORMULATEXT");
}
