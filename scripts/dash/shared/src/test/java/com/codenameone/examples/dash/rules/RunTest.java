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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class RunTest {
    /// A race of bots, recorded: the shape every golden replay has.
    static Replay botRace(LevelDef level, long seed, int runners) {
        Run run = new Run(level, Content.catalog(), seed, runners);
        Recorder rec = new Recorder(run);
        Bot[] bots = new Bot[runners];
        for (int i = 0; i < runners; i++) {
            bots[i] = new Bot(seed + i, 60 + 15 * i);
        }
        int[] in = new int[runners];
        while (!run.over()) {
            for (int i = 0; i < runners; i++) {
                in[i] = bots[i].decide(run, i);
            }
            rec.step(in);
        }
        return rec.replay();
    }

    private static LevelDef flat(String obstacle, int lane) {
        LevelDef d = new LevelDef("t", "t");
        d.length = 100 * Track.UNIT;
        d.accel = 0;
        d.place(30, obstacle, lane);
        return d;
    }

    private static Run runWith(LevelDef d, int actTick, int bits) {
        Run run = new Run(d, Content.catalog(), 1, 1);
        while (!run.over()) {
            run.step(new int[] {run.tick() == actTick ? bits : 0});
        }
        return run;
    }

    /// The tick on which a runner at start speed is `metres` short of 30 m.
    private static int ticksBefore(int metres) {
        return (30 - metres) * Track.UNIT * Run.TICKS_PER_SECOND / 9000;
    }

    @Test
    void sameSeedAndInputsGiveTheSameRun() {
        Replay a = botRace(Content.causeway(), 42, 3);
        Replay b = botRace(Content.causeway(), 42, 3);
        assertEquals(a.encode(), b.encode());
        assertEquals(a.play(Content.causeway(), Content.catalog()).stateHash(),
                b.play(Content.causeway(), Content.catalog()).stateHash());
        assertNotEquals(botRace(Content.causeway(), 43, 3).encode(), a.encode());
    }

    @Test
    void replayRoundTripsThroughText() {
        Replay r = botRace(Content.endless(), 7, 2);
        Replay back = Replay.decode(r.encode());
        assertEquals(r.encode(), back.encode());
        assertEquals(r.play(Content.endless(), Content.catalog()).stateHash(),
                back.play(Content.endless(), Content.catalog()).stateHash());
    }

    @Test
    void tamperedReplaysAreRefused() {
        String ok = botRace(Content.tutorial(), 3, 1).encode();
        assertThrows(IllegalArgumentException.class, () -> Replay.decode(ok.replace("DSH1", "XXX1")));
        assertThrows(IllegalArgumentException.class, () -> Replay.decode(ok + ";5,0,64"));
        assertThrows(IllegalArgumentException.class, () -> Replay.decode(ok + ";1,0,4"));
        assertThrows(IllegalArgumentException.class, () -> Replay.decode(ok + ";999999,3,4"));
    }

    @Test
    void jumpingClearsAHurdleAndStandingStumbles() {
        LevelDef d = flat(Content.HURDLE, Placement.ALL_LANES);
        Run cleared = runWith(d, ticksBefore(3), Input.JUMP);
        assertTrue(cleared.runner(0).finished);
        assertEquals(0, cleared.runner(0).stumbles);
        Run hit = runWith(d, -1, 0);
        assertTrue(hit.runner(0).finished, "a hurdle only trips you");
        assertEquals(1, hit.runner(0).stumbles);
    }

    @Test
    void slidingPassesUnderABarrierAndStandingFalls() {
        LevelDef d = flat(Content.BARRIER, Placement.ALL_LANES);
        assertTrue(runWith(d, ticksBefore(2), Input.SLIDE).runner(0).finished);
        Run hit = runWith(d, -1, 0);
        assertFalse(hit.runner(0).alive);
    }

    @Test
    void aHaulerIsDodgedByChangingLane() {
        LevelDef d = flat(Content.HAULER, 1);
        assertTrue(runWith(d, 10, Input.LEFT).runner(0).finished);
        assertFalse(runWith(d, -1, 0).runner(0).alive);
        assertFalse(runWith(d, 10, Input.JUMP).runner(0).alive, "no jumping over a hauler");
    }

    @Test
    void aShieldAbsorbsOneHit() {
        LevelDef d = flat(Content.BARRIER, Placement.ALL_LANES);
        d.place(10, Content.SHIELD, Placement.ALL_LANES);
        Run run = runWith(d, -1, 0);
        assertTrue(run.runner(0).finished);
        assertEquals(0, run.runner(0).shield);
    }

    @Test
    void coinsAreCollectedOncePerRunnerInARace() {
        LevelDef d = new LevelDef("c", "c");
        d.length = 60 * Track.UNIT;
        d.place(20, Content.COIN, 1);
        d.place(25, Content.COIN, 0);
        Run run = new Run(d, Content.catalog(), 1, 2);
        while (!run.over()) {
            run.step(new int[] {0, 0});
        }
        assertEquals(1, run.runner(0).coins);
        assertEquals(1, run.runner(1).coins, "each runner has their own copy of the track");
    }

    @Test
    void everyRunnerInARaceSeesTheSameTrack() {
        Run a = new Run(Content.endless(), Content.catalog(), 99, 1);
        Run b = new Run(Content.endless(), Content.catalog(), 99, 4);
        assertEquals(a.track().size(), b.track().size());
        for (int i = 0; i < a.track().size(); i++) {
            assertEquals(a.track().get(i).z, b.track().get(i).z);
            assertEquals(a.track().get(i).lane, b.track().get(i).lane);
        }
    }

    @Test
    void theOfficialLevelsValidateAndCanBeBeaten() {
        for (LevelDef d : Content.levels()) {
            List<Validator.Problem> ps = Validator.check(d, Content.catalog());
            assertFalse(Validator.hasErrors(ps), d.id + " " + ps);
            assertTrue(Validator.beatable(d, Content.catalog()), d.id);
        }
    }

    /// The chunks are fair: a sharp bot finishes the causeway on every one of
    /// 200 weekly seeds. One obstacle placed inside another's jump or lane
    /// change shows up here as a run that ends early on some seeds.
    @Test
    void aSharpBotFinishesTheCausewayOnEveryWeeklySeed() {
        LevelDef level = Content.causeway();
        for (int week = 2900; week < 3100; week++) {
            long seed = Content.weeklySeed(week, level.id);
            Run run = new Run(level, Content.catalog(), seed, 1);
            Bot bot = new Bot(seed * 31, 97);
            int[] in = new int[1];
            while (!run.over() && run.tick() < 60 * 600) {
                in[0] = bot.decide(run, 0);
                run.step(in);
            }
            assertTrue(run.runner(0).finished, "week " + week + " ended at "
                    + run.runner(0).z / Track.UNIT + " m");
        }
    }

    @Test
    void theValidatorCatchesBrokenLevels() {
        LevelDef d = new LevelDef("bad", "bad");
        d.length = 10;
        d.place(5, "nonsense", 0);
        d.place(6, Content.HURDLE, 7);
        d.stars = new int[] {5, 2, 9};
        List<Validator.Problem> ps = Validator.check(d, Content.catalog());
        assertTrue(ps.size() >= 4, ps.toString());
        LevelDef wall = flat(Content.HAULER, Placement.ALL_LANES);
        assertFalse(Validator.beatable(wall, Content.catalog()));
    }

    @Test
    void theCameraSwingsFromSideOnToChaseOnTheCauseway() {
        Track t = new Run(Content.causeway(), Content.catalog(), 1, 1).track();
        assertEquals(ViewMode.SIDE_2D, t.viewAt(100 * Track.UNIT));
        assertEquals(ViewMode.CHASE_3D, t.viewAt(320 * Track.UNIT));
        assertEquals(ViewMode.SIDE_2D, t.previousViewAt(320 * Track.UNIT));
        assertEquals(500, t.blendAt(320 * Track.UNIT));
        assertEquals(1000, t.blendAt(400 * Track.UNIT));
    }

    @Test
    void editingALevelChangesItsHashButRetitlingDoesNot() {
        LevelDef a = Content.causeway();
        LevelDef b = Content.causeway();
        b.name = "Renamed";
        b.theme = "other";
        assertEquals(a.contentHash(), b.contentHash());
        b.place(500, Content.HURDLE, 1);
        assertNotEquals(a.contentHash(), b.contentHash());
    }

    @Test
    void botsSoakTheEndlessLevelWithoutBreakingInvariants() {
        for (long seed = 1; seed <= 20; seed++) {
            Run run = new Run(Content.endless(), Content.catalog(), seed, 1);
            Bot bot = new Bot(seed, 90);
            int lastZ = 0;
            while (!run.over()) {
                run.step(new int[] {bot.decide(run, 0)});
                Runner r = run.runner(0);
                assertTrue(r.z >= lastZ);
                assertTrue(r.y >= 0 && r.laneX >= 0 && r.laneX <= 2000);
                assertTrue(r.speed <= Content.endless().maxSpeed);
                lastZ = r.z;
            }
            assertTrue(run.runner(0).z > 200 * Track.UNIT, "seed " + seed + " fell at " + run.runner(0).z);
        }
    }

    @Test
    void levelsRoundTripThroughJsonWithTheirOwnDefinitions() {
        for (LevelDef d : Content.levels()) {
            LevelCodec.Decoded back = LevelCodec.decode(LevelCodec.encode(d));
            assertEquals(d.contentHash(), back.level.contentHash(), d.id);
            assertEquals(LevelCodec.encode(d), LevelCodec.encode(back.level), d.id);
        }
        LevelDef d = flat("u:me:spikes", 1);
        java.util.List<Object> defs = new java.util.ArrayList<Object>();
        defs.add(new ObstacleDef("u:me:spikes", Clearance.JUMP, 500, 300, true));
        java.util.Map<String, String> art = new java.util.HashMap<String, String>();
        art.put("u:me:spikes", "asset-1");
        LevelCodec.Decoded back = LevelCodec.decode(LevelCodec.encode(d, defs, art, "asset-2"));
        assertEquals("asset-1", back.art.get("u:me:spikes"));
        assertEquals("asset-2", back.runnerArt);
        assertFalse(Validator.hasErrors(Validator.check(back.level, back.catalog())));
        assertTrue(runWith(back.level, ticksBefore(3), Input.JUMP).runner(0).finished);
        defs.set(0, new ObstacleDef(Content.HURDLE, Clearance.JUMP, 500, 300, true));
        assertThrows(IllegalArgumentException.class, () -> LevelCodec.decode(LevelCodec.encode(d, defs, null, null)));
        assertThrows(IllegalArgumentException.class, () -> LevelCodec.decode("{\"placements\":[[1.5,\"coin\",0]]}"));
    }
}
