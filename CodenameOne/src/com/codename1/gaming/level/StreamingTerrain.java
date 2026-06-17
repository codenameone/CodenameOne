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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// An unbounded `Terrain` that pages `TerrainChunk`s in and out through a `ChunkProvider`,
/// keeping at most `#getCacheSize()` chunks resident (LRU). World cell coordinates may be
/// negative; each maps to a chunk and a local cell. Call `#streamAround(int,int,int)` as the
/// player moves to pre-load nearby chunks and evict (saving dirty) far ones.
public class StreamingTerrain implements Terrain {
    private final ChunkProvider provider;
    private final LinkedHashMap<Long, TerrainChunk> loaded;
    private int cacheSize;

    public StreamingTerrain() {
        this(new MemoryChunkProvider(), 64);
    }

    public StreamingTerrain(ChunkProvider provider, int cacheSize) {
        this.provider = provider;
        this.cacheSize = Math.max(4, cacheSize);
        this.loaded = new LinkedHashMap<Long, TerrainChunk>(16, 0.75f, true) {
            protected boolean removeEldestEntry(Map.Entry<Long, TerrainChunk> eldest) {
                if (size() > StreamingTerrain.this.cacheSize) {
                    TerrainChunk c = eldest.getValue();
                    if (c.isDirty()) {
                        StreamingTerrain.this.provider.saveChunk(c);
                        c.clearDirty();
                    }
                    return true;
                }
                return false;
            }
        };
    }

    public int getCacheSize() {
        return cacheSize;
    }

    public StreamingTerrain setCacheSize(int cacheSize) {
        this.cacheSize = Math.max(4, cacheSize);
        return this;
    }

    public ChunkProvider getProvider() {
        return provider;
    }

    static int floorDiv(int a, int b) {
        int q = a / b;
        if ((a % b != 0) && ((a < 0) != (b < 0))) {
            q--;
        }
        return q;
    }

    private static int floorMod(int a, int b) {
        int r = a % b;
        if (r != 0 && ((a < 0) != (b < 0))) {
            r += b;
        }
        return r;
    }

    private static long key(int cx, int cz) {
        return (((long) cx) << 32) ^ (cz & 0xffffffffL);
    }

    /// Returns the resident chunk for the given chunk coords, loading it on demand.
    public TerrainChunk chunk(int cx, int cz) {
        Long k = Long.valueOf(key(cx, cz));
        TerrainChunk c = loaded.get(k);
        if (c == null) {
            c = provider.loadChunk(cx, cz);
            loaded.put(k, c);
        }
        return c;
    }

    private TerrainChunk chunkForCell(int x, int z) {
        return chunk(floorDiv(x, TerrainChunk.SIZE), floorDiv(z, TerrainChunk.SIZE));
    }

    public float getHeight(int x, int z) {
        return chunkForCell(x, z).getHeight(floorMod(x, TerrainChunk.SIZE), floorMod(z, TerrainChunk.SIZE));
    }

    public void setHeight(int x, int z, float height) {
        chunkForCell(x, z).setHeight(floorMod(x, TerrainChunk.SIZE), floorMod(z, TerrainChunk.SIZE), height);
    }

    public boolean hasGround(int x, int z) {
        return getHeight(x, z) != NO_GROUND;
    }

    public String getMaterial(int x, int z) {
        return chunkForCell(x, z).getMaterial(floorMod(x, TerrainChunk.SIZE), floorMod(z, TerrainChunk.SIZE));
    }

    public void setMaterial(int x, int z, String materialId) {
        chunkForCell(x, z).setMaterial(floorMod(x, TerrainChunk.SIZE), floorMod(z, TerrainChunk.SIZE), materialId);
    }

    public List<TerrainFeature> features() {
        List<TerrainFeature> all = new ArrayList<TerrainFeature>();
        for (TerrainChunk c : loaded.values()) {
            all.addAll(c.features());
        }
        return all;
    }

    public void addFeature(TerrainFeature feature) {
        chunkForCell((int) Math.floor(feature.getX()), (int) Math.floor(feature.getZ())).features().add(feature);
        chunkForCell((int) Math.floor(feature.getX()), (int) Math.floor(feature.getZ())).touch();
    }

    public boolean isBounded() {
        return false;
    }

    public int getCols() {
        return -1;
    }

    public int getRows() {
        return -1;
    }

    /// Pre-loads the (2*radius+1)^2 chunks around the given cell so movement is seamless; the
    /// LRU cache evicts (and saves) chunks that fall out of range.
    public void streamAround(int cellX, int cellZ, int radius) {
        int ccx = floorDiv(cellX, TerrainChunk.SIZE);
        int ccz = floorDiv(cellZ, TerrainChunk.SIZE);
        // touch in nearest-first order so the farthest become the LRU eviction candidates
        for (int r = 0; r <= radius; r++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dx = -r; dx <= r; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) == r) {
                        chunk(ccx + dx, ccz + dz);
                    }
                }
            }
        }
    }

    /// Persists every dirty resident chunk through the provider.
    public void flush() {
        for (TerrainChunk c : loaded.values()) {
            if (c.isDirty()) {
                provider.saveChunk(c);
                c.clearDirty();
            }
        }
    }

    public int loadedChunkCount() {
        return loaded.size();
    }

    /// The currently-resident chunks (snapshot) -- used to persist a small region inline.
    public List<TerrainChunk> residentChunks() {
        return new ArrayList<TerrainChunk>(loaded.values());
    }

    /// A trivial in-memory provider: remembers chunks it has seen, generates empty flat-grass
    /// chunks otherwise. Useful as a default and for tests.
    public static final class MemoryChunkProvider implements ChunkProvider {
        private final Map<Long, TerrainChunk> store = new HashMap<Long, TerrainChunk>();

        public TerrainChunk loadChunk(int cx, int cz) {
            TerrainChunk c = store.get(Long.valueOf(key(cx, cz)));
            return c != null ? c : new TerrainChunk(cx, cz);
        }

        public void saveChunk(TerrainChunk chunk) {
            store.put(Long.valueOf(key(chunk.getChunkX(), chunk.getChunkZ())), chunk);
        }
    }
}
