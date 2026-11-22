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
