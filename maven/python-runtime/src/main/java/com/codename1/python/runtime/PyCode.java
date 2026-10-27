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

/// A function's `__code__`: the parts of a code object that describe its
/// parameters, which is what `inspect.signature` and libraries that count
/// a callback's arguments read. There is no bytecode behind it.
final class PyCode extends PyObject {
    static final PyType TYPE = new PyType("code", new PyType[] {Py.OBJECT_TYPE}, null);

    private final PyFunction f;

    PyCode(PyFunction f) {
        this.f = f;
    }

    @Override
    public PyType type() {
        return TYPE;
    }

    /// Positional names, keyword-only names, then `*args` and `**kwargs`,
    /// as CPython orders `co_varnames`.
    private PyTuple varnames() {
        String[] p = f.params;
        Object[] out = new Object[p.length];
        int at = 0;
        for (int i = 0; i < f.positional; i++) {
            out[at++] = p[i];
        }
        int kwBase = f.positional + (f.varArgs ? 1 : 0);
        for (int i = 0; i < f.kwOnly; i++) {
            out[at++] = p[kwBase + i];
        }
        if (f.varArgs) {
            out[at++] = p[f.positional];
        }
        if (f.varKw) {
            out[at++] = p[p.length - 1];
        }
        return new PyTuple(out);
    }

    @Override
    public Object getAttrOrNull(String attr) {
        switch (attr) {
            case "co_argcount":
                return Py.of((long) f.positional);
            case "co_posonlyargcount":
                return Py.of(0L);
            case "co_kwonlyargcount":
                return Py.of((long) f.kwOnly);
            case "co_varnames":
                return varnames();
            case "co_nlocals":
                return Py.of((long) f.params.length);
            case "co_name":
                return f.name;
            case "co_qualname":
                return f.getAttrOrNull("__qualname__");
            case "co_flags":
                return Py.of((long) ((f.varArgs ? 0x04 : 0) | (f.varKw ? 0x08 : 0)));
            case "co_filename":
                return "<python>";
            default:
                return super.getAttrOrNull(attr);
        }
    }

    @Override
    public String repr() {
        return "<code object " + f.name + ">";
    }
}
