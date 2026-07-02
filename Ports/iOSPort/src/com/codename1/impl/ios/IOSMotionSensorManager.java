/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.impl.ios;

import com.codename1.sensors.MotionSensorManager;

/**
 * iOS implementation of the motion sensors backed by CoreMotion
 * (CMMotionManager). It exposes the raw accelerometer, gyroscope and
 * magnetometer; the core derives gravity, linear acceleration and orientation
 * from them. The native layer converts the accelerometer reading from G units
 * to m/s^2 and matches the axis convention documented by the API (at rest, face
 * up, z reports +9.81). Access to the motion hardware requires the build
 * argument {@code ios.NSMotionUsageDescription}.
 *
 * @author Codename One
 */
public class IOSMotionSensorManager extends MotionSensorManager {

    @Override
    protected boolean isNativeSensorSupported(int type) {
        return IOSImplementation.nativeInstance.isMotionSensorSupported(type);
    }

    @Override
    protected void startNativeSensor(int type) {
        IOSImplementation.nativeInstance.startMotionSensor(type, getSamplingInterval());
    }

    @Override
    protected void stopNativeSensor(int type) {
        IOSImplementation.nativeInstance.stopMotionSensor(type);
    }

    @Override
    protected boolean readNativeSensor(int type, float[] out) {
        IOSNative ni = IOSImplementation.nativeInstance;
        if (!ni.hasMotionData(type)) {
            return false;
        }
        out[0] = ni.getMotionSensorX(type);
        out[1] = ni.getMotionSensorY(type);
        out[2] = ni.getMotionSensorZ(type);
        return true;
    }
}
