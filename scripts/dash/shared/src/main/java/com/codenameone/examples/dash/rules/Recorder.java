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
package com.codenameone.examples.dash.rules;

/// Steps a [Run] and records what it was given, so the run can be submitted as a
/// [Replay] when it ends.
public final class Recorder {
    private final Run run;
    private final Replay replay;

    public Recorder(Run run) {
        this(run, run.level().id);
    }

    /// Records under `levelId`, which for a community level is its content
    /// reference (`pack@hash#level`) rather than the bare id the level carries.
    public Recorder(Run run, String levelId) {
        this.run = run;
        this.replay = new Replay(levelId, run.level().contentHash(), run.seed(), run.runnerCount());
    }

    public void step(int[] inputs) {
        if (run.over()) {
            return;
        }
        if (inputs != null) {
            for (int i = 0; i < inputs.length && i < run.runnerCount(); i++) {
                replay.record(run.tick(), i, inputs[i] & Input.ALL);
            }
        }
        run.step(inputs);
    }

    public Run run() {
        return run;
    }

    public Replay replay() {
        return replay;
    }
}
