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

/// `classmethod(func)`: binds to the class of whatever it is read through.
public final class PyClassMethod extends PyObject {
    final Object func;

    public PyClassMethod(Object func) {
        this.func = func;
    }

    @Override
    public PyType type() {
        return Py.CLASSMETHOD_TYPE;
    }

    @Override
    public Object bind(Object self) {
        return new PyMethod(self instanceof PyType ? self : Py.typeOf(self), func);
    }

    @Override
    public Object getAttrOrNull(String attr) {
        if ("__func__".equals(attr) || "__wrapped__".equals(attr)) {
            return func;
        }
        if ("__isabstractmethod__".equals(attr) || "__doc__".equals(attr) || "__name__".equals(attr)) {
            Object v = Py.obj(func).getAttrOrNull(attr);
            return v != null ? v : "__isabstractmethod__".equals(attr) ? Py.FALSE : Py.NONE;
        }
        return super.getAttrOrNull(attr);
    }
}
