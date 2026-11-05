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
import java.util.Map;

/// `BaseExceptionGroup`, `ExceptionGroup` and `except*`.
///
/// The transpiler rewrites a `try` with `except*` clauses into an ordinary
/// `try` whose single handler drives an [ExceptStar]: each clause takes the
/// part of the group its type matches, an exception its body raises is
/// recorded, and `finish` raises what is left, as PEP 654 specifies.
final class ExceptionGroups {
    private ExceptionGroups() {
    }

    static void install() {
        PyType b = Py.BASE_EXCEPTION_GROUP;
        b.define("__new__", new PyStaticMethod(PyBuiltin.positional("__new__", a -> {
            if (a.length != 3 || !(a[0] instanceof PyType)) {
                throw Py.typeError("BaseExceptionGroup.__new__() takes exactly 2 arguments (" + (a.length - 1)
                        + " given)");
            }
            return create((PyType) a[0], a[1], a[2]);
        })));
        b.define("__init__", PyBuiltin.positional("__init__", a -> Py.NONE));
        b.define("__str__", PyBuiltin.positional("__str__", a -> {
            int n = exceptions(a[0]).length;
            return Py.str(Py.toStr(message(a[0])) + " (" + n + " sub-exception" + (n == 1 ? "" : "s") + ")");
        }));
        b.define("split", PyBuiltin.positional("split", a -> {
            Object[] r = split(a[0], a[1]);
            return new PyTuple(new Object[] {r[0] == null ? Py.NONE : r[0], r[1] == null ? Py.NONE : r[1]});
        }));
        b.define("subgroup", PyBuiltin.positional("subgroup", a -> {
            Object m = split(a[0], a[1])[0];
            return m == null ? Py.NONE : m;
        }));
        b.define("derive", PyBuiltin.positional("derive", a -> create(Py.BASE_EXCEPTION_GROUP, message(a[0]), a[1])));
    }

    static void names(Map<String, Object> names) {
        names.put("__cn1_except_star__", PyBuiltin.positional("__cn1_except_star__", a -> new ExceptStar(a[0])));
        names.put("__cn1_except_star_take__", PyBuiltin.positional("__cn1_except_star_take__",
                a -> star(a[0]).take(a[1])));
        names.put("__cn1_except_star_raised__", PyBuiltin.positional("__cn1_except_star_raised__", a -> {
            star(a[0]).raised.add(a[1]);
            return Py.NONE;
        }));
        names.put("__cn1_except_star_finish__", PyBuiltin.positional("__cn1_except_star_finish__", a -> {
            star(a[0]).finish();
            return Py.NONE;
        }));
    }

    private static ExceptStar star(Object o) {
        if (o instanceof ExceptStar) {
            return (ExceptStar) o;
        }
        throw Py.typeError("internal error: not an except* state");
    }

    /// `BaseExceptionGroup(message, exceptions)`: an `ExceptionGroup` when
    /// `cls` is the base and every exception is an `Exception`.
    static PyInstance create(PyType cls, Object message, Object excs) {
        if (!(message instanceof String)) {
            throw Py.typeError("argument 1 must be str, not " + Py.typeOf(message).name);
        }
        Object[] items = Py.toArray(excs);
        if (items.length == 0) {
            throw Py.valueError("second argument (exceptions) must be a non-empty sequence");
        }
        boolean allExceptions = true;
        for (int i = 0; i < items.length; i++) {
            if (!Py.isinstance(items[i], Py.BASE_EXCEPTION)) {
                throw Py.valueError("Item " + i + " of second argument (exceptions) is not an exception");
            }
            allExceptions &= Py.isinstance(items[i], Py.EXCEPTION);
        }
        if (cls == Py.BASE_EXCEPTION_GROUP && allExceptions) { //NOPMD CompareObjectsWithEquals - identity is meant
            cls = Py.EXCEPTION_GROUP;
        } else if (cls.isSubtype(Py.EXCEPTION_GROUP) && !allExceptions) {
            throw Py.typeError("Cannot nest BaseExceptions in an ExceptionGroup");
        }
        PyInstance e = cls.newInstance();
        e.setAttrDirect("args", new PyTuple(new Object[] {message, excs}));
        e.setAttrDirect("message", message);
        e.setAttrDirect("exceptions", new PyTuple(items));
        return e;
    }

    static boolean isGroup(Object e) {
        return Py.isinstance(e, Py.BASE_EXCEPTION_GROUP);
    }

    private static Object message(Object eg) {
        return Py.obj(eg).getAttr("message");
    }

    private static Object[] exceptions(Object eg) {
        return Py.toArray(Py.obj(eg).getAttr("exceptions"));
    }

    private static boolean matches(Object e, Object cond) {
        if (cond instanceof PyType || cond instanceof PyTuple) {
            return Py.isinstance(e, cond);
        }
        return Py.isTrue(Py.call(cond, e));
    }

    /// `eg.split(cond)`: the matching part and the rest, either null.
    static Object[] split(Object eg, Object cond) {
        if (matches(eg, cond)) {
            return new Object[] {eg, null};
        }
        List<Object> ms = new ArrayList<Object>();
        List<Object> rs = new ArrayList<Object>();
        Object[] items = exceptions(eg);
        for (Object item : items) {
            Object e = item;
            if (isGroup(e)) {
                Object[] r = split(e, cond);
                if (r[0] != null) {
                    ms.add(r[0]);
                }
                if (r[1] != null) {
                    rs.add(r[1]);
                }
            } else if (matches(e, cond)) {
                ms.add(e);
            } else {
                rs.add(e);
            }
        }
        return new Object[] {derive(eg, ms), derive(eg, rs)};
    }

    private static Object derive(Object eg, List<Object> excs) {
        if (excs.isEmpty()) {
            return null;
        }
        PyList l = new PyList();
        for (int i = 0; i < excs.size(); i++) { //NOPMD ForLoopCanBeForeach - indexed: a for-each allocates an iterator
            l.append(excs.get(i));
        }
        Object d = Py.callMethod(eg, "derive", l);
        Object notes = Py.obj(eg).getAttrOrNull("__notes__");
        if (notes != null) {
            Py.obj(d).setAttr("__notes__", notes);
        }
        return d;
    }

    /// The leaf exceptions of `e`, a group or a single exception.
    static void leaves(Object e, List<Object> out) {
        if (!isGroup(e)) {
            out.add(e);
            return;
        }
        Object[] items = exceptions(e);
        for (Object item : items) {
            leaves(item, out);
        }
    }

    /// The part of group `eg` whose leaves are among `keep`, in its
    /// structure, or null.
    static Object select(Object eg, List<Object> keep) {
        List<Object> kept = new ArrayList<Object>();
        Object[] items = exceptions(eg);
        boolean all = true;
        for (Object item : items) {
            Object e = item;
            Object k = isGroup(e) ? select(e, keep) : keep.contains(e) ? e : null;
            if (k != null) {
                kept.add(k);
            }
            all &= k == e; //NOPMD CompareObjectsWithEquals - identity is meant
        }
        return all ? eg : derive(eg, kept);
    }

    /// The state of one `except*` dispatch.
    static final class ExceptStar extends PyObject {
        private final Object original;
        private Object rest;
        private final Object whole;
        private boolean taken;
        final List<Object> raised = new ArrayList<Object>();
        private final List<Object> matched = new ArrayList<Object>();

        ExceptStar(Object exc) {
            original = exc;
            rest = isGroup(exc) ? exc : create(Py.BASE_EXCEPTION_GROUP, Py.EMPTY_STR, new PyList(new Object[] {exc}));
            whole = rest;
        }

        @Override
        public PyType type() {
            return Py.OBJECT_TYPE;
        }

        Object take(Object type) {
            Object[] ts = type instanceof PyTuple ? ((PyTuple) type).items : new Object[] {type};
            for (Object x : ts) {
                if (x instanceof PyType && ((PyType) x).isSubtype(Py.BASE_EXCEPTION_GROUP)) {
                    throw Py.typeError("catching ExceptionGroup with except* is not allowed. Use except instead.");
                }
            }
            if (rest == null) {
                return Py.NONE;
            }
            Object[] r = split(rest, type);
            if (r[0] == null) {
                return Py.NONE;
            }
            taken = true;
            rest = r[1];
            matched.add(r[0]);
            return r[0];
        }

        void finish() {
            List<Object> fresh = new ArrayList<Object>();
            List<Object> keepLeaves = new ArrayList<Object>();
            boolean reraised = false;
            for (int i = 0; i < raised.size(); i++) { //NOPMD ForLoopCanBeForeach - indexed: a for-each allocates an iterator
                Object r = raised.get(i);
                if (matched.contains(r)) {
                    // A bare `raise`: that part stays unhandled, in the
                    // original group's structure.
                    leaves(r, keepLeaves);
                    reraised = true;
                } else {
                    fresh.add(r);
                }
            }
            Object keep;
            if (!taken) {
                keep = original;
            } else if (reraised) {
                if (rest != null) {
                    leaves(rest, keepLeaves);
                }
                keep = select(whole, keepLeaves);
            } else {
                keep = rest;
            }
            if (fresh.isEmpty()) {
                if (keep != null) {
                    throw new PyException(keep);
                }
                return;
            }
            if (keep == null && fresh.size() == 1) {
                // One new exception and nothing left over propagates as it is.
                throw new PyException(fresh.get(0));
            }
            PyList l = new PyList();
            for (int i = 0; i < fresh.size(); i++) { //NOPMD ForLoopCanBeForeach - indexed: a for-each allocates an iterator
                l.append(fresh.get(i));
            }
            if (keep != null) {
                l.append(keep);
            }
            throw new PyException(create(Py.BASE_EXCEPTION_GROUP, Py.EMPTY_STR, l));
        }
    }
}
