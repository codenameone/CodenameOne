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

import com.codename1.unitycompat.unityengine.Component;
import com.codename1.unitycompat.unityengine.MonoBehaviour;
import com.codename1.unitycompat.unityengine.Transform;

/// `Cinemachine.CinemachineVirtualCameraBase`: a place the camera could be
/// looking from. The [CinemachineBrain] on the camera puts it at the live
/// one of the highest priority.
///
/// This is not Cinemachine. It is the part a 2D game uses, written from
/// the package's public manual: see [CinemachineVirtualCamera].
@SuppressWarnings({"PMD.MethodNamingConventions", "PMD.FieldNamingConventions"}) // C# member names
public abstract class CinemachineVirtualCameraBase extends MonoBehaviour {
    public int m_Priority = 10;

    public int get_Priority() {
        return m_Priority;
    }

    public void set_Priority(int value) {
        m_Priority = value;
    }

    public abstract Transform get_LookAt();

    public abstract void set_LookAt(Transform value);

    public abstract Transform get_Follow();

    public abstract void set_Follow(Transform value);

    /// Registered among the components advanced late so that the brain
    /// finds it there; it is the brain that advances it.
    @Override
    public int $roles() {
        return TICKS_LATE;
    }

    @Override
    public void $copyFrom(Component source) {
        super.$copyFrom(source);
        m_Priority = ((CinemachineVirtualCameraBase) source).m_Priority;
    }

    /// Moves this virtual camera for a frame of `dt` seconds. `halfWidth`
    /// and `halfHeight` are what the camera would see from it, in world
    /// units.
    abstract void advance(float dt, float aspect);

    /// Half the height of what the camera should show, in world units.
    abstract float lensSize();
}
