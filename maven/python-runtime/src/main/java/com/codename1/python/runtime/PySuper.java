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

/// `super()`: attribute lookup that starts after `cls` in the MRO of `self`.
///
/// The transpiler turns the zero-argument form into `super(Class, self)`
/// statically, which is what CPython's `__class__` cell does at run time.
public final class PySuper extends PyObject {
    final PyType cls;
    final Object self;

    public PySuper(PyType cls, Object self) {
        this.cls = cls;
        this.self = self;
    }

    @Override
    public PyType type() {
        return Py.OBJECT_TYPE;
    }

    @Override
    public Object getAttrOrNull(String name) {
        PyType start = self instanceof PyType && ((PyType) self).isSubtype(cls) ? (PyType) self : Py.typeOf(self);
        PyType[] mro = start.mro;
        int i = 0;
        while (i < mro.length && mro[i] != cls) { //NOPMD CompareObjectsWithEquals - identity is meant
            i++;
        }
        for (i++; i < mro.length; i++) {
            Object v = mro[i].dict.get(name);
            if (v != null) {
                if (v instanceof PyClassMethod) {
                    return new PyMethod(start, ((PyClassMethod) v).func);
                }
                if (v instanceof PyStaticMethod) {
                    return ((PyStaticMethod) v).func;
                }
                if (v instanceof PyProperty) {
                    return ((PyProperty) v).get(self);
                }
                if (self instanceof PyType && "__new__".equals(name)) {
                    return v;
                }
                // A class whose own MRO is searched gets the plain function;
                // a class searched through its metaclass is an instance.
                return start == self ? v : Py.bindAttr(v, self); //NOPMD CompareObjectsWithEquals - identity is meant
            }
        }
        return null;
    }

    @Override
    public String repr() {
        return "<super: <class '" + cls.name + "'>, " + Py.toRepr(self) + ">";
    }
}
