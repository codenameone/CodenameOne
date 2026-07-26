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

/// A kind of health data -- steps, heart rate, sleep, body mass. Instances
/// are interned constants, so `==` is a valid identity test.
///
/// #### Why this is not an enum
///
/// Three reasons, in order of how much trouble each would cause:
///
/// 1. Each constant carries a [HealthUnit], and a Java enum constructor
///    that references another class's statics is an initialization-order
///    hazard -- the unit would be null for whichever class loaded first.
/// 2. [#forId(String)] has to be total across framework versions. An app
///    that persisted `"vo2Max"` and is later restored by an older runtime
///    must get `null`, not the `IllegalArgumentException` that
///    `Enum.valueOf` throws.
/// 3. Ports need to attach platform metadata to types without the core
///    knowing about it.
///
/// #### There is no custom type
///
/// Deliberately no `HealthDataType.custom(String)`. A custom type would
/// have no canonical unit, no aggregation style and no cross-platform
/// mapping -- an untyped string passthrough that works on exactly one
/// platform, which is the failure mode this API exists to prevent. Adding
/// a type is a change to this class.
///
/// #### Permissions are declared, not inferred
///
/// The build server cannot see which of these constants an app touches:
/// constant references compile to field reads, and the class scanner only
/// records type and method references. Android's per-type
/// `android.permission.health.*` set therefore comes from the
/// `android.health.read` and `android.health.write` build hints, which use
/// the [#getId()] values below as tokens. This also matches Google Play
/// policy, which requires declaring exactly the data types you use.
public final class HealthDataType {

    private static final Map<String, HealthDataType> BY_ID =
            new HashMap<String, HealthDataType>();
    private static final List<HealthDataType> ALL =
            new ArrayList<HealthDataType>();

    private final String id;
    private final HealthDataKind kind;
    private final HealthUnit canonicalUnit;
    private final HealthAggregationStyle aggregationStyle;
    private final boolean intervalOnly;

    private HealthDataType(String id, HealthDataKind kind,
            HealthUnit canonicalUnit, HealthAggregationStyle aggregationStyle,
            boolean intervalOnly) {
        this.id = id;
        this.kind = kind;
        this.canonicalUnit = canonicalUnit;
        this.aggregationStyle = aggregationStyle;
        this.intervalOnly = intervalOnly;
    }

    private static HealthDataType define(String id, HealthDataKind kind,
            HealthUnit unit, HealthAggregationStyle style,
            boolean intervalOnly) {
        HealthDataType t = new HealthDataType(id, kind, unit, style,
                intervalOnly);
        BY_ID.put(id, t);
        ALL.add(t);
        return t;
    }

    /// A cumulative quantity: accumulates over an interval, summed when
    /// aggregated, and never instantaneous.
    private static HealthDataType cumulative(String id, HealthUnit unit) {
        return define(id, HealthDataKind.QUANTITY, unit,
                HealthAggregationStyle.CUMULATIVE, true);
    }

    /// A discrete quantity: a point-in-time reading, averaged when
    /// aggregated. May still carry a span (a heart-rate average over a
    /// minute), so it is not interval-only.
    private static HealthDataType discrete(String id, HealthUnit unit) {
        return define(id, HealthDataKind.QUANTITY, unit,
                HealthAggregationStyle.DISCRETE, false);
    }

    // ------------------------------------------------------------------
    // activity
    // ------------------------------------------------------------------

    public static final HealthDataType STEPS =
            cumulative("steps", HealthUnit.COUNT);
    public static final HealthDataType DISTANCE_WALKING_RUNNING =
            cumulative("distance_walking_running", HealthUnit.METER);
    public static final HealthDataType DISTANCE_CYCLING =
            cumulative("distance_cycling", HealthUnit.METER);
    public static final HealthDataType DISTANCE_SWIMMING =
            cumulative("distance_swimming", HealthUnit.METER);
    public static final HealthDataType FLIGHTS_CLIMBED =
            cumulative("flights_climbed", HealthUnit.COUNT);
    public static final HealthDataType ELEVATION_GAINED =
            cumulative("elevation_gained", HealthUnit.METER);
    public static final HealthDataType ACTIVE_ENERGY =
            cumulative("active_energy", HealthUnit.KILOCALORIE);
    public static final HealthDataType BASAL_ENERGY =
            cumulative("basal_energy", HealthUnit.KILOCALORIE);
    public static final HealthDataType EXERCISE_TIME =
            cumulative("exercise_time", HealthUnit.MINUTE);
    public static final HealthDataType WHEELCHAIR_PUSHES =
            cumulative("wheelchair_pushes", HealthUnit.COUNT);

    // ------------------------------------------------------------------
    // vitals
    // ------------------------------------------------------------------

    public static final HealthDataType HEART_RATE =
            discrete("heart_rate", HealthUnit.COUNT_PER_MINUTE);
    public static final HealthDataType RESTING_HEART_RATE =
            discrete("resting_heart_rate", HealthUnit.COUNT_PER_MINUTE);
    public static final HealthDataType WALKING_HEART_RATE_AVERAGE =
            discrete("walking_heart_rate_average", HealthUnit.COUNT_PER_MINUTE);

    /// Heart-rate variability as the standard deviation of NN intervals,
    /// the metric both platforms expose (`HKQuantityTypeIdentifier` `...
    /// HeartRateVariabilitySDNN`). Other HRV metrics -- RMSSD, frequency
    /// domain -- are not stored by either platform; derive them yourself
    /// from the RR intervals a chest strap reports through
    /// `com.codename1.health.sensors`.
    public static final HealthDataType HEART_RATE_VARIABILITY_SDNN =
            discrete("heart_rate_variability_sdnn", HealthUnit.MILLISECOND);

    public static final HealthDataType OXYGEN_SATURATION =
            discrete("oxygen_saturation", HealthUnit.PERCENT);
    public static final HealthDataType RESPIRATORY_RATE =
            discrete("respiratory_rate", HealthUnit.COUNT_PER_MINUTE);
    public static final HealthDataType BODY_TEMPERATURE =
            discrete("body_temperature", HealthUnit.DEGREE_CELSIUS);
    public static final HealthDataType BASAL_BODY_TEMPERATURE =
            discrete("basal_body_temperature", HealthUnit.DEGREE_CELSIUS);
    public static final HealthDataType VO2_MAX =
            discrete("vo2_max", HealthUnit.ML_PER_KG_PER_MINUTE);

    /// Blood pressure, as a single sample carrying both systolic and
    /// diastolic values -- see [BloodPressureSample].
    ///
    /// HealthKit models this as an `HKCorrelation` of two separate
    /// quantity samples; Health Connect has a single `BloodPressureRecord`
    /// and no correlation concept at all. This API follows Health Connect,
    /// and the iOS port assembles and disassembles the correlation. There
    /// is deliberately no portable `Correlation` type, because it would be
    /// a fiction maintained in one port only.
    public static final HealthDataType BLOOD_PRESSURE =
            define("blood_pressure", HealthDataKind.QUANTITY,
                    HealthUnit.MILLIMETER_OF_MERCURY,
                    HealthAggregationStyle.DISCRETE, false);

    public static final HealthDataType BLOOD_GLUCOSE =
            discrete("blood_glucose", HealthUnit.MILLIMOLE_PER_LITER);

    // ------------------------------------------------------------------
    // body measurements
    // ------------------------------------------------------------------

    public static final HealthDataType BODY_MASS =
            discrete("body_mass", HealthUnit.KILOGRAM);
    public static final HealthDataType LEAN_BODY_MASS =
            discrete("lean_body_mass", HealthUnit.KILOGRAM);
    public static final HealthDataType BONE_MASS =
            discrete("bone_mass", HealthUnit.KILOGRAM);
    public static final HealthDataType BODY_FAT_PERCENTAGE =
            discrete("body_fat_percentage", HealthUnit.PERCENT);
    public static final HealthDataType BODY_MASS_INDEX =
            discrete("body_mass_index", HealthUnit.COUNT);
    public static final HealthDataType HEIGHT =
            discrete("height", HealthUnit.METER);
    public static final HealthDataType WAIST_CIRCUMFERENCE =
            discrete("waist_circumference", HealthUnit.METER);

    // ------------------------------------------------------------------
    // performance
    // ------------------------------------------------------------------

    public static final HealthDataType POWER =
            discrete("power", HealthUnit.WATT);
    public static final HealthDataType SPEED =
            discrete("speed", HealthUnit.METER_PER_SECOND);
    public static final HealthDataType CYCLING_CADENCE =
            discrete("cycling_cadence", HealthUnit.COUNT_PER_MINUTE);
    public static final HealthDataType RUNNING_CADENCE =
            discrete("running_cadence", HealthUnit.COUNT_PER_MINUTE);

    // ------------------------------------------------------------------
    // intake
    // ------------------------------------------------------------------

    public static final HealthDataType HYDRATION =
            cumulative("hydration", HealthUnit.LITER);
    public static final HealthDataType DIETARY_ENERGY =
            cumulative("dietary_energy", HealthUnit.KILOCALORIE);

    /// A logged food or meal carrying an arbitrary set of nutrients --
    /// see `com.codename1.health.nutrition`. Produces a
    /// `NutritionSample` rather than a plain quantity, because a single
    /// entry sets many nutrient fields at once.
    public static final HealthDataType NUTRITION =
            define("nutrition", HealthDataKind.SESSION, null,
                    HealthAggregationStyle.NONE, true);

    // ------------------------------------------------------------------
    // sessions and categories
    // ------------------------------------------------------------------

    /// A sleep session with optional stage detail -- see [SleepSample].
    public static final HealthDataType SLEEP =
            define("sleep", HealthDataKind.SESSION, null,
                    HealthAggregationStyle.NONE, true);

    /// A workout or exercise session -- see [WorkoutSample].
    public static final HealthDataType WORKOUT =
            define("workout", HealthDataKind.SESSION, null,
                    HealthAggregationStyle.NONE, true);

    /// A mindfulness or meditation session.
    public static final HealthDataType MINDFUL_SESSION =
            define("mindful_session", HealthDataKind.SESSION, null,
                    HealthAggregationStyle.NONE, true);

    public static final HealthDataType MENSTRUATION_FLOW =
            define("menstruation_flow", HealthDataKind.CATEGORY, null,
                    HealthAggregationStyle.NONE, false);
    public static final HealthDataType INTERMENSTRUAL_BLEEDING =
            define("intermenstrual_bleeding", HealthDataKind.CATEGORY, null,
                    HealthAggregationStyle.NONE, false);

    // ------------------------------------------------------------------

    /// The stable, portable identifier for this type. Safe to persist and
    /// to send to a server; also the token used by the
    /// `android.health.read` and `android.health.write` build hints.
    public String getId() {
        return id;
    }

    /// What shape of sample a query for this type returns.
    public HealthDataKind getKind() {
        return kind;
    }

    /// The unit that samples of this type are normalized to when a query
    /// does not request a specific one. Null for types with no natural
    /// unit -- sessions and categories.
    public HealthUnit getCanonicalUnit() {
        return canonicalUnit;
    }

    /// How this type combines over an aggregation bucket.
    public HealthAggregationStyle getAggregationStyle() {
        return aggregationStyle;
    }

    /// `true` when a sample of this type must span a time range rather
    /// than mark an instant. A step count belongs to an interval; a body
    /// mass reading belongs to a moment.
    ///
    /// [HealthStore] rejects an instantaneous write of an interval-only
    /// type with [HealthError#INVALID_ARGUMENT], rather than letting the
    /// platform throw something opaque later.
    public boolean isIntervalOnly() {
        return intervalOnly;
    }

    /// Looks a type up by [#getId()], or `null` when this version of the
    /// framework does not know it. Total by design -- see the class
    /// documentation.
    public static HealthDataType forId(String id) {
        if (id == null) {
            return null;
        }
        return BY_ID.get(id);
    }

    /// Every type known to this version of the framework. Note that a
    /// given platform supports a subset -- check
    /// [HealthStore#isTypeSupported(HealthDataType)].
    public static List<HealthDataType> values() {
        return Collections.unmodifiableList(ALL);
    }

    @Override
    public String toString() {
        return id;
    }
}
