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
package com.codenameone.examples.skirmish.rules.forge;

import com.codenameone.examples.skirmish.rules.Arena;
import com.codenameone.examples.skirmish.rules.CardDef;
import com.codenameone.examples.skirmish.rules.CardSet;
import com.codenameone.examples.skirmish.rules.Match;
import com.codenameone.examples.skirmish.rules.MatchRules;
import com.codenameone.examples.skirmish.rules.Unit;

import java.util.ArrayList;
import java.util.List;

/// A battle someone designed: the field, both sides' cards, the enemy's plan, what
/// wins, the script and the stars.
///
/// The player is always side 0, at the bottom. The enemy is side 1 and can play a
/// deck through the bot, send scripted waves, or both.
public final class MissionDef {
    public String id = "";
    public String name = "";
    public String briefing = "";
    /// An arena of the same pack, or "classic".
    public String arena = "classic";

    /// The player's eight cards, or null to let them bring their own deck.
    public String[] playerDeck;
    /// The enemy's eight cards for the bot, or null for an enemy of waves only.
    public String[] enemyDeck;
    /// How often the bot thinks, in ticks: lower is harder.
    public int botThinkTicks = 12;

    public int seconds = 180;
    public int overtimeSeconds = 60;
    public int doubleElixirFromSecond = 120;
    public int playerStartElixir = 5;
    public int enemyStartElixir = 5;
    public int playerElixirPercent = 100;
    public int enemyElixirPercent = 100;
    public int objective = MatchRules.CROWNS;
    public int objectiveCount = 3;
    public boolean kingsAsleep = true;

    public List<MatchRules.Wave> waves = new ArrayList<MatchRules.Wave>();
    public List<DialogStep> dialog = new ArrayList<DialogStep>();

    /// Two stars for winning with at least this many of your towers standing.
    public int twoStarTowers = 2;
    /// Three stars for winning in at most this many seconds (0: two-star rule plus no
    /// tower lost).
    public int threeStarSeconds;

    /// What the first win of each star pays, if the server honours it -- see the
    /// server's reward caps for packs that are not official.
    public int rewardCoins = 50;
    public int rewardXp = 20;

    /// The rules this mission's battles are played under, given the pack's cards
    /// and the arena it names.
    public MatchRules toRules(CardSet cards, Arena field) {
        int tps = Match.TICKS_PER_SECOND;
        return new MatchRules.Builder()
                .arena(field)
                .cards(cards)
                .regularTicks(seconds * tps)
                .overtimeTicks(overtimeSeconds * tps)
                .doubleElixirFrom(doubleElixirFromSecond * tps)
                .startElixir(0, playerStartElixir)
                .startElixir(1, enemyStartElixir)
                .elixirPercent(0, playerElixirPercent)
                .elixirPercent(1, enemyElixirPercent)
                .objective(objective, objectiveCount)
                .kingsAsleep(kingsAsleep)
                .waves(waves)
                .build();
    }

    /// Stars for a finished battle: 0 for a loss.
    public int stars(Match m) {
        if (m.result() != 0) {
            return 0;
        }
        int total = 0;
        for (int i = 0; i < m.arena().towers().size(); i++) {
            if (m.arena().towers().get(i).owner == 0) {
                total++;
            }
        }
        int standing = 0;
        for (int i = 0; i < m.units().size(); i++) {
            Unit u = m.units().get(i);
            if (u.owner() == 0 && u.card().kind == CardDef.TOWER) {
                standing++;
            }
        }
        if (standing < twoStarTowers) {
            return 1;
        }
        boolean third = threeStarSeconds > 0
                ? m.tick() <= threeStarSeconds * Match.TICKS_PER_SECOND
                : standing == total;
        return third ? 3 : 2;
    }
}
