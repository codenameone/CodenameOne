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
package com.codename1.python.runtime;

import com.codename1.util.MathUtil;

/// `_cn1np`: the loops of the numpy subset. The `numpy` package keeps an
/// array's shape, strides and offset and does the indexing arithmetic; every
/// loop over elements runs here, over an [NdBuf].
///
/// An operand is passed as three values: the buffer, the offset of its first
/// element and a tuple of strides in elements. A broadcast dimension has
/// stride 0, so the loops never broadcast themselves. Every result is a new
/// contiguous buffer in C order.
final class NumPy {
    private NumPy() {
    }

    static PyModule module() {
        PyModule m = new PyModule("_cn1np", new String[0]);
        m.setAttr("alloc", PyBuiltin.positional("alloc", a -> new NdBuf(kind(a[0]), (int) Py.asLong(a[1]))));
        m.setAttr("kind", PyBuiltin.positional("kind", a -> String.valueOf(buf(a[0]).kind)));
        m.setAttr("size", PyBuiltin.positional("size", a -> Py.of((long) buf(a[0]).size())));
        m.setAttr("fromflat", PyBuiltin.positional("fromflat", a -> fromFlat(kind(a[0]), a[1])));
        m.setAttr("item", PyBuiltin.positional("item", a -> buf(a[0]).item((int) Py.asLong(a[1]))));
        m.setAttr("setitem", PyBuiltin.positional("setitem", a -> {
            setScalar(buf(a[0]), (int) Py.asLong(a[1]), a[2]);
            return Py.NONE;
        }));
        m.setAttr("copy", PyBuiltin.positional("copy", a -> copy(kind(a[0]), ints(a[1]), buf(a[2]), off(a[3]), ints(a[4]))));
        m.setAttr("tolist", PyBuiltin.positional("tolist", a -> toList(ints(a[0]), buf(a[1]), off(a[2]), ints(a[3]))));
        m.setAttr("assign", PyBuiltin.positional("assign", a -> {
            assign(ints(a[0]), buf(a[1]), off(a[2]), ints(a[3]), buf(a[4]), off(a[5]), ints(a[6]));
            return Py.NONE;
        }));
        m.setAttr("fill", PyBuiltin.positional("fill", a -> {
            fill(ints(a[0]), buf(a[1]), off(a[2]), ints(a[3]), a[4]);
            return Py.NONE;
        }));
        m.setAttr("binop", PyBuiltin.positional("binop", a -> binop((int) Py.asLong(a[0]), kind(a[1]), Py.isTrue(a[2]),
                ints(a[3]), buf(a[4]), off(a[5]), ints(a[6]), buf(a[7]), off(a[8]), ints(a[9]))));
        m.setAttr("unop", PyBuiltin.positional("unop", a -> unop((int) Py.asLong(a[0]), kind(a[1]), Py.isTrue(a[2]),
                ints(a[3]), buf(a[4]), off(a[5]), ints(a[6]))));
        m.setAttr("reduce", PyBuiltin.positional("reduce", a -> reduce((int) Py.asLong(a[0]), kind(a[1]), Py.isTrue(a[2]),
                ints(a[3]), buf(a[4]), off(a[5]), ints(a[6]), (int) Py.asLong(a[7]))));
        m.setAttr("cumulate", PyBuiltin.positional("cumulate", a -> cumulate((int) Py.asLong(a[0]), kind(a[1]),
                ints(a[2]), buf(a[3]), off(a[4]), ints(a[5]), (int) Py.asLong(a[6]))));
        m.setAttr("matmul", PyBuiltin.positional("matmul", a -> matmul(kind(a[0]), (int) Py.asLong(a[1]), (int) Py.asLong(a[2]),
                (int) Py.asLong(a[3]), buf(a[4]), off(a[5]), ints(a[6]), buf(a[7]), off(a[8]), ints(a[9]))));
        m.setAttr("where", PyBuiltin.positional("where", a -> where(kind(a[0]), ints(a[1]), buf(a[2]), off(a[3]), ints(a[4]),
                buf(a[5]), off(a[6]), ints(a[7]), buf(a[8]), off(a[9]), ints(a[10]))));
        m.setAttr("positions", PyBuiltin.positional("positions", a -> positions(ints(a[0]), off(a[1]), ints(a[2]))));
        m.setAttr("compress", PyBuiltin.positional("compress", a -> compress(buf(a[0]), buf(a[1]))));
        m.setAttr("gather", PyBuiltin.positional("gather", a -> gather(buf(a[0]), buf(a[1]))));
        m.setAttr("scatter", PyBuiltin.positional("scatter", a -> {
            scatter(buf(a[0]), buf(a[1]), buf(a[2]));
            return Py.NONE;
        }));
        m.setAttr("sortrows", PyBuiltin.positional("sortrows", a -> sortRows(buf(a[0]), (int) Py.asLong(a[1]), Py.isTrue(a[2]))));
        m.setAttr("concat", PyBuiltin.positional("concat", a -> concat(kind(a[0]), a[1])));
        m.setAttr("arange", PyBuiltin.positional("arange", a -> arange(kind(a[0]), a[1], a[2], (int) Py.asLong(a[3]))));
        return m;
    }

    // ---- argument decoding --------------------------------------------

    private static char kind(Object o) {
        String s = Py.toStr(o);
        if (s.length() != 1 || "fib".indexOf(s.charAt(0)) < 0) {
            throw Py.valueError("bad dtype kind " + s);
        }
        return s.charAt(0);
    }

    private static NdBuf buf(Object o) {
        if (!(o instanceof NdBuf)) {
            throw Py.typeError("expected an ndarray buffer");
        }
        return (NdBuf) o;
    }

    private static int off(Object o) {
        return (int) Py.asLong(o);
    }

    private static int[] ints(Object o) {
        Object[] items;
        if (o instanceof PyTuple) {
            items = ((PyTuple) o).items;
        } else if (o instanceof PyList) {
            items = ((PyList) o).toArray();
        } else {
            throw Py.typeError("expected a tuple of ints");
        }
        int[] r = new int[items.length];
        for (int i = 0; i < r.length; i++) {
            r[i] = (int) Py.asLong(items[i]);
        }
        return r;
    }

    private static int count(int[] shape) {
        int n = 1;
        for (int s : shape) {
            n *= s;
        }
        return n;
    }

    private static boolean isFloatScalar(Object v) {
        return v instanceof Double || Py.typeOf(v) == Py.FLOAT_TYPE; // NOPMD CompareObjectsWithEquals - identity is meant
    }

    private static void setScalar(NdBuf b, int i, Object v) {
        if (v instanceof PyBool) {
            b.setL(i, Py.isTrue(v) ? 1 : 0);
        } else if (isFloatScalar(v)) {
            b.setD(i, Py.asDouble(v));
        } else if (b.kind == 'f') {
            b.setD(i, Py.asDouble(v));
        } else {
            b.setL(i, Py.asLong(v));
        }
    }

    private static NdBuf fromFlat(char kind, Object list) {
        Object[] items = list instanceof PyList ? ((PyList) list).toArray() : ((PyTuple) list).items;
        NdBuf b = new NdBuf(kind, items.length);
        for (int i = 0; i < items.length; i++) {
            setScalar(b, i, items[i]);
        }
        return b;
    }

    // ---- the strided walk ---------------------------------------------

    /// Advances `idx` over `shape` in C order and moves each operand's
    /// position by its strides; answers false past the last element.
    private static boolean step(int[] shape, int[] idx, int[][] strides, int[] pos) {
        for (int d = shape.length - 1; d >= 0; d--) {
            idx[d]++;
            for (int k = 0; k < pos.length; k++) {
                pos[k] += strides[k][d];
            }
            if (idx[d] < shape[d]) {
                return true;
            }
            for (int k = 0; k < pos.length; k++) {
                pos[k] -= strides[k][d] * shape[d];
            }
            idx[d] = 0;
        }
        return false;
    }

    private static NdBuf copy(char kind, int[] shape, NdBuf a, int aoff, int[] ast) {
        int n = count(shape);
        NdBuf r = new NdBuf(kind, n);
        if (n == 0) {
            return r;
        }
        int[] idx = new int[shape.length];
        int[] pos = {aoff};
        int[][] st = {ast};
        int i = 0;
        do {
            if (a.d != null) {
                r.setD(i++, a.d[pos[0]]);
            } else {
                r.setL(i++, a.l[pos[0]]);
            }
        } while (step(shape, idx, st, pos));
        return r;
    }

    private static Object toList(int[] shape, NdBuf a, int off, int[] st) {
        return toList(shape, 0, a, off, st);
    }

    private static Object toList(int[] shape, int dim, NdBuf a, int off, int[] st) {
        if (dim == shape.length) {
            return a.item(off);
        }
        PyList out = new PyList(shape[dim]);
        for (int i = 0; i < shape[dim]; i++) {
            out.append(toList(shape, dim + 1, a, off + i * st[dim], st));
        }
        return out;
    }

    private static void assign(int[] shape, NdBuf dst, int doff, int[] dst2, NdBuf src, int soff, int[] sst) {
        if (count(shape) == 0) {
            return;
        }
        int[] idx = new int[shape.length];
        int[] pos = {doff, soff};
        int[][] st = {dst2, sst};
        if (dst == src) { // NOPMD CompareObjectsWithEquals - identity is meant
            // Overlapping views: read everything before writing anything.
            NdBuf tmp = copy(src.kind, shape, src, soff, sst);
            src = tmp;
            pos[1] = 0;
            st[1] = contiguous(shape);
        }
        do {
            if (src.d != null) {
                dst.setD(pos[0], src.d[pos[1]]);
            } else {
                dst.setL(pos[0], src.l[pos[1]]);
            }
        } while (step(shape, idx, st, pos));
    }

    private static int[] contiguous(int[] shape) {
        int[] st = new int[shape.length];
        int s = 1;
        for (int d = shape.length - 1; d >= 0; d--) {
            st[d] = s;
            s *= shape[d];
        }
        return st;
    }

    private static void fill(int[] shape, NdBuf dst, int off, int[] st, Object v) {
        if (count(shape) == 0) {
            return;
        }
        int[] idx = new int[shape.length];
        int[] pos = {off};
        int[][] sts = {st};
        do {
            setScalar(dst, pos[0], v);
        } while (step(shape, idx, sts, pos));
    }

    // ---- element-wise -------------------------------------------------

    static final int ADD = 0;
    static final int SUB = 1;
    static final int MUL = 2;
    static final int TRUEDIV = 3;
    static final int FLOORDIV = 4;
    static final int MOD = 5;
    static final int POW = 6;
    static final int EQ = 7;
    static final int NE = 8;
    static final int LT = 9;
    static final int LE = 10;
    static final int GT = 11;
    static final int GE = 12;
    static final int AND = 13;
    static final int OR = 14;
    static final int XOR = 15;
    static final int MIN = 16;
    static final int MAX = 17;
    static final int ATAN2 = 18;
    static final int HYPOT = 19;
    static final int LSHIFT = 20;
    static final int RSHIFT = 21;

    private static NdBuf binop(int op, char kind, boolean fp, int[] shape, NdBuf a, int aoff, int[] ast,
            NdBuf b, int boff, int[] bst) {
        int n = count(shape);
        NdBuf r = new NdBuf(kind, n);
        if (n == 0) {
            return r;
        }
        int[] idx = new int[shape.length];
        int[] pos = {aoff, boff};
        int[][] st = {ast, bst};
        int i = 0;
        if (fp) {
            do {
                r.setD(i++, fop(op, a.getD(pos[0]), b.getD(pos[1])));
            } while (step(shape, idx, st, pos));
        } else {
            do {
                r.setL(i++, lop(op, a.l[pos[0]], b.l[pos[1]]));
            } while (step(shape, idx, st, pos));
        }
        return r;
    }

    private static double fop(int op, double x, double y) {
        switch (op) {
            case ADD:
                return x + y;
            case SUB:
                return x - y;
            case MUL:
                return x * y;
            case TRUEDIV:
                return x / y;
            case FLOORDIV:
                return Math.floor(x / y);
            case MOD: {
                if (y == 0) {
                    return Double.NaN;
                }
                double m = x % y;
                return m != 0 && (m < 0) != (y < 0) ? m + y : m;
            }
            case POW:
                return MathUtil.pow(x, y);
            case EQ:
                return x == y ? 1 : 0;
            case NE:
                return x != y ? 1 : 0;
            case LT:
                return x < y ? 1 : 0;
            case LE:
                return x <= y ? 1 : 0;
            case GT:
                return x > y ? 1 : 0;
            case GE:
                return x >= y ? 1 : 0;
            case MIN:
                return x != x || y != y ? Double.NaN : Math.min(x, y);
            case MAX:
                return x != x || y != y ? Double.NaN : Math.max(x, y);
            case ATAN2:
                return MathUtil.atan2(x, y);
            case HYPOT:
                return Math.sqrt(x * x + y * y);
            default:
                throw Py.typeError("ufunc not supported for the input types");
        }
    }

    private static long lop(int op, long x, long y) {
        switch (op) {
            case ADD:
                return x + y;
            case SUB:
                return x - y;
            case MUL:
                return x * y;
            case FLOORDIV: {
                if (y == 0) {
                    return 0;
                }
                long q = x / y;
                return (x % y != 0) && ((x < 0) != (y < 0)) ? q - 1 : q;
            }
            case MOD: {
                if (y == 0) {
                    return 0;
                }
                long m = x % y;
                return m != 0 && (m < 0) != (y < 0) ? m + y : m;
            }
            case POW: {
                if (y < 0) {
                    throw Py.valueError("Integers to negative integer powers are not allowed.");
                }
                long r = 1;
                long base = x;
                long e = y;
                while (e > 0) {
                    if ((e & 1) != 0) {
                        r *= base;
                    }
                    base *= base;
                    e >>= 1;
                }
                return r;
            }
            case EQ:
                return x == y ? 1 : 0;
            case NE:
                return x != y ? 1 : 0;
            case LT:
                return x < y ? 1 : 0;
            case LE:
                return x <= y ? 1 : 0;
            case GT:
                return x > y ? 1 : 0;
            case GE:
                return x >= y ? 1 : 0;
            case AND:
                return x & y;
            case OR:
                return x | y;
            case XOR:
                return x ^ y;
            case MIN:
                return Math.min(x, y);
            case MAX:
                return Math.max(x, y);
            case LSHIFT:
                return x << y;
            case RSHIFT:
                return x >> y;
            default:
                throw Py.typeError("ufunc not supported for the input types");
        }
    }

    static final int NEG = 0;
    static final int ABS = 1;
    static final int SQRT = 2;
    static final int EXP = 3;
    static final int LOG = 4;
    static final int SIN = 5;
    static final int COS = 6;
    static final int TAN = 7;
    static final int FLOOR = 8;
    static final int CEIL = 9;
    static final int NOT = 10;
    static final int INVERT = 11;
    static final int SIGN = 12;
    static final int SQUARE = 13;
    static final int LOG10 = 14;
    static final int LOG2 = 15;
    static final int TANH = 16;
    static final int ARCSIN = 17;
    static final int ARCCOS = 18;
    static final int ARCTAN = 19;
    static final int RINT = 20;
    static final int ISNAN = 21;
    static final int ISINF = 22;
    static final int ISFINITE = 23;
    static final int SINH = 24;
    static final int COSH = 25;
    static final int EXPM1 = 26;
    static final int LOG1P = 27;
    static final int TRUNC = 28;

    private static NdBuf unop(int op, char kind, boolean fp, int[] shape, NdBuf a, int aoff, int[] ast) {
        int n = count(shape);
        NdBuf r = new NdBuf(kind, n);
        if (n == 0) {
            return r;
        }
        int[] idx = new int[shape.length];
        int[] pos = {aoff};
        int[][] st = {ast};
        int i = 0;
        if (fp) {
            do {
                r.setD(i++, fun(op, a.getD(pos[0])));
            } while (step(shape, idx, st, pos));
        } else {
            do {
                r.setL(i++, lun(op, a.l[pos[0]], a.kind == 'b'));
            } while (step(shape, idx, st, pos));
        }
        return r;
    }

    private static double fun(int op, double x) {
        switch (op) {
            case NEG:
                return -x;
            case ABS:
                return Math.abs(x);
            case SQRT:
                return Math.sqrt(x);
            case EXP:
                return MathUtil.exp(x);
            case LOG:
                return x < 0 ? Double.NaN : MathUtil.log(x);
            case SIN:
                return Math.sin(x);
            case COS:
                return Math.cos(x);
            case TAN:
                return Math.tan(x);
            case FLOOR:
                return Math.floor(x);
            case CEIL:
                return Math.ceil(x);
            case NOT:
                return x == 0 ? 1 : 0;
            case SIGN:
                return x > 0 ? 1 : x < 0 ? -1 : x;
            case SQUARE:
                return x * x;
            case LOG10:
                return x < 0 ? Double.NaN : MathUtil.log10(x);
            case LOG2:
                return x < 0 ? Double.NaN : MathUtil.log(x) / MathUtil.log(2);
            case TANH:
                return MathFunctions.tanh(x);
            case ARCSIN:
                return MathUtil.asin(x);
            case ARCCOS:
                return MathUtil.acos(x);
            case ARCTAN:
                return MathUtil.atan(x);
            case RINT:
                return rint(x);
            case ISNAN:
                return x != x ? 1 : 0;
            case ISINF:
                return Double.isInfinite(x) ? 1 : 0;
            case ISFINITE:
                return x != x || Double.isInfinite(x) ? 0 : 1;
            case SINH:
                return MathFunctions.sinh(x);
            case COSH:
                return MathFunctions.cosh(x);
            case EXPM1:
                return MathFunctions.expm1(x);
            case LOG1P:
                return MathFunctions.log1p(x);
            case TRUNC:
                return x < 0 ? Math.ceil(x) : Math.floor(x);
            default:
                throw Py.typeError("ufunc not supported for the input types");
        }
    }

    /// Round half to even, as `np.rint` and `np.round` do.
    static double rint(double x) {
        double f = Math.floor(x);
        double diff = x - f;
        if (diff > 0.5) {
            return f + 1;
        }
        if (diff < 0.5) {
            return f;
        }
        return f % 2 == 0 ? f : f + 1;
    }

    private static long lun(int op, long x, boolean bool) {
        switch (op) {
            case NEG:
                return -x;
            case ABS:
                return Math.abs(x);
            case NOT:
                return x == 0 ? 1 : 0;
            case INVERT:
                return bool ? (x == 0 ? 1 : 0) : ~x;
            case SIGN:
                return x > 0 ? 1 : x < 0 ? -1 : 0;
            case SQUARE:
                return x * x;
            case FLOOR:
            case CEIL:
            case RINT:
            case TRUNC:
                return x;
            case ISNAN:
            case ISINF:
                return 0;
            case ISFINITE:
                return 1;
            default:
                throw Py.typeError("ufunc not supported for the input types");
        }
    }

    // ---- reductions ---------------------------------------------------

    static final int SUM = 0;
    static final int PROD = 1;
    static final int RMIN = 2;
    static final int RMAX = 3;
    static final int ARGMIN = 4;
    static final int ARGMAX = 5;
    static final int ANY = 6;
    static final int ALL = 7;

    /// Reduces along `axis` (or over everything when it is -1); the result
    /// has the input's shape without that axis.
    private static NdBuf reduce(int op, char kind, boolean fp, int[] shape, NdBuf a, int aoff, int[] ast, int axis) {
        if (axis < 0) {
            NdBuf flat = a;
            int[] fshape = {count(shape)};
            if (!isContiguous(shape, ast) || aoff != 0 || a.size() != fshape[0]) {
                flat = copy(a.kind, shape, a, aoff, ast);
            }
            return reduce(op, kind, fp, fshape, flat, 0, new int[] {1}, 0);
        }
        int len = shape[axis];
        if (len == 0 && (op == RMIN || op == RMAX || op == ARGMIN || op == ARGMAX)) {
            throw Py.valueError("zero-size array to reduction operation which has no identity");
        }
        int[] outer = new int[shape.length - 1];
        int[] ost = new int[shape.length - 1];
        for (int d = 0, j = 0; d < shape.length; d++) {
            if (d != axis) {
                outer[j] = shape[d];
                ost[j++] = ast[d];
            }
        }
        int n = count(outer);
        NdBuf r = new NdBuf(kind, n);
        if (n == 0) {
            return r;
        }
        int stride = ast[axis];
        int[] idx = new int[outer.length];
        int[] pos = {aoff};
        int[][] st = {ost};
        int i = 0;
        do {
            int p = pos[0];
            if (fp) {
                reduceD(op, r, i++, a, p, stride, len);
            } else {
                reduceL(op, r, i++, a, p, stride, len);
            }
        } while (outer.length > 0 && step(outer, idx, st, pos));
        return r;
    }

    private static boolean isContiguous(int[] shape, int[] st) {
        int[] c = contiguous(shape);
        for (int d = 0; d < shape.length; d++) {
            if (shape[d] > 1 && c[d] != st[d]) {
                return false;
            }
        }
        return true;
    }

    private static void reduceD(int op, NdBuf r, int i, NdBuf a, int p, int stride, int len) {
        switch (op) {
            case SUM: {
                // Pairwise like numpy, so long sums round the same way.
                r.setD(i, pairwise(a, p, stride, len));
                return;
            }
            case PROD: {
                double acc = 1;
                for (int k = 0; k < len; k++) {
                    acc *= a.getD(p + k * stride);
                }
                r.setD(i, acc);
                return;
            }
            case RMIN:
            case RMAX:
            case ARGMIN:
            case ARGMAX: {
                double best = a.getD(p);
                int at = 0;
                boolean min = op == RMIN || op == ARGMIN;
                for (int k = 1; k < len && best == best; k++) {
                    double v = a.getD(p + k * stride);
                    if (v != v || (min ? v < best : v > best)) {
                        best = v;
                        at = k;
                    }
                }
                if (op == ARGMIN || op == ARGMAX) {
                    r.setL(i, at);
                } else {
                    r.setD(i, best);
                }
                return;
            }
            case ANY:
            case ALL: {
                boolean any = op == ANY;
                boolean res = !any;
                for (int k = 0; k < len; k++) {
                    if ((a.getD(p + k * stride) != 0) == any) {
                        res = any;
                        break;
                    }
                }
                r.setL(i, res ? 1 : 0);
                return;
            }
            default:
                throw Py.typeError("unsupported reduction");
        }
    }

    private static double pairwise(NdBuf a, int p, int stride, int len) {
        if (len < 8) {
            double s = 0;
            for (int k = 0; k < len; k++) {
                s += a.getD(p + k * stride);
            }
            return s;
        }
        if (len <= 128) {
            double[] acc = new double[8];
            for (int j = 0; j < 8; j++) {
                acc[j] = a.getD(p + j * stride);
            }
            int k = 8;
            for (; k + 8 <= len; k += 8) {
                for (int j = 0; j < 8; j++) {
                    acc[j] += a.getD(p + (k + j) * stride);
                }
            }
            double s = ((acc[0] + acc[1]) + (acc[2] + acc[3])) + ((acc[4] + acc[5]) + (acc[6] + acc[7]));
            for (; k < len; k++) {
                s += a.getD(p + k * stride);
            }
            return s;
        }
        int half = len / 2;
        half -= half % 8;
        return pairwise(a, p, stride, half) + pairwise(a, p + half * stride, stride, len - half);
    }

    private static void reduceL(int op, NdBuf r, int i, NdBuf a, int p, int stride, int len) {
        long[] l = a.l;
        switch (op) {
            case SUM: {
                long acc = 0;
                for (int k = 0; k < len; k++) {
                    acc += l[p + k * stride];
                }
                r.setL(i, acc);
                return;
            }
            case PROD: {
                long acc = 1;
                for (int k = 0; k < len; k++) {
                    acc *= l[p + k * stride];
                }
                r.setL(i, acc);
                return;
            }
            case RMIN:
            case RMAX:
            case ARGMIN:
            case ARGMAX: {
                long best = l[p];
                int at = 0;
                boolean min = op == RMIN || op == ARGMIN;
                for (int k = 1; k < len; k++) {
                    long v = l[p + k * stride];
                    if (min ? v < best : v > best) {
                        best = v;
                        at = k;
                    }
                }
                r.setL(i, op == ARGMIN || op == ARGMAX ? at : best);
                return;
            }
            case ANY:
            case ALL: {
                boolean any = op == ANY;
                boolean res = !any;
                for (int k = 0; k < len; k++) {
                    if ((l[p + k * stride] != 0) == any) {
                        res = any;
                        break;
                    }
                }
                r.setL(i, res ? 1 : 0);
                return;
            }
            default:
                throw Py.typeError("unsupported reduction");
        }
    }

    /// `cumsum` (op 0) and `cumprod` (op 1) along `axis`, in place over a
    /// contiguous copy.
    private static NdBuf cumulate(int op, char kind, int[] shape, NdBuf a, int aoff, int[] ast, int axis) {
        NdBuf r = copy(kind, shape, a, aoff, ast);
        int[] st = contiguous(shape);
        int len = shape[axis];
        int stride = st[axis];
        int n = r.size();
        if (n == 0) {
            return r;
        }
        int block = stride * len;
        for (int base = 0; base < n; base += block) {
            for (int s = 0; s < stride; s++) {
                int p = base + s;
                for (int k = 1; k < len; k++) {
                    int q = p + k * stride;
                    int prev = q - stride;
                    if (r.d != null) {
                        r.d[q] = op == 0 ? r.d[prev] + r.d[q] : r.d[prev] * r.d[q];
                    } else {
                        r.l[q] = op == 0 ? r.l[prev] + r.l[q] : r.l[prev] * r.l[q];
                    }
                }
            }
        }
        return r;
    }

    // ---- linear algebra -----------------------------------------------

    /// `(n x k) @ (k x m)`.
    private static NdBuf matmul(char kind, int n, int k, int m, NdBuf a, int aoff, int[] ast, NdBuf b, int boff, int[] bst) {
        NdBuf r = new NdBuf(kind, n * m);
        if (kind == 'f') {
            double[] row = new double[m];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < m; j++) {
                    row[j] = 0;
                }
                for (int t = 0; t < k; t++) {
                    double av = a.getD(aoff + i * ast[0] + t * ast[1]);
                    if (av == 0) {
                        continue;
                    }
                    int bp = boff + t * bst[0];
                    for (int j = 0; j < m; j++) {
                        row[j] += av * b.getD(bp + j * bst[1]);
                    }
                }
                System.arraycopy(row, 0, r.d, i * m, m);
            }
        } else {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < m; j++) {
                    long s = 0;
                    for (int t = 0; t < k; t++) {
                        s += a.getL(aoff + i * ast[0] + t * ast[1]) * b.getL(boff + t * bst[0] + j * bst[1]);
                    }
                    r.setL(i * m + j, s);
                }
            }
        }
        return r;
    }

    private static NdBuf where(char kind, int[] shape, NdBuf c, int coff, int[] cst, NdBuf x, int xoff, int[] xst,
            NdBuf y, int yoff, int[] yst) {
        int n = count(shape);
        NdBuf r = new NdBuf(kind, n);
        if (n == 0) {
            return r;
        }
        int[] idx = new int[shape.length];
        int[] pos = {coff, xoff, yoff};
        int[][] st = {cst, xst, yst};
        int i = 0;
        do {
            boolean t = c.getD(pos[0]) != 0;
            NdBuf s = t ? x : y;
            int p = t ? pos[1] : pos[2];
            if (s.d != null) {
                r.setD(i++, s.d[p]);
            } else {
                r.setL(i++, s.l[p]);
            }
        } while (step(shape, idx, st, pos));
        return r;
    }

    // ---- fancy indexing -----------------------------------------------

    /// The buffer position of every element of a view, in C order.
    private static NdBuf positions(int[] shape, int off, int[] st) {
        int n = count(shape);
        NdBuf r = new NdBuf('i', n);
        if (n == 0) {
            return r;
        }
        int[] idx = new int[shape.length];
        int[] pos = {off};
        int[][] sts = {st};
        int i = 0;
        do {
            r.l[i++] = pos[0];
        } while (step(shape, idx, sts, pos));
        return r;
    }

    /// The positions whose mask entry is true.
    private static NdBuf compress(NdBuf pos, NdBuf mask) {
        int n = 0;
        for (int i = 0; i < mask.size(); i++) {
            if (mask.getD(i) != 0) {
                n++;
            }
        }
        NdBuf r = new NdBuf('i', n);
        for (int i = 0, j = 0; i < mask.size(); i++) {
            if (mask.getD(i) != 0) {
                r.l[j++] = pos.l[i];
            }
        }
        return r;
    }

    private static NdBuf gather(NdBuf a, NdBuf pos) {
        int n = pos.size();
        NdBuf r = new NdBuf(a.kind, n);
        for (int i = 0; i < n; i++) {
            int p = (int) pos.l[i];
            if (a.d != null) {
                r.d[i] = a.d[p];
            } else {
                r.l[i] = a.l[p];
            }
        }
        return r;
    }

    /// Writes `src` (cycled when shorter, so a scalar broadcasts) into `a`
    /// at `pos`.
    private static void scatter(NdBuf a, NdBuf pos, NdBuf src) {
        int n = pos.size();
        int m = src.size();
        if (m == 0) {
            return;
        }
        for (int i = 0; i < n; i++) {
            int p = (int) pos.l[i];
            if (src.d != null) {
                a.setD(p, src.d[i % m]);
            } else {
                a.setL(p, src.l[i % m]);
            }
        }
    }

    /// Sorts each row of `len` elements of a contiguous buffer, answering
    /// the sorted values or, with `arg`, the stable sort's indices. NaN
    /// sorts last.
    private static NdBuf sortRows(NdBuf a, int len, boolean arg) {
        int n = a.size();
        NdBuf r = arg ? new NdBuf('i', n) : new NdBuf(a.kind, n);
        int[] perm = new int[len];
        int[] tmp = new int[len];
        for (int base = 0; base + len <= n && len > 0; base += len) {
            for (int i = 0; i < len; i++) {
                perm[i] = i;
            }
            mergeSort(a, base, perm, tmp, 0, len);
            for (int i = 0; i < len; i++) {
                if (arg) {
                    r.l[base + i] = perm[i];
                } else if (a.d != null) {
                    r.d[base + i] = a.d[base + perm[i]];
                } else {
                    r.l[base + i] = a.l[base + perm[i]];
                }
            }
        }
        return r;
    }

    private static boolean less(NdBuf a, int base, int x, int y) {
        if (a.d != null) {
            double u = a.d[base + x];
            double v = a.d[base + y];
            if (v != v) {
                return u == u;
            }
            return u < v;
        }
        return a.l[base + x] < a.l[base + y];
    }

    private static void mergeSort(NdBuf a, int base, int[] p, int[] tmp, int lo, int hi) {
        if (hi - lo < 2) {
            return;
        }
        int mid = (lo + hi) >>> 1;
        mergeSort(a, base, p, tmp, lo, mid);
        mergeSort(a, base, p, tmp, mid, hi);
        int i = lo;
        int j = mid;
        int k = lo;
        while (i < mid && j < hi) {
            tmp[k++] = less(a, base, p[j], p[i]) ? p[j++] : p[i++];
        }
        while (i < mid) {
            tmp[k++] = p[i++];
        }
        while (j < hi) {
            tmp[k++] = p[j++];
        }
        System.arraycopy(tmp, lo, p, lo, hi - lo);
    }

    private static NdBuf arange(char kind, Object start, Object step, int n) {
        NdBuf r = new NdBuf(kind, n);
        if (kind == 'f') {
            double s = Py.asDouble(start);
            double d = Py.asDouble(step);
            for (int i = 0; i < n; i++) {
                r.d[i] = s + i * d;
            }
        } else {
            long s = Py.asLong(start);
            long d = Py.asLong(step);
            for (int i = 0; i < n; i++) {
                r.l[i] = s + i * d;
            }
        }
        return r;
    }

    private static NdBuf concat(char kind, Object parts) {
        Object[] items = parts instanceof PyList ? ((PyList) parts).toArray() : ((PyTuple) parts).items;
        int n = 0;
        for (Object o : items) {
            n += buf(o).size();
        }
        NdBuf r = new NdBuf(kind, n);
        int at = 0;
        for (Object o : items) {
            NdBuf b = buf(o);
            for (int i = 0; i < b.size(); i++) {
                if (b.d != null) {
                    r.setD(at++, b.d[i]);
                } else {
                    r.setL(at++, b.l[i]);
                }
            }
        }
        return r;
    }
}
