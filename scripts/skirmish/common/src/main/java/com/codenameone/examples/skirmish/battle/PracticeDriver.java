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

import com.codenameone.examples.skirmish.rules.Bot;
import com.codenameone.examples.skirmish.rules.Command;
import com.codenameone.examples.skirmish.rules.Deck;
import com.codenameone.examples.skirmish.rules.Match;
import com.codenameone.examples.skirmish.rules.Replay;

/// Offline practice: the whole match runs on the device, against [Bot], the
/// same bot the server seats when nobody else is queued. No network at all.
public final class PracticeDriver extends Driver {
    private final Bot bot;
    private final long seed;
    private final String[] deck;

    public PracticeDriver(long seed, String[] deck) {
        this.seed = seed;
        this.deck = deck;
        this.me = 0;
        this.match = new Match(seed, deck, Deck.starter());
        this.bot = new Bot(1, seed ^ 0x5DEECE66DL);
    }

    @Override
    public boolean canStep() {
        return !match.isOver();
    }

    @Override
    public void step() {
        Command c = bot.decide(match, 1);
        if (c != null) {
            log.add(c);
        }
        super.step();
    }

    @Override
    public void play(String card, int x, int y) {
        // A tick ahead, like the server's input delay but shorter: the play
        // still goes through the log, so practice can be saved as a replay.
        if (match.canPlay(0, card, x, y)) {
            log.add(new Command(match.tick() + 1, 0, card, x, y));
        }
    }

    @Override
    public String name(int seat) {
        return seat == 0 ? "You" : "Training Bot";
    }

    /// The practice match as a replay, playable in the viewer.
    public String replay() {
        return new Replay(seed, deck, Deck.starter(), log).encode();
    }
}
