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

    VaultCapabilities(DeviceProtection device) {
        this.device = device;
    }

    /// Whether an unlock policy can be honoured on this device.
    ///
    /// [UnlockPolicy#SESSION_ONLY] is always supported: it needs no key storage, because it
    /// stores no key. The other two need a device key, and the strongest additionally needs that
    /// key to be gated on user verification.
    public boolean supports(UnlockPolicy policy) {
        if (policy == UnlockPolicy.SESSION_ONLY) {
            return true;
        }
        ProtectionReport report = device.protection();
        if (!report.provides(Protection.PERSISTENT)) {
            return false;
        }
        if (policy == UnlockPolicy.REQUIRE_USER_VERIFICATION) {
            DeviceProtection gated = device.userVerifying();
            return gated != null && gated.requiresUserVerification()
                    && gated.protection().provides(Protection.USER_VERIFICATION);
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
        b.set(Protection.PERSISTENT, true);
        b.set(Protection.ISOLATED_FROM_APPLICATION_CODE, false);
        if (policy == UnlockPolicy.SESSION_ONLY) {
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
        DeviceProtection forPolicy = policy == UnlockPolicy.REQUIRE_USER_VERIFICATION
                && device.userVerifying() != null ? device.userVerifying() : device;
        ProtectionReport report = forPolicy.protection();
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
