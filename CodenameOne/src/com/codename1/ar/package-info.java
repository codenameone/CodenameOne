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

/// Cross-platform augmented reality: world tracking, plane detection, hit
/// testing, anchors, 3D content placement, light estimation, image tracking
/// and face tracking, backed by ARKit on iOS and ARCore on Android.
///
/// Start at `AR`: check `AR#isSupported()` and per-feature
/// `AR#getCapabilities()`, then `AR#open(ARSessionOptions)` a session and add
/// its `ARSession#createView()` to a form. Place content by hit testing
/// screen taps (`ARSession#hitTest(float, float)`), creating an `ARAnchor` at
/// a hit and attaching an `ARNode` holding an `ARModel` (glTF or
/// `com.codename1.gpu.Mesh` geometry).
///
/// **Coordinates**: world space is right-handed, in meters, with Y up and -Z
/// forward from the initial camera direction - the convention shared by ARKit
/// and ARCore. Poses are `ARPose` (translation + quaternion), convertible to
/// `com.codename1.gpu.Matrix4` compatible matrices.
///
/// **Threading**: all listeners fire on the EDT and all getters reflect the
/// latest delivered state. High-frequency refinements are coalesced.
///
/// **Permissions and dependencies**: referencing this package makes the build
/// pipeline inject the camera permission and plist usage description plus the
/// platform AR dependency into the application automatically; apps that do
/// not use AR pay no cost. Devices without AR hardware report
/// `AR#isSupported()` false, as do the simulator-less desktop targets - the
/// Codename One simulator itself ships a simulated AR environment for
/// development.
package com.codename1.ar;
