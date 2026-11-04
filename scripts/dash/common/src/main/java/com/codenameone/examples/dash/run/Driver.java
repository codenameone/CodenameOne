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
package com.codenameone.examples.dash.run;

import com.codenameone.examples.dash.rules.Run;

/// Where a run's ticks come from. The view asks whether it may step, steps,
/// and draws; a driver decides which inputs the step sees.
///
/// - [SoloDriver]: offline, every tick at once, recorded for the leaderboard.
/// - [RaceDriver]: lockstep with the server, never past the tick it allows.
/// - [ReplayDriver]: a finished run's log, at any speed, seekable.
///
/// All three step the same [Run] -- the class the server runs -- which is why
/// a rule changed in the rules module is changed in every one of them.
// tag::dash-driver[]
public abstract class Driver {
    protected Run run;
    /// The runner the camera follows.
    protected int me;

    public Run run() {
        return run;
    }

    public int me() {
        return me;
    }

    public String name(int runner) {
        return runner == me ? "You" : "Runner " + (runner + 1);
    }

    /// Whether the next tick may be simulated now.
    public abstract boolean canStep();

    /// Simulates one tick.
    public abstract void step();

    /// The player pressed `bits` ([com.codenameone.examples.dash.rules.Input]).
    /// The driver decides on which tick -- or whether -- it lands.
    public abstract void input(int bits);

    /// Ghost runs drawn beside the player's, which never touch the player's
    /// run: a leaderboard ghost or a bot to race offline. Empty by default.
    public Run[] ghosts() {
        return new Run[0];
    }

    /// Whether the player steers at all (not in a replay).
    public boolean interactive() {
        return true;
    }

    /// How many ticks to simulate per tick of wall time; the replay viewer
    /// speeds up and slows down.
    public int speedPercent() {
        return 100;
    }

    /// Ticks still to wait before the first step: a race's countdown.
    public int countdownTicks() {
        return 0;
    }

    public void dispose() {
    }
}
// end::dash-driver[]
