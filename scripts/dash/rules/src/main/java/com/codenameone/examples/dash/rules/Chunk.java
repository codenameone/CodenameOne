/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.dash.rules;

import java.util.ArrayList;
import java.util.List;

/// A stretch of track the endless part of a level is assembled from. The track
/// picks chunks at random among those whose `minDifficulty` the current distance
/// has reached, and may mirror one across the lanes.
///
/// Placements are relative to the chunk's start and kept sorted by distance, so
/// the track can append them as they are.
public final class Chunk {
    public final String id;
    public final int length;
    public final int minDifficulty;
    private final List<Placement> placements = new ArrayList<Placement>();

    public Chunk(String id, int length, int minDifficulty) {
        this.id = id;
        this.length = length < Track.UNIT ? Track.UNIT : length;
        this.minDifficulty = minDifficulty;
    }

    public Chunk add(int z, String defId, int lane) {
        int i = placements.size();
        while (i > 0 && placements.get(i - 1).z > z) {
            i--;
        }
        placements.add(i, new Placement(z, defId, lane));
        return this;
    }

    public List<Placement> placements() {
        return placements;
    }
}
