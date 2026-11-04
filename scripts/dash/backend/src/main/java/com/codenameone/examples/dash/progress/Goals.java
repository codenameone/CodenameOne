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
package com.codenameone.examples.dash.progress;

/// The quests and achievements, as data. A quest or an achievement counts one
/// of the [Progress][com.codenameone.examples.dash.domain.Progress]
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
        new Goal("win-1", "Win a race", "", WINS, 1, 25, 0, 40),
        new Goal("win-2", "Win 2 races", "", WINS, 2, 45, 0, 70),
        new Goal("play-3", "Run 3 races", "", RACES, 3, 35, 0, 50),
        new Goal("metres-2000", "Run 2,000 metres", "", METRES, 2000, 30, 0, 40),
        new Goal("pickups-60", "Grab 60 coins", "", PICKUPS, 60, 35, 0, 45),
        new Goal("podium-3", "Finish 3 races on the podium", "", PODIUMS, 3, 50, 0, 70),
    };

    static final Goal[] ACHIEVEMENTS = {
        new Goal("recruit", "Recruit", "Finish the tutorial run", TUTORIAL, 1, 0, 10, 50),
        new Goal("first-blood", "First Blood", "Win your first race", WINS, 1, 0, 10, 50),
        new Goal("veteran", "Veteran", "Run 10 races", RACES, 10, 0, 15, 80),
        new Goal("conqueror", "Conqueror", "Win 10 races", WINS, 10, 0, 25, 120),
        new Goal("medalist", "Medalist", "Finish 10 races on the podium", PODIUMS, 10, 0, 20, 100),
        new Goal("marathon", "Marathon", "Run 42,195 metres", METRES, 42195, 0, 20, 100),
        new Goal("magpie", "Magpie", "Grab 1,000 coins", PICKUPS, 1000, 0, 20, 100),
        new Goal("unstoppable", "Unstoppable", "Win 3 races in a row", STREAK, 3, 0, 25, 120),
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
