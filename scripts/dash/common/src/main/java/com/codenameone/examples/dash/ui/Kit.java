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
package com.codenameone.examples.dash.ui;

import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;

/// The game's own widgets, the pieces every menu is built from: the living
/// city behind the menus, the stage a runner stands on, the bursts of light
/// behind a reward, the pills that hold a currency, the icon tiles along the
/// sides of the home screen and the bar along the bottom.
///
/// They are ordinary components that paint themselves, so they lay out,
/// scroll and test like any other; `theme.css` styles the text around them.
public final class Kit {
    private Kit() {
    }

    // ---- The city at night, behind the menus ----

    /// A city skyline at dusk with a track running into it, its lane marks
    /// streaming towards the viewer. The still part is painted once per size
    /// into an image; each frame adds only the moving marks and the
    /// twinkling lights.
    public static Component backdrop() {
        return new Backdrop();
    }

    static final class Backdrop extends Component {
        private Image still;
        private long start = System.currentTimeMillis();
        private long last;

        Backdrop() {
            setUIID("Container");
            setFocusable(false);
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
            long now = System.currentTimeMillis();
            if (now - last < 33) {
                return false;
            }
            last = now;
            return true;
        }

        @Override
        protected Dimension calcPreferredSize() {
            return new Dimension(CN.getDisplayWidth(), CN.getDisplayHeight());
        }

        @Override
        public void paint(Graphics g) {
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) {
                return;
            }
            if (still == null || still.getWidth() != w || still.getHeight() != h) {
                still = paintStill(w, h);
            }
            g.drawImage(still, getX(), getY());
            float t = (System.currentTimeMillis() - start) / 1000f;
            int horizon = h * 44 / 100;
            int cx = getX() + w / 2;
            // Lane marks: evenly spaced in depth, so they bunch towards the
            // horizon and race apart near the bottom.
            int marks = 9;
            for (int lane = -1; lane <= 1; lane += 2) {
                for (int k = 0; k < marks; k++) {
                    float s0 = ((k + t * 0.9f) % marks) / marks;
                    float s1 = s0 + 0.035f;
                    int y0 = getY() + horizon + (int) ((h - horizon) * s0 * s0);
                    int y1 = getY() + horizon + (int) ((h - horizon) * s1 * s1);
                    int x0 = cx + (int) (lane * w * 0.11f * s0);
                    int x1 = cx + (int) (lane * w * 0.11f * s1);
                    int wd = Math.max(1, (int) (w * 0.012f * s1));
                    g.setColor(0x7cf2ff);
                    g.setAlpha((int) (60 + 160 * s0));
                    g.fillPolygon(new int[] {x0 - wd / 2, x0 + wd / 2, x1 + wd, x1 - wd},
                            new int[] {y0, y0, y1, y1}, 4);
                }
            }
            // A few lit windows breathe.
            for (int i = 0; i < 18; i++) {
                float a = (float) Math.sin(t * (0.7f + i * 0.13f) + i);
                if (a < 0.4f) {
                    continue;
                }
                int x = getX() + (int) (hash(i, 3) * w);
                int y = getY() + horizon - (int) (hash(i, 5) * h * 0.18f) - 4;
                g.setColor(0xffd98a);
                g.setAlpha((int) (a * 200));
                g.fillRect(x, y, Math.max(2, w / 220), Math.max(2, w / 220));
            }
            g.setAlpha(255);
        }
    }

    /// A small lit view down a level's track, tinted for its theme, for the
    /// level cards.
    public static Image thumb(String theme, int w, int h) {
        Image img = paintStill(w, h);
        Graphics g = img.getGraphics();
        int tint = "daybreak".equals(theme) ? 0xff9a3c : "ember".equals(theme) ? 0xff3c1e : 0x2ee6ff;
        g.setColor(tint);
        g.setAlpha(46);
        g.fillRect(0, 0, w, h);
        g.setAlpha(255);
        return img;
    }

    private static Image paintStill(int w, int h) {
        Image img = Image.createImage(w, h, 0xff0b0e1d);
        Graphics g = img.getGraphics();
        g.setAntiAliased(true);
        int horizon = h * 44 / 100;
        // Sky: deep blue overhead into violet and a warm band at the horizon.
        g.fillLinearGradient(0x070a1c, 0x3a1460, 0, 0, w, horizon * 3 / 4, false);
        g.fillLinearGradient(0x3a1460, 0xb83a7a, 0, horizon * 3 / 4, w, horizon / 4 + 1, false);
        // Stars.
        for (int i = 0; i < 70; i++) {
            g.setColor(0xffffff);
            g.setAlpha((int) (40 + hash(i, 11) * 140));
            int s = hash(i, 13) > 0.9f ? 3 : 2;
            g.fillRect((int) (hash(i, 7) * w), (int) (hash(i, 9) * horizon * 0.7f), s, s);
        }
        // The sun, low and soft, with a wide halo.
        int sr = w * 17 / 100;
        int sx = w / 2;
        int sy = horizon - sr / 3;
        for (int r = sr * 3; r > sr; r -= Math.max(2, sr / 12)) {
            g.setColor(0xff4f9a);
            g.setAlpha(Math.max(4, 40 - 36 * (r - sr) / (sr * 2)));
            g.fillArc(sx - r, sy - r, r * 2, r * 2, 0, 360);
        }
        g.setAlpha(255);
        for (int y = -sr; y < sr; y++) {
            int half = (int) Math.sqrt((double) sr * sr - (double) y * y);
            float k = (y + sr) / (2f * sr);
            g.setColor(mix(0xffe39a, 0xff4f8a, k));
            g.fillRect(sx - half, sy + y, half * 2, 1);
        }
        // Two layers of towers: the far one hazed into the sky.
        skyline(g, w, horizon, 0x4a2468, 0.20f, 31, false);
        skyline(g, w, horizon, 0x160f2e, 0.30f, 17, true);
        // The ground, and the track running into the sun.
        g.setAlpha(255);
        g.fillLinearGradient(0x1a1236, 0x06070f, 0, horizon, w, h - horizon, false);
        int cx = w / 2;
        int[] xs = {cx - w / 40, cx + w / 40, cx + w * 62 / 100, cx - w * 62 / 100};
        int[] ys = {horizon, horizon, h, h};
        g.setColor(0x141633);
        g.fillPolygon(xs, ys, 4);
        // Neon edges with a soft glow either side.
        for (int side = -1; side <= 1; side += 2) {
            int color = side < 0 ? 0xff3d9a : 0x2ee6ff;
            for (int glow = 5; glow >= 0; glow--) {
                int spread = glow * Math.max(1, w / 260);
                g.setColor(color);
                g.setAlpha(glow == 0 ? 255 : 26);
                int top = cx + side * (w / 40);
                int bottom = cx + side * (w * 62 / 100);
                g.fillPolygon(new int[] {top - spread / 4, top + spread / 4 + 1, bottom + spread + 2,
                    bottom - spread - 2}, new int[] {horizon, horizon, h, h}, 4);
            }
        }
        // A glow where the track meets the horizon.
        for (int r = w / 3; r >= 5; r -= Math.max(2, w / 60)) {
            g.setColor(0xff7ab8);
            g.setAlpha(10);
            g.fillArc(cx - r, horizon - r / 5, r * 2, r * 2 / 5, 0, 360);
        }
        g.setAlpha(255);
        return img;
    }

    private static void skyline(Graphics g, int w, int horizon, int color, float maxH, int salt, boolean windows) {
        int x = -w / 30;
        int i = 0;
        while (x < w) {
            int bw = (int) (w * (0.05f + hash(i, salt) * 0.08f));
            int bh = (int) (horizon * maxH * (0.35f + hash(i, salt + 1) * 0.65f));
            // Leave the middle low so the sun shows through.
            float mid = Math.abs((x + bw / 2f) - w / 2f) / (w / 2f);
            bh = (int) (bh * (0.35f + 0.65f * Math.min(1f, mid * 1.6f)));
            int top = horizon - bh;
            g.setColor(color);
            g.setAlpha(255);
            g.fillRect(x, top, bw, bh);
            if (hash(i, salt + 2) > 0.5f) {
                // A setback on top, and an aviation light.
                int sw = bw / 2;
                int shh = bh / 6;
                g.fillRect(x + bw / 4, top - shh, sw, shh);
                g.setColor(0xff3d5a);
                g.fillRect(x + bw / 2 - 1, top - shh - 3, 3, 3);
            }
            if (windows) {
                int cell = Math.max(3, w / 120);
                for (int wy = top + cell; wy < horizon - cell; wy += cell * 2) {
                    for (int wx = x + cell; wx < x + bw - cell; wx += cell * 2) {
                        float lit = hash(wx * 31 + wy, salt);
                        if (lit > 0.72f) {
                            g.setColor(lit > 0.9f ? 0x7cf2ff : 0xffcf7a);
                            g.setAlpha(lit > 0.9f ? 150 : 110);
                            g.fillRect(wx, wy, cell, cell);
                        }
                    }
                }
                g.setAlpha(255);
            }
            x += bw + (int) (hash(i, salt + 3) * w * 0.012f);
            i++;
        }
    }

    // ---- The runner on its stage ----

    /// The runner standing on a ring of light, breathing gently.
    public static Component stage(final Image figure) {
        Component c = new Component() {
            private long start = System.currentTimeMillis();

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
            protected Dimension calcPreferredSize() {
                return new Dimension(figure.getWidth() * 3 / 2, figure.getHeight() * 23 / 20);
            }

            @Override
            public void paint(Graphics g) {
                float t = (System.currentTimeMillis() - start) / 1000f;
                int cx = getX() + getWidth() / 2;
                int base = getY() + getHeight() - getHeight() / 12;
                int rw = getWidth() * 3 / 4;
                int rh = rw / 4;
                float pulse = 0.5f + 0.5f * (float) Math.sin(t * 2.2f);
                for (int i = 6; i >= 0; i--) {
                    int ww = rw + i * rw / 12;
                    int hh = rh + i * rh / 12;
                    g.setColor(0x2ee6ff);
                    g.setAlpha(i == 0 ? 120 : (int) (14 + pulse * 10));
                    g.fillArc(cx - ww / 2, base - hh / 2, ww, hh, 0, 360);
                }
                g.setColor(0x0b0e1d);
                g.setAlpha(200);
                g.fillArc(cx - rw * 4 / 10, base - rh * 4 / 10, rw * 8 / 10, rh * 8 / 10, 0, 360);
                g.setAlpha(255);
                int bob = (int) (Math.sin(t * 3.1f) * getHeight() / 90);
                g.drawImage(figure, cx - figure.getWidth() / 2, base - figure.getHeight() + rh / 4 + bob);
            }
        };
        c.setUIID("Container");
        return c;
    }

    // ---- Light behind a reward ----

    /// Slowly turning rays of light with `art` in the middle: the reveal
    /// behind anything the player has just won.
    public static Component burst(final Image art, final int color) {
        Component c = new Component() {
            private long start = System.currentTimeMillis();

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
            protected Dimension calcPreferredSize() {
                int s = Math.max(art.getWidth(), art.getHeight()) * 9 / 5;
                return new Dimension(s, s);
            }

            @Override
            public void paint(Graphics g) {
                float t = (System.currentTimeMillis() - start) / 1000f;
                int cx = getX() + getWidth() / 2;
                int cy = getY() + getHeight() / 2;
                int r = Math.min(getWidth(), getHeight()) / 2;
                int rays = 12;
                g.setAntiAliased(true);
                for (int i = 0; i < rays; i++) {
                    double a = t * 0.35 + i * Math.PI * 2 / rays;
                    double b = a + Math.PI / rays * 0.55;
                    g.setColor(color);
                    g.setAlpha(46);
                    g.fillPolygon(new int[] {cx, cx + (int) (Math.cos(a) * r), cx + (int) (Math.cos(b) * r)},
                            new int[] {cy, cy + (int) (Math.sin(a) * r), cy + (int) (Math.sin(b) * r)}, 3);
                }
                for (int i = 5; i > 0; i--) {
                    int rr = r * i / 7;
                    g.setAlpha(18);
                    g.fillArc(cx - rr, cy - rr, rr * 2, rr * 2, 0, 360);
                }
                g.setAlpha(255);
                float pop = Math.min(1f, t * 4f);
                float s = pop < 1f ? 0.6f + 0.5f * pop : 1f + 0.03f * (float) Math.sin(t * 3);
                int aw = (int) (art.getWidth() * s);
                int ah = (int) (art.getHeight() * s);
                g.drawImage(art, cx - aw / 2, cy - ah / 2, aw, ah);
            }
        };
        c.setUIID("Container");
        return c;
    }

    /// `of` stars, the first `earned` lit, each popping in after the last.
    public static Component stars(int earned, int of) {
        return stars(earned, of, 12f);
    }

    /// As [#stars(int, int)], each star `mm` millimetres across.
    public static Component stars(final int earned, final int of, final float mm) {
        Component c = new Component() {
            private long start = System.currentTimeMillis();

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
                return System.currentTimeMillis() - start < 2500;
            }

            @Override
            protected Dimension calcPreferredSize() {
                int s = CN.convertToPixels(mm);
                return new Dimension(s * of + s / 2, s * 6 / 5);
            }

            @Override
            public void paint(Graphics g) {
                float t = (System.currentTimeMillis() - start) / 1000f;
                int s = getHeight() * 5 / 6;
                int gap = (getWidth() - s * of) / Math.max(1, of + 1);
                g.setAntiAliased(true);
                for (int i = 0; i < of; i++) {
                    int x = getX() + gap + i * (s + gap) + s / 2;
                    // The middle star sits higher and larger, as on a podium.
                    boolean middle = of == 3 && i == 1;
                    int size = middle ? s : s * 4 / 5;
                    int y = getY() + getHeight() / 2 + (middle ? -s / 10 : s / 12);
                    star(g, x, y, size / 2, 0x2a3060, 255);
                    if (i < earned) {
                        float k = Math.max(0f, Math.min(1f, (t - 0.3f - i * 0.35f) * 4f));
                        if (k > 0f) {
                            float over = k < 1f ? k * 1.25f : 1f;
                            for (int gl = 3; gl > 0; gl--) {
                                star(g, x, y, (int) (size / 2 * over) + gl * size / 14, 0xffd23a, 40);
                            }
                            star(g, x, y, (int) (size / 2 * over), 0xffd23a, 255);
                            star(g, x, y - size / 14, (int) (size / 2 * over * 0.55f), 0xfff2a8, 200);
                        }
                    }
                }
                g.setAlpha(255);
            }
        };
        c.setUIID("Container");
        return c;
    }

    /// Three stars painted into an image, `earned` of them lit, for rows.
    public static Image starsImage(int earned, int height) {
        Image img = Image.createImage(height * 3 + height / 2, height, 0);
        Graphics g = img.getGraphics();
        g.setAntiAliased(true);
        for (int i = 0; i < 3; i++) {
            int cx = height / 2 + i * (height + height / 4);
            star(g, cx, height / 2 + height / 20, height / 2, i < earned ? 0xffd23a : 0x2a3060, 255);
        }
        return img;
    }

    private static void star(Graphics g, int cx, int cy, int r, int color, int alpha) {
        int[] xs = new int[10];
        int[] ys = new int[10];
        for (int i = 0; i < 10; i++) {
            double a = -Math.PI / 2 + i * Math.PI / 5;
            int rr = i % 2 == 0 ? r : r * 45 / 100;
            xs[i] = cx + (int) (Math.cos(a) * rr);
            ys[i] = cy + (int) (Math.sin(a) * rr);
        }
        g.setColor(color);
        g.setAlpha(alpha);
        g.fillPolygon(xs, ys, 10);
    }

    /// Counts a label up from 0 to `to` over a second, with `prefix` before.
    public static Label countUp(final String prefix, final int to, String uiid) {
        final Label l = new Label(prefix + to, uiid);
        final long start = System.currentTimeMillis();
        // Sized for the final number, so counting never truncates or jumps.
        l.setPreferredSize(l.getPreferredSize());
        l.setText(prefix + "0");
        CN.callSerially(new Runnable() {
            public void run() {
                if (l.getComponentForm() == null) {
                    l.setText(prefix + to);
                    return;
                }
                float k = Math.min(1f, (System.currentTimeMillis() - start) / 1100f);
                k = 1f - (1f - k) * (1f - k);
                l.setText(prefix + (int) (to * k));
                l.repaint();
                if (k < 1f) {
                    com.codename1.ui.util.UITimer.timer(30, false, l.getComponentForm(), this);
                }
            }
        });
        return l;
    }

    // ---- Currency, tiles and the bottom bar ----

    /// A currency pill: icon, amount and a plus that goes to the shop.
    public static Container pill(char icon, int amount, String uiid, String name,
            ActionListener<ActionEvent> plus) {
        Container c = new Container(new BorderLayout());
        c.setUIID("CurrencyPill");
        Label l = new Label(String.valueOf(amount), uiid);
        l.setMaterialIcon(icon, 3f);
        l.setName(name);
        c.add(BorderLayout.CENTER, l);
        if (plus != null) {
            Button b = new Button("", "PillPlus");
            b.setMaterialIcon(FontImage.MATERIAL_ADD, 2.4f);
            b.addActionListener(plus);
            c.add(BorderLayout.EAST, FlowLayout.encloseMiddle(b));
        }
        return c;
    }

    /// An icon tile for the side rails of the home screen, with a red count
    /// in its corner while there is something waiting behind it.
    public static Button tile(String text, char icon, String name, final int badge,
            ActionListener<ActionEvent> go) {
        Button b = new Button(text, "RailTile") {
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                if (badge <= 0) {
                    return;
                }
                int d = CN.convertToPixels(3.4f);
                int x = getX() + getWidth() - d - getStyle().getPaddingRightNoRTL() / 2;
                int y = getY() + getStyle().getPaddingTop() / 2;
                g.setAntiAliased(true);
                g.setColor(0xff3b5c);
                g.fillArc(x, y, d, d, 0, 360);
                g.setColor(0xffffff);
                g.setFont(com.codename1.ui.Font.createSystemFont(com.codename1.ui.Font.FACE_SYSTEM,
                        com.codename1.ui.Font.STYLE_BOLD, com.codename1.ui.Font.SIZE_SMALL));
                String s = badge > 9 ? "9+" : String.valueOf(badge);
                g.drawString(s, x + (d - g.getFont().stringWidth(s)) / 2, y + (d - g.getFont().getHeight()) / 2);
            }
        };
        b.setName(name);
        b.setMaterialIcon(icon, 3.4f);
        b.setTextPosition(Component.BOTTOM);
        if (go != null) {
            b.addActionListener(go);
        }
        return b;
    }

    /// The bar along the bottom of the main screens, `active` highlighted.
    public static Container navBar(String active, boolean admin) {
        Container bar = new Container(new GridLayout(1, admin ? 5 : 4));
        bar.setUIID("NavBar");
        bar.setSafeArea(true);
        bar.add(navItem("Home", FontImage.MATERIAL_HOME, "homeNav", active, e -> Nav.home()));
        bar.add(navItem("Levels", FontImage.MATERIAL_ROUTE, "levelsNav", active, e -> LevelsForm.show()));
        bar.add(navItem("Editor", FontImage.MATERIAL_EDIT_ROAD, "forgeButton", active,
                e -> com.codenameone.examples.dash.forge.ForgeHome.show()));
        bar.add(navItem("Shop", FontImage.MATERIAL_STOREFRONT, "shopButton", active, e -> ShopForm.show()));
        if (admin) {
            bar.add(navItem("Admin", FontImage.MATERIAL_ADMIN_PANEL_SETTINGS, "adminButton", active,
                    e -> Nav.adminConsole()));
        }
        return bar;
    }

    private static Button navItem(String text, char icon, String name, String active,
            ActionListener<ActionEvent> go) {
        boolean on = name.equals(active);
        Button b = new Button(text, on ? "NavItemOn" : "NavItem");
        b.setName(name);
        b.setMaterialIcon(icon, 4.4f);
        b.setTextPosition(Component.BOTTOM);
        if (!on) {
            b.addActionListener(go);
        }
        return b;
    }

    /// A round avatar with the progress to the next level drawn around it.
    public static Component xpRing(final Image face, final int value, final int max) {
        Component c = new Component() {
            @Override
            protected Dimension calcPreferredSize() {
                int s = face.getWidth() + CN.convertToPixels(2.4f);
                return new Dimension(s, s);
            }

            @Override
            public void paint(Graphics g) {
                int s = Math.min(getWidth(), getHeight());
                int x = getX() + (getWidth() - s) / 2;
                int y = getY() + (getHeight() - s) / 2;
                int th = CN.convertToPixels(0.9f);
                g.setAntiAliased(true);
                g.setColor(0x232a4d);
                g.fillArc(x, y, s, s, 0, 360);
                g.setColor(0xd4ff3a);
                int sweep = max <= 0 ? 0 : 360 * Math.max(0, Math.min(value, max)) / max;
                g.fillArc(x, y, s, s, 90, -sweep);
                g.setColor(0x0b0e1d);
                g.fillArc(x + th, y + th, s - th * 2, s - th * 2, 0, 360);
                g.drawImage(face, x + (s - face.getWidth()) / 2, y + (s - face.getHeight()) / 2);
            }
        };
        c.setUIID("Container");
        return c;
    }

    /// A thin column of tiles for one side of the home screen.
    public static Container rail(Component... tiles) {
        Container c = new Container(BoxLayout.y());
        c.setUIID("Rail");
        for (Component t : tiles) {
            c.add(t);
        }
        return c;
    }

    /// The picture on a shop card: the runner's helmet, the trail's streak,
    /// or a heap of gems, on a lit rounded tile.
    public static Image shopArt(String id, int size) {
        if (id.indexOf("pass") >= 0) {
            // A pass unlocks the golden trail, so it shows one.
            id = "trail-gold";
        }
        Image img = Image.createImage(size, size, 0);
        Graphics g = img.getGraphics();
        g.setAntiAliased(true);
        int glow = id.startsWith("trail-") ? trailColor(id) : id.startsWith("gems-") ? 0x2ee6ff : 0xb56cff;
        g.setColor(0x161a3a);
        g.fillRoundRect(0, 0, size, size, size / 3, size / 3);
        for (int r = size / 2; r > size / 8; r -= Math.max(1, size / 24)) {
            g.setColor(glow);
            g.setAlpha(14);
            g.fillArc(size / 2 - r, size / 2 - r, r * 2, r * 2, 0, 360);
        }
        g.setAlpha(255);
        if (id.startsWith("runner-")) {
            Image face = Portraits.face(id, size * 3 / 4);
            g.drawImage(face, (size - face.getWidth()) / 2, (size - face.getHeight()) / 2);
        } else if (id.startsWith("trail-")) {
            // A comet: a bright head trailing a fading tail.
            for (int i = 0; i < 14; i++) {
                int x = size * 3 / 4 - i * size / 22;
                int y = size / 4 + i * size / 22;
                int rr = Math.max(2, size / 7 - i * size / 120);
                g.setColor(glow);
                g.setAlpha(Math.max(20, 230 - i * 16));
                g.fillArc(x - rr, y - rr, rr * 2, rr * 2, 0, 360);
            }
            g.setColor(0xffffff);
            g.setAlpha(255);
            g.fillArc(size * 3 / 4 - size / 14, size / 4 - size / 14, size / 7, size / 7, 0, 360);
        } else {
            int n = id.endsWith("500") ? 5 : 2;
            for (int i = 0; i < n; i++) {
                int cx = size / 2 + (i - (n - 1) / 2) * size / 6 + (i % 2) * size / 20;
                int cy = size / 2 + (i % 2 == 0 ? size / 10 : -size / 14);
                gem(g, cx, cy, size / 5);
            }
        }
        g.setAlpha(255);
        return img;
    }

    private static int trailColor(String id) {
        if (id.endsWith("gold")) {
            return 0xffd23a;
        }
        if (id.endsWith("comet")) {
            return 0x7cf2ff;
        }
        if (id.endsWith("spark")) {
            return 0xff5ab4;
        }
        return 0xd4ff3a;
    }

    private static void gem(Graphics g, int cx, int cy, int r) {
        g.setAlpha(255);
        g.setColor(0x1aa8d8);
        g.fillPolygon(new int[] {cx - r, cx + r, cx}, new int[] {cy - r / 3, cy - r / 3, cy + r}, 3);
        g.setColor(0x7cf2ff);
        g.fillPolygon(new int[] {cx - r, cx - r / 2, cx + r / 2, cx + r},
                new int[] {cy - r / 3, cy - r, cy - r, cy - r / 3}, 4);
        g.setColor(0xd8fdff);
        g.fillPolygon(new int[] {cx - r / 2, cx, cx - r / 4}, new int[] {cy - r, cy - r / 3, cy - r / 3}, 3);
    }

    static int mix(int a, int b, float k) {
        int r = (int) (((a >> 16) & 0xff) * (1 - k) + ((b >> 16) & 0xff) * k);
        int g = (int) (((a >> 8) & 0xff) * (1 - k) + ((b >> 8) & 0xff) * k);
        int bl = (int) ((a & 0xff) * (1 - k) + (b & 0xff) * k);
        return (r << 16) | (g << 8) | bl;
    }

    static float hash(int i, int salt) {
        int h = i * 73856093 ^ salt * 19349663;
        h ^= h >>> 13;
        h *= 0x5bd1e995;
        h ^= h >>> 15;
        return (h & 0xffff) / 65535f;
    }
}
