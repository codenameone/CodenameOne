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
package com.codenameone.examples.skirmish.rules;

/// The battlefield's geometry, in milli-tiles.
///
/// The arena is 18 tiles wide and 32 tall. Player 0 defends the bottom half and
/// player 1 the top; a river crosses the middle and two bridges carry the lanes.
/// Everything here is a constant so that a fork can reshape the field in one place.
public final class Arena {
    public static final int TILE = 1000;
    public static final int WIDTH = 18 * TILE;
    public static final int HEIGHT = 32 * TILE;

    public static final int RIVER_TOP = 15 * TILE;
    public static final int RIVER_BOTTOM = 17 * TILE;
    public static final int RIVER_MID = 16 * TILE;

    public static final int LEFT_LANE_X = 3 * TILE + TILE / 2;
    public static final int RIGHT_LANE_X = 14 * TILE + TILE / 2;
    public static final int BRIDGE_HALF_WIDTH = TILE;

    public static final int KING_X = 9 * TILE;
    public static final int KING_Y_BOTTOM = 29 * TILE;
    public static final int PRINCESS_Y_BOTTOM = 26 * TILE;

    private Arena() {
    }

    /// Mirrors a y coordinate onto the other half, so player 1's positions are
    /// computed from player 0's.
    public static int mirrorY(int y) {
        return HEIGHT - y;
    }

    public static int laneX(int lane) {
        return lane == 0 ? LEFT_LANE_X : RIGHT_LANE_X;
    }

    /// Lane 0 is the left one, lane 1 the right.
    public static int laneOf(int x) {
        return x < WIDTH / 2 ? 0 : 1;
    }

    public static boolean onBridge(int x) {
        return Math.abs(x - LEFT_LANE_X) <= BRIDGE_HALF_WIDTH
                || Math.abs(x - RIGHT_LANE_X) <= BRIDGE_HALF_WIDTH;
    }

    /// True when `y` lies in `player`'s own half (river excluded).
    public static boolean ownHalf(int player, int y) {
        return player == 0 ? y >= RIVER_BOTTOM : y <= RIVER_TOP;
    }

    /// True when a unit at `y` has crossed the river into the enemy half.
    public static boolean crossed(int player, int y) {
        return player == 0 ? y <= RIVER_TOP : y >= RIVER_BOTTOM;
    }
}
