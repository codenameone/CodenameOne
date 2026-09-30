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

#import "CN1Crypto.h"

#ifdef CN1_INCLUDE_CRYPTO

#import <Security/Security.h>
#import <CommonCrypto/CommonCrypto.h>
#import <CommonCrypto/CommonCryptor.h>
#import <CommonCrypto/CommonRandom.h>


/* --- secure random ----------------------------------------------------- */

int cn1_crypto_secure_random(uint8_t* out, int len) {
    if (len <= 0) return 0;
    if (SecRandomCopyBytes(kSecRandomDefault, len, out) != errSecSuccess) {
        return CN1_CRYPTO_E_GENERIC;
    }
    return 0;
}

/* --- PBKDF2 ------------------------------------------------------------ */

int cn1_crypto_pbkdf2(int hashKind,
                      const uint8_t* password, int passwordLen,
                      const uint8_t* salt, int saltLen,
                      int iterations,
                      uint8_t* out, int outLen) {
    if (out == NULL || outLen <= 0 || salt == NULL || saltLen <= 0 || iterations <= 0) {
        return CN1_CRYPTO_E_BAD_INPUT;
    }
    CCPseudoRandomAlgorithm prf;
    if (hashKind == 512) {
        prf = kCCPRFHmacAlgSHA512;
    } else if (hashKind == 256) {
        prf = kCCPRFHmacAlgSHA256;
    } else {
        return CN1_CRYPTO_E_UNSUPPORTED;
    }
    /*
     * CCKeyDerivationPBKDF takes the password as (char*, length) and does not
     * stop at a NUL, which is what makes it usable for bytes that came out of a
     * UTF-8 encoder rather than a C string. A password of length zero is legal
     * and passing NULL for it is not, so an empty one gets a valid pointer.
     */
    static const uint8_t emptyPassword[1] = { 0 };
    const uint8_t* pw = (password == NULL || passwordLen <= 0) ? emptyPassword : password;
    size_t pwLen = (password == NULL || passwordLen <= 0) ? 0 : (size_t) passwordLen;
    if (CCKeyDerivationPBKDF(kCCPBKDF2, (const char*) pw, pwLen,
                             salt, (size_t) saltLen,
                             prf, (uint) iterations,
                             out, (size_t) outLen) != kCCSuccess) {
        return CN1_CRYPTO_E_GENERIC;
    }
    return outLen;
}

/* --- AES-CBC ----------------------------------------------------------- */

int cn1_crypto_aes_cbc(int encrypt, const uint8_t* key, int keyLen,
                       const uint8_t* iv,
                       const uint8_t* in, int inLen,
                       uint8_t* out, int outCap, int padding) {
    if (keyLen != kCCKeySizeAES128 && keyLen != kCCKeySizeAES192 && keyLen != kCCKeySizeAES256) {
        return CN1_CRYPTO_E_BAD_KEY;
    }
    size_t produced = 0;
    CCOptions opts = padding ? kCCOptionPKCS7Padding : 0;
    CCCryptorStatus s = CCCrypt(
        encrypt ? kCCEncrypt : kCCDecrypt,
        kCCAlgorithmAES,
        opts,
        key, (size_t) keyLen,
        iv,
        in,  (size_t) inLen,
        out, (size_t) outCap,
        &produced);
    if (s != kCCSuccess) {
        return CN1_CRYPTO_E_GENERIC;
    }
    return (int) produced;
}

/* --- AES-GCM ----------------------------------------------------------- */

#ifdef CN1_INCLUDE_CRYPTO_GCM
/*
 * AES-GCM (NIST SP 800-38D) built from PUBLIC CommonCrypto calls only.
 *
 * CommonCrypto's own GCM entry points (CCCryptorGCMAddIV, CCCryptorGCMAddAAD,
 * CCCryptorGCMFinal) are declared only in CommonCryptorSPI.h, which is not in
 * the iOS or macOS SDK. They link -- libcommonCrypto exports them -- but App
 * Store Connect rejects any upload that imports them: "Validation failed (409)
 * The app references non-public symbols ... _CCCryptorGCMAddAAD,
 * _CCCryptorGCMAddIV, _CCCryptorGCMFinal". So any application that reached
 * this code could not ship, and scripts/check-ios-private-api.py now fails the
 * build on any symbol like them.
 *
 * GCM is CTR mode plus the GHASH authenticator, and both halves are available
 * without the SPI:
 *
 *   - the keystream is AES-CTR (kCCModeCTR, big-endian counter) started at
 *     inc32(J0), where J0 = IV || 0x00000001 for the 96-bit IVs accepted here;
 *   - H = AES_K(0^128) and the tag mask AES_K(J0) are single ECB blocks;
 *   - GHASH is a GF(2^128) multiply, done below in portable C.
 *
 * CommonCrypto's CTR increments the whole 128-bit block while GCM's inc32 wraps
 * the low 32 bits only. They agree until the low word overflows, which needs
 * 2^32 - 2 blocks (64 GiB) of input; GCM forbids more than that per IV, and
 * inLen is an int, so the two never diverge here.
 *
 * The GHASH multiply below is branch-free on the key and data (the reduction is
 * a masked XOR, not a conditional), so it does not leak H through timing.
 */

static void cn1_gcm_mult(uint8_t x[16], const uint8_t h[16]) {
    uint8_t z[16];
    uint8_t v[16];
    memset(z, 0, 16);
    memcpy(v, h, 16);
    for (int i = 0; i < 128; i++) {
        uint8_t bit = (uint8_t) ((x[i >> 3] >> (7 - (i & 7))) & 1);
        uint8_t mask = (uint8_t) (0 - bit);
        for (int j = 0; j < 16; j++) {
            z[j] ^= v[j] & mask;
        }
        uint8_t lsb = (uint8_t) (v[15] & 1);
        for (int j = 15; j > 0; j--) {
            v[j] = (uint8_t) ((v[j] >> 1) | (v[j - 1] << 7));
        }
        v[0] >>= 1;
        v[0] ^= (uint8_t) (0xe1 & (0 - lsb));
    }
    memcpy(x, z, 16);
}

/* Absorbs `len` bytes into the GHASH state, zero padding the final block. */
static void cn1_gcm_ghash(uint8_t y[16], const uint8_t h[16], const uint8_t* data, size_t len) {
    while (len > 0) {
        size_t n = len < 16 ? len : 16;
        for (size_t i = 0; i < n; i++) {
            y[i] ^= data[i];
        }
        cn1_gcm_mult(y, h);
        data += n;
        len -= n;
    }
}

static int cn1_gcm_ecb_block(const uint8_t* key, int keyLen, const uint8_t in[16], uint8_t out[16]) {
    size_t produced = 0;
    CCCryptorStatus s = CCCrypt(kCCEncrypt, kCCAlgorithmAES, kCCOptionECBMode,
                                key, (size_t) keyLen, NULL, in, 16, out, 16, &produced);
    return (s == kCCSuccess && produced == 16) ? 0 : CN1_CRYPTO_E_GENERIC;
}

/* tag = AES_K(J0) XOR GHASH_H(A || pad || C || pad || [len(A)]64 || [len(C)]64) */
static int cn1_gcm_tag(const uint8_t* key, int keyLen, const uint8_t h[16], const uint8_t j0[16],
                       const uint8_t* aad, int aadLen, const uint8_t* ct, int ctLen,
                       uint8_t tag[16]) {
    uint8_t y[16];
    uint8_t lengths[16];
    memset(y, 0, 16);
    if (aad != NULL && aadLen > 0) {
        cn1_gcm_ghash(y, h, aad, (size_t) aadLen);
    }
    if (ctLen > 0) {
        cn1_gcm_ghash(y, h, ct, (size_t) ctLen);
    }
    uint64_t aadBits = ((uint64_t) (aadLen > 0 ? aadLen : 0)) * 8;
    uint64_t ctBits = ((uint64_t) ctLen) * 8;
    for (int i = 0; i < 8; i++) {
        lengths[i] = (uint8_t) (aadBits >> (56 - 8 * i));
        lengths[8 + i] = (uint8_t) (ctBits >> (56 - 8 * i));
    }
    cn1_gcm_ghash(y, h, lengths, 16);
    uint8_t mask[16];
    if (cn1_gcm_ecb_block(key, keyLen, j0, mask) != 0) {
        return CN1_CRYPTO_E_GENERIC;
    }
    for (int i = 0; i < 16; i++) {
        tag[i] = (uint8_t) (y[i] ^ mask[i]);
    }
    return 0;
}

/* AES-CTR from inc32(J0): the GCM keystream. Encrypt and decrypt are the same. */
static int cn1_gcm_ctr(const uint8_t* key, int keyLen, const uint8_t j0[16],
                       const uint8_t* in, int len, uint8_t* out) {
    if (len == 0) {
        return 0;
    }
    uint8_t counter[16];
    memcpy(counter, j0, 16);
    counter[15] = 2;
    CCCryptorRef cryptor = NULL;
    CCCryptorStatus s = CCCryptorCreateWithMode(kCCEncrypt, kCCModeCTR, kCCAlgorithmAES,
                                                ccNoPadding, counter, key, (size_t) keyLen,
                                                NULL, 0, 0, kCCModeOptionCTR_BE, &cryptor);
    if (s != kCCSuccess) {
        return CN1_CRYPTO_E_GENERIC;
    }
    size_t produced = 0;
    s = CCCryptorUpdate(cryptor, in, (size_t) len, out, (size_t) len, &produced);
    CCCryptorRelease(cryptor);
    return (s == kCCSuccess && produced == (size_t) len) ? 0 : CN1_CRYPTO_E_GENERIC;
}
#endif /* CN1_INCLUDE_CRYPTO_GCM */

int cn1_crypto_aes_gcm(int encrypt, const uint8_t* key, int keyLen,
                       const uint8_t* iv, int ivLen,
                       const uint8_t* aad, int aadLen,
                       const uint8_t* in, int inLen,
                       uint8_t* out, int outCap) {
#ifndef CN1_INCLUDE_CRYPTO_GCM
    (void) encrypt; (void) key; (void) keyLen; (void) iv; (void) ivLen;
    (void) aad; (void) aadLen; (void) in; (void) inLen; (void) out; (void) outCap;
    return CN1_CRYPTO_E_UNSUPPORTED;
#else
    if (keyLen != kCCKeySizeAES128 && keyLen != kCCKeySizeAES192 && keyLen != kCCKeySizeAES256) {
        return CN1_CRYPTO_E_BAD_KEY;
    }
    if (ivLen != 12) {
        return CN1_CRYPTO_E_BAD_INPUT;
    }
    int dataLen = encrypt ? inLen : (inLen - 16);
    if (inLen < 0 || dataLen < 0) {
        return CN1_CRYPTO_E_BAD_INPUT;
    }
    int needed = encrypt ? dataLen + 16 : dataLen;
    if (needed > outCap) {
        return CN1_CRYPTO_E_BAD_INPUT;
    }

    uint8_t zero[16];
    uint8_t h[16];
    uint8_t j0[16];
    uint8_t tag[16];
    memset(zero, 0, 16);
    if (cn1_gcm_ecb_block(key, keyLen, zero, h) != 0) {
        return CN1_CRYPTO_E_GENERIC;
    }
    memcpy(j0, iv, 12);
    j0[12] = 0;
    j0[13] = 0;
    j0[14] = 0;
    j0[15] = 1;

    if (encrypt) {
        if (cn1_gcm_ctr(key, keyLen, j0, in, dataLen, out) != 0) {
            return CN1_CRYPTO_E_GENERIC;
        }
        if (cn1_gcm_tag(key, keyLen, h, j0, aad, aadLen, out, dataLen, tag) != 0) {
            return CN1_CRYPTO_E_GENERIC;
        }
        memcpy(out + dataLen, tag, 16);
        return dataLen + 16;
    }

    // Authenticate BEFORE decrypting, so a forged message never writes unverified
    // plaintext into the caller's buffer. The tag is computed over the ciphertext,
    // which is what the input carries, and compared in constant time.
    if (cn1_gcm_tag(key, keyLen, h, j0, aad, aadLen, in, dataLen, tag) != 0) {
        return CN1_CRYPTO_E_GENERIC;
    }
    const uint8_t* expectedTag = in + dataLen;
    int diff = 0;
    for (int i = 0; i < 16; i++) diff |= (expectedTag[i] ^ tag[i]);
    if (diff != 0) {
        return CN1_CRYPTO_E_AUTH_FAIL;
    }
    if (cn1_gcm_ctr(key, keyLen, j0, in, dataLen, out) != 0) {
        return CN1_CRYPTO_E_GENERIC;
    }
    return dataLen;
#endif /* CN1_INCLUDE_CRYPTO_GCM */
}

/* --- RSA --------------------------------------------------------------- */

static SecKeyRef cn1_load_rsa_public(const uint8_t* x509, int x509Len) {
    // Strip the SubjectPublicKeyInfo wrapper: SecKeyCreateWithData on iOS only
    // accepts the bare PKCS#1 RSA public key body (modulus + exponent ASN.1).
    // We do this with a small DER parser tailored to the SPKI structure:
    //   SEQUENCE { algIdentifier SEQUENCE, BIT STRING { PKCS#1 } }
    if (x509Len < 2 || x509[0] != 0x30) return NULL;
    int p = 1;
    int seqLen, sl;
    if ((x509[p] & 0x80) == 0) { seqLen = x509[p]; p++; }
    else {
        sl = x509[p] & 0x7f; p++;
        if (sl > 4 || p + sl > x509Len) return NULL;
        seqLen = 0;
        for (int i = 0; i < sl; i++) seqLen = (seqLen << 8) | x509[p + i];
        p += sl;
    }
    (void)seqLen;
    // skip the algIdentifier inner SEQUENCE
    if (p >= x509Len || x509[p] != 0x30) return NULL;
    p++;
    int innerLen;
    if ((x509[p] & 0x80) == 0) { innerLen = x509[p]; p++; }
    else { sl = x509[p] & 0x7f; p++; innerLen = 0; for (int i = 0; i < sl; i++) innerLen = (innerLen << 8) | x509[p + i]; p += sl; }
    p += innerLen;
    // BIT STRING
    if (p >= x509Len || x509[p] != 0x03) return NULL;
    p++;
    int bitLen;
    if ((x509[p] & 0x80) == 0) { bitLen = x509[p]; p++; }
    else { sl = x509[p] & 0x7f; p++; bitLen = 0; for (int i = 0; i < sl; i++) bitLen = (bitLen << 8) | x509[p + i]; p += sl; }
    if (p >= x509Len || x509[p] != 0x00) return NULL; // unused bits
    p++;
    // The remainder is the PKCS#1 RSAPublicKey
    NSData* keyData = [NSData dataWithBytes:(x509 + p) length:(x509Len - p)];
    NSDictionary* attrs = @{
        (id) kSecAttrKeyType:  (id) kSecAttrKeyTypeRSA,
        (id) kSecAttrKeyClass: (id) kSecAttrKeyClassPublic,
        (id) kSecAttrKeySizeInBits: @(([keyData length] * 8))
    };
    CFErrorRef error = NULL;
    SecKeyRef key = SecKeyCreateWithData((__bridge CFDataRef) keyData,
                                         (__bridge CFDictionaryRef) attrs,
                                         &error);
    if (error) CFRelease(error);
    return key;
}

static SecKeyRef cn1_load_rsa_private(const uint8_t* pkcs8, int pkcs8Len) {
    // PKCS#8 wraps a PKCS#1 RSAPrivateKey. We similarly extract the inner
    // key blob. Structure: SEQUENCE { INT 0, algId, OCTET STRING { PKCS#1 } }
    if (pkcs8Len < 2 || pkcs8[0] != 0x30) return NULL;
    int p = 1, sl, len;
    if ((pkcs8[p] & 0x80) == 0) { len = pkcs8[p]; p++; }
    else { sl = pkcs8[p] & 0x7f; p++; len = 0; for (int i = 0; i < sl; i++) len = (len << 8) | pkcs8[p + i]; p += sl; }
    // INTEGER 0
    if (p >= pkcs8Len || pkcs8[p] != 0x02) return NULL;
    p++; len = pkcs8[p]; p++; p += len;
    // algIdentifier SEQUENCE
    if (p >= pkcs8Len || pkcs8[p] != 0x30) return NULL;
    p++;
    if ((pkcs8[p] & 0x80) == 0) { len = pkcs8[p]; p++; }
    else { sl = pkcs8[p] & 0x7f; p++; len = 0; for (int i = 0; i < sl; i++) len = (len << 8) | pkcs8[p + i]; p += sl; }
    p += len;
    // OCTET STRING
    if (p >= pkcs8Len || pkcs8[p] != 0x04) return NULL;
    p++;
    if ((pkcs8[p] & 0x80) == 0) { len = pkcs8[p]; p++; }
    else { sl = pkcs8[p] & 0x7f; p++; len = 0; for (int i = 0; i < sl; i++) len = (len << 8) | pkcs8[p + i]; p += sl; }
    if (p + len > pkcs8Len) return NULL;
    NSData* keyData = [NSData dataWithBytes:(pkcs8 + p) length:len];
    NSDictionary* attrs = @{
        (id) kSecAttrKeyType:  (id) kSecAttrKeyTypeRSA,
        (id) kSecAttrKeyClass: (id) kSecAttrKeyClassPrivate,
        (id) kSecAttrKeySizeInBits: @(([keyData length] * 8))
    };
    CFErrorRef error = NULL;
    SecKeyRef key = SecKeyCreateWithData((__bridge CFDataRef) keyData,
                                         (__bridge CFDictionaryRef) attrs,
                                         &error);
    if (error) CFRelease(error);
    return key;
}

static int cn1_seckey_op(SecKeyRef key, SecKeyAlgorithm alg, int forEncrypt,
                         const uint8_t* in, int inLen, uint8_t* out, int outCap) {
    if (!key) return CN1_CRYPTO_E_BAD_KEY;
    NSData* input = [NSData dataWithBytes:in length:inLen];
    CFErrorRef error = NULL;
    NSData* result;
    if (forEncrypt == 1) {
        result = (__bridge_transfer NSData*) SecKeyCreateEncryptedData(
            key, alg, (__bridge CFDataRef) input, &error);
    } else if (forEncrypt == 0) {
        result = (__bridge_transfer NSData*) SecKeyCreateDecryptedData(
            key, alg, (__bridge CFDataRef) input, &error);
    } else { /* sign */
        result = (__bridge_transfer NSData*) SecKeyCreateSignature(
            key, alg, (__bridge CFDataRef) input, &error);
    }
    if (error || !result) {
        if (error) CFRelease(error);
        return CN1_CRYPTO_E_GENERIC;
    }
    NSUInteger len = [result length];
    if ((int) len > outCap) return CN1_CRYPTO_E_BAD_INPUT;
    memcpy(out, [result bytes], len);
    return (int) len;
}

/* OAEPSHA256 uses SHA-256 for the label hash and for MGF1 alike, which is the
 * pairing the portable RSA_OAEP_SHA256 constant means. The JCE transformation
 * name it borrows leaves MGF1 on SHA-1 by default, but neither SecKey nor Web
 * Crypto can express that split, so the JavaSE and Android ports name SHA-256
 * for both explicitly and every port agrees. */
static SecKeyAlgorithm rsa_padding_alg(int paddingKind) {
    return paddingKind == 2
        ? kSecKeyAlgorithmRSAEncryptionOAEPSHA256
        : kSecKeyAlgorithmRSAEncryptionPKCS1;
}

int cn1_crypto_rsa_encrypt(int paddingKind,
                           const uint8_t* x509, int x509Len,
                           const uint8_t* in, int inLen,
                           uint8_t* out, int outCap) {
    SecKeyRef key = cn1_load_rsa_public(x509, x509Len);
    if (!key) return CN1_CRYPTO_E_BAD_KEY;
    int rc = cn1_seckey_op(key, rsa_padding_alg(paddingKind), 1, in, inLen, out, outCap);
    CFRelease(key);
    return rc;
}

int cn1_crypto_rsa_decrypt(int paddingKind,
                           const uint8_t* pkcs8, int pkcs8Len,
                           const uint8_t* in, int inLen,
                           uint8_t* out, int outCap) {
    SecKeyRef key = cn1_load_rsa_private(pkcs8, pkcs8Len);
    if (!key) return CN1_CRYPTO_E_BAD_KEY;
    int rc = cn1_seckey_op(key, rsa_padding_alg(paddingKind), 0, in, inLen, out, outCap);
    CFRelease(key);
    return rc;
}

static SecKeyAlgorithm signature_alg(int algorithm) {
    switch (algorithm) {
        case 0: return kSecKeyAlgorithmRSASignatureMessagePKCS1v15SHA256;
        case 1: return kSecKeyAlgorithmRSASignatureMessagePKCS1v15SHA384;
        case 2: return kSecKeyAlgorithmRSASignatureMessagePKCS1v15SHA512;
        case 3: return kSecKeyAlgorithmECDSASignatureMessageX962SHA256;
        case 4: return kSecKeyAlgorithmECDSASignatureMessageX962SHA384;
        case 5: return kSecKeyAlgorithmECDSASignatureMessageX962SHA512;
        default: return NULL;
    }
}

int cn1_crypto_sign(int algorithm,
                    const uint8_t* pkcs8, int pkcs8Len,
                    const uint8_t* data,  int dataLen,
                    uint8_t* out, int outCap) {
    SecKeyAlgorithm alg = signature_alg(algorithm);
    if (!alg) return CN1_CRYPTO_E_UNSUPPORTED;
    // ECDSA keys go through a separate loader; here we assume RSA. ECDSA
    // support would parse the PKCS#8 EC body and pass kSecAttrKeyTypeECSECPrimeRandom.
    SecKeyRef key = cn1_load_rsa_private(pkcs8, pkcs8Len);
    if (!key) return CN1_CRYPTO_E_BAD_KEY;
    int rc = cn1_seckey_op(key, alg, 2, data, dataLen, out, outCap);
    CFRelease(key);
    return rc;
}

int cn1_crypto_verify(int algorithm,
                      const uint8_t* x509, int x509Len,
                      const uint8_t* data, int dataLen,
                      const uint8_t* sig,  int sigLen) {
    SecKeyAlgorithm alg = signature_alg(algorithm);
    if (!alg) return CN1_CRYPTO_E_UNSUPPORTED;
    SecKeyRef key = cn1_load_rsa_public(x509, x509Len);
    if (!key) return CN1_CRYPTO_E_BAD_KEY;
    CFErrorRef error = NULL;
    NSData* dataObj = [NSData dataWithBytes:data length:dataLen];
    NSData* sigObj  = [NSData dataWithBytes:sig  length:sigLen];
    Boolean ok = SecKeyVerifySignature(key, alg,
                                       (__bridge CFDataRef) dataObj,
                                       (__bridge CFDataRef) sigObj,
                                       &error);
    if (error) CFRelease(error);
    CFRelease(key);
    return ok ? 1 : 0;
}

/* --- RSA key-pair generation ------------------------------------------ */

int cn1_crypto_generate_rsa_keypair(int bits,
                                    uint8_t* outPub,  int pubCap,  int* pubLen,
                                    uint8_t* outPriv, int privCap, int* privLen) {
    if (!pubLen || !privLen) return CN1_CRYPTO_E_BAD_INPUT;
    NSDictionary* attrs = @{
        (id) kSecAttrKeyType: (id) kSecAttrKeyTypeRSA,
        (id) kSecAttrKeySizeInBits: @(bits),
        (id) kSecPrivateKeyAttrs: @{ (id) kSecAttrIsPermanent: @NO }
    };
    CFErrorRef error = NULL;
    SecKeyRef priv = SecKeyCreateRandomKey((__bridge CFDictionaryRef) attrs, &error);
    if (!priv || error) {
        if (error) CFRelease(error);
        return CN1_CRYPTO_E_GENERIC;
    }
    SecKeyRef pub = SecKeyCopyPublicKey(priv);
    if (!pub) {
        CFRelease(priv);
        return CN1_CRYPTO_E_GENERIC;
    }

    // NB: SecKeyCopyExternalRepresentation returns the bare PKCS#1 form, NOT
    // the X.509/PKCS#8 wrapper expected by the Java API. We wrap them here.
    NSData* pubInner  = (__bridge_transfer NSData*) SecKeyCopyExternalRepresentation(pub,  &error);
    if (error) { CFRelease(error); error = NULL; }
    NSData* privInner = (__bridge_transfer NSData*) SecKeyCopyExternalRepresentation(priv, &error);
    if (error) { CFRelease(error); error = NULL; }
    CFRelease(pub);
    CFRelease(priv);
    if (!pubInner || !privInner) return CN1_CRYPTO_E_GENERIC;

    // X.509 SPKI wrapper for the public key:
    //  SEQUENCE {
    //    SEQUENCE { OID 1.2.840.113549.1.1.1, NULL },
    //    BIT STRING { <pubInner> }
    //  }
    static const uint8_t SPKI_HEADER[] = {
        0x30, 0x82, 0x00, 0x00,             // outer SEQUENCE, length filled in
        0x30, 0x0d,                          // alg SEQUENCE
        0x06, 0x09, 0x2a, 0x86, 0x48, 0x86, 0xf7, 0x0d, 0x01, 0x01, 0x01, // OID
        0x05, 0x00,                          // NULL
        0x03, 0x82, 0x00, 0x00,              // BIT STRING, length filled in
        0x00                                  // unused bits
    };
    NSUInteger pubInnerLen = [pubInner length];
    NSUInteger spkiBodyLen = sizeof(SPKI_HEADER) - 4 + pubInnerLen + 1; // +1 for unused-bits
    (void) spkiBodyLen;
    NSUInteger outerLen = (sizeof(SPKI_HEADER) - 4) + 1 + pubInnerLen; // see below
    // The SPKI structure including the BIT STRING with unused-bits 0x00 byte:
    // SEQ(len) [ SEQ(0x0d){oid+null}, BITSTRING(len_pub+1)[0x00 || pubInner] ]
    int wrappedLen = 4 /*outer header*/
                   + 2 + 0x0d /*alg seq*/
                   + 4 /*bitstring header*/ + 1 /*unused-bits byte*/
                   + (int) pubInnerLen;
    if (wrappedLen > pubCap) return CN1_CRYPTO_E_BAD_INPUT;
    uint8_t* p = outPub;
    int outerInteriorLen = wrappedLen - 4; // minus outer header
    *p++ = 0x30; *p++ = 0x82;
    *p++ = (uint8_t) (outerInteriorLen >> 8);
    *p++ = (uint8_t) (outerInteriorLen & 0xff);
    *p++ = 0x30; *p++ = 0x0d;
    static const uint8_t oid[] = { 0x06, 0x09, 0x2a, 0x86, 0x48, 0x86, 0xf7, 0x0d, 0x01, 0x01, 0x01, 0x05, 0x00 };
    memcpy(p, oid, sizeof(oid)); p += sizeof(oid);
    int bitStringLen = 1 + (int) pubInnerLen;
    *p++ = 0x03; *p++ = 0x82;
    *p++ = (uint8_t) (bitStringLen >> 8);
    *p++ = (uint8_t) (bitStringLen & 0xff);
    *p++ = 0x00;
    memcpy(p, [pubInner bytes], pubInnerLen);
    *pubLen = wrappedLen;

    // PKCS#8 wrapper for the private key:
    //  SEQUENCE { INT 0, SEQUENCE{OID, NULL}, OCTET STRING { privInner } }
    NSUInteger privInnerLen = [privInner length];
    int p8BodyLen = 3 /*INT 0*/ + 15 /*algSeq*/ + 4 /*OCTET header*/ + (int) privInnerLen;
    int p8TotalLen = 4 + p8BodyLen;
    if (p8TotalLen > privCap) return CN1_CRYPTO_E_BAD_INPUT;
    uint8_t* q = outPriv;
    *q++ = 0x30; *q++ = 0x82;
    *q++ = (uint8_t) (p8BodyLen >> 8);
    *q++ = (uint8_t) (p8BodyLen & 0xff);
    *q++ = 0x02; *q++ = 0x01; *q++ = 0x00;
    *q++ = 0x30; *q++ = 0x0d;
    memcpy(q, oid, sizeof(oid)); q += sizeof(oid);
    *q++ = 0x04; *q++ = 0x82;
    *q++ = (uint8_t) (privInnerLen >> 8);
    *q++ = (uint8_t) (privInnerLen & 0xff);
    memcpy(q, [privInner bytes], privInnerLen);
    *privLen = p8TotalLen;
    return 0;
}

#else /* CN1_INCLUDE_CRYPTO */

/*
 * When the user's app never references com.codename1.security.* the build
 * system leaves CN1_INCLUDE_CRYPTO undefined and we drop in stub versions of
 * the exported functions. The stubs let the IOSNative C bridge link against
 * something, but none of CommonCrypto's encryption symbols (and especially
 * none of the AES-GCM SPI symbols) end up referenced by the binary -- which
 * keeps Apple's static-symbol scanner happy.
 */
#include <string.h>

int cn1_crypto_secure_random(uint8_t* out, int len) {
    (void) out; (void) len;
    return CN1_CRYPTO_E_UNSUPPORTED;
}
int cn1_crypto_aes_cbc(int e, const uint8_t* k, int kl, const uint8_t* iv,
                       const uint8_t* in, int inLen, uint8_t* out, int outCap, int pad) {
    (void) e; (void) k; (void) kl; (void) iv; (void) in; (void) inLen; (void) out; (void) outCap; (void) pad;
    return CN1_CRYPTO_E_UNSUPPORTED;
}
int cn1_crypto_aes_gcm(int e, const uint8_t* k, int kl, const uint8_t* iv, int ivl,
                       const uint8_t* aad, int aadl, const uint8_t* in, int inl,
                       uint8_t* out, int outCap) {
    (void) e; (void) k; (void) kl; (void) iv; (void) ivl; (void) aad; (void) aadl;
    (void) in; (void) inl; (void) out; (void) outCap;
    return CN1_CRYPTO_E_UNSUPPORTED;
}
int cn1_crypto_rsa_encrypt(int p, const uint8_t* x, int xl, const uint8_t* in, int inl, uint8_t* out, int outCap) {
    (void) p; (void) x; (void) xl; (void) in; (void) inl; (void) out; (void) outCap;
    return CN1_CRYPTO_E_UNSUPPORTED;
}
int cn1_crypto_rsa_decrypt(int p, const uint8_t* k, int kl, const uint8_t* in, int inl, uint8_t* out, int outCap) {
    (void) p; (void) k; (void) kl; (void) in; (void) inl; (void) out; (void) outCap;
    return CN1_CRYPTO_E_UNSUPPORTED;
}
int cn1_crypto_sign(int a, const uint8_t* k, int kl, const uint8_t* d, int dl, uint8_t* out, int outCap) {
    (void) a; (void) k; (void) kl; (void) d; (void) dl; (void) out; (void) outCap;
    return CN1_CRYPTO_E_UNSUPPORTED;
}
int cn1_crypto_verify(int a, const uint8_t* x, int xl, const uint8_t* d, int dl, const uint8_t* s, int sl) {
    (void) a; (void) x; (void) xl; (void) d; (void) dl; (void) s; (void) sl;
    return CN1_CRYPTO_E_UNSUPPORTED;
}
int cn1_crypto_generate_rsa_keypair(int bits, uint8_t* outPub, int pubCap, int* pubLen,
                                    uint8_t* outPriv, int privCap, int* privLen) {
    (void) bits; (void) outPub; (void) pubCap; (void) outPriv; (void) privCap;
    if (pubLen) *pubLen = 0;
    if (privLen) *privLen = 0;
    return CN1_CRYPTO_E_UNSUPPORTED;
}

#endif /* CN1_INCLUDE_CRYPTO */
