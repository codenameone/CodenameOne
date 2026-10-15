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
package java.time.temporal;

import java.time.Duration;

/// A unit time is measured in. `ChronoUnit` holds the units of the ISO
/// calendar.
public interface TemporalUnit {
    /// The length of one unit; an estimate for the units a calendar decides.
    Duration getDuration();

    /// Whether the length is an estimate.
    boolean isDurationEstimated();

    /// Whether this is a unit of a date: a day or longer.
    boolean isDateBased();

    /// Whether this is a unit of a time of day: shorter than a day.
    boolean isTimeBased();

    /// Whether `temporal` can add or subtract this unit.
    default boolean isSupportedBy(Temporal temporal) {
        return temporal.isSupported(this);
    }

    /// `temporal` moved by `amount` of this unit.
    <R extends Temporal> R addTo(R temporal, long amount);

    /// How many whole units lie between two values of the same kind.
    long between(Temporal temporal1Inclusive, Temporal temporal2Exclusive);
}
