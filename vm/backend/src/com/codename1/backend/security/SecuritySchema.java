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
package com.codename1.backend.security;

import com.codename1.backend.Database;
import com.codename1.backend.sql.Dialect;
import com.codename1.migration.JavaMigration;
import com.codename1.migration.MigrationContext;
import com.codename1.migration.MigrationSet;
import java.io.IOException;

/// The tables the security layer's database-backed stores keep, as a migration
/// set of the layer's own.
///
/// Nothing registers it unless the application asks, and an application that
/// does not ask carries none of it. To ask, set
///
/// ```
/// cn1.security.schema.enabled=true
/// ```
///
/// in `application.properties` -- the build reads it there and registers the
/// set in the server's entry point -- or, in a server assembled by hand and in
/// a test, call
///
/// ```java
/// Migrations.register(SecuritySchema.migrations());
/// ```
///
/// before the server starts.
///
/// The setting is the build's to read. A server that finds it true anywhere
/// at run time -- the environment, a system property, a properties file beside
/// it -- without the set having been registered does not start, and says where
/// the setting belongs. Found false at run time, in a server built with it,
/// the set is not applied: the tables are then whatever the database has.
///
/// The set is named `security`, keeps its history in
/// `cn1_security_schema_history`, and runs before the application's own
/// migrations, so those may refer to its tables.
///
/// Every table is named `cn1_...`, and the same on SQLite, PostgreSQL and
/// MySQL:
///
/// | Version | Tables | Used by |
/// |---|---|---|
/// | 1 | `cn1_users`, `cn1_authorities` | [com.codename1.backend.security.core.userdetails.JdbcUserDetailsManager] |
/// | 2 | `cn1_api_key` | [com.codename1.backend.security.apikey.JdbcApiKeyRepository] |
/// | 3 | `cn1_persistent_logins` | [com.codename1.backend.security.rememberme.JdbcTokenRepository] |
/// | 4 | `cn1_mfa_totp`, `cn1_mfa_recovery_code` | [com.codename1.backend.security.mfa.JdbcTotpRepository], [com.codename1.backend.security.mfa.JdbcRecoveryCodeRepository] |
/// | 5 | `cn1_rate_limit` | [com.codename1.backend.security.ratelimit.JdbcRateLimiter] |
/// | 6 | `cn1_federated_identity` | [com.codename1.backend.security.oauth2.client.JdbcFederatedIdentityRepository] |
/// | 7 | `cn1_oauth2_registered_client` | [com.codename1.backend.security.oauth2.server.authorization.JdbcRegisteredClientRepository] |
/// | 8 | `cn1_oauth2_authorization`, `cn1_oauth2_token` | [com.codename1.backend.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService] |
/// | 9 | `cn1_webauthn_user`, `cn1_webauthn_credential` | [com.codename1.backend.security.webauthn.JdbcPublicKeyCredentialUserEntityRepository], [com.codename1.backend.security.webauthn.JdbcUserCredentialRepository] |
///
/// A user name is kept twice: as it was given, and folded to lower case in the
/// `username_key` column every table is keyed by, which is what makes a lookup
/// ignore case the same way on all three engines. Only `A` to `Z` are folded;
/// see [#usernameKey].
///
/// A moment is epoch milliseconds in a 64-bit integer and a truth value is 0 or
/// 1, as everywhere in the backend's own schemas.
public final class SecuritySchema {
    /// The name of the set.
    public static final String NAME = "security";
    /// The setting that makes the build register the set.
    public static final String ENABLED = "cn1.security.schema.enabled";

    // The versions of this set. A version is never edited once released: a new
    // table, or a change to one, is a new version appended in migrations().
    // The next takes 10. Each version is one method below that returns the
    // statements for an engine, a case in Tables.migrate, and a line in
    // migrations().

    private SecuritySchema() {
    }

    /// What each version adds, in version order: the description its history row carries.
    private static final String[] DESCRIPTIONS = {
        "users and authorities", "api keys", "persistent logins", "second factors",
        "rate limits", "federated identities", "oauth2 registered clients",
        "oauth2 authorizations", "passkeys"
    };

    /// The set, for `Migrations.register`.
    public static MigrationSet migrations() {
        MigrationSet.Builder set = MigrationSet.builder(NAME);
        for (int i = 0; i < DESCRIPTIONS.length; i++) {
            set.java(String.valueOf(i + 1), DESCRIPTIONS[i], scriptName(i + 1), new Tables(i + 1));
        }
        return set.build();
    }

    /// The name a version is recorded under in the history, and printed as when a run
    /// applies it: `com.codename1.backend.security.SecuritySchema.V4__second_factors`.
    ///
    /// One class provides every version, and its name alone -- what a history written
    /// before the versions were named holds, `...SecuritySchema.Tables` in all nine rows
    /// -- says nothing about which was applied. Such a history still validates and
    /// migrates: the script name is for people, and nothing compares it.
    /// @param version a version of the set, from 1
    static String scriptName(int version) {
        return "com.codename1.backend.security.SecuritySchema.V" + version + "__"
                + DESCRIPTIONS[version - 1].replace(' ', '_');
    }

    /// One version of the set. Written in Java rather than as a script because
    /// what a key column is called is the connected engine's to say -- MySQL and
    /// MariaDB, which a script cannot tell apart, do not even share a collation
    /// that compares text byte for byte -- and [Dialect] is where that is known.
    private static final class Tables implements JavaMigration {
        private final int version;

        Tables(int version) {
            this.version = version;
        }

        @Override
        public void migrate(MigrationContext context) throws IOException {
            Object connection = context.connection();
            if (!(connection instanceof Database)) {
                throw new IOException("The security schema is the server's: it cannot be "
                        + "applied to " + connection);
            }
            Dialect d = ((Database) connection).dialect();
            String[] statements;
            switch (version) {
                case 1: statements = users(d); break;
                case 2: statements = apiKeys(d); break;
                case 3: statements = persistentLogins(d); break;
                case 4: statements = secondFactors(d); break;
                case 5: statements = rateLimits(d); break;
                case 6: statements = federatedIdentities(d); break;
                case 7: statements = registeredClients(d); break;
                case 8: statements = authorizations(d); break;
                case 9: statements = passkeys(d); break;
                default: throw new IOException("The security schema has no version " + version);
            }
            for (String statement : statements) {
                context.execute(statement, null);
            }
        }
    }

    /// A user name as the tables are keyed by it: `A` to `Z` folded to lower
    /// case, and nothing else changed. Folded by hand because the platform's
    /// own fold follows the server's locale, and a key must not.
    public static String usernameKey(String username) {
        if (username == null) {
            return null;
        }
        char[] chars = username.toCharArray();
        boolean changed = false;
        for (int iter = 0 ; iter < chars.length ; iter++) {
            char c = chars[iter];
            if (c >= 'A' && c <= 'Z') {
                chars[iter] = (char) (c + ('a' - 'A'));
                changed = true;
            }
        }
        return changed ? new String(chars) : username;
    }

    /// The type of a text column that is a key or part of one: bounded and
    /// compared byte for byte on MySQL, where plain text can be neither.
    private static String key(Dialect d) {
        String column = d.assignedKeyColumn(Dialect.TEXT);
        String suffix = " NOT NULL PRIMARY KEY";
        if (!column.endsWith(suffix)) {
            throw new IllegalStateException("Unexpected key column for " + d.getName() + ": "
                    + column);
        }
        return column.substring(0, column.length() - suffix.length());
    }

    private static String text(Dialect d) {
        return d.columnType(Dialect.TEXT) + " NOT NULL";
    }

    private static String flag(Dialect d) {
        return d.columnType(Dialect.BOOLEAN) + " NOT NULL";
    }

    private static String moment(Dialect d) {
        return d.columnType(Dialect.BIGINT) + " NOT NULL";
    }

    private static String[] users(Dialect d) {
        return new String[] {
            "CREATE TABLE cn1_users (username_key " + key(d) + " NOT NULL PRIMARY KEY, "
                + "username " + text(d) + ", password " + text(d) + ", enabled " + flag(d)
                + ", account_non_expired " + flag(d) + ", account_non_locked " + flag(d)
                + ", credentials_non_expired " + flag(d) + ")",
            "CREATE TABLE cn1_authorities (username_key " + key(d) + " NOT NULL, "
                + "authority " + key(d) + " NOT NULL, PRIMARY KEY (username_key, authority))"};
    }

    private static String[] apiKeys(Dialect d) {
        return new String[] {
            "CREATE TABLE cn1_api_key (key_hash " + key(d) + " NOT NULL PRIMARY KEY, "
                + "id " + key(d) + " NOT NULL, owner " + key(d) + " NOT NULL, scopes " + text(d)
                + ", prefix " + text(d) + ", last_four " + text(d) + ", revoked " + flag(d)
                + ", created_at " + moment(d) + ")",
            "CREATE UNIQUE INDEX cn1_api_key_id ON cn1_api_key (id)",
            "CREATE INDEX cn1_api_key_owner ON cn1_api_key (owner)"};
    }

    private static String[] persistentLogins(Dialect d) {
        return new String[] {
            "CREATE TABLE cn1_persistent_logins (series " + key(d) + " NOT NULL PRIMARY KEY, "
                + "username_key " + key(d) + " NOT NULL, username " + text(d) + ", token_hash "
                + text(d) + ", last_used " + moment(d) + ")",
            "CREATE INDEX cn1_persistent_logins_user ON cn1_persistent_logins (username_key)"};
    }

    private static String[] secondFactors(Dialect d) {
        return new String[] {
            "CREATE TABLE cn1_mfa_totp (username_key " + key(d) + " NOT NULL PRIMARY KEY, "
                + "secret " + text(d) + ", nonce " + text(d) + ", confirmed " + flag(d)
                + ", last_used_step " + moment(d) + ", created_at " + moment(d) + ")",
            "CREATE TABLE cn1_mfa_recovery_code (username_key " + key(d) + " NOT NULL, "
                + "code_hash " + key(d) + " NOT NULL, PRIMARY KEY (username_key, code_hash))"};
    }

    private static String[] rateLimits(Dialect d) {
        return new String[] {
            "CREATE TABLE cn1_rate_limit (limit_key " + key(d) + " NOT NULL PRIMARY KEY, "
                + "window_start " + moment(d) + ", hits " + d.columnType(Dialect.INTEGER)
                + " NOT NULL)"};
    }

    private static String[] federatedIdentities(Dialect d) {
        return new String[] {
            "CREATE TABLE cn1_federated_identity (provider " + key(d) + " NOT NULL, subject "
                + key(d) + " NOT NULL, username_key " + key(d) + " NOT NULL, username " + text(d)
                + ", created_at " + moment(d) + ", PRIMARY KEY (provider, subject))",
            "CREATE INDEX cn1_federated_identity_user ON cn1_federated_identity (username_key)"};
    }

    private static String[] registeredClients(Dialect d) {
        return new String[] {
            "CREATE TABLE cn1_oauth2_registered_client (id " + key(d) + " NOT NULL PRIMARY KEY, "
                + "client_id " + key(d) + " NOT NULL, client_secret " + text(d)
                + ", client_name " + text(d) + ", authentication_methods " + text(d)
                + ", grant_types " + text(d) + ", redirect_uris " + text(d) + ", scopes "
                + text(d) + ", settings " + text(d) + ", created_at " + moment(d) + ")",
            "CREATE UNIQUE INDEX cn1_oauth2_registered_client_cid ON "
                + "cn1_oauth2_registered_client (client_id)"};
    }

    /// A grant, and beside it the secrets issued under it -- the authorization
    /// code, the refresh tokens, the device and user codes -- each as one row
    /// keyed by its SHA-256, so that using one up is one statement on one row.
    private static String[] authorizations(Dialect d) {
        return new String[] {
            "CREATE TABLE cn1_oauth2_authorization (id " + key(d) + " NOT NULL PRIMARY KEY, "
                + "registered_client_id " + key(d) + " NOT NULL, principal_name " + text(d)
                + ", grant_type " + text(d) + ", scopes " + text(d) + ", status " + key(d)
                + " NOT NULL, attributes " + text(d) + ", created_at " + moment(d)
                + ", expires_at " + moment(d) + ")",
            "CREATE INDEX cn1_oauth2_authorization_exp ON cn1_oauth2_authorization (expires_at)",
            "CREATE TABLE cn1_oauth2_token (token_hash " + key(d) + " NOT NULL PRIMARY KEY, "
                + "authorization_id " + key(d) + " NOT NULL, kind " + key(d) + " NOT NULL, used "
                + flag(d) + ", expires_at " + moment(d) + ", polled_at " + moment(d) + ")",
            "CREATE INDEX cn1_oauth2_token_auth ON cn1_oauth2_token (authorization_id)",
            "CREATE INDEX cn1_oauth2_token_exp ON cn1_oauth2_token (expires_at)"};
    }

    /// A user's passkey handle, and their credentials. Byte strings -- the
    /// handle, a credential's id, its public key -- are base64url text, so
    /// that an id is a key that compares byte for byte on every engine; an id
    /// is at most 1023 bytes, which is more text than MySQL indexes whole, so
    /// the table is keyed by its SHA-256 instead and the id is a column.
    private static String[] passkeys(Dialect d) {
        return new String[] {
            "CREATE TABLE cn1_webauthn_user (username_key " + key(d) + " NOT NULL PRIMARY KEY, "
                + "username " + text(d) + ", user_id " + key(d) + " NOT NULL, display_name "
                + text(d) + ", created_at " + moment(d) + ")",
            "CREATE UNIQUE INDEX cn1_webauthn_user_id ON cn1_webauthn_user (user_id)",
            "CREATE TABLE cn1_webauthn_credential (credential_key " + key(d)
                + " NOT NULL PRIMARY KEY, credential_id " + text(d) + ", user_id " + key(d)
                + " NOT NULL, algorithm " + moment(d) + ", public_key " + text(d)
                + ", sign_count " + moment(d) + ", uv_initialized " + flag(d)
                + ", backup_eligible " + flag(d) + ", backup_state " + flag(d) + ", transports "
                + text(d) + ", label " + text(d) + ", created_at " + moment(d) + ", last_used "
                + moment(d) + ")",
            "CREATE INDEX cn1_webauthn_credential_user ON cn1_webauthn_credential (user_id)"};
    }
}
