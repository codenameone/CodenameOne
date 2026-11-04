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
package com.codenameone.examples.dash.run;

import com.codename1.gaming.Model;
import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.Material;
import com.codename1.gpu.Mesh;
import com.codename1.gpu.Texture;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codenameone.examples.dash.rules.CityRoute;
import java.util.ArrayList;
import java.util.List;

/// What a city run shows of the real streets it follows: a sign over the
/// track with each street's name where the runner turns into it, and a
/// board on a lit column beside the track for every landmark the route
/// passes, coloured by what kind of place it is. The text is drawn with the
/// app's own font into an image and uploaded as a texture, once per sign.
final class Signage {
    /// How far ahead a sign is shown; beyond that it would be a speck.
    private static final float SHOW = 260f;
    private static final int TEX_W = 1024;
    private static final int TEX_H = 256;

    private final List<Model> models = new ArrayList<Model>();
    private final List<float[]> spans = new ArrayList<float[]>();

    Signage(GraphicsDevice d, CityRoute route, Mesh cube, Mesh quad, Mesh post, float half) {
        for (CityRoute.Sign s : route.signs()) {
            float z = -s.metres;
            int first = models.size();
            if (s.kind < 0) {
                street(d, s.text, cube, quad, post, half, z);
            } else {
                landmark(d, s, cube, quad, half, z);
            }
            spans.add(new float[] {first, models.size(), s.metres});
        }
    }

    List<Model> models() {
        return models;
    }

    /// Shows the signs between just behind the runner and [#SHOW] ahead.
    void update(float meZ) {
        for (int i = 0; i < spans.size(); i++) {
            float[] sp = spans.get(i);
            boolean on = sp[2] > meZ - 15f && sp[2] < meZ + SHOW;
            for (int m = (int) sp[0]; m < (int) sp[1]; m++) {
                models.get(m).setVisible(on);
            }
        }
    }

    /// A green street sign spanning the track on two posts.
    private void street(GraphicsDevice d, String name, Mesh cube, Mesh quad, Mesh post, float half, float z) {
        Material frame = new Material(Material.Type.PHONG).setColor(0xff3a4152).setShininess(60f);
        add(model(post, frame, -half - 0.5f, 2.3f, z, 1f, 1f, 1f));
        add(model(post, frame, half + 0.5f, 2.3f, z, 1f, 1f, 1f));
        float w = half * 2f + 0.6f;
        add(model(cube, frame, 0f, 4.75f, z + 0.06f, w + 0.25f, 1.45f, 0.1f));
        Texture t = text(d, name, null, 0xff0f6b45, 0xffffffff);
        add(model(quad, new Material(Material.Type.UNLIT).setTexture(t), 0f, 4.75f, z + 0.12f, w, 1.25f, 1f));
    }

    /// A board on a lit column at the right of the track.
    private void landmark(GraphicsDevice d, CityRoute.Sign s, Mesh cube, Mesh quad, float half, float z) {
        int c = color(s.kind);
        float x = half + 4.2f;
        Material column = new Material(Material.Type.UNLIT).setColor(c);
        Material base = new Material(Material.Type.PHONG).setColor(0xff262b38).setShininess(40f);
        add(model(cube, base, x, 0.4f, z, 1.4f, 0.8f, 1.4f));
        // The column stands behind the board, so its lit face never crosses
        // the name.
        add(model(cube, column, x, 4f, z - 0.4f, 0.35f, 7f, 0.35f));
        add(model(cube, base, x - 1.6f, 6.4f, z + 0.05f, 6.1f, 1.95f, 0.12f));
        Texture t = text(d, s.text, label(s.kind), 0xff141828, c);
        add(model(quad, new Material(Material.Type.UNLIT).setTexture(t), x - 1.6f, 6.4f, z + 0.13f, 5.9f,
                1.8f * 0.82f, 1f));
    }

    private void add(Model m) {
        models.add(m);
    }

    private static Model model(Mesh mesh, Material mat, float x, float y, float z, float sx, float sy, float sz) {
        Model m = new Model(mesh, mat);
        m.setScale(sx, sy, sz);
        m.setPosition(x, y, z);
        return m;
    }

    static String label(int kind) {
        switch (kind) {
            case CityRoute.WORSHIP:
                return "PLACE OF WORSHIP";
            case CityRoute.MUSEUM:
                return "MUSEUM";
            case CityRoute.PARK:
                return "PARK";
            case CityRoute.STATION:
                return "STATION";
            case CityRoute.MONUMENT:
                return "HISTORIC SITE";
            default:
                return "LANDMARK";
        }
    }

    static int color(int kind) {
        switch (kind) {
            case CityRoute.WORSHIP:
                return 0xffb38cff;
            case CityRoute.MUSEUM:
                return 0xffffc93a;
            case CityRoute.PARK:
                return 0xff5be37a;
            case CityRoute.STATION:
                return 0xff35e8ff;
            case CityRoute.MONUMENT:
                return 0xffff8a5a;
            default:
                return 0xffd4ff3a;
        }
    }

    /// `title` (and a small `kicker` over it) on a rounded panel, as a
    /// texture. The name shrinks until it fits the board.
    private static Texture text(GraphicsDevice d, String title, String kicker, int bg, int accent) {
        Image img = Image.createImage(TEX_W, TEX_H, 0);
        Graphics g = img.getGraphics();
        g.setAntiAliased(true);
        g.setAntiAliasedText(true);
        g.setColor(bg);
        g.fillRoundRect(0, 0, TEX_W, TEX_H, 56, 56);
        g.setColor(kicker == null ? 0xffffffff : accent);
        for (int i = 0; i < 5; i++) {
            g.drawRoundRect(6 + i, 6 + i, TEX_W - 13 - 2 * i, TEX_H - 13 - 2 * i, 44 - 2 * i, 44 - 2 * i);
        }
        int top = 26;
        if (kicker != null) {
            Font k = font(40);
            g.setFont(k);
            g.setColor(accent);
            g.drawString(kicker, 48, top);
            top += k.getHeight();
        }
        int size = kicker == null ? 128 : 92;
        Font f = font(size);
        while (f.stringWidth(title) > TEX_W - 88 && size > 44) {
            size -= 4;
            f = font(size);
        }
        g.setFont(f);
        g.setColor(kicker == null ? 0xffffffff : 0xfff2f4ff);
        int y = kicker == null ? (TEX_H - f.getHeight()) / 2 : top + (TEX_H - top - f.getHeight()) / 2 - 2;
        int x = kicker == null ? (TEX_W - f.stringWidth(title)) / 2 : 48;
        g.drawString(title, x, y);
        Texture t = d.createTexture(TEX_W, TEX_H, img.getRGB());
        t.setFilter(Texture.Filter.LINEAR);
        return t;
    }

    private static Font font(int px) {
        return Font.createTrueTypeFont("native:MainBold", "native:MainBold").derive(px, Font.STYLE_BOLD);
    }
}
