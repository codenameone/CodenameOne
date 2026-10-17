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
package com.codename1.desktopcompat.javax.swing;

import com.codename1.desktopcompat.java.awt.Component;

/// A length that can stretch and shrink: a minimum, a preferred and a
/// maximum value, and a current value somewhere around them.
/// [SpringLayout] describes every edge and size of a component as one.
///
/// Springs are combined arithmetically. A sum spreads a value it is
/// given over its two operands in proportion to how far each can move
/// from its preferred value; a maximum hands the value to both; a
/// negation and a scale map it back through their arithmetic.
///
/// Setting the value to `UNSET` clears it, after which the value reads as
/// the preferred value again.
public abstract class Spring {

    /// The value of a spring that has none.
    public static final int UNSET = Integer.MIN_VALUE;

    /// Used by the factory methods.
    protected Spring() {
    }

    /// Returns the minimum value of this spring.
    public abstract int getMinimumValue();

    /// Returns the preferred value of this spring.
    public abstract int getPreferredValue();

    /// Returns the maximum value of this spring.
    public abstract int getMaximumValue();

    /// Returns the current value of this spring.
    public abstract int getValue();

    /// Sets the current value of this spring; `UNSET` clears it.
    public abstract void setValue(int value);

    // How far the spring can move from its preferred value, towards its
    // minimum when contracting and towards its maximum otherwise.
    private int cn1Range(boolean contract) {
        return contract ? getPreferredValue() - getMinimumValue() : getMaximumValue() - getPreferredValue();
    }

    // The distance of the value from the preferred value, as a fraction
    // of the range on that side.
    double cn1Strain() {
        double delta = getValue() - getPreferredValue();
        double range = cn1Range(getValue() < getPreferredValue());
        if (range == 0) {
            // What the division answers in IEEE arithmetic, spelled out so
            // that no platform has to agree on dividing by zero.
            if (delta == 0) {
                return Double.NaN;
            }
            return delta > 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
        }
        return delta / range;
    }

    void cn1SetStrain(double strain) {
        setValue(getPreferredValue() + cn1ToInt(strain * cn1Range(strain < 0)));
    }

    // A narrowing conversion with the Java language's answers for the
    // values a native cast leaves undefined.
    private static int cn1ToInt(double d) {
        if (d != d) {
            return 0;
        }
        if (d >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (d <= Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) d;
    }

    // Whether this spring is defined, however indirectly, in terms of
    // itself. Only springs built from others can be.
    boolean cn1IsCyclic(SpringLayout layout) {
        return false;
    }

    /// Returns a spring whose minimum, preferred and maximum values are
    /// all `pref`.
    public static Spring constant(int pref) {
        return constant(pref, pref, pref);
    }

    /// Returns a spring with the given minimum, preferred and maximum
    /// values.
    public static Spring constant(int min, int pref, int max) {
        return new Fixed(min, pref, max);
    }

    /// Returns `-s`: a spring running in the opposite direction.
    public static Spring minus(Spring s) {
        return new Negated(s);
    }

    /// Returns `s1 + s2`. A value set on the sum is divided between the
    /// two so that both are strained equally.
    public static Spring sum(Spring s1, Spring s2) {
        return new Sum(s1, s2);
    }

    /// Returns `max(s1, s2)`: a spring whose every property is the larger
    /// of the two.
    public static Spring max(Spring s1, Spring s2) {
        return new Larger(s1, s2);
    }

    /// Returns a spring whose properties are those of `s` multiplied by
    /// `factor`; a negative factor swaps the minimum and the maximum.
    public static Spring scale(Spring s, float factor) {
        if (s == null) {
            throw new NullPointerException("Argument must not be null");
        }
        return new Scaled(s, factor);
    }

    /// Returns a spring that follows the minimum, preferred and maximum
    /// width of a component.
    public static Spring width(Component c) {
        if (c == null) {
            throw new NullPointerException("Argument must not be null");
        }
        return new SizeOf(c, true);
    }

    /// Returns a spring that follows the minimum, preferred and maximum
    /// height of a component.
    public static Spring height(Component c) {
        if (c == null) {
            throw new NullPointerException("Argument must not be null");
        }
        return new SizeOf(c, false);
    }

    /// A spring that keeps a value of its own.
    abstract static class Valued extends Spring {
        int size = UNSET;

        @Override
        public int getValue() {
            return size != UNSET ? size : getPreferredValue();
        }

        @Override
        public final void setValue(int value) {
            if (size == value) {
                return;
            }
            if (value == UNSET) {
                cleared();
            } else {
                assigned(value);
            }
        }

        void cleared() {
            size = UNSET;
        }

        void assigned(int value) {
            size = value;
        }
    }

    private static final class Fixed extends Valued {
        private final int min;
        private final int pref;
        private final int max;

        Fixed(int min, int pref, int max) {
            this.min = min;
            this.pref = pref;
            this.max = max;
        }

        @Override
        public int getMinimumValue() {
            return min;
        }

        @Override
        public int getPreferredValue() {
            return pref;
        }

        @Override
        public int getMaximumValue() {
            return max;
        }

        @Override
        public String toString() {
            return "StaticSpring [" + min + ", " + pref + ", " + max + "]";
        }
    }

    /// The width or the height of a component.
    static final class SizeOf extends Valued {
        final Component c;
        private final boolean horizontal;

        SizeOf(Component c, boolean horizontal) {
            this.c = c;
            this.horizontal = horizontal;
        }

        @Override
        public int getMinimumValue() {
            return horizontal ? c.getMinimumSize().width : c.getMinimumSize().height;
        }

        @Override
        public int getPreferredValue() {
            return horizontal ? c.getPreferredSize().width : c.getPreferredSize().height;
        }

        @Override
        public int getMaximumValue() {
            // A component with no maximum answers a huge one; capped so
            // that sums of several stay in range.
            return Math.min(Short.MAX_VALUE, horizontal ? c.getMaximumSize().width : c.getMaximumSize().height);
        }
    }

    private static final class Negated extends Spring {
        private final Spring s;

        Negated(Spring s) {
            this.s = s;
        }

        @Override
        public int getMinimumValue() {
            return -s.getMaximumValue();
        }

        @Override
        public int getPreferredValue() {
            return -s.getPreferredValue();
        }

        @Override
        public int getMaximumValue() {
            return -s.getMinimumValue();
        }

        @Override
        public int getValue() {
            return -s.getValue();
        }

        @Override
        public void setValue(int size) {
            // The negation of UNSET is UNSET, so clearing passes through.
            s.setValue(-size);
        }

        @Override
        boolean cn1IsCyclic(SpringLayout layout) {
            return s.cn1IsCyclic(layout);
        }
    }

    private static final class Scaled extends Spring {
        private final Spring s;
        private final float factor;

        Scaled(Spring s, float factor) {
            this.s = s;
            this.factor = factor;
        }

        @Override
        public int getMinimumValue() {
            return Math.round((factor < 0 ? s.getMaximumValue() : s.getMinimumValue()) * factor);
        }

        @Override
        public int getPreferredValue() {
            return Math.round(s.getPreferredValue() * factor);
        }

        @Override
        public int getMaximumValue() {
            return Math.round((factor < 0 ? s.getMinimumValue() : s.getMaximumValue()) * factor);
        }

        @Override
        public int getValue() {
            return Math.round(s.getValue() * factor);
        }

        @Override
        public void setValue(int value) {
            if (value == UNSET) {
                s.setValue(UNSET);
            } else {
                s.setValue(Math.round(value / factor));
            }
        }

        @Override
        boolean cn1IsCyclic(SpringLayout layout) {
            return s.cn1IsCyclic(layout);
        }
    }

    /// A spring seen through a pair of functions that are each other's
    /// inverse: the properties read through `map`, a value set goes back
    /// through `inv`.
    abstract static class Mapped extends Spring {
        private final Spring s;

        Mapped(Spring s) {
            this.s = s;
        }

        abstract int map(int i);

        abstract int inv(int i);

        @Override
        public int getMinimumValue() {
            return map(s.getMinimumValue());
        }

        @Override
        public int getPreferredValue() {
            return map(s.getPreferredValue());
        }

        @Override
        public int getMaximumValue() {
            return Math.min(Short.MAX_VALUE, map(s.getMaximumValue()));
        }

        @Override
        public int getValue() {
            return map(s.getValue());
        }

        @Override
        public void setValue(int value) {
            if (value == UNSET) {
                s.setValue(UNSET);
            } else {
                s.setValue(inv(value));
            }
        }

        @Override
        boolean cn1IsCyclic(SpringLayout layout) {
            return s.cn1IsCyclic(layout);
        }
    }

    /// A spring computed from two others. The properties are computed
    /// once and kept until the value is cleared.
    private abstract static class Pair extends Valued {
        final Spring s1;
        final Spring s2;
        private int min = UNSET;
        private int pref = UNSET;
        private int max = UNSET;

        Pair(Spring s1, Spring s2) {
            this.s1 = s1;
            this.s2 = s2;
        }

        abstract int op(int x, int y);

        @Override
        void cleared() {
            super.cleared();
            min = UNSET;
            pref = UNSET;
            max = UNSET;
            s1.setValue(UNSET);
            s2.setValue(UNSET);
        }

        @Override
        public int getMinimumValue() {
            if (min == UNSET) {
                min = op(s1.getMinimumValue(), s2.getMinimumValue());
            }
            return min;
        }

        @Override
        public int getPreferredValue() {
            if (pref == UNSET) {
                pref = op(s1.getPreferredValue(), s2.getPreferredValue());
            }
            return pref;
        }

        @Override
        public int getMaximumValue() {
            if (max == UNSET) {
                max = op(s1.getMaximumValue(), s2.getMaximumValue());
            }
            return max;
        }

        @Override
        public int getValue() {
            if (size == UNSET) {
                size = op(s1.getValue(), s2.getValue());
            }
            return size;
        }

        @Override
        boolean cn1IsCyclic(SpringLayout layout) {
            return layout.cn1IsCyclic(s1) || layout.cn1IsCyclic(s2);
        }
    }

    private static final class Sum extends Pair {
        Sum(Spring s1, Spring s2) {
            super(s1, s2);
        }

        @Override
        int op(int x, int y) {
            return x + y;
        }

        @Override
        void assigned(int value) {
            super.assigned(value);
            // The second operand takes what the first leaves, so that the
            // two add up to the value exactly whatever the rounding.
            s1.cn1SetStrain(cn1Strain());
            s2.setValue(value - s1.getValue());
        }
    }

    private static final class Larger extends Pair {
        Larger(Spring s1, Spring s2) {
            super(s1, s2);
        }

        @Override
        int op(int x, int y) {
            return Math.max(x, y);
        }

        @Override
        void assigned(int value) {
            super.assigned(value);
            s1.setValue(value);
            s2.setValue(value);
        }
    }
}
