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

import com.codename1.backend.Crypto;
import com.codename1.backend.WebSocketSession;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Value;
import com.codenameone.examples.skirmish.Clock;
import com.codenameone.examples.skirmish.Ids;
import com.codenameone.examples.skirmish.account.Lists;
import com.codenameone.examples.skirmish.admin.AdminChannel;
import com.codenameone.examples.skirmish.admin.Flags;
import com.codenameone.examples.skirmish.api.GameProtocol;
import com.codenameone.examples.skirmish.api.LiveMatchDto;
import com.codenameone.examples.skirmish.domain.Profile;
import com.codenameone.examples.skirmish.rules.Cards;
import com.codenameone.examples.skirmish.rules.Deck;
import com.codenameone.examples.skirmish.rules.Match;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/// Matchmaking and every match in progress.
///
/// All state is guarded by this object's monitor: socket callbacks and the
/// ticker run on different threads. Database work and the final messages of a
/// match happen outside the lock, in [#tick], so a slow database never stalls
/// the other matches.
// tag::skirmish-rooms[]
@Component
public class Rooms {
    /// A match ticks at the rules' rate: 50 ms.
    static final long TICK_MILLIS = 1000L / Match.TICKS_PER_SECOND;
    /// How many ticks one ticker run may catch up, so a long pause cannot stall
    /// the server re-simulating minutes of match.
    static final int MAX_CATCH_UP = 10;
    public static final String BOT_NAME = "Training Bot";

    private final Results results;
    private final Flags flags;
    private final Challenges challenges;
    private final AdminChannel admins;
    private final long botAfterMillis;

    private final Map<String, MatchRoom> byId = new HashMap<String, MatchRoom>();
    private final Map<String, MatchRoom> byPlayer = new HashMap<String, MatchRoom>();
    private final List<Waiting> queue = new ArrayList<Waiting>();
    private final Map<String, Waiting> challengeWaits = new HashMap<String, Waiting>();

    static final class Waiting {
        final Profile profile;
        final WebSocketSession session;
        final long since;

        Waiting(Profile profile, WebSocketSession session, long since) {
            this.profile = profile;
            this.session = session;
            this.since = since;
        }
    }

    public Rooms(Results results, Flags flags, Challenges challenges, AdminChannel admins,
            @Value("${skirmish.match.bot-after-ms:8000}") long botAfterMillis) {
        this.results = results;
        this.flags = flags;
        this.challenges = challenges;
        this.admins = admins;
        this.botAfterMillis = botAfterMillis;
    }

    // ------------------------------------------------------------ connections

    synchronized void connected(String username, boolean admin, WebSocketSession session) {
        if (admin) {
            admins.add(username, session);
        }
        MatchRoom room = byPlayer.get(username);
        if (room != null) {
            // Back from a drop: the new socket gets the whole log and catches up.
            int seat = room.seatOf(username);
            room.seats[seat].session = session;
            send(session, Messages.start(room, seat));
        }
    }

    synchronized void disconnected(String username, WebSocketSession session) {
        admins.remove(username, session);
        removeWaiting(username);
        MatchRoom room = byPlayer.get(username);
        if (room != null) {
            int seat = room.seatOf(username);
            if (room.seats[seat].session == session) {
                room.seats[seat].session = null;
            }
        }
        for (MatchRoom r : byId.values()) {
            r.spectators.remove(session);
        }
    }

    /// Closes everything `username` has open: used when an admin suspends or bans.
    public void kick(String username) {
        WebSocketSession session = null;
        synchronized (this) {
            removeWaiting(username);
            MatchRoom room = byPlayer.get(username);
            if (room != null) {
                int seat = room.seatOf(username);
                session = room.seats[seat].session;
                if (room.forfeit < 0 && !room.match.isOver()) {
                    room.forfeit = seat;
                }
            }
        }
        if (session != null) {
            send(session, Messages.simple(GameProtocol.KICKED));
            session.close(4403, "suspended");
        }
    }

    // ------------------------------------------------------------ matchmaking

    synchronized void queue(Profile player, WebSocketSession session) {
        if (byPlayer.containsKey(player.getUsername())) {
            return;
        }
        removeWaiting(player.getUsername());
        queue.add(new Waiting(player, session, Clock.now()));
        send(session, Messages.simple(GameProtocol.QUEUED));
        pair(Clock.now());
    }

    synchronized void challenge(Profile player, WebSocketSession session, String code) {
        String host = challenges.hostOf(code);
        if (host == null) {
            send(session, Messages.error("That challenge has expired"));
            return;
        }
        if (byPlayer.containsKey(player.getUsername())) {
            return;
        }
        Waiting other = challengeWaits.get(code);
        if (other == null || other.profile.getUsername().equals(player.getUsername())) {
            challengeWaits.put(code, new Waiting(player, session, Clock.now()));
            send(session, Messages.simple(GameProtocol.QUEUED));
            if (!host.equals(player.getUsername())) {
                challenges.joinedWhileAway(code, host, player.getDisplayName());
            }
            return;
        }
        challengeWaits.remove(code);
        challenges.used(code);
        start(other, new Waiting(player, session, Clock.now()));
    }

    synchronized void cancel(String username) {
        removeWaiting(username);
    }

    private void removeWaiting(String username) {
        for (Iterator<Waiting> it = queue.iterator(); it.hasNext();) {
            if (it.next().profile.getUsername().equals(username)) {
                it.remove();
            }
        }
        for (Iterator<Waiting> it = challengeWaits.values().iterator(); it.hasNext();) {
            if (it.next().profile.getUsername().equals(username)) {
                it.remove();
            }
        }
    }

    /// Pairs waiting players whose trophies are close. The window widens the
    /// longer they wait; after [#botAfterMillis] alone, a player gets the bot.
    private void pair(long now) {
        for (int i = 0; i < queue.size(); i++) {
            Waiting a = queue.get(i);
            for (int j = i + 1; j < queue.size(); j++) {
                Waiting b = queue.get(j);
                long waited = Math.min(now - a.since, now - b.since) / 1000;
                int window = 100 + 25 * (int) waited;
                if (Math.abs(a.profile.getTrophies() - b.profile.getTrophies()) <= window) {
                    queue.remove(j);
                    queue.remove(i);
                    start(a, b);
                    pair(now);
                    return;
                }
            }
            if (now - a.since >= botAfterMillis) {
                queue.remove(i);
                start(a, null);
                pair(now);
                return;
            }
        }
    }

    private void start(Waiting a, Waiting b) {
        String id;
        long seed;
        try {
            id = Ids.next().substring(0, 24);
            byte[] r = Crypto.randomBytes(8);
            seed = 0;
            for (int i = 0; i < 8; i++) {
                seed = (seed << 8) | (r[i] & 0xff);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        Seat s0 = seat(a);
        Seat s1 = b == null ? new Seat("", BOT_NAME, Deck.starter(), a.profile.getTrophies(), null) : seat(b);
        MatchRoom room = new MatchRoom(id, seed, s0, s1, Clock.now());
        byId.put(id, room);
        byPlayer.put(s0.username, room);
        if (!s1.isBot()) {
            byPlayer.put(s1.username, room);
        }
        for (int i = 0; i < 2; i++) {
            if (room.seats[i].session != null) {
                send(room.seats[i].session, Messages.start(room, i));
            }
        }
    }

    private static Seat seat(Waiting w) {
        List<String> deck = Lists.split(w.profile.getDeck());
        return new Seat(w.profile.getUsername(), w.profile.getDisplayName(),
                deck.toArray(new String[deck.size()]), w.profile.getTrophies(), w.session);
    }

    // ------------------------------------------------------------ in a match

    synchronized void play(String username, String card, int x, int y) {
        MatchRoom room = byPlayer.get(username);
        if (room == null) {
            return;
        }
        if (room.intent(room.seatOf(username), card, x, y)) {
            flush(room);
        }
    }

    synchronized void emote(String username, String emote) {
        MatchRoom room = byPlayer.get(username);
        if (room == null || emote == null || emote.length() > 32) {
            return;
        }
        String msg = Messages.emote(room.seatOf(username), emote);
        broadcast(room, msg);
    }

    /// A client's own hash at `tick`. A mismatch means it computed a different
    /// match: it is sent the log again, and repeated mismatches raise a flag.
    void sync(String username, int tick, String hash) {
        String flagReason = null;
        synchronized (this) {
            MatchRoom room = byPlayer.get(username);
            if (room == null || hash == null) {
                return;
            }
            Long ours = room.hashes.get(Integer.valueOf(tick));
            if (ours == null || Long.toHexString(ours.longValue()).equals(hash)) {
                return;
            }
            int seat = room.seatOf(username);
            Seat s = room.seats[seat];
            s.desyncs++;
            if (s.session != null) {
                send(s.session, Messages.start(room, seat));
            }
            if (s.desyncs == 3) {
                flagReason = "Client state diverged from the server's three times in one match";
            }
        }
        if (flagReason != null) {
            flags.auto(username, flagReason, matchOf(username));
        }
    }

    synchronized void leave(String username) {
        MatchRoom room = byPlayer.get(username);
        if (room != null && room.forfeit < 0 && !room.match.isOver()) {
            room.forfeit = room.seatOf(username);
        }
    }

    synchronized boolean spectate(String matchId, WebSocketSession session) {
        MatchRoom room = byId.get(matchId);
        if (room == null) {
            return false;
        }
        room.spectators.add(session);
        send(session, Messages.start(room, -1));
        return true;
    }

    public synchronized String matchOf(String username) {
        MatchRoom room = byPlayer.get(username);
        return room == null ? "" : room.id;
    }

    /// The cheapest card `username` holds right now, or null outside a match.
    synchronized String cheapestInHand(String username) {
        MatchRoom room = byPlayer.get(username);
        if (room == null) {
            return null;
        }
        com.codenameone.examples.skirmish.rules.PlayerState p = room.match.player(room.seatOf(username));
        String best = null;
        for (int i = 0; i < com.codenameone.examples.skirmish.rules.Deck.HAND; i++) {
            String c = p.hand(i);
            if (best == null || Cards.get(c).cost < Cards.get(best).cost) {
                best = c;
            }
        }
        return best;
    }

    public synchronized List<LiveMatchDto> live() {
        List<LiveMatchDto> out = new ArrayList<LiveMatchDto>();
        for (MatchRoom r : byId.values()) {
            out.add(new LiveMatchDto(r.id, r.seats[0].username, r.seats[1].username, r.seats[0].name,
                    r.seats[1].name, r.match.tick(), r.match.player(0).crowns(),
                    r.match.player(1).crowns(), r.seats[1].isBot()));
        }
        return out;
    }

    public synchronized int online() {
        return byPlayer.size() + queue.size();
    }

    // ------------------------------------------------------------ the ticker

    @Scheduled(fixedRate = 50, initialDelay = 50)
    public void ticker() {
        tick(Clock.now());
    }

    /// Advances every match to where the wall clock says it should be, then
    /// records the ones that ended. Tests call this with their own clock.
    public void tick(long now) {
        List<MatchRoom> ended = new ArrayList<MatchRoom>();
        synchronized (this) {
            pair(now);
            for (MatchRoom room : byId.values()) {
                long due = (now - room.startedAt) / TICK_MILLIS;
                int steps = 0;
                while (!room.isOver() && room.match.tick() < due && steps < MAX_CATCH_UP) {
                    room.step();
                    steps++;
                    if (room.match.tick() % GameProtocol.HASH_EVERY == 0) {
                        broadcast(room, Messages.hash(room.match.tick(), room.match.stateHash()));
                    }
                }
                if (steps > 0 && (room.match.tick() - room.lastSentTick >= 2 || room.sent < room.log.size())) {
                    flush(room);
                }
                if (room.isOver()) {
                    ended.add(room);
                }
            }
            for (MatchRoom room : ended) {
                byId.remove(room.id);
                for (Seat s : room.seats) {
                    if (byPlayer.get(s.username) == room) {
                        byPlayer.remove(s.username);
                    }
                }
            }
        }
        for (MatchRoom room : ended) {
            finish(room);
        }
    }

    /// Steps one room straight to the end without waiting for the clock; tests use
    /// it to play a whole match in milliseconds.
    public void fastForward(String matchId) {
        synchronized (this) {
            MatchRoom room = byId.get(matchId);
            if (room == null) {
                return;
            }
            while (!room.isOver()) {
                room.step();
            }
            flush(room);
        }
        tick(Clock.now());
    }

    private void finish(MatchRoom room) {
        Results.Outcome[] outcome;
        try {
            outcome = results.record(room);
        } catch (IOException | RuntimeException failed) {
            System.err.println("[match] could not record " + room.id + ": " + failed);
            outcome = new Results.Outcome[] {new Results.Outcome(0, 0), new Results.Outcome(0, 0)};
        }
        int result = room.result();
        for (int i = 0; i < 2; i++) {
            Seat s = room.seats[i];
            if (s.session != null) {
                send(s.session, Messages.end(result, room.match.player(0).crowns(), room.match.player(1).crowns(),
                        outcome[i].trophies, outcome[i].coins, room.id));
            }
            if (!s.isBot() && s.rejected + s.floods >= 25) {
                flags.auto(s.username, "Sent " + (s.rejected + s.floods)
                        + " invalid or excess plays in one match", room.id);
            }
        }
        String end = Messages.end(result, room.match.player(0).crowns(), room.match.player(1).crowns(), 0, 0, room.id);
        for (WebSocketSession s : room.spectators) {
            send(s, end);
        }
    }

    private void flush(MatchRoom room) {
        String msg = Messages.ticks(room.match.tick(), room.log, room.sent);
        room.sent = room.log.size();
        room.lastSentTick = room.match.tick();
        broadcast(room, msg);
    }

    private void broadcast(MatchRoom room, String msg) {
        for (Seat s : room.seats) {
            if (s.session != null) {
                send(s.session, msg);
            }
        }
        for (WebSocketSession s : room.spectators) {
            send(s, msg);
        }
    }

    static void send(WebSocketSession session, String msg) {
        try {
            // A seat with no session is a headless player: the bot, or a test
            // driving the room directly.
            if (session != null && session.isOpen()) {
                session.sendText(msg);
            }
        } catch (IOException ignored) {
            // The socket is going away; its close callback cleans up.
        }
    }
}
// end::skirmish-rooms[]
