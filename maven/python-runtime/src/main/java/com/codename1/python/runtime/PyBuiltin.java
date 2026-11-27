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
    /// Set on a builtin subclass's own `__bool__` (see BuiltinSubclass):
    /// the instance's truth is its base value's.
    boolean baseTruth;
    private One one;
    private Two two;
    /// A two-argument path that takes the receiver still wrapped, asked
    /// only where [#two] is absent: see [#withSelf2].
    private Two selfTwo;
    private Three three;
    private Four four;
    private Five five;
    private Six six;
    private Seven seven;
    /// The builtin type a [#method] belongs to; null for a function.
    private final PyType owner;
    /// `__qualname__` where it is not the name: a method made for a class in Python.
    private String qual;

    /// Names the class this is a method of, for `__qualname__`.
    PyBuiltin qual(String qualifiedName) {
        qual = qualifiedName;
        return this;
    }

    public PyBuiltin(String name, Impl impl) {
        this(name, impl, null);
    }

    private PyBuiltin(String name, Impl impl, PyType owner) {
        this.name = name;
        this.impl = impl;
        this.owner = owner;
    }

    /// A builtin that rejects keyword arguments, as most C functions do.
    /// Adds a two-argument path to a builtin whose general form takes an array, so
    /// `f(a, b)` reaches it without packing the arguments.
    PyBuiltin withTwo(Two f) {
        two = f;
        return this;
    }

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

    /// [#positional] for an implementation that reads its argument array and
    /// keeps nothing of it once it returns (its elements, yes; the array, no).
    /// Its usual call, of `n` arguments, is then lent an array ([ArgArrays])
    /// rather than handed a new one per call. Any other count goes through
    /// the array form unchanged. Not for a method of a builtin type.
    static PyBuiltin lending(final String name, final int n, final Positional p) {
        PyBuiltin b = positional(name, p);
        switch (n) {
            case 1:
                b.one = x0 -> lend(p, ArgArrays.take(1), x0, null, null, null, null, null, null);
                break;
            case 2:
                b.two = (x0, x1) -> lend(p, ArgArrays.take(2), x0, x1, null, null, null, null, null);
                break;
            case 3:
                b.three = (x0, x1, x2) -> lend(p, ArgArrays.take(3), x0, x1, x2, null, null, null, null);
                break;
            case 4:
                b.four = (x0, x1, x2, x3) -> lend(p, ArgArrays.take(4), x0, x1, x2, x3, null, null, null);
                break;
            case 5:
                b.five = (x0, x1, x2, x3, x4) -> lend(p, ArgArrays.take(5), x0, x1, x2, x3, x4, null, null);
                break;
            case 6:
                b.six = (x0, x1, x2, x3, x4, x5) -> lend(p, ArgArrays.take(6), x0, x1, x2, x3, x4, x5, null);
                break;
            case 7:
                b.seven = (x0, x1, x2, x3, x4, x5, x6) -> lend(p, ArgArrays.take(7), x0, x1, x2, x3, x4, x5, x6);
                break;
            default:
                throw new IllegalArgumentException("lending() covers one to seven arguments: " + n);
        }
        return b;
    }

    private static Object lend(Positional p, Object[] a, Object x0, Object x1, Object x2, Object x3, Object x4,
            Object x5, Object x6) {
        int n = a.length;
        a[0] = x0;
        if (n > 1) {
            a[1] = x1;
        }
        if (n > 2) {
            a[2] = x2;
        }
        if (n > 3) {
            a[3] = x3;
        }
        if (n > 4) {
            a[4] = x4;
        }
        if (n > 5) {
            a[5] = x5;
        }
        if (n > 6) {
            a[6] = x6;
        }
        Object r = p.call(a);
        ArgArrays.give(a);
        return r;
    }

    /// A builtin of exactly `n` arguments, whose fixed-arity call runs
    /// without an argument array.
    private static PyBuiltin fixed(final String name, final int n, final Positional p) {
        return positional(name, a -> {
            // Too few is an error; extra arguments are ignored, as the array
            // implementations these replaced ignored them (__exit__).
            if (a.length < n) {
                throw Py.typeError(name + " expected " + n + (n == 1 ? " argument" : " arguments") + ", got "
                        + a.length);
            }
            return p.call(a);
        });
    }

    /// [#fixed] for one argument.
    public static PyBuiltin fixed1(final String name, final One f) {
        PyBuiltin b = fixed(name, 1, a -> f.call(a[0]));
        b.one = f;
        return b;
    }

    /// [#fixed] for two arguments.
    public static PyBuiltin fixed2(final String name, final Two f) {
        PyBuiltin b = fixed(name, 2, a -> f.call(a[0], a[1]));
        b.two = f;
        return b;
    }

    /// [#fixed] for three arguments.
    public static PyBuiltin fixed3(final String name, final Three f) {
        PyBuiltin b = fixed(name, 3, a -> f.call(a[0], a[1], a[2]));
        b.three = f;
        return b;
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

    /// The array-free two-argument path of a [#methodKwSelf] builtin: `f`
    /// receives the receiver as it was passed, wrapped or not.
    PyBuiltin withSelf2(Two f) {
        selfTwo = f;
        return this;
    }

    /// [One] for four arguments.
    public interface Four {
        Object call(Object a, Object b, Object c, Object d);
    }

    /// [#with1] for four arguments.
    public PyBuiltin with4(Four f) {
        four = f;
        return this;
    }

    /// [One] for five arguments.
    public interface Five {
        Object call(Object a, Object b, Object c, Object d, Object e);
    }

    /// [#with1] for five arguments.
    public PyBuiltin with5(Five f) {
        five = f;
        return this;
    }

    /// [One] for six arguments.
    public interface Six {
        Object call(Object a, Object b, Object c, Object d, Object e, Object f);
    }

    /// [One] for seven arguments.
    public interface Seven {
        Object call(Object a, Object b, Object c, Object d, Object e, Object f, Object g);
    }

    /// [#with1] for six arguments; not for methods of a builtin type.
    public PyBuiltin with6(Six f) {
        six = f;
        return this;
    }

    /// [#with1] for seven arguments; not for methods of a builtin type.
    public PyBuiltin with7(Seven f) {
        seven = f;
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
            if (selfTwo != null) {
                receiver(a);
                return selfTwo.call(a, b);
            }
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

    @Override
    public Object call4(Object a, Object b, Object c, Object d) {
        if (four == null) {
            return super.call4(a, b, c, d);
        }
        if (owner == null) {
            return four.call(a, b, c, d);
        }
        Object s = receiver(a);
        return result(a, s, four.call(s, b, c, d));
    }

    @Override
    public Object call5(Object a, Object b, Object c, Object d, Object e) {
        if (five == null) {
            return super.call5(a, b, c, d, e);
        }
        if (owner == null) {
            return five.call(a, b, c, d, e);
        }
        Object s = receiver(a);
        return result(a, s, five.call(s, b, c, d, e));
    }

    @Override
    public Object call6(Object a, Object b, Object c, Object d, Object e, Object f) {
        if (six == null || owner != null) {
            return super.call6(a, b, c, d, e, f);
        }
        return six.call(a, b, c, d, e, f);
    }

    @Override
    public Object call7(Object a, Object b, Object c, Object d, Object e, Object f, Object g) {
        if (seven == null || owner != null) {
            return super.call7(a, b, c, d, e, f, g);
        }
        return seven.call(a, b, c, d, e, f, g);
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

    /// A method of builtin type `owner` taking the receiver and `n - 1`
    /// arguments, whose fixed-arity call runs without an argument array.
    private static PyBuiltin methodN(final PyType owner, final String name, final int n, final Positional p) {
        return method(owner, name, a -> {
            if (a.length < n) {
                throw Py.typeError(name + "() takes " + (n == 1 ? "no arguments" : "exactly " + (n - 1)
                        + (n == 2 ? " argument" : " arguments")) + " (" + (a.length - 1) + " given)");
            }
            return p.call(a);
        });
    }

    /// [#methodN] for the receiver alone.
    public static PyBuiltin method1(final PyType owner, final String name, final One f) {
        return methodN(owner, name, 1, a -> f.call(a[0])).with1(f);
    }

    /// [#methodN] for the receiver and one argument.
    public static PyBuiltin method2(final PyType owner, final String name, final Two f) {
        return methodN(owner, name, 2, a -> f.call(a[0], a[1])).with2(f);
    }

    /// [#methodN] for the receiver and two arguments.
    public static PyBuiltin method3(final PyType owner, final String name, final Three f) {
        return methodN(owner, name, 3, a -> f.call(a[0], a[1], a[2])).with3(f);
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

    /// [#methodKw] for an implementation that unwraps its receiver itself
    /// with [#base]: no copy of the arguments per call.
    static PyBuiltin methodKwSelf(final PyType owner, final String name, final Impl p) {
        return new PyBuiltin(name, new Impl() {
            @Override
            public Object call(Object[] args, String[] kwNames) {
                checkReceiver(owner, name, args);
                return p.call(args, kwNames);
            }
        }, owner);
    }

    /// The builtin value an instance of a subclass of builtin `owner`
    /// extends, or `self` itself.
    static Object base(PyType owner, Object self) {
        if (owner != Py.OBJECT_TYPE && self instanceof PyInstance) { //NOPMD CompareObjectsWithEquals - identity is meant
            Object b = ((PyInstance) self).base;
            if (b != null) {
                return b;
            }
        }
        return self;
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
                Object[] a = args.clone();
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

    /// Adds fixed-arity calls of one and two arguments to a builtin whose
    /// general form parses an argument list: the usual call then builds no
    /// array and no parser. Either may be null.
    PyBuiltin withFixed(One f1, Two f2) {
        one = f1;
        two = f2;
        return this;
    }

    /// For a method whose implementation keeps nothing of its argument
    /// array once it returns: the two-argument call (`list.__init__(self,
    /// it)` from a subclass constructor) is lent an array ([ArgArrays]).
    PyBuiltin lendingTwo() {
        final Impl p = impl;
        two = (x0, x1) -> {
            Object[] a = ArgArrays.take(2);
            a[0] = x0;
            a[1] = x1;
            Object r = p.call(a, Py.NO_KW);
            ArgArrays.give(a);
            return r;
        };
        return this;
    }

    @Override
    public Object bind(Object self) {
        if ("__new__".equals(name)) {
            // Every __new__ is a static method: `self.__new__(cls)` reads
            // the function as it is, with nothing bound.
            return this;
        }
        return new PyMethod(self, this);
    }

    @Override
    public Object call(Object[] args, String[] kwNames) {
        return impl.call(args, kwNames);
    }

    @Override
    public Object getAttrOrNull(String attr) {
        if ("__qualname__".equals(attr) && qual != null) {
            return qual;
        }
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
