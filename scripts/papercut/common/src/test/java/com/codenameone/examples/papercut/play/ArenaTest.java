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
package com.codenameone.examples.papercut.play;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codenameone.examples.papercut.api.ScoreBounds;
import org.junit.jupiter.api.Test;

/// The game with nobody watching: the [Bot] plays runs on many seeds, with no
/// display, and the results are held to what the server will accept.
class ArenaTest {
    /// Plays `mode` on `seed` with a bot of `skill` until the run ends or
    /// `limit` seconds pass.
    static Arena play(int mode, long seed, float skill, float limit) {
        Arena a = new Arena(mode, seed);
        Bot bot = new Bot(skill);
        while (!a.over && a.time < limit) {
            bot.think(a, Arena.STEP);
            a.step(Arena.STEP);
        }
        return a;
    }

    @Test
    void everyRunTheGameCanProduceIsOneTheServerAccepts() {
        for (int mode = 0; mode < 3; mode++) {
            for (long seed = 1; seed <= 12; seed++) {
                Arena a = play(mode, seed * 7919, seed % 3 == 0 ? 0.3f : 0.9f, 420);
                int secs = Math.max(1, (int) Math.ceil(a.time));
                assertTrue(ScoreBounds.possible(a.score, a.kills + a.gates, secs),
                        "mode " + mode + " seed " + seed + ": " + a.score + " from " + a.kills + " in " + secs + " s");
                assertTrue(a.multiplier <= ScoreBounds.MAX_MULTIPLIER);
            }
        }
    }

    @Test
    void theMultiplierAndPointsAgreeWithTheBound() {
        assertEquals(ScoreBounds.MAX_MULTIPLIER, Arena.MAX_MULTIPLIER);
        for (int k = 0; k < Enemy.KINDS; k++) {
            assertTrue(Enemy.POINTS[k] <= ScoreBounds.MAX_POINTS);
        }
    }

    @Test
    void aGoodBotSurvivesTheFirstMinuteAndScores() {
        int survived = 0;
        for (long seed = 1; seed <= 10; seed++) {
            Arena a = play(Arena.CLASSIC, seed, 0.9f, 60);
            if (!a.over) {
                survived++;
            }
            assertTrue(a.kills > 20, "seed " + seed + " kills " + a.kills);
        }
        assertTrue(survived >= 6, "survived " + survived + " of 10");
    }

    @Test
    void runsEndEventually() {
        // Pressure has to win in the end, or a leaderboard is a test of patience.
        int ended = 0;
        for (long seed = 1; seed <= 6; seed++) {
            if (play(Arena.CLASSIC, seed, 0.9f, 900).over) {
                ended++;
            }
        }
        assertTrue(ended >= 5, "ended " + ended + " of 6");
    }

    @Test
    void foldModeBurstsGates() {
        Arena a = play(Arena.FOLD, 42, 0.9f, 90);
        assertTrue(a.gates > 0, "gates " + a.gates);
        assertTrue(a.shots.isEmpty(), "fold has no pencil");
    }

    @Test
    void theDailySeedDealsTheSameCreatures() {
        Arena a = new Arena(Arena.DAILY, 20261120L);
        Arena b = new Arena(Arena.DAILY, 20261120L);
        for (int i = 0; i < 600; i++) {
            a.step(Arena.STEP);
            b.step(Arena.STEP);
        }
        assertEquals(a.enemies.size(), b.enemies.size());
        for (int i = 0; i < a.enemies.size(); i++) {
            assertEquals(a.enemies.get(i).kind, b.enemies.get(i).kind);
            assertEquals(a.enemies.get(i).x, b.enemies.get(i).x, 0.001f);
        }
    }

    @Test
    void theSheetSettlesBackFlat() {
        Sheet s = new Sheet(400, 400, 40);
        s.poke(200, 200, 150, 800);
        for (int i = 0; i < 600; i++) {
            s.update(Arena.STEP);
        }
        assertEquals(200, s.x(5, 5), 0.5f);
        assertFalse(Float.isNaN(s.y(3, 7)));
    }
}
