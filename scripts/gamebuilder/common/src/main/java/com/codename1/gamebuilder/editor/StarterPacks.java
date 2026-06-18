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
package com.codename1.gamebuilder.editor;

import com.codename1.gaming.level.AssetCatalog;
import com.codename1.gaming.level.AssetDef;
import com.codename1.gaming.level.GameElement;
import com.codename1.gaming.level.GameLevel;
import com.codename1.gaming.level.Layer;
import com.codename1.gaming.level.LevelLight;
import com.codename1.gaming.level.TerrainGrid;

import java.io.IOException;
import java.io.InputStream;

/// Loads the bundled starter asset packs and builds empty per-mode level templates.
///
/// The packs (platformer / top-down / board / 3D kit) live in the classpath resource
/// {@code /gamebuilder-packs.json} and are parsed by `AssetCatalog#load(java.io.InputStream)`,
/// so the same catalog drives the editor palette and a generated game's runtime loader.
public final class StarterPacks {
    public static final String RESOURCE = "/gamebuilder-packs.json";

    private StarterPacks() {
    }

    /// Loads every starter pack into a fresh catalog.
    public static AssetCatalog loadCatalog() {
        try (InputStream in = StarterPacks.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("missing resource " + RESOURCE);
            }
            return AssetCatalog.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("failed to read " + RESOURCE, e);
        }
    }

    /// The default asset pack id for a level mode.
    public static String defaultPackFor(int mode) {
        return switch (mode) {
            case GameLevel.MODE_3D -> "kit3d";
            case GameLevel.MODE_BOARD -> "board";
            default -> "platformer";
        };
    }

    /// A blank level for the given mode, pre-populated with the conventional layers
    /// (and, for 3D, a light + a small flat terrain) so the editor opens onto something
    /// usable.
    public static GameLevel newLevel(int mode) {
        GameLevel level = new GameLevel(mode);
        level.setAssetPack(defaultPackFor(mode));
        switch (mode) {
            case GameLevel.MODE_3D -> {
                level.setGrid(16, 16, 1);
                level.setCamera(0, 8, 14, 0, 0, 0).setLens(60, 0.1f, 500);
                level.lights().add(new LevelLight(0.4f, -1f, 0.3f, 0xfffff2e0, 0xff2a2f3a));
                level.setTerrain(new TerrainGrid(16, 16, 1f));
                level.addLayer(new Layer("Models", Layer.KIND_MODEL).setBand(0));
            }
            case GameLevel.MODE_BOARD -> {
                level.setGrid(10, 10, 64);
                level.addLayer(new Layer("Board", Layer.KIND_TILE).setBand(0));
                level.addLayer(new Layer("Pieces", Layer.KIND_ENTITY).setBand(2));
            }
            default -> {
                level.setGrid(26, 16, 32);
                level.addLayer(new Layer("Background", Layer.KIND_TILE).setBand(0).setParallax(0.4f, 0.6f));
                level.addLayer(new Layer("Terrain", Layer.KIND_TILE).setBand(1));
                level.addLayer(new Layer("Items", Layer.KIND_ENTITY).setBand(2));
                level.addLayer(new Layer("Actors", Layer.KIND_ENTITY).setBand(3));
            }
        }
        return level;
    }

    /// A "large world" starter: a 3D level backed by a streaming `GameWorld` of linked regions
    /// (rather than a single bounded grid), with a flat grass home region and an empty eastern
    /// neighbour to demonstrate seamless transitions. The active region's `StreamingTerrain` is
    /// the authoritative terrain; chunks page in/out so the world can be arbitrarily large.
    public static GameLevel newLargeWorld() {
        GameLevel level = newLevel(GameLevel.MODE_3D);
        level.props().put("view3d", "open");
        com.codename1.gaming.level.GameWorld world = new com.codename1.gaming.level.GameWorld();
        com.codename1.gaming.level.Region home =
                new com.codename1.gaming.level.Region("home", "Home").setOrigin(0, 0).setSpan(64, 64);
        // a flat grass starter patch around the spawn
        for (int z = 0; z < 24; z++) {
            for (int x = 0; x < 24; x++) {
                home.terrain().setHeight(x, z, 0f);
                home.terrain().setMaterial(x, z, com.codename1.gaming.level.MaterialRegistry.GRASS);
            }
        }
        home.link("east", "east-1");
        com.codename1.gaming.level.Region east =
                new com.codename1.gaming.level.Region("east-1", "East Field").setOrigin(64, 0).setSpan(64, 64);
        east.link("west", "home");
        world.addRegion(home);
        world.addRegion(east);
        world.setActiveRegion("home");
        level.setWorld(world);
        return level;
    }

    /// A small populated platformer level (a "Pixel Quest"-style starter) so the editor
    /// opens onto a real scene rather than a blank grid.
    public static GameLevel demoLevel() {
        GameLevel level = newLevel(GameLevel.MODE_2D);
        int cols = level.getCols();
        int rows = level.getRows();
        int ts = level.getTileSize();
        Layer terrain = level.getLayer("Terrain");
        for (int c = 0; c < cols; c++) {
            if (c == 13 || c == 14) {
                continue; // a pit
            }
            terrain.putTile(c, rows - 1, "ground");
            terrain.putTile(c, rows - 2, "grass");
        }
        int[][] bricks = {{5, 11}, {6, 11}, {7, 11}, {11, 9}, {17, 8}, {18, 8}, {19, 8}};
        for (int[] b : bricks) {
            terrain.putTile(b[0], b[1], "brick");
        }
        terrain.putTile(9, rows - 3, "crate");
        terrain.putTile(9, rows - 4, "crate");

        AssetCatalog cat = loadCatalog();
        int id = 0;
        // surface top is the grass row (rows-2); ground-walkers stand on it (feet on the line)
        id = addActor(level, cat, ++id, "player", 2 * ts + ts / 2, standY(cat, "player", rows, ts));
        id = addActor(level, cat, ++id, "coin", 5 * ts + 4, 10 * ts + 4);
        id = addActor(level, cat, ++id, "coin", 6 * ts + 4, 10 * ts + 4);
        id = addActor(level, cat, ++id, "coin", 7 * ts + 4, 10 * ts + 4);
        id = addActor(level, cat, ++id, "gem", 18 * ts + 4, 7 * ts + 4);
        id = addActor(level, cat, ++id, "slime", 6 * ts + ts / 2, standY(cat, "slime", rows, ts));
        id = addActor(level, cat, ++id, "door", 24 * ts + ts / 2, standY(cat, "door", rows, ts));
        addActor(level, cat, ++id, "flag", 19 * ts + ts / 2, 8 * ts - flagHalf(cat));
        return level;
    }

    /// Centre-Y that rests an actor's feet on the grass surface (top of row {@code rows-2}).
    private static int standY(AssetCatalog cat, String assetId, int rows, int ts) {
        AssetDef def = cat == null ? null : cat.def(assetId);
        int h = def == null ? ts : def.getHeight();
        return (rows - 2) * ts - h / 2;
    }

    private static int flagHalf(AssetCatalog cat) {
        AssetDef def = cat == null ? null : cat.def("flag");
        return (def == null ? 44 : def.getHeight()) / 2;
    }

    private static int addActor(GameLevel level, AssetCatalog cat, int id, String assetId, int x, int y) {
        GameElement el = new GameElement("e" + id, assetId).setName(assetId + " " + id);
        el.setLayer(level.getLayer("Items") != null && isItem(assetId) ? "Items" : "Actors");
        el.setPosition(x, y);
        AssetDef def = cat == null ? null : cat.def(assetId);
        if (def != null) {
            el.properties().putAll(def.defaultProperties());
        }
        level.addElement(el);
        return id;
    }

    private static boolean isItem(String assetId) {
        return assetId.equals("coin") || assetId.equals("gem");
    }
}
