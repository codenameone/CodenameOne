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
/// Password-protected storage that works the same way on Android, iOS and in a browser, and that
/// says what it does and does not protect.
///
/// [com.codename1.security.vault.Vault] is the entry point: enroll with a password, unlock,
/// store secrets, seal records, synchronise the encrypted state to another device. One random
/// data key protects the contents and is itself stored only in wrapped form -- under the
/// password, under a recovery code, and optionally under this device's key store so the user does
/// not have to type the password again.
///
/// The browser is the reason this package exists. There is no key store in a page, and the
/// previous answer -- a plain string in origin-private storage -- gave persistence and no
/// protection. What a browser does have is AES-GCM through Web Crypto and a `CryptoKey` that can
/// be marked non-extractable and kept in IndexedDB, and that is enough to make what is written to
/// disk ciphertext under a key that cannot be copied out. It is not enough to hide anything from
/// script running in the page, and nothing in this package claims otherwise: see the table on
/// [com.codename1.security.vault.Vault] and the field notes on
/// [com.codename1.security.vault.Protection].
///
/// #### The pieces
///
/// - [com.codename1.security.vault.Vault] -- the facade applications use.
/// - [com.codename1.security.vault.UnlockPolicy] -- session only, remembered device, or gated on
///   user verification.
/// - [com.codename1.security.vault.Protection] and
///   [com.codename1.security.vault.ProtectionReport] -- independent capability flags with a real
///   "cannot say" answer, rather than a security level.
/// - [com.codename1.security.vault.VaultError] -- typed failures, including the distinction
///   between a key that is missing and a key that could not be read.
/// - [com.codename1.security.vault.SecureEnvelope] and
///   [com.codename1.security.vault.AssociatedData] -- the versioned authenticated format every
///   port reads and writes, specified byte for byte.
/// - [com.codename1.security.vault.KdfProfile] -- the portable password KDF and its bounds.
/// - [com.codename1.security.vault.KeyHandle] -- a key you can use and cannot read.
/// - [com.codename1.security.vault.spi.DeviceProtection] -- the one piece each port implements.
package com.codename1.security.vault;
