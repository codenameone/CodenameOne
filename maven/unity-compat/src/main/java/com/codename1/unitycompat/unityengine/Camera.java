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

import UnityEngine.Color;
import UnityEngine.Vector3;

/// `UnityEngine.Camera`, orthographic: which part of the world is on the
/// surface, and how big.
///
/// `orthographicSize` is half the height of what the camera sees, in world
/// units, so one unit is `surfaceHeight / (2 * orthographicSize)` pixels
/// whatever the surface's shape; a wider surface shows more of the world to
/// the sides. The camera's position lands on the centre of the surface, and
/// a camera turned about z turns the world the other way about that centre
/// -- which is what a camera riding on a turning ship does. World y points
/// up and surface y points down, so the vertical axis is flipped on the way
/// through.
///
/// A perspective camera is not implemented; one is treated as orthographic
/// with the size the scene file gives.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class Camera extends Behaviour {
    private final Color background = new Color();
    float orthographicSize = 5f;

    public Camera() {
        // Unity's default clear colour for a new camera.
        background.r = 0.19215687f;
        background.g = 0.3019608f;
        background.b = 0.4745098f;
        background.a = 0f;
    }

    public static Camera get_main() {
        return UnityRuntime.camera(true);
    }

    @Override
    public Component $new() {
        return new Camera();
    }

    @Override
    public void $copyFrom(Component source) {
        super.$copyFrom(source);
        Camera c = (Camera) source;
        background.$assign(c.background);
        orthographicSize = c.orthographicSize;
    }

    public boolean get_orthographic() {
        return true;
    }

    public void set_orthographic(boolean value) {
    }

    public float get_orthographicSize() {
        return orthographicSize;
    }

    public void set_orthographicSize(float value) {
        orthographicSize = value;
    }

    public Color get_backgroundColor(Color ret) {
        ret.$assign(background);
        return ret;
    }

    public void set_backgroundColor(Color value) {
        background.$assign(value);
    }

    /// What a scene file sets.
    public void $background(float r, float g, float b, float a) {
        background.r = r;
        background.g = g;
        background.b = b;
        background.a = a;
    }

    public int get_pixelWidth() {
        return Screen.width;
    }

    public int get_pixelHeight() {
        return Screen.height;
    }

    public float get_aspect() {
        return Screen.height == 0 ? 1f : (float) Screen.width / (float) Screen.height;
    }

    /// Pixels per world unit on a surface of this height.
    float scale(int viewHeight) {
        float span = orthographicSize * 2f;
        return span > 0f ? viewHeight / span : 0f;
    }

    /// Surface pixels with y up from the bottom left, as Unity's screen
    /// space is; z is the distance in front of the camera.
    public Vector3 WorldToScreenPoint(Vector3 position, Vector3 ret) {
        Transform t = gameObject.transform;
        t.update();
        float s = scale(Screen.height);
        float dx = position.x - t.wx;
        float dy = position.y - t.wy;
        float a = dx * t.wcos;
        float b = dy * t.wsin;
        float c = dy * t.wcos;
        float d = dx * t.wsin;
        float vx = (a + b) * s;
        float vy = (c - d) * s;
        float dz = position.z - t.wz;
        ret.x = Screen.width / 2f + vx;
        ret.y = Screen.height / 2f + vy;
        ret.z = dz;
        return ret;
    }

    public Vector3 ScreenToWorldPoint(Vector3 position, Vector3 ret) {
        Transform t = gameObject.transform;
        t.update();
        float s = scale(Screen.height);
        float vx = position.x - Screen.width / 2f;
        float vy = position.y - Screen.height / 2f;
        float dz = position.z;
        if (s > 0f) {
            vx = vx / s;
            vy = vy / s;
        }
        float a = vx * t.wcos;
        float b = vy * t.wsin;
        float c = vx * t.wsin;
        float d = vy * t.wcos;
        ret.x = t.wx + (a - b);
        ret.y = t.wy + (c + d);
        ret.z = t.wz + dz;
        return ret;
    }

    /// As [#WorldToScreenPoint(Vector3, Vector3)] with the surface one
    /// unit wide and one high.
    public Vector3 WorldToViewportPoint(Vector3 position, Vector3 ret) {
        WorldToScreenPoint(position, ret);
        ret.x = Screen.width == 0 ? 0f : ret.x / Screen.width;
        ret.y = Screen.height == 0 ? 0f : ret.y / Screen.height;
        return ret;
    }

    public Vector3 ViewportToWorldPoint(Vector3 position, Vector3 ret) {
        float px = position.x * Screen.width;
        float py = position.y * Screen.height;
        float pz = position.z;
        ret.x = px;
        ret.y = py;
        ret.z = pz;
        return ScreenToWorldPoint(ret, ret);
    }

    int backgroundArgb() {
        return argb(background.r, background.g, background.b, 1f);
    }

    static int argb(float r, float g, float b, float a) {
        return (channel(a) << 24) | (channel(r) << 16) | (channel(g) << 8) | channel(b);
    }

    private static int channel(float v) {
        if (!(v > 0f)) { // NOPMD LogicInversion
            return 0;
        }
        if (v >= 1f) {
            return 255;
        }
        return (int) (v * 255f + 0.5f);
    }
}
