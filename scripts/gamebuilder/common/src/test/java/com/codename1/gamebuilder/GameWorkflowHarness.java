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

import com.codename1.gamebuilder.art.AssetArt;
import com.codename1.gamebuilder.editor.EditorController;
import com.codename1.gamebuilder.editor.EditorModel;
import com.codename1.gamebuilder.editor.StarterPacks;
import com.codename1.gamebuilder.editor.Tool;
import com.codename1.gamebuilder.project.ProjectBinding;
import com.codename1.gamebuilder.project.ProjectIO;
import com.codename1.gamebuilder.ui.EditorCanvas;
import com.codename1.gaming.Scene;
import com.codename1.gaming.level.AssetCatalog;
import com.codename1.gaming.level.GameElement;
import com.codename1.gaming.level.GameLevel;
import com.codename1.gaming.level.GameSceneView;
import com.codename1.ui.Display;

import java.io.File;
import java.util.List;

/// End-to-end verification that the core uses needed to BUILD A GAME work: start a
/// level, paint tiles and stamp actors (both via the controller and via the real canvas
/// brush gesture), realize it into live sprites with `GameLevel` and the runtime
/// `GameSceneView`, then save to a project and reload — asserting the game survives the
/// round trip. Exits non-zero on any failure.
public final class GameWorkflowHarness {
    private static final java.util.List<String> fail = new java.util.ArrayList<>();

    private static void check(boolean cond, String msg) {
        if (!cond) {
            fail.add(msg);
        }
    }

    public static void main(String[] args) throws Exception {
        Display.init(null);
        Display.getInstance().callSeriallyAndWait(() -> {
            try {
                run();
            } catch (Exception e) {
                fail.add("exception: " + e);
                com.codename1.io.Log.e(e);
            }
        });
        System.out.println("[Workflow] failures=" + fail.size());
        for (String f : fail) {
            System.out.println("  FAIL: " + f);
        }
        System.out.println("[Workflow] RESULT " + (fail.isEmpty() ? "OK" : "FAIL"));
        System.exit(fail.isEmpty() ? 0 : 1);
    }

    private static void run() throws Exception {
        AssetCatalog cat = StarterPacks.loadCatalog();
        AssetArt.install(cat);
        EditorModel model = new EditorModel(StarterPacks.newLevel(GameLevel.Mode.TWO_D), cat);
        model.setSceneName("Arena");
        EditorController c = new EditorController(model);

        // --- build the level via the controller: a ground row + actors ---
        model.setActiveLayer("Terrain");
        model.setSelectedAssetId("ground");
        int cols = model.level().getCols();
        int rows = model.level().getRows();
        int painted = 0;
        for (int x = 0; x < cols; x++) {
            if (c.paintTile(x, rows - 1)) {
                painted++;
            }
        }
        check(painted == cols, "painted a full ground row (" + painted + "/" + cols + ")");

        model.setActiveLayer("Actors");
        model.setSelectedAssetId("player");
        GameElement player = c.placeElement(2 * 32 + 16, (rows - 3) * 32);
        check(player != null && "player".equals(player.getAssetId()), "placed the player");
        model.setSelectedAssetId("coin");
        c.placeElement(5 * 32 + 16, (rows - 3) * 32);
        c.placeElement(6 * 32 + 16, (rows - 3) * 32);
        model.setSelectedAssetId("flag");
        c.placeElement((cols - 2) * 32, (rows - 3) * 32);
        check(model.level().elements().size() == 4, "level has 4 placed actors");

        // --- the actual brush gesture through the canvas (pointer -> paint/place) ---
        // Resilient to zoom/pan: assert a tile was painted / an actor stamped, not a
        // specific cell. Compute a click well inside the grid from the live zoom.
        EditorCanvas canvas = new EditorCanvas(c);
        canvas.setWidth(1000);
        canvas.setHeight(700);
        // Cell size is auto-fit and decoupled from the game's tile size, so target a cell
        // using the canvas's real cell size (mid-cell, well inside the grid).
        double cs = canvas.cellSize();
        int clickX = 14 + (int) (8 * cs + cs / 2);   // ~ column 8 (panX = 14)
        int clickY = 10 + (int) (8 * cs + cs / 2);   // ~ row 8    (panY = 10)
        model.setTool(Tool.BRUSH);
        model.setActiveLayer("Terrain");
        model.setSelectedAssetId("brick");
        int tilesBefore = model.level().getLayer("Terrain").tiles().size();
        canvas.pointerPressed(clickX, clickY);
        canvas.pointerReleased(clickX, clickY);
        check(model.level().getLayer("Terrain").tiles().size() == tilesBefore + 1,
                "canvas brush painted a tile");

        model.setActiveLayer("Actors");
        model.setSelectedAssetId("gem");
        int beforeStamp = model.level().elements().size();
        canvas.pointerPressed(clickX + (int) (3 * cs), clickY);
        canvas.pointerReleased(clickX + (int) (3 * cs), clickY);
        check(model.level().elements().size() == beforeStamp + 1, "canvas brush stamped an actor");

        // --- realize into a live Scene (the game actually builds) ---
        Scene scene = new Scene();
        model.level().realizeSprites(scene, cat);
        check(scene.size() > 0, "level realizes into sprites (" + scene.size() + ")");

        // --- realize through the runtime GameSceneView (what a shipped game uses) ---
        GameSceneView view = new GameSceneView(GameLevel.load(model.level().toJson()), cat);
        check(view.getScene().size() == scene.size(), "GameSceneView realizes the same scene");

        // --- save to a project and reload (build artifacts survive) ---
        File base = new File("target/itest-workflow").getAbsoluteFile();
        deleteRecursive(base);
        String games = new File(base, "games").getAbsolutePath();
        String src = new File(base, "src").getAbsolutePath();
        ProjectBinding binding = ProjectBinding.parse(
                "gamesDir=" + games + "\nsourceDir=" + src + "\npackageName=com.example.arena\n");
        ProjectIO.saveScene(binding, model, "com.example.arena");
        check(new File(games, "Arena.game").isFile(), "saved Arena.game");
        check(new File(src, "com/example/arena/Arena.java").isFile(), "generated companion Arena.java");

        GameLevel reloaded = ProjectIO.loadScene(binding, "Arena");
        check(reloaded.elements().size() == model.level().elements().size(), "reloaded element count matches");
        Scene reScene = new Scene();
        reloaded.realizeSprites(reScene, cat);
        check(reScene.size() == scene.size(), "reloaded level realizes the same sprite count");

        // --- undo across the whole build still works ---
        int n = model.level().elements().size();
        c.undo();
        check(model.level().elements().size() != n || c.canRedo(), "undo affects the build history");

        // --- terrain sculpting (3D): elevation, holes, walls round-trip through JSON ---
        EditorController t3 = new EditorController(new EditorModel(StarterPacks.newLevel(GameLevel.Mode.THREE_D), cat));
        t3.paintTerrain(2, 3, com.codename1.gamebuilder.editor.TerrainBrush.RAISE);
        t3.paintTerrain(2, 3, com.codename1.gamebuilder.editor.TerrainBrush.RAISE);
        t3.paintTerrain(4, 4, com.codename1.gamebuilder.editor.TerrainBrush.GROUND);   // carve a hole
        t3.paintTerrain(5, 5, com.codename1.gamebuilder.editor.TerrainBrush.WALL);
        com.codename1.gaming.level.TerrainGrid tg = t3.model().level().getTerrain();
        check(tg != null, "terrain grid created on first paint");
        check(Math.abs(tg.getHeight(2, 3) - 2 * EditorController.TERRAIN_STEP) < 1e-4, "two RAISE dabs stack elevation");
        check(!tg.hasGround(4, 4), "GROUND brush carves a hole");
        check(tg.getWall(5, 5) > 0f, "WALL brush stamps a wall");
        GameLevel rt = GameLevel.load(t3.model().level().toJson());
        com.codename1.gaming.level.TerrainGrid rg = rt.getTerrain();
        check(rg != null && Math.abs(rg.getHeight(2, 3) - tg.getHeight(2, 3)) < 1e-4
                && !rg.hasGround(4, 4) && rg.getWall(5, 5) > 0f, "terrain survives JSON round-trip");
        // paint a road surface and confirm it persists through JSON
        t3.model().setTerrainMaterial(com.codename1.gaming.level.TerrainGrid.MAT_ROAD);
        t3.paintTerrain(6, 6, com.codename1.gamebuilder.editor.TerrainBrush.PAINT);
        check(tg.getMaterial(6, 6) == com.codename1.gaming.level.TerrainGrid.MAT_ROAD, "PAINT sets the surface material");
        GameLevel rt2 = GameLevel.load(t3.model().level().toJson());
        check(rt2.getTerrain().getMaterial(6, 6) == com.codename1.gaming.level.TerrainGrid.MAT_ROAD,
                "surface material survives JSON round-trip");
        t3.fillGround(false);
        check(!t3.model().level().getTerrain().hasGround(0, 0), "Clear-to-sky removes the floor");
    }

    private static void deleteRecursive(File f) {
        if (f.isDirectory()) {
            File[] kids = f.listFiles();
            if (kids != null) {
                for (File k : kids) {
                    deleteRecursive(k);
                }
            }
        }
        f.delete();
    }
}
