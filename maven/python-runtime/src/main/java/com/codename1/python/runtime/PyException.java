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

/// The Java carrier of a raised Python exception. `value` is the exception
/// instance (`ValueError("x")`); `except` clauses test it with [#matches].
public final class PyException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public final transient Object value;

    public PyException(Object value) {
        super();
        this.value = value;
        // Raised while an except clause runs: the exception it handles is
        // this one's __context__. A bare `raise` hands the same one back.
        Object h = Py.handling();
        if (h != Py.NONE && h != value && value instanceof PyInstance) { //NOPMD CompareObjectsWithEquals - identity is meant
            ((PyInstance) value).setAttrDirect("__context__", h);
        }
        // A raised exception has a traceback. The translated program has no
        // frame objects, so it is an empty `traceback` instance: what
        // `traceback.format_exception` reads to print the header.
        if (value instanceof PyInstance && ((PyInstance) value).getAttrOrNull("__traceback__") == Py.NONE) { //NOPMD CompareObjectsWithEquals - identity is meant
            ((PyInstance) value).setAttrDirect("__traceback__", new PyInstance(Py.TRACEBACK_TYPE));
        }
    }

    /// `isinstance(value, type)`, where `type` may be a tuple of types.
    public boolean matches(Object type) {
        if (type instanceof PyTuple) {
            Object[] items = ((PyTuple) type).items;
            for (Object item : items) {
                if (matches(item)) {
                    return true;
                }
            }
            return false;
        }
        if (!(type instanceof PyType)) {
            throw Py.typeError("catching classes that do not inherit from BaseException is not allowed");
        }
        return Py.typeOf(value).isSubtype((PyType) type);
    }

    @Override
    public String getMessage() {
        String s = Py.toStr(value);
        return s.length() == 0 ? Py.typeOf(value).name : Py.typeOf(value).name + ": " + s;
    }
}
