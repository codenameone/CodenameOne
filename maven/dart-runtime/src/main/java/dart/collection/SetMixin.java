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

/// The runtime side of dart:collection's {@code SetMixin}/{@code SetBase}. A program class
/// applying either supplies the abstract members Dart's {@code SetMixin} leaves abstract
/// ({@code add}, {@code contains}, {@code lookup}, {@code remove}, {@code iterator},
/// {@code length}, {@code toSet}); the rest of the Set protocol is the defaults below and
/// the inherited {@link IterableMixin} ones, written as Dart's SetMixin writes them. Where
/// the object is passed as a {@code Set}, {@code dart.core.SetMixinView} bridges it.
public interface SetMixin<E> extends IterableMixin<E> {

    /// {@code SetBase.setToString(set)}: {@code {a, b}}.
    static String setToString(Iterable<?> set) {
        return IterableMixin.iterableToFullString(set, "{", "}");
    }

    /// Dart's {@code add}: true when the element was not yet in the set.
    boolean add(E value);

    @Override
    boolean contains(Object element);

    /// Dart's {@code lookup}: the element of the set equal to {@code element}, or null.
    E lookup(Object element);

    /// Dart's {@code remove}: true when the element was in the set.
    boolean remove(Object value);

    @Override
    long length();

    /// Dart's {@code toSet}: a new set with the same elements.
    dart.core.DartSet<E> toSet();

    @Override
    default boolean isEmpty() {
        return length() == 0;
    }

    @Override
    default boolean isNotEmpty() {
        return length() != 0;
    }

    /// Dart's {@code clear}: removes every element, through {@link #remove}.
    default void clear() {
        removeAll(toSet());
    }

    /// Dart's {@code addAll}.
    default void addAll(Iterable<? extends E> elements) {
        for (E e : elements) {
            add(e);
        }
    }

    /// Dart's {@code removeAll}.
    default void removeAll(Iterable<?> elements) {
        for (Object e : elements) {
            remove(e);
        }
    }

    /// Dart's {@code containsAll}.
    default boolean containsAll(Iterable<?> other) {
        for (Object e : other) {
            if (!contains(e)) {
                return false;
            }
        }
        return true;
    }

    /// Dart's {@code removeWhere}: collects first, so the test never sees a set mid-change.
    default void removeWhere(dart.runtime.Funcs.Func1<E, Boolean> test) {
        java.util.List<E> doomed = new java.util.ArrayList<E>();
        Iterator<E> it = iterator();
        while (it.moveNext()) {
            if (Boolean.TRUE.equals(test.call(it.current()))) {
                doomed.add(it.current());
            }
        }
        for (E e : doomed) {
            remove(e);
        }
    }

    /// Dart's {@code retainWhere}.
    default void retainWhere(dart.runtime.Funcs.Func1<E, Boolean> test) {
        removeWhere(e -> !Boolean.TRUE.equals(test.call(e)));
    }
}
