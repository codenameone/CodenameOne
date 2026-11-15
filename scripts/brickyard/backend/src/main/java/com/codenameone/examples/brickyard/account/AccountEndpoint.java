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
import com.codenameone.examples.brickyard.api.LimitsDto;
import com.codenameone.examples.brickyard.api.PushDeviceDto;
import com.codenameone.examples.brickyard.api.ProfileDto;
import com.codenameone.examples.brickyard.api.RegisterDto;
import com.codenameone.examples.brickyard.push.PushDevices;
import com.codenameone.examples.brickyard.push.Pushes;

import java.util.List;

/// The account routes; the work is in [Accounts].
@Component
public class AccountEndpoint implements AccountApiServer {
    private final Accounts accounts;
    private final Limits limits;
    private final PushDevices devices;
    private final Pushes pushes;

    public AccountEndpoint(Accounts accounts, Limits limits, PushDevices devices, Pushes pushes) {
        this.accounts = accounts;
        this.limits = limits;
        this.devices = devices;
        this.pushes = pushes;
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
        String me = Caller.name();
        if (profile != null && profile.avatar() != null) {
            limits.saveAvatar(me, profile.avatar());
        }
        if (profile == null || profile.displayName() == null || profile.displayName().length() == 0) {
            return accounts.describe(me);
        }
        return accounts.rename(me, profile.displayName());
    }

    @Override
    public AccountDto becomeCreator() throws Exception {
        return accounts.becomeCreator(Caller.name());
    }

    @Override
    public FriendDto addFriend(FriendDto friend) throws Exception {
        String me = Caller.name();
        FriendDto added = accounts.addFriend(me, friend == null ? null : friend.username());
        if (!accounts.friendsOf(me).contains(added.username())) {
            pushes.friendRequest(added.username(), me);
        }
        return added;
    }

    @Override
    public List<FriendDto> friends() throws Exception {
        return accounts.friendList(Caller.name());
    }

    @Override
    public ProfileDto profile() throws Exception {
        return limits.profile(Caller.name());
    }

    @Override
    public PushDeviceDto registerDevice(PushDeviceDto device) throws Exception {
        return devices.register(Caller.name(), device);
    }

    @Override
    public LimitsDto limits() throws Exception {
        return limits.limits(Caller.name());
    }

    @Override
    public LimitsDto saveLimits(LimitsDto request) throws Exception {
        return limits.save(Caller.name(), request);
    }
}
