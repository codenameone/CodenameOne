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

/// One runner's state. Plain fields, mutated only by [Run#step]; the renderer
/// reads them and interpolates between ticks.
public final class Runner {
    /// Milli-lanes moved per tick while changing lane: eight ticks per lane.
    public static final int LANE_SPEED = 125;
    /// Upward speed at take-off, millimetres per tick.
    public static final int JUMP_VELOCITY = 190;
    /// Millimetres per tick lost every tick while airborne.
    public static final int GRAVITY = 9;
    public static final int SLIDE_TICKS = 36;
    /// Two stumbles this close together, in ticks, end the run.
    public static final int STUMBLE_WINDOW = 300;
    /// Half the runner's length along the track, for overlap tests.
    public static final int HALF_DEPTH = 250;

    public final int index;
    public int lane;
    /// Lateral position in milli-lanes: lane 1 is 1000.
    public int laneX;
    public int y;
    public int vy;
    public int slideTicks;
    public int z;
    /// Millimetres per second.
    public int speed;
    int speedCarry;
    int accelCarry;
    public int coins;
    public int points;
    public int invulnerable;
    public int shield;
    public int magnet;
    public int boost;
    public int stumbles;
    int lastStumble = -STUMBLE_WINDOW - 1;
    public boolean alive = true;
    public boolean finished;
    /// The tick the runner fell or finished; -1 while running.
    public int endTick = -1;
    int cursor;
    private int[] taken = new int[8];

    Runner(int index, int lane, int speed) {
        this.index = index;
        this.lane = lane;
        this.laneX = lane * 1000;
        this.speed = speed;
    }

    public boolean running() {
        return alive && !finished;
    }

    public boolean airborne() {
        return y > 0;
    }

    public boolean sliding() {
        return slideTicks > 0 && y == 0;
    }

    /// Distance in whole metres plus pickup points.
    public int score() {
        return z / Track.UNIT + points;
    }

    boolean took(int entry) {
        int w = entry >>> 5;
        return w < taken.length && (taken[w] & (1 << (entry & 31))) != 0;
    }

    void take(int entry) {
        int w = entry >>> 5;
        if (w >= taken.length) {
            int[] n = new int[Math.max(taken.length * 2, w + 1)];
            System.arraycopy(taken, 0, n, 0, taken.length);
            taken = n;
        }
        taken[w] |= 1 << (entry & 31);
    }

    void hash(Hash h) {
        h.add(lane).add(laneX).add(y).add(vy).add(slideTicks).add(z).add(speed).add(speedCarry).add(accelCarry)
                .add(coins).add(points).add(invulnerable).add(shield).add(magnet).add(boost).add(stumbles)
                .add(lastStumble).add(alive).add(finished).add(endTick).add(cursor);
    }
}
