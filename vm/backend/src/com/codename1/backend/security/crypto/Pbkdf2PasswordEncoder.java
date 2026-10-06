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
package com.codename1.backend.security.crypto;

import com.codename1.backend.Base64;
import com.codename1.backend.Crypto;
import java.io.IOException;

/// Reads the PBKDF2 passwords Spring Security's `Pbkdf2PasswordEncoder` wrote,
/// so a user table brought over from a Spring application signs its users in
/// as it is.
///
/// A stored value is the salt followed by the derived key, as hexadecimal --
/// or base64, where the application had set that. How many rounds, which
/// digest and how long the salt is are not in it: they are whatever the encoder
/// that wrote it was configured with, and this one has to be configured the
/// same.
///
/// | Stored as | Written by | Made with |
/// |---|---|---|
/// | `{pbkdf2}` | [#defaultsForSpringSecurity_v5_5] | SHA-1, 185000 rounds, 8 byte salt |
/// | `{pbkdf2@SpringSecurity_v5_8}` | [#defaultsForSpringSecurity_v5_8] | SHA-256, 310000 rounds, 16 byte salt |
///
/// Both are registered in the encoder [PasswordEncoderFactories] makes, for
/// verifying. New passwords get `{pbkdf2-sha256}`, whose stored form says how
/// it was made; a sign-in with an older one re-encodes it.
public final class Pbkdf2PasswordEncoder implements PasswordEncoder {
    private final byte[] secret;
    private final int saltLength;
    private final int iterations;
    private final String digest;
    private final int hashBytes;
    private boolean encodeHashAsBase64;

    /// @param secret a value mixed into every salt -- Spring's "pepper"; empty
    /// for none
    /// @param saltLength the bytes of salt in front of every stored value
    /// @param iterations the rounds
    /// @param digest [Crypto#SHA1], [Crypto#SHA256] or [Crypto#SHA512]
    public Pbkdf2PasswordEncoder(CharSequence secret, int saltLength, int iterations, String digest) {
        this(secret, saltLength, iterations, digest, Crypto.SHA1.equals(digest) ? 20
                : Crypto.SHA512.equals(digest) ? 64 : 32);
    }

    private Pbkdf2PasswordEncoder(CharSequence secret, int saltLength, int iterations,
                                  String digest, int hashBytes) {
        if (!Crypto.SHA1.equals(digest) && !Crypto.SHA256.equals(digest)
                && !Crypto.SHA512.equals(digest)) {
            throw new IllegalArgumentException("The digest is SHA-1, SHA-256 or SHA-512, not "
                    + digest);
        }
        if (saltLength < 1 || iterations < 1) {
            throw new IllegalArgumentException("A salt length and a round count are required");
        }
        this.secret = utf8(secret == null ? "" : secret.toString());
        this.saltLength = saltLength;
        this.iterations = iterations;
        this.digest = digest;
        this.hashBytes = hashBytes;
    }

    /// What Spring Security wrote as `{pbkdf2}` up to its version 5.7, and
    /// still reads under that id: SHA-1, 185000 rounds, an 8 byte salt and a 32
    /// byte key.
    public static Pbkdf2PasswordEncoder defaultsForSpringSecurity_v5_5() { //NOPMD MethodNamingConventions - Spring's name
        return new Pbkdf2PasswordEncoder("", 8, 185000, Crypto.SHA1, 32);
    }

    /// What Spring Security writes as `{pbkdf2@SpringSecurity_v5_8}`: SHA-256,
    /// 310000 rounds, a 16 byte salt and a 32 byte key.
    public static Pbkdf2PasswordEncoder defaultsForSpringSecurity_v5_8() { //NOPMD MethodNamingConventions - Spring's name
        return new Pbkdf2PasswordEncoder("", 16, 310000, Crypto.SHA256, 32);
    }

    /// Whether stored values are base64 rather than hexadecimal; hexadecimal
    /// unless set.
    public void setEncodeHashAsBase64(boolean encodeHashAsBase64) {
        this.encodeHashAsBase64 = encodeHashAsBase64;
    }

    @Override
    public String encode(CharSequence rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("rawPassword cannot be null");
        }
        byte[] salt;
        try {
            salt = Crypto.randomBytes(saltLength);
        } catch (IOException err) {
            throw new IllegalStateException("Could not hash a password: " + err.getMessage(), err);
        }
        byte[] hash = derive(rawPassword, salt, hashBytes);
        byte[] stored = new byte[salt.length + hash.length];
        System.arraycopy(salt, 0, stored, 0, salt.length);
        System.arraycopy(hash, 0, stored, salt.length, hash.length);
        return encodeHashAsBase64 ? Base64.encode(stored) : hex(stored);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null || encodedPassword.length() == 0) {
            return false;
        }
        byte[] stored = encodeHashAsBase64 ? Base64.decode(encodedPassword) : unhex(encodedPassword);
        // The key is whatever follows the salt, and a stored value names its
        // own length that way: one too short to be a key, or long enough to
        // multiply the work, is not a password of this encoder.
        if (stored == null || stored.length < saltLength + 16 || stored.length > saltLength + 64) {
            return false;
        }
        byte[] salt = new byte[saltLength];
        byte[] expected = new byte[stored.length - saltLength];
        System.arraycopy(stored, 0, salt, 0, saltLength);
        System.arraycopy(stored, saltLength, expected, 0, expected.length);
        return Crypto.equalsConstantTime(expected, derive(rawPassword, salt, expected.length));
    }

    private byte[] derive(CharSequence rawPassword, byte[] salt, int length) {
        byte[] salted = new byte[salt.length + secret.length];
        System.arraycopy(salt, 0, salted, 0, salt.length);
        System.arraycopy(secret, 0, salted, salt.length, secret.length);
        try {
            // The password's UTF-8 bytes: what the JDK's PBKDF2 makes of the
            // characters Spring hands it.
            return Crypto.pbkdf2(digest, utf8(rawPassword.toString()), salted, iterations, length);
        } catch (IOException err) {
            throw new IllegalStateException("Could not hash a password: " + err.getMessage(), err);
        }
    }

    private static byte[] utf8(String value) {
        try {
            return value.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException err) {
            throw new IllegalStateException("UTF-8 is required", err);
        }
    }

    private static String hex(byte[] data) {
        char[] digits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e',
            'f'};
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(digits[(b >> 4) & 0xf]).append(digits[b & 0xf]);
        }
        return sb.toString();
    }

    /// The bytes of hexadecimal text in either case, or null when it is not.
    private static byte[] unhex(String text) {
        if ((text.length() & 1) != 0) {
            return null;
        }
        byte[] out = new byte[text.length() / 2];
        for (int iter = 0 ; iter < out.length ; iter++) {
            int high = digit(text.charAt(iter * 2));
            int low = digit(text.charAt(iter * 2 + 1));
            if (high < 0 || low < 0) {
                return null;
            }
            out[iter] = (byte) ((high << 4) | low);
        }
        return out;
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
        return -1;
    }
}
