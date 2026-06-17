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
import java.util.List;
import java.util.Map;

/// A `#SIZE`x`#SIZE` square block of terrain — the unit of streaming. Identified by its chunk
/// coordinates (`#getChunkX()`, `#getChunkZ()`), it is self-describing: a height per cell, a
/// material per cell (stored compactly as an index into the chunk's own material palette), and
/// the `TerrainFeature`s anchored within it. `StreamingTerrain` loads/saves chunks on demand so
/// a world can be arbitrarily large without holding it all in memory.
public class TerrainChunk {
    /// Cells per side of a chunk.
    public static final int SIZE = 16;

    private final int cx;
    private final int cz;
    private final float[] heights = new float[SIZE * SIZE];
    private final short[] cellMaterial = new short[SIZE * SIZE];
    private final List<String> palette = new ArrayList<String>();
    private final List<TerrainFeature> features = new ArrayList<TerrainFeature>();
    private boolean dirty;

    public TerrainChunk(int cx, int cz) {
        this.cx = cx;
        this.cz = cz;
        palette.add(MaterialRegistry.GRASS);   // index 0 default
    }

    public int getChunkX() {
        return cx;
    }

    public int getChunkZ() {
        return cz;
    }

    /// Whether the chunk has unsaved edits (used by providers to decide what to persist).
    public boolean isDirty() {
        return dirty;
    }

    public void clearDirty() {
        dirty = false;
    }

    private int idx(int localX, int localZ) {
        return localZ * SIZE + localX;
    }

    public float getHeight(int localX, int localZ) {
        if (localX < 0 || localZ < 0 || localX >= SIZE || localZ >= SIZE) {
            return Terrain.NO_GROUND;
        }
        return heights[idx(localX, localZ)];
    }

    public void setHeight(int localX, int localZ, float h) {
        if (localX >= 0 && localZ >= 0 && localX < SIZE && localZ < SIZE) {
            heights[idx(localX, localZ)] = h;
            dirty = true;
        }
    }

    public boolean hasGround(int localX, int localZ) {
        return getHeight(localX, localZ) != Terrain.NO_GROUND;
    }

    public String getMaterial(int localX, int localZ) {
        if (localX < 0 || localZ < 0 || localX >= SIZE || localZ >= SIZE) {
            return MaterialRegistry.GRASS;
        }
        int p = cellMaterial[idx(localX, localZ)];
        return p >= 0 && p < palette.size() ? palette.get(p) : MaterialRegistry.GRASS;
    }

    public void setMaterial(int localX, int localZ, String materialId) {
        if (localX < 0 || localZ < 0 || localX >= SIZE || localZ >= SIZE) {
            return;
        }
        int p = palette.indexOf(materialId);
        if (p < 0) {
            p = palette.size();
            palette.add(materialId);
        }
        cellMaterial[idx(localX, localZ)] = (short) p;
        dirty = true;
    }

    /// Features anchored in this chunk (mutate directly; mark dirty via `#touch()`).
    public List<TerrainFeature> features() {
        return features;
    }

    public void touch() {
        dirty = true;
    }

    void write(StringBuilder sb) {
        sb.append("{\"cx\":").append(cx).append(",\"cz\":").append(cz);
        sb.append(",\"palette\":[");
        for (int i = 0; i < palette.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            Json.writeString(sb, palette.get(i));
        }
        sb.append("],\"heights\":[");
        for (int i = 0; i < heights.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            Json.writeNumber(sb, heights[i]);
        }
        sb.append("],\"materials\":[");
        for (int i = 0; i < cellMaterial.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(cellMaterial[i]);
        }
        sb.append("],\"features\":[");
        for (int i = 0; i < features.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            features.get(i).write(sb);
        }
        sb.append("]}");
    }

    static TerrainChunk read(Map<String, Object> m) {
        TerrainChunk c = new TerrainChunk(Json.intval(m.get("cx"), 0), Json.intval(m.get("cz"), 0));
        c.palette.clear();
        List<Object> pal = Json.asList(m.get("palette"));
        if (pal != null) {
            for (int i = 0; i < pal.size(); i++) {
                c.palette.add(Json.str(pal.get(i), MaterialRegistry.GRASS));
            }
        }
        if (c.palette.isEmpty()) {
            c.palette.add(MaterialRegistry.GRASS);
        }
        List<Object> hs = Json.asList(m.get("heights"));
        if (hs != null) {
            for (int i = 0; i < hs.size() && i < c.heights.length; i++) {
                c.heights[i] = (float) Json.num(hs.get(i), 0);
            }
        }
        List<Object> ms = Json.asList(m.get("materials"));
        if (ms != null) {
            for (int i = 0; i < ms.size() && i < c.cellMaterial.length; i++) {
                c.cellMaterial[i] = (short) Json.intval(ms.get(i), 0);
            }
        }
        List<Object> fs = Json.asList(m.get("features"));
        if (fs != null) {
            for (int i = 0; i < fs.size(); i++) {
                Map<String, Object> fm = Json.asMap(fs.get(i));
                if (fm != null) {
                    c.features.add(TerrainFeature.read(fm));
                }
            }
        }
        return c;
    }
}
