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

import java.util.Arrays;
import java.util.Comparator;

import javafx.beans.NamedArg;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;

/// A live view of a list ordered by a comparator. With no comparator it
/// shows the source order.
///
/// Elements the comparator calls equal keep their source order. The view
/// follows the source: an added element is inserted where it belongs, and an
/// element the source reports as updated moves if its order changed.
/// Changing the comparator is reported as a permutation.
public final class SortedList<E> extends TransformationList<E, E> {

    private Object[] elements;
    private int[] indexes;
    private int size;
    private final ObjectProperty<Comparator<? super E>> comparator;

    /// Creates a view ordered by a comparator.
    public SortedList(@NamedArg("source") ObservableList<? extends E> source,
            @NamedArg("comparator") Comparator<? super E> comparator) {
        super(source);
        size = source.size();
        elements = new Object[size * 3 / 2 + 1];
        indexes = new int[elements.length];
        for (int i = 0; i < size; i++) {
            elements[i] = source.get(i);
            indexes[i] = i;
        }
        this.comparator = new ObjectPropertyBase<Comparator<? super E>>(comparator) {
            @Override
            protected void invalidated() {
                resort();
            }

            @Override
            public Object getBean() {
                return SortedList.this;
            }

            @Override
            public String getName() {
                return "comparator";
            }
        };
        resort();
    }

    /// Creates a view in source order.
    public SortedList(@NamedArg("source") ObservableList<? extends E> source) {
        this(source, null);
    }

    /// The comparator ordering the view; `null` keeps the source order.
    public final ObjectProperty<Comparator<? super E>> comparatorProperty() {
        return comparator;
    }

    /// Returns the comparator, or `null` when the view is in source order.
    public final Comparator<? super E> getComparator() {
        return comparator.get();
    }

    /// Sets the comparator; `null` shows the source order.
    public final void setComparator(Comparator<? super E> comparator) {
        this.comparator.set(comparator);
    }

    /// Orders two entries by the comparator and then by source position, so
    /// the order is total and equal elements stay in source order.
    @SuppressWarnings("unchecked")
    private int order(Object first, int firstIndex, Object second, int secondIndex) {
        Comparator<? super E> current = getComparator();
        if (current != null) {
            int result = current.compare((E) first, (E) second);
            if (result != 0) {
                return result;
            }
        }
        return firstIndex < secondIndex ? -1 : (firstIndex == secondIndex ? 0 : 1);
    }

    private void ensureCapacity(int capacity) {
        if (elements.length < capacity) {
            int grown = capacity * 3 / 2 + 1;
            elements = Arrays.copyOf(elements, grown);
            indexes = Arrays.copyOf(indexes, grown);
        }
    }

    /// Sorts the whole view again and reports the reordering, if any.
    private void resort() {
        final Object[] oldElements = elements;
        final int[] oldIndexes = indexes;
        Integer[] positions = new Integer[size];
        for (int i = 0; i < size; i++) {
            positions[i] = Integer.valueOf(i);
        }
        Arrays.sort(positions, new Comparator<Integer>() {
            @Override
            public int compare(Integer a, Integer b) {
                int x = a.intValue();
                int y = b.intValue();
                return order(oldElements[x], oldIndexes[x], oldElements[y], oldIndexes[y]);
            }
        });
        Object[] newElements = new Object[oldElements.length];
        int[] newIndexes = new int[oldIndexes.length];
        int[] perm = new int[size];
        boolean moved = false;
        for (int i = 0; i < size; i++) {
            int old = positions[i].intValue();
            newElements[i] = oldElements[old];
            newIndexes[i] = oldIndexes[old];
            perm[old] = i;
            moved |= old != i;
        }
        elements = newElements;
        indexes = newIndexes;
        if (moved) {
            modCount++;
            if (hasListeners()) {
                beginChange();
                nextPermutation(0, size, perm);
                endChange();
            }
        }
    }

    /// Returns where an entry belongs among the first `count` entries.
    private int insertionPoint(Object element, int sourceIndex, int count) {
        int low = 0;
        int high = count;
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (order(elements[mid], indexes[mid], element, sourceIndex) < 0) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

    private void insert(int position, Object element, int sourceIndex) {
        ensureCapacity(size + 1);
        System.arraycopy(elements, position, elements, position + 1, size - position);
        System.arraycopy(indexes, position, indexes, position + 1, size - position);
        elements[position] = element;
        indexes[position] = sourceIndex;
        size++;
    }

    private void delete(int position) {
        System.arraycopy(elements, position + 1, elements, position, size - position - 1);
        System.arraycopy(indexes, position + 1, indexes, position, size - position - 1);
        size--;
        elements[size] = null;
    }

    @Override
    protected void sourceChanged(Change<? extends E> c) {
        beginChange();
        try {
            while (c.next()) {
                if (c.wasPermutated()) {
                    for (int i = 0; i < size; i++) {
                        if (indexes[i] >= c.getFrom() && indexes[i] < c.getTo()) {
                            indexes[i] = c.getPermutation(indexes[i]);
                        }
                    }
                    // Equal elements are ordered by source position, which
                    // just changed; with no comparator the whole view did.
                    resort();
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

    private boolean inOrder() {
        for (int i = 1; i < size; i++) {
            if (order(elements[i - 1], indexes[i - 1], elements[i], indexes[i]) > 0) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private void update(Change<? extends E> c) {
        int from = c.getFrom();
        int to = c.getTo();
        if (inOrder()) {
            for (int i = 0; i < size; i++) {
                if (indexes[i] >= from && indexes[i] < to) {
                    nextUpdate(i);
                }
            }
            return;
        }
        // Take the updated elements out, which leaves a sorted remainder,
        // and put each back where it now belongs.
        int count = to - from;
        Object[] moved = new Object[count];
        for (int i = size - 1; i >= 0; i--) {
            if (indexes[i] >= from && indexes[i] < to) {
                Object element = elements[i];
                moved[indexes[i] - from] = element;
                delete(i);
                nextRemove(i, (E) element);
            }
        }
        for (int i = 0; i < count; i++) {
            int position = insertionPoint(moved[i], from + i, size);
            insert(position, moved[i], from + i);
            nextAdd(position, position + 1);
        }
    }

    @SuppressWarnings("unchecked")
    private void addRemove(Change<? extends E> c) {
        ObservableList<? extends E> source = getSource();
        int from = c.getFrom();
        int removedSize = c.getRemovedSize();
        int addedSize = c.getAddedSize();
        if (removedSize > 0) {
            for (int i = size - 1; i >= 0; i--) {
                if (indexes[i] >= from && indexes[i] < from + removedSize) {
                    Object element = elements[i];
                    delete(i);
                    nextRemove(i, (E) element);
                }
            }
            for (int i = 0; i < size; i++) {
                if (indexes[i] >= from) {
                    indexes[i] -= removedSize;
                }
            }
        }
        if (addedSize > 0) {
            for (int i = 0; i < size; i++) {
                if (indexes[i] >= from) {
                    indexes[i] += addedSize;
                }
            }
            if (size == 0) {
                // Filling an empty view: sort the lot once instead of
                // inserting element by element.
                ensureCapacity(addedSize);
                for (int i = 0; i < addedSize; i++) {
                    elements[i] = source.get(from + i);
                    indexes[i] = from + i;
                }
                size = addedSize;
                boolean listening = hasListeners();
                resortQuietly();
                if (listening) {
                    nextAdd(0, size);
                }
            } else {
                for (int i = from; i < from + addedSize; i++) {
                    Object element = source.get(i);
                    int position = insertionPoint(element, i, size);
                    insert(position, element, i);
                    nextAdd(position, position + 1);
                }
            }
        }
    }

    /// Sorts without reporting; for content nobody has been told about yet.
    private void resortQuietly() {
        final Object[] oldElements = elements;
        final int[] oldIndexes = indexes;
        Integer[] positions = new Integer[size];
        for (int i = 0; i < size; i++) {
            positions[i] = Integer.valueOf(i);
        }
        Arrays.sort(positions, new Comparator<Integer>() {
            @Override
            public int compare(Integer a, Integer b) {
                int x = a.intValue();
                int y = b.intValue();
                return order(oldElements[x], oldIndexes[x], oldElements[y], oldIndexes[y]);
            }
        });
        Object[] newElements = new Object[oldElements.length];
        int[] newIndexes = new int[oldIndexes.length];
        for (int i = 0; i < size; i++) {
            int old = positions[i].intValue();
            newElements[i] = oldElements[old];
            newIndexes[i] = oldIndexes[old];
        }
        elements = newElements;
        indexes = newIndexes;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    @SuppressWarnings("unchecked")
    public E get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException();
        }
        return (E) elements[index];
    }

    @Override
    public int getSourceIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException();
        }
        return indexes[index];
    }

    @Override
    public int getViewIndex(int index) {
        for (int i = 0; i < size; i++) {
            if (indexes[i] == index) {
                return i;
            }
        }
        return -1;
    }
}
