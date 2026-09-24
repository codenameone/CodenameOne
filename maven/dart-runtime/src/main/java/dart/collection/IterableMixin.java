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

import dart.runtime.Funcs;

/**
 * Dart's {@code dart:collection} {@code IterableMixin<E>}. A transpiled class
 * such as {@code class Board with IterableMixin<BoardPoint>} is emitted as a
 * Java class that {@code implements IterableMixin<BoardPoint>} and supplies the
 * single abstract member {@link #iterator()}; every other member of the
 * Iterable protocol is reached here as an inherited default that iterates via
 * the returned {@link Iterator}.
 *
 * @param <E> the element type
 */
public interface IterableMixin<E> {

    /** The applying class supplies this — the source of every default below. */
    Iterator<E> iterator();

    /** Dart's {@code Iterable.length}. */
    default long length() {
        long count = 0;
        Iterator<E> it = iterator();
        while (it.moveNext()) {
            count++;
        }
        return count;
    }

    /** Dart's {@code Iterable.isEmpty}. */
    default boolean isEmpty() {
        return !iterator().moveNext();
    }

    /** Dart's {@code Iterable.isNotEmpty}. */
    default boolean isNotEmpty() {
        return iterator().moveNext();
    }

    // Dart's StateError, as DartIterable throws: an `on StateError` fallback in the
    // application did not catch Java's NoSuchElementException/IllegalStateException.

    /** Dart's {@code Iterable.first}. */
    default E first() {
        Iterator<E> it = iterator();
        if (!it.moveNext()) {
            throw new dart.core.StateError("No element");
        }
        return it.current();
    }

    /** Dart's {@code Iterable.last}. */
    default E last() {
        Iterator<E> it = iterator();
        if (!it.moveNext()) {
            throw new dart.core.StateError("No element");
        }
        E result;
        do {
            result = it.current();
        } while (it.moveNext());
        return result;
    }

    /** Dart's {@code Iterable.single}. */
    default E single() {
        Iterator<E> it = iterator();
        if (!it.moveNext()) {
            throw new dart.core.StateError("No element");
        }
        E result = it.current();
        if (it.moveNext()) {
            throw new dart.core.StateError("Too many elements");
        }
        return result;
    }

    /** Dart's {@code Iterable.contains(element)}. */
    default boolean contains(Object element) {
        Iterator<E> it = iterator();
        while (it.moveNext()) {
            E e = it.current();
            if (dart.runtime.DartRuntime.eq(e, element)) {   // Dart's ==: 1 contains 1.0
                return true;
            }
        }
        return false;
    }

    /** Dart's {@code Iterable.forEach(action)}. */
    default void forEach(Funcs.VoidFunc1<E> action) {
        Iterator<E> it = iterator();
        while (it.moveNext()) {
            action.call(it.current());
        }
    }

    /** Dart's {@code Iterable.elementAt(index)}. */
    default E elementAt(long index) {
        if (index < 0) {
            throw new dart.core.RangeError("RangeError (index): Invalid value: Not in inclusive range: " + index);
        }
        Iterator<E> it = iterator();
        long i = 0;
        while (it.moveNext()) {
            if (i == index) {
                return it.current();
            }
            i++;
        }
        throw new dart.core.RangeError("RangeError (index): Index out of range: index should be less than " + i + ": " + index);
    }

    /** Dart's {@code Iterable.any(test)}. */
    default boolean any(Funcs.Func1<E, Boolean> test) {
        Iterator<E> it = iterator();
        while (it.moveNext()) {
            if (Boolean.TRUE.equals(test.call(it.current()))) {
                return true;
            }
        }
        return false;
    }

    /** Dart's {@code Iterable.every(test)}. */
    default boolean every(Funcs.Func1<E, Boolean> test) {
        Iterator<E> it = iterator();
        while (it.moveNext()) {
            if (!Boolean.TRUE.equals(test.call(it.current()))) {
                return false;
            }
        }
        return true;
    }

    /** Dart's {@code Iterable.join([separator])}. */
    default String join(String separator) {
        StringBuilder sb = new StringBuilder();
        Iterator<E> it = iterator();
        boolean firstElement = true;
        while (it.moveNext()) {
            if (!firstElement && separator != null) {
                sb.append(separator);
            }
            E e = it.current();
            sb.append(e == null ? "null" : e.toString());
            firstElement = false;
        }
        return sb.toString();
    }

    /** Dart's {@code Iterable.join()} with no separator. */
    default String join() {
        return join("");
    }
}
