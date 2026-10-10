/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codenameone.examples.wayline.account;

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.wayline.Caller;
import com.codenameone.examples.wayline.api.AccountApiServer;
import com.codenameone.examples.wayline.api.PasswordChangeDto;
import com.codenameone.examples.wayline.api.PhoneChallengeDto;
import com.codenameone.examples.wayline.api.PhoneCodeDto;
import com.codenameone.examples.wayline.api.PreferencesDto;
import com.codenameone.examples.wayline.api.ProfileDto;
import com.codenameone.examples.wayline.api.RegisterDto;
import com.codenameone.examples.wayline.api.TicketDto;
import com.codenameone.examples.wayline.api.UserDto;
import com.codenameone.examples.wayline.live.LiveTickets;

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

    public AccountEndpoint(Accounts accounts, PhoneVerification phones, LiveTickets tickets,
            Preferences preferences) {
        this.accounts = accounts;
        this.phones = phones;
        this.tickets = tickets;
        this.preferences = preferences;
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
        return phones.verify(Caller.name(), code == null ? null : code.code);
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
        return accounts.changePassword(Caller.name(), change == null ? null : change.current,
                change == null ? null : change.replacement);
    }

    @Override
    public UserDto deleteAccount() throws Exception {
        return accounts.close(Caller.name());
    }
}
