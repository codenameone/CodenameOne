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
package com.codenameone.examples.dash.account;

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.dash.Caller;
import com.codenameone.examples.dash.api.AccountApiServer;
import com.codenameone.examples.dash.api.PlayerDto;
import com.codenameone.examples.dash.api.ProfileDto;
import com.codenameone.examples.dash.api.PushDeviceDto;
import com.codenameone.examples.dash.api.RegisterDto;
import com.codenameone.examples.dash.api.TicketDto;
import com.codenameone.examples.dash.game.GameTickets;
import com.codenameone.examples.dash.push.PushDevices;

@Component
public class AccountEndpoint implements AccountApiServer {
    private final Accounts accounts;
    private final GameTickets tickets;
    private final PushDevices devices;

    public AccountEndpoint(Accounts accounts, GameTickets tickets, PushDevices devices) {
        this.accounts = accounts;
        this.tickets = tickets;
        this.devices = devices;
    }

    @Override
    public PlayerDto register(RegisterDto request) throws Exception {
        return accounts.register(request);
    }

    @Override
    public PlayerDto me() throws Exception {
        return accounts.describe(Caller.name());
    }

    @Override
    public PlayerDto saveProfile(ProfileDto profile) throws Exception {
        return accounts.rename(Caller.name(), profile == null ? null : profile.displayName());
    }

    @Override
    public TicketDto gameTicket() throws Exception {
        return tickets.issue(Caller.name());
    }

    @Override
    public PushDeviceDto registerPush(PushDeviceDto device) throws Exception {
        return devices.register(Caller.name(), device);
    }

    @Override
    public PushDeviceDto unregisterPush(PushDeviceDto device) throws Exception {
        return devices.unregister(Caller.name(), device);
    }
}
