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
import com.codename1.unitycompat.unityengine.Transform;

/// `Cinemachine.CinemachineTransposer`: keeps the camera at an offset from
/// its target, closing the distance at the rate the damping gives.
///
/// The offset is in the world's axes whatever the binding mode says; the
/// modes that turn it with the target are not implemented.
@SuppressWarnings({"PMD.MethodNamingConventions", "PMD.FieldNamingConventions"}) // C# member names
public final class CinemachineTransposer extends CinemachineComponentBase {
    public float m_FollowOffsetX;
    public float m_FollowOffsetY;
    public float m_FollowOffsetZ = -10f;
    public float m_XDamping = 1f;
    public float m_YDamping = 1f;
    private boolean following;

    /// What a scene file sets.
    public void $setup(float offsetX, float offsetY, float offsetZ, float dampX, float dampY) {
        m_FollowOffsetX = offsetX;
        m_FollowOffsetY = offsetY;
        m_FollowOffsetZ = offsetZ;
        m_XDamping = dampX;
        m_YDamping = dampY;
    }

    @Override
    public Component $new() {
        return new CinemachineTransposer();
    }

    @Override
    public void $copyFrom(Component source) {
        super.$copyFrom(source);
        CinemachineTransposer f = (CinemachineTransposer) source;
        $setup(f.m_FollowOffsetX, f.m_FollowOffsetY, f.m_FollowOffsetZ, f.m_XDamping, f.m_YDamping);
    }

    @Override
    void lost() {
        following = false;
    }

    @Override
    void follow(Transform camera, Transform target, float dt, float halfWidth, float halfHeight) {
        float wantX = target.$worldX() + m_FollowOffsetX;
        float wantY = target.$worldY() + m_FollowOffsetY;
        float z = target.$worldZ() + m_FollowOffsetZ;
        if (!following) {
            following = true;
            camera.$moveTo(wantX, wantY, z);
            return;
        }
        float cx = camera.$worldX();
        float cy = camera.$worldY();
        float moveX = (wantX - cx) * (1f - left(m_XDamping, dt));
        float moveY = (wantY - cy) * (1f - left(m_YDamping, dt));
        camera.$moveTo(cx + moveX, cy + moveY, z);
    }
}
