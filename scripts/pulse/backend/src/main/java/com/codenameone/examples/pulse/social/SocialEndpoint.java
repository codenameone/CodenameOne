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
package com.codenameone.examples.pulse.social;

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.pulse.Caller;
import com.codenameone.examples.pulse.api.FeedItemDto;
import com.codenameone.examples.pulse.api.FriendDto;
import com.codenameone.examples.pulse.api.FriendRequestDto;
import com.codenameone.examples.pulse.api.InviteDto;
import com.codenameone.examples.pulse.api.NudgeDto;
import com.codenameone.examples.pulse.api.SocialApiServer;

import java.util.List;

/// The server's half of `SocialApi`.
@Component
public class SocialEndpoint implements SocialApiServer {
    private final Social social;

    public SocialEndpoint(Social social) {
        this.social = social;
    }

    @Override
    public List<FriendDto> friends() throws Exception {
        return social.friends(Caller.name());
    }

    @Override
    public FriendDto request(FriendRequestDto body) throws Exception {
        return social.request(Caller.name(), body == null ? null : body.email());
    }

    @Override
    public FriendDto accept(String account) throws Exception {
        return social.accept(Caller.name(), account);
    }

    @Override
    public FriendDto remove(String account) throws Exception {
        return social.remove(Caller.name(), account);
    }

    @Override
    public List<FeedItemDto> feed() throws Exception {
        return social.feed(Caller.name());
    }

    @Override
    public FeedItemDto kudos(String id) throws Exception {
        return social.kudos(Caller.name(), id);
    }

    @Override
    public NudgeDto invite(InviteDto body) throws Exception {
        return social.invite(Caller.name(), body);
    }
}
