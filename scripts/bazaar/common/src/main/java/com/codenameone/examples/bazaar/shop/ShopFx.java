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
package com.codenameone.examples.bazaar.shop;

import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.Painter;
import com.codename1.ui.animations.Animation;
import com.codename1.ui.animations.Motion;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codenameone.examples.bazaar.ui.Lang;
import java.util.ArrayList;
import java.util.List;

/// The shop's small motions, in the spirit of the add-to-cart flight: a
/// burst of confetti, points that float up, a banner that drops in, a count
/// that runs up, an icon that pops. Each is short, drawn over the form on its
/// glass pane by one shared overlay, and gives way when the form changes, so
/// several can run at once and none blocks a tap.
final class ShopFx {
    /// One effect on the overlay: paints itself `elapsed` ms after it started,
    /// and answers false when it is over.
    interface Effect {
        boolean paint(Graphics g, Form form, long elapsed);
    }

    private static final int[] CONFETTI = {0xff5a5f, 0xffb400, 0x00a699, 0x7b61ff, 0x3d9df5,
        0xff8a00};

    private static final List<Effect> EFFECTS = new ArrayList<Effect>();
    private static final List<Long> STARTS = new ArrayList<Long>();
    private static Form on;
    private static Painter previous;
    private static Animation ticker;

    private ShopFx() {
    }

    /// Adds `e` to the overlay of `form`. A form other than the one the
    /// running effects are on drops them first.
    static void add(final Form form, Effect e) {
        if (form == null || e == null) {
            return;
        }
        if (on != form) {
            stop();
        }
        if (on == null) {
            on = form;
            previous = form.getGlassPane();
            form.setGlassPane((Graphics g, Rectangle rect) -> {
                if (previous != null) {
                    previous.paint(g, rect);
                }
                long now = System.currentTimeMillis();
                for (int iter = EFFECTS.size() - 1; iter >= 0; iter--) {
                    if (!EFFECTS.get(iter).paint(g, form, now - STARTS.get(iter).longValue())) {
                        EFFECTS.remove(iter);
                        STARTS.remove(iter);
                    }
                }
            });
            ticker = new Animation() {
                @Override
                public boolean animate() {
                    if (EFFECTS.isEmpty() || Display.getInstance().getCurrent() != form) {
                        stop();
                        return false;
                    }
                    form.repaint();
                    return true;
                }

                @Override
                public void paint(Graphics g) {
                }
            };
            form.registerAnimated(ticker);
        }
        EFFECTS.add(e);
        STARTS.add(Long.valueOf(System.currentTimeMillis()));
        form.repaint();
    }

    private static void stop() {
        if (on != null) {
            on.deregisterAnimated(ticker);
            on.setGlassPane(previous);
            on.repaint();
        }
        on = null;
        previous = null;
        ticker = null;
        EFFECTS.clear();
        STARTS.clear();
    }

    private static Image heroImage;
    private static int[] heroRect;
    private static long heroAt;

    /// Remembers the photo of the card about to open and where it sat, for
    /// [#hero] on the page that opens next.
    static void heroFrom(Image image, int x, int y, int w, int h) {
        heroImage = image;
        heroRect = new int[] {x, y, w, h};
        heroAt = System.currentTimeMillis();
    }

    /// Whether a card's photo is waiting to grow into the page about to show.
    static boolean hasHero() {
        return heroImage != null && System.currentTimeMillis() - heroAt <= 1500;
    }

    /// The shared-element step from a card to its product page: the card's
    /// photo grows from where it was into the gallery's place at the top of
    /// `form` in 320 ms, then fades into the real gallery under it.
    static void hero(Form form) {
        final Image image = heroImage;
        final int[] src = heroRect;
        heroImage = null;
        if (image == null || src == null || System.currentTimeMillis() - heroAt > 1500) {
            return;
        }
        final int dw = Math.min(form.getWidth(),
                com.codenameone.examples.bazaar.ui.Layouts.column());
        final int dh = dw * 4 / 5;
        final int dx = (form.getWidth() - dw) / 2;
        final int dy = form.getContentPane().getAbsoluteY();
        final long grow = 320;
        final long fade = 160;
        add(form, (g, f, t) -> {
            if (t > grow + fade) {
                return false;
            }
            float p = ease(Math.min(1f, t / (float) grow));
            int x = src[0] + (int) ((dx - src[0]) * p);
            int y = src[1] + (int) ((dy - src[1]) * p);
            int w = src[2] + (int) ((dw - src[2]) * p);
            int h = src[3] + (int) ((dh - src[3]) * p);
            int old = g.getAlpha();
            g.setAlpha(t <= grow ? 255 : (int) (255 * (grow + fade - t) / fade));
            g.drawImage(image, x, y, w, h);
            g.setAlpha(old);
            return true;
        });
    }

    /// Confetti from the top of `form`, for a moment worth it: an order
    /// placed, a parcel delivered, a badge unlocked.
    static void confetti(Form form) {
        final int w = Math.max(1, form.getWidth());
        final int h = Math.max(1, form.getHeight());
        final int count = 70;
        final float[][] bits = new float[count][6];
        // A fixed sequence: every run looks the same, and the captures agree.
        long seed = 17;
        for (int iter = 0; iter < count; iter++) {
            seed = (seed * 1103515245L + 12345L) & 0x7fffffffL;
            float r1 = (seed % 1000) / 1000f;
            seed = (seed * 1103515245L + 12345L) & 0x7fffffffL;
            float r2 = (seed % 1000) / 1000f;
            bits[iter][0] = w * r1;
            bits[iter][1] = -h * 0.15f * r2;
            bits[iter][2] = (r2 - 0.5f) * w * 0.5f;
            bits[iter][3] = h * (0.55f + 0.5f * r1);
            bits[iter][4] = CONFETTI[iter % CONFETTI.length];
            bits[iter][5] = r1 * 6.28f;
        }
        final int size = Math.max(4, Display.getInstance().convertToPixels(1.4f));
        final long length = 1600;
        add(form, (g, f, t) -> {
            if (t > length) {
                return false;
            }
            float s = t / 1000f;
            int alpha = t < length - 400 ? 255 : (int) (255 * (length - t) / 400);
            int old = g.getAlpha();
            boolean aa = g.isAntiAliased();
            g.setAntiAliased(true);
            g.setAlpha(alpha);
            for (int iter = 0; iter < count; iter++) {
                float[] b = bits[iter];
                int x = (int) (b[0] + b[2] * s);
                int y = (int) (b[1] + b[3] * s + h * 0.35f * s * s);
                g.setColor((int) b[4]);
                // A turning paper slip: its width breathes as it spins.
                int sw = Math.max(2, (int) (size * Math.abs(Math.cos(b[5] + s * 7))));
                g.fillRect(x, y, sw, size + size / 2);
            }
            g.setAlpha(old);
            g.setAntiAliased(aa);
            return true;
        });
    }

    /// A label of `text` that rises from over `from` and fades, with a ring
    /// of sparks: "+120 pts" when points are earned.
    static void burst(Form form, Component from, final String text) {
        if (from == null) {
            return;
        }
        final int cx = from.getAbsoluteX() + from.getWidth() / 2;
        final int cy = from.getAbsoluteY() + from.getHeight() / 2;
        final Style style = UIManager.getInstance().getComponentStyle("ShopBurst");
        final int rise = Display.getInstance().convertToPixels(14);
        final int spark = Display.getInstance().convertToPixels(9);
        final long length = 900;
        add(form, (g, f, t) -> {
            if (t > length) {
                return false;
            }
            float p = t / (float) length;
            float ease = 1 - (1 - p) * (1 - p);
            int old = g.getAlpha();
            boolean aa = g.isAntiAliased();
            g.setAntiAliased(true);
            g.setAlpha((int) (255 * (1 - p * p)));
            g.setColor(style.getFgColor());
            for (int iter = 0; iter < 10; iter++) {
                double a = iter * Math.PI / 5;
                int r = (int) (spark * ease);
                int d = Math.max(2, (int) (spark / 6 * (1 - p)));
                g.fillArc(cx + (int) (Math.cos(a) * r) - d / 2, cy + (int) (Math.sin(a) * r) - d / 2,
                        d, d, 0, 360);
            }
            Font font = style.getFont();
            g.setFont(font);
            int tw = font.stringWidth(text);
            g.drawString(text, cx - tw / 2, cy - (int) (rise * ease) - font.getHeight() / 2);
            g.setAlpha(old);
            g.setAntiAliased(aa);
            return true;
        });
    }

    /// An in-app banner that drops in from the top of `form`, stays a moment
    /// and leaves: each step of an order the shopper is watching.
    static void banner(Form form, final char icon, final String title, final String text) {
        final Style box = UIManager.getInstance().getComponentStyle("ShopBanner");
        final Style head = UIManager.getInstance().getComponentStyle("ShopBannerTitle");
        final Style body = UIManager.getInstance().getComponentStyle("ShopBannerText");
        final int pad = Display.getInstance().convertToPixels(3);
        final int margin = Display.getInstance().convertToPixels(2.5f);
        final long in = 280;
        final long hold = 2600;
        final long length = in + hold + in;
        final String t1 = Lang.tr(title);
        final Image glyph = FontImage.createMaterial(icon, box, 4.2f);
        add(form, (g, f, t) -> {
            if (t > length) {
                return false;
            }
            int w = Math.min(f.getWidth() - margin * 2, Display.getInstance().convertToPixels(110));
            int x = (f.getWidth() - w) / 2;
            int h = pad * 2 + Math.max(glyph.getHeight(),
                    head.getFont().getHeight() + (text == null ? 0 : body.getFont().getHeight()));
            int top = f.getAbsoluteY() + Display.getInstance().convertToPixels(9);
            float p;
            if (t < in) {
                p = 1 - ease(t / (float) in);
            } else if (t > in + hold) {
                p = ease((t - in - hold) / (float) in);
            } else {
                p = 0;
            }
            int y = top - (int) ((h + top) * p);
            int old = g.getAlpha();
            boolean aa = g.isAntiAliased();
            g.setAntiAliased(true);
            g.setAlpha(40);
            g.setColor(0x000000);
            g.fillRoundRect(x, y + pad / 3, w, h, pad * 2, pad * 2);
            g.setAlpha(255);
            g.setColor(box.getBgColor());
            g.fillRoundRect(x, y, w, h, pad * 2, pad * 2);
            g.drawImage(glyph, x + pad, y + (h - glyph.getHeight()) / 2);
            int tx = x + pad * 2 + glyph.getWidth();
            int ty = y + pad;
            g.setColor(head.getFgColor());
            g.setFont(head.getFont());
            g.drawString(t1, tx, ty);
            if (text != null) {
                g.setColor(body.getFgColor());
                g.setFont(body.getFont());
                g.drawString(clip(text, body.getFont(), w - (tx - x) - pad), tx,
                        ty + head.getFont().getHeight());
            }
            g.setAlpha(old);
            g.setAntiAliased(aa);
            return true;
        });
    }

    private static String clip(String text, Font font, int room) {
        if (font.stringWidth(text) <= room) {
            return text;
        }
        String s = text;
        while (s.length() > 1 && font.stringWidth(s + "...") > room) {
            s = s.substring(0, s.length() - 1);
        }
        return s + "...";
    }

    private static float ease(float p) {
        float q = Math.max(0, Math.min(1, p));
        return q * q * (3 - 2 * q);
    }

    /// Runs `label` from `from` to `to` over `ms`, written by `format`; the
    /// last frame is exactly `to`.
    interface Format {
        String format(long value);
    }

    static void countUp(final Label label, final long from, final long to, int ms,
            final Format format) {
        final Form form = label.getComponentForm();
        if (form == null || from == to) {
            label.setText(format.format(to));
            return;
        }
        final Motion m = Motion.createEaseOutMotion(0, 1000, ms);
        m.start();
        form.registerAnimated(new Animation() {
            @Override
            public boolean animate() {
                int v = m.getValue();
                label.setText(format.format(from + (to - from) * v / 1000));
                label.getParent().revalidate();
                if (m.isFinished()) {
                    label.setText(format.format(to));
                    form.deregisterAnimated(this);
                    return false;
                }
                return true;
            }

            @Override
            public void paint(Graphics g) {
            }
        });
    }

    private static final java.util.Map<String, Long> ROLLED = new java.util.HashMap<String, Long>();

    /// A money label for the amount kept under `key` (a cart line, the
    /// cart's total) that, when the amount changed since it was last built,
    /// rolls from the old amount to the new one in 300 ms once on screen.
    static Label rolling(final String key, final long cents, String uiid, final Format format) {
        Long was = ROLLED.get(key);
        ROLLED.put(key, Long.valueOf(cents));
        final long from = was == null ? cents : was.longValue();
        return new Label(format.format(from), uiid) {
            private boolean rolled;

            @Override
            protected void initComponent() {
                super.initComponent();
                if (!rolled) {
                    rolled = true;
                    if (from != cents) {
                        countUp(this, from, cents, 300, format);
                    } else {
                        setText(format.format(cents));
                    }
                }
            }
        };
    }

    /// Pops the icon of `label`: up to 135% and back, with a small overshoot,
    /// in 320 ms. A heart on wishlist, the cart tab when something lands.
    static void pop(final Label label) {
        final Image icon = label.getIcon();
        final Form form = label.getComponentForm();
        if (icon == null || form == null) {
            return;
        }
        final int w = icon.getWidth();
        final int h = icon.getHeight();
        final long start = System.currentTimeMillis();
        final long length = 320;
        label.putClientProperty("cn1$popIcon", icon);
        form.registerAnimated(new Animation() {
            @Override
            public boolean animate() {
                long t = System.currentTimeMillis() - start;
                if (t >= length || label.getComponentForm() != form) {
                    label.setIcon(icon);
                    label.repaint();
                    form.deregisterAnimated(this);
                    return false;
                }
                float p = t / (float) length;
                // Up fast, settle back with an overshoot under 100%.
                float k = p < 0.4f ? 1 + 0.35f * (p / 0.4f)
                        : 1.35f - 0.45f * ((p - 0.4f) / 0.6f) + 0.1f * ((p - 0.4f) / 0.6f)
                        * ((p - 0.4f) / 0.6f);
                int sw = Math.max(1, (int) (w * k));
                int sh = Math.max(1, (int) (h * k));
                label.setIcon(icon.scaled(sw, sh));
                label.repaint();
                return false;
            }

            @Override
            public void paint(Graphics g) {
            }
        });
        // animate() answering false still runs every frame while registered;
        // the repaint above is what shows each step.
        label.repaint();
    }
}
