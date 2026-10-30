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
package com.codenameone.examples.dash.social;

import com.codename1.backend.Crypto;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.orm.repository.PageRequest;
import com.codenameone.examples.dash.Ids;
import com.codenameone.examples.dash.api.CrewDto;
import com.codenameone.examples.dash.domain.Crew;
import com.codenameone.examples.dash.domain.CrewMember;
import com.codenameone.examples.dash.domain.Profile;
import com.codenameone.examples.dash.repository.CrewMemberRepository;
import com.codenameone.examples.dash.repository.CrewRepository;
import com.codenameone.examples.dash.repository.ProfileRepository;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// Crews: small groups of friends with their own leaderboard. A crew is
/// joined by its code, which is also its share link.
@Component
public class Crews {
    public static final int MAX_MEMBERS = 30;
    public static final int MAX_CREWS = 5;

    private final CrewRepository crews;
    private final CrewMemberRepository members;
    private final ProfileRepository profiles;

    public Crews(CrewRepository crews, CrewMemberRepository members, ProfileRepository profiles) {
        this.crews = crews;
        this.members = members;
        this.profiles = profiles;
    }

    @Transactional(readOnly = true)
    public List<CrewDto> of(String username) throws IOException {
        List<CrewDto> out = new ArrayList<CrewDto>();
        for (CrewMember m : members.crewsOf(username, new PageRequest(0, MAX_CREWS))) {
            Crew c = crews.findById(m.getCode());
            if (c != null) {
                out.add(view(c));
            }
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    public CrewDto create(String username, String name) throws IOException {
        String n = name == null ? "" : name.trim();
        if (n.length() < 2 || n.length() > 40) {
            throw new ResponseStatusException(400, "A crew name is 2 to 40 characters");
        }
        if (members.crewsOf(username, new PageRequest(0, MAX_CREWS)).size() >= MAX_CREWS) {
            throw new ResponseStatusException(409, "You are in " + MAX_CREWS + " crews already");
        }
        Crew c = new Crew();
        c.setCode(Ids.hex(Crypto.randomBytes(4)).toUpperCase(java.util.Locale.ROOT));
        c.setName(n);
        c.setOwner(username);
        c.setCreatedAt(Instant.now());
        crews.insert(c);
        return join(username, c.getCode());
    }

    @Transactional(rollbackFor = IOException.class)
    public CrewDto join(String username, String code) throws IOException {
        Crew c = code == null ? null : crews.findById(code);
        if (c == null) {
            throw new ResponseStatusException(404, "No crew has that code");
        }
        String id = code + "|" + username;
        if (members.findById(id) == null) {
            if (c.getMembers() >= MAX_MEMBERS) {
                throw new ResponseStatusException(409, "That crew is full");
            }
            if (members.crewsOf(username, new PageRequest(0, MAX_CREWS)).size() >= MAX_CREWS) {
                throw new ResponseStatusException(409, "You are in " + MAX_CREWS + " crews already");
            }
            CrewMember m = new CrewMember();
            m.setId(id);
            m.setCode(code);
            m.setUsername(username);
            m.setJoinedAt(Instant.now());
            members.insert(m);
            c.setMembers(c.getMembers() + 1);
            crews.save(c);
        }
        return view(c);
    }

    @Transactional(rollbackFor = IOException.class)
    public CrewDto leave(String username, String code) throws IOException {
        Crew c = code == null ? null : crews.findById(code);
        CrewMember m = c == null ? null : members.findById(code + "|" + username);
        if (m == null) {
            throw new ResponseStatusException(404, "You are not in that crew");
        }
        members.deleteById(m.getId());
        c.setMembers(Math.max(0, c.getMembers() - 1));
        crews.save(c);
        return view(c);
    }

    private CrewDto view(Crew c) {
        List<String> usernames = new ArrayList<String>();
        List<String> names = new ArrayList<String>();
        for (CrewMember m : members.members(c.getCode(), new PageRequest(0, MAX_MEMBERS))) {
            Profile p = profiles.findById(m.getUsername());
            usernames.add(m.getUsername());
            names.add(p == null ? m.getUsername() : p.getDisplayName());
        }
        return new CrewDto(c.getCode(), c.getName(), c.getOwner(), usernames, names);
    }
}
