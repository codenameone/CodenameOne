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

static JAVA_OBJECT cn1BytesToArray(CODENAME_ONE_THREAD_STATE, const unsigned char* data, int length) {
    JAVA_OBJECT arr = allocArray(threadStateData, length, &class_array1__JAVA_BYTE, sizeof(JAVA_ARRAY_BYTE), 1);
    if(length > 0 && data != NULL) {
        memcpy((JAVA_ARRAY_BYTE*)((JAVA_ARRAY)arr)->data, data, (size_t)length);
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
    SHA256((const unsigned char*)(JAVA_ARRAY_BYTE*)arr->data, (size_t)arr->length, digest);
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
    SHA1((const unsigned char*)(JAVA_ARRAY_BYTE*)arr->data, (size_t)arr->length, digest);
    return cn1BytesToArray(threadStateData, digest, SHA_DIGEST_LENGTH);
}

JAVA_OBJECT com_codename1_backend_Crypto_md5Impl___byte_1ARRAY_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT data) {
    unsigned char digest[MD5_DIGEST_LENGTH];
    JAVA_ARRAY arr;
    if(data == JAVA_NULL) {
        return JAVA_NULL;
    }
    arr = (JAVA_ARRAY)data;
    MD5((const unsigned char*)(JAVA_ARRAY_BYTE*)arr->data, (size_t)arr->length, digest);
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
            (const void*)(JAVA_ARRAY_BYTE*)keyArr->data, (int)keyArr->length,
            (const unsigned char*)(JAVA_ARRAY_BYTE*)dataArr->data, (size_t)dataArr->length,
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
    if(PKCS5_PBKDF2_HMAC((const char*)(JAVA_ARRAY_BYTE*)pw->data, (int)pw->length,
                         (const unsigned char*)(JAVA_ARRAY_BYTE*)sl->data, (int)sl->length,
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
    return CRYPTO_memcmp((const void*)(JAVA_ARRAY_BYTE*)aa->data,
                         (const void*)(JAVA_ARRAY_BYTE*)bb->data,
                         (size_t)aa->length) == 0 ? JAVA_TRUE : JAVA_FALSE;
}
