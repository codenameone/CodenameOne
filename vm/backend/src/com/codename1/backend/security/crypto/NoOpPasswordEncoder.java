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

/// Stores a password as it is: `{noop}secret`. For a demonstration or a test,
/// where the point is to read the password in the source.
///
/// It verifies only on a development profile. Anywhere else a `{noop}` password
/// matches nothing and the server says why once on standard error, because a
/// clear-text password that works in production is a configuration left over
/// from a laptop.
public final class NoOpPasswordEncoder implements PasswordEncoder {
    private static final NoOpPasswordEncoder INSTANCE = new NoOpPasswordEncoder();
    private static boolean development;
    private static boolean warned;

    private NoOpPasswordEncoder() {
    }

    public static NoOpPasswordEncoder getInstance() {
        return INSTANCE;
    }

    /// Says whether this process runs on a development profile. The security
    /// layer calls it from the configuration it is built with; a test that uses
    /// the encoder on its own calls it too.
    public static synchronized void setDevelopmentProfile(boolean value) {
        development = value;
    }

    private static synchronized boolean allowed() {
        if (!development && !warned) {
            warned = true;
            System.err.println("cn1: a {noop} password was presented for checking and "
                    + "refused: clear-text passwords verify only on a development profile. "
                    + "Store the password encoded -- PasswordEncoderFactories"
                    + ".createDelegatingPasswordEncoder().encode(...).");
        }
        return development;
    }

    @Override
    public String encode(CharSequence rawPassword) {
        return rawPassword.toString();
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        // Compared first and in constant time either way, so a refusal takes as
        // long as an acceptance would have.
        boolean same = Crypto.equalsConstantTime(BCryptPasswordEncoder.utf8(rawPassword.toString()),
                BCryptPasswordEncoder.utf8(encodedPassword));
        return allowed() && same;
    }
}
