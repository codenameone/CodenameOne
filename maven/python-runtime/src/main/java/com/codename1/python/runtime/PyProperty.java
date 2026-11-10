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

/// `property(fget, fset, fdel)`, the descriptor `@property` produces.
public final class PyProperty extends PyObject {
    // Not final: a property subclass's instance holds an empty one that its
    // __init__ fills (see BuiltinSubclass).
    Object fget;
    Object fset;
    Object fdel;

    public PyProperty(Object fget, Object fset, Object fdel) {
        this.fget = fget;
        this.fset = fset;
        this.fdel = fdel;
    }

    @Override
    public PyType type() {
        return Py.PROPERTY_TYPE;
    }

    void fill(PyProperty from) {
        fget = from.fget;
        fset = from.fset;
        fdel = from.fdel;
    }

    Object get(Object self) {
        if (fget == null || fget == Py.NONE) { //NOPMD CompareObjectsWithEquals - identity is meant
            throw Py.attributeError("property has no getter");
        }
        return Py.obj(fget).call(new Object[] {self}, Py.NO_KW);
    }

    void delete(Object self) {
        if (fdel == null || fdel == Py.NONE) { //NOPMD CompareObjectsWithEquals - identity is meant
            throw Py.attributeError("property has no deleter");
        }
        Py.obj(fdel).call(new Object[] {self}, Py.NO_KW);
    }

    void set(Object self, Object value) {
        if (fset == null || fset == Py.NONE) { //NOPMD CompareObjectsWithEquals - identity is meant
            throw Py.attributeError("property has no setter");
        }
        Py.obj(fset).call(new Object[] {self, value}, Py.NO_KW);
    }

    @Override
    public Object getAttrOrNull(String name) {
        if ("setter".equals(name)) {
            return PyBuiltin.positional("setter", args -> new PyProperty(fget, args[0], fdel));
        }
        if ("getter".equals(name)) {
            return PyBuiltin.positional("getter", args -> new PyProperty(args[0], fset, fdel));
        }
        if ("deleter".equals(name)) {
            return PyBuiltin.positional("deleter", args -> new PyProperty(fget, fset, args[0]));
        }
        if ("fget".equals(name)) {
            return fget == null ? Py.NONE : fget;
        }
        if ("fset".equals(name)) {
            return fset == null ? Py.NONE : fset;
        }
        if ("fdel".equals(name)) {
            return fdel == null ? Py.NONE : fdel;
        }
        if ("__isabstractmethod__".equals(name)) {
            Object[] parts = {fget, fset, fdel};
            for (Object part : parts) {
                Object v = part == null ? null : Py.obj(part).getAttrOrNull(name);
                if (v != null && Py.isTrue(v)) {
                    return Py.TRUE;
                }
            }
            return Py.FALSE;
        }
        if ("__doc__".equals(name)) {
            Object v = fget == null ? null : Py.obj(fget).getAttrOrNull(name);
            return v != null ? v : Py.NONE;
        }
        return super.getAttrOrNull(name);
    }
}
