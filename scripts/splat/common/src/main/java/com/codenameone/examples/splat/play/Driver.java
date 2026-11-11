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
package com.codenameone.examples.splat.play;

import com.codenameone.examples.splat.rules.Event;
import com.codenameone.examples.splat.rules.Input;
import com.codenameone.examples.splat.rules.Match;
import java.util.ArrayList;
import java.util.List;

/// Where a match's ticks come from. The view asks whether it may step, hands
/// over the player's input, steps and draws; the driver decides what the
/// step means.
///
/// - [PracticeDriver]: offline against the rules' bots.
/// - [OnlineDriver]: the server decides, the phone predicts.
/// - [ReplayDriver]: a recorded match, at any speed.
///
/// All three step the same [Match] the server runs, which is why a rule
/// changed in the rules module is changed in every one of them.
// tag::splat-driver[]
public abstract class Driver {
    protected Match match;
    /// The player the camera looks out of.
    protected int me;
    protected String[] names = new String[0];
    private final List<Event> events = new ArrayList<Event>();

    public Match match() {
        return match;
    }

    public int me() {
        return me;
    }

    public String name(int player) {
        if (interactive() && player == me) {
            return "You";
        }
        return player < names.length ? names[player] : "Player " + (player + 1);
    }

    /// Whether the next tick may be simulated now.
    public abstract boolean canStep();

    /// Simulates one tick with `in` as the player's input (ignored when not
    /// [#interactive()]).
    public abstract void step(Input in);

    /// What happened since the view last asked: splats, PAINTED, captures.
    public List<Event> drainEvents() {
        List<Event> out = new ArrayList<Event>(events);
        events.clear();
        return out;
    }

    protected void emit(List<Event> happened) {
        events.addAll(happened);
    }

    protected void emit(Event e) {
        events.add(e);
    }

    public boolean interactive() {
        return true;
    }

    public int speedPercent() {
        return 100;
    }

    /// Ticks still to wait before the first step: the briefing.
    public int countdownTicks() {
        return 0;
    }

    public void dispose() {
    }
}
// end::splat-driver[]
