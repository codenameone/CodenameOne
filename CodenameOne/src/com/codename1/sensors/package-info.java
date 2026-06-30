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

/// Cross platform access to the device motion sensors and high level gesture
/// detection.
///
/// The entry point is [com.codename1.sensors.MotionSensorManager]. It exposes
/// the raw sensors (accelerometer, gyroscope, magnetometer) plus the values the
/// framework derives from them (gravity, linear acceleration and orientation),
/// and it recognizes a set of common gestures (shake, flip, tilt, pick up and
/// free fall) entirely in the core so the gestures work on every platform that
/// has an accelerometer.
///
/// Reading the accelerometer:
///
/// ```java
/// MotionSensor accel = MotionSensorManager.getInstance().getSensor(MotionSensorManager.TYPE_ACCELEROMETER);
/// if (accel != null) {
///     accel.addListener(new MotionSensorListener() {
///         public void motionReceived(MotionEvent evt) {
///             // evt.getX(), evt.getY(), evt.getZ() in m/s^2, delivered on the EDT
///         }
///     });
/// }
/// ```
///
/// Reacting to a shake:
///
/// ```java
/// MotionSensorManager.getInstance().addGestureListener(GestureEvent.TYPE_SHAKE, new GestureListener() {
///     public void gestureDetected(GestureEvent evt) {
///         // the device was shaken
///     }
/// });
/// ```
///
/// Sampling is reference counted: the hardware sensors are only powered while a
/// listener is registered, so always remove your listeners when you no longer
/// need them. On iOS the build argument `ios.NSMotionUsageDescription` must be
/// supplied to gain access to the motion hardware.
package com.codename1.sensors;
