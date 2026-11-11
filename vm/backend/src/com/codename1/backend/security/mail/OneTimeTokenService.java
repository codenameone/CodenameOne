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
package com.codename1.backend.security.mail;

import com.codename1.backend.Base64Url;
import com.codename1.backend.Crypto;
import com.codename1.backend.security.Clock;
import com.codename1.impl.backend.WireBuffer;
import java.io.IOException;

/// Gives out tokens that work once: what a mailed link carries.
///
/// ```java
/// String token = tokens.create("invite", "ada@example.com", 3600);
/// // ... later, from the link
/// String username = tokens.consume("invite", token);   // null unless it is good
/// ```
///
/// A token is 256 random bits, written in 43 characters that need no escaping
/// in a URL. It is for one purpose and one user, stops working at its expiry
/// and when it is used, and is replaced when the same user is given another
/// for the same purpose. Only its SHA-256 is kept: whoever reads the table
/// learns no token, and a token is looked up by that hash, so how long a
/// lookup takes says nothing about a token that was guessed nearly right.
public final class OneTimeTokenService {
    private final OneTimeTokenRepository repository;
    private Clock clock = Clock.SYSTEM;

    public OneTimeTokenService(OneTimeTokenRepository repository) {
        if (repository == null) {
            throw new IllegalArgumentException("repository cannot be null");
        }
        this.repository = repository;
    }

    /// The clock expiry is measured by; a test sets its own.
    public OneTimeTokenService clock(Clock clock) {
        this.clock = clock;
        return this;
    }

    /// A new token for `username`, good for `validitySeconds` and for
    /// `purpose` alone. Any earlier token of that user and purpose stops
    /// working.
    public String create(String purpose, String username, int validitySeconds) {
        if (purpose == null || purpose.length() == 0 || username == null) {
            throw new IllegalArgumentException("A token needs a purpose and a user");
        }
        if (validitySeconds < 1) {
            throw new IllegalArgumentException("validitySeconds must be positive");
        }
        String token;
        try {
            token = Base64Url.encode(Crypto.randomBytes(32));
        } catch (IOException err) {
            throw new IllegalStateException("No random source: " + err.getMessage(), err);
        }
        long now = clock.currentTimeMillis();
        repository.save(hash(token), purpose, username, now, now + validitySeconds * 1000L);
        return token;
    }

    /// Uses `token` up and answers the user it was given to, or null when it
    /// is not a token for `purpose`, has expired or was already used.
    public String consume(String purpose, String token) {
        // 43 characters is what create() writes; anything else was never ours.
        if (purpose == null || token == null || token.length() != 43) {
            return null;
        }
        return repository.consume(hash(token), purpose, clock.currentTimeMillis());
    }

    /// Stops whatever token `username` holds for `purpose` from working.
    public void revoke(String purpose, String username) {
        repository.removeAll(username, purpose);
    }

    private static String hash(String token) {
        return Base64Url.encode(Crypto.sha256(WireBuffer.utf8(token)));
    }
}
