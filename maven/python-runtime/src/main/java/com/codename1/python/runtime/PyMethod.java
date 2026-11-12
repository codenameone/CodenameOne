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
    public Object call4(Object a, Object b, Object c, Object d) {
        return Py.obj(func).call5(self, a, b, c, d);
    }

    @Override
    public Object call5(Object a, Object b, Object c, Object d, Object e) {
        return Py.obj(func).call6(self, a, b, c, d, e);
    }

    @Override
    public Object call6(Object a, Object b, Object c, Object d, Object e, Object f) {
        return Py.obj(func).call7(self, a, b, c, d, e, f);
    }

    @Override
    public Object call7(Object a, Object b, Object c, Object d, Object e, Object f, Object g) {
        return Py.obj(func).call8(self, a, b, c, d, e, f, g);
    }

    @Override
    public Object call8(Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h) {
        return Py.obj(func).call9(self, a, b, c, d, e, f, g, h);
    }

    @Override
    public Object call9(Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i) {
        return Py.obj(func).call10(self, a, b, c, d, e, f, g, h, i);
    }

    @Override
    public Object call10(Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j) {
        return Py.obj(func).call11(self, a, b, c, d, e, f, g, h, i, j);
    }

    @Override
    public Object call11(Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k) {
        return Py.obj(func).call12(self, a, b, c, d, e, f, g, h, i, j, k);
    }

    @Override
    public Object call12(Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l) {
        return Py.obj(func).call13(self, a, b, c, d, e, f, g, h, i, j, k, l);
    }

    @Override
    public Object call13(Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m) {
        return Py.obj(func).call14(self, a, b, c, d, e, f, g, h, i, j, k, l, m);
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
