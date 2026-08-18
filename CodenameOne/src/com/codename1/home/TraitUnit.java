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

/// A unit of measure for a [Trait] value.
///
/// #### Conversion is affine, with one honest exception
///
/// `canonical = value * scale + offset`, the same model
/// `com.codename1.health.HealthUnit` uses, because temperature needs the
/// offset. Converting between units of different
/// [TraitUnitDimension]s throws [IllegalArgumentException]: that is a bug in
/// the calling code, not a condition to report through an `AsyncResource`.
///
/// **Mireds and Kelvin are reciprocal, not affine**, so Kelvin is deliberately
/// not a constant here -- it cannot be expressed in the table, and forcing it
/// in would produce a colour temperature that is silently wrong rather than
/// obviously wrong. Use [#miredToKelvin(double)] and
/// [#kelvinToMired(double)], or [TraitValue#getColorTemperatureKelvin()],
/// which are named so that stepping outside the conversion table is a visible
/// act.
///
/// And the trap that catches everyone once: **a higher mired value is a warmer
/// light**, because mireds are the reciprocal of Kelvin. 153 mireds is a cold
/// blue-white; 400 is candlelight.
///
/// #### Why this is an enum where HealthUnit is a class
///
/// `HealthUnit` is an interned final class because its symbol is the wire
/// format Apple's `HKUnit(from:)` parses, and because the set grows with the
/// data types. Neither is true here: the set is small, closed and defined by
/// what the two backends can actually express, and nothing about a home
/// accessory will need a unit that is not already on this list. An enum gets
/// `switch` and exhaustive reasoning for free.
///
/// The one thing an enum does not get for free is a stable wire form --
/// `ordinal()` shifts the moment a constant is inserted in the middle -- so
/// each carries an explicit [#getWireId()] and [#forWireId(int)] resolves it.
public enum TraitUnit {

    /// No unit: an ordinal, a count, a plain number. The canonical unit of
    /// [TraitUnitDimension#DIMENSIONLESS].
    NONE(0, TraitUnitDimension.DIMENSIONLESS, 1, 0),

    /// Percent, 0 to 100. The canonical unit of
    /// [TraitUnitDimension#RATIO], and the unit of every proportion in this
    /// API -- brightness, saturation, covering position, fan speed, battery
    /// level, humidity.
    ///
    /// Both backends carry these as scaled integers with different scales
    /// (Matter's level control is 0 to 254, its covering position is 0 to
    /// 10000, its battery percentage is in halves) and every one of those
    /// conversions happens inside the port. Application code sees percent.
    PERCENT(1, TraitUnitDimension.RATIO, 1, 0),

    /// Degrees Celsius. The canonical unit of
    /// [TraitUnitDimension#TEMPERATURE].
    CELSIUS(2, TraitUnitDimension.TEMPERATURE, 1, 0),

    /// Degrees Fahrenheit.
    FAHRENHEIT(3, TraitUnitDimension.TEMPERATURE, 5.0 / 9.0, -160.0 / 9.0),

    /// Degrees of arc, 0 to 360. The canonical unit of
    /// [TraitUnitDimension#ANGLE], used for colour hue.
    ARC_DEGREE(4, TraitUnitDimension.ANGLE, 1, 0),

    /// Mireds -- micro reciprocal degrees, the reciprocal of colour
    /// temperature in Kelvin scaled by a million. The canonical unit of
    /// [TraitUnitDimension#COLOR_TEMPERATURE].
    ///
    /// Chosen over Kelvin because it is what **both** platforms use natively:
    /// HomeKit's `HMCharacteristicTypeColorTemperature` is in mireds and
    /// Matter's `ColorTemperatureMireds` is in mireds. Making Kelvin
    /// canonical would mean a reciprocal on every read and every write, in
    /// both ports, for no gain.
    MIRED(5, TraitUnitDimension.COLOR_TEMPERATURE, 1, 0),

    /// Lux. The canonical unit of [TraitUnitDimension#ILLUMINANCE].
    LUX(6, TraitUnitDimension.ILLUMINANCE, 1, 0),

    /// Parts per million. The canonical unit of
    /// [TraitUnitDimension#CONCENTRATION_PARTS].
    PPM(7, TraitUnitDimension.CONCENTRATION_PARTS, 1, 0),

    /// Parts per billion.
    PPB(8, TraitUnitDimension.CONCENTRATION_PARTS, 0.001, 0),

    /// Micrograms per cubic metre. The canonical unit of
    /// [TraitUnitDimension#CONCENTRATION_MASS], used for particulate matter.
    MICROGRAM_PER_CUBIC_METER(9, TraitUnitDimension.CONCENTRATION_MASS, 1, 0);

    private final int wireId;
    private final TraitUnitDimension dimension;
    private final double scale;
    private final double offset;

    TraitUnit(int wireId, TraitUnitDimension dimension, double scale,
            double offset) {
        this.wireId = wireId;
        this.dimension = dimension;
        this.scale = scale;
        this.offset = offset;
    }

    /// The stable identifier this unit crosses the native boundary as.
    ///
    /// Fixed per constant and never reused, so inserting a unit into the
    /// middle of this list cannot silently re-label values a port already
    /// sends.
    ///
    /// #### Returns
    ///
    /// the wire identifier
    public int getWireId() {
        return wireId;
    }

    /// What this unit measures.
    ///
    /// #### Returns
    ///
    /// the dimension, never `null`
    public TraitUnitDimension getDimension() {
        return dimension;
    }

    /// Whether a value in this unit can be converted into the supplied one.
    ///
    /// #### Parameters
    ///
    /// - `other`: the target unit, or `null`
    ///
    /// #### Returns
    ///
    /// `true` when both measure the same dimension
    public boolean isCompatibleWith(TraitUnit other) {
        return other != null && other.dimension == dimension;
    }

    /// Converts a value between two units of the same dimension.
    ///
    /// #### Parameters
    ///
    /// - `value`: the quantity to convert
    ///
    /// - `from`: the unit `value` is expressed in
    ///
    /// - `to`: the unit to express it in
    ///
    /// #### Returns
    ///
    /// the converted quantity
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when either unit is `null` or they
    ///   measure different dimensions
    public static double convert(double value, TraitUnit from, TraitUnit to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException(
                    "both units are required to convert");
        }
        if (from.dimension != to.dimension) {
            throw new IllegalArgumentException("cannot convert "
                    + from.name() + " (" + from.dimension.name() + ") to "
                    + to.name() + " (" + to.dimension.name()
                    + "): different dimensions");
        }
        if (from == to) {
            return value;
        }
        double canonical = value * from.scale + from.offset;
        return (canonical - to.offset) / to.scale;
    }

    /// Resolves a unit by its wire identifier, total: an unrecognized id
    /// answers `null` rather than throwing, so a value from a newer port
    /// degrades to "no value" instead of taking down the decode.
    ///
    /// #### Parameters
    ///
    /// - `wireId`: an identifier previously returned by [#getWireId()]
    ///
    /// #### Returns
    ///
    /// the matching unit, or `null`
    public static TraitUnit forWireId(int wireId) {
        for (TraitUnit candidate : values()) {
            if (candidate.wireId == wireId) {
                return candidate;
            }
        }
        return null;
    }

    /// Converts mireds to Kelvin.
    ///
    /// Outside the [#convert(double, TraitUnit, TraitUnit)] table on purpose:
    /// the relationship is reciprocal, and an affine table that pretended
    /// otherwise would return a plausible number that is wrong everywhere
    /// except at one point.
    ///
    /// #### Parameters
    ///
    /// - `mireds`: a colour temperature in mireds; must be greater than zero
    ///
    /// #### Returns
    ///
    /// the same colour temperature in Kelvin
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when `mireds` is zero, negative or
///   not a number
    public static double miredToKelvin(double mireds) {
        // NaN named on its own, because it compares false against every
        // operator: `mireds <= 0` lets it straight through, and a reciprocal of
        // NaN is NaN, so a bad reading became a colour temperature that
        // spread through every calculation it touched instead of failing
        // here.
        if (Double.isNaN(mireds) || mireds <= 0) {
            throw new IllegalArgumentException(
                    "mireds must be positive, got " + mireds);
        }
        return 1000000.0 / mireds;
    }

    /// Converts Kelvin to mireds. See [#miredToKelvin(double)] for why this
    /// is a named method rather than a table entry.
    ///
    /// #### Parameters
    ///
    /// - `kelvin`: a colour temperature in Kelvin; must be greater than zero
    ///
    /// #### Returns
    ///
    /// the same colour temperature in mireds
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when `kelvin` is zero, negative or
///   not a number
    public static double kelvinToMired(double kelvin) {
        // NaN named on its own, because it compares false against every
        // operator: `kelvin <= 0` lets it straight through, and a reciprocal of
        // NaN is NaN, so a bad reading became a colour temperature that
        // spread through every calculation it touched instead of failing
        // here.
        if (Double.isNaN(kelvin) || kelvin <= 0) {
            throw new IllegalArgumentException(
                    "kelvin must be positive, got " + kelvin);
        }
        return 1000000.0 / kelvin;
    }
}
