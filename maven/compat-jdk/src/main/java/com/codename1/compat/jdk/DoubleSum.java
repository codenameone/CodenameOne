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
package com.codename1.compat.jdk;

/// A running sum of `double` values that carries the rounding error of each
/// addition along (Kahan summation), so that adding many small values to a
/// large one does not lose them.
final class DoubleSum {

    private double sum;
    private double compensation;
    /// The plain sum, which is what says "infinite" when the compensated
    /// one has turned into a NaN by subtracting two infinities.
    private double simple;

    void add(double value) {
        simple += value;
        double corrected = value - compensation;
        double next = sum + corrected;
        compensation = (next - sum) - corrected;
        sum = next;
    }

    void add(DoubleSum other) {
        double plain = simple + other.simple;
        add(other.sum);
        add(-other.compensation);
        simple = plain;
    }

    double value() {
        double out = sum - compensation;
        if (Double.isNaN(out) && Double.isInfinite(simple)) {
            return simple;
        }
        return out;
    }
}
