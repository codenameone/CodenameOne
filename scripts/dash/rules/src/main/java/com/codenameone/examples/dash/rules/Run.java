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

/// One run of a level by one or more runners on the same seed -- a solo run,
/// a ghost race or a live race. [#step] is the only thing that changes it.
///
/// Runners never touch each other: a race is several independent runs on one
/// shared track, which is what lets the server re-simulate every runner of a
/// live race from their inputs alone.
public final class Run {
    public static final int TICKS_PER_SECOND = 60;
    /// No run lasts longer than this, whatever its goal: thirty minutes.
    public static final int MAX_TICKS = 30 * 60 * TICKS_PER_SECOND;

    private final LevelDef level;
    private final Track track;
    private final Runner[] runners;
    private final long seed;
    private int tick;

    public Run(LevelDef level, Catalog catalog, long seed, int runnerCount) {
        if (runnerCount < 1) {
            throw new IllegalArgumentException("runners " + runnerCount);
        }
        this.level = level;
        this.seed = seed;
        this.track = new Track(level, catalog, seed);
        this.runners = new Runner[runnerCount];
        int lanes = level.lanes < 1 ? 1 : level.lanes;
        for (int i = 0; i < runnerCount; i++) {
            runners[i] = new Runner(i, lanes / 2, level.startSpeed);
        }
        track.ensure(Track.HORIZON);
    }

    public int tick() {
        return tick;
    }

    public long seed() {
        return seed;
    }

    public LevelDef level() {
        return level;
    }

    public Track track() {
        return track;
    }

    public int runnerCount() {
        return runners.length;
    }

    public Runner runner(int i) {
        return runners[i];
    }

    public boolean over() {
        for (int i = 0; i < runners.length; i++) {
            if (runners[i].running()) {
                return false;
            }
        }
        return true;
    }

    /// Advances one tick. `inputs[i]` is runner `i`'s [Input] bits for this tick;
    /// a shorter array, or null, means no input.
    public void step(int[] inputs) {
        if (over()) {
            return;
        }
        int lead = 0;
        for (int i = 0; i < runners.length; i++) {
            Runner r = runners[i];
            if (r.running()) {
                int bits = inputs != null && i < inputs.length ? inputs[i] & Input.ALL : 0;
                advance(r, bits);
            }
            lead = Math.max(lead, r.z);
        }
        track.ensure(lead + Track.HORIZON);
        tick++;
        if (tick >= MAX_TICKS) {
            for (int i = 0; i < runners.length; i++) {
                end(runners[i], runners[i].alive);
            }
        }
    }

    private void advance(Runner r, int bits) {
        int lanes = level.lanes < 1 ? 1 : level.lanes;
        if ((bits & Input.LEFT) != 0 && r.lane > 0) {
            r.lane--;
        }
        if ((bits & Input.RIGHT) != 0 && r.lane < lanes - 1) {
            r.lane++;
        }
        if ((bits & Input.JUMP) != 0 && r.y == 0) {
            r.vy = Runner.JUMP_VELOCITY;
            r.slideTicks = 0;
        }
        if ((bits & Input.SLIDE) != 0) {
            if (r.y > 0) {
                // A slide in the air is a dive: come straight down, then slide.
                r.vy = -Runner.JUMP_VELOCITY;
            }
            r.slideTicks = Runner.SLIDE_TICKS;
        }

        int target = r.lane * 1000;
        if (r.laneX < target) {
            r.laneX = Math.min(target, r.laneX + Runner.LANE_SPEED);
        } else if (r.laneX > target) {
            r.laneX = Math.max(target, r.laneX - Runner.LANE_SPEED);
        }
        if (r.y > 0 || r.vy > 0) {
            r.y += r.vy;
            r.vy -= Runner.GRAVITY;
            if (r.y <= 0) {
                r.y = 0;
                r.vy = 0;
            }
        }
        if (r.slideTicks > 0 && r.y == 0) {
            r.slideTicks--;
        }

        r.accelCarry += level.accel;
        r.speed += r.accelCarry / TICKS_PER_SECOND;
        r.accelCarry %= TICKS_PER_SECOND;
        if (r.speed > level.maxSpeed) {
            r.speed = level.maxSpeed;
        }
        int speed = r.boost > 0 ? r.speed + r.speed / 2 : r.speed;
        r.speedCarry += speed;
        r.z += r.speedCarry / TICKS_PER_SECOND;
        r.speedCarry %= TICKS_PER_SECOND;

        if (r.invulnerable > 0) {
            r.invulnerable--;
        }
        if (r.shield > 0) {
            r.shield--;
        }
        if (r.magnet > 0) {
            r.magnet--;
        }
        if (r.boost > 0) {
            r.boost--;
        }

        collide(r);
        if (!r.alive) {
            return;
        }
        boolean done;
        switch (level.goal) {
            case DISTANCE:
                done = level.length > 0 && r.z >= level.length;
                break;
            case COINS:
                done = r.coins >= level.goalValue;
                break;
            case SURVIVE:
                done = tick + 1 >= level.goalValue;
                break;
            default:
                done = false;
                break;
        }
        if (done) {
            end(r, true);
        }
    }

    private void collide(Runner r) {
        while (r.cursor < track.size()) {
            Track.Entry e = track.get(r.cursor);
            if (e.z + e.depth + 4 * Track.UNIT < r.z) {
                r.cursor++;
            } else {
                break;
            }
        }
        int reach = r.magnet > 0 ? 4 * Track.UNIT : 0;
        for (int i = r.cursor; i < track.size(); i++) {
            Track.Entry e = track.get(i);
            if (e.z > r.z + Runner.HALF_DEPTH + reach) {
                break;
            }
            if (r.took(e.index)) {
                continue;
            }
            if (e.pickup != null) {
                boolean magnetised = r.magnet > 0 && e.pickup.kind != PickupKind.MAGNET;
                int window = magnetised ? reach : 0;
                if (overlapsZ(r, e, window) && (magnetised || inLane(r, e, 500))) {
                    r.take(e.index);
                    collect(r, e.pickup);
                }
            } else if (overlapsZ(r, e, 0) && inLane(r, e, 600)) {
                if (!cleared(r, e.obstacle)) {
                    r.take(e.index);
                    hit(r, e.obstacle);
                    if (!r.alive) {
                        return;
                    }
                }
            }
        }
    }

    private static boolean overlapsZ(Runner r, Track.Entry e, int extra) {
        return r.z + Runner.HALF_DEPTH + extra >= e.z && r.z - Runner.HALF_DEPTH - extra <= e.z + e.depth;
    }

    private static boolean inLane(Runner r, Track.Entry e, int tolerance) {
        if (e.spansAllLanes()) {
            return true;
        }
        int d = r.laneX - e.lane * 1000;
        return (d < 0 ? -d : d) < tolerance;
    }

    private static boolean cleared(Runner r, ObstacleDef o) {
        switch (o.clearance) {
            case JUMP:
                return r.y > o.height;
            case SLIDE:
                return r.sliding();
            default:
                return false;
        }
    }

    private void hit(Runner r, ObstacleDef o) {
        if (r.boost > 0 || r.invulnerable > 0) {
            return;
        }
        if (r.shield > 0) {
            r.shield = 0;
            r.invulnerable = 30;
            return;
        }
        if (o.lethal || tick - r.lastStumble <= Runner.STUMBLE_WINDOW) {
            end(r, false);
            return;
        }
        r.stumbles++;
        r.lastStumble = tick;
        r.invulnerable = 60;
        r.speed = Math.max(level.startSpeed / 2, r.speed - r.speed / 3);
    }

    private static void collect(Runner r, PickupDef p) {
        r.points += p.points;
        switch (p.kind) {
            case COIN:
                r.coins++;
                break;
            case MAGNET:
                r.magnet = p.ticks;
                break;
            case SHIELD:
                r.shield = p.ticks;
                break;
            case BOOST:
                r.boost = p.ticks;
                break;
            default:
                break;
        }
    }

    private void end(Runner r, boolean finished) {
        if (!r.running()) {
            return;
        }
        if (finished) {
            r.finished = true;
        } else {
            r.alive = false;
        }
        r.endTick = tick;
    }

    /// Stars earned by runner `i`: 0 to 3. A finite goal not reached earns none.
    public int stars(int i) {
        Runner r = runners[i];
        if (level.goal != Goal.ENDLESS && !r.finished) {
            return 0;
        }
        int s = 0;
        for (int k = 0; k < level.stars.length && k < 3; k++) {
            if (r.score() >= level.stars[k]) {
                s = k + 1;
            }
        }
        return s;
    }

    /// FNV hash of everything that determines the rest of the run. Two runtimes
    /// that disagree here have desynchronised.
    public long stateHash() {
        Hash h = new Hash();
        h.add(tick).add(seed);
        for (int i = 0; i < runners.length; i++) {
            runners[i].hash(h);
        }
        return track.hashInto(h);
    }
}
