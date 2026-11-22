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
package com.codenameone.examples.splat.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/// A paintball field: a fenced rectangle of boxes, two team bases and the snack bar.
///
/// The field runs from `(0, 0)` to `(width, depth)` in millimetres. Team 0 spawns
/// near `z = 0`, team 1 near `z = depth`. Fields are built mirrored by
/// [Builder#mirror] so neither side is better, which the tests check.
public final class Arena {
    /// Millimetres per metre.
    public static final int UNIT = 1000;

    public final String id;
    public final String name;
    public final int width;
    public final int depth;
    private final List<Box> boxes;
    /// Spawn points per team, `{x, z}` pairs.
    private final int[][] spawns;
    /// Flag base per team, `{x, z}`.
    private final int[][] bases;
    /// The snack bar: centre and radius.
    public final int hillX;
    public final int hillZ;
    public final int hillRadius;
    /// Built the first time a bot asks; the same for every match on this arena.
    private final AtomicReference<Nav> nav = new AtomicReference<Nav>();

    Arena(String id, String name, int width, int depth, List<Box> boxes, int[][] spawns,
            int[][] bases, int hillX, int hillZ, int hillRadius) {
        this.id = id;
        this.name = name;
        this.width = width;
        this.depth = depth;
        this.boxes = Collections.unmodifiableList(new ArrayList<Box>(boxes));
        this.spawns = spawns;
        this.bases = bases;
        this.hillX = hillX;
        this.hillZ = hillZ;
        this.hillRadius = hillRadius;
    }

    public List<Box> boxes() {
        return boxes;
    }

    public int spawnCount(int team) {
        return spawns[team].length / 2;
    }

    public int spawnX(int team, int i) {
        return spawns[team][i * 2];
    }

    public int spawnZ(int team, int i) {
        return spawns[team][i * 2 + 1];
    }

    public int baseX(int team) {
        return bases[team][0];
    }

    public int baseZ(int team) {
        return bases[team][1];
    }

    /// The box containing the point, or null.
    public Box boxAt(int x, int y, int z) {
        for (int i = 0; i < boxes.size(); i++) {
            Box b = boxes.get(i);
            if (b.contains(x, y, z)) {
                return b;
            }
        }
        return null;
    }

    public boolean inside(int x, int z) {
        return x >= 0 && x <= width && z >= 0 && z <= depth;
    }

    /// The highest box top under a circle of `radius` at `(x, z)` that is no higher
    /// than `maxTop`, or 0 for the ground.
    public int groundAt(int x, int z, int radius, int maxTop) {
        int g = 0;
        for (int i = 0; i < boxes.size(); i++) {
            Box b = boxes.get(i);
            if (b.height <= maxTop && b.height > g && b.overlaps(x, z, radius)) {
                g = b.height;
            }
        }
        return g;
    }

    Nav nav() {
        Nav n = nav.get();
        if (n == null) {
            nav.compareAndSet(null, new Nav(this));
            n = nav.get();
        }
        return n;
    }

    /// True when a circle at `(x, z)` with feet at `feet` and the given height would
    /// stand inside a box it cannot step onto.
    public boolean blocked(int x, int z, int radius, int feet, int stepUp) {
        if (x - radius < 0 || x + radius > width || z - radius < 0 || z + radius > depth) {
            return true;
        }
        for (int i = 0; i < boxes.size(); i++) {
            Box b = boxes.get(i);
            if (b.height > feet + stepUp && b.overlaps(x, z, radius)) {
                return true;
            }
        }
        return false;
    }

    /// True when nothing solid lies on the segment between two points, sampled every
    /// quarter metre. Bots use it to see; grenades use it so paint does not go
    /// through a bunker.
    public boolean clear(int x1, int y1, int z1, int x2, int y2, int z2) {
        long dx = (long) x2 - x1;
        long dy = (long) y2 - y1;
        long dz = (long) z2 - z1;
        long len = FixedMath.isqrt(dx * dx + dy * dy + dz * dz);
        int steps = (int) (len / 250) + 1;
        for (int s = 1; s < steps; s++) {
            int x = (int) (x1 + dx * s / steps);
            int y = (int) (y1 + dy * s / steps);
            int z = (int) (z1 + dz * s / steps);
            if (boxAt(x, y, z) != null) {
                return false;
            }
        }
        return true;
    }

    public long contentHash() {
        Hash h = new Hash().add(id).add(width).add(depth).add(hillX).add(hillZ).add(hillRadius);
        for (int i = 0; i < boxes.size(); i++) {
            boxes.get(i).hash(h);
        }
        for (int t = 0; t < 2; t++) {
            for (int i = 0; i < spawns[t].length; i++) {
                h.add(spawns[t][i]);
            }
            h.add(bases[t][0]).add(bases[t][1]);
        }
        return h.value();
    }

    /// Builds a field from one half: everything added is mirrored through the
    /// centre point, so team 1's half is team 0's turned around.
    public static final class Builder {
        private final String id;
        private final String name;
        private final int width;
        private final int depth;
        private final List<Box> boxes = new ArrayList<Box>();
        private final List<Integer> spawns = new ArrayList<Integer>();
        private int baseX;
        private int baseZ;
        private int hillRadius = 4 * UNIT;

        public Builder(String id, String name, int widthM, int depthM) {
            this.id = id;
            this.name = name;
            this.width = widthM * UNIT;
            this.depth = depthM * UNIT;
        }

        /// A box given by centre and size in decimetres, mirrored for the other side.
        public Builder mirror(String kind, int cxDm, int czDm, int wDm, int dDm, int hDm) {
            int cx = cxDm * 100;
            int cz = czDm * 100;
            int hw = wDm * 50;
            int hd = dDm * 50;
            boxes.add(new Box(kind, cx - hw, cz - hd, cx + hw, cz + hd, hDm * 100));
            int mx = width - cx;
            int mz = depth - cz;
            if (mx != cx || mz != cz) {
                boxes.add(new Box(kind, mx - hw, mz - hd, mx + hw, mz + hd, hDm * 100));
            }
            return this;
        }

        /// A spawn point for team 0, in decimetres.
        public Builder spawn(int xDm, int zDm) {
            spawns.add(Integer.valueOf(xDm * 100));
            spawns.add(Integer.valueOf(zDm * 100));
            return this;
        }

        public Builder base(int xDm, int zDm) {
            baseX = xDm * 100;
            baseZ = zDm * 100;
            return this;
        }

        public Builder hill(int radiusDm) {
            hillRadius = radiusDm * 100;
            return this;
        }

        public Arena build() {
            int n = spawns.size();
            int[][] sp = new int[2][n];
            for (int i = 0; i < n; i += 2) {
                int x = spawns.get(i).intValue();
                int z = spawns.get(i + 1).intValue();
                sp[0][i] = x;
                sp[0][i + 1] = z;
                sp[1][i] = width - x;
                sp[1][i + 1] = depth - z;
            }
            int[][] b = {{baseX, baseZ}, {width - baseX, depth - baseZ}};
            return new Arena(id, name, width, depth, boxes, sp, b, width / 2, depth / 2, hillRadius);
        }
    }
}
