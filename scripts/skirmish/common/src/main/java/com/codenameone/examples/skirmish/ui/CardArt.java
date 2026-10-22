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
package com.codenameone.examples.skirmish.ui;

import com.codename1.ui.CN;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codenameone.examples.skirmish.rules.CardDef;
import com.codenameone.examples.skirmish.rules.CardSet;
import com.codenameone.examples.skirmish.rules.Cards;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/// The pictures of the game outside the arena: framed cards and full-length
/// heroes.
///
/// The portraits are renders of the same 3D models the battle uses
/// (`card_<id>.png`, `hero_<model>.png`, made by `PortraitStudio`), so a card
/// looks like the unit it puts on the field. The two spells have no model and
/// are painted here. Everything is drawn once per size and cached.
// tag::skirmish-card-art[]
public final class CardArt {
    /// The heroes a player can be shown as, and the mentor of the tutorial.
    public static final String[] HEROES = {"knight", "barbarian", "mage", "rogue_hooded"};
    public static final String MENTOR = "mage";

    private static final Map<String, Image> CACHE = new HashMap<String, Image>();
    private static final Map<String, Image> SOURCES = new HashMap<String, Image>();

    /// The cards being drawn: the game's own, or a Forge pack's with its
    /// custom cards added.
    private static CardSet cards = CardSet.BUILTIN;

    private CardArt() {
    }

    /// Draws `set`'s cards from now on; a battle on a Forge pack sets it, and
    /// leaving sets the game's own again.
    public static void use(CardSet set) {
        cards = set == null ? CardSet.BUILTIN : set;
    }

    static CardDef def(String cardId) {
        CardDef d = cards.get(cardId);
        return d != null ? d : Cards.get(cardId);
    }

    /// A card `w` pixels wide (and 1.3 times as tall): portrait, frame in
    /// the colour of its kind, elixir drop with the cost and a name ribbon.
    public static Image card(String cardId, int w) {
        return card(cardId, w, true);
    }

    /// The same card, without the name ribbon when it is too small to read.
    public static Image card(String cardId, int w, boolean named) {
        CardDef def = def(cardId);
        if (def == null) {
            return Image.createImage(w, w * 13 / 10, 0);
        }
        // A custom card's id is only unique inside its pack: the key carries
        // what is drawn, so two packs' "golem" never share a picture.
        String key = "card/" + cardId + "/" + w + "/" + named + "/" + def.name + "/" + def.cost + "/" + def.model();
        Image img = CACHE.get(key);
        if (img != null) {
            return img;
        }
        int h = w * 13 / 10;
        img = Image.createImage(w, h, 0);
        Graphics g = img.getGraphics();
        g.setAntiAliased(true);
        int r = Math.max(4, w / 8);
        int rim = frameColor(def);
        g.setColor(0x000000);
        g.setAlpha(110);
        g.fillRoundRect(1, 2, w - 1, h - 2, r, r);
        g.setAlpha(255);
        g.setColor(rim);
        g.fillRoundRect(0, 0, w - 1, h - 2, r, r);
        int in = Math.max(2, w / 22);
        g.fillLinearGradient(backTop(def), backBottom(def), in, in, w - 2 * in, h - 2 * in - 2, false);
        // A sheen along the top of the frame, the way light catches a gilt edge.
        g.setColor(0xffffff);
        g.setAlpha(70);
        g.fillRoundRect(in, in, w - 2 * in, h / 7, r / 2, r / 2);
        g.setAlpha(255);
        portrait(g, def, in, in, w - 2 * in, h - 2 * in - 2);
        if (named) {
            int ribbon = Math.max(10, h / 6);
            int ry = h - 2 - in - ribbon;
            g.setColor(0x000000);
            g.setAlpha(150);
            g.fillRect(in, ry, w - 2 * in, ribbon);
            g.setAlpha(255);
            Font f = font(Math.max(8, ribbon * 2 / 3), true);
            g.setFont(f);
            g.setColor(0xffffff);
            String name = def.name;
            while (f.stringWidth(name) > w - 2 * in - 4 && name.length() > 3) {
                name = name.substring(0, name.length() - 1);
            }
            g.drawString(name, (w - f.stringWidth(name)) / 2, ry + (ribbon - f.getHeight()) / 2);
        }
        drop(g, 0, 0, Math.max(14, w * 2 / 7), def.cost);
        CACHE.put(key, img);
        return img;
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

    /// The hero a player is shown as: theirs to keep, chosen from the name.
    public static String heroOf(String username) {
        if (username == null) {
            return HEROES[0];
        }
        return HEROES[(username.hashCode() & 0x7fffffff) % HEROES.length];
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

    /// A drop of elixir with the cost in it.
    static void drop(Graphics g, int x, int y, int size, int cost) {
        g.setColor(0x4a0f5c);
        g.fillArc(x, y, size, size, 0, 360);
        g.fillRadialGradient(0xf7a8ff, 0xa020c8, x + 2, y + 2, size - 4, size - 4);
        g.setColor(0xffffff);
        g.setAlpha(120);
        g.fillArc(x + size / 4, y + size / 6, size / 4, size / 5, 0, 360);
        g.setAlpha(255);
        Font f = font(size * 3 / 5, true);
        g.setFont(f);
        String s = String.valueOf(cost);
        g.setColor(0x2b0a35);
        g.drawString(s, x + (size - f.stringWidth(s)) / 2 + 1, y + (size - f.getHeight()) / 2 + 1);
        g.setColor(0xffffff);
        g.drawString(s, x + (size - f.stringWidth(s)) / 2, y + (size - f.getHeight()) / 2);
    }

    private static void portrait(Graphics g, CardDef def, int x, int y, int w, int h) {
        String cardId = def.id;
        if ("fire-orb".equals(cardId)) {
            fireOrb(g, x, y, w, h);
            return;
        }
        if ("arrow-rain".equals(cardId)) {
            arrowRain(g, x, y, w, h);
            return;
        }
        Image src = source("card_" + cardId);
        if (src == null && def.model() != null && !def.model().startsWith("asset:")) {
            // A Forge card on a bundled character wears that hero's picture.
            src = source("hero_" + def.model());
        }
        if (src == null) {
            initial(g, def, x, y, w, h);
            return;
        }
        int[] clip = g.getClip();
        g.clipRect(x, y, w, h);
        int dw = w;
        int dh = w * src.getHeight() / src.getWidth();
        g.drawImage(src.scaled(dw, dh), x, y + h - dh);
        g.setClip(clip);
    }

    /// A card with no picture of its own: its initial, large, on the frame.
    private static void initial(Graphics g, CardDef def, int x, int y, int w, int h) {
        String letter = def.name.length() == 0 ? "?" : def.name.substring(0, 1);
        Font f = font(Math.max(10, h / 2), true);
        g.setFont(f);
        g.setColor(0xffffff);
        g.setAlpha(200);
        g.drawString(letter, x + (w - f.stringWidth(letter)) / 2, y + (h - f.getHeight()) / 2);
        g.setAlpha(255);
    }

    private static void fireOrb(Graphics g, int x, int y, int w, int h) {
        int cx = x + w / 2;
        int cy = y + h * 11 / 20;
        int[] glow = {0x401000, 0x8a2a00, 0xd85a10, 0xffa030, 0xffe08a, 0xffffff};
        int r = w * 9 / 20;
        for (int i = 0; i < glow.length; i++) {
            int rr = r - i * r / (glow.length + 1);
            g.setColor(glow[i]);
            g.setAlpha(i == 0 ? 120 : 255);
            g.fillArc(cx - rr, cy - rr, rr * 2, rr * 2, 0, 360);
        }
        g.setAlpha(255);
        // Tongues of flame trailing up and to the left.
        g.setColor(0xffa030);
        for (int i = 0; i < 4; i++) {
            int fx = cx - r / 2 - i * w / 14;
            int fy = cy - r / 2 - i * h / 12;
            int fs = r / 2 - i * r / 10;
            g.fillArc(fx - fs / 2, fy - fs / 2, fs, fs, 0, 360);
        }
    }

    private static void arrowRain(Graphics g, int x, int y, int w, int h) {
        g.fillLinearGradient(0x8fc4e8, 0x2f5a7a, x, y, w, h, false);
        int[] xs = {w / 6, w / 2, w * 5 / 6, w / 3, w * 2 / 3};
        int[] ys = {h / 6, h / 10, h / 5, h / 2, h * 9 / 20};
        int len = h / 3;
        for (int i = 0; i < xs.length; i++) {
            int ax = x + xs[i];
            int ay = y + ys[i];
            int tx = ax + len / 4;
            int ty = ay + len;
            g.setColor(0x6a4320);
            for (int t = -1; t <= 1; t++) {
                g.drawLine(ax + t, ay, tx + t, ty);
            }
            g.setColor(0xd8dde8);
            int hs = Math.max(3, w / 14);
            g.fillTriangle(tx - hs / 2 - 1, ty - hs, tx + hs / 2 + 1, ty - hs + 1, tx + 1, ty + hs / 2);
            g.setColor(0xe0453a);
            g.fillTriangle(ax - hs / 2, ay - hs / 2, ax + hs / 2, ay - hs / 2, ax, ay + hs / 2);
        }
    }

    private static int frameColor(CardDef def) {
        if (def.kind == CardDef.SPELL) {
            return 0xb04ad8;
        }
        if (def.kind == CardDef.BUILDING) {
            return 0x9a6a3a;
        }
        return def.cost >= 5 ? 0xffc93a : 0xd8dde8;
    }

    private static int backTop(CardDef def) {
        return def.kind == CardDef.SPELL ? 0x5a2a78 : def.kind == CardDef.BUILDING ? 0x6a5040 : 0x5a8ad8;
    }

    private static int backBottom(CardDef def) {
        return def.kind == CardDef.SPELL ? 0x241038 : def.kind == CardDef.BUILDING ? 0x2a1c14 : 0x1f2f5a;
    }

    static Font font(int px, boolean bold) {
        if (Font.isNativeFontSchemeSupported()) {
            return Font.createTrueTypeFont(bold ? "native:MainBold" : "native:MainRegular",
                    bold ? "native:MainBold" : "native:MainRegular").derive(px, Font.STYLE_PLAIN);
        }
        return Font.createSystemFont(Font.FACE_SYSTEM, bold ? Font.STYLE_BOLD : Font.STYLE_PLAIN,
                Font.SIZE_MEDIUM);
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
// end::skirmish-card-art[]
