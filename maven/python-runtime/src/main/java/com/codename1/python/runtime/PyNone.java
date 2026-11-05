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

/// The singletons `None`, `NotImplemented` and `Ellipsis`.
final class PyNone extends PyObject {
    private final String name;
    private final PyType type;

    PyNone(String name, PyType type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public PyType type() {
        return type == null ? Py.NONE_TYPE : type;
    }

    @Override
    public String repr() {
        return name;
    }

    @Override
    public boolean isTrue() {
        return this != Py.NONE; //NOPMD CompareObjectsWithEquals - identity is meant
    }

    @Override
    public Object toJava() {
        return this == Py.NONE ? null : this; //NOPMD CompareObjectsWithEquals - identity is meant
    }

    @Override
    public long hash() {
        return name.hashCode();
    }
}
