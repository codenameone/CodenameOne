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
package com.codename1.unitycompat.unityengine.ui;

import UnityEngine.Color;
import com.codename1.unitycompat.unityengine.Canvas;
import com.codename1.unitycompat.unityengine.Component;
import com.codename1.unitycompat.unityengine.RectTransform;
import com.codename1.unitycompat.unityengine.Transform;
import com.codename1.unitycompat.unityengine.eventsystems.UIBehaviour;

/// `UnityEngine.UI.Graphic`: something a canvas draws, and its colour.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public abstract class Graphic extends UIBehaviour {
    private final Color color = new Color();
    int argb = 0xffffffff;
    /// What a control multiplies this graphic's colour by, to show the
    /// state it is in: a button's tint. White leaves the colour alone.
    private int tint = 0xffffffff;
    private boolean raycastTarget = true;

    protected Graphic() {
        color.r = 1f;
        color.g = 1f;
        color.b = 1f;
        color.a = 1f;
    }

    /// What a scene file sets.
    public void $color(float r, float g, float b, float a) {
        color.r = r;
        color.g = g;
        color.b = b;
        color.a = a;
        argb = (channel(a) << 24) | (channel(r) << 16) | (channel(g) << 8) | channel(b);
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

    @Override
    public void $copyFrom(Component source) {
        super.$copyFrom(source);
        Graphic g = (Graphic) source;
        $color(g.color.r, g.color.g, g.color.b, g.color.a);
        raycastTarget = g.raycastTarget;
        tint = g.tint;
    }

    /// Sets the tint a control gives this graphic, as ARGB.
    public void $tint(int value) {
        tint = value;
    }

    /// The colour this graphic is drawn in: its own, times the tint.
    public int $drawColor() {
        if (tint == 0xffffffff) {
            return argb;
        }
        return (scaled(argb >>> 24, tint >>> 24) << 24) | (scaled(argb >> 16, tint >> 16) << 16)
                | (scaled(argb >> 8, tint >> 8) << 8) | scaled(argb, tint);
    }

    private static int scaled(int a, int b) {
        return ((a & 255) * (b & 255) + 127) / 255;
    }

    /// Whether a pointer over this graphic is stopped by it. It is by
    /// default, so that a label lying over a button takes its clicks --
    /// which is why the editor offers the switch.
    /// Fills a draw command with what a graphic the runtime does not know
    /// by class shows in its rectangle, which is already in the command.
    /// False leaves the graphic undrawn.
    public boolean $paint(com.codename1.unitycompat.unityengine.DrawCommand d, float scale) {
        return false;
    }

    public boolean get_raycastTarget() {
        return raycastTarget;
    }

    public void set_raycastTarget(boolean value) {
        raycastTarget = value;
    }

    public Color get_color(Color ret) {
        ret.$assign(color);
        return ret;
    }

    public void set_color(Color value) {
        $color(value.r, value.g, value.b, value.a);
    }

    /// The canvas this is drawn by: the nearest one at or above it.
    public Canvas get_canvas() {
        return (Canvas) GetComponentInParent(Canvas.class);
    }

    public RectTransform get_rectTransform() {
        Transform t = get_transform();
        return t instanceof RectTransform ? (RectTransform) t : null;
    }
}
