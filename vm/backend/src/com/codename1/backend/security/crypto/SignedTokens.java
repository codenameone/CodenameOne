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

import com.codename1.backend.Base64Url;
import com.codename1.backend.Crypto;
import com.codename1.backend.security.Clock;
import java.io.ByteArrayOutputStream;

/// Makes and checks the tokens a server mails out: the link that confirms an
/// address, the link that resets a password, an invitation. A token names its
/// subject and when it stops working, is signed with HMAC-SHA256, and needs no
/// table: whoever comes back with one that verifies was sent it.
///
/// ```java
/// SignedTokens tokens = new SignedTokens(secret);          // 32 random bytes, kept
/// String link = "/reset?token=" + tokens.create("password-reset", user.getId(), 3600,
///         user.getPasswordHash());
/// // later, from the link:
/// String userId = tokens.verify("password-reset", token, user.getPasswordHash());
/// ```
///
/// Two things keep a token to the one job it was made for.
///
/// - **The purpose** is part of what is signed, so a token made to confirm an
///   address is not one that resets a password, though the same secret signed
///   both.
/// - **The fingerprint**, when one is given, is something that changes once the
///   token has done its work: the password hash for a reset link, the address
///   for a confirmation. It is signed and not carried, and the token stops
///   verifying the moment the value moves on -- which is what makes a reset
///   link single-use without a table of used ones. Where the subject has to be
///   known to look the fingerprint up, [#subject] reads it out of a token that
///   has not been checked yet.
///
/// The token is opaque to whoever holds it, and not secret from them: the
/// subject is readable. Do not put in it what its holder should not see.
public final class SignedTokens {
    private final byte[] secret;
    private Clock clock = Clock.SYSTEM;

    /// @param secret at least 32 bytes that stay the same for as long as the
    /// tokens are to verify; `Crypto.randomBytes(32)`, stored
    public SignedTokens(byte[] secret) {
        if (secret == null || secret.length < 32) {
            throw new IllegalArgumentException("The signing secret must be at least 32 bytes");
        }
        this.secret = secret.clone();
    }

    /// Reads the time from `clock` instead of the machine.
    public void setClock(Clock clock) {
        if (clock == null) {
            throw new IllegalArgumentException("clock cannot be null");
        }
        this.clock = clock;
    }

    /// A token for `subject` that works for `ttlSeconds`.
    public String create(String purpose, String subject, long ttlSeconds) {
        return create(purpose, subject, ttlSeconds, null);
    }

    /// A token for `subject` that works for `ttlSeconds`, and only while
    /// `fingerprint` is what it is now.
    public String create(String purpose, String subject, long ttlSeconds, String fingerprint) {
        if (purpose == null || purpose.length() == 0 || subject == null) {
            throw new IllegalArgumentException("A token needs a purpose and a subject");
        }
        if (ttlSeconds <= 0) {
            throw new IllegalArgumentException("A token needs a positive lifetime");
        }
        long expires = clock.currentTimeMillis() / 1000L + ttlSeconds;
        byte[] payload = utf8(expires + ":" + subject);
        return Base64Url.encode(payload) + "." + Base64Url.encode(mac(purpose, payload, fingerprint));
    }

    /// The subject of `token`, when it was made by this secret for this
    /// purpose and has not expired; null otherwise.
    public String verify(String purpose, String token) {
        return verify(purpose, token, null);
    }

    /// The subject of `token`, when it was made by this secret for this
    /// purpose while the fingerprint was `fingerprint`, and has not expired;
    /// null otherwise. Why a token is refused is not said: whoever presents a
    /// bad one learns nothing from the answer.
    public String verify(String purpose, String token, String fingerprint) {
        if (purpose == null || token == null) {
            return null;
        }
        int dot = token.indexOf('.');
        if (dot <= 0 || token.indexOf('.', dot + 1) >= 0) {
            return null;
        }
        byte[] payload = Base64Url.decode(token.substring(0, dot));
        byte[] presented = Base64Url.decode(token.substring(dot + 1));
        if (payload == null || presented == null) {
            return null;
        }
        // The signature first, and in constant time: nothing in the payload is
        // believed, not even its expiry, until it is known to be ours.
        if (!Crypto.equalsConstantTime(mac(purpose, payload, fingerprint), presented)) {
            return null;
        }
        String text = string(payload);
        int colon = text.indexOf(':');
        if (colon <= 0) {
            return null;
        }
        long expires;
        try {
            expires = Long.parseLong(text.substring(0, colon));
        } catch (NumberFormatException err) {
            return null;
        }
        if (clock.currentTimeMillis() / 1000L >= expires) {
            return null;
        }
        return text.substring(colon + 1);
    }

    /// The subject a token names, read without checking anything. For finding
    /// the record whose fingerprint [#verify] then needs -- never for deciding
    /// anything: this is what the bearer wrote, until `verify` says otherwise.
    ///
    /// @return the subject, or null when `token` does not have a token's shape
    public static String subject(String token) {
        if (token == null) {
            return null;
        }
        int dot = token.indexOf('.');
        byte[] payload = dot <= 0 ? null : Base64Url.decode(token.substring(0, dot));
        if (payload == null) {
            return null;
        }
        String text = string(payload);
        int colon = text.indexOf(':');
        return colon <= 0 ? null : text.substring(colon + 1);
    }

    /// HMAC over the purpose, the payload and the fingerprint, each with its
    /// length in front so that no two different triples are the same bytes.
    private byte[] mac(String purpose, byte[] payload, String fingerprint) {
        ByteArrayOutputStream signed = new ByteArrayOutputStream(payload.length + 64);
        field(signed, utf8(purpose));
        field(signed, payload);
        // "No fingerprint" and "an empty fingerprint" are different tokens.
        signed.write(fingerprint == null ? 0 : 1);
        field(signed, fingerprint == null ? new byte[0] : utf8(fingerprint));
        return Crypto.hmacSha256(secret, signed.toByteArray());
    }

    private static void field(ByteArrayOutputStream out, byte[] value) {
        out.write(value.length >>> 24);
        out.write(value.length >>> 16);
        out.write(value.length >>> 8);
        out.write(value.length);
        out.write(value, 0, value.length);
    }

    private static byte[] utf8(String value) {
        try {
            return value.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException err) {
            throw new IllegalStateException("UTF-8 is required", err);
        }
    }

    private static String string(byte[] value) {
        try {
            return new String(value, "UTF-8");
        } catch (java.io.UnsupportedEncodingException err) {
            throw new IllegalStateException("UTF-8 is required", err);
        }
    }
}
