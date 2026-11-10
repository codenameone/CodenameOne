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

/// A Java method read as an attribute, static (`Cls.name`) or bound to an
/// object (`obj.name`); calling it picks the overload.
final class PyJavaMethod extends PyObject {
    private static final PyType TYPE = new PyType("java_method", new PyType[] {Py.OBJECT_TYPE}, null);

    private final JavaBridge bridge;
    private final PyJavaClass owner;
    private final Object target;
    private final String name;

    PyJavaMethod(JavaBridge bridge, PyJavaClass owner, Object target, String name) {
        this.bridge = bridge;
        this.owner = owner;
        this.target = target;
        this.name = name;
    }

    @Override
    public PyType type() {
        return TYPE;
    }

    @Override
    public Object call(Object[] args, String[] kwNames) {
        if (kwNames.length > 0) {
            throw Py.typeError(name + "() takes no keyword arguments");
        }
        Object r = owner != null ? bridge.callStatic(owner.id, name, args) : bridge.invoke(target, name, args);
        if (r == null) {
            throw Py.attributeError("Java method '" + name + "' is not available");
        }
        return r;
    }

    @Override
    public String repr() {
        return "<java method " + name + ">";
    }
}
