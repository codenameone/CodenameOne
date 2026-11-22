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

/// Checks a level before it is played, saved or submitted. The editor shows the
/// problems as the author works; the server runs the same checks and refuses a
/// submission with any error.
public final class Validator {
    /// One finding. Warnings do not block a submission.
    public static final class Problem {
        public final boolean error;
        public final String message;

        Problem(boolean error, String message) {
            this.error = error;
            this.message = message;
        }

        @Override
        public String toString() {
            return (error ? "error: " : "warning: ") + message;
        }
    }

    private Validator() {
    }

    public static List<Problem> check(LevelDef d, Catalog c) {
        List<Problem> out = new ArrayList<Problem>();
        if (d.id == null || d.id.length() == 0 || d.id.indexOf('|') >= 0) {
            out.add(new Problem(true, "the level needs an id without '|'"));
        }
        if (d.lanes < 1 || d.lanes > 5) {
            out.add(new Problem(true, "lanes must be 1 to 5"));
        }
        if (d.startSpeed < 2000 || d.maxSpeed < d.startSpeed || d.maxSpeed > 40000) {
            out.add(new Problem(true, "speeds must be 2..40 m/s, max at least start"));
        }
        if (d.goal == Goal.DISTANCE && d.length < 50 * Track.UNIT) {
            out.add(new Problem(true, "a distance goal needs a length of 50 m or more"));
        }
        if ((d.goal == Goal.COINS || d.goal == Goal.SURVIVE) && d.goalValue <= 0) {
            out.add(new Problem(true, "the goal needs a target value"));
        }
        if (d.goal == Goal.ENDLESS && d.chunks.isEmpty()) {
            out.add(new Problem(true, "an endless level needs at least one chunk"));
        }
        for (int i = 1; i < d.stars.length; i++) {
            if (d.stars[i] < d.stars[i - 1]) {
                out.add(new Problem(true, "star scores must not decrease"));
            }
        }
        for (int i = 0; i < d.placements.size(); i++) {
            checkPlacement(out, d, c, d.placements.get(i), "at " + d.placements.get(i).z / Track.UNIT + " m");
            Placement p = d.placements.get(i);
            if (p.lane != Placement.ALL_LANES && viewAt(d, p.z) == ViewMode.SIDE_2D) {
                out.add(new Problem(false, p.defId + " at " + p.z / Track.UNIT
                        + " m is in one lane, but the camera is side-on there"));
            }
        }
        for (int i = 0; i < d.chunks.size(); i++) {
            Chunk ch = d.chunks.get(i);
            for (int j = 0; j < ch.placements().size(); j++) {
                Placement p = ch.placements().get(j);
                checkPlacement(out, d, c, p, "in chunk " + ch.id);
                if (p.z >= ch.length) {
                    out.add(new Problem(true, p.defId + " lies past the end of chunk " + ch.id));
                }
            }
        }
        return out;
    }

    private static void checkPlacement(List<Problem> out, LevelDef d, Catalog c, Placement p, String where) {
        if (!c.has(p.defId)) {
            out.add(new Problem(true, "unknown " + p.defId + " " + where));
        }
        if (p.lane != Placement.ALL_LANES && (p.lane < 0 || p.lane >= d.lanes)) {
            out.add(new Problem(true, p.defId + " " + where + " is in lane " + p.lane + " of " + d.lanes));
        }
        if (p.z < 0) {
            out.add(new Problem(true, p.defId + " " + where + " is before the start"));
        }
    }

    private static ViewMode viewAt(LevelDef d, int z) {
        ViewMode m = ViewMode.CHASE_3D;
        int best = -1;
        for (int i = 0; i < d.cues.size(); i++) {
            ViewCue cue = d.cues.get(i);
            if (cue.at <= z && cue.at >= best) {
                best = cue.at;
                m = cue.mode;
            }
        }
        return m;
    }

    public static boolean hasErrors(List<Problem> ps) {
        for (int i = 0; i < ps.size(); i++) {
            if (ps.get(i).error) {
                return true;
            }
        }
        return false;
    }

    /// True when a strong bot finishes a finite-goal level on at least one of
    /// three seeds, or survives 500 m of an endless one. The server requires it
    /// before a community level is published; the editor offers it as a button.
    public static boolean beatable(LevelDef d, Catalog c) {
        for (long seed = 1; seed <= 3; seed++) {
            Run run = new Run(d, c, seed, 1);
            Bot bot = new Bot(seed, 100);
            int[] in = new int[1];
            while (!run.over()) {
                in[0] = bot.decide(run, 0);
                run.step(in);
                if (d.goal == Goal.ENDLESS && run.runner(0).z >= 500 * Track.UNIT) {
                    return true;
                }
            }
            if (run.runner(0).finished) {
                return true;
            }
        }
        return false;
    }
}
