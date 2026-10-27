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

import java.util.HashMap;
import java.util.Map;

/// The `builtins` module: the builtin functions, the constructors of the
/// builtin types, and the methods of `object` and `BaseException`.
///
/// The transpiler resolves a name that no enclosing scope assigns to the
/// matching public field here, so `len(x)` compiles to a direct reference to
/// [#LEN] rather than a dictionary lookup.
public final class Builtins {
    /// Where `print` writes. Replaceable, for tests and for apps that route
    /// output into the UI.
    public interface Output {
        void write(String s);
    }

    private static Output out = new Output() {
        @Override
        public void write(String s) {
            System.out.print(s);
        }
    };

    /// Writes to the platform output, past any replaced `sys.stdout`.
    static void writeOut(String s) {
        out.write(s);
    }

    static final Map<String, Object> NAMES = new HashMap<String, Object>();

    public static final PyBuiltin OBJECT_NEW = PyBuiltin.positional("__new__", a -> {
        if (a.length == 0 || !(a[0] instanceof PyType)) {
            throw Py.notATypeObject("object", a);
        }
        return ((PyType) a[0]).newInstance();
    });
    /// Marks an optional argument that was not passed.
    private static final Object ABSENT = new PyStr("<absent>");
    public static final PyBuiltin OBJECT_INIT = PyBuiltin.positional("__init__", a -> Py.NONE);

    public static final Object PRINT = new PyBuiltin("print", (args, kw) -> {
        Args a = new Args("print", args, kw).check(Integer.MAX_VALUE, "sep", "end", "file", "flush");
        Object sepO = a.kw("sep");
        Object endO = a.kw("end");
        String sep = sepO == null || sepO == Py.NONE ? " " : Py.toStr(sepO); //NOPMD CompareObjectsWithEquals - identity is meant
        String end = endO == null || endO == Py.NONE ? "\n" : Py.toStr(endO); //NOPMD CompareObjectsWithEquals - identity is meant
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < a.npos; i++) {
            if (i > 0) {
                sb.append(sep);
            }
            sb.append(Py.toStr(args[i]));
        }
        sb.append(end);
        Object file = a.kw("file");
        if (file == null || file == Py.NONE) { //NOPMD CompareObjectsWithEquals - identity is meant
            // A program that replaced sys.stdout (contextlib.redirect_stdout)
            // prints there.
            file = StdModules.redirectedStdout();
        }
        if (file != null) {
            Py.callMethod(file, "write", Py.str(sb.toString()));
        } else {
            out.write(sb.toString());
        }
        return Py.NONE;
    });

    public static final Object LEN = PyBuiltin.positional("len", a -> PyInt.of(Py.len(one("len", a))));
    public static final Object REPR = PyBuiltin.positional("repr", a -> Py.str(Py.toRepr(one("repr", a))));
    public static final Object ASCII = PyBuiltin.positional("ascii", a -> Py.str(asciiOnly(Py.toRepr(one("ascii", a)))));
    public static final Object HASH = PyBuiltin.positional("hash", a -> PyInt.of(Py.hashOf(one("hash", a))));
    public static final Object ID = PyBuiltin.positional("id", a -> PyInt.of(System.identityHashCode(one("id", a))));
    public static final Object ABS = PyBuiltin.positional("abs", a -> abs(one("abs", a)));
    public static final Object CALLABLE = PyBuiltin.positional("callable", a -> Py.bool(isCallable(one("callable", a))));
    public static final Object ITER = PyBuiltin.positional("iter", a -> iter(a));
    public static final Object NEXT = PyBuiltin.positional("next", a -> next(a));
    public static final Object AITER = PyBuiltin.positional("aiter", a -> Py.callMethod(one("aiter", a), "__aiter__"));
    // anext with a default is an awaitable that answers the default on StopAsyncIteration; the
    // asyncio module supplies that wrapper, so the builtin covers the one-argument form.
    public static final Object ANEXT = PyBuiltin.positional("anext", a -> Py.callMethod(one("anext", a), "__anext__"));
    public static final Object ORD = PyBuiltin.positional("ord", a -> ord(one("ord", a)));
    public static final Object CHR = PyBuiltin.positional("chr", a -> {
        long c = Py.asLong(one("chr", a));
        if (c < 0 || c > 0x10ffff) {
            throw Py.valueError("chr() arg not in range(0x110000)");
        }
        return Py.str(new String(Character.toChars((int) c)));
    });
    public static final Object HEX = PyBuiltin.positional("hex", a -> Py.str(Formatting.format(one("hex", a), "#x")));
    public static final Object OCT = PyBuiltin.positional("oct", a -> Py.str(Formatting.format(one("oct", a), "#o")));
    public static final Object BIN = PyBuiltin.positional("bin", a -> Py.str(Formatting.format(one("bin", a), "#b")));
    public static final Object FORMAT = PyBuiltin.positional("format", a -> Py.format(a[0], a.length > 1 ? Py.toStr(a[1]) : ""));
    public static final Object DIVMOD = PyBuiltin.positional("divmod", a -> PyTuple.of(Py.floordiv(a[0], a[1]), Py.mod(a[0], a[1])));
    public static final Object POW = PyBuiltin.positional("pow", a -> pow(a));
    public static final Object ROUND = new PyBuiltin("round", (args, kw) -> round(new Args("round", args, kw).check(2, "number", "ndigits")));
    public static final Object ISINSTANCE = PyBuiltin.positional("isinstance", a -> Py.bool(Py.isinstance(a[0], a[1])));
    public static final Object ISSUBCLASS = PyBuiltin.positional("issubclass", a -> Py.bool(issubclass(a[0], a[1])));
    public static final Object GETATTR = PyBuiltin.positional("getattr", a -> getattr(a));
    public static final Object SETATTR = PyBuiltin.positional("setattr", a -> {
        Py.obj(a[0]).setAttr(Py.toStr(a[1]), a[2]);
        return Py.NONE;
    });
    public static final Object HASATTR = PyBuiltin.positional("hasattr", a -> {
        try {
            return Py.bool(Py.obj(a[0]).getAttrOrNull(Py.toStr(a[1])) != null);
        } catch (PyException e) {
            if (e.matches(Py.ATTRIBUTE_ERROR)) {
                return Py.FALSE;
            }
            throw e;
        }
    });
    public static final Object DELATTR = PyBuiltin.positional("delattr", a -> {
        Py.obj(a[0]).delAttr(Py.toStr(a[1]));
        return Py.NONE;
    });
    public static final Object VARS = PyBuiltin.positional("vars", a -> Py.obj(one("vars", a)).getAttr("__dict__"));
    public static final Object DIR = PyBuiltin.positional("dir", a -> dir(one("dir", a)));
    public static final Object SUM = new PyBuiltin("sum", (args, kw) -> sum(new Args("sum", args, kw).check(2, "start")));
    public static final Object MIN = new PyBuiltin("min", (args, kw) -> minMax("min", args, kw, true));
    public static final Object MAX = new PyBuiltin("max", (args, kw) -> minMax("max", args, kw, false));
    public static final Object ANY = PyBuiltin.positional("any", a -> {
        Object it = Py.iter(one("any", a));
        for (Object v = Py.next(it); v != null; v = Py.next(it)) {
            if (Py.isTrue(v)) {
                return Py.TRUE;
            }
        }
        return Py.FALSE;
    });
    public static final Object ALL = PyBuiltin.positional("all", a -> {
        Object it = Py.iter(one("all", a));
        for (Object v = Py.next(it); v != null; v = Py.next(it)) {
            if (!Py.isTrue(v)) {
                return Py.FALSE;
            }
        }
        return Py.TRUE;
    });
    public static final Object SORTED = new PyBuiltin("sorted", (args, kw) -> {
        Args a = new Args("sorted", args, kw).check(1, "key", "reverse");
        PyList l = new PyList();
        l.extend(a.required(0, "iterable"));
        Object rev = a.kw("reverse");
        l.sort(a.kw("key"), rev != null && Py.isTrue(rev));
        return l;
    });
    public static final Object REVERSED = PyBuiltin.positional("reversed", a -> reversed(one("reversed", a)));
    public static final Object ENUMERATE = new PyBuiltin("enumerate", (args, kw) -> {
        Args a = new Args("enumerate", args, kw).check(2, "iterable", "start");
        final Object it = Py.iter(a.required(0, "iterable"));
        Object start = a.get(1, "start", PyInt.of(0));
        final Object[] n = {start};
        return new PyJavaIter(() -> {
            Object v = Py.next(it);
            if (v == null) {
                return null;
            }
            Object r = PyTuple.of(n[0], v);
            n[0] = Py.add(n[0], PyInt.of(1));
            return r;
        });
    });
    public static final Object ZIP = new PyBuiltin("zip", (args, kw) -> {
        Args a = new Args("zip", args, kw).check(Integer.MAX_VALUE, "strict");
        final Object[] its = new Object[a.npos];
        for (int i = 0; i < its.length; i++) {
            its[i] = Py.iter(args[i]);
        }
        Object strictO = a.kw("strict");
        final boolean strict = strictO != null && Py.isTrue(strictO);
        return new PyJavaIter(() -> {
            if (its.length == 0) {
                return null;
            }
            Object[] row = new Object[its.length];
            for (int i = 0; i < its.length; i++) {
                row[i] = Py.next(its[i]);
                if (row[i] == null) {
                    if (strict) {
                        if (i > 0) {
                            throw Py.valueError("zip() argument " + (i + 1) + " is shorter than argument 1");
                        }
                        for (int j = 1; j < its.length; j++) {
                            if (Py.next(its[j]) != null) {
                                throw Py.valueError("zip() argument " + (j + 1) + " is longer than argument 1");
                            }
                        }
                    }
                    return null;
                }
            }
            return new PyTuple(row);
        });
    });
    public static final Object MAP = PyBuiltin.positional("map", a -> {
        final Object f = a[0];
        final Object[] its = new Object[a.length - 1];
        for (int i = 0; i < its.length; i++) {
            its[i] = Py.iter(a[i + 1]);
        }
        return new PyJavaIter(() -> {
            Object[] row = new Object[its.length];
            for (int i = 0; i < its.length; i++) {
                row[i] = Py.next(its[i]);
                if (row[i] == null) {
                    return null;
                }
            }
            return Py.obj(f).call(row, Py.NO_KW);
        });
    });
    public static final Object FILTER = PyBuiltin.positional("filter", a -> {
        final Object f = a[0];
        final Object it = Py.iter(a[1]);
        return new PyJavaIter(() -> {
            for (Object v = Py.next(it); v != null; v = Py.next(it)) {
                if (f == Py.NONE ? Py.isTrue(v) : Py.isTrue(Py.obj(f).call(new Object[] {v}, Py.NO_KW))) { //NOPMD CompareObjectsWithEquals - identity is meant
                    return v;
                }
            }
            return null;
        });
    });
    public static final Object SUPER = PyBuiltin.positional("super", a -> {
        if (a.length != 2 || !(a[0] instanceof PyType)) {
            throw Py.runtimeError("super(): no arguments");
        }
        return new PySuper((PyType) a[0], a[1]);
    });
    public static final Object INPUT = PyBuiltin.positional("input", a -> {
        throw Py.error(Py.OS_ERROR, "input() has no console on this platform");
    });

    private Builtins() {
    }

    /// Redirects `print`.
    public static void setOutput(Output o) {
        out = o;
    }

    /// Looks up a builtin by name, for `builtins.x` and `getattr(builtins, x)`.
    public static Object lookup(String name) {
        return NAMES.get(name);
    }

    // ---- installation ----------------------------------------------------

    static void install() {
        PyType o = Py.OBJECT_TYPE;
        o.define("__new__", OBJECT_NEW);
        o.define("__init__", OBJECT_INIT);
        o.define("__setattr__", PyBuiltin.positional("__setattr__", a -> {
            if (a[0] instanceof PyInstance) {
                ((PyInstance) a[0]).setAttrDirect(Py.toStr(a[1]), a[2]);
            } else if (a[0] instanceof PyType) {
                // What a metaclass's super().__setattr__ reaches: store,
                // never back through the metaclass.
                ((PyType) a[0]).setAttrDirect(Py.toStr(a[1]), a[2]);
            } else {
                Py.obj(a[0]).setAttr(Py.toStr(a[1]), a[2]);
            }
            return Py.NONE;
        }));
        o.define("__getattribute__", PyBuiltin.positional("__getattribute__", a -> {
            // The lookup without __getattr__: what a class's own
            // __getattribute__ or __getattr__ reaches for.
            String name = Py.toStr(a[1]);
            Object v = a[0] instanceof PyObject ? ((PyObject) a[0]).getAttrOrNull(name)
                    : Py.bindAttr(Py.typeOf(a[0]).lookup(name), a[0]);
            if (v == null) {
                throw Py.attributeError("'" + Py.typeOf(a[0]).name + "' object has no attribute '" + name + "'");
            }
            return v;
        }));
        o.define("__delattr__", PyBuiltin.positional("__delattr__", a -> {
            if (a[0] instanceof PyInstance) {
                ((PyInstance) a[0]).delAttrDirect(Py.toStr(a[1]));
            } else if (a[0] instanceof PyType) {
                ((PyType) a[0]).delAttrDirect(Py.toStr(a[1]));
            } else {
                Py.obj(a[0]).delAttr(Py.toStr(a[1]));
            }
            return Py.NONE;
        }));
        o.define("__repr__", PyBuiltin.positional("__repr__", a -> Py.str("<" + Py.typeOf(a[0]).name
                + " object at 0x" + Integer.toHexString(System.identityHashCode(a[0])) + ">")));
        o.define("__str__", PyBuiltin.positional("__str__", a -> Py.str(Py.toRepr(a[0]))));
        installExceptions();
        installFactories();
        TypeMethods.install();
        PyGenerator.install();
        ExceptionGroups.install();
        BuiltinSubclass.install();
        PyBytes.install();
        PyComplex.install();
        PyByteArray.install();
        PyMemoryView.install();
        names();
    }

    private static void installExceptions() {
        PyType b = Py.BASE_EXCEPTION;
        b.define("__init__", PyBuiltin.method(b, "__init__", a -> {
            Object[] rest = new Object[a.length - 1];
            System.arraycopy(a, 1, rest, 0, rest.length);
            if (a[0] instanceof PyInstance) {
                ((PyInstance) a[0]).setAttrDirect("args", new PyTuple(rest));
            }
            return Py.NONE;
        }));
        // Keyword arguments are accepted and ignored, as CPython's are: they
        // are for a subclass's __init__, and args holds the positional ones.
        b.define("__new__", new PyStaticMethod(new PyBuiltin("__new__", (all, kw) -> {
            Object[] a = all;
            if (kw.length > 0) {
                a = new Object[all.length - kw.length];
                System.arraycopy(all, 0, a, 0, a.length);
            }
            if (a.length == 0 || !(a[0] instanceof PyType)) {
                throw Py.notATypeObject("BaseException", a);
            }
            PyType t = (PyType) a[0];
            if (t == Py.OS_ERROR && a.length >= 3 && Py.isInt(a[1])) { //NOPMD CompareObjectsWithEquals - identity is meant
                // OSError(errno, strerror) is the subclass for that errno.
                t = osErrorFor(Py.asLong(a[1]));
            }
            PyInstance e = t.newInstance();
            Object[] rest = new Object[a.length - 1];
            System.arraycopy(a, 1, rest, 0, rest.length);
            e.setAttrDirect("args", new PyTuple(rest));
            return e;
        })));
        b.define("__str__", PyBuiltin.positional("__str__", a -> {
            Object[] args = exceptionArgs(a[0]);
            if (args.length == 0) {
                return Py.EMPTY_STR;
            }
            if (args.length == 1) {
                if (Py.typeOf(a[0]).isSubtype(Py.KEY_ERROR)) {
                    return Py.str(Py.toRepr(args[0]));
                }
                return Py.str(Py.toStr(args[0]));
            }
            return Py.str(new PyTuple(args).repr());
        }));
        b.define("__repr__", PyBuiltin.positional("__repr__", a -> {
            Object[] args = exceptionArgs(a[0]);
            String inner = args.length == 1 ? Py.toRepr(args[0]) : Py.joinRepr("", args, args.length, "");
            return Py.str(Py.typeOf(a[0]).name + "(" + inner + ")");
        }));
        installOsError();
        b.define("with_traceback", PyBuiltin.positional("with_traceback", a -> a[0]));
        b.define("__cause__", Py.NONE);
        b.define("__context__", Py.NONE);
        b.define("__suppress_context__", Py.FALSE);
        b.define("__traceback__", Py.NONE);
        Py.STOP_ITERATION.define("value", new PyProperty(PyBuiltin.positional("value", a -> {
            Object[] args = exceptionArgs(a[0]);
            return args.length == 0 ? Py.NONE : args[0];
        }), null, null));
        Py.SYSTEM_EXIT.define("code", new PyProperty(PyBuiltin.positional("code", a -> systemExitCode(a[0])),
                null, null));
    }

    /// `SystemExit.code`: None without arguments, the argument with one, the
    /// tuple of them with more.
    static Object systemExitCode(Object exc) {
        Object[] args = exceptionArgs(exc);
        if (args.length == 0) {
            return Py.NONE;
        }
        return args.length == 1 ? args[0] : new PyTuple(args);
    }

    /// OSError(errno, strerror[, filename[, winerror[, filename2]]]) keeps
    /// its parts as attributes, and `args` is just (errno, strerror).
    /// The errno numbers POSIX systems share; the ones that differ between
    /// Linux and Darwin (the socket errors, EAGAIN) stay plain OSError.
    private static PyType osErrorFor(long errno) {
        switch ((int) errno) {
            case 1:
            case 13:
                return Py.PERMISSION_ERROR;
            case 2:
                return Py.FILE_NOT_FOUND_ERROR;
            case 3:
                return Py.PROCESS_LOOKUP_ERROR;
            case 4:
                return Py.INTERRUPTED_ERROR;
            case 10:
                return Py.CHILD_PROCESS_ERROR;
            case 17:
                return Py.FILE_EXISTS_ERROR;
            case 20:
                return Py.NOT_A_DIRECTORY_ERROR;
            case 21:
                return Py.IS_A_DIRECTORY_ERROR;
            default:
                return Py.OS_ERROR;
        }
    }

    private static void installOsError() {
        PyType t = Py.OS_ERROR;
        t.define("__init__", PyBuiltin.method(t, "__init__", a -> {
            if (!(a[0] instanceof PyInstance)) {
                return Py.NONE;
            }
            PyInstance e = (PyInstance) a[0];
            int n = a.length - 1;
            Object[] args = new Object[n];
            System.arraycopy(a, 1, args, 0, n);
            Object none = Py.NONE;
            e.setAttrDirect("errno", n >= 2 && n <= 5 ? a[1] : none);
            e.setAttrDirect("strerror", n >= 2 && n <= 5 ? a[2] : none);
            e.setAttrDirect("filename", n >= 3 && n <= 5 ? a[3] : none);
            e.setAttrDirect("filename2", n == 5 ? a[5] : none);
            if (n >= 3 && n <= 5) {
                args = new Object[] {a[1], a[2]};
            }
            e.setAttrDirect("args", new PyTuple(args));
            return Py.NONE;
        }));
        t.define("__str__", PyBuiltin.positional("__str__", a -> {
            if (a[0] instanceof PyInstance) {
                PyInstance e = (PyInstance) a[0];
                Object errno = orNone(e.own("errno"));
                Object strerror = orNone(e.own("strerror"));
                Object filename = orNone(e.own("filename"));
                Object filename2 = orNone(e.own("filename2"));
                String head = "[Errno " + Py.toStr(errno) + "] " + Py.toStr(strerror);
                if (filename != Py.NONE) { //NOPMD CompareObjectsWithEquals - identity is meant
                    head += ": " + Py.toRepr(filename);
                    if (filename2 != Py.NONE) { //NOPMD CompareObjectsWithEquals - identity is meant
                        head += " -> " + Py.toRepr(filename2);
                    }
                    return Py.str(head);
                }
                if (errno != Py.NONE && strerror != Py.NONE) { //NOPMD CompareObjectsWithEquals - identity is meant
                    return Py.str(head);
                }
            }
            return Py.call(Py.BASE_EXCEPTION.lookup("__str__"), a[0]);
        }));
    }

    static Object[] exceptionArgs(Object e) {
        Object args = ((PyInstance) e).own("args");
        return args instanceof PyTuple ? ((PyTuple) args).items : Py.NO_ARGS;
    }

    private static void installFactories() {
        Py.METHOD_TYPE.factory = (t, args, kw) -> {
            if (args.length != 2 || kw.length != 0) {
                throw Py.typeError("method expected 2 arguments, got " + args.length);
            }
            if (!isCallable(args[0])) {
                throw Py.typeError("first argument must be callable");
            }
            if (args[1] == Py.NONE) {
                throw Py.typeError("instance must not be None");
            }
            return new PyMethod(args[1], args[0]);
        };
        Py.INT_TYPE.factory = (t, args, kw) -> {
            Args a = new Args("int", args, kw).check(2, "base");
            Object x = a.get(0, "x", null);
            Object base = a.get(1, "base", null);
            if (x == null) {
                return PyInt.of(0);
            }
            if (base != null) {
                return PyInt.parse(intText(x, true), (int) Py.asLong(base));
            }
            return toInt(x);
        };
        Py.BOOL_TYPE.factory = (t, args, kw) -> Py.bool(args.length > 0 && Py.isTrue(args[0]));
        Py.FLOAT_TYPE.factory = (t, args, kw) -> args.length == 0 ? Py.ofFloat(0) : toFloat(args[0]);
        Py.STR_TYPE.factory = (t, args, kw) -> {
            Object[] p = Args.bind("str", args, kw, new String[] {"object", "encoding", "errors"},
                    new Object[] {Py.EMPTY_STR, ABSENT, ABSENT});
            if (p[1] == ABSENT && p[2] == ABSENT) {
                return Py.str(Py.toStr(p[0]));
            }
            if (!(p[0] instanceof PyBytes)) {
                throw Py.typeError("decoding to str: need a bytes-like object, " + Py.typeOf(p[0]).name + " found");
            }
            return Py.str(Codecs.decode(((PyBytes) p[0]).value, p[1] == ABSENT ? "utf-8" : Py.toStr(p[1]),
                    p[2] == ABSENT ? "strict" : Py.toStr(p[2])));
        };
        Py.LIST_TYPE.factory = (t, args, kw) -> {
            PyList l = new PyList();
            if (args.length > 0) {
                l.extend(args[0]);
            }
            return l;
        };
        Py.TUPLE_TYPE.factory = (t, args, kw) -> args.length == 0 ? PyTuple.EMPTY
                : args[0] instanceof PyTuple ? args[0] : new PyTuple(Py.toArray(args[0]));
        Py.DICT_TYPE.factory = (t, args, kw) -> {
            PyDict d = new PyDict();
            int npos = args.length - kw.length;
            if (npos > 0) {
                d.update(args[0]);
            }
            for (int i = 0; i < kw.length; i++) {
                d.setItem(Py.str(kw[i]), args[npos + i]);
            }
            return d;
        };
        Py.SET_TYPE.factory = (t, args, kw) -> args.length == 0 ? new PySet() : PySet.of(args[0], false);
        Py.FROZENSET_TYPE.factory = (t, args, kw) -> args.length == 0 ? new PySet(true) : PySet.of(args[0], true);
        Py.RANGE_TYPE.factory = (t, args, kw) -> {
            if (args.length == 1) {
                return new PyRange(0, Py.index(args[0]), 1);
            }
            if (args.length == 2) {
                return new PyRange(Py.index(args[0]), Py.index(args[1]), 1);
            }
            if (args.length == 3) {
                return new PyRange(Py.index(args[0]), Py.index(args[1]), Py.index(args[2]));
            }
            throw Py.typeError("range expected at most 3 arguments, got " + args.length);
        };
        Py.SLICE_TYPE.factory = (t, args, kw) -> {
            if (args.length == 1) {
                return new PySlice(null, args[0], null);
            }
            return new PySlice(args[0], args[1], args.length > 2 ? args[2] : null);
        };
        Py.TYPE_TYPE.factory = (t, args, kw) -> {
            if (args.length == 1 && kw.length == 0) {
                return Py.typeOf(args[0]);
            }
            if (args.length - kw.length != 3) {
                throw Py.typeError("type() takes 1 or 3 arguments");
            }
            Object[] bs = Py.toArray(args[1]);
            PyType[] bases = new PyType[bs.length];
            for (int i = 0; i < bs.length; i++) {
                if (!(bs[i] instanceof PyType)) {
                    throw Py.typeError("bases must be types");
                }
                bases[i] = (PyType) bs[i];
            }
            PyType w = PyType.winner(Py.TYPE_TYPE, bases);
            if (w != Py.TYPE_TYPE) { //NOPMD CompareObjectsWithEquals - identity is meant
                return w.call(args, kw);
            }
            return PyType.typeNew(Py.prepend(Py.TYPE_TYPE, args), kw);
        };
        Py.TYPE_TYPE.define("__new__", new PyStaticMethod(new PyBuiltin("__new__", PyType::typeNew)));
        Py.TYPE_TYPE.define("__call__", PyBuiltin.methodKw(Py.TYPE_TYPE, "__call__", (args, kw) -> {
            Object[] rest = new Object[args.length - 1];
            System.arraycopy(args, 1, rest, 0, rest.length);
            return ((PyType) args[0]).construct(rest, kw);
        }));
        Py.OBJECT_TYPE.define("__init_subclass__", new PyClassMethod(new PyBuiltin("__init_subclass__", (args, kw) -> {
            if (kw.length > 0) {
                String owner = args.length > 0 && args[0] instanceof PyType ? ((PyType) args[0]).name + "." : "";
                throw Py.typeError(owner + "__init_subclass__() takes no keyword arguments");
            }
            return Py.NONE;
        })));
        Py.PROPERTY_TYPE.factory = (t, args, kw) -> {
            Args a = new Args("property", args, kw).check(4, "fget", "fset", "fdel", "doc");
            return new PyProperty(a.get(0, "fget", null), a.get(1, "fset", null), a.get(2, "fdel", null));
        };
        // Callables expose __call__, so `type(f).__dict__` answers the way
        // collections.abc.Callable's check reads it.
        PyType[] callables = {Py.FUNCTION_TYPE, Py.BUILTIN_TYPE, Py.METHOD_TYPE};
        for (PyType callable : callables) {
            callable.define("__call__", new PyBuiltin("__call__", (args, kw) -> {
                Object[] rest = new Object[args.length - 1];
                System.arraycopy(args, 1, rest, 0, rest.length);
                return Py.obj(args[0]).call(rest, kw);
            }));
        }
        // The descriptor protocol, spelled out for code that asks for it by
        // name (enum and dataclasses tell members from methods this way).
        Py.PROPERTY_TYPE.define("__get__", PyBuiltin.positional("__get__", a -> {
            PyProperty p = property(a[0]);
            return a.length < 2 || a[1] == Py.NONE ? a[0] : p.get(a[1]);
        }));
        Py.PROPERTY_TYPE.define("__set__", PyBuiltin.positional("__set__", a -> {
            property(a[0]).set(a[1], a[2]);
            return Py.NONE;
        }));
        Py.PROPERTY_TYPE.define("__delete__", PyBuiltin.positional("__delete__", a -> {
            property(a[0]).delete(a[1]);
            return Py.NONE;
        }));
        // On the type rather than only on PyProperty, so an instance of a
        // property subclass has them too; a copy keeps the receiver's class.
        Py.PROPERTY_TYPE.define("getter", PyBuiltin.positional("getter", a -> {
            PyProperty p = property(a[0]);
            return propertyCopy(a[0], a[1], p.fset, p.fdel);
        }));
        Py.PROPERTY_TYPE.define("setter", PyBuiltin.positional("setter", a -> {
            PyProperty p = property(a[0]);
            return propertyCopy(a[0], p.fget, a[1], p.fdel);
        }));
        Py.PROPERTY_TYPE.define("deleter", PyBuiltin.positional("deleter", a -> {
            PyProperty p = property(a[0]);
            return propertyCopy(a[0], p.fget, p.fset, a[1]);
        }));
        String[] parts = {"fget", "fset", "fdel"};
        for (int i = 0; i < parts.length; i++) {
            final int which = i;
            Py.PROPERTY_TYPE.define(parts[i], new PyProperty(PyBuiltin.positional(parts[i], a -> {
                PyProperty p = property(a[0]);
                Object v = which == 0 ? p.fget : which == 1 ? p.fset : p.fdel;
                return v == null ? Py.NONE : v;
            }), null, null));
        }
        PyType[] binders = {Py.FUNCTION_TYPE, Py.CLASSMETHOD_TYPE, Py.STATICMETHOD_TYPE};
        for (PyType binder : binders) {
            final PyType bt = binder;
            bt.define("__get__", PyBuiltin.positional("__get__", a -> {
                Object inst = a.length > 1 ? a[1] : Py.NONE;
                if (inst == Py.NONE) { //NOPMD CompareObjectsWithEquals - identity is meant
                    if (bt == Py.FUNCTION_TYPE) { //NOPMD CompareObjectsWithEquals - identity is meant
                        return a[0];
                    }
                    inst = a.length > 2 ? a[2] : Py.NONE;
                    if (inst == Py.NONE) { //NOPMD CompareObjectsWithEquals - identity is meant
                        return a[0];
                    }
                }
                return Py.bindAttr(a[0], inst);
            }));
        }
        Py.CLASSMETHOD_TYPE.factory = (t, args, kw) -> new PyClassMethod(args[0]);
        Py.STATICMETHOD_TYPE.factory = (t, args, kw) -> new PyStaticMethod(args[0]);
    }

    /// `type(self)(fget, fset, fdel)`: what getter, setter and deleter give.
    private static Object propertyCopy(Object self, Object fget, Object fset, Object fdel) {
        if (self instanceof PyProperty) {
            return new PyProperty(fget, fset, fdel);
        }
        return Py.call(Py.typeOf(self), fget == null ? Py.NONE : fget, fset == null ? Py.NONE : fset,
                fdel == null ? Py.NONE : fdel);
    }

    private static PyProperty property(Object o) {
        if (o instanceof PyInstance && ((PyInstance) o).base instanceof PyProperty) {
            // An instance of a property subclass.
            return (PyProperty) ((PyInstance) o).base;
        }
        if (!(o instanceof PyProperty)) {
            throw Py.typeError("descriptor requires a 'property' object but received a '" + Py.typeOf(o).name + "'");
        }
        return (PyProperty) o;
    }

    private static void names() {
        PyType[] types = {Py.OBJECT_TYPE, Py.TYPE_TYPE, Py.INT_TYPE, Py.BOOL_TYPE, Py.FLOAT_TYPE, Py.COMPLEX_TYPE, Py.STR_TYPE, Py.BYTES_TYPE,
            Py.BYTEARRAY_TYPE, Py.MEMORYVIEW_TYPE,
            Py.LIST_TYPE, Py.TUPLE_TYPE, Py.DICT_TYPE, Py.SET_TYPE, Py.FROZENSET_TYPE, Py.RANGE_TYPE,
            Py.SLICE_TYPE, Py.PROPERTY_TYPE, Py.CLASSMETHOD_TYPE, Py.STATICMETHOD_TYPE,
            Py.BASE_EXCEPTION, Py.SYSTEM_EXIT, Py.KEYBOARD_INTERRUPT, Py.GENERATOR_EXIT, Py.EXCEPTION,
            Py.STOP_ITERATION, Py.ARITHMETIC_ERROR, Py.ZERO_DIVISION_ERROR, Py.OVERFLOW_ERROR,
            Py.ASSERTION_ERROR, Py.ATTRIBUTE_ERROR, Py.LOOKUP_ERROR, Py.INDEX_ERROR, Py.KEY_ERROR,
            Py.NAME_ERROR, Py.UNBOUND_LOCAL_ERROR, Py.RUNTIME_ERROR, Py.NOT_IMPLEMENTED_ERROR,
            Py.RECURSION_ERROR, Py.TYPE_ERROR, Py.VALUE_ERROR, Py.UNICODE_ERROR, Py.IMPORT_ERROR,
            Py.MODULE_NOT_FOUND_ERROR, Py.OS_ERROR, Py.FILE_NOT_FOUND_ERROR, Py.MEMORY_ERROR};
        for (PyType e : types) {
            NAMES.put(e.name, e);
        }
        for (PyType t : Py.EXCEPTION_TYPES) {
            NAMES.put(t.name, t);
        }
        // What the transpiler rewrites a `match` case into (see PyMatch).
        NAMES.put("__cn1_match__", PyBuiltin.positional("__cn1_match__", a -> PyMatch.match(a[0], a[1])));
        ExceptionGroups.names(NAMES);
        // open() is io.open, written in Python; the transpiler bundles `io`
        // into any program that mentions open.
        NAMES.put("open", new PyBuiltin("open", (args, kw) ->
                Py.obj(PyModule.load("io").getAttr("open")).call(args, kw)));
        NAMES.put("EnvironmentError", Py.OS_ERROR);
        NAMES.put("IOError", Py.OS_ERROR);
        NAMES.put("None", Py.NONE);
        NAMES.put("True", Py.TRUE);
        NAMES.put("False", Py.FALSE);
        NAMES.put("NotImplemented", Py.NOT_IMPLEMENTED);
        NAMES.put("Ellipsis", Py.ELLIPSIS);
        Object[] fns = {PRINT, LEN, REPR, ASCII, HASH, ID, ABS, CALLABLE, ITER, NEXT, ORD, CHR, HEX, OCT, BIN,
            FORMAT, DIVMOD, POW, ROUND, ISINSTANCE, ISSUBCLASS, GETATTR, SETATTR, HASATTR, DELATTR, VARS, DIR,
            SUM, MIN, MAX, ANY, ALL, SORTED, REVERSED, ENUMERATE, ZIP, MAP, FILTER, SUPER, INPUT, AITER, ANEXT};
        for (Object fn : fns) {
            NAMES.put(((PyBuiltin) fn).name, fn);
        }
        NAMES.put("EnvironmentError", Py.OS_ERROR);
        NAMES.put("IOError", Py.OS_ERROR);
    }

    // ---- implementations -------------------------------------------------

    private static Object one(String fn, Object[] a) {
        if (a.length != 1) {
            throw Py.typeError(fn + "() takes exactly one argument (" + a.length + " given)");
        }
        return a[0];
    }

    static Object toInt(Object x) {
        if (x instanceof PyBool) {
            return PyInt.of(((PyBool) x).value);
        }
        if (Py.isInt(x)) {
            return x;
        }
        if (Py.isFloat(x)) {
            double d = Py.asDouble(x);
            if (Double.isNaN(d)) {
                throw Py.valueError("cannot convert float NaN to integer");
            }
            if (Double.isInfinite(d)) {
                throw Py.overflowError("cannot convert float infinity to integer");
            }
            if (Math.abs(d) < 9.2e18) {
                return PyInt.of((long) d);
            }
            return Py.mul(PyInt.parse(Formatting.fixed(Math.abs(d), 0), 10), PyInt.of(d < 0 ? -1 : 1));
        }
        if (x instanceof String || x instanceof PyBytes || x instanceof PyByteArray) {
            return PyInt.parse(intText(x, false), 10);
        }
        Object m = Py.typeOf(x).lookup("__int__");
        if (m == null) {
            m = Py.typeOf(x).lookup("__index__");
        }
        if (m != null) {
            return Py.obj(m).call(new Object[] {x}, Py.NO_KW);
        }
        throw Py.typeError("int() argument must be a string, a bytes-like object or a real number, not '"
                + Py.typeOf(x).name + "'");
    }

    /// The text `int()` parses: a str, or the ASCII of a bytes-like value.
    private static String intText(Object x, boolean explicitBase) {
        if (x instanceof String) {
            return (String) x;
        }
        if (x instanceof PyStr) {
            return ((PyStr) x).value;
        }
        byte[] b = x instanceof PyBytes || x instanceof PyByteArray ? PyBytes.like(x) : null;
        if (b == null) {
            if (explicitBase) {
                throw Py.typeError("int() can't convert non-string with explicit base");
            }
            throw Py.typeError("int() argument must be a string, a bytes-like object or a real number, not '"
                    + Py.typeOf(x).name + "'");
        }
        char[] c = new char[b.length];
        for (int i = 0; i < b.length; i++) {
            c[i] = (char) (b[i] & 0xff);
        }
        return new String(c);
    }

    static Object toFloat(Object x) {
        if (Py.isFloat(x)) {
            return x;
        }
        if (Py.isInt(x)) {
            return Py.ofFloat(Py.asDouble(x));
        }
        if (x instanceof String) {
            String raw = ((String) x).trim();
            String s = raw.replace("_", "");
            String l = TypeMethods.asciiLower(s);
            if ("nan".equals(l) || "+nan".equals(l) || "-nan".equals(l)) {
                return Py.ofFloat(Double.NaN);
            }
            if ("inf".equals(l) || "+inf".equals(l) || "infinity".equals(l) || "+infinity".equals(l)) {
                return Py.ofFloat(Double.POSITIVE_INFINITY);
            }
            if ("-inf".equals(l) || "-infinity".equals(l)) {
                return Py.ofFloat(Double.NEGATIVE_INFINITY);
            }
            // Python's grammar, checked here: a device's parseDouble may
            // accept more than the JDK's ("1+" parsed as 1.0 on ParparVM).
            if (decimalLiteral(raw)) {
                try {
                    return Py.ofFloat(Double.parseDouble(s));
                } catch (NumberFormatException e) {
                    // reported below with Python's message
                }
            }
            throw Py.valueError("could not convert string to float: " + Py.toRepr(x));
        }
        return Py.ofFloat(Py.asDouble(x));
    }

    /// `[sign] (digits [. [digits]] | . digits) [e [sign] digits]`, where
    /// digits may hold single underscores between digits.
    static boolean decimalLiteral(String s) {
        int n = s.length();
        int i = 0;
        if (i < n && (s.charAt(i) == '+' || s.charAt(i) == '-')) {
            i++;
        }
        int d = digits(s, i);
        boolean any = d > i;
        i = d;
        if (i < n && s.charAt(i) == '.') {
            int f = digits(s, i + 1);
            any |= f > i + 1;
            i = f;
        }
        if (!any) {
            return false;
        }
        if (i < n && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) {
            i++;
            if (i < n && (s.charAt(i) == '+' || s.charAt(i) == '-')) {
                i++;
            }
            int e = digits(s, i);
            if (e <= i) {
                return false;
            }
            i = e;
        }
        return i == n;
    }

    /// The end of a run of digits starting at `i` (`i` itself when there is
    /// none). An underscore not between two digits ends the run, and so is
    /// left over for the caller to reject.
    private static int digits(String s, int i) {
        int j = i;
        while (j < s.length()) {
            char c = s.charAt(j);
            if (c >= '0' && c <= '9') {
                j++;
            } else if (c == '_' && j > i && j + 1 < s.length() && s.charAt(j + 1) >= '0' && s.charAt(j + 1) <= '9') {
                j++;
            } else {
                break;
            }
        }
        return j;
    }

    private static Object abs(Object x) {
        if (x instanceof PyComplex) {
            return Py.ofFloat(PyComplex.abs((PyComplex) x));
        }
        if (Py.isFloat(x)) {
            return Py.ofFloat(Math.abs(Py.asDouble(x)));
        }
        if (Py.isInt(x)) {
            PyInt i = Py.intView(x);
            return i.isTrue() && Py.ltBool(i, PyInt.of(0)) ? i.neg() : PyInt.view(0).add(i);
        }
        Object m = Py.typeOf(x).lookup("__abs__");
        if (m == null) {
            throw Py.typeError("bad operand type for abs(): '" + Py.typeOf(x).name + "'");
        }
        return Py.obj(m).call(new Object[] {x}, Py.NO_KW);
    }

    static boolean isCallable(Object x) {
        return x instanceof PyFunction || x instanceof PyBuiltin || x instanceof PyMethod || x instanceof PyType
                || x instanceof PyStaticMethod || x instanceof PyInstance && Py.typeOf(x).lookup("__call__") != null;
    }

    private static Object iter(Object[] a) {
        if (a.length == 2) {
            final Object f = a[0];
            final Object sentinel = a[1];
            return new PyJavaIter(() -> {
                Object v = Py.obj(f).call(Py.NO_ARGS, Py.NO_KW);
                return Py.eqBool(v, sentinel) ? null : v;
            });
        }
        return Py.iter(one("iter", a));
    }

    private static Object next(Object[] a) {
        Object v = Py.next(a[0]);
        if (v == null) {
            if (a.length > 1) {
                return a[1];
            }
            if (a[0] instanceof PyGenerator) {
                // A finished generator's StopIteration carries its return value.
                throw PyGenerator.stopIteration(((PyGenerator) a[0]).returned());
            }
            throw new PyException(Py.STOP_ITERATION.call(Py.NO_ARGS, Py.NO_KW));
        }
        return v;
    }

    private static Object ord(Object x) {
        if (!(x instanceof String) || PyStr.codePoints((String) x) != 1) {
            throw Py.typeError("ord() expected a character, but string of length "
                    + (x instanceof String ? PyStr.codePoints((String) x) : 0) + " found");
        }
        return PyInt.of(((String) x).codePointAt(0));
    }

    private static String asciiOnly(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            int c = s.codePointAt(i);
            if (c > 0xffff) {
                i++;
            }
            if (c < 128) {
                sb.append((char) c);
            } else if (c < 256) {
                sb.append("\\x");
                PyStr.hex(sb, c, 2);
            } else if (c < 0x10000) {
                sb.append("\\u");
                PyStr.hex(sb, c, 4);
            } else {
                sb.append("\\U");
                PyStr.hex(sb, c, 8);
            }
        }
        return sb.toString();
    }

    private static Object pow(Object[] a) {
        if (a.length == 3 && a[2] != Py.NONE) { //NOPMD CompareObjectsWithEquals - identity is meant
            PyInt base = Py.intView(toInt(a[0]));
            PyInt exp = Py.intView(toInt(a[1]));
            PyInt mod = Py.intView(toInt(a[2]));
            com.codename1.util.BigInteger m = mod.toBig();
            if (exp.toBig().signum() < 0) {
                return PyInt.of(base.toBig().modInverse(m).modPow(exp.toBig().negate(), m));
            }
            com.codename1.util.BigInteger r = base.toBig().modPow(exp.toBig(), m.abs());
            if (m.signum() < 0 && r.signum() != 0) {
                r = r.add(m);
            }
            return PyInt.of(r);
        }
        return Py.pow(a[0], a[1]);
    }

    private static Object round(Args a) {
        Object x = a.required(0, "number");
        Object nd = a.get(1, "ndigits", Py.NONE);
        if (x instanceof PyInstance) {
            Object m = Py.typeOf(x).lookup("__round__");
            if (m != null) {
                return nd == Py.NONE ? Py.obj(m).call(new Object[] {x}, Py.NO_KW) : Py.obj(m).call(new Object[] {x, nd}, Py.NO_KW); //NOPMD CompareObjectsWithEquals - identity is meant
            }
        }
        if (Py.isInt(x)) {
            if (nd == Py.NONE || Py.asLong(nd) >= 0) { //NOPMD CompareObjectsWithEquals - identity is meant
                return PyInt.view(0).add(x);
            }
            Object p = PyInt.view(10).pow(PyInt.of(-Py.asLong(nd)));
            Object half = Py.floordiv(p, PyInt.of(2));
            Object q = Py.floordiv(x, p);
            Object r = Py.mod(x, p);
            if (Py.ltBool(half, r) || Py.eqBool(r, half) && Py.isTrue(Py.mod(q, PyInt.of(2)))) {
                q = Py.add(q, PyInt.of(1));
            }
            return Py.mul(q, p);
        }
        double d = Py.asDouble(x);
        if (nd == Py.NONE) { //NOPMD CompareObjectsWithEquals - identity is meant
            double r = Math.rint(d);
            return toInt(Py.ofFloat(r));
        }
        long n = Py.asLong(nd);
        if (Double.isNaN(d) || Double.isInfinite(d) || d == 0) {
            return Py.ofFloat(d);
        }
        if (n >= 0) {
            if (n > 300) {
                return Py.ofFloat(d);
            }
            String s = Formatting.fixed(Math.abs(d), (int) n);
            double r = Double.parseDouble(s);
            return Py.ofFloat(d < 0 ? -r : r);
        }
        double p = Math.pow(10, -n);
        double r = Math.rint(d / p) * p;
        return Py.ofFloat(r);
    }

    private static boolean issubclass(Object c, Object t) {
        if (!(c instanceof PyType)) {
            throw Py.typeError("issubclass() arg 1 must be a class");
        }
        if (t instanceof PyTuple) {
            Object[] items = ((PyTuple) t).items;
            for (Object item : items) {
                if (issubclass(c, item)) {
                    return true;
                }
            }
            return false;
        }
        if (!(t instanceof PyType)) {
            throw Py.typeError("issubclass() arg 2 must be a class, a tuple of classes, or a union");
        }
        return ((PyType) t).subclassCheck((PyType) c);
    }

    private static Object getattr(Object[] a) {
        if (a.length == 3) {
            try {
                Object v = Py.obj(a[0]).getAttrOrNull(Py.toStr(a[1]));
                return v != null ? v : a[2];
            } catch (PyException e) {
                if (e.matches(Py.ATTRIBUTE_ERROR)) {
                    return a[2];
                }
                throw e;
            }
        }
        return Py.obj(a[0]).getAttr(Py.toStr(a[1]));
    }

    private static Object dir(Object o) {
        PyList l = new PyList();
        if (o instanceof PyModule) {
            // A module lists its namespace, never its type's attributes.
            Object it = ((PyModule) o).globals().iter();
            for (Object k = Py.next(it); k != null; k = Py.next(it)) {
                l.append(k);
            }
            l.sort(null, false);
            return l;
        }
        java.util.Set<String> seen = new java.util.HashSet<String>();
        if (o instanceof PyInstance) {
            seen.addAll(java.util.Arrays.asList(((PyInstance) o).attributeNames()));
        }
        PyType t = o instanceof PyType ? (PyType) o : Py.typeOf(o);
        for (int i = 0; i < t.mro.length; i++) {
            seen.addAll(t.mro[i].dict.keySet());
        }
        for (String s : seen) {
            l.append(Py.str(s));
        }
        l.sort(null, false);
        return l;
    }

    private static Object sum(Args a) {
        Object it = Py.iter(a.required(0, "iterable"));
        Object acc = a.get(1, "start", PyInt.of(0));
        if (acc instanceof String) {
            throw Py.typeError("sum() can't sum strings [use ''.join(seq) instead]");
        }
        // Integer runs stay in a long; floats use Neumaier summation as CPython 3.12 does.
        long ia = 0;
        boolean intMode = Py.isInt(acc) && !Py.intView(acc).isBig() && !(acc instanceof PyBool);
        if (intMode) {
            ia = Py.intView(acc).value;
        }
        double fs = 0;
        double fc = 0;
        boolean floatMode = false;
        for (Object v = Py.next(it); v != null; v = Py.next(it)) {
            if (intMode) {
                if (Py.isInt(v) && !Py.intView(v).isBig()) {
                    long b = Py.intView(v).value;
                    long r = ia + b;
                    if (((ia ^ r) & (b ^ r)) >= 0) {
                        ia = r;
                        continue;
                    }
                }
                acc = PyInt.of(ia);
                intMode = false;
                if (Py.isFloat(v)) {
                    floatMode = true;
                    fs = Py.asDouble(acc);
                    fc = 0;
                }
            }
            if (floatMode) {
                if (Py.isFloat(v) || Py.isInt(v)) {
                    double x = Py.asDouble(v);
                    double t = fs + x;
                    if (Math.abs(fs) >= Math.abs(x)) {
                        fc += (fs - t) + x;
                    } else {
                        fc += (x - t) + fs;
                    }
                    fs = t;
                    continue;
                }
                acc = Py.ofFloat(fs + fc);
                floatMode = false;
            }
            if (Py.isFloat(acc) && Py.isFloat(v) && !floatMode) {
                floatMode = true;
                fs = Py.asDouble(acc);
                fc = 0;
                double x = Py.asDouble(v);
                double t = fs + x;
                fc += Math.abs(fs) >= Math.abs(x) ? (fs - t) + x : (x - t) + fs;
                fs = t;
                continue;
            }
            acc = Py.add(acc, v);
        }
        if (intMode) {
            return PyInt.of(ia);
        }
        if (floatMode) {
            return Py.ofFloat(fs + fc);
        }
        return acc;
    }

    private static Object minMax(String fn, Object[] args, String[] kw, boolean min) {
        Args a = new Args(fn, args, kw).check(Integer.MAX_VALUE, "key", "default");
        Object key = a.kw("key");
        if (key == Py.NONE) { //NOPMD CompareObjectsWithEquals - identity is meant
            key = null;
        }
        Object def = a.kw("default");
        Object it;
        if (a.npos == 1) {
            it = Py.iter(args[0]);
        } else {
            if (def != null) {
                throw Py.typeError("Cannot specify a default for " + fn + "() with multiple positional arguments");
            }
            it = new PyTuple(a.positional()).iter();
        }
        Object best = null;
        Object bestKey = null;
        for (Object v = Py.next(it); v != null; v = Py.next(it)) {
            Object k = key == null ? v : Py.obj(key).call(new Object[] {v}, Py.NO_KW);
            if (best == null || (min ? Py.ltBool(k, bestKey) : Py.ltBool(bestKey, k))) {
                best = v;
                bestKey = k;
            }
        }
        if (best == null) {
            if (def != null) {
                return def;
            }
            throw Py.valueError(fn + "() iterable argument is empty");
        }
        return best;
    }

    private static Object reversed(Object seq) {
        Object m = Py.typeOf(seq).lookup("__reversed__");
        if (m != null) {
            return Py.obj(m).call(new Object[] {seq}, Py.NO_KW);
        }
        if (seq instanceof PyInstance && ((PyInstance) seq).base != null) {
            // A subclass of a built-in reverses as its built-in value does.
            Object base = ((PyInstance) seq).base;
            if (base instanceof PyDict || base instanceof PyList || base instanceof PyTuple) {
                return reversed(base);
            }
        }
        if (seq instanceof PyDict) {
            Object[] keys = ((PyDict) seq).keyArray();
            return reversedArray(keys);
        }
        if (seq instanceof PyList || seq instanceof PyTuple) {
            return reversedArray(Py.toArray(seq));
        }
        final Object s = seq;
        final int[] i = {Py.len(seq)};
        return new PyJavaIter(() -> i[0] <= 0 ? null : Py.obj(s).getItem(PyInt.of(--i[0])));
    }

    private static Object reversedArray(final Object[] items) {
        final int[] i = {items.length};
        return new PyJavaIter(() -> i[0] <= 0 ? null : items[--i[0]]);
    }

    /// An attribute an instance does not have, read as `None`.
    private static Object orNone(Object o) {
        return o == null ? Py.NONE : o;
    }

}
