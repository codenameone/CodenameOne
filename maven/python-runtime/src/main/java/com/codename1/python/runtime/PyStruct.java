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

/// `_cn1struct`: packing and unpacking for the bundled `struct`, which
/// CPython does in C. The Python module parses the format and raises every
/// error; a call here answers `None` whenever the Python code has to run --
/// a value out of range, a buffer that is not bytes, a half float -- so it
/// never decides an error itself.
final class PyStruct {
    private PyStruct() {
    }

    /// One parsed format: per item its code, offset and size.
    static final class Plan extends PyObject {
        final char[] codes;
        final int[] offs;
        final int[] sizes;
        final boolean little;

        Plan(String codes, Object offs, Object sizes, boolean little) {
            this.codes = codes.toCharArray();
            int n = this.codes.length;
            this.offs = new int[n];
            this.sizes = new int[n];
            for (int k = 0; k < n; k++) {
                this.offs[k] = (int) Py.asLong(Py.getitem(offs, Long.valueOf(k)));
                this.sizes[k] = (int) Py.asLong(Py.getitem(sizes, Long.valueOf(k)));
            }
            this.little = little;
        }

        @Override
        public PyType type() {
            return Py.OBJECT_TYPE;
        }
    }

    static PyModule module() {
        PyModule m = new PyModule("_cn1struct", new String[0]);
        m.setAttr("plan", PyBuiltin.positional("plan",
                a -> new Plan(Typed.asStr(a[0]), a[1], a[2], Py.isTrue(a[3]))));
        m.setAttr("unpack", PyBuiltin.fixed3("unpack", (a0, a1, a2) -> unpack(a0, a1, (int) Py.asLong(a2))));
        m.setAttr("pack", PyBuiltin.fixed3("pack", (a0, a1, a2) -> pack(a0, (int) Py.asLong(a1), a2)));
        return m;
    }

    private static boolean signed(char c) {
        return c == 'b' || c == 'h' || c == 'i' || c == 'l' || c == 'q' || c == 'n';
    }

    private static boolean integer(char c) {
        return "bBhHiIlLqQnNP".indexOf(c) >= 0;
    }

    private static long read(byte[] b, int o, int size, boolean little) {
        long v = 0;
        if (little) {
            for (int k = size - 1; k >= 0; k--) {
                v = v << 8 | b[o + k] & 0xff;
            }
        } else {
            for (int k = 0; k < size; k++) {
                v = v << 8 | b[o + k] & 0xff;
            }
        }
        return v;
    }

    private static void write(byte[] b, int o, int size, boolean little, long v) {
        for (int k = 0; k < size; k++) {
            int at = little ? o + k : o + size - 1 - k;
            b[at] = (byte) (v >>> (8 * k));
        }
    }

    static Object unpack(Object planObj, Object buf, int offset) {
        if (!(planObj instanceof Plan) || offset < 0) {
            return Py.NONE;
        }
        Plan p = (Plan) planObj;
        byte[] b;
        if (buf instanceof PyBytes) {
            b = ((PyBytes) buf).value;
        } else if (buf instanceof PyByteArray) {
            b = ((PyByteArray) buf).buf;
        } else {
            return Py.NONE;
        }
        int n = p.codes.length;
        Object[] out = new Object[n];
        for (int k = 0; k < n; k++) {
            char c = p.codes[k];
            int o = offset + p.offs[k];
            int size = p.sizes[k];
            if (integer(c)) {
                long v = read(b, o, size, p.little);
                if (size < 8 && signed(c)) {
                    int shift = 64 - 8 * size;
                    v = v << shift >> shift;
                } else if (size == 8 && !signed(c) && v < 0) {
                    return Py.NONE;
                }
                out[k] = Long.valueOf(v);
            } else if (c == '?') {
                out[k] = b[o] != 0 ? Py.TRUE : Py.FALSE;
            } else if (c == 'd') {
                out[k] = Double.valueOf(Double.longBitsToDouble(read(b, o, 8, p.little)));
            } else if (c == 'f') {
                out[k] = Double.valueOf(Float.intBitsToFloat((int) read(b, o, 4, p.little)));
            } else if (c == 'c' || c == 's') {
                out[k] = new PyBytes(java.util.Arrays.copyOfRange(b, o, o + size));
            } else {
                return Py.NONE;
            }
        }
        return new PyTuple(out);
    }

    static Object pack(Object planObj, int total, Object args) {
        if (!(planObj instanceof Plan) || !(args instanceof PyTuple)) {
            return Py.NONE;
        }
        Plan p = (Plan) planObj;
        PyTuple t = (PyTuple) args;
        int n = p.codes.length;
        if (t.size() != n) {
            return Py.NONE;
        }
        byte[] out = new byte[total];
        for (int k = 0; k < n; k++) {
            char c = p.codes[k];
            int o = p.offs[k];
            int size = p.sizes[k];
            Object v = t.get(k);
            if (integer(c)) {
                if (!(v instanceof Long)) {
                    return Py.NONE;
                }
                long x = ((Long) v).longValue();
                boolean s = signed(c);
                if (size < 8) {
                    long lo = s ? -(1L << (8 * size - 1)) : 0;
                    long hi = s ? (1L << (8 * size - 1)) - 1 : (1L << (8 * size)) - 1;
                    if (x < lo || x > hi) {
                        return Py.NONE;
                    }
                } else if (!s && x < 0) {
                    return Py.NONE;
                }
                write(out, o, size, p.little, x);
            } else if (c == 'd' && v instanceof Double) {
                write(out, o, 8, p.little, Double.doubleToRawLongBits(((Double) v).doubleValue()));
            } else if (c == 'f' && v instanceof Double) {
                double d = ((Double) v).doubleValue();
                float f = (float) d;
                if (Float.isInfinite(f) && !Double.isInfinite(d)) {
                    return Py.NONE;
                }
                write(out, o, 4, p.little, Float.floatToRawIntBits(f) & 0xFFFFFFFFL);
            } else if (c == 's' && v instanceof PyBytes) {
                byte[] src = ((PyBytes) v).value;
                System.arraycopy(src, 0, out, o, Math.min(src.length, size));
            } else {
                return Py.NONE;
            }
        }
        return new PyBytes(out);
    }
}
