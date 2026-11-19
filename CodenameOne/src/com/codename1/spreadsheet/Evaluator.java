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
    /// Whether the formula being evaluated took a range or an array element by element where
    /// one value is wanted: Excel without dynamic arrays reads such a formula as an array
    /// formula only when the file says so, so the writers mark it as one and Excel shows what
    /// this evaluator calculated.
    private boolean spread;
    /// What spread was for the last formula evaluate finished.
    boolean lastSpread;
    /// The whole result of the last formula evaluate finished, when it was an array.
    Object[][] lastMatrix;
    private Object[][] matrixResult;
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
        boolean savedSpread = spread;
        spread = false;
        Object[][] savedMatrix = matrixResult;
        matrixResult = null;
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
            lastSpread = spread;
            spread = savedSpread;
            lastMatrix = matrixResult;
            matrixResult = savedMatrix;
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
                matrixResult = matrix(r);
                v = value(r.sheet, r.r1, r.c1);
            }
        } else if (v instanceof Object[][]) {
            Object[][] m = (Object[][]) v;
            matrixResult = m;
            spread = true;
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

    /// The most characters the texts of one array result hold together.
    static final long MAX_ARRAY_TEXT = 4L * 1024 * 1024;

    /// How deep evaluation may go, defined names included. One formula is held to
    /// FormulaParser.MAX_DEPTH, so a formula at its limit using a name at its limit fits;
    /// names chained further could nest past any stack, and past this the result is #NUM!,
    /// Excel's error for a calculation it cannot finish.
    static final int MAX_EVAL_DEPTH = 2 * FormulaParser.MAX_DEPTH + 32;

    private int evalDepth;

    Object eval(Node n) {
        if (evalDepth >= MAX_EVAL_DEPTH) {
            return CellError.NUM;
        }
        evalDepth++;
        try {
            return evalNode(n);
        } finally {
            evalDepth--;
        }
    }

    private Object evalNode(Node n) {
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
            return eval(FormulaParser.fromCell(definition, row, col));
        } finally {
            nameDepth--;
        }
    }

    private Object binary(Node top) {
        if (top.args[0].kind != Node.BINARY) {
            return binary(top, eval(top.args[0]));
        }
        // a chain, a+b+c+..., is walked and not recursed into: see Node.spine
        Node[] spine = Node.spine(top);
        Object a = eval(spine[0].args[0]);
        for (Node link : spine) {
            a = binary(link, a);
        }
        return a;
    }

    /// One operator, its left operand already evaluated.
    private Object binary(Node n, Object a) {
        String op = n.name();
        char c = op.charAt(0);
        if (c == ':' || c == ' ' || c == ',') {
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
        Object left = operand(a);
        return lift(code, left, operand(eval(n.args[1])));
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
        spread = true;
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
            // Excel answers #VALUE! when the results are not as long as the keys
            if ((down ? r.rows() : r.cols()) != count) {
                throw EvalError.VALUE;
            }
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
                case '&': {
                    String x = Values.text(a);
                    String y = Values.text(b);
                    if (x.length() + y.length() > TextFunctions.MAX_TEXT) {
                        return CellError.VALUE;
                    }
                    return x + y;
                }
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
            // an odd root of a negative number is a real one: (-8)^(1/3) is -2
            double root = 1 / y;
            double nearest = Math.floor(root + 0.5);
            if (Math.abs(nearest) < 1e9 && Math.abs(root - nearest) < 1e-9 * Math.abs(nearest) && nearest % 2 != 0) {
                return -MathUtil.pow(-x, y);
            }
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
            return callLifted(info, name, args);
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
                spread = true;
                rows = Math.max(rows, m.length);
                cols = Math.max(cols, width(m));
            }
        }
        if (rows == 0) {
            return callScalar(info, name, args);
        }
        Object[][] out = new Object[rows][cols];
        Object[] scalars = new Object[args.length];
        // Each element of REPT over a million-cell array may be the longest text there is;
        // what the whole array holds is capped as a cell's text is.
        long chars = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                for (int i = 0; i < args.length; i++) {
                    Object v = args[i];
                    scalars[i] = v instanceof Object[][] ? element((Object[][]) v, r, c) : v;
                }
                Object v = callScalar(info, name, scalars);
                if (v instanceof String) {
                    chars += ((String) v).length();
                    if (chars > MAX_ARRAY_TEXT) {
                        return CellError.VALUE;
                    }
                }
                out[r][c] = v;
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
                Object first = operand(eval(a[0]));
                if (first instanceof Object[][]) {
                    // an array of choices: one per element, as Excel 365 answers
                    Object[][] m = (Object[][]) first;
                    spread = true;
                    Object[][] out = new Object[m.length][width(m)];
                    for (int r = 0; r < m.length; r++) {
                        for (int c = 0; c < out[r].length; c++) {
                            Object e = m[r][c];
                            int k = e instanceof CellError ? -1 : (int) Math.floor(Values.num(e));
                            out[r][c] = e instanceof CellError ? e : k < 1 || k >= a.length ? CellError.VALUE
                                    : scalar(operand(eval(a[k])));
                        }
                    }
                    return out;
                }
                Object index = first;
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
            spread = true;
            Object yes = operand(eval(a[1]));
            Object no = a.length > 2 ? operand(eval(a[2])) : Boolean.FALSE;
            // A row of conditions against columns of values is a table of both, as the
            // operators make of them: IF({1,0},B1:B2,A1:A2) is two columns side by side.
            int rows = m.length;
            int cols = width(m);
            if (yes instanceof Object[][]) {
                rows = Math.max(rows, ((Object[][]) yes).length);
                cols = Math.max(cols, width((Object[][]) yes));
            }
            if (no instanceof Object[][]) {
                rows = Math.max(rows, ((Object[][]) no).length);
                cols = Math.max(cols, width((Object[][]) no));
            }
            Object[][] out = new Object[rows][cols];
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    Object e = element(m, r, c);
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
    /// Notes that the formula took an array where one value is wanted.
    void spreads() {
        spread = true;
    }

    /// The arguments of the function being called, so scalar can tell an argument that is an
    /// array from an array the function made itself.
    private Object[] callArgs;
    private int liftArg;

    /// Calls a function, and when it wants one value of an argument that holds several, calls
    /// it once per element instead and answers the array of results.
    private Object callLifted(int info, String name, Object[] args) {
        Object[] saved = callArgs;
        callArgs = args;
        try {
            return Functions.call(info, name, this, args);
        } catch (EvalError e) {
            if (e != EvalError.LIFT) { //NOPMD CompareObjectsWithEquals - the signal is a constant
                throw e;
            }
        } finally {
            callArgs = saved;
        }
        spread = true;
        int at = liftArg;
        Object[][] m = matrix(args[at]);
        Object[][] out = new Object[m.length][width(m)];
        for (int r = 0; r < m.length; r++) {
            for (int c = 0; c < out[r].length; c++) {
                Object[] one = new Object[args.length];
                System.arraycopy(args, 0, one, 0, args.length);
                one[at] = m[r][c];
                Object v;
                try {
                    v = callLifted(info, name, one);
                } catch (EvalError e) {
                    v = e.error;
                }
                if (v instanceof Object[][]) {
                    Object[][] inner = (Object[][]) v;
                    v = inner.length == 0 || inner[0].length == 0 ? null : inner[0][0];
                } else if (v instanceof Ref) {
                    v = scalar(v);
                }
                out[r][c] = v;
            }
        }
        return out;
    }

    private boolean isArgument(Object v) {
        if (callArgs == null) {
            return false;
        }
        for (int i = 0; i < callArgs.length; i++) {
            if (callArgs[i] == v) { //NOPMD CompareObjectsWithEquals - which argument, by identity
                liftArg = i;
                return true;
            }
        }
        return false;
    }

    Object scalar(Object v) {
        if (v instanceof Ref) {
            Ref r = (Ref) v;
            if (!r.isSingleCell() && r.next == null && r.lastSheet == null && isArgument(v)) {
                throw EvalError.LIFT;
            }
            return value(r.sheet, r.r1, r.c1);
        }
        if (v instanceof Object[][]) {
            Object[][] m = (Object[][]) v;
            if ((m.length > 1 || m.length == 1 && m[0].length > 1) && isArgument(v)) {
                throw EvalError.LIFT;
            }
            return m.length == 0 || m[0].length == 0 ? null : m[0][0];
        }
        return v == MISSING ? null : v;
    }

    /// The values of an argument as one matrix per area: each part of a union and each
    /// sheet of a 3D reference, for the functions that take several areas.
    ArrayList<Object[][]> areas(Object v) {
        ArrayList<Object[][]> out = new ArrayList<Object[][]>();
        if (v instanceof Ref) {
            for (Ref r = (Ref) v; r != null; r = r.next) {
                int first = workbook.indexOf(r.sheet);
                int last = r.lastSheet == null ? first : workbook.indexOf(r.lastSheet);
                for (int s = first; s <= last; s++) {
                    out.add(matrix(new Ref(workbook.getSheet(s), null, r.r1, r.c1, r.r2, r.c2)));
                }
            }
        } else {
            out.add(matrix(v));
        }
        return out;
    }

    /// The values of an argument as rows and columns. A range is cut down to the rows and
    /// columns that hold data first.
    Object[][] matrix(Object v) {
        if (v instanceof Object[][]) {
            return (Object[][]) v;
        }
        if (v instanceof Ref) {
            Ref whole = (Ref) v;
            if (whole.next != null || whole.lastSheet != null && whole.lastSheet != whole.sheet) { //NOPMD CompareObjectsWithEquals - one sheet object per sheet
                // a union or a span of sheets is not rows and columns: Excel answers #VALUE!
                // where a function wants them, and the functions that take several areas
                // (SUM, COUNT and the like) walk them themselves
                throw EvalError.VALUE;
            }
            Ref r = whole.clip();
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
