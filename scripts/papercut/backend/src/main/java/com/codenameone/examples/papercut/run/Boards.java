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
import com.codenameone.examples.papercut.account.Accounts;
import com.codenameone.examples.papercut.api.DailyDto;
import com.codenameone.examples.papercut.api.LeaderDto;
import com.codenameone.examples.papercut.domain.Friend;
import com.codenameone.examples.papercut.domain.Profile;
import com.codenameone.examples.papercut.domain.ScoreRun;
import com.codenameone.examples.papercut.repository.FriendRepository;
import com.codenameone.examples.papercut.repository.ProfileRepository;
import com.codenameone.examples.papercut.repository.ScoreRunRepository;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// The leaderboards: each player's best standing run of a mode over a period.
///
/// A board is worked out from the runs on every request rather than kept as a
/// table of bests, because WEEK and TODAY are windows a stored best cannot
/// answer, and a struck run or a benched player has to vanish from all of
/// them at once. The query walks the period's runs best first and keeps each
/// player's first, which is cheap at a sample's scale; a game with millions of
/// runs would keep a per-period best table instead.
@Component
public class Boards {
    /// How many rows a board shows.
    public static final int TOP = 50;

    private final ScoreRunRepository runs;
    private final ProfileRepository profiles;
    private final FriendRepository friends;
    private final DailySeeds seeds;

    public Boards(ScoreRunRepository runs, ProfileRepository profiles, FriendRepository friends,
            DailySeeds seeds) {
        this.runs = runs;
        this.profiles = profiles;
        this.friends = friends;
        this.seeds = seeds;
    }

    @Transactional(readOnly = true)
    public List<LeaderDto> leaderboard(String asker, String mode, String period) {
        List<LeaderDto> all = board(asker, Modes.mode(mode), Modes.period(period));
        return all.size() > TOP ? new ArrayList<LeaderDto>(all.subList(0, TOP)) : all;
    }

    /// Where `username` stands on the all-time board of `mode`; 0 when not on it.
    @Transactional(readOnly = true)
    public int rank(String username, String mode) {
        for (LeaderDto row : board(username, mode, Modes.ALL)) {
            if (row.me()) {
                return row.rank();
            }
        }
        return 0;
    }

    @Transactional(readOnly = true)
    public DailyDto daily(String asker) throws IOException {
        long now = Clock.now();
        List<LeaderDto> today = board(asker, Modes.DAILY, Modes.TODAY);
        long mine = 0;
        for (LeaderDto row : today) {
            if (row.me()) {
                mine = row.score();
            }
        }
        LeaderDto top = today.isEmpty() ? null : today.get(0);
        return new DailyDto(Clock.date(now), seeds.seedOf(now), mine, top == null ? 0 : top.score(),
                top == null ? null : top.displayName(), today.size());
    }

    /// The whole board, ranked, without the cut to [#TOP].
    List<LeaderDto> board(String asker, String mode, String period) {
        long now = Clock.now();
        long since = Modes.WEEK.equals(period) ? Clock.weekOf(now)
                : Modes.TODAY.equals(period) ? Clock.dayOf(now) : 0;
        Set<String> circle = null;
        if (Modes.FRIENDS.equals(period)) {
            circle = new HashSet<String>();
            circle.add(asker);
            for (Friend f : friends.findByUsername(asker)) {
                circle.add(f.getFriend());
            }
        }
        Map<String, Profile> seen = new HashMap<String, Profile>();
        List<LeaderDto> out = new ArrayList<LeaderDto>();
        for (ScoreRun r : runs.board(mode, Instant.ofEpochMilli(since))) {
            String who = r.getUsername();
            if (seen.containsKey(who) || (circle != null && !circle.contains(who))) {
                continue;
            }
            Profile p = profiles.findById(who);
            seen.put(who, p);
            if (p == null || Accounts.isBenched(p)) {
                continue;
            }
            out.add(new LeaderDto(out.size() + 1, who, p.getDisplayName(), r.getScore(), p.getQuill(),
                    who.equals(asker)));
        }
        return out;
    }
}
