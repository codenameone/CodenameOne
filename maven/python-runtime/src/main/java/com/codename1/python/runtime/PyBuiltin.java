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

    /// A fixed-arity implementation. A call with exactly that many
    /// positional arguments runs it without building an argument array;
    /// any other call goes through [Impl], which reports the arity error.
    public interface One {
        Object call(Object a);
    }

    /// [One] for two arguments.
    public interface Two {
        Object call(Object a, Object b);
    }

    /// [One] for three arguments.
    public interface Three {
        Object call(Object a, Object b, Object c);
    }

    public final String name;
    final Impl impl;
    private One one;
    private Two two;
    private Three three;
    /// The builtin type a [#method] belongs to; null for a function.
    private final PyType owner;

    public PyBuiltin(String name, Impl impl) {
        this(name, impl, null);
    }

    private PyBuiltin(String name, Impl impl, PyType owner) {
        this.name = name;
        this.impl = impl;
        this.owner = owner;
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

    /// A builtin of exactly one argument: `len`, `ord`, `repr`.
    public static PyBuiltin unary(final String name, final One f) {
        PyBuiltin b = positional(name, a -> {
            if (a.length != 1) {
                throw Py.typeError(name + "() takes exactly one argument (" + a.length + " given)");
            }
            return f.call(a[0]);
        });
        b.one = f;
        return b;
    }

    /// Adds the array-free path for a call with one positional argument;
    /// `f` must answer what [Impl] answers for that call.
    public PyBuiltin with1(One f) {
        one = f;
        return this;
    }

    /// [#with1] for two arguments.
    public PyBuiltin with2(Two f) {
        two = f;
        return this;
    }

    /// [#with1] for three arguments.
    public PyBuiltin with3(Three f) {
        three = f;
        return this;
    }

    @Override
    public Object call1(Object a) {
        if (one == null) {
            return super.call1(a);
        }
        if (owner == null) {
            return one.call(a);
        }
        Object s = receiver(a);
        return result(a, s, one.call(s));
    }

    @Override
    public Object call2(Object a, Object b) {
        if (two == null) {
            return super.call2(a, b);
        }
        if (owner == null) {
            return two.call(a, b);
        }
        Object s = receiver(a);
        return result(a, s, two.call(s, b));
    }

    @Override
    public Object call3(Object a, Object b, Object c) {
        if (three == null) {
            return super.call3(a, b, c);
        }
        if (owner == null) {
            return three.call(a, b, c);
        }
        Object s = receiver(a);
        return result(a, s, three.call(s, b, c));
    }

    /// [#checkReceiver] and [#unwrap] for the receiver of a fixed-arity
    /// method call: the value the implementation works on.
    private Object receiver(Object self) {
        if (!Py.typeOf(self).isSubtype(owner)) {
            throw Py.typeError("descriptor '" + name + "' for '" + owner.name + "' objects doesn't apply to a '"
                    + Py.typeOf(self).name + "' object");
        }
        if (owner != Py.OBJECT_TYPE && self instanceof PyInstance) { //NOPMD CompareObjectsWithEquals - identity is meant
            Object b = ((PyInstance) self).base;
            if (b != null) {
                return b;
            }
        }
        return self;
    }

    /// As in [#method]: a method returning the unwrapped value it was
    /// given hands back the instance instead.
    private Object result(Object self, Object s, Object r) {
        return s != self && r == s && BuiltinSubclass.mutable(owner) ? self : r; //NOPMD CompareObjectsWithEquals - identity is meant
    }

    /// A method of builtin type `owner`. The receiver is checked before the
    /// implementation runs, so it may cast `args[0]` to the owner's class:
    /// `bytes.hex(5)` is a TypeError here rather than an unchecked cast.
    public static PyBuiltin method(final PyType owner, final String name, final Positional p) {
        PyBuiltin b = new PyBuiltin(name, new Impl() {
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
        }, owner);
        return b;
    }

    /// [#method] for an implementation that takes keyword arguments.
    public static PyBuiltin methodKw(final PyType owner, final String name, final Impl p) {
        PyBuiltin b = new PyBuiltin(name, new Impl() {
            @Override
            public Object call(Object[] args, String[] kwNames) {
                checkReceiver(owner, name, args);
                Object[] a = unwrap(owner, args);
                Object r = p.call(a, kwNames);
                return a != args && r == a[0] && BuiltinSubclass.mutable(owner) ? args[0] : r; //NOPMD CompareObjectsWithEquals - identity is meant
            }
        }, owner);
        return b;
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
