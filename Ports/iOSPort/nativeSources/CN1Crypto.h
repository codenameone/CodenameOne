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
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */

/*
 * iOS native crypto helpers used by the Java com.codename1.security package.
 *
 * Each function is a plain C function so it can be called from the
 * ParparVM-generated bridge that backs the native methods declared on
 * IOSNative. The matching Java side lives in
 *     CodenameOne/src/com/codename1/security/ (all classes)
 * (and the bridge methods on CodenameOneImplementation are overridden by
 *  IOSImplementation to call these helpers).
 *
 * Implementations use the platform-supplied Security framework (Apple
 * CryptoKit on iOS 13+ would be cleaner but we still target iOS 11 and need
 * to support the older path) and CommonCrypto.
 *
 * All buffers are caller-allocated. Return values are byte lengths actually
 * written, or a negative value on error (see CN1_CRYPTO_E_* constants).
 */

#ifndef CN1Crypto_h
#define CN1Crypto_h

#import <Foundation/Foundation.h>

/*
 * Build-system toggles. IPhoneBuilder (maven plugin + BuildDaemon) scans the
 * user's compiled bytecode for references to com.codename1.security.* and
 * flips the placeholders below to enable the matching code paths. Apps that
 * don't use the crypto API end up with no extra crypto symbols in the
 * binary -- in particular the AES-GCM SPI references stay completely out
 * unless the app opts into GCM via the ios.crypto.gcm build hint.
 */
//#define CN1_INCLUDE_CRYPTO
//#define CN1_INCLUDE_CRYPTO_GCM

#define CN1_CRYPTO_E_GENERIC       -1
#define CN1_CRYPTO_E_BAD_KEY       -2
#define CN1_CRYPTO_E_BAD_INPUT     -3
#define CN1_CRYPTO_E_AUTH_FAIL     -4
#define CN1_CRYPTO_E_UNSUPPORTED   -5

/* --- secure random ----------------------------------------------------- */

/// Fills `out` with `len` cryptographically secure random bytes via
/// SecRandomCopyBytes. Returns 0 on success, negative on error.
int cn1_crypto_secure_random(uint8_t* out, int len);

/* --- AES --------------------------------------------------------------- */

/// AES-CBC encrypt / decrypt using CommonCrypto. `iv` must be 16 bytes,
/// `key` 16/24/32 bytes. `padding` is 1 for PKCS5 / 7, 0 for none.
/// Returns bytes written into `out`, or negative on error. `out` must be
/// pre-allocated to at least `inLen + 16` bytes.
int cn1_crypto_aes_cbc(int encrypt, const uint8_t* key, int keyLen,
                       const uint8_t* iv,
                       const uint8_t* in, int inLen,
                       uint8_t* out, int outCap, int padding);

/// AES-GCM encrypt / decrypt. `iv` is the 12-byte nonce. `aad` may be NULL.
/// On encrypt, the 16-byte auth tag is APPENDED to the ciphertext (JCE
/// convention). On decrypt, the last 16 bytes of `in` are the tag.
/// Returns bytes written to `out`, or negative on error.
int cn1_crypto_aes_gcm(int encrypt, const uint8_t* key, int keyLen,
                       const uint8_t* iv, int ivLen,
                       const uint8_t* aad, int aadLen,
                       const uint8_t* in, int inLen,
                       uint8_t* out, int outCap);

/* --- RSA --------------------------------------------------------------- */

/// RSA encrypt with the given X.509 SubjectPublicKeyInfo DER bytes.
/// `paddingKind` = 1 for PKCS#1, 2 for OAEP-SHA-256.
/// Returns bytes written to `out` or negative.
int cn1_crypto_rsa_encrypt(int paddingKind,
                           const uint8_t* x509, int x509Len,
                           const uint8_t* in,  int inLen,
                           uint8_t* out, int outCap);

/// RSA decrypt with the given PKCS#8 DER bytes.
int cn1_crypto_rsa_decrypt(int paddingKind,
                           const uint8_t* pkcs8, int pkcs8Len,
                           const uint8_t* in,    int inLen,
                           uint8_t* out, int outCap);

/* --- Signatures -------------------------------------------------------- */

/// Sign `data` with the given PKCS#8 private key. `algorithm` codes:
///   0 = SHA256withRSA   1 = SHA384withRSA   2 = SHA512withRSA
///   3 = SHA256withECDSA 4 = SHA384withECDSA 5 = SHA512withECDSA
/// Returns bytes written to `out` or negative.
int cn1_crypto_sign(int algorithm,
                    const uint8_t* pkcs8, int pkcs8Len,
                    const uint8_t* data,  int dataLen,
                    uint8_t* out, int outCap);

/// Verify a signature with the given X.509 public key.
/// Returns 1 on valid, 0 on invalid, negative on error.
int cn1_crypto_verify(int algorithm,
                      const uint8_t* x509, int x509Len,
                      const uint8_t* data, int dataLen,
                      const uint8_t* sig,  int sigLen);

/* --- RSA key-pair generation ------------------------------------------ */

/// Generates an RSA key pair of the given size in bits.
/// Caller pre-allocates `outPub` (X.509) and `outPriv` (PKCS#8) buffers.
/// `pubCap` / `privCap` are their sizes in bytes; the actual byte counts
/// written are stored at `*pubLen` / `*privLen`.
/// Returns 0 on success, negative on error.
int cn1_crypto_generate_rsa_keypair(int bits,
                                    uint8_t* outPub,  int pubCap,  int* pubLen,
                                    uint8_t* outPriv, int privCap, int* privLen);

#endif /* CN1Crypto_h */
