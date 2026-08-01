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

/// A decoded Running Speed and Cadence Measurement, characteristic
/// `0x2A53`.
///
/// #### Wire format
///
/// A flags byte -- bit 0 stride length present, bit 1 total distance
/// present, bit 2 running rather than walking -- then a `uint16` speed and
/// a `uint8` cadence, then the optional fields.
///
/// #### Cadence is in steps per minute, not strides
///
/// The profile transmits **steps**, so a runner at a typical 90 strides
/// per minute reports 180. Foot pods differ in which they display, and
/// halving the value to get strides is a decision for your UI, not
/// something this parser does silently. [#getStrideRatePerMinute()] is
/// provided for when you want the other convention explicitly.
///
/// Speed arrives in 1/256 m/s and total distance in tenths of a metre.
public final class RscMeasurement {

    private static final int FLAG_STRIDE_LENGTH = 0x01;
    private static final int FLAG_TOTAL_DISTANCE = 0x02;
    private static final int FLAG_RUNNING = 0x04;

    private static final double SPEED_RESOLUTION = 1.0 / 256.0;
    private static final double STRIDE_RESOLUTION_M = 0.01;
    private static final double DISTANCE_RESOLUTION_M = 0.1;

    private final double speedMetersPerSecond;
    private final int cadenceStepsPerMinute;
    private final double strideLengthMeters;
    private final double totalDistanceMeters;
    private final boolean running;

    private RscMeasurement(double speedMetersPerSecond,
            int cadenceStepsPerMinute, double strideLengthMeters,
            double totalDistanceMeters, boolean running) {
        this.speedMetersPerSecond = speedMetersPerSecond;
        this.cadenceStepsPerMinute = cadenceStepsPerMinute;
        this.strideLengthMeters = strideLengthMeters;
        this.totalDistanceMeters = totalDistanceMeters;
        this.running = running;
    }

    /// Decodes a `0x2A53` value. Returns `null` for a malformed or
    /// truncated payload; never throws.
    public static RscMeasurement parse(byte[] value) {
        if (value == null || value.length < 4) {
            return null;
        }
        GattReader r = new GattReader(value);
        int flags = r.uint8();
        double speed = r.uint16() * SPEED_RESOLUTION;
        int cadence = r.uint8();

        double stride = Double.NaN;
        if ((flags & FLAG_STRIDE_LENGTH) != 0) {
            stride = r.uint16() * STRIDE_RESOLUTION_M;
        }
        double distance = Double.NaN;
        if ((flags & FLAG_TOTAL_DISTANCE) != 0) {
            distance = r.uint32() * DISTANCE_RESOLUTION_M;
        }

        if (!r.isValid()) {
            return null;
        }
        return new RscMeasurement(speed, cadence, stride, distance,
                (flags & FLAG_RUNNING) != 0);
    }

    /// Instantaneous speed in metres per second.
    public double getSpeedMetersPerSecond() {
        return speedMetersPerSecond;
    }

    /// Cadence in **steps** per minute, as transmitted.
    public int getCadenceStepsPerMinute() {
        return cadenceStepsPerMinute;
    }

    /// Cadence expressed in strides per minute -- half the step rate.
    /// Provided so the conversion is explicit at the call site rather than
    /// guessed.
    public double getStrideRatePerMinute() {
        return cadenceStepsPerMinute / 2.0;
    }

    /// Stride length in metres, or `Double.NaN` when absent.
    public double getStrideLengthMeters() {
        return strideLengthMeters;
    }

    /// Cumulative distance in metres since the pod powered on, or
    /// `Double.NaN` when absent.
    public double getTotalDistanceMeters() {
        return totalDistanceMeters;
    }

    /// `true` when the pod classified the motion as running rather than
    /// walking.
    public boolean isRunning() {
        return running;
    }

    @Override
    public String toString() {
        return "RscMeasurement[" + speedMetersPerSecond + " m/s, "
                + cadenceStepsPerMinute + " spm]";
    }
}
