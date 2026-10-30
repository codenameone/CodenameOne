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
package com.codenameone.examples.skirmish.game;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.skirmish.Clock;
import com.codenameone.examples.skirmish.admin.Flags;
import com.codenameone.examples.skirmish.domain.MatchRecord;
import com.codenameone.examples.skirmish.domain.Profile;
import com.codenameone.examples.skirmish.progress.Progression;
import com.codenameone.examples.skirmish.repository.MatchRecordRepository;
import com.codenameone.examples.skirmish.repository.ProfileRepository;
import com.codenameone.examples.skirmish.rules.CardDef;
import com.codenameone.examples.skirmish.rules.Cards;
import com.codenameone.examples.skirmish.rules.Command;
import com.codenameone.examples.skirmish.rules.Replay;
import com.codenameone.examples.skirmish.rules.Match;
import com.codenameone.examples.skirmish.rules.RulesVersion;
import com.codenameone.examples.skirmish.social.Referrals;
import java.io.IOException;
import java.time.Instant;

/// Records a finished match: the replay, and what it did to each player.
// tag::skirmish-results[]
@Component
public class Results {
    public static final int WIN_TROPHIES = 30;
    public static final int LOSS_TROPHIES = 20;
    public static final int WIN_COINS = 20;
    public static final int BOT_WIN_COINS = 5;
    /// Matches left early in a row before the server flags a player.
    public static final int QUITTER_STREAK = 3;

    /// What one side got out of a match.
    public static final class Outcome {
        public final int trophies;
        public final int coins;

        public Outcome(int trophies, int coins) {
            this.trophies = trophies;
            this.coins = coins;
        }
    }

    private final MatchRecordRepository matches;
    private final ProfileRepository profiles;
    private final Flags flags;
    private final Progression progression;
    private final Referrals referrals;

    public Results(MatchRecordRepository matches, ProfileRepository profiles, Flags flags,
            Progression progression, Referrals referrals) {
        this.referrals = referrals;
        this.matches = matches;
        this.profiles = profiles;
        this.flags = flags;
        this.progression = progression;
    }

    @Transactional(rollbackFor = IOException.class)
    Outcome[] record(MatchRoom room) throws IOException {
        Outcome[] out = save(room);
        Seat quitter = room.forfeit >= 0 ? room.seats[room.forfeit] : null;
        if (quitter != null && !quitter.isBot()) {
            Profile p = profiles.findById(quitter.username);
            if (p != null && p.getLeftStreak() == QUITTER_STREAK) {
                flags.auto(quitter.username, "Left " + QUITTER_STREAK + " matches in a row", room.id);
            }
        }
        // After every profile is saved: paying an inviter writes a profile,
        // and that inviter may be the other seat of this very match.
        for (Seat s : room.seats) {
            if (!s.isBot()) {
                referrals.played(s.username);
            }
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    Outcome[] save(MatchRoom room) throws IOException {
        int result = room.result();
        boolean bot = room.seats[1].isBot();
        Replay replay = room.replay();
        int[] cards = new int[2];
        int[] spells = new int[2];
        for (Command c : replay.commands()) {
            cards[c.player]++;
            CardDef def = Cards.get(c.cardId);
            if (def != null && def.kind == CardDef.SPELL) {
                spells[c.player]++;
            }
        }
        Outcome[] out = new Outcome[2];
        for (int i = 0; i < 2; i++) {
            Seat s = room.seats[i];
            if (s.isBot()) {
                out[i] = new Outcome(0, 0);
                continue;
            }
            Profile p = profiles.findById(s.username);
            if (p == null) {
                out[i] = new Outcome(0, 0);
                continue;
            }
            int trophies = 0;
            int coins = 0;
            if (result == i) {
                trophies = bot ? 0 : WIN_TROPHIES;
                coins = bot ? BOT_WIN_COINS : WIN_COINS;
                p.setWins(p.getWins() + 1);
            } else if (result == Match.DRAW) {
                p.setDraws(p.getDraws() + 1);
            } else {
                trophies = bot ? 0 : -Math.min(LOSS_TROPHIES, p.getTrophies());
                p.setLosses(p.getLosses() + 1);
            }
            p.setTrophies(p.getTrophies() + trophies);
            p.setCoins(p.getCoins() + coins);
            p.setLeftStreak(room.forfeit == i ? p.getLeftStreak() + 1 : 0);
            p.setLastSeen(Instant.now());
            progression.afterMatch(p, result == i, result == Match.DRAW, room.match.player(i).crowns(),
                    cards[i], spells[i]);
            profiles.save(p);
            out[i] = new Outcome(trophies, coins);
        }
        MatchRecord m = new MatchRecord();
        m.setId(room.id);
        m.setPlayer0(room.seats[0].username);
        m.setPlayer1(room.seats[1].username);
        m.setName0(room.seats[0].name);
        m.setName1(room.seats[1].name);
        m.setResult(result);
        m.setCrowns0(room.match.player(0).crowns());
        m.setCrowns1(room.match.player(1).crowns());
        m.setTrophies0(out[0].trophies);
        m.setTrophies1(out[1].trophies);
        m.setTicks(room.match.tick());
        m.setEndedAt(Instant.now());
        m.setRulesVersion(RulesVersion.ID);
        m.setReplay(replay.encode());
        matches.insert(m);
        return out;
    }
}
// end::skirmish-results[]
