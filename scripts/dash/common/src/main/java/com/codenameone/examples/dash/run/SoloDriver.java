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

import com.codenameone.examples.dash.rules.Bot;
import com.codenameone.examples.dash.rules.Recorder;
import com.codenameone.examples.dash.rules.Replay;
import com.codenameone.examples.dash.rules.Run;
import java.util.ArrayList;
import java.util.List;

/// A run on this device alone. Every input lands on the next tick, and every
/// tick is recorded, so the finished [#replay] can go to the server, which
/// plays it again under the same rules before it believes the score.
///
/// Company is optional and never touches the player's run: a leaderboard
/// ghost replays someone's best, and bots each run a copy of the level of
/// their own, on the same seed, so the track they meet is the same.
public final class SoloDriver extends Driver {
    private final Recorder recorder;
    private final List<Run> ghosts = new ArrayList<Run>();
    private final List<ReplayFeed> feeds = new ArrayList<ReplayFeed>();
    private final List<Bot> bots = new ArrayList<Bot>();
    private final List<String> names = new ArrayList<String>();
    private final int[] inputs = new int[1];
    private final int[] botInput = new int[1];
    private int pending;

    public SoloDriver(Levels.Playable level, long seed) {
        run = new Run(level.level, level.catalog, seed, 1);
        recorder = new Recorder(run, level.ref);
    }

    /// Adds a recorded run to race against. Ignored when it was recorded on
    /// other content: a ghost of another track would run through walls.
    public SoloDriver ghost(Levels.Playable level, Replay replay, String name) {
        if (replay != null && replay.levelHash() == level.level.contentHash() && replay.runners() == 1) {
            ghosts.add(new Run(level.level, level.catalog, replay.seed(), 1));
            feeds.add(new ReplayFeed(replay));
            bots.add(null);
            names.add(name);
        }
        return this;
    }

    /// Adds a bot of `skill` (0 to 100) to race against.
    public SoloDriver bot(Levels.Playable level, int skill, String name) {
        ghosts.add(new Run(level.level, level.catalog, run.seed(), 1));
        feeds.add(null);
        bots.add(new Bot(run.seed() + 977L * (bots.size() + 1), skill));
        names.add(name);
        return this;
    }

    @Override
    public boolean canStep() {
        return !run.over();
    }

    @Override
    public void step() {
        inputs[0] = pending;
        pending = 0;
        recorder.step(inputs);
        for (int i = 0; i < ghosts.size(); i++) {
            Run g = ghosts.get(i);
            if (g.over()) {
                continue;
            }
            Bot b = bots.get(i);
            if (b != null) {
                botInput[0] = b.decide(g, 0);
                g.step(botInput);
            } else {
                feeds.get(i).step(g);
            }
        }
    }

    @Override
    public void input(int bits) {
        if (!run.over()) {
            pending |= bits;
        }
    }

    @Override
    public Run[] ghosts() {
        return ghosts.toArray(new Run[ghosts.size()]);
    }

    /// The name of ghost `i` of [#ghosts].
    public String ghostName(int i) {
        return names.get(i);
    }

    /// The run so far, ready to submit once [Run#over].
    public Replay replay() {
        return recorder.replay();
    }
}
