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
        // The same faceted android the run draws: tapered segments whose
        // two halves are lit and shaded like flat panels, armour at the
        // joints, a chest wider than the waist and a helmet with a face
        // plate. Far limbs first, in shadow.
        float dim = 0.6f;
        leg(g, o, w, h, dim, 40, 53, 29, 67, 15, 76);
        arm(g, o, w, h, dim, 45, 31, 36, 41, 28, 49);
        // The torso: hips, a narrow waist, the rib cage, shoulders, neck.
        panel(g, o.suit, w, h, new float[] {35, 57, 46, 56, 46.5f, 46, 54, 37, 52, 29, 51.5f, 21, 47, 21,
            41, 29, 38.5f, 37, 37, 46}, 0.78f);
        // The pack on the back with its light, the chest plate and its
        // light, the hip guard and its belt line, the shoulder cap.
        panel(g, o.shell, w, h, new float[] {35.5f, 31, 40.5f, 30, 40, 44, 35, 43}, 0.7f);
        panel(g, o.trim, w, h, new float[] {35.5f, 33, 36.5f, 33, 36, 41, 35, 41}, 1f);
        panel(g, o.shell, w, h, new float[] {48, 32, 53.5f, 34.5f, 52, 41, 47, 40}, 0.7f);
        panel(g, o.trim, w, h, new float[] {51, 34.5f, 52, 35, 50.5f, 40.5f, 49.5f, 40.5f}, 1f);
        panel(g, o.trim, w, h, new float[] {37.5f, 43.5f, 47, 42.5f, 47, 44, 37.5f, 45}, 1f);
        panel(g, o.shell, w, h, new float[] {34.5f, 51, 46.5f, 50, 47, 57, 35, 58}, 0.7f);
        panel(g, o.trim, w, h, new float[] {34.5f, 52, 46.5f, 51, 46.5f, 52.3f, 34.5f, 53.3f}, 1f);
        // The helmet: an octagon, wider at the temples than the jaw, a
        // face plate wrapping round the front and a crest over the crown.
        int helmet = blend(o.shell, o.suit, 0.3f);
        panel(g, helmet, w, h, new float[] {47, 21, 46, 15, 47.5f, 9, 52, 6.5f, 57, 8, 59.5f, 13,
            58.5f, 19, 53, 22.5f}, 0.72f);
        panel(g, shade(o.shell, 0.8f), w, h, new float[] {48.5f, 12, 51.5f, 11.5f, 52, 17, 49, 17.5f}, 1f);
        panel(g, o.trim, w, h, new float[] {54.5f, 10.5f, 60, 11.5f, 60, 16, 55, 16.5f}, 1f);
        panel(g, o.trim, w, h, new float[] {48.5f, 7.5f, 52, 5.8f, 56.5f, 7, 52, 7.3f}, 1f);
        panel(g, shade(o.shell, 1f), w, h, new float[] {41, 27.5f, 46, 26.5f, 47, 32, 42, 33}, 0.7f);
        // Near limbs.
        leg(g, o, w, h, 1f, 41, 54, 56, 66, 52, 85);
        arm(g, o, w, h, 1f, 46, 31, 57, 41, 66, 32);
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
        // Shoulders and neck, then the faceted helmet front on: a jaw
        // narrower than the temples, the face plate across it, a crest.
        panel(g, o.suit, size, size, new float[] {14, 100, 22, 84, 40, 78, 60, 78, 78, 84, 86, 100}, 0.75f);
        panel(g, o.shell, size, size, new float[] {16, 92, 24, 82, 38, 80, 34, 92}, 0.7f);
        panel(g, o.shell, size, size, new float[] {84, 92, 76, 82, 62, 80, 66, 92}, 0.7f);
        panel(g, blend(o.shell, o.suit, 0.3f), size, size, new float[] {38, 80, 26, 66, 24, 44, 33, 26, 50, 20,
            67, 26, 76, 44, 74, 66, 62, 80}, 0.72f);
        panel(g, o.trim, size, size, new float[] {27, 46, 50, 42, 73, 46, 72, 58, 50, 62, 28, 58}, 1f);
        panel(g, o.trim, size, size, new float[] {47, 21, 53, 21, 52, 34, 48, 34}, 1f);
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

    /// A leg from the hip through the knee to the ankle: a tapered thigh
    /// with its stripe, a knee guard, a shin, and a trainer with a sole.
    private static void leg(Graphics g, Outfit o, int w, int h, float dim, float hx, float hy, float kx,
            float ky, float ax, float ay) {
        int suit = shade(o.suit, dim);
        seg(g, suit, w, h, hx, hy, kx, ky, 9f, 6f);
        seg(g, shade(o.trim, dim), w, h, hx + (kx - hx) * 0.15f, hy + (ky - hy) * 0.15f - 1.5f,
                hx + (kx - hx) * 0.7f, hy + (ky - hy) * 0.7f - 1.5f, 1.2f, 1f);
        seg(g, suit, w, h, kx, ky, ax, ay, 6f, 3.8f);
        seg(g, shade(o.shell, dim), w, h, kx + (ax - kx) * 0.15f, ky + (ay - ky) * 0.15f,
                kx + (ax - kx) * 0.7f, ky + (ay - ky) * 0.7f, 3.4f, 2.4f);
        seg(g, shade(o.shell, dim), w, h, kx - (ax - kx) * 0.08f, ky - (ay - ky) * 0.08f,
                kx + (ax - kx) * 0.08f, ky + (ay - ky) * 0.08f, 7f, 7f);
        int kicks = shade(blend(o.shell, 0xffffff, 0.8f), dim);
        panel(g, kicks, w, h, new float[] {ax - 3, ay - 2.5f, ax + 1.5f, ay - 3, ax + 10, ay + 0.5f,
            ax + 11, ay + 3, ax - 3.5f, ay + 3}, 0.8f);
        panel(g, shade(o.trim, dim), w, h, new float[] {ax - 3.5f, ay + 3, ax + 11.5f, ay + 3,
            ax + 11, ay + 4.6f, ax - 3.5f, ay + 4.6f}, 1f);
    }

    /// An arm from the shoulder through the elbow to the fist: a tapered
    /// upper arm, an elbow guard, an armoured bracer with a light band.
    private static void arm(Graphics g, Outfit o, int w, int h, float dim, float sx, float sy, float ex,
            float ey, float fx, float fy) {
        int shell = shade(o.shell, dim);
        seg(g, shade(o.suit, dim), w, h, sx, sy, ex, ey, 5.5f, 4.2f);
        seg(g, shell, w, h, ex, ey, fx, fy, 4.6f, 3.4f);
        seg(g, shade(o.trim, dim), w, h, ex + (fx - ex) * 0.4f, ey + (fy - ey) * 0.4f,
                ex + (fx - ex) * 0.5f, ey + (fy - ey) * 0.5f, 4.9f, 4.7f);
        seg(g, shell, w, h, ex - (fx - ex) * 0.1f, ey - (fy - ey) * 0.1f, ex + (fx - ex) * 0.1f,
                ey + (fy - ey) * 0.1f, 5.2f, 5.2f);
        seg(g, shell, w, h, fx - (fx - ex) * 0.05f, fy - (fy - ey) * 0.05f, fx + (fx - ex) * 0.3f,
                fy + (fy - ey) * 0.3f, 4.4f, 3.6f);
    }

    /// A tapered segment from one point to another, `t1` and `t2` percent
    /// of the image height thick at either end, with flat ends.
    private static void seg(Graphics g, int color, int w, int h, float x1, float y1, float x2, float y2, float t1,
            float t2) {
        float ax = w * x1 / 100f;
        float ay = h * y1 / 100f;
        float bx = w * x2 / 100f;
        float by = h * y2 / 100f;
        float dx = bx - ax;
        float dy = by - ay;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.5f) {
            len = 0.5f;
        }
        // Half the thickness per unit of t, in percent of each axis.
        float nx = -dy / len * h / (2f * w);
        float ny = dx / len / 2f;
        float[] p = {x1 + nx * t1, y1 + ny * t1, x2 + nx * t2, y2 + ny * t2, x2 - nx * t2, y2 - ny * t2,
            x1 - nx * t1, y1 - ny * t1};
        panel(g, color, w, h, p, 0.72f);
    }

    /// A flat-shaded panel: the polygon `pts` (x, y pairs in percent of the
    /// image) in `color`, with every edge that faces away from a light at
    /// the top right darkened to `k` from the centre out, so a shape reads
    /// as cut facets the way the run's flat-shaded meshes do.
    private static void panel(Graphics g, int color, int w, int h, float[] pts, float k) {
        int n = pts.length / 2;
        int[] xs = new int[n];
        int[] ys = new int[n];
        float cx = 0;
        float cy = 0;
        for (int i = 0; i < n; i++) {
            xs[i] = Math.round(w * pts[i * 2] / 100f);
            ys[i] = Math.round(h * pts[i * 2 + 1] / 100f);
            cx += xs[i];
            cy += ys[i];
        }
        cx /= n;
        cy /= n;
        g.setColor(color);
        g.fillPolygon(xs, ys, n);
        if (k >= 1f) {
            return;
        }
        g.setColor(shade(color, k));
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            float mx = (xs[i] + xs[j]) / 2f - cx;
            float my = (ys[i] + ys[j]) / 2f - cy;
            // Away from the light: the edge's middle lies left of or below
            // the centre.
            if (mx * 0.6f - my * 0.8f < 0f) {
                g.fillTriangle(Math.round(cx), Math.round(cy), xs[i], ys[i], xs[j], ys[j]);
            }
        }
    }

    private static int blend(int a, int b, float k) {
        int r = (int) (((a >> 16) & 0xff) * (1 - k) + ((b >> 16) & 0xff) * k);
        int gr = (int) (((a >> 8) & 0xff) * (1 - k) + ((b >> 8) & 0xff) * k);
        int bl = (int) ((a & 0xff) * (1 - k) + (b & 0xff) * k);
        return (r << 16) | (gr << 8) | bl;
    }

    private static int shade(int argb, float k) {
        int r = (int) (((argb >> 16) & 0xff) * k);
        int gr = (int) (((argb >> 8) & 0xff) * k);
        int b = (int) ((argb & 0xff) * k);
        return (r << 16) | (gr << 8) | b;
    }
}
