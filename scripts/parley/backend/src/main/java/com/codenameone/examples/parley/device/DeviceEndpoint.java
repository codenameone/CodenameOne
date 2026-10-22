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
package com.codenameone.examples.parley.device;

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.parley.Caller;
import com.codenameone.examples.parley.api.DeviceApiServer;
import com.codenameone.examples.parley.api.DeviceDto;
import com.codenameone.examples.parley.api.DeviceRegistrationDto;
import com.codenameone.examples.parley.api.KeyCountDto;
import com.codenameone.examples.parley.api.PreKeyBundleDto;
import com.codenameone.examples.parley.api.PreKeyUploadDto;
import com.codenameone.examples.parley.live.LiveHub;

import java.util.List;

/// The server's half of `DeviceApi`.
@Component
public class DeviceEndpoint implements DeviceApiServer {
    private final Devices devices;
    private final LiveHub live;

    public DeviceEndpoint(Devices devices, LiveHub live) {
        this.devices = devices;
        this.live = live;
    }

    @Override
    public DeviceDto register(DeviceRegistrationDto device) throws Exception {
        return devices.register(Caller.name(), device);
    }

    @Override
    public List<DeviceDto> list() throws Exception {
        return devices.list(Caller.name());
    }

    @Override
    public DeviceDto unlink(String id) throws Exception {
        DeviceDto was = devices.unlink(Caller.name(), id);
        // After the commit: a connection closed for a change that was then
        // rolled back would reconnect to a device that still exists.
        live.dropDevice(id);
        return was;
    }

    @Override
    public KeyCountDto uploadPreKeys(String id, PreKeyUploadDto keys) throws Exception {
        return devices.upload(Caller.name(), id, keys);
    }

    @Override
    public KeyCountDto preKeyCount(String id) throws Exception {
        return devices.count(Caller.name(), id);
    }

    @Override
    public List<PreKeyBundleDto> bundles(String username) throws Exception {
        return devices.bundles(username);
    }

    @Override
    public PreKeyBundleDto bundle(String username, String device) throws Exception {
        return devices.bundle(username, device);
    }
}
