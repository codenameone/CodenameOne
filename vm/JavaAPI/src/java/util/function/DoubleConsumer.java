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

/// An operation on one `double` that answers nothing; the `double` form of [Consumer].
@FunctionalInterface
public interface DoubleConsumer {

    /// Performs this operation on `value`.
    ///
    /// @param value the input
    void accept(double value);

    /// This operation followed by `after`, both given the same value. If this
    /// operation throws, `after` does not run.
    ///
    /// @param after the operation to run next
    /// @return the composed operation
    default DoubleConsumer andThen(final DoubleConsumer after) {
        Objects.requireNonNull(after);
        final DoubleConsumer self = this;
        return new DoubleConsumer() {
            public void accept(double value) {
                self.accept(value);
                after.accept(value);
            }
        };
    }
}
