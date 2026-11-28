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

import java.util.concurrent.atomic.AtomicReference;

/// `enum.EnumType`, a builtin metaclass the runtime implements, as `Abc` is
/// for `abc.ABCMeta`. Its class-level protocol -- calling the class looks a
/// value up, iterating it walks the members, `in`, `len`, `[name]`,
/// `__members__`, `repr` -- is written here, over the tables the class keeps
/// (`_member_map_`, `_member_names_`, `_value2member_map_`), which the
/// compiler fills once, member by member, right after the class statement
/// (the transpiler's EnumLowering). Nothing here makes a member or looks
/// one up by a name computed at run time. What a Python metaclass's
/// `__new__` did besides -- the tables themselves -- runs right after the
/// class is made ([#init]), so the class is an ordinary class with a layout.
final class EnumMeta {
    private EnumMeta() {
    }

    /// The metaclass object `enum.EnumType` is bound to.
    static final PyType META = metaType();

    /// `enum._finish(cls, boundary)`, which sets the class's tables up.
    private static final AtomicReference<Object> FINISH = new AtomicReference<Object>();

    private static PyType metaType() {
        PyType t = new PyType("EnumType", new PyType[] {Py.TYPE_TYPE}, null);
        t.dict.put("__module__", "enum");
        t.dict.put("__iter__", PyBuiltin.method1(t, "__iter__", EnumMeta::iterOf));
        t.dict.put("__reversed__", PyBuiltin.method1(t, "__reversed__", EnumMeta::reversedOf));
        t.dict.put("__len__", PyBuiltin.method1(t, "__len__", k -> (long) len((PyType) k)));
        t.dict.put("__contains__", PyBuiltin.method2(t, "__contains__",
                (k, v) -> Py.bool(contains((PyType) k, v))));
        t.dict.put("__getitem__", PyBuiltin.method2(t, "__getitem__", (k, n) -> getItem((PyType) k, n)));
        t.dict.put("__bool__", PyBuiltin.method1(t, "__bool__", k -> Py.bool(true)));
        t.dict.put("__repr__", PyBuiltin.method1(t, "__repr__", k -> repr((PyType) k)));
        t.dict.put("__members__", new PyProperty(PyBuiltin.fixed1("__members__",
                k -> table((PyType) k, "_member_map_").copy()), null, null));
        return t;
    }

    /// Whether `k` is made by this metaclass (or a Python one deriving from it).
    static boolean governs(PyType k) {
        return k.meta != null && k.meta.isSubtype(META);
    }

    /// `_cn1op.enum_install(finish)`: keeps `enum._finish` for [#init].
    static Object install(Object finish) {
        FINISH.set(finish);
        return Py.NONE;
    }

    /// Sets up the tables of the class `k` just made, with the
    /// `boundary=` keyword of its class statement (None when absent).
    static void init(PyType k, Object boundary) {
        Object f = FINISH.get();
        if (f != null) {
            Py.obj(f).call(new Object[] {k, boundary}, new String[0]);
        }
    }

    private static PyDict table(PyType k, String name) {
        Object t = k.lookup(name);
        if (!(t instanceof PyDict)) {
            throw Py.typeError("enum " + k.name + " has no " + name);
        }
        return (PyDict) t;
    }

    private static PyList names(PyType k) {
        Object t = k.lookup("_member_names_");
        if (!(t instanceof PyList)) {
            throw Py.typeError("enum " + k.name + " has no _member_names_");
        }
        return (PyList) t;
    }

    /// The members of `k`, in definition order.
    private static PyList members(PyType k) {
        PyList n = names(k);
        PyDict map = table(k, "_member_map_");
        PyList out = new PyList(n.size());
        for (int i = 0; i < n.size(); i++) {
            out.append(map.getItem(n.get(i)));
        }
        return out;
    }

    static Object iterOf(Object k) {
        return Py.iter(members((PyType) k));
    }

    static Object reversedOf(Object k) {
        PyList m = members((PyType) k);
        PyList out = new PyList(m.size());
        for (int i = m.size() - 1; i >= 0; i--) {
            out.append(m.get(i));
        }
        return Py.iter(out);
    }

    static int len(PyType k) {
        return names(k).size();
    }

    static Object getItem(PyType k, Object name) {
        return table(k, "_member_map_").getItem(name);
    }

    static String repr(PyType k) {
        Object flag = k.lookup("_is_flag_");
        return (flag != null && Py.isTrue(flag) ? "<flag '" : "<enum '") + k.name + "'>";
    }

    static boolean contains(PyType k, Object v) {
        if (Py.typeOf(v).isSubtype(k)) {
            return true;
        }
        if (hashable(v) && table(k, "_value2member_map_").getOrNull(v) != null) {
            return true;
        }
        Object u = k.lookup("_unhashable_values_");
        return u instanceof PyList && ((PyList) u).contains(v);
    }

    private static boolean hashable(Object v) {
        return !(v instanceof PyList) && !(v instanceof PyDict) && !(v instanceof PySet);
    }

    /// `cls(value)`: the member whose value is `value`, else what the
    /// class's `_missing_` answers. Never makes a member: those are all
    /// made once, after the class statement. The functional API
    /// (`Color = Enum('Color', 'RED GREEN')`) is lowered to a class statement by the
    /// compiler; any form it could not lower is refused.
    static Object call(PyType k, Object[] args, String[] kwNames) {
        if (args.length != 1 || kwNames.length != 0) {
            throw Py.typeError(k.name + "(): the functional enum API is supported only as a module-level"
                    + " assignment of Enum(name, names), names a string or a list of literals, which the compiler"
                    + " turns into a class statement");
        }
        Object value = args[0];
        if (Py.typeOf(value).isSubtype(k)) {
            return value;
        }
        if (hashable(value)) {
            Object m = table(k, "_value2member_map_").getOrNull(value);
            if (m != null) {
                return m;
            }
        }
        Object result = Py.call(Py.getattr(k, "_missing_"), value);
        if (Py.typeOf(result).isSubtype(k)) {
            return result;
        }
        if (result == Py.NONE) { //NOPMD CompareObjectsWithEquals - identity is meant
            throw Py.valueError(Py.obj(value).repr() + " is not a valid " + k.name);
        }
        throw Py.typeError("error in " + k.name + "._missing_: returned " + Py.obj(result).repr()
                + " instead of None or a valid member");
    }

    /// `cls.name = value`: refuses to rebind a member to anything else.
    static void setAttr(PyType k, String name, Object value) {
        Object t = k.lookup("_member_map_");
        if (t instanceof PyDict) {
            Object old = ((PyDict) t).getOrNull(name);
            if (old != null && old != value) { //NOPMD CompareObjectsWithEquals - identity is meant
                throw Py.attributeError("cannot reassign member '" + name + "'");
            }
        }
        k.setAttrDirect(name, value);
    }
}
