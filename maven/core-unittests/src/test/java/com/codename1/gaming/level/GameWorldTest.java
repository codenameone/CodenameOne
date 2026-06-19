package com.codename1.gaming.level;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Tests for the streaming/region game-space model: pluggable materials, chunked streaming
/// terrain (including negative coordinates and LRU eviction), variable-size terrain features,
/// region JSON round-trip, and region streaming in a GameWorld. Pure (no Display).
class GameWorldTest {

    @Test
    void materialRegistryPluggable() {
        assertTrue(MaterialRegistry.contains(MaterialRegistry.GRASS));
        assertEquals(0x2f6fa8, MaterialRegistry.get(MaterialRegistry.WATER).getColor());
        assertTrue(MaterialRegistry.get(MaterialRegistry.WATER).isSolid());
        // unknown id resolves to a non-null placeholder
        assertNotNull(MaterialRegistry.get("does-not-exist"));
        // register a custom material
        MaterialRegistry.register(new Material("lava", "Lava", 0xd2401a).setSolid(true).setFriction(0.5));
        assertTrue(MaterialRegistry.contains("lava"));
        assertEquals(0.5, MaterialRegistry.get("lava").getFriction(), 1e-6);
    }

    @Test
    void streamingTerrainAcrossChunksAndNegatives() {
        StreamingTerrain t = new StreamingTerrain();
        // a cell well inside one chunk and one across a chunk boundary into negative space
        t.setHeight(3, 5, 2.5f);
        t.setMaterial(3, 5, MaterialRegistry.ROAD);
        t.setHeight(-1, -20, 4f);
        t.setMaterial(-1, -20, MaterialRegistry.SAND);
        assertEquals(2.5f, t.getHeight(3, 5), 1e-6);
        assertEquals(MaterialRegistry.ROAD, t.getMaterial(3, 5));
        assertEquals(4f, t.getHeight(-1, -20), 1e-6);
        assertEquals(MaterialRegistry.SAND, t.getMaterial(-1, -20));
        // unset cell defaults to flat grass, no ground sentinel honored
        assertEquals(MaterialRegistry.GRASS, t.getMaterial(100, 100));
        t.setHeight(7, 7, Terrain.NO_GROUND);
        assertFalse(t.hasGround(7, 7));
        assertTrue(t.isBounded() == false && t.getCols() == -1);
    }

    @Test
    void streamingTerrainEvictsButPersistsDirtyChunks() {
        StreamingTerrain t = new StreamingTerrain(new StreamingTerrain.MemoryChunkProvider(), 4);
        // write into many far-apart chunks, exceeding the 4-chunk cache
        for (int i = 0; i < 12; i++) {
            t.setHeight(i * TerrainChunk.SIZE, 0, i + 1);
        }
        assertTrue(t.loadedChunkCount() <= 4, "cache bounded to 4, was " + t.loadedChunkCount());
        // an evicted chunk's edit must have been persisted by the provider and reload intact
        assertEquals(1f, t.getHeight(0, 0), 1e-6);
        assertEquals(12f, t.getHeight(11 * TerrainChunk.SIZE, 0), 1e-6);
    }

    @Test
    void terrainFeatureVariableSizeRoundTrip() {
        StreamingTerrain t = new StreamingTerrain();
        TerrainFeature wall = new TerrainFeature("w1", TerrainFeature.TYPE_WALL)
                .setPosition(2.5, 0, 3.0).setSize(4, 2.2, 0.5).setMaterial(MaterialRegistry.STONE);
        t.addFeature(wall);
        assertEquals(1, t.features().size());
        assertEquals(4.0, t.features().get(0).getWidth(), 1e-6);
        assertEquals(TerrainFeature.TYPE_WALL, t.features().get(0).getType());
    }

    @Test
    void regionJsonRoundTrip() {
        Region r = new Region("forest-1", "Forest").setOrigin(0, 0).setSpan(256, 256).link("east", "forest-2");
        r.elements().add(new GameElement("e1", "tree3d").setName("Oak").setPosition(40, 0, 12));
        r.terrain().setHeight(1, 1, 3f);
        r.terrain().setMaterial(1, 1, MaterialRegistry.DIRT);
        r.terrain().addFeature(new TerrainFeature("w", TerrainFeature.TYPE_WALL).setPosition(5, 0, 5).setSize(3, 2, 1));

        Region back = Region.fromJson(r.toJson());
        assertEquals("forest-1", back.getId());
        assertEquals(256.0, back.getWidth(), 1e-6);
        assertEquals("forest-2", back.neighbors().get("east"));
        assertEquals(1, back.elements().size());
        assertEquals("Oak", back.elements().get(0).getName());
        assertEquals(3f, back.terrain().getHeight(1, 1), 1e-6);
        assertEquals(MaterialRegistry.DIRT, back.terrain().getMaterial(1, 1));
        assertEquals(1, back.terrain().features().size());
    }

    @Test
    void worldStreamsAndCrossesRegions() {
        GameWorld world = new GameWorld();
        Region a = new Region("a", "A").setOrigin(0, 0).setSpan(100, 100).link("east", "b");
        Region b = new Region("b", "B").setOrigin(100, 0).setSpan(100, 100).link("west", "a").link("east", "c");
        Region c = new Region("c", "C").setOrigin(200, 0).setSpan(100, 100).link("west", "b");
        world.addRegion(a);
        world.addRegion(b);
        world.addRegion(c);
        assertEquals("a", world.getActiveRegion().getId());
        // walk east into region B's bounds -> active region follows
        world.update(150, 50);
        assertEquals("b", world.getActiveRegion().getId());
        // neighbours of B (a and c) are kept resident within keepRadius=1
        assertNotNull(world.getRegion("a"));
        assertNotNull(world.getRegion("c"));
    }
}
