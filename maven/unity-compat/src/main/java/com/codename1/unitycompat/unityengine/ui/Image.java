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

import com.codename1.unitycompat.unityengine.Component;
import com.codename1.unitycompat.unityengine.DrawCommand;
import com.codename1.unitycompat.unityengine.Sprite;

/// `UnityEngine.UI.Image`: a sprite drawn over the rectangle of its
/// `RectTransform`, in the graphic's colour.
///
/// Unlike a `SpriteRenderer`, an image has no size of its own: the sprite
/// is stretched to the rectangle, whatever its pixels per unit. With
/// `preserveAspect` it is fitted inside the rectangle instead, centred.
/// A *filled* image draws only a fraction of the sprite, cut straight
/// across, which is what a progress bar is.
///
/// An image with no sprite is a plain rectangle of its colour. The scene
/// compiler gives such an image a white sprite it generates, so that this
/// class and whatever paints have one case and not two.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public class Image extends MaskableGraphic {
    /// `Image.Type.Filled`.
    private static final int FILLED = 3;
    private Sprite sprite;
    private int type;
    private boolean preserveAspect;
    private int fillMethod;
    private int fillOrigin;
    private float fillAmount = 1f;

    /// What a scene file sets: the sprite, `Image.Type`, and for a filled
    /// image the `FillMethod`, the origin within it and the amount.
    public void $setup(Sprite image, int imageType, boolean keepAspect, int method, int origin, float amount) {
        sprite = image;
        type = imageType;
        preserveAspect = keepAspect;
        fillMethod = method;
        fillOrigin = origin;
        fillAmount = amount;
    }

    @Override
    public Component $new() {
        return new Image();
    }

    @Override
    public void $copyFrom(Component source) {
        super.$copyFrom(source);
        Image i = (Image) source;
        $setup(i.sprite, i.type, i.preserveAspect, i.fillMethod, i.fillOrigin, i.fillAmount);
    }

    /// Completes a draw command whose rectangle is this image's, in
    /// pixels from the surface's top left. False when nothing is drawn.
    public boolean $fill(DrawCommand d) {
        if (sprite == null) {
            return false;
        }
        sprite.$source(d);
        d.text = null;
        d.color = $drawColor();
        if (preserveAspect && sprite.$width() > 0 && sprite.$height() > 0 && d.width > 0f && d.height > 0f) {
            float byWidth = d.width / sprite.$width();
            float byHeight = d.height / sprite.$height();
            if (byWidth < byHeight) {
                float h = sprite.$height() * byWidth;
                float spare = d.height - h;
                d.y = d.y + spare / 2f;
                d.height = h;
            } else {
                float w = sprite.$width() * byHeight;
                float spare = d.width - w;
                d.x = d.x + spare / 2f;
                d.width = w;
            }
        }
        if (type != FILLED || fillAmount >= 1f) {
            return true;
        }
        if (!(fillAmount > 0f)) { // NOPMD LogicInversion
            return false;
        }
        if (fillMethod == 0) {
            // Horizontal: origin 0 keeps the left, 1 the right.
            int pixels = (int) (d.sourceWidth * fillAmount + 0.5f);
            float w = d.width * fillAmount;
            if (fillOrigin == 1) {
                d.sourceX += d.sourceWidth - pixels;
                d.x = d.x + (d.width - w);
            }
            d.sourceWidth = pixels;
            d.width = w;
        } else if (fillMethod == 1) {
            // Vertical: origin 0 keeps the bottom, 1 the top. The source
            // rectangle counts from the image's top.
            int pixels = (int) (d.sourceHeight * fillAmount + 0.5f);
            float h = d.height * fillAmount;
            if (fillOrigin == 0) {
                d.sourceY += d.sourceHeight - pixels;
                d.y = d.y + (d.height - h);
            }
            d.sourceHeight = pixels;
            d.height = h;
        }
        return d.sourceWidth > 0 && d.sourceHeight > 0;
    }

    public Sprite get_sprite() {
        return sprite;
    }

    public void set_sprite(Sprite value) {
        sprite = value;
    }

    public float get_fillAmount() {
        return fillAmount;
    }

    public void set_fillAmount(float value) {
        fillAmount = value < 0f ? 0f : value > 1f ? 1f : value;
    }

    public boolean get_preserveAspect() {
        return preserveAspect;
    }

    public void set_preserveAspect(boolean value) {
        preserveAspect = value;
    }
}
