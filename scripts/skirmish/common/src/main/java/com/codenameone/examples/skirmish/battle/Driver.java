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

import com.codenameone.examples.skirmish.rules.Command;
import com.codenameone.examples.skirmish.rules.Match;
import java.util.ArrayList;
import java.util.List;

/// Where a battle's ticks come from. The renderer asks whether it may step,
/// steps, and draws; a driver decides which commands the step sees.
///
/// - [PracticeDriver]: offline, against the rules' own bot, every tick at once.
/// - [OnlineDriver]: lockstep with the server, never past the tick it allows.
/// - [ReplayDriver]: a finished match's log, at any speed, seekable.
///
/// All three run the same [Match] -- the class the server runs -- which is why
/// a unit changed in the rules module is changed in every one of them.
// tag::skirmish-driver[]
public abstract class Driver {
    protected Match match;
    protected final List<Command> log = new ArrayList<Command>();
    /// The seat drawn at the bottom of the screen.
    protected int me;

    public Match match() {
        return match;
    }

    public int me() {
        return me;
    }

    public String name(int seat) {
        return seat == me ? "You" : "Opponent";
    }

    /// Whether the next tick may be simulated now.
    public abstract boolean canStep();

    /// Simulates one tick.
    public void step() {
        match.step(log);
    }

    /// The player asks to play `card` at (x, y). The driver decides when -- or
    /// whether -- it happens.
    public abstract void play(String card, int x, int y);

    /// Whether the player at the bottom can play cards at all (not in a replay).
    public boolean interactive() {
        return true;
    }

    /// How many ticks to simulate per tick of wall time; the replay viewer
    /// speeds up and slows down.
    public int speedPercent() {
        return 100;
    }

    /// Called once when the match is over.
    public void finished() {
    }

    public void dispose() {
    }
}
// end::skirmish-driver[]
