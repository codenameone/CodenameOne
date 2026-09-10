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
package com.codename1.backend;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Java SE twin of Crypto, on the JDK's own providers.
 *
 * Same rule as the translated one: nothing is implemented by hand. The
 * constant-time compare is MessageDigest.isEqual, which the JDK documents as not
 * short-circuiting; a loop written here would let the optimizer decide, and an
 * early exit on the first differing byte lets a MAC be forged a byte at a time.
 */
public final class Crypto {
    public static final int PASSWORD_ITERATIONS = 210000;
    private static final int PASSWORD_SALT_BYTES = 16;
    private static final int PASSWORD_HASH_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private Crypto() {
    }

    public static byte[] sha256(byte[] data) {
        if(data == null) {
            return null;
        }
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (Exception err) {
            return null;
        }
    }

    /**
     * PBKDF2-HMAC-SHA-256. Exposed because SCRAM-SHA-256 -- how PostgreSQL
     * authenticates by default -- is defined in terms of it with the server's
     * iteration count, which {@link #hashPassword} does not let a caller choose.
     */
    public static byte[] pbkdf2Sha256(byte[] password, byte[] salt, int iterations, int length)
            throws IOException {
        return pbkdf2(password, salt, iterations, length);
    }

    /**
     * SHA-1, for the database wire protocols that specify it (MySQL's
     * mysql_native_password). Never for anything this code chooses.
     */
    public static byte[] sha1(byte[] data) {
        return digest("SHA-1", data);
    }

    /** MD5, for PostgreSQL's md5 authentication method. See {@link #sha1}. */
    public static byte[] md5(byte[] data) {
        return digest("MD5", data);
    }

    private static byte[] digest(String algorithm, byte[] data) {
        if(data == null) {
            return null;
        }
        try {
            return java.security.MessageDigest.getInstance(algorithm).digest(data);
        } catch (java.security.NoSuchAlgorithmException err) {
            throw new IllegalStateException(algorithm + " is not available", err);
        }
    }

    public static byte[] hmacSha256(byte[] key, byte[] data) {
        if(key == null || data == null) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception err) {
            return null;
        }
    }

    public static byte[] randomBytes(int length) throws IOException {
        if(length <= 0) {
            throw new IOException("No secure randomness available");
        }
        byte[] out = new byte[length];
        RANDOM.nextBytes(out);
        return out;
    }

    public static boolean equalsConstantTime(byte[] a, byte[] b) {
        if(a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a, b);
    }

    public static String hashPassword(String password) throws IOException {
        // A null password is a MISSING one, not an empty one. utf8(null) answers an
        // empty array, so a handler that passed a DTO field the client never sent
        // got a perfectly valid verifier -- and verifyPassword("", thatHash) then
        // succeeds, which turns an omitted credential into an empty-password
        // account. verifyPassword already refuses null; this is the other half.
        if(password == null) {
            throw new IllegalArgumentException("a password is required");
        }
        byte[] salt = randomBytes(PASSWORD_SALT_BYTES);
        byte[] hash = pbkdf2(utf8(password), salt, PASSWORD_ITERATIONS, PASSWORD_HASH_BYTES);
        return "pbkdf2$" + PASSWORD_ITERATIONS + "$" + Base64Url.encode(salt)
                + "$" + Base64Url.encode(hash);
    }

    public static boolean verifyPassword(String password, String stored) {
        if(password == null || stored == null) {
            return false;
        }
        String[] parts = split(stored, '$');
        if(parts.length != 4 || !"pbkdf2".equals(parts[0])) {
            return false;
        }
        int iterations;
        try {
            iterations = Integer.parseInt(parts[1]);
        } catch (NumberFormatException err) {
            return false;
        }
        byte[] salt = Base64Url.decode(parts[2]);
        byte[] expected = Base64Url.decode(parts[3]);
        if(salt == null || expected == null || iterations <= 0) {
            return false;
        }
        // Non-EMPTY, not merely non-null. "pbkdf2$1$$" decodes to two empty arrays,
        // pbkdf2 then derives zero bytes, and comparing an empty expectation with
        // an empty derivation is TRUE -- so a stored row of that shape accepted
        // every password. Base64Url.decode answers an empty array for an empty
        // field, so the null check above never saw it. The floors are the standard
        // minimums (RFC 8018 wants at least eight bytes of salt); anything this
        // server writes is 16 and 32.
        if(salt.length < 8 || expected.length < 16) {
            return false;
        }
        try {
            return equalsConstantTime(expected, pbkdf2(utf8(password), salt, iterations, expected.length));
        } catch (IOException err) {
            return false;
        }
    }

    /**
     * PBKDF2-HMAC-SHA256 over the password BYTES, per RFC 8018.
     *
     * Computed here rather than through PBEKeySpec, which takes chars and leaves the
     * encoding to the provider: for PBKDF2WithHmacSHA256 that encoding is UTF-8, so
     * a byte of 0xc3 handed over as a char came back out as TWO bytes. Mapping the
     * UTF-8 bytes to chars first therefore did not preserve them -- it re-encoded
     * them -- and the derived key stopped matching the native side, which passes the
     * original octets to OpenSSL. The effect was confined to non-ASCII passwords: a
     * hash written by one runtime that no longer verifies on the other, and a
     * PostgreSQL SCRAM proof that simply does not authenticate.
     */
    static byte[] pbkdf2(byte[] password, byte[] salt, int iterations, int length)
            throws IOException {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            // SecretKeySpec rejects a zero-length key. HMAC pads the key to the block
            // size with zeros, so a single zero byte and an empty key are the same
            // key -- the substitution is exact rather than a workaround.
            mac.init(new SecretKeySpec(password.length == 0 ? new byte[1] : password,
                    "HmacSHA256"));
            int hLen = mac.getMacLength();
            byte[] out = new byte[length];
            byte[] counted = new byte[salt.length + 4];
            System.arraycopy(salt, 0, counted, 0, salt.length);
            int done = 0;
            for(int block = 1 ; done < length ; block++) {
                counted[salt.length] = (byte)(block >>> 24);
                counted[salt.length + 1] = (byte)(block >>> 16);
                counted[salt.length + 2] = (byte)(block >>> 8);
                counted[salt.length + 3] = (byte)block;
                byte[] u = mac.doFinal(counted);
                byte[] t = new byte[hLen];
                System.arraycopy(u, 0, t, 0, hLen);
                for(int round = 1 ; round < iterations ; round++) {
                    u = mac.doFinal(u);
                    for(int iter = 0 ; iter < hLen ; iter++) {
                        t[iter] ^= u[iter];
                    }
                }
                int take = length - done < hLen ? length - done : hLen;
                System.arraycopy(t, 0, out, done, take);
                done += take;
            }
            return out;
        } catch (Exception err) {
            throw new IOException("Key derivation failed");
        }
    }

    static byte[] utf8(String value) {
        try {
            return value == null ? new byte[0] : value.getBytes("UTF-8");
        } catch (IOException err) {
            return new byte[0];
        }
    }

    private static String[] split(String value, char sep) {
        List<String> parts = new ArrayList<String>();
        int pos = 0;
        while(true) {
            int next = value.indexOf(sep, pos);
            if(next < 0) {
                parts.add(value.substring(pos));
                break;
            }
            parts.add(value.substring(pos, next));
            pos = next + 1;
        }
        return parts.toArray(new String[parts.size()]);
    }
}
