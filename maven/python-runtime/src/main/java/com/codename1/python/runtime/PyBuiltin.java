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

/// A function or method implemented in Java: the builtins, and the methods of
/// the builtin types. As a class attribute it binds like a Python function.
public final class PyBuiltin extends PyObject {

    /// The implementation; receives the arguments as passed.
    public interface Impl {
        Object call(Object[] args, String[] kwNames);
    }

    /// An implementation that takes positional arguments only.
    public interface Positional {
        Object call(Object[] args);
    }

    public final String name;
    final Impl impl;

    public PyBuiltin(String name, Impl impl) {
        this.name = name;
        this.impl = impl;
    }

    /// A builtin that rejects keyword arguments, as most C functions do.
    public static PyBuiltin positional(final String name, final Positional p) {
        return new PyBuiltin(name, new Impl() {
            @Override
            public Object call(Object[] args, String[] kwNames) {
                if (kwNames.length != 0) {
                    throw Py.typeError(name + "() takes no keyword arguments");
                }
                return p.call(args);
            }
        });
    }

    /// A method of builtin type `owner`. The receiver is checked before the
    /// implementation runs, so it may cast `args[0]` to the owner's class:
    /// `bytes.hex(5)` is a TypeError here rather than an unchecked cast.
    public static PyBuiltin method(final PyType owner, final String name, final Positional p) {
        return new PyBuiltin(name, new Impl() {
            @Override
            public Object call(Object[] args, String[] kwNames) {
                checkReceiver(owner, name, args);
                if (kwNames.length != 0) {
                    throw Py.typeError(name + "() takes no keyword arguments");
                }
                Object[] a = unwrap(owner, args);
                Object r = p.call(a);
                return a != args && r == a[0] && BuiltinSubclass.mutable(owner) ? args[0] : r; //NOPMD CompareObjectsWithEquals - identity is meant
            }
        });
    }

    /// [#method] for an implementation that takes keyword arguments.
    public static PyBuiltin methodKw(final PyType owner, final String name, final Impl p) {
        return new PyBuiltin(name, new Impl() {
            @Override
            public Object call(Object[] args, String[] kwNames) {
                checkReceiver(owner, name, args);
                Object[] a = unwrap(owner, args);
                Object r = p.call(a, kwNames);
                return a != args && r == a[0] && BuiltinSubclass.mutable(owner) ? args[0] : r; //NOPMD CompareObjectsWithEquals - identity is meant
            }
        });
    }

    /// The arguments with an instance of a subclass of builtin `owner`
    /// replaced by the builtin value it extends, which is what the
    /// implementation works on. A method returning that value (a chained
    /// `__iadd__` of a list) gives the instance back.
    static Object[] unwrap(PyType owner, Object[] args) {
        Object self = args[0];
        if (owner != Py.OBJECT_TYPE && self instanceof PyInstance) { //NOPMD CompareObjectsWithEquals - identity is meant
            Object b = ((PyInstance) self).base;
            if (b != null) {
                Object[] a = new Object[args.length];
                System.arraycopy(args, 0, a, 0, args.length);
                a[0] = b;
                return a;
            }
        }
        return args;
    }

    static void checkReceiver(PyType owner, String name, Object[] args) {
        if (args.length == 0) {
            throw Py.typeError("unbound method " + owner.name + "." + name + "() needs an argument");
        }
        if (!Py.typeOf(args[0]).isSubtype(owner)) {
            throw Py.typeError("descriptor '" + name + "' for '" + owner.name + "' objects doesn't apply to a '"
                    + Py.typeOf(args[0]).name + "' object");
        }
    }

    @Override
    public PyType type() {
        return Py.BUILTIN_TYPE;
    }

    @Override
    public Object bind(Object self) {
        return new PyMethod(self, this);
    }

    @Override
    public Object call(Object[] args, String[] kwNames) {
        return impl.call(args, kwNames);
    }

    @Override
    public Object getAttrOrNull(String attr) {
        if ("__name__".equals(attr) || "__qualname__".equals(attr)) {
            return name;
        }
        if ("__module__".equals(attr)) {
            // A builtins function reports its module; any other built-in
            // (a module's function, a bound helper) reports None here.
            return Builtins.NAMES.get(String.valueOf(name)) == this ? Py.str("builtins") : Py.NONE; //NOPMD CompareObjectsWithEquals - identity is meant
        }
        return super.getAttrOrNull(attr);
    }

    @Override
    public String repr() {
        return "<built-in function " + name + ">";
    }
}
