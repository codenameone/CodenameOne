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
package com.codenameone.examples.skirmish.push;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.push.PushTarget;
import com.codenameone.examples.skirmish.Ids;
import com.codenameone.examples.skirmish.Text;
import com.codenameone.examples.skirmish.api.PushDeviceDto;
import com.codenameone.examples.skirmish.domain.PushDevice;
import com.codenameone.examples.skirmish.repository.ProfileRepository;
import com.codenameone.examples.skirmish.repository.PushDeviceRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// tag::skirmish-push-devices[]
/// The devices each account is sent notifications on.
///
/// The app registers its token when somebody signs in and removes it when they
/// sign out. A token registered by a second account moves to it: the phone
/// belongs to whoever signed in on it last, and the one before is not to be
/// told about the new owner's deliveries.
@Component
public class PushDevices {
    /// How many devices one account keeps. A new one past this replaces the
    /// one heard from longest ago: a token the platform has replaced is never
    /// removed by its app, and would otherwise be sent to for good.
    static final int MAX_PER_ACCOUNT = 8;

    private final PushDeviceRepository devices;
    private final ProfileRepository profiles;

    public PushDevices(PushDeviceRepository devices, ProfileRepository profiles) {
        this.devices = devices;
        this.profiles = profiles;
    }

    @Transactional
    public PushDeviceDto register(String username, PushDeviceDto device) throws IOException {
        PushTarget target = target(device);
        String id = key(target);
        long now = System.currentTimeMillis();
        // Before anything is read, the account's profile row is held: a no-op
        // update that answers 0 when the profile is gone and otherwise keeps
        // the row locked until this commits. It settles two races.
        //
        // Against closing the account: Accounts.erase holds the same row
        // first and deletes it before it commits, so a registration either
        // runs first (and erase's cleanup then deletes the device with the
        // rest) or waits, finds no profile and is refused. Without it, a
        // registration authorized just before the closure could insert its
        // device after erase removed the account's, leaving a device of an
        // account that no longer exists and that nobody can unregister.
        //
        // Against itself: two devices registering at once for an account one
        // short of the cap would both count the same rows in makeRoom(), both
        // skip the eviction and both insert, one past the cap. Behind the hold
        // the second counts after the first committed, and evicts.
        if (profiles.hold(username) == 0) {
            throw new ResponseStatusException(404, "No such account");
        }
        // The device row itself is not locked. Two accounts registering one
        // token at the same moment -- a registration of the account that
        // signed out still in flight as the next one signs in -- can leave it
        // with the earlier account. The app settles it: a registration that
        // answers after its account signed out is sent again for the account
        // signed in now, and that one, sent after the stale one committed,
        // lands last (Notifications.signedIn).
        PushDevice row = devices.findById(id);
        if (row == null || !username.equals(row.getUsername())) {
            // A device new to this account, whether its token is new or moves
            // here from another account, takes a place under the cap.
            makeRoom(username);
        }
        if (row == null) {
            row = new PushDevice();
            row.setId(id);
            row.setProvider(target.getProvider());
            row.setToken(target.getToken());
            fill(row, username, device, now);
            devices.insert(row);
        } else {
            fill(row, username, device, now);
        }
        return new PushDeviceDto(target.getProvider(), target.getToken(), row.getPlatform());
    }

    /// Removes `username`'s devices heard from longest ago until one more fits
    /// under [#MAX_PER_ACCOUNT].
    private void makeRoom(String username) {
        List<PushDevice> held = new ArrayList<PushDevice>(devices.findByUsername(username));
        while (held.size() >= MAX_PER_ACCOUNT) {
            PushDevice oldest = held.get(0);
            for (PushDevice each : held) {
                if (each.getUpdatedAt() < oldest.getUpdatedAt()) {
                    oldest = each;
                }
            }
            devices.deleteById(oldest.getId());
            held.remove(oldest);
        }
    }

    /// Forgets a device, if it is this account's. Somebody else's is left alone.
    @Transactional
    public PushDeviceDto unregister(String username, PushDeviceDto device) throws IOException {
        PushTarget target = target(device);
        devices.deleteByIdAndUsername(key(target), username);
        return new PushDeviceDto(target.getProvider(), target.getToken(), "");
    }

    /// Where to reach `username`: every device they registered.
    @Transactional(readOnly = true)
    public List<PushTarget> targets(String username) {
        return targetsOf(username);
    }

    /// [#targets] for a caller already inside a transaction.
    List<PushTarget> targetsOf(String username) {
        List<PushTarget> out = new ArrayList<PushTarget>();
        if (username == null || username.length() == 0) {
            return out;
        }
        for (PushDevice each : devices.findByUsername(username)) {
            out.add(PushTarget.of(each.getProvider(), each.getToken()));
        }
        return out;
    }
    // end::skirmish-push-devices[]

    private static void fill(PushDevice row, String username, PushDeviceDto device, long now) {
        row.setUsername(username);
        row.setPlatform(Text.optional(device.platform(), 32, "the platform"));
        row.setUpdatedAt(now);
    }

    /// The device as a target of the push service, which also spells its
    /// provider the one way: `gcm` and `fcm` are one device.
    private static PushTarget target(PushDeviceDto device) {
        if (device == null) {
            throw new ResponseStatusException(400, "Tell us the device");
        }
        String token = Text.required(device.token(), 1024, "the device's token");
        try {
            return PushTarget.of(Text.required(device.provider(), 16, "the push provider"), token);
        } catch (IllegalArgumentException err) {
            throw new ResponseStatusException(400, err.getMessage());
        }
    }

    private static String key(PushTarget target) throws IOException {
        return Ids.sha256(target.getProvider() + ":" + target.getToken());
    }
}
