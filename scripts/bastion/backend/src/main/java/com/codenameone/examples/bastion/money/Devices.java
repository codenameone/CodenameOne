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
package com.codenameone.examples.bastion.money;

import com.codename1.backend.Base64Url;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.security.crypto.Der;
import com.codenameone.examples.bastion.Caller;
import com.codenameone.examples.bastion.Ids;
import com.codenameone.examples.bastion.api.DeviceDto;
import com.codenameone.examples.bastion.api.DeviceRegistrationDto;
import com.codenameone.examples.bastion.audit.Audit;
import com.codenameone.examples.bastion.domain.Device;
import com.codenameone.examples.bastion.repository.DeviceRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// The devices that may sign requests for a customer, and what each one is
/// trusted with.
///
/// A device is **trusted** when the platform attested it and it is not
/// rooted. With `bastion.device.policy.enforce` on (the default), only a
/// trusted device can send money to someone else, add a payee or withdraw:
/// an untrusted one is held to moving money between the customer's own
/// accounts, which is the most malware on a rooted phone could then do.
///
/// Registering needs only the access token, which is the weak point a real
/// bank closes by binding registration to a fresh sign-in with the second
/// factor, or to a code sent to the phone number on file. It is audited, and
/// the customer's device list shows every key.
@Component
public class Devices {
    /// A customer with more keys than this is a customer whose token is being
    /// used to register them.
    static final int MAX_DEVICES = 10;

    private final DeviceRepository devices;
    private final IntegrityVerifier integrity;
    private final Audit audit;

    public Devices(DeviceRepository devices, IntegrityVerifier integrity, Audit audit) {
        this.devices = devices;
        this.integrity = integrity;
        this.audit = audit;
    }

    @Transactional(rollbackFor = Exception.class)
    public DeviceDto register(String owner, DeviceRegistrationDto request) throws IOException {
        if (request == null || request.publicKey() == null) {
            throw new ResponseStatusException(400, "Send the device's public key");
        }
        byte[] key = decode(request.publicKey());
        // A P-256 key and nothing else: that is the curve ES256 is defined
        // over, and the one every phone's secure hardware can hold.
        try {
            if (key == null || !Der.EC.equals(Der.publicKeyType(key))
                    || !Der.P256.equals(Der.ecCurve(key))) {
                throw new ResponseStatusException(400, "The key must be an EC P-256 public key");
            }
        } catch (IOException unreadable) {
            throw new ResponseStatusException(400, "The key must be an EC P-256 public key");
        }
        List<Device> held = devices.findByOwnerOrderByCreatedAtAsc(owner);
        int live = 0;
        for (Device d : held) {
            live += d.isRevoked() ? 0 : 1;
        }
        if (live >= MAX_DEVICES) {
            throw new ResponseStatusException(409, "Remove a device before adding another");
        }
        String platform = clip(request.platform(), 20);
        IntegrityVerifier.Verdict verdict = integrity.verify(platform, request.integrityToken(),
                request.rooted(), request.attested());
        Device device = new Device();
        device.setId(Ids.next());
        device.setOwner(owner);
        device.setPublicKey(request.publicKey());
        device.setPlatform(platform);
        device.setRooted(verdict.rooted());
        device.setAttested(verdict.attested());
        device.setCreatedAt(System.currentTimeMillis());
        devices.insert(device);
        audit.append(owner, "DEVICE_REGISTERED", device.getId(), platform
                + (trusted(device) ? " trusted" : " untrusted")
                + (device.isRooted() ? " rooted" : "") + (device.isAttested() ? " attested" : ""));
        return dto(device);
    }

    @Transactional(readOnly = true)
    public List<DeviceDto> list(String owner) throws IOException {
        List<DeviceDto> out = new ArrayList<DeviceDto>();
        for (Device d : devices.findByOwnerOrderByCreatedAtAsc(owner)) {
            if (!d.isRevoked()) {
                out.add(dto(d));
            }
        }
        return out;
    }

    @Transactional(rollbackFor = Exception.class)
    public DeviceDto revoke(String owner, String id) throws IOException {
        Device device = id == null ? null : devices.findById(id);
        if (device == null || !device.getOwner().equals(owner)
                || devices.revoke(id, owner) != 1) {
            throw new ResponseStatusException(404, "No such device");
        }
        device.setRevoked(true);
        audit.append(owner, "DEVICE_REVOKED", id, device.getPlatform());
        return dto(device);
    }

    /// `owner`'s live device `id`, or null.
    @Transactional(readOnly = true)
    public Device live(String owner, String id) throws IOException {
        Device device = id == null ? null : devices.findById(id);
        if (device == null || device.isRevoked() || !device.getOwner().equals(owner)) {
            return null;
        }
        return device;
    }

    public static boolean trusted(Device device) {
        return device != null && device.isAttested() && !device.isRooted()
                && !device.isRevoked();
    }

    static byte[] decode(String base64url) {
        try {
            return Base64Url.decode(base64url);
        } catch (RuntimeException malformed) {
            return null;
        }
    }

    private static DeviceDto dto(Device d) {
        return new DeviceDto(d.getId(), d.getPlatform(), d.isRooted(), d.isAttested(), trusted(d),
                d.getCreatedAt());
    }

    private static String clip(String value, int max) {
        String v = value == null ? "" : value;
        return v.length() <= max ? v : v.substring(0, max);
    }

    /// The caller, who must be a customer: staff do not bank on their admin
    /// account, and an admin token is not a way into anyone's money.
    public static String customer() {
        String name = Caller.name();
        if (!Caller.hasRole(com.codenameone.examples.bastion.Roles.CUSTOMER)) {
            throw new ResponseStatusException(403, "Only customers hold accounts");
        }
        return name;
    }
}
