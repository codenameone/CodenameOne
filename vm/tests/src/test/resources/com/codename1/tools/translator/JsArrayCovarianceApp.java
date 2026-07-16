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

public class JsArrayCovarianceApp {
    public static int result;

    static class Animal {
    }

    static class Dog extends Animal {
    }

    public static void main(String[] args) {
        Dog[] dogs = new Dog[1];
        Dog[][] dogGrid = new Dog[1][];
        dogGrid[0] = new Dog[1];
        int score = 0;

        if (dogs instanceof Dog[]) {
            score |= 1;
        }
        if (dogs instanceof Animal[]) {
            score |= 2;
        }
        if (dogs instanceof Object[]) {
            score |= 4;
        }
        if (dogGrid instanceof Dog[][]) {
            score |= 8;
        }
        if (dogGrid instanceof Animal[][]) {
            score |= 16;
        }
        if (dogGrid instanceof Object[]) {
            score |= 32;
        }
        if (dogGrid[0] instanceof Animal[]) {
            score |= 64;
        }
        if (((Animal[]) dogs).length == 1) {
            score |= 128;
        }
        if (((Object[]) dogGrid).length == 1) {
            score |= 256;
        }

        // Multi-dimensional PRIMITIVE arrays are object arrays too: int[][] is an
        // Object[] whose components are int[]. List.toArray(new int[n][]) compiles
        // to exactly this checkcast (the editor's bidiRuns relies on it), so a
        // runtime that only tracks declared classes must resolve it structurally.
        int[][] grid = new int[2][];
        grid[0] = new int[]{7};
        grid[1] = new int[]{8, 9};
        if (grid instanceof Object[]) {
            score |= 512;
        }
        if (((Object[]) grid).length == 2) {
            score |= 1024;
        }
        java.util.List<int[]> rows = new java.util.ArrayList<int[]>();
        rows.add(new int[]{7});
        int[][] out = rows.toArray(new int[rows.size()][]);
        if (out.length == 1 && out[0][0] == 7) {
            score |= 2048;
        }
        result = score;
    }
}
