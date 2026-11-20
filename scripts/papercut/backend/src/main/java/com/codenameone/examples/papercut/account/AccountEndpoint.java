/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
 */
package com.codenameone.examples.papercut.account;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.papercut.api.AccountApiServer;
import com.codenameone.examples.papercut.api.PlayerDto;
import com.codenameone.examples.papercut.api.ProfileDto;
import com.codenameone.examples.papercut.api.PushDeviceDto;
import com.codenameone.examples.papercut.api.RegisterDto;
import com.codenameone.examples.papercut.push.PushDevices;

/// `AccountApi` on the server: every call acts on the caller's own account.
@Component
public class AccountEndpoint implements AccountApiServer {
    private final Accounts accounts;
    private final PushDevices devices;

    public AccountEndpoint(Accounts accounts, PushDevices devices) {
        this.accounts = accounts;
        this.devices = devices;
    }

    @Override
    public PlayerDto register(RegisterDto request) throws Exception {
        return accounts.register(request);
    }

    @Override
    public PlayerDto me() throws Exception {
        return accounts.me(Caller.name());
    }

    @Override
    public PlayerDto saveProfile(ProfileDto profile) throws Exception {
        return accounts.saveProfile(Caller.name(), profile);
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
