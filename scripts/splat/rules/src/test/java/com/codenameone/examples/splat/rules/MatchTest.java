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
package com.codenameone.examples.splat.rules;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MatchTest {
    static List<Slot> bots(int perTeam, int skill) {
        List<Slot> s = new ArrayList<Slot>();
        for (int i = 0; i < perTeam * 2; i++) {
            String marker = Catalog.MARKERS[i % Catalog.MARKERS.length].id;
            s.add(new Slot("Bot " + i, i % 2, marker, skill));
        }
        return s;
    }

    static Match run(long seed, String arena, Mode mode, int perTeam, int skill) {
        Match m = new Match(seed, Catalog.arena(arena), mode, bots(perTeam, skill));
        while (!m.over()) {
            m.step(null);
        }
        return m;
    }

    @Test
    void trigIsConsistent() {
        assertEquals(0, Trig.sin(0));
        assertEquals(Trig.ONE, Trig.sin(Trig.QUARTER));
        assertEquals(-Trig.ONE, Trig.sin(3 * Trig.QUARTER));
        for (int a = 0; a < Trig.FULL; a += 7) {
            long s = Trig.sin(a);
            long c = Trig.cos(a);
            long one = (long) Trig.ONE * Trig.ONE;
            assertTrue(Math.abs(s * s + c * c - one) < one / 100, "angle " + a);
            int back = Trig.atan2(s, c);
            assertTrue(Math.abs(Trig.delta(a, back)) <= 3, "atan2 of " + a + " was " + back);
        }
    }

    @Test
    void fieldsAreMirrored() {
        for (Arena a : Catalog.ARENAS) {
            for (Box b : a.boxes()) {
                boolean twin = false;
                for (Box o : a.boxes()) {
                    twin |= o.x0 == a.width - b.x1 && o.z0 == a.depth - b.z1 && o.height == b.height;
                }
                assertTrue(twin, a.id + " " + b.kind);
            }
            for (int t = 0; t < 2; t++) {
                for (int i = 0; i < a.spawnCount(t); i++) {
                    assertFalse(a.blocked(a.spawnX(t, i), a.spawnZ(t, i), Match.RADIUS, 0, Match.STEP_UP),
                            a.id + " spawn " + t + "/" + i);
                }
            }
        }
    }

    @Test
    void everyModeFinishesWithPaintFlying() {
        for (Arena a : Catalog.ARENAS) {
            for (Mode mode : Mode.values()) {
                Match m = run(42, a.id, mode, 4, 70);
                assertTrue(m.over());
                int paints = 0;
                for (int i = 0; i < m.playerCount(); i++) {
                    paints += m.player(i).paints();
                }
                assertTrue(paints > 5, a.id + " " + mode + " paints " + paints);
                assertTrue(m.score(0) + m.score(1) > 0, a.id + " " + mode + " nobody scored");
            }
        }
    }

    @Test
    void sameSeedSameMatch() {
        Match a = run(7, "lot7", Mode.TEAM_PAINT, 4, 60);
        Match b = run(7, "lot7", Mode.TEAM_PAINT, 4, 60);
        assertEquals(a.stateHash(), b.stateHash());
        assertEquals(a.tick(), b.tick());
        Match c = run(8, "lot7", Mode.TEAM_PAINT, 4, 60);
        assertNotEquals(a.stateHash(), c.stateHash());
    }

    @Test
    void snapshotContinuesIdentically() {
        Match a = new Match(99, Catalog.arena("cul-de-sac"), Mode.CAPTURE_THE_FLAG, bots(3, 80));
        for (int i = 0; i < 1500; i++) {
            a.step(null);
        }
        int[] snap = a.snapshot();
        Match b = new Match(99, Catalog.arena("cul-de-sac"), Mode.CAPTURE_THE_FLAG, bots(3, 80));
        b.restore(snap);
        assertArrayEquals(snap, b.snapshot());
        for (int i = 0; i < 1500; i++) {
            a.step(null);
            b.step(null);
            assertEquals(a.stateHash(), b.stateHash(), "tick " + a.tick());
        }
    }

    @Test
    void betterBotsWinMore() {
        int strong = 0;
        for (long seed = 1; seed <= 6; seed++) {
            List<Slot> s = new ArrayList<Slot>();
            for (int i = 0; i < 8; i++) {
                s.add(new Slot("B" + i, i % 2, "semi", i % 2 == 0 ? 95 : 15));
            }
            Match m = new Match(seed, Catalog.arena("lot7"), Mode.TEAM_PAINT, s);
            while (!m.over()) {
                m.step(null);
            }
            if (m.winner() == 0) {
                strong++;
            }
        }
        assertTrue(strong >= 5, "strong team won " + strong + " of 6");
    }

    @Test
    void aStraightShotPaintsAndAWipeIsJudged() {
        List<Slot> s = new ArrayList<Slot>();
        s.add(new Slot("Gary", 0, "pump", 0));
        s.add(new Slot("Kevin", 1, "pump", 0));
        Match m = new Match(3, Catalog.arena("lot7"), Mode.TEAM_PAINT, s);
        // let the respawn shield run out, then put them face to face on open ground
        Input[] idle = new Input[2];
        for (int i = 0; i <= Match.SHIELD; i++) {
            m.step(idle);
        }
        int[] snap = m.snapshot();
        Player gary = m.player(0);
        Player kevin = m.player(1);
        place(snap, m, 0, 200 * 100, 70 * 100, 0);
        place(snap, m, 1, 200 * 100, 90 * 100, Trig.HALF);
        m.restore(snap);
        assertEquals(20000, gary.x());
        Input fire = new Input(0, 0, 0, 0, Input.FIRE, -1);
        int painted = -1;
        for (int i = 0; i < 30 && painted < 0; i++) {
            m.step(new Input[] {fire, null});
            for (Event e : m.events()) {
                if (e.kind == Event.PAINTED) {
                    painted = e.tick;
                    assertEquals(0, e.a);
                    assertEquals(1, e.b);
                }
            }
        }
        assertTrue(painted >= 0, "the shot never arrived");
        assertFalse(kevin.alive());
        assertEquals(1, m.score(0));
        Input wipe = new Input(0, 0, Trig.HALF, 0, Input.WIPE, -1);
        m.step(new Input[] {Input.IDLE, wipe});
        Event judged = null;
        for (Event e : m.events()) {
            if (e.kind == Event.WIPE_OK || e.kind == Event.WIPE_CAUGHT) {
                judged = e;
            }
        }
        assertTrue(judged != null, "the referee did not look");
        if (judged.kind == Event.WIPE_OK) {
            assertTrue(kevin.alive());
            assertEquals(0, m.score(0));
        } else {
            assertFalse(kevin.alive());
            assertEquals(painted + Match.RESPAWN + Match.WIPE_PENALTY, kevin.respawnAt());
        }
        // one wipe per life
        m.step(new Input[] {Input.IDLE, Input.IDLE});
        m.step(new Input[] {Input.IDLE, wipe});
        for (Event e : m.events()) {
            assertFalse(e.kind == Event.WIPE_OK || e.kind == Event.WIPE_CAUGHT);
        }
    }

    @Test
    void lagCompensationMovesTheShotForward() {
        List<Slot> s = new ArrayList<Slot>();
        s.add(new Slot("Gary", 0, "pump", 0));
        s.add(new Slot("Kevin", 1, "pump", 0));
        Match now = new Match(5, Catalog.arena("lot7"), Mode.TEAM_PAINT, s);
        Match lagged = new Match(5, Catalog.arena("lot7"), Mode.TEAM_PAINT, s);
        for (int i = 0; i < 10; i++) {
            now.step(null);
            lagged.step(null);
        }
        now.step(new Input[] {new Input(0, 0, 0, 0, Input.FIRE, now.tick()), null});
        lagged.step(new Input[] {new Input(0, 0, 0, 0, Input.FIRE, lagged.tick() - Match.MAX_LAG_TICKS - 4), null});
        Ball a = now.balls().get(0);
        Ball b = lagged.balls().get(0);
        assertTrue(b.z() - a.z() >= Match.MAX_LAG_TICKS * 1900, "moved " + (b.z() - a.z()));
    }

    /// Rewrites one player's position in a snapshot (layout from Match/Player.write).
    static void place(int[] snap, Match m, int index, int x, int z, int yaw) {
        int header = 1 + 1 + 2 + 2 + 2 + 1 + 1 + 2 * 6 + 1;
        int perPlayer = 23 + 6;
        int base = header + index * perPlayer;
        snap[base] = x;
        snap[base + 1] = 0;
        snap[base + 2] = z;
        snap[base + 4] = yaw;
        assertEquals(m.playerCount(), snap[header - 1]);
    }
}
