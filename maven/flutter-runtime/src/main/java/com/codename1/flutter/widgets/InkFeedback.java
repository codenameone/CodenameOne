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

    /// How long the splash takes to cover the target once the finger lands.
    private static final long SPLASH_MS = 320;
    /// Fade of the splash once the press is confirmed (Flutter's ~150ms).
    private static final long FADE_MS = 180;
    /// Fade of the flat press highlight, both directions.
    private static final long HIGHLIGHT_MS = 90;

    /// Opacity of the splash and of the flat press highlight, out of 255.
    ///
    /// Material 3 puts the pressed state layer at 10% of onSurface, and the splash rides
    /// on top of it rather than replacing it - so these are deliberately low and only add
    /// up to ~13% where the splash has arrived. Anything heavier stops reading as ink and
    /// starts reading as the row having changed colour.
    private static final int SPLASH_ALPHA = 18;    // ~7%
    private static final int HIGHLIGHT_ALPHA = 15; // ~6%

    private double originX;
    private double originY;
    private double targetRadius;
    private int inkColor = 0x000000;

    /// Wall-clock start of the growth phase, and of the fade once released.
    private long startedAt;
    private long releasedAt;
    private boolean held;
    private boolean active;

    private Animation clock;

    // ------------------------------------------------------------------

    void press(Component c, int x, int y, InkResponse config) {
        if (config == null) {
            return;
        }
        originX = x;
        originY = y;
        inkColor = resolveInkColor(c, config);
        targetRadius = radiusFor(c, config, x, y);
        startedAt = System.currentTimeMillis();
        releasedAt = 0;
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
        releasedAt = System.currentTimeMillis();
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
        long now = System.currentTimeMillis();
        double grow = clamp01((now - startedAt) / (double) SPLASH_MS);
        // Held: the highlight is fully in and the splash keeps growing. Released: the
        // splash finishes wherever it is and both fade together.
        double fade = held ? 0 : clamp01((now - releasedAt) / (double) FADE_MS);
        double highlight = held
                ? clamp01((now - startedAt) / (double) HIGHLIGHT_MS)
                : (1 - fade);

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
            g.setColor(inkColor);
            if (highlight > 0) {
                g.setAlpha((int) Math.round(HIGHLIGHT_ALPHA * highlight));
                g.fillRect(cx, cy, w, h);
            }
            double r = targetRadius * easeOut(grow);
            if (r > 0) {
                g.setAlpha((int) Math.round(SPLASH_ALPHA * (held ? 1 : 1 - fade)));
                int d = (int) Math.round(r * 2);
                g.fillArc((int) Math.round(cx + originX - r),
                        (int) Math.round(cy + originY - r), d, d, 0, 360);
            }
        } finally {
            g.setAlpha(oldAlpha);
            g.setColor(oldColor);
            g.setClip(clipX, clipY, clipW, clipH);
        }

        if (!held && fade >= 1) {
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
                long now = System.currentTimeMillis();
                if (active && !held && now - releasedAt >= FADE_MS) {
                    active = false;
                }
                // A held press that never releases would otherwise animate for the life of
                // the form: the expiry above only fires once the finger is up. A real
                // finger always lifts, but a press whose release is swallowed - the
                // component removed by a rebuild mid-press, a cancelled gesture - would
                // leave this clock repainting forever. Once the splash has fully covered
                // the target there is nothing left to animate anyway, so stop asking for
                // frames and let the static ink stand until release.
                if (active && held && now - startedAt >= SPLASH_MS + HIGHLIGHT_MS) {
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
    private static int resolveInkColor(Component c, InkResponse config) {
        com.codename1.flutter.Color explicit = config.getSplashColor() != null
                ? config.getSplashColor() : config.getHighlightColor();
        if (explicit != null) {
            return (int) (explicit.value() & 0xFFFFFF);
        }
        return isLight(backgroundUnder(c)) ? 0x000000 : 0xFFFFFF;
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

    private static double easeOut(double t) {
        double inv = 1 - t;
        return 1 - inv * inv;
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }
}
