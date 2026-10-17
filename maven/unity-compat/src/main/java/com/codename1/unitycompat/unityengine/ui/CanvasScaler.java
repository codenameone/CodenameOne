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

import UnityEngine.Vector2;
import com.codename1.unitycompat.unityengine.Component;
import com.codename1.unitycompat.unityengine.eventsystems.UIBehaviour;

/// `UnityEngine.UI.CanvasScaler`: how many surface pixels one canvas unit
/// is.
///
/// - **Constant Pixel Size**: the scale factor, whatever the surface.
/// - **Scale With Screen Size**: the canvas is designed at a reference
///   resolution and scaled to the surface. Matching width (0) scales by the
///   ratio of the widths, matching height (1) by that of the heights;
///   *Expand* takes the smaller of the two so that nothing is cut off, and
///   *Shrink* the larger.
/// - **Constant Physical Size** needs the display's density, which is not
///   known here, and is treated as a scale of one.
///
/// A match strictly between 0 and 1 is blended in a straight line between
/// the two ratios. Unity's manual says only that it is "somewhere in
/// between", so its exact curve may differ; at the two ends, which is what
/// projects use, the result is exact.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public class CanvasScaler extends UIBehaviour {
    private int mode;
    private float scaleFactor = 1f;
    private float referenceWidth = 800f;
    private float referenceHeight = 600f;
    private int screenMatchMode;
    private float match;

    /// What a scene file sets.
    public void $setup(int uiScaleMode, float factor, float width, float height, int matchMode, float matchValue) {
        mode = uiScaleMode;
        scaleFactor = factor;
        referenceWidth = width;
        referenceHeight = height;
        screenMatchMode = matchMode;
        match = matchValue;
    }

    @Override
    public Component $new() {
        return new CanvasScaler();
    }

    @Override
    public void $copyFrom(Component source) {
        super.$copyFrom(source);
        CanvasScaler s = (CanvasScaler) source;
        $setup(s.mode, s.scaleFactor, s.referenceWidth, s.referenceHeight, s.screenMatchMode, s.match);
    }

    /// Surface pixels per canvas unit on a surface of this size.
    public float $scale(int width, int height) {
        if (mode == 0) {
            return scaleFactor > 0f ? scaleFactor : 1f;
        }
        if (mode != 1 || !(referenceWidth > 0f) || !(referenceHeight > 0f)) { // NOPMD LogicInversion
            return 1f;
        }
        float byWidth = width / referenceWidth;
        float byHeight = height / referenceHeight;
        float s;
        if (screenMatchMode == 1) {
            s = byWidth < byHeight ? byWidth : byHeight;
        } else if (screenMatchMode == 2) {
            s = byWidth > byHeight ? byWidth : byHeight;
        } else if (match <= 0f) {
            s = byWidth;
        } else if (match >= 1f) {
            s = byHeight;
        } else {
            float gap = byHeight - byWidth;
            float part = gap * match;
            s = byWidth + part;
        }
        return s > 0f ? s : 1f;
    }

    public float get_scaleFactor() {
        return scaleFactor;
    }

    public void set_scaleFactor(float value) {
        scaleFactor = value;
    }

    public Vector2 get_referenceResolution(Vector2 ret) {
        ret.x = referenceWidth;
        ret.y = referenceHeight;
        return ret;
    }

    public void set_referenceResolution(Vector2 value) {
        referenceWidth = value.x;
        referenceHeight = value.y;
    }

    public float get_matchWidthOrHeight() {
        return match;
    }

    public void set_matchWidthOrHeight(float value) {
        match = value;
    }
}
