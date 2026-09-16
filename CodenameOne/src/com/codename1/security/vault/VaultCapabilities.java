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

import com.codename1.security.vault.spi.DeviceProtection;

/// What this device can do, asked before an application offers the user a choice.
///
/// The distinction this class draws is between **available** and **in use**. A platform can offer
/// a remembered device without this vault having one; [Vault#protection()] answers the second
/// question, this one answers the first. An application building a settings screen needs both: it
/// shows the switch based on this, and shows the switch's state based on that.
public final class VaultCapabilities {

    private final DeviceProtection device;

    /// Whether ordinary Storage and AES-GCM are both usable, measured directly rather than read
    /// off the device protection -- see Vault.canKeepAndSealARecord for why the report is the
    /// wrong source for it.
    private final boolean canKeepARecord;

    VaultCapabilities(DeviceProtection device, boolean canKeepARecord) {
        this.device = device;
        this.canKeepARecord = canKeepARecord;
    }

    /// Whether an unlock policy can be honoured on this device.
    ///
    /// [UnlockPolicy#SESSION_ONLY] needs no key STORAGE, because it stores no key -- but it is
    /// not free of platform prerequisites, and saying it was unconditionally supported was
    /// wrong. Enrolling under it still persists a metadata record and still seals it with
    /// AES-GCM, so a browser with no usable IndexedDB or Web Crypto -- a restricted private
    /// window -- advertised a policy that could only fail, and fail after the user had already
    /// typed a password.
    ///
    /// Those prerequisites are measured DIRECTLY -- can Storage answer for an entry, and does a
    /// small AES-GCM seal succeed -- and deliberately not read off the device protection's
    /// report. That was the first attempt and it was wrong in both directions: a device store is
    /// not what session-only uses, so an application-supplied DeviceProtection reporting no
    /// persistence, an unplugged hardware token say, refused a policy that never touches it.
    ///
    /// The other two share those prerequisites and also need a device key. The strongest
    /// additionally needs that key to be gated on user verification.
    public boolean supports(UnlockPolicy policy) {
        // Every policy persists encrypted metadata, even when its device key store is healthy.
        if (!canKeepARecord) {
            return false;
        }
        if (policy == UnlockPolicy.SESSION_ONLY) {
            return true;
        }
        ProtectionReport report = device.protection();
        if (!report.provides(Protection.PERSISTENT)) {
            return false;
        }
        if (policy == UnlockPolicy.REQUIRE_USER_VERIFICATION) {
            DeviceProtection gated = device.userVerifying();
            if (gated == null || !gated.requiresUserVerification()) {
                return false;
            }
            ProtectionReport gatedReport = gated.protection();
            // The GATED mechanism's own persistence, not the base's. They can be different
            // objects -- a passkey beside a stored key in the browser, an application's own
            // adapter anywhere -- and checking only the base accepted a variant that does not
            // survive a restart, so a vault enrolled under this policy was not remembered at all
            // while the capability said it would be.
            return gatedReport.provides(Protection.PERSISTENT)
                    && gatedReport.provides(Protection.USER_VERIFICATION);
        }
        return true;
    }

    /// What a vault enrolled under this policy would be protected by, if it were enrolled now.
    ///
    /// Under [UnlockPolicy#SESSION_ONLY] the key-storage answers are all `NO`, because there is
    /// no stored key to protect -- which is a stronger position than any of them and reads as a
    /// weaker one, so an application showing this to a user should say "the password is required
    /// every time" rather than listing flags.
    public ProtectionReport protectionFor(UnlockPolicy policy) {
        ProtectionReport.Builder b = ProtectionReport.builder();
        b.set(Protection.ISOLATED_FROM_APPLICATION_CODE, false);
        if (policy == UnlockPolicy.SESSION_ONLY) {
            // What session-only needs in order to persist anything is ordinary Storage and the
            // cipher, which is what canKeepARecord measures -- not a device store it never uses.
            b.set(Protection.PERSISTENT, canKeepARecord);
            // Nothing that can reopen the vault is written, so what is at rest is ciphertext under
            // a key derived from a password that is stored nowhere. Genuinely encrypted at rest.
            b.set(Protection.ENCRYPTED_AT_REST, true);
            b.set(Protection.NON_EXTRACTABLE_KEY, false);
            b.set(Protection.OS_PROTECTED, false);
            b.set(Protection.HARDWARE_BACKED, false);
            b.set(Protection.USER_VERIFICATION, false);
            return b.build();
        }
        // Reported from the protection that policy would actually use, which on the browser is a
        // different mechanism for the gated policy than for the unattended one.
        // Asked ONCE and reused. userVerifying() is a live question -- on the browser it
        // depends on a capability probe that resolves asynchronously at startup -- so the null
        // check and the use could see different answers, and a provider that appeared for the
        // first and vanished for the second made this a null whose protection() call threw.
        DeviceProtection gated = policy == UnlockPolicy.REQUIRE_USER_VERIFICATION
                ? device.userVerifying() : null;
        DeviceProtection forPolicy = gated != null ? gated : device;
        ProtectionReport report = forPolicy.protection();
        // PERSISTENT from the mechanism this policy would actually use, not hardcoded. It used
        // to be set to true at the top of this method for every policy, so a gated variant that
        // does not survive a restart -- an application's own adapter, a token that is not
        // present -- was still described as persistent, and Vault.requireProtections then
        // accepted a PERSISTENT guarantee for an enrolment that would not be remembered at all.
        b.set(Protection.PERSISTENT, report.answer(Protection.PERSISTENT));
        // Taken from the store rather than assumed. A remembered vault is only as encrypted at
        // rest as the wrapping key is: where that key sits in the clear beside the ciphertext --
        // the simulator, and Android before the keystore existed -- the records are ciphertext and
        // the protection is not there, so a caller requiring it must be refused rather than
        // reassured. Hardcoding yes here would have defeated those stores reporting honestly.
        b.set(Protection.ENCRYPTED_AT_REST, report.answer(Protection.ENCRYPTED_AT_REST));
        b.set(Protection.NON_EXTRACTABLE_KEY, report.answer(Protection.NON_EXTRACTABLE_KEY));
        b.set(Protection.OS_PROTECTED, report.answer(Protection.OS_PROTECTED));
        b.set(Protection.HARDWARE_BACKED, report.answer(Protection.HARDWARE_BACKED));
        b.set(Protection.USER_VERIFICATION,
                policy == UnlockPolicy.REQUIRE_USER_VERIFICATION
                        ? report.answer(Protection.USER_VERIFICATION) : ProtectionReport.NO);
        return b.build();
    }

    /// What the device key store itself reports, unfiltered by any policy.
    public ProtectionReport deviceProtection() {
        return device.protection();
    }

    /// Whether this device's key store gates access on the user proving they are present.
    public boolean requiresUserVerification() {
        return device.requiresUserVerification();
    }

    /// A line per capability, for a diagnostics screen.
    @Override
    public String toString() {
        return "session=" + supports(UnlockPolicy.SESSION_ONLY)
                + ", remember=" + supports(UnlockPolicy.REMEMBER_DEVICE)
                + ", userVerification=" + supports(UnlockPolicy.REQUIRE_USER_VERIFICATION)
                + ", device[" + device.protection() + "]";
    }
}
