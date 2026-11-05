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
package com.codenameone.examples.slate.admin;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.PreAuthorize;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.slate.Caller;
import com.codenameone.examples.slate.account.Accounts;
import com.codenameone.examples.slate.account.Moderation;
import com.codenameone.examples.slate.api.AdminApiServer;
import com.codenameone.examples.slate.api.FlagDto;
import com.codenameone.examples.slate.api.RoleChangeDto;
import com.codenameone.examples.slate.api.SuspendDto;
import com.codenameone.examples.slate.api.UserDetailDto;
import com.codenameone.examples.slate.api.UserDto;
import com.codenameone.examples.slate.booking.States;
import com.codenameone.examples.slate.live.LiveHub;
import com.codenameone.examples.slate.repository.AppointmentRepository;

import java.util.List;

/// The server's half of `AdminApi`: the owner's view of the people with
/// accounts. Guarded by path in the filter chain and by role here.
///
/// This is one of the few places a name in the path chooses whose data is
/// read. Everywhere a client is the caller, they are the subject.
@Component
@PreAuthorize("hasRole('OWNER')")
public class AdminEndpoint implements AdminApiServer {
    private final Accounts accounts;
    private final Moderation moderation;
    private final AppointmentRepository appointments;
    private final LiveHub live;

    public AdminEndpoint(Accounts accounts, Moderation moderation,
            AppointmentRepository appointments, LiveHub live) {
        this.accounts = accounts;
        this.moderation = moderation;
        this.appointments = appointments;
        this.live = live;
    }

    @Override
    public List<UserDto> users() throws Exception {
        return accounts.all();
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetailDto user(String username) throws Exception {
        UserDto user = accounts.describe(username);
        Long spent = appointments.spent(username);
        return new UserDetailDto(user, (int) appointments.countByClient(username),
                (int) appointments.countByClientAndState(username, States.NO_SHOW),
                spent == null ? 0L : spent.longValue(), moderation.events(username));
    }

    @Override
    public UserDto setRoles(String username, RoleChangeDto roles) throws Exception {
        if (roles == null) {
            throw new ResponseStatusException(400, "Say which roles");
        }
        return accounts.setRoles(Caller.name(), username, roles.provider(), roles.owner());
    }

    @Override
    public UserDto suspend(String username, SuspendDto suspend) throws Exception {
        UserDto user = accounts.suspend(Caller.name(), username,
                suspend != null && suspend.suspended(), suspend == null ? "" : suspend.reason());
        if (user.suspended()) {
            // The API turns a suspended account away on its next request. A
            // live channel already open makes no further request, so it is
            // closed here.
            live.drop(username);
        }
        return user;
    }

    @Override
    public UserDto flag(String username, FlagDto flag) throws Exception {
        return accounts.flag(Caller.name(), username, flag != null && flag.flagged(),
                flag == null ? "" : flag.reason());
    }
}
