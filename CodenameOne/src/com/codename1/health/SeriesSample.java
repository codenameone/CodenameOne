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

/// A run of measurements that share one record identity -- a beat-to-beat
/// heart-rate trace, a cadence series.
///
/// #### Why this type exists
///
/// The two platforms disagree about grouping. Health Connect's
/// `HeartRateRecord` is a single record containing many samples, and
/// deleting it means deleting the record as a whole. HealthKit returns the
/// same data as many independent samples with no grouping at all.
///
/// Flattening Health Connect into individual samples loses the identity
/// you need in order to delete; inventing a series on iOS would claim a
/// grouping that is not there. So the choice is yours:
/// [SampleQuery#setFlattenSeries(boolean)] defaults to `true`, which gives
/// both platforms plain [QuantitySample] objects and lets cross-platform
/// code be identical. Turn it off when you need record identity, and
/// expect iOS to return series of size 1.
///
/// #### Storage
///
/// Values are held in parallel primitive arrays rather than a list of
/// objects. A month of continuous heart rate is tens of thousands of
/// points, and boxing each one is real memory pressure on a phone.
public final class SeriesSample extends HealthSample {

    private final long[] sampleStarts;
    private final long[] sampleEnds;
    private final double[] values;
    private final HealthUnit unit;

    private SeriesSample(HealthDataType type, long startMillis,
            long endMillis, long[] sampleStarts, long[] sampleEnds,
            double[] values, HealthUnit unit) {
        super(type, startMillis, endMillis);
        this.sampleStarts = sampleStarts;
        this.sampleEnds = sampleEnds;
        this.values = values;
        this.unit = unit;
    }

    /// Creates a series. The three arrays must be the same length and are
    /// copied defensively.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if the arrays are null, differ in
    ///   length, or `unit` is null.
    public static SeriesSample create(HealthDataType type, long startMillis,
            long endMillis, long[] sampleStarts, long[] sampleEnds,
            double[] values, HealthUnit unit) {
        if (sampleStarts == null || sampleEnds == null || values == null) {
            throw new IllegalArgumentException(
                    "a series requires start, end and value arrays");
        }
        if (sampleStarts.length != values.length
                || sampleEnds.length != values.length) {
            throw new IllegalArgumentException(
                    "series arrays differ in length: starts="
                            + sampleStarts.length + " ends=" + sampleEnds.length
                            + " values=" + values.length);
        }
        if (unit == null) {
            throw new IllegalArgumentException("a series requires a unit");
        }
        // A series with nothing in it is not a record, it is an absence of
        // one. Writing it expanded to no wire records at all, and an empty
        // batch is reported by both bridges as a successful write of
        // nothing -- the caller was told it worked while the store never
        // heard of it.
        // A series is a run of measurements, and flattening one -- the
        // default read shape -- turns each into a QuantitySample. A
        // category or session type has no numeric value, so such a series
        // could be written to the local store and then threw on the next
        // ordinary read.
        if (type == null || type.getCanonicalUnit() == null) {
            throw new IllegalArgumentException((type == null ? "null"
                    : type.getId()) + " has no numeric value, so it cannot"
                            + " be recorded as a series");
        }
        if (values.length == 0) {
            throw new IllegalArgumentException(
                    "a series needs at least one measurement");
        }
        long[] s = new long[sampleStarts.length];
        long[] e = new long[sampleEnds.length];
        double[] v = new double[values.length];
        System.arraycopy(sampleStarts, 0, s, 0, s.length);
        System.arraycopy(sampleEnds, 0, e, 0, e.length);
        System.arraycopy(values, 0, v, 0, v.length);
        return new SeriesSample(type, startMillis, endMillis, s, e, v, unit);
    }

    /// The number of measurements in this series.
    public int size() {
        return values.length;
    }

    /// The start of measurement `i`, epoch millis UTC.
    public long getSampleStartMillis(int i) {
        return sampleStarts[i];
    }

    /// The end of measurement `i`, epoch millis UTC. Equal to the start
    /// for an instantaneous measurement.
    public long getSampleEndMillis(int i) {
        return sampleEnds[i];
    }

    /// Measurement `i` converted into `in`.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if `in` measures a different
    ///   dimension.
    public double getSampleValue(int i, HealthUnit in) {
        return HealthUnit.convert(values[i], unit, in);
    }

    /// The unit every measurement in this series is expressed in.
    public HealthUnit getUnit() {
        return unit;
    }

    /// Measurement `i` as a standalone [QuantitySample]. Allocates, so
    /// prefer the indexed accessors when walking the whole series.
    ///
    /// The returned sample inherits this series' source, recording method
    /// and identifier -- meaning several extracted samples share one
    /// identifier, since they came from one record.
    public QuantitySample toQuantitySample(int i) {
        QuantitySample q = QuantitySample.create(getType(),
                new HealthQuantity(values[i], unit),
                sampleStarts[i], sampleEnds[i]);
        q.setId(getId());
        q.setSource(getSource());
        q.setRecordingMethod(getRecordingMethod());
        // Metadata too. Flattening is the default read shape, so dropping
        // it here meant a series written to the local store with a
        // correlation identifier came back as quantities carrying none --
        // while the same sample written whole round-tripped fine.
        for (java.util.Map.Entry<String, String> e
                : getMetadata().entrySet()) {
            q.putMetadata(e.getKey(), e.getValue());
        }
        return q;
    }

    @Override
    public String toString() {
        return "SeriesSample[" + getType().getId() + " x" + values.length
                + " " + unit.getSymbol() + " " + getStartMillis() + ".."
                + getEndMillis() + "]";
    }
}
