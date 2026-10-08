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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.RandomAccess;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/// `java.util.concurrent.CopyOnWriteArrayList` for the Codename One runtime.
///
/// Every mutation replaces the backing array with a modified copy, and an
/// iterator keeps the array that was current when it was created. That is the
/// property listener lists rely on, and it matters on a single thread as much
/// as across threads: a listener that removes itself, or adds another, while
/// the list is being walked to deliver an event neither disturbs the walk nor
/// throws `ConcurrentModificationException`.
///
/// It does not synchronize. Codename One user interface code runs on the
/// event dispatch thread, and a list used elsewhere must stay confined to one
/// thread.
///
/// Iterators are read only, as in the JDK: `remove`, `set` and `add` on one
/// throw `UnsupportedOperationException`. `subList` differs from the JDK: it
/// returns an unmodifiable copy of the range instead of a live view.
public class CopyOnWriteArrayList<E> implements List<E>, RandomAccess, Cloneable, java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private static final Object[] EMPTY = new Object[0];

    private Object[] array;

    public CopyOnWriteArrayList() {
        array = EMPTY;
    }

    public CopyOnWriteArrayList(Collection<? extends E> c) {
        array = c.toArray();
    }

    public CopyOnWriteArrayList(E[] toCopyIn) {
        Object[] copy = new Object[toCopyIn.length];
        System.arraycopy(toCopyIn, 0, copy, 0, copy.length);
        array = copy;
    }

    private CopyOnWriteArrayList(Object[] shared, boolean marker) {
        array = shared;
    }

    private static boolean same(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    private static int indexOf(Object o, Object[] elements, int from, int to) {
        for (int i = from; i < to; i++) {
            if (same(o, elements[i])) {
                return i;
            }
        }
        return -1;
    }

    private static int lastIndexOf(Object o, Object[] elements, int from) {
        for (int i = from; i >= 0; i--) {
            if (same(o, elements[i])) {
                return i;
            }
        }
        return -1;
    }

    @SuppressWarnings("unchecked")
    private static <T> T element(Object[] elements, int index) {
        return (T) elements[index];
    }

    @Override
    public int size() {
        return array.length;
    }

    @Override
    public boolean isEmpty() {
        return array.length == 0;
    }

    @Override
    public boolean contains(Object o) {
        Object[] elements = array;
        return indexOf(o, elements, 0, elements.length) >= 0;
    }

    @Override
    public int indexOf(Object o) {
        Object[] elements = array;
        return indexOf(o, elements, 0, elements.length);
    }

    public int indexOf(E e, int index) {
        Object[] elements = array;
        return indexOf(e, elements, index, elements.length);
    }

    @Override
    public int lastIndexOf(Object o) {
        Object[] elements = array;
        return lastIndexOf(o, elements, elements.length - 1);
    }

    public int lastIndexOf(E e, int index) {
        return lastIndexOf(e, array, index);
    }

    /// A list sharing this one's current array. Neither list ever writes to
    /// an array it has published, so the two are independent from here on,
    /// which is all a copy has to be.
    @Override
    public Object clone() {
        return new CopyOnWriteArrayList<E>(array, true);
    }

    @Override
    public Object[] toArray() {
        Object[] elements = array;
        Object[] copy = new Object[elements.length];
        System.arraycopy(elements, 0, copy, 0, elements.length);
        return copy;
    }

    @Override
    public <T> T[] toArray(T[] a) {
        Object[] elements = array;
        if (a.length < elements.length) {
            // Only a collection can create an array of the caller's type
            // without reflection.
            return new ArrayList<Object>(Arrays.asList(elements)).toArray(a);
        }
        System.arraycopy(elements, 0, a, 0, elements.length);
        if (a.length > elements.length) {
            a[elements.length] = null;
        }
        return a;
    }

    @Override
    public E get(int index) {
        return element(array, index);
    }

    @Override
    public E set(int index, E element) {
        Object[] elements = array;
        E old = element(elements, index);
        if (old != element) {
            Object[] copy = new Object[elements.length];
            System.arraycopy(elements, 0, copy, 0, elements.length);
            copy[index] = element;
            array = copy;
        }
        return old;
    }

    @Override
    public boolean add(E e) {
        Object[] elements = array;
        Object[] copy = new Object[elements.length + 1];
        System.arraycopy(elements, 0, copy, 0, elements.length);
        copy[elements.length] = e;
        array = copy;
        return true;
    }

    @Override
    public void add(int index, E element) {
        Object[] elements = array;
        int len = elements.length;
        if (index > len || index < 0) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + len);
        }
        Object[] copy = new Object[len + 1];
        System.arraycopy(elements, 0, copy, 0, index);
        System.arraycopy(elements, index, copy, index + 1, len - index);
        copy[index] = element;
        array = copy;
    }

    @Override
    public E remove(int index) {
        Object[] elements = array;
        int len = elements.length;
        E old = element(elements, index);
        Object[] copy = new Object[len - 1];
        System.arraycopy(elements, 0, copy, 0, index);
        System.arraycopy(elements, index + 1, copy, index, len - index - 1);
        array = copy;
        return old;
    }

    @Override
    public boolean remove(Object o) {
        Object[] elements = array;
        int index = indexOf(o, elements, 0, elements.length);
        if (index < 0) {
            return false;
        }
        remove(index);
        return true;
    }

    public boolean addIfAbsent(E e) {
        if (contains(e)) {
            return false;
        }
        return add(e);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        Object[] elements = array;
        for (Object e : c) {
            if (indexOf(e, elements, 0, elements.length) < 0) {
                return false;
            }
        }
        return true;
    }

    /// Keeps the elements for which membership in `c` equals `keep`.
    private boolean filter(Collection<?> c, boolean keep) {
        if (c == null) {
            throw new NullPointerException();
        }
        Object[] elements = array;
        Object[] kept = new Object[elements.length];
        int n = 0;
        for (int i = 0; i < elements.length; i++) {
            if (c.contains(elements[i]) == keep) {
                kept[n++] = elements[i];
            }
        }
        if (n == elements.length) {
            return false;
        }
        Object[] copy = new Object[n];
        System.arraycopy(kept, 0, copy, 0, n);
        array = copy;
        return true;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return filter(c, false);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return filter(c, true);
    }

    public int addAllAbsent(Collection<? extends E> c) {
        int added = 0;
        for (E e : c) {
            if (addIfAbsent(e)) {
                added++;
            }
        }
        return added;
    }

    @Override
    public void clear() {
        array = EMPTY;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return addAll(array.length, c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        Object[] incoming = c.toArray();
        Object[] elements = array;
        int len = elements.length;
        if (index > len || index < 0) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + len);
        }
        if (incoming.length == 0) {
            return false;
        }
        Object[] copy = new Object[len + incoming.length];
        System.arraycopy(elements, 0, copy, 0, index);
        System.arraycopy(incoming, 0, copy, index, incoming.length);
        System.arraycopy(elements, index, copy, index + incoming.length, len - index);
        array = copy;
        return true;
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        if (action == null) {
            throw new NullPointerException();
        }
        Object[] elements = array;
        for (int i = 0; i < elements.length; i++) {
            action.accept(CopyOnWriteArrayList.<E>element(elements, i));
        }
    }

    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        if (filter == null) {
            throw new NullPointerException();
        }
        Object[] elements = array;
        Object[] kept = new Object[elements.length];
        int n = 0;
        for (int i = 0; i < elements.length; i++) {
            if (!filter.test(CopyOnWriteArrayList.<E>element(elements, i))) {
                kept[n++] = elements[i];
            }
        }
        if (n == elements.length) {
            return false;
        }
        Object[] copy = new Object[n];
        System.arraycopy(kept, 0, copy, 0, n);
        array = copy;
        return true;
    }

    @Override
    public void replaceAll(UnaryOperator<E> operator) {
        if (operator == null) {
            throw new NullPointerException();
        }
        Object[] elements = array;
        Object[] copy = new Object[elements.length];
        for (int i = 0; i < elements.length; i++) {
            copy[i] = operator.apply(CopyOnWriteArrayList.<E>element(elements, i));
        }
        array = copy;
    }

    @Override
    public void sort(Comparator<? super E> c) {
        Object[] copy = toArray();
        if (c == null) {
            Arrays.sort(copy);
        } else {
            sortWith(copy, c);
        }
        array = copy;
    }

    @SuppressWarnings("unchecked")
    private static <T> void sortWith(Object[] elements, Comparator<? super T> c) {
        // Every element was added as an E, which is what T stands for here.
        Arrays.sort((T[]) elements, c);
    }

    @Override
    public String toString() {
        Object[] elements = array;
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < elements.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(elements[i] == this ? "(this Collection)" : String.valueOf(elements[i]));
        }
        return sb.append(']').toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof List)) {
            return false;
        }
        Object[] elements = array;
        Iterator<?> it = ((List<?>) o).iterator();
        for (int i = 0; i < elements.length; i++) {
            if (!it.hasNext() || !same(elements[i], it.next())) {
                return false;
            }
        }
        return !it.hasNext();
    }

    @Override
    public int hashCode() {
        int hash = 1;
        Object[] elements = array;
        for (int i = 0; i < elements.length; i++) {
            hash = 31 * hash + (elements[i] == null ? 0 : elements[i].hashCode());
        }
        return hash;
    }

    @Override
    public Iterator<E> iterator() {
        return new SnapshotIterator<E>(array, 0);
    }

    @Override
    public ListIterator<E> listIterator() {
        return new SnapshotIterator<E>(array, 0);
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        Object[] elements = array;
        if (index < 0 || index > elements.length) {
            throw new IndexOutOfBoundsException("Index: " + index);
        }
        return new SnapshotIterator<E>(elements, index);
    }

    /// An unmodifiable copy of the range. The JDK returns a live view; code
    /// that writes through a sublist of a copy-on-write list gets an
    /// `UnsupportedOperationException` here instead of a lost update.
    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        Object[] elements = array;
        if (fromIndex < 0 || toIndex > elements.length || fromIndex > toIndex) {
            throw new IndexOutOfBoundsException();
        }
        List<E> out = new ArrayList<E>(toIndex - fromIndex);
        for (int i = fromIndex; i < toIndex; i++) {
            out.add(CopyOnWriteArrayList.<E>element(elements, i));
        }
        return Collections.unmodifiableList(out);
    }

    private static final class SnapshotIterator<E> implements ListIterator<E> {
        private final Object[] snapshot;
        private int cursor;

        SnapshotIterator(Object[] snapshot, int cursor) {
            this.snapshot = snapshot;
            this.cursor = cursor;
        }

        @Override
        public boolean hasNext() {
            return cursor < snapshot.length;
        }

        @Override
        public boolean hasPrevious() {
            return cursor > 0;
        }

        @Override
        public E next() {
            if (cursor >= snapshot.length) {
                throw new NoSuchElementException();
            }
            return element(snapshot, cursor++);
        }

        @Override
        public E previous() {
            if (cursor <= 0) {
                throw new NoSuchElementException();
            }
            return element(snapshot, --cursor);
        }

        @Override
        public int nextIndex() {
            return cursor;
        }

        @Override
        public int previousIndex() {
            return cursor - 1;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(E e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(E e) {
            throw new UnsupportedOperationException();
        }
    }
}
