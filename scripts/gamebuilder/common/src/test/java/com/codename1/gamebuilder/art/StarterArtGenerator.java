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

import com.codename1.gamebuilder.editor.StarterPacks;
import com.codename1.gaming.level.AssetCatalog;
import com.codename1.gaming.level.AssetDef;
import com.codename1.gaming.level.AssetPack;
import com.codename1.ui.Display;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.util.ImageIO;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/// A one-time build tool (NOT runtime art): bakes every starter asset into a real PNG file
/// under `common/src/main/resources/` so the shipped game and editor load editable image
/// files instead of drawing art in code. Codename One's resource namespace is flat, so the
/// files sit at the resource root and load as `/<id>.png`. Re-run after changing `AssetArt`
/// or a starter pack. The coin is written as a 6-frame sprite sheet to exercise animation.
public final class StarterArtGenerator {
    private static final String OUT = "common/src/main/resources";

    public static void main(String[] args) throws Exception {
        Display.init(null);
        File dir = new File(OUT);
        dir.mkdirs();
        AssetCatalog catalog = StarterPacks.loadCatalog();
        int count = 0;
        for (AssetPack pack : catalog.packs()) {
            for (AssetDef def : pack.assets()) {
                if ("coin".equals(def.getId())) {
                    write(spinSheet(def), new File(dir, def.getId() + ".png"));
                } else {
                    write(AssetArt.render(def), new File(dir, def.getId() + ".png"));
                }
                count++;
            }
        }
        System.out.println("[StarterArt] wrote " + count + " sprites to " + dir.getAbsolutePath());
        System.exit(0);
    }

    /// A horizontal strip of `frames` spinning-coin frames (the coin squashed horizontally
    /// to fake a 3D spin), sized frameW x frameH per frame.
    private static Image spinSheet(AssetDef def) {
        int fw = Math.max(8, def.getWidth());
        int fh = Math.max(8, def.getHeight());
        int frames = 6;
        int c = def.getColor() & 0xffffff;
        Image sheet = Image.createImage(fw * frames, fh, 0);
        Graphics g = sheet.getGraphics();
        g.setAntiAliased(true);
        for (int f = 0; f < frames; f++) {
            int ox = f * fw;
            double t = f / (double) frames;
            int w = (int) Math.max(3, Math.abs(Math.cos(t * Math.PI)) * (fw - 4)) + 2;
            int x = ox + (fw - w) / 2;
            shade(g, c, 0.7);
            g.fillArc(x, 1, w, fh - 2, 0, 360);
            shade(g, c, 1.0);
            g.fillArc(x + 1, 2, Math.max(1, w - 2), fh - 4, 0, 360);
            shade(g, c, 1.4);
            g.fillArc(x + w / 3, fh / 5, Math.max(1, w / 4), fh / 4, 0, 360);
        }
        return sheet;
    }

    private static void shade(Graphics g, int rgb, double f) {
        int r = Math.min(255, (int) (((rgb >> 16) & 0xff) * f));
        int gr = Math.min(255, (int) (((rgb >> 8) & 0xff) * f));
        int b = Math.min(255, (int) ((rgb & 0xff) * f));
        g.setColor((r << 16) | (gr << 8) | b);
    }

    private static void write(Image img, File out) throws Exception {
        try (OutputStream os = new FileOutputStream(out)) {
            ImageIO.getImageIO().save(img, os, ImageIO.FORMAT_PNG, 1f);
        }
    }
}
