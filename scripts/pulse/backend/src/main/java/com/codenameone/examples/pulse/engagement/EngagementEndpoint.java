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
package com.codenameone.examples.pulse.engagement;

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.pulse.Caller;
import com.codenameone.examples.pulse.api.AwayDto;
import com.codenameone.examples.pulse.api.EngagementApiServer;
import com.codenameone.examples.pulse.api.HeartRateDto;
import com.codenameone.examples.pulse.api.NudgeDto;
import com.codenameone.examples.pulse.api.StatsDto;
import com.codenameone.examples.pulse.api.WorkoutDto;

import java.util.List;

/// The server's half of `EngagementApi`.
@Component
public class EngagementEndpoint implements EngagementApiServer {
    private final Engagement engagement;

    public EngagementEndpoint(Engagement engagement) {
        this.engagement = engagement;
    }

    @Override
    public StatsDto stats() throws Exception {
        return engagement.stats(Caller.name());
    }

    @Override
    public List<AwayDto> away() throws Exception {
        return engagement.away(Caller.name());
    }

    @Override
    public AwayDto addAway(AwayDto body) throws Exception {
        return engagement.addAway(Caller.name(), body);
    }

    @Override
    public AwayDto removeAway(String id) throws Exception {
        return engagement.removeAway(Caller.name(), id);
    }

    @Override
    public List<NudgeDto> nudges() throws Exception {
        return engagement.nudges(Caller.name());
    }

    @Override
    public NudgeDto opened(String id) throws Exception {
        return engagement.opened(Caller.name(), id);
    }

    @Override
    public WorkoutDto addWorkout(WorkoutDto body) throws Exception {
        return engagement.addWorkout(Caller.name(), body);
    }

    @Override
    public List<WorkoutDto> workouts() throws Exception {
        return engagement.workouts(Caller.name());
    }

    @Override
    public HeartRateDto heartRate(HeartRateDto body) throws Exception {
        return engagement.heartRate(Caller.name(), body);
    }
}
