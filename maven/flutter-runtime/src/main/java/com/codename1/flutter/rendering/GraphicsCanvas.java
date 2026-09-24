/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.rendering;

import com.codename1.flutter.Alignment;
import com.codename1.flutter.Canvas;
import com.codename1.flutter.Color;
import com.codename1.flutter.Gradient;
import com.codename1.flutter.Offset;
import com.codename1.flutter.Paint;
import com.codename1.flutter.PaintingStyle;
import com.codename1.flutter.Path;
import com.codename1.flutter.RRect;
import com.codename1.flutter.Radius;
import com.codename1.flutter.Rect;
import com.codename1.flutter.StrokeCap;
import com.codename1.flutter.StrokeJoin;
import com.codename1.ui.Graphics;
import com.codename1.ui.Stroke;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.Rectangle;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link Canvas} that draws onto a Codename One {@link Graphics} — the
 * backend behind every {@code CustomPainter} in a transpiled app.
 *
 * <h3>Coordinates</h3>
 * Flutter painters work in LOGICAL pixels; CN1 draws in device pixels. The
 * canvas therefore starts with a base transform of {@code scale(devicePixelRatio)}
 * composed with the component's origin, and the painter is handed a {@link Size}
 * in logical pixels — so a painter written against Flutter's coordinate system
 * lands at the right physical size on any density.
 *
 * <h3>Transforms</h3>
 * The transform stack is kept HERE, as a 2x3 affine matrix, and every
 * coordinate is mapped through it before reaching Graphics. That avoids
 * depending on CN1's optional {@code Transform} support, which is not available
 * on every port — a painter must not silently draw untransformed geometry
 * because a platform lacks a capability.
 */
public class GraphicsCanvas extends Canvas {

    /** Affine matrix [a c e; b d f] — the same layout as Flutter's Matrix4 2D subset. */
    private double a = 1, b = 0, c = 0, d = 1, e = 0, f = 0;

    private final List<double[]> stack = new ArrayList<double[]>();
    private final Graphics g;
    private final boolean shapes;

    public GraphicsCanvas(Graphics g, int originX, int originY, double devicePixelRatio) {
        this.g = g;
        this.shapes = g.isShapeSupported();
        translate(originX / (devicePixelRatio == 0 ? 1 : devicePixelRatio),
                originY / (devicePixelRatio == 0 ? 1 : devicePixelRatio));
        scale(devicePixelRatio, devicePixelRatio);
    }

    // ------------------------------------------------------------------
    // Transform stack
    // ------------------------------------------------------------------

    /**
     * Saves the transform AND the clip. Only the transform used to be saved, while
     * clipRect/clipRRect/clipPath narrow the Graphics clip itself, so a clip set between
     * save() and restore() stayed in force after the restore and cropped every later
     * drawing command of the painter.
     */
    @Override
    public void save() {
        stack.add(new double[] {a, b, c, d, e, f});
        g.pushClip();
    }

    @Override
    public void saveLayer(Rect bounds, Paint paint) {
        // No offscreen compositing: the layer's blend/opacity is not modelled,
        // but the transform must still nest correctly.
        save();
    }

    @Override
    public void restore() {
        if (stack.isEmpty()) {
            return;
        }
        double[] m = stack.remove(stack.size() - 1);
        a = m[0]; b = m[1]; c = m[2]; d = m[3]; e = m[4]; f = m[5];
        g.popClip();
    }

    @Override
    public void translate(double dx, double dy) {
        e += a * dx + c * dy;
        f += b * dx + d * dy;
    }

    @Override
    public void scale(double sx, double sy) {
        a *= sx; b *= sx;
        c *= sy; d *= sy;
    }

    @Override
    public void rotate(double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        double na = a * cos + c * sin;
        double nb = b * cos + d * sin;
        double nc = c * cos - a * sin;
        double nd = d * cos - b * sin;
        a = na; b = nb; c = nc; d = nd;
    }

    @Override
    public void skew(double sx, double sy) {
        double na = a + c * sy;
        double nb = b + d * sy;
        double nc = c + a * sx;
        double nd = d + b * sx;
        a = na; b = nb; c = nc; d = nd;
    }

    private float mapX(double x, double y) {
        return (float) (a * x + c * y + e);
    }

    private float mapY(double x, double y) {
        return (float) (b * x + d * y + f);
    }

    /** The transform's average scale — used for stroke widths and radii. */
    private double avgScale() {
        return (Math.sqrt(a * a + b * b) + Math.sqrt(c * c + d * d)) / 2;
    }

    // ------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------

    @Override
    public void drawRect(Rect rect, Paint paint) {
        GeneralPath p = new GeneralPath();
        appendRect(p, rect.left(), rect.top(), rect.right(), rect.bottom());
        emit(p, paint);
    }

    @Override
    public void drawRRect(RRect rrect, Paint paint) {
        GeneralPath p = new GeneralPath();
        appendRRect(p, rrect);
        emit(p, paint);
    }

    @Override
    public void drawCircle(Offset center, double radius, Paint paint) {
        drawOval(Rect.fromCircle(center, radius), paint);
    }

    @Override
    public void drawOval(Rect rect, Paint paint) {
        GeneralPath p = new GeneralPath();
        appendOval(p, rect.left(), rect.top(), rect.right(), rect.bottom());
        emit(p, paint);
    }

    @Override
    public void drawLine(Offset p1, Offset p2, Paint paint) {
        GeneralPath p = new GeneralPath();
        p.moveTo(mapX(p1.dx(), p1.dy()), mapY(p1.dx(), p1.dy()));
        p.lineTo(mapX(p2.dx(), p2.dy()), mapY(p2.dx(), p2.dy()));
        // a line has no interior: always stroked, whatever the paint style says
        strokeShape(p, paint);
    }

    @Override
    public void drawArc(Rect rect, double startAngle, double sweepAngle, boolean useCenter, Paint paint) {
        GeneralPath p = new GeneralPath();
        appendArc(p, rect, startAngle, sweepAngle, useCenter);
        emit(p, paint);
    }

    @Override
    public void drawPath(Path path, Paint paint) {
        GeneralPath p = toGeneralPath(path);
        emit(p, paint);
    }

    /**
     * Fills a triangle mesh — {@code Canvas.drawVertices}.
     *
     * <p>Codename One has no mesh primitive, so each triangle is filled as a
     * path. A mesh whose vertices all share one colour is filled as a single
     * polygon instead: it is the same picture without the hairline seams that
     * antialiased abutting triangles leave, and it is the shape the
     * 2D-transformations demo actually draws — one flat hexagon per board
     * point, as a triangle fan.
     *
     * <p>Per-vertex colour interpolation (Gouraud shading) is not modelled; a
     * multi-coloured triangle takes its first vertex's colour. The blend mode is
     * ignored — Codename One composites source-over.
     *
     * <p>Points go through {@link #mapX}/{@link #mapY} like every other
     * primitive here: a painter works in logical pixels and the canvas carries
     * the device-pixel-ratio scale, so a path built from raw coordinates comes
     * out at a third of its size on a 3x screen.
     *
     * <p>This used to draw nothing at all, so the demo's entire board — the only
     * content on that screen — was invisible against its background.
     */
    @Override
    public void drawVertices(Object vertices, Object blendMode, Paint paint) {
        if (!(vertices instanceof com.codename1.flutter.Vertices)) {
            return;
        }
        com.codename1.flutter.Vertices v = (com.codename1.flutter.Vertices) vertices;
        List<Offset> points = order(v);
        if (points.size() < 3) {
            return;
        }
        List<Color> colors = colorsOf(v, points.size());

        if (v.getMode() == com.codename1.flutter.VertexMode.triangleFan && uniform(colors)) {
            GeneralPath p = new GeneralPath();
            p.moveTo(mapX(points.get(0).dx(), points.get(0).dy()),
                    mapY(points.get(0).dx(), points.get(0).dy()));
            for (int i = 1; i < points.size(); i++) {
                p.lineTo(mapX(points.get(i).dx(), points.get(i).dy()),
                        mapY(points.get(i).dx(), points.get(i).dy()));
            }
            p.closePath();
            fillShape(p, meshPaint(paint, colors.isEmpty() ? null : colors.get(0)));
            return;
        }

        for (int t = 0; t + 2 < triangleLimit(v, points.size()); t += triangleStep(v)) {
            int a;
            int b;
            int c;
            switch (v.getMode()) {
                case triangleFan:
                    a = 0;
                    b = t + 1;
                    c = t + 2;
                    break;
                case triangleStrip:
                    a = t;
                    b = t + 1;
                    c = t + 2;
                    break;
                default:
                    a = t;
                    b = t + 1;
                    c = t + 2;
                    break;
            }
            if (c >= points.size()) {
                break;
            }
            GeneralPath p = new GeneralPath();
            p.moveTo(mapX(points.get(a).dx(), points.get(a).dy()),
                    mapY(points.get(a).dx(), points.get(a).dy()));
            p.lineTo(mapX(points.get(b).dx(), points.get(b).dy()),
                    mapY(points.get(b).dx(), points.get(b).dy()));
            p.lineTo(mapX(points.get(c).dx(), points.get(c).dy()),
                    mapY(points.get(c).dx(), points.get(c).dy()));
            p.closePath();
            fillShape(p, meshPaint(paint, colors.isEmpty() ? null : colors.get(a)));
        }
    }

    /** The mesh's positions, resolved through its index buffer when it has one. */
    private static List<Offset> order(com.codename1.flutter.Vertices v) {
        List<Offset> out = new ArrayList<Offset>();
        dart.core.DartList<Offset> positions = v.getPositions();
        if (positions == null) {
            return out;
        }
        dart.core.DartList<Integer> indices = v.getIndices();
        if (indices == null || indices.isEmpty()) {
            for (Offset o : positions) {
                out.add(o);
            }
            return out;
        }
        for (Integer i : indices) {
            if (i != null && i.intValue() >= 0 && i.intValue() < positions.size()) {
                out.add(positions.get(i.intValue()));
            }
        }
        return out;
    }

    private static List<Color> colorsOf(com.codename1.flutter.Vertices v, int count) {
        List<Color> out = new ArrayList<Color>();
        dart.core.DartList<Color> colors = v.getColors();
        if (colors == null) {
            return out;
        }
        for (int i = 0; i < count; i++) {
            out.add(i < colors.size() ? colors.get(i) : null);
        }
        return out;
    }

    private static boolean uniform(List<Color> colors) {
        if (colors.isEmpty()) {
            return true;
        }
        Color first = colors.get(0);
        for (Color c : colors) {
            if (c == null || first == null) {
                if (c != first) {
                    return false;
                }
            } else if (c.value() != first.value()) {
                return false;
            }
        }
        return true;
    }

    /** The paint to fill a triangle with: the mesh's vertex colour wins over the Paint's. */
    private static Paint meshPaint(Paint paint, Color vertexColor) {
        if (vertexColor == null) {
            return paint;
        }
        Paint p = new Paint();
        p.color(vertexColor);
        return p;
    }

    private static int triangleLimit(com.codename1.flutter.Vertices v, int count) {
        return count;
    }

    private static int triangleStep(com.codename1.flutter.Vertices v) {
        return v.getMode() == com.codename1.flutter.VertexMode.triangles ? 3 : 1;
    }

    @Override
    public void drawColor(Color color, Object blendMode) {
        if (color == null) {
            return;
        }
        int alpha = g.getAlpha();
        g.setColor(color.rgb());
        g.setAlpha(color.alpha());
        g.fillRect(g.getClipX(), g.getClipY(), g.getClipWidth(), g.getClipHeight());
        g.setAlpha(alpha);
    }

    @Override
    public void drawShadow(Path path, Color color, double elevation, boolean transparentOccluder) {
        // Approximated as a solid silhouette offset by the elevation; CN1's
        // real shadow machinery is bound to Style-driven borders, not to a
        // free-form path.
        if (color == null) {
            return;
        }
        save();
        translate(0, elevation);
        Paint p = new Paint();
        p.color(color);
        p.style(PaintingStyle.fill);
        drawPath(path, p);
        restore();
    }

    @Override
    public void clipRect(Rect rect) {
        g.clipRect((int) Math.floor(mapX(rect.left(), rect.top())),
                (int) Math.floor(mapY(rect.left(), rect.top())),
                (int) Math.ceil(rect.width() * avgScale()),
                (int) Math.ceil(rect.height() * avgScale()));
    }

    @Override
    public void clipRRect(RRect rrect) {
        // CN1 clips to rectangles; the corner rounding is dropped rather than
        // clipping nothing at all.
        clipRect(rrect.outerRect());
    }

    @Override
    public void clipPath(Path path) {
        if (shapes) {
            // NOTE: on iOS this does nothing -- see EffectRenderElement.paintRoundClipped
            // for the probe. A Canvas.clipPath is therefore inert there, and the subtree
            // paints unclipped. Left as it is because nothing in the gallery calls
            // clipPath, so there is no shape to check a fix against; when something does,
            // it needs the layer treatment the clip widgets now use.
            g.setClip(toGeneralPath(path));
        }
    }

    // ------------------------------------------------------------------
    // Path construction (device space)
    // ------------------------------------------------------------------

    private GeneralPath toGeneralPath(Path path) {
        GeneralPath p = new GeneralPath();
        double cx = 0;
        double cy = 0;
        // Whether the path has a current point, which decides how an arcTo with
        // forceMoveTo=false attaches: it JOINS the current point with a line, and only
        // starts a subpath of its own when there is nothing to join to.
        boolean hasCurrent = false;
        for (Path.Segment s : path.segments()) {
            double[] v = s.coords;
            if ("moveTo".equals(s.verb)) {
                p.moveTo(mapX(v[0], v[1]), mapY(v[0], v[1]));
                cx = v[0]; cy = v[1]; hasCurrent = true;
            } else if ("lineTo".equals(s.verb)) {
                p.lineTo(mapX(v[0], v[1]), mapY(v[0], v[1]));
                cx = v[0]; cy = v[1]; hasCurrent = true;
            } else if ("cubicTo".equals(s.verb)) {
                p.curveTo(mapX(v[0], v[1]), mapY(v[0], v[1]),
                        mapX(v[2], v[3]), mapY(v[2], v[3]),
                        mapX(v[4], v[5]), mapY(v[4], v[5]));
                cx = v[4]; cy = v[5]; hasCurrent = true;
            } else if ("quadraticBezierTo".equals(s.verb) || "conicTo".equals(s.verb)) {
                // a conic is approximated by its quadratic control polygon
                p.quadTo(mapX(v[0], v[1]), mapY(v[0], v[1]),
                        mapX(v[2], v[3]), mapY(v[2], v[3]));
                cx = v[2]; cy = v[3]; hasCurrent = true;
            } else if ("arcTo".equals(s.verb)) {
                Rect oval = Rect.fromLTRB(v[0], v[1], v[2], v[3]);
                boolean forceMoveTo = v[6] != 0;
                appendArc(p, oval, v[4], v[5], false, forceMoveTo || !hasCurrent);
                double end = v[4] + v[5];
                cx = oval.center().dx() + oval.width() / 2 * Math.cos(end);
                cy = oval.center().dy() + oval.height() / 2 * Math.sin(end);
                hasCurrent = true;
            } else if ("arcToPoint".equals(s.verb)) {
                double[] arc = arcToPoint(cx, cy, v[0], v[1], v[2], v[5] != 0, v[6] != 0,
                        ARC_SEGMENTS);
                if (arc == null) {
                    p.lineTo(mapX(v[0], v[1]), mapY(v[0], v[1]));
                } else {
                    for (int i = 0; i < arc.length; i += 2) {
                        p.lineTo(mapX(arc[i], arc[i + 1]), mapY(arc[i], arc[i + 1]));
                    }
                }
                cx = v[0]; cy = v[1]; hasCurrent = true;
            } else if ("addRect".equals(s.verb)) {
                appendRect(p, v[0], v[1], v[2], v[3]);
                hasCurrent = true;
            } else if ("addOval".equals(s.verb) || "addRRect".equals(s.verb)) {
                appendOval(p, v[0], v[1], v[2], v[3]);
                hasCurrent = true;
            } else if ("close".equals(s.verb)) {
                p.closePath();
            }
        }
        return p;
    }

    /// The points along a circular arc from (x0,y0) to (x1,y1), in the path's own
    /// coordinates, EXCLUDING the start and including the end.
    ///
    /// Flutter's {@code arcToPoint} names an arc by its END POINT and a radius, the way
    /// SVG does. Both callers used to replace it with a straight line -- the note here
    /// said that "keeps the outline closed", which it does, but a chord is not an arc: a
    /// bottom app bar's notch is two quadratics either side of one of these, so the curve
    /// went down, cut straight across, and came back up. It reads as a dimple with a bump
    /// in it, which is exactly what it is.
    ///
    /// Solved as SVG does (endpoint to centre parameterisation) for the circular case,
    /// which is the only one Flutter's own notch strategies use, and flattened: a filled
    /// path is flattened by the rasteriser anyway, and the ports flatten clip paths
    /// themselves.
    ///
    /// @param segments how many line segments to approximate with; 1 gives back the chord
    /// @return {x, y} pairs, or null when the arc is degenerate and the chord is right
    public static double[] arcToPoint(double x0, double y0, double x1, double y1,
            double radius, boolean largeArc, boolean clockwise, int segments) {
        double dx = (x0 - x1) / 2;
        double dy = (y0 - y1) / 2;
        double half = Math.sqrt(dx * dx + dy * dy);
        if (half <= 0 || segments < 2) {
            return null;
        }
        double r = Math.max(Math.abs(radius), half);
        // The centre lies off the chord's midpoint, perpendicular to it. Which side is
        // what largeArc and clockwise choose between.
        double coef = Math.sqrt(Math.max(0, (r * r - half * half))) / half;
        double sign = largeArc != clockwise ? 1 : -1;
        double cx = (x0 + x1) / 2 + sign * coef * dy;
        double cy = (y0 + y1) / 2 - sign * coef * dx;
        double a0 = Math.atan2(y0 - cy, x0 - cx);
        double a1 = Math.atan2(y1 - cy, x1 - cx);
        double sweep = a1 - a0;
        // Normalise the sweep into the direction asked for.
        if (clockwise && sweep < 0) {
            sweep += 2 * Math.PI;
        } else if (!clockwise && sweep > 0) {
            sweep -= 2 * Math.PI;
        }
        double[] out = new double[segments * 2];
        for (int i = 1; i <= segments; i++) {
            double a = a0 + sweep * i / segments;
            out[(i - 1) * 2] = cx + r * Math.cos(a);
            out[(i - 1) * 2 + 1] = cy + r * Math.sin(a);
        }
        return out;
    }

    /** Segments enough that an arc reads as a curve at any size a notch or badge uses. */
    public static final int ARC_SEGMENTS = 24;

    private void appendRect(GeneralPath p, double l, double t, double r, double b) {
        p.moveTo(mapX(l, t), mapY(l, t));
        p.lineTo(mapX(r, t), mapY(r, t));
        p.lineTo(mapX(r, b), mapY(r, b));
        p.lineTo(mapX(l, b), mapY(l, b));
        p.closePath();
    }

    private void appendRRect(GeneralPath p, RRect rr) {
        Rect r = rr.outerRect();
        double rad = radius(rr);
        if (rad <= 0) {
            appendRect(p, r.left(), r.top(), r.right(), r.bottom());
            return;
        }
        double l = r.left();
        double t = r.top();
        double ri = r.right();
        double bo = r.bottom();
        rad = Math.min(rad, Math.min(r.width(), r.height()) / 2);
        double k = rad * KAPPA;
        p.moveTo(mapX(l + rad, t), mapY(l + rad, t));
        p.lineTo(mapX(ri - rad, t), mapY(ri - rad, t));
        curve(p, ri - rad + k, t, ri, t + rad - k, ri, t + rad);
        p.lineTo(mapX(ri, bo - rad), mapY(ri, bo - rad));
        curve(p, ri, bo - rad + k, ri - rad + k, bo, ri - rad, bo);
        p.lineTo(mapX(l + rad, bo), mapY(l + rad, bo));
        curve(p, l + rad - k, bo, l, bo - rad + k, l, bo - rad);
        p.lineTo(mapX(l, t + rad), mapY(l, t + rad));
        curve(p, l, t + rad - k, l + rad - k, t, l + rad, t);
        p.closePath();
    }

    private static double radius(RRect rr) {
        Radius tl = rr.tlRadius();
        return tl == null ? 0 : tl.x();
    }

    /** Bezier constant for approximating a quarter circle. */
    private static final double KAPPA = 0.5522847498307933;

    private void appendOval(GeneralPath p, double l, double t, double r, double b) {
        double cx = (l + r) / 2;
        double cy = (t + b) / 2;
        double rx = (r - l) / 2;
        double ry = (b - t) / 2;
        double kx = rx * KAPPA;
        double ky = ry * KAPPA;
        p.moveTo(mapX(cx, t), mapY(cx, t));
        curve(p, cx + kx, t, r, cy - ky, r, cy);
        curve(p, r, cy + ky, cx + kx, b, cx, b);
        curve(p, cx - kx, b, l, cy + ky, l, cy);
        curve(p, l, cy - ky, cx - kx, t, cx, t);
        p.closePath();
    }

    private void curve(GeneralPath p, double x1, double y1, double x2, double y2, double x3, double y3) {
        p.curveTo(mapX(x1, y1), mapY(x1, y1), mapX(x2, y2), mapY(x2, y2), mapX(x3, y3), mapY(x3, y3));
    }

    /** Flattens the arc into line segments — enough for chart arcs and gauges. */
    private void appendArc(GeneralPath p, Rect rect, double startAngle, double sweepAngle, boolean useCenter) {
        appendArc(p, rect, startAngle, sweepAngle, useCenter, true);
    }

    /**
     * Appends an arc. When {@code startsNewSubpath} is false the arc is JOINED to whatever
     * the path already ends at, with a line to its start point - Flutter's
     * {@code arcTo(..., forceMoveTo: false)}. That join is what turns two opposing
     * half-circle arcs into one stadium outline instead of two separate discs.
     */
    private void appendArc(GeneralPath p, Rect rect, double startAngle, double sweepAngle,
                           boolean useCenter, boolean startsNewSubpath) {
        double cx = rect.center().dx();
        double cy = rect.center().dy();
        double rx = rect.width() / 2;
        double ry = rect.height() / 2;
        int steps = Math.max(2, (int) Math.ceil(Math.abs(sweepAngle) / (Math.PI / 36)));
        if (useCenter) {
            p.moveTo(mapX(cx, cy), mapY(cx, cy));
        }
        for (int i = 0; i <= steps; i++) {
            double ang = startAngle + sweepAngle * i / steps;
            double x = cx + rx * Math.cos(ang);
            double y = cy + ry * Math.sin(ang);
            if (i == 0 && !useCenter && startsNewSubpath) {
                p.moveTo(mapX(x, y), mapY(x, y));
            } else {
                p.lineTo(mapX(x, y), mapY(x, y));
            }
        }
        if (useCenter) {
            p.closePath();
        }
    }

    // ------------------------------------------------------------------
    // Paint application
    // ------------------------------------------------------------------

    private void emit(GeneralPath p, Paint paint) {
        if (paint != null && paint.style() == PaintingStyle.stroke) {
            strokeShape(p, paint);
        } else {
            fillShape(p, paint);
        }
    }

    private void applyColor(Paint paint) {
        Color col = paint == null ? null : paint.color();
        if (col == null) {
            g.setColor(0);
            g.setAlpha(255);
            return;
        }
        g.setColor(col.rgb());
        g.setAlpha(col.alpha());
    }

    private void fillShape(GeneralPath p, Paint paint) {
        if (fillWithGradient(p, paint)) {
            return;
        }
        int alpha = g.getAlpha();
        applyColor(paint);
        if (shapes) {
            g.fillShape(p);
        }
        g.setAlpha(alpha);
    }

    /**
     * Fills {@code p} with the Paint's gradient shader, if it has one, by clipping to the
     * shape and running Codename One's linear gradient across its bounding box. Returns
     * false when there is no gradient to paint, leaving the solid path to the caller.
     *
     * <p>Without this a shaded Paint carries no {@code color} at all and everything it
     * draws comes out the default black - which is what the gallery's settings icon did,
     * its pink and teal sticks both painting black.</p>
     */
    private boolean fillWithGradient(GeneralPath p, Paint paint) {
        if (paint == null || !(paint.shader() instanceof Gradient.GradientShader)) {
            return false;
        }
        Gradient gradient = ((Gradient.GradientShader) paint.shader()).gradient();
        int[] ramp = gradient.colorRamp();
        if (ramp.length == 0 || !shapes) {
            return false;
        }
        Rectangle bounds = p.getBounds();
        if (bounds.getWidth() <= 0 || bounds.getHeight() <= 0) {
            return false;
        }
        boolean vertical = isVertical(gradient);
        int alpha = g.getAlpha();
        int clipX = g.getClipX();
        int clipY = g.getClipY();
        int clipW = g.getClipWidth();
        int clipH = g.getClipHeight();
        try {
            if (p.isRectangle()) {
                // The ramp fills the whole path, so no clipping is needed at all.
                g.setAlpha((ramp[0] >>> 24) & 0xff);
                g.fillLinearGradient(ramp[0] & 0xffffff, ramp[ramp.length - 1] & 0xffffff,
                        bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(),
                        !vertical);
                return true;
            }
            // A SHAPE clip does not confine fillLinearGradient on every port. On iOS it
            // is simply ignored -- Graphics.isShapeClipSupported() answers true, the clip
            // is installed, and the ramp still fills the path's bounding BOX. The
            // gallery's settings icon is two stadium-shaped sticks painted that way, and
            // on the device they came out as square blocks while the desktop, where the
            // clip is honoured, drew them correctly. That is the shape of defect this
            // whole exercise keeps turning up: the sweep cannot see it.
            //
            // So do not ask a shape clip to do it. Fill the SHAPE once per band of the
            // ramp, with a RECTANGULAR clip confining each fill to its band -- a rect
            // clip and fillShape are honoured everywhere. The shape is then exact on
            // every port, and the ramp is quantised rather than absent.
            int span = vertical ? bounds.getHeight() : bounds.getWidth();
            int bands = Math.max(8, Math.min(span, 48));
            for (int i = 0; i < bands; i++) {
                int from = span * i / bands;
                int to = span * (i + 1) / bands;
                if (to <= from) {
                    continue;
                }
                int bandX = vertical ? bounds.getX() : bounds.getX() + from;
                int bandY = vertical ? bounds.getY() + from : bounds.getY();
                int bandW = vertical ? bounds.getWidth() : to - from;
                int bandH = vertical ? to - from : bounds.getHeight();
                int argb = rampAt(ramp, (i + 0.5) / bands);
                g.setClip(clipX, clipY, clipW, clipH);
                g.clipRect(bandX, bandY, bandW, bandH);
                if (g.getClipWidth() <= 0 || g.getClipHeight() <= 0) {
                    continue;
                }
                g.setAlpha((argb >>> 24) & 0xff);
                g.setColor(argb & 0xffffff);
                g.fillShape(p);
            }
        } finally {
            g.setClip(clipX, clipY, clipW, clipH);
            g.setAlpha(alpha);
        }
        return true;
    }

    /// The ramp's colour at {@code t} in 0..1, with the stops spread evenly.
    ///
    /// Even spacing is Flutter's own default when a gradient declares no {@code stops},
    /// and it is what the previous code assumed far more crudely: it read the first and
    /// last entries and ignored everything between them, so a three-stop gradient lost
    /// its middle colour entirely.
    static int rampAt(int[] ramp, double t) {
        if (ramp.length == 1) {
            return ramp[0];
        }
        if (t <= 0) {
            return ramp[0];
        }
        if (t >= 1) {
            return ramp[ramp.length - 1];
        }
        double pos = t * (ramp.length - 1);
        int i = (int) pos;
        if (i >= ramp.length - 1) {
            return ramp[ramp.length - 1];
        }
        double f = pos - i;
        int a = ramp[i];
        int b = ramp[i + 1];
        return (lerpChannel(a, b, f, 24) << 24) | (lerpChannel(a, b, f, 16) << 16)
                | (lerpChannel(a, b, f, 8) << 8) | lerpChannel(a, b, f, 0);
    }

    private static int lerpChannel(int a, int b, double t, int shift) {
        int ca = (a >>> shift) & 0xff;
        int cb = (b >>> shift) & 0xff;
        return (int) Math.round(ca + (cb - ca) * t) & 0xff;
    }

    /** Whether the gradient runs top-to-bottom rather than left-to-right. */
    private static boolean isVertical(Gradient gradient) {
        Object begin = gradient.getBegin();
        Object end = gradient.getEnd();
        if (!(begin instanceof Alignment) || !(end instanceof Alignment)) {
            return false;   // Flutter's default is centerLeft -> centerRight
        }
        double dx = Math.abs(((Alignment) end).x() - ((Alignment) begin).x());
        double dy = Math.abs(((Alignment) end).y() - ((Alignment) begin).y());
        return dy > dx;
    }

    private void strokeShape(GeneralPath p, Paint paint) {
        int alpha = g.getAlpha();
        applyColor(paint);
        if (shapes) {
            double w = paint == null || paint.strokeWidth() <= 0 ? 1 : paint.strokeWidth();
            g.drawShape(p, new Stroke((float) (w * avgScale()), capOf(paint), joinOf(paint),
                    (float) (paint == null ? 4 : paint.strokeMiterLimit())));
        }
        g.setAlpha(alpha);
    }

    private static int capOf(Paint paint) {
        StrokeCap c = paint == null ? null : paint.strokeCap();
        if (c == StrokeCap.round) {
            return Stroke.CAP_ROUND;
        }
        if (c == StrokeCap.square) {
            return Stroke.CAP_SQUARE;
        }
        return Stroke.CAP_BUTT;
    }

    private static int joinOf(Paint paint) {
        StrokeJoin j = paint == null ? null : paint.strokeJoin();
        if (j == StrokeJoin.bevel) {
            return Stroke.JOIN_BEVEL;
        }
        if (j == StrokeJoin.round) {
            return Stroke.JOIN_ROUND;
        }
        return Stroke.JOIN_MITER;
    }
}
