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
package com.codenameone.examples.reel.moderation;

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.reel.Caller;
import com.codenameone.examples.reel.account.Moderation;
import com.codenameone.examples.reel.api.BlocklistEntryDto;
import com.codenameone.examples.reel.api.ModerationApiServer;
import com.codenameone.examples.reel.api.ModerationDecisionDto;
import com.codenameone.examples.reel.api.ModerationEventDto;
import com.codenameone.examples.reel.api.ModerationItemDto;

import java.util.List;

/// The moderator's console. `/api/moderation/**` is limited to moderators and
/// admins in [com.codenameone.examples.reel.SecurityConfig].
@Component
public class ModerationEndpoint implements ModerationApiServer {
    private final Reviews reviews;
    private final Moderation history;

    public ModerationEndpoint(Reviews reviews, Moderation history) {
        this.reviews = reviews;
        this.history = history;
    }

    @Override
    public List<ModerationItemDto> queue(String kind) throws Exception {
        return reviews.queue(kind);
    }

    @Override
    public ModerationItemDto decide(String id, ModerationDecisionDto decision) throws Exception {
        return reviews.decide(Caller.name(), id, decision);
    }

    @Override
    public List<BlocklistEntryDto> blocklist() throws Exception {
        return reviews.blocklist();
    }

    @Override
    public BlocklistEntryDto block(BlocklistEntryDto entry) throws Exception {
        return reviews.block(Caller.name(), entry);
    }

    @Override
    public String unblock(String id) throws Exception {
        reviews.unblock(id);
        return "ok";
    }

    @Override
    public List<ModerationEventDto> events(String username) throws Exception {
        return history.events(username);
    }
}
