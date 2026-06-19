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

import java.util.List;

/// Abstraction over a heightfield + per-cell material + placed `TerrainFeature`s addressed by
/// world cell coordinates. The legacy `TerrainGrid` is a small fixed implementation; for
/// large/infinite worlds use `StreamingTerrain`, which pages `TerrainChunk`s in and out through
/// a `ChunkProvider`. All coordinates are integer tile/cell coordinates (which may be negative
/// for an unbounded terrain). Heights equal to `#NO_GROUND` mean "no floor here" (a hole/gap).
public interface Terrain {
    /// Sentinel height meaning "no ground in this cell".
    float NO_GROUND = -1024f;

    float getHeight(int x, int z);

    void setHeight(int x, int z, float height);

    boolean hasGround(int x, int z);

    /// Material id at the cell (resolve via `MaterialRegistry`).
    String getMaterial(int x, int z);

    void setMaterial(int x, int z, String materialId);

    /// Features whose anchor cell is currently loaded.
    List<TerrainFeature> features();

    /// Adds a feature, routing it to the chunk that owns its position.
    void addFeature(TerrainFeature feature);

    /// True for a fixed-size terrain (`#getCols()`/`#getRows()` valid), false if unbounded.
    boolean isBounded();

    /// Column count for a bounded terrain, or -1 if unbounded.
    int getCols();

    /// Row count for a bounded terrain, or -1 if unbounded.
    int getRows();
}
