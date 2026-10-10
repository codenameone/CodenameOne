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
package javafx.collections;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import javafx.beans.Observable;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;

/// A list that reports every change to its content.
public interface ObservableList<E> extends List<E>, Observable {

    /// Adds a listener told about every change to the content.
    void addListener(ListChangeListener<? super E> listener);

    /// Removes one registration of the listener; unknown listeners are ignored.
    void removeListener(ListChangeListener<? super E> listener);

    /// Appends the elements, reported as one change.
    boolean addAll(E... elements);

    /// Replaces the whole content, reported as one change.
    boolean setAll(E... elements);

    /// Replaces the whole content, reported as one change.
    boolean setAll(Collection<? extends E> col);

    /// Removes every occurrence of the elements, reported as one change.
    boolean removeAll(E... elements);

    /// Removes everything but the elements, reported as one change.
    boolean retainAll(E... elements);

    /// Removes the elements from `from` up to but excluding `to`.
    void remove(int from, int to);

    /// Returns a live view of the elements a predicate accepts.
    default FilteredList<E> filtered(Predicate<E> predicate) {
        return new FilteredList<E>(this, predicate);
    }

    /// Returns a live view of the content ordered by a comparator.
    default SortedList<E> sorted(Comparator<E> comparator) {
        return new SortedList<E>(this, comparator);
    }

    /// Returns a live view of the content in its natural order.
    default SortedList<E> sorted() {
        Comparator<E> natural = new Comparator<E>() {
            @Override
            @SuppressWarnings("unchecked")
            public int compare(E first, E second) {
                if (first == null && second == null) {
                    return 0;
                }
                if (first == null) {
                    return -1;
                }
                if (second == null) {
                    return 1;
                }
                if (first instanceof Comparable) {
                    return ((Comparable<Object>) first).compareTo(second);
                }
                // Elements with no order of their own keep the order they have.
                return 0;
            }
        };
        return sorted(natural);
    }
}
