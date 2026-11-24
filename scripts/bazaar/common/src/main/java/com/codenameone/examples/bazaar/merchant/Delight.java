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
package com.codenameone.examples.bazaar.merchant;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.Painter;
import com.codename1.ui.animations.Animation;
import com.codename1.ui.animations.Motion;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.CN;

/// The seller's small rewards: numbers that count up, goal rings that fill,
/// a burst of confetti for a milestone and a toast that pulses when an order
/// arrives. None makes a sound.
final class Delight {
    private Delight() {
    }

    /// How a counter writes a value.
    interface Format {
        String of(long value);
    }

    /// A label that counts up to its value once it is first shown.
    static Label counter(final long target, final Format format, String uiid) {
        final long[] start = {0};
        final Label l = new Label(format.of(target), uiid) {
            @Override
            protected void initComponent() {
                super.initComponent();
                Form f = getComponentForm();
                if (f != null && target != 0 && start[0] == 0) {
                    start[0] = System.currentTimeMillis();
                    setText(format.of(0));
                    f.registerAnimated(this);
                }
            }

            @Override
            protected void deinitialize() {
                Form f = getComponentForm();
                if (f != null) {
                    f.deregisterAnimated(this);
                }
                super.deinitialize();
            }

            @Override
            public boolean animate() {
                long elapsed = System.currentTimeMillis() - start[0];
                setText(format.of(SellerGoals.counted(target, elapsed, COUNT_MILLIS)));
                if (elapsed >= COUNT_MILLIS) {
                    Form f = getComponentForm();
                    if (f != null) {
                        f.deregisterAnimated(this);
                    }
                }
                return true;
            }
        };
        l.setShouldLocalize(false);
        return l;
    }

    private static final long COUNT_MILLIS = 900;

    /// A ring filled to `bp` basis points of a goal, filling as it appears,
    /// the share written in its middle.
    static Component ring(final int bp, final String name) {
        Component ring = new Component() {
            private long start;

            @Override
            protected Dimension calcPreferredSize() {
                int d = CN.convertToPixels(22f);
                return new Dimension(d, d);
            }

            @Override
            protected void initComponent() {
                super.initComponent();
                if (getComponentForm() != null && start == 0) {
                    start = System.currentTimeMillis();
                    getComponentForm().registerAnimated(this);
                }
            }

            @Override
            protected void deinitialize() {
                if (getComponentForm() != null) {
                    getComponentForm().deregisterAnimated(this);
                }
                super.deinitialize();
            }

            @Override
            public boolean animate() {
                if (System.currentTimeMillis() - start > RING_MILLIS && getComponentForm() != null) {
                    getComponentForm().deregisterAnimated(this);
                }
                return true;
            }

            @Override
            public void paint(Graphics g) {
                long shown = start == 0 ? bp
                        : SellerGoals.counted(bp, System.currentTimeMillis() - start, RING_MILLIS);
                int d = Math.min(getWidth(), getHeight());
                int x = getX() + (getWidth() - d) / 2;
                int y = getY() + (getHeight() - d) / 2;
                int thick = Math.max(4, d / 8);
                boolean aa = g.isAntiAliased();
                g.setAntiAliased(true);
                g.setColor(MCharts.color("MeChartGrid"));
                g.fillArc(x, y, d, d, 0, 360);
                g.setColor(MCharts.color(bp >= 10000 ? "MeGoalDone" : "MeChartLine"));
                g.fillArc(x, y, d, d, 90, -(int) (360 * shown / 10000));
                g.setColor(MCharts.color("MeRingHole"));
                g.fillArc(x + thick, y + thick, d - 2 * thick, d - 2 * thick, 0, 360);
                g.setAntiAliased(aa);
                String text = (shown / 100) + "%";
                com.codename1.ui.Font f = getStyle().getFont();
                g.setFont(f);
                g.setColor(getStyle().getFgColor());
                g.drawString(text, x + (d - f.stringWidth(text)) / 2, y + (d - f.getHeight()) / 2);
            }
        };
        ring.setUIID("MeGoalRing");
        ring.setName(name);
        return ring;
    }

    private static final long RING_MILLIS = 1200;

    /// A burst of confetti over the whole form, gone in under two seconds.
    static void burst(final Form form) {
        if (form == null) {
            return;
        }
        final Painter before = form.getGlassPane();
        final int n = 70;
        final float[] px = new float[n];
        final float[] py = new float[n];
        final float[] vx = new float[n];
        final float[] vy = new float[n];
        final int[] rgb = new int[n];
        final int[] palette = {MCharts.color("MeChartSlice1"), MCharts.color("MeChartSlice2"),
            MCharts.color("MeChartSlice3"), MCharts.color("MeChartSlice4"),
            MCharts.color("MeGoalDone")};
        int w = form.getWidth();
        int h = form.getHeight();
        float unit = CN.convertToPixels(1f);
        java.util.Random r = new java.util.Random();
        for (int iter = 0; iter < n; iter++) {
            px[iter] = w / 2f;
            py[iter] = h / 3f;
            double angle = r.nextDouble() * Math.PI * 2;
            float speed = unit * (0.6f + r.nextFloat() * 1.6f);
            vx[iter] = (float) (Math.cos(angle) * speed);
            vy[iter] = (float) (Math.sin(angle) * speed) - unit;
            rgb[iter] = palette[iter % palette.length];
        }
        final long start = System.currentTimeMillis();
        final float gravity = unit * 0.06f;
        final int size = Math.max(3, (int) (unit * 1.2f));
        Painter confetti = new Painter() {
            @Override
            public void paint(Graphics g, Rectangle rect) {
                if (before != null) {
                    before.paint(g, rect);
                }
                long elapsed = System.currentTimeMillis() - start;
                int alpha = elapsed < 1100 ? 255 : (int) Math.max(0, 255 - (elapsed - 1100) / 3);
                int was = g.getAlpha();
                g.setAlpha(alpha);
                for (int iter = 0; iter < n; iter++) {
                    g.setColor(rgb[iter]);
                    g.fillRect((int) px[iter], (int) py[iter], size, iter % 2 == 0 ? size : size / 2);
                }
                g.setAlpha(was);
            }
        };
        form.setGlassPane(confetti);
        form.registerAnimated(new Animation() {
            @Override
            public boolean animate() {
                long elapsed = System.currentTimeMillis() - start;
                if (elapsed > BURST_MILLIS) {
                    form.deregisterAnimated(this);
                    form.setGlassPane(before);
                    form.repaint();
                    return false;
                }
                for (int iter = 0; iter < n; iter++) {
                    vy[iter] += gravity;
                    vx[iter] *= 0.985f;
                    px[iter] += vx[iter];
                    py[iter] += vy[iter];
                }
                form.repaint();
                return false;
            }

            @Override
            public void paint(Graphics g) {
            }
        });
    }

    private static final long BURST_MILLIS = 1900;

    /// A toast that slides down from the top of `form` with a pulsing dot, and
    /// leaves by itself.
    static void toast(final Form form, String title, String detail, String name) {
        if (form == null) {
            return;
        }
        final Container toast = new Container(new BorderLayout(), "MeLiveToast");
        toast.setName(name);
        toast.add(BorderLayout.WEST, pulse());
        Container words = new Container(BoxLayout.y());
        Label t = new Label(title, "MeLiveToastTitle");
        t.setShouldLocalize(false);
        words.add(t);
        if (detail != null) {
            Label d = new Label(detail, "MeLiveToastDetail");
            d.setShouldLocalize(false);
            words.add(d);
        }
        toast.add(BorderLayout.CENTER, words);
        final Container layer = form.getLayeredPane(Delight.class, true);
        // Codename One has no null layout (it threw here, so no toast ever
        // showed); a layout that leaves its children where they are put.
        layer.setLayout(new Layout() {
            @Override
            public void layoutContainer(Container parent) {
            }

            @Override
            public Dimension getPreferredSize(Container parent) {
                return new Dimension(0, 0);
            }
        });
        Dimension pref = toast.getPreferredSize();
        final int tw = Math.min(form.getWidth() - CN.convertToPixels(8f),
                Math.max(pref.getWidth(), CN.convertToPixels(70f)));
        final int th = pref.getHeight();
        final int x = (form.getWidth() - tw) / 2;
        final int top = form.getContentPane().getAbsoluteY() - form.getAbsoluteY()
                + CN.convertToPixels(3f);
        toast.setWidth(tw);
        toast.setHeight(th);
        toast.setX(x);
        toast.setY(-th);
        layer.addComponent(toast);
        final Motion in = Motion.createEaseOutMotion(-th, top, 380);
        in.start();
        final long shown = System.currentTimeMillis();
        form.registerAnimated(new Animation() {
            private Motion out;

            @Override
            public boolean animate() {
                if (out == null && System.currentTimeMillis() - shown > TOAST_MILLIS) {
                    out = Motion.createEaseInMotion(top, -th - 4, 320);
                    out.start();
                }
                Motion m = out != null ? out : in;
                toast.setY(m.getValue());
                layer.repaint();
                if (out != null && out.isFinished()) {
                    form.deregisterAnimated(this);
                    toast.remove();
                    form.repaint();
                }
                return false;
            }

            @Override
            public void paint(Graphics g) {
            }
        });
    }

    private static final long TOAST_MILLIS = 4200;

    /// A dot with a ring that grows and fades from it, over and over.
    private static Component pulse() {
        Component c = new Component() {
            private final long start = System.currentTimeMillis();

            @Override
            protected Dimension calcPreferredSize() {
                int d = CN.convertToPixels(8f);
                return new Dimension(d, d);
            }

            @Override
            protected void initComponent() {
                super.initComponent();
                getComponentForm().registerAnimated(this);
            }

            @Override
            protected void deinitialize() {
                if (getComponentForm() != null) {
                    getComponentForm().deregisterAnimated(this);
                }
                super.deinitialize();
            }

            @Override
            public boolean animate() {
                return true;
            }

            @Override
            public void paint(Graphics g) {
                int d = Math.min(getWidth(), getHeight());
                int cx = getX() + getWidth() / 2;
                int cy = getY() + getHeight() / 2;
                double t = ((System.currentTimeMillis() - start) % 1400) / 1400.0;
                int core = d / 4;
                int ring = core + (int) ((d / 2 - core) * t);
                boolean aa = g.isAntiAliased();
                g.setAntiAliased(true);
                int was = g.getAlpha();
                g.setColor(MCharts.color("MeGoalDone"));
                g.setAlpha((int) (160 * (1 - t)));
                g.fillArc(cx - ring, cy - ring, ring * 2, ring * 2, 0, 360);
                g.setAlpha(255);
                g.fillArc(cx - core / 2, cy - core / 2, core, core, 0, 360);
                g.setAlpha(was);
                g.setAntiAliased(aa);
            }
        };
        c.setUIID("Container");
        return c;
    }
}
