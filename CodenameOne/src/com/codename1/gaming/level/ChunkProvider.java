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

/// Supplies and persists `TerrainChunk`s for a `StreamingTerrain`. Implementations decide where
/// chunks come from: procedurally generated, bundled resources, `Storage`, or the network. A
/// provider must return a (possibly empty) chunk for any coordinate so the world is seamless.
public interface ChunkProvider {
    /// Loads (or generates) the chunk at the given chunk coordinates. Never returns null.
    TerrainChunk loadChunk(int cx, int cz);

    /// Persists a dirty chunk being evicted from memory. No-op for read-only/procedural sources.
    void saveChunk(TerrainChunk chunk);
}
