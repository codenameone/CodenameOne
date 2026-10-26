/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bastion;

import com.codename1.backend.Config;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.PostConstruct;
import com.codename1.backend.annotations.Value;

/// Refuses to start a server outside a development profile whose secrets or
/// passkey site are still what this sample ships with.
///
/// The token-signing key, the key authenticator secrets are encrypted under
/// and the passkey site all have a value that makes `dev` work out of the
/// box: a key made at start-up, none, `localhost`. A deployment that forgot
/// one would otherwise run -- with tokens nothing else can verify, secrets
/// kept in the clear, or passkeys bound to a site that is not it -- and look
/// healthy. The same rule as
/// [com.codenameone.examples.bastion.money.SimulatedIntegrityVerifier]: fail
/// the start, naming what to set.
@Component
public class ProductionSettings {
    static final String DEFAULT_RP_ID = "localhost";
    static final String DEFAULT_ORIGINS = "http://localhost:8080";

    private final boolean development;
    private final String jwkKeys;
    private final String mfaKey;
    private final String rpId;
    private final String origins;

    public ProductionSettings(Config config,
            @Value("${cn1.security.authorizationserver.jwk.keys:}") String jwkKeys,
            @Value("${cn1.security.mfa.encryptionKey:}") String mfaKey,
            @Value("${bastion.webauthn.rp-id:localhost}") String rpId,
            @Value("${bastion.webauthn.origins:http://localhost:8080}") String origins) {
        this.development = config.isDevelopmentProfile();
        this.jwkKeys = jwkKeys;
        this.mfaKey = mfaKey;
        this.rpId = rpId;
        this.origins = origins;
    }

    @PostConstruct
    public void check() {
        check(development, jwkKeys, mfaKey, rpId, origins);
    }

    /// Throws naming the first setting left at its shipped value, unless
    /// `development`.
    public static void check(boolean development, String jwkKeys, String mfaKey, String rpId,
            String origins) {
        if (development) {
            return;
        }
        if (blank(jwkKeys)) {
            refuse("cn1.security.authorizationserver.jwk.keys");
        }
        if (blank(mfaKey)) {
            refuse("cn1.security.mfa.encryptionKey");
        }
        if (blank(rpId) || DEFAULT_RP_ID.equals(rpId.trim())) {
            refuse("bastion.webauthn.rp-id");
        }
        if (blank(origins) || DEFAULT_ORIGINS.equals(origins.trim())) {
            refuse("bastion.webauthn.origins");
        }
    }

    private static boolean blank(String s) {
        return s == null || s.trim().length() == 0;
    }

    private static void refuse(String key) {
        throw new IllegalStateException("Set " + key + " for this deployment: outside the dev "
                + "and test profiles Bastion does not start on the value it ships with.");
    }
}
