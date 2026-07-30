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

/// A blood-pressure reading: systolic and diastolic together, plus an
/// optional pulse.
///
/// #### One sample, not a correlation
///
/// HealthKit models blood pressure as an `HKCorrelation` wrapping two
/// separate quantity samples; Health Connect has a single
/// `BloodPressureRecord` and no correlation concept at all. This API
/// follows Health Connect, because a portable `Correlation` type would be
/// a fiction that only one platform actually has -- Android would have to
/// synthesize it and every caller would have to destructure it.
///
/// #### Local and simulator only in this release
///
/// Neither phone carries this shape. The iOS type map has no
/// `blood_pressure` entry, so the correlation assembly the port will need
/// is not written, and Health Connect's record is not mapped either: a
/// read or write of [HealthDataType#BLOOD_PRESSURE] on a device is
/// refused with [HealthError#TYPE_NOT_SUPPORTED] rather than dropped
/// without a word. It works fully against the local and simulator
/// stores. Until that changes, take the reading off the cuff's
/// [com.codename1.health.sensors.SensorSession] and keep it yourself.
public final class BloodPressureSample extends HealthSample {

    /// The body position was not recorded.
    public static final int POSITION_UNKNOWN = 0;
    /// Measured while standing.
    public static final int POSITION_STANDING = 1;
    /// Measured while sitting.
    public static final int POSITION_SITTING = 2;
    /// Measured while lying down.
    public static final int POSITION_LYING_DOWN = 3;
    /// Measured while reclining.
    public static final int POSITION_RECLINING = 4;

    /// The measurement site was not recorded.
    public static final int LOCATION_UNKNOWN = 0;
    /// Left upper arm.
    public static final int LOCATION_LEFT_UPPER_ARM = 1;
    /// Right upper arm.
    public static final int LOCATION_RIGHT_UPPER_ARM = 2;
    /// Left wrist.
    public static final int LOCATION_LEFT_WRIST = 3;
    /// Right wrist.
    public static final int LOCATION_RIGHT_WRIST = 4;

    private final HealthQuantity systolic;
    private final HealthQuantity diastolic;
    private HealthQuantity pulse;
    private int bodyPosition = POSITION_UNKNOWN;
    private int measurementLocation = LOCATION_UNKNOWN;

    private BloodPressureSample(HealthQuantity systolic,
            HealthQuantity diastolic, long instantMillis) {
        super(HealthDataType.BLOOD_PRESSURE, instantMillis, instantMillis);
        if (systolic == null || diastolic == null) {
            throw new IllegalArgumentException(
                    "blood pressure requires both systolic and diastolic");
        }
        this.systolic = systolic;
        this.diastolic = diastolic;
    }

    /// A reading in millimetres of mercury, the unit both platforms and
    /// every cuff use.
    public static BloodPressureSample create(double systolicMmHg,
            double diastolicMmHg, long instantMillis) {
        return new BloodPressureSample(
                new HealthQuantity(systolicMmHg,
                        HealthUnit.MILLIMETER_OF_MERCURY),
                new HealthQuantity(diastolicMmHg,
                        HealthUnit.MILLIMETER_OF_MERCURY),
                instantMillis);
    }

    /// A reading given as explicit pressure quantities.
    public static BloodPressureSample create(HealthQuantity systolic,
            HealthQuantity diastolic, long instantMillis) {
        // Dimension-checked here, because nothing downstream will. Shared
        // write validation only converts QuantitySample, so a reading
        // given in kilograms was stored by the local and simulator stores
        // exactly as handed in, and only blew up later when something
        // asked for it in mmHg.
        requirePressure(systolic, "systolic");
        requirePressure(diastolic, "diastolic");
        return new BloodPressureSample(systolic, diastolic, instantMillis);
    }

    private static void requirePressure(HealthQuantity q, String which) {
        if (q == null) {
            throw new IllegalArgumentException(
                    "a blood pressure reading needs a " + which + " value");
        }
        if (!q.getUnit().isCompatibleWith(
                HealthUnit.MILLIMETER_OF_MERCURY)) {
            throw new IllegalArgumentException(which + " must be a pressure,"
                    + " but " + q.getUnit().getSymbol() + " measures "
                    + q.getUnit().getDimension());
        }
    }

    /// The systolic pressure.
    public HealthQuantity getSystolic() {
        return systolic;
    }

    /// The diastolic pressure.
    public HealthQuantity getDiastolic() {
        return diastolic;
    }

    /// The pulse recorded with this reading, or null. Most cuffs report
    /// one; neither store requires it.
    public HealthQuantity getPulse() {
        return pulse;
    }

    /// Attaches the pulse recorded alongside the reading.
    public void setPulse(HealthQuantity pulse) {
        if (pulse != null && !pulse.getUnit().isCompatibleWith(
                HealthUnit.COUNT_PER_MINUTE)) {
            throw new IllegalArgumentException("a pulse must be a frequency,"
                    + " but " + pulse.getUnit().getSymbol() + " measures "
                    + pulse.getUnit().getDimension());
        }
        this.pulse = pulse;
    }

    /// One of the `POSITION_` constants.
    public int getBodyPosition() {
        return bodyPosition;
    }

    /// Records the body position, using a `POSITION_` constant.
    public void setBodyPosition(int bodyPosition) {
        this.bodyPosition = bodyPosition;
    }

    /// One of the `LOCATION_` constants.
    public int getMeasurementLocation() {
        return measurementLocation;
    }

    /// Records the measurement site, using a `LOCATION_` constant.
    public void setMeasurementLocation(int measurementLocation) {
        this.measurementLocation = measurementLocation;
    }

    @Override
    public String toString() {
        return "BloodPressureSample["
                + (int) systolic.getValue(HealthUnit.MILLIMETER_OF_MERCURY)
                + "/"
                + (int) diastolic.getValue(HealthUnit.MILLIMETER_OF_MERCURY)
                + " mmHg " + getStartMillis() + "]";
    }
}
