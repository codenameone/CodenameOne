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

/// PBKDF2 password hashes in the self-describing formats other frameworks
/// store, read as they write them: the digest and the round count come from
/// the stored value, so every row verifies whatever its era's settings were.
///
/// | format | stored value |
/// |---|---|
/// | [Format#DJANGO] | `pbkdf2_sha256$<rounds>$<salt>$<base64 hash>` (also `pbkdf2_sha1`) |
/// | [Format#PASSLIB] | `$pbkdf2-sha256$<rounds>$<ab64 salt>$<ab64 hash>` (also `$pbkdf2$`, `$pbkdf2-sha512$`) |
/// | [Format#WERKZEUG] | `pbkdf2:sha256:<rounds>$<salt>$<hex hash>` (also `sha1`, `sha512`) |
///
/// Register it under an id of its own and give the imported hashes that prefix:
///
/// ```java
/// Map<String, PasswordEncoder> encoders = new LinkedHashMap<>();
/// encoders.put("pbkdf2-sha256", new Pbkdf2Sha256PasswordEncoder());
/// encoders.put("django", new ModularPbkdf2PasswordEncoder(ModularPbkdf2PasswordEncoder.Format.DJANGO));
/// PasswordEncoder passwords = new DelegatingPasswordEncoder("pbkdf2-sha256", encoders);
/// // a row imported as "{django}pbkdf2_sha256$600000$..." verifies, and is
/// // re-hashed with the default when its owner signs in
/// ```
///
/// [#upgradeEncoding(String)] is true for every value: these are formats to
/// migrate away from, not to keep writing.
///
/// A stored value names its own work factor, so it is bounded. One above
/// `maxRounds` (5,000,000 by default) does not verify -- a hash that asked for
/// a billion rounds would hold the host thread it runs on for minutes. Werkzeug
/// values without a round count are refused too: the count they used was the
/// default of whichever Werkzeug release wrote them, and guessing it would
/// either fail every such login or need one PBKDF2 run per guess.
public final class ModularPbkdf2PasswordEncoder implements PasswordEncoder {
    /// The stored layouts read.
    public enum Format {
        DJANGO,
        PASSLIB,
        WERKZEUG
    }

    private static final int DEFAULT_ROUNDS = 600000;

    private final Format format;
    private int maxRounds = 5000000;
    private int encodeRounds = DEFAULT_ROUNDS;

    public ModularPbkdf2PasswordEncoder(Format format) {
        if (format == null) {
            throw new IllegalArgumentException("A format is required");
        }
        this.format = format;
    }

    /// The most rounds a stored value may ask for. Default 5,000,000.
    public void setMaxRounds(int maxRounds) {
        if (maxRounds < 1) {
            throw new IllegalArgumentException("maxRounds must be positive");
        }
        this.maxRounds = maxRounds;
    }

    /// Rounds [#encode(CharSequence)] writes. Default 600,000 with SHA-256.
    public void setEncodeRounds(int rounds) {
        if (rounds < 1) {
            throw new IllegalArgumentException("rounds must be positive");
        }
        this.encodeRounds = rounds;
    }

    /// A new SHA-256 hash in this format. Meant for fixtures and for tests of a
    /// migration; a running server writes its default encoder's format.
    @Override
    public String encode(CharSequence rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("rawPassword cannot be null");
        }
        byte[] random;
        try {
            random = Crypto.randomBytes(12);
        } catch (IOException err) {
            throw new IllegalStateException("Could not hash a password: " + err.getMessage(), err);
        }
        // Django and Werkzeug use the salt's characters as the salt, so it is
        // written as text that needs no escaping in either layout.
        String textSalt = hex(random);
        byte[] hash;
        switch (format) {
            case DJANGO:
                hash = derive(Crypto.SHA256, rawPassword, ascii(textSalt), encodeRounds, 32);
                return "pbkdf2_sha256$" + encodeRounds + "$" + textSalt + "$" + Base64.encode(hash);
            case PASSLIB:
                hash = derive(Crypto.SHA256, rawPassword, random, encodeRounds, 32);
                return "$pbkdf2-sha256$" + encodeRounds + "$" + ab64(random) + "$" + ab64(hash);
            default:
                hash = derive(Crypto.SHA256, rawPassword, ascii(textSalt), encodeRounds, 32);
                return "pbkdf2:sha256:" + encodeRounds + "$" + textSalt + "$" + hex(hash);
        }
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null || encodedPassword.length() == 0) {
            return false;
        }
        Parsed parsed = parse(encodedPassword);
        if (parsed == null || parsed.rounds < 1 || parsed.rounds > maxRounds
                || parsed.hash.length < 16 || parsed.hash.length > 64) {
            return false;
        }
        return Crypto.equalsConstantTime(parsed.hash, derive(parsed.digest, rawPassword,
                parsed.salt, parsed.rounds, parsed.hash.length));
    }

    @Override
    public boolean upgradeEncoding(String encodedPassword) {
        return true;
    }

    /// Whether `encodedPassword` is a value of this format this encoder would
    /// try to verify: the layout, a known digest and a round count within
    /// `maxRounds`. What a migration asks before importing a row.
    public boolean isReadable(String encodedPassword) {
        Parsed parsed = encodedPassword == null ? null : parse(encodedPassword);
        return parsed != null && parsed.rounds >= 1 && parsed.rounds <= maxRounds
                && parsed.hash.length >= 16 && parsed.hash.length <= 64;
    }

    private static final class Parsed {
        String digest;
        int rounds;
        byte[] salt;
        byte[] hash;
    }

    private Parsed parse(String value) {
        String[] parts;
        Parsed p = new Parsed();
        switch (format) {
            case DJANGO:
                parts = split(value, 4);
                if (parts == null) {
                    return null;
                }
                p.digest = "pbkdf2_sha256".equals(parts[0]) ? Crypto.SHA256
                        : "pbkdf2_sha1".equals(parts[0]) ? Crypto.SHA1 : null;
                p.rounds = number(parts[1]);
                p.salt = ascii(parts[2]);
                p.hash = Base64.decode(parts[3]);
                break;
            case PASSLIB:
                if (!value.startsWith("$")) {
                    return null;
                }
                parts = split(value.substring(1), 4);
                if (parts == null) {
                    return null;
                }
                p.digest = "pbkdf2-sha256".equals(parts[0]) ? Crypto.SHA256
                        : "pbkdf2-sha512".equals(parts[0]) ? Crypto.SHA512
                        : "pbkdf2".equals(parts[0]) ? Crypto.SHA1 : null;
                p.rounds = number(parts[1]);
                p.salt = unab64(parts[2]);
                p.hash = unab64(parts[3]);
                break;
            default:
                parts = split(value, 3);
                if (parts == null) {
                    return null;
                }
                String[] method = splitOn(parts[0], ':');
                if (method.length != 3 || !"pbkdf2".equals(method[0])) {
                    return null;
                }
                p.digest = "sha256".equals(method[1]) ? Crypto.SHA256
                        : "sha512".equals(method[1]) ? Crypto.SHA512
                        : "sha1".equals(method[1]) ? Crypto.SHA1 : null;
                p.rounds = number(method[2]);
                p.salt = ascii(parts[1]);
                p.hash = unhex(parts[2]);
                break;
        }
        if (p.digest == null || p.salt == null || p.salt.length == 0 || p.hash == null) {
            return null;
        }
        return p;
    }

    private static byte[] derive(String digest, CharSequence rawPassword, byte[] salt, int rounds,
                                 int length) {
        try {
            return Crypto.pbkdf2(digest, utf8(rawPassword.toString()), salt, rounds, length);
        } catch (IOException err) {
            throw new IllegalStateException("Could not hash a password: " + err.getMessage(), err);
        }
    }

    private static String[] split(String value, int count) {
        String[] parts = splitOn(value, '$');
        return parts.length == count ? parts : null;
    }

    private static String[] splitOn(String value, char separator) {
        int n = 1;
        for (int iter = 0 ; iter < value.length() ; iter++) {
            if (value.charAt(iter) == separator) {
                n++;
            }
        }
        String[] out = new String[n];
        int start = 0;
        int at = 0;
        for (int iter = 0 ; iter <= value.length() ; iter++) {
            if (iter == value.length() || value.charAt(iter) == separator) {
                out[at++] = value.substring(start, iter);
                start = iter + 1;
            }
        }
        return out;
    }

    private static int number(String text) {
        if (text.length() == 0 || text.length() > 9) {
            return -1;
        }
        int value = 0;
        for (int iter = 0 ; iter < text.length() ; iter++) {
            char c = text.charAt(iter);
            if (c < '0' || c > '9') {
                return -1;
            }
            value = value * 10 + (c - '0');
        }
        return value;
    }

    /// passlib's "adapted base64": `.` for `+`, no padding.
    private static String ab64(byte[] data) {
        String standard = Base64.encode(data);
        StringBuilder out = new StringBuilder(standard.length());
        for (int iter = 0 ; iter < standard.length() ; iter++) {
            char c = standard.charAt(iter);
            if (c == '+') {
                out.append('.');
            } else if (c != '=') {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static byte[] unab64(String text) {
        StringBuilder standard = new StringBuilder(text.length() + 3);
        for (int iter = 0 ; iter < text.length() ; iter++) {
            char c = text.charAt(iter);
            if (c == '+' || c == '=') {
                return null;
            }
            standard.append(c == '.' ? '+' : c);
        }
        while (standard.length() % 4 != 0) {
            standard.append('=');
        }
        return Base64.decode(standard.toString());
    }

    private static byte[] ascii(String text) {
        byte[] out = new byte[text.length()];
        for (int iter = 0 ; iter < out.length ; iter++) {
            char c = text.charAt(iter);
            if (c > 0x7e || c < 0x20) {
                return null;
            }
            out[iter] = (byte) c;
        }
        return out;
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

    private static byte[] unhex(String text) {
        if ((text.length() & 1) != 0) {
            return null;
        }
        byte[] out = new byte[text.length() / 2];
        for (int iter = 0 ; iter < out.length ; iter++) {
            int high = Character.digit(text.charAt(iter * 2), 16);
            int low = Character.digit(text.charAt(iter * 2 + 1), 16);
            if (high < 0 || low < 0) {
                return null;
            }
            out[iter] = (byte) ((high << 4) | low);
        }
        return out;
    }
}
