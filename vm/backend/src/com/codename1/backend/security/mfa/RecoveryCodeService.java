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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// Recovery codes: what signs a user in when their authenticator app is gone.
///
/// [#generate] makes ten, to be shown to the user once and never again -- the
/// server keeps a SHA-256 of each and cannot show them a second time. Each
/// works once, in place of a one-time code at sign-in. Generating again
/// replaces whatever was left.
public final class RecoveryCodeService {
    /// How many codes [#generate] makes.
    public static final int COUNT = 10;
    private static final char[] ALPHABET = "abcdefghjkmnpqrstuvwxyz23456789".toCharArray();

    private final RecoveryCodeRepository repository;

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
            hashes.add(hash(text.toString()));
        }
        repository.replace(username, hashes);
        return codes;
    }

    /// Uses a code up.
    ///
    /// @return whether `code` was one of `username`'s unused codes
    public boolean consume(String username, String code) {
        String hash = hash(code);
        return hash != null && repository.consume(username, hash);
    }

    /// How many codes `username` has left.
    public int remaining(String username) {
        return repository.count(username);
    }

    /// The hash a code is stored as: of its ten characters, without the dash
    /// or spaces and whatever case it was typed in. Null for what cannot be a
    /// code.
    static String hash(String code) {
        if (code == null) {
            return null;
        }
        byte[] plain = new byte[10];
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
            plain[length++] = (byte) c;
        }
        if (length != plain.length) {
            return null;
        }
        byte[] digest = Crypto.sha256(plain);
        StringBuilder hex = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            hex.append("0123456789abcdef".charAt((b >> 4) & 15))
               .append("0123456789abcdef".charAt(b & 15));
        }
        return hex.toString();
    }
}
