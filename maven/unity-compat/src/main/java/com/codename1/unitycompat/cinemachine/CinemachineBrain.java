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

import com.codename1.unitycompat.unityengine.Camera;
import com.codename1.unitycompat.unityengine.Component;
import com.codename1.unitycompat.unityengine.MonoBehaviour;
import com.codename1.unitycompat.unityengine.Transform;
import com.codename1.unitycompat.unityengine.UnityRuntime;
import java.util.ArrayList;

/// `Cinemachine.CinemachineBrain`: puts the camera it is on where the
/// live virtual camera of the highest priority is, after every
/// `LateUpdate`.
///
/// Every live virtual camera is advanced each frame, so that one which
/// takes over is already where it should be. The change from one to
/// another is a cut: the blends the brain lists are not read, and nor is
/// its update method -- it always runs late in the frame.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class CinemachineBrain extends MonoBehaviour {
    private Camera camera;

    @Override
    public Component $new() {
        return new CinemachineBrain();
    }

    @Override
    public int $roles() {
        return TICKS_LATE;
    }

    public Camera get_OutputCamera() {
        if (camera == null) {
            Object c = GetComponent(Camera.class);
            camera = c instanceof Camera ? (Camera) c : null;
        }
        return camera;
    }

    @Override
    public void $lateTick(float dt) {
        Camera lens = get_OutputCamera();
        if (lens == null) {
            return;
        }
        float aspect = lens.get_aspect();
        ArrayList all = UnityRuntime.$lateTickers();
        CinemachineVirtualCameraBase live = null;
        for (int i = 0; i < all.size(); i++) { // NOPMD ForLoopCanBeForeach
            Object c = all.get(i);
            if (!(c instanceof CinemachineVirtualCameraBase)) {
                continue;
            }
            CinemachineVirtualCameraBase v = (CinemachineVirtualCameraBase) c;
            if (!v.$live()) {
                continue;
            }
            v.advance(dt, aspect);
            if (live == null || v.m_Priority > live.m_Priority) {
                live = v;
            }
        }
        if (live == null) {
            return;
        }
        Transform from = live.get_transform();
        get_transform().$moveTo(from.$worldX(), from.$worldY(), from.$worldZ());
        float size = live.lensSize();
        if (size > 0f) {
            lens.set_orthographicSize(size);
        }
    }
}
