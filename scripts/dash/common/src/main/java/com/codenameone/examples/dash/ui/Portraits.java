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

import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codenameone.examples.dash.run.Outfit;
import java.util.HashMap;
import java.util.Map;

/// The runners as the menus show them, and the supply crate the reward
/// screen opens. Everything is painted in code from the runner's [Outfit],
/// the same colours the run dresses its body in, so a new runner needs no
/// art at all.
public final class Portraits {
    private static final Map<String, Image> CACHE = new HashMap<String, Image>();

    private Portraits() {
    }

    /// The portrait key of a shop runner id (`runner-dash`, ...).
    public static String of(String runnerId) {
        return runnerId == null ? "runner-dash" : runnerId;
    }

    /// The portrait of the signed-in player's runner.
    public static String mine() {
        com.codenameone.examples.dash.api.PlayerDto p = com.codenameone.examples.dash.net.Session.user();
        return of(p == null ? null : p.runner());
    }

    /// A runner mid-stride, facing right, `h` pixels tall.
    public static Image figure(String runnerId, int h) {
        String key = "figure/" + runnerId + "/" + h;
        Image img = CACHE.get(key);
        if (img != null) {
            return img;
        }
        Outfit o = Outfit.of(runnerId, 0);
        int w = h * 4 / 5;
        img = Image.createImage(w, h, 0);
        Graphics g = img.getGraphics();
        g.setAntiAliased(true);
        // A soft disc of the trim colour behind the body, and speed lines.
        int cx = w / 2;
        int cy = h / 2;
        for (int r = h / 2; r > 0; r -= Math.max(1, h / 40)) {
            g.setColor(o.trim);
            g.setAlpha(Math.max(0, 22 - r * 40 / h));
            g.fillArc(cx - r, cy - r, r * 2, r * 2, 0, 360);
        }
        g.setAlpha(140);
        g.setColor(o.trim);
        for (int i = 0; i < 4; i++) {
            int y = h * (34 + i * 9) / 100;
            int len = w * (18 + (i * 7) % 15) / 100;
            g.fillRoundRect(w * 2 / 100, y, len, Math.max(2, h / 70), h / 70, h / 70);
        }
        g.setAlpha(255);
        int back = shade(o.suit, 0.62f);
        // Far limbs first, in shadow.
        bone(g, back, w, h, 38, 52, 30, 70, 7);
        bone(g, back, w, h, 30, 70, 14, 79, 6);
        bone(g, shade(o.trim, 0.7f), w, h, 10, 78, 18, 83, 5);
        bone(g, back, w, h, 46, 29, 34, 41, 5);
        bone(g, shade(o.shell, 0.7f), w, h, 34, 41, 25, 49, 4);
        // The torso, its glowing band, and the helmet.
        bone(g, o.suit, w, h, 39, 51, 47, 28, 14);
        bone(g, o.trim, w, h, 40, 44, 45, 33, 3);
        bone(g, o.shell, w, h, 37, 53, 41, 51, 9);
        g.setColor(o.shell);
        int hr = h * 9 / 100;
        g.fillArc(w * 51 / 100 - hr, h * 15 / 100 - hr, hr * 2, hr * 2, 0, 360);
        g.setColor(o.trim);
        g.fillRoundRect(w * 51 / 100, h * 13 / 100, hr + hr / 3, hr * 2 / 3, hr / 2, hr / 2);
        g.setColor(0xffffff);
        g.setAlpha(90);
        g.fillArc(w * 51 / 100 - hr * 2 / 3, h * 15 / 100 - hr * 4 / 5, hr, hr * 2 / 3, 30, 120);
        g.setAlpha(255);
        // Near limbs.
        bone(g, o.suit, w, h, 39, 52, 56, 67, 8);
        bone(g, o.suit, w, h, 56, 67, 52, 87, 7);
        bone(g, o.trim, w, h, 52, 88, 64, 89, 5);
        bone(g, o.suit, w, h, 47, 30, 58, 39, 5);
        bone(g, o.shell, w, h, 58, 39, 68, 30, 4);
        CACHE.put(key, img);
        return img;
    }

    /// A round helmet portrait, for the player's avatar and the coach's
    /// speech bubble.
    public static Image face(String runnerId, int size) {
        String key = "face/" + runnerId + "/" + size;
        Image img = CACHE.get(key);
        if (img != null) {
            return img;
        }
        Outfit o = Outfit.of(runnerId, 0);
        img = Image.createImage(size, size, 0);
        Graphics g = img.getGraphics();
        g.setAntiAliased(true);
        g.setColor(o.trim);
        g.fillArc(0, 0, size - 1, size - 1, 0, 360);
        g.fillRadialGradient(0x2a2f55, 0x0d1022, 2, 2, size - 5, size - 5);
        int r = size * 30 / 100;
        int cx = size / 2;
        int cy = size * 54 / 100;
        g.setColor(o.suit);
        g.fillArc(cx - r * 3 / 2, size * 78 / 100, r * 3, r * 2, 0, 180);
        g.setColor(o.shell);
        g.fillArc(cx - r, cy - r, r * 2, r * 2, 0, 360);
        g.setColor(o.trim);
        g.fillRoundRect(cx - r * 4 / 5, cy - r / 4, r * 8 / 5, r * 3 / 5, r / 2, r / 2);
        g.setColor(0xffffff);
        g.setAlpha(110);
        g.fillArc(cx - r * 2 / 3, cy - r * 9 / 10, r, r * 3 / 5, 30, 120);
        g.setAlpha(255);
        CACHE.put(key, img);
        return img;
    }

    /// A supply crate, `size` pixels wide, its seams lit; an amber one for
    /// the rare kind.
    public static Image crate(int size, boolean rare) {
        String key = "crate/" + size + "/" + rare;
        Image img = CACHE.get(key);
        if (img != null) {
            return img;
        }
        int w = size;
        int h = size * 4 / 5;
        int glow = rare ? 0xffc35a : 0x35e8ff;
        img = Image.createImage(w, h, 0);
        Graphics g = img.getGraphics();
        g.setAntiAliased(true);
        int arc = w / 5;
        g.setColor(glow);
        g.setAlpha(60);
        g.fillRoundRect(0, 0, w, h, arc, arc);
        g.setAlpha(255);
        g.fillLinearGradient(0x3a4166, 0x161a30, 3, 3, w - 6, h - 6, false);
        g.setColor(0x161a30);
        g.drawRoundRect(3, 3, w - 7, h - 7, arc, arc);
        // The lid seam and the clamp, lit.
        g.setColor(glow);
        int seam = h * 38 / 100;
        g.fillRect(4, seam, w - 8, Math.max(2, h / 22));
        int cw = w / 4;
        g.fillRoundRect((w - cw) / 2, seam - h / 12, cw, h / 6, h / 12, h / 12);
        // A chevron on the front, pointing the way the runners go.
        int cy = h * 68 / 100;
        int ch = h / 7;
        int[] xs = {w / 2 - ch, w / 2, w / 2 - ch, w / 2 - ch / 2, w / 2 + ch / 2, w / 2 - ch / 2};
        int[] ys = {cy - ch, cy, cy + ch, cy + ch, cy, cy - ch};
        g.setAlpha(200);
        g.fillPolygon(xs, ys, xs.length);
        g.setAlpha(255);
        g.setColor(0xffffff);
        g.setAlpha(50);
        g.fillRoundRect(6, 6, w - 12, h / 6, arc / 2, arc / 2);
        g.setAlpha(255);
        CACHE.put(key, img);
        return img;
    }

    /// A limb or the torso: a rounded bar from one point to another, both
    /// in percent of the image, `t` percent of its height thick.
    private static void bone(Graphics g, int color, int w, int h, int x1, int y1, int x2, int y2, int t) {
        float ax = w * x1 / 100f;
        float ay = h * y1 / 100f;
        float bx = w * x2 / 100f;
        float by = h * y2 / 100f;
        float r = h * t / 200f;
        float dx = bx - ax;
        float dy = by - ay;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.5f) {
            len = 0.5f;
        }
        float nx = -dy / len * r;
        float ny = dx / len * r;
        int[] xs = {Math.round(ax + nx), Math.round(bx + nx), Math.round(bx - nx), Math.round(ax - nx)};
        int[] ys = {Math.round(ay + ny), Math.round(by + ny), Math.round(by - ny), Math.round(ay - ny)};
        g.setColor(color);
        g.fillPolygon(xs, ys, 4);
        int d = Math.max(1, Math.round(r * 2));
        g.fillArc(Math.round(ax - r), Math.round(ay - r), d, d, 0, 360);
        g.fillArc(Math.round(bx - r), Math.round(by - r), d, d, 0, 360);
    }

    private static int shade(int argb, float k) {
        int r = (int) (((argb >> 16) & 0xff) * k);
        int gr = (int) (((argb >> 8) & 0xff) * k);
        int b = (int) ((argb & 0xff) * k);
        return (r << 16) | (gr << 8) | b;
    }
}
