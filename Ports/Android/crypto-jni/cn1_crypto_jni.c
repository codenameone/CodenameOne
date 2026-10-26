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
 * JNI entry points for com.codename1.security.NativeCryptoKernels on Android.
 *
 * The kernels are cn1_crypto_kernels.c from the ParparVM runtime, compiled unchanged into this
 * translation unit against the shim cn1_globals.h beside this file; build.sh copies both next to
 * each other so the kernel file's own #include resolves here. Every entry point below pins its
 * arrays, hands them to the ParparVM-named function as {data, length} pairs and releases them.
 *
 * Pinning uses GetPrimitiveArrayCritical: the inputs are at most a message long and the kernels
 * never call back into Java, which is the contract a critical region needs. JNI forbids every
 * other JNI call while one is held -- GetArrayLength included -- so all lengths are read first.
 */
#include <jni.h>
#include "cn1_crypto_kernels.c"

#define CN1J_MAX 4

typedef struct {
    int n;
    jbyteArray java[CN1J_MAX];
    struct { void *data; int length; } pinned[CN1J_MAX];
} cn1j_pins;

static void cn1j_release(JNIEnv *env, cn1j_pins *p, int count) {
    int i;
    for (i = count - 1; i >= 0; i--) {
        if (p->java[i] != NULL && p->pinned[i].data != NULL) {
            (*env)->ReleasePrimitiveArrayCritical(env, p->java[i], p->pinned[i].data, 0);
        }
    }
}

/*
 * 1 when every non-null array is pinned. On failure nothing is left pinned and an OutOfMemoryError
 * is pending: the void kernels write their answer into an output array, so returning quietly would
 * hand the caller a zero-filled buffer that reads as a public key or a signature.
 */
static int cn1j_pin(JNIEnv *env, cn1j_pins *p) {
    int i;
    for (i = 0; i < p->n; i++) {
        p->pinned[i].data = NULL;
        p->pinned[i].length = p->java[i] == NULL ? 0 : (int) (*env)->GetArrayLength(env, p->java[i]);
    }
    for (i = 0; i < p->n; i++) {
        if (p->java[i] != NULL) {
            p->pinned[i].data = (*env)->GetPrimitiveArrayCritical(env, p->java[i], NULL);
            if (p->pinned[i].data == NULL) {
                cn1j_release(env, p, i);
                if (!(*env)->ExceptionCheck(env)) {
                    jclass oom = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
                    if (oom != NULL) {
                        (*env)->ThrowNew(env, oom, "could not pin an array for the crypto kernels");
                    }
                }
                return 0;
            }
        }
    }
    return 1;
}

#define ARG(i) (pins.java[i] == NULL ? JAVA_NULL : (JAVA_OBJECT) &pins.pinned[i])
#define PIN1(a) cn1j_pins pins; pins.n = 1; pins.java[0] = a
#define PIN2(a, b) cn1j_pins pins; pins.n = 2; pins.java[0] = a; pins.java[1] = b
#define PIN3(a, b, c) cn1j_pins pins; pins.n = 3; pins.java[0] = a; pins.java[1] = b; pins.java[2] = c
#define PIN4(a, b, c, d) cn1j_pins pins; pins.n = 4; pins.java[0] = a; pins.java[1] = b; \
        pins.java[2] = c; pins.java[3] = d
#define NS(name) Java_com_codename1_security_NativeCryptoKernels_##name

JNIEXPORT jboolean JNICALL NS(available)(JNIEnv *env, jclass cls) {
    return com_codename1_security_NativeCryptoKernels_available___R_boolean(NULL) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL NS(secpPublicKey)(JNIEnv *env, jclass cls, jbyteArray priv, jbyteArray out) {
    JAVA_BOOLEAN r;
    PIN2(priv, out);
    if (!cn1j_pin(env, &pins)) {
        return JNI_FALSE;
    }
    r = com_codename1_security_NativeCryptoKernels_secpPublicKey___byte_1ARRAY_byte_1ARRAY_R_boolean(
            NULL, ARG(0), ARG(1));
    cn1j_release(env, &pins, pins.n);
    return r ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL NS(secpSign)(JNIEnv *env, jclass cls, jbyteArray hash, jbyteArray priv,
        jbyteArray out) {
    JAVA_BOOLEAN r;
    PIN3(hash, priv, out);
    if (!cn1j_pin(env, &pins)) {
        return JNI_FALSE;
    }
    r = com_codename1_security_NativeCryptoKernels_secpSign___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_R_boolean(
            NULL, ARG(0), ARG(1), ARG(2));
    cn1j_release(env, &pins, pins.n);
    return r ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL NS(secpVerify)(JNIEnv *env, jclass cls, jbyteArray hash, jbyteArray sig,
        jbyteArray pub) {
    JAVA_INT r;
    PIN3(hash, sig, pub);
    if (!cn1j_pin(env, &pins)) {
        return -1;
    }
    r = com_codename1_security_NativeCryptoKernels_secpVerify___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_R_int(
            NULL, ARG(0), ARG(1), ARG(2));
    cn1j_release(env, &pins, pins.n);
    return r;
}

JNIEXPORT jboolean JNICALL NS(secpRecover)(JNIEnv *env, jclass cls, jbyteArray hash, jbyteArray sig,
        jint recId, jbyteArray out) {
    JAVA_BOOLEAN r;
    PIN3(hash, sig, out);
    if (!cn1j_pin(env, &pins)) {
        return JNI_FALSE;
    }
    r = com_codename1_security_NativeCryptoKernels_secpRecover___byte_1ARRAY_byte_1ARRAY_int_byte_1ARRAY_R_boolean(
            NULL, ARG(0), ARG(1), (JAVA_INT) recId, ARG(2));
    cn1j_release(env, &pins, pins.n);
    return r ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL NS(secpPrivateTweakAdd)(JNIEnv *env, jclass cls, jbyteArray priv,
        jbyteArray tweak, jbyteArray out) {
    JAVA_BOOLEAN r;
    PIN3(priv, tweak, out);
    if (!cn1j_pin(env, &pins)) {
        return JNI_FALSE;
    }
    r = com_codename1_security_NativeCryptoKernels_secpPrivateTweakAdd___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_R_boolean(
            NULL, ARG(0), ARG(1), ARG(2));
    cn1j_release(env, &pins, pins.n);
    return r ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL NS(secpPublicTweakAdd)(JNIEnv *env, jclass cls, jbyteArray pub,
        jbyteArray tweak, jbyteArray out) {
    JAVA_BOOLEAN r;
    PIN3(pub, tweak, out);
    if (!cn1j_pin(env, &pins)) {
        return JNI_FALSE;
    }
    r = com_codename1_security_NativeCryptoKernels_secpPublicTweakAdd___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_R_boolean(
            NULL, ARG(0), ARG(1), ARG(2));
    cn1j_release(env, &pins, pins.n);
    return r ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL NS(xOnlyPublicKey)(JNIEnv *env, jclass cls, jbyteArray priv, jbyteArray out) {
    JAVA_BOOLEAN r;
    PIN2(priv, out);
    if (!cn1j_pin(env, &pins)) {
        return JNI_FALSE;
    }
    r = com_codename1_security_NativeCryptoKernels_xOnlyPublicKey___byte_1ARRAY_byte_1ARRAY_R_boolean(
            NULL, ARG(0), ARG(1));
    cn1j_release(env, &pins, pins.n);
    return r ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL NS(schnorrSign)(JNIEnv *env, jclass cls, jbyteArray msg, jbyteArray priv,
        jbyteArray aux, jbyteArray out) {
    JAVA_BOOLEAN r;
    PIN4(msg, priv, aux, out);
    if (!cn1j_pin(env, &pins)) {
        return JNI_FALSE;
    }
    r = com_codename1_security_NativeCryptoKernels_schnorrSign___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_R_boolean(
            NULL, ARG(0), ARG(1), ARG(2), ARG(3));
    cn1j_release(env, &pins, pins.n);
    return r ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL NS(schnorrVerify)(JNIEnv *env, jclass cls, jbyteArray msg, jbyteArray sig,
        jbyteArray xonly) {
    JAVA_INT r;
    PIN3(msg, sig, xonly);
    if (!cn1j_pin(env, &pins)) {
        return -1;
    }
    r = com_codename1_security_NativeCryptoKernels_schnorrVerify___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_R_int(
            NULL, ARG(0), ARG(1), ARG(2));
    cn1j_release(env, &pins, pins.n);
    return r;
}

JNIEXPORT void JNICALL NS(ed25519PublicKey)(JNIEnv *env, jclass cls, jbyteArray seed, jbyteArray out) {
    PIN2(seed, out);
    if (!cn1j_pin(env, &pins)) {
        return;
    }
    com_codename1_security_NativeCryptoKernels_ed25519PublicKey___byte_1ARRAY_byte_1ARRAY(NULL, ARG(0), ARG(1));
    cn1j_release(env, &pins, pins.n);
}

JNIEXPORT void JNICALL NS(ed25519Sign)(JNIEnv *env, jclass cls, jbyteArray seed, jbyteArray pub,
        jbyteArray msg, jbyteArray out) {
    PIN4(seed, pub, msg, out);
    if (!cn1j_pin(env, &pins)) {
        return;
    }
    com_codename1_security_NativeCryptoKernels_ed25519Sign___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY(
            NULL, ARG(0), ARG(1), ARG(2), ARG(3));
    cn1j_release(env, &pins, pins.n);
}

JNIEXPORT jint JNICALL NS(ed25519Verify)(JNIEnv *env, jclass cls, jbyteArray msg, jbyteArray sig,
        jbyteArray pub) {
    JAVA_INT r;
    PIN3(msg, sig, pub);
    if (!cn1j_pin(env, &pins)) {
        return -1;
    }
    r = com_codename1_security_NativeCryptoKernels_ed25519Verify___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_R_int(
            NULL, ARG(0), ARG(1), ARG(2));
    cn1j_release(env, &pins, pins.n);
    return r;
}

JNIEXPORT void JNICALL NS(keccak256)(JNIEnv *env, jclass cls, jbyteArray data, jint off, jint len,
        jbyteArray out) {
    PIN2(data, out);
    if (!cn1j_pin(env, &pins)) {
        return;
    }
    com_codename1_security_NativeCryptoKernels_keccak256___byte_1ARRAY_int_int_byte_1ARRAY(
            NULL, ARG(0), (JAVA_INT) off, (JAVA_INT) len, ARG(1));
    cn1j_release(env, &pins, pins.n);
}

JNIEXPORT void JNICALL NS(sha256)(JNIEnv *env, jclass cls, jbyteArray data, jint off, jint len,
        jbyteArray out) {
    PIN2(data, out);
    if (!cn1j_pin(env, &pins)) {
        return;
    }
    com_codename1_security_NativeCryptoKernels_sha256___byte_1ARRAY_int_int_byte_1ARRAY(
            NULL, ARG(0), (JAVA_INT) off, (JAVA_INT) len, ARG(1));
    cn1j_release(env, &pins, pins.n);
}

JNIEXPORT void JNICALL NS(sha512)(JNIEnv *env, jclass cls, jbyteArray data, jint off, jint len,
        jbyteArray out) {
    PIN2(data, out);
    if (!cn1j_pin(env, &pins)) {
        return;
    }
    com_codename1_security_NativeCryptoKernels_sha512___byte_1ARRAY_int_int_byte_1ARRAY(
            NULL, ARG(0), (JAVA_INT) off, (JAVA_INT) len, ARG(1));
    cn1j_release(env, &pins, pins.n);
}

JNIEXPORT void JNICALL NS(ripemd160)(JNIEnv *env, jclass cls, jbyteArray data, jint off, jint len,
        jbyteArray out) {
    PIN2(data, out);
    if (!cn1j_pin(env, &pins)) {
        return;
    }
    com_codename1_security_NativeCryptoKernels_ripemd160___byte_1ARRAY_int_int_byte_1ARRAY(
            NULL, ARG(0), (JAVA_INT) off, (JAVA_INT) len, ARG(1));
    cn1j_release(env, &pins, pins.n);
}

JNIEXPORT void JNICALL NS(hmac)(JNIEnv *env, jclass cls, jint bits, jbyteArray key, jbyteArray data, jint off,
        jint len, jbyteArray out) {
    PIN3(key, data, out);
    if (!cn1j_pin(env, &pins)) {
        return;
    }
    com_codename1_security_NativeCryptoKernels_hmac___int_byte_1ARRAY_byte_1ARRAY_int_int_byte_1ARRAY(
            NULL, (JAVA_INT) bits, ARG(0), ARG(1), (JAVA_INT) off, (JAVA_INT) len, ARG(2));
    cn1j_release(env, &pins, pins.n);
}

/*
 * The whole derivation runs with the three arrays pinned: a BIP-39 seed is about a third of a
 * millisecond, short enough to hold a critical section, and copying would leave the password in
 * a native buffer as well.
 */
JNIEXPORT void JNICALL NS(pbkdf2)(JNIEnv *env, jclass cls, jint bits, jbyteArray password, jbyteArray salt,
        jint iterations, jbyteArray out) {
    PIN3(password, salt, out);
    if (!cn1j_pin(env, &pins)) {
        return;
    }
    com_codename1_security_NativeCryptoKernels_pbkdf2___int_byte_1ARRAY_byte_1ARRAY_int_byte_1ARRAY(
            NULL, (JAVA_INT) bits, ARG(0), ARG(1), (JAVA_INT) iterations, ARG(2));
    cn1j_release(env, &pins, pins.n);
}

/*
 * A batch can run for many milliseconds on worker threads, too long for a critical region, so its
 * arrays are taken with Get<Type>ArrayElements instead, which leaves the collector free. The
 * inputs are released without copying back; only the results are written to the Java array.
 */
typedef struct { void *data; int length; } cn1j_arr;

JNIEXPORT void JNICALL NS(batch)(JNIEnv *env, jclass cls, jint alg, jbyteArray msgs, jintArray msgOff,
        jbyteArray sigs, jbyteArray keys, jintArray keyOff, jbyteArray results, jint count) {
    cn1j_arr a[6];
    jarray java[6];
    int kinds[6] = { 0, 1, 0, 0, 1, 0 };   /* 1 for an int array */
    int i, ok = 1;
    java[0] = msgs; java[1] = msgOff; java[2] = sigs; java[3] = keys; java[4] = keyOff; java[5] = results;
    for (i = 0; i < 6; i++) {
        a[i].data = NULL;
        a[i].length = 0;
    }
    for (i = 0; i < 6; i++) {
        if (java[i] == NULL) {
            continue;
        }
        a[i].length = (int) (*env)->GetArrayLength(env, java[i]);
        a[i].data = kinds[i] ? (void*) (*env)->GetIntArrayElements(env, (jintArray) java[i], NULL)
                             : (void*) (*env)->GetByteArrayElements(env, (jbyteArray) java[i], NULL);
        if (a[i].data == NULL) {
            ok = 0;
            break;
        }
    }
    if (ok) {
        com_codename1_security_NativeCryptoKernels_batch___int_byte_1ARRAY_int_1ARRAY_byte_1ARRAY_byte_1ARRAY_int_1ARRAY_byte_1ARRAY_int(
                NULL, (JAVA_INT) alg, &a[0], &a[1], java[2] == NULL ? JAVA_NULL : &a[2],
                java[3] == NULL ? JAVA_NULL : &a[3], java[4] == NULL ? JAVA_NULL : &a[4], &a[5], (JAVA_INT) count);
    }
    for (i = 5; i >= 0; i--) {
        if (a[i].data == NULL) {
            continue;
        }
        if (kinds[i]) {
            (*env)->ReleaseIntArrayElements(env, (jintArray) java[i], (jint*) a[i].data, JNI_ABORT);
        } else {
            (*env)->ReleaseByteArrayElements(env, (jbyteArray) java[i], (jbyte*) a[i].data, i == 5 ? 0 : JNI_ABORT);
        }
    }
    if (!ok && !(*env)->ExceptionCheck(env)) {
        jclass oom = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
        if (oom != NULL) {
            (*env)->ThrowNew(env, oom, "could not reach an array for the crypto kernels");
        }
    }
}
