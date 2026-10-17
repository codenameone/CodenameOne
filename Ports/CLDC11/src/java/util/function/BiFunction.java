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

/// A function of two arguments.
///
/// @param <T> the first input type
/// @param <U> the second input type
/// @param <R> the result type
@FunctionalInterface
public interface BiFunction<T, U, R> {

    /// Applies this function.
    ///
    /// @param t the first input
    /// @param u the second input
    /// @return the result
    R apply(T t, U u);

    /// This function, then `after` on its result.
    ///
    /// @param after the function applied next
    /// @param <V> the result type of `after`
    /// @return the composed function
    default <V> BiFunction<T, U, V> andThen(final Function<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        final BiFunction<T, U, R> self = this;
        return new BiFunction<T, U, V>() {
            public V apply(T t, U u) {
                return after.apply(self.apply(t, u));
            }
        };
    }
}
