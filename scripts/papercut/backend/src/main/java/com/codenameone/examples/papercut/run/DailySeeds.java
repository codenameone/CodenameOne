/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
 */
package com.codenameone.examples.papercut.run;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.papercut.Clock;
import com.codenameone.examples.papercut.Ids;
import com.codenameone.examples.papercut.domain.DailySeed;
import com.codenameone.examples.papercut.repository.DailySeedRepository;
import java.io.IOException;
import java.time.Instant;

/// The seed of the daily sheet: the same for every player on a UTC day, so
/// the day's board compares like with like.
///
/// It is a hash of the date unless an admin has picked one for that day.
/// Worked out rather than stored, so a day nobody played still has a seed and
/// two servers agree on it without talking.
@Component
public class DailySeeds {
    private final DailySeedRepository overrides;

    public DailySeeds(DailySeedRepository overrides) {
        this.overrides = overrides;
    }

    @Transactional(readOnly = true)
    public long seedOf(long millis) throws IOException {
        String date = Clock.date(millis);
        DailySeed picked = overrides.findById(date);
        return picked != null ? picked.getSeed() : hashed(date);
    }

    /// The seed a date gets when nobody picked one: the first 60 bits of the
    /// SHA-256 of `papercut-daily:` and the date. Positive, so it reads the
    /// same in every log.
    public static long hashed(String date) throws IOException {
        return Long.parseLong(Ids.sha256("papercut-daily:" + date).substring(0, 15), 16);
    }

    /// Overrides today's seed.
    @Transactional(rollbackFor = IOException.class)
    public void pick(String admin, long seed) throws IOException {
        String date = Clock.date(Clock.now());
        DailySeed row = overrides.findById(date);
        boolean fresh = row == null;
        if (fresh) {
            row = new DailySeed();
            row.setDay(date);
        }
        row.setSeed(seed);
        row.setSetBy(admin);
        row.setCreatedAt(Instant.now());
        if (fresh) {
            overrides.insert(row);
        } else {
            overrides.save(row);
        }
    }
}
