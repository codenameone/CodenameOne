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
package com.codenameone.examples.brickyard.luau;

import com.codename1.util.MathUtil;

/// The base library and the `coroutine`, `table`, `math`, `os`, `bit32`,
/// `utf8` and `debug` libraries.
final class Stdlib {
    private Stdlib() {
    }

    private static final Object[] NONE = new Object[0];

    static final JavaFunction NEXT = new JavaFunction("next") {
        @Override
        public Object[] call(Interp in, Object[] a) {
            LuaTable t = table(a, 0, "next");
            Object[] out = new Object[2];
            if (t.next(arg(a, 1), out)) {
                return out;
            }
            return new Object[] {null};
        }
    };

    private static final JavaFunction IPAIRS_ITER = new JavaFunction("ipairs_iter") {
        @Override
        public Object[] call(Interp in, Object[] a) {
            int i = (int) ((Double) a[1]).doubleValue() + 1;
            Object t = a[0];
            Object v = t instanceof LuaTable ? ((LuaTable) t).rawgeti(i) : in.index(t, Interp.num(i));
            if (v == null) {
                return new Object[] {null};
            }
            return new Object[] {Interp.num(i), v};
        }
    };

    static Object[] ret(Object v) {
        return new Object[] {v};
    }

    static LuaError badArg(int i, String fn, String msg) {
        return LuaError.rt("invalid argument #" + (i + 1) + " to '" + fn + "' (" + msg + ")");
    }

    static LuaTable table(Object[] a, int i, String fn) {
        Object v = i < a.length ? a[i] : null;
        if (!(v instanceof LuaTable)) {
            throw badArg(i, fn, "table expected, got " + typeLabel(v, i < a.length));
        }
        return (LuaTable) v;
    }

    static String typeLabel(Object v, boolean present) {
        return present || v != null ? Interp.typeName(v) : "no value";
    }

    static double number(Object[] a, int i, String fn) {
        Object v = i < a.length ? a[i] : null;
        Double d = Interp.toNumber(v);
        if (d == null) {
            throw badArg(i, fn, "number expected, got " + typeLabel(v, i < a.length));
        }
        return d.doubleValue();
    }

    static double optNumber(Object[] a, int i, String fn, double def) {
        if (i >= a.length || a[i] == null) {
            return def;
        }
        return number(a, i, fn);
    }

    static int integer(Object[] a, int i, String fn) {
        double d = number(a, i, fn);
        return (int) Math.floor(d);
    }

    static int optInteger(Object[] a, int i, String fn, int def) {
        if (i >= a.length || a[i] == null) {
            return def;
        }
        return integer(a, i, fn);
    }

    static String string(Object[] a, int i, String fn) {
        Object v = i < a.length ? a[i] : null;
        if (v instanceof String) {
            return (String) v;
        }
        if (v instanceof Double) {
            return Interp.rawToString(v);
        }
        throw badArg(i, fn, "string expected, got " + typeLabel(v, i < a.length));
    }

    static void reg(LuaTable t, JavaFunction f) {
        String n = f.name();
        int dot = n.lastIndexOf('.');
        t.rawset(dot >= 0 ? n.substring(dot + 1) : n, f);
    }

    static void install(final Interp in) {
        LuaTable g = in.getGlobals();
        g.rawset("_G", g);
        g.rawset("_VERSION", "Luau");
        g.rawset("pcall", in.pcallFn);
        g.rawset("xpcall", in.xpcallFn);
        reg(g, NEXT);
        reg(g, new JavaFunction("print") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                StringBuilder b = new StringBuilder();
                for (int k = 0; k < a.length; k++) {
                    if (k > 0) {
                        b.append('\t');
                    }
                    b.append(i.tostring(a[k]));
                }
                i.print(b.toString());
                return NONE;
            }
        });
        reg(g, new JavaFunction("type") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                if (a.length == 0) {
                    throw badArg(0, "type", "value expected");
                }
                return ret(Interp.typeName(a[0]));
            }
        });
        reg(g, new JavaFunction("typeof") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                Object v = arg(a, 0);
                if (v instanceof LuaUserdata) {
                    return ret(((LuaUserdata) v).typeName());
                }
                return ret(Interp.typeName(v));
            }
        });
        reg(g, new JavaFunction("tostring") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                if (a.length == 0) {
                    throw badArg(0, "tostring", "value expected");
                }
                return ret(i.tostring(a[0]));
            }
        });
        reg(g, new JavaFunction("tonumber") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                Object v = arg(a, 0);
                if (a.length > 1 && a[1] != null) {
                    int base = integer(a, 1, "tonumber");
                    if (base < 2 || base > 36) {
                        throw badArg(1, "tonumber", "base out of range");
                    }
                    String s = (v instanceof Double ? Interp.rawToString(v) : string(a, 0, "tonumber")).trim();
                    boolean neg = s.startsWith("-");
                    if (neg) {
                        s = s.substring(1);
                    }
                    if (base == 16 && (s.startsWith("0x") || s.startsWith("0X"))) {
                        s = s.substring(2);
                    }
                    if (s.length() == 0) {
                        return ret(null);
                    }
                    double r = 0;
                    for (int k = 0; k < s.length(); k++) {
                        int d = Character.digit(s.charAt(k), base);
                        if (d < 0) {
                            return ret(null);
                        }
                        r = r * base + d;
                    }
                    return ret(Interp.num(neg ? -r : r));
                }
                return ret(Interp.toNumber(v));
            }
        });
        reg(g, new JavaFunction("pairs") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                Object t = arg(a, 0);
                Object mm = i.metamethod(t, "__pairs");
                if (mm != null) {
                    Object[] r = i.call(mm, t);
                    return new Object[] {arg(r, 0), arg(r, 1), arg(r, 2)};
                }
                table(a, 0, "pairs");
                return new Object[] {NEXT, t, null};
            }
        });
        reg(g, new JavaFunction("ipairs") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                table(a, 0, "ipairs");
                return new Object[] {IPAIRS_ITER, a[0], Interp.num(0)};
            }
        });
        reg(g, new JavaFunction("select") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                Object n = arg(a, 0);
                if ("#".equals(n)) {
                    return ret(Interp.num(a.length - 1));
                }
                int k = integer(a, 0, "select");
                if (k < 0) {
                    k = a.length + k;
                    if (k < 1) {
                        throw badArg(0, "select", "index out of range");
                    }
                } else if (k == 0) {
                    throw badArg(0, "select", "index out of range");
                }
                if (k >= a.length) {
                    return NONE;
                }
                Object[] r = new Object[a.length - k];
                System.arraycopy(a, k, r, 0, r.length);
                return r;
            }
        });
        reg(g, new JavaFunction("rawget") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                return ret(table(a, 0, "rawget").rawget(arg(a, 1)));
            }
        });
        reg(g, new JavaFunction("rawset") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                table(a, 0, "rawset").rawset(arg(a, 1), arg(a, 2));
                return ret(a[0]);
            }
        });
        reg(g, new JavaFunction("rawequal") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                return ret(Interp.rawEquals(arg(a, 0), arg(a, 1)) ? Boolean.TRUE : Boolean.FALSE);
            }
        });
        reg(g, new JavaFunction("rawlen") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                Object v = arg(a, 0);
                if (v instanceof String) {
                    return ret(Interp.num(((String) v).length()));
                }
                return ret(Interp.num(table(a, 0, "rawlen").length()));
            }
        });
        reg(g, new JavaFunction("setmetatable") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                LuaTable t = table(a, 0, "setmetatable");
                Object mt = arg(a, 1);
                if (mt != null && !(mt instanceof LuaTable)) {
                    throw badArg(1, "setmetatable", "nil or table expected");
                }
                LuaTable old = t.getMetatable();
                if (old != null && old.rawget("__metatable") != null) {
                    throw LuaError.rt("cannot change a protected metatable");
                }
                if (t.isFrozen()) {
                    throw LuaError.rt("attempt to modify a readonly table");
                }
                t.setMetatable((LuaTable) mt);
                return ret(t);
            }
        });
        reg(g, new JavaFunction("getmetatable") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                LuaTable mt = i.getMetatable(arg(a, 0));
                if (mt == null) {
                    return ret(null);
                }
                Object prot = mt.rawget("__metatable");
                return ret(prot != null ? prot : mt);
            }
        });
        reg(g, new JavaFunction("error") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                Object v = arg(a, 0);
                int level = optInteger(a, 1, "error", 1);
                if (v instanceof String && level > 0) {
                    int line = i.currentLine(level);
                    if (line >= 0) {
                        v = i.currentChunk(level) + ":" + line + ": " + v;
                    }
                }
                throw new LuaError(v);
            }
        });
        reg(g, new JavaFunction("assert") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                if (a.length == 0) {
                    throw badArg(0, "assert", "value expected");
                }
                if (!Interp.truthy(a[0])) {
                    if (a.length > 1) {
                        throw new LuaError(a[1]);
                    }
                    throw LuaError.rt("assertion failed!");
                }
                return a;
            }
        });
        reg(g, new JavaFunction("unpack") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                return unpack(a, "unpack");
            }
        });
        reg(g, new JavaFunction("newproxy") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                LuaUserdata u = new LuaUserdata();
                if (Interp.truthy(arg(a, 0))) {
                    u.setMetatable(new LuaTable());
                }
                return ret(u);
            }
        });
        reg(g, new JavaFunction("gcinfo") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                Runtime r = Runtime.getRuntime();
                return ret(Interp.num(Math.floor((r.totalMemory() - r.freeMemory()) / 1024.0)));
            }
        });
        installCoroutine(g);
        installTable(g);
        installMath(g);
        installOs(g);
        installBit32(g);
        installUtf8(g);
        installDebug(g);
        StringLib.install(in, g);
        BufferLib.install(g);
    }

    static Object[] unpack(Object[] a, String fn) {
        LuaTable t = table(a, 0, fn);
        int i = optInteger(a, 1, fn, 1);
        int j = a.length > 2 && a[2] != null ? integer(a, 2, fn) : t.length();
        if (i > j) {
            return NONE;
        }
        if (j - i >= 8000000) {
            throw LuaError.rt("too many results to unpack");
        }
        Object[] r = new Object[j - i + 1];
        for (int k = i; k <= j; k++) {
            r[k - i] = t.rawgeti(k);
        }
        return r;
    }

    private static Coroutine thread(Object[] a, int i, String fn) {
        Object v = arg0(a, i);
        if (!(v instanceof Coroutine)) {
            throw badArg(i, fn, "thread expected");
        }
        return (Coroutine) v;
    }

    static Object arg(Object[] a, int i) {
        return i < a.length ? a[i] : null;
    }

    private static Object arg0(Object[] a, int i) {
        return i < a.length ? a[i] : null;
    }

    private static void installCoroutine(LuaTable g) {
        LuaTable co = new LuaTable();
        g.rawset("coroutine", co);
        reg(co, new JavaFunction("coroutine.create") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                Object f = arg(a, 0);
                if (!(f instanceof LuaFunction)) {
                    throw badArg(0, "create", "function expected");
                }
                return ret(i.newThread((LuaFunction) f));
            }
        });
        reg(co, new JavaFunction("coroutine.resume") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                Coroutine c = thread(a, 0, "resume");
                Object[] rest = new Object[Math.max(0, a.length - 1)];
                System.arraycopy(a, 1, rest, 0, rest.length);
                return i.resume(c, rest);
            }
        });
        reg(co, new JavaFunction("coroutine.yield") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                return i.yield(a);
            }
        });
        reg(co, new JavaFunction("coroutine.status") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                return ret(thread(a, 0, "status").statusName());
            }
        });
        reg(co, new JavaFunction("coroutine.running") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                Coroutine c = i.currentThread();
                return new Object[] {c, i.isMain(c) ? Boolean.TRUE : Boolean.FALSE};
            }
        });
        reg(co, new JavaFunction("coroutine.isyieldable") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                Coroutine c = i.currentThread();
                return ret(!i.isMain(c) && c.javaNest == 0 ? Boolean.TRUE : Boolean.FALSE);
            }
        });
        reg(co, new JavaFunction("coroutine.close") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                Coroutine c = thread(a, 0, "close");
                if (c.status == Coroutine.RUNNING || c.status == Coroutine.NORMAL) {
                    throw LuaError.rt("cannot close a " + c.statusName() + " coroutine");
                }
                c.status = Coroutine.DEAD;
                c.frames.clear();
                return ret(Boolean.TRUE);
            }
        });
        reg(co, new JavaFunction("coroutine.wrap") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                Object f = arg(a, 0);
                if (!(f instanceof LuaFunction)) {
                    throw badArg(0, "wrap", "function expected");
                }
                final Coroutine c = i.newThread((LuaFunction) f);
                return ret(new JavaFunction("wrap") {
                    @Override
                    public Object[] call(Interp i2, Object[] a2) {
                        Object[] r = i2.resume(c, a2);
                        if (!Interp.truthy(r[0])) {
                            Object e = r.length > 1 ? r[1] : null;
                            throw new LuaError(e);
                        }
                        Object[] out = new Object[r.length - 1];
                        System.arraycopy(r, 1, out, 0, out.length);
                        return out;
                    }
                });
            }
        });
    }

    private static void installTable(LuaTable g) {
        LuaTable t = new LuaTable();
        g.rawset("table", t);
        reg(t, new JavaFunction("table.insert") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                LuaTable tt = table(a, 0, "insert");
                if (a.length == 2) {
                    tt.rawseti(tt.length() + 1, a[1]);
                } else if (a.length == 3) {
                    tt.insert(integer(a, 1, "insert"), a[2]);
                } else {
                    throw LuaError.rt("wrong number of arguments to 'insert'");
                }
                return NONE;
            }
        });
        reg(t, new JavaFunction("table.remove") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                LuaTable tt = table(a, 0, "remove");
                int pos = optInteger(a, 1, "remove", tt.length());
                return ret(tt.remove(pos));
            }
        });
        reg(t, new JavaFunction("table.concat") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                LuaTable tt = table(a, 0, "concat");
                String sep = a.length > 1 && a[1] != null ? string(a, 1, "concat") : "";
                int from = optInteger(a, 2, "concat", 1);
                int to = a.length > 3 && a[3] != null ? integer(a, 3, "concat") : tt.length();
                StringBuilder b = new StringBuilder();
                for (int k = from; k <= to; k++) {
                    Object v = tt.rawgeti(k);
                    if (!(v instanceof String) && !(v instanceof Double)) {
                        throw LuaError.rt("invalid value (at index " + k + ") in table for 'concat'");
                    }
                    String part = Interp.rawToString(v);
                    // Checked per element so one long separator repeated over
                    // a big table fails before the builder grows past the cap.
                    i.chargeString((long) b.length() + part.length() + sep.length());
                    b.append(part);
                    if (k < to) {
                        b.append(sep);
                    }
                }
                return ret(b.toString());
            }
        });
        reg(t, new JavaFunction("table.unpack") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                return unpack(a, "unpack");
            }
        });
        reg(t, new JavaFunction("table.pack") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                LuaTable r = new LuaTable(a.length);
                for (int k = 0; k < a.length; k++) {
                    r.rawseti(k + 1, a[k]);
                }
                r.rawset("n", Interp.num(a.length));
                return ret(r);
            }
        });
        reg(t, new JavaFunction("table.sort") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                LuaTable tt = table(a, 0, "sort");
                Object cmp = arg(a, 1);
                int n = tt.length();
                Object[] items = new Object[n];
                for (int k = 0; k < n; k++) {
                    items[k] = tt.rawgeti(k + 1);
                }
                Object[] tmp = new Object[n];
                mergeSort(i, items, tmp, 0, n, cmp);
                for (int k = 0; k < n; k++) {
                    tt.rawseti(k + 1, items[k]);
                }
                return NONE;
            }
        });
        reg(t, new JavaFunction("table.find") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                LuaTable tt = table(a, 0, "find");
                Object needle = arg(a, 1);
                int init = optInteger(a, 2, "find", 1);
                for (int k = Math.max(1, init);; k++) {
                    Object v = tt.rawgeti(k);
                    if (v == null) {
                        return ret(null);
                    }
                    if (Interp.rawEquals(v, needle)) {
                        return ret(Interp.num(k));
                    }
                }
            }
        });
        reg(t, new JavaFunction("table.create") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                int n = integer(a, 0, "create");
                if (n < 0) {
                    throw badArg(0, "create", "size out of range");
                }
                LuaTable r = new LuaTable(n);
                Object v = arg(a, 1);
                if (v != null) {
                    for (int k = 1; k <= n; k++) {
                        r.rawseti(k, v);
                    }
                }
                return ret(r);
            }
        });
        reg(t, new JavaFunction("table.clear") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                table(a, 0, "clear").clear();
                return NONE;
            }
        });
        reg(t, new JavaFunction("table.freeze") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                LuaTable tt = table(a, 0, "freeze");
                tt.freeze();
                return ret(tt);
            }
        });
        reg(t, new JavaFunction("table.isfrozen") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                return ret(table(a, 0, "isfrozen").isFrozen() ? Boolean.TRUE : Boolean.FALSE);
            }
        });
        reg(t, new JavaFunction("table.clone") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                LuaTable src = table(a, 0, "clone");
                LuaTable c = src.copy();
                c.setMetatable(src.getMetatable());
                return ret(c);
            }
        });
        reg(t, new JavaFunction("table.move") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                LuaTable src = table(a, 0, "move");
                int f = integer(a, 1, "move");
                int e = integer(a, 2, "move");
                int tpos = integer(a, 3, "move");
                LuaTable dst = a.length > 4 && a[4] != null ? table(a, 4, "move") : src;
                if (e >= f) {
                    if (tpos > f && tpos <= e && dst == src) { // NOPMD CompareObjectsWithEquals - identity is the intent: a sentinel, a shared box or a tree node
                        for (int k = e - f; k >= 0; k--) {
                            dst.rawseti(tpos + k, src.rawgeti(f + k));
                        }
                    } else {
                        for (int k = 0; k <= e - f; k++) {
                            dst.rawseti(tpos + k, src.rawgeti(f + k));
                        }
                    }
                }
                return ret(dst);
            }
        });
        reg(t, new JavaFunction("table.maxn") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                LuaTable tt = table(a, 0, "maxn");
                double m = 0;
                Object[] out = new Object[2];
                Object key = null;
                while (tt.next(key, out)) {
                    key = out[0];
                    if (key instanceof Double && ((Double) key).doubleValue() > m) {
                        m = ((Double) key).doubleValue();
                    }
                }
                return ret(Interp.num(m));
            }
        });
        reg(t, new JavaFunction("table.getn") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                return ret(Interp.num(table(a, 0, "getn").length()));
            }
        });
        reg(t, new JavaFunction("table.foreach") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                LuaTable tt = table(a, 0, "foreach");
                Object fn = arg(a, 1);
                Object[] out = new Object[2];
                Object key = null;
                while (tt.next(key, out)) {
                    key = out[0];
                    Object r = i.call1(fn, out[0], out[1]);
                    if (r != null) {
                        return ret(r);
                    }
                }
                return NONE;
            }
        });
        reg(t, new JavaFunction("table.foreachi") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                LuaTable tt = table(a, 0, "foreachi");
                Object fn = arg(a, 1);
                int n = tt.length();
                for (int k = 1; k <= n; k++) {
                    Object r = i.call1(fn, Interp.num(k), tt.rawgeti(k));
                    if (r != null) {
                        return ret(r);
                    }
                }
                return NONE;
            }
        });
    }

    private static boolean less(Interp in, Object a, Object b, Object cmp) {
        if (cmp != null) {
            return Interp.truthy(in.call1(cmp, a, b));
        }
        return in.lessThan(a, b);
    }

    static void mergeSort(Interp in, Object[] a, Object[] tmp, int lo, int hi, Object cmp) {
        if (hi - lo < 2) {
            return;
        }
        if (hi - lo <= 12) {
            for (int i = lo + 1; i < hi; i++) {
                Object x = a[i];
                int j = i - 1;
                while (j >= lo && less(in, x, a[j], cmp)) {
                    a[j + 1] = a[j];
                    j--;
                }
                a[j + 1] = x;
            }
            return;
        }
        int mid = (lo + hi) >>> 1;
        mergeSort(in, a, tmp, lo, mid, cmp);
        mergeSort(in, a, tmp, mid, hi, cmp);
        int i = lo;
        int j = mid;
        int k = lo;
        while (i < mid && j < hi) {
            tmp[k++] = less(in, a[j], a[i], cmp) ? a[j++] : a[i++];
        }
        while (i < mid) {
            tmp[k++] = a[i++];
        }
        while (j < hi) {
            tmp[k++] = a[j++];
        }
        System.arraycopy(tmp, lo, a, lo, hi - lo);
    }

    /// PCG32, the generator behind `math.random`.
    static final class Pcg {
        long state;
        long inc = 105;

        Pcg(long seed) {
            seed(seed);
        }

        void seed(long seed) {
            state = 0;
            next();
            state += seed;
            next();
        }

        int next() {
            long old = state;
            state = old * 6364136223846793005L + (inc | 1);
            int xorshifted = (int) (((old >>> 18) ^ old) >>> 27);
            int rot = (int) (old >>> 59);
            return (xorshifted >>> rot) | (xorshifted << ((-rot) & 31));
        }

        double nextDouble() {
            long hi = next() & 0xFFFFFFFFL;
            long lo = next() & 0xFFFFFFFFL;
            return ((hi << 21) ^ (lo >>> 11) & 0x1FFFFFFFFFFFFFL) * (1.0 / 9007199254740992.0) % 1.0;
        }
    }

    private static void installMath(LuaTable g) {
        LuaTable m = new LuaTable();
        g.rawset("math", m);
        m.rawset("pi", Interp.num(Math.PI));
        m.rawset("huge", Interp.num(Double.POSITIVE_INFINITY));
        final Pcg rng = new Pcg(System.currentTimeMillis());
        String[] unary = {"abs", "ceil", "floor", "sqrt", "sin", "cos", "tan", "asin", "acos", "atan", "exp",
            "log10", "sinh", "cosh", "tanh", "deg", "rad", "round", "sign"};
        for (final String name : unary) {
            reg(m, new JavaFunction("math." + name) {
                @Override
                public Object[] call(Interp i, Object[] a) {
                    double x = number(a, 0, name);
                    return ret(Interp.num(unaryMath(name, x)));
                }
            });
        }
        reg(m, new JavaFunction("math.log") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                double x = number(a, 0, "log");
                if (a.length > 1 && a[1] != null) {
                    double b = number(a, 1, "log");
                    if (b == 2) {
                        return ret(Interp.num(MathUtil.log(x) / MathUtil.log(2)));
                    }
                    if (b == 10) {
                        return ret(Interp.num(MathUtil.log10(x)));
                    }
                    return ret(Interp.num(MathUtil.log(x) / MathUtil.log(b)));
                }
                return ret(Interp.num(MathUtil.log(x)));
            }
        });
        reg(m, new JavaFunction("math.atan2") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                return ret(Interp.num(MathUtil.atan2(number(a, 0, "atan2"), number(a, 1, "atan2"))));
            }
        });
        reg(m, new JavaFunction("math.pow") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                return ret(Interp.num(Interp.pow(number(a, 0, "pow"), number(a, 1, "pow"))));
            }
        });
        reg(m, new JavaFunction("math.fmod") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                double x = number(a, 0, "fmod");
                double y = number(a, 1, "fmod");
                return ret(Interp.num(x % y));
            }
        });
        reg(m, new JavaFunction("math.modf") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                double x = number(a, 0, "modf");
                double ip = x >= 0 ? Math.floor(x) : Math.ceil(x);
                double fp = Double.isInfinite(x) ? 0 : x - ip;
                return new Object[] {Interp.num(ip), Interp.num(fp)};
            }
        });
        reg(m, new JavaFunction("math.frexp") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                double x = number(a, 0, "frexp");
                if (x == 0 || Double.isNaN(x) || Double.isInfinite(x)) {
                    return new Object[] {Interp.num(x), Interp.num(0)};
                }
                int e = 0;
                double mm = Math.abs(x);
                while (mm >= 1) {
                    mm /= 2;
                    e++;
                }
                while (mm < 0.5) {
                    mm *= 2;
                    e--;
                }
                return new Object[] {Interp.num(x < 0 ? -mm : mm), Interp.num(e)};
            }
        });
        reg(m, new JavaFunction("math.ldexp") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                return ret(Interp.num(number(a, 0, "ldexp") * Interp.pow(2, integer(a, 1, "ldexp"))));
            }
        });
        reg(m, new JavaFunction("math.min") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                double r = number(a, 0, "min");
                for (int k = 1; k < a.length; k++) {
                    r = Math.min(r, number(a, k, "min"));
                }
                return ret(Interp.num(r));
            }
        });
        reg(m, new JavaFunction("math.max") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                double r = number(a, 0, "max");
                for (int k = 1; k < a.length; k++) {
                    r = Math.max(r, number(a, k, "max"));
                }
                return ret(Interp.num(r));
            }
        });
        reg(m, new JavaFunction("math.clamp") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                double x = number(a, 0, "clamp");
                double lo = number(a, 1, "clamp");
                double hi = number(a, 2, "clamp");
                if (lo > hi) {
                    throw badArg(2, "clamp", "max must be greater than or equal to min");
                }
                return ret(Interp.num(Math.max(lo, Math.min(hi, x))));
            }
        });
        reg(m, new JavaFunction("math.random") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                if (a.length == 0) {
                    return ret(Interp.num(rng.nextDouble()));
                }
                double lo = 1;
                double hi;
                if (a.length == 1) {
                    hi = number(a, 0, "random");
                } else {
                    lo = number(a, 0, "random");
                    hi = number(a, 1, "random");
                }
                long l = (long) Math.floor(lo);
                long h = (long) Math.floor(hi);
                if (l > h) {
                    throw badArg(a.length == 1 ? 0 : 1, "random", "interval is empty");
                }
                long span = h - l + 1;
                long r = (long) (rng.nextDouble() * span);
                return ret(Interp.num(l + r));
            }
        });
        reg(m, new JavaFunction("math.randomseed") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                rng.seed((long) number(a, 0, "randomseed"));
                return NONE;
            }
        });
        reg(m, new JavaFunction("math.noise") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                return ret(Interp.num(Noise.perlin(optNumber(a, 0, "noise", 0), optNumber(a, 1, "noise", 0),
                        optNumber(a, 2, "noise", 0))));
            }
        });
        reg(m, new JavaFunction("math.lerp") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                double x = number(a, 0, "lerp");
                double y = number(a, 1, "lerp");
                double t = number(a, 2, "lerp");
                return ret(Interp.num(t == 1 ? y : x + (y - x) * t));
            }
        });
        reg(m, new JavaFunction("math.map") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                double x = number(a, 0, "map");
                double inMin = number(a, 1, "map");
                double inMax = number(a, 2, "map");
                double outMin = number(a, 3, "map");
                double outMax = number(a, 4, "map");
                return ret(Interp.num(outMin + (x - inMin) * (outMax - outMin) / (inMax - inMin)));
            }
        });
    }

    static double unaryMath(String name, double x) {
        switch (name) {
            case "abs":
                return Math.abs(x);
            case "ceil":
                return Math.ceil(x);
            case "floor":
                return Math.floor(x);
            case "sqrt":
                return Math.sqrt(x);
            case "sin":
                return Math.sin(x);
            case "cos":
                return Math.cos(x);
            case "tan":
                return Math.tan(x);
            case "asin":
                return MathUtil.asin(x);
            case "acos":
                return MathUtil.acos(x);
            case "atan":
                return MathUtil.atan(x);
            case "exp":
                return MathUtil.exp(x);
            case "log10":
                return MathUtil.log10(x);
            case "sinh":
                return Math.sinh(x);
            case "cosh":
                return Math.cosh(x);
            case "tanh":
                return Math.tanh(x);
            case "deg":
                return x * (180.0 / Math.PI);
            case "rad":
                return x * (Math.PI / 180.0);
            case "round":
                return x >= 0 ? Math.floor(x + 0.5) : -Math.floor(-x + 0.5);
            default:
                return x > 0 ? 1 : x < 0 ? -1 : 0;
        }
    }

    private static void installOs(LuaTable g) {
        LuaTable os = new LuaTable();
        g.rawset("os", os);
        final long start = System.currentTimeMillis();
        reg(os, new JavaFunction("os.time") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                Object t = arg(a, 0);
                if (t instanceof LuaTable) {
                    LuaTable tt = (LuaTable) t;
                    int y = field(tt, "year", 1970);
                    int mo = field(tt, "month", 1);
                    int d = field(tt, "day", 1);
                    int h = field(tt, "hour", 12);
                    int mi = field(tt, "min", 0);
                    int s = field(tt, "sec", 0);
                    return ret(Interp.num(DateMath.toEpoch(y, mo, d, h, mi, s)));
                }
                return ret(Interp.num(Math.floor(System.currentTimeMillis() / 1000.0)));
            }
        });
        reg(os, new JavaFunction("os.clock") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                return ret(Interp.num((System.currentTimeMillis() - start) / 1000.0));
            }
        });
        reg(os, new JavaFunction("os.difftime") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                return ret(Interp.num(number(a, 0, "difftime") - optNumber(a, 1, "difftime", 0)));
            }
        });
        reg(os, new JavaFunction("os.date") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                String fmt = a.length > 0 && a[0] != null ? string(a, 0, "date") : "%c";
                double t = a.length > 1 && a[1] != null ? number(a, 1, "date")
                        : Math.floor(System.currentTimeMillis() / 1000.0);
                return ret(DateMath.format(fmt, (long) t));
            }
        });
    }

    static int field(LuaTable t, String k, int def) {
        Object v = t.rawget(k);
        if (v instanceof Double) {
            return (int) ((Double) v).doubleValue();
        }
        return def;
    }

    private static long u32(Object[] a, int i, String fn) {
        double d = number(a, i, fn);
        long v = (long) d;
        return v & 0xFFFFFFFFL;
    }

    private static void installBit32(LuaTable g) {
        LuaTable b = new LuaTable();
        g.rawset("bit32", b);
        String[] fold = {"band", "bor", "bxor", "btest"};
        for (final String name : fold) {
            reg(b, new JavaFunction("bit32." + name) {
                @Override
                public Object[] call(Interp i, Object[] a) {
                    long r = "bor".equals(name) || "bxor".equals(name) ? 0 : 0xFFFFFFFFL;
                    for (int k = 0; k < a.length; k++) {
                        long v = u32(a, k, name);
                        if ("bor".equals(name)) {
                            r |= v;
                        } else if ("bxor".equals(name)) {
                            r ^= v;
                        } else {
                            r &= v;
                        }
                    }
                    if ("btest".equals(name)) {
                        return ret(r != 0 ? Boolean.TRUE : Boolean.FALSE);
                    }
                    return ret(Interp.num(r));
                }
            });
        }
        reg(b, new JavaFunction("bit32.bnot") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                return ret(Interp.num(~u32(a, 0, "bnot") & 0xFFFFFFFFL));
            }
        });
        String[] shifts = {"lshift", "rshift", "arshift", "lrotate", "rrotate"};
        for (final String name : shifts) {
            reg(b, new JavaFunction("bit32." + name) {
                @Override
                public Object[] call(Interp i, Object[] a) {
                    long x = u32(a, 0, name);
                    int n = integer(a, 1, name);
                    long r;
                    switch (name) {
                        case "lshift":
                            r = n >= 32 || n <= -32 ? 0 : n >= 0 ? x << n : x >>> -n;
                            break;
                        case "rshift":
                            r = n >= 32 || n <= -32 ? 0 : n >= 0 ? x >>> n : x << -n;
                            break;
                        case "arshift": {
                            int s = (int) x;
                            r = n >= 32 ? (s < 0 ? 0xFFFFFFFFL : 0) : n >= 0 ? s >> n : (long) s << -n;
                            break;
                        }
                        case "lrotate": {
                            int s = n & 31;
                            r = (x << s) | (x >>> (32 - s));
                            break;
                        }
                        default: {
                            int s = n & 31;
                            r = (x >>> s) | (x << (32 - s));
                            break;
                        }
                    }
                    return ret(Interp.num(r & 0xFFFFFFFFL));
                }
            });
        }
        reg(b, new JavaFunction("bit32.extract") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                long x = u32(a, 0, "extract");
                int f = integer(a, 1, "extract");
                int w = optInteger(a, 2, "extract", 1);
                if (f < 0 || w <= 0 || f + w > 32) {
                    throw LuaError.rt("trying to access non-existent bits");
                }
                long mask = w == 32 ? 0xFFFFFFFFL : (1L << w) - 1;
                return ret(Interp.num((x >>> f) & mask));
            }
        });
        reg(b, new JavaFunction("bit32.replace") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                long x = u32(a, 0, "replace");
                long v = u32(a, 1, "replace");
                int f = integer(a, 2, "replace");
                int w = optInteger(a, 3, "replace", 1);
                if (f < 0 || w <= 0 || f + w > 32) {
                    throw LuaError.rt("trying to access non-existent bits");
                }
                long mask = (w == 32 ? 0xFFFFFFFFL : (1L << w) - 1) << f;
                return ret(Interp.num((x & ~mask | (v << f) & mask) & 0xFFFFFFFFL));
            }
        });
        reg(b, new JavaFunction("bit32.countlz") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                return ret(Interp.num(Integer.numberOfLeadingZeros((int) u32(a, 0, "countlz"))));
            }
        });
        reg(b, new JavaFunction("bit32.countrz") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                return ret(Interp.num(Integer.numberOfTrailingZeros((int) u32(a, 0, "countrz"))));
            }
        });
        reg(b, new JavaFunction("bit32.byteswap") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                return ret(Interp.num(Integer.reverseBytes((int) u32(a, 0, "byteswap")) & 0xFFFFFFFFL));
            }
        });
    }

    private static void installUtf8(LuaTable g) {
        LuaTable u = new LuaTable();
        g.rawset("utf8", u);
        u.rawset("charpattern", "[\u0000-\u007F\u00C2-\u00FD][\u0080-\u00BF]*");
        reg(u, new JavaFunction("utf8.char") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                StringBuilder b = new StringBuilder();
                for (int k = 0; k < a.length; k++) {
                    int cp = integer(a, k, "char");
                    if (cp < 0 || cp > 0x10FFFF) {
                        throw badArg(k, "char", "value out of range");
                    }
                    Utf8.encode(b, cp);
                }
                return ret(b.toString());
            }
        });
        reg(u, new JavaFunction("utf8.len") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                String s = string(a, 0, "len");
                int from = optInteger(a, 1, "len", 1);
                int to = optInteger(a, 2, "len", -1);
                int p = from < 0 ? s.length() + from : from - 1;
                int end = to < 0 ? s.length() + to + 1 : to;
                int n = 0;
                int[] out = new int[1];
                while (p < end) {
                    int len = Utf8.decode(s, p, out);
                    if (len < 0) {
                        return new Object[] {null, Interp.num(p + 1)};
                    }
                    p += len;
                    n++;
                }
                return ret(Interp.num(n));
            }
        });
        reg(u, new JavaFunction("utf8.codepoint") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                String s = string(a, 0, "codepoint");
                int from = optInteger(a, 1, "codepoint", 1);
                int to = optInteger(a, 2, "codepoint", from);
                int p = from < 0 ? s.length() + from : from - 1;
                int end = to < 0 ? s.length() + to + 1 : to;
                java.util.ArrayList<Object> r = new java.util.ArrayList<Object>();
                int[] out = new int[1];
                while (p < end) {
                    int len = Utf8.decode(s, p, out);
                    if (len < 0) {
                        throw LuaError.rt("invalid UTF-8 code");
                    }
                    r.add(Interp.num(out[0]));
                    p += len;
                }
                return r.toArray();
            }
        });
        reg(u, new JavaFunction("utf8.offset") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                String s = string(a, 0, "offset");
                int n = integer(a, 1, "offset");
                int p = optInteger(a, 2, "offset", n >= 0 ? 1 : s.length() + 1) - 1;
                if (n > 0) {
                    n--;
                    while (n > 0 && p < s.length()) {
                        p++;
                        while (p < s.length() && (s.charAt(p) & 0xC0) == 0x80) {
                            p++;
                        }
                        n--;
                    }
                    return n == 0 ? ret(Interp.num(p + 1)) : ret(null);
                }
                if (n == 0) {
                    while (p > 0 && p < s.length() && (s.charAt(p) & 0xC0) == 0x80) {
                        p--;
                    }
                    return ret(Interp.num(p + 1));
                }
                while (n < 0 && p > 0) {
                    p--;
                    while (p > 0 && (s.charAt(p) & 0xC0) == 0x80) {
                        p--;
                    }
                    n++;
                }
                return n == 0 ? ret(Interp.num(p + 1)) : ret(null);
            }
        });
        reg(u, new JavaFunction("utf8.codes") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                final String s = string(a, 0, "codes");
                return new Object[] {new JavaFunction("codes_iter") {
                    @Override
                    public Object[] call(Interp i2, Object[] a2) {
                        int p = (int) ((Double) a2[1]).doubleValue();
                        int[] out = new int[1];
                        if (p > 0) {
                            int len = Utf8.decode(s, p - 1, out);
                            p += len < 0 ? 1 : len;
                        } else {
                            p = 1;
                        }
                        if (p > s.length()) {
                            return ret(null);
                        }
                        int len = Utf8.decode(s, p - 1, out);
                        if (len < 0) {
                            throw LuaError.rt("invalid UTF-8 code");
                        }
                        return new Object[] {Interp.num(p), Interp.num(out[0])};
                    }
                }, s, Interp.num(0)};
            }
        });
    }

    private static void installDebug(LuaTable g) {
        LuaTable d = new LuaTable();
        g.rawset("debug", d);
        reg(d, new JavaFunction("debug.traceback") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                Object msg = arg(a, 0);
                if (msg != null && !(msg instanceof String) && !(msg instanceof Double)) {
                    return ret(msg);
                }
                String tb = i.traceback(i.currentThread());
                return ret(msg == null ? tb : Interp.rawToString(msg) + "\n" + tb);
            }
        });
        reg(d, new JavaFunction("debug.info") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                Object target = arg(a, 0);
                String what = string(a, a.length > 1 ? 1 : 0, "info");
                java.util.ArrayList<Object> r = new java.util.ArrayList<Object>();
                int level = -1;
                if (target instanceof Double) {
                    level = (int) ((Double) target).doubleValue();
                }
                for (int k = 0; k < what.length(); k++) {
                    char c = what.charAt(k);
                    if (c == 'l') {
                        r.add(level > 0 ? Interp.num(i.currentLine(level)) : Interp.num(-1));
                    } else if (c == 's') {
                        r.add(level > 0 ? i.currentChunk(level) : "[C]");
                    } else if (c == 'n') {
                        r.add(target instanceof LuaFunction ? ((LuaFunction) target).name() : null);
                    } else if (c == 'a') {
                        r.add(Interp.num(0));
                        r.add(Boolean.FALSE);
                    } else if (c == 'f') {
                        r.add(target instanceof LuaFunction ? target : null);
                    }
                }
                return r.toArray();
            }
        });
    }
}
