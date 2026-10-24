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
package android.hardware;

/// One sensor [SensorManager#getDefaultSensor(int)] answered. Only the types
/// Codename One can feed exist; [SensorManager] answers null for the others,
/// exactly as Android does on a device without that hardware.
public final class Sensor {

    public static final int TYPE_ALL = -1;
    public static final int TYPE_ACCELEROMETER = 1;
    public static final int TYPE_MAGNETIC_FIELD = 2;
    /// Deprecated on Android; still delivered, in degrees.
    public static final int TYPE_ORIENTATION = 3;
    public static final int TYPE_GYROSCOPE = 4;
    public static final int TYPE_LIGHT = 5;
    public static final int TYPE_PRESSURE = 6;
    public static final int TYPE_TEMPERATURE = 7;
    public static final int TYPE_PROXIMITY = 8;
    public static final int TYPE_GRAVITY = 9;
    public static final int TYPE_LINEAR_ACCELERATION = 10;
    public static final int TYPE_ROTATION_VECTOR = 11;
    public static final int TYPE_RELATIVE_HUMIDITY = 12;
    public static final int TYPE_AMBIENT_TEMPERATURE = 13;
    public static final int TYPE_MAGNETIC_FIELD_UNCALIBRATED = 14;
    public static final int TYPE_GAME_ROTATION_VECTOR = 15;
    public static final int TYPE_GYROSCOPE_UNCALIBRATED = 16;
    public static final int TYPE_SIGNIFICANT_MOTION = 17;
    public static final int TYPE_STEP_DETECTOR = 18;
    public static final int TYPE_STEP_COUNTER = 19;
    public static final int TYPE_GEOMAGNETIC_ROTATION_VECTOR = 20;
    public static final int TYPE_HEART_RATE = 21;
    public static final int TYPE_ACCELEROMETER_UNCALIBRATED = 35;

    public static final String STRING_TYPE_ACCELEROMETER = "android.sensor.accelerometer";
    public static final String STRING_TYPE_MAGNETIC_FIELD = "android.sensor.magnetic_field";
    public static final String STRING_TYPE_ORIENTATION = "android.sensor.orientation";
    public static final String STRING_TYPE_GYROSCOPE = "android.sensor.gyroscope";
    public static final String STRING_TYPE_GRAVITY = "android.sensor.gravity";
    public static final String STRING_TYPE_LINEAR_ACCELERATION = "android.sensor.linear_acceleration";
    public static final String STRING_TYPE_ROTATION_VECTOR = "android.sensor.rotation_vector";
    public static final String STRING_TYPE_GEOMAGNETIC_ROTATION_VECTOR = "android.sensor.geomagnetic_rotation_vector";

    public static final int REPORTING_MODE_CONTINUOUS = 0;
    public static final int REPORTING_MODE_ON_CHANGE = 1;
    public static final int REPORTING_MODE_ONE_SHOT = 2;
    public static final int REPORTING_MODE_SPECIAL_TRIGGER = 3;

    private final int type;
    private final String name;
    private final String stringType;
    private final float maximumRange;
    private final float resolution;

    Sensor(int type, String name, String stringType, float maximumRange, float resolution) {
        this.type = type;
        this.name = name;
        this.stringType = stringType;
        this.maximumRange = maximumRange;
        this.resolution = resolution;
    }

    public int getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getStringType() {
        return stringType;
    }

    public String getVendor() {
        return "Codename One";
    }

    public int getVersion() {
        return 1;
    }

    public float getMaximumRange() {
        return maximumRange;
    }

    public float getResolution() {
        return resolution;
    }

    /// Unknown here; Android reports milliamperes.
    public float getPower() {
        return 0;
    }

    /// The fastest sampling Codename One asks the platform for, in
    /// microseconds.
    public int getMinDelay() {
        return 10000;
    }

    public int getMaxDelay() {
        return 1000000;
    }

    public int getReportingMode() {
        return REPORTING_MODE_CONTINUOUS;
    }

    public int getFifoMaxEventCount() {
        return 0;
    }

    public int getFifoReservedEventCount() {
        return 0;
    }

    public boolean isWakeUpSensor() {
        return false;
    }

    public boolean isDynamicSensor() {
        return false;
    }

    public int getId() {
        return type;
    }

    @Override
    public String toString() {
        return "{Sensor name=\"" + name + "\", vendor=\"Codename One\", type=" + type + "}";
    }
}
