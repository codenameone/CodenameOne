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

/// The `operator` module's functions and its three callable types, which
/// CPython implements in C (`_operator`). They were Python here: `itemgetter`
/// a class whose `__call__` tested `len(self._items)` on every call, and
/// `lt`, `add` and the rest each a `def` -- pdfplumber sorts characters with
/// one, and `sorted(key=itemgetter(0))` calls it per element.
final class Operator {

    private Operator() {
    }

    static void install(PyModule m) {
        m.setAttr("lt", PyBuiltin.fixed2("lt", Py::lt));
        m.setAttr("le", PyBuiltin.fixed2("le", Py::le));
        m.setAttr("eq", PyBuiltin.fixed2("eq", Py::eq));
        m.setAttr("ne", PyBuiltin.fixed2("ne", Py::ne));
        m.setAttr("ge", PyBuiltin.fixed2("ge", Py::ge));
        m.setAttr("gt", PyBuiltin.fixed2("gt", Py::gt));
        m.setAttr("not_", PyBuiltin.fixed1("not_", a -> Py.bool(!Py.isTrue(a))));
        m.setAttr("truth", PyBuiltin.fixed1("truth", a -> Py.bool(Py.isTrue(a))));
        m.setAttr("is_", PyBuiltin.fixed2("is_", (a, b) -> Py.bool(a == b))); //NOPMD CompareObjectsWithEquals - identity is meant
        m.setAttr("is_not", PyBuiltin.fixed2("is_not", (a, b) -> Py.bool(a != b))); //NOPMD CompareObjectsWithEquals - identity is meant
        m.setAttr("is_none", PyBuiltin.fixed1("is_none", a -> Py.bool(a == Py.NONE))); //NOPMD CompareObjectsWithEquals - identity is meant
        m.setAttr("is_not_none", PyBuiltin.fixed1("is_not_none", a -> Py.bool(a != Py.NONE))); //NOPMD CompareObjectsWithEquals - identity is meant
        m.setAttr("add", PyBuiltin.fixed2("add", Py::add));
        m.setAttr("sub", PyBuiltin.fixed2("sub", Py::sub));
        m.setAttr("mul", PyBuiltin.fixed2("mul", Py::mul));
        m.setAttr("truediv", PyBuiltin.fixed2("truediv", Py::truediv));
        m.setAttr("floordiv", PyBuiltin.fixed2("floordiv", Py::floordiv));
        m.setAttr("mod", PyBuiltin.fixed2("mod", Py::mod));
        m.setAttr("pow", PyBuiltin.fixed2("pow", Py::pow));
        m.setAttr("lshift", PyBuiltin.fixed2("lshift", Py::lshift));
        m.setAttr("rshift", PyBuiltin.fixed2("rshift", Py::rshift));
        m.setAttr("and_", PyBuiltin.fixed2("and_", Py::and));
        m.setAttr("or_", PyBuiltin.fixed2("or_", Py::or));
        m.setAttr("xor", PyBuiltin.fixed2("xor", Py::xor));
        m.setAttr("neg", PyBuiltin.fixed1("neg", Py::neg));
        m.setAttr("pos", PyBuiltin.fixed1("pos", Py::pos));
        PyBuiltin inv = PyBuiltin.fixed1("inv", Py::invert);
        m.setAttr("inv", inv);
        m.setAttr("invert", PyBuiltin.fixed1("invert", Py::invert));
        m.setAttr("contains", PyBuiltin.fixed2("contains", (a, b) -> Py.in(b, a)));
        m.setAttr("getitem", PyBuiltin.fixed2("getitem", Py::getitem));
        m.setAttr("setitem", PyBuiltin.fixed3("setitem", (a, b, c) -> {
            Py.setitem(a, b, c);
            return Py.NONE;
        }));
        m.setAttr("itemgetter", ITEMGETTER);
        m.setAttr("attrgetter", ATTRGETTER);
        m.setAttr("methodcaller", METHODCALLER);
    }

    private static PyType type(String name, PyType.Factory f) {
        PyType t = new PyType(name, new PyType[] {Py.OBJECT_TYPE}, f);
        t.define("__module__", "operator");
        return t;
    }

    private static void noKeywords(String fn, String[] kw) {
        if (kw.length != 0) {
            throw Py.typeError(fn + "() takes no keyword arguments");
        }
    }

    private static PyException arity(String fn, int n) {
        return Py.typeError(fn + " expected 1 argument, got " + n);
    }

    static final PyType ITEMGETTER = type("itemgetter", (t, a, k) -> {
        noKeywords("itemgetter", k);
        if (a.length == 0) {
            throw Py.typeError("itemgetter expected 1 argument, got 0");
        }
        return new ItemGetter(t, a.clone());
    });

    static final PyType ATTRGETTER = type("attrgetter", (t, a, k) -> {
        noKeywords("attrgetter", k);
        if (a.length == 0) {
            throw Py.typeError("attrgetter expected 1 argument, got 0");
        }
        String[][] paths = new String[a.length][];
        for (int i = 0; i < a.length; i++) {
            if (!(a[i] instanceof String)) {
                throw Py.typeError("attribute name must be a string");
            }
            paths[i] = split((String) a[i]);
        }
        return new AttrGetter(t, a.clone(), paths);
    });

    static final PyType METHODCALLER = type("methodcaller", (t, a, k) -> {
        int npos = a.length - k.length;
        if (npos < 1) {
            throw Py.typeError("methodcaller needs at least one argument, the method name");
        }
        if (!(a[0] instanceof String)) {
            throw Py.typeError("method name must be a string");
        }
        Object[] rest = new Object[a.length - 1];
        System.arraycopy(a, 1, rest, 0, rest.length);
        return new MethodCaller(t, (String) a[0], rest, k.clone());
    });

    /// A dotted attribute path, split once when the getter is made.
    private static String[] split(String path) {
        int n = 1;
        for (int i = path.indexOf('.'); i >= 0; i = path.indexOf('.', i + 1)) {
            n++;
        }
        String[] r = new String[n];
        int start = 0;
        for (int i = 0; i < n - 1; i++) {
            int dot = path.indexOf('.', start);
            r[i] = path.substring(start, dot);
            start = dot + 1;
        }
        r[n - 1] = start == 0 ? path : path.substring(start);
        return r;
    }

    private static String reprArgs(String name, Object[] args) {
        StringBuilder b = new StringBuilder("operator.").append(name).append('(');
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                b.append(", ");
            }
            b.append(Py.toRepr(args[i]));
        }
        return b.append(')').toString();
    }

    /// The base of the three: a type of their own and a one-argument call.
    private abstract static class Getter extends PyObject {
        private final PyType type;

        Getter(PyType type) {
            this.type = type;
        }

        @Override
        public PyType type() {
            return type;
        }

        abstract Object apply(Object obj);

        abstract String name();

        @Override
        public Object call1(Object a) {
            return apply(a);
        }

        @Override
        public Object call(Object[] args, String[] kwNames) {
            noKeywords(name(), kwNames);
            if (args.length != 1) {
                throw arity(name(), args.length);
            }
            return apply(args[0]);
        }
    }

    static final class ItemGetter extends Getter {
        private final Object[] items;
        private final Object single;

        ItemGetter(PyType t, Object[] items) {
            super(t);
            this.items = items;
            this.single = items.length == 1 ? items[0] : null;
        }

        @Override
        String name() {
            return "itemgetter";
        }

        @Override
        Object apply(Object obj) {
            if (single != null) {
                return Py.getitem(obj, single);
            }
            Object[] out = new Object[items.length];
            for (int i = 0; i < out.length; i++) {
                out[i] = Py.getitem(obj, items[i]);
            }
            return new PyTuple(out);
        }

        @Override
        public String repr() {
            return reprArgs("itemgetter", items);
        }
    }

    static final class AttrGetter extends Getter {
        private final Object[] names;
        private final String[][] paths;

        AttrGetter(PyType t, Object[] names, String[][] paths) {
            super(t);
            this.names = names;
            this.paths = paths;
        }

        @Override
        String name() {
            return "attrgetter";
        }

        private static Object walk(Object obj, String[] path) {
            Object v = obj;
            for (String p : path) {
                v = Py.getattr(v, p);
            }
            return v;
        }

        @Override
        Object apply(Object obj) {
            if (paths.length == 1) {
                return walk(obj, paths[0]);
            }
            Object[] out = new Object[paths.length];
            for (int i = 0; i < out.length; i++) {
                out[i] = walk(obj, paths[i]);
            }
            return new PyTuple(out);
        }

        @Override
        public String repr() {
            return reprArgs("attrgetter", names);
        }
    }

    static final class MethodCaller extends Getter {
        private final String method;
        private final Object[] args;
        private final String[] kw;

        MethodCaller(PyType t, String method, Object[] args, String[] kw) {
            super(t);
            this.method = method;
            this.args = args;
            this.kw = kw;
        }

        @Override
        String name() {
            return "methodcaller";
        }

        @Override
        Object apply(Object obj) {
            Object f = Py.getattr(obj, method);
            if (kw.length == 0) {
                switch (args.length) {
                    case 0:
                        return Py.obj(f).call0();
                    case 1:
                        return Py.obj(f).call1(args[0]);
                    default:
                        break;
                }
            }
            return Py.obj(f).call(args.clone(), kw);
        }

        @Override
        public String repr() {
            StringBuilder b = new StringBuilder("operator.methodcaller(").append(Py.toRepr(method));
            int npos = args.length - kw.length;
            for (int i = 0; i < npos; i++) {
                b.append(", ").append(Py.toRepr(args[i]));
            }
            for (int i = 0; i < kw.length; i++) {
                b.append(", ").append(kw[i]).append('=').append(Py.toRepr(args[npos + i]));
            }
            return b.append(')').toString();
        }
    }
}
