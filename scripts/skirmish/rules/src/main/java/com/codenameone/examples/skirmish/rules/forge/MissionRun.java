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

import com.codenameone.examples.skirmish.rules.Bot;
import com.codenameone.examples.skirmish.rules.CardSet;
import com.codenameone.examples.skirmish.rules.Command;
import com.codenameone.examples.skirmish.rules.Match;
import com.codenameone.examples.skirmish.rules.Replay;
import com.codenameone.examples.skirmish.rules.RulesVersion;

import java.util.ArrayList;
import java.util.List;

/// One play of a mission: the match, and the enemy bot that plays side 1.
///
/// The app plays a mission through this class and the server re-plays the result
/// through it, which is why the server can trust a mission result without having
/// watched the battle: the player's own commands are all the app sends, and the
/// enemy's moves are regenerated here from the seed. A result the server cannot
/// reproduce pays nothing.
public final class MissionRun {
    /// Ticks between a bot's decision and its card landing, as in practice battles.
    public static final int BOT_DELAY = 1;

    private final MissionDef mission;
    private final Match match;
    private final Bot bot;
    private final List<Command> log = new ArrayList<Command>();
    private final String content;
    private final String[] deck0;

    public MissionRun(ContentPack pack, String missionId, long seed, String[] ownDeck) {
        this.mission = pack.mission(missionId);
        if (mission == null) {
            throw new IllegalArgumentException("no mission " + missionId);
        }
        this.deck0 = mission.playerDeck != null ? mission.playerDeck : ownDeck;
        this.match = new Match(seed, deck0, mission.enemyDeck, pack.rulesFor(missionId));
        this.bot = mission.enemyDeck == null ? null : new Bot(1, seed ^ 0x5DEECE66DL, mission.botThinkTicks);
        this.content = new ContentRef(pack.id, pack.hash(), missionId).toString();
    }

    public Match match() { return match; }
    public MissionDef mission() { return mission; }
    public List<Command> log() { return log; }

    /// Records the player's command; it applies on its tick like any other.
    public void play(Command c) {
        if (c.player == 0) {
            log.add(c);
        }
    }

    /// Lets the bot think, then advances the match one tick.
    public void step() {
        if (bot != null && !match.isOver()) {
            Command c = bot.decide(match, BOT_DELAY);
            if (c != null) {
                log.add(c);
            }
        }
        match.step(log);
    }

    public int stars() {
        return match.isOver() ? mission.stars(match) : 0;
    }

    /// The replay the app sends with its result. The enemy's commands ride along so
    /// a viewer can play it back without the bot.
    public Replay replay() {
        return new Replay(match.seed(), deck0, mission.enemyDeck, log, RulesVersion.ID, content, match.rules());
    }

    /// Plays `claimed` again with the server's own bot, ignoring every command the
    /// claim made for side 1, and returns the finished match.
    public static Match verify(ContentPack pack, String missionId, Replay claimed) {
        MissionDef m = pack.mission(missionId);
        if (m == null) {
            throw new IllegalArgumentException("no such mission");
        }
        String[] deck0 = claimed.deck(0);
        if (m.playerDeck != null ? !same(m.playerDeck, deck0) : !pack.cardSet().isValidDeck(deck0)) {
            throw new IllegalArgumentException("the player's deck does not fit the mission");
        }
        if (!same(m.enemyDeck, claimed.deck(1))) {
            throw new IllegalArgumentException("the enemy's deck does not match the mission");
        }
        CardSet cards = pack.cardSet();
        MissionRun run = new MissionRun(pack, missionId, claimed.seed(), deck0);
        List<Command> mine = claimed.commands();
        for (int i = 0; i < mine.size(); i++) {
            Command c = mine.get(i);
            if (c.player == 0 && cards.get(c.cardId) != null) {
                run.play(c);
            }
        }
        while (!run.match.isOver()) {
            run.step();
        }
        return run.match;
    }

    private static boolean same(String[] a, String[] b) {
        if (a == null || b == null) {
            return a == b;
        }
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (!a[i].equals(b[i])) {
                return false;
            }
        }
        return true;
    }
}
