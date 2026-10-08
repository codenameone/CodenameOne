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
package android.graphics;

import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Stroke;
import com.codename1.ui.Transform;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.Rectangle;

import java.util.ArrayList;

/// Android's drawing surface over a Codename One `Graphics`.
///
/// Coordinates are local to the view (or bitmap) being drawn; the canvas maps
/// them through its current matrix onto the graphics context. Translation and
/// scale are applied to the geometry directly, which every port supports;
/// rotation and skew go through the context's transform, which is skipped on
/// a port without transform support rather than drawn wrongly.
public class Canvas {

    public static final int ALL_SAVE_FLAG = 0x1F;

    private Graphics g;
    private float originX;
    private float originY;
    private int width;
    private int height;
    private final Matrix matrix = new Matrix();
    private final ArrayList<float[]> stack = new ArrayList<float[]>();
    /// The shape clip each [#stack] record restores, or null for its
    /// rectangle; parallel to the stack.
    private final ArrayList<GeneralPath> clipShapes = new ArrayList<GeneralPath>();
    private int alphaLayer = 255;
    /// The shape the last `clipPath` installed, and the clip bounds it gave;
    /// it is the current clip while those bounds are unchanged.
    private GeneralPath clipShape;
    private final int[] clipShapeBounds = new int[4];
    /// Whether this canvas draws into a bitmap rather than onto a view.
    private boolean bitmapTarget;
    /// The bitmap this canvas draws into, told of every draw so its cached
    /// pixel array is re-read; null on a view.
    private Bitmap target;
    /// The image of [#target] that `g` draws into.
    private Image targetImage;

    public Canvas() {
    }

    public Canvas(Bitmap bitmap) {
        setBitmap(bitmap);
    }

    /// A canvas drawing into `g` with its local origin at (`x`, `y`) in the
    /// context's coordinates.
    public Canvas(Graphics g, int x, int y, int width, int height) {
        bind(g, x, y, width, height);
    }

    /// Re-targets this canvas; used by the view system to reuse one canvas.
    public void bind(Graphics g, int x, int y, int width, int height) {
        this.g = g;
        this.originX = x;
        this.originY = y;
        this.width = width;
        this.height = height;
        matrix.reset();
        stack.clear();
        clipShapes.clear();
        layers.clear();
        clipShape = null;
        alphaLayer = 255;
        bitmapTarget = false;
        target = null;
        targetImage = null;
    }

    public Graphics getGraphics() {
        return g;
    }

    public void setBitmap(Bitmap bitmap) {
        if (bitmap != null) {
            if (bitmap.isRecycled()) {
                throw new IllegalStateException("Cannot draw into a recycled bitmap");
            }
            // As on Android: drawing would change pixels the bitmap promises
            // are fixed, and that a decoded or copied bitmap may share.
            if (!bitmap.isMutable()) {
                throw new IllegalStateException("Immutable bitmap passed to Canvas");
            }
            Image img = bitmap.getImage();
            bind(img.getGraphics(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
            bitmapTarget = true;
            target = bitmap;
            targetImage = img;
        } else {
            bind(null, 0, 0, 0, 0);
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getDensity() {
        return Bitmap.DENSITY_NONE;
    }

    public void setDensity(int density) {
    }

    public boolean isHardwareAccelerated() {
        return false;
    }

    public boolean isOpaque() {
        return false;
    }

    // ------------------------------------------------------------ state

    public int save() {
        float[] m = new float[9];
        matrix.getValues(m);
        // The clip is saved in the same record as the matrix rather than with
        // Graphics.pushClip, so restoreToCount unwinds both together and a
        // save the application never restores cannot unbalance the port's
        // own clip stack.
        float[] state = new float[14];
        System.arraycopy(m, 0, state, 0, 9);
        state[9] = alphaLayer;
        state[10] = g.getClipX();
        state[11] = g.getClipY();
        state[12] = g.getClipWidth();
        state[13] = g.getClipHeight();
        stack.add(state);
        // A clipPath clip is restored as its shape, not its bounds.
        clipShapes.add(currentClipShape());
        return stack.size();
    }

    public int save(int saveFlags) {
        return save();
    }

    /// As [#saveLayerAlpha(RectF, int)] with the paint's alpha; the paint's
    /// other properties (transfer mode, color filter) are not applied.
    public int saveLayer(RectF bounds, Paint paint) {
        return saveLayerAlpha(bounds, paint == null ? 255 : paint.getAlpha());
    }

    public int saveLayer(float left, float top, float right, float bottom, Paint paint) {
        return saveLayer(new RectF(left, top, right, bottom), paint);
    }

    /// Saves the state and, when `alpha` is translucent, redirects drawing
    /// into an offscreen layer the size of the clip; the matching restore
    /// draws that layer back once at `alpha`, so overlapping draws inside it
    /// do not darken each other, as on Android. The layer is clipped to
    /// `bounds`. Where the context already carries a transform (a rotated or
    /// scaled parent view group) the layer is not allocated and `alpha` is
    /// applied to every draw instead, which differs only where draws overlap.
    public int saveLayerAlpha(RectF bounds, int alpha) {
        int c = save();
        if (bounds != null) {
            clipRect(bounds);
        }
        int a = Math.max(0, Math.min(255, alpha));
        if (a < 255 && !beginLayer(a, c)) {
            alphaLayer = alphaLayer * a / 255;
        }
        return c;
    }

    public int saveLayerAlpha(float left, float top, float right, float bottom, int alpha) {
        return saveLayerAlpha(new RectF(left, top, right, bottom), alpha);
    }

    /// An offscreen layer: what drawing into it replaced, restored with the
    /// save record at [#depth].
    private static final class Layer {
        int depth;
        int alpha;
        int x;
        int y;
        Image image;
        Graphics outer;
        float outerOriginX;
        float outerOriginY;
        Bitmap outerTarget;
        Image outerTargetImage;
    }

    private final ArrayList<Layer> layers = new ArrayList<Layer>();

    /// Points `g` at a transparent image covering the current clip; false
    /// (nothing changed) where the context has a transform the layer could
    /// not reproduce, or the clip is empty.
    private boolean beginLayer(int alpha, int depth) {
        if (g.isTransformSupported()) {
            Transform t = Transform.makeIdentity();
            g.getTransform(t);
            if (!t.isIdentity()) {
                return false;
            }
        }
        int cx = g.getClipX();
        int cy = g.getClipY();
        int cw = g.getClipWidth();
        int ch = g.getClipHeight();
        if (cw <= 0 || ch <= 0) {
            return false;
        }
        Layer l = new Layer();
        l.depth = depth;
        l.alpha = alpha;
        l.x = cx;
        l.y = cy;
        l.image = Image.createImage(cw, ch, 0);
        l.outer = g;
        l.outerOriginX = originX;
        l.outerOriginY = originY;
        l.outerTarget = target;
        l.outerTargetImage = targetImage;
        layers.add(l);
        g = l.image.getGraphics();
        originX -= cx;
        originY -= cy;
        // The layer's own alpha and the outer clip shape both apply when it
        // is drawn back; inside it, draws are opaque and clipped to it.
        alphaLayer = 255;
        clipShape = null;
        target = null;
        targetImage = null;
        return true;
    }

    public void restore() {
        if (stack.isEmpty()) {
            throw new IllegalStateException("Underflow in restore - more restores than saves");
        }
        Layer layer = null;
        if (!layers.isEmpty() && layers.get(layers.size() - 1).depth == stack.size()) {
            layer = layers.remove(layers.size() - 1);
            g = layer.outer;
            originX = layer.outerOriginX;
            originY = layer.outerOriginY;
            target = layer.outerTarget;
            targetImage = layer.outerTargetImage;
        }
        float[] state = stack.remove(stack.size() - 1);
        float[] m = new float[9];
        System.arraycopy(state, 0, m, 0, 9);
        matrix.setValues(m);
        alphaLayer = (int) state[9];
        GeneralPath shape = clipShapes.remove(clipShapes.size() - 1);
        if (shape != null) {
            g.setClip(shape);
            rememberClipShape(shape);
        } else {
            g.setClip((int) state[10], (int) state[11], (int) state[12], (int) state[13]);
            clipShape = null;
        }
        if (layer != null) {
            drawing();
            int old = g.getAlpha();
            g.setAlpha(layer.alpha * alphaLayer / 255 * old / 255);
            g.drawImage(layer.image, layer.x, layer.y);
            g.setAlpha(old);
        }
    }

    /// The shape clip in effect, or null when the clip is a rectangle (or
    /// was changed since `clipPath` set it).
    private GeneralPath currentClipShape() {
        if (clipShape != null && g.getClipX() == clipShapeBounds[0] && g.getClipY() == clipShapeBounds[1]
                && g.getClipWidth() == clipShapeBounds[2] && g.getClipHeight() == clipShapeBounds[3]) {
            return clipShape;
        }
        return null;
    }

    private void rememberClipShape(GeneralPath shape) {
        clipShape = shape;
        clipShapeBounds[0] = g.getClipX();
        clipShapeBounds[1] = g.getClipY();
        clipShapeBounds[2] = g.getClipWidth();
        clipShapeBounds[3] = g.getClipHeight();
    }

    public int getSaveCount() {
        return stack.size() + 1;
    }

    public void restoreToCount(int saveCount) {
        while (stack.size() >= saveCount && !stack.isEmpty()) {
            restore();
        }
    }

    public void translate(float dx, float dy) {
        matrix.preTranslate(dx, dy);
    }

    public void scale(float sx, float sy) {
        matrix.preScale(sx, sy);
    }

    public final void scale(float sx, float sy, float px, float py) {
        matrix.preScale(sx, sy, px, py);
    }

    public void rotate(float degrees) {
        matrix.preRotate(degrees);
    }

    public final void rotate(float degrees, float px, float py) {
        matrix.preRotate(degrees, px, py);
    }

    public void skew(float sx, float sy) {
        Matrix s = new Matrix();
        s.setSkew(sx, sy);
        matrix.preConcat(s);
    }

    public void concat(Matrix m) {
        if (m != null) {
            matrix.preConcat(m);
        }
    }

    public void setMatrix(Matrix m) {
        matrix.set(m);
    }

    public void getMatrix(Matrix out) {
        out.set(matrix);
    }

    public final Matrix getMatrix() {
        return new Matrix(matrix);
    }

    // ------------------------------------------------------------ clip

    /// Narrows the clip to the rectangle. Under a rotation or skew the
    /// rectangle maps to a quadrilateral, which is clipped as a shape so
    /// nothing paints in the corners of its bounding box; a port without
    /// shape clipping clips to that bounding box instead.
    public boolean clipRect(float left, float top, float right, float bottom) {
        if (rotated() && g.isShapeClipSupported()) {
            Path p = new Path();
            p.addRect(Math.min(left, right), Math.min(top, bottom), Math.max(left, right),
                    Math.max(top, bottom), Path.Direction.CW);
            return clipPath(p);
        }
        RectF r = new RectF(left, top, right, bottom);
        matrix.mapRect(r);
        int x = (int) Math.floor(originX + r.left);
        int y = (int) Math.floor(originY + r.top);
        g.clipRect(x, y, (int) Math.ceil(originX + r.right) - x, (int) Math.ceil(originY + r.bottom) - y);
        clipShape = null;
        return g.getClipWidth() > 0 && g.getClipHeight() > 0;
    }

    public boolean clipRect(Rect rect) {
        return clipRect(rect.left, rect.top, rect.right, rect.bottom);
    }

    public boolean clipRect(RectF rect) {
        return clipRect(rect.left, rect.top, rect.right, rect.bottom);
    }

    public boolean clipRect(int left, int top, int right, int bottom) {
        return clipRect((float) left, (float) top, (float) right, (float) bottom);
    }

    public boolean clipOutRect(float left, float top, float right, float bottom) {
        // Codename One clips are rectangles or shapes, never "everything but".
        return true;
    }

    /// Narrows the clip to `path`. `Graphics.setClip(Shape)` replaces the
    /// clip, so the path is first intersected with the current clip whenever
    /// it reaches outside it; a custom view therefore cannot paint past the
    /// bounds its parent clipped it to. The current clip is read as its
    /// bounding rectangle, so a second clipPath intersects with the first
    /// path's bounds rather than its exact outline. Ports without shape
    /// clipping clip to the path's bounds.
    public boolean clipPath(Path path) {
        if (g.isShapeClipSupported()) {
            Path p = new Path(path);
            p.transform(matrix);
            GeneralPath shape = p.toGeneralPath(originX, originY);
            int cx = g.getClipX();
            int cy = g.getClipY();
            int cw = g.getClipWidth();
            int ch = g.getClipHeight();
            Rectangle b = shape.getBounds();
            boolean inside = b.getX() >= cx && b.getY() >= cy
                    && b.getX() + b.getWidth() <= cx + cw && b.getY() + b.getHeight() <= cy + ch;
            if (!inside && !shape.intersect(cx, cy, cw, ch)) {
                g.setClip(cx, cy, 0, 0);
                return false;
            }
            g.setClip(shape);
            rememberClipShape(shape);
            return true;
        }
        RectF b = new RectF();
        path.computeBounds(b, true);
        return clipRect(b);
    }

    public boolean getClipBounds(Rect bounds) {
        Matrix inv = new Matrix();
        RectF r = new RectF(g.getClipX() - originX, g.getClipY() - originY,
                g.getClipX() + g.getClipWidth() - originX, g.getClipY() + g.getClipHeight() - originY);
        if (matrix.invert(inv)) {
            inv.mapRect(r);
        }
        r.roundOut(bounds);
        return !bounds.isEmpty();
    }

    public final Rect getClipBounds() {
        Rect r = new Rect();
        getClipBounds(r);
        return r;
    }

    public boolean quickReject(float left, float top, float right, float bottom) {
        Rect clip = new Rect();
        getClipBounds(clip);
        return !clip.intersects((int) left, (int) top, (int) Math.ceil(right), (int) Math.ceil(bottom));
    }

    public boolean quickReject(RectF rect) {
        return quickReject(rect.left, rect.top, rect.right, rect.bottom);
    }

    // ------------------------------------------------------------ paint setup

    private boolean rotated() {
        return !matrix.isScaleTranslate();
    }

    /// When the matrix rotates or skews, installs it on the context and
    /// returns the transform to restore; geometry is then drawn untransformed.
    private Transform pushRotation() {
        if (!rotated() || !g.isTransformSupported()) {
            return null;
        }
        Transform saved = Transform.makeIdentity();
        g.getTransform(saved);
        float[] v = new float[9];
        matrix.getValues(v);
        Transform t = saved.copy();
        t.translate(originX, originY);
        Transform m = Transform.makeAffine(v[0], v[3], v[1], v[4], v[2], v[5]);
        t.concatenate(m);
        g.setTransform(t);
        return saved;
    }

    private void popRotation(Transform saved) {
        if (saved != null) {
            g.setTransform(saved);
        }
    }

    private float ax(float x, float y) {
        float[] v = values();
        return originX + v[0] * x + v[1] * y + v[2];
    }

    private float ay(float x, float y) {
        float[] v = values();
        return originY + v[3] * x + v[4] * y + v[5];
    }

    private final float[] tmp = new float[9];

    private float[] values() {
        matrix.getValues(tmp);
        return tmp;
    }

    /// Every draw goes through here (most through [#apply]): drawing
    /// changes a target bitmap's pixels, so its cached copy is dropped.
    private void drawing() {
        if (target == null) {
            return;
        }
        // setPixel writes since the last draw replace the bitmap's image when
        // it is next asked for; draw into that image, clip carried over, or
        // this drawing would land in the discarded one.
        Image img = target.getImage();
        if (img != targetImage) {
            GeneralPath shape = currentClipShape();
            Graphics ng = img.getGraphics();
            if (shape != null) {
                ng.setClip(shape);
            } else {
                ng.setClip(g.getClipX(), g.getClipY(), g.getClipWidth(), g.getClipHeight());
            }
            g = ng;
            targetImage = img;
            if (shape != null) {
                rememberClipShape(shape);
            }
        }
        target.contentChanged();
    }

    private int apply(Paint paint) {
        drawing();
        int color = paint == null ? 0xff000000 : paint.getColor();
        ColorFilter cf = paint == null ? null : paint.getColorFilter();
        if (cf instanceof PorterDuffColorFilter) {
            PorterDuffColorFilter p = (PorterDuffColorFilter) cf;
            PorterDuff.Mode mode = p.getMode();
            if (mode == PorterDuff.Mode.SRC_IN || mode == PorterDuff.Mode.SRC_ATOP || mode == PorterDuff.Mode.SRC) {
                color = (p.getColor() & 0xffffff) | ((((p.getColor() >>> 24) * (color >>> 24)) / 255) << 24);
            }
        }
        int oldAlpha = g.getAlpha();
        g.setColor(color & 0xffffff);
        g.setAlpha(((color >>> 24) * alphaLayer / 255) * oldAlpha / 255);
        if (paint != null) {
            g.setAntiAliased(paint.isAntiAlias());
        }
        return oldAlpha;
    }

    private void unapply(int oldAlpha) {
        g.setAlpha(oldAlpha);
    }

    private Stroke stroke(Paint p, float scale) {
        int cap = p.getStrokeCap() == Paint.Cap.ROUND ? Stroke.CAP_ROUND
                : p.getStrokeCap() == Paint.Cap.SQUARE ? Stroke.CAP_SQUARE : Stroke.CAP_BUTT;
        int join = p.getStrokeJoin() == Paint.Join.ROUND ? Stroke.JOIN_ROUND
                : p.getStrokeJoin() == Paint.Join.BEVEL ? Stroke.JOIN_BEVEL : Stroke.JOIN_MITER;
        return new Stroke(Math.max(1f, p.getStrokeWidth() * scale), cap, join, p.getStrokeMiter());
    }

    private float scaleFactor() {
        float[] v = values();
        return (float) Math.sqrt(Math.abs(v[0] * v[4] - v[1] * v[3]));
    }

    private boolean fills(Paint p) {
        return p == null || p.getStyle() != Paint.Style.STROKE;
    }

    private boolean strokes(Paint p) {
        return p != null && p.getStyle() != Paint.Style.FILL;
    }

    // ------------------------------------------------------------ primitives

    public void drawColor(int color) {
        drawing();
        int old = g.getAlpha();
        g.setColor(color & 0xffffff);
        g.setAlpha((color >>> 24) * alphaLayer / 255 * old / 255);
        g.fillRect(g.getClipX(), g.getClipY(), g.getClipWidth(), g.getClipHeight());
        g.setAlpha(old);
    }

    public void drawColor(int color, PorterDuff.Mode mode) {
        if (mode == PorterDuff.Mode.CLEAR) {
            // Into a bitmap, CLEAR erases the clip to transparent: what an
            // eraser or a reused drawing buffer relies on. Graphics.clearRect
            // does that on the JavaSE, iOS/Mac and JavaScript ports; the
            // others do not implement it and the pixels stay, as before.
            // On a view it stays a no-op: Codename One paints the parent
            // first, so clearing would punch a hole through the parent's
            // background (Android shows black there), never what the
            // "clear before redrawing" idiom wants.
            if (bitmapTarget) {
                drawing();
                g.clearRect(g.getClipX(), g.getClipY(), g.getClipWidth(), g.getClipHeight());
            }
            return;
        }
        drawColor(color);
    }

    public void drawARGB(int a, int r, int gr, int b) {
        drawColor((a << 24) | (r << 16) | (gr << 8) | b);
    }

    public void drawRGB(int r, int gr, int b) {
        drawColor(0xff000000 | (r << 16) | (gr << 8) | b);
    }

    /// Fills the clip with `paint`. A shader-backed paint fills through
    /// `drawRect` over the clip bounds, so it gets the same gradient support
    /// a rectangle does; Android fills regardless of the paint's style.
    public void drawPaint(Paint paint) {
        if (paint.getShader() == null) {
            drawColor(paint.getColor());
            return;
        }
        Rect clip = new Rect();
        if (!getClipBounds(clip)) {
            return;
        }
        Paint fill = new Paint(paint);
        fill.setStyle(Paint.Style.FILL);
        drawRect(clip, fill);
    }

    public void drawRect(float left, float top, float right, float bottom, Paint paint) {
        if (rotated()) {
            Path p = new Path();
            p.addRect(left, top, right, bottom, Path.Direction.CW);
            drawPath(p, paint);
            return;
        }
        Shader sh = paint == null ? null : paint.getShader();
        float x0 = ax(left, top);
        float y0 = ay(left, top);
        float x1 = ax(right, bottom);
        float y1 = ay(right, bottom);
        int x = Math.round(Math.min(x0, x1));
        int y = Math.round(Math.min(y0, y1));
        int w = Math.round(Math.max(x0, x1)) - x;
        int h = Math.round(Math.max(y0, y1)) - y;
        int old = apply(paint);
        if (fills(paint)) {
            if (sh instanceof LinearGradient && ((LinearGradient) sh).getColors().length >= 2) {
                fillLinearGradient((LinearGradient) sh, x, y, w, h);
            } else {
                g.fillRect(x, y, w, h);
            }
        }
        if (strokes(paint)) {
            float sw = paint.getStrokeWidth() * scaleFactor();
            if (sw <= 1) {
                g.drawRect(x, y, w - 1, h - 1);
            } else {
                GeneralPath gp = new GeneralPath();
                gp.moveTo(x, y);
                gp.lineTo(x + w, y);
                gp.lineTo(x + w, y + h);
                gp.lineTo(x, y + h);
                gp.closePath();
                g.drawShape(gp, stroke(paint, scaleFactor()));
            }
        }
        unapply(old);
    }

    /// Fills a device rectangle with a linear gradient running the way its
    /// endpoints say: an axis-aligned one through the port's two-color fill
    /// (its colors swapped when the endpoints run right to left or bottom to
    /// top), a diagonal one through the port's angled gradient. The paint's
    /// alpha is already on the context, so the stops are drawn opaque.
    private void fillLinearGradient(LinearGradient lg, int x, int y, int w, int h) {
        int[] colors = lg.getColors();
        float dx = ax(lg.x1, lg.y1) - ax(lg.x0, lg.y0);
        float dy = ay(lg.x1, lg.y1) - ay(lg.x0, lg.y0);
        int first = colors[0] & 0xffffff;
        int last = colors[colors.length - 1] & 0xffffff;
        if (dx == 0 || dy == 0) {
            boolean horizontal = dy == 0 && dx != 0;
            boolean reversed = horizontal ? dx < 0 : dy < 0;
            g.fillLinearGradient(reversed ? last : first, reversed ? first : last, x, y, w, h, horizontal);
            return;
        }
        int[] stops = new int[colors.length];
        float[] positions = new float[colors.length];
        for (int i = 0; i < colors.length; i++) {
            stops[i] = colors[i] | 0xff000000;
            positions[i] = i / (float) (colors.length - 1);
        }
        // CSS angles: 0 points up and 90 right, in screen coordinates.
        float angle = (float) Math.toDegrees(com.codename1.util.MathUtil.atan2(dx, -dy));
        g.fillGradient(new com.codename1.ui.LinearGradient(angle, stops, positions), x, y, w, h);
    }

    public void drawRect(RectF rect, Paint paint) {
        drawRect(rect.left, rect.top, rect.right, rect.bottom, paint);
    }

    public void drawRect(Rect r, Paint paint) {
        drawRect(r.left, r.top, r.right, r.bottom, paint);
    }

    public void drawRoundRect(float left, float top, float right, float bottom, float rx, float ry, Paint paint) {
        if (rx <= 0 && ry <= 0) {
            drawRect(left, top, right, bottom, paint);
            return;
        }
        Path p = new Path();
        p.addRoundRect(left, top, right, bottom, rx, ry, Path.Direction.CW);
        drawPath(p, paint);
    }

    public void drawRoundRect(RectF rect, float rx, float ry, Paint paint) {
        drawRoundRect(rect.left, rect.top, rect.right, rect.bottom, rx, ry, paint);
    }

    public void drawOval(float left, float top, float right, float bottom, Paint paint) {
        Path p = new Path();
        p.addOval(left, top, right, bottom, Path.Direction.CW);
        drawPath(p, paint);
    }

    public void drawOval(RectF oval, Paint paint) {
        drawOval(oval.left, oval.top, oval.right, oval.bottom, paint);
    }

    public void drawCircle(float cx, float cy, float radius, Paint paint) {
        drawOval(cx - radius, cy - radius, cx + radius, cy + radius, paint);
    }

    public void drawArc(RectF oval, float startAngle, float sweepAngle, boolean useCenter, Paint paint) {
        drawArc(oval.left, oval.top, oval.right, oval.bottom, startAngle, sweepAngle, useCenter, paint);
    }

    public void drawArc(float left, float top, float right, float bottom, float startAngle, float sweepAngle,
                        boolean useCenter, Paint paint) {
        Path p = new Path();
        if (useCenter) {
            p.moveTo((left + right) / 2, (top + bottom) / 2);
            p.arcTo(left, top, right, bottom, startAngle, sweepAngle, false);
            p.close();
        } else {
            p.arcTo(left, top, right, bottom, startAngle, sweepAngle, true);
        }
        drawPath(p, paint);
    }

    public void drawLine(float startX, float startY, float stopX, float stopY, Paint paint) {
        Transform saved = pushRotation();
        try {
            float x0 = saved != null ? startX : ax(startX, startY);
            float y0 = saved != null ? startY : ay(startX, startY);
            float x1 = saved != null ? stopX : ax(stopX, stopY);
            float y1 = saved != null ? stopY : ay(stopX, stopY);
            int old = apply(paint);
            float sw = paint.getStrokeWidth() * (saved != null ? 1 : scaleFactor());
            if (sw <= 1) {
                g.drawLine(Math.round(x0), Math.round(y0), Math.round(x1), Math.round(y1));
            } else {
                GeneralPath gp = new GeneralPath();
                gp.moveTo(x0, y0);
                gp.lineTo(x1, y1);
                g.drawShape(gp, stroke(paint, saved != null ? 1 : scaleFactor()));
            }
            unapply(old);
        } finally {
            popRotation(saved);
        }
    }

    public void drawLines(float[] pts, Paint paint) {
        drawLines(pts, 0, pts.length, paint);
    }

    public void drawLines(float[] pts, int offset, int count, Paint paint) {
        for (int i = offset; i + 3 < offset + count; i += 4) {
            drawLine(pts[i], pts[i + 1], pts[i + 2], pts[i + 3], paint);
        }
    }

    public void drawPoint(float x, float y, Paint paint) {
        float r = Math.max(0.5f, paint.getStrokeWidth() / 2);
        if (paint.getStrokeCap() == Paint.Cap.ROUND) {
            Paint p = new Paint(paint);
            p.setStyle(Paint.Style.FILL);
            drawCircle(x, y, r, p);
        } else {
            Paint p = new Paint(paint);
            p.setStyle(Paint.Style.FILL);
            drawRect(x - r, y - r, x + r, y + r, p);
        }
    }

    public void drawPoints(float[] pts, Paint paint) {
        for (int i = 0; i + 1 < pts.length; i += 2) {
            drawPoint(pts[i], pts[i + 1], paint);
        }
    }

    public void drawPath(Path path, Paint paint) {
        Transform saved = pushRotation();
        try {
            Path p = path;
            if (saved == null && !matrix.isIdentity()) {
                p = new Path(path);
                p.transform(matrix);
            }
            float dx = saved != null ? 0 : originX;
            float dy = saved != null ? 0 : originY;
            GeneralPath gp = p.toGeneralPath(dx, dy);
            int old = apply(paint);
            if (fills(paint)) {
                g.fillShape(gp);
            }
            if (strokes(paint)) {
                g.drawShape(gp, stroke(paint, saved != null ? 1 : scaleFactor()));
            }
            unapply(old);
        } finally {
            popRotation(saved);
        }
    }

    // ------------------------------------------------------------ text

    public void drawText(String text, float x, float y, Paint paint) {
        drawText(text, 0, text.length(), x, y, paint);
    }

    public void drawText(CharSequence text, int start, int end, float x, float y, Paint paint) {
        drawText(text.toString(), start, end, x, y, paint);
    }

    public void drawText(char[] text, int index, int count, float x, float y, Paint paint) {
        drawText(new String(text, index, count), x, y, paint);
    }

    public void drawText(String text, int start, int end, float x, float y, Paint paint) {
        if (start >= end) {
            return;
        }
        String s = start == 0 && end == text.length() ? text : text.substring(start, end);
        Transform saved = pushRotation();
        try {
            float scale = saved != null ? 1 : Math.abs(values()[4]);
            Paint p = paint;
            if (scale != 1) {
                p = new Paint(paint);
                p.setTextSize(paint.getTextSize() * scale);
            }
            Font f = p.cn1Font();
            float w = p.measureText(s);
            float ox = x;
            if (paint.getTextAlign() == Paint.Align.CENTER) {
                ox -= w / (2 * (scale == 0 ? 1 : scale));
            } else if (paint.getTextAlign() == Paint.Align.RIGHT) {
                ox -= w / (scale == 0 ? 1 : scale);
            }
            float px = saved != null ? ox : ax(ox, y);
            float py = saved != null ? y : ay(ox, y);
            int tx = Math.round(px);
            int top = Math.round(py) - f.getAscent();
            int old = apply(paint);
            g.setFont(f);
            // textScaleX is part of measureText, so the drawn run must cover
            // the same width or aligned text lands off its measured box. The
            // glyphs are stretched about the run's start where the graphics
            // can transform; elsewhere they keep their shape and only their
            // advances are scaled, which still matches the measured width.
            float sx = paint.getTextScaleX();
            boolean scaledX = sx != 1 && sx > 0;
            Transform beforeStretch = null;
            if (scaledX && g.isTransformSupported()) {
                beforeStretch = Transform.makeIdentity();
                g.getTransform(beforeStretch);
                Transform t = beforeStretch.copy();
                t.translate(px, 0);
                t.scale(sx, 1);
                t.translate(-px, 0);
                g.setTransform(t);
            }
            float advance = scaledX && beforeStretch == null ? sx : 1;
            try {
                if (paint.getLetterSpacing() != 0 || advance != 1) {
                    float cx = px;
                    float extra = paint.getLetterSpacing() * p.getTextSize();
                    int n = s.length();
                    for (int i = 0; i < n;) {
                        // A surrogate pair is drawn and advanced as one
                        // character, matching Paint.measureText.
                        int len = Paint.charLength(s, i, n);
                        if (len == 2) {
                            g.drawString(s.substring(i, i + 2), Math.round(cx), top);
                        } else {
                            g.drawChar(s.charAt(i), Math.round(cx), top);
                        }
                        cx += (Paint.charAdvance(f, s, i, len) + extra) * advance;
                        i += len;
                    }
                } else {
                    g.drawString(s, tx, top);
                }
            } finally {
                if (beforeStretch != null) {
                    g.setTransform(beforeStretch);
                }
            }
            if (paint.isUnderlineText()) {
                int uy = Math.round(py) + Math.max(1, f.getDescent() / 3);
                g.fillRect(tx, uy, Math.round(w), Math.max(1, Math.round(p.getTextSize() / 18f)));
            }
            if (paint.isStrikeThruText()) {
                int sy = Math.round(py) - f.getAscent() / 3;
                g.fillRect(tx, sy, Math.round(w), Math.max(1, Math.round(p.getTextSize() / 18f)));
            }
            unapply(old);
        } finally {
            popRotation(saved);
        }
    }

    // ------------------------------------------------------------ bitmaps

    public void drawBitmap(Bitmap bitmap, float left, float top, Paint paint) {
        drawBitmap(bitmap, null, new RectF(left, top, left + bitmap.getWidth(), top + bitmap.getHeight()), paint);
    }

    public void drawBitmap(Bitmap bitmap, Rect src, Rect dst, Paint paint) {
        drawBitmap(bitmap, src, new RectF(dst), paint);
    }

    public void drawBitmap(Bitmap bitmap, Rect src, RectF dst, Paint paint) {
        throwIfCannotDraw(bitmap);
        Image img = bitmap.getImage();
        if (src != null && !(src.left == 0 && src.top == 0 && src.right == bitmap.getWidth()
                && src.bottom == bitmap.getHeight())) {
            img = img.subImage(src.left, src.top, src.width(), src.height(), true);
        }
        drawImage(img, dst, paint);
    }

    public void drawBitmap(Bitmap bitmap, Matrix m, Paint paint) {
        throwIfCannotDraw(bitmap);
        Matrix save = new Matrix(matrix);
        matrix.preConcat(m);
        drawBitmap(bitmap, 0, 0, paint);
        matrix.set(save);
    }

    /// Refuses a recycled bitmap the way AOSP's `BaseCanvas.throwIfCannotDraw`
    /// does. `Bitmap.recycle()` keeps the image (a canvas may share it), so
    /// without this every `drawBitmap` overload kept painting stale pixels.
    /// Every overload funnels into the `Rect`/`RectF` one; the `Matrix` one
    /// checks first too, so it cannot leave its concatenated matrix behind.
    private static void throwIfCannotDraw(Bitmap bitmap) {
        if (bitmap.isRecycled()) {
            throw new RuntimeException("Canvas: trying to use a recycled bitmap " + bitmap);
        }
    }

    /// Draws a Codename One image into `dst` (local coordinates).
    public void drawImage(Image img, RectF dst, Paint paint) {
        Transform saved = pushRotation();
        try {
            float l;
            float t;
            float r;
            float b;
            if (saved != null) {
                l = dst.left;
                t = dst.top;
                r = dst.right;
                b = dst.bottom;
            } else {
                l = ax(dst.left, dst.top);
                t = ay(dst.left, dst.top);
                r = ax(dst.right, dst.bottom);
                b = ay(dst.right, dst.bottom);
            }
            int x = Math.round(Math.min(l, r));
            int y = Math.round(Math.min(t, b));
            int w = Math.round(Math.max(l, r)) - x;
            int h = Math.round(Math.max(t, b)) - y;
            if (w <= 0 || h <= 0) {
                return;
            }
            drawing();
            int old = g.getAlpha();
            int a = paint == null ? 255 : paint.getAlpha();
            g.setAlpha(a * alphaLayer / 255 * old / 255);
            Image toDraw = img;
            ColorFilter cf = paint == null ? null : paint.getColorFilter();
            if (cf instanceof PorterDuffColorFilter) {
                toDraw = com.codename1.androidcompat.runtime.ImageTint.tint(img, (PorterDuffColorFilter) cf);
            }
            if (w == toDraw.getWidth() && h == toDraw.getHeight()) {
                g.drawImage(toDraw, x, y);
            } else {
                g.drawImage(toDraw, x, y, w, h);
            }
            g.setAlpha(old);
        } finally {
            popRotation(saved);
        }
    }

    public void drawBitmap(int[] colors, int offset, int stride, float x, float y, int width, int height,
                           boolean hasAlpha, Paint paint) {
        drawBitmap(Bitmap.createBitmap(colors, offset, stride, width, height, Bitmap.Config.ARGB_8888), x, y, paint);
    }
}
