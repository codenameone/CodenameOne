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

import com.codenameone.examples.dash.rules.Replay;
import com.codenameone.examples.dash.rules.Run;

/// Plays a finished run or race back from its log, at any speed. Seeking
/// backwards plays it again from the start: the run is deterministic, so the
/// replay is the log and nothing else.
public final class ReplayDriver extends Driver {
    private final Levels.Playable level;
    private final Replay replay;
    private final String[] names;
    private ReplayFeed feed;
    private final int length;
    private int speed = 100;
    private boolean paused;

    public ReplayDriver(Levels.Playable level, Replay replay, String[] names, int follow) {
        this.level = level;
        this.replay = replay;
        this.names = names == null ? new String[0] : names;
        this.me = Math.max(0, Math.min(follow, replay.runners() - 1));
        restart();
        while (!run.over()) {
            feed.step(run);
        }
        length = run.tick();
        restart();
    }

    /// How many ticks the whole replay lasts.
    public int length() {
        return length;
    }

    private void restart() {
        run = new Run(level.level, level.catalog, replay.seed(), replay.runners());
        feed = new ReplayFeed(replay);
    }

    @Override
    public boolean canStep() {
        return !paused && !run.over();
    }

    @Override
    public void step() {
        feed.step(run);
    }

    @Override
    public void input(int bits) {
        // A replay is not steered.
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

    public void setPaused(boolean p) {
        paused = p;
    }

    public boolean isPaused() {
        return paused;
    }

    /// Jumps to `tick`, re-simulating from the start when it lies behind.
    public void seek(int tick) {
        if (tick < run.tick()) {
            restart();
        }
        while (run.tick() < tick && !run.over()) {
            feed.step(run);
        }
    }

    /// Follows another runner.
    public void follow(int runner) {
        me = Math.max(0, Math.min(runner, run.runnerCount() - 1));
    }

    @Override
    public String name(int runner) {
        return runner < names.length && names[runner] != null && names[runner].length() > 0
                ? names[runner] : super.name(runner);
    }
}
