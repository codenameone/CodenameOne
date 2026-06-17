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
package com.codename1.gamebuilder;

import com.codename1.gaming.level.AssetCatalog;
import com.codename1.gaming.level.GameElement;
import com.codename1.gaming.level.GameLevel;
import com.codename1.gamebuilder.editor.StarterPacks;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.ImageIO;
import com.codename1.ui.util.Resources;

import java.io.ByteArrayOutputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/// Boots the game builder headlessly and writes PNGs of the editor and the live-preview
/// (play) state to target/screenshots/. Run via the exec harness idiom.
public final class ScreenshotHarness {
    private static final int W = 1440;
    private static final int H = 900;

    public static void main(String[] args) throws Exception {
        Display.init(null);
        Display.getInstance().callSeriallyAndWait(() -> {
            try {
                Resources r = Resources.openLayered("/theme");
                String[] names = r.getThemeResourceNames();
                if (names != null && names.length > 0) {
                    UIManager.getInstance().setThemeProps(r.getTheme(names[0]));
                }
            } catch (Exception ignore) {
            }
        });

        final GameBuilder[] gbRef = new GameBuilder[1];
        Display.getInstance().callSeriallyAndWait(() -> {
            gbRef[0] = new GameBuilder();
            gbRef[0].runApp();
        });
        capture("editor");

        // text-size toggle: cycle to Large, capture, then restore to Medium
        Display.getInstance().callSeriallyAndWait(() -> fireByName("btn.fontsize"));
        capture("editor-largefont");
        Display.getInstance().callSeriallyAndWait(() -> { fireByName("btn.fontsize"); fireByName("btn.fontsize"); });

        // light mode: toggle, capture, toggle back to dark
        Display.getInstance().callSeriallyAndWait(() -> fireByName("btn.theme"));
        capture("editor-light");
        Display.getInstance().callSeriallyAndWait(() -> fireByName("btn.theme"));

        // select the player to show the element/behavior inspector
        Display.getInstance().callSeriallyAndWait(() -> {
            GameBuilder gb = gbRef[0];
            var els = gb.getController().model().level().elements();
            for (int i = 0; i < els.size(); i++) {
                if ("player".equals(els.get(i).getAssetId())) {
                    gb.getController().model().setSelection(els.get(i));
                    break;
                }
            }
            gb.refreshUI();
        });
        capture("editor-inspector");

        // import a custom image -> shows the "Custom" pack with the imported asset
        Display.getInstance().callSeriallyAndWait(() -> {
            try {
                gbRef[0].getController().model().setSelection(null);
                Image sprite = Image.createImage(48, 48, 0xff7E5BEF);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ImageIO.getImageIO().save(sprite, bos, ImageIO.FORMAT_PNG, 1f);
                gbRef[0].registerImportedAsset(bos.toByteArray(), "My Hero");
            } catch (Exception ex) {
                com.codename1.io.Log.e(ex);
            }
        });
        capture("editor-import");

        // load a 3D level, stamp a tree, and zoom in -- verifies the board fills the
        // canvas, the element reads as one object (not the whole board), and zoom works
        Display.getInstance().callSeriallyAndWait(() -> {
            GameBuilder gb = gbRef[0];
            GameLevel l3d = StarterPacks.newLevel(GameLevel.MODE_3D);
            AssetCatalog cat = StarterPacks.loadCatalog();
            int ts = l3d.getTileSize();
            String[][] placements = {
                {"pillar", "8", "9"}, {"barrel", "7", "8"}, {"crate3d", "9", "8"},
                {"rock", "8", "6"}, {"tree3d", "6", "5"}, {"tree3d", "10", "5"},
                {"spawn", "8", "12"}
            };
            int id = 0;
            for (String[] p : placements) {
                GameElement el = new GameElement("e" + (++id), p[0]).setName(p[0] + " " + id);
                el.setLayer("Models");
                el.setPosition(Integer.parseInt(p[1]) * ts, Integer.parseInt(p[2]) * ts);
                var d = cat.def(p[0]);
                if (d != null) {
                    el.properties().putAll(d.defaultProperties());
                }
                l3d.addElement(el);
            }
            // author terrain: a flat floor, a raised mound, a couple of walls, and a hole —
            // exercises elevation, walls, and ground-presence so the preview has 3D relief.
            com.codename1.gaming.level.TerrainGrid ter =
                    new com.codename1.gaming.level.TerrainGrid(l3d.getCols(), l3d.getRows(), 1f);
            // a smooth ramp climbing toward the back, a grassy mound, a painted road strip,
            // two walls and a hole — exercises slopes, materials, walls and ground-presence.
            for (int rr = 0; rr < l3d.getRows(); rr++) {
                for (int cc = 0; cc < l3d.getCols(); cc++) {
                    ter.setHeight(cc, rr, Math.max(0f, (8 - rr) * 0.35f));   // ramps up toward the back
                    ter.setMaterial(cc, rr, com.codename1.gaming.level.TerrainGrid.MAT_GRASS);
                }
            }
            for (int rr = 0; rr < l3d.getRows(); rr++) {
                ter.setMaterial(8, rr, com.codename1.gaming.level.TerrainGrid.MAT_ROAD);   // a road down the middle
                ter.setMaterial(7, rr, com.codename1.gaming.level.TerrainGrid.MAT_ROAD);
            }
            ter.setWall(6, 9, 2.2f);
            ter.setWall(10, 9, 2.2f);
            ter.setGround(11, 10, false);   // a hole off to the side
            l3d.setTerrain(ter);
            gb.getController().loadLevel(l3d, "Arena 3D");
            gb.getController().model().setTool(com.codename1.gamebuilder.editor.Tool.TERRAIN);
            gb.getCanvas().zoomBy(1.5);
            gb.refreshUI();
            // show the 3D-kit pack (the project-open / new-scene flows do this for real)
            Component kit = find(Display.getInstance().getCurrent(), "tab.kit3d");
            if (kit instanceof Button) {
                ((Button) kit).released();
            }
        });
        capture("editor-3d");

        // true 3D live preview: toggle Live on the 3D scene and advance the orbit camera
        Display.getInstance().callSeriallyAndWait(() -> {
            Component live = find(Display.getInstance().getCurrent(), "btn.live");
            if (live instanceof Button) {
                ((Button) live).released();
            }
            for (int i = 0; i < 5; i++) {
                gbRef[0].getCanvas().tick(0.25);   // rotate the camera ~25° for an angled 3D view
            }
        });
        capture("editor-3d-play");
        Display.getInstance().callSeriallyAndWait(() -> {
            Component live = find(Display.getInstance().getCurrent(), "btn.live");
            if (live instanceof Button) {
                ((Button) live).released();   // stop, back to editing
            }
        });

        // render each 3D game-type prototype (open / flight / race / dungeon)
        for (final String t : new String[]{"flight", "race", "dungeon"}) {
            Display.getInstance().callSeriallyAndWait(() -> {
                gbRef[0].getController().model().level().props().put("view3d", t);
                Component live = find(Display.getInstance().getCurrent(), "btn.live");
                if (live instanceof Button) {
                    ((Button) live).released();   // start play for this type
                }
                for (int i = 0; i < 4; i++) {
                    gbRef[0].getCanvas().tick(0.2);
                }
            });
            capture("editor-3d-" + t);
            Display.getInstance().callSeriallyAndWait(() -> {
                Component live = find(Display.getInstance().getCurrent(), "btn.live");
                if (live instanceof Button) {
                    ((Button) live).released();   // stop
                }
            });
        }

        // clear selection, reload the 2D demo, toggle Live and capture the play state
        Display.getInstance().callSeriallyAndWait(() -> {
            gbRef[0].getController().loadLevel(StarterPacks.demoLevel(), "Pixel Quest");
            gbRef[0].getController().model().setSelection(null);
            gbRef[0].refreshUI();
            Component plat = find(Display.getInstance().getCurrent(), "tab.platformer");
            if (plat instanceof Button) {
                ((Button) plat).released();
            }
            Component live = find(Display.getInstance().getCurrent(), "btn.live");
            if (live instanceof Button) {
                ((Button) live).released();
            }
        });
        capture("editor-play");
        System.exit(0);
    }

    private static void capture(String name) throws Exception {
        final Image[] shot = new Image[1];
        Display.getInstance().callSeriallyAndWait(() -> {
            Form f = Display.getInstance().getCurrent();
            f.setWidth(W);
            f.setHeight(H);
            f.revalidate();
            f.layoutContainer();
            Image img = Image.createImage(W, H, 0xff061634);
            f.paintComponent(img.getGraphics(), true);
            shot[0] = img;
        });
        File outDir = new File("target/screenshots");
        outDir.mkdirs();
        File out = new File(outDir, name + ".png");
        ImageIO io = ImageIO.getImageIO();
        try (OutputStream os = new FileOutputStream(out)) {
            io.save(shot[0], os, ImageIO.FORMAT_PNG, 1f);
        }
        System.out.println("[ScreenshotHarness] wrote " + out.getAbsolutePath()
                + " (" + shot[0].getWidth() + "x" + shot[0].getHeight() + ")");
    }

    private static void fireByName(String name) {
        Component c = find(Display.getInstance().getCurrent(), name);
        if (c instanceof Button) {
            ((Button) c).released();
        }
    }

    private static Component find(Container root, String name) {
        for (int i = 0; i < root.getComponentCount(); i++) {
            Component c = root.getComponentAt(i);
            if (name.equals(c.getName())) {
                return c;
            }
            if (c instanceof Container) {
                Component f = find((Container) c, name);
                if (f != null) {
                    return f;
                }
            }
        }
        return null;
    }
}
