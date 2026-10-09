/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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

/// Arrays of more than one dimension allocated in one expression, which javac
/// compiles to a single `MULTIANEWARRAY` that pops one size per dimension it
/// allocates. Every dimension here has a different size, because an emitter that
/// reads all of them from one operand still answers correctly for a square array.
public class JsMultiArrayDimensionsApp {
    public static int result;

    String[][] names;
    long[][][] cube;

    // The sizes are computed, so each one is a separate operand on the stack, and
    // the store into a field puts `this` beneath them: popping too few operands
    // leaves a size where the receiver is expected. The guard keeps the method off
    // the straight line emitter.
    int build(int a, int b, int c) {
        if (a < 0) {
            return -1;
        }
        this.names = new String[a][b];
        this.cube = new long[a + 1][b + 2][c];
        int[][] ragged = new int[c][];
        Object[][][] partial = new Object[b][a][];
        int mask = 0;
        if (names.length == a && names[0].length == b && names[a - 1].length == b) {
            mask |= 1;
        }
        if (cube.length == a + 1 && cube[0].length == b + 2 && cube[a][b + 1].length == c) {
            mask |= 2;
        }
        if (ragged.length == c && ragged[0] == null && ragged[c - 1] == null) {
            mask |= 4;
        }
        if (partial.length == b && partial[0].length == a && partial[b - 1][a - 1] == null) {
            mask |= 8;
        }
        // Every element is addressable and independent of its neighbours.
        names[a - 1][b - 1] = "x";
        cube[a][b + 1][c - 1] = 0x100000007L;
        ragged[c - 1] = new int[] {4, 5};
        if ("x".equals(names[a - 1][b - 1]) && names[0][0] == null
                && cube[a][b + 1][c - 1] == 0x100000007L && cube[0][0][0] == 0L
                && ragged[c - 1][1] == 5) {
            mask |= 16;
        }
        return mask;
    }

    // As an operand of a call, with a value beneath the sizes that is not `this`.
    static int lengths(int seed, char[][] grid) {
        return seed + grid.length * 10 + grid[0].length;
    }

    static int nested(int rows, int cols) {
        if (rows < 0) {
            return -1;
        }
        return lengths(100, new char[rows][cols]);
    }

    public static void main(String[] args) {
        JsMultiArrayDimensionsApp app = new JsMultiArrayDimensionsApp();
        int mask = app.build(2, 3, 5);
        if (app.names.length == 2 && app.names[1].length == 3
                && app.cube.length == 3 && app.cube[2].length == 5 && app.cube[2][4].length == 5) {
            mask |= 32;
        }
        if (nested(4, 7) == 147) {
            mask |= 64;
        }
        result = mask;
    }
}
