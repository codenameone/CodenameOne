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
package com.google.android.material.shape;

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/// The surface every Material widget draws: a shape filled with a color,
/// optionally stroked, with a pressed state layer, and with a shadow when
/// it has elevation.
///
/// The shadow is drawn outside the drawable's bounds, as Android draws an
/// elevated view's shadow outside the view: a view showing one returns true
/// from `drawsOutsideBounds` while it has elevation. It is an approximation
/// of Android's ambient and key-light shadows -- layered translucent shapes,
/// offset downward with the light.
public class MaterialShapeDrawable extends Drawable {

    private static final float PRESSED_ALPHA = 0.10f;
    private static final float FOCUSED_ALPHA = 0.10f;

    private ShapeAppearanceModel shape;
    private ColorStateList fillColor;
    private ColorStateList strokeColor;
    private ColorStateList stateLayerColor;
    private float strokeWidth;
    private float elevation;
    private int alpha = 255;
    private int[] state = new int[0];
    private final Rect padding = new Rect();
    private final Rect inset = new Rect();
    private boolean hasPadding;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean dashedTop;
    private float notchStart;
    private float notchEnd;

    public MaterialShapeDrawable() {
        this(new ShapeAppearanceModel());
    }

    public MaterialShapeDrawable(ShapeAppearanceModel shape) {
        this.shape = shape;
    }

    public static MaterialShapeDrawable createWithElevationOverlay(android.content.Context context, float elevation) {
        MaterialShapeDrawable d = new MaterialShapeDrawable();
        d.setElevation(elevation);
        return d;
    }

    public void setShapeAppearanceModel(ShapeAppearanceModel shape) {
        this.shape = shape;
        invalidateSelf();
    }

    public ShapeAppearanceModel getShapeAppearanceModel() {
        return shape;
    }

    public void setCornerSize(float size) {
        setShapeAppearanceModel(shape.withCornerSize(size));
    }

    public void setFillColor(ColorStateList fillColor) {
        this.fillColor = fillColor;
        invalidateSelf();
    }

    public ColorStateList getFillColor() {
        return fillColor;
    }

    public void setStrokeColor(ColorStateList strokeColor) {
        this.strokeColor = strokeColor;
        invalidateSelf();
    }

    public ColorStateList getStrokeColor() {
        return strokeColor;
    }

    public void setStrokeWidth(float strokeWidth) {
        this.strokeWidth = strokeWidth;
        invalidateSelf();
    }

    public float getStrokeWidth() {
        return strokeWidth;
    }

    public void setStroke(float strokeWidth, int strokeColor) {
        setStrokeWidth(strokeWidth);
        setStrokeColor(ColorStateList.valueOf(strokeColor));
    }

    /// The color of the state layer Material draws over a pressed or
    /// focused surface (the widget's `rippleColor`).
    public void setStateLayerColor(ColorStateList color) {
        stateLayerColor = color;
        invalidateSelf();
    }

    public void setElevation(float elevation) {
        this.elevation = elevation;
        invalidateSelf();
    }

    public float getElevation() {
        return elevation;
    }

    /// Insets the surface inside the bounds, as Material buttons do with
    /// their `insetTop`/`insetBottom` (the touch area stays the full view).
    public void setInset(int left, int top, int right, int bottom) {
        inset.set(left, top, right, bottom);
        invalidateSelf();
    }

    public void setPadding(int left, int top, int right, int bottom) {
        padding.set(left, top, right, bottom);
        hasPadding = true;
    }

    /// Leaves a gap in the top edge of the stroke between `start` and `end`
    /// (pixels from the left of the bounds): the notch an outlined text
    /// field cuts for its floating label. Equal values close it.
    public void setTopNotch(float start, float end) {
        notchStart = start;
        notchEnd = end;
        dashedTop = end > start;
        invalidateSelf();
    }

    @Override
    public boolean getPadding(Rect out) {
        if (!hasPadding) {
            return false;
        }
        out.set(padding);
        return true;
    }

    @Override
    public void setTintList(ColorStateList tint) {
        // Material tints the fill, not the whole drawable.
        if (tint != null) {
            fillColor = tint;
            invalidateSelf();
        }
    }

    @Override
    public void setTint(int tintColor) {
        setTintList(ColorStateList.valueOf(tintColor));
    }

    @Override
    public boolean isStateful() {
        return (fillColor != null && fillColor.isStateful()) || (strokeColor != null && strokeColor.isStateful())
                || stateLayerColor != null;
    }

    @Override
    protected boolean onStateChange(int[] newState) {
        state = newState == null ? new int[0] : newState;
        return true;
    }

    @Override
    public void setAlpha(int alpha) {
        this.alpha = alpha;
        invalidateSelf();
    }

    @Override
    public int getAlpha() {
        return alpha;
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    private int colorFor(ColorStateList csl) {
        if (csl == null) {
            return 0;
        }
        int c = csl.getColorForState(state, csl.getDefaultColor());
        int a = ((c >>> 24) * alpha) / 255;
        return (c & 0x00ffffff) | (a << 24);
    }

    private boolean has(int stateAttr) {
        for (int s : state) {
            if (s == stateAttr) {
                return true;
            }
        }
        return false;
    }

    /// The surface's outline in drawable coordinates.
    public Path getPath() {
        Rect b = getBounds();
        RectF r = new RectF(b.left + inset.left, b.top + inset.top, b.right - inset.right, b.bottom - inset.bottom);
        Path p = new Path();
        p.addRoundRect(r, shape.radii(r), Path.Direction.CW);
        return p;
    }

    @Override
    public void draw(Canvas canvas) {
        Rect b = getBounds();
        RectF r = new RectF(b.left + inset.left, b.top + inset.top, b.right - inset.right, b.bottom - inset.bottom);
        if (r.width() <= 0 || r.height() <= 0) {
            return;
        }
        int fill = colorFor(fillColor);
        if (elevation > 0 && (fill >>> 24) > 0) {
            drawShadow(canvas, r);
        }
        float[] radii = shape.radii(r);
        if ((fill >>> 24) > 0) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(fill);
            fillShape(canvas, r, radii);
        }
        if (stateLayerColor != null) {
            float layer = has(android.R.attr.state_pressed) ? PRESSED_ALPHA
                    : has(android.R.attr.state_focused) && has(android.R.attr.state_hovered) ? FOCUSED_ALPHA : 0f;
            if (layer > 0f) {
                int c = stateLayerColor.getColorForState(state, stateLayerColor.getDefaultColor());
                int a = Math.round(((c >>> 24) & 0xff) * layer * alpha / 255f);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor((c & 0x00ffffff) | (a << 24));
                fillShape(canvas, r, radii);
            }
        }
        int stroke = colorFor(strokeColor);
        if (strokeWidth > 0 && (stroke >>> 24) > 0) {
            float half = strokeWidth / 2f;
            RectF s = new RectF(r.left + half, r.top + half, r.right - half, r.bottom - half);
            float[] sr = shape.radii(s);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(strokeWidth);
            paint.setColor(stroke);
            if (dashedTop) {
                canvas.drawPath(notchedOutline(s, sr), paint);
            } else {
                Path p = new Path();
                p.addRoundRect(s, sr, Path.Direction.CW);
                canvas.drawPath(p, paint);
            }
            paint.setStyle(Paint.Style.FILL);
        }
    }

    private void fillShape(Canvas canvas, RectF r, float[] radii) {
        if (shape.isRoundRect(r)) {
            canvas.drawRoundRect(r, radii[0], radii[0], paint);
        } else {
            Path p = new Path();
            p.addRoundRect(r, radii, Path.Direction.CW);
            canvas.drawPath(p, paint);
        }
    }

    /// The outline with the top edge open between the notch bounds,
    /// starting just after the notch and going clockwise.
    private Path notchedOutline(RectF s, float[] radii) {
        float tl = radii[0];
        float tr = radii[2];
        float br = radii[4];
        float bl = radii[6];
        float left = getBounds().left;
        float ns = Math.max(s.left + tl, left + notchStart);
        float ne = Math.min(s.right - tr, left + notchEnd);
        Path p = new Path();
        p.moveTo(ne, s.top);
        p.lineTo(s.right - tr, s.top);
        if (tr > 0) {
            p.arcTo(new RectF(s.right - 2 * tr, s.top, s.right, s.top + 2 * tr), 270, 90, false);
        }
        p.lineTo(s.right, s.bottom - br);
        if (br > 0) {
            p.arcTo(new RectF(s.right - 2 * br, s.bottom - 2 * br, s.right, s.bottom), 0, 90, false);
        }
        p.lineTo(s.left + bl, s.bottom);
        if (bl > 0) {
            p.arcTo(new RectF(s.left, s.bottom - 2 * bl, s.left + 2 * bl, s.bottom), 90, 90, false);
        }
        p.lineTo(s.left, s.top + tl);
        if (tl > 0) {
            p.arcTo(new RectF(s.left, s.top, s.left + 2 * tl, s.top + 2 * tl), 180, 90, false);
        }
        p.lineTo(ns, s.top);
        return p;
    }

    private void drawShadow(Canvas canvas, RectF r) {
        int steps = Math.max(1, Math.min(24, Math.round(elevation)));
        float dy = elevation * 0.35f;
        // About a quarter of black at the outline, fading outward.
        int total = Math.round(64 * alpha / 255f);
        int each = Math.max(1, total / steps);
        paint.setStyle(Paint.Style.FILL);
        for (int i = steps; i >= 1; i--) {
            float spread = i * 0.75f;
            RectF s = new RectF(r.left - spread, r.top - spread + dy, r.right + spread, r.bottom + spread + dy);
            float[] radii = shape.radii(r);
            for (int k = 0; k < radii.length; k++) {
                radii[k] += spread;
            }
            paint.setColor(each << 24);
            Path p = new Path();
            p.addRoundRect(s, radii, Path.Direction.CW);
            canvas.drawPath(p, paint);
        }
    }

    /// How far the shadow reaches outside the bounds, in pixels.
    public int getShadowExtent() {
        if (elevation <= 0) {
            return 0;
        }
        int steps = Math.max(1, Math.min(24, Math.round(elevation)));
        return Math.round(steps * 0.75f + elevation * 0.35f) + 1;
    }
}
