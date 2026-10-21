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

import UnityEngine.Bounds;
import UnityEngine.Vector2;

/// `UnityEngine.Sprite`: an image or a rectangle of one, how many of its
/// pixels make one world unit, and the point of it that sits on its
/// object's position.
///
/// It names the image as a resource and never opens it. The scene compiler
/// read the image's size and its import settings at build time, so the
/// draw list can be worked out with no image loaded -- which is what lets
/// that half run where there is no display.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class Sprite extends Object {
    final String resource;
    /// The rectangle of the image this sprite is, in pixels from the
    /// image's top left. A sheet's sprites differ only in this.
    final int sourceX;
    final int sourceY;
    final int width;
    final int height;
    final float pixelsPerUnit;
    /// 0..1 across the rectangle, measured from its bottom left as Unity
    /// does.
    final float pivotX;
    final float pivotY;
    private int textureWidth;
    private int textureHeight;
    private int[] solid;
    private Texture2D texture;

    /// What generated scene code calls for a whole image: the resource it
    /// ships as, its size in pixels, and the import settings of its
    /// `.meta`.
    public Sprite(String resource, int width, int height, float pixelsPerUnit, float pivotX, float pivotY) {
        this(resource, null, 0, 0, width, height, pixelsPerUnit, pivotX, pivotY);
    }

    /// What generated scene code calls for one sprite of a sheet. `name`
    /// is the sprite's own, null for the image's.
    public Sprite(String resource, String name, int sourceX, int sourceY, int width, int height,
            float pixelsPerUnit, float pivotX, float pivotY) {
        this.resource = resource;
        this.sourceX = sourceX;
        this.sourceY = sourceY;
        this.width = width;
        this.height = height;
        this.pixelsPerUnit = pixelsPerUnit > 0f ? pixelsPerUnit : 100f;
        this.pivotX = pivotX;
        this.pivotY = pivotY;
        if (name != null) {
            this.name = name;
        } else {
            int dot = resource.lastIndexOf('.');
            this.name = dot > 0 ? resource.substring(0, dot) : resource;
        }
    }

    /// The resource name the image is loaded by, for whatever paints.
    /// The size of the image the sprite is cut from, when it is not the
    /// sprite's own.
    public Sprite $texture(int imageWidth, int imageHeight) {
        textureWidth = imageWidth;
        textureHeight = imageHeight;
        return this;
    }

    /// Which pixels of the sprite are solid, for a tile that collides as
    /// its sprite: a row of bits for each row of pixels from the top, the
    /// leftmost pixel in the lowest bit, in as many ints as the width
    /// needs. A sprite given none is solid all over.
    public Sprite $solid(int[] rows) {
        solid = rows;
        return this;
    }

    /// Whether a pixel is solid; `row` counts from the top.
    boolean solidAt(int column, int row) {
        if (column < 0 || row < 0 || column >= width || row >= height) {
            return false;
        }
        if (solid == null) {
            return true;
        }
        int stride = (width + 31) >> 5;
        return (solid[row * stride + (column >> 5)] & (1 << (column & 31))) != 0;
    }

    boolean solidAllOver() {
        return solid == null;
    }

    public Texture2D get_texture() {
        if (texture == null) {
            texture = new Texture2D(textureWidth > 0 ? textureWidth : width, textureHeight > 0 ? textureHeight
                    : height);
            texture.name = name;
        }
        return texture;
    }

    /// The sprite's box in units, about its pivot.
    public Bounds get_bounds(Bounds ret) {
        float w = width / pixelsPerUnit;
        float h = height / pixelsPerUnit;
        float px = pivotX * w;
        float py = pivotY * h;
        ret.m_Center.x = w * 0.5f - px;
        ret.m_Center.y = h * 0.5f - py;
        ret.m_Center.z = 0f;
        ret.m_Extents.x = w * 0.5f;
        ret.m_Extents.y = h * 0.5f;
        ret.m_Extents.z = 0.1f;
        return ret;
    }

    public String $resource() {
        return resource;
    }

    /// Names this sprite's pixels as what a draw command paints.
    public void $source(DrawCommand d) {
        d.sprite = resource;
        d.sourceX = sourceX;
        d.sourceY = sourceY;
        d.sourceWidth = width;
        d.sourceHeight = height;
    }

    /// The sprite's width in pixels.
    public int $width() {
        return width;
    }

    public int $height() {
        return height;
    }

    public float get_pixelsPerUnit() {
        return pixelsPerUnit;
    }

    /// In pixels from the rectangle's bottom left, which is how Unity
    /// reports it; the normalised form is what the `.meta` stores.
    public Vector2 get_pivot(Vector2 ret) {
        ret.x = pivotX * width;
        ret.y = pivotY * height;
        return ret;
    }
}
