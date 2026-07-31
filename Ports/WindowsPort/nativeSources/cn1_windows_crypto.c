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

static char cn1WinCryptoError[512];

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

JAVA_OBJECT com_codename1_impl_windows_WindowsNative_rsaCrypt___java_lang_String_boolean_byte_1ARRAY_byte_1ARRAY_R_byte_1ARRAY(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT transformation, JAVA_BOOLEAN encrypt,
        JAVA_OBJECT keyArray, JAVA_OBJECT dataArray) {
    const char* mode = transformation == JAVA_NULL ? "" : stringToUTF8(threadStateData, transformation);
    int keyLength = 0, dataLength = 0;
    unsigned char* keyDer = cn1Bytes(keyArray, &keyLength);
    unsigned char* data = cn1Bytes(dataArray, &dataLength);
    BCRYPT_KEY_HANDLE publicKey = encrypt ? cn1PublicKey(keyDer, keyLength) : NULL;
    NCRYPT_KEY_HANDLE privateKey = encrypt ? 0 : cn1PrivateKey(keyDer, keyLength, 0);
    BCRYPT_OAEP_PADDING_INFO oaep;
    int oaepMode = strstr(mode, "OAEP") != 0;
    void* padding = 0;
    ULONG flags = oaepMode ? BCRYPT_PAD_OAEP : BCRYPT_PAD_PKCS1;
    unsigned char* out = 0;
    ULONG outLength = 0, produced = 0;
    NTSTATUS status;
    JAVA_OBJECT result = JAVA_NULL;

    if (encrypt ? (publicKey == NULL) : (privateKey == 0)) {
        return JAVA_NULL;
    }
    if (oaepMode) {
        memset(&oaep, 0, sizeof(oaep));
        /* CNG derives the mask function from this same digest, and the JCE
         * providers behind the JavaSE and Android ports mask with SHA-1 for
         * this transformation name. Naming SHA-256 here would make ciphertext
         * sealed on those ports undecryptable, so keep the SHA-1 mask. */
        oaep.pszAlgId = BCRYPT_SHA1_ALGORITHM;
        oaep.pbLabel = NULL;
        oaep.cbLabel = 0;
        padding = &oaep;
    }
    status = encrypt
            ? BCryptEncrypt(publicKey, data, (ULONG) dataLength, padding, NULL, 0, NULL, 0, &outLength, flags)
            : (NTSTATUS) NCryptDecrypt(privateKey, (PBYTE) data, (DWORD) dataLength, padding,
                                       NULL, 0, (DWORD*) &outLength, flags);
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
            ? BCryptEncrypt(publicKey, data, (ULONG) dataLength, padding, NULL, 0, out, outLength, &produced, flags)
            : (NTSTATUS) NCryptDecrypt(privateKey, (PBYTE) data, (DWORD) dataLength, padding,
                                       out, outLength, (DWORD*) &produced, flags);
    if (status != STATUS_SUCCESS) {
        cn1CryptoFail(encrypt ? "RSA encrypt" : "RSA decrypt", status);
        goto done;
    }
    result = cn1WinNewByteArray(threadStateData, out, (int) produced);

done:
    free(out);
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
    result = cn1WinNewByteArray(threadStateData, out, (int) produced);

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
        padding.pszAlgId = digestAlgorithm;
        /* A rejected signature is a normal answer here, not a fault. */
        if (BCryptVerifySignature(key, isEc ? NULL : &padding, digest, (ULONG) digestLength,
                                  signature, (ULONG) signatureLength,
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
