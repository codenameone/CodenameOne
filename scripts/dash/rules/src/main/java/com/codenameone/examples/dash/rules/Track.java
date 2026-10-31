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

/// The track one run is played on: the level's hand-placed entries, then the
/// endless section, generated ahead of the leading runner from the run's seed.
/// Every runner in a race shares it, so "same seed" means "same track".
///
/// Entries are only ever appended, in distance order, which lets each runner keep
/// a cursor instead of searching.
public final class Track {
    /// Millimetres per metre.
    public static final int UNIT = 1000;
    /// How far ahead of the leader the track is generated.
    public static final int HORIZON = 120 * UNIT;

    /// One placement with its definition resolved and its lane mirrored.
    public static final class Entry {
        public final int index;
        public final int z;
        public final int lane;
        public final int depth;
        public final ObstacleDef obstacle;
        public final PickupDef pickup;

        Entry(int index, int z, int lane, ObstacleDef obstacle, PickupDef pickup) {
            this.index = index;
            this.z = z;
            this.lane = lane;
            this.obstacle = obstacle;
            this.pickup = pickup;
            this.depth = obstacle != null ? obstacle.depth : 300;
        }

        public boolean spansAllLanes() {
            return lane == Placement.ALL_LANES;
        }
    }

    private final LevelDef level;
    private final Catalog catalog;
    private final Rng rng;
    private final List<Entry> entries = new ArrayList<Entry>();
    private int genZ;
    private final boolean procedural;

    Track(LevelDef level, Catalog catalog, long seed) {
        this.level = level;
        this.catalog = catalog;
        this.rng = new Rng(seed ^ 0x5DA57E5ADL);
        List<Placement> fixed = new ArrayList<Placement>(level.placements);
        // A stable insertion sort: the editor may hand placements in any order,
        // and two at the same distance keep the order the author gave them.
        for (int i = 1; i < fixed.size(); i++) {
            Placement p = fixed.get(i);
            int j = i;
            while (j > 0 && fixed.get(j - 1).z > p.z) {
                fixed.set(j, fixed.get(j - 1));
                j--;
            }
            fixed.set(j, p);
        }
        int end = 0;
        for (int i = 0; i < fixed.size(); i++) {
            Placement p = fixed.get(i);
            Entry e = resolve(p.z, p.lane, p.defId);
            if (e != null) {
                entries.add(e);
                end = Math.max(end, e.z + e.depth);
            }
        }
        genZ = Math.max(level.proceduralFrom, end + 4 * UNIT);
        procedural = !level.chunks.isEmpty();
    }

    private Entry resolve(int z, int lane, String id) {
        ObstacleDef o = catalog.obstacle(id);
        PickupDef p = o == null ? catalog.pickup(id) : null;
        if (o == null && p == null) {
            // Unknown ids are the validator's to report; the run skips them so a
            // level edited against a newer catalog still loads.
            return null;
        }
        return new Entry(entries.size(), z, lane, o, p);
    }

    public int difficultyAt(int z) {
        int step = level.difficultyStep < UNIT ? UNIT : level.difficultyStep;
        int d = z / step;
        return d > 10 ? 10 : d;
    }

    /// Generates the endless section up to `z`. Called by the run before every
    /// tick with the leader's distance plus [#HORIZON].
    void ensure(int z) {
        if (!procedural) {
            return;
        }
        int limit = level.goal == Goal.DISTANCE && level.length > 0 ? level.length : Integer.MAX_VALUE;
        while (genZ < z && genZ < limit) {
            int diff = difficultyAt(genZ);
            int eligible = 0;
            for (int i = 0; i < level.chunks.size(); i++) {
                if (level.chunks.get(i).minDifficulty <= diff) {
                    eligible++;
                }
            }
            if (eligible == 0) {
                // Nothing easy enough yet: leave an empty stretch and look again.
                genZ += 20 * UNIT;
                continue;
            }
            int pick = rng.nextInt(eligible);
            Chunk chunk = null;
            for (int i = 0; i < level.chunks.size(); i++) {
                if (level.chunks.get(i).minDifficulty <= diff && pick-- == 0) {
                    chunk = level.chunks.get(i);
                    break;
                }
            }
            boolean mirror = level.lanes > 1 && rng.nextInt(2) == 1;
            List<Placement> ps = chunk.placements();
            for (int i = 0; i < ps.size(); i++) {
                Placement p = ps.get(i);
                int lane = p.lane;
                if (mirror && lane != Placement.ALL_LANES) {
                    lane = level.lanes - 1 - lane;
                }
                Entry e = resolve(genZ + p.z, lane, p.defId);
                if (e != null) {
                    entries.add(e);
                }
            }
            genZ += chunk.length;
        }
    }

    public int size() {
        return entries.size();
    }

    public Entry get(int i) {
        return entries.get(i);
    }

    public LevelDef level() {
        return level;
    }

    /// The camera mode in force at `z`, and how far through its blend the runner
    /// is, from 0 to 1000. For the renderer; the simulation never asks.
    public ViewMode viewAt(int z) {
        ViewMode m = ViewMode.CHASE_3D;
        int best = -1;
        for (int i = 0; i < level.cues.size(); i++) {
            ViewCue c = level.cues.get(i);
            if (c.at <= z && c.at >= best) {
                best = c.at;
                m = c.mode;
            }
        }
        return m;
    }

    /// The mode in force before the cue that `z` is blending into, for the
    /// renderer to interpolate from.
    public ViewMode previousViewAt(int z) {
        ViewCue cur = cueAt(z);
        if (cur == null) {
            return ViewMode.CHASE_3D;
        }
        return cur.at == 0 ? cur.mode : viewAt(cur.at - 1);
    }

    /// 0..1000: how far the camera has blended into the current cue's mode.
    public int blendAt(int z) {
        ViewCue c = cueAt(z);
        if (c == null || c.blendMillis == 0) {
            return 1000;
        }
        int into = z - c.at;
        return into >= c.blendMillis ? 1000 : FixedMath.muldiv(into, 1000, c.blendMillis);
    }

    private ViewCue cueAt(int z) {
        ViewCue found = null;
        for (int i = 0; i < level.cues.size(); i++) {
            ViewCue c = level.cues.get(i);
            if (c.at <= z && (found == null || c.at >= found.at)) {
                found = c;
            }
        }
        return found;
    }

    long hashInto(Hash h) {
        h.add(entries.size()).add(genZ).add(rng.state());
        return h.value();
    }
}
