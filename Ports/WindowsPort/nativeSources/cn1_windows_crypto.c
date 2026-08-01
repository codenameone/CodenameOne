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

/*
 * The crypto half of com.codename1.impl.CodenameOneImplementation on Windows.
 *
 * CNG (bcrypt) provides the primitives and crypt32 the ASN.1: key material
 * crosses this boundary in the encodings the portable API documents -- X.509
 * SubjectPublicKeyInfo and PKCS#8 PrivateKeyInfo -- and CryptDecodeObjectEx /
 * CryptEncodeObjectEx translate those to and from the BCRYPT_RSAKEY_BLOB form
 * bcrypt wants, so no ASN.1 is parsed here by hand.
 *
 * Every entry point answers null (or false) on failure and records the status
 * for lastCryptoError, which the Java side turns into the CryptoException
 * message. A silent empty result would look like a successful encryption of
 * nothing.
 */

#include "cn1_windows.h"
#include <windows.h>
#include <bcrypt.h>
#include <wincrypt.h>
#include <ncrypt.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#ifndef STATUS_SUCCESS
#define STATUS_SUCCESS ((NTSTATUS) 0x00000000L)
#endif
#ifndef STATUS_AUTH_TAG_MISMATCH
#define STATUS_AUTH_TAG_MISMATCH ((NTSTATUS) 0xC000A002L)
#endif

#define CN1_GCM_TAG_BYTES 16

/* Per-thread: crypto failures on different threads would otherwise overwrite
 * each other and lastCryptoError() could answer with an unrelated call's
 * message. */
static __declspec(thread) char cn1WinCryptoError[512];

static void cn1CryptoFail(const char* what, NTSTATUS status) {
    _snprintf(cn1WinCryptoError, sizeof(cn1WinCryptoError), "%s (status 0x%08lx)", what,
              (unsigned long) status);
    cn1WinCryptoError[sizeof(cn1WinCryptoError) - 1] = 0;
}

static void cn1CryptoFailLast(const char* what) {
    cn1CryptoFail(what, (NTSTATUS) GetLastError());
}

JAVA_OBJECT com_codename1_impl_windows_WindowsNative_lastCryptoError___R_java_lang_String(CODENAME_ONE_THREAD_STATE) {
    return newStringFromCString(threadStateData,
                                cn1WinCryptoError[0] ? cn1WinCryptoError : "unknown crypto error");
}

/* The Windows port has no shared byte-array helper, so keep a local one that
 * matches how the rest of the port allocates arrays. */
static JAVA_OBJECT cn1WinNewByteArray(CODENAME_ONE_THREAD_STATE, const void* src, int n) {
    JAVA_OBJECT array;
    if (n < 0) {
        n = 0;
    }
    array = allocArray(threadStateData, n, &class_array1__JAVA_BYTE, sizeof(JAVA_ARRAY_BYTE), 1);
    if (array != JAVA_NULL && n > 0 && src != 0) {
        memcpy((*(JAVA_ARRAY) array).data, src, (size_t) n);
    }
    return array;
}

static unsigned char* cn1Bytes(JAVA_OBJECT array, int* length) {
    if (array == JAVA_NULL) {
        *length = 0;
        return 0;
    }
    *length = (int) (*(JAVA_ARRAY) array).length;
    return (unsigned char*) (*(JAVA_ARRAY) array).data;
}

/* ------------------------------------------------------------ random */

JAVA_BOOLEAN com_codename1_impl_windows_WindowsNative_secureRandomBytes___byte_1ARRAY_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT out) {
    int length = 0;
    unsigned char* data = cn1Bytes(out, &length);
    NTSTATUS status;
    if (data == 0 || length <= 0) {
        return JAVA_TRUE;
    }
    status = BCryptGenRandom(NULL, data, (ULONG) length, BCRYPT_USE_SYSTEM_PREFERRED_RNG);
    if (status != STATUS_SUCCESS) {
        /* Report the failure rather than leaving the buffer as it stands:
         * KeyGenerator hands this straight back as key material, so a quiet
         * return would mint a predictable key. */
        cn1CryptoFail("secure random", status);
        memset(data, 0, (size_t) length);
        return JAVA_FALSE;
    }
    return JAVA_TRUE;
}

/* ------------------------------------------------------------ AES */

JAVA_OBJECT com_codename1_impl_windows_WindowsNative_aesCrypt___java_lang_String_boolean_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_R_byte_1ARRAY(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT transformation, JAVA_BOOLEAN encrypt,
        JAVA_OBJECT keyArray, JAVA_OBJECT ivArray, JAVA_OBJECT aadArray, JAVA_OBJECT dataArray) {
    const char* mode = transformation == JAVA_NULL ? "" : stringToUTF8(threadStateData, transformation);
    int keyLength = 0, ivLength = 0, aadLength = 0, dataLength = 0;
    unsigned char* key = cn1Bytes(keyArray, &keyLength);
    unsigned char* iv = cn1Bytes(ivArray, &ivLength);
    unsigned char* aad = cn1Bytes(aadArray, &aadLength);
    unsigned char* data = cn1Bytes(dataArray, &dataLength);
    int gcm = strstr(mode, "/GCM/") != 0;
    int ecb = strstr(mode, "/ECB/") != 0;
    int padded = strstr(mode, "NoPadding") == 0;
    int bodyLength = dataLength;
    BCRYPT_ALG_HANDLE alg = NULL;
    BCRYPT_KEY_HANDLE handle = NULL;
    NTSTATUS status;
    unsigned char* out = 0;
    unsigned char* ivCopy = 0;
    ULONG outLength = 0, produced = 0;
    JAVA_OBJECT result = JAVA_NULL;
    BCRYPT_AUTHENTICATED_CIPHER_MODE_INFO auth;
    unsigned char tag[CN1_GCM_TAG_BYTES];

    /* A missing GCM nonce would otherwise repeat across messages under one
     * key, which destroys the mode, and a short CBC IV is read as a whole
     * block. */
    if (gcm && ivLength <= 0) {
        cn1CryptoFail("AES-GCM requires a nonce", 0);
        return JAVA_NULL;
    }
    if (!gcm && !ecb && ivLength != 16) {
        cn1CryptoFail("AES-CBC requires a 16 byte initialization vector", 0);
        return JAVA_NULL;
    }
    if (gcm && !encrypt) {
        if (dataLength < CN1_GCM_TAG_BYTES) {
            cn1CryptoFail("AES-GCM input is shorter than its authentication tag", 0);
            return JAVA_NULL;
        }
        bodyLength = dataLength - CN1_GCM_TAG_BYTES;
    }

    status = BCryptOpenAlgorithmProvider(&alg, BCRYPT_AES_ALGORITHM, NULL, 0);
    if (status != STATUS_SUCCESS) {
        cn1CryptoFail("AES provider", status);
        return JAVA_NULL;
    }
    status = BCryptSetProperty(alg, BCRYPT_CHAINING_MODE,
                               gcm ? (PUCHAR) BCRYPT_CHAIN_MODE_GCM
                                   : (ecb ? (PUCHAR) BCRYPT_CHAIN_MODE_ECB
                                          : (PUCHAR) BCRYPT_CHAIN_MODE_CBC),
                               gcm ? sizeof(BCRYPT_CHAIN_MODE_GCM)
                                   : (ecb ? sizeof(BCRYPT_CHAIN_MODE_ECB)
                                          : sizeof(BCRYPT_CHAIN_MODE_CBC)),
                               0);
    if (status != STATUS_SUCCESS) {
        cn1CryptoFail("AES chaining mode", status);
        goto done;
    }
    status = BCryptGenerateSymmetricKey(alg, &handle, NULL, 0, key, (ULONG) keyLength, 0);
    if (status != STATUS_SUCCESS) {
        cn1CryptoFail("AES key", status);
        goto done;
    }

    if (gcm) {
        BCRYPT_INIT_AUTH_MODE_INFO(auth);
        auth.pbNonce = iv;
        auth.cbNonce = (ULONG) ivLength;
        auth.pbAuthData = aadLength > 0 ? aad : NULL;
        auth.cbAuthData = (ULONG) aadLength;
        if (encrypt) {
            auth.pbTag = tag;
            auth.cbTag = CN1_GCM_TAG_BYTES;
        } else {
            auth.pbTag = data + bodyLength;
            auth.cbTag = CN1_GCM_TAG_BYTES;
        }
        status = encrypt
                ? BCryptEncrypt(handle, data, (ULONG) bodyLength, &auth, NULL, 0, NULL, 0, &outLength, 0)
                : BCryptDecrypt(handle, data, (ULONG) bodyLength, &auth, NULL, 0, NULL, 0, &outLength, 0);
        if (status != STATUS_SUCCESS) {
            cn1CryptoFail("AES-GCM size", status);
            goto done;
        }
        out = (unsigned char*) malloc((size_t) outLength + CN1_GCM_TAG_BYTES + 1);
        if (out == 0) {
            cn1CryptoFail("out of memory", 0);
            goto done;
        }
        status = encrypt
                ? BCryptEncrypt(handle, data, (ULONG) bodyLength, &auth, NULL, 0, out, outLength, &produced, 0)
                : BCryptDecrypt(handle, data, (ULONG) bodyLength, &auth, NULL, 0, out, outLength, &produced, 0);
        if (status != STATUS_SUCCESS) {
            /* A tampered message or wrong associated data lands here. */
            cn1CryptoFail(status == STATUS_AUTH_TAG_MISMATCH
                          ? "AES-GCM authentication failed" : "AES-GCM", status);
            goto done;
        }
        if (encrypt) {
            memcpy(out + produced, tag, CN1_GCM_TAG_BYTES);
            produced += CN1_GCM_TAG_BYTES;
        }
    } else {
        ULONG flags = padded ? BCRYPT_BLOCK_PADDING : 0;
        /* CBC updates the IV in place, so hand the cipher its own copy. */
        if (ivLength > 0) {
            ivCopy = (unsigned char*) malloc((size_t) ivLength);
            if (ivCopy == 0) {
                cn1CryptoFail("out of memory", 0);
                goto done;
            }
            memcpy(ivCopy, iv, (size_t) ivLength);
        }
        status = encrypt
                ? BCryptEncrypt(handle, data, (ULONG) bodyLength, NULL, ivCopy, (ULONG) ivLength,
                                NULL, 0, &outLength, flags)
                : BCryptDecrypt(handle, data, (ULONG) bodyLength, NULL, ivCopy, (ULONG) ivLength,
                                NULL, 0, &outLength, flags);
        if (status != STATUS_SUCCESS) {
            cn1CryptoFail("AES size", status);
            goto done;
        }
        out = (unsigned char*) malloc((size_t) outLength + 1);
        if (out == 0) {
            cn1CryptoFail("out of memory", 0);
            goto done;
        }
        /* The size query above consumed the IV copy; restore it. */
        if (ivLength > 0) {
            memcpy(ivCopy, iv, (size_t) ivLength);
        }
        status = encrypt
                ? BCryptEncrypt(handle, data, (ULONG) bodyLength, NULL, ivCopy, (ULONG) ivLength,
                                out, outLength, &produced, flags)
                : BCryptDecrypt(handle, data, (ULONG) bodyLength, NULL, ivCopy, (ULONG) ivLength,
                                out, outLength, &produced, flags);
        if (status != STATUS_SUCCESS) {
            cn1CryptoFail("AES", status);
            goto done;
        }
    }
    result = cn1WinNewByteArray(threadStateData, out, (int) produced);

done:
    free(out);
    free(ivCopy);
    if (handle != NULL) {
        BCryptDestroyKey(handle);
    }
    if (alg != NULL) {
        BCryptCloseAlgorithmProvider(alg, 0);
    }
    return result;
}

/* ------------------------------------------------------------ RSA keys */

static BCRYPT_KEY_HANDLE cn1PublicKey(const unsigned char* der, int length) {
    CERT_PUBLIC_KEY_INFO* info = 0;
    DWORD infoLength = 0;
    BCRYPT_KEY_HANDLE key = NULL;
    if (!CryptDecodeObjectEx(X509_ASN_ENCODING, X509_PUBLIC_KEY_INFO, der, (DWORD) length,
                             CRYPT_DECODE_ALLOC_FLAG, NULL, &info, &infoLength)) {
        cn1CryptoFailLast("public key is not X.509 SubjectPublicKeyInfo DER");
        return NULL;
    }
    if (!CryptImportPublicKeyInfoEx2(X509_ASN_ENCODING, info, 0, NULL, &key)) {
        cn1CryptoFailLast("public key import");
        key = NULL;
    }
    LocalFree(info);
    return key;
}

/* Imports a PKCS#8 private key of either supported kind.
 *
 * The earlier version always decoded CNG_RSA_PRIVATE_KEY_BLOB, so an EC key
 * failed to import and ECDSA signing could never work. NCrypt takes PKCS#8
 * directly and reads the algorithm out of the key itself, which covers RSA and
 * EC with one path; *isEc reports which arrived so the caller can pick the
 * matching padding.
 */
static NCRYPT_KEY_HANDLE cn1PrivateKey(const unsigned char* der, int length, int* isEc) {
    NCRYPT_PROV_HANDLE provider = 0;
    NCRYPT_KEY_HANDLE key = 0;
    SECURITY_STATUS status;
    WCHAR algorithm[64];
    DWORD algorithmBytes = 0;

    if (isEc != 0) {
        *isEc = 0;
    }
    status = NCryptOpenStorageProvider(&provider, MS_KEY_STORAGE_PROVIDER, 0);
    if (status != ERROR_SUCCESS) {
        cn1CryptoFail("key storage provider", (NTSTATUS) status);
        return 0;
    }
    status = NCryptImportKey(provider, 0, NCRYPT_PKCS8_PRIVATE_KEY_BLOB, NULL, &key,
                             (PBYTE) der, (DWORD) length, NCRYPT_DO_NOT_FINALIZE_FLAG);
    if (status != ERROR_SUCCESS) {
        /* Retry without the no-finalize hint: ephemeral keys import directly. */
        status = NCryptImportKey(provider, 0, NCRYPT_PKCS8_PRIVATE_KEY_BLOB, NULL, &key,
                                 (PBYTE) der, (DWORD) length, 0);
    } else {
        status = NCryptFinalizeKey(key, 0);
    }
    NCryptFreeObject(provider);
    if (status != ERROR_SUCCESS || key == 0) {
        cn1CryptoFail("private key is not PKCS#8 DER", (NTSTATUS) status);
        if (key != 0) {
            NCryptFreeObject(key);
        }
        return 0;
    }
    if (isEc != 0 &&
        NCryptGetProperty(key, NCRYPT_ALGORITHM_GROUP_PROPERTY, (PBYTE) algorithm,
                          sizeof(algorithm), &algorithmBytes, 0) == ERROR_SUCCESS) {
        *isEc = wcscmp(algorithm, NCRYPT_ECDSA_ALGORITHM_GROUP) == 0
                || wcscmp(algorithm, NCRYPT_ECDH_ALGORITHM_GROUP) == 0;
    }
    return key;
}

/* True when an X.509 SubjectPublicKeyInfo carries an elliptic-curve key. */
static int cn1PublicKeyIsEc(const unsigned char* der, int length) {
    CERT_PUBLIC_KEY_INFO* info = 0;
    DWORD infoLength = 0;
    int isEc = 0;
    if (CryptDecodeObjectEx(X509_ASN_ENCODING, X509_PUBLIC_KEY_INFO, der, (DWORD) length,
                            CRYPT_DECODE_ALLOC_FLAG, NULL, &info, &infoLength)) {
        isEc = info->Algorithm.pszObjId != 0
                && strcmp(info->Algorithm.pszObjId, szOID_ECC_PUBLIC_KEY) == 0;
        LocalFree(info);
    }
    return isEc;
}

static LPCWSTR cn1DigestAlgorithm(const char* algorithm) {
    if (strstr(algorithm, "SHA512") != 0 || strstr(algorithm, "SHA-512") != 0) {
        return BCRYPT_SHA512_ALGORITHM;
    }
    if (strstr(algorithm, "SHA384") != 0 || strstr(algorithm, "SHA-384") != 0) {
        return BCRYPT_SHA384_ALGORITHM;
    }
    if (strstr(algorithm, "SHA1") != 0 || strstr(algorithm, "SHA-1") != 0) {
        return BCRYPT_SHA1_ALGORITHM;
    }
    return BCRYPT_SHA256_ALGORITHM;
}

static int cn1DigestLength(LPCWSTR algorithm) {
    if (wcscmp(algorithm, BCRYPT_SHA512_ALGORITHM) == 0) {
        return 64;
    }
    if (wcscmp(algorithm, BCRYPT_SHA384_ALGORITHM) == 0) {
        return 48;
    }
    if (wcscmp(algorithm, BCRYPT_SHA1_ALGORITHM) == 0) {
        return 20;
    }
    return 32;
}

/* Hashes with the named algorithm into caller-provided storage. */
static int cn1Digest(LPCWSTR algorithm, const unsigned char* data, int length,
                     unsigned char* digest, int digestLength) {
    BCRYPT_ALG_HANDLE alg = NULL;
    NTSTATUS status = BCryptOpenAlgorithmProvider(&alg, algorithm, NULL, 0);
    if (status != STATUS_SUCCESS) {
        cn1CryptoFail("digest provider", status);
        return 0;
    }
    status = BCryptHash(alg, NULL, 0, (PUCHAR) data, (ULONG) length, digest, (ULONG) digestLength);
    BCryptCloseAlgorithmProvider(alg, 0);
    if (status != STATUS_SUCCESS) {
        cn1CryptoFail("digest", status);
        return 0;
    }
    return 1;
}


/* ------------------------------------------------- OAEP and ECDSA encodings
 *
 * Two shapes CNG cannot produce on its own:
 *
 * OAEP -- BCRYPT_OAEP_PADDING_INFO carries one digest, which CNG uses for both
 * the label hash and the mask function. The JCE providers behind the JavaSE and
 * Android ports pair a SHA-256 label hash with a SHA-1 mask for
 * "OAEPWithSHA-256AndMGF1Padding", and the Linux port matches them, so
 * ciphertext has to use that pairing to stay readable across ports. Naming one
 * digest for both halves either weakens the label hash or breaks interop, so
 * the padding is built here and the key operation runs unpadded.
 *
 * ECDSA -- NCryptSignHash answers the fixed-width r||s of P1363, while the
 * portable Signature contract (and Jwt.derToJoseEcdsa) expects ASN.1 DER, so
 * signatures are converted in both directions.
 */

/* One digest over two buffers in sequence, without joining them first.
 *
 * MGF1's second call seeds from the whole masked DB -- 351 bytes for a
 * 3072-bit key and 479 for a 4096-bit one, both of which KeyGenerator.rsa()
 * supports -- so the seed cannot be staged in a buffer sized for a hash. This
 * feeds the seed and the counter to the hash object directly instead. */
static int cn1DigestPair(LPCWSTR algorithm, const unsigned char* first, int firstLength,
                         const unsigned char* second, int secondLength,
                         unsigned char* digest, int digestLength) {
    BCRYPT_ALG_HANDLE alg = NULL;
    BCRYPT_HASH_HANDLE hash = NULL;
    NTSTATUS status = BCryptOpenAlgorithmProvider(&alg, algorithm, NULL, 0);
    if (status != STATUS_SUCCESS) {
        cn1CryptoFail("digest provider", status);
        return 0;
    }
    status = BCryptCreateHash(alg, &hash, NULL, 0, NULL, 0, 0);
    if (status == STATUS_SUCCESS) {
        status = BCryptHashData(hash, (PUCHAR) first, (ULONG) firstLength, 0);
    }
    if (status == STATUS_SUCCESS) {
        status = BCryptHashData(hash, (PUCHAR) second, (ULONG) secondLength, 0);
    }
    if (status == STATUS_SUCCESS) {
        status = BCryptFinishHash(hash, digest, (ULONG) digestLength, 0);
    }
    if (hash != NULL) {
        BCryptDestroyHash(hash);
    }
    BCryptCloseAlgorithmProvider(alg, 0);
    if (status != STATUS_SUCCESS) {
        cn1CryptoFail("digest", status);
        return 0;
    }
    return 1;
}

static int cn1Mgf1(LPCWSTR digestAlgorithm, const unsigned char* seed, int seedLength,
                   unsigned char* mask, int maskLength) {
    int digestLength = cn1DigestLength(digestAlgorithm);
    unsigned char counter[4];
    unsigned char digest[64];
    int produced = 0;
    unsigned int count = 0;
    while (produced < maskLength) {
        int chunk = maskLength - produced;
        counter[0] = (unsigned char) ((count >> 24) & 0xff);
        counter[1] = (unsigned char) ((count >> 16) & 0xff);
        counter[2] = (unsigned char) ((count >> 8) & 0xff);
        counter[3] = (unsigned char) (count & 0xff);
        if (!cn1DigestPair(digestAlgorithm, seed, seedLength, counter, 4, digest, digestLength)) {
            return 0;
        }
        if (chunk > digestLength) {
            chunk = digestLength;
        }
        memcpy(mask + produced, digest, (size_t) chunk);
        produced += chunk;
        count++;
    }
    return 1;
}

/* EME-OAEP encoding of `message` into a `blockLength`-byte block. */
static int cn1OaepEncode(LPCWSTR labelDigest, LPCWSTR maskDigest, const unsigned char* message,
                         int messageLength, unsigned char* block, int blockLength) {
    int hashLength = cn1DigestLength(labelDigest);
    int dbLength = blockLength - hashLength - 1;
    unsigned char seed[64];
    unsigned char mask[1024];
    int i;
    if (dbLength <= 0 || dbLength > (int) sizeof(mask)) {
        cn1CryptoFail("RSA-OAEP block does not fit the key", 0);
        return 0;
    }
    if (messageLength > dbLength - hashLength - 1) {
        cn1CryptoFail("RSA-OAEP message is too long for the key", 0);
        return 0;
    }
    memset(block, 0, (size_t) blockLength);
    /* DB = lHash || PS || 0x01 || M, with an empty label. */
    if (!cn1Digest(labelDigest, (const unsigned char*) "", 0, block + 1 + hashLength, hashLength)) {
        return 0;
    }
    block[blockLength - messageLength - 1] = 0x01;
    if (messageLength > 0) {
        memcpy(block + blockLength - messageLength, message, (size_t) messageLength);
    }
    if (BCryptGenRandom(NULL, seed, (ULONG) hashLength, BCRYPT_USE_SYSTEM_PREFERRED_RNG)
            != STATUS_SUCCESS) {
        cn1CryptoFail("RSA-OAEP seed", 0);
        return 0;
    }
    if (!cn1Mgf1(maskDigest, seed, hashLength, mask, dbLength)) {
        return 0;
    }
    for (i = 0; i < dbLength; i++) {
        block[1 + hashLength + i] ^= mask[i];
    }
    if (!cn1Mgf1(maskDigest, block + 1 + hashLength, dbLength, mask, hashLength)) {
        return 0;
    }
    for (i = 0; i < hashLength; i++) {
        block[1 + i] = (unsigned char) (seed[i] ^ mask[i]);
    }
    return 1;
}

/* All ones when a == b, zero otherwise, without branching on the values. */
static unsigned int cn1CtEqMask(unsigned int a, unsigned int b) {
    unsigned int diff = a ^ b;
    /* 1 when diff is nonzero, 0 when it is zero; minus one turns that into a
     * full-width mask without a comparison the compiler can branch on. */
    unsigned int nonZero = (diff | (0u - diff)) >> 31;
    return nonZero - 1u;
}

/* Reverses cn1OaepEncode, writing the recovered message and its length.
 *
 * Every check on the decrypted block feeds one accumulator and the function
 * reports a single generic failure, rather than returning early with a
 * distinct message per cause. An application that decrypts attacker-chosen
 * ciphertext and surfaces the exception (WindowsImplementation.cryptoResult
 * puts this text in it) would otherwise hand back which of the leading byte,
 * the label hash or the delimiter was wrong -- and telling those apart is
 * enough to mount the adaptive attacks OAEP exists to prevent. Only the
 * block geometry, which follows the key and not the ciphertext, is allowed to
 * bail early. */
static int cn1OaepDecode(LPCWSTR labelDigest, LPCWSTR maskDigest, unsigned char* block,
                         int blockLength, unsigned char* message, int* messageLength) {
    int hashLength = cn1DigestLength(labelDigest);
    int dbLength = blockLength - hashLength - 1;
    unsigned char mask[1024];
    unsigned char labelHash[64];
    unsigned char seed[64];
    int i;
    unsigned int bad = 0;
    unsigned int seenDelimiter = 0;
    unsigned int messageStart = 0;
    if (dbLength <= 0 || dbLength > (int) sizeof(mask)) {
        cn1CryptoFail("RSA-OAEP block does not fit the key", 0);
        return 0;
    }
    /* The leading byte must be zero; fold it in rather than returning here. */
    bad |= (unsigned int) block[0];
    if (!cn1Mgf1(maskDigest, block + 1 + hashLength, dbLength, mask, hashLength)) {
        return 0;
    }
    for (i = 0; i < hashLength; i++) {
        seed[i] = (unsigned char) (block[1 + i] ^ mask[i]);
    }
    if (!cn1Mgf1(maskDigest, seed, hashLength, mask, dbLength)) {
        return 0;
    }
    for (i = 0; i < dbLength; i++) {
        block[1 + hashLength + i] ^= mask[i];
    }
    if (!cn1Digest(labelDigest, (const unsigned char*) "", 0, labelHash, hashLength)) {
        return 0;
    }
    for (i = 0; i < hashLength; i++) {
        bad |= (unsigned int) (labelHash[i] ^ block[1 + hashLength + i]);
    }
    /* Walk the whole padding: zeros until one 0x01, then the message. The loop
     * never stops early, so its timing follows the key size alone. */
    for (i = 1 + hashLength + hashLength; i < blockLength; i++) {
        unsigned int value = block[i];
        unsigned int isDelimiter = cn1CtEqMask(value, 0x01);
        unsigned int isZero = cn1CtEqMask(value, 0x00);
        unsigned int firstDelimiter = isDelimiter & ~seenDelimiter;
        messageStart |= ((unsigned int) (i + 1)) & firstDelimiter;
        /* Ahead of the delimiter nothing but zeros is allowed. */
        bad |= ~seenDelimiter & ~isDelimiter & ~isZero;
        seenDelimiter |= isDelimiter;
    }
    bad |= ~seenDelimiter; /* no delimiter anywhere in the block */
    if (bad != 0) {
        cn1CryptoFail("RSA-OAEP decryption failed", 0);
        return 0;
    }
    *messageLength = blockLength - (int) messageStart;
    if (*messageLength > 0) {
        memcpy(message, block + messageStart, (size_t) *messageLength);
    }
    return 1;
}

/* One DER INTEGER holding an unsigned big-endian value. */
static int cn1DerInteger(const unsigned char* value, int length, unsigned char* out) {
    int start = 0;
    int written = 0;
    int pad;
    while (start < length - 1 && value[start] == 0) {
        start++;
    }
    pad = (value[start] & 0x80) != 0 ? 1 : 0;
    out[written++] = 0x02;
    out[written++] = (unsigned char) (length - start + pad);
    if (pad) {
        out[written++] = 0x00;
    }
    memcpy(out + written, value + start, (size_t) (length - start));
    return written + length - start;
}

/* P1363 r||s (as CNG produces) to the ASN.1 DER sequence the API expects.
 *
 * P-521 coordinates are 66 bytes each, so the sequence body runs to about 138
 * bytes and DER requires the long form (0x81 followed by the length) for
 * anything over 127. A single length byte there sets the high bit, which
 * Jwt.derToJoseEcdsa and every conforming parser read as a long-form marker,
 * and the ES512 signature is rejected. */
static int cn1EcdsaToDer(const unsigned char* raw, int rawLength, unsigned char* der) {
    int half = rawLength / 2;
    unsigned char body[160];
    int bodyLength = 0;
    int written = 0;
    if (rawLength <= 0 || (rawLength & 1) != 0 || half > 66) {
        return 0;
    }
    bodyLength = cn1DerInteger(raw, half, body);
    bodyLength += cn1DerInteger(raw + half, half, body + bodyLength);
    der[written++] = 0x30;
    if (bodyLength > 127) {
        der[written++] = 0x81;
    }
    der[written++] = (unsigned char) bodyLength;
    memcpy(der + written, body, (size_t) bodyLength);
    return written + bodyLength;
}

/* Coordinate width of an EC key in bytes: 66 for P-521, whose 521 bits do not
 * fill a whole byte count that any digest length happens to match. */
static int cn1EcCoordinateBytes(BCRYPT_KEY_HANDLE key) {
    DWORD bits = 0;
    ULONG copied = 0;
    if (BCryptGetProperty(key, BCRYPT_KEY_STRENGTH, (PUCHAR) &bits, sizeof(bits), &copied, 0)
            != STATUS_SUCCESS || bits == 0) {
        return 0;
    }
    return (int) ((bits + 7) / 8);
}

/* Inverse of cn1EcdsaToDer, padding each half back to `half` bytes. */
static int cn1EcdsaFromDer(const unsigned char* der, int derLength, unsigned char* raw, int half) {
    int index = 1;
    int part;
    if (derLength < 8 || der[0] != 0x30) {
        return 0;
    }
    /* Accept the long form the P-521 body needs, and only that one extra
     * length byte -- a sequence of two integers never runs past 255 bytes. */
    if (der[index] == 0x81) {
        index++;
        if (index >= derLength) {
            return 0;
        }
    } else if ((der[index] & 0x80) != 0) {
        return 0;
    }
    index++;
    memset(raw, 0, (size_t) (half * 2));
    for (part = 0; part < 2; part++) {
        int length, start, copy;
        if (index + 2 > derLength || der[index] != 0x02) {
            return 0;
        }
        length = der[index + 1];
        index += 2;
        if (length <= 0 || index + length > derLength) {
            return 0;
        }
        start = 0;
        while (start < length - 1 && der[index + start] == 0) {
            start++;
        }
        copy = length - start;
        if (copy > half) {
            return 0;
        }
        memcpy(raw + part * half + (half - copy), der + index + start, (size_t) copy);
        index += length;
    }
    return 1;
}

JAVA_OBJECT com_codename1_impl_windows_WindowsNative_rsaCrypt___java_lang_String_boolean_byte_1ARRAY_byte_1ARRAY_R_byte_1ARRAY(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT transformation, JAVA_BOOLEAN encrypt,
        JAVA_OBJECT keyArray, JAVA_OBJECT dataArray) {
    const char* mode = transformation == JAVA_NULL ? "" : stringToUTF8(threadStateData, transformation);
    int keyLength = 0, dataLength = 0;
    unsigned char* keyDer = cn1Bytes(keyArray, &keyLength);
    unsigned char* data = cn1Bytes(dataArray, &dataLength);
    BCRYPT_KEY_HANDLE publicKey = encrypt ? cn1PublicKey(keyDer, keyLength) : NULL;
    NCRYPT_KEY_HANDLE privateKey = encrypt ? 0 : cn1PrivateKey(keyDer, keyLength, 0);
    int oaepMode = strstr(mode, "OAEP") != 0;
    LPCWSTR labelDigest = strstr(mode, "SHA-1") != 0 ? BCRYPT_SHA1_ALGORITHM : BCRYPT_SHA256_ALGORITHM;
    unsigned char* out = 0;
    unsigned char* block = 0;
    ULONG outLength = 0, produced = 0;
    DWORD modulusBytes = 0, propertyBytes = 0;
    NTSTATUS status;
    JAVA_OBJECT result = JAVA_NULL;

    if (encrypt ? (publicKey == NULL) : (privateKey == 0)) {
        return JAVA_NULL;
    }

    if (oaepMode) {
        /* CNG's padding info names one digest for both the label hash and the
         * mask, so it cannot express the SHA-256 label with the SHA-1 mask that
         * the JCE providers -- and therefore the JavaSE, Android and Linux
         * ports -- use for this transformation. Pad here and run the key
         * operation raw so ciphertext stays readable across ports. */
        ULONG bits = 0;
        if (encrypt) {
            status = BCryptGetProperty(publicKey, BCRYPT_KEY_STRENGTH, (PUCHAR) &bits,
                                       sizeof(bits), &propertyBytes, 0);
            if (status != STATUS_SUCCESS) {
                cn1CryptoFail("RSA key size", status);
                goto done;
            }
            modulusBytes = bits / 8;
        } else {
            if (NCryptGetProperty(privateKey, NCRYPT_LENGTH_PROPERTY, (PBYTE) &bits,
                                  sizeof(bits), &propertyBytes, 0) != ERROR_SUCCESS) {
                cn1CryptoFail("RSA key size", 0);
                goto done;
            }
            modulusBytes = bits / 8;
        }
        block = (unsigned char*) malloc((size_t) modulusBytes + 1);
        if (block == 0) {
            cn1CryptoFail("out of memory", 0);
            goto done;
        }
        if (encrypt) {
            if (!cn1OaepEncode(labelDigest, BCRYPT_SHA1_ALGORITHM, data, dataLength, block,
                               (int) modulusBytes)) {
                goto done;
            }
            out = (unsigned char*) malloc((size_t) modulusBytes + 1);
            if (out == 0) {
                cn1CryptoFail("out of memory", 0);
                goto done;
            }
            status = BCryptEncrypt(publicKey, block, modulusBytes, NULL, NULL, 0, out,
                                   modulusBytes, &produced, BCRYPT_PAD_NONE);
            if (status != STATUS_SUCCESS) {
                cn1CryptoFail("RSA encrypt", status);
                goto done;
            }
        } else {
            DWORD recovered = 0;
            int messageLength = 0;
            if (NCryptDecrypt(privateKey, data, (DWORD) dataLength, NULL, block, modulusBytes,
                              &recovered, NCRYPT_NO_PADDING_FLAG) != ERROR_SUCCESS) {
                cn1CryptoFail("RSA decrypt", 0);
                goto done;
            }
            out = (unsigned char*) malloc((size_t) modulusBytes + 1);
            if (out == 0) {
                cn1CryptoFail("out of memory", 0);
                goto done;
            }
            if (!cn1OaepDecode(labelDigest, BCRYPT_SHA1_ALGORITHM, block, (int) modulusBytes,
                               out, &messageLength)) {
                goto done;
            }
            produced = (ULONG) messageLength;
        }
    } else {
        ULONG flags = BCRYPT_PAD_PKCS1;
        status = encrypt
                ? BCryptEncrypt(publicKey, data, (ULONG) dataLength, NULL, NULL, 0, NULL, 0,
                                &outLength, flags)
                : (NTSTATUS) NCryptDecrypt(privateKey, data, (DWORD) dataLength, NULL, NULL, 0,
                                           (DWORD*) &outLength, flags);
        if (status != STATUS_SUCCESS) {
            cn1CryptoFail("RSA size", status);
            goto done;
        }
        out = (unsigned char*) malloc((size_t) outLength + 1);
        if (out == 0) {
            cn1CryptoFail("out of memory", 0);
            goto done;
        }
        status = encrypt
                ? BCryptEncrypt(publicKey, data, (ULONG) dataLength, NULL, NULL, 0, out,
                                outLength, &produced, flags)
                : (NTSTATUS) NCryptDecrypt(privateKey, data, (DWORD) dataLength, NULL, out,
                                           outLength, (DWORD*) &produced, flags);
        if (status != STATUS_SUCCESS) {
            cn1CryptoFail(encrypt ? "RSA encrypt" : "RSA decrypt", status);
            goto done;
        }
    }
    result = cn1WinNewByteArray(threadStateData, out, (int) produced);

done:
    free(out);
    free(block);
    if (publicKey != NULL) {
        BCryptDestroyKey(publicKey);
    }
    if (privateKey != 0) {
        NCryptFreeObject(privateKey);
    }
    return result;
}

JAVA_OBJECT com_codename1_impl_windows_WindowsNative_signData___java_lang_String_byte_1ARRAY_byte_1ARRAY_R_byte_1ARRAY(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT algorithm, JAVA_OBJECT keyArray, JAVA_OBJECT dataArray) {
    const char* name = algorithm == JAVA_NULL ? "" : stringToUTF8(threadStateData, algorithm);
    int keyLength = 0, dataLength = 0, isEc = 0;
    unsigned char* keyDer = cn1Bytes(keyArray, &keyLength);
    unsigned char* data = cn1Bytes(dataArray, &dataLength);
    NCRYPT_KEY_HANDLE key = cn1PrivateKey(keyDer, keyLength, &isEc);
    LPCWSTR digestAlgorithm = cn1DigestAlgorithm(name);
    unsigned char digest[64];
    int digestLength = cn1DigestLength(digestAlgorithm);
    BCRYPT_PKCS1_PADDING_INFO padding;
    /* ECDSA carries no padding parameters; RSA signs with PKCS#1. */
    void* paddingInfo;
    DWORD flags;
    unsigned char* out = 0;
    DWORD outLength = 0, produced = 0;
    SECURITY_STATUS status;
    JAVA_OBJECT result = JAVA_NULL;

    if (key == 0) {
        return JAVA_NULL;
    }
    if (!cn1Digest(digestAlgorithm, data, dataLength, digest, digestLength)) {
        goto done;
    }
    padding.pszAlgId = digestAlgorithm;
    paddingInfo = isEc ? NULL : (void*) &padding;
    flags = isEc ? 0 : BCRYPT_PAD_PKCS1;
    status = NCryptSignHash(key, paddingInfo, digest, (DWORD) digestLength, NULL, 0,
                            &outLength, flags);
    if (status != ERROR_SUCCESS) {
        cn1CryptoFail("sign size", (NTSTATUS) status);
        goto done;
    }
    out = (unsigned char*) malloc((size_t) outLength + 1);
    if (out == 0) {
        cn1CryptoFail("out of memory", 0);
        goto done;
    }
    status = NCryptSignHash(key, paddingInfo, digest, (DWORD) digestLength, out, outLength,
                            &produced, flags);
    if (status != ERROR_SUCCESS) {
        cn1CryptoFail("sign", (NTSTATUS) status);
        goto done;
    }
    if (isEc) {
        /* NCrypt answers the fixed-width r||s of P1363; the portable Signature
         * contract, and Jwt.derToJoseEcdsa with it, expects ASN.1 DER. */
        unsigned char der[160];
        int derLength = cn1EcdsaToDer(out, (int) produced, der);
        if (derLength <= 0) {
            cn1CryptoFail("ECDSA signature encoding", 0);
            goto done;
        }
        result = cn1WinNewByteArray(threadStateData, der, derLength);
    } else {
        result = cn1WinNewByteArray(threadStateData, out, (int) produced);
    }

done:
    free(out);
    NCryptFreeObject(key);
    return result;
}

JAVA_BOOLEAN com_codename1_impl_windows_WindowsNative_verifyData___java_lang_String_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_R_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT algorithm, JAVA_OBJECT keyArray,
        JAVA_OBJECT dataArray, JAVA_OBJECT signatureArray) {
    const char* name = algorithm == JAVA_NULL ? "" : stringToUTF8(threadStateData, algorithm);
    int keyLength = 0, dataLength = 0, signatureLength = 0;
    unsigned char* keyDer = cn1Bytes(keyArray, &keyLength);
    unsigned char* data = cn1Bytes(dataArray, &dataLength);
    unsigned char* signature = cn1Bytes(signatureArray, &signatureLength);
    /* CryptImportPublicKeyInfoEx2 handles both key kinds; only the padding
     * differs, so read the algorithm out of the SubjectPublicKeyInfo. */
    int isEc = cn1PublicKeyIsEc(keyDer, keyLength);
    BCRYPT_KEY_HANDLE key = cn1PublicKey(keyDer, keyLength);
    LPCWSTR digestAlgorithm = cn1DigestAlgorithm(name);
    unsigned char digest[64];
    int digestLength = cn1DigestLength(digestAlgorithm);
    BCRYPT_PKCS1_PADDING_INFO padding;
    JAVA_BOOLEAN result = JAVA_FALSE;

    if (key == NULL) {
        return JAVA_FALSE;
    }
    if (cn1Digest(digestAlgorithm, data, dataLength, digest, digestLength)) {
        unsigned char raw[132];
        const unsigned char* toVerify = signature;
        ULONG toVerifyLength = (ULONG) signatureLength;
        int usable = 1;
        padding.pszAlgId = digestAlgorithm;
        if (isEc) {
            /* Signatures arrive as DER; CNG verifies the P1363 pair. The half
             * width is the curve's, read off the key -- deriving it from the
             * named digest gets P-521 wrong, whose coordinates are 66 bytes
             * while SHA-512 is 64, so a valid ES512 signature would be handed
             * to CNG as a 128-byte pair instead of the required 132. */
            int half = cn1EcCoordinateBytes(key);
            if (half <= 0 || half * 2 > (int) sizeof(raw)) {
                usable = 0;
            } else {
                usable = cn1EcdsaFromDer(signature, signatureLength, raw, half);
                toVerify = raw;
                toVerifyLength = (ULONG) (half * 2);
            }
        }
        /* A rejected signature is a normal answer here, not a fault. */
        if (usable && BCryptVerifySignature(key, isEc ? NULL : &padding, digest,
                                            (ULONG) digestLength, (PUCHAR) toVerify,
                                            toVerifyLength,
                                            isEc ? 0 : BCRYPT_PAD_PKCS1) == STATUS_SUCCESS) {
            result = JAVA_TRUE;
        }
    }
    BCryptDestroyKey(key);
    return result;
}

/* ------------------------------------------------------------ key pairs */

/* Returns the pair as one array: a four-byte big-endian public-key length,
 * the X.509 public key, then the PKCS#8 private key. A pair has to come from
 * a single call -- two calls would produce two unrelated keys. */
JAVA_OBJECT com_codename1_impl_windows_WindowsNative_generateRsaKeyPair___int_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_INT bits) {
    BCRYPT_ALG_HANDLE alg = NULL;
    BCRYPT_KEY_HANDLE key = NULL;
    CERT_PUBLIC_KEY_INFO* publicInfo = 0;
    DWORD publicInfoLength = 0;
    unsigned char* publicDer = 0;
    DWORD publicLength = 0;
    unsigned char* privateBlob = 0;
    ULONG privateBlobLength = 0;
    unsigned char* pkcs1 = 0;
    DWORD pkcs1Length = 0;
    unsigned char* privateDer = 0;
    DWORD privateLength = 0;
    unsigned char* blob = 0;
    CRYPT_PRIVATE_KEY_INFO keyInfo;
    unsigned char derNull[2];
    NTSTATUS status;
    JAVA_OBJECT result = JAVA_NULL;

    status = BCryptOpenAlgorithmProvider(&alg, BCRYPT_RSA_ALGORITHM, NULL, 0);
    if (status != STATUS_SUCCESS) {
        cn1CryptoFail("RSA provider", status);
        return JAVA_NULL;
    }
    status = BCryptGenerateKeyPair(alg, &key, (ULONG) bits, 0);
    if (status == STATUS_SUCCESS) {
        status = BCryptFinalizeKeyPair(key, 0);
    }
    if (status != STATUS_SUCCESS) {
        cn1CryptoFail("RSA keygen", status);
        goto done;
    }

    /* Public half: BCrypt handle -> CERT_PUBLIC_KEY_INFO -> X.509 SPKI DER. */
    if (!CryptExportPublicKeyInfoFromBCryptKeyHandle(key, X509_ASN_ENCODING, NULL, 0, NULL,
                                                     NULL, &publicInfoLength)) {
        cn1CryptoFailLast("public key export size");
        goto done;
    }
    publicInfo = (CERT_PUBLIC_KEY_INFO*) malloc(publicInfoLength);
    if (publicInfo == 0) {
        cn1CryptoFail("out of memory", 0);
        goto done;
    }
    if (!CryptExportPublicKeyInfoFromBCryptKeyHandle(key, X509_ASN_ENCODING, NULL, 0, NULL,
                                                     publicInfo, &publicInfoLength) ||
        !CryptEncodeObjectEx(X509_ASN_ENCODING, X509_PUBLIC_KEY_INFO, publicInfo,
                             CRYPT_ENCODE_ALLOC_FLAG, NULL, &publicDer, &publicLength)) {
        cn1CryptoFailLast("public key encode");
        goto done;
    }

    /* Private half: BCrypt blob -> PKCS#1 DER -> PKCS#8 PrivateKeyInfo DER. */
    status = BCryptExportKey(key, NULL, BCRYPT_RSAFULLPRIVATE_BLOB, NULL, 0, &privateBlobLength, 0);
    if (status != STATUS_SUCCESS) {
        cn1CryptoFail("private key export size", status);
        goto done;
    }
    privateBlob = (unsigned char*) malloc(privateBlobLength);
    if (privateBlob == 0) {
        cn1CryptoFail("out of memory", 0);
        goto done;
    }
    status = BCryptExportKey(key, NULL, BCRYPT_RSAFULLPRIVATE_BLOB, privateBlob, privateBlobLength,
                             &privateBlobLength, 0);
    if (status != STATUS_SUCCESS) {
        cn1CryptoFail("private key export", status);
        goto done;
    }
    if (!CryptEncodeObjectEx(X509_ASN_ENCODING, CNG_RSA_PRIVATE_KEY_BLOB, privateBlob,
                             CRYPT_ENCODE_ALLOC_FLAG, NULL, &pkcs1, &pkcs1Length)) {
        cn1CryptoFailLast("private key encode");
        goto done;
    }
    memset(&keyInfo, 0, sizeof(keyInfo));
    keyInfo.Version = 0;
    keyInfo.Algorithm.pszObjId = (LPSTR) szOID_RSA_RSA;
    /* rsaEncryption takes an explicit ASN.1 NULL parameter. */
    derNull[0] = 0x05;
    derNull[1] = 0x00;
    keyInfo.Algorithm.Parameters.cbData = sizeof(derNull);
    keyInfo.Algorithm.Parameters.pbData = derNull;
    keyInfo.PrivateKey.cbData = pkcs1Length;
    keyInfo.PrivateKey.pbData = pkcs1;
    if (!CryptEncodeObjectEx(X509_ASN_ENCODING, PKCS_PRIVATE_KEY_INFO, &keyInfo,
                             CRYPT_ENCODE_ALLOC_FLAG, NULL, &privateDer, &privateLength)) {
        cn1CryptoFailLast("PKCS#8 encode");
        goto done;
    }

    blob = (unsigned char*) malloc((size_t) publicLength + (size_t) privateLength + 4);
    if (blob == 0) {
        cn1CryptoFail("out of memory", 0);
        goto done;
    }
    blob[0] = (unsigned char) ((publicLength >> 24) & 0xff);
    blob[1] = (unsigned char) ((publicLength >> 16) & 0xff);
    blob[2] = (unsigned char) ((publicLength >> 8) & 0xff);
    blob[3] = (unsigned char) (publicLength & 0xff);
    memcpy(blob + 4, publicDer, publicLength);
    memcpy(blob + 4 + publicLength, privateDer, privateLength);
    result = cn1WinNewByteArray(threadStateData, blob, (int) (publicLength + privateLength + 4));

done:
    free(blob);
    free(publicInfo);
    free(privateBlob);
    if (publicDer != 0) {
        LocalFree(publicDer);
    }
    if (pkcs1 != 0) {
        LocalFree(pkcs1);
    }
    if (privateDer != 0) {
        LocalFree(privateDer);
    }
    if (key != NULL) {
        BCryptDestroyKey(key);
    }
    if (alg != NULL) {
        BCryptCloseAlgorithmProvider(alg, 0);
    }
    return result;
}
