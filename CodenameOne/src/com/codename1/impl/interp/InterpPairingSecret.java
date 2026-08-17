/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.impl.interp;

import com.codename1.security.Hmac;

/// The shared secret a paired computer proves it holds, on every connection.
///
/// #### What this replaces, and why
///
/// The first version of pairing sent a peer id and let a matching id authorise
/// every later push. That is a bearer token in plaintext on a LAN: anyone who
/// watched one push, or who guessed the id, could push a program of their own
/// to somebody's phone -- and a program is arbitrary code. Worse, the id never
/// changed, so a single captured frame worked forever.
///
/// What crosses the wire now is never enough to reuse. Pairing derives a
/// 256-bit secret on both ends **without transmitting it**: the only secret
/// input is the code the IDE prints and a human types into the device, and the
/// two ends combine it with the peer id and the device id, both of which are
/// public. Every connection afterwards is a fresh challenge from the device and
/// an HMAC over it, so a captured response authenticates exactly one
/// connection, and a push additionally MACs the bundle so the bytes that run
/// are the bytes that were authorised.
///
/// #### The residual weakness, stated plainly
///
/// The code is six digits, so an attacker who records a *pairing* exchange can
/// grind 10^6 candidates offline. [#ITERATIONS] iterations is what makes that
/// cost real rather than instant, and it is the reason the derivation is
/// deliberately slow. It is not a PAKE; a passive observer of the pairing
/// handshake is still the attacker this does not defeat. Observing any number
/// of *pushes*, which is the exposure that actually persists, tells them
/// nothing.
///
/// It lives in core rather than in the runtime app because it has two callers
/// that share no code: the device, and the desktop push tool, which mirrors it
/// against the JDK's own HMAC. One authoritative definition is the difference
/// between "pairing broke" and "pairing broke and a test said which end
/// changed".
///
/// @author Shai Almog
public final class InterpPairingSecret {
    /// Iterations of the derivation.
    ///
    /// Chosen so a phone spends well under a second on it once, at pairing,
    /// while an attacker grinding the six-digit code pays that cost a million
    /// times over. Changing it invalidates every existing pairing, which is
    /// tolerable (pair again) but not free.
    public static final int ITERATIONS = 20000;

    private InterpPairingSecret() {
    }

    /// Derives the shared secret from the typed code and the two public ids.
    ///
    /// Both ends compute this independently; it is never transmitted. The peer
    /// id and device id are bound in so a code seen on one device cannot pair a
    /// different one, and so two computers pairing with the same device do not
    /// end up holding the same key.
    public static byte[] derive(String code, String peerId, String deviceId) {
        byte[] key = utf8(code == null ? "" : code.trim());
        byte[] block = Hmac.sha256(key,
                utf8("cn1-device-runtime|" + peerId + "|" + deviceId));
        for (int i = 1; i < ITERATIONS; i++) {
            block = Hmac.sha256(key, block);
        }
        return block;
    }

    /// The answer to a challenge: hex HMAC-SHA256 of the challenge under the
    /// secret. Used for the pairing handshake, where there is no payload yet.
    public static String respond(byte[] secret, String challenge) {
        return hex(Hmac.sha256(secret, utf8(challenge)));
    }

    /// The answer to a challenge over a bundle.
    ///
    /// The payload is covered as well as the challenge, so an attacker who can
    /// modify the stream cannot swap in a different program behind a valid
    /// response -- the response would no longer verify against what arrived.
    public static String respond(byte[] secret, String challenge, byte[] payload) {
        Hmac mac = Hmac.create(com.codename1.security.Hash.SHA256, secret);
        mac.update(utf8(challenge));
        mac.update(payload);
        return hex(mac.doFinal());
    }

    /// Compares two hex responses without leaking where they first differ.
    public static boolean matches(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return Hmac.constantTimeEquals(utf8(a), utf8(b));
    }

    /// A fresh challenge: 32 random bytes as hex. The device issues one per
    /// connection, which is what makes a captured response worthless.
    public static String challenge() {
        return hex(com.codename1.security.SecureRandom.bytes(32));
    }

    /// Lowercase hex, since the values travel as UTF strings on the wire and
    /// live in a properties file on the desktop.
    public static String hex(byte[] data) {
        char[] out = new char[data.length * 2];
        for (int i = 0; i < data.length; i++) {
            int b = data[i] & 0xff;
            out[i * 2] = hexDigit(b >> 4);
            out[i * 2 + 1] = hexDigit(b & 0xf);
        }
        return new String(out);
    }

    /// The inverse of [#hex(byte[])], for a secret read back from storage.
    public static byte[] unhex(String s) {
        byte[] out = new byte[s.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte)((digit(s.charAt(i * 2)) << 4) | digit(s.charAt(i * 2 + 1)));
        }
        return out;
    }

    private static char hexDigit(int nibble) {
        return (char)(nibble < 10 ? '0' + nibble : 'a' + nibble - 10);
    }

    private static int digit(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        throw new IllegalArgumentException("not hex: " + c);
    }

    private static byte[] utf8(String s) {
        // getBytes("UTF-8") throws a checked exception on the device's API and
        // every caller here is passing hex or a typed code; encoding cannot
        // fail, so the checked exception would only add noise.
        try {
            return s.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 is always available");
        }
    }
}
