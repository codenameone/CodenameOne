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

/// A Java object handed to Python code that has no Python counterpart.
///
/// Python sees it as an opaque value: it can be stored, compared by identity,
/// passed back to Java and printed. Calling its methods by name would need
/// reflection, which ParparVM does not keep; the transpiler instead binds
/// Java calls statically (see the interop contract in the developer guide).
public final class PyJavaObject extends PyObject {
    public final Object value;

    public PyJavaObject(Object value) {
        this.value = value;
    }

    @Override
    public PyType type() {
        return Py.OBJECT_TYPE;
    }

    @Override
    public Object toJava() {
        return value;
    }

    @Override
    public String repr() {
        return "<java object " + value + ">";
    }

    @Override
    public Object eq(Object o) {
        return o instanceof PyJavaObject ? Py.bool(value.equals(((PyJavaObject) o).value)) : null;
    }

    @Override
    public long hash() {
        return value.hashCode();
    }
}
