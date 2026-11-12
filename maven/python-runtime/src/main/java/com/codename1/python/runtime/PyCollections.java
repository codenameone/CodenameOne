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

/// `_cn1collections`: the parts of `collections` CPython runs in C
/// (`_collections`, `_tuplegetter`), which the Python module builds on.
/// A namedtuple's `__new__` took `*args, **kwargs` and filled a list field
/// by field on every construction (`urlparse` builds one per link), each
/// field read went through `property(itemgetter(i))`, and `defaultdict()`
/// bound `*args, **kwargs` to pass nothing on to `dict.__init__`.
final class PyCollections {

    private PyCollections() {
    }

    static PyModule module() {
        PyModule m = new PyModule("_cn1collections", new String[0]);
        m.setAttr("tuplegetter", PyBuiltin.fixed2("tuplegetter", (i, doc) -> tupleGetter((int) Py.index(i))));
        m.setAttr("nt_new", PyBuiltin.fixed3("nt_new", (tn, names, defs) ->
                ntNew(Py.toStr(tn), ((PyTuple) names).items, (PyDict) defs)));
        m.setAttr("dd_init", new PyBuiltin("__init__", PyCollections::ddInit)
                .with1(self -> ddInit(self, Py.NONE)).with2(PyCollections::ddInit));
        m.setAttr("dd_missing", PyBuiltin.fixed2("__missing__", PyCollections::missing));
        return m;
    }

    /// The property a namedtuple field reads through: item `i` of the tuple.
    private static PyProperty tupleGetter(final int i) {
        return new PyProperty(PyBuiltin.fixed1("_tuplegetter", self -> item(self, i)), Py.NONE, Py.NONE);
    }

    private static Object item(Object self, int i) {
        if (self instanceof PyInstance) {
            Object b = ((PyInstance) self).base;
            if (b instanceof PyTuple) {
                Object[] items = ((PyTuple) b).items;
                if (i < items.length) {
                    return items[i];
                }
            }
        }
        return Py.getitem(self, Long.valueOf(i));
    }

    /// The namedtuple instance of `cls` holding `vals`.
    private static Object build(Object cls, Object[] vals) {
        if (cls instanceof PyType && cls != Py.TUPLE_TYPE && ((PyType) cls).isSubtype(Py.TUPLE_TYPE)) { //NOPMD CompareObjectsWithEquals - identity is meant
            PyInstance o = ((PyType) cls).newInstance();
            o.base = new PyTuple(vals);
            return o;
        }
        return Py.obj(Py.getattr(Py.TUPLE_TYPE, "__new__")).call2(cls, new PyTuple(vals));
    }

    /// The `__new__` of a namedtuple with fields `names`: a call passing
    /// every field positionally builds the tuple from its arguments, any
    /// other binds as the generated `__new__(_cls, a, b=..., ...)` would.
    private static PyBuiltin ntNew(final String tn, final Object[] names, final PyDict defs) {
        final int n = names.length;
        PyBuiltin b = new PyBuiltin("__new__", (args, kw) -> ntBind(tn, names, defs, args, kw));
        switch (n) {
            case 0:
                b.with1(c -> build(c, new Object[0]));
                break;
            case 1:
                b.with2((c, x0) -> build(c, new Object[] {x0}));
                break;
            case 2:
                b.with3((c, x0, x1) -> build(c, new Object[] {x0, x1}));
                break;
            case 3:
                b.with4((c, x0, x1, x2) -> build(c, new Object[] {x0, x1, x2}));
                break;
            case 4:
                b.with5((c, x0, x1, x2, x3) -> build(c, new Object[] {x0, x1, x2, x3}));
                break;
            case 5:
                b.with6((c, x0, x1, x2, x3, x4) -> build(c, new Object[] {x0, x1, x2, x3, x4}));
                break;
            case 6:
                b.with7((c, x0, x1, x2, x3, x4, x5) -> build(c, new Object[] {x0, x1, x2, x3, x4, x5}));
                break;
            default:
                break;
        }
        return b;
    }

    private static Object ntBind(String tn, Object[] names, PyDict defs, Object[] args, String[] kw) {
        int npos = args.length - kw.length;
        if (npos < 1) {
            throw Py.typeError(tn + ".__new__(): not enough arguments");
        }
        int n = names.length;
        int given = npos - 1;
        if (given > n) {
            int d = defs.size();
            throw Py.typeError(tn + ".__new__() takes " + (d == 0 ? "" : "from " + (n - d + 1) + " to ") + (n + 1)
                    + " positional arguments but " + npos + " were given");
        }
        Object[] vals = new Object[n];
        System.arraycopy(args, 1, vals, 0, given);
        boolean[] used = new boolean[kw.length];
        for (int k = given; k < n; k++) {
            String name = (String) names[k];
            int j = 0;
            while (j < kw.length && !kw[j].equals(name)) {
                j++;
            }
            if (j < kw.length) {
                vals[k] = args[npos + j];
                used[j] = true;
                continue;
            }
            Object d = defs.getOrNull(name);
            if (d == null) {
                throw Py.typeError(tn + ".__new__() missing 1 required positional argument: "
                        + Py.obj(name).repr());
            }
            vals[k] = d;
        }
        for (int j = 0; j < kw.length; j++) {
            if (!used[j]) {
                for (Object f : names) {
                    if (kw[j].equals(f)) {
                        throw Py.typeError(tn + ".__new__() got multiple values for argument "
                                + Py.obj(kw[j]).repr());
                    }
                }
                throw Py.typeError(tn + ".__new__() got an unexpected keyword argument " + Py.obj(kw[j]).repr());
            }
        }
        return build(args[0], vals);
    }

    /// `defaultdict.__init__(self, default_factory=None, /, *args, **kwargs)`.
    private static Object ddInit(Object[] args, String[] kw) {
        int npos = args.length - kw.length;
        if (npos < 1) {
            throw Py.typeError("descriptor '__init__' of 'collections.defaultdict' object needs an argument");
        }
        Object self = args[0];
        Object f = npos > 1 ? args[1] : Py.NONE;
        int skip = npos > 1 ? 2 : 1;
        if (args.length > skip) {
            Object[] rest = new Object[args.length - skip + 1];
            rest[0] = self;
            System.arraycopy(args, skip, rest, 1, args.length - skip);
            checkFactory(f);
            Py.obj(Py.DICT_TYPE.lookup("__init__")).call(rest, kw);
        }
        return ddInit(self, f);
    }

    private static Object ddInit(Object self, Object f) {
        checkFactory(f);
        Py.setattr(self, "default_factory", f);
        return Py.NONE;
    }

    private static void checkFactory(Object f) {
        if (f != Py.NONE && !Builtins.isCallable(f)) { //NOPMD CompareObjectsWithEquals - identity is meant
            throw Py.typeError("first argument must be callable or None");
        }
    }

    /// `defaultdict.__missing__(self, key)`.
    private static Object missing(Object self, Object key) {
        Object f = Py.getattr(self, "default_factory");
        if (f == Py.NONE) { //NOPMD CompareObjectsWithEquals - identity is meant
            throw Py.keyError(key);
        }
        Object v = Py.obj(f).call0();
        Py.setitem(self, key, v);
        return v;
    }
}
