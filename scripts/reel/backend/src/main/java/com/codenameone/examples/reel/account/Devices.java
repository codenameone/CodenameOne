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

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.reel.Requests;
import com.codenameone.examples.reel.api.DeviceDto;
import com.codenameone.examples.reel.push.Notifier;
import com.codenameone.examples.reel.domain.Device;
import com.codenameone.examples.reel.repository.DeviceRepository;

import java.time.Instant;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// The devices an account has signed in on.
///
/// A device is known by the id the app made for itself and sends with every
/// call. Its owner can sign one out from another: the row is marked revoked,
/// and a revoked device is refused playback and told to sign in again on its
/// next call to `/api/me`. Signing out every device at once is changing the
/// password.
@Component
public class Devices {
    private final Grants grants;
    /// More than this and the oldest is forgotten, so a script making up ids
    /// cannot grow the table without end.
    private static final int MAX_DEVICES = 20;

    private final DeviceRepository devices;
    private final Notifier notifier;

    public Devices(DeviceRepository devices, Grants grants, Notifier notifier) {
        this.grants = grants;
        this.devices = devices;
        this.notifier = notifier;
    }

    /// Records that the calling device was used. Answers 401 for a device its
    /// owner signed out.
    @Transactional
    public void touch(String username) {
        String id = Requests.deviceId();
        if (id.length() == 0) {
            return;
        }
        Instant now = Instant.now();
        Device row = devices.findFirstByIdAndUsername(id, username);
        if (row == null) {
            Device elsewhere = devices.findById(id);
            if (elsewhere != null) {
                // One device, one account: the account that signed in on it last.
                devices.deleteById(id);
            }
            makeRoom(username);
            // The account's other devices hear of a new one: a sign-in the
            // owner did not make is the first sign of a shared password.
            boolean known = !devices.findByUsernameOrderByLastSeenAtDesc(username).isEmpty();
            row = new Device();
            row.setId(id);
            row.setUsername(username);
            row.setCreatedAt(now);
            fill(row, now);
            row.setGrantId(Grants.current());
            devices.insert(row);
            if (known) {
                notifier.account(username, "New sign-in", "Reel was opened on " + row.getName()
                        + ". If it was not you, change your password.", null);
            }
            return;
        }
        if (row.getRevokedAt().isAfter(Instant.EPOCH)) {
            throw new ResponseStatusException(401, "This device was signed out");
        }
        String grant = Grants.current();
        if (row.getLastSeenAt().plusMillis(60000L).isBefore(now) || !Requests.ip().equals(row.getLastIp())
                || !grant.equals(row.getGrantId())) {
            fill(row, now);
            row.setGrantId(grant);
            devices.save(row);
        }
    }

    /// Whether a device may play: known, not signed out.
    @Transactional(readOnly = true)
    public boolean usable(String username, String id) {
        if (id == null || id.length() == 0) {
            return true;
        }
        Device row = devices.findFirstByIdAndUsername(id, username);
        return row == null || row.getRevokedAt().equals(Instant.EPOCH);
    }

    /// Whether a download's device is still signed in. Stricter than
    /// [#usable]: a row that is gone was pushed out by newer sign-ins (or was
    /// a signed-out row they replaced), and must not bring a revoked device's
    /// downloads back to life.
    @Transactional(readOnly = true)
    public boolean signedIn(String username, String id) {
        if (id == null || id.length() == 0) {
            return false;
        }
        Device row = devices.findFirstByIdAndUsername(id, username);
        return row != null && row.getRevokedAt().equals(Instant.EPOCH);
    }

    @Transactional(readOnly = true)
    public List<DeviceDto> list(String username) {
        List<Device> rows = devices.findByUsernameOrderByLastSeenAtDesc(username);
        String current = Requests.deviceId();
        List<DeviceDto> out = new ArrayList<DeviceDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Device d = rows.get(iter);
            if (d.getRevokedAt().isAfter(Instant.EPOCH)) {
                continue;
            }
            out.add(new DeviceDto(d.getId(), d.getName(), d.getPlatform(), d.getLastIp(),
                    d.getLastCountry(), d.getLastSeenAt().toEpochMilli(), d.getId().equals(current)));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public int count(String username) {
        return (int) devices.countByUsernameAndRevokedAt(username, Instant.EPOCH);
    }

    @Transactional(rollbackFor = IOException.class)
    /// Revokes every device the account is signed in on.
    public void signOutAll(String username) throws IOException {
        List<Device> rows = devices.findByUsernameOrderByLastSeenAtDesc(username);
        Instant now = Instant.now();
        for (int iter = 0; iter < rows.size(); iter++) {
            Device row = rows.get(iter);
            if (row.getRevokedAt().equals(Instant.EPOCH)) {
                row.setRevokedAt(now);
                devices.save(row);
                grants.endGrant(row.getGrantId());
            }
        }
    }

    @Transactional(rollbackFor = IOException.class)
    public void signOut(String username, String id) throws IOException {
        Device row = id == null ? null : devices.findFirstByIdAndUsername(id, username);
        if (row == null) {
            throw new ResponseStatusException(404, "No such device");
        }
        row.setRevokedAt(Instant.now());
        devices.save(row);
        // The device id is the app's own claim; what really signs the device
        // out is ending the sign-in it last used, so its refresh token stops
        // working and its access token runs out within minutes.
        grants.endGrant(row.getGrantId());
    }

    private void makeRoom(String username) {
        List<Device> rows = devices.findByUsernameOrderByLastSeenAtDesc(username);
        for (int iter = MAX_DEVICES - 1; iter < rows.size(); iter++) {
            devices.deleteById(rows.get(iter).getId());
        }
    }

    private static void fill(Device row, Instant now) {
        row.setName(Requests.deviceName());
        row.setPlatform(Requests.platform());
        row.setLastIp(Requests.ip());
        row.setLastCountry(Requests.country());
        row.setLastSeenAt(now);
    }
}
