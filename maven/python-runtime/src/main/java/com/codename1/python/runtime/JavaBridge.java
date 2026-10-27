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

/// The Java classes a Python program uses, reached without reflection.
///
/// The transpiler generates one subclass per program, `JavaInterop`, from
/// the class files on the build's class path. It answers only for the
/// classes the program imports, the classes their methods hand back, and
/// only for the member names the program spells, so nothing it does not use
/// is kept alive. Classes are numbered in that generated class; a
/// [PyJavaClass] carries its number.
///
/// Every method returns `null` for a member it does not have, and the
/// caller raises the Python error.
public abstract class JavaBridge {
    private static JavaBridge current;

    /// Installs the program's bridge; the generated `PythonModules` does.
    public static void install(JavaBridge bridge) {
        current = bridge;
    }

    static JavaBridge get() {
        if (current == null) {
            throw Py.runtimeError("no Java classes were bridged for this program");
        }
        return current;
    }

    /// `Cls(*args)`.
    public abstract Object construct(int cls, Object[] args);

    /// `Cls.name`: a static field's value, or null.
    public abstract Object getStatic(int cls, String name);

    /// Whether `Cls` has a static method `name`.
    public abstract boolean hasStatic(int cls, String name);

    /// `Cls.name(*args)` for a static method.
    public abstract Object callStatic(int cls, String name, Object[] args);

    /// `obj.name`: a public instance field's value, or null.
    public abstract Object getField(Object target, String name);

    /// Whether `obj` has an instance method `name`.
    public abstract boolean hasMethod(Object target, String name);

    /// `obj.name(*args)`.
    public abstract Object invoke(Object target, String name, Object[] args);

    /// `isinstance(obj, Cls)`.
    public abstract boolean isInstance(int cls, Object value);

    /// The Python name of `Cls`: its simple name.
    public abstract String className(int cls);

    /// The [PyJavaClass] numbered `cls`, made once.
    public final PyJavaClass type(int cls) {
        return PyJavaClass.of(this, cls);
    }

    // ---- conversions the generated code calls ---------------------------

    /// A Java value as Python sees it: boxed numbers as int and float, a
    /// char as a one-character str, null as None, and any other object
    /// wrapped as a [PyJavaObject] -- unless it is already a Python value.
    public static Object toPy(Object v) {
        if (v == null) {
            return Py.NONE;
        }
        if (v instanceof String || v instanceof PyObject || v instanceof Long || v instanceof Double) {
            return v;
        }
        if (v instanceof Integer || v instanceof Short || v instanceof Byte) {
            return PyInt.of(((Number) v).longValue());
        }
        if (v instanceof Float) {
            return Py.ofFloat(((Float) v).doubleValue());
        }
        if (v instanceof Boolean) {
            return Py.bool(((Boolean) v).booleanValue());
        }
        if (v instanceof Character) {
            return String.valueOf(((Character) v).charValue());
        }
        return new PyJavaObject(v);
    }

    /// A Python value handed to a parameter of type `Object`: a wrapped
    /// object unwrapped, None as null, an int as an Integer when it fits and
    /// a Long when not, a bool as a Boolean; anything else as it is.
    public static Object toJava(Object v) {
        if (v == Py.NONE) { //NOPMD CompareObjectsWithEquals - identity is meant
            return null;
        }
        if (v instanceof PyJavaObject) {
            return ((PyJavaObject) v).value;
        }
        if (v instanceof PyBool) {
            return Boolean.valueOf(v == Py.TRUE); //NOPMD CompareObjectsWithEquals - identity is meant
        }
        if (v instanceof Long) {
            long l = ((Long) v).longValue();
            if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
                return Integer.valueOf((int) l);
            }
            return v;
        }
        if (v instanceof PyObject) {
            return ((PyObject) v).toJava();
        }
        return v;
    }

    /// A Python list or tuple handed to a `java.util` collection parameter:
    /// a new `ArrayList` of its elements, so the Java side's changes are not
    /// seen by Python. A Java collection is passed as it is.
    public static Object toJavaList(Object v) {
        Object j = toJava(v);
        if (v instanceof PyList) {
            PyList l = (PyList) v;
            java.util.ArrayList r = new java.util.ArrayList(l.size());
            for (int i = 0; i < l.size(); i++) {
                r.add(toJava(l.get(i)));
            }
            return r;
        }
        if (v instanceof PyTuple) {
            PyTuple t = (PyTuple) v;
            java.util.ArrayList r = new java.util.ArrayList(t.size());
            for (int i = 0; i < t.size(); i++) {
                r.add(toJava(t.get(i)));
            }
            return r;
        }
        return j;
    }

    /// A Python dict handed to a `java.util.Map` parameter: a new
    /// `LinkedHashMap` in the dict's order.
    public static Object toJavaMap(Object v) {
        if (v instanceof PyDict) {
            PyDict d = (PyDict) v;
            java.util.LinkedHashMap r = new java.util.LinkedHashMap();
            for (Object k : d.keyArray()) {
                r.put(toJava(k), toJava(d.getItem(k)));
            }
            return r;
        }
        return toJava(v);
    }

    /// Whether `v` can be made a `java.util` list.
    public static boolean isSequence(Object v) {
        return v instanceof PyList || v instanceof PyTuple;
    }

    /// A Java exception thrown into Python code, as a `RuntimeError` naming
    /// the Java class and message.
    public static PyException javaError(Exception e) {
        return Py.runtimeError(e.toString());
    }

    /// Whether `v` can go to a parameter of reference type `t`, whose
    /// instance test the generated code made: None always can.
    public static boolean isNone(Object v) {
        return v == Py.NONE; //NOPMD CompareObjectsWithEquals - identity is meant
    }

    /// The Java object `v` wraps, or `v` itself.
    public static Object unwrap(Object v) {
        return v instanceof PyJavaObject ? ((PyJavaObject) v).value : v;
    }

    /// Whether `v` fits an `int`, `long`, `short` or `byte` parameter.
    public static boolean isInt(Object v) {
        return Py.isInt(v) || v instanceof PyBool;
    }

    /// Whether `v` fits a `double` or `float` parameter.
    public static boolean isReal(Object v) {
        return Py.isInt(v) || Py.isFloat(v) || v instanceof PyBool;
    }

    /// Whether `v` fits a `char` parameter: a one-character str.
    public static boolean isChar(Object v) {
        return v instanceof String && ((String) v).length() == 1;
    }

    /// Whether `v` is a Python callable that can stand for a functional
    /// interface: not a Java object, not a class.
    public static boolean isFunction(Object v) {
        return !(v instanceof PyJavaObject) && !(v instanceof PyJavaClass) && !(v instanceof PyType)
                && Builtins.isCallable(v);
    }

    public static long asLong(Object v) {
        return v instanceof PyBool ? (v == Py.TRUE ? 1 : 0) : Py.asLong(v); //NOPMD CompareObjectsWithEquals - identity is meant
    }

    public static double asDouble(Object v) {
        return v instanceof PyBool ? (v == Py.TRUE ? 1 : 0) : Py.asDouble(v); //NOPMD CompareObjectsWithEquals - identity is meant
    }

    public static boolean asBoolean(Object v) {
        return Py.isTrue(v);
    }

    public static char asChar(Object v) {
        return ((String) v).charAt(0);
    }

    /// Calls a Python function from a functional interface adapter.
    public static Object callPy(Object fn, Object[] args) {
        return Py.obj(fn).call(args, Py.NO_KW);
    }

    /// The error for a call no overload accepts.
    public static PyException noOverload(String owner, String name, Object[] args, String signatures) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(args[i] instanceof PyJavaObject ? ((PyJavaObject) args[i]).javaClassName()
                    : Py.typeOf(args[i]).name);
        }
        return Py.typeError(owner + "." + name + "() got (" + sb + "); it takes " + signatures);
    }
}
