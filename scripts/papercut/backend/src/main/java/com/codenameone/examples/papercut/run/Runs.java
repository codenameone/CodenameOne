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

import com.codename1.backend.Crypto;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.papercut.Clock;
import com.codenameone.examples.papercut.Ids;
import com.codenameone.examples.papercut.admin.Flags;
import com.codenameone.examples.papercut.api.RunDto;
import com.codenameone.examples.papercut.api.RunResultDto;
import com.codenameone.examples.papercut.api.RunTicketDto;
import com.codenameone.examples.papercut.api.ScoreBounds;
import com.codenameone.examples.papercut.domain.Profile;
import com.codenameone.examples.papercut.domain.RunTicket;
import com.codenameone.examples.papercut.domain.ScoreRun;
import com.codenameone.examples.papercut.repository.ProfileRepository;
import com.codenameone.examples.papercut.repository.RunTicketRepository;
import com.codenameone.examples.papercut.repository.ScoreRunRepository;
import java.io.IOException;
import java.time.Instant;

/// Tickets and results.
///
/// The server never plays a run again (see `ScoreBounds`). What it can hold a
/// result to is that it answers a ticket this server issued, once, for the
/// mode it was issued for, and that its numbers are arithmetic the game could
/// have produced. A ticket also pins the daily's seed, so a daily result is
/// always for the sheet everybody else was dealt.
@Component
public class Runs {
    /// How long a ticket stays good: longer than any run (an hour at most)
    /// plus a pause and a slow network.
    public static final long TICKET_MILLIS = 2L * 60 * 60 * 1000;
    /// Ink coins per run: a coin per thousand points, at most this many.
    public static final int MAX_COINS = 100;
    /// Extra coins for playing the daily.
    public static final int DAILY_BONUS = 5;

    private final RunTicketRepository tickets;
    private final ScoreRunRepository runs;
    private final ProfileRepository profiles;
    private final DailySeeds seeds;
    private final Boards boards;
    private final Flags flags;

    public Runs(RunTicketRepository tickets, ScoreRunRepository runs, ProfileRepository profiles,
            DailySeeds seeds, Boards boards, Flags flags) {
        this.tickets = tickets;
        this.runs = runs;
        this.profiles = profiles;
        this.seeds = seeds;
        this.boards = boards;
        this.flags = flags;
    }

    @Transactional(rollbackFor = IOException.class)
    public RunTicketDto start(String username, String mode) throws IOException {
        String m = Modes.mode(mode);
        long now = Clock.now();
        RunTicket t = new RunTicket();
        t.setToken(Ids.next());
        t.setUsername(username);
        t.setMode(m);
        t.setSeed(Modes.DAILY.equals(m) ? seeds.seedOf(now) : randomSeed());
        t.setExpiresAt(Instant.ofEpochMilli(now + TICKET_MILLIS));
        tickets.insert(t);
        return new RunTicketDto(t.getToken(), m, t.getSeed(), t.getExpiresAt().toEpochMilli());
    }

    private static long randomSeed() throws IOException {
        byte[] b = Crypto.randomBytes(8);
        long seed = 0;
        for (int i = 0; i < 8; i++) {
            seed = (seed << 8) | (b[i] & 0xff);
        }
        return seed;
    }

    /// Takes a finished run. A refusal for a reason the player can do nothing
    /// about (a stale or reused ticket) is a 4xx; a run past `ScoreBounds` is
    /// answered `accepted=false` with the reason, because the app shows it.
    @Transactional(rollbackFor = IOException.class)
    public RunResultDto submit(String username, RunDto run) throws IOException {
        if (run == null || run.token() == null) {
            throw new ResponseStatusException(400, "No run");
        }
        RunTicket t = tickets.findById(run.token());
        if (t == null || !t.getUsername().equals(username)) {
            throw new ResponseStatusException(404, "Unknown ticket");
        }
        if (t.isUsed()) {
            throw new ResponseStatusException(409, "That ticket was already used");
        }
        if (t.getExpiresAt().toEpochMilli() < Clock.now()) {
            throw new ResponseStatusException(410, "That ticket has expired");
        }
        if (!t.getMode().equalsIgnoreCase(run.mode())) {
            throw new ResponseStatusException(400, "That ticket is for " + t.getMode());
        }
        // Spent before anything else, and by a conditional update, so a second
        // submit racing this one finds it used even before this commits.
        if (tickets.use(t.getToken()) == 0) {
            throw new ResponseStatusException(409, "That ticket was already used");
        }
        // The run cannot have lasted longer than the ticket has existed: the
        // seconds the client claims are what every bound below scales with.
        long issued = t.getExpiresAt().toEpochMilli() - TICKET_MILLIS;
        long elapsed = (Clock.now() - issued) / 1000 + 5;
        if (run.seconds() > elapsed) {
            return new RunResultDto(false, false, false, best(profile(username), t.getMode()), 0, 0,
                    "That run claims " + run.seconds() + " seconds, but it started " + elapsed + " seconds ago");
        }
        int cuts = Math.max(0, run.kills()) + Math.max(0, run.gates());
        if (!ScoreBounds.possible(run.score(), cuts, run.seconds())
                || run.peakMultiplier() > ScoreBounds.MAX_MULTIPLIER) {
            return new RunResultDto(false, false, false, best(profile(username), t.getMode()), 0, 0,
                    "That score is not possible in " + run.seconds() + " seconds with " + cuts + " cuts");
        }
        return accept(username, t.getMode(), run, Clock.now());
    }

    /// Stores an accepted run and pays for it. Also how the demo data is
    /// written, with a `when` in the past.
    @Transactional(rollbackFor = IOException.class)
    public RunResultDto accept(String username, String mode, RunDto run, long when) throws IOException {
        if (profiles.hold(username) == 0) {
            throw new ResponseStatusException(404, "No such account");
        }
        Profile p = profile(username);
        ScoreRun r = new ScoreRun();
        r.setId(Ids.next());
        r.setUsername(username);
        r.setMode(mode);
        r.setScore(run.score());
        r.setKills(run.kills());
        r.setGates(run.gates());
        r.setSeconds(run.seconds());
        r.setPeakMultiplier(run.peakMultiplier());
        r.setDay(Clock.dayOf(when));
        r.setCreatedAt(Instant.ofEpochMilli(when));
        runs.insert(r);
        long before = best(p, mode);
        boolean newBest = run.score() > before;
        if (newBest) {
            setBest(p, mode, run.score());
        }
        int coins = (int) Math.min(MAX_COINS, run.score() / 1000) + (Modes.DAILY.equals(mode) ? DAILY_BONUS : 0);
        p.setInkCoins(p.getInkCoins() + coins);
        p.setRuns(p.getRuns() + 1);
        p.setLastSeen(Instant.ofEpochMilli(Math.max(when, p.getLastSeen().toEpochMilli())));
        boolean flagged = ScoreBounds.suspicious(run.score(), run.seconds());
        if (flagged) {
            p.setFlagged(true);
        }
        profiles.save(p);
        if (flagged) {
            flags.file(p, r, "More than " + ScoreBounds.SUSPICIOUS_PER_SECOND + " points a second");
        }
        return new RunResultDto(true, flagged, newBest, Math.max(before, run.score()),
                boards.rank(username, mode), coins, newBest ? "New best!" : "Run saved");
    }

    /// Works a player's three bests out again from their standing runs, after
    /// an admin struck some.
    @Transactional(rollbackFor = IOException.class)
    public void recomputeBests(String username) {
        Profile p = profile(username);
        p.setBestClassic(0);
        p.setBestFold(0);
        p.setBestDaily(0);
        for (ScoreRun r : runs.of(username)) {
            if (r.getScore() > best(p, r.getMode())) {
                setBest(p, r.getMode(), r.getScore());
            }
        }
        profiles.save(p);
    }

    /// Clears the tickets nobody came back for.
    @Scheduled(fixedDelay = 600000, initialDelay = 600000)
    @Transactional
    public void sweep() throws IOException {
        tickets.deleteByExpiresAtLessThan(Instant.ofEpochMilli(Clock.now() - Clock.DAY));
    }

    static long best(Profile p, String mode) {
        return Modes.CLASSIC.equals(mode) ? p.getBestClassic()
                : Modes.FOLD.equals(mode) ? p.getBestFold() : p.getBestDaily();
    }

    private static void setBest(Profile p, String mode, long score) {
        if (Modes.CLASSIC.equals(mode)) {
            p.setBestClassic(score);
        } else if (Modes.FOLD.equals(mode)) {
            p.setBestFold(score);
        } else {
            p.setBestDaily(score);
        }
    }

    private Profile profile(String username) {
        Profile p = profiles.findById(username);
        if (p == null) {
            throw new ResponseStatusException(404, "No such account");
        }
        return p;
    }
}
