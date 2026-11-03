/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package dart.runtime;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/// Dart's constant canonicalization: two constant expressions with the same value are the
/// same object (`identical(const [1, 2], const [1, 2])`, `identical(Vec.zero, const Vec(0, 0))`).
/// A constant is built where it is written and then exchanged here for the first instance
/// of equal value: the same class and runtime type, with components that are equal
/// primitives or strings, or the very same (themselves canonical) objects.
public final class ConstCanon {
    private static final HashMap<Key, Object> TABLE = new HashMap<Key, Object>();

    private ConstCanon() {
    }

    /// A transpiled object whose class has a const constructor: its own final field values.
    public interface Keyed {
        Object[] $constKey();
    }

    public static <T> T object(T v) {
        if (!(v instanceof Keyed)) {
            return v;
        }
        return intern(v, ((Keyed) v).$constKey());
    }

    public static <T extends dart.core.DartList<?>> T list(T v) {
        int n = (int) v.length();
        Object[] parts = new Object[n];
        for (int i = 0; i < n; i++) {
            parts[i] = v.idx(i);
        }
        return intern(v, parts);
    }

    public static <T extends Iterable<?>> T set(T v) {
        java.util.ArrayList<Object> parts = new java.util.ArrayList<Object>();
        for (Object o : v) {
            parts.add(o);
        }
        return intern(v, parts.toArray());
    }

    public static <T extends Map<?, ?>> T map(T v) {
        Object[] parts = new Object[v.size() * 2];
        int i = 0;
        Iterator<? extends Map.Entry<?, ?>> it = v.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<?, ?> e = it.next();
            parts[i++] = e.getKey();
            parts[i++] = e.getValue();
        }
        return intern(v, parts);
    }

    @SuppressWarnings("unchecked")
    private static <T> T intern(T v, Object[] parts) {
        Key k = new Key(v.getClass(), TypeWitness.runtimeType(v), parts);
        Object had = TABLE.get(k);
        if (had != null) {
            return (T) had;
        }
        TABLE.put(k, v);
        return v;
    }

    private static boolean same(Object a, Object b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null || a.getClass() != b.getClass()) {
            return false;
        }
        return a instanceof Long || a instanceof Double || a instanceof String || a instanceof Boolean
                || a instanceof Integer || a instanceof TypeWitness ? a.equals(b) : false;
    }

    private static final class Key {
        final Class<?> cls;
        final Object type;
        final Object[] parts;
        final int hash;

        Key(Class<?> cls, Object type, Object[] parts) {
            this.cls = cls;
            this.type = type;
            this.parts = parts;
            int h = cls.hashCode() * 31 + parts.length;
            for (Object p : parts) {
                h = h * 31 + (p == null ? 0 : p instanceof Long || p instanceof Double || p instanceof String
                        || p instanceof Boolean || p instanceof Integer ? p.hashCode() : System.identityHashCode(p));
            }
            this.hash = h;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Key)) {
                return false;
            }
            Key k = (Key) o;
            if (k.cls != cls || !same(type, k.type) || k.parts.length != parts.length) {
                return false;
            }
            for (int i = 0; i < parts.length; i++) {
                if (!same(parts[i], k.parts[i])) {
                    return false;
                }
            }
            return true;
        }
    }
}
