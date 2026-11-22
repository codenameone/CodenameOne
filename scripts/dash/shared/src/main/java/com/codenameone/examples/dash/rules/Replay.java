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

import java.util.ArrayList;
import java.util.List;

/// A run as data: the level, its content hash, the seed and every non-empty input.
/// Re-simulated, never recorded frames -- which is why a leaderboard can trust a
/// score: the server plays the replay and reads the score off its own run.
///
/// Text form: `DSH1|<rules>|<level id>|<content hash>|<seed>|<runners>|t,r,b;...`
public final class Replay {
    public static final String MAGIC = "DSH1";

    private final String rulesVersion;
    private final String levelId;
    private final long levelHash;
    private final long seed;
    private final int runners;
    private final List<int[]> events = new ArrayList<int[]>();

    public Replay(String levelId, long levelHash, long seed, int runners) {
        this(RulesVersion.ID, levelId, levelHash, seed, runners);
    }

    private Replay(String rulesVersion, String levelId, long levelHash, long seed, int runners) {
        this.rulesVersion = rulesVersion;
        this.levelId = levelId;
        this.levelHash = levelHash;
        this.seed = seed;
        this.runners = runners;
    }

    /// Appends one input. Ticks must not go backwards.
    public void record(int tick, int runner, int bits) {
        if (bits == 0) {
            return;
        }
        if (!events.isEmpty() && events.get(events.size() - 1)[0] > tick) {
            throw new IllegalArgumentException("tick " + tick + " before last");
        }
        events.add(new int[] {tick, runner, bits});
    }

    public String rulesVersion() {
        return rulesVersion;
    }

    public String levelId() {
        return levelId;
    }

    public long levelHash() {
        return levelHash;
    }

    public long seed() {
        return seed;
    }

    public int runners() {
        return runners;
    }

    public int eventCount() {
        return events.size();
    }

    public int[] event(int i) {
        return events.get(i);
    }

    /// Re-runs the replay to its end on `level`, which must be the level it was
    /// recorded on ([#levelHash] says whether it still is).
    public Run play(LevelDef level, Catalog catalog) {
        return playTo(level, catalog, Integer.MAX_VALUE);
    }

    /// Re-runs to `untilTick`, or the end; the replay viewer scrubs with this.
    public Run playTo(LevelDef level, Catalog catalog, int untilTick) {
        Run run = new Run(level, catalog, seed, runners);
        int[] in = new int[runners];
        int next = 0;
        while (!run.over() && run.tick() < untilTick) {
            for (int i = 0; i < runners; i++) {
                in[i] = 0;
            }
            while (next < events.size() && events.get(next)[0] == run.tick()) {
                int[] e = events.get(next++);
                if (e[1] >= 0 && e[1] < runners) {
                    in[e[1]] |= e[2];
                }
            }
            run.step(in);
        }
        return run;
    }

    public String encode() {
        StringBuilder sb = new StringBuilder();
        sb.append(MAGIC).append('|').append(rulesVersion).append('|').append(levelId).append('|')
                .append(Long.toHexString(levelHash)).append('|').append(seed).append('|').append(runners).append('|');
        for (int i = 0; i < events.size(); i++) {
            int[] e = events.get(i);
            if (i > 0) {
                sb.append(';');
            }
            sb.append(e[0]).append(',').append(e[1]).append(',').append(e[2]);
        }
        return sb.toString();
    }

    /// Parses [#encode]'s output. Refuses anything malformed -- unknown input
    /// bits, ticks out of order, a runner out of range -- because this is what a
    /// client submits for a score.
    public static Replay decode(String s) {
        List<String> f = Text.split(s, '|');
        if (f.size() != 7 || !MAGIC.equals(f.get(0))) {
            throw new IllegalArgumentException("not a replay");
        }
        int runners = Integer.parseInt(f.get(5));
        if (runners < 1 || runners > 8) {
            throw new IllegalArgumentException("runners " + runners);
        }
        Replay r = new Replay(f.get(1), f.get(2), Text.parseHex(f.get(3)), Long.parseLong(f.get(4)), runners);
        String body = f.get(6);
        if (body.length() > 0) {
            List<String> evs = Text.split(body, ';');
            for (int i = 0; i < evs.size(); i++) {
                List<String> p = Text.split(evs.get(i), ',');
                if (p.size() != 3) {
                    throw new IllegalArgumentException("event " + evs.get(i));
                }
                int t = Integer.parseInt(p.get(0));
                int who = Integer.parseInt(p.get(1));
                int bits = Integer.parseInt(p.get(2));
                if (t < 0 || t >= Run.MAX_TICKS || who < 0 || who >= runners || bits == 0 || !Input.valid(bits)) {
                    throw new IllegalArgumentException("event " + evs.get(i));
                }
                r.record(t, who, bits);
            }
        }
        return r;
    }

    /// A replay holding only runner `who`'s inputs, as runner 0: how a live race
    /// is split into one leaderboard entry per player.
    public Replay single(int who) {
        Replay r = new Replay(rulesVersion, levelId, levelHash, seed, 1);
        for (int i = 0; i < events.size(); i++) {
            int[] e = events.get(i);
            if (e[1] == who) {
                r.record(e[0], 0, e[2]);
            }
        }
        return r;
    }
}
