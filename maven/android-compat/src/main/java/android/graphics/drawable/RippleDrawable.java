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
package android.graphics.drawable;

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;

/// A `<ripple>`: its content layers, plus a highlight in the ripple color
/// while pressed. The highlight is drawn through the mask layer (or the
/// content's own shape) so rounded buttons get a rounded highlight; it
/// appears at once rather than spreading from the touch point.
public class RippleDrawable extends LayerDrawable {

    public static final int RADIUS_AUTO = -1;

    private ColorStateList color;
    private boolean pressed;
    private boolean focused;
    private int maskId;
    private int radius = RADIUS_AUTO;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public RippleDrawable(ColorStateList color, Drawable content, Drawable mask) {
        super(content == null ? new Drawable[0] : new Drawable[] {content});
        this.color = color;
        if (mask != null) {
            int i = addLayer(mask);
            setId(i, android.R.id.mask);
        }
        this.maskId = android.R.id.mask;
    }

    public void setColor(ColorStateList color) {
        this.color = color;
        invalidateSelf();
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public int getRadius() {
        return radius;
    }

    @Override
    public boolean isStateful() {
        return true;
    }

    @Override
    protected boolean onStateChange(int[] state) {
        boolean p = false;
        boolean f = false;
        for (int s : state) {
            if (s == android.R.attr.state_pressed) {
                p = true;
            } else if (s == android.R.attr.state_focused) {
                f = true;
            }
        }
        boolean changed = super.onStateChange(state) || p != pressed || f != focused;
        pressed = p;
        focused = f;
        if (changed) {
            invalidateSelf();
        }
        return changed;
    }

    @Override
    public void draw(Canvas canvas) {
        Drawable mask = null;
        for (Layer l : layers) {
            if (l.drawable == null) {
                continue;
            }
            if (l.id == maskId) {
                mask = l.drawable;
                continue;
            }
            l.drawable.draw(canvas);
        }
        if (!pressed || color == null) {
            return;
        }
        int c = color.getColorForState(getState(), color.getDefaultColor());
        Drawable shape = mask;
        if (shape == null) {
            for (Layer l : layers) {
                if (l.drawable != null && l.id != maskId) {
                    shape = l.drawable;
                    break;
                }
            }
        }
        if (shape != null) {
            shape.setColorFilter(new PorterDuffColorFilter(c, PorterDuff.Mode.SRC_IN));
            shape.draw(canvas);
            shape.setColorFilter(null);
        } else {
            paint.setColor(c);
            Rect b = getBounds();
            if (radius > 0) {
                canvas.drawCircle(b.exactCenterX(), b.exactCenterY(), radius, paint);
            } else {
                canvas.drawRect(b, paint);
            }
        }
    }
}
