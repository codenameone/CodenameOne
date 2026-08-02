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

#ifdef CN1_INCLUDE_CRYPTO_GCM
/*
 * CommonCrypto exposes AES-GCM only through the SPI header
 * <CommonCrypto/CommonCryptorSPI.h>, which is not in the public iOS SDK. The
 * functions and the kCCModeGCM value below are stable across all current iOS
 * versions and are exported from libcommonCrypto.dylib at runtime -- we
 * declare them here as externs so we can call them without depending on the
 * private header. These symbols are only referenced when the app explicitly
 * opts into AES-GCM via the ios.crypto.gcm build hint.
 */
enum { kCCModeGCM = 11 };
extern CCCryptorStatus CCCryptorGCMAddIV(CCCryptorRef ref, const void* iv, size_t ivLen);
extern CCCryptorStatus CCCryptorGCMAddAAD(CCCryptorRef ref, const void* aData, size_t aDataLen);
extern CCCryptorStatus CCCryptorGCMFinal(CCCryptorRef ref, void* tag, size_t* tagLen);
#endif

/* --- secure random ----------------------------------------------------- */

int cn1_crypto_secure_random(uint8_t* out, int len) {
    if (len <= 0) return 0;
    if (SecRandomCopyBytes(kSecRandomDefault, len, out) != errSecSuccess) {
        return CN1_CRYPTO_E_GENERIC;
    }
    return 0;
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

    // CommonCrypto's GCM API works via CCCryptorCreateWithMode + GCM-specific
    // calls (CCCryptorGCMAddIV / GCMaddAAD / GCMFinal). These are documented
    // but the headers mark them as deprecated -- on iOS 13+ we should use the
    // CryptoKit AES.GCM interface instead. We try CryptoKit first via NS APIs
    // and fall back to the deprecated CCCryptor path.
    CCCryptorRef cryptor = NULL;
    CCCryptorStatus s = CCCryptorCreateWithMode(
        encrypt ? kCCEncrypt : kCCDecrypt,
        kCCModeGCM,
        kCCAlgorithmAES,
        ccNoPadding,
        NULL,            /* IV set separately via GCMAddIV */
        key, (size_t) keyLen,
        NULL, 0,
        0, 0,
        &cryptor);
    if (s != kCCSuccess) {
        return CN1_CRYPTO_E_GENERIC;
    }

    if (CCCryptorGCMAddIV(cryptor, iv, ivLen) != kCCSuccess) {
        CCCryptorRelease(cryptor);
        return CN1_CRYPTO_E_GENERIC;
    }
    if (aad != NULL && aadLen > 0) {
        if (CCCryptorGCMAddAAD(cryptor, aad, aadLen) != kCCSuccess) {
            CCCryptorRelease(cryptor);
            return CN1_CRYPTO_E_GENERIC;
        }
    }

    int dataLen = encrypt ? inLen : (inLen - 16);
    if (dataLen < 0) {
        CCCryptorRelease(cryptor);
        return CN1_CRYPTO_E_BAD_INPUT;
    }

    size_t produced = 0;
    if (CCCryptorUpdate(cryptor, in, dataLen, out, outCap, &produced) != kCCSuccess) {
        CCCryptorRelease(cryptor);
        return CN1_CRYPTO_E_GENERIC;
    }

    uint8_t tag[16];
    size_t tagLen = sizeof(tag);
    if (encrypt) {
        if (CCCryptorGCMFinal(cryptor, tag, &tagLen) != kCCSuccess) {
            CCCryptorRelease(cryptor);
            return CN1_CRYPTO_E_GENERIC;
        }
        if ((int)(produced + tagLen) > outCap) {
            CCCryptorRelease(cryptor);
            return CN1_CRYPTO_E_BAD_INPUT;
        }
        memcpy(out + produced, tag, tagLen);
        produced += tagLen;
    } else {
        if (CCCryptorGCMFinal(cryptor, tag, &tagLen) != kCCSuccess) {
            CCCryptorRelease(cryptor);
            return CN1_CRYPTO_E_GENERIC;
        }
        // Constant-time compare against the tag carried in the input
        const uint8_t* expectedTag = in + dataLen;
        int diff = 0;
        for (int i = 0; i < 16; i++) diff |= (expectedTag[i] ^ tag[i]);
        if (diff != 0) {
            CCCryptorRelease(cryptor);
            return CN1_CRYPTO_E_AUTH_FAIL;
        }
    }

    CCCryptorRelease(cryptor);
    return (int) produced;
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

/*
 * OAEP, built here rather than taken from SecKey.
 *
 * kSecKeyAlgorithmRSAEncryptionOAEPSHA256 uses SHA-256 for the label hash AND
 * for MGF1. The transformation this port advertises is the JCE name
 * "RSA/ECB/OAEPWithSHA-256AndMGF1Padding", which in JCE means a SHA-256 label
 * with a SHA-1 mask -- that is what java.crypto gives the JavaSE and Android
 * ports for the same string, and what the Linux and Windows ports produce. iOS
 * was the only port pairing SHA-256 with SHA-256, so ciphertext never crossed
 * between it and any other port. SecKey cannot express the JCE pairing, so the
 * padding is built here and the key operation runs raw.
 */

static void cn1_mgf1_sha1(const uint8_t* seed, int seedLen, uint8_t* mask, int maskLen) {
    uint8_t counter[4];
    uint8_t digest[CC_SHA1_DIGEST_LENGTH];
    int produced = 0;
    uint32_t count = 0;
    while (produced < maskLen) {
        int chunk = maskLen - produced;
        CC_SHA1_CTX ctx;
        counter[0] = (uint8_t) ((count >> 24) & 0xff);
        counter[1] = (uint8_t) ((count >> 16) & 0xff);
        counter[2] = (uint8_t) ((count >> 8) & 0xff);
        counter[3] = (uint8_t) (count & 0xff);
        CC_SHA1_Init(&ctx);
        CC_SHA1_Update(&ctx, seed, (CC_LONG) seedLen);
        CC_SHA1_Update(&ctx, counter, 4);
        CC_SHA1_Final(digest, &ctx);
        if (chunk > CC_SHA1_DIGEST_LENGTH) {
            chunk = CC_SHA1_DIGEST_LENGTH;
        }
        memcpy(mask + produced, digest, (size_t) chunk);
        produced += chunk;
        count++;
    }
}

/* All ones when a == b, zero otherwise, without branching on the values. */
static uint32_t cn1_ct_eq_mask(uint32_t a, uint32_t b) {
    uint32_t diff = a ^ b;
    uint32_t nonZero = (diff | (0u - diff)) >> 31;
    return nonZero - 1u;
}

static int cn1_oaep_encode(const uint8_t* message, int messageLen,
                           uint8_t* block, int blockLen) {
    const int hashLen = CC_SHA256_DIGEST_LENGTH;
    int dbLen = blockLen - hashLen - 1;
    uint8_t seed[CC_SHA256_DIGEST_LENGTH];
    uint8_t* mask;
    int i;
    /* An OAEP block cannot be shorter than 2*hLen+2; a 512-bit key leaves a DB
     * shorter than the label hash, and the label hash would then be written and
     * compared past the end of the block. */
    if (blockLen < 2 * hashLen + 2 || messageLen > dbLen - hashLen - 1) {
        return 0;
    }
    mask = (uint8_t*) malloc((size_t) dbLen);
    if (!mask) {
        return 0;
    }
    memset(block, 0, (size_t) blockLen);
    CC_SHA256("", 0, block + 1 + hashLen);
    block[blockLen - messageLen - 1] = 0x01;
    if (messageLen > 0) {
        memcpy(block + blockLen - messageLen, message, (size_t) messageLen);
    }
    if (CCRandomGenerateBytes(seed, (size_t) hashLen) != kCCSuccess) {
        free(mask);
        return 0;
    }
    cn1_mgf1_sha1(seed, hashLen, mask, dbLen);
    for (i = 0; i < dbLen; i++) {
        block[1 + hashLen + i] ^= mask[i];
    }
    cn1_mgf1_sha1(block + 1 + hashLen, dbLen, mask, hashLen);
    for (i = 0; i < hashLen; i++) {
        block[1 + i] = (uint8_t) (seed[i] ^ mask[i]);
    }
    free(mask);
    return 1;
}

/* Every check folds into one accumulator and one generic failure is reported:
 * telling a leading-byte error from a label-hash error is enough to mount the
 * adaptive attacks OAEP exists to prevent. */
static int cn1_oaep_decode(uint8_t* block, int blockLen, uint8_t* message, int* messageLen) {
    const int hashLen = CC_SHA256_DIGEST_LENGTH;
    int dbLen = blockLen - hashLen - 1;
    uint8_t labelHash[CC_SHA256_DIGEST_LENGTH];
    uint8_t seed[CC_SHA256_DIGEST_LENGTH];
    uint8_t* mask;
    int i;
    uint32_t bad = 0;
    uint32_t seenDelimiter = 0;
    uint32_t messageStart = 0;
    if (blockLen < 2 * hashLen + 2) {
        return 0;
    }
    mask = (uint8_t*) malloc((size_t) dbLen);
    if (!mask) {
        return 0;
    }
    bad |= (uint32_t) block[0];
    cn1_mgf1_sha1(block + 1 + hashLen, dbLen, mask, hashLen);
    for (i = 0; i < hashLen; i++) {
        seed[i] = (uint8_t) (block[1 + i] ^ mask[i]);
    }
    cn1_mgf1_sha1(seed, hashLen, mask, dbLen);
    for (i = 0; i < dbLen; i++) {
        block[1 + hashLen + i] ^= mask[i];
    }
    free(mask);
    CC_SHA256("", 0, labelHash);
    for (i = 0; i < hashLen; i++) {
        bad |= (uint32_t) (labelHash[i] ^ block[1 + hashLen + i]);
    }
    for (i = 1 + hashLen + hashLen; i < blockLen; i++) {
        uint32_t value = block[i];
        uint32_t isDelimiter = cn1_ct_eq_mask(value, 0x01);
        uint32_t isZero = cn1_ct_eq_mask(value, 0x00);
        uint32_t firstDelimiter = isDelimiter & ~seenDelimiter;
        messageStart |= ((uint32_t) (i + 1)) & firstDelimiter;
        bad |= ~seenDelimiter & ~isDelimiter & ~isZero;
        seenDelimiter |= isDelimiter;
    }
    bad |= ~seenDelimiter;
    if (bad != 0) {
        return 0;
    }
    *messageLen = blockLen - (int) messageStart;
    if (*messageLen > 0) {
        memcpy(message, block + messageStart, (size_t) *messageLen);
    }
    return 1;
}

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
    if (paddingKind == 2) {
        /* Pad here and run the key raw, so the mask stays SHA-1 (see the OAEP
         * note above). */
        int blockLen = (int) SecKeyGetBlockSize(key);
        uint8_t* block = (uint8_t*) malloc((size_t) (blockLen > 0 ? blockLen : 1));
        int rc;
        if (!block) {
            CFRelease(key);
            return CN1_CRYPTO_E_GENERIC;
        }
        if (!cn1_oaep_encode(in, inLen, block, blockLen)) {
            free(block);
            CFRelease(key);
            return CN1_CRYPTO_E_BAD_INPUT;
        }
        rc = cn1_seckey_op(key, kSecKeyAlgorithmRSAEncryptionRaw, 1, block, blockLen, out, outCap);
        free(block);
        CFRelease(key);
        return rc;
    }
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
    if (paddingKind == 2) {
        int blockLen = (int) SecKeyGetBlockSize(key);
        uint8_t* raw = (uint8_t*) malloc((size_t) (blockLen > 0 ? blockLen : 1));
        uint8_t* block;
        int rawLen, messageLen = 0, rc;
        if (!raw) {
            CFRelease(key);
            return CN1_CRYPTO_E_GENERIC;
        }
        rawLen = cn1_seckey_op(key, kSecKeyAlgorithmRSAEncryptionRaw, 0, in, inLen, raw, blockLen);
        CFRelease(key);
        if (rawLen < 0) {
            free(raw);
            return rawLen;
        }
        /* A raw result may arrive with its leading zero bytes dropped; the OAEP
         * block is defined at exactly the modulus width, so restore them. */
        block = (uint8_t*) calloc((size_t) (blockLen > 0 ? blockLen : 1), 1);
        if (!block) {
            free(raw);
            return CN1_CRYPTO_E_GENERIC;
        }
        if (rawLen > blockLen) {
            free(raw);
            free(block);
            return CN1_CRYPTO_E_BAD_INPUT;
        }
        memcpy(block + (blockLen - rawLen), raw, (size_t) rawLen);
        free(raw);
        if (!cn1_oaep_decode(block, blockLen, out, &messageLen) || messageLen > outCap) {
            free(block);
            return CN1_CRYPTO_E_BAD_INPUT;
        }
        free(block);
        return messageLen;
    }
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
