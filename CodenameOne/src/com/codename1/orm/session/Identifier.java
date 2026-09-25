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
package com.codename1.orm.session;

import com.codename1.impl.orm.Values;
import java.util.Arrays;

/// Immutable composite identity, in the model's declared identifier-field order.
public final class Identifier {
    private final Object[] values;
    private Identifier(Object[] values) {
        this.values = values.clone();
        for (int i = 0; i < this.values.length; i++) {
            Object value = Values.storage(this.values[i]);
            if (value instanceof Integer || value instanceof Short || value instanceof Byte) {
                value = Long.valueOf(((Number) value).longValue());
            }
            if (value instanceof byte[]) {
                throw new IllegalArgumentException("Binary composite keys are not supported");
            }
            this.values[i] = value;
        }
    }
    /// Creates a composite identifier, normalizing scalar storage representations.
    /// Integral byte, short, and int values compare as longs. Copies the supplied
    /// array; components should be immutable. Binary components are unsupported.
    /// @param values key components in declared identifier-field order
    /// @return composite identifier for session lookup
    /// @throws IllegalArgumentException if a component is binary
    public static Identifier of(Object... values) {
        return new Identifier(values);
    }
    /// Returns a copy of the normalized key component array.
    /// @return components in identifier-field order
    public Object[] values() {
        return values.clone();
    }
    /// Compares normalized components in identifier-field order.
    /// @param other object to compare
    /// @return true if the other identifier has equal components
    @Override
    public boolean equals(Object other) {
        return other instanceof Identifier && Arrays.equals(values, ((Identifier) other).values);
    }
    /// Computes a hash from the normalized key components.
    /// @return component-based hash code
    @Override
    public int hashCode() {
        return Arrays.hashCode(values);
    }
}
