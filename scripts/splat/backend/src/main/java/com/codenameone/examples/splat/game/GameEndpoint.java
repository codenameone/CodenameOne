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
package com.codenameone.examples.splat.game;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.orm.repository.PageRequest;
import com.codename1.appkit.server.Caller;
import com.codenameone.examples.splat.account.Lists;
import com.codenameone.examples.splat.admin.Flags;
import com.codenameone.examples.splat.api.ChallengeDto;
import com.codenameone.examples.splat.api.CrewCreateDto;
import com.codenameone.examples.splat.api.CrewDto;
import com.codenameone.examples.splat.api.FlagDto;
import com.codenameone.examples.splat.api.GameApiServer;
import com.codenameone.examples.splat.api.LeaderDto;
import com.codenameone.examples.splat.api.RaceSummaryDto;
import com.codenameone.examples.splat.api.ReplayDto;
import com.codenameone.examples.splat.api.ReportDto;
import com.codenameone.examples.splat.api.RunnerDto;
import com.codenameone.examples.splat.domain.Profile;
import com.codenameone.examples.splat.domain.RaceEntry;
import com.codenameone.examples.splat.domain.RaceRecord;
import com.codenameone.examples.splat.repository.ProfileRepository;
import com.codenameone.examples.splat.repository.RaceEntryRepository;
import com.codenameone.examples.splat.repository.RaceRecordRepository;
import com.codenameone.examples.splat.shop.Catalog;
import com.codenameone.examples.splat.social.Crews;
import java.util.ArrayList;
import java.util.List;

@Component
public class GameEndpoint implements GameApiServer {
    private final ProfileRepository profiles;
    private final RaceRecordRepository races;
    private final RaceEntryRepository entries;
    private final Challenges challenges;
    private final Flags flags;
    private final Crews crews;

    public GameEndpoint(ProfileRepository profiles, RaceRecordRepository races, RaceEntryRepository entries,
            Challenges challenges, Flags flags, Crews crews) {
        this.profiles = profiles;
        this.races = races;
        this.entries = entries;
        this.challenges = challenges;
        this.flags = flags;
        this.crews = crews;
    }

    @Override
    @Transactional(readOnly = true)
    public RunnerDto runner() throws Exception {
        return new RunnerDto(profile(Caller.name()).getRunner());
    }

    /// The runner is a look, never an advantage: every runner has the same
    /// physics. Only one the player owns may be picked.
    @Override
    @Transactional
    public RunnerDto saveRunner(RunnerDto runner) throws Exception {
        String id = runner == null ? null : runner.runner();
        Profile p = profile(Caller.name());
        if (id == null || !Catalog.isRunner(id) || !Catalog.FREE_RUNNER.equals(id)
                && !Lists.split(p.getOwned()).contains(id)) {
            throw new ResponseStatusException(400, "You do not own that runner");
        }
        p.setRunner(id);
        profiles.save(p);
        return new RunnerDto(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RaceSummaryDto> history() throws Exception {
        return summaries(entries.history(Caller.name(), new PageRequest(0, 30)));
    }

    public static List<RaceSummaryDto> summaries(List<RaceEntry> rows) {
        List<RaceSummaryDto> out = new ArrayList<RaceSummaryDto>();
        for (RaceEntry e : rows) {
            out.add(new RaceSummaryDto(e.getRaceId(), e.getLevel(), e.getPlace(), e.getRunners(), e.getScore(),
                    e.getTrophyChange(), e.getEndedAt().toEpochMilli(), e.getTicks()));
        }
        return out;
    }

    /// A finished match. Anyone signed in may watch: replays are how players
    /// learn, and they hold nothing private.
    @Override
    @Transactional(readOnly = true)
    public ReplayDto replay(String id) throws Exception {
        RaceRecord m = id == null ? null : races.findById(id);
        if (m == null) {
            throw new ResponseStatusException(404, "No such race");
        }
        List<Integer> points = new ArrayList<Integer>();
        for (String v : Lists.split(m.getScores())) {
            points.add(Integer.valueOf(Integer.parseInt(v)));
        }
        return new ReplayDto(m.getId(), m.getLevel(), Lists.split(m.getNames()), points, m.getReplay(),
                m.getRulesVersion(), m.getEndedAt().toEpochMilli());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaderDto> leaderboard(int limit) throws Exception {
        int n = limit < 1 || limit > 100 ? 50 : limit;
        List<LeaderDto> out = new ArrayList<LeaderDto>();
        int rank = 1;
        for (Profile p : profiles.leaders(new PageRequest(0, n))) {
            out.add(new LeaderDto(rank++, p.getDisplayName(), p.getTrophies(), p.getWins()));
        }
        return out;
    }

    @Override
    public ChallengeDto challenge() throws Exception {
        return challenges.create(Caller.name());
    }

    @Override
    public List<CrewDto> crews() throws Exception {
        return crews.of(Caller.name());
    }

    @Override
    public CrewDto createCrew(CrewCreateDto crew) throws Exception {
        return crews.create(Caller.name(), crew == null ? null : crew.name());
    }

    @Override
    public CrewDto joinCrew(String code) throws Exception {
        return crews.join(Caller.name(), code);
    }

    @Override
    public CrewDto leaveCrew(String code) throws Exception {
        return crews.leave(Caller.name(), code);
    }

    @Override
    public FlagDto report(ReportDto report) throws Exception {
        if (report == null) {
            throw new ResponseStatusException(400, "Nothing to report");
        }
        return flags.report(Caller.name(), report.raceId(), report.player(), report.reason());
    }

    private Profile profile(String username) {
        Profile p = profiles.findById(username);
        if (p == null) {
            throw new ResponseStatusException(404, "No such account");
        }
        return p;
    }
}
