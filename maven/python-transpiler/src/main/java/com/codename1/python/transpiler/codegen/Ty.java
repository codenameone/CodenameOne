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
/// or None) goes to `OBJECT`, which is the plain `Object` the generic code uses:
///
/// | kind | Java | None |
/// |---|---|---|
/// | `BOOL` | `boolean` | -- |
/// | `FLOAT` | `double` | -- |
/// | `INT` | `Object` (a `Long`, or a big int) | -- |
/// | `STR` | `String` | null when `opt` |
/// | `BYTES` | `Object` (a `PyBytes`) | -- |
/// | `INST` | the class's layout class | null when `opt` |
/// | `LIST` | `PyList` | null when `opt` |
/// | `TUPLE` | `Object` (the generic tuple) | -- |
/// | `DICT` | `Object` (a `PyDict`) | -- |
/// | `CLS` | `Object` (one of the classes whose ids are `sites`, any class when there are none) | `Py.NONE` when `opt` |
/// | `OBJ` | `Object` (a runtime object of the named builtin type) | `Py.NONE` when `opt` |
/// | `METH` | `Object` (a bound method of one of the compiled methods whose ids are `sites`) | `Py.NONE` when `opt` |
/// | `NONE`, `OBJECT`, `BOT` | `Object` | `Py.NONE` |
///
/// An int stays an object because a Python int has no bound: overflowing a
/// `long` local would have nowhere to go without deoptimizing mid-statement.
/// On ParparVM a `Long` below 2^60 is a tagged immediate, so keeping it
/// boxed allocates nothing.
final class Ty {
    enum K { BOT, NONE, BOOL, INT, FLOAT, STR, BYTES, INST, LIST, TUPLE, DICT, OBJ, CLS, METH, FUNC, UNION, OBJECT }

    static final Ty BOT = new Ty(K.BOT, null, false, null);
    static final Ty NONE = new Ty(K.NONE, null, false, null);
    static final Ty BOOL = new Ty(K.BOOL, null, false, null);
    static final Ty INT = new Ty(K.INT, null, false, null);
    static final Ty FLOAT = new Ty(K.FLOAT, null, false, null);
    static final Ty STR = new Ty(K.STR, null, false, null);
    static final Ty BYTES = new Ty(K.BYTES, null, false, null);
    static final Ty OBJECT = new Ty(K.OBJECT, null, false, null);

    /// A value from a source the pass cannot see: a computed attribute, an
    /// element of a container it does not follow, the result of a call of
    /// something it does not know. It is no evidence of what a slot holds,
    /// so a join ignores it ([#join]); a slot every source of which is
    /// unseen is held as [#OBJECT] is. Typed code converts such a value to the
    /// slot's type, checked, where it enters.
    static final Ty UNSEEN = new Ty(K.OBJECT, null, false, null, "unseen");

    /// [#UNSEEN] joined with None: a type the other sources say holds the
    /// None as well.
    static final Ty UNSEEN_NONE = new Ty(K.OBJECT, null, true, null, "unseen");

    /// The list sites a `LIST` value may have come from (see `Infer`). Past
    /// this many it is any list at all. That is not a small loss: an "any
    /// list" that escapes escapes every list of the program
    /// (`Infer.escape`), and with a cap of 8 one pandas call and one in
    /// charset_normalizer did -- so every list in bs4 and markdownify, a
    /// tag's `contents` among them, held untyped elements. A set of sites
    /// stays exact well past what a real program merges.
    static final int MAX_SITES = 4096;

    final K k;
    final Layouts.Cls cls;
    final boolean opt;
    /// Sorted allocation sites of a `LIST`; empty means "any list".
    final int[] sites;
    /// The builtin type an `OBJ` value is an instance of (`set`,
    /// `ValueError`, `re.Pattern`).
    final String name;
    /// UNION: the types a value may be, two or more, none of them a union,
    /// OBJECT or BOT, at most one of each kind except INST and OBJ (one per
    /// class tree / per name), in a fixed order. A union is a known set:
    /// whatever the program does with the value, it does to one of these.
    final Ty[] alts;

    private Ty(K k, Layouts.Cls cls, boolean opt, int[] sites) {
        this(k, cls, opt, sites, null);
    }

    private Ty(K k, Layouts.Cls cls, boolean opt, int[] sites, String name) {
        this(k, cls, opt, sites, name, null);
    }

    private Ty(K k, Layouts.Cls cls, boolean opt, int[] sites, String name, Ty[] alts) {
        this.k = k;
        this.cls = cls;
        this.opt = opt;
        this.sites = sites;
        this.name = name;
        this.alts = alts;
    }

    /// The types this one may be: its union members, or itself.
    Ty[] members() {
        return k == K.UNION ? alts.clone() : new Ty[] {this};
    }

    /// Whether this is a value the analysis cannot name one type for: OBJECT,
    /// or a union of several.
    /// Whether this is [#UNSEEN].
    boolean unseen() {
        return k == K.OBJECT && "unseen".equals(name);
    }

    boolean wide() {
        return k == K.OBJECT || k == K.UNION;
    }

    /// The class object itself, one of the compiled classes whose ids
    /// (`Infer.Program#clsId`) are `ids`; held as the generic object.
    static Ty cls(int[] ids) {
        return new Ty(K.CLS, null, false, ids);
    }

    /// A bound method of one of the compiled methods whose ids
    /// (`Infer.Program#methId`) are `ids`, held as the generic object.
    /// Only locals and results hold one: it reaches no parameter, field
    /// or container (`Infer#noMeth`), so no guard has to test for it.
    static Ty meth(int[] ids) {
        return new Ty(K.METH, null, false, ids);
    }

    /// One of the functions whose ids (`Infer.Program#funcId`) are `ids`, as
    /// a value: a nested function, a lambda, a module function named
    /// without being called. The inference follows it to where it is
    /// called; generated code holds it as an object (`Infer#loc`).
    static Ty func(int[] ids) {
        return new Ty(K.FUNC, null, false, ids);
    }

    /// An instance of the builtin type `name`, held as the generic object.
    static Ty obj(String name) {
        return new Ty(K.OBJ, null, false, null, name);
    }

    static Ty inst(Layouts.Cls c, boolean opt) {
        return new Ty(K.INST, c, opt, null);
    }

    static Ty list(int[] sites, boolean opt) {
        return new Ty(K.LIST, null, opt, sites);
    }

    /// A tuple from these allocation sites. It is held as the generic
    /// object; what the kind adds is the type of each position (see
    /// `Infer.slot`), so unpacking it or indexing it with a constant gives
    /// typed values instead of dynamic ones.
    static Ty tuple(int[] sites) {
        return new Ty(K.TUPLE, null, false, sites);
    }

    /// A dict from these allocation sites. It is held as the generic object;
    /// what the kind adds is the type of its keys and values (see
    /// `Infer.dictVal`), so a read of it gives a typed value.
    static Ty dict(int[] sites) {
        return new Ty(K.DICT, null, false, sites);
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
            case OBJ:
            case CLS:
            case METH:
            case FUNC:
                return opt ? this : new Ty(k, null, true, sites, name);
            case OBJECT:
                return this;
            default:
                // An int, a float, a tuple ... or None: a union with None.
                return unionOf(this, NONE);
        }
    }

    /// The same type without None, for a value already tested non-None.
    Ty nonNone() {
        return opt ? new Ty(k, cls, false, sites, name) : this;
    }

    /// Whether the representation can hold None (as Java null).
    boolean nullable() {
        return opt && (k == K.STR || k == K.INST || k == K.LIST);
    }

    boolean isObject() {
        return k == K.BOT || k == K.NONE || k == K.OBJECT || k == K.UNION || k == K.INT || k == K.BYTES || k == K.TUPLE || k == K.DICT || k == K.OBJ || k == K.CLS || k == K.METH || k == K.FUNC;
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
        // An unseen value is no evidence (UNSEEN), but None is no type to
        // hold the rest in: a slot only None reached has nothing to guard,
        // so a value besides it stays unseen rather than claiming None.
        // It remembers the None, though: the type the other sources say
        // holds it as well.
        if (a.unseen() || b.unseen()) {
            Ty u = a.unseen() ? a : b;
            Ty o = a.unseen() ? b : a;
            if (o.k == K.NONE || o.unseen()) {
                return u.opt || o.k == K.NONE || o.opt ? UNSEEN_NONE : UNSEEN;
            }
            return u.opt ? o.orNone() : o;
        }
        if (a.k == K.OBJECT || b.k == K.OBJECT) {
            return OBJECT;
        }
        if (a.k == K.UNION || b.k == K.UNION) {
            return unionOf(a, b);
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
            return unionOf(a, b);
        }
        boolean opt = a.opt || b.opt;
        switch (a.k) {
            case INST: {
                Layouts.Cls c = common(a.cls, b.cls);
                return c == null ? unionOf(a, b) : inst(c, opt);
            }
            case LIST:
                return list(union(a.sites, b.sites), opt);
            case TUPLE:
                return tuple(union(a.sites, b.sites));
            case DICT:
                return dict(union(a.sites, b.sites));
            case CLS: {
                // No ids: any class at all.
                Ty r = cls(union(a.sites, b.sites));
                return opt ? r.orNone() : r;
            }
            case METH: {
                Ty r = meth(union(a.sites, b.sites));
                return r.sites.length == 0 ? OBJECT : opt ? r.orNone() : r;
            }
            case FUNC: {
                Ty r = func(union(a.sites, b.sites));
                return opt ? r.orNone() : r;
            }
            case STR:
                return opt ? STR.orNone() : STR;
            case OBJ:
                return a.name.equals(b.name) ? (opt ? a.orNone() : a.nonNone()) : unionOf(a, b);
            default:
                return a;
        }
    }

    /// The union of `a` and `b` (either may be one): each member of `b`
    /// joins the member of `a` it shares a kind with -- a class tree, for
    /// an instance -- or is added beside them. None is a member of its
    /// own when no member can hold it as `opt`.
    private static Ty unionOf(Ty a, Ty b) {
        java.util.List<Ty> acc = new java.util.ArrayList<Ty>(Arrays.asList(a.members()));
        for (Ty m : b.members()) {
            add(acc, m);
        }
        // A member that can be None absorbs a None member.
        int none = -1;
        for (int i = 0; i < acc.size(); i++) {
            if (acc.get(i).k == K.NONE) {
                none = i;
            }
        }
        if (none >= 0) {
            for (int i = 0; i < acc.size(); i++) {
                Ty m = acc.get(i);
                if (i != none && (m.k == K.STR || m.k == K.INST || m.k == K.LIST || m.k == K.OBJ
                        || m.k == K.CLS || m.k == K.METH || m.k == K.FUNC)) {
                    acc.set(i, m.orNone());
                    acc.remove(none);
                    break;
                }
            }
        }
        if (acc.size() == 1) {
            return acc.get(0);
        }
        Ty[] r = acc.toArray(new Ty[0]);
        Arrays.sort(r, (x, y) -> x.toString().compareTo(y.toString()));
        return new Ty(K.UNION, null, false, null, null, r);
    }

    private static void add(java.util.List<Ty> acc, Ty m) {
        for (int i = 0; i < acc.size(); i++) {
            Ty o = acc.get(i);
            boolean same = o.k == m.k || o.k == K.INT && m.k == K.BOOL || o.k == K.BOOL && m.k == K.INT;
            if (same && o.k == K.INST && common(o.cls, m.cls) == null) {
                same = false;
            }
            if (same && o.k == K.OBJ && !o.name.equals(m.name)) {
                same = false;
            }
            if (same) {
                Ty j = join(o, m);
                if (j.k == K.UNION || j.k == K.OBJECT) {
                    continue;
                }
                acc.set(i, j);
                return;
            }
        }
        acc.add(m);
    }

    /// The nearest class both derive from. An analysed class's compiled
    /// ancestors are not shared with it: generated code takes an instance
    /// of a compiled class to be one of its Java class, which an analysed
    /// class's instance is not.
    private static Layouts.Cls common(Layouts.Cls a, Layouts.Cls b) {
        for (Layouts.Cls x = a; x != null && x.compiled == a.compiled; x = x.parent) {
            for (Layouts.Cls y = b; y != null && y.compiled == b.compiled; y = y.parent) {
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
        return k == t.k && cls == t.cls && opt == t.opt && Arrays.equals(sites, t.sites) && Arrays.equals(alts, t.alts) //NOPMD CompareObjectsWithEquals - classes are interned
                && (name == null ? t.name == null : name.equals(t.name)); //NOPMD CompareObjectsWithEquals - identity is meant
    }

    @Override
    public int hashCode() {
        return k.hashCode() * 31 + (cls == null ? 0 : cls.javaName.hashCode()) + (opt ? 7 : 0);
    }

    @Override
    public String toString() {
        if (k == K.UNION) {
            StringBuilder u = new StringBuilder();
            for (Ty m : alts) {
                u.append(u.length() == 0 ? "" : " | ").append(m);
            }
            return u.toString();
        }
        String s = k == K.INST ? cls.def.name : k == K.LIST ? "list" + Arrays.toString(sites)
                : k == K.TUPLE ? "tuple" + Arrays.toString(sites)
                : k == K.DICT ? "dict" + Arrays.toString(sites) : k == K.OBJ ? "<" + name + ">" : k == K.CLS ? "class" + Arrays.toString(sites) : k == K.METH ? "method" + Arrays.toString(sites) : k == K.FUNC ? "function" + Arrays.toString(sites) : unseen() ? "unseen" : k.name();
        return opt ? s + "?" : s;
    }
}
