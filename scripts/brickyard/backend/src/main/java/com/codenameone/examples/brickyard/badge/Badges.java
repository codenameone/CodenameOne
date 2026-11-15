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
package com.codenameone.examples.brickyard.badge;

import com.codename1.appkit.server.Caller;
import com.codename1.appkit.server.Text;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.brickyard.Access;
import com.codenameone.examples.brickyard.Clock;
import com.codenameone.examples.brickyard.Ids;
import com.codenameone.examples.brickyard.account.Accounts;
import com.codenameone.examples.brickyard.api.BadgeAwardDto;
import com.codenameone.examples.brickyard.api.BadgeDto;
import com.codenameone.examples.brickyard.api.BadgeSaveDto;
import com.codenameone.examples.brickyard.domain.BadgeAward;
import com.codenameone.examples.brickyard.domain.BadgeDefinition;
import com.codenameone.examples.brickyard.experience.Experiences;
import com.codenameone.examples.brickyard.repository.BadgeAwardRepository;
import com.codenameone.examples.brickyard.repository.BadgeDefinitionRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// Badges: a creator defines them, an engine server awards them, once each.
@Component
public class Badges {
    private final BadgeDefinitionRepository definitions;
    private final BadgeAwardRepository awards;
    private final Experiences experiences;
    private final Accounts accounts;

    public Badges(BadgeDefinitionRepository definitions, BadgeAwardRepository awards,
            Experiences experiences, Accounts accounts) {
        this.definitions = definitions;
        this.awards = awards;
        this.experiences = experiences;
        this.accounts = accounts;
    }

    @Transactional(rollbackFor = IOException.class)
    public BadgeDto define(String experienceId, BadgeSaveDto badge) throws IOException {
        Access.requireOwner(experiences.find(experienceId));
        if (badge == null) {
            throw new ResponseStatusException(400, "There is no badge");
        }
        BadgeDefinition b = new BadgeDefinition();
        b.setId(Ids.next());
        b.setExperienceId(experienceId);
        b.setName(Text.required(badge.name(), 60, "the badge name"));
        b.setDescription(badge.description() == null ? "" : Text.required(badge.description(), 255,
                "the description"));
        b.setCreatedAt(Clock.now());
        definitions.insert(b);
        return toDto(b);
    }

    @Transactional(readOnly = true)
    public List<BadgeDto> of(String experienceId) {
        experiences.visible(experienceId);
        List<BadgeDto> out = new ArrayList<BadgeDto>();
        for (BadgeDefinition b : definitions.of(experienceId)) {
            out.add(toDto(b));
        }
        return out;
    }

    /// Awards a badge; awarding one the player already holds answers the first award.
    @Transactional(rollbackFor = IOException.class)
    public BadgeAwardDto award(String badgeId, String userId) {
        Access.requireEngine();
        BadgeDefinition b = badgeId == null ? null : definitions.findById(badgeId);
        if (b == null) {
            throw new ResponseStatusException(404, "No such badge");
        }
        String user = accounts.account(userId).getUsername();
        String id = badgeId + "|" + user;
        BadgeAward a = awards.findById(id);
        if (a == null) {
            a = new BadgeAward();
            a.setId(id);
            a.setBadgeId(badgeId);
            a.setExperienceId(b.getExperienceId());
            a.setUserId(user);
            a.setAwardedAt(Clock.now());
            awards.insert(a);
        }
        return toDto(a);
    }

    @Transactional(readOnly = true)
    public List<BadgeAwardDto> mine() {
        List<BadgeAwardDto> out = new ArrayList<BadgeAwardDto>();
        for (BadgeAward a : awards.of(Caller.name())) {
            out.add(toDto(a));
        }
        return out;
    }

    private static BadgeDto toDto(BadgeDefinition b) {
        return new BadgeDto(b.getId(), b.getExperienceId(), b.getName(), b.getDescription());
    }

    private static BadgeAwardDto toDto(BadgeAward a) {
        return new BadgeAwardDto(a.getBadgeId(), a.getExperienceId(), a.getUserId(), a.getAwardedAt());
    }
}
