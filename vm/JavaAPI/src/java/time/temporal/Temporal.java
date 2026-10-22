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

/// A date, a time or a moment that can be moved by an amount of time and
/// measured against another of its kind.
public interface Temporal extends TemporalAccessor {
    /// Whether this value can add or subtract `unit`.
    boolean isSupported(TemporalUnit unit);

    /// This value moved forward by `amountToAdd` of `unit`.
    Temporal plus(long amountToAdd, TemporalUnit unit);

    /// This value moved back by `amountToSubtract` of `unit`.
    default Temporal minus(long amountToSubtract, TemporalUnit unit) {
        if (amountToSubtract == Long.MIN_VALUE) {
            return plus(Long.MAX_VALUE, unit).plus(1, unit);
        }
        return plus(-amountToSubtract, unit);
    }

    /// This value moved forward by `amount`.
    default Temporal plus(TemporalAmount amount) {
        return amount.addTo(this);
    }

    /// This value moved back by `amount`.
    default Temporal minus(TemporalAmount amount) {
        return amount.subtractFrom(this);
    }

    /// The whole units of `unit` from this value to `endExclusive`.
    long until(Temporal endExclusive, TemporalUnit unit);
}
