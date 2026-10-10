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

/// One thing to draw this frame, in surface pixels: a sprite, or a run of
/// text when [#text] is not null. See [Camera] for how these numbers come
/// from the world.
///
/// For a sprite the fields are those of a `com.codename1.gaming.Sprite` on
/// purpose, so that painting is a copy: `x`/`y` is where the pivot lands,
/// `width` and `height` are positive, the anchor is 0..1 from the image's
/// top left, the rotation is clockwise, and a flip mirrors the image about
/// the pivot. The source rectangle says which part of the image, for a
/// sprite cut from a sheet.
///
/// For text, `x`/`y` is the top left of the rectangle the text is laid out
/// in and `width`/`height` its size. The runtime has no font and so no
/// metrics: it says what Unity was asked for, in pixels, and whatever
/// paints chooses the lines and, for best fit, the size.
public final class DrawCommand {
    /// The resource name of the image; null for text.
    public String sprite;
    /// The part of the image, in its pixels from its top left.
    public int sourceX;
    public int sourceY;
    public int sourceWidth;
    public int sourceHeight;
    public float x;
    public float y;
    public float width;
    public float height;
    public float anchorX;
    public float anchorY;
    /// Degrees, clockwise.
    public float rotation;
    /// ARGB, multiplied into the image; the colour of text.
    public int color;
    public boolean flipX;
    public boolean flipY;
    public int sortingOrder;
    /// The place of the sorting layer in the project's list.
    public int sortingLayer;

    /// The string to draw; null for a sprite.
    public String text;
    /// The height of the font, in surface pixels.
    public float fontSize;
    /// With [#bestFit], the largest size between these two at which the
    /// text fits its rectangle is used instead of [#fontSize].
    public boolean bestFit;
    public float minFontSize;
    public float maxFontSize;
    /// `TextAnchor`: 0..8, rows top to bottom and columns left to right.
    public int alignment;
    /// `FontStyle`: 0 normal, 1 bold, 2 italic, 3 both.
    public int fontStyle;
    /// Whether a line too long for the rectangle is broken at a space.
    public boolean wrap;

    /// How far in front of the camera; among equals the farther draws
    /// first.
    float depth;
    /// 0 for what the camera sees, 1 for what is laid over all of it.
    int group;

    DrawCommand() {
    }
}
