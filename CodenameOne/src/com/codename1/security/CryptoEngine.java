/*
 * Copyright (c) 2008-2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.security;

/// The seam through which a platform supplies faster implementations of the
/// compute-heavy cryptography in this package. Every method answers `null`
/// (or `false` where noted) when the engine has no faster path for that
/// input, and the caller then runs the portable Java implementation, which
/// produces identical bytes. A missing kernel therefore degrades to slower,
/// never to different.
///
/// Client ports install one when Codename One initializes; the backend
/// installs its own. Application code has no reason to call this class: use
/// [Pbkdf2], [Secp256k1] and [Hash], which consult the installed engine.
///
/// An engine must be thread safe and must not keep or log the key material
/// it is handed.
@com.codename1.impl.SharedWithBackend
public class CryptoEngine {
    private static final CryptoEngine PORTABLE = new CryptoEngine();
    private static CryptoEngine installed = PORTABLE;

    /// Creates an engine that accelerates nothing; subclasses override what
    /// they accelerate.
    protected CryptoEngine() {
    }

    /// The installed engine, never null.
    public static CryptoEngine get() {
        return installed;
    }

    /// Installs an engine, or restores the portable one when given `null`.
    /// Called by a port during initialization.
    public static void install(CryptoEngine engine) {
        installed = engine == null ? PORTABLE : engine;
    }

    /// PBKDF2 (RFC 8018) with HMAC over `hashAlgorithm` ([Hash#SHA256] or
    /// [Hash#SHA512]), or `null` to use the Java derivation. The arguments
    /// are already validated.
    public byte[] pbkdf2(String hashAlgorithm, byte[] password, byte[] salt, int iterations, int length) {
        return null;
    }

    /// An Ed25519 signature ([Ed25519#sign(byte[], byte[], byte[])]), or
    /// `null` to sign in Java. The arguments are already validated.
    public byte[] ed25519Sign(byte[] privateKey, byte[] publicKey, byte[] message) {
        return null;
    }

    /// An Ed25519 verification: 1 for a valid signature, 0 for an invalid
    /// one, or -1 to verify in Java.
    public int ed25519Verify(byte[] message, byte[] signature, byte[] publicKey) {
        return -1;
    }
}
