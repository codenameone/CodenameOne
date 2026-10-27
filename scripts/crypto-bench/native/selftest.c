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

/* Known-answer test of the hash kernels with nothing but a C compiler: no
 * libsecp256k1, no OpenSSL. It exists so that every compiler a translated
 * project may meet (GCC, old clang, MSVC) can be checked on the hardware path
 * it selects, which the bench cannot do on a machine without the references.
 *
 *   cc -O2 -I scripts/crypto-bench/native -I vm/ByteCodeTranslator/src \
 *      scripts/crypto-bench/native/selftest.c -o selftest && ./selftest
 */
#include "cn1_globals.h"
#include "cn1_crypto_kernels.c"
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

static int failures = 0;

static void check(const char *name, const unsigned char *got, size_t len, const char *hex) {
    char buf[260];
    size_t i;
    for (i = 0; i < len; i++) {
        sprintf(buf + 2 * i, "%02x", got[i]);
    }
    if (strcmp(buf, hex) != 0) {
        printf("FAIL %s\n  got  %s\n  want %s\n", name, buf, hex);
        failures++;
    } else {
        printf("ok   %s\n", name);
    }
}

int main(void) {
    unsigned char out[64];
    unsigned char *million = (unsigned char *)malloc(1000000);
    memset(million, 'a', 1000000);

    cn1k_sha256((const unsigned char *)"abc", 3, out);
    check("sha256 abc", out, 32, "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    cn1k_sha256(million, 1000000, out);
    check("sha256 1M a", out, 32, "cdc76e5c9914fb9281a1c7e284d73e67f1809a48a497200e046d39ccc7112cd0");
    cn1k_sha512((const unsigned char *)"abc", 3, out);
    check("sha512 abc", out, 64, "ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a"
                                 "2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f");
    cn1k_sha512(million, 1000000, out);
    check("sha512 1M a", out, 64, "e718483d0ce769644e2e42c7bc15b4638e1f98b13b2044285632a803afa973eb"
                                  "de0ff244877ea60a4cb0432ce577c31beb009c5c2c49aa2e4eadb217ad8cc09b");
    cn1k_ripemd160((const unsigned char *)"abc", 3, out);
    check("ripemd160 abc", out, 20, "8eb208f7e05d987a9b044a8e98c6b087f15a0bfc");
    cn1k_keccak256((const unsigned char *)"", 0, out);
    check("keccak256 empty", out, 32, "c5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470");
    cn1k_hmac_sha256((const unsigned char *)"Jefe", 4, (const unsigned char *)"what do ya want for nothing?", 28, out);
    check("hmac-sha256 rfc4231/2", out, 32, "5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843");
    cn1k_hmac_sha512((const unsigned char *)"Jefe", 4, (const unsigned char *)"what do ya want for nothing?", 28, out);
    check("hmac-sha512 rfc4231/2", out, 64, "164b7a7bfcf819e2e395fbe73b56e0a387bd64222e831fd610270cd7ea250554"
                                            "9758bf75c05a994a6d034f65f8f0e6fdcaeab1a34d4a6b4b636e070a38bce737");
    cn1k_pbkdf2_sha512((const unsigned char *)"password", 8, (const unsigned char *)"salt", 4, 1, out, 64);
    check("pbkdf2-sha512 c=1", out, 64, "867f70cf1ade02cff3752599a3a53dc4af34c7a669815ae5d513554e1c8cf252"
                                        "c02d470a285a0501bad999bfe943c08f050235d7d68b1da55e63f73b60a57fce");
    free(million);
    printf("%s (%d failing)\n", failures ? "FAILED" : "PASSED", failures);
    return failures ? 1 : 0;
}
