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
package com.codenameone.examples.brickyard.play;

import com.codename1.backend.Base64;
import com.codename1.backend.ResponseStatusException;
import com.codenameone.examples.brickyard.rbx.Engine;
import com.codenameone.examples.brickyard.rbx.RigidBodies;
import com.codenameone.examples.brickyard.rbx.place.PlaceFile;
import com.codenameone.examples.brickyard.rbx.place.PlaceLoader;
import com.codenameone.examples.brickyard.Clock;
import com.codenameone.examples.brickyard.Ids;
import com.codenameone.examples.brickyard.api.PlaceDto;
import com.codenameone.examples.brickyard.domain.PlayServer;
import com.codenameone.examples.brickyard.repository.PlayServerRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/// The directory of running servers, and matchmaking: a player goes to a
/// public server of the experience with a free seat, or a new one.
///
/// Rooms run on the instance that opened them; the directory itself is the
/// `play_server` table, so every instance sharing the database can send a
/// player to a room another instance runs. Each instance beats for its own
/// rooms ([#heartbeat]); a row whose instance stopped beating is reaped.
/// One instance is made per process by [PlayConfig].
public class PlayServers {
    /// A requested server id that does not exist yet opens a private server
    /// under that name when it starts with this.
    public static final String PRIVATE_PREFIX = "private-";
    /// How often an instance beats for its rooms.
    public static final long HEARTBEAT_MILLIS = 5000;
    /// A room whose row has not beaten for this long is taken as gone.
    public static final long STALE_MILLIS = 20000;

    /// Supplies the published place, read only when a room is really opened.
    public interface PlaceSource {
        PlaceDto place() throws IOException;
    }

    /// What a room tells the rest of the backend about its players.
    public interface Events {
        /// `username` connected to `room`.
        void joined(PlayRoom room, String username);

        /// `username` left `room` after `millis` connected, or gave up a
        /// seat they never connected to (`millis` is -1).
        void left(PlayRoom room, String username, long millis);

        /// A leaderstat of `username` in `room` rose by `delta`.
        default void scored(PlayRoom room, String username, String stat, long delta) {
        }
    }

    /// Where a join landed: the server, the socket URL of the instance that
    /// runs it (empty: this API host), and the room when it runs here.
    public static final class Placement {
        public final String serverId;
        public final String url;
        public final PlayRoom room;

        Placement(String serverId, String url, PlayRoom room) {
            this.serverId = serverId;
            this.url = url;
            this.room = room;
        }
    }

    private final Map<String, PlayRoom> rooms = new ConcurrentHashMap<String, PlayRoom>();
    private final PlayServerRepository rows;
    private final Events events;
    private final String instanceId;
    private final String socketUrl;

    public PlayServers(PlayServerRepository rows, Events events, String instanceId, String socketUrl) {
        this.rows = rows;
        this.events = events;
        this.instanceId = instanceId;
        this.socketUrl = socketUrl == null ? "" : socketUrl;
    }

    /// Seated players per experience over every live server, on any instance.
    public Map<String, Long> playingNow() {
        Map<String, Long> out = new java.util.HashMap<String, Long>();
        for (PlayServer row : rows.live(Clock.now() - STALE_MILLIS)) {
            if (row.getSeats() > 0) {
                Long n = out.get(row.getExperienceId());
                out.put(row.getExperienceId(), (n == null ? 0 : n.longValue()) + row.getSeats());
            }
        }
        return out;
    }

    public String getInstanceId() {
        return instanceId;
    }

    /// Seats `username` and answers where. `requested` (may be empty) names
    /// a friend's or a private server. `place` is read only when a new room
    /// opens here: seating into a running room never touches the place.
    public synchronized Placement seat(String username, String experienceId, int maxPlayers, String requested,
            PlaceSource place) throws IOException {
        long fresh = Clock.now() - STALE_MILLIS;
        if (requested != null && requested.length() > 0) {
            PlayRoom r = rooms.get(requested);
            if (r != null && !r.isClosed()) {
                return local(r, username, experienceId);
            }
            PlayServer row = rows.findById(requested);
            if (row != null && row.getHeartbeat() >= fresh && !instanceId.equals(row.getInstanceId())) {
                if (!row.getExperienceId().equals(experienceId)) {
                    throw new ResponseStatusException(404, "That server runs another experience");
                }
                if (row.getSeats() >= row.getMaxPlayers()) {
                    throw new ResponseStatusException(409, "That server is full");
                }
                return new Placement(row.getId(), row.getSocketUrl(), null);
            }
            if (!requested.startsWith(PRIVATE_PREFIX) || requested.length() > 64) {
                throw new ResponseStatusException(404, "That server has closed");
            }
            Placement elsewhere = claim(requested, experienceId, maxPlayers, row, fresh);
            if (elsewhere != null) {
                return elsewhere;
            }
            return local(open(requested, experienceId, maxPlayers, false, place), username, experienceId);
        }
        for (PlayRoom r : rooms.values()) {
            if (r.isMatchmade() && r.getExperienceId().equals(experienceId) && r.reserve(username)) {
                beat(r);
                return new Placement(r.getId(), socketUrl, r);
            }
        }
        for (PlayServer row : rows.liveOf(experienceId, fresh)) {
            if (row.getPrivateRoom() == 0 && !instanceId.equals(row.getInstanceId())
                    && row.getSeats() < row.getMaxPlayers()) {
                // The owning instance takes the seat when the ticket connects;
                // counting it here keeps a burst of joins from overfilling.
                row.setSeats(row.getSeats() + 1);
                rows.save(row);
                return new Placement(row.getId(), row.getSocketUrl(), null);
            }
        }
        return local(open("srv-" + Ids.next(), experienceId, maxPlayers, true, place), username, experienceId);
    }

    /// Claims the directory row for a private room this instance is about to
    /// open. Two instances asked for the same new private id at once both
    /// reach here; the primary key lets exactly one insert win, and the loser
    /// sends its player to the winner instead of opening a second copy. A row
    /// left by a dead instance (`existing`, stale) is taken over.
    private Placement claim(String id, String experienceId, int maxPlayers, PlayServer existing, long fresh) {
        if (existing != null) {
            rows.delete(existing);
        }
        PlayServer row = new PlayServer();
        row.setId(id);
        row.setExperienceId(experienceId);
        row.setInstanceId(instanceId);
        row.setSocketUrl(socketUrl);
        row.setSeats(0);
        row.setMaxPlayers(maxPlayers);
        row.setPrivateRoom(1);
        row.setHeartbeat(Clock.now());
        try {
            rows.insert(row);
            return null;
        } catch (RuntimeException lost) {
            PlayServer winner = rows.findById(id);
            if (winner == null || winner.getHeartbeat() < fresh || instanceId.equals(winner.getInstanceId())) {
                throw new ResponseStatusException(409, "That server is starting; try again");
            }
            if (!winner.getExperienceId().equals(experienceId)) {
                throw new ResponseStatusException(404, "That server runs another experience");
            }
            return new Placement(winner.getId(), winner.getSocketUrl(), null);
        }
    }

    private Placement local(PlayRoom r, String username, String experienceId) {
        if (!r.getExperienceId().equals(experienceId)) {
            throw new ResponseStatusException(404, "That server runs another experience");
        }
        if (!r.reserve(username)) {
            throw new ResponseStatusException(409, "That server is full");
        }
        beat(r);
        return new Placement(r.getId(), socketUrl, r);
    }

    private PlayRoom open(String id, String experienceId, int maxPlayers, boolean matchmade, PlaceSource place)
            throws IOException {
        PlayRoom r = new PlayRoom(id, experienceId, maxPlayers, matchmade, load(place.place()));
        rooms.put(id, r);
        beat(r);
        r.start(new PlayRoom.Listener() {
            @Override
            public void closed(PlayRoom room) {
                rooms.remove(room.getId(), room);
                PlayServer row = rows.findById(room.getId());
                if (row != null && instanceId.equals(row.getInstanceId())) {
                    rows.delete(row);
                }
            }

            @Override
            public void joined(PlayRoom room, String username) {
                if (events != null) {
                    events.joined(room, username);
                }
            }

            @Override
            public void left(PlayRoom room, String username, long millis) {
                if (events != null) {
                    events.left(room, username, millis);
                }
            }

            @Override
            public void scored(PlayRoom room, String username, String stat, long delta) {
                if (events != null) {
                    events.scored(room, username, stat, delta);
                }
            }
        });
        return r;
    }

    /// Writes `r`'s row: owner, seats and a fresh heartbeat.
    private void beat(PlayRoom r) {
        PlayServer row = rows.findById(r.getId());
        boolean fresh = row == null;
        if (fresh) {
            row = new PlayServer();
            row.setId(r.getId());
        }
        row.setExperienceId(r.getExperienceId());
        row.setInstanceId(instanceId);
        row.setSocketUrl(socketUrl);
        row.setSeats(r.getSeated());
        row.setMaxPlayers(r.getMaxPlayers());
        row.setPrivateRoom(r.isMatchmade() ? 0 : 1);
        row.setHeartbeat(Clock.now());
        if (fresh) {
            rows.insert(row);
        } else {
            rows.save(row);
        }
    }

    /// Beats for every room this instance runs and reaps rows no instance
    /// beats for any more. [PlayHeartbeat] calls it on a schedule.
    public void heartbeat() {
        for (PlayRoom r : rooms.values()) {
            if (!r.isClosed()) {
                beat(r);
            }
        }
        rows.reap(Clock.now() - STALE_MILLIS);
    }

    /// A SERVER engine with the place loaded and physics on.
    static Engine load(PlaceDto place) {
        Engine e = new Engine(Engine.SERVER);
        e.setPhysics(new RigidBodies());
        try {
            PlaceLoader.loadPlace(e, PlaceFile.read(Base64.decode(place.data())));
        } catch (RuntimeException bad) {
            throw new ResponseStatusException(500, "The published place does not load");
        }
        return e;
    }

    /// The room `id` when it runs on this instance.
    public PlayRoom room(String id) {
        return id == null ? null : rooms.get(id);
    }

    /// The running servers of `experienceId` on this instance.
    public List<PlayRoom> of(String experienceId) {
        List<PlayRoom> out = new ArrayList<PlayRoom>();
        for (PlayRoom r : rooms.values()) {
            if (r.getExperienceId().equals(experienceId)) {
                out.add(r);
            }
        }
        return out;
    }

    /// Closes every room this instance runs; for tests and shutdown.
    public void closeAll() {
        for (PlayRoom r : rooms.values()) {
            r.close();
        }
    }
}
