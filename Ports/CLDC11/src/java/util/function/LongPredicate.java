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

/// A test of one `long`; the `long` form of [Predicate].
@FunctionalInterface
public interface LongPredicate {

    /// Whether `value` passes.
    ///
    /// @param value the input
    /// @return true when it passes
    boolean test(long value);

    /// Passes when this and `other` both pass; `other` is not asked when this
    /// fails.
    ///
    /// @param other the second test
    /// @return the composed test
    default LongPredicate and(final LongPredicate other) {
        Objects.requireNonNull(other);
        final LongPredicate self = this;
        return new LongPredicate() {
            public boolean test(long value) {
                return self.test(value) && other.test(value);
            }
        };
    }

    /// Passes when this fails.
    ///
    /// @return the negated test
    default LongPredicate negate() {
        final LongPredicate self = this;
        return new LongPredicate() {
            public boolean test(long value) {
                return !self.test(value);
            }
        };
    }

    /// Passes when this or `other` passes; `other` is not asked when this
    /// passes.
    ///
    /// @param other the second test
    /// @return the composed test
    default LongPredicate or(final LongPredicate other) {
        Objects.requireNonNull(other);
        final LongPredicate self = this;
        return new LongPredicate() {
            public boolean test(long value) {
                return self.test(value) || other.test(value);
            }
        };
    }
}
