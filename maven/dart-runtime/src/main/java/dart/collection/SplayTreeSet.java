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
package dart.collection;

import dart.core.DartComparable;
import dart.core.DartSet;
import dart.runtime.Funcs;

/**
 * dart:collection's SplayTreeSet: a Set kept in comparator order. It is a DartSet, so
 * every Set member the transpiler knows works on it; insertion re-sorts the elements.
 */
public class SplayTreeSet<E> extends DartSet<E> {
    private final Funcs.Func2<Object, Object, Long> compare;
    private boolean sorting;

    public SplayTreeSet() {
        this(null);
    }

    @SuppressWarnings("unchecked")
    public SplayTreeSet(Object compare) {
        this.compare = (Funcs.Func2<Object, Object, Long>) compare;
    }

    private int cmp(Object a, Object b) {
        long r = compare == null ? DartComparable.compare(a, b) : compare.call(a, b).longValue();
        return r < 0 ? -1 : r > 0 ? 1 : 0;
    }

    @Override
    public boolean add(E e) {
        if (sorting) {
            return super.add(e);
        }
        if (contains(e)) {
            return false;
        }
        java.util.ArrayList<E> all = new java.util.ArrayList<E>(this);
        all.add(e);
        java.util.Collections.sort(all, new java.util.Comparator<E>() {
            public int compare(E a, E b) {
                return cmp(a, b);
            }
        });
        sorting = true;
        try {
            super.clear();
            for (E x : all) {
                super.add(x);
            }
        } finally {
            sorting = false;
        }
        return true;
    }
}
