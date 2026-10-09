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
package com.codename1.unitycompat.unityengine;

/// `UnityEngine.Canvas`: the rectangle a user interface is laid out in.
///
/// Two of Unity's three render modes are implemented, and on the surface
/// they place things identically -- the canvas covers the whole of it:
///
/// - **Screen Space - Overlay** is drawn over everything the camera sees.
/// - **Screen Space - Camera** stands `planeDistance` in front of its
///   camera and is sorted among the sprites, by sorting layer, order and
///   that distance, so a sprite can be in front of it. It turns with the
///   camera, so it is never seen rotated. With no camera set it behaves as
///   an overlay, as Unity's manual says.
///
/// **World Space** is not implemented; such a canvas is not drawn, and the
/// scene compiler says so.
///
/// A canvas that is switched off, or on an inactive object, draws nothing.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class Canvas extends Behaviour {
    int renderMode;
    Camera worldCamera;
    int sortingOrder;
    int sortingLayerID;
    float planeDistance = 100f;
    /// Surface pixels per canvas unit; the [CanvasScaler
    /// com.codename1.unitycompat.unityengine.ui.CanvasScaler] beside the
    /// canvas decides it, and it is refreshed whenever the canvas is drawn.
    float scaleFactor = 1f;

    /// What a scene file sets.
    public void $setup(int mode, Camera camera, int order, int layerId, float distance) {
        renderMode = mode;
        worldCamera = camera;
        sortingOrder = order;
        sortingLayerID = layerId;
        planeDistance = distance;
    }

    @Override
    public Component $new() {
        return new Canvas();
    }

    @Override
    public void $copyFrom(Component source) {
        super.$copyFrom(source);
        Canvas c = (Canvas) source;
        java.lang.Object camera = UnityRuntime.$remap(c.worldCamera);
        $setup(c.renderMode, camera instanceof Camera ? (Camera) camera : null, c.sortingOrder, c.sortingLayerID,
                c.planeDistance);
        scaleFactor = c.scaleFactor;
    }

    public int get_renderMode() {
        return renderMode;
    }

    public void set_renderMode(int value) {
        renderMode = value;
    }

    public Camera get_worldCamera() {
        return worldCamera;
    }

    public void set_worldCamera(Camera value) {
        worldCamera = value;
    }

    public int get_sortingOrder() {
        return sortingOrder;
    }

    public void set_sortingOrder(int value) {
        sortingOrder = value;
    }

    public float get_scaleFactor() {
        return scaleFactor;
    }

    public void set_scaleFactor(float value) {
        scaleFactor = value;
    }

    public float get_planeDistance() {
        return planeDistance;
    }

    public void set_planeDistance(float value) {
        planeDistance = value;
    }
}
