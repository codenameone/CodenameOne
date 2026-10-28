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
#include <openssl/ec.h>
#include <openssl/obj_mac.h>
#include <openssl/err.h>
#include <openssl/pkcs12.h>

#include <dlfcn.h>
#include <pthread.h>

/*
 * libcrypto is loaded on the first crypto call, not linked: it is 4 MB that an
 * application which never encrypts would otherwise map, and read from storage,
 * on every start (most visibly on the embedded target, whose whole stack is
 * smaller than that). The functions are reached through pointers named after
 * them, as cn1_linux_net.c reaches libcurl. Resolving by name against the
 * OpenSSL 3 ABI also means the binary does not carry the build host's symbol
 * versions.
 */
#if !defined(OPENSSL_VERSION_MAJOR) || OPENSSL_VERSION_MAJOR < 3
#error "The Linux port's crypto bridge needs the OpenSSL 3 headers"
#endif

/* The names dlsym finds: where OpenSSL 3 keeps an old name as a macro (OPENSSL_free,
 * EVP_PKEY_id), the function it expands to is listed, and the old name reaches
 * the pointer through that expansion. */
#define CN1_CRYPTO_FUNCTIONS(X) \
    X(ERR_clear_error) \
    X(ERR_error_string_n) \
    X(ERR_get_error) \
    X(EVP_CIPHER_CTX_ctrl) \
    X(EVP_CIPHER_CTX_free) \
    X(EVP_CIPHER_CTX_new) \
    X(EVP_CIPHER_CTX_set_padding) \
    X(EVP_CipherFinal_ex) \
    X(EVP_CipherInit_ex) \
    X(EVP_CipherUpdate) \
    X(EVP_DigestSign) \
    X(EVP_DigestSignInit) \
    X(EVP_DigestVerify) \
    X(EVP_DigestVerifyInit) \
    X(EVP_MD_CTX_free) \
    X(EVP_MD_CTX_new) \
    X(EVP_PKCS82PKEY) \
    X(EVP_PKEY2PKCS8) \
    X(EVP_PKEY_CTX_free) \
    X(EVP_PKEY_CTX_new) \
    X(EVP_PKEY_CTX_new_id) \
    X(EVP_PKEY_CTX_set_ec_paramgen_curve_nid) \
    X(EVP_PKEY_CTX_set_rsa_keygen_bits) \
    X(EVP_PKEY_CTX_set_rsa_mgf1_md) \
    X(EVP_PKEY_CTX_set_rsa_oaep_md) \
    X(EVP_PKEY_CTX_set_rsa_padding) \
    X(EVP_PKEY_get_base_id) \
    X(EVP_PKEY_decrypt) \
    X(EVP_PKEY_decrypt_init) \
    X(EVP_PKEY_derive) \
    X(EVP_PKEY_derive_init) \
    X(EVP_PKEY_derive_set_peer) \
    X(EVP_PKEY_encrypt) \
    X(EVP_PKEY_encrypt_init) \
    X(EVP_PKEY_free) \
    X(EVP_PKEY_get_id) \
    X(EVP_PKEY_keygen) \
    X(EVP_PKEY_keygen_init) \
    X(EVP_aes_128_cbc) \
    X(EVP_aes_128_ecb) \
    X(EVP_aes_128_gcm) \
    X(EVP_aes_192_cbc) \
    X(EVP_aes_192_ecb) \
    X(EVP_aes_192_gcm) \
    X(EVP_aes_256_cbc) \
    X(EVP_aes_256_ecb) \
    X(EVP_aes_256_gcm) \
    X(EVP_sha256) \
    X(EVP_sha384) \
    X(EVP_sha512) \
    X(OPENSSL_cleanse) \
    X(CRYPTO_free) \
    X(PKCS8_PRIV_KEY_INFO_free) \
    X(RAND_bytes) \
    X(d2i_AutoPrivateKey) \
    X(d2i_PKCS8_PRIV_KEY_INFO) \
    X(d2i_PUBKEY) \
    X(i2d_PKCS8_PRIV_KEY_INFO) \
    X(i2d_PUBKEY)

#define CN1_CRYPTO_POINTER(name) static __typeof__(name)* p_##name;
CN1_CRYPTO_FUNCTIONS(CN1_CRYPTO_POINTER)
#undef CN1_CRYPTO_POINTER

static pthread_once_t cn1CryptoOnce = PTHREAD_ONCE_INIT;
static int cn1CryptoAvailable;

static void cn1LoadCrypto(void) {
    void* library = dlopen("libcrypto.so.3", RTLD_NOW | RTLD_LOCAL);
    if (library == NULL) {
        fprintf(stderr, "Codename One: unable to load libcrypto.so.3: %s\n", dlerror());
        return;
    }
#define CN1_CRYPTO_RESOLVE(name) \
    if ((*(void**) (&p_##name) = dlsym(library, #name)) == NULL) { \
        fprintf(stderr, "Codename One: libcrypto.so.3 is missing %s\n", #name); \
        dlclose(library); \
        return; \
    }
    CN1_CRYPTO_FUNCTIONS(CN1_CRYPTO_RESOLVE)
#undef CN1_CRYPTO_RESOLVE
    cn1CryptoAvailable = 1;
}

static int cn1CryptoLoaded(void) {
    pthread_once(&cn1CryptoOnce, cn1LoadCrypto);
    return cn1CryptoAvailable;
}

#define ERR_clear_error p_ERR_clear_error
#define ERR_error_string_n p_ERR_error_string_n
#define ERR_get_error p_ERR_get_error
#define EVP_CIPHER_CTX_ctrl p_EVP_CIPHER_CTX_ctrl
#define EVP_CIPHER_CTX_free p_EVP_CIPHER_CTX_free
#define EVP_CIPHER_CTX_new p_EVP_CIPHER_CTX_new
#define EVP_CIPHER_CTX_set_padding p_EVP_CIPHER_CTX_set_padding
#define EVP_CipherFinal_ex p_EVP_CipherFinal_ex
#define EVP_CipherInit_ex p_EVP_CipherInit_ex
#define EVP_CipherUpdate p_EVP_CipherUpdate
#define EVP_DigestSign p_EVP_DigestSign
#define EVP_DigestSignInit p_EVP_DigestSignInit
#define EVP_DigestVerify p_EVP_DigestVerify
#define EVP_DigestVerifyInit p_EVP_DigestVerifyInit
#define EVP_MD_CTX_free p_EVP_MD_CTX_free
#define EVP_MD_CTX_new p_EVP_MD_CTX_new
#define EVP_PKCS82PKEY p_EVP_PKCS82PKEY
#define EVP_PKEY2PKCS8 p_EVP_PKEY2PKCS8
#define EVP_PKEY_CTX_free p_EVP_PKEY_CTX_free
#define EVP_PKEY_CTX_new p_EVP_PKEY_CTX_new
#define EVP_PKEY_CTX_new_id p_EVP_PKEY_CTX_new_id
#define EVP_PKEY_CTX_set_ec_paramgen_curve_nid p_EVP_PKEY_CTX_set_ec_paramgen_curve_nid
#define EVP_PKEY_CTX_set_rsa_keygen_bits p_EVP_PKEY_CTX_set_rsa_keygen_bits
#define EVP_PKEY_CTX_set_rsa_mgf1_md p_EVP_PKEY_CTX_set_rsa_mgf1_md
#define EVP_PKEY_CTX_set_rsa_oaep_md p_EVP_PKEY_CTX_set_rsa_oaep_md
#define EVP_PKEY_CTX_set_rsa_padding p_EVP_PKEY_CTX_set_rsa_padding
#define EVP_PKEY_get_base_id p_EVP_PKEY_get_base_id
#define EVP_PKEY_decrypt p_EVP_PKEY_decrypt
#define EVP_PKEY_decrypt_init p_EVP_PKEY_decrypt_init
#define EVP_PKEY_derive p_EVP_PKEY_derive
#define EVP_PKEY_derive_init p_EVP_PKEY_derive_init
#define EVP_PKEY_derive_set_peer p_EVP_PKEY_derive_set_peer
#define EVP_PKEY_encrypt p_EVP_PKEY_encrypt
#define EVP_PKEY_encrypt_init p_EVP_PKEY_encrypt_init
#define EVP_PKEY_free p_EVP_PKEY_free
#define EVP_PKEY_get_id p_EVP_PKEY_get_id
#define EVP_PKEY_keygen p_EVP_PKEY_keygen
#define EVP_PKEY_keygen_init p_EVP_PKEY_keygen_init
#define EVP_aes_128_cbc p_EVP_aes_128_cbc
#define EVP_aes_128_ecb p_EVP_aes_128_ecb
#define EVP_aes_128_gcm p_EVP_aes_128_gcm
#define EVP_aes_192_cbc p_EVP_aes_192_cbc
#define EVP_aes_192_ecb p_EVP_aes_192_ecb
#define EVP_aes_192_gcm p_EVP_aes_192_gcm
#define EVP_aes_256_cbc p_EVP_aes_256_cbc
#define EVP_aes_256_ecb p_EVP_aes_256_ecb
#define EVP_aes_256_gcm p_EVP_aes_256_gcm
#define EVP_sha256 p_EVP_sha256
#define EVP_sha384 p_EVP_sha384
#define EVP_sha512 p_EVP_sha512
#define OPENSSL_cleanse p_OPENSSL_cleanse
#define CRYPTO_free p_CRYPTO_free
#define PKCS8_PRIV_KEY_INFO_free p_PKCS8_PRIV_KEY_INFO_free
#define RAND_bytes p_RAND_bytes
#define d2i_AutoPrivateKey p_d2i_AutoPrivateKey
#define d2i_PKCS8_PRIV_KEY_INFO p_d2i_PKCS8_PRIV_KEY_INFO
#define d2i_PUBKEY p_d2i_PUBKEY
#define i2d_PKCS8_PRIV_KEY_INFO p_i2d_PKCS8_PRIV_KEY_INFO
#define i2d_PUBKEY p_i2d_PUBKEY


#define CN1_GCM_TAG_BYTES 16

/* Per-thread: crypto failures on different threads would otherwise overwrite
 * each other and lastCryptoError() could answer with an unrelated call's
 * message. */
static __thread char cn1CryptoError[512];

static void cn1CryptoFail(const char* what) {
    unsigned long code = cn1CryptoAvailable ? ERR_get_error() : 0;
    char detail[256];
    detail[0] = 0;
    if (code != 0) {
        ERR_error_string_n(code, detail, sizeof(detail));
    }
    snprintf(cn1CryptoError, sizeof(cn1CryptoError), "%s%s%s", what,
             detail[0] ? ": " : "", detail);
    if (cn1CryptoAvailable) {
        ERR_clear_error();
    }
}

/* Clears the per-thread message so a caller can tell whether the operation it
 * just ran recorded one. verifyData answers false for an invalid signature --
 * an ordinary result -- and also for an unusable algorithm or key, which is a
 * configuration error the caller deserves to see. The only difference between
 * them is whether an error was recorded, and that is only readable if the slot
 * was empty beforehand. */
JAVA_VOID com_codename1_impl_linux_LinuxNative_clearCryptoError__(CODENAME_ONE_THREAD_STATE) {
    cn1CryptoError[0] = 0;
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
    return (const unsigned char*) CN1_ARRAY_DATA(array);
}

/* ------------------------------------------------------------ random */

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_secureRandomBytes___byte_1ARRAY_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT out) {
    if (!cn1CryptoLoaded()) {
        cn1CryptoFail("libcrypto.so.3 could not be loaded");
        return JAVA_FALSE;
    }
    int length = 0;
    unsigned char* data = (unsigned char*) cn1Bytes(out, &length);
    if (data == 0 || length <= 0) {
        return JAVA_TRUE;
    }
    if (RAND_bytes(data, length) != 1) {
        // Report the failure rather than leaving the buffer as it stands:
        // KeyGenerator hands this straight back as key material, so a quiet
        // return would mint a predictable key.
        cn1CryptoFail("secure random");
        memset(data, 0, (size_t) length);
        return JAVA_FALSE;
    }
    return JAVA_TRUE;
}

/* ------------------------------------------- advertised names, and only those
 *
 * JavaSE hands these strings to JCE, which either supports a name as written or
 * refuses it. Matching loosely here -- picking CBC because a string contains
 * neither "/GCM/" nor "/ECB/", or SHA-256 because it names no digest we know --
 * means the same request encrypts or signs differently depending on which port
 * runs it, and the caller is never told. A name outside the advertised set is
 * refused instead.
 */

static int cn1IsAesTransformation(const char* transformation) {
    return strcmp(transformation, "AES/GCM/NoPadding") == 0
        || strcmp(transformation, "AES/CBC/PKCS5Padding") == 0
        || strcmp(transformation, "AES/CBC/NoPadding") == 0
        || strcmp(transformation, "AES/ECB/PKCS5Padding") == 0;
}

static int cn1IsRsaTransformation(const char* transformation) {
    return strcmp(transformation, "RSA/ECB/OAEPWithSHA-256AndMGF1Padding") == 0
        || strcmp(transformation, "RSA/ECB/PKCS1Padding") == 0;
}

/* The digest half of Signature's six advertised algorithms; 0 for anything
 * else, which the callers turn into a failure. */
static const EVP_MD* cn1SignatureDigestOrNull(const char* algorithm) {
    if (strcmp(algorithm, "SHA256withRSA") == 0 || strcmp(algorithm, "SHA256withECDSA") == 0) {
        return EVP_sha256();
    }
    if (strcmp(algorithm, "SHA384withRSA") == 0 || strcmp(algorithm, "SHA384withECDSA") == 0) {
        return EVP_sha384();
    }
    if (strcmp(algorithm, "SHA512withRSA") == 0 || strcmp(algorithm, "SHA512withECDSA") == 0) {
        return EVP_sha512();
    }
    return 0;
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
    if (!cn1CryptoLoaded()) {
        cn1CryptoFail("libcrypto.so.3 could not be loaded");
        return JAVA_NULL;
    }
    const char* mode = transformation == JAVA_NULL ? "" : stringToUTF8(threadStateData, transformation);
    int keyLength = 0, ivLength = 0, aadLength = 0, dataLength = 0;
    const unsigned char* key = cn1Bytes(keyArray, &keyLength);
    const unsigned char* iv = cn1Bytes(ivArray, &ivLength);
    const unsigned char* aad = cn1Bytes(aadArray, &aadLength);
    const unsigned char* data = cn1Bytes(dataArray, &dataLength);
    int gcm;
    if (!cn1IsAesTransformation(mode)) {
        cn1CryptoFail("unsupported cipher transformation");
        return JAVA_NULL;
    }
    gcm = strstr(mode, "/GCM/") != 0;
    int ecb = strstr(mode, "/ECB/") != 0;
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
    // OpenSSL would otherwise silently keep the context's zeroed default IV for
    // a missing GCM nonce -- repeating a nonce under one key destroys GCM --
    // and read a whole block past a short CBC IV.
    if (gcm && ivLength <= 0) {
        cn1CryptoFail("AES-GCM requires a nonce");
        return JAVA_NULL;
    }
    if (!gcm && !ecb && ivLength != 16) {
        cn1CryptoFail("AES-CBC requires a 16 byte initialization vector");
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

/* d2i_* stops at the end of the first object it recognises and reports how far
 * it got, so a buffer holding a valid key followed by extra bytes -- a
 * mis-sliced concatenation, say -- parses happily and the trailing data is
 * never seen. JavaSE and Android refuse that through KeyFactory, so accepting
 * it here would mean the same bytes validate on one port and not another. The
 * whole input has to be the key. */
static EVP_PKEY* cn1PublicKey(const unsigned char* der, int length) {
    const unsigned char* cursor = der;
    EVP_PKEY* key = d2i_PUBKEY(0, &cursor, (long) length);
    if (key == 0) {
        cn1CryptoFail("public key is not X.509 SubjectPublicKeyInfo DER");
        return 0;
    }
    if (cursor != der + length) {
        cn1CryptoFail("public key has trailing data after the SubjectPublicKeyInfo");
        EVP_PKEY_free(key);
        return 0;
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
        /* PKCS#8 only, deliberately. This used to fall back to
         * d2i_AutoPrivateKey, which also accepts bare PKCS#1 RSA and SEC1 EC
         * keys -- but the public entry point is PrivateKey.fromPkcs8(), JavaSE
         * and Android feed it to PKCS8EncodedKeySpec and Windows imports it as
         * NCRYPT_PKCS8_PRIVATE_KEY_BLOB, so all of those reject those
         * encodings. Accepting them here meant the identical key loaded on
         * Linux and nowhere else, which is a portability trap rather than a
         * kindness: code written against this port would fail on every device
         * it shipped to.
         *
         * The failed attempt above queued an entry on this thread's OpenSSL
         * error queue; clear it so the next unrelated operation to read the
         * queue does not report it. */
        ERR_clear_error();
        cn1CryptoFail("private key is not PKCS#8 DER");
        return 0;
    }
    /* Same rule as the public side: the whole buffer has to be the key. */
    if (cursor != der + length) {
        cn1CryptoFail("private key has trailing data after the PKCS#8 structure");
        EVP_PKEY_free(key);
        return 0;
    }
    return key;
}

static int cn1ApplyRsaPadding(EVP_PKEY_CTX* ctx, const char* transformation) {
    if (!cn1IsRsaTransformation(transformation)) {
        cn1CryptoFail("unsupported cipher transformation");
        return 0;
    }
    if (strstr(transformation, "OAEP") != 0) {
        const EVP_MD* md = EVP_sha256();
        // SHA-256 for the label hash AND the mask. The JCE transformation name
        // leaves MGF1 on SHA-1 by default, but no other backend in this project
        // can reproduce that: Web Crypto's RSA-OAEP takes a single hash and uses
        // it for both, and so does Apple's SecKey. SHA-256 for both is the only
        // pairing every port can produce, so it is the one the portable constant
        // means -- the JavaSE and Android ports name it explicitly rather than
        // inheriting their provider's default.
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
    if (!cn1CryptoLoaded()) {
        cn1CryptoFail("libcrypto.so.3 could not be loaded");
        return JAVA_NULL;
    }
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

/* The signature algorithm's family must match the key that was actually handed
 * over, read from the DER rather than from the caller's label. PrivateKey.
 * fromPkcs8 and PublicKey.fromX509 take an arbitrary algorithm string and never
 * check it against the bytes, so a Java-side comparison of that label can be
 * satisfied while the encoded key is a different family entirely -- and the
 * primitive below would then sign an "RSA" request with ECDSA. */
static int cn1KeyFamilyMatches(const char* algorithm, EVP_PKEY* key) {
    /* Name the family that is required rather than testing "not EC": every
     * other key type would otherwise pass as RSA, so DSA bytes carrying an
     * "RSA" label would reach EVP_DigestSign and come back as a DSA signature
     * under a SHA256withRSA request. */
    int id = EVP_PKEY_base_id(key);
    if (strstr(algorithm, "ECDSA") != 0) {
        return id == EVP_PKEY_EC;
    }
    return id == EVP_PKEY_RSA;
}


static const EVP_MD* cn1SignatureDigest(const char* algorithm) {
    return cn1SignatureDigestOrNull(algorithm);
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_signData___java_lang_String_byte_1ARRAY_byte_1ARRAY_R_byte_1ARRAY(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT algorithm, JAVA_OBJECT keyArray, JAVA_OBJECT dataArray) {
    if (!cn1CryptoLoaded()) {
        cn1CryptoFail("libcrypto.so.3 could not be loaded");
        return JAVA_NULL;
    }
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
    if (cn1SignatureDigest(name) == 0) {
        /* Not one of Signature's advertised algorithms. Passing a null digest
         * on would let OpenSSL choose one, which is how an unsupported name
         * used to come back as a valid signature over a different digest. */
        cn1CryptoFail("unsupported signature algorithm");
        EVP_PKEY_free(key);
        return JAVA_NULL;
    }
    if (!cn1KeyFamilyMatches(name, key)) {
        cn1CryptoFail("the signature algorithm does not match the key");
        EVP_PKEY_free(key);
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
    if (!cn1CryptoLoaded()) {
        cn1CryptoFail("libcrypto.so.3 could not be loaded");
        return JAVA_FALSE;
    }
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
    if (cn1SignatureDigest(name) == 0) {
        cn1CryptoFail("unsupported signature algorithm");
        EVP_PKEY_free(key);
        return JAVA_FALSE;
    }
    if (!cn1KeyFamilyMatches(name, key)) {
        cn1CryptoFail("the signature algorithm does not match the key");
        EVP_PKEY_free(key);
        return JAVA_FALSE;
    }
    ctx = EVP_MD_CTX_new();
    if (ctx == 0) {
        cn1CryptoFail("digest context");
        EVP_PKEY_free(key);
        return JAVA_FALSE;
    }
    if (EVP_DigestVerifyInit(ctx, 0, cn1SignatureDigest(name), 0, key) <= 0) {
        /* Setup failed, so nothing was verified. Returning a bare false here
         * said "that signature is invalid" about a signature never examined. */
        cn1CryptoFail("could not initialise signature verification");
    } else {
        /* EVP_DigestVerify is tri-state: 1 verified, 0 rejected, negative an
         * internal error. Folding negative in with zero reported a broken
         * verifier as an ordinary bad signature. */
        int verified = EVP_DigestVerify(ctx, signature, (size_t) signatureLength,
                                        data, (size_t) dataLength);
        if (verified == 1) {
            result = JAVA_TRUE;
        } else if (verified < 0) {
            cn1CryptoFail("signature verification failed to run");
        } else {
            /* A rejected signature is a normal answer, not a fault; clear the
             * queue so it cannot be reported against a later operation. */
            ERR_clear_error();
        }
    }
    EVP_MD_CTX_free(ctx);
    EVP_PKEY_free(key);
    return result;
}

/* ------------------------------------------------------------ key pairs */

/* Same layout as generateRsaKeyPair below. Keygen straight from an EC context
 * with the curve set works on OpenSSL 1.1 and 3 alike. */
JAVA_OBJECT com_codename1_impl_linux_LinuxNative_generateEcKeyPair___R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE) {
    if (!cn1CryptoLoaded()) {
        cn1CryptoFail("libcrypto.so.3 could not be loaded");
        return JAVA_NULL;
    }
    EVP_PKEY_CTX* ctx = EVP_PKEY_CTX_new_id(EVP_PKEY_EC, 0);
    EVP_PKEY* key = 0;
    PKCS8_PRIV_KEY_INFO* info = 0;
    unsigned char* publicDer = 0;
    unsigned char* privateDer = 0;
    unsigned char* blob = 0;
    int publicLength = 0, privateLength = 0;
    JAVA_OBJECT result = JAVA_NULL;

    if (ctx == 0) {
        cn1CryptoFail("EC keygen context");
        return JAVA_NULL;
    }
    if (EVP_PKEY_keygen_init(ctx) <= 0 ||
        EVP_PKEY_CTX_set_ec_paramgen_curve_nid(ctx, NID_X9_62_prime256v1) <= 0 ||
        EVP_PKEY_keygen(ctx, &key) <= 0) {
        cn1CryptoFail("EC keygen");
        goto done;
    }
    publicLength = i2d_PUBKEY(key, &publicDer);
    info = EVP_PKEY2PKCS8(key);
    if (info != 0) {
        privateLength = i2d_PKCS8_PRIV_KEY_INFO(info, &privateDer);
    }
    if (publicLength <= 0 || privateLength <= 0) {
        cn1CryptoFail("EC key encoding");
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

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_ecdh___byte_1ARRAY_byte_1ARRAY_R_byte_1ARRAY(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT privateArray, JAVA_OBJECT publicArray) {
    if (!cn1CryptoLoaded()) {
        cn1CryptoFail("libcrypto.so.3 could not be loaded");
        return JAVA_NULL;
    }
    int privateLength = 0, publicLength = 0;
    const unsigned char* privateDer = cn1Bytes(privateArray, &privateLength);
    const unsigned char* publicDer = cn1Bytes(publicArray, &publicLength);
    EVP_PKEY* mine = cn1PrivateKey(privateDer, privateLength);
    EVP_PKEY* theirs = 0;
    EVP_PKEY_CTX* ctx = 0;
    unsigned char secret[66];
    size_t secretLength = sizeof(secret);
    JAVA_OBJECT result = JAVA_NULL;

    if (mine == 0) {
        return JAVA_NULL;
    }
    theirs = cn1PublicKey(publicDer, publicLength);
    if (theirs == 0) {
        EVP_PKEY_free(mine);
        return JAVA_NULL;
    }
    if (EVP_PKEY_id(mine) != EVP_PKEY_EC || EVP_PKEY_id(theirs) != EVP_PKEY_EC) {
        cn1CryptoFail("ECDH needs EC keys");
        goto done;
    }
    ctx = EVP_PKEY_CTX_new(mine, 0);
    if (ctx == 0 || EVP_PKEY_derive_init(ctx) <= 0 ||
        EVP_PKEY_derive_set_peer(ctx, theirs) <= 0 ||
        EVP_PKEY_derive(ctx, secret, &secretLength) <= 0) {
        cn1CryptoFail("ECDH");
        goto done;
    }
    result = cn1LinuxNewByteArray(threadStateData, secret, (int) secretLength);

done:
    OPENSSL_cleanse(secret, sizeof(secret));
    EVP_PKEY_CTX_free(ctx);
    EVP_PKEY_free(theirs);
    EVP_PKEY_free(mine);
    return result;
}

/* Returns the pair as one array: a four-byte big-endian public-key length,
 * the X.509 public key, then the PKCS#8 private key. A pair has to come from
 * a single call -- two calls would produce two unrelated keys. */
JAVA_OBJECT com_codename1_impl_linux_LinuxNative_generateRsaKeyPair___int_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_INT bits) {
    if (!cn1CryptoLoaded()) {
        cn1CryptoFail("libcrypto.so.3 could not be loaded");
        return JAVA_NULL;
    }
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
