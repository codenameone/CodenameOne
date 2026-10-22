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

import com.codenameone.examples.bastion.Refused;
import com.codename1.backend.Base64Url;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.annotations.Value;
import com.codename1.backend.security.core.userdetails.JdbcUserDetailsManager;
import com.codename1.backend.security.crypto.Der;
import com.codename1.backend.security.crypto.PasswordEncoder;
import com.codename1.backend.security.mfa.TotpService;
import com.codenameone.examples.bastion.account.SignInGuard;
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
/// **Registering a key needs more than a token.** A device key is what
/// signs money out of the bank, so a stolen access token -- or a refresh
/// token kept alive for hours -- must not be enough to add one. Two checks:
///
/// 1. **A fresh sign-in.** The token's `auth_time` claim ([Caller#authTime])
///    must be under `bastion.device.fresh-minutes` (10) old. A refresh keeps
///    the original sign-in time, so only someone who signed in just now
///    passes.
/// 2. **The second factor, or the password again.** An account with an
///    authenticator app cannot finish a sign-in without the code, so its
///    fresh sign-in already *was* a second-factor sign-in. An account without
///    one must type its password into the registration as well; a wrong one
///    counts toward the sign-in lockout like any other. The token says how
///    its sign-in was made ([Caller#authMethods], the `amr` claim): a sign-in
///    with a passkey (`hwk`) already proved possession of a key bound to this
///    site, so it is not asked for a password it may not even have.
///
/// Every registration and every refusal is audited, and the customer's
/// device list shows every key.
@Component
public class Devices {
    /// A customer with more keys than this is a customer whose token is being
    /// used to register them.
    static final int MAX_DEVICES = 10;

    private final DeviceRepository devices;
    private final IntegrityVerifier integrity;
    private final Audit audit;
    private final TotpService totp;
    private final JdbcUserDetailsManager users;
    private final PasswordEncoder encoder;
    private final SignInGuard guard;
    private final long freshSeconds;

    public Devices(DeviceRepository devices, IntegrityVerifier integrity, Audit audit,
            TotpService totp, JdbcUserDetailsManager users, PasswordEncoder encoder,
            SignInGuard guard, @Value("${bastion.device.fresh-minutes:10}") String freshMinutes) {
        this.devices = devices;
        this.integrity = integrity;
        this.audit = audit;
        this.totp = totp;
        this.users = users;
        this.encoder = encoder;
        this.guard = guard;
        this.freshSeconds = Long.parseLong(freshMinutes.trim()) * 60L;
    }

    /// Registers a key once the caller has shown a fresh sign-in, and the
    /// password when there is no second factor; see the class comment. The
    /// refusals are audited here, outside the registration's transaction.
    public DeviceDto register(String owner, DeviceRegistrationDto request) throws IOException {
        if (request == null || request.publicKey() == null) {
            throw Refused.with(400, "send-device-public-key", "Send the device's public key");
        }
        long age = System.currentTimeMillis() / 1000L - Caller.authTime();
        if (Caller.authTime() < 0 || age > freshSeconds || age < -60L) {
            audit.append(owner, "DEVICE_REFUSED", owner, "sign-in not fresh");
            throw Refused.with(403, "sign-again-add-device-sign", "Sign in again to add a device: the sign-in must be a few minutes old");
        }
        java.util.List<String> how = Caller.authMethods();
        boolean strong = how.contains("hwk") || how.contains("mfa");
        if (!strong && !totp.isEnabled(owner)) {
            String password = request.password();
            if (password == null
                    || !encoder.matches(password, users.loadUserByUsername(owner).getPassword())) {
                guard.failed(owner);
                audit.append(owner, "DEVICE_REFUSED", owner, "password");
                throw Refused.with(403, "enter-password-add-device-turn", "Enter your password to add a device, or turn on an authenticator app");
            }
        }
        return registerOnce(owner, request);
    }

    @Transactional(rollbackFor = Exception.class)
    private DeviceDto registerOnce(String owner, DeviceRegistrationDto request)
            throws IOException {
        byte[] key = decode(request.publicKey());
        // A P-256 key and nothing else: that is the curve ES256 is defined
        // over, and the one every phone's secure hardware can hold.
        try {
            if (key == null || !Der.EC.equals(Der.publicKeyType(key))
                    || !Der.P256.equals(Der.ecCurve(key))) {
                throw Refused.with(400, "key-ec-p-256-public", "The key must be an EC P-256 public key");
            }
        } catch (IOException unreadable) {
            throw Refused.with(400, "key-ec-p-256-public", "The key must be an EC P-256 public key");
        }
        List<Device> held = devices.findByOwnerOrderByCreatedAtAsc(owner);
        int live = 0;
        for (Device d : held) {
            live += d.isRevoked() ? 0 : 1;
        }
        if (live >= MAX_DEVICES) {
            throw Refused.with(409, "remove-device-before-adding-another", "Remove a device before adding another");
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
            throw Refused.with(404, "no-such-device", "No such device");
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
            throw Refused.with(403, "customers-hold-accounts", "Only customers hold accounts");
        }
        return name;
    }
}
