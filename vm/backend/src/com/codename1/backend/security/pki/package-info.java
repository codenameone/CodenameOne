/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
/// Certificates, certification requests and key stores, read and written in
/// the same Java on the JVM and in a native build.
///
/// - [Pem] reads and writes PEM text; [Certificate] reads X.509 DER or PEM.
/// - [CertificateValidator] validates a chain against trust anchors the
///   application gives. There is no default trust store.
/// - [CertificationRequest] creates and reads PKCS#10 requests.
/// - [Pkcs12] reads and writes `.p12`/`.pfx` key stores.
/// - Keys are PKCS#8 DER (private) and SubjectPublicKeyInfo DER (public), the
///   forms `com.codename1.backend.Crypto` signs, verifies and agrees keys with;
///   `Crypto.generateEcKey` and `Crypto.ecdh` make and use P-256 and P-384 keys.
///
/// The algorithm profile, the same on every runtime:
///
/// | Use | Supported | Refused |
/// |---|---|---|
/// | Certificate and request signatures | RSA PKCS#1 v1.5 with SHA-256/384/512; RSASSA-PSS with SHA-256, MGF1-SHA-256, 32 byte salt; ECDSA P-256/SHA-256 and P-384/SHA-384 | SHA-1, MD5, other PSS parameters, ECDSA with a digest that does not match its curve |
/// | Keys | RSA 2048 to 8192 bits; EC on P-256 and P-384 | Other curves, DSA, Ed25519 |
/// | Key agreement | ECDH on P-256 and P-384 | X25519 |
/// | PKCS#12 encryption | PBES2 with PBKDF2-HMAC-SHA-256 or -SHA-1 and AES-128/192/256-CBC | RC2, 3DES (`-legacy`) |
/// | PKCS#12 integrity | HMAC-SHA-1/256/384/512 MAC | Public-key integrity, no MAC |
/// | Encoding | DER | BER indefinite lengths |
///
/// Chain validation checks signatures, validity periods, basic constraints, key
/// usage, path length, unknown critical extensions, and optionally the host
/// name and extended key usage of the leaf. It does not check revocation, name
/// constraints or certificate policies; a certificate that marks either of the
/// last two critical is refused.
package com.codename1.backend.security.pki;
