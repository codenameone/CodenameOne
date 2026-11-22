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
package com.codenameone.examples.papercut.api;

/// What a submitted run can honestly claim, and nothing else.
///
/// The server does not play a run again to check it: the game lives in the
/// app (`play/Arena`), and keeping a second copy of it on the server, in step
/// across every change, costs more than the leaderboards of a sample are
/// worth. Instead a run is held to arithmetic the game cannot exceed, and a
/// run past it is refused, while one that is merely remarkable is kept but
/// flagged for the console. A cheat that stays under the bound gets a
/// plausible score; that is the trade, and the place to tighten it is here.
///
/// `ArenaTest` in the app plays the real game against these numbers, so a
/// change to the game that breaks them fails a test, not a leaderboard.
public final class ScoreBounds {
    /// The most any one creature is worth before the multiplier.
    public static final int MAX_POINTS = 250;
    /// The multiplier's ceiling.
    public static final int MAX_MULTIPLIER = 20;
    /// The most creatures a run can cut up in a second, averaged over the run.
    public static final int MAX_KILLS_PER_SECOND = 16;
    /// The longest a run can last, in seconds: an hour.
    public static final int MAX_SECONDS = 3600;
    /// A score per second above this is flagged for a person to look at.
    public static final int SUSPICIOUS_PER_SECOND = 6000;

    private ScoreBounds() {
    }

    /// True when `score` could have come from `kills` cuts in `seconds`.
    public static boolean possible(long score, int kills, int seconds) {
        if (score < 0 || kills < 0 || seconds <= 0 || seconds > MAX_SECONDS) {
            return false;
        }
        if (kills > (long) seconds * MAX_KILLS_PER_SECOND + 40) {
            return false;
        }
        return score <= (long) kills * MAX_POINTS * MAX_MULTIPLIER;
    }

    /// True when a possible run is still worth a second look.
    public static boolean suspicious(long score, int seconds) {
        return seconds > 0 && score / seconds > SUSPICIOUS_PER_SECOND;
    }
}
