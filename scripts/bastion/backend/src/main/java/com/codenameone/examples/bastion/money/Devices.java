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

import com.codenameone.examples.bastion.repository.ProfileRepository;
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
/// 2. **A second factor, used in that sign-in.** A token whose sign-in was
///    made with the password alone (`amr` holding none of `otp`, `mfa` or
///    `hwk`) is refused whatever it carries, even the password: a password is
///    the one thing a phished or stolen session is most likely to come with,
///    and the key registered here is what signs payments. The customer is
///    asked to set up an authenticator app or a passkey and sign in with it
///    -- even on an account that has an app now, since the app may have been
///    enrolled with the very token being used. A passkey sign-in the
///    authenticator did not verify the user for (`hwk` alone) must carry the
///    password as well; a wrong one counts toward the sign-in lockout. The token says how
///    its sign-in was made ([Caller#authMethods], the `amr` claim): a sign-in
///    with a user-verified passkey (`hwk` together with `user`) already proved
///    possession of a key bound to this site and the person holding it, so it
///    is not asked for a password it may not even have. `hwk` alone does not
///    count: a key that was merely present proves nothing about who used it.
///
/// Every registration and every refusal is audited, and the customer's
/// device list shows every key.
@Component
public class Devices {
    /// A customer with more keys than this is a customer whose token is being
    /// used to register them.
    static final int MAX_DEVICES = 10;

    private final DeviceRepository devices;
    private final ProfileRepository profiles;
    private final IntegrityVerifier integrity;
    private final Audit audit;
    private final TotpService totp;
    private final JdbcUserDetailsManager users;
    private final PasswordEncoder encoder;
    private final SignInGuard guard;
    private final long freshSeconds;

    public Devices(DeviceRepository devices, IntegrityVerifier integrity, Audit audit,
            TotpService totp, JdbcUserDetailsManager users, PasswordEncoder encoder,
            SignInGuard guard, ProfileRepository profiles,
            @Value("${bastion.device.fresh-minutes:10}") String freshMinutes) {
        this.devices = devices;
        this.profiles = profiles;
        this.integrity = integrity;
        this.audit = audit;
        this.totp = totp;
        this.users = users;
        this.encoder = encoder;
        this.guard = guard;
        this.freshSeconds = Long.parseLong(freshMinutes.trim()) * 60L;
    }

    /// Registers a key once the caller has shown a fresh sign-in, and the
    /// second factor used in that sign-in; see the class comment. The
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
        // The token's own record of how its sign-in was made decides, never
        // whether the account has an authenticator app *now*: a thief with a
        // fresh password-only token could enrol an app of their own and then
        // pass as "second factor". Only an `otp`/`mfa` sign-in, or a passkey
        // one the authenticator verified the user for (`hwk` with `user`), is
        // spared the password: `hwk` alone is a key that was merely present,
        // which proves possession of the device and nothing about who holds it.
        boolean second = how.contains("mfa") || how.contains("otp") || how.contains("hwk");
        if (!second) {
            audit.append(owner, "DEVICE_REFUSED", owner, "no second factor, amr " + how);
            throw Refused.with(403, "second-factor-add-device",
                    "Set up an authenticator app or a passkey, then sign in with it to add a device");
        }
        boolean strong = how.contains("mfa") || how.contains("otp")
                || (how.contains("hwk") && how.contains("user"));
        if (!strong) {
            guard.recheck(owner, request.password(), encoder, "DEVICE_REFUSED",
                    "enter-password-add-device-turn",
                    "Enter your password to add a device, or turn on an authenticator app");
        }
        return registerOnce(owner, request, how);
    }

    @Transactional(rollbackFor = Exception.class)
    private DeviceDto registerOnce(String owner, DeviceRegistrationDto request,
            java.util.List<String> how) throws IOException {
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
        // Behind the customer's profile row: two of these at once would each
        // count the rows before the other inserted and together pass the cap.
        profiles.hold(owner);
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
                + (device.isRooted() ? " rooted" : "") + (device.isAttested() ? " attested" : "")
                // How the sign-in that added it was made, for whoever reads
                // the trail after a takeover.
                + " amr " + how);
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

    /// The password again, for revoking a device that no device signs for.
    public void recheck(String owner, String password) throws IOException {
        guard.recheck(owner, password, encoder, "DEVICE_REVOKE_REFUSED",
                "sign-or-enter-password-revoke",
                "Approve this on another device, or enter your password");
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
