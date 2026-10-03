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
package com.codename1.androidcompat.runtime;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import com.codename1.ui.Image;

/// Renders a drawable into a Codename One image, for places that take an
/// image: toolbar commands, dialog icons.
public final class DrawableImages {

    private DrawableImages() {
    }

    /// `d` drawn at `size` pixels (or its intrinsic size when `size` <= 0),
    /// tinted with `tint` unless it is 0.
    public static Image toImage(Drawable d, int size, int tint) {
        if (d == null) {
            return null;
        }
        int w = size > 0 ? size : Math.max(1, d.getIntrinsicWidth());
        int h = size > 0 ? size : Math.max(1, d.getIntrinsicHeight());
        if (size > 0 && d.getIntrinsicWidth() > 0 && d.getIntrinsicHeight() > 0) {
            float aspect = d.getIntrinsicWidth() / (float) d.getIntrinsicHeight();
            if (aspect > 1) {
                h = Math.max(1, Math.round(size / aspect));
            } else {
                w = Math.max(1, Math.round(size * aspect));
            }
        }
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        d.setBounds(0, 0, w, h);
        if (tint != 0) {
            d.setColorFilter(tint, PorterDuff.Mode.SRC_IN);
        }
        d.draw(c);
        if (tint != 0) {
            d.setColorFilter(null);
        }
        return b.getImage();
    }
}
