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
package com.codenameone.examples.brickyard.leaderboard;

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.brickyard.api.LeaderDto;
import com.codenameone.examples.brickyard.api.LeaderboardApiServer;
import com.codenameone.examples.brickyard.api.ScoreWriteDto;
import com.codenameone.examples.brickyard.api.StatBoundDto;
import com.codenameone.examples.brickyard.api.StatDto;

import java.util.List;

/// The leaderboard routes; the work is in [Leaderboards].
@Component
public class LeaderboardEndpoint implements LeaderboardApiServer {
    private final Leaderboards boards;

    public LeaderboardEndpoint(Leaderboards boards) {
        this.boards = boards;
    }

    @Override
    public LeaderDto write(String experienceId, ScoreWriteDto body) throws Exception {
        return boards.write(experienceId, body);
    }

    @Override
    public List<LeaderDto> top(String experienceId, String stat, String period, int limit) throws Exception {
        return boards.top(experienceId, stat, period, limit);
    }

    @Override
    public List<LeaderDto> friends(String experienceId, String stat, String period) throws Exception {
        return boards.friends(experienceId, stat, period);
    }

    @Override
    public List<StatDto> stats(String id) throws Exception {
        List<StatDto> out = new java.util.ArrayList<StatDto>();
        for (String name : boards.stats(id)) {
            out.add(new StatDto(name));
        }
        return out;
    }

    @Override
    public StatBoundDto setBound(String id, StatBoundDto body) throws Exception {
        return boards.setBound(id, body);
    }
}
