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
package com.codenameone.examples.brickyard.push;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.push.PushTarget;
import com.codenameone.examples.brickyard.Clock;
import com.codenameone.examples.brickyard.Ids;
import com.codenameone.examples.brickyard.api.PushDeviceDto;
import com.codenameone.examples.brickyard.domain.PushDevice;
import com.codenameone.examples.brickyard.repository.PushDeviceRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// The devices each account is sent notifications on. A token registered by
/// a second account moves to it: the phone belongs to whoever signed in last.
@Component
public class PushDevices {
    /// Devices one account keeps; a new one past this replaces the oldest.
    static final int MAX_PER_ACCOUNT = 8;

    private final PushDeviceRepository devices;

    public PushDevices(PushDeviceRepository devices) {
        this.devices = devices;
    }

    @Transactional(rollbackFor = IOException.class)
    public PushDeviceDto register(String username, PushDeviceDto device) throws IOException {
        PushTarget target = target(device);
        String id = Ids.sha256(target.getProvider() + ":" + target.getToken());
        PushDevice row = devices.findById(id);
        if (row == null || !username.equals(row.getUsername())) {
            makeRoom(username);
        }
        boolean fresh = row == null;
        if (fresh) {
            row = new PushDevice();
            row.setId(id);
            row.setProvider(target.getProvider());
            row.setToken(target.getToken());
        }
        row.setUsername(username);
        String platform = device.platform() == null ? "" : device.platform().trim();
        row.setPlatform(platform.length() > 32 ? platform.substring(0, 32) : platform);
        row.setUpdatedAt(Clock.now());
        if (fresh) {
            devices.insert(row);
        } else {
            devices.save(row);
        }
        return new PushDeviceDto(target.getProvider(), target.getToken(), row.getPlatform());
    }

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

    /// Every device `username` registered.
    public List<PushTarget> targets(String username) {
        List<PushTarget> out = new ArrayList<PushTarget>();
        for (PushDevice each : devices.findByUsername(username)) {
            out.add(PushTarget.of(each.getProvider(), each.getToken()));
        }
        return out;
    }

    private static PushTarget target(PushDeviceDto device) {
        if (device == null || device.token() == null || device.token().trim().length() == 0
                || device.token().length() > 1024 || device.provider() == null) {
            throw new ResponseStatusException(400, "Tell us the device's provider and token");
        }
        try {
            return PushTarget.of(device.provider().trim(), device.token().trim());
        } catch (IllegalArgumentException err) {
            throw new ResponseStatusException(400, err.getMessage());
        }
    }
}
