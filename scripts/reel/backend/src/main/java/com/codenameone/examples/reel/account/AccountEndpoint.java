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
package com.codenameone.examples.reel.account;

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.reel.Caller;
import com.codenameone.examples.reel.api.AccountApiServer;
import com.codenameone.examples.reel.api.AccountClosureDto;
import com.codenameone.examples.reel.api.AccountDto;
import com.codenameone.examples.reel.api.DeviceDto;
import com.codenameone.examples.reel.api.HouseholdProfileDto;
import com.codenameone.examples.reel.api.PasswordChangeDto;
import com.codenameone.examples.reel.api.PinDto;
import com.codenameone.examples.reel.api.ProfileDto;
import com.codenameone.examples.reel.api.ProfileEditDto;
import com.codenameone.examples.reel.api.PushDeviceDto;
import com.codenameone.examples.reel.api.SessionDto;
import com.codenameone.examples.reel.api.SignupDto;
import com.codenameone.examples.reel.playback.Playbacks;
import com.codenameone.examples.reel.push.PushDevices;

import java.util.List;

@Component
public class AccountEndpoint implements AccountApiServer {
    private final Accounts accounts;
    private final Households households;
    private final Devices devices;
    private final PushDevices pushes;
    private final Playbacks playbacks;

    public AccountEndpoint(Accounts accounts, Households households, Devices devices,
            PushDevices pushes, Playbacks playbacks) {
        this.accounts = accounts;
        this.households = households;
        this.devices = devices;
        this.pushes = pushes;
        this.playbacks = playbacks;
    }

    @Override
    public AccountDto signup(SignupDto signup) throws Exception {
        return accounts.register(signup);
    }

    /// Also where a device learns it was signed out: [Devices#touch] answers 401.
    @Override
    public AccountDto me() throws Exception {
        String who = Caller.name();
        devices.touch(who);
        accounts.seen(who);
        return accounts.describe(who);
    }

    @Override
    public AccountDto updateProfile(ProfileDto profile) throws Exception {
        return accounts.saveProfile(Caller.name(), profile);
    }

    @Override
    public String changePassword(PasswordChangeDto change) throws Exception {
        accounts.changePassword(Caller.name(), change == null ? null : change.current(),
                change == null ? null : change.replacement());
        return "ok";
    }

    @Override
    public String close(AccountClosureDto closure) throws Exception {
        accounts.close(Caller.name(), closure == null ? null : closure.password());
        return "closed";
    }

    @Override
    public List<DeviceDto> devices() throws Exception {
        return devices.list(Caller.name());
    }

    @Override
    public String signOutDevice(String id) throws Exception {
        String who = Caller.name();
        devices.signOut(who, id);
        playbacks.endForDevice(who, id);
        return "ok";
    }

    @Override
    public List<SessionDto> sessions() throws Exception {
        return playbacks.liveFor(Caller.name());
    }

    @Override
    public String registerPush(PushDeviceDto device) throws Exception {
        pushes.register(Caller.name(), device);
        return "ok";
    }

    @Override
    public List<HouseholdProfileDto> profiles() throws Exception {
        String who = Caller.name();
        return households.list(who, accounts.displayName(who));
    }

    @Override
    public HouseholdProfileDto createProfile(ProfileEditDto profile) throws Exception {
        return households.create(Caller.name(), profile);
    }

    @Override
    public HouseholdProfileDto updateHouseholdProfile(String id, ProfileEditDto profile) throws Exception {
        return households.update(Caller.name(), id, profile);
    }

    @Override
    public String deleteProfile(String id) throws Exception {
        households.delete(Caller.name(), id);
        return "ok";
    }

    @Override
    public HouseholdProfileDto unlockProfile(String id, PinDto pin) throws Exception {
        return households.unlock(Caller.name(), id, pin == null ? null : pin.pin());
    }
}
