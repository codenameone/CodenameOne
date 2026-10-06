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

import com.codename1.backend.Crypto;
import java.io.IOException;

/// PBKDF2-HMAC-SHA256 through the runtime's own [Crypto#hashPassword] and
/// [Crypto#verifyPassword]: a random salt per password, and the round count
/// written into the result, so a stored value says how it is to be checked.
///
/// ```java
/// pbkdf2$210000$<salt>$<hash>
/// ```
///
/// Registered as `{pbkdf2-sha256}`, the scheme new passwords get. It is not the
/// format of Spring's `Pbkdf2PasswordEncoder`, whose `{pbkdf2}` hashes this does
/// not read.
public final class Pbkdf2Sha256PasswordEncoder implements PasswordEncoder {
    @Override
    public String encode(CharSequence rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("rawPassword cannot be null");
        }
        try {
            return Crypto.hashPassword(rawPassword.toString());
        } catch (IOException err) {
            throw new IllegalStateException("Could not hash a password: " + err.getMessage(), err);
        }
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null || encodedPassword.length() == 0) {
            return false;
        }
        // Compared in constant time inside: the derived key against the stored one.
        return Crypto.verifyPassword(rawPassword.toString(), encodedPassword);
    }

    /// True when the stored password was made with fewer rounds than passwords
    /// get now.
    @Override
    public boolean upgradeEncoding(String encodedPassword) {
        if (encodedPassword == null || !encodedPassword.startsWith("pbkdf2$")) {
            return false;
        }
        int end = encodedPassword.indexOf('$', 7);
        if (end < 0) {
            return false;
        }
        try {
            return Integer.parseInt(encodedPassword.substring(7, end)) < Crypto.PASSWORD_ITERATIONS;
        } catch (NumberFormatException err) {
            return false;
        }
    }
}
