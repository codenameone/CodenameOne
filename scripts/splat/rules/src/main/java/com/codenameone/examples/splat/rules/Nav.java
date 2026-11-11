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

import java.util.Hashtable;

/// Paths for bots: the arena as a grid of half-metre cells, and for each goal
/// a breadth-first distance to it from every cell, built once and kept. A bot
/// walks downhill on that field toward the furthest point it can reach in a
/// straight line, so it goes round cover rather than into it, and cuts
/// corners the way a person does rather than zig-zagging cell to cell.
///
/// Everything is integer and built in the same order everywhere, so every
/// machine that runs a match finds the same path.
final class Nav {
    static final int CELL = 500;
    /// Cells are free with a little room to spare, so a path never hugs a wall.
    private static final int CLEARANCE = Match.RADIUS + 150;
    private static final int LOOKAHEAD = 10;
    private static final int[] DX = {1, -1, 0, 0, 1, 1, -1, -1};
    private static final int[] DZ = {0, 0, 1, -1, 1, -1, 1, -1};

    private final Arena arena;
    private final int cols;
    private final int rows;
    private final boolean[] free;
    private final Hashtable<Integer, int[]> fields = new Hashtable<Integer, int[]>();

    Nav(Arena arena) {
        this.arena = arena;
        cols = Math.max(1, arena.width / CELL);
        rows = Math.max(1, arena.depth / CELL);
        free = new boolean[cols * rows];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                free[r * cols + c] = !arena.blocked(c * CELL + CELL / 2, r * CELL + CELL / 2, CLEARANCE, 0,
                        Match.STEP_UP);
            }
        }
    }

    private int cell(int x, int z) {
        int c = FixedMath.clamp(x / CELL, 0, cols - 1);
        int r = FixedMath.clamp(z / CELL, 0, rows - 1);
        return r * cols + c;
    }

    /// The free cell nearest a point, so a goal inside cover still has a field.
    private int nearestFree(int at) {
        if (free[at]) {
            return at;
        }
        int c0 = at % cols;
        int r0 = at / cols;
        for (int ring = 1; ring < Math.max(cols, rows); ring++) {
            for (int r = r0 - ring; r <= r0 + ring; r++) {
                for (int c = c0 - ring; c <= c0 + ring; c++) {
                    if (r >= 0 && r < rows && c >= 0 && c < cols && free[r * cols + c]) {
                        return r * cols + c;
                    }
                }
            }
        }
        return at;
    }

    private int[] field(int goal) {
        Integer key = Integer.valueOf(goal);
        int[] d = fields.get(key);
        if (d != null) {
            return d;
        }
        d = new int[cols * rows];
        for (int i = 0; i < d.length; i++) {
            d[i] = Integer.MAX_VALUE;
        }
        int[] queue = new int[cols * rows];
        int head = 0;
        int tail = 0;
        d[goal] = 0;
        queue[tail++] = goal;
        while (head < tail) {
            int at = queue[head++];
            int c = at % cols;
            int r = at / cols;
            for (int k = 0; k < 4; k++) {
                int nc = c + DX[k];
                int nr = r + DZ[k];
                if (nc >= 0 && nc < cols && nr >= 0 && nr < rows) {
                    int n = nr * cols + nc;
                    if (free[n] && d[n] == Integer.MAX_VALUE) {
                        d[n] = d[at] + 1;
                        queue[tail++] = n;
                    }
                }
            }
        }
        fields.put(key, d);
        return d;
    }

    /// Where to walk next on the way from `(x, z)` to `(gx, gz)`: a point in
    /// plain sight along the path, or the goal itself when it is.
    int[] next(int x, int z, int gx, int gz) {
        if (walk(x, z, gx, gz)) {
            return new int[] {gx, gz};
        }
        int[] d = field(nearestFree(cell(gx, gz)));
        int at = cell(x, z);
        if (d[at] == Integer.MAX_VALUE) {
            at = nearestFree(at);
        }
        int best = at;
        int[] target = {gx, gz};
        for (int step = 0; step < LOOKAHEAD && d[at] > 0 && d[at] != Integer.MAX_VALUE; step++) {
            int next = at;
            int c = at % cols;
            int r = at / cols;
            for (int k = 0; k < DX.length; k++) {
                int nc = c + DX[k];
                int nr = r + DZ[k];
                if (nc >= 0 && nc < cols && nr >= 0 && nr < rows) {
                    int n = nr * cols + nc;
                    // a diagonal only when both sides of the corner are open
                    if (d[n] < d[next] && (k < 4 || free[r * cols + nc] && free[nr * cols + c])) {
                        next = n;
                    }
                }
            }
            if (next == at) {
                break;
            }
            at = next;
            int px = (at % cols) * CELL + CELL / 2;
            int pz = (at / cols) * CELL + CELL / 2;
            if (step == 0 || walk(x, z, px, pz)) {
                best = at;
                target = new int[] {px, pz};
            } else {
                break;
            }
        }
        return best == cell(x, z) && d[best] == 0 ? new int[] {gx, gz} : target;
    }

    /// Whether a player can walk straight from one point to another, sampled
    /// every quarter metre.
    boolean walk(int x1, int z1, int x2, int z2) {
        long dx = (long) x2 - x1;
        long dz = (long) z2 - z1;
        int n = (int) (FixedMath.isqrt(dx * dx + dz * dz) / 250) + 1;
        for (int i = 1; i <= n; i++) {
            int x = (int) (x1 + dx * i / n);
            int z = (int) (z1 + dz * i / n);
            if (arena.blocked(x, z, Match.RADIUS + 50, 0, Match.STEP_UP)) {
                return false;
            }
        }
        return true;
    }
}
