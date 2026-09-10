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

/**
 * The crypto a server needs to authenticate a request. Every primitive comes from
 * OpenSSL, which the backend already links for outbound TLS - none of it is
 * implemented here, because hand-rolled HMAC and hand-rolled password hashing are
 * the two most reliable ways to ship an authentication system that looks correct
 * and is not.
 */
public final class Crypto {
    /**
     * PBKDF2 iterations for a stored password. Deliberately expensive: the cost is
     * paid once per login and multiplied by every guess an attacker makes against a
     * stolen table.
     */
    public static final int PASSWORD_ITERATIONS = 210000;
    private static final int PASSWORD_SALT_BYTES = 16;
    private static final int PASSWORD_HASH_BYTES = 32;

    private Crypto() {
    }

    public static byte[] sha256(byte[] data) {
        return sha256Impl(data);
    }

    /**
     * SHA-1, for the database wire protocols that specify it (MySQL's
     * mysql_native_password). Never for anything this code chooses: passwords go
     * through {@link #hashPassword} and tokens through {@link #hmacSha256}.
     */
    public static byte[] sha1(byte[] data) {
        return sha1Impl(data);
    }

    /** MD5, for PostgreSQL's md5 authentication method. See {@link #sha1}. */
    public static byte[] md5(byte[] data) {
        return md5Impl(data);
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

    public static byte[] hmacSha256(byte[] key, byte[] data) {
        return hmacSha256Impl(key, data);
    }

    /** Cryptographically secure bytes. Throws rather than returning weak ones. */
    public static byte[] randomBytes(int length) throws IOException {
        byte[] out = randomBytesImpl(length);
        if(out == null) {
            throw new IOException("No secure randomness available");
        }
        return out;
    }

    /**
     * Compares without leaking where two values first differ. An early exit on the
     * first differing byte lets a MAC be forged one byte at a time.
     */
    public static boolean equalsConstantTime(byte[] a, byte[] b) {
        return equalsConstantTimeImpl(a, b);
    }

    /**
     * Hashes a password for storage. Returns "pbkdf2$iterations$salt$hash" with
     * both binary parts base64url-encoded, so the iteration count travels with the
     * hash and can be raised later without invalidating existing rows.
     */
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
        return "pbkdf2$" + PASSWORD_ITERATIONS + "$" + Base64Url.encode(salt) + "$" + Base64Url.encode(hash);
    }

    /** False for any malformed stored value rather than throwing. */
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
        byte[] actual = pbkdf2Impl(utf8(password), salt, iterations, expected.length);
        return actual != null && equalsConstantTime(expected, actual);
    }

    static byte[] pbkdf2(byte[] password, byte[] salt, int iterations, int length) throws IOException {
        byte[] out = pbkdf2Impl(password, salt, iterations, length);
        if(out == null) {
            throw new IOException("Key derivation failed");
        }
        return out;
    }

    static byte[] utf8(String value) {
        try {
            return value == null ? new byte[0] : value.getBytes("UTF-8");
        } catch (IOException err) {
            return new byte[0];
        }
    }

    private static String[] split(String value, char sep) {
        java.util.List parts = new java.util.ArrayList();
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
        String[] out = new String[parts.size()];
        for(int iter = 0 ; iter < out.length ; iter++) {
            out[iter] = (String)parts.get(iter);
        }
        return out;
    }

    private static native byte[] sha256Impl(byte[] data);
    private static native byte[] sha1Impl(byte[] data);
    private static native byte[] md5Impl(byte[] data);
    private static native byte[] hmacSha256Impl(byte[] key, byte[] data);
    private static native byte[] pbkdf2Impl(byte[] password, byte[] salt, int iterations, int length);
    private static native byte[] randomBytesImpl(int length);
    private static native boolean equalsConstantTimeImpl(byte[] a, byte[] b);
}
