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

/*
 * The crypto a server needs to authenticate a request: SHA-256, HMAC-SHA-256,
 * PBKDF2 and a source of randomness that is actually random.
 *
 * All four come from OpenSSL, which the backend already links for outbound TLS.
 * None of them is written here. Hand-rolled HMAC and hand-rolled password hashing
 * are the two most reliable ways to ship an authentication system that looks
 * correct and is not, and a constant-time comparison written in Java would be
 * compiled into something that is not constant time.
 */
#include "cn1_globals.h"
#include <string.h>
#include <stdlib.h>
#ifndef _WIN32
#include <unistd.h> /* CN1_RESUME_THREAD expands to usleep */
#endif
#include <openssl/sha.h>
#include <openssl/md5.h>
#include <openssl/hmac.h>
#include <openssl/evp.h>
#include <openssl/rand.h>
#include <openssl/crypto.h>
#include <openssl/rsa.h>
#include <openssl/ec.h>
#include <openssl/x509.h>
#include <openssl/objects.h>
#include <openssl/err.h>

static JAVA_OBJECT cn1BytesToArray(CODENAME_ONE_THREAD_STATE, const unsigned char* data, int length) {
    JAVA_OBJECT arr = allocArray(threadStateData, length, &class_array1__JAVA_BYTE, sizeof(JAVA_ARRAY_BYTE), 1);
    if(length > 0 && data != NULL) {
        memcpy((JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA(arr), data, (size_t)length);
    }
    return arr;
}

JAVA_OBJECT com_codename1_backend_Crypto_sha256Impl___byte_1ARRAY_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT data) {
    unsigned char digest[SHA256_DIGEST_LENGTH];
    JAVA_ARRAY arr;
    if(data == JAVA_NULL) {
        return JAVA_NULL;
    }
    arr = (JAVA_ARRAY)data;
    SHA256((const unsigned char*)(JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA(arr), (size_t)arr->length, digest);
    return cn1BytesToArray(threadStateData, digest, SHA256_DIGEST_LENGTH);
}

/*
 * SHA-1 and MD5 are here for one reason: the database wire protocols specify
 * them. MySQL's mysql_native_password is SHA1-based and PostgreSQL's md5 method
 * is MD5-based, and a client that refuses them cannot talk to the servers that
 * are deployed. Neither is used for anything this code chooses -- passwords go
 * through PBKDF2 and tokens through HMAC-SHA-256.
 */
JAVA_OBJECT com_codename1_backend_Crypto_sha1Impl___byte_1ARRAY_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT data) {
    unsigned char digest[SHA_DIGEST_LENGTH];
    JAVA_ARRAY arr;
    if(data == JAVA_NULL) {
        return JAVA_NULL;
    }
    arr = (JAVA_ARRAY)data;
    SHA1((const unsigned char*)(JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA(arr), (size_t)arr->length, digest);
    return cn1BytesToArray(threadStateData, digest, SHA_DIGEST_LENGTH);
}

JAVA_OBJECT com_codename1_backend_Crypto_md5Impl___byte_1ARRAY_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT data) {
    unsigned char digest[MD5_DIGEST_LENGTH];
    JAVA_ARRAY arr;
    if(data == JAVA_NULL) {
        return JAVA_NULL;
    }
    arr = (JAVA_ARRAY)data;
    MD5((const unsigned char*)(JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA(arr), (size_t)arr->length, digest);
    return cn1BytesToArray(threadStateData, digest, MD5_DIGEST_LENGTH);
}

JAVA_OBJECT com_codename1_backend_Crypto_hmacSha256Impl___byte_1ARRAY_byte_1ARRAY_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT key, JAVA_OBJECT data) {
    unsigned char mac[EVP_MAX_MD_SIZE];
    unsigned int macLength = 0;
    JAVA_ARRAY keyArr;
    JAVA_ARRAY dataArr;
    if(key == JAVA_NULL || data == JAVA_NULL) {
        return JAVA_NULL;
    }
    keyArr = (JAVA_ARRAY)key;
    dataArr = (JAVA_ARRAY)data;
    if(HMAC(EVP_sha256(),
            (const void*)(JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA(keyArr), (int)keyArr->length,
            (const unsigned char*)(JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA(dataArr), (size_t)dataArr->length,
            mac, &macLength) == NULL) {
        return JAVA_NULL;
    }
    return cn1BytesToArray(threadStateData, mac, (int)macLength);
}

JAVA_OBJECT com_codename1_backend_Crypto_pbkdf2Impl___byte_1ARRAY_byte_1ARRAY_int_int_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT password, JAVA_OBJECT salt, JAVA_INT iterations, JAVA_INT length) {
    JAVA_ARRAY pw;
    JAVA_ARRAY sl;
    unsigned char* out;
    JAVA_OBJECT result;
    if(password == JAVA_NULL || salt == JAVA_NULL || length <= 0 || iterations <= 0) {
        return JAVA_NULL;
    }
    pw = (JAVA_ARRAY)password;
    sl = (JAVA_ARRAY)salt;
    out = (unsigned char*)malloc((size_t)length);
    if(out == NULL) {
        return JAVA_NULL;
    }
    CN1_YIELD_THREAD; /* deliberately slow; do not stall the collector on it */
    if(PKCS5_PBKDF2_HMAC((const char*)(JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA(pw), (int)pw->length,
                         (const unsigned char*)(JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA(sl), (int)sl->length,
                         (int)iterations, EVP_sha256(), (int)length, out) != 1) {
        CN1_RESUME_THREAD;
        free(out);
        return JAVA_NULL;
    }
    CN1_RESUME_THREAD;
    result = cn1BytesToArray(threadStateData, out, length);
    OPENSSL_cleanse(out, (size_t)length);
    free(out);
    return result;
}

/*
 * Cryptographically secure randomness, not java.util.Random. A session token or a
 * salt drawn from a predictable generator is a forgeable one.
 */
JAVA_OBJECT com_codename1_backend_Crypto_randomBytesImpl___int_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_INT length) {
    unsigned char* out;
    JAVA_OBJECT result;
    if(length <= 0) {
        return JAVA_NULL;
    }
    out = (unsigned char*)malloc((size_t)length);
    if(out == NULL) {
        return JAVA_NULL;
    }
    if(RAND_bytes(out, (int)length) != 1) {
        /* Never fall back to a weaker source: a caller that gets bytes assumes they
           are unpredictable, and there is no way to signal "these are not". */
        free(out);
        return JAVA_NULL;
    }
    result = cn1BytesToArray(threadStateData, out, length);
    OPENSSL_cleanse(out, (size_t)length);
    free(out);
    return result;
}

/*
 * Constant-time comparison. In Java this would be compiled into whatever the
 * optimizer likes, and an early exit on the first differing byte leaks the prefix
 * length of a guess -- which is enough to forge a MAC one byte at a time.
 */
JAVA_BOOLEAN com_codename1_backend_Crypto_equalsConstantTimeImpl___byte_1ARRAY_byte_1ARRAY_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT a, JAVA_OBJECT b) {
    JAVA_ARRAY aa;
    JAVA_ARRAY bb;
    if(a == JAVA_NULL || b == JAVA_NULL) {
        return JAVA_FALSE;
    }
    aa = (JAVA_ARRAY)a;
    bb = (JAVA_ARRAY)b;
    if(aa->length != bb->length) {
        return JAVA_FALSE;
    }
    return CRYPTO_memcmp((const void*)(JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA(aa),
                         (const void*)(JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA(bb),
                         (size_t)aa->length) == 0 ? JAVA_TRUE : JAVA_FALSE;
}

/*
 * Everything below takes its algorithm as a number the Java twin chose from a
 * fixed list, never as a name: a name would have to be turned into a C string,
 * and a digest looked up by a string a caller supplied is one more thing that
 * can be made to pick the wrong primitive.
 *
 *   digests     1 SHA-1, 2 SHA-256, 3 SHA-384, 4 SHA-512
 *   signatures  1 RS256, 2 RS384, 3 RS512, 4 PS256, 5 ES256, 6 ES384
 */
static const EVP_MD* cn1DigestFor(JAVA_INT algorithm) {
    switch(algorithm) {
        case 1: return EVP_sha1();
        case 2: return EVP_sha256();
        case 3: return EVP_sha384();
        case 4: return EVP_sha512();
        default: return NULL;
    }
}

JAVA_OBJECT com_codename1_backend_Crypto_digestImpl___int_byte_1ARRAY_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_INT algorithm, JAVA_OBJECT data) {
    unsigned char digest[EVP_MAX_MD_SIZE];
    unsigned int length = 0;
    const EVP_MD* md = cn1DigestFor(algorithm);
    JAVA_ARRAY arr;
    if(data == JAVA_NULL || md == NULL) {
        return JAVA_NULL;
    }
    arr = (JAVA_ARRAY)data;
    if(EVP_Digest((const void*)(JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA(arr), (size_t)arr->length,
                  digest, &length, md, NULL) != 1) {
        return JAVA_NULL;
    }
    return cn1BytesToArray(threadStateData, digest, (int)length);
}

JAVA_OBJECT com_codename1_backend_Crypto_hmacImpl___int_byte_1ARRAY_byte_1ARRAY_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_INT algorithm, JAVA_OBJECT key, JAVA_OBJECT data) {
    unsigned char mac[EVP_MAX_MD_SIZE];
    unsigned int macLength = 0;
    const EVP_MD* md = cn1DigestFor(algorithm);
    JAVA_ARRAY keyArr;
    JAVA_ARRAY dataArr;
    if(key == JAVA_NULL || data == JAVA_NULL || md == NULL) {
        return JAVA_NULL;
    }
    keyArr = (JAVA_ARRAY)key;
    dataArr = (JAVA_ARRAY)data;
    if(HMAC(md, (const void*)(JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA(keyArr), (int)keyArr->length,
            (const unsigned char*)(JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA(dataArr), (size_t)dataArr->length,
            mac, &macLength) == NULL) {
        return JAVA_NULL;
    }
    return cn1BytesToArray(threadStateData, mac, (int)macLength);
}

JAVA_OBJECT com_codename1_backend_Crypto_pbkdf2DigestImpl___int_byte_1ARRAY_byte_1ARRAY_int_int_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_INT algorithm, JAVA_OBJECT password, JAVA_OBJECT salt, JAVA_INT iterations, JAVA_INT length) {
    JAVA_ARRAY pw;
    JAVA_ARRAY sl;
    unsigned char* out;
    JAVA_OBJECT result;
    const EVP_MD* md = cn1DigestFor(algorithm);
    if(password == JAVA_NULL || salt == JAVA_NULL || md == NULL || length <= 0 || iterations <= 0) {
        return JAVA_NULL;
    }
    pw = (JAVA_ARRAY)password;
    sl = (JAVA_ARRAY)salt;
    out = (unsigned char*)malloc((size_t)length);
    if(out == NULL) {
        return JAVA_NULL;
    }
    CN1_YIELD_THREAD; /* deliberately slow; do not stall the collector on it */
    if(PKCS5_PBKDF2_HMAC((const char*)(JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA(pw), (int)pw->length,
                         (const unsigned char*)(JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA(sl), (int)sl->length,
                         (int)iterations, md, (int)length, out) != 1) {
        CN1_RESUME_THREAD;
        free(out);
        return JAVA_NULL;
    }
    CN1_RESUME_THREAD;
    result = cn1BytesToArray(threadStateData, out, length);
    OPENSSL_cleanse(out, (size_t)length);
    free(out);
    return result;
}

/*
 * Whether a key is of the kind a signature algorithm is defined over. This is
 * the check that stops an RSA key being used under an EC algorithm's name, or a
 * P-384 key under ES256: the algorithm is never allowed to follow the key, nor
 * the key the algorithm.
 */
static int cn1KeyFits(EVP_PKEY* key, JAVA_INT algorithm) {
    int type = EVP_PKEY_base_id(key);
    if(algorithm >= 1 && algorithm <= 4) {
        return type == EVP_PKEY_RSA;
    }
    if(algorithm == 5 || algorithm == 6) {
        char group[64];
        size_t groupLength = 0;
        if(type != EVP_PKEY_EC) {
            return 0;
        }
        if(EVP_PKEY_get_utf8_string_param(key, "group", group, sizeof(group), &groupLength) != 1) {
            return 0;
        }
        return OBJ_sn2nid(group) == (algorithm == 5 ? NID_X9_62_prime256v1 : NID_secp384r1);
    }
    return 0;
}

static const EVP_MD* cn1SignatureDigest(JAVA_INT algorithm) {
    switch(algorithm) {
        case 1: case 4: case 5: return EVP_sha256();
        case 2: case 6: return EVP_sha384();
        case 3: return EVP_sha512();
        default: return NULL;
    }
}

/* RSASSA-PSS as JOSE's PS256 fixes it: MGF1 over the same digest, and a salt as
   long as the digest. */
static int cn1ConfigurePss(EVP_PKEY_CTX* pctx, JAVA_INT algorithm, const EVP_MD* md) {
    if(algorithm != 4) {
        return 1;
    }
    return EVP_PKEY_CTX_set_rsa_padding(pctx, RSA_PKCS1_PSS_PADDING) > 0
            && EVP_PKEY_CTX_set_rsa_pss_saltlen(pctx, RSA_PSS_SALTLEN_DIGEST) > 0
            && EVP_PKEY_CTX_set_rsa_mgf1_md(pctx, md) > 0;
}

/*
 * Signs with a PKCS#8 private key. An ECDSA signature comes back as OpenSSL
 * makes it, the ASN.1 DER SEQUENCE of r and s; the Java side turns it into
 * whatever a protocol wants. Null for any failure.
 */
JAVA_OBJECT com_codename1_backend_Crypto_signImpl___int_byte_1ARRAY_byte_1ARRAY_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_INT algorithm, JAVA_OBJECT pkcs8, JAVA_OBJECT data) {
    JAVA_ARRAY keyArr;
    JAVA_ARRAY dataArr;
    const unsigned char* p;
    EVP_PKEY* key;
    EVP_MD_CTX* ctx = NULL;
    EVP_PKEY_CTX* pctx = NULL;
    unsigned char* signature = NULL;
    size_t signatureLength = 0;
    JAVA_OBJECT result = JAVA_NULL;
    const EVP_MD* md = cn1SignatureDigest(algorithm);
    if(pkcs8 == JAVA_NULL || data == JAVA_NULL || md == NULL) {
        return JAVA_NULL;
    }
    keyArr = (JAVA_ARRAY)pkcs8;
    dataArr = (JAVA_ARRAY)data;
    p = (const unsigned char*)(JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA(keyArr);
    key = d2i_AutoPrivateKey(NULL, &p, (long)keyArr->length);
    if(key == NULL) {
        ERR_clear_error();
        return JAVA_NULL;
    }
    if(cn1KeyFits(key, algorithm)
            && (ctx = EVP_MD_CTX_new()) != NULL
            && EVP_DigestSignInit(ctx, &pctx, md, NULL, key) == 1
            && cn1ConfigurePss(pctx, algorithm, md)
            && EVP_DigestSign(ctx, NULL, &signatureLength,
                    (const unsigned char*)(JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA(dataArr), (size_t)dataArr->length) == 1
            && (signature = (unsigned char*)malloc(signatureLength)) != NULL
            && EVP_DigestSign(ctx, signature, &signatureLength,
                    (const unsigned char*)(JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA(dataArr), (size_t)dataArr->length) == 1) {
        result = cn1BytesToArray(threadStateData, signature, (int)signatureLength);
    }
    free(signature);
    EVP_MD_CTX_free(ctx);
    EVP_PKEY_free(key);
    ERR_clear_error();
    return result;
}

/*
 * Verifies against a SubjectPublicKeyInfo. Three answers, because "this is not
 * the signer's signature" and "this could not be checked" are different facts
 * and a caller that cannot tell them apart reports a broken key as a forged
 * token: 1 valid, 0 not valid, -1 the key or the algorithm is unusable.
 */
JAVA_INT com_codename1_backend_Crypto_verifyImpl___int_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT algorithm, JAVA_OBJECT spki, JAVA_OBJECT data, JAVA_OBJECT signature) {
    JAVA_ARRAY keyArr;
    JAVA_ARRAY dataArr;
    JAVA_ARRAY sigArr;
    const unsigned char* p;
    EVP_PKEY* key;
    EVP_MD_CTX* ctx = NULL;
    EVP_PKEY_CTX* pctx = NULL;
    JAVA_INT result = -1;
    const EVP_MD* md = cn1SignatureDigest(algorithm);
    if(spki == JAVA_NULL || data == JAVA_NULL || signature == JAVA_NULL || md == NULL) {
        return -1;
    }
    keyArr = (JAVA_ARRAY)spki;
    dataArr = (JAVA_ARRAY)data;
    sigArr = (JAVA_ARRAY)signature;
    p = (const unsigned char*)(JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA(keyArr);
    key = d2i_PUBKEY(NULL, &p, (long)keyArr->length);
    if(key == NULL) {
        ERR_clear_error();
        return -1;
    }
    if(cn1KeyFits(key, algorithm)
            && (ctx = EVP_MD_CTX_new()) != NULL
            && EVP_DigestVerifyInit(ctx, &pctx, md, NULL, key) == 1
            && cn1ConfigurePss(pctx, algorithm, md)) {
        /* Anything but 1 from here is a signature that does not verify --
           wrong bytes, wrong length, or DER that is not a signature at all. */
        result = EVP_DigestVerify(ctx,
                (const unsigned char*)(JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA(sigArr), (size_t)sigArr->length,
                (const unsigned char*)(JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA(dataArr), (size_t)dataArr->length) == 1 ? 1 : 0;
    }
    EVP_MD_CTX_free(ctx);
    EVP_PKEY_free(key);
    ERR_clear_error();
    return result;
}

/* A new RSA key as PKCS#8 DER, public exponent 65537. For a development
   profile that has no key of its own; see Crypto.generateRsaKey. */
JAVA_OBJECT com_codename1_backend_Crypto_generateRsaKeyImpl___int_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_INT bits) {
    EVP_PKEY* key;
    PKCS8_PRIV_KEY_INFO* info;
    unsigned char* der = NULL;
    int derLength;
    JAVA_OBJECT result = JAVA_NULL;
    if(bits < 2048 || bits > 8192) {
        return JAVA_NULL;
    }
    CN1_YIELD_THREAD; /* a prime search: tens to hundreds of milliseconds */
    key = EVP_RSA_gen((unsigned int)bits);
    CN1_RESUME_THREAD;
    if(key == NULL) {
        ERR_clear_error();
        return JAVA_NULL;
    }
    info = EVP_PKEY2PKCS8(key);
    EVP_PKEY_free(key);
    if(info == NULL) {
        ERR_clear_error();
        return JAVA_NULL;
    }
    derLength = i2d_PKCS8_PRIV_KEY_INFO(info, &der);
    PKCS8_PRIV_KEY_INFO_free(info);
    if(derLength > 0 && der != NULL) {
        result = cn1BytesToArray(threadStateData, der, derLength);
        OPENSSL_cleanse(der, (size_t)derLength);
    }
    OPENSSL_free(der);
    ERR_clear_error();
    return result;
}

/*
 * AES-GCM with a 16 byte tag appended to the ciphertext, which is the layout
 * the JDK's cipher produces and expects -- so a value sealed on one runtime
 * opens on the other. Null for any failure; when decrypting, a tag that does
 * not match is the only failure left once the Java side has checked the sizes.
 */
JAVA_OBJECT com_codename1_backend_Crypto_aesGcmImpl___boolean_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_BOOLEAN encrypt, JAVA_OBJECT key, JAVA_OBJECT iv, JAVA_OBJECT aad, JAVA_OBJECT input) {
    JAVA_ARRAY keyArr;
    JAVA_ARRAY ivArr;
    JAVA_ARRAY aadArr;
    JAVA_ARRAY inArr;
    const EVP_CIPHER* cipher;
    EVP_CIPHER_CTX* ctx;
    unsigned char* out;
    const unsigned char* in;
    int inLength;
    int bodyLength;
    int produced = 0;
    int finalLength = 0;
    int ok;
    JAVA_OBJECT result = JAVA_NULL;
    if(key == JAVA_NULL || iv == JAVA_NULL || input == JAVA_NULL) {
        return JAVA_NULL;
    }
    keyArr = (JAVA_ARRAY)key;
    ivArr = (JAVA_ARRAY)iv;
    aadArr = (JAVA_ARRAY)aad;
    inArr = (JAVA_ARRAY)input;
    switch(keyArr->length) {
        case 16: cipher = EVP_aes_128_gcm(); break;
        case 24: cipher = EVP_aes_192_gcm(); break;
        case 32: cipher = EVP_aes_256_gcm(); break;
        default: return JAVA_NULL;
    }
    inLength = (int)inArr->length;
    if(ivArr->length <= 0 || (!encrypt && inLength < 16)) {
        return JAVA_NULL;
    }
    bodyLength = encrypt ? inLength : inLength - 16;
    in = (const unsigned char*)(JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA(inArr);
    out = (unsigned char*)malloc((size_t)bodyLength + 16);
    ctx = EVP_CIPHER_CTX_new();
    if(out == NULL || ctx == NULL) {
        free(out);
        EVP_CIPHER_CTX_free(ctx);
        return JAVA_NULL;
    }
    ok = EVP_CipherInit_ex(ctx, cipher, NULL, NULL, NULL, encrypt ? 1 : 0) == 1
            && EVP_CIPHER_CTX_ctrl(ctx, EVP_CTRL_GCM_SET_IVLEN, (int)ivArr->length, NULL) == 1
            && EVP_CipherInit_ex(ctx, NULL, NULL,
                    (const unsigned char*)(JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA(keyArr),
                    (const unsigned char*)(JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA(ivArr), -1) == 1;
    if(ok && aad != JAVA_NULL && aadArr->length > 0) {
        ok = EVP_CipherUpdate(ctx, NULL, &produced,
                (const unsigned char*)(JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA(aadArr), (int)aadArr->length) == 1;
    }
    produced = 0;
    if(ok && bodyLength > 0) {
        ok = EVP_CipherUpdate(ctx, out, &produced, in, bodyLength) == 1;
    }
    if(ok && !encrypt) {
        /* The tag has to be set before the final call, which is what checks it. */
        ok = EVP_CIPHER_CTX_ctrl(ctx, EVP_CTRL_GCM_SET_TAG, 16, (void*)(in + bodyLength)) == 1;
    }
    if(ok) {
        ok = EVP_CipherFinal_ex(ctx, out + produced, &finalLength) == 1;
    }
    if(ok && encrypt) {
        ok = EVP_CIPHER_CTX_ctrl(ctx, EVP_CTRL_GCM_GET_TAG, 16, out + produced + finalLength) == 1;
    }
    if(ok) {
        result = cn1BytesToArray(threadStateData, out, produced + finalLength + (encrypt ? 16 : 0));
    }
    OPENSSL_cleanse(out, (size_t)bodyLength + 16);
    free(out);
    EVP_CIPHER_CTX_free(ctx);
    ERR_clear_error();
    return result;
}
