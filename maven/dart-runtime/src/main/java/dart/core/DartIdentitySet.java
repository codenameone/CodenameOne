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
package dart.core;

import java.util.Iterator;

/**
 * Dart's {@code Set.identity()}: elements match only when they are the SAME object.
 * Insertion ordered, like every Dart set.
 *
 * <p>{@code Set.identity()} used to answer a plain DartSet, which collapsed distinct
 * but equal elements -- two separately built {@code ValueKey('a')} -- into one.</p>
 *
 * <p>Stored in a {@link DartIdentityMap}; the inherited LinkedHashSet storage is never
 * used, and every access path is overridden to go through the map.</p>
 */
public final class DartIdentitySet<E> extends DartSet<E> {

    private final DartIdentityMap<E, Boolean> inner = new DartIdentityMap<E, Boolean>();

    @Override
    public boolean add(E e) {
        if (inner.containsKey(e)) {
            return false;
        }
        inner.put(e, Boolean.TRUE);
        structuralChange();
        return true;
    }

    @Override
    public boolean contains(Object e) {
        return inner.containsKey(e);
    }

    @Override
    public boolean remove(Object e) {
        if (!inner.containsKey(e)) {
            return false;
        }
        inner.remove(e);
        structuralChange();
        return true;
    }

    @Override
    public int size() {
        return inner.size();
    }

    @Override
    public boolean isEmpty() {
        return inner.isEmpty();
    }

    @Override
    public void clear() {
        if (!inner.isEmpty()) {
            structuralChange();
        }
        inner.clear();
    }

    @Override
    public Iterator<E> iterator() {
        return inner.keySet().iterator();
    }
}
