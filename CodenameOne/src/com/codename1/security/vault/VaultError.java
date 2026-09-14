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

/// Why a vault or protected-storage operation failed, as a code rather than a message.
///
/// Every distinction here exists because acting on the wrong one destroys data. The pair that
/// matters most is [#KEY_MISSING] against [#TEMPORARILY_UNREADABLE]: a caller that treats "I
/// could not read the key" as "there is no key" generates a replacement, and every record the
/// old key protected becomes unopenable. The same shape recurs for [#CORRUPT] against
/// [#AUTHENTICATION_FAILED] and for [#LOCKED] against [#CANCELLED].
public enum VaultError {

    /// The platform has no store to write to, or the store could not be opened at all. On the
    /// browser this covers private-browsing restrictions that refuse IndexedDB outright.
    STORAGE_UNAVAILABLE,

    /// The cryptographic provider is missing or failed. On the browser, `crypto.subtle` absent --
    /// which is what an insecure context looks like from inside the page -- or present and unable
    /// to complete the operation.
    CRYPTO_UNAVAILABLE,

    /// The caller required protections this platform does not provide. The refused protection is
    /// available from [VaultException#getUnmetProtection()].
    ///
    /// Never downgraded silently: a request for encrypted storage on a platform that can only
    /// offer plaintext fails here rather than writing plaintext.
    POLICY_NOT_MET,

    /// Hardened storage was requested from an insecure context -- plain `http:` on anything but
    /// `localhost`. Web Crypto is unavailable there by specification, and a vault that pretended
    /// otherwise would be encrypting nothing.
    INSECURE_CONTEXT,

    /// The vault exists but is locked. Unlock it and retry; nothing is wrong with the data.
    LOCKED,

    /// The user dismissed a prompt, or the operation was aborted. Distinct from
    /// [#AUTHENTICATION_FAILED]: nobody failed a check, the check did not happen.
    CANCELLED,

    /// The store answered, and the key or entry is definitely not there. Only this code -- never
    /// a null, never [#TEMPORARILY_UNREADABLE] -- justifies creating a replacement.
    KEY_MISSING,

    /// The key or entry could not be read, and the store cannot say whether it exists. A
    /// transient condition as far as any caller is concerned: retry, prompt, or give up, but do
    /// not write.
    TEMPORARILY_UNREADABLE,

    /// The stored bytes are not a well formed envelope: truncated, a bad magic, a length field
    /// that does not fit. Structural, decided before any key is involved.
    CORRUPT,

    /// The envelope parsed and the authentication tag did not verify. Either the ciphertext or
    /// its associated data was altered, or the key is the wrong one -- a wrong password lands
    /// here. No plaintext is ever returned alongside this.
    AUTHENTICATION_FAILED,

    /// The envelope is well formed and declares a version, suite or KDF this build does not
    /// implement. Deliberately not a downgrade: an old client refuses a newer envelope rather
    /// than reading it with weaker rules.
    UNSUPPORTED_FORMAT,

    /// The store refused the write for space, or evicted what was already there. On the browser
    /// this is also what a cleared-site-data or evicted-origin looks like after the fact.
    QUOTA_EXCEEDED,

    /// Two writers raced and this one lost, or the stored state moved under the operation. The
    /// caller should re-read and retry rather than overwrite.
    CONFLICT,

    /// The requested mechanism is not implemented on this port -- a passkey unlock where there is
    /// no authenticator, a hardware-backed key where there is no hardware.
    NOT_SUPPORTED,

    /// Anything the codes above do not cover.
    UNKNOWN
}
