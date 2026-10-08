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
package com.codename1.backend.security.webauthn;

import java.util.Map;

/// The software authenticator, for the tests of other packages: a registration
/// and a sign-in answered as a real client would answer them.
public final class Passkeys {
    private final SoftAuthenticator authenticator;

    public Passkeys(boolean rsa, String rpId, String origin) throws Exception {
        authenticator = new SoftAuthenticator(rsa, rpId, origin);
    }

    /// Whether the authenticator verifies the user, which it does until told
    /// otherwise.
    public Passkeys userVerified(boolean verified) {
        authenticator.flags = verified ? SoftAuthenticator.UP | SoftAuthenticator.UV
                : SoftAuthenticator.UP;
        return this;
    }

    public Passkeys format(String format) {
        authenticator.format = format;
        return this;
    }

    public Passkeys origin(String origin) {
        authenticator.origin = origin;
        return this;
    }

    public Passkeys counter(long counter, boolean counts) {
        authenticator.counter = counter;
        authenticator.counts = counts;
        return this;
    }

    public byte[] credentialId() {
        return authenticator.credentialId.clone();
    }

    /// The `RegistrationResponseJSON` to these options.
    public Map<String, Object> create(Map options) throws Exception {
        return authenticator.create(options);
    }

    /// The `AuthenticationResponseJSON` to these options.
    public Map<String, Object> get(Map options) throws Exception {
        return authenticator.get(options);
    }
}
