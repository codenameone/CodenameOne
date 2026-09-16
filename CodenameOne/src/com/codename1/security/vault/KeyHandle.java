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

import com.codename1.util.AsyncResource;

/// A key you can use and cannot read.
///
/// #### Why this is not a [com.codename1.security.Key]
///
/// [com.codename1.security.Key] is built around encoded bytes: the constructor requires them,
/// `getEncoded()` is `final`, and [com.codename1.security.SecretKey] is `final` on top of that.
/// Every one of those is correct for what that class is -- a value object wrapping key material --
/// and every one of them makes it impossible to represent a key whose material does not exist as
/// bytes the application can reach. A browser `CryptoKey` created with `extractable: false` is
/// exactly that key, and so is an Android keystore key and an iOS Secure Enclave key. Subclassing
/// would have produced a `getEncoded()` that either lies or throws, and code that takes a `Key`
/// would keep compiling while silently getting neither.
///
/// So this is a separate type with no byte accessor at all. There is no `getEncoded`, no
/// `export`, and no flag that turns one on. [#isExportable()] reports whether the underlying
/// material *could* be exported by some other means -- it never provides the means.
///
/// #### What it does not protect against
///
/// A handle stops the key material being copied. It does not stop the key being **used**: while
/// the vault is unlocked, any code running in the application -- including script an XSS
/// injected into the page -- can call [#seal] and [#open] on a handle it can reach, exactly as
/// the application does. Non-extractability limits what an attacker can carry away, not what they
/// can do while they are there. [#destroy()] and [Vault#lock()] cut that off for future calls and
/// cannot reach a plaintext already handed out.
///
/// #### Lifecycle
///
/// A handle obtained from a vault is invalidated when that vault locks. Calls afterwards fail
/// with [VaultError#LOCKED] rather than returning stale results, and an operation already in
/// flight when the lock happens does not deliver its result -- see [Vault#lock()].
public abstract class KeyHandle {

    /// Subclasses are created by the vault or by a port.
    protected KeyHandle() {
    }

    /// The algorithm this key is for, e.g. `"AES-GCM"`.
    public abstract String getAlgorithm();

    /// A stable identifier for this key, carried in the envelopes it seals so a reader holding
    /// several keys can pick the right one without trying each.
    public abstract String getKeyId();

    /// The rotation counter. Incremented by [Vault#rotateDataKey()]; envelopes record the version
    /// that sealed them, so an old envelope can still be opened after a rotation and can be
    /// identified as needing a rewrite.
    public abstract int getVersion();

    /// Whether the key material could be exported through some other path.
    ///
    /// `false` is the interesting answer and means the platform holds the key in a form it will
    /// not hand back -- a non-extractable `CryptoKey`, a keystore alias, a Secure Enclave
    /// reference. `true` means the material exists as bytes somewhere in the process; the handle
    /// still will not give them to you, but it is not claiming the platform could not.
    ///
    /// This method never enables an export. There is no method on this class that does.
    public abstract boolean isExportable();

    /// The operations this handle permits, fixed at creation.
    public abstract KeyUsage[] getUsages();

    /// Whether this handle permits an operation.
    public boolean permits(KeyUsage usage) {
        KeyUsage[] usages = getUsages();
        if (usages == null || usage == null) {
            return false;
        }
        for (KeyUsage permitted : usages) {
            if (permitted == usage) {
                return true;
            }
        }
        return false;
    }

    /// What actually protects this key, as observed. See [ProtectionReport].
    public abstract ProtectionReport getProtection();

    /// Authenticated encryption. The result is a [SecureEnvelope]: the nonce, the key id and the
    /// version are managed here and are not the caller's to choose, because a reused nonce
    /// destroys AES-GCM and an API that let one be passed in would eventually see one.
    ///
    /// #### Parameters
    ///
    /// - `plaintext`: the bytes to protect
    ///
    /// - `aad`: the binding the result may only be opened against, may be null
    ///
    /// #### Returns
    ///
    /// a resource completing with the sealed bytes, or erroring with a [VaultException]
    public abstract AsyncResource<byte[]> seal(byte[] plaintext, AssociatedData aad);

    /// Authenticated decryption of what [#seal] produced.
    ///
    /// On a tag mismatch the resource errors with [VaultError#AUTHENTICATION_FAILED] and no
    /// plaintext is delivered -- not a truncated one, not an unverified one. A caller that wants
    /// the bytes anyway cannot have them from here.
    public abstract AsyncResource<byte[]> open(byte[] sealed, AssociatedData aad);

    /// A message authentication tag over `data`, for a handle that permits [KeyUsage#MAC].
    ///
    /// The default reports [VaultError#NOT_SUPPORTED]; a handle whose platform provides HMAC
    /// overrides it.
    public AsyncResource<byte[]> mac(byte[] data) {
        AsyncResource<byte[]> out = new AsyncResource<byte[]>();
        out.error(new VaultException(VaultError.NOT_SUPPORTED,
                "this key handle does not perform message authentication"));
        return out;
    }

    /// Verifies a tag from [#mac(byte[])], in constant time.
    public AsyncResource<Boolean> verifyMac(byte[] data, byte[] tag) {
        AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        out.error(new VaultException(VaultError.NOT_SUPPORTED,
                "this key handle does not perform message authentication"));
        return out;
    }

    /// Releases the handle and, where the material is held in this process, overwrites it.
    ///
    /// Best effort, and said so plainly: a managed runtime may have copied the buffer during a
    /// collection, the browser's heap is not ours to scrub, and a JIT may hold a register copy.
    /// What this does guarantee is that the handle stops working -- subsequent calls fail with
    /// [VaultError#LOCKED] -- and that the application's own reference is cleared.
    public abstract void destroy();

    /// Whether [#destroy()] has been called, or the owning vault has locked.
    public abstract boolean isDestroyed();
}
