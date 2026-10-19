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
package java.util.concurrent;

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
/// It is safe to use from several threads at once: mutations are made one
/// at a time under an internal lock, and reads take the current array.
///
/// Iterators are read only, as in the JDK: `remove`, `set` and `add` on one
/// throw `UnsupportedOperationException`. `subList` differs from the JDK: it
/// returns an unmodifiable copy of the range instead of a live view.
public class CopyOnWriteArrayList<E> implements List<E>, RandomAccess, Cloneable, java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private static final Object[] EMPTY = new Object[0];

    private Object[] array;
    /// Guards [#array]; every mutation runs whole under it, so two threads
    /// never both copy the same array and lose one of their changes.
    private final transient Object lock = new Object();

    private Object[] getArray() {
        synchronized (lock) {
            return array;
        }
    }

    private void setArray(Object[] a) {
        synchronized (lock) {
            array = a;
        }
    }

    public CopyOnWriteArrayList() {
        setArray(EMPTY);
    }

    public CopyOnWriteArrayList(Collection<? extends E> c) {
        setArray(c.toArray());
    }

    public CopyOnWriteArrayList(E[] toCopyIn) {
        Object[] copy = new Object[toCopyIn.length];
        System.arraycopy(toCopyIn, 0, copy, 0, copy.length);
        setArray(copy);
    }

    private CopyOnWriteArrayList(Object[] shared, boolean marker) {
        setArray(shared);
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
        return getArray().length;
    }

    @Override
    public boolean isEmpty() {
        return getArray().length == 0;
    }

    @Override
    public boolean contains(Object o) {
        Object[] elements = getArray();
        return indexOf(o, elements, 0, elements.length) >= 0;
    }

    @Override
    public int indexOf(Object o) {
        Object[] elements = getArray();
        return indexOf(o, elements, 0, elements.length);
    }

    public int indexOf(E e, int index) {
        Object[] elements = getArray();
        return indexOf(e, elements, index, elements.length);
    }

    @Override
    public int lastIndexOf(Object o) {
        Object[] elements = getArray();
        return lastIndexOf(o, elements, elements.length - 1);
    }

    public int lastIndexOf(E e, int index) {
        return lastIndexOf(e, getArray(), index);
    }

    /// A list sharing this one's current array. Neither list ever writes to
    /// an array it has published, so the two are independent from here on,
    /// which is all a copy has to be.
    @Override
    public Object clone() {
        return new CopyOnWriteArrayList<E>(getArray(), true);
    }

    @Override
    public Object[] toArray() {
        Object[] elements = getArray();
        Object[] copy = new Object[elements.length];
        System.arraycopy(elements, 0, copy, 0, elements.length);
        return copy;
    }

    @Override
    public <T> T[] toArray(T[] a) {
        Object[] elements = getArray();
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
        return element(getArray(), index);
    }

    @Override
    public E set(int index, E element) {
        synchronized (lock) {
            Object[] elements = getArray();
            E old = element(elements, index);
            if (old != element) {
                Object[] copy = new Object[elements.length];
                System.arraycopy(elements, 0, copy, 0, elements.length);
                copy[index] = element;
                setArray(copy);
            }
            return old;
        }
    }

    @Override
    public boolean add(E e) {
        synchronized (lock) {
            Object[] elements = getArray();
            Object[] copy = new Object[elements.length + 1];
            System.arraycopy(elements, 0, copy, 0, elements.length);
            copy[elements.length] = e;
            setArray(copy);
            return true;
        }
    }

    @Override
    public void add(int index, E element) {
        synchronized (lock) {
            Object[] elements = getArray();
            int len = elements.length;
            if (index > len || index < 0) {
                throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + len);
            }
            Object[] copy = new Object[len + 1];
            System.arraycopy(elements, 0, copy, 0, index);
            System.arraycopy(elements, index, copy, index + 1, len - index);
            copy[index] = element;
            setArray(copy);
        }
    }

    @Override
    public E remove(int index) {
        synchronized (lock) {
            Object[] elements = getArray();
            int len = elements.length;
            E old = element(elements, index);
            Object[] copy = new Object[len - 1];
            System.arraycopy(elements, 0, copy, 0, index);
            System.arraycopy(elements, index + 1, copy, index, len - index - 1);
            setArray(copy);
            return old;
        }
    }

    @Override
    public boolean remove(Object o) {
        synchronized (lock) {
            Object[] elements = getArray();
            int index = indexOf(o, elements, 0, elements.length);
            if (index < 0) {
                return false;
            }
            remove(index);
            return true;
        }
    }

    public boolean addIfAbsent(E e) {
        synchronized (lock) {
            if (contains(e)) {
                return false;
            }
            return add(e);
        }
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        Object[] elements = getArray();
        for (Object e : c) {
            if (indexOf(e, elements, 0, elements.length) < 0) {
                return false;
            }
        }
        return true;
    }

    /// Keeps the elements for which membership in `c` equals `keep`.
    private boolean filter(Collection<?> c, boolean keep) {
        synchronized (lock) {
            if (c == null) {
                throw new NullPointerException();
            }
            Object[] elements = getArray();
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
            if (elements != getArray()) {
                // The predicate changed the list (the lock is reentrant):
                // publishing the filtered snapshot would silently undo that,
                // so keep its change and report it, as the JDK does.
                throw new java.util.ConcurrentModificationException();
            }
            Object[] copy = new Object[n];
            System.arraycopy(kept, 0, copy, 0, n);
            setArray(copy);
            return true;
        }
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
        // Snapshot the source before taking this lock, as addAll does: two
        // lists adding each other would otherwise each wait for the other.
        Object[] incoming = c.toArray();
        synchronized (lock) {
            int added = 0;
            for (Object o : incoming) {
                @SuppressWarnings("unchecked")
                E e = (E) o;
                if (addIfAbsent(e)) {
                    added++;
                }
            }
            return added;
        }
    }

    @Override
    public void clear() {
        synchronized (lock) {
            setArray(EMPTY);
        }
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return insert(Integer.MIN_VALUE, c.toArray());
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        return insert(index, c.toArray());
    }

    /// Inserts `incoming` at `index`, or at the end for `Integer.MIN_VALUE`. The caller takes
    /// the snapshot before this lock is held: reading another list under it
    /// would take that list's lock too, and two lists adding each other would
    /// deadlock.
    private boolean insert(int index, Object[] incoming) {
        synchronized (lock) {
            Object[] elements = getArray();
            if (index == Integer.MIN_VALUE) {
                index = elements.length;
            }
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
            setArray(copy);
            return true;
        }
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        if (action == null) {
            throw new NullPointerException();
        }
        Object[] elements = getArray();
        for (int i = 0; i < elements.length; i++) {
            action.accept(CopyOnWriteArrayList.<E>element(elements, i));
        }
    }

    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        synchronized (lock) {
            if (filter == null) {
                throw new NullPointerException();
            }
            Object[] elements = getArray();
            Object[] kept = new Object[elements.length];
            int n = 0;
            for (int i = 0; i < elements.length; i++) {
                if (!filter.test(CopyOnWriteArrayList.<E>element(elements, i))) {
                    kept[n++] = elements[i];
                }
            }
            if (elements != getArray()) {
                // The predicate changed the list, as in filter() below,
                // whether or not it also asked for a removal.
                throw new java.util.ConcurrentModificationException();
            }
            if (n == elements.length) {
                return false;
            }
            Object[] copy = new Object[n];
            System.arraycopy(kept, 0, copy, 0, n);
            setArray(copy);
            return true;
        }
    }

    @Override
    public void replaceAll(UnaryOperator<E> operator) {
        synchronized (lock) {
            if (operator == null) {
                throw new NullPointerException();
            }
            Object[] elements = getArray();
            Object[] copy = new Object[elements.length];
            for (int i = 0; i < elements.length; i++) {
                copy[i] = operator.apply(CopyOnWriteArrayList.<E>element(elements, i));
            }
            setArray(copy);
        }
    }

    @Override
    public void sort(Comparator<? super E> c) {
        synchronized (lock) {
            Object[] copy = toArray();
            if (c == null) {
                Arrays.sort(copy);
            } else {
                sortWith(copy, c);
            }
            setArray(copy);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> void sortWith(Object[] elements, Comparator<? super T> c) {
        // Every element was added as an E, which is what T stands for here.
        Arrays.sort((T[]) elements, c);
    }

    @Override
    public String toString() {
        Object[] elements = getArray();
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
        Object[] elements = getArray();
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
        Object[] elements = getArray();
        for (int i = 0; i < elements.length; i++) {
            hash = 31 * hash + (elements[i] == null ? 0 : elements[i].hashCode());
        }
        return hash;
    }

    @Override
    public Iterator<E> iterator() {
        return new SnapshotIterator<E>(getArray(), 0);
    }

    @Override
    public ListIterator<E> listIterator() {
        return new SnapshotIterator<E>(getArray(), 0);
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        Object[] elements = getArray();
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
        Object[] elements = getArray();
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
