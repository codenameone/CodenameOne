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

/// Virtual reality and 360 media built entirely on the portable
/// `com.codename1.gpu` pipeline and `com.codename1.sensors` motion sensors -
/// no platform SDK dependency, so everything here runs wherever
/// `Display#isGpuSupported()` is true, including the simulator.
///
/// `VRView` renders an application `VRRenderer` in side-by-side stereo with
/// head tracking; `Media360View` displays equirectangular panorama photos
/// with drag or gyroscope look-around. The building blocks are public too:
/// `OrientationFilter` (deterministic gyro/accel/magnetometer fusion),
/// `HeadTracker` (sensor wiring plus thread-safe snapshots) and
/// `VRCameraRig` (per-eye camera math).
///
/// **Threading**: `VRRenderer` and `TextureSource` callbacks run on the
/// platform render thread; everything else is EDT-friendly component API.
package com.codename1.vr;
