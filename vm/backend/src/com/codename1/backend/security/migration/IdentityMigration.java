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
package com.codename1.backend.security.migration;

import com.codename1.backend.Base64Url;
import com.codename1.backend.Crypto;
import com.codename1.backend.security.core.userdetails.User;
import com.codename1.backend.security.core.userdetails.UserDetailsManager;
import com.codename1.backend.security.crypto.DelegatingPasswordEncoder;
import com.codename1.backend.security.mfa.TotpRepository;
import com.codename1.backend.security.oauth2.client.FederatedIdentityRepository;
import com.codename1.backend.security.webauthn.CoseKey;
import com.codename1.backend.security.webauthn.CredentialRecord;
import com.codename1.backend.security.webauthn.PublicKeyCredentialUserEntity;
import com.codename1.backend.security.webauthn.PublicKeyCredentialUserEntityRepository;
import com.codename1.backend.security.webauthn.UserCredentialRepository;
import com.codename1.backend.security.webauthn.WebAuthnException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/// Moves the accounts of another identity store into this server's stores,
/// keeping everything a user signs in with, and reports every record.
///
/// The application reads its old store -- a SQL query, a JSON export, an LDAP
/// dump -- and hands the rows over; this class decides what each one becomes:
///
/// ```java
/// IdentityMigration migration = new IdentityMigration(users, passwords);
/// migration.setPasswordPrefix("django");     // the old store's rows carry no {id}
/// migration.setFederatedIdentities(links);
/// migration.setPasskeys(userEntities, credentials);
/// for (OldRow row : oldRows) {
///     migration.user(row.username, row.passwordHash, row.roles, row.enabled);
///     if (row.googleSub != null) {
///         migration.federatedIdentity("google", row.googleSub, row.username);
///     }
/// }
/// MigrationReport report = migration.finish();
/// ```
///
/// What it guarantees:
///
/// - **Usernames, subjects and authorities are written verbatim.** An issued
///   token's `sub`, a federated link and a passkey's user handle are what
///   other systems hold on to, so none of them is normalised or regenerated.
/// - **A password hash is kept only when it can be verified.** One whose
///   `{id}` the encoder does not know is not imported as a hash nobody can
///   check: the user is created with a random password nobody knows and the
///   report says a reset is needed. A kept hash is re-encoded with the default
///   on its owner's next sign in, through the existing rehash-on-login path.
/// - **Nothing existing is overwritten.** A user, a link, a user handle or a
///   credential id already present is reported as skipped. Run it again after
///   fixing the rows it skipped; what it already wrote is skipped again.
/// - **A dry run writes nothing** ([#setDryRun(boolean)]) and produces the same
///   report, apart from conflicts between two rows of the same run.
///
/// What it never carries is listed in the report too, as
/// [MigrationReport.Outcome#NOT_CARRIED]: sessions, remember-me tokens,
/// authorization codes and refresh tokens issued by the old server, and
/// recovery codes. Each is either bound to the old server's secret or stored
/// only as a hash the old server chose, and a user signs in again to get new
/// ones. Signing keys are configuration, not records: see
/// `AuthorizationServerKeys.KEY_IDS` and `VERIFICATION_KEYS`.
///
/// Not synchronized: a migration is one pass by one thread.
public final class IdentityMigration {
    private final UserDetailsManager users;
    private final DelegatingPasswordEncoder passwords;
    private FederatedIdentityRepository federated;
    private PublicKeyCredentialUserEntityRepository userEntities;
    private UserCredentialRepository credentials;
    private TotpRepository totp;
    private String passwordPrefix;
    private boolean dryRun;
    private MigrationReport report;
    private final Set<String> seenUsers = new HashSet<String>();
    private final Set<String> seenCredentials = new HashSet<String>();

    /// @param users where the accounts are created
    /// @param passwords the encoder the server verifies with; decides which
    ///        imported hashes can be kept
    public IdentityMigration(UserDetailsManager users, DelegatingPasswordEncoder passwords) {
        if (users == null || passwords == null) {
            throw new IllegalArgumentException("A user store and a password encoder are required");
        }
        this.users = users;
        this.passwords = passwords;
    }

    /// The `{id}` to put in front of a hash that has none, such as `bcrypt` for
    /// a store of bare `$2a$` values. A hash that already starts with `{` is
    /// left alone. Null (the default) adds nothing.
    public void setPasswordPrefix(String id) {
        this.passwordPrefix = id;
    }

    public void setFederatedIdentities(FederatedIdentityRepository repository) {
        this.federated = repository;
    }

    public void setPasskeys(PublicKeyCredentialUserEntityRepository userEntities,
                            UserCredentialRepository credentials) {
        this.userEntities = userEntities;
        this.credentials = credentials;
    }

    public void setTotp(TotpRepository repository) {
        this.totp = repository;
    }

    /// True to write nothing and only report. Set it before the first record.
    public void setDryRun(boolean dryRun) {
        if (report != null) {
            throw new IllegalStateException("Set the dry run before the first record");
        }
        this.dryRun = dryRun;
    }

    private MigrationReport report() {
        if (report == null) {
            report = new MigrationReport(dryRun);
        }
        return report;
    }

    /// Imports one account.
    ///
    /// @param username kept verbatim
    /// @param passwordHash the stored hash, with or without its `{id}`; null or
    ///        empty for an account that never had a password (federated only)
    /// @param authorities kept verbatim, `ROLE_` prefixes included
    /// @param enabled false keeps a disabled account disabled
    public void user(String username, String passwordHash, Collection<String> authorities,
                     boolean enabled) {
        MigrationReport r = report();
        if (username == null || username.length() == 0) {
            r.add("user", "(no username)", MigrationReport.Outcome.SKIPPED,
                    "the row has no username");
            return;
        }
        if (!seenUsers.add(username) || users.userExists(username)) {
            r.add("user", username, MigrationReport.Outcome.SKIPPED, "the user already exists");
            return;
        }
        String reason = null;
        String stored;
        if (passwordHash == null || passwordHash.length() == 0) {
            stored = unusablePassword();
            reason = "no password was stored; the user signs in another way or sets one";
        } else {
            stored = passwordPrefix != null && !passwordHash.startsWith("{")
                    ? "{" + passwordPrefix + "}" + passwordHash : passwordHash;
            if (!passwords.isSupported(stored)) {
                stored = unusablePassword();
                reason = "the password hash is in a format this server cannot verify; the "
                        + "user must reset the password";
            }
        }
        String[] roles = authorities == null ? new String[0]
                : authorities.toArray(new String[authorities.size()]);
        if (!dryRun) {
            users.createUser(User.withUsername(username).password(stored).authorities(roles)
                    .disabled(!enabled).build());
        }
        r.add("user", username, reason == null ? MigrationReport.Outcome.MIGRATED
                : MigrationReport.Outcome.NEEDS_ACTION, reason);
    }

    /// Links a sign-in provider's subject to an imported user, verbatim.
    public void federatedIdentity(String provider, String subject, String username) {
        MigrationReport r = report();
        String id = provider + "/" + subject;
        if (federated == null) {
            throw new IllegalStateException("setFederatedIdentities was not called");
        }
        if (provider == null || subject == null || username == null) {
            r.add("federated-identity", id, MigrationReport.Outcome.SKIPPED,
                    "the row lacks a provider, a subject or a username");
            return;
        }
        if (!userKnown(username)) {
            r.add("federated-identity", id, MigrationReport.Outcome.SKIPPED,
                    "no user " + username);
            return;
        }
        String existing = federated.findUsername(provider, subject);
        if (existing != null) {
            r.add("federated-identity", id, MigrationReport.Outcome.SKIPPED, existing.equals(username)
                    ? "already linked" : "already linked to another user");
            return;
        }
        if (!dryRun) {
            federated.link(provider, subject, username);
        }
        r.add("federated-identity", id, MigrationReport.Outcome.MIGRATED, null);
    }

    /// Imports a passkey, keeping its credential id and user handle.
    public void passkey(LegacyPasskey passkey) {
        MigrationReport r = report();
        if (credentials == null || userEntities == null) {
            throw new IllegalStateException("setPasskeys was not called");
        }
        byte[] credentialId = passkey.getCredentialId();
        String id = credentialId == null ? "(no credential id)" : Base64Url.encode(credentialId);
        String username = passkey.getUsername();
        byte[] handle = passkey.getUserHandle();
        if (credentialId == null || credentialId.length == 0 || handle == null
                || handle.length == 0 || username == null) {
            r.add("passkey", id, MigrationReport.Outcome.SKIPPED,
                    "the row lacks a credential id, a user handle or a username");
            return;
        }
        if (!userKnown(username)) {
            r.add("passkey", id, MigrationReport.Outcome.SKIPPED, "no user " + username);
            return;
        }
        CoseKey key;
        try {
            key = CoseKey.parse(passkey.getCoseKey());
        } catch (WebAuthnException err) {
            r.add("passkey", id, MigrationReport.Outcome.NEEDS_ACTION, "the public key cannot be "
                    + "used (" + err.getMessage() + "); the user must register a passkey again");
            return;
        } catch (RuntimeException err) {
            r.add("passkey", id, MigrationReport.Outcome.NEEDS_ACTION, "the public key is not "
                    + "valid CBOR; the user must register a passkey again");
            return;
        }
        if (!seenCredentials.add(id) || credentials.findByCredentialId(credentialId) != null) {
            r.add("passkey", id, MigrationReport.Outcome.SKIPPED, "the credential id already exists");
            return;
        }
        PublicKeyCredentialUserEntity byName = userEntities.findByUsername(username);
        if (byName != null && !Arrays.equals(byName.getId(), handle)) {
            r.add("passkey", id, MigrationReport.Outcome.SKIPPED, "user " + username
                    + " already has another user handle; a passkey answers with the handle it "
                    + "was registered with");
            return;
        }
        PublicKeyCredentialUserEntity byHandle = userEntities.findById(handle);
        if (byHandle != null && !username.equals(byHandle.getName())) {
            r.add("passkey", id, MigrationReport.Outcome.SKIPPED,
                    "the user handle belongs to another user");
            return;
        }
        if (!dryRun) {
            if (byName == null) {
                userEntities.save(new PublicKeyCredentialUserEntity(username, handle, username));
            }
            long now = System.currentTimeMillis();
            credentials.save(CredentialRecord.builder().credentialId(credentialId)
                    .userEntityUserId(handle).algorithm(key.getAlgorithm())
                    .publicKey(key.getPublicKey()).signatureCount(passkey.getSignatureCount())
                    .uvInitialized(passkey.isUvInitialized())
                    .backupEligible(passkey.isBackupEligible())
                    .backupState(passkey.isBackupState()).transports(passkey.getTransports())
                    .label(passkey.getLabel()).created(now).lastUsed(now).build());
        }
        r.add("passkey", id, MigrationReport.Outcome.MIGRATED, null);
    }

    /// Imports a confirmed TOTP secret (the raw key, not its base32 text), so the
    /// user's authenticator app keeps working.
    public void totp(String username, byte[] secret) {
        MigrationReport r = report();
        if (totp == null) {
            throw new IllegalStateException("setTotp was not called");
        }
        if (username == null || !userKnown(username)) {
            r.add("totp", String.valueOf(username), MigrationReport.Outcome.SKIPPED,
                    "no user " + username);
            return;
        }
        if (secret == null || secret.length < 10) {
            r.add("totp", username, MigrationReport.Outcome.NEEDS_ACTION,
                    "the secret is shorter than 80 bits; the user must enrol again");
            return;
        }
        if (totp.find(username) != null) {
            r.add("totp", username, MigrationReport.Outcome.SKIPPED, "the user already has TOTP");
            return;
        }
        if (!dryRun) {
            totp.save(username, secret);
            totp.confirm(username);
        }
        r.add("totp", username, MigrationReport.Outcome.MIGRATED, null);
    }

    /// Closes the run: adds the state no migration carries and returns the report.
    public MigrationReport finish() {
        MigrationReport r = report();
        r.add("sessions", "*", MigrationReport.Outcome.NOT_CARRIED,
                "bound to the old server; every user signs in once");
        r.add("remember-me", "*", MigrationReport.Outcome.NOT_CARRIED,
                "the old server's tokens are hashed or signed with its own secret");
        r.add("authorization-codes", "*", MigrationReport.Outcome.NOT_CARRIED,
                "single use and minutes long; a client in the middle of a sign in starts again");
        r.add("refresh-tokens", "*", MigrationReport.Outcome.NOT_CARRIED,
                "issued by the old server; its clients sign the user in again");
        r.add("recovery-codes", "*", MigrationReport.Outcome.NOT_CARRIED,
                "stored as hashes of the old server's choosing; users generate new ones");
        report = null;
        return r;
    }

    private boolean userKnown(String username) {
        return seenUsers.contains(username) || users.userExists(username);
    }

    /// A hash of 32 random bytes nobody ever saw: a valid value no password matches.
    private String unusablePassword() {
        try {
            return passwords.encode(Base64Url.encode(Crypto.randomBytes(32)));
        } catch (IOException err) {
            throw new IllegalStateException("Could not generate a password: " + err.getMessage(), err);
        }
    }
}
