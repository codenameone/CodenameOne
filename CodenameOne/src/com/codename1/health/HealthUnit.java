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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// A unit of measure for health data. Instances are interned constants, so
/// `==` is a valid identity test and is used throughout the API.
///
/// #### The symbol is the wire format
///
/// [#getSymbol()] is public API rather than an internal detail because the
/// string genuinely has to cross the platform boundary: Apple's
/// `HKUnit(from:)` parses exactly this syntax (`"count/min"`, `"kg"`,
/// `"kcal"`, `"mmHg"`, `"degC"`, `"mg/dL"`, `"mL/(kg*min)"`), so the iOS
/// port passes it straight through with no mapping table. The Android port
/// maps it to the matching `androidx.health.connect.client.units` type.
/// Making that contract visible and documented is better than smuggling it
/// through an implementation class.
///
/// #### Conversion
///
/// Conversion is affine (`canonical = value * scale + offset`) rather than
/// a simple ratio, because temperature needs the offset. Converting between
/// units of different dimensions throws [IllegalArgumentException]: that is
/// a bug in the calling code, not a condition to be reported through an
/// `AsyncResource`.
///
/// ```java
/// double lb = HealthUnit.convert(80, HealthUnit.KILOGRAM, HealthUnit.POUND);
/// ```
public final class HealthUnit {

    private static final Map<String, HealthUnit> BY_SYMBOL =
            new HashMap<String, HealthUnit>();
    private static final List<HealthUnit> ALL = new ArrayList<HealthUnit>();

    private final String symbol;
    private final HealthUnitDimension dimension;
    private final double scale;
    private final double offset;

    private HealthUnit(String symbol, HealthUnitDimension dimension,
            double scale, double offset) {
        this.symbol = symbol;
        this.dimension = dimension;
        this.scale = scale;
        this.offset = offset;
    }

    private static HealthUnit define(String symbol,
            HealthUnitDimension dimension, double scale, double offset) {
        HealthUnit u = new HealthUnit(symbol, dimension, scale, offset);
        BY_SYMBOL.put(symbol, u);
        ALL.add(u);
        return u;
    }

    private static HealthUnit define(String symbol,
            HealthUnitDimension dimension, double scale) {
        return define(symbol, dimension, scale, 0);
    }

    // ------------------------------------------------------------------
    // count / frequency
    // ------------------------------------------------------------------

    /// A plain tally. Canonical unit of [HealthUnitDimension#COUNT].
    public static final HealthUnit COUNT =
            define("count", HealthUnitDimension.COUNT, 1);

    /// Beats, breaths or steps per minute. Canonical unit of
    /// [HealthUnitDimension#FREQUENCY].
    public static final HealthUnit COUNT_PER_MINUTE =
            define("count/min", HealthUnitDimension.FREQUENCY, 1);

    /// Counts per second.
    public static final HealthUnit COUNT_PER_SECOND =
            define("count/s", HealthUnitDimension.FREQUENCY, 60);

    /// A ratio expressed as a percentage. Canonical unit of
    /// [HealthUnitDimension#PERCENT]. Note that HealthKit represents
    /// oxygen saturation and body-fat percentage as 0..1 fractions
    /// natively; the iOS port scales them into this unit.
    public static final HealthUnit PERCENT =
            define("%", HealthUnitDimension.PERCENT, 1);

    // ------------------------------------------------------------------
    // mass
    // ------------------------------------------------------------------

    /// Canonical unit of [HealthUnitDimension#MASS].
    public static final HealthUnit KILOGRAM =
            define("kg", HealthUnitDimension.MASS, 1);
    public static final HealthUnit GRAM =
            define("g", HealthUnitDimension.MASS, 0.001);
    public static final HealthUnit MILLIGRAM =
            define("mg", HealthUnitDimension.MASS, 0.000001);
    public static final HealthUnit MICROGRAM =
            define("mcg", HealthUnitDimension.MASS, 0.000000001);
    public static final HealthUnit POUND =
            define("lb", HealthUnitDimension.MASS, 0.45359237);
    public static final HealthUnit OUNCE =
            define("oz", HealthUnitDimension.MASS, 0.028349523125);
    public static final HealthUnit STONE =
            define("st", HealthUnitDimension.MASS, 6.35029318);

    // ------------------------------------------------------------------
    // length
    // ------------------------------------------------------------------

    /// Canonical unit of [HealthUnitDimension#LENGTH].
    public static final HealthUnit METER =
            define("m", HealthUnitDimension.LENGTH, 1);
    public static final HealthUnit KILOMETER =
            define("km", HealthUnitDimension.LENGTH, 1000);
    public static final HealthUnit CENTIMETER =
            define("cm", HealthUnitDimension.LENGTH, 0.01);
    public static final HealthUnit MILE =
            define("mi", HealthUnitDimension.LENGTH, 1609.344);
    public static final HealthUnit FOOT =
            define("ft", HealthUnitDimension.LENGTH, 0.3048);
    public static final HealthUnit INCH =
            define("in", HealthUnitDimension.LENGTH, 0.0254);
    public static final HealthUnit YARD =
            define("yd", HealthUnitDimension.LENGTH, 0.9144);

    // ------------------------------------------------------------------
    // energy
    // ------------------------------------------------------------------

    /// Canonical unit of [HealthUnitDimension#ENERGY]. Kilocalories rather
    /// than joules because it is the idiomatic unit on both platforms and
    /// in every consumer health UI.
    public static final HealthUnit KILOCALORIE =
            define("kcal", HealthUnitDimension.ENERGY, 1);
    public static final HealthUnit KILOJOULE =
            define("kJ", HealthUnitDimension.ENERGY, 0.2390057361);
    public static final HealthUnit JOULE =
            define("J", HealthUnitDimension.ENERGY, 0.0002390057361);

    // ------------------------------------------------------------------
    // time
    // ------------------------------------------------------------------

    /// Canonical unit of [HealthUnitDimension#TIME]. Milliseconds because
    /// heart-rate variability is reported in them and the rest of the API
    /// speaks epoch millis.
    public static final HealthUnit MILLISECOND =
            define("ms", HealthUnitDimension.TIME, 1);
    public static final HealthUnit SECOND =
            define("s", HealthUnitDimension.TIME, 1000);
    public static final HealthUnit MINUTE =
            define("min", HealthUnitDimension.TIME, 60000);
    public static final HealthUnit HOUR =
            define("hr", HealthUnitDimension.TIME, 3600000);

    // ------------------------------------------------------------------
    // pressure
    // ------------------------------------------------------------------

    /// Canonical unit of [HealthUnitDimension#PRESSURE].
    public static final HealthUnit MILLIMETER_OF_MERCURY =
            define("mmHg", HealthUnitDimension.PRESSURE, 1);
    public static final HealthUnit KILOPASCAL =
            define("kPa", HealthUnitDimension.PRESSURE, 7.50061683);

    // ------------------------------------------------------------------
    // temperature -- the reason conversion is affine
    // ------------------------------------------------------------------

    /// Canonical unit of [HealthUnitDimension#TEMPERATURE].
    public static final HealthUnit DEGREE_CELSIUS =
            define("degC", HealthUnitDimension.TEMPERATURE, 1, 0);

    /// Fahrenheit. `C = F * 5/9 - 160/9`, which is why [HealthUnit]
    /// conversion carries an offset rather than being a plain ratio.
    public static final HealthUnit DEGREE_FAHRENHEIT =
            define("degF", HealthUnitDimension.TEMPERATURE,
                    5.0 / 9.0, -160.0 / 9.0);

    // ------------------------------------------------------------------
    // volume
    // ------------------------------------------------------------------

    /// Canonical unit of [HealthUnitDimension#VOLUME].
    public static final HealthUnit LITER =
            define("L", HealthUnitDimension.VOLUME, 1);
    public static final HealthUnit MILLILITER =
            define("mL", HealthUnitDimension.VOLUME, 0.001);
    public static final HealthUnit FLUID_OUNCE_US =
            define("fl_oz_us", HealthUnitDimension.VOLUME, 0.0295735295625);
    public static final HealthUnit CUP_US =
            define("cup_us", HealthUnitDimension.VOLUME, 0.2365882365);

    // ------------------------------------------------------------------
    // power / velocity / oxygen uptake
    // ------------------------------------------------------------------

    /// Canonical unit of [HealthUnitDimension#POWER].
    public static final HealthUnit WATT =
            define("W", HealthUnitDimension.POWER, 1);

    /// Canonical unit of [HealthUnitDimension#VELOCITY].
    public static final HealthUnit METER_PER_SECOND =
            define("m/s", HealthUnitDimension.VELOCITY, 1);
    public static final HealthUnit KILOMETER_PER_HOUR =
            define("km/hr", HealthUnitDimension.VELOCITY, 1.0 / 3.6);
    public static final HealthUnit MILE_PER_HOUR =
            define("mi/hr", HealthUnitDimension.VELOCITY, 0.44704);

    /// Canonical unit of [HealthUnitDimension#OXYGEN_UPTAKE]; the standard
    /// VO2-max unit. The symbol is the exact `HKUnit` spelling.
    public static final HealthUnit ML_PER_KG_PER_MINUTE =
            define("mL/(kg*min)", HealthUnitDimension.OXYGEN_UPTAKE, 1);

    // ------------------------------------------------------------------
    // glucose -- see HealthUnitDimension.GLUCOSE_CONCENTRATION
    // ------------------------------------------------------------------

    /// Canonical unit of [HealthUnitDimension#GLUCOSE_CONCENTRATION]; the
    /// SI unit, used across most of the world.
    public static final HealthUnit MILLIMOLE_PER_LITER =
            define("mmol/L", HealthUnitDimension.GLUCOSE_CONCENTRATION, 1);

    /// The unit used clinically in the United States. The 18.0182 factor is
    /// the molar mass of **glucose** -- it is not a general mg/dL to mmol/L
    /// conversion and must not be reused for another analyte.
    public static final HealthUnit MILLIGRAM_PER_DECILITER =
            define("mg/dL", HealthUnitDimension.GLUCOSE_CONCENTRATION,
                    1.0 / 18.0182);

    // ------------------------------------------------------------------

    /// The unit symbol, in the exact syntax Apple's `HKUnit(from:)`
    /// accepts. See the class documentation for why this is public API.
    public String getSymbol() {
        return symbol;
    }

    /// The physical dimension this unit measures.
    public HealthUnitDimension getDimension() {
        return dimension;
    }

    /// `true` when `other` measures the same dimension and a conversion
    /// between the two is therefore meaningful.
    public boolean isCompatibleWith(HealthUnit other) {
        return other != null && other.dimension == dimension;
    }

    /// Converts `value`, expressed in this unit, into the canonical unit of
    /// this unit's dimension.
    public double toCanonical(double value) {
        return value * scale + offset;
    }

    /// Converts `value`, expressed in the canonical unit of this unit's
    /// dimension, into this unit.
    public double fromCanonical(double value) {
        return (value - offset) / scale;
    }

    /// Converts a value between two units of the same dimension.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if either unit is null or the two
    ///   measure different dimensions. Crossing dimensions is a coding
    ///   error and is surfaced as one.
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    public static double convert(double value, HealthUnit from, HealthUnit to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("null unit in conversion");
        }
        if (from == to) {
            return value;
        }
        if (from.dimension != to.dimension) {
            throw new IllegalArgumentException("cannot convert "
                    + from.symbol + " to " + to.symbol
                    + ": " + from.dimension + " is not " + to.dimension);
        }
        return to.fromCanonical(from.toCanonical(value));
    }

    /// Looks a unit up by its symbol, or `null` when the symbol is unknown
    /// to this version of the framework. Total by design: a persisted
    /// symbol read back by an older runtime yields `null` rather than an
    /// exception.
    public static HealthUnit forSymbol(String symbol) {
        if (symbol == null) {
            return null;
        }
        return BY_SYMBOL.get(symbol);
    }

    /// Every unit known to this version of the framework.
    public static List<HealthUnit> values() {
        return Collections.unmodifiableList(ALL);
    }

    /// Returns [#getSymbol()], so string concatenation in log statements
    /// and error messages reads naturally.
    @Override
    public String toString() {
        return symbol;
    }
}
