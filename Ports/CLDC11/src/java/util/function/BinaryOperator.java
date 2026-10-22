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

import java.util.Comparator;
import java.util.Objects;

/// An operation on two values of one type that answers that type.
///
/// @param <T> the operand and result type
@FunctionalInterface
public interface BinaryOperator<T> extends BiFunction<T, T, T> {

    /// The smaller of two values by `comparator`; the first on a tie.
    ///
    /// @param comparator the order
    /// @param <T> the type
    /// @return the operator
    static <T> BinaryOperator<T> minBy(final Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator);
        return new BinaryOperator<T>() {
            public T apply(T a, T b) {
                return comparator.compare(a, b) <= 0 ? a : b;
            }
        };
    }

    /// The larger of two values by `comparator`; the first on a tie.
    ///
    /// @param comparator the order
    /// @param <T> the type
    /// @return the operator
    static <T> BinaryOperator<T> maxBy(final Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator);
        return new BinaryOperator<T>() {
            public T apply(T a, T b) {
                return comparator.compare(a, b) >= 0 ? a : b;
            }
        };
    }
}
