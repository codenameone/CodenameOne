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
package com.codenameone.examples.skirmish.battle;

import com.codenameone.examples.skirmish.rules.Arena;
import com.codenameone.examples.skirmish.rules.Bot;
import com.codenameone.examples.skirmish.rules.CardDef;
import com.codenameone.examples.skirmish.rules.Cards;
import com.codenameone.examples.skirmish.rules.Command;
import com.codenameone.examples.skirmish.rules.Deck;
import com.codenameone.examples.skirmish.rules.Match;

/// The training battle: an ordinary match against the bot, except that the
/// lesson can hold time still while it explains, and the opponent only moves
/// when the lesson says so until the player is let loose.
///
/// The seed is chosen so that the opening hand holds the three cards the
/// lesson teaches with; the rules are the real ones.
public final class TutorialDriver extends Driver {
    /// The cards the lesson asks the player to play, in order.
    public static final String TROOP = "squire";
    public static final String RANGED = "twin-archers";
    public static final String SPELL = "fire-orb";

    private final long seed;
    private Bot bot;
    private boolean paused = true;

    public TutorialDriver() {
        this.me = 0;
        this.seed = openingSeed();
        this.match = new Match(seed, Deck.starter(), Deck.starter());
    }

    /// The first seed whose opening hand has every card the lesson needs.
    static long openingSeed() {
        for (long s = 1; s < 10000; s++) {
            Match m = new Match(s, Deck.starter(), Deck.starter());
            if (inHand(m, TROOP) && inHand(m, RANGED) && inHand(m, SPELL)) {
                return s;
            }
        }
        return 1;
    }

    private static boolean inHand(Match m, String card) {
        for (int i = 0; i < Deck.HAND; i++) {
            if (card.equals(m.player(0).hand(i))) {
                return true;
            }
        }
        return false;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public boolean isPaused() {
        return paused;
    }

    /// The lesson is over: from here the opponent plays, slowly.
    public void unleash() {
        paused = false;
        if (bot == null) {
            bot = new Bot(1, seed, 90);
        }
    }

    /// The opponent sends a troop down the left lane, so the lesson has
    /// something to defend against.
    public void enemyAttack() {
        int x = Arena.CLASSIC.laneX(0);
        int y = Arena.CLASSIC.mirrorY(21 * Arena.TILE);
        for (int i = 0; i < Deck.HAND; i++) {
            String card = match.player(1).hand(i);
            CardDef def = Cards.get(card);
            if (def != null && def.kind == CardDef.TROOP && !def.isFlying() && match.canPlay(1, card, x, y)) {
                log.add(new Command(match.tick() + 1, 1, card, x, y));
                return;
            }
        }
    }

    @Override
    public boolean canStep() {
        return !paused && !match.isOver();
    }

    @Override
    public void step() {
        if (bot != null) {
            Command c = bot.decide(match, 1);
            if (c != null) {
                log.add(c);
            }
        }
        super.step();
    }

    @Override
    public void play(String card, int x, int y) {
        if (match.canPlay(0, card, x, y)) {
            log.add(new Command(match.tick() + 1, 0, card, x, y));
        }
    }

    @Override
    public String name(int seat) {
        return seat == 0 ? "You" : "Training Dummy";
    }
}
