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
package com.codenameone.examples.brickyard.analytics;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.PostMapping;
import com.codename1.backend.annotations.Profile;
import com.codename1.backend.annotations.RestController;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.brickyard.Access;
import com.codenameone.examples.brickyard.Clock;
import com.codenameone.examples.brickyard.Ids;
import com.codenameone.examples.brickyard.domain.AnalyticsEvent;
import com.codenameone.examples.brickyard.domain.Experience;
import com.codenameone.examples.brickyard.repository.AnalyticsEventRepository;
import com.codenameone.examples.brickyard.repository.ExperienceRepository;
import java.io.IOException;

/// A development server's way to give a fresh database a past: two weeks of
/// visits by a cohort of made-up players, so a creator dashboard opened on a
/// demo has a curve and a retention figure rather than one bar for today.
/// The bean only exists under `CN1_PROFILE=dev`, so no production server has
/// the endpoint at all, and even there only staff may call it.
@RestController
@Profile("dev")
public class DevHistory {
    static final int DAYS = 14;
    private final AnalyticsEventRepository events;
    private final ExperienceRepository experiences;

    public DevHistory(AnalyticsEventRepository events, ExperienceRepository experiences) {
        this.events = events;
        this.experiences = experiences;
    }

    /// Writes the history and answers how many visits it added.
    @PostMapping("/api/dev/experiences/{experienceId}/history")
    @Transactional(rollbackFor = IOException.class)
    public long history(@PathVariable("experienceId") String experienceId) throws IOException {
        if (!Access.isStaff()) {
            throw new ResponseStatusException(403, "Staff only");
        }
        Experience e = experiences.findById(experienceId);
        if (e == null) {
            throw new ResponseStatusException(404, "No such experience");
        }
        long today = Clock.dayOf(Clock.now());
        long seed = experienceId.hashCode();
        long added = 0;
        // New players arrive each day, more as the experience is discovered;
        // each returns on later days with a chance that decays with time.
        for (int day = DAYS - 1; day >= 1; day--) {
            int arrivals = 4 + (DAYS - day) / 2 + (int) (next(seed + day) % 4);
            for (int p = 0; p < arrivals; p++) {
                String player = "demo-player-" + day + "-" + p;
                for (int later = 0; later <= day; later++) {
                    long roll = next(seed * 31 + day * 977 + p * 131 + later) % 100;
                    int chance = later == 0 ? 100 : later == 1 ? 45 : later == 7 ? 22 : Math.max(8, 30 - later * 3);
                    if (roll < chance) {
                        long when = today - (day - later) * Clock.DAY;
                        insert(experienceId, player, "VISIT", 1, e.getPublishedVersion(), when);
                        insert(experienceId, player, "SESSION", 120 + roll * 9, e.getPublishedVersion(), when);
                        added++;
                    }
                }
            }
        }
        return added;
    }

    private void insert(String experienceId, String player, String kind, long amount, int version, long day)
            throws IOException {
        AnalyticsEvent a = new AnalyticsEvent();
        a.setId(Ids.next());
        a.setExperienceId(experienceId);
        a.setUsername(player);
        a.setKind(kind);
        a.setName("");
        a.setAmount(amount);
        a.setDetail("");
        a.setVersion(version);
        a.setDayStart(day);
        a.setCreatedAt(day + Clock.DAY / 2);
        events.insert(a);
    }

    private static long next(long x) {
        long z = x * 6364136223846793005L + 1442695040888963407L;
        z ^= z >>> 29;
        return (z & Long.MAX_VALUE);
    }
}
