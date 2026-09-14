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
package com.codename1.security.vault;

/// What a caller insists on when opening or enrolling a vault.
///
/// The reason this exists rather than a set of arguments: the important option is the one that
/// says **fail instead of doing something weaker**. A vault asked for encrypted, non-extractable
/// storage on a platform that can only offer a plaintext string has exactly two honest
/// behaviours, and silently taking the second is how an application ends up believing it is
/// protecting something it is not. Listing a protection in [#require(Protection)] turns that into
/// a [VaultError#POLICY_NOT_MET] naming the protection that was missing.
///
/// Nothing here is required. A `new VaultOptions()` carries the defaults an application that has
/// not thought about it should get: today's KDF profile, session-only access, no auto lock, and
/// no required protections -- which means the vault reports what it managed rather than
/// refusing.
public final class VaultOptions {

    private Protection[] required = new Protection[0];
    private KdfProfile kdf = KdfProfile.current();
    private UnlockPolicy policy = UnlockPolicy.SESSION_ONLY;
    private long autoLockMillis;
    private boolean opaqueKeysOnly;
    private com.codename1.security.vault.spi.DeviceProtection deviceProtection;

    /// Adds a protection the vault must provide, or the operation fails.
    ///
    /// [ProtectionReport#UNKNOWN] does not satisfy a requirement. A platform that cannot say
    /// whether a key is hardware backed has not provided hardware backing as far as a policy is
    /// concerned, and an application that needs the guarantee needs the refusal.
    public VaultOptions require(Protection protection) {
        if (protection == null) {
            return this;
        }
        Protection[] grown = new Protection[required.length + 1];
        System.arraycopy(required, 0, grown, 0, required.length);
        grown[required.length] = protection;
        required = grown;
        return this;
    }

    /// The protections required so far.
    public Protection[] getRequired() {
        Protection[] copy = new Protection[required.length];
        System.arraycopy(required, 0, copy, 0, required.length);
        return copy;
    }

    /// Overrides the password derivation profile. Rarely useful: the default is
    /// [KdfProfile#current()], and lowering it weakens every password wrap the vault writes.
    public VaultOptions kdf(KdfProfile profile) {
        if (profile != null) {
            kdf = profile;
        }
        return this;
    }

    /// The derivation profile new wraps will use.
    public KdfProfile getKdf() {
        return kdf;
    }

    /// How this vault may be reopened. See [UnlockPolicy].
    public VaultOptions policy(UnlockPolicy newPolicy) {
        if (newPolicy != null) {
            policy = newPolicy;
        }
        return this;
    }

    /// The unlock policy.
    public UnlockPolicy getPolicy() {
        return policy;
    }

    /// Locks the vault after this many milliseconds without a vault operation. Zero, the default,
    /// never auto locks.
    ///
    /// Enforced when the vault is next used rather than by a timer, so a vault that is idle stays
    /// nominally unlocked until something asks -- at which point it locks and the call fails with
    /// [VaultError#LOCKED]. A timer would be tidier and would also keep a reference alive and
    /// wake a sleeping device; the check-on-use form cannot be late in any way a caller can
    /// observe, because nothing observes the vault except through a call.
    public VaultOptions autoLockAfter(long millis) {
        autoLockMillis = millis > 0 ? millis : 0;
        return this;
    }

    /// The auto-lock idle timeout in milliseconds, or zero for none.
    public long getAutoLockMillis() {
        return autoLockMillis;
    }

    /// Refuses any operation that would produce raw key bytes, at the cost of the encrypted
    /// database.
    ///
    /// The one place this package hands out key material is [Vault#databaseKey(String)], because
    /// SQLCipher -- on the device and in the browser's WASM build alike -- takes a key as bytes
    /// and there is no arrangement of opaque handles that changes that. A caller that cannot
    /// accept the exposure sets this, and that call then fails with
    /// [VaultError#POLICY_NOT_MET] rather than quietly doing the thing the policy forbade.
    ///
    /// The consequence is real and is the point: a vault configured this way cannot key a managed
    /// encrypted database. An application that needs both has to seal its records through
    /// [Vault#seal] and store the ciphertext in a plain database, which is a different design
    /// rather than a setting.
    public VaultOptions requireOpaqueKeysOnly() {
        opaqueKeysOnly = true;
        return this;
    }

    /// Whether raw key material may be produced at all. See [#requireOpaqueKeysOnly()].
    public boolean isOpaqueKeysOnly() {
        return opaqueKeysOnly;
    }

    /// Supplies the device key store rather than using the port's.
    ///
    /// The port's is right for almost everything, and this exists for the cases where it is not:
    /// a library wrapping a hardware token, an application whose key belongs to a management
    /// agent rather than to the device, and a test that needs the remembered-device paths without
    /// a platform key store behind them.
    ///
    /// Supplying one does not relax any check. A protection required through
    /// [#require(Protection)] is still checked against what this reports, so an implementation
    /// that claims more than it provides fails the same way the port would.
    ///
    /// #### Parameters
    ///
    /// - `protection`: the device key store, or null to use the port's
    public VaultOptions deviceProtection(com.codename1.security.vault.spi.DeviceProtection protection) {
        deviceProtection = protection;
        return this;
    }

    /// The device key store supplied through [#deviceProtection], or null for the port's.
    public com.codename1.security.vault.spi.DeviceProtection getDeviceProtection() {
        return deviceProtection;
    }
}
