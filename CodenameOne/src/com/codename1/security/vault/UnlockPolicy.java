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

/// How a vault may be reopened, chosen by the application and explained to the user.
///
/// This is the trade an application actually has to make, so it is a choice of three named
/// behaviours rather than a set of cryptographic parameters. What changes between them is
/// **which wraps of the data key are allowed to exist on this device** -- and the rule that makes
/// the strongest one mean anything is that choosing it *removes* the weaker wraps rather than
/// leaving them beside it. A vault that requires a passkey and also keeps an unattended device
/// wrap is a vault that does not require a passkey.
public enum UnlockPolicy {

    /// The password is required every time the application starts, or in a browser every time the
    /// page loads.
    ///
    /// Nothing that can reopen the vault is written to the device: the data key exists only in
    /// memory between unlock and lock. The strongest of the three against a stolen device or a
    /// copied browser profile, and the one users abandon, which is why it is not the default for
    /// anything.
    SESSION_ONLY,

    /// The user chose to be remembered on this device.
    ///
    /// The data key is additionally wrapped under a device key -- the OS key store on a native
    /// port, a non-extractable `CryptoKey` in the browser -- so the vault reopens without a
    /// prompt. That is unattended access by design: anyone who can run the application on this
    /// device, or who copies a full browser profile including its IndexedDB, reopens the vault
    /// without knowing the password.
    ///
    /// An application offering this must say so in those words. "Remember me" reads as a
    /// convenience and is a change in who can read the data.
    REMEMBER_DEVICE,

    /// The device key exists but reaching it requires the user to verify themselves -- a
    /// biometric, a device passcode, a passkey with user verification.
    ///
    /// Choosing this deletes any [#REMEMBER_DEVICE] wrap that already existed, and refuses to
    /// create one while it is in force. Without that, the unattended wrap sits beside the gated
    /// one and the prompt is decoration.
    ///
    /// Not every platform can provide it. [Vault#capabilities()] reports whether this device can,
    /// and [Vault#enroll] fails with [VaultError#POLICY_NOT_MET] rather than quietly enrolling
    /// under a weaker policy.
    REQUIRE_USER_VERIFICATION
}
