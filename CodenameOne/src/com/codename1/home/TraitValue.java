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
package com.codename1.home;

/// One value of one [Trait]: immutable, typed, and carrying its unit.
///
/// ```java
/// TraitValue on = TraitValue.of(true);
/// TraitValue dim = TraitValue.of(40, TraitUnit.PERCENT);
/// TraitValue warm = TraitValue.of(370, TraitUnit.MIRED);
/// ```
///
/// #### A tagged union, not forty subclasses
///
/// Home values are genuinely heterogeneous -- a switch is a boolean, a dimmer
/// is a percentage, a lock is one of a fixed set -- and both native layers
/// hand them over as raw numbers. Something has to give them types back.
///
/// A class per trait would mean a cast at every read site, and this codebase
/// has a hard rule against a cast whose failure you expect to handle: ParparVM
/// does not check `CHECKCAST`, so on iOS a wrong cast does not throw, it hands
/// the wrong object to the next instruction and reads the target type's fields
/// out of it. One class with kind-checked getters turns the same mistake into
/// an [IllegalStateException] naming both kinds, on every platform.
///
/// #### There is no zero-argument getDouble
///
/// [#getDouble(TraitUnit)] makes you name the unit you expect and converts,
/// or throws if the dimensions disagree. That is inherited straight from
/// `com.codename1.health.HealthQuantity`, and the reason is the same: a bare
/// `getDouble()` is how a Celsius setpoint gets rendered as Fahrenheit and how
/// a colour temperature in mireds gets treated as Kelvin. Neither mistake
/// raises anything at the time.
///
/// [#getRawDouble()] is the escape hatch, named to be awkward enough that it
/// is not reached for by habit.
///
/// #### The raw platform value
///
/// Some mappings in this API are judgment calls -- HomeKit has six air-quality
/// levels and Matter has seven, and Matter has five thermostat modes HomeKit
/// cannot express. Where the canonical answer is lossy,
/// [#getRawPlatformValue()] carries the platform's own ordinal alongside it.
/// Without it every lossy mapping decision would be a permanent lie; with it,
/// an app that must be exact can be.
public final class TraitValue {

    private final TraitValueKind kind;
    private final double numeric;
    private final String text;
    private final TraitUnit unit;
    private final int rawPlatformValue;
    private final boolean hasRawPlatformValue;

    private TraitValue(TraitValueKind kind, double numeric, String text,
            TraitUnit unit, int rawPlatformValue,
            boolean hasRawPlatformValue) {
        this.kind = kind;
        this.numeric = numeric;
        this.text = text;
        this.unit = unit;
        this.rawPlatformValue = rawPlatformValue;
        this.hasRawPlatformValue = hasRawPlatformValue;
    }

    /// A boolean value -- a switch, a motion flag, a mute.
    ///
    /// #### Parameters
    ///
    /// - `value`: the boolean
    ///
    /// #### Returns
    ///
    /// a value of kind [TraitValueKind#BOOLEAN]
    public static TraitValue of(boolean value) {
        return new TraitValue(TraitValueKind.BOOLEAN, value ? 1 : 0, null,
                TraitUnit.NONE, 0, false);
    }

    /// A unitless whole number.
    ///
    /// #### Parameters
    ///
    /// - `value`: the number
    ///
    /// #### Returns
    ///
    /// a value of kind [TraitValueKind#INT]
    public static TraitValue of(int value) {
        return new TraitValue(TraitValueKind.INT, value, null, TraitUnit.NONE,
                0, false);
    }

    /// A measured quantity.
    ///
    /// #### Parameters
    ///
    /// - `value`: the quantity
    ///
    /// - `unit`: the unit it is expressed in
    ///
    /// #### Returns
    ///
    /// a value of kind [TraitValueKind#DOUBLE]
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when `unit` is `null`. A quantity with
    ///   no unit is the bug this class exists to prevent, so it is refused at
    ///   construction rather than defaulted to something plausible.
    ///
    /// - `IllegalArgumentException`: when `value` is NaN or an infinity.
    ///   Nothing downstream can carry one: the wire encodes a number as text
    ///   and its decoder refuses these as INVALID_DATA, so a write that got
    ///   this far was accepted, stored by the local backend, and then read
    ///   back as a failure -- and on a device it would be a number no
    ///   accessory could act on.
    public static TraitValue of(double value, TraitUnit unit) {
        if (unit == null) {
            throw new IllegalArgumentException(
                    "a measured value needs a unit; use TraitUnit.NONE only"
                            + " for a genuinely dimensionless quantity");
        }
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(
                    "a measured value has to be a real number, got " + value);
        }
        return new TraitValue(TraitValueKind.DOUBLE, value, null, unit, 0,
                false);
    }

    /// Free text.
    ///
    /// #### Parameters
    ///
    /// - `value`: the text; `null` becomes the empty string
    ///
    /// #### Returns
    ///
    /// a value of kind [TraitValueKind#STRING]
    public static TraitValue of(String value) {
        return new TraitValue(TraitValueKind.STRING, 0,
                value == null ? "" : value, TraitUnit.NONE, 0, false);
    }

    /// One of a fixed set, from one of this package's domain enums.
    ///
    /// #### Parameters
    ///
    /// - `value`: the constant
    ///
    /// #### Returns
    ///
    /// a value of kind [TraitValueKind#ENUM]
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when `value` is `null`
    public static TraitValue ofEnum(Enum<?> value) {
        if (value == null) {
            throw new IllegalArgumentException("enum value is required");
        }
        return new TraitValue(TraitValueKind.ENUM, value.ordinal(),
                value.name(), TraitUnit.NONE, 0, false);
    }

    /// Builds an enum value straight from an ordinal, for the codec.
    ///
    /// Application code should use [#ofEnum(java.lang.Enum)]; this exists
    /// because the wire carries an ordinal and the decoder has no constant to
    /// hand.
    ///
    /// #### Parameters
    ///
    /// - `ordinal`: the ordinal of a constant in this package's domain enum
    ///   for the trait in question
    ///
    /// #### Returns
    ///
    /// a value of kind [TraitValueKind#ENUM]
    public static TraitValue ofEnumOrdinal(int ordinal) {
        return new TraitValue(TraitValueKind.ENUM, ordinal, null,
                TraitUnit.NONE, 0, false);
    }

    /// This value with the platform's own ordinal attached.
    ///
    /// Used by the ports where the canonical mapping is lossy, so an app can
    /// see what the accessory actually said. See [#getRawPlatformValue()].
    ///
    /// #### Parameters
    ///
    /// - `raw`: the backend's own numeric value
    ///
    /// #### Returns
    ///
    /// a copy carrying the raw value; this instance is unchanged
    public TraitValue withRawPlatformValue(int raw) {
        return new TraitValue(kind, numeric, text, unit, raw, true);
    }

    /// What sort of value this is, and therefore which getter works.
    ///
    /// #### Returns
    ///
    /// the kind, never `null`
    public TraitValueKind getKind() {
        return kind;
    }

    /// The unit a [TraitValueKind#DOUBLE] value is expressed in.
    /// [TraitUnit#NONE] for every other kind.
    ///
    /// #### Returns
    ///
    /// the unit, never `null`
    public TraitUnit getUnit() {
        return unit;
    }

    /// The boolean.
    ///
    /// #### Returns
    ///
    /// the value
    ///
    /// #### Throws
    ///
    /// - `IllegalStateException`: when this is not a
    ///   [TraitValueKind#BOOLEAN]
    public boolean getBoolean() {
        require(TraitValueKind.BOOLEAN);
        return numeric != 0;
    }

    /// The whole number.
    ///
    /// #### Returns
    ///
    /// the value
    ///
    /// #### Throws
    ///
    /// - `IllegalStateException`: when this is not an [TraitValueKind#INT]
    public int getInt() {
        require(TraitValueKind.INT);
        return (int) numeric;
    }

    /// The text.
    ///
    /// #### Returns
    ///
    /// the value, never `null`
    ///
    /// #### Throws
    ///
    /// - `IllegalStateException`: when this is not a
    ///   [TraitValueKind#STRING]
    public String getString() {
        require(TraitValueKind.STRING);
        return text == null ? "" : text;
    }

    /// The ordinal of an enum value, for the codec and for
    /// [LockState#of(TraitValue)] and its siblings.
    ///
    /// Application code should go through those lookups rather than reading
    /// the ordinal: they are total, they name the constant, and they document
    /// which ones a given backend can never produce.
    ///
    /// #### Returns
    ///
    /// the ordinal
    ///
    /// #### Throws
    ///
    /// - `IllegalStateException`: when this is not an [TraitValueKind#ENUM]
    public int getEnumOrdinal() {
        require(TraitValueKind.ENUM);
        return (int) numeric;
    }

    /// The name of the enum constant this value was built from.
    ///
    /// `null` for a value the codec decoded from a wire ordinal, which has no
    /// constant in hand. Present for every value an application builds, which
    /// is what lets [Trait#acceptsEnumValue(TraitValue)] tell one domain enum
    /// from another -- ParparVM's `Enum.getDeclaringClass()` returns `null`,
    /// so the type itself is not available to check.
    ///
    /// #### Returns
    ///
    /// the constant's name, or `null`
    ///
    /// #### Throws
    ///
    /// - `IllegalStateException`: when this is not an [TraitValueKind#ENUM]
    public String getEnumName() {
        require(TraitValueKind.ENUM);
        return text;
    }

    /// The quantity, converted into the unit you name.
    ///
    /// #### Parameters
    ///
    /// - `in`: the unit you want the answer in
    ///
    /// #### Returns
    ///
    /// the quantity expressed in `in`
    ///
    /// #### Throws
    ///
    /// - `IllegalStateException`: when this is not a
    ///   [TraitValueKind#DOUBLE]
    ///
    /// - `IllegalArgumentException`: when `in` measures a different
    ///   dimension than this value
    public double getDouble(TraitUnit in) {
        require(TraitValueKind.DOUBLE);
        return TraitUnit.convert(numeric, unit, in);
    }

    /// The quantity in whatever unit it happens to be in, with no conversion
    /// and no check.
    ///
    /// Deliberately awkward. Reach for [#getDouble(TraitUnit)] unless you
    /// have already read [#getUnit()] and are doing something with both.
    ///
    /// #### Returns
    ///
    /// the raw numeric component
    ///
    /// #### Throws
    ///
    /// - `IllegalStateException`: when this is not a
    ///   [TraitValueKind#DOUBLE]
    public double getRawDouble() {
        require(TraitValueKind.DOUBLE);
        return numeric;
    }

    /// A colour temperature in Kelvin.
    ///
    /// Separate from [#getDouble(TraitUnit)] because mireds and Kelvin are
    /// reciprocal rather than affine and so cannot be a [TraitUnit] pair; see
    /// [TraitUnit#miredToKelvin(double)].
    ///
    /// #### Returns
    ///
    /// the colour temperature in Kelvin
    ///
    /// #### Throws
    ///
    /// - `IllegalStateException`: when this is not a
    ///   [TraitValueKind#DOUBLE]
    ///
    /// - `IllegalArgumentException`: when this value is not a colour
    ///   temperature, or is not positive
    public double getColorTemperatureKelvin() {
        require(TraitValueKind.DOUBLE);
        if (unit.getDimension() != TraitUnitDimension.COLOR_TEMPERATURE) {
            throw new IllegalArgumentException(unit.name()
                    + " is not a colour temperature");
        }
        return TraitUnit.miredToKelvin(
                TraitUnit.convert(numeric, unit, TraitUnit.MIRED));
    }

    /// Whether the backend's own numeric value is available alongside the
    /// canonical one.
    ///
    /// #### Returns
    ///
    /// `true` when [#getRawPlatformValue()] is meaningful
    public boolean hasRawPlatformValue() {
        return hasRawPlatformValue;
    }

    /// The backend's own numeric value, where the canonical mapping was
    /// lossy.
    ///
    /// Meaningful only when [#hasRawPlatformValue()] answers `true`, and
    /// meaningful only in terms of the backend that produced it -- read
    /// [SmartHome#getBackend()] before interpreting it. Zero otherwise.
    ///
    /// #### Returns
    ///
    /// the platform's own ordinal, or zero
    public int getRawPlatformValue() {
        return rawPlatformValue;
    }

    private void require(TraitValueKind expected) {
        if (kind != expected) {
            throw new IllegalStateException("this is a " + kind.name()
                    + " value, not a " + expected.name()
                    + "; check getKind() first");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TraitValue)) {
            return false;
        }
        TraitValue other = (TraitValue) o;
        if (kind != other.kind || unit != other.unit) {
            return false;
        }
        if (Double.compare(numeric, other.numeric) != 0) {
            return false;
        }
        if (hasRawPlatformValue != other.hasRawPlatformValue
                || rawPlatformValue != other.rawPlatformValue) {
            return false;
        }
        return text == null ? other.text == null : text.equals(other.text);
    }

    @Override
    public int hashCode() {
        int result = kind.hashCode();
        result = 31 * result + unit.hashCode();
        long bits = Double.doubleToLongBits(numeric);
        result = 31 * result + (int) (bits ^ (bits >>> 32));
        result = 31 * result + (text == null ? 0 : text.hashCode());
        result = 31 * result + (hasRawPlatformValue ? rawPlatformValue : 0);
        return result;
    }

    @Override
    public String toString() {
        switch (kind) {
            case BOOLEAN:
                return numeric != 0 ? "true" : "false";
            case INT:
                return Integer.toString((int) numeric);
            case STRING:
                return text == null ? "" : text;
            case ENUM:
                return text != null ? text : Integer.toString((int) numeric);
            default:
                return numeric + " " + unit.name();
        }
    }
}
