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

/// An enumerated observation with no natural unit -- menstrual flow, for
/// example.
///
/// The value is a small integer whose meaning depends on the type. Use the
/// constants each type documents rather than raw numbers; the ports map
/// them onto the platform's own encoding.
public final class CategorySample extends HealthSample {

    /// [HealthDataType#MENSTRUATION_FLOW]: the platform reported a flow
    /// but not its intensity.
    public static final int FLOW_UNSPECIFIED = 1;

    /// [HealthDataType#MENSTRUATION_FLOW]: light flow.
    public static final int FLOW_LIGHT = 2;

    /// [HealthDataType#MENSTRUATION_FLOW]: medium flow.
    public static final int FLOW_MEDIUM = 3;

    /// [HealthDataType#MENSTRUATION_FLOW]: heavy flow.
    public static final int FLOW_HEAVY = 4;

    private final int value;

    private CategorySample(HealthDataType type, int value, long startMillis,
            long endMillis) {
        super(type, startMillis, endMillis);
        if (type.getKind() != HealthDataKind.CATEGORY) {
            throw new IllegalArgumentException(type.getId() + " is a "
                    + type.getKind()
                    + " type and cannot be a CategorySample");
        }
        this.value = value;
    }

    /// An instantaneous observation.
    public static CategorySample create(HealthDataType type, int value,
            long instantMillis) {
        return new CategorySample(type, value, instantMillis, instantMillis);
    }

    /// An observation spanning an interval.
    public static CategorySample create(HealthDataType type, int value,
            long startMillis, long endMillis) {
        return new CategorySample(type, value, startMillis, endMillis);
    }

    /// The enumerated value, interpreted per this sample's type.
    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "CategorySample[" + getType().getId() + "=" + value + " "
                + getStartMillis() + ".." + getEndMillis() + "]";
    }
}
