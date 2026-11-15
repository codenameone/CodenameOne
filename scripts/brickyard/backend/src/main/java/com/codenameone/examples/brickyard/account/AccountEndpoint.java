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
package com.codenameone.examples.brickyard.account;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.brickyard.api.AccountApiServer;
import com.codenameone.examples.brickyard.api.AccountDto;
import com.codenameone.examples.brickyard.api.FriendDto;
import com.codenameone.examples.brickyard.api.ProfileDto;
import com.codenameone.examples.brickyard.api.RegisterDto;

import java.util.List;

/// The account routes; the work is in [Accounts].
@Component
public class AccountEndpoint implements AccountApiServer {
    private final Accounts accounts;

    public AccountEndpoint(Accounts accounts) {
        this.accounts = accounts;
    }

    @Override
    public AccountDto register(RegisterDto request) throws Exception {
        return accounts.register(request);
    }

    @Override
    public AccountDto me() throws Exception {
        return accounts.describe(Caller.name());
    }

    @Override
    public AccountDto saveProfile(ProfileDto profile) throws Exception {
        return accounts.rename(Caller.name(), profile == null ? null : profile.displayName());
    }

    @Override
    public AccountDto becomeCreator() throws Exception {
        return accounts.becomeCreator(Caller.name());
    }

    @Override
    public FriendDto addFriend(FriendDto friend) throws Exception {
        return accounts.addFriend(Caller.name(), friend == null ? null : friend.username());
    }

    @Override
    public List<FriendDto> friends() throws Exception {
        return accounts.friendList(Caller.name());
    }
}
