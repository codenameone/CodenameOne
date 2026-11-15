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
package com.codenameone.examples.brickyard.jobs;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codenameone.examples.brickyard.Clock;
import com.codenameone.examples.brickyard.api.JobDto;
import com.codenameone.examples.brickyard.domain.JobRun;
import com.codenameone.examples.brickyard.economy.Economy;
import com.codenameone.examples.brickyard.progress.Seasons;
import com.codenameone.examples.brickyard.repository.JobRunRepository;
import java.io.IOException;

/// The platform's scheduled jobs, run by the backend's own scheduler.
///
/// Engagement payouts and Plus stipends run once a UTC day: a [JobRun] row
/// keyed `job|day` is written after each run and a run that finds it does
/// nothing, so restarts and several server instances (each also held off by
/// the scheduler's database lock) never pay a day twice. Both jobs are
/// safe to repeat anyway -- payouts mark the play they paid, a stipend is
/// paid once a month per subscription -- which is why an admin may still
/// run them by hand from the console. The season rollover runs hourly.
@Component
public class ScheduledJobs {
    public static final String ENGAGEMENT = "engagement-payouts";
    public static final String STIPENDS = "plus-stipends";

    /// A job body.
    public interface Job {
        JobDto run() throws IOException;
    }

    private final Economy economy;
    private final Seasons seasons;
    private final JobRunRepository runs;

    public ScheduledJobs(Economy economy, Seasons seasons, JobRunRepository runs) {
        this.economy = economy;
        this.seasons = seasons;
        this.runs = runs;
    }

    @Scheduled(cron = "0 10 0 * * *", lock = "brickyard-engagement-payouts")
    public void engagementPayouts() throws IOException {
        daily(ENGAGEMENT, economy::payEngagement);
    }

    @Scheduled(cron = "0 20 0 * * *", lock = "brickyard-plus-stipends")
    public void plusStipends() throws IOException {
        daily(STIPENDS, economy::payStipends);
    }

    @Scheduled(cron = "0 0 * * * *", lock = "brickyard-season-rollover")
    public void seasonRollover() throws IOException {
        seasons.rollOver();
    }

    /// Runs `job` unless it already ran today; answers what it did, or null
    /// when today's run was already done.
    public JobDto daily(String name, Job job) throws IOException {
        long day = Clock.dayOf(Clock.now());
        String id = name + "|" + day;
        if (runs.findById(id) != null) {
            return null;
        }
        JobDto done = job.run();
        JobRun r = new JobRun();
        r.setId(id);
        r.setJob(name);
        r.setDay(day);
        String detail = done.processed() + " processed, " + done.bricks() + " Bricks";
        r.setDetail(detail.length() > 200 ? detail.substring(0, 200) : detail);
        r.setCreatedAt(Clock.now());
        runs.insert(r);
        return done;
    }
}
