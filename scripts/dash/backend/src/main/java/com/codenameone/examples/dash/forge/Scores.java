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
package com.codenameone.examples.dash.forge;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.orm.repository.PageRequest;
import com.codenameone.examples.dash.Clock;
import com.codenameone.examples.dash.api.DailyDto;
import com.codenameone.examples.dash.api.GhostDto;
import com.codenameone.examples.dash.api.ScoreDto;
import com.codenameone.examples.dash.domain.CrewMember;
import com.codenameone.examples.dash.domain.Profile;
import com.codenameone.examples.dash.domain.Score;
import com.codenameone.examples.dash.repository.CrewMemberRepository;
import com.codenameone.examples.dash.repository.ProfileRepository;
import com.codenameone.examples.dash.repository.ScoreRepository;
import com.codenameone.examples.dash.rules.Content;
import com.codenameone.examples.dash.rules.Replay;
import com.codenameone.examples.dash.rules.Run;
import com.codenameone.examples.dash.rules.Runner;
import com.codenameone.examples.dash.social.Referrals;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// Level leaderboards. Every score is a replay the server played again
/// ([Forge#result]), so a number can never be typed in.
///
/// The `all` board takes any seed: a player could search seeds offline for a
/// kind track, which is why the timed boards exist. `week` and `day` count a
/// run only on that board's seed, the same track for everyone.
// tag::dash-scores[]
@Component
public class Scores {
    public static final String ALL = "all";
    public static final String WEEK = "week";
    public static final String DAY = "day";
    public static final String FRIENDS = "friends";
    public static final String CREW = "crew:";
    /// The daily challenge is always this level, on the day's seed.
    public static final String DAILY_LEVEL = Content.ENDLESS;

    private final ScoreRepository scores;
    private final ProfileRepository profiles;
    private final CrewMemberRepository members;
    private final Referrals referrals;

    public Scores(ScoreRepository scores, ProfileRepository profiles, CrewMemberRepository members,
            Referrals referrals) {
        this.scores = scores;
        this.profiles = profiles;
        this.members = members;
        this.referrals = referrals;
    }

    public static int today() {
        return (int) Clock.dayOf(Clock.now());
    }

    public static String weekBoard() {
        return "w" + (today() / 7);
    }

    public static String dayBoard() {
        return "d" + today();
    }

    public DailyDto daily() {
        long next = (today() + 1L) * Clock.DAY;
        return new DailyDto(today(), DAILY_LEVEL, Long.toString(Content.dailySeed(today())),
                (int) ((next - Clock.now()) / 1000));
    }

    /// Enters a verified run on every board it qualifies for, and answers the
    /// run's rank on `all` (1 is first).
    @Transactional(rollbackFor = IOException.class)
    public int submit(String username, String level, Replay replay, Run played) throws IOException {
        Profile p = profiles.findById(username);
        if (p == null || p.isBanned()) {
            return 0;
        }
        Runner r = played.runner(0);
        enter(ALL, level, p, replay, played);
        if (replay.seed() == Content.weeklySeed(today() / 7, level)) {
            enter(weekBoard(), level, p, replay, played);
        }
        if (DAILY_LEVEL.equals(level) && replay.seed() == Content.dailySeed(today())) {
            enter(dayBoard(), level, p, replay, played);
        }
        Long above = scores.above(ALL, level, r.score());
        return above == null ? 1 : above.intValue() + 1;
    }

    private void enter(String board, String level, Profile p, Replay replay, Run played) {
        Runner r = played.runner(0);
        String id = board + "|" + level + "|" + p.getUsername();
        Score s = scores.findById(id);
        if (s != null && s.getScore() >= r.score()) {
            return;
        }
        boolean fresh = s == null;
        if (fresh) {
            s = new Score();
            s.setId(id);
            s.setBoard(board);
            s.setLevel(level);
            s.setUsername(p.getUsername());
        }
        s.setDisplayName(p.getDisplayName());
        s.setScore(r.score());
        s.setDistance(r.z / 1000);
        s.setCoins(r.coins);
        s.setStars(played.stars(0));
        s.setSeed(replay.seed());
        s.setCreatedAt(Instant.now());
        s.setReplay(replay.encode());
        if (fresh) {
            scores.insert(s);
        } else {
            scores.save(s);
        }
    }

    /// A board, best first. `friends` is the caller, the players they
    /// invited and everyone in their crews; `crew:<code>` is one crew.
    @Transactional(readOnly = true)
    public List<ScoreDto> top(String username, String level, String board, int limit) throws IOException {
        int n = limit < 1 || limit > 100 ? 50 : limit;
        String key = boardKey(board);
        Set<String> only = null;
        if (FRIENDS.equals(board)) {
            only = friends(username);
        } else if (board != null && board.startsWith(CREW)) {
            only = new HashSet<String>();
            for (CrewMember m : members.members(board.substring(CREW.length()), new PageRequest(0, 100))) {
                only.add(m.getUsername());
            }
        }
        List<ScoreDto> out = new ArrayList<ScoreDto>();
        int rank = 1;
        int page = 0;
        while (out.size() < n && page < 20) {
            List<Score> rows = scores.top(key, level, new PageRequest(page, only == null ? n : 100));
            for (Score s : rows) {
                if (only != null && !only.contains(s.getUsername())) {
                    continue;
                }
                if (out.size() < n) {
                    out.add(new ScoreDto(rank, s.getDisplayName(), s.getScore(), s.getDistance(), s.getCoins(),
                            s.getStars(), s.getId()));
                }
                rank++;
            }
            if (only == null || rows.size() < 100) {
                break;
            }
            page++;
        }
        return out;
    }

    /// The run just above the caller's best: a ghost close enough to chase.
    @Transactional(readOnly = true)
    public GhostDto ghost(String username, String level, String board) throws IOException {
        String key = boardKey(board);
        Score mine = scores.findById(key + "|" + level + "|" + username);
        int target = mine == null ? 0 : mine.getScore();
        Score pick = null;
        Score best = null;
        // Best first, so the last one above the target is the closest.
        for (Score s : scores.top(key, level, new PageRequest(0, 100))) {
            if (s.getUsername().equals(username)) {
                continue;
            }
            if (best == null) {
                best = s;
            }
            if (s.getScore() > target) {
                pick = s;
            }
        }
        if (pick == null) {
            pick = best;
        }
        if (pick == null) {
            return null;
        }
        return new GhostDto(pick.getDisplayName(), pick.getScore(), pick.getReplay());
    }

    public Score byId(String id) {
        return scores.findById(id);
    }

    static String boardKey(String board) {
        if (WEEK.equals(board)) {
            return weekBoard();
        }
        if (DAY.equals(board)) {
            return dayBoard();
        }
        return ALL;
    }

    private Set<String> friends(String username) throws IOException {
        Set<String> out = new HashSet<String>();
        out.add(username);
        out.addAll(referrals.invitees(username));
        for (CrewMember mine : members.crewsOf(username, new PageRequest(0, 20))) {
            for (CrewMember m : members.members(mine.getCode(), new PageRequest(0, 100))) {
                out.add(m.getUsername());
            }
        }
        return out;
    }
}
// end::dash-scores[]
