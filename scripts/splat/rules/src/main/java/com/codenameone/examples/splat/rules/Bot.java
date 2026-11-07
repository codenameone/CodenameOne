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

/// The players who fill empty slots. Bots live in the rules so a match with bots
/// is still just a seed and the humans' inputs: the server, the phone and the
/// replay viewer all make the same bot decisions from the same [Rng].
///
/// A bot is stateless between ticks -- it reads the match and its own player and
/// answers an [Input] -- so there is nothing extra to snapshot. Skill sets how far
/// it sees, how fast it turns, how well it aims, and (inversely) how often it tries
/// to wipe the paint off when it is hit.
public final class Bot {
    /// Furthest a bot notices an enemy.
    static final int SIGHT = 30 * Arena.UNIT;

    private Bot() {
    }

    public static Input think(Match m, Player p, Rng r) {
        int t = m.tick();
        int skill = p.slot.botSkill;
        if (!p.alive()) {
            // the worse the bot, the more it argues
            boolean wipe = p.canWipe(t) && r.nextInt(100) >= skill;
            return new Input(0, 0, p.yaw(), 0, wipe ? Input.WIPE : 0, -1);
        }
        Player target = nearestVisibleEnemy(m, p, skill);
        int[] goal = goal(m, p);
        int buttons = 0;
        int yaw;
        int pitch = 0;
        int forward;
        int strafe = 0;
        int maxTurn = 30 + skill * 2;
        if (target != null) {
            long dx = (long) target.x() - p.x();
            long dz = (long) target.z() - p.z();
            long dist = FixedMath.isqrt(dx * dx + dz * dz);
            int speed = p.marker.speed;
            int flight = (int) (dist / (speed > 0 ? speed : 1));
            // lead a moving target by its last input, roughly
            int lead = (target.input().forward * Match.WALK / 127) * flight;
            long lx = dx + (long) lead * Trig.sin(target.yaw()) / Trig.ONE;
            long lz = dz + (long) lead * Trig.cos(target.yaw()) / Trig.ONE;
            int want = Trig.atan2(lx, lz);
            int noise = (100 - skill) * 3 + 25 + (int) (dist / 250);
            if (noise > 0) {
                want += r.nextInt(2 * noise + 1) - noise;
            }
            int drop = Match.BALL_GRAVITY * flight * flight / 2;
            int aimY = target.y() + target.height() * 2 / 3 + drop - p.eyeY();
            pitch = Trig.atan2(aimY, dist);
            if (pitch > Trig.HALF) {
                pitch -= Trig.FULL;
            }
            yaw = turn(p.yaw(), want, maxTurn);
            if (Math.abs(Trig.delta(yaw, want)) < 50 + (100 - skill) && r.nextInt(100) < 15 + skill / 3) {
                buttons |= Input.FIRE;
            }
            // weave: change direction every couple of seconds
            int weave = ((t / 50 + p.index) & 1) == 0 ? 110 : -110;
            if (m.mode() == Mode.TEAM_PAINT || p.carrying() < 0 && (p.index & 3) == 1) {
                // weave the other way rather than into a wall
                if (!open(m.arena(), p, Trig.wrap(yaw + (weave > 0 ? Trig.FULL / 4 : -Trig.FULL / 4)), 900)) {
                    weave = -weave;
                }
                strafe = weave;
                forward = dist > 14 * Arena.UNIT ? 100 : (dist < 6 * Arena.UNIT ? -60 : 0);
                if (forward < 0 && !open(m.arena(), p, Trig.wrap(yaw + Trig.HALF), 900)) {
                    forward = 0;
                }
            } else {
                // an objective to reach: keep shooting, keep walking there
                int rel = steer(m, p, goal) - yaw;
                forward = 127 * Trig.cos(rel) / Trig.ONE;
                strafe = FixedMath.clamp(127 * Trig.sin(rel) / Trig.ONE + weave / 3, -127, 127);
            }
            if (p.hasGrenade() && dist > 8 * Arena.UNIT && dist < 18 * Arena.UNIT && r.nextInt(3000) < skill) {
                buttons |= Input.THROW;
            }
            // Good bots aim down the sights at range; bad ones hip-fire everything.
            if (dist > 12 * Arena.UNIT && skill > 40) {
                buttons |= Input.AIM;
            }
            if (p.marker.spread > 20 && dist > 10 * Arena.UNIT && m.mode() == Mode.TEAM_PAINT) {
                buttons |= Input.CROUCH;
                forward = 0;
            }
        } else {
            boolean there = FixedMath.within(p.x(), p.z(), goal[0], goal[1], 1500);
            int want = steer(m, p, goal);
            yaw = turn(p.yaw(), want, maxTurn);
            int rel = want - yaw;
            forward = there ? 0 : 127 * Trig.cos(rel) / Trig.ONE;
            strafe = there ? 0 : 127 * Trig.sin(rel) / Trig.ONE;
            if (!there && Math.abs(Trig.delta(yaw, want)) < 300) {
                buttons |= Input.SPRINT;
            }
            if (p.ammo() < p.marker.hopper / 2 && !p.reloading(t)) {
                buttons |= Input.RELOAD;
            }
        }
        // Re-press a held button on alternating ticks so edge-triggered ones fire.
        if ((t & 1) == 1) {
            buttons &= ~(Input.JUMP | Input.RELOAD | Input.THROW);
        }
        return new Input(strafe, forward, yaw, pitch, buttons, -1);
    }

    /// The heading toward the next point on the arena's path to `goal`, so a
    /// bot goes round cover instead of walking into it.
    static int steer(Match m, Player p, int[] goal) {
        int[] to = m.arena().nav().next(p.x(), p.z(), goal[0], goal[1]);
        return Trig.atan2((long) to[0] - p.x(), (long) to[1] - p.z());
    }

    private static boolean open(Arena a, Player p, int dir, int dist) {
        int x = p.x() + (int) ((long) dist * Trig.sin(dir) / Trig.ONE);
        int z = p.z() + (int) ((long) dist * Trig.cos(dir) / Trig.ONE);
        return !a.blocked(x, z, Match.RADIUS, p.y(), Match.STEP_UP);
    }

    private static int turn(int from, int to, int max) {
        int d = Trig.delta(from, to);
        if (d > max) {
            d = max;
        } else if (d < -max) {
            d = -max;
        }
        return Trig.wrap(from + d);
    }

    private static Player nearestVisibleEnemy(Match m, Player p, int skill) {
        long sight = (long) SIGHT * (50 + skill) / 150;
        long best = sight * sight;
        Player out = null;
        for (int i = 0; i < m.playerCount(); i++) {
            Player e = m.player(i);
            if (!e.alive() || e.team() == p.team()) {
                continue;
            }
            long d = FixedMath.distSq(p.x(), p.z(), e.x(), e.z());
            if (d < best && m.arena().clear(p.x(), p.eyeY(), p.z(), e.x(), e.y() + e.height() - 200, e.z())) {
                best = d;
                out = e;
            }
        }
        return out;
    }

    private static int[] goal(Match m, Player p) {
        Arena a = m.arena();
        int team = p.team();
        if (m.mode() == Mode.CAPTURE_THE_FLAG) {
            if (p.carrying() >= 0) {
                return new int[] {a.baseX(team), a.baseZ(team)};
            }
            int own = team;
            if (!m.flagHome(own) && m.flagCarrier(own) < 0 && (p.index & 3) == 1) {
                return new int[] {m.flagX(own), m.flagZ(own)};
            }
            if ((p.index & 3) == 1) {
                // the defender: hangs around the base, bravely
                return new int[] {a.baseX(team), a.baseZ(team) + (team == 0 ? 4000 : -4000)};
            }
            int enemy = 1 - team;
            int c = m.flagCarrier(enemy);
            if (c >= 0 && c != p.index) {
                Player carrier = m.player(c);
                return new int[] {carrier.x(), carrier.z()};
            }
            return new int[] {m.flagX(enemy), m.flagZ(enemy)};
        }
        if (m.mode() == Mode.SNACK_BAR) {
            int spread = (p.index % 4) * 900 - 1350;
            return new int[] {a.hillX + spread, a.hillZ + ((p.index & 1) == 0 ? 2200 : -2200)};
        }
        // Team Paint: go where the other team spawns, by lanes
        int lane = (p.index % 3) - 1;
        return new int[] {a.width / 2 + lane * a.width / 4, team == 0 ? a.depth * 3 / 4 : a.depth / 4};
    }
}
