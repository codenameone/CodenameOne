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
package com.codename1.fxcompat.runtime.css;

/// One parsed style value, in the form both the build and the device hold
/// it: numbers, strings and nested values, nothing that needs a scene graph
/// class. The build writes it into a compiled style sheet; the device reads
/// it back, or gets it straight from [CssValueParser] for an inline style,
/// and turns it into the object a node takes only when a node is styled --
/// that is when an `em` length has a font size to be relative to and a
/// looked-up colour has a cascade to be looked up in.
///
/// #### The types
///
/// - [#NUMBER]: `nums[0]` with the unit `units[0]`.
/// - [#SIZES]: one to four numbers, each with its unit: insets or radii as
///   written, before the CSS rule for missing sides is applied.
/// - [#COLOR]: `flags` holds the colour as `0xAARRGGBB`.
/// - [#LOOKUP]: `text` names a colour some rule defines.
/// - [#DERIVE]: `parts[0]` is a colour, `nums[0]` the brightness change in
///   percent.
/// - [#LINEAR]: `nums` holds start x, start y, end x, end y, then one
///   offset per stop (a fraction, or `NaN` for a stop without one);
///   `parts` holds the colour of each stop; `flags` is [#FLAG_PROPORTIONAL]
///   and the cycle method.
/// - [#RADIAL]: `nums` holds focus angle, focus distance, centre x, centre
///   y, radius, then the stop offsets; otherwise as [#LINEAR].
/// - [#STRING]: `text`, case kept.
/// - [#KEYWORD]: `text`, folded to lower case.
/// - [#BOOLEAN]: `flags` is 1 for true.
/// - [#FONT]: the `-fx-font` shorthand. `text` is the family, `nums[0]`
///   the size with `units[0]`, `flags` the weight (0 when not given) in
///   the low 16 bits and above them 1 for an upright and 2 for an italic
///   posture.
/// - [#URL]: `text` is the address as written.
///
/// A value is immutable; the arrays are never changed after construction.
public final class CssValue {

    /// A number, with a unit.
    public static final int NUMBER = 1;
    /// One to four numbers.
    public static final int SIZES = 2;
    /// A colour.
    public static final int COLOR = 3;
    /// The name of a colour defined by a rule.
    public static final int LOOKUP = 4;
    /// A colour made brighter or darker.
    public static final int DERIVE = 5;
    /// A linear gradient.
    public static final int LINEAR = 6;
    /// A radial gradient.
    public static final int RADIAL = 7;
    /// Text.
    public static final int STRING = 8;
    /// A keyword.
    public static final int KEYWORD = 9;
    /// A flag.
    public static final int BOOLEAN = 10;
    /// The font shorthand.
    public static final int FONT = 11;
    /// An address.
    public static final int URL = 12;

    /// No unit: a plain number, or an angle in degrees.
    public static final byte UNIT_NONE = 0;
    /// Logical pixels; every absolute unit is converted to it when parsed.
    public static final byte UNIT_PX = 1;
    /// Relative to the font size of the node.
    public static final byte UNIT_EM = 2;
    /// A percentage, kept as written (`50` for `50%`).
    public static final byte UNIT_PERCENT = 3;

    /// Set in the `flags` of a gradient whose geometry is relative to the
    /// shape it fills.
    public static final int FLAG_PROPORTIONAL = 1;
    /// Set in the `flags` of a gradient that reflects.
    public static final int FLAG_REFLECT = 2;
    /// Set in the `flags` of a gradient that repeats.
    public static final int FLAG_REPEAT = 4;

    private static final double[] NO_NUMS = new double[0];
    private static final byte[] NO_UNITS = new byte[0];
    private static final CssValue[] NO_PARTS = new CssValue[0];

    private final int type;
    private final int flags;
    private final double[] nums;
    private final byte[] units;
    private final String text;
    private final CssValue[] parts;
    private final boolean contextual;

    /// Creates a value from all of its fields; `null` arrays stand for empty
    /// ones. The arrays are taken as they are: the caller gives them up.
    public CssValue(int type, int flags, double[] nums, byte[] units, String text, CssValue[] parts) {
        this.type = type;
        this.flags = flags;
        this.nums = nums == null ? NO_NUMS : nums;
        boolean anyUnit = false;
        for (int i = 0; units != null && i < units.length; i++) {
            anyUnit |= units[i] != UNIT_NONE;
        }
        // No array at all for numbers without units, so that a value read
        // back from a compiled sheet equals the one that was written.
        this.units = anyUnit ? units : NO_UNITS;
        this.text = text;
        this.parts = parts == null ? NO_PARTS : parts;
        boolean ctx = type == LOOKUP;
        for (int i = 0; i < this.units.length; i++) {
            ctx |= this.units[i] == UNIT_EM;
        }
        if (type == FONT || type == NUMBER) {
            // A percentage font size is relative to the inherited size.
            ctx |= this.units.length > 0 && this.units[0] == UNIT_PERCENT;
        }
        for (int i = 0; i < this.parts.length; i++) {
            ctx |= this.parts[i] != null && this.parts[i].contextual;
        }
        this.contextual = ctx;
    }

    /// A number with a unit.
    public static CssValue number(double value, byte unit) {
        return new CssValue(NUMBER, 0, new double[] {value}, new byte[] {unit}, null, null);
    }

    /// A colour from `0xAARRGGBB`.
    public static CssValue color(int argb) {
        return new CssValue(COLOR, argb, null, null, null, null);
    }

    /// A value that is only text: [#STRING], [#KEYWORD], [#LOOKUP] or
    /// [#URL].
    public static CssValue text(int type, String text) {
        return new CssValue(type, 0, null, null, text, null);
    }

    /// A flag.
    public static CssValue bool(boolean value) {
        return new CssValue(BOOLEAN, value ? 1 : 0, null, null, null, null);
    }

    /// The type, one of the constants of this class.
    public int type() {
        return type;
    }

    /// The colour, flag, or gradient and font bits; see the class
    /// description.
    public int flags() {
        return flags;
    }

    /// How many numbers the value holds.
    public int count() {
        return nums.length;
    }

    /// The number at `index`.
    public double num(int index) {
        return nums[index];
    }

    /// The unit of the number at `index`, [#UNIT_NONE] where the type keeps
    /// none.
    public byte unit(int index) {
        return index < units.length ? units[index] : UNIT_NONE;
    }

    /// The text, or `null` for a type without any.
    public String text() {
        return text;
    }

    /// How many nested values the value holds.
    public int partCount() {
        return parts.length;
    }

    /// The nested value at `index`.
    public CssValue part(int index) {
        return parts[index];
    }

    /// Whether what this value stands for depends on the node it styles:
    /// it has an `em` length, a percentage of the inherited font size, or
    /// names a looked-up colour. Any other value becomes the same object
    /// for every node, and that object can be kept.
    public boolean isContextual() {
        return contextual;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof CssValue)) {
            return false;
        }
        CssValue v = (CssValue) o;
        if (type != v.type || flags != v.flags || nums.length != v.nums.length || units.length != v.units.length
                || parts.length != v.parts.length || !(text == null ? v.text == null : text.equals(v.text))) {
            return false;
        }
        for (int i = 0; i < nums.length; i++) {
            if (Double.doubleToLongBits(nums[i]) != Double.doubleToLongBits(v.nums[i])) {
                return false;
            }
        }
        for (int i = 0; i < units.length; i++) {
            if (units[i] != v.units[i]) {
                return false;
            }
        }
        for (int i = 0; i < parts.length; i++) {
            if (parts[i] == null ? v.parts[i] != null : !parts[i].equals(v.parts[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int h = type * 31 + flags;
        for (int i = 0; i < nums.length; i++) {
            long bits = Double.doubleToLongBits(nums[i]);
            h = h * 31 + (int) (bits ^ (bits >>> 32));
        }
        if (text != null) {
            h = h * 31 + text.hashCode();
        }
        for (int i = 0; i < parts.length; i++) {
            h = h * 31 + (parts[i] == null ? 0 : parts[i].hashCode());
        }
        return h;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("CssValue[").append(type);
        if (flags != 0) {
            s.append(" 0x").append(Integer.toHexString(flags));
        }
        for (int i = 0; i < nums.length; i++) {
            s.append(' ').append(nums[i]);
            if (i < units.length && units[i] != UNIT_NONE) {
                s.append(units[i] == UNIT_PX ? "px" : units[i] == UNIT_EM ? "em" : "%");
            }
        }
        if (text != null) {
            s.append(" '").append(text).append('\'');
        }
        for (int i = 0; i < parts.length; i++) {
            s.append(' ').append(parts[i]);
        }
        return s.append(']').toString();
    }
}
