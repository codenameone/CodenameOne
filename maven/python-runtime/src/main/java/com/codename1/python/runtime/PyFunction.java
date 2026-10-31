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
    private Body1 body1;
    private Body2 body2;
    private Body3 body3;
    private Body4 body4;
    private Body5 body5;
    private Body6 body6;
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
        return widen(1) ? fill(1, a, null, null, null, null) : call(new Object[] {a}, Py.NO_KW);
    }

    @Override
    public Object call2(Object a, Object b) {
        if (body2 != null) {
            return body2.run(a, b);
        }
        return widen(2) ? fill(2, a, b, null, null, null) : call(new Object[] {a, b}, Py.NO_KW);
    }

    @Override
    public Object call3(Object a, Object b, Object c) {
        if (body3 != null) {
            return body3.run(a, b, c);
        }
        return widen(3) ? fill(3, a, b, c, null, null) : call(new Object[] {a, b, c}, Py.NO_KW);
    }

    @Override
    public Object call4(Object a, Object b, Object c, Object d) {
        if (body4 != null) {
            return body4.run(a, b, c, d);
        }
        return widen(4) ? fill(4, a, b, c, d, null) : call(new Object[] {a, b, c, d}, Py.NO_KW);
    }

    @Override
    public Object call5(Object a, Object b, Object c, Object d, Object e) {
        if (body5 != null) {
            return body5.run(a, b, c, d, e);
        }
        return widen(5) ? fill(5, a, b, c, d, e) : call(new Object[] {a, b, c, d, e}, Py.NO_KW);
    }

    @Override
    public Object call6(Object a, Object b, Object c, Object d, Object e, Object f) {
        return body6 != null ? body6.run(a, b, c, d, e, f) : call(new Object[] {a, b, c, d, e, f}, Py.NO_KW);
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
                return Py.NONE;
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
                if (annotationThunk != null) {
                    Object made = annotationThunk.run();
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

    @Override
    public Object call(Object[] args, String[] kwNames) {
        if (kwNames.length == 0 && !varArgs && kwOnly == 0 && !varKw && args.length == positional) {
            return body.run(args);
        }
        if (positional <= 6 && !varArgs && kwOnly == 0 && !varKw && args.length <= positional) {
            Object r = bindDirect(args, kwNames);
            if (r != UNBOUND) { //NOPMD CompareObjectsWithEquals - identity is meant
                return r;
            }
        }
        return body.run(bindArgs(args, kwNames));
    }

    /// A call to a function of at most six plain parameters, keywords
    /// included (`f(x, key=y)`, `f(**kw)`): the arguments are matched to
    /// parameters in locals and the fixed-arity body runs, so no second
    /// array is bound. Null when the call is not that simple -- an unknown
    /// or repeated keyword, a missing argument, no body of this arity --
    /// it answers [#UNBOUND] before running anything, and [#bindArgs] then
    /// reports the call exactly as before.
    private Object bindDirect(Object[] args, String[] kwNames) {
        int nPos = args.length - kwNames.length;
        Object x0 = null;
        Object x1 = null;
        Object x2 = null;
        Object x3 = null;
        Object x4 = null;
        Object x5 = null;
        for (int i = 0; i < args.length; i++) {
            int slot = i;
            if (i >= nPos) {
                slot = slotOf(kwNames[i - nPos]);
                if (slot < 0) {
                    return UNBOUND;
                }
            }
            Object v = args[i];
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

    /// The positional parameter named `k`, or -1.
    private int slotOf(String k) {
        for (int p = 0; p < positional; p++) {
            if (params[p].equals(k)) {
                return p;
            }
        }
        return -1;
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
        int slots = positional + (varArgs ? 1 : 0) + kwOnly + (varKw ? 1 : 0);
        Object[] out = new Object[slots];
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
        PyDict kw = varKw ? new PyDict() : null;
        int kwBase = positional + (varArgs ? 1 : 0);
        for (int i = 0; i < kwNames.length; i++) {
            String k = kwNames[i];
            Object v = args[nPos + i];
            int slot = -1;
            for (int p = 0; p < positional; p++) {
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
            if (slot < 0) {
                if (kw == null) {
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
