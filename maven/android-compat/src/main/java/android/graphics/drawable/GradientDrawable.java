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
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;

/// A `<shape>`: a rectangle, oval, line or ring with a solid or gradient fill,
/// an optional stroke, corner radii, padding and an intrinsic size.
public class GradientDrawable extends Drawable {

    public static final int RECTANGLE = 0;
    public static final int OVAL = 1;
    public static final int LINE = 2;
    public static final int RING = 3;
    public static final int LINEAR_GRADIENT = 0;
    public static final int RADIAL_GRADIENT = 1;
    public static final int SWEEP_GRADIENT = 2;

    public enum Orientation { TOP_BOTTOM, TR_BL, RIGHT_LEFT, BR_TL, BOTTOM_TOP, BL_TR, LEFT_RIGHT, TL_BR }

    private int shape = RECTANGLE;
    private ColorStateList solid;
    private int[] gradientColors;
    private Orientation orientation = Orientation.TOP_BOTTOM;
    private int gradientType = LINEAR_GRADIENT;
    private float gradientRadius = 0.5f;
    private float centerX = 0.5f;
    private float centerY = 0.5f;
    private int strokeWidth = -1;
    private ColorStateList strokeColor;
    private float radius;
    private float[] radii;
    private Rect padding;
    private int width = -1;
    private int height = -1;
    private int alpha = 255;
    private ColorFilter colorFilter;
    private float innerRadius = -1;
    private float innerRadiusRatio = 3f;
    private float thickness = -1;
    private float thicknessRatio = 9f;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public GradientDrawable() {
    }

    public GradientDrawable(Orientation orientation, int[] colors) {
        this.orientation = orientation;
        this.gradientColors = colors;
    }

    public void setShape(int shape) {
        this.shape = shape;
        invalidateSelf();
    }

    public int getShape() {
        return shape;
    }

    public void setColor(int argb) {
        solid = ColorStateList.valueOf(argb);
        gradientColors = null;
        invalidateSelf();
    }

    public void setColor(ColorStateList colorStateList) {
        solid = colorStateList;
        gradientColors = null;
        invalidateSelf();
    }

    public ColorStateList getColor() {
        return solid;
    }

    public void setColors(int[] colors) {
        gradientColors = colors;
        invalidateSelf();
    }

    public void setColors(int[] colors, float[] offsets) {
        setColors(colors);
    }

    public int[] getColors() {
        return gradientColors;
    }

    public void setOrientation(Orientation orientation) {
        this.orientation = orientation;
        invalidateSelf();
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public void setGradientType(int type) {
        gradientType = type;
        invalidateSelf();
    }

    public int getGradientType() {
        return gradientType;
    }

    public void setGradientRadius(float radius) {
        gradientRadius = radius;
    }

    public float getGradientRadius() {
        return gradientRadius;
    }

    public void setGradientCenter(float x, float y) {
        centerX = x;
        centerY = y;
    }

    public float getGradientCenterX() {
        return centerX;
    }

    public float getGradientCenterY() {
        return centerY;
    }

    public void setStroke(int width, int color) {
        setStroke(width, ColorStateList.valueOf(color), 0, 0);
    }

    public void setStroke(int width, ColorStateList colorStateList) {
        setStroke(width, colorStateList, 0, 0);
    }

    public void setStroke(int width, int color, float dashWidth, float dashGap) {
        setStroke(width, ColorStateList.valueOf(color), dashWidth, dashGap);
    }

    /// Dashes are not drawn: a dashed stroke is drawn solid.
    public void setStroke(int width, ColorStateList colorStateList, float dashWidth, float dashGap) {
        strokeWidth = width;
        strokeColor = colorStateList;
        invalidateSelf();
    }

    public void setCornerRadius(float radius) {
        this.radius = radius;
        radii = null;
        invalidateSelf();
    }

    public float getCornerRadius() {
        return radius;
    }

    /// Eight values: x/y radius pairs for top-left, top-right, bottom-right and
    /// bottom-left.
    public void setCornerRadii(float[] radii) {
        this.radii = radii;
        invalidateSelf();
    }

    public float[] getCornerRadii() {
        return radii == null ? null : radii.clone();
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void setPadding(int left, int top, int right, int bottom) {
        padding = new Rect(left, top, right, bottom);
    }

    public void setInnerRadius(int r) {
        innerRadius = r;
    }

    public void setInnerRadiusRatio(float r) {
        innerRadiusRatio = r;
    }

    public void setThickness(int t) {
        thickness = t;
    }

    public void setThicknessRatio(float r) {
        thicknessRatio = r;
    }

    @Override
    public int getIntrinsicWidth() {
        return width;
    }

    @Override
    public int getIntrinsicHeight() {
        return height;
    }

    @Override
    public boolean getPadding(Rect out) {
        if (padding != null) {
            out.set(padding);
            return true;
        }
        return super.getPadding(out);
    }

    @Override
    public boolean isStateful() {
        return (solid != null && solid.isStateful()) || (strokeColor != null && strokeColor.isStateful())
                || super.isStateful();
    }

    @Override
    protected boolean onStateChange(int[] state) {
        if (isStateful()) {
            invalidateSelf();
            return true;
        }
        return false;
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
        this.colorFilter = colorFilter;
        invalidateSelf();
    }

    @Override
    public ColorFilter getColorFilter() {
        return colorFilter;
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    private int modulate(int color) {
        int a = (color >>> 24) * alpha / 255;
        return (color & 0xffffff) | (a << 24);
    }

    private int filtered(int color) {
        ColorFilter cf = effectiveColorFilter(colorFilter);
        if (cf instanceof PorterDuffColorFilter) {
            int c = ((PorterDuffColorFilter) cf).getColor();
            return (c & 0xffffff) | ((((c >>> 24) * (color >>> 24)) / 255) << 24);
        }
        return color;
    }

    private Path buildPath(RectF r) {
        Path p = new Path();
        switch (shape) {
            case OVAL:
                p.addOval(r, Path.Direction.CW);
                break;
            case LINE: {
                float y = r.centerY();
                p.moveTo(r.left, y);
                p.lineTo(r.right, y);
                break;
            }
            case RING: {
                float cx = r.centerX();
                float cy = r.centerY();
                float outer = Math.min(r.width(), r.height()) / 2;
                float inner = innerRadius >= 0 ? innerRadius : r.width() / innerRadiusRatio;
                float thick = thickness >= 0 ? thickness : r.width() / thicknessRatio;
                float o = Math.min(outer, inner + thick);
                p.addCircle(cx, cy, o, Path.Direction.CW);
                p.addCircle(cx, cy, inner, Path.Direction.CCW);
                p.setFillType(Path.FillType.EVEN_ODD);
                break;
            }
            default:
                if (radii != null) {
                    p.addRoundRect(r, radii, Path.Direction.CW);
                } else if (radius > 0) {
                    p.addRoundRect(r, radius, radius, Path.Direction.CW);
                } else {
                    p.addRect(r, Path.Direction.CW);
                }
                break;
        }
        return p;
    }

    @Override
    public void draw(Canvas canvas) {
        Rect b = getBounds();
        if (b.isEmpty()) {
            return;
        }
        float inset = strokeWidth > 0 ? strokeWidth / 2f : 0;
        RectF r = new RectF(b.left + inset, b.top + inset, b.right - inset, b.bottom - inset);
        int[] state = getState();
        boolean hasFill = shape != LINE;
        if (hasFill && gradientColors != null && gradientColors.length > 0) {
            paint.setStyle(Paint.Style.FILL);
            int c0 = filtered(modulate(gradientColors[0]));
            int c1 = filtered(modulate(gradientColors[gradientColors.length - 1]));
            paint.setColor(c0);
            if (gradientType == LINEAR_GRADIENT && shape == RECTANGLE && radius <= 0 && radii == null) {
                boolean horizontal = orientation == Orientation.LEFT_RIGHT || orientation == Orientation.RIGHT_LEFT;
                boolean reversed = orientation == Orientation.RIGHT_LEFT || orientation == Orientation.BOTTOM_TOP;
                int from = reversed ? c1 : c0;
                int to = reversed ? c0 : c1;
                paint.setColor(from);
                paint.setShader(horizontal ? new LinearGradient(r.left, 0, r.right, 0, from, to, Shader.TileMode.CLAMP)
                        : new LinearGradient(0, r.top, 0, r.bottom, from, to, Shader.TileMode.CLAMP));
                canvas.drawRect(r, paint);
                paint.setShader(null);
            } else {
                // Codename One gradients are axis-aligned rectangles; for other
                // shapes fill with the gradient's midpoint color.
                paint.setColor(blend(c0, c1));
                canvas.drawPath(buildPath(r), paint);
            }
        } else if (hasFill && solid != null) {
            int c = filtered(modulate(solid.getColorForState(state, solid.getDefaultColor())));
            if ((c >>> 24) != 0) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(c);
                if (shape == RECTANGLE && radius <= 0 && radii == null) {
                    canvas.drawRect(r, paint);
                } else {
                    canvas.drawPath(buildPath(r), paint);
                }
            }
        }
        if (strokeWidth > 0 && strokeColor != null) {
            int c = filtered(modulate(strokeColor.getColorForState(state, strokeColor.getDefaultColor())));
            if ((c >>> 24) != 0) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(strokeWidth);
                paint.setColor(c);
                canvas.drawPath(buildPath(r), paint);
                paint.setStyle(Paint.Style.FILL);
            }
        }
    }

    private static int blend(int a, int b) {
        return ((((a >>> 24) + (b >>> 24)) / 2) << 24) | (((((a >> 16) & 0xff) + ((b >> 16) & 0xff)) / 2) << 16)
                | (((((a >> 8) & 0xff) + ((b >> 8) & 0xff)) / 2) << 8) | (((a & 0xff) + (b & 0xff)) / 2);
    }

    @Override
    public Drawable mutate() {
        return this;
    }
}
