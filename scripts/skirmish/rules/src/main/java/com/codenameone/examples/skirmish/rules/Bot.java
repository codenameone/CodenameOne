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

/// A simple opponent for offline practice and for automated matches.
///
/// It lives in the rules, not the app, so tests can play thousands of bot-versus-
/// bot matches headless, and so it is deterministic: its choices come from its own
/// seeded [Rng] and the match state, never from a clock.
public final class Bot {
    private final int player;
    private final Rng rng;
    private final int thinkEvery;
    private int saveUpTo = -1;
    private int pushLane = -1;

    public Bot(int player, long seed) {
        this(player, seed, 10);
    }

    /// `thinkEvery` is how many ticks it waits between decisions; a larger value
    /// is an easier bot.
    public Bot(int player, long seed, int thinkEvery) {
        this.player = player;
        this.rng = new Rng(seed ^ 0x5DEECE66DL);
        this.thinkEvery = Math.max(1, thinkEvery);
    }

    public int player() {
        return player;
    }

    /// The command to play on the current tick, or null. The command is stamped
    /// with `match.tick() + delay`.
    public Command decide(Match match, int delay) {
        if (match.isOver() || match.tick() % thinkEvery != 0) {
            return null;
        }
        PlayerState ps = match.player(player);
        if (saveUpTo < 0) {
            saveUpTo = 4 + rng.nextInt(5);
        }
        Unit threat = biggestThreat(match);
        if (threat == null && ps.wholeElixir() < saveUpTo) {
            return null;
        }
        int slot = rng.nextInt(Deck.HAND);
        for (int i = 0; i < Deck.HAND; i++) {
            String id = ps.hand((slot + i) % Deck.HAND);
            CardDef card = Cards.get(id);
            int[] at = placement(match, card, threat);
            if (at != null && match.canPlay(player, id, at[0], at[1])) {
                saveUpTo = -1;
                return new Command(match.tick() + delay, player, id, at[0], at[1]);
            }
        }
        return null;
    }

    private Unit biggestThreat(Match match) {
        Unit best = null;
        for (int i = 0; i < match.units().size(); i++) {
            Unit u = match.units().get(i);
            if (u.owner() != player && u.card().kind == CardDef.TROOP
                    && Arena.ownHalf(player, u.y()) && (best == null || u.hp() > best.hp())) {
                best = u;
            }
        }
        return best;
    }

    private int[] placement(Match match, CardDef card, Unit threat) {
        if (card.kind == CardDef.SPELL) {
            Unit t = threat != null ? threat : weakestEnemyTower(match);
            return t == null ? null : new int[] {t.x(), t.y()};
        }
        if (pushLane < 0 || rng.nextInt(6) == 0) {
            // Commit to a lane for a while: cards played together make a push.
            pushLane = rng.nextInt(2);
        }
        int lane = threat != null ? Arena.laneOf(threat.x()) : pushLane;
        int x = Arena.laneX(lane) + (rng.nextInt(3) - 1) * Arena.TILE;
        int depth = threat != null ? 25 : 21 + rng.nextInt(7);
        int y = depth * Arena.TILE;
        if (player == 1) {
            y = Arena.mirrorY(y);
        }
        return new int[] {x, y};
    }

    private Unit weakestEnemyTower(Match match) {
        Unit best = null;
        for (int i = 0; i < match.units().size(); i++) {
            Unit u = match.units().get(i);
            if (u.owner() != player && u.card().kind == CardDef.TOWER && (best == null || u.hp() < best.hp())) {
                best = u;
            }
        }
        return best;
    }
}
