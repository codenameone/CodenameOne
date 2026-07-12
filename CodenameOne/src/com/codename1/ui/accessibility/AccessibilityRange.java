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
package com.codename1.ui.accessibility;

/// Numeric value metadata for sliders, progress indicators, spin buttons, and
/// other adjustable controls.
public final class AccessibilityRange {
    private final double minimum;
    private final double maximum;
    private final double current;
    private final double step;
    private final String text;

    public AccessibilityRange(double minimum, double maximum, double current) {
        this(minimum, maximum, current, 0, null);
    }

    public AccessibilityRange(double minimum, double maximum, double current, double step) {
        this(minimum, maximum, current, step, null);
    }

    public AccessibilityRange(double minimum, double maximum, double current, double step, String text) {
        this.minimum = minimum;
        this.maximum = maximum;
        this.current = current;
        this.step = step;
        this.text = text;
    }

    public double getMinimum() {
        return minimum;
    }

    public double getMaximum() {
        return maximum;
    }

    public double getCurrent() {
        return current;
    }

    public double getStep() {
        return step;
    }

    public String getText() {
        return text;
    }

    public boolean isValid() {
        return !Double.isNaN(minimum) && !Double.isNaN(maximum) && !Double.isNaN(current)
                && minimum <= maximum && current >= minimum && current <= maximum && step >= 0;
    }
}
