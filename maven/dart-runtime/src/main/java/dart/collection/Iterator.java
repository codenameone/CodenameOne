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

/**
 * Dart's {@code Iterator<E>} protocol: advance with {@link #moveNext()}, then
 * read {@link #current()}. A transpiled {@code class X implements Iterator<E>}
 * supplies {@code moveNext} and the {@code current} property; the java-style
 * {@link #hasNext()}/{@link #next()} pair is provided for interop.
 *
 * <p>All members have defaults so an applying class only needs to override the
 * ones it declares (Dart's {@code moveNext} and {@code current}); anything it
 * omits falls back to an inert default.</p>
 *
 * @param <E> the element type
 */
public interface Iterator<E> {

    /** Advance to the next element; false when the iteration is exhausted. */
    default boolean moveNext() {
        return false;
    }

    /** The element reached by the most recent {@link #moveNext()}. */
    default E current() {
        return null;
    }

    /** Java-style peek: whether another element is available. */
    default boolean hasNext() {
        return moveNext();
    }

    /** Java-style advance: returns {@link #current()} after moving. */
    default E next() {
        moveNext();
        return current();
    }
}
