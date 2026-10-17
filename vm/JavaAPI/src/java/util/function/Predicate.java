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

/// A test of one value.
///
/// @param <T> the input type
@FunctionalInterface
public interface Predicate<T> {

    /// Whether `t` passes.
    ///
    /// @param t the input
    /// @return true when it passes
    boolean test(T t);

    /// Passes when this and `other` both pass; `other` is not asked when this
    /// fails.
    ///
    /// @param other the second test
    /// @return the composed test
    default Predicate<T> and(final Predicate<? super T> other) {
        Objects.requireNonNull(other);
        final Predicate<T> self = this;
        return new Predicate<T>() {
            public boolean test(T t) {
                return self.test(t) && other.test(t);
            }
        };
    }

    /// Passes when this fails.
    ///
    /// @return the negated test
    default Predicate<T> negate() {
        final Predicate<T> self = this;
        return new Predicate<T>() {
            public boolean test(T t) {
                return !self.test(t);
            }
        };
    }

    /// Passes when this or `other` passes; `other` is not asked when this
    /// passes.
    ///
    /// @param other the second test
    /// @return the composed test
    default Predicate<T> or(final Predicate<? super T> other) {
        Objects.requireNonNull(other);
        final Predicate<T> self = this;
        return new Predicate<T>() {
            public boolean test(T t) {
                return self.test(t) || other.test(t);
            }
        };
    }

    /// Passes for values equal to `targetRef` by [Object#equals], or for null
    /// when `targetRef` is null.
    ///
    /// @param targetRef the value to compare with
    /// @param <T> the input type
    /// @return the test
    static <T> Predicate<T> isEqual(final Object targetRef) {
        return new Predicate<T>() {
            public boolean test(T t) {
                return targetRef == null ? t == null : targetRef.equals(t);
            }
        };
    }

    /// The negation of `target`.
    ///
    /// @param target the test to negate
    /// @param <T> the input type
    /// @return the negated test
    @SuppressWarnings("unchecked")
    static <T> Predicate<T> not(Predicate<? super T> target) {
        Objects.requireNonNull(target);
        return (Predicate<T>) target.negate();
    }
}
