/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.security.vault;

import com.codename1.io.Util;
import com.codename1.security.Hash;
import com.codename1.security.Hmac;

/// The portable password key derivation this package uses, as a versioned profile rather than a
/// set of numbers each caller picks.
///
/// #### Why PBKDF2 and not something better
///
/// Argon2id and scrypt are better password KDFs, and neither exists in a browser. Web Crypto
/// implements exactly one -- `PBKDF2` -- and a password vault whose whole point is that Android,
/// iOS and the browser derive the same key from the same password cannot use a KDF that one of
/// the three has to emulate in application code. A JavaScript Argon2 would be orders of magnitude
/// slower than the native one the phone uses, which in practice means the parameters get lowered
/// until the browser is usable and every platform is then weaker than PBKDF2 would have been.
///
/// So: PBKDF2-HMAC-SHA256, with the iteration count carried in the envelope so it can be raised
/// without breaking anything already written. [#current()] is what new material is derived with;
/// [#needsUpgrade()] tells a caller that an envelope it just opened was written under a weaker
/// profile and should be rewrapped.
///
/// #### Bounds
///
/// The iteration count is clamped into [#MIN_ITERATIONS] to [#MAX_ITERATIONS] on the way in and
/// **rejected** rather than clamped on the way out of a parsed envelope. The asymmetry is the
/// point: an attacker who can edit stored bytes would otherwise set the count to 1 and turn a
/// password check into a guessable one, or set it to two billion and make the application hang on
/// open. A count outside the range in a stored envelope is [VaultError#UNSUPPORTED_FORMAT].
///
/// #### Where the work happens
///
/// The derivation is one native call on every port that has one ([Util#pbkdf2]), and a pure Java
/// loop over [Hmac] where there is none. The pure Java path produces identical bytes -- it is
/// RFC 8018 with no latitude in it -- but 600000 iterations of software HMAC is slow enough to be
/// a problem, so a port without the hook should add one rather than rely on it.
public final class KdfProfile {

    /// PBKDF2 with HMAC-SHA-256, the only KDF identifier version 1 of the envelope defines.
    public static final int PBKDF2_HMAC_SHA256 = 1;

    /// No derivation: the envelope is sealed directly under a key the caller already has.
    public static final int DIRECT = 0;

    /// Below this an iteration count is not a password KDF, it is a formality. OWASP's 2023
    /// floor for PBKDF2-HMAC-SHA256 is 600000; 100000 is the oldest count this will still open
    /// material written under, and [#needsUpgrade()] reports it.
    public static final int MIN_ITERATIONS = 100000;

    /// Above this the derivation is a denial of service against the user's own device. Ten
    /// million iterations is roughly a minute of a phone's time.
    public static final int MAX_ITERATIONS = 10000000;

    /// What new material is derived with today. Raising this is a compatible change: existing
    /// envelopes carry their own count and keep opening.
    public static final int DEFAULT_ITERATIONS = 600000;

    /// The salt length this package generates. Sixteen bytes of fresh randomness per envelope,
    /// which is what stops one precomputation covering two users.
    public static final int SALT_LENGTH = 16;

    private final int kdfId;
    private final int iterations;

    private KdfProfile(int kdfId, int iterations) {
        this.kdfId = kdfId;
        this.iterations = iterations;
    }

    /// The profile new material should be derived with.
    public static KdfProfile current() {
        return new KdfProfile(PBKDF2_HMAC_SHA256, DEFAULT_ITERATIONS);
    }

    /// A profile with an explicit iteration count, clamped into the supported range.
    ///
    /// Clamping rather than throwing because this is the caller-facing constructor and a caller
    /// asking for 1000 iterations has made a mistake worth correcting silently upward. The
    /// parsing path does not clamp -- see [#forStored(int, int)].
    ///
    /// #### Parameters
    ///
    /// - `iterations`: the requested count
    public static KdfProfile pbkdf2(int iterations) {
        int clamped = iterations;
        if (clamped < MIN_ITERATIONS) {
            clamped = MIN_ITERATIONS;
        }
        if (clamped > MAX_ITERATIONS) {
            clamped = MAX_ITERATIONS;
        }
        return new KdfProfile(PBKDF2_HMAC_SHA256, clamped);
    }

    /// The profile an envelope declared, validated rather than clamped.
    ///
    /// #### Parameters
    ///
    /// - `kdfId`: the identifier read from the envelope
    ///
    /// - `iterations`: the count read from the envelope
    ///
    /// #### Returns
    ///
    /// the profile
    ///
    /// #### Throws
    ///
    /// - `VaultException`: [VaultError#UNSUPPORTED_FORMAT] for an unknown identifier or a count
    ///   outside the supported range
    static KdfProfile forStored(int kdfId, int iterations) {
        if (kdfId == DIRECT) {
            if (iterations != 0) {
                throw new VaultException(VaultError.UNSUPPORTED_FORMAT,
                        "envelope declares no key derivation but carries an iteration count");
            }
            return new KdfProfile(DIRECT, 0);
        }
        if (kdfId != PBKDF2_HMAC_SHA256) {
            throw new VaultException(VaultError.UNSUPPORTED_FORMAT,
                    "unsupported key derivation function id " + kdfId);
        }
        if (iterations < MIN_ITERATIONS || iterations > MAX_ITERATIONS) {
            // Not clamped. A stored count is attacker-reachable on any platform where the
            // ciphertext is, and accepting a low one would silently turn the password check into
            // one a laptop can brute force.
            throw new VaultException(VaultError.UNSUPPORTED_FORMAT,
                    "envelope declares " + iterations + " KDF iterations, outside the supported "
                    + MIN_ITERATIONS + " to " + MAX_ITERATIONS + " range");
        }
        return new KdfProfile(PBKDF2_HMAC_SHA256, iterations);
    }

    /// The envelope's KDF identifier: [#PBKDF2_HMAC_SHA256] or [#DIRECT].
    public int getKdfId() {
        return kdfId;
    }

    /// The iteration count, or zero for [#DIRECT].
    public int getIterations() {
        return iterations;
    }

    /// Whether material written under this profile should be rewrapped under [#current()].
    ///
    /// True for anything weaker than today's default. An application that opens a vault, sees
    /// this, and has the password in hand should re-derive and rewrite; one that does not can
    /// carry on, because the envelope still opens.
    public boolean needsUpgrade() {
        return kdfId == PBKDF2_HMAC_SHA256 && iterations < DEFAULT_ITERATIONS;
    }

    /// Derives `length` bytes from a password and salt.
    ///
    /// #### Parameters
    ///
    /// - `password`: the password, as characters so the caller can clear them. Encoded with this
    ///   package's UTF-8 and not normalized -- see [Bytes#utf8(char[])].
    ///
    /// - `salt`: fresh per envelope, at least [#SALT_LENGTH] bytes for new material
    ///
    /// - `length`: how many bytes to produce, normally 32 for an AES-256 key
    ///
    /// #### Returns
    ///
    /// the derived bytes, which the caller owns and should clear
    public byte[] derive(char[] password, byte[] salt, int length) {
        if (kdfId == DIRECT) {
            throw new VaultException(VaultError.UNSUPPORTED_FORMAT,
                    "this profile performs no derivation");
        }
        if (salt == null || salt.length == 0) {
            throw new VaultException(VaultError.CORRUPT, "key derivation requires a salt");
        }
        if (length <= 0 || length > 1024) {
            throw new VaultException(VaultError.UNSUPPORTED_FORMAT,
                    "unsupported derived key length " + length);
        }
        byte[] passwordBytes = Bytes.utf8(password);
        try {
            byte[] platform = Util.pbkdf2(Hash.SHA256, passwordBytes, salt, iterations, length);
            if (platform != null) {
                return platform;
            }
            return pbkdf2Portable(passwordBytes, salt, iterations, length);
        } finally {
            Bytes.zero(passwordBytes);
        }
    }

    /// RFC 8018 PBKDF2 over [Hmac], for a port with no native derivation.
    ///
    /// Package visible rather than private so the test vectors can reach it directly. The public
    /// entry point clamps the iteration count into the supported range, and the published vectors
    /// are at RFC 8018's counts -- which are far below that floor on purpose, because a vector
    /// nobody can run in under a minute is a vector nobody checks.
    ///
    /// Byte-identical to the native path by construction: PBKDF2 has no implementation latitude,
    /// which is exactly why it can be the interoperable choice. Slow, though -- see the class
    /// note.
    static byte[] pbkdf2Portable(byte[] password, byte[] salt, int iterations, int length) {
        int hashLength = 32;
        int blocks = (length + hashLength - 1) / hashLength;
        byte[] out = new byte[blocks * hashLength];
        byte[] block = new byte[salt.length + 4];
        System.arraycopy(salt, 0, block, 0, salt.length);
        Hmac mac = Hmac.create(Hash.SHA256, password);
        for (int index = 1; index <= blocks; index++) {
            Bytes.putInt(block, salt.length, index);
            mac.reset();
            mac.update(block, 0, block.length);
            byte[] u = mac.doFinal();
            byte[] accumulated = new byte[hashLength];
            System.arraycopy(u, 0, accumulated, 0, hashLength);
            for (int round = 1; round < iterations; round++) {
                mac.reset();
                mac.update(u, 0, u.length);
                u = mac.doFinal();
                for (int iter = 0; iter < hashLength; iter++) {
                    accumulated[iter] ^= u[iter];
                }
            }
            System.arraycopy(accumulated, 0, out, (index - 1) * hashLength, hashLength);
            Bytes.zero(accumulated);
        }
        byte[] exact = new byte[length];
        System.arraycopy(out, 0, exact, 0, length);
        Bytes.zero(out);
        Bytes.zero(block);
        return exact;
    }

    @Override
    public String toString() {
        return kdfId == DIRECT ? "direct" : ("PBKDF2-HMAC-SHA256/" + iterations);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof KdfProfile)) {
            return false;
        }
        KdfProfile o = (KdfProfile) other;
        return kdfId == o.kdfId && iterations == o.iterations;
    }

    @Override
    public int hashCode() {
        return kdfId * 31 + iterations;
    }
}
