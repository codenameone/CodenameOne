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
import com.codename1.gaming.level.AssetDef;
import com.codename1.gaming.level.GameElement;
import com.codename1.gaming.level.GameLevel;
import com.codename1.gamebuilder.editor.EditorController;
import com.codename1.gamebuilder.editor.EditorModel;
import com.codename1.gamebuilder.editor.StarterPacks;
import com.codename1.gamebuilder.game.Blackjack;
import com.codename1.gamebuilder.game.Blackjack.Card;
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
import java.util.Random;

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
            gb.getController().loadLevel(StarterPacks.newLevel(GameLevel.MODE_2D), "CoffeeRun");
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
        // freely placed on the slow-scrolling Background layer at varied sizes (they're
        // big and not grid-snapped)
        edit(() -> {
            EditorController c = gb.getController();
            EditorModel m = c.model();
            int ts = lvl[0].getTileSize();
            double horizon = (lvl[0].getRows() - 2) * ts;   // ground top
            m.setActiveLayer("Background");
            m.setSelectedAssetId("mountain");
            onGround(c, 70, horizon, 1.0);
            onGround(c, 300, horizon, 1.45);
            onGround(c, 540, horizon, 0.8);
            onGround(c, 760, horizon, 1.2);
            m.setSelectedAssetId("cloud");
            place(c, 150, 90, 1.1);
            place(c, 440, 64, 0.65);
            place(c, 640, 140, 1.3);
            place(c, 800, 96, 0.85);
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
            GameElement player = onGround(c, 2 * ts, (rows - 2) * ts, 1.0);   // feet on the grass
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
            m.setSelectedAssetId("coffee");
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
            // an exception monster to dodge and a flag to reach — the goal of the level.
            // Naming them makes the editor generate `enemy` and `flag` fields in the companion.
            double horizon = (rows - 2) * ts;   // top of the grass
            m.setSelectedAssetId("exception");
            GameElement enemy = onGround(c, 11 * ts, horizon, 1.0);
            enemy.setName("enemy");
            enemy.setProperty("speed", 45);
            m.setSelectedAssetId("flag");
            GameElement flag = onGround(c, (lvl[0].getCols() - 2) * ts, horizon, 1.0);
            flag.setName("flag");
            m.setSelection(enemy);
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
                // Duke Jack: deal a fresh card off the deck (top) and flip it into his hand
                EditorController c = gb.getController();
                GameLevel lvl = c.model().level();
                double cx = lvl.getCols() * lvl.getTileSize() / 2.0;
                double deal = lvl.getRows() * lvl.getTileSize() * 0.72;
                c.model().setActiveLayer("Pieces");
                c.model().setSelectedAssetId("card");
                GameElement card = c.placeElement(cx, 40);
                if (card != null) {
                    card.setPosition(cx, 40);
                    card.setScale(2.3f);
                    card.setProperty("rank", "K");
                    card.setProperty("suit", "Spades");
                    card.setProperty("faceUp", false);
                    mover[0] = card;
                    path[0] = cx;
                    path[1] = 40;
                    path[2] = cx + 78;
                    path[3] = deal;
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
                    // step a little way into the corridor, then stop and fire coffee beans at
                    // the tea cups ahead (so the clip shows the dungeon's combat, not just a walk).
                    if (fr == 5) {
                        gb.getCanvas().keyReleased(K_UP);
                    }
                    if (fr >= 6 && fr % 3 == 0) {
                        gb.getCanvas().fire();
                    }
                } else if (mover[0] != null) {
                    double t = (fr + 1) / (double) frames;              // ease + hop the card
                    double ease = t * t * (3 - 2 * t);
                    double hop = Math.abs(Math.sin(t * Math.PI * 3)) * 10;
                    mover[0].setPosition(path[0] + (path[2] - path[0]) * ease,
                            path[1] + (path[3] - path[1]) * ease - hop);
                    if (t > 0.6) {
                        mover[0].setProperty("faceUp", true);           // flip it face-up on landing
                    }
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

    // ---- Tutorial 2: card game "Duke Jack" (blackjack) -----------------------
    // Duke holds 5H + JC (15) — the Jack shows Duke — hits a 4D to 19 (still his turn), then
    // stands; the dealer (QH up, also a Duke face card) draws to 18 and Duke wins 19 over 18.
    // Chosen so a Duke face card is on the table and each step's table is truthful.
    private static final int BJ_SEED = 173;

    private static void board() {
        edit(() -> {
            GameLevel lvl = StarterPacks.newLevel(GameLevel.MODE_BOARD);
            lvl.setGrid(8, 5, 64);   // a compact card table (not a 10x10 game board)
            gb.getController().loadLevel(lvl, "DukeJack");
            gb.getController().model().setSelection(null);
        });
        shot("board-1-new-scene");

        // green felt: paint the whole board with the green tile
        edit(() -> {
            EditorController c = gb.getController();
            EditorModel m = c.model();
            GameLevel lvl = m.level();
            m.setActiveLayer("Board");
            m.setSelectedAssetId("start");
            for (int r = 0; r < lvl.getRows(); r++) {
                for (int col = 0; col < lvl.getCols(); col++) {
                    c.paintTile(col, r);
                }
            }
        });
        shot("board-2-felt");

        // a real blackjack deal, sourced from the Blackjack engine the tutorial documents
        final Blackjack[] game = {null};
        edit(() -> {
            game[0] = new Blackjack(new Random(BJ_SEED));
            layoutHands(gb.getController(), game[0], false);   // dealer hole hidden
            gb.getController().model().setSelection(null);
        });
        shot("board-3-deal");

        // Duke's turn: hit while low, then layout his grown hand (hole still hidden)
        edit(() -> {
            Blackjack g = game[0];
            if (g.phase() == Blackjack.Phase.PLAYER_TURN && g.playerValue() < 17) {
                g.hit();
            }
            layoutHands(gb.getController(), g, false);
            gb.getController().model().setSelection(null);
        });
        shot("board-4-hit");

        // Duke stands: the dealer reveals the hole card and plays out; show the resolved table
        edit(() -> {
            Blackjack g = game[0];
            if (g.phase() == Blackjack.Phase.PLAYER_TURN) {
                g.stand();
            }
            layoutHands(gb.getController(), g, true);
            gb.getController().model().setSelection(null);
        });

        live();
        tick();
        shot("board-5-play");
        hero("board");
        recordGif("board");
        stop();
    }

    private static String suitName(int suit) {
        switch (suit) {
            case Blackjack.SUIT_HEARTS: return "Hearts";
            case Blackjack.SUIT_DIAMONDS: return "Diamonds";
            case Blackjack.SUIT_CLUBS: return "Clubs";
            default: return "Spades";
        }
    }

    /// Clears the table and lays the dealer's hand (top) and Duke's hand (bottom) as card
    /// elements, reading rank/suit from the engine. The dealer's second card stays face-down
    /// until `reveal`.
    private static void layoutHands(EditorController c, Blackjack g, boolean reveal) {
        GameLevel lvl = c.model().level();
        lvl.elements().removeIf(e -> "card".equals(e.getAssetId()));
        c.model().setActiveLayer("Pieces");
        c.model().setSelectedAssetId("card");
        double boardH = lvl.getRows() * lvl.getTileSize();
        placeHand(c, g.dealerHand(), boardH * 0.30, reveal ? -1 : 1);
        placeHand(c, g.playerHand(), boardH * 0.72, -1);
    }

    private static void placeHand(EditorController c, List<Card> hand, double y, int hideIndex) {
        double cx = c.model().level().getCols() * c.model().level().getTileSize() / 2.0;
        int n = hand.size();
        double spacing = 78;
        double startX = cx - (n - 1) * spacing / 2;
        for (int i = 0; i < n; i++) {
            Card card = hand.get(i);
            GameElement e = c.placeElement(startX + i * spacing, y);
            if (e != null) {
                e.setPosition(startX + i * spacing, y);
                e.setScale(2.3f);   // big enough to read as a real card on the table
                e.setProperty("rank", card.rankLabel());
                e.setProperty("suit", suitName(card.suit));
                e.setProperty("faceUp", i != hideIndex);
            }
        }
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

        edit(() -> {
            EditorController c = gb.getController();
            GameLevel lvl = c.model().level();
            int cols = lvl.getCols();
            int rows = lvl.getRows();
            int cx = cols / 2;
            com.codename1.gaming.level.TerrainGrid t = lvl.getTerrain();
            float wall = 2.4f;
            // a real maze built from continuous TERRAIN WALLS (not spaced pillars): a solid
            // perimeter plus internal walls, leaving the central column open as Duke's corridor.
            for (int col = 0; col < cols; col++) {
                t.setWall(col, 0, wall);
                t.setWall(col, rows - 1, wall);
            }
            for (int r = 0; r < rows; r++) {
                t.setWall(0, r, wall);
                t.setWall(cols - 1, r, wall);
            }
            for (int r = 2; r <= 6; r++) {
                t.setWall(cx - 3, r, wall);
            }
            for (int col = cx - 3; col <= cx - 1; col++) {
                t.setWall(col, 6, wall);
            }
            for (int r = 4; r <= 9; r++) {
                t.setWall(cx + 3, r, wall);
            }
            for (int col = cx + 1; col <= cx + 3; col++) {
                t.setWall(col, 4, wall);
            }
            for (int r = 9; r <= 12; r++) {
                t.setWall(cx - 2, r, wall);
            }
        });
        shot("dungeon-3-walls");

        final GameElement[] spawn = new GameElement[1];
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

        // Tea-cup enemies down the central corridor — Duke's targets.
        edit(() -> {
            EditorController c = gb.getController();
            EditorModel m = c.model();
            int ts = m.level().getTileSize();
            int cx = m.level().getCols() / 2;
            int rows = m.level().getRows();
            m.setActiveLayer("Models");
            m.setSelectedAssetId("teacup");
            for (int dr = 5; dr <= 11; dr += 3) {
                c.placeElement(cx * ts + ts / 2.0, (rows - dr) * ts + ts / 2.0);
            }
            m.setSelection(null);
        });

        live();
        for (int i = 0; i < 3; i++) {
            tick();
        }
        shot("dungeon-5-walk");

        // fire coffee beans down the corridor at the tea cups: land two hits (score climbs,
        // cups vanish), then loose a third bean and capture it mid-flight.
        Display.getInstance().callSeriallyAndWait(() -> {
            for (int shot = 0; shot < 2; shot++) {
                gb.getCanvas().fire();
                for (int k = 0; k < 6; k++) {
                    gb.getCanvas().tick(0.08);   // let the bean fly to its cup (cooldown clears)
                }
            }
            gb.getCanvas().fire();
            gb.getCanvas().tick(0.16);            // let the bean clear the muzzle for the shot
        });
        shot("dungeon-6-combat");
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

    /// Places the active asset at a pixel position with a per-instance scale.
    private static GameElement place(EditorController c, double x, double y, double scale) {
        GameElement e = c.placeElement(x, y);
        if (e != null) {
            e.setScale((float) scale);
        }
        return e;
    }

    /// Places the active asset so its *rendered base* rests exactly on `groundY` (no float).
    /// The editor clamps an element's on-screen size to at most 1.8 cells, so the rendered
    /// half-height is derived from that clamp — not the asset's raw pixel height — which is
    /// why a naive `y = ground - height/2` left mountains and flags hovering.
    private static GameElement onGround(EditorController c, double x, double groundY, double scale) {
        int ts = c.model().level().getTileSize();
        AssetDef d = c.model().catalog().def(c.model().getSelectedAssetId());
        double cells = Math.max(0.3, Math.min(1.8, (d == null ? ts : d.getHeight()) / (double) ts));
        double halfWorld = cells * ts * scale / 2.0;
        return place(c, x, groundY - halfWorld, scale);
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
