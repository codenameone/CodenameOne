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

import com.codename1.bluetooth.BluetoothUuid;
import com.codename1.health.HealthDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/// A standard Bluetooth SIG health sensor profile -- the service a device
/// advertises and the measurements it produces.
///
/// These are the adopted profiles, so any conforming device works without
/// per-vendor code: a heart-rate strap from any manufacturer speaks
/// `0x180D`.
public final class HealthSensorProfile {

    private static final List<HealthSensorProfile> ALL =
            new ArrayList<HealthSensorProfile>();

    private final String name;
    private final BluetoothUuid serviceUuid;
    private final BluetoothUuid measurementUuid;
    private final List<HealthDataType> producedTypes;
    private final boolean streaming;

    private HealthSensorProfile(String name, int serviceShortUuid,
            int measurementShortUuid, boolean streaming,
            HealthDataType[] producedTypes) {
        this.name = name;
        this.serviceUuid = BluetoothUuid.fromShort(serviceShortUuid);
        this.measurementUuid = BluetoothUuid.fromShort(measurementShortUuid);
        this.streaming = streaming;
        this.producedTypes = Collections.unmodifiableList(
                new ArrayList<HealthDataType>(Arrays.asList(producedTypes)));
        ALL.add(this);
    }

    /// Heart Rate, service `0x180D`. Streams heart rate and, on most
    /// straps, RR intervals for heart-rate variability.
    public static final HealthSensorProfile HEART_RATE =
            new HealthSensorProfile("Heart Rate", 0x180D, 0x2A37, true,
                    new HealthDataType[] { HealthDataType.HEART_RATE });

    /// Cycling Power, service `0x1818`. Streams power and, when the meter
    /// reports crank data, cadence.
    public static final HealthSensorProfile CYCLING_POWER =
            new HealthSensorProfile("Cycling Power", 0x1818, 0x2A63, true,
                    new HealthDataType[] { HealthDataType.POWER,
                            HealthDataType.CYCLING_CADENCE });

    /// Cycling Speed and Cadence, service `0x1816`. Streams derived speed
    /// and cadence -- the raw characteristic carries cumulative counters,
    /// which [SensorSession] differences for you.
    public static final HealthSensorProfile CYCLING_SPEED_CADENCE =
            new HealthSensorProfile("Cycling Speed and Cadence", 0x1816,
                    0x2A5B, true,
                    new HealthDataType[] { HealthDataType.SPEED,
                            HealthDataType.CYCLING_CADENCE });

    /// Running Speed and Cadence, service `0x1814`. Streams speed and step
    /// cadence from a foot pod.
    public static final HealthSensorProfile RUNNING_SPEED_CADENCE =
            new HealthSensorProfile("Running Speed and Cadence", 0x1814,
                    0x2A53, true,
                    new HealthDataType[] { HealthDataType.SPEED,
                            HealthDataType.RUNNING_CADENCE });

    /// Health Thermometer, service `0x1809`. Reports one measurement at a
    /// time rather than streaming.
    public static final HealthSensorProfile HEALTH_THERMOMETER =
            new HealthSensorProfile("Health Thermometer", 0x1809, 0x2A1C,
                    false,
                    new HealthDataType[] { HealthDataType.BODY_TEMPERATURE });

    /// Weight Scale, service `0x181D`. Reports one measurement per
    /// weighing.
    public static final HealthSensorProfile WEIGHT_SCALE =
            new HealthSensorProfile("Weight Scale", 0x181D, 0x2A9D, false,
                    new HealthDataType[] { HealthDataType.BODY_MASS });

    /// Blood Pressure, service `0x1810`. Reports one measurement per
    /// inflation cycle.
    public static final HealthSensorProfile BLOOD_PRESSURE =
            new HealthSensorProfile("Blood Pressure", 0x1810, 0x2A35, false,
                    new HealthDataType[] { HealthDataType.BLOOD_PRESSURE });

    /// Glucose, service `0x1808`. Reports a measurement when the user
    /// tests, and can replay stored records -- see
    /// [SensorSession#requestStoredRecords(GlucoseRecordFilter)].
    public static final HealthSensorProfile GLUCOSE =
            new HealthSensorProfile("Glucose", 0x1808, 0x2A18, false,
                    new HealthDataType[] { HealthDataType.BLOOD_GLUCOSE });

    /// The profile's human-readable name.
    public String getName() {
        return name;
    }

    /// The GATT service UUID a conforming device advertises.
    public BluetoothUuid getServiceUuid() {
        return serviceUuid;
    }

    /// The characteristic carrying this profile's measurements.
    public BluetoothUuid getMeasurementUuid() {
        return measurementUuid;
    }

    /// The health data types a session with this profile can produce.
    public List<HealthDataType> getProducedTypes() {
        return producedTypes;
    }

    /// `true` for profiles that notify continuously -- heart rate, power,
    /// cadence. `false` for episodic ones that report a single reading per
    /// measurement, such as a scale or a blood-pressure cuff.
    ///
    /// Worth branching on in UI: a streaming sensor should show a live
    /// value, while an episodic one should show "waiting for a
    /// measurement" until the user actually takes one.
    public boolean isStreaming() {
        return streaming;
    }

    /// Every profile this framework understands.
    public static List<HealthSensorProfile> values() {
        return Collections.unmodifiableList(ALL);
    }

    @Override
    public String toString() {
        return name;
    }
}
