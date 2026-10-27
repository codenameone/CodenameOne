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

/// `list[int]`: a builtin type subscripted in an annotation. It keeps the
/// origin and the arguments where `typing.get_origin`/`get_args` read them,
/// and calling it builds the origin.
final class PyGenericAlias extends PyObject {
    static final PyType TYPE = new PyType("types.GenericAlias", new PyType[] {Py.OBJECT_TYPE}, null);

    final PyType origin;
    final PyTuple args;

    PyGenericAlias(PyType origin, Object key) {
        this.origin = origin;
        this.args = key instanceof PyTuple ? (PyTuple) key : new PyTuple(new Object[] {key});
    }

    @Override
    public PyType type() {
        return TYPE;
    }

    @Override
    public Object getAttrOrNull(String attr) {
        if ("__origin__".equals(attr)) {
            return origin;
        }
        if ("__args__".equals(attr)) {
            return args;
        }
        if ("__parameters__".equals(attr)) {
            return new PyTuple(new Object[0]);
        }
        return origin.getAttrOrNull(attr);
    }

    @Override
    public Object call(Object[] a, String[] kwNames) {
        return origin.call(a, kwNames);
    }

    @Override
    public Object or(Object o) {
        return PyUnion.of(this, o);
    }

    @Override
    public Object ror(Object o) {
        return PyUnion.of(o, this);
    }

    @Override
    public Object eq(Object o) {
        if (!(o instanceof PyGenericAlias)) {
            return null;
        }
        PyGenericAlias g = (PyGenericAlias) o;
        return Py.bool(g.origin == origin && Py.eqBool(g.args, args));
    }

    @Override
    public long hash() {
        return origin.hash() * 31 + Py.hashOf(args);
    }

    @Override
    public String repr() {
        StringBuilder b = new StringBuilder(PyUnion.typeRepr(origin)).append('[');
        for (int i = 0; i < args.items.length; i++) {
            if (i > 0) {
                b.append(", ");
            }
            b.append(PyUnion.typeRepr(args.items[i]));
        }
        return b.append(']').toString();
    }
}
