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
package com.codenameone.examples.papercut.play;

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.geom.Rectangle;

/// What is written over the page, as if in the margin with a felt pen: the
/// score, the multiplier, the quills and erasers left, and the two thumb
/// sticks while they are held.
///
/// It is a transparent component layered above the [ArenaView] and painted
/// with `Graphics`, which is the simplest way to put text over a GPU view.
public final class Hud extends Component {
    private final ArenaView view;
    private Font big;
    private Font small;
    private Font tiny;
    private boolean showSticks = com.codename1.ui.Display.getInstance().isTouchScreenDevice() && !CN.isDesktop();
    private String banner;
    private long bannerUntil;

    public Hud(ArenaView view) {
        this.view = view;
        setFocusable(false);
        getAllStyles().setBgTransparency(0);
    }

    /// A word across the page for a second or two: "Daily sheet", "+1 quill".
    public void banner(String text) {
        banner = text;
        bannerUntil = System.currentTimeMillis() + 1800;
    }

    public void setShowSticks(boolean b) {
        showSticks = b;
    }

    @Override
    public void paint(Graphics g) {
        Arena a = view.getArena();
        if (a == null) {
            return;
        }
        if (big == null) {
            big = Art.title(5.2f);
            small = Art.title(3.4f);
            tiny = Art.body(2.6f);
        }
        boolean aa = g.isAntiAliased();
        g.setAntiAliased(true);
        int x0 = getX();
        int y0 = getY();
        Rectangle safe = getComponentForm() == null ? null : getComponentForm().getSafeArea();
        int left = x0 + CN.convertToPixels(4);
        int top = y0 + CN.convertToPixels(2.5f);
        if (safe != null && safe.getWidth() > 0) {
            left = Math.max(left, safe.getX() + CN.convertToPixels(2));
            top = Math.max(top, safe.getY() + CN.convertToPixels(1));
        }

        // Score, in ink, with the multiplier circled beside it.
        String score = format(a.score);
        g.setFont(big);
        ink(g, score, left, top, 0x262c42);
        int sw = big.stringWidth(score);
        g.setFont(small);
        String mult = "x" + a.multiplier;
        int mx = left + sw + CN.convertToPixels(3);
        int my = top + (big.getHeight() - small.getHeight()) / 2;
        int mw = small.stringWidth(mult) + CN.convertToPixels(3);
        g.setColor(0xe0706a);
        g.setAlpha(220);
        for (int i = 0; i < 2; i++) {
            g.drawArc(mx - CN.convertToPixels(1.5f) + i, my - CN.convertToPixels(0.6f) + i, mw,
                    small.getHeight() + CN.convertToPixels(1.2f), 0, 360);
        }
        g.setAlpha(255);
        ink(g, mult, mx, my, 0xc0443c);

        // Quills and erasers left, under the score.
        int iy = top + big.getHeight() + CN.convertToPixels(0.5f);
        int icon = CN.convertToPixels(4.5f);
        Image quill = Art.get("quill");
        for (int i = 0; i < Math.min(a.lives, 8); i++) {
            g.drawImage(quill, left + i * (icon + 4), iy, icon, icon);
        }
        if (a.mode != Arena.FOLD) {
            Image eraser = Art.get("p_eraser");
            int ex = left + Math.min(a.lives, 8) * (icon + 4) + CN.convertToPixels(3);
            for (int i = 0; i < Math.min(a.bombs, 8); i++) {
                g.drawImage(eraser, ex + i * (icon + 4), iy + icon / 6, icon, icon * 5 / 7);
            }
        } else {
            g.setFont(tiny);
            ink(g, a.gates + " gates", left + icon + CN.convertToPixels(3), iy, 0x2f6fa0);
        }

        // The run's clock, top centre, small.
        g.setFont(tiny);
        int secs = (int) a.time;
        String clock = secs / 60 + ":" + (secs % 60 < 10 ? "0" : "") + secs % 60;
        ink(g, clock, x0 + (getWidth() - tiny.stringWidth(clock)) / 2, top, 0x5a6070);

        if (showSticks) {
            drawEraserButton(g, a);
            drawStick(g, view.move, 0x6f9fd8);
            drawStick(g, view.aim, 0xe0706a);
        }
        if (banner != null && System.currentTimeMillis() < bannerUntil) {
            g.setFont(big);
            int bw = big.stringWidth(banner);
            ink(g, banner, x0 + (getWidth() - bw) / 2, y0 + getHeight() / 3, 0x262c42);
        }
        g.setAntiAliased(aa);
    }

    private void drawEraserButton(Graphics g, Arena a) {
        if (a.mode == Arena.FOLD) {
            return;
        }
        float[] b = view.eraserButton();
        int r = (int) b[2];
        int cx = getX() + (int) b[0];
        int cy = getY() + (int) b[1];
        g.setColor(0xfaf7ee);
        g.setAlpha(a.bombs > 0 ? 200 : 90);
        g.fillArc(cx - r, cy - r, r * 2, r * 2, 0, 360);
        g.setColor(0x262c42);
        g.drawArc(cx - r, cy - r, r * 2, r * 2, 0, 360);
        g.setAlpha(a.bombs > 0 ? 255 : 110);
        Image eraser = Art.get("p_eraser");
        g.drawImage(eraser, cx - r * 2 / 3, cy - r / 2, r * 4 / 3, r);
        g.setAlpha(255);
    }

    private void drawStick(Graphics g, ArenaView.Stick s, int colour) {
        if (!s.active) {
            return;
        }
        int r = (int) view.stickRadius();
        int cx = getX() + (int) s.cx;
        int cy = getY() + (int) s.cy;
        g.setColor(colour);
        g.setAlpha(110);
        g.drawArc(cx - r, cy - r, r * 2, r * 2, 0, 360);
        g.drawArc(cx - r + 1, cy - r + 1, r * 2 - 2, r * 2 - 2, 0, 360);
        float dx = s.x - s.cx;
        float dy = s.y - s.cy;
        float d = (float) Math.sqrt(dx * dx + dy * dy);
        if (d > r) {
            dx = dx / d * r;
            dy = dy / d * r;
        }
        int k = r / 3;
        g.setAlpha(170);
        g.fillArc(cx + (int) dx - k, cy + (int) dy - k, k * 2, k * 2, 0, 360);
        g.setAlpha(255);
    }

    /// Text with a faint offset copy under it: felt pen soaking into paper.
    private static void ink(Graphics g, String s, int x, int y, int colour) {
        g.setColor(colour);
        g.setAlpha(50);
        g.drawString(s, x + 1, y + 2);
        g.setAlpha(255);
        g.drawString(s, x, y);
    }

    /// 1234567 as "1,234,567".
    public static String format(long n) {
        String s = Long.toString(n);
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (i > 0 && (s.length() - i) % 3 == 0) {
                b.append(',');
            }
            b.append(s.charAt(i));
        }
        return b.toString();
    }
}
