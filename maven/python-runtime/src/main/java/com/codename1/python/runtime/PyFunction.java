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
import java.util.List;

/// A function defined in Python source.
///
/// The transpiler emits the body as a [Body] lambda that receives its
/// parameters already bound into a flat array, in declaration order:
/// positional parameters, then `*args` (a tuple), keyword-only parameters,
/// then `**kwargs` (a dict). Binding follows CPython's rules and messages.
public final class PyFunction extends PyObject {

    /// The compiled body of a function.
    public interface Body {
        Object run(Object[] a);
    }

    // A function with only positional parameters (at most six, defaults
    // allowed) is also compiled to take them directly: the transpiler picks
    // the matching constructor, and a call with exactly that many positional
    // arguments then allocates no argument array.

    public interface Body0 {
        Object run();
    }

    public interface Body1 {
        Object run(Object a);
    }

    public interface Body2 {
        Object run(Object a, Object b);
    }

    public interface Body3 {
        Object run(Object a, Object b, Object c);
    }

    public interface Body4 {
        Object run(Object a, Object b, Object c, Object d);
    }

    public interface Body5 {
        Object run(Object a, Object b, Object c, Object d, Object e);
    }

    public interface Body6 {
        Object run(Object a, Object b, Object c, Object d, Object e, Object f);
    }

    public interface Body7 {
        Object run(Object a, Object b, Object c, Object d, Object e, Object f, Object g);
    }

    public interface Body8 {
        Object run(Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h);
    }

    public interface Body9 {
        Object run(Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i);
    }

    public interface Body10 {
        Object run(Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j);
    }

    public interface Body11 {
        Object run(Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k);
    }

    public interface Body12 {
        Object run(Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l);
    }

    public interface Body13 {
        Object run(Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m);
    }

    public interface Body14 {
        Object run(Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n);
    }

    /// A function whose body is one float expression of its parameters
    /// also has its value as a raw double: the arithmetic alone, for the
    /// parameter types the expression implies. It answers NaN for "not
    /// computed here" (other argument types, an int overflow, a zero
    /// divisor, or a NaN result), and the caller then makes the ordinary
    /// call. It reads nothing but its arguments and has no side effects, so
    /// trying it first is never observable.
    public interface PureD1 {
        double run(Object a);
    }

    public interface PureD2 {
        double run(Object a, Object b);
    }

    public interface PureD3 {
        double run(Object a, Object b, Object c);
    }

    public interface PureD4 {
        double run(Object a, Object b, Object c, Object d);
    }

    /// The same twin for int arguments a caller holds unboxed (a `range`
    /// loop index), so no argument is boxed to make the call. Same NaN
    /// contract.
    public interface PureL1 {
        double run(long a);
    }

    public interface PureL2 {
        double run(long a, long b);
    }

    /// [#bindDirect]'s "not this way"; never a Python value.
    private static final Object UNBOUND = new Object();

    public final String name;
    private String qualname;
    final String[] params;
    final int positional;
    final boolean varArgs;
    final int kwOnly;
    final boolean varKw;
    final Object[] defaults;
    final Object[] kwDefaults;
    final Body body;
    private Body0 body0;
    Body1 body1;
    Body2 body2;
    private Body3 body3;
    private Body4 body4;
    private Body5 body5;
    private Body6 body6;
    private Body7 body7;
    private Body8 body8;
    private Body9 body9;
    private Body10 body10;
    private Body11 body11;
    private Body12 body12;
    private Body13 body13;
    private Body14 body14;
    private PureD1 pure1;
    private PureD2 pure2;
    private PureD3 pure3;
    private PureD4 pure4;
    private PureL1 pureL1;
    private PureL2 pureL2;
    /// The `__dict__` of the function object, created on first write.
    PyDict attrs;

    /// @param params every parameter name in binding order
    /// @param positional how many leading names are positional (or positional-or-keyword)
    /// @param varArgs whether `*args` follows the positional names
    /// @param kwOnly how many keyword-only names follow
    /// @param varKw whether `**kwargs` is last
    /// @param defaults defaults for the LAST `defaults.length` positional parameters
    /// @param kwDefaults defaults for keyword-only parameters, `null` entries where required
    public PyFunction(String name, String[] params, int positional, boolean varArgs, int kwOnly,
            boolean varKw, Object[] defaults, Object[] kwDefaults, Body body) {
        this.name = name;
        this.params = params;
        this.positional = positional;
        this.varArgs = varArgs;
        this.kwOnly = kwOnly;
        this.varKw = varKw;
        this.defaults = defaults;
        this.kwDefaults = kwDefaults;
        this.body = body;
    }

    public PyFunction(String name, String[] params, int positional, boolean varArgs, int kwOnly,
            boolean varKw, Object[] defaults, Object[] kwDefaults, Body0 b) {
        this(name, params, positional, varArgs, kwOnly, varKw, defaults, kwDefaults, (Object[] a) -> b.run());
        body0 = b;
    }

    public PyFunction(String name, String[] params, int positional, boolean varArgs, int kwOnly,
            boolean varKw, Object[] defaults, Object[] kwDefaults, Body1 b) {
        this(name, params, positional, varArgs, kwOnly, varKw, defaults, kwDefaults, (Object[] a) -> b.run(a[0]));
        body1 = b;
    }

    public PyFunction(String name, String[] params, int positional, boolean varArgs, int kwOnly,
            boolean varKw, Object[] defaults, Object[] kwDefaults, Body2 b) {
        this(name, params, positional, varArgs, kwOnly, varKw, defaults, kwDefaults, (Object[] a) -> b.run(a[0], a[1]));
        body2 = b;
    }

    public PyFunction(String name, String[] params, int positional, boolean varArgs, int kwOnly,
            boolean varKw, Object[] defaults, Object[] kwDefaults, Body3 b) {
        this(name, params, positional, varArgs, kwOnly, varKw, defaults, kwDefaults,
                (Object[] a) -> b.run(a[0], a[1], a[2]));
        body3 = b;
    }

    public PyFunction(String name, String[] params, int positional, boolean varArgs, int kwOnly,
            boolean varKw, Object[] defaults, Object[] kwDefaults, Body4 b) {
        this(name, params, positional, varArgs, kwOnly, varKw, defaults, kwDefaults,
                (Object[] a) -> b.run(a[0], a[1], a[2], a[3]));
        body4 = b;
    }

    public PyFunction(String name, String[] params, int positional, boolean varArgs, int kwOnly,
            boolean varKw, Object[] defaults, Object[] kwDefaults, Body5 b) {
        this(name, params, positional, varArgs, kwOnly, varKw, defaults, kwDefaults,
                (Object[] a) -> b.run(a[0], a[1], a[2], a[3], a[4]));
        body5 = b;
    }

    public PyFunction(String name, String[] params, int positional, boolean varArgs, int kwOnly,
            boolean varKw, Object[] defaults, Object[] kwDefaults, Body6 b) {
        this(name, params, positional, varArgs, kwOnly, varKw, defaults, kwDefaults,
                (Object[] a) -> b.run(a[0], a[1], a[2], a[3], a[4], a[5]));
        body6 = b;
    }

    public PyFunction(String name, String[] params, int positional, boolean varArgs, int kwOnly,
            boolean varKw, Object[] defaults, Object[] kwDefaults, Body7 b) {
        this(name, params, positional, varArgs, kwOnly, varKw, defaults, kwDefaults,
                (Object[] a) -> b.run(a[0], a[1], a[2], a[3], a[4], a[5], a[6]));
        body7 = b;
    }

    public PyFunction(String name, String[] params, int positional, boolean varArgs, int kwOnly,
            boolean varKw, Object[] defaults, Object[] kwDefaults, Body8 b) {
        this(name, params, positional, varArgs, kwOnly, varKw, defaults, kwDefaults,
                (Object[] a) -> b.run(a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7]));
        body8 = b;
    }

    public PyFunction(String name, String[] params, int positional, boolean varArgs, int kwOnly,
            boolean varKw, Object[] defaults, Object[] kwDefaults, Body9 b) {
        this(name, params, positional, varArgs, kwOnly, varKw, defaults, kwDefaults,
                (Object[] a) -> b.run(a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8]));
        body9 = b;
    }

    public PyFunction(String name, String[] params, int positional, boolean varArgs, int kwOnly,
            boolean varKw, Object[] defaults, Object[] kwDefaults, Body10 b) {
        this(name, params, positional, varArgs, kwOnly, varKw, defaults, kwDefaults,
                (Object[] a) -> b.run(a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8], a[9]));
        body10 = b;
    }

    public PyFunction(String name, String[] params, int positional, boolean varArgs, int kwOnly,
            boolean varKw, Object[] defaults, Object[] kwDefaults, Body11 b) {
        this(name, params, positional, varArgs, kwOnly, varKw, defaults, kwDefaults,
                (Object[] a) -> b.run(a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8], a[9], a[10]));
        body11 = b;
    }

    public PyFunction(String name, String[] params, int positional, boolean varArgs, int kwOnly,
            boolean varKw, Object[] defaults, Object[] kwDefaults, Body12 b) {
        this(name, params, positional, varArgs, kwOnly, varKw, defaults, kwDefaults,
                (Object[] a) -> b.run(a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8], a[9], a[10], a[11]));
        body12 = b;
    }

    public PyFunction(String name, String[] params, int positional, boolean varArgs, int kwOnly,
            boolean varKw, Object[] defaults, Object[] kwDefaults, Body13 b) {
        this(name, params, positional, varArgs, kwOnly, varKw, defaults, kwDefaults,
                (Object[] a) -> b.run(a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8], a[9], a[10], a[11], a[12]));
        body13 = b;
    }

    public PyFunction(String name, String[] params, int positional, boolean varArgs, int kwOnly,
            boolean varKw, Object[] defaults, Object[] kwDefaults, Body14 b) {
        this(name, params, positional, varArgs, kwOnly, varKw, defaults, kwDefaults,
                (Object[] a) -> b.run(a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8], a[9], a[10], a[11], a[12], a[13]));
        body14 = b;
    }

    @Override
    public Object call0() {
        if (body0 != null) {
            return body0.run();
        }
        return widen(0) ? fill(0, null, null, null, null, null) : call(Py.NO_ARGS, Py.NO_KW);
    }

    @Override
    public Object call1(Object a) {
        if (body1 != null) {
            return body1.run(a);
        }
        return widen(1) ? fill(1, a, null, null, null, null)
                : starOnly(1) ? star(1, a, null, null, null, null) : call(new Object[] {a}, Py.NO_KW);
    }

    @Override
    public Object call2(Object a, Object b) {
        if (body2 != null) {
            return body2.run(a, b);
        }
        return widen(2) ? fill(2, a, b, null, null, null)
                : starOnly(2) ? star(2, a, b, null, null, null) : call(new Object[] {a, b}, Py.NO_KW);
    }

    @Override
    public Object call3(Object a, Object b, Object c) {
        if (body3 != null) {
            return body3.run(a, b, c);
        }
        return widen(3) ? fill(3, a, b, c, null, null)
                : starOnly(3) ? star(3, a, b, c, null, null) : call(new Object[] {a, b, c}, Py.NO_KW);
    }

    @Override
    public Object call4(Object a, Object b, Object c, Object d) {
        if (body4 != null) {
            return body4.run(a, b, c, d);
        }
        return widen(4) ? fill(4, a, b, c, d, null)
                : starOnly(4) ? star(4, a, b, c, d, null) : call(new Object[] {a, b, c, d}, Py.NO_KW);
    }

    @Override
    public Object call5(Object a, Object b, Object c, Object d, Object e) {
        if (body5 != null) {
            return body5.run(a, b, c, d, e);
        }
        return widen(5) ? fill(5, a, b, c, d, e)
                : starOnly(5) ? star(5, a, b, c, d, e) : call(new Object[] {a, b, c, d, e}, Py.NO_KW);
    }

    @Override
    public Object call6(Object a, Object b, Object c, Object d, Object e, Object f) {
        return body6 != null ? body6.run(a, b, c, d, e, f) : call(new Object[] {a, b, c, d, e, f}, Py.NO_KW);
    }

    @Override
    public Object call7(Object a, Object b, Object c, Object d, Object e, Object f, Object g) {
        return body7 != null ? body7.run(a, b, c, d, e, f, g) : call(new Object[] {a, b, c, d, e, f, g}, Py.NO_KW);
    }

    @Override
    public Object call8(Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h) {
        return body8 != null ? body8.run(a, b, c, d, e, f, g, h) : call(new Object[] {a, b, c, d, e, f, g, h}, Py.NO_KW);
    }

    @Override
    public Object call9(Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i) {
        return body9 != null ? body9.run(a, b, c, d, e, f, g, h, i) : call(new Object[] {a, b, c, d, e, f, g, h, i}, Py.NO_KW);
    }

    @Override
    public Object call10(Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j) {
        return body10 != null ? body10.run(a, b, c, d, e, f, g, h, i, j) : call(new Object[] {a, b, c, d, e, f, g, h, i, j}, Py.NO_KW);
    }

    @Override
    public Object call11(Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k) {
        return body11 != null ? body11.run(a, b, c, d, e, f, g, h, i, j, k) : call(new Object[] {a, b, c, d, e, f, g, h, i, j, k}, Py.NO_KW);
    }

    @Override
    public Object call12(Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l) {
        return body12 != null ? body12.run(a, b, c, d, e, f, g, h, i, j, k, l) : call(new Object[] {a, b, c, d, e, f, g, h, i, j, k, l}, Py.NO_KW);
    }

    @Override
    public Object call13(Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m) {
        return body13 != null ? body13.run(a, b, c, d, e, f, g, h, i, j, k, l, m) : call(new Object[] {a, b, c, d, e, f, g, h, i, j, k, l, m}, Py.NO_KW);
    }

    @Override
    public Object call14(Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n) {
        return body14 != null ? body14.run(a, b, c, d, e, f, g, h, i, j, k, l, m, n) : call(new Object[] {a, b, c, d, e, f, g, h, i, j, k, l, m, n}, Py.NO_KW);
    }

    /// Whether a positional call with `n` arguments binds as `f(*args)` does:
    /// plain leading parameters and one `*args`, nothing keyword-only.
    private boolean starOnly(int n) {
        return varArgs && kwOnly == 0 && !varKw && n >= positional;
    }

    /// [#call] of `f(*args)` from a fixed-arity call: the leading arguments go
    /// to their slots and the rest straight into the tuple, with no argument
    /// array built first and copied out of.
    private Object star(int n, Object a, Object b, Object c, Object d, Object e) {
        Object[] slots = spareSlots;
        spareSlots = null;
        if (slots == null) {
            slots = new Object[positional + 1];
        }
        int rest = n - positional;
        Object[] items = rest == 0 ? null : new Object[rest];
        for (int i = 0; i < n; i++) {
            Object v = i == 0 ? a : i == 1 ? b : i == 2 ? c : i == 3 ? d : e;
            if (i < positional) {
                slots[i] = v;
            } else {
                items[i - positional] = v;
            }
        }
        slots[positional] = items == null ? PyTuple.EMPTY : new PyTuple(items);
        Object r = body.run(slots);
        spareSlots = clear(slots);
        return r;
    }

    /// Whether a positional call with `n` arguments can go straight to the
    /// fixed-arity body by filling the rest from the defaults: a function of
    /// at most six plain parameters, every one past `n` defaulted. Most
    /// library functions are that shape (`match(self, string, pos=0)`), and
    /// binding them through `call` built a second argument array per call.
    private boolean widen(int n) {
        return n < positional && positional <= 6 && !varArgs && kwOnly == 0 && !varKw
                && positional - defaults.length <= n;
    }

    /// Parameter `i` of a call that passed `n` arguments: `v` when it was
    /// passed, its default otherwise.
    private Object pick(int i, int n, Object v) {
        return i < n ? v : defaults[i - (positional - defaults.length)];
    }

    /// Runs the fixed-arity body with the first `n` of `a`..`e` and the
    /// defaults after them; `n` was checked by `widen`. A function built
    /// without a body of its own arity binds them through `call` as before.
    private Object fill(int n, Object a, Object b, Object c, Object d, Object e) {
        switch (positional) {
            case 1:
                if (body1 != null) {
                    return body1.run(pick(0, n, a));
                }
                break;
            case 2:
                if (body2 != null) {
                    return body2.run(pick(0, n, a), pick(1, n, b));
                }
                break;
            case 3:
                if (body3 != null) {
                    return body3.run(pick(0, n, a), pick(1, n, b), pick(2, n, c));
                }
                break;
            case 4:
                if (body4 != null) {
                    return body4.run(pick(0, n, a), pick(1, n, b), pick(2, n, c), pick(3, n, d));
                }
                break;
            case 5:
                if (body5 != null) {
                    return body5.run(pick(0, n, a), pick(1, n, b), pick(2, n, c), pick(3, n, d), pick(4, n, e));
                }
                break;
            default:
                if (body6 != null) {
                    return body6.run(pick(0, n, a), pick(1, n, b), pick(2, n, c), pick(3, n, d), pick(4, n, e),
                            pick(5, n, null));
                }
                break;
        }
        Object[] args = new Object[n];
        Object[] given = {a, b, c, d, e};
        System.arraycopy(given, 0, args, 0, n);
        return call(args, Py.NO_KW);
    }

    public PyFunction pure(PureD1 p) {
        pure1 = p;
        return this;
    }

    public PyFunction pure(PureD2 p) {
        pure2 = p;
        return this;
    }

    public PyFunction pure(PureD3 p) {
        pure3 = p;
        return this;
    }

    public PyFunction pure(PureD4 p) {
        pure4 = p;
        return this;
    }

    public PyFunction pure(PureL1 p) {
        pureL1 = p;
        return this;
    }

    public PyFunction pure(PureL2 p) {
        pureL2 = p;
        return this;
    }

    double callL1(long a) {
        if (pureL1 != null) {
            return pureL1.run(a);
        }
        return pure1 != null ? pure1.run(Long.valueOf(a)) : Double.NaN;
    }

    double callL2(long a, long b) {
        if (pureL2 != null) {
            return pureL2.run(a, b);
        }
        return pure2 != null ? pure2.run(Long.valueOf(a), Long.valueOf(b)) : Double.NaN;
    }

    double callD1(Object a) {
        return pure1 != null ? pure1.run(a) : Double.NaN;
    }

    double callD2(Object a, Object b) {
        return pure2 != null ? pure2.run(a, b) : Double.NaN;
    }

    double callD3(Object a, Object b, Object c) {
        return pure3 != null ? pure3.run(a, b, c) : Double.NaN;
    }

    double callD4(Object a, Object b, Object c, Object d) {
        return pure4 != null ? pure4.run(a, b, c, d) : Double.NaN;
    }

    /// The common shape: positional parameters only, no defaults.
    public PyFunction(String name, String[] params, Body body) {
        this(name, params, params.length, false, 0, false, Py.NO_ARGS, null, body);
    }

    @Override
    public PyType type() {
        return Py.FUNCTION_TYPE;
    }

    @Override
    public Object bind(Object self) {
        return new PyMethod(self, this);
    }

    @Override
    public String repr() {
        return "<function " + name + ">";
    }

    /// The docstring, or null for none.
    private Object doc;
    /// Builds the annotations as name/value pairs; run once, on first read.
    private Body0 annotationThunk;

    /// Attaches what `def` knows besides the code: the docstring and the
    /// annotations, which are built on first read of `__annotations__`.
    public static Object meta(Object fn, Object doc, Body0 annotations) {
        if (fn instanceof PyFunction) {
            PyFunction f = (PyFunction) fn;
            f.doc = doc;
            f.annotationThunk = annotations;
        }
        return fn;
    }

    /// The annotations as the program [Annotations#build] runs, and the
    /// module whose names it reads: what most functions have in place of a
    /// thunk.
    private String annotationTable;
    private PyModule annotationModule;

    /// As [#meta(Object, Object, Body0)], the annotations a table.
    public static Object meta(Object fn, Object doc, PyModule module, String annotations) {
        if (fn instanceof PyFunction) {
            PyFunction f = (PyFunction) fn;
            f.doc = doc;
            f.annotationTable = annotations;
            f.annotationModule = module;
        }
        return fn;
    }

    /// One annotation's value: `value`'s result, or `fallback` when
    /// evaluating it raises (an annotation the runtime cannot express, such
    /// as a generic alias it does not model, never fails the definition).
    public static Object annotation(Body0 value, Object fallback) {
        try {
            return value.run();
        } catch (PyException e) {
            return fallback;
        }
    }

    int kindFlags;

    /// `__module__`: the module the `def` ran in, null for a function made
    /// by the runtime itself.
    private Object module;

    /// Sets `__module__`, which pickle-free code still reads: `repr` of a
    /// class built by a decorator, `functools.wraps`, registries keyed by
    /// `f"{f.__module__}.{f.__qualname__}"`. Not `mod`: that is `%`.
    public PyFunction inModule(Object m) {
        module = m;
        return this;
    }

    /// Sets `__qualname__` where it is not just the name.
    public PyFunction qual(String q) {
        qualname = q;
        return this;
    }

    /// Marks a generator, coroutine or async generator function with the
    /// `co_flags` bit CPython gives it (0x20, 0x80, 0x200).
    public PyFunction kind(int flags) {
        kindFlags = flags;
        return this;
    }

    /// How many leading parameters are positional-only (before a `/`).
    int positionalOnly;

    /// Marks the first `n` parameters positional-only: a keyword argument
    /// with one of their names is refused, or goes to `**kwargs`.
    public PyFunction positionalOnly(int n) {
        positionalOnly = n;
        return this;
    }

    private String qn() {
        return qualname != null ? qualname : name;
    }

    @Override
    public Object getAttrOrNull(String attr) {
        if (attrs != null) {
            Object v = attrs.getOrNull(attr);
            if (v != null) {
                return v;
            }
        }
        switch (attr) {
            case "__name__":
                return name;
            case "__qualname__":
                return qn();
            case "__doc__":
                return doc != null ? doc : Py.NONE;
            case "__module__":
                return module != null ? module : Py.NONE;
            case "__dict__":
                if (attrs == null) {
                    attrs = new PyDict();
                }
                return attrs;
            case "__defaults__":
                return defaults.length == 0 ? Py.NONE : PyTuple.of(defaults);
            case "__code__":
                return new PyCode(this);
            case "__kwdefaults__": {
                PyDict kd = new PyDict();
                int kwBase = positional + (varArgs ? 1 : 0);
                for (int p = 0; p < kwOnly; p++) {
                    if (kwDefaults != null && kwDefaults[p] != null) {
                        kd.setItem(Py.str(params[kwBase + p]), kwDefaults[p]);
                    }
                }
                return kd.len() == 0 ? Py.NONE : kd;
            }
            case "__annotations__":
                PyDict d = new PyDict();
                if (annotationThunk != null || annotationTable != null) {
                    Object made = annotationTable != null
                            ? Annotations.build(annotationModule, annotationTable) : annotationThunk.run();
                    Object[] kv = made instanceof Object[] ? (Object[]) made : Py.NO_ARGS;
                    for (int i = 0; i < kv.length; i += 2) {
                        d.setItem(kv[i], kv[i + 1]);
                    }
                }
                setAttr("__annotations__", d);
                return d;
            default:
                return super.getAttrOrNull(attr);
        }
    }

    @Override
    public void setAttr(String attr, Object value) {
        if (attrs == null) {
            attrs = new PyDict();
        }
        attrs.setItem(attr, value);
    }

    /// `del f.name` of an attribute set on the function, as click does with
    /// the `__click_params__` its decorators collect.
    @Override
    public void delAttr(String attr) {
        if (attrs == null || !attrs.contains(attr)) {
            throw Py.attributeError("'function' object has no attribute '" + attr + "'");
        }
        attrs.delItem(attr);
    }

    @Override
    public Object call(Object[] args, String[] kwNames) {
        if (kwNames.length == 0 && !varArgs && kwOnly == 0 && !varKw && args.length == positional) {
            return body.run(args);
        }
        if (varArgs && kwNames.length == 0 && kwOnly == 0 && !varKw && args.length >= positional) {
            // f(*args) called positionally: the slots are the leading
            // arguments and one tuple of the rest, with none of bindArgs'
            // keyword matching.
            Object[] slots = spareSlots;
            spareSlots = null;
            if (slots == null) {
                slots = new Object[positional + 1];
            }
            System.arraycopy(args, 0, slots, 0, positional);
            int rest = args.length - positional;
            if (rest == 0) {
                slots[positional] = PyTuple.EMPTY;
            } else {
                Object[] items = new Object[rest];
                System.arraycopy(args, positional, items, 0, rest);
                slots[positional] = new PyTuple(items);
            }
            Object r = body.run(slots);
            spareSlots = clear(slots);
            return r;
        }
        if (positional <= 6 && !varArgs && kwOnly == 0 && !varKw && args.length <= positional) {
            Object r = bindDirect(args, kwNames);
            if (r != UNBOUND) { //NOPMD CompareObjectsWithEquals - identity is meant
                return r;
            }
        }
        // One slot array per function, reused: every generated body copies
        // its slots into locals on entry (a generator into its fields when
        // it is made), so the array is free once run returns. A nested call
        // finds the spare taken and allocates its own; a throw just leaves
        // it taken. Python code here runs on one thread at a time.
        Object[] slots = spareSlots;
        spareSlots = null;
        if (slots == null) {
            slots = new Object[positional + (varArgs ? 1 : 0) + kwOnly + (varKw ? 1 : 0)];
        }
        Object r = body.run(bindArgs(slots, args, kwNames));
        spareSlots = clear(slots);
        return r;
    }

    /// True when a call of `n` arguments can bind in locals.
    private boolean direct(int n) {
        return positional <= 6 && !varArgs && kwOnly == 0 && !varKw && n <= positional;
    }

    @Override
    public Object callKwF(String[] kwNames, Object a) {
        if (direct(1)) {
            Object r = bindDirect(1, kwNames, a, null, null, null, null, null);
            if (r != UNBOUND) { //NOPMD CompareObjectsWithEquals - identity is meant
                return r;
            }
        }
        if (planned(1)) {
            Object r = bindPlanned(1, kwNames, a, null, null, null, null, null);
            if (r != UNBOUND) { //NOPMD CompareObjectsWithEquals - identity is meant
                return r;
            }
        }
        return call(new Object[] {a}, kwNames);
    }

    @Override
    public Object callKwF(String[] kwNames, Object a, Object b) {
        if (direct(2)) {
            Object r = bindDirect(2, kwNames, a, b, null, null, null, null);
            if (r != UNBOUND) { //NOPMD CompareObjectsWithEquals - identity is meant
                return r;
            }
        }
        if (planned(2)) {
            Object r = bindPlanned(2, kwNames, a, b, null, null, null, null);
            if (r != UNBOUND) { //NOPMD CompareObjectsWithEquals - identity is meant
                return r;
            }
        }
        return call(new Object[] {a, b}, kwNames);
    }

    @Override
    public Object callKwF(String[] kwNames, Object a, Object b, Object c) {
        if (direct(3)) {
            Object r = bindDirect(3, kwNames, a, b, c, null, null, null);
            if (r != UNBOUND) { //NOPMD CompareObjectsWithEquals - identity is meant
                return r;
            }
        }
        if (planned(3)) {
            Object r = bindPlanned(3, kwNames, a, b, c, null, null, null);
            if (r != UNBOUND) { //NOPMD CompareObjectsWithEquals - identity is meant
                return r;
            }
        }
        return call(new Object[] {a, b, c}, kwNames);
    }

    @Override
    public Object callKwF(String[] kwNames, Object a, Object b, Object c, Object d) {
        if (direct(4)) {
            Object r = bindDirect(4, kwNames, a, b, c, d, null, null);
            if (r != UNBOUND) { //NOPMD CompareObjectsWithEquals - identity is meant
                return r;
            }
        }
        if (planned(4)) {
            Object r = bindPlanned(4, kwNames, a, b, c, d, null, null);
            if (r != UNBOUND) { //NOPMD CompareObjectsWithEquals - identity is meant
                return r;
            }
        }
        return call(new Object[] {a, b, c, d}, kwNames);
    }

    @Override
    public Object callKwF(String[] kwNames, Object a, Object b, Object c, Object d, Object e) {
        if (direct(5)) {
            Object r = bindDirect(5, kwNames, a, b, c, d, e, null);
            if (r != UNBOUND) { //NOPMD CompareObjectsWithEquals - identity is meant
                return r;
            }
        }
        if (planned(5)) {
            Object r = bindPlanned(5, kwNames, a, b, c, d, e, null);
            if (r != UNBOUND) { //NOPMD CompareObjectsWithEquals - identity is meant
                return r;
            }
        }
        return call(new Object[] {a, b, c, d, e}, kwNames);
    }

    @Override
    public Object callKwF(String[] kwNames, Object a, Object b, Object c, Object d, Object e, Object f) {
        if (direct(6)) {
            Object r = bindDirect(6, kwNames, a, b, c, d, e, f);
            if (r != UNBOUND) { //NOPMD CompareObjectsWithEquals - identity is meant
                return r;
            }
        }
        if (planned(6)) {
            Object r = bindPlanned(6, kwNames, a, b, c, d, e, f);
            if (r != UNBOUND) { //NOPMD CompareObjectsWithEquals - identity is meant
                return r;
            }
        }
        return call(new Object[] {a, b, c, d, e, f}, kwNames);
    }

    /// True when a keyword call of `n` arguments can go through
    /// [#bindPlanned]: no `*args` or `**kwargs` to collect into.
    private boolean planned(int n) {
        return !varArgs && !varKw && n <= positional + kwOnly;
    }

    /// The slot of each name of the last keyword-name array [#bindPlanned]
    /// matched, keyword-only parameters included.
    private String[] plannedKw;
    private int[] plannedSlots;

    /// A keyword call of a function [#direct] cannot take -- more than six
    /// parameters, or keyword-only ones: the `n` arguments go straight into
    /// the reused slot array, defaults fill the rest, and the array body
    /// runs. [#UNBOUND] before running anything when the call is not that
    /// simple (an unknown, repeated or positional-only keyword, a missing
    /// argument, a positional-only parameter named), and [#call] then binds
    /// and reports it exactly as before.
    private Object bindPlanned(int n, String[] kwNames, Object a, Object b, Object c, Object d, Object e,
            Object f) {
        int nPos = n - kwNames.length;
        if (nPos > positional) {
            return UNBOUND;
        }
        int[] kwSlots;
        if (kwNames == plannedKw) { //NOPMD CompareObjectsWithEquals - identity is meant
            kwSlots = plannedSlots;
        } else {
            kwSlots = new int[kwNames.length];
            for (int i = 0; i < kwSlots.length; i++) {
                int slot = slotOf(kwNames[i]);
                if (slot < 0) {
                    for (int p = 0; p < kwOnly; p++) {
                        if (params[positional + p].equals(kwNames[i])) {
                            slot = positional + p;
                            break;
                        }
                    }
                }
                kwSlots[i] = slot;
            }
            plannedKw = kwNames;
            plannedSlots = kwSlots;
        }
        int total = positional + kwOnly;
        Object[] slots = spareSlots;
        spareSlots = null;
        if (slots == null) {
            slots = new Object[total];
        }
        for (int i = 0; i < n; i++) {
            int slot = i < nPos ? i : kwSlots[i - nPos];
            if (slot < 0 || slots[slot] != null) {
                spareSlots = clear(slots);
                return UNBOUND;
            }
            slots[slot] = i == 0 ? a : i == 1 ? b : i == 2 ? c : i == 3 ? d : i == 4 ? e : f;
        }
        int first = positional - defaults.length;
        for (int p = 0; p < total; p++) {
            if (slots[p] == null) {
                Object v = p < positional ? dflt(p, first) : kwDefaults == null ? null : kwDefaults[p - positional];
                if (v == null) {
                    spareSlots = clear(slots);
                    return UNBOUND;
                }
                slots[p] = v;
            }
        }
        Object r = body.run(slots);
        spareSlots = clear(slots);
        return r;
    }

    private Object[] spareSlots;

    /// Set when the body only ever passes its `**kwargs` on with `**` (a
    /// logger's `debug(msg, *args, **kwargs)`): a call without keywords then
    /// gets [#NO_KWARGS] rather than a new dict nobody can see, write or keep.
    /// The transpiler proves this; see `Emitter.kwForwardOnly`.
    private boolean kwShared;

    /// The empty `**kwargs` of a [#kwShared] function. Spreading it adds
    /// nothing, and a callee that collects keywords builds its own dict.
    static final PyDict NO_KWARGS = new PyDict();

    /// Marks the function [#kwShared].
    public PyFunction kwShared() {
        kwShared = true;
        return this;
    }

    /// The slot of each name of the last two keyword-name arrays
    /// [#bindDirect] matched. Every call site hoists its array into a static,
    /// so the same array arrives again and again: one identity check instead
    /// of a string comparison per keyword and parameter.
    private String[] directKw0;
    private int[] directSlots0;
    private String[] directKw1;
    private int[] directSlots1;

    private int[] directSlots(String[] kwNames) {
        if (kwNames == directKw0) { //NOPMD CompareObjectsWithEquals - identity is meant
            return directSlots0;
        }
        if (kwNames == directKw1) { //NOPMD CompareObjectsWithEquals - identity is meant
            return directSlots1;
        }
        int[] r = new int[kwNames.length];
        for (int i = 0; i < r.length; i++) {
            r[i] = slotOf(kwNames[i]);
        }
        directKw1 = directKw0;
        directSlots1 = directSlots0;
        directKw0 = kwNames;
        directSlots0 = r;
        return r;
    }

    /// The keyword-name arrays of the last few call sites that bound through
    /// [#bindArgs], and the slot each of their names goes to (-1 for a name
    /// that is no parameter).
    private static final int SEEN = 4;
    private String[][] seenKw;
    private int[][] seenSlots;
    private int seenNext;
    private String[] pendingKw;

    private static Object[] clear(Object[] a) {
        for (int i = 0; i < a.length; i++) {
            a[i] = null;
        }
        return a;
    }

    /// A call to a function of at most six plain parameters, keywords
    /// included (`f(x, key=y)`, `f(**kw)`): the arguments are matched to
    /// parameters in locals and the fixed-arity body runs, so no second
    /// array is bound. Null when the call is not that simple -- an unknown
    /// or repeated keyword, a missing argument, no body of this arity --
    /// it answers [#UNBOUND] before running anything, and [#bindArgs] then
    /// reports the call exactly as before.
    private Object bindDirect(Object[] args, String[] kwNames) {
        int n = args.length;
        return bindDirect(n, kwNames, n > 0 ? args[0] : null, n > 1 ? args[1] : null, n > 2 ? args[2] : null,
                n > 3 ? args[3] : null, n > 4 ? args[4] : null, n > 5 ? args[5] : null);
    }

    /// [#bindDirect] over the `n` arguments `a`..`f` themselves.
    private Object bindDirect(int n, String[] kwNames, Object a, Object b, Object c, Object d, Object e,
            Object f) {
        int nPos = n - kwNames.length;
        int[] kwSlots = kwNames.length == 0 ? null : directSlots(kwNames);
        Object x0 = null;
        Object x1 = null;
        Object x2 = null;
        Object x3 = null;
        Object x4 = null;
        Object x5 = null;
        for (int i = 0; i < n; i++) {
            int slot = i;
            if (i >= nPos) {
                slot = kwSlots[i - nPos];
                if (slot < 0) {
                    return UNBOUND;
                }
            }
            Object v = i == 0 ? a : i == 1 ? b : i == 2 ? c : i == 3 ? d : i == 4 ? e : f;
            switch (slot) {
                case 0:
                    if (x0 != null) {
                        return UNBOUND;
                    }
                    x0 = v;
                    break;
                case 1:
                    if (x1 != null) {
                        return UNBOUND;
                    }
                    x1 = v;
                    break;
                case 2:
                    if (x2 != null) {
                        return UNBOUND;
                    }
                    x2 = v;
                    break;
                case 3:
                    if (x3 != null) {
                        return UNBOUND;
                    }
                    x3 = v;
                    break;
                case 4:
                    if (x4 != null) {
                        return UNBOUND;
                    }
                    x4 = v;
                    break;
                default:
                    if (x5 != null) {
                        return UNBOUND;
                    }
                    x5 = v;
                    break;
            }
        }
        int first = positional - defaults.length;
        x0 = x0 != null || positional < 1 ? x0 : dflt(0, first);
        x1 = x1 != null || positional < 2 ? x1 : dflt(1, first);
        x2 = x2 != null || positional < 3 ? x2 : dflt(2, first);
        x3 = x3 != null || positional < 4 ? x3 : dflt(3, first);
        x4 = x4 != null || positional < 5 ? x4 : dflt(4, first);
        x5 = x5 != null || positional < 6 ? x5 : dflt(5, first);
        if (positional > 0 && x0 == null || positional > 1 && x1 == null || positional > 2 && x2 == null
                || positional > 3 && x3 == null || positional > 4 && x4 == null || positional > 5 && x5 == null) {
            return UNBOUND;
        }
        switch (positional) {
            case 0:
                return body0 != null ? body0.run() : UNBOUND;
            case 1:
                return body1 != null ? body1.run(x0) : UNBOUND;
            case 2:
                return body2 != null ? body2.run(x0, x1) : UNBOUND;
            case 3:
                return body3 != null ? body3.run(x0, x1, x2) : UNBOUND;
            case 4:
                return body4 != null ? body4.run(x0, x1, x2, x3) : UNBOUND;
            case 5:
                return body5 != null ? body5.run(x0, x1, x2, x3, x4) : UNBOUND;
            default:
                return body6 != null ? body6.run(x0, x1, x2, x3, x4, x5) : UNBOUND;
        }
    }

    /// The default of positional parameter `i`, or null when it has none.
    private Object dflt(int i, int first) {
        return i >= first ? defaults[i - first] : null;
    }

    /// The positional parameter named `k`, or -1. A positional-only one
    /// cannot be named, so [#bindArgs] reports or collects that keyword.
    private int slotOf(String k) {
        for (int p = positionalOnly; p < positional; p++) {
            if (params[p].equals(k)) {
                return p;
            }
        }
        return -1;
    }

    private boolean isPositionalOnly(String k) {
        for (int p = 0; p < positionalOnly; p++) {
            if (params[p].equals(k)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> add(List<String> l, String n) {
        List<String> r = l == null ? new ArrayList<String>() : l;
        r.add(n);
        return r;
    }

    /// CPython's "missing 2 required positional arguments: 'a' and 'b'".
    private PyException missing(String kind, List<String> names) {
        StringBuilder sb = new StringBuilder(qn()).append("() missing ").append(names.size()).append(" required ")
                .append(kind).append(" argument").append(names.size() == 1 ? "" : "s").append(": ");
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) {
                sb.append(names.size() == 2 ? " and " : i == names.size() - 1 ? ", and " : ", ");
            }
            sb.append('\'').append(names.get(i)).append('\'');
        }
        return Py.typeError(sb.toString());
    }

    Object[] bindArgs(Object[] args, String[] kwNames) {
        return bindArgs(null, args, kwNames);
    }

    /// Binds into `reuse` when given (all null), or into a new array.
    private Object[] bindArgs(Object[] reuse, Object[] args, String[] kwNames) {
        int slots = positional + (varArgs ? 1 : 0) + kwOnly + (varKw ? 1 : 0);
        Object[] out = reuse != null ? reuse : new Object[slots];
        int nPos = args.length - kwNames.length;
        int copy = Math.min(nPos, positional);
        System.arraycopy(args, 0, out, 0, copy);
        if (nPos > positional) {
            if (!varArgs) {
                int min = positional - defaults.length;
                String takes = min == positional ? String.valueOf(positional) : "from " + min + " to " + positional;
                throw Py.typeError(qn() + "() takes " + takes + " positional argument"
                        + (positional == 1 && min == positional ? "" : "s") + " but " + nPos
                        + (nPos == 1 ? " was" : " were") + " given");
            }
            Object[] rest = new Object[nPos - positional];
            System.arraycopy(args, positional, rest, 0, rest.length);
            out[positional] = new PyTuple(rest);
        } else if (varArgs) {
            out[positional] = PyTuple.EMPTY;
        }
        PyDict kw = varKw ? (kwShared && kwNames.length == 0 ? NO_KWARGS : new PyDict()) : null;
        int kwBase = positional + (varArgs ? 1 : 0);
        List<String> byKeyword = null;
        // Which slot each keyword names is the same every time one call site
        // calls: its names are one constant array. Matching them against the
        // parameters by text again was most of what binding a keyword call cost.
        int[] known = null;
        if (kwNames.length > 0 && seenKw != null) {
            for (int c = 0; c < SEEN; c++) {
                if (seenKw[c] == kwNames) { //NOPMD CompareObjectsWithEquals - identity is meant
                    known = seenSlots[c];
                    break;
                }
            }
        }
        // Learnt at the second sight of an array: one built for a single call
        // (`f(**kw)`) never comes back, and must not push out those that do.
        int[] learnt = null;
        if (known == null && kwNames.length > 0) {
            if (pendingKw == kwNames) { //NOPMD CompareObjectsWithEquals - identity is meant
                learnt = new int[kwNames.length];
            } else {
                pendingKw = kwNames;
            }
        }
        for (int i = 0; i < kwNames.length; i++) {
            String k = kwNames[i];
            Object v = args[nPos + i];
            int slot = -1;
            if (known != null) {
                slot = known[i];
            } else {
                for (int p = positionalOnly; p < positional; p++) {
                    if (params[p].equals(k)) {
                        slot = p;
                        break;
                    }
                }
                if (slot < 0) {
                    for (int p = 0; p < kwOnly; p++) {
                        if (params[kwBase + p].equals(k)) {
                            slot = kwBase + p;
                            break;
                        }
                    }
                }
                if (learnt != null) {
                    learnt[i] = slot;
                }
            }
            if (slot < 0) {
                if (kw == null) {
                    if (isPositionalOnly(k)) {
                        byKeyword = add(byKeyword, k);
                        continue;
                    }
                    throw Py.typeError(qn() + "() got an unexpected keyword argument '" + k + "'");
                }
                kw.setItem(k, v);
                continue;
            }
            if (out[slot] != null) {
                throw Py.typeError(qn() + "() got multiple values for argument '" + k + "'");
            }
            out[slot] = v;
        }
        if (byKeyword != null) {
            StringBuilder sb = new StringBuilder();
            for (String n : byKeyword) {
                sb.append(sb.length() == 0 ? "" : ", ").append(n);
            }
            throw Py.typeError(qn() + "() got some positional-only arguments passed as keyword arguments: '"
                    + sb + "'");
        }
        if (learnt != null) {
            if (seenKw == null) {
                seenKw = new String[SEEN][];
                seenSlots = new int[SEEN][];
            }
            seenKw[seenNext] = kwNames;
            seenSlots[seenNext] = learnt;
            seenNext = (seenNext + 1) % SEEN;
        }
        int firstDefault = positional - defaults.length;
        List<String> missing = null;
        for (int p = 0; p < positional; p++) {
            if (out[p] == null) {
                if (p >= firstDefault) {
                    out[p] = defaults[p - firstDefault];
                } else {
                    missing = add(missing, params[p]);
                }
            }
        }
        if (missing != null) {
            throw missing("positional", missing);
        }
        for (int p = 0; p < kwOnly; p++) {
            if (out[kwBase + p] == null) {
                Object d = kwDefaults == null ? null : kwDefaults[p];
                if (d == null) {
                    missing = add(missing, params[kwBase + p]);
                } else {
                    out[kwBase + p] = d;
                }
            }
        }
        if (missing != null) {
            throw missing("keyword-only", missing);
        }
        if (varKw) {
            out[slots - 1] = kw;
        }
        return out;
    }
}
