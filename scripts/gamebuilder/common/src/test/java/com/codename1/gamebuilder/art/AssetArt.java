/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.gamebuilder.art;

import com.codename1.gaming.level.AssetCatalog;
import com.codename1.gaming.level.AssetDef;
import com.codename1.gaming.level.AssetPack;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;

import java.util.List;

/// Generates recognizable sprite art for the starter packs procedurally (art "translated
/// to code") and registers it on the `AssetCatalog`.
///
/// This is the same approach the gaming samples use -- draw with `Graphics` into a mutable
/// `Image` -- so it runs identically on every platform with no SVG/Lottie transcode
/// dependency. The `EditorCanvas` and `com.codename1.gaming.level.GameSceneView` both pick
/// these up automatically once installed (they prefer `AssetCatalog#image(String)` over the
/// flat-color placeholder). Requires a live `Display`, so call it from the UI, not from
/// headless code.
public final class AssetArt {
    private AssetArt() {
    }

    /// Draws and registers an image for every asset in every pack of the catalog.
    public static void install(AssetCatalog catalog) {
        List<AssetPack> packs = catalog.packs();
        for (int p = 0; p < packs.size(); p++) {
            List<AssetDef> defs = packs.get(p).assets();
            for (int i = 0; i < defs.size(); i++) {
                AssetDef def = defs.get(i);
                if (!catalog.hasImage(def.getId())) {
                    catalog.setImage(def.getId(), render(def));
                }
            }
        }
    }

    /// Draws a single asset to a transparent image sized from its def.
    public static Image render(AssetDef def) {
        int w = Math.max(8, def.getWidth());
        int h = Math.max(8, def.getHeight());
        Image img = Image.createImage(w, h, 0);
        Graphics g = img.getGraphics();
        g.setAntiAliased(true);
        int c = def.getColor() & 0xffffff;
        switch (def.getId()) {
            case "ground", "floor", "path", "boardtile", "start" -> ground(g, w, h, c);
            case "grass" -> grass(g, w, h, c);
            case "brick" -> brick(g, w, h, c);
            case "crate", "crate3d" -> crate(g, w, h, c);
            case "chest" -> chest(g, w, h, c);
            case "ladder" -> ladder(g, w, h, c);
            case "spike" -> spike(g, w, h, c);
            case "water" -> water(g, w, h, c);
            case "wall", "rock", "block", "pillar" -> block(g, w, h, c);
            case "tree", "tree3d" -> tree(g, w, h, c);
            case "cloud" -> cloud(g, w, h, c);
            case "mountain" -> mountain(g, w, h, c);
            case "hill" -> hill(g, w, h, c);
            case "coin" -> coin(g, w, h, c);
            case "coffee" -> coffee(g, w, h, c);
            case "exception", "bug" -> exceptionMonster(g, w, h, c);
            case "gem" -> gem(g, w, h, c);
            case "player", "hero", "spawn", "npc" -> figure(g, w, h, c);
            case "slime" -> slime(g, w, h, c);
            case "door" -> door(g, w, h, c);
            case "flag" -> flag(g, w, h, c);
            case "torch" -> torch(g, w, h, c);
            case "card" -> card(g, w, h, c);
            case "token" -> token(g, w, h, c);
            case "dice" -> dice(g, w, h, c);
            case "barrel" -> barrel(g, w, h, c);
            default -> chip(g, w, h, c);
        }
        return img;
    }

    // ---- helpers -------------------------------------------------------------

    private static int shade(int rgb, double f) {
        int r = clamp((int) (((rgb >> 16) & 0xff) * f));
        int gg = clamp((int) (((rgb >> 8) & 0xff) * f));
        int b = clamp((int) ((rgb & 0xff) * f));
        return (r << 16) | (gg << 8) | b;
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }

    // ---- tiles ---------------------------------------------------------------

    private static void ground(Graphics g, int w, int h, int c) {
        g.setColor(c);
        g.fillRect(0, 0, w, h);
        g.setColor(shade(c, 1.25));
        g.fillRect(0, 0, w, Math.max(2, h / 6));
        g.setColor(shade(c, 0.7));
        g.fillRect(0, h - Math.max(2, h / 8), w, Math.max(2, h / 8));
    }

    private static void grass(Graphics g, int w, int h, int c) {
        g.setColor(shade(c, 0.55));
        g.fillRect(0, h / 2, w, h - h / 2);
        g.setColor(c);
        g.fillRect(0, 0, w, h / 2 + 2);
        g.setColor(shade(c, 1.3));
        for (int x = 1; x < w; x += 5) {
            g.fillRect(x, 2, 2, h / 3);
        }
    }

    private static void brick(Graphics g, int w, int h, int c) {
        g.setColor(c);
        g.fillRect(0, 0, w, h);
        g.setColor(shade(c, 0.7));
        int row = Math.max(4, h / 4);
        for (int y = 0; y <= h; y += row) {
            g.drawLine(0, y, w, y);
        }
        for (int y = 0, band = 0; y < h; y += row, band++) {
            int off = (band % 2 == 0) ? 0 : w / 2;
            for (int x = off; x <= w; x += w / 2) {
                g.drawLine(x, y, x, y + row);
            }
        }
    }

    private static void crate(Graphics g, int w, int h, int c) {
        g.setColor(c);
        g.fillRect(0, 0, w, h);
        g.setColor(shade(c, 0.65));
        g.drawRect(0, 0, w - 1, h - 1);
        g.drawRect(1, 1, w - 3, h - 3);
        g.drawLine(0, 0, w, h);
        g.drawLine(w, 0, 0, h);
    }

    private static void ladder(Graphics g, int w, int h, int c) {
        g.setColor(c);
        int rail = Math.max(2, w / 6);
        g.fillRect(rail, 0, rail, h);
        g.fillRect(w - 2 * rail, 0, rail, h);
        for (int y = 2; y < h; y += Math.max(5, h / 4)) {
            g.fillRect(rail, y, w - 3 * rail, Math.max(2, h / 10));
        }
    }

    private static void spike(Graphics g, int w, int h, int c) {
        g.setColor(c);
        int n = Math.max(2, w / 8);
        int sw = w / n;
        for (int i = 0; i < n; i++) {
            int x = i * sw;
            g.fillPolygon(new int[]{x, x + sw / 2, x + sw}, new int[]{h, 0, h}, 3);
        }
    }

    private static void water(Graphics g, int w, int h, int c) {
        g.setColor(c);
        g.fillRect(0, 0, w, h);
        g.setColor(shade(c, 1.35));
        for (int y = h / 4; y < h; y += Math.max(4, h / 3)) {
            for (int x = 0; x < w; x += 8) {
                g.drawLine(x, y, x + 4, y - 2);
                g.drawLine(x + 4, y - 2, x + 8, y);
            }
        }
    }

    private static void block(Graphics g, int w, int h, int c) {
        g.setColor(c);
        g.fillRect(0, 0, w, h);
        g.setColor(shade(c, 1.3));
        g.fillRect(0, 0, w, 2);
        g.fillRect(0, 0, 2, h);
        g.setColor(shade(c, 0.65));
        g.fillRect(0, h - 2, w, 2);
        g.fillRect(w - 2, 0, 2, h);
    }

    // ---- actors --------------------------------------------------------------

    private static void tree(Graphics g, int w, int h, int c) {
        g.setColor(0x6B4A2B);
        g.fillRect(w / 2 - Math.max(1, w / 10), h / 2, Math.max(2, w / 5), h / 2);
        g.setColor(c);
        g.fillArc(0, 0, w, (int) (h * 0.75), 0, 360);
        g.setColor(shade(c, 1.25));
        g.fillArc(w / 6, h / 12, w / 2, h / 3, 0, 360);
    }

    private static void cloud(Graphics g, int w, int h, int c) {
        g.setColor(shade(c, 0.92));
        g.fillArc(0, h / 3, w * 2 / 3, h * 2 / 3, 0, 360);
        g.fillArc(w / 3, h / 4, w * 2 / 3, h * 3 / 4, 0, 360);
        g.fillArc(w / 5, h / 2, w / 2, h / 2, 0, 360);
        g.setColor(c);
        g.fillArc(w / 4, h / 3, w / 3, h / 3, 0, 360);
    }

    private static void mountain(Graphics g, int w, int h, int c) {
        g.setColor(c);
        g.fillPolygon(new int[]{0, w / 2, w}, new int[]{h, 0, h}, 3);
        g.setColor(shade(c, 0.8));   // shaded right face
        g.fillPolygon(new int[]{w / 2, w, w / 2}, new int[]{0, h, h}, 3);
        g.setColor(shade(c, 1.5));   // snow cap
        g.fillPolygon(new int[]{w / 2, w * 5 / 8, w * 3 / 8}, new int[]{0, h / 4, h / 4}, 3);
    }

    private static void hill(Graphics g, int w, int h, int c) {
        g.setColor(c);
        g.fillArc(-w / 4, h / 3, w * 3 / 2, h * 2, 0, 360);
        g.setColor(shade(c, 1.12));
        g.fillArc(w / 6, h / 2, w / 2, h, 0, 360);
    }

    private static void coffee(Graphics g, int w, int h, int c) {
        int top = h / 4;
        // steam
        g.setColor(0xcfd6e0);
        g.drawArc(w * 2 / 5 - 2, 2, 6, top, -90, 200);
        g.drawArc(w * 3 / 5 - 2, 2, 6, top, 90, 200);
        // white cup with coffee fill
        g.setColor(0xf2f4f8);
        g.fillRoundRect(w / 6, top, w * 2 / 3, h - top - 2, 6, 6);
        g.setColor(c);
        g.fillRoundRect(w / 6 + 2, top + 5, w * 2 / 3 - 4, h - top - 9, 5, 5);
        g.setColor(shade(c, 0.6));
        g.fillArc(w / 6 + 2, top + 2, w * 2 / 3 - 4, 8, 0, 360);
        // handle
        g.setColor(0xf2f4f8);
        g.drawArc(w * 5 / 6 - w / 12, top + 4, w / 4, h / 3, -80, 180);
    }

    private static void exceptionMonster(Graphics g, int w, int h, int c) {
        // jagged horns
        g.setColor(shade(c, 0.7));
        g.fillPolygon(new int[]{w / 5, w / 3, w * 5 / 12}, new int[]{h / 3, h / 12, h / 3}, 3);
        g.fillPolygon(new int[]{w * 7 / 12, w * 2 / 3, w * 4 / 5}, new int[]{h / 3, h / 12, h / 3}, 3);
        // round body
        g.setColor(c);
        g.fillArc(w / 8, h / 5, w * 3 / 4, h * 3 / 4, 0, 360);
        // angry white eyes with dark pupils
        g.setColor(0xffffff);
        g.fillArc(w / 3 - w / 12, h / 2 - h / 12, w / 6, h / 6, 0, 360);
        g.fillArc(w * 2 / 3 - w / 12, h / 2 - h / 12, w / 6, h / 6, 0, 360);
        g.setColor(0x201020);
        g.fillArc(w / 3 - 2, h / 2 - 1, 4, 4, 0, 360);
        g.fillArc(w * 2 / 3 - 2, h / 2 - 1, 4, 4, 0, 360);
        // fanged grimace
        g.drawLine(w / 3, h * 3 / 4, w * 2 / 3, h * 3 / 4);
        g.setColor(0xffffff);
        g.fillPolygon(new int[]{w * 5 / 12, w / 2, w * 7 / 12}, new int[]{h * 3 / 4, h * 5 / 6, h * 3 / 4}, 3);
    }

    private static void coin(Graphics g, int w, int h, int c) {
        g.setColor(shade(c, 0.7));
        g.fillArc(0, 0, w, h, 0, 360);
        g.setColor(c);
        g.fillArc(1, 1, w - 2, h - 2, 0, 360);
        g.setColor(shade(c, 1.4));
        g.fillArc(w / 4, h / 5, w / 4, h / 4, 0, 360);
    }

    private static void gem(Graphics g, int w, int h, int c) {
        g.setColor(c);
        g.fillPolygon(new int[]{w / 2, w, w / 2, 0}, new int[]{0, h / 2, h, h / 2}, 4);
        g.setColor(shade(c, 1.45));
        g.fillPolygon(new int[]{w / 2, w * 3 / 4, w / 2, w / 4}, new int[]{2, h / 2, h / 2, h / 2}, 4);
    }

    private static void figure(Graphics g, int w, int h, int c) {
        g.setColor(c);
        int head = Math.max(4, w / 2);
        g.fillArc(w / 2 - head / 2, 0, head, head, 0, 360);
        g.fillRoundRect(w / 4, head - 2, w / 2, h - head, 4, 4);
        g.setColor(shade(c, 0.6));
        g.fillRect(w / 4, h - Math.max(2, h / 6), w / 2, Math.max(2, h / 8));
    }

    private static void slime(Graphics g, int w, int h, int c) {
        g.setColor(c);
        g.fillArc(0, h / 4, w, h * 2, 0, 180);
        g.fillRect(0, h - h / 3, w, h / 3);
        g.setColor(0xffffff);
        g.fillArc(w / 4, h / 2, w / 6, h / 6, 0, 360);
        g.fillArc(w * 3 / 5, h / 2, w / 6, h / 6, 0, 360);
        g.setColor(0x101010);
        g.fillArc(w / 4 + 1, h / 2 + 1, w / 12, h / 12, 0, 360);
        g.fillArc(w * 3 / 5 + 1, h / 2 + 1, w / 12, h / 12, 0, 360);
    }

    private static void door(Graphics g, int w, int h, int c) {
        g.setColor(shade(c, 0.7));
        g.fillRect(0, 0, w, h);
        g.setColor(c);
        g.fillRect(2, 2, w - 4, h - 2);
        g.setColor(0xF6C944);
        g.fillArc(w - w / 3, h / 2, Math.max(3, w / 8), Math.max(3, w / 8), 0, 360);
    }

    private static void flag(Graphics g, int w, int h, int c) {
        g.setColor(0xCFD2D9);
        g.fillRect(Math.max(1, w / 8), 0, Math.max(2, w / 10), h);
        g.setColor(c);
        int px = w / 4;
        g.fillPolygon(new int[]{px, w, px}, new int[]{2, h / 4, h / 2}, 3);
    }

    private static void torch(Graphics g, int w, int h, int c) {
        g.setColor(0x6B4A2B);
        g.fillRect(w / 2 - 1, h / 2, Math.max(2, w / 4), h / 2);
        g.setColor(c);
        g.fillArc(w / 6, 0, w * 2 / 3, h * 2 / 3, 0, 360);
        g.setColor(0xFFE08A);
        g.fillArc(w / 3, h / 8, w / 3, h / 3, 0, 360);
    }

    private static void card(Graphics g, int w, int h, int c) {
        g.setColor(c);
        g.fillRoundRect(0, 0, w - 1, h - 1, 6, 6);
        g.setColor(0x9DB0D6);
        g.drawRoundRect(0, 0, w - 1, h - 1, 6, 6);
        g.setColor(0xE0466A);
        g.fillArc(3, 3, w / 4, w / 4, 0, 360);
    }

    private static void token(Graphics g, int w, int h, int c) {
        g.setColor(shade(c, 0.7));
        g.fillArc(0, 0, w, h, 0, 360);
        g.setColor(c);
        g.fillArc(2, 2, w - 4, h - 4, 0, 360);
    }

    private static void dice(Graphics g, int w, int h, int c) {
        g.setColor(c);
        g.fillRoundRect(0, 0, w - 1, h - 1, 5, 5);
        g.setColor(0x101010);
        int r = Math.max(2, w / 7);
        g.fillArc(w / 5, h / 5, r, r, 0, 360);
        g.fillArc(w * 3 / 5, h / 5, r, r, 0, 360);
        g.fillArc(w * 2 / 5, h * 2 / 5, r, r, 0, 360);
        g.fillArc(w / 5, h * 3 / 5, r, r, 0, 360);
        g.fillArc(w * 3 / 5, h * 3 / 5, r, r, 0, 360);
    }

    private static void barrel(Graphics g, int w, int h, int c) {
        g.setColor(c);
        g.fillRoundRect(w / 8, 0, w - w / 4, h, 6, 6);
        g.setColor(shade(c, 0.65));
        g.fillRect(0, h / 5, w, Math.max(2, h / 10));
        g.fillRect(0, h * 3 / 5, w, Math.max(2, h / 10));
    }

    private static void chip(Graphics g, int w, int h, int c) {
        g.setColor(c);
        g.fillRoundRect(0, 0, w - 1, h - 1, 6, 6);
        g.setColor(shade(c, 1.3));
        g.fillRect(2, 2, w - 4, Math.max(2, h / 6));
        g.setColor(shade(c, 0.6));
        g.drawRoundRect(0, 0, w - 1, h - 1, 6, 6);
    }

    private static void chest(Graphics g, int w, int h, int c) {
        g.setColor(c);
        g.fillRect(0, h / 3, w, h - h / 3);
        g.setColor(shade(c, 1.2));
        g.fillArc(0, 0, w, h * 2 / 3, 0, 180);
        g.setColor(shade(c, 0.6));
        g.fillRect(0, h / 3 - 1, w, Math.max(2, h / 10));
        g.setColor(0xF6C944);
        g.fillRect(w / 2 - 2, h / 3, 4, h / 4);
    }
}
