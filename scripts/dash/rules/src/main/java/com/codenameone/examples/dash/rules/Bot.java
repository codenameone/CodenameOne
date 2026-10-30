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

/// A runner that plays by looking ahead. Used for ghosts when nobody else is
/// online, for the editor's "can this be beaten?" check and for the soak tests.
///
/// It has its own random stream, seeded apart from the run's, so a bot's lapses
/// never change the track; what it decides is recorded as input like a player's.
public final class Bot {
    private final Rng rng;
    /// 0 to 100: how often it reacts in time, and how greedily it chases coins.
    private final int skill;
    private int cooldown;

    public Bot(long seed, int skill) {
        this.rng = new Rng(seed * 31 + 7);
        this.skill = FixedMath.clamp(skill, 0, 100);
    }

    public int decide(Run run, int who) {
        Runner r = run.runner(who);
        if (!r.running()) {
            return Input.NONE;
        }
        if (cooldown > 0) {
            cooldown--;
            return Input.NONE;
        }
        int perTick = Math.max(1, r.speed / Run.TICKS_PER_SECOND);
        int look = perTick * 40;
        Track t = run.track();
        Track.Entry threat = nearestObstacle(t, r, r.lane, look);
        if (threat != null) {
            int dist = threat.z - (r.z + Runner.HALF_DEPTH);
            if (rng.nextInt(100) >= skill + 20) {
                // A lapse: it sees the obstacle a few ticks late.
                cooldown = 3;
                return Input.NONE;
            }
            int bits = respond(run, r, threat, dist, perTick, look);
            if (bits != Input.NONE) {
                cooldown = bits == Input.JUMP ? 6 : 2;
            }
            return bits;
        }
        if (rng.nextInt(100) < skill / 2) {
            return chaseCoins(run, r, look);
        }
        return Input.NONE;
    }

    private int respond(Run run, Runner r, Track.Entry e, int dist, int perTick, int look) {
        Clearance c = e.obstacle.clearance;
        if (c == Clearance.DODGE || (e.obstacle.lethal && !e.spansAllLanes() && dist > perTick * 12)) {
            int lanes = run.level().lanes;
            int best = -1;
            for (int d = 1; d < lanes && best < 0; d++) {
                for (int s = -1; s <= 1; s += 2) {
                    int l = r.lane + s * d;
                    if (l >= 0 && l < lanes && nearestObstacle(run.track(), r, l, look) == null) {
                        best = l;
                        break;
                    }
                }
            }
            if (best >= 0) {
                return best < r.lane ? Input.LEFT : Input.RIGHT;
            }
            if (c == Clearance.DODGE) {
                return Input.NONE;
            }
        }
        if (c == Clearance.JUMP && dist <= perTick * 14 && r.y == 0) {
            return Input.JUMP;
        }
        if (c == Clearance.SLIDE && dist <= perTick * 10 && !r.sliding()) {
            return Input.SLIDE;
        }
        return Input.NONE;
    }

    private static Track.Entry nearestObstacle(Track t, Runner r, int lane, int look) {
        for (int i = r.cursor; i < t.size(); i++) {
            Track.Entry e = t.get(i);
            if (e.obstacle == null || e.z + e.depth < r.z - Runner.HALF_DEPTH) {
                continue;
            }
            if (e.z > r.z + look) {
                break;
            }
            if (e.spansAllLanes() || e.lane == lane) {
                return e;
            }
        }
        return null;
    }

    private static int chaseCoins(Run run, Runner r, int look) {
        Track t = run.track();
        for (int i = r.cursor; i < t.size(); i++) {
            Track.Entry e = t.get(i);
            if (e.z < r.z || e.pickup == null || e.spansAllLanes()) {
                continue;
            }
            if (e.z > r.z + look) {
                break;
            }
            if (e.lane != r.lane && nearestObstacle(t, r, e.lane, look) == null) {
                return e.lane < r.lane ? Input.LEFT : Input.RIGHT;
            }
            return Input.NONE;
        }
        return Input.NONE;
    }
}
