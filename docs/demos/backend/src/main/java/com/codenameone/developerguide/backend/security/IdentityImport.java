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
package com.codenameone.developerguide.backend.security;

import com.codename1.backend.security.core.userdetails.UserDetailsManager;
import com.codename1.backend.security.crypto.BCryptPasswordEncoder;
import com.codename1.backend.security.crypto.DelegatingPasswordEncoder;
import com.codename1.backend.security.crypto.ModularPbkdf2PasswordEncoder;
import com.codename1.backend.security.crypto.PasswordEncoder;
import com.codename1.backend.security.crypto.Pbkdf2Sha256PasswordEncoder;
import com.codename1.backend.security.migration.IdentityMigration;
import com.codename1.backend.security.migration.LegacyPasskey;
import com.codename1.backend.security.migration.MigrationReport;
import com.codename1.backend.security.oauth2.client.FederatedIdentityRepository;
import com.codename1.backend.security.webauthn.PublicKeyCredentialUserEntityRepository;
import com.codename1.backend.security.webauthn.UserCredentialRepository;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The Backend security chapter's migration examples. */
final class IdentityImport {
    private IdentityImport() {
    }

    /** One account as the old store exported it. */
    static final class OldUser {
        String email;
        String passwordHash;
        String roles;
        boolean active;
        String googleSubject;
        byte[] passkeyId;
        byte[] passkeyHandle;
        byte[] passkeyCoseKey;
        long passkeyCount;
    }

// tag::backend-security-migration-encoders[]
    static PasswordEncoder passwords() {
        Map<String, PasswordEncoder> encoders = new LinkedHashMap<>();
        encoders.put("pbkdf2-sha256", new Pbkdf2Sha256PasswordEncoder());
        encoders.put("bcrypt", new BCryptPasswordEncoder());
        encoders.put("django", new ModularPbkdf2PasswordEncoder(
                ModularPbkdf2PasswordEncoder.Format.DJANGO));
        return new DelegatingPasswordEncoder("pbkdf2-sha256", encoders);
    }
// end::backend-security-migration-encoders[]

// tag::backend-security-migration-run[]
    static MigrationReport migrate(List<OldUser> rows, UserDetailsManager users,
            DelegatingPasswordEncoder passwords, FederatedIdentityRepository links,
            PublicKeyCredentialUserEntityRepository handles,
            UserCredentialRepository credentials, boolean dryRun) {
        IdentityMigration migration = new IdentityMigration(users, passwords);
        migration.setDryRun(dryRun);
        migration.setPasswordPrefix("django");     // the old rows carry no {id}
        migration.setFederatedIdentities(links);
        migration.setPasskeys(handles, credentials);
        for (OldUser row : rows) {
            migration.user(row.email, row.passwordHash,
                    Arrays.asList(row.roles.split(",")), row.active);
            if (row.googleSubject != null) {
                migration.federatedIdentity("google", row.googleSubject, row.email);
            }
            if (row.passkeyId != null) {
                migration.passkey(new LegacyPasskey(row.email, row.passkeyHandle,
                        row.passkeyId, row.passkeyCoseKey).signatureCount(row.passkeyCount));
            }
        }
        MigrationReport report = migration.finish();
        System.out.println(report);   // every row that needed more than a copy
        return report;
    }
// end::backend-security-migration-run[]
}
