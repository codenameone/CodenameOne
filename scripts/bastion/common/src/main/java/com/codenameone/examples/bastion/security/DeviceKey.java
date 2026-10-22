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
package com.codenameone.examples.bastion.security;

import com.codename1.io.Preferences;
import com.codename1.security.DeviceIntegrity;
import com.codename1.security.KeyGenerator;
import com.codename1.security.KeyPair;
import com.codename1.security.PrivateKey;
import com.codename1.security.PublicKey;
import com.codename1.security.vault.AssociatedData;
import com.codename1.security.vault.SecureEnvelope;
import com.codename1.ui.CN;
import com.codename1.util.Base64;
import com.codename1.util.SuccessCallback;
import com.codenameone.examples.bastion.api.DeviceDto;
import com.codenameone.examples.bastion.api.DeviceRevokeDto;
import com.codenameone.examples.bastion.api.DeviceRegistrationDto;
import com.codenameone.examples.bastion.net.Api;
import com.codenameone.examples.bastion.net.Net;
import com.codenameone.examples.bastion.net.Session;
import com.codenameone.examples.bastion.ui.Lang;

/// This device's signing key: the second thing, after the session, that the
/// server asks for before money leaves an account.
///
/// The key is an EC P-256 pair from [KeyGenerator#ecP256()], made by the
/// platform's own crypto provider; the server gets its X.509 public half and
/// the PKCS#8 private half never leaves the device. It is kept sealed: an
/// AES-256-GCM [SecureEnvelope] whose associated data names the account and
/// purpose (so a blob copied to another account's slot does not open), under
/// the app key ([Keys]), which the OS releases only after a biometric check
/// or which the user's PIN opens -- whichever they set up. The sealed blob
/// alone, in the app's preferences, is useless without one of those.
///
/// In the simulator the OS store is an ordinary file, and the Security
/// Center says so.
public final class DeviceKey {
    private static final String SEALED_KEY = "bastion.device.sealed";
    private static final String ID_KEY = "bastion.device.id";
    private static final String OWNER_KEY = "bastion.device.owner";
    /// A device the server registered whose key this device could not keep:
    /// the app locked (or the session changed) while the registration was
    /// on its way. It is revoked at the next chance ([#revokeOrphan]).
    private static final String ORPHAN_KEY = "bastion.device.orphan";

    private static PrivateKey cached;

    private DeviceKey() {
    }

    /// The id the server gave this device, or null before registration.
    public static String deviceId() {
        String id = Preferences.get(ID_KEY, "");
        return id.length() == 0 ? null : id;
    }

    /// The account the key was registered for, or null.
    public static String owner() {
        String owner = Preferences.get(OWNER_KEY, "");
        return owner.length() == 0 ? null : owner;
    }

    /// Whether the key belongs to `username`: false for a key registered for
    /// another account, or when nobody is signed in.
    public static boolean ownedBy(String username) {
        String owner = owner();
        return owner != null && owner.equals(username);
    }

    /// The private key, unsealed, or null when there is none (or the wrapping
    /// key is gone: a restored backup on a new phone, which is then simply a
    /// device to register again).
    public static PrivateKey privateKey() {
        if (Lock.isLocked()) {
            return null;
        }
        if (cached != null) {
            return cached;
        }
        String sealed = Preferences.get(SEALED_KEY, "");
        String owner = owner();
        byte[] wrap = Keys.current();
        if (sealed.length() == 0 || owner == null || wrap == null) {
            return null;
        }
        try {
            cached = PrivateKey.fromPkcs8(PublicKey.EC,
                    SecureEnvelope.parse(Base64.decodeUrlSafe(sealed)).open(wrap, aad(owner)));
            return cached;
        } catch (RuntimeException tamperedOrForeign) {
            // A blob that does not open under this device's key, or for this
            // account, is not this device's key.
            forget();
            return null;
        }
    }

    /// Drops the unsealed key from memory: the app lock calls this.
    public static void dropFromMemory() {
        cached = null;
    }

    /// Forgets the key on this device; the server keeps its record until the
    /// device is removed there (Security Center, or another device).
    public static void forget() {
        cached = null;
        Preferences.delete(SEALED_KEY);
        Preferences.delete(ID_KEY);
        Preferences.delete(OWNER_KEY);
    }

    /// Makes a new key and registers its public half for `owner`. `password`
    /// is asked for when the account has no authenticator app (the server
    /// wants a second proof that it is the owner adding a device); null
    /// otherwise. The sign-in must be a few minutes old.
    public static void register(final String owner, final String password,
            final Net.Ok<DeviceDto> done, final Net.Failed failed) {
        KeyPair pair = KeyGenerator.ecP256();
        final PrivateKey key = pair.getPrivateKey();
        final String publicKey = Base64.encodeUrlSafe(pair.getPublicKey().getEncoded());
        final boolean rooted = DeviceIntegrity.isDeviceCompromised();
        if (DeviceIntegrity.isAttestationSupported()) {
            // Play Integrity / App Attest over the public key, so the token is
            // bound to this key and cannot be replayed for another.
            DeviceIntegrity.requestIntegrityToken(publicKey).ready(new SuccessCallback<String>() {
                @Override
                public void onSucess(String token) {
                    send(owner, password, key, publicKey, rooted, token, done, failed);
                }
            }).except(new SuccessCallback<Throwable>() {
                @Override
                public void onSucess(Throwable err) {
                    send(owner, password, key, publicKey, rooted, null, done, failed);
                }
            });
            return;
        }
        send(owner, password, key, publicKey, rooted, null, done, failed);
    }

    private static void send(final String owner, String password, final PrivateKey key,
            String publicKey, boolean rooted, String token, final Net.Ok<DeviceDto> done,
            Net.Failed failed) {
        DeviceRegistrationDto request = new DeviceRegistrationDto(publicKey,
                CN.getPlatformName(), rooted, token != null, token, password);
        final int epoch = Session.epoch();
        // Not Net.to: that drops an answer from an earlier epoch, and this one
        // must not be dropped -- the server has a device on record either way.
        Api.banking().registerDevice(request, response -> {
            DeviceDto device = response != null && response.getResponseCode() == 200
                    ? response.getResponseData() : null;
            if (device == null || epoch == Session.epoch()) {
                Net.to((DeviceDto registered) -> keep(owner, key, registered, done, failed),
                        failed).completed(response);
                return;
            }
            // Locked (or signed out) mid-registration: the key that goes with
            // the server's record is gone from memory, and an untrusted
            // stranger could not sign with it anyway. The record is revoked
            // once there is a session again, so no device the user never saw
            // finish stays on their account.
            Preferences.set(ORPHAN_KEY, device.id());
        });
    }

    /// Keeps the key the server just registered as `device`: sealed under
    /// the app key, for `owner`, who must still be the one signed in.
    static void keep(String owner, PrivateKey key, DeviceDto device,
            Net.Ok<DeviceDto> done, Net.Failed failed) {
        byte[] wrap = Keys.current();
        if (wrap == null || !owner.equals(Session.username())) {
            Preferences.set(ORPHAN_KEY, device.id());
            revokeOrphan();
            failed.failed(0, Lang.tr("Unlock Bastion first."));
            return;
        }
        byte[] sealed = SecureEnvelope.seal(wrap, "device-key", 1, aad(owner), key.getEncoded());
        Preferences.set(SEALED_KEY, Base64.encodeUrlSafe(sealed));
        Preferences.set(ID_KEY, device.id());
        Preferences.set(OWNER_KEY, owner);
        cached = key;
        done.got(device);
    }

    /// The device left registered on the server by an interrupted
    /// registration, or null.
    public static String orphan() {
        String id = Preferences.get(ORPHAN_KEY, "");
        return id.length() == 0 ? null : id;
    }

    /// Revokes [#orphan] if there is one and a session to do it with; kept
    /// for the next try when the server cannot be reached. Called after every
    /// sign-in and unlock.
    public static void revokeOrphan() {
        final String id = orphan();
        if (id == null || Session.user() == null) {
            return;
        }
        // Revoking is signed by this device's own key: the orphan's key was
        // never kept, so there is nothing else to sign with. Until this device
        // has a key of its own the orphan waits -- it is harmless meanwhile,
        // since nobody holds its private key.
        DeviceRevokeDto request = new DeviceRevokeDto(null);
        String signature = RequestSigner.sign("POST", "/api/devices/" + id + "/revoke", null,
                request);
        if (signature == null) {
            return;
        }
        Api.banking().revokeDevice(id, signature, request, Net.to((DeviceDto gone) -> {
            if (id.equals(orphan())) {
                Preferences.delete(ORPHAN_KEY);
            }
        }, (status, message) -> {
            // 404: already gone (or never this account's). Anything else is
            // tried again next time.
            if (status == 404 && id.equals(orphan())) {
                Preferences.delete(ORPHAN_KEY);
            }
        }));
    }

    private static AssociatedData aad(String owner) {
        return AssociatedData.of("bastion", "device", owner, "request-signing");
    }
}
