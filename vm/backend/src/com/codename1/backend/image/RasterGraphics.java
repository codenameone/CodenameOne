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
package com.codename1.backend.image;

/// Draws antialiased shapes, gradients and images into a [RasterImage].
///
/// Everything is pure Java, so the same drawing code runs on a device, in
/// the simulator and on a Codename One server that generates images (charts,
/// thumbnails, social cards) with no display at all.
///
/// - **Coordinates** are user space, mapped to pixels by an affine transform
///   ([#translate(double, double)], [#scale(double, double)],
///   [#rotate(double)], [#setTransform(double, double, double, double, double, double)]).
///   Pixel `(x, y)` covers the square from `(x, y)` to `(x + 1, y + 1)`, so a
///   one pixel wide line along `y = 10.5` covers row 10 exactly.
/// - **Painting** is source-over compositing of non-premultiplied colors, with
///   [#setColor(int)] or a [RasterPaint] such as [RasterGradient], scaled by
///   [#setAlpha(float)].
/// - **Coverage** is computed on eight sub-scanlines per pixel with exact
///   horizontal area; [#setAntiAlias(boolean)] turns it off.
/// - **Clipping** is a rectangle in pixels: [#clipRect(double, double, double, double)]
///   intersects the bounds of a transformed rectangle with it.
/// - **Text** goes through the [RasterFont] seam set with [#setFont(RasterFont)].
///
/// A graphics object is not thread safe; create one per thread.
public final class RasterGraphics {
    /// Stroke ends stop exactly at the end points.
    public static final int CAP_BUTT = 0;
    /// Stroke ends are half circles.
    public static final int CAP_ROUND = 1;
    /// Stroke ends extend by half the line width.
    public static final int CAP_SQUARE = 2;
    /// Stroke corners extend to a sharp point, up to the miter limit.
    public static final int JOIN_MITER = 0;
    /// Stroke corners are rounded.
    public static final int JOIN_ROUND = 1;
    /// Stroke corners are cut flat.
    public static final int JOIN_BEVEL = 2;
    /// A point is inside when the path winds around it a non-zero number of times.
    public static final int WIND_NON_ZERO = 0;
    /// A point is inside when the path crosses a ray from it an odd number of times.
    public static final int WIND_EVEN_ODD = 1;

    private static final int SUBSAMPLES = 8;

    private final RasterImage target;
    private final int[] px;
    private final int width;
    private final int height;
    private double ta = 1;
    private double tb;
    private double tc;
    private double td = 1;
    private double te;
    private double tf;
    private int color = 0xff000000;
    private RasterPaint paint;
    private float alpha = 1f;
    private double strokeWidth = 1;
    private int cap = CAP_BUTT;
    private int join = JOIN_MITER;
    private double miterLimit = 10;
    private int windingRule = WIND_NON_ZERO;
    private boolean antiAlias = true;
    private int clipX0;
    private int clipY0;
    private int clipX1;
    private int clipY1;
    private RasterFont font;

    // Edge list of the shape being filled, in device space.
    private double[] edges = new double[64];
    private int edgeCount;

    /// Creates a graphics that draws into `target`; also available as
    /// [RasterImage#createGraphics()].
    ///
    /// #### Parameters
    ///
    /// - `target`: the image to draw into
    public RasterGraphics(RasterImage target) {
        this.target = target;
        this.px = target.getPixels();
        this.width = target.getWidth();
        this.height = target.getHeight();
        resetClip();
    }

    /// Returns the image this graphics draws into.
    public RasterImage getTarget() {
        return target;
    }

    /// Sets a solid ARGB color and clears any paint.
    ///
    /// #### Parameters
    ///
    /// - `argb`: the color, not premultiplied
    public void setColor(int argb) {
        this.color = argb;
        this.paint = null;
    }

    /// Returns the solid color.
    public int getColor() {
        return color;
    }

    /// Sets the paint used instead of the solid color; null returns to the color.
    ///
    /// #### Parameters
    ///
    /// - `paint`: a gradient or pattern
    public void setPaint(RasterPaint paint) {
        this.paint = paint;
    }

    /// Returns the current paint, or null when drawing with the solid color.
    public RasterPaint getPaint() {
        return paint;
    }

    /// Sets an opacity that multiplies everything drawn.
    ///
    /// #### Parameters
    ///
    /// - `alpha`: 0 (invisible) to 1 (opaque)
    public void setAlpha(float alpha) {
        this.alpha = alpha < 0 ? 0 : alpha > 1 ? 1 : alpha;
    }

    /// Returns the opacity set by [#setAlpha(float)].
    public float getAlpha() {
        return alpha;
    }

    /// Sets the line width used by the draw methods, in user-space units.
    ///
    /// #### Parameters
    ///
    /// - `w`: the width
    public void setStrokeWidth(double w) {
        this.strokeWidth = w < 0 ? 0 : w;
    }

    /// Returns the line width.
    public double getStrokeWidth() {
        return strokeWidth;
    }

    /// Sets how open line ends are drawn.
    ///
    /// #### Parameters
    ///
    /// - `cap`: `CAP_BUTT`, `CAP_ROUND` or `CAP_SQUARE`
    public void setLineCap(int cap) {
        this.cap = cap;
    }

    /// Sets how line corners are drawn.
    ///
    /// #### Parameters
    ///
    /// - `join`: `JOIN_MITER`, `JOIN_ROUND` or `JOIN_BEVEL`
    public void setLineJoin(int join) {
        this.join = join;
    }

    /// Sets the longest a miter may be, as a multiple of half the line width;
    /// sharper corners are beveled.
    ///
    /// #### Parameters
    ///
    /// - `limit`: at least 1
    public void setMiterLimit(double limit) {
        this.miterLimit = Math.max(1, limit);
    }

    /// Sets the rule deciding what is inside a self-intersecting path.
    ///
    /// #### Parameters
    ///
    /// - `rule`: `WIND_NON_ZERO` or `WIND_EVEN_ODD`
    public void setWindingRule(int rule) {
        this.windingRule = rule;
    }

    /// Turns antialiasing on (the default) or off.
    ///
    /// #### Parameters
    ///
    /// - `aa`: true for smooth edges
    public void setAntiAlias(boolean aa) {
        this.antiAlias = aa;
    }

    /// Sets the font [#drawString(String, double, double)] uses.
    ///
    /// #### Parameters
    ///
    /// - `font`: the font
    public void setFont(RasterFont font) {
        this.font = font;
    }

    /// Returns the font, or null.
    public RasterFont getFont() {
        return font;
    }

    // ------------------------------------------------------------- transform

    /// Moves the origin.
    ///
    /// #### Parameters
    ///
    /// - `dx`: horizontal offset
    /// - `dy`: vertical offset
    public void translate(double dx, double dy) {
        te += ta * dx + tc * dy;
        tf += tb * dx + td * dy;
    }

    /// Scales user space.
    ///
    /// #### Parameters
    ///
    /// - `sx`: horizontal factor
    /// - `sy`: vertical factor
    public void scale(double sx, double sy) {
        ta *= sx;
        tb *= sx;
        tc *= sy;
        td *= sy;
    }

    /// Rotates user space about the origin.
    ///
    /// #### Parameters
    ///
    /// - `radians`: the angle; positive turns x toward y (clockwise on screen)
    public void rotate(double radians) {
        double c = Math.cos(radians);
        double s = Math.sin(radians);
        double na = ta * c + tc * s;
        double nb = tb * c + td * s;
        double nc = -ta * s + tc * c;
        double nd = -tb * s + td * c;
        ta = na;
        tb = nb;
        tc = nc;
        td = nd;
    }

    /// Replaces the transform: device `x = a * x + c * y + e`,
    /// `y = b * x + d * y + f`.
    ///
    /// #### Parameters
    ///
    /// - `a`: x scale
    /// - `b`: y shear
    /// - `c`: x shear
    /// - `d`: y scale
    /// - `e`: x translation
    /// - `f`: y translation
    public void setTransform(double a, double b, double c, double d, double e, double f) {
        ta = a;
        tb = b;
        tc = c;
        td = d;
        te = e;
        tf = f;
    }

    /// Returns the transform as `{a, b, c, d, e, f}`.
    public double[] getTransform() {
        return new double[] {ta, tb, tc, td, te, tf};
    }

    /// Restores the identity transform.
    public void resetTransform() {
        setTransform(1, 0, 0, 1, 0, 0);
    }

    // ------------------------------------------------------------------ clip

    /// Removes the clip, so the whole image can be drawn.
    public void resetClip() {
        clipX0 = 0;
        clipY0 = 0;
        clipX1 = width;
        clipY1 = height;
    }

    /// Intersects the clip with the pixel bounds of a transformed rectangle.
    ///
    /// #### Parameters
    ///
    /// - `x`: left
    /// - `y`: top
    /// - `w`: width
    /// - `h`: height
    public void clipRect(double x, double y, double w, double h) {
        double[] xs = {x, x + w, x + w, x};
        double[] ys = {y, y, y + h, y + h};
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            double dx = ta * xs[i] + tc * ys[i] + te;
            double dy = tb * xs[i] + td * ys[i] + tf;
            minX = Math.min(minX, dx);
            minY = Math.min(minY, dy);
            maxX = Math.max(maxX, dx);
            maxY = Math.max(maxY, dy);
        }
        clipX0 = Math.max(clipX0, (int) Math.floor(minX + 0.5));
        clipY0 = Math.max(clipY0, (int) Math.floor(minY + 0.5));
        clipX1 = Math.min(clipX1, (int) Math.floor(maxX + 0.5));
        clipY1 = Math.min(clipY1, (int) Math.floor(maxY + 0.5));
    }

    /// Replaces the clip with the pixel bounds of a transformed rectangle.
    ///
    /// #### Parameters
    ///
    /// - `x`: left
    /// - `y`: top
    /// - `w`: width
    /// - `h`: height
    public void setClip(double x, double y, double w, double h) {
        resetClip();
        clipRect(x, y, w, h);
    }

    /// Returns the clip as pixel bounds `{x0, y0, x1, y1}`, right and bottom
    /// exclusive.
    public int[] getClipBounds() {
        return new int[] {clipX0, clipY0, clipX1, clipY1};
    }

    // ---------------------------------------------------------- convenience

    /// Replaces every pixel inside the clip with a color, without blending.
    ///
    /// #### Parameters
    ///
    /// - `argb`: the color
    public void clear(int argb) {
        for (int y = clipY0; y < clipY1; y++) {
            for (int x = clipX0; x < clipX1; x++) {
                px[y * width + x] = argb;
            }
        }
    }

    /// Fills a rectangle.
    ///
    /// #### Parameters
    ///
    /// - `x`: left
    /// - `y`: top
    /// - `w`: width
    /// - `h`: height
    public void fillRect(double x, double y, double w, double h) {
        fill(RasterPath.rect(x, y, w, h));
    }

    /// Strokes a rectangle's outline.
    ///
    /// #### Parameters
    ///
    /// - `x`: left
    /// - `y`: top
    /// - `w`: width
    /// - `h`: height
    public void drawRect(double x, double y, double w, double h) {
        draw(RasterPath.rect(x, y, w, h));
    }

    /// Fills a rectangle with elliptical corners.
    ///
    /// #### Parameters
    ///
    /// - `x`: left
    /// - `y`: top
    /// - `w`: width
    /// - `h`: height
    /// - `arcW`: horizontal corner diameter
    /// - `arcH`: vertical corner diameter
    public void fillRoundRect(double x, double y, double w, double h, double arcW, double arcH) {
        fill(RasterPath.roundRect(x, y, w, h, arcW, arcH));
    }

    /// Strokes a rectangle with elliptical corners.
    ///
    /// #### Parameters
    ///
    /// - `x`: left
    /// - `y`: top
    /// - `w`: width
    /// - `h`: height
    /// - `arcW`: horizontal corner diameter
    /// - `arcH`: vertical corner diameter
    public void drawRoundRect(double x, double y, double w, double h, double arcW, double arcH) {
        draw(RasterPath.roundRect(x, y, w, h, arcW, arcH));
    }

    /// Fills the ellipse inscribed in a rectangle.
    ///
    /// #### Parameters
    ///
    /// - `x`: left
    /// - `y`: top
    /// - `w`: width
    /// - `h`: height
    public void fillOval(double x, double y, double w, double h) {
        fill(RasterPath.ellipse(x, y, w, h));
    }

    /// Strokes the ellipse inscribed in a rectangle.
    ///
    /// #### Parameters
    ///
    /// - `x`: left
    /// - `y`: top
    /// - `w`: width
    /// - `h`: height
    public void drawOval(double x, double y, double w, double h) {
        draw(RasterPath.ellipse(x, y, w, h));
    }

    /// Fills a pie slice of the ellipse inscribed in a rectangle.
    ///
    /// #### Parameters
    ///
    /// - `x`: left
    /// - `y`: top
    /// - `w`: width
    /// - `h`: height
    /// - `startDeg`: start angle, degrees counterclockwise from three o'clock
    /// - `extentDeg`: extent in degrees
    public void fillArc(double x, double y, double w, double h, double startDeg, double extentDeg) {
        fill(RasterPath.arc(x, y, w, h, startDeg, extentDeg, RasterPath.ARC_PIE));
    }

    /// Strokes an open arc of the ellipse inscribed in a rectangle.
    ///
    /// #### Parameters
    ///
    /// - `x`: left
    /// - `y`: top
    /// - `w`: width
    /// - `h`: height
    /// - `startDeg`: start angle, degrees counterclockwise from three o'clock
    /// - `extentDeg`: extent in degrees
    public void drawArc(double x, double y, double w, double h, double startDeg, double extentDeg) {
        draw(RasterPath.arc(x, y, w, h, startDeg, extentDeg, RasterPath.ARC_OPEN));
    }

    /// Strokes a straight line.
    ///
    /// #### Parameters
    ///
    /// - `x1`: start x
    /// - `y1`: start y
    /// - `x2`: end x
    /// - `y2`: end y
    public void drawLine(double x1, double y1, double x2, double y2) {
        draw(new RasterPath().moveTo(x1, y1).lineTo(x2, y2));
    }

    /// Fills a polygon.
    ///
    /// #### Parameters
    ///
    /// - `xs`: vertex x values
    /// - `ys`: vertex y values
    /// - `n`: vertex count
    public void fillPolygon(double[] xs, double[] ys, int n) {
        fill(RasterPath.polygon(xs, ys, n));
    }

    /// Strokes a closed polygon.
    ///
    /// #### Parameters
    ///
    /// - `xs`: vertex x values
    /// - `ys`: vertex y values
    /// - `n`: vertex count
    public void drawPolygon(double[] xs, double[] ys, int n) {
        draw(RasterPath.polygon(xs, ys, n));
    }

    /// Strokes an open chain of lines.
    ///
    /// #### Parameters
    ///
    /// - `xs`: vertex x values
    /// - `ys`: vertex y values
    /// - `n`: vertex count
    public void drawPolyline(double[] xs, double[] ys, int n) {
        draw(RasterPath.polyline(xs, ys, n));
    }

    /// Draws text with the font set by [#setFont(RasterFont)].
    ///
    /// #### Parameters
    ///
    /// - `text`: the string
    /// - `x`: left end of the baseline
    /// - `y`: the baseline
    public void drawString(String text, double x, double y) {
        if (font == null) {
            throw new IllegalStateException("no RasterFont set");
        }
        int rule = windingRule;
        windingRule = WIND_NON_ZERO;
        fill(font.getOutline(text, x, y));
        windingRule = rule;
    }

    /// Draws an image at its own size.
    ///
    /// #### Parameters
    ///
    /// - `img`: the image
    /// - `x`: left
    /// - `y`: top
    public void drawImage(RasterImage img, double x, double y) {
        drawImage(img, x, y, img.getWidth(), img.getHeight());
    }

    /// Draws an image scaled to a rectangle, sampled bilinearly through the
    /// transform. For large reductions scale it first with
    /// [RasterImage#scale(int, int, int)], which filters every source pixel.
    ///
    /// #### Parameters
    ///
    /// - `img`: the image
    /// - `x`: left
    /// - `y`: top
    /// - `w`: width
    /// - `h`: height
    public void drawImage(RasterImage img, double x, double y, double w, double h) {
        if (w == 0 || h == 0) {
            return;
        }
        RasterPaint saved = paint;
        paint = new ImagePaint(img, x, y, w / img.getWidth(), h / img.getHeight());
        fill(RasterPath.rect(x, y, w, h));
        paint = saved;
    }

    /// Samples an image placed at `(x, y)` with a scale, bilinearly.
    private static final class ImagePaint implements RasterPaint {
        private final RasterImage img;
        private final double x;
        private final double y;
        private final double sx;
        private final double sy;

        ImagePaint(RasterImage img, double x, double y, double sx, double sy) {
            this.img = img;
            this.x = x;
            this.y = y;
            this.sx = sx;
            this.sy = sy;
        }

        @Override
        public int getColor(double ux, double uy) {
            int w = img.getWidth();
            int h = img.getHeight();
            int[] p = img.getPixels();
            double fx = (ux - x) / sx - 0.5;
            double fy = (uy - y) / sy - 0.5;
            int x0 = (int) Math.floor(fx);
            int y0 = (int) Math.floor(fy);
            double ax = fx - x0;
            double ay = fy - y0;
            int x1 = clampI(x0 + 1, w);
            int y1 = clampI(y0 + 1, h);
            x0 = clampI(x0, w);
            y0 = clampI(y0, h);
            int top = RasterGradient.lerp(p[y0 * w + x0], p[y0 * w + x1], ax);
            int bottom = RasterGradient.lerp(p[y1 * w + x0], p[y1 * w + x1], ax);
            return RasterGradient.lerp(top, bottom, ay);
        }
    }

    private static int clampI(int v, int n) {
        return v < 0 ? 0 : v >= n ? n - 1 : v;
    }

    // ------------------------------------------------------------- filling

    /// Fills a path with the current color or paint and winding rule.
    ///
    /// #### Parameters
    ///
    /// - `path`: the shape
    public void fill(RasterPath path) {
        edgeCount = 0;
        double[][] polys = flatten(path);
        for (double[] poly : polys) {
            addPolygon(poly, poly.length / 2, false);
        }
        rasterize(windingRule);
    }

    /// Strokes a path with the current line width, caps and joins.
    ///
    /// #### Parameters
    ///
    /// - `path`: the shape
    public void draw(RasterPath path) {
        edgeCount = 0;
        double hw = strokeWidth / 2;
        if (hw <= 0) {
            return;
        }
        double[][] polys = flatten(path);
        boolean[] closed = closedFlags(path, polys.length);
        for (int i = 0; i < polys.length; i++) {
            strokePolyline(polys[i], closed[i], hw);
        }
        rasterize(WIND_NON_ZERO);
    }

    private static boolean[] closedFlags(RasterPath path, int count) {
        boolean[] r = new boolean[count];
        int sub = -1;
        for (int i = 0; i < path.opCount; i++) {
            if (path.ops[i] == RasterPath.MOVE) {
                sub++;
            } else if (path.ops[i] == RasterPath.CLOSE && sub >= 0 && sub < count) {
                r[sub] = true;
            }
        }
        return r;
    }

    /// How many line segments a curve with this device-space control
    /// polygon length needs.
    private int segments(double[] k, int from, int n) {
        double len = 0;
        for (int i = 0; i + 1 < n; i++) {
            double x0 = ta * k[from + i * 2] + tc * k[from + i * 2 + 1];
            double y0 = tb * k[from + i * 2] + td * k[from + i * 2 + 1];
            double x1 = ta * k[from + i * 2 + 2] + tc * k[from + i * 2 + 3];
            double y1 = tb * k[from + i * 2 + 2] + td * k[from + i * 2 + 3];
            len += Math.sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0));
        }
        int s = (int) Math.ceil(Math.sqrt(len) * 1.5);
        return s < 2 ? 2 : s > 200 ? 200 : s;
    }

    /// Flattens a path to user-space polylines, one per subpath.
    private double[][] flatten(RasterPath path) {
        java.util.ArrayList<double[]> list = new java.util.ArrayList<double[]>();
        double[] cur = new double[32];
        int n = 0;
        double[] k = path.coords;
        int c = 0;
        double lx = 0;
        double ly = 0;
        double[] seg = new double[8];
        for (int i = 0; i < path.opCount; i++) {
            byte o = path.ops[i];
            if (o == RasterPath.MOVE) {
                if (n > 0) {
                    list.add(trim(cur, n));
                }
                n = 0;
                lx = k[c];
                ly = k[c + 1];
                c += 2;
                cur = push(cur, n, lx, ly);
                n += 2;
            } else if (o == RasterPath.LINE) {
                lx = k[c];
                ly = k[c + 1];
                c += 2;
                cur = push(cur, n, lx, ly);
                n += 2;
            } else if (o == RasterPath.QUAD || o == RasterPath.CUBIC) {
                int pts = o == RasterPath.QUAD ? 3 : 4;
                seg[0] = lx;
                seg[1] = ly;
                System.arraycopy(k, c, seg, 2, (pts - 1) * 2);
                c += (pts - 1) * 2;
                int steps = segments(seg, 0, pts);
                for (int s = 1; s <= steps; s++) {
                    double t = (double) s / steps;
                    double u = 1 - t;
                    double x;
                    double y;
                    if (pts == 3) {
                        x = u * u * seg[0] + 2 * u * t * seg[2] + t * t * seg[4];
                        y = u * u * seg[1] + 2 * u * t * seg[3] + t * t * seg[5];
                    } else {
                        x = u * u * u * seg[0] + 3 * u * u * t * seg[2] + 3 * u * t * t * seg[4] + t * t * t * seg[6];
                        y = u * u * u * seg[1] + 3 * u * u * t * seg[3] + 3 * u * t * t * seg[5] + t * t * t * seg[7];
                    }
                    cur = push(cur, n, x, y);
                    n += 2;
                }
                lx = seg[(pts - 1) * 2];
                ly = seg[(pts - 1) * 2 + 1];
            } else if (n > 0) {
                // CLOSE: the next segment starts back at the subpath's start.
                lx = cur[0];
                ly = cur[1];
            }
        }
        if (n > 0) {
            list.add(trim(cur, n));
        }
        double[][] r = new double[list.size()][];
        for (int i = 0; i < r.length; i++) {
            r[i] = list.get(i);
        }
        return r;
    }

    private static double[] push(double[] a, int n, double x, double y) {
        double[] r = a;
        if (n + 2 > r.length) {
            r = new double[r.length * 2];
            System.arraycopy(a, 0, r, 0, n);
        }
        r[n] = x;
        r[n + 1] = y;
        return r;
    }

    private static double[] trim(double[] a, int n) {
        double[] r = new double[n];
        System.arraycopy(a, 0, r, 0, n);
        return r;
    }

    /// Adds a closed user-space polygon to the edge list, transformed.
    /// With `orient` the polygon is first made counterclockwise, so the
    /// pieces of a stroke union under the non-zero rule.
    private void addPolygon(double[] p, int count, boolean orient) {
        if (count < 2) {
            return;
        }
        boolean reverse = false;
        if (orient) {
            double area = 0;
            for (int i = 0; i < count; i++) {
                int j = (i + 1) % count;
                area += p[i * 2] * p[j * 2 + 1] - p[j * 2] * p[i * 2 + 1];
            }
            double det = ta * td - tb * tc;
            reverse = area * det < 0;
        }
        for (int i = 0; i < count; i++) {
            int a = reverse ? count - 1 - i : i;
            int b = reverse ? (a - 1 + count) % count : (a + 1) % count;
            double x0 = ta * p[a * 2] + tc * p[a * 2 + 1] + te;
            double y0 = tb * p[a * 2] + td * p[a * 2 + 1] + tf;
            double x1 = ta * p[b * 2] + tc * p[b * 2 + 1] + te;
            double y1 = tb * p[b * 2] + td * p[b * 2 + 1] + tf;
            if (y0 == y1) {
                continue;
            }
            if (edgeCount * 5 + 5 > edges.length) {
                double[] e = new double[edges.length * 2];
                System.arraycopy(edges, 0, e, 0, edgeCount * 5);
                edges = e;
            }
            int o = edgeCount * 5;
            if (y0 < y1) {
                edges[o] = x0;
                edges[o + 1] = y0;
                edges[o + 2] = x1;
                edges[o + 3] = y1;
                edges[o + 4] = 1;
            } else {
                edges[o] = x1;
                edges[o + 1] = y1;
                edges[o + 2] = x0;
                edges[o + 3] = y0;
                edges[o + 4] = -1;
            }
            edgeCount++;
        }
    }

    // ------------------------------------------------------------- stroking

    private void quad(double ax, double ay, double bx, double by, double cx, double cy, double dx, double dy) {
        addPolygon(new double[] {ax, ay, bx, by, cx, cy, dx, dy}, 4, true);
    }

    private void circle(double x, double y, double r) {
        double scale = Math.sqrt(Math.abs(ta * td - tb * tc));
        int n = (int) Math.ceil(Math.sqrt(r * scale) * 6);
        n = n < 8 ? 8 : n > 128 ? 128 : n;
        double[] p = new double[n * 2];
        for (int i = 0; i < n; i++) {
            double a = 2 * Math.PI * i / n;
            p[i * 2] = x + r * Math.cos(a);
            p[i * 2 + 1] = y + r * Math.sin(a);
        }
        addPolygon(p, n, true);
    }

    private void strokePolyline(double[] p, boolean closed, double hw) {
        // Drop repeated points so every segment has a direction.
        double[] q = new double[p.length];
        int n = 0;
        for (int i = 0; i < p.length; i += 2) {
            if (n == 0 || p[i] != q[n * 2 - 2] || p[i + 1] != q[n * 2 - 1]) {
                q[n * 2] = p[i];
                q[n * 2 + 1] = p[i + 1];
                n++;
            }
        }
        if (closed && n > 1 && q[0] == q[n * 2 - 2] && q[1] == q[n * 2 - 1]) {
            n--;
        }
        if (n == 1) {
            if (cap == CAP_ROUND) {
                circle(q[0], q[1], hw);
            } else if (cap == CAP_SQUARE) {
                quad(q[0] - hw, q[1] - hw, q[0] + hw, q[1] - hw, q[0] + hw, q[1] + hw, q[0] - hw, q[1] + hw);
            }
            return;
        }
        int segs = closed ? n : n - 1;
        for (int i = 0; i < segs; i++) {
            int j = (i + 1) % n;
            double x0 = q[i * 2];
            double y0 = q[i * 2 + 1];
            double x1 = q[j * 2];
            double y1 = q[j * 2 + 1];
            double dx = x1 - x0;
            double dy = y1 - y0;
            double len = Math.sqrt(dx * dx + dy * dy);
            double nx = -dy / len * hw;
            double ny = dx / len * hw;
            quad(x0 + nx, y0 + ny, x1 + nx, y1 + ny, x1 - nx, y1 - ny, x0 - nx, y0 - ny);
        }
        int first = closed ? 0 : 1;
        int last = closed ? n - 1 : n - 2;
        for (int i = first; i <= last; i++) {
            int a = (i - 1 + n) % n;
            int b = (i + 1) % n;
            joinAt(q[a * 2], q[a * 2 + 1], q[i * 2], q[i * 2 + 1], q[b * 2], q[b * 2 + 1], hw);
        }
        if (!closed) {
            capAt(q[2], q[3], q[0], q[1], hw);
            capAt(q[n * 2 - 4], q[n * 2 - 3], q[n * 2 - 2], q[n * 2 - 1], hw);
        }
    }

    /// Caps the end `(x, y)` of a segment arriving from `(fx, fy)`.
    private void capAt(double fx, double fy, double x, double y, double hw) {
        if (cap == CAP_ROUND) {
            circle(x, y, hw);
        } else if (cap == CAP_SQUARE) {
            double dx = x - fx;
            double dy = y - fy;
            double len = Math.sqrt(dx * dx + dy * dy);
            double ux = dx / len * hw;
            double uy = dy / len * hw;
            quad(x - uy, y + ux, x + ux - uy, y + uy + ux, x + ux + uy, y + uy - ux, x + uy, y - ux);
        }
    }

    private void joinAt(double ax, double ay, double x, double y, double bx, double by, double hw) {
        if (join == JOIN_ROUND) {
            circle(x, y, hw);
            return;
        }
        double d1x = x - ax;
        double d1y = y - ay;
        double d2x = bx - x;
        double d2y = by - y;
        double l1 = Math.sqrt(d1x * d1x + d1y * d1y);
        double l2 = Math.sqrt(d2x * d2x + d2y * d2y);
        d1x /= l1;
        d1y /= l1;
        d2x /= l2;
        d2y /= l2;
        double cross = d1x * d2y - d1y * d2x;
        if (Math.abs(cross) < 1e-12 && d1x * d2x + d1y * d2y > 0) {
            return;
        }
        double side = cross > 0 ? -1 : 1;
        double n1x = -d1y * side * hw;
        double n1y = d1x * side * hw;
        double n2x = -d2y * side * hw;
        double n2y = d2x * side * hw;
        if (join == JOIN_MITER) {
            double mx = n1x + n2x;
            double my = n1y + n2y;
            double ml = Math.sqrt(mx * mx + my * my);
            if (ml > 1e-12) {
                double cosHalf = (mx * n1x + my * n1y) / (ml * hw);
                if (cosHalf > 1e-9 && 1 / cosHalf <= miterLimit) {
                    double len = hw / cosHalf;
                    double tx = x + mx / ml * len;
                    double ty = y + my / ml * len;
                    quad(x, y, x + n1x, y + n1y, tx, ty, x + n2x, y + n2y);
                    return;
                }
            }
        }
        addPolygon(new double[] {x, y, x + n1x, y + n1y, x + n2x, y + n2y}, 3, true);
    }

    // ---------------------------------------------------------- rasterizing

    private void rasterize(int rule) {
        if (edgeCount == 0 || clipX0 >= clipX1 || clipY0 >= clipY1) {
            return;
        }
        double minY = Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        double minX = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        for (int i = 0; i < edgeCount; i++) {
            int o = i * 5;
            minY = Math.min(minY, edges[o + 1]);
            maxY = Math.max(maxY, edges[o + 3]);
            minX = Math.min(minX, Math.min(edges[o], edges[o + 2]));
            maxX = Math.max(maxX, Math.max(edges[o], edges[o + 2]));
        }
        int y0 = Math.max(clipY0, (int) Math.floor(minY));
        int y1 = Math.min(clipY1, (int) Math.ceil(maxY));
        int x0 = Math.max(clipX0, (int) Math.floor(minX));
        int x1 = Math.min(clipX1, (int) Math.ceil(maxX) + 1);
        if (y0 >= y1 || x0 >= x1) {
            return;
        }
        int sub = antiAlias ? SUBSAMPLES : 1;
        double weight = 1.0 / sub;
        double[] cov = new double[x1 - x0 + 1];
        double[] xs = new double[16];
        int[] dirs = new int[16];
        double inv = ta * td - tb * tc;
        boolean invertible = Math.abs(inv) > 1e-300;
        for (int y = y0; y < y1; y++) {
            boolean any = false;
            for (int s = 0; s < sub; s++) {
                double sy = y + (s + 0.5) / sub;
                int n = 0;
                for (int i = 0; i < edgeCount; i++) {
                    int o = i * 5;
                    if (sy < edges[o + 1] || sy >= edges[o + 3]) {
                        continue;
                    }
                    double x = edges[o] + (sy - edges[o + 1]) * (edges[o + 2] - edges[o]) / (edges[o + 3] - edges[o + 1]);
                    if (n == xs.length) {
                        double[] nx = new double[n * 2];
                        System.arraycopy(xs, 0, nx, 0, n);
                        xs = nx;
                        int[] nd = new int[n * 2];
                        System.arraycopy(dirs, 0, nd, 0, n);
                        dirs = nd;
                    }
                    int k = n;
                    while (k > 0 && xs[k - 1] > x) {
                        xs[k] = xs[k - 1];
                        dirs[k] = dirs[k - 1];
                        k--;
                    }
                    xs[k] = x;
                    dirs[k] = (int) edges[o + 4];
                    n++;
                }
                int wind = 0;
                for (int i = 0; i + 1 < n; i++) {
                    wind += dirs[i];
                    boolean inside = rule == WIND_EVEN_ODD ? ((i + 1) & 1) == 1 : wind != 0;
                    if (inside) {
                        if (antiAlias) {
                            span(cov, x0, x1, xs[i], xs[i + 1], weight);
                        } else {
                            span(cov, x0, x1, Math.floor(xs[i] + 0.5), Math.floor(xs[i + 1] + 0.5), weight);
                        }
                        any = true;
                    }
                }
            }
            if (!any) {
                continue;
            }
            for (int x = x0; x < x1; x++) {
                double c = cov[x - x0];
                cov[x - x0] = 0;
                if (c <= 0) {
                    continue;
                }
                if (c > 1) {
                    c = 1;
                }
                int src;
                if (paint == null) {
                    src = color;
                } else if (invertible) {
                    double dx = x + 0.5 - te;
                    double dy = y + 0.5 - tf;
                    double ux = (td * dx - tc * dy) / inv;
                    double uy = (-tb * dx + ta * dy) / inv;
                    src = paint.getColor(ux, uy);
                } else {
                    continue;
                }
                blend(y * width + x, src, c * alpha);
            }
        }
    }

    private static void span(double[] cov, int x0, int x1, double a, double b, double w) {
        double xa = Math.max(a, x0);
        double xb = Math.min(b, x1);
        if (xb <= xa) {
            return;
        }
        int ia = (int) Math.floor(xa);
        int ib = (int) Math.floor(xb);
        if (ia == ib) {
            cov[ia - x0] += (xb - xa) * w;
            return;
        }
        cov[ia - x0] += (ia + 1 - xa) * w;
        for (int i = ia + 1; i < ib; i++) {
            cov[i - x0] += w;
        }
        if (ib < x1) {
            cov[ib - x0] += (xb - ib) * w;
        }
    }

    private void blend(int idx, int src, double cover) {
        double sa = (src >>> 24) / 255.0 * cover;
        if (sa <= 0) {
            return;
        }
        int dst = px[idx];
        double da = (dst >>> 24) / 255.0;
        double oa = sa + da * (1 - sa);
        if (oa <= 0) {
            px[idx] = 0;
            return;
        }
        double fd = da * (1 - sa);
        int r = (int) (((src >> 16 & 0xff) * sa + (dst >> 16 & 0xff) * fd) / oa + 0.5);
        int g = (int) (((src >> 8 & 0xff) * sa + (dst >> 8 & 0xff) * fd) / oa + 0.5);
        int b = (int) (((src & 0xff) * sa + (dst & 0xff) * fd) / oa + 0.5);
        int a = (int) (oa * 255 + 0.5);
        px[idx] = (a > 255 ? 255 : a) << 24 | (r > 255 ? 255 : r) << 16 | (g > 255 ? 255 : g) << 8 | (b > 255 ? 255 : b);
    }
}
