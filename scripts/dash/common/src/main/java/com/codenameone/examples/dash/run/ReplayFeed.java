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

/// Feeds a recorded [Replay] into a [Run] one tick at a time, in step with
/// whatever else is running: a ghost beside the player, or the replay viewer.
final class ReplayFeed {
    private final Replay replay;
    private final int[] inputs;
    private int next;

    ReplayFeed(Replay replay) {
        this.replay = replay;
        this.inputs = new int[replay.runners()];
    }

    /// Steps `run` once with the inputs recorded for its current tick.
    void step(Run run) {
        if (run.over()) {
            return;
        }
        for (int i = 0; i < inputs.length; i++) {
            inputs[i] = 0;
        }
        int tick = run.tick();
        while (next < replay.eventCount()) {
            int[] e = replay.event(next);
            if (e[0] > tick) {
                break;
            }
            if (e[0] == tick && e[1] >= 0 && e[1] < inputs.length) {
                inputs[e[1]] |= e[2];
            }
            next++;
        }
        run.step(inputs);
    }
}
