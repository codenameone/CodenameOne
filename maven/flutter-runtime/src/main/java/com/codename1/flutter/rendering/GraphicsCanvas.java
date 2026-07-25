package com.codename1.flutter.rendering;

import com.codename1.flutter.Canvas;
import com.codename1.flutter.Color;
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

    @Override
    public void save() {
        stack.add(new double[] {a, b, c, d, e, f});
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
        for (Path.Segment s : path.segments()) {
            double[] v = s.coords;
            if ("moveTo".equals(s.verb)) {
                p.moveTo(mapX(v[0], v[1]), mapY(v[0], v[1]));
                cx = v[0]; cy = v[1];
            } else if ("lineTo".equals(s.verb)) {
                p.lineTo(mapX(v[0], v[1]), mapY(v[0], v[1]));
                cx = v[0]; cy = v[1];
            } else if ("cubicTo".equals(s.verb)) {
                p.curveTo(mapX(v[0], v[1]), mapY(v[0], v[1]),
                        mapX(v[2], v[3]), mapY(v[2], v[3]),
                        mapX(v[4], v[5]), mapY(v[4], v[5]));
                cx = v[4]; cy = v[5];
            } else if ("quadraticBezierTo".equals(s.verb) || "conicTo".equals(s.verb)) {
                // a conic is approximated by its quadratic control polygon
                p.quadTo(mapX(v[0], v[1]), mapY(v[0], v[1]),
                        mapX(v[2], v[3]), mapY(v[2], v[3]));
                cx = v[2]; cy = v[3];
            } else if ("arcTo".equals(s.verb)) {
                appendArc(p, Rect.fromLTRB(v[0], v[1], v[2], v[3]), v[4], v[5], false);
            } else if ("arcToPoint".equals(s.verb)) {
                // without full elliptical-arc solving, a straight segment to
                // the arc's end point keeps the outline closed
                p.lineTo(mapX(v[0], v[1]), mapY(v[0], v[1]));
                cx = v[0]; cy = v[1];
            } else if ("addRect".equals(s.verb)) {
                appendRect(p, v[0], v[1], v[2], v[3]);
            } else if ("addOval".equals(s.verb) || "addRRect".equals(s.verb)) {
                appendOval(p, v[0], v[1], v[2], v[3]);
            } else if ("close".equals(s.verb)) {
                p.closePath();
            }
        }
        return p;
    }

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
            if (i == 0 && !useCenter) {
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
        int alpha = g.getAlpha();
        applyColor(paint);
        if (shapes) {
            g.fillShape(p);
        }
        g.setAlpha(alpha);
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
