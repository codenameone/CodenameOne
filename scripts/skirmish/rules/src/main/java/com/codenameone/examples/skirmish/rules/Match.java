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
import java.util.List;

/// A whole battle, advanced one fixed tick at a time.
///
/// This class is the game. The server runs it to decide what happened, each app
/// runs it to show what is happening, offline practice runs it against [Bot], and a
/// replay is nothing but a seed, two decks and the [Command]s, run through it again.
/// That only works because a match is a pure function of those inputs:
///
/// - time is the tick counter, never a clock;
/// - randomness is the seeded [Rng];
/// - arithmetic is integer ([FixedMath]);
/// - units are processed in creation order, so ties always break the same way.
///
/// [#stateHash] condenses the state into one number that the server sends now and
/// then; a client whose own hash differs has desynced, and recovers by re-running
/// the command log from the start.
public final class Match {
    public static final int TICKS_PER_SECOND = 20;
    public static final int REGULAR_TICKS = 180 * TICKS_PER_SECOND;
    public static final int OVERTIME_TICKS = 60 * TICKS_PER_SECOND;
    public static final int DOUBLE_ELIXIR_FROM = 120 * TICKS_PER_SECOND;
    public static final int MAX_TICKS = REGULAR_TICKS + OVERTIME_TICKS;
    /// How far past the river a player may deploy once that lane's tower is down.
    public static final int POCKET_DEPTH = 4 * Arena.TILE;

    /// Result: no winner yet.
    public static final int ONGOING = -2;
    /// Result: the match ended level.
    public static final int DRAW = -1;

    private final long seed;
    private final Rng rng;
    private final PlayerState[] players = new PlayerState[2];
    private final List<Unit> units = new ArrayList<Unit>();
    private final List<Event> events = new ArrayList<Event>();
    private int nextId = 1;
    private int tick;
    private int result = ONGOING;

    public Match(long seed, String[] deck0, String[] deck1) {
        if (!Cards.isValidDeck(deck0) || !Cards.isValidDeck(deck1)) {
            throw new IllegalArgumentException("invalid deck");
        }
        this.seed = seed;
        this.rng = new Rng(seed);
        players[0] = new PlayerState(deck0, rng);
        players[1] = new PlayerState(deck1, rng);
        for (int p = 0; p < 2; p++) {
            placeTower(p, Cards.KING_TOWER, Arena.KING_X, Arena.KING_Y_BOTTOM).active = false;
            placeTower(p, Cards.PRINCESS_TOWER, Arena.LEFT_LANE_X, Arena.PRINCESS_Y_BOTTOM);
            placeTower(p, Cards.PRINCESS_TOWER, Arena.RIGHT_LANE_X, Arena.PRINCESS_Y_BOTTOM);
        }
    }

    private Unit placeTower(int player, String card, int x, int bottomY) {
        int y = player == 0 ? bottomY : Arena.mirrorY(bottomY);
        Unit u = new Unit(nextId++, Cards.get(card), player, x, y);
        units.add(u);
        return u;
    }

    // ------------------------------------------------------------------ queries

    public long seed() { return seed; }
    public int tick() { return tick; }
    public PlayerState player(int p) { return players[p]; }
    /// Live units in creation order. Do not modify.
    public List<Unit> units() { return units; }
    /// What happened during the last [#step]. Do not modify.
    public List<Event> events() { return events; }
    public boolean isOver() { return result != ONGOING; }
    /// The winning player, [#DRAW], or [#ONGOING].
    public int result() { return result; }
    public boolean isOvertime() { return tick >= REGULAR_TICKS; }
    public boolean isDoubleElixir() { return tick >= DOUBLE_ELIXIR_FROM; }

    /// Seconds left in the current period (regular time or overtime).
    public int secondsLeft() {
        int end = tick < REGULAR_TICKS ? REGULAR_TICKS : MAX_TICKS;
        return (end - tick + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND;
    }

    public Unit unit(int id) {
        for (int i = 0; i < units.size(); i++) {
            if (units.get(i).id == id) {
                return units.get(i);
            }
        }
        return null;
    }

    /// True when `player` could play `cardId` at (x, y) right now. The app uses
    /// this to light up the drop zone; the server to reject an intent early. The
    /// same test runs again when the command executes.
    public boolean canPlay(int player, String cardId, int x, int y) {
        if (isOver() || player < 0 || player > 1) {
            return false;
        }
        PlayerState ps = players[player];
        CardDef card = Cards.get(cardId);
        if (card == null || !card.isPlayable() || ps.handSlotOf(cardId) < 0) {
            return false;
        }
        if (ps.elixir < card.cost * PlayerState.ELIXIR_UNIT) {
            return false;
        }
        return isValidDrop(player, card, x, y);
    }

    /// True when (x, y) is inside the area `player` may drop `card` into.
    public boolean isValidDrop(int player, CardDef card, int x, int y) {
        if (x < 0 || x > Arena.WIDTH || y < 0 || y > Arena.HEIGHT) {
            return false;
        }
        if (card.kind == CardDef.SPELL) {
            return true;
        }
        if (Arena.ownHalf(player, y)) {
            return true;
        }
        // The pocket: once a lane's enemy princess tower falls, its side of the
        // enemy half opens up to the river plus POCKET_DEPTH.
        int lane = Arena.laneOf(x);
        if (princessStanding(1 - player, lane)) {
            return false;
        }
        if (player == 0) {
            return y >= Arena.RIVER_TOP - POCKET_DEPTH && y < Arena.RIVER_TOP;
        }
        return y <= Arena.RIVER_BOTTOM + POCKET_DEPTH && y > Arena.RIVER_BOTTOM;
    }

    private boolean princessStanding(int player, int lane) {
        for (int i = 0; i < units.size(); i++) {
            Unit u = units.get(i);
            if (u.owner == player && u.card.id.equals(Cards.PRINCESS_TOWER) && u.lane == lane && u.hp > 0) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ the tick

    /// Advances one tick. `commands` may hold anything; only those stamped with
    /// the current tick are applied, in the order given (the server's order).
    public void step(List<Command> commands) {
        if (isOver()) {
            return;
        }
        events.clear();
        if (commands != null) {
            for (int i = 0; i < commands.size(); i++) {
                Command c = commands.get(i);
                if (c.tick == tick) {
                    apply(c);
                }
            }
        }
        regenElixir();
        int count = units.size();
        for (int i = 0; i < count; i++) {
            Unit u = units.get(i);
            if (u.hp > 0) {
                update(u);
            }
        }
        separate();
        removeDead();
        tick++;
        checkEnd();
    }

    /// Steps until the match ends, feeding commands from `log`.
    public void runToEnd(List<Command> log) {
        while (!isOver()) {
            step(log);
        }
    }

    private void apply(Command c) {
        if (!canPlay(c.player, c.cardId, c.x, c.y)) {
            events.add(new Event(Event.REJECTED, c.player, -1, c.x, c.y, 0));
            return;
        }
        PlayerState ps = players[c.player];
        CardDef card = Cards.get(c.cardId);
        ps.elixir -= card.cost * PlayerState.ELIXIR_UNIT;
        ps.cycle(ps.handSlotOf(c.cardId));
        events.add(new Event(Event.PLAYED, c.player, -1, c.x, c.y, Cards.indexOf(card.id)));
        spawn(card, c.player, c.x, c.y);
    }

    private void spawn(CardDef card, int owner, int x, int y) {
        int n = card.count;
        for (int i = 0; i < n; i++) {
            int ox = 0;
            int oy = 0;
            if (n > 1) {
                // A fixed ring, so a group always lands in the same formation.
                int[] ring = {0, 0, -1, 0, 1, 0, 0, -1, 0, 1, -1, -1, 1, 1};
                int k = (i % 7) * 2;
                ox = ring[k] * card.spread;
                oy = ring[k + 1] * card.spread;
                if (n == 2) {
                    ox = i == 0 ? -card.spread : card.spread;
                    oy = 0;
                }
            }
            int sx = FixedMath.clamp(x + ox, 0, Arena.WIDTH);
            int sy = FixedMath.clamp(y + oy, 0, Arena.HEIGHT);
            Unit u = new Unit(nextId++, card, owner, sx, sy);
            units.add(u);
            events.add(new Event(card.kind == CardDef.SPELL ? Event.SPELL : Event.SPAWN, u.id, -1, sx, sy, 0));
        }
    }

    private void regenElixir() {
        int gain = isDoubleElixir() ? 2 : 1;
        for (int p = 0; p < 2; p++) {
            PlayerState ps = players[p];
            ps.elixir = Math.min(PlayerState.MAX_ELIXIR, ps.elixir + gain);
        }
    }

    private void update(Unit u) {
        if (u.deployTimer > 0) {
            u.deployTimer--;
            if (u.deployTimer == 0 && u.card.kind == CardDef.SPELL) {
                land(u);
            }
            return;
        }
        CardDef c = u.card;
        u.age++;
        if (c.kind == CardDef.BUILDING) {
            if (c.lifetimeTicks > 0) {
                // A building decays to nothing over its lifetime.
                int decay = Math.max(1, c.hp / c.lifetimeTicks);
                damage(u, decay, -1);
                if (u.hp <= 0) {
                    return;
                }
            }
            if (c.spawns != null && c.spawnTicks > 0 && u.age % c.spawnTicks == 0) {
                int dir = u.owner == 0 ? -1 : 1;
                spawn(Cards.get(c.spawns), u.owner, u.x, u.y + dir * (c.radius + 300));
            }
        }
        if (c.damage <= 0 || !u.active) {
            return;
        }
        Unit target = acquire(u);
        if (u.cooldown > 0) {
            u.cooldown--;
        }
        if (target != null && inRange(u, target)) {
            if (u.cooldown == 0) {
                hit(u, target);
                u.cooldown = c.hitTicks;
            }
            return;
        }
        if (c.kind == CardDef.TROOP) {
            move(u, target);
        }
    }

    private boolean inRange(Unit u, Unit t) {
        return FixedMath.within(u.x, u.y, t.x, t.y, u.card.range + u.card.radius + t.card.radius);
    }

    /// Keeps the current target while it lives and stays in reach; otherwise picks
    /// the nearest enemy it may hit. Buildings-only attackers see every building.
    private Unit acquire(Unit u) {
        Unit current = u.targetId >= 0 ? unit(u.targetId) : null;
        if (current != null && current.hp > 0 && inRange(u, current)) {
            return current;
        }
        Unit best = null;
        long bestD = Long.MAX_VALUE;
        long sight = (long) u.card.sight + u.card.radius;
        for (int i = 0; i < units.size(); i++) {
            Unit o = units.get(i);
            if (o.owner == u.owner || o.deployTimer > 0 || !o.hittableBy(u.card)) {
                continue;
            }
            long d = FixedMath.distSq(u.x, u.y, o.x, o.y);
            long reach = sight + o.card.radius;
            if (!u.card.buildingsOnly && d > reach * reach) {
                continue;
            }
            if (d < bestD) {
                bestD = d;
                best = o;
            }
        }
        u.targetId = best == null ? -1 : best.id;
        return best;
    }

    private void hit(Unit u, Unit target) {
        events.add(new Event(Event.ATTACK, u.id, target.id, target.x, target.y, u.card.damage));
        if (u.card.splash > 0) {
            splash(u.owner, u.card, target.x, target.y, u.id, false);
        } else {
            damage(target, u.card.damage, u.id);
        }
    }

    private void land(Unit spell) {
        splash(spell.owner, spell.card, spell.x, spell.y, spell.id, true);
        spell.hp = 0;
    }

    private void splash(int owner, CardDef card, int x, int y, int source, boolean spell) {
        for (int i = 0; i < units.size(); i++) {
            Unit o = units.get(i);
            if (o.owner == owner || o.hp <= 0 || o.deployTimer > 0) {
                continue;
            }
            if (!spell && !o.hittableBy(card)) {
                continue;
            }
            if (spell && o.card.kind == CardDef.SPELL) {
                continue;
            }
            if (!FixedMath.within(x, y, o.x, o.y, card.splash + o.card.radius)) {
                continue;
            }
            int dmg = card.damage;
            if (spell && o.card.kind == CardDef.TOWER) {
                dmg = FixedMath.muldiv(dmg, card.towerPercent, 100);
            }
            damage(o, dmg, source);
        }
    }

    private void damage(Unit u, int amount, int source) {
        if (u.hp <= 0) {
            return;
        }
        u.hp -= amount;
        if (u.card.kind == CardDef.TOWER && u.card.id.equals(Cards.KING_TOWER) && source >= 0) {
            u.active = true;
        }
        if (u.hp <= 0) {
            u.hp = 0;
            events.add(new Event(Event.DEATH, u.id, source, u.x, u.y, 0));
            if (u.card.kind == CardDef.TOWER) {
                towerDown(u);
            }
        }
    }

    private void towerDown(Unit tower) {
        int scorer = 1 - tower.owner;
        events.add(new Event(Event.TOWER_DOWN, tower.id, scorer, tower.x, tower.y, 0));
        if (tower.card.id.equals(Cards.KING_TOWER)) {
            players[scorer].crowns = 3;
            return;
        }
        players[scorer].crowns = Math.min(3, players[scorer].crowns + 1);
        for (int i = 0; i < units.size(); i++) {
            Unit k = units.get(i);
            if (k.owner == tower.owner && k.card.id.equals(Cards.KING_TOWER)) {
                k.active = true;
            }
        }
    }

    // ------------------------------------------------------------------ movement

    private void move(Unit u, Unit target) {
        int tx;
        int ty;
        if (target != null && (u.isFlying() || sameBank(u, target))) {
            tx = target.x;
            ty = target.y;
        } else {
            int[] w = waypoint(u);
            tx = w[0];
            ty = w[1];
        }
        step(u, tx, ty);
    }

    /// Ground units cannot wade: a target across the river is reached by bridge.
    private boolean sameBank(Unit u, Unit t) {
        int su = side(u.y);
        int st = side(t.y);
        return su == st || su == 0 || st == 0;
    }

    /// 1 for the bottom bank, -1 for the top one, 0 for the river band.
    private static int side(int y) {
        if (y >= Arena.RIVER_BOTTOM) {
            return 1;
        }
        if (y <= Arena.RIVER_TOP) {
            return -1;
        }
        return 0;
    }

    /// Where an unengaged unit walks: to its lane's bridge, over it, then at the
    /// nearest enemy tower.
    private int[] waypoint(Unit u) {
        if (!u.isFlying() && !Arena.crossed(u.owner, u.y)) {
            int lx = Arena.laneX(Arena.laneOf(u.x));
            int nearBank = u.owner == 0 ? Arena.RIVER_BOTTOM : Arena.RIVER_TOP;
            int farBank = u.owner == 0 ? Arena.RIVER_TOP - 1 : Arena.RIVER_BOTTOM + 1;
            if (Math.abs(u.x - lx) > 150 && Arena.ownHalf(u.owner, u.y)) {
                return new int[] {lx, nearBank};
            }
            return new int[] {lx, farBank};
        }
        Unit best = null;
        long bestD = Long.MAX_VALUE;
        for (int i = 0; i < units.size(); i++) {
            Unit o = units.get(i);
            if (o.owner != u.owner && o.card.kind == CardDef.TOWER && o.hp > 0) {
                long d = FixedMath.distSq(u.x, u.y, o.x, o.y);
                if (d < bestD) {
                    bestD = d;
                    best = o;
                }
            }
        }
        if (best == null) {
            return new int[] {u.x, u.y};
        }
        return new int[] {best.x, best.y};
    }

    private void step(Unit u, int tx, int ty) {
        long dx = (long) tx - u.x;
        long dy = (long) ty - u.y;
        long dist = FixedMath.isqrt(dx * dx + dy * dy);
        int speed = u.card.speed;
        if (dist == 0) {
            return;
        }
        if (dist <= speed) {
            u.x = tx;
            u.y = ty;
        } else {
            u.x += (int) (dx * speed / dist);
            u.y += (int) (dy * speed / dist);
        }
        u.lane = Arena.laneOf(u.x);
    }

    /// Pushes overlapping troops apart so a group spreads instead of stacking.
    /// Structures never move; troops are pushed out of them.
    private void separate() {
        int n = units.size();
        for (int i = 0; i < n; i++) {
            Unit a = units.get(i);
            if (a.hp <= 0 || a.card.kind != CardDef.TROOP || a.deployTimer > 0) {
                continue;
            }
            for (int j = 0; j < n; j++) {
                Unit b = units.get(j);
                if (i == j || b.hp <= 0 || b.card.kind == CardDef.SPELL || b.deployTimer > 0
                        || a.isFlying() != b.isFlying()) {
                    continue;
                }
                int minD = a.card.radius + b.card.radius;
                long d2 = FixedMath.distSq(a.x, a.y, b.x, b.y);
                if (d2 >= (long) minD * minD) {
                    continue;
                }
                long d = FixedMath.isqrt(d2);
                int overlap = (int) (minD - d);
                int share = b.card.kind == CardDef.TROOP ? overlap / 2 : overlap;
                if (d == 0) {
                    a.x += a.id < b.id ? -share : share;
                } else {
                    a.x += (int) ((a.x - (long) b.x) * share / d);
                    a.y += (int) ((a.y - (long) b.y) * share / d);
                }
                a.x = FixedMath.clamp(a.x, 0, Arena.WIDTH);
                a.y = FixedMath.clamp(a.y, 0, Arena.HEIGHT);
            }
        }
    }

    private void removeDead() {
        for (int i = units.size() - 1; i >= 0; i--) {
            if (units.get(i).hp <= 0) {
                units.remove(i);
            }
        }
    }

    private void checkEnd() {
        int c0 = players[0].crowns;
        int c1 = players[1].crowns;
        if (c0 >= 3 || c1 >= 3) {
            result = c0 > c1 ? 0 : 1;
            return;
        }
        if (tick == REGULAR_TICKS && c0 != c1) {
            result = c0 > c1 ? 0 : 1;
            return;
        }
        if (tick > REGULAR_TICKS && c0 != c1) {
            // Sudden death: the first crown in overtime decides it.
            result = c0 > c1 ? 0 : 1;
            return;
        }
        if (tick >= MAX_TICKS) {
            result = DRAW;
        }
    }

    // ------------------------------------------------------------------ hashing

    /// FNV-1a over everything that influences the future of the match.
    public long stateHash() {
        long h = 0xcbf29ce484222325L;
        h = mix(h, tick);
        h = mix(h, result);
        h = mix(h, rng.state());
        h = mix(h, nextId);
        for (int p = 0; p < 2; p++) {
            PlayerState ps = players[p];
            h = mix(h, ps.elixir);
            h = mix(h, ps.crowns);
            for (int i = 0; i < ps.order.length; i++) {
                h = mix(h, Cards.indexOf(ps.order[i]));
            }
        }
        for (int i = 0; i < units.size(); i++) {
            Unit u = units.get(i);
            h = mix(h, u.id);
            h = mix(h, Cards.indexOf(u.card.id));
            h = mix(h, u.owner);
            h = mix(h, u.x);
            h = mix(h, u.y);
            h = mix(h, u.hp);
            h = mix(h, u.cooldown);
            h = mix(h, u.targetId);
            h = mix(h, u.deployTimer);
            h = mix(h, u.age);
            h = mix(h, u.active ? 1 : 0);
        }
        return h;
    }

    private static long mix(long h, long v) {
        for (int i = 0; i < 8; i++) {
            h ^= (v >>> (i * 8)) & 0xff;
            h *= 0x100000001b3L;
        }
        return h;
    }
}
