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

import java.util.LinkedHashMap;
import java.util.Map;

/// Reads which scheme a stored password was made with from the `{id}` in front
/// of it, so one user store can hold passwords of several ages:
///
/// ```java
/// {pbkdf2-sha256}pbkdf2$210000$3q2-7w$kZ...
/// {bcrypt}$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG
/// {noop}password
/// ```
///
/// New passwords are encoded with the scheme named at construction. A stored
/// password of any other scheme reports [#upgradeEncoding], which is what makes
/// a sign-in re-encode it.
public class DelegatingPasswordEncoder implements PasswordEncoder {
    private static final String PREFIX = "{";
    private static final String SUFFIX = "}";

    private final String idForEncode;
    private final PasswordEncoder passwordEncoderForEncode;
    private final Map<String, PasswordEncoder> idToPasswordEncoder;
    private PasswordEncoder defaultPasswordEncoderForMatches = new UnmappedIdPasswordEncoder();

    /// @param idForEncode the scheme new passwords are encoded with
    /// @param idToPasswordEncoder every scheme a stored password may name
    public DelegatingPasswordEncoder(String idForEncode,
                                     Map<String, PasswordEncoder> idToPasswordEncoder) {
        if (idForEncode == null) {
            throw new IllegalArgumentException("idForEncode cannot be null");
        }
        if (!idToPasswordEncoder.containsKey(idForEncode)) {
            throw new IllegalArgumentException("idForEncode " + idForEncode
                    + " is not found in idToPasswordEncoder " + idToPasswordEncoder.keySet());
        }
        for (String id : idToPasswordEncoder.keySet()) {
            if (id == null) {
                continue;
            }
            if (id.indexOf(PREFIX) >= 0) {
                throw new IllegalArgumentException("id " + id + " cannot contain " + PREFIX);
            }
            if (id.indexOf(SUFFIX) >= 0) {
                throw new IllegalArgumentException("id " + id + " cannot contain " + SUFFIX);
            }
        }
        this.idForEncode = idForEncode;
        this.passwordEncoderForEncode = idToPasswordEncoder.get(idForEncode);
        this.idToPasswordEncoder = new LinkedHashMap<String, PasswordEncoder>(idToPasswordEncoder);
    }

    /// The encoder a stored password with no `{id}`, or one this encoder does not
    /// know, is checked with. Unless set, such a password is refused with an
    /// exception saying so: set this to the scheme of a store that predates the
    /// prefixes.
    public synchronized void setDefaultPasswordEncoderForMatches(PasswordEncoder encoder) {
        if (encoder == null) {
            throw new IllegalArgumentException("defaultPasswordEncoderForMatches cannot be null");
        }
        this.defaultPasswordEncoderForMatches = encoder;
    }

    private synchronized PasswordEncoder defaultForMatches() {
        return defaultPasswordEncoderForMatches;
    }

    @Override
    public String encode(CharSequence rawPassword) {
        return PREFIX + idForEncode + SUFFIX + passwordEncoderForEncode.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String prefixEncodedPassword) {
        if (rawPassword == null && prefixEncodedPassword == null) {
            return true;
        }
        String id = extractId(prefixEncodedPassword);
        PasswordEncoder delegate = id == null ? null : idToPasswordEncoder.get(id);
        if (delegate == null) {
            return defaultForMatches().matches(rawPassword, prefixEncodedPassword);
        }
        return delegate.matches(rawPassword, extractEncodedPassword(prefixEncodedPassword));
    }

    @Override
    public boolean upgradeEncoding(String prefixEncodedPassword) {
        String id = extractId(prefixEncodedPassword);
        if (!idForEncode.equals(id)) {
            return true;
        }
        return passwordEncoderForEncode.upgradeEncoding(
                extractEncodedPassword(prefixEncodedPassword));
    }

    private static String extractId(String prefixEncodedPassword) {
        if (prefixEncodedPassword == null || !prefixEncodedPassword.startsWith(PREFIX)) {
            return null;
        }
        int end = prefixEncodedPassword.indexOf(SUFFIX, PREFIX.length());
        if (end < 0) {
            return null;
        }
        return prefixEncodedPassword.substring(PREFIX.length(), end);
    }

    private static String extractEncodedPassword(String prefixEncodedPassword) {
        int start = prefixEncodedPassword.indexOf(SUFFIX);
        return prefixEncodedPassword.substring(start + SUFFIX.length());
    }

    /// What a password with no usable `{id}` is checked with by default: nothing.
    private static final class UnmappedIdPasswordEncoder implements PasswordEncoder {
        @Override
        public String encode(CharSequence rawPassword) {
            throw new UnsupportedOperationException("encode is not supported");
        }

        @Override
        public boolean matches(CharSequence rawPassword, String prefixEncodedPassword) {
            String id = extractId(prefixEncodedPassword);
            if (id == null || id.length() == 0) {
                throw new IllegalArgumentException("You have entered a password with no "
                        + "PasswordEncoder. If that is your intent, it should be prefixed with "
                        + "`{noop}`.");
            }
            throw new IllegalArgumentException("There is no PasswordEncoder mapped for the id \""
                    + id + "\"");
        }
    }
}
