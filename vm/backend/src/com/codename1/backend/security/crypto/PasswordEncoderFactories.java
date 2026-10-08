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

/// Makes the [PasswordEncoder] an application should use unless it has a reason
/// to choose its own.
public final class PasswordEncoderFactories {
    private PasswordEncoderFactories() {
    }

    /// A [DelegatingPasswordEncoder] that encodes with `{pbkdf2-sha256}` and
    /// also verifies `{bcrypt}`, Spring's `{pbkdf2}` and
    /// `{pbkdf2@SpringSecurity_v5_8}` and, on a development profile, `{noop}`.
    public static PasswordEncoder createDelegatingPasswordEncoder() {
        // Spring's default here is bcrypt, and this is deliberately not.
        // PBKDF2-HMAC-SHA256 is the one password hash this runtime computes in
        // native code -- OpenSSL on a packaged server, with the SHA extensions of
        // the CPU -- where bcrypt is Blowfish written in Java. The difference
        // matters more here than on a JVM: hashing a password occupies the host
        // thread it runs on for its whole duration, a virtual thread cannot be
        // parked in the middle of it, and a burst of sign-ins therefore takes
        // request threads away from everything else for as long as the hashing
        // lasts. bcrypt stays registered, so hashes brought over from a Spring
        // application verify as they are and move to the default as their owners
        // sign in.
        String encodingId = "pbkdf2-sha256";
        Map<String, PasswordEncoder> encoders = new LinkedHashMap<String, PasswordEncoder>();
        encoders.put(encodingId, new Pbkdf2Sha256PasswordEncoder());
        encoders.put("bcrypt", new BCryptPasswordEncoder());
        // What a Spring application's user table holds under these two ids,
        // read as Spring reads them.
        encoders.put("pbkdf2", Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_5());
        encoders.put("pbkdf2@SpringSecurity_v5_8",
                Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8());
        encoders.put("noop", NoOpPasswordEncoder.getInstance());
        return new DelegatingPasswordEncoder(encodingId, encoders);
    }
}
