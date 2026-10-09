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
package android.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;

/// A ViewSwitcher of two ImageViews, animating each image change.
public class ImageSwitcher extends ViewSwitcher {

    public ImageSwitcher(Context context) {
        super(context);
    }

    public ImageSwitcher(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private ImageView next() {
        View v = getNextView();
        return v instanceof ImageView ? (ImageView) v : null;
    }

    public void setImageResource(int resid) {
        ImageView image = next();
        if (image != null) {
            image.setImageResource(resid);
        }
        showNext();
    }

    public void setImageURI(Uri uri) {
        ImageView image = next();
        if (image != null) {
            image.setImageURI(uri);
        }
        showNext();
    }

    public void setImageDrawable(Drawable drawable) {
        ImageView image = next();
        if (image != null) {
            image.setImageDrawable(drawable);
        }
        showNext();
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return ImageSwitcher.class.getName();
    }
}
