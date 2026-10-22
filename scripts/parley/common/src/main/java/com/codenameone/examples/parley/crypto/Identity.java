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
package com.codenameone.examples.parley.crypto;

import com.codename1.security.KeyGenerator;
import com.codename1.security.KeyPair;
import com.codename1.security.PrivateKey;
import com.codename1.security.PublicKey;
import com.codename1.security.Signature;
import com.codenameone.examples.parley.api.DeviceRegistrationDto;
import com.codenameone.examples.parley.api.PreKeyDto;
import com.codenameone.examples.parley.api.PreKeyUploadDto;
import com.codenameone.examples.parley.api.SignedPreKeyDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// This device's keys: the long-term identity pair, the signed prekey, and
/// the one-time prekeys it has published.
///
/// Every device of an account is its own identity -- the server lists them,
/// and a sender encrypts for each -- so nothing here is ever copied off the
/// device. The private halves live only in the [Vault].
public final class Identity {
    /// How many one-time prekeys a device publishes at a time, and the count
    /// below which it publishes more.
    public static final int BATCH = 50;
    public static final int LOW = 15;

    private static final String DOCUMENT = "identity";

    private static Map<String, Object> state;

    private Identity() {
    }

    /// Loads this device's keys, or makes them the first time.
    public static void load() {
        state = Vault.get(DOCUMENT);
        if (state == null || Codec.unb64(state.get("idPriv")) == null) {
            state = new HashMap<String, Object>();
            KeyPair identity = KeyGenerator.ec(256);
            state.put("idPriv", Codec.b64(identity.getPrivateKey().getEncoded()));
            state.put("idPub", Codec.b64(identity.getPublicKey().getEncoded()));
            state.put("nextPreKey", Long.valueOf(1));
            state.put("preKeys", new HashMap<String, Object>());
            rotateSignedPreKey();
        }
    }

    public static boolean loaded() {
        return state != null;
    }

    public static void forget() {
        state = null;
    }

    /// The server's id for this device, once it has registered.
    public static String deviceId() {
        return state == null ? "" : Codec.str(state.get("device"));
    }

    /// The identity a registration is about to be sent for, so that its
    /// answer can be matched to it in [#registered(String, String)].
    public static String owner() {
        return state == null ? "" : Codec.str(state.get("idPub"));
    }

    public static void registered(String deviceId) {
        registered(deviceId, owner());
    }

    /// Records the device id the server gave the identity `owner`. An answer
    /// for any other identity is dropped: the user signed out while it was in
    /// flight, and on a shared simulator or browser the next user's identity
    /// may already be loaded -- stamping the old device id on it would route
    /// that user's messages to a device that cannot read them.
    public static void registered(String deviceId, String owner) {
        if (state == null || !owner.equals(owner())) {
            return;
        }
        state.put("device", deviceId);
        save();
    }

    public static byte[] publicKey() {
        return Codec.unb64(state.get("idPub"));
    }

    public static PrivateKey privateKey() {
        return PrivateKey.fromPkcs8(PublicKey.EC, Codec.unb64(state.get("idPriv")));
    }

    static PrivateKey signedPreKey(long id) {
        if (Codec.num(state.get("spkId")) == id) {
            return PrivateKey.fromPkcs8(PublicKey.EC, Codec.unb64(state.get("spkPriv")));
        }
        // The one before stays usable for a while, for messages that were
        // already on their way when it was replaced.
        if (Codec.num(state.get("oldSpkId")) == id && Codec.unb64(state.get("oldSpkPriv")) != null) {
            return PrivateKey.fromPkcs8(PublicKey.EC, Codec.unb64(state.get("oldSpkPriv")));
        }
        return null;
    }

    static byte[] signedPreKeyPublic() {
        return Codec.unb64(state.get("spkPub"));
    }

    /// The private half of a one-time prekey, or null when it was already
    /// used: each one opens one session, ever. [#consumePreKey] spends it.
    @SuppressWarnings("unchecked")
    static PrivateKey preKey(long id) {
        Map<String, Object> keys = (Map<String, Object>) state.get("preKeys");
        Object priv = keys.get(String.valueOf(id));
        return priv == null ? null : PrivateKey.fromPkcs8(PublicKey.EC, Codec.unb64(priv));
    }

    /// Deletes a one-time prekey a session was opened with; -1 is none.
    @SuppressWarnings("unchecked")
    static void consumePreKey(long id) {
        Map<String, Object> keys = (Map<String, Object>) state.get("preKeys");
        if (id >= 0 && keys.remove(String.valueOf(id)) != null) {
            save();
        }
    }

    /// What the server needs to list this device: the public identity key,
    /// the signed prekey and a first batch of one-time prekeys.
    public static DeviceRegistrationDto registration(String name, String platform) {
        List<PreKeyDto> oneTime = newPreKeys(BATCH);
        save();
        return new DeviceRegistrationDto(name, platform, Codec.b64(publicKey()), signed(),
                oneTime);
    }

    /// A fresh batch of one-time prekeys and the current signed prekey.
    public static PreKeyUploadDto topUp() {
        List<PreKeyDto> oneTime = newPreKeys(BATCH);
        save();
        return new PreKeyUploadDto(signed(), oneTime);
    }

    /// Replaces the signed prekey; the old one stays decryptable.
    public static void rotateSignedPreKey() {
        if (state.get("spkPriv") != null) {
            state.put("oldSpkId", state.get("spkId"));
            state.put("oldSpkPriv", state.get("spkPriv"));
        }
        KeyPair pair = KeyGenerator.ec(256);
        long id = Codec.num(state.get("spkId")) + 1;
        byte[] pub = pair.getPublicKey().getEncoded();
        state.put("spkId", Long.valueOf(id));
        state.put("spkPriv", Codec.b64(pair.getPrivateKey().getEncoded()));
        state.put("spkPub", Codec.b64(pub));
        state.put("spkSig", Codec.b64(Signature.sign(Signature.SHA256_WITH_ECDSA, privateKey(),
                pub)));
        state.put("spkAt", Long.valueOf(System.currentTimeMillis()));
        state.put("spkPending", Boolean.TRUE);
        save();
    }

    /// When the signed prekey was made, so the app can replace it weekly.
    /// The current signed prekey alone, with no new one-time keys: what a
    /// rotation uploads. One-time keys are topped up by count, separately.
    public static PreKeyUploadDto signedOnly() {
        return new PreKeyUploadDto(signed(), new ArrayList<PreKeyDto>());
    }

    /// Whether a rotated signed prekey has yet to reach the server.
    public static boolean signedPreKeyPending() {
        return state != null && Boolean.TRUE.equals(state.get("spkPending"));
    }

    /// The server has the signed prekey [#signedOnly] carried.
    public static void signedPreKeyUploaded(int id) {
        if (state != null && Codec.num(state.get("spkId")) == id) {
            state.remove("spkPending");
            save();
        }
    }

    public static long signedPreKeyAge() {
        return System.currentTimeMillis() - Codec.num(state.get("spkAt"));
    }

    private static SignedPreKeyDto signed() {
        return new SignedPreKeyDto((int) Codec.num(state.get("spkId")),
                Codec.str(state.get("spkPub")), Codec.str(state.get("spkSig")));
    }

    @SuppressWarnings("unchecked")
    private static List<PreKeyDto> newPreKeys(int count) {
        Map<String, Object> keys = (Map<String, Object>) state.get("preKeys");
        long next = Codec.num(state.get("nextPreKey"));
        List<PreKeyDto> out = new ArrayList<PreKeyDto>();
        for (int iter = 0; iter < count; iter++) {
            KeyPair pair = KeyGenerator.ec(256);
            long id = next++;
            keys.put(String.valueOf(id), Codec.b64(pair.getPrivateKey().getEncoded()));
            out.add(new PreKeyDto((int) id, Codec.b64(pair.getPublicKey().getEncoded())));
        }
        state.put("nextPreKey", Long.valueOf(next));
        return out;
    }

    private static void save() {
        Vault.put(DOCUMENT, state);
    }
}
