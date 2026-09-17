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
package com.codenameone.developerguide.snippets;

import com.codename1.db.Database;
import com.codename1.db.DatabaseConfig;
import com.codename1.io.Log;
import com.codename1.security.vault.Protection;
import com.codename1.security.vault.ProtectionReport;
import com.codename1.security.vault.UnlockPolicy;
import com.codename1.security.vault.Vault;
import com.codename1.security.vault.VaultError;
import com.codename1.security.vault.VaultException;
import com.codename1.security.vault.VaultOptions;

import java.io.IOException;

/** Compiled source snippets for the vault section of the Security guide chapter. */
public class VaultSnippets {

    /** First run: set the vault up, and let the user choose whether to be remembered. */
    public void enroll(char[] password, boolean rememberThisDevice) {
        // tag::vault-enroll[]
        Vault vault = Vault.named("notes");
        VaultOptions options = new VaultOptions()
                .policy(rememberThisDevice ? UnlockPolicy.REMEMBER_DEVICE
                        : UnlockPolicy.SESSION_ONLY)
                .autoLockAfter(5 * 60 * 1000);
        vault.configure(options);
        vault.enroll(password, options).ready(ready -> Log.p("vault ready"));
        // end::vault-enroll[]
    }

    /** Every later run: try the remembered key first, fall back to asking. */
    public void unlock(char[] password) {
        // tag::vault-unlock[]
        Vault vault = Vault.named("notes");
        vault.unlockRemembered()
                .except(notRemembered -> vault.unlockWithPassword(password)
                        .except(wrong -> Log.p("that password did not open the vault")));
        // end::vault-unlock[]
    }

    /** Storing and reading a secret. */
    public void secrets(Vault vault, char[] token) {
        // tag::vault-secret[]
        vault.putSecret("api.token", token);
        vault.getSecret("api.token").ready(value -> {
            useToken(value);
            // The caller owns the characters and can clear them; a String could not be.
            java.util.Arrays.fill(value, '\0');
        });
        // end::vault-secret[]
    }

    /** Sealing a record for a sync server that must not be able to read it. */
    public void sync(Vault vault, byte[] note) {
        // tag::vault-seal[]
        vault.seal("note-7", note).ready(sealed -> upload("note-7", sealed));
        // The vault record itself is safe to upload too: it carries the data key only in
        // wrapped form, so a server holding it can neither read the notes nor help anyone else.
        upload("vault", vault.exportSyncState());
        // end::vault-seal[]
    }

    /** Second device: enroll from the first device's state, then read its records. */
    public void enrollFromSync(byte[] syncState, char[] password, byte[] sealedNote) {
        // tag::vault-second-device[]
        Vault vault = Vault.named("notes");
        vault.importSyncState(syncState, password)
                .ready(ok -> vault.open("note-7", sealedNote).ready(this::showNote))
                .except(failure -> Log.p("could not enroll this device: " + failure.getMessage()));
        // end::vault-second-device[]
    }

    /** Choosing whether the passkey may sync to the user's other devices. */
    public void deviceBoundPasskey(char[] password, boolean mustNotLeaveThisDevice) {
        // tag::vault-device-bound[]
        VaultOptions options = new VaultOptions()
                .policy(UnlockPolicy.REQUIRE_USER_VERIFICATION);
        if (mustNotLeaveThisDevice) {
            // Excludes security keys and every synced passkey. Enrolment fails with
            // POLICY_NOT_MET where no authenticator can meet it, so have a fallback.
            options.requireDeviceBoundPasskey();
        }
        Vault vault = Vault.named("payments").configure(options);
        vault.enroll(password, options).except(failure -> offerPasswordOnly());
        // end::vault-device-bound[]
    }

    /** Refusing rather than silently storing with less protection than was asked for. */
    public void requireProtection(char[] password) {
        // tag::vault-require[]
        VaultOptions options = new VaultOptions()
                .policy(UnlockPolicy.REMEMBER_DEVICE)
                .require(Protection.OS_PROTECTED);
        Vault vault = Vault.named("payments").configure(options);
        vault.enroll(password, options).except(failure -> {
            if (failure instanceof VaultException
                    && ((VaultException) failure).getError() == VaultError.POLICY_NOT_MET) {
                // The browser reaches this: there is no OS key store in a page. Offer the
                // session-only policy instead of pretending the requirement was met.
                Log.p("this device cannot hold the key in the OS key store");
            }
        });
        // end::vault-require[]
    }

    /** Describing the protection to the user without overstating it. */
    public String describeProtection(Vault vault) {
        // tag::vault-report[]
        ProtectionReport report = vault.protection();
        if (report.answer(Protection.HARDWARE_BACKED) == ProtectionReport.UNKNOWN) {
            // Not "no". This platform will not say, and a screen that rendered that as "no"
            // would understate an authenticator that does use a secure element.
            return "Encrypted on this device; hardware backing could not be confirmed.";
        }
        return report.provides(Protection.HARDWARE_BACKED)
                ? "Encrypted with a hardware-backed key."
                : "Encrypted with a software key.";
        // end::vault-report[]
    }

    /** An encrypted database keyed from the vault. */
    public Database openDatabase(Vault vault) throws IOException {
        // tag::vault-database[]
        // The vault must be unlocked here: the key does not exist until it is. Locking the
        // vault afterwards does not close the connection -- the engine already has the key.
        return Database.openOrCreate("notes", DatabaseConfig.vault(vault, "notes"));
        // end::vault-database[]
    }

    /** A per-feature key that never becomes bytes the application can read. */
    public void operationalKey(Vault vault, byte[] payload) {
        // tag::vault-handle[]
        vault.operationalKey("cache").ready(key -> {
            // There is no getEncoded() on a KeyHandle, and no method that adds one.
            key.seal(payload, null).ready(sealed -> storeInCache(sealed));
        });
        // end::vault-handle[]
    }

    private void useToken(char[] token) {
    }

    private void upload(String name, byte[] payload) {
    }

    private void showNote(byte[] note) {
    }

    private void storeInCache(byte[] sealed) {
    }

    private void offerPasswordOnly() {
    }
}
