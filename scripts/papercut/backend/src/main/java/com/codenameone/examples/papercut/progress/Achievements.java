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
package com.codenameone.examples.papercut.progress;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.papercut.account.Lists;
import com.codenameone.examples.papercut.api.AchievementDto;
import com.codenameone.examples.papercut.api.ProgressApiServer;
import com.codenameone.examples.papercut.domain.Profile;
import com.codenameone.examples.papercut.domain.ScoreRun;
import com.codenameone.examples.papercut.repository.ProfileRepository;
import com.codenameone.examples.papercut.repository.ScoreRunRepository;
import com.codenameone.examples.papercut.run.Modes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// Achievements, and `ProgressApi` on the server. Nothing is stored: each is
/// worked out from the player's standing runs whenever asked, so an admin
/// striking a run takes back what it unlocked, and a new achievement counts
/// every run played before it existed.
@Component
public class Achievements implements ProgressApiServer {
    private final ScoreRunRepository runs;
    private final ProfileRepository profiles;

    public Achievements(ScoreRunRepository runs, ProfileRepository profiles) {
        this.runs = runs;
        this.profiles = profiles;
    }

    @Override
    public List<AchievementDto> achievements() throws Exception {
        return of(Caller.name());
    }

    @Transactional(readOnly = true)
    public List<AchievementDto> of(String username) {
        long played = 0;
        long bestClassic = 0;
        long bestGates = 0;
        long longest = 0;
        long bestMultiplier = 0;
        Set<Long> dailyDays = new HashSet<Long>();
        for (ScoreRun r : runs.of(username)) {
            played++;
            if (Modes.CLASSIC.equals(r.getMode())) {
                bestClassic = Math.max(bestClassic, r.getScore());
            }
            if (Modes.FOLD.equals(r.getMode())) {
                bestGates = Math.max(bestGates, r.getGates());
            }
            if (Modes.DAILY.equals(r.getMode())) {
                dailyDays.add(r.getDay());
            }
            longest = Math.max(longest, r.getSeconds());
            bestMultiplier = Math.max(bestMultiplier, r.getPeakMultiplier());
        }
        Profile p = profiles.findById(username);
        long papers = p == null ? 0 : Lists.split(p.getOwned()).size();
        List<AchievementDto> out = new ArrayList<AchievementDto>();
        out.add(goal("first-cut", "First Cut", "Finish a run", played, 1));
        out.add(goal("classic-100k", "Sharp", "Score 100,000 in a classic run", bestClassic, 100000));
        out.add(goal("classic-500k", "Razor", "Score 500,000 in a classic run", bestClassic, 500000));
        out.add(goal("fold-25", "Origamist", "Pass 25 gates in one fold run", bestGates, 25));
        out.add(goal("survive-3m", "Tough Paper", "Survive 3 minutes", longest, 180));
        out.add(goal("daily-10", "Regular", "Play 10 dailies", dailyDays.size(), 10));
        out.add(goal("multiplier-20", "In the Margin", "Reach the x20 multiplier", bestMultiplier, 20));
        out.add(goal("papers-3", "Stationer", "Own 3 papers", papers, 3));
        return out;
    }

    private static AchievementDto goal(String id, String name, String description, long progress, long goal) {
        return new AchievementDto(id, name, description, progress >= goal, Math.min(progress, goal), goal);
    }
}
