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

/// One paintball match: the whole simulation, and the only thing that changes it
/// is [#step].
///
/// The server runs one per room and is the authority. The phone runs the same
/// class to predict its own player between server snapshots, and the replay viewer
/// runs it from a seed and an input log. All three agree because nothing in here
/// reads a clock, a float or a random number the [Rng] did not make.
///
/// Players are one-hit: a ball that bursts on you puts you out, and you walk back
/// to your spawn with your marker over your head, as the field rules demand. For a
/// second after that you may [Input#WIPE] the paint off. Usually the referee sees.
public final class Match {
    public static final int TICK_HZ = 30;
    public static final int TIME_LIMIT = 6 * 60 * TICK_HZ;
    public static final int HEIGHT = 1750;
    public static final int CROUCH_HEIGHT = 1150;
    public static final int RADIUS = 350;
    public static final int STEP_UP = 450;
    public static final int GRAVITY = 11;
    public static final int JUMP_SPEED = 140;
    public static final int WALK = 150;
    public static final int SPRINT = 215;
    public static final int CROUCH_WALK = 70;
    /// Speed while aiming down the sights, in percent.
    public static final int ADS_SPEED = 55;
    public static final int PITCH_LIMIT = 900;
    public static final int BALL_GRAVITY = 6;
    public static final int BALL_LIFE = 90;
    public static final int RESPAWN = 3 * TICK_HZ;
    /// Ticks after being painted during which a player may try to wipe it off.
    public static final int WIPE_WINDOW = TICK_HZ;
    /// Extra time out for a wipe the referee caught.
    public static final int WIPE_PENALTY = 3 * TICK_HZ;
    /// In how many tries a wipe goes unnoticed (one in this many).
    public static final int WIPE_ODDS = 3;
    /// Ticks after a respawn during which paint does not count ("still fixing my goggles").
    public static final int SHIELD = 45;
    /// The most a shot is moved forward to make up for the shooter's lag.
    public static final int MAX_LAG_TICKS = 6;
    public static final int GRENADE_SPEED = 700;
    public static final int GRENADE_FUSE = 40;
    public static final int GRENADE_RADIUS = 2500;
    public static final int FLAG_REACH = 1200;
    public static final int FLAG_AUTO_RETURN = 20 * TICK_HZ;
    /// Speed while carrying a flag, in percent.
    public static final int FLAG_CARRY_SPEED = 85;

    private final long seed;
    private final Arena arena;
    private final Mode mode;
    private final Player[] players;
    private final List<Ball> balls = new ArrayList<Ball>();
    private final List<Event> events = new ArrayList<Event>();
    private Rng rng;
    private Rng botRng;
    private int tick;
    private final int[] score = new int[2];
    private boolean over;
    private int winner = -1;
    private final int[] flagX = new int[2];
    private final int[] flagY = new int[2];
    private final int[] flagZ = new int[2];
    private final boolean[] flagHome = {true, true};
    private final int[] flagCarrier = {-1, -1};
    private final int[] flagDroppedAt = new int[2];

    public Match(long seed, Arena arena, Mode mode, List<Slot> slots) {
        if (slots.isEmpty()) {
            throw new IllegalArgumentException("no players");
        }
        this.seed = seed;
        this.arena = arena;
        this.mode = mode;
        this.rng = new Rng(seed);
        this.botRng = new Rng(seed ^ 0x5EED0B07L);
        players = new Player[slots.size()];
        int[] perTeam = new int[2];
        for (int i = 0; i < players.length; i++) {
            Player p = new Player(i, slots.get(i));
            players[i] = p;
            int t = p.team();
            spawn(p, perTeam[t] % arena.spawnCount(t), 0);
            perTeam[t]++;
        }
        events.clear();
        for (int t = 0; t < 2; t++) {
            homeFlag(t);
        }
    }

    // ---------------------------------------------------------------- reading

    public long seed() {
        return seed;
    }

    public Arena arena() {
        return arena;
    }

    public Mode mode() {
        return mode;
    }

    /// Ticks simulated so far; the next [#step] simulates this tick.
    public int tick() {
        return tick;
    }

    public boolean over() {
        return over;
    }

    /// The winning team, or -1 for a draw or a match still running.
    public int winner() {
        return winner;
    }

    public int score(int team) {
        return score[team];
    }

    public int playerCount() {
        return players.length;
    }

    public Player player(int i) {
        return players[i];
    }

    public List<Ball> balls() {
        return Collections.unmodifiableList(balls);
    }

    /// What happened during the last [#step].
    public List<Event> events() {
        return Collections.unmodifiableList(events);
    }

    public int flagX(int team) {
        return flagX[team];
    }

    public int flagY(int team) {
        return flagY[team];
    }

    public int flagZ(int team) {
        return flagZ[team];
    }

    public boolean flagHome(int team) {
        return flagHome[team];
    }

    public int flagCarrier(int team) {
        return flagCarrier[team];
    }

    public int ticksLeft() {
        return Math.max(0, TIME_LIMIT - tick);
    }

    // ---------------------------------------------------------------- stepping

    /// Simulates one tick. `inputs` has one entry per player; a null entry (and
    /// every bot's entry, which is ignored) repeats that player's last input.
    public void step(Input[] inputs) {
        events.clear();
        if (over) {
            return;
        }
        int t = tick;
        for (int i = 0; i < players.length; i++) {
            Player p = players[i];
            if (p.slot.bot()) {
                p.input = Bot.think(this, p, botRng);
            } else if (inputs != null && i < inputs.length && inputs[i] != null) {
                p.input = inputs[i];
            }
        }
        for (int i = 0; i < players.length; i++) {
            Player p = players[i];
            Input in = p.input;
            int pressed = in.buttons & ~p.lastButtons;
            if (p.alive) {
                act(p, in, pressed, t);
            } else if ((pressed & Input.WIPE) != 0 && p.canWipe(t)) {
                wipe(p, t);
            } else if (t >= p.respawnAt) {
                Arena a = arena;
                spawn(p, rng.nextInt(a.spawnCount(p.team())), t);
            }
            p.lastButtons = in.buttons;
        }
        for (int i = 0; i < balls.size(); i++) {
            Ball b = balls.get(i);
            if (!fly(b, t)) {
                balls.remove(i);
                i--;
            }
        }
        objectives(t);
        tick = t + 1;
        finishIfDone(t);
    }

    private void act(Player p, Input in, int pressed, int t) {
        p.yaw = in.yaw;
        p.pitch = in.pitch;
        p.crouched = in.has(Input.CROUCH);
        boolean aiming = in.has(Input.AIM);
        // A sprint ends the moment you aim or pull the trigger.
        boolean sprint = in.has(Input.SPRINT) && in.forward > 0 && !aiming && !in.has(Input.FIRE);
        int speed = p.crouched ? CROUCH_WALK : (sprint ? SPRINT : WALK);
        speed = speed * p.marker.mobility / 100;
        if (aiming) {
            speed = speed * ADS_SPEED / 100;
        }
        if (p.carrying >= 0) {
            speed = speed * FLAG_CARRY_SPEED / 100;
        }
        int sin = Trig.sin(p.yaw);
        int cos = Trig.cos(p.yaw);
        long mx = (long) in.forward * sin + (long) in.strafe * cos;
        long mz = (long) in.forward * cos - (long) in.strafe * sin;
        long full = 127L * Trig.ONE;
        long mag = FixedMath.isqrt(mx * mx + mz * mz);
        long den = mag > full ? mag : full;
        int dx = (int) (mx * speed / den);
        int dz = (int) (mz * speed / den);
        if (dx != 0 && !arena.blocked(p.x + dx, p.z, RADIUS, p.y, STEP_UP)) {
            p.x += dx;
        }
        if (dz != 0 && !arena.blocked(p.x, p.z + dz, RADIUS, p.y, STEP_UP)) {
            p.z += dz;
        }
        if (p.grounded && (pressed & Input.JUMP) != 0 && !p.crouched) {
            p.vy = JUMP_SPEED;
        }
        p.y += p.vy;
        p.vy -= GRAVITY;
        int ground = arena.groundAt(p.x, p.z, RADIUS, p.y + STEP_UP);
        if (p.y <= ground) {
            p.y = ground;
            p.vy = 0;
            p.grounded = true;
        } else {
            p.grounded = false;
        }

        if (p.reloadUntil > 0 && t >= p.reloadUntil) {
            p.ammo = p.marker.hopper;
            p.reloadUntil = 0;
        }
        if ((pressed & Input.RELOAD) != 0 && p.reloadUntil == 0 && p.ammo < p.marker.hopper) {
            startReload(p, t);
        }
        if (in.has(Input.FIRE) && t >= p.cooldownUntil && p.reloadUntil == 0 && p.ammo > 0) {
            fire(p, in, t);
        }
        if ((pressed & Input.THROW) != 0 && p.grenade) {
            p.grenade = false;
            Ball b = launch(p, p.yaw, FixedMath.clamp(p.pitch + 220, -PITCH_LIMIT, PITCH_LIMIT),
                    GRENADE_SPEED, t + GRENADE_FUSE);
            b.grenade = true;
            balls.add(b);
        }
    }

    private void startReload(Player p, int t) {
        p.reloadUntil = t + p.marker.reload;
        events.add(new Event(Event.RELOAD, t, p.index, 0, p.x, p.y, p.z));
    }

    private void fire(Player p, Input in, int t) {
        MarkerDef m = p.marker;
        int s = in.has(Input.AIM) ? m.spread / 4 : m.spread;
        int yaw = p.yaw;
        int pitch = p.pitch;
        if (s > 0) {
            yaw += rng.nextInt(2 * s + 1) - s;
            pitch += rng.nextInt(s + 1) - s / 2;
        }
        Ball b = launch(p, yaw, FixedMath.clamp(pitch, -PITCH_LIMIT, PITCH_LIMIT), m.speed, t + BALL_LIFE);
        p.ammo--;
        p.cooldownUntil = t + m.cooldown;
        events.add(new Event(Event.SHOT, t, p.index, 0, b.x, b.y, b.z));
        // tag::splat-lag-comp[]
        // The shooter aimed at the world as it was `viewTick`; the ball is flown
        // forward the ticks it lost on the way here, at most MAX_LAG_TICKS, so a
        // shot on a phone with a slow connection still lands where it was aimed.
        int lag = in.viewTick < 0 ? 0 : FixedMath.clamp(t - in.viewTick, 0, MAX_LAG_TICKS);
        boolean alive = true;
        for (int i = 0; i < lag && alive; i++) {
            alive = fly(b, t);
        }
        if (alive) {
            balls.add(b);
        }
        // end::splat-lag-comp[]
        if (p.ammo == 0) {
            startReload(p, t);
        }
    }

    private Ball launch(Player p, int yaw, int pitch, int speed, int expires) {
        Ball b = new Ball();
        b.owner = p.index;
        b.team = p.team();
        int cp = Trig.cos(pitch);
        int horiz = (int) ((long) speed * cp / Trig.ONE);
        b.vx = (int) ((long) horiz * Trig.sin(yaw) / Trig.ONE);
        b.vz = (int) ((long) horiz * Trig.cos(yaw) / Trig.ONE);
        b.vy = (int) ((long) speed * Trig.sin(pitch) / Trig.ONE);
        b.x = p.x + (int) ((long) (RADIUS + 50) * Trig.sin(yaw) / Trig.ONE);
        b.z = p.z + (int) ((long) (RADIUS + 50) * Trig.cos(yaw) / Trig.ONE);
        b.y = p.eyeY() - 120;
        b.expires = expires;
        return b;
    }

    /// Moves a ball one tick in steps short enough not to pass through a player.
    /// Answers false once it has burst.
    private boolean fly(Ball b, int t) {
        int ax = Math.abs(b.vx);
        int ay = Math.abs(b.vy);
        int az = Math.abs(b.vz);
        int longest = Math.max(ax, Math.max(ay, az));
        int steps = longest / 200 + 1;
        int sx = b.x;
        int sy = b.y;
        int sz = b.z;
        for (int s = 1; s <= steps; s++) {
            int px = b.x;
            int py = b.y;
            int pz = b.z;
            b.x = sx + (int) ((long) b.vx * s / steps);
            b.y = sy + (int) ((long) b.vy * s / steps);
            b.z = sz + (int) ((long) b.vz * s / steps);
            Player hit = playerAt(b.x, b.y, b.z, b.owner);
            if (hit != null) {
                if (b.grenade) {
                    burst(b, px, py, pz, t);
                } else if (hit.team() == b.team) {
                    events.add(new Event(Event.FRIENDLY, t, hit.index, b.owner, b.x, b.y, b.z));
                } else {
                    paint(hit, b.owner, t, b.x, b.y, b.z);
                }
                return false;
            }
            if (b.y <= 0 || !arena.inside(b.x, b.z) || arena.boxAt(b.x, b.y, b.z) != null) {
                if (b.grenade) {
                    burst(b, px, py, pz, t);
                } else {
                    events.add(new Event(Event.SPLAT, t, b.owner, b.team, px, Math.max(0, py), pz));
                }
                return false;
            }
        }
        b.vy -= b.grenade ? GRAVITY : BALL_GRAVITY;
        if (t >= b.expires) {
            if (b.grenade) {
                burst(b, b.x, b.y, b.z, t);
            }
            return false;
        }
        return true;
    }

    private Player playerAt(int x, int y, int z, int ignore) {
        long r2 = (long) RADIUS * RADIUS;
        for (int i = 0; i < players.length; i++) {
            Player p = players[i];
            if (i == ignore || !p.alive) {
                continue;
            }
            long dx = (long) x - p.x;
            long dz = (long) z - p.z;
            if (dx * dx + dz * dz <= r2 && y >= p.y && y <= p.y + p.height()) {
                return p;
            }
        }
        return null;
    }

    private void burst(Ball b, int x, int y, int z, int t) {
        events.add(new Event(Event.GRENADE, t, b.owner, b.team, x, Math.max(0, y), z));
        long r2 = (long) GRENADE_RADIUS * GRENADE_RADIUS;
        for (int i = 0; i < players.length; i++) {
            Player p = players[i];
            if (!p.alive || p.team() == b.team) {
                continue;
            }
            int cy = p.y + p.height() / 2;
            long dx = (long) p.x - x;
            long dy = (long) cy - y;
            long dz = (long) p.z - z;
            if (dx * dx + dy * dy + dz * dz <= r2 && arena.clear(x, Math.max(50, y), z, p.x, cy, p.z)) {
                paint(p, b.owner, t, p.x, cy, p.z);
            }
        }
    }

    private void paint(Player victim, int shooter, int t, int x, int y, int z) {
        if (victim.shielded(t)) {
            events.add(new Event(Event.SPLAT, t, shooter, players[shooter].team(), x, y, z));
            return;
        }
        victim.alive = false;
        victim.outs++;
        victim.respawnAt = t + RESPAWN;
        victim.wipeUntil = t + WIPE_WINDOW;
        victim.wiped = false;
        victim.paintedBy = shooter;
        players[shooter].paints++;
        if (mode == Mode.TEAM_PAINT) {
            score[players[shooter].team()]++;
        }
        events.add(new Event(Event.PAINTED, t, shooter, victim.index, x, y, z));
        dropFlag(victim, t);
    }

    private void wipe(Player p, int t) {
        p.wiped = true;
        if (rng.nextInt(WIPE_ODDS) == 0) {
            p.alive = true;
            p.outs--;
            Player s = players[p.paintedBy];
            s.paints--;
            if (mode == Mode.TEAM_PAINT && score[s.team()] > 0) {
                score[s.team()]--;
            }
            events.add(new Event(Event.WIPE_OK, t, p.index, p.paintedBy, p.x, p.y, p.z));
        } else {
            p.respawnAt += WIPE_PENALTY;
            events.add(new Event(Event.WIPE_CAUGHT, t, p.index, p.paintedBy, p.x, p.y, p.z));
        }
    }

    private void spawn(Player p, int spawnIndex, int t) {
        int team = p.team();
        p.x = arena.spawnX(team, spawnIndex);
        p.z = arena.spawnZ(team, spawnIndex);
        p.y = arena.groundAt(p.x, p.z, RADIUS, Integer.MAX_VALUE);
        p.vy = 0;
        p.grounded = true;
        p.crouched = false;
        p.alive = true;
        p.yaw = team == 0 ? 0 : Trig.HALF;
        p.pitch = 0;
        p.ammo = p.marker.hopper;
        p.reloadUntil = 0;
        p.cooldownUntil = t;
        p.grenade = true;
        p.wiped = false;
        p.protectedUntil = t + SHIELD;
        events.add(new Event(Event.RESPAWN, t, p.index, team, p.x, p.y, p.z));
    }

    // ---------------------------------------------------------------- objectives

    private void homeFlag(int team) {
        flagX[team] = arena.baseX(team);
        flagZ[team] = arena.baseZ(team);
        flagY[team] = 0;
        flagHome[team] = true;
        flagCarrier[team] = -1;
    }

    private void dropFlag(Player p, int t) {
        int f = p.carrying;
        if (f < 0) {
            return;
        }
        p.carrying = -1;
        flagCarrier[f] = -1;
        flagX[f] = p.x;
        flagY[f] = p.y;
        flagZ[f] = p.z;
        flagDroppedAt[f] = t;
        events.add(new Event(Event.FLAG_DROPPED, t, p.index, f, p.x, p.y, p.z));
    }

    private void objectives(int t) {
        if (mode == Mode.CAPTURE_THE_FLAG) {
            flags(t);
        } else if (mode == Mode.SNACK_BAR) {
            hill(t);
        }
    }

    private void flags(int t) {
        for (int f = 0; f < 2; f++) {
            int c = flagCarrier[f];
            if (c >= 0) {
                Player p = players[c];
                flagX[f] = p.x;
                flagY[f] = p.y;
                flagZ[f] = p.z;
                int own = p.team();
                if (FixedMath.within(p.x, p.z, arena.baseX(own), arena.baseZ(own), FLAG_REACH)) {
                    p.carrying = -1;
                    p.captures++;
                    score[own]++;
                    homeFlag(f);
                    events.add(new Event(Event.CAPTURE, t, p.index, own, p.x, p.y, p.z));
                }
                continue;
            }
            for (int i = 0; i < players.length; i++) {
                Player p = players[i];
                if (!p.alive || !FixedMath.within(p.x, p.z, flagX[f], flagZ[f], FLAG_REACH)) {
                    continue;
                }
                if (p.team() != f && p.carrying < 0) {
                    flagCarrier[f] = i;
                    flagHome[f] = false;
                    p.carrying = f;
                    events.add(new Event(Event.FLAG_TAKEN, t, i, f, p.x, p.y, p.z));
                    break;
                }
                if (p.team() == f && !flagHome[f]) {
                    homeFlag(f);
                    events.add(new Event(Event.FLAG_RETURNED, t, i, f, flagX[f], 0, flagZ[f]));
                    break;
                }
            }
            if (!flagHome[f] && flagCarrier[f] < 0 && t >= flagDroppedAt[f] + FLAG_AUTO_RETURN) {
                homeFlag(f);
                events.add(new Event(Event.FLAG_RETURNED, t, -1, f, flagX[f], 0, flagZ[f]));
            }
        }
    }

    private void hill(int t) {
        if (t % TICK_HZ != 0) {
            return;
        }
        int[] in = new int[2];
        for (int i = 0; i < players.length; i++) {
            Player p = players[i];
            if (p.alive && FixedMath.within(p.x, p.z, arena.hillX, arena.hillZ, arena.hillRadius)) {
                in[p.team()]++;
            }
        }
        for (int team = 0; team < 2; team++) {
            if (in[team] > 0 && in[1 - team] == 0) {
                score[team]++;
                events.add(new Event(Event.HILL_POINT, t, -1, team, arena.hillX, 0, arena.hillZ));
            }
        }
    }

    private void finishIfDone(int t) {
        int target = mode.target;
        if (score[0] >= target || score[1] >= target || tick >= TIME_LIMIT) {
            over = true;
            winner = score[0] == score[1] ? -1 : (score[0] > score[1] ? 0 : 1);
            events.add(new Event(Event.END, t, -1, winner, 0, 0, 0));
        }
    }

    // ---------------------------------------------------------------- snapshots

    /// Everything that changes, as ints. Restoring it into a match built from the
    /// same seed, arena, mode and roster continues exactly as the original would.
    public int[] snapshot() {
        IntBuf b = new IntBuf();
        b.add(RulesVersion.ID).add(tick).add(rng.state()).add(botRng.state());
        b.add(score[0]).add(score[1]).add(over).add(winner);
        for (int f = 0; f < 2; f++) {
            b.add(flagX[f]).add(flagY[f]).add(flagZ[f]).add(flagHome[f]).add(flagCarrier[f]).add(flagDroppedAt[f]);
        }
        b.add(players.length);
        for (int i = 0; i < players.length; i++) {
            players[i].write(b);
        }
        b.add(balls.size());
        for (int i = 0; i < balls.size(); i++) {
            balls.get(i).write(b);
        }
        return b.toArray();
    }

    public void restore(int[] data) {
        IntBuf.Reader r = new IntBuf.Reader(data);
        int version = r.next();
        if (version != RulesVersion.ID) {
            throw new IllegalArgumentException("rules version " + version + ", expected " + RulesVersion.ID);
        }
        tick = r.next();
        rng = new Rng(r.nextLong());
        botRng = new Rng(r.nextLong());
        score[0] = r.next();
        score[1] = r.next();
        over = r.bool();
        winner = r.next();
        for (int f = 0; f < 2; f++) {
            flagX[f] = r.next();
            flagY[f] = r.next();
            flagZ[f] = r.next();
            flagHome[f] = r.bool();
            flagCarrier[f] = r.next();
            flagDroppedAt[f] = r.next();
        }
        int n = r.next();
        if (n != players.length) {
            throw new IllegalArgumentException("roster has " + players.length + " players, snapshot " + n);
        }
        for (int i = 0; i < n; i++) {
            players[i].read(r);
        }
        balls.clear();
        int nb = r.next();
        for (int i = 0; i < nb; i++) {
            balls.add(Ball.read(r));
        }
        if (!r.done()) {
            throw new IllegalArgumentException("snapshot has trailing data");
        }
        events.clear();
    }

    /// A hash of the whole state, compared between server and clients to find a desync.
    public long stateHash() {
        Hash h = new Hash();
        int[] s = snapshot();
        for (int i = 0; i < s.length; i++) {
            h.add(s[i]);
        }
        return h.value();
    }
}
