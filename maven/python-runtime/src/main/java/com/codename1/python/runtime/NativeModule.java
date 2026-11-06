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

/// A Python module written in Java: what takes the place of a C extension
/// module (`markupsafe._speedups`, `yaml._yaml`) in a Codename One build.
///
/// A C extension is a module whose functions happen to be compiled from C.
/// The Python code above it imports and calls it like any other module, so
/// supporting the package means providing those functions in Java instead;
/// everything else in the package is transpiled as it is. The build
/// generates one class per extension module with a stub for every name the
/// package's Python code uses, and lists it in
/// `src/main/python-packages/cn1-native-modules.properties`; the generated
/// registry installs it before any module is imported.
///
/// ```java
/// public static void install() {
///     new NativeModule("markupsafe._speedups")
///             .function("escape", args -> Escapes.html(String.valueOf(args[0])))
///             .install();
/// }
/// ```
///
/// A [Function] sees Java values -- a Python `str` as a `String`, an `int`
/// as an `Integer` or `Long`, a `float` as a `Double`, `bytes` as `byte[]`,
/// a list or tuple as a `java.util.List` and a dict as a `java.util.Map`,
/// `None` as null, a Java object as itself. Its result is converted back:
/// a `List` becomes a list, an `Object[]` a tuple, a `Map` a dict. A [PyBuiltin.Positional] given to [#raw] sees the
/// Python values untouched.
public final class NativeModule {
    /// A module function over Java values.
    public interface Function {
        /// Runs the function with the call's positional arguments.
        Object call(Object[] args);
    }

    private final PyModule module;

    /// A module named `name`, the dotted name the Python code imports.
    public NativeModule(String name) {
        module = new PyModule(name, new String[0]);
    }

    /// Adds the function `name` over Java values.
    public NativeModule function(final String name, final Function f) {
        module.setAttr(name, PyBuiltin.positional(name, a -> {
            Object[] args = new Object[a.length];
            for (int i = 0; i < a.length; i++) {
                args[i] = plain(a[i]);
            }
            return python(f.call(args));
        }));
        return this;
    }

    /// A Python value as plain Java: a list or tuple is a `java.util.List`,
    /// a dict a `java.util.Map` (insertion ordered), each element converted
    /// the same way; numbers, strings, bytes and `None` as `JavaBridge`
    /// converts them.
    static Object plain(Object v) {
        if (v instanceof PyList) {
            PyList l = (PyList) v;
            java.util.ArrayList<Object> r = new java.util.ArrayList<Object>(l.size());
            for (int i = 0; i < l.size(); i++) {
                r.add(plain(l.get(i)));
            }
            return r;
        }
        if (v instanceof PyTuple) {
            PyTuple t = (PyTuple) v;
            java.util.ArrayList<Object> r = new java.util.ArrayList<Object>(t.size());
            for (int i = 0; i < t.size(); i++) {
                r.add(plain(t.get(i)));
            }
            return r;
        }
        if (v instanceof PyDict) {
            PyDict d = (PyDict) v;
            java.util.LinkedHashMap<Object, Object> r = new java.util.LinkedHashMap<Object, Object>();
            for (Object k : d.keyArray()) {
                r.put(plain(k), plain(d.getItem(k)));
            }
            return r;
        }
        return JavaBridge.toJava(v);
    }

    /// A Java result as Python: a `List` is a list, an `Object[]` a tuple,
    /// a `Map` a dict and a `byte[]` bytes, converted all the way down.
    static Object python(Object v) {
        if (v instanceof java.util.List) {
            java.util.List<?> l = (java.util.List<?>) v;
            Object[] items = new Object[l.size()];
            for (int i = 0; i < items.length; i++) {
                items[i] = python(l.get(i));
            }
            return new PyList(items);
        }
        if (v instanceof Object[]) {
            Object[] a = (Object[]) v;
            Object[] items = new Object[a.length];
            for (int i = 0; i < items.length; i++) {
                items[i] = python(a[i]);
            }
            return new PyTuple(items);
        }
        if (v instanceof java.util.Map) {
            PyDict d = new PyDict();
            for (java.util.Map.Entry<?, ?> e : ((java.util.Map<?, ?>) v).entrySet()) {
                d.setItem(python(e.getKey()), python(e.getValue()));
            }
            return d;
        }
        if (v instanceof byte[]) {
            byte[] b = (byte[]) v;
            byte[] copy = new byte[b.length];
            System.arraycopy(b, 0, copy, 0, b.length);
            return new PyBytes(copy);
        }
        return Py.wrap(v);
    }

    /// Adds the function `name` over Python values, for code that needs
    /// Python's own containers or identity.
    public NativeModule raw(String name, PyBuiltin.Positional f) {
        module.setAttr(name, PyBuiltin.positional(name, f));
        return this;
    }

    /// Adds the module attribute `name`, converted from Java.
    public NativeModule constant(String name, Object value) {
        module.setAttr(name, Py.wrap(value));
        return this;
    }

    /// Makes the module importable, replacing any earlier one of that name.
    public void install() {
        PyModule.registerModule(module);
    }

    /// The `NotImplementedError` a generated stub raises until it is filled
    /// in, naming what is missing.
    public static RuntimeException notImplemented(String what) {
        return Py.error(Py.NOT_IMPLEMENTED_ERROR, what + " is a C extension function that has no Java implementation"
                + " yet: implement it in the class the build generated for its module");
    }
}
