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

/// An operation on one `int` that answers a `int`; the `int` form of [UnaryOperator].
@FunctionalInterface
public interface IntUnaryOperator {

    /// Applies this operator.
    ///
    /// @param operand the input
    /// @return the result
    int applyAsInt(int operand);

    /// `before`, then this operator on its result.
    ///
    /// @param before the operator applied first
    /// @return the composed operator
    default IntUnaryOperator compose(final IntUnaryOperator before) {
        Objects.requireNonNull(before);
        final IntUnaryOperator self = this;
        return new IntUnaryOperator() {
            public int applyAsInt(int value) {
                return self.applyAsInt(before.applyAsInt(value));
            }
        };
    }

    /// This operator, then `after` on its result.
    ///
    /// @param after the operator applied next
    /// @return the composed operator
    default IntUnaryOperator andThen(final IntUnaryOperator after) {
        Objects.requireNonNull(after);
        final IntUnaryOperator self = this;
        return new IntUnaryOperator() {
            public int applyAsInt(int value) {
                return after.applyAsInt(self.applyAsInt(value));
            }
        };
    }

    /// The operator that answers its operand.
    ///
    /// @return the identity operator
    static IntUnaryOperator identity() {
        return new IntUnaryOperator() {
            public int applyAsInt(int value) {
                return value;
            }
        };
    }
}
