/*
 * MIT License
 *
 * Copyright (c) 2020 Rive
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Java rewrite of rive-runtime for Codename One.
 * Codename One modifications Copyright (c) 2026 Codename One.
 */
package com.codename1.impl.rive;

import com.codename1.ui.Graphics;
import com.codename1.ui.RadialGradient;
import com.codename1.ui.geom.GeneralPath;

import java.util.ArrayList;

/// Draws an artboard with `com.codename1.ui.Graphics`.
///
/// Points are mapped through the current matrix here, on the CPU, rather than
/// handed to `Graphics.setTransform`: that keeps the output identical on ports
/// without transform support, and lets the graphics translation apply exactly
/// as it does to any other shape.
///
/// Clipping: `Graphics` clips to one shape at a time and cannot intersect two,
/// so a clip path inside another clip path is intersected with the outer
/// clip's bounds only. The GPU renderer has no such limit.
public final class GraphicsRiveRenderer implements RiveRenderer {
    private Graphics g;
    private final Mat2D matrix = new Mat2D();
    private final ArrayList<float[]> matrixStack = new ArrayList<float[]>();
    private final ArrayList<int[]> clipStack = new ArrayList<int[]>();
    private final ArrayList<GeneralPath> clipShapes = new ArrayList<GeneralPath>();
    private GeneralPath activeClip;
    private final GeneralPath path = new GeneralPath();
    private final float[] bounds = new float[4];
    private final float[] clipBounds = new float[4];
    private int depth;
    private final com.codename1.ui.Stroke stroke = new com.codename1.ui.Stroke(1f, 0, 0, 4f);
    private final ArrayList<GeneralPath> clipPool = new ArrayList<GeneralPath>();
    private int clipPoolUsed;
    private int savedColor;
    private int savedAlpha;
    private boolean savedAntiAlias;
    private int savedClipX;
    private int savedClipY;
    private int savedClipW;
    private int savedClipH;

    /// Starts a frame on `graphics`, with `base` mapping artboard space to the
    /// graphics' coordinates.
    public void begin(Graphics graphics, Mat2D base) {
        g = graphics;
        matrix.set(base);
        depth = 0;
        clipPoolUsed = 0;
        activeClip = null;
        savedColor = g.getColor();
        savedAlpha = g.getAlpha();
        savedAntiAlias = g.isAntiAliased();
        savedClipX = g.getClipX();
        savedClipY = g.getClipY();
        savedClipW = g.getClipWidth();
        savedClipH = g.getClipHeight();
        g.setAntiAliased(true);
    }

    /// Moves a frame in progress onto `graphics`, carrying over the color,
    /// alpha, anti-aliasing and clip of the current graphics. The state saved
    /// by [#begin(Graphics, Mat2D)] is restored onto `graphics` at
    /// [#end()]. Does nothing outside a frame.
    void retarget(Graphics graphics) {
        if (g == null) {
            return;
        }
        int cx = g.getClipX();
        int cy = g.getClipY();
        int cw = g.getClipWidth();
        int ch = g.getClipHeight();
        graphics.setColor(g.getColor());
        graphics.setAlpha(g.getAlpha());
        graphics.setAntiAliased(g.isAntiAliased());
        g = graphics;
        restoreClip(cx, cy, cw, ch);
    }

    /// Ends the frame, giving the graphics back the color, alpha,
    /// anti-aliasing and clip it had at [#begin(Graphics, Mat2D)].
    public void end() {
        g.setColor(savedColor);
        g.setAlpha(savedAlpha);
        g.setAntiAliased(savedAntiAlias);
        g.setClip(savedClipX, savedClipY, savedClipW, savedClipH);
        g = null;
    }

    @Override
    public void save() {
        float[] m;
        int[] c;
        if (depth < matrixStack.size()) {
            m = matrixStack.get(depth);
            c = clipStack.get(depth);
        } else {
            m = new float[6];
            c = new int[4];
            matrixStack.add(m);
            clipStack.add(c);
            clipShapes.add(null);
        }
        System.arraycopy(matrix.v, 0, m, 0, 6);
        c[0] = g.getClipX();
        c[1] = g.getClipY();
        c[2] = g.getClipWidth();
        c[3] = g.getClipHeight();
        clipShapes.set(depth, activeClip);
        depth++;
    }

    @Override
    public void restore() {
        if (depth == 0) {
            return;
        }
        depth--;
        System.arraycopy(matrixStack.get(depth), 0, matrix.v, 0, 6);
        int[] c = clipStack.get(depth);
        GeneralPath prev = clipShapes.get(depth);
        if (prev != activeClip) { // NOPMD CompareObjectsWithEquals - identity is the intent
            if (prev != null) {
                shapeClip(prev, c[0], c[1], c[2], c[3]);
            } else {
                g.setClip(c[0], c[1], c[2], c[3]);
            }
            activeClip = prev;
        }
    }

    @Override
    public void transform(Mat2D m) {
        matrix.multiply(matrix, m);
    }

    private void toShape(RivePath p, GeneralPath out) {
        out.reset();
        out.setWindingRule(p.fillRule == RivePath.FILL_EVEN_ODD ? GeneralPath.WIND_EVEN_ODD : GeneralPath.WIND_NON_ZERO);
        float[] v = matrix.v;
        float[] pts = p.points;
        int k = 0;
        for (int i = 0; i < p.verbCount; i++) {
            switch (p.verbs[i]) {
                case RivePath.MOVE:
                    out.moveTo(v[0] * pts[k] + v[2] * pts[k + 1] + v[4], v[1] * pts[k] + v[3] * pts[k + 1] + v[5]);
                    k += 2;
                    break;
                case RivePath.LINE:
                    out.lineTo(v[0] * pts[k] + v[2] * pts[k + 1] + v[4], v[1] * pts[k] + v[3] * pts[k + 1] + v[5]);
                    k += 2;
                    break;
                case RivePath.CUBIC:
                    out.curveTo(v[0] * pts[k] + v[2] * pts[k + 1] + v[4], v[1] * pts[k] + v[3] * pts[k + 1] + v[5],
                            v[0] * pts[k + 2] + v[2] * pts[k + 3] + v[4], v[1] * pts[k + 2] + v[3] * pts[k + 3] + v[5],
                            v[0] * pts[k + 4] + v[2] * pts[k + 5] + v[4], v[1] * pts[k + 4] + v[3] * pts[k + 5] + v[5]);
                    k += 6;
                    break;
                default:
                    out.closePath();
                    break;
            }
        }
    }

    @Override
    public void clipPath(RivePath p) {
        GeneralPath clip;
        if (clipPoolUsed < clipPool.size()) {
            clip = clipPool.get(clipPoolUsed);
        } else {
            clip = new GeneralPath();
            clipPool.add(clip);
        }
        clipPoolUsed++;
        toShape(p, clip);
        int cx = g.getClipX();
        int cy = g.getClipY();
        int cw = g.getClipWidth();
        int ch = g.getClipHeight();
        shapeClip(clip, cx, cy, cw, ch);
        activeClip = clip;
    }

    /// Clips to `s` within the rectangle (x, y, w, h). A shape that lies inside
    /// the rectangle needs no intersection at all, which is the common case.
    /// Otherwise the port intersects the two, and the Android port's
    /// `GeneralPath` intersection throws on some curves ("Unexpected shape
    /// segmentation"): thrown inside a paint, that aborted every pass of the
    /// form. The clip then falls back to the rectangle, and false tells the
    /// caller the shape is not clipped.
    private boolean shapeClip(GeneralPath s, int x, int y, int w, int h) {
        s.getBounds2D(clipBounds);
        g.setClip(s);
        if (clipBounds[0] >= x && clipBounds[1] >= y
                && clipBounds[0] + clipBounds[2] <= x + w && clipBounds[1] + clipBounds[3] <= y + h) {
            return true;
        }
        try {
            g.clipRect(x, y, w, h);
            return true;
        } catch (RuntimeException e) {
            g.setClip(x, y, w, h);
            return false;
        }
    }

    private void restoreClip(int cx, int cy, int cw, int ch) {
        if (activeClip != null) {
            shapeClip(activeClip, cx, cy, cw, ch);
        } else {
            g.setClip(cx, cy, cw, ch);
        }
    }

    private static int alpha(int argb) {
        return (argb >>> 24) & 0xff;
    }

    @Override
    public void drawPath(RivePath p, RivePaint paint) {
        if (p.isEmpty()) {
            return;
        }
        toShape(p, path);
        if (paint.stroke) {
            float scale = (float) Math.sqrt(Math.abs(matrix.v[0] * matrix.v[3] - matrix.v[1] * matrix.v[2]));
            int color = paint.gradient == RivePaint.GRADIENT_NONE || paint.stopCount == 0
                    ? paint.color : paint.stopColors[paint.stopCount / 2];
            if (alpha(color) == 0 || paint.thickness <= 0) {
                return;
            }
            g.setColor(color & 0xffffff);
            g.setAlpha(alpha(color));
            stroke.setLineWidth(paint.thickness * scale);
            stroke.setCapStyle(paint.cap);
            stroke.setJoinStyle(paint.join);
            g.drawShape(path, stroke);
            g.setAlpha(255);
            return;
        }
        if (paint.gradient == RivePaint.GRADIENT_NONE || paint.stopCount < 2) {
            int color = paint.stopCount == 1 && paint.gradient != RivePaint.GRADIENT_NONE ? paint.stopColors[0] : paint.color;
            if (alpha(color) == 0) {
                return;
            }
            g.setColor(color & 0xffffff);
            g.setAlpha(alpha(color));
            g.fillShape(path);
            g.setAlpha(255);
            return;
        }
        fillGradient(paint);
    }

    private void fillGradient(RivePaint paint) {
        int n = paint.stopCount;
        int[] colors = new int[n];
        float[] positions = new float[n];
        for (int i = 0; i < n; i++) {
            colors[i] = paint.stopColors[i];
            float pos = paint.stopPositions[i];
            if (i > 0 && pos <= positions[i - 1]) {
                pos = Math.min(1f, positions[i - 1] + 0.0001f);
            }
            positions[i] = pos;
        }
        float[] v = matrix.v;
        float sx = v[0] * paint.startX + v[2] * paint.startY + v[4];
        float sy = v[1] * paint.startX + v[3] * paint.startY + v[5];
        float ex = v[0] * paint.endX + v[2] * paint.endY + v[4];
        float ey = v[1] * paint.endX + v[3] * paint.endY + v[5];
        path.getBounds2D(bounds);
        int cx = g.getClipX();
        int cy = g.getClipY();
        int cw = g.getClipWidth();
        int ch = g.getClipHeight();
        if (!shapeClip(path, cx, cy, cw, ch)) {
            // no shape clip on this port for this path: a solid fill of the
            // middle stop stays inside the shape, where the gradient would
            // cover its whole bounding box
            int mid = colors[n / 2];
            if (alpha(mid) != 0) {
                g.setColor(mid & 0xffffff);
                g.setAlpha(alpha(mid));
                g.fillShape(path);
                g.setAlpha(255);
            }
            restoreClip(cx, cy, cw, ch);
            return;
        }
        int bx = (int) Math.floor(bounds[0]);
        int by = (int) Math.floor(bounds[1]);
        int bw = (int) Math.ceil(bounds[0] + bounds[2]) - bx;
        int bh = (int) Math.ceil(bounds[1] + bounds[3]) - by;
        if (paint.gradient == RivePaint.GRADIENT_LINEAR && bw > 0 && bh > 0) {
            fillLinear(colors, positions, sx, sy, ex, ey, bx, by, bw, bh);
        } else if (bw > 0 && bh > 0) {
            float dx = ex - sx;
            float dy = ey - sy;
            float r = (float) Math.sqrt(dx * dx + dy * dy);
            RadialGradient rg = new RadialGradient(colors, positions);
            rg.setShape(RadialGradient.SHAPE_CIRCLE);
            rg.setExtent(RadialGradient.EXTENT_EXPLICIT);
            rg.setRelativeCenterX((sx - bx) / bw);
            rg.setRelativeCenterY((sy - by) / bh);
            rg.setRelativeRadiusX(r / bw);
            rg.setRelativeRadiusY(r / bh);
            g.fillGradient(rg, bx, by, bw, bh);
        }
        restoreClip(cx, cy, cw, ch);
    }

    /// A Rive linear gradient runs from (sx, sy) to (ex, ey). The core's
    /// `LinearGradient` is CSS's: an angle, with a gradient line through the
    /// middle of the filled rectangle. Rive's stops are moved onto that line
    /// here. `LinearGradientPaint` would take the two points as they are, but
    /// it rotates through `Transform` and throws on ports without transforms.
    private void fillLinear(int[] colors, float[] positions, float sx, float sy, float ex, float ey,
            int bx, int by, int bw, int bh) {
        float dx = ex - sx;
        float dy = ey - sy;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len == 0f) {
            g.setColor(colors[colors.length - 1] & 0xffffff);
            g.setAlpha(alpha(colors[colors.length - 1]));
            g.fillRect(bx, by, bw, bh);
            g.setAlpha(255);
            return;
        }
        float sin = dx / len;
        float cos = -dy / len;
        float half = Math.abs(bw * 0.5f * sin) + Math.abs(bh * 0.5f * cos);
        float cx = bx + bw * 0.5f;
        float cy = by + bh * 0.5f;
        float projS = (sx - cx) * sin - (sy - cy) * cos;
        int n = positions.length;
        float[] css = new float[n];
        for (int i = 0; i < n; i++) {
            css[i] = half == 0f ? positions[i] : (positions[i] * len + projS + half) / (2f * half);
        }
        float angle = (float) Math.toDegrees(com.codename1.util.MathUtil.atan2(sin, cos));
        int count = clipStops(colors, css);
        int[] c = new int[count];
        float[] p = new float[count];
        System.arraycopy(clipColors, 0, c, 0, count);
        System.arraycopy(clipPositions, 0, p, 0, count);
        g.fillGradient(new com.codename1.ui.LinearGradient(angle, c, p), bx, by, bw, bh);
    }

    private int[] clipColors = new int[8];
    private float[] clipPositions = new float[8];

    /// Clips stops at increasing positions to [0, 1], putting an interpolated
    /// stop at each end that a stop lies past; results go to clipColors and
    /// clipPositions. Always at least two stops.
    private int clipStops(int[] colors, float[] pos) {
        int n = pos.length;
        if (clipColors.length < n + 2) {
            clipColors = new int[n + 2];
            clipPositions = new float[n + 2];
        }
        int out = 0;
        for (int i = 0; i < n; i++) {
            float p = pos[i];
            if (p < 0f) {
                if (i + 1 < n && pos[i + 1] > 0f) {
                    clipColors[out] = lerp(colors[i], colors[i + 1], -p / (pos[i + 1] - p));
                    clipPositions[out++] = 0f;
                }
                continue;
            }
            if (p > 1f) {
                if (i > 0 && pos[i - 1] < 1f) {
                    clipColors[out] = lerp(colors[i - 1], colors[i], (1f - pos[i - 1]) / (p - pos[i - 1]));
                    clipPositions[out++] = 1f;
                }
                break;
            }
            clipColors[out] = colors[i];
            clipPositions[out++] = p;
        }
        if (out == 0) {
            int c = pos[0] > 1f ? colors[0] : colors[n - 1];
            clipColors[0] = c;
            clipColors[1] = c;
            clipPositions[0] = 0f;
            clipPositions[1] = 1f;
            return 2;
        }
        if (out == 1) {
            clipColors[1] = clipColors[0];
            clipPositions[1] = clipPositions[0] < 1f ? 1f : clipPositions[0];
            if (clipPositions[0] >= 1f) {
                clipPositions[0] = 0f;
            }
            return 2;
        }
        return out;
    }

    private static int lerp(int a, int b, float t) {
        int r = 0;
        for (int shift = 0; shift < 32; shift += 8) {
            int ca = (a >>> shift) & 0xff;
            int cb = (b >>> shift) & 0xff;
            r |= ((int) (ca + (cb - ca) * t + 0.5f) & 0xff) << shift;
        }
        return r;
    }

    @Override
    public void drawImage(Object image, int blendMode, float opacity) {
        if (!(image instanceof com.codename1.ui.Image)) {
            return;
        }
        com.codename1.ui.Image img = (com.codename1.ui.Image) image;
        float[] v = matrix.v;
        g.setAlpha((int) (255 * Math.max(0f, Math.min(1f, opacity))));
        if (v[1] == 0f && v[2] == 0f) {
            g.drawImage(img, (int) v[4], (int) v[5], (int) (img.getWidth() * v[0]), (int) (img.getHeight() * v[3]));
        } else {
            drawImageQuad(img, blendMode, opacity);
        }
        g.setAlpha(255);
    }

    private static final float[] QUAD_UVS = {0, 0, 1, 0, 1, 1, 0, 1};
    private static final int[] QUAD_INDICES = {0, 1, 2, 0, 2, 3};
    private final float[] quadVertices = new float[8];
    private final GeneralPath triangle = new GeneralPath();
    private com.codename1.ui.Transform meshTransform;
    private com.codename1.ui.Transform savedTransform;

    /// Draws `image` rotated or skewed as a two-triangle mesh.
    void drawImageQuad(com.codename1.ui.Image img, int blendMode, float opacity) {
        float w = img.getWidth();
        float h = img.getHeight();
        float[] q = quadVertices;
        q[0] = 0;
        q[1] = 0;
        q[2] = w;
        q[3] = 0;
        q[4] = w;
        q[5] = h;
        q[6] = 0;
        q[7] = h;
        drawImageMesh(img, q, QUAD_UVS, 4, QUAD_INDICES, 6, blendMode, opacity);
    }

    /// Each triangle is drawn as the image under the affine map that takes
    /// its texture triangle onto its deformed triangle, clipped to that
    /// triangle. A port without transforms or shape clips gets the image
    /// stretched over the bounds of the deformed vertices instead: the
    /// placement follows the bones, the deformation inside does not.
    @Override
    public void drawImageMesh(Object image, float[] vertices, float[] uvs, int vertexCount,
            int[] indices, int indexCount, int blendMode, float opacity) {
        if (!(image instanceof com.codename1.ui.Image) || vertexCount == 0) {
            return;
        }
        com.codename1.ui.Image img = (com.codename1.ui.Image) image;
        g.setAlpha((int) (255 * Math.max(0f, Math.min(1f, opacity))));
        if (indexCount >= 3 && g.isTransformSupported() && g.isShapeClipSupported()) {
            drawTriangles(img, vertices, uvs, indices, indexCount);
        } else {
            drawMeshBounds(img, vertices, vertexCount);
        }
        g.setAlpha(255);
    }

    private void drawTriangles(com.codename1.ui.Image img, float[] vertices, float[] uvs, int[] indices, int indexCount) {
        if (meshTransform == null) {
            meshTransform = com.codename1.ui.Transform.makeIdentity();
            savedTransform = com.codename1.ui.Transform.makeIdentity();
        }
        g.getTransform(savedTransform);
        float[] v = matrix.v;
        float iw = img.getWidth();
        float ih = img.getHeight();
        int cx = g.getClipX();
        int cy = g.getClipY();
        int cw = g.getClipWidth();
        int ch = g.getClipHeight();
        for (int t = 0; t + 2 < indexCount; t += 3) {
            int i0 = indices[t] * 2;
            int i1 = indices[t + 1] * 2;
            int i2 = indices[t + 2] * 2;
            float x0 = v[0] * vertices[i0] + v[2] * vertices[i0 + 1] + v[4];
            float y0 = v[1] * vertices[i0] + v[3] * vertices[i0 + 1] + v[5];
            float x1 = v[0] * vertices[i1] + v[2] * vertices[i1 + 1] + v[4];
            float y1 = v[1] * vertices[i1] + v[3] * vertices[i1 + 1] + v[5];
            float x2 = v[0] * vertices[i2] + v[2] * vertices[i2 + 1] + v[4];
            float y2 = v[1] * vertices[i2] + v[3] * vertices[i2 + 1] + v[5];
            float s0 = uvs[i0] * iw;
            float t0 = uvs[i0 + 1] * ih;
            float ds1 = uvs[i1] * iw - s0;
            float dt1 = uvs[i1 + 1] * ih - t0;
            float ds2 = uvs[i2] * iw - s0;
            float dt2 = uvs[i2 + 1] * ih - t0;
            float det = ds1 * dt2 - ds2 * dt1;
            if (Math.abs(det) < 1e-6f) {
                continue;
            }
            float dx1 = x1 - x0;
            float dy1 = y1 - y0;
            float dx2 = x2 - x0;
            float dy2 = y2 - y0;
            float a = (dx1 * dt2 - dx2 * dt1) / det;
            float c = (dx2 * ds1 - dx1 * ds2) / det;
            float b = (dy1 * dt2 - dy2 * dt1) / det;
            float d = (dy2 * ds1 - dy1 * ds2) / det;
            float e = x0 - a * s0 - c * t0;
            float f = y0 - b * s0 - d * t0;
            triangle.reset();
            triangle.moveTo(x0, y0);
            triangle.lineTo(x1, y1);
            triangle.lineTo(x2, y2);
            triangle.closePath();
            if (!shapeClip(triangle, cx, cy, cw, ch)) {
                // unclipped, the whole image would cover the rectangle
                continue;
            }
            meshTransform.setAffine(a, b, c, d, e, f);
            g.setTransform(meshTransform);
            g.drawImage(img, 0, 0);
            g.setTransform(savedTransform);
        }
        restoreClip(cx, cy, cw, ch);
    }

    private void drawMeshBounds(com.codename1.ui.Image img, float[] vertices, int vertexCount) {
        float[] v = matrix.v;
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (int i = 0; i < vertexCount; i++) {
            float x = vertices[i * 2];
            float y = vertices[i * 2 + 1];
            float mx = v[0] * x + v[2] * y + v[4];
            float my = v[1] * x + v[3] * y + v[5];
            minX = Math.min(minX, mx);
            minY = Math.min(minY, my);
            maxX = Math.max(maxX, mx);
            maxY = Math.max(maxY, my);
        }
        g.drawImage(img, (int) minX, (int) minY, (int) (maxX - minX), (int) (maxY - minY));
    }

    @Override
    public boolean supportsMeshes() {
        return false;
    }
}
