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

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.splat.domain.Profile;
import com.codenameone.examples.splat.domain.RaceEntry;
import com.codenameone.examples.splat.domain.RaceRecord;
import com.codenameone.examples.splat.progress.Progression;
import com.codenameone.examples.splat.repository.ProfileRepository;
import com.codenameone.examples.splat.repository.RaceEntryRepository;
import com.codenameone.examples.splat.repository.RaceRecordRepository;
import com.codenameone.examples.splat.rules.Player;
import com.codenameone.examples.splat.rules.RulesVersion;
import com.codenameone.examples.splat.social.Referrals;
import java.io.IOException;
import java.time.Instant;

/// Records a finished match: the replay, and what it did to each player.
// tag::splat-results[]
@Component
public class Results {
    /// Trophies for a win, and lost for a loss; a draw moves nothing.
    public static final int WIN_TROPHIES = 20;
    public static final int LOSS_TROPHIES = 15;
    /// Coins per paint, and the most a match can pay.
    public static final int COINS_PER_PAINT = 3;
    public static final int MAX_MATCH_COINS = 60;

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

    /// The trophy change for a player whose team won (`place` 0) or lost, with
    /// `humans` players in the match. A draw is `place` 0 for everyone and
    /// `draw` true. Against bots alone it is halved and never negative: losing to
    /// the server should not cost a ranking.
    public static int trophies(int place, boolean draw, int humans) {
        int change = draw ? 0 : place == 0 ? WIN_TROPHIES : -LOSS_TROPHIES;
        if (humans <= 1) {
            change = Math.max(0, change / 2);
        }
        return change;
    }

    @Transactional(rollbackFor = IOException.class)
    Outcome[] record(MatchRoom room) throws IOException {
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
    Outcome[] save(MatchRoom room) throws IOException {
        int n = room.seats.length;
        int humans = room.humans();
        int[] places = room.places();
        boolean draw = room.match.winner() < 0;
        int[] wipes = room.wipes();
        Outcome[] out = new Outcome[n];
        Instant now = Instant.now();
        StringBuilder players = new StringBuilder();
        StringBuilder names = new StringBuilder();
        StringBuilder placeList = new StringBuilder();
        StringBuilder scoreList = new StringBuilder();
        StringBuilder trophyList = new StringBuilder();
        for (int i = 0; i < n; i++) {
            Seat s = room.seats[i];
            Player r = room.match.player(i);
            out[i] = new Outcome(0, 0);
            if (!s.isBot()) {
                Profile p = profiles.findById(s.username);
                if (p != null) {
                    int t = trophies(places[i], draw, humans);
                    t = Math.max(t, -p.getTrophies());
                    int coins = s.left ? 0 : Math.min(MAX_MATCH_COINS, r.paints() * COINS_PER_PAINT);
                    p.setTrophies(p.getTrophies() + t);
                    p.setCoins(p.getCoins() + coins);
                    if (draw) {
                        p.setDraws(p.getDraws() + 1);
                    } else if (places[i] == 0) {
                        p.setWins(p.getWins() + 1);
                    } else {
                        p.setLosses(p.getLosses() + 1);
                    }
                    p.setLeftStreak(s.left ? p.getLeftStreak() + 1 : 0);
                    p.setLastSeen(now);
                    progression.afterMatch(p, !draw && places[i] == 0, i == room.leader(), r.paints(), wipes[i]);
                    profiles.save(p);
                    out[i] = new Outcome(t, coins);
                    RaceEntry e = new RaceEntry();
                    e.setId(room.id + "|" + s.username);
                    e.setRaceId(room.id);
                    e.setUsername(s.username);
                    e.setLevel(room.level);
                    e.setPlace(places[i]);
                    e.setRunners(n);
                    e.setScore(r.paints());
                    e.setTrophyChange(t);
                    e.setTicks(room.match.tick());
                    e.setEndedAt(now);
                    entries.insert(e);
                }
            }
            String sep = i == 0 ? "" : ",";
            players.append(sep).append(s.username);
            names.append(sep).append(s.name.replace(',', ' '));
            placeList.append(sep).append(places[i]);
            scoreList.append(sep).append(r.paints());
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
        m.setTicks(room.match.tick());
        m.setEndedAt(now);
        m.setRulesVersion(String.valueOf(RulesVersion.ID));
        m.setReplay(room.replay.encode());
        races.insert(m);
        return out;
    }
}
// end::splat-results[]
