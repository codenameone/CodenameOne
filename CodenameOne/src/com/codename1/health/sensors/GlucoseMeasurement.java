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
package com.codename1.health.sensors;

/// A decoded Glucose Measurement, characteristic `0x2A18`.
///
/// #### Wire format
///
/// A flags byte, a `uint16` sequence number, a 7-byte base time, then the
/// optional fields the flags select:
///
/// | Bit | Meaning |
/// |-----|---------|
/// | 0 | time offset present (`sint16` minutes) |
/// | 1 | concentration, type and location present |
/// | 2 | concentration units: 0 = kg/L, 1 = mol/L |
/// | 3 | sensor status annunciation present |
/// | 4 | context information will follow on `0x2A34` |
///
/// #### The units bit selects a different quantity, not a different scale
///
/// A meter reporting kg/L transmits a mass concentration; one reporting
/// mol/L transmits a molar concentration. Converting between them divides
/// by the molar mass of glucose, 18.0182 -- it is not a generic unit
/// conversion, and applying it to any other analyte would be wrong. This
/// parser normalizes to mmol/L, the SI clinical unit, and exposes
/// [#getMilligramsPerDeciliter()] for the convention used in the United
/// States.
///
/// #### Sequence numbers and stored records
///
/// Meters store readings and upload them in a burst, so the sequence
/// number rather than the arrival order is what identifies a measurement.
/// Retrieving stored records means driving the Record Access Control Point
/// -- see [GlucoseRecordFilter] and
/// [SensorSession#requestStoredRecords(GlucoseRecordFilter)].
public final class GlucoseMeasurement {

    private static final int FLAG_TIME_OFFSET = 0x01;
    private static final int FLAG_CONCENTRATION = 0x02;
    private static final int FLAG_UNITS_MOL_PER_L = 0x04;
    private static final int FLAG_STATUS = 0x08;
    private static final int FLAG_CONTEXT_FOLLOWS = 0x10;

    /// Molar mass of glucose, used to convert mg/dL to mmol/L. Specific to
    /// glucose -- see the class documentation.
    private static final double MG_PER_DL_PER_MMOL_PER_L = 18.0182;

    /// The sample came from somewhere this profile does not name.
    public static final int SAMPLE_LOCATION_UNKNOWN = 0;
    /// Fingertip.
    public static final int SAMPLE_LOCATION_FINGER = 1;
    /// Alternate site test, such as the forearm.
    public static final int SAMPLE_LOCATION_ALTERNATE_SITE = 2;
    /// Earlobe.
    public static final int SAMPLE_LOCATION_EARLOBE = 3;
    /// Control solution rather than blood -- a calibration check, and
    /// **not** a reading to store as the user's glucose.
    public static final int SAMPLE_LOCATION_CONTROL_SOLUTION = 4;

    private final int sequenceNumber;
    private final long timestampMillis;
    private final double mmolPerLiter;
    private final int sampleLocation;
    private final int sampleType;
    private final boolean contextFollows;
    private final boolean hasConcentration;

    private GlucoseMeasurement(int sequenceNumber, long timestampMillis,
            double mmolPerLiter, int sampleLocation, int sampleType,
            boolean contextFollows, boolean hasConcentration) {
        this.sequenceNumber = sequenceNumber;
        this.timestampMillis = timestampMillis;
        this.mmolPerLiter = mmolPerLiter;
        this.sampleLocation = sampleLocation;
        this.sampleType = sampleType;
        this.contextFollows = contextFollows;
        this.hasConcentration = hasConcentration;
    }

    /// Decodes a `0x2A18` value. Returns `null` for a malformed or
    /// truncated payload; never throws.
    public static GlucoseMeasurement parse(byte[] value) {
        if (value == null || value.length < 10) {
            return null;
        }
        GattReader r = new GattReader(value);
        int flags = r.uint8();
        int sequence = r.uint16();
        long baseTime = GattDateTime.read(r);

        long timestamp = baseTime;
        if ((flags & FLAG_TIME_OFFSET) != 0) {
            int offsetMinutes = r.sint16();
            if (baseTime >= 0) {
                timestamp = baseTime + offsetMinutes * 60000L;
            }
        }

        double mmol = Double.NaN;
        int location = SAMPLE_LOCATION_UNKNOWN;
        int type = 0;
        boolean hasConcentration = (flags & FLAG_CONCENTRATION) != 0;
        if (hasConcentration) {
            double raw = Ieee11073.sfloat(r.uint16());
            int typeAndLocation = r.uint8();
            type = typeAndLocation & 0x0F;
            location = (typeAndLocation >> 4) & 0x0F;
            if ((flags & FLAG_UNITS_MOL_PER_L) != 0) {
                // Transmitted in mol/L; 1 mol/L is 1000 mmol/L.
                mmol = raw * 1000.0;
            } else {
                // Transmitted in kg/L. 1 kg/L is 100000 mg/dL.
                double mgPerDl = raw * 100000.0;
                mmol = mgPerDl / MG_PER_DL_PER_MMOL_PER_L;
            }
        }
        if ((flags & FLAG_STATUS) != 0) {
            r.skip(2);
        }

        if (!r.isValid()) {
            return null;
        }
        if (hasConcentration && Double.isNaN(mmol)) {
            // The meter signalled that it could not obtain a reading.
            return null;
        }
        return new GlucoseMeasurement(sequence, timestamp, mmol, location,
                type, (flags & FLAG_CONTEXT_FOLLOWS) != 0, hasConcentration);
    }

    /// The meter's sequence number for this reading. Stable across
    /// re-transmission, and the right key for de-duplicating a burst of
    /// stored records.
    public int getSequenceNumber() {
        return sequenceNumber;
    }

    /// When the reading was taken, epoch millis, or `-1` when the meter's
    /// clock was unset. Prefer this over the time of receipt -- stored
    /// records arrive long after the fact.
    public long getTimestampMillis() {
        return timestampMillis;
    }

    /// The concentration in mmol/L, or `Double.NaN` when this record
    /// carried none.
    public double getMillimolesPerLiter() {
        return mmolPerLiter;
    }

    /// The concentration in mg/dL, the unit used clinically in the United
    /// States, or `Double.NaN` when absent.
    public double getMilligramsPerDeciliter() {
        return mmolPerLiter * MG_PER_DL_PER_MMOL_PER_L;
    }

    /// `true` when this record carried a concentration. A record without
    /// one is a placeholder the meter kept for a failed test.
    public boolean hasConcentration() {
        return hasConcentration;
    }

    /// Where the sample was taken, as a `SAMPLE_LOCATION_` constant.
    ///
    /// Check for [#SAMPLE_LOCATION_CONTROL_SOLUTION] before storing a
    /// reading as the user's glucose: that value means the meter was being
    /// calibrated against a test fluid, and recording it as a blood
    /// measurement puts a fictitious reading into their medical history.
    public int getSampleLocation() {
        return sampleLocation;
    }

    /// The fluid type code the meter reported -- capillary whole blood,
    /// venous plasma, interstitial fluid and so on.
    public int getSampleType() {
        return sampleType;
    }

    /// `true` when the meter will follow this record with a context
    /// notification on `0x2A34` carrying meal, exercise and medication
    /// annotations.
    public boolean isContextFollowing() {
        return contextFollows;
    }

    /// `true` when this reading came from control solution rather than
    /// blood and must not be stored as a glucose measurement.
    public boolean isControlSolution() {
        return sampleLocation == SAMPLE_LOCATION_CONTROL_SOLUTION;
    }

    public String toString() {
        return "GlucoseMeasurement[#" + sequenceNumber + " " + mmolPerLiter
                + " mmol/L]";
    }
}
