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

import com.codename1.backend.Crypto;
import com.codename1.backend.WebSocketSession;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Value;
import com.codenameone.examples.splat.Clock;
import com.codenameone.examples.splat.Ids;
import com.codenameone.examples.splat.admin.AdminChannel;
import com.codenameone.examples.splat.admin.Flags;
import com.codenameone.examples.splat.api.ChallengeDto;
import com.codenameone.examples.splat.api.GameProtocol;
import com.codenameone.examples.splat.api.LiveRaceDto;
import com.codenameone.examples.splat.domain.Profile;
import com.codenameone.examples.splat.rules.Catalog;
import com.codenameone.examples.splat.rules.Input;
import com.codenameone.examples.splat.rules.Match;
import com.codenameone.examples.splat.social.Crews;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/// Parties, quick-match queues and every match in progress.
///
/// All state is guarded by this object's monitor: socket callbacks and the
/// ticker run on different threads. Database work and the final messages of a
/// race happen outside the lock, in [#tick], so a slow database never stalls
/// the other races.
// tag::splat-rooms[]
@Component
public class Rooms {
    /// The ticker runs every 50 ms and steps each race to the wall clock.
    static final long TICKER_MILLIS = 50;
    /// How many ticks one ticker run may catch up, so a long pause cannot stall
    /// the server re-simulating minutes of race.
    static final int MAX_CATCH_UP = 30;
    /// A quick match is 4v4, bots filling the gaps.
    public static final int QUICK_PLAYERS = 8;
    /// The bots are the regulars every paintball field has.
    static final String[] BOT_NAMES = {"Uncle Dave", "Brayden (11)", "Tactical Greg", "Sharon From Accounts",
        "Coach Phil", "The Groom", "Best Man Kyle", "Steve Who Brought His Own Marker", "Grandpa Lou",
        "Kevin (Disputes Everything)", "Mandy", "A Guy In Jeans"};

    private final Results results;
    private final Flags flags;
    private final Challenges challenges;
    private final AdminChannel admins;
    private final Crews crews;
    private final long botAfterMillis;

    private final Map<String, MatchRoom> byId = new HashMap<String, MatchRoom>();
    private final Map<String, MatchRoom> byPlayer = new HashMap<String, MatchRoom>();
    /// Quick-race queues, one per level.
    private final Map<String, List<Waiting>> queues = new HashMap<String, List<Waiting>>();
    /// Parties by challenge code, and the code each member is in.
    private final Map<String, Party> parties = new HashMap<String, Party>();
    private final Map<String, String> partyOf = new HashMap<String, String>();

    static final class Waiting {
        final Profile profile;
        WebSocketSession session;
        final long since;

        Waiting(Profile profile, WebSocketSession session, long since) {
            this.profile = profile;
            this.session = session;
            this.since = since;
        }
    }

    /// Friends gathered on one code before the host says go. Voice and chat
    /// work in the lobby too, so the party can talk before it races.
    static final class Party {
        final String code;
        final String url;
        final String host;
        String level;
        final List<Waiting> members = new ArrayList<Waiting>();

        Party(String code, String url, String host, String level) {
            this.code = code;
            this.url = url;
            this.host = host;
            this.level = level;
        }
    }

    public Rooms(Results results, Flags flags, Challenges challenges, AdminChannel admins,
            Crews crews, @Value("${splat.race.bot-after-ms:6000}") long botAfterMillis) {
        this.crews = crews;
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
            send(session, Messages.start(room, seat, room.startedAt - Clock.now()));
            return;
        }
        Party party = parties.get(partyOf.get(username));
        if (party != null) {
            for (Waiting w : party.members) {
                if (w.profile.getUsername().equals(username)) {
                    w.session = session;
                }
            }
            send(session, partyMessage(party));
        }
    }

    synchronized void disconnected(String username, WebSocketSession session) {
        admins.remove(username, session);
        removeQueued(username);
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
        // A party member who drops keeps their place until the race starts or
        // they leave; the socket is simply gone until they reconnect.
        Party party = parties.get(partyOf.get(username));
        if (party != null) {
            for (Waiting w : party.members) {
                if (w.session == session) {
                    w.session = null;
                }
            }
        }
    }

    /// Closes everything `username` has open: used when an admin suspends or bans.
    public void kick(String username) {
        WebSocketSession session = null;
        synchronized (this) {
            removeQueued(username);
            leaveParty(username);
            MatchRoom room = byPlayer.get(username);
            if (room != null) {
                Seat s = room.seats[room.seatOf(username)];
                session = s.session;
                s.left = true;
            }
        }
        if (session != null) {
            send(session, Messages.simple(GameProtocol.KICKED));
            session.close(4403, "suspended");
        }
    }

    // ------------------------------------------------------------ parties

    /// Opens a party on `level` and answers its share link. With a `crew`
    /// code the host belongs to, the rest of that crew is pushed the invite.
    void host(Profile player, WebSocketSession session, String level, String crew) throws IOException {
        String ref = Arenas.normalize(level);
        if (ref == null) {
            send(session, Messages.error("No such field or mode"));
            return;
        }
        ChallengeDto c = challenges.create(player.getUsername());
        synchronized (this) {
            abandonMatch(player.getUsername());
            leaveParty(player.getUsername());
            removeQueued(player.getUsername());
            Party party = new Party(c.code(), c.url(), player.getUsername(), ref);
            party.members.add(new Waiting(player, session, Clock.now()));
            parties.put(party.code, party);
            partyOf.put(player.getUsername(), party.code);
            broadcast(party, partyMessage(party));
        }
        if (crew != null) {
            crews.callToRace(player.getUsername(), player.getDisplayName(), crew, c.code());
        }
    }

    synchronized void join(Profile player, WebSocketSession session, String code) {
        Party party = code == null ? null : parties.get(code);
        if (party == null || challenges.hostOf(code) == null) {
            send(session, Messages.error("That party has closed"));
            return;
        }
        abandonMatch(player.getUsername());
        if (code.equals(partyOf.get(player.getUsername()))) {
            send(session, partyMessage(party));
            return;
        }
        if (party.members.size() >= GameProtocol.MAX_PLAYERS) {
            send(session, Messages.error("That party is full"));
            return;
        }
        leaveParty(player.getUsername());
        removeQueued(player.getUsername());
        party.members.add(new Waiting(player, session, Clock.now()));
        partyOf.put(player.getUsername(), code);
        challenges.joinedWhileAway(code, party.host, player.getDisplayName());
        broadcast(party, partyMessage(party));
    }

    /// The host starts the race. A party of one gets bots, so hosting alone
    /// is still a race.
    synchronized void go(String username, String level) {
        Party party = parties.get(partyOf.get(username));
        if (party == null || !party.host.equals(username)) {
            return;
        }
        if (level != null) {
            String ref = Arenas.normalize(level);
            if (ref != null) {
                party.level = ref;
            }
        }
        parties.remove(party.code);
        for (Waiting w : party.members) {
            partyOf.remove(w.profile.getUsername());
        }
        challenges.used(party.code);
        // Bots round every party up to even teams of at least four.
        int n = Math.max(QUICK_PLAYERS, party.members.size() + (party.members.size() & 1));
        start(party.level, party.members, n);
    }

    private Party leaveParty(String username) {
        String code = partyOf.remove(username);
        Party party = code == null ? null : parties.get(code);
        if (party == null) {
            return null;
        }
        for (Iterator<Waiting> it = party.members.iterator(); it.hasNext();) {
            if (it.next().profile.getUsername().equals(username)) {
                it.remove();
            }
        }
        if (party.members.isEmpty() || party.host.equals(username)) {
            // The host leaving closes the party for everyone.
            parties.remove(code);
            for (Waiting w : party.members) {
                partyOf.remove(w.profile.getUsername());
                send(w.session, Messages.error("The host closed the party"));
            }
        } else {
            broadcast(party, partyMessage(party));
        }
        return party;
    }

    private static String partyMessage(Party party) {
        List<String> names = new ArrayList<String>();
        List<String> players = new ArrayList<String>();
        for (Waiting w : party.members) {
            names.add(w.profile.getDisplayName());
            players.add(w.profile.getUsername());
        }
        return Messages.party(party.code, party.url, party.level, party.host, names, players);
    }

    // ------------------------------------------------------------ quick races

    synchronized void quick(Profile player, WebSocketSession session, String level) {
        String ref = Arenas.normalize(level);
        if (ref == null) {
            send(session, Messages.error("No such field or mode"));
            return;
        }
        abandonMatch(player.getUsername());
        removeQueued(player.getUsername());
        leaveParty(player.getUsername());
        List<Waiting> q = queues.get(ref);
        if (q == null) {
            q = new ArrayList<Waiting>();
            queues.put(ref, q);
        }
        q.add(new Waiting(player, session, Clock.now()));
        send(session, Messages.simple(GameProtocol.QUEUED));
        match(Clock.now());
    }

    synchronized void cancel(String username) {
        removeQueued(username);
        leaveParty(username);
    }

    private void removeQueued(String username) {
        for (List<Waiting> q : queues.values()) {
            for (Iterator<Waiting> it = q.iterator(); it.hasNext();) {
                if (it.next().profile.getUsername().equals(username)) {
                    it.remove();
                }
            }
        }
    }

    /// Starts a queue's race once it is full, or once its oldest player has
    /// waited [#botAfterMillis], with bots in the empty lanes.
    private void match(long now) {
        for (Iterator<Map.Entry<String, List<Waiting>>> it = queues.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, List<Waiting>> e = it.next();
            List<Waiting> q = e.getValue();
            if (q.isEmpty()) {
                it.remove();
                continue;
            }
            if (q.size() >= QUICK_PLAYERS || now - q.get(0).since >= botAfterMillis) {
                List<Waiting> runners = new ArrayList<Waiting>();
                while (!q.isEmpty() && runners.size() < QUICK_PLAYERS) {
                    runners.add(q.remove(0));
                }
                start(e.getKey(), runners, QUICK_PLAYERS);
            }
        }
    }

    private void start(String level, List<Waiting> humans, int players) {
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
        int n = Math.max(humans.size(), Math.min(players, GameProtocol.MAX_PLAYERS));
        Seat[] seats = new Seat[n];
        int avg = 0;
        for (int i = 0; i < humans.size(); i++) {
            Profile p = humans.get(i).profile;
            // Humans alternate teams, so a party of friends is split fairly and the
            // bots that fill in are split the same way.
            seats[i] = new Seat(p.getUsername(), p.getDisplayName(), marker(p.getRunner()), p.getTrophies(), i % 2,
                    humans.get(i).session);
            avg += p.getTrophies();
        }
        avg = humans.isEmpty() ? 0 : avg / humans.size();
        for (int i = humans.size(); i < n; i++) {
            String name = BOT_NAMES[(int) (((seed >>> 3) + i) % BOT_NAMES.length)];
            String marker = Catalog.MARKERS[(int) (((seed >>> 7) + i) % Catalog.MARKERS.length)].id;
            seats[i] = new Seat("", name, marker, avg, i % 2, null);
        }
        MatchRoom room = new MatchRoom(id, level, Arenas.arena(level), Arenas.mode(level), seed, seats,
                Clock.now() + GameProtocol.COUNTDOWN_MILLIS);
        byId.put(id, room);
        for (Seat s : seats) {
            if (!s.isBot()) {
                byPlayer.put(s.username, room);
            }
        }
        for (int i = 0; i < n; i++) {
            send(seats[i].session, Messages.start(room, i, GameProtocol.COUNTDOWN_MILLIS));
        }
    }

    /// The marker a profile carries, or the default when it names none.
    static String marker(String id) {
        for (int i = 0; i < Catalog.MARKERS.length; i++) {
            if (Catalog.MARKERS[i].id.equals(id)) {
                return id;
            }
        }
        return "semi";
    }

    // ------------------------------------------------------------ in a match

    synchronized void input(String username, int seq, Input in) {
        MatchRoom room = byPlayer.get(username);
        // An input during the countdown is simply the first one used at tick 0.
        if (room != null) {
            room.input(room.seatOf(username), seq, in);
        }
    }

    /// Emotes, chat and voice signaling go to whoever shares a race or a party
    /// with the sender.
    /// Both carry the sender's display name, never the account name: that is
    /// an email address, and other runners see it.
    synchronized void emote(String username, String name, String emote) {
        if (emote == null || emote.length() > 32) {
            return;
        }
        relay(username, Messages.emote(name, emote), false);
    }

    synchronized void chat(String username, String name, String text) {
        if (text == null || text.trim().length() == 0) {
            return;
        }
        String t = text.length() > 200 ? text.substring(0, 200) : text;
        relay(username, Messages.chat(name, t), false);
    }

    /// One WebRTC signaling message to one peer. Only peers in the same race or
    /// party can be reached, so the socket is no relay for strangers.
    synchronized void voice(String username, String to, String data) {
        if (to == null || data == null || data.length() > GameProtocol.MAX_MESSAGE) {
            return;
        }
        for (WebSocketSession s : peers(username, to)) {
            send(s, Messages.voice(username, data));
        }
    }

    synchronized void mute(String username, boolean on) {
        relay(username, Messages.mute(username, on), true);
    }

    private void relay(String username, String msg, boolean includeSelf) {
        MatchRoom room = byPlayer.get(username);
        if (room != null) {
            for (Seat s : room.seats) {
                if (includeSelf || !s.username.equals(username)) {
                    send(s.session, msg);
                }
            }
            return;
        }
        Party party = parties.get(partyOf.get(username));
        if (party != null) {
            for (Waiting w : party.members) {
                if (includeSelf || !w.profile.getUsername().equals(username)) {
                    send(w.session, msg);
                }
            }
        }
    }

    private List<WebSocketSession> peers(String username, String to) {
        List<WebSocketSession> out = new ArrayList<WebSocketSession>();
        MatchRoom room = byPlayer.get(username);
        if (room != null) {
            int seat = room.seatOf(to);
            if (seat >= 0 && room.seats[seat].session != null) {
                out.add(room.seats[seat].session);
            }
            return out;
        }
        Party party = parties.get(partyOf.get(username));
        if (party != null && party.code.equals(partyOf.get(to))) {
            for (Waiting w : party.members) {
                if (w.profile.getUsername().equals(to) && w.session != null) {
                    out.add(w.session);
                }
            }
        }
        return out;
    }

    /// A player still seated in a match who asks for a party or a new match
    /// has walked away from the old one: reconnecting puts them back in their
    /// seat, but an explicit request for something else must win, or it is
    /// silently ignored until that match ends.
    private void abandonMatch(String username) {
        MatchRoom room = byPlayer.get(username);
        if (room != null) {
            room.seats[room.seatOf(username)].left = true;
            byPlayer.remove(username);
        }
    }

    synchronized void leave(String username) {
        abandonMatch(username);
        leaveParty(username);
        removeQueued(username);
    }

    synchronized boolean spectate(String raceId, WebSocketSession session) {
        MatchRoom room = byId.get(raceId);
        if (room == null) {
            return false;
        }
        room.spectators.add(session);
        send(session, Messages.start(room, -1, room.startedAt - Clock.now()));
        return true;
    }

    public synchronized String partyCode(String username) {
        String code = partyOf.get(username);
        return code == null ? "" : code;
    }

    public synchronized String raceOf(String username) {
        MatchRoom room = byPlayer.get(username);
        return room == null ? "" : room.id;
    }

    public synchronized List<LiveRaceDto> live() {
        List<LiveRaceDto> out = new ArrayList<LiveRaceDto>();
        for (MatchRoom r : byId.values()) {
            List<String> players = new ArrayList<String>();
            List<String> names = new ArrayList<String>();
            for (Seat s : r.seats) {
                players.add(s.username);
                names.add(s.name);
            }
            out.add(new LiveRaceDto(r.id, r.level, players, names, r.match.tick(), r.seats[r.leader()].name,
                    r.seats.length - r.humans()));
        }
        return out;
    }

    public synchronized int online() {
        int waiting = 0;
        for (List<Waiting> q : queues.values()) {
            waiting += q.size();
        }
        return byPlayer.size() + waiting + partyOf.size();
    }

    // ------------------------------------------------------------ the ticker

    @Scheduled(fixedRate = TICKER_MILLIS, initialDelay = TICKER_MILLIS)
    public void ticker() {
        tick(Clock.now());
    }

    /// Advances every race to where the wall clock says it should be, then
    /// records the ones that ended. Tests call this with their own clock.
    public void tick(long now) {
        List<MatchRoom> ended = new ArrayList<MatchRoom>();
        synchronized (this) {
            match(now);
            for (MatchRoom room : byId.values()) {
                if (now < room.startedAt) {
                    continue;
                }
                long due = (now - room.startedAt) * Match.TICK_HZ / 1000;
                int steps = 0;
                while (!room.isOver() && room.match.tick() < due && steps < MAX_CATCH_UP) {
                    step(room);
                    steps++;
                }

                if (room.isOver()) {
                    ended.add(room);
                }
            }
            for (MatchRoom room : ended) {
                detach(room);
            }
        }
        for (MatchRoom room : ended) {
            finish(room);
        }
    }

    private void step(MatchRoom room) {
        room.step();
        if (room.match.tick() % GameProtocol.SNAP_EVERY == 0 || room.isOver()) {
            snap(room);
        }
    }

    /// Steps one room straight to the end without waiting for the clock; tests use
    /// it to run a whole race in milliseconds.
    public void fastForward(String raceId) {
        MatchRoom room;
        synchronized (this) {
            room = byId.get(raceId);
            if (room == null) {
                return;
            }
            while (!room.isOver()) {
                step(room);
            }
            // Taken out under the same lock, so this thread records it and a
            // caller reading the history straight after this returns finds it.
            detach(room);
        }
        finish(room);
    }

    private void detach(MatchRoom room) {
        byId.remove(room.id);
        for (Seat s : room.seats) {
            if (byPlayer.get(s.username) == room) {
                byPlayer.remove(s.username);
            }
        }
    }

    private void finish(MatchRoom room) {
        Results.Outcome[] outcome;
        try {
            outcome = results.record(room);
        } catch (IOException | RuntimeException failed) {
            System.err.println("[race] could not record " + room.id + ": " + failed);
            outcome = new Results.Outcome[room.seats.length];
            for (int i = 0; i < outcome.length; i++) {
                outcome[i] = new Results.Outcome(0, 0);
            }
        }
        int[] places = room.places();
        int[] scores = new int[room.seats.length];
        for (int i = 0; i < scores.length; i++) {
            scores[i] = room.match.player(i).paints();
        }
        for (int i = 0; i < room.seats.length; i++) {
            Seat s = room.seats[i];
            send(s.session, Messages.end(room.id, places, scores, outcome[i].trophies, outcome[i].coins));
            if (!s.isBot() && s.floods >= 25) {
                flags.auto(s.username, "Sent " + s.floods + " inputs over the rate limit in one match", room.id);
            }
            if (!s.isBot() && s.snaps >= 20) {
                flags.auto(s.username, "Turned " + s.snaps + " times faster than a wrist can in one match", room.id);
            }
        }
        String end = Messages.end(room.id, places, scores, 0, 0);
        for (WebSocketSession s : room.spectators) {
            send(s, end);
        }
    }

    /// Sends each player the state, with the last of their own inputs it used.
    private void snap(MatchRoom room) {
        int[] state = room.match.snapshot();
        String events = room.drainEvents();
        for (int i = 0; i < room.seats.length; i++) {
            Seat s = room.seats[i];
            if (s.session != null) {
                send(s.session, Messages.snap(room.match.tick(), s.appliedSeq, state, events));
            }
        }
        String watch = Messages.snap(room.match.tick(), -1, state, events);
        for (WebSocketSession s : room.spectators) {
            send(s, watch);
        }
    }

    private void broadcast(MatchRoom room, String msg) {
        for (Seat s : room.seats) {
            send(s.session, msg);
        }
        for (WebSocketSession s : room.spectators) {
            send(s, msg);
        }
    }

    private void broadcast(Party party, String msg) {
        for (Waiting w : party.members) {
            send(w.session, msg);
        }
    }

    static void send(WebSocketSession session, String msg) {
        try {
            // A seat with no session is a headless runner: a bot, a dropped
            // player, or a test driving the room directly.
            if (session != null && session.isOpen()) {
                session.sendText(msg);
            }
        } catch (IOException ignored) {
            // The socket is going away; its close callback cleans up.
        }
    }
}
// end::splat-rooms[]
