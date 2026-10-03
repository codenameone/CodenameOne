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
package androidx.core.widget;

import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.widget.ImageView;

/// Static helpers over `ImageView`.
public final class ImageViewCompat {

    private ImageViewCompat() {
    }

    public static void setImageTintList(ImageView view, ColorStateList tint) {
        view.setImageTintList(tint);
    }

    public static ColorStateList getImageTintList(ImageView view) {
        return view.getImageTintList();
    }

    public static void setImageTintMode(ImageView view, PorterDuff.Mode mode) {
        view.setImageTintMode(mode);
    }

    public static PorterDuff.Mode getImageTintMode(ImageView view) {
        return view.getImageTintMode();
    }
}
