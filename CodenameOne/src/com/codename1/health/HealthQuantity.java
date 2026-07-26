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
package com.codename1.health;

/// An immutable value paired with the [HealthUnit] it is expressed in.
///
/// There is deliberately **no zero-argument `getValue()`**. Reading a
/// number out of a quantity forces the caller to name the unit they want
/// it in, which removes an entire class of bug -- the pounds-read-as-
/// kilograms mistake that silently scales every downstream chart by 2.2.
///
/// ```java
/// HealthQuantity weight = sample.getQuantity();
/// double kg = weight.getValue(HealthUnit.KILOGRAM);
/// double lb = weight.getValue(HealthUnit.POUND);
/// ```
public final class HealthQuantity {

    private final double value;
    private final HealthUnit unit;

    /// Creates a quantity.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if `unit` is null.
    public HealthQuantity(double value, HealthUnit unit) {
        if (unit == null) {
            throw new IllegalArgumentException("a quantity requires a unit");
        }
        this.value = value;
        this.unit = unit;
    }

    /// The unit this quantity was created with.
    public HealthUnit getUnit() {
        return unit;
    }

    /// The value converted into `in`.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if `in` measures a different dimension.
    public double getValue(HealthUnit in) {
        return HealthUnit.convert(value, unit, in);
    }

    /// The value in [#getUnit()], without conversion. Named so that it
    /// cannot be reached for by accident when [#getValue(HealthUnit)] is
    /// what the caller actually wants.
    public double getRawValue() {
        return value;
    }

    /// This quantity re-expressed in `unit`. Returns `this` when the unit
    /// already matches.
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    public HealthQuantity in(HealthUnit unit) {
        if (unit == this.unit) {
            return this;
        }
        return new HealthQuantity(getValue(unit), unit);
    }

    /// Two quantities are equal when they carry the same raw value and the
    /// same unit. Deliberately **not** conversion-aware: 1000 g and 1 kg
    /// are not `equals`, because making them so would give a class with
    /// floating-point-dependent equality and a hash code that cannot be
    /// consistent with it.
    @Override
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HealthQuantity)) {
            return false;
        }
        HealthQuantity other = (HealthQuantity) o;
        return unit == other.unit
                && Double.doubleToLongBits(value)
                        == Double.doubleToLongBits(other.value);
    }

    @Override
    public int hashCode() {
        long bits = Double.doubleToLongBits(value);
        return 31 * ((int) (bits ^ (bits >>> 32))) + unit.getSymbol().hashCode();
    }

    @Override
    public String toString() {
        return value + " " + unit.getSymbol();
    }
}
