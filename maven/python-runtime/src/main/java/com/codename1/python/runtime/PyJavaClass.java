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

/// A Java class imported into Python (`from java.com.codename1.ui import
/// Form`): calling it constructs an instance, its attributes are the class's
/// static fields and methods, and `isinstance` tests against it.
public final class PyJavaClass extends PyObject {
    private static final PyType TYPE = new PyType("java_class", new PyType[] {Py.OBJECT_TYPE}, null);

    private final JavaBridge bridge;
    final int id;

    private PyJavaClass(JavaBridge bridge, int id) {
        this.bridge = bridge;
        this.id = id;
    }

    private static PyJavaClass[] made = new PyJavaClass[0];

    static PyJavaClass of(JavaBridge bridge, int id) {
        if (id >= made.length) {
            PyJavaClass[] grown = new PyJavaClass[Math.max(id + 1, made.length * 2)];
            System.arraycopy(made, 0, grown, 0, made.length);
            made = grown;
        }
        PyJavaClass c = made[id];
        if (c == null) {
            c = new PyJavaClass(bridge, id);
            made[id] = c;
        }
        return c;
    }

    @Override
    public PyType type() {
        return TYPE;
    }

    public String name() {
        return bridge.className(id);
    }

    @Override
    public String repr() {
        return "<java class '" + name() + "'>";
    }

    @Override
    public Object call(Object[] args, String[] kwNames) {
        if (kwNames.length > 0) {
            throw Py.typeError(name() + "() takes no keyword arguments");
        }
        Object r = bridge.construct(id, args);
        if (r == null) {
            throw Py.typeError("cannot construct Java class " + name());
        }
        return r;
    }

    @Override
    public Object getAttrOrNull(String name) {
        Object v = bridge.getStatic(id, name);
        if (v != null) {
            return v;
        }
        if (bridge.hasStatic(id, name)) {
            return new PyJavaMethod(bridge, this, null, name);
        }
        if ("__name__".equals(name) || "__qualname__".equals(name)) {
            return name();
        }
        return super.getAttrOrNull(name);
    }

    boolean isInstance(Object o) {
        return o instanceof PyJavaObject && bridge.isInstance(id, ((PyJavaObject) o).value);
    }

    @Override
    public long hash() {
        return id;
    }
}
