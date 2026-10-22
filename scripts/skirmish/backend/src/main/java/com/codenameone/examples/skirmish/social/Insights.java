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
package com.codenameone.examples.skirmish.social;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.skirmish.Clock;
import com.codenameone.examples.skirmish.account.Accounts;
import com.codenameone.examples.skirmish.api.ConditionDto;
import com.codenameone.examples.skirmish.api.NameCountDto;
import com.codenameone.examples.skirmish.api.PlayerDto;
import com.codenameone.examples.skirmish.api.QueryDto;
import com.codenameone.examples.skirmish.api.QueryResultDto;
import com.codenameone.examples.skirmish.api.InsightsDto;
import com.codenameone.examples.skirmish.domain.MatchRecord;
import com.codenameone.examples.skirmish.domain.Profile;
import com.codenameone.examples.skirmish.domain.Referral;
import com.codenameone.examples.skirmish.domain.StorePurchase;
import com.codenameone.examples.skirmish.repository.MatchRecordRepository;
import com.codenameone.examples.skirmish.repository.ProfileRepository;
import com.codenameone.examples.skirmish.repository.ReferralRepository;
import com.codenameone.examples.skirmish.repository.StorePurchaseRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// The console's reports and its query tool, read from the game's own
/// records: accounts, finished battles, store purchases and invites.
///
/// These are the numbers the game can answer about itself. What players do
/// inside the app -- screens, taps, sessions, invite shares, push opens -- is
/// reported by the app to Codename One analytics and read there.
// tag::skirmish-insights[]
@Component
public class Insights {
    public static final int MAX_DAYS = 90;
    /// How many matching players a query lists; the count covers them all.
    public static final int LISTED = 50;
    private static final int[] RETURNED_AFTER = {1, 7, 30};
    private static final int[] BANDS = {0, 100, 300, 600, 1000};

    private final ProfileRepository profiles;
    private final MatchRecordRepository matches;
    private final StorePurchaseRepository purchases;
    private final ReferralRepository referrals;
    private final Accounts accounts;

    public Insights(ProfileRepository profiles, MatchRecordRepository matches, StorePurchaseRepository purchases,
            ReferralRepository referrals, Accounts accounts) {
        this.profiles = profiles;
        this.matches = matches;
        this.purchases = purchases;
        this.referrals = referrals;
        this.accounts = accounts;
    }

    @Transactional(readOnly = true)
    public InsightsDto report(int days) {
        int n = days < 1 ? 1 : days > MAX_DAYS ? MAX_DAYS : days;
        long now = Clock.now();
        long first = Clock.dayOf(now) - (n - 1) * Clock.DAY;
        List<Profile> all = profiles.findAll();
        int[] joined = new int[n];
        for (Profile p : all) {
            bump(joined, first, p.getCreatedAt());
        }
        int[] battles = new int[n];
        List<Set<String>> fought = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            fought.add(new HashSet<String>());
        }
        for (MatchRecord m : matches.since(first)) {
            int d = dayIndex(first, m.getEndedAt(), n);
            if (d >= 0) {
                battles[d]++;
                fought.get(d).add(m.getPlayer0());
                fought.get(d).add(m.getPlayer1());
            }
        }
        Set<String> known = new HashSet<>();
        for (Profile p : all) {
            known.add(p.getUsername());
        }
        int[] active = new int[n];
        for (int i = 0; i < n; i++) {
            // A bot is a match's second side, not a player.
            fought.get(i).retainAll(known);
            active[i] = fought.get(i).size();
        }
        int[] gems = new int[n];
        List<StorePurchase> sales = purchases.since(0);
        Set<String> paid = new HashSet<>();
        for (StorePurchase s : sales) {
            int d = dayIndex(first, s.getCreatedAt(), n);
            if (d >= 0) {
                gems[d] += s.getGems();
            }
            paid.add(s.getUsername());
        }
        Set<String> inviters = new HashSet<>();
        for (Referral r : referrals.findAll()) {
            inviters.add(r.getInviter());
        }
        int played = 0;
        int won = 0;
        for (Profile p : all) {
            if (p.getWins() + p.getLosses() + p.getDraws() > 0) {
                played++;
            }
            if (p.getWins() > 0) {
                won++;
            }
        }
        List<NameCountDto> funnel = new ArrayList<>();
        funnel.add(new NameCountDto("Joined", all.size()));
        funnel.add(new NameCountDto("Played", played));
        funnel.add(new NameCountDto("Won", won));
        funnel.add(new NameCountDto("Paid", paid.size()));
        funnel.add(new NameCountDto("Invited a friend", inviters.size()));
        return new InsightsDto(first, list(active), list(joined), list(battles), list(gems), funnel,
                retention(all, now), bands(all));
    }

    /// Of the players who joined at least N days ago, the share seen N days
    /// or more after joining. The server keeps when a player was last seen,
    /// not every visit, so this is "came back at least once that late".
    private static List<NameCountDto> retention(List<Profile> all, long now) {
        List<NameCountDto> out = new ArrayList<>();
        for (int after : RETURNED_AFTER) {
            long span = after * Clock.DAY;
            int cohort = 0;
            int back = 0;
            for (Profile p : all) {
                if (p.getCreatedAt() <= now - span) {
                    cohort++;
                    if (p.getLastSeen() - p.getCreatedAt() >= span) {
                        back++;
                    }
                }
            }
            out.add(new NameCountDto("Day " + after, cohort == 0 ? 0 : back * 100 / cohort));
        }
        return out;
    }

    private static List<NameCountDto> bands(List<Profile> all) {
        int[] counts = new int[BANDS.length];
        for (Profile p : all) {
            int b = 0;
            while (b + 1 < BANDS.length && p.getTrophies() >= BANDS[b + 1]) {
                b++;
            }
            counts[b]++;
        }
        List<NameCountDto> out = new ArrayList<>();
        for (int b = 0; b < BANDS.length; b++) {
            out.add(new NameCountDto(b + 1 < BANDS.length ? BANDS[b] + "-" + (BANDS[b + 1] - 1) : BANDS[b] + "+",
                    counts[b]));
        }
        return out;
    }

    /// The players meeting every condition: how many, and the first
    /// [#LISTED] of them, the longest away first.
    @Transactional(readOnly = true)
    public QueryResultDto query(QueryDto q) {
        Audience audience = audience(q == null ? null : q.conditions());
        List<Profile> found = audience.filter(profiles.findAll());
        Collections.sort(found, (a, b) -> a.getLastSeen() < b.getLastSeen() ? -1
                : a.getLastSeen() > b.getLastSeen() ? 1 : a.getUsername().compareTo(b.getUsername()));
        List<PlayerDto> listed = new ArrayList<>();
        for (int i = 0; i < found.size() && i < LISTED; i++) {
            listed.add(accounts.toDto(found.get(i)));
        }
        return new QueryResultDto(found.size(), listed);
    }

    /// The audience for a list of conditions, with the purchases and invites
    /// the conditions may ask about.
    public Audience audience(List<ConditionDto> conditions) {
        Audience.check(conditions);
        return Audience.of(conditions, purchases.since(0), referrals.findAll());
    }
    // end::skirmish-insights[]

    private static void bump(int[] series, long first, long at) {
        int d = dayIndex(first, at, series.length);
        if (d >= 0) {
            series[d]++;
        }
    }

    private static int dayIndex(long first, long at, int n) {
        if (at < first) {
            return -1;
        }
        long d = (at - first) / Clock.DAY;
        return d >= n ? -1 : (int) d;
    }

    private static List<Integer> list(int[] values) {
        List<Integer> out = new ArrayList<>();
        for (int v : values) {
            out.add(v);
        }
        return out;
    }
}
