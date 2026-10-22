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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// The battlefield's geometry, in milli-tiles.
///
/// Player 0 defends the bottom half and player 1 the top; a river crosses the
/// middle and a bridge carries each lane over it. An arena is data: `CLASSIC` is
/// the field every ranked battle is played on, and the Forge builds others --
/// one to three lanes, a wider or narrower river, rocks, and towers anywhere on
/// either side. An arena never changes once a match starts.
public final class Arena {
    public static final int TILE = 1000;

    /// The limits the editor and the server's validator hold every arena to.
    public static final int MIN_TILES_WIDE = 10;
    public static final int MAX_TILES_WIDE = 24;
    public static final int MIN_TILES_TALL = 16;
    public static final int MAX_TILES_TALL = 40;
    public static final int MAX_LANES = 3;
    public static final int MAX_TOWERS = 12;
    public static final int MAX_OBSTACLES = 40;

    public static final Arena CLASSIC = classic();

    final String id;
    final String name;
    final String theme;
    final int width;
    final int height;
    final int riverTop;
    final int riverBottom;
    final int[] lanes;
    final int bridgeHalfWidth;
    final List<Tower> towers;
    final List<Obstacle> obstacles;

    /// A tower standing on the field when the match starts.
    public static final class Tower {
        public final int owner;
        public final String card;
        public final int x;
        public final int y;
        /// Losing the king loses the match; it wakes when hit or when a tower falls.
        public final boolean king;

        public Tower(int owner, String card, int x, int y, boolean king) {
            this.owner = owner;
            this.card = card;
            this.x = x;
            this.y = y;
            this.king = king;
        }
    }

    /// A rock, tree or wall: troops walk around it and nothing can hit it.
    public static final class Obstacle {
        public final String kind;
        public final int x;
        public final int y;
        public final int radius;

        public Obstacle(String kind, int x, int y, int radius) {
            this.kind = kind;
            this.x = x;
            this.y = y;
            this.radius = radius;
        }
    }

    public Arena(String id, String name, String theme, int width, int height, int riverTop, int riverBottom,
            int[] lanes, int bridgeHalfWidth, List<Tower> towers, List<Obstacle> obstacles) {
        this.id = id;
        this.name = name;
        this.theme = theme == null ? "meadow" : theme;
        this.width = width;
        this.height = height;
        this.riverTop = riverTop;
        this.riverBottom = riverBottom;
        this.lanes = copy(lanes);
        this.bridgeHalfWidth = bridgeHalfWidth;
        this.towers = Collections.unmodifiableList(new ArrayList<Tower>(towers));
        this.obstacles = Collections.unmodifiableList(new ArrayList<Obstacle>(obstacles));
    }

    private static int[] copy(int[] a) {
        int[] out = new int[a.length];
        System.arraycopy(a, 0, out, 0, a.length);
        return out;
    }

    private static Arena classic() {
        int w = 18 * TILE;
        int h = 32 * TILE;
        int left = 3 * TILE + TILE / 2;
        int right = 14 * TILE + TILE / 2;
        List<Tower> towers = new ArrayList<Tower>();
        mirrored(towers, h, Cards.KING_TOWER, 9 * TILE, 29 * TILE, true);
        mirrored(towers, h, Cards.PRINCESS_TOWER, left, 26 * TILE, false);
        mirrored(towers, h, Cards.PRINCESS_TOWER, right, 26 * TILE, false);
        return new Arena("classic", "Meadow", "meadow", w, h, 15 * TILE, 17 * TILE,
                new int[] {left, right}, TILE, sortByOwner(towers), new ArrayList<Obstacle>());
    }

    /// Adds a tower for player 0 at (x, y) and its mirror image for player 1.
    public static void mirrored(List<Tower> into, int height, String card, int x, int y, boolean king) {
        into.add(new Tower(0, card, x, y, king));
        into.add(new Tower(1, card, x, height - y, king));
    }

    /// Player 0's towers first, then player 1's, each in the order given. Unit ids
    /// are handed out in this order, so it is part of the simulation.
    public static List<Tower> sortByOwner(List<Tower> towers) {
        List<Tower> out = new ArrayList<Tower>();
        for (int p = 0; p < 2; p++) {
            for (int i = 0; i < towers.size(); i++) {
                if (towers.get(i).owner == p) {
                    out.add(towers.get(i));
                }
            }
        }
        return out;
    }

    public String id() { return id; }
    public String name() { return name; }
    public String theme() { return theme; }
    public int width() { return width; }
    public int height() { return height; }
    public int riverTop() { return riverTop; }
    public int riverBottom() { return riverBottom; }
    public int riverMid() { return (riverTop + riverBottom) / 2; }
    public int laneCount() { return lanes.length; }
    public int bridgeHalfWidth() { return bridgeHalfWidth; }
    public List<Tower> towers() { return towers; }
    public List<Obstacle> obstacles() { return obstacles; }

    /// `owner`'s king tower.
    public Tower king(int owner) {
        for (int i = 0; i < towers.size(); i++) {
            Tower t = towers.get(i);
            if (t.owner == owner && t.king) {
                return t;
            }
        }
        return null;
    }

    /// The y of player 0's first tower that is not the king: on the classic field,
    /// the row both archer towers stand on.
    public int princessY() {
        for (int i = 0; i < towers.size(); i++) {
            Tower t = towers.get(i);
            if (t.owner == 0 && !t.king) {
                return t.y;
            }
        }
        return king(0).y;
    }

    /// Mirrors a y coordinate onto the other half.
    public int mirrorY(int y) {
        return height - y;
    }

    public int laneX(int lane) {
        return lanes[Math.max(0, Math.min(lanes.length - 1, lane))];
    }

    /// The lane whose bridge is nearest `x`; lanes are numbered from the left.
    public int laneOf(int x) {
        int best = 0;
        int bestD = Integer.MAX_VALUE;
        for (int i = 0; i < lanes.length; i++) {
            int d = Math.abs(x - lanes[i]);
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        return best;
    }

    public boolean onBridge(int x) {
        for (int i = 0; i < lanes.length; i++) {
            if (Math.abs(x - lanes[i]) <= bridgeHalfWidth) {
                return true;
            }
        }
        return false;
    }

    /// True when `y` lies in `player`'s own half (river excluded).
    public boolean ownHalf(int player, int y) {
        return player == 0 ? y >= riverBottom : y <= riverTop;
    }

    /// True when a unit at `y` has crossed the river into the enemy half.
    public boolean crossed(int player, int y) {
        return player == 0 ? y <= riverTop : y >= riverBottom;
    }

    /// True when (x, y) is inside an obstacle; nothing may be dropped there.
    public boolean blocked(int x, int y) {
        for (int i = 0; i < obstacles.size(); i++) {
            Obstacle o = obstacles.get(i);
            if (FixedMath.within(x, y, o.x, o.y, o.radius)) {
                return true;
            }
        }
        return false;
    }

    /// Folds the geometry into a match's state hash, so two copies of a match on
    /// different arenas can never agree by accident.
    long hash(long h) {
        long v = Match.mixValue(h, width);
        v = Match.mixValue(v, height);
        v = Match.mixValue(v, riverTop);
        v = Match.mixValue(v, riverBottom);
        v = Match.mixValue(v, bridgeHalfWidth);
        for (int i = 0; i < lanes.length; i++) {
            v = Match.mixValue(v, lanes[i]);
        }
        for (int i = 0; i < obstacles.size(); i++) {
            Obstacle o = obstacles.get(i);
            v = Match.mixValue(v, o.x);
            v = Match.mixValue(v, o.y);
            v = Match.mixValue(v, o.radius);
        }
        return v;
    }
}
