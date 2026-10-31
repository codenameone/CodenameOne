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

/// Structural pattern matching: the `match` statement.
///
/// The transpiler rewrites each `case` into a call of [#match] with a
/// description of the pattern built of tuples, whose first item says what
/// the pattern is:
///
/// - `("_",)` matches anything; `("c", name)` captures the subject;
/// - `("v", value)` compares with `==`, `("s", value)` with `is`;
/// - `("q", p...)` a sequence, where `("*", name or None)` is a star;
/// - `("m", keys, patterns, rest name or None)` a mapping;
/// - `("k", cls, positional, keyword names, keyword patterns)` a class;
/// - `("|", alternative...)` and `("as", pattern, name)`.
///
/// The result is the dict of captured names, or None for no match.
public final class PyMatch {
    private PyMatch() {
    }

    public static Object match(Object pattern, Object subject) {
        PyDict out = new PyDict();
        return match(pattern, subject, out) ? out : Py.NONE;
    }

    private static Object[] parts(Object pattern) {
        if (pattern instanceof PyTuple) {
            return ((PyTuple) pattern).items;
        }
        throw Py.runtimeError("internal error: bad pattern");
    }

    private static boolean match(Object pattern, Object s, PyDict out) {
        Object[] p = parts(pattern);
        String tag = (String) p[0];
        if ("_".equals(tag)) {
            return true;
        }
        if ("c".equals(tag)) {
            out.setItem(p[1], s);
            return true;
        }
        if ("v".equals(tag)) {
            return Py.eqBool(s, p[1]);
        }
        if ("s".equals(tag)) {
            return s == p[1]; //NOPMD CompareObjectsWithEquals - identity is meant
        }
        if ("as".equals(tag)) {
            if (!match(p[1], s, out)) {
                return false;
            }
            out.setItem(p[2], s);
            return true;
        }
        if ("|".equals(tag)) {
            for (int i = 1; i < p.length; i++) {
                PyDict alt = new PyDict();
                if (match(p[i], s, alt)) {
                    out.update(alt);
                    return true;
                }
            }
            return false;
        }
        if ("q".equals(tag)) {
            return sequence(p, s, out);
        }
        if ("m".equals(tag)) {
            return mapping(p, s, out);
        }
        if ("k".equals(tag)) {
            return cls(p, s, out);
        }
        throw Py.runtimeError("internal error: bad pattern '" + tag + "'");
    }

    private static Object storage(Object s) {
        return s instanceof PyInstance && ((PyInstance) s).base != null ? ((PyInstance) s).base : s;
    }

    private static boolean sequence(Object[] p, Object s, PyDict out) {
        Object v = storage(s);
        if (!(v instanceof PyList || v instanceof PyTuple || v instanceof PyRange)) {
            return false;
        }
        Object[] items = Py.toArray(v);
        int n = p.length - 1;
        int star = -1;
        for (int i = 0; i < n; i++) {
            if ("*".equals(parts(p[i + 1])[0])) {
                star = i;
            }
        }
        if (star < 0 ? items.length != n : items.length < n - 1) {
            return false;
        }
        if (star < 0) {
            for (int i = 0; i < n; i++) {
                if (!match(p[i + 1], items[i], out)) {
                    return false;
                }
            }
            return true;
        }
        int after = n - star - 1;
        for (int i = 0; i < star; i++) {
            if (!match(p[i + 1], items[i], out)) {
                return false;
            }
        }
        for (int i = 0; i < after; i++) {
            if (!match(p[star + 2 + i], items[items.length - after + i], out)) {
                return false;
            }
        }
        Object name = parts(p[star + 1])[1];
        if (name != Py.NONE) { //NOPMD CompareObjectsWithEquals - identity is meant
            PyList rest = new PyList();
            for (int i = star; i < items.length - after; i++) {
                rest.append(items[i]);
            }
            out.setItem(name, rest);
        }
        return true;
    }

    private static boolean mapping(Object[] p, Object s, PyDict out) {
        Object v = storage(s);
        if (!(v instanceof PyDict)) {
            return false;
        }
        PyDict d = (PyDict) v;
        Object[] keys = parts(p[1]);
        Object[] pats = parts(p[2]);
        for (int i = 0; i < keys.length; i++) {
            Object x = d.getOrNull(keys[i]);
            if (x == null || !match(pats[i], x, out)) {
                return false;
            }
        }
        if (p[3] != Py.NONE) { //NOPMD CompareObjectsWithEquals - identity is meant
            PyDict rest = new PyDict();
            Object[] all = d.keyArray();
            for (Object e : all) {
                boolean named = false;
                for (int j = 0; j < keys.length && !named; j++) {
                    named = Py.eqBool(e, keys[j]);
                }
                if (!named) {
                    rest.setItem(e, d.getItem(e));
                }
            }
            out.setItem(p[3], rest);
        }
        return true;
    }

    /// The builtins whose one positional sub-pattern matches the subject itself.
    private static boolean selfMatching(PyType t) {
        return t == Py.BOOL_TYPE || t == Py.BYTES_TYPE || t == Py.DICT_TYPE || t == Py.FLOAT_TYPE //NOPMD CompareObjectsWithEquals - identity is meant
                || t == Py.FROZENSET_TYPE || t == Py.INT_TYPE || t == Py.LIST_TYPE || t == Py.SET_TYPE //NOPMD CompareObjectsWithEquals - identity is meant
                || t == Py.STR_TYPE || t == Py.TUPLE_TYPE; //NOPMD CompareObjectsWithEquals - identity is meant
    }

    private static boolean cls(Object[] p, Object s, PyDict out) {
        if (!(p[1] instanceof PyType)) {
            throw Py.typeError("called match pattern must be a class");
        }
        PyType t = (PyType) p[1];
        if (!Py.isinstance(s, t)) {
            return false;
        }
        Object[] pos = parts(p[2]);
        Object[] kwNames = parts(p[3]);
        Object[] kwPats = parts(p[4]);
        if (pos.length > 0) {
            if (selfMatching(t)) {
                if (pos.length > 1) {
                    throw Py.typeError(t.name + "() accepts 1 positional sub-pattern (" + pos.length + " given)");
                }
                if (!match(pos[0], s, out)) {
                    return false;
                }
            } else {
                Object ma = t.getAttrOrNull("__match_args__");
                Object[] names = ma == null ? new Object[0] : Py.toArray(ma);
                if (pos.length > names.length) {
                    throw Py.typeError(t.name + "() accepts " + names.length + " positional sub-pattern"
                            + (names.length == 1 ? "" : "s") + " (" + pos.length + " given)");
                }
                for (int i = 0; i < pos.length; i++) {
                    Object a = Py.obj(s).getAttrOrNull(Py.toStr(names[i]));
                    if (a == null || !match(pos[i], a, out)) {
                        return false;
                    }
                }
            }
        }
        for (int i = 0; i < kwNames.length; i++) {
            Object a = Py.obj(s).getAttrOrNull(Py.toStr(kwNames[i]));
            if (a == null || !match(kwPats[i], a, out)) {
                return false;
            }
        }
        return true;
    }
}
