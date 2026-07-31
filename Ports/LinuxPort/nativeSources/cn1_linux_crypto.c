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
 * The crypto half of com.codename1.impl.CodenameOneImplementation, backed by
 * OpenSSL's EVP layer (libcrypto, which the port already pulls in through
 * libcurl).
 *
 * Key material crosses this boundary in the same DER encodings the portable
 * API documents -- X.509 SubjectPublicKeyInfo for public keys and PKCS#8
 * PrivateKeyInfo for private keys -- so nothing here has to parse ASN.1 by
 * hand: d2i_PUBKEY and d2i_PKCS8_PRIV_KEY_INFO do it.
 *
 * Every entry point answers null (or false) on failure and records a reason
 * retrievable through lastCryptoError, which the Java side turns into the
 * CryptoException message. A silent empty result would look like a successful
 * encryption of nothing.
 */

#include "cn1_linux.h"
#include <string.h>
#include <stdio.h>
#include <stdlib.h>

#include <openssl/evp.h>
#include <openssl/rand.h>
#include <openssl/x509.h>
#include <openssl/rsa.h>
#include <openssl/err.h>
#include <openssl/pkcs12.h>

#define CN1_GCM_TAG_BYTES 16

static char cn1CryptoError[512];

static void cn1CryptoFail(const char* what) {
    unsigned long code = ERR_get_error();
    char detail[256];
    detail[0] = 0;
    if (code != 0) {
        ERR_error_string_n(code, detail, sizeof(detail));
    }
    snprintf(cn1CryptoError, sizeof(cn1CryptoError), "%s%s%s", what,
             detail[0] ? ": " : "", detail);
    ERR_clear_error();
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_lastCryptoError___R_java_lang_String(CODENAME_ONE_THREAD_STATE) {
    return newStringFromCString(threadStateData,
                                cn1CryptoError[0] ? cn1CryptoError : "unknown crypto error");
}

static const unsigned char* cn1Bytes(JAVA_OBJECT array, int* length) {
    if (array == JAVA_NULL) {
        *length = 0;
        return 0;
    }
    *length = (int) (*(JAVA_ARRAY) array).length;
    return (const unsigned char*) (*(JAVA_ARRAY) array).data;
}

/* ------------------------------------------------------------ random */

JAVA_VOID com_codename1_impl_linux_LinuxNative_secureRandomBytes___byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT out) {
    int length = 0;
    unsigned char* data = (unsigned char*) cn1Bytes(out, &length);
    if (data == 0 || length <= 0) {
        return;
    }
    if (RAND_bytes(data, length) != 1) {
        cn1CryptoFail("secure random");
        memset(data, 0, (size_t) length);
    }
}

/* ------------------------------------------------------------ AES */

static const EVP_CIPHER* cn1AesCipher(const char* transformation, int keyLength) {
    int gcm = strstr(transformation, "/GCM/") != 0;
    int ecb = strstr(transformation, "/ECB/") != 0;
    switch (keyLength) {
        case 16:
            return gcm ? EVP_aes_128_gcm() : (ecb ? EVP_aes_128_ecb() : EVP_aes_128_cbc());
        case 24:
            return gcm ? EVP_aes_192_gcm() : (ecb ? EVP_aes_192_ecb() : EVP_aes_192_cbc());
        case 32:
            return gcm ? EVP_aes_256_gcm() : (ecb ? EVP_aes_256_ecb() : EVP_aes_256_cbc());
        default:
            return 0;
    }
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_aesCrypt___java_lang_String_boolean_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_R_byte_1ARRAY(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT transformation, JAVA_BOOLEAN encrypt,
        JAVA_OBJECT keyArray, JAVA_OBJECT ivArray, JAVA_OBJECT aadArray, JAVA_OBJECT dataArray) {
    const char* mode = transformation == JAVA_NULL ? "" : stringToUTF8(threadStateData, transformation);
    int keyLength = 0, ivLength = 0, aadLength = 0, dataLength = 0;
    const unsigned char* key = cn1Bytes(keyArray, &keyLength);
    const unsigned char* iv = cn1Bytes(ivArray, &ivLength);
    const unsigned char* aad = cn1Bytes(aadArray, &aadLength);
    const unsigned char* data = cn1Bytes(dataArray, &dataLength);
    int gcm = strstr(mode, "/GCM/") != 0;
    int padded = strstr(mode, "NoPadding") == 0;
    const EVP_CIPHER* cipher = cn1AesCipher(mode, keyLength);
    EVP_CIPHER_CTX* ctx = 0;
    unsigned char* out = 0;
    unsigned char tag[CN1_GCM_TAG_BYTES];
    int bodyLength = dataLength;
    int outLength = 0, finalLength = 0, discard = 0;
    JAVA_OBJECT result = JAVA_NULL;

    if (cipher == 0) {
        cn1CryptoFail("unsupported AES key length");
        return JAVA_NULL;
    }
    if (gcm && !encrypt) {
        if (dataLength < CN1_GCM_TAG_BYTES) {
            cn1CryptoFail("AES-GCM input is shorter than its authentication tag");
            return JAVA_NULL;
        }
        bodyLength = dataLength - CN1_GCM_TAG_BYTES;
    }

    ctx = EVP_CIPHER_CTX_new();
    if (ctx == 0) {
        cn1CryptoFail("cipher context");
        return JAVA_NULL;
    }
    /* Room for a full trailing block of padding, plus the tag when sealing. */
    out = (unsigned char*) malloc((size_t) bodyLength + EVP_MAX_BLOCK_LENGTH + CN1_GCM_TAG_BYTES);
    if (out == 0) {
        EVP_CIPHER_CTX_free(ctx);
        cn1CryptoFail("out of memory");
        return JAVA_NULL;
    }

    if (EVP_CipherInit_ex(ctx, cipher, 0, 0, 0, encrypt ? 1 : 0) != 1) {
        goto failed;
    }
    if (gcm && ivLength > 0 &&
        EVP_CIPHER_CTX_ctrl(ctx, EVP_CTRL_AEAD_SET_IVLEN, ivLength, 0) != 1) {
        goto failed;
    }
    if (EVP_CipherInit_ex(ctx, 0, 0, key, ivLength > 0 ? iv : 0, encrypt ? 1 : 0) != 1) {
        goto failed;
    }
    if (EVP_CIPHER_CTX_set_padding(ctx, padded ? 1 : 0) != 1) {
        goto failed;
    }
    if (gcm && aadLength > 0 && EVP_CipherUpdate(ctx, 0, &discard, aad, aadLength) != 1) {
        goto failed;
    }
    if (bodyLength > 0 && EVP_CipherUpdate(ctx, out, &outLength, data, bodyLength) != 1) {
        goto failed;
    }
    if (gcm && !encrypt &&
        EVP_CIPHER_CTX_ctrl(ctx, EVP_CTRL_AEAD_SET_TAG, CN1_GCM_TAG_BYTES,
                            (void*) (data + bodyLength)) != 1) {
        goto failed;
    }
    if (EVP_CipherFinal_ex(ctx, out + outLength, &finalLength) != 1) {
        /* For GCM this is the tag check: a tampered message lands here. */
        cn1CryptoFail(gcm && !encrypt ? "AES-GCM authentication failed" : "AES finalize");
        free(out);
        EVP_CIPHER_CTX_free(ctx);
        return JAVA_NULL;
    }
    outLength += finalLength;
    if (gcm && encrypt) {
        if (EVP_CIPHER_CTX_ctrl(ctx, EVP_CTRL_AEAD_GET_TAG, CN1_GCM_TAG_BYTES, tag) != 1) {
            goto failed;
        }
        memcpy(out + outLength, tag, CN1_GCM_TAG_BYTES);
        outLength += CN1_GCM_TAG_BYTES;
    }
    result = cn1LinuxNewByteArray(threadStateData, out, outLength);
    free(out);
    EVP_CIPHER_CTX_free(ctx);
    return result;

failed:
    cn1CryptoFail("AES");
    free(out);
    EVP_CIPHER_CTX_free(ctx);
    return JAVA_NULL;
}

/* ------------------------------------------------------------ keys */

static EVP_PKEY* cn1PublicKey(const unsigned char* der, int length) {
    const unsigned char* cursor = der;
    EVP_PKEY* key = d2i_PUBKEY(0, &cursor, (long) length);
    if (key == 0) {
        cn1CryptoFail("public key is not X.509 SubjectPublicKeyInfo DER");
    }
    return key;
}

static EVP_PKEY* cn1PrivateKey(const unsigned char* der, int length) {
    const unsigned char* cursor = der;
    EVP_PKEY* key = 0;
    PKCS8_PRIV_KEY_INFO* info = d2i_PKCS8_PRIV_KEY_INFO(0, &cursor, (long) length);
    if (info != 0) {
        key = EVP_PKCS82PKEY(info);
        PKCS8_PRIV_KEY_INFO_free(info);
    }
    if (key == 0) {
        /* Tolerate a bare PKCS#1/SEC1 key as well; some callers keep those. */
        cursor = der;
        key = d2i_AutoPrivateKey(0, &cursor, (long) length);
    }
    if (key == 0) {
        cn1CryptoFail("private key is not PKCS#8 DER");
    }
    return key;
}

static int cn1ApplyRsaPadding(EVP_PKEY_CTX* ctx, const char* transformation) {
    if (strstr(transformation, "OAEP") != 0) {
        const EVP_MD* md = strstr(transformation, "SHA-1") != 0 ? EVP_sha1() : EVP_sha256();
        if (EVP_PKEY_CTX_set_rsa_padding(ctx, RSA_PKCS1_OAEP_PADDING) <= 0 ||
            EVP_PKEY_CTX_set_rsa_oaep_md(ctx, md) <= 0 ||
            EVP_PKEY_CTX_set_rsa_mgf1_md(ctx, md) <= 0) {
            return 0;
        }
        return 1;
    }
    return EVP_PKEY_CTX_set_rsa_padding(ctx, RSA_PKCS1_PADDING) > 0;
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_rsaCrypt___java_lang_String_boolean_byte_1ARRAY_byte_1ARRAY_R_byte_1ARRAY(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT transformation, JAVA_BOOLEAN encrypt,
        JAVA_OBJECT keyArray, JAVA_OBJECT dataArray) {
    const char* mode = transformation == JAVA_NULL ? "" : stringToUTF8(threadStateData, transformation);
    int keyLength = 0, dataLength = 0;
    const unsigned char* keyDer = cn1Bytes(keyArray, &keyLength);
    const unsigned char* data = cn1Bytes(dataArray, &dataLength);
    EVP_PKEY* key = encrypt ? cn1PublicKey(keyDer, keyLength) : cn1PrivateKey(keyDer, keyLength);
    EVP_PKEY_CTX* ctx = 0;
    unsigned char* out = 0;
    size_t outLength = 0;
    JAVA_OBJECT result = JAVA_NULL;

    if (key == 0) {
        return JAVA_NULL;
    }
    ctx = EVP_PKEY_CTX_new(key, 0);
    if (ctx == 0) {
        cn1CryptoFail("RSA context");
        EVP_PKEY_free(key);
        return JAVA_NULL;
    }
    if ((encrypt ? EVP_PKEY_encrypt_init(ctx) : EVP_PKEY_decrypt_init(ctx)) <= 0 ||
        !cn1ApplyRsaPadding(ctx, mode)) {
        cn1CryptoFail("RSA init");
        goto done;
    }
    if ((encrypt ? EVP_PKEY_encrypt(ctx, 0, &outLength, data, (size_t) dataLength)
                 : EVP_PKEY_decrypt(ctx, 0, &outLength, data, (size_t) dataLength)) <= 0) {
        cn1CryptoFail("RSA size");
        goto done;
    }
    out = (unsigned char*) malloc(outLength);
    if (out == 0) {
        cn1CryptoFail("out of memory");
        goto done;
    }
    if ((encrypt ? EVP_PKEY_encrypt(ctx, out, &outLength, data, (size_t) dataLength)
                 : EVP_PKEY_decrypt(ctx, out, &outLength, data, (size_t) dataLength)) <= 0) {
        cn1CryptoFail(encrypt ? "RSA encrypt" : "RSA decrypt");
        goto done;
    }
    result = cn1LinuxNewByteArray(threadStateData, out, (int) outLength);

done:
    free(out);
    EVP_PKEY_CTX_free(ctx);
    EVP_PKEY_free(key);
    return result;
}

/* ------------------------------------------------------------ signatures */

static const EVP_MD* cn1SignatureDigest(const char* algorithm) {
    if (strstr(algorithm, "SHA512") != 0 || strstr(algorithm, "SHA-512") != 0) {
        return EVP_sha512();
    }
    if (strstr(algorithm, "SHA384") != 0 || strstr(algorithm, "SHA-384") != 0) {
        return EVP_sha384();
    }
    if (strstr(algorithm, "SHA1") != 0 || strstr(algorithm, "SHA-1") != 0) {
        return EVP_sha1();
    }
    return EVP_sha256();
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_signData___java_lang_String_byte_1ARRAY_byte_1ARRAY_R_byte_1ARRAY(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT algorithm, JAVA_OBJECT keyArray, JAVA_OBJECT dataArray) {
    const char* name = algorithm == JAVA_NULL ? "" : stringToUTF8(threadStateData, algorithm);
    int keyLength = 0, dataLength = 0;
    const unsigned char* keyDer = cn1Bytes(keyArray, &keyLength);
    const unsigned char* data = cn1Bytes(dataArray, &dataLength);
    EVP_PKEY* key = cn1PrivateKey(keyDer, keyLength);
    EVP_MD_CTX* ctx = 0;
    unsigned char* out = 0;
    size_t outLength = 0;
    JAVA_OBJECT result = JAVA_NULL;

    if (key == 0) {
        return JAVA_NULL;
    }
    ctx = EVP_MD_CTX_new();
    if (ctx == 0) {
        cn1CryptoFail("digest context");
        EVP_PKEY_free(key);
        return JAVA_NULL;
    }
    if (EVP_DigestSignInit(ctx, 0, cn1SignatureDigest(name), 0, key) <= 0 ||
        EVP_DigestSign(ctx, 0, &outLength, data, (size_t) dataLength) <= 0) {
        cn1CryptoFail("sign init");
        goto done;
    }
    out = (unsigned char*) malloc(outLength);
    if (out == 0) {
        cn1CryptoFail("out of memory");
        goto done;
    }
    if (EVP_DigestSign(ctx, out, &outLength, data, (size_t) dataLength) <= 0) {
        cn1CryptoFail("sign");
        goto done;
    }
    result = cn1LinuxNewByteArray(threadStateData, out, (int) outLength);

done:
    free(out);
    EVP_MD_CTX_free(ctx);
    EVP_PKEY_free(key);
    return result;
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_verifyData___java_lang_String_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_R_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT algorithm, JAVA_OBJECT keyArray,
        JAVA_OBJECT dataArray, JAVA_OBJECT signatureArray) {
    const char* name = algorithm == JAVA_NULL ? "" : stringToUTF8(threadStateData, algorithm);
    int keyLength = 0, dataLength = 0, signatureLength = 0;
    const unsigned char* keyDer = cn1Bytes(keyArray, &keyLength);
    const unsigned char* data = cn1Bytes(dataArray, &dataLength);
    const unsigned char* signature = cn1Bytes(signatureArray, &signatureLength);
    EVP_PKEY* key = cn1PublicKey(keyDer, keyLength);
    EVP_MD_CTX* ctx = 0;
    JAVA_BOOLEAN result = JAVA_FALSE;

    if (key == 0) {
        return JAVA_FALSE;
    }
    ctx = EVP_MD_CTX_new();
    if (ctx == 0) {
        cn1CryptoFail("digest context");
        EVP_PKEY_free(key);
        return JAVA_FALSE;
    }
    if (EVP_DigestVerifyInit(ctx, 0, cn1SignatureDigest(name), 0, key) > 0 &&
        EVP_DigestVerify(ctx, signature, (size_t) signatureLength, data, (size_t) dataLength) == 1) {
        result = JAVA_TRUE;
    } else {
        /* A rejected signature is a normal answer, not a fault; clear the
         * queue so it cannot be reported against a later operation. */
        ERR_clear_error();
    }
    EVP_MD_CTX_free(ctx);
    EVP_PKEY_free(key);
    return result;
}

/* ------------------------------------------------------------ key pairs */

/* Returns the pair as one array: a four-byte big-endian public-key length,
 * the X.509 public key, then the PKCS#8 private key. A pair has to come from
 * a single call -- two calls would produce two unrelated keys. */
JAVA_OBJECT com_codename1_impl_linux_LinuxNative_generateRsaKeyPair___int_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_INT bits) {
    EVP_PKEY_CTX* ctx = EVP_PKEY_CTX_new_id(EVP_PKEY_RSA, 0);
    EVP_PKEY* key = 0;
    PKCS8_PRIV_KEY_INFO* info = 0;
    unsigned char* publicDer = 0;
    unsigned char* privateDer = 0;
    unsigned char* blob = 0;
    int publicLength = 0, privateLength = 0;
    JAVA_OBJECT result = JAVA_NULL;

    if (ctx == 0) {
        cn1CryptoFail("RSA keygen context");
        return JAVA_NULL;
    }
    if (EVP_PKEY_keygen_init(ctx) <= 0 ||
        EVP_PKEY_CTX_set_rsa_keygen_bits(ctx, bits) <= 0 ||
        EVP_PKEY_keygen(ctx, &key) <= 0) {
        cn1CryptoFail("RSA keygen");
        goto done;
    }
    publicLength = i2d_PUBKEY(key, &publicDer);
    info = EVP_PKEY2PKCS8(key);
    if (info != 0) {
        privateLength = i2d_PKCS8_PRIV_KEY_INFO(info, &privateDer);
    }
    if (publicLength <= 0 || privateLength <= 0) {
        cn1CryptoFail("RSA key encoding");
        goto done;
    }
    blob = (unsigned char*) malloc((size_t) publicLength + (size_t) privateLength + 4);
    if (blob == 0) {
        cn1CryptoFail("out of memory");
        goto done;
    }
    blob[0] = (unsigned char) ((publicLength >> 24) & 0xff);
    blob[1] = (unsigned char) ((publicLength >> 16) & 0xff);
    blob[2] = (unsigned char) ((publicLength >> 8) & 0xff);
    blob[3] = (unsigned char) (publicLength & 0xff);
    memcpy(blob + 4, publicDer, (size_t) publicLength);
    memcpy(blob + 4 + publicLength, privateDer, (size_t) privateLength);
    result = cn1LinuxNewByteArray(threadStateData, blob, publicLength + privateLength + 4);

done:
    free(blob);
    OPENSSL_free(publicDer);
    OPENSSL_free(privateDer);
    if (info != 0) {
        PKCS8_PRIV_KEY_INFO_free(info);
    }
    if (key != 0) {
        EVP_PKEY_free(key);
    }
    EVP_PKEY_CTX_free(ctx);
    return result;
}
