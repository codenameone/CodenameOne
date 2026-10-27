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

/// `int | None`: a union of types, flattened and without duplicates. It
/// answers `isinstance` and carries `__args__` for `typing.get_args`.
final class PyUnion extends PyObject {
    static final PyType TYPE = new PyType("typing.Union", new PyType[] {Py.OBJECT_TYPE}, null);

    final PyTuple args;

    private PyUnion(Object[] items) {
        this.args = new PyTuple(items);
    }

    static Object of(Object a, Object b) {
        List<Object> out = new ArrayList<Object>();
        add(out, a);
        add(out, b);
        if (out.size() == 1) {
            return out.get(0);
        }
        return new PyUnion(out.toArray());
    }

    private static void add(List<Object> out, Object t) {
        if (t instanceof PyUnion) {
            for (Object o : ((PyUnion) t).args.items) {
                add(out, o);
            }
            return;
        }
        Object v = t == Py.NONE ? Py.NONE_TYPE : t;
        for (Object o : out) {
            if (o == v || Py.eqBool(o, v)) {
                return;
            }
        }
        out.add(v);
    }

    /// How a type reads inside an alias: `int`, `mod.Cls`, `None`.
    static String typeRepr(Object t) {
        if (t == Py.NONE || t == Py.NONE_TYPE) {
            return "None";
        }
        if (t == Py.ELLIPSIS) {
            return "...";
        }
        if (t instanceof PyType) {
            PyType p = (PyType) t;
            Object q = p.dict.get("__qualname__");
            String qual = q instanceof String ? (String) q : p.name;
            Object mod = p.dict.get("__module__");
            if (mod instanceof String && !"builtins".equals(mod)) {
                return mod + "." + qual;
            }
            return qual;
        }
        return Py.toRepr(t);
    }

    boolean instanceCheck(Object o) {
        for (Object t : args.items) {
            if (Py.isinstance(o, t)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public PyType type() {
        return TYPE;
    }

    @Override
    public Object getAttrOrNull(String attr) {
        if ("__args__".equals(attr)) {
            return args;
        }
        if ("__origin__".equals(attr)) {
            return TYPE;
        }
        if ("__parameters__".equals(attr)) {
            return new PyTuple(new Object[0]);
        }
        return super.getAttrOrNull(attr);
    }

    @Override
    public Object or(Object o) {
        return of(this, o);
    }

    @Override
    public Object ror(Object o) {
        return of(o, this);
    }

    @Override
    public Object eq(Object o) {
        if (!(o instanceof PyUnion)) {
            return null;
        }
        Object[] mine = args.items;
        Object[] theirs = ((PyUnion) o).args.items;
        if (mine.length != theirs.length) {
            return Py.FALSE;
        }
        for (Object t : mine) {
            boolean found = false;
            for (Object u : theirs) {
                if (Py.eqBool(t, u)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return Py.FALSE;
            }
        }
        return Py.TRUE;
    }

    @Override
    public long hash() {
        long h = 0;
        for (Object t : args.items) {
            h ^= Py.hashOf(t);
        }
        return h;
    }

    @Override
    public String repr() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < args.items.length; i++) {
            if (i > 0) {
                b.append(" | ");
            }
            b.append(typeRepr(args.items[i]));
        }
        return b.toString();
    }
}
