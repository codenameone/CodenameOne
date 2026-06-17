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

import com.codename1.gamebuilder.editor.CompanionCodeGen;
import com.codename1.gamebuilder.editor.EditorController;
import com.codename1.gamebuilder.editor.EditorModel;
import com.codename1.gamebuilder.editor.StarterPacks;
import com.codename1.gamebuilder.editor.Tool;
import com.codename1.gaming.Scene;
import com.codename1.gaming.level.AssetCatalog;
import com.codename1.gaming.level.GameElement;
import com.codename1.gaming.level.GameLevel;
import com.codename1.gaming.level.GameSceneView;
import com.codename1.gaming.level.Layer;
import com.codename1.ui.Display;

import java.util.List;

/// Executes the exact steps from the three Game Builder tutorials (2D platformer, board
/// game, 3D dungeon) through the editor API and asserts each one produces a *valid,
/// runnable* game: it realizes into sprites, survives a JSON save/reload, and emits a
/// non-empty companion class. This is the "actually do the steps and produce a valid game"
/// check that backs the blog tutorials. Run with the harness idiom.
public final class TutorialValidationHarness {
    private static final List<String> fail = new java.util.ArrayList<>();

    private static void check(boolean cond, String msg) {
        if (!cond) {
            fail.add(msg);
        }
    }

    public static void main(String[] args) throws Exception {
        Display.init(null);
        Display.getInstance().callSeriallyAndWait(() -> {
            platformer2D();
            boardGame();
            dungeon3D();
        });
        System.out.println("[Tutorials] failures=" + fail.size());
        for (String f : fail) {
            System.out.println("  FAIL: " + f);
        }
        System.out.println("[Tutorials] RESULT " + (fail.isEmpty() ? "OK" : "FAIL"));
        System.exit(fail.isEmpty() ? 0 : 1);
    }

    // ---- Tutorial 1: a 2D platformer ("Coin Run") ---------------------------
    private static void platformer2D() {
        AssetCatalog cat = StarterPacks.loadCatalog();
        EditorModel m = new EditorModel(StarterPacks.newLevel(GameLevel.MODE_2D), cat);
        m.setSceneName("CoinRun");
        EditorController c = new EditorController(m);
        GameLevel lvl = m.level();
        int rows = lvl.getRows();
        // Step: paint a ground line of grass on the Terrain layer
        m.setActiveLayer("Terrain");
        m.setSelectedAssetId("grass");
        for (int col = 0; col < lvl.getCols(); col++) {
            c.paintTile(col, rows - 2);
        }
        check(lvl.getLayer("Terrain").tiles().size() >= lvl.getCols(), "platformer: ground painted");
        // Step: place the player on the Actors layer + give it lives/jumpHeight
        m.setActiveLayer("Actors");
        m.setSelectedAssetId("player");
        int ts = lvl.getTileSize();
        GameElement player = c.placeElement(2 * ts, (rows - 3) * ts);
        check(player != null, "platformer: player placed");
        player.setProperty("lives", 3);
        player.setProperty("jumpHeight", 110);
        // Step: scatter coins (the default behavior collects them for score)
        m.setSelectedAssetId("coin");
        c.placeElement(5 * ts, (rows - 4) * ts);
        c.placeElement(6 * ts, (rows - 4) * ts);
        // Step: scene rules
        c.setLevelProperty("gravity", 9.8);
        c.setLevelProperty("background", "Sky");
        validate("platformer", m, cat, 3);
    }

    // ---- Tutorial 2: a board game ("Checkers Start") ------------------------
    private static void boardGame() {
        AssetCatalog cat = StarterPacks.loadCatalog();
        EditorModel m = new EditorModel(StarterPacks.newLevel(GameLevel.MODE_BOARD), cat);
        m.setSceneName("Checkers");
        EditorController c = new EditorController(m);
        GameLevel lvl = m.level();
        // Step: fill the board tiles (the "boardtile" square)
        Layer board = firstTileLayer(lvl);
        m.setActiveLayer(board.getName());
        m.setSelectedAssetId("boardtile");
        for (int r = 0; r < lvl.getRows(); r++) {
            for (int col = 0; col < lvl.getCols(); col++) {
                c.paintTile(col, r);
            }
        }
        check(board.tiles().size() > 0, "board: tiles painted");
        // Step: place a piece (the "token" actor) on the Pieces layer with a "player" property
        Layer pieces = firstEntityLayer(lvl);
        m.setActiveLayer(pieces.getName());
        m.setSelectedAssetId("token");
        int ts = lvl.getTileSize();
        GameElement p1 = c.placeElement(ts / 2.0, ts / 2.0);
        check(p1 != null, "board: piece placed");
        if (p1 != null) {
            p1.setProperty("player", 1);
            p1.setProperty("cell", "a1");
        }
        validate("board", m, cat, 1);
    }

    // ---- Tutorial 3: a 3D dungeon ("Crypt Walk") ----------------------------
    private static void dungeon3D() {
        AssetCatalog cat = StarterPacks.loadCatalog();
        EditorModel m = new EditorModel(StarterPacks.newLevel(GameLevel.MODE_3D), cat);
        m.setSceneName("CryptWalk");
        EditorController c = new EditorController(m);
        GameLevel lvl = m.level();
        // Step: choose the dungeon 3D play style
        c.setLevelProperty("view3d", "dungeon");
        check("dungeon".equals(lvl.getString("view3d", "open")), "3D: dungeon play style set");
        // Step: place walls (pillars), a treasure (collectible) and a spawn
        Layer models = firstEntityLayer(lvl);
        m.setActiveLayer(models.getName());
        int ts = lvl.getTileSize();
        m.setSelectedAssetId("pillar");
        for (int i = 2; i < 8; i++) {
            c.placeElement(i * ts + ts / 2.0, 3 * ts + ts / 2.0);
        }
        m.setSelectedAssetId("rock");
        GameElement rock = c.placeElement(5 * ts + ts / 2.0, 6 * ts + ts / 2.0);
        check(rock != null, "3D: scenery placed");
        m.setSelectedAssetId("spawn");
        GameElement spawn = c.placeElement(8 * ts + ts / 2.0, 8 * ts + ts / 2.0);
        check(spawn != null, "3D: spawn placed");
        spawn.setProperty("player", true);
        validate("3D dungeon", m, cat, 8);
    }

    // ---- shared validation ---------------------------------------------------
    private static void validate(String name, EditorModel m, AssetCatalog cat, int minElements) {
        GameLevel lvl = m.level();
        check(lvl.elements().size() >= minElements, name + ": has the placed elements ("
                + lvl.elements().size() + ")");
        boolean is3d = lvl.getMode() == GameLevel.MODE_3D;
        // 2D / board realize into live Sprites; 3D realizes into Models (no 2D sprites).
        if (!is3d) {
            Scene scene = new Scene();
            lvl.realizeSprites(scene, cat);
            check(scene.size() > 0, name + ": realizes into a Scene (" + scene.size() + ")");
        }
        // survives a JSON save/reload (the .game file) and builds in the runtime view
        try {
            GameLevel re = GameLevel.load(lvl.toJson());
            check(re.elements().size() == lvl.elements().size(), name + ": JSON round-trips elements");
            check(re.getMode() == lvl.getMode(), name + ": JSON round-trips mode");
            GameSceneView view = new GameSceneView(re, cat);   // must construct without error
            check(view != null, name + ": runtime GameSceneView builds");
            if (lvl.getMode() == GameLevel.MODE_2D) {
                check(view.getScene().size() > 0, name + ": runtime GameSceneView realizes sprites");
            }
        } catch (Exception e) {
            fail.add(name + ": JSON/runtime threw " + e);
        }
        // emits a usable companion class + .game data
        String java = CompanionCodeGen.companionJava(m.getSceneName().toLowerCase(), m.getSceneName(),
                "/" + m.getSceneName() + ".game", m.level());
        check(java != null && java.contains(m.getSceneName()), name + ": companion Java generated");
        String data = CompanionCodeGen.gameData(m);
        check(data != null && data.length() > 10, name + ": .game data generated");
    }

    private static Layer firstTileLayer(GameLevel lvl) {
        for (Layer l : lvl.layers()) {
            if (l.getKind() == Layer.KIND_TILE) {
                return l;
            }
        }
        return lvl.layers().get(0);
    }

    private static Layer firstEntityLayer(GameLevel lvl) {
        for (Layer l : lvl.layers()) {
            if (l.getKind() != Layer.KIND_TILE) {
                return l;
            }
        }
        return lvl.layers().get(lvl.layers().size() - 1);
    }
}
