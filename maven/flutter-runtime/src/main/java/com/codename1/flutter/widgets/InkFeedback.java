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
package com.codename1.flutter.widgets;

import com.codename1.flutter.material.InkResponse;
import com.codename1.flutter.rendering.Dp;
import com.codename1.ui.Component;
import com.codename1.ui.Graphics;
import com.codename1.ui.animations.Animation;

/**
 * Material touch feedback for one tap area: the splash that expands from the touch point
 * and the press highlight underneath it — Flutter's {@code InkResponse} ink.
 *
 * <p>Two overlapping effects, because Material uses both and they read very differently
 * with only one of them: the HIGHLIGHT is a flat wash over the whole target that fades in
 * while the finger is down and says "this is pressed"; the SPLASH is a circle growing from
 * the exact touch point that says "this is where you touched". Neither alone feels right.</p>
 *
 * <p>Timings follow Flutter's InkRipple: the splash keeps growing while the finger is held
 * and only fades once the press is confirmed, so a long press does not leave a
 * half-finished circle sitting on screen.</p>
 *
 * <p>It rides Codename One's animation loop rather than an
 * {@code AnimationController}: this is component-level feedback with no widget of its own,
 * and it must keep running after the press that started it — including through a rebuild
 * of the subtree it belongs to.</p>
 */
final class InkFeedback {

    /// Growth of the ripple while the finger is still down.
    ///
    /// A full second, and that is the point: unconfirmed growth is slow enough to read as
    /// a circle travelling outward. Growing it in a third of a second instead -- which
    /// this did -- means the disc has already covered the target before the eye finds it,
    /// so the whole effect registers as the row flashing a flat grey rather than as ink.
    private static final long UNCONFIRMED_MS = 1000;
    /// Growth once the press is confirmed: the SAME progress, finished at this rate.
    private static final long RADIUS_MS = 225;
    /// Fade of the ink in.
    private static final long FADE_IN_MS = 75;
    /// Fade of the ink out, once confirmed.
    private static final long FADE_OUT_MS = 375;
    /// ... of which the ink holds full opacity for this fraction before it starts to go.
    private static final double FADE_OUT_HOLD = 225.0 / 375.0;
    /// Fade of the flat press highlight, both directions.
    private static final long HIGHLIGHT_MS = 200;

    /// The ripple begins as a disc this fraction of its target, never at nothing, and
    /// finishes slightly past the target so no seam shows at the edge.
    private static final double START_RADIUS_FRACTION = 0.30;
    private static final double RADIUS_OVERSHOOT = 5;

    /// Default ink, as ARGB. The alpha travels WITH the colour because a caller's
    /// explicit splash colour carries its own, and masking it off left every custom ink
    /// at the default weight.
    ///
    /// Deliberately NOT ThemeData's splashColor/highlightColor defaults (40% of a light
    /// grey). Those are the Material 2 fallbacks; what a Material 3 ink actually paints
    /// is a state layer at a fraction of onSurface, and it is far lighter. Measured
    /// against the reference on a pressed mail card: 100ms into the press its card is
    /// still clean white, while the 40% wash covered the whole card -- text, sender and
    /// icon all visibly greyed -- which reads as the row having changed colour rather
    /// than as ink.
    private static final int LIGHT_SPLASH_ARGB = 0x12000000;
    private static final int LIGHT_HIGHLIGHT_ARGB = 0x0F000000;
    private static final int DARK_INK_ARGB = 0x12FFFFFF;

    private double originX;
    private double originY;
    private double targetRadius;
    private int splashArgb = LIGHT_SPLASH_ARGB;
    private int highlightArgb = LIGHT_HIGHLIGHT_ARGB;

    /// Where the radius and the highlight had got to when the finger lifted.
    ///
    /// The confirmed growth does not restart: it carries the same normalized progress on
    /// at the faster rate, so a ripple released early finishes from where it stood rather
    /// than jumping back to the start.
    private double progressAtRelease;
    private double highlightAtRelease;

    /// Start of the growth phase, and of the fade once released, on the ANIMATION clock.
    ///
    /// Not the wall clock. Ink is an animation like any other, and reading the wall clock
    /// made this one the only thing on screen that a frozen clock could not hold still:
    /// asked for the frame at 100ms the ripple showed however far real time had carried
    /// it, so it saturated within two frames and then sat there. On a device that reads
    /// as ink that arrives too fast; in a test it is simply not reproducible.
    private long startedAt;
    private long releasedAt;
    private boolean held;
    private boolean active;

    private Animation clock;

    /**
     * A target that can say whether Codename One has turned the gesture into a drag.
     *
     * <p>{@code Component.isDragActivated()} is protected, so only the component itself
     * can answer. The clock needs the answer because once a drag starts, Codename One
     * delivers the rest of the gesture -- the release included -- to whatever is
     * scrolling, and nothing will ever release this ink.</p>
     */
    interface DragAware {
        boolean gestureBecameDrag();
    }

    // ------------------------------------------------------------------

    void press(Component c, int x, int y, InkResponse config) {
        if (config == null) {
            return;
        }
        originX = x;
        originY = y;
        boolean light = isLight(backgroundUnder(c));
        splashArgb = resolveArgb(config.getSplashColor(), config.getHighlightColor(),
                light ? LIGHT_SPLASH_ARGB : DARK_INK_ARGB);
        highlightArgb = resolveArgb(config.getHighlightColor(), config.getSplashColor(),
                light ? LIGHT_HIGHLIGHT_ARGB : DARK_INK_ARGB);
        targetRadius = radiusFor(c, config, x, y);
        startedAt = com.codename1.flutter.animation.MotionClock.now();
        releasedAt = 0;
        progressAtRelease = 0;
        highlightAtRelease = 0;
        held = true;
        active = true;
        attach(c);
        c.repaint();
    }

    /** The press became a tap: finish growing, then fade out. */
    void release(Component c) {
        if (!active || !held) {
            return;
        }
        held = false;
        long now = com.codename1.flutter.animation.MotionClock.now();
        progressAtRelease = clamp01((now - startedAt) / (double) UNCONFIRMED_MS);
        highlightAtRelease = clamp01((now - startedAt) / (double) HIGHLIGHT_MS);
        releasedAt = now;
        // The clock may have stopped itself while the press was held (see animate()); the
        // fade still needs frames, so make sure it is running again.
        attach(c);
        c.repaint();
    }

    /**
     * The press turned into a scroll. Flutter drops the ink immediately in that case —
     * leaving it to fade would paint a splash on a list that is already moving under the
     * finger, which reads as a mis-tap.
     */
    void cancel(Component c) {
        if (!active) {
            return;
        }
        active = false;
        held = false;
        detach(c);
        c.repaint();
    }

    // ------------------------------------------------------------------

    void paint(Graphics g, Component c) {
        if (!active) {
            return;
        }
        long now = com.codename1.flutter.animation.MotionClock.now();
        // One normalized progress for the radius, advanced at whichever rate applies:
        // slowly while the finger is down, then finished quickly once the tap is
        // confirmed. It never restarts, so the circle does not jump on release.
        double progress = held
                ? clamp01((now - startedAt) / (double) UNCONFIRMED_MS)
                : clamp01(progressAtRelease + (now - releasedAt) / (double) RADIUS_MS);
        double fadeIn = clamp01((now - startedAt) / (double) FADE_IN_MS);
        // The ink holds full opacity for the first stretch of the fade and only then
        // starts to go, so a quick tap still shows a complete ripple.
        double fadeOut = held ? 0
                : clamp01((clamp01((now - releasedAt) / (double) FADE_OUT_MS)
                        - FADE_OUT_HOLD) / (1 - FADE_OUT_HOLD));
        double ink = fadeIn * (1 - fadeOut);
        double highlight = held
                ? clamp01((now - startedAt) / (double) HIGHLIGHT_MS)
                : clamp01(highlightAtRelease - (now - releasedAt) / (double) HIGHLIGHT_MS);

        int oldColor = g.getColor();
        int oldAlpha = g.getAlpha();
        // PARENT-relative, not absolute: inside Component.paint the Graphics has already
        // accumulated every ancestor's translation, so absolute coordinates land at roughly
        // twice the offset and the clip below then intersects to nothing.
        int cx = c.getX();
        int cy = c.getY();
        int w = c.getWidth();
        int h = c.getHeight();

        int clipX = g.getClipX();
        int clipY = g.getClipY();
        int clipW = g.getClipWidth();
        int clipH = g.getClipHeight();
        // Ink is CONTAINED: it must not bleed past the tap target, and a splash from a
        // corner is wider than the box by construction.
        g.clipRect(cx, cy, w, h);
        try {
            if (highlight > 0) {
                g.setColor(highlightArgb & 0xFFFFFF);
                g.setAlpha((int) Math.round(((highlightArgb >>> 24) & 0xFF) * highlight));
                g.fillRect(cx, cy, w, h);
            }
            double start = targetRadius * START_RADIUS_FRACTION;
            double r = start + (targetRadius + RADIUS_OVERSHOOT - start)
                    * com.codename1.flutter.animation.Curves.ease.transform(progress);
            if (r > 0 && ink > 0) {
                g.setColor(splashArgb & 0xFFFFFF);
                g.setAlpha((int) Math.round(((splashArgb >>> 24) & 0xFF) * ink));
                int d = (int) Math.round(r * 2);
                g.fillArc((int) Math.round(cx + originX - r),
                        (int) Math.round(cy + originY - r), d, d, 0, 360);
            }
        } finally {
            g.setAlpha(oldAlpha);
            g.setColor(oldColor);
            g.setClip(clipX, clipY, clipW, clipH);
        }

        if (!held && fadeOut >= 1) {
            active = false;
            detach(c);
        }
    }

    // ------------------------------------------------------------------

    private void attach(Component c) {
        com.codename1.ui.Form f = c.getComponentForm();
        if (f == null || clock != null) {
            return;
        }
        final Component target = c;
        clock = new Animation() {
            @Override
            public boolean animate() {
                // Expire on the CLOCK, not in paint(). paint() only runs while the
                // component is actually being painted, so ink on a component that scrolls
                // away or stops repainting would stay "active" forever and keep this
                // clock - and its repaint - running for the life of the form.
                long now = com.codename1.flutter.animation.MotionClock.now();
                if (active && !held && now - releasedAt >= FADE_OUT_MS) {
                    active = false;
                }
                // The gesture turned into a drag. Codename One then delivers the rest of
                // it -- including the release -- to whatever is scrolling, so nothing will
                // ever release this ink and a HELD press keeps its highlight standing
                // (deliberately, see below). A mail row in the study went grey when
                // touched and stayed grey for exactly this: pressed inside a scrollable,
                // the finger moved a pixel, and the release went to the list.
                if (active && held && target instanceof DragAware
                        && ((DragAware) target).gestureBecameDrag()) {
                    active = false;
                    held = false;
                    target.repaint();
                    detach(target);
                    return false;
                }
                // A held press that never releases would otherwise animate for the life of
                // the form: the expiry above only fires once the finger is up. A real
                // finger always lifts, but a press whose release is swallowed - the
                // component removed by a rebuild mid-press, a cancelled gesture - would
                // leave this clock repainting forever. Once the splash has fully covered
                // the target there is nothing left to animate anyway, so stop asking for
                // frames and let the static ink stand until release.
                if (active && held && now - startedAt >= UNCONFIRMED_MS) {
                    detach(target);
                    return false;
                }
                if (active) {
                    // Repaint the COMPONENT, and always return false.
                    //
                    // Returning true from a registered Animation that is not a Component
                    // makes paintDirty set the flush region to the whole screen, call
                    // paint() on the animation - which paints nothing here, the ink is
                    // drawn by the component - and then flush the entire screen. That
                    // pushes a buffer this frame never painted into, which is visible as
                    // a full-screen flicker for as long as any ink is running.
                    //
                    // Repainting the component instead queues it with a real dirty
                    // region, so only the tap target is flushed.
                    target.repaint();
                } else {
                    detach(target);
                }
                return false;
            }

            @Override
            public void paint(Graphics g) {
            }
        };
        f.registerAnimated(clock);
    }

    private void detach(Component c) {
        if (clock == null) {
            return;
        }
        com.codename1.ui.Form f = c == null ? null : c.getComponentForm();
        if (f != null) {
            f.deregisterAnimated(clock);
        }
        clock = null;
    }

    /**
     * Flutter's target radius: far enough to cover the whole box from wherever the finger
     * landed, so the splash never stops short of a corner.
     */
    private static double radiusFor(Component c, InkResponse config, double x, double y) {
        if (config.getRadius() != null) {
            return Dp.px(config.getRadius());
        }
        double w = c.getWidth();
        double h = c.getHeight();
        double dx = Math.max(x, w - x);
        double dy = Math.max(y, h - y);
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * The ink colour: the widget's own splashColor when it names one, else a neutral
     * derived from the surface it sits on — dark ink on light surfaces and light ink on
     * dark ones, which is what Material's onSurface state layer amounts to.
     */
    /// The first colour that was actually given, ALPHA INCLUDED, else the default.
    ///
    /// A caller states one of the two far more often than both, and the reference falls
    /// back to the other one rather than to its theme default in that case -- an ink that
    /// names only a splash colour should not highlight in an unrelated grey.
    private static int resolveArgb(com.codename1.flutter.Color preferred,
            com.codename1.flutter.Color fallback, int defaultArgb) {
        com.codename1.flutter.Color explicit = preferred != null ? preferred : fallback;
        if (explicit == null) {
            return defaultArgb;
        }
        int argb = (int) explicit.value();
        // A colour given with no alpha at all is opaque by construction, and painting ink
        // at full opacity hides the row under it. Treat it as the default weight.
        return (argb >>> 24) == 0 ? (defaultArgb & 0xFF000000) | (argb & 0xFFFFFF) : argb;
    }

    /// The colour actually behind this tap area: the overlay itself is transparent, so ask
    /// the ancestors until one of them paints.
    private static int backgroundUnder(Component c) {
        for (Component a = c; a != null; a = a.getParent()) {
            com.codename1.ui.plaf.Style s = a.getStyle();
            if (s != null && s.getBgTransparency() != 0) {
                return s.getBgColor();
            }
        }
        return 0xFFFFFF;
    }

    private static boolean isLight(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (r * 299 + g * 587 + b * 114) / 1000 >= 128;
    }


    private static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }
}
