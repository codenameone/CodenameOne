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

import com.codename1.appkit.server.Caller;
import com.codename1.appkit.server.Text;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.brickyard.Access;
import com.codenameone.examples.brickyard.Clock;
import com.codenameone.examples.brickyard.Ids;
import com.codenameone.examples.brickyard.account.Accounts;
import com.codenameone.examples.brickyard.api.LeaderDto;
import com.codenameone.examples.brickyard.api.ScoreWriteDto;
import com.codenameone.examples.brickyard.api.StatBoundDto;
import com.codenameone.examples.brickyard.domain.CheatFlag;
import com.codenameone.examples.brickyard.domain.Experience;
import com.codenameone.examples.brickyard.domain.LeaderboardEntry;
import com.codenameone.examples.brickyard.domain.ScoreWrite;
import com.codenameone.examples.brickyard.domain.StatBound;
import com.codenameone.examples.brickyard.experience.Experiences;
import com.codenameone.examples.brickyard.repository.CheatFlagRepository;
import com.codenameone.examples.brickyard.repository.LeaderboardEntryRepository;
import com.codenameone.examples.brickyard.repository.ScoreWriteRepository;
import com.codenameone.examples.brickyard.repository.StatBoundRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Leaderboards, kept as one total per player, stat and day. Writes are
/// server-authoritative: only an engine server reports a stat change, and a
/// change that moves a stat further in a minute than the experience allows is
/// refused and recorded as a [CheatFlag].
@Component
public class Leaderboards {
    /// The bound for a stat whose experience never set one.
    public static final long DEFAULT_MAX_PER_MINUTE = 1000;
    public static final String DAILY = "daily";
    public static final String WEEKLY = "weekly";
    public static final String ALL_TIME = "all";

    private final LeaderboardEntryRepository entries;
    private final ScoreWriteRepository writes;
    private final StatBoundRepository bounds;
    private final CheatFlagRepository flags;
    private final Experiences experiences;
    private final Accounts accounts;

    public Leaderboards(LeaderboardEntryRepository entries, ScoreWriteRepository writes,
            StatBoundRepository bounds, CheatFlagRepository flags, Experiences experiences, Accounts accounts) {
        this.entries = entries;
        this.writes = writes;
        this.bounds = bounds;
        this.flags = flags;
        this.experiences = experiences;
        this.accounts = accounts;
    }

    /// The stats `experienceId` has entries for, sorted: what a leaderboard
    /// screen offers to pick from.
    @Transactional(readOnly = true)
    public List<String> stats(String experienceId) {
        experiences.visible(experienceId);
        java.util.TreeSet<String> names = new java.util.TreeSet<String>();
        for (LeaderboardEntry e : entries.findByExperienceId(experienceId)) {
            names.add(e.getStat());
        }
        return new java.util.ArrayList<String>(names);
    }

    /// Not transactional on purpose: a refused write still has to leave its flag behind.
    public LeaderDto write(String experienceId, ScoreWriteDto score) throws IOException {
        Access.requireEngine();
        experiences.find(experienceId);
        if (score == null) {
            throw new ResponseStatusException(400, "There is no score");
        }
        return record(experienceId, score.stat(), accounts.account(score.userId()).getUsername(), score.delta());
    }

    /// Adds `delta` to `user`'s `stat` in today's entry, after the per-minute
    /// bound; what a live server's leaderstats report goes through here too.
    public LeaderDto record(String experienceId, String statName, String user, long delta) throws IOException {
        String stat = stat(statName);
        long now = Clock.now();
        long moved = Math.abs(delta);
        for (ScoreWrite w : writes.since(experienceId, stat, user, now - Clock.MINUTE)) {
            moved += Math.abs(w.getDelta());
        }
        long max = maxPerMinute(experienceId, stat);
        if (moved > max) {
            CheatFlag f = new CheatFlag();
            f.setId(Ids.next());
            f.setExperienceId(experienceId);
            f.setStat(stat);
            f.setUserId(user);
            f.setDelta(delta);
            f.setReason(stat + " moved " + moved + " in a minute; the bound is " + max);
            f.setCreatedAt(now);
            flags.insert(f);
            throw new ResponseStatusException(422, "That change is out of bounds and was flagged");
        }
        ScoreWrite w = new ScoreWrite();
        w.setId(Ids.next());
        w.setExperienceId(experienceId);
        w.setStat(stat);
        w.setUserId(user);
        w.setDelta(delta);
        w.setWrittenAt(now);
        writes.insert(w);
        long day = Clock.dayOf(now);
        List<LeaderboardEntry> today = entries.day(experienceId, stat, user, day);
        LeaderboardEntry e;
        if (today.isEmpty()) {
            e = new LeaderboardEntry();
            e.setId(Ids.next());
            e.setExperienceId(experienceId);
            e.setStat(stat);
            e.setUserId(user);
            e.setDayStart(day);
            e.setStatValue(delta);
            entries.insert(e);
        } else {
            e = today.get(0);
            e.setStatValue(e.getStatValue() + delta);
            entries.save(e);
        }
        return new LeaderDto(0, publicId(user), accounts.publicName(user), e.getStatValue());
    }

    @Transactional(readOnly = true)
    public List<LeaderDto> top(String experienceId, String stat, String period, int limit) {
        experiences.visible(experienceId);
        int n = limit <= 0 ? 10 : Math.min(limit, 100);
        return rank(totals(experienceId, stat(stat), period, null), n);
    }

    /// The caller and their friends only.
    @Transactional(readOnly = true)
    public List<LeaderDto> friends(String experienceId, String stat, String period) {
        experiences.visible(experienceId);
        List<String> who = new ArrayList<String>(accounts.friendsOf(Caller.name()));
        who.add(Caller.name());
        return rank(totals(experienceId, stat(stat), period, who), 100);
    }

    @Transactional(rollbackFor = IOException.class)
    public StatBoundDto setBound(String experienceId, StatBoundDto bound) {
        Experience e = experiences.find(experienceId);
        Access.requireOwner(e);
        if (bound == null || bound.maxPerMinute() < 1) {
            throw new ResponseStatusException(400, "A bound is at least 1 a minute");
        }
        String stat = stat(bound.stat());
        String id = experienceId + "|" + stat;
        StatBound b = bounds.findById(id);
        if (b == null) {
            b = new StatBound();
            b.setId(id);
            b.setExperienceId(experienceId);
            b.setStat(stat);
            b.setMaxPerMinute(bound.maxPerMinute());
            bounds.insert(b);
        } else {
            b.setMaxPerMinute(bound.maxPerMinute());
            bounds.save(b);
        }
        return new StatBoundDto(stat, b.getMaxPerMinute());
    }

    @Transactional(readOnly = true)
    public List<CheatFlag> flagsOf(String experienceId) {
        return flags.of(experienceId);
    }

    private long maxPerMinute(String experienceId, String stat) {
        StatBound b = bounds.findById(experienceId + "|" + stat);
        return b == null ? DEFAULT_MAX_PER_MINUTE : b.getMaxPerMinute();
    }

    private Map<String, Long> totals(String experienceId, String stat, String period, List<String> only) {
        long today = Clock.dayOf(Clock.now());
        long from;
        if (period == null || period.length() == 0 || ALL_TIME.equals(period)) {
            from = 0;
        } else if (DAILY.equals(period)) {
            from = today;
        } else if (WEEKLY.equals(period)) {
            from = today - 6 * Clock.DAY;
        } else {
            throw new ResponseStatusException(400, "A period is daily, weekly or all");
        }
        Map<String, Long> out = new HashMap<String, Long>();
        for (LeaderboardEntry e : entries.since(experienceId, stat, from)) {
            if (only == null || only.contains(e.getUserId())) {
                Long sum = out.get(e.getUserId());
                out.put(e.getUserId(), (sum == null ? 0L : sum.longValue()) + e.getStatValue());
            }
        }
        return out;
    }

    private List<LeaderDto> rank(Map<String, Long> totals, int limit) {
        List<Map.Entry<String, Long>> sorted = new ArrayList<Map.Entry<String, Long>>(totals.entrySet());
        Collections.sort(sorted, new Comparator<Map.Entry<String, Long>>() {
            @Override
            public int compare(Map.Entry<String, Long> a, Map.Entry<String, Long> b) {
                int byValue = b.getValue().compareTo(a.getValue());
                return byValue != 0 ? byValue : a.getKey().compareTo(b.getKey());
            }
        });
        List<String> shown = new ArrayList<String>();
        for (int i = 0; i < sorted.size() && i < limit; i++) {
            shown.add(sorted.get(i).getKey());
        }
        // Usernames are e-mail addresses: a board shows display names, fetched in one query.
        Map<String, String> names = accounts.publicNames(shown);
        List<LeaderDto> out = new ArrayList<LeaderDto>();
        for (int i = 0; i < shown.size(); i++) {
            String name = names.get(shown.get(i));
            out.add(new LeaderDto(i + 1, publicId(shown.get(i)), name == null ? "Player" : name,
                    sorted.get(i).getValue().longValue()));
        }
        return out;
    }

    private static String stat(String value) {
        return Text.required(value, 64, "the stat");
    }

    /// A stable id for a leaderboard row that is not the username, which is
    /// an e-mail address and must not reach other players.
    public static String publicId(String username) {
        return Integer.toHexString(username == null ? 0 : username.hashCode());
    }
}
