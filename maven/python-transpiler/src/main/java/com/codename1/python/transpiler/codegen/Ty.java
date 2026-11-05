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
package com.codename1.python.transpiler.codegen;

import java.util.Arrays;

/// A static type the whole-program inference (`Infer`) gives a value, and
/// the Java representation typed code keeps it in.
///
/// The lattice is deliberately small. Every kind maps to one Java
/// representation, so a join that would need two (an int or a float, a bool
/// or None) goes to `DYN`, which is the plain `Object` the generic code uses:
///
/// | kind | Java | None |
/// |---|---|---|
/// | `BOOL` | `boolean` | -- |
/// | `FLOAT` | `double` | -- |
/// | `INT` | `Object` (a `Long`, or a big int) | -- |
/// | `STR` | `String` | null when `opt` |
/// | `INST` | the class's layout class | null when `opt` |
/// | `LIST` | `PyList` | null when `opt` |
/// | `NONE`, `DYN`, `BOT` | `Object` | `Py.NONE` |
///
/// An int stays an object because a Python int has no bound: overflowing a
/// `long` local would have nowhere to go without deoptimizing mid-statement.
/// On ParparVM a `Long` below 2^60 is a tagged immediate, so keeping it
/// boxed allocates nothing.
final class Ty {
    enum K { BOT, NONE, BOOL, INT, FLOAT, STR, INST, LIST, DYN }

    static final Ty BOT = new Ty(K.BOT, null, false, null);
    static final Ty NONE = new Ty(K.NONE, null, false, null);
    static final Ty BOOL = new Ty(K.BOOL, null, false, null);
    static final Ty INT = new Ty(K.INT, null, false, null);
    static final Ty FLOAT = new Ty(K.FLOAT, null, false, null);
    static final Ty STR = new Ty(K.STR, null, false, null);
    static final Ty DYN = new Ty(K.DYN, null, false, null);

    /// The list sites a `LIST` value may have come from (see `Infer`). Past
    /// this many it is any list at all.
    static final int MAX_SITES = 8;

    final K k;
    final Layouts.Cls cls;
    final boolean opt;
    /// Sorted allocation sites of a `LIST`; empty means "any list".
    final int[] sites;

    private Ty(K k, Layouts.Cls cls, boolean opt, int[] sites) {
        this.k = k;
        this.cls = cls;
        this.opt = opt;
        this.sites = sites;
    }

    static Ty inst(Layouts.Cls c, boolean opt) {
        return new Ty(K.INST, c, opt, null);
    }

    static Ty list(int[] sites, boolean opt) {
        return new Ty(K.LIST, null, opt, sites);
    }

    Ty orNone() {
        switch (k) {
            case BOT:
            case NONE:
                return NONE;
            case STR:
            case INST:
            case LIST:
                return opt ? this : new Ty(k, cls, true, sites);
            default:
                return DYN;
        }
    }

    /// The same type without None, for a value already tested non-None.
    Ty nonNone() {
        return opt ? new Ty(k, cls, false, sites) : this;
    }

    /// Whether the representation can hold None (as Java null).
    boolean nullable() {
        return opt && (k == K.STR || k == K.INST || k == K.LIST);
    }

    boolean isObject() {
        return k == K.BOT || k == K.NONE || k == K.DYN || k == K.INT;
    }

    static Ty join(Ty a, Ty b) {
        if (a == null || a.k == K.BOT) {
            return b == null ? BOT : b;
        }
        if (b == null || b.k == K.BOT) {
            return a;
        }
        if (a.equals(b)) {
            return a;
        }
        if (a.k == K.DYN || b.k == K.DYN) {
            return DYN;
        }
        if (a.k == K.NONE) {
            return b.orNone();
        }
        if (b.k == K.NONE) {
            return a.orNone();
        }
        // A bool is an int in Python, and both are objects there.
        if (a.k == K.INT && b.k == K.BOOL || a.k == K.BOOL && b.k == K.INT) {
            return INT;
        }
        if (a.k != b.k) {
            return DYN;
        }
        boolean opt = a.opt || b.opt;
        switch (a.k) {
            case INST: {
                Layouts.Cls c = common(a.cls, b.cls);
                return c == null ? DYN : inst(c, opt);
            }
            case LIST:
                return list(union(a.sites, b.sites), opt);
            case STR:
                return opt ? STR.orNone() : STR;
            default:
                return a;
        }
    }

    private static Layouts.Cls common(Layouts.Cls a, Layouts.Cls b) {
        for (Layouts.Cls x = a; x != null; x = x.parent) {
            for (Layouts.Cls y = b; y != null; y = y.parent) {
                if (x == y) { //NOPMD CompareObjectsWithEquals - identity is meant
                    return x;
                }
            }
        }
        return null;
    }

    private static int[] union(int[] a, int[] b) {
        if (a.length == 0 || b.length == 0) {
            return new int[0];
        }
        int[] r = new int[a.length + b.length];
        int n = 0;
        int i = 0;
        int j = 0;
        while (i < a.length || j < b.length) {
            int v;
            if (j >= b.length || i < a.length && a[i] < b[j]) {
                v = a[i++];
            } else if (i >= a.length || b[j] < a[i]) {
                v = b[j++];
            } else {
                v = a[i++];
                j++;
            }
            r[n++] = v;
        }
        return n > MAX_SITES ? new int[0] : Arrays.copyOf(r, n);
    }

    /// Whether every value of `this` is a value of `t`.
    boolean within(Ty t) {
        return join(this, t).equals(t);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Ty)) {
            return false;
        }
        Ty t = (Ty) o;
        return k == t.k && cls == t.cls && opt == t.opt && Arrays.equals(sites, t.sites); //NOPMD CompareObjectsWithEquals - identity is meant
    }

    @Override
    public int hashCode() {
        return k.hashCode() * 31 + (cls == null ? 0 : cls.javaName.hashCode()) + (opt ? 7 : 0);
    }

    @Override
    public String toString() {
        String s = k == K.INST ? cls.def.name : k == K.LIST ? "list" + Arrays.toString(sites) : k.name();
        return opt ? s + "?" : s;
    }
}
