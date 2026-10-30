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

/// A level: hand-placed track, an optional endless section assembled from
/// chunks, camera cues, a goal and star thresholds. This is what the editor
/// edits, what a player submits and what a leaderboard is keyed by.
///
/// Distances are millimetres, speeds millimetres per second.
public final class LevelDef {
    public String id;
    public String name;
    public String theme = "dusk";
    public int lanes = 3;
    public int startSpeed = 9000;
    public int maxSpeed = 22000;
    /// Millimetres per second gained every second.
    public int accel = 120;
    /// For [Goal#DISTANCE], the finish line. Ignored otherwise.
    public int length;
    public Goal goal = Goal.DISTANCE;
    public int goalValue;
    /// Where the endless section starts; it never starts before the last
    /// hand-placed obstacle ends.
    public int proceduralFrom;
    /// Distance per difficulty step of the endless section.
    public int difficultyStep = 150 * Track.UNIT;
    /// Scores for one, two and three stars.
    public int[] stars = {100, 300, 600};
    public final List<Placement> placements = new ArrayList<Placement>();
    public final List<Chunk> chunks = new ArrayList<Chunk>();
    public final List<ViewCue> cues = new ArrayList<ViewCue>();
    /// Tutorial or story lines, shown by distance.
    public final List<DialogLine> dialog = new ArrayList<DialogLine>();

    public LevelDef(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public LevelDef place(int metres, String defId, int lane) {
        placements.add(new Placement(metres * Track.UNIT, defId, lane));
        return this;
    }

    public LevelDef cue(int metres, ViewMode mode, int blendMetres) {
        cues.add(new ViewCue(metres * Track.UNIT, mode, blendMetres * Track.UNIT));
        return this;
    }

    /// The camera at the start of the run: the first cue at distance 0, or the
    /// chase view.
    public ViewMode startView() {
        for (int i = 0; i < cues.size(); i++) {
            if (cues.get(i).at == 0) {
                return cues.get(i).mode;
            }
        }
        return ViewMode.CHASE_3D;
    }

    /// A hash of everything that changes how the level plays, so a replay or a
    /// leaderboard entry can tell that a level was edited under it. Cosmetic
    /// fields -- name, theme, cues, dialog -- are left out on purpose: retitling
    /// a level must not wipe its scores.
    public long contentHash() {
        Hash h = new Hash();
        h.add(lanes).add(startSpeed).add(maxSpeed).add(accel).add(length).add(goal.ordinal()).add(goalValue)
                .add(proceduralFrom).add(difficultyStep);
        for (int i = 0; i < placements.size(); i++) {
            Placement p = placements.get(i);
            h.add(p.z).add(p.lane).add(p.defId);
        }
        for (int i = 0; i < chunks.size(); i++) {
            Chunk c = chunks.get(i);
            h.add(c.id).add(c.length).add(c.minDifficulty);
            for (int j = 0; j < c.placements().size(); j++) {
                Placement p = c.placements().get(j);
                h.add(p.z).add(p.lane).add(p.defId);
            }
        }
        return h.value();
    }
}
