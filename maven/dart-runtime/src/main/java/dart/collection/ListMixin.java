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

import dart.core.DartIterable;
import dart.core.DartList;
import dart.runtime.Funcs;

/**
 * Dart's {@code dart:collection} {@code ListMixin<E>}. The applying class
 * supplies {@code length}, {@code length=}, {@code operator []} and
 * {@code operator []=} ({@link #length()}, {@link #length(long)},
 * {@link #$index(long)}, {@link #$indexSet(long, Object)}); the rest of the List
 * surface is reached here as defaults built on those four, as Dart's mixin is.
 *
 * <p>The Iterable-shaped members that return a lazy view ({@code where},
 * {@code map}, {@code skip}, {@code take}) operate on a snapshot taken by
 * {@link #toList()}. Dart's views read the list on every iteration; a caller that
 * mutates the list between building the view and walking it sees the old
 * elements here.</p>
 *
 * @param <E> the element type
 */
public interface ListMixin<E> extends IterableMixin<E> {

    @Override
    long length();

    /** Dart's {@code length=}. */
    void length(long newLength);

    /** Dart's {@code operator [](index)}. */
    E $index(long index);

    /** Dart's {@code operator []=(index, value)}. */
    void $indexSet(long index, E value);

    @Override
    default Iterator<E> iterator() {
        return new Iterator<E>() {
            private long i = -1;
            private E cur;

            @Override
            public boolean moveNext() {
                if (i + 1 >= length()) {
                    i = length();
                    cur = null;
                    return false;
                }
                i++;
                cur = $index(i);
                return true;
            }

            @Override
            public E current() {
                return cur;
            }
        };
    }

    @Override
    default boolean isEmpty() {
        return length() == 0;
    }

    @Override
    default boolean isNotEmpty() {
        return length() != 0;
    }

    @Override
    default E first() {
        if (length() == 0) {
            throw new dart.core.StateError("No element");
        }
        return $index(0);
    }

    @Override
    default E last() {
        if (length() == 0) {
            throw new dart.core.StateError("No element");
        }
        return $index(length() - 1);
    }

    @Override
    default E elementAt(long index) {
        return $index(index);
    }

    /** Dart's {@code add}: grows the list by one and stores the element. */
    default void add(E element) {
        long n = length();
        length(n + 1);
        $indexSet(n, element);
    }

    /** Dart's {@code addAll}. */
    default void addAll(Iterable<? extends E> elements) {
        for (E e : elements) {
            add(e);
        }
    }

    /** Dart's {@code removeLast}. */
    default E removeLast() {
        if (length() == 0) {
            throw new dart.core.StateError("No element");
        }
        E e = $index(length() - 1);
        length(length() - 1);
        return e;
    }

    /** Dart's {@code clear}. */
    default void clear() {
        length(0);
    }

    /** Dart's {@code fillRange(start, end, [fill])}. */
    default void fillRange(long start, long end, E fill) {
        for (long i = start; i < end; i++) {
            $indexSet(i, fill);
        }
    }

    /** Dart's {@code indexOf(element)}. */
    default long indexOf(Object element) {
        return indexOf(element, 0);
    }

    /** Dart's {@code indexOf(element, start)}. */
    default long indexOf(Object element, long start) {
        long n = length();
        for (long i = start < 0 ? 0 : start; i < n; i++) {
            if (dart.runtime.DartRuntime.eq($index(i), element)) {
                return i;
            }
        }
        return -1;
    }

    /** Dart's {@code lastIndexOf(element)}. */
    default long lastIndexOf(Object element) {
        for (long i = length() - 1; i >= 0; i--) {
            if (dart.runtime.DartRuntime.eq($index(i), element)) {
                return i;
            }
        }
        return -1;
    }

    /** Dart's {@code toList()}: a growable copy. */
    default DartList<E> toList() {
        DartList<E> out = new DartList<E>();
        long n = length();
        for (long i = 0; i < n; i++) {
            out.add($index(i));
        }
        return out;
    }

    /** Dart's {@code toList(growable: ...)}; the copy is growable either way. */
    default DartList<E> toList(boolean growable) {
        return toList();
    }

    /** Dart's {@code sublist(start, [end])}. */
    default DartList<E> sublist(long start, long end) {
        long n = length();
        if (start < 0 || start > n || end < start || end > n) {
            throw new dart.core.RangeError("RangeError: Invalid range " + start + ".." + end + " for length " + n);
        }
        DartList<E> out = new DartList<E>();
        for (long i = start; i < end; i++) {
            out.add($index(i));
        }
        return out;
    }

    /** Dart's {@code sublist(start)}. */
    default DartList<E> sublist(long start) {
        return sublist(start, length());
    }

    /** Dart's {@code where(test)}, over a snapshot (see the class comment). */
    default DartIterable<E> where(Funcs.Func1<E, Boolean> test) {
        return toList().where(test);
    }

    /** Dart's {@code map(f)}, over a snapshot (see the class comment). */
    default <R> DartIterable<R> map(Funcs.Func1<E, R> f) {
        return toList().map(f);
    }

    /** Dart's {@code skip(count)}, over a snapshot. */
    default DartIterable<E> skip(long count) {
        return toList().skip(count);
    }

    /** Dart's {@code take(count)}, over a snapshot. */
    default DartIterable<E> take(long count) {
        return toList().take(count);
    }

    /** Dart's {@code fold(initial, combine)}. */
    default <R> R fold(R initialValue, Funcs.Func2<R, E, R> combine) {
        R acc = initialValue;
        long n = length();
        for (long i = 0; i < n; i++) {
            acc = combine.call(acc, $index(i));
        }
        return acc;
    }
}
