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

/// An operation on one `long` that answers a `long`; the `long` form of [UnaryOperator].
@FunctionalInterface
public interface LongUnaryOperator {

    /// Applies this operator.
    ///
    /// @param operand the input
    /// @return the result
    long applyAsLong(long operand);

    /// `before`, then this operator on its result.
    ///
    /// @param before the operator applied first
    /// @return the composed operator
    default LongUnaryOperator compose(final LongUnaryOperator before) {
        Objects.requireNonNull(before);
        final LongUnaryOperator self = this;
        return new LongUnaryOperator() {
            public long applyAsLong(long value) {
                return self.applyAsLong(before.applyAsLong(value));
            }
        };
    }

    /// This operator, then `after` on its result.
    ///
    /// @param after the operator applied next
    /// @return the composed operator
    default LongUnaryOperator andThen(final LongUnaryOperator after) {
        Objects.requireNonNull(after);
        final LongUnaryOperator self = this;
        return new LongUnaryOperator() {
            public long applyAsLong(long value) {
                return after.applyAsLong(self.applyAsLong(value));
            }
        };
    }

    /// The operator that answers its operand.
    ///
    /// @return the identity operator
    static LongUnaryOperator identity() {
        return new LongUnaryOperator() {
            public long applyAsLong(long value) {
                return value;
            }
        };
    }
}
