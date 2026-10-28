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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.codename1.backend.Json;
import com.codename1.backend.security.core.userdetails.InMemoryUserDetailsManager;
import com.codename1.backend.security.crypto.DelegatingPasswordEncoder;
import com.codename1.backend.security.crypto.PasswordEncoder;
import com.codename1.backend.security.crypto.Pbkdf2Sha256PasswordEncoder;
import com.codename1.backend.security.migration.IdentityMigration;
import com.codename1.backend.security.migration.LegacyPasskey;
import com.codename1.backend.security.migration.MigrationReport;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/// A passkey registered with another server signs in here once imported: the
/// authenticator keeps its key, its credential id and the user handle it answers with.
class PasskeyMigrationTest {
    @Test
    @DisplayName("Imported passkeys answer a challenge from this server; broken ones are reported")
    void importedPasskeysSignIn() throws Exception {
        InMemoryUserDetailsManager users = new InMemoryUserDetailsManager();
        InMemoryPublicKeyCredentialUserEntityRepository entities =
                new InMemoryPublicKeyCredentialUserEntityRepository();
        InMemoryUserCredentialRepository credentials = new InMemoryUserCredentialRepository();
        IdentityMigration migration = new IdentityMigration(users, new DelegatingPasswordEncoder(
                "pbkdf2-sha256", Collections.<String, PasswordEncoder>singletonMap("pbkdf2-sha256",
                        new Pbkdf2Sha256PasswordEncoder())));
        migration.setPasskeys(entities, credentials);
        migration.user("ada", null, Arrays.asList("ROLE_USER"), true);

        // The authenticators as the old server saw them: a key, an id, a handle, a count.
        byte[] handle = SoftAuthenticator.random(16);
        SoftAuthenticator ec = new SoftAuthenticator(false, "example.org", "https://example.org");
        SoftAuthenticator rsa = new SoftAuthenticator(true, "example.org", "https://example.org");
        for (SoftAuthenticator a : new SoftAuthenticator[] {ec, rsa}) {
            a.userHandle = handle;
            a.counter = 41;
            migration.passkey(new LegacyPasskey("ada", handle, a.credentialId,
                    SoftAuthenticator.cbor(a.coseKey(), false)).signatureCount(41)
                    .transports(a.transports).label("old"));
        }
        migration.passkey(new LegacyPasskey("ada", handle, ec.credentialId,
                SoftAuthenticator.cbor(ec.coseKey(), false)));
        migration.passkey(new LegacyPasskey("ada", SoftAuthenticator.random(16),
                SoftAuthenticator.random(32), SoftAuthenticator.cbor(ec.coseKey(), false)));
        migration.passkey(new LegacyPasskey("ada", handle, SoftAuthenticator.random(32),
                new byte[] {1, 2, 3}));
        migration.passkey(new LegacyPasskey("nobody", handle, SoftAuthenticator.random(32),
                SoftAuthenticator.cbor(ec.coseKey(), false)));
        MigrationReport report = migration.finish();
        // ada (no password), the corrupt key -> action; duplicate, other handle, nobody -> skipped
        assertEquals(2, report.count(MigrationReport.Outcome.MIGRATED), report.toString());
        assertEquals(2, report.count(MigrationReport.Outcome.NEEDS_ACTION), report.toString());
        assertEquals(3, report.count(MigrationReport.Outcome.SKIPPED), report.toString());

        WebAuthnRelyingPartyOperations ops = new WebAuthnRelyingPartyOperations(
                new PublicKeyCredentialRpEntity("example.org", "Example"),
                Arrays.asList("https://example.org"), entities, credentials);
        for (SoftAuthenticator a : new SoftAuthenticator[] {ec, rsa}) {
            PublicKeyCredentialRequestOptions request = ops.createCredentialRequestOptions(null);
            Map<String, Object> answer = Json.parseObject(Json.write(a.get(request.toMap())));
            assertEquals("ada", ops.authenticate(request, answer).getUser().getName());
            assertEquals(42, credentials.findByCredentialId(a.credentialId).getSignatureCount());
        }
        assertArrayEquals(handle, entities.findByUsername("ada").getId());
    }
}
