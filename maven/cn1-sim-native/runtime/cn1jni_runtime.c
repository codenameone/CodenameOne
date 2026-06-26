/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
 * cn1jni runtime implementation - see cn1jni_runtime.h for the design.
 * The Objective-C parts (fromNSString/toNSString) live in cn1jni_objc.m.
 */
#include "cn1jni_runtime.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

/* ---- per-thread state ---------------------------------------------------- */

/*
 * One block tracking a malloc'd buffer (UTF-8 conversions etc.) owned by the
 * current arena scope.
 */
typedef struct cn1jni_alloc {
    struct cn1jni_alloc *next;
    /* buffer follows the header */
} cn1jni_alloc;

/*
 * ts MUST be the first member: shims hand us a struct ThreadLocalData* and we
 * recover the enclosing tls record by casting.
 */
typedef struct cn1jni_tls {
    struct ThreadLocalData ts;
    JNIEnv *env;
    cn1jni_object *arenaHead;
    cn1jni_alloc *allocHead;
    jthrowable pendingThrow;
    int depth;
    int pushedFrame;
    int attachedHere;
} cn1jni_tls;

static JavaVM *cn1jni_vm;

/*
 * One record per attached thread. C11 thread-local pointer with lazy
 * allocation - no exit destructor: the simulator's threads (EDT, main,
 * network/media callbacks) are few and long-lived, so the records simply
 * live for the process lifetime.
 */
static _Thread_local cn1jni_tls *cn1jni_tls_slot;

static cn1jni_tls *cn1jni_get_tls(void) {
    cn1jni_tls *tls = cn1jni_tls_slot;
    if (tls == NULL) {
        tls = (cn1jni_tls *) calloc(1, sizeof(cn1jni_tls));
        /* a stable per-thread id: the record's own address */
        tls->ts.threadId = (JAVA_LONG) (intptr_t) tls;
        cn1jni_tls_slot = tls;
    }
    return tls;
}

static cn1jni_tls *tls_of(struct ThreadLocalData *ts) {
    return (cn1jni_tls *) ts;
}

JNIEnv *cn1jni_env(struct ThreadLocalData *ts) {
    return tls_of(ts)->env;
}

/* ---- arena --------------------------------------------------------------- */

static void cn1jni_track(cn1jni_tls *tls, cn1jni_object *w) {
    w->arenaNext = tls->arenaHead;
    tls->arenaHead = w;
}

/* implemented in cn1jni_objc.m - drops the cached NSString */
extern void cn1jni_release_nscache(cn1jni_object *w);

static void cn1jni_release_elements(JNIEnv *env, cn1jni_object *w, int copyBack) {
    if (w->kind != CN1JNI_KIND_ARRAY_PRIM || w->proto.data == NULL) {
        return;
    }
    int mode = copyBack ? 0 : JNI_ABORT;
    switch (w->elemType) {
        case 'Z':
            (*env)->ReleaseBooleanArrayElements(env, (jbooleanArray) w->jref, (jboolean *) w->proto.data, mode);
            break;
        case 'B':
            (*env)->ReleaseByteArrayElements(env, (jbyteArray) w->jref, (jbyte *) w->proto.data, mode);
            break;
        case 'C':
            (*env)->ReleaseCharArrayElements(env, (jcharArray) w->jref, (jchar *) w->proto.data, mode);
            break;
        case 'S':
            (*env)->ReleaseShortArrayElements(env, (jshortArray) w->jref, (jshort *) w->proto.data, mode);
            break;
        case 'I':
            (*env)->ReleaseIntArrayElements(env, (jintArray) w->jref, (jint *) w->proto.data, mode);
            break;
        case 'J':
            (*env)->ReleaseLongArrayElements(env, (jlongArray) w->jref, (jlong *) w->proto.data, mode);
            break;
        case 'F':
            (*env)->ReleaseFloatArrayElements(env, (jfloatArray) w->jref, (jfloat *) w->proto.data, mode);
            break;
        case 'D':
            (*env)->ReleaseDoubleArrayElements(env, (jdoubleArray) w->jref, (jdouble *) w->proto.data, mode);
            break;
        default:
            break;
    }
    w->proto.data = NULL;
}

void cn1jni_sync_array(struct ThreadLocalData *ts, JAVA_OBJECT o) {
    if (o == JAVA_NULL) {
        return;
    }
    cn1jni_tls *tls = tls_of(ts);
    cn1jni_object *w = (cn1jni_object *) o;
    if (w->kind != CN1JNI_KIND_ARRAY_PRIM || w->proto.data == NULL) {
        return;
    }
    JNIEnv *env = tls->env;
    /* JNI_COMMIT copies back without releasing the buffer */
    switch (w->elemType) {
        case 'Z':
            (*env)->ReleaseBooleanArrayElements(env, (jbooleanArray) w->jref, (jboolean *) w->proto.data, JNI_COMMIT);
            break;
        case 'B':
            (*env)->ReleaseByteArrayElements(env, (jbyteArray) w->jref, (jbyte *) w->proto.data, JNI_COMMIT);
            break;
        case 'C':
            (*env)->ReleaseCharArrayElements(env, (jcharArray) w->jref, (jchar *) w->proto.data, JNI_COMMIT);
            break;
        case 'S':
            (*env)->ReleaseShortArrayElements(env, (jshortArray) w->jref, (jshort *) w->proto.data, JNI_COMMIT);
            break;
        case 'I':
            (*env)->ReleaseIntArrayElements(env, (jintArray) w->jref, (jint *) w->proto.data, JNI_COMMIT);
            break;
        case 'J':
            (*env)->ReleaseLongArrayElements(env, (jlongArray) w->jref, (jlong *) w->proto.data, JNI_COMMIT);
            break;
        case 'F':
            (*env)->ReleaseFloatArrayElements(env, (jfloatArray) w->jref, (jfloat *) w->proto.data, JNI_COMMIT);
            break;
        case 'D':
            (*env)->ReleaseDoubleArrayElements(env, (jdoubleArray) w->jref, (jdouble *) w->proto.data, JNI_COMMIT);
            break;
        default:
            break;
    }
}

static void cn1jni_free_wrapper(JNIEnv *env, cn1jni_object *w) {
    cn1jni_release_elements(env, w, 1);
    cn1jni_release_nscache(w);
    if (w->jref != NULL) {
        if (w->pinned) {
            (*env)->DeleteGlobalRef(env, w->jref);
        }
        /*
         * Local refs are deliberately NOT deleted here: a shim may be
         * returning this very reference to the JVM (return values unwrap to
         * the wrapped jref). The JNI frame teardown reclaims downcall locals
         * automatically and PopLocalFrame reclaims upcall locals.
         */
        w->jref = NULL;
    }
    free(w);
}

static void cn1jni_arena_release(cn1jni_tls *tls) {
    JNIEnv *env = tls->env;
    cn1jni_object *w = tls->arenaHead;
    tls->arenaHead = NULL;
    while (w != NULL) {
        cn1jni_object *next = w->arenaNext;
        if (!w->pinned) {
            cn1jni_free_wrapper(env, w);
        }
        w = next;
    }
    cn1jni_alloc *a = tls->allocHead;
    tls->allocHead = NULL;
    while (a != NULL) {
        cn1jni_alloc *next = a->next;
        free(a);
        a = next;
    }
}

void *cn1jni_arena_alloc(struct ThreadLocalData *ts, size_t size) {
    cn1jni_tls *tls = tls_of(ts);
    cn1jni_alloc *a = (cn1jni_alloc *) malloc(sizeof(cn1jni_alloc) + size);
    a->next = tls->allocHead;
    tls->allocHead = a;
    return (void *) (a + 1);
}

/* ---- enter/exit ---------------------------------------------------------- */

struct ThreadLocalData *cn1jni_enter(JNIEnv *env) {
    cn1jni_tls *tls = cn1jni_get_tls();
    tls->env = env;
    tls->depth++;
    return &tls->ts;
}

void cn1jni_exit(struct ThreadLocalData *ts) {
    cn1jni_tls *tls = tls_of(ts);
    tls->depth--;
    if (tls->depth > 0) {
        return;
    }
    tls->depth = 0;
    JNIEnv *env = tls->env;
    jthrowable pending = tls->pendingThrow;
    tls->pendingThrow = NULL;
    tls->ts.exception = JAVA_NULL;
    cn1jni_arena_release(tls);
    if (pending != NULL && env != NULL) {
        (*env)->Throw(env, pending);
        (*env)->DeleteLocalRef(env, pending);
    }
}

struct ThreadLocalData *cn1jni_upcall_enter(void) {
    cn1jni_tls *tls = cn1jni_get_tls();
    if (tls->env == NULL) {
        JNIEnv *env = NULL;
        if ((*cn1jni_vm)->GetEnv(cn1jni_vm, (void **) &env, JNI_VERSION_1_6) != JNI_OK) {
            (*cn1jni_vm)->AttachCurrentThreadAsDaemon(cn1jni_vm, (void **) &env, NULL);
            tls->attachedHere = 1;
        }
        tls->env = env;
    }
    if (tls->depth == 0) {
        if ((*tls->env)->PushLocalFrame(tls->env, 64) == 0) {
            tls->pushedFrame = 1;
        }
    }
    tls->depth++;
    return &tls->ts;
}

void cn1jni_upcall_exit(struct ThreadLocalData *ts) {
    cn1jni_tls *tls = tls_of(ts);
    tls->depth--;
    if (tls->depth > 0) {
        return;
    }
    tls->depth = 0;
    JNIEnv *env = tls->env;
    tls->pendingThrow = NULL;
    tls->ts.exception = JAVA_NULL;
    cn1jni_arena_release(tls);
    /* upcalls cannot leave exceptions pending on a native thread */
    if (env != NULL && (*env)->ExceptionCheck(env)) {
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
    }
    if (tls->pushedFrame) {
        tls->pushedFrame = 0;
        (*env)->PopLocalFrame(env, NULL);
    }
}

struct ThreadLocalData *getThreadLocalData(void) {
    cn1jni_tls *tls = cn1jni_get_tls();
    if (tls->env == NULL && cn1jni_vm != NULL) {
        JNIEnv *env = NULL;
        if ((*cn1jni_vm)->GetEnv(cn1jni_vm, (void **) &env, JNI_VERSION_1_6) != JNI_OK) {
            (*cn1jni_vm)->AttachCurrentThreadAsDaemon(cn1jni_vm, (void **) &env, NULL);
            tls->attachedHere = 1;
        }
        tls->env = env;
    }
    return &tls->ts;
}

/* ---- wrapper creation ---------------------------------------------------- */

static cn1jni_object *cn1jni_new_wrapper(cn1jni_tls *tls, jobject ref, int kind) {
    cn1jni_object *w = (cn1jni_object *) calloc(1, sizeof(cn1jni_object));
    w->jref = ref;
    w->kind = kind;
    cn1jni_track(tls, w);
    return w;
}

JAVA_OBJECT cn1jni_wrap_object(struct ThreadLocalData *ts, jobject o) {
    if (o == NULL) {
        return JAVA_NULL;
    }
    return (JAVA_OBJECT) cn1jni_new_wrapper(tls_of(ts), o, CN1JNI_KIND_OBJECT);
}

JAVA_OBJECT cn1jni_wrap_string(struct ThreadLocalData *ts, jstring s) {
    if (s == NULL) {
        return JAVA_NULL;
    }
    return (JAVA_OBJECT) cn1jni_new_wrapper(tls_of(ts), s, CN1JNI_KIND_STRING);
}

#define CN1JNI_DEFINE_WRAP_ARRAY(NAME, JARRAY, JTYPE, SIG, GETTER, PRIMSIZE) \
JAVA_OBJECT cn1jni_wrap_array_##NAME(struct ThreadLocalData *ts, JARRAY a) { \
    if (a == NULL) { \
        return JAVA_NULL; \
    } \
    cn1jni_tls *tls = tls_of(ts); \
    JNIEnv *env = tls->env; \
    cn1jni_object *w = cn1jni_new_wrapper(tls, a, CN1JNI_KIND_ARRAY_PRIM); \
    w->elemType = SIG; \
    w->proto.length = (*env)->GetArrayLength(env, a); \
    w->proto.dimensions = 1; \
    w->proto.primitiveSize = PRIMSIZE; \
    w->proto.data = (*env)->GETTER(env, a, NULL); \
    return (JAVA_OBJECT) w; \
}

CN1JNI_DEFINE_WRAP_ARRAY(boolean, jbooleanArray, jboolean, 'Z', GetBooleanArrayElements, 1)
CN1JNI_DEFINE_WRAP_ARRAY(byte, jbyteArray, jbyte, 'B', GetByteArrayElements, 1)
CN1JNI_DEFINE_WRAP_ARRAY(char, jcharArray, jchar, 'C', GetCharArrayElements, 2)
CN1JNI_DEFINE_WRAP_ARRAY(short, jshortArray, jshort, 'S', GetShortArrayElements, 2)
CN1JNI_DEFINE_WRAP_ARRAY(int, jintArray, jint, 'I', GetIntArrayElements, 4)
CN1JNI_DEFINE_WRAP_ARRAY(long, jlongArray, jlong, 'J', GetLongArrayElements, 8)
CN1JNI_DEFINE_WRAP_ARRAY(float, jfloatArray, jfloat, 'F', GetFloatArrayElements, 4)
CN1JNI_DEFINE_WRAP_ARRAY(double, jdoubleArray, jdouble, 'D', GetDoubleArrayElements, 8)

JAVA_OBJECT cn1jni_wrap_array_object(struct ThreadLocalData *ts, jobjectArray a) {
    if (a == NULL) {
        return JAVA_NULL;
    }
    cn1jni_tls *tls = tls_of(ts);
    JNIEnv *env = tls->env;
    cn1jni_object *w = cn1jni_new_wrapper(tls, a, CN1JNI_KIND_ARRAY_OBJECT);
    w->elemType = 'L';
    w->proto.length = (*env)->GetArrayLength(env, a);
    w->proto.dimensions = 1;
    /*
     * Materialize the element wrappers eagerly: port code iterates
     * ((JAVA_ARRAY_OBJECT*)((JAVA_ARRAY)x)->data)[i]. The elements are
     * wrapped as strings when they are strings (the dominant case in the
     * port sources: String[] parameters).
     */
    JAVA_OBJECT *data = (JAVA_OBJECT *) cn1jni_arena_alloc(ts, sizeof(JAVA_OBJECT) * (w->proto.length > 0 ? w->proto.length : 1));
    jclass stringCls = (*env)->FindClass(env, "java/lang/String");
    for (int i = 0; i < w->proto.length; i++) {
        jobject el = (*env)->GetObjectArrayElement(env, a, i);
        if (el == NULL) {
            data[i] = JAVA_NULL;
        } else if ((*env)->IsInstanceOf(env, el, stringCls)) {
            data[i] = cn1jni_wrap_string(ts, (jstring) el);
        } else {
            data[i] = cn1jni_wrap_object(ts, el);
        }
    }
    w->proto.data = data;
    return (JAVA_OBJECT) w;
}

/* ---- unwrapping ---------------------------------------------------------- */

jobject cn1jni_unwrap_object(struct ThreadLocalData *ts, JAVA_OBJECT o) {
    if (o == JAVA_NULL) {
        return NULL;
    }
    return ((cn1jni_object *) o)->jref;
}

jstring cn1jni_unwrap_string(struct ThreadLocalData *ts, JAVA_OBJECT o) {
    return (jstring) cn1jni_unwrap_object(ts, o);
}

#define CN1JNI_DEFINE_UNWRAP_ARRAY(NAME, JARRAY) \
JARRAY cn1jni_unwrap_array_##NAME(struct ThreadLocalData *ts, JAVA_OBJECT o) { \
    if (o == JAVA_NULL) { \
        return NULL; \
    } \
    cn1jni_sync_array(ts, o); \
    return (JARRAY) ((cn1jni_object *) o)->jref; \
}

CN1JNI_DEFINE_UNWRAP_ARRAY(boolean, jbooleanArray)
CN1JNI_DEFINE_UNWRAP_ARRAY(byte, jbyteArray)
CN1JNI_DEFINE_UNWRAP_ARRAY(char, jcharArray)
CN1JNI_DEFINE_UNWRAP_ARRAY(short, jshortArray)
CN1JNI_DEFINE_UNWRAP_ARRAY(int, jintArray)
CN1JNI_DEFINE_UNWRAP_ARRAY(long, jlongArray)
CN1JNI_DEFINE_UNWRAP_ARRAY(float, jfloatArray)
CN1JNI_DEFINE_UNWRAP_ARRAY(double, jdoubleArray)

jobjectArray cn1jni_unwrap_array_object(struct ThreadLocalData *ts, JAVA_OBJECT o) {
    if (o == JAVA_NULL) {
        return NULL;
    }
    return (jobjectArray) ((cn1jni_object *) o)->jref;
}

/* ---- pinning ------------------------------------------------------------- */

JAVA_OBJECT cn1jni_pin(struct ThreadLocalData *ts, JAVA_OBJECT o) {
    if (o == JAVA_NULL) {
        return o;
    }
    cn1jni_tls *tls = tls_of(ts);
    cn1jni_object *w = (cn1jni_object *) o;
    if (!w->pinned) {
        JNIEnv *env = tls->env;
        jobject g = (*env)->NewGlobalRef(env, w->jref);
        (*env)->DeleteLocalRef(env, w->jref);
        w->jref = g;
        w->pinned = 1;
        /* unlink from the arena so arena release does not free it */
        cn1jni_object **p = &tls->arenaHead;
        while (*p != NULL) {
            if (*p == w) {
                *p = w->arenaNext;
                break;
            }
            p = &(*p)->arenaNext;
        }
        w->arenaNext = NULL;
    }
    return o;
}

void cn1jni_unpin(struct ThreadLocalData *ts, JAVA_OBJECT o) {
    if (o == JAVA_NULL) {
        return;
    }
    cn1jni_object *w = (cn1jni_object *) o;
    if (w->pinned) {
        cn1jni_free_wrapper(tls_of(ts)->env, w);
    }
}

/* ---- ParparVM runtime API ------------------------------------------------ */

#define CN1JNI_DEFINE_NEW_ARRAY(MACRO_NAME, NAME, JARRAY, NEWFN) \
JAVA_OBJECT MACRO_NAME(CODENAME_ONE_THREAD_STATE, JAVA_INT size) { \
    cn1jni_tls *tls = tls_of(threadStateData); \
    JNIEnv *env = tls->env; \
    JARRAY arr = (*env)->NEWFN(env, size); \
    return cn1jni_wrap_array_##NAME(threadStateData, arr); \
}

CN1JNI_DEFINE_NEW_ARRAY(__NEW_ARRAY_JAVA_BOOLEAN, boolean, jbooleanArray, NewBooleanArray)
CN1JNI_DEFINE_NEW_ARRAY(__NEW_ARRAY_JAVA_CHAR, char, jcharArray, NewCharArray)
CN1JNI_DEFINE_NEW_ARRAY(__NEW_ARRAY_JAVA_BYTE, byte, jbyteArray, NewByteArray)
CN1JNI_DEFINE_NEW_ARRAY(__NEW_ARRAY_JAVA_SHORT, short, jshortArray, NewShortArray)
CN1JNI_DEFINE_NEW_ARRAY(__NEW_ARRAY_JAVA_INT, int, jintArray, NewIntArray)
CN1JNI_DEFINE_NEW_ARRAY(__NEW_ARRAY_JAVA_LONG, long, jlongArray, NewLongArray)
CN1JNI_DEFINE_NEW_ARRAY(__NEW_ARRAY_JAVA_FLOAT, float, jfloatArray, NewFloatArray)
CN1JNI_DEFINE_NEW_ARRAY(__NEW_ARRAY_JAVA_DOUBLE, double, jdoubleArray, NewDoubleArray)

JAVA_OBJECT xmlvm_create_java_string(CODENAME_ONE_THREAD_STATE, const char *str) {
    cn1jni_tls *tls = tls_of(threadStateData);
    JNIEnv *env = tls->env;
    jstring s = (*env)->NewStringUTF(env, str);
    return cn1jni_wrap_string(threadStateData, s);
}

const char *stringToUTF8(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT str) {
    if (str == JAVA_NULL) {
        return NULL;
    }
    cn1jni_tls *tls = tls_of(threadStateData);
    JNIEnv *env = tls->env;
    jstring js = (jstring) ((cn1jni_object *) str)->jref;
    const char *utf = (*env)->GetStringUTFChars(env, js, NULL);
    if (utf == NULL) {
        return NULL;
    }
    size_t len = strlen(utf) + 1;
    char *copy = (char *) cn1jni_arena_alloc(threadStateData, len);
    memcpy(copy, utf, len);
    (*env)->ReleaseStringUTFChars(env, js, utf);
    return copy;
}

void throwException(struct ThreadLocalData *threadStateData, JAVA_OBJECT exceptionArg) {
    cn1jni_tls *tls = tls_of(threadStateData);
    threadStateData->exception = exceptionArg;
    if (exceptionArg != JAVA_NULL) {
        /*
         * Defer the actual JNI Throw to cn1jni_exit: ParparVM code keeps
         * running after throwException, and JNI calls with a pending
         * exception are illegal.
         */
        tls->pendingThrow = (jthrowable) ((cn1jni_object *) exceptionArg)->jref;
    }
}

/* ---- classNameLookup stub (per-app table in translated builds) ----------- */

int classNameLookup[1] = {0};

/* ---- JNI_OnLoad ----------------------------------------------------------- */

JavaVM *cn1jni_javavm(void) {
    return cn1jni_vm;
}

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    cn1jni_vm = vm;
    JNIEnv *env = NULL;
    if ((*vm)->GetEnv(vm, (void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
#ifdef __APPLE__
    if (cn1sim_register_ios(env) != JNI_OK) {
        fprintf(stderr, "cn1sim: failed to register IOSNative methods\n");
        return JNI_ERR;
    }
    if (cn1sim_register_host(env) != JNI_OK) {
        fprintf(stderr, "cn1sim: failed to register CN1SimHost methods\n");
        return JNI_ERR;
    }
    if (cn1sim_register_window(env) != JNI_OK) {
        fprintf(stderr, "cn1sim: failed to register CN1SimHost window methods\n");
        return JNI_ERR;
    }
#else
    if (cn1sim_register_windows(env) != JNI_OK) {
        fprintf(stderr, "cn1sim: failed to register WindowsNative methods\n");
        return JNI_ERR;
    }
#endif
    return JNI_VERSION_1_6;
}
