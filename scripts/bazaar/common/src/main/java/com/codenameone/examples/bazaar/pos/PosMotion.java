/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bazaar.pos;

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.Painter;
import com.codename1.ui.animations.Animation;
import com.codename1.ui.animations.Motion;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.plaf.UIManager;

/// The register's motion: short, so a busy cashier never waits on it.
///
/// An item flies from its tile into the cart, the total counts to its new
/// value, a finished sale draws a check, and the shift's goal bar fills. Each
/// runs on the form's animation loop and none blocks a tap.
final class PosMotion {
    private PosMotion() {
    }

    /// Flies `image` from `from` into `to` along a short arc, shrinking and
    /// fading, then runs `done` (which may be null). Nothing flies when
    /// either end is off screen.
    static void fly(final Form form, Image image, Component from, Component to,
            final Runnable done) {
        if (form == null || image == null || from == null || to == null
                || from.getWidth() <= 0 || to.getWidth() <= 0) {
            if (done != null) {
                done.run();
            }
            return;
        }
        final int size = Math.max(8, Math.min(from.getWidth(), from.getHeight()));
        final int sx = from.getAbsoluteX() + from.getWidth() / 2;
        final int sy = from.getAbsoluteY() + from.getHeight() / 2;
        final int tx = to.getAbsoluteX() + to.getWidth() / 2;
        final int ty = to.getAbsoluteY() + to.getHeight() / 2;
        final int end = CN.convertToPixels(5);
        final Image picture = image.scaled(size, size);
        final Motion motion = Motion.createEaseInOutMotion(0, 1000, 420);
        final Painter previous = form.getGlassPane();
        form.setGlassPane((Graphics g, Rectangle rect) -> {
            if (previous != null) {
                previous.paint(g, rect);
            }
            int t = motion.getValue();
            int x = sx + (tx - sx) * t / 1000;
            int lift = (int) (Math.sin(Math.PI * t / 1000.0) * Math.min(size, CN.convertToPixels(14)) * 0.5);
            int y = sy + (ty - sy) * t / 1000 - lift;
            int s = size - (size - end) * t / 1000;
            int old = g.getAlpha();
            g.setAlpha(255 - 140 * t / 1000);
            g.drawImage(picture, x - s / 2, y - s / 2, s, s);
            g.setAlpha(old);
        });
        run(form, motion, () -> {
            form.setGlassPane(previous);
            form.repaint();
            if (done != null) {
                done.run();
            }
        });
    }

    /// Counts the money shown in `label` from `from` to `to` cents, with
    /// `prefix` ahead of the amount (already translated).
    static void count(final Label label, final long from, final long to, final String prefix,
            final String suffix) {
        final Form form = label.getComponentForm();
        if (form == null || from == to) {
            label.setText(prefix + PosState.money(to) + suffix);
            return;
        }
        final Motion motion = Motion.createEaseOutMotion(0, 1000, 280);
        label.putClientProperty("posCount", motion);
        motion.start();
        form.registerAnimated(new Animation() {
            @Override
            public boolean animate() {
                if (label.getClientProperty("posCount") != motion) {
                    form.deregisterAnimated(this);
                    return false;
                }
                long v = from + (to - from) * motion.getValue() / 1000;
                label.setText(prefix + PosState.money(v) + suffix);
                if (motion.isFinished()) {
                    label.putClientProperty("posCount", null);
                    label.setText(prefix + PosState.money(to) + suffix);
                    form.deregisterAnimated(this);
                }
                label.repaint();
                return false;
            }

            @Override
            public void paint(Graphics g) {
            }
        });
    }

    private static void run(final Form form, final Motion motion, final Runnable finished) {
        motion.start();
        form.registerAnimated(new Animation() {
            @Override
            public boolean animate() {
                if (motion.isFinished()) {
                    form.deregisterAnimated(this);
                    finished.run();
                    return false;
                }
                return true;
            }

            @Override
            public void paint(Graphics g) {
            }
        });
    }

    /// The check a finished sale draws: a disc that grows in, then the tick
    /// stroked across it, in a little over half a second.
    static final class Check extends Component {
        private final Motion motion = Motion.createEaseOutMotion(0, 1000, 560);
        private boolean started;

        Check() {
            setUIID("PosCheck");
            setName("pos-sale-check");
        }

        @Override
        protected void initComponent() {
            super.initComponent();
            if (!started) {
                started = true;
                motion.start();
                final Form f = getComponentForm();
                if (f != null) {
                    f.registerAnimated(this);
                }
            }
        }

        @Override
        public boolean animate() {
            if (motion.isFinished()) {
                Form f = getComponentForm();
                if (f != null) {
                    f.deregisterAnimated(this);
                }
            }
            return true;
        }

        @Override
        protected com.codename1.ui.geom.Dimension calcPreferredSize() {
            int s = CN.convertToPixels(16);
            return new com.codename1.ui.geom.Dimension(getWidth() > 0 ? getWidth() : s, s);
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            int t = motion.getValue();
            int full = CN.convertToPixels(13);
            int cx = getX() + getWidth() / 2;
            int cy = getY() + getHeight() / 2;
            // The disc overshoots a touch and settles: 0..600 grows to 108%,
            // then back to 100%.
            int scale = t < 600 ? t * 1080 / 600 : 1080 - (t - 600) * 80 / 400;
            int d = full * scale / 1000;
            g.setAntiAliased(true);
            int fg = UIManager.getInstance().getComponentStyle("PosCheck").getFgColor();
            int old = g.getAlpha();
            g.setColor(fg);
            g.setAlpha(46);
            int halo = d + CN.convertToPixels(3) * t / 1000;
            g.fillArc(cx - halo / 2, cy - halo / 2, halo, halo, 0, 360);
            g.setAlpha(255);
            g.fillArc(cx - d / 2, cy - d / 2, d, d, 0, 360);
            g.setAlpha(old);
            if (t > 350) {
                // The tick, stroked from its short arm to the end of its long one.
                int p = Math.min(1000, (t - 350) * 1000 / 550);
                int u = full / 26;
                int ax = cx - 6 * u;
                int ay = cy;
                int bx = cx - 2 * u;
                int by = cy + 4 * u;
                int ex = cx + 6 * u;
                int ey = cy - 5 * u;
                int stroke = Math.max(2, CN.convertToPixels(1.1f));
                g.setColor(UIManager.getInstance().getComponentStyle("PosCheck").getBgColor());
                if (p <= 400) {
                    int q = p * 1000 / 400;
                    line(g, ax, ay, ax + (bx - ax) * q / 1000, ay + (by - ay) * q / 1000, stroke);
                } else {
                    int q = (p - 400) * 1000 / 600;
                    line(g, ax, ay, bx, by, stroke);
                    line(g, bx, by, bx + (ex - bx) * q / 1000, by + (ey - by) * q / 1000, stroke);
                }
            }
        }

        /// A thick round-capped line, as discs along it: Graphics has no
        /// stroke width without shapes, which not every port draws.
        private static void line(Graphics g, int x0, int y0, int x1, int y1, int w) {
            int steps = Math.max(1, Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)));
            for (int i = 0; i <= steps; i++) {
                int x = x0 + (x1 - x0) * i / steps;
                int y = y0 + (y1 - y0) * i / steps;
                g.fillArc(x - w / 2, y - w / 2, w, w, 0, 360);
            }
        }
    }

    /// The shift's goal as a slim bar that fills to its value, and glows when
    /// a milestone is crossed.
    static final class GoalBar extends Component {
        private int shown;
        private int target;
        private Motion motion;
        private int glow;

        GoalBar() {
            setUIID("PosGoalTrack");
            setName("pos-goal-bar");
        }

        /// Fills to `permille` thousandths, animated from where it stands.
        void fill(int permille, boolean celebrate) {
            int from = current();
            target = Math.max(0, Math.min(1000, permille));
            if (celebrate) {
                glow = 1;
            }
            Form f = getComponentForm();
            if (f == null) {
                shown = target;
                return;
            }
            shown = from;
            motion = Motion.createEaseOutMotion(0, 1000, celebrate ? 900 : 450);
            motion.start();
            f.registerAnimated(this);
        }

        private int current() {
            if (motion == null) {
                return shown;
            }
            return shown + (target - shown) * motion.getValue() / 1000;
        }

        @Override
        public boolean animate() {
            if (motion != null && motion.isFinished()) {
                shown = target;
                motion = null;
                glow = 0;
                Form f = getComponentForm();
                if (f != null) {
                    f.deregisterAnimated(this);
                }
            }
            return true;
        }

        @Override
        protected com.codename1.ui.geom.Dimension calcPreferredSize() {
            return new com.codename1.ui.geom.Dimension(CN.convertToPixels(20), CN.convertToPixels(1.4f));
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            int w = getWidth();
            int h = Math.min(getHeight(), CN.convertToPixels(1.4f));
            int y = getY() + (getHeight() - h) / 2;
            int v = current();
            int fill = w * v / 1000;
            g.setAntiAliased(true);
            g.setColor(UIManager.getInstance().getComponentStyle("PosGoalTrack").getFgColor());
            g.fillRoundRect(getX(), y, w, h, h, h);
            if (fill <= 0) {
                return;
            }
            int color = UIManager.getInstance().getComponentStyle("PosGoalFill").getFgColor();
            if (v >= 1000) {
                color = UIManager.getInstance().getComponentStyle("PosGoalDone").getFgColor();
            }
            g.setColor(color);
            g.fillRoundRect(getX(), y, Math.max(h, fill), h, h, h);
            if (glow > 0 && motion != null) {
                // A highlight sweeping along the filled part.
                int t = motion.getValue();
                int gx = getX() + fill * t / 1000;
                int old = g.getAlpha();
                g.setColor(0xffffff);
                g.setAlpha(150 * (1000 - t) / 1000);
                g.fillRoundRect(Math.max(getX(), gx - h * 3), y, h * 3, h, h, h);
                g.setAlpha(old);
            }
        }
    }
}
