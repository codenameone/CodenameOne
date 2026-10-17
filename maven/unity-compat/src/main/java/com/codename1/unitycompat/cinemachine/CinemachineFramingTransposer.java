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

/// `Cinemachine.CinemachineFramingTransposer`: moves the camera across
/// the screen's plane to keep its target at a place on the screen.
///
/// The place is `m_ScreenX`, `m_ScreenY`, as fractions of the screen from
/// its top left. Around it is the dead zone, in which the target moves
/// without the camera following; outside that the camera closes the
/// distance at the rate the damping gives; and at the edge of the soft
/// zone it is not damped at all, so the target never leaves it.
///
/// Lookahead, the bias of the soft zone and group framing are not
/// implemented.
@SuppressWarnings({"PMD.MethodNamingConventions", "PMD.FieldNamingConventions"}) // C# member names
public final class CinemachineFramingTransposer extends CinemachineComponentBase {
    public float m_TrackedObjectOffsetX;
    public float m_TrackedObjectOffsetY;
    public float m_XDamping = 1f;
    public float m_YDamping = 1f;
    public float m_ScreenX = 0.5f;
    public float m_ScreenY = 0.5f;
    public float m_CameraDistance = 10f;
    public float m_DeadZoneWidth;
    public float m_DeadZoneHeight;
    public boolean m_UnlimitedSoftZone;
    public float m_SoftZoneWidth = 0.8f;
    public float m_SoftZoneHeight = 0.8f;
    public boolean m_CenterOnActivate = true;
    private boolean following;

    /// What a scene file sets.
    public void $setup(float offsetX, float offsetY, float dampX, float dampY, float screenX, float screenY,
            float distance, float deadWidth, float deadHeight, boolean unlimited, float softWidth,
            float softHeight, boolean centres) {
        m_TrackedObjectOffsetX = offsetX;
        m_TrackedObjectOffsetY = offsetY;
        m_XDamping = dampX;
        m_YDamping = dampY;
        m_ScreenX = screenX;
        m_ScreenY = screenY;
        m_CameraDistance = distance;
        m_DeadZoneWidth = deadWidth;
        m_DeadZoneHeight = deadHeight;
        m_UnlimitedSoftZone = unlimited;
        m_SoftZoneWidth = softWidth;
        m_SoftZoneHeight = softHeight;
        m_CenterOnActivate = centres;
    }

    @Override
    public Component $new() {
        return new CinemachineFramingTransposer();
    }

    @Override
    public void $copyFrom(Component source) {
        super.$copyFrom(source);
        CinemachineFramingTransposer f = (CinemachineFramingTransposer) source;
        $setup(f.m_TrackedObjectOffsetX, f.m_TrackedObjectOffsetY, f.m_XDamping, f.m_YDamping, f.m_ScreenX,
                f.m_ScreenY, f.m_CameraDistance, f.m_DeadZoneWidth, f.m_DeadZoneHeight, f.m_UnlimitedSoftZone,
                f.m_SoftZoneWidth, f.m_SoftZoneHeight, f.m_CenterOnActivate);
    }

    @Override
    void lost() {
        following = false;
    }

    /// How far the camera has to go along one axis. `off` is how far the
    /// target is from its place on the screen, `dead` and `soft` half the
    /// sizes of the two zones, all in world units.
    private static float step(float off, float dead, float soft, boolean unlimited, float left) {
        float beyond = off > dead ? off - dead : off < -dead ? off + dead : 0f;
        if (beyond == 0f) {
            return 0f;
        }
        float move = beyond * (1f - left);
        if (!unlimited) {
            float reach = soft > dead ? soft : dead;
            float after = off - move;
            if (after > reach) {
                move = off - reach;
            } else if (after < -reach) {
                move = off + reach;
            }
        }
        return move;
    }

    @Override
    void follow(Transform camera, Transform target, float dt, float halfWidth, float halfHeight) {
        float tx = target.$worldX() + m_TrackedObjectOffsetX;
        float ty = target.$worldY() + m_TrackedObjectOffsetY;
        // Where the camera has to be for the target to be at its place.
        float acrossX = (m_ScreenX - 0.5f) * 2f;
        float acrossY = (m_ScreenY - 0.5f) * 2f;
        float shiftX = acrossX * halfWidth;
        float shiftY = acrossY * halfHeight;
        float wantX = tx - shiftX;
        float wantY = ty + shiftY;
        float z = target.$worldZ() - m_CameraDistance;
        if (!following) {
            following = true;
            if (m_CenterOnActivate) {
                camera.$moveTo(wantX, wantY, z);
                return;
            }
        }
        float cx = camera.$worldX();
        float cy = camera.$worldY();
        float moveX = step(wantX - cx, m_DeadZoneWidth * halfWidth, m_SoftZoneWidth * halfWidth,
                m_UnlimitedSoftZone, left(m_XDamping, dt));
        float moveY = step(wantY - cy, m_DeadZoneHeight * halfHeight, m_SoftZoneHeight * halfHeight,
                m_UnlimitedSoftZone, left(m_YDamping, dt));
        camera.$moveTo(cx + moveX, cy + moveY, z);
    }
}
