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
package com.codenameone.examples.splat.progress;

/// The quests and achievements, as data. A quest or an achievement counts one
/// of the [Progress][com.codenameone.examples.splat.domain.Progress]
/// statistics; adding one is a line here.
public final class Goals {
    public static final int WINS = 0;
    public static final int RACES = 1;
    public static final int METRES = 2;
    public static final int PICKUPS = 3;
    public static final int PODIUMS = 4;
    public static final int STREAK = 5;
    public static final int TUTORIAL = 6;
    public static final int LEVEL = 7;

    /// A goal: the statistic it counts, how many, and what reaching it pays.
    public static final class Goal {
        public final String id;
        public final String title;
        public final String detail;
        public final int stat;
        public final int goal;
        public final int coins;
        public final int gems;
        public final int xp;

        Goal(String id, String title, String detail, int stat, int goal, int coins, int gems, int xp) {
            this.id = id;
            this.title = title;
            this.detail = detail;
            this.stat = stat;
            this.goal = goal;
            this.coins = coins;
            this.gems = gems;
            this.xp = xp;
        }
    }

    /// The daily pool. Each player gets three of these a day, picked by
    /// [#questsFor].
    static final Goal[] QUESTS = {
        new Goal("win-1", "Win a match", "", WINS, 1, 25, 0, 40),
        new Goal("win-2", "Win 2 matches", "", WINS, 2, 45, 0, 70),
        new Goal("play-3", "Play 3 matches", "", RACES, 3, 35, 0, 50),
        new Goal("paints-15", "Paint 15 people", "", METRES, 15, 30, 0, 40),
        new Goal("wipes-2", "Get away with 2 wipes", "", PICKUPS, 2, 35, 0, 45),
        new Goal("mvp-1", "Be the top painter once", "", PODIUMS, 1, 50, 0, 70),
    };

    static final Goal[] ACHIEVEMENTS = {
        new Goal("recruit", "Waiver Signed", "Finish the safety briefing", TUTORIAL, 1, 0, 10, 50),
        new Goal("first-blood", "First Paint", "Win your first match", WINS, 1, 0, 10, 50),
        new Goal("veteran", "Weekend Veteran", "Play 10 matches", RACES, 10, 0, 15, 80),
        new Goal("conqueror", "Field Marshal", "Win 10 matches", WINS, 10, 0, 25, 120),
        new Goal("medalist", "Most Valuable Painter", "Top painter in 10 matches", PODIUMS, 10, 0, 20, 100),
        new Goal("marathon", "Abstract Expressionist", "Paint 500 people", METRES, 500, 0, 20, 100),
        new Goal("magpie", "It Was A Bounce", "Get away with 25 wipes", PICKUPS, 25, 0, 20, 100),
        new Goal("unstoppable", "Unbearable At Barbecues", "Win 3 matches in a row", STREAK, 3, 0, 25, 120),
        new Goal("seasoned", "Seasoned", "Reach level 5", LEVEL, 5, 0, 30, 0),
    };

    private Goals() {
    }

    /// Today's three quests for a player: the same all day, different between
    /// players and days, and never two alike.
    static Goal[] questsFor(String username, long day) {
        int h = (username + ":" + day).hashCode();
        int first = (h & 0x7fffffff) % QUESTS.length;
        int step = 1 + ((h >>> 8) & 0x7fffffff) % (QUESTS.length - 1);
        Goal[] out = new Goal[3];
        for (int n = 0; n < 3; n++) {
            int at = (first + n * step) % QUESTS.length;
            while (taken(out, n, QUESTS[at])) {
                at = (at + 1) % QUESTS.length;
            }
            out[n] = QUESTS[at];
        }
        return out;
    }

    private static boolean taken(Goal[] picked, int count, Goal g) {
        for (int i = 0; i < count; i++) {
            if (picked[i] == g) {
                return true;
            }
        }
        return false;
    }

    static Goal quest(String id) {
        for (Goal g : QUESTS) {
            if (g.id.equals(id)) {
                return g;
            }
        }
        return null;
    }

    /// Experience at which `level` starts: 0, 100, 300, 600, 1000...
    public static int levelStart(int level) {
        return 50 * level * (level - 1);
    }

    public static int levelOf(int xp) {
        int level = 1;
        while (xp >= levelStart(level + 1)) {
            level++;
        }
        return level;
    }
}
