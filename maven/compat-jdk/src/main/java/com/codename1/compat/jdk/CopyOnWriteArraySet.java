/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codename1.compat.jdk;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

/// `java.util.concurrent.CopyOnWriteArraySet` for the Codename One runtime:
/// a set kept in a [CopyOnWriteArrayList], in the order its elements were
/// added.
///
/// Every mutation replaces the backing array with a modified copy, and an
/// iterator keeps the array that was current when it was created, so a
/// listener that removes itself while the set is being walked neither
/// disturbs the walk nor throws `ConcurrentModificationException`. Iterators
/// are read only, as in the JDK.
///
/// It does not synchronize. Codename One user interface code runs on the
/// event dispatch thread, and a set used elsewhere must stay confined to one
/// thread.
public class CopyOnWriteArraySet<E> extends AbstractSet<E> implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private final CopyOnWriteArrayList<E> al;

    public CopyOnWriteArraySet() {
        al = new CopyOnWriteArrayList<E>();
    }

    public CopyOnWriteArraySet(Collection<? extends E> c) {
        al = new CopyOnWriteArrayList<E>();
        al.addAllAbsent(c);
    }

    @Override
    public int size() {
        return al.size();
    }

    @Override
    public boolean isEmpty() {
        return al.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return al.contains(o);
    }

    @Override
    public Object[] toArray() {
        return al.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return al.toArray(a);
    }

    @Override
    public void clear() {
        al.clear();
    }

    @Override
    public boolean remove(Object o) {
        return al.remove(o);
    }

    @Override
    public boolean add(E e) {
        return al.addIfAbsent(e);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return al.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return al.addAllAbsent(c) > 0;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return al.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return al.retainAll(c);
    }

    /// The elements as they were when this was called, in the order they
    /// were added. `remove` on the iterator throws
    /// `UnsupportedOperationException`.
    @Override
    public Iterator<E> iterator() {
        return al.iterator();
    }

    public boolean removeIf(Predicate<? super E> filter) {
        return al.removeIf(filter);
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        al.forEach(action);
    }
}
