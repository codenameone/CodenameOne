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
package com.codenameone.examples.brickyard.experience;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.brickyard.api.ExperienceApiServer;
import com.codenameone.examples.brickyard.api.ExperienceDto;
import com.codenameone.examples.brickyard.api.ExperienceSaveDto;
import com.codenameone.examples.brickyard.api.JoinDto;
import com.codenameone.examples.brickyard.api.JoinTicketDto;
import com.codenameone.examples.brickyard.api.PresenceDto;
import com.codenameone.examples.brickyard.api.PlaceDto;
import com.codenameone.examples.brickyard.api.PlaceUploadDto;
import com.codenameone.examples.brickyard.api.RollbackDto;
import com.codenameone.examples.brickyard.api.VersionDto;
import com.codenameone.examples.brickyard.api.VoteDto;

import com.codenameone.examples.brickyard.play.PlayRoom;
import com.codenameone.examples.brickyard.play.PlayServers;
import com.codenameone.examples.brickyard.play.PlayTickets;
import com.codenameone.examples.brickyard.social.Social;
import java.util.List;

/// The experience routes; the work is in [Experiences].
@Component
public class ExperienceEndpoint implements ExperienceApiServer {
    private final Experiences experiences;

    private final PlayServers servers;
    private final PlayTickets tickets;
    private final Social social;

    public ExperienceEndpoint(Experiences experiences, PlayServers servers, PlayTickets tickets, Social social) {
        this.experiences = experiences;
        this.servers = servers;
        this.tickets = tickets;
        this.social = social;
    }

    @Override
    public ExperienceDto create(ExperienceSaveDto body) throws Exception {
        return experiences.create(Caller.name(), body);
    }

    @Override
    public List<ExperienceDto> browse(String genre) throws Exception {
        return experiences.browse(genre);
    }

    @Override
    public ExperienceDto experience(String id) throws Exception {
        return experiences.describe(id);
    }

    @Override
    public VersionDto saveDraft(String id, PlaceUploadDto body) throws Exception {
        return experiences.upload(id, body == null ? null : body.data(), false);
    }

    @Override
    public VersionDto publish(String id, PlaceUploadDto body) throws Exception {
        return experiences.upload(id, body == null ? null : body.data(), true);
    }

    @Override
    public VersionDto rollback(String id, RollbackDto body) throws Exception {
        return experiences.rollback(id, body == null ? 0 : body.number());
    }

    @Override
    public List<VersionDto> versions(String id) throws Exception {
        return experiences.versions(id);
    }

    @Override
    public PlaceDto place(String id) throws Exception {
        return experiences.place(id);
    }

    @Override
    public ExperienceDto visit(String id) throws Exception {
        return experiences.visit(id);
    }

    @Override
    public ExperienceDto favourite(String id) throws Exception {
        return experiences.favourite(id);
    }

    @Override
    public ExperienceDto vote(String id, VoteDto body) throws Exception {
        return experiences.vote(id, body == null ? 0 : body.vote());
    }

    /// Matchmaking: seats the caller on a server of the experience (the one
    /// `serverId` names, else any public one with a free seat, else a new
    /// one), marks them in game there so friends can join, and answers a
    /// ticket for the game socket.
    @Override
    public JoinTicketDto join(String id, JoinDto body) throws Exception {
        String me = Caller.name();
        ExperienceDto e = experiences.describe(id);
        PlaceDto place = experiences.place(id);
        String requested = body == null || body.serverId() == null ? "" : body.serverId();
        PlayRoom room = servers.seat(me, id, Math.max(1, e.maxPlayers()), requested, place);
        social.setPresence(me, new PresenceDto(me, "IN_GAME", id, room.getId(), 0));
        return new JoinTicketDto(tickets.issue(me, room.getId()), room.getId(), PlayTickets.TICKET_SECONDS);
    }
}
