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
package com.codename1.fxcompat.runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.RandomAccess;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.collections.ModifiableObservableListBase;
import javafx.util.Callback;

/// The observable list `FXCollections` hands out: a `java.util.List` whose
/// modifications are reported, each bulk operation as one change and a sort
/// as a permutation.
///
/// With an extractor the list also watches the observables the extractor
/// names for each element and reports an update of the element when one of
/// them turns invalid.
public final class ArrayObservableList<E> extends ModifiableObservableListBase<E> implements RandomAccess {

    private final List<E> backing;
    private final Callback<E, Observable[]> extractor;
    private final IdentityHashMap<E, Watched> watched;
    private final InvalidationListener elementListener;

    /// Wraps a list, optionally watching its elements.
    public ArrayObservableList(List<E> backing, Callback<E, Observable[]> extractor) {
        this.backing = backing;
        this.extractor = extractor;
        if (extractor == null) {
            watched = null;
            elementListener = null;
        } else {
            watched = new IdentityHashMap<E, Watched>();
            elementListener = new InvalidationListener() {
                @Override
                public void invalidated(Observable observable) {
                    elementInvalidated(observable);
                }
            };
            for (int i = 0; i < backing.size(); i++) {
                attach(backing.get(i));
            }
        }
    }

    private void elementInvalidated(Observable observable) {
        beginChange();
        try {
            for (int i = 0; i < backing.size(); i++) {
                Watched entry = watched.get(backing.get(i));
                if (entry != null && entry.has(observable)) {
                    nextUpdate(i);
                }
            }
        } finally {
            endChange();
        }
    }

    private void attach(E element) {
        if (extractor == null || element == null) {
            return;
        }
        Watched entry = watched.get(element);
        if (entry == null) {
            Observable[] observables = extractor.call(element);
            entry = new Watched(observables == null ? new Observable[0] : observables.clone());
            watched.put(element, entry);
            for (Observable observable : entry.observables) {
                observable.addListener(elementListener);
            }
        }
        entry.count++;
    }

    private void detach(E element) {
        if (extractor == null || element == null) {
            return;
        }
        Watched entry = watched.get(element);
        if (entry != null && --entry.count == 0) {
            watched.remove(element);
            for (Observable observable : entry.observables) {
                observable.removeListener(elementListener);
            }
        }
    }

    @Override
    public E get(int index) {
        return backing.get(index);
    }

    @Override
    public int size() {
        return backing.size();
    }

    @Override
    protected void doAdd(int index, E element) {
        backing.add(index, element);
        attach(element);
    }

    @Override
    protected E doSet(int index, E element) {
        E old = backing.set(index, element);
        detach(old);
        attach(element);
        return old;
    }

    @Override
    protected E doRemove(int index) {
        E old = backing.remove(index);
        detach(old);
        return old;
    }

    @Override
    public int indexOf(Object o) {
        return backing.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return backing.lastIndexOf(o);
    }

    @Override
    public boolean contains(Object o) {
        return backing.contains(o);
    }

    @Override
    public Object[] toArray() {
        return backing.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return backing.toArray(a);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return addAll(backing.size(), c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        if (index < 0 || index > backing.size()) {
            throw new IndexOutOfBoundsException("Index: " + index);
        }
        List<E> copy = new ArrayList<E>(c);
        if (copy.isEmpty()) {
            return false;
        }
        beginChange();
        try {
            backing.addAll(index, copy);
            for (int i = 0; i < copy.size(); i++) {
                attach(copy.get(i));
            }
            modCount++;
            nextAdd(index, index + copy.size());
        } finally {
            endChange();
        }
        return true;
    }

    @Override
    protected void removeRange(int fromIndex, int toIndex) {
        if (fromIndex >= toIndex) {
            return;
        }
        beginChange();
        try {
            List<E> range = backing.subList(fromIndex, toIndex);
            List<E> removed = new ArrayList<E>(range);
            range.clear();
            for (int i = 0; i < removed.size(); i++) {
                detach(removed.get(i));
            }
            modCount++;
            nextRemove(fromIndex, removed);
        } finally {
            endChange();
        }
    }

    /// Sorts the content and reports the reordering as a permutation. A
    /// `null` comparator sorts into the natural order.
    ///
    /// #### Throws
    ///
    /// - `ClassCastException`: when there is no comparator and an element
    ///   is not `Comparable`
    @Override
    @SuppressWarnings("unchecked")
    public void sort(final Comparator<? super E> comparator) {
        int count = backing.size();
        if (count < 2) {
            return;
        }
        final Object[] elements = backing.toArray();
        Integer[] order = new Integer[count];
        for (int i = 0; i < count; i++) {
            order[i] = Integer.valueOf(i);
        }
        Arrays.sort(order, new Comparator<Integer>() {
            @Override
            public int compare(Integer a, Integer b) {
                Object first = elements[a.intValue()];
                Object second = elements[b.intValue()];
                if (comparator != null) {
                    return comparator.compare((E) first, (E) second);
                }
                if (!(first instanceof Comparable)) {
                    throw new ClassCastException("Element is not Comparable");
                }
                return ((Comparable<Object>) first).compareTo(second);
            }
        });
        int[] perm = new int[count];
        boolean moved = false;
        for (int i = 0; i < count; i++) {
            int old = order[i].intValue();
            perm[old] = i;
            moved |= old != i;
        }
        if (!moved) {
            return;
        }
        beginChange();
        try {
            for (int i = 0; i < count; i++) {
                backing.set(i, (E) elements[order[i].intValue()]);
            }
            modCount++;
            nextPermutation(0, count, perm);
        } finally {
            endChange();
        }
    }

    /// The observables watched on behalf of one element, and how often that
    /// element is in the list.
    private static final class Watched {
        private final Observable[] observables;
        private int count;

        Watched(Observable[] observables) {
            this.observables = observables;
        }

        boolean has(Observable observable) {
            for (Observable candidate : observables) {
                if (candidate == observable) {
                    return true;
                }
            }
            return false;
        }
    }
}
