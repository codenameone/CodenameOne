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
package com.codename1.gaming.level;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Round-trip and behavior tests for the {@code com.codename1.gaming.level} data model.
/// These are pure (no {@code Display}) so they do not extend {@code UITestBase}: they
/// only exercise the model, JSON serialization and the isometric projection.
class GameLevelTest {

    @Test
    void roundTrip2D() throws Exception {
        GameLevel level = new GameLevel(GameLevel.Mode.TWO_D);
        level.setAssetPack("platformer").setGrid(26, 16, 32);
        level.props().put("gravity", 9.8);
        level.props().put("title", "Pixel Quest");

        Layer terrain = new Layer("Terrain", Layer.Kind.TILE).setBand(1);
        terrain.putTile(0, 15, "ground").putTile(1, 15, "ground").putTile(5, 11, "brick");
        Layer actors = new Layer("Actors", Layer.Kind.ENTITY).setBand(3);
        level.addLayer(terrain).addLayer(actors);

        GameElement player = new GameElement("e1", "player").setName("Player 1").setLayer("Actors");
        player.setPosition(160, 224).setProperty("lives", 3).setProperty("jumpHeight", 96);
        GameElement coin = new GameElement("e2", "coin").setLayer("Actors").setPosition(200, 100);
        coin.setProperty("value", 10);
        level.addElement(player).addElement(coin);

        GameLevel back = GameLevel.load(level.toJson());

        assertEquals(GameLevel.Mode.TWO_D, back.getMode());
        assertEquals("platformer", back.getAssetPack());
        assertEquals(26, back.getCols());
        assertEquals(16, back.getRows());
        assertEquals(32, back.getTileSize());
        assertEquals(9.8, back.getDouble("gravity", 0), 0.0001);
        assertEquals("Pixel Quest", back.getString("title", null));

        assertEquals(2, back.layers().size());
        Layer bt = back.getLayer("Terrain");
        assertNotNull(bt);
        assertEquals(Layer.Kind.TILE, bt.getKind());
        assertEquals(1, bt.getBand());
        assertEquals(3, bt.tiles().size());
        assertEquals("brick", bt.getTile(5, 11));
        assertEquals("ground", bt.getTile(0, 15));

        assertEquals(2, back.elements().size());
        GameElement bp = back.elements().get(0);
        assertEquals("e1", bp.getId());
        assertEquals("player", bp.getAssetId());
        assertEquals("Player 1", bp.getName());
        assertEquals("Actors", bp.getLayer());
        assertEquals(160, bp.getX(), 0.0001);
        assertEquals(224, bp.getY(), 0.0001);
        assertEquals(3, bp.getInt("lives", 0));
        assertEquals(96, bp.getInt("jumpHeight", 0));
        assertEquals(10, back.elements().get(1).getInt("value", 0));
    }

    @Test
    void roundTrip3D() throws Exception {
        GameLevel level = new GameLevel(GameLevel.Mode.THREE_D);
        level.setAssetPack("kit3d").setGrid(8, 8, 1);
        level.setCamera(0, 6, 12, 0, 0, 0).setLens(55, 0.2f, 800);
        level.lights().add(new LevelLight(0.3f, -1f, 0.2f, 0xfffff0e0, 0xff303040));
        TerrainGrid grid = new TerrainGrid(3, 2, 2f);
        grid.setHeight(0, 0, 1.5f).setHeight(2, 1, -0.5f);
        level.setTerrain(grid);

        GameElement crate = new GameElement("m1", "crate").setLayer("Models");
        crate.setPosition(1, 0.5, -2).setScale(2, 1, 1).setRotation(30);
        level.addLayer(new Layer("Models", Layer.Kind.MODEL).setBand(0));
        level.addElement(crate);

        GameLevel back = GameLevel.load(level.toJson());

        assertEquals(GameLevel.Mode.THREE_D, back.getMode());
        assertEquals(6f, back.getEyeY(), 0.0001);
        assertEquals(12f, back.getEyeZ(), 0.0001);
        assertEquals(55f, back.getFov(), 0.0001);
        assertEquals(0.2f, back.getNear(), 0.0001);
        assertEquals(800f, back.getFar(), 0.0001);

        assertEquals(1, back.lights().size());
        LevelLight bl = back.lights().get(0);
        assertEquals(-1f, bl.getDirectionY(), 0.0001);
        assertEquals(0xfffff0e0, bl.getColor());
        assertEquals(0xff303040, bl.getAmbientColor());

        TerrainGrid bg = back.getTerrain();
        assertNotNull(bg);
        assertEquals(3, bg.getCols());
        assertEquals(2, bg.getRows());
        assertEquals(2f, bg.getCellSize(), 0.0001);
        assertEquals(1.5f, bg.getHeight(0, 0), 0.0001);
        assertEquals(-0.5f, bg.getHeight(2, 1), 0.0001);

        GameElement bc = back.elements().get(0);
        assertEquals(-2, bc.getZ(), 0.0001);
        assertEquals(2f, bc.getScaleX(), 0.0001);
        assertEquals(30f, bc.getRotation(), 0.0001);
    }

    @Test
    void roundTripBoard() throws Exception {
        GameLevel level = new GameLevel(GameLevel.Mode.BOARD);
        level.setAssetPack("board").setGrid(8, 8, 64);
        Layer squares = new Layer("Board", Layer.Kind.TILE).setBand(0);
        squares.putTile(0, 0, "boardtile").putTile(7, 7, "start");
        level.addLayer(squares);
        // board elements store their column/row in x/y
        level.addElement(new GameElement("t1", "token").setLayer("Board").setPosition(3, 5)
                .setProperty("player", "P1"));

        GameLevel back = GameLevel.load(level.toJson());
        assertEquals(GameLevel.Mode.BOARD, back.getMode());
        assertEquals("start", back.getLayer("Board").getTile(7, 7));
        GameElement t = back.elements().get(0);
        assertEquals(3, t.getX(), 0.0001);
        assertEquals(5, t.getY(), 0.0001);
        assertEquals("P1", t.getString("player", null));
    }

    @Test
    void elementPropertyTyping() {
        GameElement e = new GameElement("x", "y");
        e.setProperty("n", Double.valueOf(42.7));
        e.setProperty("s", "13");
        e.setProperty("flagTrue", "true");
        e.setProperty("flagBool", Boolean.TRUE);

        assertEquals(43, e.getInt("n", 0));
        assertEquals(42.7, e.getDouble("n", 0), 0.0001);
        assertEquals(13, e.getInt("s", 0));            // numeric string coerces
        assertEquals(13.0, e.getDouble("s", 0), 0.0001);
        assertTrue(e.getBoolean("flagTrue", false));
        assertTrue(e.getBoolean("flagBool", false));
        assertFalse(e.getBoolean("missing", false));
        assertEquals(7, e.getInt("missing", 7));       // default fallback
        assertEquals("13", e.getString("s", null));
    }

    @Test
    void isoProjectionInverts() {
        IsoProjection iso = new IsoProjection(100f, 100f, 64f, 32f);
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                int px = Math.round(iso.tileCenterX(r, c));
                int py = Math.round(iso.tileCenterY(r, c));
                int[] cell = iso.pick(px, py);
                assertEquals(r, cell[0], "row for (" + r + "," + c + ")");
                assertEquals(c, cell[1], "col for (" + r + "," + c + ")");
            }
        }
    }

    @Test
    void assetPackLoads() throws Exception {
        String json = "{\"packs\":[{\"id\":\"platformer\",\"name\":\"Platformer\",\"assets\":["
                + "{\"id\":\"brick\",\"name\":\"Brick\",\"kind\":\"tile\",\"color\":\"#C2603A\"},"
                + "{\"id\":\"coin\",\"name\":\"Coin\",\"kind\":\"actor\",\"w\":24,\"h\":24,"
                + "\"color\":\"#F6C944\",\"defaults\":{\"value\":10}},"
                + "{\"id\":\"player\",\"kind\":\"actor\",\"w\":28,\"h\":32,\"unique\":true,"
                + "\"color\":\"#4D86FF\",\"defaults\":{\"lives\":3,\"jumpHeight\":96}}]}]}";
        AssetCatalog catalog = AssetCatalog.load(json);

        AssetDef brick = catalog.def("brick");
        assertNotNull(brick);
        assertTrue(brick.isTile());
        assertEquals(0xffC2603A, brick.getColor());

        AssetDef coin = catalog.def("coin");
        assertEquals(AssetDef.Kind.ACTOR, coin.getKind());
        assertEquals(24, coin.getWidth());
        assertEquals(10, Json.intval(coin.defaultProperties().get("value"), 0));

        AssetDef player = catalog.def("player");
        assertTrue(player.isUnique());
        assertEquals(96, Json.intval(player.defaultProperties().get("jumpHeight"), 0));

        AssetPack pack = catalog.getPack("platformer");
        assertNotNull(pack);
        assertEquals(3, pack.size());
    }

    @Test
    void layerTileHelpers() {
        Layer l = new Layer("T", Layer.Kind.TILE);
        l.putTile(2, 3, "brick");
        assertEquals("brick", l.getTile(2, 3));
        assertEquals("2,3", Layer.cellKey(2, 3));
        l.putTile(2, 3, null);                          // null clears
        assertNull(l.getTile(2, 3));
        l.putTile(4, 4, "ground").removeTile(4, 4);
        assertTrue(l.tiles().isEmpty());
    }
}
