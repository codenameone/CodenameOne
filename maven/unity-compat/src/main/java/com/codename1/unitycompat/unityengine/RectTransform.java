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

import UnityEngine.Vector2;

/// `UnityEngine.RectTransform`: a rectangle laid out against its parent's,
/// the transform of everything under a [Canvas].
///
/// The layout is the one Unity's manual describes. The two anchors are
/// points of the parent rectangle, as fractions of it. `sizeDelta` is how
/// much bigger than the rectangle between the anchors this one is, and
/// `anchoredPosition` is where its pivot sits relative to the pivot's own
/// place between the anchors.
///
/// A rectangle is scaled about its pivot by its transform's scale. It is
/// not rotated, and its plain position is not derived from its layout.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class RectTransform extends Transform {
    float anchorMinX = 0.5f;
    float anchorMinY = 0.5f;
    float anchorMaxX = 0.5f;
    float anchorMaxY = 0.5f;
    float anchoredX;
    float anchoredY;
    float sizeX = 100f;
    float sizeY = 100f;
    float pivotX = 0.5f;
    float pivotY = 0.5f;

    /// What a scene file sets.
    public void $layout(float minX, float minY, float maxX, float maxY, float posX, float posY, float sx, float sy,
            float px, float py) {
        anchorMinX = minX;
        anchorMinY = minY;
        anchorMaxX = maxX;
        anchorMaxY = maxY;
        anchoredX = posX;
        anchoredY = posY;
        sizeX = sx;
        sizeY = sy;
        pivotX = px;
        pivotY = py;
    }

    /// The size and the pivot a scene gave, as `out[0..3]`: width, height,
    /// pivot across and pivot up. It is the rectangle itself for one that
    /// is under no canvas, where there is nothing to stretch it across.
    public void $box(float[] out) {
        out[0] = sizeX;
        out[1] = sizeY;
        out[2] = pivotX;
        out[3] = pivotY;
    }

    @Override
    public Component $new() {
        return new RectTransform();
    }

    @Override
    public void $copyFrom(Component source) {
        super.$copyFrom(source);
        RectTransform r = (RectTransform) source;
        $layout(r.anchorMinX, r.anchorMinY, r.anchorMaxX, r.anchorMaxY, r.anchoredX, r.anchoredY, r.sizeX, r.sizeY,
                r.pivotX, r.pivotY);
    }

    /// This rectangle in canvas units, y up from the canvas's bottom left,
    /// as `out[0..3]`: x, y, width, height -- and in `out[4..5]` how much
    /// it has been scaled by, across and up, by its own scale and that of
    /// every rectangle above it. `canvasWidth` and `canvasHeight` are the
    /// size of the canvas, whose own rectangle -- and that of anything not
    /// under a `RectTransform` -- is all of it, unscaled.
    ///
    /// A rectangle is laid out in its parent's own units and then scaled
    /// about its pivot, which is what a transform's scale means; so a
    /// scaled button's label, laid out inside the button, grows with it.
    public void $rect(float canvasWidth, float canvasHeight, float[] out) {
        boolean root = gameObject.GetComponent(Canvas.class) != null || !(parent instanceof RectTransform);
        if (root) {
            out[0] = 0f;
            out[1] = 0f;
            out[2] = canvasWidth;
            out[3] = canvasHeight;
            out[4] = 1f;
            out[5] = 1f;
            return;
        }
        ((RectTransform) parent).$rect(canvasWidth, canvasHeight, out);
        float parentScaleX = out[4];
        float parentScaleY = out[5];
        // The parent's size in its own units, before it was scaled.
        float parentWidth = parentScaleX != 0f ? out[2] / parentScaleX : 0f;
        float parentHeight = parentScaleY != 0f ? out[3] / parentScaleY : 0f;
        float spanX = parentWidth * (anchorMaxX - anchorMinX);
        float spanY = parentHeight * (anchorMaxY - anchorMinY);
        float fromX = parentWidth * anchorMinX;
        float fromY = parentHeight * anchorMinY;
        float w = spanX + sizeX;
        float h = spanY + sizeY;
        float ax = spanX * pivotX;
        float ay = spanY * pivotY;
        // Where the pivot is, in the parent's units from its bottom left.
        float pivotAtX = fromX + ax + anchoredX;
        float pivotAtY = fromY + ay + anchoredY;
        float scaledW = w * scaleX;
        float scaledH = h * scaleY;
        float px = scaledW * pivotX;
        float py = scaledH * pivotY;
        float left = pivotAtX - px;
        float bottom = pivotAtY - py;
        float worldLeft = left * parentScaleX;
        float worldBottom = bottom * parentScaleY;
        out[0] = out[0] + worldLeft;
        out[1] = out[1] + worldBottom;
        out[2] = scaledW * parentScaleX;
        out[3] = scaledH * parentScaleY;
        out[4] = parentScaleX * scaleX;
        out[5] = parentScaleY * scaleY;
    }

    public Vector2 get_anchorMin(Vector2 ret) {
        ret.x = anchorMinX;
        ret.y = anchorMinY;
        return ret;
    }

    public void set_anchorMin(Vector2 value) {
        anchorMinX = value.x;
        anchorMinY = value.y;
    }

    public Vector2 get_anchorMax(Vector2 ret) {
        ret.x = anchorMaxX;
        ret.y = anchorMaxY;
        return ret;
    }

    public void set_anchorMax(Vector2 value) {
        anchorMaxX = value.x;
        anchorMaxY = value.y;
    }

    public Vector2 get_anchoredPosition(Vector2 ret) {
        ret.x = anchoredX;
        ret.y = anchoredY;
        return ret;
    }

    public void set_anchoredPosition(Vector2 value) {
        anchoredX = value.x;
        anchoredY = value.y;
    }

    public Vector2 get_sizeDelta(Vector2 ret) {
        ret.x = sizeX;
        ret.y = sizeY;
        return ret;
    }

    public void set_sizeDelta(Vector2 value) {
        sizeX = value.x;
        sizeY = value.y;
    }

    public Vector2 get_pivot(Vector2 ret) {
        ret.x = pivotX;
        ret.y = pivotY;
        return ret;
    }

    public void set_pivot(Vector2 value) {
        pivotX = value.x;
        pivotY = value.y;
    }
}
