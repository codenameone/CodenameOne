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
package com.codenameone.examples.dash.account;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.dash.Clock;
import com.codenameone.examples.dash.Ids;
import com.codenameone.examples.dash.domain.Profile;
import com.codenameone.examples.dash.domain.RaceRecord;
import com.codenameone.examples.dash.domain.StorePurchase;
import com.codenameone.examples.dash.repository.ProfileRepository;
import com.codenameone.examples.dash.repository.RaceRecordRepository;
import com.codenameone.examples.dash.repository.StorePurchaseRepository;
import com.codenameone.examples.dash.rules.Bot;
import com.codenameone.examples.dash.rules.Content;
import com.codenameone.examples.dash.rules.LevelDef;
import com.codenameone.examples.dash.rules.Recorder;
import com.codenameone.examples.dash.rules.RulesVersion;
import com.codenameone.examples.dash.rules.Run;
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
        long today = Clock.dayOf(Clock.now());
        int days = 28;
        for (int i = 0; i < field.length; i++) {
            Profile p = profiles.findById(field[i][0]);
            if (p != null) {
                p.setCreatedAt(Instant.ofEpochMilli(today - (long) (days - 2 - i * 3) * Clock.DAY));
                profiles.save(p);
            }
        }
        LevelDef level = Content.level(Content.CAUSEWAY);
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
                int a = (d * 7 + k * 3) % joined;
                int b = (a + 1 + k) % joined;
                if (a == b) {
                    b = (b + 1) % joined;
                }
                race(field, level, new int[] {a, b}, day + (9 + k * 2L) * 3600000L, d * 131 + k);
            }
            if (d % 4 == 1 && joined > 0) {
                StorePurchase s = new StorePurchase();
                s.setId(Ids.next());
                s.setUsername(field[d % joined][0]);
                s.setStore("demo");
                boolean big = d % 8 == 1;
                s.setSku(big ? "dash.gems_500" : "dash.gems_80");
                s.setGems(big ? 500 : 80);
                s.setCreatedAt(Instant.ofEpochMilli(day + 15 * 3600000L));
                purchases.insert(s);
            }
        }
    }

    private void race(String[][] field, LevelDef level, int[] who, long endedAt, long salt) throws IOException {
        long seed = Content.weeklySeed((int) (endedAt / Clock.DAY / 7), level.id) + salt;
        Run run = new Run(level, Content.catalog(), seed, who.length);
        Recorder rec = new Recorder(run, level.id);
        Bot[] bots = new Bot[who.length];
        for (int i = 0; i < who.length; i++) {
            bots[i] = new Bot(seed * 31 + i, Integer.parseInt(field[who[i]][2]));
        }
        int[] in = new int[who.length];
        while (!run.over()) {
            for (int i = 0; i < who.length; i++) {
                in[i] = bots[i].decide(run, i);
            }
            rec.step(in);
        }
        StringBuilder players = new StringBuilder();
        StringBuilder names = new StringBuilder();
        StringBuilder places = new StringBuilder();
        StringBuilder points = new StringBuilder();
        StringBuilder trophies = new StringBuilder();
        for (int i = 0; i < who.length; i++) {
            int place = 0;
            for (int j = 0; j < who.length; j++) {
                if (j != i && ahead(run, j, i)) {
                    place++;
                }
            }
            String sep = i == 0 ? "" : ",";
            players.append(sep).append(field[who[i]][0]);
            names.append(sep).append(field[who[i]][1]);
            places.append(sep).append(place);
            points.append(sep).append(run.runner(i).score());
            int t = place == 0 ? 30 : -10;
            trophies.append(sep).append(t);
            Profile p = profiles.findById(field[who[i]][0]);
            if (p != null) {
                p.setTrophies(Math.max(0, p.getTrophies() + t));
                if (place == 0) {
                    p.setWins(p.getWins() + 1);
                } else {
                    p.setLosses(p.getLosses() + 1);
                }
                p.setLastSeen(Instant.ofEpochMilli(endedAt));
                profiles.save(p);
            }
        }
        RaceRecord m = new RaceRecord();
        m.setId(Ids.next());
        m.setLevel(level.id);
        m.setPlayers(players.toString());
        m.setNames(names.toString());
        m.setPlaces(places.toString());
        m.setScores(points.toString());
        m.setTrophies(trophies.toString());
        m.setRunners(who.length);
        m.setHumans(who.length);
        m.setTicks(run.tick());
        m.setEndedAt(Instant.ofEpochMilli(endedAt));
        m.setRulesVersion(RulesVersion.ID);
        m.setReplay(rec.replay().encode());
        races.insert(m);
    }

    /// Whether runner `a` finished ahead of runner `b`: further down the
    /// track, or level and with more points (lower index on a full tie).
    private static boolean ahead(Run run, int a, int b) {
        int za = run.runner(a).z;
        int zb = run.runner(b).z;
        if (za != zb) {
            return za > zb;
        }
        int sa = run.runner(a).score();
        int sb = run.runner(b).score();
        return sa != sb ? sa > sb : a < b;
    }
}
