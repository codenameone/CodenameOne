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
package com.codename1.fxcompat.runtime;

import java.util.ArrayList;
import java.util.List;

import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.LinearGradientPaint;
import com.codename1.ui.MultipleGradientPaint;
import com.codename1.ui.Stroke;
import com.codename1.ui.Transform;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.util.MathUtil;

import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;

/// The one renderer behind shapes, the canvas, text, images, backgrounds
/// and borders. It draws logical geometry onto a Codename One graphics.
///
/// The renderer keeps its own affine matrix from logical coordinates to
/// the device pixels of the graphics. Geometry is mapped through that
/// matrix before it reaches Codename One, so translation, scale, rotation
/// and shear of paths are exact on every port, whether or not the port can
/// transform its graphics. Only text and images under a rotation or shear
/// need the port's own transform; where that is missing they are drawn
/// upright at the transformed position.
///
/// The matrix is `{a, b, c, d, tx, ty}`: `x' = a*x + c*y + tx`,
/// `y' = b*x + d*y + ty`.
public final class Renderer {

    /// Receives every drawing operation, for tests.
    public interface Trace {
        /// Called for each operation: `fill`, `stroke`, `text` or `image`,
        /// with the device bounds `{minX, minY, maxX, maxY}` of what was
        /// drawn, its paint and, for text, the string.
        void drawn(String operation, double[] deviceBounds, Paint paint, String text);
    }

    private static Trace trace;

    private final Graphics g;
    private double[] m;
    private final ArrayList<double[]> saved = new ArrayList<double[]>();
    private double opacity = 1;

    /// Creates a renderer whose logical origin is at a device position of
    /// the graphics and whose scale is the display's.
    public Renderer(Graphics g, double originXPixels, double originYPixels) {
        this.g = g;
        double s = Units.scale();
        this.m = new double[] {s, 0, 0, s, originXPixels, originYPixels};
    }

    /// Installs the receiver of drawing operations; `null` removes it.
    public static void setTrace(Trace receiver) {
        trace = receiver;
    }

    /// Returns the graphics drawn on.
    public Graphics graphics() {
        return g;
    }

    /// Returns a copy of the current matrix.
    public double[] matrix() {
        return new double[] {m[0], m[1], m[2], m[3], m[4], m[5]};
    }

    /// Replaces the current matrix.
    public void setMatrix(double[] matrix) {
        m = new double[] {matrix[0], matrix[1], matrix[2], matrix[3], matrix[4], matrix[5]};
    }

    /// Remembers the matrix and the opacity.
    public void save() {
        saved.add(new double[] {m[0], m[1], m[2], m[3], m[4], m[5], opacity});
    }

    /// Returns to the matrix and opacity of the matching [#save()].
    public void restore() {
        if (saved.isEmpty()) {
            return;
        }
        double[] s = saved.remove(saved.size() - 1);
        m = new double[] {s[0], s[1], s[2], s[3], s[4], s[5]};
        opacity = s[6];
    }

    /// Multiplies the opacity everything is drawn with.
    public void multiplyOpacity(double factor) {
        opacity *= factor < 0 ? 0 : (factor > 1 ? 1 : factor);
    }

    /// Returns the opacity everything is drawn with.
    public double opacity() {
        return opacity;
    }

    /// Applies an affine matrix to the coordinates that follow.
    public void concat(double a, double b, double c, double d, double tx, double ty) {
        double na = m[0] * a + m[2] * b;
        double nb = m[1] * a + m[3] * b;
        double nc = m[0] * c + m[2] * d;
        double nd = m[1] * c + m[3] * d;
        double ntx = m[0] * tx + m[2] * ty + m[4];
        double nty = m[1] * tx + m[3] * ty + m[5];
        m = new double[] {na, nb, nc, nd, ntx, nty};
    }

    /// Moves the origin.
    public void translate(double x, double y) {
        concat(1, 0, 0, 1, x, y);
    }

    /// Scales about the origin.
    public void scale(double x, double y) {
        concat(x, 0, 0, y, 0, 0);
    }

    /// Rotates clockwise about the origin, in degrees.
    public void rotate(double degrees) {
        double r = Math.toRadians(degrees);
        double cos = Math.cos(r);
        double sin = Math.sin(r);
        concat(cos, sin, -sin, cos, 0, 0);
    }

    private boolean axisAligned() {
        return m[1] == 0 && m[2] == 0;
    }

    private double uniformScale() {
        double det = Math.abs(m[0] * m[3] - m[1] * m[2]);
        return Math.sqrt(det);
    }

    private static int argb(Color c) {
        return c.cn1Argb();
    }

    private int alpha(int base, int colorAlpha) {
        int a = (int) Math.round(base * (colorAlpha / 255.0) * opacity);
        return a < 0 ? 0 : (a > 255 ? 255 : a);
    }

    private static double[] deviceBounds(FxPath path, double[] matrix) {
        double[] b = path.transformed(matrix).bounds();
        return b == null ? new double[] {0, 0, 0, 0} : b;
    }

    /// Fills a rectangle; the fast path for backgrounds.
    public void fillRect(double x, double y, double w, double h, Paint paint) {
        if (paint == null || w <= 0 || h <= 0) {
            return;
        }
        if (paint instanceof Color && axisAligned()) {
            Color c = (Color) paint;
            int x1 = (int) Math.round(m[0] * x + m[4]);
            int y1 = (int) Math.round(m[3] * y + m[5]);
            int x2 = (int) Math.round(m[0] * (x + w) + m[4]);
            int y2 = (int) Math.round(m[3] * (y + h) + m[5]);
            if (trace != null) {
                trace.drawn("fill", new double[] {Math.min(x1, x2), Math.min(y1, y2), Math.max(x1, x2),
                    Math.max(y1, y2)}, paint, null);
            }
            int old = g.getAlpha();
            int a = alpha(old, argb(c) >>> 24);
            if (a > 0) {
                g.setAlpha(a);
                g.setColor(argb(c) & 0xffffff);
                g.fillRect(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x2 - x1), Math.abs(y2 - y1));
                g.setAlpha(old);
            }
            return;
        }
        FxPath path = new FxPath();
        path.addRect(x, y, w, h);
        fill(path, paint, x, y, w, h);
    }

    /// Fills a path. The bounds are those a proportional gradient is
    /// measured against, in the path's coordinates.
    public void fill(FxPath path, Paint paint, double bx, double by, double bw, double bh) {
        if (paint == null || path == null || path.isEmpty()) {
            return;
        }
        if (trace != null) {
            trace.drawn("fill", deviceBounds(path, m), paint, null);
        }
        if (paint instanceof Color) {
            Color c = (Color) paint;
            int old = g.getAlpha();
            int a = alpha(old, argb(c) >>> 24);
            if (a == 0) {
                return;
            }
            g.setAlpha(a);
            g.setColor(argb(c) & 0xffffff);
            fillDevice(path);
            g.setAlpha(old);
        } else if (paint instanceof LinearGradient) {
            fillLinear(path, (LinearGradient) paint, bx, by, bw, bh);
        } else if (paint instanceof RadialGradient) {
            fillRadial(path, (RadialGradient) paint, bx, by, bw, bh);
        }
    }

    private void fillDevice(FxPath path) {
        if (g.isShapeSupported()) {
            boolean aa = g.isAntiAliased();
            g.setAntiAliased(true);
            g.fillShape(path.toDevice(m));
            g.setAntiAliased(aa);
            return;
        }
        // No path support on this port: fill each sub path as a polygon.
        FxPath flat = path.transformed(m).flatten(0.5);
        double[] pts = flat.points();
        int p = 0;
        int start = 0;
        for (int i = 0; i <= flat.commandCount(); i++) {
            byte command = i < flat.commandCount() ? flat.command(i) : FxPath.MOVE;
            if (command == FxPath.MOVE) {
                polygon(pts, start, p);
                start = p;
                if (i < flat.commandCount()) {
                    p += 2;
                }
            } else if (command == FxPath.LINE) {
                p += 2;
            }
        }
    }

    private void polygon(double[] pts, int from, int to) {
        int n = (to - from) / 2;
        if (n < 3) {
            return;
        }
        int[] xs = new int[n];
        int[] ys = new int[n];
        for (int i = 0; i < n; i++) {
            xs[i] = (int) Math.round(pts[from + i * 2]);
            ys[i] = (int) Math.round(pts[from + i * 2 + 1]);
        }
        g.fillPolygon(xs, ys, n);
    }

    private static MultipleGradientPaint.CycleMethod cycle(CycleMethod method) {
        if (method == CycleMethod.REFLECT) {
            return MultipleGradientPaint.CycleMethod.REFLECT;
        } else if (method == CycleMethod.REPEAT) {
            return MultipleGradientPaint.CycleMethod.REPEAT;
        }
        return MultipleGradientPaint.CycleMethod.NO_CYCLE;
    }

    private void fillLinear(FxPath path, LinearGradient lg, double bx, double by, double bw, double bh) {
        List<Stop> stops = lg.getStops();
        if (!g.isShapeSupported()) {
            fill(path, stops.get(stops.size() / 2).getColor(), bx, by, bw, bh);
            return;
        }
        double sx = lg.isProportional() ? bx + lg.getStartX() * bw : lg.getStartX();
        double sy = lg.isProportional() ? by + lg.getStartY() * bh : lg.getStartY();
        double ex = lg.isProportional() ? bx + lg.getEndX() * bw : lg.getEndX();
        double ey = lg.isProportional() ? by + lg.getEndY() * bh : lg.getEndY();
        float[] fractions = new float[stops.size()];
        int[] colors = new int[stops.size()];
        float last = -1;
        for (int i = 0; i < fractions.length; i++) {
            float f = (float) stops.get(i).getOffset();
            // Codename One needs strictly increasing fractions.
            if (f <= last) {
                f = Math.min(1f, last + 0.0001f);
            }
            fractions[i] = f;
            last = f;
            int c = argb(stops.get(i).getColor());
            colors[i] = (alpha(255, c >>> 24) << 24) | (c & 0xffffff);
        }
        LinearGradientPaint native0 = new LinearGradientPaint(m[0] * sx + m[2] * sy + m[4],
                m[1] * sx + m[3] * sy + m[5], m[0] * ex + m[2] * ey + m[4], m[1] * ex + m[3] * ey + m[5], fractions,
                colors, cycle(lg.getCycleMethod()), MultipleGradientPaint.ColorSpaceType.SRGB, null);
        int color = g.getColor();
        g.setColor(native0);
        fillDevice(path);
        g.setColor(color);
    }

    private static Color sample(List<Stop> stops, double t) {
        Stop previous = stops.get(0);
        for (int i = 1; i < stops.size(); i++) {
            Stop next = stops.get(i);
            if (t <= next.getOffset()) {
                double span = next.getOffset() - previous.getOffset();
                return span <= 0 ? next.getColor()
                        : previous.getColor().interpolate(next.getColor(), (t - previous.getOffset()) / span);
            }
            previous = next;
        }
        return previous.getColor();
    }

    private void fillRadial(FxPath path, RadialGradient rg, double bx, double by, double bw, double bh) {
        List<Stop> stops = rg.getStops();
        Color outer = stops.get(stops.size() - 1).getColor();
        if (!g.isShapeSupported() || !g.isShapeClipSupported()) {
            fill(path, outer, bx, by, bw, bh);
            return;
        }
        double cx = rg.isProportional() ? bx + rg.getCenterX() * bw : rg.getCenterX();
        double cy = rg.isProportional() ? by + rg.getCenterY() * bh : rg.getCenterY();
        double rx = rg.isProportional() ? rg.getRadius() * bw : rg.getRadius();
        double ry = rg.isProportional() ? rg.getRadius() * bh : rg.getRadius();
        // Everything outside the circle takes the last colour; inside,
        // rings from the outside in, each one covering the centre of the
        // one before.
        fill(path, outer, bx, by, bw, bh);
        int[] clip = g.getClip();
        g.pushClip();
        g.setClip(path.toDevice(m));
        g.clipRect(clip[0], clip[1], clip[2], clip[3]);
        int rings = (int) Math.max(8, Math.min(64, Math.max(rx, ry) * uniformScale() / 2));
        for (int i = rings; i >= 1; i--) {
            double t = (double) i / rings;
            FxPath ring = new FxPath();
            ring.addEllipse(cx, cy, rx * t, ry * t);
            Color c = sample(stops, t - 0.5 / rings);
            int old = g.getAlpha();
            g.setAlpha(alpha(old, argb(c) >>> 24));
            g.setColor(argb(c) & 0xffffff);
            g.fillShape(ring.toDevice(m));
            g.setAlpha(old);
        }
        g.popClip();
    }

    private static Color solid(Paint paint) {
        if (paint instanceof Color) {
            return (Color) paint;
        } else if (paint instanceof LinearGradient) {
            List<Stop> s = ((LinearGradient) paint).getStops();
            return s.get(s.size() / 2).getColor();
        } else if (paint instanceof RadialGradient) {
            List<Stop> s = ((RadialGradient) paint).getStops();
            return s.get(s.size() / 2).getColor();
        }
        return null;
    }

    /// Strokes the outline of a path. A gradient paint strokes with its
    /// middle colour. Dashes are cut out of the flattened outline, so they
    /// work on every port.
    public void stroke(FxPath path, Paint paint, double width, StrokeLineCap cap, StrokeLineJoin join,
            double miterLimit, double[] dashes, double dashOffset) {
        Color c = solid(paint);
        if (c == null || path == null || path.isEmpty() || !(width > 0)) {
            return;
        }
        FxPath outline = dashes == null || dashes.length == 0 ? path : path.dashed(dashes, dashOffset);
        if (outline.isEmpty()) {
            return;
        }
        if (trace != null) {
            double[] b = deviceBounds(outline, m);
            double half = width * uniformScale() / 2;
            trace.drawn("stroke", new double[] {b[0] - half, b[1] - half, b[2] + half, b[3] + half}, paint, null);
        }
        int old = g.getAlpha();
        int a = alpha(old, argb(c) >>> 24);
        if (a == 0) {
            return;
        }
        g.setAlpha(a);
        g.setColor(argb(c) & 0xffffff);
        float deviceWidth = (float) Math.max(1, width * uniformScale());
        if (g.isShapeSupported()) {
            int nativeCap = cap == StrokeLineCap.ROUND ? Stroke.CAP_ROUND
                    : (cap == StrokeLineCap.SQUARE ? Stroke.CAP_SQUARE : Stroke.CAP_BUTT);
            int nativeJoin = join == StrokeLineJoin.ROUND ? Stroke.JOIN_ROUND
                    : (join == StrokeLineJoin.BEVEL ? Stroke.JOIN_BEVEL : Stroke.JOIN_MITER);
            boolean aa = g.isAntiAliased();
            g.setAntiAliased(true);
            g.drawShape(outline.toDevice(m),
                    new Stroke(deviceWidth, nativeCap, nativeJoin, (float) Math.max(1, miterLimit)));
            g.setAntiAliased(aa);
        } else {
            strokeLines(outline, Math.round(deviceWidth));
        }
        g.setAlpha(old);
    }

    private void strokeLines(FxPath outline, int thickness) {
        FxPath flat = outline.transformed(m).flatten(0.5);
        double[] pts = flat.points();
        int p = 0;
        double sx = 0;
        double sy = 0;
        double cx = 0;
        double cy = 0;
        for (int i = 0; i < flat.commandCount(); i++) {
            byte command = flat.command(i);
            if (command == FxPath.MOVE) {
                sx = pts[p];
                sy = pts[p + 1];
                cx = sx;
                cy = sy;
                p += 2;
                continue;
            }
            double nx = command == FxPath.CLOSE ? sx : pts[p];
            double ny = command == FxPath.CLOSE ? sy : pts[p + 1];
            if (command == FxPath.LINE) {
                p += 2;
            }
            int x1 = (int) Math.round(cx);
            int y1 = (int) Math.round(cy);
            int x2 = (int) Math.round(nx);
            int y2 = (int) Math.round(ny);
            if (thickness > 1 && (x1 == x2 || y1 == y2)) {
                int half = thickness / 2;
                g.fillRect(Math.min(x1, x2) - (x1 == x2 ? half : 0), Math.min(y1, y2) - (y1 == y2 ? half : 0),
                        x1 == x2 ? thickness : Math.abs(x2 - x1), y1 == y2 ? thickness : Math.abs(y2 - y1));
            } else {
                g.drawLine(x1, y1, x2, y2);
            }
            cx = nx;
            cy = ny;
        }
    }

    /// Returns the native font that draws a JavaFX font under the current
    /// matrix: its size is converted, never scaled by a transform.
    public com.codename1.ui.Font deviceFont(Font font) {
        Font f = font == null ? Font.getDefault() : font;
        double k = uniformScale() / Units.scale();
        if (Math.abs(k - 1) < 0.001) {
            return Fonts.of(f);
        }
        return Fonts.create(f.getFamily(), f.cn1Weight().getWeight(),
                f.cn1Posture() == javafx.scene.text.FontPosture.ITALIC, f.getSize() * k);
    }

    /// Draws one line of text with its top left corner at a point.
    public void drawText(String text, double x, double y, Font font, Paint paint) {
        Color c = solid(paint);
        if (text == null || text.length() == 0 || c == null) {
            return;
        }
        com.codename1.ui.Font nativeFont = deviceFont(font);
        double px = m[0] * x + m[2] * y + m[4];
        double py = m[1] * x + m[3] * y + m[5];
        if (trace != null) {
            trace.drawn("text", new double[] {px, py, px + nativeFont.stringWidth(text), py + nativeFont.getHeight()},
                    paint, text);
        }
        int old = g.getAlpha();
        int a = alpha(old, argb(c) >>> 24);
        if (a == 0) {
            return;
        }
        g.setAlpha(a);
        g.setColor(argb(c) & 0xffffff);
        g.setFont(nativeFont);
        if (axisAligned() || !g.isTransformSupported()) {
            g.drawString(text, (int) Math.round(px), (int) Math.round(py));
        } else {
            Transform before = Transform.makeIdentity();
            g.getTransform(before);
            Transform t = before.copy();
            t.translate((float) px, (float) py);
            t.rotate((float) MathUtil.atan2(m[1], m[0]), 0, 0);
            g.setTransform(t);
            g.drawString(text, 0, 0);
            g.setTransform(before);
        }
        g.setAlpha(old);
    }

    /// Draws an image into a rectangle.
    public void drawImage(Image image, double x, double y, double w, double h) {
        if (image == null || w <= 0 || h <= 0) {
            return;
        }
        double px = m[0] * x + m[2] * y + m[4];
        double py = m[1] * x + m[3] * y + m[5];
        double k = uniformScale();
        int pw = (int) Math.max(1, Math.round(w * (axisAligned() ? Math.abs(m[0]) : k)));
        int ph = (int) Math.max(1, Math.round(h * (axisAligned() ? Math.abs(m[3]) : k)));
        if (trace != null) {
            trace.drawn("image", new double[] {px, py, px + pw, py + ph}, null, null);
        }
        int old = g.getAlpha();
        g.setAlpha(alpha(old, 255));
        if (axisAligned() || !g.isTransformSupported()) {
            g.drawImage(image, (int) Math.round(px), (int) Math.round(py), pw, ph);
        } else {
            Transform before = Transform.makeIdentity();
            g.getTransform(before);
            Transform t = before.copy();
            t.translate((float) px, (float) py);
            t.rotate((float) MathUtil.atan2(m[1], m[0]), 0, 0);
            g.setTransform(t);
            g.drawImage(image, 0, 0, pw, ph);
            g.setTransform(before);
        }
        g.setAlpha(old);
    }

    /// Returns the path of a rectangle with corner radii, the outline of
    /// a background or border.
    public static FxPath roundRect(double x, double y, double w, double h, double topLeft, double topRight,
            double bottomRight, double bottomLeft) {
        FxPath path = new FxPath();
        path.addRoundRect(x, y, w, h, topLeft, topRight, bottomRight, bottomLeft);
        return path;
    }

    /// Returns the Codename One path of a logical path under the current
    /// matrix, for clipping.
    public GeneralPath devicePath(FxPath path) {
        return path.toDevice(m);
    }
}
