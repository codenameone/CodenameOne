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

import com.codename1.security.CryptoException;
import com.codename1.security.Hash;
import com.codenameone.examples.parley.api.PreKeyBundleDto;

import java.util.HashMap;
import java.util.Map;

/// The [Ratchet] sessions this device holds, one per device it talks to,
/// plus the one before it.
///
/// Two devices that start a session with each other at the same moment each
/// end up with two; the one kept as "previous" opens whatever the other side
/// sent on the session it started, and whichever answers first wins. The same
/// happens when a contact reinstalls: their new device's first message starts
/// a new session, and messages still in flight on the old one still open.
public final class Sessions {
    private static final Map<String, Ratchet> cache = new HashMap<String, Ratchet>();

    private Sessions() {
    }

    public static void clear() {
        cache.clear();
    }

    public static boolean has(String username, String device) {
        return current(username, device) != null;
    }

    /// Starts a session from a prekey bundle, replacing any there was.
    public static void start(PreKeyBundleDto bundle) {
        Ratchet session = Ratchet.initiate(bundle);
        keep(bundle.username(), bundle.deviceId(), session, current(bundle.username(),
                bundle.deviceId()));
    }

    /// Encrypts for one device; answers header and ciphertext.
    public static String[] encrypt(String username, String device, byte[] plaintext) {
        Ratchet session = current(username, device);
        if (session == null) {
            throw new CryptoException("No session with " + username + "/" + device);
        }
        String[] out = session.encrypt(plaintext);
        Vault.put(name("s", username, device), session.toMap());
        return out;
    }

    /// Decrypts a message from one device, trying the current session, the
    /// previous one, and a new one when the header starts one.
    ///
    /// #### Throws
    ///
    /// - `CryptoException`: when none of them opens it
    public static byte[] decrypt(String username, String device, String headerText,
            byte[] ciphertext) {
        Map<String, Object> header = Codec.parse(headerText);
        if (header == null) {
            throw new CryptoException("That message has no header");
        }
        Ratchet current = current(username, device);
        Ratchet previous = load(name("p", username, device));
        boolean fresh = Ratchet.starts(header) && (current == null
                || !Ratchet.ephemeral(header).equals(current.startedBy()))
                && (previous == null || !Ratchet.ephemeral(header).equals(previous.startedBy()));
        CryptoException last = null;
        if (!fresh) {
            if (current != null) {
                try {
                    Ratchet trial = current.copy();
                    byte[] plain = trial.decrypt(header, headerText, ciphertext);
                    keep(username, device, trial, null);
                    return plain;
                } catch (CryptoException failed) {
                    last = failed;
                }
            }
            if (previous != null) {
                try {
                    Ratchet trial = previous.copy();
                    byte[] plain = trial.decrypt(header, headerText, ciphertext);
                    Vault.put(name("p", username, device), trial.toMap());
                    return plain;
                } catch (CryptoException failed) {
                    last = failed;
                }
            }
        }
        if (Ratchet.starts(header)) {
            Ratchet started = Ratchet.respond(header);
            byte[] plain = started.decrypt(header, headerText, ciphertext);
            keep(username, device, started, current);
            return plain;
        }
        throw last != null ? last : new CryptoException("No session with " + username + "/" + device);
    }

    /// The identity key of the device a session is with, for safety numbers.
    public static byte[] identity(String username, String device) {
        Ratchet session = current(username, device);
        return session == null ? null : session.theirIdentity();
    }

    public static void forget(String username, String device) {
        cache.remove(name("s", username, device));
        Vault.remove(name("s", username, device));
        Vault.remove(name("p", username, device));
    }

    private static Ratchet current(String username, String device) {
        String name = name("s", username, device);
        Ratchet out = cache.get(name);
        if (out == null) {
            out = load(name);
            if (out != null) {
                cache.put(name, out);
            }
        }
        return out;
    }

    private static void keep(String username, String device, Ratchet now, Ratchet before) {
        String name = name("s", username, device);
        cache.put(name, now);
        Vault.put(name, now.toMap());
        if (before != null) {
            Vault.put(name("p", username, device), before.toMap());
        }
    }

    private static Ratchet load(String name) {
        Map<String, Object> stored = Vault.get(name);
        return stored == null ? null : Ratchet.fromMap(stored);
    }

    /// Storage names say nothing about whom they are for.
    private static String name(String kind, String username, String device) {
        String hash = Codec.b64(Hash.sha256(Codec.utf8(username + "/" + device)));
        return kind + "-" + hash.substring(0, 22).replace('/', '_').replace('+', '-');
    }
}
