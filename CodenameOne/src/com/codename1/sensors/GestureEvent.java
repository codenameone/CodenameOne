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
package com.codename1.sensors;

/// A high level gesture detected by the framework from the motion sensor
/// stream and delivered to a [GestureListener]. The gesture detection runs in
/// the core so the same gestures are available on every platform that exposes
/// an accelerometer.
///
/// The available gestures are identified by the `TYPE_*` constants on this
/// class; register interest in one of them through
/// [MotionSensorManager#addGestureListener(int, GestureListener)].
public final class GestureEvent {

    /// The device was shaken back and forth.
    public static final int TYPE_SHAKE = 1;

    /// The device was turned face down (screen towards the ground).
    public static final int TYPE_FLIP_FACE_DOWN = 2;

    /// The device was turned face up (screen towards the sky).
    public static final int TYPE_FLIP_FACE_UP = 3;

    /// The device was tilted to the left (top edge rotating towards the user's
    /// left hand).
    public static final int TYPE_TILT_LEFT = 4;

    /// The device was tilted to the right.
    public static final int TYPE_TILT_RIGHT = 5;

    /// The top of the device was tilted away from the user (pitched forward).
    public static final int TYPE_TILT_FORWARD = 6;

    /// The top of the device was tilted towards the user (pitched backward).
    public static final int TYPE_TILT_BACKWARD = 7;

    /// The device was picked up from a resting position.
    public static final int TYPE_PICK_UP = 8;

    /// The device is in free fall (near weightlessness was detected).
    public static final int TYPE_FREE_FALL = 9;

    private final int type;
    private final double value;
    private final long timestamp;

    /// Creates a new gesture event. Application code does not normally
    /// construct these; they are delivered by the framework.
    ///
    /// #### Parameters
    ///
    /// - `type`: one of the `TYPE_*` constants
    /// - `value`: a magnitude describing the gesture; the peak acceleration in
    ///   meters per second squared for `TYPE_SHAKE`, `TYPE_PICK_UP` and
    ///   `TYPE_FREE_FALL`, or the tilt angle in radians for the tilt and flip
    ///   gestures
    /// - `timestamp`: the event time in milliseconds
    public GestureEvent(int type, double value, long timestamp) {
        this.type = type;
        this.value = value;
        this.timestamp = timestamp;
    }

    /// The gesture that occurred, one of the `TYPE_*` constants.
    public int getType() {
        return type;
    }

    /// A magnitude describing the strength of the gesture. See the constructor
    /// documentation for the meaning per gesture type.
    public double getValue() {
        return value;
    }

    /// The time at which the gesture was detected, in milliseconds, compatible
    /// with `System.currentTimeMillis()`.
    public long getTimestamp() {
        return timestamp;
    }

    /// A human readable name for the gesture type, useful for logging.
    public String getName() {
        switch (type) {
            case TYPE_SHAKE:
                return "shake";
            case TYPE_FLIP_FACE_DOWN:
                return "flipFaceDown";
            case TYPE_FLIP_FACE_UP:
                return "flipFaceUp";
            case TYPE_TILT_LEFT:
                return "tiltLeft";
            case TYPE_TILT_RIGHT:
                return "tiltRight";
            case TYPE_TILT_FORWARD:
                return "tiltForward";
            case TYPE_TILT_BACKWARD:
                return "tiltBackward";
            case TYPE_PICK_UP:
                return "pickUp";
            case TYPE_FREE_FALL:
                return "freeFall";
            default:
                return "unknown";
        }
    }

    @Override
    public String toString() {
        return "GestureEvent[" + getName() + ", value=" + value + "]";
    }
}
