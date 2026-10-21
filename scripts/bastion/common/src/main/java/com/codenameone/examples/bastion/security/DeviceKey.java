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
import com.codename1.security.SecureRandom;
import com.codename1.security.SecureStorage;
import com.codename1.security.vault.AssociatedData;
import com.codename1.security.vault.SecureEnvelope;
import com.codename1.ui.CN;
import com.codename1.util.Base64;
import com.codename1.util.SuccessCallback;
import com.codenameone.examples.bastion.api.DeviceDto;
import com.codenameone.examples.bastion.api.DeviceRegistrationDto;
import com.codenameone.examples.bastion.net.Api;
import com.codenameone.examples.bastion.net.Net;

/// This device's signing key: the second thing, after the session, that the
/// server asks for before money leaves an account.
///
/// The key is an EC P-256 pair from [KeyGenerator#ecP256()], made by the
/// platform's own crypto provider; the server gets its X.509 public half and
/// the PKCS#8 private half never leaves the device. It is kept sealed: an
/// AES-256-GCM [SecureEnvelope] whose associated data names the account and
/// purpose (so a blob copied to another account's slot does not open), under
/// a 256-bit wrapping key that lives in [SecureStorage] -- the iOS Keychain or
/// the Android Keystore-backed store. The sealed blob alone, in the app's
/// preferences, is useless off the device.
///
/// In the simulator `SecureStorage` is an ordinary file, and the Security
/// Center reports the key as not hardware protected there.
public final class DeviceKey {
    private static final String WRAP_ACCOUNT = "bastion.device.wrap";
    private static final String SEALED_KEY = "bastion.device.sealed";
    private static final String ID_KEY = "bastion.device.id";
    private static final String OWNER_KEY = "bastion.device.owner";

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
        byte[] wrap = wrapKey(false);
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
        Api.banking().registerDevice(request, Net.to(device -> {
            byte[] wrap = wrapKey(true);
            if (wrap == null) {
                failed.failed(0, "This device has no secure storage for the key.");
                return;
            }
            byte[] sealed = SecureEnvelope.seal(wrap, "device-key", 1, aad(owner), key.getEncoded());
            Preferences.set(SEALED_KEY, Base64.encodeUrlSafe(sealed));
            Preferences.set(ID_KEY, device.id());
            Preferences.set(OWNER_KEY, owner);
            cached = key;
            done.got(device);
        }, failed));
    }

    private static AssociatedData aad(String owner) {
        return AssociatedData.of("bastion", "device", owner, "request-signing");
    }

    /// The wrapping key from secure storage, made on first use when `create`.
    private static byte[] wrapKey(boolean create) {
        SecureStorage storage = SecureStorage.getInstance();
        String stored = storage.get(WRAP_ACCOUNT);
        if (stored == null && create) {
            // setIfAbsent: two starts racing make one key, not two.
            stored = storage.setIfAbsent(WRAP_ACCOUNT,
                    Base64.encodeUrlSafe(SecureRandom.bytes(32)));
        }
        if (stored == null) {
            return null;
        }
        byte[] key = Base64.decodeUrlSafe(stored);
        return key != null && key.length == 32 ? key : null;
    }
}
