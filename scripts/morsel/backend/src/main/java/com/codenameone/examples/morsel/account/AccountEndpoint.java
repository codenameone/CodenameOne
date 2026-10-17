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
package com.codenameone.examples.morsel.account;

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.morsel.Caller;
import com.codenameone.examples.morsel.api.AccountApiServer;
import com.codenameone.examples.morsel.api.AccountClosureDto;
import com.codenameone.examples.morsel.api.PasswordChangeDto;
import com.codenameone.examples.morsel.api.PhoneChallengeDto;
import com.codenameone.examples.morsel.api.PhoneCodeDto;
import com.codenameone.examples.morsel.api.PreferencesDto;
import com.codenameone.examples.morsel.api.ProfileDto;
import com.codenameone.examples.morsel.api.PushDeviceDto;
import com.codenameone.examples.morsel.api.RegisterDto;
import com.codenameone.examples.morsel.api.TicketDto;
import com.codenameone.examples.morsel.api.UserDto;
import com.codenameone.examples.morsel.live.LiveTickets;
import com.codenameone.examples.morsel.push.PushDevices;

/// The server's half of `AccountApi`.
///
/// `AccountApiServer` is generated from the contract the app calls through, and
/// the build routes each of the contract's paths to this class. Change a path or
/// a type in the contract and both sides stop compiling until they agree.
@Component
public class AccountEndpoint implements AccountApiServer {
    private final Accounts accounts;
    private final PhoneVerification phones;
    private final LiveTickets tickets;
    private final Preferences preferences;
    private final PushDevices devices;

    public AccountEndpoint(Accounts accounts, PhoneVerification phones, LiveTickets tickets,
            Preferences preferences, PushDevices devices) {
        this.accounts = accounts;
        this.phones = phones;
        this.tickets = tickets;
        this.preferences = preferences;
        this.devices = devices;
    }

    @Override
    public UserDto register(RegisterDto request) throws Exception {
        return accounts.register(request);
    }

    @Override
    public UserDto me() throws Exception {
        return accounts.describe(Caller.name());
    }

    @Override
    public PhoneChallengeDto startPhoneVerification() throws Exception {
        return phones.start(Caller.name());
    }

    @Override
    public UserDto verifyPhone(PhoneCodeDto code) throws Exception {
        return phones.verify(Caller.name(), code == null ? null : code.code());
    }

    @Override
    public TicketDto liveTicket() throws Exception {
        return tickets.issue(Caller.name());
    }

    @Override
    public PreferencesDto preferences() throws Exception {
        return preferences.get(Caller.name());
    }

    @Override
    public PreferencesDto savePreferences(PreferencesDto wanted) throws Exception {
        return preferences.save(Caller.name(), wanted);
    }

    @Override
    public ProfileDto profile() throws Exception {
        return accounts.profile(Caller.name());
    }

    @Override
    public UserDto saveProfile(ProfileDto profile) throws Exception {
        return accounts.saveProfile(Caller.name(), profile);
    }

    @Override
    public UserDto changePassword(PasswordChangeDto change) throws Exception {
        return accounts.changePassword(Caller.name(), change == null ? null : change.current(),
                change == null ? null : change.replacement());
    }

    @Override
    public UserDto deleteAccount(AccountClosureDto closure) throws Exception {
        return accounts.close(Caller.name(), closure == null ? null : closure.password());
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
