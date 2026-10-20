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

import com.codename1.backend.Base64;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.security.crypto.Der;
import com.codenameone.examples.parley.Ids;
import com.codenameone.examples.parley.Text;
import com.codenameone.examples.parley.api.DeviceDto;
import com.codenameone.examples.parley.api.DeviceRegistrationDto;
import com.codenameone.examples.parley.api.KeyCountDto;
import com.codenameone.examples.parley.api.PreKeyBundleDto;
import com.codenameone.examples.parley.api.PreKeyDto;
import com.codenameone.examples.parley.api.PreKeyUploadDto;
import com.codenameone.examples.parley.api.SignedPreKeyDto;
import com.codenameone.examples.parley.domain.Device;
import com.codenameone.examples.parley.domain.OneTimePreKey;
import com.codenameone.examples.parley.repository.DeviceRepository;
import com.codenameone.examples.parley.repository.EnvelopeRepository;
import com.codenameone.examples.parley.repository.OneTimePreKeyRepository;
import com.codenameone.examples.parley.repository.ProfileRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// The devices of each account and the public keys they publish.
///
/// Every device is its own end of every encrypted session, so a message to an
/// account is encrypted once for each of its devices, and once for each of the
/// sender's other devices too. The server keeps the public halves: an identity
/// key, a signed prekey that the identity key vouches for, and a supply of
/// one-time prekeys. A sender fetches one bundle per device and can open a
/// session with a device that is switched off.
///
/// The signature over the signed prekey is checked here as well as by every
/// sender. That does not make the server trusted -- a server that wanted to
/// lie would swap the identity key too, which is what safety numbers catch --
/// but it stops a broken client from publishing keys nobody can use.
@Component
public class Devices {
    /// How many devices one account may link: phones, tablets, browsers.
    public static final int MAX_DEVICES = 6;

    /// How many one-time prekeys a device may hold on the server at once.
    public static final int MAX_PREKEYS = 200;

    private static final String[] PLATFORMS = {"ios", "android", "web", "desktop", "simulator"};

    private final DeviceRepository devices;
    private final OneTimePreKeyRepository preKeys;
    private final EnvelopeRepository envelopes;
    private final ProfileRepository profiles;

    public Devices(DeviceRepository devices, OneTimePreKeyRepository preKeys,
            EnvelopeRepository envelopes, ProfileRepository profiles) {
        this.devices = devices;
        this.preKeys = preKeys;
        this.envelopes = envelopes;
        this.profiles = profiles;
    }

    /// Links a new device. Its id is chosen here, so two devices can never
    /// claim the same one.
    @Transactional(rollbackFor = IOException.class)
    public DeviceDto register(String username, DeviceRegistrationDto request) throws IOException {
        if (request == null) {
            throw new ResponseStatusException(400, "Tell us about the device");
        }
        if (profiles.hold(username) == 0) {
            throw new ResponseStatusException(404, "No such account");
        }
        if (devices.countByUsername(username) >= MAX_DEVICES) {
            throw new ResponseStatusException(409,
                    "That account has " + MAX_DEVICES + " devices. Unlink one first");
        }
        String identity = key(request.identityKey(), "the identity key");
        SignedPreKeyDto signed = signed(identity, request.signedPreKey());
        long now = System.currentTimeMillis();
        Device row = new Device();
        row.setId(Ids.next());
        row.setUsername(username);
        row.setName(Text.required(request.name(), 120, "the device's name"));
        row.setPlatform(Text.oneOf(request.platform(), PLATFORMS, "simulator", "the platform"));
        row.setIdentityKey(identity);
        row.setSignedPreKeyId(signed.id());
        row.setSignedPreKey(signed.publicKey());
        row.setSignedPreKeySignature(signed.signature());
        row.setCreatedAt(now);
        row.setLastSeenAt(now);
        devices.insert(row);
        store(row.getId(), request.oneTimePreKeys());
        return toDto(row);
    }

    @Transactional(readOnly = true)
    public List<DeviceDto> list(String username) {
        List<Device> rows = devices.findByUsername(username);
        List<DeviceDto> out = new ArrayList<DeviceDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(toDto(rows.get(iter)));
        }
        return out;
    }

    /// Unlinks a device: its keys go, and so do the envelopes waiting for it,
    /// which nobody else can read.
    @Transactional(rollbackFor = IOException.class)
    public DeviceDto unlink(String username, String id) throws IOException {
        Device row = owned(username, id);
        DeviceDto was = toDto(row);
        preKeys.deleteByDeviceId(id);
        envelopes.deleteByRecipientDevice(id);
        devices.deleteById(id);
        return was;
    }

    /// Adds one-time prekeys and, optionally, rotates the signed prekey.
    @Transactional(rollbackFor = IOException.class)
    public KeyCountDto upload(String username, String id, PreKeyUploadDto keys)
            throws IOException {
        Device row = owned(username, id);
        if (keys == null) {
            throw new ResponseStatusException(400, "No keys");
        }
        if (keys.signedPreKey() != null) {
            SignedPreKeyDto signed = signed(row.getIdentityKey(), keys.signedPreKey());
            row.setSignedPreKeyId(signed.id());
            row.setSignedPreKey(signed.publicKey());
            row.setSignedPreKeySignature(signed.signature());
        }
        store(id, keys.oneTimePreKeys());
        return new KeyCountDto((int) preKeys.countByDeviceId(id));
    }

    @Transactional(readOnly = true)
    public KeyCountDto count(String username, String id) {
        owned(username, id);
        return new KeyCountDto((int) preKeys.countByDeviceId(id));
    }

    /// One bundle for every device of an account.
    @Transactional(rollbackFor = IOException.class)
    public List<PreKeyBundleDto> bundles(String username) throws IOException {
        List<Device> rows = devices.findByUsername(username);
        if (rows.isEmpty() && profiles.findById(username) == null) {
            throw new ResponseStatusException(404, "No such account");
        }
        List<PreKeyBundleDto> out = new ArrayList<PreKeyBundleDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(bundle(rows.get(iter)));
        }
        return out;
    }

    /// The bundle of one device. Hands out, and so deletes, one of its
    /// one-time prekeys.
    @Transactional(rollbackFor = IOException.class)
    public PreKeyBundleDto bundle(String username, String device) throws IOException {
        Device row = devices.findByIdAndUsername(device, username);
        if (row == null) {
            throw new ResponseStatusException(404, "No such device");
        }
        return bundle(row);
    }

    /// Whether the device is one of the account's own.
    @Transactional(readOnly = true)
    public boolean owns(String username, String device) {
        return device != null && devices.findByIdAndUsername(device, username) != null;
    }

    /// Notes that a device was online now, and so the account: "last seen".
    @Transactional
    public void seen(String username, String device) {
        long now = System.currentTimeMillis();
        devices.seen(device, now);
        profiles.seen(username, now);
    }

    /// The device, or a 404 when it is not the caller's: a device of somebody
    /// else's is not found, not forbidden, so ids cannot be probed.
    public Device owned(String username, String id) {
        Device row = id == null ? null : devices.findByIdAndUsername(id, username);
        if (row == null) {
            throw new ResponseStatusException(404, "No such device");
        }
        return row;
    }

    private PreKeyBundleDto bundle(Device row) throws IOException {
        // Two senders asking at once can both read the same oldest key; the
        // delete is what claims it, and only the one whose delete removed the
        // row may hand it out. The other tries the next key.
        PreKeyDto oneTime = null;
        for (int attempt = 0; attempt < 8 && oneTime == null; attempt++) {
            OneTimePreKey one = preKeys.findFirstByDeviceIdOrderByKeyIdAsc(row.getId());
            if (one == null) {
                break;
            }
            if (preKeys.claim(one.getId()) == 1) {
                oneTime = new PreKeyDto(one.getKeyId(), one.getPublicKey());
            }
        }
        return new PreKeyBundleDto(row.getUsername(), row.getId(), row.getIdentityKey(),
                new SignedPreKeyDto(row.getSignedPreKeyId(), row.getSignedPreKey(),
                        row.getSignedPreKeySignature()), oneTime);
    }

    private void store(String device, List<PreKeyDto> keys) throws IOException {
        if (keys == null) {
            return;
        }
        long held = preKeys.countByDeviceId(device);
        if (held + keys.size() > MAX_PREKEYS) {
            throw new ResponseStatusException(400,
                    "A device holds at most " + MAX_PREKEYS + " one-time keys");
        }
        for (int iter = 0; iter < keys.size(); iter++) {
            PreKeyDto key = keys.get(iter);
            if (key == null) {
                throw new ResponseStatusException(400, "An empty key");
            }
            String id = device + ":" + key.id();
            if (preKeys.existsById(id)) {
                throw new ResponseStatusException(409, "Key " + key.id() + " was uploaded already");
            }
            OneTimePreKey row = new OneTimePreKey();
            row.setId(id);
            row.setDeviceId(device);
            row.setKeyId(key.id());
            row.setPublicKey(key(key.publicKey(), "a one-time key"));
            preKeys.insert(row);
        }
    }

    private static SignedPreKeyDto signed(String identity, SignedPreKeyDto signed)
            throws IOException {
        if (signed == null) {
            throw new ResponseStatusException(400, "A device needs a signed prekey");
        }
        String key = key(signed.publicKey(), "the signed prekey");
        byte[] signature = signed.signature() == null ? null : Base64.decode(signed.signature());
        if (signature == null || signature.length > 80
                || !com.codename1.backend.Crypto.verify("ES256", Base64.decode(identity),
                        Base64.decode(key), signature)) {
            throw new ResponseStatusException(400,
                    "The signed prekey is not signed by the identity key");
        }
        return new SignedPreKeyDto(signed.id(), key, signed.signature());
    }

    /// A public key as it is stored: base64 of a P-256 SubjectPublicKeyInfo.
    static String key(String value, String what) {
        byte[] der = value == null ? null : Base64.decode(value);
        if (der == null || der.length > 120) {
            throw new ResponseStatusException(400, "That is not a key: " + what);
        }
        try {
            if (!Der.P256.equals(Der.ecCurve(der))) {
                throw new ResponseStatusException(400, "Keys are P-256: " + what);
            }
        } catch (IOException notAKey) {
            throw new ResponseStatusException(400, "That is not a key: " + what);
        }
        return Base64.encode(der);
    }

    private static DeviceDto toDto(Device row) {
        return new DeviceDto(row.getId(), row.getName(), row.getPlatform(), row.getCreatedAt(),
                row.getLastSeenAt());
    }
}
