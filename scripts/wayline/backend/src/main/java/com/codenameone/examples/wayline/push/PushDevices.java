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
package com.codenameone.examples.wayline.push;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.push.PushTarget;
import com.codenameone.examples.wayline.Ids;
import com.codenameone.examples.wayline.Text;
import com.codenameone.examples.wayline.api.PushDeviceDto;
import com.codenameone.examples.wayline.domain.PushDevice;
import com.codenameone.examples.wayline.repository.PushDeviceRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// tag::wayline-push-devices[]
/// The devices each account is sent notifications on.
///
/// The app registers its token when somebody signs in and removes it when they
/// sign out. A token registered by a second account moves to it: the phone
/// belongs to whoever signed in on it last, and the one before is not to be
/// told about the new owner's rides.
@Component
public class PushDevices {
    /// How many devices one account keeps. A new one past this replaces the
    /// one heard from longest ago: a token the platform has replaced is never
    /// removed by its app, and would otherwise be sent to for good.
    static final int MAX_PER_ACCOUNT = 8;

    private final PushDeviceRepository devices;

    public PushDevices(PushDeviceRepository devices) {
        this.devices = devices;
    }

    @Transactional
    public PushDeviceDto register(String username, PushDeviceDto device) throws IOException {
        PushTarget target = target(device);
        String id = key(target);
        long now = System.currentTimeMillis();
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
    // end::wayline-push-devices[]

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
