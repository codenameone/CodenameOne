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
package com.codename1.backend.security.mfa;

import com.codename1.backend.Crypto;
import com.codename1.backend.security.SecuritySchema;
import com.codename1.backend.security.crypto.Pbkdf2Sha256PasswordEncoder;
import com.codename1.backend.security.crypto.PasswordEncoder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

/// Recovery codes: what signs a user in when their authenticator app is gone.
///
/// [#generate] makes ten, to be shown to the user once and never again -- the
/// server keeps a salted PBKDF2 password hash of each and cannot show them a second time. Each
/// works once, in place of a one-time code at sign-in. Generating again
/// replaces whatever was left.
public final class RecoveryCodeService {
    /// How many codes [#generate] makes.
    public static final int COUNT = 10;
    private static final char[] ALPHABET = "abcdefghjkmnpqrstuvwxyz23456789".toCharArray();

    // Shared across service instances: a caller cannot multiply expensive verification
    // work by opening more sessions or using another client network.
    private static final Set<String> VERIFYING = new HashSet<String>();
    private static final int MAX_VERIFYING = 4;

    private final RecoveryCodeRepository repository;
    private final PasswordEncoder encoder = new Pbkdf2Sha256PasswordEncoder();

    public RecoveryCodeService(RecoveryCodeRepository repository) {
        if (repository == null) {
            throw new IllegalArgumentException("repository cannot be null");
        }
        this.repository = repository;
    }

    /// Makes [#COUNT] new codes for `username`, replacing any they had.
    ///
    /// @return the codes, each written `xxxxx-xxxxx`: the only time they exist
    /// outside the user's keeping
    public List<String> generate(String username) {
        List<String> codes = new ArrayList<String>(COUNT);
        List<String> hashes = new ArrayList<String>(COUNT);
        byte[] random;
        try {
            // A byte for each character and as many again to spare: a byte
            // that would favour the start of the alphabet is passed over.
            random = Crypto.randomBytes(COUNT * 10 * 4);
        } catch (IOException err) {
            throw new IllegalStateException("No random source: " + err.getMessage(), err);
        }
        int limit = 256 - 256 % ALPHABET.length;
        int at = 0;
        for (int code = 0 ; code < COUNT ; code++) {
            StringBuilder text = new StringBuilder(11);
            while (text.length() < 11) {
                if (text.length() == 5) {
                    text.append('-');
                    continue;
                }
                if (at == random.length) {
                    try {
                        random = Crypto.randomBytes(random.length);
                    } catch (IOException err) {
                        throw new IllegalStateException("No random source: " + err.getMessage(),
                                err);
                    }
                    at = 0;
                }
                int value = random[at++] & 0xff;
                if (value < limit) {
                    text.append(ALPHABET[value % ALPHABET.length]);
                }
            }
            codes.add(text.toString());
            hashes.add(encoder.encode(normalize(text.toString())));
        }
        repository.replace(username, hashes);
        return codes;
    }

    /// Uses a code up. At most four verifications run in this process, and at most
    /// one per username (case insensitive). A busy verifier returns false immediately;
    /// it does not consume a code or queue password-hashing work.
    ///
    /// @return whether `code` was one of `username`'s unused codes
    public boolean consume(String username, String code) {
        String plain = normalize(code);
        if (username == null || plain == null) {
            return false;
        }
        String user = SecuritySchema.usernameKey(username);
        synchronized (VERIFYING) {
            if (VERIFYING.size() >= MAX_VERIFYING || !VERIFYING.add(user)) {
                return false;
            }
        }
        try {
            for (String hash : repository.findHashes(username)) {
                if (encoder.matches(plain, hash)) {
                    // Only the atomic removal decides success across server processes.
                    return repository.consume(username, hash);
                }
            }
            return false;
        } finally {
            synchronized (VERIFYING) {
                VERIFYING.remove(user);
            }
        }
    }

    /// Whether `code` is written as a recovery code is: ten letters and digits,
    /// with or without the dash. It says nothing about whether anybody has that
    /// code -- only that it is not something else, such as the digits of a
    /// one-time code.
    public static boolean isCodeShaped(String code) {
        return normalize(code) != null;
    }

    /// How many codes `username` has left.
    public int remaining(String username) {
        return repository.count(username);
    }

    /// Its ten characters without separators, in lowercase, or null when malformed.
    private static String normalize(String code) {
        if (code == null) {
            return null;
        }
        char[] plain = new char[10];
        int length = 0;
        for (int iter = 0 ; iter < code.length() ; iter++) {
            char c = code.charAt(iter);
            if (c == '-' || c == ' ') {
                continue;
            }
            if (c >= 'A' && c <= 'Z') {
                c = (char) (c + ('a' - 'A'));
            }
            if (length == plain.length || !((c >= 'a' && c <= 'z') || (c >= '2' && c <= '9'))) {
                return null;
            }
            plain[length++] = c;
        }
        if (length != plain.length) {
            return null;
        }
        return new String(plain);
    }
}
