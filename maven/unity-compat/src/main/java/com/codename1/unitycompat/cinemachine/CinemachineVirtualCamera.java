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

/// `Cinemachine.CinemachineVirtualCamera`: follows a target.
///
/// How it follows is the body component of its pipeline, which Unity
/// keeps on a hidden child object: a [CinemachineFramingTransposer], which
/// keeps the target at a place on the screen, or a [CinemachineTransposer],
/// which keeps a fixed offset from it. With neither, or with nothing to
/// follow, it stays where it is.
///
/// Aiming at `LookAt`, noise, extensions such as the confiner, and blends
/// between virtual cameras are not implemented: the camera cuts.
@SuppressWarnings({"PMD.MethodNamingConventions", "PMD.FieldNamingConventions"}) // C# member names
public final class CinemachineVirtualCamera extends CinemachineVirtualCameraBase {
    public Transform m_LookAt;
    public Transform m_Follow;
    public LensSettings m_Lens = new LensSettings();
    private CinemachineComponentBase body;
    private boolean looked;

    /// What a scene file sets.
    public void $setup(int priority, float orthographicSize) {
        m_Priority = priority;
        m_Lens.OrthographicSize = orthographicSize;
    }

    @Override
    public Component $new() {
        return new CinemachineVirtualCamera();
    }

    @Override
    public void $copyFrom(Component source) {
        super.$copyFrom(source);
        CinemachineVirtualCamera v = (CinemachineVirtualCamera) source;
        m_LookAt = v.m_LookAt;
        m_Follow = v.m_Follow;
        m_Lens.$assign(v.m_Lens);
    }

    @Override
    public Transform get_LookAt() {
        return m_LookAt;
    }

    @Override
    public void set_LookAt(Transform value) {
        m_LookAt = value;
    }

    @Override
    public Transform get_Follow() {
        return m_Follow;
    }

    @Override
    public void set_Follow(Transform value) {
        m_Follow = value;
    }

    @Override
    void advance(float dt, float aspect) {
        if (!looked) {
            looked = true;
            Object b = GetComponentInChildren(CinemachineComponentBase.class);
            body = b instanceof CinemachineComponentBase ? (CinemachineComponentBase) b : null;
        }
        Transform target = m_Follow;
        if (body == null || target == null || !target.$live()) {
            if (body != null) {
                body.lost();
            }
            return;
        }
        float halfHeight = m_Lens.OrthographicSize;
        body.follow(get_transform(), target, dt, halfHeight * aspect, halfHeight);
    }

    @Override
    float lensSize() {
        return m_Lens.OrthographicSize;
    }
}
