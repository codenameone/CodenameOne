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

/// What a [KeyHandle] is allowed to do.
///
/// Usages are fixed when a handle is created and are not widened afterwards. A handle created to
/// seal records cannot be talked into signing, which matters because the two operations under one
/// key is a classic way to turn a confidentiality primitive into an oracle. Web Crypto enforces
/// the same restriction on a `CryptoKey`, and this mirrors it so a port cannot be more permissive
/// than the browser is.
public enum KeyUsage {

    /// Authenticated encryption: [KeyHandle#seal].
    SEAL,

    /// Authenticated decryption: [KeyHandle#open].
    OPEN,

    /// Message authentication: [KeyHandle#mac].
    MAC,

    /// Verification of a tag produced by [#MAC].
    VERIFY_MAC,

    /// Wrapping another key. Separate from [#SEAL] so a key that only protects other keys cannot
    /// be pointed at application data, and the reverse.
    WRAP,

    /// Unwrapping a key wrapped under [#WRAP].
    UNWRAP
}
