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
import com.codename1.gamebuilder.editor.EditorController;
import com.codename1.gamebuilder.editor.EditorModel;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;

/// Drives the editor through the exact steps of the three blog tutorials (2D platformer, board
/// game, 3D dungeon) and writes a screenshot after each step into the website's blog image
/// folder, so the published tutorials are illustrated by real, regenerated screenshots. The
/// same steps are asserted runnable by `TutorialValidationHarness`; this one captures them.
public final class BlogTutorialScreenshots {
    // Render the editor in a roomy window so the toolbar lays out without crowding,
    // then publish the PNG a little smaller (SHOT_SCALE). Heroes are 1024x512 JPEGs.
    private static final int W = 1680;
    private static final int H = 1050;
    private static final double SHOT_SCALE = 0.8;
    private static final int HERO_W = 1024;
    private static final int HERO_H = 512;
    private static final String OUT = "../../docs/website/static/blog/gamebuilder";

    private static GameBuilder gb;
    private static int count;

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
        Display.getInstance().callSeriallyAndWait(() -> {
            gb = new GameBuilder();
            gb.runApp();
        });

        platformer();
        board();
        dungeon();

        System.out.println("[BlogShots] wrote " + count + " screenshots to " + new File(OUT).getAbsolutePath());
        System.exit(0);
    }

    // ---- Tutorial 1: 2D platformer "Coin Run" --------------------------------
    private static void platformer() {
        edit(() -> {
            gb.getController().loadLevel(StarterPacks.newLevel(GameLevel.MODE_2D), "CoinRun");
            gb.getController().model().setSelection(null);
        });
        shot("platformer-1-new-scene");

        final GameLevel[] lvl = new GameLevel[1];
        edit(() -> {
            EditorController c = gb.getController();
            EditorModel m = c.model();
            lvl[0] = m.level();
            int ts = lvl[0].getTileSize();
            int rows = lvl[0].getRows();
            m.setActiveLayer("Terrain");
            m.setSelectedAssetId("grass");
            for (int col = 0; col < lvl[0].getCols(); col++) {
                c.paintTile(col, rows - 2);
            }
        });
        shot("platformer-2-ground");

        // a parallax background: mountains along the horizon and clouds in the sky,
        // painted on the slow-scrolling Background layer
        edit(() -> {
            EditorController c = gb.getController();
            EditorModel m = c.model();
            int rows = lvl[0].getRows();
            int cols = lvl[0].getCols();
            m.setActiveLayer("Background");
            m.setSelectedAssetId("mountain");
            for (int col = 0; col < cols; col += 2) {
                c.paintTile(col, rows - 4);
                c.paintTile(col + 1, rows - 3);
            }
            m.setSelectedAssetId("cloud");
            c.paintTile(3, 2);
            c.paintTile(9, 1);
            c.paintTile(15, 3);
            c.paintTile(21, 2);
            m.setSelection(null);
        });
        shot("platformer-2b-background");

        edit(() -> {
            EditorController c = gb.getController();
            EditorModel m = c.model();
            int ts = lvl[0].getTileSize();
            int rows = lvl[0].getRows();
            m.setActiveLayer("Actors");
            m.setSelectedAssetId("player");
            GameElement player = c.placeElement(2 * ts, (rows - 3) * ts);
            player.setName("player");        // named objects become generated Sprite fields
            player.setProperty("lives", 3);
            player.setProperty("jumpHeight", 110);
            m.setSelection(player);
        });
        shot("platformer-3-player");

        edit(() -> {
            EditorController c = gb.getController();
            EditorModel m = c.model();
            int ts = lvl[0].getTileSize();
            int rows = lvl[0].getRows();
            m.setSelectedAssetId("coin");
            c.placeElement(5 * ts, (rows - 4) * ts);
            c.placeElement(6 * ts, (rows - 4) * ts);
            c.placeElement(7 * ts, (rows - 4) * ts);
            m.setSelection(null);
        });
        shot("platformer-4-coins");

        edit(() -> {
            EditorController c = gb.getController();
            EditorModel m = c.model();
            int ts = lvl[0].getTileSize();
            int rows = lvl[0].getRows();
            // an enemy to dodge and a flag to reach — the goal of the level
            m.setSelectedAssetId("slime");
            GameElement slime = c.placeElement(11 * ts, (rows - 3) * ts);
            slime.setProperty("speed", 40);
            m.setSelectedAssetId("flag");
            c.placeElement((lvl[0].getCols() - 2) * ts, (rows - 3) * ts);
            m.setSelection(slime);
        });
        shot("platformer-5-enemy-goal");

        edit(() -> {
            EditorController c = gb.getController();
            c.setLevelProperty("gravity", 9.8);
            c.setLevelProperty("background", "Sky");
            c.model().setSelection(null);
        });
        shot("platformer-6-scene");

        live();
        shot("platformer-7-play");
        hero("platformer");
        recordGif("platformer");
        stop();
    }

    // Raw key codes the JavaSE port maps to game actions (Display.GAME_* constants
    // themselves map to 0 via getGameAction, so we must feed the device codes).
    private static final int K_UP = -91;
    private static final int K_RIGHT = -94;

    /// Records a short gameplay GIF of the live preview by driving scripted input each frame
    /// and cropping to the canvas (the device-framed game). Frames are written to a temp dir
    /// and assembled into a GIF by the build step.
    private static void recordGif(String name) {
        final int frames = 24;
        final int mode = gb.getController().model().level().getMode();
        final GameElement[] mover = new GameElement[1];
        final double[] path = new double[4]; // fromX, fromY, toX, toY (board piece slide)
        Display.getInstance().callSeriallyAndWait(() -> {
            ensureLive();
            if (mode == GameLevel.MODE_2D) {
                gb.getCanvas().keyPressed(K_RIGHT);   // run right
            } else if (mode == GameLevel.MODE_3D) {
                gb.getCanvas().keyPressed(K_UP);      // walk forward (3D)
            } else {
                GameElement t = firstToken();                    // slide a checker piece
                if (t != null) {
                    int ts = gb.getController().model().level().getTileSize();
                    mover[0] = t;
                    path[0] = t.getX();
                    path[1] = t.getY();
                    path[2] = t.getX() + 3 * ts;
                    path[3] = t.getY() + 3 * ts;
                }
            }
        });
        for (int f = 0; f < frames; f++) {
            final int fr = f;
            Display.getInstance().callSeriallyAndWait(() -> {
                if (mode == GameLevel.MODE_2D) {
                    if (fr % 6 == 0) {
                        gb.getCanvas().keyPressed(K_UP);     // periodic jump (edge)
                    } else if (fr % 6 == 2) {
                        gb.getCanvas().keyReleased(K_UP);
                    }
                } else if (mode == GameLevel.MODE_3D) {
                    // walk forward down the corridor, then pan to look around (so the
                    // camera never jams head-on into the far wall and goes gray).
                    if (fr == frames / 2) {
                        gb.getCanvas().keyReleased(K_UP);
                        gb.getCanvas().keyPressed(K_RIGHT);
                    }
                } else if (mover[0] != null) {
                    double t = (fr + 1) / (double) frames;              // ease + hop the piece
                    double ease = t * t * (3 - 2 * t);
                    double hop = Math.abs(Math.sin(t * Math.PI * 3)) * 10;
                    mover[0].setPosition(path[0] + (path[2] - path[0]) * ease,
                            path[1] + (path[3] - path[1]) * ease - hop);
                }
                gb.getCanvas().tick(0.06);
            });
            cropFrame(name, fr);
        }
    }

    private static void ensureLive() {
        Component live = find(Display.getInstance().getCurrent(), "btn.live");
        if (live instanceof Button && !gb.getCanvas().isPlayMode()) {
            ((Button) live).released();
        }
        gb.getCanvas().tick(0.06);
    }

    private static void cropFrame(String name, int fr) {
        final Image[] img = new Image[1];
        final int[] box = new int[4];
        Display.getInstance().callSeriallyAndWait(() -> {
            Form f = Display.getInstance().getCurrent();
            f.setWidth(W);
            f.setHeight(H);
            f.revalidate();
            f.layoutContainer();
            Image i = Image.createImage(W, H, 0xff061634);
            f.paintComponent(i.getGraphics(), true);
            img[0] = i;
            box[0] = gb.getCanvas().getAbsoluteX();
            box[1] = gb.getCanvas().getAbsoluteY();
            box[2] = gb.getCanvas().getWidth();
            box[3] = gb.getCanvas().getHeight();
        });
        try {
            Image crop = img[0].subImage(Math.max(0, box[0]), Math.max(0, box[1]),
                    Math.min(box[2], W - box[0]), Math.min(box[3], H - box[1]), true);
            File dir = new File("target/gifframes/" + name);
            dir.mkdirs();
            try (OutputStream os = new FileOutputStream(new File(dir, String.format("%02d.png", fr)))) {
                ImageIO.getImageIO().save(crop, os, ImageIO.FORMAT_PNG, 1f);
            }
        } catch (Exception ex) {
            com.codename1.io.Log.e(ex);
        }
    }

    // ---- Tutorial 2: board game "Checkers Start" -----------------------------
    private static void board() {
        edit(() -> {
            gb.getController().loadLevel(StarterPacks.newLevel(GameLevel.MODE_BOARD), "CheckersStart");
            gb.getController().model().setSelection(null);
        });
        shot("board-1-new-scene");

        edit(() -> {
            EditorController c = gb.getController();
            EditorModel m = c.model();
            GameLevel lvl = m.level();
            m.setActiveLayer("Board");
            m.setSelectedAssetId("boardtile");
            for (int r = 0; r < lvl.getRows(); r++) {
                for (int col = 0; col < lvl.getCols(); col++) {
                    c.paintTile(col, r);
                }
            }
        });
        shot("board-2-tiles");

        edit(() -> {
            EditorController c = gb.getController();
            EditorModel m = c.model();
            int ts = m.level().getTileSize();
            m.setActiveLayer("Pieces");
            m.setSelectedAssetId("token");
            GameElement p1 = c.placeElement(ts / 2.0, ts / 2.0);
            GameElement p2 = c.placeElement(2 * ts + ts / 2.0, 2 * ts + ts / 2.0);
            if (p1 != null) {
                p1.setProperty("player", 1);
                p1.setProperty("cell", "a1");
            }
            if (p2 != null) {
                p2.setProperty("player", 2);
                p2.setProperty("cell", "c3");
            }
            m.setSelection(p1);
        });
        shot("board-3-pieces");

        edit(() -> gb.getController().model().setSelection(null));
        shot("board-4-build");

        live();
        tick();
        shot("board-5-play");
        hero("board");
        recordGif("board");
        stop();
    }

    // ---- Tutorial 3: first-person 3D dungeon "Crypt Walk" --------------------
    private static void dungeon() {
        edit(() -> {
            gb.getController().loadLevel(StarterPacks.newLevel(GameLevel.MODE_3D), "CryptWalk");
            gb.getController().model().setSelection(null);
            Component kit = find(Display.getInstance().getCurrent(), "tab.kit3d");
            if (kit instanceof Button) {
                ((Button) kit).released();
            }
        });
        shot("dungeon-1-new-scene");

        edit(() -> {
            gb.getController().setLevelProperty("view3d", "dungeon");
            gb.getController().model().setSelection(null);
        });
        shot("dungeon-2-style");

        final GameElement[] spawn = new GameElement[1];
        edit(() -> {
            EditorController c = gb.getController();
            EditorModel m = c.model();
            GameLevel lvl = m.level();
            int ts = lvl.getTileSize();
            com.codename1.gaming.level.Layer models = lvl.layers().get(lvl.layers().size() - 1);
            m.setActiveLayer(models.getName());
            // two pillar walls flanking an open central corridor, so walking forward
            // sends the walls sweeping past the camera (a real first-person move).
            int cx = lvl.getCols() / 2;
            m.setSelectedAssetId("pillar");
            for (int r = 0; r < lvl.getRows(); r++) {
                c.placeElement((cx - 2) * ts + ts / 2.0, r * ts + ts / 2.0);
                c.placeElement((cx + 2) * ts + ts / 2.0, r * ts + ts / 2.0);
            }
            // keep the central lane clear (a head-on rock would jam the walk); tuck the
            // rock into a side niche as scenery.
            m.setSelectedAssetId("rock");
            c.placeElement((cx - 1) * ts + ts / 2.0, 2 * ts + ts / 2.0);
        });
        shot("dungeon-3-walls");

        edit(() -> {
            EditorController c = gb.getController();
            EditorModel m = c.model();
            int ts = m.level().getTileSize();
            int cx = m.level().getCols() / 2;
            m.setSelectedAssetId("spawn");
            spawn[0] = c.placeElement(cx * ts + ts / 2.0, (m.level().getRows() - 2) * ts + ts / 2.0);
            if (spawn[0] != null) {
                spawn[0].setProperty("player", true);
                m.setSelection(spawn[0]);
            }
        });
        shot("dungeon-4-spawn");

        live();
        for (int i = 0; i < 3; i++) {
            tick();
        }
        shot("dungeon-5-walk");
        hero("dungeon");
        recordGif("dungeon");
        stop();
    }

    // ---- helpers -------------------------------------------------------------
    private static void edit(Runnable r) {
        Display.getInstance().callSeriallyAndWait(() -> {
            r.run();
            gb.refreshUI();
        });
    }

    private static void live() {
        Display.getInstance().callSeriallyAndWait(() -> {
            Component live = find(Display.getInstance().getCurrent(), "btn.live");
            if (live instanceof Button) {
                ((Button) live).released();
            }
            gb.getCanvas().tick(0.2);
        });
    }

    private static void tick() {
        Display.getInstance().callSeriallyAndWait(() -> gb.getCanvas().tick(0.2));
    }

    private static void stop() {
        Display.getInstance().callSeriallyAndWait(() -> {
            Component live = find(Display.getInstance().getCurrent(), "btn.live");
            if (live instanceof Button) {
                ((Button) live).released();
            }
        });
    }

    private static void shot(String name) {
        final Image[] img = new Image[1];
        Display.getInstance().callSeriallyAndWait(() -> {
            Form f = Display.getInstance().getCurrent();
            f.setWidth(W);
            f.setHeight(H);
            f.revalidate();
            f.layoutContainer();
            Image i = Image.createImage(W, H, 0xff061634);
            f.paintComponent(i.getGraphics(), true);
            img[0] = i;
        });
        try {
            File dir = new File(OUT);
            dir.mkdirs();
            Image scaled = img[0].scaled((int) Math.round(W * SHOT_SCALE), (int) Math.round(H * SHOT_SCALE));
            File out = new File(dir, name + ".png");
            try (OutputStream os = new FileOutputStream(out)) {
                ImageIO.getImageIO().save(scaled, os, ImageIO.FORMAT_PNG, 1f);
            }
            count++;
            System.out.println("[BlogShots] " + out.getName());
        } catch (Exception ex) {
            com.codename1.io.Log.e(ex);
        }
    }

    /// Writes a 1024x512 JPEG hero banner from the current live game viewport
    /// (cover-cropped from the canvas, so it reads as gameplay, not editor chrome).
    private static void hero(String name) {
        final Image[] img = new Image[1];
        final int[] box = new int[4];
        Display.getInstance().callSeriallyAndWait(() -> {
            Form f = Display.getInstance().getCurrent();
            f.setWidth(W);
            f.setHeight(H);
            f.revalidate();
            f.layoutContainer();
            Image i = Image.createImage(W, H, 0xff061634);
            f.paintComponent(i.getGraphics(), true);
            img[0] = i;
            box[0] = gb.getCanvas().getAbsoluteX();
            box[1] = gb.getCanvas().getAbsoluteY();
            box[2] = gb.getCanvas().getWidth();
            box[3] = gb.getCanvas().getHeight();
        });
        try {
            Image crop = img[0].subImage(Math.max(0, box[0]), Math.max(0, box[1]),
                    Math.min(box[2], W - box[0]), Math.min(box[3], H - box[1]), true);
            Image cover = crop.scaledLargerRatio(HERO_W, HERO_H);
            int cx = Math.max(0, (cover.getWidth() - HERO_W) / 2);
            int cy = Math.max(0, (cover.getHeight() - HERO_H) / 2);
            Image banner = cover.subImage(cx, cy, HERO_W, HERO_H, true);
            File dir = new File(OUT);
            dir.mkdirs();
            File out = new File(dir, name + "-hero.jpg");
            try (OutputStream os = new FileOutputStream(out)) {
                ImageIO.getImageIO().save(banner, os, ImageIO.FORMAT_JPEG, 0.9f);
            }
            System.out.println("[BlogShots] " + out.getName());
        } catch (Exception ex) {
            com.codename1.io.Log.e(ex);
        }
    }

    private static GameElement firstToken() {
        List<GameElement> els = gb.getController().model().level().elements();
        for (int i = 0; i < els.size(); i++) {
            if ("token".equals(els.get(i).getAssetId())) {
                return els.get(i);
            }
        }
        return els.isEmpty() ? null : els.get(0);
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
