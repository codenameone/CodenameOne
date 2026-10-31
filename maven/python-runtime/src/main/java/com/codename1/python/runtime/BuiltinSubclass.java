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

/// Subclasses of the builtin value types: `class Stack(list)`,
/// `class Celsius(float)`, `class Counter(dict)`.
///
/// An instance of such a class is a [PyInstance] whose [PyInstance#base]
/// holds the builtin value it extends: a `PyList` for a list subclass, a
/// `Long` for an int subclass. A method of the builtin type called on it
/// runs on that value (see `PyBuiltin.method`), and the dunder methods
/// installed here give the builtin types the `__getitem__`, `__add__`, ...
/// that `super()` and the instance's operators resolve to. A dunder the
/// subclass defines itself comes first in its MRO, so it wins as in CPython.
final class BuiltinSubclass {
    private BuiltinSubclass() {
    }

    interface Binary {
        Object apply(PyObject self, Object other);
    }

    interface Unary {
        Object apply(PyObject self);
    }

    /// Whether instances of `t` can be extended by a class.
    static boolean subclassable(PyType t) {
        return t == Py.INT_TYPE || t == Py.FLOAT_TYPE || t == Py.STR_TYPE || t == Py.BYTES_TYPE //NOPMD CompareObjectsWithEquals - identity is meant
                || t == Py.LIST_TYPE || t == Py.TUPLE_TYPE || t == Py.DICT_TYPE || t == Py.SET_TYPE //NOPMD CompareObjectsWithEquals - identity is meant
                || t == Py.FROZENSET_TYPE || t == Py.PROPERTY_TYPE; //NOPMD CompareObjectsWithEquals - identity is meant
    }

    /// Whether `t` is mutable: its value is created empty with the instance
    /// and filled by `__init__`; an immutable one is built by `__new__`.
    static boolean mutable(PyType t) {
        return t == Py.LIST_TYPE || t == Py.DICT_TYPE || t == Py.SET_TYPE || t == Py.PROPERTY_TYPE; //NOPMD CompareObjectsWithEquals - identity is meant
    }

    private static void binary(PyType t, String name, Binary op) {
        t.define(name, PyBuiltin.method(t, name, a -> {
            if (a.length != 2) {
                throw Py.typeError("expected 1 argument, got " + (a.length - 1));
            }
            // The other operand may be a subclass instance too (two IntEnum
            // members compared): the built-in operation sees its value.
            Object other = a[1] instanceof PyInstance && ((PyInstance) a[1]).base != null
                    ? ((PyInstance) a[1]).base : a[1];
            Object r = op.apply(Py.obj(a[0]), other);
            return r == null ? Py.NOT_IMPLEMENTED : r;
        }));
    }

    private static void unary(PyType t, String name, Unary op) {
        t.define(name, PyBuiltin.method(t, name, a -> op.apply(Py.obj(a[0]))));
    }

    private static void arithmetic(PyType t) {
        binary(t, "__add__", PyObject::add);
        binary(t, "__radd__", PyObject::radd);
        binary(t, "__sub__", PyObject::sub);
        binary(t, "__rsub__", PyObject::rsub);
        binary(t, "__mul__", PyObject::mul);
        binary(t, "__rmul__", PyObject::rmul);
        binary(t, "__truediv__", PyObject::truediv);
        binary(t, "__rtruediv__", PyObject::rtruediv);
        binary(t, "__floordiv__", PyObject::floordiv);
        binary(t, "__rfloordiv__", PyObject::rfloordiv);
        binary(t, "__mod__", PyObject::mod);
        binary(t, "__rmod__", PyObject::rmod);
        binary(t, "__pow__", PyObject::pow);
        binary(t, "__rpow__", PyObject::rpow);
        unary(t, "__neg__", PyObject::neg);
        unary(t, "__pos__", PyObject::pos);
        unary(t, "__abs__", s -> Builtins.lookup("abs") instanceof PyObject
                ? ((PyObject) Builtins.lookup("abs")).call(new Object[] {s}, Py.NO_KW) : null);
        unary(t, "__float__", s -> Py.ofFloat(s.asDouble()));
        unary(t, "__bool__", s -> Py.bool(s.isTrue()));
    }

    private static void bits(PyType t) {
        binary(t, "__and__", PyObject::and);
        binary(t, "__rand__", PyObject::rand);
        binary(t, "__or__", PyObject::or);
        binary(t, "__ror__", PyObject::ror);
        binary(t, "__xor__", PyObject::xor);
        binary(t, "__rxor__", PyObject::rxor);
    }

    private static void compare(PyType t) {
        binary(t, "__eq__", PyObject::eq);
        binary(t, "__ne__", (s, o) -> {
            Object r = s.eq(o);
            return r == null || r == Py.NOT_IMPLEMENTED ? null : Py.bool(!Py.isTrue(r)); //NOPMD CompareObjectsWithEquals - identity is meant
        });
        binary(t, "__lt__", PyObject::lt);
        binary(t, "__le__", PyObject::le);
        binary(t, "__gt__", PyObject::gt);
        binary(t, "__ge__", PyObject::ge);
        unary(t, "__repr__", s -> Py.str(s.repr()));
        // The receiver reaches here already unwrapped to the built-in value,
        // so this formats the value, never back through a subclass.
        t.define("__format__", PyBuiltin.method(t, "__format__",
                a -> Py.str(Formatting.format(a[0], a.length > 1 ? Py.toStr(a[1]) : ""))));
        if (mutable(t)) {
            t.define("__hash__", Py.NONE);
        } else {
            unary(t, "__hash__", s -> PyInt.of(s.hash()));
        }
    }

    private static void container(PyType t) {
        unary(t, "__len__", s -> PyInt.of(s.len()));
        unary(t, "__iter__", PyObject::iter);
        binary(t, "__contains__", (s, x) -> Py.bool(s.contains(x)));
        binary(t, "__getitem__", PyObject::getItem);
        if (t == Py.LIST_TYPE || t == Py.DICT_TYPE || t == Py.BYTEARRAY_TYPE || t == Py.MEMORYVIEW_TYPE) { //NOPMD CompareObjectsWithEquals - identity is meant
            t.define("__setitem__", PyBuiltin.method(t, "__setitem__", a -> {
                Py.obj(a[0]).setItem(a[1], a[2]);
                return Py.NONE;
            }));
            t.define("__delitem__", PyBuiltin.method(t, "__delitem__", a -> {
                Py.obj(a[0]).delItem(a[1]);
                return Py.NONE;
            }));
        }
    }

    /// `T.__new__(cls, *args)` of an immutable builtin `T`.
    private static void newMethod(final PyType t) {
        t.define("__new__", new PyStaticMethod(new PyBuiltin("__new__", (args, kw) -> {
            if (args.length == 0 || !(args[0] instanceof PyType)) {
                throw Py.notATypeObject(t.name, args);
            }
            PyType cls = (PyType) args[0];
            if (!cls.isSubtype(t)) {
                throw Py.typeError(t.name + ".__new__(" + cls.name + "): " + cls.name + " is not a subtype of "
                        + t.name);
            }
            Object[] rest = new Object[args.length - 1];
            System.arraycopy(args, 1, rest, 0, rest.length);
            Object v = t.factory.create(t, rest, kw);
            if (cls == t) { //NOPMD CompareObjectsWithEquals - identity is meant
                return v;
            }
            PyInstance o = cls.newInstance();
            o.base = v;
            return o;
        })));
    }

    /// `T.__init__(self, *args)` of a mutable builtin `T`: refills the value.
    private static void initMethod(final PyType t) {
        t.define("__init__", PyBuiltin.methodKw(t, "__init__", (args, kw) -> {
            Object[] rest = new Object[args.length - 1];
            System.arraycopy(args, 1, rest, 0, rest.length);
            Object fresh = t.factory.create(t, rest, kw);
            Object self = args[0];
            if (self instanceof PyProperty && fresh instanceof PyProperty) {
                ((PyProperty) self).fill((PyProperty) fresh);
            } else if (self instanceof PyList) {
                PyList l = (PyList) self;
                l.clear();
                l.extend(fresh);
            } else if (self instanceof PyDict) {
                PyDict d = (PyDict) self;
                d.clear();
                d.update(fresh);
            } else if (self instanceof PySet) {
                PySet s = (PySet) self;
                s.clear();
                Object it = Py.iter(fresh);
                for (Object v = Py.next(it); v != null; v = Py.next(it)) {
                    s.addItem(v);
                }
            }
            return Py.NONE;
        }));
    }

    static void install() {
        // Not subclassable here, but their protocol is reachable by name.
        PyType[] buffers = {Py.BYTEARRAY_TYPE, Py.MEMORYVIEW_TYPE};
        for (PyType buffer : buffers) {
            container(buffer);
            binary(buffer, "__eq__", PyObject::eq);
            unary(buffer, "__repr__", x -> Py.str(x.repr()));
        }
        Py.BYTEARRAY_TYPE.define("__hash__", Py.NONE);
        binary(Py.BYTEARRAY_TYPE, "__add__", PyObject::add);
        initMethod(Py.PROPERTY_TYPE);
        PyType[] all = {Py.INT_TYPE, Py.FLOAT_TYPE, Py.STR_TYPE, Py.BYTES_TYPE, Py.LIST_TYPE, Py.TUPLE_TYPE,
            Py.DICT_TYPE, Py.SET_TYPE, Py.FROZENSET_TYPE};
        for (PyType e : all) {
            PyType t = e;
            compare(t);
            if (mutable(t)) {
                initMethod(t);
            } else {
                newMethod(t);
            }
            if (t == Py.INT_TYPE || t == Py.FLOAT_TYPE) { //NOPMD CompareObjectsWithEquals - identity is meant
                arithmetic(t);
                if (t == Py.INT_TYPE) { //NOPMD CompareObjectsWithEquals - identity is meant
                    bits(t);
                    binary(t, "__lshift__", PyObject::lshift);
                    binary(t, "__rshift__", PyObject::rshift);
                    unary(t, "__int__", s -> PyInt.of(s.asLong()));
                    unary(t, "__index__", s -> PyInt.of(s.asLong()));
                    unary(t, "__invert__", PyObject::invert);
                }
            } else {
                container(t);
                if (t == Py.SET_TYPE || t == Py.FROZENSET_TYPE) { //NOPMD CompareObjectsWithEquals - identity is meant
                    bits(t);
                    binary(t, "__sub__", PyObject::sub);
                    binary(t, "__rsub__", PyObject::rsub);
                } else if (t == Py.DICT_TYPE) { //NOPMD CompareObjectsWithEquals - identity is meant
                    binary(t, "__or__", PyObject::or);
                    binary(t, "__ror__", PyObject::ror);
                } else {
                    binary(t, "__add__", PyObject::add);
                    binary(t, "__mul__", PyObject::mul);
                    binary(t, "__rmul__", PyObject::rmul);
                    if (t == Py.STR_TYPE || t == Py.BYTES_TYPE) { //NOPMD CompareObjectsWithEquals - identity is meant
                        binary(t, "__mod__", PyObject::mod);
                    }
                }
                if (t == Py.LIST_TYPE) { //NOPMD CompareObjectsWithEquals - identity is meant
                    binary(t, "__iadd__", (s, o) -> {
                        ((PyList) s).extend(o);
                        return s;
                    });
                }
            }
            if (t == Py.STR_TYPE) { //NOPMD CompareObjectsWithEquals - identity is meant
                unary(t, "__str__", s -> Py.str(s.str()));
            }
        }
    }
}
