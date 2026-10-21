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

import com.codenameone.examples.skirmish.rules.Match;
import com.codenameone.examples.skirmish.rules.Replay;

/// A finished match, re-simulated from its seed, decks and plays. Nothing was
/// recorded frame by frame: the rules are deterministic, so running them again
/// on any device draws exactly the match that was played.
///
/// Seeking backwards starts over from tick 0 and runs to the target without
/// drawing, which takes milliseconds for a whole match.
public final class ReplayDriver extends Driver {
    private final Replay replay;
    private final String[] names;
    private int speed = 100;
    private boolean paused;
    private int length = -1;

    public ReplayDriver(Replay replay, String name0, String name1, int bottomSeat) {
        this.replay = replay;
        this.names = new String[] {name0, name1};
        this.me = bottomSeat;
        this.log.addAll(replay.commands());
        this.match = replay.start();
    }

    @Override
    public boolean canStep() {
        return !paused && !match.isOver();
    }

    @Override
    public void play(String card, int x, int y) {
        // A replay is watched, not played.
    }

    @Override
    public boolean interactive() {
        return false;
    }

    @Override
    public int speedPercent() {
        return speed;
    }

    public void setSpeedPercent(int percent) {
        speed = Math.max(25, Math.min(800, percent));
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    /// How many ticks the match lasted, worked out once by running it.
    public int length() {
        if (length < 0) {
            Match probe = replay.start();
            probe.runToEnd(log);
            length = probe.tick();
        }
        return length;
    }

    /// Jumps to `tick`, re-simulating from the start when it is behind us.
    public void seek(int tick) {
        if (tick < match.tick()) {
            match = replay.start();
        }
        while (match.tick() < tick && !match.isOver()) {
            match.step(log);
        }
    }

    @Override
    public String name(int seat) {
        return names[seat];
    }
}
