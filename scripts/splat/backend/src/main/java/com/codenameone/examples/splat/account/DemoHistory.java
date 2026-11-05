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
package com.codenameone.examples.splat.account;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.splat.Clock;
import com.codenameone.examples.splat.Ids;
import com.codenameone.examples.splat.domain.Profile;
import com.codenameone.examples.splat.domain.RaceRecord;
import com.codenameone.examples.splat.domain.StorePurchase;
import com.codenameone.examples.splat.repository.ProfileRepository;
import com.codenameone.examples.splat.repository.RaceRecordRepository;
import com.codenameone.examples.splat.repository.StorePurchaseRepository;
import com.codenameone.examples.splat.rules.Input;
import com.codenameone.examples.splat.rules.Match;
import com.codenameone.examples.splat.rules.Mode;
import com.codenameone.examples.splat.rules.Replay;
import com.codenameone.examples.splat.rules.RulesVersion;
import com.codenameone.examples.splat.rules.Slot;
import com.codenameone.examples.splat.game.Arenas;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.time.Instant;

/// A development server's past: what [DemoAccounts] writes behind its field
/// of runners so the console has a month to chart.
@Component
public class DemoHistory {
    private final ProfileRepository profiles;
    private final RaceRecordRepository races;
    private final StorePurchaseRepository purchases;

    public DemoHistory(ProfileRepository profiles, RaceRecordRepository races, StorePurchaseRepository purchases) {
        this.profiles = profiles;
        this.races = races;
        this.purchases = purchases;
    }

    /// Four weeks behind the field, so the console's charts have a shape on
    /// a server started this morning: the runners joined on days spread over
    /// the month, raced each other a few times a day -- real races of the
    /// rules' bots, replays included -- and now and then bought gems.
    @Transactional
    public void seed(String[][] field) throws IOException {
        // Once per database: a first start that died half way through seeds
        // the field again on the next one, but never a second month.
        if (races.count() > 0) {
            return;
        }
        long today = Clock.dayOf(Clock.now());
        int days = 28;
        for (int i = 0; i < field.length; i++) {
            Profile p = profiles.findById(field[i][0]);
            if (p != null) {
                p.setCreatedAt(Instant.ofEpochMilli(today - (long) (days - 2 - i * 3) * Clock.DAY));
                profiles.save(p);
            }
        }
        for (int d = days - 1; d >= 1; d--) {
            long day = today - (long) d * Clock.DAY;
            // Busier as the month goes on and more of the field has joined.
            int joined = 0;
            for (int i = 0; i < field.length; i++) {
                if (days - 2 - i * 3 >= d) {
                    joined++;
                }
            }
            int count = joined < 2 ? 0 : 1 + (days - d) / 6 + (d % 3);
            for (int k = 0; k < count; k++) {
                // Two a side from whoever has joined, on a field and mode that rotate.
                int[] who = new int[Math.min(4, joined - joined % 2)];
                for (int w = 0; w < who.length; w++) {
                    who[w] = (d * 7 + k * 3 + w) % joined;
                }
                String ref = MATCHES[(d + k) % MATCHES.length];
                match(field, ref, who, day + (9 + k * 2L) * 3600000L, d * 131L + k);
            }
            if (d % 4 == 1 && joined > 0) {
                StorePurchase s = new StorePurchase();
                s.setId(Ids.next());
                s.setUsername(field[d % joined][0]);
                s.setStore("demo");
                boolean big = d % 8 == 1;
                s.setSku(big ? "splat.gems_500" : "splat.gems_80");
                s.setGems(big ? 500 : 80);
                s.setCreatedAt(Instant.ofEpochMilli(day + 15 * 3600000L));
                purchases.insert(s);
            }
        }
    }

    private static final String[] MATCHES = {
        "weekend:TEAM_PAINT", "cul-de-sac:SNACK_BAR", "weekend:CAPTURE_THE_FLAG", "cul-de-sac:TEAM_PAINT",
    };

    private void match(String[][] field, String ref, int[] who, long endedAt, long seed) throws IOException {
        Mode mode = Arenas.mode(ref);
        List<Slot> slots = new ArrayList<Slot>();
        for (int i = 0; i < who.length; i++) {
            slots.add(new Slot(field[who[i]][1], i % 2, "semi", Integer.parseInt(field[who[i]][2])));
        }
        Replay replay = new Replay(seed, Arenas.arena(ref), mode, slots);
        Match match = replay.newMatch();
        Input[] idle = new Input[who.length];
        while (!match.over()) {
            match.step(idle);
        }
        int winner = match.winner();
        StringBuilder players = new StringBuilder();
        StringBuilder names = new StringBuilder();
        StringBuilder places = new StringBuilder();
        StringBuilder points = new StringBuilder();
        StringBuilder trophies = new StringBuilder();
        for (int i = 0; i < who.length; i++) {
            boolean won = winner >= 0 && i % 2 == winner;
            int place = winner < 0 || won ? 0 : 1;
            String sep = i == 0 ? "" : ",";
            players.append(sep).append(field[who[i]][0]);
            names.append(sep).append(field[who[i]][1]);
            places.append(sep).append(place);
            points.append(sep).append(match.player(i).paints());
            int t = winner < 0 ? 0 : won ? 20 : -15;
            trophies.append(sep).append(t);
            Profile p = profiles.findById(field[who[i]][0]);
            if (p != null) {
                p.setTrophies(Math.max(0, p.getTrophies() + t));
                if (won) {
                    p.setWins(p.getWins() + 1);
                } else if (winner >= 0) {
                    p.setLosses(p.getLosses() + 1);
                }
                p.setLastSeen(Instant.ofEpochMilli(endedAt));
                profiles.save(p);
            }
        }
        RaceRecord m = new RaceRecord();
        m.setId(Ids.next());
        m.setLevel(ref);
        m.setPlayers(players.toString());
        m.setNames(names.toString());
        m.setPlaces(places.toString());
        m.setScores(points.toString());
        m.setTrophies(trophies.toString());
        m.setRunners(who.length);
        m.setHumans(who.length);
        m.setTicks(match.tick());
        m.setEndedAt(Instant.ofEpochMilli(endedAt));
        m.setRulesVersion(String.valueOf(RulesVersion.ID));
        m.setReplay(replay.encode());
        races.insert(m);
    }
}
