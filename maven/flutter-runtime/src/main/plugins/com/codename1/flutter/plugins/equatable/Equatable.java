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
package com.codename1.flutter.plugins.equatable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dart.core.DartList;
import dart.runtime.DartRuntime;

/// equatable's `Equatable`: value equality over [#props], the way the package
/// defines it -- same runtime type, and props equal element by element, with lists,
/// sets and maps inside them compared by content rather than identity.
public abstract class Equatable {

    public abstract DartList<Object> props();

    /// `stringify`: null defers to the global setting, which is on (debug builds
    /// are where an app reads its states' toString).
    public Boolean stringify() {
        return null;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof Equatable && other.getClass() == getClass()
                && deepEquals(props(), ((Equatable) other).props());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode() ^ deepHash(props());
    }

    @Override
    public String toString() {
        Boolean s = stringify();
        String name = getClass().getSimpleName();
        if (s != null && !s.booleanValue()) {
            return name;
        }
        StringBuilder sb = new StringBuilder(name).append('(');
        List<Object> p = props();
        if (p != null) {
            for (int i = 0; i < p.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(DartRuntime.str(p.get(i)));
            }
        }
        return sb.append(')').toString();
    }

    static boolean deepEquals(Object a, Object b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a instanceof List && b instanceof List) {
            List<?> x = (List<?>) a;
            List<?> y = (List<?>) b;
            if (x.size() != y.size()) {
                return false;
            }
            for (int i = 0; i < x.size(); i++) {
                if (!deepEquals(x.get(i), y.get(i))) {
                    return false;
                }
            }
            return true;
        }
        if (a instanceof Set && b instanceof Set) {
            Set<?> x = (Set<?>) a;
            Set<?> y = (Set<?>) b;
            if (x.size() != y.size()) {
                return false;
            }
            for (Object o : x) {
                if (!containsDeep(y, o)) {
                    return false;
                }
            }
            return true;
        }
        if (a instanceof Map && b instanceof Map) {
            Map<?, ?> x = (Map<?, ?>) a;
            Map<?, ?> y = (Map<?, ?>) b;
            if (x.size() != y.size()) {
                return false;
            }
            for (Map.Entry<?, ?> e : x.entrySet()) {
                if (!y.containsKey(e.getKey()) || !deepEquals(e.getValue(), y.get(e.getKey()))) {
                    return false;
                }
            }
            return true;
        }
        return DartRuntime.eq(a, b);
    }

    private static boolean containsDeep(Set<?> s, Object o) {
        if (s.contains(o)) {
            return true;
        }
        for (Iterator<?> it = s.iterator(); it.hasNext();) {
            if (deepEquals(it.next(), o)) {
                return true;
            }
        }
        return false;
    }

    static int deepHash(Object o) {
        if (o == null) {
            return 0;
        }
        if (o instanceof List) {
            int h = 1;
            for (Object e : (List<?>) o) {
                h = h * 31 + deepHash(e);
            }
            return h;
        }
        if (o instanceof Set) {
            int h = 0;
            for (Object e : (Set<?>) o) {
                h += deepHash(e);
            }
            return h;
        }
        if (o instanceof Map) {
            int h = 0;
            for (Map.Entry<?, ?> e : ((Map<?, ?>) o).entrySet()) {
                h += deepHash(e.getKey()) ^ deepHash(e.getValue());
            }
            return h;
        }
        // deepEquals compares through DartRuntime.eq, where 1 == 1.0; the hash must agree.
        return dart.runtime.DartRuntime.hashOf(o);
    }
}
