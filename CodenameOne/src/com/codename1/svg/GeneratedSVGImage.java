package com.codename1.svg;

import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Transform;

/// Base class for SVG images emitted by the build-time transcoder.
///
/// A subclass is generated per source SVG file. The subclass overrides
/// [#paintSVG(Graphics, long)] to issue the actual drawing commands and
/// passes its intrinsic size and viewBox to the constructor. This class
/// handles viewport mapping (scaling the viewBox into the requested
/// destination rectangle) and animation time tracking so generated code
/// stays declarative.
///
/// The generated classes are normally registered with a [com.codename1.ui.util.Resources]
/// object via the auto-generated `com.codename1.generated.svg.SVGRegistry`
/// so they appear under their original filename when calling
/// [com.codename1.ui.util.Resources#getImage(String)].
public abstract class GeneratedSVGImage extends Image {

    /// Sentinel value used by [#progress] when an animation declared
    /// `repeatCount="indefinite"`.
    public static final int REPEAT_INDEFINITE = -1;

    private final int width;
    private final int height;
    private final float viewBoxX;
    private final float viewBoxY;
    private final float viewBoxWidth;
    private final float viewBoxHeight;
    private final boolean animated;
    private long animationStartMs = -1L;
    private long animationOverrideMs = -1L;

    /// Construct a generated SVG image with intrinsic size and viewBox metadata.
    ///
    /// #### Parameters
    ///
    /// - `width`: intrinsic pixel width (defaults to the viewBox width)
    ///
    /// - `height`: intrinsic pixel height
    ///
    /// - `viewBoxX`: x origin of the viewBox in SVG user units
    ///
    /// - `viewBoxY`: y origin of the viewBox in SVG user units
    ///
    /// - `viewBoxWidth`: width of the viewBox; falls back to `width` if `<= 0`
    ///
    /// - `viewBoxHeight`: height of the viewBox; falls back to `height` if `<= 0`
    ///
    /// - `animated`: true if any SMIL animation was found inside the SVG
    protected GeneratedSVGImage(int width, int height,
                                float viewBoxX, float viewBoxY,
                                float viewBoxWidth, float viewBoxHeight,
                                boolean animated) {
        super(null);
        this.width = width;
        this.height = height;
        this.viewBoxX = viewBoxX;
        this.viewBoxY = viewBoxY;
        this.viewBoxWidth = viewBoxWidth <= 0 ? width : viewBoxWidth;
        this.viewBoxHeight = viewBoxHeight <= 0 ? height : viewBoxHeight;
        this.animated = animated;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public boolean isAnimation() {
        return animated;
    }

    /// Always returns whether this image is animated; the animation state is
    /// re-derived from wall-clock time on each paint, so there is nothing to
    /// advance here. Returning `true` requests that the embedding component
    /// be repainted.
    @Override
    public boolean animate() {
        return animated;
    }

    /// Generated implementations render the SVG content using the supplied
    /// graphics context. `elapsedMs` is the number of milliseconds since the
    /// first paint of this image (or since the most recent [#resetAnimation]).
    ///
    /// #### Parameters
    ///
    /// - `g`: the graphics context, already transformed so SVG user-space
    ///   coordinates map onto the destination rectangle
    ///
    /// - `elapsedMs`: animation time in milliseconds, `0` for non-animated SVGs
    protected abstract void paintSVG(Graphics g, long elapsedMs);

    @Override
    protected void drawImage(Graphics g, Object nativeGraphics, int x, int y) {
        drawImage(g, nativeGraphics, x, y, width, height);
    }

    @Override
    protected void drawImage(Graphics g, Object nativeGraphics, int x, int y, int w, int h) {
        if (!g.isShapeSupported()) {
            return;
        }
        long elapsed = 0L;
        if (animated) {
            if (animationOverrideMs >= 0L) {
                elapsed = animationOverrideMs;
            } else {
                if (animationStartMs < 0L) {
                    animationStartMs = System.currentTimeMillis();
                }
                elapsed = System.currentTimeMillis() - animationStartMs;
            }
        }

        Transform saved = null;
        try {
            saved = g.getTransform();
        } catch (Throwable ignored) {
            saved = null;
        }
        int savedColor = g.getColor();
        int savedAlpha = g.getAlpha();
        try {
            float sx = (float) w / viewBoxWidth;
            float sy = (float) h / viewBoxHeight;
            Transform t;
            if (saved != null) {
                t = saved.copy();
            } else {
                t = Transform.makeIdentity();
            }
            t.translate((float) x, (float) y);
            t.scale(sx, sy);
            t.translate(-viewBoxX, -viewBoxY);
            g.setTransform(t);
            paintSVG(g, elapsed);
        } finally {
            if (saved != null) {
                g.setTransform(saved);
            } else {
                g.setTransform(Transform.makeIdentity());
            }
            g.setColor(savedColor);
            g.setAlpha(savedAlpha);
        }
    }

    /// We render to any requested size on the fly, so a "scaled" instance is
    /// the same instance. Returning `this` avoids allocating a wrapper for
    /// every layout pass.
    @Override
    public Image scaled(int width, int height) {
        return this;
    }

    /// Reset the animation clock so the next paint begins at `t = 0`. Useful
    /// for screenshot tests that want a deterministic frame.
    public void resetAnimation() {
        animationStartMs = -1L;
    }

    /// Pin the animation clock to a fixed elapsed time. Pass `-1` to release
    /// the override and resume wall-clock time. Used by screenshot tests to
    /// capture specific frames.
    public void setAnimationTimeMillis(long elapsedMs) {
        this.animationOverrideMs = elapsedMs;
    }

    // ---------------------------------------------------------------------
    // SMIL helpers — referenced by generated code; keep signatures stable.
    // ---------------------------------------------------------------------

    /// Compute the active progress through an animation cycle, in the range
    /// `[0, 1]`. Honors begin offsets, repeat counts and the SMIL fill="freeze"
    /// behavior. Generated code calls this per animated attribute per paint.
    public static float progress(long elapsedMs, long beginMs, long durMs,
                                 int repeatCount, boolean freeze) {
        if (durMs <= 0L) {
            return freeze ? 1f : 0f;
        }
        long t = elapsedMs - beginMs;
        if (t < 0L) {
            return 0f;
        }
        if (repeatCount == REPEAT_INDEFINITE) {
            long cycle = t % durMs;
            return (float) cycle / (float) durMs;
        }
        long total = durMs * (long) repeatCount;
        if (t >= total) {
            return freeze ? 1f : 0f;
        }
        long cycle = t % durMs;
        return (float) cycle / (float) durMs;
    }

    public static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }

    /// Lerp between two ARGB colors. Each channel is linearly interpolated.
    public static int lerpColor(int fromArgb, int toArgb, float t) {
        int fa = (fromArgb >>> 24) & 0xFF;
        int fr = (fromArgb >>> 16) & 0xFF;
        int fg = (fromArgb >>> 8) & 0xFF;
        int fb = fromArgb & 0xFF;
        int ta = (toArgb >>> 24) & 0xFF;
        int tr = (toArgb >>> 16) & 0xFF;
        int tg = (toArgb >>> 8) & 0xFF;
        int tb = toArgb & 0xFF;
        int a = round(fa + (ta - fa) * t);
        int r = round(fr + (tr - fr) * t);
        int g = round(fg + (tg - fg) * t);
        int b = round(fb + (tb - fb) * t);
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    /// Multi-stop floating point lerp. Stops are evenly spaced in `[0, 1]`.
    public static float lerpValues(float[] values, float t) {
        if (values == null || values.length == 0) {
            return 0f;
        }
        if (values.length == 1) {
            return values[0];
        }
        if (t <= 0f) {
            return values[0];
        }
        if (t >= 1f) {
            return values[values.length - 1];
        }
        float seg = 1f / (values.length - 1);
        int i = (int) Math.floor(t / seg);
        if (i >= values.length - 1) {
            i = values.length - 2;
        }
        float local = (t - i * seg) / seg;
        return values[i] + (values[i + 1] - values[i]) * local;
    }

    private static int round(float v) {
        int r = (int) (v + 0.5f);
        if (r < 0) {
            return 0;
        }
        if (r > 255) {
            return 255;
        }
        return r;
    }

    /// Append an SVG elliptical arc segment to the given path using the
    /// endpoint parameterization defined by the SVG 1.1 spec (appendix F.6).
    /// The current point of `p` is treated as the arc's start; on return the
    /// current point is the end of the arc. Decomposes into up to four
    /// cubic Beziers — a single quadrant per Bezier — for accuracy.
    public static void svgArc(com.codename1.ui.geom.GeneralPath p,
                              float x1, float y1,
                              float rx, float ry,
                              float xAxisRotationDeg,
                              boolean largeArc, boolean sweep,
                              float x2, float y2) {
        if (rx == 0f || ry == 0f) {
            p.lineTo(x2, y2);
            return;
        }
        float arx = Math.abs(rx);
        float ary = Math.abs(ry);
        double phi = Math.toRadians(xAxisRotationDeg);
        double cosPhi = Math.cos(phi);
        double sinPhi = Math.sin(phi);

        // F.6.5.1 — compute (x1', y1')
        double dx2 = (x1 - x2) / 2.0;
        double dy2 = (y1 - y2) / 2.0;
        double x1p =  cosPhi * dx2 + sinPhi * dy2;
        double y1p = -sinPhi * dx2 + cosPhi * dy2;

        // F.6.6.2 — ensure radii are large enough
        double rx2 = arx * arx;
        double ry2 = ary * ary;
        double x1p2 = x1p * x1p;
        double y1p2 = y1p * y1p;
        double radiiCheck = x1p2 / rx2 + y1p2 / ry2;
        if (radiiCheck > 1.0) {
            double s = Math.sqrt(radiiCheck);
            arx = (float) (s * arx);
            ary = (float) (s * ary);
            rx2 = arx * arx;
            ry2 = ary * ary;
        }

        // F.6.5.2 — compute (cx', cy')
        double sign = (largeArc == sweep) ? -1.0 : 1.0;
        double sq = (rx2 * ry2 - rx2 * y1p2 - ry2 * x1p2) / (rx2 * y1p2 + ry2 * x1p2);
        if (sq < 0.0) {
            sq = 0.0;
        }
        double coef = sign * Math.sqrt(sq);
        double cxp = coef * (arx * y1p / ary);
        double cyp = coef * -(ary * x1p / arx);

        // F.6.5.3 — compute (cx, cy)
        double sx2 = (x1 + x2) / 2.0;
        double sy2 = (y1 + y2) / 2.0;
        double cx = sx2 + (cosPhi * cxp - sinPhi * cyp);
        double cy = sy2 + (sinPhi * cxp + cosPhi * cyp);

        // F.6.5.4 — start angle and sweep
        double ux = (x1p - cxp) / arx;
        double uy = (y1p - cyp) / ary;
        double vx = (-x1p - cxp) / arx;
        double vy = (-y1p - cyp) / ary;
        double theta1 = vectorAngle(1.0, 0.0, ux, uy);
        double deltaTheta = vectorAngle(ux, uy, vx, vy);
        if (!sweep && deltaTheta > 0.0) {
            deltaTheta -= 2.0 * Math.PI;
        } else if (sweep && deltaTheta < 0.0) {
            deltaTheta += 2.0 * Math.PI;
        }

        // Split into segments small enough that the cubic approximation stays accurate.
        int segments = (int) Math.ceil(Math.abs(deltaTheta) / (Math.PI / 2.0));
        if (segments < 1) {
            segments = 1;
        }
        double dt = deltaTheta / segments;
        double t = (4.0 / 3.0) * Math.tan(dt / 4.0);

        double cosTheta1 = Math.cos(theta1);
        double sinTheta1 = Math.sin(theta1);
        double px = x1;
        double py = y1;
        for (int i = 0; i < segments; i++) {
            double theta2 = theta1 + dt;
            double cosTheta2 = Math.cos(theta2);
            double sinTheta2 = Math.sin(theta2);
            // end of this segment in (cx, cy, arx, ary, phi) frame
            double ex = cx + arx * (cosPhi * cosTheta2 - sinPhi * sinTheta2);
            double ey = cy + ary * (sinPhi * cosTheta2 + cosPhi * sinTheta2);
            // control point offsets in ellipse-local frame, then rotate
            double dx1L = -arx * sinTheta1;
            double dy1L =  ary * cosTheta1;
            double dx2L = -arx * sinTheta2;
            double dy2L =  ary * cosTheta2;
            double c1xL = px + t * (cosPhi * dx1L - sinPhi * dy1L);
            double c1yL = py + t * (sinPhi * dx1L + cosPhi * dy1L);
            double c2xL = ex - t * (cosPhi * dx2L - sinPhi * dy2L);
            double c2yL = ey - t * (sinPhi * dx2L + cosPhi * dy2L);
            p.curveTo((float) c1xL, (float) c1yL,
                      (float) c2xL, (float) c2yL,
                      (float) ex, (float) ey);
            theta1 = theta2;
            cosTheta1 = cosTheta2;
            sinTheta1 = sinTheta2;
            px = ex;
            py = ey;
        }
    }

    private static double vectorAngle(double ux, double uy, double vx, double vy) {
        double dot = ux * vx + uy * vy;
        double len = Math.sqrt((ux * ux + uy * uy) * (vx * vx + vy * vy));
        double cos = dot / len;
        if (cos < -1.0) {
            cos = -1.0;
        }
        if (cos > 1.0) {
            cos = 1.0;
        }
        double a = Math.acos(cos);
        if ((ux * vy - uy * vx) < 0.0) {
            a = -a;
        }
        return a;
    }
}
