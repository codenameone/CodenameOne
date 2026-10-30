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

import com.codename1.ui.CN;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/// Portraits of the runners, rendered from the same models the run draws,
/// and the chest the reward screen opens.
public final class Portraits {
    private static final Map<String, Image> CACHE = new HashMap<String, Image>();
    private static final Map<String, Image> SOURCES = new HashMap<String, Image>();

    private Portraits() {
    }

    /// The portrait of a shop runner id (`runner-dash`, ...).
    public static String of(String runnerId) {
        if ("runner-nova".equals(runnerId)) {
            return "mage";
        }
        if ("runner-kite".equals(runnerId)) {
            return "barbarian";
        }
        if ("runner-dash".equals(runnerId) || runnerId == null) {
            return "rogue_hooded";
        }
        return "knight";
    }

    /// The portrait of the signed-in player's runner.
    public static String mine() {
        com.codenameone.examples.dash.api.PlayerDto p = com.codenameone.examples.dash.net.Session.user();
        return of(p == null ? null : p.runner());
    }

    /// A hero standing in its victory pose, `h` pixels tall.
    public static Image hero(String model, int h) {
        String key = "hero/" + model + "/" + h;
        Image img = CACHE.get(key);
        if (img == null) {
            Image src = source("hero_" + model);
            if (src == null) {
                return Image.createImage(h * 4 / 5, h, 0);
            }
            img = src.scaled(h * src.getWidth() / src.getHeight(), h);
            CACHE.put(key, img);
        }
        return img;
    }

    /// A round portrait of a hero's face, for the player's avatar and the
    /// mentor's speech bubble.
    public static Image face(String model, int size) {
        String key = "face/" + model + "/" + size;
        Image img = CACHE.get(key);
        if (img != null) {
            return img;
        }
        img = Image.createImage(size, size, 0);
        Graphics g = img.getGraphics();
        g.setAntiAliased(true);
        g.setColor(0xf0b04c);
        g.fillArc(0, 0, size - 1, size - 1, 0, 360);
        g.fillRadialGradient(0x5a6db0, 0x1f2843, 2, 2, size - 5, size - 5);
        Image src = source("hero_" + model);
        if (src != null) {
            // The head is the top third of a full-length render.
            int sw = src.getWidth() / 2;
            int sx = (src.getWidth() - sw) / 2;
            Image head = src.subImage(sx, src.getHeight() / 20, sw, sw, true).scaled(size - 6, size - 6);
            Image mask = Image.createImage(size - 6, size - 6, 0);
            Graphics mg = mask.getGraphics();
            mg.setAntiAliased(true);
            mg.setColor(0xffffff);
            mg.fillArc(0, 0, size - 7, size - 7, 0, 360);
            Object m = mask.createMask();
            g.drawImage(head.applyMask(m), 3, 3);
        }
        CACHE.put(key, img);
        return img;
    }

    /// A treasure chest, `size` pixels wide; a gold one for the rare kind.
    public static Image chest(int size, boolean gold) {
        String key = "chest/" + size + "/" + gold;
        Image img = CACHE.get(key);
        if (img != null) {
            return img;
        }
        int w = size;
        int h = size * 4 / 5;
        img = Image.createImage(w, h, 0);
        Graphics g = img.getGraphics();
        g.setAntiAliased(true);
        int wood = gold ? 0xd89a2a : 0x9a5a24;
        int dark = gold ? 0x8a5a10 : 0x5a3410;
        int band = gold ? 0xfff2b0 : 0xffd34d;
        int lid = h * 2 / 5;
        g.setColor(dark);
        g.fillRoundRect(0, lid - 2, w, h - lid + 2, w / 8, w / 8);
        g.fillLinearGradient(wood, dark, 2, lid, w - 4, h - lid - 2, false);
        g.setColor(dark);
        g.fillArc(0, 0, w, lid * 2, 0, 180);
        g.setColor(wood);
        g.fillArc(2, 2, w - 4, lid * 2 - 4, 0, 180);
        g.setColor(0xffffff);
        g.setAlpha(60);
        g.fillArc(w / 6, 4, w * 2 / 3, lid, 20, 140);
        g.setAlpha(255);
        g.setColor(band);
        int bw = Math.max(2, w / 9);
        g.fillRect(w / 5 - bw / 2, 2, bw, h - 4);
        g.fillRect(w * 4 / 5 - bw / 2, 2, bw, h - 4);
        g.fillRect(2, lid - bw / 2, w - 4, bw);
        int lk = Math.max(6, w / 5);
        g.setColor(0x2b1a07);
        g.fillRoundRect((w - lk) / 2 - 1, lid - lk / 3 - 1, lk + 2, lk + 2, lk / 3, lk / 3);
        g.setColor(band);
        g.fillRoundRect((w - lk) / 2, lid - lk / 3, lk, lk, lk / 3, lk / 3);
        g.setColor(0x2b1a07);
        g.fillArc(w / 2 - lk / 8, lid, lk / 4, lk / 4, 0, 360);
        CACHE.put(key, img);
        return img;
    }

    private static Image source(String name) {
        if (SOURCES.containsKey(name)) {
            return SOURCES.get(name);
        }
        Image img = null;
        try {
            InputStream in = CN.getResourceAsStream("/" + name + ".png");
            if (in != null) {
                try {
                    img = EncodedImage.create(in);
                } finally {
                    in.close();
                }
            }
        } catch (java.io.IOException err) {
            img = null;
        }
        SOURCES.put(name, img);
        return img;
    }
}
