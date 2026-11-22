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

/// The built-in obstacles, pickups, chunks and official levels. A fork changes
/// the game here (or in the editor, which produces the same objects from data).
public final class Content {
    public static final String HURDLE = "hurdle";
    public static final String BARRIER = "barrier";
    public static final String HAULER = "hauler";
    public static final String GAP = "gap";
    public static final String COIN = "coin";
    public static final String GEM = "gem";
    public static final String MAGNET = "magnet";
    public static final String SHIELD = "shield";
    public static final String BOOST = "boost";

    public static final String TUTORIAL = "first-steps";
    public static final String CAUSEWAY = "neon-causeway";
    public static final String ENDLESS = "endless";

    private static Catalog catalog;

    private Content() {
    }

    public static synchronized Catalog catalog() {
        if (catalog == null) {
            catalog = new Catalog()
                    .add(new ObstacleDef(HURDLE, Clearance.JUMP, 300, 600, false))
                    .add(new ObstacleDef(BARRIER, Clearance.SLIDE, 400, 0, true))
                    .add(new ObstacleDef(HAULER, Clearance.DODGE, 9000, 0, true))
                    .add(new ObstacleDef(GAP, Clearance.JUMP, 1600, 0, true))
                    .add(new PickupDef(COIN, PickupKind.COIN, 10, 0))
                    .add(new PickupDef(GEM, PickupKind.GEM, 100, 0))
                    .add(new PickupDef(MAGNET, PickupKind.MAGNET, 25, 8 * Run.TICKS_PER_SECOND))
                    .add(new PickupDef(SHIELD, PickupKind.SHIELD, 25, 20 * Run.TICKS_PER_SECOND))
                    .add(new PickupDef(BOOST, PickupKind.BOOST, 25, 4 * Run.TICKS_PER_SECOND));
        }
        return catalog;
    }

    /// The chunk pool every official endless section draws from, easiest first.
    /// Chunks are short and full: a coin trail runs through nearly every one,
    /// so the line a player takes always has something to pick up, and a
    /// hazard is never more than a couple of seconds away.
    public static List<Chunk> chunks() {
        // Spacing rule: a jump carries the runner about 14 m at top speed, and
        // a lane change takes a quarter second. So two obstacles a runner has
        // to answer one after the other stand at least 16 m apart, nothing
        // stands in a chunk's first 6 m or within 12 m of its end (the next
        // chunk may open with anything), and a hauler always leaves a lane
        // open with room to reach it. ChunkFairnessTest holds every chunk to
        // this by sending the bot down hundreds of seeds.
        List<Chunk> c = new ArrayList<Chunk>();
        int u = Track.UNIT;
        c.add(trail(new Chunk("warmup", 22 * u, 0).add(10 * u, HURDLE, 1), 2, 14, 1));
        c.add(trail(new Chunk("side-step", 34 * u, 0).add(6 * u, HAULER, 0).add(22 * u, HURDLE, 2), 2, 26, 1));
        c.add(trail(new Chunk("duck", 22 * u, 1).add(10 * u, BARRIER, Placement.ALL_LANES), 4, 18, 1)
                .add(16 * u, GEM, 0));
        c.add(trail(new Chunk("leap", 24 * u, 1).add(10 * u, GAP, Placement.ALL_LANES), 6, 16, 1));
        c.add(trail(new Chunk("hop-skip", 36 * u, 1).add(6 * u, HURDLE, 0).add(6 * u, HURDLE, 2)
                .add(24 * u, BARRIER, 1), 2, 30, 0));
        c.add(trail(new Chunk("corridor", 34 * u, 2).add(6 * u, HAULER, 0).add(6 * u, HAULER, 2)
                .add(22 * u, HURDLE, 1), 2, 28, 1).add(28 * u, MAGNET, 0));
        c.add(trail(new Chunk("slalom", 42 * u, 2).add(6 * u, HURDLE, 0).add(18 * u, HURDLE, 1)
                .add(30 * u, HURDLE, 2), 2, 36, 2).add(36 * u, GEM, 0));
        c.add(trail(new Chunk("weave", 52 * u, 3).add(6 * u, HAULER, 0).add(6 * u, HAULER, 1)
                .add(30 * u, HAULER, 1).add(30 * u, HAULER, 2), 2, 46, 2).add(46 * u, SHIELD, 0));
        c.add(trail(new Chunk("combo", 38 * u, 4).add(6 * u, HURDLE, Placement.ALL_LANES)
                .add(26 * u, BARRIER, Placement.ALL_LANES), 2, 32, 1).add(32 * u, BOOST, 2));
        c.add(trail(new Chunk("traffic", 56 * u, 5).add(6 * u, HAULER, 1).add(26 * u, HAULER, 0)
                .add(26 * u, HAULER, 2).add(44 * u, BARRIER, Placement.ALL_LANES), 2, 50, 1).add(50 * u, GEM, 2));
        c.add(trail(new Chunk("gauntlet", 42 * u, 6).add(6 * u, HAULER, 1).add(8 * u, BARRIER, 0)
                .add(8 * u, HURDLE, 2).add(30 * u, GAP, Placement.ALL_LANES), 18, 38, 1).add(38 * u, GEM, 0));
        return c;
    }

    /// Coins every 2 m in `lane` from `from` to `to` metres into the chunk.
    private static Chunk trail(Chunk c, int from, int to, int lane) {
        for (int m = from; m <= to; m += 2) {
            c.add(m * Track.UNIT, COIN, lane);
        }
        return c;
    }

    /// Every official level, in the order the home screen lists them.
    public static List<LevelDef> levels() {
        List<LevelDef> l = new ArrayList<LevelDef>();
        l.add(tutorial());
        l.add(causeway());
        l.add(endless());
        return l;
    }

    public static LevelDef level(String id) {
        List<LevelDef> l = levels();
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i).id.equals(id)) {
                return l.get(i);
            }
        }
        return null;
    }

    /// The how-to-play level: side-on, one move at a time, nothing lethal.
    public static LevelDef tutorial() {
        LevelDef d = new LevelDef(TUTORIAL, "First Steps");
        d.theme = "daybreak";
        d.length = 400 * Track.UNIT;
        d.accel = 0;
        d.stars = new int[] {400, 450, 500};
        d.cue(0, ViewMode.SIDE_2D, 0);
        d.dialog.add(new DialogLine(20 * Track.UNIT, "Coach", "Swipe up to jump the hurdle.", Input.JUMP));
        d.place(50, HURDLE, Placement.ALL_LANES);
        d.place(90, HURDLE, Placement.ALL_LANES);
        d.dialog.add(new DialogLine(130 * Track.UNIT, "Coach", "Swipe down to slide under the barrier.", Input.SLIDE));
        d.place(160, BARRIER, Placement.ALL_LANES);
        d.place(210, BARRIER, Placement.ALL_LANES);
        for (int i = 0; i < 10; i++) {
            d.place(240 + i * 3, COIN, Placement.ALL_LANES);
        }
        d.dialog.add(new DialogLine(290 * Track.UNIT, "Coach", "Gaps need a jump too. Then you are ready.", Input.JUMP));
        d.place(320, GAP, Placement.ALL_LANES);
        d.place(370, GEM, Placement.ALL_LANES);
        return d;
    }

    // tag::dash-causeway[]
    /// Opens side-on and swings into the chase camera at 300 metres, where the
    /// lanes appear: the faux-2D-to-3D level.
    public static LevelDef causeway() {
        LevelDef d = new LevelDef(CAUSEWAY, "Neon Causeway");
        d.theme = "neon";
        d.length = 1500 * Track.UNIT;
        d.stars = new int[] {900, 1400, 1800};
        d.cue(0, ViewMode.SIDE_2D, 0);
        d.cue(300, ViewMode.CHASE_3D, 40);
        for (int m = 60; m < 280; m += 45) {
            d.place(m, (m / 45) % 2 == 0 ? HURDLE : BARRIER, Placement.ALL_LANES);
            d.place(m + 15, COIN, Placement.ALL_LANES);
        }
        d.dialog.add(new DialogLine(290 * Track.UNIT, "Vee", "The road opens up. Swipe sideways!", Input.LEFT));
        d.proceduralFrom = 340 * Track.UNIT;
        d.difficultyStep = 180 * Track.UNIT;
        d.chunks.addAll(chunks());
        return d;
    }
    // end::dash-causeway[]

    /// The chase-view endless run every leaderboard and the daily challenge use.
    public static LevelDef endless() {
        LevelDef d = new LevelDef(ENDLESS, "Endless");
        d.theme = "ember";
        d.goal = Goal.ENDLESS;
        d.stars = new int[] {500, 1500, 3000};
        d.cue(0, ViewMode.CHASE_3D, 0);
        d.proceduralFrom = 40 * Track.UNIT;
        d.chunks.addAll(chunks());
        return d;
    }

    /// The seed of the daily challenge for a day number (days since the epoch, in
    /// UTC, which the server supplies). Everyone plays the same track that day.
    public static long dailySeed(int day) {
        return new Rng(0xDA11L * 1000003L + day).nextLong();
    }

    /// The seed of a level's weekly board for a week number (days since the
    /// epoch divided by seven). A weekly score only counts on this seed.
    public static long weeklySeed(int week, String levelId) {
        return new Rng(0x3EE7L * 1000003L + week * 31L + new Hash().add(levelId).value()).nextLong();
    }
}
