/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 */
package com.codename1.ui;

import com.codename1.ui.animations.AnimationTime;
import com.codename1.util.MathUtil;

/// Base class for SVG images emitted by the build-time transcoder
/// (`maven/svg-transcoder`).
///
/// A subclass is generated per source SVG file. The subclass overrides
/// [#paintSVG(Graphics, long)] to issue the actual drawing commands and
/// passes its intrinsic dimensions and viewBox to the constructor. This class
/// handles three concerns the generated code should not have to think about:
///
/// 1. **Viewport mapping** -- the viewBox is scaled into the destination
///    rectangle on every paint, so the same generated class can render at any
///    requested width/height without re-emitting code.
///
/// 2. **DPI-aware default sizing** -- the SVG's declared `width`/`height` are
///    interpreted as design pixels at [com.codename1.ui.CN1Constants#DENSITY_MEDIUM]
///    (the same convention CN1 uses for its multi-image density buckets) and
///    scaled to the device density so an icon that looks "right" on a desktop
///    simulator also looks right on a high-DPI handset. Override with
///    [#scaled(int, int)] or by passing explicit dimensions on construction.
///
/// 3. **Deterministic animation time** -- animation progress is read from
///    [AnimationTime#now()], so tests that pin the clock with
///    [AnimationTime#setTime(long)] capture predictable frames. The "first
///    paint" timestamp is captured the first time [#paintSVG] is invoked,
///    making animations start at `t = 0` from the user's perspective
///    regardless of when the image instance was constructed.
///
/// Generated SVGs are normally registered with a [com.codename1.ui.util.Resources]
/// object via the auto-generated `com.codename1.generated.svg.SVGRegistry` so
/// they appear under their original filename when calling
/// [com.codename1.ui.util.Resources#getImage(String)].
public abstract class GeneratedSVGImage extends Image {

    /// Sentinel value used by [#progress] when an animation declared
    /// `repeatCount="indefinite"`.
    public static final int REPEAT_INDEFINITE = -1;

    private final int intrinsicWidth;
    private final int intrinsicHeight;
    private final int sourceDensity;
    private final int width;
    private final int height;
    private final float viewBoxX;
    private final float viewBoxY;
    private final float viewBoxWidth;
    private final float viewBoxHeight;
    private final boolean animated;
    private long animationStartMs = -1L;

    /// Construct with intrinsic SVG dimensions and viewBox metadata. The
    /// rendered size defaults to the intrinsic dimensions scaled by the
    /// device's density (so SVGs designed at standard mdpi look correct on
    /// every screen). Use [#scaled(int, int)] for explicit pixel sizing.
    ///
    /// #### Parameters
    ///
    /// - `intrinsicWidth`: SVG-declared width in design pixels
    ///
    /// - `intrinsicHeight`: SVG-declared height in design pixels
    ///
    /// - `viewBoxX`: x origin of the viewBox in SVG user units
    ///
    /// - `viewBoxY`: y origin of the viewBox in SVG user units
    ///
    /// - `viewBoxWidth`: width of the viewBox; falls back to `intrinsicWidth` if `<= 0`
    ///
    /// - `viewBoxHeight`: height of the viewBox; falls back to `intrinsicHeight` if `<= 0`
    ///
    /// - `animated`: true if any SMIL animation was found inside the SVG
    protected GeneratedSVGImage(int intrinsicWidth, int intrinsicHeight,
                                float viewBoxX, float viewBoxY,
                                float viewBoxWidth, float viewBoxHeight,
                                boolean animated) {
        this(intrinsicWidth, intrinsicHeight,
                viewBoxX, viewBoxY, viewBoxWidth, viewBoxHeight,
                animated, CN1Constants.DENSITY_MEDIUM);
    }

    /// Construct with an explicit source density for the SVG's declared
    /// dimensions. This is the constructor the auto-generated `SVGRegistry`
    /// targets when the CSS for an SVG declared a `cn1-source-dpi` -- the
    /// runtime width/height then track that hint instead of assuming
    /// [com.codename1.ui.CN1Constants#DENSITY_MEDIUM].
    ///
    /// #### Parameters
    ///
    /// - `sourceDensity`: one of the `CN1Constants.DENSITY_*` constants,
    ///   describing the device class the SVG was designed for. A value of
    ///   `0` falls back to the SVG's intrinsic pixels (no scaling).
    protected GeneratedSVGImage(int intrinsicWidth, int intrinsicHeight,
                                float viewBoxX, float viewBoxY,
                                float viewBoxWidth, float viewBoxHeight,
                                boolean animated, int sourceDensity) {
        super(null);
        this.intrinsicWidth = intrinsicWidth;
        this.intrinsicHeight = intrinsicHeight;
        this.sourceDensity = sourceDensity;
        int density = readDeviceDensitySafely();
        this.width = scaleForDensity(intrinsicWidth, density, sourceDensity);
        this.height = scaleForDensity(intrinsicHeight, density, sourceDensity);
        this.viewBoxX = viewBoxX;
        this.viewBoxY = viewBoxY;
        this.viewBoxWidth = viewBoxWidth <= 0 ? intrinsicWidth : viewBoxWidth;
        this.viewBoxHeight = viewBoxHeight <= 0 ? intrinsicHeight : viewBoxHeight;
        this.animated = animated;
    }

    /// Construct with explicit absolute width/height -- expressed in device
    /// pixels here, but the auto-generated subclass takes them in millimeters
    /// and converts via [Display#convertToPixels(float)] so the dimensions
    /// carry across DPIs the same way `font-size: 3mm` does. This is the
    /// constructor the [SVGRegistry] uses when the CSS rule specified
    /// `cn1-svg-width` / `cn1-svg-height`, overriding any density-based
    /// sizing.
    ///
    /// #### Parameters
    ///
    /// - `explicitWidth`: rendered width in device pixels (`>= 1`)
    ///
    /// - `explicitHeight`: rendered height in device pixels (`>= 1`)
    protected GeneratedSVGImage(int intrinsicWidth, int intrinsicHeight,
                                float viewBoxX, float viewBoxY,
                                float viewBoxWidth, float viewBoxHeight,
                                boolean animated,
                                int explicitWidth, int explicitHeight) {
        super(null);
        this.intrinsicWidth = intrinsicWidth;
        this.intrinsicHeight = intrinsicHeight;
        this.sourceDensity = 0;
        this.width = Math.max(1, explicitWidth);
        this.height = Math.max(1, explicitHeight);
        this.viewBoxX = viewBoxX;
        this.viewBoxY = viewBoxY;
        this.viewBoxWidth = viewBoxWidth <= 0 ? intrinsicWidth : viewBoxWidth;
        this.viewBoxHeight = viewBoxHeight <= 0 ? intrinsicHeight : viewBoxHeight;
        this.animated = animated;
    }

    /// Convert a length in millimeters to device pixels using the current
    /// [Display] DPI. Provided as a static helper for the generated subclass
    /// constructors that accept mm-typed dimensions. Falls back to treating
    /// the input as a literal pixel count when [Display] is not initialized
    /// (e.g. during unit tests that construct an image before
    /// `Display.init()` has run).
    public static int mmToPixels(float mm) {
        try {
            return Math.max(1, Display.getInstance().convertToPixels(mm));
        } catch (Throwable t) {
            return Math.max(1, (int) Math.round(mm));
        }
    }

    @Override
    public final int getWidth() {
        return width;
    }

    @Override
    public final int getHeight() {
        return height;
    }

    @Override
    public final boolean isAnimation() {
        return animated;
    }

    /// Returning `true` requests a repaint on the next animation tick so the
    /// embedding component re-reads the SMIL clock. The animation state itself
    /// is re-derived from [AnimationTime#now()] on each paint, so there is
    /// nothing to advance imperatively.
    @Override
    public final boolean animate() {
        return animated;
    }

    /// Generated implementations render the SVG content using the supplied
    /// graphics context. `elapsedMs` is the number of milliseconds since the
    /// first paint of this image instance, measured against
    /// [AnimationTime#now()] so test code can pin the clock.
    ///
    /// #### Parameters
    ///
    /// - `g`: the graphics context, already transformed so SVG user-space
    ///   coordinates map onto the destination rectangle
    ///
    /// - `elapsedMs`: animation time in milliseconds, `0` for non-animated SVGs
    protected abstract void paintSVG(Graphics g, long elapsedMs);

    @Override
    protected final void drawImage(Graphics g, Object nativeGraphics, int x, int y) {
        drawImage(g, nativeGraphics, x, y, width, height);
    }

    @Override
    protected final void drawImage(Graphics g, Object nativeGraphics,
                                   int x, int y, int w, int h) {
        if (!g.isShapeSupported()) {
            return;
        }
        long elapsed = currentAnimationOffsetMs();

        Transform saved = null;
        try {
            saved = g.getTransform();
        } catch (Throwable ignored) {
            saved = null;
        }
        int savedColor = g.getColor();
        int savedAlpha = g.getAlpha();
        boolean savedAA = g.isAntiAliased();
        try {
            // Anti-alias every SVG render. Vector shapes drawn without AA
            // look stair-stepped on every port we ship, and the perf cost
            // on modern hardware is negligible.
            g.setAntiAliased(true);
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
            g.setAntiAliased(savedAA);
        }
    }

    /// Returns a lightweight view onto this image that reports `width` /
    /// `height` from [#getWidth] / [#getHeight] but reuses the underlying
    /// rendering. The returned image's animation clock is shared with the
    /// source so progress is consistent across both views.
    @Override
    public final Image scaled(int width, int height) {
        return new SVGScaledView(this, width, height);
    }

    /// Reset the per-instance animation start so the next paint begins at
    /// `t = 0`. Tests typically prefer [AnimationTime#setTime(long)] instead,
    /// which controls every animation in the VM at once.
    public final void resetAnimation() {
        animationStartMs = -1L;
    }

    /// The intrinsic SVG width before DPI scaling -- useful for tests or
    /// callers that want to apply a different sizing heuristic.
    public final int getIntrinsicWidth() {
        return intrinsicWidth;
    }

    /// The intrinsic SVG height before DPI scaling.
    public final int getIntrinsicHeight() {
        return intrinsicHeight;
    }

    /// Compute the animation offset that the next [#paintSVG] call would see,
    /// capturing the per-instance start timestamp from [AnimationTime#now()]
    /// on first call. Package-visible so tests can exercise the clock without
    /// needing a Graphics context with shape support; production code reaches
    /// this through [#drawImage(Graphics, Object, int, int, int, int)].
    long currentAnimationOffsetMs() {
        if (!animated) {
            return 0L;
        }
        long now = AnimationTime.now();
        if (animationStartMs < 0L) {
            animationStartMs = now;
        }
        long elapsed = now - animationStartMs;
        return elapsed < 0L ? 0L : elapsed;
    }

    private static int readDeviceDensitySafely() {
        // Display may not be fully initialized when the image is constructed
        // (e.g. during static-init of an SVGRegistry that's loaded before
        // Display.init in a unit test). Fall back to DENSITY_MEDIUM, which is
        // the design-time default and produces 1:1 pixels for the intrinsic
        // dimensions.
        try {
            return Display.getInstance().getDeviceDensity();
        } catch (Throwable t) {
            return CN1Constants.DENSITY_MEDIUM;
        }
    }

    /// Returns the source density the SVG was designed for, in
    /// `CN1Constants.DENSITY_*` units. `0` means "no scaling, use intrinsic
    /// pixels". CSS can override the default via `cn1-source-dpi:`.
    public final int getSourceDensity() {
        return sourceDensity;
    }

    private static int scaleForDensity(int designPixels, int density, int sourceDensity) {
        if (designPixels <= 0) {
            return 1;
        }
        if (sourceDensity <= 0 || density <= 0 || density == sourceDensity) {
            return designPixels;
        }
        int scaled = (int) Math.floor(((double) designPixels * density) / sourceDensity + 0.5);
        return scaled < 1 ? 1 : scaled;
    }

    // ---------------------------------------------------------------------
    // SMIL helpers -- referenced by generated code; keep signatures stable.
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
    /// cubic Beziers -- a single quadrant per Bezier -- for accuracy.
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

        // F.6.5.1 -- compute (x1', y1')
        double dx2 = (x1 - x2) / 2.0;
        double dy2 = (y1 - y2) / 2.0;
        double x1p =  cosPhi * dx2 + sinPhi * dy2;
        double y1p = -sinPhi * dx2 + cosPhi * dy2;

        // F.6.6.2 -- ensure radii are large enough
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

        // F.6.5.2 -- compute (cx', cy')
        double sign = (largeArc == sweep) ? -1.0 : 1.0;
        double sq = (rx2 * ry2 - rx2 * y1p2 - ry2 * x1p2) / (rx2 * y1p2 + ry2 * x1p2);
        if (sq < 0.0) {
            sq = 0.0;
        }
        double coef = sign * Math.sqrt(sq);
        double cxp = coef * (arx * y1p / ary);
        double cyp = coef * -(ary * x1p / arx);

        // F.6.5.3 -- compute (cx, cy)
        double sx2 = (x1 + x2) / 2.0;
        double sy2 = (y1 + y2) / 2.0;
        double cx = sx2 + (cosPhi * cxp - sinPhi * cyp);
        double cy = sy2 + (sinPhi * cxp + cosPhi * cyp);

        // F.6.5.4 -- start angle and sweep
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
            double ex = cx + arx * (cosPhi * cosTheta2 - sinPhi * sinTheta2);
            double ey = cy + ary * (sinPhi * cosTheta2 + cosPhi * sinTheta2);
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
        // CLDC's java.lang.Math has no acos; use the CN1 helper.
        double a = MathUtil.acos(cos);
        if ((ux * vy - uy * vx) < 0.0) {
            a = -a;
        }
        return a;
    }
}
