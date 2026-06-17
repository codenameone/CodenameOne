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

import com.codename1.gaming.level.GameElement;
import com.codename1.gaming.level.GameWorld;
import com.codename1.gaming.level.Material;
import com.codename1.gaming.level.MaterialRegistry;
import com.codename1.gaming.level.Region;
import com.codename1.gaming.level.StreamingTerrain;
import com.codename1.gaming.level.Terrain;
import com.codename1.gaming.level.TerrainChunk;
import com.codename1.gaming.level.TerrainFeature;

import java.util.ArrayList;
import java.util.List;

/// Runnable mirror of the core GameWorldTest, exercising the streaming/region API against the
/// freshly-built core (resolved from .m2-local). Run via the exec harness idiom.
public final class GameWorldHarness {
    private static final List<String> fail = new ArrayList<String>();

    private static void check(boolean cond, String msg) {
        if (!cond) {
            fail.add(msg);
            System.out.println("  FAIL: " + msg);
        }
    }

    public static void main(String[] args) {
        // pluggable materials
        check(MaterialRegistry.contains(MaterialRegistry.GRASS), "grass registered");
        check(MaterialRegistry.get(MaterialRegistry.WATER).isSolid(), "water solid");
        check(MaterialRegistry.get("nope") != null, "unknown material -> placeholder");
        MaterialRegistry.register(new Material("lava", "Lava", 0xd2401a).setSolid(true).setFriction(0.5));
        check(MaterialRegistry.contains("lava"), "custom material registered");

        // streaming terrain across chunks + negative coords
        StreamingTerrain t = new StreamingTerrain();
        t.setHeight(3, 5, 2.5f);
        t.setMaterial(3, 5, MaterialRegistry.ROAD);
        t.setHeight(-1, -20, 4f);
        t.setMaterial(-1, -20, MaterialRegistry.SAND);
        check(t.getHeight(3, 5) == 2.5f, "height in chunk");
        check(MaterialRegistry.ROAD.equals(t.getMaterial(3, 5)), "material in chunk");
        check(t.getHeight(-1, -20) == 4f, "height negative coords");
        check(MaterialRegistry.SAND.equals(t.getMaterial(-1, -20)), "material negative coords");
        check(MaterialRegistry.GRASS.equals(t.getMaterial(100, 100)), "default grass");
        t.setHeight(7, 7, Terrain.NO_GROUND);
        check(!t.hasGround(7, 7), "NO_GROUND hole");
        check(!t.isBounded() && t.getCols() == -1, "unbounded");

        // LRU eviction persists dirty chunks
        StreamingTerrain s = new StreamingTerrain(new StreamingTerrain.MemoryChunkProvider(), 4);
        for (int i = 0; i < 12; i++) {
            s.setHeight(i * TerrainChunk.SIZE, 0, i + 1);
        }
        check(s.loadedChunkCount() <= 4, "cache bounded (" + s.loadedChunkCount() + ")");
        check(s.getHeight(0, 0) == 1f, "evicted chunk persisted+reloaded");
        check(s.getHeight(11 * TerrainChunk.SIZE, 0) == 12f, "last chunk intact");

        // variable-size feature
        t.addFeature(new TerrainFeature("w1", TerrainFeature.TYPE_WALL)
                .setPosition(2.5, 0, 3.0).setSize(4, 2.2, 0.5).setMaterial(MaterialRegistry.STONE));
        check(t.features().size() >= 1, "feature added");

        // region JSON round-trip
        Region r = new Region("forest-1", "Forest").setOrigin(0, 0).setSpan(256, 256).link("east", "forest-2");
        r.elements().add(new GameElement("e1", "tree3d").setName("Oak").setPosition(40, 0, 12));
        r.terrain().setHeight(1, 1, 3f);
        r.terrain().setMaterial(1, 1, MaterialRegistry.DIRT);
        r.terrain().addFeature(new TerrainFeature("w", TerrainFeature.TYPE_WALL).setPosition(5, 0, 5).setSize(3, 2, 1));
        Region back = Region.fromJson(r.toJson());
        check("forest-1".equals(back.getId()), "region id round-trip");
        check(back.getWidth() == 256.0, "region span round-trip");
        check("forest-2".equals(back.neighbors().get("east")), "neighbor link round-trip");
        check(back.elements().size() == 1 && "Oak".equals(back.elements().get(0).getName()), "region elements round-trip");
        check(back.terrain().getHeight(1, 1) == 3f, "region terrain height round-trip");
        check(MaterialRegistry.DIRT.equals(back.terrain().getMaterial(1, 1)), "region terrain material round-trip");
        check(back.terrain().features().size() == 1, "region feature round-trip");

        // world streaming / region crossing
        GameWorld world = new GameWorld();
        world.addRegion(new Region("a", "A").setOrigin(0, 0).setSpan(100, 100).link("east", "b"));
        world.addRegion(new Region("b", "B").setOrigin(100, 0).setSpan(100, 100).link("west", "a").link("east", "c"));
        world.addRegion(new Region("c", "C").setOrigin(200, 0).setSpan(100, 100).link("west", "b"));
        check("a".equals(world.getActiveRegion().getId()), "initial active region");
        world.update(150, 50);
        check("b".equals(world.getActiveRegion().getId()), "crossed into region B");
        check(world.getRegion("a") != null && world.getRegion("c") != null, "neighbors kept resident");

        // GameLevel <-> GameWorld JSON integration (large-world level persists through .game)
        try {
            com.codename1.gaming.level.GameLevel lvl = new com.codename1.gaming.level.GameLevel(
                    com.codename1.gaming.level.GameLevel.MODE_3D);
            GameWorld w = new GameWorld();
            Region home = new Region("home", "Home").setOrigin(0, 0).setSpan(128, 128).link("east", "east-1");
            home.terrain().setHeight(2, 2, 1.5f);
            home.terrain().setMaterial(2, 2, MaterialRegistry.ROAD);
            w.addRegion(home);
            w.addRegion(new Region("east-1", "East").setOrigin(128, 0).setSpan(128, 128).link("west", "home"));
            lvl.setWorld(w);
            check(lvl.isLargeWorld(), "level marked large-world");
            com.codename1.gaming.level.GameLevel lback = com.codename1.gaming.level.GameLevel.load(lvl.toJson());
            check(lback.isLargeWorld(), "world survives .game round-trip");
            check(lback.getWorld().getActiveRegion() != null
                    && "home".equals(lback.getWorld().getActiveRegion().getId()), "active region preserved");
            check(lback.getWorld().getRegion("home").terrain().getHeight(2, 2) == 1.5f, "region terrain in .game");
            check("east-1".equals(lback.getWorld().getActiveRegion().neighbors().get("east")), "neighbor links in .game");
        } catch (Exception ex) {
            fail.add("GameLevel world round-trip threw: " + ex);
            com.codename1.io.Log.e(ex);
        }

        System.out.println("[GameWorld] failures=" + fail.size());
        System.out.println("[GameWorld] RESULT " + (fail.isEmpty() ? "OK" : "FAIL"));
    }
}
