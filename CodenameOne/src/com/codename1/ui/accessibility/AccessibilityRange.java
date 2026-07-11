/*
 * Copyright (c) 2026 Codename One and contributors. All rights reserved.
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
