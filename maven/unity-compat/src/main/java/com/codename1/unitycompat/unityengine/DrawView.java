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

/// The camera of the frame being drawn, for a component that adds its own
/// commands to the draw list: a tilemap, a particle system, a text placed
/// in the world.
///
/// It turns a place in the world into a place on the screen exactly as a
/// [SpriteRenderer] is turned, so that a tile and a sprite standing on the
/// same spot land on the same pixel.
@SuppressWarnings("PMD.MethodNamingConventions") // $-names are what generated and runtime code call
public final class DrawView {
    DrawList list;
    float viewWidth;
    float viewHeight;
    float scale;
    float centreX;
    float centreY;
    float eyeX;
    float eyeY;
    float eyeZ;
    float eyeCos = 1f;
    float eyeSin;
    float eyeTurn;

    DrawView() {
    }

    /// Screen pixels to a world unit.
    public float $scale() {
        return scale;
    }

    /// The camera's place in the world.
    public float $eyeX() {
        return eyeX;
    }

    /// The camera's place in the world.
    public float $eyeY() {
        return eyeY;
    }

    /// How far from the camera, in world units along either axis, anything
    /// can be and still be on the screen: half the diagonal of the view, so
    /// that it holds for a camera that is turned as well.
    public float $reach() {
        if (!(scale > 0f)) { // NOPMD LogicInversion
            return 0f;
        }
        float w = viewWidth / scale;
        float h = viewHeight / scale;
        return (w + h) * 0.5f;
    }

    /// Half the width of the view in world units, for a camera that is not
    /// turned.
    public float $halfWidth() {
        return scale > 0f ? viewWidth / scale * 0.5f : 0f;
    }

    /// Half the height of the view in world units, for a camera that is not
    /// turned.
    public float $halfHeight() {
        return scale > 0f ? viewHeight / scale * 0.5f : 0f;
    }

    /// Whether the camera is turned about its axis.
    public boolean $turned() {
        return eyeTurn != 0f;
    }

    /// Draws a sprite whose pivot is at a place in the world. `sx` and `sy`
    /// are the world scale, negative to mirror; `turn` is in degrees,
    /// anticlockwise. Nothing is added for a sprite that is off the screen.
    public void $sprite(Sprite sprite, float wx, float wy, float wz, float sx, float sy, float turn, int color,
            boolean flipX, boolean flipY, int sortingOrder, int sortingLayerID) {
        float unit = scale / sprite.pixelsPerUnit;
        float w = sprite.width * unit;
        float h = sprite.height * unit;
        w = w * sx;
        h = h * sy;
        boolean mirrorX = w < 0f;
        boolean mirrorY = h < 0f;
        if (mirrorX) {
            w = -w;
        }
        if (mirrorY) {
            h = -h;
        }
        float dx = wx - eyeX;
        float dy = wy - eyeY;
        float xc = dx * eyeCos;
        float ys = dy * eyeSin;
        float yc = dy * eyeCos;
        float xs = dx * eyeSin;
        float vx = (xc + ys) * scale;
        float vy = (yc - xs) * scale;
        float px = centreX + vx;
        float py = centreY - vy;
        float anchorX = sprite.pivotX;
        float anchorY = 1f - sprite.pivotY;
        float reachX = (anchorX > 0.5f ? anchorX : 1f - anchorX) * w;
        float reachY = (anchorY > 0.5f ? anchorY : 1f - anchorY) * h;
        float reach = reachX + reachY;
        if (!(reach > 0f) || px + reach < 0f || px - reach > viewWidth || py + reach < 0f // NOPMD LogicInversion
                || py - reach > viewHeight) {
            return;
        }
        DrawCommand d = list.next();
        d.sprite = sprite.resource;
        d.sourceX = sprite.sourceX;
        d.sourceY = sprite.sourceY;
        d.sourceWidth = sprite.width;
        d.sourceHeight = sprite.height;
        d.text = null;
        d.x = px;
        d.y = py;
        d.width = w;
        d.height = h;
        d.anchorX = anchorX;
        d.anchorY = anchorY;
        d.flipX = flipX != mirrorX;
        d.flipY = flipY != mirrorY;
        float around = turn - eyeTurn;
        d.rotation = around == 0f ? 0f : -around;
        d.color = color;
        d.sortingOrder = sortingOrder;
        d.sortingLayer = UnityRuntime.sortingLayer(sortingLayerID);
        d.depth = wz - eyeZ;
        d.group = 0;
    }

    /// Starts a command for text in a rectangle of the world: `left` and
    /// `top` are its top left corner, `width` and `height` its size, all in
    /// world units. The caller fills in the text and how it is set; the
    /// font sizes it gives are in screen pixels, which is what
    /// [#$scale()] converts to. Null when the rectangle is off the screen.
    public DrawCommand $text(float left, float top, float wz, float width, float height, int sortingOrder,
            int sortingLayerID) {
        float dx = left - eyeX;
        float dy = top - eyeY;
        float vx = dx * scale;
        float vy = dy * scale;
        float px = centreX + vx;
        float py = centreY - vy;
        float w = width * scale;
        float h = height * scale;
        if (!(w > 0f) || !(h > 0f) || px + w < 0f || px > viewWidth || py + h < 0f // NOPMD LogicInversion
                || py > viewHeight) {
            return null;
        }
        DrawCommand d = list.next();
        d.sprite = null;
        d.sourceX = 0;
        d.sourceY = 0;
        d.sourceWidth = 0;
        d.sourceHeight = 0;
        d.x = px;
        d.y = py;
        d.width = w;
        d.height = h;
        d.anchorX = 0f;
        d.anchorY = 0f;
        d.rotation = 0f;
        d.flipX = false;
        d.flipY = false;
        d.sortingOrder = sortingOrder;
        d.sortingLayer = UnityRuntime.sortingLayer(sortingLayerID);
        d.depth = wz - eyeZ;
        d.group = 0;
        return d;
    }
}
