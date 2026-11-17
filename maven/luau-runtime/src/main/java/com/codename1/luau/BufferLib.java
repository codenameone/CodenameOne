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
package com.codename1.luau;

/// The `buffer` library: little-endian reads and writes of integers,
/// floats and strings at byte offsets.
final class BufferLib {
    private BufferLib() {
    }

    private static final Object[] NONE = new Object[0];

    private static LuaBuffer buf(Object[] a, String fn) {
        Object v = Stdlib.arg(a, 0);
        if (!(v instanceof LuaBuffer)) {
            throw Stdlib.badArg(0, fn, "buffer expected, got " + Interp.typeName(v));
        }
        return (LuaBuffer) v;
    }

    private static int offset(LuaBuffer b, Object[] a, int i, int size, String fn) {
        int off = Stdlib.integer(a, i, fn);
        if (off < 0 || off + size > b.data.length) {
            throw LuaError.rt("buffer access out of bounds");
        }
        return off;
    }

    static long read(byte[] d, int off, int n) {
        long v = 0;
        for (int i = n - 1; i >= 0; i--) {
            v = (v << 8) | (d[off + i] & 0xFF);
        }
        return v;
    }

    static void write(byte[] d, int off, int n, long v) {
        for (int i = 0; i < n; i++) {
            d[off + i] = (byte) (v >>> (8 * i));
        }
    }

    static void install(LuaTable g) {
        LuaTable b = new LuaTable();
        g.rawset("buffer", b);
        Stdlib.reg(b, new JavaFunction("buffer.create") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                int n = Stdlib.integer(a, 0, "create");
                // Same ceiling and cost as a string: a buffer is a script's
                // other way to ask the server for a large allocation.
                if (n < 0 || n > StringLib.MAX_STRING) {
                    throw Stdlib.badArg(0, "create", "size out of range");
                }
                i.chargeString(n);
                return new Object[] {new LuaBuffer(n)};
            }
        });
        Stdlib.reg(b, new JavaFunction("buffer.fromstring") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                String s = Stdlib.string(a, 0, "fromstring");
                LuaBuffer r = new LuaBuffer(s.length());
                for (int k = 0; k < s.length(); k++) {
                    r.data[k] = (byte) s.charAt(k);
                }
                return new Object[] {r};
            }
        });
        Stdlib.reg(b, new JavaFunction("buffer.tostring") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                LuaBuffer r = buf(a, "tostring");
                StringBuilder sb = new StringBuilder(r.data.length);
                for (byte x : r.data) {
                    sb.append((char) (x & 0xFF));
                }
                return new Object[] {sb.toString()};
            }
        });
        Stdlib.reg(b, new JavaFunction("buffer.len") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                return new Object[] {Interp.num(buf(a, "len").data.length)};
            }
        });
        final String[] ints = {"i8", "u8", "i16", "u16", "i32", "u32"};
        for (final String t : ints) {
            final int size = t.endsWith("8") ? 1 : t.endsWith("16") ? 2 : 4;
            final boolean signed = t.charAt(0) == 'i';
            Stdlib.reg(b, new JavaFunction("buffer.read" + t) {
                @Override
                public Object[] call(Interp i, Object[] a) {
                    LuaBuffer r = buf(a, name());
                    long v = read(r.data, offset(r, a, 1, size, name()), size);
                    if (signed) {
                        int shift = 64 - size * 8;
                        v = (v << shift) >> shift;
                    }
                    return new Object[] {Interp.num(v)};
                }
            });
            Stdlib.reg(b, new JavaFunction("buffer.write" + t) {
                @Override
                public Object[] call(Interp i, Object[] a) {
                    LuaBuffer r = buf(a, name());
                    write(r.data, offset(r, a, 1, size, name()), size, (long) Stdlib.number(a, 2, name()));
                    return NONE;
                }
            });
        }
        Stdlib.reg(b, new JavaFunction("buffer.readf32") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                LuaBuffer r = buf(a, "readf32");
                int bits = (int) read(r.data, offset(r, a, 1, 4, "readf32"), 4);
                return new Object[] {Interp.num(Float.intBitsToFloat(bits))};
            }
        });
        Stdlib.reg(b, new JavaFunction("buffer.writef32") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                LuaBuffer r = buf(a, "writef32");
                write(r.data, offset(r, a, 1, 4, "writef32"), 4,
                        Float.floatToIntBits((float) Stdlib.number(a, 2, "writef32")));
                return NONE;
            }
        });
        Stdlib.reg(b, new JavaFunction("buffer.readf64") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                LuaBuffer r = buf(a, "readf64");
                long bits = read(r.data, offset(r, a, 1, 8, "readf64"), 8);
                return new Object[] {Interp.num(Double.longBitsToDouble(bits))};
            }
        });
        Stdlib.reg(b, new JavaFunction("buffer.writef64") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                LuaBuffer r = buf(a, "writef64");
                write(r.data, offset(r, a, 1, 8, "writef64"), 8,
                        Double.doubleToLongBits(Stdlib.number(a, 2, "writef64")));
                return NONE;
            }
        });
        Stdlib.reg(b, new JavaFunction("buffer.readstring") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                LuaBuffer r = buf(a, "readstring");
                int n = Stdlib.integer(a, 2, "readstring");
                int off = offset(r, a, 1, n, "readstring");
                StringBuilder sb = new StringBuilder(n);
                for (int k = 0; k < n; k++) {
                    sb.append((char) (r.data[off + k] & 0xFF));
                }
                return new Object[] {sb.toString()};
            }
        });
        Stdlib.reg(b, new JavaFunction("buffer.writestring") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                LuaBuffer r = buf(a, "writestring");
                String s = Stdlib.string(a, 2, "writestring");
                int n = a.length > 3 && a[3] != null ? Stdlib.integer(a, 3, "writestring") : s.length();
                int off = offset(r, a, 1, n, "writestring");
                for (int k = 0; k < n; k++) {
                    r.data[off + k] = (byte) s.charAt(k);
                }
                return NONE;
            }
        });
        Stdlib.reg(b, new JavaFunction("buffer.copy") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                LuaBuffer dst = buf(a, "copy");
                int doff = Stdlib.integer(a, 1, "copy");
                Object sv = Stdlib.arg(a, 2);
                if (!(sv instanceof LuaBuffer)) {
                    throw Stdlib.badArg(2, "copy", "buffer expected");
                }
                LuaBuffer src = (LuaBuffer) sv;
                int soff = Stdlib.optInteger(a, 3, "copy", 0);
                int n = a.length > 4 && a[4] != null ? Stdlib.integer(a, 4, "copy") : src.data.length - soff;
                if (doff < 0 || soff < 0 || n < 0 || doff + n > dst.data.length || soff + n > src.data.length) {
                    throw LuaError.rt("buffer access out of bounds");
                }
                System.arraycopy(src.data, soff, dst.data, doff, n);
                return NONE;
            }
        });
        Stdlib.reg(b, new JavaFunction("buffer.fill") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                LuaBuffer r = buf(a, "fill");
                int off = Stdlib.integer(a, 1, "fill");
                int v = Stdlib.integer(a, 2, "fill");
                int n = a.length > 3 && a[3] != null ? Stdlib.integer(a, 3, "fill") : r.data.length - off;
                if (off < 0 || n < 0 || off + n > r.data.length) {
                    throw LuaError.rt("buffer access out of bounds");
                }
                for (int k = 0; k < n; k++) {
                    r.data[off + k] = (byte) v;
                }
                return NONE;
            }
        });
    }
}
