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

/// Everything about a match that is not the two decks and the seed: the arena,
/// the cards, the clock, the elixir, what wins and any troops a mission sends on
/// a schedule. `CLASSIC` is ranked play; a mission built in the Forge makes its own.
///
/// Rules are immutable and part of the simulation: two copies of a match agree
/// only if they were started with equal rules, which is why a replay of a mission
/// records the content pack and its hash.
public final class MatchRules {
    /// Three minutes, then a minute of sudden death; the most crowns wins.
    public static final int CROWNS = 0;
    /// Player 0 wins by keeping their king standing until the clock runs out.
    public static final int DEFEND = 1;
    /// Player 0 must win `objectiveCount` crowns before the clock runs out.
    public static final int RAID = 2;

    public static final int MIN_TICKS = 30 * Match.TICKS_PER_SECOND;
    public static final int MAX_TICKS = 10 * 60 * Match.TICKS_PER_SECOND;
    public static final int MAX_WAVES = 400;

    public static final MatchRules CLASSIC = new Builder().build();

    final Arena arena;
    final CardSet cards;
    final int regularTicks;
    final int overtimeTicks;
    final int doubleElixirFrom;
    final int[] startElixir;
    final int[] elixirPercent;
    final int objective;
    final int objectiveCount;
    final boolean kingsAsleep;
    final List<Wave> waves;

    /// A troop, building or spell a mission plays for a side at a fixed tick,
    /// without a hand or elixir. Waves are how a scripted enemy attacks.
    public static final class Wave {
        public final int tick;
        public final int player;
        public final String card;
        public final int x;
        public final int y;

        public Wave(int tick, int player, String card, int x, int y) {
            this.tick = tick;
            this.player = player;
            this.card = card;
            this.x = x;
            this.y = y;
        }
    }

    private MatchRules(Builder b) {
        arena = b.arena;
        cards = b.cards;
        regularTicks = b.regularTicks;
        overtimeTicks = b.objective == CROWNS ? b.overtimeTicks : 0;
        doubleElixirFrom = b.doubleElixirFrom;
        startElixir = new int[] {b.startElixir[0], b.startElixir[1]};
        elixirPercent = new int[] {b.elixirPercent[0], b.elixirPercent[1]};
        objective = b.objective;
        objectiveCount = b.objectiveCount;
        kingsAsleep = b.kingsAsleep;
        List<Wave> sorted = new ArrayList<Wave>(b.waves);
        // Stable by tick, so two waves on one tick keep the order they were authored in.
        for (int i = 1; i < sorted.size(); i++) {
            Wave w = sorted.get(i);
            int j = i - 1;
            while (j >= 0 && sorted.get(j).tick > w.tick) {
                sorted.set(j + 1, sorted.get(j));
                j--;
            }
            sorted.set(j + 1, w);
        }
        waves = Collections.unmodifiableList(sorted);
    }

    public Arena arena() { return arena; }
    public CardSet cards() { return cards; }
    public int regularTicks() { return regularTicks; }
    public int overtimeTicks() { return overtimeTicks; }
    public int maxTicks() { return regularTicks + overtimeTicks; }
    public int doubleElixirFrom() { return doubleElixirFrom; }
    public int startElixir(int player) { return startElixir[player]; }
    public int elixirPercent(int player) { return elixirPercent[player]; }
    public int objective() { return objective; }
    public int objectiveCount() { return objectiveCount; }
    public boolean kingsAsleep() { return kingsAsleep; }
    public List<Wave> waves() { return waves; }

    public boolean isClassic() {
        return this == CLASSIC;
    }

    public Builder toBuilder() {
        Builder b = new Builder();
        b.arena = arena;
        b.cards = cards;
        b.regularTicks = regularTicks;
        b.overtimeTicks = overtimeTicks;
        b.doubleElixirFrom = doubleElixirFrom;
        b.startElixir = new int[] {startElixir[0], startElixir[1]};
        b.elixirPercent = new int[] {elixirPercent[0], elixirPercent[1]};
        b.objective = objective;
        b.objectiveCount = objectiveCount;
        b.kingsAsleep = kingsAsleep;
        b.waves = new ArrayList<Wave>(waves);
        return b;
    }

    long hash(long h) {
        long v = arena.hash(h);
        v = Match.mixValue(v, cards.all().size());
        v = Match.mixValue(v, regularTicks);
        v = Match.mixValue(v, overtimeTicks);
        v = Match.mixValue(v, doubleElixirFrom);
        for (int p = 0; p < 2; p++) {
            v = Match.mixValue(v, startElixir[p]);
            v = Match.mixValue(v, elixirPercent[p]);
        }
        v = Match.mixValue(v, objective);
        v = Match.mixValue(v, objectiveCount);
        v = Match.mixValue(v, kingsAsleep ? 1 : 0);
        v = Match.mixValue(v, waves.size());
        return v;
    }

    public static final class Builder {
        Arena arena = Arena.CLASSIC;
        CardSet cards = CardSet.BUILTIN;
        int regularTicks = 180 * Match.TICKS_PER_SECOND;
        int overtimeTicks = 60 * Match.TICKS_PER_SECOND;
        int doubleElixirFrom = 120 * Match.TICKS_PER_SECOND;
        int[] startElixir = {PlayerState.START_ELIXIR, PlayerState.START_ELIXIR};
        int[] elixirPercent = {100, 100};
        int objective = CROWNS;
        int objectiveCount = 3;
        boolean kingsAsleep = true;
        List<Wave> waves = new ArrayList<Wave>();

        public Builder arena(Arena v) { arena = v; return this; }
        public Builder cards(CardSet v) { cards = v; return this; }
        public Builder regularTicks(int v) { regularTicks = v; return this; }
        public Builder overtimeTicks(int v) { overtimeTicks = v; return this; }
        public Builder doubleElixirFrom(int v) { doubleElixirFrom = v; return this; }
        /// Elixir at the start, in whole drops.
        public Builder startElixir(int player, int drops) { startElixir[player] = drops * PlayerState.ELIXIR_UNIT; return this; }
        /// How fast a side's elixir refills, 100 being normal.
        public Builder elixirPercent(int player, int v) { elixirPercent[player] = v; return this; }
        public Builder objective(int kind, int count) { objective = kind; objectiveCount = count; return this; }
        public Builder kingsAsleep(boolean v) { kingsAsleep = v; return this; }
        public Builder wave(Wave w) { waves.add(w); return this; }
        public Builder waves(List<Wave> w) { waves = new ArrayList<Wave>(w); return this; }

        public MatchRules build() {
            return new MatchRules(this);
        }
    }
}
