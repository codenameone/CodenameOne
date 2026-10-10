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
package javafx.collections.transformation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import javafx.beans.NamedArg;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;

/// A live view of the elements of a list that a predicate accepts, in their
/// source order. With no predicate it shows everything.
///
/// The view follows the source: elements appear and disappear as they are
/// added, removed, or -- when the source reports an update -- start or stop
/// matching. Changing the predicate is reported as a replacement of the
/// whole content.
public final class FilteredList<E> extends TransformationList<E, E> {

    private int[] filtered;
    private int size;
    private final ObjectProperty<Predicate<? super E>> predicate;

    /// Creates a view showing the elements a predicate accepts.
    public FilteredList(@NamedArg("source") ObservableList<E> source,
            @NamedArg("predicate") Predicate<? super E> predicate) {
        super(source);
        this.filtered = new int[source.size() * 3 / 2 + 1];
        this.predicate = new ObjectPropertyBase<Predicate<? super E>>(predicate) {
            @Override
            protected void invalidated() {
                refilter();
            }

            @Override
            public Object getBean() {
                return FilteredList.this;
            }

            @Override
            public String getName() {
                return "predicate";
            }
        };
        refilter();
    }

    /// Creates a view showing every element.
    public FilteredList(@NamedArg("source") ObservableList<E> source) {
        this(source, null);
    }

    /// The predicate an element must satisfy to be shown; `null` shows all.
    public final ObjectProperty<Predicate<? super E>> predicateProperty() {
        return predicate;
    }

    /// Returns the predicate, or `null` when everything is shown.
    public final Predicate<? super E> getPredicate() {
        return predicate.get();
    }

    /// Sets the predicate; `null` shows everything.
    public final void setPredicate(Predicate<? super E> predicate) {
        this.predicate.set(predicate);
    }

    private boolean accepts(E element) {
        Predicate<? super E> current = getPredicate();
        return current == null || current.test(element);
    }

    private void ensureCapacity(int capacity) {
        if (filtered.length < capacity) {
            filtered = Arrays.copyOf(filtered, capacity * 3 / 2 + 1);
        }
    }

    /// Returns the first view position whose source index is not below the
    /// given one.
    private int findPosition(int sourceIndex) {
        int low = 0;
        int high = size;
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (filtered[mid] < sourceIndex) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

    private void refilter() {
        ObservableList<? extends E> source = getSource();
        List<E> removed = hasListeners() ? new ArrayList<E>(this) : null;
        int count = source.size();
        ensureCapacity(count);
        size = 0;
        for (int i = 0; i < count; i++) {
            if (accepts(source.get(i))) {
                filtered[size++] = i;
            }
        }
        modCount++;
        if (removed != null) {
            beginChange();
            nextReplace(0, size, removed);
            endChange();
        }
    }

    @Override
    protected void sourceChanged(Change<? extends E> c) {
        beginChange();
        try {
            while (c.next()) {
                if (c.wasPermutated()) {
                    permutate(c);
                } else if (c.wasUpdated()) {
                    update(c);
                } else {
                    addRemove(c);
                }
            }
            modCount++;
        } finally {
            endChange();
        }
    }

    private void permutate(Change<? extends E> c) {
        int first = findPosition(c.getFrom());
        int last = findPosition(c.getTo());
        int count = last - first;
        if (count == 0) {
            return;
        }
        // Each key is the new source index with the old view offset below it,
        // so sorting the keys orders the shown elements by where they are now.
        long[] keys = new long[count];
        for (int i = 0; i < count; i++) {
            keys[i] = ((long) c.getPermutation(filtered[first + i]) << 32) | i;
        }
        Arrays.sort(keys);
        int[] perm = new int[count];
        boolean moved = false;
        for (int i = 0; i < count; i++) {
            int oldOffset = (int) (keys[i] & 0xffffffffL);
            perm[oldOffset] = first + i;
            filtered[first + i] = (int) (keys[i] >>> 32);
            moved |= oldOffset != i;
        }
        if (moved) {
            nextPermutation(first, last, perm);
        }
    }

    private void update(Change<? extends E> c) {
        ObservableList<? extends E> source = getSource();
        for (int i = c.getFrom(); i < c.getTo(); i++) {
            E element = source.get(i);
            int position = findPosition(i);
            boolean shown = position < size && filtered[position] == i;
            boolean matches = accepts(element);
            if (shown && matches) {
                nextUpdate(position);
            } else if (shown) {
                System.arraycopy(filtered, position + 1, filtered, position, size - position - 1);
                size--;
                nextRemove(position, element);
            } else if (matches) {
                ensureCapacity(size + 1);
                System.arraycopy(filtered, position, filtered, position + 1, size - position);
                filtered[position] = i;
                size++;
                nextAdd(position, position + 1);
            }
        }
    }

    private void addRemove(Change<? extends E> c) {
        ObservableList<? extends E> source = getSource();
        int from = c.getFrom();
        int removedSize = c.getRemovedSize();
        int addedSize = c.getAddedSize();
        if (removedSize > 0) {
            int first = findPosition(from);
            int last = findPosition(from + removedSize);
            if (last > first) {
                List<E> removed = new ArrayList<E>(last - first);
                for (int p = first; p < last; p++) {
                    removed.add(c.getRemoved().get(filtered[p] - from));
                }
                System.arraycopy(filtered, last, filtered, first, size - last);
                size -= last - first;
                nextRemove(first, removed);
            }
            for (int p = first; p < size; p++) {
                filtered[p] -= removedSize;
            }
        }
        if (addedSize > 0) {
            int position = findPosition(from);
            int[] accepted = new int[addedSize];
            int count = 0;
            for (int i = from; i < from + addedSize; i++) {
                if (accepts(source.get(i))) {
                    accepted[count++] = i;
                }
            }
            ensureCapacity(size + count);
            System.arraycopy(filtered, position, filtered, position + count, size - position);
            System.arraycopy(accepted, 0, filtered, position, count);
            size += count;
            for (int p = position + count; p < size; p++) {
                filtered[p] += addedSize;
            }
            if (count > 0) {
                nextAdd(position, position + count);
            }
        }
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public E get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException();
        }
        return getSource().get(filtered[index]);
    }

    @Override
    public int getSourceIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException();
        }
        return filtered[index];
    }

    @Override
    public int getViewIndex(int index) {
        int position = findPosition(index);
        return position < size && filtered[position] == index ? position : -1;
    }
}
