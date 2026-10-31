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
package com.codenameone.examples.dash.game;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.dash.domain.Profile;
import com.codenameone.examples.dash.domain.RaceEntry;
import com.codenameone.examples.dash.domain.RaceRecord;
import com.codenameone.examples.dash.progress.Progression;
import com.codenameone.examples.dash.repository.ProfileRepository;
import com.codenameone.examples.dash.repository.RaceEntryRepository;
import com.codenameone.examples.dash.repository.RaceRecordRepository;
import com.codenameone.examples.dash.rules.Runner;
import com.codenameone.examples.dash.rules.RulesVersion;
import com.codenameone.examples.dash.social.Referrals;
import java.io.IOException;
import java.time.Instant;

/// Records a finished race: the replay, and what it did to each player.
// tag::dash-results[]
@Component
public class Results {
    /// Trophies per place gained or lost: in a race of `n`, place `p` moves
    /// `(n - 1 - 2p) * TROPHY_STEP`, so the winner of four gets +30 and the
    /// last -30, and the sum is zero.
    public static final int TROPHY_STEP = 10;
    /// Coins are a twentieth of the score, capped.
    public static final int MAX_RACE_COINS = 50;

    /// What one runner got out of a race.
    public static final class Outcome {
        public final int trophies;
        public final int coins;

        public Outcome(int trophies, int coins) {
            this.trophies = trophies;
            this.coins = coins;
        }
    }

    private final RaceRecordRepository races;
    private final RaceEntryRepository entries;
    private final ProfileRepository profiles;
    private final Progression progression;
    private final Referrals referrals;

    public Results(RaceRecordRepository races, RaceEntryRepository entries, ProfileRepository profiles,
            Progression progression, Referrals referrals) {
        this.races = races;
        this.entries = entries;
        this.profiles = profiles;
        this.progression = progression;
        this.referrals = referrals;
    }

    /// The trophy change for `place` of `runners` with `humans` players in
    /// the race. Against bots alone it is halved and never negative: losing
    /// to the server should not cost a ranking.
    public static int trophies(int place, int runners, int humans) {
        int change = (runners - 1 - 2 * place) * TROPHY_STEP;
        if (humans <= 1) {
            change = Math.max(0, change / 2);
        }
        return change;
    }

    @Transactional(rollbackFor = IOException.class)
    Outcome[] record(RaceRoom room) throws IOException {
        Outcome[] out = save(room);
        // After every profile is saved: paying an inviter writes a profile,
        // and that inviter may be another runner of this very race.
        for (Seat s : room.seats) {
            if (!s.isBot()) {
                referrals.played(s.username);
            }
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    Outcome[] save(RaceRoom room) throws IOException {
        int n = room.seats.length;
        int humans = room.humans();
        int[] places = room.places();
        Outcome[] out = new Outcome[n];
        Instant now = Instant.now();
        StringBuilder players = new StringBuilder();
        StringBuilder names = new StringBuilder();
        StringBuilder placeList = new StringBuilder();
        StringBuilder scoreList = new StringBuilder();
        StringBuilder trophyList = new StringBuilder();
        for (int i = 0; i < n; i++) {
            Seat s = room.seats[i];
            Runner r = room.run.runner(i);
            out[i] = new Outcome(0, 0);
            if (!s.isBot()) {
                Profile p = profiles.findById(s.username);
                if (p != null) {
                    int t = trophies(places[i], n, humans);
                    t = Math.max(t, -p.getTrophies());
                    int coins = s.left ? 0 : Math.min(MAX_RACE_COINS, r.score() / 20);
                    p.setTrophies(p.getTrophies() + t);
                    p.setCoins(p.getCoins() + coins);
                    if (places[i] == 0) {
                        p.setWins(p.getWins() + 1);
                    } else {
                        p.setLosses(p.getLosses() + 1);
                    }
                    p.setLeftStreak(s.left ? p.getLeftStreak() + 1 : 0);
                    p.setLastSeen(now);
                    progression.afterRace(p, places[i], n, r.z / 1000, r.coins);
                    profiles.save(p);
                    out[i] = new Outcome(t, coins);
                    RaceEntry e = new RaceEntry();
                    e.setId(room.id + "|" + s.username);
                    e.setRaceId(room.id);
                    e.setUsername(s.username);
                    e.setLevel(room.level);
                    e.setPlace(places[i]);
                    e.setRunners(n);
                    e.setScore(r.score());
                    e.setTrophyChange(t);
                    e.setTicks(room.run.tick());
                    e.setEndedAt(now);
                    entries.insert(e);
                }
            }
            String sep = i == 0 ? "" : ",";
            players.append(sep).append(s.username);
            names.append(sep).append(s.name.replace(',', ' '));
            placeList.append(sep).append(places[i]);
            scoreList.append(sep).append(r.score());
            trophyList.append(sep).append(out[i].trophies);
        }
        RaceRecord m = new RaceRecord();
        m.setId(room.id);
        m.setLevel(room.level);
        m.setPlayers(players.toString());
        m.setNames(names.toString());
        m.setPlaces(placeList.toString());
        m.setScores(scoreList.toString());
        m.setTrophies(trophyList.toString());
        m.setRunners(n);
        m.setHumans(humans);
        m.setTicks(room.run.tick());
        m.setEndedAt(now);
        m.setRulesVersion(RulesVersion.ID);
        m.setReplay(room.replay.encode());
        races.insert(m);
        return out;
    }
}
// end::dash-results[]
