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

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.brickyard.api.AwardDto;
import com.codenameone.examples.brickyard.api.BadgeApiServer;
import com.codenameone.examples.brickyard.api.BadgeAwardDto;
import com.codenameone.examples.brickyard.api.BadgeDto;
import com.codenameone.examples.brickyard.api.BadgeSaveDto;

import java.util.List;

/// The badge routes; the work is in [Badges].
@Component
public class BadgeEndpoint implements BadgeApiServer {
    private final Badges badges;

    public BadgeEndpoint(Badges badges) {
        this.badges = badges;
    }

    @Override
    public BadgeDto define(String id, BadgeSaveDto body) throws Exception {
        return badges.define(id, body);
    }

    @Override
    public List<BadgeDto> badges(String id) throws Exception {
        return badges.of(id);
    }

    @Override
    public BadgeAwardDto award(String badgeId, AwardDto body) throws Exception {
        return badges.award(badgeId, body == null ? null : body.userId());
    }

    @Override
    public List<BadgeAwardDto> mine() throws Exception {
        return badges.mine();
    }
}
