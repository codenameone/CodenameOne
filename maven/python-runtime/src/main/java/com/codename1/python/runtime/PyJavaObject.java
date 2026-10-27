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

/// A Java object seen from Python. Its attributes are its public methods and
/// fields; `==` and `hash` are the object's own `equals` and `hashCode`, and
/// `str` its `toString`.
public final class PyJavaObject extends PyObject {
    private static final PyType TYPE = new PyType("java_object", new PyType[] {Py.OBJECT_TYPE}, null);

    /// The Java object.
    public final Object value;

    public PyJavaObject(Object value) {
        this.value = value;
    }

    @Override
    public PyType type() {
        return TYPE;
    }

    /// The Java class's name, for messages.
    public String javaClassName() {
        return value.getClass().getName();
    }

    @Override
    public Object getAttrOrNull(String name) {
        JavaBridge b = JavaBridge.get();
        Object v = b.getField(value, name);
        if (v != null) {
            return v;
        }
        if (b.hasMethod(value, name)) {
            return new PyJavaMethod(b, null, value, name);
        }
        return super.getAttrOrNull(name);
    }

    @Override
    public Object toJava() {
        return value;
    }

    @Override
    public String repr() {
        return "<java object " + javaClassName() + ">";
    }

    @Override
    public String str() {
        return String.valueOf(value);
    }

    @Override
    public Object eq(Object o) {
        return o instanceof PyJavaObject ? Py.bool(value.equals(((PyJavaObject) o).value)) : null;
    }

    // A Java collection behaves as its Python counterpart: `len`, `in`,
    // iteration and indexing. Elements cross as [JavaBridge#toPy] makes them.

    @Override
    public int len() {
        if (value instanceof java.util.Collection) {
            return ((java.util.Collection) value).size();
        }
        if (value instanceof java.util.Map) {
            return ((java.util.Map) value).size();
        }
        return super.len();
    }

    @Override
    public boolean contains(Object x) {
        if (value instanceof java.util.Collection) {
            return ((java.util.Collection) value).contains(JavaBridge.toJava(x));
        }
        if (value instanceof java.util.Map) {
            return ((java.util.Map) value).containsKey(JavaBridge.toJava(x));
        }
        return super.contains(x);
    }

    @Override
    public Object iter() {
        final java.util.Iterator it;
        if (value instanceof java.lang.Iterable) {
            it = ((java.lang.Iterable) value).iterator();
        } else if (value instanceof java.util.Map) {
            it = ((java.util.Map) value).keySet().iterator();
        } else if (value instanceof java.util.Iterator) {
            it = (java.util.Iterator) value;
        } else {
            return super.iter();
        }
        return new PyJavaIter(new PyJavaIter.Source() {
            @Override
            public Object next() {
                return it.hasNext() ? JavaBridge.toPy(it.next()) : null;
            }
        });
    }

    @Override
    public Object getItem(Object key) {
        if (value instanceof java.util.List && Py.isInt(key)) {
            java.util.List l = (java.util.List) value;
            long i = Py.asLong(key);
            if (i < 0) {
                i += l.size();
            }
            if (i < 0 || i >= l.size()) {
                throw Py.error(Py.INDEX_ERROR, "list index out of range");
            }
            return JavaBridge.toPy(l.get((int) i));
        }
        if (value instanceof java.util.Map) {
            java.util.Map m = (java.util.Map) value;
            Object k = JavaBridge.toJava(key);
            if (!m.containsKey(k)) {
                throw Py.keyError(key);
            }
            return JavaBridge.toPy(m.get(k));
        }
        return super.getItem(key);
    }

    @Override
    public long hash() {
        return value.hashCode();
    }
}
