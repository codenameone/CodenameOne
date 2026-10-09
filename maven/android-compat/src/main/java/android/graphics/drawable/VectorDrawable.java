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
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;

import java.util.ArrayList;
import java.util.List;

/// A `<vector>`: groups of paths in a viewport, scaled to the bounds. Built by
/// the drawable inflater from the compiled XML; colors may be theme-dependent
/// and are resolved when the drawable is inflated.
public class VectorDrawable extends Drawable {

    /// A transform group.
    public static final class Group {
        public float rotation;
        public float pivotX;
        public float pivotY;
        public float scaleX = 1;
        public float scaleY = 1;
        public float translateX;
        public float translateY;
        public final List<Object> children = new ArrayList<Object>();

        Matrix matrix() {
            Matrix m = new Matrix();
            m.postTranslate(-pivotX, -pivotY);
            m.postScale(scaleX, scaleY);
            m.postRotate(rotation);
            m.postTranslate(translateX + pivotX, translateY + pivotY);
            return m;
        }
    }

    /// A filled and/or stroked path.
    public static final class VPath {
        public Path path;
        public ColorStateList fill;
        public float fillAlpha = 1;
        public ColorStateList stroke;
        public float strokeWidth;
        public float strokeAlpha = 1;
        public Paint.Cap cap = Paint.Cap.BUTT;
        public Paint.Join join = Paint.Join.MITER;
        public float miter = 4;
        public boolean evenOdd;
    }

    /// A clip applied to the paths after it in the same group.
    public static final class ClipPath {
        public Path path;
    }

    private final Group root = new Group();
    private float viewportWidth;
    private float viewportHeight;
    private int intrinsicWidth;
    private int intrinsicHeight;
    private int alpha = 255;
    private ColorFilter colorFilter;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public VectorDrawable() {
    }

    public Group getRoot() {
        return root;
    }

    public void setViewport(float w, float h) {
        viewportWidth = w;
        viewportHeight = h;
    }

    public void setIntrinsicSize(int w, int h) {
        intrinsicWidth = w;
        intrinsicHeight = h;
    }

    @Override
    public int getIntrinsicWidth() {
        return intrinsicWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        return intrinsicHeight;
    }

    @Override
    public boolean isStateful() {
        return super.isStateful() || stateful(root);
    }

    private static boolean stateful(Group g) {
        for (Object o : g.children) {
            if (o instanceof Group && stateful((Group) o)) {
                return true;
            }
            if (o instanceof VPath) {
                VPath p = (VPath) o;
                if ((p.fill != null && p.fill.isStateful()) || (p.stroke != null && p.stroke.isStateful())) {
                    return true;
                }
            }
        }
        return false;
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
    public void draw(Canvas canvas) {
        Rect b = getBounds();
        if (b.isEmpty() || viewportWidth <= 0 || viewportHeight <= 0) {
            return;
        }
        canvas.save();
        canvas.clipRect(b);
        canvas.translate(b.left, b.top);
        float sx = b.width() / viewportWidth;
        float sy = b.height() / viewportHeight;
        if (isAutoMirrored() && getLayoutDirection() == 1) {
            canvas.translate(b.width(), 0);
            canvas.scale(-sx, sy);
        } else {
            canvas.scale(sx, sy);
        }
        ColorFilter cf = effectiveColorFilter(colorFilter);
        drawGroup(canvas, root, cf instanceof PorterDuffColorFilter ? (PorterDuffColorFilter) cf : null);
        canvas.restore();
    }

    private void drawGroup(Canvas canvas, Group g, PorterDuffColorFilter tint) {
        canvas.save();
        if (g != root) {
            canvas.concat(g.matrix());
        }
        for (Object o : g.children) {
            if (o instanceof Group) {
                drawGroup(canvas, (Group) o, tint);
            } else if (o instanceof ClipPath) {
                canvas.clipPath(((ClipPath) o).path);
            } else if (o instanceof VPath) {
                drawPath(canvas, (VPath) o, tint);
            }
        }
        canvas.restore();
    }

    private int color(ColorStateList csl, float pathAlpha, PorterDuffColorFilter tint) {
        int c = csl.getColorForState(getState(), csl.getDefaultColor());
        if (tint != null) {
            int t = tint.getColor();
            c = (t & 0xffffff) | ((((t >>> 24) * (c >>> 24)) / 255) << 24);
        }
        int a = Math.round((c >>> 24) * pathAlpha * alpha / 255f);
        return (c & 0xffffff) | (Math.max(0, Math.min(255, a)) << 24);
    }

    private void drawPath(Canvas canvas, VPath p, PorterDuffColorFilter tint) {
        if (p.path == null) {
            return;
        }
        p.path.setFillType(p.evenOdd ? Path.FillType.EVEN_ODD : Path.FillType.WINDING);
        if (p.fill != null) {
            int c = color(p.fill, p.fillAlpha, tint);
            if ((c >>> 24) != 0) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(c);
                canvas.drawPath(p.path, paint);
            }
        }
        if (p.stroke != null && p.strokeWidth > 0) {
            int c = color(p.stroke, p.strokeAlpha, tint);
            if ((c >>> 24) != 0) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(p.strokeWidth);
                paint.setStrokeCap(p.cap);
                paint.setStrokeJoin(p.join);
                paint.setStrokeMiter(p.miter);
                paint.setColor(c);
                canvas.drawPath(p.path, paint);
            }
        }
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
    public void setColorFilter(ColorFilter cf) {
        colorFilter = cf;
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
}
