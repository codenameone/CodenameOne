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

import java.lang.ref.WeakReference;

/// `weakref.ref`: a reference that does not keep its referent alive.
///
/// The referent is held by a Java `WeakReference`, so it goes when the
/// garbage collector next runs rather than at the moment its last strong
/// reference is dropped, as it does under CPython's reference counting.
/// The callback is never called: no port can say when a referent died.
public final class PyWeakRef extends PyObject {
    private final WeakReference<Object> ref;
    private boolean hashed;
    private long cachedHash;
    final Object callback;

    PyWeakRef(Object referent, Object callback) {
        if (!referenceable(referent)) {
            throw Py.typeError("cannot create weak reference to '" + Py.typeOf(referent).name + "' object");
        }
        this.ref = new WeakReference<Object>(referent);
        this.callback = callback;
    }

    /// The values CPython can weakly reference: instances of classes, functions,
    /// classes, sets, methods, generators and modules. Ints, strs, tuples,
    /// lists and dicts cannot be.
    static boolean referenceable(Object o) {
        return o instanceof PyInstance || o instanceof PyFunction || o instanceof PyType || o instanceof PySet
                || o instanceof PyMethod || o instanceof PyGenerator || o instanceof PyModule;
    }

    @Override
    public PyType type() {
        return Py.WEAKREF_TYPE;
    }

    /// The referent, or `None` once it has been collected.
    Object get() {
        Object o = ref.get();
        return o == null ? Py.NONE : o;
    }

    @Override
    public Object call(Object[] args, String[] kw) {
        if (args.length != 0 || kw.length != 0) {
            throw Py.typeError("weakref() takes no arguments");
        }
        return get();
    }

    @Override
    public long hash() {
        if (!hashed) {
            Object o = ref.get();
            if (o == null) {
                throw Py.typeError("weak object has gone away");
            }
            cachedHash = Py.hashOf(o);
            hashed = true;
        }
        return cachedHash;
    }

    @Override
    public Object eq(Object o) {
        if (!(o instanceof PyWeakRef)) {
            return Py.NOT_IMPLEMENTED;
        }
        Object a = ref.get();
        Object b = ((PyWeakRef) o).ref.get();
        if (a == null || b == null) {
            return Py.bool(this == o); //NOPMD CompareObjectsWithEquals - identity is meant
        }
        return Py.bool(Py.isTrue(Py.eq(a, b)));
    }

    @Override
    public String repr() {
        String id = Integer.toHexString(System.identityHashCode(this));
        Object o = ref.get();
        if (o == null) {
            return "<weakref at 0x" + id + "; dead>";
        }
        return "<weakref at 0x" + id + "; to '" + Py.typeOf(o).name + "' at 0x"
                + Integer.toHexString(System.identityHashCode(o)) + ">";
    }

    @Override
    public Object getAttrOrNull(String name) {
        if ("__callback__".equals(name)) {
            return callback == null ? Py.NONE : callback;
        }
        return super.getAttrOrNull(name);
    }
}
