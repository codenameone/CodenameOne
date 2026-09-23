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

import com.codename1.security.CryptoException;

/// The failure every vault and protected-storage operation reports, carrying a [VaultError]
/// rather than only a message.
///
/// Extends [CryptoException] so existing `catch (CryptoException)` around
/// [com.codename1.security.Cipher] work keeps compiling and keeps catching. Branch on
/// [#getError()] rather than on the message.
///
/// #### The message carries no secret
///
/// Redaction happens where the exception is constructed rather than where it is read, because a
/// message reaches logs, crash reports and telemetry before anybody chooses to look at it. The
/// rule for anyone adding a throw site: the account name is allowed, the value is not, and
/// neither is a length or a prefix of it.
public class VaultException extends CryptoException {

    private final VaultError error;
    private final Protection unmetProtection;

    /// #### Parameters
    ///
    /// - `error`: the code callers branch on, null becomes [VaultError#UNKNOWN]
    ///
    /// - `message`: description for a human. Must not contain a secret, an account value or a
    ///   fragment of ciphertext: this string reaches logs, crash reports and telemetry.
    public VaultException(VaultError error, String message) {
        this(error, message, null, null);
    }

    /// #### Parameters
    ///
    /// - `error`: the code callers branch on
    ///
    /// - `message`: description for a human, carrying no secret material
    ///
    /// - `cause`: the underlying failure, or null
    public VaultException(VaultError error, String message, Throwable cause) {
        this(error, message, null, cause);
    }

    /// The constructor for [VaultError#POLICY_NOT_MET], which is the one failure that can name
    /// what was missing.
    ///
    /// #### Parameters
    ///
    /// - `error`: the code callers branch on
    ///
    /// - `message`: description for a human, carrying no secret material
    ///
    /// - `unmetProtection`: the protection that was required and not provided, or null
    ///
    /// - `cause`: the underlying failure, or null
    public VaultException(VaultError error, String message, Protection unmetProtection, Throwable cause) {
        super(message, cause);
        this.error = error == null ? VaultError.UNKNOWN : error;
        this.unmetProtection = unmetProtection;
    }

    /// The code to branch on.
    public VaultError getError() {
        return error;
    }

    /// For [VaultError#POLICY_NOT_MET], the protection that was required and not provided.
    /// Null for every other code.
    public Protection getUnmetProtection() {
        return unmetProtection;
    }

    /// Whether this failure means the entry is definitely absent, which is the only state in
    /// which generating a replacement key is safe.
    ///
    /// Written as a method rather than left to callers comparing codes because getting it wrong
    /// is unrecoverable: a caller that also accepts [VaultError#TEMPORARILY_UNREADABLE] here
    /// overwrites a key that was there and orphans everything it protected.
    public boolean isDefinitelyAbsent() {
        return error == VaultError.KEY_MISSING;
    }

}
