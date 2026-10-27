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

/// Standard library modules implemented in Java because CPython implements
/// them in C: `math`, `sys`, `time`. Each is built the first time it is
/// imported.
public final class StdModules {

    private StdModules() {
    }

    /// Builds and registers module `name` if this class provides it.
    static boolean provide(String name) {
        if ("math".equals(name)) {
            PyModule.registerModule(math());
            return true;
        }
        if ("sys".equals(name)) {
            PyModule.registerModule(sys());
            return true;
        }
        if ("_time".equals(name)) {
            PyModule.registerModule(time());
            return true;
        }
        if ("atexit".equals(name)) {
            PyModule.registerModule(atexit());
            return true;
        }
        if ("_weakref".equals(name)) {
            PyModule.registerModule(weakref());
            return true;
        }
        if ("_cn1fs".equals(name)) {
            PyModule.registerModule(fileSystem());
            return true;
        }
        if ("builtins".equals(name)) {
            PyModule m = new PyModule("builtins", new String[0]);
            for (java.util.Map.Entry<String, Object> e : Builtins.NAMES.entrySet()) {
                m.setAttr(e.getKey(), e.getValue());
            }
            PyModule.registerModule(m);
            return true;
        }
        return false;
    }

    /// The `atexit` handlers, each its function followed by its arguments
    /// (the trailing ones named by the matching entry of `EXIT_KW`).
    private static final java.util.List<Object[]> EXIT_CALLS = new java.util.ArrayList<Object[]>();
    private static final java.util.List<String[]> EXIT_KW = new java.util.ArrayList<String[]>();

    /// Runs the `atexit` handlers, last registered first, once each. The
    /// generated `runMain()` calls this when the main module finishes,
    /// whether or not it raised; a handler that raises is reported on
    /// stderr and the rest still run, as in CPython.
    /// What a program that ended with `e` exits with, reporting it on stderr
    /// as the interpreter does: a `SystemExit` with no code or None is 0,
    /// with an int that int, with anything else 1 after printing it; any
    /// other exception is printed and gives 1.
    public static int exitStatus(PyException e) {
        if (e.matches(Py.SYSTEM_EXIT)) {
            Object code = Builtins.systemExitCode(e.value);
            if (code == Py.NONE) { //NOPMD CompareObjectsWithEquals - identity is meant
                return 0;
            }
            if (Py.isInt(code)) {
                return (int) Py.asLong(code);
            }
            System.err.println(Py.toStr(code));
            return 1;
        }
        System.err.println("Traceback (most recent call last):");
        System.err.println(e.getMessage());
        return 1;
    }

    public static void runExitHandlers() {
        while (!EXIT_CALLS.isEmpty()) {
            int last = EXIT_CALLS.size() - 1;
            Object[] c = EXIT_CALLS.remove(last);
            String[] kw = EXIT_KW.remove(last);
            Object[] args = new Object[c.length - 1];
            System.arraycopy(c, 1, args, 0, args.length);
            try {
                Py.obj(c[0]).call(args, kw);
            } catch (PyException e) {
                System.err.println("Exception ignored in atexit callback " + Py.obj(c[0]).repr() + ":");
                System.err.println(e.getMessage());
            }
        }
    }

    private static PyModule atexit() {
        PyModule m = new PyModule("atexit", new String[0]);
        m.setAttr("register", new PyBuiltin("register", (args, kwNames) -> {
            if (args.length - kwNames.length < 1) {
                throw Py.typeError("register() takes at least 1 argument (0 given)");
            }
            if (!Builtins.isCallable(args[0])) {
                throw Py.typeError("the first argument must be callable");
            }
            Object[] call = new Object[args.length];
            System.arraycopy(args, 0, call, 0, args.length);
            EXIT_CALLS.add(call);
            EXIT_KW.add(kwNames);
            return args[0];
        }));
        m.setAttr("unregister", PyBuiltin.positional("unregister", a -> {
            for (int i = EXIT_CALLS.size() - 1; i >= 0; i--) {
                if (Py.isTrue(Py.eq(EXIT_CALLS.get(i)[0], a[0]))) {
                    EXIT_CALLS.remove(i);
                    EXIT_KW.remove(i);
                }
            }
            return Py.NONE;
        }));
        m.setAttr("_run_exitfuncs", PyBuiltin.positional("_run_exitfuncs", a -> {
            runExitHandlers();
            return Py.NONE;
        }));
        m.setAttr("_clear", PyBuiltin.positional("_clear", a -> {
            EXIT_CALLS.clear();
            EXIT_KW.clear();
            return Py.NONE;
        }));
        m.setAttr("_ncallbacks", PyBuiltin.positional("_ncallbacks", a -> PyInt.of(EXIT_CALLS.size())));
        return m;
    }

    /// `_weakref`: the reference type the `weakref` module builds on.
    private static PyModule weakref() {
        PyModule m = new PyModule("_weakref", new String[0]);
        PyType t = Py.WEAKREF_TYPE;
        if (t.factory == null) {
            t.define("__module__", "weakref");
            t.factory = (type, args, kw) -> {
                Args a = new Args("ref", args, kw).check(2);
                Object cb = a.get(1, "callback", Py.NONE);
                return new PyWeakRef(a.get(0, "object", null), cb == Py.NONE ? null : cb); //NOPMD CompareObjectsWithEquals - identity is meant
            };
        }
        m.setAttr("ref", t);
        m.setAttr("ReferenceType", t);
        m.setAttr("referenceable", PyBuiltin.positional("referenceable",
                a -> Py.bool(PyWeakRef.referenceable(a[0]))));
        return m;
    }

    /// Seconds east of UTC of the device's zone at instant `t` (seconds
    /// since the epoch), daylight saving included.
    private static int utcOffset(double t) {
        java.util.TimeZone z = java.util.TimeZone.getDefault();
        java.util.Calendar c = java.util.Calendar.getInstance(z);
        c.setTime(new java.util.Date((long) Math.floor(t * 1000)));
        int ms = ((c.get(java.util.Calendar.HOUR_OF_DAY) * 60 + c.get(java.util.Calendar.MINUTE)) * 60
                + c.get(java.util.Calendar.SECOND)) * 1000 + c.get(java.util.Calendar.MILLISECOND);
        return z.getOffset(1, c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH),
                c.get(java.util.Calendar.DAY_OF_MONTH), c.get(java.util.Calendar.DAY_OF_WEEK), ms) / 1000;
    }

    private interface FsOp {
        Object run(PyFileSystem fs, Object[] a) throws java.io.IOException;
    }

    private static void fs(PyModule m, String name, final FsOp op) {
        m.setAttr(name, PyBuiltin.positional(name, a -> {
            try {
                return op.run(PyFileSystem.get(), a);
            } catch (java.io.IOException e) {
                throw Py.error(Py.OS_ERROR, String.valueOf(e.getMessage())); //NOPMD PreserveStackTrace - a Python exception has no Java cause
            }
        }));
    }

    /// `_cn1fs`: the primitives `io` and `os` are written over. Paths are
    /// absolute; the Python side resolves them against the working
    /// directory and turns a missing file into the right exception.
    private static PyModule fileSystem() {
        PyModule m = new PyModule("_cn1fs", new String[0]);
        fs(m, "home", (fs, a) -> Py.str(fs.home()));
        fs(m, "read", (fs, a) -> {
            java.io.InputStream in = fs.openInput(Py.toStr(a[0]));
            try {
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                for (int n = in.read(buf); n > 0; n = in.read(buf)) {
                    out.write(buf, 0, n);
                }
                return new PyBytes(out.toByteArray());
            } finally {
                in.close();
            }
        });
        fs(m, "write", (fs, a) -> {
            java.io.OutputStream out = fs.openOutput(Py.toStr(a[0]), Py.isTrue(a[2]));
            try {
                out.write(PyBytes.like(a[1]));
            } finally {
                out.close();
            }
            return Py.NONE;
        });
        fs(m, "exists", (fs, a) -> Py.bool(fs.exists(Py.toStr(a[0]))));
        fs(m, "isdir", (fs, a) -> Py.bool(fs.isDirectory(Py.toStr(a[0]))));
        fs(m, "listdir", (fs, a) -> {
            String[] names = fs.list(Py.toStr(a[0]));
            Object[] r = new Object[names.length];
            for (int i = 0; i < names.length; i++) {
                r[i] = Py.str(names[i]);
            }
            return new PyList(r);
        });
        fs(m, "mkdir", (fs, a) -> {
            fs.mkdir(Py.toStr(a[0]));
            return Py.NONE;
        });
        fs(m, "remove", (fs, a) -> {
            fs.delete(Py.toStr(a[0]));
            return Py.NONE;
        });
        fs(m, "rename", (fs, a) -> {
            fs.rename(Py.toStr(a[0]), Py.toStr(a[1]));
            return Py.NONE;
        });
        fs(m, "size", (fs, a) -> PyInt.of(fs.size(Py.toStr(a[0]))));
        fs(m, "mtime", (fs, a) -> Py.ofFloat(fs.lastModified(Py.toStr(a[0])) / 1000.0));
        return m;
    }

    private interface D1 {
        double f(double x);
    }

    private static void fn1(PyModule m, String name, final D1 f) {
        m.setAttr(name, PyBuiltin.positional(name, a -> Py.ofFloat(f.f(Py.asDouble(a[0])))));
    }

    private static PyModule math() {
        PyModule m = new PyModule("math", new String[0]);
        m.setAttr("pi", Py.ofFloat(Math.PI));
        m.setAttr("e", Py.ofFloat(Math.E));
        MathFunctions.install(m);
        m.setAttr("tau", Py.ofFloat(2 * Math.PI));
        m.setAttr("inf", Py.ofFloat(Double.POSITIVE_INFINITY));
        m.setAttr("nan", Py.ofFloat(Double.NaN));
        m.setAttr("sqrt", PyBuiltin.positional("sqrt", a -> {
            double x = Py.asDouble(a[0]);
            if (x < 0) {
                throw Py.valueError("math domain error");
            }
            return Py.ofFloat(Math.sqrt(x));
        }));
        fn1(m, "sin", Math::sin);
        fn1(m, "cos", Math::cos);
        fn1(m, "tan", Math::tan);
        fn1(m, "asin", com.codename1.util.MathUtil::asin);
        fn1(m, "acos", com.codename1.util.MathUtil::acos);
        fn1(m, "atan", com.codename1.util.MathUtil::atan);
        fn1(m, "exp", com.codename1.util.MathUtil::exp);
        fn1(m, "fabs", Math::abs);
        fn1(m, "degrees", Math::toDegrees);
        fn1(m, "radians", Math::toRadians);
        m.setAttr("atan2", PyBuiltin.positional("atan2",
                a -> Py.ofFloat(com.codename1.util.MathUtil.atan2(Py.asDouble(a[0]), Py.asDouble(a[1])))));
        m.setAttr("pow", PyBuiltin.positional("pow",
                a -> Py.ofFloat(com.codename1.util.MathUtil.pow(Py.asDouble(a[0]), Py.asDouble(a[1])))));
        m.setAttr("hypot", PyBuiltin.positional("hypot", a -> {
            double s = 0;
            for (int i = 0; i < a.length; i++) {
                double v = Py.asDouble(a[i]);
                s += v * v;
            }
            return Py.ofFloat(Math.sqrt(s));
        }));
        m.setAttr("log", PyBuiltin.positional("log", a -> {
            double x = Py.asDouble(a[0]);
            if (x <= 0) {
                throw Py.valueError("math domain error");
            }
            double r = com.codename1.util.MathUtil.log(x);
            if (a.length > 1) {
                r /= com.codename1.util.MathUtil.log(Py.asDouble(a[1]));
            }
            return Py.ofFloat(r);
        }));
        m.setAttr("log10", PyBuiltin.positional("log10",
                a -> Py.ofFloat(com.codename1.util.MathUtil.log10(Py.asDouble(a[0])))));
        m.setAttr("log2", PyBuiltin.positional("log2",
                a -> Py.ofFloat(com.codename1.util.MathUtil.log(Py.asDouble(a[0])) / com.codename1.util.MathUtil.log(2))));
        m.setAttr("floor", PyBuiltin.positional("floor", a -> a[0] instanceof PyInt ? a[0]
                : Builtins.toInt(Py.ofFloat(Math.floor(Py.asDouble(a[0]))))));
        m.setAttr("ceil", PyBuiltin.positional("ceil", a -> a[0] instanceof PyInt ? a[0]
                : Builtins.toInt(Py.ofFloat(Math.ceil(Py.asDouble(a[0]))))));
        m.setAttr("trunc", PyBuiltin.positional("trunc", a -> Builtins.toInt(a[0])));
        m.setAttr("isnan", PyBuiltin.positional("isnan", a -> Py.bool(Double.isNaN(Py.asDouble(a[0])))));
        m.setAttr("isinf", PyBuiltin.positional("isinf", a -> Py.bool(Double.isInfinite(Py.asDouble(a[0])))));
        m.setAttr("isfinite", PyBuiltin.positional("isfinite", a -> {
            double d = Py.asDouble(a[0]);
            return Py.bool(!Double.isNaN(d) && !Double.isInfinite(d));
        }));
        m.setAttr("copysign", PyBuiltin.positional("copysign", a -> {
            double x = Math.abs(Py.asDouble(a[0]));
            double s = Py.asDouble(a[1]);
            return Py.ofFloat(s < 0 || s == 0 && 1 / s < 0 ? -x : x);
        }));
        m.setAttr("fmod", PyBuiltin.positional("fmod", a -> Py.ofFloat(Py.asDouble(a[0]) % Py.asDouble(a[1]))));
        m.setAttr("gcd", PyBuiltin.positional("gcd", a -> {
            com.codename1.util.BigInteger g = com.codename1.util.BigInteger.ZERO;
            for (int i = 0; i < a.length; i++) {
                g = g.gcd(Py.intView(Builtins.toInt(a[i])).toBig());
            }
            return PyInt.of(g);
        }));
        m.setAttr("factorial", PyBuiltin.positional("factorial", a -> {
            long n = Py.asLong(a[0]);
            if (n < 0) {
                throw Py.valueError("factorial() not defined for negative values");
            }
            Object r = PyInt.of(1);
            for (long i = 2; i <= n; i++) {
                r = Py.mul(r, PyInt.of(i));
            }
            return r;
        }));
        m.setAttr("isqrt", PyBuiltin.positional("isqrt", a -> {
            com.codename1.util.BigInteger n = Py.intView(Builtins.toInt(a[0])).toBig();
            if (n.signum() < 0) {
                throw Py.valueError("isqrt() argument must be nonnegative");
            }
            if (n.signum() == 0) {
                return PyInt.of(0);
            }
            com.codename1.util.BigInteger x = com.codename1.util.BigInteger.ONE.shiftLeft((n.bitLength() + 1) / 2);
            while (true) {
                com.codename1.util.BigInteger y = x.add(n.divide(x)).shiftRight(1);
                if (y.compareTo(x) >= 0) {
                    return PyInt.of(x);
                }
                x = y;
            }
        }));
        m.setAttr("fsum", PyBuiltin.positional("fsum", a -> Py.obj(Builtins.SUM).call(new Object[] {a[0], Py.ofFloat(0)}, Py.NO_KW)));
        m.setAttr("prod", PyBuiltin.positional("prod", a -> {
            Object r = PyInt.of(1);
            Object it = Py.iter(a[0]);
            for (Object v = Py.next(it); v != null; v = Py.next(it)) {
                r = Py.mul(r, v);
            }
            return r;
        }));
        m.setAttr("comb", PyBuiltin.positional("comb", a -> {
            long n = Py.asLong(a[0]);
            long k = Py.asLong(a[1]);
            if (k < 0 || k > n) {
                return PyInt.of(0);
            }
            k = Math.min(k, n - k);
            Object r = PyInt.of(1);
            for (long i = 1; i <= k; i++) {
                r = Py.floordiv(Py.mul(r, PyInt.of(n - k + i)), PyInt.of(i));
            }
            return r;
        }));
        m.setAttr("isclose", new PyBuiltin("isclose", (args, kw) -> {
            Args a = new Args("isclose", args, kw).check(2, "rel_tol", "abs_tol");
            double x = Py.asDouble(args[0]);
            double y = Py.asDouble(args[1]);
            double rel = Py.asDouble(a.get(-1, "rel_tol", Py.ofFloat(1e-9)));
            double abs = Py.asDouble(a.get(-1, "abs_tol", Py.ofFloat(0)));
            if (x == y) {
                return Py.TRUE;
            }
            double diff = Math.abs(x - y);
            return Py.bool(diff <= Math.abs(rel * y) || diff <= Math.abs(rel * x) || diff <= abs);
        }));
        return m;
    }

    private static PyModule sys() {
        PyModule m = new PyModule("sys", new String[0]);
        m.setAttr("maxsize", PyInt.of(Long.MAX_VALUE));
        m.setAttr("platform", Py.str("codenameone"));
        m.setAttr("byteorder", Py.str("little"));
        m.setAttr("version", Py.str("3.13.0 (Codename One)"));
        m.setAttr("version_info", PyTuple.of(PyInt.of(3), PyInt.of(13), PyInt.of(0), Py.str("final"), PyInt.of(0)));
        Object[] argv = new Object[Py.argv.length];
        for (int i = 0; i < argv.length; i++) {
            argv[i] = Py.argv[i];
        }
        m.setAttr("argv", new PyList(argv));
        m.setAttr("path", new PyList());
        m.setAttr("modules", PyModule.SYS_MODULES);
        m.setAttr("float_info", PyTuple.of(Py.ofFloat(Double.MAX_VALUE), PyInt.of(1024), PyInt.of(308),
                Py.ofFloat(Double.MIN_NORMAL), PyInt.of(-1021), PyInt.of(-307), PyInt.of(15), PyInt.of(53),
                Py.ofFloat(Math.ulp(1.0)), PyInt.of(2), PyInt.of(1)));
        m.setAttr("getrecursionlimit", PyBuiltin.positional("getrecursionlimit", a -> PyInt.of(1000)));
        m.setAttr("setrecursionlimit", PyBuiltin.positional("setrecursionlimit", a -> Py.NONE));
        m.setAttr("exception", PyBuiltin.positional("exception", a -> Py.handling()));
        m.setAttr("exc_info", PyBuiltin.positional("exc_info", a -> {
            Object e = Py.handling();
            if (e == Py.NONE) { //NOPMD CompareObjectsWithEquals - identity is meant
                return PyTuple.of(Py.NONE, Py.NONE, Py.NONE);
            }
            return PyTuple.of(Py.typeOf(e), e, Py.NONE);
        }));
        m.setAttr("intern", PyBuiltin.positional("intern", a -> a[0]));
        m.setAttr("exit", PyBuiltin.positional("exit", a -> {
            throw new PyException(Py.SYSTEM_EXIT.call(a, Py.NO_KW));
        }));
        Object stdout = new PyInstance(Py.OBJECT_TYPE) {
            @Override
            public Object getAttrOrNull(String name) {
                if ("write".equals(name)) {
                    return PyBuiltin.positional("write", a -> {
                        Builtins.writeOut(Py.toStr(a[0]));
                        return PyInt.of(Py.len(a[0]));
                    });
                }
                if ("flush".equals(name)) {
                    return PyBuiltin.positional("flush", a -> Py.NONE);
                }
                return super.getAttrOrNull(name);
            }
        };
        Object stderr = new PyInstance(Py.OBJECT_TYPE) {
            @Override
            public Object getAttrOrNull(String name) {
                if ("write".equals(name)) {
                    return PyBuiltin.positional("write", a -> {
                        System.err.print(Py.toStr(a[0]));
                        return PyInt.of(Py.len(a[0]));
                    });
                }
                if ("flush".equals(name)) {
                    return PyBuiltin.positional("flush", a -> Py.NONE);
                }
                return super.getAttrOrNull(name);
            }
        };
        m.setAttr("stdout", stdout);
        m.setAttr("stderr", stderr);
        m.setAttr("__stdout__", stdout);
        m.setAttr("__stderr__", stderr);
        sysModule = m;
        originalStdout = stdout;
        return m;
    }

    /// The sys module once something imported it, and the stdout it started with.
    private static PyModule sysModule;
    private static Object originalStdout;

    /// `sys.stdout` when the program replaced it, else null (print writes
    /// to the platform output directly).
    static Object redirectedStdout() {
        if (sysModule == null) {
            return null;
        }
        Object so = sysModule.getAttrOrNull("stdout");
        return so == null || so == originalStdout || so == Py.NONE ? null : so; //NOPMD CompareObjectsWithEquals - identity is meant
    }

    private static PyModule time() {
        PyModule m = new PyModule("_time", new String[0]);
        m.setAttr("utcoffset", PyBuiltin.positional("utcoffset", a -> PyInt.of(utcOffset(Py.asDouble(a[0])))));
        m.setAttr("tzname", PyBuiltin.positional("tzname", a -> Py.str(java.util.TimeZone.getDefault().getID())));
        m.setAttr("time", PyBuiltin.positional("time", a -> Py.ofFloat(System.currentTimeMillis() / 1000.0)));
        m.setAttr("time_ns", PyBuiltin.positional("time_ns", a -> PyInt.of(System.currentTimeMillis() * 1000000L)));
        m.setAttr("perf_counter", PyBuiltin.positional("perf_counter", a -> Py.ofFloat(System.nanoTime() / 1e9)));
        m.setAttr("perf_counter_ns", PyBuiltin.positional("perf_counter_ns", a -> PyInt.of(System.nanoTime())));
        m.setAttr("monotonic", PyBuiltin.positional("monotonic", a -> Py.ofFloat(System.nanoTime() / 1e9)));
        m.setAttr("sleep", PyBuiltin.positional("sleep", a -> {
            try {
                Thread.sleep((long) (Py.asDouble(a[0]) * 1000));
            } catch (InterruptedException e) {
                throw Py.error(Py.KEYBOARD_INTERRUPT, ""); //NOPMD PreserveStackTrace - a Python exception has no Java cause
            }
            return Py.NONE;
        }));
        return m;
    }
}
