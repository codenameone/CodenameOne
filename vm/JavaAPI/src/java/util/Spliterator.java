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
package java.util;

import java.util.function.Consumer;

/// A source of elements that hands them out one at a time, which is what
/// `java.util.stream.StreamSupport` makes a stream of.
///
/// Streams here are sequential, so nothing splits one: `trySplit` may
/// answer null, and the ones this library makes always do.
public interface Spliterator<T> {

    /// The elements have a defined encounter order.
    int ORDERED = 0x00000010;

    /// No two elements are equal.
    int DISTINCT = 0x00000001;

    /// The elements are in the order [#getComparator()] gives.
    int SORTED = 0x00000004;

    /// [#estimateSize()] is exact.
    int SIZED = 0x00000040;

    /// No element is null.
    int NONNULL = 0x00000100;

    /// The source cannot be changed.
    int IMMUTABLE = 0x00000400;

    /// The source may be changed safely while it is read.
    int CONCURRENT = 0x00001000;

    /// The parts a split makes are sized too.
    int SUBSIZED = 0x00004000;

    /// Hands the next element to `action` and answers true, or answers false
    /// when there is none.
    boolean tryAdvance(Consumer<? super T> action);

    /// Hands every remaining element to `action`, in order.
    default void forEachRemaining(Consumer<? super T> action) {
        while (tryAdvance(action)) {
            // each call handed one element on
        }
    }

    /// A part of the elements to be read separately, or null when this
    /// source does not split.
    Spliterator<T> trySplit();

    /// How many elements remain, `Long.MAX_VALUE` when that is not known.
    long estimateSize();

    /// [#estimateSize()] when the source is [#SIZED], -1 otherwise.
    default long getExactSizeIfKnown() {
        return (characteristics() & SIZED) == 0 ? -1L : estimateSize();
    }

    /// The characteristics of this source, as a set of the bits declared here.
    int characteristics();

    /// Whether every bit of `characteristics` is one of this source's.
    default boolean hasCharacteristics(int characteristics) {
        return (characteristics() & characteristics) == characteristics;
    }

    /// The order of a [#SORTED] source, null for the natural order.
    ///
    /// #### Throws
    ///
    /// - `IllegalStateException`: when the source is not sorted
    default Comparator<? super T> getComparator() {
        throw new IllegalStateException();
    }
}
