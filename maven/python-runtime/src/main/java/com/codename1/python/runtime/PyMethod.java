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

/// A function bound to the object it was read from (`obj.method`).
public final class PyMethod extends PyObject {
    public final Object self;
    public final Object func;

    public PyMethod(Object self, Object func) {
        this.self = self;
        this.func = func;
    }

    @Override
    public PyType type() {
        return Py.METHOD_TYPE;
    }

    @Override
    public Object call(Object[] args, String[] kwNames) {
        return Py.obj(func).call(Py.prepend(self, args), kwNames);
    }

    @Override
    public Object call0() {
        return Py.obj(func).call1(self);
    }

    @Override
    public Object call1(Object a) {
        return Py.obj(func).call2(self, a);
    }

    @Override
    public Object call2(Object a, Object b) {
        return Py.obj(func).call3(self, a, b);
    }

    @Override
    public Object call3(Object a, Object b, Object c) {
        return Py.obj(func).call4(self, a, b, c);
    }

    @Override
    public Object getAttrOrNull(String name) {
        if ("__self__".equals(name)) {
            return self;
        }
        if ("__func__".equals(name)) {
            return func;
        }
        return Py.obj(func).getAttrOrNull(name);
    }

    @Override
    public Object eq(Object o) {
        if (o instanceof PyMethod) {
            PyMethod m = (PyMethod) o;
            return Py.bool(m.self == self && m.func == func); //NOPMD CompareObjectsWithEquals - identity is meant
        }
        return null;
    }

    @Override
    public long hash() {
        return System.identityHashCode(self) * 31L + System.identityHashCode(func);
    }

    @Override
    public String repr() {
        return "<bound method " + Py.toStr(Py.obj(func).getAttr("__name__")) + " of " + Py.toRepr(self) + ">";
    }
}
