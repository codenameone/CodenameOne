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

/// A numeric measurement with a unit -- the most common kind of health
/// sample.
///
/// ```java
/// QuantitySample weight = QuantitySample.create(
///         HealthDataType.BODY_MASS,
///         new HealthQuantity(72.5, HealthUnit.KILOGRAM),
///         System.currentTimeMillis());
/// weight.setRecordingMethod(RecordingMethod.MANUAL_ENTRY);
/// ```
public final class QuantitySample extends HealthSample {

    private final HealthQuantity quantity;

    private QuantitySample(HealthDataType type, HealthQuantity quantity,
            long startMillis, long endMillis) {
        super(type, startMillis, endMillis);
        if (quantity == null) {
            throw new IllegalArgumentException(
                    "a quantity sample requires a quantity");
        }
        if (type.getKind() != HealthDataKind.QUANTITY) {
            throw new IllegalArgumentException(type.getId()
                    + " is a " + type.getKind()
                    + " type and cannot be a QuantitySample");
        }
        this.quantity = quantity;
    }

    /// An instantaneous measurement.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if the type is interval-only, such as
    ///   [HealthDataType#STEPS] -- a step count belongs to a span of time,
    ///   never to a moment.
    public static QuantitySample create(HealthDataType type,
            HealthQuantity quantity, long instantMillis) {
        if (type != null && type.isIntervalOnly()) {
            throw new IllegalArgumentException(type.getId()
                    + " accumulates over time and needs a start and an end;"
                    + " use create(type, quantity, start, end)");
        }
        return new QuantitySample(type, quantity, instantMillis,
                instantMillis);
    }

    /// A measurement spanning an interval.
    public static QuantitySample create(HealthDataType type,
            HealthQuantity quantity, long startMillis, long endMillis) {
        return new QuantitySample(type, quantity, startMillis, endMillis);
    }

    /// The measured value together with its unit. Read a number out of it
    /// with [HealthQuantity#getValue(HealthUnit)], which forces the unit
    /// to be named at the call site.
    public HealthQuantity getQuantity() {
        return quantity;
    }

    /// Shorthand for `getQuantity().getValue(unit)`.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if `unit` measures a different
    ///   dimension than this sample's value.
    public double getValue(HealthUnit unit) {
        return quantity.getValue(unit);
    }

    @Override
    public String toString() {
        return "QuantitySample[" + getType().getId() + " " + quantity
                + " " + getStartMillis() + ".." + getEndMillis() + "]";
    }
}
