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

import com.codenameone.examples.splat.rules.Input;
import com.codenameone.examples.splat.rules.Replay;
import com.codenameone.examples.splat.rules.Slot;

/// A recorded match, simulated again from its seed and its human inputs. The
/// bots are not in the file at all: they think inside the rules, so they make the
/// same choices again. The camera can follow anyone.
public final class ReplayDriver extends Driver {
    private final Replay replay;
    private Replay.Cursor cursor;
    private int speed = 100;
    private boolean paused;

    public ReplayDriver(Replay replay, int follow) {
        this.replay = replay;
        names = new String[replay.slots().size()];
        for (int i = 0; i < names.length; i++) {
            Slot s = replay.slots().get(i);
            names[i] = s.name;
        }
        restart();
        me = Math.max(0, Math.min(follow, names.length - 1));
    }

    /// Back to the first tick.
    public void restart() {
        cursor = replay.cursor();
        match = cursor.match();
    }

    public void follow(int player) {
        me = ((player % names.length) + names.length) % names.length;
    }

    public void setSpeedPercent(int percent) {
        speed = percent;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public boolean paused() {
        return paused;
    }

    /// Jumps ahead `ticks` without drawing them.
    public void skip(int ticks) {
        for (int i = 0; i < ticks && cursor.step(); i++) {
            match.events();
        }
    }

    @Override
    public boolean canStep() {
        return !paused && !match.over();
    }

    @Override
    public void step(Input ignored) {
        if (cursor.step()) {
            emit(match.events());
        }
    }

    @Override
    public boolean interactive() {
        return false;
    }

    @Override
    public int speedPercent() {
        return paused ? 0 : speed;
    }
}
