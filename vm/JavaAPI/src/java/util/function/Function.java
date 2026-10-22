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
package java.util.function;

import java.util.Objects;

/// A function of one argument.
///
/// @param <T> the input type
/// @param <R> the result type
@FunctionalInterface
public interface Function<T, R> {

    /// Applies this function.
    ///
    /// @param t the input
    /// @return the result
    R apply(T t);

    /// `before`, then this function on its result.
    ///
    /// @param before the function applied first
    /// @param <V> the input type of `before`
    /// @return the composed function
    default <V> Function<V, R> compose(final Function<? super V, ? extends T> before) {
        Objects.requireNonNull(before);
        final Function<T, R> self = this;
        return new Function<V, R>() {
            public R apply(V v) {
                return self.apply(before.apply(v));
            }
        };
    }

    /// This function, then `after` on its result.
    ///
    /// @param after the function applied next
    /// @param <V> the result type of `after`
    /// @return the composed function
    default <V> Function<T, V> andThen(final Function<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        final Function<T, R> self = this;
        return new Function<T, V>() {
            public V apply(T t) {
                return after.apply(self.apply(t));
            }
        };
    }

    /// The function that answers its argument.
    ///
    /// @param <T> the type
    /// @return the identity function
    static <T> Function<T, T> identity() {
        return new Function<T, T>() {
            public T apply(T t) {
                return t;
            }
        };
    }
}
