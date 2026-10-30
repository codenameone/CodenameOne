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
    public static List<Chunk> chunks() {
        List<Chunk> c = new ArrayList<Chunk>();
        c.add(coinLine(new Chunk("warmup", 30 * Track.UNIT, 0).add(14 * Track.UNIT, HURDLE, 1), 0, 2));
        c.add(new Chunk("side-step", 30 * Track.UNIT, 0).add(10 * Track.UNIT, HAULER, 0).add(10 * Track.UNIT, COIN, 1)
                .add(12 * Track.UNIT, COIN, 1).add(14 * Track.UNIT, COIN, 1));
        c.add(new Chunk("duck", 30 * Track.UNIT, 1).add(12 * Track.UNIT, BARRIER, Placement.ALL_LANES)
                .add(20 * Track.UNIT, GEM, 1));
        c.add(new Chunk("leap", 32 * Track.UNIT, 1).add(12 * Track.UNIT, GAP, Placement.ALL_LANES)
                .add(13 * Track.UNIT, COIN, 1));
        c.add(new Chunk("corridor", 40 * Track.UNIT, 2).add(10 * Track.UNIT, HAULER, 0).add(10 * Track.UNIT, HAULER, 2)
                .add(15 * Track.UNIT, HURDLE, 1).add(25 * Track.UNIT, MAGNET, 1));
        c.add(new Chunk("weave", 44 * Track.UNIT, 3).add(8 * Track.UNIT, HAULER, 0).add(8 * Track.UNIT, HAULER, 1)
                .add(26 * Track.UNIT, HAULER, 1).add(26 * Track.UNIT, HAULER, 2).add(36 * Track.UNIT, SHIELD, 0));
        c.add(new Chunk("combo", 40 * Track.UNIT, 4).add(10 * Track.UNIT, HURDLE, Placement.ALL_LANES)
                .add(24 * Track.UNIT, BARRIER, Placement.ALL_LANES).add(34 * Track.UNIT, BOOST, 1));
        c.add(new Chunk("gauntlet", 48 * Track.UNIT, 6).add(8 * Track.UNIT, HAULER, 1).add(10 * Track.UNIT, BARRIER, 0)
                .add(10 * Track.UNIT, HURDLE, 2).add(28 * Track.UNIT, GAP, Placement.ALL_LANES)
                .add(40 * Track.UNIT, GEM, 1));
        return c;
    }

    private static Chunk coinLine(Chunk c, int lane, int count) {
        for (int i = 0; i < count; i++) {
            c.add((20 + 2 * i) * Track.UNIT, COIN, lane);
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
        d.theme = "meadow";
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
        d.theme = "dusk";
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
