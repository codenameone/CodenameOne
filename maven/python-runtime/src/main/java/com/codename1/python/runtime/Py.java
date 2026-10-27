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

/// The operations transpiled code calls: operators with CPython's dispatch
/// rules, attribute access, calls, iteration helpers, and the builtin types
/// and exceptions.
///
/// Everything here is static and the transpiler emits direct calls, so the
/// common cases (`int + int`, `list[i]`) are a type test and an inlined
/// virtual call away from the arithmetic.
public final class Py {
    public static final Object[] NO_ARGS = new Object[0];
    public static final String[] NO_KW = new String[0];

    // ---- types -----------------------------------------------------------
    // Order matters: `object` and `type` first, every other type derives.

    public static final PyType OBJECT_TYPE = new PyType("object", new PyType[0], null);
    public static final PyType TYPE_TYPE = builtinType("type", null);
    public static final PyType NONE_TYPE = builtinType("NoneType", null);
    public static final PyType INT_TYPE = builtinType("int", null);
    public static final PyType BOOL_TYPE = new PyType("bool", new PyType[] {INT_TYPE}, null);
    public static final PyType FLOAT_TYPE = builtinType("float", null);
    public static final PyType COMPLEX_TYPE = builtinType("complex", null);
    public static final PyType STR_TYPE = builtinType("str", null);
    public static final PyType BYTES_TYPE = builtinType("bytes", null);
    public static final PyType BYTEARRAY_TYPE = builtinType("bytearray", null);
    public static final PyType MEMORYVIEW_TYPE = builtinType("memoryview", null);
    public static final PyType WEAKREF_TYPE = builtinType("ReferenceType", null);
    public static final PyType LIST_TYPE = builtinType("list", null);
    public static final PyType TUPLE_TYPE = builtinType("tuple", null);
    public static final PyType DICT_TYPE = builtinType("dict", null);
    public static final PyType SET_TYPE = builtinType("set", null);
    public static final PyType FROZENSET_TYPE = builtinType("frozenset", null);
    public static final PyType RANGE_TYPE = builtinType("range", null);
    public static final PyType SLICE_TYPE = builtinType("slice", null);
    public static final PyType FUNCTION_TYPE = builtinType("function", null);
    public static final PyType BUILTIN_TYPE = builtinType("builtin_function_or_method", null);
    public static final PyType METHOD_TYPE = builtinType("method", null);
    public static final PyType PROPERTY_TYPE = builtinType("property", null);
    public static final PyType CLASSMETHOD_TYPE = builtinType("classmethod", null);
    public static final PyType STATICMETHOD_TYPE = builtinType("staticmethod", null);
    public static final PyType ITERATOR_TYPE = builtinType("iterator", null);
    public static final PyType GENERATOR_TYPE = builtinType("generator", null);
    public static final PyType COROUTINE_TYPE = builtinType("coroutine", null);
    public static final PyType ASYNC_GENERATOR_TYPE = builtinType("async_generator", null);
    public static final PyType MODULE_TYPE = builtinType("module", null);
    public static final PyType NOT_IMPLEMENTED_TYPE = builtinType("NotImplementedType", null);
    public static final PyType ELLIPSIS_TYPE = builtinType("ellipsis", null);

    // ---- singletons ------------------------------------------------------

    public static final Object NONE = new PyNone("None", null);
    public static final Object NOT_IMPLEMENTED = new PyNone("NotImplemented", NOT_IMPLEMENTED_TYPE);
    public static final Object ELLIPSIS = new PyNone("Ellipsis", ELLIPSIS_TYPE);
    public static final PyBool TRUE = new PyBool(true);
    public static final PyBool FALSE = new PyBool(false);
    public static final String EMPTY_STR = "";
    static final String[] ASCII = new String[128];

    // ---- exceptions ------------------------------------------------------

    /// Every builtin exception type, in declaration order, for the builtins table.
    static final List<PyType> EXCEPTION_TYPES = new ArrayList<PyType>();
    public static final PyType BASE_EXCEPTION = exceptionType("BaseException", OBJECT_TYPE);
    public static final PyType SYSTEM_EXIT = exceptionType("SystemExit", BASE_EXCEPTION);
    public static final PyType KEYBOARD_INTERRUPT = exceptionType("KeyboardInterrupt", BASE_EXCEPTION);
    public static final PyType GENERATOR_EXIT = exceptionType("GeneratorExit", BASE_EXCEPTION);
    public static final PyType EXCEPTION = exceptionType("Exception", BASE_EXCEPTION);
    public static final PyType BASE_EXCEPTION_GROUP = exceptionType("BaseExceptionGroup", BASE_EXCEPTION);
    public static final PyType EXCEPTION_GROUP = exceptionGroupType();
    public static final PyType STOP_ITERATION = exceptionType("StopIteration", EXCEPTION);
    public static final PyType ARITHMETIC_ERROR = exceptionType("ArithmeticError", EXCEPTION);
    public static final PyType ZERO_DIVISION_ERROR = exceptionType("ZeroDivisionError", ARITHMETIC_ERROR);
    public static final PyType OVERFLOW_ERROR = exceptionType("OverflowError", ARITHMETIC_ERROR);
    public static final PyType ASSERTION_ERROR = exceptionType("AssertionError", EXCEPTION);
    public static final PyType ATTRIBUTE_ERROR = exceptionType("AttributeError", EXCEPTION);
    public static final PyType LOOKUP_ERROR = exceptionType("LookupError", EXCEPTION);
    public static final PyType INDEX_ERROR = exceptionType("IndexError", LOOKUP_ERROR);
    public static final PyType KEY_ERROR = exceptionType("KeyError", LOOKUP_ERROR);
    public static final PyType NAME_ERROR = exceptionType("NameError", EXCEPTION);
    public static final PyType UNBOUND_LOCAL_ERROR = exceptionType("UnboundLocalError", NAME_ERROR);
    public static final PyType RUNTIME_ERROR = exceptionType("RuntimeError", EXCEPTION);
    public static final PyType NOT_IMPLEMENTED_ERROR = exceptionType("NotImplementedError", RUNTIME_ERROR);
    public static final PyType RECURSION_ERROR = exceptionType("RecursionError", RUNTIME_ERROR);
    public static final PyType TYPE_ERROR = exceptionType("TypeError", EXCEPTION);
    public static final PyType VALUE_ERROR = exceptionType("ValueError", EXCEPTION);
    public static final PyType UNICODE_ERROR = exceptionType("UnicodeError", VALUE_ERROR);
    public static final PyType IMPORT_ERROR = exceptionType("ImportError", EXCEPTION);
    public static final PyType MODULE_NOT_FOUND_ERROR = exceptionType("ModuleNotFoundError", IMPORT_ERROR);
    public static final PyType OS_ERROR = exceptionType("OSError", EXCEPTION);
    public static final PyType FILE_NOT_FOUND_ERROR = exceptionType("FileNotFoundError", OS_ERROR);
    public static final PyType MEMORY_ERROR = exceptionType("MemoryError", EXCEPTION);
    public static final PyType UNICODE_ENCODE_ERROR = exceptionType("UnicodeEncodeError", UNICODE_ERROR);
    public static final PyType UNICODE_DECODE_ERROR = exceptionType("UnicodeDecodeError", UNICODE_ERROR);
    public static final PyType UNICODE_TRANSLATE_ERROR = exceptionType("UnicodeTranslateError", UNICODE_ERROR);
    public static final PyType FLOATING_POINT_ERROR = exceptionType("FloatingPointError", ARITHMETIC_ERROR);
    public static final PyType STOP_ASYNC_ITERATION = exceptionType("StopAsyncIteration", EXCEPTION);
    public static final PyType EOF_ERROR = exceptionType("EOFError", EXCEPTION);
    public static final PyType BUFFER_ERROR = exceptionType("BufferError", EXCEPTION);
    public static final PyType REFERENCE_ERROR = exceptionType("ReferenceError", EXCEPTION);
    public static final PyType SYSTEM_ERROR = exceptionType("SystemError", EXCEPTION);
    public static final PyType SYNTAX_ERROR = exceptionType("SyntaxError", EXCEPTION);
    public static final PyType INDENTATION_ERROR = exceptionType("IndentationError", SYNTAX_ERROR);
    public static final PyType TAB_ERROR = exceptionType("TabError", INDENTATION_ERROR);
    public static final PyType PYTHON_FINALIZATION_ERROR = exceptionType("PythonFinalizationError", RUNTIME_ERROR);
    public static final PyType FILE_EXISTS_ERROR = exceptionType("FileExistsError", OS_ERROR);
    public static final PyType PERMISSION_ERROR = exceptionType("PermissionError", OS_ERROR);
    public static final PyType IS_A_DIRECTORY_ERROR = exceptionType("IsADirectoryError", OS_ERROR);
    public static final PyType NOT_A_DIRECTORY_ERROR = exceptionType("NotADirectoryError", OS_ERROR);
    public static final PyType TIMEOUT_ERROR = exceptionType("TimeoutError", OS_ERROR);
    public static final PyType INTERRUPTED_ERROR = exceptionType("InterruptedError", OS_ERROR);
    public static final PyType BLOCKING_IO_ERROR = exceptionType("BlockingIOError", OS_ERROR);
    public static final PyType CHILD_PROCESS_ERROR = exceptionType("ChildProcessError", OS_ERROR);
    public static final PyType PROCESS_LOOKUP_ERROR = exceptionType("ProcessLookupError", OS_ERROR);
    public static final PyType CONNECTION_ERROR = exceptionType("ConnectionError", OS_ERROR);
    public static final PyType BROKEN_PIPE_ERROR = exceptionType("BrokenPipeError", CONNECTION_ERROR);
    public static final PyType CONNECTION_ABORTED_ERROR = exceptionType("ConnectionAbortedError", CONNECTION_ERROR);
    public static final PyType CONNECTION_REFUSED_ERROR = exceptionType("ConnectionRefusedError", CONNECTION_ERROR);
    public static final PyType CONNECTION_RESET_ERROR = exceptionType("ConnectionResetError", CONNECTION_ERROR);
    public static final PyType WARNING = exceptionType("Warning", EXCEPTION);
    public static final PyType USER_WARNING = exceptionType("UserWarning", WARNING);
    public static final PyType DEPRECATION_WARNING = exceptionType("DeprecationWarning", WARNING);
    public static final PyType PENDING_DEPRECATION_WARNING = exceptionType("PendingDeprecationWarning", WARNING);
    public static final PyType SYNTAX_WARNING = exceptionType("SyntaxWarning", WARNING);
    public static final PyType RUNTIME_WARNING = exceptionType("RuntimeWarning", WARNING);
    public static final PyType FUTURE_WARNING = exceptionType("FutureWarning", WARNING);
    public static final PyType IMPORT_WARNING = exceptionType("ImportWarning", WARNING);
    public static final PyType UNICODE_WARNING = exceptionType("UnicodeWarning", WARNING);
    public static final PyType BYTES_WARNING = exceptionType("BytesWarning", WARNING);
    public static final PyType RESOURCE_WARNING = exceptionType("ResourceWarning", WARNING);
    public static final PyType ENCODING_WARNING = exceptionType("EncodingWarning", WARNING);

    private static PyType builtinType(String name, PyType.Factory f) {
        return new PyType(name, new PyType[] {OBJECT_TYPE}, f);
    }

    private static PyType exceptionGroupType() {
        PyType t = new PyType("ExceptionGroup", new PyType[] {BASE_EXCEPTION_GROUP, EXCEPTION}, null);
        EXCEPTION_TYPES.add(t);
        return t;
    }

    private static PyType exceptionType(String name, PyType base) {
        PyType t = new PyType(name, new PyType[] {base}, null);
        EXCEPTION_TYPES.add(t);
        return t;
    }

    static {
        for (int i = 0; i < ASCII.length; i++) {
            ASCII[i] = String.valueOf((char) i);
        }
        Builtins.install();
    }

    private Py() {
    }

    // ---- value representation -------------------------------------------
    //
    // A Python value is a Java Object. An int that fits in a long is a
    // java.lang.Long and a float is a java.lang.Double; every other value
    // (bool, big ints, str, containers, instances) is a PyObject. ParparVM
    // keeps a Long within [-2^60, 2^60) and many Doubles as tagged immediates
    // that never reach the heap, so int and float arithmetic does not
    // allocate there. PyInt and PyFloat remain the implementations of int and
    // float behaviour: obj() hands a Long or Double to them as a short-lived
    // view on the paths that are not worth a fast path of their own.

    /// The PyObject behaviour of any value.
    public static PyObject obj(Object o) {
        if (o instanceof PyObject) {
            return (PyObject) o;
        }
        if (o instanceof Long) {
            return PyInt.view(((Long) o).longValue());
        }
        if (o instanceof Double) {
            return new PyFloat(((Double) o).doubleValue());
        }
        if (o instanceof String) {
            return new PyStr((String) o);
        }
        if (o == null) {
            throw new NullPointerException("null Python value");
        }
        return new PyJavaObject(o);
    }

    /// `attr`, found on a class, as read through `self`: a function binds,
    /// and a value the runtime holds as a plain Java object (an int, a float,
    /// a str) is returned as it is, never as the transient view [#obj] makes.
    public static Object bindAttr(Object attr, Object self) {
        if (attr instanceof PyProperty) {
            return ((PyProperty) attr).get(self);
        }
        return attr instanceof PyObject ? ((PyObject) attr).bind(self) : attr;
    }

    /// True for a Python int, bool included.
    public static boolean isInt(Object o) {
        return o instanceof Long || o instanceof PyInt;
    }

    public static PyType typeOf(Object o) {
        if (o instanceof Long) {
            return INT_TYPE;
        }
        if (o instanceof Double) {
            return FLOAT_TYPE;
        }
        if (o instanceof String) {
            return STR_TYPE;
        }
        return obj(o).type();
    }

    public static long asLong(Object o) {
        if (o instanceof Long) {
            return ((Long) o).longValue();
        }
        return obj(o).asLong();
    }

    public static double asDouble(Object o) {
        if (o instanceof Double) {
            return ((Double) o).doubleValue();
        }
        if (o instanceof Long) {
            return ((Long) o).longValue();
        }
        return obj(o).asDouble();
    }

    public static String toStr(Object o) {
        if (o instanceof Long) {
            return Long.toString(((Long) o).longValue());
        }
        if (o instanceof String) {
            return (String) o;
        }
        return obj(o).str();
    }

    public static String toRepr(Object o) {
        if (o instanceof Long) {
            return Long.toString(((Long) o).longValue());
        }
        if (o instanceof String) {
            return PyStr.repr((String) o);
        }
        return obj(o).repr();
    }

    public static long hashOf(Object o) {
        if (o instanceof Long) {
            return Hashing.ofLong(((Long) o).longValue());
        }
        if (o instanceof String) {
            return o.hashCode();
        }
        return obj(o).hash();
    }

    /// CPython's error for `T.__new__(x)` when `x` is not a type.
    static PyException notATypeObject(String owner, Object[] args) {
        if (args.length == 0) {
            return typeError(owner + ".__new__(): not enough arguments");
        }
        return typeError(owner + ".__new__(X): X is not a type object (" + typeOf(args[0]).name + ")");
    }

    /// The hash of a dict key (`role` "a dict key") or a set element: an
    /// unhashable one is named as such, as CPython does.
    static long keyHash(Object o, String role) {
        if (o instanceof Long) {
            return Hashing.ofLong(((Long) o).longValue());
        }
        if (o instanceof String) {
            return o.hashCode();
        }
        try {
            return obj(o).hash();
        } catch (PyException e) {
            String m = e.matches(TYPE_ERROR) ? toStr(e.value) : "";
            if (m.startsWith("unhashable type: ")) {
                throw typeError("cannot use '" + typeOf(o).name + "' as " + role + " (" + m + ")"); //NOPMD PreserveStackTrace - CPython's error has no cause
            }
            throw e;
        }
    }

    /// `f(o)` where `f` is what the name `len` holds: the builtin's work
    /// without a call when it is the builtin.
    public static Object len(Object f, Object o) {
        if (f == Builtins.LEN) { //NOPMD CompareObjectsWithEquals - identity is meant
            return PyInt.of(len(o));
        }
        return call(f, o);
    }

    public static int len(Object o) {
        if (o instanceof PyObject) {
            return ((PyObject) o).len();
        }
        if (o instanceof String) {
            return PyStr.codePoints((String) o);
        }
        return obj(o).len();
    }

    // ---- boxing ----------------------------------------------------------

    public static PyBool bool(boolean b) {
        return b ? TRUE : FALSE;
    }

    public static Object of(long v) {
        return Long.valueOf(v);
    }

    public static Object of(double v) {
        return Double.valueOf(v);
    }

    public static Object ofFloat(double v) {
        return Double.valueOf(v);
    }

    /// True for a Python float.
    public static boolean isFloat(Object o) {
        return o instanceof Double || o instanceof PyFloat;
    }

    /// The PyInt behaviour of an int (callers have checked [#isInt(Object)]).
    public static PyInt intView(Object o) {
        if (o instanceof PyInt) {
            return (PyInt) o;
        }
        return PyInt.view(((Long) o).longValue());
    }

    /// A Python str: the Java string itself.
    public static String str(String s) {
        return s;
    }

    /// The one-character string `c`, shared for ASCII as CPython shares it.
    public static String chr(char c) {
        return c < 128 ? ASCII[c] : String.valueOf(c);
    }

    /// Converts a Java value to its Python counterpart: the reverse of
    /// [PyObject#toJava()]. Unknown Java objects are wrapped as [PyJavaObject].
    public static Object wrap(Object o) {
        if (o == null) {
            return NONE;
        }
        if (o instanceof PyObject) {
            return (PyObject) o;
        }
        if (o instanceof String) {
            return str((String) o);
        }
        if (o instanceof Boolean) {
            return bool(((Boolean) o).booleanValue());
        }
        if (o instanceof Long || o instanceof Double) {
            return o;
        }
        if (o instanceof Integer || o instanceof Short || o instanceof Byte) {
            return PyInt.of(((Number) o).longValue());
        }
        if (o instanceof Float) {
            return Py.ofFloat(((Float) o).doubleValue());
        }
        if (o instanceof com.codename1.util.BigInteger) {
            return PyInt.of((com.codename1.util.BigInteger) o);
        }
        if (o instanceof Character) {
            return str(String.valueOf(((Character) o).charValue()));
        }
        return new PyJavaObject(o);
    }

    // ---- errors ----------------------------------------------------------

    /// Instantiates exception type `t` with `msg` and wraps it for `throw`.
    public static PyException error(PyType t, String msg) {
        return new PyException(t.call(new Object[] {str(msg)}, NO_KW));
    }

    public static PyException typeError(String m) {
        return error(TYPE_ERROR, m);
    }

    public static PyException valueError(String m) {
        return error(VALUE_ERROR, m);
    }

    public static PyException indexError(String m) {
        return error(INDEX_ERROR, m);
    }

    public static PyException attributeError(String m) {
        return error(ATTRIBUTE_ERROR, m);
    }

    public static PyException zeroDivision(String m) {
        return error(ZERO_DIVISION_ERROR, m);
    }

    public static PyException overflowError(String m) {
        return error(OVERFLOW_ERROR, m);
    }

    public static PyException runtimeError(String m) {
        return error(RUNTIME_ERROR, m);
    }

    public static PyException nameError(String name) {
        return error(NAME_ERROR, "name '" + name + "' is not defined");
    }

    public static PyException unboundLocal(String name) {
        return error(UNBOUND_LOCAL_ERROR, "cannot access local variable '" + name
                + "' where it is not associated with a value");
    }

    public static PyException keyError(Object key) {
        return new PyException(KEY_ERROR.call(new Object[] {key}, NO_KW));
    }

    /// `raise x`: `x` is an exception instance or class.
    public static PyException raise(Object x) {
        if (x instanceof PyType) {
            PyType t = (PyType) x;
            if (!t.isSubtype(BASE_EXCEPTION)) {
                return typeError("exceptions must derive from BaseException");
            }
            x = t.call(NO_ARGS, NO_KW);
        } else if (!Py.typeOf(x).isSubtype(BASE_EXCEPTION)) {
            return typeError("exceptions must derive from BaseException");
        }
        return new PyException(x);
    }

    /// `raise x from cause`.
    public static PyException raise(Object x, Object cause) {
        PyException e = raise(x);
        if (e.value instanceof PyInstance) {
            ((PyInstance) e.value).setAttrDirect("__cause__", cause instanceof PyType
                    ? Py.obj(cause).call(NO_ARGS, NO_KW) : cause);
            ((PyInstance) e.value).setAttrDirect("__suppress_context__", TRUE);
        }
        return e;
    }

    /// The exception the innermost running `except` clause handles, for
    /// `sys.exception()` and `sys.exc_info()`; `None` outside one.
    private static Object handling = NONE;

    /// Marks `e` as the exception being handled and answers the one it
    /// replaces, which [#exitHandler(Object)] restores.
    public static Object enterHandler(PyException e) {
        Object prev = handling;
        handling = e.value;
        return prev;
    }

    public static void exitHandler(Object prev) {
        handling = prev;
    }

    static Object handling() {
        return handling;
    }

    /// Converts anything a `try` caught into a Python exception, so an
    /// `except` clause sees Java failures under their Python names.
    public static PyException convert(Throwable t) {
        if (t instanceof PyException) {
            return (PyException) t;
        }
        if (t instanceof StackOverflowError) {
            return error(RECURSION_ERROR, "maximum recursion depth exceeded");
        }
        if (t instanceof OutOfMemoryError) {
            return error(MEMORY_ERROR, "");
        }
        if (t instanceof ArithmeticException) {
            return error(ZERO_DIVISION_ERROR, String.valueOf(t.getMessage()));
        }
        if (t instanceof IndexOutOfBoundsException) {
            return error(INDEX_ERROR, String.valueOf(t.getMessage()));
        }
        if (t instanceof IllegalArgumentException) {
            return error(VALUE_ERROR, String.valueOf(t.getMessage()));
        }
        if (t instanceof java.io.IOException) {
            return error(OS_ERROR, String.valueOf(t.getMessage()));
        }
        return error(RUNTIME_ERROR, t.toString());
    }

    // ---- truth and comparison --------------------------------------------

    public static boolean isTrue(Object o) {
        if (o == TRUE) {
            return true;
        }
        if (o == FALSE || o == NONE) {
            return false;
        }
        if (o instanceof Long) {
            return ((Long) o).longValue() != 0;
        }
        if (o instanceof Double) {
            return ((Double) o).doubleValue() != 0;
        }
        if (o instanceof String) {
            return ((String) o).length() != 0;
        }
        return obj(o).isTrue();
    }

    public static Object not(Object o) {
        return bool(!isTrue(o));
    }

    public static boolean eqBool(Object a, Object b) {
        if (a instanceof Long && b instanceof Long) {
            return ((Long) a).longValue() == ((Long) b).longValue();
        }
        if (a == b) { //NOPMD CompareObjectsWithEquals - identity is meant
            return !(Py.isFloat(a)) || !Double.isNaN(Py.asDouble(a));
        }
        if (a instanceof String) {
            return b instanceof String && a.equals(b);
        }
        return isTrue(eq(a, b));
    }

    public static Object eq(Object a, Object b) {
        if (a instanceof String && b instanceof String) {
            return bool(a.equals(b));
        }
        if (a instanceof Long) {
            long x = ((Long) a).longValue();
            if (b instanceof Long) {
                long y = ((Long) b).longValue();
                return bool(x == y);
            } else if (b instanceof Double) {
                double y = ((Double) b).doubleValue();
                if (x == (x << 11 >> 11)) {
                    return bool(x == y);
                }
            }
        } else if (a instanceof Double) {
            double x = ((Double) a).doubleValue();
            if (b instanceof Double) {
                double y = ((Double) b).doubleValue();
                return bool(x == y);
            } else if (b instanceof Long) {
                long y = ((Long) b).longValue();
                if (y == (y << 11 >> 11)) {
                    return bool(x == y);
                }
            }
        }
        Object r = Py.obj(a).eq(b);
        if (r == null) {
            r = Py.obj(b).eq(a);
        }
        return r != null ? r : bool(a == b); //NOPMD CompareObjectsWithEquals - identity is meant
    }

    public static Object ne(Object a, Object b) {
        if (a instanceof PyInstance) {
            Object m = Py.typeOf(a).lookup("__ne__");
            if (m != null) {
                Object r = Py.obj(m).call(new Object[] {a, b}, NO_KW);
                if (r != NOT_IMPLEMENTED) {
                    return r;
                }
            }
        }
        return not(eq(a, b));
    }

    public static Object lt(Object a, Object b) {
        if (a instanceof Long) {
            long x = ((Long) a).longValue();
            if (b instanceof Long) {
                long y = ((Long) b).longValue();
                return bool(x < y);
            } else if (b instanceof Double) {
                double y = ((Double) b).doubleValue();
                if (x == (x << 11 >> 11)) {
                    return bool(x < y);
                }
            }
        } else if (a instanceof Double) {
            double x = ((Double) a).doubleValue();
            if (b instanceof Double) {
                double y = ((Double) b).doubleValue();
                return bool(x < y);
            } else if (b instanceof Long) {
                long y = ((Long) b).longValue();
                if (y == (y << 11 >> 11)) {
                    return bool(x < y);
                }
            }
        }
        Object r = Py.obj(a).lt(b);
        if (r == null) {
            r = Py.obj(b).gt(a);
        }
        if (r == null) {
            throw cmpError("<", a, b);
        }
        return r;
    }

    public static Object le(Object a, Object b) {
        if (a instanceof Long) {
            long x = ((Long) a).longValue();
            if (b instanceof Long) {
                long y = ((Long) b).longValue();
                return bool(x <= y);
            } else if (b instanceof Double) {
                double y = ((Double) b).doubleValue();
                if (x == (x << 11 >> 11)) {
                    return bool(x <= y);
                }
            }
        } else if (a instanceof Double) {
            double x = ((Double) a).doubleValue();
            if (b instanceof Double) {
                double y = ((Double) b).doubleValue();
                return bool(x <= y);
            } else if (b instanceof Long) {
                long y = ((Long) b).longValue();
                if (y == (y << 11 >> 11)) {
                    return bool(x <= y);
                }
            }
        }
        Object r = Py.obj(a).le(b);
        if (r == null) {
            r = Py.obj(b).ge(a);
        }
        if (r == null) {
            throw cmpError("<=", a, b);
        }
        return r;
    }

    public static Object gt(Object a, Object b) {
        if (a instanceof Long) {
            long x = ((Long) a).longValue();
            if (b instanceof Long) {
                long y = ((Long) b).longValue();
                return bool(x > y);
            } else if (b instanceof Double) {
                double y = ((Double) b).doubleValue();
                if (x == (x << 11 >> 11)) {
                    return bool(x > y);
                }
            }
        } else if (a instanceof Double) {
            double x = ((Double) a).doubleValue();
            if (b instanceof Double) {
                double y = ((Double) b).doubleValue();
                return bool(x > y);
            } else if (b instanceof Long) {
                long y = ((Long) b).longValue();
                if (y == (y << 11 >> 11)) {
                    return bool(x > y);
                }
            }
        }
        Object r = Py.obj(a).gt(b);
        if (r == null) {
            r = Py.obj(b).lt(a);
        }
        if (r == null) {
            throw cmpError(">", a, b);
        }
        return r;
    }

    public static Object ge(Object a, Object b) {
        if (a instanceof Long) {
            long x = ((Long) a).longValue();
            if (b instanceof Long) {
                long y = ((Long) b).longValue();
                return bool(x >= y);
            } else if (b instanceof Double) {
                double y = ((Double) b).doubleValue();
                if (x == (x << 11 >> 11)) {
                    return bool(x >= y);
                }
            }
        } else if (a instanceof Double) {
            double x = ((Double) a).doubleValue();
            if (b instanceof Double) {
                double y = ((Double) b).doubleValue();
                return bool(x >= y);
            } else if (b instanceof Long) {
                long y = ((Long) b).longValue();
                if (y == (y << 11 >> 11)) {
                    return bool(x >= y);
                }
            }
        }
        Object r = Py.obj(a).ge(b);
        if (r == null) {
            r = Py.obj(b).le(a);
        }
        if (r == null) {
            throw cmpError(">=", a, b);
        }
        return r;
    }

    public static boolean ltBool(Object a, Object b) {
        if (a instanceof Long && b instanceof Long) {
            return ((Long) a).longValue() < ((Long) b).longValue();
        }
        if (a instanceof Double && b instanceof Double) {
            return ((Double) a).doubleValue() < ((Double) b).doubleValue();
        }
        if (a instanceof String && b instanceof String) {
            return PyStr.compare((String) a, (String) b) < 0;
        }
        return isTrue(lt(a, b));
    }

    private static PyException cmpError(String op, Object a, Object b) {
        return typeError("'" + op + "' not supported between instances of '" + Py.typeOf(a).name
                + "' and '" + Py.typeOf(b).name + "'");
    }

    public static Object is(Object a, Object b) {
        return bool(identical(a, b));
    }

    public static Object isNot(Object a, Object b) {
        return bool(!identical(a, b));
    }

    /// `is`. Small ints and one-character strings are interned as in CPython;
    /// beyond that two equal numbers are `is`-identical only if they are the
    /// same object, also as in CPython.
    public static boolean identical(Object a, Object b) {
        return a == b; //NOPMD CompareObjectsWithEquals - identity is meant
    }

    public static Object in(Object x, Object container) {
        return bool(Py.obj(container).contains(x));
    }

    public static Object notIn(Object x, Object container) {
        return bool(!Py.obj(container).contains(x));
    }

    // ---- arithmetic ------------------------------------------------------

    private static PyException opError(String op, Object a, Object b) {
        return typeError("unsupported operand type(s) for " + op + ": '" + Py.typeOf(a).name
                + "' and '" + Py.typeOf(b).name + "'");
    }

    /// Whether `b`'s reflected method must be tried first: CPython does so
    /// when `b`'s type is a proper subclass of `a`'s.
    private static boolean rightFirst(Object a, Object b) {
        PyType ta = Py.typeOf(a);
        PyType tb = Py.typeOf(b);
        return ta != tb && tb.isSubtype(ta) && b instanceof PyInstance; //NOPMD CompareObjectsWithEquals - identity is meant
    }

    /// `d[k] = d.get(k, dflt) + inc`, probing an exact dict once.
    public static void dictCount(Object d, Object k, Object dflt, Object inc) {
        if (d instanceof PyDict && ((PyDict) d).type() == DICT_TYPE) {
            ((PyDict) d).count(k, dflt, inc);
            return;
        }
        Object v = add(call(getattr(d, "get"), k, dflt), inc);
        setitem(d, k, v);
    }

    public static Object add(Object a, Object b) {
        if (a instanceof String && b instanceof String) {
            return ((String) a).concat((String) b);
        }
        if (a instanceof Long) {
            long x = ((Long) a).longValue();
            if (b instanceof Long) {
                long y = ((Long) b).longValue();
                long r = x + y;
                if (((x ^ r) & (y ^ r)) >= 0) {
                    return Long.valueOf(r);
                }
            } else if (b instanceof Double) {
                double y = ((Double) b).doubleValue();
                return Double.valueOf(x + y);
            }
        } else if (a instanceof Double) {
            double x = ((Double) a).doubleValue();
            if (b instanceof Double) {
                double y = ((Double) b).doubleValue();
                return Double.valueOf(x + y);
            } else if (b instanceof Long) {
                long y = ((Long) b).longValue();
                return Double.valueOf(x + y);
            }
        }
        if (Py.isInt(a) && Py.isInt(b)) {
            PyInt x = Py.intView(a);
            PyInt y = Py.intView(b);
            if (x.big == null && y.big == null) {
                long r = x.value + y.value;
                if (((x.value ^ r) & (y.value ^ r)) >= 0) {
                    return PyInt.of(r);
                }
            }
        }
        if (rightFirst(a, b)) {
            Object r = Py.obj(b).radd(a);
            if (r != null) {
                return r;
            }
        }
        Object r = Py.obj(a).add(b);
        if (r == null) {
            r = Py.obj(b).radd(a);
            if (r == null) {
                if (a instanceof String && !(b instanceof String)) {
                    throw typeError("can only concatenate str (not \"" + Py.typeOf(b).name + "\") to str");
                }
                if (a instanceof PyList && !(b instanceof PyList)) {
                    throw typeError("can only concatenate list (not \"" + Py.typeOf(b).name + "\") to list");
                }
                throw opError("+", a, b);
            }
        }
        return r;
    }

    public static Object sub(Object a, Object b) {
        if (a instanceof Long) {
            long x = ((Long) a).longValue();
            if (b instanceof Long) {
                long y = ((Long) b).longValue();
                long r = x - y;
                if (((x ^ y) & (x ^ r)) >= 0) {
                    return Long.valueOf(r);
                }
            } else if (b instanceof Double) {
                double y = ((Double) b).doubleValue();
                return Double.valueOf(x - y);
            }
        } else if (a instanceof Double) {
            double x = ((Double) a).doubleValue();
            if (b instanceof Double) {
                double y = ((Double) b).doubleValue();
                return Double.valueOf(x - y);
            } else if (b instanceof Long) {
                long y = ((Long) b).longValue();
                return Double.valueOf(x - y);
            }
        }
        if (Py.isInt(a) && Py.isInt(b)) {
            PyInt x = Py.intView(a);
            PyInt y = Py.intView(b);
            if (x.big == null && y.big == null) {
                long r = x.value - y.value;
                if (((x.value ^ y.value) & (x.value ^ r)) >= 0) {
                    return PyInt.of(r);
                }
            }
        }
        Object r = rightFirst(a, b) ? Py.obj(b).rsub(a) : null;
        if (r == null) {
            r = Py.obj(a).sub(b);
        }
        if (r == null) {
            r = Py.obj(b).rsub(a);
        }
        if (r == null) {
            throw opError("-", a, b);
        }
        return r;
    }

    public static Object mul(Object a, Object b) {
        if (a instanceof Long) {
            long x = ((Long) a).longValue();
            if (b instanceof Long) {
                long y = ((Long) b).longValue();
                if (x == (int) x && y == (int) y) {
                    return Long.valueOf(x * y);
                }
                // Wider operands: the product is exact when dividing it back
                // gives the operand (MIN_VALUE * -1 is the one case that lies).
                long r = x * y;
                if (y != 0 && r / y == x && !(x == Long.MIN_VALUE && y == -1) && !(y == Long.MIN_VALUE && x == -1)) {
                    return Long.valueOf(r);
                }
            } else if (b instanceof Double) {
                double y = ((Double) b).doubleValue();
                return Double.valueOf(x * y);
            }
        } else if (a instanceof Double) {
            double x = ((Double) a).doubleValue();
            if (b instanceof Double) {
                double y = ((Double) b).doubleValue();
                return Double.valueOf(x * y);
            } else if (b instanceof Long) {
                long y = ((Long) b).longValue();
                return Double.valueOf(x * y);
            }
        }
        Object r = rightFirst(a, b) ? Py.obj(b).rmul(a) : null;
        if (r == null) {
            r = Py.obj(a).mul(b);
        }
        if (r == null) {
            r = Py.obj(b).rmul(a);
        }
        if (r == null) {
            // A sequence with a non-int: CPython reports the repeat's error.
            if (isSequence(a) || isSequence(b)) {
                throw typeError("can't multiply sequence by non-int of type '"
                        + Py.typeOf(isSequence(a) ? b : a).name + "'");
            }
            throw opError("*", a, b);
        }
        return r;
    }

    public static Object truediv(Object a, Object b) {
        if (a instanceof Long) {
            long x = ((Long) a).longValue();
            if (b instanceof Long) {
                long y = ((Long) b).longValue();
                if (y != 0 && x == (x << 11 >> 11) && y == (y << 11 >> 11)) {
                    return Double.valueOf((double) x / (double) y);
                }
            } else if (b instanceof Double) {
                double y = ((Double) b).doubleValue();
                if (y != 0) {
                    return Double.valueOf(x / y);
                }
            }
        } else if (a instanceof Double) {
            double x = ((Double) a).doubleValue();
            if (b instanceof Double) {
                double y = ((Double) b).doubleValue();
                if (y != 0) {
                    return Double.valueOf(x / y);
                }
            } else if (b instanceof Long) {
                long y = ((Long) b).longValue();
                if (y != 0) {
                    return Double.valueOf(x / y);
                }
            }
        }
        Object r = Py.obj(a).truediv(b);
        if (r == null) {
            r = Py.obj(b).rtruediv(a);
        }
        if (r == null) {
            throw opError("/", a, b);
        }
        return r;
    }

    public static Object floordiv(Object a, Object b) {
        if (a instanceof Long) {
            long x = ((Long) a).longValue();
            if (b instanceof Long) {
                long y = ((Long) b).longValue();
                if (y != 0 && !(x == Long.MIN_VALUE && y == -1)) {
                    long q = x / y;
                    if ((x % y != 0) && ((x ^ y) < 0)) {
                        q--;
                    }
                    return Long.valueOf(q);
                }
            }
        }
        Object r = Py.obj(a).floordiv(b);
        if (r == null) {
            r = Py.obj(b).rfloordiv(a);
        }
        if (r == null) {
            throw opError("//", a, b);
        }
        return r;
    }

    public static Object mod(Object a, Object b) {
        if (a instanceof String) {
            return Formatting.percent((String) a, b);
        }
        if (a instanceof Long) {
            long x = ((Long) a).longValue();
            if (b instanceof Long) {
                long y = ((Long) b).longValue();
                if (y != 0 && y != -1) {
                    long m = x % y;
                    if (m != 0 && ((m ^ y) < 0)) {
                        m += y;
                    }
                    return Long.valueOf(m);
                }
            }
        }
        Object r = Py.obj(a).mod(b);
        if (r == null) {
            r = Py.obj(b).rmod(a);
        }
        if (r == null) {
            throw opError("%", a, b);
        }
        return r;
    }

    public static Object pow(Object a, Object b) {
        if (a instanceof Double) {
            double x = ((Double) a).doubleValue();
            if (b instanceof Double) {
                double y = ((Double) b).doubleValue();
                if (x > 0 || y == Math.floor(y)) {
                    double r = Math.pow(x, y);
                    if (!Double.isInfinite(r) && !Double.isNaN(r)) {
                        return Double.valueOf(r);
                    }
                }
            } else if (b instanceof Long) {
                long y = ((Long) b).longValue();
                if (x != 0 || y >= 0) {
                    double r = Math.pow(x, y);
                    if (!Double.isInfinite(r) && !Double.isNaN(r)) {
                        return Double.valueOf(r);
                    }
                }
            }
        }
        Object r = Py.obj(a).pow(b);
        if (r == null) {
            r = Py.obj(b).rpow(a);
        }
        if (r == null) {
            throw opError("** or pow()", a, b);
        }
        return r;
    }

    public static Object lshift(Object a, Object b) {
        if (a instanceof Long) {
            long x = ((Long) a).longValue();
            if (b instanceof Long) {
                long y = ((Long) b).longValue();
                if (y >= 0 && y < 63 && (x << y >> y) == x) {
                    return Long.valueOf(x << y);
                }
            }
        }
        Object r = Py.obj(a).lshift(b);
        if (r == null) {
            throw opError("<<", a, b);
        }
        return r;
    }

    public static Object rshift(Object a, Object b) {
        if (a instanceof Long) {
            long x = ((Long) a).longValue();
            if (b instanceof Long) {
                long y = ((Long) b).longValue();
                if (y >= 0) {
                    return Long.valueOf(y > 63 ? (x < 0 ? -1 : 0) : x >> y);
                }
            }
        }
        Object r = Py.obj(a).rshift(b);
        if (r == null) {
            throw opError(">>", a, b);
        }
        return r;
    }

    public static Object and(Object a, Object b) {
        if (a instanceof Long) {
            long x = ((Long) a).longValue();
            if (b instanceof Long) {
                long y = ((Long) b).longValue();
                return Long.valueOf(x & y);
            }
        }
        Object r = Py.obj(a).and(b);
        if (r == null) {
            r = Py.obj(b).rand(a);
        }
        if (r == null) {
            throw opError("&", a, b);
        }
        return r;
    }

    public static Object or(Object a, Object b) {
        if (a instanceof Long) {
            long x = ((Long) a).longValue();
            if (b instanceof Long) {
                long y = ((Long) b).longValue();
                return Long.valueOf(x | y);
            }
        }
        Object r = Py.obj(a).or(b);
        if (r == null) {
            r = Py.obj(b).ror(a);
        }
        if (r == null) {
            throw opError("|", a, b);
        }
        return r;
    }

    public static Object xor(Object a, Object b) {
        if (a instanceof Long) {
            long x = ((Long) a).longValue();
            if (b instanceof Long) {
                long y = ((Long) b).longValue();
                return Long.valueOf(x ^ y);
            }
        }
        Object r = Py.obj(a).xor(b);
        if (r == null) {
            r = Py.obj(b).rxor(a);
        }
        if (r == null) {
            throw opError("^", a, b);
        }
        return r;
    }

    /// `a += b`: the in-place method when the type has one, else `a + b`.
    public static Object iadd(Object a, Object b) {
        Object r = a instanceof PyObject ? ((PyObject) a).iadd(b) : null;
        return r != null ? r : add(a, b);
    }

    public static Object isub(Object a, Object b) {
        Object r = a instanceof PyObject ? ((PyObject) a).inplace("__isub__", b) : null;
        return r != null ? r : sub(a, b);
    }

    public static Object imul(Object a, Object b) {
        Object r = a instanceof PyObject ? ((PyObject) a).inplace("__imul__", b) : null;
        return r != null ? r : mul(a, b);
    }

    public static Object itruediv(Object a, Object b) {
        Object r = a instanceof PyObject ? ((PyObject) a).inplace("__itruediv__", b) : null;
        return r != null ? r : truediv(a, b);
    }

    public static Object ifloordiv(Object a, Object b) {
        Object r = a instanceof PyObject ? ((PyObject) a).inplace("__ifloordiv__", b) : null;
        return r != null ? r : floordiv(a, b);
    }

    public static Object imod(Object a, Object b) {
        Object r = a instanceof PyObject ? ((PyObject) a).inplace("__imod__", b) : null;
        return r != null ? r : mod(a, b);
    }

    public static Object ipow(Object a, Object b) {
        Object r = a instanceof PyObject ? ((PyObject) a).inplace("__ipow__", b) : null;
        return r != null ? r : pow(a, b);
    }

    public static Object ilshift(Object a, Object b) {
        Object r = a instanceof PyObject ? ((PyObject) a).inplace("__ilshift__", b) : null;
        return r != null ? r : lshift(a, b);
    }

    public static Object irshift(Object a, Object b) {
        Object r = a instanceof PyObject ? ((PyObject) a).inplace("__irshift__", b) : null;
        return r != null ? r : rshift(a, b);
    }

    public static Object iand(Object a, Object b) {
        Object r = a instanceof PyObject ? ((PyObject) a).inplace("__iand__", b) : null;
        return r != null ? r : and(a, b);
    }

    public static Object ior(Object a, Object b) {
        Object r = a instanceof PyObject ? ((PyObject) a).inplace("__ior__", b) : null;
        return r != null ? r : or(a, b);
    }

    public static Object ixor(Object a, Object b) {
        Object r = a instanceof PyObject ? ((PyObject) a).inplace("__ixor__", b) : null;
        return r != null ? r : xor(a, b);
    }

    public static Object neg(Object a) {
        if (a instanceof Long && ((Long) a).longValue() != Long.MIN_VALUE) {
            return Long.valueOf(-((Long) a).longValue());
        }
        if (a instanceof Double) {
            return Double.valueOf(-((Double) a).doubleValue());
        }
        return Py.obj(a).neg();
    }

    public static Object pos(Object a) {
        return Py.obj(a).pos();
    }

    public static Object invert(Object a) {
        return Py.obj(a).invert();
    }

    /// Bitwise results of two bools stay bool, as in CPython.
    static Object wrapIntResult(PyInt a, PyInt b, long v) {
        if (a instanceof PyBool && b instanceof PyBool) {
            return bool(v != 0);
        }
        return PyInt.of(v);
    }

    // ---- attributes, items, calls ----------------------------------------

    public static Object getattr(Object o, String name) {
        return Py.obj(o).getAttr(name);
    }

    public static void setattr(Object o, String name, Object v) {
        Py.obj(o).setAttr(name, v);
    }

    public static Object getitem(Object o, Object key) {
        if (o instanceof PyList) {
            PyList l = (PyList) o;
            if (key instanceof Long) {
                long i = ((Long) key).longValue();
                if (i >= 0 && i < l.size) {
                    long[] li = l.ints;
                    if (li != null) {
                        return Long.valueOf(li[(int) i]);
                    }
                    if (l.floats == null) {
                        return l.items[(int) i];
                    }
                }
            }
            return l.getItem(key);
        }
        return Py.obj(o).getItem(key);
    }

    // `f(...)` as a raw double when `f` can answer one without running its
    // body (PyFunction.PureD1..4); NaN means "make the ordinary call".

    /// callD for int arguments held unboxed.
    public static double callDL(Object f, long a) {
        return f instanceof PyFunction ? ((PyFunction) f).callL1(a) : Double.NaN;
    }

    public static double callDL(Object f, long a, long b) {
        return f instanceof PyFunction ? ((PyFunction) f).callL2(a, b) : Double.NaN;
    }

    public static double callD(Object f, Object a) {
        return f instanceof PyFunction ? ((PyFunction) f).callD1(a) : Double.NaN;
    }

    public static double callD(Object f, Object a, Object b) {
        return f instanceof PyFunction ? ((PyFunction) f).callD2(a, b) : Double.NaN;
    }

    public static double callD(Object f, Object a, Object b, Object c) {
        return f instanceof PyFunction ? ((PyFunction) f).callD3(a, b, c) : Double.NaN;
    }

    public static double callD(Object f, Object a, Object b, Object c, Object d) {
        return f instanceof PyFunction ? ((PyFunction) f).callD4(a, b, c, d) : Double.NaN;
    }

    private static boolean isSequence(Object o) {
        return o instanceof String || o instanceof PyList || o instanceof PyTuple || o instanceof PyBytes
                || o instanceof PyByteArray;
    }

    /// `o[i]` for an index known when the code was generated.
    public static Object getitem(Object o, long i) {
        if (o instanceof PyList) {
            PyList l = (PyList) o;
            if (i >= 0 && i < l.size) {
                return l.get((int) i);
            }
        } else if (o instanceof PyTuple) {
            Object[] items = ((PyTuple) o).items;
            if (i >= 0 && i < items.length) {
                return items[(int) i];
            }
        }
        return Py.obj(o).getItem(Long.valueOf(i));
    }

    static String[] argv = {""};

    /// What `sys.argv` holds: the program name, then the arguments a
    /// launcher passes. Must be called before `sys` is first imported.
    public static void setArgv(String program, String[] args) {
        String[] a = new String[args.length + 1];
        a[0] = program;
        System.arraycopy(args, 0, a, 1, args.length);
        argv = a;
    }

    /// `o[a:b:c]`, with NONE for a missing part.
    public static Object getslice(Object o, Object a, Object b, Object c) {
        if (o instanceof PyList) {
            return ((PyList) o).getSlice(a, b, c);
        }
        return Py.obj(o).getItem(new PySlice(a, b, c));
    }

    /// `o[a:b:c]` for a local whose previous list `old` nothing else can
    /// see any more: a list slice is copied into `old` rather than a new
    /// list. Anything else is an ordinary [#getslice(Object, Object, Object, Object)].
    public static Object recycleSlice(Object old, Object o, Object a, Object b, Object c) {
        if (o instanceof PyList) {
            if (old instanceof PyList && ((PyList) old).owned && old != o) { //NOPMD CompareObjectsWithEquals - identity is meant
                return ((PyList) o).sliceInto((PyList) old, a, b, c);
            }
            PyList r = ((PyList) o).getSlice(a, b, c);
            r.owned = true;
            return r;
        }
        return getslice(o, a, b, c);
    }

    /// `[]` for a local whose previous list `old` nothing else can see any
    /// more: that list is emptied and handed back, keeping its array.
    public static PyList recycleList(Object old) {
        if (old instanceof PyList && ((PyList) old).owned) {
            PyList l = (PyList) old;
            l.reset();
            return l;
        }
        PyList l = new PyList();
        l.owned = true;
        return l;
    }

    /// The iterator of `for w in s.split():`. A str is split lazily
    /// (TypeMethods.Words); anything else has its `split` called as written.
    public static PyObject splitWords(Object s) {
        if (s instanceof String) {
            return new TypeMethods.Words((String) s);
        }
        return iter(callMethod(s, "split"));
    }

    /// The iterator of `for x in s.split(sep):`, lazy for a str split on a
    /// non-empty str; anything else has its `split` called as written.
    public static PyObject splitOn(Object s, Object sep) {
        if (s instanceof String && sep instanceof String && ((String) sep).length() > 0) {
            return new TypeMethods.Pieces((String) s, (String) sep);
        }
        return iter(callMethod(s, "split", sep));
    }

    /// The position `i` names in a list or tuple of `n` items, or -1 when
    /// `i` is not an int in range.
    private static int floatIndex(Object i, int n) {
        if (!(i instanceof Long)) {
            return -1;
        }
        long k = ((Long) i).longValue();
        if (k < 0) {
            k += n;
        }
        return k < 0 || k >= n ? -1 : (int) k;
    }

    /// Whether `o[i]` is a float of a list or tuple, readable through
    /// [#floatAt(Object, Object)]. Never runs user code, so a generated
    /// float region can test through it and still give up without side
    /// effects.
    public static boolean isFloatAt(Object o, Object i) {
        if (o instanceof PyList) {
            PyList l = (PyList) o;
            int k = floatIndex(i, l.size);
            return k >= 0 && l.isFloatAt(k);
        }
        if (o instanceof PyTuple) {
            Object[] items = ((PyTuple) o).items;
            int k = floatIndex(i, items.length);
            return k >= 0 && items[k] instanceof Double;
        }
        return false;
    }

    /// `o[i]` unboxed, once [#isFloatAt(Object, Object)] said it is a float.
    public static double floatAt(Object o, Object i) {
        if (o instanceof PyList) {
            PyList l = (PyList) o;
            return l.floatAt(floatIndex(i, l.size));
        }
        Object[] items = ((PyTuple) o).items;
        return ((Double) items[floatIndex(i, items.length)]).doubleValue();
    }

    /// `o[i]` unboxed when it is a float of a list or tuple, else NaN: the
    /// generated region then leaves for the general path, which also
    /// handles an item that really is NaN. One call where isFloatAt and
    /// floatAt took two.
    public static double floatItem(Object o, Object i) {
        if (!(i instanceof Long)) {
            return Double.NaN;
        }
        return floatItem(o, ((Long) i).longValue());
    }

    /// floatItem for an index the region holds unboxed. The common case, an
    /// unboxed list read in range, is all this method does, so that the C
    /// compiler can inline it into the region; everything else is
    /// floatItemSlow.
    private static final double[] NO_FLOATS = new double[0];

    /// The unboxed items of a list that holds only floats, which a loop
    /// that cannot grow or retype the list reads directly; an empty array
    /// for anything else.
    public static double[] floatArray(Object o) {
        double[] f = o instanceof PyList ? ((PyList) o).floats : null;
        return f != null ? f : NO_FLOATS;
    }

    /// How many items [#floatArray] holds: the list's length, or 0 when the
    /// list is not all floats, so every index then misses and the reader
    /// takes its general path.
    public static int floatCount(Object o) {
        return o instanceof PyList && ((PyList) o).floats != null ? ((PyList) o).size : 0;
    }

    public static double floatItem(Object o, long k) {
        if (o instanceof PyList) {
            PyList l = (PyList) o;
            double[] f = l.floats;
            if (f != null && k >= 0 && k < l.size) {
                return f[(int) k];
            }
        }
        return floatItemSlow(o, k);
    }

    private static double floatItemSlow(Object o, long k) {
        if (o instanceof PyList) {
            PyList l = (PyList) o;
            int n = l.size;
            if (k < 0) {
                k += n;
            }
            if (k < 0 || k >= n) {
                return Double.NaN;
            }
            double[] f = l.floats;
            if (f != null) {
                return f[(int) k];
            }
            if (l.ints != null) {
                return Double.NaN;
            }
            Object v = l.items[(int) k];
            return v instanceof Double ? ((Double) v).doubleValue() : Double.NaN;
        }
        if (o instanceof PyTuple) {
            Object[] items = ((PyTuple) o).items;
            if (k < 0) {
                k += items.length;
            }
            if (k < 0 || k >= items.length) {
                return Double.NaN;
            }
            Object v = items[(int) k];
            return v instanceof Double ? ((Double) v).doubleValue() : Double.NaN;
        }
        return Double.NaN;
    }

    /// setFloat for an index the region holds unboxed.
    public static void setFloat(Object o, long k, double v) {
        // Only the unboxed in-range store here, so it inlines into the region.
        if (o instanceof PyList) {
            PyList l = (PyList) o;
            double[] f = l.floats;
            if (f != null && k >= 0 && k < l.size) {
                f[(int) k] = v;
                return;
            }
        }
        setFloatSlow(o, k, v);
    }

    private static void setFloatSlow(Object o, long k, double v) {
        if (o instanceof PyList) {
            PyList l = (PyList) o;
            long i = k < 0 ? k + l.size : k;
            if (i >= 0 && i < l.size) {
                l.setFloat((int) i, v);
                return;
            }
        }
        setitem(o, Long.valueOf(k), Double.valueOf(v));
    }

    /// `o[i] = v` for a float a region computed: an unboxed list stores it
    /// unboxed, anything else as `setitem` would.
    public static void setFloat(Object o, Object i, double v) {
        if (o instanceof PyList) {
            PyList l = (PyList) o;
            int k = floatIndex(i, l.size);
            if (k >= 0) {
                l.setFloat(k, v);
                return;
            }
        }
        setitem(o, i, Double.valueOf(v));
    }

    /// Whether `f(start, stop, step)` is the builtin `range` over ints a
    /// generated loop can count itself. The bounds stay well inside a long,
    /// so stepping past the end can never wrap around.
    public static boolean isIntRange(Object f, Object start, Object stop, Object step) {
        if (f != RANGE_TYPE || !(start instanceof Long) || !(stop instanceof Long) || !(step instanceof Long)) {
            return false;
        }
        long a = ((Long) start).longValue();
        long b = ((Long) stop).longValue();
        long c = ((Long) step).longValue();
        long lim = 1L << 61;
        return c != 0 && a > -lim && a < lim && b > -lim && b < lim && c > -lim && c < lim;
    }

    /// `o[a:b:c] = v[d:e:f]`, the source first as Python evaluates it. Two
    /// lists copy directly, without the intermediate list.
    public static void setsliceFrom(Object v, Object d, Object e, Object f, Object o, Object a, Object b, Object c) {
        if (v instanceof PyList && o instanceof PyList) {
            ((PyList) o).setSliceFrom(a, b, c, (PyList) v, d, e, f);
            return;
        }
        Object src = getslice(v, d, e, f);
        setslice(o, a, b, c, src);
    }

    /// `o[a:b:c] = v`, with NONE for a missing part.
    public static void setslice(Object o, Object a, Object b, Object c, Object v) {
        if (o instanceof PyList) {
            ((PyList) o).setSlice(a, b, c, v);
            return;
        }
        Py.obj(o).setItem(new PySlice(a, b, c), v);
    }

    public static void setitem(Object o, Object key, Object v) {
        if (o instanceof PyList) {
            PyList l = (PyList) o;
            if (key instanceof Long) {
                long i = ((Long) key).longValue();
                if (i >= 0 && i < l.size) {
                    long[] li = l.ints;
                    if (li != null) {
                        if (v instanceof Long) {
                            li[(int) i] = ((Long) v).longValue();
                            return;
                        }
                    } else if (l.floats == null) {
                        l.items[(int) i] = v;
                        return;
                    }
                }
            }
            l.setItem(key, v);
            return;
        }
        Py.obj(o).setItem(key, v);
    }

    /// One-argument call; `len(x)` skips the argument array and the builtin.
    public static Object call(Object f, Object a) {
        if (f == Builtins.LEN) { //NOPMD CompareObjectsWithEquals - identity is meant
            return Long.valueOf(len(a));
        }
        return Py.obj(f).call1(a);
    }

    public static Object call(Object f) {
        return Py.obj(f).call0();
    }

    public static Object call(Object f, Object a, Object b) {
        return Py.obj(f).call2(a, b);
    }

    public static Object call(Object f, Object a, Object b, Object c) {
        return Py.obj(f).call3(a, b, c);
    }

    public static Object call(Object f, Object a, Object b, Object c, Object d) {
        return Py.obj(f).call4(a, b, c, d);
    }

    public static Object call(Object f, Object... args) {
        return Py.obj(f).call(args, NO_KW);
    }

    public static Object callKw(Object f, Object[] args, String[] kwNames) {
        return Py.obj(f).call(args, kwNames);
    }

    /// `obj.name(args)`, without materialising the bound method when `name`
    /// is a plain function on the class.
    public static Object callMethod(Object o, String name, Object... args) {
        if (o instanceof PyInstance) {
            PyInstance inst = (PyInstance) o;
            if (inst.own(name) == null) {
                Object m = inst.cls.lookup(name);
                if (m instanceof PyFunction) {
                    return Py.obj(m).call(prepend(o, args), NO_KW);
                }
            }
        } else if (!(o instanceof PyType) && !(o instanceof PyModule) && !(o instanceof PySuper)) {
            // A super proxy reports object as its type; its attributes come
            // from the MRO after the class it was made for, never from there.
            Object m = Py.typeOf(o).lookup(name);
            if (m instanceof PyBuiltin) {
                return Py.obj(m).call(prepend(o, args), NO_KW);
            }
        }
        return Py.obj(Py.obj(o).getAttr(name)).call(args, NO_KW);
    }

    /// A call whose arguments contain `*seq` or `**map` unpacking. `parts`
    /// lists each argument; `kinds` says 0 = plain, 1 = star, 2 = double star,
    /// 3 = keyword (name in `names` at the same index).
    public static Object callSpread(Object f, Object[] parts, int[] kinds, String[] names) {
        List<Object> pos = new ArrayList<Object>();
        List<String> kwn = new ArrayList<String>();
        List<Object> kwv = new ArrayList<Object>();
        for (int i = 0; i < parts.length; i++) {
            switch (kinds[i]) {
                case 0:
                    pos.add(parts[i]);
                    break;
                case 1: {
                    Object it = Py.iter(parts[i]);
                    for (Object v = Py.next(it); v != null; v = Py.next(it)) {
                        pos.add(v);
                    }
                    break;
                }
                case 2: {
                    Object m = parts[i];
                    Object[] keys = m instanceof PyDict ? ((PyDict) m).keyArray()
                            : toArray(Py.obj(Py.obj(m).getAttr("keys")).call(NO_ARGS, NO_KW));
                    for (Object key : keys) {
                        if (!(key instanceof String)) {
                            throw typeError("keywords must be strings");
                        }
                        String kn = ((String) key);
                        if (kwn.contains(kn)) {
                            throw typeError("got multiple values for keyword argument '" + kn + "'");
                        }
                        kwn.add(kn);
                        kwv.add(Py.obj(m).getItem(key));
                    }
                    break;
                }
                default:
                    kwn.add(names[i]);
                    kwv.add(parts[i]);
                    break;
            }
        }
        Object[] args = new Object[pos.size() + kwv.size()];
        for (int i = 0; i < pos.size(); i++) {
            args[i] = pos.get(i);
        }
        for (int i = 0; i < kwv.size(); i++) {
            args[pos.size() + i] = kwv.get(i);
        }
        return Py.obj(f).call(args, kwn.toArray(new String[kwn.size()]));
    }

    public static Object[] prepend(Object first, Object[] rest) {
        Object[] r = new Object[rest.length + 1];
        r[0] = first;
        System.arraycopy(rest, 0, r, 1, rest.length);
        return r;
    }

    /// An integer index; `__index__` for other types.
    public static long index(Object o) {
        if (o instanceof Long) {
            return ((Long) o).longValue();
        }
        if (Py.isInt(o)) {
            return Py.asLong(o);
        }
        if (o instanceof PyInstance) {
            return Py.asLong(o);
        }
        throw typeError("indices must be integers or slices, not " + Py.typeOf(o).name);
    }

    // ---- iteration -------------------------------------------------------

    public static PyObject iter(Object o) {
        Object r = obj(o).iter();
        if (r instanceof PyObject) {
            return (PyObject) r;
        }
        throw typeError("iter() returned non-iterator of type '" + typeOf(r).name + "'");
    }

    /// The next item of iterator `it`, or null when it is exhausted.
    public static Object next(Object it) {
        return ((PyObject) it).next();
    }

    /// Materialises any iterable.
    public static Object[] toArray(Object o) {
        if (o instanceof PyTuple) {
            return ((PyTuple) o).items;
        }
        if (o instanceof PyList) {
            return ((PyList) o).toArray();
        }
        PyList l = new PyList();
        l.extend(o);
        return l.toArray();
    }

    /// `a, b = value`: exactly `n` items or `ValueError`.
    public static Object[] unpack(Object o, int n) {
        if (o instanceof PyTuple) {
            Object[] items = ((PyTuple) o).items;
            if (items.length == n) {
                return items;
            }
            throw unpackError(items.length, n);
        }
        Object it = Py.iter(o);
        Object[] r = new Object[n];
        for (int i = 0; i < n; i++) {
            Object v = Py.next(it);
            if (v == null) {
                throw valueError("not enough values to unpack (expected " + n + ", got " + i + ")");
            }
            r[i] = v;
        }
        if (Py.next(it) != null) {
            throw valueError("too many values to unpack (expected " + n + ")");
        }
        return r;
    }

    /// [#unpack(Object, int)] for a target made of plain names: storing to
    /// those cannot change a list being unpacked, so its own array is read
    /// in place instead of being copied. The caller only reads the result.
    public static Object[] unpackNames(Object o, int n) {
        if (o instanceof PyList) {
            PyList l = (PyList) o;
            if (l.size == n) {
                return l.floats != null || l.ints != null ? l.toArray() : l.items;
            }
            throw unpackError(l.size, n);
        }
        if (o instanceof PyTuple) {
            // A tuple never changes, and the generated code only reads the
            // array it gets back, at once: its own items need no copy.
            Object[] items = ((PyTuple) o).items;
            if (items.length == n) {
                return items;
            }
        }
        return unpack(o, n);
    }

    /// `a, *b, c = value`: `before` items, a list, then `after` items.
    public static Object[] unpackStar(Object o, int before, int after) {
        Object[] all = toArray(o);
        if (all.length < before + after) {
            throw valueError("not enough values to unpack (expected at least " + (before + after)
                    + ", got " + all.length + ")");
        }
        Object[] r = new Object[before + 1 + after];
        System.arraycopy(all, 0, r, 0, before);
        Object[] mid = new Object[all.length - before - after];
        System.arraycopy(all, before, mid, 0, mid.length);
        r[before] = new PyList(mid);
        System.arraycopy(all, all.length - after, r, before + 1, after);
        return r;
    }

    private static PyException unpackError(int got, int n) {
        if (got < n) {
            return valueError("not enough values to unpack (expected " + n + ", got " + got + ")");
        }
        return valueError("too many values to unpack (expected " + n + ")");
    }

    // ---- literals --------------------------------------------------------

    /// `[]`, without the empty varargs array.
    public static PyList list() {
        return new PyList();
    }

    public static PyList list(Object... items) {
        return PyList.of(items);
    }

    public static PyTuple tuple(Object... items) {
        return PyTuple.of(items);
    }

    /// `{k0: v0, k1: v1, ...}` from alternating keys and values.
    public static PyDict dict(Object... kv) {
        PyDict d = new PyDict();
        for (int i = 0; i < kv.length; i += 2) {
            d.setItem(kv[i], kv[i + 1]);
        }
        return d;
    }

    public static PySet set(Object... items) {
        PySet s = new PySet();
        for (Object item : items) {
            s.addItem(item);
        }
        return s;
    }

    public static PySlice slice(Object a, Object b, Object c) {
        return new PySlice(a, b, c);
    }

    // ---- strings ---------------------------------------------------------

    public static String strOf(Object o) {
        return Py.toStr(o);
    }

    /// The string a `__str__`/`__repr__` override returned, type checked.
    static String strValue(Object r, String method) {
        if (!(r instanceof String)) {
            throw typeError(method + " returned non-string (type " + Py.typeOf(r).name + ")");
        }
        return ((String) r);
    }

    /// `format(value, spec)`, the engine of f-strings.
    public static Object format(Object v, String spec) {
        return str(Formatting.format(v, spec));
    }

    /// Joins the pieces of an f-string.
    public static String concat(Object... parts) {
        StringBuilder sb = new StringBuilder();
        for (Object e : parts) {
            Object part = e;
            sb.append(part instanceof String ? ((String) part) : strOf(part));
        }
        return sb.toString();
    }

    static String joinRepr(String open, Object[] items, int n, String close) {
        StringBuilder sb = new StringBuilder(open);
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(Py.toRepr(items[i]));
        }
        return sb.append(close).toString();
    }

    private static final List<Object> REPR_STACK = new ArrayList<Object>();

    /// Recursion guard for containers that contain themselves.
    static boolean enterRepr(Object o) {
        for (int i = 0; i < REPR_STACK.size(); i++) { //NOPMD ForLoopCanBeForeach - indexed: a for-each allocates an iterator
            if (REPR_STACK.get(i) == o) { //NOPMD CompareObjectsWithEquals - identity is meant
                return true;
            }
        }
        REPR_STACK.add(o);
        return false;
    }

    static void exitRepr(Object o) {
        REPR_STACK.remove(REPR_STACK.size() - 1);
    }

    static boolean seqEquals(Object[] a, int na, Object[] b, int nb) {
        if (na != nb) {
            return false;
        }
        for (int i = 0; i < na; i++) {
            if (a[i] != b[i] && !eqBool(a[i], b[i])) { //NOPMD CompareObjectsWithEquals - identity is meant
                return false;
            }
        }
        return true;
    }

    static int seqCompare(Object[] a, int na, Object[] b, int nb) {
        int n = Math.min(na, nb);
        for (int i = 0; i < n; i++) {
            if (a[i] != b[i] && !eqBool(a[i], b[i])) { //NOPMD CompareObjectsWithEquals - identity is meant
                return ltBool(a[i], b[i]) ? -1 : 1;
            }
        }
        return na - nb;
    }

    // ---- type tests ------------------------------------------------------

    /// A mapping argument as the dict holding it.
    public static PyDict toDict(Object o) {
        if (o instanceof PyDict) {
            return (PyDict) o;
        }
        if (o instanceof PyInstance && ((PyInstance) o).base instanceof PyDict) {
            return (PyDict) ((PyInstance) o).base;
        }
        PyDict d = new PyDict();
        d.update(o);
        return d;
    }

    public static boolean isinstance(Object o, Object t) {
        if (t instanceof PyTuple) {
            Object[] items = ((PyTuple) t).items;
            for (Object item : items) {
                if (isinstance(o, item)) {
                    return true;
                }
            }
            return false;
        }
        if (t instanceof PyJavaClass) {
            return ((PyJavaClass) t).isInstance(o);
        }
        if (!(t instanceof PyType)) {
            throw typeError("isinstance() arg 2 must be a type, a tuple of types, or a union");
        }
        return ((PyType) t).instanceCheck(o);
    }

    /// The items of a display with `*x` unpacking: `star[i]` marks the parts
    /// to iterate rather than take whole.
    public static Object[] spread(Object[] parts, boolean[] star) {
        PyList out = new PyList(parts.length);
        for (int i = 0; i < parts.length; i++) {
            if (star[i]) {
                Object it = iter(parts[i]);
                for (Object v = Py.next(it); v != null; v = Py.next(it)) {
                    out.append(v);
                }
            } else {
                out.append(parts[i]);
            }
        }
        return out.toArray();
    }

    /// A dict display with `**m` entries: `kv` holds key/value pairs, and a
    /// `null` key marks a mapping to merge in at that position.
    public static PyDict dictSpread(Object... kv) {
        PyDict d = new PyDict();
        for (int i = 0; i < kv.length; i += 2) {
            if (kv[i] == null) {
                Object m = kv[i + 1];
                Object keys = iter(callMethod(m, "keys"));
                for (Object k = Py.next(keys); k != null; k = Py.next(keys)) {
                    d.setItem(k, getitem(m, k));
                }
            } else {
                d.setItem(kv[i], kv[i + 1]);
            }
        }
        return d;
    }

    /// `from m import *` into `target`: `__all__` when defined, else every
    /// name that does not start with an underscore.
    public static void importStar(PyModule target, PyModule m) {
        Object all = m.getAttrOrNull("__all__");
        if (all != null) {
            Object it = iter(all);
            for (Object n = Py.next(it); n != null; n = Py.next(it)) {
                String name = strOf(n);
                target.setAttr(name, importFrom(m, name));
            }
            return;
        }
        String[] names = m.exportedNames();
        for (String e : names) {
            if (!e.startsWith("_")) {
                Object v = m.getAttrOrNull(e);
                if (v != null) {
                    target.setAttr(e, v);
                }
            }
        }
    }

    // ---- modules ---------------------------------------------------------

    /// `import name`.
    public static PyModule importModule(String name) {
        return PyModule.load(name);
    }

    /// `from module import name`.
    public static Object importFrom(PyModule m, String name) {
        Object v = m.getAttrOrNull(name);
        if (v == null) {
            PyModule sub = PyModule.find(m.name + "." + name);
            if (sub != null) {
                return sub;
            }
            throw error(IMPORT_ERROR, "cannot import name '" + name + "' from '" + m.name + "'");
        }
        return v;
    }
}
