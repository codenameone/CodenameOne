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
package com.codename1.unitycompat.cinemachine;

import com.codename1.unitycompat.unityengine.MonoBehaviour;
import com.codename1.unitycompat.unityengine.Transform;

/// The body stage of a virtual camera's pipeline: what decides where the
/// camera is, given what it follows.
public abstract class CinemachineComponentBase extends MonoBehaviour {
    /// Moves `camera` after `target` for a frame of `dt` seconds.
    /// `halfWidth` and `halfHeight` are what the camera sees, in world
    /// units.
    abstract void follow(Transform camera, Transform target, float dt, float halfWidth, float halfHeight);

    /// There is nothing to follow: the next target is one to start on.
    abstract void lost();

    /// How much of a distance is left after `dt` seconds of a damping
    /// time, which is the time the manual gives for all but a hundredth of
    /// it to be covered.
    static float left(float damping, float dt) {
        if (!(damping > 0.0001f) || !(dt > 0f)) { // NOPMD LogicInversion
            return 0f;
        }
        // ln(0.01)
        double k = -4.605170185988091 / damping;
        return (float) com.codename1.unitycompat.system.Math.Exp(k * dt);
    }
}
