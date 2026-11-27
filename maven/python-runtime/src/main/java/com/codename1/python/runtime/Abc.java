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

import java.util.ArrayList;
import java.util.TreeSet;

/// `abc.ABCMeta`, done by the runtime as CPython's `_abc` does it in C.
///
/// The build is closed world: a class whose metaclass is a Python class
/// deriving from `type` has no layout, and the old `abc.py` metaclass also
/// read every abstract name with a computed `getattr`. Here `ABCMeta` is a
/// builtin metaclass the runtime knows by identity, so a class it governs
/// keeps a layout and the fast construction paths: [PyType#finish] computes
/// `__abstractmethods__` once, when the class is made, constructing a class
/// with any of them left raises the TypeError `ABCMeta.__call__` raised, and
/// `isinstance`/`issubclass` consult `__subclasshook__`, the registry and the
/// registries of the class's subclasses, in that order, as before.
final class Abc {
    private Abc() {
    }

    /// The metaclass object `abc.ABCMeta` is bound to.
    static final PyType META = metaType();

    private static PyType metaType() {
        PyType t = new PyType("ABCMeta", new PyType[] {Py.TYPE_TYPE}, null);
        t.dict.put("__module__", "abc");
        t.define("register", PyBuiltin.method(t, "register", a -> register(a[0], a[1])));
        // Called by name too (a Python metaclass deriving from ABCMeta and
        // calling super()); the runtime itself goes to subclassCheck.
        t.define("__instancecheck__", PyBuiltin.method(t, "__instancecheck__",
                a -> Py.bool(subclassCheck((PyType) a[0], Py.typeOf(a[1])))));
        t.define("__subclasscheck__", PyBuiltin.method(t, "__subclasscheck__",
                a -> Py.bool(a[1] instanceof PyType && subclassCheck((PyType) a[0], (PyType) a[1]))));
        return t;
    }

    static boolean governs(PyType k) {
        // A Python metaclass deriving from ABCMeta keeps these checks unless
        // it overrides them, as it would in CPython.
        return k.meta != null && k.meta.isSubtype(META) && !(k.meta.lookup("__instancecheck__") instanceof PyFunction)
                && !(k.meta.lookup("__subclasscheck__") instanceof PyFunction);
    }

    /// `ABCMeta.register(cls, subclass)`: a virtual subclass, answered by
    /// [#subclassCheck] from now on. Returns the subclass, so it decorates.
    static Object register(Object cls, Object subclass) {
        if (!(cls instanceof PyType) || !(subclass instanceof PyType)) {
            throw Py.typeError("Can only register classes");
        }
        PyType k = (PyType) cls;
        PyType s = (PyType) subclass;
        if (!subclassCheck(k, s)) {
            if (k.abcRegistry == null) {
                k.abcRegistry = new ArrayList<PyType>();
            }
            k.abcRegistry.add(s);
        }
        return subclass;
    }

    /// What `ABCMeta.__new__` computed: the names bound abstract in the
    /// class body, and those abstract in a base that the class still finds
    /// abstract, as a frozenset in the class dict.
    static void init(PyType k) {
        TreeSet<String> names = new TreeSet<String>();
        for (java.util.Map.Entry<String, Object> e : k.dict.entrySet()) {
            if (isAbstract(e.getValue())) {
                names.add(e.getKey());
            }
        }
        for (PyType b : k.bases) {
            Object am = b.lookup("__abstractmethods__");
            if (am instanceof PySet) {
                for (Object n : ((PySet) am).toArray()) {
                    if (n instanceof String && isAbstract(k.lookup((String) n))) {
                        names.add((String) n);
                    }
                }
            }
        }
        Object[] items = names.toArray();
        k.dict.put("__abstractmethods__", PySet.frozenConstants(items));
        k.abstractNames = items.length == 0 ? null : names.toArray(new String[items.length]);
    }

    /// The TypeError constructing a class with abstract methods raises.
    static PyException instantiationError(PyType k) {
        String[] n = k.abstractNames;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n.length; i++) {
            sb.append(i == 0 ? "" : ", ").append('\'').append(n[i]).append('\'');
        }
        return Py.typeError("Can't instantiate abstract class " + k.name + " without an implementation for abstract method"
                + (n.length > 1 ? "s " : " ") + sb);
    }

    static boolean isAbstract(Object v) {
        Object f = v;
        if (f instanceof PyClassMethod) {
            f = ((PyClassMethod) f).func;
        } else if (f instanceof PyStaticMethod) {
            f = ((PyStaticMethod) f).func;
        } else if (f instanceof PyProperty) {
            PyProperty p = (PyProperty) f;
            return flagged(p.fget) || flagged(p.fset) || flagged(p.fdel);
        }
        return flagged(f);
    }

    private static boolean flagged(Object f) {
        if (f == null || f == Py.NONE) { //NOPMD CompareObjectsWithEquals - identity is meant
            return false;
        }
        Object a = Py.obj(f).getAttrOrNull("__isabstractmethod__");
        return a != null && Py.isTrue(a);
    }

    /// `ABCMeta.__subclasscheck__(cls, c)`.
    static boolean subclassCheck(PyType cls, PyType c) {
        if (c.isSubtype(cls)) {
            return true;
        }
        Object hook = cls.lookup("__subclasshook__") == null ? null : cls.getAttrOrNull("__subclasshook__");
        if (hook != null) {
            Object ok = Py.obj(hook).call1(c);
            if (ok != Py.NOT_IMPLEMENTED) { //NOPMD CompareObjectsWithEquals - identity is meant
                return Py.isTrue(ok);
            }
        }
        if (cls.abcRegistry != null) {
            for (PyType k : c.mro) {
                for (PyType r : cls.abcRegistry) {
                    if (k == r || r.subclassCheck(k)) { //NOPMD CompareObjectsWithEquals - identity is meant
                        return true;
                    }
                }
            }
        }
        for (PyType sub : cls.subclassList()) {
            if (sub.subclassCheck(c)) {
                return true;
            }
        }
        return false;
    }
}
