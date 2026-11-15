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
import com.codename1.backend.annotations.Component;
import com.codename1.rbx.Engine;
import com.codename1.rbx.RigidBodies;
import com.codename1.rbx.place.PlaceFile;
import com.codename1.rbx.place.PlaceLoader;
import com.codenameone.examples.brickyard.Ids;
import com.codenameone.examples.brickyard.api.PlaceDto;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/// The directory of running servers, and matchmaking: a player goes to a
/// public server of the experience with a free seat, or a new one.
@Component
public class PlayServers {
    /// A requested server id that does not exist yet opens a private server
    /// under that name when it starts with this.
    public static final String PRIVATE_PREFIX = "private-";

    private final Map<String, PlayRoom> rooms = new ConcurrentHashMap<String, PlayRoom>();

    /// Seats `username` and answers the room. `requested` (may be empty)
    /// names a friend's or a private server.
    public synchronized PlayRoom seat(String username, String experienceId, int maxPlayers, String requested,
            PlaceDto place) throws IOException {
        if (requested != null && requested.length() > 0) {
            PlayRoom r = rooms.get(requested);
            if (r == null || r.isClosed()) {
                if (!requested.startsWith(PRIVATE_PREFIX) || requested.length() > 64) {
                    throw new ResponseStatusException(404, "That server has closed");
                }
                r = open(requested, experienceId, maxPlayers, false, place);
            }
            if (!r.getExperienceId().equals(experienceId)) {
                throw new ResponseStatusException(404, "That server runs another experience");
            }
            if (!r.reserve(username)) {
                throw new ResponseStatusException(409, "That server is full");
            }
            return r;
        }
        for (PlayRoom r : rooms.values()) {
            if (r.isMatchmade() && r.getExperienceId().equals(experienceId) && r.reserve(username)) {
                return r;
            }
        }
        PlayRoom r = open("srv-" + Ids.next(), experienceId, maxPlayers, true, place);
        r.reserve(username);
        return r;
    }

    private PlayRoom open(String id, String experienceId, int maxPlayers, boolean matchmade, PlaceDto place) {
        PlayRoom r = new PlayRoom(id, experienceId, maxPlayers, matchmade, load(place));
        rooms.put(id, r);
        r.start(new PlayRoom.Closed() {
            @Override
            public void closed(PlayRoom room) {
                rooms.remove(room.getId(), room);
            }
        });
        return r;
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

    public PlayRoom room(String id) {
        return id == null ? null : rooms.get(id);
    }

    /// The running servers of `experienceId`.
    public List<PlayRoom> of(String experienceId) {
        List<PlayRoom> out = new ArrayList<PlayRoom>();
        for (PlayRoom r : rooms.values()) {
            if (r.getExperienceId().equals(experienceId)) {
                out.add(r);
            }
        }
        return out;
    }
}
